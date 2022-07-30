package io.historizr.device.db;

public final class Db {
	private Db() {
		throw new RuntimeException();
	}

	public static final class Sql {
		private Sql() {
			throw new RuntimeException();
		}

		private static final String SIGNAL_ALIAS = """
					id,
					id_data_type as dataTypeId,
					name,
					topic,
					deadband,
					is_on_change as isOnChange,
					has_full_payload as hasFullPayload
				""".stripIndent();
		public static final String QUERY_DATA_TYPE = """
				select id, id_mapping as mappingId, name from data_type""".stripIndent();
		public static final String QUERY_SIGNALS = """
				select %s from signal
				""".stripIndent().formatted(SIGNAL_ALIAS);
		public static final String QUERY_SIGNAL = QUERY_SIGNALS + " where id = ?";
		private static final String[] COLUMNS_SIGNAL = { "id_data_type", "name", "topic", "deadband", "is_on_change",
				"has_full_payload" };
		public static final String INSERT_SIGNAL = """
				insert into signal(id, %s)
				values(?, ?, ?, ?, ?, ?, ?)""".formatted(String.join(",", COLUMNS_SIGNAL)).stripIndent();
		public static final String UPDATE_SIGNAL = """
				update signal
				set %s = ?
				where id = ?""".formatted(String.join(" = ?,", COLUMNS_SIGNAL)).stripIndent();
		public static final String DELETE_SIGNAL = """
				delete from signal
				where id = ?""".stripIndent();
	}

}
