package io.historizr.device;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.historizr.device.db.DataType;
import io.historizr.device.db.Sample;

public final class SampleRepo implements AutoCloseable {
	private final Config cfg;
	private final ExecutorService deferred = Executors.newSingleThreadExecutor();
	private final ConcurrentHashMap<Long, Sample> samplesById = new ConcurrentHashMap<>();
	private MqttClient client;
	private SignalRepo signalRepo;

	public SampleRepo(Config cfg) {
		this.cfg = Objects.requireNonNull(cfg);
	}

	public final SampleRepo init(SignalRepo signalRepo) throws MqttException {
		this.signalRepo = Objects.requireNonNull(signalRepo);
		var clientId = cfg.hasClientIdUuid() ? cfg.clientId() + '_' + UUID.randomUUID() : cfg.clientId();
		client = new MqttClient(cfg.broker(), clientId);
		client.connect();
		return this;
	}

	@Override
	public final void close() throws Exception {
		if (client != null) {
			client.close();
		}
		deferred.shutdown();
	}

	private static record DeferredPublish(MqttClient client, String topic, MqttMessage message) implements Runnable {
		@Override
		public final void run() {
			try {
				client.publish(topic, message);
			} catch (MqttException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private static final Sample sampleOf(DataType.Catalog dataType, MqttMessage msg, OffsetDateTime now) {
		return switch (dataType) {
		case BOOL -> Sample.OfBool.of(msg.toString(), now);
		case F32 -> Sample.OfFloat.of(msg.toString(), now);
		case F64 -> Sample.OfDouble.of(msg.toString(), now);
		case I64 -> Sample.OfLong.of(msg.toString(), now);
		case STR -> Sample.OfString.of(msg.toString(), now);
		// Late call msg.toString to avoid parsing payload if the type is unknown.
		default -> null;
		};
	}

	private final void handleMessage(String topic, MqttMessage msg) {
		var now = OffsetDateTime.now(ZoneOffset.UTC);
		var signal = signalRepo.signalByTopic(topic);
		if (signal == null) {
			// Signal doesn't has an assigned topic.
			return;
		}
		var dataType = signalRepo.dataTypeById(signal.dataTypeId());
		if (dataType == null) {
			// Unrecognized data type for this signal. Shouldn't happen.
			return;
		}
		var mappedType = DataType.Catalog.of(dataType.mappingId());
		var sample = sampleOf(mappedType, msg, now);
		if (sample == null) {
			// Unrecognized data type.
			return;
		}
		var oid = Long.valueOf(signal.id());
		samplesById.compute(oid, (key, existing) -> {
			if (existing != null) {
				if (signal.isOnChange() && !existing.hasDifferentValue(sample)) {
					// Signal only emits values on change and it's the same.
					return existing;
				}
				if (existing.tstamp.compareTo(sample.tstamp) > 0) {
					// Somehow we got an older sample, dont update.
					return existing;
				}
			}
			// Passed the check so just update it.
			return sample;
		});
		// Encode and send via mqtt.
		byte[] payload;
		try {
			payload = OpsJson.writer().writeValueAsBytes(sample);
		} catch (JsonProcessingException e) {
			// This should never happen.
			throw new RuntimeException(e);
		}
		var outMsg = new MqttMessage(payload);
		var outTopic = cfg.outputTopic() + signal.name();
		// Cant publish from the method that handles the subscription event.
		deferred.execute(new DeferredPublish(client, outTopic, outMsg));
	}

	public final void removeSample(long id) {
		var oid = Long.valueOf(id);
		samplesById.remove(oid);
	}

	public final void subscribe() {
		try {
			client.subscribe(cfg.inputTopic(), this::handleMessage);
		} catch (MqttException e) {
			throw new RuntimeException(e);
		}
	}

	public final void debugOutput() {
		try {
			client.subscribe(cfg.outputTopic() + '#', (topic, msg) -> {
				System.out.println("%s: %s".formatted(topic, msg));
			});
		} catch (MqttException e) {
			throw new RuntimeException(e);
		}
	}
}
