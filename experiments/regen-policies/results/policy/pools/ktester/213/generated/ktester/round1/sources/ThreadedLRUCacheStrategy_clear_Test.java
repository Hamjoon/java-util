package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ThreadedLRUCacheStrategy_clear_Test {

    /**
     * Instance under test – created fresh for each test method.
     */
    private ThreadedLRUCacheStrategy<String, Integer> cache;

    /**
     * Common configuration values for the cache.
     */
    private static final int CAPACITY = 5;

    private static final int CLEANUP_DELAY_MILLIS = 50;

    @BeforeEach
    void setupBeforeEach() {
        // Create a new cache before each test to guarantee isolation.
        cache = new ThreadedLRUCacheStrategy<>(CAPACITY, CLEANUP_DELAY_MILLIS);
        // Populate the cache with a few entries (less than capacity) – used by many tests.
        cache.put("one", 1);
        cache.put("two", 2);
        cache.put("three", 3);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the cache reference is cleared so the scheduled purge task can be GC‑ed.
        cache = null;
    }

    /**
     * Helper that injects a value into the private {@code cache} field via reflection.
     */
    private void setInternalCache(Object newCache) throws Exception {
        Field field = ThreadedLRUCacheStrategy.class.getDeclaredField("cache");
        field.setAccessible(true);
        field.set(cache, newCache);
    }

    @Test
    void clear_RemovesAllEntries_andCacheIsEmpty() {
        // Given: the cache has three entries (verified via size())
        assertEquals(3, cache.size(), "Pre‑condition: cache should contain three entries");
        // When: clear() is invoked
        cache.clear();
        // Then: the cache reports as empty and size is zero
        assertTrue(cache.isEmpty(), "Cache should be empty after clear()");
        assertEquals(0, cache.size(), "Size should be zero after clear()");
        // Additionally, verify that previously stored keys are no longer present
        assertFalse(cache.containsKey("one"));
        assertFalse(cache.containsKey("two"));
        assertFalse(cache.containsKey("three"));
    }

    /* --------------------------------------------------------------
     *  Clear on an already empty cache – no‑op behavior
     * -------------------------------------------------------------- */
    @Test
    void clear_OnEmptyCache_IsNoOp() {
        // Create a fresh cache without any entries.
        ThreadedLRUCacheStrategy<String, Integer> emptyCache = new ThreadedLRUCacheStrategy<>(CAPACITY, CLEANUP_DELAY_MILLIS);
        assertTrue(emptyCache.isEmpty(), "Pre‑condition: cache must be empty");
        assertEquals(0, emptyCache.size());
        // When
        emptyCache.clear();
        // Then – still empty
        assertTrue(emptyCache.isEmpty(), "Cache should remain empty after clear()");
        assertEquals(0, emptyCache.size());
    }

    /* --------------------------------------------------------------
     *  Idempotent clear – calling clear() twice
     * -------------------------------------------------------------- */
    @Test
    void clear_TwiceInARow_LeavesCacheEmpty() {
        // First clear – makes cache empty.
        cache.clear();
        assertTrue(cache.isEmpty());
        // Second clear – should not throw and cache stays empty.
        cache.clear();
        assertTrue(cache.isEmpty(), "Cache must stay empty after second clear()");
        assertEquals(0, cache.size());
    }

    /* --------------------------------------------------------------
     *  Clear after partial removals
     * -------------------------------------------------------------- */
    @Test
    void clear_AfterPartialRemove_EmptiesCache() {
        // Remove a subset of entries.
        cache.remove("one");
        cache.remove("two");
        assertEquals(1, cache.size(), "Only one entry should remain before clear()");
        // Clear the cache.
        cache.clear();
        // Verify complete emptiness.
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
        assertFalse(cache.containsKey("three"));
        assertTrue(cache.keySet().isEmpty());
        assertTrue(cache.values().isEmpty());
    }

    /* --------------------------------------------------------------
     *  Clear after bulk putAll()
     * -------------------------------------------------------------- */
    @Test
    void clear_AfterPutAll_EmptiesCache() {
        // Build a map with more entries than the default capacity.
        Map<String, Integer> bulk = new HashMap<>();
        for (int i = 0; i < 20; i++) {
            bulk.put("k" + i, i);
        }
        // Use a fresh cache to avoid interference from @BeforeEach population.
        ThreadedLRUCacheStrategy<String, Integer> bulkCache = new ThreadedLRUCacheStrategy<>(CAPACITY, CLEANUP_DELAY_MILLIS);
        bulkCache.putAll(bulk);
        assertEquals(bulk.size(), bulkCache.size(), "Cache should contain all entries added via putAll()");
        // When
        bulkCache.clear();
        // Then
        assertTrue(bulkCache.isEmpty(), "Cache must be empty after clear()");
        assertEquals(0, bulkCache.size());
        assertTrue(bulkCache.keySet().isEmpty());
        assertTrue(bulkCache.values().isEmpty());
    }

    @Test
    void clear_WhenInternalCacheIsNull_ThrowsNullPointerException() throws Exception {
        // Arrange – force the internal cache reference to null.
        setInternalCache(null);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> cache.clear(), "clear() should throw NPE when internal cache is null");
    }

    @Test
    void clear_WhenInternalCacheClearThrowsUnsupportedOperationException_Propagates() throws Exception {
        // Arrange – mock a ConcurrentMap whose clear() throws UnsupportedOperationException.
        @SuppressWarnings("unchecked")
        ConcurrentMap<String, ThreadedLRUCacheStrategy.Node<String, Integer>> mockMap = mock(ConcurrentMap.class);
        doThrow(new UnsupportedOperationException("clear not supported")).when(mockMap).clear();
        setInternalCache(mockMap);
        // Act & Assert
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> cache.clear(), "clear() must propagate UnsupportedOperationException from internal map");
        assertEquals("clear not supported", ex.getMessage());
    }

    @Test
    void clear_WhenInternalCacheClearThrowsIllegalStateException_Propagates() throws Exception {
        // Arrange – mock a ConcurrentMap whose clear() throws IllegalStateException.
        @SuppressWarnings("unchecked")
        ConcurrentMap<String, ThreadedLRUCacheStrategy.Node<String, Integer>> mockMap = mock(ConcurrentMap.class);
        doThrow(new IllegalStateException("illegal state during clear")).when(mockMap).clear();
        setInternalCache(mockMap);
        // Act & Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> cache.clear(), "clear() must propagate IllegalStateException from internal map");
        assertEquals("illegal state during clear", ex.getMessage());
    }

    /* --------------------------------------------------------------
     *  Parameterized test for idempotent clear on various initial states
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "clearIdempotent_{index}")
    @ValueSource(booleans = { true, false })
    void clear_Idempotent_WithDifferentInitialStates(boolean prePopulate) {
        // Create a fresh cache for each iteration.
        ThreadedLRUCacheStrategy<String, Integer> localCache = new ThreadedLRUCacheStrategy<>(CAPACITY, CLEANUP_DELAY_MILLIS);
        if (prePopulate) {
            // Populate with a few entries.
            localCache.put("a", 1);
            localCache.put("b", 2);
        }
        // First clear – should make the cache empty regardless of prior state.
        localCache.clear();
        assertTrue(localCache.isEmpty());
        // Second clear – idempotent check.
        localCache.clear();
        assertTrue(localCache.isEmpty());
        assertEquals(0, localCache.size());
    }
}
