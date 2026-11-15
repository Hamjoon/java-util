package com.cedarsoftware.util;

import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link MathUtilities}.
 *
 * The tests cover:
 * <ul>
 *   <li>minimum / maximum for primitive long and double arrays</li>
 *   <li>minimum / maximum for {@link BigInteger} and {@link BigDecimal} arrays</li>
 *   <li>error handling for null entries in the Big* methods</li>
 *   <li>parseToMinimalNumericType – various numeric strings, edge values, sign handling and error cases</li>
 *   <li>nextPermutation – normal progression, exhaustion, and null‑list handling</li>
 * </ul>
 */
public class MathUtilitiesTest {

    /* -------------------------------------------------------
     *  primitive long tests
     * ------------------------------------------------------- */

    @Test
    public void testMinimumLongMultiple() {
        long result = MathUtilities.minimum(5L, -3L, 12L, 0L);
        assertEquals(-3L, result);
    }

    @Test
    public void testMaximumLongMultiple() {
        long result = MathUtilities.maximum(5L, -3L, 12L, 0L);
        assertEquals(12L, result);
    }

    @Test
    public void testMinimumLongSingle() {
        long result = MathUtilities.minimum(42L);
        assertEquals(42L, result);
    }

    @Test
    public void testMaximumLongSingle() {
        long result = MathUtilities.maximum(42L);
        assertEquals(42L, result);
    }

    /* -------------------------------------------------------
     *  primitive double tests
     * ------------------------------------------------------- */

    @Test
    public void testMinimumDoubleMultiple() {
        double result = MathUtilities.minimum(5.5, -3.2, 12.0, 0.0);
        assertEquals(-3.2, result, 0.0);
    }

    @Test
    public void testMaximumDoubleMultiple() {
        double result = MathUtilities.maximum(5.5, -3.2, 12.0, 0.0);
        assertEquals(12.0, result, 0.0);
    }

    @Test
    public void testMinimumDoubleSingle() {
        double result = MathUtilities.minimum(42.42);
        assertEquals(42.42, result, 0.0);
    }

    @Test
    public void testMaximumDoubleSingle() {
        double result = MathUtilities.maximum(42.42);
        assertEquals(42.42, result, 0.0);
    }

    /* -------------------------------------------------------
     *  BigInteger tests
     * ------------------------------------------------------- */

    @Test
    public void testMinimumBigIntegerMultiple() {
        BigInteger a = new BigInteger("-100");
        BigInteger b = new BigInteger("0");
        BigInteger c = new BigInteger("999999999999999999999");
        BigInteger result = MathUtilities.minimum(a, b, c);
        assertEquals(a, result);
    }

    @Test
    public void testMaximumBigIntegerMultiple() {
        BigInteger a = new BigInteger("-100");
        BigInteger b = new BigInteger("0");
        BigInteger c = new BigInteger("999999999999999999999");
        BigInteger result = MathUtilities.maximum(a, b, c);
        assertEquals(c, result);
    }

    @Test
    public void testMinimumBigIntegerSingle() {
        BigInteger val = new BigInteger("12345678901234567890");
        assertEquals(val, MathUtilities.minimum(val));
    }

    @Test
    public void testMaximumBigIntegerSingle() {
        BigInteger val = new BigInteger("-12345678901234567890");
        assertEquals(val, MathUtilities.maximum(val));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMinimumBigIntegerNullEntryThrows() {
        MathUtilities.minimum(new BigInteger("1"), null, new BigInteger("2"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMaximumBigIntegerNullEntryThrows() {
        MathUtilities.maximum(null);
    }

    /* -------------------------------------------------------
     *  BigDecimal tests
     * ------------------------------------------------------- */

    @Test
    public void testMinimumBigDecimalMultiple() {
        BigDecimal a = new BigDecimal("-0.001");
        BigDecimal b = new BigDecimal("0");
        BigDecimal c = new BigDecimal("12345.6789");
        BigDecimal result = MathUtilities.minimum(a, b, c);
        assertEquals(a, result);
    }

    @Test
    public void testMaximumBigDecimalMultiple() {
        BigDecimal a = new BigDecimal("-0.001");
        BigDecimal b = new BigDecimal("0");
        BigDecimal c = new BigDecimal("12345.6789");
        BigDecimal result = MathUtilities.maximum(a, b, c);
        assertEquals(c, result);
    }

    @Test
    public void testMinimumBigDecimalSingle() {
        BigDecimal val = new BigDecimal("3.1415926535897932384626");
        assertEquals(val, MathUtilities.minimum(val));
    }

    @Test
    public void testMaximumBigDecimalSingle() {
        BigDecimal val = new BigDecimal("-2.718281828459045235360");
        assertEquals(val, MathUtilities.maximum(val));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMinimumBigDecimalNullEntryThrows() {
        MathUtilities.minimum(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMaximumBigDecimalNullEntryThrows() {
        MathUtilities.maximum(new BigDecimal("1.0"), null);
    }

    /* -------------------------------------------------------
     *  parseToMinimalNumericType tests
     * ------------------------------------------------------- */

    @Test
    public void testParseToMinimalNumericTypeLong() {
        Number n = MathUtilities.parseToMinimalNumericType("1234567890");
        assertTrue(n instanceof Long);
        assertEquals(1234567890L, n.longValue());
    }

    @Test
    public void testParseToMinimalNumericTypeBigInteger() {
        String bigIntStr = "99999999999999999999999999999999999999";
        Number n = MathUtilities.parseToMinimalNumericType(bigIntStr);
        assertTrue(n instanceof BigInteger);
        assertEquals(new BigInteger(bigIntStr), n);
    }

    @Test
    public void testParseToMinimalNumericTypeDouble() {
        Number n = MathUtilities.parseToMinimalNumericType("3.14159");
        assertTrue(n instanceof Double);
        assertEquals(3.14159, n.doubleValue(), 0.0);
    }

    @Test
    public void testParseToMinimalNumericTypeBigDecimalDueToExponent() {
        Number n = MathUtilities.parseToMinimalNumericType("1e308"); // exponent = 308 → forces BigDecimal
        assertTrue(n instanceof BigDecimal);
        assertEquals(new BigDecimal("1e308"), n);
    }

    @Test
    public void testParseToMinimalNumericTypeNegativeNumber() {
        Number n = MathUtilities.parseToMinimalNumericType("-00123");
        assertTrue(n instanceof Long);
        assertEquals(-123L, n.longValue());
    }

    @Test
    public void testParseToMinimalNumericTypeLeadingZeros() {
        Number n = MathUtilities.parseToMinimalNumericType("00000123");
        assertTrue(n instanceof Long);
        assertEquals(123L, n.longValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseToMinimalNumericTypeNullStringThrows() {
        MathUtilities.parseToMinimalNumericType(null);
    }

    @Test(expected = NumberFormatException.class)
    public void testParseToMinimalNumericTypeInvalidStringThrows() {
        MathUtilities.parseToMinimalNumericType("not-a-number");
    }

    /* -------------------------------------------------------
     *  nextPermutation tests
     * ------------------------------------------------------- */

    @Test
    public void testNextPermutationNormalProgression() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3));
        List<List<Integer>> permutations = new ArrayList<>();

        // First permutation is the original order
        permutations.add(new ArrayList<>(list));
        while (MathUtilities.nextPermutation(list)) {
            permutations.add(new ArrayList<>(list));
        }

        List<List<Integer>> expected = Arrays.asList(
                Arrays.asList(1, 2, 3),
                Arrays.asList(1, 3, 2),
                Arrays.asList(2, 1, 3),
                Arrays.asList(2, 3, 1),
                Arrays.asList(3, 1, 2),
                Arrays.asList(3, 2, 1)
        );

        assertEquals(expected, permutations);
    }

    @Test
    public void testNextPermutationExhaustedReturnsFalse() {
        List<Integer> list = new ArrayList<>(Arrays.asList(3, 2, 1));
        assertFalse(MathUtilities.nextPermutation(list));
        // List must stay unchanged when no further permutation exists
        assertEquals(Arrays.asList(3, 2, 1), list);
    }

    @Test(expected = NullPointerException.class)
    public void testNextPermutationNullListThrows() {
        MathUtilities.nextPermutation(null);
    }

    @Test
    public void testNextPermutationWithStrings() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        assertTrue(MathUtilities.nextPermutation(list));
        assertEquals(Arrays.asList("a", "c", "b"), list);
    }
}
