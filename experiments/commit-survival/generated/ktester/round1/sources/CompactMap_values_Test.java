package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class CompactMap_values_Test {

    // used for normal‑iterator tests
    private CompactMap<String, Integer> map;

    // used for copy‑iterator tests
    private CompactMap<String, Integer> copyMap;

    /**
     * Simple subclass that forces the normal (non‑copy) iterator.
     */
    private static class NormalIterMap<K, V> extends CompactMap<K, V> {

        @Override
        protected boolean useCopyIterator() {
            return false;
        }
    }

    /**
     * Simple subclass that forces the copy‑snapshot iterator.
     */
    private static class CopyIterMap<K, V> extends CompactMap<K, V> {

        @Override
        protected boolean useCopyIterator() {
            return true;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        map = new NormalIterMap<>();
        copyMap = new CopyIterMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC; not strictly required.
        map = null;
        copyMap = null;
    }

    /**
     * Verify that an empty CompactMap reports an empty values collection.
     */
    @Test
    void valuesEmptyMap() {
        Collection<Integer> values = map.values();
        assertEquals(0, values.size(), "Empty map should have size 0");
        assertFalse(values.iterator().hasNext(), "Iterator should have no elements");
        // clear() on an empty collection must keep the map empty and not throw.
        assertDoesNotThrow(values::clear, "clear() on empty values collection should not throw");
        assertTrue(map.isEmpty(), "Map should remain empty after clear()");
    }

    /**
     * Verify that values().clear() empties the map regardless of iterator type.
     */
    @Test
    void valuesClearEmptiesMap() {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assertEquals(3, map.size());
        Collection<Integer> values = map.values();
        values.clear();
        assertTrue(map.isEmpty(), "Map must be empty after values().clear()");
        assertEquals(0, values.size(), "Values collection must report size 0 after clear()");
    }

    /**
     * Normal iterator should iterate over all current values.
     */
    @Test
    void valuesIteratorNormalIteratesAll() {
        map.put("x", 10);
        map.put("y", 20);
        map.put("z", 30);
        Collection<Integer> values = map.values();
        Iterator<Integer> it = values.iterator();
        // Collect values from iterator
        java.util.Set<Integer> seen = new java.util.HashSet<>();
        while (it.hasNext()) {
            seen.add(it.next());
        }
        assertEquals(3, seen.size(), "Iterator must return exactly three values");
        assertTrue(seen.containsAll(java.util.List.of(10, 20, 30)));
    }

    /**
     * The copy iterator returns a snapshot of the values at the moment the iterator
     * is created.  Adding a new entry after obtaining the iterator must not be visible
     * through that iterator.
     */
    @Test
    void valuesIteratorCopyIsSnapshot() {
        copyMap.put("k1", 100);
        copyMap.put("k2", 200);
        Collection<Integer> values = copyMap.values();
        Iterator<Integer> snapshotIt = values.iterator();
        // Modify the map after the iterator has been created
        copyMap.put("k3", 300);
        // The snapshot iterator should only see the original two values
        java.util.Set<Integer> snapshotValues = new java.util.HashSet<>();
        while (snapshotIt.hasNext()) {
            snapshotValues.add(snapshotIt.next());
        }
        assertEquals(2, snapshotValues.size(), "Copy iterator must reflect the state of the map at iterator creation time");
        assertTrue(snapshotValues.containsAll(java.util.List.of(100, 200)), "Snapshot must contain the original values only");
        // Ensure the map itself now has three entries
        assertEquals(3, copyMap.size(), "Map should contain three entries after the put");
    }

    /**
     * The values collection reports the correct size after modifications.
     */
    @Test
    void valuesSizeReflectsMapSize() {
        map.put("one", 1);
        assertEquals(1, map.values().size());
        map.put("two", 2);
        assertEquals(2, map.values().size());
        map.remove("one");
        assertEquals(1, map.values().size());
        map.clear();
        assertEquals(0, map.values().size());
    }

    /**
     * Verify that the copy‑iterator is used and iterates in insertion order.
     */
    @Test
    void valuesIteratorWhenCopyIteratorUsed_typeAndOrder() {
        copyMap.put("a", 1);
        copyMap.put("b", 2);
        copyMap.put("c", 3);
        Collection<Integer> values = copyMap.values();
        Iterator<Integer> it = values.iterator();
        // Check concrete iterator class name via reflection
        String iteratorClassName = it.getClass().getSimpleName();
        assertTrue(iteratorClassName.contains("CopyValueIterator"), "Iterator should be an instance of CopyValueIterator, got: " + iteratorClassName);
        // Verify iteration order matches insertion order
        List<Integer> iterated = new ArrayList<>();
        while (it.hasNext()) {
            iterated.add(it.next());
        }
        assertEquals(List.of(1, 2, 3), iterated, "Copy iterator must return values in insertion order");
    }

    /**
     * Copy iterator must be safe against concurrent modifications.
     */
    @Test
    void valuesIteratorWhenCopyIteratorUsed_concurrentModificationSafety() {
        copyMap.put("x", 10);
        copyMap.put("y", 20);
        Collection<Integer> values = copyMap.values();
        Iterator<Integer> it = values.iterator();
        // Structural modification after iterator creation
        copyMap.put("z", 30);
        // No CME should be thrown; iterator should still see the original snapshot
        List<Integer> snapshot = new ArrayList<>();
        while (it.hasNext()) {
            snapshot.add(it.next());
        }
        assertEquals(List.of(10, 20), snapshot, "Snapshot iterator must not see modifications performed after its creation");
    }

    /**
     * Verify that the compact (non‑copy) iterator is used and reflects current map state.
     */
    @Test
    void valuesIteratorWhenCompactIteratorUsed_typeAndContent() {
        map.put("p", 5);
        map.put("q", 6);
        map.put("r", 7);
        Collection<Integer> values = map.values();
        Iterator<Integer> it = values.iterator();
        String iteratorClassName = it.getClass().getSimpleName();
        assertTrue(iteratorClassName.contains("CompactValueIterator"), "Iterator should be an instance of CompactValueIterator, got: " + iteratorClassName);
        // Collect values – order is not guaranteed for the compact iterator,
        // but all values must be present.
        Set<Integer> seen = new HashSet<>();
        while (it.hasNext()) {
            seen.add(it.next());
        }
        assertEquals(Set.of(5, 6, 7), seen, "Compact iterator must return all current values");
    }

    /**
     * Compact iterator must detect concurrent modifications and throw CME.
     */
    @Test
    void valuesIteratorWhenCompactIteratorUsed_concurrentModificationThrows() {
        map.put("k1", 1);
        map.put("k2", 2);
        Iterator<Integer> it = map.values().iterator();
        // Modify the map structurally
        map.remove("k1");
        // The next call to next() should throw ConcurrentModificationException
        assertThrows(ConcurrentModificationException.class, it::next, "Compact iterator must throw CME after structural modification");
    }

    /**
     * iterator.remove() deletes the underlying entry and updates sizes.
     */
    @Test
    void valuesIteratorRemove_successfulRemoval() {
        map.put("a", 10);
        map.put("b", 20);
        map.put("c", 30);
        Iterator<Integer> it = map.values().iterator();
        assertTrue(it.hasNext());
        // should be 10 (or any value, we just remove the entry we got)
        Integer first = it.next();
        // Remove via iterator
        it.remove();
        // The map must no longer contain the removed value's key.
        assertEquals(2, map.size(), "Map size must decrease after iterator.remove()");
        assertEquals(2, map.values().size(), "Values collection size must reflect removal");
        // Verify that the removed value is indeed gone
        assertFalse(map.containsValue(first), "Removed value must no longer be present");
    }

    /**
     * iterator.remove() called twice without next() must throw IllegalStateException.
     */
    @Test
    void valuesIteratorRemove_illegalStateOnDoubleRemove() {
        map.put("x", 1);
        map.put("y", 2);
        Iterator<Integer> it = map.values().iterator();
        assertTrue(it.hasNext());
        // first element
        it.next();
        // first removal OK
        it.remove();
        // Second remove without another next()
        assertThrows(IllegalStateException.class, it::remove, "Second remove without next() must throw IllegalStateException");
    }

    /**
     * values().remove(value) removes the first matching entry and returns true.
     */
    @Test
    void valuesCollectionRemove_existingValue() {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        Collection<Integer> values = map.values();
        assertTrue(values.remove(2), "remove(existingValue) must return true");
        assertEquals(2, map.size(), "Map size must decrease after removal");
        assertFalse(map.containsValue(2), "Removed value must no longer be present");
        assertFalse(values.contains(2), "Values collection must no longer contain the removed value");
    }

    /**
     * values().remove(value) returns false when the value is not present.
     */
    @Test
    void valuesCollectionRemove_nonExistingValue() {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        Collection<Integer> values = map.values();
        assertFalse(values.remove(99), "remove(nonExistingValue) must return false");
        assertEquals(3, map.size(), "Map size must stay unchanged");
    }

    /**
     * values().add(...) is unsupported.
     */
    @Test
    void valuesAddUnsupported_addThrows() {
        map.put("k", 1);
        Collection<Integer> values = map.values();
        assertThrows(UnsupportedOperationException.class, () -> values.add(42), "add() on values collection must throw UnsupportedOperationException");
    }

    /**
     * values().addAll(...) is unsupported.
     */
    @Test
    void valuesAddUnsupported_addAllThrows() {
        map.put("k", 1);
        Collection<Integer> values = map.values();
        List<Integer> toAdd = List.of(2, 3, 4);
        assertThrows(UnsupportedOperationException.class, () -> values.addAll(toAdd), "addAll() on values collection must throw UnsupportedOperationException");
    }

    /**
     * values().contains(...) works for present, absent and null values.
     */
    @Test
    void valuesContains_variousScenarios() {
        map.put("x", "alpha");
        map.put("y", "beta");
        assertTrue(map.values().contains("beta"), "Existing value must be reported as contained");
        assertFalse(map.values().contains("gamma"), "Non‑existing value must not be reported as contained");
        // null handling
        map.put("z", null);
        assertTrue(map.values().contains(null), "Collection must contain null after a null entry is added");
        map.remove("z");
        assertFalse(map.values().contains(null), "Collection must not contain null after removal");
    }

    /**
     * toArray on a copy‑iterator map preserves insertion order.
     */
    @Test
    void valuesToArrayOrderCopy_snapshotArray() {
        copyMap.put("1", "A");
        copyMap.put("2", "B");
        copyMap.put("3", "C");
        Object[] array = copyMap.values().toArray();
        assertArrayEquals(new Object[] { "A", "B", "C" }, array, "toArray() must return values in insertion order for copy iterator");
        String[] typed = copyMap.values().toArray(new String[0]);
        assertArrayEquals(new String[] { "A", "B", "C" }, typed, "toArray(T[]) must preserve insertion order for copy iterator");
    }

    /**
     * iterator.remove() before next() throws IllegalStateException.
     */
    @Test
    void valuesIteratorIllegalState_removeBeforeNext() {
        map.put("a", 1);
        Iterator<Integer> it = map.values().iterator();
        assertThrows(IllegalStateException.class, it::remove, "Calling remove() before next() must throw IllegalStateException");
    }

    /**
     * iterator.remove() after iterator is exhausted throws IllegalStateException.
     */
    @Test
    void valuesIteratorIllegalState_removeAfterExhausted() {
        map.put("a", 1);
        map.put("b", 2);
        Iterator<Integer> it = map.values().iterator();
        while (it.hasNext()) {
            it.next();
        }
        // iterator is now exhausted
        assertThrows(IllegalStateException.class, it::remove, "Calling remove() after iterator is exhausted must throw IllegalStateException");
    }

    /**
     * After map.clear(), iterator has no elements and next() throws NoSuchElementException.
     */
    @Test
    void valuesAfterMapClear_iteratorBehavior() {
        map.put("a", 1);
        map.put("b", 2);
        map.clear();
        Iterator<Integer> it = map.values().iterator();
        assertFalse(it.hasNext(), "Iterator must report no elements after map.clear()");
        assertThrows(NoSuchElementException.class, it::next, "Calling next() on an empty iterator must throw NoSuchElementException");
        assertEquals(0, map.values().size(), "Values collection size must be 0 after clear");
    }

    /**
     * values().clear() on an already empty map is a no‑op and does not throw.
     */
    @Test
    void valuesAfterMapClear_clearOnEmptyDoesNotThrow() {
        assertTrue(map.isEmpty(), "Map should start empty");
        assertDoesNotThrow(() -> map.values().clear(), "Calling clear() on values collection of an empty map must not throw");
        assertTrue(map.isEmpty(), "Map must remain empty after clear()");
    }

    /**
     * values().containsAll(...) returns true only when all elements are present.
     */
    @Test
    void valuesContainsAll_variousScenarios() {
        map.put("k1", 10);
        map.put("k2", 20);
        map.put("k3", 30);
        List<Integer> subset = List.of(10, 30);
        assertTrue(map.values().containsAll(subset), "containsAll should return true when all elements are present");
        List<Integer> superset = List.of(10, 99);
        assertFalse(map.values().containsAll(superset), "containsAll should return false when any element is missing");
    }
}
