package com.cedarsoftware.util;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

/**
 * Comprehensive JUnit‑4 test suite for {@link TTLCache}.
 * Covers normal usage, edge cases, and exception handling.
 */
public class TTLCacheTest {

    // -----------------------------------------------------------------
    // Helper – small TTL cache used by several tests
    // -----------------------------------------------------------------
    private TTLCache<String, String> cache;

    @Before
    public void setUp() {
        // default TTL of 1 second, unlimited size
        cache = new TTLCache<>(1000);
    }

    @AfterClass
    public static void tearDownClass() {
        // Shut down the shared scheduler so the test JVM can exit cleanly
        TTLCache.shutdown();
    }

    // -----------------------------------------------------------------
    // 1‑3  Constructors – basic creation & validation
    // -----------------------------------------------------------------
    @Test
    public void testConstructorValid() {
        TTLCache<String, String> c = new TTLCache<>(500);
        assertTrue(c.isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorZeroTTL() {
        new TTLCache<String, String>(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorSmallCleanup() {
        new TTLCache<String, String>(1000, -1, 5);
    }

    // -----------------------------------------------------------------
    // 4  put
    // -----------------------------------------------------------------
    @Test
    public void testPutReturnsOldValue() {
        assertNull(cache.put("a", "1"));
        assertEquals("1", cache.put("a", "2"));
    }

    // -----------------------------------------------------------------
    // 5  get (non‑expired)
    // -----------------------------------------------------------------
    @Test
    public void testGetReturnsValue() {
        cache.put("k", "v");
        assertEquals("v", cache.get("k"));
    }

    // -----------------------------------------------------------------
    // 6  remove
    // -----------------------------------------------------------------
    @Test
    public void testRemove() {
        cache.put("x", "y");
        assertEquals("y", cache.remove("x"));
        assertNull(cache.get("x"));
    }

    // -----------------------------------------------------------------
    // 7  clear
    // -----------------------------------------------------------------
    @Test
    public void testClearEmptiesCache() {
        cache.put("a", "1");
        cache.clear();
        assertTrue(cache.isEmpty());
    }

    // -----------------------------------------------------------------
    // 8  size
    // -----------------------------------------------------------------
    @Test
    public void testSizeReflectsEntries() {
        cache.put("a", "1");
        cache.put("b", "2");
        assertEquals(2, cache.size());
    }

    // -----------------------------------------------------------------
    // 9  isEmpty
    // -----------------------------------------------------------------
    @Test
    public void testIsEmpty() {
        assertTrue(cache.isEmpty());
        cache.put("a", "1");
        assertFalse(cache.isEmpty());
    }

    // -----------------------------------------------------------------
    //10  containsKey (non‑expired)
    // -----------------------------------------------------------------
    @Test
    public void testContainsKey() {
        cache.put("k", "v");
        assertTrue(cache.containsKey("k"));
        assertFalse(cache.containsKey("missing"));
    }

    // -----------------------------------------------------------------
    //11  containsValue
    // -----------------------------------------------------------------
    @Test
    public void testContainsValue() {
        cache.put("k", "v");
        assertTrue(cache.containsValue("v"));
        assertFalse(cache.containsValue("other"));
    }

    // -----------------------------------------------------------------
    //12  putAll
    // -----------------------------------------------------------------
    @Test
    public void testPutAll() {
        Map<String, String> src = new HashMap<>();
        src.put("a", "1");
        src.put("b", "2");
        cache.putAll(src);
        assertEquals(2, cache.size());
        assertEquals("1", cache.get("a"));
        assertEquals("2", cache.get("b"));
    }

    // -----------------------------------------------------------------
    //13  keySet
    // -----------------------------------------------------------------
    @Test
    public void testKeySet() {
        cache.put("a", "1");
        cache.put("b", "2");
        Set<String> ks = cache.keySet();
        assertTrue(ks.containsAll(Arrays.asList("a", "b")));
        assertEquals(2, ks.size());
    }

    // -----------------------------------------------------------------
    //14  values
    // -----------------------------------------------------------------
    @Test
    public void testValues() {
        cache.put("a", "1");
        cache.put("b", "2");
        Collection<String> vals = cache.values();
        assertTrue(vals.containsAll(Arrays.asList("1", "2")));
        assertEquals(2, vals.size());
    }

    // -----------------------------------------------------------------
    //15  entrySet iterator remove
    // -----------------------------------------------------------------
    @Test
    public void testEntrySetIteratorRemove() {
        cache.put("a", "1");
        cache.put("b", "2");
        Iterator<Entry<String, String>> it = cache.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, String> e = it.next();
            if ("a".equals(e.getKey())) {
                it.remove();
            }
        }
        assertFalse(cache.containsKey("a"));
        assertEquals(1, cache.size());
    }

    // -----------------------------------------------------------------
    // Iterator.remove() without a preceding next()
    // -----------------------------------------------------------------
    @Test(expected = IllegalStateException.class)
    public void testIteratorRemoveWithoutNext() {
        Iterator<Entry<String, String>> it = cache.entrySet().iterator();
        it.remove(); // should throw IllegalStateException
    }

    // -----------------------------------------------------------------
    //16  equals
    // -----------------------------------------------------------------
    @Test
    public void testEqualsWithMap() {
        cache.put("a", "1");
        Map<String, String> m = new HashMap<>();
        m.put("a", "1");
        assertTrue(cache.equals(m));
    }

    @Test
    public void testEqualsWithNonMap() {
        assertFalse(cache.equals("not a map"));
    }

    // -----------------------------------------------------------------
    //17  hashCode – consistency & empty cache case
    // -----------------------------------------------------------------
    @Test
    public void testHashCodeConsistent() {
        TTLCache<String, String> c1 = new TTLCache<>(1000);
        TTLCache<String, String> c2 = new TTLCache<>(1000);
        c1.put("k", "v");
        c2.put("k", "v");
        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    public void testHashCodeEmptyCache() {
        TTLCache<String, String> empty = new TTLCache<>(1000);
        assertEquals(1, empty.hashCode());
    }

    // -----------------------------------------------------------------
    //18  toString
    // -----------------------------------------------------------------
    @Test
    public void testToStringFormat() {
        cache.put("a", "1");
        cache.put("b", "2");
        String s = cache.toString();
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        assertTrue(s.contains("a=1"));
        assertTrue(s.contains("b=2"));
    }

    @Test
    public void testToStringEmpty() {
        TTLCache<String, String> empty = new TTLCache<>(1000);
        assertEquals("{}", empty.toString());
    }

    // -----------------------------------------------------------------
    //19  shutdown (static)
    // -----------------------------------------------------------------
    @Test
    public void testShutdownDoesNotThrow() {
        TTLCache.shutdown(); // should complete without exception
    }

    // -----------------------------------------------------------------
    // Edge case: null key / null value support
    // -----------------------------------------------------------------
    @Test
    public void testNullKeyAndValue() {
        cache.put(null, "nullKey");
        cache.put("nullValue", null);
        assertEquals("nullKey", cache.get(null));
        assertNull(cache.get("nullValue"));
        assertTrue(cache.containsKey(null));
        assertTrue(cache.containsValue(null));
        assertEquals(2, cache.size());
    }

    // -----------------------------------------------------------------
    // Edge case: entry expiration
    // -----------------------------------------------------------------
    @Test
    public void testEntryExpires() throws InterruptedException {
        TTLCache<String, String> shortTTL = new TTLCache<>(50); // 50 ms TTL
        shortTTL.put("temp", "value");
        // wait longer than TTL
        Thread.sleep(80);
        assertNull(shortTTL.get("temp"));
        assertEquals(0, shortTTL.size());
    }

    // -----------------------------------------------------------------
    // Edge case: LRU eviction when maxSize is defined
    // -----------------------------------------------------------------
    @Test
    public void testLruEviction() {
        TTLCache<String, String> lru = new TTLCache<>(1000, 2); // maxSize = 2
        lru.put("first", "1");   // becomes LRU
        lru.put("second", "2");
        lru.put("third", "3");   // should evict "first"
        assertFalse(lru.containsKey("first"));
        assertTrue(lru.containsKey("second"));
        assertTrue(lru.containsKey("third"));
        assertEquals(2, lru.size());
    }
}
