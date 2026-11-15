package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Assert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Comprehensive JUnit‑4 test suite for {@link MathUtilities}.
 *
 * The tests cover:
 * <ul>
 *   <li>minimum / maximum for {@code long}, {@code double}, {@link BigInteger}, {@link BigDecimal}</li>
 *   <li>Null‑argument validation for the {@link BigInteger} and {@link BigDecimal} overloads</li>
 *   <li>parseToMinimalNumericType – all branches (Long, BigInteger, Double, BigDecimal) and sign/leading‑zero handling</li>
 *   <li>nextPermutation – correct permutation generation, termination condition and generic type handling</li>
 * </ul>
 */
public class MathUtilitiesTest
{
    /* ---------------------------------------------------------------------- */
    /*  minimum / maximum for primitive long                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testLongMinimumAndMaximumSimple()
    {
        long min = MathUtilities.minimum(5L, -3L, 12L, 0L);
        long max = MathUtilities.maximum(5L, -3L, 12L, 0L);
        Assert.assertEquals(-3L, min);
        Assert.assertEquals(12L, max);
    }

    @Test
    public void testLongMinimumAndMaximumSingleElement()
    {
        long value = 42L;
        Assert.assertEquals(value, MathUtilities.minimum(value));
        Assert.assertEquals(value, MathUtilities.maximum(value));
    }

    @Test
    public void testLongMinimumAndMaximumWithExtremeValues()
    {
        long min = MathUtilities.minimum(Long.MAX_VALUE, Long.MIN_VALUE, 0L);
        long max = MathUtilities.maximum(Long.MAX_VALUE, Long.MIN_VALUE, 0L);
        Assert.assertEquals(Long.MIN_VALUE, min);
        Assert.assertEquals(Long.MAX_VALUE, max);
    }

    /* ---------------------------------------------------------------------- */
    /*  minimum / maximum for primitive double                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDoubleMinimumAndMaximumSimple()
    {
        double min = MathUtilities.minimum(1.5, -2.3, 0.0, 4.2);
        double max = MathUtilities.maximum(1.5, -2.3, 0.0, 4.2);
        Assert.assertEquals(-2.3, min, 0.0);
        Assert.assertEquals(4.2, max, 0.0);
    }

    @Test
    public void testDoubleMinimumAndMaximumSingleElement()
    {
        double v = -123.456;
        Assert.assertEquals(v, MathUtilities.minimum(v), 0.0);
        Assert.assertEquals(v, MathUtilities.maximum(v), 0.0);
    }

    @Test
    public void testDoubleMinimumAndMaximumWithNaN()
    {
        // NaN propagates through Math.min/Math.max – the implementation does not guard against it.
        double nan = Double.NaN;
        double min = MathUtilities.minimum(1.0, nan, 2.0);
        double max = MathUtilities.maximum(1.0, nan, 2.0);
        Assert.assertTrue(Double.isNaN(min));
        Assert.assertTrue(Double.isNaN(max));
    }

    /* ---------------------------------------------------------------------- */
    /*  minimum / maximum for BigInteger                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testBigIntegerMinimumAndMaximumSimple()
    {
        BigInteger a = new BigInteger("12345678901234567890");
        BigInteger b = new BigInteger("-98765432109876543210");
        BigInteger c = BigInteger.ZERO;

        Assert.assertEquals(b, MathUtilities.minimum(a, b, c));
        Assert.assertEquals(a, MathUtilities.maximum(a, b, c));
    }

    @Test
    public void testBigIntegerSingleElement()
    {
        BigInteger bi = new BigInteger("42");
        Assert.assertEquals(bi, MathUtilities.minimum(bi));
        Assert.assertEquals(bi, MathUtilities.maximum(bi));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBigIntegerMinimumNullElement()
    {
        MathUtilities.minimum(BigInteger.ONE, null, BigInteger.TEN);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBigIntegerMaximumNullElement()
    {
        MathUtilities.maximum(null);
    }

    /* ---------------------------------------------------------------------- */
    /*  minimum / maximum for BigDecimal                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testBigDecimalMinimumAndMaximumSimple()
    {
        BigDecimal a = new BigDecimal("3.14159");
        BigDecimal b = new BigDecimal("-2.71828");
        BigDecimal c = new BigDecimal("0.0");

        Assert.assertEquals(b, MathUtilities.minimum(a, b, c));
        Assert.assertEquals(a, MathUtilities.maximum(a, b, c));
    }

    @Test
    public void testBigDecimalSingleElement()
    {
        BigDecimal bd = new BigDecimal("12345.6789");
        Assert.assertEquals(bd, MathUtilities.minimum(bd));
        Assert.assertEquals(bd, MathUtilities.maximum(bd));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBigDecimalMinimumNullElement()
    {
        MathUtilities.minimum(new BigDecimal("1.0"), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBigDecimalMaximumNullElement()
    {
        MathUtilities.maximum(null, new BigDecimal("2.0"));
    }

    /* ---------------------------------------------------------------------- */
    /*  parseToMinimalNumericType                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testParseToMinimalNumericTypeLongInsideRange()
    {
        Number n = MathUtilities.parseToMinimalNumericType("123456789");
        Assert.assertTrue(n instanceof Long);
        Assert.assertEquals(123456789L, n);
    }

    @Test
    public void testParseToMinimalNumericTypeLongWithLeadingZeros()
    {
        Number n = MathUtilities.parseToMinimalNumericType("00000123");
        Assert.assertTrue(n instanceof Long);
        Assert.assertEquals(123L, n);
    }

    @Test
    public void testParseToMinimalNumericTypeLongNegative()
    {
        Number n = MathUtilities.parseToMinimalNumericType("-42");
        Assert.assertTrue(n instanceof Long);
        Assert.assertEquals(-42L, n);
    }

    @Test
    public void testParseToMinimalNumericTypeBigIntegerBeyondLong()
    {
        // 20 digits – exceeds Long.MAX_VALUE (19 digits)
        String bigIntStr = "123456789012345678901";
        Number n = MathUtilities.parseToMinimalNumericType(bigIntStr);
        Assert.assertTrue(n instanceof BigInteger);
        Assert.assertEquals(new BigInteger(bigIntStr), n);
    }

    @Test
    public void testParseToMinimalNumericTypeDoubleSimple()
    {
        Number n = MathUtilities.parseToMinimalNumericType("3.14159");
        Assert.assertTrue(n instanceof Double);
        Assert.assertEquals(3.14159, (Double) n, 0.0);
    }

    @Test
    public void testParseToMinimalNumericTypeDoubleWithExponentWithinLimit()
    {
        Number n = MathUtilities.parseToMinimalNumericType("1.23e5");
        Assert.assertTrue(n instanceof Double);
        Assert.assertEquals(1.23e5, (Double) n, 0.0);
    }

    @Test
    public void testParseToMinimalNumericTypeBigDecimalWhenMantissaTooLarge()
    {
        // 18 digits mantissa → should fall back to BigDecimal
        String largeMantissa = "123456789012345678.0";
        Number n = MathUtilities.parseToMinimalNumericType(largeMantissa);
        Assert.assertTrue(n instanceof BigDecimal);
        Assert.assertEquals(new BigDecimal(largeMantissa), n);
    }

    @Test
    public void testParseToMinimalNumericTypeBigDecimalWhenExponentTooLarge()
    {
        // exponent > 307 forces BigDecimal
        Number n = MathUtilities.parseToMinimalNumericType("1.0e308");
        Assert.assertTrue(n instanceof BigDecimal);
        Assert.assertEquals(new BigDecimal("1.0e308"), n);
    }

    @Test
    public void testParseToMinimalNumericTypeNegativeZero()
    {
        Number n = MathUtilities.parseToMinimalNumericType("-0");
        // "-0" becomes "0" after leading‑zero removal; parsed as Long zero
        Assert.assertTrue(n instanceof Long);
        Assert.assertEquals(0L, n);
    }

    @Test
    public void testParseToMinimalNumericTypePositiveSign()
    {
        Number n = MathUtilities.parseToMinimalNumericType("+00123");
        Assert.assertTrue(n instanceof Long);
        Assert.assertEquals(123L, n);
    }

    /* ---------------------------------------------------------------------- */
    /*  nextPermutation                                                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testNextPermutationBasic()
    {
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
        actual.add(new ArrayList<>(list)); // initial state

        while (MathUtilities.nextPermutation(list))
        {
            actual.add(new ArrayList<>(list));
        }

        // Verify we generated exactly 6 permutations (3!).
        Assert.assertEquals(expected, actual);
        // After the final call, list should be in descending order and method returned false.
        Assert.assertFalse(MathUtilities.nextPermutation(list));
        Assert.assertEquals(expected.get(expected.size() - 1), list);
    }

    @Test
    public void testNextPermutationSingleElement()
    {
        List<String> list = new ArrayList<>(Arrays.asList("only"));
        // No next permutation should exist.
        Assert.assertFalse(MathUtilities.nextPermutation(list));
        Assert.assertEquals(Arrays.asList("only"), list);
    }

    @Test
    public void testNextPermutationWithRepeatedElements()
    {
        // The algorithm does not deduplicate; it treats each position as distinct.
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 1, 2));
        List<List<Integer>> permutations = new ArrayList<>();
        permutations.add(new ArrayList<>(list));

        while (MathUtilities.nextPermutation(list))
        {
            permutations.add(new ArrayList<>(list));
        }

        // For 3 elements, there are 6 permutations, even with duplicates.
        Assert.assertEquals(6, permutations.size());
        // The last permutation should be the descending order.
        Assert.assertEquals(Arrays.asList(2, 1, 1), list);
    }

    /* ---------------------------------------------------------------------- */
    /*  Constants sanity check                                                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testConstantsMatchLongBounds()
    {
        Assert.assertEquals(BigInteger.valueOf(Long.MIN_VALUE), MathUtilities.BIG_INT_LONG_MIN);
        Assert.assertEquals(BigInteger.valueOf(Long.MAX_VALUE), MathUtilities.BIG_INT_LONG_MAX);
        Assert.assertEquals(BigDecimal.valueOf(-Double.MAX_VALUE), MathUtilities.BIG_DEC_DOUBLE_MIN);
        Assert.assertEquals(BigDecimal.valueOf(Double.MAX_VALUE), MathUtilities.BIG_DEC_DOUBLE_MAX);
    }
}
