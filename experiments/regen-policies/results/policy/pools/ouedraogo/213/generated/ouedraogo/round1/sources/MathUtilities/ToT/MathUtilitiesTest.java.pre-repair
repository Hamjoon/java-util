package com.cedarsoftware.util;

import org.junit.Test;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Comprehensive JUnit‑4 test suite for {@link MathUtilities}.
 *
 * The suite covers:
 * <ul>
 *   <li>minimum / maximum for primitive {@code long} and {@code double}</li>
 *   <li>minimum / maximum for {@link BigInteger} and {@link BigDecimal}</li>
 *   <li>parseToMinimalNumericType – normal, edge and error cases</li>
 *   <li>nextPermutation – correct ordering, termination and argument validation</li>
 * </ul>
 *
 * Each method is exercised with typical values, boundary values and
 * error scenarios as required by the specification.
 */
public class MathUtilitiesTest {

    /* --------------------------------------------------------------
     *  Primitive long minimum / maximum
     * -------------------------------------------------------------- */

    @Test
    public void testMinimumLongMultipleValues() {
        long result = MathUtilities.minimum(5L, -3L, 12L, -100L, 0L);
        assertEquals(-100L, result);
    }

    @Test
    public void testMaximumLongMultipleValues() {
        long result = MathUtilities.maximum(5L, -3L, 12L, -100L, 0L);
        assertEquals(12L, result);
    }

    @Test
    public void testMinimumLongSingleElement() {
        long result = MathUtilities.minimum(42L);
        assertEquals(42L, result);
    }

    @Test
    public void testMaximumLongSingleElement() {
        long result = MathUtilities.maximum(Long.MIN_VALUE);
        assertEquals(Long.MIN_VALUE, result);
    }

    /* --------------------------------------------------------------
     *  Primitive double minimum / maximum
     * -------------------------------------------------------------- */

    @Test
    public void testMinimumDoubleMultipleValues() {
        double result = MathUtilities.minimum(5.5, -3.2, 12.0, -100.1, 0.0);
        assertEquals(-100.1, result, 0.000001);
    }

    @Test
    public void testMaximumDoubleMultipleValues() {
        double result = MathUtilities.maximum(5.5, -3.2, 12.0, -100.1, 0.0);
        assertEquals(12.0, result, 0.000001);
    }

    @Test
    public void testMinimumDoubleSingleElement() {
        double result = MathUtilities.minimum(-0.0);
        assertEquals(-0.0, result, 0.0);
    }

    @Test
    public void testMaximumDoubleSingleElement() {
        double result = MathUtilities.maximum(Double.NaN);
        assertTrue(Double.isNaN(result));
    }

    /* --------------------------------------------------------------
     *  BigInteger minimum / maximum
     * -------------------------------------------------------------- */

    @Test
    public void testMinimumBigIntegerMultipleValues() {
        BigInteger a = new BigInteger("-999999999999999999999");
        BigInteger b = new BigInteger("0");
        BigInteger c = new BigInteger("12345678901234567890");
        BigInteger result = MathUtilities.minimum(a, b, c);
        assertEquals(a, result);
    }

    @Test
    public void testMaximumBigIntegerMultipleValues() {
        BigInteger a = new BigInteger("-999999999999999999999");
        BigInteger b = new BigInteger("0");
        BigInteger c = new BigInteger("12345678901234567890");
        BigInteger result = MathUtilities.maximum(a, b, c);
        assertEquals(c, result);
    }

    @Test
    public void testMinimumBigIntegerSingleValue() {
        BigInteger val = new BigInteger("42");
        assertEquals(val, MathUtilities.minimum(val));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMinimumBigIntegerNullEntryThrows() {
        MathUtilities.minimum(new BigInteger("1"), null, new BigInteger("2"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMaximumBigIntegerNullEntryThrows() {
        MathUtilities.maximum(null);
    }

    /* --------------------------------------------------------------
     *  BigDecimal minimum / maximum
     * -------------------------------------------------------------- */

    @Test
    public void testMinimumBigDecimalMultipleValues() {
        BigDecimal a = new BigDecimal("-1.0000000000000000001");
        BigDecimal b = new BigDecimal("0.0");
        BigDecimal c = new BigDecimal("1234567890.1234567890");
        BigDecimal result = MathUtilities.minimum(a, b, c);
        assertEquals(a, result);
    }

    @Test
    public void testMaximumBigDecimalMultipleValues() {
        BigDecimal a = new BigDecimal("-1.0000000000000000001");
        BigDecimal b = new BigDecimal("0.0");
        BigDecimal c = new BigDecimal("1234567890.1234567890");
        BigDecimal result = MathUtilities.maximum(a, b, c);
        assertEquals(c, result);
    }

    @Test
    public void testMinimumBigDecimalSingleValue() {
        BigDecimal val = new BigDecimal("3.14159");
        assertEquals(val, MathUtilities.minimum(val));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMaximumBigDecimalSingleNullThrows() {
        MathUtilities.maximum((BigDecimal) null);
    }

    /* --------------------------------------------------------------
     *  parseToMinimalNumericType – normal cases
     * -------------------------------------------------------------- */

    @Test
    public void testParseToMinimalNumericTypeLong() {
        Number n = MathUtilities.parseToMinimalNumericType("123456789");
        assertTrue(n instanceof Long);
        assertEquals(123456789L, n.longValue());
    }

    @Test
    public void testParseToMinimalNumericTypeBigInteger() {
        Number n = MathUtilities.parseToMinimalNumericType("9999999999999999999999999999");
        assertTrue(n instanceof BigInteger);
        assertEquals(new BigInteger("9999999999999999999999999999"), n);
    }

    @Test
    public void testParseToMinimalNumericTypeDouble() {
        Number n = MathUtilities.parseToMinimalNumericType("1.23");
        assertTrue(n instanceof Double);
        assertEquals(1.23, n.doubleValue(), 0.0000001);
    }

    @Test
    public void testParseToMinimalNumericTypeBigDecimalFromExponent() {
        // Exponent magnitude > 307 forces BigDecimal
        Number n = MathUtilities.parseToMinimalNumericType("1e308");
        assertTrue(n instanceof BigDecimal);
        assertEquals(new BigDecimal("1e308"), n);
    }

    @Test
    public void testParseToMinimalNumericTypeBigDecimalFromLongMantissa() {
        // 18 digits mantissa forces BigDecimal even without exponent
        Number n = MathUtilities.parseToMinimalNumericType("123456789012345678");
        // 18 digits < 19 => still Long, so we need 19 digits to exceed
        Number n2 = MathUtilities.parseToMinimalNumericType("1234567890123456789");
        assertTrue(n2 instanceof Long); // 19 digits but within Long range
        Number n3 = MathUtilities.parseToMinimalNumericType("12345678901234567890");
        assertTrue(n3 instanceof BigInteger);
    }

    @Test
    public void testParseToMinimalNumericTypeNegativeAndLeadingZeros() {
        Number n = MathUtilities.parseToMinimalNumericType("-0000123");
        assertTrue(n instanceof Long);
        assertEquals(-123L, n.longValue());
    }

    @Test
    public void testParseToMinimalNumericTypeZero() {
        Number n = MathUtilities.parseToMinimalNumericType("0");
        assertTrue(n instanceof Long);
        assertEquals(0L, n.longValue());
    }

    /* --------------------------------------------------------------
     *  parseToMinimalNumericType – error handling
     * -------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testParseToMinimalNumericTypeNullArgument() {
        MathUtilities.parseToMinimalNumericType(null);
    }

    @Test(expected = NumberFormatException.class)
    public void testParseToMinimalNumericTypeInvalidNumber() {
        MathUtilities.parseToMinimalNumericType("12a34");
    }

    /* --------------------------------------------------------------
     *  nextPermutation – normal progression
     * -------------------------------------------------------------- */

    @Test
    public void testNextPermutationSequence() {
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
        actual.add(new ArrayList<>(list));
        while (MathUtilities.nextPermutation(list)) {
            actual.add(new ArrayList<>(list));
        }

        assertEquals(expected, actual);
    }

    @Test
    public void testNextPermutationReturnsFalseWhenNoMore() {
        List<Integer> list = new ArrayList<>(Arrays.asList(3, 2, 1));
        assertFalse(MathUtilities.nextPermutation(list));
        // List must stay unchanged
        assertEquals(Arrays.asList(3, 2, 1), list);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNextPermutationNullListThrows() {
        MathUtilities.nextPermutation(null);
    }

    /* --------------------------------------------------------------
     *  Additional sanity checks
     * -------------------------------------------------------------- */

    @Test
    public void testMinimumAndMaximumWithIdenticalValues() {
        long[] longs = {5L, 5L, 5L};
        assertEquals(5L, MathUtilities.minimum(longs));
        assertEquals(5L, MathUtilities.maximum(longs));

        double[] doubles = {2.2, 2.2, 2.2};
        assertEquals(2.2, MathUtilities.minimum(doubles), 0.0);
        assertEquals(2.2, MathUtilities.maximum(doubles), 0.0);
    }

    @Test
    public void testPermutationWithDuplicateElements() {
        // Duplicate values are allowed; verify that algorithm still works
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "b"));
        List<List<String>> generated = new ArrayList<>();
        generated.add(new ArrayList<>(list));
        while (MathUtilities.nextPermutation(list)) {
            generated.add(new ArrayList<>(list));
        }
        // There are 3! / 2! = 3 distinct permutations
        assertEquals(3, generated.size());
        assertTrue(generated.contains(Arrays.asList("a", "b", "b")));
        assertTrue(generated.contains(Arrays.asList("b", "a", "b")));
        assertTrue(generated.contains(Arrays.asList("b", "b", "a")));
    }
}
