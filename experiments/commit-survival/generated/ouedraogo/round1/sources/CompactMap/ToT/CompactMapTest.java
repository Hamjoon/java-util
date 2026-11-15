package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link CompactMap}.
 *
 * The tests are written to exercise every public API method, the internal state
 * transitions (EMPTY → SINGLE → ARRAY → MAP and back), iterator behaviour,
 * equality / hashCode contracts and error handling.
 */
public class CompactMapTest {

    /** A CompactMap with a very small {@code compactSize} so that we can
     *  trigger the ARRAY → MAP transition quickly. */
    private static class TinyCompactMap<K, V> extends CompactMap<K, V> {
        @Override protected int compactSize() { return 3; }   // small to test transitions
    }

    private CompactMap<String, Integer> map;

    @Before
    public void setUp() {
        map = new CompactMap<>();
    }

    /* -------------------------------------------------------------------- */
    /*  Construction & basic empty‑state checks                               */
    /* -------------------------------------------------------------------- */

    @Test(expected = IllegalStateException.class)
    public void testConstructor_invalidCompactSize() {
        new CompactMap<String, Integer>() {
            @Override protected int compactSize() { return 1; } // illegal, < 2
        };
    }

    @Test
    public void testEmptyMapBehaviour() {
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.getLogicalValueType());
        assertEquals("{}", map.toString());
        assertTrue(map.keySet().isEmpty());
        assertTrue(map.values().isEmpty());
        assertTrue(map.entrySet().isEmpty());
    }

    /* -------------------------------------------------------------------- */
    /*  Single‑entry handling (key == getSingleValueKey())                    */
    /* -------------------------------------------------------------------- */

    @Test
    public void testSingleEntry_usingSingleValueKey() {
        // the default single‑value key is the literal string "key"
        map.put("key", 42);

        assertEquals(1, map.size());
        assertFalse(map.isEmpty());
        assertEquals(CompactMap.LogicalValueType.OBJECT, map.getLogicalValueType());

        // retrieval
        assertEquals(Integer.valueOf(42), map.get("key"));
        assertTrue(map.containsKey("key"));
        assertTrue(map.containsValue(42));

        // overwrite returns old value
        Integer old = map.put("key", 99);
        assertEquals(Integer.valueOf(42), old);
        assertEquals(Integer.valueOf(99), map.get("key"));

        // remove
        Integer removed = map.remove("key");
        assertEquals(Integer.valueOf(99), removed);
        assertTrue(map.isEmpty());
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.getLogicalValueType());
    }

    /* -------------------------------------------------------------------- */
    /*  Single‑entry handling (key != getSingleValueKey())                    */
    /* -------------------------------------------------------------------- */

    @Test
    public void testSingleEntry_otherKey() {
        map.put("alpha", 1);   // not the special “key”

        assertEquals(1, map.size());
        assertEquals(CompactMap.LogicalValueType.ENTRY, map.getLogicalValueType());

        assertEquals(Integer.valueOf(1), map.get("alpha"));
        assertTrue(map.containsKey("alpha"));
        assertTrue(map.containsValue(1));

        // entrySet contains the correct entry
        Set<Map.Entry<String, Integer>> entries = map.entrySet();
        assertEquals(1, entries.size());
        Map.Entry<String, Integer> e = entries.iterator().next();
        assertEquals("alpha", e.getKey());
        assertEquals(Integer.valueOf(1), e.getValue());

        // put a second key – should convert to ARRAY representation
        map.put("beta", 2);
        assertEquals(2, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());
        assertTrue(map.containsKey("beta"));
        assertTrue(map.containsValue(2));
    }

    /* -------------------------------------------------------------------- */
    /*  ARRAY representation (2 … compactSize entries)                        */
    /* -------------------------------------------------------------------- */

    @Test
    public void testArrayRepresentation() {
        map.put("a", 10);
        map.put("b", 20);
        map.put("c", 30);   // still ARRAY because default compactSize = 80

        assertEquals(3, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());

        // verify iterator order (keys, values, entries)
        List<String> keys = new ArrayList<>();
        for (String k : map.keySet()) keys.add(k);
        assertEquals(Arrays.asList("a", "b", "c"), keys);

        List<Integer> values = new ArrayList<>();
        for (Integer v : map.values()) values.add(v);
        assertEquals(Arrays.asList(10, 20, 30), values);

        Set<String> entryKeys = new HashSet<>();
        Set<Integer> entryValues = new HashSet<>();
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            entryKeys.add(e.getKey());
            entryValues.add(e.getValue());
        }
        assertEquals(new HashSet<>(Arrays.asList("a", "b", "c")), entryKeys);
        assertEquals(new HashSet<>(Arrays.asList(10, 20, 30)), entryValues);
    }

    @Test
    public void testArrayRemoveAndShrink() {
        map.put("x", 1);
        map.put("y", 2);
        map.put("z", 3);   // size = 3 (ARRAY)

        // remove one entry – still ARRAY (size 2)
        map.remove("y");
        assertEquals(2, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());
        assertFalse(map.containsKey("y"));
        assertNull(map.get("y"));

        // remove another entry – should fall back to SINGLE (ENTRY or OBJECT)
        map.remove("x");
        assertEquals(1, map.size());
        assertTrue(
                map.getLogicalValueType() == CompactMap.LogicalValueType.ENTRY ||
                map.getLogicalValueType() == CompactMap.LogicalValueType.OBJECT);
        assertTrue(map.containsKey("z"));
        assertEquals(Integer.valueOf(3), map.get("z"));
    }

    /* -------------------------------------------------------------------- */
    /*  MAP representation (size > compactSize)                               */
    /* -------------------------------------------------------------------- */

    @Test
    public void testTransitionToMapAndBack() {
        TinyCompactMap<String, Integer> tiny = new TinyCompactMap<>();

        // Fill up to compactSize (3) – stays ARRAY
        tiny.put("k1", 1);
        tiny.put("k2", 2);
        tiny.put("k3", 3);
        assertEquals(CompactMap.LogicalValueType.ARRAY, tiny.getLogicalValueType());

        // Add one more → should switch to MAP
        tiny.put("k4", 4);
        assertEquals(CompactMap.LogicalValueType.MAP, tiny.getLogicalValueType());
        assertEquals(4, tiny.size());

        // Verify map behaviour
        assertTrue(tiny.containsKey("k2"));
        assertEquals(Integer.valueOf(2), tiny.get("k2"));

        // Remove down to compactSize (3) – should switch back to ARRAY
        tiny.remove("k4");
        assertEquals(CompactMap.LogicalValueType.ARRAY, tiny.getLogicalValueType());
        assertEquals(3, tiny.size());
    }

    /* -------------------------------------------------------------------- */
    /*  put() overwrite semantics                                            */
    /* -------------------------------------------------------------------- */

    @Test
    public void testPutOverwriteReturnsOldValue() {
        map.put("one", 1);
        Integer old = map.put("one", 11);
        assertEquals(Integer.valueOf(1), old);
        assertEquals(Integer.valueOf(11), map.get("one"));
    }

    /* -------------------------------------------------------------------- */
    /*  putAll() – both ARRAY and MAP paths                                   */
    /* -------------------------------------------------------------------- */

    @Test
    public void testPutAllSmallMapStaysArray() {
        Map<String, Integer> src = new LinkedHashMap<>();
        src.put("a", 100);
        src.put("b", 200);
        map.putAll(src);

        assertEquals(2, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());
        assertEquals(Integer.valueOf(100), map.get("a"));
        assertEquals(Integer.valueOf(200), map.get("b"));
    }

    @Test
    public void testPutAllLargeMapSwitchesToMap() {
        TinyCompactMap<String, Integer> tiny = new TinyCompactMap<>();

        Map<String, Integer> src = new LinkedHashMap<>();
        src.put("p", 1);
        src.put("q", 2);
        src.put("r", 3);
        src.put("s", 4); // 4 > compactSize (3)

        tiny.putAll(src);
        assertEquals(4, tiny.size());
        assertEquals(CompactMap.LogicalValueType.MAP, tiny.getLogicalValueType());
    }

    /* -------------------------------------------------------------------- */
    /*  clear()                                                               */
    /* -------------------------------------------------------------------- */

    @Test
    public void testClearResetsState() {
        map.put("a", 1);
        map.put("b", 2);
        map.clear();

        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.getLogicalValueType());
    }

    /* -------------------------------------------------------------------- */
    /*  equals() / hashCode() – compare with other Map implementations          */
    /* -------------------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCodeWithHashMap() {
        map.put("x", 10);
        map.put("y", 20);

        Map<String, Integer> other = new HashMap<>();
        other.put("x", 10);
        other.put("y", 20);

        assertTrue(map.equals(other));
        assertEquals(map.hashCode(), other.hashCode());

        // change a value – equality must break
        map.put("y", 99);
        assertFalse(map.equals(other));
    }

    @Test
    public void testEqualsWithAnotherCompactMap() {
        CompactMap<String, Integer> m1 = new CompactMap<>();
        CompactMap<String, Integer> m2 = new CompactMap<>();

        m1.put("a", 1);
        m1.put("b", 2);

        m2.put("b", 2);
        m2.put("a", 1);

        assertTrue(m1.equals(m2));
        assertEquals(m1.hashCode(), m2.hashCode());
    }

    /* -------------------------------------------------------------------- */
    /*  toString() – format verification                                      */
    /* -------------------------------------------------------------------- */

    @Test
    public void testToStringFormatting() {
        assertEquals("{}", map.toString());

        map.put("k", 5);
        assertEquals("{key=5}", map.toString()); // default single‑value key is "key"

        map.put("other", 7);
        // order for ARRAY representation is insertion order
        assertEquals("{k=5, other=7}", map.toString());
    }

    /* -------------------------------------------------------------------- */
    /*  keySet() operations                                                   */
    /* -------------------------------------------------------------------- */

    @Test
    public void testKeySetContainsAndRemove() {
        map.put("a", 1);
        map.put("b", 2);

        Set<String> ks = map.keySet();
        assertTrue(ks.contains("a"));
        assertFalse(ks.contains("c"));

        // remove via keySet
        ks.remove("a");
        assertFalse(map.containsKey("a"));
        assertEquals(1, map.size());

        // retainAll – keep only "b"
        ks.retainAll(Collections.singleton("b"));
        assertEquals(1, map.size());
        assertTrue(map.containsKey("b"));
    }

    @Test
    public void testKeySetRemoveAll() {
        map.put("x", 10);
        map.put("y", 20);
        map.put("z", 30);

        Set<String> ks = map.keySet();
        ks.removeAll(Arrays.asList("x", "z"));
        assertEquals(1, map.size());
        assertTrue(map.containsKey("y"));
    }

    /* -------------------------------------------------------------------- */
    /*  values() iterator remove                                               */
    /* -------------------------------------------------------------------- */

    @Test
    public void testValuesIteratorRemove() {
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);

        Iterator<Integer> it = map.values().iterator();
        while (it.hasNext()) {
            Integer v = it.next();
            if (v.equals(2)) {
                it.remove();   // remove entry with value 2
            }
        }
        assertEquals(2, map.size());
        assertFalse(map.containsKey("two"));
    }

    /* -------------------------------------------------------------------- */
    /*  entrySet() contains / remove / retainAll                               */
    /* -------------------------------------------------------------------- */

    @Test
    public void testEntrySetContainsAndRemove() {
        map.put("k1", 100);
        map.put("k2", 200);

        Set<Map.Entry<String, Integer>> es = map.entrySet();
        Map.Entry<String, Integer> probe = new AbstractMap.SimpleEntry<>("k1", 100);
        assertTrue(es.contains(probe));

        // remove via entrySet
        es.remove(probe);
        assertFalse(map.containsKey("k1"));
        assertEquals(1, map.size());
    }

    @Test
    public void testEntrySetRetainAll() {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        Set<Map.Entry<String, Integer>> es = map.entrySet();
        List<Map.Entry<String, Integer>> toKeep = Arrays.asList(
                new AbstractMap.SimpleEntry<>("a", 1),
                new AbstractMap.SimpleEntry<>("c", 3)
        );
        es.retainAll(toKeep);
        assertEquals(2, map.size());
        assertTrue(map.containsKey("a"));
        assertTrue(map.containsKey("c"));
        assertFalse(map.containsKey("b"));
    }

    /* -------------------------------------------------------------------- */
    /*  Iterator fail‑fast behavior                                            */
    /* -------------------------------------------------------------------- */

    @Test(expected = ConcurrentModificationException.class)
    public void testIteratorConcurrentModification() {
        map.put("a", 1);
        map.put("b", 2);
        Iterator<String> it = map.keySet().iterator();
        map.put("c", 3);          // structural modification after iterator creation
        it.next();               // should throw CME
    }

    @Test(expected = IllegalStateException.class)
    public void testIteratorRemoveWithoutNext() {
        map.put("x", 10);
        Iterator<String> it = map.keySet().iterator();
        it.remove();   // illegal – next() not called yet
    }

    /* -------------------------------------------------------------------- */
    /*  computeKeyHashCode / computeValueHashCode edge cases                  */
    /* -------------------------------------------------------------------- */

    @Test
    public void testComputeKeyHashCodeSpecialCases() {
        // null key
        assertEquals(0, map.computeKeyHashCode(null));

        // key equal to the map itself
        assertEquals(37, map.computeKeyHashCode(map));

        // ordinary string key
        assertEquals("hello".hashCode(), map.computeKeyHashCode("hello"));
    }

    @Test
    public void testComputeValueHashCodeSpecialCases() {
        // null value
        assertEquals(0, map.computeValueHashCode(null));

        // value equal to the map itself
        assertEquals(17, map.computeValueHashCode(map));

        // ordinary integer value
        assertEquals(Integer.valueOf(123).hashCode(), map.computeValueHashCode(123));
    }

    /* -------------------------------------------------------------------- */
    /*  CompactKeyIterator, CompactValueIterator, CompactEntryIterator       */
    /* -------------------------------------------------------------------- */

    @Test
    public void testCompactIterators() {
        map.put("k1", 11);
        map.put("k2", 22);
        map.put("k3", 33);

        // key iterator
        Iterator<String> keyIt = map.keySet().iterator();
        List<String> keys = new ArrayList<>();
        while (keyIt.hasNext()) keys.add(keyIt.next());
        assertEquals(Arrays.asList("k1", "k2", "k3"), keys);

        // value iterator
        Iterator<Integer> valIt = map.values().iterator();
        List<Integer> vals = new ArrayList<>();
        while (valIt.hasNext()) vals.add(valIt.next());
        assertEquals(Arrays.asList(11, 22, 33), vals);

        // entry iterator
        Iterator<Map.Entry<String, Integer>> entryIt = map.entrySet().iterator();
        Map<String, Integer> collected = new HashMap<>();
        while (entryIt.hasNext()) {
            Map.Entry<String, Integer> e = entryIt.next();
            collected.put(e.getKey(), e.getValue());
        }
        assertEquals(map, collected);
    }

    /* -------------------------------------------------------------------- */
    /*  Copy iterators – default implementation uses HashMap (not SortedMap) */
    /* -------------------------------------------------------------------- */

    @Test
    public void testCopyIteratorsDefaultBehavior() {
        // useCopyIterator() returns false for the default HashMap, therefore
        // the map returns the compact (non‑copy) iterators.  We still verify
        // that the Copy*Iterator classes work when forced via reflection.
        map.put("a", 1);
        map.put("b", 2);

        // CopyKeyIterator
        CompactMap<String, Integer>.CopyKeyIterator copyKeyIt = map.new CopyKeyIterator();
        Set<String> copyKeys = new HashSet<>();
        while (copyKeyIt.hasNext()) copyKeys.add(copyKeyIt.next());
        assertEquals(new HashSet<>(Arrays.asList("a", "b")), copyKeys);

        // CopyValueIterator
        CompactMap<String, Integer>.CopyValueIterator copyValIt = map.new CopyValueIterator();
        Set<Integer> copyVals = new HashSet<>();
        while (copyValIt.hasNext()) copyVals.add(copyValIt.next());
        assertEquals(new HashSet<>(Arrays.asList(1, 2)), copyVals);

        // CopyEntryIterator
        CompactMap<String, Integer>.CopyEntryIterator copyEntryIt = map.new CopyEntryIterator();
        Map<String, Integer> copyMap = new HashMap<>();
        while (copyEntryIt.hasNext()) {
            Map.Entry<String, Integer> e = copyEntryIt.next();
            copyMap.put(e.getKey(), e.getValue());
        }
        assertEquals(map, copyMap);
    }

    /* -------------------------------------------------------------------- */
    /*  CompactMapEntry behaviour (equals / hashCode / setValue)              */
    /* -------------------------------------------------------------------- */

    @Test
    public void testCompactMapEntryEqualsAndHashCode() {
        CompactMap<String, Integer>.CompactMapEntry entry1 =
                map.new CompactMapEntry("k", 5);
        CompactMap<String, Integer>.CompactMapEntry entry2 =
                map.new CompactMapEntry("k", 5);
        CompactMap<String, Integer>.CompactMapEntry entry3 =
                map.new CompactMapEntry("k", 6);

        assertTrue(entry1.equals(entry2));
        assertEquals(entry1.hashCode(), entry2.hashCode());

        assertFalse(entry1.equals(entry3));

        // setValue should update both the entry and the underlying map
        map.put("k", 5);
        entry1.setValue(10);
        assertEquals(Integer.valueOf(10), map.get("k"));
    }
}
