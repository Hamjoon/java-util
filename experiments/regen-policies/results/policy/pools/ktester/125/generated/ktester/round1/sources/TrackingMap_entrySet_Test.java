package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.AbstractMap;

@ExtendWith(MockitoExtension.class)
class TrackingMap_entrySet_Test {

    private Map<String, Integer> backingMap;

    private TrackingMap<String, Integer> trackingMap;

    @BeforeEach
    void setupBeforeEach() {
        backingMap = new HashMap<>();
        trackingMap = new TrackingMap<>(backingMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC; not strictly required.
        backingMap = null;
        trackingMap = null;
    }

    @Test
    void entrySetReflectsAllEntries() {
        backingMap.put("a", 1);
        backingMap.put("b", 2);
        Set<Map.Entry<String, Integer>> entrySet = trackingMap.entrySet();
        assertEquals(2, entrySet.size(), "Entry set size should match number of entries in the map");
        assertTrue(entrySet.stream().anyMatch(e -> "a".equals(e.getKey()) && Integer.valueOf(1).equals(e.getValue())), "Entry set should contain key 'a' with value 1");
        assertTrue(entrySet.stream().anyMatch(e -> "b".equals(e.getKey()) && Integer.valueOf(2).equals(e.getValue())), "Entry set should contain key 'b' with value 2");
    }

    @Test
    void entrySetIsBackedByBackingMap() {
        trackingMap.put("x", 10);
        Set<Map.Entry<String, Integer>> entrySet = trackingMap.entrySet();
        assertEquals(1, entrySet.size(), "Entry set should contain the single inserted entry");
        Iterator<Map.Entry<String, Integer>> iterator = entrySet.iterator();
        assertTrue(iterator.hasNext(), "Iterator should have a next element");
        Map.Entry<String, Integer> entry = iterator.next();
        assertEquals("x", entry.getKey(), "Key should be 'x'");
        assertEquals(10, entry.getValue(), "Value should be 10");
        // Remove via iterator and verify removal propagates to both maps.
        iterator.remove();
        assertFalse(trackingMap.containsKey("x"), "TrackingMap should no longer contain key 'x' after removal");
        assertFalse(backingMap.containsKey("x"), "Backing map should no longer contain key 'x' after removal");
    }

    @Test
    void entrySetIsSameInstanceAsBackingMapEntrySet() {
        trackingMap.put("k1", 100);
        trackingMap.put("k2", 200);
        // The contract of TrackingMap.entrySet() is to return the internal map's entrySet.
        // Therefore the returned set should be the identical object as backingMap.entrySet().
        assertSame(backingMap.entrySet(), trackingMap.entrySet(), "entrySet() should return the exact Set instance from the backing map");
    }

    @Test
    void entrySetOnEmptyMap() {
        Set<Map.Entry<String, Integer>> entrySet = trackingMap.entrySet();
        assertNotNull(entrySet, "entrySet() must never return null");
        assertTrue(entrySet.isEmpty(), "Entry set of an empty map should be empty");
    }

    /**
     * Verify that an empty TrackingMap returns an empty Set.
     */
    @Test
    void entrySetEmptyMapReturnsEmptySet() {
        // No entries added to backingMap.
        Set<Map.Entry<String, Integer>> set = trackingMap.entrySet();
        assertNotNull(set, "entrySet() must never return null");
        assertTrue(set.isEmpty(), "Expected empty Set for an empty backing map");
    }

    /**
     * Verify that entrySet contains exactly the same entries as the wrapped map,
     * including a null value.
     */
    @Test
    void entrySetContainsAllWrappedEntriesIncludingNullValue() {
        backingMap.put("one", 1);
        backingMap.put("two", null);
        backingMap.put("three", 3);
        Set<Map.Entry<String, Integer>> set = trackingMap.entrySet();
        assertEquals(3, set.size(), "Set size should match number of entries in the backing map");
        assertTrue(set.stream().anyMatch(e -> "one".equals(e.getKey()) && Integer.valueOf(1).equals(e.getValue())), "Set must contain entry ('one',1)");
        assertTrue(set.stream().anyMatch(e -> "two".equals(e.getKey()) && e.getValue() == null), "Set must contain entry ('two',null)");
        assertTrue(set.stream().anyMatch(e -> "three".equals(e.getKey()) && Integer.valueOf(3).equals(e.getValue())), "Set must contain entry ('three',3)");
    }

    /**
     * Removing an entry via the iterator of the returned Set must affect the underlying map.
     */
    @Test
    void entrySetIteratorRemoveUpdatesUnderlyingMap() {
        backingMap.put("a", 1);
        backingMap.put("b", 2);
        Set<Map.Entry<String, Integer>> set = trackingMap.entrySet();
        Iterator<Map.Entry<String, Integer>> it = set.iterator();
        while (it.hasNext()) {
            Map.Entry<String, Integer> e = it.next();
            if ("a".equals(e.getKey())) {
                // remove entry for key "a"
                it.remove();
                break;
            }
        }
        assertFalse(trackingMap.containsKey("a"), "Key 'a' should have been removed from TrackingMap");
        assertEquals(1, trackingMap.size(), "TrackingMap size should be 1 after removal");
        assertFalse(backingMap.containsKey("a"), "Key 'a' should have been removed from backing map");
    }

    /**
     * The Set returned by entrySet() must be unmodifiable for add operations.
     */
    @Test
    void entrySetAddOperationThrowsUnsupportedOperationException() {
        trackingMap.put("existing", 5);
        Set<Map.Entry<String, Integer>> set = trackingMap.entrySet();
        AbstractMap.SimpleEntry<String, Integer> newEntry = new AbstractMap.SimpleEntry<>("c", 3);
        assertThrows(UnsupportedOperationException.class, () -> set.add(newEntry), "Adding a new entry to the entrySet should throw UnsupportedOperationException");
    }

    /**
     * Verify that the Set view is live: after obtaining the Set, a later put()
     * is reflected in the same Set instance.
     */
    @Test
    void entrySetLiveViewReflectsSubsequentPut() {
        // initially empty
        Set<Map.Entry<String, Integer>> set = trackingMap.entrySet();
        // modify map after obtaining the view
        trackingMap.put("x", 99);
        assertTrue(set.stream().anyMatch(e -> "x".equals(e.getKey()) && Integer.valueOf(99).equals(e.getValue())), "Set view should contain the newly added entry after put()");
        assertEquals(1, set.size(), "Set size should be updated to reflect new entry");
    }

    /**
     * Verify that the Set view reflects removal of a key directly from the map.
     */
    @Test
    void entrySetLiveViewReflectsDirectRemoval() {
        trackingMap.put("k", 10);
        Set<Map.Entry<String, Integer>> set = trackingMap.entrySet();
        // Direct removal via TrackingMap API
        trackingMap.remove("k");
        assertTrue(set.isEmpty(), "Set view should be empty after the underlying entry is removed");
    }

    /**
     * entrySet() must contain an entry whose key is null when the backing map
     * has a null key.
     */
    @Test
    void entrySetContainsNullKeyEntry() {
        backingMap.put(null, 42);
        Set<Map.Entry<String, Integer>> set = trackingMap.entrySet();
        assertEquals(1, set.size(), "Set should contain exactly one entry for the null key");
        Map.Entry<String, Integer> entry = set.iterator().next();
        assertNull(entry.getKey(), "Entry key should be null");
        assertEquals(42, entry.getValue(), "Entry value should match the value associated with the null key");
    }

    /**
     * Removing the entry with a null key via the iterator must delete the null key
     * from the underlying map.
     */
    @Test
    void entrySetIteratorRemoveNullKey() {
        backingMap.put(null, 42);
        backingMap.put("a", 1);
        Set<Map.Entry<String, Integer>> set = trackingMap.entrySet();
        Iterator<Map.Entry<String, Integer>> it = set.iterator();
        while (it.hasNext()) {
            Map.Entry<String, Integer> e = it.next();
            if (e.getKey() == null) {
                it.remove();
                break;
            }
        }
        assertFalse(trackingMap.containsKey(null), "Null key should have been removed from TrackingMap");
        assertEquals(1, trackingMap.size(), "TrackingMap size should be 1 after removing null key");
        assertFalse(backingMap.containsKey(null), "Null key should have been removed from backing map");
    }

    /**
     * If the internalMap field is {@code null}, entrySet() should throw a
     * {@link NullPointerException}. This simulates a corrupted instance.
     */
    @Test
    void entrySetThrowsNPEWhenInternalMapIsNull() throws Exception {
        // Create a normal instance first.
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        // Use reflection to set the private field 'internalMap' to null.
        Field internalMapField = TrackingMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(map, null);
        // The method should now throw NPE.
        assertThrows(NullPointerException.class, map::entrySet, "Calling entrySet() when internalMap is null must throw NullPointerException");
    }
}
