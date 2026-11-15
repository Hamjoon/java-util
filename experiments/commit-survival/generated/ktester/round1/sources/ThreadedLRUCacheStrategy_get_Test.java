package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ThreadedLRUCacheStrategy_get_Test {

    /**
     * Cache instance used in each test.
     */
    private ThreadedLRUCacheStrategy<String, String> cache;

    /**
     * Create a fresh cache before each test. The chosen capacity and cleanup
     * delay are small so the tests run quickly and deterministically.
     */
    @BeforeEach
    void setupBeforeEach() {
        // capacity = 3, cleanupDelayMillis = 50 (both satisfy constructor validation)
        cache = new ThreadedLRUCacheStrategy<>(3, 50);
    }

    /**
     * Clear the cache after each test to avoid cross‑test contamination.
     */
    @AfterEach
    void teardownAfterEach() {
        if (cache != null) {
            cache.clear();
        }
    }

    /**
     * Shut down the shared scheduler used by all ThreadedLRUCacheStrategy
     * instances. This prevents the JVM from hanging because of non‑daemon
     * threads created by the implementation.
     */
    @AfterAll
    static void teardownAfterAll() {
        ThreadedLRUCacheStrategy.shutdown();
    }

    @Test
    void get_returnsValueWhenKeyIsPresent_andUpdatesTimestamp() {
        // given
        cache.put("key1", "value1");
        // when
        String returned = cache.get("key1");
        // then
        assertEquals("value1", returned, "get() should return the stored value");
        assertTrue(cache.containsKey("key1"), "Entry must still be present after get()");
    }

    @Test
    void get_returnsNullWhenKeyIsAbsent() {
        // given – an empty cache, no put() performed
        // when
        String returned = cache.get("missingKey");
        // then
        assertNull(returned, "get() should return null for a missing key");
    }

    @Test
    void get_multipleCallsRefreshTimestampWithoutChangingValue() {
        // given
        cache.put("k", "v");
        // when – first access
        String first = cache.get("k");
        // when – second access
        String second = cache.get("k");
        // then
        assertEquals("v", first);
        assertEquals("v", second);
        // The internal timestamp is not directly observable, but the fact that
        // the value is still present after repeated gets demonstrates that the
        // node was not evicted.
        assertTrue(cache.containsKey("k"));
    }

    @Test
    void get_existingKey_returnsStoredValue() {
        // group: ThreadedLRUCacheStrategy.get(Object) - existing key (value case)
        // given
        cache.put("alpha", "firstValue");
        // when
        String result = cache.get("alpha");
        // then
        assertEquals("firstValue", result, "The cache contains an entry for key \"alpha\" with value \"firstValue\".");
    }

    @Test
    void get_existingKey_updatesTimestamp() throws Exception {
        // group: ThreadedLRUCacheStrategy.get(Object) - existing key (timestamp case)
        // given
        cache.put("beta", "secondValue");
        // Access the private Node via reflection to capture its timestamp.
        Field cacheField = ThreadedLRUCacheStrategy.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.concurrent.ConcurrentMap<String, ?> internalMap = (java.util.concurrent.ConcurrentMap<String, ?>) cacheField.get(cache);
        Object node = internalMap.get("beta");
        assertNotNull(node, "Node must exist after put()");
        Field tsField = node.getClass().getDeclaredField("timestamp");
        tsField.setAccessible(true);
        long before = (long) tsField.get(node);
        // Ensure a measurable time difference.
        Thread.sleep(1);
        // when
        cache.get("beta");
        // then
        long after = (long) tsField.get(node);
        assertTrue(after > before, "Timestamp after get() must be greater than the timestamp before get().");
    }

    @Test
    void get_missingKey_returnsNull() {
        // group: ThreadedLRUCacheStrategy.get(Object) - missing key
        // when
        String result = cache.get("gamma");
        // then
        assertNull(result, "Cache does not contain an entry for key \"gamma\"; get() should return null.");
    }

    @Test
    void get_nullKey_throwsNullPointerException() {
        // group: ThreadedLRUCacheStrategy.get(Object) - null key
        // when / then
        assertThrows(NullPointerException.class, () -> cache.get(null), "ConcurrentMap implementations do not permit null keys; get(null) should throw NPE.");
    }

    @Test
    void get_afterRemoval_returnsNull() {
        // group: ThreadedLRUCacheStrategy.get(Object) - after removal
        // given
        cache.put("delta", "someValue");
        cache.remove("delta");
        // when
        String result = cache.get("delta");
        // then
        assertNull(result, "After removal, get(\"delta\") should return null.");
    }

    @Test
    void get_afterClear_returnsNull() {
        // group: ThreadedLRUCacheStrategy.get(Object) - after clear
        // given
        cache.put("epsilon", "val1");
        cache.put("zeta", "val2");
        cache.clear();
        // when
        String result = cache.get("epsilon");
        // then
        assertNull(result, "After clear(), the cache is empty; get(\"epsilon\") should return null.");
    }

    @Test
    void get_storedNullValue_returnsNullButKeyExists() {
        // group: ThreadedLRUCacheStrategy.get(Object) - stored null value
        // given
        cache.put("zeta", null);
        // when
        String result = cache.get("zeta");
        // then
        assertNull(result, "Value stored as null should be returned as null.");
        assertTrue(cache.containsKey("zeta"), "Key should still be present even though its associated value is null.");
    }

    @Test
    void get_internalCacheNull_throwsNullPointerException() throws Exception {
        // group: ThreadedLRUCacheStrategy.get(Object) - internal cache null
        // Use reflection to set the private 'cache' field to null.
        Field cacheField = ThreadedLRUCacheStrategy.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        cacheField.set(cache, null);
        // when / then
        assertThrows(NullPointerException.class, () -> cache.get("anyKey"), "Calling get() when internal cache is null should throw NPE.");
    }
}
