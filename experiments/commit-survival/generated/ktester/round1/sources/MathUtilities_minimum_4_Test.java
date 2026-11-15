package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MathUtilities_minimum_4_Test {

    private static Stream<Arguments> provideNormalCases() {
        return Stream.of(// basic functionality
        Arguments.of(new double[] { 1.0, 2.5, -3.4, 0.0 }, -3.4, "Array contains positive, negative and zero values; expect -3.4"), // single element
        Arguments.of(new double[] { 5.0 }, 5.0, "Single‑element array; expect that element"), // all equal
        Arguments.of(new double[] { -1.0, -1.0, -1.0 }, -1.0, "All elements equal; expect the common value"), // extreme double values
        Arguments.of(new double[] { Double.MAX_VALUE, Double.MIN_VALUE, -Double.MAX_VALUE }, -Double.MAX_VALUE, "Array includes extreme double values; expect most negative value"), // zero and negative zero
        Arguments.of(new double[] { 0.0, -0.0 }, -0.0, "Zero and negative zero; expect -0.0 (first minimum encountered)"), // later minimum
        Arguments.of(new double[] { 10.0, 8.5, 3.2, 4.4, 2.1, 9.9 }, 2.1, "Smallest value appears near the end of the array"), // all zeros (positive and negative)
        Arguments.of(new double[] { 0.0, 0.0, -0.0, 0.0 }, 0.0, "All elements are zero; expect 0.0 (first zero)"), // large random array
        Arguments.of(new double[] { 3.14, -2.71, 0.0, 1.618, -1000.0, 500.0, -0.001, 42.0, -999.999, 2.0 }, -1000.0, "Wide‑range array; expect -1000.0"), // subnormal positive values
        Arguments.of(new double[] { Double.MIN_VALUE, Double.MIN_NORMAL, 1.0 }, Double.MIN_VALUE, "Subnormal positive numbers; expect Double.MIN_VALUE (4.9E-324)"), // subnormal negative values
        Arguments.of(new double[] { -Double.MIN_VALUE, -Double.MIN_NORMAL, -1.0 }, -1.0, "Negative subnormals; expect -1.0 as the most negative"));
    }

    private static Stream<Arguments> provideNaNCases() {
        return Stream.of(Arguments.of(new double[] { Double.NaN, 5.0, -3.0 }, "First element NaN; result must be NaN"), Arguments.of(new double[] { 5.0, Double.NaN, -3.0 }, "NaN in subsequent position; result must be NaN"), Arguments.of(new double[] { Double.NaN, Double.NaN }, "All elements NaN; result must be NaN"));
    }

    private static Stream<Arguments> provideInfinityCases() {
        return Stream.of(Arguments.of(new double[] { Double.POSITIVE_INFINITY, 1.0, 2.0 }, 1.0, "Positive infinity larger than any finite number; expect 1.0"), Arguments.of(new double[] { Double.NEGATIVE_INFINITY, 1.0, -5.0 }, Double.NEGATIVE_INFINITY, "Negative infinity smaller than any finite number; expect -Infinity"), Arguments.of(new double[] { Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY }, Double.NEGATIVE_INFINITY, "Mix of both infinities; expect -Infinity"), Arguments.of(new double[] { Double.POSITIVE_INFINITY, Double.NaN }, Double.NaN, "Infinity with NaN; result must be NaN"));
    }

    /**
     * Verify that the method returns the smallest value from a typical array.
     */
    @Test
    void testMinimumMultipleValues() {
        double[] values = { 5.0, 2.3, -1.0, 4.5 };
        double expected = -1.0;
        double actual = MathUtilities.minimum(values);
        assertEquals(expected, actual, 0.0, "minimum should return the smallest element");
    }

    /**
     * Verify that a single‑element array returns that element.
     */
    @Test
    void testMinimumSingleValue() {
        double[] values = { 42.0 };
        double expected = 42.0;
        double actual = MathUtilities.minimum(values);
        assertEquals(expected, actual, 0.0, "minimum of a single‑element array should be that element");
    }

    /**
     * Verify the behaviour when the array contains {@code Double.NaN}.
     * According to {@link Math#min(double, double)}, any comparison with NaN yields NaN,
     * so the result should be NaN.
     */
    @Test
    void testMinimumWithNaN() {
        double[] values = { Double.NaN, 1.0, -2.0 };
        double actual = MathUtilities.minimum(values);
        assertTrue(Double.isNaN(actual), "minimum should be NaN when the first element is NaN");
    }

    /**
     * Verify that calling the method with an empty array throws
     * {@link ArrayIndexOutOfBoundsException} because the implementation
     * accesses {@code values[0]} without a size check.
     */
    @Test
    void testMinimumEmptyArrayThrowsException() {
        double[] empty = {};
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> MathUtilities.minimum(empty), "minimum should throw ArrayIndexOutOfBoundsException for empty array");
    }

    /**
     * Verifies that {@code minimum(double...)} returns the smallest value
     * for a variety of normal input arrays.
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("provideNormalCases")
    void testMinimumNormal(double[] values, double expected, String description) {
        double actual = MathUtilities.minimum(values);
        if (Double.isNaN(expected)) {
            assertTrue(Double.isNaN(actual), description);
        } else if (Double.doubleToLongBits(expected) == Double.doubleToLongBits(-0.0)) {
            // Special check for -0.0 because assertEquals treats -0.0 == 0.0
            assertEquals(Double.doubleToLongBits(-0.0), Double.doubleToLongBits(actual), description);
        } else {
            assertEquals(expected, actual, 0.0, description);
        }
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("provideInfinityCases")
    void testMinimumWithInfinities(double[] values, double expected, String description) {
        double actual = MathUtilities.minimum(values);
        if (Double.isNaN(expected)) {
            assertTrue(Double.isNaN(actual), description);
        } else {
            assertEquals(expected, actual, 0.0, description);
        }
    }

    @Test
    void testMinimumNullArrayThrowsException() {
        double[] nullArray = null;
        assertThrows(NullPointerException.class, () -> MathUtilities.minimum(nullArray), "Null array should trigger NullPointerException");
    }

    /**
     * Verify the behaviour when the array contains {@code Double.NaN}.
     * According to {@link Math#min(double, double)}, any comparison with NaN yields NaN,
     * so the result should be NaN.
     */
    @Test
    void testMinimumWithNaN_Original() {
        double[] values = { Double.NaN, 1.0, -2.0 };
        double actual = MathUtilities.minimum(values);
        assertTrue(Double.isNaN(actual), "minimum should be NaN when the first element is NaN");
    }
}
