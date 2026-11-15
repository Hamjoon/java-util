package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive JUnit‑4 test suite for {@code OffsetTimeConversions}.
 *
 * The tests cover:
 *   • Normal (typical) usage of every public static conversion method.
 *   • Edge‑case values such as extreme offsets, nanosecond precision,
 *     and values that overflow {@code int}.
 *   • Expected failure modes – {@code null} arguments and wrong source
 *     types (which result in {@code NullPointerException} or
 *     {@code ClassCastException} because the implementation casts directly
 *     to {@code OffsetTime} without checks).
 *
 * The {@code Converter} argument is not used by the implementation, so a
 * {@code null} reference is supplied where required.
 */
public class OffsetTimeConversionsTest {

    // -----------------------------------------------------------------------
    // Helper constants – used by many test methods
    // -----------------------------------------------------------------------
    private static final Converter DUMMY_CONVERTER = null; // not used by the class

    private static final OffsetTime MIDNIGHT_UTC =
            OffsetTime.of(0, 0, 0, 0, ZoneOffset.UTC);

    private static final OffsetTime NOON_PLUS_TWO =
            OffsetTime.of(12, 0, 0, 0, ZoneOffset.ofHours(2));

    private static final OffsetTime MAX_NANO_PLUS_EIGHT =
            OffsetTime.of(23, 59, 59, 999_999_999, ZoneOffset.ofHoursMinutes(8, 0));

    private static final OffsetTime MIN_OFFSET_MINUS_EIGHTEEN =
            OffsetTime.of(0, 0, 0, 0, ZoneOffset.ofHoursMinutes(-18, 0));

    // -----------------------------------------------------------------------
    // 1. toString
    // -----------------------------------------------------------------------
    @Test
    public void testToStringTypical() {
        String result = OffsetTimeConversions.toString(NOON_PLUS_TWO, DUMMY_CONVERTER);
        assertEquals("12:00:00+02:00", result);
    }

    @Test
    public void testToStringWithNanoseconds() {
        String result = OffsetTimeConversions.toString(MAX_NANO_PLUS_EIGHT, DUMMY_CONVERTER);
        assertEquals("23:59:59.999999999+08:00", result);
    }

    @Test(expected = NullPointerException.class)
    public void testToStringNullArgument() {
        OffsetTimeConversions.toString(null, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToStringWrongTypeArgument() {
        OffsetTimeConversions.toString("not an OffsetTime", DUMMY_CONVERTER);
    }

    // -----------------------------------------------------------------------
    // 2. toMap
    // -----------------------------------------------------------------------
    @Test
    public void testToMapTypical() {
        Map<String, Object> map = OffsetTimeConversions.toMap(NOON_PLUS_TWO, DUMMY_CONVERTER);
        assertNotNull(map);
        assertEquals(1, map.size());
        assertTrue(map.containsKey(MapConversions.OFFSET_TIME));
        assertEquals(NOON_PLUS_TWO.toString(), map.get(MapConversions.OFFSET_TIME));
    }

    @Test(expected = NullPointerException.class)
    public void testToMapNullArgument() {
        OffsetTimeConversions.toMap(null, DUMMY_CONVERTER);
    }

    // -----------------------------------------------------------------------
    // 3. toInteger
    // -----------------------------------------------------------------------
    @Test
    public void testToIntegerWithinIntRange() {
        int value = OffsetTimeConversions.toInteger(MIDNIGHT_UTC, DUMMY_CONVERTER);
        // midnight UTC on 1970‑01‑01 is epoch 0 milliseconds
        assertEquals(0, value);
    }

    @Test
    public void testToIntegerOverflow() {
        // 1970‑01‑01 23:59:59.999999999+18:00 is far beyond int range
        OffsetTime large = OffsetTime.of(23, 59, 59, 999_999_999, ZoneOffset.ofHours(18));
        int value = OffsetTimeConversions.toInteger(large, DUMMY_CONVERTER);
        // The method simply casts long to int, so we verify the truncation behaviour
        long expectedLong = OffsetTimeConversions.toLong(large, DUMMY_CONVERTER);
        assertEquals((int) expectedLong, value);
    }

    @Test(expected = NullPointerException.class)
    public void testToIntegerNullArgument() {
        OffsetTimeConversions.toInteger(null, DUMMY_CONVERTER);
    }

    // -----------------------------------------------------------------------
    // 4. toLong
    // -----------------------------------------------------------------------
    @Test
    public void testToLongMidnightUtc() {
        long epochMilli = OffsetTimeConversions.toLong(MIDNIGHT_UTC, DUMMY_CONVERTER);
        assertEquals(0L, epochMilli);
    }

    @Test
    public void testToLongWithPositiveOffset() {
        // 12:00+02:00 on 1970‑01‑01 => 10:00 UTC => 10 * 60 * 60 * 1000 = 36_000_000 ms
        long epochMilli = OffsetTimeConversions.toLong(NOON_PLUS_TWO, DUMMY_CONVERTER);
        assertEquals(36_000_000L, epochMilli);
    }

    @Test
    public void testToLongWithNegativeExtremeOffset() {
        // 00:00-18:00 => 18:00 UTC on previous day => -(18h) = -64_800_000 ms
        long epochMilli = OffsetTimeConversions.toLong(MIN_OFFSET_MINUS_EIGHTEEN, DUMMY_CONVERTER);
        assertEquals(-64_800_000L, epochMilli);
    }

    @Test(expected = NullPointerException.class)
    public void testToLongNullArgument() {
        OffsetTimeConversions.toLong(null, DUMMY_CONVERTER);
    }

    // -----------------------------------------------------------------------
    // 5. toDouble
    // -----------------------------------------------------------------------
    @Test
    public void testToDoubleMidnightUtc() {
        double d = OffsetTimeConversions.toDouble(MIDNIGHT_UTC, DUMMY_CONVERTER);
        assertEquals(0.0, d, 0.0);
    }

    @Test
    public void testToDoubleWithNanoseconds() {
        double d = OffsetTimeConversions.toDouble(MAX_NANO_PLUS_EIGHT, DUMMY_CONVERTER);
        // Expected seconds = 23:59:59 => 86_399 seconds
        // plus 0.999999999 seconds from nanoseconds
        double expected = 86_399.999_999_999;
        assertEquals(expected, d, 1e-12);
    }

    @Test(expected = NullPointerException.class)
    public void testToDoubleNullArgument() {
        OffsetTimeConversions.toDouble(null, DUMMY_CONVERTER);
    }

    // -----------------------------------------------------------------------
    // 6. toBigInteger
    // -----------------------------------------------------------------------
    @Test
    public void testToBigIntegerMidnightUtc() {
        BigInteger bi = OffsetTimeConversions.toBigInteger(MIDNIGHT_UTC, DUMMY_CONVERTER);
        assertEquals(BigInteger.ZERO, bi);
    }

    @Test
    public void testToBigIntegerWithNanoseconds() {
        BigInteger bi = OffsetTimeConversions.toBigInteger(MAX_NANO_PLUS_EIGHT, DUMMY_CONVERTER);
        // 86_399 seconds * 1_000_000_000 + 999_999_999 nanoseconds
        BigInteger expected = BigInteger.valueOf(86_399L)
                .multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(999_999_999L));
        assertEquals(expected, bi);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigIntegerNullArgument() {
        OffsetTimeConversions.toBigInteger(null, DUMMY_CONVERTER);
    }

    // -----------------------------------------------------------------------
    // 7. toBigDecimal
    // -----------------------------------------------------------------------
    @Test
    public void testToBigDecimalMidnightUtc() {
        BigDecimal bd = OffsetTimeConversions.toBigDecimal(MIDNIGHT_UTC, DUMMY_CONVERTER);
        assertEquals(BigDecimal.ZERO, bd);
    }

    @Test
    public void testToBigDecimalWithNanoseconds() {
        BigDecimal bd = OffsetTimeConversions.toBigDecimal(MAX_NANO_PLUS_EIGHT, DUMMY_CONVERTER);
        // 86_399 seconds + 0.999999999
        BigDecimal expected = new BigDecimal("86399.999999999");
        assertEquals(0, expected.compareTo(bd)); // compareTo == 0 means equal numerically
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimalNullArgument() {
        OffsetTimeConversions.toBigDecimal(null, DUMMY_CONVERTER);
    }

    // -----------------------------------------------------------------------
    // 8. toAtomicInteger
    // -----------------------------------------------------------------------
    @Test
    public void testToAtomicIntegerTypical() {
        AtomicInteger ai = OffsetTimeConversions.toAtomicInteger(NOON_PLUS_TWO, DUMMY_CONVERTER);
        assertEquals(36_000_000, ai.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicIntegerNullArgument() {
        OffsetTimeConversions.toAtomicInteger(null, DUMMY_CONVERTER);
    }

    // -----------------------------------------------------------------------
    // 9. toAtomicLong
    // -----------------------------------------------------------------------
    @Test
    public void testToAtomicLongTypical() {
        AtomicLong al = OffsetTimeConversions.toAtomicLong(NOON_PLUS_TWO, DUMMY_CONVERTER);
        assertEquals(36_000_000L, al.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLongNullArgument() {
        OffsetTimeConversions.toAtomicLong(null, DUMMY_CONVERTER);
    }

    // -----------------------------------------------------------------------
    // 10. Private helper getEpoch (indirectly tested via public methods)
    // -----------------------------------------------------------------------
    @Test
    public void testEpochConsistencyBetweenMethods() {
        // Verify that toLong and toDouble compute consistent epoch values
        long epochMilli = OffsetTimeConversions.toLong(NOON_PLUS_TWO, DUMMY_CONVERTER);
        double epochDouble = OffsetTimeConversions.toDouble(NOON_PLUS_TWO, DUMMY_CONVERTER);
        assertEquals(epochMilli / 1000.0, epochDouble, 1e-12);
    }

    // -----------------------------------------------------------------------
    // 11. Wrong source type – should throw ClassCastException for all methods
    // -----------------------------------------------------------------------
    @Test(expected = ClassCastException.class)
    public void testWrongSourceTypeToString() {
        OffsetTimeConversions.toString(12345, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testWrongSourceTypeToMap() {
        OffsetTimeConversions.toMap(12345, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testWrongSourceTypeToLong() {
        OffsetTimeConversions.toLong(12345, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testWrongSourceTypeToDouble() {
        OffsetTimeConversions.toDouble(12345, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testWrongSourceTypeToBigInteger() {
        OffsetTimeConversions.toBigInteger(12345, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testWrongSourceTypeToBigDecimal() {
        OffsetTimeConversions.toBigDecimal(12345, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testWrongSourceTypeToAtomicInteger() {
        OffsetTimeConversions.toAtomicInteger(12345, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testWrongSourceTypeToAtomicLong() {
        OffsetTimeConversions.toAtomicLong(12345, DUMMY_CONVERTER);
    }
}
