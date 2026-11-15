package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MathUtilities_nextPermutation_Test {

    /**
     * Simple wrapper that reverses the natural integer ordering.
     * Larger numeric values are considered "smaller".
     */
    private static final class ReverseInt implements Comparable<ReverseInt> {

        private final int value;

        ReverseInt(int value) {
            this.value = value;
        }

        @Override
        public int compareTo(ReverseInt other) {
            // Reverse order: larger value is "smaller"
            return Integer.compare(other.value, this.value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof ReverseInt))
                return false;
            return value == ((ReverseInt) o).value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public String toString() {
            return "ReverseInt{" + value + '}';
        }
    }

    /**
     * Comparable that deliberately throws a RuntimeException from compareTo.
     */
    private static final class BadComparable implements Comparable<BadComparable> {

        private final int value;

        BadComparable(int value) {
            this.value = value;
        }

        @Override
        public int compareTo(BadComparable o) {
            throw new RuntimeException("Intentional failure in compareTo");
        }

        @Override
        public String toString() {
            return "BadComparable{" + value + '}';
        }
    }

    @Test
    void nextPermutation_simpleIntegerList_returnsTrueAndReorders() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3));
        boolean result = MathUtilities.nextPermutation(list);
        assertTrue(result, "Should have a next permutation");
        assertEquals(Arrays.asList(1, 3, 2), list, "List should be reordered to the next lexicographic permutation");
    }

    @Test
    void nextPermutation_descendingIntegerList_returnsFalseAndLeavesUnchanged() {
        List<Integer> list = new ArrayList<>(Arrays.asList(3, 2, 1));
        List<Integer> original = new ArrayList<>(list);
        boolean result = MathUtilities.nextPermutation(list);
        assertFalse(result, "No further permutation should exist");
        assertEquals(original, list, "List must remain unchanged when no permutation exists");
    }

    @Test
    void nextPermutation_singleElementList_returnsFalse() {
        List<String> list = new ArrayList<>(Collections.singletonList("only"));
        List<String> original = new ArrayList<>(list);
        boolean result = MathUtilities.nextPermutation(list);
        assertFalse(result, "Single‑element list cannot be permuted");
        assertEquals(original, list, "List must stay the same");
    }

    @Test
    void nextPermutation_emptyList_returnsFalse() {
        List<Integer> list = new ArrayList<>();
        boolean result = MathUtilities.nextPermutation(list);
        assertFalse(result, "Empty list cannot have a permutation");
        assertTrue(list.isEmpty(), "List must remain empty");
    }

    @Test
    void nextPermutation_withDuplicates_returnsTrueAndProducesCorrectOrder() {
        // Starting from 1,1,2 -> next permutation is 1,2,1
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 1, 2));
        boolean result = MathUtilities.nextPermutation(list);
        assertTrue(result, "A next permutation should exist even with duplicates");
        assertEquals(Arrays.asList(1, 2, 1), list, "Permutation with duplicates must follow lexicographic order");
    }

    @Test
    void nextPermutation_multipleCalls_cycleThroughAllPermutations() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3));
        List<List<Integer>> expected = Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(1, 3, 2), Arrays.asList(2, 1, 3), Arrays.asList(2, 3, 1), Arrays.asList(3, 1, 2), Arrays.asList(3, 2, 1));
        // Capture the initial state
        List<List<Integer>> actual = new ArrayList<>();
        actual.add(new ArrayList<>(list));
        while (MathUtilities.nextPermutation(list)) {
            actual.add(new ArrayList<>(list));
        }
        assertEquals(expected, actual, "All permutations should be generated in correct order");
        // After the final permutation, another call must return false
        assertFalse(MathUtilities.nextPermutation(list), "No permutation should exist after the last one");
    }

    /**
     * Two‑element list – ascending order (next permutation exists).
     */
    @Test
    void nextPermutation_twoElements_ascending_returnsTrueAndReverses() {
        List<Integer> list = new ArrayList<>(Arrays.asList(5, 9));
        boolean result = MathUtilities.nextPermutation(list);
        assertTrue(result, "Ascending two‑element list should have a next permutation");
        assertEquals(Arrays.asList(9, 5), list, "Next permutation of [5,9] is [9,5]");
    }

    /**
     * Two‑element list – descending order (already highest).
     */
    @Test
    void nextPermutation_twoElements_descending_returnsFalseAndUnchanged() {
        List<Integer> list = new ArrayList<>(Arrays.asList(9, 5));
        List<Integer> original = new ArrayList<>(list);
        boolean result = MathUtilities.nextPermutation(list);
        assertFalse(result, "Descending two‑element list is the highest permutation");
        assertEquals(original, list, "List must stay unchanged");
    }

    /**
     * Duplicate list where the current permutation is the highest one.
     */
    @Test
    void nextPermutation_duplicates_highest_returnsFalse() {
        List<Integer> list = new ArrayList<>(Arrays.asList(2, 1, 1));
        List<Integer> original = new ArrayList<>(list);
        boolean result = MathUtilities.nextPermutation(list);
        assertFalse(result, "Highest permutation with duplicates should return false");
        assertEquals(original, list, "List must remain unchanged");
    }

    /**
     * Exhaustively iterate through all 24 permutations of a 4‑element list.
     */
    @Test
    void nextPermutation_fourElements_cycleThroughAll24Permutations() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3, 4));
        Set<List<Integer>> seen = new LinkedHashSet<>();
        // initial state
        seen.add(new ArrayList<>(list));
        while (MathUtilities.nextPermutation(list)) {
            seen.add(new ArrayList<>(list));
        }
        // After the loop the list must be in descending order and next call returns false
        assertEquals(Arrays.asList(4, 3, 2, 1), list, "Final permutation should be the descending order");
        assertFalse(MathUtilities.nextPermutation(list), "No further permutation should exist");
        assertEquals(24, seen.size(), "A 4‑element list must generate exactly 24 distinct permutations");
    }

    /**
     * Custom Comparable that defines a reverse numeric ordering.
     */
    @Test
    void nextPermutation_customComparable_reverseOrder() {
        List<ReverseInt> list = new ArrayList<>(Arrays.asList(new ReverseInt(1), new ReverseInt(2), new ReverseInt(3)));
        boolean result = MathUtilities.nextPermutation(list);
        assertTrue(result, "ReverseInt list should have a next permutation");
        // Expected order according to the JSON specification
        List<ReverseInt> expected = Arrays.asList(new ReverseInt(1), new ReverseInt(3), new ReverseInt(2));
        assertEquals(expected, list, "Permutation should respect the custom reverse ordering");
    }

    /**
     * Null element inside the list – should propagate NullPointerException.
     */
    @Test
    void nextPermutation_nullElement_throwsNullPointerException() {
        List<Integer> list = new ArrayList<>(Arrays.asList(null, 1, 2));
        assertThrows(NullPointerException.class, () -> MathUtilities.nextPermutation(list), "A null element should cause NullPointerException");
    }

    /**
     * Null list argument – the method attempts to dereference and throws NPE.
     */
    @Test
    void nextPermutation_nullList_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> MathUtilities.nextPermutation(null), "Passing a null list should result in NullPointerException");
    }

    /**
     * Incompatible element types (Integer vs String) – expect ClassCastException.
     */
    @Test
    void nextPermutation_incompatibleElementTypes_throwsClassCastException() {
        // Suppress unchecked warnings because we deliberately create a mixed list
        @SuppressWarnings("unchecked")
        List list = new ArrayList<>(Arrays.asList(1, "a"));
        assertThrows(ClassCastException.class, () -> MathUtilities.nextPermutation(list), "Mixed element types should cause ClassCastException");
    }

    /**
     * Unmodifiable list – swap() will invoke set() and throw UnsupportedOperationException.
     */
    @Test
    void nextPermutation_unmodifiableList_throwsUnsupportedOperationException() {
        List<Integer> modifiable = new ArrayList<>(Arrays.asList(1, 2, 3));
        List<Integer> unmodifiable = Collections.unmodifiableList(modifiable);
        assertThrows(UnsupportedOperationException.class, () -> MathUtilities.nextPermutation(unmodifiable), "Swapping on an unmodifiable list must throw UnsupportedOperationException");
    }

    /**
     * Elements whose compareTo throws a runtime exception – the exception propagates.
     */
    @Test
    void nextPermutation_compareToThrowsRuntimeException_propagates() {
        List<BadComparable> list = new ArrayList<>(Arrays.asList(new BadComparable(1), new BadComparable(2)));
        assertThrows(RuntimeException.class, () -> MathUtilities.nextPermutation(list), "Exception thrown from compareTo should propagate");
    }
}
