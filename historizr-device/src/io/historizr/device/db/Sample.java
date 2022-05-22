package io.historizr.device.db;

import java.time.OffsetDateTime;
import java.util.Objects;

import io.historizr.device.OpsMisc;

public abstract class Sample {

	public final OffsetDateTime tstamp;
	public final boolean quality;

	Sample(OffsetDateTime tstamp, boolean quality) {
		super();
		this.tstamp = Objects.requireNonNull(tstamp);
		this.quality = quality;
	}

	public abstract boolean hasDifferentValue(Sample s);

	public static final class OfBool extends Sample {
		private static final String BOOL_TRUE = "1";
		private static final String BOOL_FALSE = "0";
		public final boolean value;

		public OfBool(OffsetDateTime tstamp, boolean quality, boolean value) {
			super(tstamp, quality);
			this.value = value;
		}

		public static OfBool of(String v, OffsetDateTime t) {
			return of(v, t, true);
		}

		public static OfBool of(String v, OffsetDateTime t, boolean q) {
			var isParsed = false;
			var tmp = false;
			if (!OpsMisc.isNullOrEmpty(v)) {
				if (v.length() == 1) {
					// We reinterpret 0 and 1 as booleans.
					if (BOOL_FALSE.equals(v)) {
						tmp = false;
						isParsed = true;
					} else if (BOOL_TRUE.equals(v)) {
						tmp = true;
						isParsed = true;
					}
				}
				if (!isParsed) {
					// Otherwise just test for true/false string.
					try {
						tmp = Boolean.parseBoolean(v);
						isParsed = true;
					} catch (NumberFormatException e) {
						// Skip.
					}
				}
			}
			return new OfBool(t, isParsed, tmp);
		}

		@Override
		public final boolean hasDifferentValue(Sample s) {
			return s instanceof OfBool v ? v.value != this.value : true;
		}
	}

	public static final class OfFloat extends Sample {
		public final float value;

		public OfFloat(OffsetDateTime tstamp, boolean quality, float value) {
			super(tstamp, quality);
			this.value = value;
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
	}

	public static final class OfDouble extends Sample {
		public final double value;

		public OfDouble(OffsetDateTime tstamp, boolean quality, double value) {
			super(tstamp, quality);
			this.value = value;
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
	}

	public static final class OfLong extends Sample {
		public final long value;

		public OfLong(OffsetDateTime tstamp, boolean quality, long value) {
			super(tstamp, quality);
			this.value = value;
		}

		public static OfLong of(String v, OffsetDateTime t) {
			return of(v, t, true);
		}

		public static OfLong of(String v, OffsetDateTime t, boolean q) {

			if (!OpsMisc.isNullOrEmpty(v)) {
				try {
					var tmp = Long.parseLong(v);
					return new OfLong(t, q, tmp);
				} catch (NumberFormatException e) {
					// Skip.
				}
			}
			return new OfLong(t, false, 0);
		}

		@Override
		public final boolean hasDifferentValue(Sample s) {
			return s instanceof OfLong v ? v.value != this.value : true;
		}
	}

	public static final class OfString extends Sample {
		public final String value;

		public OfString(OffsetDateTime tstamp, boolean quality, String value) {
			super(tstamp, quality);
			this.value = value;
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
	}
}
