package io.historizr.device;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class OpsJson {
	private OpsJson() {
		throw new RuntimeException();
	}

	private static final ObjectMapper JSON = new ObjectMapper()
			.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
			.registerModule(new JavaTimeModule());

	public static final ObjectWriter writer() {
		return JSON.writer();
	}

	public static final ObjectReader reader() {
		return JSON.reader();
	}
}
