package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ConcurrentSet_addAll_Test {

    private ConcurrentSet<Integer> intSet;

    private ConcurrentSet<String> stringSet;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh instances for each test to guarantee isolation.
        intSet = new ConcurrentSet<>();
        stringSet = new ConcurrentSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help the garbage collector – not strictly required but keeps the pattern clear.
        intSet = null;
        stringSet = null;
    }

    @Test
    void addAllReturnsTrueWhenAtLeastOneElementIsNew() {
        // given
        List<Integer> source = Arrays.asList(1, 2, 3);
        // when
        boolean modified = intSet.addAll(source);
        // then
        assertTrue(modified, "addAll should return true when the set is modified");
        assertEquals(3, intSet.size(), "Set size should reflect the added elements");
        assertTrue(intSet.containsAll(source), "All source elements must be present");
    }

    @Test
    void addAllReturnsFalseWhenAllElementsAlreadyExist() {
        // given
        intSet.addAll(Arrays.asList(5, 6, 7));
        // when
        boolean modified = intSet.addAll(Arrays.asList(5, 6, 7));
        // then
        assertFalse(modified, "addAll should return false when no new elements are added");
        assertEquals(3, intSet.size(), "Set size must remain unchanged");
    }

    @Test
    void addAllHandlesMixedExistingAndNewElements() {
        // given
        intSet.addAll(Arrays.asList(10, 20));
        // when
        boolean modified = intSet.addAll(Arrays.asList(20, 30, 40));
        // then
        assertTrue(modified, "addAll should return true because new elements were added");
        assertEquals(4, intSet.size(), "Set should contain the original plus the new elements");
        assertTrue(intSet.containsAll(Arrays.asList(10, 20, 30, 40)));
    }

    @Test
    void addAllWithEmptyCollectionDoesNotModifySet() {
        // given
        intSet.addAll(Arrays.asList(1, 2));
        // when
        boolean modified = intSet.addAll(Collections.emptyList());
        // then
        assertFalse(modified, "addAll should return false for an empty collection");
        assertEquals(2, intSet.size(), "Set size must stay the same");
    }

    @Test
    void addAllAcceptsNullElementsWhenSetSupportsThem() {
        // given
        List<String> source = Arrays.asList("a", null, "b");
        // when
        boolean modified = stringSet.addAll(source);
        // then
        assertTrue(modified, "addAll should report modification when null is added");
        assertEquals(3, stringSet.size(), "Set size must include the null element");
        assertTrue(stringSet.contains("a"));
        assertTrue(stringSet.contains(null));
        assertTrue(stringSet.contains("b"));
    }

    @Test
    void addAllReturnsFalseIfOnlyNullAlreadyPresent() {
        // given
        stringSet.add(null);
        // when
        boolean modified = stringSet.addAll(Collections.singletonList(null));
        // then
        assertFalse(modified, "addAll should return false when only duplicate nulls are added");
        assertEquals(1, stringSet.size(), "Set should still contain exactly one null element");
    }

    @Test
    void addAllDoesNotExposeInternalCollection() {
        // given
        List<Integer> source = new ArrayList<>(Arrays.asList(100, 200));
        // when
        intSet.addAll(source);
        // mutate the original collection after the call
        source.clear();
        // then
        assertEquals(2, intSet.size(), "Modifications to the source collection after addAll must not affect the set");
        assertTrue(intSet.containsAll(Arrays.asList(100, 200)));
    }

    /**
     * addAll – empty collection
     */
    @Test
    void addAllEmptyCollectionReturnsFalse() {
        // given
        List<String> empty = Collections.emptyList();
        // when
        boolean modified = stringSet.addAll(empty);
        // then
        assertFalse(modified, "Calling addAll with an empty collection should not modify the set and must return false.");
        assertEquals(0, stringSet.size(), "Set must remain empty.");
    }

    /**
     * addAll – collection with all new elements
     */
    @Test
    void addAllAllNewElementsReturnsTrue() {
        // given
        List<String> newElems = Arrays.asList("x", "y", "z");
        // when
        boolean modified = stringSet.addAll(newElems);
        // then
        assertTrue(modified, "All elements are new, so addAll must return true.");
        assertEquals(3, stringSet.size(), "All new elements must be present.");
        assertTrue(stringSet.containsAll(newElems));
    }

    /**
     * addAll – collection with some existing elements
     */
    @Test
    void addAllSomeExistingElementsReturnsTrue() {
        // given: set already contains "a"
        stringSet.add("a");
        List<String> mixed = Arrays.asList("a", "b", "c");
        // when
        boolean modified = stringSet.addAll(mixed);
        // then
        assertTrue(modified, "At least one new element (b,c) is added, so result must be true.");
        assertEquals(3, stringSet.size(), "Set should contain a, b and c.");
        assertTrue(stringSet.containsAll(mixed));
    }

    /**
     * addAll – collection with all existing elements
     */
    @Test
    void addAllAllExistingElementsReturnsFalse() {
        // given
        List<String> existing = Arrays.asList("p", "q");
        stringSet.addAll(existing);
        // when
        boolean modified = stringSet.addAll(existing);
        // then
        assertFalse(modified, "No new elements are added, so addAll must return false.");
        assertEquals(2, stringSet.size(), "Set size must stay unchanged.");
    }

    /**
     * addAll – collection containing duplicate elements
     */
    @Test
    void addAllDuplicateElementsAddsDistinctOnlyOnce() {
        // given
        List<String> dupes = Arrays.asList("d", "d", "e");
        // when
        boolean modified = stringSet.addAll(dupes);
        // then
        assertTrue(modified, "At least one distinct element is added, so result must be true.");
        assertEquals(2, stringSet.size(), "Duplicate 'd' should be stored only once.");
        assertTrue(stringSet.containsAll(Arrays.asList("d", "e")));
    }

    /**
     * addAll – single‑element collection
     */
    @Test
    void addAllSingleElementCollectionReturnsTrue() {
        // given
        List<String> single = Collections.singletonList("single");
        // when
        boolean modified = stringSet.addAll(single);
        // then
        assertTrue(modified, "Adding a single new element must return true.");
        assertEquals(1, stringSet.size(), "Set should contain exactly one element.");
        assertTrue(stringSet.contains("single"));
    }

    /**
     * addAll – null collection argument
     */
    @Test
    void addAllNullCollectionThrowsNullPointerException() {
        // when / then
        assertThrows(NullPointerException.class, () -> intSet.addAll(null), "Passing a null collection must throw NullPointerException.");
    }

    /**
     * addAll – collection containing null element
     */
    @Test
    void addAllCollectionWithNullElementThrowsNullPointerException() {
        // given
        List<String> listWithNull = new ArrayList<>();
        listWithNull.add(null);
        // when / then
        assertThrows(NullPointerException.class, () -> stringSet.addAll(listWithNull), "Adding a collection that contains a null element must throw NullPointerException.");
    }

    /**
     * addAll – unmodifiable collection
     */
    @Test
    void addAllUnmodifiableCollectionReturnsTrue() {
        // given
        List<String> unmodifiable = Collections.unmodifiableList(Arrays.asList("u1", "u2"));
        // when
        boolean modified = stringSet.addAll(unmodifiable);
        // then
        assertTrue(modified, "Reading from an unmodifiable collection should succeed and return true.");
        assertEquals(2, stringSet.size(), "Both elements must be added.");
        assertTrue(stringSet.containsAll(Arrays.asList("u1", "u2")));
    }
}
