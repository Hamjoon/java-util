/*
 * Comprehensive JUnit4 test suite for com.cedarsoftware.util.CompactMap
 *
 * The tests cover:
 *   • Empty map behavior
 *   • Single entry (both key equals getSingleValueKey() and regular entry)
 *   • Array storage (2‑4 entries) and transition to a backing Map
 *   • Removal and shrinking back to smaller representations
 *   • Logical value type detection
 *   • Iterators (key, value, entry) – normal and copy iterators
 *   • keySet, values, entrySet collection views
 *   • equals, hashCode, toString
 *   • CompactMapEntry setValue “write‑through” semantics
 *
 * The CompactMap implementation is protected in several places; we expose those
 * members via a concrete subclass used only for testing.
 */

package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.util.*;

import static org.junit.Assert.*;

public class CompactMapTest {

    /**
     * Test subclass that makes the compact size small (3) so we can trigger the
     * transition from array storage to a backing Map quickly.
     */
    private static class SmallCompactMap<K, V> extends CompactMap<K, V> {
        @Override
        protected int compactSize() {
            return 3; // small size to force map transition
        }

        // expose protected helpers for testing
        public LogicalValueType getLogicalValueTypePublic() {
            return super.getLogicalValueType();
        }

        public K getLogicalSingleKeyPublic() {
            return super.getLogicalSingleKey();
        }

        public V getLogicalSingleValuePublic() {
            return super.getLogicalSingleValue();
        }
    }

    private SmallCompactMap<String, String> map;

    @Before
    public void setUp() {
        map = new SmallCompactMap<>();
    }

    @Test
    public void testEmptyMapBehavior() {
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertFalse(map.containsKey("any"));
        assertFalse(map.containsValue("any"));
        assertNull(map.get("any"));
        assertEquals("{}", map.toString());

        // equals with another empty map
        SmallCompactMap<String, String> other = new SmallCompactMap<>();
        assertEquals(map, other);
        assertEquals(0, map.hashCode());

        // logical value type should be EMPTY
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.getLogicalValueTypePublic());
    }

    @Test
    public void testSingleEntryWithSingleValueKey() {
        // The default getSingleValueKey() returns "key"
        map.put("key", "singleValue");
        assertFalse(map.isEmpty());
        assertEquals(1, map.size());
        assertTrue(map.containsKey("key"));
        assertTrue(map.containsValue("singleValue"));
        assertEquals("singleValue", map.get("key"));
        assertEquals("singleValue", map.getLogicalSingleValuePublic());
        assertEquals("key", map.getLogicalSingleKeyPublic());

        // internal representation should be OBJECT (value only)
        assertEquals(CompactMap.LogicalValueType.OBJECT, map.getLogicalValueTypePublic());

        // overwrite the same key
        String old = map.put("key", "newValue");
        assertEquals("singleValue", old);
        assertEquals("newValue", map.get("key"));
    }

    @Test
    public void testSingleEntryWithDifferentKey() {
        map.put("otherKey", "value");
        assertEquals(1, map.size());
        assertTrue(map.containsKey("otherKey"));
        assertEquals("value", map.get("otherKey"));

        // internal representation should be ENTRY
        assertEquals(CompactMap.LogicalValueType.ENTRY, map.getLogicalValueTypePublic());

        // test entrySet contains the correct entry
        Set<Map.Entry<String, String>> entries = map.entrySet();
        assertEquals(1, entries.size());
        Map.Entry<String, String> e = entries.iterator().next();
        assertEquals("otherKey", e.getKey());
        assertEquals("value", e.getValue());

        // test CompactMapEntry setValue writes through to the map
        SmallCompactMap<String, String>.CompactMapEntry entry = (SmallCompactMap<String, String>.CompactMapEntry) e;
        entry.setValue("changed");
        assertEquals("changed", map.get("otherKey"));
    }

    @Test
    public void testArrayStorageAndTransitionToMap() {
        // compactSize = 3, so 2 entries stay in array, 4th forces map transition
        map.put("a", "1");
        map.put("b", "2");
        assertEquals(2, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueTypePublic());

        // verify iterator over keys/values/entries works in array mode
        Iterator<String> keyIt = map.keySet().iterator();
        Set<String> keys = new HashSet<>();
        while (keyIt.hasNext()) {
            keys.add(keyIt.next());
        }
        assertEquals(new HashSet<>(Arrays.asList("a", "b")), keys);

        Iterator<String> valIt = map.values().iterator();
        Set<String> vals = new HashSet<>();
        while (valIt.hasNext()) {
            vals.add(valIt.next());
        }
        assertEquals(new HashSet<>(Arrays.asList("1", "2")), vals);

        // Add third entry – still array (size == compactSize)
        map.put("c", "3");
        assertEquals(3, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueTypePublic());

        // Add fourth entry – should switch to Map
        map.put("d", "4");
        assertEquals(4, map.size());
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueTypePublic());

        // Ensure all entries are present after transition
        assertEquals("1", map.get("a"));
        assertEquals("2", map.get("b"));
        assertEquals("3", map.get("c"));
        assertEquals("4", map.get("d"));
    }

    @Test
    public void testRemovalAndShrinkingBack() {
        // Populate to trigger map transition
        map.put("a", "1");
        map.put("b", "2");
        map.put("c", "3");
        map.put("d", "4"); // now in MAP mode
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueTypePublic());

        // Remove entries until we drop back to array mode (size == compactSize)
        map.remove("d");
        assertEquals(3, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueTypePublic());

        // Remove one more to go to 2 entries (still array)
        map.remove("c");
        assertEquals(2, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueTypePublic());

        // Remove one more – should shrink to single entry representation
        map.remove("b");
        assertEquals(1, map.size());
        // The remaining key is "a"
        assertTrue(map.containsKey("a"));
        assertEquals("1", map.get("a"));
        // Depending on key, representation may be OBJECT or ENTRY
        // Since key != getSingleValueKey() ("key"), it will be ENTRY
        assertEquals(CompactMap.LogicalValueType.ENTRY, map.getLogicalValueTypePublic());

        // Remove the last entry – back to EMPTY
        map.remove("a");
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.getLogicalValueTypePublic());
    }

    @Test
    public void testKeySetCollectionView() {
        map.put("x", "10");
        map.put("y", "20");

        Set<String> ks = map.keySet();
        assertTrue(ks.contains("x"));
        assertTrue(ks.contains("y"));
        assertEquals(2, ks.size());

        // remove via keySet
        ks.remove("x");
        assertFalse(map.containsKey("x"));
        assertEquals(1, map.size());

        // retainAll via keySet
        ks.retainAll(Collections.singleton("y"));
        assertEquals(1, map.size());
        assertTrue(map.containsKey("y"));

        // clear via keySet
        ks.clear();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testValuesCollectionView() {
        map.put("p", "alpha");
        map.put("q", "beta");

        Collection<String> vals = map.values();
        assertTrue(vals.contains("alpha"));
        assertTrue(vals.contains("beta"));
        assertEquals(2, vals.size());

        // clear via values collection
        vals.clear();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testEntrySetOperations() {
        map.put("k1", "v1");
        map.put("k2", "v2");
        map.put("k3", "v3");

        Set<Map.Entry<String, String>> es = map.entrySet();
        assertEquals(3, es.size());

        // contains check
        Map.Entry<String, String> sample = new AbstractMap.SimpleEntry<>("k2", "v2");
        assertTrue(es.contains(sample));

        // remove via entrySet
        es.remove(sample);
        assertFalse(map.containsKey("k2"));
        assertEquals(2, map.size());

        // retainAll via entrySet
        Map.Entry<String, String> keep = new AbstractMap.SimpleEntry<>("k1", "v1");
        es.retainAll(Arrays.asList(keep));
        assertEquals(1, map.size());
        assertTrue(map.containsKey("k1"));
    }

    @Test
    public void testEqualsAndHashCode() {
        map.put("a", "1");
        map.put("b", "2");

        Map<String, String> other = new HashMap<>();
        other.put("a", "1");
        other.put("b", "2");

        assertTrue(map.equals(other));
        assertTrue(other.equals(map));
        assertEquals(map.hashCode(), other.hashCode());

        // modify other
        other.put("c", "3");
        assertFalse(map.equals(other));
    }

    @Test
    public void testToStringFormatting() {
        assertEquals("{}", map.toString());

        map.put("one", "1");
        assertEquals("{one=1}", map.toString());

        map.put("two", "2");
        // order is not guaranteed for HashMap; accept both possible orders
        String s = map.toString();
        assertTrue(s.equals("{one=1, two=2}") || s.equals("{two=2, one=1}"));
    }

    @Test
    public void testIteratorsRemoveAndConcurrentModification() {
        map.put("a", "1");
        map.put("b", "2");
        map.put("c", "3"); // still array mode (size==3)

        Iterator<String> it = map.keySet().iterator();
        assertTrue(it.hasNext());
        assertEquals("a", it.next());

        // remove via iterator
        it.remove();
        assertFalse(map.containsKey("a"));
        assertEquals(2, map.size());

        // verify concurrent modification detection
        Iterator<String> it2 = map.keySet().iterator();
        map.put("d", "4"); // structural change after iterator creation
        try {
            it2.next();
            fail("Expected ConcurrentModificationException");
        } catch (ConcurrentModificationException expected) {
            // expected
        }
    }

    @Test
    public void testCopyIteratorsWhenUsingSortedMap() {
        // Override getNewMap to return a TreeMap (SortedMap) to force copy iterators
        SmallCompactMap<String, String> sortedMap = new SmallCompactMap<String, String>() {
            @Override
            protected Map<String, String> getNewMap() {
                return new TreeMap<>();
            }
        };
        sortedMap.put("x", "1");
        sortedMap.put("y", "2");
        sortedMap.put("z", "3"); // still array mode (size 3)

        // The iterators should be copy iterators because underlying map would be SortedMap
        Iterator<String> keyIter = sortedMap.keySet().iterator();
        List<String> keyList = new ArrayList<>();
        while (keyIter.hasNext()) {
            keyList.add(keyIter.next());
        }
        Collections.sort(keyList);
        assertEquals(Arrays.asList("x", "y", "z"), keyList);
    }

    @Test
    public void testCompactMapEntryEqualsAndHashCode() {
        SmallCompactMap<String, String>.CompactMapEntry e1 =
                map.new CompactMapEntry("k", "v");
        SmallCompactMap<String, String>.CompactMapEntry e2 =
                map.new CompactMapEntry("k", "v");
        SmallCompactMap<String, String>.CompactMapEntry e3 =
                map.new CompactMapEntry("k", "different");

        assertEquals(e1, e2);
        assertNotEquals(e1, e3);
        assertEquals(e1.hashCode(), e2.hashCode());
        assertNotEquals(e1.hashCode(), e3.hashCode());
    }

    @Test
    public void testPutAllBehavior() {
        Map<String, String> src = new HashMap<>();
        src.put("a", "1");
        src.put("b", "2");
        src.put("c", "3");

        // putAll into empty map – size <= compactSize, stays in array mode
        map.putAll(src);
        assertEquals(3, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueTypePublic());

        // add more to exceed compactSize (3) – should switch to MAP
        map.put("d", "4");
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueTypePublic());
    }

    @Test
    public void testComputeKeyAndValueHashCodesIndirectly() {
        // Use default getSingleValueKey() = "key"
        map.put("key", "val");
        // hashCode for map with single entry should be computeKeyHashCode ^ computeValueHashCode
        int expected = map.computeKeyHashCode("key") ^ map.computeValueHashCode("val");
        assertEquals(expected, map.hashCode());
    }
}
