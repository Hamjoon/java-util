package com.cedarsoftware.util;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link TTLCache}.
 *
 * The test suite is organised as follows:
 *
 * 1.  Public API extraction (method signatures) – listed in the comments of the class.
 * 2.  Basic functionality tests – one test per public method that verifies the
 *     expected “happy‑path” behaviour.
 * 3.  Edge‑case & exception tests – tests that target invalid arguments,
 *     TTL expiration, LRU eviction, null handling, iterator removal,
 *     equals/hashCode contract and the static {@code shutdown()} method.
 *
 * The tests use only the public constructors and methods defined in the source
 * file (no mocking, no subclassing).  The background purge task is exercised
 * indirectly by waiting for the TTL to elapse and then invoking {@code get} or
 * {@code containsKey}, which trigger removal of expired entries.
 */
public class TTLCacheTest {

    /* ----------------------------------------------------------------------
     * 1. Public method signatures (extracted from TTLCache source)
     * ---------------------------------------------------------------------- */
    // Constructors
    // public TTLCache(long ttlMillis)
    // public TTLCache(long ttlMillis, int maxSize)
    // public TTLCache(long ttlMillis, int maxSize, long cleanupIntervalMillis)

    // Map interface methods (public)
    // public V put(K key, V value)
    // public V get(Object key)
    // public V remove(Object key)
    // public void clear()
    // public int size()
    // public boolean isEmpty()
    // public boolean containsKey(Object key)
    // public boolean containsValue(Object value)
    // public void putAll(Map<? extends K, ? extends V> m)
    // public Set<K> keySet()
    // public java.util.Collection<V> values()
    // public Set<java.util.Map.Entry<K, V>> entrySet()
    // public boolean equals(Object o)
    // public int hashCode()
    // public String toString()

    // Static utility
    // public static void shutdown()

    /* ----------------------------------------------------------------------
     * 2. Basic functionality tests – one per public method
     * ---------------------------------------------------------------------- */

    private TTLCache<Integer, String> cache;

    @Before
    public void setUp() {
        // TTL = 500 ms, maxSize = unlimited, cleanup interval = 200 ms
        cache = new TTLCache<>(500, -1, 200);
    }

    @AfterClass
    public static void tearDownClass() {
        // Ensure the shared scheduler is terminated after the whole suite runs
        TTLCache.shutdown();
    }

    @Test
    public void testPutAndGetBasic() {
        String old = cache.put(1, "one");
        assertNull(old);
        assertEquals("one", cache.get(1));
    }

    @Test
    public void testPutReturnsPreviousValue() {
        cache.put(1, "first");
        String prev = cache.put(1, "second");
        assertEquals("first", prev);
        assertEquals("second", cache.get(1));
    }

    @Test
    public void testRemove() {
        cache.put(10, "ten");
        String removed = cache.remove(10);
        assertEquals("ten", removed);
        assertNull(cache.get(10));
        assertTrue(cache.isEmpty());
    }

    @Test
    public void testClear() {
        cache.put(1, "a");
        cache.put(2, "b");
        cache.clear();
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
    }

    @Test
    public void testSizeAndIsEmpty() {
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());

        cache.put(5, "five");
        assertFalse(cache.isEmpty());
        assertEquals(1, cache.size());
    }

    @Test
    public void testContainsKeyAndContainsValue() {
        cache.put(100, "hundred");
        assertTrue(cache.containsKey(100));
        assertTrue(cache.containsValue("hundred"));

        assertFalse(cache.containsKey(200));
        assertFalse(cache.containsValue("two hundred"));
    }

    @Test
    public void testPutAll() {
        Map<Integer, String> source = new HashMap<>();
        source.put(1, "one");
        source.put(2, "two");
        cache.putAll(source);

        assertEquals(2, cache.size());
        assertEquals("one", cache.get(1));
        assertEquals("two", cache.get(2));
    }

    @Test
    public void testKeySet() {
        cache.put(1, "a");
        cache.put(2, "b");
        Set<Integer> keys = cache.keySet();
        assertEquals(2, keys.size());
        assertTrue(keys.contains(1));
        assertTrue(keys.contains(2));
    }

    @Test
    public void testValues() {
        cache.put(1, "alpha");
        cache.put(2, "beta");
        java.util.Collection<String> values = cache.values();
        assertEquals(2, values.size());
        assertTrue(values.contains("alpha"));
        assertTrue(values.contains("beta"));
    }

    @Test
    public void testEntrySetIterationAndRemove() {
        cache.put(1, "one");
        cache.put(2, "two");
        Set<Map.Entry<Integer, String>> entrySet = cache.entrySet();
        assertEquals(2, entrySet.size());

        Iterator<Map.Entry<Integer, String>> it = entrySet.iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, String> e = it.next();
            if (e.getKey().equals(1)) {
                it.remove(); // triggers TTLCache.EntryIterator.remove()
            }
        }
        assertFalse(cache.containsKey(1));
        assertTrue(cache.containsKey(2));
        assertEquals(1, cache.size());
    }

    @Test
    public void testEqualsAndHashCode() {
        cache.put(1, "one");
        cache.put(2, "two");

        Map<Integer, String> other = new HashMap<>();
        other.put(1, "one");
        other.put(2, "two");

        assertTrue(cache.equals(other));
        assertEquals(other.hashCode(), cache.hashCode());
    }

    @Test
    public void testToStringFormat() {
        cache.put(1, "one");
        cache.put(2, "two");
        String s = cache.toString();
        // Order is not guaranteed, so we test for the presence of both entries
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        assertTrue(s.contains("1=one"));
        assertTrue(s.contains("2=two"));
    }

    /* ----------------------------------------------------------------------
     * 3. Edge‑case & exception tests
     * ---------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorZeroTtlThrows() {
        new TTLCache<>(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNegativeCleanupIntervalThrows() {
        new TTLCache<>(100, -1, 5);
    }

    @Test
    public void testNullKeyAndNullValue() {
        // The cache uses ConcurrentMapNullSafe internally, which permits null keys/values.
        cache.put(null, "nullKey");
        cache.put(10, null);
        assertEquals("nullKey", cache.get(null));
        assertNull(cache.get(10));

        assertTrue(cache.containsKey(null));
        assertTrue(cache.containsValue(null));
    }

    @Test
    public void testTtlExpiration() throws InterruptedException {
        TTLCache<Integer, String> shortTtlCache = new TTLCache<>(100, -1, 50);
        shortTtlCache.put(1, "temp");
        assertEquals("temp", shortTtlCache.get(1));

        // Wait longer than TTL
        Thread.sleep(200);
        // Access should trigger removal of the expired entry
        assertNull(shortTtlCache.get(1));
        assertEquals(0, shortTtlCache.size());
    }

    @Test
    public void testLruEvictionWhenMaxSizeExceeded() {
        // maxSize = 2, ttl long enough to avoid expiration
        TTLCache<Integer, String> lruCache = new TTLCache<>(5000, 2, 200);
        lruCache.put(1, "one");   // becomes LRU
        lruCache.put(2, "two");   // now 1 is LRU, 2 is MRU
        lruCache.get(1);          // access 1 -> now 2 is LRU, 1 is MRU
        lruCache.put(3, "three"); // should evict key 2

        assertTrue(lruCache.containsKey(1));
        assertFalse(lruCache.containsKey(2));
        assertTrue(lruCache.containsKey(3));
        assertEquals(2, lruCache.size());
    }

    @Test
    public void testContainsKeyRemovesExpiredEntry() throws InterruptedException {
        TTLCache<Integer, String> cacheWithShortTtl = new TTLCache<>(100, -1, 50);
        cacheWithShortTtl.put(1, "expireSoon");
        Thread.sleep(150);
        assertFalse(cacheWithShortTtl.containsKey(1));
        assertEquals(0, cacheWithShortTtl.size());
    }

    @Test
    public void testEntrySetIteratorRemoveWhenEmpty() {
        Set<Map.Entry<Integer, String>> entrySet = cache.entrySet();
        Iterator<Map.Entry<Integer, String>> it = entrySet.iterator();
        try {
            it.remove();
            fail("Expected IllegalStateException when calling remove() before next()");
        } catch (IllegalStateException expected) {
            // expected
        }
    }

    @Test
    public void testStaticShutdownDoesNotThrow() {
        // The method is void and should be safe to call multiple times.
        TTLCache.shutdown();
        // second call – should be a no‑op and not throw
        TTLCache.shutdown();
    }

    /* ----------------------------------------------------------------------
     * Helper methods (none required for this suite)
     * ---------------------------------------------------------------------- */
}
