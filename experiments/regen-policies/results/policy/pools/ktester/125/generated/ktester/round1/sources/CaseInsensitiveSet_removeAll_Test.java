package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveSet_removeAll_Test {

    private CaseInsensitiveSet<Object> set;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh set for each test and populate it with a known mixture
        set = new CaseInsensitiveSet<>();
        set.add("One");
        set.add("Two");
        set.add("Three");
        set.add(42);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and ensure no cross‑test contamination
        set = null;
    }

    /**
     * Supplies arguments for the basic functionality groups:
     * - all elements removed,
     * - some elements removed,
     * - no elements removed,
     * - empty collection,
     * - duplicate elements in collection.
     */
    private static Stream<Arguments> basicScenariosProvider() {
        return Stream.of(// All elements removed
        Arguments.of(// initial set contents
        new LinkedHashSet<>(Arrays.asList("alpha", "beta")), // collection to remove
        Arrays.asList("alpha", "beta"), // expected return value
        true, // expected final size
        0), // Some elements removed (one present, one absent)
        Arguments.of(new LinkedHashSet<>(Arrays.asList("gamma", "epsilon", "zeta")), Arrays.asList("gamma", "delta"), true, 2), // No elements removed
        Arguments.of(new LinkedHashSet<>(Arrays.asList("kappa", "lambda")), Arrays.asList("theta", "iota"), false, 2), // Empty collection (nothing to remove)
        Arguments.of(new LinkedHashSet<>(Arrays.asList("a", "b")), Collections.emptyList(), false, 2), // Duplicate elements in collection
        Arguments.of(new LinkedHashSet<>(Collections.singletonList("mu")), Arrays.asList("mu", "mu"), true, 0));
    }

    @Test
    void testRemoveAllRemovesExistingElements() {
        // given
        Collection<Object> toRemove = Arrays.asList("One", "Two");
        // when
        boolean modified = set.removeAll(toRemove);
        // then
        assertTrue(modified, "removeAll should report modification when elements are removed");
        assertEquals(2, set.size(), "Set size should reflect the removed elements");
        assertFalse(set.contains("One"));
        assertFalse(set.contains("Two"));
        assertTrue(set.contains("Three"));
        assertTrue(set.contains(42));
    }

    @Test
    void testRemoveAllIsCaseInsensitiveForStrings() {
        // given
        // lower‑case version
        Collection<Object> toRemove = Collections.singletonList("one");
        // when
        boolean modified = set.removeAll(toRemove);
        // then
        assertTrue(modified, "removeAll should remove a string irrespective of case");
        assertFalse(set.contains("ONE"));
        assertEquals(3, set.size());
    }

    @Test
    void testRemoveAllPartialMatchLeavesOtherElementsIntact() {
        // given
        Collection<Object> toRemove = Arrays.asList("Three", "nonexistent", 99);
        // when
        boolean modified = set.removeAll(toRemove);
        // then
        assertTrue(modified, "removeAll should return true when at least one element is removed");
        assertFalse(set.contains("Three"));
        assertTrue(set.contains("One"));
        assertTrue(set.contains("Two"));
        assertTrue(set.contains(42));
        assertEquals(3, set.size());
    }

    @Test
    void testRemoveAllWithNoMatchingElementsReturnsFalse() {
        // given
        Collection<Object> toRemove = Arrays.asList("four", "FIVE", 100);
        // when
        boolean modified = set.removeAll(toRemove);
        // then
        assertFalse(modified, "removeAll should return false when nothing is removed");
        assertEquals(4, set.size(), "Set size must remain unchanged");
    }

    @Test
    void testRemoveAllEmptyCollectionDoesNotModifySet() {
        // given
        Collection<Object> empty = Collections.emptyList();
        // when
        boolean modified = set.removeAll(empty);
        // then
        assertFalse(modified, "removeAll on an empty collection must return false");
        assertEquals(4, set.size(), "Set should stay unchanged");
    }

    @Test
    void testRemoveAllHandlesNullElementsGracefully() {
        // given
        set.add(null);
        Collection<Object> toRemove = Collections.singletonList(null);
        // when
        boolean modified = set.removeAll(toRemove);
        // then
        assertTrue(modified, "removeAll should remove a null element if present");
        assertFalse(set.contains(null));
    }

    @ParameterizedTest(name = "[{index}] initial={0}, toRemove={1}, expectedModified={2}, expectedSize={3}")
    @MethodSource("basicScenariosProvider")
    void testRemoveAllBasicScenarios(Set<String> initialElements, Collection<String> toRemove, boolean expectedModified, int expectedSize) {
        // Use a set with a String generic to verify case‑insensitive behaviour where relevant
        CaseInsensitiveSet<String> localSet = new CaseInsensitiveSet<>();
        initialElements.forEach(localSet::add);
        boolean modified = localSet.removeAll(toRemove);
        assertEquals(expectedModified, modified, "removeAll returned unexpected modification flag");
        assertEquals(expectedSize, localSet.size(), "Set size after removeAll does not match expectation");
        // Ensure that elements reported as removed are indeed absent
        toRemove.forEach(elem -> assertFalse(localSet.contains(elem), "Element '" + elem + "' should have been removed"));
    }

    @Test
    void testRemoveAllCaseInsensitiveRemoval() {
        // Set contains lower‑case "alpha"
        CaseInsensitiveSet<String> ciSet = new CaseInsensitiveSet<>();
        ciSet.add("alpha");
        // Remove using upper‑case version
        boolean modified = ciSet.removeAll(Collections.singletonList("ALPHA"));
        assertTrue(modified, "removeAll should succeed irrespective of case");
        assertTrue(ciSet.isEmpty(), "Set should be empty after case‑insensitive removal");
    }

    @Test
    void testRemoveAllNullCollectionThrowsNPE() {
        assertThrows(NullPointerException.class, () -> set.removeAll(null), "Passing a null collection must throw NullPointerException");
    }

    @Test
    void testRemoveAllNullElementInCollectionThrowsNPE() {
        // The default backing map does not allow null keys – expect NPE from map.remove(null)
        set.add(null);
        Collection<Object> coll = Collections.singletonList(null);
        assertThrows(NullPointerException.class, () -> set.removeAll(coll), "Removing a null element should propagate NullPointerException");
    }

    @Test
    void testRemoveAllIteratorThrowsRuntimeException() {
        // Custom collection whose iterator() throws a RuntimeException
        Collection<Object> throwingCollection = new Collection<Object>() {

            @Override
            public int size() {
                return 1;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public boolean contains(Object o) {
                return false;
            }

            @Override
            public Iterator<Object> iterator() {
                throw new RuntimeException("Iterator failure");
            }

            @Override
            public Object[] toArray() {
                return new Object[0];
            }

            @Override
            public <T> T[] toArray(T[] a) {
                return a;
            }

            @Override
            public boolean add(Object e) {
                return false;
            }

            @Override
            public boolean remove(Object o) {
                return false;
            }

            @Override
            public boolean containsAll(Collection<?> c) {
                return false;
            }

            @Override
            public boolean addAll(Collection<? extends Object> c) {
                return false;
            }

            @Override
            public boolean removeAll(Collection<?> c) {
                return false;
            }

            @Override
            public boolean retainAll(Collection<?> c) {
                return false;
            }

            @Override
            public void clear() {
            }
        };
        assertThrows(RuntimeException.class, () -> set.removeAll(throwingCollection), "removeAll must propagate RuntimeException thrown by the iterator");
    }

    @Test
    void testRemoveAllWithIncompatibleElementTypeReturnsFalse() {
        // Set is typed to String – adding only strings
        CaseInsensitiveSet<String> stringSet = new CaseInsensitiveSet<>();
        stringSet.add("apple");
        stringSet.add("banana");
        // Collection contains an Integer; cast succeeds (unchecked) but map.remove finds nothing
        boolean modified = stringSet.removeAll(Collections.singletonList(123));
        assertFalse(modified, "removeAll should return false when no matching element is found");
        assertEquals(2, stringSet.size(), "Set size must remain unchanged");
        assertTrue(stringSet.contains("apple"));
        assertTrue(stringSet.contains("banana"));
    }

    @Test
    void testRemoveAllWithDifferentSetInstanceSharingElements() {
        CaseInsensitiveSet<String> ciSet = new CaseInsensitiveSet<>();
        ciSet.add("apple");
        ciSet.add("banana");
        Set<String> otherSet = new HashSet<>(Arrays.asList("apple", "banana"));
        boolean modified = ciSet.removeAll(otherSet);
        assertTrue(modified, "removeAll should remove all matching elements even when collection is a different Set implementation");
        assertTrue(ciSet.isEmpty(), "Set should be empty after removing all elements");
    }

    @Test
    void testRemoveAllWhenSetIsEmptyAndCollectionNonEmpty() {
        CaseInsensitiveSet<String> emptySet = new CaseInsensitiveSet<>();
        Collection<String> toRemove = Arrays.asList("foo", "bar");
        boolean modified = emptySet.removeAll(toRemove);
        assertFalse(modified, "removeAll on an empty set should return false");
        assertTrue(emptySet.isEmpty(), "Set must remain empty");
    }

    @Test
    void testRemoveAllWhenBothSetAndCollectionAreEmpty() {
        CaseInsensitiveSet<String> emptySet = new CaseInsensitiveSet<>();
        Collection<String> emptyColl = Collections.emptyList();
        boolean modified = emptySet.removeAll(emptyColl);
        assertFalse(modified, "removeAll with both set and collection empty should return false");
        assertTrue(emptySet.isEmpty(), "Set must stay empty");
    }

    @Test
    void testRemoveAllFullClear() {
        CaseInsensitiveSet<String> ciSet = new CaseInsensitiveSet<>();
        ciSet.addAll(Arrays.asList("one", "two", "three", "four"));
        boolean modified = ciSet.removeAll(Arrays.asList("one", "two", "three", "four"));
        assertTrue(modified, "removeAll should return true when it clears the set");
        assertTrue(ciSet.isEmpty(), "Set should be empty after removing all elements");
    }
}
