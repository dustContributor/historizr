package io.historizr.server.db;

import java.net.InetAddress;

import io.vertx.pgclient.data.Inet;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

public record Device(
		long id,
		long typeId,
		String name,
		InetAddress address,
		int port) {

	public static Device of(Row rs) {
		int i = 0;
		return new Device(
				rs.getLong(i++),
				rs.getLong(i++),
				rs.getString(i++),
				((Inet) rs.getValue(i++)).getAddress(),
				rs.getInteger(i++));
	}

	public final Tuple tuple(MappingOp behavior) {
		return into(Tuple.tuple(), behavior);
	}

	public final Tuple into(Tuple dest, MappingOp behavior) {
		if (behavior == MappingOp.ID_FIRST) {
			dest.addLong(id());
		}
		dest.addLong(typeId())
				.addString(name())
				.addValue(new Inet().setAddress(address()))
				.addInteger(port());
		if (behavior == MappingOp.ID_LAST) {
			dest.addLong(id());
		}
		return dest;
	}

	public static final Device empty() {
		return empty(0);
	}

	public static final Device empty(long id) {
		return new Device(id, 0, null, null, 0);
	}
}
