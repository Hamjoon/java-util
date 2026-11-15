package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

class LockingLRUCacheStrategy_remove_Test {

    private static final int CAPACITY = 5;

    private LockingLRUCacheStrategy<String, Integer> cache;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh cache for each test.
        cache = new LockingLRUCacheStrategy<>(CAPACITY);
        // Populate with a few entries that will be used by the tests.
        cache.put("one", 1);
        cache.put("two", 2);
        cache.put("three", 3);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the cache to avoid side‑effects between tests.
        cache.clear();
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> removeExistingKeyProvider() {
        return Stream.of(org.junit.jupiter.params.provider.Arguments.of("alpha", 100), org.junit.jupiter.params.provider.Arguments.of("beta", 200), org.junit.jupiter.params.provider.Arguments.of("gamma", 300));
    }

    private static Stream<String> removeNonExistingKeyProvider() {
        return Stream.of("beta", "delta", "epsilon");
    }

    @Test
    void removeExistingKey_returnsValueAndRemovesEntry() {
        // Arrange – ensure the key is in the cache with the expected value.
        cache.put(key, expected);
        int sizeBefore = cache.size();
        // Act
        Integer removed = cache.remove(key);
        // Assert
        assertEquals(expected, removed, "remove should return the associated value");
        assertNull(cache.get(key), "removed key should no longer be present");
        assertEquals(sizeBefore - 1, cache.size(), "size should decrease after removal");
    }

    @Test
    void removeNonExistingKey_returnsNullAndLeavesCacheUnchanged() {
        // given
        assertEquals(3, cache.size(), "pre‑condition: cache should contain three entries");
        // when
        Integer removed = cache.remove("nonexistent");
        // then
        assertNull(removed, "remove should return null for a missing key");
        assertEquals(3, cache.size(), "size should remain unchanged when key is absent");
    }

    @Test
    void removeNullKey_handlesNullCorrectly() {
        // given
        cache.put(null, 99);
        assertEquals(4, cache.size(), "cache should contain the null key entry");
        // when
        Integer removed = cache.remove(null);
        // then
        assertEquals(99, removed, "remove should return the value associated with the null key");
        assertNull(cache.get(null), "null key should no longer be present after removal");
        assertEquals(3, cache.size(), "size should reflect the removal of the null key");
    }

    /**
     * According to the supplied JSON test case, invoking {@code remove(null)}
     * must throw a {@link NullPointerException}.  The implementation of
     * {@code ConcurrentHashMapNullSafe} used internally does not allow {@code null}
     * keys, which leads to this behaviour.
     */
    @Test
    void removeNullKey_throwsNullPointerException() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> cache.remove(null), "remove(null) should throw NullPointerException");
    }
}
