package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 * Comprehensive JUnit 4 test suite for {@link MathUtilities}.
 */
public class MathUtilitiesTest {

    /* ----------------------------------------------------------------------
     * Tests for long minimum / maximum
     * ---------------------------------------------------------------------- */

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
    public void testLongMinMaxSingleElement() {
        assertEquals(42L, MathUtilities.minimum(42L));
        assertEquals(42L, MathUtilities.maximum(42L));
    }

    @Test
    public void testLongMinMaxWithExtremeValues() {
        assertEquals(Long.MIN_VALUE, MathUtilities.minimum(Long.MAX_VALUE, Long.MIN_VALUE, 0L));
        assertEquals(Long.MAX_VALUE, MathUtilities.maximum(Long.MAX_VALUE, Long.MIN_VALUE, 0L));
    }

    /* ----------------------------------------------------------------------
     * Tests for double minimum / maximum
     * ---------------------------------------------------------------------- */

    @Test
    public void testDoubleMinimumTypical() {
        double result = MathUtilities.minimum(5.5, -3.2, 12.0, 0.0);
        assertEquals(-3.2, result, 0.0);
    }

    @Test
    public void testDoubleMaximumTypical() {
        double result = MathUtilities.maximum(5.5, -3.2, 12.0, 0.0);
        assertEquals(12.0, result, 0.0);
    }

    @Test
    public void testDoubleMinMaxWithInfinityAndNaN() {
        assertTrue(Double.isNaN(MathUtilities.minimum(Double.NaN, 1.0)));
        assertTrue(Double.isNaN(MathUtilities.maximum(Double.NaN, 1.0)));

        assertEquals(Double.POSITIVE_INFINITY,
                MathUtilities.maximum(Double.POSITIVE_INFINITY, 1.0), 0.0);
        assertEquals(Double.NEGATIVE_INFINITY,
                MathUtilities.minimum(Double.NEGATIVE_INFINITY, 1.0), 0.0);
    }

    @Test
    public void testDoubleMinMaxSingleElement() {
        assertEquals(3.14, MathUtilities.minimum(3.14), 0.0);
        assertEquals(3.14, MathUtilities.maximum(3.14), 0.0);
    }

    /* ----------------------------------------------------------------------
     * Tests for BigInteger minimum / maximum
     * ---------------------------------------------------------------------- */

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

    /* ----------------------------------------------------------------------
     * Tests for BigDecimal minimum / maximum
     * ---------------------------------------------------------------------- */

    @Test
    public void testBigDecimalMinimumTypical() {
        BigDecimal a = new BigDecimal("-1.0001");
        BigDecimal b = new BigDecimal("0.0");
        BigDecimal c = new BigDecimal("1.0001");
        assertEquals(a, MathUtilities.minimum(a, b, c));
    }

    @Test
    public void testBigDecimalMaximumTypical() {
        BigDecimal a = new BigDecimal("-1.0001");
        BigDecimal b = new BigDecimal("0.0");
        BigDecimal c = new BigDecimal("1.0001");
        assertEquals(c, MathUtilities.maximum(a, b, c));
    }

    @Test
    public void testBigDecimalSingleElement() {
        BigDecimal val = new BigDecimal("3.1415");
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

    /* ----------------------------------------------------------------------
     * Tests for parseToMinimalNumericType
     * ---------------------------------------------------------------------- */

    @Test
    public void testParseIntegerWithinLongRange() {
        Number n = MathUtilities.parseToMinimalNumericType("12345");
        assertTrue(n instanceof Long);
        assertEquals(12345L, n.longValue());
    }

    @Test
    public void testParseIntegerWithLeadingZerosAndPlusSign() {
        Number n = MathUtilities.parseToMinimalNumericType("+000123");
        assertTrue(n instanceof Long);
        assertEquals(123L, n.longValue());
    }

    @Test
    public void testParseNegativeIntegerWithLeadingZeros() {
        Number n = MathUtilities.parseToMinimalNumericType("-000987");
        assertTrue(n instanceof Long);
        assertEquals(-987L, n.longValue());
    }

    @Test
    public void testParseIntegerBeyondLongRangeReturnsBigInteger() {
        // 20 digits > Long.MAX_VALUE (19 digits)
        String big = "123456789012345678901";
        Number n = MathUtilities.parseToMinimalNumericType(big);
        assertTrue(n instanceof BigInteger);
        assertEquals(new BigInteger(big), n);
    }

    @Test
    public void testParseDecimalFitsInDouble() {
        Number n = MathUtilities.parseToMinimalNumericType("3.14159265358979");
        assertTrue(n instanceof Double);
        assertEquals(3.14159265358979, n.doubleValue(), 1e-15);
    }

    @Test
    public void testParseDecimalRequiresBigDecimalDueToPrecision() {
        // 18 significant digits -> exceeds Double safe mantissa (<17)
        Number n = MathUtilities.parseToMinimalNumericType("0.123456789012345678");
        assertTrue(n instanceof BigDecimal);
        assertEquals(new BigDecimal("0.123456789012345678"), n);
    }

    @Test
    public void testParseDecimalWithLargeExponentReturnsDouble() {
        Number n = MathUtilities.parseToMinimalNumericType("1.23e5");
        assertTrue(n instanceof Double);
        assertEquals(1.23e5, n.doubleValue(), 0.0);
    }

    @Test
    public void testParseDecimalWithLargeExponentReturnsBigDecimal() {
        // Exponent magnitude >= 308 forces BigDecimal
        Number n = MathUtilities.parseToMinimalNumericType("1.23e308");
        assertTrue(n instanceof BigDecimal);
        assertEquals(new BigDecimal("1.23e308"), n);
    }

    @Test
    public void testParseZeroAndNegativeZero() {
        Number posZero = MathUtilities.parseToMinimalNumericType("0");
        Number negZero = MathUtilities.parseToMinimalNumericType("-0");
        assertTrue(posZero instanceof Long);
        assertEquals(0L, posZero.longValue());
        assertTrue(negZero instanceof Long);
        assertEquals(0L, negZero.longValue()); // -0 parsed as 0 long
    }

    /* ----------------------------------------------------------------------
     * Tests for nextPermutation
     * ---------------------------------------------------------------------- */

    @Test
    public void testNextPermutationTypicalSequence() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3));
        // First call -> 1,3,2
        assertTrue(MathUtilities.nextPermutation(list));
        assertEquals(Arrays.asList(1, 3, 2), list);

        // Second call -> 2,1,3
        assertTrue(MathUtilities.nextPermutation(list));
        assertEquals(Arrays.asList(2, 1, 3), list);

        // Third call -> 2,3,1
        assertTrue(MathUtilities.nextPermutation(list));
        assertEquals(Arrays.asList(2, 3, 1), list);

        // Fourth call -> 3,1,2
        assertTrue(MathUtilities.nextPermutation(list));
        assertEquals(Arrays.asList(3, 1, 2), list);

        // Fifth call -> 3,2,1
        assertTrue(MathUtilities.nextPermutation(list));
        assertEquals(Arrays.asList(3, 2, 1), list);

        // Sixth call -> no more permutations
        assertFalse(MathUtilities.nextPermutation(list));
        // List should remain in the last (descending) order
        assertEquals(Arrays.asList(3, 2, 1), list);
    }

    @Test
    public void testNextPermutationSingleElementReturnsFalse() {
        List<String> list = new ArrayList<>(Arrays.asList("only"));
        assertFalse(MathUtilities.nextPermutation(list));
        assertEquals(Arrays.asList("only"), list);
    }

    @Test
    public void testNextPermutationAlreadyHighestReturnsFalse() {
        List<Integer> list = new ArrayList<>(Arrays.asList(3, 2, 1));
        assertFalse(MathUtilities.nextPermutation(list));
        assertEquals(Arrays.asList(3, 2, 1), list);
    }

    @Test
    public void testNextPermutationWithStrings() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        assertTrue(MathUtilities.nextPermutation(list));
        assertEquals(Arrays.asList("a", "c", "b"), list);
    }

    @Test
    public void testNextPermutationEmptyListReturnsFalse() {
        List<Integer> list = new ArrayList<>();
        assertFalse(MathUtilities.nextPermutation(list));
        assertTrue(list.isEmpty());
    }
}
