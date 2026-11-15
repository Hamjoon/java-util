package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.Year;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive JUnit‑4 test suite for {@link NumberConversions}.
 * All static conversion methods are exercised with typical, boundary and error cases.
 */
public class NumberConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // ConverterOptions is part of the library; it has a public no‑arg constructor.
        // Using the default instance gives a sensible ZoneId (usually system default).
        converter = new Converter(new ConverterOptions());
    }

    // ----------------------------------------------------------------------
    // Primitive numeric conversions and zero‑value shortcuts
    // ----------------------------------------------------------------------
    @Test
    public void testByteConversions() {
        Byte source = (byte) 123;
        assertEquals((byte) 123, NumberConversions.toByte(source, converter));
        assertEquals(Byte.valueOf((byte)0), NumberConversions.toByteZero(source, converter));
    }

    @Test
    public void testShortConversions() {
        Short source = (short) 32000;
        assertEquals((short) 32000, NumberConversions.toShort(source, converter));
        assertEquals(Short.valueOf((short)0), NumberConversions.toShortZero(source, converter));
    }

    @Test
    public void testIntConversions() {
        Integer source = 1_234_567;
        assertEquals(1_234_567, NumberConversions.toInt(source, converter));
        assertEquals(Integer.valueOf(0), NumberConversions.toIntZero(source, converter));
    }

    @Test
    public void testLongConversions() {
        Long source = 9_876_543_210L;
        assertEquals(9_876_543_210L, NumberConversions.toLong(source, converter));
        assertEquals(Long.valueOf(0L), NumberConversions.toLongZero(source, converter));
    }

    @Test
    public void testFloatConversions() {
        Float source = 12.34f;
        assertEquals(12.34f, NumberConversions.toFloat(source, converter), 0.00001f);
        assertEquals(Float.valueOf(0f), NumberConversions.toFloatZero(source, converter));
    }

    @Test
    public void testFloatToString() {
        Float zero = 0f;
        Float nonZero = 3.14f;
        assertEquals("0", NumberConversions.floatToString(zero, converter));
        assertEquals(nonZero.toString(), NumberConversions.floatToString(nonZero, converter));
    }

    @Test
    public void testDoubleConversions() {
        Double source = 12345.6789;
        assertEquals(12345.6789, NumberConversions.toDouble(source, converter), 0.0000001);
        assertEquals(Double.valueOf(0d), NumberConversions.toDoubleZero(source, converter));
    }

    @Test
    public void testDoubleToString() {
        Double zero = 0d;
        Double nonZero = -2.71828;
        assertEquals("0", NumberConversions.doubleToString(zero, converter));
        assertEquals(nonZero.toString(), NumberConversions.doubleToString(nonZero, converter));
    }

    // ----------------------------------------------------------------------
    // BigDecimal / BigInteger from integer types
    // ----------------------------------------------------------------------
    @Test
    public void testIntegerTypeToBigDecimalAndBigInteger() {
        Long source = 123456789L;
        BigDecimal bd = NumberConversions.integerTypeToBigDecimal(source, converter);
        BigInteger bi = NumberConversions.integerTypeToBigInteger(source, converter);
        assertEquals(BigDecimal.valueOf(123456789L), bd);
        assertEquals(BigInteger.valueOf(123456789L), bi);
    }

    // ----------------------------------------------------------------------
    // Atomic types
    // ----------------------------------------------------------------------
    @Test
    public void testAtomicConversions() {
        Integer intSrc = 42;
        Long longSrc = 999L;
        AtomicInteger ai = NumberConversions.toAtomicInteger(intSrc, converter);
        AtomicLong al = NumberConversions.toAtomicLong(longSrc, converter);
        AtomicBoolean abZero = NumberConversions.toAtomicBoolean(0L, converter);
        AtomicBoolean abNonZero = NumberConversions.toAtomicBoolean(5L, converter);

        assertEquals(42, ai.get());
        assertEquals(999L, al.get());
        assertFalse(abZero.get());
        assertTrue(abNonZero.get());
    }

    // ----------------------------------------------------------------------
    // Floating‑point to BigDecimal / BigInteger
    // ----------------------------------------------------------------------
    @Test
    public void testFloatingPointToBigDecimal() {
        Double src = 12345.6789;
        BigDecimal bd = NumberConversions.floatingPointToBigDecimal(src, converter);
        assertEquals(BigDecimal.valueOf(12345.6789), bd);
    }

    @Test
    public void testFloatingPointToBigIntegerRounding() {
        // Positive number – should floor
        Double pos = 12.9;
        BigInteger biPos = NumberConversions.floatingPointToBigInteger(pos, converter);
        assertEquals(BigInteger.valueOf(12L), biPos);

        // Negative number – should ceil (i.e., toward zero)
        Double neg = -12.9;
        BigInteger biNeg = NumberConversions.floatingPointToBigInteger(neg, converter);
        assertEquals(BigInteger.valueOf(-12L), biNeg);

        // Exact integer value – no rounding needed
        Double exact = 100.0;
        BigInteger biExact = NumberConversions.floatingPointToBigInteger(exact, converter);
        assertEquals(BigInteger.valueOf(100L), biExact);
    }

    // ----------------------------------------------------------------------
    // Zero‑check helpers
    // ----------------------------------------------------------------------
    @Test
    public void testIsIntTypeNotZero() {
        assertTrue(NumberConversions.isIntTypeNotZero(5L, converter));
        assertFalse(NumberConversions.isIntTypeNotZero(0L, converter));
    }

    @Test
    public void testIsFloatTypeNotZero() {
        assertTrue(NumberConversions.isFloatTypeNotZero(0.1, converter));
        assertFalse(NumberConversions.isFloatTypeNotZero(0.0, converter));
    }

    @Test
    public void testIsBigIntegerNotZero() {
        assertTrue(NumberConversions.isBigIntegerNotZero(BigInteger.ONE, converter));
        assertFalse(NumberConversions.isBigIntegerNotZero(BigInteger.ZERO, converter));
    }

    @Test
    public void testIsBigDecimalNotZero() {
        assertTrue(NumberConversions.isBigDecimalNotZero(new BigDecimal("3.14"), converter));
        assertFalse(NumberConversions.isBigDecimalNotZero(BigDecimal.ZERO, converter));
    }

    // ----------------------------------------------------------------------
    // Character conversion and error handling
    // ----------------------------------------------------------------------
    @Test
    public void testToCharacterWithinRange() {
        // value 65 -> 'A'
        Integer src = 65;
        assertEquals('A', NumberConversions.toCharacter(src, converter));
        // max char value
        Integer max = (int) Character.MAX_VALUE;
        assertEquals((char) Character.MAX_VALUE, NumberConversions.toCharacter(max, converter));
        // zero
        Integer zero = 0;
        assertEquals('\0', NumberConversions.toCharacter(zero, converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToCharacterOutOfRangePositive() {
        // value > Character.MAX_VALUE should throw
        Integer src = (int) Character.MAX_VALUE + 1;
        NumberConversions.toCharacter(src, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToCharacterOutOfRangeNegative() {
        // negative value should throw
        Integer src = -1;
        NumberConversions.toCharacter(src, converter);
    }

    // ----------------------------------------------------------------------
    // Date / Time conversions
    // ----------------------------------------------------------------------
    @Test
    public void testDateConversions() {
        Long epochMillis = 1_600_000_000_000L; // some fixed epoch
        Date date = NumberConversions.toDate(epochMillis, converter);
        assertEquals(epochMillis.longValue(), date.getTime());

        Duration duration = NumberConversions.toDuration(epochMillis, converter);
        assertEquals(epochMillis.longValue(), duration.toMillis());

        Instant instant = NumberConversions.toInstant(epochMillis, converter);
        assertEquals(epochMillis.longValue(), instant.toEpochMilli());

        java.sql.Date sqlDate = NumberConversions.toSqlDate(epochMillis, converter);
        assertEquals(epochMillis.longValue(), sqlDate.getTime());

        Timestamp timestamp = NumberConversions.toTimestamp(epochMillis, converter);
        assertEquals(epochMillis.longValue(), timestamp.getTime());
    }

    @Test
    public void testCalendarConversion() {
        Long epochMillis = 1_600_000_000_000L;
        Calendar cal = NumberConversions.toCalendar(epochMillis, converter);
        assertEquals(epochMillis.longValue(), cal.getTimeInMillis());
    }

    @Test
    public void testZonedAndOffsetDateTimeConversions() {
        Long epochMillis = 1_600_000_000_000L;
        ZoneId zone = converter.getOptions().getZoneId();

        ZonedDateTime zdt = NumberConversions.toZonedDateTime(epochMillis, converter);
        assertEquals(Instant.ofEpochMilli(epochMillis), zdt.toInstant());
        assertEquals(zone, zdt.getZone());

        OffsetDateTime odt = NumberConversions.toOffsetDateTime(epochMillis, converter);
        assertEquals(zdt.toOffsetDateTime(), odt);
    }

    @Test
    public void testLocalDateAndLocalDateTimeConversions() {
        // Use a known epoch that corresponds to a date with no time‑zone ambiguity
        Long epochMillis = 1_600_000_000_000L; // 2020‑09‑13T12:26:40Z
        LocalDate ld = NumberConversions.toLocalDate(epochMillis, converter);
        LocalDateTime ldt = NumberConversions.toLocalDateTime(epochMillis, converter);

        ZonedDateTime zdt = NumberConversions.toZonedDateTime(epochMillis, converter);
        assertEquals(zdt.toLocalDate(), ld);
        assertEquals(zdt.toLocalDateTime(), ldt);
    }

    // ----------------------------------------------------------------------
    // Year conversion
    // ----------------------------------------------------------------------
    @Test
    public void testYearConversion() {
        Short src = 2021;
        Year year = NumberConversions.toYear(src, converter);
        assertEquals(2021, year.getValue());

        // short negative value should still produce a Year (though out of normal range)
        Short neg = -5;
        Year negYear = NumberConversions.toYear(neg, converter);
        assertEquals(-5, negYear.getValue());
    }
}
