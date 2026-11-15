package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Collection;

import org.junit.AfterClass;
import org.junit.Test;

public class TTLCacheTest {

    /***  Constructor validation ***/
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorInvalidTtl() {
        new TTLCache<>(0L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorInvalidCleanupInterval() {
        new TTLCache<>(100L, -1, 5L);
    }

    /***  Basic put / get / size / isEmpty ***/
    @Test
    public void testPutAndGetSimple() {
        TTLCache<String, String> cache = new TTLCache<>(5000L);
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());

        assertNull(cache.put("k1", "v1"));
        assertEquals("v1", cache.get("k1"));
        assertTrue(cache.containsKey("k1"));
        assertTrue(cache.containsValue("v1"));
        assertEquals(1, cache.size());
        assertFalse(cache.isEmpty());

        // replace existing value
        assertEquals("v1", cache.put("k1", "v2"));
        assertEquals("v2", cache.get("k1"));
        assertEquals(1, cache.size());
    }

    /***  Expiration behavior ***/
    @Test
    public void testExpiration() throws InterruptedException {
        // ttl = 30 ms, cleanup interval = 10 ms
        TTLCache<String, String> cache = new TTLCache<>(30L, -1, 10L);
        cache.put("expireKey", "expireValue");

        // value should be present immediately
        assertEquals("expireValue", cache.get("expireKey"));
        assertTrue(cache.containsKey("expireKey"));

        // wait long enough for the entry to expire and be purged
        Thread.sleep(120L);

        // after expiration the entry must be gone
        assertNull(cache.get("expireKey"));
        assertFalse(cache.containsKey("expireKey"));
        assertEquals(0, cache.size());
    }

    /***  LRU eviction when maxSize is set ***/
    @Test
    public void testLruEviction() {
        // ttl large enough to avoid expiration, maxSize = 2
        TTLCache<String, String> cache = new TTLCache<>(5000L, 2, 60000L);
        cache.put("a", "1");   // LRU: a
        cache.put("b", "2");   // LRU: a, b (b most recent)

        // access 'a' to make it most-recently used
        assertEquals("1", cache.get("a")); // LRU order now: b, a

        // inserting third entry should evict 'b'
        cache.put("c", "3");

        assertNull(cache.get("b"));
        assertFalse(cache.containsKey("b"));
        assertTrue(cache.containsKey("a"));
        assertTrue(cache.containsKey("c"));
        assertEquals(2, cache.size());

        // verify order via toString (a should appear before c because a is older than c)
        String representation = cache.toString();
        int idxA = representation.indexOf("a=1");
        int idxC = representation.indexOf("c=3");
        assertTrue("a should appear before c in the string representation", idxA < idxC);
    }

    /***  containsKey / containsValue with expired entries ***/
    @Test
    public void testContainsKeyAndValueWithExpiration() throws InterruptedException {
        TTLCache<String, String> cache = new TTLCache<>(20L, -1, 10L);
        cache.put("k", "v");
        assertTrue(cache.containsKey("k"));
        assertTrue(cache.containsValue("v"));

        Thread.sleep(70L); // ensure expiration

        assertFalse(cache.containsKey("k"));
        assertFalse(cache.containsValue("v"));
    }

    /***  remove operation ***/
    @Test
    public void testRemove() {
        TTLCache<String, String> cache = new TTLCache<>(5000L);
        cache.put("x", "xval");
        cache.put("y", "yval");

        assertEquals("xval", cache.remove("x"));
        assertNull(cache.get("x"));
        assertEquals(1, cache.size());

        // removing non‑existent key returns null
        assertNull(cache.remove("nonexistent"));
    }

    /***  clear operation ***/
    @Test
    public void testClear() {
        TTLCache<Integer, Integer> cache = new TTLCache<>(1000L);
        cache.put(1, 10);
        cache.put(2, 20);
        cache.put(3, 30);
        assertFalse(cache.isEmpty());
        assertEquals(3, cache.size());

        cache.clear();
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
        assertNull(cache.get(1));
    }

    /***  putAll ***/
    @Test
    public void testPutAll() {
        TTLCache<String, String> cache = new TTLCache<>(5000L);
        Map<String, String> source = new HashMap<>();
        source.put("a", "A");
        source.put("b", "B");
        source.put("c", "C");

        cache.putAll(source);
        assertEquals(3, cache.size());
        assertEquals("A", cache.get("a"));
        assertEquals("B", cache.get("b"));
        assertEquals("C", cache.get("c"));
    }

    /***  keySet and values collections ***/
    @Test
    public void testKeySetAndValues() {
        TTLCache<String, Integer> cache = new TTLCache<>(5000L);
        cache.put("one", 1);
        cache.put("two", 2);
        cache.put("three", 3);

        Set<String> keys = cache.keySet();
        assertTrue(keys.contains("one"));
        assertTrue(keys.contains("two"));
        assertTrue(keys.contains("three"));
        assertEquals(3, keys.size());

        Collection<Integer> values = cache.values();
        assertTrue(values.contains(1));
        assertTrue(values.contains(2));
        assertTrue(values.contains(3));
        assertEquals(3, values.size());
    }

    /***  entrySet iterator remove ***/
    @Test
    public void testEntrySetIteratorRemove() {
        TTLCache<String, String> cache = new TTLCache<>(5000L);
        cache.put("k1", "v1");
        cache.put("k2", "v2");
        cache.put("k3", "v3");

        // Remove entry with key "k2" via iterator
        java.util.Iterator<java.util.Map.Entry<String, String>> it = cache.entrySet().iterator();
        while (it.hasNext()) {
            java.util.Map.Entry<String, String> e = it.next();
            if ("k2".equals(e.getKey())) {
                it.remove();
                break;
            }
        }

        assertFalse(cache.containsKey("k2"));
        assertEquals(2, cache.size());
        assertTrue(cache.containsKey("k1"));
        assertTrue(cache.containsKey("k3"));
    }

    /***  equals and hashCode ***/
    @Test
    public void testEqualsAndHashCode() {
        TTLCache<String, String> cache1 = new TTLCache<>(5000L);
        TTLCache<String, String> cache2 = new TTLCache<>(5000L);

        cache1.put("a", "1");
        cache1.put("b", "2");

        cache2.put("a", "1");
        cache2.put("b", "2");

        assertEquals(cache1, cache2);
        assertEquals(cache1.hashCode(), cache2.hashCode());

        // change one cache
        cache2.put("c", "3");
        assertNotEquals(cache1, cache2);
    }

    /***  toString representation ***/
    @Test
    public void testToString() {
        TTLCache<String, Integer> cache = new TTLCache<>(5000L);
        cache.put("x", 100);
        cache.put("y", 200);
        String s = cache.toString();

        // Basic format check – must start with '{' and end with '}'
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        // Must contain both entries
        assertTrue(s.contains("x=100"));
        assertTrue(s.contains("y=200"));
        // Must contain a comma between entries when more than one
        assertTrue(s.contains(", "));
    }

    /***  Scheduler shutdown to avoid thread leakage ***/
    @AfterClass
    public static void shutdownScheduler() {
        TTLCache.shutdown();
    }
}
