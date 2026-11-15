package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MathUtilities_maximum_Test {

    /**
     * Provides all normal‑scenario test data extracted from the supplied JSON.
     *
     * Each {@code Arguments} instance contains:
     *   – the input {@code long[]} array,
     *   – the expected maximum value,
     *   – a short description (used only for readability in the test name).
     */
    private static Stream<Arguments> normalAndEdgeProvider() {
        return Stream.of(// Maximum Long Values - Normal Scenarios
        Arguments.of(new long[] { 5L }, 5L, "Single element array – the method should return the only element."), Arguments.of(new long[] { 10L, 2L, 3L }, 10L, "Maximum is the first element; loop should never replace the initial value."), Arguments.of(new long[] { 2L, 15L, 3L }, 15L, "Maximum appears in the middle of the array; the loop must update the current value once."), Arguments.of(new long[] { 2L, 3L, 20L }, 20L, "Maximum is the last element; the loop must update on the final iteration."), Arguments.of(new long[] { 7L, 7L, 7L }, 7L, "All elements are equal; the method should return that common value."), Arguments.of(new long[] { -5L, -2L, -10L }, -2L, "All values are negative; the least negative (largest) should be returned."), Arguments.of(new long[] { -10L, 0L, 5L }, 5L, "Mixed negative and positive numbers; the positive maximum should be returned."), Arguments.of(new long[] { Long.MIN_VALUE, 0L, Long.MAX_VALUE }, Long.MAX_VALUE, "Array contains Long.MIN_VALUE and Long.MAX_VALUE; the method must correctly handle extreme values."), Arguments.of(createAscendingArray(0, 99), 99L, "Large array (100 elements) in ascending order; ensures the loop iterates many times and returns the final element."), // Maximum Long Values - Edge Cases
        Arguments.of(new long[] { Long.MAX_VALUE }, Long.MAX_VALUE, "Single element array containing Long.MAX_VALUE; should return that value."), Arguments.of(new long[] { Long.MIN_VALUE }, Long.MIN_VALUE, "Single element array containing Long.MIN_VALUE; should return that value."), Arguments.of(new long[] { 0L, 0L, 0L, 0L }, 0L, "Array of all zeros; the maximum is zero."), Arguments.of(new long[] { 5L, 9L, 9L, 3L }, 9L, "Maximum value appears more than once; method should still return the correct maximum."), Arguments.of(new long[] { 1L, 2L }, 2L, "Two‑element array where the second element is larger; verifies simple comparison."), Arguments.of(new long[] { -1L, 100L, 50L, 200L, 150L }, 200L, "Unsorted array with a clear maximum in the middle; ensures the loop finds the correct value regardless of order."));
    }

    /**
     * Helper that creates an ascending {@code long[]} from {@code start} (inclusive)
     * to {@code end} (inclusive). Used for the 100‑element test case.
     */
    private static long[] createAscendingArray(int start, int end) {
        long[] arr = new long[end - start + 1];
        for (int i = start; i <= end; i++) {
            arr[i - start] = i;
        }
        return arr;
    }

    @Test
    void maximum_returnsLargestValue_forAllPositiveNumbers() {
        // given
        long[] values = { 1L, 5L, 3L, 9L, 2L };
        // when
        long result = MathUtilities.maximum(values);
        // then
        assertEquals(9L, result, "Maximum of all positive numbers should be the greatest element");
    }

    @Test
    void maximum_returnsLargestValue_forAllNegativeNumbers() {
        // given
        long[] values = { -10L, -3L, -7L, -1L, -20L };
        // when
        long result = MathUtilities.maximum(values);
        // then
        assertEquals(-1L, result, "Maximum of all negative numbers should be the least negative (closest to zero)");
    }

    @Test
    void maximum_returnsLargestValue_forMixedSignNumbers() {
        // given
        long[] values = { -5L, 0L, 12L, -3L, 7L };
        // when
        long result = MathUtilities.maximum(values);
        // then
        assertEquals(12L, result, "Maximum should correctly handle mixed positive, negative and zero values");
    }

    @Test
    void maximum_returnsTheOnlyElement_whenArrayHasSingleItem() {
        // given
        long[] values = { 42L };
        // when
        long result = MathUtilities.maximum(values);
        // then
        assertEquals(42L, result, "Maximum of a single‑element array should be that element");
    }

    @Test
    void maximum_handlesLongMinAndLongMax_correctly() {
        // given
        long[] values = { Long.MIN_VALUE, Long.MAX_VALUE, 0L, -1L, 1L };
        // when
        long result = MathUtilities.maximum(values);
        // then
        assertEquals(Long.MAX_VALUE, result, "Maximum must correctly identify Long.MAX_VALUE as the largest value");
    }

    @Test
    void maximum_throwsArrayIndexOutOfBoundsException_whenArrayIsEmpty() {
        // given
        long[] empty = {};
        // when / then
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> MathUtilities.maximum(empty), "Calling maximum with an empty array should throw ArrayIndexOutOfBoundsException");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("normalAndEdgeProvider")
    void maximum_returnsExpectedValue_forVariousScenarios(long[] values, long expected, String description) {
        // when
        long result = MathUtilities.maximum(values);
        // then
        assertEquals(expected, result, () -> "Failed scenario: " + description);
    }

    @Test
    void maximum_throwsNullPointerException_whenArgumentIsNull() {
        // given
        long[] nullArray = null;
        // when / then
        assertThrows(NullPointerException.class, () -> MathUtilities.maximum(nullArray), "Calling maximum with a null argument should throw NullPointerException");
    }
}
