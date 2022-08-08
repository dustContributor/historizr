package io.historizr.server;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

import io.historizr.server.Config.DbConfig;
import io.vertx.core.json.JsonObject;

public record Config(
		DbConfig db,
		int port) {

	public final JsonObject toJson() {
		return JsonObject.mapFrom(this);
	}

	public static Config read() {
		return read("config.json");
	}

	public static record DbConfig(String host, String user, String pass, String database, int port) {

	}

	public static Config read(String path) {
		try {
			return OpsJson.reader().readValue(Path.of(path).toFile(), Config.class);
		} catch (IOException e1) {
			throw new UncheckedIOException(e1);
		}
	}
}