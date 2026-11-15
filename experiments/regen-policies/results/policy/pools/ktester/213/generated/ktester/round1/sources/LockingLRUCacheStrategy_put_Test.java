package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

class LockingLRUCacheStrategy_put_Test {

    private static final int CAPACITY = 3;

    private LockingLRUCacheStrategy<String, Integer> cache;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh cache before each test
        cache = new LockingLRUCacheStrategy<>(CAPACITY);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the cache to help GC and ensure isolation
        cache.clear();
    }

    /**
     * Supplies combinations where either the key, the value, or both are {@code null}.
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> nullKeyValueProvider() {
        return Stream.of(// null key, non‑null value
        org.junit.jupiter.params.provider.Arguments.of(null, "someValue"), // non‑null key, null value
        org.junit.jupiter.params.provider.Arguments.of("someKey", null), // both null
        org.junit.jupiter.params.provider.Arguments.of(null, null));
    }

    @Test
    void putNewEntryReturnsNullAndStoresValue() {
        // given
        String key = "a";
        Integer value = 1;
        // when
        Integer returned = cache.put(key, value);
        // then
        assertNull(returned, "Putting a new entry should return null");
        assertEquals(value, cache.get(key), "Cache should contain the newly put value");
        assertEquals(1, cache.size(), "Cache size should be 1 after one insertion");
    }

    @Test
    void putExistingKeyUpdatesValueAndReturnsPrevious() {
        // given
        String key = "A";
        // initial insertion
        cache.put(key, "first");
        // when
        String returned = cache.put(key, "second");
        // then
        assertEquals("first", returned, "put should return the previous value for an existing key");
        assertEquals("second", cache.get(key), "Value must be updated to the new one");
        assertEquals(1, cache.size(), "Cache size must stay unchanged after an update");
    }

    @Test
    void putBeyondCapacityEvictsLeastRecentlyUsed() {
        // Use a smaller capacity to make the test clearer
        LockingLRUCacheStrategy<String, String> smallCache = new LockingLRUCacheStrategy<>(2);
        // Fill to capacity
        // LRU order: A
        smallCache.put("A", "first");
        // LRU order: B, A
        smallCache.put("B", "second");
        // Access "A" to make it most recently used
        assertEquals("first", smallCache.get("A"));
        // LRU order now: A, B
        // Insert third entry, causing eviction of the tail ("B")
        smallCache.put("C", "third");
        // Verify eviction
        assertNull(smallCache.get("B"), "Least recently used entry should be evicted");
        assertEquals("first", smallCache.get("A"));
        assertEquals("third", smallCache.get("C"));
        assertEquals(2, smallCache.size(), "Cache size must stay at the defined capacity");
    }

    @Test
    void putNullKeyAndValueAreHandled() {
        // given
        String nullKey = null;
        Integer nullValue = null;
        // when
        Integer returned = cache.put(nullKey, nullValue);
        // then
        assertNull(returned, "Putting a null key/value should behave like a new entry and return null");
        assertTrue(cache.containsKey(nullKey), "Cache should contain the null key");
        assertNull(cache.get(nullKey), "Value associated with null key should be null");
    }

    @Test
    void putNewEntryReturnsNull() {
        // given
        String key = "A";
        String value = "first";
        // when
        String returned = cache.put(key, value);
        // then
        assertNull(returned, "First insertion should return null");
        assertEquals(value, cache.get(key), "Cache must contain the inserted value");
    }

    @ParameterizedTest(name = "{index}: put(key={0}, value={1}) throws NPE")
    @MethodSource("nullKeyValueProvider")
    void putNullKeyOrValueThrowsNPE(String key, String value) {
        // When a null key or value is supplied the underlying ConcurrentHashMapNullSafe
        // is expected to throw a NullPointerException.
        assertThrows(NullPointerException.class, () -> cache.put(key, value), "put(null, ...) or put(..., null) should propagate NullPointerException");
    }
}
