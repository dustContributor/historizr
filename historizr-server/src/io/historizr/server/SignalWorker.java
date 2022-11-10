package io.historizr.server;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.historizr.server.api.SignalApi;
import io.historizr.server.db.Db;
import io.historizr.server.db.Device;
import io.historizr.shared.OpsMisc;
import io.historizr.shared.db.Signal;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;

public final class SignalWorker extends AbstractVerticle {
	private static final Logger LOGGER = OpsMisc.classLogger();
	public static final String API_DEVICE = "/device";
	public static final String API_SIGNAL = "/signal";

	private final SqlClient conn;

	private MessageConsumer<JsonObject> inserted;
	private MessageConsumer<JsonObject> updated;
	private MessageConsumer<JsonObject> deleted;

	private WebClient client;

	public SignalWorker(SqlClient conn) {
		this.conn = Objects.requireNonNull(conn);
	}

	private static MessageConsumer<JsonObject> handle(EventBus bus, String event, Consumer<Signal> handler) {
		return bus.<JsonObject>consumer(event).handler(e -> handler.accept(e.body().mapTo(Signal.class)));
	}

	private final Future<HttpResponse<Buffer>> notifySignal(HttpMethod method, Device device, Signal signal) {
		var port = device.port();
		var address = device.address().getHostAddress();
		String kind;
		if (HttpMethod.POST.equals(method)) {
			kind = "INSERT";
		} else if (HttpMethod.PUT.equals(method)) {
			kind = "UPDATE";
		} else if (HttpMethod.DELETE.equals(method)) {
			kind = "DELETE";
		} else {
			kind = "UNKNOWN";
		}
		var body = JsonObject.mapFrom(signal);
		// Device id isn't used on the device's signal model
		body.remove("deviceId");
		return this.client.request(method, port, address, API_SIGNAL)
				.sendJson(body)
				.onSuccess(v -> {
					if (v.statusCode() != 200) {
						LOGGER.warning("Failed notifying signal %s %s on device %s with status ".formatted(signal.id(),
								kind, device.id(), v.statusCode()));
						return;
					}
					LOGGER.fine(() -> "Notified device %s of %s!".formatted(device.id(), kind));
				})
				.onFailure(e -> {
					LOGGER.log(Level.WARNING,
							"Failed notifying signal %s %s on device %s".formatted(signal.id(), kind, device.id()), e);
				});
	}

	private final Future<Device> queryDevice(Signal signal) {
		return conn.preparedQuery(Db.Device.QUERY_BY_ID)
				.execute(Tuple.of(signal.deviceId()))
				.compose(rows -> {
					if (rows.size() < 1) {
						var msg = "Device not found: " + signal.deviceId();
						LOGGER.log(Level.WARNING, msg);
						return Future.failedFuture(msg);
					}
					var device = Device.of(rows.iterator().next());
					return Future.succeededFuture(device);
				}, r -> {
					var msg = "Failed to query device: " + signal.deviceId();
					LOGGER.log(Level.WARNING, msg, r);
					return Future.failedFuture(msg);
				});
	}

	@Override
	public final void start() throws Exception {
		LOGGER.info("Starting...");
		try {
			this.client = WebClient.create(vertx);
			var bus = vertx.eventBus();
			inserted = handle(bus, SignalApi.EVENT_INSERTED, signal -> {
				LOGGER.fine(() -> "Notifying device of signal INSERT " + signal);
				queryDevice(signal).map(device -> {
					return notifySignal(HttpMethod.POST, device, signal);
				});
			});
			updated = handle(bus, SignalApi.EVENT_UPDATED, signal -> {
				LOGGER.fine(() -> "Notifying device of signal UPDATE " + signal);
				queryDevice(signal).map(device -> {
					return notifySignal(HttpMethod.PUT, device, signal);
				});
			});
			deleted = handle(bus, SignalApi.EVENT_DELETED, signal -> {
				LOGGER.fine(() -> "Notifying device of signal DELETE " + signal);
				queryDevice(signal).map(device -> {
					return notifySignal(HttpMethod.DELETE, device, signal);
				});
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
		if (client != null) {
			client.close();
		}
		unregister(inserted);
		unregister(updated);
		unregister(deleted);
		LOGGER.info("Stopped!");
	}

	private static void unregister(MessageConsumer<?> c) throws InterruptedException {
		if (c != null) {
			c.unregister().wait();
		}
	}
}
