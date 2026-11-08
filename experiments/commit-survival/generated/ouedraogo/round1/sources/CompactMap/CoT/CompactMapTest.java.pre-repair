package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.*;

public class CompactMapTest {

    /**
     * A small‑size CompactMap used to force transitions between the
     * internal representations (single value, array, Map) without having
     * to insert dozens of elements.
     */
    private static class SmallCompactMap<K, V> extends CompactMap<K, V> {
        private final int sizeLimit;

        SmallCompactMap(int compactSize) {
            this.sizeLimit = compactSize;
        }

        @Override
        protected int compactSize() {
            return sizeLimit;
        }

        // expose the protected enum for assertions
        public LogicalValueType logicalValueType() {
            return getLogicalValueType();
        }
    }

    private CompactMap<String, String> map;
    private SmallCompactMap<Integer, String> smallMap;

    @Before
    public void setUp() {
        map = new CompactMap<>();
        // use a compactSize of 3 so that 4 entries force a Map transition
        smallMap = new SmallCompactMap<>(3);
    }

    /*** 1. Public method signatures ***/
    // Constructors
    //   CompactMap()
    //   CompactMap(Map<K,V> other)

    // Map interface methods (public in CompactMap)
    //   int size()
    //   boolean isEmpty()
    //   boolean containsKey(Object key)
    //   boolean containsValue(Object value)
    //   V get(Object key)
    //   V put(K key, V value)
    //   V remove(Object key)
    //   void putAll(Map<? extends K, ? extends V> map)
    //   void clear()
    //   int hashCode()
    //   boolean equals(Object obj)
    //   String toString()
    //   Set<K> keySet()
    //   Collection<V> values()
    //   Set<Entry<K,V>> entrySet()
    //   Map<K,V> minus(Object removeMe)
    //   Map<K,V> plus(Object right)

    /*** 2. Basic functionality tests ***/

    @Test
    public void testEmptyMap() {
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
        assertFalse(map.containsKey("anything"));
        assertFalse(map.containsValue("anything"));
        assertNull(map.get("anything"));
        assertEquals("{}", map.toString());
    }

    @Test
    public void testSingleEntryWithDefaultSingleValueKey() {
        // The default single‑value key is "key"
        assertNull(map.put("key", "value1"));
        assertEquals(1, map.size());
        assertEquals("value1", map.get("key"));
        // Overwrite same key – should return previous value
        assertEquals("value1", map.put("key", "value2"));
        assertEquals("value2", map.get("key"));
    }

    @Test
    public void testSingleEntryDifferentKeyCreatesArray() {
        assertNull(map.put("a", "1"));
        assertEquals(1, map.size());
        assertNull(map.put("b", "2")); // second distinct key forces array
        assertEquals(2, map.size());
        assertEquals("1", map.get("a"));
        assertEquals("2", map.get("b"));
    }

    @Test
    public void testOverwriteInArray() {
        map.put("x", "old");
        map.put("y", "yval"); // now array with 2 entries
        assertEquals("old", map.put("x", "new"));
        assertEquals("new", map.get("x"));
        assertEquals("yval", map.get("y"));
    }

    @Test
    public void testTransitionToMapWhenExceedingCompactSize() {
        // compactSize() == 3, so inserting 4 entries switches to a Map
        assertNull(smallMap.put(1, "one"));
        assertNull(smallMap.put(2, "two"));
        assertNull(smallMap.put(3, "three"));
        // still array (size == compactSize)
        assertEquals(SmallCompactMap.LogicalValueType.ARRAY, smallMap.logicalValueType());
        assertNull(smallMap.put(4, "four")); // triggers Map transition
        assertEquals(SmallCompactMap.LogicalValueType.MAP, smallMap.logicalValueType());
        assertEquals(4, smallMap.size());
        assertEquals("four", smallMap.get(4));
    }

    @Test
    public void testRemoveWhenSizeIsTwoShrinksToSingle() {
        map.put("k1", "v1");
        map.put("k2", "v2"); // size == 2 (array)
        assertEquals(2, map.size());

        // remove one key, should shrink to single entry
        assertEquals("v1", map.remove("k1"));
        assertEquals(1, map.size());
        // remaining entry must be still accessible
        assertEquals("v2", map.get("k2"));
    }

    @Test
    public void testRemoveFromMapAndSwitchBackToArray() {
        // Fill to cause Map transition
        smallMap.put(1, "one");
        smallMap.put(2, "two");
        smallMap.put(3, "three");
        smallMap.put(4, "four"); // now Map
        assertEquals(SmallCompactMap.LogicalValueType.MAP, smallMap.logicalValueType());

        // Remove one entry – size becomes compactSize (3) → should convert back to array
        assertEquals("four", smallMap.remove(4));
        assertEquals(3, smallMap.size());
        assertEquals(SmallCompactMap.LogicalValueType.ARRAY, smallMap.logicalValueType());
    }

    @Test
    public void testPutAllLargerThanCompactSizeCreatesMapDirectly() {
        Map<Integer, String> many = new HashMap<>();
        many.put(10, "ten");
        many.put(20, "twenty");
        many.put(30, "thirty");
        many.put(40, "forty"); // 4 entries > compactSize (3)

        smallMap.putAll(many);
        assertEquals(4, smallMap.size());
        assertEquals(SmallCompactMap.LogicalValueType.MAP, smallMap.logicalValueType());
        assertEquals("twenty", smallMap.get(20));
    }

    @Test
    public void testContainsKeyAndValueWithNulls() {
        map.put(null, null);
        assertTrue(map.containsKey(null));
        assertTrue(map.containsValue(null));
        assertNull(map.get(null));
    }

    @Test
    public void testEqualsAndHashCode() {
        CompactMap<String, String> m1 = new CompactMap<>();
        CompactMap<String, String> m2 = new CompactMap<>();
        m1.put("a", "1");
        m1.put("b", "2");
        m2.put("b", "2");
        m2.put("a", "1");

        assertTrue(m1.equals(m2));
        assertTrue(m2.equals(m1));
        assertEquals(m1.hashCode(), m2.hashCode());

        m2.put("c", "3");
        assertFalse(m1.equals(m2));
    }

    @Test
    public void testToStringMultipleEntries() {
        map.put("one", "1");
        map.put("two", "2");
        String s = map.toString();
        // Order is not guaranteed, but both entries must appear
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        assertTrue(s.contains("one=1"));
        assertTrue(s.contains("two=2"));
    }

    /*** 3. Edge‑case & exception tests ***/

    @Test(expected = UnsupportedOperationException.class)
    public void testMinusUnsupported() {
        map.minus("any");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPlusUnsupported() {
        map.plus("any");
    }

    @Test
    public void testIteratorRemoveWithoutNextThrows() {
        map.put("x", "1");
        Iterator<String> it = map.keySet().iterator();
        try {
            it.remove();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    @Test
    public void testIteratorConcurrentModificationException() {
        map.put("a", "1");
        map.put("b", "2");
        Iterator<String> it = map.keySet().iterator();
        assertTrue(it.hasNext());
        // modify the map after iterator creation
        map.put("c", "3");
        try {
            it.next();
            fail("Expected ConcurrentModificationException");
        } catch (ConcurrentModificationException e) {
            // expected
        }
    }

    @Test
    public void testCopyIteratorsBehaviour() {
        // Force array representation (size 2)
        map.put("k1", "v1");
        map.put("k2", "v2");

        // CopyKeyIterator
        Iterator<String> copyKeyIt = map.keySet().iterator(); // uses copy iterator when underlying map is SortedMap; our map uses HashMap → normal iterator, but we can still test functionality
        Set<String> collectedKeys = new HashSet<>();
        while (copyKeyIt.hasNext()) {
            collectedKeys.add(copyKeyIt.next());
        }
        assertEquals(new HashSet<>(Arrays.asList("k1", "k2")), collectedKeys);

        // CopyValueIterator
        Iterator<String> copyValIt = map.values().iterator();
        Set<String> collectedVals = new HashSet<>();
        while (copyValIt.hasNext()) {
            collectedVals.add(copyValIt.next());
        }
        assertEquals(new HashSet<>(Arrays.asList("v1", "v2")), collectedVals);

        // CopyEntryIterator
        Iterator<Map.Entry<String, String>> copyEntIt = map.entrySet().iterator();
        Map<String, String> collectedMap = new HashMap<>();
        while (copyEntIt.hasNext()) {
            Map.Entry<String, String> e = copyEntIt.next();
            collectedMap.put(e.getKey(), e.getValue());
        }
        assertEquals(2, collectedMap.size());
        assertEquals("v1", collectedMap.get("k1"));
        assertEquals("v2", collectedMap.get("k2"));
    }

    @Test
    public void testCompactMapEntryEqualsAndHashCode() {
        CompactMap<String, String> m = new CompactMap<>();
        m.put("alpha", "A");
        Map.Entry<String, String> entry = m.entrySet().iterator().next(); // should be a CompactMapEntry
        assertTrue(entry instanceof CompactMap.CompactMapEntry);
        CompactMap.CompactMapEntry cmEntry = (CompactMap.CompactMapEntry) entry;

        // equals with another entry having same key/value
        Map.Entry<String, String> other = new AbstractMap.SimpleEntry<>("alpha", "A");
        assertTrue(cmEntry.equals(other));
        assertEquals(cmEntry.hashCode(), other.hashCode());

        // not equal if value differs
        Map.Entry<String, String> diff = new AbstractMap.SimpleEntry<>("alpha", "B");
        assertFalse(cmEntry.equals(diff));
    }

    @Test
    public void testClearResetsMap() {
        map.put("x", "1");
        map.put("y", "2");
        assertFalse(map.isEmpty());
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertFalse(map.containsKey("x"));
    }
}
