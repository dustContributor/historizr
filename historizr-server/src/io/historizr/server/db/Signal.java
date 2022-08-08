package io.historizr.server.db;

import java.util.Map;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

public record Signal(long id, long deviceId, int dataTypeId, String name, String topic, int deadband,
		boolean isOnChange,
		boolean hasFullPayload) {
	public static Signal of(Row rs) {
		int i = 0;
		return new Signal(
				rs.getLong(i++),
				rs.getLong(i++),
				rs.getInteger(i++),
				rs.getString(i++),
				rs.getString(i++),
				rs.getInteger(i++),
				rs.getBoolean(i++),
				rs.getBoolean(i++));
	}

	public final JsonArray into(JsonArray dest, MappingOp behavior) {
		if (behavior == MappingOp.ID_FIRST) {
			dest.add(id());
		}
		dest.add(deviceId())
				.add(dataTypeId())
				.add(name())
				.add(topic())
				.add(deadband())
				.add(isOnChange())
				.add(hasFullPayload());
		if (behavior == MappingOp.ID_LAST) {
			dest.add(id());
		}
		return dest;
	}

	@SuppressWarnings("unchecked")
	public final JsonObject into(JsonObject dest) {
		Json.CODEC.fromValue(this, Map.class).forEach((k, v) -> {
			dest.put(k.toString(), v);
		});
		return dest;
	}

	public final Tuple into(Tuple dest, MappingOp behavior) {
		if (behavior == MappingOp.ID_FIRST) {
			dest.addLong(id());
		}
		dest.addLong(deviceId())
				.addInteger(dataTypeId())
				.addString(name())
				.addString(topic())
				.addInteger(deadband())
				.addBoolean(isOnChange())
				.addBoolean(hasFullPayload());
		if (behavior == MappingOp.ID_LAST) {
			dest.addLong(id());
		}
		return dest;
	}

	public static final Signal empty() {
		return empty(0);
	}

	public static final Signal empty(long id) {
		return new Signal(id, 0, 0, null, null, 0, false, false);
	}
}