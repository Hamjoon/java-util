package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ThreadedLRUCacheStrategy_isEmpty_Test {

    private ThreadedLRUCacheStrategy<String, String> cache;

    /**
     * Create a fresh cache instance before each test.
     * The chosen capacity and cleanup delay are arbitrary but valid.
     */
    @BeforeEach
    void setupBeforeEach() {
        cache = new ThreadedLRUCacheStrategy<>(10, 100);
    }

    /**
     * Ensure the cache is cleared after each test so that tests are isolated.
     */
    @AfterEach
    void teardownAfterEach() {
        if (cache != null) {
            cache.clear();
        }
    }

    /**
     * Shut down the shared scheduler used by all ThreadedLRUCacheStrategy instances.
     */
    @AfterAll
    static void teardownAfterAll() {
        ThreadedLRUCacheStrategy.shutdown();
    }

    /**
     * Supplies a stream of maps for the putAll tests.
     */
    private static Stream<Map<String, String>> putAllMapsProvider() {
        return Stream.of(Collections.emptyMap(), Map.of("k1", "v1"), Map.of("a", "1", "b", "2", "c", "3"));
    }

    @Test
    void isEmpty_returnsTrue_forNewCache() {
        // given: a freshly created cache (handled by @BeforeEach)
        // when
        boolean empty = cache.isEmpty();
        // then
        assertTrue(empty, "A newly created cache should be empty");
    }

    @Test
    void isEmpty_returnsFalse_afterPut() {
        // given
        cache.put("key1", "value1");
        // when
        boolean empty = cache.isEmpty();
        // then
        assertFalse(empty, "Cache should not be empty after a put operation");
    }

    @Test
    void isEmpty_returnsTrue_afterClear() {
        // given
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        assertFalse(cache.isEmpty(), "Pre‑condition: cache should contain entries");
        // when
        cache.clear();
        boolean empty = cache.isEmpty();
        // then
        assertTrue(empty, "Cache should be empty after clear()");
    }

    @Test
    void isEmpty_returnsTrue_whenAllEntriesRemovedViaRemove() {
        // given
        cache.put("k1", "v1");
        cache.put("k2", "v2");
        cache.remove("k1");
        cache.remove("k2");
        // when
        boolean empty = cache.isEmpty();
        // then
        assertTrue(empty, "Cache should be empty after all entries are removed");
    }

    @Test
    void constructor_throwsException_forInvalidCapacity() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new ThreadedLRUCacheStrategy<String, String>(0, 100));
        assertEquals("Capacity must be at least 1.", ex.getMessage());
    }

    @Test
    void constructor_throwsException_forInvalidCleanupDelay() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new ThreadedLRUCacheStrategy<String, String>(10, 5));
        assertEquals("cleanupDelayMillis must be at least 10 milliseconds.", ex.getMessage());
    }

    @Test
    void isEmpty_true_afterPutAllWithEmptyMap() {
        // given: an empty map
        Map<String, String> emptyMap = Collections.emptyMap();
        // when
        cache.putAll(emptyMap);
        boolean empty = cache.isEmpty();
        // then
        assertTrue(empty, "putAll(empty) should leave the cache empty");
    }

    @Test
    void isEmpty_false_afterPutAllWithMultipleEntries() {
        // given: a map with several entries
        Map<String, String> many = new HashMap<>();
        many.put("a", "1");
        many.put("b", "2");
        many.put("c", "3");
        // when
        cache.putAll(many);
        boolean empty = cache.isEmpty();
        // then
        assertFalse(empty, "Cache should be non‑empty after putAll with entries");
    }

    @Test
    void isEmpty_true_afterPutAllThenClear() {
        // given
        Map<String, String> many = Map.of("x", "10", "y", "20");
        cache.putAll(many);
        assertFalse(cache.isEmpty(), "Pre‑condition: cache should contain entries");
        // when
        cache.clear();
        boolean empty = cache.isEmpty();
        // then
        assertTrue(empty, "Cache should be empty after clear following putAll");
    }

    @Test
    void isEmpty_false_whenReplacingExistingKey() {
        // given: an entry already present
        cache.put("dupKey", "first");
        assertFalse(cache.isEmpty(), "Pre‑condition: cache not empty");
        // when: replace the value for the same key
        cache.put("dupKey", "second");
        boolean empty = cache.isEmpty();
        // then
        assertFalse(empty, "Replacing a value should not make the cache empty");
        assertEquals("second", cache.get("dupKey"), "Value should be updated to the new one");
    }

    @Test
    void isEmpty_true_afterInsertAndRemoveSingleEntry() {
        // given
        cache.put("single", "value");
        assertFalse(cache.isEmpty(), "Pre‑condition: cache not empty");
        // when
        cache.remove("single");
        boolean empty = cache.isEmpty();
        // then
        assertTrue(empty, "Cache should be empty after removing the only entry");
    }

    @Test
    void isEmpty_false_whenOneOfTwoEntriesRemoved() {
        // given
        cache.put("first", "1");
        cache.put("second", "2");
        assertFalse(cache.isEmpty(), "Pre‑condition: cache not empty");
        // when
        cache.remove("first");
        boolean empty = cache.isEmpty();
        // then
        assertFalse(empty, "Cache should still contain one entry after removing only one of two");
        assertEquals("2", cache.get("second"));
        assertNull(cache.get("first"));
    }

    @Test
    void isEmpty_false_whenRemovingNonExistentKey() {
        // given
        cache.put("present", "yes");
        assertFalse(cache.isEmpty(), "Pre‑condition: cache not empty");
        // when
        // no such key
        cache.remove("absent");
        boolean empty = cache.isEmpty();
        // then
        assertFalse(empty, "Removing a non‑existent key must not affect emptiness");
        assertEquals("yes", cache.get("present"));
    }

    @Test
    void isEmpty_true_whenPutNullRemovesMapping() {
        // given: put a key with a non‑null value
        cache.put("toBeRemoved", "value");
        assertFalse(cache.isEmpty(), "Pre‑condition: cache not empty");
        // when: per Map contract, put(key, null) removes the entry
        cache.put("toBeRemoved", null);
        boolean empty = cache.isEmpty();
        // then
        assertTrue(empty, "Cache should be empty after putting null for the only key");
        assertNull(cache.get("toBeRemoved"));
    }

    @ParameterizedTest(name = "putAll with map size {0} results in isEmpty={1}")
    @MethodSource("putAllMapsProvider")
    void isEmpty_behaviour_withVariousPutAllMaps(Map<String, String> sourceMap) {
        // when
        cache.putAll(sourceMap);
        boolean empty = cache.isEmpty();
        // then
        boolean expected = sourceMap.isEmpty();
        assertEquals(expected, empty, () -> "Cache emptiness should match the source map emptiness");
    }
}
