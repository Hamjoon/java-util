package com.cedarsoftware.util.cache;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link LockingLRUCacheStrategy}.
 *
 * The tests cover:
 * <ul>
 *   <li>basic CRUD operations</li>
 *   <li>capacity based eviction (LRU behaviour)</li>
 *   <li>handling of {@code null} keys/values</li>
 *   <li>behaviour of collection view methods (keySet, values, entrySet)</li>
 *   <li>equality, hashCode and toString (including self‑reference handling)</li>
 *   <li>edge cases such as zero capacity and lock contention</li>
 * </ul>
 */
public class LockingLRUCacheStrategyTest {

    private LockingLRUCacheStrategy<String, Object> cache;

    @Before
    public void setUp() {
        // default capacity of 2 for most tests; individual tests may override
        cache = new LockingLRUCacheStrategy<>(2);
    }

    /* -------------------------------------------------
     *  Basic put / get / size / isEmpty tests
     * ------------------------------------------------- */
    @Test
    public void testPutAndGetBasic() {
        assertNull(cache.put("a", "alpha"));
        assertEquals("alpha", cache.get("a"));
        assertEquals(1, cache.size());
        assertFalse(cache.isEmpty());
    }

    @Test
    public void testOverwriteExistingKey() {
        cache.put("k", "v1");
        // put returns the *new* value (implementation detail)
        assertEquals("v2", cache.put("k", "v2"));
        assertEquals("v2", cache.get("k"));
        assertEquals(1, cache.size());
    }

    @Test
    public void testNullKeyAndValue() {
        cache.put(null, null);
        assertTrue(cache.containsKey(null));
        assertTrue(cache.containsValue(null));
        assertNull(cache.get(null));
        assertEquals(1, cache.size());
    }

    /* -------------------------------------------------
     *  Capacity / LRU eviction tests
     * ------------------------------------------------- */
    @Test
    public void testCapacityEviction() {
        cache.put("first", 1);
        cache.put("second", 2);
        // cache is full (capacity 2). Adding a third entry should evict "first"
        cache.put("third", 3);
        assertNull("First entry should have been evicted", cache.get("first"));
        assertEquals(Integer.valueOf(2), cache.get("second"));
        assertEquals(Integer.valueOf(3), cache.get("third"));
        assertEquals(2, cache.size());
    }

    @Test
    public void testZeroCapacityNeverStores() {
        LockingLRUCacheStrategy<String, String> zeroCache = new LockingLRUCacheStrategy<>(0);
        assertNull(zeroCache.put("x", "y"));
        assertNull(zeroCache.get("x"));
        assertEquals(0, zeroCache.size());
    }

    /* -------------------------------------------------
     *  Remove / clear tests
     * ------------------------------------------------- */
    @Test
    public void testRemoveExisting() {
        cache.put("k", "v");
        assertEquals("v", cache.remove("k"));
        assertNull(cache.get("k"));
        assertEquals(0, cache.size());
    }

    @Test
    public void testRemoveNonExistingReturnsNull() {
        assertNull(cache.remove("missing"));
    }

    @Test
    public void testClear() {
        cache.put("a", 1);
        cache.put("b", 2);
        cache.clear();
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
        assertNull(cache.get("a"));
        assertNull(cache.get("b"));
    }

    /* -------------------------------------------------
     *  containsKey / containsValue tests
     * ------------------------------------------------- */
    @Test
    public void testContainsKey() {
        cache.put("present", "yes");
        assertTrue(cache.containsKey("present"));
        assertFalse(cache.containsKey("absent"));
    }

    @Test
    public void testContainsValueIncludingNullAndSelfReference() {
        cache.put("nullVal", null);
        cache.put("self", cache); // self‑reference as a value
        assertTrue(cache.containsValue(null));
        assertTrue(cache.containsValue(cache));
        assertFalse(cache.containsValue("not there"));
    }

    /* -------------------------------------------------
     *  Collection view tests (keySet, values, entrySet)
     * ------------------------------------------------- */
    @Test
    public void testKeySet() {
        cache.put("one", 1);
        cache.put("two", 2);
        Set<String> keys = cache.keySet();
        assertEquals(new HashSet<>(Arrays.asList("one", "two")), keys);
    }

    @Test
    public void testValues() {
        cache.put("a", "A");
        cache.put("b", "B");
        Collection<Object> values = cache.values();
        assertTrue(values.containsAll(Arrays.asList("A", "B")));
        assertEquals(2, values.size());
    }

    @Test
    public void testEntrySetOrderIsLRU() {
        cache.put("old", 1);   // becomes LRU
        cache.put("new", 2);   // most recent
        // Access "old" to move it to head
        cache.get("old");
        Set<Map.Entry<String, Object>> entries = cache.entrySet();
        Iterator<Map.Entry<String, Object>> it = entries.iterator();
        Map.Entry<String, Object> first = it.next();
        Map.Entry<String, Object> second = it.next();
        // After the get, "old" should be most‑recent, thus first in iteration
        assertEquals("old", first.getKey());
        assertEquals(Integer.valueOf(1), first.getValue());
        assertEquals("new", second.getKey());
    }

    /* -------------------------------------------------
     *  equals / hashCode / toString tests
     * ------------------------------------------------- */
    @Test
    public void testEqualsWithSameContentMap() {
        cache.put("x", 10);
        cache.put("y", 20);
        Map<String, Integer> other = new LinkedHashMap<>();
        other.put("x", 10);
        other.put("y", 20);
        assertTrue(cache.equals(other));
        assertTrue(other.equals(cache)); // LinkedHashMap's equals uses entrySet, should match
    }

    @Test
    public void testEqualsWithDifferentContent() {
        cache.put("a", 1);
        Map<String, Integer> other = Collections.singletonMap("a", 2);
        assertFalse(cache.equals(other));
    }

    @Test
    public void testEqualsWithNonMap() {
        assertFalse(cache.equals("not a map"));
    }

    @Test
    public void testHashCodeConsistency() {
        cache.put("k1", "v1");
        cache.put("k2", "v2");
        // Compute expected hashCode using the same algorithm as the class
        int expected = 1;
        expected = 31 * expected + "k1".hashCode();
        expected = 31 * expected + "v1".hashCode();
        expected = 31 * expected + "k2".hashCode();
        expected = 31 * expected + "v2".hashCode();
        assertEquals(expected, cache.hashCode());
    }

    @Test
    public void testToStringBasic() {
        cache.put("one", 1);
        cache.put("two", 2);
        String s = cache.toString();
        // Order is most‑recent first, i.e., "two" then "one"
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        assertTrue(s.contains("two=2"));
        assertTrue(s.contains("one=1"));
    }

    @Test
    public void testToStringSelfReference() {
        cache.put("self", cache);
        String s = cache.toString();
        assertTrue(s.contains("self=(this Collection)"));
    }

    /* -------------------------------------------------
     *  Concurrency edge case: get when lock is held elsewhere
     * ------------------------------------------------- */
    @Test
    public void testGetWhenLockIsHeldByAnotherThread() throws Exception {
        // Prepare three entries with capacity 3
        LockingLRUCacheStrategy<String, String> bigCache = new LockingLRUCacheStrategy<>(3);
        bigCache.put("a", "A");
        bigCache.put("b", "B");
        bigCache.put("c", "C");

        // Use reflection to acquire the private lock
        Field lockField = LockingLRUCacheStrategy.class.getDeclaredField("lock");
        lockField.setAccessible(true);
        final java.util.concurrent.locks.Lock internalLock = (java.util.concurrent.locks.Lock) lockField.get(bigCache);

        internalLock.lock(); // hold the lock so tryLock() in get() will fail
        try {
            // This get should *not* move the accessed node to head because tryLock fails
            bigCache.get("a"); // "a" is currently the LRU (tail‑most)
        } finally {
            internalLock.unlock();
        }

        // Adding a new entry forces eviction of the current LRU, which should be "a"
        bigCache.put("d", "D");
        assertNull("Entry 'a' should have been evicted because get() did not move it", bigCache.get("a"));
        assertEquals("B", bigCache.get("b"));
        assertEquals("C", bigCache.get("c"));
        assertEquals("D", bigCache.get("d"));
    }
}
