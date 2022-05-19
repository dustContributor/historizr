package io.historizr.device;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

import io.historizr.device.db.Db;

public record Config(
		String clientId,
		String broker,
		String db,
		String inputTopic,
		String outputTopic,
		int apiPort,
		boolean hasClientIdUuid,
		boolean hasDebugTopic) {
	public Db toDb() {
		return new Db(this);
	}

	public static Config read() {
		return read("config.json");
	}

	public static Config read(String path) {
		try {
			return OpsJson.reader().readValue(Path.of(path).toFile(), Config.class);
		} catch (IOException e1) {
			throw new UncheckedIOException(e1);
		}
	}
}