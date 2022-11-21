package io.historizr.device.db;

import java.time.OffsetDateTime;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAlias;

import io.historizr.shared.OpsMisc;

public abstract sealed class Sample permits Sample.OfBool, Sample.OfLong, Sample.OfFloat, Sample.OfDouble, Sample.OfString {
	public final long id;
	@JsonAlias("t")
	public final OffsetDateTime tstamp;
	@JsonAlias("q")
	public final boolean quality;

	Sample() {
		this.id = 0;
		this.tstamp = null;
		this.quality = false;
	}

	Sample(long id, OffsetDateTime tstamp, boolean quality) {
		super();
		this.id = id;
		this.tstamp = tstamp;
		this.quality = quality;
	}

	public boolean isValid() {
		return tstamp != null;
	}

	public static final double DEADBAND_SCALE = 1.0 / 1000.0;

	public static final double toDeadband(double v) {
		return v * DEADBAND_SCALE;
	}

	public static final boolean exceeds(double refValue, double newValue, double deadband) {
		if (refValue == newValue) {
			// Values are either finite and equal, or the same infinite signs.
			return false;
		}
		if (Double.isNaN(refValue)) {
			// If the new value isn't a NaN, accept it.
			return !Double.isNaN(newValue);
		}
		if (Double.isInfinite(refValue)) {
			// At this point the new value is either finite, or a different infinity.
			return true;
		}
		// Both values are finite, compare them.
		var a = refValue * -deadband + refValue;
		var b = refValue * deadband + refValue;
		// Swap if necessary, ref value could be negative.
		var min = Math.min(a, b);
		var max = Math.max(a, b);
		return newValue < min || newValue > max;
	}

	public abstract Sample withId(long id);

	public abstract boolean exceedsDeadband(Sample s, double deadband);

	public abstract boolean hasDifferentValue(Sample s);

	public static final class OfBool extends Sample {

		private static final String BOOL_NUM_FALSE = "0";
		private static final String BOOL_NUM_TRUE = "1";
		private static final String BOOL_LBL_FALSE = "false";
		private static final String BOOL_LBL_TRUE = "true";

		@JsonAlias("v")
		public final boolean value;

		public OfBool() {
			this.value = false;
		}

		public OfBool(long id, OffsetDateTime tstamp, boolean quality, boolean value) {
			super(id, tstamp, quality);
			this.value = value;
		}

		public OfBool(OffsetDateTime tstamp, boolean quality, boolean value) {
			this(0, tstamp, quality, value);
		}

		public static OfBool of(String v, OffsetDateTime t) {
			return of(v, t, true);
		}

		public static OfBool of(String v, OffsetDateTime t, boolean q) {
			var isParsed = false;
			var value = false;
			switch (v) {
			case BOOL_NUM_FALSE:
				value = false;
				isParsed = true;
				break;
			case BOOL_NUM_TRUE:
				value = true;
				isParsed = true;
				break;
			case BOOL_LBL_FALSE:
				value = false;
				isParsed = true;
				break;
			case BOOL_LBL_TRUE:
				value = true;
				isParsed = true;
				break;
			}
			return new OfBool(t, isParsed, value);
		}

		@Override
		public final boolean hasDifferentValue(Sample s) {
			return s instanceof OfBool v ? v.value != this.value : true;
		}

		@Override
		public final boolean exceedsDeadband(Sample s, double deadband) {
			return hasDifferentValue(s);
		}

		@Override
		public final Sample withId(long id) {
			return new OfBool(id, tstamp, quality, value);
		}
	}

	public static final class OfFloat extends Sample {
		@JsonAlias("v")
		public final float value;

		public OfFloat() {
			this.value = 0;
		}

		public OfFloat(long id, OffsetDateTime tstamp, boolean quality, float value) {
			super(id, tstamp, quality);
			this.value = value;
		}

		public OfFloat(OffsetDateTime tstamp, boolean quality, float value) {
			this(0, tstamp, quality, value);
		}

		public static OfFloat of(String v, OffsetDateTime t) {
			return of(v, t, true);
		}

		public static OfFloat of(String v, OffsetDateTime t, boolean q) {
			if (!OpsMisc.isNullOrEmpty(v)) {
				try {
					var tmp = Float.parseFloat(v);
					return new OfFloat(t, q, tmp);
				} catch (NumberFormatException e) {
					// Skip.
				}
			}
			return new OfFloat(t, false, 0);
		}

		@Override
		public final boolean hasDifferentValue(Sample s) {
			return s instanceof OfFloat v ? v.value != this.value : true;
		}

		@Override
		public final boolean exceedsDeadband(Sample s, double deadband) {
			return s instanceof OfFloat v ? Sample.exceeds(value, v.value, deadband) : true;
		}

		@Override
		public final Sample withId(long id) {
			return new OfFloat(id, tstamp, quality, value);
		}
	}

	public static final class OfDouble extends Sample {
		@JsonAlias("v")
		public final double value;

		public OfDouble() {
			this.value = 0;
		}

		public OfDouble(long id, OffsetDateTime tstamp, boolean quality, double value) {
			super(id, tstamp, quality);
			this.value = value;
		}

		public OfDouble(OffsetDateTime tstamp, boolean quality, double value) {
			this(0, tstamp, quality, value);
		}

		public static OfDouble of(String v, OffsetDateTime t) {
			return of(v, t, true);
		}

		public static OfDouble of(String v, OffsetDateTime t, boolean q) {
			if (!OpsMisc.isNullOrEmpty(v)) {
				try {
					var tmp = Double.parseDouble(v);
					return new OfDouble(t, q, tmp);
				} catch (NumberFormatException e) {
					// Skip.
				}
			}
			return new OfDouble(t, false, 0);
		}

		@Override
		public final boolean hasDifferentValue(Sample s) {
			return s instanceof OfDouble v ? v.value != this.value : true;
		}

		@Override
		public final boolean exceedsDeadband(Sample s, double deadband) {
			return s instanceof OfDouble v ? Sample.exceeds(value, v.value, deadband) : true;
		}

		@Override
		public final Sample withId(long id) {
			return new OfDouble(id, tstamp, quality, value);
		}
	}

	public static final class OfLong extends Sample {
		@JsonAlias("v")
		public final long value;

		public OfLong() {
			this.value = 0;
		}

		public OfLong(long id, OffsetDateTime tstamp, boolean quality, long value) {
			super(id, tstamp, quality);
			this.value = value;
		}

		public static OfLong of(String v, OffsetDateTime t) {
			return of(v, t, true);
		}

		public static OfLong of(String v, OffsetDateTime t, boolean q) {

			if (!OpsMisc.isNullOrEmpty(v)) {
				try {
					var tmp = Long.parseLong(v);
					return new OfLong(0, t, q, tmp);
				} catch (NumberFormatException e) {
					// Skip.
				}
			}
			return new OfLong(0, t, false, 0);
		}

		@Override
		public final boolean hasDifferentValue(Sample s) {
			return s instanceof OfLong v ? v.value != this.value : true;
		}

		@Override
		public final boolean exceedsDeadband(Sample s, double deadband) {
			return s instanceof OfLong v ? Sample.exceeds(value, v.value, deadband) : true;
		}

		@Override
		public final Sample withId(long id) {
			return new OfLong(id, tstamp, quality, value);
		}
	}

	public static final class OfString extends Sample {
		@JsonAlias("v")
		public final String value;

		public OfString() {
			this.value = null;
		}

		@Override
		public boolean isValid() {
			return super.isValid() && value != null;
		}

		public OfString(long id, OffsetDateTime tstamp, boolean quality, String value) {
			super(id, tstamp, quality);
			this.value = value;
		}

		public OfString(OffsetDateTime tstamp, boolean quality, String value) {
			this(0, tstamp, quality, value);
		}

		public static OfString of(String v, OffsetDateTime t) {
			return of(v, t, true);
		}

		public static OfString of(String v, OffsetDateTime t, boolean q) {
			// Null string is interpreted as bad quality.
			return new OfString(t, v != null, v == null ? "" : v);
		}

		@Override
		public final boolean hasDifferentValue(Sample s) {
			return s instanceof OfString v ? !Objects.equals(v.value, this.value) : true;
		}

		@Override
		public final boolean exceedsDeadband(Sample s, double deadband) {
			return hasDifferentValue(s);
		}

		@Override
		public final Sample withId(long id) {
			return new OfString(id, tstamp, quality, value);
		}
	}
}
