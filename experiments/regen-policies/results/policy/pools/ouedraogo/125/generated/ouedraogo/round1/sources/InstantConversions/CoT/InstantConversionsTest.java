package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class InstantConversionsTest {

    private Converter converter;
    private ConverterOptions options;

    @Before
    public void setUp() {
        // The library provides a default‑constructor for ConverterOptions that uses the
        // system default ZoneId / TimeZone.  This is sufficient for the conversion tests.
        options = new ConverterOptions();               // assumes a public no‑arg ctor
        converter = new Converter(options);
    }

    /* ---------------------------------------------------------------------- */
    /*  1.  toMap                                                             */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToMapBasic() {
        Instant instant = Instant.ofEpochSecond(12345L, 678_000_000);
        Map<String, Object> map = InstantConversions.toMap(instant, converter);

        assertEquals(2, map.size());
        assertTrue(map.containsKey("seconds"));
        assertTrue(map.containsKey("nanos"));
        assertEquals(12345L, map.get("seconds"));
        assertEquals(678_000_000, map.get("nanos"));
    }

    @Test(expected = NullPointerException.class)
    public void testToMapWithNull() {
        InstantConversions.toMap(null, converter);
    }

    /* ---------------------------------------------------------------------- */
    /*  2.  toZonedDateTime                                                    */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToZonedDateTime() {
        Instant instant = Instant.parse("2022-03-15T12:34:56.123456789Z");
        ZonedDateTime zdt = InstantConversions.toZonedDateTime(instant, converter);

        ZoneId expectedZone = options.getZoneId();               // should be system default
        ZonedDateTime expected = instant.atZone(expectedZone);
        assertEquals(expected, zdt);
    }

    @Test(expected = NullPointerException.class)
    public void testToZonedDateTimeWithNull() {
        InstantConversions.toZonedDateTime(null, converter);
    }

    /* ---------------------------------------------------------------------- */
    /*  3.  toOffsetDateTime                                                  */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToOffsetDateTime() {
        Instant instant = Instant.parse("2021-01-01T00:00:00Z");
        OffsetDateTime odt = InstantConversions.toOffsetDateTime(instant, converter);

        TimeZone tz = options.getTimeZone();
        int offsetSeconds = tz.getOffset(instant.toEpochMilli()) / 1000;
        ZoneOffset expectedOffset = ZoneOffset.ofTotalSeconds(offsetSeconds);
        OffsetDateTime expected = instant.atOffset(expectedOffset);

        assertEquals(expected, odt);
    }

    @Test(expected = NullPointerException.class)
    public void testToOffsetDateTimeWithNull() {
        InstantConversions.toOffsetDateTime(null, converter);
    }

    /* ---------------------------------------------------------------------- */
    /*  4.  toLong                                                            */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToLong() {
        Instant instant = Instant.ofEpochSecond(10L, 500_000_000); // 10.5 seconds
        long millis = InstantConversions.toLong(instant, converter);
        assertEquals(10_500L, millis);
    }

    @Test(expected = NullPointerException.class)
    public void testToLongWithNull() {
        InstantConversions.toLong(null, converter);
    }

    /* ---------------------------------------------------------------------- */
    /*  5.  toDouble                                                          */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToDouble() {
        Instant instant = Instant.ofEpochSecond(5L, 250_000_000); // 5.25 seconds
        double d = InstantConversions.toDouble(instant, converter);
        assertEquals(5.25, d, 0.000_000_001);
    }

    @Test(expected = NullPointerException.class)
    public void testToDoubleWithNull() {
        InstantConversions.toDouble(null, converter);
    }

    /* ---------------------------------------------------------------------- */
    /*  6.  toAtomicLong                                                      */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToAtomicLong() {
        Instant instant = Instant.ofEpochSecond(2L, 0);
        AtomicLong atomic = InstantConversions.toAtomicLong(instant, converter);
        assertEquals(2_000L, atomic.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLongWithNull() {
        InstantConversions.toAtomicLong(null, converter);
    }

    /* ---------------------------------------------------------------------- */
    /*  7.  toTimestamp                                                       */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToTimestamp() {
        Instant instant = Instant.parse("2020-12-31T23:59:59.999999999Z");
        Timestamp ts = InstantConversions.toTimestamp(instant, converter);
        assertEquals(Timestamp.from(instant), ts);
    }

    @Test(expected = NullPointerException.class)
    public void testToTimestampWithNull() {
        InstantConversions.toTimestamp(null, converter);
    }

    /* ---------------------------------------------------------------------- */
    /*  8.  toSqlDate                                                         */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToSqlDate() {
        Instant instant = Instant.ofEpochSecond(0L, 0);
        java.sql.Date sqlDate = InstantConversions.toSqlDate(instant, converter);
        assertEquals(new java.sql.Date(0L), sqlDate);
    }

    @Test(expected = NullPointerException.class)
    public void testToSqlDateWithNull() {
        InstantConversions.toSqlDate(null, converter);
    }

    /* ---------------------------------------------------------------------- */
    /*  9.  toDate                                                            */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToDate() {
        Instant instant = Instant.ofEpochSecond(1L, 0);
        Date date = InstantConversions.toDate(instant, converter);
        assertEquals(new Date(1_000L), date);
    }

    @Test(expected = NullPointerException.class)
    public void testToDateWithNull() {
        InstantConversions.toDate(null, converter);
    }

    /* ---------------------------------------------------------------------- */
    /* 10.  toCalendar                                                        */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToCalendar() {
        Instant instant = Instant.ofEpochSecond(1622548800L, 123_456_789); // 2021‑06‑01T12:00:00.123456789Z
        Calendar cal = InstantConversions.toCalendar(instant, converter);
        assertEquals(TimeZone.getTimeZone(options.getZoneId()), cal.getTimeZone());
        assertEquals(instant.toEpochMilli(), cal.getTimeInMillis());
    }

    @Test(expected = NullPointerException.class)
    public void testToCalendarWithNull() {
        InstantConversions.toCalendar(null, converter);
    }

    /* ---------------------------------------------------------------------- */
    /* 11.  toBigInteger                                                      */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToBigInteger() {
        Instant instant = Instant.ofEpochSecond(3L, 456_000_000);
        BigInteger bi = InstantConversions.toBigInteger(instant, converter);
        BigInteger expected = BigInteger.valueOf(3L).multiply(BigInteger.valueOf(1_000_000_000L))
                .add(BigInteger.valueOf(456_000_000L));
        assertEquals(expected, bi);
    }

    @Test
    public void testToBigIntegerEdgeCases() {
        // Instant.MIN (seconds = -31557014167219200, nanos = 0)
        Instant min = Instant.MIN;
        BigInteger minBi = InstantConversions.toBigInteger(min, converter);
        BigInteger expectedMin = BigInteger.valueOf(min.getEpochSecond())
                .multiply(BigInteger.valueOf(1_000_000_000L))
                .add(BigInteger.valueOf(min.getNano()));
        assertEquals(expectedMin, minBi);

        // Instant.MAX (seconds = 31556889864403199, nanos = 999_999_999)
        Instant max = Instant.MAX;
        BigInteger maxBi = InstantConversions.toBigInteger(max, converter);
        BigInteger expectedMax = BigInteger.valueOf(max.getEpochSecond())
                .multiply(BigInteger.valueOf(1_000_000_000L))
                .add(BigInteger.valueOf(max.getNano()));
        assertEquals(expectedMax, maxBi);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigIntegerWithNull() {
        InstantConversions.toBigInteger(null, converter);
    }

    /* ---------------------------------------------------------------------- */
    /* 12.  toBigDecimal                                                      */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToBigDecimal() {
        Instant instant = Instant.ofEpochSecond(7L, 890_000_000);
        BigDecimal bd = InstantConversions.toBigDecimal(instant, converter);
        BigDecimal expected = BigDecimalConversions.secondsAndNanosToDouble(7L, 890_000_000);
        assertEquals(0, expected.compareTo(bd));
    }

    @Test
    public void testToBigDecimalEdgeCases() {
        // zero instant
        Instant zero = Instant.EPOCH;
        BigDecimal bdZero = InstantConversions.toBigDecimal(zero, converter);
        assertEquals(BigDecimal.ZERO, bdZero);

        // maximum nanos within a second
        Instant withMaxNanos = Instant.ofEpochSecond(1L, 999_999_999);
        BigDecimal bdMaxNanos = InstantConversions.toBigDecimal(withMaxNanos, converter);
        BigDecimal expected = BigDecimalConversions.secondsAndNanosToDouble(1L, 999_999_999);
        assertEquals(0, expected.compareTo(bdMaxNanos));
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimalWithNull() {
        InstantConversions.toBigDecimal(null, converter);
    }

    /* ---------------------------------------------------------------------- */
    /* 13.  toLocalDateTime                                                   */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToLocalDateTime() {
        Instant instant = Instant.parse("2023-08-10T15:30:45.123456789Z");
        LocalDateTime ldt = InstantConversions.toLocalDateTime(instant, converter);
        LocalDateTime expected = instant.atZone(options.getZoneId()).toLocalDateTime();
        assertEquals(expected, ldt);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalDateTimeWithNull() {
        InstantConversions.toLocalDateTime(null, converter);
    }

    /* ---------------------------------------------------------------------- */
    /* 14.  toLocalDate                                                       */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToLocalDate() {
        Instant instant = Instant.parse("1999-12-31T23:59:59Z");
        LocalDate ld = InstantConversions.toLocalDate(instant, converter);
        LocalDate expected = instant.atZone(options.getZoneId()).toLocalDate();
        assertEquals(expected, ld);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalDateWithNull() {
        InstantConversions.toLocalDate(null, converter);
    }

    /* ---------------------------------------------------------------------- */
    /* 15.  toLocalTime                                                       */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToLocalTime() {
        Instant instant = Instant.parse("2000-01-01T00:00:00.999999999Z");
        LocalTime lt = InstantConversions.toLocalTime(instant, converter);
        LocalTime expected = instant.atZone(options.getZoneId()).toLocalTime();
        assertEquals(expected, lt);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalTimeWithNull() {
        InstantConversions.toLocalTime(null, converter);
    }
}
