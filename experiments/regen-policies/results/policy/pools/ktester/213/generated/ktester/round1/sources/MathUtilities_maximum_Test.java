package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MathUtilities_maximum_Test {

    /**
     * Supplies the arguments for {@link #testMaximumLong_NormalCases}.
     */
    private static Stream<Arguments> provideMaximumLongNormalCases() {
        return Stream.of(// Single element array
        Arguments.of(new long[] { 5L }, 5L, "Single element array should return that element."), // Increasing sequence
        Arguments.of(new long[] { 1L, 2L, 3L, 4L }, 4L, "Increasing sequence – the maximum is the last element."), // Decreasing sequence
        Arguments.of(new long[] { 4L, 3L, 2L, 1L }, 4L, "Decreasing sequence – the maximum is the first element."), // Mixed positive and negative numbers
        Arguments.of(new long[] { -10L, 0L, 5L, -3L }, 5L, "Mixed positive and negative numbers – should return the greatest positive value."), // Array contains Long.MAX_VALUE
        Arguments.of(new long[] { 0L, -1L, -100L, 42L, 999_999_999L, Long.MAX_VALUE }, Long.MAX_VALUE, "Array contains Long.MAX_VALUE – it must be identified as the maximum."), // Array contains Long.MIN_VALUE and other negatives
        Arguments.of(new long[] { Long.MIN_VALUE, -100L, -5L, -1L }, -1L, "Array contains Long.MIN_VALUE and other negative numbers – the maximum should be the least negative value."), // All elements equal
        Arguments.of(new long[] { 7L, 7L, 7L, 7L }, 7L, "All elements equal – the method should return that common value."), // Duplicate maximum values
        Arguments.of(new long[] { 5L, 9L, 3L, 9L, 2L }, 9L, "Array contains duplicate maximum values; method should return the maximum value (9) regardless of its position or repetition."), // All non‑positive numbers with zero present
        Arguments.of(new long[] { 0L, -1L, -2L, -3L }, 0L, "All non‑positive numbers with zero present; zero is the greatest and should be returned."), // Single element Long.MIN_VALUE
        Arguments.of(new long[] { Long.MIN_VALUE }, Long.MIN_VALUE, "Single element array containing Long.MIN_VALUE; method must return that value."), // Mix of Long.MIN_VALUE, zero, and Long.MAX_VALUE
        Arguments.of(new long[] { Long.MIN_VALUE, 0L, Long.MAX_VALUE }, Long.MAX_VALUE, "Array mixes Long.MIN_VALUE, zero, and Long.MAX_VALUE; the maximum is Long.MAX_VALUE."), // Large increasing array (100 elements)
        Arguments.of(createIncreasingArray(1, 100), 100L, "Large array (100 elements) with increasing values; verifies method works correctly with many iterations."));
    }

    /**
     * Helper that creates a long[] containing a sequence of increasing numbers.
     *
     * @param start inclusive start value
     * @param count number of elements
     * @return array of size {@code count} where element i = start + i
     */
    private static long[] createIncreasingArray(long start, int count) {
        long[] arr = new long[count];
        for (int i = 0; i < count; i++) {
            arr[i] = start + i;
        }
        return arr;
    }

    @Test
    void testMaximumLongWithMultipleValues() {
        long max = MathUtilities.maximum(0, 1, 2);
        assertEquals(2, max);
        max = MathUtilities.maximum(0, 1);
        assertEquals(1, max);
        max = MathUtilities.maximum(0);
        assertEquals(0, max);
        max = MathUtilities.maximum(-10, -9, -8, -7, -6, -5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertEquals(10, max);
        max = MathUtilities.maximum(10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0, -1, -2, -3, -4, -5, -6, -7, -8, -9, -10);
        assertEquals(10, max);
        max = MathUtilities.maximum(-1, 0, 1);
        assertEquals(1, max);
        max = MathUtilities.maximum(-1, 1);
        assertEquals(1, max);
        max = MathUtilities.maximum(-100_000_000, 0, 100_000_000);
        assertEquals(100_000_000, max);
        max = MathUtilities.maximum(-100_000_000, 100_000_000);
        assertEquals(100_000_000, max);
    }

    @Test
    void testMaximumLongWithArrayArgument() {
        long[] values = { 45, -13, 123_213_123 };
        long max = MathUtilities.maximum(values);
        assertEquals(123_213_123, max);
    }

    @Test
    void testMaximumLongWithSingleNegativeValue() {
        long max = MathUtilities.maximum(-42);
        assertEquals(-42, max);
    }

    @Test
    void testMaximumLongWithAllEqualValues() {
        long max = MathUtilities.maximum(7, 7, 7, 7);
        assertEquals(7, max);
    }

    @Test
    void testMaximumLongWithLongMinAndMaxConstants() {
        long max = MathUtilities.maximum(MathUtilities.BIG_INT_LONG_MIN.longValue(), MathUtilities.BIG_INT_LONG_MAX.longValue(), 0);
        assertEquals(MathUtilities.BIG_INT_LONG_MAX.longValue(), max);
    }

    /**
     * Parameterized test that iterates over a collection of normal/edge case
     * inputs defined in the JSON specification.
     *
     * Each argument consists of:
     *   - a long[] to pass to {@code MathUtilities.maximum}
     *   - the expected maximum value
     *   - a short description (used only for readability)
     */
    @ParameterizedTest(name = "{2}")
    @MethodSource("provideMaximumLongNormalCases")
    void testMaximumLong_NormalCases(long[] values, long expected, String description) {
        // Act
        long actual = MathUtilities.maximum(values);
        // Assert
        assertEquals(expected, actual, description);
    }

    // -------------------------------------------------------------------------
    @Test
    void testMaximumLong_EmptyArray_ThrowsArrayIndexOutOfBoundsException() {
        // An empty var‑args array results in values.length == 0,
        // the implementation accesses values[0] and therefore throws.
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> MathUtilities.maximum(new long[] {}), "Empty array – accessing values[0] should throw ArrayIndexOutOfBoundsException.");
    }

    @Test
    void testMaximumLong_NullArray_ThrowsNullPointerException() {
        // Passing an explicit null var‑args argument leads to a NullPointerException
        // when the method tries to read values.length.
        assertThrows(NullPointerException.class, () -> MathUtilities.maximum((long[]) null), "Passing a null array (explicit null varargs) should result in NullPointerException.");
    }
}
