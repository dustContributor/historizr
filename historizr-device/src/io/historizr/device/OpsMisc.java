package io.historizr.device;

import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.impl.CodecManager;
import io.vertx.core.json.JsonObject;
import io.vertx.core.tracing.TracingPolicy;
import io.vertx.ext.web.RoutingContext;

public final class OpsMisc {
	private OpsMisc() {
		throw new RuntimeException();
	}

	public static boolean isNullOrEmpty(String v) {
		return v == null || v.isEmpty();
	}

	public static final boolean hasFailed(AsyncResult<?> res, RoutingContext ctx) {
		if (res.succeeded()) {
			return false;
		}
		ctx.fail(res.cause());
		return true;
	}

	private static final DeliveryOptions JSON_DELIVERY = new DeliveryOptions()
			.setCodecName(CodecManager.JSON_OBJECT_MESSAGE_CODEC.name())
			.setTracingPolicy(TracingPolicy.IGNORE);

	public static final EventBus sendJson(EventBus bus, String address, Object obj) {
		return sendJson(bus, address, JsonObject.mapFrom(obj));
	}

	public static final EventBus sendJson(EventBus bus, String address, JsonObject obj) {
		return bus.send(address, obj, JSON_DELIVERY);
	}
}
