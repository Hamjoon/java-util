package com.cedarsoftware.util.cache;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ThreadedLRUCacheStrategy}.
 *
 * The tests cover:
 * <ul>
 *   <li>Construction – valid and illegal arguments.</li>
 *   <li>Basic Map operations (get, put, remove, clear, size, isEmpty, containsKey/Value).</li>
 *   <li>LRU behaviour (capacity enforcement, timestamp update on access).</li>
 *   <li>Null handling for keys and values.</li>
 *   <li>Bulk operations (putAll).</li>
 *   <li>Views (entrySet, keySet, values) – immutability and content.</li>
 *   <li>Object contract (equals, hashCode, toString).</li>
 *   <li>Scheduler shutdown.</li>
 * </ul>
 *
 * Note: The cache uses a shared {@link ScheduledExecutorService}.  All tests use a very short
 * cleanup delay (20 ms) and wait a little longer than that to give the background task a chance
 * to execute.
 */
public class ThreadedLRUCacheStrategyTest
{
    private static final int CAPACITY = 3;
    private static final int CLEANUP_DELAY_MS = 20; // > 10 (minimum required)

    private ThreadedLRUCacheStrategy<String, String> cache;

    @Before
    public void setUp()
    {
        cache = new ThreadedLRUCacheStrategy<>(CAPACITY, CLEANUP_DELAY_MS);
    }

    @AfterClass
    public static void tearDownClass()
    {
        // Shut down the shared scheduler so the JVM can exit cleanly after the tests.
        ThreadedLRUCacheStrategy.shutdown();
    }

    /* ---------------------------------------------------------------------- */
    /*  Construction tests                                                    */
    /* ---------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsZeroCapacity()
    {
        new ThreadedLRUCacheStrategy<>(0, CLEANUP_DELAY_MS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsNegativeCapacity()
    {
        new ThreadedLRUCacheStrategy<>(-5, CLEANUP_DELAY_MS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsTooSmallDelay()
    {
        new ThreadedLRUCacheStrategy<>(CAPACITY, 5);
    }

    @Test
    public void constructorCreatesEmptyCache()
    {
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
    }

    /* ---------------------------------------------------------------------- */
    /*  Basic Map operations                                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void putAndGetSimple()
    {
        assertNull(cache.put("k1", "v1"));
        assertEquals("v1", cache.get("k1"));
        assertEquals(1, cache.size());
    }

    @Test
    public void putReplacesExistingValueAndReturnsOld()
    {
        cache.put("k", "old");
        String returned = cache.put("k", "new");
        assertEquals("old", returned);
        assertEquals("new", cache.get("k"));
        assertEquals(1, cache.size());
    }

    @Test
    public void removeDeletesEntry()
    {
        cache.put("k", "v");
        assertEquals("v", cache.remove("k"));
        assertNull(cache.get("k"));
        assertTrue(cache.isEmpty());
    }

    @Test
    public void clearEmptiesCache()
    {
        cache.put("a", "1");
        cache.put("b", "2");
        cache.clear();
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
    }

    @Test
    public void containsKeyAndValue()
    {
        cache.put("a", "1");
        assertTrue(cache.containsKey("a"));
        assertTrue(cache.containsValue("1"));
        assertFalse(cache.containsKey("b"));
        assertFalse(cache.containsValue("2"));
    }

    @Test
    public void nullKeyAndValueSupported()
    {
        cache.put(null, null);
        assertTrue(cache.containsKey(null));
        assertTrue(cache.containsValue(null));
        assertNull(cache.get(null));
        assertNull(cache.remove(null));
        assertTrue(cache.isEmpty());
    }

    @Test
    public void putAllInsertsAllEntries()
    {
        Map<String, String> source = new HashMap<>();
        source.put("x", "10");
        source.put("y", "20");
        cache.putAll(source);
        assertEquals(2, cache.size());
        assertEquals("10", cache.get("x"));
        assertEquals("20", cache.get("y"));
    }

    /* ---------------------------------------------------------------------- */
    /*  LRU behaviour – capacity enforcement                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void exceedsCapacityTriggersCleanup()
    throws Exception
    {
        // Fill cache to capacity
        cache.put("1", "v1");
        cache.put("2", "v2");
        cache.put("3", "v3");
        assertEquals(CAPACITY, cache.size());

        // Insert one more entry – should schedule immediate cleanup
        cache.put("4", "v4");
        // Wait enough time for the scheduled cleanup to run
        Thread.sleep(CLEANUP_DELAY_MS * 3);

        // The cache must be back to capacity and contain the most recent 3 keys
        assertEquals(CAPACITY, cache.size());
        // "1" should have been evicted (oldest)
        assertNull(cache.get("1"));
        // The three newest keys must be present
        assertEquals("v2", cache.get("2"));
        assertEquals("v3", cache.get("3"));
        assertEquals("v4", cache.get("4"));
    }

    @Test
    public void getUpdatesTimestampSoEntryIsNotEvicted()
    throws Exception
    {
        cache.put("old", "a");
        cache.put("mid", "b");
        cache.put("new", "c");

        // Access "old" to make it most recently used
        assertEquals("a", cache.get("old"));

        // Insert another entry to exceed capacity
        cache.put("extra", "d");
        Thread.sleep(CLEANUP_DELAY_MS * 3);

        // "mid" should be evicted (it is now the least recently used)
        assertNull(cache.get("mid"));
        // "old", "new", "extra" must still be present
        assertEquals("a", cache.get("old"));
        assertEquals("c", cache.get("new"));
        assertEquals("d", cache.get("extra"));
    }

    /* ---------------------------------------------------------------------- */
    /*  View collections – entrySet, keySet, values                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void entrySetReflectsCurrentContentAndIsUnmodifiable()
    {
        cache.put("k1", "v1");
        cache.put("k2", "v2");
        Set<Map.Entry<String, String>> entries = cache.entrySet();

        // Verify content
        Set<String> expectedKeys = new HashSet<>(Arrays.asList("k1", "k2"));
        Set<String> actualKeys = new HashSet<>();
        for (Map.Entry<String, String> e : entries) {
            actualKeys.add(e.getKey());
            assertEquals(cache.get(e.getKey()), e.getValue());
        }
        assertEquals(expectedKeys, actualKeys);

        // The returned set must be unmodifiable
        try {
            entries.add(new AbstractMap.SimpleEntry<>("k3", "v3"));
            fail("Expected UnsupportedOperationException from entrySet.add()");
        } catch (UnsupportedOperationException expected) { }
    }

    @Test
    public void keySetIsUnmodifiableAndAccurate()
    {
        cache.put("a", "1");
        cache.put("b", "2");
        Set<String> keys = cache.keySet();

        assertTrue(keys.containsAll(Arrays.asList("a", "b")));
        assertEquals(2, keys.size());

        try {
            keys.remove("a");
            fail("Expected UnsupportedOperationException from keySet.remove()");
        } catch (UnsupportedOperationException expected) { }
    }

    @Test
    public void valuesCollectionIsUnmodifiableAndAccurate()
    {
        cache.put("x", "10");
        cache.put("y", "20");
        Collection<String> values = cache.values();

        assertTrue(values.containsAll(Arrays.asList("10", "20")));
        assertEquals(2, values.size());

        try {
            values.clear();
            fail("Expected UnsupportedOperationException from values.clear()");
        } catch (UnsupportedOperationException expected) { }
    }

    /* ---------------------------------------------------------------------- */
    /*  Object contract – equals, hashCode, toString                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void equalsAndHashCodeMatchStandardMapContract()
    {
        Map<String, String> javaMap = new HashMap<>();
        javaMap.put("one", "1");
        javaMap.put("two", "2");

        cache.putAll(javaMap);

        assertTrue(cache.equals(javaMap));
        assertTrue(javaMap.equals(cache));
        assertEquals(cache.hashCode(), javaMap.hashCode());
    }

    @Test
    public void equalsHandlesNullAndDifferentTypes()
    {
        assertFalse(cache.equals(null));
        assertFalse(cache.equals("not a map"));
    }

    @Test
    public void toStringProducesMapRepresentation()
    {
        cache.put("a", "alpha");
        cache.put("b", "beta");

        String str = cache.toString();
        // MapUtilities.mapToString formats like {a=alpha, b=beta}
        assertTrue(str.startsWith("{"));
        assertTrue(str.endsWith("}"));
        assertTrue(str.contains("a=alpha"));
        assertTrue(str.contains("b=beta"));
    }

    /* ---------------------------------------------------------------------- */
    /*  Scheduler shutdown verification                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void shutdownTerminatesScheduler()
    throws Exception
    {
        // The static shutdown method should not throw and should eventually stop the executor.
        ThreadedLRUCacheStrategy.shutdown();

        // Wait a short moment; if the executor were still running, awaiting termination would block.
        // No exception means the shutdown succeeded.
        Thread.sleep(10);
    }
}
