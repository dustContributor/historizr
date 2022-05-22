package io.historizr.device;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.historizr.device.db.DataType;
import io.historizr.device.db.Db;
import io.historizr.device.db.Signal;

public final class SignalRepo {
	private final static Logger LOGGER = Logger.getLogger(SignalRepo.class.getName());
	private final Object signalsLock = new Object();
	private final Config cfg;
	private Map<String, Signal> signalsByTopic;
	private Map<Long, Signal> signalsById;
	private Map<Integer, DataType> dataTypesById;

	public SignalRepo(Config cfg) {
		this.cfg = Objects.requireNonNull(cfg);
	}

	public final SignalRepo init() {
		LOGGER.info("Initializing...");
		var signals = new ArrayList<Signal>();
		var dataTypes = new ArrayList<DataType>();
		LOGGER.info("Querying data...");
		try (var db = cfg.toDb();
				var conn = db.connect().conn();
				var sSt = db.prepare(Db.Sql.QUERY_SIGNAL);
				var dtSt = db.prepare(Db.Sql.QUERY_DATA_TYPE);) {
			var sRs = sSt.executeQuery();
			while (sRs.next()) {
				var row = Signal.of(sRs);
				signals.add(row);
			}
			var dtRs = dtSt.executeQuery();
			while (dtRs.next()) {
				var row = DataType.of(dtRs);
				dataTypes.add(row);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		LOGGER.info("Data queried!");

		signalsByTopic = signals.stream().collect(Collectors.toMap(k -> k.topic(), v -> v));
		signalsById = signals.stream().collect(Collectors.toMap(k -> k.id(), v -> v));
		dataTypesById = dataTypes.stream().collect(Collectors.toMap(k -> k.id(), v -> v));
		LOGGER.info("Initialized!");

		return this;
	}

	public final void updateSignal(Signal signal) {
		LOGGER.fine(() -> "Updating signal: " + signal);
		var id = Long.valueOf(signal.id());
		synchronized (signalsLock) {
			var old = signalsById.get(id);
			if (old != null && !Objects.equals(old.topic(), signal.topic())) {
				// Topic got updated so we remove it by the old stale topic key.
				signalsByTopic.remove(old.topic());
			}
			// These will either insert or update the existing entries.
			signalsById.put(id, signal);
			signalsByTopic.put(signal.topic(), signal);
		}
	}

	public final boolean removeSignal(Signal signal) {
		LOGGER.fine(() -> "Removing signal: " + signal);
		var id = Long.valueOf(signal.id());
		var isRemoved = false;
		synchronized (signalsLock) {
			var old = signalsById.remove(id);
			isRemoved = old != null;
			if (isRemoved) {
				signalsByTopic.remove(old.topic());
			}
		}
		return isRemoved;
	}

	public final Signal signalByTopic(String topic) {
		synchronized (signalsLock) {
			return signalsByTopic.get(topic);
		}
	}

	public final Signal signalById(long id) {
		synchronized (signalsLock) {
			return signalsById.get(Long.valueOf(id));
		}
	}

	public final DataType dataTypeById(int id) {
		return dataTypesById.get(Integer.valueOf(id));
	}

}
