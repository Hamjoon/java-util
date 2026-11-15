package com.cedarsoftware.util.cache;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link ThreadedLRUCacheStrategy}.
 *
 * The tests cover:
 * <ul>
 *   <li>Construction validation (illegal arguments)</li>
 *   <li>Basic Map operations (get, put, putAll, remove, clear, size, isEmpty)</li>
 *   <li>Null‑key / null‑value handling</li>
 *   <li>LRU eviction logic (timestamp ordering, get‑touch updates)</li>
 *   <li>containsKey / containsValue</li>
 *   <li>Views: entrySet, keySet, values – immutability and content</li>
 *   <li>equals, hashCode and toString contracts</li>
 * </ul>
 *
 * The cleanup task runs on a daemon scheduler; a short {@code cleanupDelayMillis}
 * (10 ms) is used and the test thread sleeps a little longer to allow the
 * asynchronous purge to complete.
 */
public class ThreadedLRUCacheStrategyTest {

    private ThreadedLRUCacheStrategy<String, String> cache;

    /** Small capacity and short cleanup delay make the async purge deterministic for tests. */
    @Before
    public void setUp() {
        cache = new ThreadedLRUCacheStrategy<>(2, 10);
    }

    /* ---------- Constructor argument validation ---------- */

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
        new ThreadedLRUCacheStrategy<>(2, 5);
    }

    /* ---------- Basic Map operations ---------- */

    @Test
    public void testPutAndGetSimple() {
        assertNull(cache.put("a", "alpha"));
        assertEquals("alpha", cache.get("a"));
        assertEquals(1, cache.size());
    }

    @Test
    public void testPutReplacesExistingAndUpdatesTimestamp() throws InterruptedException {
        cache.put("k", "v1");
        Thread.sleep(5);
        assertEquals("v1", cache.put("k", "v2")); // old value returned
        assertEquals("v2", cache.get("k"));
        // after replacement the entry is the most recent; putting a third entry will evict the older one
        cache.put("x", "vx");
        cache.put("y", "vy"); // triggers async cleanup (capacity=2)
        waitForAsyncCleanup();
        // "k" should still be present because it was the most recently accessed
        assertTrue(cache.containsKey("k"));
        // one of "x" or "y" must have been evicted; size must be exactly capacity
        assertEquals(2, cache.size());
    }

    @Test
    public void testPutAllAddsAllEntries() {
        Map<String, String> src = new HashMap<>();
        src.put("1", "one");
        src.put("2", "two");
        cache.putAll(src);
        assertEquals(2, cache.size());
        assertEquals("one", cache.get("1"));
        assertEquals("two", cache.get("2"));
    }

    @Test
    public void testRemoveDeletesEntry() {
        cache.put("del", "value");
        assertEquals("value", cache.remove("del"));
        assertNull(cache.get("del"));
        assertEquals(0, cache.size());
    }

    @Test
    public void testClearEmptiesCache() {
        cache.put("a", "1");
        cache.put("b", "2");
        cache.clear();
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
    }

    @Test
    public void testIsEmptyAndSize() {
        assertTrue(cache.isEmpty());
        cache.put("x", "x");
        assertFalse(cache.isEmpty());
        assertEquals(1, cache.size());
    }

    /* ---------- Null handling ---------- */

    @Test
    public void testNullKeyAndValueSupported() {
        cache.put(null, null);
        assertTrue(cache.containsKey(null));
        assertTrue(cache.containsValue(null));
        assertNull(cache.get(null));
        assertNull(cache.remove(null));
        assertFalse(cache.containsKey(null));
    }

    /* ---------- LRU eviction logic ---------- */

    @Test
    public void testLeastRecentlyUsedIsEvicted() throws InterruptedException {
        // capacity = 2
        cache.put("first", "1");   // timestamp T1
        Thread.sleep(5);
        cache.put("second", "2");  // timestamp T2 (newer)
        Thread.sleep(5);
        // Access "first" to make it most recent (timestamp T3)
        assertEquals("1", cache.get("first"));
        Thread.sleep(5);
        // Insert third entry; size becomes 3 -> async cleanup scheduled
        cache.put("third", "3");
        waitForAsyncCleanup();

        // After cleanup we expect "second" (the *least* recently used) to be gone
        assertTrue(cache.containsKey("first"));
        assertTrue(cache.containsKey("third"));
        assertFalse(cache.containsKey("second"));
        assertEquals(2, cache.size());
    }

    /* ---------- containsKey / containsValue ---------- */

    @Test
    public void testContainsMethods() {
        cache.put("k1", "v1");
        cache.put("k2", null);
        assertTrue(cache.containsKey("k1"));
        assertTrue(cache.containsKey("k2"));
        assertFalse(cache.containsKey("missing"));

        assertTrue(cache.containsValue("v1"));
        assertTrue(cache.containsValue(null));
        assertFalse(cache.containsValue("nope"));
    }

    /* ---------- entrySet, keySet, values immutability and content ---------- */

    @Test
    public void testEntrySetIsUnmodifiableAndAccurate() {
        cache.put("e1", "v1");
        cache.put("e2", "v2");
        Set<Map.Entry<String, String>> entries = cache.entrySet();

        assertEquals(2, entries.size());
        assertTrue(entries.contains(new java.util.AbstractMap.SimpleEntry<>("e1", "v1")));
        assertTrue(entries.contains(new java.util.AbstractMap.SimpleEntry<>("e2", "v2")));

        // Attempt to modify – must throw UnsupportedOperationException
        try {
            entries.clear();
            fail("entrySet should be unmodifiable");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    @Test
    public void testKeySetIsUnmodifiableAndAccurate() {
        cache.put("kA", "vA");
        cache.put("kB", "vB");
        Set<String> keys = cache.keySet();

        assertEquals(2, keys.size());
        assertTrue(keys.containsAll(java.util.Arrays.asList("kA", "kB")));

        try {
            keys.remove("kA");
            fail("keySet should be unmodifiable");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    @Test
    public void testValuesIsUnmodifiableAndAccurate() {
        cache.put("a", "1");
        cache.put("b", "2");
        Collection<String> vals = cache.values();

        assertEquals(2, vals.size());
        assertTrue(vals.containsAll(java.util.Arrays.asList("1", "2")));

        try {
            vals.add("3");
            fail("values collection should be unmodifiable");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    /* ---------- equals, hashCode, toString ---------- */

    @Test
    public void testEqualsAndHashCodeAgainstStandardMap() {
        cache.put("x", "X");
        cache.put("y", "Y");

        Map<String, String> stdMap = new HashMap<>();
        stdMap.put("x", "X");
        stdMap.put("y", "Y");

        assertTrue(cache.equals(stdMap));
        assertEquals(cache.hashCode(), stdMap.hashCode());
    }

    @Test
    public void testEqualsIsReflexiveAndHandlesNull() {
        assertTrue(cache.equals(cache));
        assertFalse(cache.equals(null));
        assertFalse(cache.equals("not a map"));
    }

    @Test
    public void testToStringContainsEntries() {
        cache.put("one", "1");
        cache.put("two", "2");
        String s = cache.toString();
        assertNotNull(s);
        // The exact format is delegated to MapUtilities.mapToString; we only verify content.
        assertTrue(s.contains("one"));
        assertTrue(s.contains("1"));
        assertTrue(s.contains("two"));
        assertTrue(s.contains("2"));
    }

    /* ---------- Helper: wait for the asynchronous cleanup to finish ---------- */

    /**
     * Waits a short period (greater than {@code cleanupDelayMillis}) for the
     * background purge task to execute. The wait loop checks the cache size
     * and returns early if the size is at or below capacity.
     */
    private void waitForAsyncCleanup() throws InterruptedException {
        long deadline = System.currentTimeMillis() + 200; // generous upper bound
        while (System.currentTimeMillis() < deadline) {
            if (cache.size() <= cache.getCapacity()) {
                return;
            }
            TimeUnit.MILLISECONDS.sleep(5);
        }
        fail("Async cleanup did not finish within expected time");
    }
}
