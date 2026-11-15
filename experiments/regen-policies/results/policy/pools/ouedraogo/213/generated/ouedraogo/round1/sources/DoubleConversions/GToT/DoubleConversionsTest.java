package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.time.Duration;

import static org.junit.Assert.*;

public class DoubleConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // The library supplies a public no‑arg constructor for ConverterOptions.
        // Using the default system zone / time‑zone is sufficient for our tests.
        ConverterOptions options = new ConverterOptions(); // assume public ctor
        converter = new Converter(options);
    }

    /* --------------------------------------------------------------
     *  HAPPY‑PATH TESTS
     * -------------------------------------------------------------- */

    @Test
    public void testToInstant() {
        Double d = 1_234_567.891234567;               // 1 234 567 s + fraction
        Instant expected = Instant.ofEpochSecond(1_234_567, 891_234_567L);
        Instant actual = DoubleConversions.toInstant(d, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToDate() {
        Double d = 1_234.567; // seconds
        Date expected = new Date((long) (1_234.567 * 1000));
        Date actual = DoubleConversions.toDate(d, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToSqlDate() {
        // 2022‑01‑01T00:00:00Z  → epoch seconds = 1640995200
        Double d = 1_640_995_200.0;
        java.sql.Date expected = java.sql.Date.valueOf(
                Instant.ofEpochSecond(1_640_995_200L)
                        .atZone(converter.getOptions().getZoneId())
                        .toLocalDate()
        );
        java.sql.Date actual = DoubleConversions.toSqlDate(d, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToCalendar() {
        Double d = 1_600_000.0; // seconds → 1 600 000 000 ms
        Calendar cal = DoubleConversions.toCalendar(d, converter);
        assertEquals(TimeZone.getTimeZone(converter.getOptions().getTimeZone().getID()),
                     cal.getTimeZone());
        assertEquals(1_600_000_000L, cal.getTimeInMillis());
    }

    @Test
    public void testToLocalTime() {
        Double seconds = 12_345.678901234; // 3 h 25 m 45 s + fraction
        long nanosOfDay = (long) (seconds * 1_000_000_000L);
        LocalTime expected = LocalTime.ofNanoOfDay(nanosOfDay);
        LocalTime actual = DoubleConversions.toLocalTime(seconds, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLocalDate() {
        Double d = 1_640_995_200.0; // 2022‑01‑01
        LocalDate expected = Instant.ofEpochSecond(1_640_995_200L)
                .atZone(converter.getOptions().getZoneId())
                .toLocalDate();
        LocalDate actual = DoubleConversions.toLocalDate(d, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLocalDateTime() {
        Double d = 1_640_995_200.123; // 2022‑01‑01T00:00:00.123Z
        LocalDateTime expected = Instant.ofEpochSecond(1_640_995_200L, 123_000_000L)
                .atZone(converter.getOptions().getZoneId())
                .toLocalDateTime();
        LocalDateTime actual = DoubleConversions.toLocalDateTime(d, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToZonedDateTime() {
        Double d = 1_640_995_200.0;
        ZonedDateTime expected = Instant.ofEpochSecond(1_640_995_200L)
                .atZone(converter.getOptions().getZoneId());
        ZonedDateTime actual = DoubleConversions.toZonedDateTime(d, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToOffsetTime() {
        Double seconds = 12_345.678901234; // same as LocalTime test
        long wholeSecs = (long) seconds.doubleValue();
        double frac = seconds - wholeSecs;
        long nanos = (long) (frac * 1_000_000_000L);
        Instant instant = Instant.ofEpochSecond(wholeSecs, nanos);
        OffsetTime expected = OffsetTime.ofInstant(instant, converter.getOptions().getZoneId());
        OffsetTime actual = DoubleConversions.toOffsetTime(seconds, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToOffsetDateTime() {
        Double d = 1_640_995_200.0;
        OffsetDateTime expected = Instant.ofEpochSecond(1_640_995_200L)
                .atZone(converter.getOptions().getZoneId())
                .toOffsetDateTime();
        OffsetDateTime actual = DoubleConversions.toOffsetDateTime(d, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToTimestamp() {
        Double d = 1_640_995_200.0;
        Timestamp expected = Timestamp.from(Instant.ofEpochSecond(1_640_995_200L));
        Timestamp actual = DoubleConversions.toTimestamp(d, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToDuration() {
        Double d = 123.456789012; // 123 s + 456 789 012 ns
        Duration expected = Duration.ofSeconds(123, 456_789_012L);
        Duration actual = DoubleConversions.toDuration(d, converter);
        assertEquals(expected, actual);
    }

    /* --------------------------------------------------------------
     *  EDGE‑CASE / EXCEPTION TESTS
     * -------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTimeNegative() {
        Double d = -0.1;
        DoubleConversions.toLocalTime(d, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTimeTooLarge() {
        Double d = 86_400.0; // exactly 24 h → out of range
        DoubleConversions.toLocalTime(d, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToOffsetTimeNegative() {
        Double d = -5.0;
        DoubleConversions.toOffsetTime(d, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToOffsetTimeTooLarge() {
        Double d = 86_400.0;
        DoubleConversions.toOffsetTime(d, converter);
    }

    @Test(expected = DateTimeException.class)
    public void testToInstantNaN() {
        Double d = Double.NaN;
        DoubleConversions.toInstant(d, converter);
    }

    @Test(expected = DateTimeException.class)
    public void testToInstantInfinity() {
        Double d = Double.POSITIVE_INFINITY;
        DoubleConversions.toInstant(d, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToInstantNull() {
        DoubleConversions.toInstant(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToDateNull() {
        DoubleConversions.toDate(null, converter);
    }

    @Test
    public void testToDateVeryLargeValue() {
        // 9 223 372 036 854 775.807 seconds ≈ Long.MAX_VALUE ms
        Double d = (double) Long.MAX_VALUE / 1000.0;
        Date date = DoubleConversions.toDate(d, converter);
        // The constructor will overflow to a negative epoch‑millis; we only verify that no exception is thrown
        assertNotNull(date);
    }

    @Test
    public void testToDurationRounding() {
        // 0.9999999999 seconds → should become 0 s + 999 999 999 ns (rounded down)
        Double d = 0.9999999999;
        Duration expected = Duration.ofSeconds(0, 999_999_999L);
        Duration actual = DoubleConversions.toDuration(d, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToSqlDateBeforeEpoch() {
        // 1960‑01‑01 → negative epoch seconds
        Double d = -315_619_200.0; // 1960‑01‑01T00:00:00Z
        java.sql.Date sqlDate = DoubleConversions.toSqlDate(d, converter);
        LocalDate expected = Instant.ofEpochSecond(-315_619_200L)
                .atZone(converter.getOptions().getZoneId())
                .toLocalDate();
        assertEquals(java.sql.Date.valueOf(expected), sqlDate);
    }
}
