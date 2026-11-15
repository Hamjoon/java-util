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
 *   <li>Constructor argument validation</li>
 *   <li>Basic put/get functionality</li>
 *   <li>TTL expiration handling</li>
 *   <li>LRU eviction when a maxSize is supplied</li>
 *   <li>Map contract methods (remove, clear, size, isEmpty, containsKey, containsValue, keySet, values, entrySet)</li>
 *   <li>Equality, hashCode and toString implementations</li>
 *   <li>putAll handling</li>
 * </ul>
 *
 * No external mocking frameworks are used; all objects are created via the public API of {@link TTLCache}
 * and its standard Java dependencies.
 */
public class TTLCacheTest {

    /** Small TTL (50 ms) used for expiration tests */
    private static final long SHORT_TTL = 50L;

    /** Cleanup interval (10 ms) – short enough for tests but respects the minimum constraint */
    private static final long CLEANUP_INTERVAL = 10L;

    /** Maximum size for LRU tests */
    private static final int MAX_SIZE = 3;

    /** Fresh cache instance for each test */
    private TTLCache<String, String> cache;

    @Before
    public void setUp() {
        // default cache without LRU limit – each test can replace it with a custom configuration if needed
        cache = new TTLCache<>(1000L, -1, CLEANUP_INTERVAL);
    }

    @AfterClass
    public static void tearDownClass() {
        // Shut down the shared scheduler once all tests have finished
        TTLCache.shutdown();
    }

    /* --------------------------------------------------------------------- *
     *  Constructor validation
     * --------------------------------------------------------------------- */

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

    /* --------------------------------------------------------------------- *
     *  Basic put / get
     * --------------------------------------------------------------------- */

    @Test
    public void testPutAndGet() {
        String previous = cache.put("k1", "v1");
        assertNull("Previous value should be null on first insert", previous);
        assertEquals("v1", cache.get("k1"));
        assertEquals(1, cache.size());
    }

    @Test
    public void testPutReplacesExistingValue() {
        cache.put("k1", "v1");
        String old = cache.put("k1", "v2");
        assertEquals("v1", old);
        assertEquals("v2", cache.get("k1"));
        assertEquals(1, cache.size());
    }

    /* --------------------------------------------------------------------- *
     *  TTL expiration
     * --------------------------------------------------------------------- */

    @Test
    public void testGetReturnsNullWhenEntryExpired() throws InterruptedException {
        TTLCache<String, String> shortCache = new TTLCache<>(SHORT_TTL, -1, CLEANUP_INTERVAL);
        shortCache.put("a", "alpha");
        // Wait longer than TTL
        Thread.sleep(SHORT_TTL + 20);
        assertNull("Entry should be expired and thus return null", shortCache.get("a"));
        assertFalse("containsKey must report false for expired entry", shortCache.containsKey("a"));
        assertEquals(0, shortCache.size());
    }

    @Test
    public void testContainsKeyRemovesExpiredEntry() throws InterruptedException {
        TTLCache<String, String> shortCache = new TTLCache<>(SHORT_TTL, -1, CLEANUP_INTERVAL);
        shortCache.put("x", "y");
        Thread.sleep(SHORT_TTL + 20);
        assertFalse(shortCache.containsKey("x"));
        // The internal map should have been cleaned up
        assertEquals(0, shortCache.size());
    }

    /* --------------------------------------------------------------------- *
     *  LRU eviction
     * --------------------------------------------------------------------- */

    @Test
    public void testLRUEvictionWhenMaxSizeExceeded() {
        TTLCache<String, String> lruCache = new TTLCache<>(1000L, MAX_SIZE, CLEANUP_INTERVAL);
        // Insert MAX_SIZE + 1 entries; the first one should be evicted
        lruCache.put("1", "one");
        lruCache.put("2", "two");
        lruCache.put("3", "three");
        lruCache.put("4", "four"); // triggers eviction

        assertNull("Least recently used entry should have been evicted", lruCache.get("1"));
        assertEquals("two", lruCache.get("2"));
        assertEquals("three", lruCache.get("3"));
        assertEquals("four", lruCache.get("4"));
        assertEquals(MAX_SIZE, lruCache.size());
    }

    @Test
    public void testLRUUpdatesOnAccess() {
        TTLCache<String, String> lruCache = new TTLCache<>(1000L, MAX_SIZE, CLEANUP_INTERVAL);
        lruCache.put("A", "a");
        lruCache.put("B", "b");
        lruCache.put("C", "c");
        // Access "A" making it most recently used
        assertEquals("a", lruCache.get("A"));
        // Insert a new entry causing eviction
        lruCache.put("D", "d");
        // "B" should be evicted (it is now the LRU)
        assertNull(lruCache.get("B"));
        assertEquals("a", lruCache.get("A"));
        assertEquals("c", lruCache.get("C"));
        assertEquals("d", lruCache.get("D"));
    }

    /* --------------------------------------------------------------------- *
     *  remove / clear
     * --------------------------------------------------------------------- */

    @Test
    public void testRemoveDeletesEntryAndUpdatesLRUList() {
        cache.put("k", "v");
        assertEquals("v", cache.remove("k"));
        assertNull(cache.get("k"));
        assertEquals(0, cache.size());
    }

    @Test
    public void testClearEmptiesCache() {
        cache.put("a", "1");
        cache.put("b", "2");
        cache.clear();
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
        assertFalse(cache.containsKey("a"));
        assertFalse(cache.containsKey("b"));
    }

    /* --------------------------------------------------------------------- *
     *  keySet, values, entrySet
     * --------------------------------------------------------------------- */

    @Test
    public void testKeySetAndValuesReflectCacheContents() {
        cache.put("k1", "v1");
        cache.put("k2", "v2");

        Set<String> keys = cache.keySet();
        assertTrue(keys.containsAll(java.util.Arrays.asList("k1", "k2")));
        assertEquals(2, keys.size());

        java.util.Collection<String> values = cache.values();
        assertTrue(values.containsAll(java.util.Arrays.asList("v1", "v2")));
        assertEquals(2, values.size());
    }

    @Test
    public void testEntrySetIterationAndRemove() {
        cache.put("one", "1");
        cache.put("two", "2");
        cache.put("three", "3");

        Set<Map.Entry<String, String>> entrySet = cache.entrySet();
        assertEquals(3, entrySet.size());

        Iterator<Map.Entry<String, String>> it = entrySet.iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> e = it.next();
            if ("two".equals(e.getKey())) {
                it.remove(); // should remove from cache as well
            }
        }

        assertEquals(2, cache.size());
        assertFalse(cache.containsKey("two"));
        assertTrue(cache.containsKey("one"));
        assertTrue(cache.containsKey("three"));
    }

    @Test
    public void testEntrySetRemoveWithoutCallingIteratorRemoveThrows() {
        cache.put("x", "y");
        Set<Map.Entry<String, String>> entrySet = cache.entrySet();
        try {
            entrySet.clear(); // clear uses cache.clear() – should work
        } catch (Exception e) {
            fail("clear() on entrySet should not throw");
        }

        // Re‑populate and attempt illegal iterator.remove()
        cache.put("a", "1");
        Iterator<Map.Entry<String, String>> it = cache.entrySet().iterator();
        it.next(); // advance to first element
        it.remove(); // valid
        assertEquals(0, cache.size());

        // Now calling remove again without next() should throw IllegalStateException
        try {
            it.remove();
            fail("Expected IllegalStateException when calling remove() twice without next()");
        } catch (IllegalStateException expected) {
            // expected
        }
    }

    /* --------------------------------------------------------------------- *
     *  putAll
     * --------------------------------------------------------------------- */

    @Test
    public void testPutAllCopiesAllEntries() {
        Map<String, String> source = new HashMap<>();
        source.put("a", "alpha");
        source.put("b", "beta");
        source.put("c", "gamma");

        cache.putAll(source);
        assertEquals(3, cache.size());
        assertEquals("alpha", cache.get("a"));
        assertEquals("beta", cache.get("b"));
        assertEquals("gamma", cache.get("c"));
    }

    /* --------------------------------------------------------------------- *
     *  equals, hashCode, toString
     * --------------------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCode() {
        TTLCache<String, String> c1 = new TTLCache<>(1000L, -1, CLEANUP_INTERVAL);
        TTLCache<String, String> c2 = new TTLCache<>(1000L, -1, CLEANUP_INTERVAL);

        c1.put("k", "v");
        c2.put("k", "v");

        assertTrue(c1.equals(c2));
        assertTrue(c2.equals(c1));
        assertEquals(c1.hashCode(), c2.hashCode());

        // Changing one cache should break equality
        c2.put("x", "y");
        assertFalse(c1.equals(c2));
        assertFalse(c2.equals(c1));
    }

    @Test
    public void testToStringFormat() {
        cache.put("one", "1");
        cache.put("two", "2");
        String s = cache.toString();
        // The string must start with '{' and end with '}'
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        // It should contain both entries (order not guaranteed)
        assertTrue(s.contains("one=1"));
        assertTrue(s.contains("two=2"));
        // No trailing comma
        assertFalse(s.contains(", }"));
    }

    /* --------------------------------------------------------------------- *
     *  size / isEmpty
     * --------------------------------------------------------------------- */

    @Test
    public void testSizeAndIsEmpty() {
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());

        cache.put("k", "v");
        assertFalse(cache.isEmpty());
        assertEquals(1, cache.size());

        cache.remove("k");
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
    }

    /* --------------------------------------------------------------------- *
     *  containsValue (does not consider TTL)
     * --------------------------------------------------------------------- */

    @Test
    public void testContainsValueIgnoresExpiration() throws InterruptedException {
        TTLCache<String, String> shortCache = new TTLCache<>(SHORT_TTL, -1, CLEANUP_INTERVAL);
        shortCache.put("a", "value");
        assertTrue(shortCache.containsValue("value"));
        Thread.sleep(SHORT_TTL + 20);
        // After expiration the value is no longer present in the map
        assertFalse(shortCache.containsValue("value"));
    }

    /* --------------------------------------------------------------------- *
     *  Internal consistency after concurrent-like operations
     * --------------------------------------------------------------------- */

    @Test
    public void testMultipleOperationsMaintainLRUIntegrity() {
        TTLCache<String, String> lruCache = new TTLCache<>(1000L, 2, CLEANUP_INTERVAL);
        lruCache.put("first", "1");
        lruCache.put("second", "2");
        // Access "first" to make it MRU
        assertEquals("1", lruCache.get("first"));
        // Adding third entry should evict "second"
        lruCache.put("third", "3");
        assertNull(lruCache.get("second"));
        assertEquals("1", lruCache.get("first"));
        assertEquals("3", lruCache.get("third"));
    }

    /* --------------------------------------------------------------------- *
     *  Ensure that the background purge task does not interfere with API usage
     * --------------------------------------------------------------------- */

    @Test
    public void testPurgeTaskDoesNotThrowWhenCacheIsEmpty() throws Exception {
        // The scheduler runs automatically; we just wait a short period to allow it to execute.
        Thread.sleep(CLEANUP_INTERVAL * 2);
        // No exception means the task handled the empty cache gracefully.
        assertTrue(cache.isEmpty());
    }
}
