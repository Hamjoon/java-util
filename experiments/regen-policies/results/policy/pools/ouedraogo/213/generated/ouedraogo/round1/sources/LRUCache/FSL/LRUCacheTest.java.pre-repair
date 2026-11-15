package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Collection;

import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link LRUCache}.
 * <p>
 * The tests cover:
 * <ul>
 *   <li>Construction with each strategy (LOCKING, THREADED, default locking)</li>
 *   <li>Basic {@link Map} operations (put, get, remove, containsKey/Value, clear, size, isEmpty)</li>
 *   <li>LRU eviction semantics for both strategies</li>
 *   <li>Support for {@code null} keys and values</li>
 *   <li>putAll, keySet, values, entrySet view collections</li>
 *   <li>equals, hashCode and toString delegations</li>
 *   <li>Deprecated {@code shutdown()} method (no‑op)</li>
 *   <li>IllegalArgumentException when an unsupported {@link LRUCache.StrategyType} is supplied</li>
 * </ul>
 */
public class LRUCacheTest {

    /** Simple helper to pause the current thread – used for the threaded strategy where eviction is asynchronous. */
    private void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    /* -------------------------------------------------------------
     *  Construction / Capacity tests
     * ------------------------------------------------------------- */

    @Test
    public void testLockingConstructorAndCapacityEviction() {
        LRUCache<String, Integer> cache = new LRUCache<>(2); // default locking strategy
        assertEquals(2, cache.getCapacity());

        // Insert two entries – cache should hold both
        cache.put("A", 1);
        cache.put("B", 2);
        assertEquals(2, cache.size());
        assertTrue(cache.containsKey("A"));
        assertTrue(cache.containsKey("B"));

        // Access "A" to make it most‑recently used
        assertEquals(Integer.valueOf(1), cache.get("A"));

        // Insert a third entry – the LRU entry ("B") must be evicted
        cache.put("C", 3);
        assertEquals(2, cache.size());
        assertTrue(cache.containsKey("A"));
        assertTrue(cache.containsKey("C"));
        assertFalse(cache.containsKey("B"));
    }

    @Test
    public void testThreadedConstructorAndCapacityEviction() {
        // Use the explicit threaded constructor with a very short cleanup delay
        LRUCache<String, Integer> cache = new LRUCache<>(2, LRUCache.StrategyType.THREADED);
        assertEquals(2, cache.getCapacity());

        cache.put("X", 10);
        cache.put("Y", 20);
        assertEquals(2, cache.size());

        // Access "X" to make it recent
        assertEquals(Integer.valueOf(10), cache.get("X"));

        // Adding a third entry should trigger asynchronous cleanup
        cache.put("Z", 30);
        // Wait a little longer than the default cleanup delay (10 ms) to let the daemon thread evict
        pause(50);

        // After eviction, the LRU entry ("Y") must be gone
        assertEquals(2, cache.size());
        assertTrue(cache.containsKey("X"));
        assertTrue(cache.containsKey("Z"));
        assertFalse(cache.containsKey("Y"));
    }

    @Test
    public void testThreadedConstructorWithCustomDelay() {
        // Custom delay = 1 ms – ensures eviction happens quickly
        LRUCache<String, String> cache = new LRUCache<>(1, 1);
        cache.put("first", "1");
        cache.put("second", "2");
        pause(10); // give the cleanup thread time to run

        assertEquals(1, cache.size());
        assertFalse(cache.containsKey("first"));
        assertTrue(cache.containsKey("second"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnsupportedStrategyThrowsException() {
        // Force a null strategy (not THREADED nor LOCKING) – should raise IllegalArgumentException
        new LRUCache<>(5, (LRUCache.StrategyType) null);
    }

    /* -------------------------------------------------------------
     *  Basic Map operations
     * ------------------------------------------------------------- */

    @Test
    public void testBasicMapOperations() {
        LRUCache<String, String> cache = new LRUCache<>(5, LRUCache.StrategyType.LOCKING);

        // put & get
        assertNull(cache.put("k1", "v1"));
        assertEquals("v1", cache.get("k1"));

        // overwrite existing key
        assertEquals("v1", cache.put("k1", "v1b"));
        assertEquals("v1b", cache.get("k1"));

        // remove
        assertEquals("v1b", cache.remove("k1"));
        assertNull(cache.get("k1"));
        assertFalse(cache.containsKey("k1"));

        // clear & isEmpty
        cache.put("a", "a");
        cache.put("b", "b");
        cache.clear();
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
    }

    @Test
    public void testNullKeyAndValueSupport() {
        LRUCache<String, String> cache = new LRUCache<>(3, LRUCache.StrategyType.LOCKING);

        // null key
        cache.put(null, "nullKey");
        assertTrue(cache.containsKey(null));
        assertEquals("nullKey", cache.get(null));

        // null value
        cache.put("nullValue", null);
        assertTrue(cache.containsKey("nullValue"));
        assertNull(cache.get("nullValue"));

        // both null
        cache.put(null, null);
        assertTrue(cache.containsKey(null));
        assertNull(cache.get(null));
    }

    @Test
    public void testPutAllAndViews() {
        LRUCache<Integer, String> cache = new LRUCache<>(10, LRUCache.StrategyType.LOCKING);
        Map<Integer, String> source = new HashMap<>();
        source.put(1, "one");
        source.put(2, "two");
        source.put(3, "three");

        cache.putAll(source);
        assertEquals(3, cache.size());
        assertTrue(cache.keySet().containsAll(source.keySet()));
        assertTrue(cache.values().containsAll(source.values()));
        assertTrue(cache.entrySet().containsAll(source.entrySet()));
    }

    /* -------------------------------------------------------------
     *  equals, hashCode, toString
     * ------------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCodeDelegation() {
        LRUCache<String, Integer> cache = new LRUCache<>(4, LRUCache.StrategyType.LOCKING);
        cache.put("x", 100);
        cache.put("y", 200);

        Map<String, Integer> reference = new HashMap<>();
        reference.put("x", 100);
        reference.put("y", 200);

        assertEquals(reference, cache);                 // uses LRUCache.equals()
        assertEquals(cache, reference);                 // symmetric
        assertEquals(reference.hashCode(), cache.hashCode());
    }

    @Test
    public void testToStringContainsEntries() {
        LRUCache<String, String> cache = new LRUCache<>(2, LRUCache.StrategyType.LOCKING);
        cache.put("alpha", "A");
        cache.put("beta", "B");
        String s = cache.toString();

        assertTrue(s.contains("alpha"));
        assertTrue(s.contains("beta"));
        assertTrue(s.contains("A"));
        assertTrue(s.contains("B"));
    }

    @Test
    public void testDeprecatedShutdownDoesNothing() {
        LRUCache<String, String> cache = new LRUCache<>(2, LRUCache.StrategyType.LOCKING);
        // The method is deprecated and a no‑op; simply ensure it can be called without exception.
        cache.shutdown();
    }

    /* -------------------------------------------------------------
     *  Additional edge cases
     * ------------------------------------------------------------- */

    @Test
    public void testContainsValueWithNulls() {
        LRUCache<String, String> cache = new LRUCache<>(2, LRUCache.StrategyType.LOCKING);
        cache.put("k", null);
        assertTrue(cache.containsValue(null));
        assertFalse(cache.containsValue("notPresent"));
    }

    @Test
    public void testIsEmptyAfterClear() {
        LRUCache<Integer, Integer> cache = new LRUCache<>(3, LRUCache.StrategyType.THREADED);
        cache.put(1, 1);
        cache.put(2, 2);
        assertFalse(cache.isEmpty());
        cache.clear();
        assertTrue(cache.isEmpty());
    }

    @Test
    public void testEntrySetModificationReflectsCache() {
        LRUCache<String, Integer> cache = new LRUCache<>(5, LRUCache.StrategyType.LOCKING);
        cache.put("one", 1);
        cache.put("two", 2);

        Set<Map.Entry<String, Integer>> entries = cache.entrySet();
        // Remove via iterator
        entries.iterator().remove(); // removes an arbitrary entry
        assertEquals(1, cache.size());
        // Remaining entry should still be accessible
        for (Map.Entry<String, Integer> e : entries) {
            assertEquals(cache.get(e.getKey()), e.getValue());
        }
    }
}
