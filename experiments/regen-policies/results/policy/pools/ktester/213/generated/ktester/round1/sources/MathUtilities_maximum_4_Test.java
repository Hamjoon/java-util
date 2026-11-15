package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.cedarsoftware.util.MathUtilities.maximum;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MathUtilities_maximum_4_Test {

    private static Stream<Arguments> typicalValuesProvider() {
        return Stream.of(// Single element
        Arguments.of(new double[] { 5.0 }, 5.0), // Multiple positive numbers
        Arguments.of(new double[] { 1.2, 3.4, 2.2, 0.0, -1.5 }, 3.4), // All negative numbers – the least negative is the maximum
        Arguments.of(new double[] { -10.0, -3.5, -7.2, -0.1 }, -0.1), // Array containing both +0.0 and -0.0 – +0.0 is considered larger
        Arguments.of(new double[] { 0.0, -0.0, 0.0, -0.0 }, 0.0), // Duplicate maximum values
        Arguments.of(new double[] { 3.5, 2.0, 3.5, 1.0 }, 3.5));
    }

    private static Stream<Arguments> nanValuesProvider() {
        return Stream.of(Arguments.of(new double[] { Double.NaN, 1.0, 2.0 }), Arguments.of(new double[] { 3.5, Double.NaN, 4.2 }), Arguments.of(new double[] { Double.NaN }));
    }

    private static Stream<Arguments> infinityValuesProvider() {
        return Stream.of(// Positive infinity dominates any finite value
        Arguments.of(new double[] { Double.POSITIVE_INFINITY, 1.0, 1000.0 }, Double.POSITIVE_INFINITY), // Negative infinity is smaller than any finite number; the largest finite wins
        Arguments.of(new double[] { Double.NEGATIVE_INFINITY, -5.0, -10.0 }, -5.0), // All elements are negative infinity
        Arguments.of(new double[] { Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY }, Double.NEGATIVE_INFINITY));
    }

    private static Stream<Arguments> extremeValuesProvider() {
        return Stream.of(// Double.MAX_VALUE with a very small positive and zero
        Arguments.of(new double[] { Double.MAX_VALUE, Double.MIN_NORMAL, 0.0 }, Double.MAX_VALUE), // Small positive (sub‑normal) greater than any negative number
        Arguments.of(new double[] { Double.MIN_NORMAL, -1.0 }, Double.MIN_NORMAL), // Smallest positive sub‑normal (≈4.9E-324) is less than zero; zero should win
        // 5e-324 approximated
        Arguments.of(new double[] { Double.MIN_VALUE * Double.MIN_VALUE, 0.0 }, 0.0));
    }

    @Test
    void maximumSingleElement() {
        assertEquals(5.0, maximum(5.0));
        assertEquals(-3.14, maximum(-3.14));
    }

    @Test
    void maximumAllPositive() {
        assertEquals(9.9, maximum(1.2, 3.4, 5.6, 9.9, 2.2));
        assertEquals(Double.MAX_VALUE, maximum(1.0, Double.MAX_VALUE, 100.0));
    }

    @Test
    void maximumMixedSign() {
        assertEquals(7.5, maximum(-2.5, 0.0, 7.5, -10.0));
        assertEquals(0.0, maximum(-0.1, -0.2, -0.3, 0.0));
    }

    @Test
    void maximumAllNegative() {
        assertEquals(-1.0, maximum(-5.0, -3.2, -1.0, -9.9));
        assertEquals(-Double.MIN_VALUE, maximum(-Double.MIN_VALUE, -1e-10));
    }

    @Test
    void maximumVeryLargeAndVerySmallValues() {
        // Double.MAX_VALUE
        double large = 1.7976931348623157e308;
        // Double.MIN_NORMAL
        double small = 2.2250738585072014e-308;
        assertEquals(large, maximum(large, small, 0.0));
        assertEquals(small, maximum(small, -large, -1.0));
    }

    @Test
    void maximumWithNaN() {
        double nan = Double.NaN;
        // According to Math.max, if either argument is NaN, the result is NaN.
        // The implementation iterates using Math.max, so any NaN in the array
        // should cause the final result to be NaN.
        assertTrue(Double.isNaN(maximum(nan, 1.0, 2.0)));
        assertTrue(Double.isNaN(maximum(1.0, nan, 2.0)));
        assertTrue(Double.isNaN(maximum(1.0, 2.0, nan)));
    }

    @Test
    void maximumUsingArrayArgument() {
        double[] values = { 45.1, -13.2, 12345.6, 0.0, 99.9 };
        assertEquals(12345.6, maximum(values));
    }

    @Test
    void maximumWithInfinity() {
        assertEquals(Double.POSITIVE_INFINITY, maximum(1.0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
        assertEquals(Double.NEGATIVE_INFINITY, maximum(Double.NEGATIVE_INFINITY, -1.0, -2.0));
    }

    /* ------------------------------------------------------------
     *  Group: Maximum with typical values
     * ------------------------------------------------------------ */
    @ParameterizedTest(name = "[{index}] maximum({0}) = {1}")
    @MethodSource("typicalValuesProvider")
    void maximumTypicalValues(double[] values, double expected) {
        assertEquals(expected, maximum(values));
    }

    /* ------------------------------------------------------------
     *  Group: Maximum with infinities
     * ------------------------------------------------------------ */
    @ParameterizedTest(name = "[{index}] maximum({0}) = {1}")
    @MethodSource("infinityValuesProvider")
    void maximumWithInfinities(double[] values, double expected) {
        assertEquals(expected, maximum(values));
    }

    /* ------------------------------------------------------------
     *  Group: Maximum with empty array throws exception
     * ------------------------------------------------------------ */
    @Test
    void maximumEmptyArrayThrowsException() {
        // Calling with no arguments triggers an ArrayIndexOutOfBoundsException
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> maximum());
    }

    /* ------------------------------------------------------------
     *  Group: Maximum with null array throws exception
     * ------------------------------------------------------------ */
    @Test
    void maximumNullArrayThrowsException() {
        // Passing an explicit null should result in a NullPointerException
        assertThrows(NullPointerException.class, () -> maximum((double[]) null));
    }

    /* ------------------------------------------------------------
     *  Group: Maximum with extreme double values
     * ------------------------------------------------------------ */
    @ParameterizedTest(name = "[{index}] maximum({0}) = {1}")
    @MethodSource("extremeValuesProvider")
    void maximumExtremeValues(double[] values, double expected) {
        assertEquals(expected, maximum(values));
    }

    /* ------------------------------------------------------------
     *  Group: Maximum with mixed zeros and positive numbers
     * ------------------------------------------------------------ */
    @Test
    void maximumMixedZerosAndPositive() {
        double[] values = { -0.0, 0.0, -0.0, 5.0 };
        assertEquals(5.0, maximum(values));
    }
}
