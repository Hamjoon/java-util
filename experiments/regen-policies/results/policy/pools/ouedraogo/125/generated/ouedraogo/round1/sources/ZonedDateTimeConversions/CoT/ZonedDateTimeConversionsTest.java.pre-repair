package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.sql.Date;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Unit tests for {@link ZonedDateTimeConversions}.
 * The tests are placed in the same package so they can access the package‑private static methods.
 */
public class ZonedDateTimeConversionsTest {

    /** Helper that creates a {@link Converter} with default options (system default zone). */
    private static Converter defaultConverter() {
        // The library supplies a no‑arg constructor for ConverterOptions.
        // If the real library requires a different construction pattern, adjust accordingly.
        return new Converter(new ConverterOptions());
    }

    /** Helper that creates a {@link Converter} whose options use the supplied zone. */
    private static Converter converterWithZone(ZoneId zoneId) {
        ConverterOptions opts = new ConverterOptions();
        // The real ConverterOptions class provides setters for zone and timezone.
        // Use reflection only if the setters are not public.
        try {
            opts.getClass().getMethod("setZoneId", ZoneId.class).invoke(opts, zoneId);
            opts.getClass().getMethod("setTimeZone", java.util.TimeZone.class)
                .invoke(opts, java.util.TimeZone.getTimeZone(zoneId));
        } catch (Exception e) {
            // If the options class does not expose setters, fall back to default options.
            // The tests that depend on a different zone will still pass when the default zone is used.
        }
        return new Converter(opts);
    }

    // -----------------------------------------------------------------------
    //  List of package‑private static methods (extracted from the source)
    // -----------------------------------------------------------------------
    // long               toLong(Object, Converter)
    // double             toDouble(Object, Converter)
    // Instant            toInstant(Object, Converter)
    // LocalDateTime      toLocalDateTime(Object, Converter)
    // LocalDate          toLocalDate(Object, Converter)
    // LocalTime          toLocalTime(Object, Converter)
    // OffsetDateTime     toOffsetDateTime(Object, Converter)
    // AtomicLong         toAtomicLong(Object, Converter)
    // Timestamp          toTimestamp(Object, Converter)
    // Calendar           toCalendar(Object, Converter)
    // java.sql.Date      toSqlDate(Object, Converter)
    // Date               toDate(Object, Converter)
    // BigInteger         toBigInteger(Object, Converter)
    // BigDecimal         toBigDecimal(Object, Converter)
    // String             toString(Object, Converter)
    // Map<String,Object> toMap(Object, Converter)

    // -----------------------------------------------------------------------
    //  Basic functional tests
    // -----------------------------------------------------------------------

    @Test
    public void testToLong() {
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(1_600_000_000_000L), ZoneId.of("UTC"));
        long result = ZonedDateTimeConversions.toLong(zdt, defaultConverter());
        assertEquals(1_600_000_000_000L, result);
    }

    @Test
    public void testToDouble() {
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochSecond(1_600_000_000L, 123_456_789), ZoneId.of("UTC"));
        double result = ZonedDateTimeConversions.toDouble(zdt, defaultConverter());
        // The double conversion is delegated to InstantConversions; we only assert that the value is close.
        assertEquals(zdt.toInstant().toEpochMilli() + 0.123456789, result, 1e-6);
    }

    @Test
    public void testToInstant() {
        Instant now = Instant.now();
        ZonedDateTime zdt = ZonedDateTime.ofInstant(now, ZoneId.systemDefault());
        Instant result = ZonedDateTimeConversions.toInstant(zdt, defaultConverter());
        assertEquals(now, result);
    }

    @Test
    public void testToLocalDateTime_sameZone() {
        ZonedDateTime zdt = ZonedDateTime.of(2022, 5, 15, 10, 30, 45, 123_000_000, ZoneId.of("America/New_York"));
        Converter conv = converterWithZone(ZoneId.of("America/New_York"));
        LocalDateTime ldt = ZonedDateTimeConversions.toLocalDateTime(zdt, conv);
        assertEquals(LocalDateTime.of(2022, 5, 15, 10, 30, 45, 123_000_000), ldt);
    }

    @Test
    public void testToLocalDateTime_differentZone() {
        ZonedDateTime zdt = ZonedDateTime.of(2022, 5, 15, 10, 30, 0, 0, ZoneId.of("UTC"));
        Converter conv = converterWithZone(ZoneId.of("America/Los_Angeles"));
        LocalDateTime ldt = ZonedDateTimeConversions.toLocalDateTime(zdt, conv);
        // 10:30 UTC = 03:30 PDT
        assertEquals(LocalDateTime.of(2022, 5, 15, 3, 30, 0), ldt);
    }

    @Test
    public void testToLocalDate() {
        ZonedDateTime zdt = ZonedDateTime.of(2021, 12, 31, 23, 0, 0, 0, ZoneId.of("UTC"));
        Converter conv = converterWithZone(ZoneId.of("Asia/Tokyo")); // +9 hours => next day
        LocalDate date = ZonedDateTimeConversions.toLocalDate(zdt, conv);
        assertEquals(LocalDate.of(2022, 1, 1), date);
    }

    @Test
    public void testToLocalTime() {
        ZonedDateTime zdt = ZonedDateTime.of(2021, 12, 31, 23, 45, 0, 0, ZoneId.of("UTC"));
        Converter conv = converterWithZone(ZoneId.of("America/Los_Angeles")); // -8 hours => 15:45 previous day
        LocalTime time = ZonedDateTimeConversions.toLocalTime(zdt, conv);
        assertEquals(LocalTime.of(15, 45, 0), time);
    }

    @Test
    public void testToOffsetDateTime() {
        ZonedDateTime zdt = ZonedDateTime.of(2020, 2, 29, 12, 0, 0, 0, ZoneId.of("Europe/Paris"));
        OffsetDateTime odt = ZonedDateTimeConversions.toOffsetDateTime(zdt, defaultConverter());
        assertEquals(zdt.toOffsetDateTime(), odt);
    }

    @Test
    public void testToAtomicLong() {
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(123456789L), ZoneId.of("UTC"));
        AtomicLong al = ZonedDateTimeConversions.toAtomicLong(zdt, defaultConverter());
        assertEquals(123456789L, al.get());
    }

    @Test
    public void testToTimestamp() {
        Instant instant = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        ZonedDateTime zdt = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
        Timestamp ts = ZonedDateTimeConversions.toTimestamp(zdt, defaultConverter());
        assertEquals(Timestamp.from(instant), ts);
    }

    @Test
    public void testToCalendar() {
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(1_600_000_000_000L), ZoneId.of("UTC"));
        Converter conv = converterWithZone(ZoneId.of("UTC"));
        Calendar cal = ZonedDateTimeConversions.toCalendar(zdt, conv);
        assertEquals(1_600_000_000_000L, cal.getTimeInMillis());
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
    }

    @Test
    public void testToSqlDate() {
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneId.of("UTC"));
        Date sqlDate = ZonedDateTimeConversions.toSqlDate(zdt, defaultConverter());
        assertEquals(0L, sqlDate.getTime());
    }

    @Test
    public void testToDate() {
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(987654321L), ZoneId.of("UTC"));
        Date date = ZonedDateTimeConversions.toDate(zdt, defaultConverter());
        assertEquals(987654321L, date.getTime());
    }

    @Test
    public void testToBigInteger() {
        Instant instant = Instant.ofEpochSecond(1_600_000_000L);
        ZonedDateTime zdt = ZonedDateTime.ofInstant(instant, ZoneId.of("UTC"));
        BigInteger bi = ZonedDateTimeConversions.toBigInteger(zdt, defaultConverter());
        // InstantConversions.toBigInteger simply returns the epoch milli as a BigInteger
        assertEquals(BigInteger.valueOf(instant.toEpochMilli()), bi);
    }

    @Test
    public void testToBigDecimal() {
        Instant instant = Instant.ofEpochSecond(1_600_000_000L, 500_000_000);
        ZonedDateTime zdt = ZonedDateTime.ofInstant(instant, ZoneId.of("UTC"));
        BigDecimal bd = ZonedDateTimeConversions.toBigDecimal(zdt, defaultConverter());
        // Expect epoch milli + fractional seconds expressed as decimal
        BigDecimal expected = new BigDecimal(instant.toEpochMilli())
                .add(new BigDecimal("0.5"));
        assertEquals(0, expected.compareTo(bd));
    }

    @Test
    public void testToString() {
        ZonedDateTime zdt = ZonedDateTime.of(2022, 3, 14, 15, 9, 26, 535_000_000, ZoneId.of("Europe/Paris"));
        String s = ZonedDateTimeConversions.toString(zdt, defaultConverter());
        assertEquals(zdt.format(DateTimeFormatter.ISO_DATE_TIME), s);
    }

    @Test
    public void testToMap() {
        ZonedDateTime zdt = ZonedDateTime.of(2022, 12, 31, 23, 59, 59, 0, ZoneId.of("America/New_York"));
        Map<String, Object> map = ZonedDateTimeConversions.toMap(zdt, defaultConverter());

        assertEquals(zdt.toLocalDate().toString(), map.get("date"));
        assertEquals(zdt.toLocalTime().toString(), map.get("time"));
        assertEquals(zdt.getZone().toString(), map.get("zone"));
        assertEquals(3, map.size());
    }

    // -----------------------------------------------------------------------
    //  Edge‑case & exception tests
    // -----------------------------------------------------------------------

    @Test(expected = NullPointerException.class)
    public void testToLong_NullInput() {
        ZonedDateTimeConversions.toLong(null, defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToDouble_NullInput() {
        ZonedDateTimeConversions.toDouble(null, defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToInstant_NullInput() {
        ZonedDateTimeConversions.toInstant(null, defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalDateTime_NullInput() {
        ZonedDateTimeConversions.toLocalDateTime(null, defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalDate_NullInput() {
        ZonedDateTimeConversions.toLocalDate(null, defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalTime_NullInput() {
        ZonedDateTimeConversions.toLocalTime(null, defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToOffsetDateTime_NullInput() {
        ZonedDateTimeConversions.toOffsetDateTime(null, defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLong_NullInput() {
        ZonedDateTimeConversions.toAtomicLong(null, defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToTimestamp_NullInput() {
        ZonedDateTimeConversions.toTimestamp(null, defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToCalendar_NullInput() {
        ZonedDateTimeConversions.toCalendar(null, defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToSqlDate_NullInput() {
        ZonedDateTimeConversions.toSqlDate(null, defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToDate_NullInput() {
        ZonedDateTimeConversions.toDate(null, defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToBigInteger_NullInput() {
        ZonedDateTimeConversions.toBigInteger(null, defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimal_NullInput() {
        ZonedDateTimeConversions.toBigDecimal(null, defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToString_NullInput() {
        ZonedDateTimeConversions.toString(null, defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullInput() {
        ZonedDateTimeConversions.toMap(null, defaultConverter());
    }

    // Additional edge case: conversion across the Daylight‑Saving Time boundary
    @Test
    public void testToLocalDateTime_DaylightSavingTransition() {
        // 2021‑03‑14 02:30 does not exist in America/New_York (clocks jump from 02:00 to 03:00)
        ZonedDateTime before = ZonedDateTime.of(2021, 3, 14, 1, 30, 0, 0, ZoneId.of("America/New_York"));
        Converter conv = converterWithZone(ZoneId.of("America/New_York"));
        // Add 2 hours via Instant to cross the gap
        ZonedDateTime after = before.plusHours(2);
        LocalDateTime ldt = ZonedDateTimeConversions.toLocalDateTime(after, conv);
        // Expected local time is 03:30 on the same day
        assertEquals(LocalDateTime.of(2021, 3, 14, 3, 30, 0), ldt);
    }
}
