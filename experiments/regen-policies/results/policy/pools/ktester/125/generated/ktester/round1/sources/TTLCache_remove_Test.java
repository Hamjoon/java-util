package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TTLCache_remove_Test {

    private TTLCache<Integer, String> ttlCache;

    /**
     * Create a fresh cache before each test.
     */
    @BeforeEach
    void setupBeforeEach() {
        // TTL of 10 seconds, no LRU limit (maxSize = -1). The default cleanup interval is fine.
        ttlCache = new TTLCache<>(10_000L, -1);
    }

    /**
     * Clear the cache after each test to avoid cross‑test contamination.
     */
    @AfterEach
    void teardownAfterEach() {
        ttlCache.clear();
    }

    /**
     * Global resources (e.g., background purge thread) are shut down once all tests finish.
     */
    @AfterAll
    static void teardownAfterAll() {
        TTLCache.shutdown();
    }

    private static Stream<Arguments> existingEntryProvider() {
        return Stream.of(Arguments.of("alpha", "value1"), // note: value is a String because TTLCache<V> is String in the test class
        Arguments.of(42, "123"));
    }

    private static Stream<Arguments> nonExistingEntryProvider() {
        return Stream.of(Arguments.of("missing"), Arguments.of(999));
    }

    @Test
    void removeExistingKey_returnsValueAndDeletesEntry() {
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        ttlCache.put(3, "C");
        // Verify initial state
        assertEquals(3, ttlCache.size());
        assertTrue(ttlCache.containsKey(2));
        // Remove key 2
        String removed = ttlCache.remove(2);
        assertEquals("B", removed, "remove should return the previous value");
        // Verify that the entry is gone
        assertNull(ttlCache.get(2), "get should return null after removal");
        assertFalse(ttlCache.containsKey(2), "containsKey should be false after removal");
        assertEquals(2, ttlCache.size(), "size should decrement after removal");
    }

    @Test
    void removeNonExistingKey_returnsNullAndLeavesCacheUnchanged() {
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        // Attempt to remove a key that was never added
        String result = ttlCache.remove(99);
        assertNull(result, "remove should return null for absent key");
        // Cache state must be unchanged
        assertEquals(2, ttlCache.size());
        assertTrue(ttlCache.containsKey(1));
        assertTrue(ttlCache.containsKey(2));
    }

    @Test
    void removeFromEmptyCache_returnsNull() {
        assertTrue(ttlCache.isEmpty(), "Cache should start empty");
        assertNull(ttlCache.remove(1), "remove on empty cache must return null");
    }

    @Test
    void removeNullKey_behavesAccordingToMapContract() {
        // TTLCache supports null keys via a sentinel; we can test removal of a null key.
        ttlCache.put(null, "nullValue");
        assertTrue(ttlCache.containsKey(null));
        assertEquals("nullValue", ttlCache.remove(null));
        assertFalse(ttlCache.containsKey(null));
        assertNull(ttlCache.get(null));
    }

    @Test
    void consecutiveRemovalsOfSameKey_returnNullAfterFirstRemoval() {
        ttlCache.put(5, "E");
        assertEquals("E", ttlCache.remove(5), "First removal should return the value");
        assertNull(ttlCache.remove(5), "Second removal of the same key should return null");
        assertEquals(0, ttlCache.size(), "Cache should be empty after removal");
    }

    /* -------------------------------------------------------------
     *  Parameterised tests for "remove existing entry"
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] remove existing key {0} → {1}")
    @MethodSource("existingEntryProvider")
    void removeExistingEntry(Object key, String expectedValue) {
        // Populate the cache with the key/value pair under test.
        @SuppressWarnings("unchecked")
        TTLCache<Object, String> cache = (TTLCache<Object, String>) ttlCache;
        cache.put(key, expectedValue);
        // Pre‑condition checks
        assertTrue(cache.containsKey(key));
        int sizeBefore = cache.size();
        // Act
        String removed = cache.remove(key);
        // Assert
        assertEquals(expectedValue, removed, "remove should return the previous value");
        assertNull(cache.get(key), "value must be gone after removal");
        assertFalse(cache.containsKey(key), "containsKey must be false after removal");
        assertEquals(sizeBefore - 1, cache.size(), "size must decrement");
    }

    /* -------------------------------------------------------------
     *  Parameterised tests for "remove non‑existing entry"
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] remove non‑existing key {0}")
    @MethodSource("nonExistingEntryProvider")
    void removeNonExistingEntry(Object key) {
        // Populate the cache with some other entries
        ttlCache.put(1, "one");
        ttlCache.put(2, "two");
        int sizeBefore = ttlCache.size();
        // Act
        String result = ttlCache.remove(key);
        // Assert
        assertNull(result, "remove must return null for absent key");
        assertEquals(sizeBefore, ttlCache.size(), "size must stay unchanged");
        assertTrue(ttlCache.containsKey(1));
        assertTrue(ttlCache.containsKey(2));
    }

    /* -------------------------------------------------------------
     *  Null‑key handling – expects NullPointerException
     * ------------------------------------------------------------- */
    @Test
    void removeNullKey_propagatesNullPointerException() {
        // According to the JSON spec, the underlying ConcurrentMap rejects null keys.
        // We therefore expect a NullPointerException.
        assertThrows(NullPointerException.class, () -> ttlCache.remove(null));
    }

    /* -------------------------------------------------------------
     *  Verify lock handling – lock must be released after remove
     * ------------------------------------------------------------- */
    @Test
    void verifyLockHandling_lockIsReleasedAfterRemoval() throws Exception {
        ttlCache.put(99, "val");
        // Access the private lock via reflection
        Field lockField = TTLCache.class.getDeclaredField("lock");
        lockField.setAccessible(true);
        ReentrantLock lock = (ReentrantLock) lockField.get(ttlCache);
        assertFalse(lock.isLocked(), "pre‑condition: lock must not be held");
        String removed = ttlCache.remove(99);
        assertEquals("val", removed);
        // After the method returns the lock must be free.
        assertFalse(lock.isLocked(), "lock must be released after remove");
    }

    /* -------------------------------------------------------------
     *  Remove after cache clear
     * ------------------------------------------------------------- */
    @Test
    void removeAfterCacheClear_returnsNull() {
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        ttlCache.clear();
        assertTrue(ttlCache.isEmpty(), "cache should be empty after clear");
        assertNull(ttlCache.remove(1), "remove on cleared cache must return null");
    }

    /* -------------------------------------------------------------
     *  Unsupported underlying map – throws UnsupportedOperationException
     * ------------------------------------------------------------- */
    @Test
    void removeWithUnsupportedUnderlyingMap_propagatesException() throws Exception {
        // Create a mock ConcurrentMap that throws on remove()
        @SuppressWarnings("unchecked")
        ConcurrentMap<Integer, TTLCache.CacheEntry<Integer, String>> mockMap = mock(ConcurrentMap.class);
        when(mockMap.remove(any())).thenThrow(new UnsupportedOperationException("remove not supported"));
        // Inject the mock map into a fresh TTLCache instance via reflection
        TTLCache<Integer, String> cache = new TTLCache<>(10_000L, -1);
        Field mapField = TTLCache.class.getDeclaredField("cacheMap");
        mapField.setAccessible(true);
        mapField.set(cache, mockMap);
        // The cache now delegates to the mocked map; remove should propagate the exception.
        assertThrows(UnsupportedOperationException.class, () -> cache.remove(10));
    }

    /* -------------------------------------------------------------
     *  unlink throws exception – this scenario is difficult to trigger
     *  without deep instrumentation. The test is provided but disabled.
     * ------------------------------------------------------------- */
    @Disabled("Testing private unlink exception requires byte‑code manipulation or a custom subclass; " + "out of scope for plain unit tests.")
    @Test
    void removeWhenUnlinkThrowsException_propagatesException() {
        // Placeholder for a potential implementation using a custom subclass or instrumentation.
        // The expectation is that an IllegalStateException thrown from unlink is propagated
        // and the lock is released (no deadlock).
    }
}
