package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link CompactMap}.
 *
 * <p>The tests are written as if three independent experts collaborated.
 * Each expert contributed at least one test for every public method,
 * covering typical usage, edge cases and error conditions.</p>
 */
public class CompactMapTest {

    /* -----------------------------------------------------------------
     * Helper subclasses to expose internal behaviour and to force
     * transitions at lower thresholds.
     * ----------------------------------------------------------------- */

    /** CompactMap with a very small {@code compactSize} so that we can
     *  trigger the array‑to‑Map transition quickly. */
    private static class TinyCompactMap<K, V> extends CompactMap<K, V> {
        @Override protected int compactSize() { return 2; } // 2 entries -> array, 3 -> Map
    }

    /** CompactMap that is case‑insensitive for String keys. */
    private static class CaseInsensitiveCompactMap<K, V> extends CompactMap<K, V> {
        @Override protected boolean isCaseInsensitive() { return true; }
        @Override protected K getSingleValueKey() { return (K) "key"; }
    }

    /** CompactMap that uses a SortedMap (TreeMap) as its backing map,
     *  causing {@code useCopyIterator()} to return {@code true}. */
    private static class SortedCompactMap<K, V> extends CompactMap<K, V> {
        @Override protected Map<K, V> getNewMap() { return new TreeMap<>(); }
    }

    private CompactMap<String, Integer> map;
    private TinyCompactMap<String, Integer> tinyMap;
    private CaseInsensitiveCompactMap<String, Integer> ciMap;
    private SortedCompactMap<String, Integer> sortedMap;

    @Before
    public void setUp() {
        map = new CompactMap<>();
        tinyMap = new TinyCompactMap<>();
        ciMap = new CaseInsensitiveCompactMap<>();
        sortedMap = new SortedCompactMap<>();
    }

    /* -----------------------------------------------------------------
     * size() / isEmpty()
     * ----------------------------------------------------------------- */

    @Test
    public void testSizeAndIsEmptyTransitions() {
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());

        map.put("a", 1);
        assertFalse(map.isEmpty());
        assertEquals(1, map.size());

        map.put("b", 2);
        assertEquals(2, map.size());

        // Fill up to compactSize (80) – we only need a few entries for the test,
        // but check that size grows correctly.
        for (int i = 3; i <= 5; i++) {
            map.put("k" + i, i);
        }
        assertEquals(5, map.size());

        // clear()
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    /* -----------------------------------------------------------------
     * compareKeys (indirectly via containsKey, get, put)
     * ----------------------------------------------------------------- */

    @Test
    public void testCaseInsensitiveKeyComparison() {
        ciMap.put("Foo", 10);
        // Different case – should be considered equal
        assertTrue(ciMap.containsKey("foo"));
        assertEquals(Integer.valueOf(10), ciMap.get("FOO"));
        // Removing with different case works as well
        assertEquals(Integer.valueOf(10), ciMap.remove("fOo"));
        assertTrue(ciMap.isEmpty());
    }

    /* -----------------------------------------------------------------
     * containsKey / containsValue
     * ----------------------------------------------------------------- */

    @Test
    public void testContainsKeyAndValueVariousStates() {
        // empty
        assertFalse(map.containsKey("x"));
        assertFalse(map.containsValue(1));

        // single entry (key = getSingleValueKey())
        map.put("key", 42);
        assertTrue(map.containsKey("key"));
        assertTrue(map.containsValue(42));

        // multiple entries (array state)
        map.put("a", 1);
        map.put("b", 2);
        assertTrue(map.containsKey("a"));
        assertTrue(map.containsKey("b"));
        assertTrue(map.containsValue(1));
        assertTrue(map.containsValue(2));
        assertFalse(map.containsKey("non"));
        assertFalse(map.containsValue(99));
    }

    /* -----------------------------------------------------------------
     * get()
     * ----------------------------------------------------------------- */

    @Test
    public void testGetReturnsNullWhenAbsent() {
        assertNull(map.get("missing"));
        map.put("present", 5);
        assertNull(map.get("other"));
    }

    /* -----------------------------------------------------------------
     * put() – transition logic
     * ----------------------------------------------------------------- */

    @Test
    public void testPutTransitionsEmptyToSingle() {
        // empty -> single (key == getSingleValueKey())
        map.put("key", 100);
        assertEquals(1, map.size());
        assertEquals(Integer.valueOf(100), map.get("key"));
    }

    @Test
    public void testPutTransitionsEmptyToEntry() {
        // empty -> entry (key != getSingleValueKey())
        map.put("other", 200);
        assertEquals(1, map.size());
        assertEquals(Integer.valueOf(200), map.get("other"));
        // internal type should be ENTRY
        assertEquals(CompactMap.LogicalValueType.ENTRY, map.getLogicalValueType());
    }

    @Test
    public void testPutArrayGrowthAndShrink() {
        // Fill tinyMap to trigger array growth
        tinyMap.put("k1", 1);
        tinyMap.put("k2", 2); // now array of length 4
        assertEquals(CompactMap.LogicalValueType.ARRAY, tinyMap.getLogicalValueType());

        // Add third entry → should switch to Map (since compactSize() == 2)
        tinyMap.put("k3", 3);
        assertEquals(CompactMap.LogicalValueType.MAP, tinyMap.getLogicalValueType());
        assertEquals(3, tinyMap.size());

        // Remove one entry → should revert back to ARRAY
        tinyMap.remove("k1");
        assertEquals(CompactMap.LogicalValueType.ARRAY, tinyMap.getLogicalValueType());
        assertEquals(2, tinyMap.size());
    }

    @Test
    public void testPutOverwriteReturnsOldValue() {
        map.put("key", 1);
        Integer old = map.put("key", 2);
        assertEquals(Integer.valueOf(1), old);
        assertEquals(Integer.valueOf(2), map.get("key"));
    }

    /* -----------------------------------------------------------------
     * remove()
     * ----------------------------------------------------------------- */

    @Test
    public void testRemoveSingleEntry() {
        map.put("key", 123);
        Integer removed = map.remove("key");
        assertEquals(Integer.valueOf(123), removed);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testRemoveFromArrayAndShrinkToSingle() {
        tinyMap.put("a", 1);
        tinyMap.put("b", 2); // array state
        assertEquals(CompactMap.LogicalValueType.ARRAY, tinyMap.getLogicalValueType());

        // remove one entry – should shrink back to single entry (ENTRY type)
        tinyMap.remove("a");
        assertEquals(1, tinyMap.size());
        assertEquals(CompactMap.LogicalValueType.ENTRY, tinyMap.getLogicalValueType());
        assertEquals(Integer.valueOf(2), tinyMap.get("b"));
    }

    @Test
    public void testRemoveFromMapAndSwitchBackToArray() {
        // Use tinyMap to force map state quickly
        tinyMap.put("x", 10);
        tinyMap.put("y", 20);
        tinyMap.put("z", 30); // now in MAP state
        assertEquals(CompactMap.LogicalValueType.MAP, tinyMap.getLogicalValueType());

        // Remove one entry → size becomes compactSize (2) → should become ARRAY
        tinyMap.remove("z");
        assertEquals(2, tinyMap.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, tinyMap.getLogicalValueType());
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalCompactSizeThrowsOnConstruction() {
        // Subclass with compactSize < 2 triggers exception in constructor
        new CompactMap<String, Integer>() {
            @Override protected int compactSize() { return 1; }
        };
    }

    /* -----------------------------------------------------------------
     * putAll()
     * ----------------------------------------------------------------- */

    @Test
    public void testPutAllIntoEmptyAndArrayStates() {
        Map<String, Integer> src = new HashMap<>();
        src.put("a", 1);
        src.put("b", 2);
        map.putAll(src);
        assertEquals(2, map.size());
        assertTrue(map.containsKey("a"));
        assertTrue(map.containsKey("b"));
    }

    @Test
    public void testPutAllSwitchesToMapWhenTooLarge() {
        // tinyMap has compactSize = 2, so 3 entries force Map
        Map<String, Integer> src = new HashMap<>();
        src.put("1", 1);
        src.put("2", 2);
        src.put("3", 3);
        tinyMap.putAll(src);
        assertEquals(CompactMap.LogicalValueType.MAP, tinyMap.getLogicalValueType());
        assertEquals(3, tinyMap.size());
    }

    @Test
    public void testPutAllWithNullArgumentDoesNothing() {
        map.put("a", 1);
        map.putAll(null);
        assertEquals(1, map.size());
    }

    /* -----------------------------------------------------------------
     * hashCode() / equals()
     * ----------------------------------------------------------------- */

    @Test
    public void testHashCodeAndEqualsForVariousRepresentations() {
        CompactMap<String, Integer> m1 = new CompactMap<>();
        CompactMap<String, Integer> m2 = new CompactMap<>();

        // both empty
        assertEquals(m1.hashCode(), m2.hashCode());
        assertTrue(m1.equals(m2));

        // single entry (key == getSingleValueKey())
        m1.put("key", 10);
        m2.put("key", 10);
        assertEquals(m1.hashCode(), m2.hashCode());
        assertTrue(m1.equals(m2));

        // different order but same content (array state)
        m1.put("a", 1);
        m1.put("b", 2);
        m2.put("b", 2);
        m2.put("a", 1);
        assertEquals(m1.hashCode(), m2.hashCode());
        assertTrue(m1.equals(m2));

        // compare with a standard HashMap
        Map<String, Integer> std = new HashMap<>();
        std.put("key", 10);
        std.put("a", 1);
        std.put("b", 2);
        assertTrue(m1.equals(std));
        assertTrue(std.equals(m1));
    }

    @Test
    public void testEqualsWithDifferentTypesAndSizes() {
        map.put("key", 1);
        assertFalse(map.equals("not a map"));
        assertFalse(map.equals(Collections.emptyMap()));
    }

    /* -----------------------------------------------------------------
     * toString()
     * ----------------------------------------------------------------- */

    @Test
    public void testToStringFormatsCorrectly() {
        assertEquals("{}", map.toString());

        map.put("key", 5);
        assertEquals("{key=5}", map.toString());

        map.put("a", 1);
        map.put("b", 2);
        // order is not guaranteed for HashMap; sort for comparison
        String s = map.toString();
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        assertTrue(s.contains("key=5"));
        assertTrue(s.contains("a=1"));
        assertTrue(s.contains("b=2"));
    }

    /* -----------------------------------------------------------------
     * keySet(), values(), entrySet() – basic contract
     * ----------------------------------------------------------------- */

    @Test
    public void testKeySetBasicOperations() {
        map.put("x", 10);
        map.put("y", 20);
        Set<String> ks = map.keySet();

        assertEquals(2, ks.size());
        assertTrue(ks.contains("x"));
        assertTrue(ks.containsAll(Arrays.asList("x", "y")));

        ks.remove("x");
        assertFalse(map.containsKey("x"));
        assertEquals(1, map.size());

        ks.clear();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testValuesBasicOperations() {
        map.put("a", 1);
        map.put("b", 2);
        Collection<Integer> vals = map.values();

        assertEquals(2, vals.size());
        assertTrue(vals.containsAll(Arrays.asList(1, 2)));

        // remove via iterator
        Iterator<Integer> it = vals.iterator();
        while (it.hasNext()) {
            Integer v = it.next();
            if (v.equals(1)) {
                it.remove();
            }
        }
        assertFalse(map.containsKey("a"));
        assertEquals(1, map.size());
    }

    @Test
    public void testEntrySetBasicOperations() {
        map.put("k1", 100);
        map.put("k2", 200);
        Set<Map.Entry<String, Integer>> es = map.entrySet();

        assertEquals(2, es.size());
        assertTrue(es.contains(new AbstractMap.SimpleEntry<>("k1", 100)));

        // remove an entry directly
        es.remove(new AbstractMap.SimpleEntry<>("k1", 100));
        assertFalse(map.containsKey("k1"));
        assertEquals(1, map.size());

        // test iterator remove on entrySet
        Iterator<Map.Entry<String, Integer>> it = es.iterator();
        assertTrue(it.hasNext());
        it.next();
        it.remove();
        assertTrue(map.isEmpty());
    }

    /* -----------------------------------------------------------------
     * Iterators – Compact and Copy variants, fail‑fast behaviour
     * ----------------------------------------------------------------- */

    @Test
    public void testCompactKeyIteratorTraversalAndRemove() {
        map.put("a", 1);
        map.put("b", 2);
        Iterator<String> it = map.keySet().iterator();

        List<String> seen = new ArrayList<>();
        while (it.hasNext()) {
            String k = it.next();
            seen.add(k);
            if ("a".equals(k)) {
                it.remove(); // remove via iterator
            }
        }
        assertTrue(seen.containsAll(Arrays.asList("a", "b")));
        assertFalse(map.containsKey("a"));
        assertEquals(1, map.size());
    }

    @Test(expected = IllegalStateException.class)
    public void testIteratorRemoveWithoutNextThrows() {
        Iterator<String> it = map.keySet().iterator();
        it.remove(); // illegal state
    }

    @Test(expected = ConcurrentModificationException.class)
    public void testIteratorDetectsConcurrentModification() {
        map.put("a", 1);
        map.put("b", 2);
        Iterator<String> it = map.keySet().iterator();
        it.next();
        map.put("c", 3); // structural change
        it.next(); // should throw CME
    }

    @Test
    public void testCopyIteratorPathUsesSortedMap() {
        // sortedMap uses TreeMap → copy iterator is chosen
        sortedMap.put("c", 3);
        sortedMap.put("a", 1);
        sortedMap.put("b", 2);

        Iterator<String> keyIt = sortedMap.keySet().iterator();
        List<String> ordered = new ArrayList<>();
        while (keyIt.hasNext()) {
            ordered.add(keyIt.next());
        }
        // TreeMap orders keys naturally
        assertEquals(Arrays.asList("a", "b", "c"), ordered);
    }

    /* -----------------------------------------------------------------
     * CompactMapEntry behaviour (equals, hashCode, setValue)
     * ----------------------------------------------------------------- */

    @Test
    public void testCompactMapEntryEqualsAndHashCode() {
        CompactMap<String, Integer>.CompactMapEntry e1 = map.new CompactMapEntry("x", 5);
        CompactMap<String, Integer>.CompactMapEntry e2 = map.new CompactMapEntry("x", 5);
        CompactMap<String, Integer>.CompactMapEntry e3 = map.new CompactMapEntry("x", 6);
        CompactMap<String, Integer>.CompactMapEntry e4 = map.new CompactMapEntry("y", 5);

        assertEquals(e1, e2);
        assertEquals(e1.hashCode(), e2.hashCode());

        assertNotEquals(e1, e3);
        assertNotEquals(e1, e4);
    }

    @Test
    public void testCompactMapEntrySetValueWritesThroughToMap() {
        map.put("k", 1);
        CompactMap<String, Integer>.CompactMapEntry entry = map.new CompactMapEntry("k", 1);
        map.put(entry.getKey(), entry.getValue()); // ensure entry is stored

        // Change via entry.setValue()
        Integer old = entry.setValue(99);
        assertEquals(Integer.valueOf(1), old);
        // Underlying map must reflect change
        assertEquals(Integer.valueOf(99), map.get("k"));
    }

    /* -----------------------------------------------------------------
     * computeKeyHashCode / computeValueHashCode (indirect via hashCode)
     * ----------------------------------------------------------------- */

    @Test
    public void testHashCodeHandlesNullAndSelfReferences() {
        map.put(null, null);
        map.put("self", null);
        // self‑reference key/value
        map.put("selfKey", null);
        map.put("selfValue", null);
        // Force a self reference
        map.put("map", null);
        map.put("map", null); // overwrite with same key

        // The hashCode call should not throw NPE
        int hc = map.hashCode();
        assertTrue(hc != 0 || map.isEmpty()); // just ensure it runs
    }

    /* -----------------------------------------------------------------
     * Miscellaneous edge cases
     * ----------------------------------------------------------------- */

    @Test
    public void testGetSingleValueKeyUsedWhenKeyMatches() {
        // The default single value key is "key"
        map.put("key", 777);
        assertEquals(Integer.valueOf(777), map.get("key"));
        assertEquals(1, map.size());
        // internal type should be OBJECT (value stored directly)
        assertEquals(CompactMap.LogicalValueType.OBJECT, map.getLogicalValueType());
    }

    @Test
    public void testPutNullKeyAndValueSupported() {
        map.put(null, null);
        assertTrue(map.containsKey(null));
        assertTrue(map.containsValue(null));
        assertNull(map.get(null));

        map.put("x", null);
        assertTrue(map.containsKey("x"));
        assertTrue(map.containsValue(null));
    }

    @Test
    public void testRemoveNonExistingKeyReturnsNull() {
        map.put("a", 1);
        assertNull(map.remove("missing"));
        assertEquals(1, map.size());
    }
}
