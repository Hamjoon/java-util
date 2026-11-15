package com.cedarsoftware.util.convert;

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

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test class for {@link NumberConversions}.
 *
 * The tests are written in the same package so that the package‑private static
 * methods are directly accessible.
 */
public class NumberConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // ConverterOptions has a public no‑arg constructor in the library.
        converter = new Converter(new ConverterOptions());
    }

    /* -------------------------------------------------------------
     *  1.  Primitive‑to‑Number conversions (normal path)
     * ------------------------------------------------------------- */

    @Test
    public void testToByte() {
        assertEquals((byte) 123, NumberConversions.toByte((byte) 123, converter));
        assertEquals((byte) -12, NumberConversions.toByte((short) -12, converter));
    }

    @Test
    public void testToByteZero() {
        assertEquals(Byte.valueOf((byte) 0), NumberConversions.toByteZero(null, converter));
    }

    @Test
    public void testToShort() {
        assertEquals((short) 32000, NumberConversions.toShort((short) 32000, converter));
        assertEquals((short) -123, NumberConversions.toShort((int) -123, converter));
    }

    @Test
    public void testToShortZero() {
        assertEquals(Short.valueOf((short) 0), NumberConversions.toShortZero(null, converter));
    }

    @Test
    public void testToInt() {
        assertEquals(42, NumberConversions.toInt(Integer.valueOf(42), converter));
        assertEquals(-1, NumberConversions.toInt(Long.valueOf(-1L), converter));
    }

    @Test
    public void testToIntZero() {
        assertEquals(Integer.valueOf(0), NumberConversions.toIntZero(null, converter));
    }

    @Test
    public void testToLong() {
        assertEquals(123456789L, NumberConversions.toLong(Long.valueOf(123456789L), converter));
        assertEquals(-5L, NumberConversions.toLong(Integer.valueOf(-5), converter));
    }

    @Test
    public void testToLongZero() {
        assertEquals(Long.valueOf(0L), NumberConversions.toLongZero(null, converter));
    }

    @Test
    public void testToFloat() {
        assertEquals(3.14f, NumberConversions.toFloat(Float.valueOf(3.14f), converter), 0.00001f);
        assertEquals(-2.5f, NumberConversions.toFloat(Double.valueOf(-2.5), converter), 0.00001f);
    }

    @Test
    public void testToFloatZero() {
        assertEquals(Float.valueOf(0f), NumberConversions.toFloatZero(null, converter));
    }

    @Test
    public void testFloatToString() {
        assertEquals("0", NumberConversions.floatToString(Float.valueOf(0f), converter));
        assertEquals("1.5", NumberConversions.floatToString(Float.valueOf(1.5f), converter));
        // non‑zero special value (NaN)
        assertEquals("NaN", NumberConversions.floatToString(Float.valueOf(Float.NaN), converter));
    }

    @Test
    public void testToDouble() {
        assertEquals(2.71828, NumberConversions.toDouble(Double.valueOf(2.71828), converter), 1e-12);
        assertEquals(-0.5, NumberConversions.toDouble(Float.valueOf(-0.5f), converter), 1e-12);
    }

    @Test
    public void testToDoubleZero() {
        assertEquals(Double.valueOf(0d), NumberConversions.toDoubleZero(null, converter));
    }

    @Test
    public void testDoubleToString() {
        assertEquals("0", NumberConversions.doubleToString(Double.valueOf(0d), converter));
        assertEquals("3.1415", NumberConversions.doubleToString(Double.valueOf(3.1415), converter));
        // non‑zero special value (Infinity)
        assertEquals("Infinity", NumberConversions.doubleToString(Double.valueOf(Double.POSITIVE_INFINITY), converter));
    }

    /* -------------------------------------------------------------
     *  2.  Integer‑type → BigDecimal / BigInteger
     * ------------------------------------------------------------- */

    @Test
    public void testIntegerTypeToBigDecimal() {
        NumberConversions.toLong(Long.valueOf(12345L), converter); // just to ensure conversion works
        BigDecimal bd = NumberConversions.integerTypeToBigDecimal(Long.valueOf(12345L), converter);
        assertEquals(BigDecimal.valueOf(12345L), bd);
    }

    @Test
    public void testIntegerTypeToBigInteger() {
        BigInteger bi = NumberConversions.integerTypeToBigInteger(Integer.valueOf(9876), converter);
        assertEquals(BigInteger.valueOf(9876L), bi);
    }

    /* -------------------------------------------------------------
     *  3.  Atomic types
     * ------------------------------------------------------------- */

    @Test
    public void testToAtomicLong() {
        AtomicLong al = NumberConversions.toAtomicLong(Integer.valueOf(55), converter);
        assertEquals(55L, al.get());
    }

    @Test
    public void testToAtomicInteger() {
        AtomicInteger ai = NumberConversions.toAtomicInteger(Long.valueOf(99L), converter);
        assertEquals(99, ai.get());
    }

    @Test
    public void testToAtomicBoolean() {
        AtomicBoolean abTrue = NumberConversions.toAtomicBoolean(Long.valueOf(1L), converter);
        AtomicBoolean abFalse = NumberConversions.toAtomicBoolean(Long.valueOf(0L), converter);
        assertTrue(abTrue.get());
        assertFalse(abFalse.get());
    }

    /* -------------------------------------------------------------
     *  4.  Floating‑point → BigDecimal / BigInteger
     * ------------------------------------------------------------- */

    @Test
    public void testFloatingPointToBigDecimal() {
        BigDecimal bd = NumberConversions.floatingPointToBigDecimal(Double.valueOf(12.34), converter);
        assertEquals(BigDecimal.valueOf(12.34), bd);
    }

    @Test
    public void testFloatingPointToBigInteger_Rounding() {
        // Positive number – floor
        BigInteger biPos = NumberConversions.floatingPointToBigInteger(Double.valueOf(123.9), converter);
        assertEquals(BigInteger.valueOf(123L), biPos);

        // Negative number – ceil
        BigInteger biNeg = NumberConversions.floatingPointToBigInteger(Double.valueOf(-123.9), converter);
        assertEquals(BigInteger.valueOf(-123L), biNeg);
    }

    /* -------------------------------------------------------------
     *  5.  Zero‑check helpers
     * ------------------------------------------------------------- */

    @Test
    public void testIsIntTypeNotZero() {
        assertTrue(NumberConversions.isIntTypeNotZero(Long.valueOf(5L), converter));
        assertFalse(NumberConversions.isIntTypeNotZero(Long.valueOf(0L), converter));
    }

    @Test
    public void testIsFloatTypeNotZero() {
        assertTrue(NumberConversions.isFloatTypeNotZero(Double.valueOf(0.1), converter));
        assertFalse(NumberConversions.isFloatTypeNotZero(Double.valueOf(0.0), converter));
    }

    @Test
    public void testIsBigIntegerNotZero() {
        assertTrue(NumberConversions.isBigIntegerNotZero(BigInteger.valueOf(7), converter));
        assertFalse(NumberConversions.isBigIntegerNotZero(BigInteger.ZERO, converter));
    }

    @Test
    public void testIsBigDecimalNotZero() {
        assertTrue(NumberConversions.isBigDecimalNotZero(BigDecimal.valueOf(-3.14), converter));
        assertFalse(NumberConversions.isBigDecimalNotZero(BigDecimal.ZERO, converter));
    }

    /* -------------------------------------------------------------
     *  6.  Character conversion (including error case)
     * ------------------------------------------------------------- */

    @Test
    public void testToCharacter_ValidRange() {
        // 0
        assertEquals('\u0000', NumberConversions.toCharacter(Long.valueOf(0L), converter));
        // middle value
        assertEquals('A', NumberConversions.toCharacter(Integer.valueOf(65), converter));
        // max char value
        assertEquals(Character.MAX_VALUE, NumberConversions.toCharacter(Integer.valueOf(Character.MAX_VALUE), converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToCharacter_ValueTooLarge() {
        // value greater than Character.MAX_VALUE should throw
        NumberConversions.toCharacter(Long.valueOf((long) Character.MAX_VALUE + 1), converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToCharacter_NegativeValue() {
        // negative values are illegal
        NumberConversions.toCharacter(Integer.valueOf(-1), converter);
    }

    /* -------------------------------------------------------------
     *  7.  Date / Time conversions
     * ------------------------------------------------------------- */

    @Test
    public void testToDate() {
        long epoch = 1_600_000_000_000L; // some fixed instant
        Date d = NumberConversions.toDate(Long.valueOf(epoch), converter);
        assertEquals(epoch, d.getTime());
    }

    @Test
    public void testToDuration() {
        long millis = 12345L;
        Duration dur = NumberConversions.toDuration(Long.valueOf(millis), converter);
        assertEquals(millis, dur.toMillis());
    }

    @Test
    public void testToInstant() {
        long epoch = 1_500_000_000_000L;
        Instant instant = NumberConversions.toInstant(Long.valueOf(epoch), converter);
        assertEquals(epoch, instant.toEpochMilli());
    }

    @Test
    public void testToSqlDate() {
        long epoch = 0L;
        java.sql.Date sqlDate = NumberConversions.toSqlDate(Long.valueOf(epoch), converter);
        assertEquals(epoch, sqlDate.getTime());
    }

    @Test
    public void testToTimestamp() {
        long epoch = 2_000_000_000L;
        Timestamp ts = NumberConversions.toTimestamp(Long.valueOf(epoch), converter);
        assertEquals(epoch, ts.getTime());
    }

    @Test
    public void testToCalendar() {
        long epoch = 1_234_567_890L;
        Calendar cal = NumberConversions.toCalendar(Long.valueOf(epoch), converter);
        assertEquals(epoch, cal.getTimeInMillis());
    }

    @Test
    public void testToZonedDateTime() {
        long epoch = 1_600_000_000_000L;
        ZonedDateTime zdt = NumberConversions.toZonedDateTime(Long.valueOf(epoch), converter);
        assertEquals(epoch, zdt.toInstant().toEpochMilli());
        // ensure the zone is the one from ConverterOptions
        assertEquals(converter.getOptions().getZoneId(), zdt.getZone());
    }

    @Test
    public void testToLocalDate() {
        // Use an epoch that corresponds to a known date (e.g., 2020‑01‑01)
        Instant instant = Instant.parse("2020-01-01T00:00:00Z");
        long epoch = instant.toEpochMilli();
        LocalDate ld = NumberConversions.toLocalDate(Long.valueOf(epoch), converter);
        assertEquals(LocalDate.of(2020, 1, 1), ld);
    }

    @Test
    public void testToLocalDateTime() {
        Instant instant = Instant.parse("2021-06-15T13:45:30Z");
        long epoch = instant.toEpochMilli();
        LocalDateTime ldt = NumberConversions.toLocalDateTime(Long.valueOf(epoch), converter);
        // The result should be the same instant in the converter's zone
        ZonedDateTime expected = instant.atZone(converter.getOptions().getZoneId());
        assertEquals(expected.toLocalDateTime(), ldt);
    }

    @Test
    public void testToOffsetDateTime() {
        Instant instant = Instant.parse("2022-12-31T23:59:59Z");
        long epoch = instant.toEpochMilli();
        OffsetDateTime odt = NumberConversions.toOffsetDateTime(Long.valueOf(epoch), converter);
        ZonedDateTime expected = instant.atZone(converter.getOptions().getZoneId());
        assertEquals(expected.toOffsetDateTime(), odt);
    }

    /* -------------------------------------------------------------
     *  8.  Year conversion
     * ------------------------------------------------------------- */

    @Test
    public void testToYear() {
        Short yearShort = Short.valueOf((short) 1999);
        Year y = NumberConversions.toYear(yearShort, converter);
        assertEquals(1999, y.getValue());
    }
}
