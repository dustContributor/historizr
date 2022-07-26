package io.historizr.device.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

public record Signal(long id, int dataTypeId, String name, String topic, int deadband, boolean isOnChange,
		boolean hasFullPayload) {
	public static Signal of(Row rs) {
		return new Signal(rs.getLong(0), rs.getInteger(1), rs.getString(2), rs.getString(3), rs.getInteger(4),
				rs.getBoolean(5),
				rs.getBoolean(6));
	}

	public static Signal of(ResultSet rs) {
		try {
			return new Signal(rs.getLong(1), rs.getInt(2), rs.getString(3), rs.getString(4), rs.getInt(5),
					rs.getBoolean(6),
					rs.getBoolean(7));
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public final JsonArray into(JsonArray dest) {
		return into(dest, false);
	}

	public final JsonArray into(JsonArray dest, boolean isIdLast) {
		if (!isIdLast) {
			dest.add(id());
		}
		dest.add(dataTypeId())
				.add(name())
				.add(topic())
				.add(deadband())
				.add(isOnChange());
		if (isIdLast) {
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

	public final Tuple into(Tuple dest) {
		return into(dest, false);
	}

	public final Tuple into(Tuple dest, boolean isIdLast) {
		if (!isIdLast) {
			dest.addLong(id());
		}
		dest.addInteger(dataTypeId())
				.addString(name())
				.addString(topic())
				.addInteger(deadband())
				.addBoolean(isOnChange());
		if (isIdLast) {
			dest.addLong(id());
		}
		return dest;
	}

	public static final Signal empty() {
		return empty(0);
	}

	public static final Signal empty(long id) {
		return new Signal(id, 0, null, null, 0, false, false);
	}
}