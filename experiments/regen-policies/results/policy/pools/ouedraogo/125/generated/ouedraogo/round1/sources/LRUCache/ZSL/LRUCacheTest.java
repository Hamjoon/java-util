package com.cedarsoftware.util;

import com.cedarsoftware.util.LRUCache.StrategyType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class LRUCacheTest {

    private LRUCache<String, Integer> lockingCache;
    private LRUCache<String, Integer> threadedCache;

    @Before
    public void setUp() {
        // capacity 3 for easy eviction testing
        lockingCache = new LRUCache<>(3); // defaults to LOCKING
        threadedCache = new LRUCache<>(3, StrategyType.THREADED);
    }

    @After
    public void tearDown() {
        // clean up the thread‑pool used by the threaded strategy
        if (threadedCache != null) {
            threadedCache.shutdown();
        }
    }

    /* -------------------- basic Map contract tests -------------------- */

    @Test
    public void testPutAndGetLocking() {
        assertNull(lockingCache.put("a", 1));
        assertEquals(Integer.valueOf(1), lockingCache.get("a"));
        assertNull(lockingCache.put("b", 2));
        assertEquals(Integer.valueOf(2), lockingCache.get("b"));
    }

    @Test
    public void testPutAndGetThreaded() {
        assertNull(threadedCache.put("x", 10));
        assertEquals(Integer.valueOf(10), threadedCache.get("x"));
        assertNull(threadedCache.put("y", 20));
        assertEquals(Integer.valueOf(20), threadedCache.get("y"));
    }

    @Test
    public void testContainsKeyAndValue() {
        lockingCache.put("k1", 100);
        assertTrue(lockingCache.containsKey("k1"));
        assertTrue(lockingCache.containsValue(100));
        assertFalse(lockingCache.containsKey("missing"));
        assertFalse(lockingCache.containsValue(999));
    }

    @Test
    public void testRemoveAndSize() {
        lockingCache.put("one", 1);
        lockingCache.put("two", 2);
        assertEquals(2, lockingCache.size());
        assertEquals(Integer.valueOf(1), lockingCache.remove("one"));
        assertEquals(1, lockingCache.size());
        assertNull(lockingCache.get("one"));
    }

    @Test
    public void testClearAndIsEmpty() {
        threadedCache.put("a", 1);
        threadedCache.put("b", 2);
        assertFalse(threadedCache.isEmpty());
        threadedCache.clear();
        assertTrue(threadedCache.isEmpty());
        assertEquals(0, threadedCache.size());
    }

    @Test
    public void testPutAll() {
        Map<String, Integer> src = new HashMap<>();
        src.put("p", 5);
        src.put("q", 6);
        src.put("r", 7);
        lockingCache.putAll(src);
        assertEquals(3, lockingCache.size());
        assertEquals(Integer.valueOf(6), lockingCache.get("q"));
    }

    @Test
    public void testKeySetValuesEntrySet() {
        lockingCache.put("k1", 10);
        lockingCache.put("k2", 20);
        Set<String> keys = lockingCache.keySet();
        Collection<Integer> values = lockingCache.values();
        Set<Map.Entry<String, Integer>> entries = lockingCache.entrySet();

        assertTrue(keys.containsAll(Arrays.asList("k1", "k2")));
        assertTrue(values.containsAll(Arrays.asList(10, 20)));
        assertEquals(2, entries.size());

        // modify through entrySet iterator and verify map reflects change
        for (Map.Entry<String, Integer> e : entries) {
            e.setValue(e.getValue() + 1);
        }
        assertEquals(Integer.valueOf(11), lockingCache.get("k1"));
        assertEquals(Integer.valueOf(21), lockingCache.get("k2"));
    }

    @Test
    public void testEqualsAndHashCode() {
        LRUCache<String, Integer> another = new LRUCache<>(3);
        lockingCache.put("a", 1);
        lockingCache.put("b", 2);
        another.put("a", 1);
        another.put("b", 2);
        assertTrue(lockingCache.equals(another));
        assertEquals(lockingCache.hashCode(), another.hashCode());

        // compare with a plain HashMap having same entries
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertTrue(lockingCache.equals(map));
        assertTrue(map.equals(lockingCache));
    }

    @Test
    public void testToString() {
        lockingCache.put("x", 99);
        String s = lockingCache.toString();
        assertNotNull(s);
        assertTrue(s.contains("x"));
        assertTrue(s.contains("99"));
    }

    /* -------------------- LRU eviction behavior -------------------- */

    @Test
    public void testLockingEvictionOrder() {
        // capacity is 3
        lockingCache.put("1", 1);
        lockingCache.put("2", 2);
        lockingCache.put("3", 3);
        // access "1" to make it most recently used
        assertEquals(Integer.valueOf(1), lockingCache.get("1"));
        // add fourth entry, should evict least recently used ("2")
        lockingCache.put("4", 4);
        assertNull(lockingCache.get("2"));
        assertEquals(Integer.valueOf(1), lockingCache.get("1"));
        assertEquals(Integer.valueOf(3), lockingCache.get("3"));
        assertEquals(Integer.valueOf(4), lockingCache.get("4"));
    }

    @Test
    public void testThreadedEvictionAfterDelay() throws InterruptedException {
        // capacity 3, cleanup delay default 10ms (set via constructor)
        LRUCache<String, Integer> cache = new LRUCache<>(3, StrategyType.THREADED);
        try {
            cache.put("a", 1);
            cache.put("b", 2);
            cache.put("c", 3);
            // access "a" to mark recent
            assertEquals(Integer.valueOf(1), cache.get("a"));
            // add a fourth entry; eviction happens asynchronously
            cache.put("d", 4);
            // give the background thread time to run
            Thread.sleep(50);
            // "b" should have been evicted (least recently used)
            assertNull(cache.get("b"));
            assertEquals(Integer.valueOf(1), cache.get("a"));
            assertEquals(Integer.valueOf(3), cache.get("c"));
            assertEquals(Integer.valueOf(4), cache.get("d"));
        } finally {
            cache.shutdown();
        }
    }

    @Test
    public void testNullKeyAndValueSupported() {
        // both strategies accept null keys/values
        lockingCache.put(null, 123);
        lockingCache.put("nullValue", null);
        assertEquals(Integer.valueOf(123), lockingCache.get(null));
        assertNull(lockingCache.get("nullValue"));
        assertTrue(lockingCache.containsKey(null));
        assertTrue(lockingCache.containsValue(null));
    }

    @Test
    public void testShutdownDoesNotThrow() {
        // shutdown should be safe to call multiple times
        threadedCache.shutdown();
        threadedCache.shutdown(); // second call should be a no‑op
    }
}
