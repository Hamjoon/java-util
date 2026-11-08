package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.Test;

/**
 * Unit tests for {@link CompactMap}.  The tests use a small {@code compactSize}
 * (3) so that the internal representation changes from EMPTY → SINGLE → ARRAY → MAP
 * can be exercised without inserting many entries.
 */
public class CompactMapTest {

    /** Subclass that makes the {@code compactSize} small and exposes some protected helpers. */
    private static class TestCompactMap extends CompactMap<String, String> {
        @Override
        protected int compactSize() { return 3; }   // small size to trigger ARRAY → MAP quickly

        /** Expose the logical value type for assertions. */
        public LogicalValueType getLogicalValueTypePublic() {
            return super.getLogicalValueType();
        }

        /** Expose the single value key (default is "key") for clarity. */
        public String getSingleValueKeyPublic() {
            return super.getSingleValueKey();
        }
    }

    /** Helper to create a map with given entries. */
    private static TestCompactMap mapOf(Object... kv) {
        TestCompactMap map = new TestCompactMap();
        for (int i = 0; i < kv.length; i += 2) {
            map.put(String.valueOf(kv[i]), String.valueOf(kv[i + 1]));
        }
        return map;
    }

    @Test
    public void testEmptyMap() {
        CompactMap<String, String> map = new TestCompactMap();
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
        assertFalse(map.containsKey("any"));
        assertFalse(map.containsValue("any"));
        assertNull(map.get("any"));
        assertEquals("{}", map.toString());
        assertEquals(CompactMap.LogicalValueType.EMPTY,
                ((TestCompactMap) map).getLogicalValueTypePublic());
    }

    @Test
    public void testSingleEntryWithDefaultKey() {
        TestCompactMap map = new TestCompactMap();
        // The default single value key is "key"
        map.put("key", "value");
        assertEquals(1, map.size());
        assertFalse(map.isEmpty());
        assertTrue(map.containsKey("key"));
        assertTrue(map.containsValue("value"));
        assertEquals("value", map.get("key"));
        assertEquals(CompactMap.LogicalValueType.OBJECT,
                map.getLogicalValueTypePublic());

        // Overwrite the same key
        String old = map.put("key", "new");
        assertEquals("value", old);
        assertEquals("new", map.get("key"));
    }

    @Test
    public void testSingleEntryDifferentKey() {
        TestCompactMap map = new TestCompactMap();
        map.put("foo", "bar");
        assertEquals(1, map.size());
        assertTrue(map.containsKey("foo"));
        assertTrue(map.containsValue("bar"));
        assertEquals("bar", map.get("foo"));
        assertEquals(CompactMap.LogicalValueType.ENTRY,
                map.getLogicalValueTypePublic());

        // Remove the entry
        String removed = map.remove("foo");
        assertEquals("bar", removed);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testArrayTransition() {
        TestCompactMap map = new TestCompactMap();
        // Add two distinct keys -> should become ARRAY representation
        map.put("a", "1");
        map.put("b", "2");
        assertEquals(2, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY,
                map.getLogicalValueTypePublic());

        // Verify retrieval
        assertEquals("1", map.get("a"));
        assertEquals("2", map.get("b"));
        assertTrue(map.containsKey("a"));
        assertTrue(map.containsKey("b"));
        assertTrue(map.containsValue("1"));
        assertTrue(map.containsValue("2"));

        // Add third entry, still ARRAY (size == compactSize)
        map.put("c", "3");
        assertEquals(3, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY,
                map.getLogicalValueTypePublic());

        // Add fourth entry -> should switch to MAP
        map.put("d", "4");
        assertEquals(4, map.size());
        assertEquals(CompactMap.LogicalValueType.MAP,
                map.getLogicalValueTypePublic());

        // Verify all entries are present
        assertEquals("1", map.get("a"));
        assertEquals("2", map.get("b"));
        assertEquals("3", map.get("c"));
        assertEquals("4", map.get("d"));
    }

    @Test
    public void testRemoveFromArrayShrinksToEntry() {
        TestCompactMap map = new TestCompactMap();
        map.put("x", "10");
        map.put("y", "20");   // now ARRAY with 2 entries
        assertEquals(CompactMap.LogicalValueType.ARRAY,
                map.getLogicalValueTypePublic());

        // Remove one key, should shrink back to SINGLE (ENTRY)
        String removed = map.remove("x");
        assertEquals("10", removed);
        assertEquals(1, map.size());
        assertEquals(CompactMap.LogicalValueType.ENTRY,
                map.getLogicalValueTypePublic());
        assertEquals("20", map.get("y"));
    }

    @Test
    public void testMapTransitionAndShrinkBack() {
        TestCompactMap map = new TestCompactMap();
        // Fill up to exceed compactSize (3) -> MAP
        map.put("1", "one");
        map.put("2", "two");
        map.put("3", "three");
        map.put("4", "four"); // now MAP
        assertEquals(CompactMap.LogicalValueType.MAP,
                map.getLogicalValueTypePublic());

        // Remove entries until size == compactSize (3) -> should switch back to ARRAY
        map.remove("4");
        assertEquals(3, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY,
                map.getLogicalValueTypePublic());

        // Verify remaining entries
        assertEquals("one", map.get("1"));
        assertEquals("two", map.get("2"));
        assertEquals("three", map.get("3"));
    }

    @Test
    public void testKeySetAndValuesIterators() {
        TestCompactMap map = mapOf("k1", "v1", "k2", "v2", "k3", "v3");
        Set<String> keys = map.keySet();
        Collection<String> values = map.values();

        // Verify iterator order (HashMap order is unspecified, so just check contents)
        Set<String> keyCopy = new HashSet<>(keys);
        assertEquals(new HashSet<>(Arrays.asList("k1","k2","k3")), keyCopy);

        Set<String> valueCopy = new HashSet<>(values);
        assertEquals(new HashSet<>(Arrays.asList("v1","v2","v3")), valueCopy);

        // Remove via keySet iterator
        Iterator<String> kit = keys.iterator();
        while (kit.hasNext()) {
            String k = kit.next();
            if ("k2".equals(k)) {
                kit.remove();
            }
        }
        assertFalse(map.containsKey("k2"));
        assertEquals(2, map.size());

        // Remove via values iterator
        Iterator<String> vit = values.iterator();
        while (vit.hasNext()) {
            String v = vit.next();
            if ("v3".equals(v)) {
                vit.remove();
            }
        }
        assertFalse(map.containsValue("v3"));
        assertEquals(1, map.size());
    }

    @Test
    public void testEntrySetOperations() {
        TestCompactMap map = mapOf("a", "A", "b", "B");
        Set<Map.Entry<String, String>> entrySet = map.entrySet();

        // Verify contains
        Map.Entry<String, String> e = new AbstractMap.SimpleEntry<>("a", "A");
        assertTrue(entrySet.contains(e));

        // Remove via entrySet.remove
        boolean removed = entrySet.remove(e);
        assertTrue(removed);
        assertFalse(map.containsKey("a"));
        assertEquals(1, map.size());

        // Retain only a specific entry
        Map.Entry<String, String> keep = new AbstractMap.SimpleEntry<>("b", "B");
        entrySet.retainAll(Collections.singleton(keep));
        assertEquals(1, map.size());
        assertTrue(map.containsKey("b"));
    }

    @Test
    public void testEqualsAndHashCode() {
        TestCompactMap map1 = mapOf("x", "1", "y", "2");
        TestCompactMap map2 = mapOf("y", "2", "x", "1");
        HashMap<String, String> hash = new HashMap<>();
        hash.put("x", "1");
        hash.put("y", "2");

        assertTrue(map1.equals(map2));
        assertTrue(map2.equals(map1));
        assertTrue(map1.equals(hash));
        assertTrue(hash.equals(map1));

        assertEquals(map1.hashCode(), map2.hashCode());
        assertEquals(map1.hashCode(), hash.hashCode());
    }

    @Test
    public void testToStringFormatting() {
        TestCompactMap map = new TestCompactMap();
        assertEquals("{}", map.toString());

        map.put("key", "value");
        assertEquals("{key=value}", map.toString());

        map.put("self", null);
        // null value prints "null"
        String s = map.toString();
        assertTrue(s.contains("self=null"));
        assertTrue(s.contains("key=value"));
    }

    @Test
    public void testCompactMapEntrySetValuePropagation() {
        TestCompactMap map = new TestCompactMap();
        map.put("foo", "bar"); // single entry (ENTRY)
        CompactMap<String, String>.CompactMapEntry entry =
                (CompactMap<String, String>.CompactMapEntry) map.entrySet().iterator().next();

        // Change via entry.setValue should update the map
        entry.setValue("newBar");
        assertEquals("newBar", map.get("foo"));
        assertEquals("newBar", entry.getValue());
    }

    @Test
    public void testCopyIteratorsProduceConsistentSnapshot() {
        TestCompactMap map = mapOf("a", "1", "b", "2");
        // Force copy iterator path by using a SortedMap (TreeMap) as the backing map.
        TestCompactMap sortedMap = new TestCompactMap() {
            @Override
            protected Map<String, String> getNewMap() {
                return new java.util.TreeMap<>();
            }
        };
        sortedMap.putAll(map);

        // Verify that copy iterators see the same elements even after modification
        Iterator<String> keyIter = sortedMap.keySet().iterator(); // copy iterator due to SortedMap
        Iterator<String> valIter = sortedMap.values().iterator();
        Iterator<Map.Entry<String, String>> entryIter = sortedMap.entrySet().iterator();

        // Modify original map after obtaining iterators
        sortedMap.put("c", "3");
        sortedMap.remove("a");

        // Snapshot should still contain original two entries only
        Set<String> snapshotKeys = new HashSet<>();
        while (keyIter.hasNext()) snapshotKeys.add(keyIter.next());
        assertEquals(new HashSet<>(Arrays.asList("a","b")), snapshotKeys);

        Set<String> snapshotVals = new HashSet<>();
        while (valIter.hasNext()) snapshotVals.add(valIter.next());
        assertEquals(new HashSet<>(Arrays.asList("1","2")), snapshotVals);

        Set<Map.Entry<String, String>> snapshotEntries = new HashSet<>();
        while (entryIter.hasNext()) snapshotEntries.add(entryIter.next());
        assertEquals(2, snapshotEntries.size());
        assertTrue(snapshotEntries.contains(new AbstractMap.SimpleEntry<>("a","1")));
        assertTrue(snapshotEntries.contains(new AbstractMap.SimpleEntry<>("b","2")));
    }

    @Test(expected = IllegalStateException.class)
    public void testIteratorRemoveWithoutNext() {
        TestCompactMap map = mapOf("x", "X");
        Iterator<String> it = map.keySet().iterator();
        // Directly call remove before next()
        it.remove();
    }

    @Test(expected = ConcurrentModificationException.class)
    public void testIteratorDetectsConcurrentModification() {
        TestCompactMap map = mapOf("p", "P", "q", "Q");
        Iterator<String> it = map.keySet().iterator();
        assertTrue(it.hasNext());
        it.next();
        // Modify map directly
        map.put("r", "R");
        // Next iterator operation should detect modification
        it.next();
    }
}
