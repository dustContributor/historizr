package io.historizr.device;

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
