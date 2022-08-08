package io.historizr.server.db;

import io.vertx.sqlclient.Row;

public record DataType(int id, int mappingId, String name) {

	public static DataType of(Row rs) {
		return new DataType(rs.getInteger(1), rs.getInteger(2), rs.getString(3));
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
