package com.cedarsoftware.util.convert;

import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Year;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.cedarsoftware.util.convert.MapConversions.YEAR;
import static org.junit.Assert.*;

public class YearConversionsTest {

    // Helper method – the Converter argument is not used by YearConversions,
    // therefore we can safely pass null.
    private static final Converter DUMMY_CONVERTER = null;

    @Test
    public void testToInt_basicYear() {
        Year y = Year.of(2023);
        int result = YearConversions.toInt(y, DUMMY_CONVERTER);
        assertEquals(2023, result);
    }

    @Test
    public void testToInt_negativeYear() {
        Year y = Year.of(-1);
        int result = YearConversions.toInt(y, DUMMY_CONVERTER);
        assertEquals(-1, result);
    }

    @Test
    public void testToLong_delegatesToInt() {
        Year y = Year.of(1999);
        long result = YearConversions.toLong(y, DUMMY_CONVERTER);
        assertEquals(1999L, result);
    }

    @Test
    public void testToShort_withinRange() {
        Year y = Year.of(123);
        short result = YearConversions.toShort(y, DUMMY_CONVERTER);
        assertEquals(123, result);
    }

    @Test
    public void testToShort_overflow() {
        // 40000 exceeds Short.MAX_VALUE (32767) – casting should wrap.
        Year y = Year.of(40000);
        short result = YearConversions.toShort(y, DUMMY_CONVERTER);
        short expected = (short) 40000; // Java's truncating cast
        assertEquals(expected, result);
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
        Year y = Year.of(1975);
        double d = YearConversions.toDouble(y, DUMMY_CONVERTER);
        assertEquals(1975.0, d, 0.0);
    }

    @Test
    public void testToFloat() {
        Year y = Year.of(1975);
        float f = YearConversions.toFloat(y, DUMMY_CONVERTER);
        assertEquals(1975.0f, f, 0.0f);
    }

    @Test
    public void testToAtomicBoolean_trueForNonZero() {
        Year y = Year.of(1995);
        AtomicBoolean ab = YearConversions.toAtomicBoolean(y, DUMMY_CONVERTER);
        assertTrue(ab.get());
    }

    @Test
    public void testToAtomicBoolean_falseForZero() {
        Year y = Year.of(0);
        AtomicBoolean ab = YearConversions.toAtomicBoolean(y, DUMMY_CONVERTER);
        assertFalse(ab.get());
    }

    @Test
    public void testToBigInteger() {
        Year y = Year.of(2021);
        BigInteger bi = YearConversions.toBigInteger(y, DUMMY_CONVERTER);
        assertEquals(BigInteger.valueOf(2021), bi);
    }

    @Test
    public void testToBigDecimal() {
        Year y = Year.of(2021);
        BigDecimal bd = YearConversions.toBigDecimal(y, DUMMY_CONVERTER);
        assertEquals(BigDecimal.valueOf(2021), bd);
    }

    @Test
    public void testToString_standardYear() {
        Year y = Year.of(2022);
        String s = YearConversions.toString(y, DUMMY_CONVERTER);
        assertEquals("2022", s);
    }

    @Test
    public void testToString_negativeYear() {
        Year y = Year.of(-5);
        String s = YearConversions.toString(y, DUMMY_CONVERTER);
        // Year.toString() follows ISO‑8601, e.g., "-0005"
        assertEquals("-0005", s);
    }

    @Test
    public void testToMap_containsYearKeyAndValue() {
        Year y = Year.of(2018);
        Map<?, ?> map = YearConversions.toMap(y, DUMMY_CONVERTER);
        assertNotNull(map);
        assertTrue(map.containsKey(YEAR));
        assertEquals(Integer.valueOf(2018), map.get(YEAR));
        // Ensure the map is a LinkedHashMap preserving insertion order (single entry)
        assertEquals(1, map.size());
    }

    @Test
    public void testAllConversionsConsistent() {
        // Verify that every conversion method ultimately yields the same integer value.
        Year y = Year.of(1234);
        int expected = 1234;

        assertEquals(expected, YearConversions.toInt(y, DUMMY_CONVERTER));
        assertEquals(expected, YearConversions.toLong(y, DUMMY_CONVERTER));
        assertEquals((short) expected, YearConversions.toShort(y, DUMMY_CONVERTER));
        assertEquals(expected, YearConversions.toAtomicInteger(y, DUMMY_CONVERTER).get());
        assertEquals((long) expected, YearConversions.toAtomicLong(y, DUMMY_CONVERTER).get());
        assertEquals((double) expected, YearConversions.toDouble(y, DUMMY_CONVERTER), 0.0);
        assertEquals((float) expected, YearConversions.toFloat(y, DUMMY_CONVERTER), 0.0f);
        assertEquals(Boolean.TRUE, YearConversions.toAtomicBoolean(y, DUMMY_CONVERTER).get());
        assertEquals(BigInteger.valueOf(expected), YearConversions.toBigInteger(y, DUMMY_CONVERTER));
        assertEquals(BigDecimal.valueOf(expected), YearConversions.toBigDecimal(y, DUMMY_CONVERTER));
        assertEquals(y.toString(), YearConversions.toString(y, DUMMY_CONVERTER));
        Map<?, ?> map = YearConversions.toMap(y, DUMMY_CONVERTER);
        assertEquals(Integer.valueOf(expected), map.get(YEAR));
    }
}
