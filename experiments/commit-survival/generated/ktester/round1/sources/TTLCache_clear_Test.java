package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

@ExtendWith(MockitoExtension.class)
class TTLCache_clear_Test {

    private TTLCache<Integer, String> ttlCache;

    /**
     * Creates a fresh cache instance before each test. The cache is configured with a
     * relatively long TTL so that entries do not expire during the test execution.
     */
    @BeforeEach
    void setupBeforeEach() {
        // 10‑second TTL, no LRU limit
        ttlCache = new TTLCache<>(10_000, -1);
        // Populate the cache with a few entries that we can later verify are removed.
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        ttlCache.put(3, "C");
    }

    /**
     * After each test we clear the reference to help the GC and ensure that any
     * background purge task associated with the cache instance is no longer reachable.
     */
    @AfterEach
    void teardownAfterEach() {
        if (ttlCache != null) {
            // guarantee a clean state before the next test
            ttlCache.clear();
            ttlCache = null;
        }
    }

    /**
     * Shuts down the shared background executor used by {@link TTLCache}.
     */
    @AfterAll
    static void teardownAfterAll() {
        TTLCache.shutdown();
    }

    /**
     * Helper to set a private final field via reflection.
     */
    private static void setPrivateField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        // remove final modifier
        Field modifiers = Field.class.getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(f, f.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
        f.set(target, value);
    }

    @Test
    void clearRemovesAllEntriesAndLeavesCacheEmpty() {
        // Pre‑condition: the cache contains the three entries added in @BeforeEach
        assertEquals(3, ttlCache.size(), "Cache should contain three entries before clear");
        assertFalse(ttlCache.isEmpty(), "Cache should not be empty before clear");
        // Act
        ttlCache.clear();
        // Assert
        assertEquals(0, ttlCache.size(), "Cache size must be zero after clear");
        assertTrue(ttlCache.isEmpty(), "Cache should be empty after clear");
        assertNull(ttlCache.get(1), "Previously stored key 1 must be removed");
        assertNull(ttlCache.get(2), "Previously stored key 2 must be removed");
        assertNull(ttlCache.get(3), "Previously stored key 3 must be removed");
    }

    @Test
    void clearOnAlreadyEmptyCacheDoesNotThrow() {
        // Arrange: empty the cache first
        ttlCache.clear();
        assertTrue(ttlCache.isEmpty(), "Cache should be empty before invoking clear again");
        // Act & Assert: second clear should be a no‑op and must not throw
        assertDoesNotThrow(() -> ttlCache.clear(), "Calling clear on an empty cache should not throw");
        assertTrue(ttlCache.isEmpty(), "Cache should remain empty after second clear");
    }

    @Test
    void clearAllowsSubsequentPutOperations() {
        // Act: clear the cache
        ttlCache.clear();
        // Verify that the cache behaves like a fresh instance after clear
        ttlCache.put(42, "Answer");
        assertEquals(1, ttlCache.size(), "Cache should contain one entry after put following clear");
        assertEquals("Answer", ttlCache.get(42), "Value retrieved after clear should match the one put");
    }

    @Test
    void clearResetsLRUOrderWhenLRUIsEnabled() {
        // Use a cache with LRU enabled
        TTLCache<Integer, String> lruCache = new TTLCache<>(10_000, 3);
        lruCache.put(1, "A");
        lruCache.put(2, "B");
        lruCache.put(3, "C");
        // Access key 1 to make it most recently used
        lruCache.get(1);
        // Clear the cache
        lruCache.clear();
        // After clear, the cache should be empty and a new entry should not be evicted immediately
        lruCache.put(4, "D");
        assertEquals(1, lruCache.size(), "Cache should contain exactly one entry after clear and a new put");
        assertEquals("D", lruCache.get(4), "The newly inserted entry must be present");
        // No eviction should happen because the cache is effectively fresh
        assertNull(lruCache.get(1), "Previously stored key 1 must have been removed by clear");
    }

    /**
     * Helper to read a private field via reflection.
     */
    @SuppressWarnings("unchecked")
    private static <T> T getPrivateField(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return (T) f.get(target);
    }

    @Test
    void clearResetsLinkedListPointersOnEmptyCache() throws Exception {
        // create a brand‑new cache that has never stored entries
        TTLCache<Integer, String> emptyCache = new TTLCache<>(10_000, -1);
        // sanity check – internal map should be empty
        assertEquals(0, emptyCache.size());
        emptyCache.clear();
        // verify that the sentinel nodes are linked together again
        TTLCache.Node<Integer, String> head = getPrivateField(emptyCache, "head");
        TTLCache.Node<Integer, String> tail = getPrivateField(emptyCache, "tail");
        assertSame(tail, head.next, "head.next must point to tail after clear()");
        assertSame(head, tail.prev, "tail.prev must point to head after clear()");
    }

    @Test
    void clearResetsLinkedListPointersWhenCacheHadEntries() throws Exception {
        // pre‑condition: three entries are present (set up in @BeforeEach)
        assertEquals(3, ttlCache.size());
        ttlCache.clear();
        TTLCache.Node<Integer, String> head = getPrivateField(ttlCache, "head");
        TTLCache.Node<Integer, String> tail = getPrivateField(ttlCache, "tail");
        assertSame(tail, head.next, "head.next must be reset to tail");
        assertSame(head, tail.prev, "tail.prev must be reset to head");
    }

    @Test
    void lockIsReleasedEvenWhenClearThrowsException() throws Exception {
        // Replace the internal map with a mock that throws on clear()
        @SuppressWarnings("unchecked")
        ConcurrentMap<Integer, TTLCache.CacheEntry<Integer, String>> mockMap = mock(ConcurrentMap.class);
        doThrow(new IllegalStateException("forced")).when(mockMap).clear();
        setPrivateField(ttlCache, "cacheMap", mockMap);
        // Capture the internal lock for later verification
        ReentrantLock lock = getPrivateField(ttlCache, "lock");
        assertTrue(lock.tryLock(), "pre‑condition: lock must be free");
        lock.unlock();
        // Act & assert: clear() propagates the exception
        IllegalStateException ex = assertThrows(IllegalStateException.class, ttlCache::clear);
        assertEquals("forced", ex.getMessage());
        // The lock must be free again – tryLock should succeed
        assertTrue(lock.tryLock(), "lock must be released after exception");
        lock.unlock();
    }

    @Test
    void clearCanBeInvokedConcurrentlyWithoutCorruptingState() throws Exception {
        TTLCache<Integer, String> concurrentCache = new TTLCache<>(10_000, -1);
        ExecutorService exec = Executors.newFixedThreadPool(3);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(3);
        // Thread 1 – continuously puts entries
        exec.submit(() -> {
            try {
                startLatch.await();
                for (int i = 0; i < 500; i++) {
                    concurrentCache.put(i, "V" + i);
                }
            } catch (InterruptedException ignored) {
            } finally {
                doneLatch.countDown();
            }
        });
        // Thread 2 – calls clear()
        exec.submit(() -> {
            try {
                startLatch.await();
                concurrentCache.clear();
            } catch (InterruptedException ignored) {
            } finally {
                doneLatch.countDown();
            }
        });
        // Thread 3 – also calls clear()
        exec.submit(() -> {
            try {
                startLatch.await();
                concurrentCache.clear();
            } catch (InterruptedException ignored) {
            } finally {
                doneLatch.countDown();
            }
        });
        // start all threads
        startLatch.countDown();
        // wait for completion
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "threads should finish quickly");
        // After the race, the cache must be empty and the linked list reset
        assertEquals(0, concurrentCache.size(), "cache must be empty after concurrent clears");
        TTLCache.Node<Integer, String> head = getPrivateField(concurrentCache, "head");
        TTLCache.Node<Integer, String> tail = getPrivateField(concurrentCache, "tail");
        assertSame(tail, head.next, "head.next must point to tail");
        assertSame(head, tail.prev, "tail.prev must point to head");
        exec.shutdownNow();
    }

    @Test
    void clearThrowsWhenCacheMapIsNull() throws Exception {
        setPrivateField(ttlCache, "cacheMap", null);
        NullPointerException npe = assertThrows(NullPointerException.class, ttlCache::clear);
        assertNotNull(npe);
    }

    @Test
    void clearThrowsWhenHeadIsNull() throws Exception {
        setPrivateField(ttlCache, "head", null);
        NullPointerException npe = assertThrows(NullPointerException.class, ttlCache::clear);
        assertNotNull(npe);
    }

    @Test
    void clearThrowsWhenTailIsNull() throws Exception {
        setPrivateField(ttlCache, "tail", null);
        NullPointerException npe = assertThrows(NullPointerException.class, ttlCache::clear);
        assertNotNull(npe);
    }

    @Test
    void clearThrowsWhenLockIsNull() throws Exception {
        setPrivateField(ttlCache, "lock", null);
        NullPointerException npe = assertThrows(NullPointerException.class, ttlCache::clear);
        assertNotNull(npe);
    }

    @Test
    void clearPropagatesExceptionFromCustomMapAndReleasesLock() throws Exception {
        // custom map whose clear() always throws
        @SuppressWarnings("unchecked")
        ConcurrentMap<Integer, TTLCache.CacheEntry<Integer, String>> explodingMap = mock(ConcurrentMap.class);
        doThrow(new IllegalStateException("boom")).when(explodingMap).clear();
        setPrivateField(ttlCache, "cacheMap", explodingMap);
        ReentrantLock lock = getPrivateField(ttlCache, "lock");
        assertTrue(lock.tryLock(), "pre‑condition: lock must be free");
        lock.unlock();
        IllegalStateException ise = assertThrows(IllegalStateException.class, ttlCache::clear);
        assertEquals("boom", ise.getMessage());
        // lock must be free again
        assertTrue(lock.tryLock(), "lock must be released after exception");
        lock.unlock();
    }
}
