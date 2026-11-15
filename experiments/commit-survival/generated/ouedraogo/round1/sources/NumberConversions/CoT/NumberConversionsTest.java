package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class NumberConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // Assume ConverterOptions has a public no‑arg constructor and provides a default ZoneId.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    /* ----------------------------------------------------------------------
     *  List of public (package‑private) static methods in NumberConversions
     * ---------------------------------------------------------------------- */
    // static byte   toByte(Object, Converter)
    // static Byte   toByteZero(Object, Converter)
    // static short  toShort(Object, Converter)
    // static Short  toShortZero(Object, Converter)
    // static int    toInt(Object, Converter)
    // static Integer toIntZero(Object, Converter)
    // static long   toLong(Object, Converter)
    // static Long   toLongZero(Object, Converter)
    // static float  toFloat(Object, Converter)
    // static Float  toFloatZero(Object, Converter)
    // static String floatToString(Object, Converter)
    // static double toDouble(Object, Converter)
    // static Double toDoubleZero(Object, Converter)
    // static String doubleToString(Object, Converter)
    // static BigDecimal integerTypeToBigDecimal(Object, Converter)
    // static BigInteger integerTypeToBigInteger(Object, Converter)
    // static AtomicLong   toAtomicLong(Object, Converter)
    // static AtomicInteger toAtomicInteger(Object, Converter)
    // static AtomicBoolean toAtomicBoolean(Object, Converter)
    // static BigDecimal floatingPointToBigDecimal(Object, Converter)
    // static BigInteger floatingPointToBigInteger(Object, Converter)
    // static boolean isIntTypeNotZero(Object, Converter)
    // static boolean isFloatTypeNotZero(Object, Converter)
    // static boolean isBigIntegerNotZero(Object, Converter)
    // static boolean isBigDecimalNotZero(Object, Converter)
    // static char   toCharacter(Object, Converter) throws IllegalArgumentException
    // static Date   toDate(Object, Converter)
    // static Duration toDuration(Object, Converter)
    // static Instant toInstant(Object, Converter)
    // static java.sql.Date toSqlDate(Object, Converter)
    // static Timestamp toTimestamp(Object, Converter)
    // static Calendar toCalendar(Object, Converter)
    // static LocalDate toLocalDate(Object, Converter)
    // static LocalDateTime toLocalDateTime(Object, Converter)
    // static ZonedDateTime toZonedDateTime(Object, Converter)
    // static OffsetDateTime toOffsetDateTime(Object, Converter)
    // static Year   toYear(Object, Converter)

    /* -------------------------- Basic functionality tests -------------------------- */

    @Test
    public void testToByte() {
        assertEquals((byte) 123, NumberConversions.toByte((byte) 123, converter));
        assertEquals((byte) -45, NumberConversions.toByte((short) -45, converter));
        assertEquals((byte) 127, NumberConversions.toByte(Integer.valueOf(127), converter));
    }

    @Test
    public void testToByteZero() {
        assertEquals(Byte.valueOf((byte) 0), NumberConversions.toByteZero(Integer.valueOf(999), converter));
    }

    @Test
    public void testToShort() {
        assertEquals((short) 32000, NumberConversions.toShort((short) 32000, converter));
        assertEquals((short) -1234, NumberConversions.toShort(Integer.valueOf(-1234), converter));
    }

    @Test
    public void testToShortZero() {
        assertEquals(Short.valueOf((short) 0), NumberConversions.toShortZero(Long.valueOf(1000L), converter));
    }

    @Test
    public void testToInt() {
        assertEquals(42, NumberConversions.toInt(Integer.valueOf(42), converter));
        assertEquals(-1, NumberConversions.toInt(Long.valueOf(-1L), converter));
    }

    @Test
    public void testToIntZero() {
        assertEquals(Integer.valueOf(0), NumberConversions.toIntZero(Double.valueOf(99.9), converter));
    }

    @Test
    public void testToLong() {
        assertEquals(123456789L, NumberConversions.toLong(Long.valueOf(123456789L), converter));
        assertEquals(0L, NumberConversions.toLong(Integer.valueOf(0), converter));
    }

    @Test
    public void testToLongZero() {
        assertEquals(Long.valueOf(0L), NumberConversions.toLongZero(Float.valueOf(3.14f), converter));
    }

    @Test
    public void testToFloat() {
        assertEquals(1.5f, NumberConversions.toFloat(Float.valueOf(1.5f), converter), 0.00001f);
        assertEquals(-2.0f, NumberConversions.toFloat(Double.valueOf(-2.0), converter), 0.00001f);
    }

    @Test
    public void testToFloatZero() {
        assertEquals(Float.valueOf(0f), NumberConversions.toFloatZero(Long.valueOf(123L), converter));
    }

    @Test
    public void testFloatToString() {
        assertEquals("0", NumberConversions.floatToString(0.0f, converter));
        assertEquals("3.14", NumberConversions.floatToString(3.14f, converter));
        assertEquals("-2.5", NumberConversions.floatToString(-2.5f, converter));
    }

    @Test
    public void testToDouble() {
        assertEquals(2.71828, NumberConversions.toDouble(Double.valueOf(2.71828), converter), 0.0000001);
        assertEquals(-1.0, NumberConversions.toDouble(Float.valueOf(-1.0f), converter), 0.0000001);
    }

    @Test
    public void testToDoubleZero() {
        assertEquals(Double.valueOf(0.0), NumberConversions.toDoubleZero(Integer.valueOf(5), converter));
    }

    @Test
    public void testDoubleToString() {
        assertEquals("0", NumberConversions.doubleToString(0.0d, converter));
        assertEquals("123.456", NumberConversions.doubleToString(123.456d, converter));
    }

    @Test
    public void testIntegerTypeToBigDecimal() {
        assertEquals(BigDecimal.valueOf(12345L), NumberConversions.integerTypeToBigDecimal(Long.valueOf(12345L), converter));
    }

    @Test
    public void testIntegerTypeToBigInteger() {
        assertEquals(BigInteger.valueOf(98765L), NumberConversions.integerTypeToBigInteger(Long.valueOf(98765L), converter));
    }

    @Test
    public void testToAtomicLong() {
        AtomicLong al = NumberConversions.toAtomicLong(Integer.valueOf(42), converter);
        assertEquals(42L, al.get());
    }

    @Test
    public void testToAtomicInteger() {
        AtomicInteger ai = NumberConversions.toAtomicInteger(Long.valueOf(99L), converter);
        assertEquals(99, ai.get());
    }

    @Test
    public void testToAtomicBoolean() {
        assertTrue(NumberConversions.toAtomicBoolean(Long.valueOf(1L), converter).get());
        assertFalse(NumberConversions.toAtomicBoolean(Integer.valueOf(0), converter).get());
    }

    @Test
    public void testFloatingPointToBigDecimal() {
        assertEquals(BigDecimal.valueOf(3.14159), NumberConversions.floatingPointToBigDecimal(Double.valueOf(3.14159), converter));
    }

    @Test
    public void testFloatingPointToBigInteger_Rounding() {
        // Positive value – should floor
        assertEquals(BigInteger.valueOf(123L), NumberConversions.floatingPointToBigInteger(Double.valueOf(123.9), converter));
        // Negative value – should ceil (i.e., more negative)
        assertEquals(BigInteger.valueOf(-124L), NumberConversions.floatingPointToBigInteger(Double.valueOf(-123.9), converter));
        // Exact integer
        assertEquals(BigInteger.valueOf(0L), NumberConversions.floatingPointToBigInteger(Double.valueOf(0.0), converter));
    }

    @Test
    public void testIsIntTypeNotZero() {
        assertTrue(NumberConversions.isIntTypeNotZero(Integer.valueOf(5), converter));
        assertFalse(NumberConversions.isIntTypeNotZero(Long.valueOf(0L), converter));
    }

    @Test
    public void testIsFloatTypeNotZero() {
        assertTrue(NumberConversions.isFloatTypeNotZero(Float.valueOf(0.1f), converter));
        assertFalse(NumberConversions.isFloatTypeNotZero(Double.valueOf(0.0), converter));
    }

    @Test
    public void testIsBigIntegerNotZero() {
        assertTrue(NumberConversions.isBigIntegerNotZero(BigInteger.valueOf(10), converter));
        assertFalse(NumberConversions.isBigIntegerNotZero(BigInteger.ZERO, converter));
    }

    @Test
    public void testIsBigDecimalNotZero() {
        assertTrue(NumberConversions.isBigDecimalNotZero(new BigDecimal("0.01"), converter));
        assertFalse(NumberConversions.isBigDecimalNotZero(BigDecimal.ZERO, converter));
    }

    @Test
    public void testToCharacter_Valid() {
        assertEquals('A', NumberConversions.toCharacter(Integer.valueOf(65), converter));
        assertEquals('\u0000', NumberConversions.toCharacter(Integer.valueOf(0), converter));
        assertEquals(Character.MAX_VALUE, NumberConversions.toCharacter(Integer.valueOf((int) Character.MAX_VALUE), converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToCharacter_NegativeValue() {
        NumberConversions.toCharacter(Integer.valueOf(-1), converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToCharacter_ValueTooLarge() {
        NumberConversions.toCharacter(Long.valueOf((long) Character.MAX_VALUE + 1), converter);
    }

    @Test
    public void testToDate() {
        long now = System.currentTimeMillis();
        Date d = NumberConversions.toDate(Long.valueOf(now), converter);
        assertEquals(now, d.getTime());
    }

    @Test
    public void testToDuration() {
        Duration dur = NumberConversions.toDuration(Long.valueOf(1500L), converter);
        assertEquals(Duration.ofMillis(1500L), dur);
    }

    @Test
    public void testToInstant() {
        long epoch = 1625097600000L; // 2021‑07‑01T00:00:00Z
        Instant instant = NumberConversions.toInstant(Long.valueOf(epoch), converter);
        assertEquals(Instant.ofEpochMilli(epoch), instant);
    }

    @Test
    public void testToSqlDate() {
        long epoch = 0L;
        java.sql.Date sqlDate = NumberConversions.toSqlDate(Long.valueOf(epoch), converter);
        assertEquals(epoch, sqlDate.getTime());
    }

    @Test
    public void testToTimestamp() {
        long epoch = 123456789L;
        Timestamp ts = NumberConversions.toTimestamp(Long.valueOf(epoch), converter);
        assertEquals(epoch, ts.getTime());
    }

    @Test
    public void testToCalendar() {
        long epoch = 1609459200000L; // 2021‑01‑01T00:00:00Z
        Calendar cal = NumberConversions.toCalendar(Long.valueOf(epoch), converter);
        assertEquals(epoch, cal.getTimeInMillis());
    }

    @Test
    public void testToZonedDateTime_andRelated() {
        long epoch = 1622505600000L; // 2021‑06‑01T00:00:00Z
        ZonedDateTime zdt = NumberConversions.toZonedDateTime(Long.valueOf(epoch), converter);
        assertEquals(Instant.ofEpochMilli(epoch), zdt.toInstant());

        LocalDate ld = NumberConversions.toLocalDate(Long.valueOf(epoch), converter);
        assertEquals(zdt.toLocalDate(), ld);

        LocalDateTime ldt = NumberConversions.toLocalDateTime(Long.valueOf(epoch), converter);
        assertEquals(zdt.toLocalDateTime(), ldt);

        OffsetDateTime odt = NumberConversions.toOffsetDateTime(Long.valueOf(epoch), converter);
        assertEquals(zdt.toOffsetDateTime(), odt);
    }

    @Test
    public void testToYear() {
        Number n = Short.valueOf((short) 2023);
        Year year = NumberConversions.toYear(n, converter);
        assertEquals(2023, year.getValue());

        // Verify that short overflow is handled via shortValue()
        Number overflow = Integer.valueOf(65537); // 65537 % 2^16 = 1 when cast to short
        Year overflowYear = NumberConversions.toYear(overflow, converter);
        assertEquals(1, overflowYear.getValue());
    }
}
