package com.cedarsoftware.util;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class TTLCacheTest {

    // Helper to create a cache with a very short TTL and cleanup interval
    private TTLCache<String, String> createCache(long ttlMillis, int maxSize, long cleanupInterval) {
        return new TTLCache<>(ttlMillis, maxSize, cleanupInterval);
    }

    @AfterClass
    public static void shutdownScheduler() {
        // Shut down the shared scheduler once all tests have finished.
        TTLCache.shutdown();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsZeroTtl() {
        new TTLCache<>(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsNegativeTtl() {
        new TTLCache<>(-5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsTooSmallCleanupInterval() {
        new TTLCache<>(100, -1, 5);
    }

    @Test
    public void testBasicPutAndGet() {
        TTLCache<String, String> cache = createCache(5000, -1, 60000);
        assertNull(cache.put("a", "alpha"));
        assertEquals("alpha", cache.get("a"));
        assertEquals(1, cache.size());
        assertFalse(cache.isEmpty());
    }

    @Test
    public void testPutReturnsPreviousValue() {
        TTLCache<String, String> cache = createCache(5000, -1, 60000);
        cache.put("k", "v1");
        assertEquals("v1", cache.put("k", "v2"));
        assertEquals("v2", cache.get("k"));
    }

    @Test
    public void testExpirationViaGet() throws InterruptedException {
        TTLCache<String, String> cache = createCache(30, -1, 10);
        cache.put("temp", "value");
        assertEquals("value", cache.get("temp"));
        Thread.sleep(50); // exceed TTL
        assertNull(cache.get("temp"));
        assertEquals(0, cache.size());
    }

    @Test
    public void testExpirationViaBackgroundPurge() throws InterruptedException {
        // TTL 30 ms, cleanup interval 10 ms – background thread should purge automatically.
        TTLCache<String, String> cache = createCache(30, -1, 10);
        cache.put("x", "y");
        Thread.sleep(70); // give background task time to run
        assertFalse(cache.containsKey("x"));
        assertEquals(0, cache.size());
    }

    @Test
    public void testLruEvictionWhenMaxSizeExceeded() {
        TTLCache<String, String> cache = createCache(5000, 2, 60000);
        cache.put("first", "1");
        cache.put("second", "2");
        // Access "first" to make it most-recently used
        cache.get("first");
        cache.put("third", "3"); // should evict "second"
        assertNull(cache.get("second"));
        assertEquals("1", cache.get("first"));
        assertEquals("3", cache.get("third"));
        assertEquals(2, cache.size());
    }

    @Test
    public void testRemoveUpdatesLruStructure() {
        TTLCache<String, String> cache = createCache(5000, 3, 60000);
        cache.put("a", "A");
        cache.put("b", "B");
        cache.put("c", "C");
        assertEquals(3, cache.size());

        // Remove middle entry
        assertEquals("B", cache.remove("b"));
        assertNull(cache.get("b"));
        assertEquals(2, cache.size());

        // Ensure remaining entries are still accessible
        assertEquals("A", cache.get("a"));
        assertEquals("C", cache.get("c"));
    }

    @Test
    public void testClearResetsCache() {
        TTLCache<String, String> cache = createCache(5000, -1, 60000);
        cache.put("x", "X");
        cache.put("y", "Y");
        assertFalse(cache.isEmpty());
        cache.clear();
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
        assertNull(cache.get("x"));
    }

    @Test
    public void testContainsKeyAndContainsValue() throws InterruptedException {
        TTLCache<String, String> cache = createCache(30, -1, 10);
        cache.put("k1", "v1");
        cache.put("k2", null);
        cache.put(null, "nullKey");

        assertTrue(cache.containsKey("k1"));
        assertTrue(cache.containsKey("k2"));
        assertTrue(cache.containsKey(null));

        assertTrue(cache.containsValue("v1"));
        assertTrue(cache.containsValue(null));
        assertTrue(cache.containsValue("nullKey"));

        Thread.sleep(50); // expire entries
        assertFalse(cache.containsKey("k1"));
        assertFalse(cache.containsValue("v1"));
    }

    @Test
    public void testPutAllCopiesEntries() {
        TTLCache<String, String> cache = createCache(5000, -1, 60000);
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

    @Test
    public void testKeySetAndValues() {
        TTLCache<String, String> cache = createCache(5000, -1, 60000);
        cache.put("a", "A");
        cache.put("b", "B");
        cache.put("c", "C");

        Set<String> keys = cache.keySet();
        Collection<String> values = cache.values();

        assertEquals(new HashSet<>(Arrays.asList("a", "b", "c")), keys);
        assertTrue(values.containsAll(Arrays.asList("A", "B", "C")));
        assertEquals(3, values.size());
    }

    @Test
    public void testEntrySetIteratorAndRemove() {
        TTLCache<String, String> cache = createCache(5000, -1, 60000);
        cache.put("x", "X");
        cache.put("y", "Y");
        cache.put("z", "Z");

        Iterator<Map.Entry<String, String>> it = cache.entrySet().iterator();
        int count = 0;
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            if ("y".equals(entry.getKey())) {
                it.remove(); // remove via iterator
            }
            count++;
        }
        assertEquals(3, count);
        assertEquals(2, cache.size());
        assertNull(cache.get("y"));
        assertTrue(cache.containsKey("x"));
        assertTrue(cache.containsKey("z"));
    }

    @Test
    public void testEqualsAndHashCode() {
        TTLCache<String, String> cache1 = createCache(5000, -1, 60000);
        TTLCache<String, String> cache2 = createCache(5000, -1, 60000);

        cache1.put("a", "A");
        cache1.put("b", "B");

        cache2.put("a", "A");
        cache2.put("b", "B");

        assertTrue(cache1.equals(cache2));
        assertEquals(cache1.hashCode(), cache2.hashCode());

        // Change one cache
        cache2.put("c", "C");
        assertFalse(cache1.equals(cache2));
    }

    @Test
    public void testToStringFormat() {
        TTLCache<String, Integer> cache = createCache(5000, -1, 60000);
        cache.put("one", 1);
        cache.put("two", 2);
        String str = cache.toString();

        // The string should contain both entries in any order, separated by comma and space
        assertTrue(str.startsWith("{"));
        assertTrue(str.endsWith("}"));
        assertTrue(str.contains("one=1"));
        assertTrue(str.contains("two=2"));
        // Ensure there is exactly one comma when there are two entries
        assertEquals(1, str.chars().filter(ch -> ch == ',').count());
    }

    @Test
    public void testNullKeyAndNullValueHandling() {
        TTLCache<String, String> cache = createCache(5000, -1, 60000);
        cache.put(null, null);
        assertTrue(cache.containsKey(null));
        assertTrue(cache.containsValue(null));
        assertNull(cache.get(null));

        cache.put("k", null);
        assertTrue(cache.containsKey("k"));
        assertTrue(cache.containsValue(null));
        assertNull(cache.get("k"));
    }

    @Test
    public void testSizeAndIsEmptyAfterVariousOperations() {
        TTLCache<Integer, String> cache = createCache(5000, -1, 60000);
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());

        cache.put(1, "one");
        assertFalse(cache.isEmpty());
        assertEquals(1, cache.size());

        cache.put(2, "two");
        assertEquals(2, cache.size());

        cache.remove(1);
        assertEquals(1, cache.size());

        cache.clear();
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
    }
}
