package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class CompactMapTest {

    private CompactMap<String, Integer> emptyMap;

    @Before
    public void setUp() {
        emptyMap = new CompactMap<>();
    }

    /*--------------------------------------------------------------
     *  Public constructors and basic state checks
     *--------------------------------------------------------------*/
    @Test
    public void testDefaultConstructor() {
        assertTrue(emptyMap.isEmpty());
        assertEquals(0, emptyMap.size());
        assertEquals(CompactMap.LogicalValueType.EMPTY, emptyMap.getLogicalValueType());
    }

    @Test
    public void testConstructorWithMapCopiesEntries() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);
        CompactMap<String, Integer> map = new CompactMap<>(source);
        assertEquals(2, map.size());
        assertTrue(map.containsKey("a"));
        assertTrue(map.containsKey("b"));
        assertEquals(Integer.valueOf(1), map.get("a"));
        assertEquals(Integer.valueOf(2), map.get("b"));
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullMapThrows() {
        new CompactMap<String, Integer>(null);
    }

    /*--------------------------------------------------------------
     *  isDefaultCompactMap()
     *--------------------------------------------------------------*/
    @Test
    public void testIsDefaultCompactMapTrueForPlainInstance() {
        CompactMap<String, Integer> map = new CompactMap<>();
        assertTrue(map.isDefaultCompactMap());
    }

    @Test
    public void testIsDefaultCompactMapFalseWhenConfiguredDifferently() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .caseSensitive(false)          // different case sensitivity
                .compactSize(10)              // different compact size
                .build();
        assertFalse(map.isDefaultCompactMap());
    }

    /*--------------------------------------------------------------
     *  Basic Map operations (size, isEmpty, contains, get, put, remove)
     *--------------------------------------------------------------*/
    @Test
    public void testPutAndGetSingleEntry() {
        CompactMap<String, Integer> map = new CompactMap<>();
        assertNull(map.put("one", 1));
        assertEquals(1, map.size());
        assertFalse(map.isEmpty());
        assertTrue(map.containsKey("one"));
        assertTrue(map.containsValue(1));
        assertEquals(Integer.valueOf(1), map.get("one"));
    }

    @Test
    public void testOverwriteExistingKey() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("x", 10);
        Integer old = map.put("x", 20);
        assertEquals(Integer.valueOf(10), old);
        assertEquals(Integer.valueOf(20), map.get("x"));
        assertEquals(1, map.size());
    }

    @Test
    public void testRemoveExistingKey() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("k", 5);
        Integer removed = map.remove("k");
        assertEquals(Integer.valueOf(5), removed);
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertNull(map.get("k"));
    }

    @Test
    public void testRemoveNonExistingKeyReturnsNull() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("a", 1);
        assertNull(map.remove("b"));
        assertEquals(1, map.size());
    }

    /*--------------------------------------------------------------
     *  Transition from compact array to backing map
     *--------------------------------------------------------------*/
    @Test
    public void testTransitionToMapWhenCompactSizeExceeded() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .compactSize(2)          // small compact size to trigger transition quickly
                .build();

        map.put("k1", 1);
        map.put("k2", 2);
        // At this point we should be in ARRAY state
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());

        map.put("k3", 3); // should cause transition to MAP
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType());
        assertEquals(3, map.size());
    }

    @Test
    public void testTransitionBackToSingleEntryAfterRemovals() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .compactSize(3)
                .build();

        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3); // now in MAP state (size > compactSize)
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType());

        // Remove two entries, should transition back to SINGLE ENTRY
        map.remove("b");
        map.remove("c");
        assertEquals(CompactMap.LogicalValueType.ENTRY, map.getLogicalValueType());
        assertEquals(1, map.size());
        assertTrue(map.containsKey("a"));
    }

    /*--------------------------------------------------------------
     *  putAll and clear
     *--------------------------------------------------------------*/
    @Test
    public void testPutAllCopiesAllEntries() {
        Map<String, Integer> source = new HashMap<>();
        source.put("x", 100);
        source.put("y", 200);
        CompactMap<String, Integer> map = new CompactMap<>();
        map.putAll(source);
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(100), map.get("x"));
        assertEquals(Integer.valueOf(200), map.get("y"));
    }

    @Test
    public void testPutAllWithNullDoesNothing() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.putAll(null);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testClearEmptiesMap() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertFalse(map.containsKey("a"));
    }

    /*--------------------------------------------------------------
     *  hashCode and equals
     *--------------------------------------------------------------*/
    @Test
    public void testHashCodeConsistency() {
        CompactMap<String, Integer> map1 = new CompactMap<>();
        map1.put("a", 1);
        map1.put("b", 2);

        CompactMap<String, Integer> map2 = new CompactMap<>();
        map2.put("b", 2);
        map2.put("a", 1);

        assertEquals(map1.hashCode(), map2.hashCode());
        assertTrue(map1.equals(map2));
        assertTrue(map2.equals(map1));
    }

    @Test
    public void testEqualsWithDifferentSizes() {
        CompactMap<String, Integer> map1 = new CompactMap<>();
        map1.put("a", 1);
        CompactMap<String, Integer> map2 = new CompactMap<>();
        map2.put("a", 1);
        map2.put("b", 2);
        assertFalse(map1.equals(map2));
    }

    /*--------------------------------------------------------------
     *  toString representation
     *--------------------------------------------------------------*/
    @Test
    public void testToStringFormat() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("one", 1);
        map.put("two", 2);
        String s = map.toString();
        // Order is unspecified for unordered map, just check that it contains both pairs
        assertTrue(s.contains("one=1"));
        assertTrue(s.contains("two=2"));
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
    }

    /*--------------------------------------------------------------
     *  keySet, values, entrySet views and iterator behavior
     *--------------------------------------------------------------*/
    @Test
    public void testKeySetView() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("k1", 10);
        map.put("k2", 20);
        Set<String> keys = map.keySet();
        assertEquals(2, keys.size());
        assertTrue(keys.contains("k1"));
        assertTrue(keys.contains("k2"));
    }

    @Test
    public void testValuesView() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Collection<Integer> values = map.values();
        assertEquals(2, values.size());
        assertTrue(values.contains(1));
        assertTrue(values.contains(2));
    }

    @Test
    public void testEntrySetViewAndIteratorRemove() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("x", 10);
        map.put("y", 20);
        Set<Map.Entry<String, Integer>> entries = map.entrySet();
        Iterator<Map.Entry<String, Integer>> it = entries.iterator();
        assertTrue(it.hasNext());
        Map.Entry<String, Integer> e = it.next();
        it.remove(); // remove the entry we just visited
        assertEquals(1, map.size());
        assertFalse(map.containsKey(e.getKey()));
    }

    @Test(expected = IllegalStateException.class)
    public void testIteratorRemoveWithoutNextThrows() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("z", 5);
        Iterator<String> it = map.keySet().iterator();
        it.remove(); // should throw IllegalStateException because next() not called
    }

    @Test(expected = ConcurrentModificationException.class)
    public void testIteratorDetectsConcurrentModification() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Iterator<String> it = map.keySet().iterator();
        map.put("c", 3); // modify after iterator creation
        it.hasNext(); // should throw ConcurrentModificationException
    }

    /*--------------------------------------------------------------
     *  Builder configuration tests
     *--------------------------------------------------------------*/
    @Test
    public void testBuilderCaseInsensitive() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .caseSensitive(false)
                .compactSize(5)
                .build();
        map.put("Key", 100);
        // case‑insensitive lookup
        assertTrue(map.containsKey("key"));
        assertEquals(Integer.valueOf(100), map.get("KEY"));
    }

    @Test
    public void testBuilderSortedOrder() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .sortedOrder()
                .compactSize(5)
                .build();
        map.put("b", 2);
        map.put("a", 1);
        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        Map.Entry<String, Integer> first = it.next();
        Map.Entry<String, Integer> second = it.next();
        assertEquals("a", first.getKey());
        assertEquals(Integer.valueOf(1), first.getValue());
        assertEquals("b", second.getKey());
        assertEquals(Integer.valueOf(2), second.getValue());
    }

    @Test
    public void testBuilderInsertionOrder() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .insertionOrder()
                .compactSize(5)
                .build();
        map.put("first", 1);
        map.put("second", 2);
        map.put("third", 3);
        List<String> keys = new ArrayList<>(map.keySet());
        assertEquals(Arrays.asList("first", "second", "third"), keys);
    }

    @Test
    public void testBuilderMapType() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .mapType(LinkedHashMap.class)
                .compactSize(5)
                .build();
        map.put("a", 1);
        map.put("b", 2);
        // Verify that the backing map is a LinkedHashMap by checking ordering preservation
        List<String> keys = new ArrayList<>(map.keySet());
        assertEquals(Arrays.asList("a", "b"), keys);
    }

    @Test
    public void testBuilderSingleValueKeyOptimisation() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .singleValueKey("uid")
                .compactSize(5)
                .build();
        map.put("uid", 42); // should use optimized OBJECT storage
        assertEquals(CompactMap.LogicalValueType.OBJECT, map.getLogicalValueType());
        assertEquals(Integer.valueOf(42), map.get("uid"));
        // Adding a different key forces transition to ENTRY storage
        map.put("other", 99);
        assertEquals(CompactMap.LogicalValueType.ENTRY, map.getLogicalValueType());
    }

    /*--------------------------------------------------------------
     *  getConfig and withConfig
     *--------------------------------------------------------------*/
    @Test
    public void testGetConfigReflectsBuilderSettings() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .caseSensitive(false)
                .compactSize(7)
                .sortedOrder()
                .singleValueKey("uid")
                .mapType(TreeMap.class)
                .build();

        Map<String, Object> cfg = map.getConfig();
        assertEquals(7, cfg.get(CompactMap.COMPACT_SIZE));
        assertEquals(Boolean.FALSE, cfg.get(CompactMap.CASE_SENSITIVE));
        assertEquals(CompactMap.SORTED, cfg.get(CompactMap.ORDERING));
        assertEquals("uid", cfg.get(CompactMap.SINGLE_KEY));
        assertEquals(TreeMap.class, cfg.get(CompactMap.MAP_TYPE));
    }

    @Test
    public void testWithConfigCreatesNewMapWithSameEntries() {
        CompactMap<String, Integer> original = CompactMap.<String, Integer>builder()
                .compactSize(5)
                .sortedOrder()
                .build();
        original.put("a", 1);
        original.put("b", 2);

        Map<String, Object> newConfig = new HashMap<>();
        newConfig.put(CompactMap.COMPACT_SIZE, 3); // change compact size
        newConfig.put(CompactMap.ORDERING, CompactMap.REVERSE); // change ordering

        CompactMap<String, Integer> newMap = original.withConfig(newConfig);
        assertEquals(2, newMap.size());
        assertEquals(Integer.valueOf(1), newMap.get("a"));
        assertEquals(Integer.valueOf(2), newMap.get("b"));
        // ordering should now be reverse – verify iterator order
        Iterator<String> it = newMap.keySet().iterator();
        assertEquals("b", it.next());
        assertEquals("a", it.next());
    }

    /*--------------------------------------------------------------
     *  CompactMapComparator edge cases
     *--------------------------------------------------------------*/
    @Test
    public void testComparatorHandlesNulls() {
        CompactMap.CompactMapComparator comp = new CompactMap.CompactMapComparator(false, false);
        assertEquals(0, comp.compare(null, null));
        assertTrue(comp.compare(null, "a") > 0);
        assertTrue(comp.compare("a", null) < 0);
    }

    @Test
    public void testComparatorCaseInsensitive() {
        CompactMap.CompactMapComparator comp = new CompactMap.CompactMapComparator(true, false);
        assertEquals(0, comp.compare("Apple", "apple"));
        assertTrue(comp.compare("apple", "Banana") < 0);
    }

    @Test
    public void testComparatorReverseOrdering() {
        CompactMap.CompactMapComparator comp = new CompactMap.CompactMapComparator(false, true);
        assertTrue(comp.compare("a", "b") > 0); // reverse means "a" > "b"
        assertEquals(0, comp.compare("c", "c"));
    }

    @Test
    public void testComparatorDifferentTypes() {
        CompactMap.CompactMapComparator comp = new CompactMap.CompactMapComparator(false, false);
        // String vs Integer – compare by class name
        int result = comp.compare("abc", 123);
        assertTrue(result != 0);
        // Comparable same type
        assertEquals(0, comp.compare(5, 5));
        assertTrue(comp.compare(3, 7) < 0);
    }
}
