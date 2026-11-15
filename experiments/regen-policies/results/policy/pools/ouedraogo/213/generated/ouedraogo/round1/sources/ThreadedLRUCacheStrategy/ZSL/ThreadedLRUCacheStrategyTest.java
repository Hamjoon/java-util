package com.cedarsoftware.util.cache;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ThreadedLRUCacheStrategy}.
 * The tests cover:
 * <ul>
 *   <li>Constructor validation</li>
 *   <li>Basic Map operations (get, put, remove, clear, isEmpty, size)</li>
 *   <li>LRU eviction logic</li>
 *   <li>containsKey / containsValue</li>
 *   <li>entrySet / keySet / values immutability</li>
 *   <li>equals, hashCode and toString contracts</li>
 *   <li>putAll handling of nulls</li>
 * </ul>
 *
 * Note: The cache uses a shared {@link ScheduledExecutorService} that runs
 * cleanup tasks after {@code cleanupDelayMillis}.  All tests that rely on
 * eviction wait a little longer than the configured delay to ensure the
 * background task has executed.
 */
public class ThreadedLRUCacheStrategyTest {

    private static final int CAPACITY = 3;
    // Small delay to keep the test fast but still above the minimum of 10 ms.
    private static final int CLEANUP_DELAY = 20;

    private ThreadedLRUCacheStrategy<String, Integer> cache;

    @Before
    public void setUp() {
        cache = new ThreadedLRUCacheStrategy<>(CAPACITY, CLEANUP_DELAY);
    }

    @After
    public void tearDown() {
        if (cache != null) {
            cache.clear();
        }
    }

    /** Helper that waits for at least one cleanup cycle to finish. */
    private void awaitCleanup() throws InterruptedException {
        // Wait a bit longer than the configured delay.
        Thread.sleep(CLEANUP_DELAY * 3L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsZeroCapacity() {
        new ThreadedLRUCacheStrategy<>(0, CLEANUP_DELAY);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsNegativeDelay() {
        new ThreadedLRUCacheStrategy<>(CAPACITY, 5);
    }

    @Test
    public void basicPutGetRemoveWorks() {
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());

        // put returns null for new entry
        assertNull(cache.put("one", 1));
        assertEquals(Integer.valueOf(1), cache.get("one"));
        assertFalse(cache.isEmpty());
        assertEquals(1, cache.size());

        // put returns previous value
        assertEquals(Integer.valueOf(1), cache.put("one", 11));
        assertEquals(Integer.valueOf(11), cache.get("one"));
        assertEquals(1, cache.size());

        // remove returns the value and actually deletes the entry
        assertEquals(Integer.valueOf(11), cache.remove("one"));
        assertNull(cache.get("one"));
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
    }

    @Test
    public void putAllAndNullHandling() {
        Map<String, Integer> src = new HashMap<>();
        src.put("a", 1);
        src.put(null, 2);          // null key
        src.put("b", null);        // null value
        cache.putAll(src);

        assertEquals(3, cache.size());
        assertTrue(cache.containsKey("a"));
        assertTrue(cache.containsKey(null));
        assertTrue(cache.containsKey("b"));
        assertEquals(Integer.valueOf(1), cache.get("a"));
        assertEquals(Integer.valueOf(2), cache.get(null));
        assertNull(cache.get("b"));
    }

    @Test
    public void containsKeyAndContainsValue() {
        cache.put("x", 100);
        cache.put(null, 200);
        cache.put("y", null);

        assertTrue(cache.containsKey("x"));
        assertTrue(cache.containsKey(null));
        assertTrue(cache.containsKey("y"));
        assertFalse(cache.containsKey("z"));

        assertTrue(cache.containsValue(100));
        assertTrue(cache.containsValue(200));
        assertTrue(cache.containsValue(null));
        assertFalse(cache.containsValue(999));
    }

    @Test
    public void entrySetKeySetAndValuesAreUnmodifiable() {
        cache.put("k1", 10);
        cache.put("k2", 20);
        cache.put(null, 30);

        Set<Map.Entry<String, Integer>> entrySet = cache.entrySet();
        Set<String> keySet = cache.keySet();
        Collection<Integer> values = cache.values();

        // Verify contents
        assertEquals(3, entrySet.size());
        assertEquals(3, keySet.size());
        assertEquals(3, values.size());

        // Attempt modifications – should throw UnsupportedOperationException
        try {
            entrySet.clear();
            fail("entrySet should be unmodifiable");
        } catch (UnsupportedOperationException ignored) {}

        try {
            keySet.add("new");
            fail("keySet should be unmodifiable");
        } catch (UnsupportedOperationException ignored) {}

        try {
            values.remove(10);
            fail("values collection should be unmodifiable");
        } catch (UnsupportedOperationException ignored) {}
    }

    @Test
    public void equalsAndHashCodeContract() {
        cache.put("a", 1);
        cache.put("b", 2);
        cache.put(null, 3);

        // Build a plain HashMap with the same entries
        Map<String, Integer> other = new HashMap<>();
        other.put("a", 1);
        other.put("b", 2);
        other.put(null, 3);

        assertTrue(cache.equals(other));
        assertTrue(other.equals(cache)); // HashMap's equals also checks entries

        // Hash codes must be equal for equal maps
        assertEquals(cache.hashCode(), other.hashCode());

        // Changing one entry makes them unequal
        cache.put("a", 99);
        assertFalse(cache.equals(other));
    }

    @Test
    public void toStringProducesMapLikeRepresentation() {
        cache.put("one", 1);
        cache.put("two", 2);
        String s = cache.toString();
        assertTrue(s.contains("one=1"));
        assertTrue(s.contains("two=2"));
        // Order is not guaranteed – just ensure both pairs appear.
    }

    @Test
    public void lruEvictionRemovesLeastRecentlyUsed() throws Exception {
        // Fill the cache to capacity
        cache.put("first", 1);
        cache.put("second", 2);
        cache.put("third", 3);
        assertEquals(CAPACITY, cache.size());

        // Access "first" to make it most recent
        assertEquals(Integer.valueOf(1), cache.get("first"));

        // Add two more entries – this puts the cache over capacity (size = 5)
        cache.put("fourth", 4);
        cache.put("fifth", 5);
        assertEquals(5, cache.size());

        // Wait for background cleanup to run
        awaitCleanup();

        // After cleanup the size must be back to capacity
        assertEquals(CAPACITY, cache.size());

        // "second" and "third" were the least recently used and should be evicted
        assertNull(cache.get("second"));
        assertNull(cache.get("third"));

        // "first", "fourth", and "fifth" must still be present
        assertEquals(Integer.valueOf(1), cache.get("first"));
        assertEquals(Integer.valueOf(4), cache.get("fourth"));
        assertEquals(Integer.valueOf(5), cache.get("fifth"));
    }

    @Test
    public void immediateCleanupIsScheduledWhenSizeExceedsCapacity() throws Exception {
        // Insert items rapidly to exceed capacity before the regular scheduled task runs.
        cache.put("a", 1);
        cache.put("b", 2);
        cache.put("c", 3);
        cache.put("d", 4); // size = 4 > capacity = 3, should schedule immediate cleanup

        // Wait a little longer than the cleanup delay to allow the immediate task to fire.
        awaitCleanup();

        // Verify that the cache size is back to capacity and that the LRU entry ("a") was evicted.
        assertEquals(CAPACITY, cache.size());
        assertNull(cache.get("a"));
        assertNotNull(cache.get("b"));
        assertNotNull(cache.get("c"));
        assertNotNull(cache.get("d"));
    }

    @Test
    public void clearEmptiesCacheAndResetsState() {
        cache.put("x", 10);
        cache.put("y", 20);
        assertFalse(cache.isEmpty());
        assertEquals(2, cache.size());

        cache.clear();
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());

        // After clear, put should work normally
        cache.put("z", 30);
        assertEquals(Integer.valueOf(30), cache.get("z"));
        assertEquals(1, cache.size());
    }
}
