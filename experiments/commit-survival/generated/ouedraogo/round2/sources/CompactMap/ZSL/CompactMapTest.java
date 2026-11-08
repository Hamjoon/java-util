package com.cedarsoftware.util;
import com.cedarsoftware.util.CompactMap.LogicalValueType;


import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class CompactMapTest {

    /*** Helper subclass that allows us to control the compactSize and map implementation ***/
    private static class SmallCompactMap extends CompactMap<String, Integer> {
        @Override
        protected int compactSize() { return 3; }               // small limit to trigger transitions quickly
        @Override
        protected Map<String, Integer> getNewMap() { return new HashMap<>(); }
        /** expose protected methods for testing */
        public LogicalValueType logicalValueType() { return getLogicalValueType(); }
        public int computeKeyHash(Object key) { return computeKeyHashCode(key); }
        public int computeValueHash(Object value) { return computeValueHashCode(value); }
    }

    /*** Subclass that forces a SortedMap (TreeMap) to be used, activating copy‑iterator paths ***/
    private static class SortedCompactMap extends SmallCompactMap {
        @Override
        protected Map<String, Integer> getNewMap() { return new TreeMap<>(); }
    }

    /*** Subclass that enables case‑insensitive key handling ***/
    private static class CaseInsensitiveCompactMap extends SmallCompactMap {
        @Override
        protected boolean isCaseInsensitive() { return true; }
    }

    private SmallCompactMap map;

    @Before
    public void setUp() {
        map = new SmallCompactMap();
    }

    @Test
    public void testEmptyMapBehavior() {
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
        assertNull(map.get("anything"));
        assertFalse(map.containsKey("anything"));
        assertFalse(map.containsValue(123));
        assertEquals("{}", map.toString());
    }

    @Test
    public void testSingleEntryWithSingleValueKey() {
        // the default single‑value key is "key"
        assertNull(map.put("key", 42));
        assertEquals(1, map.size());
        assertFalse(map.isEmpty());
        assertEquals(Integer.valueOf(42), map.get("key"));
        assertTrue(map.containsKey("key"));
        assertTrue(map.containsValue(42));
        assertEquals(LogicalValueType.OBJECT, map.logicalValueType());

        // overwrite same key
        Integer old = map.put("key", 99);
        assertEquals(Integer.valueOf(42), old);
        assertEquals(Integer.valueOf(99), map.get("key"));
    }

    @Test
    public void testSingleEntryWithNonSingleValueKey() {
        assertNull(map.put("foo", 7));
        assertEquals(1, map.size());
        assertTrue(map.containsKey("foo"));
        assertEquals(Integer.valueOf(7), map.get("foo"));
        assertEquals(LogicalValueType.ENTRY, map.logicalValueType());

        // ensure entrySet contains the correct entry
        Set<Map.Entry<String, Integer>> entries = map.entrySet();
        assertEquals(1, entries.size());
        Map.Entry<String, Integer> e = entries.iterator().next();
        assertEquals("foo", e.getKey());
        assertEquals(Integer.valueOf(7), e.getValue());
    }

    @Test
    public void testArrayExpansionAndMapTransition() {
        // Fill up to compactSize (3) – should be stored in an Object[]
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assertEquals(3, map.size());
        assertEquals(LogicalValueType.ARRAY, map.logicalValueType());

        // Adding the fourth entry forces a switch to a regular Map
        map.put("d", 4);
        assertEquals(4, map.size());
        assertEquals(LogicalValueType.MAP, map.logicalValueType());

        // Verify all entries are present
        for (char ch = 'a'; ch <= 'd'; ch++) {
            String k = String.valueOf(ch);
            assertTrue(map.containsKey(k));
            assertEquals(Integer.valueOf(ch - 'a' + 1), map.get(k));
        }

        // Remove one entry – size goes back to compactSize (3) and should convert back to array
        map.remove("d");
        assertEquals(3, map.size());
        assertEquals(LogicalValueType.ARRAY, map.logicalValueType());

        // Remove another entry – array shrinks to size 2
        map.remove("c");
        assertEquals(2, map.size());
        assertEquals(LogicalValueType.ARRAY, map.logicalValueType());

        // Remove one more – should become a single entry (OBJECT or ENTRY)
        map.remove("b");
        assertEquals(1, map.size());
        LogicalValueType type = map.logicalValueType();
        assertTrue(type == LogicalValueType.OBJECT || type == LogicalValueType.ENTRY);
    }

    @Test
    public void testRemoveFromArraySizeTwoTransitionsToSingle() {
        map.put("x", 10);
        map.put("y", 20); // now size == 2, stored as array
        assertEquals(LogicalValueType.ARRAY, map.logicalValueType());

        // Remove "x" – should drop to single entry using CompactMapEntry
        Integer removed = map.remove("x");
        assertEquals(Integer.valueOf(10), removed);
        assertEquals(1, map.size());
        assertTrue(map.containsKey("y"));
        assertEquals(Integer.valueOf(20), map.get("y"));
    }

    @Test
    public void testEqualsAndHashCode() {
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        map.put("four", 4); // forces map transition

        Map<String, Integer> reference = new HashMap<>();
        reference.put("one", 1);
        reference.put("two", 2);
        reference.put("three", 3);
        reference.put("four", 4);

        assertEquals(reference, map);
        assertEquals(reference.hashCode(), map.hashCode());

        // change a value and ensure inequality
        map.put("four", 44);
        assertNotEquals(reference, map);
    }

    @Test
    public void testToStringFormatting() {
        map.put("alpha", 100);
        map.put("beta", 200);
        String s = map.toString();
        // Order is not guaranteed for HashMap, so we check both possibilities
        assertTrue(s.equals("{alpha=100, beta=200}") || s.equals("{beta=200, alpha=100}"));
    }

    @Test
    public void testEntrySetIteratorRemove() {
        map.put("k1", 1);
        map.put("k2", 2);
        map.put("k3", 3);
        map.put("k4", 4); // map transition

        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Integer> e = it.next();
            if (e.getKey().equals("k2")) {
                it.remove();
            }
        }
        assertEquals(3, map.size());
        assertFalse(map.containsKey("k2"));
        assertEquals(Integer.valueOf(1), map.get("k1"));
        assertEquals(Integer.valueOf(3), map.get("k3"));
        assertEquals(Integer.valueOf(4), map.get("k4"));
    }

    @Test
    public void testCopyIteratorBehaviour() {
        SortedCompactMap sortedMap = new SortedCompactMap();
        sortedMap.put("a", 1);
        sortedMap.put("b", 2);
        sortedMap.put("c", 3); // still array (size <= compactSize)

        // obtain iterator (should be copy‑iterator because underlying map is a SortedMap)
        Iterator<String> keyIter = sortedMap.keySet().iterator();
        assertTrue(keyIter instanceof SortedCompactMap.CopyKeyIterator);

        // modify the map after iterator creation – copy iterator must NOT see the new entry and must NOT throw CME
        sortedMap.put("d", 4);
        List<String> captured = new ArrayList<>();
        while (keyIter.hasNext()) {
            captured.add(keyIter.next());
        }
        assertEquals(Arrays.asList("a", "b", "c"), captured);
        // the newly added entry is still present in the map
        assertTrue(sortedMap.containsKey("d"));
    }

    @Test
    public void testCompactMapEntrySetValuePropagation() {
        map.put("foo", 5);
        Map.Entry<String, Integer> entry = map.entrySet().iterator().next();
        assertEquals(Integer.valueOf(5), entry.getValue());

        // change value via entry.setValue()
        entry.setValue(55);
        assertEquals(Integer.valueOf(55), map.get("foo"));
    }

    @Test
    public void testCaseInsensitiveKeyComparison() {
        CaseInsensitiveCompactMap ciMap = new CaseInsensitiveCompactMap();
        ciMap.put("Key", 123);
        // lookup with different case should succeed
        assertTrue(ciMap.containsKey("key"));
        assertEquals(Integer.valueOf(123), ciMap.get("kEy"));
    }

    @Test
    public void testComputeKeyAndValueHashCodes() {
        // OBJECT type (single value key)
        assertEquals("key".hashCode(), map.computeKeyHash("key"));
        // null key hash should be 0
        assertEquals(0, map.computeKeyHash(null));

        // test value hash
        assertEquals(0, map.computeValueHash(null));
        assertEquals(Integer.valueOf(99).hashCode(), map.computeValueHash(99));
        // value equal to the map itself should yield 17
        assertEquals(17, map.computeValueHash(map));
    }

    @Test
    public void testKeySetAndValuesCollections() {
        map.put("x", 10);
        map.put("y", 20);
        map.put("z", 30);
        Set<String> keys = map.keySet();
        Collection<Integer> values = map.values();

        assertTrue(keys.containsAll(Arrays.asList("x", "y", "z")));
        assertTrue(values.containsAll(Arrays.asList(10, 20, 30)));

        // remove via keySet
        keys.remove("y");
        assertFalse(map.containsKey("y"));
        assertEquals(2, map.size());

        // clear via values collection
        values.clear();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testLogicalValueTypeTransitions() {
        // start empty
        assertEquals(LogicalValueType.EMPTY, map.logicalValueType());

        // single entry with default key -> OBJECT
        map.put("key", 1);
        assertEquals(LogicalValueType.OBJECT, map.logicalValueType());

        // replace with different key -> ENTRY
        map.clear();
        map.put("other", 2);
        assertEquals(LogicalValueType.ENTRY, map.logicalValueType());

        // array
        map.clear();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(LogicalValueType.ARRAY, map.logicalValueType());

        // map
        map.put("c", 3);
        map.put("d", 4); // triggers map transition (size 4 > compactSize 3)
        assertEquals(LogicalValueType.MAP, map.logicalValueType());
    }
}
