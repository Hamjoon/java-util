package com.cedarsoftware.util;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class TTLCacheTest {

    private TTLCache<String, String> cache;

    @Before
    public void setUp() {
        // ttl = 200 ms, maxSize = 3, cleanup interval = 50 ms (fast for tests)
        cache = new TTLCache<>(200, 3, 50);
    }

    @AfterClass
    public static void tearDownClass() {
        // Shut down the shared scheduler to avoid thread leakage after all tests
        TTLCache.shutdown();
    }

    @Test
    public void testPutAndGetBasic() {
        assertNull(cache.put("a", "alpha"));
        assertEquals("alpha", cache.get("a"));
        assertEquals(1, cache.size());

        // replace existing value
        assertEquals("alpha", cache.put("a", "beta"));
        assertEquals("beta", cache.get("a"));
    }

    @Test
    public void testExpiration() throws InterruptedException {
        cache.put("k1", "v1");
        assertEquals("v1", cache.get("k1"));
        // wait longer than ttl
        Thread.sleep(300);
        // entry should be expired and removed
        assertNull(cache.get("k1"));
        assertFalse(cache.containsKey("k1"));
        assertEquals(0, cache.size());
    }

    @Test
    public void testLRUEviction() {
        // maxSize = 3, insert 4 entries, the least recently used should be evicted
        cache.put("1", "one");
        cache.put("2", "two");
        cache.put("3", "three");
        // access "1" to make it most recently used
        cache.get("1");
        // inserting fourth entry should evict "2" (the LRU)
        cache.put("4", "four");

        assertTrue(cache.containsKey("1"));
        assertFalse(cache.containsKey("2"));
        assertTrue(cache.containsKey("3"));
        assertTrue(cache.containsKey("4"));
        assertEquals(3, cache.size());
    }

    @Test
    public void testContainsKeyAndContainsValue() {
        cache.put("x", "Xvalue");
        cache.put("y", "Yvalue");
        assertTrue(cache.containsKey("x"));
        assertTrue(cache.containsValue("Yvalue"));
        assertFalse(cache.containsKey("z"));
        assertFalse(cache.containsValue("Zvalue"));
    }

    @Test
    public void testRemoveAndClear() {
        cache.put("a", "A");
        cache.put("b", "B");
        assertEquals("A", cache.remove("a"));
        assertNull(cache.get("a"));
        assertEquals(1, cache.size());

        cache.clear();
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
    }

    @Test
    public void testEntrySetIteratorRemove() {
        cache.put("k1", "v1");
        cache.put("k2", "v2");
        cache.put("k3", "v3");

        Set<Map.Entry<String, String>> entrySet = cache.entrySet();
        Iterator<Map.Entry<String, String>> it = entrySet.iterator();

        while (it.hasNext()) {
            Map.Entry<String, String> e = it.next();
            if ("k2".equals(e.getKey())) {
                it.remove(); // should remove from cache
            }
        }

        assertFalse(cache.containsKey("k2"));
        assertEquals(2, cache.size());
    }

    @Test
    public void testEqualsAndHashCode() {
        TTLCache<String, String> other = new TTLCache<>(200, -1, 50);
        cache.put("a", "1");
        cache.put("b", "2");
        other.put("a", "1");
        other.put("b", "2");

        assertEquals(cache, other);
        assertEquals(other, cache);
        assertEquals(cache.hashCode(), other.hashCode());

        // modify other
        other.put("c", "3");
        assertNotEquals(cache, other);
    }

    @Test
    public void testToStringFormat() {
        cache.put("one", "1");
        cache.put("two", "2");
        String s = cache.toString();

        // The string should start with { and end with } and contain both entries
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        assertTrue(s.contains("one=1"));
        assertTrue(s.contains("two=2"));
        // order is not guaranteed, just ensure comma separation if both present
        assertTrue(s.contains(","));
    }

    @Test
    public void testPutAllAndKeySetValues() {
        Map<String, String> map = new HashMap<>();
        map.put("kA", "vA");
        map.put("kB", "vB");
        cache.putAll(map);

        Set<String> keys = cache.keySet();
        Collection<String> values = cache.values();

        assertTrue(keys.containsAll(Arrays.asList("kA", "kB")));
        assertTrue(values.containsAll(Arrays.asList("vA", "vB")));
        assertEquals(2, cache.size());
    }

    @Test
    public void testPurgeTaskRunsAutomatically() throws InterruptedException {
        // ttl 100 ms, cleanup interval 30 ms
        TTLCache<String, String> fastCache = new TTLCache<>(100, -1, 30);
        fastCache.put("temp", "value");
        assertEquals("value", fastCache.get("temp"));
        // Wait enough for both ttl and cleanup task to fire
        Thread.sleep(200);
        // The entry should have been purged by the background task
        assertNull(fastCache.get("temp"));
        assertEquals(0, fastCache.size());
        fastCache.clear(); // just to be tidy
    }
}
