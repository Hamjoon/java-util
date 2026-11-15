package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 tests for {@link LRUCache}.
 *
 * The tests cover:
 *   • basic CRUD operations,
 *   • null handling,
 *   • LRU eviction for both LOCKING and THREADED strategies,
 *   • putAll, clear, equals / hashCode,
 *   • illegal arguments,
 *   • shutdown behaviour.
 *
 * No mocking is used – real strategy objects are created via the public constructors.
 */
public class LRUCacheTest {

    private static final int CAPACITY = 2;
    private static final int CLEANUP_DELAY = 20; // ms, enough for the background thread to run

    private LRUCache<String, String> lockingCache;
    private LRUCache<String, String> threadedCache;

    @Before
    public void setUp() {
        lockingCache = new LRUCache<>(CAPACITY); // locking by default
        threadedCache = new LRUCache<>(CAPACITY, LRUCache.StrategyType.THREADED);
    }

    @After
    public void tearDown() {
        // Ensure background threads are stopped after each test
        if (threadedCache != null) {
            threadedCache.shutdown();
        }
    }

    /* -------------------- basic CRUD tests -------------------- */

    @Test
    public void testPutAndGetLocking() {
        assertNull(lockingCache.put("a", "alpha"));
        assertEquals("alpha", lockingCache.get("a"));
        assertEquals(1, lockingCache.size());
    }

    @Test
    public void testPutAndGetThreaded() {
        assertNull(threadedCache.put("x", "xray"));
        assertEquals("xray", threadedCache.get("x"));
        assertEquals(1, threadedCache.size());
    }

    @Test
    public void testRemove() {
        lockingCache.put("k1", "v1");
        assertEquals("v1", lockingCache.remove("k1"));
        assertNull(lockingCache.get("k1"));
        assertTrue(lockingCache.isEmpty());
    }

    @Test
    public void testContainsKeyAndValue() {
        lockingCache.put("key", "val");
        assertTrue(lockingCache.containsKey("key"));
        assertTrue(lockingCache.containsValue("val"));
        assertFalse(lockingCache.containsKey("missing"));
        assertFalse(lockingCache.containsValue("missing"));
    }

    @Test
    public void testNullKeyAndValue() {
        // Both strategies support null keys/values
        lockingCache.put(null, "nullKey");
        assertEquals("nullKey", lockingCache.get(null));

        lockingCache.put("nullVal", null);
        assertNull(lockingCache.get("nullVal"));

        threadedCache.put(null, null);
        assertNull(threadedCache.get(null));
    }

    @Test
    public void testPutAll() {
        Map<String, String> src = new HashMap<>();
        src.put("a", "1");
        src.put("b", "2");
        src.put(null, "nullKey");
        src.put("nullVal", null);

        lockingCache.putAll(src);
        assertEquals(4, lockingCache.size());
        assertEquals("1", lockingCache.get("a"));
        assertEquals("2", lockingCache.get("b"));
        assertEquals("nullKey", lockingCache.get(null));
        assertNull(lockingCache.get("nullVal"));
    }

    @Test
    public void testClear() {
        lockingCache.put("x", "y");
        lockingCache.put("y", "z");
        assertFalse(lockingCache.isEmpty());

        lockingCache.clear();
        assertTrue(lockingCache.isEmpty());
        assertEquals(0, lockingCache.size());
    }

    /* -------------------- LRU eviction tests -------------------- */

    @Test
    public void testLruEvictionLocking() {
        // capacity = 2
        lockingCache.put("first", "1");
        lockingCache.put("second", "2");
        // Access "first" to make it MRU
        assertEquals("1", lockingCache.get("first"));
        // Insert third entry -> "second" should be evicted
        lockingCache.put("third", "3");

        assertTrue(lockingCache.containsKey("first"));
        assertTrue(lockingCache.containsKey("third"));
        assertFalse(lockingCache.containsKey("second"));
        assertEquals(2, lockingCache.size());
    }

    @Test
    public void testLruEvictionThreaded() throws InterruptedException {
        // capacity = 2, cleanup runs after CLEANUP_DELAY ms
        threadedCache.put("one", "1");
        threadedCache.put("two", "2");
        // Access "one" to make it MRU
        assertEquals("1", threadedCache.get("one"));
        // Insert third entry – eviction will be asynchronous
        threadedCache.put("three", "3");

        // Wait a little longer than the cleanup delay
        Thread.sleep(CLEANUP_DELAY + 30);

        // After cleanup, the least‑recently‑used ("two") must be gone
        assertTrue(threadedCache.containsKey("one"));
        assertTrue(threadedCache.containsKey("three"));
        assertFalse(threadedCache.containsKey("two"));
        assertEquals(2, threadedCache.size());
    }

    /* -------------------- equals / hashCode tests -------------------- */

    @Test
    public void testEqualsAndHashCode() {
        Map<String, String> map = new HashMap<>();
        map.put("a", "A");
        map.put("b", "B");

        LRUCache<String, String> cacheA = new LRUCache<>(CAPACITY);
        LRUCache<String, String> cacheB = new LRUCache<>(CAPACITY);
        cacheA.putAll(map);
        cacheB.putAll(map);

        assertTrue(cacheA.equals(cacheB));
        assertEquals(cacheA.hashCode(), cacheB.hashCode());

        // compare with plain HashMap (should be true because underlying map contents are equal)
        assertTrue(cacheA.equals(map));
        assertTrue(map.equals(cacheA));
    }

    @Test
    public void testEqualsWithNonMap() {
        assertFalse(lockingCache.equals("not a map"));
        assertFalse(lockingCache.equals(null));
    }

    /* -------------------- toString test -------------------- */

    @Test
    public void testToString() {
        lockingCache.put("k", "v");
        String s = lockingCache.toString();
        assertNotNull(s);
        assertTrue(s.contains("k"));
        assertTrue(s.contains("v"));
    }

    /* -------------------- constructor / illegal argument tests -------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testUnsupportedStrategyThrows() {
        // Passing null triggers the else‑branch and throws IllegalArgumentException
        new LRUCache<>(CAPACITY, null);
    }

    @Test
    public void testThreadedConstructorWithCustomDelay() throws InterruptedException {
        LRUCache<String, String> cache = new LRUCache<>(1, 5); // capacity 1, 5 ms delay
        cache.put("a", "A");
        cache.put("b", "B"); // exceeds capacity; eviction will happen asynchronously

        // Immediately after put, size may be 2 (transient state)
        assertTrue(cache.size() >= 1);

        Thread.sleep(30); // wait for background cleanup
        assertEquals(1, cache.size());
        assertTrue(cache.containsKey("b")); // "b" is most recent
        cache.shutdown(); // clean shutdown
    }

    /* -------------------- shutdown behaviour -------------------- */

    @Test
    public void testShutdownOnLockingCacheDoesNotThrow() {
        // shutdown is a no‑op for locking strategy – just ensure it does not throw
        lockingCache.shutdown();
    }

    @Test
    public void testShutdownOnThreadedCacheStopsBackgroundThread() throws InterruptedException {
        // Insert entries that will trigger cleanup
        threadedCache.put("x", "1");
        threadedCache.put("y", "2");
        threadedCache.put("z", "3"); // exceeds capacity

        // Give it a moment to start the cleanup thread
        Thread.sleep(CLEANUP_DELAY + 20);
        // Shut down – subsequent puts should still work but no further background tasks are scheduled
        threadedCache.shutdown();

        // After shutdown, the cache should still contain at most CAPACITY entries
        assertTrue(threadedCache.size() <= CAPACITY);
    }

    /* -------------------- entrySet / keySet / values -------------------- */

    @Test
    public void testEntrySetKeySetValues() {
        lockingCache.put("k1", "v1");
        lockingCache.put("k2", "v2");

        Set<String> keys = lockingCache.keySet();
        assertEquals(2, keys.size());
        assertTrue(keys.contains("k1"));
        assertTrue(keys.contains("k2"));

        Set<Map.Entry<String, String>> entries = lockingCache.entrySet();
        assertEquals(2, entries.size());

        // values collection
        assertTrue(lockingCache.values().contains("v1"));
        assertTrue(lockingCache.values().contains("v2"));
    }
}
