package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link DoubleConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Typical conversions (positive values, fractions)</li>
 *   <li>Edge cases (zero, maximum‑day time, rounding of nanoseconds)</li>
 *   <li>Error scenarios (values out of range for time‑only conversions)</li>
 * </ul>
 *
 * The {@link Converter} is created with deterministic options:
 * <ul>
 *   <li>ZoneId = UTC</li>
 *   <li>TimeZone = UTC</li>
 * </ul>
 * This guarantees that all date‑time results are repeatable regardless of the
 * host machine’s default time‑zone.
 */
public class DoubleConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // Create options with UTC zone and time‑zone.
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(ZoneId.of("UTC"));
        options.setTimeZone(TimeZone.getTimeZone("UTC"));
        converter = new Converter(options);
    }

    /* -------------------------------------------------------
     *  toInstant
     * ------------------------------------------------------- */
    @Test
    public void testToInstantTypical() {
        Double d = 1_234_567.891234567; // 1,234,567 seconds + fraction
        Instant instant = DoubleConversions.toInstant(d, converter);

        long expectedSeconds = 1_234_567L;
        long expectedNanos = Math.round((d - expectedSeconds) * 1_000_000_000L);
        Instant expected = Instant.ofEpochSecond(expectedSeconds, expectedNanos);

        assertEquals(expected, instant);
    }

    @Test
    public void testToInstantZero() {
        Double d = 0.0;
        Instant instant = DoubleConversions.toInstant(d, converter);
        assertEquals(Instant.EPOCH, instant);
    }

    @Test
    public void testToInstantNegativeFractionRounding() {
        // -1.2 seconds -> seconds = -1, fraction = -0.2 -> nanos should round correctly
        Double d = -1.2;
        Instant instant = DoubleConversions.toInstant(d, converter);

        // Java's Instant.ofEpochSecond handles negative seconds & positive nanos.
        long seconds = (long) d; // -1
        long nanos = Math.round((d - seconds) * 1_000_000_000L); // -0.2 * 1e9 = -200,000,000 -> rounded
        Instant expected = Instant.ofEpochSecond(seconds, nanos);
        assertEquals(expected, instant);
    }

    /* -------------------------------------------------------
     *  toDate
     * ------------------------------------------------------- */
    @Test
    public void testToDateTypical() {
        Double d = 1_500.75; // 1500.75 seconds = 1500750 ms
        Date date = DoubleConversions.toDate(d, converter);
        assertEquals(1_500_750L, date.getTime());
    }

    @Test
    public void testToDateTruncatesFraction() {
        Double d = 123.999; // fraction is ignored when casting to long
        Date date = DoubleConversions.toDate(d, converter);
        assertEquals(123_000L, date.getTime()); // 123 seconds * 1000 ms
    }

    /* -------------------------------------------------------
     *  toSqlDate
     * ------------------------------------------------------- */
    @Test
    public void testToSqlDateTypical() {
        // 86400 seconds = exactly one day after epoch -> 1970‑01‑02
        Double d = 86_400.0;
        java.sql.Date sqlDate = DoubleConversions.toSqlDate(d, converter);
        assertEquals(java.sql.Date.valueOf("1970-01-02"), sqlDate);
    }

    @Test
    public void testToSqlDateWithFraction() {
        // 0.5 day = 12 hours => 1970‑01‑01 (date part unchanged)
        Double d = 43_200.0; // 12 hours
        java.sql.Date sqlDate = DoubleConversions.toSqlDate(d, converter);
        assertEquals(java.sql.Date.valueOf("1970-01-01"), sqlDate);
    }

    /* -------------------------------------------------------
     *  toCalendar
     * ------------------------------------------------------- */
    @Test
    public void testToCalendarTypical() {
        Double d = 1_234_567.0;
        Calendar cal = DoubleConversions.toCalendar(d, converter);
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
        assertEquals(1_234_567_000L, cal.getTimeInMillis());
    }

    @Test
    public void testToCalendarFractionIsTruncated() {
        Double d = 123.456; // 123.456 sec -> 123456 ms (fraction kept because of *1000)
        Calendar cal = DoubleConversions.toCalendar(d, converter);
        assertEquals(123_456L, cal.getTimeInMillis());
    }

    /* -------------------------------------------------------
     *  toLocalTime
     * ------------------------------------------------------- */
    @Test
    public void testToLocalTimeTypical() {
        // 13:30:15.123456789 = 13*3600 + 30*60 + 15.123456789 seconds
        double seconds = 13 * 3600 + 30 * 60 + 15.123456789;
        LocalTime lt = DoubleConversions.toLocalTime(seconds, converter);
        LocalTime expected = LocalTime.of(13, 30, 15, 123_456_789);
        assertEquals(expected, lt);
    }

    @Test
    public void testToLocalTimeZero() {
        LocalTime lt = DoubleConversions.toLocalTime(0.0, converter);
        assertEquals(LocalTime.MIDNIGHT, lt);
    }

    @Test
    public void testToLocalTimeMaximumAllowed() {
        // 23:59:59.999999999 (just under 86400 seconds)
        double seconds = 86_399.999_999_999;
        LocalTime lt = DoubleConversions.toLocalTime(seconds, converter);
        assertEquals(LocalTime.of(23, 59, 59, 999_999_999), lt);
    }

    @Test
    public void testToLocalTimeOutOfRangeThrows() {
        double tooBig = 86_400.0; // exactly 24h -> out of range
        try {
            DoubleConversions.toLocalTime(tooBig, converter);
            fail("Expected IllegalArgumentException for value >= 86400");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("must be >= 0 && <= 86399.999999999"));
        }
    }

    /* -------------------------------------------------------
     *  toOffsetTime
     * ------------------------------------------------------- */
    @Test
    public void testToOffsetTimeTypical() {
        double seconds = 10_000.5; // 2h 46m 40.5s
        OffsetTime ot = DoubleConversions.toOffsetTime(seconds, converter);
        Instant instant = Instant.ofEpochSecond(10_000L, 500_000_000L);
        OffsetTime expected = OffsetTime.ofInstant(instant, ZoneId.of("UTC"));
        assertEquals(expected, ot);
    }

    @Test
    public void testToOffsetTimeOutOfRangeThrows() {
        double tooBig = 86_400.0; // 24h -> invalid for time‑only conversion
        try {
            DoubleConversions.toOffsetTime(tooBig, converter);
            fail("Expected IllegalArgumentException for value >= 86400");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("must be >= 0 && <= 86399.999999999"));
        }
    }

    /* -------------------------------------------------------
     *  toZonedDateTime / toLocalDate / toLocalDateTime
     * ------------------------------------------------------- */
    @Test
    public void testToZonedDateTimeTypical() {
        Double d = 1_600_000_000.0; // 2020‑09‑13T12:26:40Z
        ZonedDateTime zdt = DoubleConversions.toZonedDateTime(d, converter);
        ZonedDateTime expected = Instant.ofEpochSecond(1_600_000_000L).atZone(ZoneId.of("UTC"));
        assertEquals(expected, zdt);
    }

    @Test
    public void testToLocalDateFromEpoch() {
        Double d = 0.0; // epoch
        LocalDate ld = DoubleConversions.toLocalDate(d, converter);
        assertEquals(LocalDate.of(1970, 1, 1), ld);
    }

    @Test
    public void testToLocalDateTimeTypical() {
        Double d = 86_400.0 + 3_600.0 + 60.0 + 1.0; // 1 day + 1 hour + 1 minute + 1 second
        LocalDateTime ldt = DoubleConversions.toLocalDateTime(d, converter);
        LocalDateTime expected = Instant.ofEpochSecond(86_400L + 3_600L + 60L + 1L)
                .atZone(ZoneId.of("UTC")).toLocalDateTime();
        assertEquals(expected, ldt);
    }

    /* -------------------------------------------------------
     *  toOffsetDateTime
     * ------------------------------------------------------- */
    @Test
    public void testToOffsetDateTimeTypical() {
        Double d = 1_234_567.0;
        OffsetDateTime odt = DoubleConversions.toOffsetDateTime(d, converter);
        OffsetDateTime expected = Instant.ofEpochSecond(1_234_567L)
                .atZone(ZoneId.of("UTC")).toOffsetDateTime();
        assertEquals(expected, odt);
    }

    /* -------------------------------------------------------
     *  toTimestamp
     * ------------------------------------------------------- */
    @Test
    public void testToTimestampTypical() {
        Double d = 1_600_000_000.123; // includes fractional seconds
        Timestamp ts = DoubleConversions.toTimestamp(d, converter);
        Instant expected = Instant.ofEpochSecond(1_600_000_000L, 123_000_000L);
        assertEquals(Timestamp.from(expected), ts);
    }

    /* -------------------------------------------------------
     *  toDuration
     * ------------------------------------------------------- */
    @Test
    public void testToDurationTypical() {
        Double d = 123.456789012; // 123 seconds + 456,789,012 ns
        Duration dur = DoubleConversions.toDuration(d, converter);
        Duration expected = Duration.ofSeconds(123L, 456_789_012L);
        assertEquals(expected, dur);
    }

    @Test
    public void testToDurationNegative() {
        Double d = -10.5; // -10 seconds - 0.5 second
        Duration dur = DoubleConversions.toDuration(d, converter);
        // Duration normalises the sign across seconds and nanos
        Duration expected = Duration.ofSeconds(-10L, -500_000_000L);
        assertEquals(expected, dur);
    }

    @Test
    public void testToDurationLargeValue() {
        Double d = 1e12; // 1,000,000,000,000 seconds
        Duration dur = DoubleConversions.toDuration(d, converter);
        Duration expected = Duration.ofSeconds(1_000_000_000_000L, 0L);
        assertEquals(expected, dur);
    }
}
