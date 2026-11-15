package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class MathUtilities_nextPermutation_Test {

    private static Stream<Arguments> provideNormalPermutationCases() {
        return Stream.of(// Empty list – false, unchanged
        arguments("Empty list – no next permutation", Collections.emptyList(), false, Collections.emptyList()), // Single element – false, unchanged
        arguments("Single‑element list", Collections.singletonList(5), false, Collections.singletonList(5)), // Descending list – false, unchanged
        arguments("Descending order list (terminal permutation)", Arrays.asList(3, 2, 1), false, Arrays.asList(3, 2, 1)), // Simple ascending list
        arguments("Ascending list [1,2,3]", Arrays.asList(1, 2, 3), true, Arrays.asList(1, 3, 2)), // Pivot at index 0
        arguments("Pivot at index 0 – [1,3,2]", Arrays.asList(1, 3, 2), true, Arrays.asList(2, 1, 3)), // Duplicates – [1,1,2]
        arguments("List with duplicate values – [1,1,2]", Arrays.asList(1, 1, 2), true, Arrays.asList(1, 2, 1)), // All elements equal – no next permutation
        arguments("All elements equal – [2,2,2]", Arrays.asList(2, 2, 2), false, Arrays.asList(2, 2, 2)), // Duplicates where swap involves rightmost greater duplicate
        arguments("Duplicates – [1,2,2]", Arrays.asList(1, 2, 2), true, Arrays.asList(2, 1, 2)), // Pivot near the beginning – complex suffix reversal
        arguments("Complex pivot – [1,5,4,3,2]", Arrays.asList(1, 5, 4, 3, 2), true, Arrays.asList(2, 1, 3, 4, 5)), // Long descending list – terminal permutation
        arguments("Long descending list", Arrays.asList(9, 8, 7, 6, 5, 4, 3, 2, 1, 0), false, Arrays.asList(9, 8, 7, 6, 5, 4, 3, 2, 1, 0)), // Long ascending list – only last two elements change
        arguments("Long ascending list", Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), true, Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 9, 8)), // Generic type – Strings
        arguments("Generic String list", Arrays.asList("a", "b", "c"), true, Arrays.asList("a", "c", "b")));
    }

    private static Stream<Arguments> provideExceptionCases() {
        return Stream.of(// Null reference for the list itself
        arguments("Null list reference", null), // List containing a null element at the start
        arguments("List with leading null element", Arrays.asList(null, 1)), // List containing a null element in the middle (after pivot)
        arguments("List with null element after pivot", Arrays.asList(1, null, 2)));
    }

    @Test
    void nextPermutation_simpleAscending_returnsTrueAndRearranges() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3));
        boolean result = MathUtilities.nextPermutation(list);
        assertTrue(result, "Expected true for a non‑terminal permutation");
        assertEquals(Arrays.asList(1, 3, 2), list, "Permutation did not match expected order");
    }

    @Test
    void nextPermutation_multipleCalls_progressThroughPermutations() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3));
        // 1st permutation: 1 3 2
        assertTrue(MathUtilities.nextPermutation(list));
        assertEquals(Arrays.asList(1, 3, 2), list);
        // 2nd permutation: 2 1 3
        assertTrue(MathUtilities.nextPermutation(list));
        assertEquals(Arrays.asList(2, 1, 3), list);
        // 3rd permutation: 2 3 1
        assertTrue(MathUtilities.nextPermutation(list));
        assertEquals(Arrays.asList(2, 3, 1), list);
        // 4th permutation: 3 1 2
        assertTrue(MathUtilities.nextPermutation(list));
        assertEquals(Arrays.asList(3, 1, 2), list);
        // 5th permutation: 3 2 1
        assertTrue(MathUtilities.nextPermutation(list));
        assertEquals(Arrays.asList(3, 2, 1), list);
        // No further permutations
        assertFalse(MathUtilities.nextPermutation(list));
        assertEquals(Arrays.asList(3, 2, 1), list);
    }

    @Test
    void nextPermutation_descendingList_returnsFalseAndLeavesUnchanged() {
        List<Integer> list = new ArrayList<>(Arrays.asList(3, 2, 1));
        boolean result = MathUtilities.nextPermutation(list);
        assertFalse(result, "Expected false for the last permutation");
        assertEquals(Arrays.asList(3, 2, 1), list, "List should remain unchanged when no permutation exists");
    }

    @Test
    void nextPermutation_withDuplicates_handlesCorrectly() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 1, 2));
        boolean result = MathUtilities.nextPermutation(list);
        assertTrue(result);
        assertEquals(Arrays.asList(1, 2, 1), list);
    }

    @Test
    void nextPermutation_singleElement_returnsFalse() {
        List<String> list = new ArrayList<>(Collections.singletonList("only"));
        boolean result = MathUtilities.nextPermutation(list);
        assertFalse(result);
        assertEquals(Collections.singletonList("only"), list);
    }

    @Test
    void nextPermutation_emptyList_returnsFalse() {
        List<Integer> list = new ArrayList<>();
        boolean result = MathUtilities.nextPermutation(list);
        assertFalse(result);
        assertTrue(list.isEmpty(), "Empty list should stay empty");
    }

    /**
     * Normal‑flow cases – the method should return {@code true} (or {@code false}
     * for the terminal permutation) and mutate the list to the expected next
     * lexicographic permutation.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideNormalPermutationCases")
    void nextPermutation_normalFlow(String description, List<?> input, boolean expectedResult, List<?> expectedList) {
        // Create a mutable copy of the supplied list (the method mutates it)
        List<Object> mutable = new ArrayList<>(input);
        boolean result = MathUtilities.nextPermutation(mutable);
        assertEquals(expectedResult, result, "Unexpected boolean result");
        assertEquals(expectedList, mutable, "List was not transformed as expected");
    }

    /**
     * Exception‑scenario cases – the method is expected to throw a
     * {@link NullPointerException}.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideExceptionCases")
    void nextPermutation_exceptionScenarios(String description, List<?> input) {
        assertThrows(NullPointerException.class, () -> MathUtilities.nextPermutation(input), "Expected NullPointerException for: " + description);
    }
}
