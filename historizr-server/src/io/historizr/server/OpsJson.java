package io.historizr.server;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

	public static final String toString(Object o) {
		try {
			return JSON.writeValueAsString(o);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	public static final <T> T fromString(String o, Class<T> t) {
		try {
			return JSON.readValue(o, t);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	public static final <T> T fromBytes(byte[] o, Class<T> t) {
		try {
			return JSON.readValue(o, t);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static final byte[] toBytes(Object o) {
		try {
			return JSON.writeValueAsBytes(o);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	public static final ObjectNode toObjectNode(Object o) {
		return JSON.convertValue(o, ObjectNode.class);
	}
}
