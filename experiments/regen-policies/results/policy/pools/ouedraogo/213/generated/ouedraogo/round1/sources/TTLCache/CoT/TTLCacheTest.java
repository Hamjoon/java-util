package com.cedarsoftware.util;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class TTLCacheTest {

    // -----------------------------------------------------------------------
    //  Basic tests for the public API
    // -----------------------------------------------------------------------

    @Test
    public void testConstructorWithOnlyTTL() {
        TTLCache<String, String> cache = new TTLCache<>(1000L);
        assertEquals(0, cache.size());
        assertTrue(cache.isEmpty());
    }

    @Test
    public void testConstructorWithTTLAndMaxSize() {
        TTLCache<String, String> cache = new TTLCache<>(1000L, 5);
        assertEquals(0, cache.size());
    }

    @Test
    public void testConstructorFull() {
        TTLCache<String, String> cache = new TTLCache<>(1000L, 5, 200L);
        assertEquals(0, cache.size());
    }

    @Test
    public void testPutAndGet() {
        TTLCache<String, Integer> cache = new TTLCache<>(5000L);
        Integer old = cache.put("a", 1);
        assertNull(old);
        assertEquals(Integer.valueOf(1), cache.get("a"));
    }

    @Test
    public void testPutReturnsPreviousValue() {
        TTLCache<String, Integer> cache = new TTLCache<>(5000L);
        cache.put("a", 1);
        Integer old = cache.put("a", 2);
        assertEquals(Integer.valueOf(1), old);
        assertEquals(Integer.valueOf(2), cache.get("a"));
    }

    @Test
    public void testRemove() {
        TTLCache<String, String> cache = new TTLCache<>(5000L);
        cache.put("k", "v");
        String removed = cache.remove("k");
        assertEquals("v", removed);
        assertNull(cache.get("k"));
    }

    @Test
    public void testClear() {
        TTLCache<String, String> cache = new TTLCache<>(5000L);
        cache.put("x", "1");
        cache.put("y", "2");
        cache.clear();
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
    }

    @Test
    public void testSizeAndIsEmpty() {
        TTLCache<String, String> cache = new TTLCache<>(5000L);
        assertTrue(cache.isEmpty());
        cache.put("a", "a");
        assertEquals(1, cache.size());
        assertFalse(cache.isEmpty());
    }

    @Test
    public void testContainsKey() {
        TTLCache<String, String> cache = new TTLCache<>(5000L);
        cache.put("key", "value");
        assertTrue(cache.containsKey("key"));
        assertFalse(cache.containsKey("missing"));
    }

    @Test
    public void testContainsValue() {
        TTLCache<String, String> cache = new TTLCache<>(5000L);
        cache.put("k1", "v1");
        cache.put("k2", "v2");
        assertTrue(cache.containsValue("v1"));
        assertFalse(cache.containsValue("v3"));
    }

    @Test
    public void testPutAll() {
        TTLCache<String, Integer> cache = new TTLCache<>(5000L);
        Map<String, Integer> src = new HashMap<>();
        src.put("a", 1);
        src.put("b", 2);
        cache.putAll(src);
        assertEquals(2, cache.size());
        assertEquals(Integer.valueOf(1), cache.get("a"));
        assertEquals(Integer.valueOf(2), cache.get("b"));
    }

    @Test
    public void testKeySet() {
        TTLCache<String, Integer> cache = new TTLCache<>(5000L);
        cache.put("k1", 10);
        cache.put("k2", 20);
        Set<String> keys = cache.keySet();
        assertTrue(keys.containsAll(Arrays.asList("k1", "k2")));
        assertEquals(2, keys.size());
    }

    @Test
    public void testValues() {
        TTLCache<String, Integer> cache = new TTLCache<>(5000L);
        cache.put("a", 1);
        cache.put("b", 2);
        Collection<Integer> values = cache.values();
        assertTrue(values.containsAll(Arrays.asList(1, 2)));
        assertEquals(2, values.size());
    }

    @Test
    public void testEntrySetIteration() {
        TTLCache<String, Integer> cache = new TTLCache<>(5000L);
        cache.put("x", 100);
        cache.put("y", 200);
        Set<Entry<String, Integer>> entries = cache.entrySet();
        Map<String, Integer> map = new HashMap<>();
        for (Entry<String, Integer> e : entries) {
            map.put(e.getKey(), e.getValue());
        }
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(100), map.get("x"));
        assertEquals(Integer.valueOf(200), map.get("y"));
    }

    @Test
    public void testEqualsAndHashCode() {
        TTLCache<String, Integer> cache = new TTLCache<>(5000L);
        cache.put("a", 1);
        cache.put("b", 2);

        Map<String, Integer> other = new HashMap<>();
        other.put("a", 1);
        other.put("b", 2);

        assertTrue(cache.equals(other));
        assertEquals(other.hashCode(), cache.hashCode());
    }

    @Test
    public void testToString() {
        TTLCache<String, Integer> cache = new TTLCache<>(5000L);
        cache.put("one", 1);
        cache.put("two", 2);
        String s = cache.toString();
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        assertTrue(s.contains("one=1"));
        assertTrue(s.contains("two=2"));
    }

    @Test
    public void testShutdownDoesNotThrow() {
        TTLCache.shutdown(); // just ensure no exception is thrown
    }

    // -----------------------------------------------------------------------
    //  Edge‑case and exception tests
    // -----------------------------------------------------------------------

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsZeroTTL() {
        new TTLCache<>(0L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsNegativeTTL() {
        new TTLCache<>(-5L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsTooSmallCleanupInterval() {
        new TTLCache<>(100L, -1, 5L);
    }

    @Test
    public void testLRUEvictionWhenMaxSizeExceeded() {
        TTLCache<String, String> cache = new TTLCache<>(5000L, 2, 100L);
        cache.put("first", "1");   // LRU candidate
        cache.put("second", "2");  // now LRU is "first"
        cache.get("second");       // make "second" MRU
        cache.put("third", "3");   // should evict "first"

        assertNull(cache.get("first"));
        assertEquals("2", cache.get("second"));
        assertEquals("3", cache.get("third"));
        assertEquals(2, cache.size());
    }

    @Test
    public void testExpirationRemovesEntry() throws InterruptedException {
        TTLCache<String, Integer> cache = new TTLCache<>(50L, -1, 20L);
        cache.put("temp", 999);
        assertEquals(Integer.valueOf(999), cache.get("temp"));
        Thread.sleep(80L); // exceed ttl
        assertNull(cache.get("temp"));
        assertFalse(cache.containsKey("temp"));
        assertEquals(0, cache.size());
    }

    @Test
    public void testContainsKeyAfterExpiration() throws InterruptedException {
        TTLCache<String, String> cache = new TTLCache<>(30L, -1, 15L);
        cache.put("k", "v");
        Thread.sleep(50L);
        assertFalse(cache.containsKey("k"));
    }

    @Test
    public void testNullKeyAndNullValue() {
        TTLCache<String, String> cache = new TTLCache<>(5000L);
        cache.put(null, "valueForNullKey");
        cache.put("nullValue", null);
        assertEquals("valueForNullKey", cache.get(null));
        assertNull(cache.get("nullValue"));
        assertTrue(cache.containsKey(null));
        assertTrue(cache.containsValue(null));
    }

    @Test(expected = IllegalStateException.class)
    public void testEntryIteratorRemoveBeforeNext() {
        TTLCache<String, Integer> cache = new TTLCache<>(5000L);
        cache.put("a", 1);
        Iterator<Entry<String, Integer>> it = cache.entrySet().iterator();
        it.remove(); // illegal because next() not yet called
    }

    @Test(expected = IllegalStateException.class)
    public void testEntryIteratorRemoveTwice() {
        TTLCache<String, Integer> cache = new TTLCache<>(5000L);
        cache.put("a", 1);
        Iterator<Entry<String, Integer>> it = cache.entrySet().iterator();
        it.next();
        it.remove(); // first removal OK
        it.remove(); // second removal without advancing -> IllegalStateException
    }

    @Test
    public void testPutAllHandlesNulls() {
        TTLCache<String, String> cache = new TTLCache<>(5000L);
        Map<String, String> src = new HashMap<>();
        src.put(null, "nullKey");
        src.put("nullValue", null);
        cache.putAll(src);
        assertEquals("nullKey", cache.get(null));
        assertNull(cache.get("nullValue"));
    }

    // -----------------------------------------------------------------------
    //  Cleanup after all tests
    // -----------------------------------------------------------------------

    @AfterClass
    public static void tearDownClass() {
        // Shut down the shared scheduler to not leak threads after the test suite
        TTLCache.shutdown();
    }
}
