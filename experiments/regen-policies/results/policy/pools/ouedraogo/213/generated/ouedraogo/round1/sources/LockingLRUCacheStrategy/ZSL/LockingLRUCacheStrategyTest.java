package com.cedarsoftware.util.cache;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link LockingLRUCacheStrategy}.
 *
 * The tests cover:
 * <ul>
 *   <li>basic map operations (get, put, remove, clear, size, isEmpty)</li>
 *   <li>LRU ordering – most‑recently‑used element appears first in {@code entrySet()},
 *       {@code keySet()} and {@code values()} as well as in {@code toString()}</li>
 *   <li>capacity eviction logic</li>
 *   <li>behaviour of {@code containsKey}, {@code containsValue}</li>
 *   <li>equality, hashCode and string representation including self‑reference handling</li>
 *   <li>putAll handling and that it respects capacity eviction</li>
 * </ul>
 *
 * The cache is deliberately exercised in a single‑threaded context because the
 * implementation uses a non‑blocking try‑lock for {@code get()} which cannot be
 * deterministically verified in a unit test without heavy concurrency plumbing.
 */
public class LockingLRUCacheStrategyTest {

    private LockingLRUCacheStrategy<String, String> cache;

    @Before
    public void setUp() {
        // capacity of 3 gives us room to test eviction
        cache = new LockingLRUCacheStrategy<>(3);
    }

    @Test
    public void testPutAndGetBasic() {
        assertNull(cache.put("A", "1"));
        assertEquals("1", cache.get("A"));
        assertNull(cache.put("A", "2")); // replace existing value
        assertEquals("2", cache.get("A"));
        assertEquals(1, cache.size());
    }

    @Test
    public void testRemove() {
        cache.put("X", "x");
        cache.put("Y", "y");
        assertEquals("x", cache.remove("X"));
        assertNull(cache.get("X"));
        assertFalse(cache.containsKey("X"));
        assertEquals(1, cache.size());
        assertNull(cache.remove("nonexistent"));
    }

    @Test
    public void testClearAndIsEmpty() {
        cache.put("k1", "v1");
        cache.put("k2", "v2");
        assertFalse(cache.isEmpty());
        cache.clear();
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
        assertTrue(cache.keySet().isEmpty());
        assertTrue(cache.values().isEmpty());
        assertTrue(cache.entrySet().isEmpty());
    }

    @Test
    public void testCapacityEviction() {
        // Fill to capacity
        cache.put("1", "one");
        cache.put("2", "two");
        cache.put("3", "three");
        assertEquals(3, cache.size());

        // Access "1" to make it most‑recently used
        cache.get("1");

        // Adding a fourth entry should evict the least‑recently used ("2")
        cache.put("4", "four");
        assertEquals(3, cache.size());
        assertFalse(cache.containsKey("2"));
        assertTrue(cache.containsKey("1"));
        assertTrue(cache.containsKey("3"));
        assertTrue(cache.containsKey("4"));
    }

    @Test
    public void testLruOrderInViews() {
        cache.put("A", "a");
        cache.put("B", "b");
        cache.put("C", "c"); // order now: C, B, A (C most recent)

        // Access B -> order should become B, C, A
        cache.get("B");

        List<String> expectedKeys = Arrays.asList("B", "C", "A");
        List<String> expectedValues = Arrays.asList("b", "c", "a");

        // entrySet order
        List<String> entryKeys = new ArrayList<>();
        List<String> entryValues = new ArrayList<>();
        for (Map.Entry<String, String> e : cache.entrySet()) {
            entryKeys.add(e.getKey());
            entryValues.add(e.getValue());
        }
        assertEquals(expectedKeys, entryKeys);
        assertEquals(expectedValues, entryValues);

        // keySet order
        assertEquals(expectedKeys, new ArrayList<>(cache.keySet()));

        // values order
        assertEquals(expectedValues, new ArrayList<>(cache.values()));

        // toString order
        assertEquals("{B=b, C=c, A=a}", cache.toString());
    }

    @Test
    public void testContainsKeyAndContainsValue() {
        cache.put("k", "v");
        assertTrue(cache.containsKey("k"));
        assertTrue(cache.containsValue("v"));
        assertFalse(cache.containsKey("missing"));
        assertFalse(cache.containsValue("missing"));
    }

    @Test
    public void testPutAllRespectsCapacity() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("one", "1");
        map.put("two", "2");
        map.put("three", "3");
        map.put("four", "4"); // 4 entries, capacity is 3

        cache.putAll(map);
        assertEquals(3, cache.size());

        // The least‑recently used after putAll should be the first inserted ("one")
        assertFalse(cache.containsKey("one"));
        assertTrue(cache.containsKey("two"));
        assertTrue(cache.containsKey("three"));
        assertTrue(cache.containsKey("four"));
    }

    @Test
    public void testEqualsAndHashCode() {
        cache.put("a", "A");
        cache.put("b", "B");

        // Build a plain HashMap with same entries and same order
        Map<String, String> other = new LinkedHashMap<>();
        other.put("b", "B");
        other.put("a", "A"); // note order: most recent first (b then a)

        assertEquals(other, cache); // Map.equals uses entrySet()
        assertEquals(other.hashCode(), cache.hashCode());
    }

    @Test
    public void testSelfReferenceInToString() {
        // Insert the cache itself as a value to trigger self‑reference handling
        cache.put("self", null); // first a normal entry
        cache.put("selfKey", null);
        cache.put("self", cache); // replace with self reference

        String s = cache.toString();
        // The self reference should be rendered as "(this Collection)"
        assertTrue(s.contains("self=(this Collection)"));
    }

    @Test
    public void testNullKeyAndNullValueSupport() {
        cache.put(null, "nullKey");
        cache.put("nullValue", null);
        cache.put(null, null); // replace previous null key entry

        assertTrue(cache.containsKey(null));
        assertTrue(cache.containsValue(null));
        assertNull(cache.get(null));
        assertNull(cache.get("nullValue"));

        // Verify LRU ordering includes null keys/values
        List<String> expectedKeys = Arrays.asList(null, "nullValue");
        assertEquals(expectedKeys, new ArrayList<>(cache.keySet()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeCapacityThrows() {
        // The constructor validates capacity; a negative value should throw.
        new LockingLRUCacheStrategy<String, String>(-1);
    }

    @Test
    public void testGetDoesNotBlockWhenLockUnavailable() throws InterruptedException {
        // This test ensures that get() returns the value even if the lock cannot be acquired.
        // We simulate lock contention by holding the lock in another thread.
        final LockingLRUCacheStrategy<String, String> contended = new LockingLRUCacheStrategy<>(2);
        contended.put("k", "v");

        Thread locker = new Thread(() -> {
            contended.lock.lock(); // directly access the lock field (package‑private)
            try {
                // Hold the lock long enough for the other thread to attempt tryLock
                Thread.sleep(200);
            } catch (InterruptedException ignored) {
            } finally {
                contended.lock.unlock();
            }
        });
        locker.start();

        // Give the locker thread a moment to acquire the lock
        Thread.sleep(50);

        // get() should return the value even though the lock is held (tryLock fails)
        String value = contended.get("k");
        assertEquals("v", value);

        locker.join();
    }
}
