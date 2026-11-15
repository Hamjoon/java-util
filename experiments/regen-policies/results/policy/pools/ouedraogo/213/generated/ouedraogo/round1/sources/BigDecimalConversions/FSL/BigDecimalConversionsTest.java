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
 * The tests use a real {@link Converter} instance (no mocks) and rely only on the
 * public API shown in the prompt.  All conversion methods are exercised with
 * typical values, boundary values and error conditions.
 */
public class BigDecimalConversionsTest {

    private Converter converter;                     // the Converter used for all tests
    private ZoneId zoneId;                           // cached zone id from the converter
    private TimeZone timeZone;                       // cached time zone from the converter

    @Before
    public void setUp() {
        // The Converter class is part of the library; we use a default options instance.
        // The concrete implementation of ConverterOptions is not shown, but the library
        // provides a public no‑arg constructor.  The default time‑zone/zone‑id are used
        // (typically the JVM defaults).  All expected values are derived from those
        // defaults, so the tests are independent of the actual default zone.
        converter = new Converter(new ConverterOptions());
        zoneId = converter.getOptions().getZoneId();
        timeZone = converter.getOptions().getTimeZone();
    }

    /* --------------------------------------------------------------------- */
    /*  Calendar conversion                                                   */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToCalendar_simpleSeconds() {
        BigDecimal seconds = new BigDecimal("2.5"); // 2.5 s = 2500 ms
        Calendar cal = BigDecimalConversions.toCalendar(seconds, converter);
        assertNotNull(cal);
        assertEquals(2500L, cal.getTimeInMillis());
        // ensure the calendar uses the converter's time‑zone
        assertEquals(timeZone, cal.getTimeZone());
    }

    @Test
    public void testToCalendar_zero() {
        Calendar cal = BigDecimalConversions.toCalendar(BigDecimal.ZERO, converter);
        assertEquals(0L, cal.getTimeInMillis());
    }

    /* --------------------------------------------------------------------- */
    /*  Instant conversion                                                    */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToInstant_withFraction() {
        BigDecimal src = new BigDecimal("12345.678901234");
        Instant instant = BigDecimalConversions.toInstant(src, converter);
        assertEquals(12345L, instant.getEpochSecond());
        assertEquals(678_901_234L, instant.getNano());
    }

    @Test
    public void testToInstant_integerOnly() {
        BigDecimal src = new BigDecimal("42");
        Instant instant = BigDecimalConversions.toInstant(src, converter);
        assertEquals(42L, instant.getEpochSecond());
        assertEquals(0L, instant.getNano());
    }

    /* --------------------------------------------------------------------- */
    /*  Duration conversion                                                   */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToDuration_withFraction() {
        BigDecimal src = new BigDecimal("5.000000001");
        Duration dur = BigDecimalConversions.toDuration(src, converter);
        assertEquals(5L, dur.getSeconds());
        assertEquals(1L, dur.getNano());
    }

    @Test
    public void testToDuration_zero() {
        Duration dur = BigDecimalConversions.toDuration(BigDecimal.ZERO, converter);
        assertEquals(Duration.ZERO, dur);
    }

    /* --------------------------------------------------------------------- */
    /*  LocalTime conversion                                                  */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToLocalTime_midnight() {
        LocalTime lt = BigDecimalConversions.toLocalTime(BigDecimal.ZERO, converter);
        assertEquals(LocalTime.MIDNIGHT, lt);
    }

    @Test
    public void testToLocalTime_withNanos() {
        // 1.5 seconds → 00:00:01.500000000
        BigDecimal src = new BigDecimal("1.5");
        LocalTime lt = BigDecimalConversions.toLocalTime(src, converter);
        assertEquals(LocalTime.of(0, 0, 1, 500_000_000), lt);
    }

    @Test
    public void testToLocalTime_lastMomentOfDay() {
        // 86399.999999999 seconds = one nanosecond before next day
        BigDecimal src = new BigDecimal("86399.999999999");
        LocalTime lt = BigDecimalConversions.toLocalTime(src, converter);
        assertEquals(LocalTime.of(23, 59, 59, 999_999_999), lt);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTime_negativeThrows() {
        BigDecimalConversions.toLocalTime(new BigDecimal("-0.1"), converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTime_overflowThrows() {
        // 86400 seconds would be 24 h, which is out of range
        BigDecimalConversions.toLocalTime(new BigDecimal("86400"), converter);
    }

    /* --------------------------------------------------------------------- */
    /*  LocalDate & LocalDateTime conversion                                   */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToLocalDate_epoch() {
        // epoch 0 → 1970‑01‑01 in any zone
        LocalDate ld = BigDecimalConversions.toLocalDate(BigDecimal.ZERO, converter);
        assertEquals(LocalDate.of(1970, 1, 1), ld);
    }

    @Test
    public void testToLocalDateTime_epoch() {
        LocalDateTime ldt = BigDecimalConversions.toLocalDateTime(BigDecimal.ZERO, converter);
        assertEquals(LocalDateTime.ofInstant(Instant.EPOCH, zoneId), ldt);
    }

    /* --------------------------------------------------------------------- */
    /*  OffsetTime conversion                                                  */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToOffsetTime_simple() {
        // 10.25 seconds → 00:00:10.250000000 with the converter's zone offset
        BigDecimal src = new BigDecimal("10.25");
        OffsetTime ot = BigDecimalConversions.toOffsetTime(src, converter);
        OffsetTime expected = OffsetTime.ofInstant(
                Instant.ofEpochSecond(10, 250_000_000), zoneId);
        assertEquals(expected, ot);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToOffsetTime_negativeThrows() {
        BigDecimalConversions.toOffsetTime(new BigDecimal("-1"), converter);
    }

    /* --------------------------------------------------------------------- */
    /*  OffsetDateTime conversion                                             */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToOffsetDateTime_epoch() {
        OffsetDateTime odt = BigDecimalConversions.toOffsetDateTime(BigDecimal.ZERO, converter);
        assertEquals(OffsetDateTime.ofInstant(Instant.EPOCH, zoneId), odt);
    }

    /* --------------------------------------------------------------------- */
    /*  ZonedDateTime conversion                                              */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToZonedDateTime_fractional() {
        BigDecimal src = new BigDecimal("100.123456789");
        ZonedDateTime zdt = BigDecimalConversions.toZonedDateTime(src, converter);
        Instant expectedInstant = Instant.ofEpochSecond(100, 123_456_789);
        assertEquals(expectedInstant.atZone(zoneId), zdt);
    }

    /* --------------------------------------------------------------------- */
    /*  java.util.Date conversion                                             */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToDate_roundTrip() {
        BigDecimal src = new BigDecimal("123456.789");
        Date date = BigDecimalConversions.toDate(src, converter);
        Instant back = date.toInstant();
        assertEquals(Instant.ofEpochSecond(123456, 789_000_000), back);
    }

    /* --------------------------------------------------------------------- */
    /*  java.sql.Date conversion                                              */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToSqlDate_midnight() {
        // 0 seconds = 1970‑01‑01
        java.sql.Date sqlDate = BigDecimalConversions.toSqlDate(BigDecimal.ZERO, converter);
        assertEquals(java.sql.Date.valueOf(LocalDate.of(1970, 1, 1)), sqlDate);
    }

    @Test
    public void testToSqlDate_nonMidnight() {
        // 86400 seconds = 1970‑01‑02 (midnight of next day)
        java.sql.Date sqlDate = BigDecimalConversions.toSqlDate(new BigDecimal("86400"), converter);
        assertEquals(java.sql.Date.valueOf(LocalDate.of(1970, 1, 2)), sqlDate);
    }

    /* --------------------------------------------------------------------- */
    /*  java.sql.Timestamp conversion                                         */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToTimestamp_withNanos() {
        BigDecimal src = new BigDecimal("3.000000123");
        Timestamp ts = BigDecimalConversions.toTimestamp(src, converter);
        assertEquals(3L, ts.getTime() / 1000); // seconds part
        assertEquals(123, ts.getNanos());
    }

    /* --------------------------------------------------------------------- */
    /*  BigInteger conversion                                                 */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToBigInteger() {
        BigDecimal src = new BigDecimal("12345678901234567890");
        BigInteger bi = BigDecimalConversions.toBigInteger(src, converter);
        assertEquals(new BigInteger("12345678901234567890"), bi);
    }

    /* --------------------------------------------------------------------- */
    /*  String conversion (stripTrailingZeros)                               */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToString_stripsTrailingZeros() {
        BigDecimal src = new BigDecimal("123.45000");
        String result = BigDecimalConversions.toString(src, converter);
        assertEquals("123.45", result);
    }

    @Test
    public void testToString_withoutFraction() {
        BigDecimal src = new BigDecimal("1000");
        String result = BigDecimalConversions.toString(src, converter);
        assertEquals("1000", result);
    }

    /* --------------------------------------------------------------------- */
    /*  UUID conversion – delegated to BigIntegerConversions                  */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToUUID_delegatesCorrectly() {
        // Use a known 128‑bit value that maps to a UUID.
        BigDecimal src = new BigDecimal(new BigInteger("12345678901234567890123456789012"));
        UUID uuidViaMethod = BigDecimalConversions.toUUID(src, converter);
        UUID uuidDirect = BigIntegerConversions.toUUID(src.toBigInteger(), converter);
        assertEquals(uuidDirect, uuidViaMethod);
    }

    /* --------------------------------------------------------------------- */
    /*  secondsAndNanosToDouble helper                                         */
    /* --------------------------------------------------------------------- */
    @Test
    public void testSecondsAndNanosToDouble_basic() {
        BigDecimal bd = BigDecimalConversions.secondsAndNanosToDouble(10L, 250_000_000L);
        assertEquals(new BigDecimal("10.250000000"), bd);
    }

    @Test
    public void testSecondsAndNanosToDouble_zeroNanos() {
        BigDecimal bd = BigDecimalConversions.secondsAndNanosToDouble(7L, 0L);
        assertEquals(new BigDecimal("7"), bd);
    }

    @Test
    public void testSecondsAndNanosToDouble_maxNanos() {
        // 1 second + 999 999 999 ns = just under 2 seconds
        BigDecimal bd = BigDecimalConversions.secondsAndNanosToDouble(1L, 999_999_999L);
        assertEquals(new BigDecimal("1.999999999"), bd);
    }
}
