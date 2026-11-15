package com.cedarsoftware.util;

import com.cedarsoftware.util.LRUCache.StrategyType;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link LRUCache}.
 * <p>
 * The tests cover:
 * <ul>
 *   <li>All public methods (basic happy‑path tests)</li>
 *   <li>Edge‑cases such as null keys/values, zero/negative capacity, duplicate keys</li>
 *   <li>Eviction behaviour for both LOCKING and THREADED strategies</li>
 *   <li>Exception handling (illegal strategy argument)</li>
 *   <li>Correctness of {@code equals}, {@code hashCode} and collection views</li>
 * </ul>
 */
public class LRUCacheTest {

    private static final int CAPACITY = 3;

    private LRUCache<Integer, String> lockingCache;
    private LRUCache<Integer, String> threadedCache;

    @Before
    public void setUp() {
        lockingCache = new LRUCache<>(CAPACITY, StrategyType.LOCKING);
        // use a very short cleanup delay so eviction happens quickly in tests
        threadedCache = new LRUCache<>(CAPACITY, 5);
    }

    /* -------------------------------------------------
     *  Basic constructor / capacity tests
     * ------------------------------------------------- */

    @Test
    public void testLockingConstructorSetsCapacity() {
        assertEquals(CAPACITY, lockingCache.getCapacity());
    }

    @Test
    public void testThreadedConstructorSetsCapacity() {
        assertEquals(CAPACITY, threadedCache.getCapacity());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullStrategyThrows() {
        new LRUCache<>(CAPACITY, (StrategyType) null);
    }

    @Test
    public void testZeroCapacityCache() {
        LRUCache<Integer, String> zeroCap = new LRUCache<>(0, StrategyType.LOCKING);
        assertEquals(0, zeroCap.getCapacity());
        zeroCap.put(1, "one");
        // capacity 0 => every put should evict immediately, size stays 0
        assertEquals(0, zeroCap.size());
        assertNull(zeroCap.get(1));
    }

    @Test
    public void testNegativeCapacityIsAllowedByConstructor() {
        // The underlying strategy does not guard against negative numbers,
        // we just verify that the cache reports the same value.
        LRUCache<Integer, String> negative = new LRUCache<>(-5, StrategyType.LOCKING);
        assertEquals(-5, negative.getCapacity());
    }

    /* -------------------------------------------------
     *  put / get / remove basic behaviour
     * ------------------------------------------------- */

    @Test
    public void testPutAndGet() {
        assertNull(lockingCache.put(1, "one"));
        assertEquals("one", lockingCache.get(1));
        assertNull(lockingCache.put(1, "uno")); // first insert returns null
        assertEquals("uno", lockingCache.get(1));
    }

    @Test
    public void testPutNullKeyAndValue() {
        assertNull(lockingCache.put(null, "nullKey"));
        assertEquals("nullKey", lockingCache.get(null));

        assertNull(lockingCache.put(99, null));
        assertNull(lockingCache.get(99));
    }

    @Test
    public void testRemove() {
        lockingCache.put(1, "one");
        assertEquals("one", lockingCache.remove(1));
        assertNull(lockingCache.get(1));
        assertTrue(lockingCache.isEmpty());
    }

    @Test
    public void testRemoveNonExistingKeyReturnsNull() {
        assertNull(lockingCache.remove(42));
    }

    /* -------------------------------------------------
     *  putAll / clear / size / isEmpty
     * ------------------------------------------------- */

    @Test
    public void testPutAll() {
        Map<Integer, String> map = new HashMap<>();
        map.put(1, "one");
        map.put(2, "two");
        lockingCache.putAll(map);
        assertEquals(2, lockingCache.size());
        assertEquals("one", lockingCache.get(1));
        assertEquals("two", lockingCache.get(2));
    }

    @Test
    public void testClear() {
        lockingCache.put(1, "one");
        lockingCache.put(2, "two");
        lockingCache.clear();
        assertTrue(lockingCache.isEmpty());
        assertEquals(0, lockingCache.size());
    }

    /* -------------------------------------------------
     *  containsKey / containsValue
     * ------------------------------------------------- */

    @Test
    public void testContainsKeyAndValue() {
        lockingCache.put(1, "one");
        assertTrue(lockingCache.containsKey(1));
        assertTrue(lockingCache.containsValue("one"));
        assertFalse(lockingCache.containsKey(2));
        assertFalse(lockingCache.containsValue("two"));
    }

    /* -------------------------------------------------
     *  entrySet / keySet / values views
     * ------------------------------------------------- */

    @Test
    public void testEntrySetKeySetValuesReflectCache() {
        lockingCache.put(1, "one");
        lockingCache.put(2, "two");

        Set<Map.Entry<Integer, String>> entrySet = lockingCache.entrySet();
        assertEquals(2, entrySet.size());
        // verify that the entrySet is backed by the cache
        entrySet.iterator().next().setValue("changed");
        assertEquals("changed", lockingCache.get(1));

        Set<Integer> keySet = lockingCache.keySet();
        assertTrue(keySet.containsAll(Arrays.asList(1, 2)));

        Collection<String> values = lockingCache.values();
        assertTrue(values.containsAll(Arrays.asList("changed", "two")));
    }

    /* -------------------------------------------------
     *  toString / hashCode / equals
     * ------------------------------------------------- */

    @Test
    public void testToStringContainsEntries() {
        lockingCache.put(1, "one");
        String s = lockingCache.toString();
        assertTrue(s.contains("1=one"));
    }

    @Test
    public void testHashCodeConsistency() {
        lockingCache.put(1, "one");
        int h1 = lockingCache.hashCode();
        int h2 = lockingCache.hashCode();
        assertEquals(h1, h2);
    }

    @Test
    public void testEqualsWithSameContent() {
        lockingCache.put(1, "one");
        lockingCache.put(2, "two");

        Map<Integer, String> other = new HashMap<>();
        other.put(1, "one");
        other.put(2, "two");

        assertTrue(lockingCache.equals(other));
        assertTrue(other.equals(lockingCache));
    }

    @Test
    public void testEqualsWithDifferentContent() {
        lockingCache.put(1, "one");
        Map<Integer, String> other = Collections.singletonMap(1, "different");
        assertFalse(lockingCache.equals(other));
    }

    @Test
    public void testEqualsWithNonMapAndNull() {
        assertFalse(lockingCache.equals(null));
        assertFalse(lockingCache.equals("not a map"));
    }

    /* -------------------------------------------------
     *  Eviction behaviour (LRU) – LOCKING strategy
     * ------------------------------------------------- */

    @Test
    public void testLockingEvictsLeastRecentlyUsed() {
        // capacity = 3, insert 4 distinct keys
        lockingCache.put(1, "one");   // LRU order: 1
        lockingCache.put(2, "two");   // 1,2
        lockingCache.put(3, "three"); // 1,2,3
        // access key 1 to make it most recent
        assertEquals("one", lockingCache.get(1)); // order: 2,3,1
        // insert fourth entry, should evict key 2 (the least recent)
        lockingCache.put(4, "four");
        assertNull(lockingCache.get(2));
        assertEquals("one", lockingCache.get(1));
        assertEquals("three", lockingCache.get(3));
        assertEquals("four", lockingCache.get(4));
        assertEquals(3, lockingCache.size());
    }

    /* -------------------------------------------------
     *  Eviction behaviour (LRU) – THREADED strategy
     * ------------------------------------------------- */

    @Test
    public void testThreadedEvictsLeastRecentlyUsed() throws InterruptedException {
        // capacity = 3, cleanup delay = 5 ms (set in setUp)
        threadedCache.put(1, "one");
        threadedCache.put(2, "two");
        threadedCache.put(3, "three");
        // access key 1 to make it recent
        assertEquals("one", threadedCache.get(1));
        // add fourth entry – eviction will happen asynchronously
        threadedCache.put(4, "four");

        // wait a bit longer than cleanupDelay to let the background thread run
        Thread.sleep(50);

        // key 2 should have been evicted
        assertNull(threadedCache.get(2));
        assertEquals("one", threadedCache.get(1));
        assertEquals("three", threadedCache.get(3));
        assertEquals("four", threadedCache.get(4));
        assertEquals(3, threadedCache.size());
    }

    /* -------------------------------------------------
     *  Deprecated shutdown method – ensure it exists and does nothing
     * ------------------------------------------------- */

    @Test
    public void testShutdownDoesNotThrow() {
        // The method is deprecated and empty; just verify it can be called.
        lockingCache.shutdown();
        threadedCache.shutdown();
    }
}
