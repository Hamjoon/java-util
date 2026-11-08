package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.util.*;

import static org.junit.Assert.*;

public class CompactMapTest {

    private CompactMap<String, Integer> emptyMap;

    @Before
    public void setUp() {
        emptyMap = new CompactMap<>();
    }

    /* -------------------------------------------------
     *  Basic public API – size, isEmpty, toString, clear
     * ------------------------------------------------- */
    @Test
    public void testEmptyMapSizeAndIsEmpty() {
        assertEquals(0, emptyMap.size());
        assertTrue(emptyMap.isEmpty());
        assertEquals("{}", emptyMap.toString());

        emptyMap.clear(); // should stay empty without error
        assertEquals(0, emptyMap.size());
        assertTrue(emptyMap.isEmpty());
    }

    /* -------------------------------------------------
     *  Single‑entry optimisation (default singleValueKey = "id")
     * ------------------------------------------------- */
    @Test
    public void testSingleEntryOptimisation() {
        CompactMap<String, String> map = new CompactMap<>();
        assertNull(map.put("id", "first"));
        assertEquals(1, map.size());
        assertEquals("first", map.get("id"));
        assertTrue(map.containsKey("id"));
        assertTrue(map.containsValue("first"));

        // Overwrite same key – should return previous value
        assertEquals("first", map.put("id", "second"));
        assertEquals(1, map.size());
        assertEquals("second", map.get("id"));

        // Remove the only entry – map becomes empty again
        assertEquals("second", map.remove("id"));
        assertTrue(map.isEmpty());
        assertNull(map.get("id"));
    }

    /* -------------------------------------------------
     *  Normal put / get / contains behaviour
     * ------------------------------------------------- */
    @Test
    public void testPutGetContains() {
        CompactMap<String, Integer> map = new CompactMap<>();
        assertNull(map.put("one", 1));
        assertNull(map.put("two", 2));
        assertEquals(2, map.size());

        assertTrue(map.containsKey("one"));
        assertTrue(map.containsKey("two"));
        assertFalse(map.containsKey("three"));

        assertTrue(map.containsValue(1));
        assertTrue(map.containsValue(2));
        assertFalse(map.containsValue(3));

        assertEquals(Integer.valueOf(1), map.get("one"));
        assertEquals(Integer.valueOf(2), map.get("two"));
        assertNull(map.get("three"));
    }

    /* -------------------------------------------------
     *  Transition from compact array to backing map
     *  (compactSize set to 2, add three entries)
     * ------------------------------------------------- */
    @Test
    public void testTransitionToBackingMap() {
        CompactMap<String, String> map = CompactMap.<String, String>builder()
                .compactSize(2)               // force transition after 2 entries
                .build();

        // first two entries stay in compact array
        map.put("a", "A");
        map.put("b", "B");
        assertEquals(2, map.size());
        assertEquals("A", map.get("a"));
        assertEquals("B", map.get("b"));

        // third entry forces a switch to a backing HashMap
        map.put("c", "C");
        assertEquals(3, map.size());
        assertEquals("C", map.get("c"));

        // All entries must still be accessible
        assertEquals("A", map.get("a"));
        assertEquals("B", map.get("b"));
        assertEquals("C", map.get("c"));
    }

    /* -------------------------------------------------
     *  Sorted order – iteration order of keySet, values and entrySet
     * ------------------------------------------------- */
    @Test
    public void testSortedOrderIteration() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .sortedOrder()
                .mapType(TreeMap.class)      // TreeMap provides natural ordering
                .compactSize(5)               // small enough to stay in array for this test
                .build();

        map.put("beta", 2);
        map.put("alpha", 1);
        map.put("gamma", 3);

        // Expected natural order: alpha, beta, gamma
        List<String> expectedKeys = Arrays.asList("alpha", "beta", "gamma");
        List<Integer> expectedValues = Arrays.asList(1, 2, 3);
        List<Map.Entry<String, Integer>> expectedEntries = new ArrayList<>();
        expectedEntries.add(new AbstractMap.SimpleEntry<>("alpha", 1));
        expectedEntries.add(new AbstractMap.SimpleEntry<>("beta", 2));
        expectedEntries.add(new AbstractMap.SimpleEntry<>("gamma", 3));

        assertEquals(expectedKeys, new ArrayList<>(map.keySet()));
        assertEquals(expectedValues, new ArrayList<>(map.values()));
        assertEquals(expectedEntries, new ArrayList<>(map.entrySet()));
    }

    /* -------------------------------------------------
     *  Case‑insensitive key handling (default singleValueKey is "id")
     * ------------------------------------------------- */
    @Test
    public void testCaseInsensitiveLookup() {
        CompactMap<String, String> map = CompactMap.<String, String>builder()
                .caseSensitive(false)        // enable case‑insensitive mode
                .build();

        map.put("Key", "value");
        // Different capitalisations must resolve to the same entry
        assertEquals("value", map.get("key"));
        assertEquals("value", map.get("KEY"));
        assertTrue(map.containsKey("kEy"));
        assertEquals(1, map.size());

        // Overwrite using a different case – should replace the same entry
        assertEquals("value", map.put("kEy", "new"));
        assertEquals("new", map.get("KEY"));
        assertEquals(1, map.size());
    }

    /* -------------------------------------------------
     *  keySet – contains, iterator removal, clear
     * ------------------------------------------------- */
    @Test
    public void testKeySetOperations() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("x", 10);
        map.put("y", 20);
        map.put("z", 30);

        Set<String> keys = map.keySet();
        assertTrue(keys.contains("x"));
        assertTrue(keys.contains("y"));
        assertTrue(keys.contains("z"));
        assertFalse(keys.contains("w"));

        // Remove via iterator
        Iterator<String> it = keys.iterator();
        while (it.hasNext()) {
            String k = it.next();
            if ("y".equals(k)) {
                it.remove();
            }
        }
        assertFalse(map.containsKey("y"));
        assertEquals(2, map.size());

        // clear via keySet
        keys.clear();
        assertTrue(map.isEmpty());
    }

    /* -------------------------------------------------
     *  entrySet – contains, remove, iterator removal
     * ------------------------------------------------- */
    @Test
    public void testEntrySetOperations() {
        CompactMap<String, String> map = new CompactMap<>();
        map.put("a", "A");
        map.put("b", "B");
        map.put("c", "C");

        Set<Map.Entry<String, String>> entries = map.entrySet();

        // contains check using a fresh entry object
        Map.Entry<String, String> entry = new AbstractMap.SimpleEntry<>("b", "B");
        assertTrue(entries.contains(entry));

        // remove via set.remove()
        assertTrue(entries.remove(entry));
        assertFalse(map.containsKey("b"));
        assertEquals(2, map.size());

        // iterator removal
        Iterator<Map.Entry<String, String>> it = entries.iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> e = it.next();
            if ("a".equals(e.getKey())) {
                it.remove();
            }
        }
        assertFalse(map.containsKey("a"));
        assertEquals(1, map.size());
    }

    /* -------------------------------------------------
     *  values collection – iteration order matches key order
     * ------------------------------------------------- */
    @Test
    public void testValuesCollection() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .insertionOrder()
                .mapType(LinkedHashMap.class)
                .compactSize(10)
                .build();

        map.put("first", 1);
        map.put("second", 2);
        map.put("third", 3);

        Collection<Integer> values = map.values();
        List<Integer> expected = Arrays.asList(1, 2, 3);
        assertEquals(expected, new ArrayList<>(values));
    }

    /* -------------------------------------------------
     *  equals() and hashCode() consistency with a regular HashMap
     * ------------------------------------------------- */
    @Test
    public void testEqualsAndHashCode() {
        Map<String, Integer> reference = new HashMap<>();
        reference.put("one", 1);
        reference.put("two", 2);
        reference.put("three", 3);

        CompactMap<String, Integer> map = new CompactMap<>();
        map.putAll(reference);

        assertEquals(reference, map);
        assertEquals(map, reference);
        assertEquals(reference.hashCode(), map.hashCode());
    }

    /* -------------------------------------------------
     *  putAll with null and empty map – no exception, no change
     * ------------------------------------------------- */
    @Test
    public void testPutAllNullAndEmpty() {
        CompactMap<String, String> map = new CompactMap<>();

        // null argument – method returns silently
        map.putAll(null);
        assertTrue(map.isEmpty());

        // empty map – also no change
        map.putAll(Collections.emptyMap());
        assertTrue(map.isEmpty());

        // non‑empty map – normal behaviour
        Map<String, String> src = new HashMap<>();
        src.put("k", "v");
        map.putAll(src);
        assertEquals(1, map.size());
        assertEquals("v", map.get("k"));
    }

    /* -------------------------------------------------
     *  Deprecated operations – must throw UnsupportedOperationException
     * ------------------------------------------------- */
    @Test(expected = UnsupportedOperationException.class)
    public void testMinusUnsupported() {
        CompactMap<String, String> map = new CompactMap<>();
        map.minus("any");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPlusUnsupported() {
        CompactMap<String, String> map = new CompactMap<>();
        map.plus("any");
    }

    /* -------------------------------------------------
     *  Builder – verifies that all configuration methods are reachable
     * ------------------------------------------------- */
    @Test
    public void testBuilderConfiguration() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);

        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .caseSensitive(false)
                .compactSize(3)
                .mapType(LinkedHashMap.class)
                .singleValueKey("uid")
                .sourceMap(source)
                .insertionOrder()
                .build();

        // source map entries must be present
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(1), map.get("a"));
        assertEquals(Integer.valueOf(2), map.get("b"));

        // verify case‑insensitive lookup
        assertEquals(Integer.valueOf(1), map.get("A"));
    }
}
