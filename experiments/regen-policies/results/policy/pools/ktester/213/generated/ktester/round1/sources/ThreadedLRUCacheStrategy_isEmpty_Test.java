package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

class ThreadedLRUCacheStrategy_isEmpty_Test {

    private ThreadedLRUCacheStrategy<String, Integer> cache;

    /**
     * Create a fresh cache instance before each test.
     * Capacity 5 and a modest cleanup delay are sufficient for the tests.
     */
    @BeforeEach
    void setupBeforeEach() {
        cache = new ThreadedLRUCacheStrategy<>(5, 50);
    }

    /**
     * Ensure the cache is cleared after each test so that scheduled purge tasks
     * do not interfere with subsequent executions.
     */
    @AfterEach
    void teardownAfterEach() {
        cache.clear();
    }

    /**
     * Helper to replace the private final {@code cache} field via reflection.
     */
    private void injectCacheMap(ConcurrentMap<String, ThreadedLRUCacheStrategy.Node<String, Integer>> customMap) throws Exception {
        Field cacheField = ThreadedLRUCacheStrategy.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        // Remove final modifier (works on most JVMs for testing purposes)
        java.lang.reflect.Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(cacheField, cacheField.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
        cacheField.set(cache, customMap);
    }

    private static Stream<Arguments> putAllProvider() {
        return Stream.of(Arguments.of(Collections.emptyMap(), true, "empty map → cache stays empty"), Arguments.of(Map.of("a", 1, "b", 2), false, "non‑empty map → cache becomes non‑empty"));
    }

    @Test
    void isEmpty_onNewCache_returnsTrue() {
        // given – a freshly created cache (handled by @BeforeEach)
        // when
        boolean empty = cache.isEmpty();
        // then
        assertTrue(empty, "A newly created cache should be empty");
    }

    @Test
    void isEmpty_afterPut_returnsFalse() {
        // given
        cache.put("one", 1);
        // when
        boolean empty = cache.isEmpty();
        // then
        assertFalse(empty, "Cache should not be empty after a put operation");
    }

    @Test
    void isEmpty_afterPutAndRemove_returnsTrue() {
        // given
        cache.put("one", 1);
        cache.remove("one");
        // when
        boolean empty = cache.isEmpty();
        // then
        assertTrue(empty, "Cache should be empty after the sole entry is removed");
    }

    @Test
    void isEmpty_afterClear_returnsTrue() {
        // given
        cache.put("one", 1);
        cache.put("two", 2);
        cache.clear();
        // when
        boolean empty = cache.isEmpty();
        // then
        assertTrue(empty, "Cache should be empty after clear()");
    }

    @Test
    void constructor_withInvalidCapacity_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new ThreadedLRUCacheStrategy<String, Integer>(0, 50));
        assertEquals("Capacity must be at least 1.", ex.getMessage());
    }

    @Test
    void constructor_withInvalidCleanupDelay_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new ThreadedLRUCacheStrategy<String, Integer>(5, 5));
        assertEquals("cleanupDelayMillis must be at least 10 milliseconds.", ex.getMessage());
    }

    @Test
    void isEmpty_afterClearOnAlreadyEmptyCache_returnsTrue() {
        // cache is already empty
        // explicit clear on empty cache
        cache.clear();
        assertTrue(cache.isEmpty(), "Calling clear() on an empty cache must keep it empty");
    }

    @Test
    void isEmpty_putAllWithEmptyMap_returnsTrue() {
        Map<String, Integer> emptyMap = Collections.emptyMap();
        cache.putAll(emptyMap);
        assertTrue(cache.isEmpty(), "putAll(emptyMap) must not change emptiness");
    }

    @Test
    void isEmpty_putAllWithNonEmptyMap_returnsFalse() {
        Map<String, Integer> data = new HashMap<>();
        data.put("a", 1);
        data.put("b", 2);
        cache.putAll(data);
        assertFalse(cache.isEmpty(), "putAll(non‑empty map) must make cache non‑empty");
    }

    @Test
    void isEmpty_removeAllEntries_oneByOne_returnsTrue() {
        cache.put("x", 10);
        cache.put("y", 20);
        cache.put("z", 30);
        cache.remove("x");
        cache.remove("y");
        cache.remove("z");
        assertTrue(cache.isEmpty(), "Removing each entry should leave the cache empty");
    }

    @Test
    void isEmpty_removeNonExistingKey_doesNotAffectNonEmptyCache() {
        cache.put("present", 1);
        // no such key
        cache.remove("absent");
        assertFalse(cache.isEmpty(), "Removing a missing key must not make a non‑empty cache empty");
    }

    @Test
    void isEmpty_entrySetClear_clearsUnderlyingMap() {
        cache.put("k1", 100);
        cache.put("k2", 200);
        // entrySet().clear() should delegate to the underlying map
        cache.entrySet().clear();
        assertTrue(cache.isEmpty(), "entrySet().clear() must empty the cache");
    }

    @Test
    void isEmpty_capacityEviction_keepsCacheNonEmpty() {
        // capacity 2, add three distinct entries → LRU eviction of the first
        ThreadedLRUCacheStrategy<String, Integer> smallCache = new ThreadedLRUCacheStrategy<>(2, 50);
        smallCache.put("first", 1);
        smallCache.put("second", 2);
        // triggers eviction of "first"
        smallCache.put("third", 3);
        assertFalse(smallCache.isEmpty(), "After eviction the cache still contains entries");
        assertEquals(2, smallCache.size(), "Cache size must respect capacity after eviction");
        smallCache.clear();
    }

    @Test
    void isEmpty_concurrentPutAndRemove_resultsInEmptyCache() throws Exception {
        // Prepare a set of keys that will be both inserted and removed concurrently
        List<String> keys = Arrays.asList("k1", "k2", "k3", "k4", "k5");
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        ExecutorService exec = Executors.newFixedThreadPool(2);
        // Thread 1 – puts all keys
        exec.submit(() -> {
            try {
                startLatch.await();
                for (String k : keys) {
                    cache.put(k, k.length());
                }
            } catch (InterruptedException ignored) {
            } finally {
                doneLatch.countDown();
            }
        });
        // Thread 2 – removes all keys
        exec.submit(() -> {
            try {
                startLatch.await();
                for (String k : keys) {
                    cache.remove(k);
                }
            } catch (InterruptedException ignored) {
            } finally {
                doneLatch.countDown();
            }
        });
        // Start both threads simultaneously
        startLatch.countDown();
        // Wait for both to finish
        doneLatch.await();
        exec.shutdownNow();
        assertTrue(cache.isEmpty(), "After concurrent put/remove of the same keys the cache must be empty");
    }

    @Test
    void isEmpty_whenInternalCacheIsNull_throwsNullPointerException() throws Exception {
        // Simulate a null internal map
        injectCacheMap(null);
        assertThrows(NullPointerException.class, () -> cache.isEmpty(), "Calling isEmpty() when internal cache is null must throw NPE");
    }

    @Test
    void isEmpty_whenInternalCacheThrowsRuntimeException_propagatesIt() throws Exception {
        // Mock a ConcurrentMap that throws RuntimeException on isEmpty()
        @SuppressWarnings("unchecked")
        ConcurrentMap<String, ThreadedLRUCacheStrategy.Node<String, Integer>> badMap = (ConcurrentMap<String, ThreadedLRUCacheStrategy.Node<String, Integer>>) mock(ConcurrentMap.class);
        when(badMap.isEmpty()).thenThrow(new RuntimeException("boom"));
        injectCacheMap(badMap);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> cache.isEmpty(), "Exception from internal map must be propagated");
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void isEmpty_whenInternalCacheThrowsUnsupportedOperationException_propagatesIt() throws Exception {
        // Mock a ConcurrentMap that throws UnsupportedOperationException on isEmpty()
        @SuppressWarnings("unchecked")
        ConcurrentMap<String, ThreadedLRUCacheStrategy.Node<String, Integer>> badMap = (ConcurrentMap<String, ThreadedLRUCacheStrategy.Node<String, Integer>>) mock(ConcurrentMap.class);
        when(badMap.isEmpty()).thenThrow(new UnsupportedOperationException("not supported"));
        injectCacheMap(badMap);
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> cache.isEmpty(), "UnsupportedOperationException must be propagated");
        assertEquals("not supported", ex.getMessage());
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("putAllProvider")
    void isEmpty_putAll_variousMaps(Map<String, Integer> source, boolean expectEmpty, String description) {
        cache.putAll(source);
        assertEquals(expectEmpty, cache.isEmpty(), "putAll result: " + description);
    }
}
