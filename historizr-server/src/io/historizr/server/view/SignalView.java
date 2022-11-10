package io.historizr.server.view;

import static io.historizr.shared.OpsReq.failed;
import static io.historizr.shared.OpsReq.notFound;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.historizr.server.db.Db;
import io.historizr.shared.OpsMisc;
import io.historizr.shared.db.DataType;
import io.historizr.shared.db.Signal;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.common.template.TemplateEngine;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;

public final class SignalView {
	private SignalView() {
		throw new RuntimeException();
	}

	private static final Logger LOGGER = OpsMisc.classLogger();

	private static final String PARAM_DEVICE_ID = "deviceId";
	private static final String PARAM_DEVICE_NAME = "deviceName";

	private static final String ROUTE = "/v/signal";

	public static void register(Vertx vertx, Router router, TemplateEngine tmpl, SqlClient conn) {
		var toModels = Collectors.mapping(Signal::ofRow, Collectors.toList());
		var toDataTypes = Collectors.mapping(DataType::ofRow, Collectors.toList());
		router.get(ROUTE).handler(ctx -> {
			var deviceIdPar = ctx.queryParam(PARAM_DEVICE_ID);
			if (notFound(deviceIdPar, ctx)) {
				return;
			}
			var deviceId = OpsMisc.parseLongOrDefault(deviceIdPar.get(0));
			var ftDataTypes = conn.query(Db.DataType.QUERY)
					.collecting(toDataTypes)
					.execute()
					.onFailure(r -> {
						var msg = "Failed to query data types";
						LOGGER.log(Level.WARNING, msg, r);
						ctx.fail(r);
					});
			var ftSignals = conn.preparedQuery(Db.Signal.QUERY_BY_DEVICE_ID)
					.collecting(toModels)
					.execute(Tuple.of(deviceId))
					.onFailure(r -> {
						var msg = "Failed to query signals";
						LOGGER.log(Level.WARNING, msg, r);
						ctx.fail(r);
					});
			CompositeFuture.all(ftDataTypes, ftSignals)
					.compose(r -> {
						var dataTypesById = ftDataTypes.result().value()
								.stream()
								.collect(Collectors.toMap(v -> v.id(), v -> v));
						var devices = ftSignals.result().value();
						return tmpl.render(
								Map.of("items", devices, "dataTypesById", dataTypesById),
								"res/signal");
					})
					.onComplete(h -> {
						if (failed(h, ctx)) {
							return;
						}
						ctx.end(h.result());
					});
		});
	}
}
