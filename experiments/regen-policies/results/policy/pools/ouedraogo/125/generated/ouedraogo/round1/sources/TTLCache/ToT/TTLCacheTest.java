package com.cedarsoftware.util;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

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
 *   <li>Basic put / get semantics, including overwrite behaviour</li>
 *   <li>Time‑to‑live expiration and the background purge task</li>
 *   <li>LRU eviction when a maximum size is supplied</li>
 *   <li>Removal, clear, size / isEmpty</li>
 *   <li>containsKey / containsValue handling of expired entries</li>
 *   <li>EntrySet iterator removal, equals, hashCode and toString</li>
 *   <li>Support for {@code null} keys and values</li>
 * </ul>
 *
 * The shared scheduler used by {@link TTLCache} is shut down once after all tests
 * to avoid thread‑leak warnings.
 */
public class TTLCacheTest {

    /** Small TTL (ms) used for expiration tests */
    private static final long QUICK_TTL = 50L;

    /** Very short cleanup interval so the background purge runs quickly */
    private static final long QUICK_CLEANUP = 30L;

    @AfterClass
    public static void shutdownScheduler() {
        TTLCache.shutdown();
    }

    @Before
    public void setUp() {
        // No per‑test setup required; each test creates its own cache instance.
    }

    /* -------------------------------------------------
     *  Constructor validation
     * ------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsZeroTtl() {
        new TTLCache<>(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsNegativeTtl() {
        new TTLCache<>(-5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsTooSmallCleanupInterval() {
        new TTLCache<>(100, -1, 5); // cleanup interval < 10 ms
    }

    /* -------------------------------------------------
     *  Basic put / get / size / isEmpty
     * ------------------------------------------------- */

    @Test
    public void testPutAndGetBasic() {
        TTLCache<String, Integer> cache = new TTLCache<>(1000);
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());

        Integer old = cache.put("a", 1);
        assertNull(old);
        assertEquals(Integer.valueOf(1), cache.get("a"));
        assertFalse(cache.isEmpty());
        assertEquals(1, cache.size());
    }

    @Test
    public void testPutOverwriteReturnsOldValue() {
        TTLCache<String, String> cache = new TTLCache<>(1000);
        cache.put("key", "first");
        String old = cache.put("key", "second");
        assertEquals("first", old);
        assertEquals("second", cache.get("key"));
        assertEquals(1, cache.size());
    }

    @Test
    public void testNullKeyAndValueSupported() {
        TTLCache<String, String> cache = new TTLCache<>(1000);
        cache.put(null, null);
        assertTrue(cache.containsKey(null));
        assertTrue(cache.containsValue(null));
        assertNull(cache.get(null));
        assertEquals(1, cache.size());
    }

    /* -------------------------------------------------
     *  TTL expiration and background purge task
     * ------------------------------------------------- */

    @Test
    public void testEntryExpiresAfterTtl() throws InterruptedException {
        TTLCache<String, String> cache = new TTLCache<>(QUICK_TTL, -1, QUICK_CLEANUP);
        cache.put("temp", "value");

        // Immediately present
        assertEquals("value", cache.get("temp"));
        assertTrue(cache.containsKey("temp"));

        // Wait beyond TTL and a little extra for the purge task to run
        Thread.sleep(QUICK_TTL + 2 * QUICK_CLEANUP);

        // Should be gone because of expiration
        assertNull(cache.get("temp"));
        assertFalse(cache.containsKey("temp"));
        assertEquals(0, cache.size());
    }

    @Test
    public void testContainsKeyRemovesExpiredEntry() throws InterruptedException {
        TTLCache<Integer, String> cache = new TTLCache<>(QUICK_TTL, -1, QUICK_CLEANUP);
        cache.put(1, "one");
        Thread.sleep(QUICK_TTL + QUICK_CLEANUP);
        assertFalse(cache.containsKey(1));
        assertEquals(0, cache.size());
    }

    @Test
    public void testContainsValueIgnoresExpiredEntry() throws InterruptedException {
        TTLCache<Integer, String> cache = new TTLCache<>(QUICK_TTL, -1, QUICK_CLEANUP);
        cache.put(1, "one");
        Thread.sleep(QUICK_TTL + QUICK_CLEANUP);
        assertFalse(cache.containsValue("one"));
        assertEquals(0, cache.size());
    }

    /* -------------------------------------------------
     *  LRU eviction (maxSize)
     * ------------------------------------------------- */

    @Test
    public void testLruEvictionWhenMaxSizeExceeded() {
        TTLCache<String, Integer> cache = new TTLCache<>(1000, 2); // maxSize = 2

        cache.put("first", 1);
        cache.put("second", 2);
        // Access "first" to make it MRU
        assertEquals(Integer.valueOf(1), cache.get("first"));

        cache.put("third", 3); // should evict "second" (the LRU)

        assertTrue(cache.containsKey("first"));
        assertTrue(cache.containsKey("third"));
        assertFalse(cache.containsKey("second"));
        assertEquals(2, cache.size());
    }

    @Test
    public void testLruEvictionWithNoAccessLeavesOldest() {
        TTLCache<String, String> cache = new TTLCache<>(1000, 2);
        cache.put("A", "a");
        cache.put("B", "b");
        cache.put("C", "c"); // "A" should be evicted

        assertFalse(cache.containsKey("A"));
        assertTrue(cache.containsKey("B"));
        assertTrue(cache.containsKey("C"));
    }

    /* -------------------------------------------------
     *  Removal and clear
     * ------------------------------------------------- */

    @Test
    public void testRemoveReturnsValueAndUpdatesState() {
        TTLCache<String, String> cache = new TTLCache<>(1000);
        cache.put("k", "v");
        String removed = cache.remove("k");
        assertEquals("v", removed);
        assertNull(cache.get("k"));
        assertEquals(0, cache.size());
        assertFalse(cache.containsKey("k"));
    }

    @Test
    public void testRemoveNonExistingKeyReturnsNull() {
        TTLCache<String, String> cache = new TTLCache<>(1000);
        assertNull(cache.remove("missing"));
    }

    @Test
    public void testClearEmptiesCache() {
        TTLCache<Integer, Integer> cache = new TTLCache<>(1000);
        cache.put(1, 10);
        cache.put(2, 20);
        assertEquals(2, cache.size());

        cache.clear();
        assertEquals(0, cache.size());
        assertTrue(cache.isEmpty());
        assertFalse(cache.containsKey(1));
    }

    /* -------------------------------------------------
     *  EntrySet iterator removal
     * ------------------------------------------------- */

    @Test
    public void testEntrySetIteratorRemoveDeletesEntry() {
        TTLCache<String, Integer> cache = new TTLCache<>(1000);
        cache.put("one", 1);
        cache.put("two", 2);
        cache.put("three", 3);

        Iterator<Map.Entry<String, Integer>> it = cache.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Integer> e = it.next();
            if ("two".equals(e.getKey())) {
                it.remove(); // remove via iterator
            }
        }

        assertEquals(2, cache.size());
        assertFalse(cache.containsKey("two"));
        assertTrue(cache.containsKey("one"));
        assertTrue(cache.containsKey("three"));
    }

    @Test(expected = IllegalStateException.class)
    public void testIteratorRemoveWithoutNextThrows() {
        TTLCache<String, String> cache = new TTLCache<>(1000);
        cache.put("a", "A");
        Iterator<Map.Entry<String, String>> it = cache.entrySet().iterator();
        it.remove(); // illegal, no prior next()
    }

    /* -------------------------------------------------
     *  equals, hashCode and toString
     * ------------------------------------------------- */

    @Test
    public void testEqualsAndHashCodeAgainstStandardMap() {
        TTLCache<String, Integer> cache = new TTLCache<>(1000);
        cache.put("x", 42);
        cache.put("y", 84);

        Map<String, Integer> reference = new HashMap<>();
        reference.put("x", 42);
        reference.put("y", 84);

        assertTrue(cache.equals(reference));
        assertTrue(reference.equals(cache)); // Map's equals is symmetric
        assertEquals(reference.hashCode(), cache.hashCode());
    }

    @Test
    public void testEqualsDetectsDifference() {
        TTLCache<String, Integer> cache = new TTLCache<>(1000);
        cache.put("a", 1);
        cache.put("b", 2);

        Map<String, Integer> other = new HashMap<>();
        other.put("a", 1);
        other.put("b", 3); // different value

        assertFalse(cache.equals(other));
    }

    @Test
    public void testToStringFormatting() {
        TTLCache<String, String> cache = new TTLCache<>(1000);
        cache.put("k1", "v1");
        cache.put("k2", "v2");

        String s = cache.toString();
        // Order is LRU -> MRU; we cannot guarantee order, so just check content.
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        assertTrue(s.contains("k1=v1"));
        assertTrue(s.contains("k2=v2"));
        // The string should contain a comma when more than one entry
        assertTrue(s.contains(","));
    }

    /* -------------------------------------------------
     *  putAll and keySet / values collections
     * ------------------------------------------------- */

    @Test
    public void testPutAllCopiesAllEntries() {
        TTLCache<String, Integer> cache = new TTLCache<>(1000);
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 10);
        source.put("b", 20);
        cache.putAll(source);

        assertEquals(2, cache.size());
        assertEquals(Integer.valueOf(10), cache.get("a"));
        assertEquals(Integer.valueOf(20), cache.get("b"));
    }

    @Test
    public void testKeySetReflectsCurrentKeys() {
        TTLCache<Integer, String> cache = new TTLCache<>(1000);
        cache.put(1, "one");
        cache.put(2, "two");

        Set<Integer> keys = cache.keySet();
        assertEquals(2, keys.size());
        assertTrue(keys.contains(1));
        assertTrue(keys.contains(2));
    }

    @Test
    public void testValuesCollectionReflectsCurrentValues() {
        TTLCache<Integer, String> cache = new TTLCache<>(1000);
        cache.put(1, "one");
        cache.put(2, "two");

        assertTrue(cache.values().contains("one"));
        assertTrue(cache.values().contains("two"));
        assertEquals(2, cache.values().size());
    }
}
