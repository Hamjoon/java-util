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
 * The tests cover:
 * <ul>
 *   <li>Constructor validation (illegal arguments)</li>
 *   <li>Basic Map operations (put, get, remove, clear, size, isEmpty)</li>
 *   <li>TTL expiration handling</li>
 *   <li>LRU eviction when a maxSize is defined</li>
 *   <li>Null key / null value support</li>
 *   <li>containsKey / containsValue semantics</li>
 *   <li>putAll, keySet, values, entrySet iterator & removal</li>
 *   <li>equals, hashCode and toString contracts</li>
 *   <li>Scheduler shutdown</li>
 * </ul>
 *
 * All tests use only the public API of {@link TTLCache} and standard JDK classes.
 */
public class TTLCacheTest {

    /** Small TTL (30 ms) and very short cleanup interval (20 ms) to keep tests fast. */
    private static final long TTL_MILLIS = 30L;
    private static final long CLEANUP_INTERVAL = 20L;

    private TTLCache<String, String> cache;

    @Before
    public void setUp() {
        // Use a cache without LRU limit for most tests
        cache = new TTLCache<>(TTL_MILLIS, -1, CLEANUP_INTERVAL);
    }

    @AfterClass
    public static void tearDownClass() {
        // Shut down the shared scheduler once all tests have finished
        TTLCache.shutdown();
    }

    /* ---------------------------------------------------------------------- */
    /* Constructor validation                                                 */
    /* ---------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void ctorZeroTtlThrows() {
        new TTLCache<>(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ctorNegativeCleanupThrows() {
        new TTLCache<>(10, -1, 5);
    }

    /* ---------------------------------------------------------------------- */
    /* Basic Map contract (put / get / remove / clear / size / isEmpty)       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void putAndGetSimple() {
        assertNull(cache.put("a", "alpha"));
        assertEquals("alpha", cache.get("a"));
        assertEquals(1, cache.size());
        assertFalse(cache.isEmpty());
    }

    @Test
    public void replaceExistingValue() {
        cache.put("k", "v1");
        String old = cache.put("k", "v2");
        assertEquals("v1", old);
        assertEquals("v2", cache.get("k"));
        assertEquals(1, cache.size());
    }

    @Test
    public void removeExistingKey() {
        cache.put("x", "y");
        String removed = cache.remove("x");
        assertEquals("y", removed);
        assertNull(cache.get("x"));
        assertEquals(0, cache.size());
    }

    @Test
    public void removeNonExistingKeyReturnsNull() {
        assertNull(cache.remove("missing"));
    }

    @Test
    public void clearEmptiesCache() {
        cache.put("1", "one");
        cache.put("2", "two");
        cache.clear();
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
    }

    /* ---------------------------------------------------------------------- */
    /* TTL expiration handling                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void entryExpiresAfterTtl() throws InterruptedException {
        cache.put("temp", "data");
        assertEquals("data", cache.get("temp")); // still valid

        // Wait longer than TTL; the entry should be considered expired
        Thread.sleep(TTL_MILLIS + 20);
        assertNull(cache.get("temp"));
        assertEquals(0, cache.size());
    }

    @Test
    public void getDoesNotRefreshExpiration() throws InterruptedException {
        cache.put("a", "1");
        Thread.sleep(TTL_MILLIS / 2);
        // First access – still within TTL
        assertEquals("1", cache.get("a"));
        // Wait past original expiry (no refresh)
        Thread.sleep(TTL_MILLIS);
        assertNull(cache.get("a"));
    }

    /* ---------------------------------------------------------------------- */
    /* LRU eviction when maxSize is defined                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void lruEvictionRespectsMaxSize() {
        TTLCache<String, String> lruCache = new TTLCache<>(TTL_MILLIS, 2, CLEANUP_INTERVAL);
        lruCache.put("first", "1");   // LRU order: first
        lruCache.put("second", "2");  // LRU order: first, second

        // Access "first" to make it MRU
        assertEquals("1", lruCache.get("first")); // LRU order: second, first

        // Adding third entry should evict "second"
        lruCache.put("third", "3");
        assertNull(lruCache.get("second"));
        assertEquals("1", lruCache.get("first"));
        assertEquals("3", lruCache.get("third"));
        assertEquals(2, lruCache.size());
    }

    @Test
    public void lruEvictionWhenLockNotAcquiredSkipsUpdate() {
        // The lock.tryLock() may fail under heavy contention, but we cannot force it.
        // This test simply verifies that the cache still respects maxSize even if the lock
        // is not taken (the eviction path uses removeEntry which acquires the lock internally).
        TTLCache<String, String> lruCache = new TTLCache<>(TTL_MILLIS, 1, CLEANUP_INTERVAL);
        lruCache.put("a", "A");
        lruCache.put("b", "B"); // should evict "a"
        assertNull(lruCache.get("a"));
        assertEquals("B", lruCache.get("b"));
    }

    /* ---------------------------------------------------------------------- */
    /* Null key / null value support                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void nullKeySupported() {
        cache.put(null, "nullKey");
        assertEquals("nullKey", cache.get(null));
        assertTrue(cache.containsKey(null));
        assertTrue(cache.containsValue("nullKey"));
        assertEquals(1, cache.size());
    }

    @Test
    public void nullValueSupported() {
        cache.put("nullVal", null);
        assertNull(cache.get("nullVal"));
        assertTrue(cache.containsKey("nullVal"));
        assertTrue(cache.containsValue(null));
    }

    /* ---------------------------------------------------------------------- */
    /* containsKey / containsValue                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void containsKeyAndValueReflectExpiration() throws InterruptedException {
        cache.put("k", "v");
        assertTrue(cache.containsKey("k"));
        assertTrue(cache.containsValue("v"));

        Thread.sleep(TTL_MILLIS + 20);
        assertFalse(cache.containsKey("k"));
        assertFalse(cache.containsValue("v"));
    }

    /* ---------------------------------------------------------------------- */
    /* putAll, keySet, values, entrySet iterator & removal                  */
    /* ---------------------------------------------------------------------- */

    @Test
    public void putAllCopiesAllEntries() {
        Map<String, String> source = new HashMap<>();
        source.put("a", "1");
        source.put("b", "2");
        cache.putAll(source);
        assertEquals(2, cache.size());
        assertEquals("1", cache.get("a"));
        assertEquals("2", cache.get("b"));
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
        cache.put("x", "X");
        cache.put("y", "Y");
        assertTrue(cache.values().contains("X"));
        assertTrue(cache.values().contains("Y"));
        assertEquals(2, cache.values().size());
    }

    @Test
    public void entrySetIterationAndRemove() {
        cache.put("one", "1");
        cache.put("two", "2");

        Iterator<Map.Entry<String, String>> it = cache.entrySet().iterator();
        assertTrue(it.hasNext());
        Map.Entry<String, String> e = it.next();
        assertNotNull(e);
        // Remove via iterator
        it.remove();

        // After removal, the cache should contain only one entry
        assertEquals(1, cache.size());
        // The removed key should be absent
        assertFalse(cache.containsKey(e.getKey()));
    }

    @Test(expected = IllegalStateException.class)
    public void entrySetIteratorRemoveWithoutNextThrows() {
        cache.put("a", "A");
        Iterator<Map.Entry<String, String>> it = cache.entrySet().iterator();
        // Directly call remove without calling next()
        it.remove();
    }

    /* ---------------------------------------------------------------------- */
    /* equals, hashCode and toString                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void equalsAndHashCodeMatchStandardMap() {
        cache.put("k1", "v1");
        cache.put("k2", "v2");

        Map<String, String> javaMap = new HashMap<>();
        javaMap.put("k1", "v1");
        javaMap.put("k2", "v2");

        assertTrue(cache.equals(javaMap));
        assertEquals(javaMap.hashCode(), cache.hashCode());
    }

    @Test
    public void equalsHandlesDifferentEntryOrder() {
        TTLCache<String, String> c1 = new TTLCache<>(TTL_MILLIS);
        TTLCache<String, String> c2 = new TTLCache<>(TTL_MILLIS);
        c1.put("a", "1");
        c1.put("b", "2");
        c2.put("b", "2");
        c2.put("a", "1");
        assertTrue(c1.equals(c2));
        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    public void toStringProducesExpectedFormat() {
        cache.put("x", "X");
        cache.put("y", "Y");
        String s = cache.toString();
        // Order is based on insertion (no LRU moves because lock may be obtained)
        // Accept both possible orders
        assertTrue(s.equals("{x=X, y=Y}") || s.equals("{y=Y, x=X}"));
        assertTrue(s.startsWith("{") && s.endsWith("}"));
    }

    /* ---------------------------------------------------------------------- */
    /* Scheduler shutdown verification (no exception on multiple calls)     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void shutdownCanBeCalledMultipleTimesSafely() {
        // First shutdown already called in @AfterClass; calling again should not fail
        TTLCache.shutdown();
        TTLCache.shutdown(); // no exception expected
    }

    /* ---------------------------------------------------------------------- */
    /* Helper – small sleep wrapper (handles InterruptedException)          */
    /* ---------------------------------------------------------------------- */
    private void shortSleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
