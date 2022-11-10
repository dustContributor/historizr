package io.historizr.server.db;

import io.historizr.shared.db.MappingOp;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

public record DeviceType(
		long id,
		String name,
		String description) {

	public static DeviceType ofRow(Row rs) {
		int i = 0;
		return new DeviceType(
				rs.getLong(i++),
				rs.getString(i++),
				rs.getString(i++));
	}

	public static DeviceType of(Row rs) {
		return ofRow(rs);
	}

	public final Tuple tuple(MappingOp behavior) {
		return into(Tuple.tuple(), behavior);
	}

	public final Tuple into(Tuple dest, MappingOp behavior) {
		if (behavior == MappingOp.ID_FIRST) {
			dest.addLong(id());
		}
		dest.addString(name())
				.addString(description());
		if (behavior == MappingOp.ID_LAST) {
			dest.addLong(id());
		}
		return dest;
	}

	public static final DeviceType empty() {
		return empty(0);
	}

	public static final DeviceType empty(long id) {
		return new DeviceType(id, null, null);
	}
}
