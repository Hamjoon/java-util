package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactMap;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import static org.junit.Assert.*;

public class TimestampConversionsTest {

    /*** ----------------------------------------------------------------------
     *  List of static methods (package‑private) in TimestampConversions
     *  ----------------------------------------------------------------------
     *
     *  static double               toDouble(Object from, Converter converter)
     *  static BigDecimal           toBigDecimal(Object from, Converter converter)
     *  static BigInteger           toBigInteger(Object from, Converter converter)
     *  static LocalDateTime        toLocalDateTime(Object from, Converter converter)
     *  static Duration             toDuration(Object from, Converter converter)
     *  static OffsetDateTime       toOffsetDateTime(Object from, Converter converter)
     *  static Calendar             toCalendar(Object from, Converter converter)
     *  static Date                 toDate(Object from, Converter converter)
     *  static java.sql.Date        toSqlDate(Object from, Converter converter)
     *  static long                 toLong(Object from, Converter converter)
     *  static String               toString(Object from, Converter converter)
     *  static Map<String,Object>   toMap(Object from, Converter converter)
     *
     *  All methods expect the *from* argument to be a java.sql.Timestamp.
     *  They use the supplied Converter only for its ConverterOptions (zone / time‑zone).
     *
     ***/

    private Converter utcConverter;   // Converter that uses UTC (the default ZoneId)
    private ZoneId utcZone = ZoneOffset.UTC;

    @Before
    public void setUp() {
        // ConverterOptions has a public no‑arg constructor in the library.
        // It defaults to UTC; if not, we explicitly set it.
        ConverterOptions opts = new ConverterOptions();
        opts.setZoneId(utcZone);               // ensure UTC
        opts.setTimeZone(TimeZone.getTimeZone(utcZone)); // ensure matching TimeZone
        utcConverter = new Converter(opts);
    }

    /*** ----------------------------------------------------------------------
     *  Helper to create a Timestamp from an Instant (preserves nanos)
     *  ----------------------------------------------------------------------*/
    private Timestamp ts(Instant i) {
        Timestamp t = Timestamp.from(i);
        // Timestamp.from truncates nanos to milliseconds, so we set nanos explicitly
        t.setNanos(i.getNano());
        return t;
    }

    /*** ----------------------------------------------------------------------
     *  BASIC FUNCTIONALITY TESTS
     *  ----------------------------------------------------------------------*/

    @Test
    public void testToDouble() {
        Instant instant = Instant.parse("2023-03-15T12:34:56.123456789Z");
        Timestamp ts = ts(instant);
        double result = TimestampConversions.toDouble(ts, utcConverter);
        // Expected double = seconds + nanos/1e9
        double expected = instant.getEpochSecond() + instant.getNano() / 1_000_000_000.0;
        assertEquals(expected, result, 1e-12);
    }

    @Test
    public void testToBigDecimal() {
        Instant instant = Instant.now();
        Timestamp ts = ts(instant);
        BigDecimal bd = TimestampConversions.toBigDecimal(ts, utcConverter);
        // InstantConversions.toBigDecimal returns seconds + nanos/1e9 as BigDecimal
        BigDecimal expected = BigDecimal.valueOf(instant.getEpochSecond())
                .add(BigDecimal.valueOf(instant.getNano(), 9));
        assertEquals(0, expected.compareTo(bd));
    }

    @Test
    public void testToBigInteger() {
        Instant instant = Instant.ofEpochSecond(1_600_000_000L);
        Timestamp ts = ts(instant);
        BigInteger bi = TimestampConversions.toBigInteger(ts, utcConverter);
        // InstantConversions.toBigInteger returns epoch seconds as BigInteger
        assertEquals(BigInteger.valueOf(1_600_000_000L), bi);
    }

    @Test
    public void testToLocalDateTime() {
        Instant instant = Instant.parse("2022-11-30T23:45:01Z");
        Timestamp ts = ts(instant);
        LocalDateTime ldt = TimestampConversions.toLocalDateTime(ts, utcConverter);
        assertEquals(LocalDateTime.ofInstant(instant, utcZone), ldt);
    }

    @Test
    public void testToDuration() {
        Instant instant = Instant.parse("1970-01-01T00:00:10.500Z");
        Timestamp ts = ts(instant);
        Duration d = TimestampConversions.toDuration(ts, utcConverter);
        assertEquals(Duration.ofSeconds(10, 500_000_000), d);
    }

    @Test
    public void testToOffsetDateTime() {
        Instant instant = Instant.parse("2021-07-01T08:15:30.250Z");
        Timestamp ts = ts(instant);
        OffsetDateTime odt = TimestampConversions.toOffsetDateTime(ts, utcConverter);
        assertEquals(OffsetDateTime.ofInstant(instant, utcZone), odt);
    }

    @Test
    public void testToCalendar() {
        Instant instant = Instant.parse("2000-01-01T00:00:00Z");
        Timestamp ts = ts(instant);
        Calendar cal = TimestampConversions.toCalendar(ts, utcConverter);
        assertEquals(TimeZone.getTimeZone(utcZone), cal.getTimeZone());
        assertEquals(instant.toEpochMilli(), cal.getTimeInMillis());
    }

    @Test
    public void testToDate() {
        Instant instant = Instant.now();
        Timestamp ts = ts(instant);
        Date date = TimestampConversions.toDate(ts, utcConverter);
        assertEquals(Date.from(instant), date);
    }

    @Test
    public void testToSqlDate() {
        Instant instant = Instant.parse("1999-12-31T23:59:59.999Z");
        Timestamp ts = ts(instant);
        java.sql.Date sqlDate = TimestampConversions.toSqlDate(ts, utcConverter);
        // java.sql.Date stores only the date part (time set to 00:00:00)
        assertEquals(instant.toEpochMilli(), sqlDate.getTime());
    }

    @Test
    public void testToLong() {
        Instant instant = Instant.parse("2020-02-29T12:00:00Z");
        Timestamp ts = ts(instant);
        long value = TimestampConversions.toLong(ts, utcConverter);
        assertEquals(instant.toEpochMilli(), value);
    }

    @Test
    public void testToStringWholeSeconds() {
        // nanos == 0 -> pattern without fraction
        Instant instant = Instant.parse("2023-01-01T00:00:00Z");
        Timestamp ts = ts(instant);
        String s = TimestampConversions.toString(ts, utcConverter);
        assertEquals("2023-01-01T00:00:00Z", s);
    }

    @Test
    public void testToStringMilliseconds() {
        // nanos divisible by 1_000_000 but not zero
        Instant instant = Instant.parse("2023-01-01T00:00:00.123Z");
        Timestamp ts = ts(instant);
        String s = TimestampConversions.toString(ts, utcConverter);
        assertEquals("2023-01-01T00:00:00.123Z", s);
    }

    @Test
    public void testToStringNanoseconds() {
        // nanos not divisible by 1_000_000
        Instant instant = Instant.parse("2023-01-01T00:00:00.123456789Z");
        Timestamp ts = ts(instant);
        String s = TimestampConversions.toString(ts, utcConverter);
        assertEquals("2023-01-01T00:00:00.123456789Z", s);
    }

    @Test
    public void testToMap() {
        Instant instant = Instant.parse("2022-05-04T10:20:30.987654321Z");
        Timestamp ts = ts(instant);
        Map<String, Object> map = TimestampConversions.toMap(ts, utcConverter);

        OffsetDateTime odt = TimestampConversions.toOffsetDateTime(ts, utcConverter);
        assertEquals(odt.toLocalDate().toString(), map.get(MapConversions.DATE));
        assertEquals(odt.toLocalTime().toString(), map.get(MapConversions.TIME));
        assertEquals(utcZone.toString(), map.get(MapConversions.ZONE));
        assertEquals(ts.getTime(), map.get(MapConversions.EPOCH_MILLIS));
        assertEquals(odt.getNano(), map.get(MapConversions.NANOS));
    }

    /*** ----------------------------------------------------------------------
     *  EDGE‑CASE & EXCEPTION TESTS
     *  ----------------------------------------------------------------------*/

    @Test(expected = NullPointerException.class)
    public void testToDouble_NullTimestamp() {
        TimestampConversions.toDouble(null, utcConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimal_NullTimestamp() {
        TimestampConversions.toBigDecimal(null, utcConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalDateTime_NullTimestamp() {
        TimestampConversions.toLocalDateTime(null, utcConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToDuration_NullTimestamp() {
        TimestampConversions.toDuration(null, utcConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToOffsetDateTime_NullTimestamp() {
        TimestampConversions.toOffsetDateTime(null, utcConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToCalendar_NullTimestamp() {
        TimestampConversions.toCalendar(null, utcConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToDate_NullTimestamp() {
        TimestampConversions.toDate(null, utcConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToSqlDate_NullTimestamp() {
        TimestampConversions.toSqlDate(null, utcConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToLong_NullTimestamp() {
        TimestampConversions.toLong(null, utcConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToString_NullTimestamp() {
        TimestampConversions.toString(null, utcConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullTimestamp() {
        TimestampConversions.toMap(null, utcConverter);
    }

    @Test
    public void testToString_NegativeEpoch() {
        // Timestamp before 1970‑01‑01 (negative epoch seconds)
        Instant instant = Instant.parse("-0001-01-01T00:00:00Z");
        Timestamp ts = ts(instant);
        String s = TimestampConversions.toString(ts, utcConverter);
        // The formatter includes the sign on the year, e.g. "-0001-01-01T00:00:00Z"
        assertTrue(s.startsWith("-0001-01-01T00:00:00"));
    }

    @Test
    public void testToString_MaxNanos() {
        // Nanos = 999,999,999 (maximum sub‑second precision)
        Instant instant = Instant.parse("2023-12-31T23:59:59.999999999Z");
        Timestamp ts = ts(instant);
        String s = TimestampConversions.toString(ts, utcConverter);
        assertEquals("2023-12-31T23:59:59.999999999Z", s);
    }

    @Test
    public void testToMap_EmptyZoneId() {
        // Use a ConverterOptions with a custom ZoneId to verify map reflects it
        ConverterOptions opts = new ConverterOptions();
        ZoneId customZone = ZoneId.of("America/New_York");
        opts.setZoneId(customZone);
        opts.setTimeZone(TimeZone.getTimeZone(customZone));
        Converter customConverter = new Converter(opts);

        Instant instant = Instant.parse("2022-08-15T12:00:00Z");
        Timestamp ts = ts(instant);
        Map<String, Object> map = TimestampConversions.toMap(ts, customConverter);
        assertEquals(customZone.toString(), map.get(MapConversions.ZONE));
    }

}
