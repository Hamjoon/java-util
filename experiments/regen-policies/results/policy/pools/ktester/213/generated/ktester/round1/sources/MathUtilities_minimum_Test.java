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
class MathUtilities_minimum_Test {

    /**
     * Provides normal (non‑exception) test data extracted from the JSON spec.
     *
     * Each {@link Arguments} contains:
     *   - the input {@code long[]} array
     *   - the expected minimum value
     */
    private static Stream<Arguments> normalCasesProvider() {
        return Stream.of(// Single Element group
        Arguments.of(new long[] { 42L }, 42L), Arguments.of(new long[] { -17L }, -17L), Arguments.of(new long[] { 0L }, 0L), // Multiple Elements – Min at Start
        Arguments.of(new long[] { -100L, 0L, 50L, 200L }, -100L), // Multiple Elements – Min in Middle
        Arguments.of(new long[] { 10L, -5L, 3L, 8L }, -5L), // Multiple Elements – Min at End
        Arguments.of(new long[] { 100L, 20L, 30L, -999L }, -999L), // All Equal Values
        Arguments.of(new long[] { 7L, 7L, 7L, 7L, 7L }, 7L), // Boundary Values
        Arguments.of(new long[] { Long.MAX_VALUE, Long.MIN_VALUE, 0L, 12345L }, Long.MIN_VALUE), Arguments.of(new long[] { Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE }, Long.MAX_VALUE), Arguments.of(new long[] { Long.MIN_VALUE, Long.MIN_VALUE, -1L }, Long.MIN_VALUE), // Large Random Array
        Arguments.of(new long[] { 34L, -12L, 0L, 999_999_999L, -500L, 123_456_789L, Long.MIN_VALUE, 42L }, Long.MIN_VALUE));
    }

    @Test
    void testMinimumWithSingleElement() {
        assertEquals(5L, MathUtilities.minimum(5L));
    }

    @Test
    void testMinimumWithTwoElements() {
        assertEquals(-10L, MathUtilities.minimum(-10L, 20L));
        assertEquals(-10L, MathUtilities.minimum(20L, -10L));
    }

    @Test
    void testMinimumWithMultiplePositiveAndNegative() {
        long result = MathUtilities.minimum(0L, 1L, 2L, 3L, 4L);
        assertEquals(0L, result);
        result = MathUtilities.minimum(-1L, 0L, 1L);
        assertEquals(-1L, result);
    }

    @Test
    void testMinimumWithDescendingOrder() {
        long result = MathUtilities.minimum(10L, 9L, 8L, 7L, 6L, 5L, 4L, 3L, 2L, 1L, 0L, -1L, -2L, -3L, -4L, -5L);
        assertEquals(-5L, result);
    }

    @Test
    void testMinimumWithLargeRange() {
        long result = MathUtilities.minimum(-100_000_000L, 0L, 100_000_000L);
        assertEquals(-100_000_000L, result);
    }

    @Test
    void testMinimumWithArrayArgument() {
        long[] values = { 45L, -13L, 123_213_123L };
        assertEquals(-13L, MathUtilities.minimum(values));
    }

    @Test
    void testMinimumWithExtremeLongValues() {
        long result = MathUtilities.minimum(Long.MAX_VALUE, Long.MIN_VALUE, 0L);
        assertEquals(Long.MIN_VALUE, result);
    }

    @Test
    void testMinimumAllEqualValues() {
        long result = MathUtilities.minimum(7L, 7L, 7L, 7L);
        assertEquals(7L, result);
    }

    @Test
    void testMinimumWithZeroAndNegatives() {
        long result = MathUtilities.minimum(0L, -1L, -2L, -3L);
        assertEquals(-3L, result);
    }

    @ParameterizedTest(name = "minimum({0}) = {1}")
    @MethodSource("normalCasesProvider")
    void testMinimumNormalCases(long[] values, long expected) {
        // Act
        long actual = MathUtilities.minimum(values);
        // Assert
        assertEquals(expected, actual, () -> "Expected minimum of " + expected + " but got " + actual);
    }

    @Test
    void testMinimumEmptyInputThrowsArrayIndexOutOfBoundsException() {
        // The method accesses values[0] without checking length, so an empty array
        // triggers an ArrayIndexOutOfBoundsException.
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> MathUtilities.minimum(new long[] {}));
    }

    @Test
    void testMinimumNullInputThrowsNullPointerException() {
        // Passing null should result in a NullPointerException because the
        // implementation dereferences the array before a null‑check.
        assertThrows(NullPointerException.class, () -> MathUtilities.minimum((long[]) null));
    }
}
