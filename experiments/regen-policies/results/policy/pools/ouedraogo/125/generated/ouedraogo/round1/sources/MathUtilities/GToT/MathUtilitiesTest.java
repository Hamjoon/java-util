package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Comprehensive JUnit‑4 test suite for {@link MathUtilities}.
 *
 * The test suite is organised as follows:
 *
 * <ul>
 *   <li>Basic functional tests for each overload of {@code minimum} / {@code maximum}</li>
 *   <li>Edge‑case tests (single element arrays, empty arrays, null entries)</li>
 *   <li>Tests for {@code parseToMinimalNumericType} covering type selection,
 *       sign handling, leading‑zero stripping and exception scenarios</li>
 *   <li>Tests for {@code nextPermutation} covering normal permutation generation,
 *       termination condition, null handling and duplicate elements</li>
 * </ul>
 *
 * All tests use only the public API of {@code MathUtilities} and the Java standard
 * library classes that appear in its source.
 */
public class MathUtilitiesTest {

    /* -------------------------------------------------
     *  minimum / maximum for primitive long values
     * ------------------------------------------------- */

    @Test
    public void testMinimumLongBasic() {
        long result = MathUtilities.minimum(5L, -3L, 9L, 0L);
        assertEquals(-3L, result);
    }

    @Test
    public void testMaximumLongBasic() {
        long result = MathUtilities.maximum(5L, -3L, 9L, 0L);
        assertEquals(9L, result);
    }

    @Test
    public void testMinimumLongSingleElement() {
        assertEquals(42L, MathUtilities.minimum(42L));
    }

    @Test
    public void testMaximumLongSingleElement() {
        assertEquals(42L, MathUtilities.maximum(42L));
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testMinimumLongEmptyArray() {
        MathUtilities.minimum(new long[0]);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testMaximumLongEmptyArray() {
        MathUtilities.maximum(new long[0]);
    }

    /* -------------------------------------------------
     *  minimum / maximum for primitive double values
     * ------------------------------------------------- */

    @Test
    public void testMinimumDoubleBasic() {
        double result = MathUtilities.minimum(5.5, -3.2, 9.0, 0.0);
        assertEquals(-3.2, result, 0.0);
    }

    @Test
    public void testMaximumDoubleBasic() {
        double result = MathUtilities.maximum(5.5, -3.2, 9.0, 0.0);
        assertEquals(9.0, result, 0.0);
    }

    @Test
    public void testMinimumDoubleSingleElement() {
        assertEquals(3.14, MathUtilities.minimum(3.14), 0.0);
    }

    @Test
    public void testMaximumDoubleSingleElement() {
        assertEquals(3.14, MathUtilities.maximum(3.14), 0.0);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testMinimumDoubleEmptyArray() {
        MathUtilities.minimum(new double[0]);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testMaximumDoubleEmptyArray() {
        MathUtilities.maximum(new double[0]);
    }

    /* -------------------------------------------------
     *  minimum / maximum for BigInteger values
     * ------------------------------------------------- */

    @Test
    public void testMinimumBigIntegerBasic() {
        BigInteger result = MathUtilities.minimum(
                new BigInteger("12345678901234567890"),
                new BigInteger("-12345678901234567890"),
                new BigInteger("0"));
        assertEquals(new BigInteger("-12345678901234567890"), result);
    }

    @Test
    public void testMaximumBigIntegerBasic() {
        BigInteger result = MathUtilities.maximum(
                new BigInteger("12345678901234567890"),
                new BigInteger("-12345678901234567890"),
                new BigInteger("0"));
        assertEquals(new BigInteger("12345678901234567890"), result);
    }

    @Test
    public void testMinimumBigIntegerSingleElement() {
        BigInteger val = new BigInteger("42");
        assertSame(val, MathUtilities.minimum(val));
    }

    @Test
    public void testMaximumBigIntegerSingleElement() {
        BigInteger val = new BigInteger("42");
        assertSame(val, MathUtilities.maximum(val));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMinimumBigIntegerNullEntry() {
        MathUtilities.minimum(new BigInteger("1"), null, new BigInteger("2"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMaximumBigIntegerNullEntry() {
        MathUtilities.maximum(null);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testMinimumBigIntegerEmptyArray() {
        MathUtilities.minimum(new BigInteger[0]);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testMaximumBigIntegerEmptyArray() {
        MathUtilities.maximum(new BigInteger[0]);
    }

    /* -------------------------------------------------
     *  minimum / maximum for BigDecimal values
     * ------------------------------------------------- */

    @Test
    public void testMinimumBigDecimalBasic() {
        BigDecimal result = MathUtilities.minimum(
                new BigDecimal("12345.6789"),
                new BigDecimal("-12345.6789"),
                new BigDecimal("0"));
        assertEquals(new BigDecimal("-12345.6789"), result);
    }

    @Test
    public void testMaximumBigDecimalBasic() {
        BigDecimal result = MathUtilities.maximum(
                new BigDecimal("12345.6789"),
                new BigDecimal("-12345.6789"),
                new BigDecimal("0"));
        assertEquals(new BigDecimal("12345.6789"), result);
    }

    @Test
    public void testMinimumBigDecimalSingleElement() {
        BigDecimal val = new BigDecimal("3.1415");
        assertSame(val, MathUtilities.minimum(val));
    }

    @Test
    public void testMaximumBigDecimalSingleElement() {
        BigDecimal val = new BigDecimal("3.1415");
        assertSame(val, MathUtilities.maximum(val));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMinimumBigDecimalNullEntry() {
        MathUtilities.minimum(new BigDecimal("1.0"), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMaximumBigDecimalNullEntry() {
        MathUtilities.maximum(null);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testMinimumBigDecimalEmptyArray() {
        MathUtilities.minimum(new BigDecimal[0]);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testMaximumBigDecimalEmptyArray() {
        MathUtilities.maximum(new BigDecimal[0]);
    }

    /* -------------------------------------------------
     *  parseToMinimalNumericType
     * ------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testParseToMinimalNumericType_NullInput() {
        MathUtilities.parseToMinimalNumericType(null);
    }

    @Test(expected = NumberFormatException.class)
    public void testParseToMinimalNumericType_InvalidNumber() {
        MathUtilities.parseToMinimalNumericType("not-a-number");
    }

    @Test
    public void testParseToMinimalNumericType_LongWithinRange() {
        Number n = MathUtilities.parseToMinimalNumericType("123456");
        assertTrue(n instanceof Long);
        assertEquals(123456L, n.longValue());
    }

    @Test
    public void testParseToMinimalNumericType_LongWithLeadingZeros() {
        Number n = MathUtilities.parseToMinimalNumericType("00000123");
        assertTrue(n instanceof Long);
        assertEquals(123L, n.longValue());
    }

    @Test
    public void testParseToMinimalNumericType_NegativeZero() {
        Number n = MathUtilities.parseToMinimalNumericType("-0");
        assertTrue(n instanceof Long);
        assertEquals(0L, n.longValue());
    }

    @Test
    public void testParseToMinimalNumericType_BigIntegerOutsideLong() {
        String big = "99999999999999999999999999999999999999";
        Number n = MathUtilities.parseToMinimalNumericType(big);
        assertTrue(n instanceof BigInteger);
        assertEquals(new BigInteger(big), n);
    }

    @Test
    public void testParseToMinimalNumericType_DoubleWithinPrecision() {
        Number n = MathUtilities.parseToMinimalNumericType("1.2345");
        assertTrue(n instanceof Double);
        assertEquals(1.2345, n.doubleValue(), 0.0);
    }

    @Test
    public void testParseToMinimalNumericType_DoubleWithExponentWithinLimit() {
        Number n = MathUtilities.parseToMinimalNumericType("1e2"); // 100
        assertTrue(n instanceof Double);
        assertEquals(100.0, n.doubleValue(), 0.0);
    }

    @Test
    public void testParseToMinimalNumericType_BigDecimalDueToLargeExponent() {
        Number n = MathUtilities.parseToMinimalNumericType("1e309");
        assertTrue(n instanceof BigDecimal);
        assertEquals(new BigDecimal("1e309"), n);
    }

    @Test
    public void testParseToMinimalNumericType_BigDecimalDueToManyDigits() {
        String manyDigits = "12345678901234567890123456789012345.6789";
        Number n = MathUtilities.parseToMinimalNumericType(manyDigits);
        assertTrue(n instanceof BigDecimal);
        assertEquals(new BigDecimal(manyDigits), n);
    }

    @Test
    public void testParseToMinimalNumericType_PositiveSign() {
        Number n = MathUtilities.parseToMinimalNumericType("+00123");
        assertTrue(n instanceof Long);
        assertEquals(123L, n.longValue());
    }

    /* -------------------------------------------------
     *  nextPermutation
     * ------------------------------------------------- */

    @Test
    public void testNextPermutation_StandardSequence() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3));
        List<List<Integer>> permutations = new ArrayList<>();
        permutations.add(new ArrayList<>(list)); // first permutation

        while (MathUtilities.nextPermutation(list)) {
            permutations.add(new ArrayList<>(list));
        }

        List<List<Integer>> expected = Arrays.asList(
                Arrays.asList(1, 2, 3),
                Arrays.asList(1, 3, 2),
                Arrays.asList(2, 1, 3),
                Arrays.asList(2, 3, 1),
                Arrays.asList(3, 1, 2),
                Arrays.asList(3, 2, 1)
        );

        assertEquals(expected, permutations);
    }

    @Test
    public void testNextPermutation_SingleElement() {
        List<String> list = new ArrayList<>(Collections.singletonList("only"));
        boolean hasNext = MathUtilities.nextPermutation(list);
        assertFalse(hasNext);
        assertEquals(Collections.singletonList("only"), list);
    }

    @Test
    public void testNextPermutation_EmptyList() {
        List<Integer> list = new ArrayList<>();
        boolean hasNext = MathUtilities.nextPermutation(list);
        assertFalse(hasNext);
        assertTrue(list.isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNextPermutation_NullList() {
        MathUtilities.nextPermutation(null);
    }

    @Test
    public void testNextPermutation_WithDuplicates() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 1, 2));
        List<List<Integer>> permutations = new ArrayList<>();
        permutations.add(new ArrayList<>(list));

        while (MathUtilities.nextPermutation(list)) {
            permutations.add(new ArrayList<>(list));
        }

        // With duplicates we expect 3 distinct permutations
        List<List<Integer>> expected = Arrays.asList(
                Arrays.asList(1, 1, 2),
                Arrays.asList(1, 2, 1),
                Arrays.asList(2, 1, 1)
        );

        assertEquals(expected, permutations);
    }

    /* -------------------------------------------------
     *  Helper to assert same instance for single‑element overloads
     * ------------------------------------------------- */

    private static <T> void assertSame(T expected, T actual) {
        assertTrue("Expected same instance", expected == actual);
    }
}
