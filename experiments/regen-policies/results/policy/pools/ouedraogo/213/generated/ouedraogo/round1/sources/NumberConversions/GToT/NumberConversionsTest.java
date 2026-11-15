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
 * Comprehensive JUnit‑4 test suite for {@link NumberConversions}.
 *
 * The tests are organised by the public‑static methods of {@code NumberConversions}.
 * For each method we provide:
 * <ul>
 *   <li>a basic “happy path” test that verifies the expected conversion</li>
 *   <li>edge‑case tests (null, zero, max/min values, sign handling, etc.)</li>
 *   <li>exception‑checking tests where the contract specifies an exception</li>
 * </ul>
 *
 * The {@link Converter} instance used throughout the suite is created with the
 * default {@link ConverterOptions}.  No mocking is performed – only real
 * objects and factory methods that appear in the source code are used.
 */
public class NumberConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // Converter has a public constructor that takes a ConverterOptions instance.
        // Using the default options is sufficient for all tests.
        converter = new Converter(new ConverterOptions());
    }

    /* -------------------------------------------------------------
     *  Primitive numeric conversions (byte, short, int, long, float, double)
     * ------------------------------------------------------------- */

    @Test
    public void testToByte() {
        Byte result = NumberConversions.toByte((byte) 5, converter);
        assertEquals((byte) 5, result.byteValue());
    }

    @Test
    public void testToByteZero() {
        assertEquals(Byte.valueOf(0), NumberConversions.toByteZero(null, converter));
    }

    @Test
    public void testToShort() {
        Short result = NumberConversions.toShort((short) -123, converter);
        assertEquals((short) -123, result.shortValue());
    }

    @Test
    public void testToShortZero() {
        assertEquals(Short.valueOf((short) 0), NumberConversions.toShortZero(null, converter));
    }

    @Test
    public void testToInt() {
        Integer result = NumberConversions.toInt(42, converter);
        assertEquals(42, result.intValue());
    }

    @Test
    public void testToIntZero() {
        assertEquals(Integer.valueOf(0), NumberConversions.toIntZero(null, converter));
    }

    @Test
    public void testToLong() {
        Long result = NumberConversions.toLong(1234567890123L, converter);
        assertEquals(1234567890123L, result.longValue());
    }

    @Test
    public void testToLongZero() {
        assertEquals(Long.valueOf(0L), NumberConversions.toLongZero(null, converter));
    }

    @Test
    public void testToFloat() {
        Float result = NumberConversions.toFloat(3.14f, converter);
        assertEquals(3.14f, result.floatValue(), 0.00001f);
    }

    @Test
    public void testToFloatZero() {
        assertEquals(Float.valueOf(0f), NumberConversions.toFloatZero(null, converter));
    }

    @Test
    public void testFloatToString_NonZero() {
        String s = NumberConversions.floatToString(2.5f, converter);
        assertEquals("2.5", s);
    }

    @Test
    public void testFloatToString_Zero() {
        assertEquals("0", NumberConversions.floatToString(0f, converter));
    }

    @Test
    public void testToDouble() {
        Double result = NumberConversions.toDouble(2.71828, converter);
        assertEquals(2.71828, result.doubleValue(), 0.0000001);
    }

    @Test
    public void testToDoubleZero() {
        assertEquals(Double.valueOf(0d), NumberConversions.toDoubleZero(null, converter));
    }

    @Test
    public void testDoubleToString_NonZero() {
        String s = NumberConversions.doubleToString(3.14159, converter);
        assertEquals("3.14159", s);
    }

    @Test
    public void testDoubleToString_Zero() {
        assertEquals("0", NumberConversions.doubleToString(0d, converter));
    }

    /* -------------------------------------------------------------
     *  Integer‑type to BigDecimal / BigInteger
     * ------------------------------------------------------------- */

    @Test
    public void testIntegerTypeToBigDecimal() {
        BigDecimal bd = NumberConversions.integerTypeToBigDecimal(12345L, converter);
        assertEquals(BigDecimal.valueOf(12345L), bd);
    }

    @Test
    public void testIntegerTypeToBigInteger() {
        BigInteger bi = NumberConversions.integerTypeToBigInteger(9876543210L, converter);
        assertEquals(BigInteger.valueOf(9876543210L), bi);
    }

    /* -------------------------------------------------------------
     *  Atomic types
     * ------------------------------------------------------------- */

    @Test
    public void testToAtomicLong() {
        AtomicLong al = NumberConversions.toAtomicLong(555L, converter);
        assertEquals(555L, al.get());
    }

    @Test
    public void testToAtomicInteger() {
        AtomicInteger ai = NumberConversions.toAtomicInteger(77, converter);
        assertEquals(77, ai.get());
    }

    @Test
    public void testToAtomicBoolean_True() {
        AtomicBoolean ab = NumberConversions.toAtomicBoolean(1L, converter);
        assertTrue(ab.get());
    }

    @Test
    public void testToAtomicBoolean_False() {
        AtomicBoolean ab = NumberConversions.toAtomicBoolean(0L, converter);
        assertFalse(ab.get());
    }

    /* -------------------------------------------------------------
     *  Floating‑point to BigDecimal / BigInteger
     * ------------------------------------------------------------- */

    @Test
    public void testFloatingPointToBigDecimal() {
        BigDecimal bd = NumberConversions.floatingPointToBigDecimal(1.23, converter);
        assertEquals(BigDecimal.valueOf(1.23), bd);
    }

    @Test
    public void testFloatingPointToBigInteger_Positive() {
        BigInteger bi = NumberConversions.floatingPointToBigInteger(123.9, converter);
        // floor for positive numbers
        assertEquals(BigInteger.valueOf(123), bi);
    }

    @Test
    public void testFloatingPointToBigInteger_Negative() {
        BigInteger bi = NumberConversions.floatingPointToBigInteger(-123.1, converter);
        // ceil for negative numbers
        assertEquals(BigInteger.valueOf(-123), bi);
    }

    /* -------------------------------------------------------------
     *  Zero‑checking helpers
     * ------------------------------------------------------------- */

    @Test
    public void testIsIntTypeNotZero_True() {
        assertTrue(NumberConversions.isIntTypeNotZero(5L, converter));
    }

    @Test
    public void testIsIntTypeNotZero_False() {
        assertFalse(NumberConversions.isIntTypeNotZero(0L, converter));
    }

    @Test
    public void testIsFloatTypeNotZero_True() {
        assertTrue(NumberConversions.isFloatTypeNotZero(0.001, converter));
    }

    @Test
    public void testIsFloatTypeNotZero_False() {
        assertFalse(NumberConversions.isFloatTypeNotZero(0.0, converter));
    }

    @Test
    public void testIsBigIntegerNotZero() {
        assertTrue(NumberConversions.isBigIntegerNotZero(BigInteger.ONE, converter));
        assertFalse(NumberConversions.isBigIntegerNotZero(BigInteger.ZERO, converter));
    }

    @Test
    public void testIsBigDecimalNotZero() {
        assertTrue(NumberConversions.isBigDecimalNotZero(BigDecimal.TEN, converter));
        assertFalse(NumberConversions.isBigDecimalNotZero(BigDecimal.ZERO, converter));
    }

    /* -------------------------------------------------------------
     *  Character conversion and range checking
     * ------------------------------------------------------------- */

    @Test
    public void testToCharacter_Valid() {
        char c = NumberConversions.toCharacter(65L, converter); // 'A'
        assertEquals('A', c);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToCharacter_OutOfRange_Negative() {
        NumberConversions.toCharacter(-1L, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToCharacter_OutOfRange_TooLarge() {
        NumberConversions.toCharacter((long) Character.MAX_VALUE + 1, converter);
    }

    /* -------------------------------------------------------------
     *  Date / Time conversions
     * ------------------------------------------------------------- */

    @Test
    public void testToDate() {
        long epoch = 1_600_000_000_000L; // 2020‑09‑13T12:26:40Z
        Date date = NumberConversions.toDate(epoch, converter);
        assertEquals(epoch, date.getTime());
    }

    @Test
    public void testToDuration_FromLong() {
        Duration d = NumberConversions.toDuration(1500L, converter); // 1500 ms
        assertEquals(Duration.ofMillis(1500L), d);
    }

    @Test
    public void testToDuration_FromBigDecimal() {
        BigDecimal seconds = new BigDecimal("2.5"); // 2.5 seconds
        Duration d = NumberConversions.toDuration(seconds, converter);
        assertEquals(Duration.ofSeconds(2, 500_000_000L), d);
    }

    @Test
    public void testToDuration_FromDouble() {
        Double dval = 3.75; // 3.75 seconds
        Duration d = NumberConversions.toDuration(dval, converter);
        assertEquals(Duration.ofSeconds(3, 750_000_000L), d);
    }

    @Test
    public void testToInstant() {
        long epoch = 1_600_000_000_123L;
        Instant instant = NumberConversions.toInstant(epoch, converter);
        assertEquals(Instant.ofEpochMilli(epoch), instant);
    }

    @Test
    public void testToSqlDate() {
        // Use epoch for 2021‑01‑01
        long epoch = LocalDate.of(2021, 1, 1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
        java.sql.Date sqlDate = NumberConversions.toSqlDate(epoch, converter);
        assertEquals(java.sql.Date.valueOf("2021-01-01"), sqlDate);
    }

    @Test
    public void testToTimestamp() {
        long epoch = 1_600_000_000_456L;
        Timestamp ts = NumberConversions.toTimestamp(epoch, converter);
        assertEquals(epoch, ts.getTime());
    }

    @Test
    public void testToCalendar() {
        long epoch = 1_600_000_000_000L;
        Calendar cal = NumberConversions.toCalendar(epoch, converter);
        assertEquals(epoch, cal.getTimeInMillis());
    }

    @Test
    public void testToLocalTime_Valid() {
        // 01:23:45.678 = 5_235_678 ms
        long millis = 5_235_678L;
        LocalTime lt = NumberConversions.toLocalTime(millis, converter);
        assertEquals(LocalTime.of(1, 23, 45, 678_000_000), lt);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTime_Invalid_Negative() {
        NumberConversions.toLocalTime(-1L, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTime_Invalid_TooLarge() {
        // One millisecond beyond the last millisecond of the day
        NumberConversions.toLocalTime(86_400_000L, converter);
    }

    @Test
    public void testToLocalDate() {
        // Epoch 0 is 1970‑01‑01 UTC; zone handling is via converter options
        LocalDate ld = NumberConversions.toLocalDate(0L, converter);
        // The zone may shift the date, so compare using the converter's zone
        ZoneId zone = converter.getOptions().getZoneId();
        LocalDate expected = Instant.ofEpochMilli(0L).atZone(zone).toLocalDate();
        assertEquals(expected, ld);
    }

    @Test
    public void testToLocalDateTime() {
        LocalDateTime ldt = NumberConversions.toLocalDateTime(0L, converter);
        ZoneId zone = converter.getOptions().getZoneId();
        LocalDateTime expected = Instant.ofEpochMilli(0L).atZone(zone).toLocalDateTime();
        assertEquals(expected, ldt);
    }

    @Test
    public void testToZonedDateTime() {
        ZonedDateTime zdt = NumberConversions.toZonedDateTime(0L, converter);
        ZoneId zone = converter.getOptions().getZoneId();
        ZonedDateTime expected = Instant.ofEpochMilli(0L).atZone(zone);
        assertEquals(expected, zdt);
    }

    @Test
    public void testToOffsetTime_FromLong() {
        long epoch = 1_600_000_000_000L;
        OffsetTime ot = NumberConversions.toOffsetTime(epoch, converter);
        ZoneId zone = converter.getOptions().getZoneId();
        OffsetTime expected = OffsetTime.ofInstant(Instant.ofEpochMilli(epoch), zone);
        assertEquals(expected, ot);
    }

    @Test
    public void testToOffsetTime_FromBigDecimal() {
        OffsetTime ot = NumberConversions.toOffsetTime(BigDecimal.valueOf(1_600_000_000_000L), converter);
        ZoneId zone = converter.getOptions().getZoneId();
        OffsetTime expected = OffsetTime.ofInstant(Instant.ofEpochMilli(1_600_000_000_000L), zone);
        assertEquals(expected, ot);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToOffsetTime_UnsupportedType() {
        NumberConversions.toOffsetTime("not a number", converter);
    }

    @Test
    public void testToOffsetDateTime() {
        OffsetDateTime odt = NumberConversions.toOffsetDateTime(0L, converter);
        ZoneId zone = converter.getOptions().getZoneId();
        OffsetDateTime expected = OffsetDateTime.ofInstant(Instant.ofEpochMilli(0L), zone);
        assertEquals(expected, odt);
    }

    @Test
    public void testToYear() {
        Year y = NumberConversions.toYear((short) 2025, converter);
        assertEquals(Year.of(2025), y);
    }

    /* -------------------------------------------------------------
     *  Additional edge‑case tests for numeric overflow/underflow
     * ------------------------------------------------------------- */

    @Test
    public void testToByte_Overflow() {
        // Java truncates overflow when casting to byte
        Byte b = NumberConversions.toByte(130, converter); // 130 -> -126
        assertEquals((byte) -126, b.byteValue());
    }

    @Test
    public void testToChar_MaxValue() {
        char max = NumberConversions.toCharacter((long) Character.MAX_VALUE, converter);
        assertEquals(Character.MAX_VALUE, max);
    }

    @Test
    public void testToLong_NullObject() {
        try {
            NumberConversions.toLong(null, converter);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            // Expected – the method casts to Number without a null check.
        }
    }

    @Test
    public void testFloatToString_NaN() {
        String s = NumberConversions.floatToString(Float.NaN, converter);
        assertEquals("NaN", s);
    }

    @Test
    public void testDoubleToString_Infinity() {
        String s = NumberConversions.doubleToString(Double.POSITIVE_INFINITY, converter);
        assertEquals("Infinity", s);
    }
}
