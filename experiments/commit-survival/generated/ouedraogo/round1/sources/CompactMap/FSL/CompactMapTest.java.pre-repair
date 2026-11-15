package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.Test;

/**
 * Test suite for {@link CompactMap}.  The tests use a small {@code compactSize}
 * (3) to exercise the internal state transitions (empty → single → array →
 * map) without needing a large number of entries.
 */
public class CompactMapTest {

    /** Subclass that makes the protected members accessible for testing. */
    static class TestableCompactMap extends CompactMap<String, String> {
        @Override
        protected int compactSize() { return 3; }   // small size to trigger transitions

        public LogicalValueType logicalValueType() { return super.getLogicalValueType(); }

        public int computeKeyHash(Object key) { return super.computeKeyHashCode(key); }

        public int computeValueHash(Object value) { return super.computeValueHashCode(value); }
    }

    /** Helper to create a map with the given entries (preserves insertion order). */
    private TestableCompactMap mapOf(Object... kv) {
        TestableCompactMap map = new TestableCompactMap();
        for (int i = 0; i < kv.length; i += 2) {
            map.put((String) kv[i], (String) kv[i + 1]);
        }
        return map;
    }

    @Test
    public void testEmptyMap() {
        CompactMap<String, String> map = new TestableCompactMap();
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
        assertFalse(map.containsKey("any"));
        assertFalse(map.containsValue("any"));
        assertNull(map.get("any"));
        assertEquals(0, map.hashCode());
        assertEquals("{}", map.toString());

        Map<String, String> empty = new HashMap<>();
        assertTrue(map.equals(empty));
        assertTrue(empty.equals(map));
    }

    @Test
    public void testSingleEntryWithDefaultKey() {
        TestableCompactMap map = new TestableCompactMap();
        // default single‑value key is "key"
        map.put("key", "value");
        assertEquals(1, map.size());
        assertFalse(map.isEmpty());
        assertTrue(map.containsKey("key"));
        assertTrue(map.containsValue("value"));
        assertEquals("value", map.get("key"));
        assertEquals(LogicalValueType.OBJECT, map.logicalValueType());

        // replace the same key – should return previous value
        String old = map.put("key", "new");
        assertEquals("value", old);
        assertEquals("new", map.get("key"));
    }

    @Test
    public void testSingleEntryWithCustomKey() {
        TestableCompactMap map = new TestableCompactMap();
        map.put("a", "alpha");
        assertEquals(1, map.size());
        assertTrue(map.containsKey("a"));
        assertTrue(map.containsValue("alpha"));
        assertEquals("alpha", map.get("a"));
        assertEquals(LogicalValueType.ENTRY, map.logicalValueType());

        // test entrySet contains the right entry
        Set<Map.Entry<String, String>> entries = map.entrySet();
        assertEquals(1, entries.size());
        Map.Entry<String, String> e = entries.iterator().next();
        assertEquals("a", e.getKey());
        assertEquals("alpha", e.getValue());

        // test CompactMapEntry#setValue propagates to the map
        e.setValue("beta");
        assertEquals("beta", map.get("a"));
    }

    @Test
    public void testTransitionToArray() {
        TestableCompactMap map = new TestableCompactMap();
        map.put("a", "1");
        map.put("b", "2");               // second entry – should become an array
        assertEquals(2, map.size());
        assertEquals(LogicalValueType.ARRAY, map.logicalValueType());

        assertTrue(map.containsKey("a"));
        assertTrue(map.containsKey("b"));
        assertEquals("1", map.get("a"));
        assertEquals("2", map.get("b"));

        // iterator over keys
        Iterator<String> ki = map.keySet().iterator();
        Set<String> ks = new HashSet<>();
        while (ki.hasNext()) ks.add(ki.next());
        assertEquals(new HashSet<>(Arrays.asList("a", "b")), ks);
    }

    @Test
    public void testArrayToMapTransition() {
        TestableCompactMap map = new TestableCompactMap();
        map.put("a", "1");
        map.put("b", "2");
        map.put("c", "3");               // still array (size == compactSize)
        assertEquals(LogicalValueType.ARRAY, map.logicalValueType());

        map.put("d", "4");               // size 4 > compactSize => map
        assertEquals(4, map.size());
        assertEquals(LogicalValueType.MAP, map.logicalValueType());

        // verify all entries are present
        assertEquals("1", map.get("a"));
        assertEquals("2", map.get("b"));
        assertEquals("3", map.get("c"));
        assertEquals("4", map.get("d"));
    }

    @Test
    public void testRemovalFromArrayToSingle() {
        TestableCompactMap map = new TestableCompactMap();
        map.put("x", "10");
        map.put("y", "20");               // array of size 2
        assertEquals(LogicalValueType.ARRAY, map.logicalValueType());

        // remove one entry – should shrink to single entry (ENTRY type)
        String removed = map.remove("x");
        assertEquals("10", removed);
        assertEquals(1, map.size());
        assertEquals(LogicalValueType.ENTRY, map.logicalValueType());
        assertTrue(map.containsKey("y"));
    }

    @Test
    public void testRemovalFromMapBackToArray() {
        TestableCompactMap map = new TestableCompactMap();
        // force map state
        map.put("a", "1");
        map.put("b", "2");
        map.put("c", "3");
        map.put("d", "4");               // now map
        assertEquals(LogicalValueType.MAP, map.logicalValueType());

        // remove one entry – size becomes 3 == compactSize, should switch back to array
        map.remove("d");
        assertEquals(3, map.size());
        assertEquals(LogicalValueType.ARRAY, map.logicalValueType());
        assertTrue(map.containsKey("a"));
        assertTrue(map.containsKey("b"));
        assertTrue(map.containsKey("c"));
    }

    @Test
    public void testPutAllTriggersMapWhenLarge() {
        TestableCompactMap map = new TestableCompactMap();
        Map<String, String> big = new HashMap<>();
        for (int i = 0; i < 5; i++) {
            big.put("k" + i, "v" + i);
        }
        map.putAll(big);                 // 5 > compactSize => map directly
        assertEquals(5, map.size());
        assertEquals(LogicalValueType.MAP, map.logicalValueType());
        for (int i = 0; i < 5; i++) {
            assertEquals("v" + i, map.get("k" + i));
        }
    }

    @Test
    public void testKeySetRemoveAndFailFast() {
        TestableCompactMap map = mapOf("a","1","b","2","c","3");
        Iterator<String> it = map.keySet().iterator();
        assertTrue(it.hasNext());
        String first = it.next();
        it.remove();                     // remove via iterator
        assertEquals(2, map.size());
        assertFalse(map.containsKey(first));

        // fail‑fast: modify map after iterator creation
        Iterator<String> it2 = map.keySet().iterator();
        map.put("d","4");
        try {
            it2.hasNext();
            fail("Expected ConcurrentModificationException");
        } catch (ConcurrentModificationException e) {
            // expected
        }
    }

    @Test
    public void testEntrySetContainsAndRemove() {
        TestableCompactMap map = mapOf("x","100","y","200");
        Set<Map.Entry<String,String>> es = map.entrySet();

        Map.Entry<String,String> probe = new AbstractMap.SimpleEntry<>("x","100");
        assertTrue(es.contains(probe));

        // remove via entry set
        assertTrue(es.remove(probe));
        assertEquals(1, map.size());
        assertFalse(map.containsKey("x"));
        assertTrue(map.containsKey("y"));
    }

    @Test
    public void testValuesIteratorAndRemove() {
        TestableCompactMap map = mapOf("p","P","q","Q","r","R");
        Iterator<String> vi = map.values().iterator();
        List<String> vals = new ArrayList<>();
        while (vi.hasNext()) {
            vals.add(vi.next());
        }
        assertEquals(new HashSet<>(Arrays.asList("P","Q","R")), new HashSet<>(vals));

        // remove a value via iterator
        Iterator<String> vi2 = map.values().iterator();
        while (vi2.hasNext()) {
            String v = vi2.next();
            if ("Q".equals(v)) {
                vi2.remove();
            }
        }
        assertEquals(2, map.size());
        assertFalse(map.containsValue("Q"));
    }

    @Test
    public void testEqualsAndHashCodeWithStandardMap() {
        TestableCompactMap map = mapOf("a","A","b","B","c","C");
        Map<String,String> other = new HashMap<>();
        other.put("a","A");
        other.put("b","B");
        other.put("c","C");
        assertTrue(map.equals(other));
        assertTrue(other.equals(map));
        assertEquals(other.hashCode(), map.hashCode());

        // change one value – equality must break
        map.put("c","Z");
        assertFalse(map.equals(other));
        assertFalse(other.equals(map));
    }

    @Test
    public void testToStringFormatting() {
        TestableCompactMap map = new TestableCompactMap();
        assertEquals("{}", map.toString());

        map.put("x", "1");
        assertEquals("{x=1}", map.toString());

        map.put("y", "2");
        // order is unspecified for HashMap‑backed map, but for array state order is insertion order
        String s = map.toString();
        assertTrue(s.equals("{x=1, y=2}") || s.equals("{y=2, x=1}"));
    }

    @Test
    public void testCompactMapEntryEqualsAndHashCode() {
        TestableCompactMap map = new TestableCompactMap();
        map.put("k", "v");
        Map.Entry<String,String> entry = map.entrySet().iterator().next();

        // equality with another entry having same key/value
        Map.Entry<String,String> same = new AbstractMap.SimpleEntry<>("k","v");
        assertTrue(entry.equals(same));
        assertEquals(entry.hashCode(), same.hashCode());

        // case where key differs
        Map.Entry<String,String> diffKey = new AbstractMap.SimpleEntry<>("k2","v");
        assertFalse(entry.equals(diffKey));

        // case where value differs
        Map.Entry<String,String> diffVal = new AbstractMap.SimpleEntry<>("k","v2");
        assertFalse(entry.equals(diffVal));
    }

    @Test
    public void testLogicalValueTypeTransitions() {
        TestableCompactMap map = new TestableCompactMap();
        assertEquals(LogicalValueType.EMPTY, map.logicalValueType());

        map.put("key", "val");                 // single OBJECT
        assertEquals(LogicalValueType.OBJECT, map.logicalValueType());

        map.clear();
        map.put("a","1");                      // ENTRY (key != default)
        assertEquals(LogicalValueType.ENTRY, map.logicalValueType());

        map.put("b","2");                      // ARRAY (size 2)
        assertEquals(LogicalValueType.ARRAY, map.logicalValueType());

        map.put("c","3");                      // still ARRAY (size == compactSize)
        assertEquals(LogicalValueType.ARRAY, map.logicalValueType());

        map.put("d","4");                      // MAP (size > compactSize)
        assertEquals(LogicalValueType.MAP, map.logicalValueType());
    }

    @Test
    public void testComputeKeyAndValueHashCode() {
        TestableCompactMap map = new TestableCompactMap();
        // normal string key
        assertEquals("test".hashCode(), map.computeKeyHash("test"));

        // null key
        assertEquals(0, map.computeKeyHash(null));

        // key that is the map itself
        assertEquals(37, map.computeKeyHash(map));

        // value hash
        assertEquals(0, map.computeValueHash(null));
        assertEquals(17, map.computeValueHash(map));
        assertEquals("val".hashCode(), map.computeValueHash("val"));
    }

    @Test
    public void testCopyIteratorBehaviour() {
        // Force the copy iterator path by using a SortedMap (TreeMap) which makes useCopyIterator true
        TestableCompactMap map = new TestableCompactMap() {
            @Override
            protected Map<String, String> getNewMap() {
                return new java.util.TreeMap<>();
            }
        };
        map.put("a","1");
        map.put("b","2");

        // key iterator should be a CopyKeyIterator (order sorted)
        Iterator<String> ki = map.keySet().iterator();
        List<String> keys = new ArrayList<>();
        while (ki.hasNext()) keys.add(ki.next());
        assertEquals(Arrays.asList("a","b"), keys); // TreeMap order

        // value iterator
        Iterator<String> vi = map.values().iterator();
        List<String> vals = new ArrayList<>();
        while (vi.hasNext()) vals.add(vi.next());
        assertEquals(Arrays.asList("1","2"), vals);
    }
}
