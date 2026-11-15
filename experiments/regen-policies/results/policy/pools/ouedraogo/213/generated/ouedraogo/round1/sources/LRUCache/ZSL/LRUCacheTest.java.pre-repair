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
 * <p>This test class exercises every public method of {@code LRUCache}
 * using both underlying strategies (LOCKING and THREADED).  The tests
 * avoid any mocking – they instantiate the real cache implementations
 * exactly as the production code does.</p>
 *
 * <p>Because the {@code ThreadedLRUCacheStrategy} evicts entries on a
 * background daemon thread, tests that verify eviction for that
 * strategy include a short {@code Thread.sleep} to give the cleanup
 * task a chance to run.</p>
 */
public class LRUCacheTest {

    private static final int SMALL_CAPACITY = 2;
    private static final int CLEANUP_DELAY = 50; // ms – matches the constructor used for threaded tests

    private LRUCache<String, Integer> lockingCache;
    private LRUCache<String, Integer> threadedCache;

    @Before
    public void setUp() {
        // Default constructor -> locking strategy
        lockingCache = new LRUCache<>(SMALL_CAPACITY);

        // Explicitly request the threaded strategy
        threadedCache = new LRUCache<>(SMALL_CAPACITY, LRUCache.StrategyType.THREADED);
    }

    @After
    public void tearDown() {
        // The threaded cache uses daemon threads, but we call shutdown for completeness.
        // No operation – method is deprecated but kept for API compatibility.
        if (lockingCache != null) {
            lockingCache.shutdown();
        }
        if (threadedCache != null) {
            threadedCache.shutdown();
        }
    }

    /* ----------------------------------------------------------------------
     *  Basic Map contract tests – same for both strategies
     * ---------------------------------------------------------------------- */

    @Test
    public void testLockingBasicOperations() {
        basicOperations(lockingCache);
    }

    @Test
    public void testThreadedBasicOperations() {
        basicOperations(threadedCache);
    }

    private void basicOperations(LRUCache<String, Integer> cache) {
        // initially empty
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());

        // put & get
        assertNull(cache.put("one", 1));
        assertEquals(Integer.valueOf(1), cache.get("one"));
        assertEquals(1, cache.size());

        // overwrite existing key
        assertEquals(Integer.valueOf(1), cache.put("one", 11));
        assertEquals(Integer.valueOf(11), cache.get("one"));
        assertEquals(1, cache.size());

        // containsKey / containsValue
        assertTrue(cache.containsKey("one"));
        assertTrue(cache.containsValue(11));
        assertFalse(cache.containsKey("two"));
        assertFalse(cache.containsValue(2));

        // remove
        assertEquals(Integer.valueOf(11), cache.remove("one"));
        assertNull(cache.get("one"));
        assertTrue(cache.isEmpty());

        // clear on non‑empty cache
        cache.put("a", 100);
        cache.put("b", 200);
        assertEquals(2, cache.size());
        cache.clear();
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());

        // capacity getter
        assertEquals(SMALL_CAPACITY, cache.getCapacity());
    }

    /* ----------------------------------------------------------------------
     *  Eviction behavior – LOCKING strategy (immediate)
     * ---------------------------------------------------------------------- */

    @Test
    public void testLockingEvictionIsImmediate() {
        // Capacity = 2, add three entries
        lockingCache.put("first", 1);
        lockingCache.put("second", 2);
        // At this point cache is full
        assertEquals(2, lockingCache.size());
        assertTrue(lockingCache.containsKey("first"));
        assertTrue(lockingCache.containsKey("second"));

        // Adding third entry should evict the *least recently used* entry.
        // Since we have not accessed any entry after insertion, "first" is LRU.
        lockingCache.put("third", 3);

        assertEquals(2, lockingCache.size());
        assertFalse("LRU entry 'first' should have been evicted", lockingCache.containsKey("first"));
        assertTrue(lockingCache.containsKey("second"));
        assertTrue(lockingCache.containsKey("third"));

        // Access "second" to make it most‑recent, then add another entry.
        assertEquals(Integer.valueOf(2), lockingCache.get("second"));
        lockingCache.put("fourth", 4);

        // Now "third" should be evicted because it became LRU after the get().
        assertFalse(lockingCache.containsKey("third"));
        assertTrue(lockingCache.containsKey("second"));
        assertTrue(lockingCache.containsKey("fourth"));
    }

    /* ----------------------------------------------------------------------
     *  Eviction behavior – THREADED strategy (delayed)
     * ---------------------------------------------------------------------- */

    @Test
    public void testThreadedEvictionOccursAfterDelay() throws Exception {
        // Build a cache with a short cleanup delay
        LRUCache<String, Integer> cache = new LRUCache<>(SMALL_CAPACITY, CLEANUP_DELAY);
        cache.put("A", 10);
        cache.put("B", 20);
        assertEquals(2, cache.size());

        // Insert third entry – cache may temporarily hold 3 entries
        cache.put("C", 30);
        assertEquals(3, cache.size()); // immediate size may be > capacity

        // Wait longer than the cleanup delay to allow background eviction
        Thread.sleep(CLEANUP_DELAY * 3);

        // After cleanup, size must be back to capacity and the LRU entry removed.
        assertEquals(2, cache.size());
        // "A" was the LRU (never accessed) so it should be gone.
        assertFalse(cache.containsKey("A"));
        assertTrue(cache.containsKey("B"));
        assertTrue(cache.containsKey("C"));
    }

    /* ----------------------------------------------------------------------
     *  putAll, keySet, values, entrySet
     * ---------------------------------------------------------------------- */

    @Test
    public void testPutAllAndCollectionViewsLocking() {
        collectionViewsTest(lockingCache);
    }

    @Test
    public void testPutAllAndCollectionViewsThreaded() {
        collectionViewsTest(threadedCache);
    }

    private void collectionViewsTest(LRUCache<String, Integer> cache) {
        Map<String, Integer> source = new HashMap<>();
        source.put("x", 100);
        source.put("y", 200);
        source.put("z", 300);

        cache.putAll(source);

        // Because capacity is 2, only the two most‑recently added keys should remain.
        // The ordering of putAll is unspecified, but the implementation will retain the
        // last two entries inserted (by iteration order of the source map).  Using a
        // LinkedHashMap guarantees order, but here we accept any two of the three.
        assertEquals(2, cache.size());

        Set<String> keys = cache.keySet();
        Collection<Integer> values = cache.values();
        Set<Map.Entry<String, Integer>> entries = cache.entrySet();

        // Every key in the cache must have a corresponding value and entry.
        assertEquals(keys.size(), values.size());
        assertEquals(keys.size(), entries.size());

        for (String k : keys) {
            assertTrue(values.contains(cache.get(k)));
            boolean found = false;
            for (Map.Entry<String, Integer> e : entries) {
                if (e.getKey().equals(k) && e.getValue().equals(cache.get(k))) {
                    found = true;
                    break;
                }
            }
            assertTrue("Entry for key '" + k + "' not found in entrySet()", found);
        }
    }

    /* ----------------------------------------------------------------------
     *  Null handling
     * ---------------------------------------------------------------------- */

    @Test
    public void testNullKeyAndValueLocking() {
        nullKeyAndValueTest(lockingCache);
    }

    @Test
    public void testNullKeyAndValueThreaded() {
        nullKeyAndValueTest(threadedCache);
    }

    private void nullKeyAndValueTest(LRUCache<String, Integer> cache) {
        // null key with non‑null value
        assertNull(cache.put(null, 42));
        assertTrue(cache.containsKey(null));
        assertEquals(Integer.valueOf(42), cache.get(null));

        // non‑null key with null value
        assertNull(cache.put("nullValue", null));
        assertTrue(cache.containsKey("nullValue"));
        assertNull(cache.get("nullValue"));

        // Overwrite null key with a different value
        assertEquals(Integer.valueOf(42), cache.put(null, 99));
        assertEquals(Integer.valueOf(99), cache.get(null));

        // Cleanup
        cache.clear();
        assertTrue(cache.isEmpty());
    }

    /* ----------------------------------------------------------------------
     *  equals, hashCode, toString
     * ---------------------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCodeLocking() {
        equalsAndHashCodeTest(lockingCache);
    }

    @Test
    public void testEqualsAndHashCodeThreaded() {
        equalsAndHashCodeTest(threadedCache);
    }

    private void equalsAndHashCodeTest(LRUCache<String, Integer> cache) {
        cache.put("a", 1);
        cache.put("b", 2);

        // Build a plain HashMap with the same entries
        Map<String, Integer> same = new HashMap<>();
        same.put("a", 1);
        same.put("b", 2);

        // Equality against another Map implementation
        assertTrue(cache.equals(same));
        assertTrue(same.equals(cache)); // HashMap's equals uses Map contract

        // Hash codes must match according to Map contract
        assertEquals(same.hashCode(), cache.hashCode());

        // Different map – should not be equal
        Map<String, Integer> different = Collections.singletonMap("a", 99);
        assertFalse(cache.equals(different));

        // Equality with self
        assertTrue(cache.equals(cache));

        // toString contains the entries (order not guaranteed)
        String str = cache.toString();
        assertNotNull(str);
        assertTrue(str.contains("a=1"));
        assertTrue(str.contains("b=2"));
    }

    /* ----------------------------------------------------------------------
     *  getCapacity sanity checks
     * ---------------------------------------------------------------------- */

    @Test
    public void testGetCapacityValues() {
        assertEquals(SMALL_CAPACITY, lockingCache.getCapacity());

        LRUCache<String, Integer> custom = new LRUCache<>(10, LRUCache.StrategyType.THREADED);
        assertEquals(10, custom.getCapacity());
    }

    /* ----------------------------------------------------------------------
     *  Defensive checks – constructor arguments
     * ---------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testUnsupportedStrategyThrows() {
        // Force an illegal enum value via reflection (simulated by null)
        new LRUCache<>(5, null);
    }
}
