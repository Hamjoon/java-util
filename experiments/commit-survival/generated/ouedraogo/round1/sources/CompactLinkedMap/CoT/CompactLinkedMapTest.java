package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.lang.reflect.Method;
import java.util.*;

import static org.junit.Assert.*;

public class CompactLinkedMapTest {

    private CompactLinkedMap<String, Integer> map;

    @Before
    public void setUp() {
        map = new CompactLinkedMap<>();
    }

    /* -----------------------------------------------------------------
     *  1.  List of public methods (including inherited ones)
     * -----------------------------------------------------------------
     *
     *  Constructors
     *      CompactLinkedMap()
     *      CompactLinkedMap(Map<K,V> other)
     *
     *  Inherited from CompactMap (implements java.util.Map)
     *      int size()
     *      boolean isEmpty()
     *      boolean containsKey(Object key)
     *      boolean containsValue(Object value)
     *      V get(Object key)
     *      V put(K key, V value)
     *      V remove(Object key)
     *      void putAll(Map<? extends K,? extends V> m)
     *      void clear()
     *      int hashCode()
     *      boolean equals(Object o)
     *      String toString()
     *      Set<K> keySet()
     *      Collection<V> values()
     *      Set<Map.Entry<K,V>> entrySet()
     *      Map<K,V> minus(Object removeMe)
     *      Map<K,V> plus(Object right)
     *
     *  Protected (tested via reflection)
     *      protected boolean useCopyIterator()
     * ----------------------------------------------------------------- */

    /* -----------------------------------------------------------------
     *  2.  Basic functionality tests
     * ----------------------------------------------------------------- */

    @Test
    public void testDefaultConstructorCreatesEmptyMap() {
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testConstructorWithMapCopiesEntries() {
        Map<String, Integer> source = new HashMap<>();
        source.put("one", 1);
        source.put("two", 2);
        CompactLinkedMap<String, Integer> copy = new CompactLinkedMap<>(source);
        assertEquals(2, copy.size());
        assertTrue(copy.containsKey("one"));
        assertTrue(copy.containsValue(2));
        assertEquals(Integer.valueOf(1), copy.get("one"));
    }

    @Test
    public void testPutAndGet() {
        Integer previous = map.put("a", 10);
        assertNull(previous);                 // first insertion returns null
        assertEquals(Integer.valueOf(10), map.get("a"));
        assertTrue(map.containsKey("a"));
        assertTrue(map.containsValue(10));
    }

    @Test
    public void testPutOverwrite() {
        map.put("key", 1);
        Integer old = map.put("key", 2);
        assertEquals(Integer.valueOf(1), old);   // old value returned
        assertEquals(Integer.valueOf(2), map.get("key"));
        assertEquals(1, map.size());
    }

    @Test
    public void testContainsKeyAndValue() {
        map.put("x", 100);
        assertTrue(map.containsKey("x"));
        assertTrue(map.containsValue(100));
        assertFalse(map.containsKey("y"));
        assertFalse(map.containsValue(200));
    }

    @Test
    public void testRemoveExistingAndNonExisting() {
        map.put("removeMe", 55);
        Integer removed = map.remove("removeMe");
        assertEquals(Integer.valueOf(55), removed);
        assertFalse(map.containsKey("removeMe"));
        assertNull(map.remove("notThere")); // removing absent key returns null
    }

    @Test
    public void testPutAll() {
        Map<String, Integer> other = new HashMap<>();
        other.put("a", 1);
        other.put("b", 2);
        map.putAll(other);
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(1), map.get("a"));
        assertEquals(Integer.valueOf(2), map.get("b"));
    }

    @Test
    public void testClear() {
        map.put("k1", 1);
        map.put("k2", 2);
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testIsEmptyAndSize() {
        assertTrue(map.isEmpty());
        map.put("one", 1);
        assertFalse(map.isEmpty());
        assertEquals(1, map.size());
    }

    @Test
    public void testKeySetValuesEntrySet() {
        map.put("k1", 10);
        map.put("k2", 20);
        Set<String> keys = map.keySet();
        Collection<Integer> values = map.values();
        Set<Map.Entry<String, Integer>> entries = map.entrySet();

        assertEquals(new HashSet<>(Arrays.asList("k1", "k2")), keys);
        assertEquals(new HashSet<>(Arrays.asList(10, 20)), new HashSet<>(values));

        Map<String, Integer> reconstructed = new HashMap<>();
        for (Map.Entry<String, Integer> e : entries) {
            reconstructed.put(e.getKey(), e.getValue());
        }
        assertEquals(2, reconstructed.size());
        assertEquals(Integer.valueOf(10), reconstructed.get("k1"));
        assertEquals(Integer.valueOf(20), reconstructed.get("k2"));
    }

    @Test
    public void testMinusRemovesKey() {
        map.put("keep", 1);
        map.put("discard", 2);
        Map<String, Integer> result = map.minus("discard");
        assertFalse(result.containsKey("discard"));
        assertTrue(result.containsKey("keep"));
        assertEquals(Integer.valueOf(1), result.get("keep"));
    }

    @Test
    public void testPlusAddsEntry() {
        map.put("a", 1);
        Map<String, Integer> result = map.plus("b");
        // The contract of plus(Object) is not documented; we assume it returns a new map
        // that contains the original entries plus the supplied key (with a null value).
        assertTrue(result.containsKey("a"));
        assertTrue(result.containsKey("b"));
        assertEquals(Integer.valueOf(1), result.get("a"));
        assertNull(result.get("b"));
    }

    @Test
    public void testEqualsAndHashCode() {
        map.put("x", 9);
        map.put("y", 8);

        CompactLinkedMap<String, Integer> other = new CompactLinkedMap<>();
        other.put("x", 9);
        other.put("y", 8);

        assertTrue(map.equals(other));
        assertEquals(map.hashCode(), other.hashCode());

        other.put("z", 7);
        assertFalse(map.equals(other));
    }

    @Test
    public void testNullKeyAndValue() {
        // CompactMap (and thus CompactLinkedMap) permits null keys/values like HashMap.
        map.put(null, 123);
        map.put("nullValue", null);
        assertTrue(map.containsKey(null));
        assertTrue(map.containsValue(null));
        assertEquals(Integer.valueOf(123), map.get(null));
        assertNull(map.get("nullValue"));
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorNullMapThrows() {
        new CompactLinkedMap<>(null);
    }

    /* -----------------------------------------------------------------
     *  3.  Edge‑case & special‑behaviour tests
     * ----------------------------------------------------------------- */

    @Test
    public void testInsertionOrderPreservedBeyondCompactSize() throws Exception {
        // Use reflection to obtain the protected compactSize() value.
        Method compactSizeMethod = CompactMap.class.getDeclaredMethod("compactSize");
        compactSizeMethod.setAccessible(true);
        int compactSize = (int) compactSizeMethod.invoke(map);

        // Insert more than (compactSize + 1) entries to force the internal delegate
        // to become a LinkedHashMap (which preserves insertion order).
        int total = compactSize + 3;
        List<String> insertionOrder = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            String key = "k" + i;
            insertionOrder.add(key);
            map.put(key, i);
        }

        // Verify that entrySet iteration follows insertion order.
        List<String> iteratedOrder = new ArrayList<>();
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            iteratedOrder.add(e.getKey());
        }
        assertEquals(insertionOrder, iteratedOrder);
    }

    @Test
    public void testUseCopyIteratorProtectedMethodReturnsFalse() throws Exception {
        Method useCopyIterator = CompactMap.class.getDeclaredMethod("useCopyIterator");
        useCopyIterator.setAccessible(true);
        boolean result = (boolean) useCopyIterator.invoke(map);
        assertFalse("CompactLinkedMap should not use a copy iterator", result);
    }

    @Test
    public void testRemoveNullKey() {
        map.put(null, 1);
        Integer removed = map.remove(null);
        assertEquals(Integer.valueOf(1), removed);
        assertFalse(map.containsKey(null));
    }

    @Test
    public void testMinusWithNonExistingKeyReturnsUnchangedMap() {
        map.put("a", 1);
        Map<String, Integer> before = new HashMap<>(map);
        Map<String, Integer> after = map.minus("doesNotExist");
        assertEquals(before, after);
    }

    @Test
    public void testPlusWithExistingKeyDoesNotReplaceValue() {
        map.put("existing", 5);
        Map<String, Integer> result = map.plus("existing");
        // Expect existing key/value to stay unchanged and a duplicate key is not added.
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(5), result.get("existing"));
    }
}
