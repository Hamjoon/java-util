package com.cedarsoftware.util.convert;

import static com.cedarsoftware.util.convert.MapConversions.YEAR;
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
 * JUnit‑4 test suite for {@link YearConversions}.
 *
 * The tests are written from the same package as {@code YearConversions}
 * so that package‑private static methods are directly accessible.
 *
 * The test plan follows the steps requested:
 *
 * 1. List all public (actually package‑private static) methods with signatures.
 * 2. Provide a basic test for the normal (happy‑path) behaviour of each method.
 * 3. Identify edge‑cases and exception scenarios (null input, wrong type, boundary years,
 *    zero‑year for boolean conversion, etc.).
 * 4. Add explicit tests for those edge‑cases.
 * 5. Merge everything into a single test class.
 */
public class YearConversionsTest {

    /* -----------------------------------------------------------------------
     * 1.  Method signatures (extracted from the source)
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
     *
     * The {@code Converter} argument is not used by any implementation, therefore a
     * {@code null} reference is sufficient for all tests.
     */

    /* -----------------------------------------------------------------------
     * 2.  Normal (happy‑path) tests
     * ----------------------------------------------------------------------- */

    @Test
    public void testToInt() {
        Year y = Year.of(2020);
        int result = YearConversions.toInt(y, null);
        assertEquals(2020, result);
    }

    @Test
    public void testToLong() {
        Year y = Year.of(1999);
        long result = YearConversions.toLong(y, null);
        assertEquals(1999L, result);
    }

    @Test
    public void testToShort() {
        Year y = Year.of(123);
        short result = YearConversions.toShort(y, null);
        assertEquals((short)123, result);
    }

    @Test
    public void testToAtomicInteger() {
        Year y = Year.of(2015);
        AtomicInteger ai = YearConversions.toAtomicInteger(y, null);
        assertEquals(2015, ai.get());
    }

    @Test
    public void testToAtomicLong() {
        Year y = Year.of(2015);
        AtomicLong al = YearConversions.toAtomicLong(y, null);
        assertEquals(2015L, al.get());
    }

    @Test
    public void testToDouble() {
        Year y = Year.of(1985);
        double d = YearConversions.toDouble(y, null);
        assertEquals(1985.0, d, 0.0);
    }

    @Test
    public void testToFloat() {
        Year y = Year.of(1985);
        float f = YearConversions.toFloat(y, null);
        assertEquals(1985.0f, f, 0.0f);
    }

    @Test
    public void testToAtomicBoolean_NonZero() {
        Year y = Year.of(2021);
        AtomicBoolean ab = YearConversions.toAtomicBoolean(y, null);
        assertTrue(ab.get());
    }

    @Test
    public void testToAtomicBoolean_Zero() {
        Year y = Year.of(0);
        AtomicBoolean ab = YearConversions.toAtomicBoolean(y, null);
        assertFalse(ab.get());
    }

    @Test
    public void testToBigInteger() {
        Year y = Year.of(3000);
        BigInteger bi = YearConversions.toBigInteger(y, null);
        assertEquals(BigInteger.valueOf(3000), bi);
    }

    @Test
    public void testToBigDecimal() {
        Year y = Year.of(3000);
        BigDecimal bd = YearConversions.toBigDecimal(y, null);
        assertEquals(BigDecimal.valueOf(3000), bd);
    }

    @Test
    public void testToString() {
        Year y = Year.of(2022);
        String s = YearConversions.toString(y, null);
        assertEquals("2022", s);
    }

    @Test
    public void testToMap() {
        Year y = Year.of(1975);
        @SuppressWarnings("unchecked")
        Map<String, Integer> map = (Map<String, Integer>) YearConversions.toMap(y, null);
        assertNotNull(map);
        assertEquals(1, map.size());
        assertTrue(map.containsKey(YEAR));
        assertEquals(Integer.valueOf(1975), map.get(YEAR));
    }

    /* -----------------------------------------------------------------------
     * 3.  Edge‑case / exception tests
     * ----------------------------------------------------------------------- */

    /** Helper that asserts a {@link ClassCastException} when a non‑Year object is passed. */
    private void expectClassCast(Object badInput) {
        try {
            YearConversions.toInt(badInput, null);
            fail("Expected ClassCastException for input: " + badInput);
        } catch (ClassCastException expected) {
            // expected path
        }
    }

    /** Helper that asserts a {@link NullPointerException} when a null Year is passed. */
    private void expectNPE(Object nullInput) {
        try {
            YearConversions.toInt(nullInput, null);
            fail("Expected NullPointerException for null input");
        } catch (NullPointerException expected) {
            // expected path
        }
    }

    @Test
    public void testNullInput_NullPointerException() {
        expectNPE(null);
    }

    @Test
    public void testWrongType_ClassCastException() {
        expectClassCast(Integer.valueOf(2020));
        expectClassCast("2020");
        expectClassCast(new Object());
    }

    @Test
    public void testBoundaryYear_MaxSupported() {
        // Year supports up to 999,999,999 (fits in int)
        Year maxYear = Year.of(999_999_999);
        int v = YearConversions.toInt(maxYear, null);
        assertEquals(999_999_999, v);
        // Verify that conversions that rely on toInt also work
        assertEquals(999_999_999L, YearConversions.toLong(maxYear, null));
        assertEquals((short) -1, YearConversions.toShort(maxYear, null)); // overflow to short
        assertEquals(999_999_999.0, YearConversions.toDouble(maxYear, null), 0.0);
        assertEquals(BigInteger.valueOf(999_999_999), YearConversions.toBigInteger(maxYear, null));
    }

    @Test
    public void testBoundaryYear_MinSupported() {
        // Minimum supported year is -999,999,999
        Year minYear = Year.of(-999_999_999);
        int v = YearConversions.toInt(minYear, null);
        assertEquals(-999_999_999, v);
        // Boolean conversion: any non‑zero should be true
        AtomicBoolean ab = YearConversions.toAtomicBoolean(minYear, null);
        assertTrue(ab.get());
    }

    @Test
    public void testToAtomicBoolean_NegativeYear() {
        Year y = Year.of(-1);
        AtomicBoolean ab = YearConversions.toAtomicBoolean(y, null);
        assertTrue(ab.get());
    }

    @Test
    public void testToMap_ImmutableResult() {
        Year y = Year.of(2023);
        @SuppressWarnings("unchecked")
        Map<String, Integer> map = (Map<String, Integer>) YearConversions.toMap(y, null);
        // The returned map should be mutable (CompactLinkedMap is mutable);
        // we verify that adding a new entry works and does not affect the original year.
        map.put("extra", 42);
        assertEquals(Integer.valueOf(42), map.get("extra"));
        assertEquals(Integer.valueOf(2023), map.get(YEAR));
    }

    /* -----------------------------------------------------------------------
     * 4.  Additional sanity checks – ensure that the unused Converter argument
     *     does not affect behaviour.
     * ----------------------------------------------------------------------- */

    @Test
    public void testWithNonNullConverterDoesNotChangeResult() {
        Year y = Year.of(2000);
        Converter dummy = new Converter(new ConverterOptions()); // real instance, not used
        assertEquals(2000, YearConversions.toInt(y, dummy));
        assertEquals(2000L, YearConversions.toLong(y, dummy));
        assertEquals((short)2000, YearConversions.toShort(y, dummy));
        assertEquals("2000", YearConversions.toString(y, dummy));
    }
}

