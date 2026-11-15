package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveSet_retainAll_Test {

    private CaseInsensitiveSet<Object> set;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a fresh set before each test
        set = new CaseInsensitiveSet<>();
        set.add("One");
        set.add("Two");
        set.add("Three");
        // non‑String element to verify mixed‑type handling
        set.add(42);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the template tidy
        set = null;
    }

    private static Stream<Arguments> emptyCollectionScenarios() {
        return Stream.of(// set had elements, should be cleared → true
        Arguments.of(false, true), // set already empty → false
        Arguments.of(true, false));
    }

    /**
     * retainAll should remove elements that are not present in the supplied collection.
     * The method must return {@code true} when the set is modified.
     */
    @Test
    void retainAllRemovesMissingElements() {
        // GIVEN
        // "Three" and 42 are absent
        Collection<Object> toRetain = Arrays.asList("One", "Two");
        // WHEN
        boolean modified = set.retainAll(toRetain);
        // THEN
        assertTrue(modified, "retainAll should report modification when elements are removed");
        assertEquals(2, set.size(), "Set should contain only the retained elements");
        assertTrue(set.contains("One"));
        assertTrue(set.contains("Two"));
        assertFalse(set.contains("Three"));
        assertFalse(set.contains(42));
    }

    /**
     * When the collection supplied to {@code retainAll} contains all elements of the set,
     * the set must remain unchanged and the method should return {@code false}.
     */
    @Test
    void retainAllNoChangeWhenAllElementsPresent() {
        // GIVEN
        Collection<Object> toRetain = Arrays.asList("One", "Two", "Three", 42);
        // WHEN
        boolean modified = set.retainAll(toRetain);
        // THEN
        assertFalse(modified, "retainAll should report no modification when all elements are retained");
        assertEquals(4, set.size(), "Set size must stay unchanged");
        assertTrue(set.containsAll(toRetain));
    }

    /**
     * Case‑insensitive handling: a collection that contains strings differing only by case
     * should be considered a match and therefore retained.
     */
    @Test
    void retainAllIsCaseInsensitiveForStrings() {
        // GIVEN
        // different case from stored values
        Collection<Object> toRetain = Arrays.asList("one", "tWo");
        // WHEN
        boolean modified = set.retainAll(toRetain);
        // THEN
        assertTrue(modified, "retainAll should modify the set when case‑insensitive matches reduce the contents");
        assertEquals(2, set.size(), "Only the two case‑insensitively matched strings should remain");
        assertTrue(set.contains("One"));
        assertTrue(set.contains("Two"));
        assertFalse(set.contains("Three"));
        assertFalse(set.contains(42));
    }

    /**
     * Mixed‑type collection: ensure that non‑String elements are compared using normal equality.
     */
    @Test
    void retainAllHandlesMixedTypes() {
        // GIVEN
        Collection<Object> toRetain = Collections.singletonList(42);
        // WHEN
        boolean modified = set.retainAll(toRetain);
        // THEN
        assertTrue(modified, "retainAll should modify the set when only a non‑String element is retained");
        assertEquals(1, set.size());
        assertTrue(set.contains(42));
        assertFalse(set.contains("One"));
        assertFalse(set.contains("Two"));
        assertFalse(set.contains("Three"));
    }

    /**
     * An empty collection passed to {@code retainAll} should clear the set and return {@code true}
     * if the set was not already empty.
     */
    @Test
    void retainAllWithEmptyCollectionClearsSet() {
        // GIVEN
        Collection<Object> empty = Collections.emptyList();
        // WHEN
        boolean modified = set.retainAll(empty);
        // THEN
        assertTrue(modified, "retainAll should report modification when clearing a non‑empty set");
        assertTrue(set.isEmpty(), "Set should be empty after retaining an empty collection");
    }

    /**
     * When the set is already empty, retainAll must return {@code false} regardless of the collection.
     */
    @Test
    void retainAllOnEmptySetReturnsFalse() {
        // GIVEN
        // make the set empty
        set.clear();
        Collection<Object> any = Arrays.asList("anything", 123);
        // WHEN
        boolean modified = set.retainAll(any);
        // THEN
        assertFalse(modified, "retainAll on an already empty set should report no modification");
        assertTrue(set.isEmpty(), "Set should remain empty");
    }

    /**
     * Parameterized test covering the two scenarios from the JSON group
     * “retainAll with empty collection”.
     * - Non‑empty set + empty collection → modified == true
     * - Empty set + empty collection   → modified == false
     */
    @ParameterizedTest
    @MethodSource("emptyCollectionScenarios")
    void retainAllEmptyCollectionScenarios(boolean initiallyEmpty, boolean expectedModified) {
        // GIVEN
        if (initiallyEmpty) {
            set.clear();
        }
        Collection<Object> empty = Collections.emptyList();
        // WHEN
        boolean modified = set.retainAll(empty);
        // THEN
        assertEquals(expectedModified, modified, "retainAll with empty collection returned unexpected modification flag");
        if (expectedModified) {
            assertTrue(set.isEmpty(), "Set should be empty after modification");
        } else {
            assertTrue(set.isEmpty(), "Set should already be empty");
        }
    }

    /**
     * The collection contains only {@code null}. The set does not contain {@code null},
     * therefore all existing elements are removed and the method returns {@code true}.
     */
    @Test
    void retainAllWithNullElementInCollection() {
        // GIVEN
        Collection<Object> collectionWithNull = Collections.singletonList(null);
        // WHEN
        boolean modified = set.retainAll(collectionWithNull);
        // THEN
        assertTrue(modified, "retainAll should modify the set when collection contains only null");
        assertTrue(set.isEmpty(), "All elements should be removed because none match null");
    }

    /**
     * The set holds only {@link String} elements. Supplying a collection that mixes
     * incompatible types (e.g., {@link Integer}) should cause a {@link ClassCastException}
     * because the method casts each collection element to the set's generic type.
     */
    @Test
    void retainAllWithIncompatibleElementTypeThrows() {
        // GIVEN a set that only accepts Strings
        CaseInsensitiveSet<String> stringSet = new CaseInsensitiveSet<>();
        stringSet.add("A");
        stringSet.add("B");
        // 123 cannot be cast to String
        Collection<Object> mixed = Arrays.asList(123, "A");
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> stringSet.retainAll(mixed), "retainAll should throw ClassCastException when collection contains an element of incompatible type");
    }

    /**
     * Passing {@code null} as the collection argument must result in a {@link NullPointerException}
     * as the implementation iterates over the collection without a null‑check.
     */
    @Test
    void retainAllWithNullCollectionArgumentThrows() {
        // GIVEN
        Collection<?> nullCollection = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> set.retainAll(nullCollection), "retainAll should throw NullPointerException when the collection argument is null");
    }

    /**
     * Verify that case‑insensitive matching works when *all* elements match,
     * i.e., the method returns {@code false} because nothing is removed.
     */
    @Test
    void retainAllAllElementsMatchCaseInsensitively() {
        // GIVEN a set with known strings
        CaseInsensitiveSet<String> ciSet = new CaseInsensitiveSet<>();
        ciSet.add("A");
        ciSet.add("B");
        // Collection contains same strings with different case
        Collection<Object> collection = Arrays.asList("a", "B");
        // WHEN
        boolean modified = ciSet.retainAll(collection);
        // THEN
        assertFalse(modified, "retainAll should not modify the set when all elements match case‑insensitively");
        assertEquals(2, ciSet.size());
        assertTrue(ciSet.contains("A"));
        assertTrue(ciSet.contains("B"));
    }
}
