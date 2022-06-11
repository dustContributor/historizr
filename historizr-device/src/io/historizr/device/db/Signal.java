package io.historizr.device.db;

import java.sql.ResultSet;
import java.sql.SQLException;

import io.vertx.core.json.JsonArray;

public record Signal(long id, int dataTypeId, String name, String topic, int deadband, boolean isOnChange,
		boolean hasFullPayload) {
	public static Signal of(ResultSet rs) throws SQLException {
		return new Signal(rs.getLong(1), rs.getInt(2), rs.getString(3), rs.getString(4), rs.getInt(5), rs.getBoolean(6),
				rs.getBoolean(7));
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

	public static final Signal empty() {
		return empty(0);
	}

	public static final Signal empty(long id) {
		return new Signal(id, 0, null, null, 0, false, false);
	}
}