package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import org.junit.Test;

/**
 * JUnit‑4 test suite for {@link MathUtilities}.
 */
public class MathUtilitiesTest {

    /* -------------------------------------------------
     *  Basic functionality tests
     * ------------------------------------------------- */

    @Test
    public void testMinimumLong() {
        assertEquals(1L, MathUtilities.minimum(5L, 3L, 1L, 9L));
    }

    @Test
    public void testMaximumLong() {
        assertEquals(9L, MathUtilities.maximum(5L, 3L, 1L, 9L));
    }

    @Test
    public void testMinimumDouble() {
        assertEquals(1.2, MathUtilities.minimum(5.5, 3.3, 1.2, 9.9), 0.0);
    }

    @Test
    public void testMaximumDouble() {
        assertEquals(9.9, MathUtilities.maximum(5.5, 3.3, 1.2, 9.9), 0.0);
    }

    @Test
    public void testMinimumBigInteger() {
        BigInteger a = new BigInteger("12345678901234567890");
        BigInteger b = new BigInteger("-12345678901234567890");
        BigInteger c = BigInteger.ZERO;
        assertEquals(b, MathUtilities.minimum(a, b, c));
    }

    @Test
    public void testMaximumBigInteger() {
        BigInteger a = new BigInteger("12345678901234567890");
        BigInteger b = new BigInteger("-12345678901234567890");
        BigInteger c = BigInteger.ZERO;
        assertEquals(a, MathUtilities.maximum(a, b, c));
    }

    @Test
    public void testMinimumBigDecimal() {
        BigDecimal a = new BigDecimal("3.14");
        BigDecimal b = new BigDecimal("-2.71");
        BigDecimal c = new BigDecimal("0");
        assertEquals(b, MathUtilities.minimum(a, b, c));
    }

    @Test
    public void testMaximumBigDecimal() {
        BigDecimal a = new BigDecimal("3.14");
        BigDecimal b = new BigDecimal("-2.71");
        BigDecimal c = new BigDecimal("0");
        assertEquals(a, MathUtilities.maximum(a, b, c));
    }

    @Test
    public void testParseToMinimalNumericType_Long() {
        Number n = MathUtilities.parseToMinimalNumericType("12345");
        assertTrue(n instanceof Long);
        assertEquals(12345L, n);
    }

    @Test
    public void testParseToMinimalNumericType_Double() {
        Number n = MathUtilities.parseToMinimalNumericType("12.34");
        assertTrue(n instanceof Double);
        assertEquals(12.34, n.doubleValue(), 0.0);
    }

    @Test
    public void testParseToMinimalNumericType_BigDecimal() {
        Number n = MathUtilities.parseToMinimalNumericType("1e309"); // exceeds double range
        assertTrue(n instanceof BigDecimal);
        assertEquals(new BigDecimal("1e309"), n);
    }

    @Test
    public void testParseToMinimalNumericType_BigInteger() {
        Number n = MathUtilities.parseToMinimalNumericType("9999999999999999999999999999");
        assertTrue(n instanceof BigInteger);
        assertEquals(new BigInteger("9999999999999999999999999999"), n);
    }

    @Test
    public void testNextPermutationSimple() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3));
        List<List<Integer>> permutations = new ArrayList<>();
        permutations.add(new ArrayList<>(list)); // first (original) permutation
        while (MathUtilities.nextPermutation(list)) {
            permutations.add(new ArrayList<>(list));
        }
        assertEquals(6, permutations.size());
        assertEquals(Arrays.asList(3, 2, 1), permutations.get(5));
    }

    /* -------------------------------------------------
     *  Edge‑case / exception tests
     * ------------------------------------------------- */

    // ---- primitive var‑args empty array ----
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

    // ---- BigInteger null handling ----
    @Test(expected = IllegalArgumentException.class)
    public void testMinimumBigIntegerSingleNull() {
        MathUtilities.minimum((BigInteger) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMaximumBigIntegerSingleNull() {
        MathUtilities.maximum((BigInteger) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMinimumBigIntegerWithNullElement() {
        MathUtilities.minimum(BigInteger.ONE, null, BigInteger.TEN);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMaximumBigIntegerWithNullElement() {
        MathUtilities.maximum(BigInteger.ONE, null, BigInteger.TEN);
    }

    // ---- BigDecimal null handling ----
    @Test(expected = IllegalArgumentException.class)
    public void testMinimumBigDecimalSingleNull() {
        MathUtilities.minimum((BigDecimal) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMaximumBigDecimalSingleNull() {
        MathUtilities.maximum((BigDecimal) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMinimumBigDecimalWithNullElement() {
        MathUtilities.minimum(BigDecimal.ONE, null, BigDecimal.TEN);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMaximumBigDecimalWithNullElement() {
        MathUtilities.maximum(BigDecimal.ONE, null, BigDecimal.TEN);
    }

    // ---- parseToMinimalNumericType edge cases ----
    @Test(expected = IllegalArgumentException.class)
    public void testParseNullString() {
        MathUtilities.parseToMinimalNumericType(null);
    }

    @Test(expected = NumberFormatException.class)
    public void testParseInvalidString() {
        MathUtilities.parseToMinimalNumericType("not-a-number");
    }

    @Test
    public void testParseLongBoundaries() {
        Number max = MathUtilities.parseToMinimalNumericType(Long.toString(Long.MAX_VALUE));
        assertTrue(max instanceof Long);
        assertEquals(Long.MAX_VALUE, max.longValue());

        Number min = MathUtilities.parseToMinimalNumericType(Long.toString(Long.MIN_VALUE));
        assertTrue(min instanceof Long);
        assertEquals(Long.MIN_VALUE, min.longValue());
    }

    @Test
    public void testParseJustBeyondLongRange() {
        String beyond = new BigInteger(Long.MAX_VALUE).add(BigInteger.ONE).toString();
        Number n = MathUtilities.parseToMinimalNumericType(beyond);
        assertTrue(n instanceof BigInteger);
        assertEquals(new BigInteger(beyond), n);
    }

    @Test
    public void testParseDoubleWithExponentThatFitsDouble() {
        Number n = MathUtilities.parseToMinimalNumericType("1.23e10");
        assertTrue(n instanceof Double);
        assertEquals(1.23e10, n.doubleValue(), 0.0);
    }

    @Test
    public void testParseDoubleWithLargeExponentForBigDecimal() {
        Number n = MathUtilities.parseToMinimalNumericType("1.23e400");
        assertTrue(n instanceof BigDecimal);
        assertEquals(new BigDecimal("1.23e400"), n);
    }

    @Test
    public void testParseLeadingZerosAndSigns() {
        Number pos = MathUtilities.parseToMinimalNumericType("+000123");
        assertTrue(pos instanceof Long);
        assertEquals(123L, pos.longValue());

        Number neg = MathUtilities.parseToMinimalNumericType("-0000456");
        assertTrue(neg instanceof Long);
        assertEquals(-456L, neg.longValue());

        Number zero = MathUtilities.parseToMinimalNumericType("-0000");
        assertTrue(zero instanceof Long);
        assertEquals(0L, zero.longValue());
    }

    // ---- nextPermutation edge cases ----
    @Test(expected = IllegalArgumentException.class)
    public void testNextPermutationNullList() {
        MathUtilities.nextPermutation(null);
    }

    @Test
    public void testNextPermutationEmptyList() {
        List<Integer> empty = new ArrayList<>();
        assertFalse(MathUtilities.nextPermutation(empty));
    }

    @Test
    public void testNextPermutationSingleElement() {
        List<String> single = new ArrayList<>(Collections.singletonList("only"));
        assertFalse(MathUtilities.nextPermutation(single));
    }

    @Test
    public void testNextPermutationDescendingOrder() {
        List<Integer> desc = new ArrayList<>(Arrays.asList(3, 2, 1));
        assertFalse(MathUtilities.nextPermutation(desc));
    }
}
