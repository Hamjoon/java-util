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

    // Helper to obtain a dummy Converter – the implementation never uses it
    private static final Converter DUMMY_CONVERTER = new Converter(null);

    @Test
    public void testToInt() {
        Year y2021 = Year.of(2021);
        assertEquals(2021, YearConversions.toInt(y2021, DUMMY_CONVERTER));

        Year yZero = Year.of(0);
        assertEquals(0, YearConversions.toInt(yZero, DUMMY_CONVERTER));

        Year yNegative = Year.of(-123);
        assertEquals(-123, YearConversions.toInt(yNegative, DUMMY_CONVERTER));
    }

    @Test
    public void testToLong() {
        Year y1999 = Year.of(1999);
        long expected = 1999L;
        assertEquals(expected, YearConversions.toLong(y1999, DUMMY_CONVERTER));
    }

    @Test
    public void testToShort() {
        Year y40000 = Year.of(40000);
        short expected = (short) 40000;
        assertEquals(expected, YearConversions.toShort(y40000, DUMMY_CONVERTER));

        Year yMinus32768 = Year.of(-32768);
        assertEquals((short) -32768, YearConversions.toShort(yMinus32768, DUMMY_CONVERTER));
    }

    @Test
    public void testToAtomicInteger() {
        Year y2022 = Year.of(2022);
        AtomicInteger ai = YearConversions.toAtomicInteger(y2022, DUMMY_CONVERTER);
        assertEquals(2022, ai.get());
    }

    @Test
    public void testToAtomicLong() {
        Year y2022 = Year.of(2022);
        AtomicLong al = YearConversions.toAtomicLong(y2022, DUMMY_CONVERTER);
        assertEquals(2022L, al.get());
    }

    @Test
    public void testToDouble() {
        Year y2022 = Year.of(2022);
        double d = YearConversions.toDouble(y2022, DUMMY_CONVERTER);
        assertEquals(2022.0, d, 0.0);
    }

    @Test
    public void testToFloat() {
        Year y2022 = Year.of(2022);
        float f = YearConversions.toFloat(y2022, DUMMY_CONVERTER);
        assertEquals(2022.0f, f, 0.0f);
    }

    @Test
    public void testToAtomicBoolean() {
        Year yNonZero = Year.of(2022);
        AtomicBoolean abTrue = YearConversions.toAtomicBoolean(yNonZero, DUMMY_CONVERTER);
        assertTrue(abTrue.get());

        Year yZero = Year.of(0);
        AtomicBoolean abFalse = YearConversions.toAtomicBoolean(yZero, DUMMY_CONVERTER);
        assertFalse(abFalse.get());
    }

    @Test
    public void testToBigInteger() {
        Year y2022 = Year.of(2022);
        BigInteger bi = YearConversions.toBigInteger(y2022, DUMMY_CONVERTER);
        assertEquals(BigInteger.valueOf(2022), bi);
    }

    @Test
    public void testToBigDecimal() {
        Year y2022 = Year.of(2022);
        BigDecimal bd = YearConversions.toBigDecimal(y2022, DUMMY_CONVERTER);
        assertEquals(BigDecimal.valueOf(2022), bd);
    }

    @Test
    public void testToString() {
        Year y2022 = Year.of(2022);
        assertEquals("2022", YearConversions.toString(y2022, DUMMY_CONVERTER));

        Year yNegative = Year.of(-5);
        assertEquals("-5", YearConversions.toString(yNegative, DUMMY_CONVERTER));
    }

    @Test
    public void testToMap() {
        Year y2022 = Year.of(2022);
        Map<?, ?> map = YearConversions.toMap(y2022, DUMMY_CONVERTER);
        assertNotNull(map);
        assertEquals(1, map.size());
        assertTrue(map.containsKey(YEAR));
        assertEquals(Integer.valueOf(2022), map.get(YEAR));
    }

    @Test(expected = ClassCastException.class)
    public void testInvalidInputThrowsClassCastException() {
        // Passing an object that is not a Year should result in a ClassCastException
        YearConversions.toInt("not-a-year", DUMMY_CONVERTER);
    }
}
