package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.ConcurrentModificationException;

import org.junit.Test;

/**
 * Comprehensive JUnit 4 test suite for {@link CompactMap}.
 * Covers all public behaviours, internal state transitions,
 * ordering options, case‑sensitivity, iterator semantics and
 * configuration utilities.
 */
public class CompactMapTest {

    /* --------------------------------------------------------------------- */
    /*  Helper methods                                                       */
    /* --------------------------------------------------------------------- */

    /** Creates a map with a tiny compact size to force transitions quickly. */
    private CompactMap<String, Integer> createSmallCompactMap(int compactSize) {
        return CompactMap.<String, Integer>builder()
                .compactSize(compactSize)
                .mapType(HashMap.class)   // explicit to avoid defaults
                .build();
    }

    /** Returns a list of keys from the map's keySet iterator. */
    private List<String> iterateKeys(CompactMap<String, Integer> map) {
        List<String> list = new ArrayList<>();
        for (String k : map.keySet()) {
            list.add(k);
        }
        return list;
    }

    /** Returns a list of entries from the map's entrySet iterator. */
    private List<Map.Entry<String, Integer>> iterateEntries(CompactMap<String, Integer> map) {
        List<Map.Entry<String, Integer>> list = new ArrayList<>();
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            list.add(e);
        }
        return list;
    }

    /** Returns a list of values from the map's values iterator. */
    private List<Integer> iterateValues(CompactMap<String, Integer> map) {
        List<Integer> list = new ArrayList<>();
        for (Integer v : map.values()) {
            list.add(v);
        }
        return list;
    }

    /* --------------------------------------------------------------------- */
    /*  1. Empty map basics                                                  */
    /* --------------------------------------------------------------------- */

    @Test
    public void testEmptyMapBehavior() {
        CompactMap<String, Integer> map = new CompactMap<>();

        assertTrue("Map should be empty", map.isEmpty());
        assertEquals(0, map.size());
        assertNull(map.get("any"));
        assertFalse(map.containsKey("any"));
        assertFalse(map.containsValue(1));
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.getLogicalValueType());

        // equals and hashCode with another empty map
        CompactMap<String, Integer> other = new CompactMap<>();
        assertTrue(map.equals(other));
        assertEquals(0, map.hashCode());

        // equals with a standard empty HashMap
        assertTrue(map.equals(new HashMap<>()));
        assertTrue(new HashMap<>().equals(map));

        // toString should be empty braces
        assertEquals("{}", map.toString());

        // config defaults
        Map<String, Object> cfg = map.getConfig();
        assertEquals(CompactMap.DEFAULT_COMPACT_SIZE, cfg.get(CompactMap.COMPACT_SIZE));
        assertEquals(true, cfg.get(CompactMap.CASE_SENSITIVE));
        assertEquals(CompactMap.UNORDERED, cfg.get(CompactMap.ORDERING));
        assertEquals(CompactMap.DEFAULT_SINGLE_KEY, cfg.get(CompactMap.SINGLE_KEY));
        assertEquals(HashMap.class, cfg.get(CompactMap.MAP_TYPE));
    }

    /* --------------------------------------------------------------------- */
    /*  2. Single‑entry optimized storage (OBJECT)                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testSingleEntryOptimizedStorage() {
        CompactMap<String, String> map = new CompactMap<>();

        // key matches the default singleValueKey ("id")
        String old = map.put("id", "value");
        assertNull(old);
        assertEquals(1, map.size());
        assertEquals("value", map.get("id"));
        assertTrue(map.containsKey("id"));
        assertTrue(map.containsValue("value"));
        assertEquals(CompactMap.LogicalValueType.OBJECT, map.getLogicalValueType());

        // Overwrite the same key
        String prev = map.put("id", "new");
        assertEquals("value", prev);
        assertEquals("new", map.get("id"));
        assertEquals(CompactMap.LogicalValueType.OBJECT, map.getLogicalValueType());

        // Remove the entry – map should become empty again
        String removed = map.remove("id");
        assertEquals("new", removed);
        assertTrue(map.isEmpty());
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.getLogicalValueType());
    }

    /* --------------------------------------------------------------------- */
    /*  3. Single‑entry via CompactMapEntry (ENTRY)                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testSingleEntryStandardStorage() {
        CompactMap<String, Integer> map = new CompactMap<>();

        // Use a key that is NOT the singleValueKey
        map.put("foo", 42);
        assertEquals(1, map.size());
        assertEquals(Integer.valueOf(42), map.get("foo"));
        assertEquals(CompactMap.LogicalValueType.ENTRY, map.getLogicalValueType());

        // Retrieve the entry via entrySet and mutate it using setValue
        Map.Entry<String, Integer> entry = map.entrySet().iterator().next();
        assertTrue(entry instanceof CompactMap.CompactMapEntry);
        Integer old = entry.setValue(100);
        assertEquals(Integer.valueOf(42), old);
        assertEquals(Integer.valueOf(100), map.get("foo"));
        assertEquals(Integer.valueOf(100), entry.getValue());

        // Remove using keySet.remove()
        boolean removed = map.keySet().remove("foo");
        assertTrue(removed);
        assertTrue(map.isEmpty());
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.getLogicalValueType());
    }

    /* --------------------------------------------------------------------- */
    /*  4. Compact array storage (ARRAY) – transition to single entry        */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCompactArrayStorageAndTransition() {
        CompactMap<String, Integer> map = createSmallCompactMap(5); // compactSize = 5

        // Insert two distinct keys – should move to ARRAY state
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(2, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());

        // Verify contains/lookup
        assertTrue(map.containsKey("a"));
        assertTrue(map.containsKey("b"));
        assertEquals(Integer.valueOf(1), map.get("a"));
        assertEquals(Integer.valueOf(2), map.get("b"));

        // Remove one key – should transition back to single entry (ENTRY)
        Integer removed = map.remove("a");
        assertEquals(Integer.valueOf(1), removed);
        assertEquals(1, map.size());
        assertEquals(CompactMap.LogicalValueType.ENTRY, map.getLogicalValueType());
        assertEquals(Integer.valueOf(2), map.get("b"));
    }

    /* --------------------------------------------------------------------- */
    /*  5. Transition to backing Map when size exceeds compactSize         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testTransitionToBackingMapAndBack() {
        CompactMap<String, Integer> map = createSmallCompactMap(3); // threshold = 3

        // Fill up to compactSize (3 entries) – still ARRAY
        map.put("k1", 10);
        map.put("k2", 20);
        map.put("k3", 30);
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());

        // Add one more – triggers transition to MAP
        map.put("k4", 40);
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType());
        assertEquals(4, map.size());

        // Verify map semantics
        assertTrue(map.containsKey("k2"));
        assertEquals(Integer.valueOf(30), map.get("k3"));

        // Remove entries until size equals compactSize (3) – should go back to ARRAY
        map.remove("k1");
        assertEquals(3, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());

        // Ensure remaining entries are intact
        assertTrue(map.containsKey("k2"));
        assertTrue(map.containsKey("k3"));
        assertTrue(map.containsKey("k4"));
    }

    /* --------------------------------------------------------------------- */
    /*  6. Case‑insensitive map with sorted order                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCaseInsensitiveSortedMap() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .caseSensitive(false)
                .sortedOrder()
                .compactSize(10)
                .build();

        map.put("Apple", 1);
        map.put("banana", 2);
        map.put("APPLE", 3); // should replace "Apple" (case‑insensitive)

        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(3), map.get("apple"));
        assertEquals(Integer.valueOf(2), map.get("BANANA"));

        // Verify iteration order is natural sorted (case‑insensitive)
        List<String> keys = iterateKeys(map);
        assertEquals(Arrays.asList("APPLE", "banana"), keys); // "APPLE" (stored key) comes before "banana"

        // Comparator behavior directly
        CompactMap.CompactMapComparator comp = new CompactMap.CompactMapComparator(true, false);
        assertEquals(0, comp.compare("Key", "key"));
        assertTrue(comp.compare("a", "b") < 0);
        assertTrue(comp.compare("b", "a") > 0);
    }

    /* --------------------------------------------------------------------- */
    /*  7. Insertion order map                                                */
    /* --------------------------------------------------------------------- */

    @Test
    public void testInsertionOrderPreserved() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .insertionOrder()
                .compactSize(10)
                .build();

        map.put("first", 1);
        map.put("second", 2);
        map.put("third", 3);

        List<String> keys = iterateKeys(map);
        assertEquals(Arrays.asList("first", "second", "third"), keys);
    }

    /* --------------------------------------------------------------------- */
    /*  8. Reverse order map                                                  */
    /* --------------------------------------------------------------------- */

    @Test
    public void testReverseOrderSorting() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .reverseOrder()
                .compactSize(10)
                .build();

        map.put("a", 1);
        map.put("c", 3);
        map.put("b", 2);

        // Expected reverse natural order: c, b, a
        List<String> keys = iterateKeys(map);
        assertEquals(Arrays.asList("c", "b", "a"), keys);
    }

    /* --------------------------------------------------------------------- */
    /*  9. Collection view removals & iterator remove behavior                */
    /* --------------------------------------------------------------------- */

    @Test
    public void testKeySetAndValuesRemoval() {
        CompactMap<String, Integer> map = createSmallCompactMap(5);
        map.put("x", 10);
        map.put("y", 20);
        map.put("z", 30);

        // Remove via keySet
        assertTrue(map.keySet().remove("y"));
        assertFalse(map.containsKey("y"));
        assertEquals(2, map.size());

        // Remove via values collection (removes first matching entry)
        assertTrue(map.values().remove(30));
        assertFalse(map.containsValue(30));
        assertEquals(1, map.size());

        // EntrySet removal
        map.put("a", 100);
        Map.Entry<String, Integer> entry = map.entrySet().iterator().next();
        assertTrue(map.entrySet().remove(entry));
        assertEquals(1, map.size()); // one entry removed, one remains
    }

    @Test
    public void testIteratorRemoveAndConcurrentModification() {
        CompactMap<String, Integer> map = createSmallCompactMap(5);
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);

        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        assertTrue(it.hasNext());
        Map.Entry<String, Integer> e = it.next();
        assertEquals("one", e.getKey());

        // Remove via iterator
        it.remove();
        assertFalse(map.containsKey("one"));
        assertEquals(2, map.size());

        // Concurrent modification detection
        Iterator<String> keyIt = map.keySet().iterator();
        assertTrue(keyIt.hasNext());
        // Modify map directly
        map.put("four", 4);
        try {
            keyIt.next(); // should throw
            fail("Expected ConcurrentModificationException");
        } catch (ConcurrentModificationException expected) {
            // expected
        }
    }

    /* --------------------------------------------------------------------- */
    /* 10. equals / hashCode with different map implementations               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCodeConsistency() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("a", 1);
        map.put("b", 2);

        // Standard HashMap with same entries
        Map<String, Integer> std = new HashMap<>();
        std.put("a", 1);
        std.put("b", 2);
        assertTrue(map.equals(std));
        assertTrue(std.equals(map));
        assertEquals(map.hashCode(), std.hashCode());

        // Different entry
        std.put("c", 3);
        assertFalse(map.equals(std));
    }

    /* --------------------------------------------------------------------- */
    /* 11. withConfig creates a new map preserving entries but with new cfg */
    /* --------------------------------------------------------------------- */

    @Test
    public void testWithConfigCreatesNewMapWithModifiedSettings() {
        // Original map: default config, unordered
        CompactMap<String, Integer> original = new CompactMap<>();
        original.put("id", 99); // uses optimized OBJECT storage
        original.put("alpha", 1);
        original.put("beta", 2);

        Map<String, Object> newConfig = new HashMap<>(original.getConfig());
        newConfig.put(CompactMap.ORDERING, CompactMap.SORTED);
        newConfig.put(CompactMap.CASE_SENSITIVE, false);
        newConfig.put(CompactMap.COMPACT_SIZE, 4); // small to stay in ARRAY

        CompactMap<String, Integer> newMap = original.withConfig(newConfig);

        // Verify entries are preserved
        assertEquals(3, newMap.size());
        assertEquals(Integer.valueOf(99), newMap.get("id"));
        assertEquals(Integer.valueOf(1), newMap.get("alpha"));
        assertEquals(Integer.valueOf(2), newMap.get("beta"));

        // Verify new configuration took effect
        Map<String, Object> cfg = newMap.getConfig();
        assertEquals(CompactMap.SORTED, cfg.get(CompactMap.ORDERING));
        assertEquals(false, cfg.get(CompactMap.CASE_SENSITIVE));
        assertEquals(4, cfg.get(CompactMap.COMPACT_SIZE));

        // Sorted order should be reflected in iteration
        List<String> keys = iterateKeys(newMap);
        assertEquals(Arrays.asList("alpha", "beta", "id"), keys);
    }

    /* --------------------------------------------------------------------- */
    /* 12. Direct tests of CompactMapComparator                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCompactMapComparatorBehaviour() {
        // Case‑sensitive, natural order
        CompactMap.CompactMapComparator comp = new CompactMap.CompactMapComparator(false, false);
        assertTrue(comp.compare("a", "b") < 0);
        assertTrue(comp.compare("b", "a") > 0);
        assertEquals(0, comp.compare("same", "same"));

        // Case‑insensitive
        CompactMap.CompactMapComparator ci = new CompactMap.CompactMapComparator(true, false);
        assertEquals(0, ci.compare("Key", "key"));
        assertTrue(ci.compare("a", "B") < 0);

        // Reverse order
        CompactMap.CompactMapComparator rev = new CompactMap.CompactMapComparator(false, true);
        assertTrue(rev.compare("a", "b") > 0);
        assertTrue(rev.compare("b", "a") < 0);
    }

    /* --------------------------------------------------------------------- */
    /* 13. toString reflects current entries                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToStringRepresentation() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("x", 1);
        map.put("y", 2);
        String s = map.toString();
        // Order is unspecified for unordered map; verify content
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        assertTrue(s.contains("x=1"));
        assertTrue(s.contains("y=2"));
    }
}
