package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.stream.IntStream;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class CompactMap_clear_Test {

    /* -------------------------------------------------------------
     *  Helper to create a CompactMap with a given compactSize and
     *  backing map implementation.
     * ------------------------------------------------------------- */
    private <K, V> CompactMap<K, V> newMap(final int compactSize, final Class<? extends Map> backingMapClass) {
        return new CompactMap<K, V>() {

            @Override
            protected int compactSize() {
                return compactSize;
            }

            @Override
            protected Map<K, V> getNewMap() {
                try {
                    return backingMapClass.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    /**
     * Common assertions that a map is in the “freshly created” empty state.
     */
    private void assertEmptyState(CompactMap<?, ?> map) {
        assertTrue(map.isEmpty(), "Map must be empty");
        assertEquals(0, map.size(), "Size must be 0");
        assertNull(map.get("any"), "get() must return null for any key");
        assertFalse(map.containsKey("any"), "containsKey() must be false");
        assertFalse(map.containsValue("any"), "containsValue() must be false");
        assertTrue(map.entrySet().isEmpty(), "entrySet() must be empty");
        assertTrue(map.values().isEmpty(), "values() collection must be empty");
    }

    /* -------------------------------------------------------------
     *  1. clear() on an already empty map
     * ------------------------------------------------------------- */
    @Test
    void clearOnEmptyMap() {
        CompactMap<String, Object> map = newMap(4, LinkedHashMap.class);
        assertTrue(map.isEmpty(), "Map should start empty");
        assertEquals(0, map.size());
        // invoke clear()
        map.clear();
        // verify state is still empty
        assertTrue(map.isEmpty(), "Map should remain empty after clear()");
        assertEquals(0, map.size());
        assertNull(map.get("any"));
    }

    /* -------------------------------------------------------------
     *  2. clear() after a single entry has been added
     * ------------------------------------------------------------- */
    @Test
    void clearAfterSingleEntry() {
        CompactMap<String, Integer> map = newMap(4, LinkedHashMap.class);
        map.put("one", 1);
        assertEquals(1, map.size());
        assertFalse(map.isEmpty());
        assertEquals(1, map.get("one"));
        map.clear();
        assertTrue(map.isEmpty(), "Map must be empty after clear()");
        assertEquals(0, map.size());
        assertNull(map.get("one"));
        assertFalse(map.containsKey("one"));
    }

    /* -------------------------------------------------------------
     *  3. clear() when the map is in the compact‑array state
     *     (size < compactSize, but > 1)
     * ------------------------------------------------------------- */
    @Test
    void clearFromCompactArrayState() {
        // compactSize = 5, we will add 3 entries → compact‑array storage
        CompactMap<String, String> map = newMap(5, LinkedHashMap.class);
        map.put("a", "A");
        map.put("b", "B");
        map.put("c", "C");
        assertEquals(3, map.size());
        assertTrue(map.containsKey("b"));
        assertEquals("B", map.get("b"));
        map.clear();
        assertTrue(map.isEmpty(), "Map should be empty after clear()");
        assertEquals(0, map.size());
        assertNull(map.get("a"));
        assertFalse(map.containsKey("c"));
    }

    /* -------------------------------------------------------------
     *  4. clear() when the map has transitioned to a backing map
     *     (size > compactSize)
     * ------------------------------------------------------------- */
    @Test
    void clearFromBackingMapState() {
        // compactSize = 2, inserting 4 entries forces transition to backing map
        CompactMap<Integer, Integer> map = newMap(2, LinkedHashMap.class);
        map.put(1, 10);
        map.put(2, 20);
        map.put(3, 30);
        map.put(4, 40);
        assertEquals(4, map.size());
        assertTrue(map.containsKey(3));
        assertEquals(30, map.get(3));
        map.clear();
        assertTrue(map.isEmpty(), "Map must be empty after clear()");
        assertEquals(0, map.size());
        assertNull(map.get(2));
        assertFalse(map.containsKey(4));
    }

    /* -------------------------------------------------------------
     *  5. After clear(), the map should behave like a fresh instance
     * ------------------------------------------------------------- */
    @Test
    void clearThenReuseMap() {
        CompactMap<String, Double> map = newMap(3, LinkedHashMap.class);
        map.put("x", 1.1);
        map.put("y", 2.2);
        map.clear();
        // reuse the map
        map.put("z", 3.3);
        assertEquals(1, map.size());
        assertTrue(map.containsKey("z"));
        assertEquals(3.3, map.get("z"));
        assertFalse(map.isEmpty());
    }

    /* -------------------------------------------------------------
     *  6. clear() on a case‑insensitive map
     * ------------------------------------------------------------- */
    @Test
    void clearCaseInsensitiveMap() {
        CompactMap<String, String> map = CompactMap.<String, String>builder().caseSensitive(false).build();
        map.put("Key", "value1");
        // should replace the previous entry
        map.put("key", "value2");
        assertEquals(1, map.size());
        assertTrue(map.containsKey("KEY"));
        assertEquals("value2", map.get("kEy"));
        map.clear();
        assertEmptyState(map);
        // after clear the map must still be case‑insensitive
        map.put("Another", "v");
        assertTrue(map.containsKey("another"));
    }

    /* -------------------------------------------------------------
     *  7. clear() after using SINGLE_KEY optimisation
     * ------------------------------------------------------------- */
    @Test
    void clearSingleKeyMode() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().singleValueKey("id").build();
        map.put("id", 42);
        assertEquals(1, map.size());
        assertTrue(map.containsKey("id"));
        assertEquals(42, map.get("id"));
        map.clear();
        assertEmptyState(map);
        // verify that SINGLE_KEY restriction is still enforced
        map.put("id", 99);
        assertEquals(1, map.size());
        assertEquals(99, map.get("id"));
        // a different key must not be accepted (implementation may ignore or replace)
        map.put("other", 1);
        assertFalse(map.containsKey("other"));
    }

    /* -------------------------------------------------------------
     *  8. clear() on a sorted‑ordering map
     * ------------------------------------------------------------- */
    @Test
    void clearSortedOrderingMap() {
        CompactMap<Integer, String> map = CompactMap.<Integer, String>builder().sortedOrder().build();
        map.put(5, "five");
        map.put(2, "two");
        map.put(8, "eight");
        // verify ordering via keySet iterator
        Iterator<Integer> it = map.keySet().iterator();
        assertEquals(2, it.next());
        assertEquals(5, it.next());
        assertEquals(8, it.next());
        map.clear();
        assertEmptyState(map);
        // ordering must still be sorted after reuse
        map.put(3, "three");
        map.put(1, "one");
        List<Integer> keys = new ArrayList<>(map.keySet());
        assertEquals(List.of(1, 3), keys);
    }

    /* -------------------------------------------------------------
     *  9. clear() after inserting null key and/or null value
     * ------------------------------------------------------------- */
    @Test
    void clearWithNullKeyAndValue() {
        CompactMap<String, String> map = newMap(4, LinkedHashMap.class);
        map.put(null, "nullKey");
        map.put("nullValue", null);
        // overwrite previous entry
        map.put(null, null);
        assertEquals(2, map.size());
        assertTrue(map.containsKey(null));
        assertTrue(map.containsValue(null));
        assertNull(map.get("nullValue"));
        assertNull(map.get(null));
        map.clear();
        assertEmptyState(map);
        // ensure nulls can be added again after clear
        map.put(null, "again");
        assertEquals(1, map.size());
        assertEquals("again", map.get(null));
    }

    /* -------------------------------------------------------------
     * 10. clear() after a large number of entries (stress test)
     * ------------------------------------------------------------- */
    @Test
    void clearAfterLargeNumberOfEntries() {
        // Use a relatively small compactSize to force backing‑map usage early
        CompactMap<Integer, Integer> map = newMap(50, LinkedHashMap.class);
        int entryCount = 10_000;
        IntStream.range(0, entryCount).forEach(i -> map.put(i, i * 2));
        assertEquals(entryCount, map.size());
        assertTrue(map.containsKey(entryCount - 1));
        assertEquals((entryCount - 1) * 2, map.get(entryCount - 1));
        map.clear();
        assertEmptyState(map);
    }

    /* -------------------------------------------------------------
     * 11. clear() invoked twice consecutively on a non‑empty map
     * ------------------------------------------------------------- */
    @Test
    void doubleClearOnNonEmptyMap() {
        CompactMap<String, String> map = newMap(4, LinkedHashMap.class);
        map.put("a", "A");
        map.put("b", "B");
        // first clear
        map.clear();
        assertEmptyState(map);
        // second clear should be a no‑op but must not throw
        assertDoesNotThrow(map::clear);
        assertEmptyState(map);
    }

    /* -------------------------------------------------------------
     * 12. clear() after a sequence of adds and removes before clear
     * ------------------------------------------------------------- */
    @Test
    void clearAfterAddsAndRemoves() {
        CompactMap<String, Integer> map = newMap(4, LinkedHashMap.class);
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        map.remove("two");
        map.put("four", 4);
        map.remove("one");
        // map now contains "three" and "four"
        assertEquals(2, map.size());
        assertTrue(map.containsKey("three"));
        assertTrue(map.containsKey("four"));
        map.clear();
        assertEmptyState(map);
    }

    /* -------------------------------------------------------------
     * 13. clear() then reuse map (size/emptiness checks after reuse)
     * ------------------------------------------------------------- */
    @Test
    void clearThenReuseMap_WithDetailedChecks() {
        CompactMap<String, String> map = newMap(3, LinkedHashMap.class);
        map.put("first", "1");
        map.put("second", "2");
        map.clear();
        // after clear
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
        // reuse
        map.put("reused", "value");
        assertEquals(1, map.size(), "size after put should be 1");
        assertFalse(map.isEmpty(), "map should not be empty after put");
        assertTrue(map.containsKey("reused"));
        assertTrue(map.containsValue("value"));
        assertEquals("value", map.get("reused"));
    }
}
