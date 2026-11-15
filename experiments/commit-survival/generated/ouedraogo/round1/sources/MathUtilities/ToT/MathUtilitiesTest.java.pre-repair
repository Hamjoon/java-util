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
 *   <li>Typical, edge and error cases for the overloads of {@code minimum}/{@code maximum}
 *       for {@code long}, {@code double}, {@code BigInteger} and {@code BigDecimal}.</li>
 *   <li>All branches of {@link MathUtilities#parseToMinimalNumericType(String)} including
 *       sign handling, leading‑zero stripping, exponent handling and size limits.</li>
 *   <li>Correct behaviour of {@link MathUtilities#nextPermutation(List)} for normal,
 *       descending, single‑element and empty lists.</li>
 * </ul>
 */
public class MathUtilitiesTest {

    /* ---------------------------------------------------------------------- */
    /*  minimum / maximum for primitive long                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testLongMinimumTypical() {
        long result = MathUtilities.minimum(5L, -3L, 12L, 0L);
        assertEquals(-3L, result);
    }

    @Test
    public void testLongMaximumTypical() {
        long result = MathUtilities.maximum(5L, -3L, 12L, 0L);
        assertEquals(12L, result);
    }

    @Test
    public void testLongMinimumSingleElement() {
        long result = MathUtilities.minimum(42L);
        assertEquals(42L, result);
    }

    @Test
    public void testLongMaximumSingleElement() {
        long result = MathUtilities.maximum(-7L);
        assertEquals(-7L, result);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testLongMinimumEmptyArray() {
        MathUtilities.minimum(); // should throw because values[0] is accessed
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testLongMaximumEmptyArray() {
        MathUtilities.maximum(); // same reason as above
    }

    /* ---------------------------------------------------------------------- */
    /*  minimum / maximum for primitive double                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDoubleMinimumTypical() {
        double result = MathUtilities.minimum(1.5, -2.3, 0.0, 4.4);
        assertEquals(-2.3, result, 0.0);
    }

    @Test
    public void testDoubleMaximumTypical() {
        double result = MathUtilities.maximum(1.5, -2.3, 0.0, 4.4);
        assertEquals(4.4, result, 0.0);
    }

    @Test
    public void testDoubleMinimumWithNaN() {
        double result = MathUtilities.minimum(Double.NaN, 5.0);
        assertTrue(Double.isNaN(result));
    }

    @Test
    public void testDoubleMaximumWithInfinity() {
        double result = MathUtilities.maximum(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        assertEquals(Double.POSITIVE_INFINITY, result, 0.0);
    }

    @Test
    public void testDoubleMinimumSingleElement() {
        double result = MathUtilities.minimum(-0.0);
        assertEquals(-0.0, result, 0.0);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testDoubleMaximumEmptyArray() {
        MathUtilities.maximum(); // will hit values[0]
    }

    /* ---------------------------------------------------------------------- */
    /*  minimum / maximum for BigInteger                                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testBigIntegerMinimumTypical() {
        BigInteger a = new BigInteger("-12345678901234567890");
        BigInteger b = new BigInteger("0");
        BigInteger c = new BigInteger("12345678901234567890");
        assertEquals(a, MathUtilities.minimum(a, b, c));
    }

    @Test
    public void testBigIntegerMaximumTypical() {
        BigInteger a = new BigInteger("-12345678901234567890");
        BigInteger b = new BigInteger("0");
        BigInteger c = new BigInteger("12345678901234567890");
        assertEquals(c, MathUtilities.maximum(a, b, c));
    }

    @Test
    public void testBigIntegerMinimumSingleElement() {
        BigInteger val = new BigInteger("42");
        assertEquals(val, MathUtilities.minimum(val));
    }

    @Test
    public void testBigIntegerMaximumSingleElement() {
        BigInteger val = new BigInteger("-42");
        assertEquals(val, MathUtilities.maximum(val));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBigIntegerMinimumNullEntry() {
        MathUtilities.minimum(new BigInteger("1"), null, new BigInteger("2"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBigIntegerMaximumNullEntry() {
        MathUtilities.maximum(null);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testBigIntegerMinimumEmptyArray() {
        MathUtilities.minimum(); // same internal logic as primitive overloads
    }

    /* ---------------------------------------------------------------------- */
    /*  minimum / maximum for BigDecimal                                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testBigDecimalMinimumTypical() {
        BigDecimal a = new BigDecimal("-1.0001");
        BigDecimal b = new BigDecimal("0");
        BigDecimal c = new BigDecimal("3.1415");
        assertEquals(a, MathUtilities.minimum(a, b, c));
    }

    @Test
    public void testBigDecimalMaximumTypical() {
        BigDecimal a = new BigDecimal("-1.0001");
        BigDecimal b = new BigDecimal("0");
        BigDecimal c = new BigDecimal("3.1415");
        assertEquals(c, MathUtilities.maximum(a, b, c));
    }

    @Test
    public void testBigDecimalMinimumSingleElement() {
        BigDecimal val = new BigDecimal("123.456");
        assertEquals(val, MathUtilities.minimum(val));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBigDecimalMaximumNullEntry() {
        MathUtilities.maximum(new BigDecimal("1.0"), null);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testBigDecimalMaximumEmptyArray() {
        MathUtilities.maximum(); // will throw when accessing values[0]
    }

    /* ---------------------------------------------------------------------- */
    /*  parseToMinimalNumericType                                               */
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
    public void testParseBigIntegerBeyondLong() {
        String big = "9223372036854775808"; // Long.MAX_VALUE + 1
        Number n = MathUtilities.parseToMinimalNumericType(big);
        assertTrue(n instanceof BigInteger);
        assertEquals(new BigInteger(big), n);
    }

    @Test
    public void testParseDoubleSimple() {
        Number n = MathUtilities.parseToMinimalNumericType("3.14159");
        assertTrue(n instanceof Double);
        assertEquals(3.14159, (Double) n, 0.0);
    }

    @Test
    public void testParseDoubleWithExponentWithinLimit() {
        Number n = MathUtilities.parseToMinimalNumericType("1.23e5");
        assertTrue(n instanceof Double);
        assertEquals(123000.0, (Double) n, 0.0);
    }

    @Test
    public void testParseBigDecimalWhenMantissaTooLarge() {
        // 18 digits in mantissa forces BigDecimal
        String val = "123456789012345678.0";
        Number n = MathUtilities.parseToMinimalNumericType(val);
        assertTrue(n instanceof BigDecimal);
        assertEquals(new BigDecimal(val), n);
    }

    @Test
    public void testParseBigDecimalWhenExponentTooLarge() {
        // Exponent 309 is beyond Double range
        String val = "1e309";
        Number n = MathUtilities.parseToMinimalNumericType(val);
        assertTrue(n instanceof BigDecimal);
        assertEquals(new BigDecimal(val), n);
    }

    @Test
    public void testParseZeroAndNegativeZero() {
        Number posZero = MathUtilities.parseToMinimalNumericType("0");
        Number negZero = MathUtilities.parseToMinimalNumericType("-0");
        assertTrue(posZero instanceof Long);
        assertTrue(negZero instanceof Long);
        assertEquals(0L, posZero);
        assertEquals(0L, negZero);
    }

    @Test
    public void testParseWithExplicitPlusSign() {
        Number n = MathUtilities.parseToMinimalNumericType("+007");
        assertTrue(n instanceof Long);
        assertEquals(7L, n);
    }

    @Test(expected = NumberFormatException.class)
    public void testParseInvalidNumberThrows() {
        MathUtilities.parseToMinimalNumericType("not-a-number");
    }

    /* ---------------------------------------------------------------------- */
    /*  nextPermutation                                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testNextPermutationTypicalSequence() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3));
        List<List<Integer>> expected = Arrays.asList(
                Arrays.asList(1, 3, 2),
                Arrays.asList(2, 1, 3),
                Arrays.asList(2, 3, 1),
                Arrays.asList(3, 1, 2),
                Arrays.asList(3, 2, 1)
        );

        List<List<Integer>> actual = new ArrayList<>();
        while (MathUtilities.nextPermutation(list)) {
            actual.add(new ArrayList<>(list));
        }
        assertEquals(expected, actual);
        // After last permutation the method should return false
        assertFalse(MathUtilities.nextPermutation(list));
    }

    @Test
    public void testNextPermutationDescendingListReturnsFalseImmediately() {
        List<Integer> list = new ArrayList<>(Arrays.asList(3, 2, 1));
        assertFalse(MathUtilities.nextPermutation(list));
        // List must remain unchanged
        assertEquals(Arrays.asList(3, 2, 1), list);
    }

    @Test
    public void testNextPermutationSingleElement() {
        List<String> list = new ArrayList<>(Arrays.asList("only"));
        assertFalse(MathUtilities.nextPermutation(list));
    }

    @Test
    public void testNextPermutationEmptyList() {
        List<Object> list = new ArrayList<>();
        assertFalse(MathUtilities.nextPermutation(list));
    }

    @Test
    public void testNextPermutationWithDuplicates() {
        // Duplicate values are allowed; algorithm will treat them as distinct positions.
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 1, 2));
        List<List<Integer>> expected = Arrays.asList(
                Arrays.asList(1, 2, 1),
                Arrays.asList(2, 1, 1)
        );

        List<List<Integer>> actual = new ArrayList<>();
        while (MathUtilities.nextPermutation(list)) {
            actual.add(new ArrayList<>(list));
        }
        assertEquals(expected, actual);
    }
}
