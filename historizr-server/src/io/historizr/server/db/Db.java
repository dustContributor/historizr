package io.historizr.server.db;

import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class Db {
	private Db() {
		throw new RuntimeException();
	}

	public static final class Sql {
		private Sql() {
			throw new RuntimeException();
		}

		private static final CharSequence argList(int count) {
			return argList(1, count);
		}

		private static final CharSequence argList(int start, int count) {
			var b = new StringBuilder();
			var len = start + count;
			for (int i = start; i < len; ++i) {
				if (b.length() > 0) {
					b.append(',');
				}
				b.append('$').append(i);
			}
			return b;
		}

		private static final String select(Object... fmts) {
			return sql("select %s from %s", fmts);
		}

		private static final String sql(String base, Object... fmts) {
			return base.formatted(fmts).stripIndent();
		}

		private static final String TABLE_SIGNAL = "signal";
		private static final String TABLE_DATA_TYPE = "data_type";
		private static final String[] COLUMNS_SIGNAL = { "id_device", "id_data_type", "name", "topic", "deadband",
				"is_on_change",
				"has_full_payload" };
		private static final String[] COLUMNS_SIGNAL_ALL = Stream.concat(Stream.of("id"), Stream.of(COLUMNS_SIGNAL))
				.toArray(String[]::new);
		private static final String[] COLUMNS_DATA_TYPE_ALL = { "id", "id_mapping", "name" };
		public static final String QUERY_DATA_TYPE = select(String.join(",", COLUMNS_DATA_TYPE_ALL), TABLE_DATA_TYPE);
		public static final String QUERY_SIGNAL = select(String.join(",", COLUMNS_SIGNAL_ALL), TABLE_SIGNAL);
		public static final String INSERT_SIGNAL = sql("""
				insert into %s(%s)
				values(%s)
				returning %s""", TABLE_SIGNAL, String.join(",", COLUMNS_SIGNAL), argList(COLUMNS_SIGNAL.length),
				String.join(",", COLUMNS_SIGNAL_ALL));
		public static final String UPDATE_SIGNAL = sql("""
				update %s
				set %s
				where id = %s
				returning %s""", TABLE_SIGNAL, IntStream.range(0, COLUMNS_SIGNAL.length)
				.mapToObj(i -> COLUMNS_SIGNAL[i] + " = $" + (i + 1))
				.collect(Collectors.joining(",")), "$" + (COLUMNS_SIGNAL.length + 1),
				String.join(",", COLUMNS_SIGNAL_ALL));
		public static final String DELETE_SIGNAL = sql("""
				delete from %s
				where id = $1""", TABLE_SIGNAL);
	}

}
