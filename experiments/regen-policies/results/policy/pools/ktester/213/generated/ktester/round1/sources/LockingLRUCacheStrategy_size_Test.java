package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;

class LockingLRUCacheStrategy_size_Test {

    private static final int CAPACITY = 4;

    private LockingLRUCacheStrategy<String, Integer> cache;

    private static final int DEFAULT_CAPACITY = 4;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh cache for each test to guarantee isolation
        cache = new LockingLRUCacheStrategy<>(CAPACITY);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the cache is cleared after each test; this also exercises clear()
        cache.clear();
    }

    @Test
    @DisplayName("size() returns 0 for a newly created cache")
    void sizeIsZeroWhenEmpty() {
        // given: a freshly created cache (handled by @BeforeEach)
        // when:
        int size = cache.size();
        // then:
        assertEquals(0, size, "New cache should have size 0");
    }

    @Test
    @DisplayName("size() reflects the number of entries after puts")
    void sizeReflectsNumberOfPutEntries() {
        // given:
        cache.put("one", 1);
        cache.put("two", 2);
        cache.put("three", 3);
        // when:
        int size = cache.size();
        // then:
        assertEquals(3, size, "Cache size should match number of inserted entries");
    }

    @Test
    @DisplayName("size() decreases after removing an entry")
    void sizeDecreasesAfterRemove() {
        // given:
        cache.put("a", 10);
        cache.put("b", 20);
        cache.put("c", 30);
        assertEquals(3, cache.size(), "Pre‑condition: size should be 3");
        // when:
        cache.remove("b");
        // then:
        assertEquals(2, cache.size(), "Size should decrement after removal");
    }

    @Test
    @DisplayName("size() becomes zero after clear()")
    void sizeBecomesZeroAfterClear() {
        // given:
        cache.put("x", 100);
        cache.put("y", 200);
        assertTrue(cache.size() > 0, "Pre‑condition: cache should not be empty");
        // when:
        cache.clear();
        // then:
        assertEquals(0, cache.size(), "Cache size should be 0 after clear()");
        assertTrue(cache.isEmpty(), "Cache should report empty after clear()");
    }

    @Test
    @DisplayName("size() respects LRU eviction when capacity is exceeded")
    void sizeRespectsCapacityAfterEviction() {
        // given: capacity is 4, insert 5 entries to trigger eviction
        cache.put("k1", 1);
        cache.put("k2", 2);
        cache.put("k3", 3);
        cache.put("k4", 4);
        // cache is now full
        assertEquals(4, cache.size(), "Cache should be at full capacity");
        // when: add one more entry, causing the least‑recently‑used entry to be evicted
        cache.put("k5", 5);
        // then: size remains equal to capacity
        assertEquals(4, cache.size(), "Cache size should stay at capacity after eviction");
    }

    // ---------- size() edge cases ----------
    @Test
    @DisplayName("size() on a cache with capacity 0 always returns 0")
    void sizeWithZeroCapacity() {
        LockingLRUCacheStrategy<String, Integer> zeroCapCache = new LockingLRUCacheStrategy<>(0);
        zeroCapCache.put("a", 1);
        zeroCapCache.put("b", 2);
        assertEquals(0, zeroCapCache.size(), "Cache with capacity 0 must report size 0");
    }

    @Test
    @DisplayName("size() after immediate clear on a fresh cache")
    void sizeAfterImmediateClear() {
        // clear without any puts
        cache.clear();
        assertEquals(0, cache.size(), "Size should remain 0 after clear on empty cache");
    }

    // ---------- duplicate keys ----------
    @Test
    @DisplayName("size() unchanged when same key is put twice")
    void sizeUnchangedOnDuplicateKey() {
        cache.put("dup", 1);
        // replacement
        cache.put("dup", 2);
        assertEquals(1, cache.size(), "Replacing an existing key must not change size");
    }

    @Test
    @DisplayName("size() unchanged after replacing one of three keys")
    void sizeUnchangedAfterPartialReplacement() {
        cache.put("k1", 1);
        cache.put("k2", 2);
        cache.put("k3", 3);
        // replace existing
        cache.put("k2", 22);
        assertEquals(3, cache.size(), "Size should stay 3 after replacing an existing key");
    }

    // ---------- null handling ----------
    @Test
    @DisplayName("put(null, value) throws NullPointerException and size stays 0")
    void putNullKeyThrows() {
        assertThrows(NullPointerException.class, () -> cache.put(null, 1), "Putting a null key should throw NPE");
        assertEquals(0, cache.size(), "Size must remain 0 after failed null‑key put");
    }

    @Test
    @DisplayName("put(non‑null key, null value) increases size")
    void putNullValueAllowed() {
        cache.put("key", null);
        assertEquals(1, cache.size(), "Null values are allowed, size should be 1");
    }

    // ---------- putAll overlapping keys ----------
    @Test
    @DisplayName("putAll with overlapping keys results in correct size")
    void putAllOverlappingKeys() {
        cache.put("a", 1);
        cache.put("b", 2);
        Map<String, Integer> toAdd = new LinkedHashMap<>();
        // overlap
        toAdd.put("b", 20);
        toAdd.put("c", 3);
        toAdd.put("d", 4);
        cache.putAll(toAdd);
        assertEquals(4, cache.size(), "After putAll, distinct keys should be 4");
    }

    @Test
    @DisplayName("putAll with duplicate keys inside the map counts each key once")
    void putAllWithDuplicateKeysInMap() {
        // LinkedHashMap cannot contain duplicate keys, so we simulate by using a map that
        // contains the same logical key twice via putAll on a map that already has the key.
        cache.put("x", 1);
        Map<String, Integer> dupMap = new LinkedHashMap<>();
        dupMap.put("y", 2);
        // last put wins, still one distinct key
        dupMap.put("y", 3);
        cache.putAll(dupMap);
        assertEquals(2, cache.size(), "Only distinct keys count, size should be 2");
    }

    // ---------- removing non‑existent key ----------
    @Test
    @DisplayName("remove non‑existent key does not change size")
    void removeNonExistentKeyKeepsSize() {
        cache.put("a", 1);
        cache.put("b", 2);
        assertEquals(2, cache.size());
        // not present
        cache.remove("c");
        assertEquals(2, cache.size(), "Size must stay unchanged when removing absent key");
    }

    @Test
    @DisplayName("remove from empty cache keeps size 0")
    void removeFromEmptyCache() {
        assertEquals(0, cache.size());
        cache.remove("nothing");
        assertEquals(0, cache.size(), "Removing from empty cache should keep size 0");
    }

    // ---------- concurrency ----------
    @Test
    @DisplayName("concurrent inserts from multiple threads result in correct size")
    void concurrentInserts() throws InterruptedException {
        int threadCount = 10;
        int perThread = 10;
        ExecutorService exec = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            exec.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < perThread; j++) {
                        cache.put("t" + threadId + "k" + j, j);
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        // let all threads run
        startLatch.countDown();
        doneLatch.await();
        exec.shutdownNow();
        assertEquals(threadCount * perThread, cache.size(), "All distinct keys from all threads should be present");
    }

    @Test
    @DisplayName("concurrent duplicate puts result in size equal to unique keys")
    void concurrentDuplicatePuts() throws InterruptedException {
        int threadCount = 5;
        // same keys across threads
        int perThread = 10;
        ExecutorService exec = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            exec.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < perThread; j++) {
                        cache.put("key" + j, j);
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        startLatch.countDown();
        doneLatch.await();
        exec.shutdownNow();
        assertEquals(perThread, cache.size(), "Only unique keys should be counted despite many duplicate puts");
    }

    // ---------- exception scenarios ----------
    @Test
    @DisplayName("size() throws NullPointerException when internal cache field is null")
    void sizeThrowsWhenInternalCacheNull() throws Exception {
        // Use reflection to set the private 'cache' field to null
        Field cacheField = LockingLRUCacheStrategy.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        cacheField.set(cache, null);
        assertThrows(NullPointerException.class, cache::size, "Calling size() with a null internal cache should throw NPE");
    }

    @Test
    @DisplayName("size() propagates exception thrown by internal cache")
    void sizePropagatesInternalCacheException() throws Exception {
        // Create a mock of ConcurrentHashMapNullSafe that throws on size()
        @SuppressWarnings("unchecked")
        ConcurrentHashMapNullSafe<Object, LockingLRUCacheStrategy.Node<String, Integer>> mockMap = (ConcurrentHashMapNullSafe<Object, LockingLRUCacheStrategy.Node<String, Integer>>) Mockito.mock(ConcurrentHashMapNullSafe.class);
        when(mockMap.size()).thenThrow(new UnsupportedOperationException("mocked"));
        // Inject the mock via reflection
        Field cacheField = LockingLRUCacheStrategy.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        cacheField.set(cache, mockMap);
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, cache::size, "Exception from internal map should be propagated");
        assertEquals("mocked", ex.getMessage());
    }
}
