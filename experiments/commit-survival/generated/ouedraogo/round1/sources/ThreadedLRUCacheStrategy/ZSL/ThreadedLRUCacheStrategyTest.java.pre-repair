package com.cedarsoftware.util.cache;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ThreadedLRUCacheStrategy}.
 *
 * <p>The tests focus on:
 * <ul>
 *   <li>Constructor argument validation.</li>
 *   <li>Basic Map contract (put, get, remove, size, clear, isEmpty, contains…).</li>
 *   <li>LRU eviction order – access updates the timestamp.</li>
 *   <li>Views returned by {@code keySet()}, {@code values()}, {@code entrySet()}.</li>
 *   <li>Equality, hashCode and {@code toString()} handling of self‑references.</li>
 *   <li>Proper shutdown of the shared scheduler.</li>
 * </ul>
 *
 * <p>All timings use a very small {@code cleanupDelayMillis} (20 ms) so the cleanup task finishes quickly.
 * The tests wait a short amount of time (≥ 2 × delay) after operations that trigger cleanup to avoid flakiness.
 */
public class ThreadedLRUCacheStrategyTest
{
    /** Small delay to keep tests fast while still allowing the background cleanup task to run. */
    private static final int CLEANUP_DELAY_MS = 20;

    /** Capacity used for most tests – small enough to force eviction quickly. */
    private static final int CAPACITY = 2;

    private ThreadedLRUCacheStrategy<Integer, String> cache;

    @Before
    public void setUp()
    {
        // Fresh cache for each test method.
        cache = new ThreadedLRUCacheStrategy<>(CAPACITY, CLEANUP_DELAY_MS);
    }

    @AfterClass
    public static void tearDownClass()
    {
        // Ensure the shared scheduler is terminated after the whole test suite.
        ThreadedLRUCacheStrategy.shutdown();
    }

    /* --------------------------------------------------------------------- */
    /* Constructor validation                                                */
    /* --------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsZeroCapacity()
    {
        new ThreadedLRUCacheStrategy<>(0, CLEANUP_DELAY_MS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsNegativeCapacity()
    {
        new ThreadedLRUCacheStrategy<>(-5, CLEANUP_DELAY_MS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsTooSmallDelay()
    {
        new ThreadedLRUCacheStrategy<>(CAPACITY, 5);
    }

    /* --------------------------------------------------------------------- */
    /* Basic Map operations                                                  */
    /* --------------------------------------------------------------------- */

    @Test
    public void testPutGetRemove()
    {
        assertTrue(cache.isEmpty());

        // put returns null for a new key
        assertNull(cache.put(1, "one"));
        assertEquals(1, cache.size());
        assertEquals("one", cache.get(1));

        // update existing key – old value is returned
        assertEquals("one", cache.put(1, "uno"));
        assertEquals("uno", cache.get(1));
        assertEquals(1, cache.size());

        // remove returns the removed value
        assertEquals("uno", cache.remove(1));
        assertNull(cache.get(1));
        assertTrue(cache.isEmpty());

        // removing a non‑existent key returns null
        assertNull(cache.remove(999));
    }

    @Test
    public void testNullKeyAndValue()
    {
        // null key
        assertNull(cache.put(null, "nullKey"));
        assertEquals("nullKey", cache.get(null));
        assertTrue(cache.containsKey(null));

        // null value
        assertNull(cache.put(42, null));
        assertTrue(cache.containsKey(42));
        assertTrue(cache.containsValue(null));
        assertNull(cache.get(42));

        // both null
        assertNull(cache.put(null, null));
        assertNull(cache.get(null));
    }

    @Test
    public void testPutAllAndClear()
    {
        Map<Integer, String> source = new HashMap<>();
        source.put(1, "A");
        source.put(2, "B");
        source.put(3, "C");

        cache.putAll(source);
        // size may be > capacity temporarily – wait for cleanup
        waitForCleanup();

        // after cleanup only the two most‑recently used entries should remain.
        // Insertion order is the only thing that influences timestamps here,
        // so the two latest inserted keys (2 and 3) survive.
        assertEquals(2, cache.size());
        assertFalse(cache.containsKey(1));
        assertTrue(cache.containsKey(2));
        assertTrue(cache.containsKey(3));

        cache.clear();
        assertTrue(cache.isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /* LRU eviction behaviour                                                */
    /* --------------------------------------------------------------------- */

    @Test
    public void testLruEvictionOrder()
    {
        // Fill to capacity
        cache.put(1, "A");   // timestamp T1
        cache.put(2, "B");   // timestamp T2

        // Access key 1 to make it most‑recently used (updates timestamp)
        assertEquals("A", cache.get(1));

        // Insert third entry – should trigger eviction of key 2 (least recent)
        cache.put(3, "C");
        waitForCleanup();

        assertEquals(2, cache.size());
        assertTrue(cache.containsKey(1));
        assertTrue(cache.containsKey(3));
        assertFalse(cache.containsKey(2));
    }

    @Test
    public void testMultipleEvictionsWhenOverCapacity()
    {
        // capacity = 2, insert 4 items quickly
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
        cache.put(4, "four");

        // Wait enough for cleanup to run (it may run twice)
        waitForCleanup();

        // Only the two most‑recently accessed/inserted entries should survive.
        // Since we never accessed any key after insertion, the last two inserted (3 and 4) remain.
        assertEquals(2, cache.size());
        assertFalse(cache.containsKey(1));
        assertFalse(cache.containsKey(2));
        assertTrue(cache.containsKey(3));
        assertTrue(cache.containsKey(4));
    }

    /* --------------------------------------------------------------------- */
    /* Views (keySet, values, entrySet)                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testKeySetValuesEntrySet()
    {
        cache.put(10, "X");
        cache.put(20, "Y");
        waitForCleanup(); // ensure no pending eviction

        Set<Integer> expectedKeys = new HashSet<>(Arrays.asList(10, 20));
        Collection<String> expectedValues = new ArrayList<>(Arrays.asList("X", "Y"));
        Set<Map.Entry<Integer, String>> expectedEntries = new HashSet<>();
        expectedEntries.add(new AbstractMap.SimpleEntry<>(10, "X"));
        expectedEntries.add(new AbstractMap.SimpleEntry<>(20, "Y"));

        assertEquals(expectedKeys, cache.keySet());
        assertEquals(expectedValues, cache.values());
        assertEquals(expectedEntries, cache.entrySet());

        // Verify the returned collections are unmodifiable
        try
        {
            cache.keySet().add(30);
            fail("keySet should be unmodifiable");
        }
        catch (UnsupportedOperationException ignored) { }

        try
        {
            cache.values().add("Z");
            fail("values should be unmodifiable");
        }
        catch (UnsupportedOperationException ignored) { }

        try
        {
            cache.entrySet().remove(expectedEntries.iterator().next());
            fail("entrySet should be unmodifiable");
        }
        catch (UnsupportedOperationException ignored) { }
    }

    /* --------------------------------------------------------------------- */
    /* Equality and hashCode                                                */
    /* --------------------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCode()
    {
        cache.put(1, "one");
        cache.put(2, "two");
        waitForCleanup();

        // Build a plain HashMap with the same entries
        Map<Integer, String> plain = new HashMap<>();
        plain.put(1, "one");
        plain.put(2, "two");

        // Equality with any Map implementation
        assertTrue(cache.equals(plain));
        assertTrue(plain.equals(cache)); // HashMap uses its own equals implementation

        // Same entries, different order – still equal
        ThreadedLRUCacheStrategy<Integer, String> other = new ThreadedLRUCacheStrategy<>(CAPACITY, CLEANUP_DELAY_MS);
        other.put(2, "two");
        other.put(1, "one");
        waitForCleanup();

        assertTrue(cache.equals(other));
        assertEquals(cache.hashCode(), other.hashCode());

        // Different content
        other.put(3, "three");
        waitForCleanup();
        assertFalse(cache.equals(other));
    }

    /* --------------------------------------------------------------------- */
    /* toString – especially self‑reference handling                        */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToStringSimple()
    {
        cache.put(5, "five");
        cache.put(6, "six");
        waitForCleanup();

        String s = cache.toString();
        // The order is not guaranteed because entrySet is built from a ConcurrentSet.
        // Verify that both key‑value pairs appear in the string.
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        assertTrue(s.contains("5=five"));
        assertTrue(s.contains("6=six"));
    }

    @Test
    public void testToStringSelfReference()
    {
        // Use a cache that can hold Object values.
        ThreadedLRUCacheStrategy<Integer, Object> selfCache = new ThreadedLRUCacheStrategy<>(CAPACITY, CLEANUP_DELAY_MS);
        selfCache.put(1, selfCache); // value is the map itself
        waitForCleanup();

        String s = selfCache.toString();
        // Expect the placeholder "(this Map)" for the self reference.
        assertTrue(s.contains("1=(this Map)"));
    }

    /* --------------------------------------------------------------------- */
    /* Helper – wait for the asynchronous cleanup to finish                 */
    /* --------------------------------------------------------------------- */

    /**
     * Waits a little longer than two cleanup cycles. This is sufficient for the
     * background task to have run at least once, even on a heavily loaded CI node.
     */
    private static void waitForCleanup()
    {
        try
        {
            Thread.sleep(CLEANUP_DELAY_MS * 3L);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            fail("Interrupted while waiting for cleanup");
        }
    }
}
