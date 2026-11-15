package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Collection;

import org.junit.AfterClass;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 tests for {@link TTLCache}.
 */
public class TTLCacheTest {

    /** Shut down the shared scheduler after the whole suite to avoid thread leaks. */
    @AfterClass
    public static void afterAll() {
        TTLCache.shutdown();
    }

    /* ---------- Constructor validation ---------- */

    @Test(expected = IllegalArgumentException.class)
    public void ctorRejectsZeroTtl() {
        new TTLCache<>(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ctorRejectsNegativeTtl() {
        new TTLCache<>(-5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ctorRejectsTooSmallCleanupInterval() {
        new TTLCache<>(100, -1, 5);
    }

    /* ---------- Basic put / get / size / contains ---------- */

    @Test
    public void basicPutGetWorks() {
        TTLCache<String, String> cache = new TTLCache<>(1000);
        assertNull(cache.put("a", "alpha"));            // first insertion returns null
        assertEquals("alpha", cache.get("a"));
        assertEquals(1, cache.size());
        assertFalse(cache.isEmpty());
        assertTrue(cache.containsKey("a"));
        assertTrue(cache.containsValue("alpha"));
        assertEquals("alpha", cache.put("a", "beta"));  // replace returns old value
        assertEquals("beta", cache.get("a"));
    }

    /* ---------- Expiration handling ---------- */

    @Test
    public void entriesExpireAfterTtl() throws InterruptedException {
        // ttl = 50 ms, cleanup interval = 10 ms (fast enough for the test)
        TTLCache<String, String> cache = new TTLCache<>(50, -1, 10);
        cache.put("b", "beta");
        assertEquals("beta", cache.get("b"));
        Thread.sleep(120); // wait longer than ttl
        // get should return null and entry must be removed
        assertNull(cache.get("b"));
        assertFalse(cache.containsKey("b"));
        assertEquals(0, cache.size());
    }

    /* ---------- LRU eviction when maxSize is set ---------- */

    @Test
    public void lruEvictionHonoursMaxSize() {
        TTLCache<String, String> cache = new TTLCache<>(1000, 2);
        cache.put("k1", "v1");
        cache.put("k2", "v2");
        // Access k1 to make it most‑recently used
        assertEquals("v1", cache.get("k1"));
        // Adding a third entry should evict the least‑recently used (k2)
        cache.put("k3", "v3");
        assertTrue(cache.containsKey("k1"));
        assertFalse(cache.containsKey("k2"));
        assertTrue(cache.containsKey("k3"));
        assertEquals(2, cache.size());
    }

    /* ---------- remove and clear ---------- */

    @Test
    public void removeDeletesEntryAndReturnsValue() {
        TTLCache<String, String> cache = new TTLCache<>(1000);
        cache.put("x", "xval");
        assertEquals("xval", cache.remove("x"));
        assertNull(cache.get("x"));
        assertEquals(0, cache.size());
    }

    @Test
    public void clearEmptiesCache() {
        TTLCache<Integer, String> cache = new TTLCache<>(1000);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.clear();
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
        assertTrue(cache.keySet().isEmpty());
        assertTrue(cache.values().isEmpty());
    }

    /* ---------- putAll, keySet, values ---------- */

    @Test
    public void putAllCopiesAllMappings() {
        Map<String, String> source = new HashMap<>();
        source.put("a", "1");
        source.put("b", "2");
        TTLCache<String, String> cache = new TTLCache<>(1000);
        cache.putAll(source);
        assertEquals(2, cache.size());
        assertTrue(cache.containsKey("a"));
        assertTrue(cache.containsValue("2"));
    }

    @Test
    public void keySetAndValuesReflectCurrentState() {
        TTLCache<String, Integer> cache = new TTLCache<>(1000);
        cache.put("one", 1);
        cache.put("two", 2);
        Set<String> keys = cache.keySet();
        Collection<Integer> vals = cache.values();
        assertTrue(keys.contains("one"));
        assertTrue(keys.contains("two"));
        assertTrue(vals.contains(1));
        assertTrue(vals.contains(2));
    }

    /* ---------- EntrySet iterator removal ---------- */

    @Test
    public void entrySetIteratorCanRemoveElements() {
        TTLCache<String, String> cache = new TTLCache<>(1000);
        cache.put("a", "A");
        cache.put("b", "B");
        cache.put("c", "C");
        Iterator<Map.Entry<String, String>> it = cache.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> e = it.next();
            if ("b".equals(e.getKey())) {
                it.remove(); // should delete entry "b"
            }
        }
        assertEquals(2, cache.size());
        assertFalse(cache.containsKey("b"));
        assertTrue(cache.containsKey("a"));
        assertTrue(cache.containsKey("c"));
    }

    /* ---------- equals and hashCode ---------- */

    @Test
    public void equalsWorksWithOtherMaps() {
        TTLCache<String, String> cache = new TTLCache<>(1000);
        cache.put("x", "X");
        cache.put("y", "Y");

        Map<String, String> map = new HashMap<>();
        map.put("x", "X");
        map.put("y", "Y");

        assertTrue(cache.equals(map));
        assertTrue(map.equals(cache)); // Map's equals uses entrySet, which works

        TTLCache<String, String> otherCache = new TTLCache<>(1000);
        otherCache.put("x", "X");
        otherCache.put("y", "Y");
        assertTrue(cache.equals(otherCache));
    }

    @Test
    public void hashCodeConsistentWithLruOrder() {
        TTLCache<String, String> cache1 = new TTLCache<>(1000);
        TTLCache<String, String> cache2 = new TTLCache<>(1000);

        cache1.put("a", "A");
        cache1.put("b", "B");
        cache2.put("a", "A");
        cache2.put("b", "B");

        // Same insertion order → same hash code
        assertEquals(cache1.hashCode(), cache2.hashCode());

        // Change access order in cache2
        cache2.get("a"); // makes "a" most‑recently used
        // LRU order differs, hash codes should differ
        assertNotEquals(cache1.hashCode(), cache2.hashCode());
    }

    /* ---------- toString format ---------- */

    @Test
    public void toStringProducesExpectedFormat() {
        TTLCache<String, Integer> cache = new TTLCache<>(1000);
        cache.put("one", 1);
        cache.put("two", 2);
        String s = cache.toString();
        // Order is insertion order because we never accessed entries
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        assertTrue(s.contains("one=1"));
        assertTrue(s.contains("two=2"));
        // The two entries must be separated by a comma and a space
        assertTrue(s.contains(", "));
    }

    /* ---------- Support for null keys and values ---------- */

    @Test
    public void cacheAcceptsNullKeyAndValue() {
        TTLCache<String, String> cache = new TTLCache<>(1000);
        cache.put(null, null);
        assertTrue(cache.containsKey(null));
        assertTrue(cache.containsValue(null));
        // get returns null both for “missing” and for a stored null value,
        // but containsKey distinguishes the two cases.
        assertNull(cache.get(null));
        assertEquals(1, cache.size());
        assertEquals(null, cache.remove(null));
        assertEquals(0, cache.size());
    }
}
