package io.historizr.device.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import io.historizr.device.Config;

public final class Db implements AutoCloseable {
	private final Config cfg;
	private Connection conn;

	public static final class Sql {
		private Sql() {
			throw new RuntimeException();
		}

		private static final String SIGNAL_ALIAS = """
					id,
					id_data_type as dataTypeId,
					name,
					topic,
					is_on_change as isOnChange
				""".stripIndent();
		public static final String QUERY_DATA_TYPE = """
				select id, id_mapping as mappingId, name from data_type""".stripIndent();
		public static final String QUERY_SIGNAL = """
				select %s from signal
				""".stripIndent().formatted(SIGNAL_ALIAS);
		public static final String INSERT_SIGNAL = """
				insert into signal(
					id,
					id_data_type,
					name,
					topic,
					is_on_change)
				values(?, ?, ?, ?, ?)
				returning %s""".formatted(SIGNAL_ALIAS).stripIndent();
		public static final String UPDATE_SIGNAL = """
				update signal
				set
					id_data_type = ?,
					name = ?,
					topic = ?,
					is_on_change = ?
				where id = ?
				returning %s""".formatted(SIGNAL_ALIAS).stripIndent();
		public static final String DELETE_SIGNAL = """
				delete from signal
				where id = ?""".stripIndent();
	}

	public Db(Config cfg) {
		super();
		this.cfg = cfg;
	}

	public Db connect() throws SQLException {
		conn = DriverManager.getConnection(cfg.db());
		return this;
	}

	public PreparedStatement prepare(String sql) throws SQLException {
		return conn.prepareStatement(sql);
	}

	public Connection conn() {
		return conn;
	}

	@Override
	public void close() throws Exception {
		if (conn != null) {
			conn.close();
		}
	}
}
