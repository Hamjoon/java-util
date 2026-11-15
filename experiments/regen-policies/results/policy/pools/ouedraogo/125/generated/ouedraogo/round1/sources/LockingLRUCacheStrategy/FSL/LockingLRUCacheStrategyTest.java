package com.cedarsoftware.util.cache;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.Test;

/**
 * Comprehensive JUnit4 test suite for {@link LockingLRUCacheStrategy}.
 * Covers construction, basic map operations, LRU eviction order,
 * special cases (null keys/values, self‑reference formatting),
 * and overridden Object methods.
 */
public class LockingLRUCacheStrategyTest {

    /* ---------- Constructor validation ---------- */

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNegativeCapacity() {
        new LockingLRUCacheStrategy<String, String>(-1);
    }

    @Test
    public void testConstructorZeroCapacity() {
        LockingLRUCacheStrategy<String, String> cache = new LockingLRUCacheStrategy<>(0);
        assertEquals(0, cache.size());
        assertTrue(cache.isEmpty());
    }

    /* ---------- Basic put / get / remove ---------- */

    @Test
    public void testPutAndGetSimple() {
        LockingLRUCacheStrategy<String, String> cache = new LockingLRUCacheStrategy<>(10);
        assertNull(cache.put("a", "A"));
        assertEquals("A", cache.get("a"));
        assertNull(cache.put("a", "B")); // replace existing value
        assertEquals("B", cache.get("a"));
    }

    @Test
    public void testGetNonExistingReturnsNull() {
        LockingLRUCacheStrategy<String, String> cache = new LockingLRUCacheStrategy<>(5);
        assertNull(cache.get("missing"));
    }

    @Test
    public void testRemoveExisting() {
        LockingLRUCacheStrategy<String, Integer> cache = new LockingLRUCacheStrategy<>(5);
        cache.put("one", 1);
        cache.put("two", 2);
        assertEquals(Integer.valueOf(1), cache.remove("one"));
        assertNull(cache.get("one"));
        assertEquals(1, cache.size());
    }

    @Test
    public void testRemoveNonExistingReturnsNull() {
        LockingLRUCacheStrategy<String, String> cache = new LockingLRUCacheStrategy<>(3);
        assertNull(cache.remove("nothing"));
    }

    /* ---------- LRU eviction order ---------- */

    @Test
    public void testEvictionWhenCapacityExceeded() {
        LockingLRUCacheStrategy<String, String> cache = new LockingLRUCacheStrategy<>(2);
        cache.put("first", "1");
        cache.put("second", "2");
        // Access "first" to make it MRU
        assertEquals("1", cache.get("first"));
        // Adding third entry should evict "second"
        cache.put("third", "3");
        assertNull(cache.get("second"));
        assertEquals("1", cache.get("first"));
        assertEquals("3", cache.get("third"));
    }

    @Test
    public void testGetMovesNodeToHead() {
        LockingLRUCacheStrategy<String, String> cache = new LockingLRUCacheStrategy<>(3);
        cache.put("A", "a");
        cache.put("B", "b");
        cache.put("C", "c");
        // Initial order (most recent first) is C,B,A because each put adds to head
        assertEquals("{C=c, B=b, A=a}", cache.toString());

        // Access "A" -> should become most recent
        cache.get("A");
        assertEquals("{A=a, C=c, B=b}", cache.toString());
    }

    /* ---------- clear, size, isEmpty ---------- */

    @Test
    public void testClearEmptiesCache() {
        LockingLRUCacheStrategy<Integer, Integer> cache = new LockingLRUCacheStrategy<>(5);
        cache.put(1, 10);
        cache.put(2, 20);
        cache.clear();
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
        assertNull(cache.get(1));
    }

    /* ---------- containsKey / containsValue ---------- */

    @Test
    public void testContainsKeyAndValue() {
        LockingLRUCacheStrategy<String, String> cache = new LockingLRUCacheStrategy<>(4);
        cache.put("k1", "v1");
        cache.put("k2", null);
        cache.put(null, "vNullKey");
        assertTrue(cache.containsKey("k1"));
        assertTrue(cache.containsKey(null));
        assertFalse(cache.containsKey("missing"));

        assertTrue(cache.containsValue("v1"));
        assertTrue(cache.containsValue(null));
        assertTrue(cache.containsValue("vNullKey"));
        assertFalse(cache.containsValue("nope"));
    }

    /* ---------- entrySet, keySet, values order ---------- */

    @Test
    public void testEntrySetOrderMatchesLRU() {
        LockingLRUCacheStrategy<String, Integer> cache = new LockingLRUCacheStrategy<>(3);
        cache.put("one", 1);
        cache.put("two", 2);
        cache.put("three", 3);
        // order: three,two,one
        Set<Map.Entry<String, Integer>> entries = cache.entrySet();
        Iterator<Map.Entry<String, Integer>> it = entries.iterator();
        Map.Entry<String, Integer> e1 = it.next();
        Map.Entry<String, Integer> e2 = it.next();
        Map.Entry<String, Integer> e3 = it.next();
        assertEquals("three", e1.getKey());
        assertEquals(Integer.valueOf(3), e1.getValue());
        assertEquals("two", e2.getKey());
        assertEquals(Integer.valueOf(2), e2.getValue());
        assertEquals("one", e3.getKey());
        assertEquals(Integer.valueOf(1), e3.getValue());
    }

    @Test
    public void testKeySetAndValuesOrder() {
        LockingLRUCacheStrategy<String, String> cache = new LockingLRUCacheStrategy<>(2);
        cache.put("first", "F");
        cache.put("second", "S");
        // order: second,first
        List<String> keys = new ArrayList<>(cache.keySet());
        List<String> values = new ArrayList<>(cache.values());
        assertEquals(Arrays.asList("second", "first"), keys);
        assertEquals(Arrays.asList("S", "F"), values);
    }

    /* ---------- putAll functionality ---------- */

    @Test
    public void testPutAllAddsAllEntriesAndRespectsCapacity() {
        LockingLRUCacheStrategy<String, String> cache = new LockingLRUCacheStrategy<>(3);
        Map<String, String> src = new LinkedHashMap<>();
        src.put("a", "A");
        src.put("b", "B");
        src.put("c", "C");
        src.put("d", "D"); // will cause eviction of LRU ("a")
        cache.putAll(src);
        assertNull(cache.get("a"));
        assertEquals("B", cache.get("b"));
        assertEquals("C", cache.get("c"));
        assertEquals("D", cache.get("d"));
        assertEquals(3, cache.size());
    }

    /* ---------- equals and hashCode ---------- */

    @Test
    public void testEqualsWithSameContent() {
        LockingLRUCacheStrategy<String, Integer> cache1 = new LockingLRUCacheStrategy<>(5);
        LockingLRUCacheStrategy<String, Integer> cache2 = new LockingLRUCacheStrategy<>(5);
        cache1.put("x", 10);
        cache1.put("y", 20);
        cache2.put("x", 10);
        cache2.put("y", 20);
        assertTrue(cache1.equals(cache2));
        assertTrue(cache2.equals(cache1));
        assertEquals(cache1.hashCode(), cache2.hashCode());
    }

    @Test
    public void testEqualsWithDifferentOrderButSameMappings() {
        // Order does not affect Map equality; entrySet comparison handles it.
        LockingLRUCacheStrategy<String, String> cache = new LockingLRUCacheStrategy<>(3);
        cache.put("a", "A");
        cache.put("b", "B");
        cache.get("a"); // now order: a,b
        Map<String, String> other = new LinkedHashMap<>();
        other.put("b", "B");
        other.put("a", "A");
        assertTrue(cache.equals(other));
    }

    @Test
    public void testEqualsWithDifferentMappings() {
        LockingLRUCacheStrategy<String, String> cache = new LockingLRUCacheStrategy<>(2);
        cache.put("k", "v");
        Map<String, String> other = new HashMap<>();
        other.put("k", "different");
        assertFalse(cache.equals(other));
    }

    /* ---------- toString formatting and self‑reference handling ---------- */

    @Test
    public void testToStringSimple() {
        LockingLRUCacheStrategy<Integer, String> cache = new LockingLRUCacheStrategy<>(2);
        cache.put(1, "one");
        cache.put(2, "two");
        // most recent first => 2,1
        assertEquals("{2=two, 1=one}", cache.toString());
    }

    @Test
    public void testToStringSelfReferenceKeyAndValue() {
        LockingLRUCacheStrategy<Object, Object> cache = new LockingLRUCacheStrategy<>(2);
        cache.put(cache, "value");
        cache.put("key", cache);
        // order: key,cache
        String expected = "{key=(this Collection), (this Collection)=value}";
        assertEquals(expected, cache.toString());
    }

    /* ---------- hashCode consistency with manual calculation ---------- */

    @Test
    public void testHashCodeMatchesManualComputation() {
        LockingLRUCacheStrategy<String, String> cache = new LockingLRUCacheStrategy<>(3);
        cache.put("x", "X");
        cache.put("y", "Y");
        // order: y,x
        int manual = 1;
        // y entry
        manual = 31 * manual + "y".hashCode();
        manual = 31 * manual + "Y".hashCode();
        // x entry
        manual = 31 * manual + "x".hashCode();
        manual = 31 * manual + "X".hashCode();
        assertEquals(manual, cache.hashCode());
    }

    /* ---------- Edge cases: null key and null value ---------- */

    @Test
    public void testNullKeyAndValueHandling() {
        LockingLRUCacheStrategy<String, String> cache = new LockingLRUCacheStrategy<>(2);
        cache.put(null, null);
        assertTrue(cache.containsKey(null));
        assertTrue(cache.containsValue(null));
        assertNull(cache.get(null));
        assertEquals("{null=null}", cache.toString());
    }

    /* ---------- Concurrency‑related behavior (tryLock) - basic sanity ---------- */

    @Test
    public void testGetDoesNotBlockWhenLockUnavailable() throws InterruptedException {
        final LockingLRUCacheStrategy<Integer, String> cache = new LockingLRUCacheStrategy<>(2);
        cache.put(1, "one");
        cache.put(2, "two");

        // Acquire the lock in a separate thread and hold it for a short time
        Thread locker = new Thread(() -> {
            cache.lock.lock();
            try {
                // Sleep while lock is held
                try { Thread.sleep(200); } catch (InterruptedException ignored) {}
            } finally {
                cache.lock.unlock();
            }
        });
        locker.start();
        // Give locker a chance to acquire the lock
        Thread.sleep(50);

        // Now call get while the lock is held; tryLock should fail and the method should still return the value
        String result = cache.get(1);
        assertEquals("one", result);
        // Ensure the order has NOT been changed because moveToHead was skipped
        // After the previous puts order was: 2 (most recent), 1
        assertEquals("{2=two, 1=one}", cache.toString());

        locker.join();
    }
}
