package io.historizr.server.view;

import static io.historizr.server.OpsReq.failed;

import java.util.Map;
import java.util.function.IntFunction;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.historizr.server.OpsMisc;
import io.historizr.server.OpsReq;
import io.historizr.server.db.DataType;
import io.historizr.server.db.Db;
import io.historizr.server.db.Device;
import io.historizr.server.db.DeviceType;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
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
		var toDeviceTypes = Collectors.mapping(DeviceType::of, Collectors.toList());
		var modelType = Device.class;
		router.get(ROUTE).handler(ctx -> {
			var ftDeviceTypes = conn.query(Db.DeviceType.QUERY)
					.collecting(toDeviceTypes)
					.execute()
					.onFailure(r -> {
						var msg = "Failed to query device types";
						LOGGER.log(Level.WARNING, msg, r);
						ctx.fail(r);
					});
			var ftDevices = conn.query(Db.Device.QUERY)
					.collecting(toModels)
					.execute()
					.onFailure(r -> {
						var msg = "Failed to query devices";
						LOGGER.log(Level.WARNING, msg, r);
						ctx.fail(r);
					});
			CompositeFuture.all(ftDeviceTypes, ftDevices)
					.compose(r -> {
						var deviceTypesById = ftDeviceTypes.result().value()
								.stream()
								.collect(Collectors.toMap(v -> v.id(), v -> v));
						var devices = ftDevices.result().value();
						return tmpl.render(
								Map.of("items", devices, "deviceTypesById", deviceTypesById),
								"res/device");
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
