package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ThreadedLRUCacheStrategy_remove_Test {

    private static ScheduledExecutorService scheduler;

    private ThreadedLRUCacheStrategy<String, Integer> cache;

    @BeforeAll
    static void setupBeforeAll() {
        // The cache class uses a shared scheduler internally; we create one here
        // to avoid interference with other tests that might run in the same JVM.
        scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    @BeforeEach
    void setupBeforeEach() {
        // capacity 5, cleanup delay 50 ms (arbitrary valid values)
        cache = new ThreadedLRUCacheStrategy<>(5, 50);
        // Populate the cache with entries needed for the various test groups
        // normal entry
        cache.put("alpha", "value1");
        // entry whose value is null
        cache.put("gamma", null);
        // entry used for repeated‑removal test
        cache.put("delta", "value4");
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the cache is cleared between tests
        cache.clear();
    }

    @AfterAll
    static void teardownAfterAll() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }

    private static Stream<Arguments> provideRemoveScenarios() {
        return Stream.of(// existing key
        Arguments.of("alpha", "value1", false, "remove existing key"), // non‑existing key
        Arguments.of("beta", null, false, "remove non‑existing key"), // key of different type (Integer) – should simply return null
        Arguments.of(123, null, false, "remove with different key type returns null"));
    }

    @Test
    void removeExistingKey_returnsValueAndRemovesEntry() {
        // given
        assertTrue(cache.containsKey("two"));
        // when
        Integer removed = cache.remove("two");
        // then
        assertEquals(2, removed);
        assertFalse(cache.containsKey("two"));
        // originally 3 entries, one removed
        assertEquals(2, cache.size());
    }

    @Test
    void removeNonExistingKey_returnsNull() {
        // given
        assertFalse(cache.containsKey("nonexistent"));
        // when
        Integer removed = cache.remove("nonexistent");
        // then
        assertNull(removed);
        // cache size should remain unchanged
        assertEquals(3, cache.size());
    }

    @Test
    void removeNullKey_handlesNullKey() {
        // given
        cache.put(null, 99);
        assertTrue(cache.containsKey(null));
        // when
        Integer removed = cache.remove(null);
        // then
        assertEquals(99, removed);
        assertFalse(cache.containsKey(null));
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("provideRemoveScenarios")
    void removeVariousScenarios(Object key, String expectedValue, boolean expectException, String description) {
        if (expectException) {
            // No scenario in this provider throws; kept for future extension
            fail("Unexpected exception expectation");
        } else {
            // Act
            String removed = cache.remove(key);
            // Assert
            assertEquals(expectedValue, removed, "Returned value does not match expectation for key: " + key);
            // Verify that the entry is no longer present when a value was actually removed
            if (expectedValue != null) {
                assertFalse(cache.containsKey(key), "Key should have been removed from the cache");
            } else {
                // size should stay unchanged when nothing was removed
                assertEquals(3, cache.size(), "Cache size should remain unchanged when key does not exist");
            }
        }
    }

    @Test
    void removeNullKey_propagatesNullPointerException() {
        // The underlying ConcurrentMap throws NPE for a null key; the cache should let it bubble up.
        assertThrows(NullPointerException.class, () -> cache.remove(null), "Removing a null key must throw NullPointerException");
    }

    @Test
    void removeKeyMappedToNullValue_returnsNullAndRemovesEntry() {
        assertTrue(cache.containsKey("gamma"), "Pre‑condition: cache must contain the key \"gamma\"");
        String removed = cache.remove("gamma");
        assertNull(removed, "Removing a key whose value is null should return null");
        assertFalse(cache.containsKey("gamma"), "The key should be removed from the cache");
        assertEquals(2, cache.size(), "Cache size should decrease after removal");
    }

    @Test
    void removeAfterClear_returnsNull() {
        // make the cache empty
        cache.clear();
        assertEquals(0, cache.size(), "Cache must be empty after clear");
        String removed = cache.remove("alpha");
        assertNull(removed, "Removing any key after clear should return null");
    }

    @Test
    void removeRepeatedCalls_behaviourIsIdempotent() {
        // First removal – should return the associated value
        String first = cache.remove("delta");
        assertEquals("value4", first, "First removal must return the stored value");
        // Second removal – entry no longer exists, should return null
        String second = cache.remove("delta");
        assertNull(second, "Second removal of the same key must return null");
        // Cache size must have decreased by exactly one
        assertEquals(2, cache.size(), "Cache size should reflect a single successful removal");
    }

    @Test
    void removeIncompatibleKeyType_propagatesClassCastException() {
        // The cache is declared with String keys; passing an Integer may cause a ClassCastException
        // depending on the internal map implementation. The test asserts that the exception, if any,
        // propagates out of the remove method.
        assertThrows(ClassCastException.class, () -> cache.remove(123), "Removing a key of an incompatible type should propagate ClassCastException");
    }
}
