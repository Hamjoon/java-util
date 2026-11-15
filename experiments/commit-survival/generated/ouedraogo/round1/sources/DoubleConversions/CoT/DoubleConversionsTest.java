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

    /** Minimal stub of the library's ConverterOptions used only for the tests. */
    private static class TestConverterOptions {
        private final TimeZone timeZone;
        private final ZoneId zoneId;

        TestConverterOptions(TimeZone timeZone, ZoneId zoneId) {
            this.timeZone = timeZone;
            this.zoneId   = zoneId;
        }

        public TimeZone getTimeZone() {
            return timeZone;
        }

        public ZoneId getZoneId() {
            return zoneId;
        }
    }

    /** Helper method to create a Converter that returns the supplied options. */
    private static Converter createConverter(TimeZone tz, ZoneId zoneId) {
        // The real library's Converter has a public constructor that accepts a ConverterOptions object.
        // We supply a simple stub that satisfies the required getters.
        TestConverterOptions opts = new TestConverterOptions(tz, zoneId);
        return new Converter(opts);
    }

    private Converter utcConverter;

    @Before
    public void setUp() {
        utcConverter = createConverter(TimeZone.getTimeZone("UTC"), ZoneId.of("UTC"));
    }

    /* ----------------------------------------------------------------------
     *  List of package‑private static methods in DoubleConversions
     * ---------------------------------------------------------------------- */
    // static Instant           toInstant(Object from, Converter converter)
    // static Date             toDate(Object from, Converter converter)
    // static Date             toSqlDate(Object from, Converter converter)
    // static Calendar         toCalendar(Object from, Converter converter)
    // static LocalTime        toLocalTime(Object from, Converter converter)
    // static LocalDate        toLocalDate(Object from, Converter converter)
    // static LocalDateTime    toLocalDateTime(Object from, Converter converter)
    // static ZonedDateTime    toZonedDateTime(Object from, Converter converter)
    // static OffsetDateTime   toOffsetDateTime(Object from, Converter converter)
    // static Timestamp        toTimestamp(Object from, Converter converter)
    // static Duration         toDuration(Object from, Converter converter)

    /* ----------------------------------------------------------------------
     *  Basic functional tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testToInstantWholeAndFractionalSeconds() {
        Double d = 12345.6789; // 12345 seconds + 0.6789 sec
        Instant result = DoubleConversions.toInstant(d, utcConverter);
        long expectedSeconds = 12345L;
        long expectedNanos   = Math.round(0.6789 * 1_000_000_000L);
        assertEquals(Instant.ofEpochSecond(expectedSeconds, expectedNanos), result);
    }

    @Test
    public void testToInstantNegativeValue() {
        Double d = -10.25; // negative epoch seconds
        Instant result = DoubleConversions.toInstant(d, utcConverter);
        long expectedSeconds = -10L;
        long expectedNanos   = Math.round((-10.25 - expectedSeconds) * 1_000_000_000L);
        assertEquals(Instant.ofEpochSecond(expectedSeconds, expectedNanos), result);
    }

    @Test
    public void testToDateSimpleConversion() {
        Double d = 2.5; // 2.5 seconds -> 2500 ms
        Date result = DoubleConversions.toDate(d, utcConverter);
        assertEquals(new Date(2500L), result);
    }

    @Test
    public void testToDateNaN() {
        Double d = Double.NaN;
        Date result = DoubleConversions.toDate(d, utcConverter);
        // (long)NaN is 0, so we expect epoch (0 ms)
        assertEquals(new Date(0L), result);
    }

    @Test
    public void testToSqlDateConversion() {
        Double d = 1.0; // 1 second -> 1000 ms
        java.sql.Date result = (java.sql.Date) DoubleConversions.toSqlDate(d, utcConverter);
        assertEquals(new java.sql.Date(1000L), result);
    }

    @Test
    public void testToCalendarUsesConverterTimeZone() {
        // Use a non‑UTC time zone to ensure the calendar respects the option.
        TimeZone pst = TimeZone.getTimeZone("America/Los_Angeles");
        ZoneId pstId = ZoneId.of("America/Los_Angeles");
        Converter pstConverter = createConverter(pst, pstId);

        Double seconds = 0.0; // epoch start
        Calendar cal = DoubleConversions.toCalendar(seconds, pstConverter);
        assertEquals(pst.getID(), cal.getTimeZone().getID());
        assertEquals(0L, cal.getTimeInMillis());
    }

    @Test
    public void testToLocalTimeValidBoundaries() {
        // exactly midnight
        Double midnight = 0.0;
        LocalTime ltMidnight = DoubleConversions.toLocalTime(midnight, utcConverter);
        assertEquals(LocalTime.MIDNIGHT, ltMidnight);

        // last nanosecond of the day
        Double last = 86_399.999_999_999; // 23:59:59.999999999
        LocalTime ltLast = DoubleConversions.toLocalTime(last, utcConverter);
        assertEquals(LocalTime.of(23, 59, 59, 999_999_999), ltLast);
    }

    @Test
    public void testToLocalTimeOutOfRangeThrows() {
        Double negative = -0.1;
        try {
            DoubleConversions.toLocalTime(negative, utcConverter);
            fail("Expected IllegalArgumentException for negative seconds");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("must be >= 0 && <= 86399.999999999"));
        }

        Double tooLarge = 86_400.0; // exactly 24 hours, which is invalid
        try {
            DoubleConversions.toLocalTime(tooLarge, utcConverter);
            fail("Expected IllegalArgumentException for seconds >= 86400");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("must be >= 0 && <= 86399.999999999"));
        }
    }

    @Test
    public void testToLocalDateTimeFromInstant() {
        Double d = 1609459200.0; // 2021‑01‑01T00:00:00Z
        LocalDateTime ldt = DoubleConversions.toLocalDateTime(d, utcConverter);
        assertEquals(LocalDateTime.of(2021, 1, 1, 0, 0), ldt);
    }

    @Test
    public void testToZonedDateTimeRespectsZoneId() {
        // Use a different zone to verify conversion.
        ZoneId paris = ZoneId.of("Europe/Paris");
        Converter parisConverter = createConverter(TimeZone.getTimeZone(paris), paris);

        Double d = 0.0; // epoch
        ZonedDateTime zdt = DoubleConversions.toZonedDateTime(d, parisConverter);
        assertEquals(ZoneId.of("Europe/Paris"), zdt.getZone());
        assertEquals(Instant.EPOCH, zdt.toInstant());
    }

    @Test
    public void testToOffsetDateTime() {
        Double d = 0.0;
        OffsetDateTime odt = DoubleConversions.toOffsetDateTime(d, utcConverter);
        assertEquals(OffsetDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC), odt);
    }

    @Test
    public void testToTimestampMatchesInstant() {
        Double d = 1622559600.123; // 2021‑06‑01T12:00:00.123Z
        Timestamp ts = DoubleConversions.toTimestamp(d, utcConverter);
        Instant expected = DoubleConversions.toInstant(d, utcConverter);
        assertEquals(Timestamp.from(expected), ts);
    }

    @Test
    public void testToDurationWholeAndFractional() {
        Double d = 5.75; // 5 seconds + 750 ms
        Duration dur = DoubleConversions.toDuration(d, utcConverter);
        assertEquals(Duration.ofSeconds(5, 750_000_000L), dur);
    }

    @Test
    public void testToDurationNegative() {
        Double d = -2.3; // -2 seconds - 300_000_000 ns
        Duration dur = DoubleConversions.toDuration(d, utcConverter);
        // Duration normalises negative values correctly
        assertEquals(Duration.ofSeconds(-2, -300_000_000L), dur);
    }

    /* ----------------------------------------------------------------------
     *  Edge‑case tests for rounding and overflow handling
     * ---------------------------------------------------------------------- */

    @Test
    public void testToInstantRoundingEdge() {
        // double value that is just below a half‑nanosecond boundary
        double d = 1.0000000004; // fractional part .0000000004 * 1e9 = 0.4 ns -> rounds to 0
        Instant result = DoubleConversions.toInstant(d, utcConverter);
        assertEquals(Instant.ofEpochSecond(1L, 0L), result);
    }

    @Test
    public void testToDurationRoundingEdge() {
        // fractional part that should round up
        double d = 2.9999999995; // .9999999995 * 1e9 = 999_999_999.5 -> rounds to 1_000_000_000 (i.e., +1 sec)
        Duration dur = DoubleConversions.toDuration(d, utcConverter);
        assertEquals(Duration.ofSeconds(3L, 0L), dur);
    }
}
