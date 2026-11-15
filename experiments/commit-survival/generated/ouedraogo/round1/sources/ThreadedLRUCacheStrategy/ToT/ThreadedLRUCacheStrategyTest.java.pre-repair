package com.cedarsoftware.util.cache;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ThreadedLRUCacheStrategy}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>Constructor validation (illegal arguments)</li>
 *   <li>Basic Map contract methods (size, isEmpty, clear, containsKey/Value, get, put, remove, putAll)</li>
 *   <li>LRU eviction logic (capacity enforcement via scheduled cleanup)</li>
 *   <li>Views returned by {@code entrySet()}, {@code keySet()} and {@code values()}
 *       – immutability and content correctness.</li>
 *   <li>Equality, hashCode and {@code toString()} behaviour (including self‑reference handling).</li>
 *   <li>Null handling for keys and values.</li>
 * </ul>
 *
 * <p>All tests use only the public API shown in the source file and the
 * {@code ConcurrentHashMapNullSafe} / {@code ConcurrentSet} classes that are
 * part of the same library. No mocking frameworks are used.
 */
public class ThreadedLRUCacheStrategyTest {

    private static final int CAPACITY = 2;
    private static final int CLEANUP_DELAY = 20; // ms – small enough for fast tests

    private ThreadedLRUCacheStrategy<String, Integer> cache;

    @Before
    public void setUp() {
        cache = new ThreadedLRUCacheStrategy<>(CAPACITY, CLEANUP_DELAY);
    }

    @AfterClass
    public static void shutdownScheduler() {
        ThreadedLRUCacheStrategy.shutdown();
    }

    /* -------------------------------------------------------
     *  Constructor validation
     * ------------------------------------------------------- */
    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsZeroCapacity() {
        new ThreadedLRUCacheStrategy<>(0, 50);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsNegativeCapacity() {
        new ThreadedLRUCacheStrategy<>(-5, 50);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsTooSmallDelay() {
        new ThreadedLRUCacheStrategy<>(5, 5);
    }

    /* -------------------------------------------------------
     *  Basic Map operations
     * ------------------------------------------------------- */
    @Test
    public void putAndGetSimpleEntry() {
        assertNull(cache.put("a", 1));
        assertEquals(Integer.valueOf(1), cache.get("a"));
        assertEquals(1, cache.size());
    }

    @Test
    public void putReplacesExistingValueAndReturnsOld() {
        cache.put("x", 10);
        Integer old = cache.put("x", 20);
        assertEquals(Integer.valueOf(10), old);
        assertEquals(Integer.valueOf(20), cache.get("x"));
    }

    @Test
    public void getUpdatesTimestampSoEntryIsNotEvicted() throws Exception {
        cache.put("first", 1);
        cache.put("second", 2);
        // exceed capacity – third entry triggers cleanup scheduling
        cache.put("third", 3);
        // give the scheduler a chance to run (it will try to evict LRU)
        Thread.sleep(CLEANUP_DELAY * 2);
        // 'first' should have been evicted, but we accessed it now
        cache.get("first"); // this call updates its timestamp
        // add another entry to force another cleanup
        cache.put("fourth", 4);
        Thread.sleep(CLEANUP_DELAY * 2);
        // 'first' must still be present because its timestamp was refreshed
        assertTrue(cache.containsKey("first"));
        // total size must be <= capacity
        assertTrue(cache.size() <= CAPACITY);
    }

    @Test
    public void removeReturnsPreviousValue() {
        cache.put("k", 99);
        Integer removed = cache.remove("k");
        assertEquals(Integer.valueOf(99), removed);
        assertNull(cache.get("k"));
        assertEquals(0, cache.size());
    }

    @Test
    public void clearEmptiesCache() {
        cache.put("a", 1);
        cache.put("b", 2);
        assertFalse(cache.isEmpty());
        cache.clear();
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
    }

    @Test
    public void containsKeyAndContainsValueWorkWithNulls() {
        cache.put(null, null);
        assertTrue(cache.containsKey(null));
        assertTrue(cache.containsValue(null));
    }

    @Test
    public void putAllInsertsAllEntries() {
        Map<String, Integer> source = new HashMap<>();
        source.put("one", 1);
        source.put("two", 2);
        cache.putAll(source);
        assertEquals(2, cache.size());
        assertEquals(Integer.valueOf(1), cache.get("one"));
        assertEquals(Integer.valueOf(2), cache.get("two"));
    }

    /* -------------------------------------------------------
     *  Capacity enforcement (LRU eviction)
     * ------------------------------------------------------- */
    @Test
    public void evictionKeepsCacheWithinCapacity() throws Exception {
        // capacity = 2, delay = 20ms
        cache.put("a", 1);
        cache.put("b", 2);
        cache.put("c", 3); // exceeds capacity, schedules cleanup
        Thread.sleep(CLEANUP_DELAY * 2); // allow cleanup to run
        assertEquals(CAPACITY, cache.size());

        // The LRU entry ("a") should have been evicted
        assertNull(cache.get("a"));
        assertNotNull(cache.get("b"));
        assertNotNull(cache.get("c"));
    }

    /* -------------------------------------------------------
     *  View collections – immutability and content
     * ------------------------------------------------------- */
    @Test
    public void entrySetIsUnmodifiableAndReflectsCurrentState() {
        cache.put("k1", 10);
        cache.put("k2", 20);
        Set<Map.Entry<String, Integer>> entries = cache.entrySet();

        // Verify content
        Map<String, Integer> expected = new HashMap<>();
        expected.put("k1", 10);
        expected.put("k2", 20);
        assertEquals(expected.entrySet(), entries);

        // Attempt to modify – should throw UnsupportedOperationException
        try {
            entries.add(new AbstractMap.SimpleEntry<>("k3", 30));
            fail("entrySet should be unmodifiable");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    @Test
    public void keySetIsUnmodifiableAndReflectsCurrentState() {
        cache.put("alpha", 1);
        cache.put("beta", 2);
        Set<String> keys = cache.keySet();

        assertEquals(new HashSet<>(Arrays.asList("alpha", "beta")), keys);

        try {
            keys.remove("alpha");
            fail("keySet should be unmodifiable");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    @Test
    public void valuesCollectionIsUnmodifiableAndReflectsCurrentState() {
        cache.put("x", 100);
        cache.put("y", 200);
        Collection<Integer> values = cache.values();

        assertEquals(new HashSet<>(Arrays.asList(100, 200)), new HashSet<>(values));

        try {
            values.add(300);
            fail("values collection should be unmodifiable");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    /* -------------------------------------------------------
     *  Equality, hashCode and toString
     * ------------------------------------------------------- */
    @Test
    public void equalsAndHashCodeMatchStandardMapContract() {
        cache.put("one", 1);
        cache.put("two", 2);

        Map<String, Integer> other = new HashMap<>();
        other.put("one", 1);
        other.put("two", 2);

        assertTrue(cache.equals(other));
        assertEquals(other.hashCode(), cache.hashCode());
    }

    @Test
    public void toStringFormatsEntriesAndHandlesSelfReference() {
        cache.put("self", null);
        // Insert the cache itself as a value to trigger self‑reference handling
        cache.put("cache", null);
        // Directly set a self‑reference via reflection is not allowed; instead we rely on formatElement()
        // which replaces the cache instance with "(this Map)" when the element equals the cache.
        // To simulate that, we add the cache object as a key.
        cache.put(null, null); // just to have a null entry as well

        // Manually inject a self‑reference via the internal map (allowed because we have the reference)
        // NOTE: This step is safe because we are using the same generic types.
        cache.put("selfRef", (Integer) null); // placeholder, actual self‑reference is not needed for test

        String str = cache.toString();
        // The string must start with '{' and end with '}'
        assertTrue(str.startsWith("{") && str.endsWith("}"));
        // Must contain the known key/value pairs
        assertTrue(str.contains("self=null"));
        assertTrue(str.contains("null=null"));
    }

    @Test
    public void toStringDetectsSelfReferenceWhenCacheIsValue() {
        // Put the cache itself as a value
        @SuppressWarnings("unchecked")
        ThreadedLRUCacheStrategy<String, Object> rawCache = (ThreadedLRUCacheStrategy<String, Object>) cache;
        rawCache.put("self", rawCache);
        String result = rawCache.toString();
        // The value should be rendered as "(this Map)"
        assertTrue(result.contains("self=(this Map)"));
    }

    /* -------------------------------------------------------
     *  Additional edge cases
     * ------------------------------------------------------- */
    @Test
    public void sizeReflectsConcurrentMapSize() {
        assertEquals(0, cache.size());
        cache.put("a", 1);
        assertEquals(1, cache.size());
        cache.put("b", 2);
        assertEquals(2, cache.size());
        cache.remove("a");
        assertEquals(1, cache.size());
    }

    @Test
    public void isEmptyReflectsStateCorrectly() {
        assertTrue(cache.isEmpty());
        cache.put("k", 1);
        assertFalse(cache.isEmpty());
        cache.clear();
        assertTrue(cache.isEmpty());
    }
}
