package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Assert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

public class MathUtilitiesTest {

    // -----------------------------------------------------------------
    //  Basic functionality tests
    // -----------------------------------------------------------------
    @Test
    public void testMinimumLong() {
        Assert.assertEquals(1L, MathUtilities.minimum(5L, 3L, 1L, 9L));
    }

    @Test
    public void testMaximumLong() {
        Assert.assertEquals(9L, MathUtilities.maximum(5L, 3L, 1L, 9L));
    }

    @Test
    public void testMinimumDouble() {
        Assert.assertEquals(1.5, MathUtilities.minimum(2.5, 1.5, 3.0), 0.0);
    }

    @Test
    public void testMaximumDouble() {
        Assert.assertEquals(3.0, MathUtilities.maximum(2.5, 1.5, 3.0), 0.0);
    }

    @Test
    public void testMinimumBigInteger() {
        BigInteger a = new BigInteger("12345678901234567890");
        BigInteger b = new BigInteger("9876543210987654321");
        Assert.assertEquals(b, MathUtilities.minimum(a, b));
    }

    @Test
    public void testMaximumBigInteger() {
        BigInteger a = new BigInteger("12345678901234567890");
        BigInteger b = new BigInteger("9876543210987654321");
        Assert.assertEquals(a, MathUtilities.maximum(a, b));
    }

    @Test
    public void testMinimumBigDecimal() {
        BigDecimal a = new BigDecimal("1.23");
        BigDecimal b = new BigDecimal("4.56");
        Assert.assertEquals(a, MathUtilities.minimum(a, b));
    }

    @Test
    public void testMaximumBigDecimal() {
        BigDecimal a = new BigDecimal("1.23");
        BigDecimal b = new BigDecimal("4.56");
        Assert.assertEquals(b, MathUtilities.maximum(a, b));
    }

    @Test
    public void testParseToMinimalNumericType_Long() {
        Number n = MathUtilities.parseToMinimalNumericType("12345");
        Assert.assertTrue(n instanceof Long);
        Assert.assertEquals(12345L, n);
    }

    @Test
    public void testParseToMinimalNumericType_Double() {
        Number n = MathUtilities.parseToMinimalNumericType("12.34");
        Assert.assertTrue(n instanceof Double);
        Assert.assertEquals(12.34, n.doubleValue(), 0.0);
    }

    @Test
    public void testParseToMinimalNumericType_BigInteger() {
        Number n = MathUtilities.parseToMinimalNumericType("999999999999999999999999");
        Assert.assertTrue(n instanceof BigInteger);
        Assert.assertEquals(new BigInteger("999999999999999999999999"), n);
    }

    @Test
    public void testParseToMinimalNumericType_BigDecimal() {
        Number n = MathUtilities.parseToMinimalNumericType("1e309");
        Assert.assertTrue(n instanceof BigDecimal);
        Assert.assertEquals(new BigDecimal("1e309"), n);
    }

    @Test
    public void testNextPermutationSimple() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3));
        Assert.assertTrue(MathUtilities.nextPermutation(list));
        Assert.assertEquals(Arrays.asList(1, 3, 2), list);
    }

    // -----------------------------------------------------------------
    //  Edge‑case & exception tests
    // -----------------------------------------------------------------
    // minimum / maximum long
    @Test
    public void testMinimumLongSingleElement() {
        Assert.assertEquals(42L, MathUtilities.minimum(42L));
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testMinimumLongEmptyArray() {
        MathUtilities.minimum();
    }

    // minimum / maximum double
    @Test
    public void testMaximumDoubleSingleElement() {
        Assert.assertEquals(3.14, MathUtilities.maximum(3.14), 0.0);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testMaximumDoubleEmptyArray() {
        MathUtilities.maximum();
    }

    // BigInteger overloads
    @Test
    public void testMinimumBigIntegerSingleNonNull() {
        BigInteger bi = new BigInteger("123");
        Assert.assertEquals(bi, MathUtilities.minimum(bi));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMinimumBigIntegerSingleNull() {
        MathUtilities.minimum((BigInteger) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMinimumBigIntegerWithNullEntry() {
        MathUtilities.minimum(new BigInteger("10"), null, new BigInteger("5"));
    }

    // BigDecimal overloads
    @Test
    public void testMaximumBigDecimalSingleNonNull() {
        BigDecimal bd = new BigDecimal("9.99");
        Assert.assertEquals(bd, MathUtilities.maximum(bd));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMaximumBigDecimalSingleNull() {
        MathUtilities.maximum((BigDecimal) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMaximumBigDecimalWithNullEntry() {
        MathUtilities.maximum(new BigDecimal("1.0"), null);
    }

    // parseToMinimalNumericType edge cases
    @Test(expected = IllegalArgumentException.class)
    public void testParseNullString() {
        MathUtilities.parseToMinimalNumericType(null);
    }

    @Test(expected = NumberFormatException.class)
    public void testParseInvalidString() {
        MathUtilities.parseToMinimalNumericType("abc123");
    }

    @Test
    public void testParseLeadingZeros() {
        Number n = MathUtilities.parseToMinimalNumericType("00000123");
        Assert.assertTrue(n instanceof Long);
        Assert.assertEquals(123L, n);
    }

    @Test
    public void testParseNegativeZero() {
        Number n = MathUtilities.parseToMinimalNumericType("-0");
        Assert.assertTrue(n instanceof Long);
        Assert.assertEquals(0L, n);
    }

    @Test
    public void testParseLongBoundary() {
        Number max = MathUtilities.parseToMinimalNumericType(Long.toString(Long.MAX_VALUE));
        Number min = MathUtilities.parseToMinimalNumericType(Long.toString(Long.MIN_VALUE));
        Assert.assertTrue(max instanceof Long);
        Assert.assertTrue(min instanceof Long);
        Assert.assertEquals(Long.MAX_VALUE, max);
        Assert.assertEquals(Long.MIN_VALUE, min);
    }

    @Test
    public void testParseJustOutsideLong() {
        Number n = MathUtilities.parseToMinimalNumericType("9223372036854775808");
        Assert.assertTrue(n instanceof BigInteger);
        Assert.assertEquals(new BigInteger("9223372036854775808"), n);
    }

    @Test
    public void testParseDoubleWithinPrecision() {
        Number n = MathUtilities.parseToMinimalNumericType("1.234567890123456");
        Assert.assertTrue(n instanceof Double);
        Assert.assertEquals(1.234567890123456, n.doubleValue(), 0.0);
    }

    @Test
    public void testParseDoubleTooPrecise() {
        Number n = MathUtilities.parseToMinimalNumericType("1.2345678901234567");
        Assert.assertTrue(n instanceof BigDecimal);
        Assert.assertEquals(new BigDecimal("1.2345678901234567"), n);
    }

    @Test
    public void testParseExponentBeyondDouble() {
        Number n = MathUtilities.parseToMinimalNumericType("1e309");
        Assert.assertTrue(n instanceof BigDecimal);
        Assert.assertEquals(new BigDecimal("1e309"), n);
    }

    // nextPermutation edge cases
    @Test(expected = IllegalArgumentException.class)
    public void testNextPermutationNullList() {
        MathUtilities.nextPermutation(null);
    }

    @Test
    public void testNextPermutationEmptyList() {
        List<Integer> list = new ArrayList<>();
        Assert.assertFalse(MathUtilities.nextPermutation(list));
        Assert.assertTrue(list.isEmpty());
    }

    @Test
    public void testNextPermutationSingletonList() {
        List<String> list = new ArrayList<>(Collections.singletonList("only"));
        Assert.assertFalse(MathUtilities.nextPermutation(list));
        Assert.assertEquals(Collections.singletonList("only"), list);
    }

    @Test
    public void testNextPermutationFullCycle() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3));
        List<List<Integer>> permutations = new ArrayList<>();
        permutations.add(new ArrayList<>(list)); // first (original) permutation
        while (MathUtilities.nextPermutation(list)) {
            permutations.add(new ArrayList<>(list));
        }
        // 3! = 6 permutations expected
        Assert.assertEquals(6, permutations.size());
        // Last permutation should be descending order
        Assert.assertEquals(Arrays.asList(3, 2, 1), permutations.get(permutations.size() - 1));
        // No further permutations
        Assert.assertFalse(MathUtilities.nextPermutation(list));
    }
}
