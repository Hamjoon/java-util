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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * JUnit‑4 test suite for {@link NumberConversions}.
 *
 * The tests are organised in three logical sections that mirror the
 * development steps described in the task:
 *
 * 1. **Public method extraction** – a comment block lists every static method
 *    that is visible to the test class (package‑private static methods are
 *    accessible because the test resides in the same package).
 *
 * 2. **Basic functionality tests** – one straightforward test per method that
 *    verifies the normal, expected behaviour.
 *
 * 3. **Edge‑case & exception tests** – additional tests that target boundary
 *    values, rounding rules, zero‑handling and the IllegalArgumentException
 *    thrown by {@code toCharacter}.
 *
 * The test class uses a real {@link Converter} instance created with a default
 * {@link ConverterOptions}. No mocking or subclassing of library classes is
 * performed, in accordance with the constraints.
 */
public class NumberConversionsTest
{
    /* -----------------------------------------------------------------------
     * 1. List of public (package‑private static) methods in NumberConversions
     * -----------------------------------------------------------------------
     *
     *   static byte   toByte(Object, Converter)
     *   static Byte   toByteZero(Object, Converter)
     *   static short  toShort(Object, Converter)
     *   static Short  toShortZero(Object, Converter)
     *   static int    toInt(Object, Converter)
     *   static Integer toIntZero(Object, Converter)
     *   static long   toLong(Object, Converter)
     *   static Long   toLongZero(Object, Converter)
     *   static float  toFloat(Object, Converter)
     *   static Float  toFloatZero(Object, Converter)
     *   static String floatToString(Object, Converter)
     *   static double toDouble(Object, Converter)
     *   static Double toDoubleZero(Object, Converter)
     *   static String doubleToString(Object, Converter)
     *   static BigDecimal integerTypeToBigDecimal(Object, Converter)
     *   static BigInteger integerTypeToBigInteger(Object, Converter)
     *   static AtomicLong   toAtomicLong(Object, Converter)
     *   static AtomicInteger toAtomicInteger(Object, Converter)
     *   static AtomicBoolean toAtomicBoolean(Object, Converter)
     *   static BigDecimal floatingPointToBigDecimal(Object, Converter)
     *   static BigInteger floatingPointToBigInteger(Object, Converter)
     *   static boolean isIntTypeNotZero(Object, Converter)
     *   static boolean isFloatTypeNotZero(Object, Converter)
     *   static boolean isBigIntegerNotZero(Object, Converter)
     *   static boolean isBigDecimalNotZero(Object, Converter)
     *   static char   toCharacter(Object, Converter)               // may throw IllegalArgumentException
     *   static Date   toDate(Object, Converter)
     *   static Duration toDuration(Object, Converter)
     *   static Instant   toInstant(Object, Converter)
     *   static java.sql.Date toSqlDate(Object, Converter)
     *   static Timestamp   toTimestamp(Object, Converter)
     *   static Calendar    toCalendar(Object, Converter)
     *   static LocalDate   toLocalDate(Object, Converter)
     *   static LocalDateTime toLocalDateTime(Object, Converter)
     *   static ZonedDateTime toZonedDateTime(Object, Converter)
     *   static OffsetDateTime toOffsetDateTime(Object, Converter)
     *   static Year   toYear(Object, Converter)
     */

    private Converter converter;

    @Before
    public void setUp()
    {
        // ConverterOptions has a public no‑arg constructor in the library.
        converter = new Converter(new ConverterOptions());
    }

    /* -----------------------------------------------------------------------
     * 2. Basic functionality tests
     * -----------------------------------------------------------------------
     */

    @Test
    public void testToByte()
    {
        Byte src = (byte) 42;
        assertEquals((byte) 42, NumberConversions.toByte(src, converter));
    }

    @Test
    public void testToByteZero()
    {
        // The method ignores the input and always returns the constant zero.
        assertEquals(CommonValues.BYTE_ZERO, NumberConversions.toByteZero("any", converter));
    }

    @Test
    public void testToShort()
    {
        Short src = (short) 12345;
        assertEquals((short) 12345, NumberConversions.toShort(src, converter));
    }

    @Test
    public void testToShortZero()
    {
        assertEquals(CommonValues.SHORT_ZERO, NumberConversions.toShortZero(null, converter));
    }

    @Test
    public void testToInt()
    {
        Integer src = 123456789;
        assertEquals(123456789, NumberConversions.toInt(src, converter));
    }

    @Test
    public void testToIntZero()
    {
        assertEquals(CommonValues.INTEGER_ZERO, NumberConversions.toIntZero("ignored", converter));
    }

    @Test
    public void testToLong()
    {
        Long src = 9_223_372_036_854_775_807L; // Long.MAX_VALUE
        assertEquals(Long.MAX_VALUE, NumberConversions.toLong(src, converter));
    }

    @Test
    public void testToLongZero()
    {
        assertEquals(CommonValues.LONG_ZERO, NumberConversions.toLongZero(null, converter));
    }

    @Test
    public void testToFloat()
    {
        Float src = 3.14f;
        assertEquals(3.14f, NumberConversions.toFloat(src, converter), 0.000001f);
    }

    @Test
    public void testToFloatZero()
    {
        assertEquals(CommonValues.FLOAT_ZERO, NumberConversions.toFloatZero("x", converter));
    }

    @Test
    public void testFloatToString()
    {
        Float src = 5.0f;
        assertEquals("5.0", NumberConversions.floatToString(src, converter));

        Float zero = 0.0f;
        assertEquals("0", NumberConversions.floatToString(zero, converter));
    }

    @Test
    public void testToDouble()
    {
        Double src = 2.718281828459045;
        assertEquals(2.718281828459045, NumberConversions.toDouble(src, converter), 1e-15);
    }

    @Test
    public void testToDoubleZero()
    {
        assertEquals(CommonValues.DOUBLE_ZERO, NumberConversions.toDoubleZero(null, converter));
    }

    @Test
    public void testDoubleToString()
    {
        Double src = 6.022e23;
        assertEquals("6.022E23", NumberConversions.doubleToString(src, converter));

        Double zero = 0.0;
        assertEquals("0", NumberConversions.doubleToString(zero, converter));
    }

    @Test
    public void testIntegerTypeToBigDecimal()
    {
        Long src = 123L;
        BigDecimal expected = BigDecimal.valueOf(123L);
        assertEquals(expected, NumberConversions.integerTypeToBigDecimal(src, converter));
    }

    @Test
    public void testIntegerTypeToBigInteger()
    {
        Integer src = 456;
        BigInteger expected = BigInteger.valueOf(456L);
        assertEquals(expected, NumberConversions.integerTypeToBigInteger(src, converter));
    }

    @Test
    public void testToAtomicLong()
    {
        Long src = 999L;
        AtomicLong al = NumberConversions.toAtomicLong(src, converter);
        assertEquals(999L, al.get());
    }

    @Test
    public void testToAtomicInteger()
    {
        Integer src = 42;
        AtomicInteger ai = NumberConversions.toAtomicInteger(src, converter);
        assertEquals(42, ai.get());
    }

    @Test
    public void testToAtomicBoolean()
    {
        Long trueSrc = 1L;
        Long falseSrc = 0L;
        assertTrue(NumberConversions.toAtomicBoolean(trueSrc, converter).get());
        assertFalse(NumberConversions.toAtomicBoolean(falseSrc, converter).get());
    }

    @Test
    public void testFloatingPointToBigDecimal()
    {
        Double src = 123.456;
        BigDecimal expected = BigDecimal.valueOf(123.456);
        assertEquals(expected, NumberConversions.floatingPointToBigDecimal(src, converter));
    }

    @Test
    public void testFloatingPointToBigInteger()
    {
        Double src = 123.999; // should truncate toward zero (floor for positive)
        BigInteger expected = BigInteger.valueOf(123L);
        assertEquals(expected, NumberConversions.floatingPointToBigInteger(src, converter));
    }

    @Test
    public void testIsIntTypeNotZero()
    {
        Long zero = 0L;
        Long nonZero = 7L;
        assertFalse(NumberConversions.isIntTypeNotZero(zero, converter));
        assertTrue(NumberConversions.isIntTypeNotZero(nonZero, converter));
    }

    @Test
    public void testIsFloatTypeNotZero()
    {
        Double zero = 0.0;
        Double nonZero = -0.0001;
        assertFalse(NumberConversions.isFloatTypeNotZero(zero, converter));
        assertTrue(NumberConversions.isFloatTypeNotZero(nonZero, converter));
    }

    @Test
    public void testIsBigIntegerNotZero()
    {
        assertFalse(NumberConversions.isBigIntegerNotZero(BigInteger.ZERO, converter));
        assertTrue(NumberConversions.isBigIntegerNotZero(BigInteger.ONE, converter));
    }

    @Test
    public void testIsBigDecimalNotZero()
    {
        assertFalse(NumberConversions.isBigDecimalNotZero(BigDecimal.ZERO, converter));
        assertTrue(NumberConversions.isBigDecimalNotZero(new BigDecimal("0.0001"), converter));
    }

    @Test
    public void testToCharacterWithinRange()
    {
        Integer src = 65; // 'A'
        assertEquals('A', NumberConversions.toCharacter(src, converter));
    }

    @Test
    public void testToDate()
    {
        Long epoch = 1_600_000_000_000L; // some arbitrary epoch ms
        Date expected = new Date(epoch);
        assertEquals(expected, NumberConversions.toDate(epoch, converter));
    }

    @Test
    public void testToDuration()
    {
        Long millis = 5_000L;
        Duration expected = Duration.ofMillis(millis);
        assertEquals(expected, NumberConversions.toDuration(millis, converter));
    }

    @Test
    public void testToInstant()
    {
        Long epoch = 1_234_567_890L;
        Instant expected = Instant.ofEpochMilli(epoch);
        assertEquals(expected, NumberConversions.toInstant(epoch, converter));
    }

    @Test
    public void testToSqlDate()
    {
        Long epoch = 0L;
        java.sql.Date expected = new java.sql.Date(epoch);
        assertEquals(expected, NumberConversions.toSqlDate(epoch, converter));
    }

    @Test
    public void testToTimestamp()
    {
        Long epoch = 12345L;
        Timestamp expected = new Timestamp(epoch);
        assertEquals(expected, NumberConversions.toTimestamp(epoch, converter));
    }

    @Test
    public void testToCalendar()
    {
        Long epoch = 0L;
        Calendar cal = NumberConversions.toCalendar(epoch, converter);
        assertEquals(epoch, cal.getTimeInMillis());
    }

    @Test
    public void testToLocalDate()
    {
        // Use epoch that falls on 1970‑01‑01 UTC
        Long epoch = 0L;
        LocalDate expected = LocalDate.ofInstant(Instant.ofEpochMilli(epoch), converter.getOptions().getZoneId());
        assertEquals(expected, NumberConversions.toLocalDate(epoch, converter));
    }

    @Test
    public void testToLocalDateTime()
    {
        Long epoch = 1_600_000_000_000L;
        LocalDateTime expected = LocalDateTime.ofInstant(Instant.ofEpochMilli(epoch), converter.getOptions().getZoneId());
        assertEquals(expected, NumberConversions.toLocalDateTime(epoch, converter));
    }

    @Test
    public void testToZonedDateTime()
    {
        Long epoch = 1_600_000_000_000L;
        ZonedDateTime expected = Instant.ofEpochMilli(epoch).atZone(converter.getOptions().getZoneId());
        assertEquals(expected, NumberConversions.toZonedDateTime(epoch, converter));
    }

    @Test
    public void testToOffsetDateTime()
    {
        Long epoch = 1_600_000_000_000L;
        OffsetDateTime expected = NumberConversions.toZonedDateTime(epoch, converter).toOffsetDateTime();
        assertEquals(expected, NumberConversions.toOffsetDateTime(epoch, converter));
    }

    @Test
    public void testToYear()
    {
        Short src = 2023;
        Year expected = Year.of(2023);
        assertEquals(expected, NumberConversions.toYear(src, converter));
    }

    /* -----------------------------------------------------------------------
     * 3. Edge‑case and exception tests
     * -----------------------------------------------------------------------
     */

    @Test(expected = IllegalArgumentException.class)
    public void testToCharacterOutOfRangeNegative()
    {
        Number src = -1;
        NumberConversions.toCharacter(src, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToCharacterOutOfRangeTooLarge()
    {
        // Character.MAX_VALUE == 65535
        Number src = 70_000;
        NumberConversions.toCharacter(src, converter);
    }

    @Test
    public void testFloatToStringZeroEdge()
    {
        Float zero = -0.0f; // negative zero should still be treated as zero
        assertEquals("0", NumberConversions.floatToString(zero, converter));
    }

    @Test
    public void testDoubleToStringZeroEdge()
    {
        Double zero = -0.0; // negative zero handling
        assertEquals("0", NumberConversions.doubleToString(zero, converter));
    }

    @Test
    public void testFloatingPointToBigIntegerNegative()
    {
        Double src = -123.456; // should round toward zero (ceil for negative)
        BigInteger expected = BigInteger.valueOf(-123L);
        assertEquals(expected, NumberConversions.floatingPointToBigInteger(src, converter));
    }

    @Test
    public void testIsIntTypeNotZeroWithLongMin()
    {
        Long min = Long.MIN_VALUE; // non‑zero negative value
        assertTrue(NumberConversions.isIntTypeNotZero(min, converter));
    }

    @Test
    public void testIsFloatTypeNotZeroWithNaN()
    {
        Double nan = Double.NaN;
        // NaN is not equal to zero, so the method should return true
        assertTrue(NumberConversions.isFloatTypeNotZero(nan, converter));
    }

    @Test
    public void testToAtomicBooleanWithNegativeLong()
    {
        Long src = -5L;
        // any non‑zero value, positive or negative, yields true
        assertTrue(NumberConversions.toAtomicBoolean(src, converter).get());
    }

    @Test
    public void testToCalendarEpochBoundary()
    {
        // Test the exact moment of a leap second (which Java treats as the next second)
        Long leapSecond = 1_577_836_800_000L; // 2020‑01‑01T00:00:00Z (UTC)
        Calendar cal = NumberConversions.toCalendar(leapSecond, converter);
        assertEquals(leapSecond, cal.getTimeInMillis());
    }

    @Test
    public void testToYearWithShortOverflow()
    {
        // short max = 32767, which is a valid year in Java's Year class.
        Short src = Short.MAX_VALUE;
        Year expected = Year.of(32767);
        assertEquals(expected, NumberConversions.toYear(src, converter));
    }
}
