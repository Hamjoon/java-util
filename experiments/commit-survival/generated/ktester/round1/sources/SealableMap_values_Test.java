package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Iterator;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class SealableMap_values_Test {

    private SealableMap<String, Integer> map;

    private volatile boolean sealedState = false;

    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    @BeforeEach
    void setupBeforeEach() {
        // Reset the sealed flag before each test.
        sealedState = false;
        // Create a SealableMap that uses the mutable supplier.
        map = new SealableMap<>(sealedSupplier);
        // Populate with deterministic data, including a null entry.
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        map.put(null, null);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references – helps the GC and guarantees isolation.
        map = null;
    }

    @Test
    void valuesReflectCurrentMapContentsWhenUnsealed() {
        Collection<Integer> values = map.values();
        // The collection must contain exactly the values we inserted.
        assertNotNull(values, "values() must not return null");
        assertEquals(4, values.size(), "Unexpected number of values");
        assertTrue(values.contains(1));
        assertTrue(values.contains(2));
        assertTrue(values.contains(3));
        assertTrue(values.contains(null));
    }

    @Test
    void valuesUpdateWhenUnderlyingMapChanges() {
        Collection<Integer> values = map.values();
        // Modify the underlying map.
        map.put("four", 4);
        map.remove("two");
        // replace null value
        map.put(null, 99);
        // The view should reflect those changes.
        assertEquals(4, values.size());
        assertTrue(values.contains(1));
        assertTrue(values.contains(3));
        assertTrue(values.contains(4));
        assertTrue(values.contains(99));
        assertFalse(values.contains(2));
        assertFalse(values.contains(null));
    }

    @Test
    void modificationOfValuesCollectionIsForbiddenWhenSealed() {
        Collection<Integer> values = map.values();
        // Seal the map – any mutating operation on the view must fail.
        sealedState = true;
        assertThrows(UnsupportedOperationException.class, () -> values.add(5), "Adding to a sealed values view should throw");
        assertThrows(UnsupportedOperationException.class, () -> values.remove(3), "Removing from a sealed values view should throw");
        assertThrows(UnsupportedOperationException.class, values::clear, "Clearing a sealed values view should throw");
    }

    @Test
    void valuesCollectionAllowsModificationWhenUnsealed() {
        Collection<Integer> values = map.values();
        // Normal (unsealed) mutation should succeed.
        assertTrue(values.add(5), "Adding a new value should succeed");
        assertTrue(values.contains(5));
        assertEquals(5, map.size(), "Map size should increase after adding via values view");
        assertTrue(values.remove(2), "Removing an existing value should succeed");
        assertFalse(values.contains(2));
        assertEquals(4, map.size(), "Map size should decrease after removal via values view");
    }

    @Test
    void valuesCollectionHandlesNullsCorrectly() {
        Collection<Integer> values = map.values();
        // The collection already contains a null entry (from the null key).
        assertTrue(values.contains(null), "Values view must contain null when map has a null value");
        // Replace the null value with a non‑null value.
        map.put(null, 42);
        assertFalse(values.contains(null), "Null should no longer be present after replacement");
        assertTrue(values.contains(42), "New value should be reflected in the view");
    }

    @Test
    void valuesIteratorRespectsSealState() {
        Collection<Integer> values = map.values();
        // Obtain an iterator while the map is unsealed – iterator.remove() should work.
        var iterator = values.iterator();
        assertTrue(iterator.hasNext());
        iterator.next();
        // should not throw
        iterator.remove();
        // Seal the map and verify that iterator.remove() now fails.
        sealedState = true;
        var sealedIterator = values.iterator();
        assertTrue(sealedIterator.hasNext());
        sealedIterator.next();
        assertThrows(UnsupportedOperationException.class, sealedIterator::remove, "Iterator.remove() on a sealed values view must throw");
    }

    @Test
    void valuesStreamOperatesOnCurrentSnapshot() {
        Collection<Integer> values = map.values();
        // Stream should see the current snapshot.
        long count = values.stream().filter(v -> v != null).count();
        assertEquals(3L, count, "There are three non‑null values initially");
        // Change the map after obtaining the collection.
        map.put("four", 4);
        map.remove("one");
        // The stream operates on the updated collection.
        long newCount = values.stream().filter(v -> v != null).count();
        assertEquals(3L, newCount, "After modifications, there should still be three non‑null values");
    }

    /* -------------------------------------------------------------
     * 1. Basic functionality – the returned collection is a copy.
     * ------------------------------------------------------------- */
    @Test
    void valuesReturnsSealableListCopyAndIsIndependentFromMap() {
        // Obtain the values collection.
        Collection<Integer> values = map.values();
        // The collection must contain exactly the values we inserted (including null).
        assertNotNull(values, "values() must not return null");
        assertEquals(4, values.size(), "Unexpected number of values");
        assertTrue(values.contains(1));
        assertTrue(values.contains(2));
        assertTrue(values.contains(3));
        assertTrue(values.contains(null));
        // Modify the underlying map after the collection has been obtained.
        map.put("four", 4);
        map.remove("two");
        // replace null value
        map.put(null, 99);
        // The previously obtained collection must stay unchanged (defensive copy).
        assertEquals(4, values.size(), "Copy should not reflect later map modifications");
        assertTrue(values.contains(1));
        assertTrue(values.contains(2));
        assertTrue(values.contains(3));
        assertTrue(values.contains(null));
        assertFalse(values.contains(4));
        assertFalse(values.contains(99));
    }

    @Test
    void valuesOnEmptyMapReturnsEmptySealableList() {
        // Create a fresh empty map.
        SealableMap<String, Integer> emptyMap = new SealableMap<>(sealedSupplier);
        Collection<Integer> emptyValues = emptyMap.values();
        assertNotNull(emptyValues, "values() must not return null for an empty map");
        assertTrue(emptyValues.isEmpty(), "Expected an empty collection for an empty map");
    }

    /* -------------------------------------------------------------
     * 2. Sealed behavior – mutating operations must fail with
     *    IllegalStateException (as defined by the JSON spec).
     * ------------------------------------------------------------- */
    @Test
    void sealedValuesAddThrowsIllegalStateException() {
        // Obtain the collection while the map is unsealed.
        Collection<Integer> values = map.values();
        // Seal the map.
        sealedState = true;
        assertThrows(IllegalStateException.class, () -> values.add(5), "Adding to a sealed values view should throw IllegalStateException");
    }

    @Test
    void sealedValuesClearThrowsIllegalStateException() {
        Collection<Integer> values = map.values();
        sealedState = true;
        assertThrows(IllegalStateException.class, values::clear, "Clearing a sealed values view should throw IllegalStateException");
    }

    @Test
    void sealedValuesRemoveNullThrowsIllegalStateException() {
        // Ensure the map contains a null value.
        assertTrue(map.values().contains(null));
        // Seal the map.
        sealedState = true;
        assertThrows(IllegalStateException.class, () -> map.values().remove(null), "Removing a null element from a sealed values view must throw IllegalStateException");
    }

    /* -------------------------------------------------------------
     * 3. Unsealed (mutable) behavior – the returned list can be
     *    mutated but such mutations do NOT affect the underlying map.
     * ------------------------------------------------------------- */
    @Test
    void unsealedValuesAddDoesNotAffectUnderlyingMap() {
        Collection<Integer> values = map.values();
        // Add a new element via the collection.
        assertTrue(values.add(5), "Adding a new value should succeed on an unsealed view");
        assertTrue(values.contains(5), "The added value must be present in the collection");
        assertEquals(5, values.size(), "Collection size must reflect the addition");
        // The underlying map must stay unchanged.
        assertFalse(map.containsValue(5), "Underlying map must not contain the value added to the view");
        assertEquals(4, map.size(), "Map size must remain unchanged");
    }

    @Test
    void unsealedValuesRemoveDoesNotAffectUnderlyingMap() {
        Collection<Integer> values = map.values();
        // Remove an existing value via the collection.
        assertTrue(values.remove(2), "Removing an existing value should succeed on an unsealed view");
        assertFalse(values.contains(2), "Removed value must no longer be in the collection");
        assertEquals(3, values.size(), "Collection size must reflect the removal");
        // The underlying map must stay unchanged.
        assertTrue(map.containsValue(2), "Underlying map must still contain the removed value");
        assertEquals(4, map.size(), "Map size must remain unchanged");
    }

    /* -------------------------------------------------------------
     * 4. Independence from underlying map after the call – the
     *    collection must not reflect later map modifications.
     * ------------------------------------------------------------- */
    @Test
    void valuesDoNotReflectEntriesAddedAfterCall() {
        // capture snapshot
        Collection<Integer> snapshot = map.values();
        // Add a new entry to the underlying map.
        map.put("four", 4);
        // Snapshot must stay unchanged.
        assertEquals(4, snapshot.size(), "Snapshot should not grow after map addition");
        assertFalse(snapshot.contains(4), "Snapshot must not contain the newly added value");
    }

    @Test
    void valuesDoNotReflectEntriesRemovedAfterCall() {
        // capture snapshot
        Collection<Integer> snapshot = map.values();
        // Remove an entry from the underlying map.
        map.remove("two");
        // Snapshot must still contain the removed value.
        assertEquals(4, snapshot.size(), "Snapshot size must stay the same after map removal");
        assertTrue(snapshot.contains(2), "Snapshot must still contain the value that was removed from the map");
    }

    /* -------------------------------------------------------------
     * 5. Null handling – null values are part of the snapshot and can be
     *    removed when unsealed, but removal is prohibited when sealed.
     * ------------------------------------------------------------- */
    @Test
    void valuesContainNullWhenMapHasNullValue() {
        Collection<Integer> values = map.values();
        assertTrue(values.contains(null), "Values view must contain null when map has a null value");
    }

    @Test
    void valuesReplaceNullWhenMapUpdatesValue() {
        Collection<Integer> values = map.values();
        // Replace the null value with a non‑null value.
        map.put(null, 42);
        // The original snapshot must still contain null (defensive copy).
        assertTrue(values.contains(null), "Snapshot must still contain the original null after map update");
        // A fresh call should reflect the new value.
        Collection<Integer> fresh = map.values();
        assertFalse(fresh.contains(null), "New snapshot must not contain null after replacement");
        assertTrue(fresh.contains(42), "New snapshot must contain the updated non‑null value");
    }

    @Test
    void iteratorRemoveRespectsSealState() {
        Collection<Integer> values = map.values();
        // Unsealed iterator should allow remove().
        Iterator<Integer> it = values.iterator();
        assertTrue(it.hasNext());
        Integer first = it.next();
        // should not throw
        it.remove();
        assertEquals(3, values.size(), "After iterator.remove() the collection size must shrink");
        // Seal the map and obtain a new iterator.
        sealedState = true;
        Iterator<Integer> sealedIt = values.iterator();
        assertTrue(sealedIt.hasNext());
        sealedIt.next();
        assertThrows(IllegalStateException.class, sealedIt::remove, "Iterator.remove() on a sealed values view must throw IllegalStateException");
    }
}
