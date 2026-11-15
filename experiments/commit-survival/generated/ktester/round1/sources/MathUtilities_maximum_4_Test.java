package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Unit tests for {@link MathUtilities#maximum(double...)}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>normal scenarios – positive, negative, duplicate, extreme values, and NaN handling;</li>
 *   <li>edge cases – single element, zero/negative‑zero, infinity;</li>
 *   <li>exception scenarios – empty array and {@code null} argument.</li>
 * </ul>
 *
 * <p>No external dependencies are required for the method under test, therefore Mockito is only
 * used to satisfy the class‑level {@code @ExtendWith(MockitoExtension.class)} annotation.</p>
 */
@ExtendWith(MockitoExtension.class)
class MathUtilities_maximum_4_Test {

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialise
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    /* ----------------------------------------------------------------------
     *  Existing simple tests – kept for readability and documentation.
     * ---------------------------------------------------------------------- */

    @Test
    void maximum_WithPositiveValues_ReturnsLargest() {
        double[] values = {1.2, 3.4, 2.5, 7.8, 5.0};
        double result = MathUtilities.maximum(values);
        assertEquals(7.8, result, 0.000001);
    }

    @Test
    void maximum_WithNegativeAndPositiveValues_ReturnsLargest() {
        double[] values = {-10.0, -3.5, 0.0, 2.2, -1.1};
        double result = MathUtilities.maximum(values);
        assertEquals(2.2, result, 0.000001);
    }

    @Test
    void maximum_WithAllNegativeValues_ReturnsLeastNegative() {
        double[] values = {-8.0, -15.5, -3.3, -20.0};
        double result = MathUtilities.maximum(values);
        assertEquals(-3.3, result, 0.000001);
    }

    @Test
    void maximum_WithSingleElement_ReturnsThatElement() {
        double[] values = {42.0};
        double result = MathUtilities.maximum(values);
        assertEquals(42.0, result, 0.000001);
    }

    @Test
    void maximum_WithInfinityValues_ReturnsInfinityWhenPresent() {
        double[] values = {1.0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 100.0};
        double result = MathUtilities.maximum(values);
        assertTrue(Double.isInfinite(result) && result > 0);
    }

    /* ----------------------------------------------------------------------
     *  Parameterised tests for the "normal cases" group taken from the JSON spec.
     * ---------------------------------------------------------------------- */

    /**
     * Provides a stream of arguments for {@link #maximum_NormalCases(double[], Double)}.
     *
     * <p>Each {@code Arguments} instance contains:
     * <ul>
     *   <li>a {@code double[]} input array,</li>
     *   <li>the expected result (as {@link Double}; {@code null} is used to indicate an expected NaN).</li>
     * </ul>
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> provideMaximumNormalCases() {
        return Stream.of(
                // Array with positive, negative and duplicate values; expects the largest value 2.5.
                arguments(new double[]{1.0, 2.5, -3.0, 2.5}, 2.5),

                // Array containing negative infinity and negative numbers; the maximum should be -0.0.
                arguments(new double[]{Double.NEGATIVE_INFINITY, -100.0, -0.0}, -0.0),

                // Array with values near Double.MAX_VALUE; expects Double.MAX_VALUE as the maximum.
                arguments(new double[]{Double.MAX_VALUE, 1.0e308, 1.0e307}, Double.MAX_VALUE),

                // First element is NaN; Math.max propagates NaN, so the result should be NaN.
                arguments(new double[]{Double.NaN, 5.0, 10.0}, null),

                // NaN appears after the first element; Math.max propagates NaN, so the result should be NaN.
                arguments(new double[]{5.0, Double.NaN, 10.0}, null)
        );
    }

    @ParameterizedTest(name = "[{index}] maximum({0}) => {1}")
    @MethodSource("provideMaximumNormalCases")
    void maximum_NormalCases(double[] values, Double expected) {
        double result = MathUtilities.maximum(values);

        if (expected == null) { // expected NaN
            assertTrue(Double.isNaN(result), "Result should be NaN");
        } else if (Double.doubleToLongBits(expected) == Double.doubleToLongBits(-0.0)) {
            // Preserve sign of zero – use bit‑wise comparison.
            assertEquals(Double.doubleToLongBits(-0.0), Double.doubleToLongBits(result),
                    "Result should be -0.0");
        } else {
            assertEquals(expected, result, 0.000001,
                    () -> "Expected maximum to be " + expected + " but was " + result);
        }
    }

    /* ----------------------------------------------------------------------
     *  Exception handling tests – based on the "exception cases" group.
     * ---------------------------------------------------------------------- */

    @Test
    void maximum_WithEmptyArray_ThrowsArrayIndexOutOfBoundsException() {
        double[] values = {};
        assertThrows(ArrayIndexOutOfBoundsException.class,
                () -> MathUtilities.maximum(values),
                "Calling maximum on an empty array should throw ArrayIndexOutOfBoundsException");
    }

    @Test
    void maximum_WithNullArray_ThrowsNullPointerException() {
        double[] values = null;
        assertThrows(NullPointerException.class,
                () -> MathUtilities.maximum(values),
                "Calling maximum with a null var‑args array should throw NullPointerException");
    }
}