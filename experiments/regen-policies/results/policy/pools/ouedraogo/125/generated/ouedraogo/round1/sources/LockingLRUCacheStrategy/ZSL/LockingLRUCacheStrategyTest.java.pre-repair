package com.cedarsoftware.util.cache;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.Assert.*;

public class LockingLRUCacheStrategyTest {

    private LockingLRUCacheStrategy<String, Object> cache;

    @Before
    public void setUp() {
        cache = new LockingLRUCacheStrategy<>(3);
    }

    @Test
    public void testPutAndGetBasic() {
        assertNull(cache.put("one", 1));
        assertNull(cache.put("two", 2));
        assertNull(cache.put("three", 3));

        assertEquals(1, cache.get("one"));
        assertEquals(2, cache.get("two"));
        assertEquals(3, cache.get("three"));
    }

    @Test
    public void testLRUOrderAfterGets() {
        cache.put("A", "a");
        cache.put("B", "b");
        cache.put("C", "c"); // C is most recent

        // Access B, should become most recent
        cache.get("B");

        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, Object> e : cache.entrySet()) {
            keys.add(e.getKey());
        }
        // Expected order: B, C, A
        assertEquals(Arrays.asList("B", "C", "A"), keys);
    }

    @Test
    public void testCapacityEviction() {
        LockingLRUCacheStrategy<Integer, String> smallCache = new LockingLRUCacheStrategy<>(2);
        smallCache.put(1, "one");
        smallCache.put(2, "two");
        // Cache is full now; next put should evict least recently used (1)
        smallCache.put(3, "three");

        assertFalse(smallCache.containsKey(1));
        assertTrue(smallCache.containsKey(2));
        assertTrue(smallCache.containsKey(3));
        assertEquals(2, smallCache.size());
    }

    @Test
    public void testRemove() {
        cache.put("X", "x");
        cache.put("Y", "y");
        assertEquals("x", cache.remove("X"));
        assertFalse(cache.containsKey("X"));
        assertEquals(1, cache.size());

        // Removing non‑existent key returns null
        assertNull(cache.remove("Z"));
    }

    @Test
    public void testClear() {
        cache.put("k1", "v1");
        cache.put("k2", "v2");
        cache.clear();

        assertTrue(cache.isEmpty());
        assertNull(cache.get("k1"));
    }

    @Test
    public void testContainsKeyAndContainsValueWithNulls() {
        cache.put(null, null);
        cache.put("notNull", null);
        cache.put(null, "notNull");

        assertTrue(cache.containsKey(null));
        assertTrue(cache.containsValue(null));
        assertTrue(cache.containsValue("notNull"));
    }

    @Test
    public void testEntrySetKeySetValuesOrder() {
        cache.put("first", 1);
        cache.put("second", 2);
        cache.put("third", 3);

        // Access "first" to make it most recent
        cache.get("first");

        // entrySet order should be first, third, second
        List<String> entryKeys = new ArrayList<>();
        for (Map.Entry<String, Object> e : cache.entrySet()) {
            entryKeys.add(e.getKey());
        }
        assertEquals(Arrays.asList("first", "third", "second"), entryKeys);

        // keySet order should match entrySet order
        List<String> keySet = new ArrayList<>(cache.keySet());
        assertEquals(entryKeys, keySet);

        // values order should correspond
        List<Object> values = new ArrayList<>(cache.values());
        assertEquals(Arrays.asList(1, 3, 2), values);
    }

    @Test
    public void testPutAll() {
        Map<String, Integer> src = new LinkedHashMap<>();
        src.put("a", 10);
        src.put("b", 20);
        src.put("c", 30);

        cache.putAll(src);
        assertEquals(3, cache.size());
        assertEquals(Integer.valueOf(10), cache.get("a"));
        assertEquals(Integer.valueOf(20), cache.get("b"));
        assertEquals(Integer.valueOf(30), cache.get("c"));
    }

    @Test
    public void testEqualsAndHashCode() {
        cache.put("k1", "v1");
        cache.put("k2", "v2");
        cache.put("k3", "v3");

        // Build a LinkedHashMap with the same LRU order (most recent first)
        Map<String, String> reference = new LinkedHashMap<>();
        // The most recent entry is k3 (last put)
        reference.put("k3", "v3");
        reference.put("k2", "v2");
        reference.put("k1", "v1");

        assertEquals(reference.entrySet(), cache.entrySet());
        assertEquals(reference.hashCode(), cache.hashCode());

        // Equality with another map having same entries/order
        Map<String, String> other = new LinkedHashMap<>(reference);
        assertTrue(cache.equals(other));
        assertTrue(other.equals(cache));
    }

    @Test
    public void testToStringAndSelfReferenceFormatting() {
        cache.put("self", cache); // self‑reference as value
        cache.put("num", 42);
        String s = cache.toString();

        // The self reference should be replaced with placeholder
        assertTrue(s.contains("self=(this Collection)"));
        assertTrue(s.contains("num=42"));
        // String should start and end with braces
        assertTrue(s.startsWith("{") && s.endsWith("}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeCapacityThrows() {
        new LockingLRUCacheStrategy<>(-1);
    }

    @Test
    public void testGetTryLockBehavior() throws Exception {
        // Populate cache
        cache.put("A", "a");
        cache.put("B", "b");
        cache.put("C", "c"); // order: C (MRU), B, A

        // Acquire the internal lock manually so tryLock will fail
        Field lockField = LockingLRUCacheStrategy.class.getDeclaredField("lock");
        lockField.setAccessible(true);
        java.util.concurrent.locks.Lock internalLock = (java.util.concurrent.locks.Lock) lockField.get(cache);
        internalLock.lock(); // hold the lock

        try {
            // get() should return value but NOT move node to head because tryLock fails
            assertEquals("a", cache.get("A"));
        } finally {
            internalLock.unlock();
        }

        // Verify order is still C, B, A
        List<String> orderAfterFailedTryLock = new ArrayList<>();
        for (Map.Entry<String, Object> e : cache.entrySet()) {
            orderAfterFailedTryLock.add(e.getKey());
        }
        assertEquals(Arrays.asList("C", "B", "A"), orderAfterFailedTryLock);

        // Now a normal get() should move "A" to head
        assertEquals("a", cache.get("A"));
        List<String> orderAfterSuccessfulGet = new ArrayList<>();
        for (Map.Entry<String, Object> e : cache.entrySet()) {
            orderAfterSuccessfulGet.add(e.getKey());
        }
        assertEquals(Arrays.asList("A", "C", "B"), orderAfterSuccessfulGet);
    }
}
