package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class MathUtilitiesTest {

    /* -------------------------------------------------
     *  PUBLIC METHOD SIGNATURES (extracted from MathUtilities)
     * -------------------------------------------------
     * public static long   minimum(long... values)
     * public static long   maximum(long... values)
     * public static double minimum(double... values)
     * public static double maximum(double... values)
     * public static BigInteger minimum(BigInteger... values)
     * public static BigInteger maximum(BigInteger... values)
     * public static BigDecimal  minimum(BigDecimal... values)
     * public static BigDecimal  maximum(BigDecimal... values)
     * public static Number parseToMinimalNumericType(String numStr)
     * static <T extends Comparable<? super T>> boolean nextPermutation(List<T> list)
     */

    /* -------------------------------------------------
     *  BASIC FUNCTIONALITY TESTS
     * ------------------------------------------------- */

    @Test
    public void testMinimumLongBasic() {
        assertEquals(1L, MathUtilities.minimum(5L, 3L, 1L, 9L));
        assertEquals(-10L, MathUtilities.minimum(-10L, -5L, 0L));
        assertEquals(Long.MIN_VALUE, MathUtilities.minimum(Long.MAX_VALUE, Long.MIN_VALUE));
        // single element
        assertEquals(42L, MathUtilities.minimum(42L));
    }

    @Test
    public void testMaximumLongBasic() {
        assertEquals(9L, MathUtilities.maximum(5L, 3L, 1L, 9L));
        assertEquals(0L, MathUtilities.maximum(-10L, -5L, 0L));
        assertEquals(Long.MAX_VALUE, MathUtilities.maximum(Long.MAX_VALUE, Long.MIN_VALUE));
        // single element
        assertEquals(42L, MathUtilities.maximum(42L));
    }

    @Test
    public void testMinimumDoubleBasic() {
        assertEquals(1.0, MathUtilities.minimum(5.5, 3.2, 1.0, 9.8), 0.0);
        assertEquals(-10.5, MathUtilities.minimum(-10.5, -5.2, 0.0), 0.0);
        // single element
        assertEquals(42.42, MathUtilities.minimum(42.42), 0.0);
    }

    @Test
    public void testMaximumDoubleBasic() {
        assertEquals(9.8, MathUtilities.maximum(5.5, 3.2, 1.0, 9.8), 0.0);
        assertEquals(0.0, MathUtilities.maximum(-10.5, -5.2, 0.0), 0.0);
        // single element
        assertEquals(42.42, MathUtilities.maximum(42.42), 0.0);
    }

    @Test
    public void testMinimumBigIntegerBasic() {
        BigInteger a = new BigInteger("12345678901234567890");
        BigInteger b = new BigInteger("98765432109876543210");
        BigInteger c = new BigInteger("-12345678901234567890");
        assertEquals(c, MathUtilities.minimum(a, b, c));
        // single element
        assertEquals(a, MathUtilities.minimum(a));
    }

    @Test
    public void testMaximumBigIntegerBasic() {
        BigInteger a = new BigInteger("12345678901234567890");
        BigInteger b = new BigInteger("98765432109876543210");
        BigInteger c = new BigInteger("-12345678901234567890");
        assertEquals(b, MathUtilities.maximum(a, b, c));
        // single element
        assertEquals(a, MathUtilities.maximum(a));
    }

    @Test
    public void testMinimumBigDecimalBasic() {
        BigDecimal a = new BigDecimal("1.2345");
        BigDecimal b = new BigDecimal("-2.3456");
        BigDecimal c = new BigDecimal("0.0001");
        assertEquals(b, MathUtilities.minimum(a, b, c));
        // single element
        assertEquals(a, MathUtilities.minimum(a));
    }

    @Test
    public void testMaximumBigDecimalBasic() {
        BigDecimal a = new BigDecimal("1.2345");
        BigDecimal b = new BigDecimal("-2.3456");
        BigDecimal c = new BigDecimal("0.0001");
        assertEquals(a, MathUtilities.maximum(a, b, c));
        // single element
        assertEquals(a, MathUtilities.maximum(a));
    }

    @Test
    public void testParseToMinimalNumericTypeBasic() {
        // Long within range
        Number n1 = MathUtilities.parseToMinimalNumericType("12345");
        assertTrue(n1 instanceof Long);
        assertEquals(12345L, n1.longValue());

        // BigInteger outside Long range
        Number n2 = MathUtilities.parseToMinimalNumericType("9223372036854775808"); // Long.MAX_VALUE + 1
        assertTrue(n2 instanceof BigInteger);
        assertEquals(new BigInteger("9223372036854775808"), n2);

        // Double parsing
        Number n3 = MathUtilities.parseToMinimalNumericType("3.14159");
        assertTrue(n3 instanceof Double);
        assertEquals(3.14159, n3.doubleValue(), 0.0);

        // BigDecimal fallback (many mantissa digits)
        Number n4 = MathUtilities.parseToMinimalNumericType("0.1234567890123456789");
        assertTrue(n4 instanceof BigDecimal);
        assertEquals(new BigDecimal("0.1234567890123456789"), n4);
    }

    @Test
    public void testNextPermutationBasic() {
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
        actual.add(new ArrayList<>(list)); // first permutation (original)

        while (MathUtilities.nextPermutation(list)) {
            actual.add(new ArrayList<>(list));
        }

        assertEquals(expected, actual);
        // after last permutation, method should return false and list stays at the last permutation
        assertFalse(MathUtilities.nextPermutation(list));
        assertEquals(expected.get(expected.size() - 1), list);
    }

    /* -------------------------------------------------
     *  EDGE CASE & EXCEPTION TESTS
     * ------------------------------------------------- */

    // ---- minimum / maximum for primitive arrays: empty array handling ----
    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testMinimumLongEmptyArray() {
        MathUtilities.minimum(new long[0]);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testMaximumLongEmptyArray() {
        MathUtilities.maximum(new long[0]);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testMinimumDoubleEmptyArray() {
        MathUtilities.minimum(new double[0]);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testMaximumDoubleEmptyArray() {
        MathUtilities.maximum(new double[0]);
    }

    // ---- BigInteger null handling ----
    @Test(expected = IllegalArgumentException.class)
    public void testMinimumBigIntegerNullSingle() {
        MathUtilities.minimum((BigInteger) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMaximumBigIntegerNullSingle() {
        MathUtilities.maximum((BigInteger) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMinimumBigIntegerNullInArray() {
        MathUtilities.minimum(BigInteger.ONE, null, BigInteger.TEN);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMaximumBigIntegerNullInArray() {
        MathUtilities.maximum(BigInteger.ONE, null, BigInteger.TEN);
    }

    // ---- BigDecimal null handling ----
    @Test(expected = IllegalArgumentException.class)
    public void testMinimumBigDecimalNullSingle() {
        MathUtilities.minimum((BigDecimal) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMaximumBigDecimalNullSingle() {
        MathUtilities.maximum((BigDecimal) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMinimumBigDecimalNullInArray() {
        MathUtilities.minimum(BigDecimal.ONE, null, BigDecimal.TEN);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMaximumBigDecimalNullInArray() {
        MathUtilities.maximum(BigDecimal.ONE, null, BigDecimal.TEN);
    }

    // ---- Double edge cases (NaN, Infinity) ----
    @Test
    public void testMinimumDoubleWithNaN() {
        double result = MathUtilities.minimum(Double.NaN, 5.0);
        assertTrue(Double.isNaN(result));
    }

    @Test
    public void testMaximumDoubleWithInfinity() {
        double result = MathUtilities.maximum(Double.NEGATIVE_INFINITY, 10.0);
        assertEquals(10.0, result, 0.0);
    }

    // ---- parseToMinimalNumericType edge cases ----
    @Test
    public void testParseLeadingZerosAndSigns() {
        Number n1 = MathUtilities.parseToMinimalNumericType("0000123");
        assertTrue(n1 instanceof Long);
        assertEquals(123L, n1.longValue());

        Number n2 = MathUtilities.parseToMinimalNumericType("-0000123");
        assertTrue(n2 instanceof Long);
        assertEquals(-123L, n2.longValue());

        Number n3 = MathUtilities.parseToMinimalNumericType("+0000123");
        assertTrue(n3 instanceof Long);
        assertEquals(123L, n3.longValue());
    }

    @Test
    public void testParseScientificNotationWithinDoubleRange() {
        Number n = MathUtilities.parseToMinimalNumericType("1.23e5");
        assertTrue(n instanceof Double);
        assertEquals(123000.0, n.doubleValue(), 0.0);
    }

    @Test
    public void testParseScientificNotationBeyondDoubleRange() {
        Number n = MathUtilities.parseToMinimalNumericType("1e309"); // exceeds Double.MAX_EXPONENT
        assertTrue(n instanceof BigDecimal);
        assertEquals(new BigDecimal("1e309"), n);
    }

    @Test
    public void testParseVeryLongIntegerExactlyLongMax() {
        String maxLong = Long.toString(Long.MAX_VALUE);
        Number n = MathUtilities.parseToMinimalNumericType(maxLong);
        assertTrue(n instanceof Long);
        assertEquals(Long.MAX_VALUE, n.longValue());
    }

    @Test
    public void testParseVeryLongIntegerJustAboveLongMax() {
        // Long.MAX_VALUE + 1
        String justAbove = new BigInteger(Long.toString(Long.MAX_VALUE)).add(BigInteger.ONE).toString();
        Number n = MathUtilities.parseToMinimalNumericType(justAbove);
        assertTrue(n instanceof BigInteger);
        assertEquals(new BigInteger(justAbove), n);
    }

    @Test
    public void testParseDecimalWithManyDigitsFallsBackToBigDecimal() {
        // 18 mantissa digits -> should be BigDecimal (threshold is <17 for Double)
        Number n = MathUtilities.parseToMinimalNumericType("0.123456789012345678");
        assertTrue(n instanceof BigDecimal);
        assertEquals(new BigDecimal("0.123456789012345678"), n);
    }

    // ---- nextPermutation edge cases ----
    @Test
    public void testNextPermutationSingleElement() {
        List<Integer> list = Collections.singletonList(42);
        assertFalse(MathUtilities.nextPermutation(list));
        assertEquals(Collections.singletonList(42), list);
    }

    @Test
    public void testNextPermutationEmptyList() {
        List<Integer> list = new ArrayList<>();
        assertFalse(MathUtilities.nextPermutation(list));
        assertTrue(list.isEmpty());
    }

    @Test
    public void testNextPermutationAlreadyLastPermutation() {
        List<Integer> list = new ArrayList<>(Arrays.asList(3, 2, 1));
        assertFalse(MathUtilities.nextPermutation(list));
        // list should remain unchanged
        assertEquals(Arrays.asList(3, 2, 1), list);
    }
}
