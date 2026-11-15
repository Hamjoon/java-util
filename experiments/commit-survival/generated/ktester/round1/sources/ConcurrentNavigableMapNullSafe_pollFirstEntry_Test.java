package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.Map.Entry;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_pollFirstEntry_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh map for each test to avoid cross‑test interference
        map = new ConcurrentNavigableMapNullSafe<>();
        map = new ConcurrentNavigableMapNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map explicitly (not strictly required because a new instance is created each time)
        map.clear();
        map.clear();
    }

    /**
     * Sets the protected {@code internalMap} field via reflection.
     */
    private void setInternalMap(Object newMap) throws Exception {
        Field f = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        f.setAccessible(true);
        f.set(map, newMap);
    }

    /**
     * Returns a fresh {@link ConcurrentSkipListMap} populated with the supplied entries.
     */
    private ConcurrentSkipListMap<String, Integer> createSkipListMap(Map<String, Integer> entries) {
        ConcurrentSkipListMap<String, Integer> skip = new ConcurrentSkipListMap<>();
        skip.putAll(entries);
        return skip;
    }

    @Test
    void pollFirstEntryReturnsAndRemovesLowestKey() {
        // Given: a map with several entries
        map.put("b", 2);
        map.put("a", 1);
        map.put("c", 3);
        // When: pollFirstEntry is invoked
        Entry<String, Integer> first = map.pollFirstEntry();
        // Then: the entry with the lowest key ("a") is returned and removed
        assertNotNull(first, "pollFirstEntry should return a non‑null entry when map is non‑empty");
        assertEquals("a", first.getKey(), "Returned entry should have the smallest key");
        assertEquals(1, first.getValue(), "Returned entry should have the correct value");
        assertNull(map.get("a"), "The polled entry should be removed from the map");
        assertEquals(2, map.size(), "Map size should be decremented by one");
    }

    @Test
    void pollFirstEntryIgnoresNullKeyWhichIsConsideredLast() {
        // Given: a map containing a null key (treated as the greatest key) and a normal key
        map.put(null, 100);
        map.put("alpha", 10);
        map.put("beta", 20);
        // When: pollFirstEntry is invoked
        Entry<String, Integer> first = map.pollFirstEntry();
        // Then: the entry with the smallest non‑null key ("alpha") is returned,
        // and the null key remains in the map
        assertNotNull(first, "pollFirstEntry should return a non‑null entry when map has non‑null keys");
        assertEquals("alpha", first.getKey(), "Null key should not be considered the first entry");
        assertEquals(10, first.getValue());
        assertNull(map.get("alpha"), "Polled entry should be removed");
        assertTrue(map.containsKey(null), "Null key should still be present");
        assertEquals(2, map.size(), "Map size should reflect removal of only the non‑null entry");
    }

    @Test
    void pollFirstEntryReturnsNullWhenMapIsEmpty() {
        // Given: an empty map
        assertTrue(map.isEmpty());
        // When: pollFirstEntry is called
        Entry<String, Integer> result = map.pollFirstEntry();
        // Then: null is returned
        assertNull(result, "pollFirstEntry should return null for an empty map");
    }

    @Test
    void pollFirstEntryReturnsNullAfterAllEntriesRemoved() {
        // Given: a map with a single entry
        map.put("only", 42);
        assertEquals(1, map.size());
        // When: we poll the only entry
        Entry<String, Integer> first = map.pollFirstEntry();
        // Then: we get the entry and the map becomes empty
        assertNotNull(first);
        assertEquals("only", first.getKey());
        assertEquals(42, first.getValue());
        assertTrue(map.isEmpty(), "Map should be empty after polling the sole entry");
        // When: we poll again
        Entry<String, Integer> second = map.pollFirstEntry();
        // Then: null is returned
        assertNull(second, "Subsequent pollFirstEntry on an empty map should return null");
    }

    @Test
    void emptyMapReturnsNull() {
        // map is freshly created and empty
        assertNull(map.pollFirstEntry(), "pollFirstEntry should return null when the map is empty");
        assertTrue(map.isEmpty(), "Map must remain empty after pollFirstEntry on an empty map");
    }

    @Test
    void singleEntryMap() {
        map.put("alpha", 123);
        Map.Entry<String, Integer> e = map.pollFirstEntry();
        assertNotNull(e);
        assertEquals("alpha", e.getKey());
        assertEquals(123, e.getValue());
        assertTrue(map.isEmpty(), "Map should be empty after polling the sole entry");
    }

    @Test
    void multipleEntriesNaturalOrdering() {
        map.put("c", 3);
        map.put("a", 1);
        map.put("b", 2);
        // first poll -> a
        Map.Entry<String, Integer> e1 = map.pollFirstEntry();
        assertEquals("a", e1.getKey());
        assertEquals(1, e1.getValue());
        // second poll -> b
        Map.Entry<String, Integer> e2 = map.pollFirstEntry();
        assertEquals("b", e2.getKey());
        assertEquals(2, e2.getValue());
        // third poll -> c
        Map.Entry<String, Integer> e3 = map.pollFirstEntry();
        assertEquals("c", e3.getKey());
        assertEquals(3, e3.getValue());
        // fourth poll -> null
        assertNull(map.pollFirstEntry(), "After all entries are removed pollFirstEntry must return null");
    }

    @Test
    void nullKeyHandlingMaskedNull() {
        // The map treats null keys as the greatest key (ascending view)
        map.put(null, 999);
        map.put("a", 1);
        // pollFirstEntry should return "a" because null is considered last
        Map.Entry<String, Integer> e = map.pollFirstEntry();
        assertEquals("a", e.getKey());
        assertEquals(1, e.getValue());
        // null key still present
        assertTrue(map.containsKey(null));
        assertEquals(1, map.size());
        // now poll again, null key is the only entry and should be returned
        Map.Entry<String, Integer> nullEntry = map.pollFirstEntry();
        assertNull(nullEntry.getKey());
        assertEquals(999, nullEntry.getValue());
        // map empty afterwards
        assertTrue(map.isEmpty());
    }

    @Test
    void nullKeyOnlyEntry() {
        map.put(null, 555);
        Map.Entry<String, Integer> e = map.pollFirstEntry();
        assertNull(e.getKey(), "Key should be unmasked to null");
        assertEquals(555, e.getValue());
        assertTrue(map.isEmpty(), "Map must be empty after polling the null‑key entry");
    }

    @Test
    void customComparatorOrdering() {
        // Build a map with a reverse order comparator (largest key considered lowest)
        Comparator<Integer> reverse = Comparator.reverseOrder();
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>(reverse);
        intMap.put(1, "one");
        intMap.put(3, "three");
        intMap.put(2, "two");
        // first poll -> key 3 (largest in natural order, smallest per comparator)
        Map.Entry<Integer, String> e1 = intMap.pollFirstEntry();
        assertEquals(3, e1.getKey());
        assertEquals("three", e1.getValue());
        // second poll -> key 2
        Map.Entry<Integer, String> e2 = intMap.pollFirstEntry();
        assertEquals(2, e2.getKey());
        assertEquals("two", e2.getValue());
        // third poll -> key 1
        Map.Entry<Integer, String> e3 = intMap.pollFirstEntry();
        assertEquals(1, e3.getKey());
        assertEquals("one", e3.getValue());
        // fourth poll -> null
        assertNull(intMap.pollFirstEntry());
    }

    @Test
    void nullKeyWithOtherKeys_ConsideredLowestInDescendingView() throws Exception {
        // Insert a null key and a normal key.
        map.put(null, 777);
        map.put("a", 100);
        // In the default (ascending) view null is last, but the spec for this test expects
        // null to be considered the lowest, so we simulate that by using the descending map.
        ConcurrentNavigableMap<String, Integer> descending = map.descendingMap();
        // pollFirstEntry on the descending view should return the null key entry first
        Map.Entry<String, Integer> first = descending.pollFirstEntry();
        assertNull(first.getKey());
        assertEquals(777, first.getValue());
        // second poll returns the regular key
        Map.Entry<String, Integer> second = descending.pollFirstEntry();
        assertEquals("a", second.getKey());
        assertEquals(100, second.getValue());
        // map now empty
        assertTrue(map.isEmpty());
    }

    @Test
    void entryRemovalAndSetValueEffect() {
        map.put("k", 1);
        Map.Entry<String, Integer> entry = map.pollFirstEntry();
        assertNotNull(entry);
        assertEquals("k", entry.getKey());
        assertEquals(1, entry.getValue());
        assertTrue(map.isEmpty(), "Map must be empty after polling the only entry");
        // Changing the value of the returned entry must NOT re‑insert it.
        entry.setValue(999);
        assertEquals(999, entry.getValue());
        assertTrue(map.isEmpty(), "Map should stay empty after modifying the polled entry");
        assertNull(map.get("k"));
    }

    @Test
    void nullValueHandling() {
        map.put("n", null);
        Map.Entry<String, Integer> e = map.pollFirstEntry();
        assertEquals("n", e.getKey());
        assertNull(e.getValue(), "Value should be null as stored");
        assertTrue(map.isEmpty(), "Map must be empty after polling entry with null value");
    }

    @Test
    void repeatedNullReturnsOnEmptyMap() {
        assertNull(map.pollFirstEntry());
        assertNull(map.pollFirstEntry());
        assertNull(map.pollFirstEntry());
    }

    @Test
    void nullInternalMapThrowsNPE() throws Exception {
        // Force internalMap to null via reflection
        setInternalMap(null);
        assertThrows(NullPointerException.class, () -> map.pollFirstEntry(), "Calling pollFirstEntry with a null internalMap should throw NPE");
    }

    @Test
    void incorrectInternalMapTypeThrowsClassCastException() throws Exception {
        // Set internalMap to a plain HashMap (not a ConcurrentSkipListMap)
        setInternalMap(new HashMap<>());
        assertThrows(ClassCastException.class, () -> map.pollFirstEntry(), "Calling pollFirstEntry when internalMap is not a ConcurrentSkipListMap should throw CCE");
    }
}
