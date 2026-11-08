package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.concurrent.ConcurrentModificationException;

@ExtendWith(MockitoExtension.class)
class CompactMap_values_Test {

    private CompactMap<String, Integer> map;

    /**
     * Helper subclass to force the copy‑iterator path.
     */
    private static class CopyIteratorMap extends CompactMap<String, Integer> {

        @Override
        protected boolean useCopyIterator() {
            return true;
        }
    }

    /**
     * Helper subclass to force the compact‑iterator path.
     */
    private static class CompactIteratorMap extends CompactMap<String, Integer> {

        @Override
        protected boolean useCopyIterator() {
            return false;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh CompactMap for each test and populate it with a few entries
        map = new CompactMap<>();
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the map to help the GC and to guarantee isolation
        if (map != null) {
            map.clear();
        }
    }

    @Test
    void valuesCollectionReflectsMapSize() {
        Collection<Integer> values = map.values();
        // The collection size must be the same as the map size
        assertEquals(map.size(), values.size(), "values().size() should be equal to map.size()");
        // The collection must contain exactly the values we inserted
        assertTrue(values.contains(1));
        assertTrue(values.contains(2));
        assertTrue(values.contains(3));
        assertEquals(3, values.stream().distinct().count(), "values() should contain three distinct entries");
    }

    @Test
    void iteratorReturnsAllValues() {
        Collection<Integer> values = map.values();
        Iterator<Integer> it = values.iterator();
        // Collect values from iterator
        java.util.Set<Integer> iterated = new java.util.HashSet<>();
        while (it.hasNext()) {
            iterated.add(it.next());
        }
        // The iterator must have visited all map values
        assertEquals(3, iterated.size(), "Iterator should have visited three values");
        assertTrue(iterated.containsAll(java.util.List.of(1, 2, 3)), "Iterator should contain the map's values");
    }

    @Test
    void clearOnValuesCollectionClearsMap() {
        Collection<Integer> values = map.values();
        // Clear through the collection view
        values.clear();
        // The underlying map must now be empty
        assertTrue(map.isEmpty(), "Map should be empty after clearing values collection");
        assertEquals(0, values.size(), "Values collection size should be zero after clear");
    }

    @Test
    void valuesCollectionIsLiveView() {
        Collection<Integer> values = map.values();
        // Modify the map directly
        map.put("four", 4);
        map.remove("two");
        // The collection view must reflect those changes
        assertTrue(values.contains(4), "Values view should contain newly added value");
        assertFalse(values.contains(2), "Values view should not contain removed value");
        assertEquals(map.size(), values.size(), "Values view size must stay in sync with map size");
    }

    @Test
    void iteratorSelectionBasedOnUseCopyIterator_true() {
        CompactMap<String, Integer> copyMap = new CopyIteratorMap();
        copyMap.put("a", 1);
        copyMap.put("b", 2);
        Iterator<Integer> it = copyMap.values().iterator();
        // The iterator class name should contain "CopyValueIterator"
        assertTrue(it.getClass().getSimpleName().contains("CopyValueIterator"), "Iterator should be a CopyValueIterator when useCopyIterator() returns true");
    }

    @Test
    void iteratorSelectionBasedOnUseCopyIterator_false() {
        CompactMap<String, Integer> compactMap = new CompactIteratorMap();
        compactMap.put("a", 1);
        compactMap.put("b", 2);
        Iterator<Integer> it = compactMap.values().iterator();
        // The iterator class name should contain "CompactValueIterator"
        assertTrue(it.getClass().getSimpleName().contains("CompactValueIterator"), "Iterator should be a CompactValueIterator when useCopyIterator() returns false");
    }

    @Test
    void valuesCollectionSize_EmptyMap() {
        CompactMap<String, Integer> empty = new CompactMap<>();
        assertEquals(0, empty.values().size(), "A newly created CompactMap is empty; values().size() must be 0");
    }

    @Test
    void valuesCollectionSize_AfterRemoval() {
        map.remove("two");
        assertEquals(2, map.values().size(), "After removing one entry, values().size() must reflect the new size (2)");
    }

    @Test
    void iteratorOnEmptyMap_hasNextFalse() {
        CompactMap<String, Integer> empty = new CompactMap<>();
        Iterator<Integer> it = empty.values().iterator();
        assertFalse(it.hasNext(), "Iterator on empty map should report hasNext() == false");
    }

    @Test
    void iteratorOnEmptyMap_nextThrows() {
        CompactMap<String, Integer> empty = new CompactMap<>();
        Iterator<Integer> it = empty.values().iterator();
        assertThrows(NoSuchElementException.class, it::next, "Calling next() on iterator of empty values collection must throw NoSuchElementException");
    }

    @Test
    void iteratorTraversalOrder() {
        CompactMap<String, Integer> ordered = new CompactMap<>();
        ordered.put("a", 1);
        ordered.put("b", 2);
        ordered.put("c", 3);
        Iterator<Integer> it = ordered.values().iterator();
        List<Integer> result = new ArrayList<>();
        while (it.hasNext()) {
            result.add(it.next());
        }
        // CompactMap does not guarantee insertion order, but the collection must contain all values exactly once.
        assertEquals(Set.of(1, 2, 3), new HashSet<>(result), "Iterator must return exactly the set of values without duplication");
    }

    @Test
    void iteratorRemoveUnsupported() {
        Iterator<Integer> it = map.values().iterator();
        assertThrows(UnsupportedOperationException.class, it::remove, "Iterator.remove() must throw UnsupportedOperationException");
    }

    @Test
    void concurrentModificationDetection_realIterator() {
        CompactMap<String, Integer> compactMap = new CompactIteratorMap();
        compactMap.put("x", 10);
        compactMap.put("y", 20);
        Iterator<Integer> it = compactMap.values().iterator();
        // Modify the map after iterator creation
        compactMap.put("z", 30);
        // hasNext should not throw CME
        assertDoesNotThrow(it::hasNext, "hasNext() may return true/false but must not throw CME before next()");
        // The next call must detect the concurrent modification
        assertThrows(ConcurrentModificationException.class, it::next, "Iterator.next() must throw ConcurrentModificationException after map modification");
    }

    @Test
    void concurrentModificationTolerance_copyIterator() {
        CompactMap<String, Integer> copyMap = new CopyIteratorMap();
        copyMap.put("x", 10);
        copyMap.put("y", 20);
        Iterator<Integer> it = copyMap.values().iterator();
        // Modify the underlying map after iterator creation
        copyMap.put("z", 30);
        copyMap.remove("x");
        // The iterator should continue without CME and should not see the newly added value (30)
        List<Integer> traversed = new ArrayList<>();
        while (it.hasNext()) {
            traversed.add(it.next());
        }
        assertFalse(traversed.contains(30), "Copy iterator must not see values added after its creation");
        // It should still contain the snapshot values (10 and 20)
        assertTrue(traversed.containsAll(List.of(10, 20)), "Copy iterator must iterate over the snapshot values");
    }

    @Test
    void valuesCollectionAddUnsupported() {
        Collection<Integer> values = map.values();
        assertThrows(UnsupportedOperationException.class, () -> values.add(99), "values().add() must throw UnsupportedOperationException");
    }

    @Test
    void valuesCollectionAddAllUnsupported() {
        Collection<Integer> values = map.values();
        assertThrows(UnsupportedOperationException.class, () -> values.addAll(List.of(4, 5)), "values().addAll() must throw UnsupportedOperationException");
    }

    @Test
    void valuesCollectionRemoveUnsupported() {
        Collection<Integer> values = map.values();
        assertThrows(UnsupportedOperationException.class, () -> values.remove(1), "values().remove(Object) must throw UnsupportedOperationException");
    }
}
