package com.cedarsoftware.util;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link TTLCache}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public constructors and their validation logic.</li>
 *   <li>Basic Map operations (put, get, remove, clear, size, isEmpty).</li>
 *   <li>Support for {@code null} keys and values.</li>
 *   <li>Time‑to‑live expiration.</li>
 *   <li>LRU eviction when a maximum size is supplied.</li>
 *   <li>Views returned by {@code keySet()}, {@code values()}, {@code entrySet()} and iterator removal.</li>
 *   <li>Equality, hashCode and {@code toString()} contracts.</li>
 *   <li>Shutdown of the shared scheduler.</li>
 * </ul>
 *
 * The TTL and cleanup intervals are kept very small so that the tests run quickly
 * and deterministically.
 */
public class TTLCacheTest {

    /** A short TTL (in ms) used for most tests – 50 ms. */
    private static final long SHORT_TTL = 50L;

    /** A short cleanup interval (in ms) – 20 ms. */
    private static final long SHORT_CLEANUP = 20L;

    private TTLCache<String, String> cache;

    @Before
    public void setUp() {
        // Each test gets its own fresh cache instance.
        cache = new TTLCache<>(SHORT_TTL, -1, SHORT_CLEANUP);
    }

    @AfterClass
    public static void tearDownClass() {
        // Shut down the shared scheduler so the JVM can exit cleanly after the tests.
        TTLCache.shutdown();
    }

    /* ----------------------------------------------------------------------
     *  Constructor validation
     * ---------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void constructorShouldRejectZeroTtl() {
        new TTLCache<>(0L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorShouldRejectNegativeTtl() {
        new TTLCache<>(-5L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorShouldRejectTooSmallCleanupInterval() {
        new TTLCache<>(100L, -1, 5L);
    }

    @Test
    public void constructorWithAllParametersWorks() {
        TTLCache<Integer, Integer> c = new TTLCache<>(100L, 10, 50L);
        assertNotNull(c);
        assertTrue(c.isEmpty());
    }

    /* ----------------------------------------------------------------------
     *  Basic put / get / remove
     * ---------------------------------------------------------------------- */

    @Test
    public void putAndGetBasic() {
        assertNull(cache.put("k1", "v1"));
        assertEquals("v1", cache.get("k1"));
        assertEquals(1, cache.size());
    }

    @Test
    public void putReturnsPreviousValue() {
        cache.put("k1", "v1");
        assertEquals("v1", cache.put("k1", "v2"));
        assertEquals("v2", cache.get("k1"));
    }

    @Test
    public void removeDeletesEntry() {
        cache.put("k1", "v1");
        assertEquals("v1", cache.remove("k1"));
        assertNull(cache.get("k1"));
        assertTrue(cache.isEmpty());
    }

    @Test
    public void clearEmptiesCache() {
        cache.put("a", "1");
        cache.put("b", "2");
        cache.clear();
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
    }

    /* ----------------------------------------------------------------------
     *  Null handling (the underlying ConcurrentHashMapNullSafe supports it)
     * ---------------------------------------------------------------------- */

    @Test
    public void putNullKeyAndValue() {
        cache.put(null, null);
        assertTrue(cache.containsKey(null));
        assertTrue(cache.containsValue(null));
        assertNull(cache.get(null));
        assertEquals(1, cache.size());
    }

    @Test
    public void containsValueHandlesNull() {
        cache.put("x", null);
        assertTrue(cache.containsValue(null));
    }

    /* ----------------------------------------------------------------------
     *  Expiration (TTL)
     * ---------------------------------------------------------------------- */

    @Test
    public void entryExpiresAfterTtl() throws InterruptedException {
        cache.put("temp", "value");
        assertEquals("value", cache.get("temp"));
        // Wait longer than TTL; the next get should purge the entry.
        Thread.sleep(SHORT_TTL + 30);
        assertNull(cache.get("temp"));
        assertFalse(cache.containsKey("temp"));
        assertEquals(0, cache.size());
    }

    @Test
    public void containsKeyTriggersExpiration() throws InterruptedException {
        cache.put("temp", "v");
        Thread.sleep(SHORT_TTL + 30);
        assertFalse(cache.containsKey("temp"));
        assertEquals(0, cache.size());
    }

    /* ----------------------------------------------------------------------
     *  LRU eviction when maxSize is set
     * ---------------------------------------------------------------------- */

    @Test
    public void lruEvictionRespectsAccessOrder() {
        TTLCache<String, String> lruCache = new TTLCache<>(1000L, 2, 500L);
        lruCache.put("first", "1");   // becomes LRU
        lruCache.put("second", "2");  // most recent
        // Access "first" -> it becomes most recent, "second" becomes LRU
        assertEquals("1", lruCache.get("first"));
        // Adding a third entry should evict "second"
        lruCache.put("third", "3");
        assertNull(lruCache.get("second"));
        assertEquals("1", lruCache.get("first"));
        assertEquals("3", lruCache.get("third"));
        assertEquals(2, lruCache.size());
    }

    @Test
    public void lruEvictionWhenMaxSizeIsZero() {
        TTLCache<String, String> zeroCache = new TTLCache<>(1000L, 0, 500L);
        zeroCache.put("a", "1");
        assertTrue(zeroCache.isEmpty());
        assertNull(zeroCache.get("a"));
    }

    /* ----------------------------------------------------------------------
     *  Bulk operations and view collections
     * ---------------------------------------------------------------------- */

    @Test
    public void putAllCopiesAllEntries() {
        Map<String, String> src = new HashMap<>();
        src.put("x", "10");
        src.put("y", "20");
        cache.putAll(src);
        assertEquals(2, cache.size());
        assertEquals("10", cache.get("x"));
        assertEquals("20", cache.get("y"));
    }

    @Test
    public void keySetReflectsCurrentKeys() {
        cache.put("k1", "v1");
        cache.put("k2", "v2");
        Set<String> keys = cache.keySet();
        assertTrue(keys.contains("k1"));
        assertTrue(keys.contains("k2"));
        assertEquals(2, keys.size());
    }

    @Test
    public void valuesReflectCurrentValues() {
        cache.put("k1", "v1");
        cache.put("k2", "v2");
        assertTrue(cache.values().containsAll(java.util.Arrays.asList("v1", "v2")));
        assertEquals(2, cache.values().size());
    }

    @Test
    public void entrySetIterationAndRemove() {
        cache.put("a", "1");
        cache.put("b", "2");
        Iterator<Map.Entry<String, String>> it = cache.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> e = it.next();
            if ("a".equals(e.getKey())) {
                it.remove(); // should remove entry "a"
            }
        }
        assertFalse(cache.containsKey("a"));
        assertTrue(cache.containsKey("b"));
        assertEquals(1, cache.size());
    }

    /* ----------------------------------------------------------------------
     *  Equality, hashCode and toString
     * ---------------------------------------------------------------------- */

    @Test
    public void equalsAndHashCodeMatchStandardMap() {
        Map<String, String> reference = new HashMap<>();
        reference.put("one", "1");
        reference.put("two", "2");

        cache.putAll(reference);

        assertTrue(cache.equals(reference));
        assertEquals(reference.hashCode(), cache.hashCode());
    }

    @Test
    public void toStringProducesExpectedFormat() {
        cache.put("k1", "v1");
        cache.put("k2", "v2");
        String s = cache.toString();
        // Order is not guaranteed; we just check for the surrounding braces and key/value pairs.
        assertTrue(s.startsWith("{") && s.endsWith("}"));
        assertTrue(s.contains("k1=v1"));
        assertTrue(s.contains("k2=v2"));
    }

    /* ----------------------------------------------------------------------
     *  Static shutdown
     * ---------------------------------------------------------------------- */

    @Test
    public void shutdownDoesNotThrow() {
        // The method is static; calling it multiple times should be safe.
        TTLCache.shutdown();
        TTLCache.shutdown(); // second call – should be a no‑op
    }
}
