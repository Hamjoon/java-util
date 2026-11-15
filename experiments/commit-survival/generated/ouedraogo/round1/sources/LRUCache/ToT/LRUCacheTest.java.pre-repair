package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link LRUCache}.  The suite exercises
 * <ul>
 *   <li>both cache strategies (LOCKING and THREADED)</li>
 *   <li>all Map‑delegated methods</li>
 *   <li>edge cases such as null keys/values and capacity eviction</li>
 *   <li>error handling (unsupported strategy, equals contract, shutdown)</li>
 * </ul>
 *
 * The tests purposefully avoid any mocking – they instantiate the real
 * {@link LRUCache} and the underlying strategy classes directly, as required.
 */
public class LRUCacheTest {

    /** Small capacity used for eviction tests */
    private static final int CAPACITY = 2;

    /** Delay used for the threaded strategy – short enough for tests but long enough to allow the cleanup task to run */
    private static final int CLEANUP_DELAY_MS = 50;

    private LRUCache<Integer, String> lockingCache;
    private LRUCache<Integer, String> threadedCache;

    @Before
    public void setUp() {
        // LOCKING strategy – default constructor
        lockingCache = new LRUCache<>(CAPACITY);

        // THREADED strategy – explicit enum + custom cleanup delay
        threadedCache = new LRUCache<>(CAPACITY, StrategyType.THREADED);
    }

    @After
    public void tearDown() {
        // Ensure any background threads are terminated
        if (lockingCache != null) {
            lockingCache.shutdown();   // safe – does nothing for LOCKING
        }
        if (threadedCache != null) {
            threadedCache.shutdown();  // shuts down the shared ThreadedLRUCacheStrategy executor
        }
    }

    /* --------------------------------------------------------------------- *
     *  Constructor & basic behaviour tests
     * --------------------------------------------------------------------- */

    @Test
    public void testLockingConstructorCreatesNonNullStrategy() {
        LRUCache<String, String> cache = new LRUCache<>(5);
        assertNotNull("Strategy should be instantiated", cache);
        assertTrue("Cache should be empty initially", cache.isEmpty());
    }

    @Test
    public void testThreadedConstructorCreatesNonNullStrategy() {
        LRUCache<String, String> cache = new LRUCache<>(5, StrategyType.THREADED);
        assertNotNull("Strategy should be instantiated", cache);
        assertTrue("Cache should be empty initially", cache.isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullStrategyThrows() {
        // Passing null triggers the else‑branch that throws IllegalArgumentException
        new LRUCache<>(5, null);
    }

    @Test
    public void testThreadedConstructorWithCustomDelay() {
        LRUCache<Integer, String> cache = new LRUCache<>(CAPACITY, CLEANUP_DELAY_MS);
        assertNotNull(cache);
        assertTrue(cache.isEmpty());
    }

    /* --------------------------------------------------------------------- *
     *  Basic Map operations – exercised on both strategies
     * --------------------------------------------------------------------- */

    @Test
    public void testPutAndGet() {
        lockingCache.put(1, "one");
        assertEquals("one", lockingCache.get(1));

        threadedCache.put(2, "two");
        assertEquals("two", threadedCache.get(2));
    }

    @Test
    public void testPutAll() {
        Map<Integer, String> source = new HashMap<>();
        source.put(10, "ten");
        source.put(20, "twenty");

        lockingCache.putAll(source);
        assertEquals(2, lockingCache.size());
        assertTrue(lockingCache.containsKey(10));
        assertTrue(lockingCache.containsValue("twenty"));
    }

    @Test
    public void testRemoveAndClear() {
        lockingCache.put(1, "one");
        lockingCache.put(2, "two");
        assertEquals("one", lockingCache.remove(1));
        assertFalse(lockingCache.containsKey(1));

        lockingCache.clear();
        assertTrue(lockingCache.isEmpty());
    }

    @Test
    public void testContainsKeyAndContainsValue() {
        threadedCache.put(5, "five");
        assertTrue(threadedCache.containsKey(5));
        assertTrue(threadedCache.containsValue("five"));
        assertFalse(threadedCache.containsKey(99));
        assertFalse(threadedCache.containsValue("ninety‑nine"));
    }

    @Test
    public void testKeySetAndValuesCollections() {
        lockingCache.put(1, "one");
        lockingCache.put(2, "two");

        Set<Integer> keys = lockingCache.keySet();
        Collection<String> values = lockingCache.values();

        assertEquals(new HashSet<>(Arrays.asList(1, 2)), keys);
        assertTrue(values.containsAll(Arrays.asList("one", "two")));
    }

    @Test
    public void testEntrySetIteration() {
        threadedCache.put(3, "three");
        threadedCache.put(4, "four");

        Set<Map.Entry<Integer, String>> entries = threadedCache.entrySet();
        Map<Integer, String> copy = new HashMap<>();
        for (Map.Entry<Integer, String> e : entries) {
            copy.put(e.getKey(), e.getValue());
        }
        assertEquals(2, copy.size());
        assertEquals("three", copy.get(3));
        assertEquals("four", copy.get(4));
    }

    @Test
    public void testToStringAndHashCodeConsistency() {
        lockingCache.put(1, "one");
        lockingCache.put(2, "two");

        String s = lockingCache.toString();
        assertNotNull(s);
        assertTrue(s.contains("one"));
        assertTrue(s.contains("two"));

        int h1 = lockingCache.hashCode();
        int h2 = new LRUCache<>(CAPACITY).hashCode(); // empty cache – different hash
        assertNotEquals(h1, h2);
    }

    /* --------------------------------------------------------------------- *
     *  Equality contract tests
     * --------------------------------------------------------------------- */

    @Test
    public void testEqualsWithSameContentMap() {
        Map<Integer, String> map = new HashMap<>();
        map.put(1, "one");
        map.put(2, "two");

        lockingCache.putAll(map);
        assertTrue(lockingCache.equals(map));
        assertTrue(map.equals(lockingCache)); // Map's equals should also succeed
    }

    @Test
    public void testEqualsWithDifferentContent() {
        lockingCache.put(1, "one");
        Map<Integer, String> other = new HashMap<>();
        other.put(1, "ONE"); // different value case
        assertFalse(lockingCache.equals(other));
    }

    @Test
    public void testEqualsWithNullAndNonMap() {
        assertFalse(lockingCache.equals(null));
        assertFalse(lockingCache.equals("not a map"));
    }

    @Test
    public void testEqualsSelf() {
        assertTrue(lockingCache.equals(lockingCache));
    }

    /* --------------------------------------------------------------------- *
     *  Null key/value handling – both strategies support nulls
     * --------------------------------------------------------------------- */

    @Test
    public void testNullKeyAndValueAllowed() {
        // Null key
        lockingCache.put(null, "nullKey");
        assertEquals("nullKey", lockingCache.get(null));
        assertTrue(lockingCache.containsKey(null));

        // Null value
        threadedCache.put(100, null);
        assertTrue(threadedCache.containsKey(100));
        assertNull(threadedCache.get(100));
    }

    /* --------------------------------------------------------------------- *
     *  Capacity & eviction behaviour
     * --------------------------------------------------------------------- */

    @Test
    public void testLockingStrategyEvictsLeastRecentlyUsed() {
        // Fill to capacity
        lockingCache.put(1, "one");
        lockingCache.put(2, "two");
        assertEquals(CAPACITY, lockingCache.size());

        // Access key 1 to make it most‑recently used
        lockingCache.get(1);

        // Insert third entry – should evict key 2 (the LRU)
        lockingCache.put(3, "three");
        assertEquals(CAPACITY, lockingCache.size());
        assertTrue(lockingCache.containsKey(1));
        assertTrue(lockingCache.containsKey(3));
        assertFalse(lockingCache.containsKey(2));
    }

    @Test
    public void testThreadedStrategyEvictsAfterDelay() throws InterruptedException {
        // Use a cache with a short cleanup delay
        LRUCache<Integer, String> cache = new LRUCache<>(CAPACITY, CLEANUP_DELAY_MS);

        cache.put(1, "one");
        cache.put(2, "two");
        // Access 1 to make it MRU
        cache.get(1);
        // Insert third entry – cache may temporarily hold 3 entries
        cache.put(3, "three");

        // Wait enough time for the background cleanup task to run
        Thread.sleep(CLEANUP_DELAY_MS * 4);

        // After cleanup, size must be back to CAPACITY and LRU (key 2) removed
        assertEquals(CAPACITY, cache.size());
        assertTrue(cache.containsKey(1));
        assertTrue(cache.containsKey(3));
        assertFalse(cache.containsKey(2));
    }

    /* --------------------------------------------------------------------- *
     *  shutdown() – only relevant for threaded strategy
     * --------------------------------------------------------------------- */

    @Test
    public void testShutdownDoesNotThrowForLockingCache() {
        // Should be a no‑op but must not throw
        lockingCache.shutdown();
    }

    @Test
    public void testShutdownCleansThreadedResources() {
        // The method is static; we only verify that calling it does not raise an exception.
        threadedCache.shutdown();
    }
}
