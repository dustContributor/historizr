package io.historizr.device;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import io.historizr.device.db.DataType;
import io.historizr.device.db.Sample;

public final class SampleRepo implements AutoCloseable {
	private final static Logger LOGGER = Logger.getLogger(SampleRepo.class.getName());
	private final Config cfg;
	private final ExecutorService deferred = Executors.newSingleThreadExecutor();
	private final ConcurrentHashMap<Long, Sample> samplesById = new ConcurrentHashMap<>();
	private MqttClient client;
	private SignalRepo signalRepo;

	public SampleRepo(Config cfg) {
		this.cfg = Objects.requireNonNull(cfg);
	}

	public final SampleRepo init(SignalRepo signalRepo) throws MqttException {
		LOGGER.info("Initializing...");
		this.signalRepo = Objects.requireNonNull(signalRepo);
		var clientId = cfg.hasClientIdUuid() ? cfg.clientId() + '_' + UUID.randomUUID() : cfg.clientId();
		LOGGER.info("MQTT client id: " + clientId);
		client = new MqttClient(cfg.broker(), clientId);
		LOGGER.info("Connecting to broker...");
		client.connect();
		LOGGER.info("Connected!");
		LOGGER.info("Initialized!");
		return this;
	}

	@Override
	public final void close() throws Exception {
		LOGGER.info("Closing...");
		if (client != null) {
			client.close();
		}
		deferred.shutdown();
		LOGGER.info("Closed!");
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
		LOGGER.fine(() -> "Incoming message:topic: " + msg + ":" + topic);
		var now = OffsetDateTime.now(ZoneOffset.UTC);
		var signal = signalRepo.signalByTopic(topic);
		if (signal == null) {
			// Signal doesn't has an assigned topic.
			LOGGER.warning(() -> "Topic doesn't has an assigned signal: " + topic);
			return;
		}
		var dataType = signalRepo.dataTypeById(signal.dataTypeId());
		if (dataType == null) {
			// Unrecognized data type for this signal. Shouldn't happen.
			LOGGER.warning(() -> "Unrecognized data type for signal: " + signal);
			return;
		}
		var mappedType = DataType.Catalog.of(dataType.mappingId());
		var sample = sampleOf(mappedType, msg, now);
		if (sample == null) {
			LOGGER.warning(() -> "Unrecognized mapped data type for signal: " + signal + ", mapped to: "
					+ dataType);
			// Unrecognized mapped data type. Shouldn't happen.
			return;
		}
		var oid = Long.valueOf(signal.id());
		var changed = samplesById.compute(oid, (key, existing) -> {
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
		if (sample != changed) {
			// Existing sample didn't get updated, wont emit.
			return;
		}
		// Encode and send via mqtt.
		var payload = OpsJson.toBytes(sample);
		var outMsg = new MqttMessage(payload);
		var outTopic = cfg.outputTopic() + signal.name();
		LOGGER.fine(() -> "Publishing message:topic: " + outMsg + ":" + outTopic);
		// Cant publish from the method that handles the subscription event.
		deferred.execute(new DeferredPublish(client, outTopic, outMsg));
	}

	public final void removeSample(long id) {
		var oid = Long.valueOf(id);
		samplesById.remove(oid);
	}

	public final void subscribe() {
		subscribe(cfg.inputTopic(), this::handleMessage);
	}

	public final void debugOutput() {
		subscribe(cfg.outputTopic() + '#', (topic, msg) -> {
			LOGGER.info(topic + ": " + msg);
		});
	}

	private final void subscribe(String topic, IMqttMessageListener handler) {
		try {
			LOGGER.info("Subscribing to " + topic);
			client.subscribe(topic, handler);
			LOGGER.info("Subscribed!");
		} catch (MqttException e) {
			throw new RuntimeException(e);
		}
	}
}
