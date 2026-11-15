package com.cedarsoftware.util.cache;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link ThreadedLRUCacheStrategy}.
 *
 * The tests cover:
 * <ul>
 *   <li>Public API signatures</li>
 *   <li>Basic functional behaviour of each method</li>
 *   <li>Edge‑cases such as null keys/values and illegal constructor arguments</li>
 *   <li>LRU eviction logic (capacity enforcement)</li>
 *   <li>Equality, hashCode and toString contracts</li>
 * </ul>
 *
 * The scheduled cleanup task runs on a daemon thread.  All tests use a very small
 * {@code cleanupDelayMillis} (20 ms) and explicitly {@code Thread.sleep}
 * long enough for the background task to execute.
 */
public class ThreadedLRUCacheStrategyTest {

    /** Small delay to make the background cleanup run quickly. */
    private static final int CLEANUP_DELAY = 20; // ms

    private ThreadedLRUCacheStrategy<String, Integer> cache;

    @Before
    public void setUp() {
        // capacity 2 is enough to test eviction without making the test too slow
        cache = new ThreadedLRUCacheStrategy<>(2, CLEANUP_DELAY);
    }

    @After
    public void tearDown() {
        // Ensure the cache is cleared so later tests start fresh.
        cache.clear();
    }

    /* --------------------------------------------------------------
     *  1.  Public method signatures (extracted for documentation)
     * --------------------------------------------------------------
     *
     *  public ThreadedLRUCacheStrategy(int capacity, int cleanupDelayMillis)
     *  public int getCapacity()
     *  public V get(Object key)
     *  public V put(K key, V value)
     *  public void putAll(Map<? extends K, ? extends V> m)
     *  public boolean isEmpty()
     *  public V remove(Object key)
     *  public void clear()
     *  public int size()
     *  public boolean containsKey(Object key)
     *  public boolean containsValue(Object value)
     *  public Set<Map.Entry<K, V>> entrySet()
     *  public Set<K> keySet()
     *  public Collection<V> values()
     *  public boolean equals(Object o)
     *  public int hashCode()
     *  public String toString()
     */

    /* --------------------------------------------------------------
     *  2.  Basic functional tests for each public method
     * -------------------------------------------------------------- */

    @Test
    public void testGetCapacity() {
        assertEquals(2, cache.getCapacity());
    }

    @Test
    public void testPutAndGetSimple() {
        assertNull(cache.put("a", 1));          // first insert returns null
        assertEquals(Integer.valueOf(1), cache.get("a"));
        assertEquals(1, cache.size());
    }

    @Test
    public void testPutReturnsPreviousValue() {
        cache.put("x", 10);
        Integer old = cache.put("x", 20);
        assertEquals(Integer.valueOf(10), old);
        assertEquals(Integer.valueOf(20), cache.get("x"));
    }

    @Test
    public void testPutAll() {
        Map<String, Integer> map = new HashMap<>();
        map.put("k1", 100);
        map.put("k2", 200);
        cache.putAll(map);
        assertEquals(2, cache.size());
        assertEquals(Integer.valueOf(100), cache.get("k1"));
        assertEquals(Integer.valueOf(200), cache.get("k2"));
    }

    @Test
    public void testIsEmptyAndClear() {
        assertTrue(cache.isEmpty());
        cache.put("one", 1);
        assertFalse(cache.isEmpty());
        cache.clear();
        assertTrue(cache.isEmpty());
    }

    @Test
    public void testRemove() {
        cache.put("toRemove", 999);
        Integer removed = cache.remove("toRemove");
        assertEquals(Integer.valueOf(999), removed);
        assertNull(cache.get("toRemove"));
        assertEquals(0, cache.size());
    }

    @Test
    public void testContainsKeyAndValue() {
        cache.put("alpha", 42);
        assertTrue(cache.containsKey("alpha"));
        assertTrue(cache.containsValue(42));
        assertFalse(cache.containsKey("beta"));
        assertFalse(cache.containsValue(99));
    }

    @Test
    public void testEntrySetKeySetValues() {
        cache.put("k1", 1);
        cache.put("k2", 2);
        Set<Map.Entry<String, Integer>> entries = cache.entrySet();
        assertEquals(2, entries.size());
        // entrySet should be unmodifiable – attempting to add throws UnsupportedOperationException
        try {
            entries.add(new AbstractMap.SimpleEntry<>("k3", 3));
            fail("entrySet should be unmodifiable");
        } catch (UnsupportedOperationException ignored) {}

        Set<String> keys = cache.keySet();
        assertTrue(keys.containsAll(Arrays.asList("k1", "k2")));
        try {
            keys.remove("k1");
            fail("keySet should be unmodifiable");
        } catch (UnsupportedOperationException ignored) {}

        Collection<Integer> values = cache.values();
        assertTrue(values.containsAll(Arrays.asList(1, 2)));
        try {
            values.clear();
            fail("values collection should be unmodifiable");
        } catch (UnsupportedOperationException ignored) {}
    }

    @Test
    public void testEqualsAndHashCode() {
        cache.put("a", 1);
        cache.put("b", 2);

        Map<String, Integer> other = new HashMap<>();
        other.put("a", 1);
        other.put("b", 2);

        assertTrue(cache.equals(other));
        assertEquals(cache.hashCode(), other.hashCode());

        // change one entry – equality should break
        cache.put("b", 99);
        assertFalse(cache.equals(other));
    }

    @Test
    public void testToStringUsesMapUtilities() {
        cache.put("x", 10);
        String s = cache.toString();
        assertNotNull(s);
        assertTrue(s.contains("x"));
        assertTrue(s.contains("10"));
    }

    /* --------------------------------------------------------------
     *  3.  Edge‑case / exception handling tests
     * -------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsZeroCapacity() {
        new ThreadedLRUCacheStrategy<>(0, 20);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsNegativeCapacity() {
        new ThreadedLRUCacheStrategy<>(-5, 20);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsTooSmallDelay() {
        new ThreadedLRUCacheStrategy<>(2, 5); // less than 10 ms
    }

    @Test
    public void testNullKeyAndNullValueSupported() {
        cache.put(null, null);
        assertTrue(cache.containsKey(null));
        assertTrue(cache.containsValue(null));
        assertNull(cache.get(null));
        assertNull(cache.remove(null));
        assertFalse(cache.containsKey(null));
    }

    /* --------------------------------------------------------------
     *  4.  LRU eviction (capacity enforcement) tests
     * -------------------------------------------------------------- */

    @Test
    public void testEvictionWhenCapacityExceeded() throws Exception {
        // capacity = 2, cleanup delay = 20 ms
        cache.put("first", 1);   // becomes MRU after put
        Thread.sleep(CLEANUP_DELAY + 5); // allow any pending cleanup (none expected)

        cache.put("second", 2);
        Thread.sleep(CLEANUP_DELAY + 5);

        // At this point cache is full (size == 2)
        assertEquals(2, cache.size());
        assertTrue(cache.containsKey("first"));
        assertTrue(cache.containsKey("second"));

        // Access "first" to make it most‑recently used
        cache.get("first");

        // Insert third entry – size becomes 3, triggering scheduled cleanup
        cache.put("third", 3);
        // Wait for the background cleanup to run (twice the delay is safe)
        Thread.sleep(CLEANUP_DELAY * 2);

        // After cleanup only the two most recent entries should remain:
        // "first" (touched) and "third". "second" must have been evicted.
        assertEquals(2, cache.size());
        assertTrue(cache.containsKey("first"));
        assertTrue(cache.containsKey("third"));
        assertFalse(cache.containsKey("second"));
    }

    @Test
    public void testImmediateCleanupSchedulingWhenSizeExceedsCapacity() throws Exception {
        // Insert three entries quickly; size will be > capacity before the periodic task runs.
        cache.put("a", 1);
        cache.put("b", 2);
        cache.put("c", 3); // triggers scheduleImmediateCleanup()
        // Wait a bit longer than the cleanup delay to ensure the immediate task fires.
        Thread.sleep(CLEANUP_DELAY * 2);

        // Verify that only the two most recent entries survive.
        assertEquals(2, cache.size());
        // The LRU order is not deterministic without timestamps, but we know the oldest ("a") should be gone.
        assertFalse(cache.containsKey("a"));
        assertTrue(cache.containsKey("b"));
        assertTrue(cache.containsKey("c"));
    }

    /* --------------------------------------------------------------
     *  5.  Helper to pause for background tasks (used above)
     * -------------------------------------------------------------- */
    private void pauseForCleanup() throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(CLEANUP_DELAY * 2);
    }
}
