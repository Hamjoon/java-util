package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

class MathUtilities_maximum_4_Test {

    /**
     * Supplies all normal (i.e. non‑exception) test data.
     *
     * <p>
     * The data mirrors the exhaustive list from the JSON payload, covering:
     * <ul>
     *   <li>standard mixed values</li>
     *   <li>all‑negative arrays</li>
     *   <li>single‑element arrays</li>
     *   <li>duplicate maximum values (1‑20 repetitions)</li>
     *   <li>edge values: {@code NaN}, {@code Infinity}, sub‑normal numbers, etc.</li>
     * </ul>
     * </p>
     */
    static Stream<Arguments> normalCases() {
        return Stream.of(// Standard cases
        Arguments.of(new double[] { 5.0, 3.2, 9.8, 1.1 }, 9.8, "Standard case with multiple positive numbers"), Arguments.of(new double[] { -2.5, -7.1, -0.3, -4.0 }, -0.3, "All negative numbers; the least negative is the maximum"), Arguments.of(new double[] { 3.14 }, 3.14, "Single‑element array"), Arguments.of(new double[] { 2.0, 2.0, 2.0 }, 2.0, "All elements equal"), Arguments.of(new double[] { 1.0, 2.0, 3.0, 3.0, 2.0 }, 3.0, "Duplicate maximum values"), Arguments.of(new double[] { 1.0, -5.0, 0.0, 4.5, 4.5, 4.5 }, 4.5, "Mixed values with multiple occurrences of the maximum"), Arguments.of(new double[] { 5.0, 2.0, 3.0, 4.0, 6.0, 6.0, 1.0 }, 6.0, "Maximum appears more than once and not at first position"), Arguments.of(new double[] { 5.0, 6.0, 7.0, 8.0, 9.0, 10.0 }, 10.0, "Strictly increasing sequence"), Arguments.of(new double[] { 10.0, 9.0, 8.0, 7.0, 6.0, 5.0 }, 10.0, "Strictly decreasing sequence"), Arguments.of(new double[] { 0.0, -0.0, 0.0 }, 0.0, "Positive zero and negative zero compare equal"), Arguments.of(new double[] { 1.0, 2.0, 3.0, -0.0, -0.0 }, 3.0, "Mix of regular numbers and negative zero"), // Larger arrays – loop exercised many times
        Arguments.of(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0 }, 10.0, "Larger array, increasing values"), // Duplicate‑maximum repetitions (2‑20 times)
        Arguments.of(duplicateMaxArray(10, 2), 10.0, "Maximum appears twice"), Arguments.of(duplicateMaxArray(10, 3), 10.0, "Maximum appears three times"), Arguments.of(duplicateMaxArray(10, 4), 10.0, "Maximum appears four times"), Arguments.of(duplicateMaxArray(10, 5), 10.0, "Maximum appears five times"), Arguments.of(duplicateMaxArray(10, 6), 10.0, "Maximum appears six times"), Arguments.of(duplicateMaxArray(10, 7), 10.0, "Maximum appears seven times"), Arguments.of(duplicateMaxArray(10, 8), 10.0, "Maximum appears eight times"), Arguments.of(duplicateMaxArray(10, 9), 10.0, "Maximum appears nine times"), Arguments.of(duplicateMaxArray(10, 10), 10.0, "Maximum appears ten times"), Arguments.of(duplicateMaxArray(10, 11), 10.0, "Maximum appears eleven times"), Arguments.of(duplicateMaxArray(10, 12), 10.0, "Maximum appears twelve times"), Arguments.of(duplicateMaxArray(10, 13), 10.0, "Maximum appears thirteen times"), Arguments.of(duplicateMaxArray(10, 14), 10.0, "Maximum appears fourteen times"), Arguments.of(duplicateMaxArray(10, 15), 10.0, "Maximum appears fifteen times"), Arguments.of(duplicateMaxArray(10, 16), 10.0, "Maximum appears sixteen times"), Arguments.of(duplicateMaxArray(10, 17), 10.0, "Maximum appears seventeen times"), Arguments.of(duplicateMaxArray(10, 18), 10.0, "Maximum appears eighteen times"), Arguments.of(duplicateMaxArray(10, 19), 10.0, "Maximum appears nineteen times"), Arguments.of(duplicateMaxArray(10, 20), 10.0, "Maximum appears twenty times"), // Zero‑only arrays
        Arguments.of(new double[] { 0.0, 0.0, 0.0, 0.0 }, 0.0, "All zeros"), Arguments.of(new double[] { 0.0, -0.0, -0.0, 0.0 }, 0.0, "Positive and negative zero mix"), // Edge values – NaN handling
        Arguments.of(new double[] { 1.0, Double.NaN, 2.0 }, Double.NaN, "NaN in the middle propagates"), Arguments.of(new double[] { Double.NaN, 1.0, 2.0 }, Double.NaN, "NaN as first element propagates"), Arguments.of(new double[] { Double.NaN }, Double.NaN, "Single NaN element"), // Edge values – Infinity handling
        Arguments.of(new double[] { Double.POSITIVE_INFINITY, 1.0, 2.0 }, Double.POSITIVE_INFINITY, "Positive infinity present"), Arguments.of(new double[] { -Double.MAX_VALUE, -1.0, -Double.NEGATIVE_INFINITY }, -1.0, "Negative infinity with larger finite numbers"), Arguments.of(new double[] { -Double.NEGATIVE_INFINITY, -Double.NEGATIVE_INFINITY }, -Double.NEGATIVE_INFINITY, "All negative infinity"), Arguments.of(new double[] { Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY }, Double.POSITIVE_INFINITY, "Both infinities, positive wins"), // Extreme finite values
        Arguments.of(new double[] { Double.MAX_VALUE, 5e-324 }, Double.MAX_VALUE, "Double.MAX_VALUE vs smallest subnormal"), Arguments.of(new double[] { 5e-324, 0.0 }, 0.0, "Subnormal vs zero, zero is larger"), Arguments.of(new double[] { 5e-324, -0.0 }, 5e-324, "Subnormal vs negative zero"), Arguments.of(new double[] { 2.2250738585072014e-308, 5e-324 }, 2.2250738585072014e-308, "Double.MIN_NORMAL vs subnormal"), Arguments.of(new double[] { 0.0, -0.0, Double.NEGATIVE_INFINITY }, 0.0, "Zero vs negative infinity"), Arguments.of(new double[] { Double.NEGATIVE_INFINITY, -1e+308 }, -1e+308, "Negative infinity vs large negative finite"));
    }

    /**
     * Helper that builds an array containing numbers 1..9 followed by the
     * specified {@code max} repeated {@code count} times.
     *
     * @param max   the maximum value to repeat
     * @param count how many times the maximum should appear
     * @return a new {@code double[]} instance
     */
    private static double[] duplicateMaxArray(double max, int count) {
        double[] base = new double[9 + count];
        // fill with increasing values 1..9
        for (int i = 0; i < 9; i++) {
            base[i] = i + 1;
        }
        // fill the tail with the maximum value
        for (int i = 9; i < base.length; i++) {
            base[i] = max;
        }
        return base;
    }

    /**
     * Supplies the exceptional test data.
     */
    static Stream<Arguments> exceptionCases() {
        return Stream.of(Arguments.of(new double[] {}, ArrayIndexOutOfBoundsException.class, "Empty array should cause ArrayIndexOutOfBoundsException"), Arguments.of(null, NullPointerException.class, "Null argument should cause NullPointerException"));
    }

    @Test
    void maximumMultipleValues() {
        double[] values = { 1.5, 3.2, -7.8, 4.4, 0.0 };
        double result = MathUtilities.maximum(values);
        assertEquals(4.4, result, "Maximum of mixed values should be 4.4");
    }

    @Test
    void maximumSingleValue() {
        double[] values = { 42.0 };
        double result = MathUtilities.maximum(values);
        assertEquals(42.0, result, "Maximum of a single‑element array should be that element");
    }

    @Test
    void maximumAllNegativeValues() {
        double[] values = { -10.5, -3.2, -7.7, -0.1 };
        double result = MathUtilities.maximum(values);
        assertEquals(-0.1, result, "Maximum of all negative numbers should be the least negative");
    }

    @Test
    void maximumWithInfinity() {
        double[] values = { Double.NEGATIVE_INFINITY, 5.0, Double.POSITIVE_INFINITY, -100.0 };
        double result = MathUtilities.maximum(values);
        assertEquals(Double.POSITIVE_INFINITY, result, "Maximum should be POSITIVE_INFINITY when present");
    }

    @Test
    void maximumWithNaN() {
        double[] values = { 1.0, Double.NaN, 2.0 };
        double result = MathUtilities.maximum(values);
        assertTrue(Double.isNaN(result), "If any argument is NaN, Math.max propagates NaN");
    }

    @Test
    void maximumWithExtremeDoubleValues() {
        double[] values = { Double.MAX_VALUE, -Double.MAX_VALUE, Double.MIN_VALUE, -Double.MIN_VALUE };
        double result = MathUtilities.maximum(values);
        assertEquals(Double.MAX_VALUE, result, "Maximum should be Double.MAX_VALUE");
    }

    @Test
    void maximumEmptyArrayThrowsArrayIndexOutOfBoundsException() {
        double[] empty = {};
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> MathUtilities.maximum(empty), "Calling maximum with an empty array should throw ArrayIndexOutOfBoundsException");
    }

    /**
     * Executes a large collection of normal test cases (including the many
     * duplicate‑maximum scenarios from the supplied JSON).  The expected value
     * may be {@code Double.NaN} – in that case we verify the result with
     * {@link Double#isNaN(double)}.
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("normalCases")
    void maximumNormalCases(double[] values, double expected, String description) {
        double result = MathUtilities.maximum(values);
        if (Double.isNaN(expected)) {
            assertTrue(Double.isNaN(result), () -> "Expected NaN for case: " + description);
        } else {
            assertEquals(expected, result, () -> "Incorrect maximum for case: " + description);
        }
    }

    /**
     * Verifies that the method throws the expected exception for invalid inputs.
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("exceptionCases")
    void maximumExceptionCases(double[] values, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> MathUtilities.maximum(values), () -> "Expected " + expectedException.getSimpleName() + " for case: " + description);
    }
}
