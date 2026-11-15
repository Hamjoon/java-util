package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.ConcurrentModificationException;
import java.util.concurrent.ConcurrentMap;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ThreadedLRUCacheStrategy_put_Test {

    /**
     * Cache instance used by each test.
     */
    private ThreadedLRUCacheStrategy<String, String> cache;

    /**
     * Small capacity to make eviction easy to observe.
     */
    private static final int CAPACITY = 2;

    /**
     * Very short cleanup delay so that the background task runs quickly.
     */
    private static final int CLEANUP_DELAY_MILLIS = 20;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh cache for every test to avoid cross‑test interference.
        cache = new ThreadedLRUCacheStrategy<>(CAPACITY, CLEANUP_DELAY_MILLIS);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the cache to release any references that could affect later tests.
        cache.clear();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the shared scheduler used by all ThreadedLRUCacheStrategy instances.
        ThreadedLRUCacheStrategy.shutdown();
    }

    @Test
    void putReturnsNullWhenKeyIsNew() {
        // given
        String key = "alpha";
        String value = "first";
        // when
        String returned = cache.put(key, value);
        // then
        assertNull(returned, "put should return null when the key is not already present");
        assertEquals(1, cache.size(), "Cache size should be 1 after inserting a new entry");
        assertEquals(value, cache.get(key), "The value stored in the cache must match the inserted value");
    }

    @Test
    void putReturnsOldValueWhenKeyAlreadyExists() {
        // given
        String key = "beta";
        String firstValue = "one";
        String secondValue = "two";
        // initial insertion
        cache.put(key, firstValue);
        // when
        String returned = cache.put(key, secondValue);
        // then
        assertEquals(firstValue, returned, "put should return the previous value associated with the key");
        assertEquals(1, cache.size(), "Cache size should remain 1 after replacing an existing entry");
        assertEquals(secondValue, cache.get(key), "The value associated with the key must be updated to the new value");
    }

    @Test
    void putTriggersEvictionWhenCapacityIsExceeded() throws InterruptedException {
        // given
        // capacity is 2; we will insert 3 distinct keys.
        String k1 = "k1", v1 = "v1";
        String k2 = "k2", v2 = "v2";
        String k3 = "k3", v3 = "v3";
        // when: insert first two entries – cache should be at capacity.
        cache.put(k1, v1);
        cache.put(k2, v2);
        // then: size must be exactly the capacity.
        assertEquals(CAPACITY, cache.size(), "Cache should be at its configured capacity");
        // when: insert a third entry, which should cause an eviction.
        cache.put(k3, v3);
        // The eviction is performed by a background task scheduled for immediate cleanup.
        // Wait a short time to let that task run.
        TimeUnit.MILLISECONDS.sleep(CLEANUP_DELAY_MILLIS * 2);
        // then: size must not exceed capacity.
        assertTrue(cache.size() <= CAPACITY, "Cache size must not exceed the configured capacity after eviction");
        // Additionally, at least one of the first two entries must have been removed.
        // Because the strategy is LRU, the least‑recently‑used entry (k1) is expected to be evicted.
        // We cannot guarantee ordering in a multithreaded environment, but we can assert that
        // the cache contains the most recently inserted entry and at most one of the older ones.
        assertNotNull(cache.get(k3), "The most recently inserted entry must still be present");
        int presentOldEntries = 0;
        if (cache.get(k1) != null)
            presentOldEntries++;
        if (cache.get(k2) != null)
            presentOldEntries++;
        assertTrue(presentOldEntries <= 1, "At most one of the earlier entries should remain after eviction");
    }

    @ParameterizedTest(name = "put(null key, \"{1}\") throws NPE")
    @CsvSource({ "'null','value1'", "'null','null'" })
    void putNullKeyThrowsNullPointerException(String ignoredKey, String value) {
        // The CSV source provides a placeholder; we explicitly pass null as the key.
        assertThrows(NullPointerException.class, () -> cache.put(null, value), "Putting a null key should raise NullPointerException");
    }

    @ParameterizedTest(name = "put(\"{0}\", null) throws NPE")
    @CsvSource({ "'someKey','null'", "'null','null'" })
    void putNullValueThrowsNullPointerException(String key, String ignoredValue) {
        // The CSV source provides a placeholder; we explicitly pass null as the value.
        assertThrows(NullPointerException.class, () -> cache.put(key, null), "Putting a null value should raise NullPointerException");
    }

    @Test
    void putConcurrentModificationExceptionIsPropagated() throws Exception {
        // given: replace the internal ConcurrentMap with a mock that throws CME on put()
        ConcurrentMap<String, ThreadedLRUCacheStrategy.Node<String, String>> mockMap = mock(ConcurrentMap.class);
        when(mockMap.put(anyString(), any())).thenThrow(new ConcurrentModificationException("simulated concurrent modification"));
        // Use reflection to inject the mock into the cache instance.
        Field cacheField = ThreadedLRUCacheStrategy.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        cacheField.set(cache, mockMap);
        // when / then: the put operation should propagate the CME.
        assertThrows(ConcurrentModificationException.class, () -> cache.put("concurrentKey", "value"), "A ConcurrentModificationException from the underlying map should be re‑thrown");
    }
}
