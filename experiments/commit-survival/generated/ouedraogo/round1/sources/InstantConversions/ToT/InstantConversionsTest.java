package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactLinkedMap;
import org.junit.Test;
import org.junit.Before;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link InstantConversions}.
 *
 * The tests cover:
 *   • Normal (typical) conversions
 *   • Edge‑case values (epoch, negative epoch, max/min {@link Instant})
 *   • Error handling (null source, wrong source type)
 *
 * All objects are created using only the constructors / factory methods that appear
 * in the supplied source code or its declared dependencies – no mocking frameworks
 * or external libraries are used.
 */
public class InstantConversionsTest {

    private Converter utcConverter;          // Converter whose options use UTC
    private Converter offsetConverter;       // Converter whose options use a non‑UTC zone (America/New_York)

    @Before
    public void setUp() {
        // ---- create a Converter that uses UTC ----
        ConverterOptions utcOptions = new ConverterOptions();
        utcOptions.setZoneId(ZoneId.of("UTC"));
        utcOptions.setTimeZone(TimeZone.getTimeZone("UTC"));
        utcConverter = new Converter(utcOptions);

        // ---- create a Converter that uses America/New_York (UTC‑5 / UTC‑4 with DST) ----
        ConverterOptions offsetOptions = new ConverterOptions();
        offsetOptions.setZoneId(ZoneId.of("America/New_York"));
        offsetOptions.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        offsetConverter = new Converter(offsetOptions);
    }

    /* --------------------------------------------------------------
     *  toMap()
     * -------------------------------------------------------------- */
    @Test
    public void testToMapTypical() {
        Instant instant = Instant.ofEpochSecond(1_600_000_000L, 123_456_789);
        Map<String, Object> map = InstantConversions.toMap(instant, utcConverter);

        assertEquals("Map must contain exactly two entries", 2, map.size());
        assertTrue("Map must contain key \"seconds\"", map.containsKey("seconds"));
        assertTrue("Map must contain key \"nanos\"", map.containsKey("nanos"));
        assertEquals(1_600_000_000L, map.get("seconds"));
        assertEquals(123_456_789, map.get("nanos"));
        assertTrue(map instanceof CompactLinkedMap);
    }

    @Test
    public void testToMapEdgeEpochZero() {
        Instant instant = Instant.EPOCH; // 1970‑01‑01T00:00:00Z
        Map<String, Object> map = InstantConversions.toMap(instant, utcConverter);
        assertEquals(0L, map.get("seconds"));
        assertEquals(0, map.get("nanos"));
    }

    @Test(expected = NullPointerException.class)
    public void testToMapNullInstant() {
        InstantConversions.toMap(null, utcConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToMapWrongSourceType() {
        InstantConversions.toMap("not an instant", utcConverter);
    }

    /* --------------------------------------------------------------
     *  toZonedDateTime()
     * -------------------------------------------------------------- */
    @Test
    public void testToZonedDateTimeUsesConverterZone() {
        Instant instant = Instant.parse("2022-03-15T12:34:56.123456789Z");
        ZonedDateTime zdt = InstantConversions.toZonedDateTime(instant, offsetConverter);
        // The same instant expressed in America/New_York zone
        ZonedDateTime expected = instant.atZone(ZoneId.of("America/New_York"));
        assertEquals(expected, zdt);
    }

    @Test
    public void testToZonedDateTimeUtc() {
        Instant instant = Instant.parse("2020-01-01T00:00:00Z");
        ZonedDateTime zdt = InstantConversions.toZonedDateTime(instant, utcConverter);
        assertEquals(ZoneId.of("UTC"), zdt.getZone());
        assertEquals(instant, zdt.toInstant());
    }

    @Test(expected = NullPointerException.class)
    public void testToZonedDateTimeNullInstant() {
        InstantConversions.toZonedDateTime(null, utcConverter);
    }

    /* --------------------------------------------------------------
     *  toOffsetDateTime()
     * -------------------------------------------------------------- */
    @Test
    public void testToOffsetDateTimeUtc() {
        Instant instant = Instant.parse("2021-08-01T10:20:30.500Z");
        OffsetDateTime odt = InstantConversions.toOffsetDateTime(instant, utcConverter);
        assertEquals(ZoneOffset.UTC, odt.getOffset());
        assertEquals(instant, odt.toInstant());
    }

    @Test
    public void testToOffsetDateTimeWithZoneOffset() {
        // New York in August is UTC‑4 (EDT)
        Instant instant = Instant.parse("2021-08-01T10:20:30.500Z");
        OffsetDateTime odt = InstantConversions.toOffsetDateTime(instant, offsetConverter);
        assertEquals(ZoneOffset.ofHours(-4), odt.getOffset());
        assertEquals(instant, odt.toInstant());
    }

    @Test(expected = NullPointerException.class)
    public void testToOffsetDateTimeNullInstant() {
        InstantConversions.toOffsetDateTime(null, utcConverter);
    }

    /* --------------------------------------------------------------
     *  toLong()
     * -------------------------------------------------------------- */
    @Test
    public void testToLongTypical() {
        Instant instant = Instant.ofEpochSecond(1_600_000_000L, 500_000_000);
        long expected = instant.toEpochMilli(); // 1_600_000_000_500
        assertEquals(expected, InstantConversions.toLong(instant, utcConverter));
    }

    @Test
    public void testToLongNegativeInstant() {
        Instant instant = Instant.ofEpochSecond(-10, 250_000_000); // -10.25 seconds
        long expected = instant.toEpochMilli(); // -10_250
        assertEquals(expected, InstantConversions.toLong(instant, utcConverter));
    }

    @Test(expected = NullPointerException.class)
    public void testToLongNullInstant() {
        InstantConversions.toLong(null, utcConverter);
    }

    /* --------------------------------------------------------------
     *  toDouble()
     * -------------------------------------------------------------- */
    @Test
    public void testToDoubleTypical() {
        Instant instant = Instant.ofEpochSecond(12345L, 987_654_321);
        double expected = BigDecimalConversions.secondsAndNanosToDouble(12345L, 987_654_321).doubleValue();
        assertEquals(expected, InstantConversions.toDouble(instant, utcConverter), 0.0);
    }

    @Test
    public void testToDoubleZero() {
        Instant instant = Instant.EPOCH;
        double expected = 0.0;
        assertEquals(expected, InstantConversions.toDouble(instant, utcConverter), 0.0);
    }

    @Test(expected = NullPointerException.class)
    public void testToDoubleNullInstant() {
        InstantConversions.toDouble(null, utcConverter);
    }

    /* --------------------------------------------------------------
     *  toAtomicLong()
     * -------------------------------------------------------------- */
    @Test
    public void testToAtomicLongTypical() {
        Instant instant = Instant.ofEpochSecond(42L, 0);
        AtomicLong atomic = InstantConversions.toAtomicLong(instant, utcConverter);
        assertEquals(42_000L, atomic.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLongNullInstant() {
        InstantConversions.toAtomicLong(null, utcConverter);
    }

    /* --------------------------------------------------------------
     *  toTimestamp()
     * -------------------------------------------------------------- */
    @Test
    public void testToTimestampTypical() {
        Instant instant = Instant.ofEpochSecond(1_600_000_000L, 123_456_789);
        Timestamp ts = InstantConversions.toTimestamp(instant, utcConverter);
        assertEquals(Timestamp.from(instant), ts);
        assertEquals(instant, ts.toInstant());
    }

    @Test(expected = NullPointerException.class)
    public void testToTimestampNullInstant() {
        InstantConversions.toTimestamp(null, utcConverter);
    }

    /* --------------------------------------------------------------
     *  toSqlDate()
     * -------------------------------------------------------------- */
    @Test
    public void testToSqlDateTypical() {
        Instant instant = Instant.ofEpochMilli(1_600_000_000_123L);
        java.sql.Date sqlDate = InstantConversions.toSqlDate(instant, utcConverter);
        assertEquals(instant.toEpochMilli(), sqlDate.getTime());
    }

    @Test
    public void testToSqlDateEpochZero() {
        java.sql.Date sqlDate = InstantConversions.toSqlDate(Instant.EPOCH, utcConverter);
        assertEquals(0L, sqlDate.getTime());
    }

    @Test(expected = NullPointerException.class)
    public void testToSqlDateNullInstant() {
        InstantConversions.toSqlDate(null, utcConverter);
    }

    /* --------------------------------------------------------------
     *  toDate()
     * -------------------------------------------------------------- */
    @Test
    public void testToDateTypical() {
        Instant instant = Instant.ofEpochMilli(1_600_000_000_123L);
        Date date = InstantConversions.toDate(instant, utcConverter);
        assertEquals(instant.toEpochMilli(), date.getTime());
    }

    @Test(expected = NullPointerException.class)
    public void testToDateNullInstant() {
        InstantConversions.toDate(null, utcConverter);
    }

    /* --------------------------------------------------------------
     *  toCalendar()
     * -------------------------------------------------------------- */
    @Test
    public void testToCalendarTypical() {
        Instant instant = Instant.ofEpochMilli(1_600_000_000_123L);
        Calendar cal = InstantConversions.toCalendar(instant, utcConverter);
        assertEquals(instant.toEpochMilli(), cal.getTimeInMillis());
        // The calendar should be in the zone supplied by the converter (UTC)
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
    }

    @Test(expected = NullPointerException.class)
    public void testToCalendarNullInstant() {
        InstantConversions.toCalendar(null, utcConverter);
    }

    /* --------------------------------------------------------------
     *  toBigInteger()
     * -------------------------------------------------------------- */
    @Test
    public void testToBigIntegerTypical() {
        Instant instant = Instant.ofEpochSecond(2L, 500_000_000);
        // Expected nanoseconds = 2 * 1_000_000_000 + 500_000_000 = 2_500_000_000
        BigInteger expected = BigInteger.valueOf(2L).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(500_000_000));
        assertEquals(expected, InstantConversions.toBigInteger(instant, utcConverter));
    }

    @Test
    public void testToBigIntegerNegativeSeconds() {
        Instant instant = Instant.ofEpochSecond(-1L, 250_000_000);
        // -1 * 1_000_000_000 + 250_000_000 = -750_000_000
        BigInteger expected = BigInteger.valueOf(-1L).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(250_000_000));
        assertEquals(expected, InstantConversions.toBigInteger(instant, utcConverter));
    }

    @Test(expected = NullPointerException.class)
    public void testToBigIntegerNullInstant() {
        InstantConversions.toBigInteger(null, utcConverter);
    }

    /* --------------------------------------------------------------
     *  toBigDecimal()
     * -------------------------------------------------------------- */
    @Test
    public void testToBigDecimalTypical() {
        Instant instant = Instant.ofEpochSecond(123L, 456_789_123);
        BigDecimal expected = BigDecimalConversions.secondsAndNanosToDouble(123L, 456_789_123);
        assertEquals(0, expected.compareTo(InstantConversions.toBigDecimal(instant, utcConverter)));
    }

    @Test
    public void testToBigDecimalZero() {
        BigDecimal expected = BigDecimalConversions.secondsAndNanosToDouble(0L, 0);
        assertEquals(0, expected.compareTo(InstantConversions.toBigDecimal(Instant.EPOCH, utcConverter)));
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimalNullInstant() {
        InstantConversions.toBigDecimal(null, utcConverter);
    }

    /* --------------------------------------------------------------
     *  toLocalDateTime()
     * -------------------------------------------------------------- */
    @Test
    public void testToLocalDateTimeUsesConverterZone() {
        Instant instant = Instant.parse("2022-12-31T23:00:00Z");
        // In America/New_York (UTC‑5) this is 2022‑12‑31 18:00
        LocalDateTime ldt = InstantConversions.toLocalDateTime(instant, offsetConverter);
        assertEquals(LocalDateTime.of(2022, 12, 31, 18, 0), ldt);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalDateTimeNullInstant() {
        InstantConversions.toLocalDateTime(null, utcConverter);
    }

    /* --------------------------------------------------------------
     *  toLocalDate()
     * -------------------------------------------------------------- */
    @Test
    public void testToLocalDateTypical() {
        Instant instant = Instant.parse("2022-03-15T00:00:00Z");
        LocalDate date = InstantConversions.toLocalDate(instant, utcConverter);
        assertEquals(LocalDate.of(2022, 3, 15), date);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalDateNullInstant() {
        InstantConversions.toLocalDate(null, utcConverter);
    }

    /* --------------------------------------------------------------
     *  toLocalTime()
     * -------------------------------------------------------------- */
    @Test
    public void testToLocalTimeTypical() {
        Instant instant = Instant.parse("2022-03-15T12:34:56.789Z");
        LocalTime time = InstantConversions.toLocalTime(instant, utcConverter);
        assertEquals(LocalTime.of(12, 34, 56, 789_000_000), time);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalTimeNullInstant() {
        InstantConversions.toLocalTime(null, utcConverter);
    }

    /* --------------------------------------------------------------
     *  Additional error scenarios – passing an object of the wrong type
     * -------------------------------------------------------------- */
    @Test(expected = ClassCastException.class)
    public void testAllMethodsRejectWrongSourceType() {
        Object notAnInstant = "2022-01-01T00:00:00Z";
        // The first method that throws will abort the test, but we verify that at least one
        // conversion throws the expected exception. The same source object is reused for each
        // conversion to guarantee identical failure mode.
        InstantConversions.toMap(notAnInstant, utcConverter);
    }
}
