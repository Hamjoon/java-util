package com.cedarsoftware.util.cache;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ThreadedLRUCacheStrategy}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public methods and their basic contract.</li>
 *   <li>Edge cases such as null keys/values, capacity boundaries, and concurrent‑like timing of cleanup.</li>
 *   <li>Exception handling for illegal constructor arguments.</li>
 *   <li>Correct LRU eviction behavior triggered by the scheduled cleanup task.</li>
 *   <li>Equality, hashCode and toString consistency with Map contract.</li>
 * </ul>
 *
 * The shared scheduler used by the cache runs daemon threads, therefore the tests wait a short
 * amount of time after operations that may schedule a cleanup to allow the background task to execute.
 */
public class ThreadedLRUCacheStrategyTest {

    // Reasonable defaults for most tests – small capacity makes eviction easy to verify.
    private static final int CAPACITY = 3;
    private static final int CLEANUP_DELAY_MS = 50; // short enough for tests but respects the 10 ms minimum

    private ThreadedLRUCacheStrategy<String, String> cache;

    @Before
    public void setUp() {
        cache = new ThreadedLRUCacheStrategy<>(CAPACITY, CLEANUP_DELAY_MS);
    }

    @After
    public void tearDown() {
        // Clear the cache to avoid interference between tests.
        cache.clear();
    }

    /* ----------------------------------------------------------------------
     *  1. Constructor validation
     * ---------------------------------------------------------------------- */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsZeroCapacity() {
        new ThreadedLRUCacheStrategy<>(0, CLEANUP_DELAY_MS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsNegativeCapacity() {
        new ThreadedLRUCacheStrategy<>(-5, CLEANUP_DELAY_MS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsTooSmallDelay() {
        new ThreadedLRUCacheStrategy<>(CAPACITY, 5);
    }

    /* ----------------------------------------------------------------------
     *  2. Basic Map operations
     * ---------------------------------------------------------------------- */
    @Test
    public void testPutAndGet() {
        assertNull(cache.put("k1", "v1"));
        assertEquals("v1", cache.get("k1"));
        // put same key again – should return previous value
        assertEquals("v1", cache.put("k1", "v1b"));
        assertEquals("v1b", cache.get("k1"));
    }

    @Test
    public void testPutNullKeyAndValue() {
        // Null key allowed
        assertNull(cache.put(null, "nullKey"));
        assertEquals("nullKey", cache.get(null));

        // Null value allowed
        assertNull(cache.put("nullValue", null));
        assertNull(cache.get("nullValue"));
    }

    @Test
    public void testRemove() {
        cache.put("a", "A");
        cache.put("b", "B");
        assertEquals("A", cache.remove("a"));
        assertNull(cache.get("a"));
        assertEquals(1, cache.size());

        // Removing absent key returns null
        assertNull(cache.remove("nonexistent"));
    }

    @Test
    public void testClearAndIsEmpty() {
        cache.put("x", "X");
        cache.put("y", "Y");
        assertFalse(cache.isEmpty());
        cache.clear();
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
    }

    @Test
    public void testContainsKeyAndContainsValue() {
        cache.put("k", "v");
        assertTrue(cache.containsKey("k"));
        assertTrue(cache.containsValue("v"));
        assertFalse(cache.containsKey("missing"));
        assertFalse(cache.containsValue("missing"));
    }

    @Test
    public void testPutAll() {
        Map<String, String> source = new HashMap<>();
        source.put("one", "1");
        source.put("two", "2");
        source.put("three", "3");
        cache.putAll(source);
        assertEquals(3, cache.size());
        assertEquals("1", cache.get("one"));
        assertEquals("2", cache.get("two"));
        assertEquals("3", cache.get("three"));
    }

    /* ----------------------------------------------------------------------
     *  3. Collection view methods
     * ---------------------------------------------------------------------- */
    @Test
    public void testKeySet() {
        cache.put("k1", "v1");
        cache.put("k2", "v2");
        Set<String> keys = cache.keySet();
        assertEquals(2, keys.size());
        assertTrue(keys.containsAll(Arrays.asList("k1", "k2")));

        // The returned set must be unmodifiable – attempt to modify should throw
        try {
            keys.add("k3");
            fail("keySet should be unmodifiable");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    @Test
    public void testValuesCollection() {
        cache.put("a", "A");
        cache.put("b", "B");
        Collection<String> values = cache.values();
        assertEquals(2, values.size());
        assertTrue(values.containsAll(Arrays.asList("A", "B")));

        try {
            values.remove("A");
            fail("values collection should be unmodifiable");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    @Test
    public void testEntrySet() {
        cache.put("k", "v");
        Set<Map.Entry<String, String>> entries = cache.entrySet();
        assertEquals(1, entries.size());
        Map.Entry<String, String> entry = entries.iterator().next();
        assertEquals("k", entry.getKey());
        assertEquals("v", entry.getValue());

        try {
            entries.clear();
            fail("entrySet should be unmodifiable");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    /* ----------------------------------------------------------------------
     *  4. Equality, hashCode and toString
     * ---------------------------------------------------------------------- */
    @Test
    public void testEqualsAndHashCode() {
        cache.put("a", "A");
        cache.put("b", "B");

        Map<String, String> other = new HashMap<>();
        other.put("a", "A");
        other.put("b", "B");

        assertTrue(cache.equals(other));
        assertEquals(cache.hashCode(), other.hashCode());
    }

    @Test
    public void testToString() {
        cache.put("x", "X");
        String s = cache.toString();
        assertTrue(s.contains("x"));
        assertTrue(s.contains("X"));
    }

    /* ----------------------------------------------------------------------
     *  5. LRU eviction behavior
     * ---------------------------------------------------------------------- */
    @Test
    public void testEvictionWhenCapacityExceeded() throws InterruptedException {
        // Fill up to capacity
        cache.put("1", "one");
        cache.put("2", "two");
        cache.put("3", "three");
        assertEquals(CAPACITY, cache.size());

        // Access "1" to make it most‑recently used
        assertEquals("one", cache.get("1"));

        // Add a fourth entry – this should trigger a scheduled cleanup
        cache.put("4", "four");
        // Wait enough for the cleanup task to run (twice the delay to be safe)
        TimeUnit.MILLISECONDS.sleep(CLEANUP_DELAY_MS * 3);

        // The least‑recently used entry ("2") must have been evicted
        assertNull(cache.get("2"));
        assertEquals("one", cache.get("1"));   // still present
        assertEquals("three", cache.get("3"));
        assertEquals("four", cache.get("4"));
        assertEquals(CAPACITY, cache.size());
    }

    @Test
    public void testImmediateCleanupScheduling() throws InterruptedException {
        // Directly exceed capacity without waiting for the regular schedule
        cache.put("a", "A");
        cache.put("b", "B");
        cache.put("c", "C");
        cache.put("d", "D"); // triggers immediate cleanup scheduling

        // Wait for the immediate cleanup to fire
        TimeUnit.MILLISECONDS.sleep(CLEANUP_DELAY_MS * 2);

        // Exactly CAPACITY entries must remain
        assertEquals(CAPACITY, cache.size());
        // One of the earlier entries must be gone; we cannot guarantee which due to timing,
        // but we can assert that at least one of the first three keys is missing.
        int missing = 0;
        for (String k : Arrays.asList("a", "b", "c")) {
            if (cache.get(k) == null) missing++;
        }
        assertTrue("At least one of the first three entries should have been evicted", missing >= 1);
    }

    /* ----------------------------------------------------------------------
     *  6. Edge cases around cleanup flags
     * ---------------------------------------------------------------------- */
    @Test
    public void testNoCleanupWhenSizeWithinCapacity() throws InterruptedException {
        cache.put("x", "X");
        cache.put("y", "Y");
        // Size is 2 < capacity (3); no cleanup should be scheduled.
        // Wait longer than the regular cleanup interval to ensure nothing happens.
        TimeUnit.MILLISECONDS.sleep(CLEANUP_DELAY_MS * 4);
        assertEquals(2, cache.size());
        assertEquals("X", cache.get("x"));
        assertEquals("Y", cache.get("y"));
    }

    @Test
    public void testCleanupReschedulesIfStillOverCapacity() throws InterruptedException {
        // Insert many items to force multiple cleanup cycles.
        for (int i = 0; i < CAPACITY + 5; i++) {
            cache.put("k" + i, "v" + i);
        }
        // Wait enough for several cleanup runs.
        TimeUnit.MILLISECONDS.sleep(CLEANUP_DELAY_MS * 10);
        assertEquals(CAPACITY, cache.size());
        // Verify that the most recent entries survived.
        for (int i = 5; i < CAPACITY + 5; i++) {
            assertEquals("v" + i, cache.get("k" + i));
        }
    }

    /* ----------------------------------------------------------------------
     *  7. Concurrency‑like timing (no real multithreading needed)
     * ---------------------------------------------------------------------- */
    @Test
    public void testGetUpdatesTimestampSoItemIsNotImmediatelyEvicted() throws InterruptedException {
        cache.put("old", "O");
        cache.put("mid", "M");
        cache.put("new", "N");

        // Access "old" to refresh its timestamp
        assertEquals("O", cache.get("old"));

        // Insert another entry to exceed capacity
        cache.put("extra", "E");

        // Wait for cleanup
        TimeUnit.MILLISECONDS.sleep(CLEANUP_DELAY_MS * 3);

        // "mid" should be evicted (it was the least recently used), not "old"
        assertNull(cache.get("mid"));
        assertEquals("O", cache.get("old"));
        assertEquals("N", cache.get("new"));
        assertEquals("E", cache.get("extra"));
    }
}
