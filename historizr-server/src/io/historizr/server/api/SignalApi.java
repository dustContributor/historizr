package io.historizr.server.api;

import static io.historizr.server.OpsMisc.sendJson;
import static io.historizr.server.OpsReq.failed;
import static io.historizr.server.OpsReq.notFound;

import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.historizr.server.OpsMisc;
import io.historizr.server.db.Db;
import io.historizr.server.db.MappingOp;
import io.historizr.server.db.Signal;
import io.vertx.core.eventbus.EventBus;
import io.vertx.ext.web.Router;
import io.vertx.sqlclient.SqlClient;

public final class SignalApi {
	private SignalApi() {
		throw new RuntimeException();
	}

	private static final Logger LOGGER = OpsMisc.classLogger();
	private static final String EVENT_ROOT = Signal.class.getName();
	public static final String EVENT_INSERTED = EVENT_ROOT + ".inserted";
	public static final String EVENT_UPDATED = EVENT_ROOT + ".updated";
	public static final String EVENT_DELETED = EVENT_ROOT + ".deleted";

	private static final String ROUTE = "/signal";

	public static Router register(EventBus bus, Router router, SqlClient conn) {
		var toModels = Collectors.mapping(Signal::of, Collectors.toList());
		var modelType = Signal.class;
		router.get(ROUTE).handler(ctx -> {
			conn.query(Db.Signal.QUERY)
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
			conn.preparedQuery(Db.Signal.INSERT)
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
			conn.preparedQuery(Db.Signal.UPDATE)
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
//		router.delete(ROUTE)
//				.handler(ctx -> {
//					var id = ctx.queryParam("id");
//					var pars = new JsonArray(id);
//					conn.updateWithParams(Db.Sql.DELETE_SIGNAL, pars, r -> {
//						if (failed(r, ctx)) {
//							return;
//						}
//						var res = r.result();
//						if (res.getUpdated() > 0) {
//							var entity = io.historizr.device.db.Signal.empty(pars.getLong(0));
//							sendJson(bus, EVENT_DELETED, entity);
//						}
//						LOGGER.fine(() -> "DELETE " + OpsJson.toString(res));
//						ctx.json(new Object() {
//							@SuppressWarnings("unused")
//							public int getUpdated() {
//								return res.getUpdated();
//							}
//						});
//					});
//				});
		return router;
	}
}
