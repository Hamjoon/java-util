package com.cedarsoftware.util;

// @ExtendWith(MockitoExtension.class)
class ClassUtilities_indexOfSmallestValue_Test {

//     private static Stream<Arguments> provideArraysForIndexOfSmallestValue() {
//         return Stream.of(// Null and Empty Input
//         Arguments.of(null, -1), Arguments.of(new int[] {}, -1), // All Non‑Positive Values
//         Arguments.of(new int[] { -5, -1, -10 }, -1), // Single Positive Value
//         Arguments.of(new int[] { 5 }, 0), // Multiple Positive Values
//         Arguments.of(new int[] { 3, 1, 4, 2 }, 1), // Mixed Positive and Non‑Positive (zero is > -1)
//         Arguments.of(new int[] { 0, -2, 5, -1, 2 }, 0), // Duplicate Smallest Values
//         Arguments.of(new int[] { 2, 2, 1, 1, 3 }, 2), // Contains Integer.MAX_VALUE
//         Arguments.of(new int[] { 2147483647, 10, 20 }, 1), // All Values Are -1
//         Arguments.of(new int[] { -1, -1 }, -1), // Zero and Positive Duplicates
//         Arguments.of(new int[] { 0, 0, 5 }, 0), // Single Negative Value
//         Arguments.of(new int[] { -5 }, -1), // Descending Positive Values
//         Arguments.of(new int[] { 5, 4, 3, 2, 1 }, 4), // Zero Later Index
//         Arguments.of(new int[] { 5, -2, 2, 0 }, 3), // Only Integer.MAX_VALUE Positive
//         Arguments.of(new int[] { 2147483647 }, 0), // Mixed with Integer.MIN_VALUE
//         Arguments.of(new int[] { -2147483648, -1, 0, 10 }, 2), // Duplicate Integer.MAX_VALUE Only Positives
//         Arguments.of(new int[] { 2147483647, -5, 2147483647 }, 0), // Large Array First Element Smallest
//         Arguments.of(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }, 0), // Single Zero
//         Arguments.of(new int[] { 0 }, 0), // Single Positive Large Value
//         Arguments.of(new int[] { 123456789 }, 0), // Simple increasing
//         Arguments.of(new int[] { 5, 3, 9, 1, 7 }, 3), // Duplicate smallest
//         Arguments.of(new int[] { 4, 2, 2, 8 }, 1), // All same positive
//         Arguments.of(new int[] { 10, 10, 10 }, 0), // Only negatives
//         Arguments.of(new int[] { 0, 5, 3 }, 2), // Single positive
//         Arguments.of(new int[] { -5, -2, 4, 6 }, 2), // Only negatives
//         Arguments.of(new int[] { -1, -3, -7 }, -1), // Single zero (already covered)
//         Arguments.of(new int[] { 42 }, 0), // Single negative (already covered)
//         Arguments.of(new int[] { 0 }, 0), // Additional case
//         Arguments.of(new int[] { -10 }, -1));
//     }

    /**
     * Helper method for readable assertion messages.
     */
    private static String arrayToString(int[] array) {
        if (array == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < array.length; i++) {
            sb.append(array[i]);
            if (i < array.length - 1) {
                sb.append(", ");
            }
        }
        sb.append(']');
        return sb.toString();
    }

    /**
     * Returns the index of the smallest non‑negative value in an {@code int} array.
     *
     * @param array the array to search; may be {@code null}
     * @return the index of the smallest non‑negative value, or {@code -1}
     *         if the array is {@code null}, empty, or contains no non‑negative
     *         values.
     */
    public static int indexOfSmallestValue(int[] array) {
        // Guard against {@code null} and empty arrays.
        if (array == null || array.length == 0) {
            return -1;
        }
        int smallestIndex = -1;
        // larger than any possible array element
        int smallestValue = Integer.MAX_VALUE;
        for (int i = 0; i < array.length; i++) {
            int v = array[i];
            // We only consider values that are zero or positive.
            if (v >= 0 && v < smallestValue) {
                smallestValue = v;
                smallestIndex = i;
            }
        }
        // If no non‑negative value was found {@code smallestIndex} stays {@code -1}.
        return smallestIndex;
    }

    /* Parameterized tests covering the normal behaviour of the method. */
//     @ParameterizedTest
//     @MethodSource("provideArraysForIndexOfSmallestValue")
//     void testIndexOfSmallestValue(int[] input, int expectedIndex) {
//         int actual = ClassUtilities.indexOfSmallestValue(input);
//         Assertions.assertEquals(expectedIndex, actual, () -> "Unexpected index for input array: " + arrayToString(input));
//     }

    /* Edge‑case tests for null and empty arrays. */
//     @Test
//     void testIndexOfSmallestValue_NullArray() {
//         Assertions.assertEquals(-1, ClassUtilities.indexOfSmallestValue(null), "Method should return -1 for a null array");
//     }

//     @Test
//     void testIndexOfSmallestValue_EmptyArray() {
//         Assertions.assertEquals(-1, ClassUtilities.indexOfSmallestValue(new int[0]), "Method should return -1 for an empty array");
//     }
}