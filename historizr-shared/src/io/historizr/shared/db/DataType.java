package io.historizr.shared.db;

import java.sql.ResultSet;
import java.sql.SQLException;

import io.vertx.sqlclient.Row;

public record DataType(int id, int mappingId, String name) {

	public static DataType ofRow(Row rs) {
		int i = 0;
		return new DataType(
				rs.getInteger(i++),
				rs.getInteger(i++),
				rs.getString(i++));
	}

	public static DataType ofResultSet(ResultSet rs) {
		try {
			int i = 1;
			return new DataType(
					rs.getInt(i++),
					rs.getInt(i++),
					rs.getString(i++));
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public static DataType of(Row rs) {
		return ofRow(rs);
	}

	public static DataType of(ResultSet rs) {
		return ofResultSet(rs);
	}

	public static enum Catalog {
		UNKNOWN(0),
		BOOL(1),
		I64(9),
		F32(10),
		F64(11),
		STR(12);

		public final int id;

		private static final Catalog[] VALUES = Catalog.values();

		private Catalog(int id) {
			this.id = id;
		}

		public static Catalog of(int id) {
			for (var item : VALUES) {
				if (item.id == id) {
					return item;
				}
			}
			return Catalog.UNKNOWN;
		}

	}
}
