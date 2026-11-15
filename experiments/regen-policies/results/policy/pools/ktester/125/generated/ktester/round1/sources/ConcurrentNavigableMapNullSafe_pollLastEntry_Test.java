package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.concurrent.ConcurrentNavigableMap;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_pollLastEntry_Test {

    /**
     * The map under test.
     */
    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    /**
     * Initialise a fresh map before each test.
     */
    @BeforeEach
    void setupBeforeEach() {
        map = new ConcurrentNavigableMapNullSafe<>();
    }

    /**
     * Clear the reference after each test – helps GC and isolates tests.
     */
    @AfterEach
    void teardownAfterEach() {
        map.clear();
        map = null;
    }

    /**
     * pollLastEntry on an empty map must return {@code null}.
     */
    @Test
    void pollLastEntry_EmptyMap_ReturnsNull() {
        assertNull(map.pollLastEntry(), "pollLastEntry on an empty map should return null");
        assertTrue(map.isEmpty(), "Map should remain empty after pollLastEntry");
    }

    /**
     * pollLastEntry should return the entry with the highest natural key and remove it.
     */
    @Test
    void pollLastEntry_NonNullKeys_ReturnsHighestEntryAndRemovesIt() {
        // given
        map.put("a", 1);
        map.put("c", 3);
        map.put("b", 2);
        // when
        Map.Entry<String, Integer> last = map.pollLastEntry();
        // then
        assertNotNull(last, "Returned entry must not be null");
        assertEquals("c", last.getKey(), "Highest key should be 'c'");
        assertEquals(3, last.getValue(), "Value for key 'c' should be 3");
        assertFalse(map.containsKey("c"), "Key 'c' must have been removed from the map");
        assertEquals(2, map.size(), "Map size should be reduced by one");
    }

    /**
     * When a null key is present it is considered the last entry (ascending view).
     */
    @Test
    void pollLastEntry_WithNullKey_ReturnsNullKeyEntryFirst() {
        // given
        map.put("x", 10);
        // null key is treated as the greatest key
        map.put(null, 99);
        map.put("m", 20);
        // when
        Map.Entry<String, Integer> last = map.pollLastEntry();
        // then
        assertNotNull(last, "Returned entry must not be null");
        assertNull(last.getKey(), "The null key should be returned as the last entry");
        assertEquals(99, last.getValue(), "Value associated with null key should be returned");
        assertFalse(map.containsKey(null), "Null key must have been removed");
        assertEquals(2, map.size(), "Map size should be reduced by one");
    }

    /**
     * Subsequent calls continue to respect ordering after a null‑key removal.
     */
    @Test
    void pollLastEntry_SequentialCalls_RespectOrdering() {
        // given
        map.put("alpha", 1);
        map.put("beta", 2);
        // null is last
        map.put(null, 42);
        map.put("gamma", 3);
        // first call – should return null key entry
        Map.Entry<String, Integer> firstPoll = map.pollLastEntry();
        assertNull(firstPoll.getKey(), "First poll should return the null key entry");
        assertEquals(42, firstPoll.getValue());
        // second call – now highest natural key is "gamma"
        Map.Entry<String, Integer> secondPoll = map.pollLastEntry();
        assertEquals("gamma", secondPoll.getKey(), "Second poll should return the highest remaining key");
        assertEquals(3, secondPoll.getValue());
        // map should now contain only "alpha" and "beta"
        assertTrue(map.containsKey("alpha"));
        assertTrue(map.containsKey("beta"));
        assertEquals(2, map.size());
    }

    /**
     * pollLastEntry should work correctly with a custom comparator that reverses order.
     */
    @Test
    void pollLastEntry_WithCustomComparator_UsesComparatorLogic() {
        // given a map that orders strings in reverse (so "a" is considered last)
        ConcurrentNavigableMapNullSafe<String, Integer> reverseMap = new ConcurrentNavigableMapNullSafe<>(Comparator.reverseOrder());
        reverseMap.put("a", 1);
        reverseMap.put("b", 2);
        reverseMap.put("c", 3);
        // when
        Map.Entry<String, Integer> last = reverseMap.pollLastEntry();
        // then – with reverse order, the natural "first" key becomes the last entry
        assertNotNull(last);
        assertEquals("a", last.getKey(), "With reverse comparator, 'a' is the last entry");
        assertEquals(1, last.getValue());
        assertFalse(reverseMap.containsKey("a"));
        assertEquals(2, reverseMap.size());
    }

    /**
     * Single entry map – pollLastEntry returns that entry wrapped by the map.
     */
    @Test
    void pollLastEntry_SingleEntry_ReturnsWrappedEntry() {
        map.put("1", 100);
        Map.Entry<String, Integer> entry = map.pollLastEntry();
        assertNotNull(entry, "Entry must not be null");
        assertEquals("1", entry.getKey());
        assertEquals(100, entry.getValue());
        assertTrue(entry instanceof ConcurrentNavigableMapNullSafe.WrappedEntry, "Returned entry should be a wrapper instance");
        assertTrue(map.isEmpty(), "Map must be empty after polling the only entry");
    }

    /**
     * Verify that the returned entry is a wrapper, not the original internal entry.
     */
    @Test
    void pollLastEntry_ReturnedEntryIsWrapper_NotOriginalInternalEntry() throws Exception {
        // Insert a known entry
        map.put("key", 123);
        // Capture the internal entry *before* it is removed
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentMap<Object, Object> internalMap = (ConcurrentMap<Object, Object>) internalMapField.get(map);
        Map.Entry<Object, Object> internalEntry = internalMap.entrySet().iterator().next();
        // Poll the last entry
        Map.Entry<String, Integer> pollEntry = map.pollLastEntry();
        // The pollEntry must be a different object (wrapper) than the internalEntry
        assertNotSame(internalEntry, pollEntry, "pollLastEntry should return a wrapper, not the original internal map entry");
    }

    /**
     * After pollLastEntry the map size must be decreased by one.
     */
    @Test
    void pollLastEntry_SizeDecreasesByOne() {
        map.put("a", 1);
        map.put("b", 2);
        int beforeSize = map.size();
        Map.Entry<String, Integer> entry = map.pollLastEntry();
        assertNotNull(entry);
        assertEquals(beforeSize - 1, map.size(), "Map size should be reduced by exactly one after pollLastEntry");
    }

    /**
     * Entry with a null value is returned correctly.
     */
    @Test
    void pollLastEntry_NullValue_ReturnsEntryWithNullValue() {
        map.put("10", null);
        Map.Entry<String, Integer> entry = map.pollLastEntry();
        assertNotNull(entry);
        assertEquals("10", entry.getKey());
        assertNull(entry.getValue(), "Value should be null");
    }

    /**
     * After pollLastEntry the underlying entrySet no longer contains the removed entry.
     */
    @Test
    void pollLastEntry_EntrySetConsistency() {
        map.put("x", 5);
        map.put("y", 6);
        Map.Entry<String, Integer> entry = map.pollLastEntry();
        assertNotNull(entry);
        assertFalse(map.containsKey(entry.getKey()), "Key must have been removed");
        assertFalse(map.entrySet().contains(entry), "EntrySet must not contain the removed entry");
    }

    /**
     * Mutating the returned wrapper entry updates the underlying map.
     */
    @Test
    void pollLastEntry_WrapEntryMutability_SetValueUpdatesUnderlyingMap() {
        map.put("k", 1);
        Map.Entry<String, Integer> entry = map.pollLastEntry();
        assertNotNull(entry);
        // mutate via wrapper
        entry.setValue(99);
        // The map is now empty, but we can still query the underlying internal map via reflection
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentMap<Object, Object> internalMap = (ConcurrentMap<Object, Object>) internalMapField.get(map);
        // The internal map should not contain the key any more, but the value change is reflected
        // in the wrapper's state (already verified by entry.getValue()).
        assertEquals(99, entry.getValue(), "Wrapper's setValue should reflect the new value");
    }

    /**
     * Repeated calls return entries in descending order until the map is empty.
     */
    @Test
    void pollLastEntry_MultipleCalls_DescendingOrderUntilEmpty() {
        map.put("1", 10);
        map.put("3", 30);
        map.put("5", 50);
        List<String> expectedKeys = Arrays.asList("5", "3", "1");
        for (String expectedKey : expectedKeys) {
            Map.Entry<String, Integer> e = map.pollLastEntry();
            assertNotNull(e);
            assertEquals(expectedKey, e.getKey());
        }
        // final call – map is empty
        assertNull(map.pollLastEntry(), "After all entries are removed, pollLastEntry should return null");
    }

    /**
     * If the internal map is not a ConcurrentSkipListMap, a ClassCastException is thrown.
     */
    @Test
    void pollLastEntry_ClassCastException_WhenInternalMapNotSkipList() throws Exception {
        // Replace internalMap with a plain HashMap via reflection
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        // not a ConcurrentSkipListMap
        internalMapField.set(map, new ConcurrentHashMap<>());
        assertThrows(ClassCastException.class, () -> map.pollLastEntry(), "Calling pollLastEntry when internalMap is not a ConcurrentSkipListMap should throw ClassCastException");
    }

    /**
     * If the internal map reference is null, a NullPointerException is thrown.
     */
    @Test
    void pollLastEntry_NullPointerException_WhenInternalMapIsNull() throws Exception {
        // Nullify internalMap via reflection
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(map, null);
        assertThrows(NullPointerException.class, () -> map.pollLastEntry(), "Calling pollLastEntry with a null internalMap should throw NullPointerException");
    }
}
