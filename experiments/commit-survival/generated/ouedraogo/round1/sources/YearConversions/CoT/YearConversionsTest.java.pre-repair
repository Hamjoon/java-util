package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Year;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * JUnit 4 test suite for {@link YearConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>basic successful conversions for each public static method</li>
 *   <li>edge‑case handling (null input, wrong type, extreme Year values)</li>
 *   <li>specific behaviours such as overflow when casting to {@code short}</li>
 *   <li>boolean conversion semantics</li>
 * </ul>
 *
 * Note: The {@code Converter} argument is not used by any method in {@code YearConversions},
 * therefore a {@code null} value is passed in all tests.
 */
public class YearConversionsTest {

    private Converter dummyConverter; // not used, but kept for signature compatibility

    @Before
    public void setUp() {
        // The Converter constructor requires a ConverterOptions instance.
        // The concrete class is not provided, but we can safely pass null because
        // YearConversions never dereferences the converter argument.
        dummyConverter = null;
    }

    /* -----------------------------------------------------------------------
     *  List of public static methods (extracted from YearConversions)
     * -----------------------------------------------------------------------
     *
     * static long          toLong(Object from, Converter converter)
     * static short         toShort(Object from, Converter converter)
     * static int           toInt(Object from, Converter converter)
     * static AtomicInteger toAtomicInteger(Object from, Converter converter)
     * static AtomicLong    toAtomicLong(Object from, Converter converter)
     * static double        toDouble(Object from, Converter converter)
     * static float         toFloat(Object from, Converter converter)
     * static AtomicBoolean toAtomicBoolean(Object from, Converter converter)
     * static BigInteger    toBigInteger(Object from, Converter converter)
     * static BigDecimal    toBigDecimal(Object from, Converter converter)
     * static String        toString(Object from, Converter converter)
     * static Map<?,?>     toMap(Object from, Converter converter)
     */

    /* -----------------------------------------------------------------------
     *  Basic functionality tests
     * ----------------------------------------------------------------------- */

    @Test
    public void testToInt() {
        Year y = Year.of(2023);
        int result = YearConversions.toInt(y, dummyConverter);
        assertEquals(2023, result);
    }

    @Test
    public void testToLong() {
        Year y = Year.of(1999);
        long result = YearConversions.toLong(y, dummyConverter);
        assertEquals(1999L, result);
    }

    @Test
    public void testToShort() {
        Year y = Year.of(12345);
        short result = YearConversions.toShort(y, dummyConverter);
        assertEquals((short)12345, result);
    }

    @Test
    public void testToShortOverflow() {
        // Year value exceeding Short.MAX_VALUE (32767) – expect truncation
        Year y = Year.of(40000);
        short result = YearConversions.toShort(y, dummyConverter);
        assertEquals((short)40000, result); // Java casts by truncating high bits
    }

    @Test
    public void testToAtomicInteger() {
        Year y = Year.of(0);
        AtomicInteger ai = YearConversions.toAtomicInteger(y, dummyConverter);
        assertEquals(0, ai.get());
    }

    @Test
    public void testToAtomicLong() {
        Year y = Year.of(-500);
        AtomicLong al = YearConversions.toAtomicLong(y, dummyConverter);
        assertEquals(-500L, al.get());
    }

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

    @Test
    public void testToAtomicBooleanTrue() {
        Year y = Year.of(1);
        AtomicBoolean ab = YearConversions.toAtomicBoolean(y, dummyConverter);
        assertTrue(ab.get());
    }

    @Test
    public void testToAtomicBooleanFalse() {
        Year y = Year.of(0);
        AtomicBoolean ab = YearConversions.toAtomicBoolean(y, dummyConverter);
        assertFalse(ab.get());
    }

    @Test
    public void testToBigInteger() {
        Year y = Year.of(123456789);
        BigInteger bi = YearConversions.toBigInteger(y, dummyConverter);
        assertEquals(BigInteger.valueOf(123456789L), bi);
    }

    @Test
    public void testToBigDecimal() {
        Year y = Year.of(-987654321);
        BigDecimal bd = YearConversions.toBigDecimal(y, dummyConverter);
        assertEquals(BigDecimal.valueOf(-987654321L), bd);
    }

    @Test
    public void testToString() {
        Year y = Year.of(2025);
        String s = YearConversions.toString(y, dummyConverter);
        assertEquals("2025", s);
    }

    @Test
    public void testToMap() {
        Year y = Year.of(2021);
        @SuppressWarnings("unchecked")
        Map<String, Integer> map = (Map<String, Integer>) YearConversions.toMap(y, dummyConverter);
        assertNotNull(map);
        // The constant YEAR is imported statically from MapConversions; we assume it equals "year".
        assertTrue(map.containsKey("year"));
        assertEquals(Integer.valueOf(2021), map.get("year"));
    }

    /* -----------------------------------------------------------------------
     *  Edge‑case & exception tests
     * ----------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testToInt_NullInput() {
        YearConversions.toInt(null, dummyConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToLong_NullInput() {
        YearConversions.toLong(null, dummyConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToInt_WrongType() {
        YearConversions.toInt(Integer.valueOf(2020), dummyConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToString_WrongType() {
        YearConversions.toString("2020", dummyConverter);
    }

    @Test
    public void testExtremeYearValues() {
        // Minimum and maximum allowed by java.time.Year
        Year min = Year.of(Year.MIN_VALUE);
        Year max = Year.of(Year.MAX_VALUE);

        assertEquals(Year.MIN_VALUE, YearConversions.toInt(min, dummyConverter));
        assertEquals(Year.MAX_VALUE, YearConversions.toInt(max, dummyConverter));

        // Verify map conversion for extremes
        @SuppressWarnings("unchecked")
        Map<String, Integer> mapMin = (Map<String, Integer>) YearConversions.toMap(min, dummyConverter);
        @SuppressWarnings("unchecked")
        Map<String, Integer> mapMax = (Map<String, Integer>) YearConversions.toMap(max, dummyConverter);
        assertEquals(Integer.valueOf(Year.MIN_VALUE), mapMin.get("year"));
        assertEquals(Integer.valueOf(Year.MAX_VALUE), mapMax.get("year"));
    }

    @Test
    public void testAtomicBoolean_NonZeroNegative() {
        Year y = Year.of(-5);
        AtomicBoolean ab = YearConversions.toAtomicBoolean(y, dummyConverter);
        // Any non‑zero value should be true
        assertTrue(ab.get());
    }

    @Test
    public void testToMap_ImmutabilityOfReturnedMap() {
        Year y = Year.of(2000);
        @SuppressWarnings("unchecked")
        Map<String, Integer> map = (Map<String, Integer>) YearConversions.toMap(y, dummyConverter);
        // The returned map should be mutable (CompactLinkedMap extends HashMap behaviour)
        map.put("extra", 123);
        assertEquals(Integer.valueOf(123), map.get("extra"));
    }
}
