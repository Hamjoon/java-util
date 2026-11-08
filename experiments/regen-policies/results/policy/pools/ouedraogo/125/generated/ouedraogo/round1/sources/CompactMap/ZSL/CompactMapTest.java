package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class CompactMapTest {

    private CompactMap<String, String> map;

    @Before
    public void setUp() {
        map = new CompactMap<>();
    }

    /* -------------------------------------------------
     * Basic map contract tests (size, isEmpty, clear)
     * ------------------------------------------------- */
    @Test
    public void testEmptyMap() {
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
        assertNull(map.get("any"));
        assertFalse(map.containsKey("any"));
        assertFalse(map.containsValue("any"));
    }

    @Test
    public void testPutAndGetSingleEntry() {
        assertNull(map.put("id", "single"));
        assertEquals(1, map.size());
        assertFalse(map.isEmpty());
        assertEquals("single", map.get("id"));
        assertTrue(map.containsKey("id"));
        assertTrue(map.containsValue("single"));
    }

    @Test
    public void testOverwriteSingleEntry() {
        map.put("id", "first");
        assertEquals("first", map.put("id", "second"));
        assertEquals(1, map.size());
        assertEquals("second", map.get("id"));
    }

    @Test
    public void testTransitionToArrayAndMap() {
        // Use a small compactSize to force transition quickly
        CompactMap<String, String> small = CompactMap.<String, String>builder()
                .compactSize(3)               // array up to 3 entries
                .mapType(HashMap.class)       // backing map when exceeded
                .build();

        // 1st entry (single value optimisation)
        small.put("id", "v1");
        assertEquals(1, small.size());
        assertEquals("v1", small.get("id"));

        // 2nd entry forces array storage (size 2)
        small.put("a", "v2");
        assertEquals(2, small.size());
        assertEquals("v2", small.get("a"));

        // 3rd entry still array (size 3)
        small.put("b", "v3");
        assertEquals(3, small.size());

        // 4th entry forces transition to backing map
        small.put("c", "v4");
        assertEquals(4, small.size());
        assertEquals("v4", small.get("c"));
        assertTrue(small.containsKey("a"));
        assertTrue(small.containsKey("b"));
    }

    @Test
    public void testRemoveTransitions() {
        CompactMap<String, String> small = CompactMap.<String, String>builder()
                .compactSize(3)
                .build();

        // Fill to array size (2 entries)
        small.put("id", "v1"); // single optimisation
        small.put("x", "v2");   // now array size 2
        assertEquals(2, small.size());

        // Remove one entry -> should go back to single entry optimisation
        assertEquals("v2", small.remove("x"));
        assertEquals(1, small.size());
        assertTrue(small.containsKey("id"));
        assertFalse(small.containsKey("x"));
    }

    @Test
    public void testClear() {
        map.put("id", "v");
        map.put("a", "b");
        assertFalse(map.isEmpty());
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertNull(map.get("id"));
    }

    /* -------------------------------------------------
     * Equality and hashCode contract
     * ------------------------------------------------- */
    @Test
    public void testEqualsAndHashCode() {
        Map<String, String> reference = new HashMap<>();
        reference.put("id", "one");
        reference.put("key", "two");

        CompactMap<String, String> cMap = new CompactMap<>(reference);
        assertEquals(reference.size(), cMap.size());
        assertTrue(cMap.equals(reference));
        assertTrue(reference.equals(cMap));
        assertEquals(reference.hashCode(), cMap.hashCode());
    }

    @Test
    public void testEqualsWithDifferentOrderings() {
        CompactMap<String, String> map1 = CompactMap.<String, String>builder()
                .insertionOrder()
                .build();
        CompactMap<String, String> map2 = CompactMap.<String, String>builder()
                .insertionOrder()
                .build();

        map1.put("a", "1");
        map1.put("b", "2");

        map2.put("b", "2");
        map2.put("a", "1");

        assertTrue(map1.equals(map2));
        assertEquals(map1.hashCode(), map2.hashCode());
    }

    /* -------------------------------------------------
     * toString sanity check
     * ------------------------------------------------- */
    @Test
    public void testToStringContainsEntries() {
        map.put("id", "v1");
        map.put("x", "y");
        String s = map.toString();
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        assertTrue(s.contains("id=v1"));
        assertTrue(s.contains("x=y"));
    }

    /* -------------------------------------------------
     * keySet, values, entrySet behaviours
     * ------------------------------------------------- */
    @Test
    public void testKeySetOperations() {
        map.put("id", "1");
        map.put("a", "2");
        Set<String> keys = map.keySet();

        assertEquals(2, keys.size());
        assertTrue(keys.contains("id"));
        assertTrue(keys.contains("a"));

        // remove via keySet
        assertTrue(keys.remove("a"));
        assertFalse(map.containsKey("a"));
        assertEquals(1, map.size());

        // clear via keySet
        keys.clear();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testValuesOperations() {
        map.put("id", "1");
        map.put("a", "2");
        Collection<String> values = map.values();

        assertEquals(2, values.size());
        assertTrue(values.contains("1"));
        assertTrue(values.contains("2"));

        // remove via iterator
        Iterator<String> it = values.iterator();
        while (it.hasNext()) {
            String v = it.next();
            if ("2".equals(v)) {
                it.remove();
            }
        }
        assertFalse(map.containsValue("2"));
        assertEquals(1, map.size());
    }

    @Test
    public void testEntrySetOperations() {
        map.put("id", "1");
        map.put("a", "2");
        Set<Map.Entry<String, String>> entries = map.entrySet();

        assertEquals(2, entries.size());

        // contains check
        Map.Entry<String, String> e = new AbstractMap.SimpleEntry<>("a", "2");
        assertTrue(entries.contains(e));

        // remove entry
        assertTrue(entries.remove(e));
        assertFalse(map.containsKey("a"));
        assertEquals(1, map.size());

        // retainAll
        Map.Entry<String, String> keep = new AbstractMap.SimpleEntry<>("id", "1");
        entries.retainAll(Collections.singleton(keep));
        assertEquals(1, map.size());
        assertTrue(map.containsKey("id"));
    }

    /* -------------------------------------------------
     * Case‑insensitive behaviour
     * ------------------------------------------------- */
    @Test
    public void testCaseInsensitiveKeys() {
        CompactMap<String, String> ciMap = CompactMap.<String, String>builder()
                .caseSensitive(false)
                .build();

        ciMap.put("Key", "value");
        assertTrue(ciMap.containsKey("key"));
        assertTrue(ciMap.containsKey("KEY"));
        assertEquals("value", ciMap.get("kEy"));
    }

    /* -------------------------------------------------
     * Sorted ordering and comparator
     * ------------------------------------------------- */
    @Test
    public void testSortedOrderIteration() {
        CompactMap<String, Integer> sorted = CompactMap.<String, Integer>builder()
                .sortedOrder()
                .compactSize(4)               // keep it small
                .build();

        sorted.put("c", 3);
        sorted.put("a", 1);
        sorted.put("b", 2);

        Iterator<String> it = sorted.keySet().iterator();
        List<String> order = new ArrayList<>();
        while (it.hasNext()) {
            order.add(it.next());
        }
        // natural order a,b,c
        assertEquals(Arrays.asList("a", "b", "c"), order);
    }

    @Test
    public void testReverseOrderIteration() {
        CompactMap<String, Integer> rev = CompactMap.<String, Integer>builder()
                .reverseOrder()
                .compactSize(4)
                .build();

        rev.put("c", 3);
        rev.put("a", 1);
        rev.put("b", 2);

        Iterator<String> it = rev.keySet().iterator();
        List<String> order = new ArrayList<>();
        while (it.hasNext()) {
            order.add(it.next());
        }
        // reverse natural order c,b,a
        assertEquals(Arrays.asList("c", "b", "a"), order);
    }

    /* -------------------------------------------------
     * Iterator concurrent‑modification detection
     * ------------------------------------------------- */
    @Test(expected = ConcurrentModificationException.class)
    public void testIteratorConcurrentModification() {
        map.put("id", "1");
        map.put("a", "2");
        Iterator<String> it = map.keySet().iterator();
        assertTrue(it.hasNext());
        map.put("b", "3"); // modify after iterator creation
        it.next(); // should throw
    }

    @Test
    public void testIteratorRemove() {
        map.put("id", "1");
        map.put("a", "2");
        map.put("b", "3");

        Iterator<String> it = map.keySet().iterator();
        while (it.hasNext()) {
            String k = it.next();
            if ("a".equals(k)) {
                it.remove();
            }
        }
        assertFalse(map.containsKey("a"));
        assertEquals(2, map.size());
    }

    /* -------------------------------------------------
     * CompactMapComparator direct tests
     * ------------------------------------------------- */
    @Test
    public void testComparatorCaseInsensitive() {
        CompactMap.CompactMapComparator comp = new CompactMap.CompactMapComparator(true, false);
        assertEquals(0, comp.compare("abc", "ABC"));
        assertTrue(comp.compare("a", "b") < 0);
        assertTrue(comp.compare("z", "y") > 0);
    }

    @Test
    public void testComparatorReverseOrder() {
        CompactMap.CompactMapComparator comp = new CompactMap.CompactMapComparator(false, true);
        assertTrue(comp.compare("a", "b") > 0); // reversed
        assertTrue(comp.compare("b", "a") < 0);
    }

    @Test
    public void testComparatorWithNulls() {
        CompactMap.CompactMapComparator comp = new CompactMap.CompactMapComparator(false, false);
        assertTrue(comp.compare(null, "a") > 0);
        assertTrue(comp.compare("a", null) < 0);
        assertEquals(0, comp.compare(null, null));
    }

    /* -------------------------------------------------
     * CompactMapEntry behaviour (equals & hashCode)
     * ------------------------------------------------- */
    @Test
    public void testCompactMapEntryEqualsAndHashCode() {
        CompactMap<String, String>.CompactMapEntry entry1 = map.new CompactMapEntry("k", "v");
        CompactMap<String, String>.CompactMapEntry entry2 = map.new CompactMapEntry("k", "v");
        CompactMap<String, String>.CompactMapEntry entry3 = map.new CompactMapEntry("k", "different");

        assertEquals(entry1, entry2);
        assertNotEquals(entry1, entry3);
        assertEquals(entry1.hashCode(), entry2.hashCode());
        assertNotEquals(entry1.hashCode(), entry3.hashCode());
    }

    /* -------------------------------------------------
     * Builder sourceMap initialisation and ordering compatibility
     * ------------------------------------------------- */
    @Test
    public void testBuilderWithSourceMap() {
        Map<String, Integer> src = new LinkedHashMap<>();
        src.put("first", 1);
        src.put("second", 2);

        CompactMap<String, Integer> built = CompactMap.<String, Integer>builder()
                .insertionOrder()
                .sourceMap(src)
                .build();

        assertEquals(2, built.size());
        Iterator<String> it = built.keySet().iterator();
        assertEquals("first", it.next());
        assertEquals("second", it.next());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderInvalidCompactSize() {
        CompactMap.<String, String>builder()
                .compactSize(1) // less than required minimum of 2
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderInvalidMapTypeForOrdering() {
        CompactMap.<String, String>builder()
                .mapType(HashMap.class)
                .sortedOrder() // HashMap does not support Sorted ordering
                .build();
    }
}
