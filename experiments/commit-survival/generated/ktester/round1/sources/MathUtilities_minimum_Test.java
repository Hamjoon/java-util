package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MathUtilities_minimum_Test {

    /**
     * Provides normal, boundary, duplicate‑minimum and large‑array cases.
     */
    private static Stream<Arguments> normalAndBoundaryProvider() {
        return Stream.of(// Normal cases
        Arguments.of(new long[] { 5L }, 5L, "Single element array should return that element as the minimum."), Arguments.of(new long[] { 3L, 7L, 2L, 9L }, 2L, "Multiple positive numbers – the smallest (2) should be returned."), Arguments.of(new long[] { 10L, -5L, 3L }, -5L, "Mixed positive and negative values – the negative value should be identified as the minimum."), Arguments.of(new long[] { Long.MAX_VALUE, Long.MIN_VALUE, 0L }, Long.MIN_VALUE, "Array containing Long.MAX_VALUE, Long.MIN_VALUE and zero – Long.MIN_VALUE must be returned."), Arguments.of(new long[] { 4L, 4L, 4L }, 4L, "All elements are equal – the common value should be returned."), // Boundary values
        Arguments.of(new long[] { Long.MAX_VALUE }, Long.MAX_VALUE, "Single element equal to Long.MAX_VALUE should be returned unchanged."), Arguments.of(new long[] { Long.MIN_VALUE }, Long.MIN_VALUE, "Single element equal to Long.MIN_VALUE should be returned unchanged."), Arguments.of(new long[] { Long.MAX_VALUE, Long.MAX_VALUE - 1 }, Long.MAX_VALUE - 1, "Array with Long.MAX_VALUE and the next lower value – the smaller one must be returned."), Arguments.of(new long[] { Long.MIN_VALUE, Long.MIN_VALUE + 1 }, Long.MIN_VALUE, "Array with Long.MIN_VALUE and the next higher value – Long.MIN_VALUE is the minimum."), // Duplicate minimum values
        Arguments.of(new long[] { 5L, -1L, -1L, 3L }, -1L, "Multiple occurrences of the minimum (-1) should still result in -1."), Arguments.of(new long[] { 7L, 7L, 5L, 5L, 9L }, 5L, "Array with two different duplicate minima (5) – the method must return 5."), // Large descending array
        Arguments.of(new long[] { 20L, 19L, 18L, 17L, 16L, 15L, 14L, 13L, 12L, 11L, 10L, 9L, 8L, 7L, 6L, 5L, 4L, 3L, 2L, 1L }, 1L, "A larger array (20 elements) in strictly descending order – the last element (1) is the minimum."));
    }

    /**
     * Provides exceptional cases: empty array and {@code null}.
     */
    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(Arguments.of(new long[] {}, ArrayIndexOutOfBoundsException.class, "Calling minimum with an empty var‑args array should throw ArrayIndexOutOfBoundsException because the method accesses values[0] without a length check."), Arguments.of(null, NullPointerException.class, "Passing a null reference as the var‑args argument results in a NullPointerException when the method tries to read values.length."));
    }

    @Test
    @DisplayName("minimum with a single element returns that element")
    void testMinimumSingleElement() {
        long[] values = { 42L };
        long result = MathUtilities.minimum(values);
        assertEquals(42L, result, "The minimum of a single‑element array should be the element itself");
    }

    @Test
    @DisplayName("minimum with multiple positive numbers")
    void testMinimumMultiplePositive() {
        long[] values = { 10L, 5L, 20L, 7L, 3L };
        long result = MathUtilities.minimum(values);
        assertEquals(3L, result, "The smallest positive value should be returned");
    }

    @Test
    @DisplayName("minimum with mixed positive and negative numbers")
    void testMinimumMixedSign() {
        long[] values = { 15L, -4L, 8L, -20L, 0L };
        long result = MathUtilities.minimum(values);
        assertEquals(-20L, result, "The most negative value should be returned");
    }

    @Test
    @DisplayName("minimum with Long.MAX_VALUE and Long.MIN_VALUE")
    void testMinimumWithLongExtremes() {
        long[] values = { Long.MAX_VALUE, 123L, Long.MIN_VALUE, 0L };
        long result = MathUtilities.minimum(values);
        assertEquals(Long.MIN_VALUE, result, "Long.MIN_VALUE should be identified as the minimum");
    }

    @Test
    @DisplayName("minimum with duplicate values")
    void testMinimumWithDuplicates() {
        long[] values = { 7L, 7L, 7L, 7L };
        long result = MathUtilities.minimum(values);
        assertEquals(7L, result, "When all values are equal, that value should be returned");
    }

    @Test
    @DisplayName("minimum with empty array should throw ArrayIndexOutOfBoundsException")
    void testMinimumEmptyArray() {
        long[] empty = {};
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> MathUtilities.minimum(empty), "Calling minimum with an empty array should throw ArrayIndexOutOfBoundsException");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("normalAndBoundaryProvider")
    @DisplayName("Parameterized tests for normal, boundary and duplicate‑minimum cases")
    void testMinimumParameterized(long[] values, long expected, String description) {
        long result = MathUtilities.minimum(values);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("exceptionProvider")
    @DisplayName("Parameterized tests for exceptional cases")
    void testMinimumExceptions(long[] values, Class<? extends Throwable> exceptionClass, String description) {
        assertThrows(exceptionClass, () -> MathUtilities.minimum(values), description);
    }
}
