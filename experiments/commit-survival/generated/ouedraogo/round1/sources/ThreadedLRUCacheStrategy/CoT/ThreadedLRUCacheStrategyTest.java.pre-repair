package com.cedarsoftware.util.cache;

import org.junit.*;
import java.util.*;

import static org.junit.Assert.*;

public class ThreadedLRUCacheStrategyTest {

    private static final int SMALL_CAPACITY = 2;
    private static final int CLEANUP_DELAY = 20; // milliseconds (fast for tests)

    private ThreadedLRUCacheStrategy<String, String> cache;

    @Before
    public void setUp() {
        cache = new ThreadedLRUCacheStrategy<>(SMALL_CAPACITY, CLEANUP_DELAY);
    }

    @After
    public void tearDown() {
        cache.clear();
    }

    @AfterClass
    public static void afterAll() {
        // Shut down the shared scheduler after all tests have run
        ThreadedLRUCacheStrategy.shutdown();
    }

    /* ---------------------------------------------------------------------- */
    /*  Public method signatures (extracted)                               */
    /* ---------------------------------------------------------------------- */
    // public ThreadedLRUCacheStrategy(int capacity, int cleanupDelayMillis)
    // public static void shutdown()
    // public V get(Object key)
    // public V put(K key, V value)
    // public void putAll(Map<? extends K, ? extends V> m)
    // public boolean isEmpty()
    // public V remove(Object key)
    // public void clear()
    // public int size()
    // public boolean containsKey(Object key)
    // public boolean containsValue(Object value)
    // public Set<Map.Entry<K, V>> entrySet()
    // public Set<K> keySet()
    // public Collection<V> values()
    // public boolean equals(Object o)
    // public int hashCode()
    // public String toString()

    /* ---------------------------------------------------------------------- */
    /*  Basic functionality tests                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testPutAndGet() {
        assertNull(cache.put("one", "1"));
        assertEquals("1", cache.get("one"));
        assertNull(cache.get("missing"));
    }

    @Test
    public void testPutOverwriteUpdatesTimestamp() throws InterruptedException {
        cache.put("key", "v1");
        Thread.sleep(5);
        cache.put("key", "v2"); // overwrite
        assertEquals("v2", cache.get("key"));
    }

    @Test
    public void testPutAll() {
        Map<String, String> source = new HashMap<>();
        source.put("a", "A");
        source.put("b", "B");
        cache.putAll(source);
        assertEquals(2, cache.size());
        assertEquals("A", cache.get("a"));
        assertEquals("B", cache.get("b"));
    }

    @Test
    public void testIsEmptyAndClear() {
        assertTrue(cache.isEmpty());
        cache.put("x", "X");
        assertFalse(cache.isEmpty());
        cache.clear();
        assertTrue(cache.isEmpty());
    }

    @Test
    public void testRemove() {
        cache.put("k", "v");
        assertEquals("v", cache.remove("k"));
        assertNull(cache.get("k"));
        assertNull(cache.remove("nonexistent"));
    }

    @Test
    public void testSize() {
        assertEquals(0, cache.size());
        cache.put("1", "one");
        assertEquals(1, cache.size());
    }

    @Test
    public void testContainsKeyAndContainsValue() {
        cache.put(null, "nullKey");
        cache.put("nullValue", null);
        assertTrue(cache.containsKey(null));
        assertTrue(cache.containsValue(null));
        assertTrue(cache.containsKey("nullValue"));
        assertFalse(cache.containsKey("missing"));
        assertFalse(cache.containsValue("missing"));
    }

    @Test
    public void testEntrySetImmutability() {
        cache.put("k", "v");
        Set<Map.Entry<String, String>> entrySet = cache.entrySet();
        assertEquals(1, entrySet.size());
        try {
            entrySet.add(new AbstractMap.SimpleEntry<>("x", "y"));
            fail("entrySet should be immutable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    @Test
    public void testKeySetImmutability() {
        cache.put("k", "v");
        Set<String> keySet = cache.keySet();
        assertEquals(1, keySet.size());
        try {
            keySet.add("newKey");
            fail("keySet should be immutable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    @Test
    public void testValuesImmutability() {
        cache.put("k", "v");
        Collection<String> values = cache.values();
        assertEquals(1, values.size());
        try {
            values.add("newVal");
            fail("values collection should be immutable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    @Test
    public void testEqualsAndHashCode() {
        cache.put("a", "A");
        cache.put("b", "B");

        Map<String, String> other = new HashMap<>();
        other.put("a", "A");
        other.put("b", "B");

        assertTrue(cache.equals(other));
        assertEquals(cache.hashCode(), other.hashCode());
    }

    @Test
    public void testToStringWithSelfReference() {
        cache.put("self", cache); // store a self reference
        String s = cache.toString();
        assertTrue("toString should replace self reference with placeholder",
                s.contains("(this Map)"));
    }

    /* ---------------------------------------------------------------------- */
    /*  Edge case / exception handling tests                                 */
    /* ---------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorInvalidCapacity() {
        new ThreadedLRUCacheStrategy<>(0, 100);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorInvalidDelay() {
        new ThreadedLRUCacheStrategy<>(10, 5);
    }

    @Test
    public void testLRUEviction() throws InterruptedException {
        // capacity = 2, delay = 20ms
        cache.put("first", "1");
        Thread.sleep(5);
        cache.put("second", "2");
        Thread.sleep(5);
        cache.put("third", "3"); // exceeds capacity, triggers cleanup after delay

        // Wait enough time for the scheduled cleanup to run
        Thread.sleep(CLEANUP_DELAY * 3);

        // The cache should contain only the two most recently used entries
        assertEquals(2, cache.size());
        assertNull("Oldest entry should have been evicted", cache.get("first"));
        assertEquals("2", cache.get("second"));
        assertEquals("3", cache.get("third"));
    }

    @Test
    public void testGetUpdatesTimestampPreventsEviction() throws InterruptedException {
        cache.put("a", "A");
        cache.put("b", "B");
        Thread.sleep(5);
        // Access 'a' to update its timestamp
        cache.get("a");
        Thread.sleep(5);
        cache.put("c", "C"); // triggers cleanup

        Thread.sleep(CLEANUP_DELAY * 3);

        // 'b' should be evicted because 'a' was accessed more recently
        assertNull(cache.get("b"));
        assertEquals("A", cache.get("a"));
        assertEquals("C", cache.get("c"));
    }

    @Test
    public void testShutdownDoesNotThrow() {
        // The static shutdown method should complete without exception
        ThreadedLRUCacheStrategy.shutdown();
        // Re‑initialise a new cache after shutdown to ensure the scheduler can be used again
        // (the implementation creates a new scheduler only on class load, so this just checks no NPE)
        ThreadedLRUCacheStrategy<String, String> newCache = new ThreadedLRUCacheStrategy<>(1, 10);
        newCache.put("x", "X");
        assertEquals("X", newCache.get("x"));
        newCache.clear();
    }
}
