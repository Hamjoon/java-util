package com.cedarsoftware.util.cache;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class LockingLRUCacheStrategyTest {

    private LockingLRUCacheStrategy<String, Integer> cache;

    @Before
    public void setUp() {
        // default capacity 3 for most tests
        cache = new LockingLRUCacheStrategy<>(3);
    }

    /* --------------------------------------------------------------------- */
    /*  PUBLIC METHOD LIST (extracted from the source)                     */
    /* --------------------------------------------------------------------- */
    // public LockingLRUCacheStrategy(int capacity)
    // public V get(Object key)
    // public V put(K key, V value)
    // public void putAll(Map<? extends K, ? extends V> m)
    // public V remove(Object key)
    // public void clear()
    // public int size()
    // public boolean isEmpty()
    // public boolean containsKey(Object key)
    // public boolean containsValue(Object value)
    // public Set<Map.Entry<K, V>> entrySet()
    // public Set<K> keySet()
    // public Collection<V> values()
    // public boolean equals(Object o)
    // public String toString()
    // public int hashCode()

    /* --------------------------------------------------------------------- */
    /*  BASIC FUNCTIONALITY TESTS                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testPutAndGet() {
        assertNull(cache.put("a", 1));
        assertEquals(Integer.valueOf(1), cache.get("a"));
        assertEquals(1, cache.size());
    }

    @Test
    public void testOverwriteExistingKey() {
        cache.put("a", 1);
        // overwrite with new value
        assertEquals(Integer.valueOf(1), cache.put("a", 2));
        assertEquals(Integer.valueOf(2), cache.get("a"));
        assertEquals(1, cache.size());
    }

    @Test
    public void testEvictionWhenCapacityExceeded() {
        cache.put("a", 1);
        cache.put("b", 2);
        cache.put("c", 3);
        // cache is now full (capacity 3)
        cache.put("d", 4); // should evict least recently used ("a")
        assertNull(cache.get("a"));
        assertEquals(Integer.valueOf(2), cache.get("b"));
        assertEquals(Integer.valueOf(3), cache.get("c"));
        assertEquals(Integer.valueOf(4), cache.get("d"));
        assertEquals(3, cache.size());
    }

    @Test
    public void testLRUOrderChangesOnGet() {
        cache.put("a", 1);
        cache.put("b", 2);
        cache.put("c", 3);
        // access "a" to make it most recently used
        cache.get("a");
        // add new entry, should evict "b" (the LRU after the get)
        cache.put("d", 4);
        assertNull(cache.get("b"));
        assertEquals(Integer.valueOf(1), cache.get("a"));
        assertEquals(Integer.valueOf(3), cache.get("c"));
        assertEquals(Integer.valueOf(4), cache.get("d"));
    }

    @Test
    public void testCapacityZeroEvictsImmediately() {
        LockingLRUCacheStrategy<String, Integer> zeroCache = new LockingLRUCacheStrategy<>(0);
        zeroCache.put("x", 10);
        // With capacity 0, the just‑added entry should be evicted
        assertNull(zeroCache.get("x"));
        assertEquals(0, zeroCache.size());
    }

    @Test
    public void testNullKeyAndNullValue() {
        cache.put(null, null);
        assertTrue(cache.containsKey(null));
        assertTrue(cache.containsValue(null));
        assertNull(cache.get(null));
        assertNull(cache.remove(null));
        assertFalse(cache.containsKey(null));
    }

    @Test
    public void testPutAll() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("x", 100);
        map.put("y", 200);
        cache.putAll(map);
        assertEquals(Integer.valueOf(100), cache.get("x"));
        assertEquals(Integer.valueOf(200), cache.get("y"));
        assertEquals(2, cache.size());
    }

    @Test
    public void testRemove() {
        cache.put("a", 1);
        cache.put("b", 2);
        Integer removed = cache.remove("a");
        assertEquals(Integer.valueOf(1), removed);
        assertNull(cache.get("a"));
        assertEquals(1, cache.size());
    }

    @Test
    public void testClear() {
        cache.put("a", 1);
        cache.put("b", 2);
        cache.clear();
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
        assertFalse(cache.containsKey("a"));
        assertFalse(cache.containsValue(2));
    }

    @Test
    public void testContainsKeyAndContainsValue() {
        cache.put("k1", 10);
        cache.put("k2", null);
        assertTrue(cache.containsKey("k1"));
        assertTrue(cache.containsKey("k2"));
        assertTrue(cache.containsValue(10));
        assertTrue(cache.containsValue(null));
        assertFalse(cache.containsKey("missing"));
        assertFalse(cache.containsValue(999));
    }

    @Test
    public void testKeySetAndValuesOrder() {
        cache.put("first", 1);
        cache.put("second", 2);
        cache.put("third", 3);
        // Access "first" to move it to head
        cache.get("first");
        List<String> expectedKeys = Arrays.asList("first", "third", "second");
        List<Integer> expectedValues = Arrays.asList(1, 3, 2);
        assertEquals(expectedKeys, new ArrayList<>(cache.keySet()));
        assertEquals(expectedValues, new ArrayList<>(cache.values()));
    }

    @Test
    public void testEntrySetEquality() {
        cache.put("a", 1);
        cache.put("b", 2);
        Set<Map.Entry<String, Integer>> entrySet = cache.entrySet();
        Map<String, Integer> reference = new LinkedHashMap<>();
        reference.put("b", 2);
        reference.put("a", 1); // after two puts, "a" is LRU, "b" is MRU
        assertEquals(reference.entrySet(), entrySet);
    }

    @Test
    public void testEqualsAndHashCodeWithMap() {
        cache.put("x", 10);
        cache.put("y", 20);
        Map<String, Integer> other = new LinkedHashMap<>();
        other.put("y", 20);
        other.put("x", 10);
        assertTrue(cache.equals(other));
        assertEquals(other.entrySet().hashCode(), cache.hashCode());
    }

    @Test
    public void testToStringFormatting() {
        cache.put("a", 1);
        cache.put("b", 2);
        String s = cache.toString();
        // Order should be MRU first ("b") then "a"
        assertTrue(s.startsWith("{b=2, a=1"));
        assertTrue(s.endsWith("}"));
    }

    @Test
    public void testSelfReferenceFormatting() {
        // Insert the cache itself as a key and as a value
        cache.put("selfKey", null);
        cache.put(null, cache); // value is the cache itself
        String s = cache.toString();
        // The placeholder "(this Collection)" should appear for the self-reference
        assertTrue(s.contains("(this Collection)"));
    }

    /* --------------------------------------------------------------------- */
    /*  EDGE CASES & EXCEPTION SCENARIOS                                      */
    /* --------------------------------------------------------------------- */
    // The class does not throw custom exceptions, but we verify that
    // operations with nulls and zero capacity behave gracefully (already covered).

}
