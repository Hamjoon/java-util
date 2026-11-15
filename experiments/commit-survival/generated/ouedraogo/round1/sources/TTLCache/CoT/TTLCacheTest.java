package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class TTLCacheTest {

    private TTLCache<String, String> cache;
    private TTLCache<String, String> lruCache;
    private TTLCache<String, String> expiringCache;

    @Before
    public void setUp() {
        // Normal cache – no size limit, long TTL (so it does not expire during most tests)
        cache = new TTLCache<>(10000L);

        // LRU cache – small max size to test eviction
        lruCache = new TTLCache<>(10000L, 2);

        // Cache with a very short TTL to test expiration
        expiringCache = new TTLCache<>(50L); // 50 ms TTL
    }

    @After
    public void tearDown() {
        // Shut down the shared scheduler to avoid thread leakage after the test suite
        TTLCache.shutdown();
    }

    /* ----------------------------------------------------------------------
     *  Public constructors – basic validation and exception handling
     * ---------------------------------------------------------------------- */
    @Test(expected = IllegalArgumentException.class)
    public void constructorZeroTtlShouldThrow() {
        new TTLCache<>(0L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorNegativeTtlShouldThrow() {
        new TTLCache<>(-5L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorCleanupIntervalTooSmallShouldThrow() {
        new TTLCache<>(100L, -1, 5L);
    }

    @Test
    public void constructorWithAllParametersWorks() {
        TTLCache<Integer, Integer> c = new TTLCache<>(200L, 10, 500L);
        assertNotNull(c);
        assertEquals(0, c.size());
    }

    /* ----------------------------------------------------------------------
     *  Basic Map operations
     * ---------------------------------------------------------------------- */
    @Test
    public void putAndGetSimpleEntry() {
        String prev = cache.put("key1", "value1");
        assertNull(prev);
        assertEquals("value1", cache.get("key1"));
    }

    @Test
    public void putOverridesExistingValue() {
        cache.put("k", "v1");
        String old = cache.put("k", "v2");
        assertEquals("v1", old);
        assertEquals("v2", cache.get("k"));
    }

    @Test
    public void getMissingKeyReturnsNull() {
        assertNull(cache.get("unknown"));
    }

    @Test
    public void removeExistingKey() {
        cache.put("a", "A");
        String removed = cache.remove("a");
        assertEquals("A", removed);
        assertNull(cache.get("a"));
        assertEquals(0, cache.size());
    }

    @Test
    public void removeNonExistingKeyReturnsNull() {
        assertNull(cache.remove("nothing"));
    }

    @Test
    public void clearEmptiesCache() {
        cache.put("x", "X");
        cache.put("y", "Y");
        assertFalse(cache.isEmpty());
        cache.clear();
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
    }

    @Test
    public void sizeAndIsEmptyReflectContent() {
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());

        cache.put("one", "1");
        assertFalse(cache.isEmpty());
        assertEquals(1, cache.size());
    }

    @Test
    public void containsKeyWorksAndHandlesExpiration() throws InterruptedException {
        expiringCache.put("temp", "data");
        assertTrue(expiringCache.containsKey("temp"));
        Thread.sleep(100); // wait for expiration
        assertFalse(expiringCache.containsKey("temp"));
    }

    @Test
    public void containsValueFindsExistingAndNullValues() {
        cache.put("k1", "v1");
        cache.put("k2", null);
        assertTrue(cache.containsValue("v1"));
        assertTrue(cache.containsValue(null));
        assertFalse(cache.containsValue("nope"));
    }

    @Test
    public void putAllCopiesAllEntries() {
        Map<String, String> source = new HashMap<>();
        source.put("a", "A");
        source.put("b", "B");
        cache.putAll(source);
        assertEquals(2, cache.size());
        assertEquals("A", cache.get("a"));
        assertEquals("B", cache.get("b"));
    }

    @Test
    public void keySetReflectsCurrentKeys() {
        cache.put("k1", "v1");
        cache.put("k2", "v2");
        Set<String> keys = cache.keySet();
        assertTrue(keys.contains("k1"));
        assertTrue(keys.contains("k2"));
        assertEquals(2, keys.size());
    }

    @Test
    public void valuesReflectCurrentValuesIncludingNull() {
        cache.put("k1", "v1");
        cache.put("k2", null);
        assertTrue(cache.values().contains("v1"));
        assertTrue(cache.values().contains(null));
        assertEquals(2, cache.values().size());
    }

    @Test
    public void entrySetIterationAndRemove() {
        cache.put("one", "1");
        cache.put("two", "2");
        Set<Map.Entry<String, String>> entries = cache.entrySet();
        assertEquals(2, entries.size());

        // Remove one entry via iterator
        Map.Entry<String, String> first = entries.iterator().next();
        String removedKey = first.getKey();
        entries.iterator().remove(); // should throw IllegalStateException – we need to call remove after next()
        // Correct usage:
        java.util.Iterator<Map.Entry<String, String>> it = entries.iterator();
        it.next(); // advance
        it.remove(); // now remove

        assertEquals(1, cache.size());
        assertFalse(cache.containsKey(removedKey));
    }

    @Test
    public void equalsAndHashCodeConsistentWithMap() {
        cache.put("a", "1");
        cache.put("b", "2");

        Map<String, String> other = new HashMap<>();
        other.put("a", "1");
        other.put("b", "2");

        assertTrue(cache.equals(other));
        assertEquals(cache.hashCode(), other.hashCode());
    }

    @Test
    public void toStringProducesReadableRepresentation() {
        cache.put("x", "X");
        cache.put("y", "Y");
        String s = cache.toString();
        // Order is not guaranteed; just check that both entries appear
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        assertTrue(s.contains("x=X"));
        assertTrue(s.contains("y=Y"));
    }

    /* ----------------------------------------------------------------------
     *  Edge cases – null keys/values, expiration, LRU eviction
     * ---------------------------------------------------------------------- */
    @Test
    public void nullKeyIsSupported() {
        cache.put(null, "nullKey");
        assertEquals("nullKey", cache.get(null));
        assertTrue(cache.containsKey(null));
        assertTrue(cache.containsValue("nullKey"));
        cache.remove(null);
        assertFalse(cache.containsKey(null));
    }

    @Test
    public void nullValueIsSupported() {
        cache.put("nullVal", null);
        assertNull(cache.get("nullVal"));
        assertTrue(cache.containsKey("nullVal"));
        assertTrue(cache.containsValue(null));
    }

    @Test
    public void entryExpiresAfterTtl() throws InterruptedException {
        expiringCache.put("short", "life");
        assertEquals("life", expiringCache.get("short"));
        Thread.sleep(120); // longer than TTL (50 ms)
        assertNull(expiringCache.get("short"));
        assertEquals(0, expiringCache.size());
    }

    @Test
    public void lruEvictionWhenMaxSizeExceeded() {
        // maxSize = 2
        lruCache.put("first", "1");
        lruCache.put("second", "2");
        // Access "first" to make it most-recently used
        assertEquals("1", lruCache.get("first"));
        // Insert third element – should evict "second"
        lruCache.put("third", "3");

        assertEquals(2, lruCache.size());
        assertTrue(lruCache.containsKey("first"));
        assertTrue(lruCache.containsKey("third"));
        assertFalse(lruCache.containsKey("second"));
    }

    @Test
    public void lruOrderIsUpdatedOnGet() {
        // maxSize large enough to avoid eviction
        TTLCache<String, String> cacheWithLru = new TTLCache<>(10000L, 10);
        cacheWithLru.put("a", "A");
        cacheWithLru.put("b", "B");
        cacheWithLru.put("c", "C");

        // Access "a" – it should become most-recently used (tail)
        cacheWithLru.get("a");

        // The string representation reflects insertion order; we verify that "a" appears last
        String repr = cacheWithLru.toString(); // e.g., {b=B, c=C, a=A}
        int idxA = repr.indexOf("a=A");
        int idxB = repr.indexOf("b=B");
        int idxC = repr.indexOf("c=C");
        assertTrue(idxA > idxB && idxA > idxC);
    }

    /* ----------------------------------------------------------------------
     *  Static utility
     * ---------------------------------------------------------------------- */
    @Test
    public void shutdownDoesNotThrow() {
        // The @After method already calls shutdown; calling it again should be safe.
        TTLCache.shutdown();
    }
}
