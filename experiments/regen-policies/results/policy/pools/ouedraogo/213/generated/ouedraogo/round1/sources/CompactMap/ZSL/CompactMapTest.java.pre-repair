package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class CompactMapTest {

    private CompactMap<String, Integer> defaultMap;

    @Before
    public void setUp() {
        defaultMap = new CompactMap<>();
    }

    /* --------------------------------------------------------------
     * Basic map contract tests (size, isEmpty, clear, toString)
     * -------------------------------------------------------------- */
    @Test
    public void testEmptyMapContract() {
        assertTrue(defaultMap.isEmpty());
        assertEquals(0, defaultMap.size());
        assertEquals("{}", defaultMap.toString());
        assertEquals(CompactMap.LogicalValueType.EMPTY, defaultMap.getLogicalValueType());

        defaultMap.clear(); // should be a no‑op
        assertTrue(defaultMap.isEmpty());
    }

    @Test
    public void testSingleEntryAndSingleValueKeyOptimization() {
        // The default single‑value key is "id"
        defaultMap.put("id", 42);
        assertEquals(1, defaultMap.size());
        assertEquals(Integer.valueOf(42), defaultMap.get("id"));
        assertEquals(CompactMap.LogicalValueType.OBJECT, defaultMap.getLogicalValueType());

        // Adding a second entry should transition to compact array storage
        defaultMap.put("other", 99);
        assertEquals(2, defaultMap.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, defaultMap.getLogicalValueType());
        assertTrue(defaultMap.containsKey("id"));
        assertTrue(defaultMap.containsKey("other"));
    }

    @Test
    public void testContainsKeyAndContainsValue() {
        defaultMap.put("a", 1);
        defaultMap.put("b", 2);
        assertTrue(defaultMap.containsKey("a"));
        assertTrue(defaultMap.containsValue(2));
        assertFalse(defaultMap.containsKey("c"));
        assertFalse(defaultMap.containsValue(3));
    }

    @Test
    public void testRemoveAndTransitionBackToSingleEntry() {
        defaultMap.put("x", 10);
        defaultMap.put("y", 20);
        assertEquals(CompactMap.LogicalValueType.ARRAY, defaultMap.getLogicalValueType());

        // Remove one entry – should leave a single entry (array -> single)
        Integer removed = defaultMap.remove("y");
        assertEquals(Integer.valueOf(20), removed);
        assertEquals(1, defaultMap.size());
        assertEquals(CompactMap.LogicalValueType.ENTRY, defaultMap.getLogicalValueType());

        // Remove last entry – map becomes empty again
        defaultMap.remove("x");
        assertTrue(defaultMap.isEmpty());
        assertEquals(CompactMap.LogicalValueType.EMPTY, defaultMap.getLogicalValueType());
    }

    @Test
    public void testEqualsAndHashCode() {
        Map<String, Integer> other = new HashMap<>();
        other.put("a", 1);
        other.put("b", 2);

        defaultMap.putAll(other);
        assertEquals(other, defaultMap);
        assertEquals(other.hashCode(), defaultMap.hashCode());

        // Change a value – maps should no longer be equal
        defaultMap.put("b", 99);
        assertNotEquals(other, defaultMap);
    }

    /* --------------------------------------------------------------
     * Builder based maps – compact size, ordering and case sensitivity
     * -------------------------------------------------------------- */
    @Test
    public void testBuilderWithSmallCompactSizeTriggersMapState() {
        CompactMap<String, String> map = CompactMap.<String, String>builder()
                .compactSize(3)          // tiny threshold
                .build();

        // 3 entries -> still array
        map.put("k1", "v1");
        map.put("k2", "v2");
        map.put("k3", "v3");
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());

        // 4th entry forces transition to backing map
        map.put("k4", "v4");
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType());
        assertEquals(4, map.size());
    }

    @Test
    public void testInsertionOrderPreserved() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .insertionOrder()
                .compactSize(5)
                .build();

        map.put("first", 1);
        map.put("second", 2);
        map.put("third", 3);

        List<String> keys = new ArrayList<>();
        for (String k : map.keySet()) {
            keys.add(k);
        }
        assertEquals(Arrays.asList("first", "second", "third"), keys);
    }

    @Test
    public void testSortedOrder() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .sortedOrder()
                .compactSize(5)
                .build();

        map.put("b", 2);
        map.put("c", 3);
        map.put("a", 1);

        List<String> keys = new ArrayList<>();
        for (String k : map.keySet()) {
            keys.add(k);
        }
        // natural (String) ordering
        assertEquals(Arrays.asList("a", "b", "c"), keys);
    }

    @Test
    public void testReverseOrder() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .reverseOrder()
                .compactSize(5)
                .build();

        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        List<String> keys = new ArrayList<>();
        for (String k : map.keySet()) {
            keys.add(k);
        }
        assertEquals(Arrays.asList("c", "b", "a"), keys);
    }

    @Test
    public void testCaseInsensitiveKeys() {
        CompactMap<String, String> map = CompactMap.<String, String>builder()
                .caseSensitive(false)   // case‑insensitive
                .sortedOrder()
                .compactSize(5)
                .build();

        map.put("Key", "value1");
        // different case should be considered the same key
        assertTrue(map.containsKey("key"));
        assertEquals("value1", map.get("KEY"));

        // putting a different case updates the same entry
        map.put("kEy", "value2");
        assertEquals(1, map.size());
        assertEquals("value2", map.get("key"));
    }

    /* --------------------------------------------------------------
     * Collection view tests (keySet, values, entrySet)
     * -------------------------------------------------------------- */
    @Test
    public void testKeySetRemoveAndClear() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .compactSize(5)
                .build();

        map.put("x", 1);
        map.put("y", 2);
        map.put("z", 3);

        Set<String> ks = map.keySet();
        assertTrue(ks.remove("y"));
        assertFalse(ks.contains("y"));
        assertEquals(2, map.size());

        ks.clear();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testValuesCollectionRemove() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .compactSize(5)
                .build();

        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);

        Collection<Integer> vals = map.values();
        assertTrue(vals.remove(2));
        assertFalse(map.containsValue(2));
        assertEquals(2, map.size());

        // iterator remove
        Iterator<Integer> it = vals.iterator();
        assertTrue(it.hasNext());
        it.next();
        it.remove(); // remove first value
        assertEquals(1, map.size());
    }

    @Test
    public void testEntrySetRemoveAllAndRetainAll() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .compactSize(5)
                .build();

        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);

        Set<Map.Entry<String, Integer>> entrySet = map.entrySet();

        // removeAll via a collection of entries
        List<Map.Entry<String, Integer>> toRemove = new ArrayList<>();
        toRemove.add(new AbstractMap.SimpleEntry<>("b", 2));
        toRemove.add(new AbstractMap.SimpleEntry<>("d", 4));
        assertTrue(entrySet.removeAll(toRemove));
        assertEquals(2, map.size());
        assertFalse(map.containsKey("b"));
        assertFalse(map.containsKey("d"));

        // retainAll – keep only entry "a"
        List<Map.Entry<String, Integer>> toRetain = Collections.singletonList(
                new AbstractMap.SimpleEntry<>("a", 1));
        assertTrue(entrySet.retainAll(toRetain));
        assertEquals(1, map.size());
        assertTrue(map.containsKey("a"));
        assertFalse(map.containsKey("c"));
    }

    /* --------------------------------------------------------------
     * withConfig – creating a new map with altered configuration
     * -------------------------------------------------------------- */
    @Test
    public void testWithConfigChangesOrdering() {
        // start with insertion order
        CompactMap<String, Integer> original = CompactMap.<String, Integer>builder()
                .insertionOrder()
                .compactSize(5)
                .build();
        original.put("b", 2);
        original.put("a", 1);
        original.put("c", 3);

        // build a new map that is sorted
        Map<String, Object> cfg = new HashMap<>(original.getConfig());
        cfg.put(CompactMap.ORDERING, CompactMap.SORTED);
        CompactMap<String, Integer> sorted = original.withConfig(cfg);

        List<String> sortedKeys = new ArrayList<>();
        for (String k : sorted.keySet()) {
            sortedKeys.add(k);
        }
        assertEquals(Arrays.asList("a", "b", "c"), sortedKeys);
        // entries must be equal
        assertEquals(original, sorted);
    }

    /* --------------------------------------------------------------
     * CompactMapComparator direct tests
     * -------------------------------------------------------------- */
    @Test
    public void testCompactMapComparatorCaseInsensitive() {
        CompactMap.CompactMapComparator comp = new CompactMap.CompactMapComparator(true, false);
        assertEquals(0, comp.compare("abc", "ABC"));
        assertTrue(comp.compare("a", "b") < 0);
        assertTrue(comp.compare("z", "y") > 0);
    }

    @Test
    public void testCompactMapComparatorReverse() {
        CompactMap.CompactMapComparator comp = new CompactMap.CompactMapComparator(false, true);
        assertTrue(comp.compare(1, 2) > 0); // reverse order
        assertTrue(comp.compare(2, 1) < 0);
    }

    @Test
    public void testCompactMapComparatorMixedTypes() {
        CompactMap.CompactMapComparator comp = new CompactMap.CompactMapComparator(false, false);
        // String vs Integer – compare by class name
        int result = comp.compare("abc", 123);
        assertTrue(result != 0);
        // Null handling
        assertTrue(comp.compare(null, "x") > 0);
        assertTrue(comp.compare("x", null) < 0);
        assertEquals(0, comp.compare(null, null));
    }

    /* --------------------------------------------------------------
     * CompactMapEntry behavior (setValue writes through)
     * -------------------------------------------------------------- */
    @Test
    public void testCompactMapEntrySetValueWritesThroughParent() {
        CompactMap<String, String> map = new CompactMap<>();
        map.put("id", "original");
        // get the entry via entrySet iterator
        Map.Entry<String, String> entry = map.entrySet().iterator().next();
        assertTrue(entry instanceof CompactMap.CompactMapEntry);
        CompactMap.CompactMapEntry cmEntry = (CompactMap.CompactMapEntry) entry;
        cmEntry.setValue("changed");
        assertEquals("changed", map.get("id"));
    }

    /* --------------------------------------------------------------
     * Iterator concurrent modification detection
     * -------------------------------------------------------------- */
    @Test(expected = ConcurrentModificationException.class)
    public void testIteratorDetectsConcurrentModification() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .compactSize(5)
                .build();
        map.put("a", 1);
        map.put("b", 2);

        Iterator<String> it = map.keySet().iterator();
        assertTrue(it.hasNext());
        map.put("c", 3); // modify after iterator creation
        it.next(); // should throw CME
    }
}
