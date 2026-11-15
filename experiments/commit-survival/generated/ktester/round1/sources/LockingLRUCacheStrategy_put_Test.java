package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LockingLRUCacheStrategy_put_Test {

    /**
     * Cache instance used in each test.
     */
    private LockingLRUCacheStrategy<String, Integer> cache;

    /**
     * Capacity chosen small enough to trigger eviction logic.
     */
    private static final int CAPACITY = 2;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh cache for every test to avoid cross‑test interference.
        cache = new LockingLRUCacheStrategy<>(CAPACITY);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and make sure no stray references remain.
        cache = null;
    }

    @Test
    @DisplayName("put() with a new key returns null and stores the value")
    void putNewKeyReturnsNullAndStoresValue() {
        // given
        String key = "one";
        Integer value = 1;
        // when
        Integer returned = cache.put(key, value);
        // then
        assertNull(returned, "Putting a new key should return null");
        assertEquals(1, cache.size(), "Cache size should be 1 after one insertion");
        assertEquals(value, cache.get(key), "Stored value should be retrievable via get()");
    }

    @Test
    @DisplayName("put() with an existing key updates the value and returns the new value")
    void putExistingKeyUpdatesValue() {
        // given
        String key = "two";
        Integer firstValue = 2;
        Integer secondValue = 20;
        // initial insertion
        cache.put(key, firstValue);
        // when
        Integer returned = cache.put(key, secondValue);
        // then
        assertEquals(secondValue, returned, "put() should return the new value for an existing key");
        assertEquals(1, cache.size(), "Cache size must remain unchanged after an update");
        assertEquals(secondValue, cache.get(key), "The value associated with the key must be the updated one");
    }

    @Test
    @DisplayName("put() evicts the least‑recently‑used entry when capacity is exceeded")
    void putEvictsLeastRecentlyUsedWhenCapacityExceeded() {
        // given
        // becomes most‑recent
        cache.put("A", 10);
        // now order: B (MRU), A (LRU)
        cache.put("B", 20);
        // when – inserting a third entry should evict "A"
        cache.put("C", 30);
        // then
        assertEquals(CAPACITY, cache.size(), "Cache size must stay equal to the configured capacity");
        assertNull(cache.get("A"), "The least‑recently‑used entry (\"A\") should have been evicted");
        assertEquals(20, cache.get("B"), "Entry \"B\" should still be present");
        assertEquals(30, cache.get("C"), "Entry \"C\" should be present as the newly added element");
    }

    @Test
    @DisplayName("put() moves an accessed key to the head of the LRU list")
    void putMovesKeyToHead() {
        // given
        cache.put("X", 1);
        cache.put("Y", 2);
        // Access "X" again via put (update) to move it to head
        cache.put("X", 10);
        // when inserting a third element, "Y" should be evicted because "X" is now most‑recent
        cache.put("Z", 3);
        // then
        assertNull(cache.get("Y"), "Key \"Y\" should have been evicted as it became LRU");
        assertEquals(10, cache.get("X"), "Key \"X\" must retain its updated value");
        assertEquals(3, cache.get("Z"), "Key \"Z\" must be present");
    }

    @Test
    @DisplayName("Lock is released after put – subsequent put does not deadlock")
    void lockReleaseVerification() {
        // First put should acquire and release the lock.
        cache.put("E", 100);
        // Second put on the same thread must succeed immediately if the lock was released.
        assertDoesNotThrow(() -> cache.put("F", 200), "Lock was not released after first put");
    }

    @Test
    @DisplayName("null key handling – cache accepts null keys")
    void nullKeyHandling() {
        cache.put(null, 999);
        assertEquals(999, cache.get(null), "Cache must return the value associated with a null key");
        assertTrue(cache.containsKey(null), "containsKey(null) must be true after insertion");
    }

    @Test
    @DisplayName("null value handling – cache stores null values")
    void nullValueHandling() {
        cache.put("N", null);
        assertTrue(cache.containsKey("N"), "Key with null value must be present");
        assertNull(cache.get("N"), "get(\"N\") should return null for a stored null value");
    }

    @Test
    @DisplayName("zero capacity cache – every put results in immediate eviction")
    void zeroCapacityCache() {
        LockingLRUCacheStrategy<String, Integer> zeroCache = new LockingLRUCacheStrategy<>(0);
        zeroCache.put("Z", 5);
        assertEquals(0, zeroCache.size(), "Cache size must remain 0 for zero capacity");
        assertNull(zeroCache.get("Z"), "Entry should be evicted immediately");
    }

    @Test
    @DisplayName("negative capacity initialization – constructor throws IllegalArgumentException")
    void negativeCapacityInitialization() {
        assertThrows(IllegalArgumentException.class, () -> new LockingLRUCacheStrategy<String, Integer>(-1), "Creating a cache with negative capacity should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("capacity overflow with null key – null key participates in LRU ordering")
    void capacityOverflowWithNullKey() {
        // capacity = 1
        LockingLRUCacheStrategy<String, Integer> oneCache = new LockingLRUCacheStrategy<>(1);
        // null key is MRU
        oneCache.put(null, 111);
        // should evict null key (LRU)
        oneCache.put("X", 222);
        assertNull(oneCache.get(null), "Null key should have been evicted");
        assertEquals(222, oneCache.get("X"));
    }

    @Test
    @DisplayName("re‑entrancy deadlock detection – lock is re‑entrant")
    void reentrancyDeadlockDetection() throws Exception {
        // Obtain the private lock via reflection.
        Field lockField = LockingLRUCacheStrategy.class.getDeclaredField("lock");
        lockField.setAccessible(true);
        Lock internalLock = (Lock) lockField.get(cache);
        // Acquire the lock once (simulating that we already hold it).
        internalLock.lock();
        try {
            // Inside the same thread we call put again – it should acquire the same lock re‑entrantly.
            assertDoesNotThrow(() -> cache.put("R", 1), "Re‑entrant lock acquisition should not deadlock");
        } finally {
            // ensure we clean up
            internalLock.unlock();
        }
    }

    @Test
    @DisplayName("exception from underlying map – exception propagates and lock is released")
    void exceptionFromUnderlyingMap(@Mock ConcurrentHashMapNullSafe<Object, LockingLRUCacheStrategy.Node<String, Integer>> mockMap) throws Exception {
        // Configure mock to throw on put.
        when(mockMap.put(any(), any())).thenThrow(new RuntimeException("simulated failure"));
        // Inject mock into cache via reflection.
        Field cacheField = LockingLRUCacheStrategy.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        cacheField.set(cache, mockMap);
        // Capture lock state before invocation.
        Field lockField = LockingLRUCacheStrategy.class.getDeclaredField("lock");
        lockField.setAccessible(true);
        Lock lock = (Lock) lockField.get(cache);
        assertFalse(((ReentrantLock) lock).isLocked(), "Lock should be free before put");
        // Verify that the RuntimeException bubbles up.
        RuntimeException ex = assertThrows(RuntimeException.class, () -> cache.put("bad", 42), "Exception from underlying map must be propagated");
        assertEquals("simulated failure", ex.getMessage());
        // After exception the lock must be released.
        assertFalse(((ReentrantLock) lock).isLocked(), "Lock must be released after exception");
    }

    @Test
    @DisplayName("thread‑safety with concurrent puts – no deadlock and correct final size")
    void threadSafetyWithConcurrentPuts() throws InterruptedException {
        final int threadCount = 8;
        // more threads than distinct keys to force contention
        final int distinctKeys = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            final int id = i;
            executor.submit(() -> {
                // signal ready
                ready.countDown();
                try {
                    // wait for the go‑signal
                    start.await();
                    String key = "K" + (id % distinctKeys);
                    cache.put(key, id);
                } catch (InterruptedException ignored) {
                } finally {
                    done.countDown();
                }
            });
        }
        // Wait until all threads are ready, then fire them simultaneously.
        ready.await();
        start.countDown();
        done.await();
        executor.shutdownNow();
        // The cache capacity is 2, so final size must be 2 (the capacity) regardless of thread count.
        assertEquals(CAPACITY, cache.size(), "Final cache size must equal the configured capacity after concurrent puts");
        // Ensure that the two stored keys are among the distinct set.
        for (String k : cache.keySet()) {
            assertTrue(k.startsWith("K"), "Stored key should be one of the expected keys");
        }
    }

    @ParameterizedTest(name = "null key = {0}, null value = {1}")
    @CsvSource({ // null key, non‑null value
    "true, false", // non‑null key, null value
    "false, true", // both null
    "true, true" })
    @DisplayName("combined null key/value handling")
    void nullKeyAndValueCombined(boolean nullKey, boolean nullValue) {
        String key = nullKey ? null : "comboKey";
        Integer value = nullValue ? null : 777;
        cache.put(key, value);
        assertTrue(cache.containsKey(key), "Cache must contain the key even if it is null");
        assertEquals(value, cache.get(key), "Cache must return the correct (possibly null) value");
    }

    @Test
    @DisplayName("capacity eviction scenario described in JSON – LRU order after update")
    void capacityEvictionAfterUpdateRefreshesLRUOrder() {
        // Setup cache with capacity 2 and initial entries X and Y
        cache.put("X", 1);
        cache.put("Y", 2);
        // Update X to make it most‑recent
        cache.put("X", 10);
        // Insert Z – should evict Y
        cache.put("Z", 30);
        assertNull(cache.get("Y"), "Y should be evicted after X was refreshed and Z inserted");
        assertEquals(10, cache.get("X"));
        assertEquals(30, cache.get("Z"));
    }

    @Test
    @DisplayName("capacity eviction scenario – inserting third key evicts LRU")
    void capacityEvictionScenario() {
        // Cache capacity is 2
        cache.put("X", 5);
        cache.put("Y", 6);
        // Insert Z – should evict X (the LRU)
        cache.put("Z", 7);
        assertNull(cache.get("X"));
        assertEquals(6, cache.get("Y"));
        assertEquals(7, cache.get("Z"));
    }
}
