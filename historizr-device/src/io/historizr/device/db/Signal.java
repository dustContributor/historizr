package io.historizr.device.db;

import java.sql.ResultSet;
import java.sql.SQLException;

public record Signal(long id, int dataTypeId, String name, String topic, boolean isOnChange) {
	public static Signal of(ResultSet rs) throws SQLException {
		return new Signal(rs.getLong(1), rs.getInt(2), rs.getString(3), rs.getString(4), rs.getBoolean(5));
	}
}