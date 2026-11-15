package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class MathUtilities_maximum_Test {

    private static Stream<Arguments> normalCases() {
        return Stream.of(// Group: "Maximum Long Array - Normal Cases"
        arguments(new long[] { 5, 3, 9, 2, 7 }, 9L), arguments(new long[] { 42 }, 42L), arguments(new long[] { -10, -20, -5, -30 }, -5L), arguments(new long[] { 0, 0, 0, 0 }, 0L), arguments(new long[] { 1, 2, 3, 4, 5 }, 5L), arguments(new long[] { 5, 4, 3, 2, 1 }, 5L), arguments(new long[] { 123456789012345L, -98765432109876L, 123456789012346L }, 123456789012346L), arguments(new long[] { 9223372036854775807L, -9223372036854775808L, 0L }, 9223372036854775807L), // Additional edge cases from the second group that expect a value
        arguments(new long[] { -9223372036854775808L }, -9223372036854775808L), arguments(new long[] { 9223372036854775807L }, 9223372036854775807L), arguments(new long[] { -9223372036854775808L, -5, -10 }, -5L), arguments(new long[] { 9223372036854775806L, 9223372036854775807L }, 9223372036854775807L), arguments(new long[] { -1, 1 }, 1L), arguments(new long[] { 42, 42, 42 }, 42L), arguments(new long[] { 7, 7, 3, 7, 5 }, 7L), arguments(new long[] { -1000, -2000, -3000, -4000, -5000 }, -1000L));
    }

    private static Stream<Arguments> exceptionCases() {
        return Stream.of(// Group: "Maximum Long Array - Edge Cases and Additional Scenarios"
        arguments(new long[] {}, ArrayIndexOutOfBoundsException.class), arguments(null, NullPointerException.class));
    }

    @Test
    void maximumWithIncreasingSequence() {
        long max = MathUtilities.maximum(0L, 1L, 2L, 3L, 4L);
        assertEquals(4L, max);
    }

    @Test
    void maximumWithDecreasingSequence() {
        long max = MathUtilities.maximum(5L, 4L, 3L, 2L, 1L);
        assertEquals(5L, max);
    }

    @Test
    void maximumWithSingleElement() {
        long max = MathUtilities.maximum(42L);
        assertEquals(42L, max);
    }

    @Test
    void maximumWithMixedPositiveAndNegative() {
        long max = MathUtilities.maximum(-10L, -5L, 0L, 5L, 10L);
        assertEquals(10L, max);
    }

    @Test
    void maximumWithAllNegativeValues() {
        long max = MathUtilities.maximum(-20L, -30L, -5L, -100L);
        assertEquals(-5L, max);
    }

    @Test
    void maximumWithArrayArgument() {
        long[] values = { 45L, -13L, 123_213_123L, Long.MAX_VALUE, Long.MIN_VALUE };
        long max = MathUtilities.maximum(values);
        assertEquals(Long.MAX_VALUE, max);
    }

    @Test
    void maximumWithLongMinAndMax() {
        long max = MathUtilities.maximum(Long.MIN_VALUE, Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, max);
    }

    @Test
    void maximumWithZeroAndNegativeZero() {
        // Java has only one representation of zero for long, but we still verify behavior.
        long max = MathUtilities.maximum(0L, -0L);
        assertEquals(0L, max);
    }

    @Test
    void maximumWithRepeatedValues() {
        long max = MathUtilities.maximum(7L, 7L, 7L, 7L);
        assertEquals(7L, max);
    }

    @Test
    void maximumWithLargePositiveValues() {
        long max = MathUtilities.maximum(9_999_999_999L, 8_888_888_888L, 7_777_777_777L);
        assertEquals(9_999_999_999L, max);
    }

    @Test
    void maximumWithLargeNegativeValues() {
        long max = MathUtilities.maximum(-9_999_999_999L, -8_888_888_888L, -7_777_777_777L);
        assertEquals(-7_777_777_777L, max);
    }

    @Test
    void maximumWithEmptyArrayThrowsArrayIndexOutOfBoundsException() {
        // The implementation accesses values[0] without a guard, so an empty array
        // results in an ArrayIndexOutOfBoundsException.
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> MathUtilities.maximum(new long[] {}));
    }

    /**
     * Normal (non‑exception) scenarios taken from the JSON matrix.
     *
     * Each argument consists of:
     *   1. the input long array,
     *   2. the expected maximum value.
     */
    @ParameterizedTest(name = "[{index}] maximum({0}) = {1}")
    @MethodSource("normalCases")
    void maximumNormalCases(long[] values, long expected) {
        assertEquals(expected, MathUtilities.maximum(values));
    }

    /**
     * Exception scenarios taken from the JSON matrix.
     *
     * Each argument consists of:
     *   1. the input long array (may be {@code null}),
     *   2. the expected exception class.
     */
    @ParameterizedTest(name = "[{index}] maximum({0}) throws {1}")
    @MethodSource("exceptionCases")
    void maximumExceptionCases(long[] values, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> MathUtilities.maximum(values));
    }
}
