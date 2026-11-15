package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

/**
 * Comprehensive JUnit‑4 test suite for {@link BigDecimalConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>normal / typical values</li>
 *   <li>boundary conditions (midnight, last nanosecond of a day, epoch, etc.)</li>
 *   <li>error scenarios that must throw {@link IllegalArgumentException}</li>
 *   <li>validation that the conversion utilities are consistent with each other</li>
 * </ul>
 *
 * The {@link Converter} is instantiated with a fresh {@link ConverterOptions}
 * object for each test, relying on the default time‑zone (UTC) which makes the
 * expected results deterministic.
 */
public class BigDecimalConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // The library supplies a public ConverterOptions class with a no‑arg ctor.
        // Its default time‑zone is UTC, which is suitable for deterministic testing.
        ConverterOptions options = new ConverterOptions();
        // Ensure the defaults are UTC – this is important for the expected values.
        options.setTimeZone(TimeZone.getTimeZone("UTC"));
        options.setZoneId(ZoneId.of("UTC"));
        converter = new Converter(options);
    }

    /* -------------------------------------------------------------------- */
    /*  toCalendar                                                          */
    /* -------------------------------------------------------------------- */
    @Test
    public void testToCalendar_Typical() {
        BigDecimal seconds = new BigDecimal("1.5"); // 1.5 sec = 1500 ms
        Calendar cal = BigDecimalConversions.toCalendar(seconds, converter);
        assertEquals(1500L, cal.getTimeInMillis());
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
    }

    @Test
    public void testToCalendar_Negative() {
        BigDecimal seconds = new BigDecimal("-2.25"); // -2250 ms
        Calendar cal = BigDecimalConversions.toCalendar(seconds, converter);
        assertEquals(-2250L, cal.getTimeInMillis());
    }

    /* -------------------------------------------------------------------- */
    /*  toInstant                                                            */
    /* -------------------------------------------------------------------- */
    @Test
    public void testToInstant_Typical() {
        BigDecimal seconds = new BigDecimal("123.456789012");
        Instant instant = BigDecimalConversions.toInstant(seconds, converter);
        assertEquals(123L, instant.getEpochSecond());
        assertEquals(456_789_012L, instant.getNano());
    }

    @Test
    public void testToInstant_WholeSecondOnly() {
        BigDecimal seconds = new BigDecimal("42");
        Instant instant = BigDecimalConversions.toInstant(seconds, converter);
        assertEquals(42L, instant.getEpochSecond());
        assertEquals(0L, instant.getNano());
    }

    /* -------------------------------------------------------------------- */
    /*  toDuration                                                          */
    /* -------------------------------------------------------------------- */
    @Test
    public void testToDuration_Typical() {
        BigDecimal seconds = new BigDecimal("5.000000001");
        Duration d = BigDecimalConversions.toDuration(seconds, converter);
        assertEquals(5L, d.getSeconds());
        assertEquals(1L, d.getNano());
    }

    @Test
    public void testToDuration_Zero() {
        BigDecimal seconds = BigDecimal.ZERO;
        Duration d = BigDecimalConversions.toDuration(seconds, converter);
        assertEquals(Duration.ZERO, d);
    }

    /* -------------------------------------------------------------------- */
    /*  toLocalTime                                                         */
    /* -------------------------------------------------------------------- */
    @Test
    public void testToLocalTime_Midnight() {
        BigDecimal seconds = BigDecimal.ZERO;
        LocalTime lt = BigDecimalConversions.toLocalTime(seconds, converter);
        assertEquals(LocalTime.MIDNIGHT, lt);
    }

    @Test
    public void testToLocalTime_LastNanosecondOfDay() {
        // 86 399.999 999 999 seconds = one nanosecond before next day
        BigDecimal seconds = new BigDecimal("86399.999999999");
        LocalTime lt = BigDecimalConversions.toLocalTime(seconds, converter);
        assertEquals(LocalTime.MAX, lt);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTime_TooLarge() {
        // One second beyond the allowed range
        BigDecimal seconds = new BigDecimal("86400");
        BigDecimalConversions.toLocalTime(seconds, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTime_Negative() {
        BigDecimal seconds = new BigDecimal("-0.1");
        BigDecimalConversions.toLocalTime(seconds, converter);
    }

    /* -------------------------------------------------------------------- */
    /*  toLocalDate & toLocalDateTime (via ZonedDateTime)                    */
    /* -------------------------------------------------------------------- */
    @Test
    public void testToLocalDate_Epoch() {
        // 0 seconds since the epoch => 1970‑01‑01 in UTC
        BigDecimal seconds = BigDecimal.ZERO;
        LocalDate ld = BigDecimalConversions.toLocalDate(seconds, converter);
        assertEquals(LocalDate.of(1970, 1, 1), ld);
    }

    @Test
    public void testToLocalDateTime_Epoch() {
        BigDecimal seconds = BigDecimal.ZERO;
        LocalDateTime ldt = BigDecimalConversions.toLocalDateTime(seconds, converter);
        assertEquals(LocalDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC), ldt);
    }

    @Test
    public void testToLocalDate_SpecificDate() {
        // 2022‑03‑14 15:09:26 UTC
        long epochSec = 1647264566L;
        BigDecimal seconds = BigDecimal.valueOf(epochSec);
        LocalDate ld = BigDecimalConversions.toLocalDate(seconds, converter);
        assertEquals(LocalDate.of(2022, 3, 14), ld);
    }

    /* -------------------------------------------------------------------- */
    /*  toOffsetTime                                                        */
    /* -------------------------------------------------------------------- */
    @Test
    public void testToOffsetTime_Typical() {
        // 0.5 seconds => 00:00:00.500 with UTC offset
        BigDecimal seconds = new BigDecimal("0.5");
        OffsetTime ot = BigDecimalConversions.toOffsetTime(seconds, converter);
        assertEquals(OffsetTime.of(LocalTime.ofNanoOfDay(500_000_000L), ZoneOffset.UTC), ot);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToOffsetTime_TooLarge() {
        BigDecimal seconds = new BigDecimal("86400"); // out of range for a time‑of‑day
        BigDecimalConversions.toOffsetTime(seconds, converter);
    }

    /* -------------------------------------------------------------------- */
    /*  toOffsetDateTime                                                    */
    /* -------------------------------------------------------------------- */
    @Test
    public void testToOffsetDateTime_Epoch() {
        OffsetDateTime odt = BigDecimalConversions.toOffsetDateTime(BigDecimal.ZERO, converter);
        assertEquals(OffsetDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC), odt);
    }

    /* -------------------------------------------------------------------- */
    /*  toZonedDateTime                                                     */
    /* -------------------------------------------------------------------- */
    @Test
    public void testToZonedDateTime_Typical() {
        // 2021‑12‑31 23:59:59 UTC
        BigDecimal seconds = BigDecimal.valueOf(1640995199L);
        ZonedDateTime zdt = BigDecimalConversions.toZonedDateTime(seconds, converter);
        assertEquals(ZonedDateTime.ofInstant(Instant.ofEpochSecond(1640995199L), ZoneOffset.UTC), zdt);
    }

    /* -------------------------------------------------------------------- */
    /*  toDate & toTimestamp                                                */
    /* -------------------------------------------------------------------- */
    @Test
    public void testToDate_Typical() {
        BigDecimal seconds = new BigDecimal("10.123456789");
        Date date = BigDecimalConversions.toDate(seconds, converter);
        Instant expected = Instant.ofEpochSecond(10L, 123_456_789L);
        assertEquals(Date.from(expected), date);
    }

    @Test
    public void testToTimestamp_Typical() {
        BigDecimal seconds = new BigDecimal("0.001");
        Timestamp ts = BigDecimalConversions.toTimestamp(seconds, converter);
        Instant expected = Instant.ofEpochSecond(0L, 1_000_000L);
        assertEquals(Timestamp.from(expected), ts);
    }

    /* -------------------------------------------------------------------- */
    /*  toSqlDate                                                          */
    /* -------------------------------------------------------------------- */
    @Test
    public void testToSqlDate_Epoch() {
        java.sql.Date sqlDate = BigDecimalConversions.toSqlDate(BigDecimal.ZERO, converter);
        assertEquals(java.sql.Date.valueOf(LocalDate.of(1970, 1, 1)), sqlDate);
    }

    @Test
    public void testToSqlDate_NonZero() {
        // 2020‑02‑29 12:34:56 UTC (leap day)
        Instant instant = Instant.parse("2020-02-29T12:34:56Z");
        BigDecimal seconds = BigDecimal.valueOf(instant.getEpochSecond());
        java.sql.Date sqlDate = BigDecimalConversions.toSqlDate(seconds, converter);
        assertEquals(java.sql.Date.valueOf(LocalDate.of(2020, 2, 29)), sqlDate);
    }

    /* -------------------------------------------------------------------- */
    /*  toBigInteger                                                        */
    /* -------------------------------------------------------------------- */
    @Test
    public void testToBigInteger_Typical() {
        BigDecimal bd = new BigDecimal("12345678901234567890");
        BigInteger bi = BigDecimalConversions.toBigInteger(bd, converter);
        assertEquals(new BigInteger("12345678901234567890"), bi);
    }

    @Test
    public void testToBigInteger_WithFraction() {
        // Fractional part is discarded by BigDecimal.toBigInteger()
        BigDecimal bd = new BigDecimal("42.999");
        BigInteger bi = BigDecimalConversions.toBigInteger(bd, converter);
        assertEquals(BigInteger.valueOf(42L), bi);
    }

    /* -------------------------------------------------------------------- */
    /*  toString                                                            */
    /* -------------------------------------------------------------------- */
    @Test
    public void testToString_StripTrailingZeros() {
        BigDecimal bd = new BigDecimal("1.2300");
        String s = BigDecimalConversions.toString(bd, converter);
        assertEquals("1.23", s);
    }

    @Test
    public void testToString_NoFraction() {
        BigDecimal bd = new BigDecimal("100");
        String s = BigDecimalConversions.toString(bd, converter);
        assertEquals("100", s);
    }

    /* -------------------------------------------------------------------- */
    /*  toUUID                                                               */
    /* -------------------------------------------------------------------- */
    @Test
    public void testToUUID_Zero() {
        // Zero should map to the nil UUID (all bits zero). The concrete mapping
        // performed by BigIntegerConversions.toUUID is not part of this test;
        // we only verify that a UUID instance is produced and that it matches
        // the expected nil UUID for the known input value 0.
        BigDecimal bd = BigDecimal.ZERO;
        UUID uuid = BigDecimalConversions.toUUID(bd, converter);
        assertNotNull(uuid);
        assertEquals(new UUID(0L, 0L), uuid);
    }

    @Test
    public void testToUUID_RandomValue() {
        // Use a deterministic positive BigInteger that fits into 128 bits.
        BigDecimal bd = new BigDecimal("340282366920938463463374607431768211455"); // 2^128-1
        UUID uuid = BigDecimalConversions.toUUID(bd, converter);
        assertNotNull(uuid);
        // The most‑significant and least‑significant bits should both be -1
        // because 2^128‑1 corresponds to all 1 bits.
        assertEquals(-1L, uuid.getMostSignificantBits());
        assertEquals(-1L, uuid.getLeastSignificantBits());
    }

    /* -------------------------------------------------------------------- */
    /*  secondsAndNanosToDouble                                             */
    /* -------------------------------------------------------------------- */
    @Test
    public void testSecondsAndNanosToDouble_Typical() {
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(12L, 345_000_000L);
        assertEquals(new BigDecimal("12.345"), result);
    }

    @Test
    public void testSecondsAndNanosToDouble_NanosZero() {
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(7L, 0L);
        assertEquals(BigDecimal.valueOf(7L), result);
    }

    @Test
    public void testSecondsAndNanosToDouble_MaxNanos() {
        // 999,999,999 nanos = 0.999999999 seconds
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(0L, 999_999_999L);
        assertEquals(new BigDecimal("0.999999999"), result);
    }
}
