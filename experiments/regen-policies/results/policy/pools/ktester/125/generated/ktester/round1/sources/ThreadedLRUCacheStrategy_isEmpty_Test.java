package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class ThreadedLRUCacheStrategy_isEmpty_Test {

    private ThreadedLRUCacheStrategy<String, Integer> cache;

    @BeforeEach
    void setupBeforeEach() {
        // Use a modest capacity and a short cleanup delay – the values are not
        // relevant for the isEmpty() behaviour we are testing.
        cache = new ThreadedLRUCacheStrategy<>(10, 100);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure each test starts with a fresh cache instance.
        cache.clear();
    }

    /**
     * The shared scheduler used by all {@link ThreadedLRUCacheStrategy} instances
     * should be shut down once all tests have finished.
     */
    @AfterAll
    static void teardownAfterAll() {
        // Gracefully stop the background purge thread used by the cache implementation.
        ThreadedLRUCacheStrategy.shutdown();
    }

    @Test
    void isEmpty_onNewCache_returnsTrue() {
        // given: a freshly created cache (handled by @BeforeEach)
        // when
        boolean empty = cache.isEmpty();
        // then
        assertTrue(empty, "A newly created cache should be empty");
    }

    @Test
    void isEmpty_afterPut_returnsFalse() {
        // given
        cache.put("key1", 42);
        // when
        boolean empty = cache.isEmpty();
        // then
        assertFalse(empty, "Cache should not be empty after a put operation");
    }

    @Test
    void isEmpty_afterClear_returnsTrue() {
        // given
        cache.put("key1", 42);
        cache.put("key2", 99);
        cache.clear();
        // when
        boolean empty = cache.isEmpty();
        // then
        assertTrue(empty, "Cache should be empty after clear()");
    }

    @Test
    void isEmpty_afterRemoveOnlyEntry_returnsTrue() {
        // given: a cache with a single entry
        cache.put("onlyKey", 1);
        // when: the entry is removed
        cache.remove("onlyKey");
        // then: the cache reports empty
        assertTrue(cache.isEmpty(), "Cache should be empty after removing the only entry");
    }

    @Test
    void isEmpty_whenInternalCacheIsNull_throwsNullPointerException() throws Exception {
        // given: manipulate the private 'cache' field to be null via reflection
        Field cacheField = ThreadedLRUCacheStrategy.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        cacheField.set(cache, null);
        // when / then: calling isEmpty() should throw NPE because the underlying map is null
        assertThrows(NullPointerException.class, () -> cache.isEmpty(), "Calling isEmpty() when internal cache map is null should throw NullPointerException");
    }
}
