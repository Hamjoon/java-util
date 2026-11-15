package com.cedarsoftware.util.cache;

import org.junit.*;
import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Comprehensive JUnit‑4 test suite for {@link ThreadedLRUCacheStrategy}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public API methods (signatures are listed in the class comment).</li>
 *   <li>Basic functional verification.</li>
 *   <li>Edge‑case handling (null keys/values, illegal arguments, self‑reference formatting).</li>
 *   <li>Exception scenarios (unmodifiable collections, illegal constructor arguments).</li>
 *   <li>LRU cleanup behavior when the cache exceeds its capacity.</li>
 * </ul>
 *
 * The shared scheduler used by the cache is shut down once after all tests.
 */
public class ThreadedLRUCacheStrategyTest {

    /** Small delay to allow the background cleanup task to run. */
    private static final int CLEANUP_WAIT_MS = 100;

    /** Helper to create a cache with a very short cleanup delay. */
    private ThreadedLRUCacheStrategy<String, String> createCache(int capacity) {
        // 10 ms is the minimum allowed delay – it gives the cleanup task a chance to fire quickly.
        return new ThreadedLRUCacheStrategy<>(capacity, 10);
    }

    /* ---------------------------------------------------------------------- */
    /*  1. Constructor validation                                               */
    /* ---------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsZeroCapacity() {
        new ThreadedLRUCacheStrategy<>(0, 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsNegativeCapacity() {
        new ThreadedLRUCacheStrategy<>(-5, 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsTooSmallDelay() {
        new ThreadedLRUCacheStrategy<>(5, 5); // must be >= 10
    }

    /* ---------------------------------------------------------------------- */
    /*  2. Basic put / get / size / isEmpty                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testPutAndGet() {
        ThreadedLRUCacheStrategy<String, String> cache = createCache(3);
        assertNull(cache.put("a", "alpha"));
        assertEquals("alpha", cache.get("a"));
        assertEquals(1, cache.size());
        assertFalse(cache.isEmpty());
    }

    @Test
    public void testPutNullKeyAndValue() {
        ThreadedLRUCacheStrategy<String, String> cache = createCache(2);
        assertNull(cache.put(null, null));
        assertNull(cache.get(null));
        assertTrue(cache.containsKey(null));
        assertTrue(cache.containsValue(null));
        assertEquals(1, cache.size());
    }

    @Test
    public void testPutReturnsOldValue() {
        ThreadedLRUCacheStrategy<String, String> cache = createCache(2);
        cache.put("k", "v1");
        assertEquals("v1", cache.put("k", "v2"));
        assertEquals("v2", cache.get("k"));
        assertEquals(1, cache.size());
    }

    /* ---------------------------------------------------------------------- */
    /*  3. remove / clear                                                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testRemoveExistingKey() {
        ThreadedLRUCacheStrategy<String, String> cache = createCache(2);
        cache.put("x", "X");
        assertEquals("X", cache.remove("x"));
        assertNull(cache.get("x"));
        assertEquals(0, cache.size());
    }

    @Test
    public void testRemoveNonExistingKey() {
        ThreadedLRUCacheStrategy<String, String> cache = createCache(2);
        assertNull(cache.remove("missing"));
    }

    @Test
    public void testClearEmptiesCache() {
        ThreadedLRUCacheStrategy<String, String> cache = createCache(3);
        cache.put("a", "1");
        cache.put("b", "2");
        cache.clear();
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
    }

    /* ---------------------------------------------------------------------- */
    /*  4. containsKey / containsValue                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testContainsKeyAndValue() {
        ThreadedLRUCacheStrategy<String, String> cache = createCache(2);
        cache.put("k1", "v1");
        cache.put("k2", null);
        assertTrue(cache.containsKey("k1"));
        assertTrue(cache.containsValue("v1"));
        assertTrue(cache.containsKey("k2"));
        assertTrue(cache.containsValue(null));
        assertFalse(cache.containsKey("no"));
        assertFalse(cache.containsValue("no"));
    }

    /* ---------------------------------------------------------------------- */
    /*  5. putAll                                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testPutAllCopiesEntries() {
        ThreadedLRUCacheStrategy<String, String> cache = createCache(5);
        Map<String, String> src = new HashMap<>();
        src.put("a", "A");
        src.put("b", "B");
        cache.putAll(src);
        assertEquals(2, cache.size());
        assertEquals("A", cache.get("a"));
        assertEquals("B", cache.get("b"));
    }

    /* ---------------------------------------------------------------------- */
    /*  6. entrySet / keySet / values – immutability                           */
    /* ---------------------------------------------------------------------- */

    @Test(expected = UnsupportedOperationException.class)
    public void testEntrySetIsUnmodifiable() {
        ThreadedLRUCacheStrategy<String, String> cache = createCache(2);
        cache.put("k", "v");
        Set<Map.Entry<String, String>> es = cache.entrySet();
        es.add(new AbstractMap.SimpleEntry<>("x", "y"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testKeySetIsUnmodifiable() {
        ThreadedLRUCacheStrategy<String, String> cache = createCache(2);
        cache.put("k", "v");
        Set<String> ks = cache.keySet();
        ks.add("newKey");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testValuesIsUnmodifiable() {
        ThreadedLRUCacheStrategy<String, String> cache = createCache(2);
        cache.put("k", "v");
        Collection<String> vals = cache.values();
        vals.add("newVal");
    }

    /* ---------------------------------------------------------------------- */
    /*  7. equals / hashCode                                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCodeAgainstPlainMap() {
        ThreadedLRUCacheStrategy<String, Integer> cache = createCache(3);
        cache.put("one", 1);
        cache.put("two", 2);

        Map<String, Integer> plain = new HashMap<>();
        plain.put("one", 1);
        plain.put("two", 2);

        assertTrue(cache.equals(plain));
        assertEquals(cache.hashCode(), plain.hashCode());
    }

    @Test
    public void testEqualsReflexiveAndWithNull() {
        ThreadedLRUCacheStrategy<String, String> cache = createCache(1);
        cache.put("a", "A");
        assertTrue(cache.equals(cache));
        assertFalse(cache.equals(null));
    }

    /* ---------------------------------------------------------------------- */
    /*  8. toString – self‑reference handling                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToStringSelfReferenceReplaced() {
        ThreadedLRUCacheStrategy<String, Object> cache = createCache(2);
        cache.put("self", cache); // value points back to the cache itself
        String s = cache.toString();
        // Expected format: {self=(this Map)}
        assertTrue(s.contains("self=(this Map)"));
    }

    @Test
    public void testToStringStandardFormatting() {
        ThreadedLRUCacheStrategy<String, String> cache = createCache(2);
        cache.put("k1", "v1");
        cache.put("k2", "v2");
        String s = cache.toString();
        // Order is not guaranteed, but both entries must appear.
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        assertTrue(s.contains("k1=v1"));
        assertTrue(s.contains("k2=v2"));
    }

    /* ---------------------------------------------------------------------- */
    /*  9. LRU cleanup when capacity is exceeded                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAutomaticCleanupRemovesLeastRecentlyUsed() throws InterruptedException {
        // Capacity 2, delay 10 ms (minimum)
        ThreadedLRUCacheStrategy<Integer, String> cache = createCache(2);
        cache.put(1, "one");   // first inserted
        cache.put(2, "two");   // second inserted
        // Access key 1 to make it most‑recently used
        cache.get(1);
        // Insert third entry – this pushes size to 3 (> capacity)
        cache.put(3, "three");

        // Wait enough time for the scheduled cleanup to run
        TimeUnit.MILLISECONDS.sleep(CLEANUP_WAIT_MS);

        // After cleanup, the LRU entry (key 2) should have been evicted
        assertNull(cache.get(2));
        assertEquals("one", cache.get(1));
        assertEquals("three", cache.get(3));
        assertEquals(2, cache.size());
    }

    /* ---------------------------------------------------------------------- */
    /* 10. Scheduler shutdown                                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testShutdownDoesNotThrow() {
        // The method is static; calling it repeatedly should be safe.
        ThreadedLRUCacheStrategy.shutdown();
        // No assertion – just ensure no exception is thrown.
    }

    /* ---------------------------------------------------------------------- */
    /* 11. Verify that put triggers immediate cleanup flag (indirectly)        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testPutTriggersImmediateCleanupWhenOverCapacity() throws InterruptedException {
        // Capacity 1, very short delay.
        ThreadedLRUCacheStrategy<String, String> cache = createCache(1);
        cache.put("a", "A"); // size = 1 (within capacity)
        cache.put("b", "B"); // size = 2 → should schedule immediate cleanup

        // Wait for the cleanup task to execute.
        TimeUnit.MILLISECONDS.sleep(CLEANUP_WAIT_MS);

        // Only the most‑recently used entry (key "b") should survive.
        assertNull(cache.get("a"));
        assertEquals("B", cache.get("b"));
        assertEquals(1, cache.size());
    }

    /* ---------------------------------------------------------------------- */
    /* 12. Cleanup after all tests                                               */
    /* ---------------------------------------------------------------------- */

    @AfterClass
    public static void shutdownScheduler() {
        ThreadedLRUCacheStrategy.shutdown();
    }
}
