package com.cedarsoftware.util.cache;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class ThreadedLRUCacheStrategyTest {

    // A small cleanup delay makes the asynchronous eviction fast enough for unit tests
    private static final int CLEANUP_DELAY_MS = 20;

    @Before
    public void setUp() {
        // Ensure the cache is empty before each test (the cache instance is created per test)
    }

    @AfterClass
    public static void tearDownClass() {
        // Shut down the shared scheduler to avoid thread‑leak warnings after the test suite finishes
        ThreadedLRUCacheStrategy.shutdown();
    }

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
        new ThreadedLRUCacheStrategy<>(5, 5); // delay must be >=10
    }

    @Test
    public void testBasicPutGetRemove() {
        ThreadedLRUCacheStrategy<String, String> cache = new ThreadedLRUCacheStrategy<>(5, CLEANUP_DELAY_MS);

        assertTrue(cache.isEmpty());
        assertNull(cache.put("k1", "v1"));
        assertEquals(1, cache.size());

        // get should return the value and update the timestamp
        assertEquals("v1", cache.get("k1"));
        assertTrue(cache.containsKey("k1"));
        assertTrue(cache.containsValue("v1"));

        // replace existing key
        assertEquals("v1", cache.put("k1", "v2"));
        assertEquals("v2", cache.get("k1"));
        assertEquals(1, cache.size());

        // remove
        assertEquals("v2", cache.remove("k1"));
        assertFalse(cache.containsKey("k1"));
        assertTrue(cache.isEmpty());
    }

    @Test
    public void testNullKeyAndValue() {
        ThreadedLRUCacheStrategy<String, String> cache = new ThreadedLRUCacheStrategy<>(3, CLEANUP_DELAY_MS);

        assertNull(cache.put(null, "nullKey"));
        assertEquals("nullKey", cache.get(null));
        assertTrue(cache.containsKey(null));
        assertTrue(cache.containsValue("nullKey"));

        assertNull(cache.put("nullValue", null));
        assertNull(cache.get("nullValue"));
        assertTrue(cache.containsKey("nullValue"));
        assertTrue(cache.containsValue(null));

        // replace null key with null value
        assertEquals("nullKey", cache.put(null, null));
        assertNull(cache.get(null));
        assertTrue(cache.containsKey(null));
        assertTrue(cache.containsValue(null));
    }

    @Test
    public void testPutAllAndBulkViews() {
        ThreadedLRUCacheStrategy<String, Integer> cache = new ThreadedLRUCacheStrategy<>(10, CLEANUP_DELAY_MS);
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);
        source.put("c", 3);

        cache.putAll(source);
        assertEquals(3, cache.size());
        assertTrue(cache.keySet().containsAll(source.keySet()));
        assertTrue(cache.values().containsAll(source.values()));
        assertTrue(cache.entrySet().containsAll(source.entrySet()));
    }

    @Test
    public void testEntrySetKeySetValuesAreUnmodifiable() {
        ThreadedLRUCacheStrategy<String, String> cache = new ThreadedLRUCacheStrategy<>(2, CLEANUP_DELAY_MS);
        cache.put("x", "X");
        cache.put("y", "Y");

        Set<Map.Entry<String, String>> entrySet = cache.entrySet();
        Set<String> keySet = cache.keySet();
        Collection<String> values = cache.values();

        try {
            entrySet.add(new AbstractMap.SimpleEntry<>("z", "Z"));
            fail("entrySet should be unmodifiable");
        } catch (UnsupportedOperationException ignored) {}

        try {
            keySet.add("z");
            fail("keySet should be unmodifiable");
        } catch (UnsupportedOperationException ignored) {}

        try {
            values.add("Z");
            fail("values collection should be unmodifiable");
        } catch (UnsupportedOperationException ignored) {}
    }

    @Test
    public void testEqualsAndHashCode() {
        ThreadedLRUCacheStrategy<String, String> cache1 = new ThreadedLRUCacheStrategy<>(5, CLEANUP_DELAY_MS);
        ThreadedLRUCacheStrategy<String, String> cache2 = new ThreadedLRUCacheStrategy<>(5, CLEANUP_DELAY_MS);
        Map<String, String> map = new HashMap<>();

        cache1.put("k1", "v1");
        cache1.put("k2", "v2");
        cache2.put("k1", "v1");
        cache2.put("k2", "v2");
        map.put("k1", "v1");
        map.put("k2", "v2");

        assertEquals(cache1, cache2);
        assertEquals(cache1.hashCode(), cache2.hashCode());
        assertEquals(cache1, map);
        assertEquals(map, cache1);
        assertEquals(cache1.hashCode(), map.hashCode());

        cache2.put("k3", "v3");
        assertNotEquals(cache1, cache2);
        assertNotEquals(cache1, Collections.emptyMap());
    }

    @Test
    public void testToStringContainsEntries() {
        ThreadedLRUCacheStrategy<String, Integer> cache = new ThreadedLRUCacheStrategy<>(3, CLEANUP_DELAY_MS);
        cache.put("one", 1);
        cache.put("two", 2);
        String s = cache.toString();

        assertTrue(s.contains("one"));
        assertTrue(s.contains("two"));
        assertTrue(s.contains("1"));
        assertTrue(s.contains("2"));
    }

    @Test
    public void testClearAndIsEmpty() {
        ThreadedLRUCacheStrategy<Integer, String> cache = new ThreadedLRUCacheStrategy<>(4, CLEANUP_DELAY_MS);
        cache.put(1, "a");
        cache.put(2, "b");
        assertFalse(cache.isEmpty());
        cache.clear();
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
    }

    @Test
    public void testCapacityEvictionLRU() throws InterruptedException {
        // capacity = 3, delay = 20ms (fast enough for test)
        ThreadedLRUCacheStrategy<String, String> cache = new ThreadedLRUCacheStrategy<>(3, CLEANUP_DELAY_MS);

        // Insert three items
        cache.put("A", "alpha");
        cache.put("B", "bravo");
        cache.put("C", "charlie");

        // Access "A" to make it most recently used
        assertEquals("alpha", cache.get("A"));

        // Insert fourth item, which should trigger eviction after the delay
        cache.put("D", "delta");

        // Wait for the asynchronous cleanup to run (a bit longer than the delay)
        TimeUnit.MILLISECONDS.sleep(CLEANUP_DELAY_MS * 3);

        // At this point cache size must be back to capacity (3)
        assertEquals(3, cache.size());

        // "B" should have been evicted because it was the least recently used
        assertFalse(cache.containsKey("B"));
        assertTrue(cache.containsKey("A"));
        assertTrue(cache.containsKey("C"));
        assertTrue(cache.containsKey("D"));
    }

    @Test
    public void testMultipleEvictionCycles() throws InterruptedException {
        // capacity = 2, delay = 20ms
        ThreadedLRUCacheStrategy<Integer, String> cache = new ThreadedLRUCacheStrategy<>(2, CLEANUP_DELAY_MS);

        // Fill beyond capacity several times
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three"); // triggers eviction
        TimeUnit.MILLISECONDS.sleep(CLEANUP_DELAY_MS * 3);
        assertEquals(2, cache.size());
        assertFalse(cache.containsKey(1)); // LRU should be evicted

        cache.put(4, "four");
        cache.put(5, "five");
        TimeUnit.MILLISECONDS.sleep(CLEANUP_DELAY_MS * 3);
        assertEquals(2, cache.size());
        // After the second round the remaining keys should be 4 and 5 (2 should have been evicted)
        assertFalse(cache.containsKey(2));
        assertTrue(cache.containsKey(4));
        assertTrue(cache.containsKey(5));
    }
}
