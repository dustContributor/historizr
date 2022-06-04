package io.historizr.device;

import io.vertx.core.AsyncResult;
import io.vertx.ext.web.RoutingContext;

public final class OpsReq {
	private OpsReq() {
		throw new RuntimeException();
	}

	public static final boolean failed(AsyncResult<?> res, RoutingContext ctx) {
		if (res.succeeded()) {
			return false;
		}
		ctx.fail(res.cause());
		return true;
	}

	public static final boolean notFound(RoutingContext ctx) {
		return OpsReq.notFound(0, ctx);
	}

	public static final boolean notFound(int items, RoutingContext ctx) {
		if (items > 0) {
			return false;
		}
		ctx.response().setStatusCode(404).end();
		return true;
	}

}
