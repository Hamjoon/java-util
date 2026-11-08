package com.cedarsoftware.util;

// import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
// import static org.junit.Assert.assertEquals;

// @ExtendWith(MockitoExtension.class)
class ClassUtilities_indexOfSmallestValue_Test {

    /**
     * Simple holder for an input array and the expected result.
     */
    private static final class TestCase {

        final int[] array;

        final int expected;

        TestCase(int[] array, int expected) {
            this.array = array;
            this.expected = expected;
        }
    }

//     private static Stream<Arguments> provideArraysForSmallestValue() {
//         return Stream.of(// single‑element arrays
//         Arguments.of(new int[] { 5 }, 0), Arguments.of(new int[] { 0 }, 0), // multiple elements, distinct smallest
//         Arguments.of(new int[] { 7, 3, 9, 2, 8 }, 3), // smallest value appears more than once – first occurrence should win
//         Arguments.of(new int[] { 10, 10, 10 }, 0), // mixture of positive and negative numbers – negatives are ignored
//         Arguments.of(new int[] { 4, 2, 5, 2, 9 }, 1), // zeros are valid non‑negative values
//         Arguments.of(new int[] { 5, 0, 3, 0, 7 }, 1), // mixed negative/positive – first non‑negative smallest is at index 3
//         Arguments.of(new int[] { -5, 4, -2, 1, 3 }, 3), // all negative numbers – method should return -1
//         Arguments.of(new int[] { -3, -1, -7 }, -1));
//     }

//     private static Stream<Arguments> provideNullOrEmptyArrays() {
//         return Stream.of(Arguments.of(null, -1), Arguments.of(new int[0], -1));
//     }

//     private static Stream<Arguments> provideSingleElementArrays() {
//         return Stream.of(Arguments.of(new int[] { 10 }, 0), Arguments.of(new int[] { 0 }, 0), Arguments.of(new int[] { -5 }, -1));
//     }

//     private static Stream<Arguments> provideMultipleElementArrays() {
//         return Stream.of(Arguments.of(new int[] { 3, 1, 4, 2 }, 1), Arguments.of(new int[] { 0, 5, 2 }, 0), Arguments.of(new int[] { 2, 1, 1, 3 }, 1), Arguments.of(new int[] { 5, 4, 3, 2, 1 }, 4), Arguments.of(new int[] { 2147483647, 100, 50 }, 2));
//     }

//     private static Stream<Arguments> provideAllNegativeArrays() {
//         return Stream.of(Arguments.of(new int[] { -1, -2, -3 }, -1), Arguments.of(new int[] { -10, -5, -1, -100 }, -1));
//     }

//     private static Stream<Arguments> provideMixedNegativePositiveArrays() {
//         return Stream.of(Arguments.of(new int[] { -5, 3, -2, 2 }, 3), Arguments.of(new int[] { -5, 1, -2, 4 }, 1));
//     }

//     private static Stream<Arguments> provideBoundaryValueArrays() {
//         return Stream.of(Arguments.of(new int[] { 0, 2147483647 }, 0), Arguments.of(new int[] { 2147483647 }, 0));
//     }

    /**
     * Builds the complete set of test cases that were previously supplied by the various {@code provide*} methods.
     */
    private static List<TestCase> buildTestCases() {
        List<TestCase> cases = new ArrayList<>();
        // ---- Positive / normal scenarios -------------------------------------------------
        cases.add(new TestCase(new int[] { 5 }, 0));
        cases.add(new TestCase(new int[] { 0 }, 0));
        cases.add(new TestCase(new int[] { 7, 3, 9, 2, 8 }, 3));
        cases.add(new TestCase(new int[] { 10, 10, 10 }, 0));
        cases.add(new TestCase(new int[] { 4, 2, 5, 2, 9 }, 1));
        cases.add(new TestCase(new int[] { 5, 0, 3, 0, 7 }, 1));
        cases.add(new TestCase(new int[] { -5, 4, -2, 1, 3 }, 3));
        cases.add(new TestCase(new int[] { -3, -1, -7 }, -1));
        // ---- Edge‑case tests ------------------------------------------------------------
        cases.add(new TestCase(null, -1));
        cases.add(new TestCase(new int[0], -1));
        cases.add(new TestCase(new int[] { -10, -5, -1, -100 }, -1));
        // ---- Single‑element arrays -------------------------------------------------------
        cases.add(new TestCase(new int[] { 10 }, 0));
        cases.add(new TestCase(new int[] { 0 }, 0));
        cases.add(new TestCase(new int[] { -5 }, -1));
        // ---- Multiple‑element arrays ------------------------------------------------------
        cases.add(new TestCase(new int[] { 3, 1, 4, 2 }, 1));
        cases.add(new TestCase(new int[] { 0, 5, 2 }, 0));
        cases.add(new TestCase(new int[] { 2, 1, 1, 3 }, 1));
        cases.add(new TestCase(new int[] { 5, 4, 3, 2, 1 }, 4));
        cases.add(new TestCase(new int[] { 2147483647, 100, 50 }, 2));
        // ---- All‑negative arrays -----------------------------------------------------------
        cases.add(new TestCase(new int[] { -1, -2, -3 }, -1));
        cases.add(new TestCase(new int[] { -10, -5, -1, -100 }, -1));
        // ---- Mixed negative / positive ---------------------------------------------------
        cases.add(new TestCase(new int[] { -5, 3, -2, 2 }, 3));
        cases.add(new TestCase(new int[] { -5, 1, -2, 4 }, 1));
        // ---- Boundary values -------------------------------------------------------------
        cases.add(new TestCase(new int[] { 0, 2147483647 }, 0));
        cases.add(new TestCase(new int[] { 2147483647 }, 0));
        return cases;
    }

    /* ----------------------------------------------------------------------
     *  Parameterized tests for normal behaviour
     * ---------------------------------------------------------------------- */
//     @ParameterizedTest(name = "[{index}] array = {0} → expected index = {1}")
//     @MethodSource("provideArraysForSmallestValue")
//     void testIndexOfSmallestValue_PositiveScenarios(int[] input, int expectedIndex) {
//         int actual = ClassUtilities.indexOfSmallestValue(input);
//         assertEquals(expectedIndex, actual, () -> "Unexpected index for array " + java.util.Arrays.toString(input));
//     }

    /* ----------------------------------------------------------------------
     *  Edge‑case tests
     * ---------------------------------------------------------------------- */
//     @Test
//     @DisplayName("null array returns -1")
//     void testIndexOfSmallestValue_NullArray() {
//         assertEquals(-1, ClassUtilities.indexOfSmallestValue(null), "Method should return -1 for a null array");
//     }

//     @Test
//     @DisplayName("empty array returns -1")
//     void testIndexOfSmallestValue_EmptyArray() {
//         assertEquals(-1, ClassUtilities.indexOfSmallestValue(new int[0]), "Method should return -1 for an empty array");
//     }

//     @Test
//     @DisplayName("array with only negative values returns -1")
//     void testIndexOfSmallestValue_AllNegatives() {
//         int[] negatives = { -10, -5, -1, -100 };
//         assertEquals(-1, ClassUtilities.indexOfSmallestValue(negatives), "Method should ignore negative numbers and return -1 when none are ≥ 0");
//     }

    /* ----------------------------------------------------------------------
     *  Group: NullOrEmptyArray
     * ---------------------------------------------------------------------- */
//     @ParameterizedTest(name = "[{index}] array = {0} → expected = {1}")
//     @MethodSource("provideNullOrEmptyArrays")
//     void testIndexOfSmallestValue_NullOrEmptyArray(int[] input, int expected) {
//         int actual = ClassUtilities.indexOfSmallestValue(input);
//         assertEquals(expected, actual, () -> "Expected " + expected + " for array " + java.util.Arrays.toString(input));
//     }

    /* ----------------------------------------------------------------------
     *  Group: SingleElementArray
     * ---------------------------------------------------------------------- */
//     @ParameterizedTest(name = "[{index}] array = {0} → expected = {1}")
//     @MethodSource("provideSingleElementArrays")
//     void testIndexOfSmallestValue_SingleElementArray(int[] input, int expected) {
//         int actual = ClassUtilities.indexOfSmallestValue(input);
//         assertEquals(expected, actual, () -> "Expected " + expected + " for single‑element array " + java.util.Arrays.toString(input));
//     }

    /* ----------------------------------------------------------------------
     *  Group: MultipleElements
     * ---------------------------------------------------------------------- */
//     @ParameterizedTest(name = "[{index}] array = {0} → expected = {1}")
//     @MethodSource("provideMultipleElementArrays")
//     void testIndexOfSmallestValue_MultipleElements(int[] input, int expected) {
//         int actual = ClassUtilities.indexOfSmallestValue(input);
//         assertEquals(expected, actual, () -> "Expected " + expected + " for array " + java.util.Arrays.toString(input));
//     }

    /* ----------------------------------------------------------------------
     *  Group: AllNegativeValues
     * ---------------------------------------------------------------------- */
//     @ParameterizedTest(name = "[{index}] array = {0} → expected = {1}")
//     @MethodSource("provideAllNegativeArrays")
//     void testIndexOfSmallestValue_AllNegativeValues(int[] input, int expected) {
//         int actual = ClassUtilities.indexOfSmallestValue(input);
//         assertEquals(expected, actual, () -> "Expected " + expected + " for all‑negative array " + java.util.Arrays.toString(input));
//     }

    /* ----------------------------------------------------------------------
     *  Group: MixedNegativeAndPositive
     * ---------------------------------------------------------------------- */
//     @ParameterizedTest(name = "[{index}] array = {0} → expected = {1}")
//     @MethodSource("provideMixedNegativePositiveArrays")
//     void testIndexOfSmallestValue_MixedNegativeAndPositive(int[] input, int expected) {
//         int actual = ClassUtilities.indexOfSmallestValue(input);
//         assertEquals(expected, actual, () -> "Expected " + expected + " for mixed array " + java.util.Arrays.toString(input));
//     }

    /* ----------------------------------------------------------------------
     *  Group: BoundaryValues
     * ---------------------------------------------------------------------- */
//     @ParameterizedTest(name = "[{index}] array = {0} → expected = {1}")
//     @MethodSource("provideBoundaryValueArrays")
//     void testIndexOfSmallestValue_BoundaryValues(int[] input, int expected) {
//         int actual = ClassUtilities.indexOfSmallestValue(input);
//         assertEquals(expected, actual, () -> "Expected " + expected + " for boundary‑value array " + java.util.Arrays.toString(input));
//     }

    /**
     * Executes all test cases and verifies the result of {@link ClassUtilities#indexOfSmallestValue(int[])}.
     */
//     @Test
//     public void testIndexOfSmallestValue() {
//         for (TestCase tc : buildTestCases()) {
//             int actual = ClassUtilities.indexOfSmallestValue(tc.array);
//             assertEquals("Unexpected index for array " + Arrays.toString(tc.array), tc.expected, actual);
//         }
//     }
}