package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class MathUtilitiesTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /* ----------------------------------------------------------------------
     *  minimum / maximum for primitive long
     * ---------------------------------------------------------------------- */

    @Test
    public void testMinimumLongBasic() {
        long result = MathUtilities.minimum(5L, -3L, 12L, 0L);
        assertEquals(-3L, result);
    }

    @Test
    public void testMaximumLongBasic() {
        long result = MathUtilities.maximum(5L, -3L, 12L, 0L);
        assertEquals(12L, result);
    }

    @Test
    public void testMinimumLongSingleElement() {
        long result = MathUtilities.minimum(42L);
        assertEquals(42L, result);
    }

    @Test
    public void testMaximumLongSingleElement() {
        long result = MathUtilities.maximum(-7L);
        assertEquals(-7L, result);
    }

    @Test
    public void testMinimumLongEmptyArray() {
        thrown.expect(ArrayIndexOutOfBoundsException.class);
        MathUtilities.minimum(new long[0]);
    }

    @Test
    public void testMaximumLongEmptyArray() {
        thrown.expect(ArrayIndexOutOfBoundsException.class);
        MathUtilities.maximum(new long[0]);
    }

    /* ----------------------------------------------------------------------
     *  minimum / maximum for primitive double
     * ---------------------------------------------------------------------- */

    @Test
    public void testMinimumDoubleBasic() {
        double result = MathUtilities.minimum(5.5, -3.2, 12.0, 0.0);
        assertEquals(-3.2, result, 0.0);
    }

    @Test
    public void testMaximumDoubleBasic() {
        double result = MathUtilities.maximum(5.5, -3.2, 12.0, 0.0);
        assertEquals(12.0, result, 0.0);
    }

    @Test
    public void testMinimumDoubleWithNaN() {
        double result = MathUtilities.minimum(Double.NaN, 1.0);
        // Math.min returns NaN if either argument is NaN
        assertTrue(Double.isNaN(result));
    }

    @Test
    public void testMaximumDoubleWithInfinity() {
        double result = MathUtilities.maximum(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        assertEquals(Double.POSITIVE_INFINITY, result, 0.0);
    }

    @Test
    public void testMinimumDoubleEmptyArray() {
        thrown.expect(ArrayIndexOutOfBoundsException.class);
        MathUtilities.minimum(new double[0]);
    }

    @Test
    public void testMaximumDoubleEmptyArray() {
        thrown.expect(ArrayIndexOutOfBoundsException.class);
        MathUtilities.maximum(new double[0]);
    }

    /* ----------------------------------------------------------------------
     *  minimum / maximum for BigInteger
     * ---------------------------------------------------------------------- */

    @Test
    public void testMinimumBigIntegerBasic() {
        BigInteger a = new BigInteger("-100");
        BigInteger b = new BigInteger("0");
        BigInteger c = new BigInteger("50");
        BigInteger result = MathUtilities.minimum(a, b, c);
        assertEquals(a, result);
    }

    @Test
    public void testMaximumBigIntegerBasic() {
        BigInteger a = new BigInteger("-100");
        BigInteger b = new BigInteger("0");
        BigInteger c = new BigInteger("50");
        BigInteger result = MathUtilities.maximum(a, b, c);
        assertEquals(c, result);
    }

    @Test
    public void testMinimumBigIntegerSingleElement() {
        BigInteger val = new BigInteger("12345678901234567890");
        assertEquals(val, MathUtilities.minimum(val));
    }

    @Test
    public void testMaximumBigIntegerSingleElement() {
        BigInteger val = new BigInteger("-12345678901234567890");
        assertEquals(val, MathUtilities.maximum(val));
    }

    @Test
    public void testMinimumBigIntegerNullElement() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Cannot passed null BigInteger entry to minimum()");
        MathUtilities.minimum(new BigInteger("1"), null, new BigInteger("2"));
    }

    @Test
    public void testMaximumBigIntegerNullElement() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Cannot passed null BigInteger entry to maximum()");
        MathUtilities.maximum(null);
    }

    @Test
    public void testMinimumBigIntegerEmptyArray() {
        thrown.expect(ArrayIndexOutOfBoundsException.class);
        MathUtilities.minimum(new BigInteger[0]);
    }

    @Test
    public void testMaximumBigIntegerEmptyArray() {
        thrown.expect(ArrayIndexOutOfBoundsException.class);
        MathUtilities.maximum(new BigInteger[0]);
    }

    /* ----------------------------------------------------------------------
     *  minimum / maximum for BigDecimal
     * ---------------------------------------------------------------------- */

    @Test
    public void testMinimumBigDecimalBasic() {
        BigDecimal a = new BigDecimal("-1.5");
        BigDecimal b = new BigDecimal("0.0");
        BigDecimal c = new BigDecimal("2.5");
        assertEquals(a, MathUtilities.minimum(a, b, c));
    }

    @Test
    public void testMaximumBigDecimalBasic() {
        BigDecimal a = new BigDecimal("-1.5");
        BigDecimal b = new BigDecimal("0.0");
        BigDecimal c = new BigDecimal("2.5");
        assertEquals(c, MathUtilities.maximum(a, b, c));
    }

    @Test
    public void testMinimumBigDecimalSingleElement() {
        BigDecimal val = new BigDecimal("12345.6789");
        assertEquals(val, MathUtilities.minimum(val));
    }

    @Test
    public void testMaximumBigDecimalSingleElement() {
        BigDecimal val = new BigDecimal("-9876.54321");
        assertEquals(val, MathUtilities.maximum(val));
    }

    @Test
    public void testMinimumBigDecimalNullElement() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Cannot passed null BigDecimal entry to minimum()");
        MathUtilities.minimum(new BigDecimal("1"), null);
    }

    @Test
    public void testMaximumBigDecimalNullElement() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Cannot pass null BigDecimal entry to maximum()");
        MathUtilities.maximum(null);
    }

    @Test
    public void testMinimumBigDecimalEmptyArray() {
        thrown.expect(ArrayIndexOutOfBoundsException.class);
        MathUtilities.minimum(new BigDecimal[0]);
    }

    @Test
    public void testMaximumBigDecimalEmptyArray() {
        thrown.expect(ArrayIndexOutOfBoundsException.class);
        MathUtilities.maximum(new BigDecimal[0]);
    }

    /* ----------------------------------------------------------------------
     *  parseToMinimalNumericType
     * ---------------------------------------------------------------------- */

    @Test
    public void testParseLongWithinRange() {
        Number n = MathUtilities.parseToMinimalNumericType("12345");
        assertTrue(n instanceof Long);
        assertEquals(12345L, n);
    }

    @Test
    public void testParseLongNegativeWithLeadingZeros() {
        Number n = MathUtilities.parseToMinimalNumericType("-0000123");
        assertTrue(n instanceof Long);
        assertEquals(-123L, n);
    }

    @Test
    public void testParseBigIntegerBeyondLong() {
        String big = "9223372036854775808"; // Long.MAX_VALUE + 1
        Number n = MathUtilities.parseToMinimalNumericType(big);
        assertTrue(n instanceof BigInteger);
        assertEquals(new BigInteger(big), n);
    }

    @Test
    public void testParseDoubleWithinPrecision() {
        Number n = MathUtilities.parseToMinimalNumericType("1.2345");
        assertTrue(n instanceof Double);
        assertEquals(1.2345, (Double) n, 0.0);
    }

    @Test
    public void testParseDoubleWithExponentSmall() {
        Number n = MathUtilities.parseToMinimalNumericType("1.23e2");
        assertTrue(n instanceof Double);
        assertEquals(123.0, (Double) n, 0.0);
    }

    @Test
    public void testParseBigDecimalDueToPrecision() {
        // 17+ mantissa digits forces BigDecimal
        String val = "12345678901234567.89";
        Number n = MathUtilities.parseToMinimalNumericType(val);
        assertTrue(n instanceof BigDecimal);
        assertEquals(new BigDecimal(val), n);
    }

    @Test
    public void testParseBigDecimalDueToLargeExponent() {
        String val = "1e309"; // exponent > 308 forces BigDecimal
        Number n = MathUtilities.parseToMinimalNumericType(val);
        assertTrue(n instanceof BigDecimal);
        assertEquals(new BigDecimal(val), n);
    }

    @Test
    public void testParseNullString() {
        thrown.expect(IllegalArgumentException.class);
        MathUtilities.parseToMinimalNumericType(null);
    }

    @Test
    public void testParseInvalidNumber() {
        thrown.expect(NumberFormatException.class);
        MathUtilities.parseToMinimalNumericType("not-a-number");
    }

    /* ----------------------------------------------------------------------
     *  nextPermutation
     * ---------------------------------------------------------------------- */

    @Test
    public void testNextPermutationBasicSequence() {
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
    public void testNextPermutationEmptyList() {
        List<Integer> empty = new ArrayList<>();
        assertFalse(MathUtilities.nextPermutation(empty));
        assertTrue(empty.isEmpty());
    }

    @Test
    public void testNextPermutationSingleElement() {
        List<String> single = new ArrayList<>(Collections.singletonList("only"));
        assertFalse(MathUtilities.nextPermutation(single));
        assertEquals(Collections.singletonList("only"), single);
    }

    @Test
    public void testNextPermutationNullList() {
        thrown.expect(IllegalArgumentException.class);
        MathUtilities.nextPermutation(null);
    }

    @Test
    public void testNextPermutationWithDuplicates() {
        // Duplicates are allowed; algorithm should treat them as distinct positions
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 1, 2));
        List<List<Integer>> permutations = new ArrayList<>();
        permutations.add(new ArrayList<>(list));
        while (MathUtilities.nextPermutation(list)) {
            permutations.add(new ArrayList<>(list));
        }

        // Expected permutations (lexicographic order)
        List<List<Integer>> expected = Arrays.asList(
                Arrays.asList(1, 1, 2),
                Arrays.asList(1, 2, 1),
                Arrays.asList(2, 1, 1)
        );

        assertEquals(expected, permutations);
    }
}
