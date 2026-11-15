package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;

class ConcurrentNavigableMapNullSafe_firstEntry_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh map for each test case
        map = new ConcurrentNavigableMapNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the pattern consistent
        map = null;
    }

    @Test
    void firstEntry_returnsEntryWithLowestKey_whenMapHasMultipleKeys() {
        // given
        map.put("b", 2);
        map.put("a", 1);
        map.put("c", 3);
        // null is considered *last* in the natural ordering
        map.put(null, 100);
        // when
        Map.Entry<String, Integer> first = map.firstEntry();
        // then
        assertNotNull(first, "firstEntry() should not be null when the map contains non‑null keys");
        assertEquals("a", first.getKey(), "The smallest (lexicographically) key should be returned");
        assertEquals(1, first.getValue());
    }

    @Test
    void firstEntry_returnsNull_whenMapIsEmpty() {
        // given – an empty map (setupBeforeEach already provides this)
        // when
        Map.Entry<String, Integer> first = map.firstEntry();
        // then
        assertNull(first, "firstEntry() must return null for an empty map");
    }

    @Test
    void firstEntry_returnsNull_whenOnlyNullKeyIsPresent() {
        // given
        map.put(null, 42);
        // when
        Map.Entry<String, Integer> first = map.firstEntry();
        // then
        // According to the class contract, null keys are ordered last.
        // Therefore, with only a null key present, there is no “first” entry.
        assertNull(first, "firstEntry() should return null when the only entry has a null key");
    }

    @Test
    void firstEntry_doesNotExposeInternalEntryObject() {
        // given
        map.put("x", 10);
        map.put("y", 20);
        // when
        Map.Entry<String, Integer> first = map.firstEntry();
        // then
        // The returned entry must be a wrapper; mutating it should affect the map.
        assertEquals("x", first.getKey());
        assertEquals(10, first.getValue());
        // mutate via the entry
        first.setValue(99);
        assertEquals(99, map.get("x"), "Changing the value through the entry should reflect in the map");
    }

    @Test
    void firstEntry_returnsEntry_whenSingleNonNullEntry() {
        // given
        map.put("apple", 10);
        // when
        Map.Entry<String, Integer> first = map.firstEntry();
        // then
        assertNotNull(first);
        assertEquals("apple", first.getKey());
        assertEquals(10, first.getValue());
    }

    @Test
    void firstEntry_returnsSmallestKey_amongMultipleEntries() {
        // given
        map.put("beta", 2);
        map.put("alpha", 1);
        map.put("gamma", 3);
        // when
        Map.Entry<String, Integer> first = map.firstEntry();
        // then
        assertNotNull(first);
        assertEquals("alpha", first.getKey());
        assertEquals(1, first.getValue());
    }

    @Test
    void firstEntry_ignoresNullKey_whenMixedKeysPresent() {
        // given
        map.put(null, 42);
        map.put("banana", 20);
        map.put("cherry", 30);
        // when
        Map.Entry<String, Integer> first = map.firstEntry();
        // then
        assertNotNull(first);
        assertEquals("banana", first.getKey());
        assertEquals(20, first.getValue());
    }

    @Test
    void firstEntry_respectsCaseSensitiveOrdering() {
        // given
        map.put("A", 100);
        map.put("a", 5);
        map.put("b", 10);
        // when
        Map.Entry<String, Integer> first = map.firstEntry();
        // then
        assertNotNull(first);
        // In natural String ordering, lower‑case 'a' > upper‑case 'A', so 'A' is smallest
        assertEquals("A", first.getKey());
        assertEquals(100, first.getValue());
    }

    @Test
    void firstEntry_numericKeys_smallestIntegerReturned() {
        // This test uses a map with Integer keys via raw type casting because the
        // generic type of the wrapper is <String,Integer>. We create a separate
        // instance with Integer keys using reflection.
        ConcurrentNavigableMapNullSafe<Integer, Integer> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(-10, -10);
        intMap.put(0, 0);
        intMap.put(5, 5);
        Map.Entry<Integer, Integer> first = intMap.firstEntry();
        assertNotNull(first);
        assertEquals(-10, first.getKey());
        assertEquals(-10, first.getValue());
    }

    @Test
    void firstEntry_throwsClassCastException_whenInternalMapNotSkipList() throws Exception {
        // Replace the internalMap with a ConcurrentHashMap (does not implement firstEntry())
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(map, new ConcurrentHashMap<>());
        assertThrows(ClassCastException.class, () -> map.firstEntry(), "Casting a non‑ConcurrentSkipListMap internal map should throw ClassCastException");
    }

    @Test
    void firstEntry_throwsNullPointerException_whenInternalMapIsNull() throws Exception {
        // Set internalMap to null via reflection
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(map, null);
        assertThrows(NullPointerException.class, () -> map.firstEntry(), "Calling firstEntry() with a null internalMap should throw NullPointerException");
    }

    @Test
    void firstEntry_throwsClassCastException_whenIncomparableMixedKeyTypes() throws Exception {
        // Create a raw ConcurrentSkipListMap and insert mixed key types
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentSkipListMap rawMap = new ConcurrentSkipListMap();
        rawMap.put("string", 1);
        // Integer key – incomparable with String
        rawMap.put(1, 2);
        internalMapField.set(map, rawMap);
        assertThrows(ClassCastException.class, () -> map.firstEntry(), "Mixed incomparable key types should cause ClassCastException on firstEntry()");
    }

    @Test
    void firstEntry_propagatesExceptionFromFaultyComparator() throws Exception {
        // Comparator that always throws IllegalArgumentException
        Comparator<Object> badComparator = (o1, o2) -> {
            throw new IllegalArgumentException("Comparator failure");
        };
        ConcurrentSkipListMap<Object, Object> badMap = new ConcurrentSkipListMap<>(badComparator);
        badMap.put("a", 1);
        badMap.put("b", 2);
        // Inject the faulty map
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(map, badMap);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> map.firstEntry(), "Exception thrown by comparator should propagate");
        assertEquals("Comparator failure", ex.getMessage());
    }
}
