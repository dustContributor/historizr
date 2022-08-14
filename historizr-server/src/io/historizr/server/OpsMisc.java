package io.historizr.server;

import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import io.vertx.core.AsyncResult;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.json.JsonObject;
import io.vertx.core.tracing.TracingPolicy;

public final class OpsMisc {
	private OpsMisc() {
		throw new RuntimeException();
	}

	public static final String className() {
		return StackWalker
				.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
				.getCallerClass()
				.getName();
	}

	public static final Logger classLogger() {
		var name = StackWalker
				.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
				.getCallerClass()
				.getName();
		return Logger.getLogger(name);
	}

	public static boolean isNullOrEmpty(String v) {
		return v == null || v.isEmpty();
	}

	public static <T, I extends Iterable<T>> Stream<T> stream(AsyncResult<I> res) {
		return stream(res, false);
	}

	public static <T, I extends Iterable<T>> Stream<T> stream(AsyncResult<I> res, boolean parallel) {
		return stream(res.result(), parallel);
	}

	public static <T> Stream<T> stream(Iterable<T> it) {
		return stream(it, false);
	}

	public static <T> Stream<T> stream(Iterable<T> it, boolean parallel) {
		return StreamSupport.stream(it.spliterator(), parallel);
	}

	/**
	 * Passthrough codec that returns the object to send over the event bus as-is.
	 */
	public static final class PassthroughCodec implements MessageCodec<Object, Object> {
		protected PassthroughCodec() {
			// Empty.
		}

		public static final MessageCodec<Object, Object> INSTANCE = new PassthroughCodec();

		@Override
		public final void encodeToWire(Buffer buffer, Object s) {
			throw new UnsupportedOperationException();
		}

		@Override
		public final Object decodeFromWire(int pos, Buffer buffer) {
			throw new UnsupportedOperationException();
		}

		@Override
		public final Object transform(Object s) {
			return s;
		}

		@Override
		public final String name() {
			return PassthroughCodec.class.getName();
		}

		@Override
		public final byte systemCodecID() {
			return -1;
		}
	}

	private static final DeliveryOptions JSON_DELIVERY = new DeliveryOptions()
			.setCodecName(PassthroughCodec.INSTANCE.name())
			.setLocalOnly(true)
			.setTracingPolicy(TracingPolicy.IGNORE);

	public static final EventBus sendJson(EventBus bus, String address, Object obj) {
		return sendJson(bus, address, JsonObject.mapFrom(obj));
	}

	public static final EventBus sendJson(EventBus bus, String address, JsonObject obj) {
		return bus.send(address, obj, JSON_DELIVERY);
	}
}
