package io.historizr.device;

import java.sql.SQLException;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.spi.DataSourceProvider;

public final class H2Provider implements DataSourceProvider {
	private final static Logger LOGGER = Logger.getLogger(H2Provider.class.getName());

	private final JsonObject initialConfig = new JsonObject();

	public H2Provider config(String key, Object value) {
		initialConfig.put(key, value);
		return this;
	}

	@Override
	public final JsonObject getInitialConfig() {
		return new JsonObject().mergeIn(initialConfig);
	}

	@Override
	public final int maximumPoolSize(DataSource dataSource, JsonObject config) throws SQLException {
		return 1;
	}

	@Override
	public final DataSource getDataSource(JsonObject config) throws SQLException {
		var cfg = (Config) config.getValue("cfg");
		var dataSource = new JdbcDataSource();
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
