package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LRUCache_remove_Test {

    private LRUCache<Integer, String> cache;

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the cache to avoid cross‑test contamination
        if (cache != null) {
            cache.clear();
        }
    }

    /* --------------------------------------------------------------------- */
    static Collection<LRUCache.StrategyType> strategies() {
        return Arrays.asList(LRUCache.StrategyType.LOCKING, LRUCache.StrategyType.THREADED);
    }

    /* --------------------------------------------------------------------- */
    private void createCache(LRUCache.StrategyType strategy) {
        // Capacity of 3 is sufficient for the scenarios exercised below
        cache = new LRUCache<>(3, strategy);
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void removeExistingKeyReturnsValueAndUpdatesCache(LRUCache.StrategyType strategy) {
        // given
        createCache(strategy);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
        // when
        String removed = cache.remove(2);
        // then
        assertEquals("two", removed, "remove should return the previous value");
        assertFalse(cache.containsKey(2), "Key 2 should no longer be present");
        assertEquals(2, cache.size(), "Size should decrease after removal");
        // other entries must stay untouched
        assertEquals("one", cache.get(1));
        assertEquals("three", cache.get(3));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void removeNonExistingKeyReturnsNullAndLeavesCacheUnchanged(LRUCache.StrategyType strategy) {
        // given
        createCache(strategy);
        cache.put(10, "ten");
        cache.put(20, "twenty");
        // when
        // 30 was never added
        String removed = cache.remove(30);
        // then
        assertNull(removed, "remove should return null for a missing key");
        assertEquals(2, cache.size(), "Size must remain unchanged");
        assertTrue(cache.containsKey(10));
        assertTrue(cache.containsKey(20));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void removeNullKeyWorksWhenNullKeyIsPresent(LRUCache.StrategyType strategy) {
        // given
        createCache(strategy);
        cache.put(null, "nullValue");
        cache.put(1, "one");
        // when
        String removed = cache.remove(null);
        // then
        assertEquals("nullValue", removed, "remove should return the value associated with null key");
        assertFalse(cache.containsKey(null), "Null key should be removed");
        assertEquals(1, cache.size(), "Only the non‑null entry should remain");
        assertEquals("one", cache.get(1));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void removeNullKeyReturnsNullWhenNullKeyAbsent(LRUCache.StrategyType strategy) {
        // given
        createCache(strategy);
        cache.put(5, "five");
        // when
        String removed = cache.remove(null);
        // then
        assertNull(removed, "remove should return null when null key is absent");
        assertEquals(1, cache.size(), "Cache size must stay the same");
        assertTrue(cache.containsKey(5));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void removeAlphaKeyReturnsFirstAndEntryDisappears(LRUCache.StrategyType strategy) {
        // basic functionality – key "alpha" present
        createCache(strategy);
        // use integer key to match cache generic type
        cache.put(100, "first");
        // map the integer 100 to a logical "alpha" for the test description
        // (the JSON uses string keys, but our cache is <Integer, String>)
        // We'll store the mapping in a separate map for clarity.
        // For the purpose of this test we treat 100 as "alpha".
        cache.put(101, "second");
        // when
        // "alpha"
        String removed = cache.remove(100);
        // then
        assertEquals("first", removed, "Removing existing key should return its value");
        assertFalse(cache.containsKey(100), "Key should be removed");
        assertEquals(1, cache.size(), "Cache size should decrease");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void removeBetaKeyReturnsNullAndCacheUnchanged(LRUCache.StrategyType strategy) {
        // basic functionality – key "beta" does not exist
        createCache(strategy);
        cache.put(200, "value200");
        // when
        // "beta" (non‑existent)
        String removed = cache.remove(201);
        // then
        assertNull(removed, "Removing a non‑existing key must return null");
        assertEquals(1, cache.size(), "Cache size must remain unchanged");
        assertTrue(cache.containsKey(200));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void removeNullKeyWhenAbsentReturnsNull(LRUCache.StrategyType strategy) {
        // basic functionality – null key absent
        createCache(strategy);
        cache.put(300, "threeHundred");
        // when
        String removed = cache.remove(null);
        // then
        assertNull(removed, "Removing null when absent should return null");
        assertEquals(1, cache.size());
        assertTrue(cache.containsKey(300));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void removeKeyWithExplicitNullValueReturnsNullAndRemovesEntry(LRUCache.StrategyType strategy) {
        // basic functionality – key "gamma" maps to null value
        createCache(strategy);
        // "gamma" -> null
        cache.put(400, null);
        cache.put(401, "notNull");
        // when
        // should return null (previous value)
        String removed = cache.remove(400);
        // then
        assertNull(removed, "Removing a key whose value is null should return null");
        assertFalse(cache.containsKey(400), "Key must be removed even though its value was null");
        assertEquals(1, cache.size());
        assertEquals("notNull", cache.get(401));
    }

    @Test
    void removalAfterEvictionReturnsNullForEvictedKeyAndValueForExistingKey() {
        // capacity overflow scenario – use LOCKING strategy (deterministic eviction)
        cache = new LRUCache<>(2, LRUCache.StrategyType.LOCKING);
        // key "a"
        cache.put(1, "a");
        // key "b"
        cache.put(2, "b");
        // access "a" to make it most‑recently used
        cache.get(1);
        // inserting third entry triggers eviction of least‑recently used ("b")
        cache.put(3, "c");
        // "b" should be evicted
        assertFalse(cache.containsKey(2), "Key 2 (\"b\") should have been evicted");
        // attempt to remove evicted key
        String removedEvicted = cache.remove(2);
        assertNull(removedEvicted, "Removing an evicted key must return null");
        // remove still‑present key "a"
        String removedA = cache.remove(1);
        assertEquals("a", removedA, "Removing existing key should return its value");
        assertFalse(cache.containsKey(1));
        assertEquals(1, cache.size(), "Cache size should reflect the removal");
    }

    @Test
    void removeNullKeyThrowsNullPointerExceptionWhenStrategyDisallowsNull() throws Exception {
        // Create a cache with a mocked strategy that throws NPE on remove(null)
        cache = new LRUCache<>(3, LRUCache.StrategyType.LOCKING);
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockStrategy = mock(Map.class);
        when(mockStrategy.remove(null)).thenThrow(new NullPointerException("null keys not allowed"));
        // inject the mock via reflection
        Field f = LRUCache.class.getDeclaredField("strategy");
        f.setAccessible(true);
        f.set(cache, mockStrategy);
        // exercise
        assertThrows(NullPointerException.class, () -> cache.remove(null), "Cache should propagate NullPointerException from the backing strategy");
    }

    @Test
    void removeWithIncompatibleKeyTypeThrowsClassCastException() {
        // LRUCache<Integer, String> but we pass a String key
        cache = new LRUCache<>(3, LRUCache.StrategyType.LOCKING);
        cache.put(1, "one");
        // The underlying map (HashMap) will throw ClassCastException when a wrong type is used
        assertThrows(ClassCastException.class, () -> cache.remove("stringKey"), "Removing with an incompatible key type must propagate ClassCastException");
    }

    @Test
    void removeFromUnmodifiableBackingMapThrowsUnsupportedOperationException() throws Exception {
        // Build a cache whose strategy is an unmodifiable map
        cache = new LRUCache<>(3, LRUCache.StrategyType.LOCKING);
        Map<Integer, String> base = new HashMap<>();
        base.put(1, "one");
        Map<Integer, String> unmodifiable = Collections.unmodifiableMap(base);
        // inject via reflection
        Field f = LRUCache.class.getDeclaredField("strategy");
        f.setAccessible(true);
        f.set(cache, unmodifiable);
        // attempt removal – should raise UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, () -> cache.remove(1), "Removing from an unmodifiable backing map must throw UnsupportedOperationException");
    }
}
