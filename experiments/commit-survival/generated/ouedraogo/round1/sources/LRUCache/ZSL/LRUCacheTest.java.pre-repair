package com.cedarsoftware.util;

import com.cedarsoftware.util.LRUCache.StrategyType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link LRUCache}.
 *
 * <p>The tests cover both underlying strategies (LOCKING and THREADED) and exercise every public
 * method defined in {@code LRUCache}.  Real objects are created – no mocking or subclassing of the
 * library classes is performed.</p>
 */
public class LRUCacheTest {

    private static final int CAPACITY = 3;
    private static final int CLEANUP_DELAY = 5; // milliseconds – small enough for fast tests

    private LRUCache<Integer, String> lockingCache;
    private LRUCache<Integer, String> threadedCache;

    @Before
    public void setUp() {
        // Default constructor uses the LOCKING strategy
        lockingCache = new LRUCache<>(CAPACITY);

        // Explicitly request the THREADED strategy with a tiny cleanup delay
        threadedCache = new LRUCache<>(CAPACITY, CLEANUP_DELAY);
    }

    @After
    public void tearDown() {
        // Ensure the threaded cache's internal scheduler is shut down to avoid thread leaks.
        threadedCache.shutdown();
    }

    /* ---------------------------------------------------------------------- */
    /*                         BASIC MAP OPERATIONS                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testPutAndGetLocking() {
        assertNull(lockingCache.put(1, "one"));
        assertEquals("one", lockingCache.get(1));

        assertNull(lockingCache.put(2, "two"));
        assertEquals("two", lockingCache.get(2));

        // Overwrite an existing key
        assertEquals("one", lockingCache.put(1, "uno"));
        assertEquals("uno", lockingCache.get(1));
    }

    @Test
    public void testPutAndGetThreaded() {
        assertNull(threadedCache.put(10, "ten"));
        assertEquals("ten", threadedCache.get(10));

        assertNull(threadedCache.put(20, "twenty"));
        assertEquals("twenty", threadedCache.get(20));

        // Overwrite an existing key
        assertEquals("ten", threadedCache.put(10, "diez"));
        assertEquals("diez", threadedCache.get(10));
    }

    @Test
    public void testNullKeyAndValueLocking() {
        assertNull(lockingCache.put(null, null));
        assertTrue(lockingCache.containsKey(null));
        assertTrue(lockingCache.containsValue(null));
        assertNull(lockingCache.get(null));

        // replace null value with a non‑null value
        assertNull(lockingCache.put(null, "nullKey"));
        assertEquals("nullKey", lockingCache.get(null));
    }

    @Test
    public void testNullKeyAndValueThreaded() {
        assertNull(threadedCache.put(null, null));
        assertTrue(threadedCache.containsKey(null));
        assertTrue(threadedCache.containsValue(null));
        assertNull(threadedCache.get(null));

        // replace null value with a non‑null value
        assertNull(threadedCache.put(null, "nullKey"));
        assertEquals("nullKey", threadedCache.get(null));
    }

    @Test
    public void testPutAllLocking() {
        Map<Integer, String> source = new HashMap<>();
        source.put(1, "one");
        source.put(2, "two");
        source.put(3, "three");

        lockingCache.putAll(source);
        assertEquals(3, lockingCache.size());
        assertEquals("one", lockingCache.get(1));
        assertEquals("two", lockingCache.get(2));
        assertEquals("three", lockingCache.get(3));
    }

    @Test
    public void testPutAllThreaded() {
        Map<Integer, String> source = new HashMap<>();
        source.put(10, "ten");
        source.put(20, "twenty");
        source.put(30, "thirty");

        threadedCache.putAll(source);
        // Allow cleanup thread to run (even though we are below capacity)
        sleep(20);
        assertEquals(3, threadedCache.size());
        assertEquals("ten", threadedCache.get(10));
        assertEquals("twenty", threadedCache.get(20));
        assertEquals("thirty", threadedCache.get(30));
    }

    @Test
    public void testContainsAndRemoveLocking() {
        lockingCache.put(1, "one");
        lockingCache.put(2, "two");

        assertTrue(lockingCache.containsKey(1));
        assertTrue(lockingCache.containsValue("two"));

        assertEquals("one", lockingCache.remove(1));
        assertFalse(lockingCache.containsKey(1));
        assertFalse(lockingCache.containsValue("one"));
        assertEquals(1, lockingCache.size());
    }

    @Test
    public void testContainsAndRemoveThreaded() {
        threadedCache.put(10, "ten");
        threadedCache.put(20, "twenty");

        assertTrue(threadedCache.containsKey(10));
        assertTrue(threadedCache.containsValue("twenty"));

        assertEquals("ten", threadedCache.remove(10));
        assertFalse(threadedCache.containsKey(10));
        assertFalse(threadedCache.containsValue("ten"));
        assertEquals(1, threadedCache.size());
    }

    @Test
    public void testClearLocking() {
        lockingCache.put(1, "one");
        lockingCache.put(2, "two");
        lockingCache.clear();

        assertTrue(lockingCache.isEmpty());
        assertEquals(0, lockingCache.size());
    }

    @Test
    public void testClearThreaded() {
        threadedCache.put(10, "ten");
        threadedCache.put(20, "twenty");
        threadedCache.clear();

        // Give the background thread a moment to notice the clear (no‑op but safe)
        sleep(10);
        assertTrue(threadedCache.isEmpty());
        assertEquals(0, threadedCache.size());
    }

    @Test
    public void testSizeAndIsEmpty() {
        assertTrue(lockingCache.isEmpty());
        assertEquals(0, lockingCache.size());

        lockingCache.put(1, "one");
        assertFalse(lockingCache.isEmpty());
        assertEquals(1, lockingCache.size());

        assertTrue(threadedCache.isEmpty());
        assertEquals(0, threadedCache.size());

        threadedCache.put(10, "ten");
        assertFalse(threadedCache.isEmpty());
        assertEquals(1, threadedCache.size());
    }

    /* ---------------------------------------------------------------------- */
    /*                         LRU EVICTION BEHAVIOR                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCapacityEvictionLocking() {
        // Fill cache to capacity
        lockingCache.put(1, "one");
        lockingCache.put(2, "two");
        lockingCache.put(3, "three");

        // Adding a fourth entry should evict the least‑recently used (key 1)
        lockingCache.put(4, "four");

        assertNull("LRU entry should have been evicted", lockingCache.get(1));
        assertEquals("two", lockingCache.get(2));
        assertEquals("three", lockingCache.get(3));
        assertEquals("four", lockingCache.get(4));
        assertEquals(CAPACITY, lockingCache.size());
    }

    @Test
    public void testCapacityEvictionThreaded() {
        // Fill cache to capacity
        threadedCache.put(10, "ten");
        threadedCache.put(20, "twenty");
        threadedCache.put(30, "thirty");

        // Insert a fourth entry – the cleanup thread will run after CLEANUP_DELAY ms
        threadedCache.put(40, "forty");

        // Wait longer than cleanup delay to allow eviction to happen
        sleep(50);

        // The cache size must be <= capacity
        assertTrue("Cache size should not exceed capacity after cleanup", threadedCache.size() <= CAPACITY);

        // The LRU entry (key 10) is expected to be evicted
        assertNull("LRU entry should have been evicted by threaded strategy", threadedCache.get(10));

        // Verify remaining entries are present
        assertEquals("twenty", threadedCache.get(20));
        assertEquals("thirty", threadedCache.get(30));
        assertEquals("forty", threadedCache.get(40));
    }

    /* ---------------------------------------------------------------------- */
    /*                         COLLECTION VIEWS                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testKeySetAndValuesAndEntrySetLocking() {
        lockingCache.put(1, "one");
        lockingCache.put(2, "two");
        lockingCache.put(3, "three");

        Set<Integer> keys = lockingCache.keySet();
        Collection<String> values = lockingCache.values();
        Set<Map.Entry<Integer, String>> entries = lockingCache.entrySet();

        assertEquals(3, keys.size());
        assertTrue(keys.containsAll(Arrays.asList(1, 2, 3)));

        assertEquals(3, values.size());
        assertTrue(values.containsAll(Arrays.asList("one", "two", "three")));

        assertEquals(3, entries.size());
        Map<Integer, String> reconstructed = new HashMap<>();
        for (Map.Entry<Integer, String> e : entries) {
            reconstructed.put(e.getKey(), e.getValue());
        }
        assertEquals(lockingCache, reconstructed);
    }

    @Test
    public void testKeySetAndValuesAndEntrySetThreaded() {
        threadedCache.put(10, "ten");
        threadedCache.put(20, "twenty");
        threadedCache.put(30, "thirty");

        // Allow possible cleanup to finish
        sleep(20);

        Set<Integer> keys = threadedCache.keySet();
        Collection<String> values = threadedCache.values();
        Set<Map.Entry<Integer, String>> entries = threadedCache.entrySet();

        assertEquals(3, keys.size());
        assertTrue(keys.containsAll(Arrays.asList(10, 20, 30)));

        assertEquals(3, values.size());
        assertTrue(values.containsAll(Arrays.asList("ten", "twenty", "thirty")));

        assertEquals(3, entries.size());
        Map<Integer, String> reconstructed = new HashMap<>();
        for (Map.Entry<Integer, String> e : entries) {
            reconstructed.put(e.getKey(), e.getValue());
        }
        assertEquals(threadedCache, reconstructed);
    }

    /* ---------------------------------------------------------------------- */
    /*                         OBJECT CONTRACT METHODS                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCodeLocking() {
        Map<Integer, String> reference = new HashMap<>();
        reference.put(1, "one");
        reference.put(2, "two");
        reference.put(3, "three");

        lockingCache.putAll(reference);

        assertTrue(lockingCache.equals(reference));
        assertTrue(reference.equals(lockingCache));
        assertEquals(reference.hashCode(), lockingCache.hashCode());
    }

    @Test
    public void testEqualsAndHashCodeThreaded() {
        Map<Integer, String> reference = new HashMap<>();
        reference.put(10, "ten");
        reference.put(20, "twenty");
        reference.put(30, "thirty");

        threadedCache.putAll(reference);
        // Ensure cleanup thread has run (even though we are exactly at capacity)
        sleep(20);

        assertTrue(threadedCache.equals(reference));
        assertTrue(reference.equals(threadedCache));
        assertEquals(reference.hashCode(), threadedCache.hashCode());
    }

    @Test
    public void testToStringLocking() {
        lockingCache.put(1, "one");
        lockingCache.put(2, "two");

        String s = lockingCache.toString();
        assertNotNull(s);
        assertTrue(s.contains("1=one"));
        assertTrue(s.contains("2=two"));
    }

    @Test
    public void testToStringThreaded() {
        threadedCache.put(10, "ten");
        threadedCache.put(20, "twenty");

        // Give background thread a chance to settle
        sleep(20);

        String s = threadedCache.toString();
        assertNotNull(s);
        assertTrue(s.contains("10=ten"));
        assertTrue(s.contains("20=twenty"));
    }

    /* ---------------------------------------------------------------------- */
    /*                         THREADED SHUTDOWN                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testShutdownThreaded() {
        // The shutdown method should be safe to call multiple times without exception.
        threadedCache.shutdown();
        // second call – still safe
        threadedCache.shutdown();
        // No assertions – we just verify that no exception is thrown.
    }

    /* ---------------------------------------------------------------------- */
    /*                         Helper Methods                                   */
    /* ---------------------------------------------------------------------- */

    /** Simple wrapper around Thread.sleep that swallows InterruptedException. */
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
