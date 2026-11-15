package com.cedarsoftware.util;

import com.cedarsoftware.util.cache.LockingLRUCacheStrategy;
import com.cedarsoftware.util.cache.ThreadedLRUCacheStrategy;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link LRUCache}.
 *
 * The tests are organised in the order the specification asked for:
 *   1. List of public methods (see comment block {@code // PUBLIC METHODS}).
 *   2. Basic functionality tests – one test per method that checks the happy‑path.
 *   3. Edge‑case / exception tests – covering nulls, capacity limits, illegal arguments,
 *      and the asynchronous eviction behaviour of the threaded strategy.
 *   4. All tests are merged into a single file {@code LRUCacheTest.java}.
 *
 * The class uses the concrete {@link LockingLRUCacheStrategy} for deterministic LRU ordering
 * and the {@link ThreadedLRUCacheStrategy} for exercising the asynchronous cleanup path.
 */
public class LRUCacheTest {

    // -------------------------------------------------------------------------
    // 1. PUBLIC METHODS OF LRUCache (extracted from the source)
    // -------------------------------------------------------------------------
    // Constructors
    //   LRUCache(int capacity)
    //   LRUCache(int capacity, StrategyType strategyType)
    //   LRUCache(int capacity, int cleanupDelayMillis)
    //
    // Instance methods
    //   int getCapacity()
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
    //   @Deprecated void shutdown()

    // -------------------------------------------------------------------------
    // Test fixtures
    // -------------------------------------------------------------------------
    private LRUCache<String, Integer> lockingCache;
    private LRUCache<String, Integer> threadedCache;

    @Before
    public void setUp() {
        // deterministic locking strategy (capacity 3)
        lockingCache = new LRUCache<>(3);
        // threaded strategy with a very short cleanup delay (capacity 3, 5 ms)
        threadedCache = new LRUCache<>(3, 5);
    }

    // -------------------------------------------------------------------------
    // 2. BASIC FUNCTIONALITY TESTS (one per public method)
    // -------------------------------------------------------------------------

    @Test
    public void testConstructorLockingStrategyCapacity() {
        LRUCache<Integer, String> c = new LRUCache<>(5);
        assertEquals(5, c.getCapacity());
    }

    @Test
    public void testConstructorStrategyTypeLocking() {
        LRUCache<Integer, String> c = new LRUCache<>(4, LRUCache.StrategyType.LOCKING);
        assertEquals(4, c.getCapacity());
    }

    @Test
    public void testConstructorStrategyTypeThreaded() {
        LRUCache<Integer, String> c = new LRUCache<>(2, LRUCache.StrategyType.THREADED);
        assertEquals(2, c.getCapacity());
    }

    @Test
    public void testConstructorThreadedWithDelay() {
        LRUCache<Integer, String> c = new LRUCache<>(7, 20);
        assertEquals(7, c.getCapacity());
    }

    @Test
    public void testPutAndGet() {
        lockingCache.put("a", 1);
        lockingCache.put("b", 2);
        assertEquals(Integer.valueOf(1), lockingCache.get("a"));
        assertEquals(Integer.valueOf(2), lockingCache.get("b"));
    }

    @Test
    public void testPutAll() {
        Map<String, Integer> map = new HashMap<>();
        map.put("x", 10);
        map.put("y", 20);
        lockingCache.putAll(map);
        assertEquals(2, lockingCache.size());
        assertTrue(lockingCache.containsKey("x"));
        assertTrue(lockingCache.containsValue(20));
    }

    @Test
    public void testRemove() {
        lockingCache.put("k", 99);
        assertEquals(Integer.valueOf(99), lockingCache.remove("k"));
        assertNull(lockingCache.get("k"));
        assertTrue(lockingCache.isEmpty());
    }

    @Test
    public void testClear() {
        lockingCache.put("one", 1);
        lockingCache.put("two", 2);
        lockingCache.clear();
        assertTrue(lockingCache.isEmpty());
        assertEquals(0, lockingCache.size());
    }

    @Test
    public void testSizeAndIsEmpty() {
        assertTrue(lockingCache.isEmpty());
        assertEquals(0, lockingCache.size());
        lockingCache.put("a", 1);
        assertFalse(lockingCache.isEmpty());
        assertEquals(1, lockingCache.size());
    }

    @Test
    public void testContainsKeyAndContainsValue() {
        lockingCache.put("key", 42);
        assertTrue(lockingCache.containsKey("key"));
        assertTrue(lockingCache.containsValue(42));
        assertFalse(lockingCache.containsKey("missing"));
        assertFalse(lockingCache.containsValue(0));
    }

    @Test
    public void testEntrySetKeySetValues() {
        lockingCache.put("p", 5);
        lockingCache.put("q", 6);
        Set<Map.Entry<String, Integer>> entrySet = lockingCache.entrySet();
        assertEquals(2, entrySet.size());

        Set<String> keys = lockingCache.keySet();
        assertTrue(keys.containsAll(Arrays.asList("p", "q")));

        Collection<Integer> values = lockingCache.values();
        assertTrue(values.containsAll(Arrays.asList(5, 6)));
    }

    @Test
    public void testToStringAndHashCode() {
        lockingCache.put("a", 1);
        lockingCache.put("b", 2);
        String s = lockingCache.toString();
        assertTrue(s.contains("a=1"));
        assertTrue(s.contains("b=2"));
        assertEquals(lockingCache.hashCode(), lockingCache.entrySet().hashCode());
    }

    @Test
    public void testEqualsWithAnotherMap() {
        Map<String, Integer> other = new HashMap<>();
        other.put("x", 100);
        other.put("y", 200);
        lockingCache.putAll(other);
        assertTrue(lockingCache.equals(other));
        assertTrue(other.equals(lockingCache));
    }

    @Test
    public void testEqualsWithSelfAndNull() {
        assertTrue(lockingCache.equals(lockingCache));
        assertFalse(lockingCache.equals(null));
        assertFalse(lockingCache.equals("not a map"));
    }

    @Test
    @Deprecated
    public void testShutdownIsDeprecatedButDoesNothing() {
        // No exception should be thrown; method is a no‑op.
        lockingCache.shutdown();
    }

    // -------------------------------------------------------------------------
    // 3. EDGE‑CASE & EXCEPTION TESTS
    // -------------------------------------------------------------------------

    @Test
    public void testNullKeyAndNullValue() {
        // Both key and value may be null according to the Javadoc.
        lockingCache.put(null, null);
        assertTrue(lockingCache.containsKey(null));
        assertTrue(lockingCache.containsValue(null));
        assertNull(lockingCache.get(null));
        assertNull(lockingCache.remove(null));
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullStrategyTypeThrowsNPE() {
        // Passing null for the enum leads to a NPE when the code tries to compare it.
        new LRUCache<>(5, null);
    }

    @Test
    public void testCapacityZeroDoesNotThrowButIsEmpty() {
        LRUCache<String, String> zeroCap = new LRUCache<>(0);
        assertEquals(0, zeroCap.getCapacity());
        zeroCap.put("any", "value"); // underlying strategy may accept but immediately evict
        assertTrue(zeroCap.isEmpty());
        assertEquals(0, zeroCap.size());
    }

    @Test
    public void testLruEvictionLockingStrategy() {
        // capacity = 3, insert 4 items, the least‑recently‑used ("a") should be evicted.
        lockingCache.put("a", 1);
        lockingCache.put("b", 2);
        lockingCache.put("c", 3);
        // Access "a" to make it most‑recently‑used, then add a new entry.
        lockingCache.get("a");
        lockingCache.put("d", 4); // eviction should remove "b"
        assertNull(lockingCache.get("b"));
        assertNotNull(lockingCache.get("a"));
        assertEquals(3, lockingCache.size());
    }

    @Test
    public void testThreadedStrategyAsyncEviction() throws InterruptedException {
        // The Threaded strategy may temporarily exceed capacity; we wait for cleanup.
        threadedCache.put("1", 1);
        threadedCache.put("2", 2);
        threadedCache.put("3", 3);
        // At this point cache is full.
        threadedCache.put("4", 4); // triggers async cleanup
        // Give the background thread a chance to run (cleanupDelayMillis = 5 ms)
        Thread.sleep(50);
        // After cleanup, size must be <= capacity (3) and the LRU entry ("1") should be gone.
        assertEquals(3, threadedCache.size());
        assertNull(threadedCache.get("1"));
        assertNotNull(threadedCache.get("4"));
    }

    @Test
    public void testPutAllDoesNotExceedCapacityWhenUsingLockingStrategy() {
        Map<String, Integer> many = new HashMap<>();
        many.put("a", 1);
        many.put("b", 2);
        many.put("c", 3);
        many.put("d", 4); // 4 entries, capacity is 3
        lockingCache.putAll(many);
        // The underlying strategy should have evicted the LRU entry.
        assertEquals(3, lockingCache.size());
        // Which key got evicted is implementation‑specific; we just assert that at least one is missing.
        int present = 0;
        for (String k : many.keySet()) {
            if (lockingCache.containsKey(k)) present++;
        }
        assertEquals(3, present);
    }

    @Test
    public void testEqualsWithDifferentImplementations() {
        LRUCache<String, Integer> other = new LRUCache<>(3, LRUCache.StrategyType.THREADED);
        other.put("x", 10);
        other.put("y", 20);
        lockingCache.put("x", 10);
        lockingCache.put("y", 20);
        assertTrue(lockingCache.equals(other));
        assertTrue(other.equals(lockingCache));
    }

    @Test
    public void testHashCodeConsistencyWithEquals() {
        LRUCache<String, Integer> c1 = new LRUCache<>(2);
        LRUCache<String, Integer> c2 = new LRUCache<>(2);
        c1.put("k1", 100);
        c1.put("k2", 200);
        c2.put("k1", 100);
        c2.put("k2", 200);
        assertEquals(c1, c2);
        assertEquals(c1.hashCode(), c2.hashCode());
    }

    // -------------------------------------------------------------------------
    // 4. COMPLETED TEST FILE (all tests above are part of the final suite)
    // -------------------------------------------------------------------------
}
