package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.DateTimeException;
import java.time.Year;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

/**
 * Tests for {@link YearConversions}.  The class is package‑private, therefore the
 * test class lives in the same package so that it can call the static methods directly.
 */
public class YearConversionsTest {

    // -----------------------------------------------------------------------
    // Helper – a dummy Converter (methods are never used by YearConversions)
    // -----------------------------------------------------------------------
    private static final Converter DUMMY_CONVERTER = new Converter(null);

    // -----------------------------------------------------------------------
    // Basic functionality tests
    // -----------------------------------------------------------------------
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
        Year y = Year.of(1980);
        assertEquals((short)1980, YearConversions.toShort(y, DUMMY_CONVERTER));
    }

    @Test
    public void testToAtomicInteger() {
        Year y = Year.of(2001);
        AtomicInteger ai = YearConversions.toAtomicInteger(y, DUMMY_CONVERTER);
        assertEquals(2001, ai.get());
    }

    @Test
    public void testToAtomicLong() {
        Year y = Year.of(2002);
        AtomicLong al = YearConversions.toAtomicLong(y, DUMMY_CONVERTER);
        assertEquals(2002L, al.get());
    }

    @Test
    public void testToDouble() {
        Year y = Year.of(2010);
        assertEquals(2010.0, YearConversions.toDouble(y, DUMMY_CONVERTER), 0.0);
    }

    @Test
    public void testToFloat() {
        Year y = Year.of(2011);
        assertEquals(2011.0f, YearConversions.toFloat(y, DUMMY_CONVERTER), 0.0f);
    }

    @Test
    public void testToAtomicBooleanTrue() {
        Year y = Year.of(2022);
        AtomicBoolean ab = YearConversions.toAtomicBoolean(y, DUMMY_CONVERTER);
        assertTrue(ab.get());
    }

    @Test
    public void testToAtomicBooleanFalseForYearZero() {
        Year y = Year.of(0);          // ISO‑8601 year zero
        AtomicBoolean ab = YearConversions.toAtomicBoolean(y, DUMMY_CONVERTER);
        assertFalse(ab.get());
    }

    @Test
    public void testToBigInteger() {
        Year y = Year.of(1234);
        BigInteger bi = YearConversions.toBigInteger(y, DUMMY_CONVERTER);
        assertEquals(BigInteger.valueOf(1234), bi);
    }

    @Test
    public void testToBigDecimal() {
        Year y = Year.of(5678);
        BigDecimal bd = YearConversions.toBigDecimal(y, DUMMY_CONVERTER);
        assertEquals(BigDecimal.valueOf(5678), bd);
    }

    @Test
    public void testToString() {
        Year y = Year.of(1995);
        assertEquals("1995", YearConversions.toString(y, DUMMY_CONVERTER));
    }

    @Test
    public void testToMap() {
        Year y = Year.of(2025);
        @SuppressWarnings("unchecked")
        Map<String, Integer> map = (Map<String, Integer>) YearConversions.toMap(y, DUMMY_CONVERTER);
        assertEquals(1, map.size());
        assertTrue(map.containsKey(MapConversions.YEAR));
        assertEquals(Integer.valueOf(2025), map.get(MapConversions.YEAR));
    }

    // -----------------------------------------------------------------------
    // Edge‑case / exception tests
    // -----------------------------------------------------------------------
    @Test(expected = NullPointerException.class)
    public void testToInt_NullInput() {
        YearConversions.toInt(null, DUMMY_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToLong_NullInput() {
        YearConversions.toLong(null, DUMMY_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToShort_NullInput() {
        YearConversions.toShort(null, DUMMY_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicInteger_NullInput() {
        YearConversions.toAtomicInteger(null, DUMMY_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLong_NullInput() {
        YearConversions.toAtomicLong(null, DUMMY_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToDouble_NullInput() {
        YearConversions.toDouble(null, DUMMY_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToFloat_NullInput() {
        YearConversions.toFloat(null, DUMMY_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicBoolean_NullInput() {
        YearConversions.toAtomicBoolean(null, DUMMY_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigInteger_NullInput() {
        YearConversions.toBigInteger(null, DUMMY_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimal_NullInput() {
        YearConversions.toBigDecimal(null, DUMMY_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToString_NullInput() {
        YearConversions.toString(null, DUMMY_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullInput() {
        YearConversions.toMap(null, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToInt_WrongType() {
        YearConversions.toInt(Integer.valueOf(2020), DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToLong_WrongType() {
        YearConversions.toLong("2020", DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicBoolean_WrongType() {
        YearConversions.toAtomicBoolean(new Object(), DUMMY_CONVERTER);
    }

    // Very large positive and negative years (within the allowed ISO‑8601 range)
    @Test
    public void testExtremeYears() {
        Year max = Year.of(999_999_999);
        Year min = Year.of(-999_999_999);

        assertEquals(999_999_999, YearConversions.toInt(max, DUMMY_CONVERTER));
        assertEquals(-999_999_999, YearConversions.toInt(min, DUMMY_CONVERTER));

        assertEquals(999_999_999L, YearConversions.toLong(max, DUMMY_CONVERTER));
        assertEquals(-999_999_999L, YearConversions.toLong(min, DUMMY_CONVERTER));

        assertEquals((short)999_999_999, YearConversions.toShort(max, DUMMY_CONVERTER));
        // short overflow is intentional – we just verify the cast behaviour
        assertEquals((short)-999_999_999, YearConversions.toShort(min, DUMMY_CONVERTER));
    }

    // Attempt to create a Year outside the supported range – should throw DateTimeException
    @Test(expected = DateTimeException.class)
    public void testYearOutOfRangeThrows() {
        Year.of(1_000_000_000); // outside the allowed range
    }
}
