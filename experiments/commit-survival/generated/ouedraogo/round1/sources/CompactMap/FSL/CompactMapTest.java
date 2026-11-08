package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class CompactMapTest {

    /** A CompactMap with a very small compactSize so we can trigger the
     *  array‑to‑map transition quickly. */
    private static class SmallCompactMap<K, V> extends CompactMap<K, V> {
        private final int maxSize;

        SmallCompactMap(int maxSize) {
            this.maxSize = maxSize;
        }

        @Override
        protected int compactSize() {
            return maxSize;
        }

        @Override
        protected Map<K, V> getNewMap() {
            // default map – a plain HashMap (unsorted)
            return new HashMap<>(compactSize() + 1);
        }
    }

    /** Same as SmallCompactMap but forces useCopyIterator() to be true
     *  by returning a SortedMap (TreeMap). */
    private static class SortedCompactMap<K, V> extends SmallCompactMap<K, V> {
        SortedCompactMap(int maxSize) {
            super(maxSize);
        }

        @Override
        protected Map<K, V> getNewMap() {
            // TreeMap implements SortedMap → useCopyIterator() will be true
            return new TreeMap<>();
        }

        @Override
        protected boolean useCopyIterator() {
            // force copy iterator path regardless of the map type
            return true;
        }
    }

    private SmallCompactMap<String, String> map;

    @Before
    public void setUp() {
        // compactSize = 3 gives us: EMPTY → SINGLE → ARRAY (2‑3) → MAP (4+)
        map = new SmallCompactMap<>(3);
    }

    @Test
    public void testEmptyState() {
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.getLogicalValueType());
    }

    @Test
    public void testSingleEntryUsingSingleValueKey() {
        // key equals getSingleValueKey() ("key")
        map.put("key", "value1");
        assertEquals(1, map.size());
        assertEquals("value1", map.get("key"));
        assertTrue(map.containsKey("key"));
        assertFalse(map.containsKey("other"));
        assertEquals(CompactMap.LogicalValueType.OBJECT, map.getLogicalValueType());

        // overwrite same key
        String old = map.put("key", "value2");
        assertEquals("value1", old);
        assertEquals("value2", map.get("key"));
    }

    @Test
    public void testSingleEntryUsingDifferentKey() {
        map.put("a", "b");
        assertEquals(1, map.size());
        assertEquals("b", map.get("a"));
        assertTrue(map.containsKey("a"));
        assertEquals(CompactMap.LogicalValueType.ENTRY, map.getLogicalValueType());

        // overwrite same key
        String old = map.put("a", "c");
        assertEquals("b", old);
        assertEquals("c", map.get("a"));
    }

    @Test
    public void testTransitionToArray() {
        map.put("a", "1");               // single entry (ENTRY)
        map.put("b", "2");               // becomes array of 2 entries
        assertEquals(2, map.size());
        assertTrue(map.containsKey("a"));
        assertTrue(map.containsKey("b"));
        assertEquals("1", map.get("a"));
        assertEquals("2", map.get("b"));
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());
    }

    @Test
    public void testArrayGrowthAndMapTransition() {
        // fill up to compactSize (3) → still array
        map.put("k1", "v1");
        map.put("k2", "v2");
        map.put("k3", "v3");
        assertEquals(3, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());

        // fourth entry forces transition to Map
        map.put("k4", "v4");
        assertEquals(4, map.size());
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType());

        // verify all entries are present
        for (int i = 1; i <= 4; i++) {
            assertEquals("v" + i, map.get("k" + i));
        }
    }

    @Test
    public void testRemoveFromArrayToSingle() {
        map.put("x", "1");
        map.put("y", "2"); // now array of 2
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());

        // remove one entry, should shrink back to SINGLE
        String removed = map.remove("x");
        assertEquals("1", removed);
        assertEquals(1, map.size());
        assertEquals("2", map.get("y"));
        // internal type now ENTRY (because key != singleValueKey)
        assertEquals(CompactMap.LogicalValueType.ENTRY, map.getLogicalValueType());
    }

    @Test
    public void testRemoveFromMapToArray() {
        // create map with 4 entries → internal MAP
        map.put("a", "1");
        map.put("b", "2");
        map.put("c", "3");
        map.put("d", "4");
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType());

        // remove one entry → size becomes compactSize (3) → should become ARRAY
        map.remove("d");
        assertEquals(3, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());
        assertTrue(map.containsKey("a"));
        assertTrue(map.containsKey("b"));
        assertTrue(map.containsKey("c"));
    }

    @Test
    public void testPutAllTransitions() {
        // putAll with > compactSize entries must go straight to MAP
        Map<String, String> big = new HashMap<>();
        big.put("1", "v1");
        big.put("2", "v2");
        big.put("3", "v3");
        big.put("4", "v4"); // 4 > compactSize (3)
        map.putAll(big);
        assertEquals(4, map.size());
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType());
        assertEquals("v3", map.get("3"));
    }

    @Test
    public void testEqualsAndHashCode() {
        // Build a map with three entries (array representation)
        map.put("a", "1");
        map.put("b", "2");
        map.put("c", "3");

        // Reference HashMap with same entries
        Map<String, String> ref = new HashMap<>();
        ref.put("a", "1");
        ref.put("b", "2");
        ref.put("c", "3");

        assertTrue(map.equals(ref));
        assertTrue(ref.equals(map));
        assertEquals(ref.hashCode(), map.hashCode());

        // Change a value – equality should break
        map.put("b", "X");
        assertFalse(map.equals(ref));
    }

    @Test
    public void testToStringFormatting() {
        map.put("one", "1");
        map.put("two", "2");
        String s = map.toString();
        // Order is not guaranteed for HashMap; just check the format
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        assertTrue(s.contains("one=1"));
        assertTrue(s.contains("two=2"));
    }

    @Test
    public void testKeySetIteratorRemove() {
        map.put("k1", "v1");
        map.put("k2", "v2");
        map.put("k3", "v3"); // array representation (size 3)

        Iterator<String> it = map.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            if ("k2".equals(key)) {
                it.remove(); // remove via iterator
            }
        }

        assertEquals(2, map.size());
        assertFalse(map.containsKey("k2"));
        assertTrue(map.containsKey("k1"));
        assertTrue(map.containsKey("k3"));
    }

    @Test
    public void testEntrySetCopyIteratorRetainAll() {
        SortedCompactMap<String, String> sortedMap = new SortedCompactMap<>(3);
        sortedMap.put("a", "1");
        sortedMap.put("b", "2");
        sortedMap.put("c", "3"); // will use copy iterator (SortedMap)

        // retain only entries a and c
        List<Map.Entry<String, String>> retain = new ArrayList<>();
        retain.add(new AbstractMap.SimpleEntry<>("a", "1"));
        retain.add(new AbstractMap.SimpleEntry<>("c", "3"));
        sortedMap.entrySet().retainAll(retain);

        assertEquals(2, sortedMap.size());
        assertTrue(sortedMap.containsKey("a"));
        assertTrue(sortedMap.containsKey("c"));
        assertFalse(sortedMap.containsKey("b"));
    }

    @Test
    public void testCompactMapEntryEqualsAndHashCode() {
        CompactMap<String, String>.CompactMapEntry entry1 = map.new CompactMapEntry("x", "y");
        CompactMap<String, String>.CompactMapEntry entry2 = map.new CompactMapEntry("x", "y");
        CompactMap<String, String>.CompactMapEntry entry3 = map.new CompactMapEntry("x", "z");

        assertEquals(entry1, entry2);
        assertNotEquals(entry1, entry3);
        assertEquals(entry1.hashCode(), entry2.hashCode());
        assertNotEquals(entry1.hashCode(), entry3.hashCode());
    }

    @Test(expected = ConcurrentModificationException.class)
    public void testIteratorConcurrentModification() {
        map.put("one", "1");
        map.put("two", "2");
        Iterator<String> it = map.keySet().iterator();
        assertTrue(it.hasNext());
        map.put("three", "3"); // modify after iterator creation
        it.next(); // should throw CME
    }

    @Test(expected = IllegalStateException.class)
    public void testIteratorRemoveWithoutNext() {
        map.put("a", "1");
        Iterator<String> it = map.keySet().iterator();
        // call remove before next()
        it.remove();
    }

    @Test
    public void testValuesIteratorAndCopyIteratorPath() {
        // Use SortedCompactMap to force copy iterator path for values()
        SortedCompactMap<String, String> sorted = new SortedCompactMap<>(3);
        sorted.put("k1", "v1");
        sorted.put("k2", "v2");

        Iterator<String> valIt = sorted.values().iterator();
        Set<String> seen = new HashSet<>();
        while (valIt.hasNext()) {
            seen.add(valIt.next());
        }
        assertEquals(new HashSet<>(Arrays.asList("v1", "v2")), seen);
    }

    @Test
    public void testClearResetsState() {
        map.put("x", "1");
        map.put("y", "2");
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.getLogicalValueType());
    }
}
