package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.Assert.*;

public class DoubleConversionsTest {

    /** Simple stub that mimics the real ConverterOptions used by the library.
     *  It only provides the methods that DoubleConversions accesses.
     */
    static class StubConverterOptions {
        private final TimeZone timeZone;
        private final ZoneId zoneId;

        StubConverterOptions(TimeZone tz, ZoneId zid) {
            this.timeZone = tz;
            this.zoneId = zid;
        }

        public TimeZone getTimeZone() {
            return timeZone;
        }

        public ZoneId getZoneId() {
            return zoneId;
        }
    }

    /** Minimal concrete Converter that forwards to the stub options.
     *  Only the methods required by DoubleConversions are needed for the tests.
     */
    static class StubConverter extends Converter {
        private final StubConverterOptions options;

        public StubConverter(StubConverterOptions options) {
            super(null); // the real Converter constructor expects a ConverterOptions;
                        // we pass null because we never use the superclass fields.
            this.options = options;
        }

        @Override
        public ConverterOptions getOptions() {
            // Wrap the stub options in an anonymous subclass that satisfies the
            // expected return type without pulling in the real library code.
            return new ConverterOptions() {
                public TimeZone getTimeZone() {
                    return options.getTimeZone();
                }

                public ZoneId getZoneId() {
                    return options.getZoneId();
                }
            };
        }
    }

    /** Helper class to expose the needed methods of the real ConverterOptions
     *  without depending on its actual implementation.
     */
    abstract static class ConverterOptions {
        public abstract TimeZone getTimeZone();

        public abstract ZoneId getZoneId();
    }

    private Converter utcConverter;

    @Before
    public void setUp() {
        StubConverterOptions opts = new StubConverterOptions(TimeZone.getTimeZone("UTC"),
                ZoneId.of("UTC"));
        utcConverter = new StubConverter(opts);
    }

    /* ----------------------------------------------------------------------
     *  1. toInstant
     * ---------------------------------------------------------------------- */

    @Test
    public void testToInstant_Basic() {
        double epochSeconds = 1622548800.123; // 2021‑06‑01T12:00:00.123Z
        Instant instant = DoubleConversions.toInstant(epochSeconds, utcConverter);
        assertEquals(1622548800L, instant.getEpochSecond());
        assertEquals(123_000_000, instant.getNano());
    }

    @Test
    public void testToInstant_NegativeValue() {
        double epochSeconds = -1.5; // one and a half seconds before epoch
        Instant instant = DoubleConversions.toInstant(epochSeconds, utcConverter);
        assertEquals(-1L, instant.getEpochSecond());
        assertEquals(-500_000_000, instant.getNano()); // Instant normalises negative nanos
        // Normalisation: Instant.ofEpochSecond(-1, -500_000_000) becomes -2 seconds + 500_000_000 nanos
        assertEquals(-2L, instant.getEpochSecond());
        assertEquals(500_000_000, instant.getNano());
    }

    @Test(expected = DateTimeException.class)
    public void testToInstant_NanoOverflow() {
        // Fractional part causes rounding to 1_000_000_000 nanos, which is illegal.
        double epochSeconds = 1.9999999995; // rounds up to 2 seconds, 0 nanos
        DoubleConversions.toInstant(epochSeconds, utcConverter);
    }

    /* ----------------------------------------------------------------------
     *  2. toDate
     * ---------------------------------------------------------------------- */

    @Test
    public void testToDate_Basic() {
        double epochSeconds = 1622548800.5; // 1622548800500 ms
        Date date = DoubleConversions.toDate(epochSeconds, utcConverter);
        assertEquals(1622548800500L, date.getTime());
    }

    @Test
    public void testToDate_Negative() {
        double epochSeconds = -10.75; // -10750 ms
        Date date = DoubleConversions.toDate(epochSeconds, utcConverter);
        assertEquals(-10750L, date.getTime());
    }

    /* ----------------------------------------------------------------------
     *  3. toSqlDate
     * ---------------------------------------------------------------------- */

    @Test
    public void testToSqlDate_Basic() {
        double epochSeconds = 0.0;
        java.sql.Date sqlDate = (java.sql.Date) DoubleConversions.toSqlDate(epochSeconds, utcConverter);
        assertEquals(0L, sqlDate.getTime());
    }

    @Test
    public void testToSqlDate_WithFraction() {
        double epochSeconds = 86400.999; // one day + 999 ms
        java.sql.Date sqlDate = (java.sql.Date) DoubleConversions.toSqlDate(epochSeconds, utcConverter);
        // java.sql.Date truncates the time component, keeping only the date part.
        // 86400.999 * 1000 = 86400999 ms -> truncated to 86400000 ms (midnight of next day)
        assertEquals(86400000L, sqlDate.getTime());
    }

    /* ----------------------------------------------------------------------
     *  4. toCalendar
     * ---------------------------------------------------------------------- */

    @Test
    public void testToCalendar_Basic() {
        double epochSeconds = 1622548800.0; // exact second
        Calendar cal = DoubleConversions.toCalendar(epochSeconds, utcConverter);
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
        assertEquals(1622548800000L, cal.getTimeInMillis());
    }

    @Test
    public void testToCalendar_Negative() {
        double epochSeconds = -3600.0; // one hour before epoch
        Calendar cal = DoubleConversions.toCalendar(epochSeconds, utcConverter);
        assertEquals(-3600000L, cal.getTimeInMillis());
    }

    /* ----------------------------------------------------------------------
     *  5. toLocalTime
     * ---------------------------------------------------------------------- */

    @Test
    public void testToLocalTime_Basic() {
        double seconds = 3661.123456789; // 01:01:01.123456789
        LocalTime lt = DoubleConversions.toLocalTime(seconds, utcConverter);
        assertEquals(LocalTime.of(1, 1, 1, 123_456_789), lt);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTime_Negative() {
        DoubleConversions.toLocalTime(-1.0, utcConverter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTime_TooLarge() {
        // 24 hours = 86400 seconds, which is out of range (max is 86399.999999999)
        DoubleConversions.toLocalTime(86400.0, utcConverter);
    }

    /* ----------------------------------------------------------------------
     *  6. toLocalDate
     * ---------------------------------------------------------------------- */

    @Test
    public void testToLocalDate_Basic() {
        // 2021‑06‑01T00:00:00Z
        double epochSeconds = 1622505600.0;
        LocalDate ld = DoubleConversions.toLocalDate(epochSeconds, utcConverter);
        assertEquals(LocalDate.of(2021, 6, 1), ld);
    }

    /* ----------------------------------------------------------------------
     *  7. toLocalDateTime
     * ---------------------------------------------------------------------- */

    @Test
    public void testToLocalDateTime_Basic() {
        double epochSeconds = 1622548800.123; // 2021‑06‑01T12:00:00.123Z
        LocalDateTime ldt = DoubleConversions.toLocalDateTime(epochSeconds, utcConverter);
        assertEquals(LocalDateTime.of(2021, 6, 1, 12, 0, 0, 123_000_000), ldt);
    }

    /* ----------------------------------------------------------------------
     *  8. toZonedDateTime
     * ---------------------------------------------------------------------- */

    @Test
    public void testToZonedDateTime_Basic() {
        double epochSeconds = 0.0;
        ZonedDateTime zdt = DoubleConversions.toZonedDateTime(epochSeconds, utcConverter);
        assertEquals(ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.of("UTC")), zdt);
    }

    /* ----------------------------------------------------------------------
     *  9. toOffsetDateTime
     * ---------------------------------------------------------------------- */

    @Test
    public void testToOffsetDateTime_Basic() {
        double epochSeconds = 0.0;
        OffsetDateTime odt = DoubleConversions.toOffsetDateTime(epochSeconds, utcConverter);
        assertEquals(OffsetDateTime.ofInstant(Instant.EPOCH, ZoneId.of("UTC")), odt);
    }

    /* ----------------------------------------------------------------------
     * 10. toTimestamp
     * ---------------------------------------------------------------------- */

    @Test
    public void testToTimestamp_Basic() {
        double epochSeconds = 1622548800.123456789;
        Timestamp ts = DoubleConversions.toTimestamp(epochSeconds, utcConverter);
        assertEquals(1622548800123L, ts.getTime()); // milliseconds part
        assertEquals(123456789, ts.getNanos());   // nanos part
    }

    /* ----------------------------------------------------------------------
     * 11. toDuration
     * ---------------------------------------------------------------------- */

    @Test
    public void testToDuration_Basic() {
        double d = 123.456789012; // 123 seconds + 456,789,012 ns
        Duration dur = DoubleConversions.toDuration(d, utcConverter);
        assertEquals(123L, dur.getSeconds());
        assertEquals(456_789_012L, dur.getNano());
    }

    @Test
    public void testToDuration_Negative() {
        double d = -10.75; // -10 seconds - 750,000,000 ns
        Duration dur = DoubleConversions.toDuration(d, utcConverter);
        // Duration normalises negative nanoseconds
        assertEquals(-11L, dur.getSeconds());
        assertEquals(250_000_000L, dur.getNano());
    }

    @Test
    public void testToDuration_Zero() {
        double d = 0.0;
        Duration dur = DoubleConversions.toDuration(d, utcConverter);
        assertEquals(Duration.ZERO, dur);
    }
}
