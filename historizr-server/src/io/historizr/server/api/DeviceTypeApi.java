package io.historizr.server.api;

import static io.historizr.server.OpsReq.failed;
import static io.historizr.server.OpsReq.notFound;

import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.historizr.server.OpsMisc;
import io.historizr.server.db.Db;
import io.historizr.server.db.DeviceType;
import io.historizr.server.db.MappingOp;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;

public final class DeviceTypeApi {
	private DeviceTypeApi() {
		throw new RuntimeException();
	}

	private static final Logger LOGGER = OpsMisc.classLogger();

	private static final String ROUTE = "/devicetype";

	public static Router register(Vertx vertx, Router router, SqlClient conn) {
		var toModels = Collectors.mapping(DeviceType::of, Collectors.toList());
		var modelType = DeviceType.class;
		router.get(ROUTE).handler(ctx -> {
			conn.query(Db.DeviceType.QUERY)
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
			conn.preparedQuery(Db.DeviceType.INSERT)
					.collecting(toModels)
					.execute(pars, r -> {
						if (failed(r, ctx)) {
							return;
						}
						var res = r.result().value().get(0);
						LOGGER.fine(() -> "POST " + res);
						ctx.json(res);
					});
		});
		router.put(ROUTE).handler(ctx -> {
			var entity = ctx.body().asPojo(modelType);
			var pars = entity.tuple(MappingOp.ID_LAST);
			conn.preparedQuery(Db.DeviceType.UPDATE)
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
						ctx.json(res);
					});
		});
		router.delete(ROUTE).handler(ctx -> {
			var entity = ctx.body().asPojo(modelType);
			var pars = Tuple.of(entity.id());
			conn.preparedQuery(Db.DeviceType.DELETE)
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
						ctx.json(res);
					});
		});
		return router;
	}
}
