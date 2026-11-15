package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.*;

public class DoubleConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // The library provides a ConverterOptions class (not shown). We assume it has a default
        // constructor that yields a UTC ZoneId / TimeZone, which is sufficient for deterministic tests.
        ConverterOptions options = new ConverterOptions();
        this.converter = new Converter(options);
    }

    /* -------------------------------------------------
     * toInstant
     * ------------------------------------------------- */
    @Test
    public void testToInstant_WholeSeconds() {
        Double value = 12345.0;
        Instant expected = Instant.ofEpochSecond(12345L, 0L);
        Instant actual = DoubleConversions.toInstant(value, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToInstant_WithFraction_RoundingDown() {
        Double value = 10.123456789; // fraction = .123456789 * 1_000_000_000 = 123,456,789
        Instant expected = Instant.ofEpochSecond(10L, 123_456_789L);
        Instant actual = DoubleConversions.toInstant(value, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToInstant_WithFraction_RoundingUp() {
        // fraction that should round up to the next nanosecond
        Double value = 20.9999999996; // .9999999996 * 1_000_000_000 = 999,999,999.6 → rounds to 1,000,000,000
        // The rounding pushes nanos to 1_000_000_000 which is equivalent to adding one second.
        Instant expected = Instant.ofEpochSecond(21L, 0L);
        Instant actual = DoubleConversions.toInstant(value, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToInstant_NegativeValue() {
        Double value = -5.5;
        // -5 seconds and -0.5 fraction → nanoseconds = round((-0.5) * 1e9) = -500,000,000
        // Instant normalises: -5 seconds - 500,000,000 nanos = -6 seconds + 500,000,000 nanos
        Instant expected = Instant.ofEpochSecond(-6L, 500_000_000L);
        Instant actual = DoubleConversions.toInstant(value, converter);
        assertEquals(expected, actual);
    }

    /* -------------------------------------------------
     * toDate
     * ------------------------------------------------- */
    @Test
    public void testToDate_Epoch() {
        Double value = 0.0;
        Date expected = new Date(0L);
        Date actual = DoubleConversions.toDate(value, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToDate_WithFraction_TruncatesMilliseconds() {
        Double value = 1.999; // 1999 ms → truncated to 1999 ms (long cast)
        Date expected = new Date(1999L);
        Date actual = DoubleConversions.toDate(value, converter);
        assertEquals(expected, actual);
    }

    /* -------------------------------------------------
     * toSqlDate
     * ------------------------------------------------- */
    @Test
    public void testToSqlDate_Basic() {
        Double value = 86400.0; // one day in seconds
        java.sql.Date expected = new java.sql.Date(86_400_000L);
        java.sql.Date actual = DoubleConversions.toSqlDate(value, converter);
        assertEquals(expected, actual);
    }

    /* -------------------------------------------------
     * toCalendar
     * ------------------------------------------------- */
    @Test
    public void testToCalendar_Basic() {
        Double seconds = 1609459200.0; // 2021-01-01T00:00:00Z
        Calendar cal = DoubleConversions.toCalendar(seconds, converter);
        assertEquals(1609459200_000L, cal.getTimeInMillis());

        // Verify that the calendar fields correspond to the UTC date
        assertEquals(2021, cal.get(Calendar.YEAR));
        assertEquals(Calendar.JANUARY, cal.get(Calendar.MONTH));
        assertEquals(1, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, cal.get(Calendar.MINUTE));
        assertEquals(0, cal.get(Calendar.SECOND));
    }

    /* -------------------------------------------------
     * toLocalTime
     * ------------------------------------------------- */
    @Test
    public void testToLocalTime_Midnight() {
        Double seconds = 0.0;
        LocalTime expected = LocalTime.MIDNIGHT;
        LocalTime actual = DoubleConversions.toLocalTime(seconds, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLocalTime_Noon() {
        Double seconds = 12 * 60 * 60.0; // 43200 seconds
        LocalTime expected = LocalTime.NOON;
        LocalTime actual = DoubleConversions.toLocalTime(seconds, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLocalTime_WithNanoseconds() {
        Double seconds = 1.123456789; // 1 second + 123,456,789 ns
        LocalTime expected = LocalTime.ofNanoOfDay(1_123_456_789L);
        LocalTime actual = DoubleConversions.toLocalTime(seconds, converter);
        assertEquals(expected, actual);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTime_NegativeThrows() {
        Double seconds = -1.0;
        DoubleConversions.toLocalTime(seconds, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTime_ExceedsDayThrows() {
        Double seconds = 86_400.0; // exactly 24 hours, out of range (max 86399.999...)
        DoubleConversions.toLocalTime(seconds, converter);
    }

    /* -------------------------------------------------
     * toZonedDateTime / toOffsetDateTime / toLocalDate / toLocalDateTime
     * ------------------------------------------------- */
    @Test
    public void testZonedAndOffsetDateTimeConversions() {
        Double epochSeconds = 0.0; // Unix epoch
        ZonedDateTime zdt = DoubleConversions.toZonedDateTime(epochSeconds, converter);
        OffsetDateTime odt = DoubleConversions.toOffsetDateTime(epochSeconds, converter);

        // Both should represent the same instant
        assertEquals(Instant.EPOCH, zdt.toInstant());
        assertEquals(Instant.EPOCH, odt.toInstant());

        // Zone should be the one supplied by ConverterOptions
        ZoneId expectedZone = converter.getOptions().getZoneId();
        assertEquals(expectedZone, zdt.getZone());
        assertEquals(expectedZone, odt.getOffset().getId().equals("Z") ? ZoneOffset.UTC : odt.getOffset());
    }

    @Test
    public void testLocalDateAndLocalDateTime() {
        // 2022-03-15T10:20:30Z
        Double epochSeconds = 1647349230.0;
        LocalDate expectedDate = LocalDate.of(2022, 3, 15);
        LocalDateTime expectedDateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(1647349230L), converter.getOptions().getZoneId());

        assertEquals(expectedDate, DoubleConversions.toLocalDate(epochSeconds, converter));
        assertEquals(expectedDateTime, DoubleConversions.toLocalDateTime(epochSeconds, converter));
    }

    /* -------------------------------------------------
     * toTimestamp
     * ------------------------------------------------- */
    @Test
    public void testToTimestamp() {
        Double epochSeconds = 1622548800.123; // 2021-06-01T12:00:00.123Z
        Timestamp ts = DoubleConversions.toTimestamp(epochSeconds, converter);
        Instant expected = DoubleConversions.toInstant(epochSeconds, converter);
        assertEquals(expected, ts.toInstant());
    }

    /* -------------------------------------------------
     * toDuration
     * ------------------------------------------------- */
    @Test
    public void testToDuration_WholeSeconds() {
        Double value = 30.0;
        Duration expected = Duration.ofSeconds(30L, 0L);
        Duration actual = DoubleConversions.toDuration(value, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToDuration_WithFraction() {
        Double value = 5.75; // 5 seconds + 0.75 * 1_000_000_000 = 750,000,000 ns
        Duration expected = Duration.ofSeconds(5L, 750_000_000L);
        Duration actual = DoubleConversions.toDuration(value, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToDuration_NegativeValue() {
        Double value = -2.5; // -2 seconds -0.5 fraction → -3 seconds + 500,000,000 ns
        Duration expected = Duration.ofSeconds(-3L, 500_000_000L);
        Duration actual = DoubleConversions.toDuration(value, converter);
        assertEquals(expected, actual);
    }
}
