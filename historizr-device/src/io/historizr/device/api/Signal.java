package io.historizr.device.api;

import static io.historizr.device.OpsMisc.hasFailed;

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
	public static final String EVENT_INSERTED = EVENT_ROOT + ".inserted";
	public static final String EVENT_UPDATED = EVENT_ROOT + ".updated";
	public static final String EVENT_DELETED = EVENT_ROOT + ".deleted";

	private static final String ROUTE = "/signal";

	public static Router register(EventBus bus, Router router, JDBCClient conn) {
		var signalModel = io.historizr.device.db.Signal.class;
		router.get(ROUTE)
				.handler(ctx -> {
					conn.query(Db.Sql.QUERY_SIGNAL, r -> {
						if (hasFailed(r, ctx)) {
							return;
						}
						var rs = r.result();
						ctx.json(rs.getRows());
					});
				});
		router.post(ROUTE)
				.handler(ctx -> {
					var entity = ctx.body().asPojo(signalModel);
					var pars = new JsonArray()
							.add(entity.id())
							.add(entity.dataTypeId())
							.add(entity.name())
							.add(entity.topic())
							.add(entity.isOnChange());
					conn.queryWithParams(Db.Sql.INSERT_SIGNAL, pars, r -> {
						if (hasFailed(r, ctx)) {
							return;
						}
						var res = r.result().getRows().get(0);
						bus.send(EVENT_INSERTED, res.mapTo(signalModel));
						ctx.json(res);
					});
				});
		router.put(ROUTE)
				.handler(ctx -> {
					var entity = ctx.body().asPojo(signalModel);
					var pars = new JsonArray()
							.add(entity.dataTypeId())
							.add(entity.name())
							.add(entity.topic())
							.add(entity.isOnChange())
							.add(entity.id());
					conn.queryWithParams(Db.Sql.UPDATE_SIGNAL, pars, r -> {
						if (hasFailed(r, ctx)) {
							return;
						}
						var res = r.result().getRows().get(0);
						bus.send(EVENT_UPDATED, res.mapTo(signalModel));
						ctx.json(res);
					});
				});
		router.delete(ROUTE)
				.handler(ctx -> {
					var id = ctx.queryParam("id");
					var pars = new JsonArray(id);
					conn.updateWithParams(Db.Sql.DELETE_SIGNAL, pars, r -> {
						if (hasFailed(r, ctx)) {
							return;
						}
						var res = r.result();
						if (res.getUpdated() > 0) {
							var entity = new io.historizr.device.db.Signal(pars.getLong(0), 0, null, null, false);
							bus.send(EVENT_DELETED, entity);
						}
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
