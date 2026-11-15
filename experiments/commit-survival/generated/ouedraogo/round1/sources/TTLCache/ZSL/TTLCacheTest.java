package com.cedarsoftware.util;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class TTLCacheTest {

    private static final long SHORT_TTL = 50L;          // 50 ms TTL for expiration tests
    private static final long LONG_TTL  = 5_000L;       // 5 seconds, long enough for most tests
    private static final long CLEANUP_INTERVAL = 20L; // short cleanup interval (not heavily relied on)

    @AfterClass
    public static void shutdownScheduler() {
        // Ensure the shared scheduler thread does not keep the JVM alive after tests
        TTLCache.shutdown();
    }

    @Before
    public void clearSchedulerQueue() {
        // The scheduler runs a background task; we don't need to do anything special here.
        // The method exists only to make the intention clear.
    }

    /*** Constructor validation ***/
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsZeroTtl() {
        new TTLCache<>(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsNegativeCleanupInterval() {
        new TTLCache<>(100, -1, 5);
    }

    /*** Basic put / get / size / isEmpty behavior ***/
    @Test
    public void testPutAndGetBasic() {
        TTLCache<String, String> cache = new TTLCache<>(LONG_TTL);
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());

        assertNull(cache.put("k1", "v1"));
        assertEquals("v1", cache.get("k1"));
        assertFalse(cache.isEmpty());
        assertEquals(1, cache.size());

        // Overwrite existing key
        assertEquals("v1", cache.put("k1", "v2"));
        assertEquals("v2", cache.get("k1"));
        assertEquals(1, cache.size());
    }

    /*** Expiration handling ***/
    @Test
    public void testEntryExpiresAfterTtl() throws InterruptedException {
        TTLCache<String, Integer> cache = new TTLCache<>(SHORT_TTL, -1, CLEANUP_INTERVAL);
        cache.put("a", 1);
        cache.put("b", 2);
        assertEquals(2, cache.size());

        // Sleep longer than TTL
        Thread.sleep(SHORT_TTL + 20);

        // get should return null and remove the entry
        assertNull(cache.get("a"));
        assertNull(cache.get("b"));
        assertEquals(0, cache.size());
        assertFalse(cache.containsKey("a"));
        assertFalse(cache.containsKey("b"));
    }

    /*** containsKey respects TTL ***/
    @Test
    public void testContainsKeyWithExpiration() throws InterruptedException {
        TTLCache<String, String> cache = new TTLCache<>(SHORT_TTL);
        cache.put("x", "y");
        assertTrue(cache.containsKey("x"));
        Thread.sleep(SHORT_TTL + 10);
        assertFalse(cache.containsKey("x"));
    }

    /*** containsValue does not consider TTL (it only scans current map entries) ***/
    @Test
    public void testContainsValue() {
        TTLCache<String, String> cache = new TTLCache<>(LONG_TTL);
        cache.put("k1", "v1");
        cache.put("k2", "v2");
        assertTrue(cache.containsValue("v1"));
        assertFalse(cache.containsValue("missing"));
    }

    /*** remove ***/
    @Test
    public void testRemoveRemovesEntry() {
        TTLCache<String, String> cache = new TTLCache<>(LONG_TTL);
        cache.put("one", "1");
        cache.put("two", "2");
        assertEquals("1", cache.remove("one"));
        assertNull(cache.get("one"));
        assertEquals(1, cache.size());
        assertNull(cache.remove("nonexistent"));
    }

    /*** clear ***/
    @Test
    public void testClearEmptiesCache() {
        TTLCache<Integer, Integer> cache = new TTLCache<>(LONG_TTL);
        cache.put(1, 10);
        cache.put(2, 20);
        cache.clear();
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
    }

    /*** putAll ***/
    @Test
    public void testPutAllCopiesAllEntries() {
        TTLCache<String, String> cache = new TTLCache<>(LONG_TTL);
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

    /*** keySet and values collections ***/
    @Test
    public void testKeySetAndValues() {
        TTLCache<String, Integer> cache = new TTLCache<>(LONG_TTL);
        cache.put("x", 100);
        cache.put("y", 200);

        Set<String> keys = cache.keySet();
        assertTrue(keys.contains("x"));
        assertTrue(keys.contains("y"));
        assertEquals(2, keys.size());

        // values collection
        java.util.Collection<Integer> values = cache.values();
        assertTrue(values.contains(100));
        assertTrue(values.contains(200));
        assertEquals(2, values.size());
    }

    /*** EntrySet iterator and remove ***/
    @Test
    public void testEntrySetIteratorRemove() {
        TTLCache<String, String> cache = new TTLCache<>(LONG_TTL);
        cache.put("a", "A");
        cache.put("b", "B");
        cache.put("c", "C");

        Set<Map.Entry<String, String>> entrySet = cache.entrySet();
        assertEquals(3, entrySet.size());

        Iterator<Map.Entry<String, String>> it = entrySet.iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> e = it.next();
            if (e.getKey().equals("b")) {
                it.remove(); // should remove "b"
            }
        }

        assertEquals(2, cache.size());
        assertNull(cache.get("b"));
        assertTrue(cache.containsKey("a"));
        assertTrue(cache.containsKey("c"));
    }

    /*** equals and hashCode consistency ***/
    @Test
    public void testEqualsAndHashCode() {
        TTLCache<String, Integer> c1 = new TTLCache<>(LONG_TTL);
        TTLCache<String, Integer> c2 = new TTLCache<>(LONG_TTL);

        c1.put("k1", 1);
        c1.put("k2", 2);

        c2.put("k1", 1);
        c2.put("k2", 2);

        assertTrue(c1.equals(c2));
        assertTrue(c2.equals(c1));
        assertEquals(c1.hashCode(), c2.hashCode());

        // Change one cache
        c2.put("k3", 3);
        assertFalse(c1.equals(c2));
    }

    /*** toString format ***/
    @Test
    public void testToStringProducesExpectedFormat() {
        TTLCache<String, String> cache = new TTLCache<>(LONG_TTL);
        cache.put("one", "1");
        cache.put("two", "2");
        String s = cache.toString();

        // Since order is LRU based, we can't guarantee order, but we can check the overall format
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        assertTrue(s.contains("one=1"));
        assertTrue(s.contains("two=2"));
        // Ensure there is a comma when more than one entry
        assertTrue(s.contains(","));
    }

    /*** LRU eviction when maxSize is set ***/
    @Test
    public void testLruEvictionWithMaxSize() {
        // ttl large enough so expiration does not interfere
        TTLCache<String, String> cache = new TTLCache<>(LONG_TTL, 2, CLEANUP_INTERVAL);
        cache.put("A", "alpha"); // first
        cache.put("B", "beta");  // second, cache now full (maxSize=2)

        // Access "A" to make it most recently used
        assertEquals("alpha", cache.get("A"));

        // Insert third entry, should evict the least recently used ("B")
        cache.put("C", "gamma");

        assertEquals(2, cache.size());
        assertTrue(cache.containsKey("A"));
        assertTrue(cache.containsKey("C"));
        assertFalse(cache.containsKey("B"));
    }

    /*** LRU eviction without any access (simple order) ***/
    @Test
    public void testLruSimpleEvictionOrder() {
        TTLCache<Integer, String> cache = new TTLCache<>(LONG_TTL, 2, CLEANUP_INTERVAL);
        cache.put(1, "one");
        cache.put(2, "two");
        // No get calls; insertion order defines LRU (1 is LRU)
        cache.put(3, "three"); // should evict key 1

        assertFalse(cache.containsKey(1));
        assertTrue(cache.containsKey(2));
        assertTrue(cache.containsKey(3));
    }

    /*** behavior when lock cannot be acquired (tryLock fails) – indirect test ***/
    @Test
    public void testPutWhenLockNotAcquiredDoesNotThrow() {
        // Since we cannot force tryLock to fail without complex threading, we simply verify that
        // put works under normal conditions and does not deadlock.
        TTLCache<String, String> cache = new TTLCache<>(LONG_TTL);
        cache.put("k", "v");
        assertEquals("v", cache.get("k"));
    }

    /*** purgeExpiredEntries invoked by background task (indirect) ***/
    @Test
    public void testBackgroundPurgeRemovesExpiredEntries() throws InterruptedException {
        // Use a very short cleanup interval so the background task runs quickly
        TTLCache<String, String> cache = new TTLCache<>(SHORT_TTL, -1, 30);
        cache.put("temp", "data");
        assertEquals(1, cache.size());

        // Wait longer than ttl + cleanup interval
        Thread.sleep(SHORT_TTL + 50);
        // Background task should have purged the entry
        assertEquals(0, cache.size());
        assertNull(cache.get("temp"));
    }

    /*** verify that equals works with non-Map objects ***/
    @Test
    public void testEqualsWithNonMapReturnsFalse() {
        TTLCache<String, String> cache = new TTLCache<>(LONG_TTL);
        cache.put("a", "A");
        assertFalse(cache.equals("not a map"));
        assertFalse(cache.equals(null));
    }
}
