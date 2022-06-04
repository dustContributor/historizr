package io.historizr.device.api;

import static io.historizr.device.OpsReq.failed;

import io.historizr.device.db.Db;
import io.vertx.core.eventbus.EventBus;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.web.Router;

public class DataType {
	private DataType() {
		throw new RuntimeException();
	}

	private static final String ROUTE = "/datatype";

	public static Router register(EventBus bus, Router router, JDBCClient conn) {
		router.get(ROUTE)
				.handler(ctx -> {
					conn.query(Db.Sql.QUERY_DATA_TYPE, r -> {
						if (failed(r, ctx)) {
							return;
						}
						var res = r.result();
						ctx.json(res.getRows());
					});
				});
		return router;
	}
}
