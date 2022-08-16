package io.historizr.server.api;

import static io.historizr.server.OpsMisc.sendJson;
import static io.historizr.server.OpsReq.failed;
import static io.historizr.server.OpsReq.notFound;

import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.historizr.server.OpsMisc;
import io.historizr.server.db.Db;
import io.historizr.server.db.MappingOp;
import io.historizr.server.db.Device;
import io.vertx.core.eventbus.EventBus;
import io.vertx.ext.web.Router;
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

	public static Router register(EventBus bus, Router router, SqlClient conn) {
		var toModels = Collectors.mapping(Device::of, Collectors.toList());
		var modelType = Device.class;
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
