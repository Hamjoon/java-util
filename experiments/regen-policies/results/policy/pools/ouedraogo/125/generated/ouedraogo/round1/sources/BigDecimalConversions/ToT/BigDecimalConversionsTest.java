package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.Assert.*;

public class BigDecimalConversionsTest {

    private Converter converter;

    /**
     * The library’s {@code Converter} relies on the {@code ConverterOptions}
     * for time‑zone information.  The exact API of {@code ConverterOptions}
     * is not shown, but a default instance uses the JVM’s default time‑zone.
     * To make the tests deterministic we force the JVM default to UTC
     * before any test runs.
     */
    @Before
    public void setUp() {
        TimeZone utc = TimeZone.getTimeZone("UTC");
        TimeZone.setDefault(utc);
        // The library’s Converter has a constructor that accepts a ConverterOptions instance.
        // We use the no‑arg constructor of ConverterOptions (assumed to exist) to obtain defaults.
        this.converter = new Converter(new ConverterOptions());
    }

    /* -------------------------------------------------------------
     *  Calendar conversion
     * ------------------------------------------------------------- */
    @Test
    public void testToCalendar_Typical() {
        BigDecimal seconds = new BigDecimal("1.5"); // 1.5 seconds = 1500 ms
        Calendar cal = BigDecimalConversions.toCalendar(seconds, converter);
        assertEquals(1500L, cal.getTimeInMillis());
        // verify that the calendar uses the converter’s time‑zone (UTC)
        assertEquals(TimeZone.getDefault(), cal.getTimeZone());
    }

    /* -------------------------------------------------------------
     *  Instant conversion
     * ------------------------------------------------------------- */
    @Test
    public void testToInstant_Typical() {
        // 123.456789012 seconds = 123 s + 456 789 012 ns
        BigDecimal seconds = new BigDecimal("123.456789012");
        Instant instant = BigDecimalConversions.toInstant(seconds, converter);
        assertEquals(Instant.ofEpochSecond(123L, 456_789_012L), instant);
    }

    @Test
    public void testToInstant_Zero() {
        BigDecimal seconds = BigDecimal.ZERO;
        Instant instant = BigDecimalConversions.toInstant(seconds, converter);
        assertEquals(Instant.EPOCH, instant);
    }

    /* -------------------------------------------------------------
     *  Duration conversion
     * ------------------------------------------------------------- */
    @Test
    public void testToDuration_Typical() {
        BigDecimal seconds = new BigDecimal("10.000000001"); // 10 s + 1 ns
        Duration duration = BigDecimalConversions.toDuration(seconds, converter);
        assertEquals(Duration.ofSeconds(10L, 1L), duration);
    }

    @Test
    public void testToDuration_Negative() {
        BigDecimal seconds = new BigDecimal("-5.5"); // -5 s - 0.5 s
        Duration duration = BigDecimalConversions.toDuration(seconds, converter);
        // Duration stores a negative second count and a positive nanosecond part.
        assertEquals(Duration.ofSeconds(-6L, 500_000_000L), duration);
    }

    /* -------------------------------------------------------------
     *  LocalTime conversion
     * ------------------------------------------------------------- */
    @Test
    public void testToLocalTime_Typical() {
        // 0.5 seconds → 500 000 000 ns after midnight
        BigDecimal seconds = new BigDecimal("0.5");
        LocalTime time = BigDecimalConversions.toLocalTime(seconds, converter);
        assertEquals(LocalTime.ofNanoOfDay(500_000_000L), time);
    }

    @Test
    public void testToLocalTime_EdgeOfDay() {
        // 86399.999999999 seconds = last nanosecond of the day
        BigDecimal seconds = new BigDecimal("86399.999999999");
        LocalTime time = BigDecimalConversions.toLocalTime(seconds, converter);
        assertEquals(LocalTime.MAX, time);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTime_NegativeValue() {
        BigDecimal seconds = new BigDecimal("-0.1");
        BigDecimalConversions.toLocalTime(seconds, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTime_TooLargeValue() {
        // One second beyond the day limit
        BigDecimal seconds = new BigDecimal("86400");
        BigDecimalConversions.toLocalTime(seconds, converter);
    }

    /* -------------------------------------------------------------
     *  ZonedDateTime, LocalDate, LocalDateTime, OffsetDateTime
     * ------------------------------------------------------------- */
    @Test
    public void testToZonedDateTime_Typical() {
        // 0 seconds = epoch start
        BigDecimal seconds = BigDecimal.ZERO;
        ZonedDateTime zdt = BigDecimalConversions.toZonedDateTime(seconds, converter);
        assertEquals(Instant.EPOCH.atZone(ZoneId.systemDefault()), zdt);
    }

    @Test
    public void testToLocalDate_Typical() {
        // 86400 seconds = exactly one day after epoch → 1970‑01‑02
        BigDecimal seconds = new BigDecimal("86400");
        LocalDate date = BigDecimalConversions.toLocalDate(seconds, converter);
        assertEquals(LocalDate.of(1970, 1, 2), date);
    }

    @Test
    public void testToLocalDateTime_Typical() {
        // 1609459200 seconds = 2021‑01‑01T00:00:00Z
        BigDecimal seconds = new BigDecimal("1609459200");
        LocalDateTime ldt = BigDecimalConversions.toLocalDateTime(seconds, converter);
        assertEquals(LocalDateTime.ofInstant(Instant.ofEpochSecond(1609459200L), ZoneId.systemDefault()), ldt);
    }

    @Test
    public void testToOffsetDateTime_Typical() {
        // 0 seconds → 1970‑01‑01T00:00:00Z
        BigDecimal seconds = BigDecimal.ZERO;
        OffsetDateTime odt = BigDecimalConversions.toOffsetDateTime(seconds, converter);
        assertEquals(OffsetDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC), odt);
    }

    /* -------------------------------------------------------------
     *  java.util.Date, java.sql.Date, Timestamp
     * ------------------------------------------------------------- */
    @Test
    public void testToDate_Typical() {
        BigDecimal seconds = new BigDecimal("12345.678"); // 12345 s + 678 ms
        Date date = BigDecimalConversions.toDate(seconds, converter);
        long expectedMillis = 12_345_678L; // 12 345 678 ms
        assertEquals(new Date(expectedMillis), date);
    }

    @Test
    public void testToSqlDate_Typical() {
        BigDecimal seconds = new BigDecimal("86400"); // exactly one day
        java.sql.Date sqlDate = BigDecimalConversions.toSqlDate(seconds, converter);
        // java.sql.Date stores only the date part (midnight UTC)
        assertEquals(new java.sql.Date(86_400_000L), sqlDate);
    }

    @Test
    public void testToTimestamp_Typical() {
        BigDecimal seconds = new BigDecimal("1.001"); // 1 s + 1 ms
        Timestamp ts = BigDecimalConversions.toTimestamp(seconds, converter);
        assertEquals(Timestamp.from(Instant.ofEpochSecond(1L, 1_000_000L)), ts);
    }

    /* -------------------------------------------------------------
     *  BigInteger conversion
     * ------------------------------------------------------------- */
    @Test
    public void testToBigInteger_Typical() {
        BigDecimal value = new BigDecimal("12345.987");
        BigInteger result = BigDecimalConversions.toBigInteger(value, converter);
        assertEquals(BigInteger.valueOf(12345L), result); // fractional part is discarded
    }

    @Test
    public void testToBigInteger_Negative() {
        BigDecimal value = new BigDecimal("-9876.543");
        BigInteger result = BigDecimalConversions.toBigInteger(value, converter);
        assertEquals(BigInteger.valueOf(-9876L), result);
    }

    /* -------------------------------------------------------------
     *  String conversion
     * ------------------------------------------------------------- */
    @Test
    public void testToString_Typical() {
        BigDecimal value = new BigDecimal("123.45000");
        String str = BigDecimalConversions.toString(value, converter);
        assertEquals("123.45", str); // trailing zeros stripped
    }

    @Test
    public void testToString_ScientificNotation() {
        BigDecimal value = new BigDecimal("1E-7"); // 0.0000001
        String str = BigDecimalConversions.toString(value, converter);
        assertEquals("0.0000001", str);
    }

    /* -------------------------------------------------------------
     *  UUID conversion
     * ------------------------------------------------------------- */
    @Test
    public void testToUUID_Typical() {
        UUID original = UUID.randomUUID();

        // Convert UUID → BigInteger → BigDecimal
        BigInteger uuidBigInt = new BigInteger(original.toString().replace("-", ""), 16);
        BigDecimal decimal = new BigDecimal(uuidBigInt);

        UUID recovered = BigDecimalConversions.toUUID(decimal, converter);
        assertEquals(original, recovered);
    }

    @Test(expected = NumberFormatException.class)
    public void testToUUID_InvalidBigDecimal() {
        // A negative BigDecimal cannot be turned into a valid UUID
        BigDecimal negative = new BigDecimal("-1");
        BigDecimalConversions.toUUID(negative, converter);
    }

    /* -------------------------------------------------------------
     *  Helper static method: secondsAndNanosToDouble
     * ------------------------------------------------------------- */
    @Test
    public void testSecondsAndNanosToDouble_Typical() {
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(5L, 250_000_000L);
        assertEquals(new BigDecimal("5.25"), result);
    }

    @Test
    public void testSecondsAndNanosToDouble_NanosZero() {
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(10L, 0L);
        assertEquals(new BigDecimal("10"), result);
    }

    @Test
    public void testSecondsAndNanosToDouble_NegativeSeconds() {
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(-3L, 500_000_000L);
        assertEquals(new BigDecimal("-2.5"), result);
    }
}
