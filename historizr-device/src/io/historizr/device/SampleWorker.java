package io.historizr.device;

import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.historizr.device.api.DeviceApi;
import io.historizr.device.api.SignalApi;
import io.historizr.shared.OpsMisc;
import io.historizr.shared.db.Signal;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;

public final class SampleWorker extends AbstractVerticle {
	private static final Logger LOGGER = OpsMisc.classLogger();
	private SignalRepo signalRepo;
	private SampleRepo sampleRepo;
	private MessageConsumer<JsonObject> inserted;
	private MessageConsumer<JsonObject> updated;
	private MessageConsumer<JsonObject> deleted;
	private MessageConsumer<Object> discardSampleState;

	private static MessageConsumer<JsonObject> handle(EventBus bus, String event, Consumer<Signal> handler) {
		return bus.<JsonObject>consumer(event).handler(e -> handler.accept(e.body().mapTo(Signal.class)));
	}

	public final SignalRepo signalRepo() {
		return signalRepo;
	}

	public final SampleRepo sampleRepo() {
		return sampleRepo;
	}

	@Override
	public final void start() throws Exception {
		LOGGER.info("Starting...");
		try {
			var bus = vertx.eventBus();
			var cfg = this.config().mapTo(Config.class);
			this.signalRepo = new SignalRepo(cfg);
			signalRepo.init();
			this.sampleRepo = new SampleRepo(cfg, vertx);
			sampleRepo.init(signalRepo);
			if (cfg.hasDebugTopic()) {
				sampleRepo.debugOutput();
			}
			sampleRepo.subscribe();
			inserted = handle(bus, SignalApi.EVENT_INSERTED, e -> {
				signalRepo.updateSignal(e);
				LOGGER.fine(() -> "Inserted " + e);
			});
			updated = handle(bus, SignalApi.EVENT_UPDATED, e -> {
				signalRepo.updateSignal(e);
				LOGGER.fine(() -> "Updated " + e);
			});
			deleted = handle(bus, SignalApi.EVENT_DELETED, e -> {
				signalRepo.removeSignal(e);
				sampleRepo.removeSample(e.id());
				LOGGER.fine(() -> "Deleted " + e);
			});
			discardSampleState = bus.consumer(DeviceApi.EVENT_DISCARD_SAMPLE_STATE).handler(e -> {
				sampleRepo.discardSampleState();
				LOGGER.fine(() -> "Discarded sample state");
			});
			discardSampleState = bus.consumer(DeviceApi.EVENT_DISCARD_SAMPLE_STATS).handler(e -> {
				sampleRepo.discardSampleStats();
				LOGGER.fine(() -> "Discarded sample stats");
			});
			LOGGER.info("Started!");
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Failed to start", e);
			throw e;
		}
	}

	@Override
	public final void stop() throws Exception {
		LOGGER.info("Stopping...");
		inserted.unregister().wait();
		updated.unregister().wait();
		deleted.unregister().wait();
		discardSampleState.unregister().wait();
		try (var sampleRepo = this.sampleRepo) {
			// Close.
		}
		LOGGER.info("Stopped!");
	}
}
