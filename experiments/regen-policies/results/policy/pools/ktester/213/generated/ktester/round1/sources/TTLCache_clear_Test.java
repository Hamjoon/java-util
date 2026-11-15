package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

// kept from the template; no mocks are required for this test
@ExtendWith(MockitoExtension.class)
class TTLCache_clear_Test {

    private TTLCache<Integer, String> ttlCache;

    @BeforeEach
    void setupBeforeEach() {
        // Create a cache with a long TTL (so entries do not expire during the test)
        ttlCache = new TTLCache<>(10_000L, -1);
        // Populate it with a few entries that we will later clear
        ttlCache.put(1, "One");
        ttlCache.put(2, "Two");
        ttlCache.put(3, "Three");
        // Sanity‑check that the entries are really there before each test runs
        assertEquals(3, ttlCache.size(), "Setup failed – cache should contain three entries");
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the cache is cleared between tests to avoid cross‑test interference.
        if (ttlCache != null) {
            ttlCache.clear();
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the background purge thread used by TTLCache.
        TTLCache.shutdown();
    }

    /**
     * Returns the {@code next} field of a Node via reflection.
     */
    private static Object getNodeNext(Object node) throws Exception {
        Field next = node.getClass().getDeclaredField("next");
        next.setAccessible(true);
        return next.get(node);
    }

    /**
     * Returns the {@code prev} field of a Node via reflection.
     */
    private static Object getNodePrev(Object node) throws Exception {
        Field prev = node.getClass().getDeclaredField("prev");
        prev.setAccessible(true);
        return prev.get(node);
    }

    /**
     * Retrieves the private {@code head} field of a TTLCache instance.
     */
    private static Object getHead(TTLCache<?, ?> cache) throws Exception {
        Field head = TTLCache.class.getDeclaredField("head");
        head.setAccessible(true);
        return head.get(cache);
    }

    /**
     * Retrieves the private {@code tail} field of a TTLCache instance.
     */
    private static Object getTail(TTLCache<?, ?> cache) throws Exception {
        Field tail = TTLCache.class.getDeclaredField("tail");
        tail.setAccessible(true);
        return tail.get(cache);
    }

    /**
     * Retrieves the private {@code cacheMap} field of a TTLCache instance.
     */
    private static ConcurrentMap<?, ?> getCacheMap(TTLCache<?, ?> cache) throws Exception {
        Field map = TTLCache.class.getDeclaredField("cacheMap");
        map.setAccessible(true);
        return (ConcurrentMap<?, ?>) map.get(cache);
    }

    /**
     * Sets a private field to a given value via reflection.
     */
    private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static Stream<Integer> largeEntryCounts() {
        return Stream.of(1000);
    }

    @Test
    void clearEmptiesCacheAndResetsState() {
        // WHEN
        ttlCache.clear();
        // THEN
        assertTrue(ttlCache.isEmpty(), "Cache should be empty after clear()");
        assertEquals(0, ttlCache.size(), "Size should be zero after clear()");
        assertFalse(ttlCache.containsKey(1), "Key 1 should not be present after clear()");
        assertFalse(ttlCache.containsKey(2), "Key 2 should not be present after clear()");
        assertFalse(ttlCache.containsKey(3), "Key 3 should not be present after clear()");
        // The linked‑list used for LRU is internal; we verify its effect indirectly:
        // after a clear, putting a new entry should succeed and be retrievable.
        ttlCache.put(4, "Four");
        assertEquals("Four", ttlCache.get(4), "Cache should accept new entries after clear()");
    }

    @Test
    void clearCanBeCalledMultipleTimesWithoutError() {
        // First clear
        ttlCache.clear();
        assertTrue(ttlCache.isEmpty());
        // Second clear – should be a no‑op but must not throw
        ttlCache.clear();
        assertTrue(ttlCache.isEmpty());
        // Third clear after adding new entries
        ttlCache.put(5, "Five");
        ttlCache.put(6, "Six");
        assertEquals(2, ttlCache.size());
        ttlCache.clear();
        assertTrue(ttlCache.isEmpty());
        assertNull(ttlCache.get(5));
        assertNull(ttlCache.get(6));
    }

    @Test
    void clearDoesNotAffectSubsequentCacheBehavior() {
        // Clear the cache
        ttlCache.clear();
        // Populate again with different keys
        ttlCache.put(10, "Ten");
        ttlCache.put(20, "Twenty");
        // Verify normal map semantics still hold
        assertEquals(2, ttlCache.size());
        assertEquals("Ten", ttlCache.get(10));
        assertEquals("Twenty", ttlCache.get(20));
        assertTrue(ttlCache.containsKey(10));
        assertTrue(ttlCache.containsKey(20));
    }

    @Test
    void clearOnNewlyCreatedEmptyCache() throws Exception {
        TTLCache<Integer, String> emptyCache = new TTLCache<>(10_000L, -1);
        // pre‑condition
        assertTrue(emptyCache.isEmpty());
        assertEquals(0, emptyCache.size());
        // clear should not throw
        assertDoesNotThrow(emptyCache::clear, "clear() on empty cache should not throw");
        // verify state
        assertTrue(emptyCache.isEmpty());
        assertEquals(0, emptyCache.size());
        // verify linked list reset
        Object head = getHead(emptyCache);
        Object tail = getTail(emptyCache);
        assertSame(tail, getNodeNext(head), "head.next should point to tail after clear");
        assertSame(head, getNodePrev(tail), "tail.prev should point to head after clear");
    }

    @Test
    void clearAfterPartialRemoval() throws Exception {
        TTLCache<String, String> cache = new TTLCache<>(10_000L, -1);
        // populate with 5 entries
        for (int i = 1; i <= 5; i++) {
            cache.put("k" + i, "v" + i);
        }
        // remove two entries
        cache.remove("k1");
        cache.remove("k2");
        assertEquals(3, cache.size(), "size before clear should be 3");
        // clear
        cache.clear();
        // verify empty
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
        // linked‑list reset
        Object head = getHead(cache);
        Object tail = getTail(cache);
        assertSame(tail, getNodeNext(head));
        assertSame(head, getNodePrev(tail));
    }

    @ParameterizedTest(name = "clearAfterPutAllWithManyEntries count={0}")
    @MethodSource("largeEntryCounts")
    void clearAfterPutAllWithManyEntries(int entryCount) throws Exception {
        TTLCache<Integer, Integer> cache = new TTLCache<>(10_000L, -1);
        Map<Integer, Integer> many = new HashMap<>();
        for (int i = 0; i < entryCount; i++) {
            many.put(i, i);
        }
        cache.putAll(many);
        assertEquals(entryCount, cache.size(), "size before clear");
        cache.clear();
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
        // linked‑list reset
        Object head = getHead(cache);
        Object tail = getTail(cache);
        assertSame(tail, getNodeNext(head));
        assertSame(head, getNodePrev(tail));
    }

    @Test
    void consecutiveClearCallsLockReleaseVerification() throws Exception {
        TTLCache<Integer, String> cache = new TTLCache<>(10_000L, -1);
        cache.put(1, "One");
        cache.put(2, "Two");
        // first clear
        assertDoesNotThrow(cache::clear);
        // second clear immediately
        assertDoesNotThrow(cache::clear);
        // verify empty and list reset
        assertTrue(cache.isEmpty());
        Object head = getHead(cache);
        Object tail = getTail(cache);
        assertSame(tail, getNodeNext(head));
        assertSame(head, getNodePrev(tail));
    }

    @Test
    void concurrentClearCallsFromMultipleThreads() throws Exception {
        TTLCache<Integer, String> cache = new TTLCache<>(10_000L, -1);
        cache.put(1, "One");
        cache.put(2, "Two");
        cache.put(3, "Three");
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                // each thread just calls clear()
                cache.clear();
                return null;
            }));
        }
        // wait for all
        for (Future<?> f : futures) {
            // will re‑throw any exception
            f.get(5, TimeUnit.SECONDS);
        }
        executor.shutdownNow();
        // final state checks
        assertTrue(cache.isEmpty(), "Cache should be empty after concurrent clears");
        assertEquals(0, cache.size());
        Object head = getHead(cache);
        Object tail = getTail(cache);
        assertSame(tail, getNodeNext(head));
        assertSame(head, getNodePrev(tail));
    }

    @Test
    void clearAfterAccessingKeySetView() throws Exception {
        TTLCache<Integer, String> cache = new TTLCache<>(10_000L, -1);
        cache.put(1, "One");
        cache.put(2, "Two");
        cache.put(3, "Three");
        // obtain view before clear
        Set<Integer> keySet = cache.keySet();
        assertEquals(3, keySet.size());
        cache.clear();
        assertTrue(cache.isEmpty());
        assertTrue(keySet.isEmpty(), "KeySet view must reflect cleared cache");
        assertEquals(0, cache.size());
        Object head = getHead(cache);
        Object tail = getTail(cache);
        assertSame(tail, getNodeNext(head));
        assertSame(head, getNodePrev(tail));
    }

    @Test
    void clearWithNullKeyAndNullValueEntries() throws Exception {
        TTLCache<String, String> cache = new TTLCache<>(10_000L, -1);
        // The implementation allows a sentinel for null keys/values.
        cache.put(null, "nullKey");
        cache.put("nullValue", null);
        assertEquals(2, cache.size());
        cache.clear();
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
        Object head = getHead(cache);
        Object tail = getTail(cache);
        assertSame(tail, getNodeNext(head));
        assertSame(head, getNodePrev(tail));
    }

    @Test
    void clearWithNullInternalCacheMapThrowsNPE() throws Exception {
        TTLCache<Integer, String> cache = new TTLCache<>(10_000L, -1);
        // inject null cacheMap
        setPrivateField(cache, "cacheMap", null);
        // clear should now throw NullPointerException when trying to call cacheMap.clear()
        assertThrows(NullPointerException.class, cache::clear, "clear() should throw NPE when internal cacheMap is null");
    }

    @Test
    void clearWithNullHeadNodeThrowsNPE() throws Exception {
        TTLCache<Integer, String> cache = new TTLCache<>(10_000L, -1);
        // set head to null, keep tail intact
        setPrivateField(cache, "head", null);
        // clear will acquire lock then attempt head.next = tail -> NPE
        assertThrows(NullPointerException.class, cache::clear, "clear() should throw NPE when head is null");
    }

    @Test
    void clearWithNullTailNodeThrowsNPE() throws Exception {
        TTLCache<Integer, String> cache = new TTLCache<>(10_000L, -1);
        // set tail to null, keep head intact
        setPrivateField(cache, "tail", null);
        // clear will attempt tail.prev = head -> NPE
        assertThrows(NullPointerException.class, cache::clear, "clear() should throw NPE when tail is null");
    }

    @Test
    void clearWhenUnderlyingMapThrowsException() throws Exception {
        // Create a mock ConcurrentMap that throws on clear()
        @SuppressWarnings("unchecked")
        ConcurrentMap<Integer, TTLCache.CacheEntry<Integer, String>> throwingMap = mock(ConcurrentMap.class);
        doThrow(new IllegalStateException("boom")).when(throwingMap).clear();
        TTLCache<Integer, String> cache = new TTLCache<>(10_000L, -1);
        // Replace the internal map with the throwing mock
        setPrivateField(cache, "cacheMap", throwingMap);
        // Expect the IllegalStateException to propagate
        IllegalStateException ex = assertThrows(IllegalStateException.class, cache::clear, "clear() should propagate exception from underlying map");
        assertEquals("boom", ex.getMessage());
    }
}
