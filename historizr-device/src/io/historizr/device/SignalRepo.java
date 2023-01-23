package io.historizr.device;

import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.historizr.device.db.Db;
import io.historizr.shared.OpsMisc;
import io.historizr.shared.db.DataType;
import io.historizr.shared.db.Signal;

public final class SignalRepo {
	private static final Logger LOGGER = OpsMisc.classLogger();
	private final ReentrantReadWriteLock signalsLock = new ReentrantReadWriteLock();
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
		try (var db = DriverManager.getConnection(cfg.db());
				var sSt = db.createStatement();
				var dtSt = db.createStatement();) {
			var sRs = sSt.executeQuery(Db.Signal.QUERY);
			while (sRs.next()) {
				var row = Signal.of(sRs);
				signals.add(row);
			}
			var dtRs = dtSt.executeQuery(Db.DataType.QUERY);
			while (dtRs.next()) {
				var row = DataType.of(dtRs);
				dataTypes.add(row);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		LOGGER.info("Data queried!");

		try {
			signalsLock.writeLock().lock();
			signalsByTopic = signals.stream().collect(Collectors.toMap(k -> k.topic(), v -> v));
			signalsById = signals.stream().collect(Collectors.toMap(k -> k.id(), v -> v));
		} finally {
			signalsLock.writeLock().unlock();
		}
		dataTypesById = dataTypes.stream().collect(Collectors.toMap(k -> k.id(), v -> v));
		LOGGER.info("Initialized!");

		return this;
	}

	public final void updateSignal(Signal signal) {
		LOGGER.fine(() -> "Updating signal: " + signal);
		var id = Long.valueOf(signal.id());
		try {
			signalsLock.writeLock().lock();
			var old = signalsById.get(id);
			if (old != null && !Objects.equals(old.topic(), signal.topic())) {
				// Topic got updated so we remove it by the old stale topic key.
				signalsByTopic.remove(old.topic());
			}
			// These will either insert or update the existing entries.
			signalsById.put(id, signal);
			signalsByTopic.put(signal.topic(), signal);
		} finally {
			signalsLock.writeLock().unlock();
		}
	}

	public final boolean removeSignal(Signal signal) {
		LOGGER.fine(() -> "Removing signal: " + signal);
		var id = Long.valueOf(signal.id());
		var isRemoved = false;
		try {
			signalsLock.writeLock().lock();
			var old = signalsById.remove(id);
			isRemoved = old != null;
			if (isRemoved) {
				signalsByTopic.remove(old.topic());
			}
		} finally {
			signalsLock.writeLock().unlock();
		}
		return isRemoved;
	}

	public final Signal signalByTopic(String topic) {
		try {
			signalsLock.readLock().lock();
			return signalsByTopic.get(topic);
		} finally {
			signalsLock.readLock().unlock();
		}
	}

	public final Signal signalById(long id) {
		var oid = Long.valueOf(id);
		try {
			signalsLock.readLock().lock();
			return signalsById.get(oid);
		} finally {
			signalsLock.readLock().unlock();
		}
	}

	public final DataType dataTypeById(int id) {
		return dataTypesById.get(Integer.valueOf(id));
	}

}
