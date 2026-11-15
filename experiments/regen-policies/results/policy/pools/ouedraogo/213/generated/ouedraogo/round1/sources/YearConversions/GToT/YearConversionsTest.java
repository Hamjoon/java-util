package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Year;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

/**
 * Unit tests for {@link YearConversions}.
 *
 * The class under test has package‑private static methods, therefore the test
 * class lives in the same package so it can call them directly.
 *
 * The {@code Converter} argument is not used by any method – we simply pass
 * {@code null} to keep the tests focused on the conversion logic.
 */
public class YearConversionsTest {

    /* --------------------------------------------------------------------- */
    /* Helper – a dummy Converter (not required for the current logic)      */
    /* --------------------------------------------------------------------- */
    private static final Converter DUMMY_CONVERTER = null;

    /* --------------------------------------------------------------------- */
    /* 1. Basic functional tests                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToInt() {
        Year y = Year.of(2023);
        assertEquals(2023, YearConversions.toInt(y, DUMMY_CONVERTER));
    }

    @Test
    public void testToLong() {
        Year y = Year.of(1999);
        assertEquals(1999L, YearConversions.toLong(y, DUMMY_CONVERTER));
    }

    @Test
    public void testToShort() {
        Year y = Year.of(123);
        assertEquals((short) 123, YearConversions.toShort(y, DUMMY_CONVERTER));
    }

    @Test
    public void testToAtomicInteger() {
        Year y = Year.of(2020);
        AtomicInteger ai = YearConversions.toAtomicInteger(y, DUMMY_CONVERTER);
        assertNotNull(ai);
        assertEquals(2020, ai.get());
    }

    @Test
    public void testToAtomicLong() {
        Year y = Year.of(2020);
        AtomicLong al = YearConversions.toAtomicLong(y, DUMMY_CONVERTER);
        assertNotNull(al);
        assertEquals(2020L, al.get());
    }

    @Test
    public void testToDouble() {
        Year y = Year.of(1985);
        assertEquals(1985.0, YearConversions.toDouble(y, DUMMY_CONVERTER), 0.0);
    }

    @Test
    public void testToFloat() {
        Year y = Year.of(1985);
        assertEquals(1985.0f, YearConversions.toFloat(y, DUMMY_CONVERTER), 0.0f);
    }

    @Test
    public void testToAtomicBoolean() {
        Year nonZero = Year.of(2021);
        Year zero = Year.of(0);
        AtomicBoolean abTrue = YearConversions.toAtomicBoolean(nonZero, DUMMY_CONVERTER);
        AtomicBoolean abFalse = YearConversions.toAtomicBoolean(zero, DUMMY_CONVERTER);
        assertTrue(abTrue.get());
        assertFalse(abFalse.get());
    }

    @Test
    public void testToBigInteger() {
        Year y = Year.of(123456);
        BigInteger bi = YearConversions.toBigInteger(y, DUMMY_CONVERTER);
        assertEquals(BigInteger.valueOf(123456), bi);
    }

    @Test
    public void testToBigDecimal() {
        Year y = Year.of(654321);
        BigDecimal bd = YearConversions.toBigDecimal(y, DUMMY_CONVERTER);
        assertEquals(BigDecimal.valueOf(654321), bd);
    }

    @Test
    public void testToString() {
        Year y = Year.of(2022);
        assertEquals("2022", YearConversions.toString(y, DUMMY_CONVERTER));
    }

    @Test
    public void testToMap() {
        Year y = Year.of(1975);
        @SuppressWarnings("unchecked")
        Map<String, Integer> map = (Map<String, Integer>) YearConversions.toMap(y, DUMMY_CONVERTER);
        assertNotNull(map);
        assertEquals(1, map.size());
        assertTrue(map.containsKey(MapConversions.YEAR));
        assertEquals(Integer.valueOf(1975), map.get(MapConversions.YEAR));
    }

    /* --------------------------------------------------------------------- */
    /* 2. Edge‑case tests                                                   */
    /* --------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testToInt_NullInput() {
        YearConversions.toInt(null, DUMMY_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToLong_NullInput() {
        YearConversions.toLong(null, DUMMY_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullInput() {
        YearConversions.toMap(null, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToInt_WrongType() {
        YearConversions.toInt(Integer.valueOf(2020), DUMMY_CONVERTER);
    }

    @Test
    public void testExtremePositiveYear() {
        Year max = Year.of(999_999_999);
        assertEquals(999_999_999, YearConversions.toInt(max, DUMMY_CONVERTER));
        assertEquals(999_999_999L, YearConversions.toLong(max, DUMMY_CONVERTER));
        assertEquals(BigInteger.valueOf(999_999_999), YearConversions.toBigInteger(max, DUMMY_CONVERTER));
    }

    @Test
    public void testExtremeNegativeYear() {
        Year min = Year.of(-999_999_999);
        assertEquals(-999_999_999, YearConversions.toInt(min, DUMMY_CONVERTER));
        assertEquals(-999_999_999L, YearConversions.toLong(min, DUMMY_CONVERTER));
        assertEquals(BigInteger.valueOf(-999_999_999), YearConversions.toBigInteger(min, DUMMY_CONVERTER));
    }

    @Test
    public void testZeroYearBoolean() {
        Year zero = Year.of(0);
        AtomicBoolean ab = YearConversions.toAtomicBoolean(zero, DUMMY_CONVERTER);
        assertFalse(ab.get());
    }

    @Test
    public void testMapKeyConstant() {
        Year y = Year.of(2000);
        @SuppressWarnings("unchecked")
        Map<String, Integer> map = (Map<String, Integer>) YearConversions.toMap(y, DUMMY_CONVERTER);
        assertTrue(map.containsKey(MapConversions.YEAR));
    }
}
