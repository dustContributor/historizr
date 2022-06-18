package io.historizr.device;

import static io.historizr.device.api.Signal.EVENT_DELETED;
import static io.historizr.device.api.Signal.EVENT_INSERTED;
import static io.historizr.device.api.Signal.EVENT_UPDATED;

import java.util.function.Consumer;
import java.util.logging.Logger;

import io.historizr.device.db.Signal;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;

public final class SampleWorker extends AbstractVerticle {
	private final static Logger LOGGER = Logger.getLogger(SampleWorker.class.getName());
	private SignalRepo signalRepo;
	private SampleRepo sampleRepo;
	private MessageConsumer<JsonObject> inserted;
	private MessageConsumer<JsonObject> updated;
	private MessageConsumer<JsonObject> deleted;

	private static MessageConsumer<JsonObject> handle(EventBus bus, String event, Consumer<Signal> handler) {
		return bus.<JsonObject>consumer(event).handler(e -> handler.accept(e.body().mapTo(Signal.class)));
	}

	@Override
	public final void start() throws Exception {
		LOGGER.info("Starting...");
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
		inserted = handle(bus, EVENT_INSERTED, e -> {
			signalRepo.updateSignal(e);
			LOGGER.fine(() -> "Inserted " + e);
		});
		updated = handle(bus, EVENT_UPDATED, e -> {
			signalRepo.updateSignal(e);
			LOGGER.fine(() -> "Updated " + e);
		});
		deleted = handle(bus, EVENT_DELETED, e -> {
			signalRepo.removeSignal(e);
			sampleRepo.removeSample(e.id());
			LOGGER.fine(() -> "Deleted " + e);
		});
		LOGGER.info("Started!");
	}

	@Override
	public final void stop() throws Exception {
		LOGGER.info("Stopping...");
		inserted.unregister().wait();
		updated.unregister().wait();
		deleted.unregister().wait();
		try (var sampleRepo = this.sampleRepo) {
			// Close.
		}
		LOGGER.info("Stopped!");
	}
}
