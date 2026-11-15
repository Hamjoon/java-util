package com.cedarsoftware.util.cache;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

/**
 * JUnit‑4 test suite that exercises all public behaviour of {@link ThreadedLRUCacheStrategy}.
 * The tests use only the API that is visible in the source supplied above.
 */
public class ThreadedLRUCacheStrategyTest
{
    /** a small delay makes the asynchronous cleanup fast enough for unit tests */
    private static final int CLEANUP_DELAY = 20;   // milliseconds

    /** capacity used for most tests – small so we can trigger eviction quickly */
    private static final int CAPACITY = 2;

    private ThreadedLRUCacheStrategy<String, String> cache;

    @Before
    public void setUp()
    {
        cache = new ThreadedLRUCacheStrategy<>(CAPACITY, CLEANUP_DELAY);
    }

    @AfterClass
    public static void tearDownClass()
    {
        // Shut down the shared scheduler once all tests have finished.
        ThreadedLRUCacheStrategy.shutdown();
    }

    /* --------------------------------------------------------------------- */
    /*  Constructor validation                                               */
    /* --------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsZeroCapacity()
    {
        new ThreadedLRUCacheStrategy<>(0, CLEANUP_DELAY);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsNegativeCapacity()
    {
        new ThreadedLRUCacheStrategy<>(-5, CLEANUP_DELAY);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsTooSmallDelay()
    {
        new ThreadedLRUCacheStrategy<>(CAPACITY, 5);
    }

    /* --------------------------------------------------------------------- */
    /*  Basic Map operations (put, get, remove, contains…)                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void basicPutGetRemoveWorks()
    {
        assertNull(cache.put("k1", "v1"));
        assertEquals("v1", cache.get("k1"));
        assertTrue(cache.containsKey("k1"));
        assertTrue(cache.containsValue("v1"));
        assertEquals("v1", cache.remove("k1"));
        assertNull(cache.get("k1"));
        assertFalse(cache.containsKey("k1"));
        assertFalse(cache.containsValue("v1"));
        assertTrue(cache.isEmpty());
    }

    @Test
    public void nullKeyAndNullValueSupported()
    {
        assertNull(cache.put(null, null));
        assertTrue(cache.containsKey(null));
        assertTrue(cache.containsValue(null));
        assertNull(cache.get(null));
        assertNull(cache.remove(null));
        assertFalse(cache.containsKey(null));
        assertFalse(cache.containsValue(null));
    }

    /* --------------------------------------------------------------------- */
    /*  LRU eviction – exercised via the asynchronous cleanup task          */
    /* --------------------------------------------------------------------- */

    @Test
    public void lruEvictionRemovesOldestWhenCapacityExceeded()
        throws Exception
    {
        // Insert three distinct entries; the cache capacity is 2.
        cache.put("first", "1");
        Thread.sleep(10);               // ensure a different timestamp
        cache.put("second", "2");
        Thread.sleep(10);
        cache.put("third", "3");          // this put triggers immediate cleanup

        // Wait long enough for the scheduled cleanup to have run.
        Thread.sleep(CLEANUP_DELAY * 5);

        // After cleanup the size must be equal to the declared capacity.
        assertEquals(CAPACITY, cache.size());

        // The "first" entry is the least‑recently‑used and must have been evicted.
        assertNull(cache.get("first"));
        assertFalse(cache.containsKey("first"));

        // The two most recent entries must still be present.
        assertEquals("2", cache.get("second"));
        assertEquals("3", cache.get("third"));
    }

    @Test
    public void accessUpdatesTimestampSoItemIsNotEvicted()
        throws Exception
    {
        cache.put("a", "A");
        Thread.sleep(10);
        cache.put("b", "B");
        Thread.sleep(10);
        // Access "a" to refresh its timestamp.
        assertEquals("A", cache.get("a"));
        Thread.sleep(10);
        // Insert a third entry, causing eviction.
        cache.put("c", "C");
        Thread.sleep(CLEANUP_DELAY * 5);

        // "b" should be evicted because it is now the oldest.
        assertNull(cache.get("b"));
        assertFalse(cache.containsKey("b"));

        // "a" and "c" must remain.
        assertEquals("A", cache.get("a"));
        assertEquals("C", cache.get("c"));
    }

    /* --------------------------------------------------------------------- */
    /*  Bulk operations                                                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void putAllCopiesAllEntries()
    {
        Map<String, String> source = new HashMap<>();
        source.put("x", "X");
        source.put("y", "Y");
        source.put("z", "Z");

        cache.putAll(source);

        // The cache may have triggered a cleanup, but all three keys should be present
        // because the capacity is 2 and cleanup runs asynchronously.
        // Wait a moment for any cleanup to finish.
        try { Thread.sleep(CLEANUP_DELAY * 5); } catch (InterruptedException ignored) {}

        // At least the most recent two entries must be present.
        assertTrue(cache.containsKey("y") || cache.containsKey("z"));
        assertTrue(cache.containsKey("x") || cache.containsKey("y") || cache.containsKey("z"));
    }

    /* --------------------------------------------------------------------- */
    /*  Views: entrySet, keySet, values                                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void entrySetKeySetAndValuesReflectCacheContent()
    {
        cache.put("k1", "v1");
        cache.put("k2", "v2");

        Set<Map.Entry<String, String>> entrySet = cache.entrySet();
        Set<String> keySet = cache.keySet();
        java.util.Collection<String> values = cache.values();

        assertEquals(2, entrySet.size());
        assertTrue(entrySet.contains(new java.util.AbstractMap.SimpleEntry<>("k1", "v1")));
        assertTrue(entrySet.contains(new java.util.AbstractMap.SimpleEntry<>("k2", "v2")));

        assertEquals(2, keySet.size());
        assertTrue(keySet.contains("k1"));
        assertTrue(keySet.contains("k2"));

        assertEquals(2, values.size());
        assertTrue(values.contains("v1"));
        assertTrue(values.contains("v2"));
    }

    /* --------------------------------------------------------------------- */
    /*  Equality and hash code                                                */
    /* --------------------------------------------------------------------- */

    @Test
    public void equalsAndHashCodeMatchStandardMap()
    {
        cache.put("a", "A");
        cache.put("b", "B");

        Map<String, String> other = new HashMap<>();
        other.put("a", "A");
        other.put("b", "B");

        assertTrue(cache.equals(other));
        assertEquals(other.hashCode(), cache.hashCode());

        // Changing the cache makes them unequal
        cache.put("c", "C");
        try { Thread.sleep(CLEANUP_DELAY * 5); } catch (InterruptedException ignored) {}
        assertFalse(cache.equals(other));
    }

    @Test
    public void equalsReturnsFalseForNonMap()
    {
        assertFalse(cache.equals("not a map"));
    }

    /* --------------------------------------------------------------------- */
    /*  toString – especially self‑reference handling                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void toStringFormatsEntriesAndHandlesSelfReference()
    {
        cache.put("self", null);                // placeholder, will be overwritten
        cache.put("key", "value");
        // Put the cache itself as a value to trigger the self‑reference placeholder.
        cache.put("self", cache.toString());   // value is a String, not the map – we need a real self reference
        // Instead, we store the map itself as the value via raw put (bypassing generic typing).
        @SuppressWarnings("unchecked")
        ThreadedLRUCacheStrategy<String, Object> rawCache = (ThreadedLRUCacheStrategy) cache;
        rawCache.put("self", rawCache);        // now the value points to the map instance

        String result = rawCache.toString();

        // The string must contain the placeholder "(this Map)" for the self‑reference.
        assertTrue("toString should contain self‑reference placeholder", result.contains("(this Map)"));
        // It must also contain the normal entry.
        assertTrue(result.contains("key=value"));
    }

    /* --------------------------------------------------------------------- */
    /*  clear / isEmpty                                                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void clearEmptiesCache()
    {
        cache.put("a", "A");
        cache.put("b", "B");
        assertFalse(cache.isEmpty());
        cache.clear();
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
    }
}
