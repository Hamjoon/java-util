package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.junit.Assert.*;

/**
 * JUnit 4 test suite for {@link TimestampConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Typical conversions for all static methods</li>
 *   <li>Edge cases such as zero‑nanosecond timestamps and null arguments</li>
 *   <li>Verification that zone handling follows the {@link ConverterOptions} supplied to the {@link Converter}</li>
 * </ul>
 *
 * The test class creates real {@link Timestamp}, {@link Converter}, {@link ConverterOptions},
 * {@link Calendar}, {@link Date} and other JDK objects – no mocking or subclassing of library
 * types is performed, as required by the exercise.
 */
public class TimestampConversionsTest {

    private static final ZoneId ZONE_UTC = ZoneId.of("UTC");
    private static final ZoneId ZONE_PARIS = ZoneId.of("Europe/Paris");
    private static final TimeZone TZ_UTC = TimeZone.getTimeZone("UTC");
    private static final TimeZone TZ_PARIS = TimeZone.getTimeZone("Europe/Paris");

    private Converter utcConverter;
    private Converter parisConverter;

    @Before
    public void setUp() {
        // ConverterOptions is not part of the provided snippet, but we assume it has a
        // no‑arg constructor and setters for zoneId and timeZone.
        ConverterOptions utcOptions = new ConverterOptions();
        utcOptions.setZoneId(ZONE_UTC);
        utcOptions.setTimeZone(TZ_UTC);
        utcConverter = new Converter(utcOptions);

        ConverterOptions parisOptions = new ConverterOptions();
        parisOptions.setZoneId(ZONE_PARIS);
        parisOptions.setTimeZone(TZ_PARIS);
        parisConverter = new Converter(parisOptions);
    }

    /* --------------------------------------------------------------
     * Helper methods
     * -------------------------------------------------------------- */

    /** Creates a {@link Timestamp} for the given instant and nanos. */
    private Timestamp tsOf(Instant instant, int nanos) {
        Timestamp ts = new Timestamp(instant.toEpochMilli());
        ts.setNanos(nanos);
        return ts;
    }

    /** Returns the expected double value for a given {@link Duration}. */
    private static double durationToDouble(Duration d) {
        return d.getSeconds() + d.getNano() / 1_000_000_000.0;
    }

    /** Returns a {@link BigDecimal} representing the exact instant (seconds + nanos). */
    private static BigDecimal instantToBigDecimal(Instant i) {
        BigDecimal seconds = BigDecimal.valueOf(i.getEpochSecond());
        BigDecimal nanos   = BigDecimal.valueOf(i.getNano(), 9); // scale 9 = divide by 1_000_000_000
        return seconds.add(nanos);
    }

    /* --------------------------------------------------------------
     * Tests for toDouble
     * -------------------------------------------------------------- */
    @Test
    public void testToDouble_WithNanoseconds() {
        Instant instant = Instant.parse("2023-01-02T12:34:56Z");
        Timestamp ts = tsOf(instant, 123_456_789);
        double expected = durationToDouble(Duration.between(Instant.EPOCH, instant));

        double actual = TimestampConversions.toDouble(ts, utcConverter);
        assertEquals(expected, actual, 1e-12);
    }

    @Test
    public void testToDouble_WithZeroNanos() {
        Instant instant = Instant.parse("2020-05-01T00:00:00Z");
        Timestamp ts = tsOf(instant, 0);
        double expected = durationToDouble(Duration.between(Instant.EPOCH, instant));

        double actual = TimestampConversions.toDouble(ts, utcConverter);
        assertEquals(expected, actual, 0.0);
    }

    @Test(expected = NullPointerException.class)
    public void testToDouble_NullArgument() {
        TimestampConversions.toDouble(null, utcConverter);
    }

    /* --------------------------------------------------------------
     * Tests for toBigDecimal
     * -------------------------------------------------------------- */
    @Test
    public void testToBigDecimal() {
        Instant instant = Instant.parse("2021-12-31T23:59:59.999999999Z");
        Timestamp ts = tsOf(instant, 999_999_999);
        BigDecimal expected = instantToBigDecimal(instant);

        BigDecimal actual = TimestampConversions.toBigDecimal(ts, utcConverter);
        assertEquals(0, expected.compareTo(actual));
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimal_NullArgument() {
        TimestampConversions.toBigDecimal(null, utcConverter);
    }

    /* --------------------------------------------------------------
     * Tests for toBigInteger
     * -------------------------------------------------------------- */
    @Test
    public void testToBigInteger() {
        Instant instant = Instant.parse("1970-01-01T00:00:05Z"); // 5 seconds after epoch
        Timestamp ts = tsOf(instant, 0);
        BigInteger expected = BigInteger.valueOf(5);

        BigInteger actual = TimestampConversions.toBigInteger(ts, utcConverter);
        assertEquals(expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigInteger_NullArgument() {
        TimestampConversions.toBigInteger(null, utcConverter);
    }

    /* --------------------------------------------------------------
     * Tests for toLocalDateTime
     * -------------------------------------------------------------- */
    @Test
    public void testToLocalDateTime_Utc() {
        Instant instant = Instant.parse("2022-03-15T10:20:30Z");
        Timestamp ts = tsOf(instant, 0);
        LocalDateTime expected = LocalDateTime.ofInstant(instant, ZONE_UTC);

        LocalDateTime actual = TimestampConversions.toLocalDateTime(ts, utcConverter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLocalDateTime_Paris() {
        Instant instant = Instant.parse("2022-03-15T10:20:30Z");
        Timestamp ts = tsOf(instant, 0);
        LocalDateTime expected = LocalDateTime.ofInstant(instant, ZONE_PARIS);

        LocalDateTime actual = TimestampConversions.toLocalDateTime(ts, parisConverter);
        assertEquals(expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalDateTime_NullArgument() {
        TimestampConversions.toLocalDateTime(null, utcConverter);
    }

    /* --------------------------------------------------------------
     * Tests for toDuration
     * -------------------------------------------------------------- */
    @Test
    public void testToDuration() {
        Instant instant = Instant.parse("2000-01-01T00:00:00Z");
        Timestamp ts = tsOf(instant, 0);
        Duration expected = Duration.between(Instant.EPOCH, instant);

        Duration actual = TimestampConversions.toDuration(ts, utcConverter);
        assertEquals(expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void testToDuration_NullArgument() {
        TimestampConversions.toDuration(null, utcConverter);
    }

    /* --------------------------------------------------------------
     * Tests for toOffsetDateTime
     * -------------------------------------------------------------- */
    @Test
    public void testToOffsetDateTime_Utc() {
        Instant instant = Instant.parse("2024-06-01T08:15:30Z");
        Timestamp ts = tsOf(instant, 0);
        OffsetDateTime expected = instant.atZone(ZONE_UTC).toOffsetDateTime();

        OffsetDateTime actual = TimestampConversions.toOffsetDateTime(ts, utcConverter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToOffsetDateTime_Paris() {
        Instant instant = Instant.parse("2024-06-01T08:15:30Z");
        Timestamp ts = tsOf(instant, 0);
        OffsetDateTime expected = instant.atZone(ZONE_PARIS).toOffsetDateTime();

        OffsetDateTime actual = TimestampConversions.toOffsetDateTime(ts, parisConverter);
        assertEquals(expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void testToOffsetDateTime_NullArgument() {
        TimestampConversions.toOffsetDateTime(null, utcConverter);
    }

    /* --------------------------------------------------------------
     * Tests for toCalendar
     * -------------------------------------------------------------- */
    @Test
    public void testToCalendar_Utc() {
        long millis = 1_600_000_000_000L; // 2020‑09‑13T12:26:40Z
        Timestamp ts = new Timestamp(millis);
        Calendar cal = TimestampConversions.toCalendar(ts, utcConverter);

        assertEquals(TZ_UTC, cal.getTimeZone());
        assertEquals(millis, cal.getTimeInMillis());
    }

    @Test
    public void testToCalendar_Paris() {
        long millis = 1_600_000_000_000L;
        Timestamp ts = new Timestamp(millis);
        Calendar cal = TimestampConversions.toCalendar(ts, parisConverter);

        assertEquals(TZ_PARIS, cal.getTimeZone());
        assertEquals(millis, cal.getTimeInMillis());
    }

    @Test(expected = NullPointerException.class)
    public void testToCalendar_NullArgument() {
        TimestampConversions.toCalendar(null, utcConverter);
    }

    /* --------------------------------------------------------------
     * Tests for toDate
     * -------------------------------------------------------------- */
    @Test
    public void testToDate() {
        Instant instant = Instant.parse("2019-07-20T15:45:00Z");
        Timestamp ts = tsOf(instant, 0);

        Date expected = Date.from(instant);
        Date actual = TimestampConversions.toDate(ts, utcConverter);

        assertEquals(expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void testToDate_NullArgument() {
        TimestampConversions.toDate(null, utcConverter);
    }

    /* --------------------------------------------------------------
     * Tests for toSqlDate
     * -------------------------------------------------------------- */
    @Test
    public void testToSqlDate() {
        // The java.sql.Date constructor discards the time‑of‑day part.
        Timestamp ts = Timestamp.valueOf("2022-12-31 23:59:59");
        java.sql.Date sqlDate = TimestampConversions.toSqlDate(ts, utcConverter);
        assertEquals(java.sql.Date.valueOf("2022-12-31"), sqlDate);
    }

    @Test(expected = NullPointerException.class)
    public void testToSqlDate_NullArgument() {
        TimestampConversions.toSqlDate(null, utcConverter);
    }

    /* --------------------------------------------------------------
     * Tests for toLong
     * -------------------------------------------------------------- */
    @Test
    public void testToLong() {
        long millis = System.currentTimeMillis();
        Timestamp ts = new Timestamp(millis);
        assertEquals(millis, TimestampConversions.toLong(ts, utcConverter));
    }

    @Test(expected = NullPointerException.class)
    public void testToLong_NullArgument() {
        TimestampConversions.toLong(null, utcConverter);
    }

    /* --------------------------------------------------------------
     * Tests for toString
     * -------------------------------------------------------------- */
    @Test
    public void testToString_WholeSeconds() {
        Timestamp ts = Timestamp.valueOf("2023-01-01 00:00:00");
        ts.setNanos(0);
        String result = TimestampConversions.toString(ts, utcConverter);
        String expected = "2023-01-01T00:00:00Z";
        assertEquals(expected, result);
    }

    @Test
    public void testToString_Milliseconds() {
        Timestamp ts = Timestamp.valueOf("2023-01-01 12:34:56");
        ts.setNanos(123_000_000); // exactly 123 ms
        String result = TimestampConversions.toString(ts, utcConverter);
        // pattern "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
        String expected = "2023-01-01T12:34:56.123Z";
        assertEquals(expected, result);
    }

    @Test
    public void testToString_Nanoseconds() {
        Timestamp ts = Timestamp.valueOf("2023-01-01 12:34:56");
        ts.setNanos(123_456_789);
        String result = TimestampConversions.toString(ts, utcConverter);
        // pattern "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSSXXX"
        String expected = "2023-01-01T12:34:56.123456789Z";
        assertEquals(expected, result);
    }

    @Test(expected = NullPointerException.class)
    public void testToString_NullArgument() {
        TimestampConversions.toString(null, utcConverter);
    }

    /* --------------------------------------------------------------
     * Tests for toMap
     * -------------------------------------------------------------- */
    @Test
    public void testToMap_Utc() {
        Timestamp ts = Timestamp.valueOf("2022-08-15 10:20:30");
        ts.setNanos(555_777_999);
        Map<String, Object> map = TimestampConversions.toMap(ts, utcConverter);

        // Expected values
        OffsetDateTime odt = ts.toInstant().atZone(ZONE_UTC).toOffsetDateTime();

        assertEquals(5, map.size());

        assertEquals(odt.toLocalDate().toString(), map.get(MapConversions.DATE));
        assertEquals(odt.toLocalTime().toString(), map.get(MapConversions.TIME));
        assertEquals(ZONE_UTC.toString(), map.get(MapConversions.ZONE));
        assertEquals(ts.getTime(), map.get(MapConversions.EPOCH_MILLIS));
        assertEquals(odt.getNano(), map.get(MapConversions.NANOS));
    }

    @Test
    public void testToMap_Paris() {
        Timestamp ts = Timestamp.valueOf("2022-08-15 10:20:30");
        ts.setNanos(0);
        Map<String, Object> map = TimestampConversions.toMap(ts, parisConverter);

        OffsetDateTime odt = ts.toInstant().atZone(ZONE_PARIS).toOffsetDateTime();

        assertEquals(odt.toLocalDate().toString(), map.get(MapConversions.DATE));
        assertEquals(odt.toLocalTime().toString(), map.get(MapConversions.TIME));
        assertEquals(ZONE_PARIS.toString(), map.get(MapConversions.ZONE));
        assertEquals(ts.getTime(), map.get(MapConversions.EPOCH_MILLIS));
        assertEquals(odt.getNano(), map.get(MapConversions.NANOS));
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullArgument() {
        TimestampConversions.toMap(null, utcConverter);
    }

    /* --------------------------------------------------------------
     * Additional sanity checks – ensure that the class cannot be instantiated.
     * -------------------------------------------------------------- */
    @Test(expected = IllegalAccessException.class)
    public void testConstructorIsPrivate() throws Exception {
        // The class is final with a private constructor; attempt to reflectively invoke it.
        java.lang.reflect.Constructor<TimestampConversions> ctor = TimestampConversions.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        ctor.newInstance();
    }
}
