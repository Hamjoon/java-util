package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link MathUtilities}.
 *
 * <p>All public static methods are exercised, covering normal operation,
 * boundary conditions and the documented exception paths.</p>
 */
public class MathUtilitiesTest {

    /* ---------------------------------------------------------------------- */
    /* Minimum / Maximum for primitive long                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testLongMinimumSimple() {
        assertEquals(1L, MathUtilities.minimum(5L, 3L, 1L, 9L));
        assertEquals(-10L, MathUtilities.minimum(-10L, 0L, 7L));
    }

    @Test
    public void testLongMaximumSimple() {
        assertEquals(9L, MathUtilities.maximum(5L, 3L, 1L, 9L));
        assertEquals(0L, MathUtilities.maximum(-10L, 0L, -7L));
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testLongMinimumEmptyArray() {
        MathUtilities.minimum();   // should throw because values[0] is accessed
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testLongMaximumEmptyArray() {
        MathUtilities.maximum();   // same reasoning as above
    }

    /* ---------------------------------------------------------------------- */
    /* Minimum / Maximum for primitive double                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDoubleMinimumSimple() {
        assertEquals(-5.5, MathUtilities.minimum(2.2, -5.5, 3.3), 0.0);
        assertEquals(0.0, MathUtilities.minimum(0.0, 0.0, 0.0), 0.0);
    }

    @Test
    public void testDoubleMaximumSimple() {
        assertEquals(8.8, MathUtilities.maximum(2.2, -5.5, 8.8), 0.0);
        assertEquals(-1.1, MathUtilities.maximum(-1.1, -9.9, -10.0), 0.0);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testDoubleMinimumEmptyArray() {
        MathUtilities.minimum();   // triggers out‑of‑bounds on values[0]
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testDoubleMaximumEmptyArray() {
        MathUtilities.maximum();   // triggers out‑of‑bounds on values[0]
    }

    /* ---------------------------------------------------------------------- */
    /* Minimum / Maximum for BigInteger                                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testBigIntegerMinimumAndMaximum() {
        BigInteger a = new BigInteger("-12345678901234567890");
        BigInteger b = new BigInteger("0");
        BigInteger c = new BigInteger("12345678901234567890");

        assertEquals(a, MathUtilities.minimum(a, b, c));
        assertEquals(c, MathUtilities.maximum(a, b, c));
    }

    @Test
    public void testBigIntegerSingleElement() {
        BigInteger val = new BigInteger("42");
        assertEquals(val, MathUtilities.minimum(val));
        assertEquals(val, MathUtilities.maximum(val));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBigIntegerMinimumNullElement() {
        MathUtilities.minimum(new BigInteger("1"), null, new BigInteger("2"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBigIntegerMaximumNullElement() {
        MathUtilities.maximum(null);
    }

    /* ---------------------------------------------------------------------- */
    /* Minimum / Maximum for BigDecimal                                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testBigDecimalMinimumAndMaximum() {
        BigDecimal a = new BigDecimal("-0.0001");
        BigDecimal b = new BigDecimal("0");
        BigDecimal c = new BigDecimal("9999999999.9999");

        assertEquals(a, MathUtilities.minimum(a, b, c));
        assertEquals(c, MathUtilities.maximum(a, b, c));
    }

    @Test
    public void testBigDecimalSingleElement() {
        BigDecimal val = new BigDecimal("3.14159");
        assertEquals(val, MathUtilities.minimum(val));
        assertEquals(val, MathUtilities.maximum(val));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBigDecimalMinimumNullElement() {
        MathUtilities.minimum(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBigDecimalMaximumNullElement() {
        MathUtilities.maximum(new BigDecimal("1.0"), null);
    }

    /* ---------------------------------------------------------------------- */
    /* parseToMinimalNumericType                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testParseLongWithinRange() {
        Number n = MathUtilities.parseToMinimalNumericType("12345");
        assertTrue(n instanceof Long);
        assertEquals(12345L, n);
    }

    @Test
    public void testParseLongWithLeadingZeros() {
        Number n = MathUtilities.parseToMinimalNumericType("00000123");
        assertTrue(n instanceof Long);
        assertEquals(123L, n);
    }

    @Test
    public void testParseLongNegative() {
        Number n = MathUtilities.parseToMinimalNumericType("-987");
        assertTrue(n instanceof Long);
        assertEquals(-987L, n);
    }

    @Test
    public void testParseBigIntegerOutsideLongRange() {
        // 20 decimal digits > Long.MAX_VALUE (19 digits)
        String big = "99999999999999999999";
        Number n = MathUtilities.parseToMinimalNumericType(big);
        assertTrue(n instanceof BigInteger);
        assertEquals(new BigInteger(big), n);
    }

    @Test
    public void testParseDoubleSimple() {
        Number n = MathUtilities.parseToMinimalNumericType("3.1415");
        assertTrue(n instanceof Double);
        assertEquals(3.1415, n);
    }

    @Test
    public void testParseDoubleWithSmallExponent() {
        Number n = MathUtilities.parseToMinimalNumericType("1e2");
        assertTrue(n instanceof Double);
        assertEquals(100.0, n);
    }

    @Test
    public void testParseBigDecimalDueToLargeExponent() {
        Number n = MathUtilities.parseToMinimalNumericType("1e308");
        assertTrue(n instanceof BigDecimal);
        assertEquals(new BigDecimal("1e308"), n);
    }

    @Test
    public void testParseBigDecimalDueToManyMantissaDigits() {
        // 18 digits before decimal point → mantissaSize >= 17, forces BigDecimal
        Number n = MathUtilities.parseToMinimalNumericType("123456789012345678.9");
        assertTrue(n instanceof BigDecimal);
        assertEquals(new BigDecimal("123456789012345678.9"), n);
    }

    @Test
    public void testParsePositiveSignPreserved() {
        Number n = MathUtilities.parseToMinimalNumericType("+007");
        assertTrue(n instanceof Long);
        assertEquals(7L, n);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseNullString() {
        MathUtilities.parseToMinimalNumericType(null);
    }

    @Test(expected = NumberFormatException.class)
    public void testParseInvalidNumber() {
        MathUtilities.parseToMinimalNumericType("not-a-number");
    }

    /* ---------------------------------------------------------------------- */
    /* nextPermutation                                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testNextPermutationTypicalSequence() {
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
        // first permutation is the original list
        actual.add(new ArrayList<>(list));
        while (MathUtilities.nextPermutation(list)) {
            actual.add(new ArrayList<>(list));
        }

        assertEquals(expected, actual);
        // after the last permutation the method should have returned false
        assertFalse(MathUtilities.nextPermutation(list));
        // list should now be in descending order (the last permutation)
        assertEquals(expected.get(expected.size() - 1), list);
    }

    @Test
    public void testNextPermutationSingleElement() {
        List<String> list = new ArrayList<>(Collections.singletonList("only"));
        assertFalse(MathUtilities.nextPermutation(list));
        assertEquals(Collections.singletonList("only"), list);
    }

    @Test
    public void testNextPermutationAlreadyHighest() {
        List<Integer> list = new ArrayList<>(Arrays.asList(3, 2, 1));
        assertFalse(MathUtilities.nextPermutation(list));
        // list must remain unchanged
        assertEquals(Arrays.asList(3, 2, 1), list);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNextPermutationNullList() {
        MathUtilities.nextPermutation(null);
    }

    @Test
    public void testNextPermutationWithRepeatedElements() {
        // The algorithm works with duplicates – the sequence length stays the same
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 1, 2));
        List<List<Integer>> permutations = new ArrayList<>();
        permutations.add(new ArrayList<>(list));
        while (MathUtilities.nextPermutation(list)) {
            permutations.add(new ArrayList<>(list));
        }

        // Expected distinct permutations (lexicographic order)
        List<List<Integer>> expected = Arrays.asList(
                Arrays.asList(1, 1, 2),
                Arrays.asList(1, 2, 1),
                Arrays.asList(2, 1, 1)
        );
        assertEquals(expected, permutations);
    }
}
