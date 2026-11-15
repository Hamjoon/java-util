package com.cedarsoftware.util.cache;

import org.junit.Test;
import org.junit.Before;

import java.util.*;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link LockingLRUCacheStrategy}.
 *
 * The tests are organised in the order suggested by the task:
 *   1. List of public methods (see the comments above each test).
 *   2. Basic functionality tests.
 *   3. Edge‑case / exception tests.
 *   4. Comprehensive test class that merges everything.
 */
public class LockingLRUCacheStrategyTest {

    // ----------------------------------------------------------------------
    // 1. Public API extracted from the source
    // ----------------------------------------------------------------------
    //
    // public LockingLRUCacheStrategy(int capacity)
    // public int getCapacity()
    // public V get(Object key)
    // public V put(K key, V value)
    // public void putAll(Map<? extends K, ? extends V> m)
    // public V remove(Object key)
    // public void clear()
    // public int size()
    // public boolean isEmpty()
    // public boolean containsKey(Object key)
    // public boolean containsValue(Object value)
    // public Set<Map.Entry<K,V>> entrySet()
    // public Set<K> keySet()
    // public Collection<V> values()
    // public boolean equals(Object o)
    // public String toString()
    // public int hashCode()
    // ----------------------------------------------------------------------


    // ----------------------------------------------------------------------
    // 2. Basic functionality tests
    // ----------------------------------------------------------------------

    @Test
    public void testConstructorAndCapacity() {
        LockingLRUCacheStrategy<String, Integer> cache = new LockingLRUCacheStrategy<>(5);
        assertEquals(5, cache.getCapacity());
    }

    @Test
    public void testPutAndGetSimple() {
        LockingLRUCacheStrategy<String, String> cache = new LockingLRUCacheStrategy<>(3);
        assertNull(cache.put("a", "alpha"));
        assertEquals("alpha", cache.get("a"));
        // update existing key
        assertEquals("alpha", cache.put("a", "newAlpha"));
        assertEquals("newAlpha", cache.get("a"));
    }

    @Test
    public void testSizeAndIsEmpty() {
        LockingLRUCacheStrategy<Integer, Integer> cache = new LockingLRUCacheStrategy<>(2);
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());

        cache.put(1, 10);
        assertFalse(cache.isEmpty());
        assertEquals(1, cache.size());

        cache.put(2, 20);
        assertEquals(2, cache.size());
    }

    @Test
    public void testContainsKeyAndContainsValue() {
        LockingLRUCacheStrategy<String, String> cache = new LockingLRUCacheStrategy<>(2);
        cache.put("k1", "v1");
        cache.put("k2", "v2");

        assertTrue(cache.containsKey("k1"));
        assertTrue(cache.containsValue("v2"));
        assertFalse(cache.containsKey("k3"));
        assertFalse(cache.containsValue("v3"));
    }

    @Test
    public void testRemove() {
        LockingLRUCacheStrategy<String, String> cache = new LockingLRUCacheStrategy<>(2);
        cache.put("x", "xval");
        cache.put("y", "yval");

        assertEquals("xval", cache.remove("x"));
        assertNull(cache.get("x"));
        assertEquals(1, cache.size());

        // removing non‑existent key returns null
        assertNull(cache.remove("nonexistent"));
    }

    @Test
    public void testClear() {
        LockingLRUCacheStrategy<Integer, String> cache = new LockingLRUCacheStrategy<>(3);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.clear();

        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
        assertNull(cache.get(1));
    }

    @Test
    public void testEntrySetKeySetValuesOrder() {
        // capacity 3, insert three entries, then access one to make it MRU
        LockingLRUCacheStrategy<String, Integer> cache = new LockingLRUCacheStrategy<>(3);
        cache.put("a", 1);
        cache.put("b", 2);
        cache.put("c", 3);
        // access 'a' -> should become most recent
        cache.get("a");

        // Expected LRU order: a (most recent), c, b (least recent)
        List<String> expectedKeyOrder = Arrays.asList("a", "c", "b");
        List<Integer> expectedValueOrder = Arrays.asList(1, 3, 2);

        // entrySet
        Iterator<Map.Entry<String, Integer>> entryIter = cache.entrySet().iterator();
        int i = 0;
        while (entryIter.hasNext()) {
            Map.Entry<String, Integer> e = entryIter.next();
            assertEquals(expectedKeyOrder.get(i), e.getKey());
            assertEquals(expectedValueOrder.get(i), e.getValue());
            i++;
        }
        assertEquals(3, i);

        // keySet
        Iterator<String> keyIter = cache.keySet().iterator();
        i = 0;
        while (keyIter.hasNext()) {
            assertEquals(expectedKeyOrder.get(i), keyIter.next());
            i++;
        }
        assertEquals(3, i);

        // values
        Iterator<Integer> valIter = cache.values().iterator();
        i = 0;
        while (valIter.hasNext()) {
            assertEquals(expectedValueOrder.get(i), valIter.next());
            i++;
        }
        assertEquals(3, i);
    }

    @Test
    public void testEqualsAndHashCode() {
        LockingLRUCacheStrategy<String, Integer> cache1 = new LockingLRUCacheStrategy<>(4);
        LockingLRUCacheStrategy<String, Integer> cache2 = new LockingLRUCacheStrategy<>(4);
        Map<String, Integer> plainMap = new LinkedHashMap<>();

        cache1.put("x", 100);
        cache1.put("y", 200);
        cache2.put("x", 100);
        cache2.put("y", 200);
        plainMap.put("x", 100);
        plainMap.put("y", 200);

        // Equality with another LockingLRUCacheStrategy
        assertTrue(cache1.equals(cache2));
        assertTrue(cache2.equals(cache1));

        // Equality with a generic Map
        assertTrue(cache1.equals(plainMap));
        assertTrue(plainMap.equals(cache1));

        // hashCode must be equal for equal maps
        assertEquals(cache1.hashCode(), cache2.hashCode());
        assertEquals(cache1.hashCode(), plainMap.hashCode());
    }

    @Test
    public void testToStringSimpleAndSelfReference() {
        LockingLRUCacheStrategy<Object, Object> cache = new LockingLRUCacheStrategy<>(2);
        cache.put("key", "value");
        cache.put(null, null);
        String s = cache.toString();

        // The string should contain both entries in LRU order (most recent first)
        assertTrue(s.startsWith("{null=null, key=value") || s.startsWith("{key=value, null=null"));
        assertTrue(s.endsWith("}"));

        // Self‑reference handling
        cache.clear();
        cache.put("self", cache);
        String selfString = cache.toString();
        assertTrue(selfString.contains("(this Collection)"));
    }

    // ----------------------------------------------------------------------
    // 3. Edge‑case / exception tests
    // ----------------------------------------------------------------------

    @Test
    public void testZeroCapacityEviction() {
        // Capacity zero means no element can stay in the cache.
        LockingLRUCacheStrategy<String, String> cache = new LockingLRUCacheStrategy<>(0);
        assertNull(cache.put("a", "A"));
        assertEquals(0, cache.size());
        assertNull(cache.get("a"));
    }

    @Test
    public void testNullKeyAndNullValue() {
        LockingLRUCacheStrategy<String, String> cache = new LockingLRUCacheStrategy<>(2);
        cache.put(null, "nullKey");
        cache.put("nullValue", null);

        assertEquals("nullKey", cache.get(null));
        assertNull(cache.get("nullValue"));
        assertTrue(cache.containsKey(null));
        assertTrue(cache.containsValue(null));
    }

    @Test(expected = NullPointerException.class)
    public void testPutAllNullMapThrowsException() {
        LockingLRUCacheStrategy<String, Integer> cache = new LockingLRUCacheStrategy<>(2);
        cache.putAll(null); // should throw NPE as per Javadoc
    }

    @Test
    public void testPutAllCopiesAllEntries() {
        LockingLRUCacheStrategy<String, Integer> cache = new LockingLRUCacheStrategy<>(5);
        Map<String, Integer> source = new LinkedHashMap<>();
        source.put("a", 1);
        source.put("b", 2);
        source.put("c", 3);

        cache.putAll(source);
        assertEquals(3, cache.size());
        assertEquals(Integer.valueOf(2), cache.get("b"));
    }

    @Test
    public void testEvictionOrderWhenCapacityReached() {
        // capacity = 2, insert three keys, verify the first inserted is evicted
        LockingLRUCacheStrategy<String, String> cache = new LockingLRUCacheStrategy<>(2);
        cache.put("first", "1");
        cache.put("second", "2");
        // at capacity now, next put should evict "first"
        cache.put("third", "3");

        assertNull(cache.get("first"));
        assertEquals("2", cache.get("second"));
        assertEquals("3", cache.get("third"));
        assertEquals(2, cache.size());

        // Access "second" to make it MRU, then add another entry to cause eviction of "third"
        cache.get("second");
        cache.put("fourth", "4");
        assertNull(cache.get("third"));
        assertEquals("2", cache.get("second"));
        assertEquals("4", cache.get("fourth"));
    }

    // ----------------------------------------------------------------------
    // 4. Full test class is already assembled above.
    // ----------------------------------------------------------------------
}
