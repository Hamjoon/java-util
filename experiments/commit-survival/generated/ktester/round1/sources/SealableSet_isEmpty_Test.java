package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableSet_isEmpty_Test {

    /**
     * The SealableSet under test
     */
    private SealableSet<Integer> set;

    /**
     * Controls the sealed state supplied to the SealableSet
     */
    private volatile boolean sealed = false;

    /**
     * Supplier that returns the current value of {@code sealed}
     */
    private Supplier<Boolean> sealedSupplier = () -> sealed;

    /**
     * Runs before each test – creates a fresh SealableSet instance
     */
    @BeforeEach
    void setupBeforeEach() {
        // By default we use the simple constructor that creates an empty backing set.
        set = new SealableSet<>(sealedSupplier);
    }

    /**
     * Runs after each test – clears references to help GC
     */
    @AfterEach
    void teardownAfterEach() {
        set = null;
    }

    /**
     * Helper to create a SealableSet pre‑populated with the given elements
     */
    private SealableSet<Integer> createSetWithElements(Integer... elems) {
        SealableSet<Integer> s = new SealableSet<>(sealedSupplier);
        Collections.addAll(s, elems);
        return s;
    }

    private static Stream<Arguments> provideRetainAllScenarios() {
        return Stream.of(Arguments.of("retainAll with disjoint collection should empty the set", Arrays.asList(1, 2, 3), Arrays.asList(4, 5), true), Arguments.of("retainAll with subset should leave set non‑empty", Arrays.asList(1, 2, 3), Arrays.asList(2, 3, 9), false), Arguments.of("retainAll with identical collection should keep set non‑empty", Arrays.asList(7, 8), Arrays.asList(7, 8), false));
    }

    private static Stream<Arguments> provideAddAllScenarios() {
        return Stream.of(Arguments.of("addAll on empty set with non‑empty collection makes it non‑empty", Collections.emptyList(), Arrays.asList(1, 2), false), Arguments.of("addAll on non‑empty set with empty collection leaves it non‑empty", Arrays.asList(5), Collections.emptyList(), false), Arguments.of("addAll with duplicate elements does not change emptiness", Arrays.asList(9, 10), Arrays.asList(9, 10), false));
    }

    /**
     * isEmpty() must return true for a newly‑created, empty set
     */
    @Test
    void testIsEmptyOnNewSet() {
        assertTrue(set.isEmpty(), "A newly created SealableSet should be empty");
    }

    /**
     * isEmpty() must return false after elements have been added
     */
    @Test
    void testIsEmptyAfterAddingElements() {
        assertTrue(set.isEmpty(), "Pre‑condition: set should start empty");
        set.add(1);
        set.add(2);
        assertFalse(set.isEmpty(), "Set should report non‑empty after adding elements");
    }

    /**
     * isEmpty() should reflect the underlying set even when the SealableSet is sealed
     */
    @Test
    void testIsEmptyWhenSealed() {
        // Populate the set first
        set.add(10);
        set.add(20);
        assertFalse(set.isEmpty(), "Pre‑condition: set should be non‑empty before sealing");
        // Seal the set – mutating operations are now prohibited, but isEmpty must still work
        sealed = true;
        assertFalse(set.isEmpty(), "isEmpty() must still return false when the set is sealed and non‑empty");
        // Clear the underlying set via a new unsealed instance to verify true case while sealed
        // (cannot clear directly because it would throw; we create a fresh sealed set with empty collection)
        SealableSet<Integer> emptySealed = new SealableSet<>(Arrays.asList(), sealedSupplier);
        // keep sealed
        sealed = true;
        assertTrue(emptySealed.isEmpty(), "A sealed but empty SealableSet must report empty");
    }

    /**
     * Verify that the constructor which accepts an existing collection respects emptiness
     */
    @Test
    void testIsEmptyWithCollectionConstructor() {
        Collection<Integer> emptyCollection = Collections.emptyList();
        SealableSet<Integer> emptyFromCollection = new SealableSet<>(emptyCollection, sealedSupplier);
        assertTrue(emptyFromCollection.isEmpty(), "Constructor with empty collection should produce an empty set");
        Collection<Integer> nonEmpty = Arrays.asList(5, 6, 7);
        SealableSet<Integer> nonEmptyFromCollection = new SealableSet<>(nonEmpty, sealedSupplier);
        assertFalse(nonEmptyFromCollection.isEmpty(), "Constructor with non‑empty collection should produce a non‑empty set");
    }

    /**
     * Verify that the constructor which wraps an existing Set respects emptiness
     */
    @Test
    void testIsEmptyWithSetConstructor() {
        java.util.Set<Integer> backingSet = new java.util.HashSet<>();
        SealableSet<Integer> wrappedEmpty = new SealableSet<>(backingSet, sealedSupplier);
        assertTrue(wrappedEmpty.isEmpty(), "Wrapping an empty backing set should be empty");
        backingSet.add(42);
        SealableSet<Integer> wrappedNonEmpty = new SealableSet<>(backingSet, sealedSupplier);
        assertFalse(wrappedNonEmpty.isEmpty(), "Wrapping a non‑empty backing set should be non‑empty");
    }

    @Test
    void testIsEmpty_WithWrappedSetEmptyAtConstruction() {
        // The default set is already empty.
        assertTrue(set.isEmpty(), "Empty wrapped Set should report true");
    }

    @Test
    void testIsEmpty_WithWrappedSetSingleElementAtConstruction() {
        set = new SealableSet<>(Arrays.asList(99), sealedSupplier);
        assertFalse(set.isEmpty(), "Wrapped Set with one element should report false");
    }

    @Test
    void testIsEmpty_AddElementAfterConstruction() {
        set.add(7);
        assertFalse(set.isEmpty(), "After adding an element, isEmpty() must return false");
    }

    @Test
    void testIsEmpty_ClearNonEmptySet() {
        set.addAll(Arrays.asList(1, 2, 3));
        set.clear();
        assertTrue(set.isEmpty(), "After clear() on a non‑empty set, isEmpty() must return true");
    }

    @Test
    void testIsEmpty_RemoveAllElements() {
        set.addAll(Arrays.asList(4, 5, 6));
        set.removeAll(Arrays.asList(4, 5, 6));
        assertTrue(set.isEmpty(), "After removeAll() removing every element, isEmpty() must return true");
    }

    @Test
    void testIsEmpty_RemoveOnlyElement() {
        set = new SealableSet<>(Arrays.asList(10), sealedSupplier);
        assertTrue(set.remove(10), "remove(Object) should succeed");
        assertTrue(set.isEmpty(), "Set should be empty after removing its sole element");
    }

    @Test
    void testIsEmpty_RemoveOneOfTwoElements() {
        set = new SealableSet<>(Arrays.asList(1, 2), sealedSupplier);
        assertTrue(set.remove(1), "remove(Object) should succeed for existing element");
        assertFalse(set.isEmpty(), "Set should not be empty after removing only one of two elements");
    }

    @Test
    void testIsEmpty_RemoveNonExistingElement() {
        set = new SealableSet<>(Arrays.asList(3, 4), sealedSupplier);
        assertFalse(set.remove(99), "remove(Object) should return false for missing element");
        assertFalse(set.isEmpty(), "Set should remain non‑empty after attempting to remove a non‑present element");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideRetainAllScenarios")
    void testIsEmpty_RetainAllScenarios(String description, Collection<Integer> initial, Collection<Integer> retainWith, boolean expectedEmpty) {
        set = new SealableSet<>(initial, sealedSupplier);
        set.retainAll(retainWith);
        assertEquals(expectedEmpty, set.isEmpty(), description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideAddAllScenarios")
    void testIsEmpty_AddAllScenarios(String description, Collection<Integer> start, Collection<Integer> toAdd, boolean expectedEmpty) {
        set = new SealableSet<>(start, sealedSupplier);
        set.addAll(toAdd);
        assertEquals(expectedEmpty, set.isEmpty(), description);
    }

    @Test
    void testIsEmpty_IteratorRemoveSingleElement() {
        set = new SealableSet<>(Arrays.asList(42), sealedSupplier);
        Iterator<Integer> it = set.iterator();
        assertTrue(it.hasNext(), "Iterator should have an element");
        it.next();
        // remove the sole element
        it.remove();
        assertTrue(set.isEmpty(), "Set should be empty after iterator.remove() on single‑element set");
    }

    @Test
    void testIsEmpty_IteratorRemoveFromThreeElements() {
        set = new SealableSet<>(Arrays.asList(1, 2, 3), sealedSupplier);
        Iterator<Integer> it = set.iterator();
        // remove the first element
        assertTrue(it.hasNext());
        it.next();
        it.remove();
        assertFalse(set.isEmpty(), "Set should still contain elements after removing one of three");
    }

    @Test
    void testIsEmpty_AddNullElement() {
        // HashSet permits null; SealableSet should delegate the same behaviour.
        set.add(null);
        assertFalse(set.isEmpty(), "Adding null should make the set non‑empty");
    }

    @Test
    void testIsEmpty_AddDuplicateElement() {
        set.add(100);
        // duplicate add
        boolean changed = set.add(100);
        assertFalse(changed, "Adding a duplicate should return false");
        assertFalse(set.isEmpty(), "Set remains non‑empty after duplicate add");
    }

    @Test
    void testIsEmpty_NullUnderlyingSet_NullPointerException() {
        SealableSet<Integer> nullBacking = new SealableSet<>(null, sealedSupplier);
        assertThrows(NullPointerException.class, nullBacking::isEmpty, "Calling isEmpty() on a SealableSet constructed with a null backing set should throw NPE");
    }

    @Test
    void testIsEmpty_UnderlyingSetThrowsRuntimeException() {
        // Custom Set implementation that throws on isEmpty()
        Set<Integer> explodingSet = new Set<Integer>() {

            @Override
            public int size() {
                return 0;
            }

            @Override
            public boolean isEmpty() {
                throw new RuntimeException("boom");
            }

            @Override
            public boolean contains(Object o) {
                return false;
            }

            @Override
            public Iterator<Integer> iterator() {
                return Collections.emptyIterator();
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
            public boolean add(Integer integer) {
                return true;
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
            public boolean addAll(Collection<? extends Integer> c) {
                return false;
            }

            @Override
            public boolean retainAll(Collection<?> c) {
                return false;
            }

            @Override
            public boolean removeAll(Collection<?> c) {
                return false;
            }

            @Override
            public void clear() {
            }
        };
        SealableSet<Integer> setWithExplodingBacking = new SealableSet<>(explodingSet, sealedSupplier);
        RuntimeException ex = assertThrows(RuntimeException.class, setWithExplodingBacking::isEmpty, "RuntimeException from underlying set should propagate");
        assertEquals("boom", ex.getMessage());
    }
}
