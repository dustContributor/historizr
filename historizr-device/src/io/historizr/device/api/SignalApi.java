package io.historizr.device.api;

import static io.historizr.device.OpsMisc.sendJson;
import static io.historizr.device.OpsReq.failed;
import static io.historizr.device.OpsReq.notFound;

import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.historizr.device.OpsJson;
import io.historizr.device.OpsMisc;
import io.historizr.device.db.Db;
import io.vertx.core.eventbus.EventBus;
import io.vertx.ext.web.Router;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;

public final class Signal {
	private Signal() {
		throw new RuntimeException();
	}

	private static final String EVENT_ROOT = Signal.class.getName();
	private final static Logger LOGGER = Logger.getLogger(EVENT_ROOT);
	public static final String EVENT_INSERTED = EVENT_ROOT + ".inserted";
	public static final String EVENT_UPDATED = EVENT_ROOT + ".updated";
	public static final String EVENT_DELETED = EVENT_ROOT + ".deleted";

	private static final String ROUTE = "/signal";

	public static Router register(EventBus bus, Router router, SqlClient conn) {
		var toModels = Collectors.mapping((Row r) -> io.historizr.device.db.Signal.of(r), Collectors.toList());
		var modelType = io.historizr.device.db.Signal.class;
		router.get(ROUTE).handler(ctx -> {
			conn.query(Db.Sql.QUERY_SIGNALS)
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
			var pars = entity.into(Tuple.tuple());
			conn.preparedQuery(Db.Sql.INSERT_SIGNAL)
					.collecting(toModels)
					.execute(pars, r -> {
						if (failed(r, ctx)) {
							return;
						}
						conn.preparedQuery(Db.Sql.QUERY_SIGNAL)
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
			var pars = entity.into(Tuple.tuple(), true);
			conn.preparedQuery(Db.Sql.UPDATE_SIGNAL)
					.execute(pars, r -> {
						if (failed(r, ctx)) {
							return;
						}
						conn.preparedQuery(Db.Sql.QUERY_SIGNAL)
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
			var sid = ctx.queryParams().get("id");
			var oid = OpsMisc.tryParseLong(sid);
			if (oid == null) {
				notFound(ctx);
				return;
			}
			var pars = Tuple.of(oid);
			conn.preparedQuery(Db.Sql.DELETE_SIGNAL)
					.collecting(toModels)
					.execute(pars, r -> {
						if (failed(r, ctx)) {
							return;
						}
						var res = r.result();
						if (res.rowCount() > 0) {
							var entity = io.historizr.device.db.Signal.empty(pars.getLong(0));
							sendJson(bus, EVENT_DELETED, entity);
							LOGGER.fine(() -> "DELETE " + OpsJson.toString(entity));
						}
						ctx.json(new Object() {
							@SuppressWarnings("unused")
							public int getUpdated() {
								return res.rowCount();
							}
						});
					});
		});
		return router;
	}
}
