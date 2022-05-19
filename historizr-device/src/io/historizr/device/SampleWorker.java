package io.historizr.device;

import io.historizr.device.api.Signal;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;

public final class SampleWorker extends AbstractVerticle {
	public static final String NAME = SampleWorker.class.getName();
	private SignalRepo signalRepo;
	private SampleRepo sampleRepo;

	private static MessageConsumer<io.historizr.device.db.Signal> signalConsumer(EventBus bus, String name) {
		return bus.<io.historizr.device.db.Signal>consumer(name);
	}

	@Override
	public final void start() throws Exception {
		var bus = vertx.eventBus();
		var cfg = this.config().mapTo(Config.class);
		this.signalRepo = new SignalRepo(cfg);
		signalRepo.init();
		this.sampleRepo = new SampleRepo(cfg);
		sampleRepo.init(signalRepo);
		if (cfg.hasDebugTopic()) {
			sampleRepo.debugOutput();
		}
		sampleRepo.subscribe();
		var inserted = signalConsumer(bus, Signal.EVENT_INSERTED);
		var updated = signalConsumer(bus, Signal.EVENT_UPDATED);
		var deleted = signalConsumer(bus, Signal.EVENT_DELETED);
		inserted.handler(e -> {
			signalRepo.updateSignal(e.body());
		});
		updated.handler(e -> {
			signalRepo.updateSignal(e.body());
		});
		deleted.handler(e -> {
			signalRepo.removeSignal(e.body());
		});
	}

	@Override
	public final void stop() throws Exception {
		var signalRepo = this.sampleRepo;
		try (signalRepo) {
			// Close.
		}
	}
}
