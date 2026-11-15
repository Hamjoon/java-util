package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

class LockingLRUCacheStrategy_isEmpty_Test {

    private static final int CAPACITY = 5;

    private LockingLRUCacheStrategy<String, Integer> cache;

    class BadCache extends LockingLRUCacheStrategy<String, Integer> {

        BadCache(int capacity) {
            super(capacity);
        }

        @Override
        public int size() {
            throw new UnsupportedOperationException("size not supported");
        }
    }

    class DisposableCache extends LockingLRUCacheStrategy<String, Integer> {

        private boolean disposed = false;

        DisposableCache(int capacity) {
            super(capacity);
        }

        void dispose() {
            disposed = true;
        }

        @Override
        public int size() {
            if (disposed) {
                throw new IllegalStateException("Cache has been disposed");
            }
            return super.size();
        }
    }

    class BadCache extends LockingLRUCacheStrategy<String, Integer> {

        BadCache(int capacity) {
            super(capacity);
        }

        @Override
        public int size() {
            throw new UnsupportedOperationException("size not supported");
        }
    }

    class DisposableCache extends LockingLRUCacheStrategy<String, Integer> {

        private boolean disposed = false;

        DisposableCache(int capacity) {
            super(capacity);
        }

        void dispose() {
            disposed = true;
        }

        @Override
        public int size() {
            if (disposed) {
                throw new IllegalStateException("Cache has been disposed");
            }
            return super.size();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh cache instance before each test
        cache = new LockingLRUCacheStrategy<>(CAPACITY);
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the cache and release reference
        if (cache != null) {
            cache.clear();
            cache = null;
        }
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> provideMapsForPutAll() {
        Map<String, Integer> empty = Collections.emptyMap();
        Map<String, Integer> nonEmpty = new LinkedHashMap<>();
        nonEmpty.put("k1", 1);
        nonEmpty.put("k2", 2);
        return Stream.of(org.junit.jupiter.params.provider.Arguments.of(empty, true), org.junit.jupiter.params.provider.Arguments.of(nonEmpty, false));
    }

    @Test
    void isEmpty_ShouldReturnTrue_ForNewCache() {
        // given: a newly created cache (handled in @BeforeEach)
        // when
        boolean empty = cache.isEmpty();
        // then
        assertTrue(empty, "A newly created cache must be empty");
        assertEquals(0, cache.size(), "Size of a newly created cache must be 0");
    }

    @Test
    void isEmpty_ShouldReturnFalse_AfterPuttingAnEntry() {
        // given
        cache.put("key1", 100);
        // when
        boolean empty = cache.isEmpty();
        // then
        assertFalse(empty, "Cache should not be empty after adding an entry");
        assertEquals(1, cache.size(), "Size should reflect the added entry");
    }

    @Test
    void isEmpty_ShouldReturnTrue_AfterClearingCache() {
        // given
        cache.put("keyA", 1);
        cache.put("keyB", 2);
        assertFalse(cache.isEmpty(), "Pre‑condition: cache must not be empty after puts");
        // when
        cache.clear();
        boolean empty = cache.isEmpty();
        // then
        assertTrue(empty, "Cache should be empty after clear()");
        assertEquals(0, cache.size(), "Size must be 0 after clear()");
    }

    @Test
    void isEmpty_ShouldHandleNullKeyAndValue() {
        // given: cache supports null keys/values
        cache.put(null, null);
        assertFalse(cache.isEmpty(), "Cache should not be empty after putting null key/value");
        // when
        cache.remove(null);
        boolean empty = cache.isEmpty();
        // then
        assertTrue(empty, "Cache should be empty after removing the null entry");
    }

    @Test
    void isEmpty_ShouldReturnFalse_AfterMultipleDistinctEntries() {
        cache.put("a", 1);
        cache.put("b", 2);
        cache.put("c", 3);
        assertFalse(cache.isEmpty(), "Cache with several entries must not be empty");
        assertEquals(3, cache.size());
    }

    @Test
    void isEmpty_ShouldReturnTrue_AfterRemovingAllEntriesIndividually() {
        cache.put("x", 10);
        cache.put("y", 20);
        cache.put("z", 30);
        cache.remove("x");
        cache.remove("y");
        cache.remove("z");
        assertTrue(cache.isEmpty(), "Cache should be empty after removing every entry");
        assertEquals(0, cache.size());
    }

    @Test
    void isEmpty_ShouldReturnFalse_WhenNullKeyWithNonNullValue() {
        cache.put(null, 42);
        assertFalse(cache.isEmpty(), "Null key with a value still counts as an entry");
        assertEquals(1, cache.size());
    }

    @Test
    void isEmpty_ShouldReturnFalse_WhenNullValueWithNonNullKey() {
        cache.put("notNullKey", null);
        assertFalse(cache.isEmpty(), "Non‑null key with null value still counts as an entry");
        assertEquals(1, cache.size());
    }

    @Test
    void isEmpty_ShouldReturnFalse_AfterDuplicateKeyInsertion() {
        cache.put("dup", 1);
        // replacement, size stays 1
        cache.put("dup", 2);
        assertFalse(cache.isEmpty(), "Cache should still be non‑empty after duplicate key insertion");
        assertEquals(1, cache.size(), "Size must remain 1 after overwriting an existing key");
    }

    @Test
    void isEmpty_ShouldReturnFalse_AfterRemovingNonExistentKey() {
        cache.put("present", 1);
        // no effect
        cache.remove("absent");
        assertFalse(cache.isEmpty(), "Removing a missing key must not change non‑empty state");
        assertEquals(1, cache.size());
    }

    @ParameterizedTest(name = "{index}: putAll with {0} entries")
    @MethodSource("provideMapsForPutAll")
    void isEmpty_ShouldReflectPutAllResult(Map<String, Integer> sourceMap, boolean expectedEmpty) {
        cache.putAll(sourceMap);
        assertEquals(expectedEmpty, cache.isEmpty(), "isEmpty() after putAll should match expected emptiness");
        assertEquals(sourceMap.size(), cache.size(), "size() after putAll must equal the source map size");
    }

    @Test
    void isEmpty_ShouldRemainFalse_WhenCacheEvictsDueToCapacity() {
        // Fill to capacity
        for (int i = 0; i < CAPACITY; i++) {
            cache.put("k" + i, i);
        }
        assertEquals(CAPACITY, cache.size());
        // Insert one more entry – LRU eviction should occur but cache stays non‑empty
        cache.put("extra", 999);
        assertFalse(cache.isEmpty(), "Cache must still be non‑empty after eviction");
        assertEquals(CAPACITY, cache.size(), "Size stays at capacity after eviction");
    }

    @Test
    void isEmpty_ShouldReturnTrue_AfterConcurrentPutsAndClear() throws Exception {
        int threadCount = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        // Each thread will put a few entries then wait for the clear signal
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    // wait for the go‑signal
                    startLatch.await();
                    for (int i = 0; i < 3; i++) {
                        cache.put("t" + threadId + "_k" + i, i);
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        // Start all put threads
        startLatch.countDown();
        // Wait a moment to let them populate the cache
        Thread.sleep(100);
        // Clear while puts may still be happening
        cache.clear();
        // Wait for all threads to finish
        doneLatch.await();
        executor.shutdownNow();
        assertTrue(cache.isEmpty(), "Cache must be empty after concurrent puts followed by clear()");
        assertEquals(0, cache.size());
    }

    @Test
    void isEmpty_ShouldPropagateUnsupportedOperationException_FromSize() {
        // Subclass that deliberately throws from size()
        class BadCache extends LockingLRUCacheStrategy<String, Integer> {

            BadCache(int capacity) {
                super(capacity);
            }

            @Override
            public int size() {
                throw new UnsupportedOperationException("size not supported");
            }
        }
        LockingLRUCacheStrategy<String, Integer> badCache = new BadCache(CAPACITY);
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, badCache::isEmpty, "isEmpty() must propagate UnsupportedOperationException from size()");
        assertEquals("size not supported", ex.getMessage());
    }

    @Test
    void isEmpty_ShouldThrowNullPointerException_WhenInternalMapIsNull() throws Exception {
        // Use reflection to locate the backing map field (most likely a LinkedHashMap)
        Field mapField = null;
        for (Field f : cache.getClass().getDeclaredFields()) {
            if (Map.class.isAssignableFrom(f.getType())) {
                mapField = f;
                break;
            }
        }
        assertNotNull(mapField, "Unable to locate internal map field via reflection");
        mapField.setAccessible(true);
        // corrupt the internal state
        mapField.set(cache, null);
        NullPointerException npe = assertThrows(NullPointerException.class, cache::isEmpty, "Calling isEmpty() with a null internal map should throw NPE");
        // The message is implementation‑dependent; we only assert the type.
    }

    @Test
    void isEmpty_ShouldThrowIllegalStateException_AfterDispose() {
        // Subclass that adds a dispose flag and throws from size() when disposed
        class DisposableCache extends LockingLRUCacheStrategy<String, Integer> {

            private boolean disposed = false;

            DisposableCache(int capacity) {
                super(capacity);
            }

            void dispose() {
                disposed = true;
            }

            @Override
            public int size() {
                if (disposed) {
                    throw new IllegalStateException("Cache has been disposed");
                }
                return super.size();
            }
        }
        DisposableCache disposable = new DisposableCache(CAPACITY);
        disposable.put("a", 1);
        disposable.dispose();
        IllegalStateException ise = assertThrows(IllegalStateException.class, disposable::isEmpty, "isEmpty() must propagate IllegalStateException after cache disposal");
        assertEquals("Cache has been disposed", ise.getMessage());
    }
}
