package io.historizr.server.api;

import static io.historizr.shared.OpsReq.notFound;
import static io.historizr.shared.OpsReq.ok;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.historizr.server.SignalWorker;
import io.historizr.server.db.Db;
import io.historizr.server.db.Device;
import io.historizr.shared.OpsMisc;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;

public final class DeviceOpsApi {
	private DeviceOpsApi() {
		throw new RuntimeException();
	}

	private static final Logger LOGGER = OpsMisc.classLogger();

	private static final String ROUTE = "/deviceops";

	public static Router register(Vertx vertx, Router router, SqlClient conn) {
		var toModels = Collectors.mapping(Device::of, Collectors.toList());
		var modelType = Device.class;
		var client = WebClient.create(vertx);
		router.post(ROUTE + "/status").handler(ctx -> {
			var entity = ctx.body().asPojo(modelType);
			conn.preparedQuery(Db.Device.QUERY_BY_ID)
					.collecting(toModels)
					.execute(Tuple.of(entity.id()))
					.compose(r -> {
						var rows = r.value();
						if (notFound(rows, ctx)) {
							return deviceNotFound(entity.id());
						}
						var device = rows.get(0);
						return client
								.get(device.port(), device.address().getHostAddress(),
										SignalWorker.API_DEVICE)
								.send();
					}).onSuccess(r -> {
						ctx.json(r.bodyAsJsonObject());
					}).onFailure(r -> {
						var msg = "Failed to discard sample state";
						LOGGER.log(Level.WARNING, msg, r);
						ctx.fail(r);
					});
		});
		router.post(ROUTE + "/discardsamplestate").handler(ctx -> {
			var entity = ctx.body().asPojo(modelType);
			conn.preparedQuery(Db.Device.QUERY_BY_ID)
					.collecting(toModels)
					.execute(Tuple.of(entity.id()))
					.compose(r -> {
						var rows = r.value();
						if (notFound(rows, ctx)) {
							return deviceNotFound(entity.id());
						}
						var device = rows.get(0);
						return client
								.get(device.port(), device.address().getHostAddress(),
										SignalWorker.API_DEVICE_DISCARD_SAMPLE_STATE)
								.send();
					}).onSuccess(r -> {
						ok(ctx);
					}).onFailure(r -> {
						var msg = "Failed to discard sample state";
						LOGGER.log(Level.WARNING, msg, r);
						ctx.fail(r);
					});
		});
		router.post(ROUTE + "/discardsamplestats").handler(ctx -> {
			var entity = ctx.body().asPojo(modelType);
			conn.preparedQuery(Db.Device.QUERY_BY_ID)
					.collecting(toModels)
					.execute(Tuple.of(entity.id()))
					.compose(r -> {
						var rows = r.value();
						if (notFound(rows, ctx)) {
							return Future.succeededFuture();
						}
						var device = rows.get(0);
						return client
								.get(device.port(), device.address().getHostAddress(),
										SignalWorker.API_DEVICE_DISCARD_SAMPLE_STATS)
								.send();
					}).onSuccess(r -> {
						ok(ctx);
					}).onFailure(r -> {
						var msg = "Failed to discard sample stats";
						LOGGER.log(Level.WARNING, msg, r);
						ctx.fail(r);
					});
		});

		return router;
	}

	private static final <T> Future<T> deviceNotFound(long id) {
		return Future.failedFuture("Device with id %d not found".formatted(id));
	}
}
