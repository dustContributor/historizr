package io.historizr.server.db;

import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class Db {
	private Db() {
		throw new RuntimeException();
	}

	public static final class Signal {
		private Signal() {
			throw new RuntimeException();
		}

		private static final String TBL = "signal";
		private static final String[] COL = { "id_device", "id_data_type", "name", "topic", "deadband",
				"is_on_change",
				"has_full_payload" };
		private static final String[] COL_ALL = Stream.concat(Stream.of("id"), Stream.of(COL))
				.toArray(String[]::new);

		public static final String QUERY = select(String.join(",", COL_ALL), TBL);
		public static final String INSERT = sql("""
				insert into %s(%s)
				values(%s)
				returning %s""", TBL, String.join(",", COL), argList(COL.length),
				String.join(",", COL_ALL));
		public static final String UPDATE = sql("""
				update %s
				set %s
				where id = %s
				returning %s""", TBL, IntStream.range(0, COL.length)
				.mapToObj(i -> COL[i] + " = $" + (i + 1))
				.collect(Collectors.joining(",")), "$" + (COL.length + 1),
				String.join(",", COL_ALL));
		public static final String DELETE = sql("""
				delete from %s
				where id = $1""", TBL);
	}

	public static final class DataType {
		private DataType() {
			throw new RuntimeException();
		}

		private static final String TBL = "data_type";
		private static final String[] COL = { "id_mapping", "name" };
		private static final String[] COL_ALL = Stream.concat(Stream.of("id"), Stream.of(COL))
				.toArray(String[]::new);
		public static final String QUERY = select(String.join(",", COL_ALL), TBL);
	}

	public static final class Device {
		private Device() {
			throw new RuntimeException();
		}

		private static final String TBL = "device";
		private static final String[] COL = { "id_type", "name", "name", "address", "port" };
		private static final String[] COL_ALL = Stream.concat(Stream.of("id"), Stream.of(COL))
				.toArray(String[]::new);

		public static final String QUERY = select(String.join(",", COL_ALL), TBL);
		public static final String INSERT = sql("""
				insert into %s(%s)
				values(%s)
				returning %s""", TBL, String.join(",", COL), argList(COL.length),
				String.join(",", COL_ALL));
		public static final String UPDATE = sql("""
				update %s
				set %s
				where id = %s
				returning %s""", TBL, IntStream.range(0, COL.length)
				.mapToObj(i -> COL[i] + " = $" + (i + 1))
				.collect(Collectors.joining(",")), "$" + (COL.length + 1),
				String.join(",", COL_ALL));
		public static final String DELETE = sql("""
				delete from %s
				where id = $1""", TBL);
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

}
