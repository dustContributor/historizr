package io.historizr.device.api;

import static io.historizr.device.OpsMisc.sendJson;
import static io.historizr.device.OpsReq.failed;
import static io.historizr.device.OpsReq.notFound;

import java.util.logging.Logger;

import io.historizr.device.OpsJson;
import io.historizr.device.db.Db;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.web.Router;

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

	public static Router register(EventBus bus, Router router, JDBCClient conn) {
		var signalModel = io.historizr.device.db.Signal.class;
		router.get(ROUTE)
				.handler(ctx -> {
					conn.query(Db.Sql.QUERY_SIGNAL, r -> {
						if (failed(r, ctx)) {
							return;
						}
						var rs = r.result();
						ctx.json(rs.getRows());
					});
				});
		router.post(ROUTE)
				.handler(ctx -> {
					var entity = ctx.body().asPojo(signalModel);
					var pars = entity.into(new JsonArray());
					conn.queryWithParams(Db.Sql.INSERT_SIGNAL, pars, r -> {
						if (failed(r, ctx)) {
							return;
						}
						var res = r.result().getRows().get(0);
						LOGGER.fine(() -> "POST " + res);
						sendJson(bus, EVENT_INSERTED, res);
						ctx.json(res);
					});
				});
		router.put(ROUTE)
				.handler(ctx -> {
					var entity = ctx.body().asPojo(signalModel);
					var pars = entity.into(new JsonArray(), true);
					conn.queryWithParams(Db.Sql.UPDATE_SIGNAL, pars, r -> {
						if (failed(r, ctx)) {
							return;
						}
						var rows = r.result().getRows();
						if (notFound(rows.size(), ctx)) {
							return;
						}
						var res = rows.get(0);
						LOGGER.fine(() -> "PUT " + res);
						sendJson(bus, EVENT_UPDATED, res);
						ctx.json(res);
					});
				});
		router.delete(ROUTE)
				.handler(ctx -> {
					var id = ctx.queryParam("id");
					var pars = new JsonArray(id);
					conn.updateWithParams(Db.Sql.DELETE_SIGNAL, pars, r -> {
						if (failed(r, ctx)) {
							return;
						}
						var res = r.result();
						if (res.getUpdated() > 0) {
							var entity = io.historizr.device.db.Signal.empty(pars.getLong(0));
							sendJson(bus, EVENT_DELETED, entity);
						}
						LOGGER.fine(() -> "DELETE " + OpsJson.toString(res));
						ctx.json(new Object() {
							@SuppressWarnings("unused")
							public int getUpdated() {
								return res.getUpdated();
							}
						});
					});
				});
		return router;
	}
}
