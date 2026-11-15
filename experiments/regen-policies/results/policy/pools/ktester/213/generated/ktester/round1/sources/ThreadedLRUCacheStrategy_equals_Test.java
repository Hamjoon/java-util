package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;

class ThreadedLRUCacheStrategy_equals_Test {

    private ThreadedLRUCacheStrategy<String, Integer> cache;

    /**
     * Cache that overrides {@code entrySet()} to return {@code null}.
     */
    private static class NullEntrySetCache<K, V> extends ThreadedLRUCacheStrategy<K, V> {

        NullEntrySetCache(int capacity, long cleanupDelay) {
            super(capacity, cleanupDelay);
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            return null;
        }
    }

    /**
     * Cache that overrides {@code entrySet()} to throw an {@link IllegalStateException}.
     */
    private static class ThrowingEntrySetCache<K, V> extends ThreadedLRUCacheStrategy<K, V> {

        ThrowingEntrySetCache(int capacity, long cleanupDelay) {
            super(capacity, cleanupDelay);
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            throw new IllegalStateException("entrySet failure");
        }
    }

    /**
     * Map implementation that throws {@link UnsupportedOperationException}
     * when {@code entrySet()} is invoked.
     */
    private static class ThrowingEntrySetMap<K, V> extends HashMap<K, V> {

        @Override
        public Set<Entry<K, V>> entrySet() {
            throw new UnsupportedOperationException("entrySet not supported");
        }
    }

    /**
     * Creates a fresh cache instance before each test.
     * The capacity and cleanup delay are small values suitable for unit testing.
     */
    @BeforeEach
    void setupBeforeEach() {
        // capacity = 5, cleanupDelayMillis = 20 (both satisfy constructor validation)
        cache = new ThreadedLRUCacheStrategy<>(5, 20);
        // Populate the cache with deterministic data
        cache.put("one", 1);
        cache.put("two", 2);
        cache.put("three", 3);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the cache to avoid side‑effects between tests.
        cache.clear();
    }

    @Test
    void equals_sameInstance_returnsTrue() {
        assertTrue(cache.equals(cache), "Cache should be equal to itself");
    }

    @Test
    void equals_otherMapWithSameEntries_returnsTrue() {
        Map<String, Integer> other = new HashMap<>();
        other.put("one", 1);
        other.put("two", 2);
        other.put("three", 3);
        assertTrue(cache.equals(other), "Cache should be equal to another map with the same entries");
    }

    @Test
    void equals_otherMapWithDifferentEntries_returnsFalse() {
        Map<String, Integer> other = new HashMap<>();
        other.put("one", 1);
        other.put("two", 2);
        // Different value for key "three"
        other.put("three", 99);
        assertFalse(cache.equals(other), "Cache should not be equal when entry values differ");
    }

    @Test
    void equals_otherMapWithMissingEntry_returnsFalse() {
        Map<String, Integer> other = new HashMap<>();
        other.put("one", 1);
        other.put("two", 2);
        // "three" is missing
        assertFalse(cache.equals(other), "Cache should not be equal when the other map is missing entries");
    }

    @Test
    void equals_nonMapObject_returnsFalse() {
        Object notAMap = "I am not a map";
        assertFalse(cache.equals(notAMap), "Cache should not be equal to an object that is not a Map");
    }

    @Test
    void equals_null_returnsFalse() {
        assertFalse(cache.equals(null), "Cache should not be equal to null");
    }

    @Test
    void equals_emptyCacheEqualsEmptyMap_returnsTrue() {
        ThreadedLRUCacheStrategy<String, Integer> emptyCache = new ThreadedLRUCacheStrategy<>(5, 20);
        assertTrue(emptyCache.equals(new HashMap<>()), "Two empty maps should be equal");
    }

    @Test
    void equals_nonEmptyCacheEqualsEmptyMap_returnsFalse() {
        assertFalse(cache.equals(new HashMap<>()), "Non‑empty cache should not equal an empty map");
    }

    @Test
    void equals_sameEntriesDifferentImplementation_returnsTrue() {
        Map<String, Integer> other = new TreeMap<>();
        other.put("one", 1);
        other.put("two", 2);
        other.put("three", 3);
        assertTrue(cache.equals(other), "Equality should be true regardless of Map implementation");
    }

    @Test
    void equals_sameEntriesDifferentOrder_returnsTrue() {
        Map<String, Integer> other = new LinkedHashMap<>();
        // Insertion order differs from the cache's order
        other.put("three", 3);
        other.put("one", 1);
        other.put("two", 2);
        assertTrue(cache.equals(other), "Set equality is order‑independent, so result must be true");
    }

    @Test
    void equals_differentValuesForSameKey_returnsFalse() {
        Map<String, Integer> other = Map.of("one", 1, "two", 2, "three", // different value
        99);
        assertFalse(cache.equals(other), "Different value for an existing key must make maps unequal");
    }

    @Test
    void equals_extraKeyInOtherMap_returnsFalse() {
        Map<String, Integer> other = Map.of("one", 1, "two", 2, "three", 3, "four", // extra entry
        4);
        assertFalse(cache.equals(other), "Additional entry in the other map must cause inequality");
    }

    @Test
    void equals_missingKeyInOtherMap_returnsFalse() {
        Map<String, Integer> other = Map.of("one", 1, "two", // missing "three"
        2);
        assertFalse(cache.equals(other), "Missing entry in the other map must cause inequality");
    }

    @Test
    void equals_nullValueEquality_returnsTrue() {
        ThreadedLRUCacheStrategy<String, Integer> c = new ThreadedLRUCacheStrategy<>(5, 20);
        c.put("a", null);
        c.put("b", 2);
        Map<String, Integer> other = new HashMap<>();
        other.put("a", null);
        other.put("b", 2);
        assertTrue(c.equals(other), "Maps containing null values should compare equal when entries match");
    }

    @Test
    void equals_entrySetNullInCache_throwsNullPointerException() {
        NullEntrySetCache<String, Integer> c = new NullEntrySetCache<>(5, 20);
        Map<String, Integer> other = new HashMap<>();
        assertThrows(NullPointerException.class, () -> c.equals(other), "When entrySet() returns null, equals should throw NPE");
    }

    @Test
    void equals_entrySetThrowsInCache_propagatesIllegalStateException() {
        ThrowingEntrySetCache<String, Integer> c = new ThrowingEntrySetCache<>(5, 20);
        Map<String, Integer> other = new HashMap<>();
        assertThrows(IllegalStateException.class, () -> c.equals(other), "Exception from cache.entrySet() must be propagated");
    }

    @Test
    void equals_otherMapEntrySetThrows_propagatesUnsupportedOperationException() {
        ThreadedLRUCacheStrategy<String, Integer> c = new ThreadedLRUCacheStrategy<>(5, 20);
        ThrowingEntrySetMap<String, Integer> other = new ThrowingEntrySetMap<>();
        assertThrows(UnsupportedOperationException.class, () -> c.equals(other), "Exception from otherMap.entrySet() must be propagated");
    }
}
