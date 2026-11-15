package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 * Comprehensive JUnit4 test suite for {@link MathUtilities}.
 */
public class MathUtilitiesTest {

    /* -------------------------------------------------
     *  Tests for minimum / maximum (long)
     * ------------------------------------------------- */
    @Test
    public void testLongMinimumMultipleValues() {
        assertEquals(-10L, MathUtilities.minimum(5L, -10L, 3L, 0L));
        assertEquals(Long.MIN_VALUE, MathUtilities.minimum(Long.MAX_VALUE, Long.MIN_VALUE, 0L));
    }

    @Test
    public void testLongMaximumMultipleValues() {
        assertEquals(9L, MathUtilities.maximum(5L, -10L, 9L, 0L));
        assertEquals(Long.MAX_VALUE, MathUtilities.maximum(Long.MAX_VALUE, Long.MIN_VALUE, 0L));
    }

    @Test
    public void testLongMinimumAndMaximumSingleElement() {
        assertEquals(42L, MathUtilities.minimum(42L));
        assertEquals(42L, MathUtilities.maximum(42L));
    }

    /* -------------------------------------------------
     *  Tests for minimum / maximum (double)
     * ------------------------------------------------- */
    @Test
    public void testDoubleMinimumMultipleValues() {
        assertEquals(-2.5, MathUtilities.minimum(0.0, -2.5, 3.14, 2.718), 0.0);
        assertEquals(-Double.MAX_VALUE, MathUtilities.minimum(Double.MAX_VALUE, -Double.MAX_VALUE, 0.0), 0.0);
    }

    @Test
    public void testDoubleMaximumMultipleValues() {
        assertEquals(3.14, MathUtilities.maximum(0.0, -2.5, 3.14, 2.718), 0.0);
        assertEquals(Double.MAX_VALUE, MathUtilities.maximum(Double.MAX_VALUE, -Double.MAX_VALUE, 0.0), 0.0);
    }

    @Test
    public void testDoubleMinimumAndMaximumSingleElement() {
        assertEquals(1.2345, MathUtilities.minimum(1.2345), 0.0);
        assertEquals(1.2345, MathUtilities.maximum(1.2345), 0.0);
    }

    /* -------------------------------------------------
     *  Tests for minimum / maximum (BigInteger)
     * ------------------------------------------------- */
    @Test
    public void testBigIntegerMinimumAndMaximum() {
        BigInteger a = new BigInteger("-12345678901234567890");
        BigInteger b = new BigInteger("0");
        BigInteger c = new BigInteger("12345678901234567890");

        assertEquals(a, MathUtilities.minimum(a, b, c));
        assertEquals(c, MathUtilities.maximum(a, b, c));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBigIntegerMinimumNullEntryThrows() {
        MathUtilities.minimum(BigInteger.ONE, null, BigInteger.TEN);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBigIntegerMaximumNullEntryThrows() {
        MathUtilities.maximum(null);
    }

    @Test
    public void testBigIntegerMinimumSingleElement() {
        BigInteger val = new BigInteger("999");
        assertEquals(val, MathUtilities.minimum(val));
    }

    @Test
    public void testBigIntegerMaximumSingleElement() {
        BigInteger val = new BigInteger("-999");
        assertEquals(val, MathUtilities.maximum(val));
    }

    /* -------------------------------------------------
     *  Tests for minimum / maximum (BigDecimal)
     * ------------------------------------------------- */
    @Test
    public void testBigDecimalMinimumAndMaximum() {
        BigDecimal a = new BigDecimal("-12345.6789");
        BigDecimal b = new BigDecimal("0.0");
        BigDecimal c = new BigDecimal("12345.6789");

        assertEquals(a, MathUtilities.minimum(a, b, c));
        assertEquals(c, MathUtilities.maximum(a, b, c));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBigDecimalMinimumNullEntryThrows() {
        MathUtilities.minimum(new BigDecimal("1.0"), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBigDecimalMaximumNullEntryThrows() {
        MathUtilities.maximum(null);
    }

    @Test
    public void testBigDecimalMinimumSingleElement() {
        BigDecimal val = new BigDecimal("3.1415");
        assertEquals(val, MathUtilities.minimum(val));
    }

    @Test
    public void testBigDecimalMaximumSingleElement() {
        BigDecimal val = new BigDecimal("-3.1415");
        assertEquals(val, MathUtilities.maximum(val));
    }

    /* -------------------------------------------------
     *  Tests for parseToMinimalNumericType
     * ------------------------------------------------- */
    @Test(expected = IllegalArgumentException.class)
    public void testParseToMinimalNumericTypeNullArgument() {
        MathUtilities.parseToMinimalNumericType(null);
    }

    @Test
    public void testParseIntegerWithinLongRange() {
        Number n = MathUtilities.parseToMinimalNumericType("1234567890");
        assertTrue(n instanceof Long);
        assertEquals(1234567890L, n.longValue());
    }

    @Test
    public void testParseIntegerBeyondLongRangeReturnsBigInteger() {
        String big = "9999999999999999999999999999"; // > Long.MAX_VALUE
        Number n = MathUtilities.parseToMinimalNumericType(big);
        assertTrue(n instanceof BigInteger);
        assertEquals(new BigInteger(big), n);
    }

    @Test
    public void testParseNegativeIntegerWithLeadingZeros() {
        Number n = MathUtilities.parseToMinimalNumericType("-0000123");
        assertTrue(n instanceof Long);
        assertEquals(-123L, n.longValue());
    }

    @Test
    public void testParseZeroVariations() {
        Number n1 = MathUtilities.parseToMinimalNumericType("0");
        Number n2 = MathUtilities.parseToMinimalNumericType("-0");
        Number n3 = MathUtilities.parseToMinimalNumericType("+0000");
        assertTrue(n1 instanceof Long);
        assertTrue(n2 instanceof Long);
        assertTrue(n3 instanceof Long);
        assertEquals(0L, n1.longValue());
        assertEquals(0L, n2.longValue());
        assertEquals(0L, n3.longValue());
    }

    @Test
    public void testParseDecimalWithinDoublePrecision() {
        Number n = MathUtilities.parseToMinimalNumericType("3.1415926535");
        assertTrue(n instanceof Double);
        assertEquals(3.1415926535, n.doubleValue(), 0.0);
    }

    @Test
    public void testParseDecimalRequiringBigDecimalDueToPrecision() {
        // 20 decimal digits, exceeds the 16‑digit threshold used in the implementation
        String dec = "0.12345678901234567890";
        Number n = MathUtilities.parseToMinimalNumericType(dec);
        assertTrue(n instanceof BigDecimal);
        assertEquals(new BigDecimal(dec), n);
    }

    @Test
    public void testParseScientificNotationWithinDoubleRange() {
        Number n = MathUtilities.parseToMinimalNumericType("1e5");
        assertTrue(n instanceof Double);
        assertEquals(1e5, n.doubleValue(), 0.0);
    }

    @Test
    public void testParseScientificNotationBeyondDoubleExponent() {
        // Exponent 400 > 308 triggers BigDecimal
        Number n = MathUtilities.parseToMinimalNumericType("1e400");
        assertTrue(n instanceof BigDecimal);
        assertEquals(new BigDecimal("1e400"), n);
    }

    @Test
    public void testParsePositiveSignPreserved() {
        Number n = MathUtilities.parseToMinimalNumericType("+42");
        assertTrue(n instanceof Long);
        assertEquals(42L, n.longValue());
    }

    @Test
    public void testParseNegativeDecimal() {
        Number n = MathUtilities.parseToMinimalNumericType("-0.001");
        assertTrue(n instanceof Double);
        assertEquals(-0.001, n.doubleValue(), 0.0);
    }

    /* -------------------------------------------------
     *  Tests for nextPermutation
     * ------------------------------------------------- */
    @Test(expected = IllegalArgumentException.class)
    public void testNextPermutationNullListThrows() {
        MathUtilities.nextPermutation(null);
    }

    @Test
    public void testNextPermutationEmptyListReturnsFalse() {
        List<Integer> list = new ArrayList<>();
        boolean result = MathUtilities.nextPermutation(list);
        assertFalse(result);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testNextPermutationSingleElementReturnsFalse() {
        List<String> list = new ArrayList<>(Arrays.asList("only"));
        boolean result = MathUtilities.nextPermutation(list);
        assertFalse(result);
        assertEquals(Arrays.asList("only"), list);
    }

    @Test
    public void testNextPermutationLexicographicSequence() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3));
        List<List<Integer>> expected = Arrays.asList(
                Arrays.asList(1, 2, 3),
                Arrays.asList(1, 3, 2),
                Arrays.asList(2, 1, 3),
                Arrays.asList(2, 3, 1),
                Arrays.asList(3, 1, 2),
                Arrays.asList(3, 2, 1)
        );

        List<List<Integer>> actual = new ArrayList<>();
        // capture the initial permutation
        actual.add(new ArrayList<>(list));
        while (MathUtilities.nextPermutation(list)) {
            actual.add(new ArrayList<>(list));
        }

        assertEquals(expected, actual);
    }

    @Test
    public void testNextPermutationAfterLastReturnsFalseAndLeavesListUnchanged() {
        List<Character> list = new ArrayList<>(Arrays.asList('c', 'b', 'a')); // last permutation
        boolean result = MathUtilities.nextPermutation(list);
        assertFalse(result);
        assertEquals(Arrays.asList('c', 'b', 'a'), list);
    }

    @Test
    public void testNextPermutationWithRepeatedElements() {
        // The algorithm works with Comparable, repeated values are allowed.
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 1, 2));
        List<List<Integer>> expected = Arrays.asList(
                Arrays.asList(1, 1, 2),
                Arrays.asList(1, 2, 1),
                Arrays.asList(2, 1, 1)
        );

        List<List<Integer>> actual = new ArrayList<>();
        actual.add(new ArrayList<>(list));
        while (MathUtilities.nextPermutation(list)) {
            actual.add(new ArrayList<>(list));
        }

        assertEquals(expected, actual);
    }
}
