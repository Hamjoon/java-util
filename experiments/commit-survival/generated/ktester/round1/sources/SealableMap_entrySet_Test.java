package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class SealableMap_entrySet_Test {

    private SealableMap<String, Integer> map;

    private volatile boolean sealedState = false;

    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    @BeforeEach
    void setupBeforeEach() {
        // The constructor used in the original test suite creates an internal HashMap.
        map = new SealableMap<>(sealedSupplier);
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        // include a null entry for later verification
        map.put(null, null);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset sealed state to avoid cross‑test interference
        sealedState = false;
    }

    @Test
    void testEntrySetIsNotNullAndContainsExpectedEntries() {
        Set<Map.Entry<String, Integer>> entries = map.entrySet();
        assertNotNull(entries, "entrySet() should not return null");
        assertEquals(4, entries.size(), "entrySet() size should match map size");
        // Verify a known entry is present
        boolean containsTwo = entries.stream().anyMatch(e -> Objects.equals(e.getKey(), "two") && Objects.equals(e.getValue(), 2));
        assertTrue(containsTwo, "entrySet() should contain the entry for key \"two\"");
        // Verify the null entry is present
        boolean containsNull = entries.stream().anyMatch(e -> e.getKey() == null && e.getValue() == null);
        assertTrue(containsNull, "entrySet() should contain the null key/value entry");
    }

    @Test
    void testEntrySetModificationWhenUnsealed() {
        Set<Map.Entry<String, Integer>> entries = map.entrySet();
        assertEquals(4, entries.size());
        // Remove an entry via iterator when the map is unsealed
        Iterator<Map.Entry<String, Integer>> it = entries.iterator();
        while (it.hasNext()) {
            Map.Entry<String, Integer> e = it.next();
            if ("one".equals(e.getKey())) {
                // should succeed
                it.remove();
                break;
            }
        }
        assertNull(map.get("one"), "Removed key should no longer be present in the map");
        assertEquals(3, map.size(), "Map size should reflect the removal");
        assertEquals(3, entries.size(), "Entry set size should reflect the removal");
    }

    @Test
    void testEntrySetModificationWhenSealed() {
        Set<Map.Entry<String, Integer>> entries = map.entrySet();
        // Seal the map
        sealedState = true;
        // removeIf should throw UnsupportedOperationException (original behaviour)
        assertThrows(UnsupportedOperationException.class, () -> entries.removeIf(e -> "two".equals(e.getKey())));
        // iterator().remove() should also throw
        Iterator<Map.Entry<String, Integer>> it = entries.iterator();
        assertTrue(it.hasNext(), "Iterator should have at least one element");
        // move to first element
        it.next();
        assertThrows(UnsupportedOperationException.class, it::remove);
    }

    @Test
    void testEntrySetAddWhenSealedThrows() {
        Set<Map.Entry<String, Integer>> entries = map.entrySet();
        // Seal the map
        sealedState = true;
        Map.Entry<String, Integer> newEntry = new AbstractMap.SimpleImmutableEntry<>("five", 5);
        assertThrows(UnsupportedOperationException.class, () -> entries.add(newEntry));
    }

    @Test
    void testEntrySetAddWhenUnsealedSucceeds() {
        Set<Map.Entry<String, Integer>> entries = map.entrySet();
        // Ensure map is unsealed (default state)
        assertFalse(sealedState, "Map should be unsealed at start of test");
        Map.Entry<String, Integer> newEntry = new AbstractMap.SimpleImmutableEntry<>("five", 5);
        boolean added = entries.add(newEntry);
        assertTrue(added, "Adding a new entry should succeed when unsealed");
        assertEquals(5, map.get("five"));
        assertEquals(5, map.size());
    }

    @Test
    void testEntrySetIteratorRemovalWhenSealedThrows() {
        Set<Map.Entry<String, Integer>> entries = map.entrySet();
        // Seal the map
        sealedState = true;
        Iterator<Map.Entry<String, Integer>> it = entries.iterator();
        assertTrue(it.hasNext());
        // advance to first element
        it.next();
        assertThrows(UnsupportedOperationException.class, it::remove, "Iterator.remove() must throw when map is sealed");
    }

    @Test
    void testEntrySetReturnsSealableSetInstance() {
        Set<Map.Entry<String, Integer>> entries = map.entrySet();
        // The concrete type should be SealableSet (or a subclass thereof)
        assertTrue(entries instanceof SealableSet, "entrySet() must return an instance of SealableSet when map is unsealed");
    }

    @Test
    void testEntrySetOnEmptyUnderlyingMap() {
        // Create a fresh SealableMap that stays empty
        SealableMap<String, Integer> emptyMap = new SealableMap<>(sealedSupplier);
        Set<Map.Entry<String, Integer>> entries = emptyMap.entrySet();
        assertNotNull(entries, "entrySet() must never be null");
        assertTrue(entries.isEmpty(), "Entry set of an empty map should be empty");
        assertEquals(0, entries.size(), "Size must be zero for an empty underlying map");
    }

    @Test
    void testSealedModificationThrowsIllegalStateException() {
        // Obtain the entry set while the map is still unsealed
        Set<Map.Entry<String, Integer>> entries = map.entrySet();
        // Seal the map
        sealedState = true;
        // Attempt a mutating operation – remove(Object) – and expect IllegalStateException
        Map.Entry<String, Integer> entryToRemove = new AbstractMap.SimpleImmutableEntry<>("one", 1);
        assertThrows(IllegalStateException.class, () -> entries.remove(entryToRemove), "Removing an entry from a sealed SealableSet must throw IllegalStateException");
    }

    @Test
    void testSealableSetDelegatesEqualsHashCodeToUnderlyingSet() {
        Set<Map.Entry<String, Integer>> entries = map.entrySet();
        // underlying view (same instance)
        Set<Map.Entry<String, Integer>> underlying = map.entrySet();
        // Equality
        assertEquals(underlying, entries, "SealableSet should be equal to the wrapped entrySet");
        // Hash code
        assertEquals(underlying.hashCode(), entries.hashCode(), "hashCode() must be delegated to the wrapped entrySet");
        // toString
        assertEquals(underlying.toString(), entries.toString(), "toString() must be delegated to the wrapped entrySet");
    }

    @Test
    void testEntrySetReflectsSubsequentMapModifications() {
        Set<Map.Entry<String, Integer>> entries = map.entrySet();
        // Add a new entry directly to the map after obtaining the view
        map.put("four", 4);
        // The entry set must now contain the newly added entry
        boolean containsFour = entries.stream().anyMatch(e -> Objects.equals(e.getKey(), "four") && Objects.equals(e.getValue(), 4));
        assertTrue(containsFour, "Entry set must reflect additions made to the underlying map");
    }

    @Test
    void testEntrySetRemoveUpdatesUnderlyingMapWhenUnsealed() {
        Set<Map.Entry<String, Integer>> entries = map.entrySet();
        // Find the entry for key "two"
        Map.Entry<String, Integer> target = entries.stream().filter(e -> "two".equals(e.getKey())).findFirst().orElseThrow(() -> new AssertionError("Expected entry not found"));
        // Remove via the set view
        boolean removed = entries.remove(target);
        assertTrue(removed, "remove(entry) should succeed when map is unsealed");
        assertNull(map.get("two"), "Underlying map must no longer contain the removed key");
        assertEquals(3, map.size(), "Map size must be updated after removal");
    }

    @Test
    void testIteratorRemoveThrowsIllegalStateExceptionWhenSealed() {
        Set<Map.Entry<String, Integer>> entries = map.entrySet();
        // Obtain iterator before sealing
        Iterator<Map.Entry<String, Integer>> it = entries.iterator();
        assertTrue(it.hasNext());
        // advance to first element
        it.next();
        // Seal the map now
        sealedState = true;
        // According to the spec, iterator.remove() should now throw IllegalStateException
        assertThrows(IllegalStateException.class, it::remove, "Iterator.remove() must throw IllegalStateException when the map is sealed");
    }

    @Test
    void testEntrySetSizeMatchesMapSizeAfterModifications() {
        Set<Map.Entry<String, Integer>> entries = map.entrySet();
        assertEquals(map.size(), entries.size(), "Initial sizes must match");
        // Add a new entry
        map.put("four", 4);
        assertEquals(map.size(), entries.size(), "Sizes must stay in sync after addition");
        // Remove an entry via the set view
        entries.removeIf(e -> "one".equals(e.getKey()));
        assertEquals(map.size(), entries.size(), "Sizes must stay in sync after removal");
    }

    @Test
    void testEntrySetContainsEntryTrueAndFalse() {
        Set<Map.Entry<String, Integer>> entries = map.entrySet();
        Map.Entry<String, Integer> present = new AbstractMap.SimpleImmutableEntry<>("three", 3);
        Map.Entry<String, Integer> absent = new AbstractMap.SimpleImmutableEntry<>("missing", 99);
        assertTrue(entries.contains(present), "contains() must return true for an existing entry");
        assertFalse(entries.contains(absent), "contains() must return false for a non‑existent entry");
    }

    @Test
    void testEntrySetWithNullUnderlyingMapThrowsNPE() throws Exception {
        // Create a SealableMap instance normally
        SealableMap<String, Integer> nullMap = new SealableMap<>(sealedSupplier);
        // Use reflection to replace the internal 'map' field with null
        Field mapField = SealableMap.class.getDeclaredField("map");
        mapField.setAccessible(true);
        mapField.set(nullMap, null);
        // Invoking entrySet() should now throw NullPointerException
        assertThrows(NullPointerException.class, nullMap::entrySet, "Calling entrySet() on a SealableMap with a null underlying map must throw NPE");
    }
}
