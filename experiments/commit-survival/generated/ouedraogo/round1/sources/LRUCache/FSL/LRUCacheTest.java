package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Collection;

import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link LRUCache}.
 * The tests cover both locking and threaded strategies, all Map‑interface
 * methods delegated by LRUCache, null handling, eviction policy, equals/hashCode,
 * toString and the shutdown hook.
 */
public class LRUCacheTest {

    /* ----------------------------------------------------------------------
     *  Basic operations – LOCKING strategy (default constructor)
     * ---------------------------------------------------------------------- */
    @Test
    public void testLockingBasicOperations() {
        LRUCache<String, Integer> cache = new LRUCache<>(2); // locking strategy by default

        // initially empty
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());

        // put entries
        assertNull(cache.put("one", 1));
        assertNull(cache.put("two", 2));
        assertEquals(2, cache.size());
        assertFalse(cache.isEmpty());

        // get & contains
        assertEquals(Integer.valueOf(1), cache.get("one"));
        assertTrue(cache.containsKey("two"));
        assertTrue(cache.containsValue(2));

        // replace existing key
        assertEquals(Integer.valueOf(1), cache.put("one", 10));
        assertEquals(Integer.valueOf(10), cache.get("one"));
        assertEquals(2, cache.size());

        // remove
        assertEquals(Integer.valueOf(2), cache.remove("two"));
        assertFalse(cache.containsKey("two"));
        assertEquals(1, cache.size());

        // clear
        cache.clear();
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
    }

    @Test
    public void testLockingEvictionOrder() {
        LRUCache<String, String> cache = new LRUCache<>(2); // capacity 2

        cache.put("A", "alpha");
        cache.put("B", "bravo");
        // Access "A" to make it recently used
        assertEquals("alpha", cache.get("A"));
        // Insert third entry – should evict "B" (the least recently used)
        cache.put("C", "charlie");

        assertEquals(2, cache.size());
        assertNull("B should have been evicted", cache.get("B"));
        assertEquals("alpha", cache.get("A"));
        assertEquals("charlie", cache.get("C"));
    }

    /* ----------------------------------------------------------------------
     *  Basic operations – THREADED strategy
     * ---------------------------------------------------------------------- */
    @Test
    public void testThreadedBasicOperations() throws InterruptedException {
        LRUCache<String, Integer> cache = new LRUCache<>(2, StrategyType.THREADED);

        cache.put("one", 1);
        cache.put("two", 2);

        // Verify basic map behaviour
        assertEquals(Integer.valueOf(1), cache.get("one"));
        assertTrue(cache.containsKey("two"));
        assertTrue(cache.containsValue(2));

        // putAll
        Map<String, Integer> extra = new HashMap<>();
        extra.put("three", 3);
        extra.put("four", 4);
        cache.putAll(extra);

        // Because the threaded strategy cleans up asynchronously,
        // give it a little time to trim back to capacity.
        Thread.sleep(50);

        assertEquals(2, cache.size());
        // The two most recent puts were "three" and "four"
        assertNull(cache.get("one"));
        assertNull(cache.get("two"));
        assertEquals(Integer.valueOf(3), cache.get("three"));
        assertEquals(Integer.valueOf(4), cache.get("four"));
    }

    @Test
    public void testThreadedEvictionOrder() throws InterruptedException {
        LRUCache<String, String> cache = new LRUCache<>(2, StrategyType.THREADED);

        cache.put("X", "xray");
        cache.put("Y", "yankee");
        // Access Y to make it most recent
        assertEquals("yankee", cache.get("Y"));
        // Insert third entry – should evict X after cleanup delay
        cache.put("Z", "zulu");

        // Wait for asynchronous cleanup
        Thread.sleep(50);

        assertEquals(2, cache.size());
        assertNull("X should have been evicted", cache.get("X"));
        assertEquals("yankee", cache.get("Y"));
        assertEquals("zulu", cache.get("Z"));
    }

    /* ----------------------------------------------------------------------
     *  Null handling
     * ---------------------------------------------------------------------- */
    @Test
    public void testNullKeyAndValue() {
        LRUCache<String, String> cache = new LRUCache<>(3);
        assertNull(cache.put(null, "nullKey"));
        assertNull(cache.put("nullValue", null));

        assertEquals("nullKey", cache.get(null));
        assertNull(cache.get("nullValue"));
        assertTrue(cache.containsKey(null));
        assertTrue(cache.containsValue(null));

        assertEquals(2, cache.size());
    }

    /* ----------------------------------------------------------------------
     *  Collection view methods
     * ---------------------------------------------------------------------- */
    @Test
    public void testKeySetValuesEntrySet() {
        LRUCache<String, Integer> cache = new LRUCache<>(5);
        cache.put("a", 1);
        cache.put("b", 2);
        cache.put("c", 3);

        Set<String> keys = cache.keySet();
        Collection<Integer> values = cache.values();
        Set<Map.Entry<String, Integer>> entries = cache.entrySet();

        assertEquals(3, keys.size());
        assertTrue(keys.contains("a"));
        assertTrue(keys.contains("b"));
        assertTrue(keys.contains("c"));

        assertEquals(3, values.size());
        assertTrue(values.contains(1));
        assertTrue(values.contains(2));
        assertTrue(values.contains(3));

        assertEquals(3, entries.size());
        for (Map.Entry<String, Integer> e : entries) {
            assertEquals(e.getValue(), cache.get(e.getKey()));
        }
    }

    /* ----------------------------------------------------------------------
     *  equals, hashCode and toString
     * ---------------------------------------------------------------------- */
    @Test
    public void testEqualsAndHashCode() {
        LRUCache<String, String> cache = new LRUCache<>(4);
        cache.put("k1", "v1");
        cache.put("k2", "v2");

        Map<String, String> map = new HashMap<>();
        map.put("k1", "v1");
        map.put("k2", "v2");

        // Equality with a plain Map
        assertTrue(cache.equals(map));
        assertTrue(map.equals(cache));

        // Equality with itself
        assertTrue(cache.equals(cache));

        // Different content
        cache.put("k3", "v3");
        assertFalse(cache.equals(map));

        // hashCode should be consistent with the underlying map
        assertEquals(map.hashCode(), cache.hashCode());
    }

    @Test
    public void testToStringContainsEntries() {
        LRUCache<Integer, String> cache = new LRUCache<>(2);
        cache.put(1, "one");
        cache.put(2, "two");

        String s = cache.toString();
        assertNotNull(s);
        assertTrue(s.contains("1=one"));
        assertTrue(s.contains("2=two"));
    }

    /* ----------------------------------------------------------------------
     *  shutdown for threaded strategy
     * ---------------------------------------------------------------------- */
    @Test
    public void testShutdownDoesNotThrow() {
        LRUCache<Integer, String> cache = new LRUCache<>(2, StrategyType.THREADED);
        cache.put(1, "one");
        // The shutdown method should complete without exception even if the cache is still in use.
        cache.shutdown();
        // After shutdown, the cache should still behave like a normal map.
        cache.put(2, "two");
        assertEquals("two", cache.get(2));
    }

    /* ----------------------------------------------------------------------
     *  Constructor variations – capacity + cleanup delay
     * ---------------------------------------------------------------------- */
    @Test
    public void testConstructorWithCleanupDelay() throws InterruptedException {
        // capacity = 1, cleanup delay = 5 ms
        LRUCache<String, String> cache = new LRUCache<>(1, 5);
        cache.put("first", "1");
        cache.put("second", "2"); // exceeds capacity

        // Immediately after put, size may be 2 (transient state)
        assertTrue(cache.size() >= 1);

        // After the cleanup delay the cache must be trimmed to capacity 1
        Thread.sleep(20);
        assertEquals(1, cache.size());
        // The most recent entry should survive
        assertEquals("2", cache.get("second"));
        assertNull(cache.get("first"));
    }
}
