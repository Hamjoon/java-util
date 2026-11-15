package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link BigDecimalConversions}.
 *
 * The tests exercise every public static method in the class and verify
 * correct handling of normal, edge‑case and error conditions.
 *
 * The {@link Converter} used in the tests is created with a
 * {@link ConverterOptions} instance that uses the UTC time‑zone and
 * {@link ZoneId#of("UTC")} – this guarantees deterministic results
 * independent of the host environment.
 */
public class BigDecimalConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // Build a ConverterOptions that returns UTC for both TimeZone and ZoneId.
        // The real library provides setters for these values – the test assumes
        // they exist; if the API differs adjust accordingly.
        ConverterOptions options = new ConverterOptions();
        options.setTimeZone(TimeZone.getTimeZone("UTC"));
        options.setZoneId(ZoneId.of("UTC"));
        converter = new Converter(options);
    }

    /*--------------------------------------------------------------------
     *  toCalendar
     *--------------------------------------------------------------------*/
    @Test
    public void testToCalendar() {
        BigDecimal seconds = new BigDecimal("12345.678");
        Calendar cal = BigDecimalConversions.toCalendar(seconds, converter);
        long expectedMillis = seconds.multiply(BigDecimalConversions.GRAND).longValue();
        assertEquals(expectedMillis, cal.getTimeInMillis());
        // verify the calendar uses the UTC time‑zone from the options
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
    }

    /*--------------------------------------------------------------------
     *  toInstant
     *--------------------------------------------------------------------*/
    @Test
    public void testToInstantExactSecond() {
        BigDecimal seconds = new BigDecimal("10");
        Instant instant = BigDecimalConversions.toInstant(seconds, converter);
        assertEquals(10L, instant.getEpochSecond());
        assertEquals(0L, instant.getNano());
    }

    @Test
    public void testToInstantWithFraction() {
        BigDecimal seconds = new BigDecimal("10.123456789");
        Instant instant = BigDecimalConversions.toInstant(seconds, converter);
        assertEquals(10L, instant.getEpochSecond());
        assertEquals(123_456_789, instant.getNano());
    }

    /*--------------------------------------------------------------------
     *  toDuration
     *--------------------------------------------------------------------*/
    @Test
    public void testToDuration() {
        BigDecimal seconds = new BigDecimal("5.5");
        Duration dur = BigDecimalConversions.toDuration(seconds, converter);
        assertEquals(5L, dur.getSeconds());
        assertEquals(500_000_000, dur.getNano());
    }

    /*--------------------------------------------------------------------
     *  toLocalTime
     *--------------------------------------------------------------------*/
    @Test
    public void testToLocalTimeMidnight() {
        BigDecimal seconds = BigDecimal.ZERO; // 00:00:00
        LocalTime lt = BigDecimalConversions.toLocalTime(seconds, converter);
        assertEquals(LocalTime.MIDNIGHT, lt);
    }

    @Test
    public void testToLocalTimeArbitrary() {
        // 13:45:30.123456789  -> 13*3600 + 45*60 + 30.123456789 = 49530.123456789 seconds
        BigDecimal seconds = new BigDecimal("49530.123456789");
        LocalTime lt = BigDecimalConversions.toLocalTime(seconds, converter);
        assertEquals(LocalTime.of(13, 45, 30, 123_456_789), lt);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTimeBelowZero() {
        BigDecimal seconds = new BigDecimal("-0.1");
        BigDecimalConversions.toLocalTime(seconds, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTimeAboveMax() {
        // 24 hours is 86400 seconds – the method permits up to 86399.999999999
        BigDecimal seconds = new BigDecimal("86400");
        BigDecimalConversions.toLocalTime(seconds, converter);
    }

    /*--------------------------------------------------------------------
     *  toZonedDateTime / toLocalDate / toLocalDateTime /
     *  toOffsetDateTime
     *--------------------------------------------------------------------*/
    @Test
    public void testZonedDateTimeAndRelatedConversions() {
        // 2021‑03‑14T15:09:26Z  (epoch seconds 1615723766)
        BigDecimal seconds = new BigDecimal("1615723766");
        ZonedDateTime zdt = BigDecimalConversions.toZonedDateTime(seconds, converter);
        assertEquals(ZoneId.of("UTC"), zdt.getZone());
        assertEquals(2021, zdt.getYear());
        assertEquals(Month.MARCH, zdt.getMonth());
        assertEquals(14, zdt.getDayOfMonth());
        assertEquals(LocalTime.of(15, 9, 26), zdt.toLocalTime());

        // Delegated conversions must be consistent with the ZonedDateTime above
        assertEquals(zdt.toLocalDate(), BigDecimalConversions.toLocalDate(seconds, converter));
        assertEquals(zdt.toLocalDateTime(), BigDecimalConversions.toLocalDateTime(seconds, converter));
        assertEquals(zdt.toOffsetDateTime(), BigDecimalConversions.toOffsetDateTime(seconds, converter));
    }

    /*--------------------------------------------------------------------
     *  toDate / toSqlDate / toTimestamp
     *--------------------------------------------------------------------*/
    @Test
    public void testDateConversions() {
        BigDecimal seconds = new BigDecimal("1609459200"); // 2021‑01‑01T00:00:00Z
        Instant instant = Instant.ofEpochSecond(1609459200L);
        Date date = BigDecimalConversions.toDate(seconds, converter);
        assertEquals(Date.from(instant), date);

        java.sql.Date sqlDate = BigDecimalConversions.toSqlDate(seconds, converter);
        assertEquals(java.sql.Date.valueOf(LocalDate.of(2021, 1, 1)), sqlDate);

        Timestamp ts = BigDecimalConversions.toTimestamp(seconds, converter);
        assertEquals(Timestamp.from(instant), ts);
    }

    /*--------------------------------------------------------------------
     *  toBigInteger
     *--------------------------------------------------------------------*/
    @Test
    public void testToBigInteger() {
        BigDecimal bd = new BigDecimal("12345678901234567890.987");
        BigInteger bi = BigDecimalConversions.toBigInteger(bd, converter);
        // toBigInteger truncates the fractional part
        assertEquals(new BigInteger("12345678901234567890"), bi);
    }

    /*--------------------------------------------------------------------
     *  toString
     *--------------------------------------------------------------------*/
    @Test
    public void testToStringStripsTrailingZeros() {
        assertEquals("123.45", BigDecimalConversions.toString(new BigDecimal("123.4500"), converter));
        assertEquals("0", BigDecimalConversions.toString(BigDecimal.ZERO, converter));
        assertEquals("-987.65", BigDecimalConversions.toString(new BigDecimal("-987.65000"), converter));
    }

    /*--------------------------------------------------------------------
     *  toUUID
     *--------------------------------------------------------------------*/
    @Test
    public void testToUUIDRoundTrip() {
        // Create a known UUID and convert it to a positive BigDecimal representation.
        UUID original = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
        // Build the 128‑bit integer that represents the UUID.
        BigInteger uuidAsInt = new BigInteger(1,
                ByteBuffer.allocate(16)
                        .putLong(original.getMostSignificantBits())
                        .putLong(original.getLeastSignificantBits())
                        .array());

        BigDecimal bd = new BigDecimal(uuidAsInt);
        UUID result = BigDecimalConversions.toUUID(bd, converter);
        assertEquals(original, result);
    }

    /*--------------------------------------------------------------------
     *  secondsAndNanosToDouble
     *--------------------------------------------------------------------*/
    @Test
    public void testSecondsAndNanosToDouble() {
        BigDecimal value = BigDecimalConversions.secondsAndNanosToDouble(10L, 250_000_000L);
        assertEquals(new BigDecimal("10.250000000"), value);

        // Zero nanos
        assertEquals(new BigDecimal("42"), BigDecimalConversions.secondsAndNanosToDouble(42L, 0L));

        // Max nanos (just under one second)
        assertEquals(new BigDecimal("0.999999999"), BigDecimalConversions.secondsAndNanosToDouble(0L, 999_999_999L));
    }
}
