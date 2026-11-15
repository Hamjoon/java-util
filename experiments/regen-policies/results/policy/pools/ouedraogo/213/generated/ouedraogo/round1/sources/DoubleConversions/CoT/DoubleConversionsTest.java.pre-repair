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

    private Converter converter;

    @Before
    public void setUp() {
        // Build a Converter with deterministic UTC options.
        // The real library provides a mutable ConverterOptions – we assume it has a public no‑arg
        // constructor and setters for the zone and time‑zone.
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(ZoneId.of("UTC"));
        options.setTimeZone(TimeZone.getTimeZone("UTC"));
        converter = new Converter(options);
    }

    /* --------------------------------------------------------------------- *
     *  Tests for toInstant()
     * --------------------------------------------------------------------- */

    @Test
    public void testToInstant_BasicPositive() {
        Double input = 12345.678;                     // 12345 seconds + 0.678 sec
        Instant expected = Instant.ofEpochSecond(12345L, 678_000_000L);
        Instant actual = DoubleConversions.toInstant(input, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToInstant_NegativeValue() {
        Double input = -10.5;                        // -10 seconds - 0.5 sec
        // The fractional part -0.5 is multiplied and rounded → -500_000_000 nanos
        Instant expected = Instant.ofEpochSecond(-10L, -500_000_000L);
        Instant actual = DoubleConversions.toInstant(input, converter);
        assertEquals(expected, actual);
    }

    /* --------------------------------------------------------------------- *
     *  Tests for toDate()
     * --------------------------------------------------------------------- */

    @Test
    public void testToDate_Basic() {
        Double input = 1.234;                        // 1.234 seconds → 1234 ms
        Date expected = new Date(1234L);
        Date actual = DoubleConversions.toDate(input, converter);
        assertEquals(expected, actual);
    }

    /* --------------------------------------------------------------------- *
     *  Tests for toSqlDate()
     * --------------------------------------------------------------------- */

    @Test
    public void testToSqlDate_ExactMidnight() {
        Double input = 0.0;                         // epoch start
        java.sql.Date expected = java.sql.Date.valueOf(LocalDate.ofEpochDay(0));
        java.sql.Date actual = DoubleConversions.toSqlDate(input, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToSqlDate_OneDayLater() {
        Double input = 86_400.0;                     // 24 h → 1970‑01‑02 (UTC)
        java.sql.Date expected = java.sql.Date.valueOf(LocalDate.of(1970, 1, 2));
        java.sql.Date actual = DoubleConversions.toSqlDate(input, converter);
        assertEquals(expected, actual);
    }

    /* --------------------------------------------------------------------- *
     *  Tests for toCalendar()
     * --------------------------------------------------------------------- */

    @Test
    public void testToCalendar_Basic() {
        Double input = 0.5;                          // 0.5 s → 500 ms
        Calendar cal = DoubleConversions.toCalendar(input, converter);
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
        assertEquals(500L, cal.getTimeInMillis());
    }

    /* --------------------------------------------------------------------- *
     *  Tests for toLocalTime()
     * --------------------------------------------------------------------- */

    @Test
    public void testToLocalTime_ValidMidday() {
        // 12 h 30 m 15.250 s → 45 210.250 s since start of day
        Double input = 12 * 3600 + 30 * 60 + 15.250; // 45015.25 seconds
        LocalTime expected = LocalTime.of(12, 30, 15, 250_000_000);
        LocalTime actual = DoubleConversions.toLocalTime(input, converter);
        assertEquals(expected, actual);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTime_Negative() {
        Double input = -1.0;
        DoubleConversions.toLocalTime(input, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTime_Over24Hours() {
        Double input = 86_400.0; // exactly 24 h – out of range (must be <= 86399.999999999)
        DoubleConversions.toLocalTime(input, converter);
    }

    /* --------------------------------------------------------------------- *
     *  Tests for toOffsetTime()
     * --------------------------------------------------------------------- */

    @Test
    public void testToOffsetTime_Valid() {
        // 01:02:03.004005006 (seconds since midnight = 3723.004005006)
        Double input = 1 * 3600 + 2 * 60 + 3.004005006;
        OffsetTime actual = DoubleConversions.toOffsetTime(input, converter);
        OffsetTime expected = OffsetTime.ofInstant(
                Instant.ofEpochSecond(3723L, 4_005_006L), // note: nanos from fractional part
                ZoneId.of("UTC"));
        assertEquals(expected, actual);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToOffsetTime_Negative() {
        Double input = -0.1;
        DoubleConversions.toOffsetTime(input, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToOffsetTime_Over24Hours() {
        Double input = 86_400.0;
        DoubleConversions.toOffsetTime(input, converter);
    }

    /* --------------------------------------------------------------------- *
     *  Tests for toLocalDate(), toLocalDateTime(), toZonedDateTime(),
     *  toOffsetDateTime() – they are thin wrappers around toInstant()
     * --------------------------------------------------------------------- */

    @Test
    public void testToLocalDate_Basic() {
        Double input = 86_400.0; // 1970‑01‑02 UTC
        LocalDate expected = LocalDate.of(1970, 1, 2);
        LocalDate actual = DoubleConversions.toLocalDate(input, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLocalDateTime_Basic() {
        Double input = 86_400.5; // 1970‑01‑02T00:00:00.5Z
        LocalDateTime expected = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(86_400L, 500_000_000L),
                ZoneId.of("UTC"));
        LocalDateTime actual = DoubleConversions.toLocalDateTime(input, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToZonedDateTime_Basic() {
        Double input = 0.0;
        ZonedDateTime expected = Instant.EPOCH.atZone(ZoneId.of("UTC"));
        ZonedDateTime actual = DoubleConversions.toZonedDateTime(input, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToOffsetDateTime_Basic() {
        Double input = 0.0;
        OffsetDateTime expected = Instant.EPOCH.atZone(ZoneId.of("UTC")).toOffsetDateTime();
        OffsetDateTime actual = DoubleConversions.toOffsetDateTime(input, converter);
        assertEquals(expected, actual);
    }

    /* --------------------------------------------------------------------- *
     *  Tests for toTimestamp()
     * --------------------------------------------------------------------- */

    @Test
    public void testToTimestamp_Basic() {
        Double input = 123.456; // 123 seconds + 456 ms
        Instant instant = Instant.ofEpochSecond(123L, 456_000_000L);
        Timestamp expected = Timestamp.from(instant);
        Timestamp actual = DoubleConversions.toTimestamp(input, converter);
        assertEquals(expected, actual);
    }

    /* --------------------------------------------------------------------- *
     *  Tests for toDuration()
     * --------------------------------------------------------------------- */

    @Test
    public void testToDuration_Basic() {
        Double input = 5.75; // 5 s + 750 ms
        Duration expected = Duration.ofSeconds(5L, 750_000_000L);
        Duration actual = DoubleConversions.toDuration(input, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToDuration_Negative() {
        Double input = -2.3; // negative duration is allowed
        Duration expected = Duration.ofSeconds(-2L, -300_000_000L);
        Duration actual = DoubleConversions.toDuration(input, converter);
        assertEquals(expected, actual);
    }
}
