package io.historizr.shared;

import java.util.Collection;

import io.vertx.core.AsyncResult;
import io.vertx.ext.web.RoutingContext;

public final class OpsReq {
	private OpsReq() {
		throw new RuntimeException();
	}

	private static final Object OK = new Object() {
		@SuppressWarnings("unused")
		public final String getStatus() {
			return "ok";
		}
	};

	public static final void ok(RoutingContext ctx) {
		ctx.json(OK);
	}

	public static final boolean failed(AsyncResult<?> res, RoutingContext ctx) {
		if (res.succeeded()) {
			return false;
		}
		ctx.fail(res.cause());
		return true;
	}

	public static final boolean notFound(RoutingContext ctx) {
		return notFound(0, ctx);
	}

	public static final boolean notFound(Collection<?> items, RoutingContext ctx) {
		return notFound(items.size(), ctx);
	}

	public static final boolean notFound(int items, RoutingContext ctx) {
		if (items > 0) {
			return false;
		}
		ctx.response().setStatusCode(404).end();
		return true;
	}

}
