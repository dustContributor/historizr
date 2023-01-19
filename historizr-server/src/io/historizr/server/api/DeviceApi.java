package io.historizr.server.api;

import static io.historizr.shared.OpsMisc.sendJson;
import static io.historizr.shared.OpsReq.failed;
import static io.historizr.shared.OpsReq.notFound;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.historizr.server.SignalWorker;
import io.historizr.server.db.Db;
import io.historizr.server.db.Device;
import io.historizr.shared.OpsMisc;
import io.historizr.shared.db.MappingOp;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;

public final class DeviceApi {
	private DeviceApi() {
		throw new RuntimeException();
	}

	private static final Logger LOGGER = OpsMisc.classLogger();
	private static final String EVENT_ROOT = OpsMisc.className();
	public static final String EVENT_INSERTED = EVENT_ROOT + ".inserted";
	public static final String EVENT_UPDATED = EVENT_ROOT + ".updated";
	public static final String EVENT_DELETED = EVENT_ROOT + ".deleted";

	private static final String ROUTE = "/device";

	record ServerRevision(Device device, long revision) {
		public static ServerRevision of(Row r) {
			return new ServerRevision(Device.of(r), r.getLong(r.size() - 1));
		}
	}

	record DeviceRevision(
			String host,
			long signalsRevision,
			long signalsTotal,
			long signalsWithOnChange,
			long signalsWithDeadband,
			long signalsWithFullPayload,
			long messagesRegistered,
			long messagesReceived,
			long messagesProcessed,
			long messagesSkipped,
			long messagesPublished) {
	}

	public static Router register(Vertx vertx, Router router, SqlClient conn) {
		var bus = vertx.eventBus();
		var toModels = Collectors.mapping(Device::of, Collectors.toList());
		var modelType = Device.class;
		var toRevisions = Collectors.mapping(ServerRevision::of, Collectors.toList());
		var client = WebClient.create(vertx);
		router.post(ROUTE + "/verify").handler(ctx -> {
			var entity = ctx.body().asPojo(modelType);
			@SuppressWarnings("unused")
			var tmp = new Object() {
				public ServerRevision server;
				public DeviceRevision device;
			};
			conn.preparedQuery(Db.Misc.REVISION_TOTAL)
					.collecting(toRevisions)
					.execute(Tuple.of(entity.id()))
					.compose(r -> {
						var rows = r.value();
						if (notFound(rows, ctx)) {
							return Future.succeededFuture();
						}
						tmp.server = rows.get(0);
						var device = tmp.server.device();
						return client.get(device.port(), device.address().getHostAddress(), SignalWorker.API_DEVICE)
								.send();
					}).onSuccess(r -> {
						if (tmp.server == null) {
							// Couldn't find device
							return;
						}
						tmp.device = r.bodyAsJson(DeviceRevision.class);
						ctx.json(tmp);
					}).onFailure(r -> {
						var msg = "Failed to query revision sum";
						LOGGER.log(Level.WARNING, msg, r);
						ctx.fail(r);
					});
		});

		router.get(ROUTE).handler(ctx -> {
			conn.query(Db.Device.QUERY)
					.collecting(toModels)
					.execute(r -> {
						if (failed(r, ctx)) {
							return;
						}
						var res = r.result().value();
						ctx.json(res);
					});
		});
		router.post(ROUTE).handler(ctx -> {
			var entity = ctx.body().asPojo(modelType);
			var pars = entity.tuple(MappingOp.ID_SKIP);
			conn.preparedQuery(Db.Device.INSERT)
					.collecting(toModels)
					.execute(pars, r -> {
						if (failed(r, ctx)) {
							return;
						}
						var res = r.result().value().get(0);
						LOGGER.fine(() -> "POST " + res);
						sendJson(bus, EVENT_INSERTED, res);
						ctx.json(res);
					});
		});
		router.put(ROUTE).handler(ctx -> {
			var entity = ctx.body().asPojo(modelType);
			var pars = entity.tuple(MappingOp.ID_LAST);
			conn.preparedQuery(Db.Device.UPDATE)
					.collecting(toModels)
					.execute(pars, r -> {
						if (failed(r, ctx)) {
							return;
						}
						var rows = r.result().value();
						if (notFound(rows, ctx)) {
							return;
						}
						var res = rows.get(0);
						LOGGER.fine(() -> "PUT " + res);
						sendJson(bus, EVENT_UPDATED, res);
						ctx.json(res);
					});
		});
		router.delete(ROUTE).handler(ctx -> {
			var entity = ctx.body().asPojo(modelType);
			var pars = Tuple.of(entity.id());
			conn.preparedQuery(Db.Device.DELETE)
					.collecting(toModels)
					.execute(pars, r -> {
						if (failed(r, ctx)) {
							return;
						}
						var rows = r.result().value();
						if (notFound(rows, ctx)) {
							return;
						}
						var res = rows.get(0);
						LOGGER.fine(() -> "DELETE " + res);
						sendJson(bus, EVENT_UPDATED, res);
						ctx.json(res);
					});
		});
		return router;
	}
}
