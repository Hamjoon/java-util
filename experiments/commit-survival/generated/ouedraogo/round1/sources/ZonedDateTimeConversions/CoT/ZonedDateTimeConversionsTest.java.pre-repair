package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link ZonedDateTimeConversions}.
 *
 * The test class lives in the same package as the class under test
 * so that package‑private static methods are accessible.
 */
public class ZonedDateTimeConversionsTest {

    private Converter converter;          // default converter (system zone)
    private Converter converterOtherZone; // converter whose zone is different from the source

    @Before
    public void setUp() {
        // The real library provides a default constructor for ConverterOptions.
        // We rely on the default options (system default zone & time‑zone).
        converter = new Converter(new ConverterOptions());

        // Build a Converter that uses a different zone (e.g., America/New_York)
        ConverterOptions otherOpts = new ConverterOptions();
        otherOpts.setZoneId(ZoneId.of("America/New_York"));
        otherOpts.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        converterOtherZone = new Converter(otherOpts);
    }

    /* --------------------------------------------------------------
     *  Basic “happy‑path” tests
     * -------------------------------------------------------------- */

    @Test
    public void testToLong() {
        ZonedDateTime zdt = ZonedDateTime.of(2023, 5, 17, 12, 30, 45, 123_000_000, ZoneId.of("UTC"));
        long expected = zdt.toInstant().toEpochMilli();
        assertEquals(expected, ZonedDateTimeConversions.toLong(zdt, converter));
    }

    @Test
    public void testToDouble() {
        ZonedDateTime zdt = ZonedDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        double expected = InstantConversions.toDouble(zdt.toInstant(), converter);
        assertEquals(expected, ZonedDateTimeConversions.toDouble(zdt, converter), 0.0);
    }

    @Test
    public void testToInstant() {
        ZonedDateTime zdt = ZonedDateTime.now();
        assertEquals(zdt.toInstant(), ZonedDateTimeConversions.toInstant(zdt, converter));
    }

    @Test
    public void testToLocalDateTime() {
        ZonedDateTime source = ZonedDateTime.of(2022, 12, 31, 23, 59, 59, 0, ZoneId.of("Europe/Paris"));
        // Expect conversion to the converter’s zone (system default)
        ZonedDateTime adjusted = source.withZoneSameInstant(converter.getOptions().getZoneId());
        assertEquals(adjusted.toLocalDateTime(),
                     ZonedDateTimeConversions.toLocalDateTime(source, converter));
    }

    @Test
    public void testToLocalDate() {
        ZonedDateTime source = ZonedDateTime.of(2022, 2, 28, 10, 15, 30, 0, ZoneId.of("UTC"));
        ZonedDateTime adjusted = source.withZoneSameInstant(converter.getOptions().getZoneId());
        assertEquals(adjusted.toLocalDate(),
                     ZonedDateTimeConversions.toLocalDate(source, converter));
    }

    @Test
    public void testToLocalTime() {
        ZonedDateTime source = ZonedDateTime.of(2022, 2, 28, 10, 15, 30, 0, ZoneId.of("UTC"));
        ZonedDateTime adjusted = source.withZoneSameInstant(converter.getOptions().getZoneId());
        assertEquals(adjusted.toLocalTime(),
                     ZonedDateTimeConversions.toLocalTime(source, converter));
    }

    @Test
    public void testToOffsetDateTime() {
        ZonedDateTime source = ZonedDateTime.of(2020, 6, 15, 8, 0, 0, 0, ZoneId.of("Asia/Tokyo"));
        assertEquals(source.toOffsetDateTime(),
                     ZonedDateTimeConversions.toOffsetDateTime(source, converter));
    }

    @Test
    public void testToAtomicLong() {
        ZonedDateTime source = ZonedDateTime.ofInstant(Instant.ofEpochMilli(1_600_000_000_000L), ZoneId.of("UTC"));
        AtomicLong expected = new AtomicLong(source.toInstant().toEpochMilli());
        assertEquals(expected.get(),
                     ZonedDateTimeConversions.toAtomicLong(source, converter).get());
    }

    @Test
    public void testToTimestamp() {
        ZonedDateTime source = ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault());
        Timestamp ts = Timestamp.from(source.toInstant());
        assertEquals(ts, ZonedDateTimeConversions.toTimestamp(source, converter));
    }

    @Test
    public void testToCalendar() {
        ZonedDateTime source = ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("UTC"));
        Calendar cal = Calendar.getInstance(converter.getOptions().getTimeZone());
        cal.setTimeInMillis(source.toInstant().toEpochMilli());

        Calendar result = ZonedDateTimeConversions.toCalendar(source, converter);
        assertEquals(cal.getTimeInMillis(), result.getTimeInMillis());
        assertEquals(cal.getTimeZone(), result.getTimeZone());
    }

    @Test
    public void testToSqlDate() {
        ZonedDateTime source = ZonedDateTime.ofInstant(Instant.ofEpochMilli(1_234_567_890L), ZoneId.of("UTC"));
        java.sql.Date expected = new java.sql.Date(1_234_567_890L);
        assertEquals(expected, ZonedDateTimeConversions.toSqlDate(source, converter));
    }

    @Test
    public void testToDate() {
        ZonedDateTime source = ZonedDateTime.ofInstant(Instant.ofEpochMilli(9_876_543_210L), ZoneId.of("UTC"));
        Date expected = new Date(9_876_543_210L);
        assertEquals(expected, ZonedDateTimeConversions.toDate(source, converter));
    }

    @Test
    public void testToBigInteger() {
        ZonedDateTime source = ZonedDateTime.ofInstant(Instant.ofEpochSecond(123456789L), ZoneId.of("UTC"));
        BigInteger expected = InstantConversions.toBigInteger(source.toInstant(), converter);
        assertEquals(expected, ZonedDateTimeConversions.toBigInteger(source, converter));
    }

    @Test
    public void testToBigDecimal() {
        ZonedDateTime source = ZonedDateTime.ofInstant(Instant.ofEpochSecond(123456789L, 987_654_321), ZoneId.of("UTC"));
        BigDecimal expected = InstantConversions.toBigDecimal(source.toInstant(), converter);
        assertEquals(expected, ZonedDateTimeConversions.toBigDecimal(source, converter));
    }

    @Test
    public void testToString() {
        ZonedDateTime source = ZonedDateTime.of(2023, 3, 14, 15, 9, 26, 535_000_000, ZoneId.of("Europe/London"));
        String expected = source.format(DateTimeFormatter.ISO_DATE_TIME);
        assertEquals(expected, ZonedDateTimeConversions.toString(source, converter));
    }

    @Test
    public void testToMap() {
        ZonedDateTime source = ZonedDateTime.of(2021, 11, 7, 22, 45, 0, 0, ZoneId.of("America/Los_Angeles"));
        Map<String, Object> map = ZonedDateTimeConversions.toMap(source, converter);

        assertEquals(3, map.size());
        assertEquals(source.toLocalDate().toString(), map.get(MapConversions.DATE));
        assertEquals(source.toLocalTime().toString(), map.get(MapConversions.TIME));
        assertEquals(source.getZone().toString(), map.get(MapConversions.ZONE));
    }

    /* --------------------------------------------------------------
     *  Edge‑case / exception tests
     * -------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testToLong_NullArgument() {
        ZonedDateTimeConversions.toLong(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToLong_WrongType() {
        ZonedDateTimeConversions.toLong("not a ZonedDateTime", converter);
    }

    @Test
    public void testToLong_EpochBoundary() {
        ZonedDateTime beforeEpoch = ZonedDateTime.ofInstant(Instant.ofEpochMilli(-1L), ZoneId.of("UTC"));
        assertEquals(-1L, ZonedDateTimeConversions.toLong(beforeEpoch, converter));

        ZonedDateTime epoch = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.of("UTC"));
        assertEquals(0L, ZonedDateTimeConversions.toLong(epoch, converter));
    }

    @Test
    public void testToCalendar_DifferentTargetZone() {
        ZonedDateTime source = ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("UTC"));
        Calendar cal = ZonedDateTimeConversions.toCalendar(source, converterOtherZone);
        // The calendar must use the zone supplied by the converter (New York)
        assertEquals(TimeZone.getTimeZone("America/New_York"), cal.getTimeZone());
        // Milliseconds should still match the instant
        assertEquals(source.toInstant().toEpochMilli(), cal.getTimeInMillis());
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullArgument() {
        ZonedDateTimeConversions.toMap(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToString_NullArgument() {
        ZonedDateTimeConversions.toString(null, converter);
    }

    // Additional null‑argument tests for the remaining methods follow the same pattern.
    @Test(expected = NullPointerException.class) public void testToInstant_Null() { ZonedDateTimeConversions.toInstant(null, converter); }
    @Test(expected = NullPointerException.class) public void testToLocalDateTime_Null() { ZonedDateTimeConversions.toLocalDateTime(null, converter); }
    @Test(expected = NullPointerException.class) public void testToLocalDate_Null() { ZonedDateTimeConversions.toLocalDate(null, converter); }
    @Test(expected = NullPointerException.class) public void testToLocalTime_Null() { ZonedDateTimeConversions.toLocalTime(null, converter); }
    @Test(expected = NullPointerException.class) public void testToOffsetDateTime_Null() { ZonedDateTimeConversions.toOffsetDateTime(null, converter); }
    @Test(expected = NullPointerException.class) public void testToAtomicLong_Null() { ZonedDateTimeConversions.toAtomicLong(null, converter); }
    @Test(expected = NullPointerException.class) public void testToTimestamp_Null() { ZonedDateTimeConversions.toTimestamp(null, converter); }
    @Test(expected = NullPointerException.class) public void testToCalendar_Null() { ZonedDateTimeConversions.toCalendar(null, converter); }
    @Test(expected = NullPointerException.class) public void testToSqlDate_Null() { ZonedDateTimeConversions.toSqlDate(null, converter); }
    @Test(expected = NullPointerException.class) public void testToDate_Null() { ZonedDateTimeConversions.toDate(null, converter); }
    @Test(expected = NullPointerException.class) public void testToBigInteger_Null() { ZonedDateTimeConversions.toBigInteger(null, converter); }
    @Test(expected = NullPointerException.class) public void testToBigDecimal_Null() { ZonedDateTimeConversions.toBigDecimal(null, converter); }

}
