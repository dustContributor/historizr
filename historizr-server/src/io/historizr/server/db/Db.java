package io.historizr.server.db;

public final class Db {
	private Db() {
		throw new RuntimeException();
	}

	public static final class Sql {
		private Sql() {
			throw new RuntimeException();
		}

		private static final CharSequence argList(int count) {
			var b = new StringBuilder();
			for (int i = 0; i < count; ++i) {
				if (b.length() > 0) {
					b.append(',');
				}
				b.append('$').append(i + 1);
			}
			return b;
		}

		private static final String select(Object... fmts) {
			return sql("select %s from %s", fmts);
		}

		private static final String sql(String base, Object... fmts) {
			return base.formatted(fmts).stripIndent();
		}

		private static final String TABLE_SIGNAL = "signal";
		private static final String TABLE_DATA_TYPE = "data_type";

		private static final String ALIAS_SIGNAL = """
					id,
					id_device as deviceId,
					id_data_type as dataTypeId,
					name,
					topic,
					deadband,
					is_on_change as isOnChange,
					has_full_payload as hasFullPayload
				""".stripIndent();

		private static final String ALIAS_DATA_TYPE = """
					id, id_mapping as mappingId, name
				""".stripIndent();
		public static final String QUERY_DATA_TYPE = select(ALIAS_DATA_TYPE, TABLE_DATA_TYPE);
		public static final String QUERY_SIGNAL = select(ALIAS_SIGNAL, TABLE_SIGNAL);
		private static final String[] COLUMNS_SIGNAL = { "id_device", "id_data_type", "name", "topic", "deadband",
				"is_on_change",
				"has_full_payload" };
		public static final String INSERT_SIGNAL = sql("""
				insert into %s(%s)
				values(%s)
				returning %s""", TABLE_SIGNAL, String.join(", ", COLUMNS_SIGNAL), argList(COLUMNS_SIGNAL.length),
				ALIAS_SIGNAL);
		public static final String UPDATE_SIGNAL = sql("""
				update %s
				set %s = ?
				where id = ?
				returning %s""", TABLE_SIGNAL, String.join(" = ?, ", COLUMNS_SIGNAL), ALIAS_SIGNAL);
		public static final String DELETE_SIGNAL = sql("""
				delete from %s
				where id = $1""", TABLE_SIGNAL);
	}

}
