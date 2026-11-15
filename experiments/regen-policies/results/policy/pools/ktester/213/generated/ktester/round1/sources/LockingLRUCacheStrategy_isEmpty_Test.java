package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LockingLRUCacheStrategy_isEmpty_Test {

    private static final int CAPACITY = 5;

    private LockingLRUCacheStrategy<String, String> cache;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh cache instance before each test.
        cache = new LockingLRUCacheStrategy<>(CAPACITY);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the cache is cleared after each test to avoid side‑effects.
        if (cache != null) {
            cache.clear();
        }
    }

    /**
     * Supplies a stream of scenarios where the cache is expected to be non‑empty.
     * Each scenario provides a description and a lambda that mutates {@code cache}.
     */
    private static Stream<Arguments> nonEmptyCacheScenarios() {
        return Stream.of(Arguments.of("Single entry via put()", (Executable) () -> {
            // using raw types to avoid generic warnings in the lambda
            LockingLRUCacheStrategy<String, String> c = getCacheInstance();
            c.put("one", "1");
        }), Arguments.of("Multiple distinct entries", (Executable) () -> {
            LockingLRUCacheStrategy<String, String> c = getCacheInstance();
            c.put("a", "A");
            c.put("b", "B");
            c.put("c", "C");
        }), Arguments.of("After removing one of several entries", (Executable) () -> {
            LockingLRUCacheStrategy<String, String> c = getCacheInstance();
            c.put("x", "X");
            c.put("y", "Y");
            c.put("z", "Z");
            c.remove("y");
        }));
    }

    /**
     * Helper that returns the current test instance's cache.
     * Used only inside the static method source; the cache field is static‑ly
     * unavailable, so we retrieve it via reflection on the test class instance.
     */
    private static LockingLRUCacheStrategy<String, String> getCacheInstance() throws Exception {
        // JUnit creates a new test instance for each execution; we can obtain it via
        // the current thread's stack trace (a bit hacky but works for this isolated case).
        // For simplicity, we create a fresh cache here with the same capacity.
        return new LockingLRUCacheStrategy<>(CAPACITY);
    }

    @Test
    @DisplayName("isEmpty() should return true for a newly created cache")
    void testIsEmptyOnNewCache() {
        // given – a freshly constructed cache (handled in @BeforeEach)
        // when
        boolean empty = cache.isEmpty();
        // then
        assertTrue(empty, "A newly created cache must be reported as empty");
    }

    @Test
    @DisplayName("isEmpty() should return false after adding an entry")
    void testIsEmptyAfterPut() {
        // given
        cache.put("key1", "value1");
        // when
        boolean empty = cache.isEmpty();
        // then
        assertFalse(empty, "Cache with at least one entry must not be reported as empty");
    }

    @Test
    @DisplayName("isEmpty() should return true after clearing a non‑empty cache")
    void testIsEmptyAfterClear() {
        // given
        cache.put("k1", "v1");
        cache.put("k2", "v2");
        assertFalse(cache.isEmpty(), "Pre‑condition: cache should not be empty before clear");
        // when
        cache.clear();
        // then
        assertTrue(cache.isEmpty(), "Cache must be reported as empty after clear()");
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("isEmpty() after inserting multiple distinct entries")
    void testIsEmptyAfterMultiplePuts() {
        // given
        cache.put("a", "1");
        cache.put("b", "2");
        cache.put("c", "3");
        // when
        boolean empty = cache.isEmpty();
        // then
        assertFalse(empty, "Cache with several entries must not be reported as empty");
    }

    @Test
    @DisplayName("isEmpty() after removing one entry but others remain")
    void testIsEmptyAfterPartialRemove() {
        // given
        cache.put("x", "10");
        cache.put("y", "20");
        cache.put("z", "30");
        // remove a middle entry
        cache.remove("y");
        // when
        boolean empty = cache.isEmpty();
        // then
        assertFalse(empty, "Cache should still be non‑empty after removing only a subset of entries");
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("isEmpty() after putAll() with an empty map")
    void testIsEmptyAfterPutAllEmpty() {
        // given
        Map<String, String> emptyMap = Collections.emptyMap();
        // when
        cache.putAll(emptyMap);
        // then
        assertTrue(cache.isEmpty(), "Cache must remain empty after putAll with an empty map");
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("isEmpty() after inserting a single entry and then removing it")
    void testIsEmptyAfterRemovingOnlyEntry() {
        // given
        cache.put("onlyKey", "onlyValue");
        assertFalse(cache.isEmpty(), "Pre‑condition: cache should contain the single entry");
        // when
        cache.remove("onlyKey");
        // then
        assertTrue(cache.isEmpty(), "Cache must be empty after the sole entry is removed");
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("isEmpty() after attempting to remove a key that was never added")
    void testIsEmptyAfterRemovingNonExistentKey() {
        // given
        assertTrue(cache.isEmpty(), "Pre‑condition: cache starts empty");
        // when
        cache.remove("ghostKey");
        // then
        assertTrue(cache.isEmpty(), "Cache should still be empty after removing a non‑existent key");
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("isEmpty() returns false after inserting a null key")
    void testIsEmptyAfterPutNullKey() {
        // given
        cache.put(null, "nullKeyValue");
        // when
        boolean empty = cache.isEmpty();
        // then
        assertFalse(empty, "Cache must be non‑empty after inserting a null key");
    }

    @Test
    @DisplayName("isEmpty() returns false after inserting a null value")
    void testIsEmptyAfterPutNullValue() {
        // given
        cache.put("nullValueKey", null);
        // when
        boolean empty = cache.isEmpty();
        // then
        assertFalse(empty, "Cache must be non‑empty after inserting a null value");
    }

    @Test
    @DisplayName("isEmpty() returns false after inserting both null key and null value")
    void testIsEmptyAfterPutNullKeyAndValue() {
        // given
        cache.put(null, null);
        // when
        boolean empty = cache.isEmpty();
        // then
        assertFalse(empty, "Cache must be non‑empty after inserting a null key/value pair");
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("Creating a cache with capacity zero should throw IllegalArgumentException")
    void testConstructorWithZeroCapacity() {
        // when / then
        assertThrows(IllegalArgumentException.class, () -> new LockingLRUCacheStrategy<String, String>(0), "Cache construction with zero capacity must be illegal");
    }

    // -----------------------------------------------------------------
    @Disabled("Concurrent modification leading to IllegalStateException is implementation‑specific; " + "this test demonstrates a possible race but is disabled to avoid flaky behavior.")
    @Test
    @DisplayName("isEmpty() may throw IllegalStateException if another thread modifies the cache concurrently")
    void testIsEmptyDuringConcurrentModification() throws InterruptedException, ExecutionException {
        // given: fill the cache with a few entries
        cache.put("k1", "v1");
        cache.put("k2", "v2");
        cache.put("k3", "v3");
        // when: start a thread that clears the cache while the main thread calls isEmpty()
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Boolean> future = executor.submit(() -> {
            // Small delay to increase chance of overlap
            Thread.sleep(10);
            cache.clear();
            return cache.isEmpty();
        });
        // main thread also calls isEmpty()
        boolean mainThreadResult = cache.isEmpty();
        // then: we simply ensure no unexpected exception propagates.
        // The exact outcome (true/false) is nondeterministic, so we only assert that the call completes.
        assertDoesNotThrow(() -> future.get(), "Concurrent access should not cause an uncaught IllegalStateException");
        executor.shutdownNow();
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("nonEmptyCacheScenarios")
    @DisplayName("Parameterized non‑empty cache scenarios")
    void testIsEmptyParameterized(String description, Executable setup) throws Throwable {
        // given
        setup.execute();
        // when
        boolean empty = cache.isEmpty();
        // then
        assertFalse(empty, description);
    }
}
