package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CompactSet_contains_Test {

    /**
     * Simple subclass that forces a tiny compactSize so we can reach the Set‑based state quickly.
     */
    private static class TinyCompactSet<E> extends CompactSet<E> {

        @Override
        protected int compactSize() {
            // Transition to Set after only 2 elements (used by some existing tests)
            return 2;
        }

        @Override
        protected Set<E> getNewSet() {
            // Use a regular HashSet for the delegate Set
            return new HashSet<>();
        }
    }

    /**
     * Subclass that allows the compactSize to be defined at construction time.
     */
    private static class CustomCompactSet<E> extends CompactSet<E> {

        private final int threshold;

        CustomCompactSet(int threshold) {
            this.threshold = threshold;
        }

        @Override
        protected int compactSize() {
            return threshold;
        }

        @Override
        protected Set<E> getNewSet() {
            return new HashSet<>();
        }
    }

    static Stream<Arguments> emptySetProvider() {
        return Stream.of(Arguments.of("Empty set should return false for any item", "anything", false), Arguments.of("Empty set should return false for null", null, false));
    }

    static Stream<Arguments> singleElementProvider() {
        return Stream.of(Arguments.of("Contains should return true for the added element \"alpha\"", "alpha", "alpha", true), Arguments.of("Contains should return false for a different element \"beta\"", "alpha", "beta", false));
    }

    static Stream<Arguments> arrayRepresentationProvider() {
        List<Integer> base = Arrays.asList(1, 2, 3);
        return Stream.of(Arguments.of("Array mode: should contain 1", base, 1, true), Arguments.of("Array mode: should contain 2", base, 2, true), Arguments.of("Array mode: should NOT contain 4", base, 4, false));
    }

    static Stream<Arguments> compactSizeThresholdProvider() {
        // threshold = 8, add 0‑7 (8 elements) -> still array mode
        List<Integer> eightElements = new ArrayList<>();
        for (int i = 0; i < 8; i++) eightElements.add(i);
        return Stream.of(Arguments.of("At threshold (size==compactSize): should contain 7", 8, eightElements, 7, true), Arguments.of("At threshold (size==compactSize): should NOT contain 8", 8, eightElements, 8, false));
    }

    static Stream<Arguments> beyondCompactSizeProvider() {
        // threshold = 8, add 0‑9 (10 elements) -> should have switched to Set mode
        List<Integer> tenElements = new ArrayList<>();
        for (int i = 0; i < 10; i++) tenElements.add(i);
        return Stream.of(Arguments.of("Beyond threshold: should contain 9", 8, tenElements, 9, true), Arguments.of("Beyond threshold: should NOT contain 10", 8, tenElements, 10, false));
    }

    @Test
    @DisplayName("contains() returns false when the set is empty")
    void testContainsEmptySetReturnsFalse() {
        CompactSet<String> set = new CompactSet<>();
        assertFalse(set.contains("anything"), "Empty set should not contain any element");
        assertFalse(set.contains(null), "Empty set should not contain null");
    }

    @Test
    @DisplayName("contains() finds an element stored in the internal array")
    void testContainsInArrayTrue() {
        CompactSet<String> set = new CompactSet<>();
        set.add("alpha");
        set.add("beta");
        assertTrue(set.contains("alpha"), "Should contain previously added element");
        assertTrue(set.contains("beta"), "Should contain previously added element");
    }

    @Test
    @DisplayName("contains() returns false for a non‑existent element in the internal array")
    void testContainsInArrayFalse() {
        CompactSet<String> set = new CompactSet<>();
        set.add("alpha");
        set.add("beta");
        assertFalse(set.contains("gamma"), "Should not contain element that was never added");
    }

    @Test
    @DisplayName("contains() works correctly after transition to delegate Set")
    void testContainsInSetTrue() {
        // Using the tiny compact size subclass to force transition after 3 adds
        CompactSet<String> set = new TinyCompactSet<>();
        set.add("one");
        set.add("two");
        // At this point we are still in array mode (size == compactSize)
        // triggers transition to Set mode
        set.add("three");
        assertTrue(set.contains("one"));
        assertTrue(set.contains("two"));
        assertTrue(set.contains("three"));
    }

    @Test
    @DisplayName("contains() returns false for missing element after transition to delegate Set")
    void testContainsInSetFalse() {
        CompactSet<String> set = new TinyCompactSet<>();
        set.add("one");
        set.add("two");
        // transition to Set mode
        set.add("three");
        assertFalse(set.contains("four"));
    }

    @Test
    @DisplayName("contains() correctly handles a stored null element")
    void testContainsNullElement() {
        CompactSet<String> set = new CompactSet<>();
        set.add(null);
        assertTrue(set.contains(null), "Set should report containing null after it is added");
        assertFalse(set.contains("non‑null"), "Set should not contain unrelated non‑null element");
    }

    @Test
    @DisplayName("contains() works with a collection‑based constructor")
    void testContainsWithCollectionConstructor() {
        Collection<String> initial = Set.of("a", "b", "c");
        CompactSet<String> set = new CompactSet<>(initial);
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertTrue(set.contains("c"));
        assertFalse(set.contains("d"));
    }

    // ---------- Group: contains method - empty set ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("emptySetProvider")
    void testEmptySet(String description, Object item, boolean expected) {
        CompactSet<Object> set = new CompactSet<>();
        assertEquals(expected, set.contains(item), description);
    }

    // ---------- Group: contains method - single element ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("singleElementProvider")
    void testSingleElement(String description, Object itemToAdd, Object queryItem, boolean expected) {
        CompactSet<Object> set = new CompactSet<>();
        set.add(itemToAdd);
        assertEquals(expected, set.contains(queryItem), description);
    }

    // ---------- Group: multiple elements within array representation ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("arrayRepresentationProvider")
    void testArrayRepresentation(String description, List<Integer> toAdd, Integer query, boolean expected) {
        CompactSet<Integer> set = new CompactSet<>();
        toAdd.forEach(set::add);
        assertEquals(expected, set.contains(query), description);
    }

    // ---------- Group: at compactSize threshold ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("compactSizeThresholdProvider")
    void testAtCompactSizeThreshold(String description, int threshold, List<Integer> toAdd, Integer query, boolean expected) {
        CompactSet<Integer> set = new CustomCompactSet<>(threshold);
        toAdd.forEach(set::add);
        assertEquals(expected, set.contains(query), description);
    }

    // ---------- Group: beyond compactSize (set representation) ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("beyondCompactSizeProvider")
    void testBeyondCompactSize(String description, int threshold, List<Integer> toAdd, Integer query, boolean expected) {
        CompactSet<Integer> set = new CustomCompactSet<>(threshold);
        toAdd.forEach(set::add);
        assertEquals(expected, set.contains(query), description);
    }

    // ---------- Group: null element handling ----------
    @Test
    @DisplayName("contains() correctly reports a stored null element")
    void testContainsNullElementHandling() {
        CompactSet<Object> set = new CompactSet<>();
        set.add(null);
        assertTrue(set.contains(null), "Set should contain null after it is added");
        assertFalse(set.contains("nonNull"), "Set should not contain unrelated non‑null element");
    }

    // ---------- Group: different type mismatch ----------
    @Test
    @DisplayName("contains() returns false when queried with a different type")
    void testContainsDifferentTypeMismatch() {
        CompactSet<Integer> set = new CompactSet<>();
        set.add(1);
        set.add(2);
        set.add(3);
        assertFalse(set.contains("string"), "Querying with a different type should return false");
    }
}
