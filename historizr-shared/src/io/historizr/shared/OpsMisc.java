package io.historizr.shared;

import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
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

	public static final String hostName() {
		try {
			return java.net.InetAddress.getLocalHost().getHostName();
		} catch (Exception e) {
			return "unknown";
		}
	}

	public static boolean isNullOrEmpty(String v) {
		return v == null || v.isEmpty();
	}

	public static Long tryParseLong(String v) {
		if (v == null || v.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(v);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public static long parseLongOrDefault(String v, long def) {
		if (v == null || v.isEmpty()) {
			return def;
		}
		try {
			return Long.parseLong(v);
		} catch (NumberFormatException e) {
			return def;
		}
	}

	public static long parseLongOrDefault(String v) {
		return parseLongOrDefault(v, 0);
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

	public static final <T> EventBus send(EventBus bus, String address, T obj) {
		return bus.send(address, obj, JSON_DELIVERY);
	}

	public static final <T, R> Future<Message<R>> request(EventBus bus, String address, T obj) {
		return bus.request(address, obj, JSON_DELIVERY);
	}
}
