package com.cedarsoftware.util.cache;

import org.junit.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ThreadedLRUCacheStrategy}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public API methods (constructor, Map methods, static shutdown)</li>
 *   <li>Typical usage scenarios</li>
 *   <li>Edge cases (null keys/values, capacity limits, eviction, unmodifiable collections)</li>
 *   <li>Exception handling (illegal arguments, wrong type in equals)</li>
 * </ul>
 *
 * Note: The cache uses a shared {@code ScheduledExecutorService}.  The cleanup delay is set to the minimum
 * allowed value (10 ms) so that tests can reliably wait for the background eviction task to run.
 */
public class ThreadedLRUCacheStrategyTest {

    private static final int CAPACITY = 2;
    private static final int CLEANUP_DELAY = 10; // milliseconds – the minimum allowed

    private ThreadedLRUCacheStrategy<String, Integer> cache;

    @Before
    public void setUp() {
        cache = new ThreadedLRUCacheStrategy<>(CAPACITY, CLEANUP_DELAY);
    }

    @After
    public void tearDown() {
        cache.clear();
    }

    @AfterClass
    public static void shutdownScheduler() {
        // Shut down the shared scheduler once all tests are finished.
        ThreadedLRUCacheStrategy.shutdown();
    }

    /* ----------------------------------------------------------------------
     * Constructor validation
     * ---------------------------------------------------------------------- */
    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsZeroCapacity() {
        new ThreadedLRUCacheStrategy<>(0, CLEANUP_DELAY);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsNegativeCapacity() {
        new ThreadedLRUCacheStrategy<>(-5, CLEANUP_DELAY);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsTooSmallCleanupDelay() {
        new ThreadedLRUCacheStrategy<>(CAPACITY, 5);
    }

    /* ----------------------------------------------------------------------
     * Basic Map operations
     * ---------------------------------------------------------------------- */
    @Test
    public void putAndGetBasic() {
        assertNull(cache.put("one", 1));
        assertEquals(Integer.valueOf(1), cache.get("one"));
        assertEquals(1, cache.size());
    }

    @Test
    public void putReplacesExistingValueAndReturnsOld() {
        cache.put("key", 10);
        Integer old = cache.put("key", 20);
        assertEquals(Integer.valueOf(10), old);
        assertEquals(Integer.valueOf(20), cache.get("key"));
    }

    @Test
    public void getReturnsNullForMissingKey() {
        assertNull(cache.get("absent"));
    }

    @Test
    public void removeExistingKey() {
        cache.put("a", 1);
        Integer removed = cache.remove("a");
        assertEquals(Integer.valueOf(1), removed);
        assertFalse(cache.containsKey("a"));
        assertNull(cache.get("a"));
    }

    @Test
    public void removeNonExistingKeyReturnsNull() {
        assertNull(cache.remove("nothing"));
    }

    @Test
    public void containsKeyAndValue() {
        cache.put("x", 100);
        assertTrue(cache.containsKey("x"));
        assertTrue(cache.containsValue(100));
        assertFalse(cache.containsKey("y"));
        assertFalse(cache.containsValue(200));
    }

    @Test
    public void isEmptyAndClear() {
        assertTrue(cache.isEmpty());
        cache.put("k", 1);
        assertFalse(cache.isEmpty());
        cache.clear();
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
    }

    @Test
    public void sizeReflectsNumberOfEntries() {
        assertEquals(0, cache.size());
        cache.put("a", 1);
        cache.put("b", 2);
        assertEquals(2, cache.size());
    }

    @Test
    public void putAllAddsAllEntries() {
        Map<String, Integer> source = new HashMap<>();
        source.put("one", 1);
        source.put("two", 2);
        cache.putAll(source);
        assertEquals(2, cache.size());
        assertEquals(Integer.valueOf(1), cache.get("one"));
        assertEquals(Integer.valueOf(2), cache.get("two"));
    }

    /* ----------------------------------------------------------------------
     * Null handling (both key and value may be null)
     * ---------------------------------------------------------------------- */
    @Test
    public void putAndGetNullKey() {
        cache.put(null, 42);
        assertTrue(cache.containsKey(null));
        assertEquals(Integer.valueOf(42), cache.get(null));
        assertTrue(cache.containsValue(42));
    }

    @Test
    public void putAndGetNullValue() {
        cache.put("nullValue", null);
        assertTrue(cache.containsKey("nullValue"));
        assertTrue(cache.containsValue(null));
        assertNull(cache.get("nullValue"));
    }

    @Test
    public void putNullKeyAndNullValue() {
        cache.put(null, null);
        assertTrue(cache.containsKey(null));
        assertTrue(cache.containsValue(null));
        assertNull(cache.get(null));
    }

    /* ----------------------------------------------------------------------
     * LRU eviction behaviour
     * ---------------------------------------------------------------------- */
    @Test
    public void evictionWhenCapacityExceeded() throws InterruptedException {
        // Fill to capacity
        cache.put("first", 1);
        cache.put("second", 2);
        assertEquals(2, cache.size());

        // Access "first" to make it most‑recently used
        cache.get("first");

        // Add third entry – should trigger async cleanup
        cache.put("third", 3);

        // Wait enough time for the scheduled cleanup to run
        TimeUnit.MILLISECONDS.sleep(CLEANUP_DELAY * 3);

        // Verify that the least‑recently used ("second") was evicted
        assertEquals(2, cache.size());
        assertTrue(cache.containsKey("first"));
        assertTrue(cache.containsKey("third"));
        assertFalse(cache.containsKey("second"));
    }

    @Test
    public void immediateCleanupIsScheduledWhenOverCapacity() throws InterruptedException {
        // Insert three items quickly; the third insertion should schedule an immediate cleanup
        cache.put("a", 1);
        cache.put("b", 2);
        cache.put("c", 3); // over capacity

        // Wait for the cleanup task to execute
        TimeUnit.MILLISECONDS.sleep(CLEANUP_DELAY * 3);

        // Exactly two items must remain
        assertEquals(2, cache.size());
        // The LRU (which is "a") must have been removed
        assertFalse(cache.containsKey("a"));
    }

    /* ----------------------------------------------------------------------
     * Collection view tests (entrySet, keySet, values)
     * ---------------------------------------------------------------------- */
    @Test
    public void entrySetReflectsCurrentMappingsAndIsUnmodifiable() {
        cache.put("k1", 10);
        cache.put("k2", 20);
        Set<Map.Entry<String, Integer>> entries = cache.entrySet();

        // Verify content
        Map<String, Integer> mapFromEntries = new HashMap<>();
        for (Map.Entry<String, Integer> e : entries) {
            mapFromEntries.put(e.getKey(), e.getValue());
        }
        assertEquals(2, mapFromEntries.size());
        assertEquals(Integer.valueOf(10), mapFromEntries.get("k1"));
        assertEquals(Integer.valueOf(20), mapFromEntries.get("k2"));

        // Verify unmodifiable
        try {
            entries.add(new AbstractMap.SimpleEntry<>("x", 99));
            fail("entrySet should be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    @Test
    public void keySetReflectsCurrentKeysAndIsUnmodifiable() {
        cache.put("alpha", 1);
        cache.put("beta", 2);
        Set<String> keys = cache.keySet();

        assertTrue(keys.containsAll(Arrays.asList("alpha", "beta")));
        assertEquals(2, keys.size());

        try {
            keys.remove("alpha");
            fail("keySet should be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    @Test
    public void valuesReflectCurrentValuesAndIsUnmodifiable() {
        cache.put("x", 100);
        cache.put("y", 200);
        Collection<Integer> values = cache.values();

        assertTrue(values.containsAll(Arrays.asList(100, 200)));
        assertEquals(2, values.size());

        try {
            values.add(300);
            fail("values collection should be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    /* ----------------------------------------------------------------------
     * equals, hashCode and toString
     * ---------------------------------------------------------------------- */
    @Test
    public void equalsAndHashCodeAgainstAnotherMap() {
        cache.put("a", 1);
        cache.put("b", 2);

        Map<String, Integer> other = new HashMap<>();
        other.put("a", 1);
        other.put("b", 2);

        assertTrue(cache.equals(other));
        assertEquals(other.hashCode(), cache.hashCode());

        // Change cache – equality should break
        cache.put("c", 3);
        // Wait for eviction so size returns to 2 (capacity)
        try { TimeUnit.MILLISECONDS.sleep(CLEANUP_DELAY * 3); } catch (InterruptedException ignored) {}
        assertTrue(cache.equals(other)); // still equal because the LRU entry was evicted
    }

    @Test
    public void equalsReturnsFalseForNonMap() {
        assertFalse(cache.equals("not a map"));
    }

    @Test
    public void toStringProducesNonNullString() {
        cache.put("one", 1);
        String s = cache.toString();
        assertNotNull(s);
        assertTrue(s.contains("one"));
        assertTrue(s.contains("1"));
    }

    /* ----------------------------------------------------------------------
     * Static shutdown behaviour – ensure it does not throw
     * ---------------------------------------------------------------------- */
    @Test
    public void shutdownDoesNotThrow() {
        // The @AfterClass method already calls shutdown; this test just ensures the call is safe.
        ThreadedLRUCacheStrategy.shutdown();
    }
}
