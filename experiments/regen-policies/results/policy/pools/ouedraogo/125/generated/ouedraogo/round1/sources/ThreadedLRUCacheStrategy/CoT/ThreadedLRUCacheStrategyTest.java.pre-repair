package com.cedarsoftware.util.cache;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class ThreadedLRUCacheStrategyTest {

    /*** Helper constants for tests ***/
    private static final int SMALL_CAPACITY = 2;
    private static final int SMALL_DELAY_MS = 20; // short enough for fast tests

    private ThreadedLRUCacheStrategy<String, String> cache;

    @Before
    public void setUp() {
        // Default cache used by most tests – capacity large enough to avoid automatic eviction
        cache = new ThreadedLRUCacheStrategy<>(10, 100);
    }

    @AfterClass
    public static void tearDownClass() {
        // Shut down the shared scheduler once all tests have finished
        ThreadedLRUCacheStrategy.shutdown();
    }

    /*** 1. Constructor validation ***/
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsZeroCapacity() {
        new ThreadedLRUCacheStrategy<>(0, 50);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsNegativeCapacity() {
        new ThreadedLRUCacheStrategy<>(-5, 50);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsTooSmallDelay() {
        new ThreadedLRUCacheStrategy<>(5, 5); // must be >= 10
    }

    /*** 2. Basic put / get functionality ***/
    @Test
    public void testPutAndGet() {
        assertNull(cache.put("key1", "value1"));
        assertEquals("value1", cache.get("key1"));
    }

    @Test
    public void testPutOverwriteReturnsOldValue() {
        cache.put("k", "v1");
        String old = cache.put("k", "v2");
        assertEquals("v1", old);
        assertEquals("v2", cache.get("k"));
    }

    /*** 3. Null key / null value handling ***/
    @Test
    public void testNullKeyAndValue() {
        assertNull(cache.put(null, null));
        assertTrue(cache.containsKey(null));
        assertTrue(cache.containsValue(null));
        assertNull(cache.get(null));
        assertNull(cache.remove(null));
    }

    /*** 4. Removal ***/
    @Test
    public void testRemoveExistingKey() {
        cache.put("a", "alpha");
        String removed = cache.remove("a");
        assertEquals("alpha", removed);
        assertFalse(cache.containsKey("a"));
        assertEquals(0, cache.size());
    }

    @Test
    public void testRemoveNonExistingKeyReturnsNull() {
        assertNull(cache.remove("nonexistent"));
    }

    /*** 5. Clear and isEmpty ***/
    @Test
    public void testClearEmptiesCache() {
        cache.put("x", "X");
        cache.put("y", "Y");
        assertFalse(cache.isEmpty());
        cache.clear();
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
    }

    /*** 6. containsKey / containsValue ***/
    @Test
    public void testContainsKeyAndValue() {
        cache.put("k1", "v1");
        assertTrue(cache.containsKey("k1"));
        assertTrue(cache.containsValue("v1"));
        assertFalse(cache.containsKey("k2"));
        assertFalse(cache.containsValue("v2"));
    }

    /*** 7. entrySet, keySet, values ***/
    @Test
    public void testEntrySetKeySetValuesReflectCacheContent() {
        cache.put("one", "1");
        cache.put("two", "2");

        Set<Map.Entry<String, String>> entrySet = cache.entrySet();
        assertEquals(2, entrySet.size());
        assertTrue(entrySet.contains(new AbstractMap.SimpleEntry<>("one", "1")));
        assertTrue(entrySet.contains(new AbstractMap.SimpleEntry<>("two", "2")));

        Set<String> keySet = cache.keySet();
        assertEquals(2, keySet.size());
        assertTrue(keySet.contains("one"));
        assertTrue(keySet.contains("two"));

        assertEquals(2, cache.values().size());
        assertTrue(cache.values().contains("1"));
        assertTrue(cache.values().contains("2"));
    }

    /*** 8. putAll ***/
    @Test
    public void testPutAllCopiesAllEntries() {
        Map<String, String> source = new HashMap<>();
        source.put("a", "A");
        source.put("b", "B");
        cache.putAll(source);
        assertEquals(2, cache.size());
        assertEquals("A", cache.get("a"));
        assertEquals("B", cache.get("b"));
    }

    @Test
    public void testPutAllWithEmptyMapDoesNothing() {
        cache.putAll(Collections.emptyMap());
        assertEquals(0, cache.size());
    }

    /*** 9. Equality and hashCode ***/
    @Test
    public void testEqualsAndHashCodeWithAnotherCache() {
        ThreadedLRUCacheStrategy<String, String> other = new ThreadedLRUCacheStrategy<>(10, 100);
        cache.put("k", "v");
        other.put("k", "v");
        assertTrue(cache.equals(other));
        assertTrue(other.equals(cache));
        assertEquals(cache.hashCode(), other.hashCode());
    }

    @Test
    public void testEqualsWithPlainMap() {
        Map<String, String> map = new HashMap<>();
        map.put("x", "X");
        cache.put("x", "X");
        assertTrue(cache.equals(map));
        assertTrue(map.equals(cache));
    }

    @Test
    public void testEqualsWithDifferentContentIsFalse() {
        ThreadedLRUCacheStrategy<String, String> other = new ThreadedLRUCacheStrategy<>(10, 100);
        cache.put("k1", "v1");
        other.put("k2", "v2");
        assertFalse(cache.equals(other));
    }

    @Test
    public void testEqualsWithNonMapObjectIsFalse() {
        assertFalse(cache.equals("not a map"));
    }

    /*** 10. toString produces a non‑null representation ***/
    @Test
    public void testToStringContainsEntries() {
        cache.put("foo", "bar");
        String s = cache.toString();
        assertNotNull(s);
        assertTrue(s.contains("foo"));
        assertTrue(s.contains("bar"));
    }

    /*** 11. Capacity eviction (edge case) ***/
    @Test
    public void testEvictionWhenCapacityExceeded() throws InterruptedException {
        // Small cache to force eviction quickly
        ThreadedLRUCacheStrategy<String, String> smallCache =
                new ThreadedLRUCacheStrategy<>(SMALL_CAPACITY, SMALL_DELAY_MS);
        smallCache.put("1", "one");
        smallCache.put("2", "two");
        // Adding third entry should trigger immediate cleanup
        smallCache.put("3", "three");

        // Wait longer than the cleanup delay to allow the background task to run
        Thread.sleep(SMALL_DELAY_MS * 5);

        // After cleanup, size must not exceed capacity
        assertTrue(smallCache.size() <= SMALL_CAPACITY);
        // Verify that the most recently used entries are retained (the exact ones depend on timing)
        // At least one of the first two entries may have been evicted
        int retained = 0;
        if (smallCache.containsKey("1")) retained++;
        if (smallCache.containsKey("2")) retained++;
        if (smallCache.containsKey("3")) retained++;
        assertEquals(SMALL_CAPACITY, retained);
    }

    /*** 12. Immediate cleanup scheduling when size > capacity ***/
    @Test
    public void testScheduleImmediateCleanupIsTriggeredOnPut() throws InterruptedException {
        ThreadedLRUCacheStrategy<String, String> cache = new ThreadedLRUCacheStrategy<>(1, SMALL_DELAY_MS);
        cache.put("first", "1");
        cache.put("second", "2"); // should schedule immediate cleanup

        Thread.sleep(SMALL_DELAY_MS * 5); // give cleanup time to run
        assertEquals(1, cache.size());
        // Only the most recent entry ("second") should survive
        assertTrue(cache.containsKey("second"));
        assertFalse(cache.containsKey("first"));
    }

    /*** 13. Shutdown does not throw ***/
    @Test
    public void testShutdownDoesNotThrow() {
        // Calling shutdown multiple times should be safe
        ThreadedLRUCacheStrategy.shutdown();
        ThreadedLRUCacheStrategy.shutdown(); // no exception expected
    }
}
