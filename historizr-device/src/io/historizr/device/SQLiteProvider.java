package io.historizr.device;

import java.sql.SQLException;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.spi.DataSourceProvider;

public final class SQLiteProvider implements DataSourceProvider {
	private final static Logger LOGGER = Logger.getLogger(SQLiteProvider.class.getName());

	@Override
	public final int maximumPoolSize(DataSource dataSource, JsonObject config) throws SQLException {
		return 1;
	}

	private static final SQLiteConfig DEFAULT_CONFIG;
	static {
		var tmp = new SQLiteConfig();
		// Very important to avoid trash data in the signal table.
		tmp.enforceForeignKeys(true);
		DEFAULT_CONFIG = tmp;
	}

	@Override
	public final DataSource getDataSource(JsonObject config) throws SQLException {
		var cfg = (Config) config.getValue("cfg");
		var dataSource = new SQLiteDataSource(DEFAULT_CONFIG);
		dataSource.setUrl(cfg.db());
		return dataSource;
	}

	@Override
	public final void close(DataSource dataSource) throws SQLException {
		if (dataSource instanceof AutoCloseable closeable) {
			try {
				closeable.close();
			} catch (SQLException e) {
				throw e;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
}
