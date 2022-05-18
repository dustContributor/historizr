package io.historizr.device.db;

import java.sql.ResultSet;
import java.sql.SQLException;

public record DataType(int id, int mappingId, String name) {

	public static DataType of(ResultSet rs) throws SQLException {
		return new DataType(rs.getInt(1), rs.getInt(2), rs.getString(3));
	}

	public enum Catalog {
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
