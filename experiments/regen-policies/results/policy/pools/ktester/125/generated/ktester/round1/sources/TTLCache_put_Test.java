package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

@ExtendWith(MockitoExtension.class)
class TTLCache_put_Test {

    /**
     * Cache instance used by the individual test methods.
     */
    private TTLCache<Integer, String> ttlCache;

    /**
     * Initialise a fresh cache before each test.
     */
    @BeforeEach
    void setupBeforeEach() {
        // Default TTL of 10 seconds, no size limit – suitable for most tests.
        ttlCache = new TTLCache<>(10_000L, -1);
    }

    /**
     * Clean up after each test – ensure the cache reference is cleared.
     */
    @AfterEach
    void teardownAfterEach() {
        ttlCache = null;
    }

    /**
     * Global clean‑up – shut down the background purge thread used by all caches.
     */
    @AfterAll
    static void teardownAfterAll() {
        TTLCache.shutdown();
    }

    /**
     * Verify that {@code put} returns {@code null} for a new key and the previous
     * value when the key already exists.
     */
    @Test
    void testPutReturnsPreviousValue() {
        // First insertion – no previous mapping.
        String previous = ttlCache.put(1, "A");
        assertNull(previous, "First insertion should return null");
        // Second insertion with a different key – also null.
        assertNull(ttlCache.put(2, "B"));
        // Overwrite existing key – should return the old value.
        previous = ttlCache.put(1, "A2");
        assertEquals("A", previous, "Overwriting should return the previous value");
        // Confirm the new value is stored.
        assertEquals("A2", ttlCache.get(1));
    }

    /**
     * Verify that {@code put} correctly updates the cache size and that the
     * LRU eviction policy works when a maximum size is defined.
     */
    @Test
    void testPutWithLRUEviction() {
        // Cache with a maximum of 2 entries.
        ttlCache = new TTLCache<>(10_000L, 2);
        // cache: [1]
        ttlCache.put(1, "A");
        // cache: [1,2] (LRU order: 1 oldest)
        ttlCache.put(2, "B");
        assertEquals(2, ttlCache.size());
        // Access key 1 to make it most‑recently used.
        assertEquals("A", ttlCache.get(1));
        // Insert a third entry – should evict the least recently used (key 2).
        ttlCache.put(3, "C");
        // Verify eviction.
        assertNull(ttlCache.get(2), "Key 2 should have been evicted by LRU policy");
        assertEquals("A", ttlCache.get(1), "Key 1 should still be present");
        assertEquals("C", ttlCache.get(3), "Key 3 should be present");
        assertEquals(2, ttlCache.size(), "Cache size should respect maxSize");
    }

    /**
     * Ensure that an entry put into the cache can be retrieved before its TTL expires.
     */
    @Test
    void testPutAndImmediateGetBeforeExpiration() {
        // Very short TTL but we will read immediately.
        ttlCache = new TTLCache<>(500L, -1, 1_000L);
        ttlCache.put(10, "X");
        assertEquals("X", ttlCache.get(10), "Value should be retrievable before TTL expires");
        assertTrue(ttlCache.containsKey(10));
        assertEquals(1, ttlCache.size());
    }

    /**
     * Verify that {@code putAll} (which delegates to {@code put}) respects the same
     * semantics – this also indirectly tests the {@code put} implementation.
     */
    @Test
    void testPutAllDelegatesToPut() {
        Map<Integer, String> source = Map.of(1, "One", 2, "Two", 3, "Three");
        ttlCache.putAll(source);
        // All entries should be present.
        source.forEach((k, v) -> assertEquals(v, ttlCache.get(k)));
        // Size should match the source map.
        assertEquals(source.size(), ttlCache.size());
    }

    @ParameterizedTest(name = "[{index}] put({0}, {1}) → expected: {2}")
    @CsvSource({ // key, value, expectedPrevious (null means no previous entry)
    "'A', 'alpha', ''", "'A', 'beta',  'alpha'" })
    void testBasicPutBehavior(String key, String value, String expectedPrevious) {
        // The cache stores Integer keys, but the test data uses Strings.
        // Convert to Integer by using the string's hashCode for simplicity.
        Integer intKey = key.hashCode();
        // First call (or subsequent) – capture the return value.
        String previous = ttlCache.put(intKey, value);
        if (expectedPrevious.isEmpty()) {
            assertNull(previous, "First insertion should return null");
        } else {
            assertEquals(expectedPrevious, previous, "When overwriting an existing key the previous value should be returned");
        }
        // Verify that the new value is stored.
        assertEquals(value, ttlCache.get(intKey), "The cache must contain the newly inserted value");
    }

    @Test
    void testPutWithTTLAndImmediateRead() {
        // Very short TTL (200 ms) and no size limit.
        ttlCache = new TTLCache<>(200L, -1);
        ttlCache.put(10, "X");
        assertEquals("X", ttlCache.get(10), "Value must be readable before TTL expires");
        assertTrue(ttlCache.containsKey(10));
    }

    @Test
    void testEntryExpiresAfterTTL() throws InterruptedException {
        // TTL = 100 ms, no size limit.
        ttlCache = new TTLCache<>(100L, -1);
        ttlCache.put(1, "first");
        assertEquals("first", ttlCache.get(1), "Value should be present immediately after put");
        // Wait longer than the TTL and give the background purge thread a chance to run.
        TimeUnit.MILLISECONDS.sleep(250);
        // After expiration the cache should behave as if the entry does not exist.
        assertNull(ttlCache.get(1), "Value must be null after its TTL has expired");
        assertFalse(ttlCache.containsKey(1), "containsKey should be false after expiration");
    }

    @Test
    void testReinsertEvictedKeyAfterLRU() {
        // Max size = 1 to force eviction.
        ttlCache = new TTLCache<>(10_000L, 1);
        // cache holds 1
        ttlCache.put(1, "A");
        // evicts key 1
        ttlCache.put(2, "B");
        assertNull(ttlCache.get(1), "Key 1 should have been evicted");
        assertEquals("B", ttlCache.get(2));
        // Re‑insert the previously evicted key.
        String previous = ttlCache.put(1, "A2");
        assertNull(previous, "Re‑inserting an evicted key should behave like a fresh insertion");
        assertEquals("A2", ttlCache.get(1));
        assertEquals("A2", ttlCache.get(1));
        // Now the cache still respects maxSize = 1, so key 2 must have been evicted.
        assertNull(ttlCache.get(2), "Key 2 should have been evicted after re‑inserting key 1");
    }

    @Test
    void testPutNullKeyThrowsException() {
        assertThrows(NullPointerException.class, () -> ttlCache.put(null, "someValue"), "Putting a null key must throw NullPointerException");
    }

    @Test
    void testPutNullValueThrowsException() {
        assertThrows(NullPointerException.class, () -> ttlCache.put(42, null), "Putting a null value must throw NullPointerException");
    }

    @Test
    void testPutNullKeyAndValueThrowsException() {
        assertThrows(NullPointerException.class, () -> ttlCache.put(null, null), "Putting null key and value must throw NullPointerException");
    }

    @Test
    void testPutWhenInternalCacheMapIsNullThrowsException() throws Exception {
        // Use reflection to set the private 'cacheMap' field to null.
        Field cacheMapField = TTLCache.class.getDeclaredField("cacheMap");
        cacheMapField.setAccessible(true);
        cacheMapField.set(ttlCache, null);
        // The method should now throw a NullPointerException when it tries to use cacheMap.
        assertThrows(NullPointerException.class, () -> ttlCache.put(1, "value"), "If internal cacheMap is null, put should throw NullPointerException");
    }
}
