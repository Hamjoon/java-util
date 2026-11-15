package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link LRUCache}.
 *
 * <p>This file is generated automatically from the source of {@code LRUCache}.
 * It contains:</p>
 * <ol>
 *   <li>A list of all public methods (including constructors) with their signatures.</li>
 *   <li>Basic sanity tests that verify the normal behaviour of each method.</li>
 *   <li>Edge‑case / exception tests that cover null handling, capacity limits,
 *       strategy selection errors and the {@code shutdown} behaviour.</li>
 * </ol>
 *
 * <p>All tests use only the public API that is visible in the supplied source
 * (no mocking, no subclassing of internal classes).</p>
 */
public class LRUCacheTest {

    /* --------------------------------------------------------------------- */
    /* 1.  PUBLIC API EXTRACTION                                               */
    /* --------------------------------------------------------------------- */
    // Constructors
    //   LRUCache(int capacity)
    //   LRUCache(int capacity, StrategyType strategyType)
    //   LRUCache(int capacity, int cleanupDelayMillis)
    //
    // Map methods (delegated to the internal strategy)
    //   V get(Object key)
    //   V put(K key, V value)
    //   void putAll(Map<? extends K, ? extends V> m)
    //   V remove(Object key)
    //   void clear()
    //   int size()
    //   boolean isEmpty()
    //   boolean containsKey(Object key)
    //   boolean containsValue(Object value)
    //   Set<Map.Entry<K,V>> entrySet()
    //   Set<K> keySet()
    //   Collection<V> values()
    //   String toString()
    //   int hashCode()
    //   boolean equals(Object obj)
    //
    // LRUCache specific
    //   void shutdown()

    /* --------------------------------------------------------------------- */
    /* 2.  TEST FIXTURE                                                       */
    /* --------------------------------------------------------------------- */
    private LRUCache<String, Integer> lockingCache;
    private LRUCache<String, Integer> threadedCache;

    @Before
    public void setUp() {
        // default capacity 3 is enough for all basic tests
        lockingCache = new LRUCache<>(3);                         // uses LOCKING strategy
        threadedCache = new LRUCache<>(3, LRUCache.StrategyType.THREADED);
    }

    @After
    public void tearDown() {
        // shutdown only affects threaded strategy; safe to call on both
        if (lockingCache != null) lockingCache.shutdown();
        if (threadedCache != null) threadedCache.shutdown();
    }

    /* --------------------------------------------------------------------- */
    /* 3.  BASIC FUNCTIONALITY TESTS                                          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testPutAndGet_Locking() {
        assertNull(lockingCache.put("a", 1));
        assertEquals(Integer.valueOf(1), lockingCache.get("a"));
    }

    @Test
    public void testPutAndGet_Threaded() {
        assertNull(threadedCache.put("x", 42));
        assertEquals(Integer.valueOf(42), threadedCache.get("x"));
    }

    @Test
    public void testOverwriteValue() {
        lockingCache.put("key", 10);
        Integer previous = lockingCache.put("key", 20);
        assertEquals(Integer.valueOf(10), previous);
        assertEquals(Integer.valueOf(20), lockingCache.get("key"));
    }

    @Test
    public void testRemove() {
        lockingCache.put("removeMe", 5);
        Integer removed = lockingCache.remove("removeMe");
        assertEquals(Integer.valueOf(5), removed);
        assertNull(lockingCache.get("removeMe"));
        assertTrue(lockingCache.isEmpty());
    }

    @Test
    public void testClear() {
        threadedCache.put("a", 1);
        threadedCache.put("b", 2);
        threadedCache.clear();
        assertTrue(threadedCache.isEmpty());
        assertEquals(0, threadedCache.size());
    }

    @Test
    public void testSizeAndIsEmpty() {
        assertTrue(lockingCache.isEmpty());
        assertEquals(0, lockingCache.size());

        lockingCache.put("one", 1);
        assertFalse(lockingCache.isEmpty());
        assertEquals(1, lockingCache.size());
    }

    @Test
    public void testContainsKeyAndContainsValue() {
        lockingCache.put("k1", 100);
        assertTrue(lockingCache.containsKey("k1"));
        assertTrue(lockingCache.containsValue(100));

        assertFalse(lockingCache.containsKey("missing"));
        assertFalse(lockingCache.containsValue(999));
    }

    @Test
    public void testPutAll() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        lockingCache.putAll(map);
        assertEquals(3, lockingCache.size());
        assertTrue(lockingCache.containsKey("b"));
        assertEquals(Integer.valueOf(2), lockingCache.get("b"));
    }

    @Test
    public void testKeySetAndValuesAndEntrySet() {
        lockingCache.put("k1", 10);
        lockingCache.put("k2", 20);

        Set<String> keys = lockingCache.keySet();
        Collection<Integer> values = lockingCache.values();
        Set<Map.Entry<String, Integer>> entries = lockingCache.entrySet();

        assertEquals(new HashSet<>(Arrays.asList("k1", "k2")), keys);
        assertEquals(new HashSet<>(Arrays.asList(10, 20)), new HashSet<>(values));

        Map<String, Integer> reconstructed = new HashMap<>();
        for (Map.Entry<String, Integer> e : entries) {
            reconstructed.put(e.getKey(), e.getValue());
        }
        assertEquals(2, reconstructed.size());
        assertEquals(Integer.valueOf(10), reconstructed.get("k1"));
    }

    @Test
    public void testToStringAndHashCodeConsistency() {
        lockingCache.put("a", 1);
        String s = lockingCache.toString();
        assertTrue(s.contains("a"));
        assertTrue(s.contains("1"));

        int hash = lockingCache.hashCode();
        // Hash code must be the same as the underlying map's hash code
        assertEquals(lockingCache.entrySet().hashCode(), hash);
    }

    @Test
    public void testEqualsWithAnotherMap() {
        Map<String, Integer> other = new HashMap<>();
        other.put("x", 9);
        other.put("y", 8);

        lockingCache.putAll(other);
        assertTrue(lockingCache.equals(other));
        assertTrue(other.equals(lockingCache));
    }

    @Test
    public void testEqualsWithSelfAndDifferentType() {
        assertTrue(lockingCache.equals(lockingCache));
        assertFalse(lockingCache.equals("not a map"));
        assertFalse(lockingCache.equals(null));
    }

    /* --------------------------------------------------------------------- */
    /* 4.  EDGE CASES & EXCEPTION TESTS                                       */
    /* --------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithUnsupportedStrategy() {
        // The enum only defines THREADED and LOCKING, but we pass null to force the else branch.
        new LRUCache<>(5, null);
    }

    @Test
    public void testNullKeyAndNullValue() {
        // Both strategies support null keys and values.
        lockingCache.put(null, null);
        assertTrue(lockingCache.containsKey(null));
        assertTrue(lockingCache.containsValue(null));
        assertNull(lockingCache.get(null));

        // Overwrite null key with a real value
        lockingCache.put(null, 123);
        assertEquals(Integer.valueOf(123), lockingCache.get(null));
    }

    @Test
    public void testCapacityEvictionLockingStrategy() {
        // Locking strategy evicts synchronously on put().
        LRUCache<Integer, String> cache = new LRUCache<>(2); // capacity 2
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three"); // should evict least-recently used (key 1)

        assertFalse(cache.containsKey(1));
        assertTrue(cache.containsKey(2));
        assertTrue(cache.containsKey(3));
        assertEquals(2, cache.size());
    }

    @Test
    public void testCapacityEvictionThreadedStrategy() throws InterruptedException {
        // Threaded strategy evicts asynchronously; we give it a little time.
        LRUCache<Integer, String> cache = new LRUCache<>(2, LRUCache.StrategyType.THREADED);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three"); // now size may be 3 temporarily

        // Wait a bit longer than the default cleanup delay (10 ms) to let the background thread run.
        Thread.sleep(50);

        assertFalse(cache.containsKey(1)); // 1 should have been evicted
        assertTrue(cache.containsKey(2));
        assertTrue(cache.containsKey(3));
        assertEquals(2, cache.size());
        cache.shutdown(); // clean shutdown for the test
    }

    @Test
    public void testShutdownOnLockingStrategyDoesNothing() {
        // No exception should be thrown; shutdown is a no‑op for locking.
        lockingCache.shutdown();
        // Verify cache still works after shutdown
        lockingCache.put("afterShutdown", 777);
        assertEquals(Integer.valueOf(777), lockingCache.get("afterShutdown"));
    }

    @Test
    public void testShutdownOnThreadedStrategyCleansUpResources() {
        // We cannot directly verify thread pool termination without reflection,
        // but we can ensure that calling shutdown does not break subsequent calls.
        threadedCache.shutdown();
        threadedCache.put("new", 1);
        assertEquals(Integer.valueOf(1), threadedCache.get("new"));
    }

    @Test
    public void testPutAllWithNullMapThrowsNPE() {
        try {
            lockingCache.putAll(null);
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
            // expected behaviour – Map.putAll must reject null argument
        }
    }

    @Test
    public void testGetOnMissingKeyReturnsNull() {
        assertNull(lockingCache.get("nonexistent"));
    }

    @Test
    public void testRemoveMissingKeyReturnsNull() {
        assertNull(threadedCache.remove("absent"));
    }

    @Test
    public void testRepeatedPutSameKeyUpdatesLRUOrderLocking() {
        // Verify that a later put on an existing key makes it most‑recently used.
        LRUCache<Integer, String> cache = new LRUCache<>(2);
        cache.put(1, "A");
        cache.put(2, "B");
        // Access key 1 to make it MRU
        cache.get(1);
        // Insert new key, should evict key 2 (LRU)
        cache.put(3, "C");
        assertTrue(cache.containsKey(1));
        assertFalse(cache.containsKey(2));
        assertTrue(cache.containsKey(3));
    }

    /* --------------------------------------------------------------------- */
    /* 5.  COMPLETION                                                          */
    /* --------------------------------------------------------------------- */
}
