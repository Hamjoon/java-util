package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;
import java.util.Arrays;

class ArrayUtilities_getArraySubset_Test {

    static Stream<Arguments> normalSubsetProvider() {
        return Stream.of(Arguments.of("Typical subset extraction: elements from index 1 (inclusive) to 4 (exclusive).", new Integer[] { 1, 2, 3, 4, 5 }, 1, 4, new Integer[] { 2, 3, 4 }), Arguments.of("Full copy of the original array (start = 0, end = length).", new Integer[] { 10, 20, 30 }, 0, 3, new Integer[] { 10, 20, 30 }), Arguments.of("Subset of a String array.", new String[] { "a", "b", "c", "d" }, 1, 3, new String[] { "b", "c" }), Arguments.of("Subset containing a single element.", new Integer[] { 100 }, 0, 1, new Integer[] { 100 }), Arguments.of("Empty subset when start equals end; should return an empty array of the same component type.", new Integer[] { 5, 6, 7, 8 }, 2, 2, new Integer[] {}));
    }

    static Stream<Arguments> boundaryVariationProvider() {
        return Stream.of(Arguments.of("Subset of a Boolean array, checking generic handling of primitive wrapper types.", new Boolean[] { true, false, true }, 0, 2, new Boolean[] { true, false }), Arguments.of("Empty source array with start and end at 0 should return an empty array without error.", new Object[] {}, 0, 0, new Object[] {}), Arguments.of("Array containing null elements; ensure they are preserved in the subset.", new Object[] { null, "x", null }, 1, 3, new Object[] { "x", null }), Arguments.of("Subset that includes only the last element of the array.", new Integer[] { 1, 2, 3, 4, 5 }, 4, 5, new Integer[] { 5 }), Arguments.of("Subset that includes only the first element of the array.", new Integer[] { 1, 2, 3, 4, 5 }, 0, 1, new Integer[] { 1 }), Arguments.of("Subset with Unicode characters to verify proper handling of multi‑byte strings.", new String[] { "α", "β", "γ", "δ" }, 2, 4, new String[] { "γ", "δ" }), Arguments.of("Full copy where start=0 and end=length, confirming that the returned array is a new instance (not the same reference).", new Integer[] { 1, 2, 3 }, 0, 3, new Integer[] { 1, 2, 3 }), Arguments.of("Empty subset with start and end both zero on a non‑empty array; should return an empty array of the same component type.", new Integer[] { 1, 2, 3 }, 0, 0, new Integer[] {}));
    }

    static Stream<Arguments> exceptionProvider() {
        return Stream.of(Arguments.of("Negative start index should trigger ArrayIndexOutOfBoundsException.", new Integer[] { 1, 2, 3 }, -1, 2, ArrayIndexOutOfBoundsException.class), Arguments.of("End index beyond array length should trigger ArrayIndexOutOfBoundsException.", new Integer[] { 1, 2, 3 }, 0, 5, ArrayIndexOutOfBoundsException.class), Arguments.of("Start greater than end should trigger ArrayIndexOutOfBoundsException.", new Integer[] { 1, 2, 3, 4 }, 3, 2, ArrayIndexOutOfBoundsException.class), Arguments.of("Null array argument should trigger NullPointerException.", null, 0, 0, NullPointerException.class), Arguments.of("Start index far negative should still trigger ArrayIndexOutOfBoundsException.", new Integer[] { 1, 2, 3 }, -5, 2, ArrayIndexOutOfBoundsException.class), Arguments.of("Negative end index should trigger ArrayIndexOutOfBoundsException.", new Integer[] { 1, 2, 3 }, 0, -1, ArrayIndexOutOfBoundsException.class), Arguments.of("Both start and end beyond array length should trigger ArrayIndexOutOfBoundsException.", new Integer[] { 1, 2, 3 }, 4, 5, ArrayIndexOutOfBoundsException.class), Arguments.of("Start greater than end (2 > 1) should trigger ArrayIndexOutOfBoundsException.", new Integer[] { 1, 2, 3 }, 2, 1, ArrayIndexOutOfBoundsException.class));
    }

    /* ----------------------------------------------------------------------
     * Test normal behaviour: extracting a middle subset from a non‑empty array.
     * ---------------------------------------------------------------------- */
    @Test
    void getArraySubset_middleSubset_returnsExpectedElements() {
        // given
        String[] source = { "alpha", "beta", "gamma", "delta", "epsilon" };
        int start = 1;
        // exclusive, should return beta, gamma, delta
        int end = 4;
        // when
        String[] result = ArrayUtilities.getArraySubset(source, start, end);
        // then
        String[] expected = { "beta", "gamma", "delta" };
        assertArrayEquals(expected, result, "The subset should contain the elements from index 1 (inclusive) to 4 (exclusive).");
    }

    /* ----------------------------------------------------------------------
     * Test extracting the full array (start = 0, end = length).
     * ---------------------------------------------------------------------- */
    @Test
    void getArraySubset_fullRange_returnsCloneOfOriginal() {
        // given
        Integer[] source = { 1, 2, 3, 4 };
        int start = 0;
        int end = source.length;
        // when
        Integer[] result = ArrayUtilities.getArraySubset(source, start, end);
        // then
        assertArrayEquals(source, result, "Full‑range subset should be equal to the original array.");
        // Ensure a new array instance is returned (not the same reference)
        assertNotSame(source, result, "The returned array should be a new instance, not the original.");
    }

    /* ----------------------------------------------------------------------
     * Test behaviour with an empty source array.
     * ---------------------------------------------------------------------- */
    @Test
    void getArraySubset_emptyArray_returnsEmptyArray() {
        // given
        Object[] source = new Object[0];
        int start = 0;
        int end = 0;
        // when
        Object[] result = ArrayUtilities.getArraySubset(source, start, end);
        // then
        assertArrayEquals(ArrayUtilities.EMPTY_OBJECT_ARRAY, result, "Subset of an empty array should also be an empty array.");
    }

    /* ----------------------------------------------------------------------
     * Test that illegal arguments (negative start, end beyond length, start > end)
     * result in the expected ArrayIndexOutOfBoundsException from Arrays.copyOfRange.
     * ---------------------------------------------------------------------- */
    @Test
    void getArraySubset_invalidIndices_throwsArrayIndexOutOfBoundsException() {
        // given
        String[] source = { "a", "b", "c" };
        // when / then
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> ArrayUtilities.getArraySubset(source, -1, 2), "Negative start index should throw ArrayIndexOutOfBoundsException.");
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> ArrayUtilities.getArraySubset(source, 0, 5), "End index greater than array length should throw ArrayIndexOutOfBoundsException.");
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> ArrayUtilities.getArraySubset(source, 3, 2), "Start index greater than end index should throw ArrayIndexOutOfBoundsException.");
    }

    /* ----------------------------------------------------------------------
     * Parameterized tests for the "Normal Subset Cases" group.
     * ---------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("normalSubsetProvider")
    void getArraySubset_normalCases(String description, Object[] source, int start, int end, Object[] expected) {
        Object[] result = ArrayUtilities.getArraySubset(source, start, end);
        assertArrayEquals(expected, result, description);
    }

    /* ----------------------------------------------------------------------
     * Parameterized tests for the "Boundary and Type Variations" group.
     * ---------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("boundaryVariationProvider")
    void getArraySubset_boundaryVariations(String description, Object[] source, int start, int end, Object[] expected) {
        Object[] result = ArrayUtilities.getArraySubset(source, start, end);
        assertArrayEquals(expected, result, description);
    }

    /* ----------------------------------------------------------------------
     * Parameterized tests for the "Exception Cases" group.
     * ---------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("exceptionProvider")
    void getArraySubset_exceptionCases(String description, Object[] source, int start, int end, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> ArrayUtilities.getArraySubset(source, start, end), description);
    }
}
