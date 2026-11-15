package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Collection;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link LRUCache}.
 *
 * The suite is written from the perspective of three independent “experts”.
 * Each expert contributed one test per public method (including constructors)
 * and the final file aggregates all those contributions.
 *
 * The tests cover:
 *   • all constructors and {@code getCapacity()}
 *   • basic Map operations (put, get, remove, contains*, size, isEmpty, clear)
 *   • bulk operations (putAll, keySet, values, entrySet)
 *   • LRU eviction semantics for both LOCKING and THREADED strategies
 *   • handling of {@code null} keys and values
 *   • {@code equals}, {@code hashCode} and {@code toString}
 *   • deprecated {@code shutdown()} (no‑op)
 *
 * No mocking frameworks are used – real {@link LRUCache} instances backed by
 * the concrete strategy implementations are exercised.
 */
public class LRUCacheTest {

    private static final int SMALL_CAPACITY = 2;
    private static final int LARGE_CAPACITY = 5;
    private static final int CLEANUP_DELAY_MS = 5; // small enough for tests but > 0

    private LRUCache<String, Integer> lockingCache;
    private LRUCache<String, Integer> threadedCache;
    private LRUCache<String, Integer> threadedDelayCache;

    @Before
    public void setUp() {
        // LOCKING strategy (default constructor)
        lockingCache = new LRUCache<>(LARGE_CAPACITY);

        // THREADED strategy via enum
        threadedCache = new LRUCache<>(LARGE_CAPACITY, LRUCache.StrategyType.THREADED);

        // THREADED strategy via delay‑based constructor
        threadedDelayCache = new LRUCache<>(SMALL_CAPACITY, CLEANUP_DELAY_MS);
    }

    @After
    public void tearDown() {
        // shutdown is a no‑op but calling it verifies the method exists
        if (lockingCache != null) lockingCache.shutdown();
        if (threadedCache != null) threadedCache.shutdown();
        if (threadedDelayCache != null) threadedDelayCache.shutdown();
    }

    /* ----------------------------------------------------------------------
     * Constructor / getCapacity tests (Expert 1)
     * ---------------------------------------------------------------------- */

    @Test
    public void testLockingConstructorAndCapacity() {
        LRUCache<String, String> cache = new LRUCache<>(3);
        assertEquals("Locking cache should report the capacity supplied to its constructor",
                3, cache.getCapacity());
    }

    @Test
    public void testThreadedConstructorWithEnumAndCapacity() {
        LRUCache<Integer, Integer> cache = new LRUCache<>(4, LRUCache.StrategyType.THREADED);
        assertEquals("Threaded cache (enum ctor) should report its capacity",
                4, cache.getCapacity());
    }

    @Test
    public void testThreadedConstructorWithDelayAndCapacity() {
        int capacity = 7;
        int delay = 12;
        LRUCache<Integer, String> cache = new LRUCache<>(capacity, delay);
        assertEquals("Threaded cache (delay ctor) should report its capacity",
                capacity, cache.getCapacity());
    }

    /* ----------------------------------------------------------------------
     * Basic Map operations (Expert 2)
     * ---------------------------------------------------------------------- */

    @Test
    public void testPutAndGetBasic() {
        lockingCache.put("one", 1);
        assertEquals(Integer.valueOf(1), lockingCache.get("one"));
    }

    @Test
    public void testPutNullKeyAndValue() {
        // Both LOCKING and THREADED strategies support nulls
        threadedCache.put(null, null);
        assertTrue("Cache should contain null key", threadedCache.containsKey(null));
        assertTrue("Cache should contain null value", threadedCache.containsValue(null));
        assertNull("Getting null key should return null value", threadedCache.get(null));
    }

    @Test
    public void testRemoveExistingAndNonExisting() {
        lockingCache.put("a", 10);
        assertEquals(Integer.valueOf(10), lockingCache.remove("a"));
        assertNull("Removing a non‑existent key should return null", lockingCache.remove("missing"));
    }

    @Test
    public void testContainsKeyAndValue() {
        threadedCache.put("x", 100);
        assertTrue(threadedCache.containsKey("x"));
        assertTrue(threadedCache.containsValue(100));
        assertFalse(threadedCache.containsKey("y"));
        assertFalse(threadedCache.containsValue(200));
    }

    @Test
    public void testClearAndIsEmpty() {
        lockingCache.put("k1", 1);
        lockingCache.put("k2", 2);
        assertFalse(lockingCache.isEmpty());
        lockingCache.clear();
        assertTrue(lockingCache.isEmpty());
        assertEquals(0, lockingCache.size());
    }

    @Test
    public void testPutAll() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);
        source.put("c", 3);
        lockingCache.putAll(source);
        assertEquals(3, lockingCache.size());
        assertTrue(lockingCache.keySet().containsAll(source.keySet()));
        assertTrue(lockingCache.values().containsAll(source.values()));
    }

    @Test
    public void testKeySetValuesEntrySetReflectChanges() {
        lockingCache.put("k1", 10);
        lockingCache.put("k2", 20);

        Set<String> keys = lockingCache.keySet();
        Collection<Integer> values = lockingCache.values();
        Set<Map.Entry<String, Integer>> entries = lockingCache.entrySet();

        assertEquals(2, keys.size());
        assertTrue(keys.contains("k1"));
        assertTrue(keys.contains("k2"));

        assertEquals(2, values.size());
        assertTrue(values.contains(10));
        assertTrue(values.contains(20));

        assertEquals(2, entries.size());
        for (Map.Entry<String, Integer> e : entries) {
            if (e.getKey().equals("k1")) {
                assertEquals(Integer.valueOf(10), e.getValue());
            } else if (e.getKey().equals("k2")) {
                assertEquals(Integer.valueOf(20), e.getValue());
            } else {
                fail("Unexpected entry: " + e);
            }
        }
    }

    /* ----------------------------------------------------------------------
     * LRU eviction semantics (Expert 3)
     * ---------------------------------------------------------------------- */

    @Test
    public void testEvictionLockingStrategy() {
        LRUCache<Integer, String> cache = new LRUCache<>(SMALL_CAPACITY); // LOCKING by default
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three"); // should evict key 1 (LRU)

        assertEquals("Cache size should never exceed capacity", SMALL_CAPACITY, cache.size());
        assertFalse("Key 1 should have been evicted", cache.containsKey(1));
        assertTrue("Key 2 should still be present", cache.containsKey(2));
        assertTrue("Key 3 should still be present", cache.containsKey(3));
    }

    @Test
    public void testEvictionThreadedStrategyImmediateAfterDelay() throws InterruptedException {
        LRUCache<Integer, String> cache = new LRUCache<>(SMALL_CAPACITY, LRUCache.StrategyType.THREADED);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three"); // now 3 entries, eviction runs asynchronously

        // Wait a bit longer than the default cleanup delay (10 ms) used by the enum ctor
        Thread.sleep(30);

        assertEquals("After cleanup the size must be <= capacity", SMALL_CAPACITY, cache.size());
        assertFalse("Least‑recently‑used key (1) should have been evicted", cache.containsKey(1));
        assertTrue("Key 2 should still be present", cache.containsKey(2));
        assertTrue("Key 3 should still be present", cache.containsKey(3));
    }

    @Test
    public void testEvictionThreadedStrategyWithCustomDelay() throws InterruptedException {
        LRUCache<Integer, String> cache = new LRUCache<>(SMALL_CAPACITY, CLEANUP_DELAY_MS);
        cache.put(10, "ten");
        cache.put(20, "twenty");
        cache.put(30, "thirty"); // exceed capacity

        // Wait for the custom cleanup delay to fire
        Thread.sleep(CLEANUP_DELAY_MS * 5);

        assertEquals("Cache should be trimmed back to its capacity", SMALL_CAPACITY, cache.size());
        assertFalse("Key 10 (LRU) should be evicted", cache.containsKey(10));
        assertTrue(cache.containsKey(20));
        assertTrue(cache.containsKey(30));
    }

    /* ----------------------------------------------------------------------
     * Equality, hashCode and toString (Expert 1 again)
     * ---------------------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCodeWithSameContent() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        LRUCache<String, Integer> cache = new LRUCache<>(LARGE_CAPACITY);
        cache.putAll(map);

        // Equality with another map having the same entries
        assertTrue("Cache should be equal to a map with identical entries", cache.equals(map));
        assertEquals("Hash codes must be equal when contents are equal", map.hashCode(), cache.hashCode());

        // Equality with another LRUCache instance
        LRUCache<String, Integer> otherCache = new LRUCache<>(LARGE_CAPACITY);
        otherCache.putAll(map);
        assertTrue("Two caches with same entries should be equal", cache.equals(otherCache));
        assertEquals(cache.hashCode(), otherCache.hashCode());
    }

    @Test
    public void testEqualsWithDifferentContentAndNonMap() {
        LRUCache<String, Integer> cache = new LRUCache<>(LARGE_CAPACITY);
        cache.put("x", 9);
        cache.put("y", 10);

        Map<String, Integer> differentMap = Collections.singletonMap("x", 9);
        assertFalse("Cache should not be equal to a map with different entries", cache.equals(differentMap));

        assertFalse("Cache should not be equal to null", cache.equals(null));
        assertFalse("Cache should not be equal to an unrelated object", cache.equals("some string"));
    }

    @Test
    public void testToStringContainsEntries() {
        lockingCache.put("alpha", 100);
        lockingCache.put("beta", 200);
        String s = lockingCache.toString();

        assertNotNull("toString should never return null", s);
        assertTrue("String representation should contain key 'alpha'", s.contains("alpha"));
        assertTrue("String representation should contain value 100", s.contains("100"));
        assertTrue("String representation should contain key 'beta'", s.contains("beta"));
        assertTrue("String representation should contain value 200", s.contains("200"));
    }

    /* ----------------------------------------------------------------------
     * Deprecated shutdown (Expert 2)
     * ---------------------------------------------------------------------- */

    @Test
    @SuppressWarnings("deprecation")
    public void testShutdownIsNoOp() {
        // The method is deprecated and does nothing; we just verify it can be called without exception.
        LRUCache<String, String> cache = new LRUCache<>(2);
        cache.shutdown(); // should not throw
    }
}
