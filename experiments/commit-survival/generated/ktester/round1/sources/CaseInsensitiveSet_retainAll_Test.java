package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveSet_retainAll_Test {

    private CaseInsensitiveSet<String> set;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a fresh set for each test
        set = new CaseInsensitiveSet<>();
        set.add("Apple");
        set.add("Banana");
        set.add("Cherry");
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the set is cleared after each test (good practice)
        if (set != null) {
            set.clear();
        }
    }

    private static Stream<Arguments> basicBehaviourProvider() {
        return Stream.of(// collection exactly matches the set – no change, false
        Arguments.of(List.of("one", "two", "three"), List.of("one", "two", "three"), false, "no element removed"), // collection missing one element – change, true
        Arguments.of(List.of("one", "two", "three"), List.of("one", "three"), true, "element \"two\" removed"), // empty collection – all elements removed, true
        Arguments.of(List.of("one", "two", "three"), Collections.emptyList(), true, "all elements removed"), // collection contains only foreign elements – no removal, false
        Arguments.of(List.of("one", "two", "three"), List.of("four", "five"), false, "no matching elements"));
    }

    private static Stream<Arguments> caseInsensitiveProvider() {
        return Stream.of(// ONE, Two – "three" should be removed → true
        Arguments.of(List.of("one", "two", "three"), List.of("ONE", "Two"), true, "removes \"three\""), // ALL upper case – nothing removed → false
        Arguments.of(List.of("one", "two", "three"), List.of("ONE", "TWO", "THREE"), false, "no removal"));
    }

    private static Stream<Arguments> duplicateElementsProvider() {
        return Stream.of(// duplicates do not affect logic – no removal
        Arguments.of(List.of("a", "b"), List.of("a", "a", "b", "b"), false, "duplicates, all elements retained"), // collection only contains "a" (twice) – "b" removed
        Arguments.of(List.of("a", "b"), List.of("a", "a"), true, "\"b\" removed"));
    }

    private static Stream<Arguments> differentTypesProvider() {
        return Stream.of(// collection of Integers – no match, false
        Arguments.of(List.of("1", "2"), List.of(1, 2), false, "Integer collection, no removal"), // mixed collection – string "1" matches, Integer 2 does not affect removal
        Arguments.of(List.of("1", "2"), List.of("1", 2), false, "Mixed collection, no removal"));
    }

    @Test
    void retainAll_RemovesElementsNotInCollection_ReturnsTrue() {
        // given
        set.add("Apple");
        set.add("Banana");
        set.add("Cherry");
        // lower‑case "banana"
        Collection<String> toRetain = Arrays.asList("Apple", "banana");
        // when
        boolean changed = set.retainAll(toRetain);
        // then
        assertTrue(changed, "retainAll should report a change when elements are removed");
        assertEquals(2, set.size(), "Set should contain exactly two elements after retainAll");
        assertTrue(set.contains("Apple"), "Set should still contain 'Apple'");
        assertTrue(set.contains("Banana"), "Set should still contain 'Banana' (case‑insensitive retain)");
        assertFalse(set.contains("Cherry"), "Set should no longer contain 'Cherry'");
    }

    @Test
    void retainAll_WithCaseInsensitiveMatch_NoChange_ReturnsFalse() {
        // given
        set.add("Apple");
        set.add("Banana");
        set.add("Cherry");
        // different cases
        Collection<String> toRetain = Arrays.asList("apple", "BANANA", "CHERRY");
        // when
        boolean changed = set.retainAll(toRetain);
        // then
        assertFalse(changed, "retainAll should report no change when all elements are retained");
        assertEquals(3, set.size(), "Set size should remain unchanged");
        assertTrue(set.contains("Apple"));
        assertTrue(set.contains("Banana"));
        assertTrue(set.contains("Cherry"));
    }

    @Test
    void retainAll_EmptyCollection_ClearsSet_ReturnsTrue() {
        // given
        set.add("Apple");
        set.add("Banana");
        set.add("Cherry");
        Collection<String> empty = Collections.emptyList();
        // when
        boolean changed = set.retainAll(empty);
        // then
        assertTrue(changed, "retainAll should report a change when the set becomes empty");
        assertTrue(set.isEmpty(), "Set should be empty after retaining an empty collection");
    }

    @Test
    void retainAll_SupersetCollection_NoChange_ReturnsFalse() {
        // given
        set.add("Apple");
        set.add("Banana");
        set.add("Cherry");
        Collection<String> superset = Arrays.asList("Apple", "Banana", "Cherry", "Date");
        // when
        boolean changed = set.retainAll(superset);
        // then
        assertFalse(changed, "retainAll should report no change when the collection contains all set elements");
        assertEquals(3, set.size(), "Set size should remain unchanged");
    }

    @ParameterizedTest(name = "[{index}] retainAll basic – {3}")
    @MethodSource("basicBehaviourProvider")
    void retainAll_BasicBehaviour(List<String> initial, Collection<String> toRetain, boolean expectedChanged, String _description) {
        // given – initialise set with the required elements
        initial.forEach(set::add);
        // when
        boolean changed = set.retainAll(toRetain);
        // then
        assertEquals(expectedChanged, changed, "retainAll should return " + expectedChanged + " when " + _description);
        // verify the final content matches the collection (case‑insensitive)
        Set<String> expected = new HashSet<>(initial);
        expected.retainAll(toRetain);
        assertEquals(expected.size(), set.size(), "Set size after retainAll should match expected size");
        expected.forEach(e -> assertTrue(set.contains(e), "Set should still contain element '" + e + "'"));
    }

    @ParameterizedTest(name = "[{index}] retainAll case‑insensitive – {3}")
    @MethodSource("caseInsensitiveProvider")
    void retainAll_CaseInsensitive(List<String> initial, Collection<String> toRetain, boolean expectedChanged, String _description) {
        initial.forEach(set::add);
        boolean changed = set.retainAll(toRetain);
        assertEquals(expectedChanged, changed, "Expected change flag " + expectedChanged + " when " + _description);
        // Validate that the remaining elements are exactly those that match case‑insensitively
        Set<String> expected = new HashSet<>(initial);
        expected.retainAll(toRetain);
        assertEquals(expected.size(), set.size(), "Resulting set size does not match expectation");
        expected.forEach(e -> assertTrue(set.contains(e), "Expected element '" + e + "' to be present"));
    }

    @Test
    void retainAll_OnEmptySet_NonEmptyCollection_ReturnsFalse() {
        // given – set is empty
        assertTrue(set.isEmpty());
        // when
        boolean changed = set.retainAll(List.of("any"));
        // then
        assertFalse(changed, "retainAll on an empty set should report no change");
        assertTrue(set.isEmpty(), "Set should remain empty");
    }

    @Test
    void retainAll_OnEmptySet_EmptyCollection_ReturnsFalse() {
        // given – both set and collection are empty
        assertTrue(set.isEmpty());
        // when
        boolean changed = set.retainAll(Collections.emptyList());
        // then
        assertFalse(changed, "retainAll with both empty should report no change");
        assertTrue(set.isEmpty(), "Set should remain empty");
    }

    @ParameterizedTest(name = "[{index}] retainAll duplicate elements – {3}")
    @MethodSource("duplicateElementsProvider")
    void retainAll_DuplicateElements(List<String> initial, Collection<String> toRetain, boolean expectedChanged, String _description) {
        initial.forEach(set::add);
        boolean changed = set.retainAll(toRetain);
        assertEquals(expectedChanged, changed, "Expected change flag " + expectedChanged + " when " + _description);
        // Expected final set is the intersection (case‑insensitive)
        Set<String> expected = new HashSet<>(initial);
        expected.retainAll(toRetain);
        assertEquals(expected.size(), set.size(), "Final set size mismatch");
        expected.forEach(e -> assertTrue(set.contains(e), "Expected element '" + e + "' to remain"));
    }

    @Test
    void retainAll_CollectionContainsNull_ThrowsNullPointerException() {
        // given – set with two strings
        set.add("x");
        set.add("y");
        List<String> collectionWithNull = new ArrayList<>();
        collectionWithNull.add(null);
        // when / then
        assertThrows(NullPointerException.class, () -> set.retainAll(collectionWithNull), "retainAll should throw NPE when collection contains null");
    }

    @Test
    void retainAll_NullCollectionArgument_ThrowsNullPointerException() {
        // given – set already contains some elements
        set.add("anything");
        // when / then
        assertThrows(NullPointerException.class, () -> set.retainAll(null), "retainAll should throw NPE when the collection argument is null");
    }

    @ParameterizedTest(name = "[{index}] retainAll different element types – {3}")
    @MethodSource("differentTypesProvider")
    void retainAll_DifferentElementTypes(List<String> initial, Collection<?> toRetain, boolean expectedChanged, String _description) {
        // initialise set with the string elements
        initial.forEach(set::add);
        boolean changed = set.retainAll(toRetain);
        assertEquals(expectedChanged, changed, "Expected change flag " + expectedChanged + " when " + _description);
        // The set should stay unchanged
        assertEquals(initial.size(), set.size(), "Set size should remain unchanged");
        initial.forEach(e -> assertTrue(set.contains(e), "Set should still contain original element '" + e + "'"));
    }
}
