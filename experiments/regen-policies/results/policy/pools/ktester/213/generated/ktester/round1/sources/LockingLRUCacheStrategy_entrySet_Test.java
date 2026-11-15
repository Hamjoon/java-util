package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LockingLRUCacheStrategy_entrySet_Test {

    private LockingLRUCacheStrategy<String, Integer> cache;

    class ThrowingCache extends LockingLRUCacheStrategy<String, Integer> {

        ThrowingCache(int capacity) {
            super(capacity);
        }

        @Override
        public Set<Map.Entry<String, Integer>> entrySet() {
            // replicate original lock acquisition
            try {
                // use reflection to get the private lock field
                Field lockField = LockingLRUCacheStrategy.class.getDeclaredField("lock");
                lockField.setAccessible(true);
                Lock lock = (Lock) lockField.get(this);
                lock.lock();
                try {
                    throw new RuntimeException("simulated failure");
                } finally {
                    lock.unlock();
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            // unreachable
            return Collections.emptySet();
        }
    }

    class BadUnlockCache extends LockingLRUCacheStrategy<String, Integer> {

        BadUnlockCache(int capacity) {
            super(capacity);
        }

        @Override
        public Set<Map.Entry<String, Integer>> entrySet() {
            try {
                // obtain the private lock via reflection
                Field lockField = LockingLRUCacheStrategy.class.getDeclaredField("lock");
                lockField.setAccessible(true);
                Lock lock = (Lock) lockField.get(this);
                // lock it normally
                lock.lock();
                // now unlock *twice* – the extra unlock will cause IllegalMonitorStateException
                // first unlock (normal)
                lock.unlock();
                // second unlock (illegal)
                lock.unlock();
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            // call super – its finally block will attempt to unlock again -> IllegalMonitorStateException
            return super.entrySet();
        }
    }

    class ThrowingCache extends LockingLRUCacheStrategy<String, Integer> {

        ThrowingCache(int capacity) {
            super(capacity);
        }

        @Override
        public Set<Map.Entry<String, Integer>> entrySet() {
            // replicate original lock acquisition
            try {
                // use reflection to get the private lock field
                Field lockField = LockingLRUCacheStrategy.class.getDeclaredField("lock");
                lockField.setAccessible(true);
                Lock lock = (Lock) lockField.get(this);
                lock.lock();
                try {
                    throw new RuntimeException("simulated failure");
                } finally {
                    lock.unlock();
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            // unreachable
            return Collections.emptySet();
        }
    }

    class BadUnlockCache extends LockingLRUCacheStrategy<String, Integer> {

        BadUnlockCache(int capacity) {
            super(capacity);
        }

        @Override
        public Set<Map.Entry<String, Integer>> entrySet() {
            try {
                // obtain the private lock via reflection
                Field lockField = LockingLRUCacheStrategy.class.getDeclaredField("lock");
                lockField.setAccessible(true);
                Lock lock = (Lock) lockField.get(this);
                // lock it normally
                lock.lock();
                // now unlock *twice* – the extra unlock will cause IllegalMonitorStateException
                // first unlock (normal)
                lock.unlock();
                // second unlock (illegal)
                lock.unlock();
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            // call super – its finally block will attempt to unlock again -> IllegalMonitorStateException
            return super.entrySet();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a cache with a small capacity to keep the test focused
        cache = new LockingLRUCacheStrategy<>(5);
        // Populate the cache with deterministic data
        cache.put("one", 1);
        cache.put("two", 2);
        cache.put("three", 3);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the cache is cleared after each test to avoid cross‑test contamination
        cache.clear();
    }

    private static Lock getLock(LockingLRUCacheStrategy<?, ?> cacheInstance) {
        try {
            Field lockField = LockingLRUCacheStrategy.class.getDeclaredField("lock");
            lockField.setAccessible(true);
            return (Lock) lockField.get(cacheInstance);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void entrySetContainsAllInsertedEntries() {
        Set<Map.Entry<String, Integer>> entrySet = cache.entrySet();
        // Verify size matches the number of inserted entries
        assertEquals(3, entrySet.size(), "entrySet size should reflect the number of entries in the cache");
        // Verify each expected entry is present
        Map<String, Integer> expected = new LinkedHashMap<>();
        expected.put("one", 1);
        expected.put("two", 2);
        expected.put("three", 3);
        for (Map.Entry<String, Integer> e : entrySet) {
            assertTrue(expected.containsKey(e.getKey()), "Unexpected key in entrySet: " + e.getKey());
            assertEquals(expected.get(e.getKey()), e.getValue(), "Value mismatch for key: " + e.getKey());
        }
    }

    @Test
    void entrySetIsASnapshotNotAffectedBySubsequentModifications() {
        Set<Map.Entry<String, Integer>> snapshot = cache.entrySet();
        // Modify the cache after obtaining the snapshot
        cache.put("four", 4);
        cache.remove("one");
        // The snapshot must remain unchanged
        assertEquals(3, snapshot.size(), "Snapshot size should remain the original size");
        Set<String> keysInSnapshot = new HashSet<>();
        for (Map.Entry<String, Integer> e : snapshot) {
            keysInSnapshot.add(e.getKey());
        }
        assertTrue(keysInSnapshot.containsAll(Arrays.asList("one", "two", "three")), "Snapshot should still contain the original keys");
        assertFalse(keysInSnapshot.contains("four"), "Snapshot must not contain newly added keys");
    }

    @Test
    void entrySetHandlesNullKeyAndNullValue() {
        cache.put(null, null);
        cache.put("nullKey", null);
        // Overwrites previous null key entry
        cache.put(null, 99);
        Set<Map.Entry<String, Integer>> entrySet = cache.entrySet();
        // Build the expected map based on the current state of the cache
        Map<String, Integer> expected = new LinkedHashMap<>();
        expected.put("one", 1);
        expected.put("two", 2);
        expected.put("three", 3);
        expected.put(null, 99);
        expected.put("nullKey", null);
        assertEquals(expected.size(), entrySet.size(), "entrySet size should include null key/value entries");
        for (Map.Entry<String, Integer> e : entrySet) {
            assertTrue(expected.containsKey(e.getKey()), "Unexpected key (including null) in entrySet: " + e.getKey());
            assertEquals(expected.get(e.getKey()), e.getValue(), "Value mismatch for key (including null) in entrySet");
        }
    }

    @Test
    void entrySetPreservesInsertionOrder() {
        // Clear and re‑populate to have a known order
        cache.clear();
        cache.put("first", 1);
        cache.put("second", 2);
        cache.put("third", 3);
        Set<Map.Entry<String, Integer>> entrySet = cache.entrySet();
        // LinkedHashMap preserves insertion order; entrySet iterator should reflect that order
        List<String> expectedOrder = Arrays.asList("first", "second", "third");
        List<String> actualOrder = new ArrayList<>();
        for (Map.Entry<String, Integer> e : entrySet) {
            actualOrder.add(e.getKey());
        }
        assertEquals(expectedOrder, actualOrder, "entrySet iteration order should follow insertion order");
    }

    @Test
    void entrySetEmptyCacheReturnsEmptySet() {
        // ensure empty
        cache.clear();
        Set<Map.Entry<String, Integer>> entrySet = cache.entrySet();
        assertTrue(entrySet.isEmpty(), "entrySet of an empty cache should be empty");
        assertEquals(0, entrySet.size(), "Size must be 0 for empty cache");
    }

    @Test
    void entrySetSingleEntryReturnsExactlyOneElement() {
        cache.clear();
        cache.put("key1", 42);
        Set<Map.Entry<String, Integer>> entrySet = cache.entrySet();
        assertEquals(1, entrySet.size(), "Should contain exactly one entry");
        Map.Entry<String, Integer> e = entrySet.iterator().next();
        assertEquals("key1", e.getKey());
        assertEquals(42, e.getValue());
    }

    @Test
    void entrySetMultipleEntriesPreservesInsertionOrder() {
        cache.clear();
        cache.put("k1", 1);
        cache.put("k2", 2);
        cache.put("k3", 3);
        Set<Map.Entry<String, Integer>> entrySet = cache.entrySet();
        List<String> expected = Arrays.asList("k1", "k2", "k3");
        List<String> actual = new ArrayList<>();
        for (Map.Entry<String, Integer> e : entrySet) {
            actual.add(e.getKey());
        }
        assertEquals(expected, actual, "Insertion order must be preserved");
    }

    @Test
    void entrySetModificationDoesNotAffectCache() {
        Set<Map.Entry<String, Integer>> setA = cache.entrySet();
        Set<Map.Entry<String, Integer>> setB = cache.entrySet();
        // mutate setA
        Iterator<Map.Entry<String, Integer>> it = setA.iterator();
        if (it.hasNext()) {
            it.next();
            // remove from snapshot only
            it.remove();
        }
        // cache must stay unchanged
        assertEquals(3, cache.size(), "Cache size must remain unchanged after mutating snapshot");
        // setB must still contain all original entries
        assertEquals(3, setB.size(), "Second snapshot must be unaffected by mutation of first snapshot");
    }

    @Test
    void entrySetLockIsAlwaysReleasedEvenWhenExceptionThrown() {
        // Subclass that throws inside entrySet after acquiring the lock
        class ThrowingCache extends LockingLRUCacheStrategy<String, Integer> {

            ThrowingCache(int capacity) {
                super(capacity);
            }

            @Override
            public Set<Map.Entry<String, Integer>> entrySet() {
                // replicate original lock acquisition
                try {
                    // use reflection to get the private lock field
                    Field lockField = LockingLRUCacheStrategy.class.getDeclaredField("lock");
                    lockField.setAccessible(true);
                    Lock lock = (Lock) lockField.get(this);
                    lock.lock();
                    try {
                        throw new RuntimeException("simulated failure");
                    } finally {
                        lock.unlock();
                    }
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
                // unreachable
                return Collections.emptySet();
            }
        }
        ThrowingCache throwingCache = new ThrowingCache(5);
        throwingCache.put("a", 1);
        // verify that after the exception the lock is not held
        RuntimeException ex = assertThrows(RuntimeException.class, throwingCache::entrySet, "Exception should propagate");
        assertEquals("simulated failure", ex.getMessage());
        // reflect lock to assert it is not held by current thread
        try {
            Field lockField = LockingLRUCacheStrategy.class.getDeclaredField("lock");
            lockField.setAccessible(true);
            Lock lock = (Lock) lockField.get(throwingCache);
            assertFalse(((ReentrantLock) lock).isHeldByCurrentThread(), "Lock must be released even after exception");
        } catch (Exception e) {
            fail("Reflection failed: " + e);
        }
    }

    @Test
    void entrySetReflectsLruOrderAfterAccesses() {
        cache.clear();
        cache.put("k1", 1);
        cache.put("k2", 2);
        cache.put("k3", 3);
        // access k2 to make it most-recently used
        cache.get("k2");
        Set<Map.Entry<String, Integer>> entrySet = cache.entrySet();
        List<String> actualOrder = new ArrayList<>();
        for (Map.Entry<String, Integer> e : entrySet) {
            actualOrder.add(e.getKey());
        }
        // Expected order: k2 (most recent), then k3, then k1
        List<String> expectedOrder = Arrays.asList("k2", "k3", "k1");
        assertEquals(expectedOrder, actualOrder, "entrySet must reflect LRU order after a get() operation");
    }

    @Test
    void entrySetShowsEvictionWhenCapacityExceeded() {
        // capacity 2
        LockingLRUCacheStrategy<String, Integer> smallCache = new LockingLRUCacheStrategy<>(2);
        smallCache.put("a", 1);
        smallCache.put("b", 2);
        // this put should evict "a"
        smallCache.put("c", 3);
        Set<Map.Entry<String, Integer>> entrySet = smallCache.entrySet();
        assertEquals(2, entrySet.size(), "Cache size must respect capacity after eviction");
        Map<String, Integer> expected = new LinkedHashMap<>();
        // after eviction, order should be most-recently used first: "c", then "b"
        expected.put("c", 3);
        expected.put("b", 2);
        List<String> actualOrder = new ArrayList<>();
        for (Map.Entry<String, Integer> e : entrySet) {
            actualOrder.add(e.getKey());
            assertEquals(expected.get(e.getKey()), e.getValue(), "Value mismatch after eviction for key " + e.getKey());
        }
        assertEquals(new ArrayList<>(expected.keySet()), actualOrder, "Order after eviction must be LRU (most recent first)");
    }

    @Test
    void entrySetAfterClearReturnsEmptySet() {
        cache.clear();
        Set<Map.Entry<String, Integer>> entrySet = cache.entrySet();
        assertTrue(entrySet.isEmpty(), "entrySet must be empty after cache.clear()");
        assertEquals(0, entrySet.size());
    }

    @Test
    void entrySetHandlesNullValues() {
        cache.clear();
        cache.put("nullVal", null);
        Set<Map.Entry<String, Integer>> entrySet = cache.entrySet();
        assertEquals(1, entrySet.size());
        Map.Entry<String, Integer> e = entrySet.iterator().next();
        assertEquals("nullVal", e.getKey());
        assertNull(e.getValue(), "Value should be null");
    }

    @Test
    void entrySetConcurrentCallsDoNotDeadlock() throws InterruptedException {
        int threadCount = 2;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        List<Set<Map.Entry<String, Integer>>> results = Collections.synchronizedList(new ArrayList<>());
        Runnable task = () -> {
            try {
                // wait for both threads to be ready
                startLatch.await();
                results.add(cache.entrySet());
            } catch (Exception e) {
                fail("Exception in concurrent entrySet call: " + e);
            } finally {
                doneLatch.countDown();
            }
        };
        for (int i = 0; i < threadCount; i++) {
            new Thread(task).start();
        }
        // release both threads
        startLatch.countDown();
        // wait for completion
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "Threads should finish within timeout");
        // both results should have same size and contain same entries
        assertEquals(2, results.size());
        for (Set<Map.Entry<String, Integer>> set : results) {
            assertEquals(3, set.size());
        }
        // verify lock is not held after both calls
        try {
            Field lockField = LockingLRUCacheStrategy.class.getDeclaredField("lock");
            lockField.setAccessible(true);
            Lock lock = (Lock) lockField.get(cache);
            assertFalse(((ReentrantLock) lock).isHeldByCurrentThread(), "Lock must be released after concurrent entrySet calls");
        } catch (Exception e) {
            fail("Reflection error: " + e);
        }
    }

    @Test
    void entrySetLockAcquisitionFailureThrowsIllegalMonitorStateException() {
        // Subclass that manually unlocks before calling super.entrySet()
        class BadUnlockCache extends LockingLRUCacheStrategy<String, Integer> {

            BadUnlockCache(int capacity) {
                super(capacity);
            }

            @Override
            public Set<Map.Entry<String, Integer>> entrySet() {
                try {
                    // obtain the private lock via reflection
                    Field lockField = LockingLRUCacheStrategy.class.getDeclaredField("lock");
                    lockField.setAccessible(true);
                    Lock lock = (Lock) lockField.get(this);
                    // lock it normally
                    lock.lock();
                    // now unlock *twice* – the extra unlock will cause IllegalMonitorStateException
                    // first unlock (normal)
                    lock.unlock();
                    // second unlock (illegal)
                    lock.unlock();
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
                // call super – its finally block will attempt to unlock again -> IllegalMonitorStateException
                return super.entrySet();
            }
        }
        BadUnlockCache badCache = new BadUnlockCache(5);
        badCache.put("x", 1);
        IllegalMonitorStateException ex = assertThrows(IllegalMonitorStateException.class, badCache::entrySet, "Double unlock should cause IllegalMonitorStateException");
        // No further assertions needed; the exception confirms the lock handling bug.
    }
}
