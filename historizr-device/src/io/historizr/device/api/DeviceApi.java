package io.historizr.device.api;

import static io.historizr.shared.OpsReq.failed;

import java.util.HashMap;

import io.historizr.device.SampleWorker;
import io.historizr.device.db.Db;
import io.historizr.shared.OpsMisc;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.sqlclient.SqlClient;

public final class DeviceApi {
	private DeviceApi() {
		throw new RuntimeException();
	}

	private static final String ROUTE = "/device";

	public static Router register(Vertx vertx, Router router, SqlClient conn, SampleWorker sampleWorker) {
		router.get(ROUTE).handler(ctx -> {
			conn.query(Db.Misc.DEVICE_STATS)
					.execute(r -> {
						if (failed(r, ctx)) {
							return;
						}
						var rows = r.result().value();

						var res = new HashMap<>();
						res.put("host", OpsMisc.hostName());
						if (rows.size() > 0) {
							var row = rows.iterator().next();
							var revision = row.getLong(0);
							var total = row.getLong(1);
							var onChange = row.getLong(2);
							var deadband = row.getLong(3);
							var fullPayload = row.getLong(4);
							res.put("signalsRevision", revision);
							res.put("signalsTotal", total);
							res.put("signalsWithOnChange", onChange);
							res.put("signalsWithDeadband", deadband);
							res.put("signalsWithFullPayload", fullPayload);
						}
						if (sampleWorker != null) {
							var sample = sampleWorker.sampleRepo();
							res.put("messagesReceived", sample.receivedCount());
							res.put("messagesProcessed", sample.processedCount());
							res.put("messagesSkipped", sample.skippedCount());
							res.put("messagesPublished", sample.publishedCount());
						}
						ctx.json(res);
					});
		});
		return router;
	}
}
