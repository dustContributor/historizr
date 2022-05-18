package io.historizr.device;

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
}