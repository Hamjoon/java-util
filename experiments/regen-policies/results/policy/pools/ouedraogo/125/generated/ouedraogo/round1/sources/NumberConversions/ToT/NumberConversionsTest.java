/****************************************************************************************
 * NumberConversionsTest.java
 *
 * Comprehensive JUnit‑4 test suite for com.cedarsoftware.util.convert.NumberConversions.
 *
 * The test class resides in the same package as NumberConversions so that package‑private
 * static methods are directly accessible.
 *
 * The tests cover:
 *   • Normal conversions for all primitive/wrapper types.
 *   • Zero‑value shortcut methods.
 *   • String conversion helpers for float/double (including the special “0” case).
 *   • BigDecimal / BigInteger conversions for integer and floating‑point sources.
 *   • Atomic wrapper conversions.
 *   • Boolean conversion based on numeric zero / non‑zero.
 *   • Non‑zero predicate helpers.
 *   • Char conversion with in‑range and out‑of‑range values (exception case).
 *   • Date / Time conversions (Date, Duration, Instant, SqlDate, Timestamp, Calendar,
 *     LocalDate, LocalDateTime, ZonedDateTime, OffsetDateTime, Year).
 *
 * No mocking frameworks are used – all objects are instantiated via public constructors
 * or factory methods that appear in the source code or its public API.
 *
 * Author:  OpenAI ChatGPT
 * Date:    2026‑11‑14
 ****************************************************************************************/

package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Assert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;

/**
 * Test suite for NumberConversions.
 *
 * NOTE: The test class must be placed in the same package as NumberConversions
 *       because the conversion methods are package‑private.
 */
public class NumberConversionsTest {

    /** Helper to create a Converter with default options. */
    private static Converter defaultConverter() {
        // ConverterOptions is part of the public API; we assume a no‑arg constructor exists.
        return new Converter(new ConverterOptions());
    }

    /* -------------------------------------------------------------------------- */
    /* Primitive/wrapper conversions                                               */
    /* -------------------------------------------------------------------------- */

    @Test
    public void testToByte_andToByteZero() {
        Converter c = defaultConverter();

        // Normal conversion
        Byte source = (byte) 0x7F; // 127
        byte result = NumberConversions.toByte(source, c);
        Assert.assertEquals((byte) 127, result);

        // Zero shortcut (always returns the constant)
        Byte zero = NumberConversions.toByteZero(source, c);
        Assert.assertSame(CommonValues.BYTE_ZERO, zero);
    }

    @Test
    public void testToShort_andToShortZero() {
        Converter c = defaultConverter();

        Short source = (short) 32000;
        short result = NumberConversions.toShort(source, c);
        Assert.assertEquals((short) 32000, result);

        Short zero = NumberConversions.toShortZero(source, c);
        Assert.assertSame(CommonValues.SHORT_ZERO, zero);
    }

    @Test
    public void testToInt_andToIntZero() {
        Converter c = defaultConverter();

        Integer source = 123456789;
        int result = NumberConversions.toInt(source, c);
        Assert.assertEquals(123456789, result);

        Integer zero = NumberConversions.toIntZero(source, c);
        Assert.assertSame(CommonValues.INTEGER_ZERO, zero);
    }

    @Test
    public void testToLong_andToLongZero() {
        Converter c = defaultConverter();

        Long source = 9_223_372_036_854_775_807L; // Long.MAX_VALUE
        long result = NumberConversions.toLong(source, c);
        Assert.assertEquals(Long.MAX_VALUE, result);

        Long zero = NumberConversions.toLongZero(source, c);
        Assert.assertSame(CommonValues.LONG_ZERO, zero);
    }

    @Test
    public void testToFloat_andToFloatZero() {
        Converter c = defaultConverter();

        Float source = 3.14f;
        float result = NumberConversions.toFloat(source, c);
        Assert.assertEquals(3.14f, result, 0.00001f);

        Float zero = NumberConversions.toFloatZero(source, c);
        Assert.assertSame(CommonValues.FLOAT_ZERO, zero);
    }

    @Test
    public void testToDouble_andToDoubleZero() {
        Converter c = defaultConverter();

        Double source = -12345.6789;
        double result = NumberConversions.toDouble(source, c);
        Assert.assertEquals(-12345.6789, result, 0.0000001);

        Double zero = NumberConversions.toDoubleZero(source, c);
        Assert.assertSame(CommonValues.DOUBLE_ZERO, zero);
    }

    /* -------------------------------------------------------------------------- */
    /* Float / Double toString helpers                                            */
    /* -------------------------------------------------------------------------- */

    @Test
    public void testFloatToString() {
        Converter c = defaultConverter();

        // Zero case – must produce literal "0"
        Assert.assertEquals("0", NumberConversions.floatToString(0.0f, c));

        // Normal non‑zero value – uses Float.toString()
        Float f = 1.23f;
        Assert.assertEquals(f.toString(), NumberConversions.floatToString(f, c));
    }

    @Test
    public void testDoubleToString() {
        Converter c = defaultConverter();

        // Zero case
        Assert.assertEquals("0", NumberConversions.doubleToString(0.0d, c));

        // Normal non‑zero value
        Double d = -4.56;
        Assert.assertEquals(d.toString(), NumberConversions.doubleToString(d, c));
    }

    /* -------------------------------------------------------------------------- */
    /* BigDecimal / BigInteger conversions                                         */
    /* -------------------------------------------------------------------------- */

    @Test
    public void testIntegerTypeToBigDecimal_andBigInteger() {
        Converter c = defaultConverter();

        Long source = 123456789L;
        BigDecimal bd = NumberConversions.integerTypeToBigDecimal(source, c);
        Assert.assertEquals(BigDecimal.valueOf(123456789L), bd);

        BigInteger bi = NumberConversions.integerTypeToBigInteger(source, c);
        Assert.assertEquals(BigInteger.valueOf(123456789L), bi);
    }

    @Test
    public void testFloatingPointToBigDecimal() {
        Converter c = defaultConverter();

        Double source = 9876.54321;
        BigDecimal bd = NumberConversions.floatingPointToBigDecimal(source, c);
        Assert.assertEquals(BigDecimal.valueOf(9876.54321), bd);
    }

    @Test
    public void testFloatingPointToBigInteger_RoundingBehaviour() {
        Converter c = defaultConverter();

        // Positive value – should floor
        Double pos = 12.9;
        BigInteger biPos = NumberConversions.floatingPointToBigInteger(pos, c);
        Assert.assertEquals(BigInteger.valueOf(12L), biPos);

        // Negative value – should ceil
        Double neg = -7.3;
        BigInteger biNeg = NumberConversions.floatingPointToBigInteger(neg, c);
        Assert.assertEquals(BigInteger.valueOf(-7L), biNeg);

        // Exact integer value – no rounding change
        Double exact = 42.0;
        BigInteger biExact = NumberConversions.floatingPointToBigInteger(exact, c);
        Assert.assertEquals(BigInteger.valueOf(42L), biExact);
    }

    /* -------------------------------------------------------------------------- */
    /* Atomic wrapper conversions                                                  */
    /* -------------------------------------------------------------------------- */

    @Test
    public void testAtomicConversions() {
        Converter c = defaultConverter();

        Long longSrc = 555L;
        AtomicLong atomicLong = NumberConversions.toAtomicLong(longSrc, c);
        Assert.assertEquals(555L, atomicLong.get());

        Integer intSrc = 42;
        AtomicInteger atomicInt = NumberConversions.toAtomicInteger(intSrc, c);
        Assert.assertEquals(42, atomicInt.get());

        // AtomicBoolean: non‑zero → true, zero → false
        AtomicBoolean trueBool = NumberConversions.toAtomicBoolean(1L, c);
        Assert.assertTrue(trueBool.get());

        AtomicBoolean falseBool = NumberConversions.toAtomicBoolean(0L, c);
        Assert.assertFalse(falseBool.get());
    }

    /* -------------------------------------------------------------------------- */
    /* Non‑zero predicate helpers                                                  */
    /* -------------------------------------------------------------------------- */

    @Test
    public void testIsIntTypeNotZero() {
        Converter c = defaultConverter();

        Assert.assertTrue(NumberConversions.isIntTypeNotZero(123L, c));
        Assert.assertFalse(NumberConversions.isIntTypeNotZero(0L, c));
    }

    @Test
    public void testIsFloatTypeNotZero() {
        Converter c = defaultConverter();

        Assert.assertTrue(NumberConversions.isFloatTypeNotZero(0.0001d, c));
        Assert.assertFalse(NumberConversions.isFloatTypeNotZero(0.0d, c));
    }

    @Test
    public void testIsBigIntegerNotZero() {
        Assert.assertTrue(NumberConversions.isBigIntegerNotZero(BigInteger.valueOf(5), null));
        Assert.assertFalse(NumberConversions.isBigIntegerNotZero(BigInteger.ZERO, null));
    }

    @Test
    public void testIsBigDecimalNotZero() {
        Assert.assertTrue(NumberConversions.isBigDecimalNotZero(BigDecimal.valueOf(-0.1), null));
        Assert.assertFalse(NumberConversions.isBigDecimalNotZero(BigDecimal.ZERO, null));
    }

    /* -------------------------------------------------------------------------- */
    /* Character conversion (including out‑of‑range exception)                     */
    /* -------------------------------------------------------------------------- */

    @Test
    public void testToCharacter_InRange() {
        Converter c = defaultConverter();

        // Minimum value (0)
        char c0 = NumberConversions.toCharacter(0L, c);
        Assert.assertEquals('\u0000', c0);

        // Maximum char value
        char max = NumberConversions.toCharacter((long) Character.MAX_VALUE, c);
        Assert.assertEquals(Character.MAX_VALUE, max);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToCharacter_OutOfRange_Negative() {
        Converter c = defaultConverter();
        NumberConversions.toCharacter(-1L, c);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToCharacter_OutOfRange_TooLarge() {
        Converter c = defaultConverter();
        NumberConversions.toCharacter((long) Character.MAX_VALUE + 1, c);
    }

    /* -------------------------------------------------------------------------- */
    /* Date / Time conversions                                                     */
    /* -------------------------------------------------------------------------- */

    @Test
    public void testToDate() {
        Converter c = defaultConverter();

        long epoch = 1_600_000_000_000L; // 2020‑09‑13T12:26:40Z
        Date d = NumberConversions.toDate(epoch, c);
        Assert.assertEquals(epoch, d.getTime());
    }

    @Test
    public void testToDuration() {
        Converter c = defaultConverter();

        long millis = 12345L;
        Duration dur = NumberConversions.toDuration(millis, c);
        Assert.assertEquals(millis, dur.toMillis());
    }

    @Test
    public void testToInstant() {
        Converter c = defaultConverter();

        long epoch = 0L;
        Instant instant = NumberConversions.toInstant(epoch, c);
        Assert.assertEquals(Instant.EPOCH, instant);
    }

    @Test
    public void testToSqlDate() {
        Converter c = defaultConverter();

        long epoch = 1620000000000L;
        java.sql.Date sqlDate = NumberConversions.toSqlDate(epoch, c);
        Assert.assertEquals(epoch, sqlDate.getTime());
    }

    @Test
    public void testToTimestamp() {
        Converter c = defaultConverter();

        long epoch = 1620000000000L;
        Timestamp ts = NumberConversions.toTimestamp(epoch, c);
        Assert.assertEquals(epoch, ts.getTime());
    }

    @Test
    public void testToCalendar() {
        Converter c = defaultConverter();

        long epoch = 1620000000000L;
        Calendar cal = NumberConversions.toCalendar(epoch, c);
        Assert.assertEquals(epoch, cal.getTimeInMillis());
    }

    @Test
    public void testTemporalConversions_ZonedAndOffset() {
        Converter c = defaultConverter();

        // Use a known epoch that maps to a date/time with a non‑zero offset
        long epoch = 1_600_000_000_000L; // 2020‑09‑13T12:26:40Z
        ZonedDateTime zdt = NumberConversions.toZonedDateTime(epoch, c);
        Assert.assertEquals(Instant.ofEpochMilli(epoch), zdt.toInstant());

        OffsetDateTime odt = NumberConversions.toOffsetDateTime(epoch, c);
        Assert.assertEquals(zdt.toOffsetDateTime(), odt);
    }

    @Test
    public void testToLocalDate_andLocalDateTime() {
        Converter c = defaultConverter();

        // Choose a date that is easy to verify in UTC
        long epoch = 1_640_995_200_000L; // 2022‑01‑01T00:00:00Z
        LocalDate ld = NumberConversions.toLocalDate(epoch, c);
        LocalDateTime ldt = NumberConversions.toLocalDateTime(epoch, c);

        ZoneId zone = c.getOptions().getZoneId();
        ZonedDateTime zdt = Instant.ofEpochMilli(epoch).atZone(zone);

        Assert.assertEquals(zdt.toLocalDate(), ld);
        Assert.assertEquals(zdt.toLocalDateTime(), ldt);
    }

    @Test
    public void testToYear() {
        Converter c = defaultConverter();

        // Year is derived from a short value (e.g., 2023)
        Short yearShort = 2023;
        Year y = NumberConversions.toYear(yearShort, c);
        Assert.assertEquals(Year.of(2023), y);
    }
}
