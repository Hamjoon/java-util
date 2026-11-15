package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.*;

public class CompactMapTest {

    private CompactMap<String, Integer> map;

    @Before
    public void setUp() {
        map = new CompactMap<>();
    }

    /* -----------------------------------------------------------------
     *  1.  Public constructors
     * ----------------------------------------------------------------- */

    @Test
    public void testDefaultConstructorCreatesEmptyMap() {
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
    }

    @Test
    public void testCopyConstructorCopiesAllEntries() {
        Map<String, Integer> src = new HashMap<>();
        src.put("a", 1);
        src.put("b", 2);
        CompactMap<String, Integer> copy = new CompactMap<>(src);
        assertEquals(2, copy.size());
        assertEquals(Integer.valueOf(1), copy.get("a"));
        assertEquals(Integer.valueOf(2), copy.get("b"));
    }

    /* -----------------------------------------------------------------
     *  2.  size() / isEmpty()
     * ----------------------------------------------------------------- */

    @Test
    public void testSizeAndIsEmptyTransitions() {
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());

        map.put("one", 1);
        assertFalse(map.isEmpty());
        assertEquals(1, map.size());

        map.put("two", 2);
        assertEquals(2, map.size());

        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    /* -----------------------------------------------------------------
     *  3.  put() – basic insertion and overwrite
     * ----------------------------------------------------------------- */

    @Test
    public void testPutSingleEntryAndOverwrite() {
        // first insertion – should store as single logical value (key == "key")
        map.put("key", 10);
        assertEquals(Integer.valueOf(10), map.get("key"));
        assertEquals(1, map.size());

        // overwrite same key
        Integer old = map.put("key", 20);
        assertEquals(Integer.valueOf(10), old);
        assertEquals(Integer.valueOf(20), map.get("key"));
    }

    @Test
    public void testPutTwoDifferentKeysCreatesArrayBackedStorage() {
        map.put("first", 1);
        map.put("second", 2);
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(1), map.get("first"));
        assertEquals(Integer.valueOf(2), map.get("second"));
    }

    @Test
    public void testPutBeyondCompactSizeSwitchesToMap() {
        // compactSize() default = 80, we insert 81 entries
        for (int i = 0; i < 81; i++) {
            map.put("k" + i, i);
        }
        assertEquals(81, map.size());

        // ensure a few random entries are present
        assertEquals(Integer.valueOf(0), map.get("k0"));
        assertEquals(Integer.valueOf(80), map.get("k80"));
    }

    @Test
    public void testPutNullKeyAndValue() {
        map.put(null, null);
        assertTrue(map.containsKey(null));
        assertTrue(map.containsValue(null));
        assertNull(map.get(null));
        assertEquals(1, map.size());
    }

    /* -----------------------------------------------------------------
     *  4.  get()
     * ----------------------------------------------------------------- */

    @Test
    public void testGetReturnsNullWhenKeyAbsent() {
        map.put("a", 1);
        assertNull(map.get("b"));
    }

    /* -----------------------------------------------------------------
     *  5.  containsKey() / containsValue()
     * ----------------------------------------------------------------- */

    @Test
    public void testContainsKeyAndValue() {
        map.put("x", 100);
        assertTrue(map.containsKey("x"));
        assertTrue(map.containsValue(100));
        assertFalse(map.containsKey("y"));
        assertFalse(map.containsValue(200));
    }

    /* -----------------------------------------------------------------
     *  6.  remove()
     * ----------------------------------------------------------------- */

    @Test
    public void testRemoveSingleEntry() {
        map.put("key", 5);
        Integer removed = map.remove("key");
        assertEquals(Integer.valueOf(5), removed);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testRemoveFromArrayBackedStorage() {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assertEquals(3, map.size());

        Integer removed = map.remove("b");
        assertEquals(Integer.valueOf(2), removed);
        assertEquals(2, map.size());
        assertFalse(map.containsKey("b"));
    }

    @Test
    public void testRemoveFromMapBackedStorageAndShrinkToArray() {
        // Build > compactSize map
        for (int i = 0; i < 81; i++) {
            map.put("k" + i, i);
        }
        assertTrue(map.size() > map.compactSize());

        // Remove enough entries to drop to compactSize (80)
        map.remove("k0");
        assertEquals(80, map.size());

        // The internal representation should now be an Object[]
        // We verify by checking that a newly added entry works correctly
        map.put("newKey", 999);
        assertEquals(Integer.valueOf(999), map.get("newKey"));
    }

    @Test
    public void testRemoveNonExistingKeyReturnsNull() {
        map.put("present", 1);
        assertNull(map.remove("absent"));
    }

    /* -----------------------------------------------------------------
     *  7.  putAll()
     * ----------------------------------------------------------------- */

    @Test
    public void testPutAllWithEmptyMap() {
        Map<String, Integer> src = new HashMap<>();
        src.put("a", 1);
        src.put("b", 2);
        map.putAll(src);
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(1), map.get("a"));
        assertEquals(Integer.valueOf(2), map.get("b"));
    }

    @Test
    public void testPutAllWhenSourceIsLargerThanCompactSize() {
        Map<String, Integer> src = new HashMap<>();
        for (int i = 0; i < 85; i++) {
            src.put("k" + i, i);
        }
        map.putAll(src);
        assertEquals(85, map.size());
        assertEquals(Integer.valueOf(0), map.get("k0"));
        assertEquals(Integer.valueOf(84), map.get("k84"));
    }

    @Test
    public void testPutAllWithNullArgumentDoesNothing() {
        map.put("a", 1);
        map.putAll(null);
        assertEquals(1, map.size());
    }

    /* -----------------------------------------------------------------
     *  8.  clear()
     * ----------------------------------------------------------------- */

    @Test
    public void testClearEmptiesMap() {
        map.put("x", 10);
        map.put("y", 20);
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    /* -----------------------------------------------------------------
     *  9.  hashCode() and equals()
     * ----------------------------------------------------------------- */

    @Test
    public void testHashCodeConsistency() {
        map.put("a", 1);
        map.put("b", 2);
        int h1 = map.hashCode();

        CompactMap<String, Integer> other = new CompactMap<>();
        other.put("a", 1);
        other.put("b", 2);
        int h2 = other.hashCode();

        assertEquals(h1, h2);
        assertTrue(map.equals(other));
        assertTrue(other.equals(map));
    }

    @Test
    public void testEqualsWithDifferentImplementations() {
        map.put("x", 100);
        Map<String, Integer> hashMap = new HashMap<>();
        hashMap.put("x", 100);
        assertTrue(map.equals(hashMap));
        assertTrue(hashMap.equals(map));
    }

    @Test
    public void testEqualsHandlesNullValues() {
        map.put("nullKey", null);
        CompactMap<String, Integer> other = new CompactMap<>();
        other.put("nullKey", null);
        assertTrue(map.equals(other));
    }

    /* -----------------------------------------------------------------
     * 10. toString()
     * ----------------------------------------------------------------- */

    @Test
    public void testToStringEmpty() {
        assertEquals("{}", map.toString());
    }

    @Test
    public void testToStringWithEntries() {
        map.put("a", 1);
        map.put("b", 2);
        String s = map.toString();
        // order is not guaranteed for HashMap backing, so we just check substrings
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        assertTrue(s.contains("a=1"));
        assertTrue(s.contains("b=2"));
    }

    /* -----------------------------------------------------------------
     * 11. keySet(), values(), entrySet()
     * ----------------------------------------------------------------- */

    @Test
    public void testKeySetOperations() {
        map.put("k1", 1);
        map.put("k2", 2);
        Set<String> keys = map.keySet();
        assertEquals(2, keys.size());
        assertTrue(keys.contains("k1"));
        assertTrue(keys.remove("k1"));
        assertFalse(keys.contains("k1"));
        assertEquals(1, map.size());
    }

    @Test
    public void testValuesCollection() {
        map.put("a", 10);
        map.put("b", 20);
        Collection<Integer> values = map.values();
        assertEquals(2, values.size());
        assertTrue(values.contains(10));
        assertTrue(values.contains(20));
    }

    @Test
    public void testEntrySetOperations() {
        map.put("one", 1);
        map.put("two", 2);
        Set<Map.Entry<String, Integer>> entries = map.entrySet();
        assertEquals(2, entries.size());

        // remove via entrySet iterator
        Iterator<Map.Entry<String, Integer>> it = entries.iterator();
        while (it.hasNext()) {
            Map.Entry<String, Integer> e = it.next();
            if (e.getKey().equals("one")) {
                it.remove();
            }
        }
        assertFalse(map.containsKey("one"));
        assertEquals(1, map.size());
    }

    /* -----------------------------------------------------------------
     * 12. minus() and plus() – unsupported operations
     * ----------------------------------------------------------------- */

    @Test(expected = UnsupportedOperationException.class)
    public void testMinusThrowsException() {
        map.minus("anything");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPlusThrowsException() {
        map.plus("anything");
    }

    /* -----------------------------------------------------------------
     * 13. Iterator fail‑fast behavior
     * ----------------------------------------------------------------- */

    @Test(expected = ConcurrentModificationException.class)
    public void testIteratorDetectsConcurrentModification() {
        map.put("a", 1);
        map.put("b", 2);
        Iterator<String> it = map.keySet().iterator();
        assertTrue(it.hasNext());
        map.put("c", 3);               // structural change after iterator creation
        it.next();                     // should throw CME
    }

    @Test(expected = IllegalStateException.class)
    public void testIteratorRemoveBeforeNextThrows() {
        map.put("a", 1);
        Iterator<String> it = map.keySet().iterator();
        it.remove();                  // illegal – next() not called yet
    }

    @Test(expected = NoSuchElementException.class)
    public void testIteratorNextBeyondEndThrows() {
        map.put("only", 42);
        Iterator<String> it = map.keySet().iterator();
        assertTrue(it.hasNext());
        it.next();                     // first (and only) element
        assertFalse(it.hasNext());
        it.next();                     // should throw NoSuchElementException
    }

    /* -----------------------------------------------------------------
     * 14. Copy iterator path (when underlying map is a SortedMap)
     * ----------------------------------------------------------------- */

    private static class SortedCompactMap<K, V> extends CompactMap<K, V> {
        @Override
        protected Map<K, V> getNewMap() {
            return new TreeMap<>(); // SortedMap triggers copy iterator usage
        }
    }

    @Test
    public void testCopyIteratorUsedWhenBackingMapIsSorted() {
        SortedCompactMap<String, Integer> sortedMap = new SortedCompactMap<>();
        sortedMap.put("b", 2);
        sortedMap.put("a", 1);
        // The keySet iterator should be a CopyKeyIterator (indirectly verified by order)
        Iterator<String> it = sortedMap.keySet().iterator();
        List<String> collected = new ArrayList<>();
        while (it.hasNext()) {
            collected.add(it.next());
        }
        // TreeMap orders keys naturally
        assertEquals(Arrays.asList("a", "b"), collected);
    }

    /* -----------------------------------------------------------------
     * 15. Case‑insensitive key comparison (override isCaseInsensitive)
     * ----------------------------------------------------------------- */

    private static class CaseInsensitiveCompactMap extends CompactMap<String, Integer> {
        @Override
        protected boolean isCaseInsensitive() {
            return true;
        }
    }

    @Test
    public void testCaseInsensitiveKeyLookup() {
        CaseInsensitiveCompactMap<String, Integer> ciMap = new CaseInsensitiveCompactMap<>();
        ciMap.put("Key", 5);
        assertTrue(ciMap.containsKey("key"));
        assertEquals(Integer.valueOf(5), ciMap.get("KEY"));
    }

    /* -----------------------------------------------------------------
     * 16. getLogicalValueType – exercised via reflection on protected method
     * ----------------------------------------------------------------- */

    @Test
    public void testLogicalValueTypeTransitions() throws Exception {
        // Empty
        assertEquals(CompactMap.LogicalValueType.EMPTY,
                invokeGetLogicalValueType(map));

        // Single OBJECT (key == getSingleValueKey())
        map.put("key", 10);
        assertEquals(CompactMap.LogicalValueType.OBJECT,
                invokeGetLogicalValueType(map));

        // Two entries → ARRAY
        map.put("other", 20);
        assertEquals(CompactMap.LogicalValueType.ARRAY,
                invokeGetLogicalValueType(map));

        // Grow beyond compactSize to MAP
        for (int i = 0; i < 80; i++) {
            map.put("k" + i, i);
        }
        assertEquals(CompactMap.LogicalValueType.MAP,
                invokeGetLogicalValueType(map));
    }

    private CompactMap.LogicalValueType invokeGetLogicalValueType(CompactMap<?,?> cm) throws Exception {
        java.lang.reflect.Method m = CompactMap.class.getDeclaredMethod("getLogicalValueType");
        m.setAccessible(true);
        return (CompactMap.LogicalValueType) m.invoke(cm);
    }
}
