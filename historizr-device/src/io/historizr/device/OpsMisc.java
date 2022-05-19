package io.historizr.device;

import io.vertx.core.AsyncResult;
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
}
