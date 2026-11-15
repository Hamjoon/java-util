package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Year;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive JUnit‑4 test suite for {@link YearConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Typical successful conversions for all supported target types.</li>
 *   <li>Boundary / overflow behaviour (e.g. {@code short} casting).</li>
 *   <li>Null source handling – every method should throw {@link NullPointerException}.</li>
 *   <li>Invalid source type handling – every method should throw {@link ClassCastException}.</li>
 *   <li>Specific logic for {@code AtomicBoolean} (zero vs non‑zero year).</li>
 *   <li>Correct map representation produced by {@code toMap}.</li>
 * </ul>
 *
 * The {@code Converter} argument is not used by the implementation, therefore a {@code null}
 * reference is passed in all cases.  This is safe because the methods never dereference it.
 */
public class YearConversionsTest {

    // -------------------------------------------------------------------------
    // Helper constants
    // -------------------------------------------------------------------------

    private static final Converter NULL_CONVERTER = null; // not used by YearConversions

    // -------------------------------------------------------------------------
    // Normal conversion tests
    // -------------------------------------------------------------------------

    @Test
    public void testToIntTypical() {
        Year y = Year.of(2023);
        int result = YearConversions.toInt(y, NULL_CONVERTER);
        assertEquals(2023, result);
    }

    @Test
    public void testToLongTypical() {
        Year y = Year.of(1999);
        long result = YearConversions.toLong(y, NULL_CONVERTER);
        assertEquals(1999L, result);
    }

    @Test
    public void testToShortTypicalWithinRange() {
        Year y = Year.of(12345);
        short result = YearConversions.toShort(y, NULL_CONVERTER);
        assertEquals((short)12345, result);
    }

    @Test
    public void testToShortOverflow() {
        // 40000 does not fit into a signed 16‑bit short → wraps around
        Year y = Year.of(40_000);
        short result = YearConversions.toShort(y, NULL_CONVERTER);
        assertEquals((short)40_000, result); // expected wrap‑around value
    }

    @Test
    public void testToAtomicInteger() {
        Year y = Year.of(-7);
        AtomicInteger ai = YearConversions.toAtomicInteger(y, NULL_CONVERTER);
        assertEquals(-7, ai.get());
    }

    @Test
    public void testToAtomicLong() {
        Year y = Year.of(0);
        AtomicLong al = YearConversions.toAtomicLong(y, NULL_CONVERTER);
        assertEquals(0L, al.get());
    }

    @Test
    public void testToDouble() {
        Year y = Year.of(2022);
        double d = YearConversions.toDouble(y, NULL_CONVERTER);
        assertEquals(2022.0, d, 0.0);
    }

    @Test
    public void testToFloat() {
        Year y = Year.of(-123);
        float f = YearConversions.toFloat(y, NULL_CONVERTER);
        assertEquals(-123.0f, f, 0.0f);
    }

    @Test
    public void testToAtomicBooleanTrue() {
        Year y = Year.of(1);
        AtomicBoolean ab = YearConversions.toAtomicBoolean(y, NULL_CONVERTER);
        assertTrue(ab.get());
    }

    @Test
    public void testToAtomicBooleanFalse() {
        Year y = Year.of(0);
        AtomicBoolean ab = YearConversions.toAtomicBoolean(y, NULL_CONVERTER);
        assertFalse(ab.get());
    }

    @Test
    public void testToBigInteger() {
        Year y = Year.of(987654321);
        BigInteger bi = YearConversions.toBigInteger(y, NULL_CONVERTER);
        assertEquals(BigInteger.valueOf(987654321L), bi);
    }

    @Test
    public void testToBigDecimal() {
        Year y = Year.of(-42);
        BigDecimal bd = YearConversions.toBigDecimal(y, NULL_CONVERTER);
        assertEquals(BigDecimal.valueOf(-42L), bd);
    }

    @Test
    public void testToString() {
        Year y = Year.of(2020);
        String s = YearConversions.toString(y, NULL_CONVERTER);
        assertEquals("2020", s);
    }

    @Test
    public void testToMap() {
        Year y = Year.of(1984);
        Map<?, ?> map = YearConversions.toMap(y, NULL_CONVERTER);
        assertTrue(map instanceof Map);
        // YEAR is imported statically from MapConversions
        assertEquals(1, map.size());
        assertTrue(map.containsKey(YEAR));
        assertEquals(Integer.valueOf(1984), map.get(YEAR));
    }

    // -------------------------------------------------------------------------
    // Null source handling – every method should throw NullPointerException
    // -------------------------------------------------------------------------

    @Test(expected = NullPointerException.class)
    public void testToIntNullSource() {
        YearConversions.toInt(null, NULL_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToLongNullSource() {
        YearConversions.toLong(null, NULL_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToShortNullSource() {
        YearConversions.toShort(null, NULL_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicIntegerNullSource() {
        YearConversions.toAtomicInteger(null, NULL_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLongNullSource() {
        YearConversions.toAtomicLong(null, NULL_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToDoubleNullSource() {
        YearConversions.toDouble(null, NULL_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToFloatNullSource() {
        YearConversions.toFloat(null, NULL_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicBooleanNullSource() {
        YearConversions.toAtomicBoolean(null, NULL_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigIntegerNullSource() {
        YearConversions.toBigInteger(null, NULL_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimalNullSource() {
        YearConversions.toBigDecimal(null, NULL_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToStringNullSource() {
        YearConversions.toString(null, NULL_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToMapNullSource() {
        YearConversions.toMap(null, NULL_CONVERTER);
    }

    // -------------------------------------------------------------------------
    // Invalid source type handling – every method should throw ClassCastException
    // -------------------------------------------------------------------------

    @Test(expected = ClassCastException.class)
    public void testToIntWrongType() {
        YearConversions.toInt(Integer.valueOf(2020), NULL_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToLongWrongType() {
        YearConversions.toLong("2020", NULL_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToShortWrongType() {
        YearConversions.toShort(Boolean.TRUE, NULL_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicIntegerWrongType() {
        YearConversions.toAtomicInteger(new Object(), NULL_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicLongWrongType() {
        YearConversions.toAtomicLong(new java.util.Date(), NULL_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToDoubleWrongType() {
        YearConversions.toDouble(3.14, NULL_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToFloatWrongType() {
        YearConversions.toFloat(BigDecimal.TEN, NULL_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicBooleanWrongType() {
        YearConversions.toAtomicBoolean(Short.valueOf((short)1), NULL_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToBigIntegerWrongType() {
        YearConversions.toBigInteger(123L, NULL_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToBigDecimalWrongType() {
        YearConversions.toBigDecimal(new java.math.MathContext(5), NULL_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToStringWrongType() {
        YearConversions.toString(Boolean.FALSE, NULL_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToMapWrongType() {
        YearConversions.toMap(Double.valueOf(1.0), NULL_CONVERTER);
    }
}
