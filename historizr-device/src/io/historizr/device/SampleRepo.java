package io.historizr.device;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import io.historizr.device.db.DataType;
import io.historizr.device.db.Sample;
import io.historizr.device.db.Signal;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

public final class SampleRepo implements AutoCloseable {
	private final static Logger LOGGER = Logger.getLogger(SampleRepo.class.getName());
	private final Config cfg;
	private final Vertx vertx;
	private final ConcurrentHashMap<Long, Sample> samplesById = new ConcurrentHashMap<>();
	private MqttClient client;
	private SignalRepo signalRepo;
	private long receivedCount;
	private long processedCount;
	private long skippedCount;

	public SampleRepo(Config cfg, Vertx vertx) {
		this.cfg = Objects.requireNonNull(cfg);
		this.vertx = Objects.requireNonNull(vertx);
	}

	public final SampleRepo init(SignalRepo signalRepo) throws MqttException {
		LOGGER.info("Initializing...");
		this.signalRepo = Objects.requireNonNull(signalRepo);
		var clientId = makeClientId();
		LOGGER.info("MQTT client id: " + clientId);
		client = new MqttClient(cfg.broker(), clientId);
		LOGGER.info("Connecting to broker...");
		client.connect();
		LOGGER.info("Connected!");
		LOGGER.info("Initialized!");
		return this;
	}

	private final String makeClientId() {
		return cfg.hasClientIdUuid() ? cfg.clientId() + '_' + UUID.randomUUID() : cfg.clientId();
	}

	@Override
	public final void close() throws Exception {
		LOGGER.info("Closing...");
		if (client != null) {
			client.close();
		}
		LOGGER.info("Closed!");
	}

	private static record DeferredPublish(MqttClient client, String topic, MqttMessage message)
			implements Handler<Void> {
		@Override
		public final void handle(Void event) {
			try {
				client.publish(topic, message);
			} catch (MqttException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private static final Sample sampleOf(DataType.Catalog dataType, MqttMessage msg, OffsetDateTime now,
			boolean hasFullPayload) {
		if (dataType == null || dataType == DataType.Catalog.UNKNOWN) {
			// Avoid parsing payload if the type is unknown.
			return null;
		}
		return hasFullPayload ? fullPayload(dataType, msg.getPayload())
				: simplePayload(dataType, msg.toString(), now);
	}

	private static final Sample fullPayload(DataType.Catalog dataType, byte[] payload) {
		var type = switch (dataType) {
		case BOOL -> Sample.OfBool.class;
		case F32 -> Sample.OfFloat.class;
		case F64 -> Sample.OfDouble.class;
		case I64 -> Sample.OfLong.class;
		case STR -> Sample.OfString.class;
		default -> null;
		};
		try {
			return OpsJson.fromBytes(payload, type);
		} catch (Exception ex) {
			return null;
		}
	}

	private static final Sample simplePayload(DataType.Catalog dataType, String payload, OffsetDateTime now) {
		return switch (dataType) {
		case BOOL -> Sample.OfBool.of(payload, now);
		case F32 -> Sample.OfFloat.of(payload, now);
		case F64 -> Sample.OfDouble.of(payload, now);
		case I64 -> Sample.OfLong.of(payload, now);
		case STR -> Sample.OfString.of(payload, now);
		default -> null;
		};
	}

	private static final Sample evaluateChange(Signal signal, Sample current, Sample previous) {
		if (previous == null) {
			// No previous sample registered, update.
			return current;
		}
		if (previous.tstamp.compareTo(current.tstamp) > 0) {
			// Somehow we got an older sample, don't update.
			return previous;
		}
		if (signal.deadband() > 0) {
			// Deadband configured for this signal.
			var deadband = Sample.toDeadband(signal.deadband());
			if (!previous.exceedsDeadband(current, deadband)) {
				// Value within the deadband, don't update.
				return previous;
			}
		} else if (signal.isOnChange()) {
			// Signal only emits if it changed.
			if (!previous.hasDifferentValue(current)) {
				// Sample didn't change, don't update.
				return previous;
			}
		}
		return current;
	}

	private final void handleMessage(String topic, MqttMessage msg) {
		receivedCount = receivedCount() + 1;
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
		var sample = sampleOf(mappedType, msg, now, signal.hasFullPayload());
		if (sample == null) {
			LOGGER.warning(() -> "Unrecognized mapped data type for signal: " + signal + ", mapped to: "
					+ dataType);
			// Unrecognized mapped data type. Shouldn't happen.
			return;
		}
		processedCount = processedCount() + 1;
		var oid = Long.valueOf(signal.id());
		var changed = samplesById.compute(oid, (key, existing) -> evaluateChange(signal, sample, existing));
		if (sample != changed) {
			// Existing sample didn't get updated, wont emit.
			skippedCount = skippedCount() + 1;
			return;
		}
		// Encode and send via mqtt.
		var payload = OpsJson.toBytes(sample.withId(signal.id()));
		var outMsg = new MqttMessage(payload);
		var outTopic = cfg.outputTopic() + signal.name();
		LOGGER.fine(() -> "Publishing message:topic: " + outMsg + ":" + outTopic);
		// Cant publish from the method that handles the subscription event.
		vertx.runOnContext(new DeferredPublish(client, outTopic, outMsg));
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

	public final long receivedCount() {
		return receivedCount;
	}

	public final long processedCount() {
		return processedCount;
	}

	public final long skippedCount() {
		return skippedCount;
	}
}
