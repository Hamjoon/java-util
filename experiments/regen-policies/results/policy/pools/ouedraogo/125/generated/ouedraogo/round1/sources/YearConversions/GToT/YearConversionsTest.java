package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactMap;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.DateTimeException;
import java.time.Year;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.cedarsoftware.util.convert.MapConversions.YEAR;
import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link YearConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>normal conversion of a typical {@link Year}</li>
 *   <li>edge values (minimum, maximum, zero, negative)</li>
 *   <li>exceptional scenarios – null source and wrong source type</li>
 *   <li>behaviour of the {@code AtomicBoolean} conversion (zero ⇒ false, otherwise true)</li>
 *   <li>correct structure of the {@code Map} conversion</li>
 * </ul>
 *
 * The {@code Converter} argument is not used by any method in {@code YearConversions},
 * therefore a {@code null} reference is passed safely.
 */
public class YearConversionsTest {

    // ----------------------------------------------------------------------
    // Helper methods
    // ----------------------------------------------------------------------
    private static final Converter DUMMY_CONVERTER = null;   // not used by YearConversions

    /**
     * Creates a {@link Year} instance for the given int value.
     * If the value is outside the valid range, the test will fail with a clear message.
     */
    private static Year yearOf(int value) {
        try {
            return Year.of(value);
        } catch (DateTimeException e) {
            fail("Year.of(" + value + ") threw DateTimeException – value out of range");
            return null; // unreachable
        }
    }

    // ----------------------------------------------------------------------
    //  toInt / toLong / toShort / toDouble / toFloat
    // ----------------------------------------------------------------------
    @Test
    public void testToInt_normal() {
        Year y = yearOf(2023);
        assertEquals(2023, YearConversions.toInt(y, DUMMY_CONVERTER));
    }

    @Test
    public void testToInt_edgeValues() {
        // Minimum allowed year
        Year min = yearOf(-999_999_999);
        assertEquals(-999_999_999, YearConversions.toInt(min, DUMMY_CONVERTER));

        // Maximum allowed year
        Year max = yearOf(999_999_999);
        assertEquals(999_999_999, YearConversions.toInt(max, DUMMY_CONVERTER));

        // Zero (year 0 is valid)
        Year zero = yearOf(0);
        assertEquals(0, YearConversions.toInt(zero, DUMMY_CONVERTER));
    }

    @Test(expected = NullPointerException.class)
    public void testToInt_nullSource() {
        YearConversions.toInt(null, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToInt_wrongSourceType() {
        YearConversions.toInt(Integer.valueOf(2023), DUMMY_CONVERTER);
    }

    @Test
    public void testToLong_delegatesToInt() {
        Year y = yearOf(1999);
        assertEquals(1999L, YearConversions.toLong(y, DUMMY_CONVERTER));
    }

    @Test
    public void testToShort_delegatesToInt() {
        Year y = yearOf(1999);
        assertEquals((short)1999, YearConversions.toShort(y, DUMMY_CONVERTER));
    }

    @Test
    public void testToDouble_delegatesToInt() {
        Year y = yearOf(1999);
        assertEquals(1999.0, YearConversions.toDouble(y, DUMMY_CONVERTER), 0.0);
    }

    @Test
    public void testToFloat_delegatesToInt() {
        Year y = yearOf(1999);
        assertEquals(1999.0f, YearConversions.toFloat(y, DUMMY_CONVERTER), 0.0f);
    }

    // ----------------------------------------------------------------------
    //  toAtomicInteger / toAtomicLong / toAtomicBoolean
    // ----------------------------------------------------------------------
    @Test
    public void testToAtomicInteger() {
        Year y = yearOf(2020);
        AtomicInteger ai = YearConversions.toAtomicInteger(y, DUMMY_CONVERTER);
        assertNotNull(ai);
        assertEquals(2020, ai.get());
    }

    @Test
    public void testToAtomicLong() {
        Year y = yearOf(2020);
        AtomicLong al = YearConversions.toAtomicLong(y, DUMMY_CONVERTER);
        assertNotNull(al);
        assertEquals(2020L, al.get());
    }

    @Test
    public void testToAtomicBoolean_zeroIsFalse() {
        Year zero = yearOf(0);
        AtomicBoolean ab = YearConversions.toAtomicBoolean(zero, DUMMY_CONVERTER);
        assertFalse(ab.get());
    }

    @Test
    public void testToAtomicBoolean_nonZeroIsTrue() {
        Year y = yearOf(2021);
        AtomicBoolean ab = YearConversions.toAtomicBoolean(y, DUMMY_CONVERTER);
        assertTrue(ab.get());
    }

    // ----------------------------------------------------------------------
    //  toBigInteger / toBigDecimal
    // ----------------------------------------------------------------------
    @Test
    public void testToBigInteger() {
        Year y = yearOf(12345);
        BigInteger bi = YearConversions.toBigInteger(y, DUMMY_CONVERTER);
        assertEquals(BigInteger.valueOf(12345), bi);
    }

    @Test
    public void testToBigDecimal() {
        Year y = yearOf(12345);
        BigDecimal bd = YearConversions.toBigDecimal(y, DUMMY_CONVERTER);
        assertEquals(BigDecimal.valueOf(12345), bd);
    }

    // ----------------------------------------------------------------------
    //  toString
    // ----------------------------------------------------------------------
    @Test
    public void testToString() {
        Year y = yearOf(2022);
        String s = YearConversions.toString(y, DUMMY_CONVERTER);
        assertEquals("2022", s);
    }

    // ----------------------------------------------------------------------
    //  toMap
    // ----------------------------------------------------------------------
    @Test
    public void testToMap_normal() {
        Year y = yearOf(2024);
        Map<?, ?> map = YearConversions.toMap(y, DUMMY_CONVERTER);

        assertNotNull(map);
        assertTrue(map instanceof Map);
        assertEquals(1, map.size());
        assertTrue(map.containsKey(YEAR));
        assertEquals(Integer.valueOf(2024), map.get(YEAR));
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_nullSource() {
        YearConversions.toMap(null, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToMap_wrongSourceType() {
        YearConversions.toMap("2023", DUMMY_CONVERTER);
    }

    // ----------------------------------------------------------------------
    //  Additional edge verification for map ordering (CompactMap insertion order)
    // ----------------------------------------------------------------------
    @Test
    public void testToMap_isInsertionOrdered() {
        // CompactMap builder is instructed to keep insertion order.
        // Since we only put a single entry, the test simply asserts that the key set iterator
        // returns the YEAR constant as the first (and only) element.
        Year y = yearOf(2000);
        Map<?, ?> map = YearConversions.toMap(y, DUMMY_CONVERTER);
        assertEquals(YEAR, map.keySet().iterator().next());
    }
}
