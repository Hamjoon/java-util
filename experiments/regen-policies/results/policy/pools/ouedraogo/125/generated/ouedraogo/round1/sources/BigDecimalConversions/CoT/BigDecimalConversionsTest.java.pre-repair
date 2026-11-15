package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;
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
 * JUnit‑4 test suite for {@link BigDecimalConversions}.
 *
 * The tests are divided into three sections:
 *
 * 1. **Method list** – a comment that enumerates every public static method
 *    (the class itself is package‑private, but all conversion methods are
 *    static and therefore directly testable).
 *
 * 2. **Basic functional tests** – one straightforward test for each method
 *    that verifies the normal (happy‑path) behaviour.
 *
 * 3. **Edge‑case / exception tests** – tests that target special values,
 *    boundary conditions and the IllegalArgumentException thrown by
 *    {@code toLocalTime}.
 *
 * The {@code Converter} instance is created once per test class using the
 * default options (UTC time‑zone).  The options class is part of the library
 * and provides {@code getTimeZone()} and {@code getZoneId()} methods; the test
 * configures them explicitly to avoid locale‑dependent failures.
 */
public class BigDecimalConversionsTest {

    /* ----------------------------------------------------------------------
     * 1. List of public static conversion methods (signatures)
     * ---------------------------------------------------------------------- */
    // static Calendar            toCalendar(Object from, Converter converter)
    // static Instant             toInstant(Object from, Converter converter)
    // static Duration            toDuration(Object from, Converter converter)
    // static LocalTime           toLocalTime(Object from, Converter converter)
    // static LocalDate           toLocalDate(Object from, Converter converter)
    // static LocalDateTime       toLocalDateTime(Object from, Converter converter)
    // static OffsetDateTime      toOffsetDateTime(Object from, Converter converter)
    // static ZonedDateTime       toZonedDateTime(Object from, Converter converter)
    // static Date                toDate(Object from, Converter converter)
    // static java.sql.Date       toSqlDate(Object from, Converter converter)
    // static Timestamp           toTimestamp(Object from, Converter converter)
    // static BigInteger          toBigInteger(Object from, Converter converter)
    // static String              toString(Object from, Converter converter)
    // static UUID                toUUID(Object from, Converter converter)
    // static BigDecimal          secondsAndNanosToDouble(long seconds, long nanos)

    private Converter converter;

    @Before
    public void setUp() {
        // The library provides a ConverterOptions class with setters for
        // time‑zone and zone‑id.  We configure them to UTC to make the tests
        // deterministic.
        ConverterOptions options = new ConverterOptions();
        options.setTimeZone(TimeZone.getTimeZone("UTC"));
        options.setZoneId(ZoneId.of("UTC"));
        converter = new Converter(options);
    }

    /* ----------------------------------------------------------------------
     * 2. Basic functional tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testToCalendar() {
        BigDecimal seconds = new BigDecimal("12345.678"); // 12345.678 s
        Calendar cal = BigDecimalConversions.toCalendar(seconds, converter);
        long expectedMillis = seconds.multiply(BigDecimalConversions.GRAND).longValue();
        assertEquals(expectedMillis, cal.getTimeInMillis());
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
    }

    @Test
    public void testToInstant() {
        BigDecimal seconds = new BigDecimal("1609459200.123456789"); // 2021‑01‑01T00:00:00Z + 123456789 ns
        Instant instant = BigDecimalConversions.toInstant(seconds, converter);
        Instant expected = Instant.ofEpochSecond(1609459200L, 123456789L);
        assertEquals(expected, instant);
    }

    @Test
    public void testToDuration() {
        BigDecimal seconds = new BigDecimal("90.5"); // 90.5 s
        Duration duration = BigDecimalConversions.toDuration(seconds, converter);
        Duration expected = Duration.ofSeconds(90L, 500_000_000L);
        assertEquals(expected, duration);
    }

    @Test
    public void testToLocalTime() {
        // 12:34:56.789123456 = 45296.789123456 seconds since midnight
        BigDecimal seconds = new BigDecimal("45296.789123456");
        LocalTime time = BigDecimalConversions.toLocalTime(seconds, converter);
        LocalTime expected = LocalTime.of(12, 34, 56, 789_123_456);
        assertEquals(expected, time);
    }

    @Test
    public void testToLocalDate() {
        // 2022‑03‑15T00:00:00Z  -> epoch seconds = 1647302400
        BigDecimal seconds = new BigDecimal("1647302400");
        LocalDate date = BigDecimalConversions.toLocalDate(seconds, converter);
        LocalDate expected = LocalDate.of(2022, 3, 15);
        assertEquals(expected, date);
    }

    @Test
    public void testToLocalDateTime() {
        // 2022‑03‑15T12:30:45.123Z
        BigDecimal seconds = new BigDecimal("1647342645.123");
        LocalDateTime ldt = BigDecimalConversions.toLocalDateTime(seconds, converter);
        LocalDateTime expected = LocalDateTime.of(2022, 3, 15, 12, 30, 45, 123_000_000);
        assertEquals(expected, ldt);
    }

    @Test
    public void testToOffsetDateTime() {
        // 2022‑03‑15T12:30:45.123Z  -> offset zero (UTC)
        BigDecimal seconds = new BigDecimal("1647342645.123");
        OffsetDateTime odt = BigDecimalConversions.toOffsetDateTime(seconds, converter);
        OffsetDateTime expected = OffsetDateTime.ofInstant(Instant.ofEpochSecond(1647342645L, 123_000_000L), ZoneOffset.UTC);
        assertEquals(expected, odt);
    }

    @Test
    public void testToZonedDateTime() {
        // 2022‑03‑15T12:30:45.123Z  -> zone UTC
        BigDecimal seconds = new BigDecimal("1647342645.123");
        ZonedDateTime zdt = BigDecimalConversions.toZonedDateTime(seconds, converter);
        ZonedDateTime expected = ZonedDateTime.ofInstant(Instant.ofEpochSecond(1647342645L, 123_000_000L), ZoneId.of("UTC"));
        assertEquals(expected, zdt);
    }

    @Test
    public void testToDate() {
        BigDecimal seconds = new BigDecimal("1609459200"); // 2021‑01‑01T00:00:00Z
        Date date = BigDecimalConversions.toDate(seconds, converter);
        Date expected = Date.from(Instant.ofEpochSecond(1609459200L));
        assertEquals(expected, date);
    }

    @Test
    public void testToSqlDate() {
        BigDecimal seconds = new BigDecimal("1609459200"); // 2021‑01‑01
        java.sql.Date sqlDate = BigDecimalConversions.toSqlDate(seconds, converter);
        java.sql.Date expected = java.sql.Date.valueOf(LocalDate.of(2021, 1, 1));
        assertEquals(expected, sqlDate);
    }

    @Test
    public void testToTimestamp() {
        BigDecimal seconds = new BigDecimal("1609459200.5"); // 2021‑01‑01T00:00:00.5Z
        Timestamp ts = BigDecimalConversions.toTimestamp(seconds, converter);
        Timestamp expected = Timestamp.from(Instant.ofEpochSecond(1609459200L, 500_000_000L));
        assertEquals(expected, ts);
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
        // Use a known 128‑bit value: 0x00112233445566778899aabbccddeeff
        BigInteger bi = new BigInteger("00112233445566778899aabbccddeeff", 16);
        BigDecimal bd = new BigDecimal(bi);
        UUID uuid = BigDecimalConversions.toUUID(bd, converter);
        UUID expected = new UUID(
                bi.shiftRight(64).longValue(),
                bi.and(BigInteger.valueOf(Long.MAX_VALUE).shiftLeft(1).add(BigInteger.ONE)).longValue()
        );
        assertEquals(expected, uuid);
    }

    @Test
    public void testSecondsAndNanosToDouble() {
        long secs = 12345L;
        long nanos = 678_901_234L;
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(secs, nanos);
        BigDecimal expected = BigDecimal.valueOf(secs).add(BigDecimal.valueOf(nanos, 9));
        assertEquals(0, expected.compareTo(result));
    }

    /* ----------------------------------------------------------------------
     * 3. Edge‑case / exception tests
     * ---------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTime_NegativeValue() {
        BigDecimal negative = new BigDecimal("-0.000000001");
        BigDecimalConversions.toLocalTime(negative, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTime_TooLargeValue() {
        // 24 hours = 86400 seconds – 1 nanosecond is the exclusive upper bound
        BigDecimal tooBig = new BigDecimal("86400");
        BigDecimalConversions.toLocalTime(tooBig, converter);
    }

    @Test
    public void testToLocalTime_BoundaryZero() {
        BigDecimal zero = BigDecimal.ZERO;
        LocalTime time = BigDecimalConversions.toLocalTime(zero, converter);
        assertEquals(LocalTime.MIDNIGHT, time);
    }

    @Test
    public void testToLocalTime_BoundaryMaximum() {
        // 23:59:59.999999999 = 86399.999999999 seconds
        BigDecimal max = new BigDecimal("86399.999999999");
        LocalTime time = BigDecimalConversions.toLocalTime(max, converter);
        assertEquals(LocalTime.of(23, 59, 59, 999_999_999), time);
    }

    @Test
    public void testToInstant_LargeFractionalNanos() {
        // Fractional part with more than 9 digits should be truncated by BigDecimal
        BigDecimal seconds = new BigDecimal("10.1234567899"); // extra digit after nanosecond precision
        Instant instant = BigDecimalConversions.toInstant(seconds, converter);
        // Expected nanosecond part is rounded down (BigDecimal.longValue truncates)
        Instant expected = Instant.ofEpochSecond(10L, 123_456_789L);
        assertEquals(expected, instant);
    }

    @Test
    public void testToString_TrailingZerosAndScale() {
        BigDecimal bd = new BigDecimal("0.0001000");
        String s = BigDecimalConversions.toString(bd, converter);
        // stripTrailingZeros() removes the unnecessary zeros
        assertEquals("0.0001", s);
    }

    @Test
    public void testToBigInteger_NegativeValue() {
        BigDecimal bd = new BigDecimal("-42.999");
        BigInteger bi = BigDecimalConversions.toBigInteger(bd, converter);
        // toBigInteger() truncates toward zero
        assertEquals(BigInteger.valueOf(-42), bi);
    }

    @Test
    public void testSecondsAndNanosToDouble_ZeroNanos() {
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(0L, 0L);
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    public void testSecondsAndNanosToDouble_MaxNanos() {
        // nanos = 999,999,999 should be interpreted as 0.999999999 seconds
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(5L, 999_999_999L);
        BigDecimal expected = new BigDecimal("5.999999999");
        assertEquals(0, expected.compareTo(result));
    }
}
