package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.NavigableSet;
import java.util.Iterator;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.ConcurrentNavigableMap;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_keySet_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        map = new ConcurrentNavigableMapNullSafe<>();
        // Populate with a deterministic ordering for the default tests
        map.put("apple", 1);
        map.put("banana", 2);
        map.put("cherry", 3);
        // null key – should appear last in the ascending view
        map.put(null, 99);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to avoid cross‑test contamination
        map.clear();
    }

    @Test
    void keySetContainsAllKeysIncludingNull() {
        // GIVEN – map is pre‑populated in @BeforeEach
        // WHEN
        NavigableSet<String> keySet = map.keySet();
        // THEN – size and presence
        assertEquals(4, keySet.size(), "keySet should contain exactly four elements");
        assertTrue(keySet.contains("apple"));
        assertTrue(keySet.contains("banana"));
        assertTrue(keySet.contains("cherry"));
        assertTrue(keySet.contains(null));
    }

    @Test
    void keySetOrderingPlacesNullLastInAscendingView() {
        // GIVEN
        NavigableSet<String> keySet = map.keySet();
        // WHEN
        Iterator<String> iterator = keySet.iterator();
        // THEN – natural (String) ordering, null last
        assertEquals("apple", iterator.next());
        assertEquals("banana", iterator.next());
        assertEquals("cherry", iterator.next());
        assertNull(iterator.next(), "Null key must be the last element in the ascending view");
        assertFalse(iterator.hasNext());
    }

    @Test
    void keySetFirstAndLastReflectMapOrdering() {
        // GIVEN
        NavigableSet<String> keySet = map.keySet();
        // WHEN / THEN
        assertEquals("apple", keySet.first(), "first() should return the smallest (non‑null) key");
        assertNull(keySet.last(), "last() should return null because null is considered last");
    }

    @Test
    void keySetRemovalUpdatesMap() {
        // GIVEN
        NavigableSet<String> keySet = map.keySet();
        // WHEN – remove a non‑null key via the set view
        boolean removed = keySet.remove("banana");
        // THEN – the set reports successful removal and the map no longer contains the entry
        assertTrue(removed, "remove should return true for an existing key");
        assertFalse(keySet.contains("banana"));
        assertNull(map.get("banana"));
        assertEquals(3, map.size(), "Map size should decrease after removal via keySet");
        // WHEN – remove the null key via the set view
        boolean nullRemoved = keySet.remove(null);
        // THEN
        assertTrue(nullRemoved, "remove should succeed for the null key");
        assertFalse(keySet.contains(null));
        assertNull(map.get(null));
        assertEquals(2, map.size(), "Map size should reflect the second removal");
    }

    @Test
    void keySetIsBackedByMapWhenMapIsModifiedDirectly() {
        // GIVEN
        NavigableSet<String> keySet = map.keySet();
        // WHEN – add a new entry directly to the map
        map.put("date", 4);
        // THEN – the keySet view reflects the new key in the correct order (after "cherry", before null)
        assertTrue(keySet.contains("date"));
        Iterator<String> it = keySet.iterator();
        assertEquals("apple", it.next());
        assertEquals("banana", it.next());
        assertEquals("cherry", it.next());
        assertEquals("date", it.next());
        assertNull(it.next());
    }

    @Test
    @DisplayName("keySet() - Empty map")
    void keySetEmptyMap() {
        // GIVEN – an empty map (clear the one created in @BeforeEach)
        map.clear();
        // WHEN
        NavigableSet<String> keySet = map.keySet();
        // THEN
        assertEquals(0, keySet.size(), "keySet of an empty map must have size 0");
        assertFalse(keySet.iterator().hasNext(), "Iterator should have no elements");
    }

    @Test
    @DisplayName("keySet() - Non‑empty map with Integer keys")
    void keySetIntegerKeys() {
        // GIVEN – a map with Integer keys, created locally to avoid interfering with the String map
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(2, "two");
        intMap.put(1, "one");
        intMap.put(3, "three");
        // WHEN
        NavigableSet<Integer> keySet = intMap.keySet();
        // THEN – size and natural ordering
        assertEquals(3, keySet.size(), "keySet should contain three integer keys");
        Iterator<Integer> it = keySet.iterator();
        assertEquals(1, it.next());
        assertEquals(2, it.next());
        assertEquals(3, it.next());
        assertFalse(it.hasNext(), "No more elements expected");
    }

    @Test
    @DisplayName("keySet() - Contains only a null key")
    void keySetOnlyNullKey() {
        // GIVEN – a fresh map that only contains a null key
        ConcurrentNavigableMapNullSafe<String, Integer> nullOnlyMap = new ConcurrentNavigableMapNullSafe<>();
        nullOnlyMap.put(null, 42);
        // WHEN
        NavigableSet<String> keySet = nullOnlyMap.keySet();
        // THEN
        assertEquals(1, keySet.size(), "keySet should report size 1 when only null key is present");
        assertTrue(keySet.contains(null), "keySet must contain the null key");
        // iterator order – null is the only element, thus also the first and last
        Iterator<String> it = keySet.iterator();
        assertNull(it.next());
        assertFalse(it.hasNext());
    }

    @Test
    @DisplayName("keySet() - Reflects map modifications (add then remove via set)")
    void keySetReflectsMapModifications() {
        // GIVEN – obtain a view before any modifications
        NavigableSet<String> keySet = map.keySet();
        // WHEN – add a new entry directly to the map
        map.put("C", 3);
        // THEN – the set sees the new key in proper order (after "cherry", before null)
        assertTrue(keySet.contains("C"));
        Iterator<String> it = keySet.iterator();
        assertEquals("apple", it.next());
        assertEquals("banana", it.next());
        assertEquals("cherry", it.next());
        assertEquals("C", it.next());
        assertNull(it.next());
        // WHEN – remove a key via the set view
        boolean removed = keySet.remove("apple");
        // THEN – map no longer contains the key and set size reflects removal
        assertTrue(removed, "Removal via set should succeed");
        assertNull(map.get("apple"), "Map must no longer contain the removed key");
        assertEquals(4, keySet.size(), "Set size should be 4 after removal (original 4 + added C - removed apple)");
        assertFalse(keySet.contains("apple"));
    }

    @Test
    @DisplayName("keySet() - Iterator order matches map ordering for unsorted inserts")
    void keySetIteratorOrderMatchesMapOrdering() {
        // GIVEN – a map where keys are inserted out of order
        ConcurrentNavigableMapNullSafe<Integer, String> unsortedMap = new ConcurrentNavigableMapNullSafe<>();
        unsortedMap.put(5, "five");
        unsortedMap.put(1, "one");
        unsortedMap.put(3, "three");
        // WHEN
        NavigableSet<Integer> keySet = unsortedMap.keySet();
        // THEN – iterator must return keys sorted ascending
        Iterator<Integer> it = keySet.iterator();
        assertEquals(1, it.next());
        assertEquals(3, it.next());
        assertEquals(5, it.next());
        assertFalse(it.hasNext());
    }

    @Test
    @DisplayName("keySet() - Unsupported add operation")
    void keySetUnsupportedAdd() {
        // GIVEN
        NavigableSet<String> keySet = map.keySet();
        // WHEN / THEN – attempting to add directly must throw UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, () -> keySet.add("newKey"), "Adding a key directly to the keySet view must be unsupported");
    }

    @Test
    @DisplayName("keySet() - Iterator remove without preceding next()")
    void iteratorRemoveWithoutNextThrows() {
        // GIVEN
        NavigableSet<String> keySet = map.keySet();
        Iterator<String> it = keySet.iterator();
        // WHEN / THEN – calling remove before next should throw IllegalStateException
        assertThrows(IllegalStateException.class, it::remove, "Iterator.remove() called before next() must throw IllegalStateException");
    }
}
