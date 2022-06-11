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
		public static final String QUERY_SIGNAL = """
				select %s from signal
				""".stripIndent().formatted(SIGNAL_ALIAS);
		private static final String[] COLUMNS_SIGNAL = { "id_data_type", "name", "topic", "deadband", "is_on_change" };
		public static final String INSERT_SIGNAL = """
				insert into signal(id, %s)
				values(?, ?, ?, ?, ?, ?)
				returning %s""".formatted(String.join(",", COLUMNS_SIGNAL), SIGNAL_ALIAS).stripIndent();
		public static final String UPDATE_SIGNAL = """
				update signal
				set %s = ?
				where id = ?
				returning %s""".formatted(String.join(" = ?,", COLUMNS_SIGNAL), SIGNAL_ALIAS).stripIndent();
		public static final String DELETE_SIGNAL = """
				delete from signal
				where id = ?""".stripIndent();
	}

}
