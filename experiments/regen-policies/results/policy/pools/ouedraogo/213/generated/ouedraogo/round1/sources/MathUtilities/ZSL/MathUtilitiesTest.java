package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Assert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Comprehensive JUnit 4 test suite for {@link MathUtilities}.
 */
public class MathUtilitiesTest {

    /* ------------------------------------------------------------
     *  Tests for long minimum / maximum
     * ------------------------------------------------------------ */

    @Test
    public void testLongMinimumSingleElement() {
        long result = MathUtilities.minimum(42L);
        Assert.assertEquals(42L, result);
    }

    @Test
    public void testLongMinimumMultipleElements() {
        long result = MathUtilities.minimum(10L, -5L, 0L, 99L, Long.MIN_VALUE);
        Assert.assertEquals(Long.MIN_VALUE, result);
    }

    @Test
    public void testLongMaximumSingleElement() {
        long result = MathUtilities.maximum(7L);
        Assert.assertEquals(7L, result);
    }

    @Test
    public void testLongMaximumMultipleElements() {
        long result = MathUtilities.maximum(-10L, 0L, 5L, Long.MAX_VALUE, 123L);
        Assert.assertEquals(Long.MAX_VALUE, result);
    }

    /* ------------------------------------------------------------
     *  Tests for double minimum / maximum
     * ------------------------------------------------------------ */

    @Test
    public void testDoubleMinimumSingleElement() {
        double result = MathUtilities.minimum(3.14);
        Assert.assertEquals(3.14, result, 0.0);
    }

    @Test
    public void testDoubleMinimumMultipleElements() {
        double result = MathUtilities.minimum(1.5, -2.5, 0.0, Double.MAX_VALUE, -Double.MAX_VALUE);
        Assert.assertEquals(-Double.MAX_VALUE, result, 0.0);
    }

    @Test
    public void testDoubleMaximumSingleElement() {
        double result = MathUtilities.maximum(-0.1);
        Assert.assertEquals(-0.1, result, 0.0);
    }

    @Test
    public void testDoubleMaximumMultipleElements() {
        double result = MathUtilities.maximum(-1.0, 2.0, Double.MIN_VALUE, Double.MAX_VALUE);
        Assert.assertEquals(Double.MAX_VALUE, result, 0.0);
    }

    /* ------------------------------------------------------------
     *  Tests for BigInteger minimum / maximum
     * ------------------------------------------------------------ */

    @Test
    public void testBigIntegerMinimumSingleElement() {
        BigInteger bi = new BigInteger("12345678901234567890");
        Assert.assertEquals(bi, MathUtilities.minimum(bi));
    }

    @Test
    public void testBigIntegerMinimumMultipleElements() {
        BigInteger a = new BigInteger("-999999999999999999999");
        BigInteger b = BigInteger.ZERO;
        BigInteger c = new BigInteger("999999999999999999999");
        Assert.assertEquals(a, MathUtilities.minimum(a, b, c));
    }

    @Test
    public void testBigIntegerMaximumSingleElement() {
        BigInteger bi = new BigInteger("-12345678901234567890");
        Assert.assertEquals(bi, MathUtilities.maximum(bi));
    }

    @Test
    public void testBigIntegerMaximumMultipleElements() {
        BigInteger a = new BigInteger("-500");
        BigInteger b = new BigInteger("0");
        BigInteger c = new BigInteger("500");
        Assert.assertEquals(c, MathUtilities.maximum(a, b, c));
    }

    @Test
    public void testBigIntegerMinimumNullEntryThrows() {
        try {
            MathUtilities.minimum(new BigInteger("1"), null, new BigInteger("2"));
            Assert.fail("Expected IllegalArgumentException for null entry");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testBigIntegerMaximumNullEntryThrows() {
        try {
            MathUtilities.maximum(null);
            Assert.fail("Expected IllegalArgumentException for null entry");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /* ------------------------------------------------------------
     *  Tests for BigDecimal minimum / maximum
     * ------------------------------------------------------------ */

    @Test
    public void testBigDecimalMinimumSingleElement() {
        BigDecimal bd = new BigDecimal("3.14159");
        Assert.assertEquals(bd, MathUtilities.minimum(bd));
    }

    @Test
    public void testBigDecimalMinimumMultipleElements() {
        BigDecimal a = new BigDecimal("-1000.0001");
        BigDecimal b = new BigDecimal("0");
        BigDecimal c = new BigDecimal("1000.0001");
        Assert.assertEquals(a, MathUtilities.minimum(a, b, c));
    }

    @Test
    public void testBigDecimalMaximumSingleElement() {
        BigDecimal bd = new BigDecimal("-2.71828");
        Assert.assertEquals(bd, MathUtilities.maximum(bd));
    }

    @Test
    public void testBigDecimalMaximumMultipleElements() {
        BigDecimal a = new BigDecimal("-5");
        BigDecimal b = new BigDecimal("5");
        BigDecimal c = new BigDecimal("10");
        Assert.assertEquals(c, MathUtilities.maximum(a, b, c));
    }

    @Test
    public void testBigDecimalMinimumNullEntryThrows() {
        try {
            MathUtilities.minimum(new BigDecimal("1.0"), null);
            Assert.fail("Expected IllegalArgumentException for null BigDecimal");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testBigDecimalMaximumNullEntryThrows() {
        try {
            MathUtilities.maximum(null);
            Assert.fail("Expected IllegalArgumentException for null BigDecimal");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /* ------------------------------------------------------------
     *  Tests for parseToMinimalNumericType
     * ------------------------------------------------------------ */

    @Test
    public void testParseIntegerWithinLong() {
        Number n = MathUtilities.parseToMinimalNumericType("123456789");
        Assert.assertTrue(n instanceof Long);
        Assert.assertEquals(123456789L, n.longValue());
    }

    @Test
    public void testParseIntegerBeyondLong() {
        String big = "999999999999999999999999999999";
        Number n = MathUtilities.parseToMinimalNumericType(big);
        Assert.assertTrue(n instanceof BigInteger);
        Assert.assertEquals(new BigInteger(big), n);
    }

    @Test
    public void testParseNegativeIntegerWithLeadingZeros() {
        Number n = MathUtilities.parseToMinimalNumericType("-0000123");
        Assert.assertTrue(n instanceof Long);
        Assert.assertEquals(-123L, n.longValue());
    }

    @Test
    public void testParseZeroAndLeadingZeros() {
        Number n1 = MathUtilities.parseToMinimalNumericType("0");
        Number n2 = MathUtilities.parseToMinimalNumericType("0000");
        Assert.assertTrue(n1 instanceof Long);
        Assert.assertTrue(n2 instanceof Long);
        Assert.assertEquals(0L, n1.longValue());
        Assert.assertEquals(0L, n2.longValue());
    }

    @Test
    public void testParseDecimalWithinDouble() {
        Number n = MathUtilities.parseToMinimalNumericType("1.2345");
        Assert.assertTrue(n instanceof Double);
        Assert.assertEquals(1.2345, n.doubleValue(), 0.0);
    }

    @Test
    public void testParseDecimalWithSmallExponentWithinDouble() {
        Number n = MathUtilities.parseToMinimalNumericType("1.23e2");
        Assert.assertTrue(n instanceof Double);
        Assert.assertEquals(123.0, n.doubleValue(), 0.0);
    }

    @Test
    public void testParseDecimalWithLargeExponentRequiresBigDecimal() {
        Number n = MathUtilities.parseToMinimalNumericType("1.23e309");
        Assert.assertTrue(n instanceof BigDecimal);
        Assert.assertEquals(new BigDecimal("1.23e309"), n);
    }

    @Test
    public void testParseDecimalWithManyDigitsRequiresBigDecimal() {
        // 18 digits in mantissa -> exceeds the 16‑digit threshold for Double
        String manyDigits = "123456789012345678.9";
        Number n = MathUtilities.parseToMinimalNumericType(manyDigits);
        Assert.assertTrue(n instanceof BigDecimal);
        Assert.assertEquals(new BigDecimal(manyDigits), n);
    }

    @Test
    public void testParseInvalidNumberThrowsNumberFormatException() {
        try {
            MathUtilities.parseToMinimalNumericType("not-a-number");
            Assert.fail("Expected NumberFormatException");
        } catch (NumberFormatException e) {
            // expected
        }
    }

    @Test
    public void testParseNullStringThrowsIllegalArgumentException() {
        try {
            MathUtilities.parseToMinimalNumericType(null);
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /* ------------------------------------------------------------
     *  Tests for nextPermutation
     * ------------------------------------------------------------ */

    @Test
    public void testNextPermutationGeneratesAllPermutations() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3));
        List<List<Integer>> observed = new ArrayList<>();
        observed.add(new ArrayList<>(list)); // first permutation

        while (MathUtilities.nextPermutation(list)) {
            observed.add(new ArrayList<>(list));
        }

        List<List<Integer>> expected = Arrays.asList(
                Arrays.asList(1, 2, 3),
                Arrays.asList(1, 3, 2),
                Arrays.asList(2, 1, 3),
                Arrays.asList(2, 3, 1),
                Arrays.asList(3, 1, 2),
                Arrays.asList(3, 2, 1)
        );

        Assert.assertEquals(expected, observed);
    }

    @Test
    public void testNextPermutationReturnsFalseWhenNoMorePermutations() {
        List<Integer> list = new ArrayList<>(Arrays.asList(3, 2, 1));
        boolean hasNext = MathUtilities.nextPermutation(list);
        Assert.assertFalse(hasNext);
        // list should remain unchanged
        Assert.assertEquals(Arrays.asList(3, 2, 1), list);
    }

    @Test
    public void testNextPermutationWithSingleElementList() {
        List<String> list = new ArrayList<>(Arrays.asList("only"));
        boolean hasNext = MathUtilities.nextPermutation(list);
        Assert.assertFalse(hasNext);
        Assert.assertEquals(Arrays.asList("only"), list);
    }

    @Test
    public void testNextPermutationNullListThrowsIllegalArgumentException() {
        try {
            MathUtilities.nextPermutation(null);
            Assert.fail("Expected IllegalArgumentException for null list");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testNextPermutationModifiesListInPlace() {
        List<Integer> list = new ArrayList<>(Arrays.asList(2, 1, 3));
        // The next permutation after [2,1,3] is [2,3,1]
        boolean hasNext = MathUtilities.nextPermutation(list);
        Assert.assertTrue(hasNext);
        Assert.assertEquals(Arrays.asList(2, 3, 1), list);
    }
}
