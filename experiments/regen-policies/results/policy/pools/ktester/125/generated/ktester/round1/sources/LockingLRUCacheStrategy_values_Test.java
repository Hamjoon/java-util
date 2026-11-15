package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@ExtendWith(MockitoExtension.class)
class LockingLRUCacheStrategy_values_Test {

    private static final int CAPACITY = 5;

    private LockingLRUCacheStrategy<String, Integer> cache;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh cache for each test
        cache = new LockingLRUCacheStrategy<>(CAPACITY);
        // Populate it with a known set of entries
        cache.put("one", 1);
        cache.put("two", 2);
        cache.put("three", 3);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure cache is cleared even if a test corrupts internal state
        try {
            cache.clear();
        } catch (Exception ignored) {
        }
    }

    @Test
    void valuesReturnsAllValuesInInsertionOrder() {
        Collection<Integer> values = cache.values();
        // Verify size matches the number of entries inserted
        assertEquals(3, values.size(), "values() should contain exactly three elements");
        // Verify the order is the same as insertion order (LinkedHashMap preserves order)
        List<Integer> expectedOrder = List.of(1, 2, 3);
        List<Integer> actualOrder = new ArrayList<>(values);
        assertEquals(expectedOrder, actualOrder, "values() should preserve insertion order");
    }

    @Test
    void valuesCollectionIsIndependentFromCache() {
        Collection<Integer> values = cache.values();
        // Modify the returned collection
        Iterator<Integer> iterator = values.iterator();
        if (iterator.hasNext()) {
            iterator.next();
            // remove one element from the collection view
            iterator.remove();
        }
        // The cache itself must remain unchanged
        assertEquals(3, cache.size(), "Modifying the collection returned by values() must not affect the cache size");
        assertTrue(cache.containsValue(1) || cache.containsValue(2) || cache.containsValue(3), "Cache should still contain its original values after the collection is modified");
    }

    @Test
    void valuesReflectsCacheAfterEviction() {
        // Fill the cache beyond its capacity to trigger eviction (capacity is 5, we already have 3)
        cache.put("four", 4);
        cache.put("five", 5);
        // This should evict the least-recently-used entry ("one")
        cache.put("six", 6);
        Collection<Integer> values = cache.values();
        // The cache size should never exceed the configured capacity
        assertEquals(CAPACITY, cache.size(), "Cache size should not exceed its capacity");
        // The evicted value (1) must no longer be present
        assertFalse(values.contains(1), "Evicted value should not appear in values() collection");
        // Remaining values should be present
        assertTrue(values.containsAll(List.of(2, 3, 4, 5, 6)), "All non‑evicted values must be present in the collection returned by values()");
    }

    @Test
    void emptyCacheReturnsEmptyCollection() {
        // cache is freshly created and empty
        Collection<Integer> values = cache.values();
        assertTrue(values.isEmpty(), "Empty cache should return an empty collection");
    }

    @Test
    void singleEntryReturnsSingletonCollection() {
        cache.put("k1", 42);
        Collection<Integer> values = cache.values();
        assertEquals(1, values.size(), "Single entry cache should return collection of size 1");
        assertTrue(values.contains(42), "Returned collection must contain the inserted value");
    }

    @Test
    void multipleEntriesPreserveInsertionOrder() {
        cache.put("k1", 10);
        cache.put("k2", 20);
        cache.put("k3", 30);
        Collection<Integer> values = cache.values();
        List<Integer> expected = List.of(10, 20, 30);
        assertEquals(expected, new ArrayList<>(values), "Values must follow insertion order");
    }

    @Test
    void afterAccessReordersValuesAccordingToLRU() {
        cache.put("k1", 100);
        cache.put("k2", 200);
        cache.put("k3", 300);
        // Access k1 to make it most‑recently used
        assertEquals(100, cache.get("k1"));
        Collection<Integer> values = cache.values();
        // According to the implementation, iteration starts at head (least recent)
        // Expected order: k2 (least), k3, k1 (most recent)
        List<Integer> expected = List.of(200, 300, 100);
        assertEquals(expected, new ArrayList<>(values), "Values order should reflect LRU after a get() access");
    }

    @Test
    void valuesHandlesNullValuesCorrectly() {
        cache.put("nullKey", null);
        cache.put("nonNullKey", 777);
        Collection<Integer> values = cache.values();
        List<Integer> expected = List.of(null, 777);
        assertEquals(expected, new ArrayList<>(values), "Collection must contain null values in proper order");
    }

    @Test
    void concurrentModificationDoesNotThrowException() throws Exception {
        // Populate cache
        cache.put("a", 1);
        cache.put("b", 2);
        cache.put("c", 3);
        ExecutorService exec = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicBoolean exceptionThrown = new AtomicBoolean(false);
        // Thread 1: call values() and hold the lock for a short time
        Future<?> valuesFuture = exec.submit(() -> {
            try {
                // wait until both threads are ready
                startLatch.await();
                // this acquires and releases the lock quickly
                cache.values();
            } catch (Throwable t) {
                exceptionThrown.set(true);
            }
        });
        // Thread 2: modify the cache while thread 1 is executing values()
        Future<?> modifyFuture = exec.submit(() -> {
            try {
                startLatch.await();
                cache.put("d", 4);
                cache.remove("a");
            } catch (Throwable t) {
                exceptionThrown.set(true);
            }
        });
        // Release both threads
        startLatch.countDown();
        // Wait for completion
        valuesFuture.get(2, TimeUnit.SECONDS);
        modifyFuture.get(2, TimeUnit.SECONDS);
        exec.shutdownNow();
        assertFalse(exceptionThrown.get(), "Concurrent modification should not cause an exception");
    }

    @Test
    void lockIsReleasedWhenValuesThrowsException() throws Exception {
        // Create a key whose hashCode throws NullPointerException
        Object badKey = new Object() {

            @Override
            public int hashCode() {
                throw new NullPointerException("forced");
            }

            @Override
            public boolean equals(Object obj) {
                return this == obj;
            }
        };
        // Insert the problematic entry via reflection (put() would call hashCode internally and fail)
        // So we directly manipulate the internal linked list.
        // Obtain head, tail, and create a Node instance via reflection.
        Field headField = LockingLRUCacheStrategy.class.getDeclaredField("head");
        Field tailField = LockingLRUCacheStrategy.class.getDeclaredField("tail");
        headField.setAccessible(true);
        tailField.setAccessible(true);
        // The cache constructor creates sentinel head/tail nodes; we reuse them.
        Object head = headField.get(cache);
        Object tail = tailField.get(cache);
        // Create a new Node instance (inner class) via reflection
        Class<?> nodeClass = Class.forName("com.cedarsoftware.util.cache.LockingLRUCacheStrategy$Node");
        java.lang.reflect.Constructor<?> ctor = nodeClass.getDeclaredConstructor(LockingLRUCacheStrategy.class, Object.class, Object.class, nodeClass, nodeClass);
        ctor.setAccessible(true);
        // Parameters: outer instance, key, value, prev, next
        Object badNode = ctor.newInstance(cache, badKey, 999, head, tail);
        // Insert the node between head and tail
        Field nextField = nodeClass.getDeclaredField("next");
        Field prevField = nodeClass.getDeclaredField("prev");
        nextField.setAccessible(true);
        prevField.setAccessible(true);
        nextField.set(head, badNode);
        prevField.set(tail, badNode);
        // Also set node's next/prev correctly
        nextField.set(badNode, tail);
        prevField.set(badNode, head);
        // Now calling values() should trigger NullPointerException inside map.put
        // Verify that after the exception the lock is not held.
        Field lockField = LockingLRUCacheStrategy.class.getDeclaredField("lock");
        lockField.setAccessible(true);
        Lock lock = (Lock) lockField.get(cache);
        assertThrows(NullPointerException.class, () -> cache.values(), "Expected NullPointerException from faulty node");
        // The lock must be free – tryLock should succeed
        assertTrue(lock.tryLock(), "Lock should be released even after exception");
        lock.unlock();
    }

    @Test
    void valuesThrowsWhenHeadIsCorrupted() throws Exception {
        // Corrupt the internal head reference
        Field headField = LockingLRUCacheStrategy.class.getDeclaredField("head");
        headField.setAccessible(true);
        headField.set(cache, null);
        // Expect NullPointerException when values() attempts to access head.next
        assertThrows(NullPointerException.class, () -> cache.values(), "Corrupted head should cause NullPointerException");
    }
}
