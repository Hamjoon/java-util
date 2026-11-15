package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Year;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link YearConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Typical conversions for a normal year (e.g., 2023).</li>
 *   <li>Edge values: the minimum and maximum {@link Year} supported by the JDK.</li>
 *   <li>Null source – expecting {@link NullPointerException}.</li>
 *   <li>Wrong source type – expecting {@link ClassCastException}.</li>
 *   <li>Verification of the map representation (key defined in {@link MapConversions#YEAR}).</li>
 * </ul>
 *
 * The {@code Converter} argument is not used by the implementation, therefore a {@code null}
 * reference is sufficient for all calls.
 */
public class YearConversionsTest {

    // a dummy converter – not used by the YearConversions methods
    private Converter dummyConverter;

    @Before
    public void setUp() {
        // The Converter constructor requires a ConverterOptions instance.
        // The actual options are irrelevant for the static conversion methods,
        // so we can safely pass null (the constructor tolerates it) or create a minimal stub.
        dummyConverter = new Converter(null);
    }

    /* -------------------------------------------------------------
     *  Helper utilities
     * ------------------------------------------------------------- */

    private static void assertMapContainsYear(Map<?,?> map, int expectedValue) {
        assertNotNull("Map must not be null", map);
        assertTrue("Map must contain key YEAR", map.containsKey(YEAR));
        assertEquals("Map value for YEAR must match the year value",
                expectedValue, map.get(YEAR));
    }

    /* -------------------------------------------------------------
     *  Tests for toInt / toLong / toShort
     * ------------------------------------------------------------- */

    @Test
    public void testToIntTypical() {
        Year y = Year.of(2023);
        int result = YearConversions.toInt(y, dummyConverter);
        assertEquals(2023, result);
    }

    @Test
    public void testToIntMinValue() {
        Year y = Year.of(Year.MIN_VALUE);
        int result = YearConversions.toInt(y, dummyConverter);
        assertEquals(Year.MIN_VALUE, result);
    }

    @Test
    public void testToIntMaxValue() {
        Year y = Year.of(Year.MAX_VALUE);
        int result = YearConversions.toInt(y, dummyConverter);
        assertEquals(Year.MAX_VALUE, result);
    }

    @Test(expected = NullPointerException.class)
    public void testToIntNullSource() {
        YearConversions.toInt(null, dummyConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToIntWrongType() {
        YearConversions.toInt(Integer.valueOf(2023), dummyConverter);
    }

    @Test
    public void testToLongDelegatesToInt() {
        Year y = Year.of(1999);
        long result = YearConversions.toLong(y, dummyConverter);
        assertEquals(1999L, result);
    }

    @Test
    public void testToShortTruncatesCorrectly() {
        Year y = Year.of(32767); // max value for short
        short result = YearConversions.toShort(y, dummyConverter);
        assertEquals((short)32767, result);
    }

    @Test
    public void testToShortOverflow() {
        // Overflow is defined by Java casting rules (wrap‑around)
        Year y = Year.of(32768);
        short result = YearConversions.toShort(y, dummyConverter);
        assertEquals((short) -32768, result);
    }

    /* -------------------------------------------------------------
     *  Tests for atomic conversions
     * ------------------------------------------------------------- */

    @Test
    public void testToAtomicInteger() {
        Year y = Year.of(2020);
        AtomicInteger ai = YearConversions.toAtomicInteger(y, dummyConverter);
        assertNotNull(ai);
        assertEquals(2020, ai.get());
    }

    @Test
    public void testToAtomicLong() {
        Year y = Year.of(-44);
        AtomicLong al = YearConversions.toAtomicLong(y, dummyConverter);
        assertNotNull(al);
        assertEquals(-44L, al.get());
    }

    @Test
    public void testToAtomicBooleanTrue() {
        Year y = Year.of(1); // any non‑zero maps to true
        AtomicBoolean ab = YearConversions.toAtomicBoolean(y, dummyConverter);
        assertTrue(ab.get());
    }

    @Test
    public void testToAtomicBooleanFalse() {
        Year y = Year.of(0);
        AtomicBoolean ab = YearConversions.toAtomicBoolean(y, dummyConverter);
        assertFalse(ab.get());
    }

    /* -------------------------------------------------------------
     *  Tests for BigInteger / BigDecimal conversions
     * ------------------------------------------------------------- */

    @Test
    public void testToBigInteger() {
        Year y = Year.of(123456);
        BigInteger bi = YearConversions.toBigInteger(y, dummyConverter);
        assertEquals(BigInteger.valueOf(123456), bi);
    }

    @Test
    public void testToBigDecimal() {
        Year y = Year.of(-98765);
        BigDecimal bd = YearConversions.toBigDecimal(y, dummyConverter);
        assertEquals(BigDecimal.valueOf(-98765), bd);
    }

    /* -------------------------------------------------------------
     *  Tests for floating‑point conversions
     * ------------------------------------------------------------- */

    @Test
    public void testToDouble() {
        Year y = Year.of(2022);
        double d = YearConversions.toDouble(y, dummyConverter);
        assertEquals(2022.0, d, 0.0);
    }

    @Test
    public void testToFloat() {
        Year y = Year.of(-2022);
        float f = YearConversions.toFloat(y, dummyConverter);
        assertEquals(-2022.0f, f, 0.0f);
    }

    /* -------------------------------------------------------------
     *  Tests for String conversion
     * ------------------------------------------------------------- */

    @Test
    public void testToString() {
        Year y = Year.of(2021);
        String s = YearConversions.toString(y, dummyConverter);
        assertEquals("2021", s);
    }

    @Test
    public void testToStringNegativeYear() {
        Year y = Year.of(-44); // e.g., 44 BC
        String s = YearConversions.toString(y, dummyConverter);
        assertEquals("-44", s);
    }

    /* -------------------------------------------------------------
     *  Tests for Map conversion
     * ------------------------------------------------------------- */

    @Test
    public void testToMapTypical() {
        Year y = Year.of(2000);
        Map<?,?> map = YearConversions.toMap(y, dummyConverter);
        assertMapContainsYear(map, 2000);
    }

    @Test
    public void testToMapZeroYear() {
        Year y = Year.of(0);
        Map<?,?> map = YearConversions.toMap(y, dummyConverter);
        assertMapContainsYear(map, 0);
    }

    @Test(expected = NullPointerException.class)
    public void testToMapNullSource() {
        YearConversions.toMap(null, dummyConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToMapWrongType() {
        YearConversions.toMap("2020", dummyConverter);
    }
}
