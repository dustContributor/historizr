package io.historizr.device;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.function.BiFunction;

import io.historizr.device.db.Sample;
import io.historizr.shared.db.DataType;

final record DataTypeMapper(
		DataType.Catalog dataType,
		Class<? extends Sample> sampleType,
		BiFunction<String, OffsetDateTime, Sample> toSample) {

	private static final DataTypeMapper[] MAPPERS;

	static {
		var values = DataType.Catalog.values();
		var maxId = Arrays.stream(values)
				.mapToInt(v -> v.id)
				.max()
				.getAsInt();
		MAPPERS = new DataTypeMapper[maxId + 1];
		Arrays.stream(values).map(v -> switch (v) {
		case BOOL -> new DataTypeMapper(v, Sample.OfBool.class, Sample.OfBool::of);
		case F32 -> new DataTypeMapper(v, Sample.OfFloat.class, Sample.OfFloat::of);
		case F64 -> new DataTypeMapper(v, Sample.OfDouble.class, Sample.OfDouble::of);
		case I64 -> new DataTypeMapper(v, Sample.OfLong.class, Sample.OfLong::of);
		case STR -> new DataTypeMapper(v, Sample.OfString.class, Sample.OfString::of);
		default -> null;
		}).forEach(v -> {
			if (v != null) {
				MAPPERS[v.dataType().id] = v;
			}
		});
	}

	public static DataTypeMapper ofId(int id) {
		return id >= 0 && id < MAPPERS.length ? MAPPERS[id] : null;
	}
}
