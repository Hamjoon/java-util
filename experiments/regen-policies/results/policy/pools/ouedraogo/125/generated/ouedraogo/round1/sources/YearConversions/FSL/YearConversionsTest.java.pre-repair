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
 */
public class YearConversionsTest {

    // A dummy Converter – the implementation is not used by YearConversions,
    // but the API requires a non‑null instance.
    private static final Converter DUMMY_CONVERTER = new Converter(null);

    @Test
    public void testToInt() {
        Year y = Year.of(2023);
        assertEquals(2023, YearConversions.toInt(y, DUMMY_CONVERTER));
    }

    @Test
    public void testToLong() {
        Year y = Year.of(1999);
        long expected = 1999L;
        assertEquals(expected, YearConversions.toLong(y, DUMMY_CONVERTER));
    }

    @Test
    public void testToShortWithinRange() {
        Year y = Year.of(12345);          // fits into a short
        short expected = 12345;
        assertEquals(expected, YearConversions.toShort(y, DUMMY_CONVERTER));
    }

    @Test
    public void testToShortOverflow() {
        Year y = Year.of(40000);          // exceeds Short.MAX_VALUE (32767)
        // (short)40000 == 40000 - 65536 = -25536
        short expected = (short) 40000;
        assertEquals(expected, YearConversions.toShort(y, DUMMY_CONVERTER));
    }

    @Test
    public void testToAtomicInteger() {
        Year y = Year.of(2020);
        AtomicInteger ai = YearConversions.toAtomicInteger(y, DUMMY_CONVERTER);
        assertEquals(2020, ai.get());
    }

    @Test
    public void testToAtomicLong() {
        Year y = Year.of(2020);
        AtomicLong al = YearConversions.toAtomicLong(y, DUMMY_CONVERTER);
        assertEquals(2020L, al.get());
    }

    @Test
    public void testToDouble() {
        Year y = Year.of(2015);
        double d = YearConversions.toDouble(y, DUMMY_CONVERTER);
        assertEquals(2015.0, d, 0.0);
    }

    @Test
    public void testToFloat() {
        Year y = Year.of(2015);
        float f = YearConversions.toFloat(y, DUMMY_CONVERTER);
        assertEquals(2015.0f, f, 0.0f);
    }

    @Test
    public void testToAtomicBooleanTrue() {
        Year y = Year.of(2021);   // non‑zero year => true
        AtomicBoolean ab = YearConversions.toAtomicBoolean(y, DUMMY_CONVERTER);
        assertTrue(ab.get());
    }

    @Test
    public void testToAtomicBooleanFalse() {
        Year y = Year.of(0);      // zero year => false
        AtomicBoolean ab = YearConversions.toAtomicBoolean(y, DUMMY_CONVERTER);
        assertFalse(ab.get());
    }

    @Test
    public void testToBigInteger() {
        Year y = Year.of(1984);
        BigInteger bi = YearConversions.toBigInteger(y, DUMMY_CONVERTER);
        assertEquals(BigInteger.valueOf(1984), bi);
    }

    @Test
    public void testToBigDecimal() {
        Year y = Year.of(1984);
        BigDecimal bd = YearConversions.toBigDecimal(y, DUMMY_CONVERTER);
        assertEquals(BigDecimal.valueOf(1984), bd);
    }

    @Test
    public void testToString() {
        Year y = Year.of(2022);
        assertEquals("2022", YearConversions.toString(y, DUMMY_CONVERTER));
    }

    @Test
    public void testToMap() {
        Year y = Year.of(2025);
        Map<?, ?> map = YearConversions.toMap(y, DUMMY_CONVERTER);

        // The map should contain exactly one entry: MapConversions.YEAR -> 2025
        assertEquals(1, map.size());
        assertTrue(map.containsKey(MapConversions.YEAR));
        assertEquals(Integer.valueOf(2025), map.get(MapConversions.YEAR));
    }

    @Test(expected = NullPointerException.class)
    public void testNullInputThrowsNPE() {
        // All conversion methods assume a non‑null Year; passing null should NPE.
        YearConversions.toInt(null, DUMMY_CONVERTER);
    }
}
