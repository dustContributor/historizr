package io.historizr.device;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import io.historizr.device.db.DataType;
import io.historizr.device.db.Db;
import io.historizr.device.db.Signal;

public final class SignalRepo {
	private final Config cfg;
	private Map<String, Signal> signalsByTopic;
	private Map<Long, Signal> signalsById;
	private Map<Integer, DataType> dataTypesById;

	public SignalRepo(Config cfg) {
		this.cfg = Objects.requireNonNull(cfg);
	}

	public final SignalRepo init() {
		var signals = new ArrayList<Signal>();
		var dataTypes = new ArrayList<DataType>();
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

		signalsByTopic = signals.stream().collect(Collectors.toMap(k -> k.topic(), v -> v));
		signalsById = signals.stream().collect(Collectors.toMap(k -> k.id(), v -> v));
		dataTypesById = dataTypes.stream().collect(Collectors.toMap(k -> k.id(), v -> v));

		return this;
	}

	public final Signal signalByTopic(String topic) {
		return signalsByTopic.get(topic);
	}

	public final Signal signalById(long id) {
		return signalsById.get(Long.valueOf(id));
	}

	public final DataType dataTypeById(int id) {
		return dataTypesById.get(Integer.valueOf(id));
	}

}
