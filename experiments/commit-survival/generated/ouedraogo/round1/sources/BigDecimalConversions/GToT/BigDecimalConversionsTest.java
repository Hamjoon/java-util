package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@code BigDecimalConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>All static conversion methods with typical (happy‑path) inputs.</li>
 *   <li>Edge‑case values such as zero, the maximum valid {@code LocalTime},
 *       negative values and values that exceed a day.</li>
 *   <li>Exception handling – especially the {@code IllegalArgumentException}
 *       thrown by {@code toLocalTime} for out‑of‑range inputs.</li>
 * </ul>
 *
 * The {@link Converter} instance is created once per test class using a
 * {@link ConverterOptions} that is configured for the UTC time‑zone.  This
 * guarantees deterministic results independent of the machine’s default
 * locale or time‑zone.
 */
public class BigDecimalConversionsTest {

    private Converter converter;

    /**
     * Build a {@link Converter} that always uses UTC.  The concrete
     * {@code ConverterOptions} class is part of the library under test – it
     * provides {@code setTimeZone(TimeZone)} and {@code setZoneId(ZoneId)}.
     * If the real class uses different method names the compiler will point
     * it out; the test can be adjusted accordingly.
     */
    @Before
    public void setUp() {
        ConverterOptions options = new ConverterOptions();
        options.setTimeZone(TimeZone.getTimeZone("UTC"));
        options.setZoneId(ZoneId.of("UTC"));
        converter = new Converter(options);
    }

    /* --------------------------------------------------------------------- */
    /*  List of public (package‑private static) methods in BigDecimalConversions */
    /* --------------------------------------------------------------------- */
    // static Calendar   toCalendar(Object, Converter)
    // static Instant    toInstant(Object, Converter)
    // static Duration   toDuration(Object, Converter)
    // static LocalTime  toLocalTime(Object, Converter)
    // static LocalDate  toLocalDate(Object, Converter)
    // static LocalDateTime toLocalDateTime(Object, Converter)
    // static OffsetDateTime toOffsetDateTime(Object, Converter)
    // static ZonedDateTime   toZonedDateTime(Object, Converter)
    // static Date       toDate(Object, Converter)
    // static java.sql.Date toSqlDate(Object, Converter)
    // static Timestamp  toTimestamp(Object, Converter)
    // static BigInteger toBigInteger(Object, Converter)
    // static String     toString(Object, Converter)
    // static UUID       toUUID(Object, Converter)
    // static BigDecimal secondsAndNanosToDouble(long, long)

    /* --------------------------------------------------------------------- */
    /*  Happy‑path tests */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToCalendar() {
        BigDecimal seconds = new BigDecimal("1.234"); // 1.234 s = 1234 ms
        Calendar cal = BigDecimalConversions.toCalendar(seconds, converter);
        assertEquals(1234L, cal.getTimeInMillis());
    }

    @Test
    public void testToInstant() {
        // 123.456789012 seconds = 123 s + 456,789,012 ns
        BigDecimal bd = new BigDecimal("123.456789012");
        Instant instant = BigDecimalConversions.toInstant(bd, converter);
        assertEquals(123L, instant.getEpochSecond());
        assertEquals(456_789_012L, instant.getNano());
    }

    @Test
    public void testToDuration() {
        BigDecimal bd = new BigDecimal("5.000000001"); // 5 s + 1 ns
        Duration dur = BigDecimalConversions.toDuration(bd, converter);
        assertEquals(5L, dur.getSeconds());
        assertEquals(1L, dur.getNano());
    }

    @Test
    public void testToLocalTime_Valid() {
        // 13:45:30.123456789 -> seconds since midnight = 13*3600 + 45*60 + 30 + 0.123456789
        BigDecimal seconds = new BigDecimal("49530.123456789");
        LocalTime lt = BigDecimalConversions.toLocalTime(seconds, converter);
        assertEquals(LocalTime.of(13, 45, 30, 123_456_789), lt);
    }

    @Test
    public void testToLocalDate() {
        // Epoch day 0 = 1970‑01‑01
        BigDecimal seconds = BigDecimal.ZERO;
        LocalDate ld = BigDecimalConversions.toLocalDate(seconds, converter);
        assertEquals(LocalDate.ofEpochDay(0), ld);
    }

    @Test
    public void testToLocalDateTime() {
        BigDecimal seconds = new BigDecimal("1609459200"); // 2021‑01‑01T00:00:00Z
        LocalDateTime ldt = BigDecimalConversions.toLocalDateTime(seconds, converter);
        assertEquals(LocalDateTime.of(2021, 1, 1, 0, 0), ldt);
    }

    @Test
    public void testToOffsetDateTime() {
        BigDecimal seconds = new BigDecimal("0"); // epoch
        OffsetDateTime odt = BigDecimalConversions.toOffsetDateTime(seconds, converter);
        assertEquals(OffsetDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC), odt);
    }

    @Test
    public void testToZonedDateTime() {
        BigDecimal seconds = new BigDecimal("86400"); // exactly one day after epoch
        ZonedDateTime zdt = BigDecimalConversions.toZonedDateTime(seconds, converter);
        assertEquals(ZonedDateTime.ofInstant(Instant.ofEpochSecond(86400), ZoneId.of("UTC")), zdt);
    }

    @Test
    public void testToDate() {
        BigDecimal seconds = new BigDecimal("10"); // 10 s after epoch
        Date date = BigDecimalConversions.toDate(seconds, converter);
        assertEquals(Date.from(Instant.ofEpochSecond(10)), date);
    }

    @Test
    public void testToSqlDate() {
        BigDecimal seconds = new BigDecimal("0");
        java.sql.Date sqlDate = BigDecimalConversions.toSqlDate(seconds, converter);
        assertEquals(new java.sql.Date(0L), sqlDate);
    }

    @Test
    public void testToTimestamp() {
        BigDecimal seconds = new BigDecimal("12345.678"); // 12 345 s + 678 ms
        Timestamp ts = BigDecimalConversions.toTimestamp(seconds, converter);
        assertEquals(12_345_678L, ts.getTime()); // milliseconds
    }

    @Test
    public void testToBigInteger() {
        BigDecimal bd = new BigDecimal("12345678901234567890");
        BigInteger bi = BigDecimalConversions.toBigInteger(bd, converter);
        assertEquals(new BigInteger("12345678901234567890"), bi);
    }

    @Test
    public void testToString() {
        BigDecimal bd = new BigDecimal("123.45000");
        String s = BigDecimalConversions.toString(bd, converter);
        assertEquals("123.45", s);
    }

    @Test
    public void testToUUID() {
        // Zero BigDecimal should become the nil UUID (all‑zero)
        BigDecimal bd = BigDecimal.ZERO;
        UUID uuid = BigDecimalConversions.toUUID(bd, converter);
        assertEquals(new UUID(0L, 0L), uuid);
    }

    @Test
    public void testSecondsAndNanosToDouble() {
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(5L, 250_000_000L);
        assertEquals(new BigDecimal("5.250000000"), result);
    }

    /* --------------------------------------------------------------------- */
    /*  Edge‑case / exception tests */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToLocalTime_Zero() {
        BigDecimal seconds = BigDecimal.ZERO;
        LocalTime lt = BigDecimalConversions.toLocalTime(seconds, converter);
        assertEquals(LocalTime.MIDNIGHT, lt);
    }

    @Test
    public void testToLocalTime_MaximumValid() {
        // 23:59:59.999999999 = 86 399.999999999 seconds
        BigDecimal max = new BigDecimal("86399.999999999");
        LocalTime lt = BigDecimalConversions.toLocalTime(max, converter);
        assertEquals(LocalTime.MAX, lt);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTime_Negative() {
        BigDecimal negative = new BigDecimal("-0.1");
        BigDecimalConversions.toLocalTime(negative, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTime_TooLarge() {
        // One second more than the maximum valid value
        BigDecimal tooLarge = new BigDecimal("86400");
        BigDecimalConversions.toLocalTime(tooLarge, converter);
    }

    @Test
    public void testToCalendar_LargeValue() {
        // Use a value that exceeds Integer.MAX_VALUE milliseconds to ensure long handling
        BigDecimal seconds = new BigDecimal("2147483.648"); // 2,147,483,648 ms
        Calendar cal = BigDecimalConversions.toCalendar(seconds, converter);
        assertEquals(2_147_483_648L, cal.getTimeInMillis());
    }

    @Test
    public void testToInstant_WholeNumber() {
        BigDecimal seconds = new BigDecimal("42");
        Instant instant = BigDecimalConversions.toInstant(seconds, converter);
        assertEquals(42L, instant.getEpochSecond());
        assertEquals(0, instant.getNano());
    }

    @Test
    public void testToDuration_Zero() {
        BigDecimal zero = BigDecimal.ZERO;
        Duration dur = BigDecimalConversions.toDuration(zero, converter);
        assertEquals(Duration.ZERO, dur);
    }

    @Test
    public void testToString_TrailingZerosOnly() {
        BigDecimal bd = new BigDecimal("1000.0000");
        String s = BigDecimalConversions.toString(bd, converter);
        assertEquals("1000", s);
    }

    @Test
    public void testToBigInteger_RoundingTowardsZero() {
        // 123.999 should become 123 (BigDecimal#toBigInteger truncates)
        BigDecimal bd = new BigDecimal("123.999");
        BigInteger bi = BigDecimalConversions.toBigInteger(bd, converter);
        assertEquals(BigInteger.valueOf(123), bi);
    }
}
