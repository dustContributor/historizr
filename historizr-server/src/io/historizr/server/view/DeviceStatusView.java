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
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.common.template.TemplateEngine;

public final class DeviceStatusView {
	private DeviceStatusView() {
		throw new RuntimeException();
	}

	private static final Logger LOGGER = OpsMisc.classLogger();
	private static final String PARAM_DEVICE_ID = "deviceId";

	private static final String ROUTE = "/v/devicestatus";
	private static final String ROUTE_DISCARD_STATS = ROUTE + "/discardstats";
	private static final String ROUTE_DISCARD_STATE = ROUTE + "/discardstate";

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
						var res = r.bodyAsJsonObject().getMap();
						res.put("id", deviceId);
						var stats = res.entrySet().stream().filter(v -> v.getKey().contains("Stats")).findAny();
						Object statsMap = Map.of();
						if (stats.isPresent()) {
							// Flatten the result a bit
							res.remove(stats.get().getKey());
							statsMap = stats.get().getValue();
						}
						return tmpl.render(Map.of("props", res, "stats", statsMap), "res/deviceStatus");
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
		router.get(ROUTE_DISCARD_STATS).handler(
				opHandlerFor(
						client,
						cfg,
						DeviceOpsApi.ROUTE_DISCARD_SAMPLE_STATS,
						"discard sample stats"));
		router.get(ROUTE_DISCARD_STATE).handler(
				opHandlerFor(
						client,
						cfg,
						DeviceOpsApi.ROUTE_DISCARD_SAMPLE_STATE,
						"discard sample state"));
	}

	private static final Handler<RoutingContext> opHandlerFor(
			WebClient client,
			Config cfg,
			String path,
			String failure) {
		return ctx -> {
			var deviceIdPar = ctx.queryParam(PARAM_DEVICE_ID);
			if (notFound(deviceIdPar, ctx)) {
				return;
			}
			var deviceId = OpsMisc.parseLongOrDefault(deviceIdPar.get(0));
			client.post(cfg.port(), "localhost", path)
					.sendJson(new JsonObject().put("id", deviceId))
					.onComplete(h -> {
						var r = h.result();
						// TODO generalize this?
						if (!HttpStatusClass.SUCCESS.contains(r.statusCode())) {
							h = Future.failedFuture("Failed to %s, result %s"
									.formatted(failure, r.statusMessage()));
						}
						if (failed(h, ctx)) {
							var msg = "Failed to %s".formatted(failure);
							LOGGER.log(Level.WARNING, msg, h.cause());
							return;
						}
						ctx.redirect("back");
					});
		};
	}
}
