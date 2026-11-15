package com.cedarsoftware.util.convert;

import org.junit.*;
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
 * Comprehensive JUnit‑4 test suite for {@link BigDecimalConversions}.
 *
 * The test class lives in the same package as {@code BigDecimalConversions}
 * so it can access the package‑private static conversion methods.
 *
 * The tests cover:
 *   1. Normal “happy‑path” usage of every conversion method.
 *   2. Edge‑case values (zero, maximum allowed values, negative numbers, etc.).
 *   3. Expected exception handling for illegal inputs.
 *
 * All tests use a {@link Converter} constructed with a fresh {@link ConverterOptions}
 * instance.  The JVM default time‑zone is forced to UTC in {@code @BeforeClass}
 * so that date‑time results are deterministic irrespective of the machine on which
 * the tests run.
 */
public class BigDecimalConversionsTest
{
    /** Force UTC for deterministic date‑time calculations. */
    @BeforeClass
    public static void forceUtc()
    {
        TimeZone utc = TimeZone.getTimeZone("UTC");
        TimeZone.setDefault(utc);
    }

    private Converter converter;

    /** Create a fresh {@link Converter} for each test method. */
    @Before
    public void createConverter()
    {
        // ConverterOptions has a public no‑arg constructor (as assumed from the library).
        // The default options use the system default TimeZone/ZoneId – which we have set to UTC.
        converter = new Converter(new ConverterOptions());
    }

    /* --------------------------------------------------------------------- */
    /*  Public method list (extracted from the source)                       */
    /* --------------------------------------------------------------------- */
    // static Calendar                toCalendar(Object, Converter)
    // static Instant                 toInstant(Object, Converter)
    // static Duration                toDuration(Object, Converter)
    // static LocalTime               toLocalTime(Object, Converter)
    // static LocalDate               toLocalDate(Object, Converter)
    // static LocalDateTime           toLocalDateTime(Object, Converter)
    // static OffsetTime              toOffsetTime(Object, Converter)
    // static OffsetDateTime          toOffsetDateTime(Object, Converter)
    // static ZonedDateTime           toZonedDateTime(Object, Converter)
    // static Date                    toDate(Object, Converter)
    // static java.sql.Date           toSqlDate(Object, Converter)
    // static Timestamp               toTimestamp(Object, Converter)
    // static BigInteger              toBigInteger(Object, Converter)
    // static String                  toString(Object, Converter)
    // static UUID                    toUUID(Object, Converter)
    // static BigDecimal              secondsAndNanosToDouble(long, long)

    /* --------------------------------------------------------------------- */
    /*  1. Basic functionality tests (happy‑path)                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToCalendar()
    {
        BigDecimal seconds = new BigDecimal("1.5"); // 1.5 s = 1500 ms
        Calendar cal = BigDecimalConversions.toCalendar(seconds, converter);
        assertEquals(1500L, cal.getTimeInMillis());
    }

    @Test
    public void testToInstant()
    {
        // 123.456789012 seconds → 123 s + 456 789 012 ns
        BigDecimal seconds = new BigDecimal("123.456789012");
        Instant instant = BigDecimalConversions.toInstant(seconds, converter);
        assertEquals(123L, instant.getEpochSecond());
        assertEquals(456_789_012L, instant.getNano());
    }

    @Test
    public void testToDuration()
    {
        BigDecimal seconds = new BigDecimal("10.000000001");
        Duration duration = BigDecimalConversions.toDuration(seconds, converter);
        assertEquals(10L, duration.getSeconds());
        assertEquals(1L, duration.getNano());
    }

    @Test
    public void testToLocalTime()
    {
        // 13 h 30 m 15.123456789 s = 48615.123456789 s since midnight
        BigDecimal seconds = new BigDecimal("48615.123456789");
        LocalTime lt = BigDecimalConversions.toLocalTime(seconds, converter);
        assertEquals(LocalTime.of(13, 30, 15, 123_456_789), lt);
    }

    @Test
    public void testToLocalDate()
    {
        // Epoch second 0 → 1970‑01‑01 (UTC)
        BigDecimal seconds = BigDecimal.ZERO;
        LocalDate ld = BigDecimalConversions.toLocalDate(seconds, converter);
        assertEquals(LocalDate.of(1970, 1, 1), ld);
    }

    @Test
    public void testToLocalDateTime()
    {
        // 2020‑02‑29T12:34:56.789Z → epoch seconds 1582980896.789
        BigDecimal seconds = new BigDecimal("1582980896.789");
        LocalDateTime ldt = BigDecimalConversions.toLocalDateTime(seconds, converter);
        assertEquals(LocalDateTime.of(2020, 2, 29, 12, 34, 56, 789_000_000), ldt);
    }

    @Test
    public void testToOffsetTime()
    {
        // 08:15:30.250 (UTC offset 0)
        BigDecimal seconds = new BigDecimal("29730.25"); // 8h*3600 + 15*60 + 30 + .25
        OffsetTime ot = BigDecimalConversions.toOffsetTime(seconds, converter);
        assertEquals(OffsetTime.of(8, 15, 30, 250_000_000, ZoneOffset.UTC), ot);
    }

    @Test
    public void testToOffsetDateTime()
    {
        // 2021‑03‑14T01:59:59.999999999Z
        BigDecimal seconds = new BigDecimal("1615689599.999999999");
        OffsetDateTime odt = BigDecimalConversions.toOffsetDateTime(seconds, converter);
        assertEquals(OffsetDateTime.ofInstant(Instant.ofEpochSecond(1615689599, 999_999_999), ZoneOffset.UTC), odt);
    }

    @Test
    public void testToZonedDateTime()
    {
        BigDecimal seconds = new BigDecimal("0");
        ZonedDateTime zdt = BigDecimalConversions.toZonedDateTime(seconds, converter);
        assertEquals(ZonedDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC), zdt);
    }

    @Test
    public void testToDate()
    {
        BigDecimal seconds = new BigDecimal("0");
        Date date = BigDecimalConversions.toDate(seconds, converter);
        assertEquals(Date.from(Instant.EPOCH), date);
    }

    @Test
    public void testToSqlDate()
    {
        // 2022‑01‑01T00:00:00Z → epoch seconds 1640995200
        BigDecimal seconds = new BigDecimal("1640995200");
        java.sql.Date sqlDate = BigDecimalConversions.toSqlDate(seconds, converter);
        assertEquals(java.sql.Date.valueOf(LocalDate.of(2022, 1, 1)), sqlDate);
    }

    @Test
    public void testToTimestamp()
    {
        BigDecimal seconds = new BigDecimal("1.001");
        Timestamp ts = BigDecimalConversions.toTimestamp(seconds, converter);
        assertEquals(1001L, ts.getTime()); // milliseconds
    }

    @Test
    public void testToBigInteger()
    {
        BigDecimal bd = new BigDecimal("12345678901234567890");
        BigInteger bi = BigDecimalConversions.toBigInteger(bd, converter);
        assertEquals(new BigInteger("12345678901234567890"), bi);
    }

    @Test
    public void testToString()
    {
        // stripTrailingZeros should remove the trailing zeros after the decimal point
        BigDecimal bd = new BigDecimal("123.45000");
        String s = BigDecimalConversions.toString(bd, converter);
        assertEquals("123.45", s);
    }

    @Test
    public void testToUUID()
    {
        // Use a known UUID → its most‑significant and least‑significant bits combined into a 128‑bit integer.
        UUID expected = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
        BigInteger bigInt = new BigInteger(expected.toString().replace("-", ""), 16);
        BigDecimal bd = new BigDecimal(bigInt);
        // The conversion delegates to BigIntegerConversions.toUUID; we assume that method works correctly.
        UUID actual = BigDecimalConversions.toUUID(bd, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testSecondsAndNanosToDouble()
    {
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(5L, 250_000_000L);
        assertEquals(new BigDecimal("5.25"), result);
    }

    /* --------------------------------------------------------------------- */
    /*  2. Edge‑case and exception handling tests                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToLocalTime_Zero()
    {
        BigDecimal seconds = BigDecimal.ZERO;
        LocalTime lt = BigDecimalConversions.toLocalTime(seconds, converter);
        assertEquals(LocalTime.MIDNIGHT, lt);
    }

    @Test
    public void testToLocalTime_Maximum()
    {
        // 86 399.999 999 999 seconds = one nanosecond before next midnight
        BigDecimal max = new BigDecimal("86399.999999999");
        LocalTime lt = BigDecimalConversions.toLocalTime(max, converter);
        assertEquals(LocalTime.of(23, 59, 59, 999_999_999), lt);
    }

    @Test
    public void testToLocalTime_Negative_Throws()
    {
        BigDecimal neg = new BigDecimal("-1");
        try
        {
            BigDecimalConversions.toLocalTime(neg, converter);
            fail("Expected IllegalArgumentException for negative input");
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().contains("must be >= 0 && <= 86399.999999999"));
        }
    }

    @Test
    public void testToLocalTime_TooLarge_Throws()
    {
        BigDecimal tooBig = new BigDecimal("86400"); // exactly one day
        try
        {
            BigDecimalConversions.toLocalTime(tooBig, converter);
            fail("Expected IllegalArgumentException for value > 23:59:59.999999999");
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().contains("must be >= 0 && <= 86399.999999999"));
        }
    }

    @Test
    public void testToOffsetTime_Negative_Throws()
    {
        BigDecimal neg = new BigDecimal("-0.1");
        try
        {
            BigDecimalConversions.toOffsetTime(neg, converter);
            fail("Expected IllegalArgumentException for negative offset time");
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().contains("must be >= 0 && <= 86399.999999999"));
        }
    }

    @Test
    public void testToOffsetTime_TooLarge_Throws()
    {
        BigDecimal tooBig = new BigDecimal("86400.000000001");
        try
        {
            BigDecimalConversions.toOffsetTime(tooBig, converter);
            fail("Expected IllegalArgumentException for offset time > 23:59:59.999999999");
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().contains("must be >= 0 && <= 86399.999999999"));
        }
    }

    @Test
    public void testSecondsAndNanosToDouble_NegativeValues()
    {
        // The method does not guard against negative values – it simply adds them.
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(-3L, -500_000_000L);
        assertEquals(new BigDecimal("-3.5"), result);
    }
}
