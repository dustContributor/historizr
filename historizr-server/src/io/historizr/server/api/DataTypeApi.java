package io.historizr.server.api;

import static io.historizr.server.OpsReq.failed;

import io.historizr.server.OpsMisc;
import io.historizr.server.db.Db;
import io.vertx.core.eventbus.EventBus;
import io.vertx.ext.web.Router;
import io.vertx.sqlclient.SqlClient;

public class DataTypeApi {
	private DataTypeApi() {
		throw new RuntimeException();
	}

	private static final String ROUTE = "/datatype";

	public static Router register(EventBus bus, Router router, SqlClient conn) {
		router.get(ROUTE).handler(ctx -> {
			conn.query(Db.Sql.QUERY_DATA_TYPE).execute(r -> {
				if (failed(r, ctx)) {
					return;
				}

				var res = OpsMisc.stream(r)
						.map(io.historizr.server.db.DataType::of)
						.toList();
				ctx.json(res);
			});
		});
		return router;
	}
}
