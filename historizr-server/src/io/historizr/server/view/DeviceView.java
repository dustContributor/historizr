package io.historizr.server.view;

import static io.historizr.server.OpsReq.failed;

import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.historizr.server.OpsMisc;
import io.historizr.server.db.Db;
import io.historizr.server.db.Device;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.common.template.TemplateEngine;
import io.vertx.sqlclient.SqlClient;

public final class DeviceView {
	private DeviceView() {
		throw new RuntimeException();
	}

	private static final Logger LOGGER = OpsMisc.classLogger();

	private static final String ROUTE = "/v/device";

	public static void register(Vertx vertx, Router router, TemplateEngine tmpl, SqlClient conn) {
		var toModels = Collectors.mapping(Device::of, Collectors.toList());
		var modelType = Device.class;
		router.get(ROUTE).handler(ctx -> {
			conn.query(Db.Device.QUERY)
					.collecting(toModels)
					.execute(r -> {
						if (failed(r, ctx)) {
							return;
						}
						var res = r.result().value();
						tmpl.render(Map.of("items", res), "res/device")
								.onComplete(h -> {
									if (failed(h, ctx)) {
										return;
									}
									ctx.end(h.result());
								});
					});
		});
	}
}
