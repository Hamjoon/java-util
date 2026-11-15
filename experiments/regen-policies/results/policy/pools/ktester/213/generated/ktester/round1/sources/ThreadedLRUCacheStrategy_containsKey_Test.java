package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ThreadedLRUCacheStrategy_containsKey_Test {

    private ThreadedLRUCacheStrategy<String, Integer> cache;

    /* --------------------------------------------------------------------- */
    private static final class HashCodeExceptionKey {

        @Override
        public int hashCode() {
            throw new RuntimeException("hashCode exception");
        }

        @Override
        public boolean equals(Object obj) {
            return false;
        }
    }

    private static final class EqualsExceptionKey {

        @Override
        public int hashCode() {
            // any stable hash
            return 42;
        }

        @Override
        public boolean equals(Object obj) {
            throw new RuntimeException("equals exception");
        }
    }

    /**
     * Creates a fresh cache instance before each test and populates it with a
     * couple of entries so that the {@code containsKey} behaviour can be verified.
     */
    @BeforeEach
    void setupBeforeEach() {
        // capacity = 5, cleanupDelayMillis = 50 (arbitrary valid values)
        cache = new ThreadedLRUCacheStrategy<>(5, 50);
        cache.put("one", 1);
        cache.put("two", 2);
    }

    /**
     * Clears the cache after each test to avoid side‑effects.
     */
    @AfterEach
    void teardownAfterEach() {
        cache.clear();
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> basicKeyProvider() {
        return Stream.of(// present
        org.junit.jupiter.params.provider.Arguments.of("one", true), // not added yet
        org.junit.jupiter.params.provider.Arguments.of("alpha", false), org.junit.jupiter.params.provider.Arguments.of("missing", false), // wrong type
        org.junit.jupiter.params.provider.Arguments.of(123, false));
    }

    @Test
    @DisplayName("containsKey returns true for a key that is present")
    void containsKey_returnsTrueWhenKeyExists() {
        assertTrue(cache.containsKey("one"), "Cache should contain the key \"one\"");
    }

    @Test
    @DisplayName("containsKey returns false for a key that is absent")
    void containsKey_returnsFalseWhenKeyMissing() {
        assertFalse(cache.containsKey("missing"), "Cache should not contain the key \"missing\"");
    }

    @Test
    @DisplayName("containsKey correctly handles a null key")
    void containsKey_handlesNullKey() {
        // The cache supports null keys per the API documentation.
        cache.put(null, 0);
        assertTrue(cache.containsKey(null), "Cache should contain the null key after it has been added");
    }

    @Test
    @DisplayName("containsKey does not modify the cache size")
    void containsKey_doesNotChangeSize() {
        int sizeBefore = cache.size();
        cache.containsKey("one");
        assertEquals(sizeBefore, cache.size(), "Calling containsKey must not alter the cache size");
    }

    @ParameterizedTest(name = "{index}: containsKey({0}) → {1}")
    @MethodSource("basicKeyProvider")
    void containsKey_basicScenarios(Object key, boolean expected) {
        // Arrange – the cache already contains "one" and "two"
        // Act
        boolean actual = cache.containsKey(key);
        // Assert
        assertEquals(expected, actual, () -> "containsKey(" + key + ") should return " + expected);
    }

    /*  Null‑key handling – the underlying map (ConcurrentHashMapNullSafe)
     *  does not allow null keys, therefore a NullPointerException is expected.
     * --------------------------------------------------------------------- */
    @Test
    @DisplayName("containsKey throws NullPointerException for a null key")
    void containsKey_nullKey_throwsNPE() {
        assertThrows(NullPointerException.class, () -> cache.containsKey(null), "Calling containsKey(null) must throw NullPointerException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("containsKey returns false after the key has been removed")
    void containsKey_afterRemoval_isFalse() {
        cache.put("beta", 3);
        assertTrue(cache.containsKey("beta"), "Pre‑condition: key must exist before removal");
        cache.remove("beta");
        assertFalse(cache.containsKey("beta"), "Key should not be present after removal");
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("containsKey returns false for any key after cache.clear()")
    void containsKey_afterClear_isFalse() {
        cache.put("gamma", 7);
        assertTrue(cache.containsKey("gamma"), "Pre‑condition: key must exist before clear");
        cache.clear();
        assertFalse(cache.containsKey("gamma"), "Key must be absent after clear");
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("containsKey reflects entries added via putAll")
    void containsKey_afterPutAll_behaviour() {
        Map<String, Integer> map = new HashMap<>();
        map.put("delta", 4);
        map.put("epsilon", 5);
        cache.putAll(map);
        assertTrue(cache.containsKey("delta"), "Key added via putAll must be present");
        assertFalse(cache.containsKey("zeta"), "Key not added via putAll must be absent");
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("containsKey reflects LRU eviction when capacity is exceeded")
    void containsKey_afterCapacityEviction() {
        // capacity 2 forces eviction after the third put
        ThreadedLRUCacheStrategy<String, Integer> smallCache = new ThreadedLRUCacheStrategy<>(2, 50);
        // becomes LRU
        smallCache.put("oldKey", 1);
        smallCache.put("midKey", 2);
        // triggers eviction of "oldKey"
        smallCache.put("newKey", 3);
        // Give the background cleanup task a moment to run (it is scheduled asynchronously)
        // In real code we would await, but for unit test we can invoke the cleanup directly if exposed.
        // Here we simply assert the expected state.
        assertFalse(smallCache.containsKey("oldKey"), "Oldest entry should have been evicted due to capacity limit");
        assertTrue(smallCache.containsKey("midKey"), "Mid entry should still be present after eviction");
        assertTrue(smallCache.containsKey("newKey"), "Most recent entry must be present");
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("containsKey works with distinct String instances that are equal")
    void containsKey_equalButDistinctStringInstances() {
        cache.put("theta", 10);
        String distinct1 = new String("theta");
        String distinct2 = new StringBuilder("theta").toString();
        assertTrue(cache.containsKey(distinct1), "containsKey must rely on equals(), not reference identity");
        assertTrue(cache.containsKey(distinct2), "containsKey must rely on equals(), not reference identity");
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("containsKey propagates RuntimeException from key.hashCode()")
    void containsKey_keyHashCodeThrowsRuntimeException() {
        Object badKey = new HashCodeExceptionKey();
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> cache.containsKey(badKey), "Exception from hashCode() should be propagated");
        assertEquals("hashCode exception", thrown.getMessage());
    }

    @Test
    @DisplayName("containsKey propagates RuntimeException from key.equals()")
    void containsKey_keyEqualsThrowsRuntimeException() {
        // The map will first compute hashCode (which is normal) and then call equals()
        Object badKey = new EqualsExceptionKey();
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> cache.containsKey(badKey), "Exception from equals() should be propagated");
        assertEquals("equals exception", thrown.getMessage());
    }
}
