package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.Assert.*;

public class CompactMapTest {

    private CompactMap<String, String> emptyMap;

    @Before
    public void setUp() {
        emptyMap = new CompactMap<>();
    }

    /** Helper to read the protected “val” field via reflection. */
    private Object getInternalVal(CompactMap<?, ?> map) throws Exception {
        Field f = CompactMap.class.getDeclaredField("val");
        f.setAccessible(true);
        return f.get(map);
    }

    @Test
    public void testEmptyMapBasics() {
        assertEquals(0, emptyMap.size());
        assertTrue(emptyMap.isEmpty());
        assertFalse(emptyMap.containsKey("anything"));
        assertFalse(emptyMap.containsValue("anything"));
        assertNull(emptyMap.get("anything"));
    }

    @Test
    public void testSingleEntryOptimization() throws Exception {
        CompactMap<String, String> map = new CompactMap<>();
        // The default single value key is "id"
        map.put("id", "singleValue");
        assertEquals(1, map.size());
        assertEquals("singleValue", map.get("id"));
        // internal storage should be the raw value, not a CompactMapEntry
        Object internal = getInternalVal(map);
        assertTrue(internal instanceof String);
        assertEquals("singleValue", internal);

        // Updating the same key should replace the stored value
        map.put("id", "newValue");
        assertEquals("newValue", map.get("id"));
        internal = getInternalVal(map);
        assertTrue(internal instanceof String);
        assertEquals("newValue", internal);
    }

    @Test
    public void testMultipleEntriesAndBasicOperations() {
        CompactMap<String, String> map = new CompactMap<>();
        map.put("a", "1");
        map.put("b", "2");
        assertEquals(2, map.size());
        assertFalse(map.isEmpty());
        assertTrue(map.containsKey("a"));
        assertTrue(map.containsKey("b"));
        assertTrue(map.containsValue("1"));
        assertTrue(map.containsValue("2"));
        assertEquals("1", map.get("a"));
        assertEquals("2", map.get("b"));

        // Overwrite existing key
        map.put("a", "10");
        assertEquals(2, map.size());
        assertEquals("10", map.get("a"));
    }

    @Test
    public void testCompactArrayTransitionToMap() throws Exception {
        CompactMap<String, String> map = CompactMap.builder()
                .compactSize(2)   // trigger transition after 3 entries
                .build();

        map.put("k1", "v1");
        map.put("k2", "v2");
        // Still in compact array (Object[])
        Object internal = getInternalVal(map);
        assertTrue(internal instanceof Object[]);

        map.put("k3", "v3"); // should switch to backing Map
        internal = getInternalVal(map);
        assertTrue(internal instanceof Map);
        assertEquals(3, map.size());
        assertEquals("v1", map.get("k1"));
        assertEquals("v2", map.get("k2"));
        assertEquals("v3", map.get("k3"));

        // Removing one entry should revert back to compact array (size == compactSize)
        map.remove("k3");
        internal = getInternalVal(map);
        assertTrue(internal instanceof Object[]);
        assertEquals(2, map.size());
    }

    @Test
    public void testCaseInsensitiveKeys() {
        CompactMap<String, String> map = CompactMap.builder()
                .caseSensitive(false)
                .build();

        map.put("Key", "value");
        assertTrue(map.containsKey("key"));
        assertTrue(map.containsKey("KEY"));
        assertEquals("value", map.get("kEy"));
        // Ensure equals works with another map having same logical entries
        Map<String, String> other = new HashMap<>();
        other.put("key", "value");
        assertTrue(map.equals(other));
        assertTrue(other.equals(map));
    }

    @Test
    public void testSortedOrderIteration() {
        CompactMap<String, Integer> map = CompactMap.builder()
                .sortedOrder()
                .compactSize(5)
                .build();

        map.put("c", 3);
        map.put("a", 1);
        map.put("b", 2);

        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            keys.add(e.getKey());
        }
        assertEquals(Arrays.asList("a", "b", "c"), keys);
    }

    @Test
    public void testInsertionOrderIteration() {
        CompactMap<String, Integer> map = CompactMap.builder()
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
    public void testKeySetOperations() {
        CompactMap<String, String> map = new CompactMap<>();
        map.put("a", "1");
        map.put("b", "2");
        map.put("c", "3");

        Set<String> ks = map.keySet();
        assertTrue(ks.remove("b"));
        assertFalse(ks.contains("b"));
        assertEquals(2, map.size());

        ks.removeAll(Arrays.asList("a", "nonexistent"));
        assertFalse(ks.contains("a"));
        assertEquals(1, map.size());

        ks.retainAll(Collections.singleton("c"));
        assertTrue(ks.contains("c"));
        assertEquals(1, map.size());
    }

    @Test
    public void testValuesCollection() {
        CompactMap<String, String> map = new CompactMap<>();
        map.put("x", "10");
        map.put("y", "20");
        Collection<String> vals = map.values();

        List<String> list = new ArrayList<>(vals);
        assertTrue(list.containsAll(Arrays.asList("10", "20")));

        // Remove via iterator
        Iterator<String> it = vals.iterator();
        while (it.hasNext()) {
            if ("10".equals(it.next())) {
                it.remove();
            }
        }
        assertFalse(map.containsKey("x"));
        assertEquals(1, map.size());
    }

    @Test
    public void testEntrySetOperationsAndEquality() {
        CompactMap<String, String> map = new CompactMap<>();
        map.put("k1", "v1");
        map.put("k2", "v2");

        Set<Map.Entry<String, String>> es = map.entrySet();
        assertTrue(es.contains(new AbstractMap.SimpleEntry<>("k1", "v1")));
        assertFalse(es.contains(new AbstractMap.SimpleEntry<>("k1", "different")));

        // Remove an entry via the set
        Map.Entry<String, String> toRemove = new AbstractMap.SimpleEntry<>("k2", "v2");
        assertTrue(es.remove(toRemove));
        assertFalse(map.containsKey("k2"));
        assertEquals(1, map.size());

        // retainAll
        es.retainAll(Collections.singleton(new AbstractMap.SimpleEntry<>("k1", "v1")));
        assertEquals(1, map.size());
        assertTrue(map.containsKey("k1"));
    }

    @Test(expected = ConcurrentModificationException.class)
    public void testIteratorConcurrentModificationDetection() {
        CompactMap<String, String> map = new CompactMap<>();
        map.put("a", "1");
        map.put("b", "2");
        Iterator<String> it = map.keySet().iterator();
        map.put("c", "3"); // structural modification
        it.next(); // should throw CME
    }

    @Test
    public void testIteratorRemoveFunctionality() {
        CompactMap<String, String> map = new CompactMap<>();
        map.put("one", "1");
        map.put("two", "2");
        map.put("three", "3");

        Iterator<String> it = map.keySet().iterator();
        while (it.hasNext()) {
            String k = it.next();
            if ("two".equals(k)) {
                it.remove();
            }
        }
        assertFalse(map.containsKey("two"));
        assertEquals(2, map.size());
    }

    @Test
    public void testCompactMapEntrySetValuePropagation() {
        CompactMap<String, String> map = new CompactMap<>();
        map.put("id", "initial");
        Map.Entry<String, String> entry = map.entrySet().iterator().next();
        assertEquals("initial", entry.getValue());

        entry.setValue("updated");
        assertEquals("updated", map.get("id"));
        // Ensure entry still reflects new value
        assertEquals("updated", entry.getValue());
    }

    @Test
    public void testEntryEqualsAndHashCode() {
        CompactMap<String, String> map = new CompactMap<>();
        map.put("k", "v");
        Map.Entry<String, String> entry = map.entrySet().iterator().next();

        // Equality with equivalent SimpleEntry
        AbstractMap.SimpleEntry<String, String> simple = new AbstractMap.SimpleEntry<>("k", "v");
        assertTrue(entry.equals(simple));
        assertTrue(simple.equals(entry));

        // Hash code consistency
        assertEquals(entry.hashCode(), simple.hashCode());
    }

    @Test
    public void testEqualsAndHashCodeWithStandardMap() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("one", 1);
        map.put("two", 2);

        Map<String, Integer> std = new HashMap<>();
        std.put("one", 1);
        std.put("two", 2);

        assertTrue(map.equals(std));
        assertTrue(std.equals(map));
        assertEquals(map.hashCode(), std.hashCode());
    }

    @Test
    public void testToStringContainsEntries() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("x", 10);
        map.put("y", 20);
        String s = map.toString();
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        assertTrue(s.contains("x=10"));
        assertTrue(s.contains("y=20"));
    }

    @Test
    public void testClearResetsMap() {
        CompactMap<String, String> map = new CompactMap<>();
        map.put("a", "1");
        map.put("b", "2");
        assertFalse(map.isEmpty());
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertNull(map.get("a"));
    }

    @Test
    public void testNullKeyAndValueSupport() {
        CompactMap<String, String> map = new CompactMap<>();
        map.put(null, "nullKey");
        map.put("nullValue", null);
        assertTrue(map.containsKey(null));
        assertTrue(map.containsValue(null));
        assertEquals("nullKey", map.get(null));
        assertNull(map.get("nullValue"));
    }

    @Test
    public void testSingleEntryStoredAsCompactMapEntryWhenKeyDiffersFromSingleValueKey() throws Exception {
        CompactMap<String, String> map = new CompactMap<>();
        map.put("differentKey", "value");
        assertEquals(1, map.size());
        Object internal = getInternalVal(map);
        assertTrue(internal instanceof CompactMap.CompactMapEntry);
    }
}
