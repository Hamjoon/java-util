package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.*;
import java.lang.reflect.*;

class LockingLRUCacheStrategy_isEmpty_Test {

    private static final int CAPACITY = 5;

    private LockingLRUCacheStrategy<String, Integer> cache;

    private static final int DEFAULT_CAPACITY = 5;

    class BadCache<K, V> extends LockingLRUCacheStrategy<K, V> {

        BadCache(int capacity) {
            super(capacity);
        }

        @Override
        public int size() {
            throw new UnsupportedOperationException("size not supported");
        }
    }

    class BadCache<K, V> extends LockingLRUCacheStrategy<K, V> {

        BadCache(int capacity) {
            super(capacity);
        }

        @Override
        public int size() {
            throw new UnsupportedOperationException("size not supported");
        }
    }

    /**
     * Creates a fresh cache instance before each test.
     * This corresponds to the “given” phase of the tests.
     */
    @BeforeEach
    void setupBeforeEach() {
        cache = new LockingLRUCacheStrategy<>(DEFAULT_CAPACITY);
    }

    /**
     * Clears the reference after each test to help the GC.
     */
    @AfterEach
    void teardownAfterEach() {
        cache = null;
    }

    /**
     * Finds the first declared field of the given type in the class hierarchy.
     *
     * @param clazz the class to inspect
     * @param type  the field type to look for
     * @return the {@link Field} or {@code null} if none found
     */
    private static Field findFieldOfType(Class<?> clazz, Class<?> type) {
        Class<?> current = clazz;
        while (current != null) {
            for (Field f : current.getDeclaredFields()) {
                if (type.isAssignableFrom(f.getType())) {
                    return f;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    @Test
    @DisplayName("isEmpty() should return true for a newly created cache")
    void testIsEmptyOnNewCache() {
        // given – cache is freshly created in @BeforeEach
        // when
        boolean result = cache.isEmpty();
        // then
        assertTrue(result, "A newly created cache must be empty");
    }

    @Test
    @DisplayName("isEmpty() should return false after adding an entry")
    void testIsEmptyAfterPut() {
        // given – cache is freshly created
        cache.put("key1", 42);
        // when
        boolean result = cache.isEmpty();
        // then
        assertFalse(result, "Cache should not be empty after a put operation");
    }

    @Test
    @DisplayName("isEmpty() should return true after clearing the cache")
    void testIsEmptyAfterClear() {
        // given – cache with some entries
        cache.put("a", 1);
        cache.put("b", 2);
        cache.clear();
        // when
        boolean result = cache.isEmpty();
        // then
        assertTrue(result, "Cache should be empty after clear()");
    }

    @Test
    @DisplayName("isEmpty() should correctly reflect removal of the last entry")
    void testIsEmptyAfterRemovingLastEntry() {
        // given – cache with a single entry
        cache.put("only", 99);
        cache.remove("only");
        // when
        boolean result = cache.isEmpty();
        // then
        assertTrue(result, "Cache should be empty after removing the only entry");
    }

    @Test
    @DisplayName("isEmpty() should stay true after putAll with an empty map")
    void testIsEmptyAfterPutAllEmptyMap() {
        Map<String, Integer> empty = Collections.emptyMap();
        cache.putAll(empty);
        assertTrue(cache.isEmpty(), "Cache must remain empty after putAll(empty)");
    }

    @Test
    @DisplayName("isEmpty() should be false after putAll with entries")
    void testIsEmptyAfterPutAllWithEntries() {
        Map<String, Integer> data = Map.of("x", 1, "y", 2, "z", 3);
        cache.putAll(data);
        assertFalse(cache.isEmpty(), "Cache must not be empty after putAll with entries");
    }

    @Test
    @DisplayName("isEmpty() should stay false when an existing key is replaced")
    void testIsEmptyAfterReplacingExistingKey() {
        cache.put("dup", 10);
        // replacement, size stays 1
        cache.put("dup", 20);
        assertFalse(cache.isEmpty(), "Cache must not be empty after replacing an existing key");
        assertEquals(1, cache.size(), "Size should remain 1 after replacement");
    }

    @Test
    @DisplayName("isEmpty() should be false when null key/value are inserted")
    void testIsEmptyAfterNullKeyOrValue() {
        cache.put(null, 123);
        assertFalse(cache.isEmpty(), "Cache must not be empty after inserting a null key");
        cache.clear();
        cache.put("k", null);
        assertFalse(cache.isEmpty(), "Cache must not be empty after inserting a null value");
    }

    @Test
    @DisplayName("isEmpty() remains false after LRU eviction (capacity exceeded)")
    void testIsEmptyAfterLruEviction() {
        // small capacity to force eviction
        LockingLRUCacheStrategy<Integer, String> smallCache = new LockingLRUCacheStrategy<>(2);
        smallCache.put(1, "a");
        smallCache.put(2, "b");
        // should evict key 1
        smallCache.put(3, "c");
        assertEquals(2, smallCache.size(), "Size should be equal to capacity after eviction");
        assertFalse(smallCache.isEmpty(), "Cache must not be empty after eviction");
    }

    @Test
    @DisplayName("isEmpty() should be true after concurrent put‑remove cycles")
    void testIsEmptyConcurrentModifications() throws InterruptedException {
        int threadCount = 10;
        ExecutorService exec = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            exec.submit(() -> {
                try {
                    // synchronize start
                    startLatch.await();
                    cache.put("k" + idx, idx);
                    cache.remove("k" + idx);
                } catch (InterruptedException ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        // let all threads run
        startLatch.countDown();
        // wait for completion
        doneLatch.await();
        exec.shutdownNow();
        assertTrue(cache.isEmpty(), "Cache must be empty after all concurrent put‑remove operations");
    }

    @Test
    @DisplayName("isEmpty() propagates NullPointerException when internal map is null")
    void testIsEmptyWhenInternalMapIsNull() throws Exception {
        // Use reflection to set the (presumed) internal map field to null.
        Field mapField = findFieldOfType(cache.getClass(), Map.class);
        assertNotNull(mapField, "Unable to locate internal Map field via reflection");
        mapField.setAccessible(true);
        mapField.set(cache, null);
        assertThrows(NullPointerException.class, () -> cache.isEmpty(), "Calling isEmpty() with a null internal map should throw NPE");
    }

    @Test
    @DisplayName("isEmpty() propagates UnsupportedOperationException from overridden size()")
    void testIsEmptyWhenSizeThrowsUnsupportedOperation() {
        class BadCache<K, V> extends LockingLRUCacheStrategy<K, V> {

            BadCache(int capacity) {
                super(capacity);
            }

            @Override
            public int size() {
                throw new UnsupportedOperationException("size not supported");
            }
        }
        LockingLRUCacheStrategy<String, Integer> badCache = new BadCache<>(DEFAULT_CAPACITY);
        assertThrows(UnsupportedOperationException.class, badCache::isEmpty, "isEmpty() must propagate the exception thrown by size()");
    }

    @Test
    @DisplayName("isEmpty() propagates IllegalStateException when lock is not initialized")
    void testIsEmptyWhenLockIsNull() throws Exception {
        // Find the ReentrantLock (or Lock) field and null it.
        Field lockField = findFieldOfType(cache.getClass(), Lock.class);
        assertNotNull(lockField, "Unable to locate internal Lock field via reflection");
        lockField.setAccessible(true);
        lockField.set(cache, null);
        // The exact exception type depends on the implementation; we expect IllegalStateException.
        assertThrows(IllegalStateException.class, () -> cache.isEmpty(), "Calling isEmpty() with a null lock should throw IllegalStateException");
    }
}
