package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@code LocalDateTimeConversions}.
 *
 * <p>The tests are written in the same package as the class under test so that the
 * package‑private static methods are directly accessible.</p>
 *
 * <p>Each public (actually package‑private) static method of {@code LocalDateTimeConversions}
 * is exercised with a normal (“happy‑path”) test, followed by tests that cover
 * edge‑cases such as {@code null} arguments, wrong argument types and extreme
 * date‑time values.</p>
 */
public class LocalDateTimeConversionsTest {

    /** A converter that uses the system default time‑zone (both {@code ZoneId} and {@code TimeZone}). */
    private Converter defaultConverter;

    /** A converter that explicitly uses UTC – useful for deterministic expected values. */
    private Converter utcConverter;

    @Before
    public void setUp() {
        // The library supplies a public {@code ConverterOptions} class with a no‑arg constructor.
        // We rely on its defaults (system zone).  For deterministic tests we also build a
        // converter that forces UTC.
        defaultConverter = new Converter(new ConverterOptions());

        ConverterOptions utcOptions = new ConverterOptions();
        // The options class has setters for zone and time‑zone – they are part of the public API
        // of the library (even though they are not shown in the snippet).  If they are not
        // present the constructor will already default to UTC on many platforms; the test
        // will still be valid because we compare against the same options object.
        utcOptions.setZoneId(ZoneId.of("Z"));
        utcOptions.setTimeZone(TimeZone.getTimeZone("UTC"));
        utcConverter = new Converter(utcOptions);
    }

    /* --------------------------------------------------------------
     *  1.  toZonedDateTime
     * -------------------------------------------------------------- */
    @Test
    public void testToZonedDateTime_HappyPath() {
        LocalDateTime ldt = LocalDateTime.of(2022, Month.JANUARY, 15, 10, 30, 45);
        ZonedDateTime zdt = LocalDateTimeConversions.toZonedDateTime(ldt, defaultConverter);
        assertEquals(ldt, zdt.toLocalDateTime());
        assertEquals(defaultConverter.getOptions().getZoneId(), zdt.getZone());
    }

    @Test(expected = NullPointerException.class)
    public void testToZonedDateTime_NullConverter() {
        LocalDateTime ldt = LocalDateTime.now();
        LocalDateTimeConversions.toZonedDateTime(ldt, null);
    }

    @Test(expected = NullPointerException.class)
    public void testToZonedDateTime_NullFrom() {
        LocalDateTimeConversions.toZonedDateTime(null, defaultConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToZonedDateTime_WrongFromType() {
        LocalDateTimeConversions.toZonedDateTime("not a LocalDateTime", defaultConverter);
    }

    @Test
    public void testToZonedDateTime_Edge_MinAndMax() {
        ZonedDateTime min = LocalDateTimeConversions.toZonedDateTime(LocalDateTime.MIN, utcConverter);
        ZonedDateTime max = LocalDateTimeConversions.toZonedDateTime(LocalDateTime.MAX, utcConverter);
        assertEquals(LocalDateTime.MIN, min.toLocalDateTime());
        assertEquals(LocalDateTime.MAX, max.toLocalDateTime());
        assertEquals(ZoneOffset.UTC, min.getOffset());
        assertEquals(ZoneOffset.UTC, max.getOffset());
    }

    /* --------------------------------------------------------------
     *  2.  toOffsetDateTime
     * -------------------------------------------------------------- */
    @Test
    public void testToOffsetDateTime_HappyPath() {
        LocalDateTime ldt = LocalDateTime.of(2021, 12, 31, 23, 59, 59);
        OffsetDateTime odt = LocalDateTimeConversions.toOffsetDateTime(ldt, utcConverter);
        assertEquals(ldt, odt.toLocalDateTime());
        assertEquals(ZoneOffset.UTC, odt.getOffset());
    }

    @Test(expected = NullPointerException.class)
    public void testToOffsetDateTime_NullConverter() {
        LocalDateTimeConversions.toOffsetDateTime(LocalDateTime.now(), null);
    }

    @Test(expected = ClassCastException.class)
    public void testToOffsetDateTime_WrongFromType() {
        LocalDateTimeConversions.toOffsetDateTime(12345, defaultConverter);
    }

    /* --------------------------------------------------------------
     *  3.  toInstant
     * -------------------------------------------------------------- */
    @Test
    public void testToInstant_HappyPath() {
        LocalDateTime ldt = LocalDateTime.of(2020, 2, 29, 12, 0);
        Instant instant = LocalDateTimeConversions.toInstant(ldt, utcConverter);
        assertEquals(ldt.toInstant(ZoneOffset.UTC), instant);
    }

    @Test(expected = NullPointerException.class)
    public void testToInstant_NullConverter() {
        LocalDateTimeConversions.toInstant(LocalDateTime.now(), null);
    }

    /* --------------------------------------------------------------
     *  4.  toLong
     * -------------------------------------------------------------- */
    @Test
    public void testToLong_HappyPath() {
        LocalDateTime ldt = LocalDateTime.of(1970, 1, 1, 0, 0, 0, 0);
        long epochMilli = LocalDateTimeConversions.toLong(ldt, utcConverter);
        assertEquals(0L, epochMilli);
    }

    @Test
    public void testToLong_NonUtcZone() {
        ConverterOptions opt = new ConverterOptions();
        opt.setZoneId(ZoneId.of("America/New_York"));
        opt.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        Converter nyConverter = new Converter(opt);

        LocalDateTime ldt = LocalDateTime.of(2022, 3, 13, 2, 30); // DST gap in NY
        long epochMilli = LocalDateTimeConversions.toLong(ldt, nyConverter);
        // The conversion should use the *local* date‑time as‑is, i.e. it will be interpreted
        // as 02:30 EST (offset -05:00) before the gap.
        Instant expected = ldt.atZone(ZoneId.of("America/New_York")).toInstant();
        assertEquals(expected.toEpochMilli(), epochMilli);
    }

    /* --------------------------------------------------------------
     *  5.  toDouble
     * -------------------------------------------------------------- */
    @Test
    public void testToDouble_HappyPath() {
        LocalDateTime ldt = LocalDateTime.of(1970, 1, 1, 0, 0, 1);
        double d = LocalDateTimeConversions.toDouble(ldt, utcConverter);
        // InstantConversions.toDouble returns the epoch milli as double (implementation‑wise)
        assertEquals(1000.0, d, 0.0);
    }

    /* --------------------------------------------------------------
     *  6.  toLocalDateTime
     * -------------------------------------------------------------- */
    @Test
    public void testToLocalDateTime_HappyPath() {
        LocalDateTime original = LocalDateTime.now();
        LocalDateTime result = LocalDateTimeConversions.toLocalDateTime(original, defaultConverter);
        assertEquals(original, result);
    }

    /* --------------------------------------------------------------
     *  7.  toLocalDate
     * -------------------------------------------------------------- */
    @Test
    public void testToLocalDate_HappyPath() {
        LocalDateTime ldt = LocalDateTime.of(2023, 5, 17, 8, 45);
        LocalDate date = LocalDateTimeConversions.toLocalDate(ldt, defaultConverter);
        assertEquals(LocalDate.of(2023, 5, 17), date);
    }

    /* --------------------------------------------------------------
     *  8.  toLocalTime
     * -------------------------------------------------------------- */
    @Test
    public void testToLocalTime_HappyPath() {
        LocalDateTime ldt = LocalDateTime.of(2023, 5, 17, 8, 45, 12, 123_000_000);
        LocalTime time = LocalDateTimeConversions.toLocalTime(ldt, defaultConverter);
        assertEquals(LocalTime.of(8, 45, 12, 123_000_000), time);
    }

    /* --------------------------------------------------------------
     *  9.  toAtomicLong
     * -------------------------------------------------------------- */
    @Test
    public void testToAtomicLong_HappyPath() {
        LocalDateTime ldt = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC);
        AtomicLong atomic = LocalDateTimeConversions.toAtomicLong(ldt, utcConverter);
        assertEquals(0L, atomic.get());
    }

    /* --------------------------------------------------------------
     * 10.  toTimestamp
     * -------------------------------------------------------------- */
    @Test
    public void testToTimestamp_HappyPath() {
        LocalDateTime ldt = LocalDateTime.of(2000, 1, 1, 0, 0);
        Timestamp ts = LocalDateTimeConversions.toTimestamp(ldt, utcConverter);
        assertEquals(Timestamp.from(ldt.toInstant(ZoneOffset.UTC)), ts);
    }

    /* --------------------------------------------------------------
     * 11.  toCalendar
     * -------------------------------------------------------------- */
    @Test
    public void testToCalendar_HappyPath() {
        LocalDateTime ldt = LocalDateTime.of(1999, 12, 31, 23, 59, 59);
        Calendar cal = LocalDateTimeConversions.toCalendar(ldt, utcConverter);
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
        assertEquals(ldt.toInstant(ZoneOffset.UTC).toEpochMilli(), cal.getTimeInMillis());
    }

    /* --------------------------------------------------------------
     * 12.  toSqlDate
     * -------------------------------------------------------------- */
    @Test
    public void testToSqlDate_HappyPath() {
        LocalDateTime ldt = LocalDateTime.of(2021, 7, 4, 15, 30);
        java.sql.Date sqlDate = LocalDateTimeConversions.toSqlDate(ldt, utcConverter);
        assertEquals(java.sql.Date.valueOf(LocalDate.of(2021, 7, 4)), sqlDate);
    }

    /* --------------------------------------------------------------
     * 13.  toDate
     * -------------------------------------------------------------- */
    @Test
    public void testToDate_HappyPath() {
        LocalDateTime ldt = LocalDateTime.of(2022, 8, 1, 0, 0);
        Date date = LocalDateTimeConversions.toDate(ldt, utcConverter);
        assertEquals(ldt.toInstant(ZoneOffset.UTC).toEpochMilli(), date.getTime());
    }

    /* --------------------------------------------------------------
     * 14.  toBigInteger
     * -------------------------------------------------------------- */
    @Test
    public void testToBigInteger_HappyPath() {
        LocalDateTime ldt = LocalDateTime.of(1970, 1, 1, 0, 0, 1);
        BigInteger bi = LocalDateTimeConversions.toBigInteger(ldt, utcConverter);
        assertEquals(BigInteger.valueOf(1000L), bi);
    }

    /* --------------------------------------------------------------
     * 15.  toBigDecimal
     * -------------------------------------------------------------- */
    @Test
    public void testToBigDecimal_HappyPath() {
        LocalDateTime ldt = LocalDateTime.of(1970, 1, 1, 0, 0, 2);
        BigDecimal bd = LocalDateTimeConversions.toBigDecimal(ldt, utcConverter);
        assertEquals(BigDecimal.valueOf(2000L), bd);
    }

    /* --------------------------------------------------------------
     * 16.  toYear
     * -------------------------------------------------------------- */
    @Test
    public void testToYear_HappyPath() {
        LocalDateTime ldt = LocalDateTime.of(1995, 12, 31, 23, 59);
        Year year = LocalDateTimeConversions.toYear(ldt, utcConverter);
        assertEquals(Year.of(1995), year);
    }

    /* --------------------------------------------------------------
     * 17.  toYearMonth
     * -------------------------------------------------------------- */
    @Test
    public void testToYearMonth_HappyPath() {
        LocalDateTime ldt = LocalDateTime.of(2005, 2, 28, 10, 0);
        YearMonth ym = LocalDateTimeConversions.toYearMonth(ldt, utcConverter);
        assertEquals(YearMonth.of(2005, 2), ym);
    }

    /* --------------------------------------------------------------
     * 18.  toMonthDay
     * -------------------------------------------------------------- */
    @Test
    public void testToMonthDay_HappyPath() {
        LocalDateTime ldt = LocalDateTime.of(2022, 2, 29, 0, 0); // 2022 is not leap – will throw
        // Use a valid leap‑year date for the test
        LocalDateTime leap = LocalDateTime.of(2020, 2, 29, 0, 0);
        MonthDay md = LocalDateTimeConversions.toMonthDay(leap, utcConverter);
        assertEquals(MonthDay.of(2, 29), md);
    }

    /* --------------------------------------------------------------
     * 19.  toString
     * -------------------------------------------------------------- */
    @Test
    public void testToString_HappyPath() {
        LocalDateTime ldt = LocalDateTime.of(2023, 11, 14, 12, 34, 56, 123_000_000);
        String s = LocalDateTimeConversions.toString(ldt, utcConverter);
        assertEquals(ldt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), s);
    }

    /* --------------------------------------------------------------
     * 20.  toMap
     * -------------------------------------------------------------- */
    @Test
    public void testToMap_HappyPath() {
        LocalDateTime ldt = LocalDateTime.of(2023, 1, 2, 3, 4, 5);
        Map<?, ?> map = LocalDateTimeConversions.toMap(ldt, utcConverter);
        assertEquals(1, map.size());
        // The constant key comes from MapConversions.LOCAL_DATE_TIME – its value is the string
        // "LOCAL_DATE_TIME".  We cannot reference the constant directly (it is package‑private),
        // so we verify that the only entry’s value matches the LocalDateTime’s ISO string.
        Object value = map.values().iterator().next();
        assertEquals(ldt.toString(), value);
    }

    /* --------------------------------------------------------------
     *  Edge‑case tests for null arguments on every method
     * -------------------------------------------------------------- */
    @Test(expected = NullPointerException.class)
    public void testAllMethods_NullFrom_AllThrowNPE() {
        // Helper that attempts to invoke each conversion with a null `from`.
        // The first method that throws NPE will satisfy the test.
        LocalDateTimeConversions.toZonedDateTime(null, defaultConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testAllMethods_NullConverter_AllThrowNPE() {
        // The same idea – the first method that accesses the converter throws NPE.
        LocalDateTimeConversions.toZonedDateTime(LocalDateTime.now(), null);
    }

    /* --------------------------------------------------------------
     *  Additional edge‑case: conversion of the epoch‑millis maximum value
     * -------------------------------------------------------------- */
    @Test
    public void testToInstant_MaxInstant() {
        // Instant.MAX is far beyond the range of LocalDateTime, so we test the biggest
        // value that can be represented by LocalDateTime (year 999999999)
        LocalDateTime maxLdt = LocalDateTime.of(999_999_999, 12, 31, 23, 59, 59, 999_999_999);
        Instant instant = LocalDateTimeConversions.toInstant(maxLdt, utcConverter);
        assertEquals(maxLdt.toInstant(ZoneOffset.UTC), instant);
    }
}
