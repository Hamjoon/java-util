package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Assert;

import java.math.BigInteger;
import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * JUnit‑4 test suite for {@link MathUtilities}.
 * The tests cover normal functionality, edge cases and exception handling.
 */
public class MathUtilitiesTest {

    // -----------------------------------------------------------------
    // Basic functionality tests
    // -----------------------------------------------------------------
    @Test
    public void testMinimumLongBasic() {
        Assert.assertEquals(1L, MathUtilities.minimum(5L, 3L, 1L, 9L));
    }

    @Test
    public void testMaximumLongBasic() {
        Assert.assertEquals(9L, MathUtilities.maximum(5L, 3L, 1L, 9L));
    }

    @Test
    public void testMinimumDoubleBasic() {
        Assert.assertEquals(1.1, MathUtilities.minimum(5.5, 3.3, 1.1, 9.9), 0.0);
    }

    @Test
    public void testMaximumDoubleBasic() {
        Assert.assertEquals(9.9, MathUtilities.maximum(5.5, 3.3, 1.1, 9.9), 0.0);
    }

    @Test
    public void testMinimumBigIntegerBasic() {
        BigInteger a = new BigInteger("-100");
        BigInteger b = new BigInteger("0");
        BigInteger c = new BigInteger("50");
        Assert.assertEquals(a, MathUtilities.minimum(a, b, c));
    }

    @Test
    public void testMaximumBigIntegerBasic() {
        BigInteger a = new BigInteger("-100");
        BigInteger b = new BigInteger("0");
        BigInteger c = new BigInteger("50");
        Assert.assertEquals(c, MathUtilities.maximum(a, b, c));
    }

    @Test
    public void testMinimumBigDecimalBasic() {
        BigDecimal a = new BigDecimal("-10.5");
        BigDecimal b = new BigDecimal("0.0");
        BigDecimal c = new BigDecimal("3.14");
        Assert.assertEquals(a, MathUtilities.minimum(a, b, c));
    }

    @Test
    public void testMaximumBigDecimalBasic() {
        BigDecimal a = new BigDecimal("-10.5");
        BigDecimal b = new BigDecimal("0.0");
        BigDecimal c = new BigDecimal("3.14");
        Assert.assertEquals(c, MathUtilities.maximum(a, b, c));
    }

    // -----------------------------------------------------------------
    // Edge‑case / exception tests for primitive arrays
    // -----------------------------------------------------------------
    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testMinimumLongEmpty() {
        MathUtilities.minimum();
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testMaximumLongEmpty() {
        MathUtilities.maximum();
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testMinimumDoubleEmpty() {
        MathUtilities.minimum();
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testMaximumDoubleEmpty() {
        MathUtilities.maximum();
    }

    // -----------------------------------------------------------------
    // Edge‑case / exception tests for BigInteger methods
    // -----------------------------------------------------------------
    @Test(expected = IllegalArgumentException.class)
    public void testMinimumBigIntegerSingleNull() {
        MathUtilities.minimum((BigInteger) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMaximumBigIntegerSingleNull() {
        MathUtilities.maximum((BigInteger) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMinimumBigIntegerWithNullInArray() {
        MathUtilities.minimum(BigInteger.ONE, null, BigInteger.TEN);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMaximumBigIntegerWithNullInArray() {
        MathUtilities.maximum(BigInteger.ONE, null, BigInteger.TEN);
    }

    // -----------------------------------------------------------------
    // Edge‑case / exception tests for BigDecimal methods
    // -----------------------------------------------------------------
    @Test(expected = IllegalArgumentException.class)
    public void testMinimumBigDecimalSingleNull() {
        MathUtilities.minimum((BigDecimal) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMaximumBigDecimalSingleNull() {
        MathUtilities.maximum((BigDecimal) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMinimumBigDecimalWithNullInArray() {
        MathUtilities.minimum(BigDecimal.ONE, null, BigDecimal.TEN);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMaximumBigDecimalWithNullInArray() {
        MathUtilities.maximum(BigDecimal.ONE, null, BigDecimal.TEN);
    }

    // -----------------------------------------------------------------
    // Tests for parseToMinimalNumericType
    // -----------------------------------------------------------------
    @Test
    public void testParseLongWithinRange() {
        Number n = MathUtilities.parseToMinimalNumericType("12345");
        Assert.assertTrue(n instanceof Long);
        Assert.assertEquals(12345L, n);
    }

    @Test
    public void testParseLongWithLeadingZerosAndPlusSign() {
        Number n = MathUtilities.parseToMinimalNumericType("+0000123");
        Assert.assertTrue(n instanceof Long);
        Assert.assertEquals(123L, n);
    }

    @Test
    public void testParseLongNegativeWithLeadingZeros() {
        Number n = MathUtilities.parseToMinimalNumericType("-0000123");
        Assert.assertTrue(n instanceof Long);
        Assert.assertEquals(-123L, n);
    }

    @Test
    public void testParseLongBoundaryValues() {
        Number min = MathUtilities.parseToMinimalNumericType(Long.toString(Long.MIN_VALUE));
        Number max = MathUtilities.parseToMinimalNumericType(Long.toString(Long.MAX_VALUE));
        Assert.assertTrue(min instanceof Long);
        Assert.assertTrue(max instanceof Long);
        Assert.assertEquals(Long.MIN_VALUE, min);
        Assert.assertEquals(Long.MAX_VALUE, max);
    }

    @Test
    public void testParseBigIntegerBeyondLong() {
        String big = "9223372036854775808"; // Long.MAX_VALUE + 1
        Number n = MathUtilities.parseToMinimalNumericType(big);
        Assert.assertTrue(n instanceof BigInteger);
        Assert.assertEquals(new BigInteger(big), n);
    }

    @Test
    public void testParseDoubleWithinRange() {
        Number n = MathUtilities.parseToMinimalNumericType("3.14159");
        Assert.assertTrue(n instanceof Double);
        Assert.assertEquals(3.14159, (Double) n, 0.0);
    }

    @Test
    public void testParseDoubleWithExponentWithinLimit() {
        Number n = MathUtilities.parseToMinimalNumericType("1.23e3");
        Assert.assertTrue(n instanceof Double);
        Assert.assertEquals(1230.0, (Double) n, 0.0);
    }

    @Test
    public void testParseBigDecimalDueToPrecision() {
        String bigDec = "12345678901234567890.0"; // 20 digits → > Double mantissa
        Number n = MathUtilities.parseToMinimalNumericType(bigDec);
        Assert.assertTrue(n instanceof BigDecimal);
        Assert.assertEquals(new BigDecimal(bigDec), n);
    }

    @Test
    public void testParseBigDecimalDueToLargeExponent() {
        String bigExp = "1e309"; // exponent > 308 forces BigDecimal
        Number n = MathUtilities.parseToMinimalNumericType(bigExp);
        Assert.assertTrue(n instanceof BigDecimal);
        Assert.assertEquals(new BigDecimal(bigExp), n);
    }

    @Test(expected = NumberFormatException.class)
    public void testParseInvalidNumberThrows() {
        MathUtilities.parseToMinimalNumericType("not-a-number");
    }

    // -----------------------------------------------------------------
    // Tests for nextPermutation (package‑private, accessible because test is
    // in the same package)
    // -----------------------------------------------------------------
    @Test
    public void testNextPermutationNormalProgression() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3));
        boolean hasNext = MathUtilities.nextPermutation(list);
        Assert.assertTrue(hasNext);
        Assert.assertEquals(Arrays.asList(1, 3, 2), list);
    }

    @Test
    public void testNextPermutationLastPermutationReturnsFalse() {
        List<Integer> list = new ArrayList<>(Arrays.asList(3, 2, 1));
        boolean hasNext = MathUtilities.nextPermutation(list);
        Assert.assertFalse(hasNext);
        Assert.assertEquals(Arrays.asList(3, 2, 1), list);
    }

    @Test
    public void testNextPermutationSingleElement() {
        List<String> list = new ArrayList<>(Collections.singletonList("only"));
        boolean hasNext = MathUtilities.nextPermutation(list);
        Assert.assertFalse(hasNext);
        Assert.assertEquals(Collections.singletonList("only"), list);
    }

    @Test
    public void testNextPermutationEmptyList() {
        List<Integer> list = new ArrayList<>();
        boolean hasNext = MathUtilities.nextPermutation(list);
        Assert.assertFalse(hasNext);
        Assert.assertTrue(list.isEmpty());
    }
}
