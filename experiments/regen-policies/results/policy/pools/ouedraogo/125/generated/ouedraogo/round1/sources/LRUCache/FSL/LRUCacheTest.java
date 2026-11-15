package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Collection;

import org.junit.After;
import org.junit.Test;

/**
 * Comprehensive JUnit4 tests for {@link LRUCache}.
 * The tests cover both {@link LRUCache.StrategyType#LOCKING} and {@link LRUCache.StrategyType#THREADED}
 * implementations, exercising all public methods defined in the class.
 *
 * Note: The threaded implementation may evict entries asynchronously.
 * A small wait is introduced after puts to allow the cleanup task to run.
 */
public class LRUCacheTest {

    private static final int CAPACITY = 3;
    private static final int CLEANUP_DELAY = 5; // ms, small to keep tests fast

    private LRUCache<String, String> lockingCache;
    private LRUCache<String, String> threadedCache;

    @After
    public void tearDown() {
        if (lockingCache != null) {
            lockingCache.clear();
        }
        if (threadedCache != null) {
            threadedCache.shutdown();
            threadedCache.clear();
        }
    }

    /** Helper to create both caches */
    private void initCaches() {
        lockingCache = new LRUCache<>(CAPACITY, LRUCache.StrategyType.LOCKING);
        threadedCache = new LRUCache<>(CAPACITY, CLEANUP_DELAY);
    }

    /* ---------- Basic put / get / remove ---------- */

    @Test
    public void testPutGetRemoveLocking() {
        initCaches();

        // put and get
        assertNull(lockingCache.put("a", "1"));
        assertEquals("1", lockingCache.get("a"));

        // replace existing key
        assertEquals("1", lockingCache.put("a", "2"));
        assertEquals("2", lockingCache.get("a"));

        // remove
        assertEquals("2", lockingCache.remove("a"));
        assertNull(lockingCache.get("a"));
    }

    @Test
    public void testPutGetRemoveThreaded() throws InterruptedException {
        initCaches();

        assertNull(threadedCache.put("x", "alpha"));
        assertEquals("alpha", threadedCache.get("x"));

        assertEquals("alpha", threadedCache.put("x", "beta"));
        assertEquals("beta", threadedCache.get("x"));

        assertEquals("beta", threadedCache.remove("x"));
        assertNull(threadedCache.get("x"));
    }

    /* ---------- Null handling ---------- */

    @Test
    public void testNullKeyAndValueLocking() {
        initCaches();

        // null key
        assertNull(lockingCache.put(null, "nullKey"));
        assertEquals("nullKey", lockingCache.get(null));
        assertTrue(lockingCache.containsKey(null));

        // null value
        assertNull(lockingCache.put("nullValue", null));
        assertNull(lockingCache.get("nullValue"));
        assertTrue(lockingCache.containsValue(null));

        // both null
        assertNull(lockingCache.put(null, null));
        assertNull(lockingCache.get(null));
    }

    @Test
    public void testNullKeyAndValueThreaded() throws InterruptedException {
        initCaches();

        assertNull(threadedCache.put(null, "nullKey"));
        assertEquals("nullKey", threadedCache.get(null));
        assertTrue(threadedCache.containsKey(null));

        assertNull(threadedCache.put("nullValue", null));
        assertNull(threadedCache.get("nullValue"));
        assertTrue(threadedCache.containsValue(null));

        assertNull(threadedCache.put(null, null));
        assertNull(threadedCache.get(null));
    }

    /* ---------- Size, isEmpty, clear ---------- */

    @Test
    public void testSizeAndIsEmptyLocking() {
        initCaches();

        assertTrue(lockingCache.isEmpty());
        assertEquals(0, lockingCache.size());

        lockingCache.put("k1", "v1");
        assertFalse(lockingCache.isEmpty());
        assertEquals(1, lockingCache.size());

        lockingCache.clear();
        assertTrue(lockingCache.isEmpty());
        assertEquals(0, lockingCache.size());
    }

    @Test
    public void testSizeAndIsEmptyThreaded() throws InterruptedException {
        initCaches();

        assertTrue(threadedCache.isEmpty());
        assertEquals(0, threadedCache.size());

        threadedCache.put("k1", "v1");
        // give cleanup thread a moment (it shouldn't remove anything yet)
        Thread.sleep(CLEANUP_DELAY * 2);
        assertFalse(threadedCache.isEmpty());
        assertEquals(1, threadedCache.size());

        threadedCache.clear();
        assertTrue(threadedCache.isEmpty());
        assertEquals(0, threadedCache.size());
    }

    /* ---------- LRU eviction behavior ---------- */

    @Test
    public void testEvictionLocking() {
        initCaches();

        // fill to capacity
        lockingCache.put("1", "one");
        lockingCache.put("2", "two");
        lockingCache.put("3", "three");

        // access "1" to make it most-recently used
        assertEquals("one", lockingCache.get("1"));

        // add fourth entry, should evict least-recently used ("2")
        lockingCache.put("4", "four");

        assertFalse(lockingCache.containsKey("2"));
        assertTrue(lockingCache.containsKey("1"));
        assertTrue(lockingCache.containsKey("3"));
        assertTrue(lockingCache.containsKey("4"));
        assertEquals(3, lockingCache.size());
    }

    @Test
    public void testEvictionThreaded() throws InterruptedException {
        initCaches();

        threadedCache.put("a", "A");
        threadedCache.put("b", "B");
        threadedCache.put("c", "C");

        // access "a" to refresh its usage
        assertEquals("A", threadedCache.get("a"));

        // insert fourth entry; eviction happens asynchronously
        threadedCache.put("d", "D");
        Thread.sleep(CLEANUP_DELAY * 5); // give cleanup thread time to run

        // "b" should be evicted (least recently used)
        assertFalse(threadedCache.containsKey("b"));
        assertTrue(threadedCache.containsKey("a"));
        assertTrue(threadedCache.containsKey("c"));
        assertTrue(threadedCache.containsKey("d"));
        assertEquals(3, threadedCache.size());
    }

    /* ---------- putAll ---------- */

    @Test
    public void testPutAllLocking() {
        initCaches();

        Map<String, String> map = new HashMap<>();
        map.put("x", "X");
        map.put("y", "Y");
        map.put("z", "Z");

        lockingCache.putAll(map);
        assertEquals(3, lockingCache.size());
        assertEquals("X", lockingCache.get("x"));
        assertEquals("Y", lockingCache.get("y"));
        assertEquals("Z", lockingCache.get("z"));
    }

    @Test
    public void testPutAllThreaded() throws InterruptedException {
        initCaches();

        Map<String, String> map = new HashMap<>();
        map.put("p", "P");
        map.put("q", "Q");
        map.put("r", "R");

        threadedCache.putAll(map);
        Thread.sleep(CLEANUP_DELAY * 5); // allow possible eviction

        // capacity is 3, so all should be present
        assertEquals(3, threadedCache.size());
        assertEquals("P", threadedCache.get("p"));
        assertEquals("Q", threadedCache.get("q"));
        assertEquals("R", threadedCache.get("r"));
    }

    /* ---------- collection views ---------- */

    @Test
    public void testKeySetAndValuesLocking() {
        initCaches();

        lockingCache.put("k1", "v1");
        lockingCache.put("k2", "v2");

        Set<String> keys = lockingCache.keySet();
        Collection<String> values = lockingCache.values();

        assertTrue(keys.contains("k1"));
        assertTrue(keys.contains("k2"));
        assertEquals(2, keys.size());

        assertTrue(values.contains("v1"));
        assertTrue(values.contains("v2"));
        assertEquals(2, values.size());
    }

    @Test
    public void testEntrySetThreaded() throws InterruptedException {
        initCaches();

        threadedCache.put("e1", "val1");
        threadedCache.put("e2", "val2");
        Thread.sleep(CLEANUP_DELAY * 2);

        Set<Map.Entry<String, String>> entries = threadedCache.entrySet();
        assertEquals(2, entries.size());

        boolean foundE1 = false, foundE2 = false;
        for (Map.Entry<String, String> entry : entries) {
            if ("e1".equals(entry.getKey())) {
                foundE1 = true;
                assertEquals("val1", entry.getValue());
            }
            if ("e2".equals(entry.getKey())) {
                foundE2 = true;
                assertEquals("val2", entry.getValue());
            }
        }
        assertTrue(foundE1);
        assertTrue(foundE2);
    }

    /* ---------- equals, hashCode, toString ---------- */

    @Test
    public void testEqualsAndHashCodeLocking() {
        initCaches();

        Map<String, String> other = new HashMap<>();
        other.put("a", "1");
        other.put("b", "2");

        lockingCache.putAll(other);

        assertTrue(lockingCache.equals(other));
        assertEquals(other.hashCode(), lockingCache.hashCode());

        // compare with itself
        assertTrue(lockingCache.equals(lockingCache));
    }

    @Test
    public void testEqualsAndHashCodeThreaded() throws InterruptedException {
        initCaches();

        Map<String, String> other = new HashMap<>();
        other.put("x", "10");
        other.put("y", "20");

        threadedCache.putAll(other);
        Thread.sleep(CLEANUP_DELAY * 2);

        assertTrue(threadedCache.equals(other));
        assertEquals(other.hashCode(), threadedCache.hashCode());
    }

    @Test
    public void testToStringLocking() {
        initCaches();

        lockingCache.put("k", "v");
        String s = lockingCache.toString();
        assertNotNull(s);
        assertTrue(s.contains("k"));
        assertTrue(s.contains("v"));
    }

    @Test
    public void testToStringThreaded() throws InterruptedException {
        initCaches();

        threadedCache.put("alpha", "A");
        Thread.sleep(CLEANUP_DELAY * 2);
        String s = threadedCache.toString();
        assertNotNull(s);
        assertTrue(s.contains("alpha"));
        assertTrue(s.contains("A"));
    }

    /* ---------- shutdown for threaded strategy ---------- */

    @Test
    public void testShutdownThreaded() {
        // Construct a threaded cache and invoke shutdown; no exception means success.
        LRUCache<String, String> cache = new LRUCache<>(CAPACITY, LRUCache.StrategyType.THREADED);
        cache.shutdown(); // should call ThreadedLRUCacheStrategy.shutdown()
        // after shutdown we can still use the cache (no guarantee, but should not throw)
        cache.put("k", "v");
        assertEquals("v", cache.get("k"));
    }
}
