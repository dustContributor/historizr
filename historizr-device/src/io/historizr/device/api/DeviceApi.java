package io.historizr.device.api;

import static io.historizr.device.OpsReq.failed;
import static io.historizr.device.OpsReq.notFound;

import java.util.HashMap;

import io.historizr.device.OpsMisc;
import io.historizr.device.db.Db;
import io.vertx.core.eventbus.EventBus;
import io.vertx.ext.web.Router;
import io.vertx.sqlclient.SqlClient;

public final class DeviceApi {
	private DeviceApi() {
		throw new RuntimeException();
	}

	private static final String ROUTE = "/device";

	public static Router register(EventBus bus, Router router, SqlClient conn) {
		router.get(ROUTE).handler(ctx -> {
			conn.query(Db.Misc.REVISION_SUM)
					.execute(r -> {
						if (failed(r, ctx)) {
							return;
						}
						var rows = r.result().value();
						if (notFound(rows.size(), ctx)) {
							return;
						}
						var row = rows.iterator().next();
						var revision = row.getLong(0);
						var res = new HashMap<>();
						res.put("revision", revision);
						res.put("host", OpsMisc.hostName());
						ctx.json(res);
					});
		});
		return router;
	}
}
