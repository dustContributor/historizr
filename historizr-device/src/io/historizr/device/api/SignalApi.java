package io.historizr.device.api;

import static io.historizr.shared.OpsMisc.sendJson;
import static io.historizr.shared.OpsReq.failed;
import static io.historizr.shared.OpsReq.notFound;

import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.historizr.device.db.Db;
import io.historizr.shared.OpsMisc;
import io.historizr.shared.db.MappingOp;
import io.historizr.shared.db.Signal;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;

public final class SignalApi {
	private SignalApi() {
		throw new RuntimeException();
	}

	private static final Logger LOGGER = OpsMisc.classLogger();
	private static final String EVENT_ROOT = OpsMisc.className();
	public static final String EVENT_INSERTED = EVENT_ROOT + ".inserted";
	public static final String EVENT_UPDATED = EVENT_ROOT + ".updated";
	public static final String EVENT_DELETED = EVENT_ROOT + ".deleted";

	private static final String ROUTE = "/signal";

	public static Router register(Vertx vertx, Router router, SqlClient conn) {
		var bus = vertx.eventBus();
		var toModels = Collectors.mapping(Signal::ofRow, Collectors.toList());
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
			var pars = entity.tuple(MappingOp.ID_FIRST);
			conn.preparedQuery(Db.Signal.INSERT)
					.collecting(toModels)
					.execute(pars, r -> {
						if (failed(r, ctx)) {
							return;
						}
						conn.preparedQuery(Db.Signal.QUERY_BY_ID)
								.collecting(toModels)
								.execute(Tuple.of(entity.id()), r1 -> {
									var rows = r1.result().value();
									if (notFound(rows, ctx)) {
										return;
									}
									var res = rows.get(0);
									LOGGER.fine(() -> "POST " + res);
									sendJson(bus, EVENT_INSERTED, res);
									ctx.json(res);
								});
					});
		});
		router.put(ROUTE).handler(ctx -> {
			var entity = ctx.body().asPojo(modelType);
			var pars = entity.tuple(MappingOp.ID_LAST);
			conn.preparedQuery(Db.Signal.UPDATE)
					.execute(pars, r -> {
						if (failed(r, ctx)) {
							return;
						}
						conn.preparedQuery(Db.Signal.QUERY_BY_ID)
								.collecting(toModels)
								.execute(Tuple.of(entity.id()), r1 -> {
									var rows = r1.result().value();
									if (notFound(rows, ctx)) {
										return;
									}
									var res = rows.get(0);
									LOGGER.fine(() -> "PUT " + res);
									sendJson(bus, EVENT_UPDATED, res);
									ctx.json(res);
								});

					});
		});
		router.delete(ROUTE).handler(ctx -> {
			var entity = ctx.body().asPojo(modelType);
			var pars = Tuple.of(entity.id());
			conn.preparedQuery(Db.Signal.DELETE)
					.execute(pars, r -> {
						if (failed(r, ctx)) {
							return;
						}
						var rows = r.result().rowCount();
						if (notFound(rows, ctx)) {
							return;
						}
						LOGGER.fine(() -> "DELETE " + entity);
						sendJson(bus, EVENT_DELETED, entity);
						ctx.json(entity);
					});
		});
		return router;
	}
}
