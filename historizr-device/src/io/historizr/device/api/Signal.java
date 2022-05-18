package io.historizr.device.api;

import static io.historizr.device.OpsMisc.hasFailed;

import io.historizr.device.db.Db;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.web.Router;

public final class Signal {
	private Signal() {
		throw new RuntimeException();
	}

	private static final String ROUTE = "/signal";

	public static Router register(Router router, JDBCClient conn) {
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
					var entity = ctx.body().asPojo(io.historizr.device.db.Signal.class);
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
						var res = r.result();
						ctx.json(res.getRows());
					});
				});
		router.put(ROUTE)
				.handler(ctx -> {
					var entity = ctx.body().asPojo(io.historizr.device.db.Signal.class);
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
						var res = r.result();
						ctx.json(res.getRows());
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
