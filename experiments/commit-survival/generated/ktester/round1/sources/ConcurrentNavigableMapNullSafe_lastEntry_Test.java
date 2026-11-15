package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
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
class ConcurrentNavigableMapNullSafe_lastEntry_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh map for every test to avoid cross‑test contamination
        map = new ConcurrentNavigableMapNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map – not strictly required because a new instance is created each time,
        // but it demonstrates proper cleanup practice.
        map.clear();
    }

    /**
     * Uses reflection to set the {@code internalMap} field defined in
     * {@link AbstractConcurrentNullSafeMap}. The field is package‑private
     * (or protected) and therefore not directly accessible.
     *
     * @param target the wrapper instance whose internal map should be replaced
     * @param newMap the map to inject (may be {@code null})
     * @throws Exception if reflection fails
     */
    private static void setInternalMap(Object target, Object newMap) throws Exception {
        Field f = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        f.setAccessible(true);
        f.set(target, newMap);
    }

    @Test
    void lastEntryWhenMapIsEmptyReturnsNull() {
        assertNull(map.lastEntry(), "lastEntry() should return null for an empty map");
    }

    @Test
    void lastEntryReturnsHighestKeyWhenNoNullKeyPresent() {
        map.put("apple", 1);
        map.put("banana", 2);
        // "cherry" is lexicographically highest
        map.put("cherry", 3);
        Entry<String, Integer> entry = map.lastEntry();
        assertNotNull(entry, "lastEntry() should not be null when map has entries");
        assertEquals("cherry", entry.getKey(), "lastEntry() should return the entry with the highest key");
        assertEquals(3, entry.getValue());
        // Verify that the returned entry is live – modifying its value updates the map
        entry.setValue(30);
        assertEquals(30, map.get("cherry"));
    }

    @Test
    void lastEntryReturnsNullKeyWhenNullKeyIsPresent() {
        map.put("alpha", 10);
        map.put("beta", 20);
        // Null key should be considered the last entry
        map.put(null, 999);
        Entry<String, Integer> entry = map.lastEntry();
        assertNotNull(entry, "lastEntry() should not be null when map contains a null key");
        assertNull(entry.getKey(), "The key of the last entry should be null when a null key is present");
        assertEquals(999, entry.getValue());
        // Changing the value through the entry must reflect in the map
        entry.setValue(111);
        assertEquals(111, map.get(null));
    }

    @Test
    void lastEntryWithOnlyNullKey() {
        map.put(null, 42);
        Entry<String, Integer> entry = map.lastEntry();
        assertNotNull(entry);
        assertNull(entry.getKey(), "When the only key is null, lastEntry() must return that entry");
        assertEquals(42, entry.getValue());
    }

    @Test
    void lastEntryReturnsSingleEntry() {
        map.put("apple", 1);
        Entry<String, Integer> e = map.lastEntry();
        assertNotNull(e);
        assertEquals("apple", e.getKey());
        assertEquals(1, e.getValue());
    }

    // Group: null key present together with other keys – null key should be last
    @Test
    void lastEntryIgnoresNullKeyWhenNullIsConsideredHighest() {
        map.put("apple", 1);
        map.put("zebra", 99);
        map.put(null, 500);
        Entry<String, Integer> e = map.lastEntry();
        assertNotNull(e);
        assertNull(e.getKey(), "Null key should be returned as the last entry");
        assertEquals(500, e.getValue());
    }

    // Group: entry with null value
    @Test
    void lastEntryReturnsEntryWithNullValue() {
        map.put("maxKey", null);
        Entry<String, Integer> e = map.lastEntry();
        assertNotNull(e);
        assertEquals("maxKey", e.getKey());
        assertNull(e.getValue());
    }

    // Group: custom comparator (reverse order)
    @Test
    void lastEntryRespectsReverseComparator() throws Exception {
        // Build a map that uses a reverse‑order comparator
        Comparator<String> reverse = Comparator.reverseOrder();
        ConcurrentSkipListMap<Object, Object> internal = new ConcurrentSkipListMap<>(reverse);
        // Use reflection to replace the internal map of the wrapper
        setInternalMap(map, internal);
        map.put("apple", 10);
        map.put("banana", 20);
        map.put("zebra", 30);
        // In reverse order the “highest” key is the natural smallest: "apple"
        Entry<String, Integer> e = map.lastEntry();
        assertNotNull(e);
        assertEquals("apple", e.getKey());
        assertEquals(10, e.getValue());
    }

    // Group: integer keys
    @Test
    void lastEntryWorksWithIntegerKeys() throws Exception {
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        // Replace internal map with a proper ConcurrentSkipListMap<Integer, String>
        ConcurrentSkipListMap<Object, Object> internal = new ConcurrentSkipListMap<>();
        setInternalMap(intMap, internal);
        intMap.put(5, "a");
        intMap.put(17, "b");
        intMap.put(42, "max");
        Entry<Integer, String> e = intMap.lastEntry();
        assertNotNull(e);
        assertEquals(42, e.getKey());
        assertEquals("max", e.getValue());
    }

    // Group: entry.setValue updates underlying map (already covered, but keep explicit)
    @Test
    void lastEntryEntrySetValueUpdatesUnderlyingMap() {
        map.put("alpha", 1);
        map.put("omega", 2);
        // should be "omega"
        Entry<String, Integer> e = map.lastEntry();
        assertEquals("omega", e.getKey());
        e.setValue(99);
        assertEquals(99, map.get("omega"));
        assertEquals(99, e.getValue());
    }

    // Group: after pollLastEntry()
    @Test
    void lastEntryReflectsStateAfterPollLastEntry() {
        map.put(1, "a");
        map.put(2, "b");
        map.put(3, "c");
        // Remove the highest entry
        map.pollLastEntry();
        Entry<Integer, String> e = map.lastEntry();
        assertNotNull(e);
        assertEquals(2, e.getKey());
        assertEquals("b", e.getValue());
    }

    @Test
    void lastEntryThrowsNPEWhenInternalMapIsNull() throws Exception {
        // Force internalMap to null via reflection
        setInternalMap(map, null);
        assertThrows(NullPointerException.class, () -> map.lastEntry(), "When internalMap is null, lastEntry() should throw NPE");
    }

    @Test
    void lastEntryThrowsClassCastExceptionWhenInternalMapNotSkipList() throws Exception {
        // Replace internalMap with a plain HashMap (not a ConcurrentSkipListMap)
        Map<Object, Object> hashMap = new HashMap<>();
        setInternalMap(map, hashMap);
        assertThrows(ClassCastException.class, () -> map.lastEntry(), "When internalMap is not a ConcurrentSkipListMap, a ClassCastException is expected");
    }

    @Disabled("wrapEntry is private and throws only under internal bugs; testing it requires PowerMock or byte‑code manipulation")
    @Test
    void lastEntryPropagatesRuntimeExceptionFromWrapEntry() {
        // This test is intentionally left disabled because the private wrapEntry method
        // cannot be stubbed with standard Mockito. A more powerful framework would be needed.
    }
}
