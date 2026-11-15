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
        // Assume ConverterOptions has a public no‑arg constructor.
        // The default options (system time‑zone) are sufficient for deterministic tests
        // because we always derive the expected value from the same options instance.
        converter = new Converter(new ConverterOptions());
    }

    /* -------------------------------------------------
     *  toInstant
     * ------------------------------------------------- */
    @Test
    public void testToInstant_PositiveWithFraction() {
        double seconds = 1622547801.123456789; // arbitrary epoch seconds with fraction
        Instant expected = Instant.ofEpochSecond(
                (long) seconds,
                Math.round((seconds - (long) seconds) * 1_000_000_000L));
        Instant actual = DoubleConversions.toInstant(seconds, converter);
        assertEquals("Instant conversion with fraction failed", expected, actual);
    }

    @Test
    public void testToInstant_NegativeWithFraction() {
        double seconds = -42.987654321;
        Instant expected = Instant.ofEpochSecond(
                (long) seconds,
                Math.round((seconds - (long) seconds) * 1_000_000_000L));
        Instant actual = DoubleConversions.toInstant(seconds, converter);
        assertEquals("Instant conversion for negative value failed", expected, actual);
    }

    /* -------------------------------------------------
     *  toDate
     * ------------------------------------------------- */
    @Test
    public void testToDate() {
        double seconds = 1622547801.75; // 0.75 sec = 750 ms
        long expectedMillis = (long) (seconds * 1000);
        Date expected = new Date(expectedMillis);
        Date actual = DoubleConversions.toDate(seconds, converter);
        assertEquals("Date conversion failed", expected, actual);
    }

    /* -------------------------------------------------
     *  toSqlDate
     * ------------------------------------------------- */
    @Test
    public void testToSqlDate() {
        double seconds = 1622547801.25; // 0.25 sec = 250 ms
        long expectedMillis = (long) (seconds * 1000);
        java.sql.Date expected = new java.sql.Date(expectedMillis);
        java.sql.Date actual = DoubleConversions.toSqlDate(seconds, converter);
        assertEquals("SQL Date conversion failed", expected, actual);
    }

    /* -------------------------------------------------
     *  toCalendar
     * ------------------------------------------------- */
    @Test
    public void testToCalendar() {
        double seconds = 12345.678; // arbitrary
        long expectedMillis = (long) (seconds * 1000);
        Calendar actual = DoubleConversions.toCalendar(seconds, converter);
        assertEquals("Calendar epoch millis mismatch", expectedMillis, actual.getTimeInMillis());
    }

    /* -------------------------------------------------
     *  toLocalTime
     * ------------------------------------------------- */
    @Test
    public void testToLocalTime_NormalValue() {
        double seconds = 3600.5; // 01:00:00.500
        long nanosOfDay = (long) (seconds * 1_000_000_000L);
        LocalTime expected = LocalTime.ofNanoOfDay(nanosOfDay);
        LocalTime actual = DoubleConversions.toLocalTime(seconds, converter);
        assertEquals("LocalTime conversion failed", expected, actual);
    }

    @Test
    public void testToLocalTime_EdgeMaximum() {
        // Maximum allowed value: 86 399.999 999 999 (just before 24 h)
        double seconds = 86_399.999_999_999;
        long nanosOfDay = (long) (seconds * 1_000_000_000L);
        LocalTime expected = LocalTime.ofNanoOfDay(nanosOfDay);
        LocalTime actual = DoubleConversions.toLocalTime(seconds, converter);
        assertEquals("LocalTime conversion at upper edge failed", expected, actual);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTime_OutOfRange_TooLarge() {
        double seconds = 86_400.0; // exactly 24 h – out of range
        DoubleConversions.toLocalTime(seconds, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTime_OutOfRange_Negative() {
        double seconds = -0.1;
        DoubleConversions.toLocalTime(seconds, converter);
    }

    /* -------------------------------------------------
     *  toZonedDateTime / toLocalDate / toLocalDateTime / toOffsetDateTime
     * ------------------------------------------------- */
    @Test
    public void testZonedDateTimeAndRelatedConversions() {
        double seconds = 1609459200.0; // 2021‑01‑01T00:00:00Z
        Instant instant = DoubleConversions.toInstant(seconds, converter);
        ZoneId zoneId = converter.getOptions().getZoneId();

        ZonedDateTime expectedZdt = instant.atZone(zoneId);
        ZonedDateTime actualZdt = DoubleConversions.toZonedDateTime(seconds, converter);
        assertEquals("ZonedDateTime conversion mismatch", expectedZdt, actualZdt);

        LocalDate expectedDate = expectedZdt.toLocalDate();
        LocalDate actualDate = DoubleConversions.toLocalDate(seconds, converter);
        assertEquals("LocalDate conversion mismatch", expectedDate, actualDate);

        LocalDateTime expectedDateTime = expectedZdt.toLocalDateTime();
        LocalDateTime actualDateTime = DoubleConversions.toLocalDateTime(seconds, converter);
        assertEquals("LocalDateTime conversion mismatch", expectedDateTime, actualDateTime);

        OffsetDateTime expectedOffset = expectedZdt.toOffsetDateTime();
        OffsetDateTime actualOffset = DoubleConversions.toOffsetDateTime(seconds, converter);
        assertEquals("OffsetDateTime conversion mismatch", expectedOffset, actualOffset);
    }

    /* -------------------------------------------------
     *  toTimestamp
     * ------------------------------------------------- */
    @Test
    public void testToTimestamp() {
        double seconds = 1622547801.123; // arbitrary
        Instant instant = DoubleConversions.toInstant(seconds, converter);
        Timestamp expected = Timestamp.from(instant);
        Timestamp actual = DoubleConversions.toTimestamp(seconds, converter);
        assertEquals("Timestamp conversion failed", expected, actual);
    }

    /* -------------------------------------------------
     *  toDuration
     * ------------------------------------------------- */
    @Test
    public void testToDuration_WithFraction() {
        double seconds = 123.456789012;
        long wholeSeconds = (long) seconds;
        long nanos = (long) ((seconds - wholeSeconds) * 1_000_000_000L);
        Duration expected = Duration.ofSeconds(wholeSeconds, nanos);
        Duration actual = DoubleConversions.toDuration(seconds, converter);
        assertEquals("Duration conversion with fraction failed", expected, actual);
    }

    @Test
    public void testToDuration_WholeSecondsOnly() {
        double seconds = 42.0;
        Duration expected = Duration.ofSeconds(42L, 0L);
        Duration actual = DoubleConversions.toDuration(seconds, converter);
        assertEquals("Duration conversion for whole seconds failed", expected, actual);
    }

    @Test
    public void testToDuration_NegativeValue() {
        double seconds = -5.25;
        long wholeSeconds = (long) seconds; // -5
        long nanos = (long) ((seconds - wholeSeconds) * 1_000_000_000L); // -0.25 * 1e9 = -250_000_000
        Duration expected = Duration.ofSeconds(wholeSeconds, nanos);
        Duration actual = DoubleConversions.toDuration(seconds, converter);
        assertEquals("Duration conversion for negative value failed", expected, actual);
    }
}
