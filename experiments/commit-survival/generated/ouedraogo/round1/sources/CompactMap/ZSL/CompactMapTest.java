package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class CompactMapTest {

    /**
     * A small‑size CompactMap so we can trigger the internal state changes
     * (EMPTY → OBJECT/ENTRY → ARRAY → MAP) without having to add many elements.
     */
    private static class SmallCompactMap<K, V> extends CompactMap<K, V> {
        @Override
        protected int compactSize() { return 3; }               // small threshold
        @Override
        protected Map<K, V> getNewMap() { return new HashMap<>(); }
        @Override
        protected boolean isCaseInsensitive() { return false; }
    }

    private SmallCompactMap<String, String> map;

    @Before
    public void setUp() {
        map = new SmallCompactMap<>();
    }

    /* -----------------------------------------------------------
     *  Basic empty‑map behaviour
     * ----------------------------------------------------------- */
    @Test
    public void testEmptyMap() {
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
        assertNull(map.get("any"));
        assertFalse(map.containsKey("any"));
        assertFalse(map.containsValue("any"));
        assertEquals("{}", map.toString());
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.getLogicalValueType());
    }

    /* -----------------------------------------------------------
     *  Single entry where key matches getSingleValueKey()
     * ----------------------------------------------------------- */
    @Test
    public void testSingleEntryWithDefaultKey() {
        // default getSingleValueKey() returns "key"
        map.put("key", "value1");
        assertEquals(1, map.size());
        assertFalse(map.isEmpty());
        assertEquals("value1", map.get("key"));
        assertTrue(map.containsKey("key"));
        assertTrue(map.containsValue("value1"));
        assertEquals(CompactMap.LogicalValueType.OBJECT, map.getLogicalValueType());

        // Overwrite the same key
        String old = map.put("key", "value2");
        assertEquals("value1", old);
        assertEquals("value2", map.get("key"));
    }

    /* -----------------------------------------------------------
     *  Single entry where key does NOT match getSingleValueKey()
     * ----------------------------------------------------------- */
    @Test
    public void testSingleEntryWithCustomKey() {
        map.put("custom", "val");
        assertEquals(1, map.size());
        assertEquals(CompactMap.LogicalValueType.ENTRY, map.getLogicalValueType());

        // verify entrySet contains the correct entry
        Set<Map.Entry<String, String>> entries = map.entrySet();
        assertEquals(1, entries.size());
        Map.Entry<String, String> e = entries.iterator().next();
        assertEquals("custom", e.getKey());
        assertEquals("val", e.getValue());

        // test CompactMapEntry#setValue propagates to the map
        CompactMap<String, String>.CompactMapEntry entry = (CompactMap<String, String>.CompactMapEntry) e;
        String previous = entry.setValue("newVal");
        assertEquals("val", previous);
        assertEquals("newVal", map.get("custom"));
    }

    /* -----------------------------------------------------------
     *  Transition to ARRAY (2‑3 entries)
     * ----------------------------------------------------------- */
    @Test
    public void testArrayTransition() {
        map.put("key", "v1");          // size 1 (OBJECT)
        map.put("a", "v2");            // size 2 → ARRAY
        assertEquals(2, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());

        // third entry still ARRAY (compactSize = 3)
        map.put("b", "v3");
        assertEquals(3, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());

        // verify contents via iteration
        Set<String> keys = new HashSet<>(Arrays.asList("key", "a", "b"));
        assertEquals(keys, map.keySet());

        // verify values
        Collection<String> values = map.values();
        assertTrue(values.containsAll(Arrays.asList("v1", "v2", "v3")));
    }

    /* -----------------------------------------------------------
     *  Transition to MAP (size > compactSize)
     * ----------------------------------------------------------- */
    @Test
    public void testMapTransition() {
        map.put("k1", "v1");
        map.put("k2", "v2");
        map.put("k3", "v3");   // still ARRAY (size == compactSize)
        map.put("k4", "v4");   // should switch to MAP
        assertEquals(4, map.size());
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType());

        // verify that the underlying map behaves like a normal HashMap
        assertTrue(map.containsKey("k2"));
        assertEquals("v2", map.get("k2"));
    }

    /* -----------------------------------------------------------
     *  Removal causing shrink back to ARRAY / ENTRY / OBJECT
     * ----------------------------------------------------------- */
    @Test
    public void testRemovalShrinksStructure() {
        // Build map with 4 entries (MAP state)
        map.put("a", "1");
        map.put("b", "2");
        map.put("c", "3");
        map.put("d", "4");
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType());

        // Remove one entry – still MAP (size 3 == compactSize)
        map.remove("d");
        assertEquals(3, map.size());
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType());

        // Remove another entry – should convert to ARRAY (size 2)
        map.remove("c");
        assertEquals(2, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());

        // Remove one more – should become ENTRY (size 1)
        map.remove("b");
        assertEquals(1, map.size());
        assertEquals(CompactMap.LogicalValueType.ENTRY, map.getLogicalValueType());

        // Finally remove last – back to EMPTY
        map.remove("a");
        assertEquals(0, map.size());
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.getLogicalValueType());
    }

    /* -----------------------------------------------------------
     *  putAll handling for small and large collections
     * ----------------------------------------------------------- */
    @Test
    public void testPutAll() {
        Map<String, String> src = new HashMap<>();
        src.put("x", "1");
        src.put("y", "2");   // size 2 → ARRAY
        map.putAll(src);
        assertEquals(2, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());

        // add more to exceed compactSize
        src.put("z", "3");
        src.put("w", "4");   // total 4 → MAP
        map.putAll(src);
        assertEquals(4, map.size());
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType());
    }

    /* -----------------------------------------------------------
     *  equals() and hashCode() against a regular HashMap
     * ----------------------------------------------------------- */
    @Test
    public void testEqualsAndHashCode() {
        map.put("a", "1");
        map.put("b", "2");
        map.put("c", "3"); // ARRAY state

        Map<String, String> other = new HashMap<>();
        other.put("a", "1");
        other.put("b", "2");
        other.put("c", "3");

        assertTrue(map.equals(other));
        assertTrue(other.equals(map));
        assertEquals(map.hashCode(), other.hashCode());

        // change a value – equality should break
        other.put("b", "X");
        assertFalse(map.equals(other));
    }

    /* -----------------------------------------------------------
     *  toString formatting
     * ----------------------------------------------------------- */
    @Test
    public void testToString() {
        assertEquals("{}", map.toString());

        map.put("key", "value");
        assertEquals("{key=value}", map.toString());

        map.put("a", "1");
        map.put("b", "2");
        // Order is undefined for HashMap, but for ARRAY representation we can sort for assertion
        String s = map.toString();
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        // Ensure all expected key=value pairs appear
        assertTrue(s.contains("key=value"));
        assertTrue(s.contains("a=1"));
        assertTrue(s.contains("b=2"));
    }

    /* -----------------------------------------------------------
     *  keySet, values, entrySet collection behaviours
     * ----------------------------------------------------------- */
    @Test
    public void testCollectionViews() {
        map.put("k1", "v1");
        map.put("k2", "v2");
        map.put("k3", "v3"); // ARRAY state

        // keySet remove
        Set<String> ks = map.keySet();
        assertTrue(ks.remove("k2"));
        assertFalse(map.containsKey("k2"));
        assertEquals(2, map.size());

        // values iterator remove
        Iterator<String> vit = map.values().iterator();
        while (vit.hasNext()) {
            String v = vit.next();
            if ("v3".equals(v)) {
                vit.remove();
            }
        }
        assertFalse(map.containsValue("v3"));
        assertEquals(1, map.size());

        // entrySet clear
        map.entrySet().clear();
        assertTrue(map.isEmpty());
    }

    /* -----------------------------------------------------------
     *  Iterator fail‑fast and illegal state checks
     * ----------------------------------------------------------- */
    @Test(expected = IllegalStateException.class)
    public void testIteratorRemoveBeforeNext() {
        map.put("a", "1");
        Iterator<String> it = map.keySet().iterator();
        it.remove(); // should throw IllegalStateException
    }

    @Test(expected = ConcurrentModificationException.class)
    public void testIteratorConcurrentModification() {
        map.put("a", "1");
        map.put("b", "2");
        Iterator<String> it = map.keySet().iterator();
        assertTrue(it.hasNext());
        map.put("c", "3"); // structural change after iterator creation
        it.next(); // should detect concurrent modification
    }

    /* -----------------------------------------------------------
     *  CompactMapEntry equality and hashCode (case‑insensitive support)
     * ----------------------------------------------------------- */
    @Test
    public void testCompactMapEntryEqualityAndHashCode() {
        map.put("alpha", "A");
        Map.Entry<String, String> e1 = map.entrySet().iterator().next();
        CompactMap<String, String>.CompactMapEntry entry = (CompactMap<String, String>.CompactMapEntry) e1;

        // equal to a simple SimpleEntry with same key/value
        Map.Entry<String, String> simple = new AbstractMap.SimpleEntry<>("alpha", "A");
        assertTrue(entry.equals(simple));
        assertEquals(entry.hashCode(), simple.hashCode());

        // different value – not equal
        Map.Entry<String, String> diff = new AbstractMap.SimpleEntry<>("alpha", "B");
        assertFalse(entry.equals(diff));
    }

    /* -----------------------------------------------------------
     *  Case‑insensitive key handling (override isCaseInsensitive)
     * ----------------------------------------------------------- */
    private static class CaseInsensitiveCompactMap extends SmallCompactMap<String, String> {
        @Override protected boolean isCaseInsensitive() { return true; }
    }

    @Test
    public void testCaseInsensitiveKeys() {
        CaseInsensitiveCompactMap ciMap = new CaseInsensitiveCompactMap();
        ciMap.put("Key", "value");
        assertTrue(ciMap.containsKey("key"));
        assertEquals("value", ciMap.get("KEY"));
        // verify removal is also case‑insensitive
        assertEquals("value", ciMap.remove("kEy"));
        assertTrue(ciMap.isEmpty());
    }

    /* -----------------------------------------------------------
     *  computeKeyHashCode and computeValueHashCode edge cases
     * ----------------------------------------------------------- */
    @Test
    public void testHashCodeUtilities() {
        // null key → 0
        assertEquals(0, map.computeKeyHashCode(null));

        // self‑reference key
        map.put("self", "value");
        assertEquals(37, map.computeKeyHashCode(map)); // map == this

        // regular string key (case‑sensitive)
        assertEquals("abc".hashCode(), map.computeKeyHashCode("abc"));

        // value hash codes
        assertEquals(0, map.computeValueHashCode(null));
        assertEquals(17, map.computeValueHashCode(map)); // self‑reference value
        assertEquals("val".hashCode(), map.computeValueHashCode("val"));
    }

    /* -----------------------------------------------------------
     *  verify iterator implementations (Compact vs Copy)
     * ----------------------------------------------------------- */
    @Test
    public void testIterators() {
        // Force ARRAY state
        map.put("1", "one");
        map.put("2", "two");

        // CompactKeyIterator
        Iterator<String> ckIt = map.new CompactKeyIterator();
        Set<String> ckKeys = new HashSet<>();
        while (ckIt.hasNext()) {
            ckKeys.add(ckIt.next());
        }
        assertEquals(new HashSet<>(Arrays.asList("1", "2")), ckKeys);

        // CompactValueIterator
        Iterator<String> cvIt = map.new CompactValueIterator();
        Set<String> cvVals = new HashSet<>();
        while (cvIt.hasNext()) {
            cvVals.add(cvIt.next());
        }
        assertEquals(new HashSet<>(Arrays.asList("one", "two")), cvVals);

        // CompactEntryIterator
        Iterator<Map.Entry<String, String>> ceIt = map.new CompactEntryIterator();
        Map<String, String> collected = new HashMap<>();
        while (ceIt.hasNext()) {
            Map.Entry<String, String> e = ceIt.next();
            collected.put(e.getKey(), e.getValue());
        }
        assertEquals(map, collected);
    }
}
