package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static com.cedarsoftware.util.MathUtilities.nextPermutation;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.*;
import java.util.stream.*;

@ExtendWith(MockitoExtension.class)
class MathUtilities_nextPermutation_Test {

    class Box implements Comparable<Box> {

        final int value;

        Box(int v) {
            this.value = v;
        }

        @Override
        public int compareTo(Box o) {
            return Integer.compare(this.value, o.value);
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof Box) && ((Box) o).value == this.value;
        }

        @Override
        public String toString() {
            return "Box(" + value + ")";
        }
    }

    /**
     * Supplies all integer‑based test cases from the JSON specification.
     */
    static Stream<Arguments> intListProvider() {
        return Stream.of(// Empty list
        Arguments.of("Empty list – no permutation possible", new ArrayList<Integer>(), false, Collections.emptyList()), // Single‑element list
        Arguments.of("Single‑element list – no permutation possible", new ArrayList<>(Collections.singletonList(42)), false, Collections.singletonList(42)), // Two‑element lists
        Arguments.of("Two‑element ascending list", new ArrayList<>(Arrays.asList(1, 2)), true, Arrays.asList(2, 1)), Arguments.of("Two‑element descending list (already highest)", new ArrayList<>(Arrays.asList(2, 1)), false, Arrays.asList(2, 1)), // Already highest permutation (descending order)
        Arguments.of("Descending list of five elements – highest permutation", new ArrayList<>(Arrays.asList(5, 4, 3, 2, 1)), false, Arrays.asList(5, 4, 3, 2, 1)), // Typical permutation progression
        Arguments.of("First step from [1,2,3]", new ArrayList<>(Arrays.asList(1, 2, 3)), true, Arrays.asList(1, 3, 2)), Arguments.of("Second step from [1,3,2]", new ArrayList<>(Arrays.asList(1, 3, 2)), true, Arrays.asList(2, 1, 3)), Arguments.of("Later step from [2,3,1]", new ArrayList<>(Arrays.asList(2, 3, 1)), true, Arrays.asList(3, 1, 2)), // Lists with duplicate elements
        Arguments.of("Duplicate elements – next permutation exists", new ArrayList<>(Arrays.asList(1, 1, 2)), true, Arrays.asList(1, 2, 1)), Arguments.of("Duplicate elements – already highest", new ArrayList<>(Arrays.asList(2, 1, 1)), false, Arrays.asList(2, 1, 1)), // Lists with negative numbers
        Arguments.of("Negative numbers – first permutation", new ArrayList<>(Arrays.asList(-3, -2, -1)), true, Arrays.asList(-3, -1, -2)), Arguments.of("Negative numbers – descending (highest)", new ArrayList<>(Arrays.asList(-1, -2, -3)), false, Arrays.asList(-1, -2, -3)), // Large list (size > 10)
        Arguments.of("Large list – minimal suffix reversal", new ArrayList<>(IntStream.rangeClosed(1, 10).boxed().collect(Collectors.toList())), true, Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 10, 9)));
    }

    /**
     * Supplies the three variations where a {@code null} appears at the
     * beginning, middle, or end of the list.
     */
    static Stream<Arguments> nullElementListProvider() {
        return Stream.of(Arguments.of(0, new ArrayList<>(Arrays.asList(null, 1, 2))), Arguments.of(1, new ArrayList<>(Arrays.asList(1, null, 2))), Arguments.of(2, new ArrayList<>(Arrays.asList(1, 2, null))));
    }

    @Test
    void testNextPermutationSimpleIncreasing() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3));
        boolean hasNext = nextPermutation(list);
        assertTrue(hasNext, "Should have a next permutation");
        assertEquals(Arrays.asList(1, 3, 2), list, "First next permutation of [1,2,3]");
        hasNext = nextPermutation(list);
        assertTrue(hasNext);
        assertEquals(Arrays.asList(2, 1, 3), list);
        hasNext = nextPermutation(list);
        assertTrue(hasNext);
        assertEquals(Arrays.asList(2, 3, 1), list);
        hasNext = nextPermutation(list);
        assertTrue(hasNext);
        assertEquals(Arrays.asList(3, 1, 2), list);
        hasNext = nextPermutation(list);
        assertTrue(hasNext);
        assertEquals(Arrays.asList(3, 2, 1), list);
        // Last permutation – next call should return false and leave list unchanged
        hasNext = nextPermutation(list);
        assertFalse(hasNext, "No further permutations should exist");
        assertEquals(Arrays.asList(3, 2, 1), list);
    }

    @Test
    void testNextPermutationWithDuplicates() {
        // The algorithm works on comparable elements; duplicates are allowed.
        List<String> list = new ArrayList<>(Arrays.asList("a", "a", "b"));
        boolean hasNext = nextPermutation(list);
        assertTrue(hasNext);
        assertEquals(Arrays.asList("a", "b", "a"), list);
        hasNext = nextPermutation(list);
        assertTrue(hasNext);
        assertEquals(Arrays.asList("b", "a", "a"), list);
        hasNext = nextPermutation(list);
        assertFalse(hasNext);
        assertEquals(Arrays.asList("b", "a", "a"), list);
    }

    @Test
    void testNextPermutationSingleElement() {
        List<Integer> list = new ArrayList<>(Collections.singletonList(42));
        boolean hasNext = nextPermutation(list);
        assertFalse(hasNext, "A single‑element list has no next permutation");
        assertEquals(Collections.singletonList(42), list);
    }

    @Test
    void testNextPermutationEmptyList() {
        List<Integer> list = new ArrayList<>();
        boolean hasNext = nextPermutation(list);
        assertFalse(hasNext, "An empty list has no next permutation");
        assertTrue(list.isEmpty(), "List should remain empty");
    }

    @Test
    void testNextPermutationAlreadyHighestLexicographic() {
        List<Integer> list = new ArrayList<>(Arrays.asList(3, 2, 1));
        boolean hasNext = nextPermutation(list);
        assertFalse(hasNext, "Highest lexicographic permutation should return false");
        assertEquals(Arrays.asList(3, 2, 1), list);
    }

    @Test
    void testNextPermutationLongerList() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3, 4));
        // Verify a few steps forward
        assertTrue(nextPermutation(list));
        assertEquals(Arrays.asList(1, 2, 4, 3), list);
        assertTrue(nextPermutation(list));
        assertEquals(Arrays.asList(1, 3, 2, 4), list);
        // Jump to the last permutation
        while (nextPermutation(list)) {
            // iterate until false
        }
        assertEquals(Arrays.asList(4, 3, 2, 1), list);
    }

    @Test
    void testNextPermutationWithCustomComparable() {
        class Box implements Comparable<Box> {

            final int value;

            Box(int v) {
                this.value = v;
            }

            @Override
            public int compareTo(Box o) {
                return Integer.compare(this.value, o.value);
            }

            @Override
            public boolean equals(Object o) {
                return (o instanceof Box) && ((Box) o).value == this.value;
            }

            @Override
            public String toString() {
                return "Box(" + value + ")";
            }
        }
        List<Box> list = new ArrayList<>(Arrays.asList(new Box(2), new Box(1), new Box(3)));
        assertTrue(nextPermutation(list), "Custom Comparable should produce next permutation");
        assertEquals(Arrays.asList(new Box(2), new Box(3), new Box(1)), list, "Permutation result for custom Comparable is incorrect");
    }

    /**
     * Test groups that operate on {@code List<Integer>} – empty, single,
     * two‑element, typical progression, duplicate elements, negative numbers,
     * and a larger list (>10 elements).
     *
     * Each {@link Arguments} entry contains:
     *   1. A description (used only for readability in the test name)
     *   2. The initial list (as a mutable {@link ArrayList})
     *   3. The expected boolean return value
     *   4. The expected list state after the call
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("intListProvider")
    void testNextPermutationIntLists(String description, List<Integer> input, boolean expectedReturn, List<Integer> expectedAfter) {
        // Act
        boolean actualReturn = nextPermutation(input);
        // Assert
        assertEquals(expectedReturn, actualReturn, () -> "Return value mismatch for case: " + description);
        assertEquals(expectedAfter, input, () -> "List state mismatch for case: " + description);
    }

    /**
     * Tests that a {@code null} list reference triggers a {@link NullPointerException}.
     * The method does not perform an explicit null‑check, so the NPE originates from
     * the first dereference of the argument.
     */
    @Test
    void testNextPermutationNullListThrows() {
        assertThrows(NullPointerException.class, () -> nextPermutation(null), "Passing a null list should throw NullPointerException");
    }

    /**
     * Parameterised test for lists that contain a {@code null} element.
     * The algorithm will attempt to call {@code compareTo} on the {@code null}
     * element, causing a {@link NullPointerException}.
     */
    @ParameterizedTest(name = "Null element at index {0}")
    @MethodSource("nullElementListProvider")
    void testNextPermutationNullElementThrows(int nullIndex, List<Integer> input) {
        assertThrows(NullPointerException.class, () -> nextPermutation(input), () -> "Expected NullPointerException for null element at index " + nullIndex);
    }
}
