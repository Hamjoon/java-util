package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.*;

public class NumberConversionsTest {

    private Converter converter; // used for methods that need a Converter (zone handling)

    @Before
    public void setUp() {
        // Assuming ConverterOptions has a public no‑arg constructor and provides a default ZoneId.
        // If a different constructor is required, adjust accordingly.
        converter = new Converter(new ConverterOptions());
    }

    /* ---------- Primitive numeric conversions ---------- */

    @Test
    public void testToByte() {
        assertEquals((byte) 123, NumberConversions.toByte((byte) 123, converter));
        assertEquals((byte) -5, NumberConversions.toByte((short) -5, converter));
        assertEquals((byte) 0, NumberConversions.toByte(0, converter));
    }

    @Test
    public void testToByteZero() {
        assertEquals(Byte.valueOf((byte) 0), NumberConversions.toByteZero(null, converter));
    }

    @Test
    public void testToShort() {
        assertEquals((short) 32000, NumberConversions.toShort((short) 32000, converter));
        assertEquals((short) -123, NumberConversions.toShort(-123, converter));
    }

    @Test
    public void testToShortZero() {
        assertEquals(Short.valueOf((short) 0), NumberConversions.toShortZero(null, converter));
    }

    @Test
    public void testToInt() {
        assertEquals(42, NumberConversions.toInt(42, converter));
        assertEquals(-1, NumberConversions.toInt(-1L, converter));
    }

    @Test
    public void testToIntZero() {
        assertEquals(Integer.valueOf(0), NumberConversions.toIntZero(null, converter));
    }

    @Test
    public void testToLong() {
        assertEquals(123456789L, NumberConversions.toLong(123456789L, converter));
        assertEquals(-987654321L, NumberConversions.toLong(-987654321, converter));
    }

    @Test
    public void testToLongZero() {
        assertEquals(Long.valueOf(0L), NumberConversions.toLongZero(null, converter));
    }

    @Test
    public void testToFloat() {
        assertEquals(3.14f, NumberConversions.toFloat(3.14f, converter), 0.0f);
        assertEquals(-0.0f, NumberConversions.toFloat(-0.0, converter), 0.0f);
    }

    @Test
    public void testToFloatZero() {
        assertEquals(Float.valueOf(0f), NumberConversions.toFloatZero(null, converter));
    }

    @Test
    public void testFloatToString() {
        assertEquals("0", NumberConversions.floatToString(0f, converter));
        assertEquals("3.5", NumberConversions.floatToString(3.5f, converter));
        // Ensure that non‑zero but very small values are not converted to "0"
        assertEquals("1.0E-8", NumberConversions.floatToString(1.0e-8f, converter));
    }

    @Test
    public void testToDouble() {
        assertEquals(2.71828, NumberConversions.toDouble(2.71828, converter), 0.0);
    }

    @Test
    public void testToDoubleZero() {
        assertEquals(Double.valueOf(0d), NumberConversions.toDoubleZero(null, converter));
    }

    @Test
    public void testDoubleToString() {
        assertEquals("0", NumberConversions.doubleToString(0d, converter));
        assertEquals("1.23", NumberConversions.doubleToString(1.23, converter));
        assertEquals("1.0E-10", NumberConversions.doubleToString(1.0e-10, converter));
    }

    /* ---------- Conversions to big numbers ---------- */

    @Test
    public void testIntegerTypeToBigDecimal() {
        BigDecimal bd = NumberConversions.integerTypeToBigDecimal(12345L, converter);
        assertEquals(new BigDecimal("12345"), bd);
    }

    @Test
    public void testIntegerTypeToBigInteger() {
        BigInteger bi = NumberConversions.integerTypeToBigInteger(12345L, converter);
        assertEquals(BigInteger.valueOf(12345L), bi);
    }

    @Test
    public void testFloatingPointToBigDecimal() {
        BigDecimal bd = NumberConversions.floatingPointToBigDecimal(12.34, converter);
        assertEquals(BigDecimal.valueOf(12.34), bd);
    }

    @Test
    public void testFloatingPointToBigInteger() {
        // Positive number – floor is applied
        BigInteger biPos = NumberConversions.floatingPointToBigInteger(12.99, converter);
        assertEquals(BigInteger.valueOf(12), biPos);

        // Negative number – ceil is applied
        BigInteger biNeg = NumberConversions.floatingPointToBigInteger(-12.99, converter);
        assertEquals(BigInteger.valueOf(-12), biNeg);

        // Exact integer value
        BigInteger biExact = NumberConversions.floatingPointToBigInteger(5.0, converter);
        assertEquals(BigInteger.valueOf(5), biExact);
    }

    /* ---------- Atomic types ---------- */

    @Test
    public void testToAtomicLong() {
        AtomicLong al = NumberConversions.toAtomicLong(123L, converter);
        assertEquals(123L, al.get());
    }

    @Test
    public void testToAtomicInteger() {
        AtomicInteger ai = NumberConversions.toAtomicInteger(42, converter);
        assertEquals(42, ai.get());
    }

    @Test
    public void testToAtomicBoolean() {
        assertTrue(NumberConversions.toAtomicBoolean(1L, converter).get());
        assertFalse(NumberConversions.toAtomicBoolean(0L, converter).get());
        assertTrue(NumberConversions.toAtomicBoolean(-5L, converter).get()); // non‑zero => true
    }

    /* ---------- Zero‑checks ---------- */

    @Test
    public void testIsIntTypeNotZero() {
        assertTrue(NumberConversions.isIntTypeNotZero(5, converter));
        assertFalse(NumberConversions.isIntTypeNotZero(0L, converter));
    }

    @Test
    public void testIsFloatTypeNotZero() {
        assertTrue(NumberConversions.isFloatTypeNotZero(0.1f, converter));
        assertFalse(NumberConversions.isFloatTypeNotZero(0.0, converter));
    }

    @Test
    public void testIsBigIntegerNotZero() {
        assertTrue(NumberConversions.isBigIntegerNotZero(BigInteger.ONE, converter));
        assertFalse(NumberConversions.isBigIntegerNotZero(BigInteger.ZERO, converter));
    }

    @Test
    public void testIsBigDecimalNotZero() {
        assertTrue(NumberConversions.isBigDecimalNotZero(new BigDecimal("0.01"), converter));
        assertFalse(NumberConversions.isBigDecimalNotZero(BigDecimal.ZERO, converter));
    }

    /* ---------- Character conversion ---------- */

    @Test
    public void testToCharacterWithinRange() {
        char c = NumberConversions.toCharacter(65L, converter); // 'A'
        assertEquals('A', c);
    }

    @Test
    public void testToCharacterZero() {
        assertEquals('\u0000', NumberConversions.toCharacter(0L, converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToCharacterOutOfRange() {
        // Character.MAX_VALUE == 65535, use a larger number to trigger exception
        NumberConversions.toCharacter(70000L, converter);
    }

    /* ---------- Date / Time conversions ---------- */

    @Test
    public void testToDate() {
        long epoch = 1_600_000_000_000L; // arbitrary epoch ms
        Date d = NumberConversions.toDate(epoch, converter);
        assertEquals(new Date(epoch), d);
    }

    @Test
    public void testToDuration() {
        Duration dur = NumberConversions.toDuration(5000L, converter);
        assertEquals(Duration.ofMillis(5000L), dur);
    }

    @Test
    public void testToInstant() {
        long epoch = 1_500_000_000_000L;
        Instant instant = NumberConversions.toInstant(epoch, converter);
        assertEquals(Instant.ofEpochMilli(epoch), instant);
    }

    @Test
    public void testToSqlDate() {
        long epoch = 1_600_000_000_000L;
        java.sql.Date sqlDate = NumberConversions.toSqlDate(epoch, converter);
        assertEquals(new java.sql.Date(epoch), sqlDate);
    }

    @Test
    public void testToTimestamp() {
        long epoch = 1_600_000_000_000L;
        Timestamp ts = NumberConversions.toTimestamp(epoch, converter);
        assertEquals(new Timestamp(epoch), ts);
    }

    @Test
    public void testToCalendar() {
        long epoch = 1_600_000_000_000L;
        Calendar cal = NumberConversions.toCalendar(epoch, converter);
        assertEquals(epoch, cal.getTimeInMillis());
    }

    @Test
    public void testToZonedDateTime() {
        long epoch = 1_600_000_000_000L;
        ZonedDateTime zdt = NumberConversions.toZonedDateTime(epoch, converter);
        assertEquals(Instant.ofEpochMilli(epoch), zdt.toInstant());
        assertEquals(converter.getOptions().getZoneId(), zdt.getZone());
    }

    @Test
    public void testToLocalDate() {
        long epoch = 1_600_000_000_000L; // corresponds to 2020‑09‑13 (UTC)
        LocalDate ld = NumberConversions.toLocalDate(epoch, converter);
        assertEquals(LocalDate.ofInstant(Instant.ofEpochMilli(epoch), converter.getOptions().getZoneId()), ld);
    }

    @Test
    public void testToLocalDateTime() {
        long epoch = 1_600_000_000_000L;
        LocalDateTime ldt = NumberConversions.toLocalDateTime(epoch, converter);
        assertEquals(LocalDateTime.ofInstant(Instant.ofEpochMilli(epoch), converter.getOptions().getZoneId()), ldt);
    }

    @Test
    public void testToOffsetDateTime() {
        long epoch = 1_600_000_000_000L;
        OffsetDateTime odt = NumberConversions.toOffsetDateTime(epoch, converter);
        assertEquals(OffsetDateTime.ofInstant(Instant.ofEpochMilli(epoch), converter.getOptions().getZoneId()), odt);
    }

    /* ---------- Year conversion ---------- */

    @Test
    public void testToYear() {
        short yearShort = 2023;
        Year y = NumberConversions.toYear(yearShort, converter);
        assertEquals(Year.of(2023), y);
    }

    /* ---------- Additional edge cases ---------- */

    @Test
    public void testFloatingPointToBigIntegerRoundingEdge() {
        // value exactly halfway between two integers – format uses floor for positive numbers
        BigInteger bi = NumberConversions.floatingPointToBigInteger(2.5, converter);
        assertEquals(BigInteger.valueOf(2), bi);
    }

    @Test
    public void testFloatToStringZeroEdge() {
        // -0.0f should still be treated as zero
        assertEquals("0", NumberConversions.floatToString(-0.0f, converter));
    }

    @Test
    public void testDoubleToStringZeroEdge() {
        // -0.0d should still be treated as zero
        assertEquals("0", NumberConversions.doubleToString(-0.0d, converter));
    }
}
