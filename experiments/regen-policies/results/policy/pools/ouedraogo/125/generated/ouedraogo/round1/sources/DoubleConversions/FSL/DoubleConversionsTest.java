package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.junit.Test;

/**
 * JUnit 4 test suite that exercises every public static method in
 * {@link DoubleConversions}. The tests use real {@link Converter} and
 * {@link ConverterOptions} objects (the library supplies a default constructor
 * for {@code ConverterOptions}) and rely only on the API surface that is
 * visible in the supplied source code and the {@code Converter} signature.
 *
 * The default {@code ConverterOptions} implementation uses the JVM default
 * time‑zone; the tests compute expected results using the same zone obtained
 * from {@code converter.getOptions().getZoneId()} so the suite is deterministic
 * regardless of the host environment.
 */
public class DoubleConversionsTest {

    /** Helper that creates a {@link Converter} with the library’s default options. */
    private static Converter createConverter() {
        // The library provides a no‑arg constructor for ConverterOptions.
        // If a different constructor is required, adapt accordingly.
        return new Converter(new ConverterOptions());
    }

    @Test
    public void testToInstantWholeAndFractionalSeconds() {
        Converter converter = createConverter();

        // 2021‑01‑01T00:00:00.123456789Z  -> epoch seconds = 1609459200
        double value = 1609459200.123456789;
        Instant expected = Instant.ofEpochSecond(1609459200L, 123_456_789L);
        Instant actual = DoubleConversions.toInstant(value, converter);

        assertEquals("Instant seconds part", expected.getEpochSecond(), actual.getEpochSecond());
        assertEquals("Instant nanoseconds part", expected.getNano(), actual.getNano());
    }

    @Test
    public void testToInstantZero() {
        Converter converter = createConverter();
        double value = 0.0;
        Instant expected = Instant.ofEpochSecond(0L, 0L);
        assertEquals(expected, DoubleConversions.toInstant(value, converter));
    }

    @Test
    public void testToDate() {
        Converter converter = createConverter();

        double seconds = 1_234_567.89; // 1,234,567.89 seconds after epoch
        Date expected = new Date((long) (seconds * 1000));
        Date actual = DoubleConversions.toDate(seconds, converter);

        assertEquals(expected.getTime(), actual.getTime());
    }

    @Test
    public void testToSqlDate() {
        Converter converter = createConverter();

        double seconds = 9_876_543.21;
        java.sql.Date expected = new java.sql.Date((long) (seconds * 1000));
        java.sql.Date actual = (java.sql.Date) DoubleConversions.toSqlDate(seconds, converter);

        assertEquals(expected.getTime(), actual.getTime());
    }

    @Test
    public void testToCalendar() {
        Converter converter = createConverter();

        double seconds = 3_600.5; // 1 hour and half a second after epoch
        long expectedMillis = (long) (seconds * 1000);
        Calendar calendar = DoubleConversions.toCalendar(seconds, converter);

        assertEquals("Epoch milliseconds", expectedMillis, calendar.getTimeInMillis());
        // Verify that the calendar uses the same time‑zone as the converter options
        TimeZone expectedTZ = converter.getOptions().getTimeZone();
        assertEquals(expectedTZ.getID(), calendar.getTimeZone().getID());
    }

    @Test
    public void testToLocalTimeTypical() {
        Converter converter = createConverter();

        // 1 hour, 2 minutes, 3.456789012 seconds => total seconds = 3723.456789012
        double seconds = 1 * 3600 + 2 * 60 + 3.456789012;
        LocalTime expected = LocalTime.of(1, 2, 3, 456_789_012);
        LocalTime actual = DoubleConversions.toLocalTime(seconds, converter);

        assertEquals(expected, actual);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTimeNegative() {
        Converter converter = createConverter();
        DoubleConversions.toLocalTime(-0.1, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTimeTooLarge() {
        Converter converter = createConverter();
        // 24 hours = 86400 seconds – not allowed (must be <= 86399.999999999)
        DoubleConversions.toLocalTime(86400.0, converter);
    }

    @Test
    public void testToLocalDateAndLocalDateTimeAndZonedDateTimeAndOffsetDateTime() {
        Converter converter = createConverter();

        // Use a known epoch second that falls on a non‑DST date to keep expectations simple.
        double epochSeconds = 1_600_000_000.0; // 2020‑09‑13T12:26:40Z
        Instant instant = DoubleConversions.toInstant(epochSeconds, converter);
        ZoneId zone = converter.getOptions().getZoneId();

        ZonedDateTime expectedZoned = instant.atZone(zone);
        OffsetDateTime expectedOffset = expectedZoned.toOffsetDateTime();
        LocalDate expectedDate = expectedZoned.toLocalDate();
        LocalDateTime expectedDateTime = expectedZoned.toLocalDateTime();

        assertEquals(expectedZoned, DoubleConversions.toZonedDateTime(epochSeconds, converter));
        assertEquals(expectedOffset, DoubleConversions.toOffsetDateTime(epochSeconds, converter));
        assertEquals(expectedDate, DoubleConversions.toLocalDate(epochSeconds, converter));
        assertEquals(expectedDateTime, DoubleConversions.toLocalDateTime(epochSeconds, converter));
    }

    @Test
    public void testToTimestamp() {
        Converter converter = createConverter();

        double epochSeconds = 1622548800.0; // 2021‑06‑01T12:00:00Z
        Instant instant = DoubleConversions.toInstant(epochSeconds, converter);
        Timestamp expected = Timestamp.from(instant);
        Timestamp actual = DoubleConversions.toTimestamp(epochSeconds, converter);

        assertEquals(expected.getTime(), actual.getTime());
        assertEquals(expected.getNanos(), actual.getNanos());
    }

    @Test
    public void testToDurationWholeAndFractional() {
        Converter converter = createConverter();

        double value = 12345.678901234; // 12345 seconds + 678,901,234 nanoseconds
        long expectedSeconds = 12_345L;
        long expectedNanos = 678_901_234L;
        Duration expected = Duration.ofSeconds(expectedSeconds, expectedNanos);
        Duration actual = DoubleConversions.toDuration(value, converter);

        assertEquals(expected, actual);
    }

    @Test
    public void testToDurationZero() {
        Converter converter = createConverter();

        double value = 0.0;
        Duration expected = Duration.ofSeconds(0, 0);
        assertEquals(expected, DoubleConversions.toDuration(value, converter));
    }
}
