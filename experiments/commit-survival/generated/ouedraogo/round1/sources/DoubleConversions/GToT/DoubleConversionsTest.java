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
        // ConverterOptions is part of the library; we assume it has a public no‑arg constructor
        // that sets sensible defaults (UTC time zone, etc.).
        ConverterOptions options = new ConverterOptions();
        this.converter = new Converter(options);
    }

    /* ----------------------------------------------------------------------
     *  toInstant
     * ---------------------------------------------------------------------- */
    @Test
    public void testToInstant_Basic() {
        Double input = 1.5;                       // 1 second + 0.5 s
        Instant result = DoubleConversions.toInstant(input, converter);
        assertEquals(1L, result.getEpochSecond());
        assertEquals(500_000_000L, result.getNano());
    }

    @Test
    public void testToInstant_Negative() {
        Double input = -2.25;                     // -2 s – 250 ms
        Instant result = DoubleConversions.toInstant(input, converter);
        assertEquals(-2L, result.getEpochSecond());
        // The nanosecond part is always positive; Instant normalises the sign.
        assertEquals(750_000_000L, result.getNano());
    }

    /* ----------------------------------------------------------------------
     *  toDate
     * ---------------------------------------------------------------------- */
    @Test
    public void testToDate_Basic() {
        Double input = 1.234;                    // 1.234 s → 1234 ms
        Date result = DoubleConversions.toDate(input, converter);
        assertEquals(1234L, result.getTime());
    }

    @Test
    public void testToDate_RoundDown() {
        Double input = 0.9999;                    // 0.9999 s → 999 ms (truncates)
        Date result = DoubleConversions.toDate(input, converter);
        assertEquals(999L, result.getTime());
    }

    /* ----------------------------------------------------------------------
     *  toSqlDate
     * ---------------------------------------------------------------------- */
    @Test
    public void testToSqlDate_Basic() {
        Double input = 86400.0;                  // exactly one day
        java.sql.Date result = (java.sql.Date) DoubleConversions.toSqlDate(input, converter);
        // java.sql.Date stores the date part only; the epoch day for 86400 s is Jan 2 1970
        assertEquals(86400000L, result.getTime());
    }

    /* ----------------------------------------------------------------------
     *  toCalendar
     * ---------------------------------------------------------------------- */
    @Test
    public void testToCalendar_Basic() {
        Double input = 12345.678;                // seconds → ms
        Calendar cal = DoubleConversions.toCalendar(input, converter);
        assertEquals(12345678L, cal.getTimeInMillis());
    }

    @Test
    public void testToCalendar_TimeZone() {
        // Ensure the calendar respects the Converter's time zone (default is UTC)
        Double input = 0.0;
        Calendar cal = DoubleConversions.toCalendar(input, converter);
        assertEquals(0L, cal.getTimeInMillis());
        assertEquals(converter.getOptions().getTimeZone(), cal.getTimeZone());
    }

    /* ----------------------------------------------------------------------
     *  toLocalTime
     * ---------------------------------------------------------------------- */
    @Test
    public void testToLocalTime_Basic() {
        Double input = 3661.123456789;            // 1 h 1 m 1.123456789 s
        LocalTime lt = DoubleConversions.toLocalTime(input, converter);
        assertEquals(LocalTime.of(1, 1, 1, 123_456_789), lt);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTime_Negative() {
        Double input = -1.0;
        DoubleConversions.toLocalTime(input, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTime_TooLarge() {
        // 24 h = 86400 s; the allowed max is 86399.999999999
        Double input = 86400.0;
        DoubleConversions.toLocalTime(input, converter);
    }

    /* ----------------------------------------------------------------------
     *  toLocalDate
     * ---------------------------------------------------------------------- */
    @Test
    public void testToLocalDate_Basic() {
        // 2022‑03‑14T15:09:26Z  → 2022‑03‑14
        Double epochSeconds = 1647276566.0;
        LocalDate ld = DoubleConversions.toLocalDate(epochSeconds, converter);
        assertEquals(LocalDate.of(2022, 3, 14), ld);
    }

    /* ----------------------------------------------------------------------
     *  toLocalDateTime
     * ---------------------------------------------------------------------- */
    @Test
    public void testToLocalDateTime_Basic() {
        Double epochSeconds = 1647276566.789;     // 2022‑03‑14T15:09:26.789Z
        LocalDateTime ldt = DoubleConversions.toLocalDateTime(epochSeconds, converter);
        assertEquals(LocalDateTime.of(2022, 3, 14, 15, 9, 26, 789_000_000), ldt);
    }

    /* ----------------------------------------------------------------------
     *  toZonedDateTime
     * ---------------------------------------------------------------------- */
    @Test
    public void testToZonedDateTime_Basic() {
        Double epochSeconds = 0.0;              // epoch start
        ZonedDateTime zdt = DoubleConversions.toZonedDateTime(epochSeconds, converter);
        ZoneId expectedZone = converter.getOptions().getZoneId();
        assertEquals(ZonedDateTime.ofInstant(Instant.EPOCH, expectedZone), zdt);
    }

    /* ----------------------------------------------------------------------
     *  toOffsetDateTime
     * ---------------------------------------------------------------------- */
    @Test
    public void testToOffsetDateTime_Basic() {
        Double epochSeconds = 0.0;
        OffsetDateTime odt = DoubleConversions.toOffsetDateTime(epochSeconds, converter);
        ZoneOffset expectedOffset = converter.getOptions().getZoneId().getRules().getOffset(Instant.EPOCH);
        assertEquals(OffsetDateTime.ofInstant(Instant.EPOCH, expectedOffset), odt);
    }

    /* ----------------------------------------------------------------------
     *  toTimestamp
     * ---------------------------------------------------------------------- */
    @Test
    public void testToTimestamp_Basic() {
        Double epochSeconds = 1625097600.123;    // 2021‑07‑01T00:00:00.123Z
        Timestamp ts = DoubleConversions.toTimestamp(epochSeconds, converter);
        assertEquals(1625097600123L, ts.getTime());
        assertEquals(123_000_000, ts.getNanos());
    }

    /* ----------------------------------------------------------------------
     *  toDuration
     * ---------------------------------------------------------------------- */
    @Test
    public void testToDuration_Basic() {
        Double input = 12345.6789;                // 12345 s + 678 ms
        Duration d = DoubleConversions.toDuration(input, converter);
        assertEquals(12345L, d.getSeconds());
        assertEquals(678_900_000L, d.getNano());
    }

    @Test
    public void testToDuration_Negative() {
        Double input = -10.5;                    // -10 s – 500 ms
        Duration d = DoubleConversions.toDuration(input, converter);
        // Duration normalises the sign: -10 s - 500 ms = -10.5 s
        assertEquals(-10L, d.getSeconds());
        assertEquals(-500_000_000, d.getNano());
    }
}
