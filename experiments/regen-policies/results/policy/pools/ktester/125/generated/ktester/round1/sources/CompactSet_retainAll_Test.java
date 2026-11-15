package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompactSet_retainAll_Test {

    /**
     * A CompactSet used in the tests.
     */
    private CompactSet<String> set;

    /**
     * A collection used as the argument for {@code retainAll}.
     */
    private Collection<String> retainCollection;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh CompactSet for every test to guarantee isolation.
        set = new CompactSet<>();
        // Populate the set with a known deterministic order – insertion order is the default.
        set.add("alpha");
        set.add("beta");
        set.add("gamma");
        set.add("delta");
        // Initialise the collection that will be passed to retainAll; each test can modify it.
        retainCollection = new ArrayList<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help the garbage collector; not strictly necessary but keeps the pattern clear.
        set.clear();
        retainCollection.clear();
    }

    private static Stream<Arguments> retainAllFunctionalProvider() {
        return Stream.of(// proper subset → element 1 removed, change = true
        Arguments.of(Arrays.asList(1, 2, 3), Arrays.asList(2, 3), true, Arrays.asList(2, 3)), // exact match → no change
        Arguments.of(Arrays.asList(1, 2), Arrays.asList(1, 2), false, Arrays.asList(1, 2)), // empty collection → clears set, change = true
        Arguments.of(Arrays.asList(1, 2, 3), Collections.emptyList(), true, Collections.emptyList()), // no common elements → clears set, change = true
        Arguments.of(Arrays.asList(1, 2, 3), Arrays.asList(4, 5), true, Collections.emptyList()), // collection with duplicates → behaves like subset, change = true
        Arguments.of(Arrays.asList(1, 2, 3), Arrays.asList(2, 2, 3), true, Arrays.asList(2, 3)), // retain on an initially empty set → no change, false
        Arguments.of(Collections.emptyList(), Arrays.asList(1), false, Collections.emptyList()));
    }

    /**
     * Verify that {@code retainAll} removes elements that are not present in the supplied collection
     * and returns {@code true} when the set is modified.
     */
    @Test
    void retainAllRemovesMissingElements() {
        // Keep only "beta" and "gamma"
        retainCollection.addAll(Arrays.asList("beta", "gamma"));
        boolean changed = set.retainAll(retainCollection);
        assertTrue(changed, "retainAll should report that the set was modified");
        assertEquals(2, set.size(), "Set should contain exactly two elements after retainAll");
        assertTrue(set.containsAll(retainCollection), "Set should contain the retained elements");
        assertFalse(set.contains("alpha"), "Element not in collection should have been removed");
        assertFalse(set.contains("delta"), "Element not in collection should have been removed");
    }

    /**
     * When the supplied collection contains all elements of the set, {@code retainAll}
     * must leave the set unchanged and return {@code false}.
     */
    @Test
    void retainAllNoChangeWhenAllElementsPresent() {
        retainCollection.addAll(Arrays.asList("alpha", "beta", "gamma", "delta"));
        boolean changed = set.retainAll(retainCollection);
        assertFalse(changed, "retainAll should report no modification when all elements are retained");
        assertEquals(4, set.size(), "Set size should remain unchanged");
        assertTrue(set.containsAll(retainCollection), "All original elements should still be present");
    }

    /**
     * Passing an empty collection should clear the set and return {@code true}
     * (unless the set was already empty).
     */
    @Test
    void retainAllWithEmptyCollectionClearsSet() {
        // retainCollection is already empty
        boolean changed = set.retainAll(retainCollection);
        assertTrue(changed, "retainAll should report modification when clearing a non‑empty set");
        assertTrue(set.isEmpty(), "Set should be empty after retaining against an empty collection");
    }

    /**
     * If the set is already empty, {@code retainAll} must return {@code false}
     * regardless of the collection supplied.
     */
    @Test
    void retainAllOnEmptySetReturnsFalse() {
        // make the set empty
        set.clear();
        retainCollection.addAll(Arrays.asList("any", "values"));
        boolean changed = set.retainAll(retainCollection);
        assertFalse(changed, "retainAll on an empty set should report no modification");
        assertTrue(set.isEmpty(), "Set should remain empty");
    }

    /**
     * The method should correctly handle {@code null} elements inside the collection.
     * CompactSet does not allow {@code null} values, so such elements are simply ignored
     * by the underlying {@code retainAll} implementation.
     */
    @Test
    void retainAllIgnoresNullElementsInCollection() {
        retainCollection.addAll(Arrays.asList("beta", null));
        boolean changed = set.retainAll(retainCollection);
        assertTrue(changed, "retainAll should report modification when elements are removed");
        assertEquals(1, set.size(), "Only the non‑null retained element should remain");
        assertTrue(set.contains("beta"));
        assertFalse(set.contains("alpha"));
        assertFalse(set.contains("gamma"));
        assertFalse(set.contains("delta"));
    }

    /**
     * Verify that the method delegates to the underlying map's keySet correctly
     * by using a spy on the internal map. This ensures the contract of {@code retainAll}
     * is exercised without accessing private fields directly.
     */
    @Test
    void retainAllDelegatesToMapKeySet() {
        // Build a CompactSet with a custom map that we can spy on.
        CompactMap<String, Object> backingMap = CompactMap.<String, Object>builder().compactSize(// any valid size >=2
        4).caseSensitive(true).build();
        // Wrap the map's keySet with a spy to capture the retainAll call.
        Set<String> spyKeySet = spy(backingMap.keySet());
        // Replace the map inside a new CompactSet via the protected constructor.
        CompactSet<String> spySet = new CompactSet<>(new CompactMap<String, Object>(backingMap) {

            @Override
            public Set<String> keySet() {
                return spyKeySet;
            }
        });
        // Populate the set via the public API.
        spySet.addAll(Arrays.asList("one", "two", "three"));
        // Prepare collection to retain.
        Collection<String> toRetain = Collections.singleton("two");
        // Execute retainAll.
        boolean changed = spySet.retainAll(toRetain);
        // Verify delegation.
        verify(spyKeySet, times(1)).retainAll(toRetain);
        assertTrue(changed);
        assertEquals(1, spySet.size());
        assertTrue(spySet.contains("two"));
    }

    /**
     * Parameterised functional tests for {@code retainAll} using an {@code Integer} set.
     *
     * <p>Each argument consists of:
     * <ul>
     *   <li>initial set contents</li>
     *   <li>collection supplied to {@code retainAll}</li>
     *   <li>expected boolean result (true if the set changes)</li>
     *   <li>expected final set contents after the call</li>
     * </ul>
     * </p>
     */
    @ParameterizedTest(name = "{index} ⇒ set={0}, retain={1}, changed={2}, result={3}")
    @MethodSource("retainAllFunctionalProvider")
    void retainAllFunctionalCases(List<Integer> initialSet, Collection<Integer> retainArg, boolean expectedChanged, List<Integer> expectedResult) {
        CompactSet<Integer> intSet = new CompactSet<>();
        intSet.addAll(initialSet);
        boolean changed = intSet.retainAll(retainArg);
        assertEquals(expectedChanged, changed, "retainAll should return " + expectedChanged + " for collection " + retainArg);
        assertEquals(expectedResult.size(), intSet.size(), "Resulting set size mismatch");
        assertTrue(intSet.containsAll(expectedResult), "Resulting set should contain exactly " + expectedResult);
    }

    /**
     * {@code retainAll(null)} must throw {@link NullPointerException}
     * according to the {@link Set} contract.
     */
    @Test
    void retainAllNullCollectionThrowsNPE() {
        assertThrows(NullPointerException.class, () -> set.retainAll(null), "Calling retainAll with a null collection should throw NullPointerException");
    }

    /**
     * Retaining the set with itself must not modify the set and must return {@code false}.
     */
    @Test
    void retainAllWithSelfReferenceReturnsFalse() {
        // The set already contains ["alpha","beta","gamma","delta"]
        boolean changed = set.retainAll(set);
        assertFalse(changed, "retainAll(self) should report no modification");
        assertEquals(4, set.size(), "Set size must remain unchanged");
        assertTrue(set.containsAll(Arrays.asList("alpha", "beta", "gamma", "delta")));
    }

    /**
     * Verify that a type mismatch between the set element type and the collection element type
     * results in a {@link ClassCastException} when the underlying {@code keySet.retainAll}
     * attempts to compare elements.
     */
    @Test
    void retainAllTypeMismatchThrowsClassCastException() {
        CompactSet<Integer> intSet = new CompactSet<>();
        intSet.addAll(Arrays.asList(1, 2, 3));
        // Collection of Strings – incompatible with Integer set.
        Collection<String> wrongType = Arrays.asList("a", "b");
        assertThrows(ClassCastException.class, () -> intSet.retainAll(wrongType), "retainAll with incompatible element types should throw ClassCastException");
    }
}
