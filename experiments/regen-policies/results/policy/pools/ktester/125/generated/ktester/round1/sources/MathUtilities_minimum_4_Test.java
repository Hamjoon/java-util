package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class MathUtilities_minimum_4_Test {

    /**
     * Supplies normal‑result test data.
     */
    private static Stream<Arguments> provideValuesAndExpectedResults() {
        return Stream.of(// basic functionality
        arguments(new double[] { 1.0, 2.5, -3.4, 0.0 }, -3.4), arguments(new double[] { 5.5 }, 5.5), arguments(new double[] { -1.0, -1.0, -1.0 }, -1.0), arguments(new double[] { Double.MAX_VALUE, Double.MIN_VALUE, -Double.MAX_VALUE }, -Double.MAX_VALUE), arguments(new double[] { 0.0, -0.0 }, -0.0), // edge cases
        arguments(new double[] { Double.NaN, 1.0, -2.0 }, Double.NaN), arguments(new double[] { 1.0, Double.NaN, -2.0 }, -2.0), arguments(new double[] { Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY }, Double.NEGATIVE_INFINITY), arguments(new double[] { Double.NEGATIVE_INFINITY, -1.0 }, Double.NEGATIVE_INFINITY), arguments(new double[] { Double.MAX_VALUE, Double.MAX_VALUE }, Double.MAX_VALUE), arguments(new double[] { Double.MIN_VALUE, 0.0 }, 0.0), arguments(new double[] { 0.0, -0.0, 0.0 }, -0.0), arguments(new double[] { Double.NaN }, Double.NaN), arguments(new double[] { Double.NEGATIVE_INFINITY }, Double.NEGATIVE_INFINITY));
    }

    /**
     * Supplies exception‑expectation test data.
     */
    private static Stream<Arguments> provideValuesAndExpectedExceptions() {
        return Stream.of(arguments(new double[0], ArrayIndexOutOfBoundsException.class), arguments(null, NullPointerException.class));
    }

    @Test
    void testMinimumDoubleArrayTypicalValues() {
        double min = MathUtilities.minimum(0.1, 1.2, 2.3);
        assertEquals(0.1, min, 0.0);
        min = MathUtilities.minimum(-5.5, -2.2, -9.9, 0.0);
        assertEquals(-9.9, min, 0.0);
    }

    @Test
    void testMinimumDoubleArraySingleElement() {
        double min = MathUtilities.minimum(42.0);
        assertEquals(42.0, min, 0.0);
    }

    @Test
    void testMinimumDoubleArrayAllNegative() {
        double min = MathUtilities.minimum(-1.0, -2.0, -3.0, -4.0);
        assertEquals(-4.0, min, 0.0);
    }

    @Test
    void testMinimumDoubleArrayMixedSigns() {
        double min = MathUtilities.minimum(-10.0, 0.0, 10.0, -5.5, 5.5);
        assertEquals(-10.0, min, 0.0);
    }

    @Test
    void testMinimumDoubleArrayLargeValues() {
        double min = MathUtilities.minimum(1.0e200, 1.0e150, -1.0e180);
        assertEquals(-1.0e180, min, 0.0);
    }

    @Test
    void testMinimumDoubleArrayWithInfinity() {
        double min = MathUtilities.minimum(Double.POSITIVE_INFINITY, 100.0, 200.0);
        assertEquals(100.0, min, 0.0);
        min = MathUtilities.minimum(Double.NEGATIVE_INFINITY, 100.0, 200.0);
        assertEquals(Double.NEGATIVE_INFINITY, min, 0.0);
    }

    @Test
    void testMinimumDoubleArrayWithNaN() {
        // According to Math.min, NaN is treated as the larger value,
        // so the minimum should ignore NaN when another number is present.
        double min = MathUtilities.minimum(Double.NaN, 5.0, 10.0);
        assertEquals(5.0, min, 0.0);
        // When NaN is the only element, it becomes the result.
        min = MathUtilities.minimum(Double.NaN);
        assertTrue(Double.isNaN(min));
    }

    @Test
    void testMinimumDoubleArrayEmptyThrowsException() {
        // The implementation accesses values[0]; an empty array should cause
        // an ArrayIndexOutOfBoundsException.
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> MathUtilities.minimum(new double[0]));
    }

    @Test
    void testMinimumDoubleArrayUsingArrayReference() {
        double[] values = { 45.1, -13.1, 123213123.1 };
        double min = MathUtilities.minimum(values);
        assertEquals(-13.1, min, 0.0);
    }

    /**
     * Parameterized test that verifies the returned minimum for a variety of
     * normal and edge‑case inputs.
     */
    @ParameterizedTest(name = "{index}: minimum({0}) → {1}")
    @MethodSource("provideValuesAndExpectedResults")
    void testMinimumDoubleArrayReturnsExpected(double[] values, double expected) {
        double actual = MathUtilities.minimum(values);
        // NaN handling – use Double.isNaN because NaN != NaN
        if (Double.isNaN(expected)) {
            assertTrue(Double.isNaN(actual), "Expected NaN but got " + actual);
            return;
        }
        // Signed zero handling – compare raw bits to distinguish -0.0 from +0.0
        if (Double.doubleToLongBits(expected) == Double.doubleToLongBits(-0.0d)) {
            assertEquals(Double.doubleToLongBits(-0.0d), Double.doubleToLongBits(actual), "Expected -0.0 but got " + actual);
            return;
        }
        // Regular comparison with zero tolerance
        assertEquals(expected, actual, 0.0, () -> "Minimum of " + java.util.Arrays.toString(values) + " should be " + expected);
    }

    /**
     * Parameterized test that verifies the method throws the expected exception
     * for illegal inputs (empty array or {@code null} reference).
     */
    @ParameterizedTest(name = "{index}: minimum({0}) throws {1}")
    @MethodSource("provideValuesAndExpectedExceptions")
    void testMinimumDoubleArrayThrowsExpectedException(double[] values, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> MathUtilities.minimum(values), () -> "Expected " + expectedException.getSimpleName() + " for input " + java.util.Arrays.toString(values));
    }
}
