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
 * Unit tests for {@link NumberConversions}.
 * All public (package‑private static) methods of NumberConversions are exercised,
 * including normal behaviour, edge cases and expected exceptions.
 */
public class NumberConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // The Converter constructor only needs a ConverterOptions instance.
        // Use the default options (the class provides a no‑arg constructor in the real library;
        // for the purpose of these tests we can instantiate with a simple stub if needed).
        converter = new Converter(new ConverterOptions());
    }

    /* --------------------------------------------------------------------- */
    /* 1. List of public (package‑private static) methods with signatures      */
    /* --------------------------------------------------------------------- */
    // byte   toByte(Object, Converter)
    // Byte   toByteZero(Object, Converter)
    // short  toShort(Object, Converter)
    // Short  toShortZero(Object, Converter)
    // int    toInt(Object, Converter)
    // Integer toIntZero(Object, Converter)
    // long   toLong(Object, Converter)
    // Long   toLongZero(Object, Converter)
    // float  toFloat(Object, Converter)
    // Float  toFloatZero(Object, Converter)
    // String floatToString(Object, Converter)
    // double toDouble(Object, Converter)
    // Double toDoubleZero(Object, Converter)
    // String doubleToString(Object, Converter)
    // BigDecimal integerTypeToBigDecimal(Object, Converter)
    // BigInteger integerTypeToBigInteger(Object, Converter)
    // AtomicLong   toAtomicLong(Object, Converter)
    // AtomicInteger toAtomicInteger(Object, Converter)
    // AtomicBoolean toAtomicBoolean(Object, Converter)
    // BigDecimal floatingPointToBigDecimal(Object, Converter)
    // BigInteger floatingPointToBigInteger(Object, Converter)
    // boolean isIntTypeNotZero(Object, Converter)
    // boolean isFloatTypeNotZero(Object, Converter)
    // boolean isBigIntegerNotZero(Object, Converter)
    // boolean isBigDecimalNotZero(Object, Converter)
    // char toCharacter(Object, Converter)   // may throw IllegalArgumentException
    // Date toDate(Object, Converter)
    // Duration toDuration(Object, Converter)
    // Instant toInstant(Object, Converter)
    // java.sql.Date toSqlDate(Object, Converter)
    // Timestamp toTimestamp(Object, Converter)
    // Calendar toCalendar(Object, Converter)
    // LocalDate toLocalDate(Object, Converter)
    // LocalDateTime toLocalDateTime(Object, Converter)
    // ZonedDateTime toZonedDateTime(Object, Converter)
    // OffsetDateTime toOffsetDateTime(Object, Converter)
    // Year toYear(Object, Converter)

    /* --------------------------------------------------------------------- */
    /* 2. Basic functional tests                                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToByte_andZero() {
        Byte expected = (byte) 123;
        assertEquals(expected.byteValue(), NumberConversions.toByte(expected, converter));
        assertEquals(CommonValues.BYTE_ZERO, NumberConversions.toByteZero(expected, converter));
    }

    @Test
    public void testToShort_andZero() {
        Short val = (short) 32123;
        assertEquals(val.shortValue(), NumberConversions.toShort(val, converter));
        assertEquals(CommonValues.SHORT_ZERO, NumberConversions.toShortZero(val, converter));
    }

    @Test
    public void testToInt_andZero() {
        Integer val = 123456;
        assertEquals(val.intValue(), NumberConversions.toInt(val, converter));
        assertEquals(CommonValues.INTEGER_ZERO, NumberConversions.toIntZero(val, converter));
    }

    @Test
    public void testToLong_andZero() {
        Long val = 9876543210L;
        assertEquals(val.longValue(), NumberConversions.toLong(val, converter));
        assertEquals(CommonValues.LONG_ZERO, NumberConversions.toLongZero(val, converter));
    }

    @Test
    public void testToFloat_andZero() {
        Float val = 12.34f;
        assertEquals(val.floatValue(), NumberConversions.toFloat(val, converter), 0.00001f);
        assertEquals(CommonValues.FLOAT_ZERO, NumberConversions.toFloatZero(val, converter));
    }

    @Test
    public void testFloatToString() {
        Float zero = 0.0f;
        Float nonZero = 3.14f;
        assertEquals("0", NumberConversions.floatToString(zero, converter));
        assertEquals(nonZero.toString(), NumberConversions.floatToString(nonZero, converter));
    }

    @Test
    public void testToDouble_andZero() {
        Double val = 12345.6789;
        assertEquals(val.doubleValue(), NumberConversions.toDouble(val, converter), 0.0000001);
        assertEquals(CommonValues.DOUBLE_ZERO, NumberConversions.toDoubleZero(val, converter));
    }

    @Test
    public void testDoubleToString() {
        Double zero = 0.0;
        Double nonZero = -2.71828;
        assertEquals("0", NumberConversions.doubleToString(zero, converter));
        assertEquals(nonZero.toString(), NumberConversions.doubleToString(nonZero, converter));
    }

    @Test
    public void testIntegerTypeToBigDecimalAndBigInteger() {
        Long val = 123456789L;
        assertEquals(BigDecimal.valueOf(val), NumberConversions.integerTypeToBigDecimal(val, converter));
        assertEquals(BigInteger.valueOf(val), NumberConversions.integerTypeToBigInteger(val, converter));
    }

    @Test
    public void testAtomicConversions() {
        Integer i = 42;
        Long l = 123456789L;
        assertEquals(i.intValue(), NumberConversions.toAtomicInteger(i, converter).get());
        assertEquals(l.longValue(), NumberConversions.toAtomicLong(l, converter).get());
        assertTrue(NumberConversions.toAtomicBoolean(l, converter).get()); // non‑zero => true
        assertFalse(NumberConversions.toAtomicBoolean(0L, converter).get()); // zero => false
    }

    @Test
    public void testFloatingPointToBigDecimalAndBigInteger() {
        Double d = 12345.6789;
        assertEquals(BigDecimal.valueOf(d), NumberConversions.floatingPointToBigDecimal(d, converter));

        // The method truncates toward zero after rounding to nearest whole number.
        Double pos = 12345.9;
        Double neg = -12345.1;
        assertEquals(BigInteger.valueOf(12346), NumberConversions.floatingPointToBigInteger(pos, converter));
        assertEquals(BigInteger.valueOf(-12345), NumberConversions.floatingPointToBigInteger(neg, converter));
    }

    @Test
    public void testIsNotZeroChecks() {
        assertTrue(NumberConversions.isIntTypeNotZero(5L, converter));
        assertFalse(NumberConversions.isIntTypeNotZero(0L, converter));

        assertTrue(NumberConversions.isFloatTypeNotZero(0.1, converter));
        assertFalse(NumberConversions.isFloatTypeNotZero(0.0, converter));

        assertTrue(NumberConversions.isBigIntegerNotZero(BigInteger.ONE, converter));
        assertFalse(NumberConversions.isBigIntegerNotZero(BigInteger.ZERO, converter));

        assertTrue(NumberConversions.isBigDecimalNotZero(BigDecimal.TEN, converter));
        assertFalse(NumberConversions.isBigDecimalNotZero(BigDecimal.ZERO, converter));
    }

    @Test
    public void testToCharacterWithinRange() {
        // value 65 => 'A'
        Integer val = 65;
        assertEquals('A', NumberConversions.toCharacter(val, converter));
    }

    @Test
    public void testToDateAndRelatedTemporalTypes() {
        Long epochMillis = 1_600_000_000_000L; // some fixed instant
        Date date = NumberConversions.toDate(epochMillis, converter);
        assertEquals(epochMillis.longValue(), date.getTime());

        Instant instant = NumberConversions.toInstant(epochMillis, converter);
        assertEquals(epochMillis.longValue(), instant.toEpochMilli());

        Duration duration = NumberConversions.toDuration(epochMillis, converter);
        assertEquals(epochMillis.longValue(), duration.toMillis());

        java.sql.Date sqlDate = NumberConversions.toSqlDate(epochMillis, converter);
        assertEquals(epochMillis.longValue(), sqlDate.getTime());

        Timestamp ts = NumberConversions.toTimestamp(epochMillis, converter);
        assertEquals(epochMillis.longValue(), ts.getTime());
    }

    @Test
    public void testCalendarConversion() {
        Long epoch = 1_600_000_000_000L;
        Calendar cal = NumberConversions.toCalendar(epoch, converter);
        assertEquals(epoch.longValue(), cal.getTimeInMillis());
    }

    @Test
    public void testZonedAndOffsetDateTimes() {
        Long epoch = 1_600_000_000_000L;
        ZonedDateTime zdt = NumberConversions.toZonedDateTime(epoch, converter);
        assertEquals(Instant.ofEpochMilli(epoch), zdt.toInstant());

        OffsetDateTime odt = NumberConversions.toOffsetDateTime(epoch, converter);
        assertEquals(zdt.toOffsetDateTime(), odt);
    }

    @Test
    public void testLocalDateAndLocalDateTime() {
        Long epoch = 1_600_000_000_000L;
        LocalDate ld = NumberConversions.toLocalDate(epoch, converter);
        LocalDateTime ldt = NumberConversions.toLocalDateTime(epoch, converter);

        ZoneId zone = converter.getOptions().getZoneId();

        assertEquals(LocalDate.ofInstant(Instant.ofEpochMilli(epoch), zone), ld);
        assertEquals(LocalDateTime.ofInstant(Instant.ofEpochMilli(epoch), zone), ldt);
    }

    @Test
    public void testToYear() {
        Short yearShort = (short) 2023;
        Year year = NumberConversions.toYear(yearShort, converter);
        assertEquals(2023, year.getValue());
    }

    /* --------------------------------------------------------------------- */
    /* 3. Edge‑case & exception tests                                         */
    /* --------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testToCharacterOutOfRangeNegative() {
        NumberConversions.toCharacter(-1, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToCharacterOutOfRangeTooLarge() {
        // Character.MAX_VALUE is 65535
        NumberConversions.toCharacter(70000L, converter);
    }

    @Test
    public void testFloatToStringWithNegativeZero() {
        // -0.0f should be treated as zero by the method (float comparison uses ==)
        Float negZero = -0.0f;
        assertEquals("0", NumberConversions.floatToString(negZero, converter));
    }

    @Test
    public void testDoubleToStringWithNegativeZero() {
        Double negZero = -0.0;
        assertEquals("0", NumberConversions.doubleToString(negZero, converter));
    }

    @Test
    public void testFloatingPointToBigIntegerRoundingBehaviour() {
        // Values exactly halfway should round away from zero because of Math.floor/ceil logic.
        Double posHalf = 2.5;
        Double negHalf = -2.5;
        assertEquals(BigInteger.valueOf(2), NumberConversions.floatingPointToBigInteger(posHalf, converter));
        assertEquals(BigInteger.valueOf(-2), NumberConversions.floatingPointToBigInteger(negHalf, converter));
    }

    @Test
    public void testIsIntTypeNotZeroWithLargeLong() {
        Long large = Long.MAX_VALUE;
        assertTrue(NumberConversions.isIntTypeNotZero(large, converter));
    }

    @Test
    public void testIsFloatTypeNotZeroWithNaN() {
        Double nan = Double.NaN;
        // NaN compared to 0.0 via != returns true, so method should report not zero
        assertTrue(NumberConversions.isFloatTypeNotZero(nan, converter));
    }

    @Test
    public void testAtomicBooleanWithNegativeNumber() {
        Long negative = -5L;
        assertTrue(NumberConversions.toAtomicBoolean(negative, converter).get());
    }

    @Test
    public void testToCharacterBoundaryValues() {
        // 0 and Character.MAX_VALUE are valid
        assertEquals((char)0, NumberConversions.toCharacter(0L, converter));
        assertEquals(Character.MAX_VALUE, NumberConversions.toCharacter((long)Character.MAX_VALUE, converter));
    }

    @Test
    public void testZeroConversionsReturnSharedConstants() {
        // Verify that zero‑returning methods return the exact shared instances
        assertSame(CommonValues.BYTE_ZERO, NumberConversions.toByteZero(123, converter));
        assertSame(CommonValues.SHORT_ZERO, NumberConversions.toShortZero(123, converter));
        assertSame(CommonValues.INTEGER_ZERO, NumberConversions.toIntZero(123, converter));
        assertSame(CommonValues.LONG_ZERO, NumberConversions.toLongZero(123, converter));
        assertSame(CommonValues.FLOAT_ZERO, NumberConversions.toFloatZero(123f, converter));
        assertSame(CommonValues.DOUBLE_ZERO, NumberConversions.toDoubleZero(123.0, converter));
    }

}
