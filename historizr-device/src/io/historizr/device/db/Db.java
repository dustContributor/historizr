package io.historizr.device.db;

import java.util.stream.Stream;

public final class Db {
	private Db() {
		throw new RuntimeException();
	}

	public static final class Misc {
		private Misc() {
			throw new RuntimeException();
		}

		public static final String REVISION_SUM = "select sum(revision) from signal";
	}

	public static final class Signal {
		private Signal() {
			throw new RuntimeException();
		}

		private static final String TBL = "signal";
		private static final String[] COL = {
				"id_data_type",
				"name",
				"topic",
				"deadband",
				"is_on_change",
				"has_full_payload",
				"revision"
		};
		private static final String[] COL_ALL = Stream.concat(Stream.of("id"), Stream.of(COL))
				.toArray(String[]::new);
		public static final String QUERY = select(TBL, COL_ALL);
		public static final String QUERY_BY_ID = QUERY + " where id = $1";
		public static final String INSERT = sql("""
				insert into %s(%s)
				values(?, ?, ?, ?, ?, ?, ?, ?)""", TBL, String.join(",", COL_ALL));
		public static final String UPDATE = sql("""
				update %s
				set %s = ?
				where id = ?""", TBL, String.join(" = ?,", COL));
		public static final String DELETE = sql("""
				delete from %s
				where id = ?""", TBL);
	}

	public static final class DataType {
		private DataType() {
			throw new RuntimeException();
		}

		private static final String TBL = "data_type";
		private static final String[] COL = { "id_mapping", "name" };
		private static final String[] COL_ALL = Stream.concat(Stream.of("id"), Stream.of(COL))
				.toArray(String[]::new);

		public static final String QUERY = select(TBL, COL_ALL);
	}

	private static final String select(String tbl, String[] allCols) {
		return sql("select %s from %s", String.join(",", allCols), tbl);
	}

	private static final String sql(String base, Object... fmts) {
		return base.formatted(fmts).stripIndent();
	}

}
