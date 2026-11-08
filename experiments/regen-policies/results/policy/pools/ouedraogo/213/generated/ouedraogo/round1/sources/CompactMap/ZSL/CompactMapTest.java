package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.util.*;

import static org.junit.Assert.*;

public class CompactMapTest {

    private CompactMap<String, Object> emptyMap;

    @Before
    public void setUp() {
        emptyMap = new CompactMap<>();
    }

    /* ------------------------------------------------------------
     * Basic empty map checks
     * ------------------------------------------------------------ */
    @Test
    public void testEmptyMap() {
        assertEquals(0, emptyMap.size());
        assertTrue(emptyMap.isEmpty());
        assertFalse(emptyMap.containsKey("any"));
        assertFalse(emptyMap.containsValue("any"));
        assertNull(emptyMap.get("any"));
        assertEquals(CompactMap.LogicalValueType.EMPTY, emptyMap.getLogicalValueType());

        assertTrue(emptyMap.keySet().isEmpty());
        assertTrue(emptyMap.values().isEmpty());
        assertTrue(emptyMap.entrySet().isEmpty());

        // toString of empty map should be {}
        assertEquals("{}", emptyMap.toString());
    }

    /* ------------------------------------------------------------
     * Single entry – object storage (optimized for singleValueKey)
     * ------------------------------------------------------------ */
    @Test
    public void testSingleEntryObjectStorage() {
        CompactMap<String, Object> map = new CompactMap<>();
        // default single value key is "id"
        map.put("id", "singleValue");

        assertEquals(1, map.size());
        assertFalse(map.isEmpty());
        assertEquals("singleValue", map.get("id"));
        assertTrue(map.containsKey("id"));
        assertTrue(map.containsValue("singleValue"));
        assertEquals(CompactMap.LogicalValueType.OBJECT, map.getLogicalValueType());

        // Overwrite same key – should return previous value
        Object old = map.put("id", "newValue");
        assertEquals("singleValue", old);
        assertEquals("newValue", map.get("id"));
    }

    /* ------------------------------------------------------------
     * Single entry – entry storage (key different from singleValueKey)
     * ------------------------------------------------------------ */
    @Test
    public void testSingleEntryEntryStorage() {
        CompactMap<String, Object> map = new CompactMap<>();
        map.put("foo", 42);

        assertEquals(1, map.size());
        assertEquals(CompactMap.LogicalValueType.ENTRY, map.getLogicalValueType());
        assertEquals(42, map.get("foo"));
        assertTrue(map.containsKey("foo"));
        assertTrue(map.containsValue(42));

        Set<Map.Entry<String, Object>> entries = map.entrySet();
        assertEquals(1, entries.size());
        Map.Entry<String, Object> entry = entries.iterator().next();
        assertEquals("foo", entry.getKey());
        assertEquals(42, entry.getValue());
    }

    /* ------------------------------------------------------------
     * Transition from OBJECT -> ENTRY -> ARRAY -> MAP
     * ------------------------------------------------------------ */
    @Test
    public void testTransitionsThroughAllStates() {
        // Use a small compact size to force transition quickly
        CompactMap<String, Object> map = CompactMap.<String, Object>builder()
                .compactSize(3) // threshold 3 entries
                .build();

        // 1) OBJECT state (singleValueKey)
        map.put("id", "first");
        assertEquals(CompactMap.LogicalValueType.OBJECT, map.getLogicalValueType());
        assertEquals(1, map.size());

        // 2) ENTRY state (different key)
        map.put("a", "second");
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());
        assertEquals(2, map.size());

        // 3) Still ARRAY (still below compactSize)
        map.put("b", "third");
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());
        assertEquals(3, map.size());

        // 4) Exceed compactSize -> MAP state
        map.put("c", "fourth");
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType());
        assertEquals(4, map.size());

        // Verify all entries are present
        assertEquals("first", map.get("id"));
        assertEquals("second", map.get("a"));
        assertEquals("third", map.get("b"));
        assertEquals("fourth", map.get("c"));
    }

    /* ------------------------------------------------------------
     * Case‑insensitive key handling
     * ------------------------------------------------------------ */
    @Test
    public void testCaseInsensitiveKeys() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .caseSensitive(false) // enable case‑insensitivity
                .build();

        map.put("Key", 1);
        assertTrue(map.containsKey("key"));
        assertTrue(map.containsKey("KEY"));
        assertEquals(Integer.valueOf(1), map.get("kEy"));
        assertEquals(CompactMap.LogicalValueType.ENTRY, map.getLogicalValueType());
    }

    /* ------------------------------------------------------------
     * Sorted order iteration
     * ------------------------------------------------------------ */
    @Test
    public void testSortedOrderIteration() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .sortedOrder()
                .compactSize(10)
                .build();

        map.put("b", 2);
        map.put("c", 3);
        map.put("a", 1);

        Iterator<String> it = map.keySet().iterator();
        assertEquals("a", it.next());
        assertEquals("b", it.next());
        assertEquals("c", it.next());
        assertFalse(it.hasNext());

        // Values should follow the same order
        Iterator<Integer> vit = map.values().iterator();
        assertEquals(Integer.valueOf(1), vit.next());
        assertEquals(Integer.valueOf(2), vit.next());
        assertEquals(Integer.valueOf(3), vit.next());
    }

    /* ------------------------------------------------------------
     * Reverse order iteration
     * ------------------------------------------------------------ */
    @Test
    public void testReverseOrderIteration() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .reverseOrder()
                .compactSize(10)
                .build();

        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        Iterator<String> it = map.keySet().iterator();
        assertEquals("c", it.next());
        assertEquals("b", it.next());
        assertEquals("a", it.next());
        assertFalse(it.hasNext());
    }

    /* ------------------------------------------------------------
     * Insertion order iteration
     * ------------------------------------------------------------ */
    @Test
    public void testInsertionOrderIteration() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .insertionOrder()
                .compactSize(10)
                .build();

        map.put("first", 1);
        map.put("second", 2);
        map.put("third", 3);

        Iterator<String> it = map.keySet().iterator();
        assertEquals("first", it.next());
        assertEquals("second", it.next());
        assertEquals("third", it.next());
        assertFalse(it.hasNext());
    }

    /* ------------------------------------------------------------
     * keySet removal and bulk operations
     * ------------------------------------------------------------ */
    @Test
    public void testKeySetOperations() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        Set<String> ks = map.keySet();
        assertTrue(ks.remove("b"));
        assertFalse(map.containsKey("b"));
        assertEquals(2, map.size());

        // removeAll
        ks.removeAll(Arrays.asList("a", "nonexistent"));
        assertFalse(map.containsKey("a"));
        assertTrue(map.containsKey("c"));
        assertEquals(1, map.size());

        // retainAll
        ks.retainAll(Collections.singleton("c"));
        assertTrue(map.containsKey("c"));
        assertEquals(1, map.size());

        // clear via keySet
        ks.clear();
        assertTrue(map.isEmpty());
    }

    /* ------------------------------------------------------------
     * values collection removal
     * ------------------------------------------------------------ */
    @Test
    public void testValuesCollectionRemove() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("x", 10);
        map.put("y", 20);
        map.put("z", 30);

        Collection<Integer> vals = map.values();
        assertTrue(vals.remove(20));
        assertFalse(map.containsValue(20));
        assertEquals(2, map.size());

        // iterator remove
        Iterator<Integer> it = vals.iterator();
        assertTrue(it.hasNext());
        Integer v = it.next();
        it.remove(); // remove first value
        assertEquals(1, map.size());
    }

    /* ------------------------------------------------------------
     * entrySet removal and bulk operations
     * ------------------------------------------------------------ */
    @Test
    public void testEntrySetOperations() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("k1", 100);
        map.put("k2", 200);
        map.put("k3", 300);

        Set<Map.Entry<String, Integer>> es = map.entrySet();
        Map.Entry<String, Integer> e = new AbstractMap.SimpleEntry<>("k2", 200);
        assertTrue(es.remove(e));
        assertFalse(map.containsKey("k2"));
        assertEquals(2, map.size());

        // retainAll – keep only k3
        Set<Map.Entry<String, Integer>> toRetain = new HashSet<>();
        toRetain.add(new AbstractMap.SimpleEntry<>("k3", 300));
        es.retainAll(toRetain);
        assertEquals(1, map.size());
        assertTrue(map.containsKey("k3"));
    }

    /* ------------------------------------------------------------
     * Iterator concurrent‑modification detection
     * ------------------------------------------------------------ */
    @Test(expected = ConcurrentModificationException.class)
    public void testIteratorConcurrentModification() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("one", 1);
        map.put("two", 2);

        Iterator<String> it = map.keySet().iterator();
        // modify map after iterator creation
        map.put("three", 3);
        // next() should detect the modification
        it.next();
    }

    /* ------------------------------------------------------------
     * CompactMapEntry setValue updates the map
     * ------------------------------------------------------------ */
    @Test
    public void testCompactMapEntrySetValue() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("alpha", 10);
        Map.Entry<String, Integer> entry = map.entrySet().iterator().next();
        assertEquals(Integer.valueOf(10), entry.getValue());

        entry.setValue(99);
        assertEquals(Integer.valueOf(99), map.get("alpha"));
    }

    /* ------------------------------------------------------------
     * hashCode and equals consistency
     * ------------------------------------------------------------ */
    @Test
    public void testHashCodeAndEquals() {
        CompactMap<String, Integer> map1 = new CompactMap<>();
        map1.put("a", 1);
        map1.put("b", 2);

        CompactMap<String, Integer> map2 = new CompactMap<>();
        map2.put("b", 2);
        map2.put("a", 1);

        assertEquals(map1, map2);
        assertEquals(map1.hashCode(), map2.hashCode());

        // Different content
        map2.put("c", 3);
        assertNotEquals(map1, map2);
    }

    /* ------------------------------------------------------------
     * toString format verification
     * ------------------------------------------------------------ */
    @Test
    public void testToStringRepresentation() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("x", 5);
        map.put("y", 10);
        String s = map.toString();

        // The string must start with '{' and end with '}'
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        // Contains both entries (order not guaranteed for default unordered)
        assertTrue(s.contains("x=5"));
        assertTrue(s.contains("y=10"));
    }

    /* ------------------------------------------------------------
     * Clear resets to EMPTY state
     * ------------------------------------------------------------ */
    @Test
    public void testClearResetsState() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("k", 1);
        map.put("id", 2); // also triggers other transitions
        assertFalse(map.isEmpty());

        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.getLogicalValueType());
    }

    /* ------------------------------------------------------------
     * withConfig creates a new map with same entries and new config
     * ------------------------------------------------------------ */
    @Test
    public void testWithConfigCreatesNewMap() {
        CompactMap<String, Integer> original = CompactMap.<String, Integer>builder()
                .compactSize(4)
                .caseSensitive(false)
                .sortedOrder()
                .build();

        original.put("A", 1);
        original.put("b", 2);
        original.put("C", 3);

        Map<String, Object> cfg = original.getConfig();
        cfg.put(CompactMap.COMPACT_SIZE, 2); // change compact size

        CompactMap<String, Integer> copy = original.withConfig(cfg);
        assertEquals(3, copy.size());
        assertEquals(Integer.valueOf(1), copy.get("A"));
        assertEquals(Integer.valueOf(2), copy.get("b"));
        assertEquals(Integer.valueOf(3), copy.get("C"));

        // New config should be reflected
        assertEquals(2, copy.compactSize());
        assertFalse(copy.isCaseInsensitive()); // caseSensitive default true
        assertEquals(CompactMap.SORTED, copy.getOrdering());
    }
}
