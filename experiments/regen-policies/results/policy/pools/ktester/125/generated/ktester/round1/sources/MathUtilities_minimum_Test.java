package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.cedarsoftware.util.MathUtilities.minimum;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MathUtilities_minimum_Test {

    static Stream<Arguments> emptyArrayProvider() {
        return Stream.of(Arguments.of(new ArrayIndexOutOfBoundsException(), new long[0]));
    }

    static Stream<Arguments> nullArrayProvider() {
        return Stream.of(Arguments.of(new NullPointerException(), null));
    }

    static Stream<Arguments> singleElementProvider() {
        return Stream.of(Arguments.of(new long[] { 0L }, 0L), Arguments.of(new long[] { 123_456_789L }, 123_456_789L), Arguments.of(new long[] { -987_654_321L }, -987_654_321L));
    }

    static Stream<Arguments> twoElementProvider() {
        return Stream.of(Arguments.of(new long[] { 5L, 10L }, 5L), Arguments.of(new long[] { 20L, -3L }, -3L), Arguments.of(new long[] { 7L, 7L }, 7L));
    }

    static Stream<Arguments> mixedSignsProvider() {
        return Stream.of(Arguments.of(new long[] { 3L, -1L, 4L, -5L, 2L }, -5L), Arguments.of(new long[] { 0L, -10L, 10L, -20L, 20L }, -20L));
    }

    static Stream<Arguments> allEqualProvider() {
        return Stream.of(Arguments.of(new long[] { 42L, 42L, 42L, 42L }, 42L));
    }

    static Stream<Arguments> containsLongMinProvider() {
        return Stream.of(Arguments.of(new long[] { 0L, -1L, -2L, -3L, Long.MIN_VALUE }, Long.MIN_VALUE));
    }

    static Stream<Arguments> containsLongMaxProvider() {
        return Stream.of(Arguments.of(new long[] { Long.MAX_VALUE, 100L, -100L, 0L }, -100L));
    }

    static Stream<Arguments> differentPositionsProvider() {
        return Stream.of(// first
        Arguments.of(new long[] { -10L, 5L, 6L, 7L, 8L }, -10L), // middle
        Arguments.of(new long[] { 5L, 6L, -10L, 7L, 8L }, -10L), // last
        Arguments.of(new long[] { 5L, 6L, 7L, 8L, -10L }, -10L));
    }

    static Stream<Arguments> largeArrayProvider() {
        return Stream.of(Arguments.of(new long[] { 1000L, 999L, 998L, 997L, 996L, 995L, 994L, 993L, 992L, 991L, 990L, 989L, 988L, 987L, 986L, 985L, 984L, 983L, 982L, 981L, 980L, 979L, 978L, 977L, 976L, 975L, 974L, 973L, 972L, 971L, 970L, 969L, 968L, 967L, 966L, 965L, 964L, 963L, 962L, 961L, 960L, 959L, 958L, 957L, 956L, 955L, 954L, 953L, 952L, 951L, 950L, -5000L, 949L, 948L }, -5000L));
    }

    @Test
    void testMinimumLongVarargs() {
        assertEquals(0L, minimum(0, 1, 2));
        assertEquals(0L, minimum(0, 1));
        assertEquals(0L, minimum(0));
        assertEquals(-10L, minimum(-10, -9, -8, -7, -6, -5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        assertEquals(-10L, minimum(10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0, -1, -2, -3, -4, -5, -6, -7, -8, -9, -10));
        assertEquals(-1L, minimum(-1, 0, 1));
        assertEquals(-1L, minimum(-1, 1));
        assertEquals(-100_000_000L, minimum(-100_000_000L, 0, 100_000_000L));
        assertEquals(-100_000_000L, minimum(-100_000_000L, 100_000_000L));
    }

    @Test
    void testMinimumLongArray() {
        long[] values = { 45L, -13L, 123_213_123L };
        assertEquals(-13L, minimum(values));
        long[] single = { Long.MAX_VALUE };
        assertEquals(Long.MAX_VALUE, minimum(single));
        long[] allEqual = { 7L, 7L, 7L, 7L };
        assertEquals(7L, minimum(allEqual));
    }

    @Test
    void testMinimumLongWithLongMinValue() {
        long[] values = { Long.MAX_VALUE, Long.MIN_VALUE, 0L, 123L };
        assertEquals(Long.MIN_VALUE, minimum(values));
    }

    @Test
    void testMinimumLongEmptyArrayThrows() {
        long[] empty = new long[0];
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> minimum(empty));
    }

    @Test
    void testMinimumLongMixedPositiveAndNegative() {
        assertEquals(-999L, minimum(500L, -999L, 0L, 250L, 1000L));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "Empty array should throw {0}")
    @MethodSource("emptyArrayProvider")
    void testEmptyArrayThrows(ArrayIndexOutOfBoundsException ignored, long[] values) {
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> minimum(values));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "Null array should throw {0}")
    @MethodSource("nullArrayProvider")
    void testNullArrayThrows(NullPointerException ignored, long[] values) {
        assertThrows(NullPointerException.class, () -> minimum(values));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "single element [{0}] => {1}")
    @MethodSource("singleElementProvider")
    void testSingleElementArray(long[] values, long expected) {
        assertEquals(expected, minimum(values));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "two elements {0} => {1}")
    @MethodSource("twoElementProvider")
    void testTwoElementArray(long[] values, long expected) {
        assertEquals(expected, minimum(values));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "mixed signs {0} => {1}")
    @MethodSource("mixedSignsProvider")
    void testMultipleElementsMixedSigns(long[] values, long expected) {
        assertEquals(expected, minimum(values));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "all equal {0} => {1}")
    @MethodSource("allEqualProvider")
    void testAllEqualValues(long[] values, long expected) {
        assertEquals(expected, minimum(values));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "contains Long.MIN_VALUE {0} => {1}")
    @MethodSource("containsLongMinProvider")
    void testContainsLongMinValue(long[] values, long expected) {
        assertEquals(expected, minimum(values));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "contains Long.MAX_VALUE {0} => {1}")
    @MethodSource("containsLongMaxProvider")
    void testContainsLongMaxValue(long[] values, long expected) {
        assertEquals(expected, minimum(values));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "minimum position {0} => {1}")
    @MethodSource("differentPositionsProvider")
    void testMinimumAtDifferentPositions(long[] values, long expected) {
        assertEquals(expected, minimum(values));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "large array => {1}")
    @MethodSource("largeArrayProvider")
    void testLargeArrayPerformance(long[] values, long expected) {
        assertEquals(expected, minimum(values));
    }
}
