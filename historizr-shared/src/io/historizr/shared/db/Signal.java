package io.historizr.shared.db;

import java.sql.ResultSet;
import java.sql.SQLException;

import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

public record Signal(
		long id,
		long deviceId,
		int dataTypeId,
		String name,
		String topic,
		int deadband,
		boolean isOnChange,
		boolean hasFullPayload,
		long revision) {
	public static Signal ofRow(Row rs) {
		int i = 0;
		return new Signal(
				rs.getLong(i++),
				rs.getLong(i++),
				rs.getInteger(i++),
				rs.getString(i++),
				rs.getString(i++),
				rs.getInteger(i++),
				rs.getBoolean(i++),
				rs.getBoolean(i++),
				rs.getLong(i++));
	}

	public static Signal ofResultSet(ResultSet rs) {
		try {
			int i = 1;
			return new Signal(
					rs.getLong(i++),
					rs.getLong(i++),
					rs.getInt(i++),
					rs.getString(i++),
					rs.getString(i++),
					rs.getInt(i++),
					rs.getBoolean(i++),
					rs.getBoolean(i++),
					rs.getLong(i++));
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public static Signal of(Row rs) {
		return ofRow(rs);
	}

	public static Signal of(ResultSet rs) {
		return ofResultSet(rs);
	}

	public final Tuple tuple(MappingOp behavior) {
		return into(Tuple.tuple(), behavior);
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
				.addBoolean(hasFullPayload())
				.addLong(revision());
		if (behavior == MappingOp.ID_LAST) {
			dest.addLong(id());
		}
		return dest;
	}

	public static final Signal empty() {
		return empty(0);
	}

	public static final Signal empty(long id) {
		return new Signal(id, 0, 0, null, null, 0, false, false, 0);
	}
}