package io.historizr.server.db;

import io.vertx.sqlclient.Row;

public record DataType(
		int id,
		int mappingId,
		String name) {

	public static DataType of(Row rs) {
		int i = 0;
		return new DataType(
				rs.getInteger(i++),
				rs.getInteger(i++),
				rs.getString(i++));
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
