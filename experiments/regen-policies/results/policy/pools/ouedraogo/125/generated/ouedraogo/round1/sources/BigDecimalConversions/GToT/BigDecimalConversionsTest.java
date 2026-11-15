package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@code BigDecimalConversions}.
 * <p>
 * The tests cover:
 * <ul>
 *   <li>basic happy‑path functionality of every public static conversion method</li>
 *   <li>edge‑case values (negative numbers, values outside the valid range, very large numbers)</li>
 *   <li>exception handling – especially the {@code IllegalArgumentException} thrown by {@code toLocalTime}</li>
 *   <li>correct handling of nanosecond precision and rounding</li>
 * </ul>
 *
 * The {@code Converter} instance is created with a default {@code ConverterOptions}
 * (the library supplies a no‑arg constructor for {@code ConverterOptions} that
 * yields the system default time‑zone and zone‑id).  No mocking frameworks are used;
 * all objects are created via their public constructors or factory methods.
 */
public class BigDecimalConversionsTest {

    /** Converter used for all tests – the only dependency of the static methods. */
    private Converter converter;

    @Before
    public void setUp() {
        // The library provides a public no‑arg constructor for ConverterOptions.
        // Using the default options ensures system default time‑zone/zone‑id are used.
        ConverterOptions options = new ConverterOptions();
        this.converter = new Converter(options);
    }

    /* ------------------------------------------------------------
     *  1.  toCalendar
     * ------------------------------------------------------------ */
    @Test
    public void testToCalendar_Basic() {
        // 1.5 seconds → 1500 milliseconds
        BigDecimal input = new BigDecimal("1.5");
        Calendar cal = BigDecimalConversions.toCalendar(input, converter);
        assertEquals(1500L, cal.getTimeInMillis());
    }

    @Test
    public void testToCalendar_Edge_Negative() {
        // Negative seconds should produce a negative epoch‑millis value
        BigDecimal input = new BigDecimal("-2.75");
        Calendar cal = BigDecimalConversions.toCalendar(input, converter);
        assertEquals(-2750L, cal.getTimeInMillis());
    }

    /* ------------------------------------------------------------
     *  2.  toInstant
     * ------------------------------------------------------------ */
    @Test
    public void testToInstant_Basic() {
        // 123.456789012 seconds → epoch seconds 123, nanos 456789012
        BigDecimal input = new BigDecimal("123.456789012");
        Instant instant = BigDecimalConversions.toInstant(input, converter);
        assertEquals(123L, instant.getEpochSecond());
        assertEquals(456_789_012L, instant.getNano());
    }

    @Test
    public void testToInstant_Edge_WholeNumber() {
        // Whole number should have zero nanos
        BigDecimal input = new BigDecimal("42");
        Instant instant = BigDecimalConversions.toInstant(input, converter);
        assertEquals(42L, instant.getEpochSecond());
        assertEquals(0L, instant.getNano());
    }

    @Test
    public void testToInstant_Edge_NegativeFraction() {
        // -0.000000001 seconds → epoch second 0, nanos = -1 (invalid for Instant)
        // The implementation uses longValue() for seconds which truncates toward zero,
        // and the remainder for nanos.  Instant.ofEpochSecond will throw for negative nanos,
        // so we verify that the method propagates the exception.
        BigDecimal input = new BigDecimal("-0.000000001");
        try {
            BigDecimalConversions.toInstant(input, converter);
            fail("Expected DateTimeException for negative nanos");
        } catch (Exception e) {
            // Expected – any RuntimeException is acceptable here.
        }
    }

    /* ------------------------------------------------------------
     *  3.  toDuration
     * ------------------------------------------------------------ */
    @Test
    public void testToDuration_Basic() {
        BigDecimal input = new BigDecimal("10.250");
        Duration duration = BigDecimalConversions.toDuration(input, converter);
        assertEquals(10L, duration.getSeconds());
        assertEquals(250_000_000L, duration.getNano());
    }

    @Test
    public void testToDuration_Edge_Negative() {
        BigDecimal input = new BigDecimal("-5.5");
        Duration duration = BigDecimalConversions.toDuration(input, converter);
        // Duration.ofSeconds allows negative seconds and nanos; nanos must be positive,
        // so the implementation will pass a negative nanos value to Duration.ofSeconds,
        // which throws an exception. Verify that.
        try {
            BigDecimalConversions.toDuration(input, converter);
            fail("Expected ArithmeticException for negative nanos");
        } catch (Exception e) {
            // Expected
        }
    }

    /* ------------------------------------------------------------
     *  4.  toLocalTime
     * ------------------------------------------------------------ */
    @Test
    public void testToLocalTime_Basic() {
        // 12:34:56.123456789 = 45296.123456789 seconds
        BigDecimal input = new BigDecimal("45296.123456789");
        LocalTime time = BigDecimalConversions.toLocalTime(input, converter);
        assertEquals(LocalTime.of(12, 34, 56, 123_456_789), time);
    }

    @Test
    public void testToLocalTime_Edge_Zero() {
        BigDecimal input = BigDecimal.ZERO;
        LocalTime time = BigDecimalConversions.toLocalTime(input, converter);
        assertEquals(LocalTime.MIDNIGHT, time);
    }

    @Test
    public void testToLocalTime_Edge_Maximum() {
        // 23:59:59.999999999 = 86_399.999999999 seconds
        BigDecimal input = new BigDecimal("86399.999999999");
        LocalTime time = BigDecimalConversions.toLocalTime(input, converter);
        assertEquals(LocalTime.MAX, time);
    }

    @Test
    public void testToLocalTime_Negative_Throws() {
        BigDecimal input = new BigDecimal("-1");
        try {
            BigDecimalConversions.toLocalTime(input, converter);
            fail("Expected IllegalArgumentException for negative value");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("must be >= 0 && <= 86399.999999999"));
        }
    }

    @Test
    public void testToLocalTime_TooLarge_Throws() {
        // One second beyond the maximum allowed
        BigDecimal input = new BigDecimal("86400");
        try {
            BigDecimalConversions.toLocalTime(input, converter);
            fail("Expected IllegalArgumentException for value > 86399.999999999");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("must be >= 0 && <= 86399.999999999"));
        }
    }

    /* ------------------------------------------------------------
     *  5.  toLocalDate (delegates to toZonedDateTime)
     * ------------------------------------------------------------ */
    @Test
    public void testToLocalDate_Basic() {
        // Epoch + 2 days = 172800 seconds
        BigDecimal input = new BigDecimal("172800");
        LocalDate date = BigDecimalConversions.toLocalDate(input, converter);
        // Expected date is 1970‑01‑03 (system default zone, but epoch is same)
        assertEquals(LocalDate.ofEpochDay(2), date);
    }

    /* ------------------------------------------------------------
     *  6.  toLocalDateTime
     * ------------------------------------------------------------ */
    @Test
    public void testToLocalDateTime_Basic() {
        // 2021‑03‑14T15:09:26.535897932 = epoch seconds 1615734566 + fraction
        // Compute the seconds from the known epoch.
        Instant known = Instant.parse("2021-03-14T15:09:26.535897932Z");
        long epochSec = known.getEpochSecond();
        long nano = known.getNano();
        BigDecimal input = BigDecimal.valueOf(epochSec).add(BigDecimal.valueOf(nano, 9));
        LocalDateTime ldt = BigDecimalConversions.toLocalDateTime(input, converter);
        assertEquals(LocalDateTime.ofInstant(known, converter.getOptions().getZoneId()), ldt);
    }

    /* ------------------------------------------------------------
     *  7.  toOffsetDateTime
     * ------------------------------------------------------------ */
    @Test
    public void testToOffsetDateTime_Basic() {
        Instant instant = Instant.parse("2022-01-01T00:00:00Z");
        BigDecimal input = BigDecimal.valueOf(instant.getEpochSecond());
        OffsetDateTime odt = BigDecimalConversions.toOffsetDateTime(input, converter);
        assertEquals(instant.atOffset(converter.getOptions().getZoneId().getRules().getOffset(instant)), odt);
    }

    /* ------------------------------------------------------------
     *  8.  toZonedDateTime
     * ------------------------------------------------------------ */
    @Test
    public void testToZonedDateTime_Basic() {
        Instant instant = Instant.parse("2000-01-01T12:00:00Z");
        BigDecimal input = new BigDecimal(instant.getEpochSecond());
        ZonedDateTime zdt = BigDecimalConversions.toZonedDateTime(input, converter);
        assertEquals(instant.atZone(converter.getOptions().getZoneId()), zdt);
    }

    /* ------------------------------------------------------------
     *  9.  toDate
     * ------------------------------------------------------------ */
    @Test
    public void testToDate_Basic() {
        Instant instant = Instant.parse("1999-12-31T23:59:59Z");
        BigDecimal input = new BigDecimal(instant.getEpochSecond());
        Date date = BigDecimalConversions.toDate(input, converter);
        assertEquals(Date.from(instant), date);
    }

    /* ------------------------------------------------------------
     * 10.  toSqlDate
     * ------------------------------------------------------------ */
    @Test
    public void testToSqlDate_Basic() {
        Instant instant = Instant.parse("2020-02-29T00:00:00Z");
        BigDecimal input = new BigDecimal(instant.getEpochSecond());
        java.sql.Date sqlDate = BigDecimalConversions.toSqlDate(input, converter);
        assertEquals(new java.sql.Date(instant.toEpochMilli()), sqlDate);
    }

    /* ------------------------------------------------------------
     * 11.  toTimestamp
     * ------------------------------------------------------------ */
    @Test
    public void testToTimestamp_Basic() {
        Instant instant = Instant.parse("2021-12-31T23:59:59.123456789Z");
        BigDecimal input = new BigDecimal(instant.getEpochSecond())
                .add(BigDecimal.valueOf(instant.getNano(), 9));
        Timestamp ts = BigDecimalConversions.toTimestamp(input, converter);
        assertEquals(Timestamp.from(instant), ts);
    }

    /* ------------------------------------------------------------
     * 12.  toBigInteger
     * ------------------------------------------------------------ */
    @Test
    public void testToBigInteger_Basic() {
        BigDecimal input = new BigDecimal("12345678901234567890");
        BigInteger bi = BigDecimalConversions.toBigInteger(input, converter);
        assertEquals(new BigInteger("12345678901234567890"), bi);
    }

    @Test
    public void testToBigInteger_WithFraction() {
        // Fractional part is discarded by toBigInteger()
        BigDecimal input = new BigDecimal("9876.54321");
        BigInteger bi = BigDecimalConversions.toBigInteger(input, converter);
        assertEquals(BigInteger.valueOf(9876L), bi);
    }

    /* ------------------------------------------------------------
     * 13.  toString
     * ------------------------------------------------------------ */
    @Test
    public void testToString_StripsTrailingZeros() {
        BigDecimal input = new BigDecimal("123.45000");
        String s = BigDecimalConversions.toString(input, converter);
        assertEquals("123.45", s);
    }

    @Test
    public void testToString_WholeNumber() {
        BigDecimal input = new BigDecimal("1000");
        String s = BigDecimalConversions.toString(input, converter);
        assertEquals("1000", s);
    }

    /* ------------------------------------------------------------
     * 14.  toUUID
     * ------------------------------------------------------------ */
    @Test
    public void testToUUID_Basic() {
        // Use a known UUID, convert to BigInteger, then back.
        UUID original = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
        BigInteger bigInt = new BigInteger(1, asBytes(original));
        BigDecimal input = new BigDecimal(bigInt);
        UUID result = BigDecimalConversions.toUUID(input, converter);
        assertEquals(original, result);
    }

    private static byte[] asBytes(UUID uuid) {
        // Convert UUID to a 16‑byte array (big‑endian)
        byte[] bytes = new byte[16];
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        for (int i = 0; i < 8; i++) {
            bytes[i] = (byte) (msb >>> (8 * (7 - i)));
        }
        for (int i = 8; i < 16; i++) {
            bytes[i] = (byte) (lsb >>> (8 * (7 - (i - 8))));
        }
        return bytes;
    }

    /* ------------------------------------------------------------
     * 15.  secondsAndNanosToDouble
     * ------------------------------------------------------------ */
    @Test
    public void testSecondsAndNanosToDouble_Basic() {
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(10L, 250_000_000L);
        assertEquals(new BigDecimal("10.25"), result);
    }

    @Test
    public void testSecondsAndNanosToDouble_NanosZero() {
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(5L, 0L);
        assertEquals(new BigDecimal("5"), result);
    }

    @Test
    public void testSecondsAndNanosToDouble_MaxNanos() {
        // 1 second + 999,999,999 nanos → 1.999999999
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(1L, 999_999_999L);
        assertEquals(new BigDecimal("1.999999999"), result);
    }
}
