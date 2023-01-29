package io.historizr.server.view;

import static io.historizr.shared.OpsReq.failed;
import static io.historizr.shared.OpsReq.notFound;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.historizr.server.Config;
import io.historizr.server.api.DeviceOpsApi;
import io.historizr.shared.OpsMisc;
import io.netty.handler.codec.http.HttpStatusClass;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.common.template.TemplateEngine;

public final class DeviceStatusView {
	private DeviceStatusView() {
		throw new RuntimeException();
	}

	private static final Logger LOGGER = OpsMisc.classLogger();
	private static final String PARAM_DEVICE_ID = "deviceId";

	private static final String ROUTE = "/v/devicestatus";

	public static void register(Vertx vertx, Router router, TemplateEngine tmpl, Config cfg) {
		var client = WebClient.create(vertx);
		router.get(ROUTE).handler(ctx -> {
			var deviceIdPar = ctx.queryParam(PARAM_DEVICE_ID);
			if (notFound(deviceIdPar, ctx)) {
				return;
			}
			var deviceId = OpsMisc.parseLongOrDefault(deviceIdPar.get(0));
			client.post(cfg.port(), "localhost", DeviceOpsApi.ROUTE_STATUS)
					.sendJson(new JsonObject().put("id", deviceId))
					.compose(r -> {
						if (!HttpStatusClass.SUCCESS.contains(r.statusCode())) {
							var msg = "Failed to read status, result %s"
									.formatted(r.statusMessage());
							throw new RuntimeException(msg);
						}
						var res = r.bodyAsJsonObject();
						return tmpl.render(Map.of("props", res.getMap()), "res/deviceStatus");
					})
					.onComplete(h -> {
						if (failed(h, ctx)) {
							var msg = "Failed to read status";
							LOGGER.log(Level.WARNING, msg, h.cause());
							return;
						}
						ctx.end(h.result());
					});
		});
	}
}
