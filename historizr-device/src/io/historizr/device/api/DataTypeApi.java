package io.historizr.device.api;

import static io.historizr.device.OpsReq.failed;

import java.util.stream.Collectors;

import io.historizr.device.db.DataType;
import io.historizr.device.db.Db;
import io.vertx.core.eventbus.EventBus;
import io.vertx.ext.web.Router;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.SqlClient;

public final class DataTypeApi {
	private DataTypeApi() {
		throw new RuntimeException();
	}

	private static final String ROUTE = "/datatype";

	public static Router register(EventBus bus, Router router, SqlClient conn) {
		var toModels = Collectors.mapping((Row r) -> DataType.of(r), Collectors.toList());
		var modelType = DataType.class;
		router.get(ROUTE).handler(ctx -> {
			conn.query(Db.Sql.QUERY_DATA_TYPE)
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
