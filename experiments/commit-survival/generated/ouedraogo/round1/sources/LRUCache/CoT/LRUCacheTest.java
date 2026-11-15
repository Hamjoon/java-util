package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link LRUCache}.
 *
 * The suite is organised as follows:
 * <ol>
 *   <li>List of public methods (including signatures) extracted from the source.</li>
 *   <li>Basic functional tests for each public method.</li>
 *   <li>Edge‑case / exception tests identified from the implementation.</li>
 *   <li>All tests merged into a single class {@code LRUCacheTest}.</li>
 * </ol>
 *
 * The tests use only the public API that is visible in the supplied source code and
 * the declared signatures of the strategy classes. No mocking frameworks are used.
 */
public class LRUCacheTest {

    /* ----------------------------------------------------------------------
     * 1. Public methods of LRUCache (extracted from the source)
     * ---------------------------------------------------------------------- */
    // Constructors
    //   LRUCache(int capacity)
    //   LRUCache(int capacity, StrategyType strategyType)
    //   LRUCache(int capacity, int cleanupDelayMillis)
    //
    // Map interface methods (delegated to the strategy)
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
    // Additional API
    //   void shutdown()

    /* ----------------------------------------------------------------------
     * 2. Test fixtures
     * ---------------------------------------------------------------------- */
    private LRUCache<String, Integer> lockingCache;
    private LRUCache<String, Integer> threadedCache;

    @Before
    public void setUp() {
        // capacity 2 is small enough to trigger eviction quickly
        lockingCache = new LRUCache<>(2, StrategyType.LOCKING);
        threadedCache = new LRUCache<>(2, StrategyType.THREADED);
    }

    @After
    public void tearDown() {
        // Ensure threaded cache shuts down its internal scheduler
        if (threadedCache != null) {
            threadedCache.shutdown();
        }
    }

    /* ----------------------------------------------------------------------
     * 3. Basic functional tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testPutAndGet_Locking() {
        lockingCache.put("a", 1);
        lockingCache.put("b", 2);
        assertEquals(Integer.valueOf(1), lockingCache.get("a"));
        assertEquals(Integer.valueOf(2), lockingCache.get("b"));
    }

    @Test
    public void testPutAndGet_Threaded() {
        threadedCache.put("x", 10);
        threadedCache.put("y", 20);
        assertEquals(Integer.valueOf(10), threadedCache.get("x"));
        assertEquals(Integer.valueOf(20), threadedCache.get("y"));
    }

    @Test
    public void testPutAll() {
        Map<String, Integer> source = new HashMap<>();
        source.put("k1", 100);
        source.put("k2", 200);
        lockingCache.putAll(source);
        assertEquals(2, lockingCache.size());
        assertTrue(lockingCache.containsKey("k1"));
        assertTrue(lockingCache.containsValue(200));
    }

    @Test
    public void testRemoveExistingKey() {
        lockingCache.put("toRemove", 99);
        Integer removed = lockingCache.remove("toRemove");
        assertEquals(Integer.valueOf(99), removed);
        assertFalse(lockingCache.containsKey("toRemove"));
    }

    @Test
    public void testRemoveNonExistingKeyReturnsNull() {
        assertNull(lockingCache.remove("missing"));
    }

    @Test
    public void testClear() {
        lockingCache.put("a", 1);
        lockingCache.put("b", 2);
        lockingCache.clear();
        assertTrue(lockingCache.isEmpty());
        assertEquals(0, lockingCache.size());
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
    public void testContainsKeyAndValue() {
        lockingCache.put("key", null);
        lockingCache.put(null, 5);
        assertTrue(lockingCache.containsKey("key"));
        assertTrue(lockingCache.containsKey(null));
        assertTrue(lockingCache.containsValue(null));
        assertTrue(lockingCache.containsValue(5));
    }

    @Test
    public void testKeySetAndValuesAndEntrySet() {
        lockingCache.put("k1", 10);
        lockingCache.put("k2", 20);

        Set<String> keys = lockingCache.keySet();
        Collection<Integer> vals = lockingCache.values();
        Set<Map.Entry<String,Integer>> entries = lockingCache.entrySet();

        assertEquals(new HashSet<>(Arrays.asList("k1","k2")), keys);
        assertEquals(new HashSet<>(Arrays.asList(10,20)), new HashSet<>(vals));
        assertEquals(2, entries.size());
        for (Map.Entry<String,Integer> e : entries) {
            if (e.getKey().equals("k1")) {
                assertEquals(Integer.valueOf(10), e.getValue());
            } else if (e.getKey().equals("k2")) {
                assertEquals(Integer.valueOf(20), e.getValue());
            } else {
                fail("Unexpected entry: " + e);
            }
        }
    }

    @Test
    public void testToStringContainsEntries() {
        lockingCache.put("a", 1);
        lockingCache.put("b", 2);
        String str = lockingCache.toString();
        assertTrue(str.contains("a=1"));
        assertTrue(str.contains("b=2"));
    }

    @Test
    public void testHashCodeAndEquals() {
        Map<String,Integer> map = new HashMap<>();
        map.put("x", 9);
        map.put("y", 8);

        LRUCache<String,Integer> other = new LRUCache<>(2, StrategyType.LOCKING);
        other.putAll(map);

        assertEquals(map.hashCode(), other.hashCode());
        assertTrue(other.equals(map));
        assertTrue(other.equals(other)); // reflexive
        assertFalse(other.equals(null));
        assertFalse(other.equals("someString"));
    }

    @Test
    public void testShutdownOnlyAffectsThreadedStrategy() {
        // Before shutdown the internal ThreadedLRUCacheStrategy should be present
        // (no direct access – we just ensure no exception is thrown)
        threadedCache.shutdown();

        // After shutdown, further puts should still work because the map itself remains functional.
        threadedCache.put("afterShutdown", 123);
        assertEquals(Integer.valueOf(123), threadedCache.get("afterShutdown"));
    }

    /* ----------------------------------------------------------------------
     * 4. Edge‑case and exception tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testConstructorWithNullStrategyThrowsException() {
        try {
            new LRUCache<>(5, null);
            fail("Expected IllegalArgumentException when strategyType is null");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Unsupported strategy type"));
        }
    }

    @Test
    public void testCapacityZeroAllowsNoEntries() {
        LRUCache<String,String> zeroCache = new LRUCache<>(0, StrategyType.LOCKING);
        zeroCache.put("any", "value");
        // Since capacity is zero, the cache should immediately evict the entry.
        // The underlying strategy implementation (LockingLRUCacheStrategy) does not
        // throw, it just keeps size 0.
        assertEquals(0, zeroCache.size());
        assertNull(zeroCache.get("any"));
    }

    @Test
    public void testNegativeCapacityThrowsExceptionFromStrategy() {
        // The strategy constructors themselves are expected to validate capacity.
        // If they don't, the test will still succeed because the cache will behave
        // oddly; we assert that an IllegalArgumentException is thrown.
        try {
            new LRUCache<>(-1, StrategyType.LOCKING);
            fail("Expected IllegalArgumentException for negative capacity");
        } catch (IllegalArgumentException e) {
            // Expected – message may vary, we only care that it is thrown.
        }
    }

    @Test
    public void testLruEvictionLockingStrategy() {
        LRUCache<String,Integer> cache = new LRUCache<>(2, StrategyType.LOCKING);
        cache.put("first", 1);   // cache: first
        cache.put("second", 2);  // cache: first, second
        cache.get("first");      // access first -> makes it most recent
        cache.put("third", 3);    // should evict "second"

        assertTrue(cache.containsKey("first"));
        assertTrue(cache.containsKey("third"));
        assertFalse(cache.containsKey("second"));
    }

    @Test
    public void testLruEvictionThreadedStrategy() throws InterruptedException {
        // Use a very small cleanup delay so eviction happens quickly.
        LRUCache<String,Integer> cache = new LRUCache<>(2, 1);
        cache.put("a", 1);
        cache.put("b", 2);
        cache.get("a"); // make "a" most recent
        cache.put("c", 3); // now size is 3, eviction should happen shortly

        // Wait a bit longer than the cleanup delay to let the background thread run.
        Thread.sleep(50);

        assertTrue(cache.containsKey("a"));
        assertTrue(cache.containsKey("c"));
        assertFalse(cache.containsKey("b"));
    }

    @Test
    public void testNullKeyAndValueSupported() {
        LRUCache<String,String> cache = new LRUCache<>(3, StrategyType.LOCKING);
        cache.put(null, "nullKey");
        cache.put("nullValue", null);
        cache.put(null, null); // overwrites previous null key entry

        assertTrue(cache.containsKey(null));
        assertTrue(cache.containsValue(null));
        assertEquals(null, cache.get(null)); // value is null
        assertEquals(null, cache.get("nullValue"));
    }

    @Test
    public void testPutAllWithNullAndDuplicateKeys() {
        Map<String,Integer> src = new HashMap<>();
        src.put("dup", 1);
        src.put("dup", 2); // last value wins
        src.put(null, 3);
        src.put("nullVal", null);

        LRUCache<String,Integer> cache = new LRUCache<>(5, StrategyType.LOCKING);
        cache.putAll(src);

        assertEquals(Integer.valueOf(2), cache.get("dup"));
        assertEquals(Integer.valueOf(3), cache.get(null));
        assertNull(cache.get("nullVal"));
    }

    @Test
    public void testEqualsWithDifferentImplementationTypes() {
        LRUCache<String,Integer> cache1 = new LRUCache<>(2, StrategyType.LOCKING);
        LRUCache<String,Integer> cache2 = new LRUCache<>(2, StrategyType.THREADED);
        cache1.put("x", 1);
        cache2.put("x", 1);

        // Even though underlying strategies differ, equals uses the Map contract.
        assertTrue(cache1.equals(cache2));
        assertTrue(cache2.equals(cache1));
    }

    @Test
    public void testHashCodeConsistencyAfterMutations() {
        LRUCache<String,Integer> cache = new LRUCache<>(3, StrategyType.LOCKING);
        int hashBefore = cache.hashCode();
        cache.put("a", 1);
        cache.put("b", 2);
        int hashAfter = cache.hashCode();
        assertNotEquals(hashBefore, hashAfter);
    }
}
