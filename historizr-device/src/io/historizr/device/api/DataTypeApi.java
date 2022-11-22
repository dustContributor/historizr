package io.historizr.device.api;

import static io.historizr.shared.OpsReq.failed;

import java.util.stream.Collectors;

import io.historizr.device.db.Db;
import io.historizr.shared.db.DataType;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.sqlclient.SqlClient;

public final class DataTypeApi {
	private DataTypeApi() {
		throw new RuntimeException();
	}

	private static final String ROUTE = "/datatype";

	public static Router register(Vertx vertx, Router router, SqlClient conn) {
		var toModels = Collectors.mapping(DataType::ofRow, Collectors.toList());
		router.get(ROUTE).handler(ctx -> {
			conn.query(Db.DataType.QUERY)
					.collecting(toModels)
					.execute(r -> {
						if (failed(r, ctx)) {
							return;
						}
						var res = r.result().value();
						ctx.json(res);
					});
		});
		return router;
	}
}
