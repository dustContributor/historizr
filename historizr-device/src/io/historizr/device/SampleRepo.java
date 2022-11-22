package io.historizr.device;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import io.historizr.device.db.Sample;
import io.historizr.shared.OpsJson;
import io.historizr.shared.OpsMisc;
import io.historizr.shared.db.Signal;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

public final class SampleRepo implements AutoCloseable {
	private static final Logger LOGGER = OpsMisc.classLogger();
	private final Config cfg;
	private final Vertx vertx;
	private final ConcurrentHashMap<Long, Sample> samplesById = new ConcurrentHashMap<>();
	private MqttClient publisherClient;
	private MqttClient subscriberClient;
	private SignalRepo signalRepo;
	private long receivedCount;
	private long processedCount;
	private long skippedCount;
	private long publishedCount;

	public SampleRepo(Config cfg, Vertx vertx) {
		this.cfg = Objects.requireNonNull(cfg);
		this.vertx = Objects.requireNonNull(vertx);
	}

	public final SampleRepo init(SignalRepo signalRepo) throws MqttException {
		LOGGER.info("Initializing...");
		this.signalRepo = Objects.requireNonNull(signalRepo);
		var clientId = makeClientId();
		publisherClient = connectClient(clientId, "pub");
		subscriberClient = connectClient(clientId, "sub");
		LOGGER.info("Initialized!");
		return this;
	}

	private final MqttClient connectClient(String baseId, String sufix) throws MqttException {
		var clientId = baseId + '_' + sufix;
		LOGGER.info("Creating MQTT " + sufix + " client");
		var client = new MqttClient(cfg.broker(), clientId, null);
		LOGGER.info("Connecting to broker with client id " + clientId + "...");
		client.connect();
		LOGGER.info("Connected!");
		return client;
	}

	private final String makeClientId() {
		return cfg.hasClientIdUuid() ? cfg.clientId() + '_' + UUID.randomUUID() : cfg.clientId();
	}

	@Override
	public final void close() throws Exception {
		LOGGER.info("Closing...");
		if (publisherClient != null) {
			publisherClient.close();
		}
		if (subscriberClient != null) {
			subscriberClient.close();
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

	private static final Sample sampleOf(DataTypeMapper mapper, MqttMessage msg, boolean hasFullPayload) {
		if (mapper == null) {
			// Avoid parsing payload if the type is unknown.
			return null;
		}
		if (hasFullPayload) {
			try {
				var obj = OpsJson.fromBytes(msg.getPayload(), mapper.sampleType());
				if (obj != null && obj.isValid()) {
					return obj;
				}
			} catch (Exception ex) {
				LOGGER.log(Level.FINEST, "Failed reading full payload", ex);
			}
			return null;
		}
		return mapper.toSample().apply(msg.toString(), now());
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

	private static final OffsetDateTime now() {
		return OffsetDateTime.now(ZoneOffset.UTC);
	}

	private final void handleMessage(String topic, MqttMessage msg) {
		++receivedCount;
		LOGGER.fine(() -> "Incoming message:topic: " + msg + ":" + topic);
		var signal = signalRepo.signalByTopic(topic);
		if (signal == null) {
			// Signal doesn't has an assigned topic.
			LOGGER.fine(() -> "Topic doesn't has an assigned signal: " + topic);
			return;
		}
		var dataType = signalRepo.dataTypeById(signal.dataTypeId());
		if (dataType == null) {
			// Unrecognized data type for this signal. Shouldn't happen.
			LOGGER.fine(() -> "Unrecognized data type for signal: " + signal);
			return;
		}
		var dataTypeMapper = DataTypeMapper.ofId(dataType.mappingId());
		var sample = sampleOf(dataTypeMapper, msg, signal.hasFullPayload());
		if (sample == null) {
			LOGGER.fine(() -> "Unrecognized mapped data type for signal: " + signal + ", data type: "
					+ dataType + ", mapped to: " + dataTypeMapper);
			// Unrecognized mapped data type. Shouldn't happen.
			return;
		}
		++processedCount;
		var oid = Long.valueOf(signal.id());
		var changed = samplesById.compute(oid, (key, existing) -> evaluateChange(signal, sample, existing));
		if (sample != changed) {
			// Existing sample didn't get updated, wont emit.
			++skippedCount;
			return;
		}
		// Encode and send via mqtt.
		var payload = OpsJson.toBytes(sample.withId(signal.id()));
		var outMsg = new MqttMessage(payload);
		var outTopic = cfg.outputTopic() + signal.name();
		LOGGER.fine(() -> "Publishing message:topic: " + outMsg + ":" + outTopic);
		// Cant publish from the method that handles the subscription event.
		vertx.runOnContext(new DeferredPublish(publisherClient, outTopic, outMsg));
		++publishedCount;
	}

	public final void removeSample(long id) {
		var oid = Long.valueOf(id);
		samplesById.remove(oid);
	}

	public final void discardSampleState() {
		samplesById.clear();
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
			subscriberClient.subscribe(topic, handler);
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

	public final long publishedCount() {
		return publishedCount;
	}
}
