package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

class ThreadedLRUCacheStrategy_get_Test {

    /**
     * Cache instance used by each test.
     */
    private ThreadedLRUCacheStrategy<Integer, String> cache;

    /**
     * Create a fresh cache before each test.
     */
    @BeforeEach
    void setupBeforeEach() {
        // Capacity 10 and a cleanup delay that is long enough to avoid interference
        // with the simple get/put scenarios exercised here.
        cache = new ThreadedLRUCacheStrategy<>(10, 1000);
    }

    /**
     * Clear the cache after each test to avoid cross‑test contamination.
     */
    @AfterEach
    void teardownAfterEach() {
        cache.clear();
    }

    /**
     * Verify that {@code get} returns the value previously {@code put} into the cache.
     */
    @Test
    void get_ReturnsValue_WhenKeyExists() {
        // given
        Integer key = 42;
        String expectedValue = "The answer";
        cache.put(key, expectedValue);
        // when
        String actualValue = cache.get(key);
        // then
        assertEquals(expectedValue, actualValue, "get should return the value associated with an existing key");
    }

    /**
     * Verify that {@code get} returns {@code null} for a key that has never been added.
     */
    @Test
    void get_ReturnsNull_WhenKeyDoesNotExist() {
        // given
        String missingKey = "missingKey";
        // when
        String result = cache.get(missingKey);
        // then
        assertNull(result, "get should return null when the requested key is not present in the cache");
    }

    /**
     * Verify that a subsequent call to {@code get} still returns the value after the first access,
     * confirming that the entry remains in the cache (i.e., the timestamp update does not remove it).
     */
    @Test
    void get_MultipleCalls_ReturnConsistentValue() {
        // given
        Integer key = 7;
        String value = "Lucky";
        cache.put(key, value);
        // when
        String first = cache.get(key);
        String second = cache.get(key);
        // then
        assertEquals(value, first, "First get should return the stored value");
        assertEquals(value, second, "Second get should still return the stored value");
    }

    @Test
    @DisplayName("get returns null for a null key when cache permits null keys")
    void get_ReturnsNull_WhenKeyIsNull() {
        // when
        String result = cache.get(null);
        // then
        assertNull(result, "When a null key is supplied, get should return null (key absent)");
    }

    @Test
    @DisplayName("get propagates NullPointerException if underlying map rejects null key")
    void get_ThrowsNPE_WhenUnderlyingMapRejectsNullKey() throws Exception {
        // Replace the internal cache with a mock that throws NPE on get(null)
        @SuppressWarnings("unchecked")
        ConcurrentMap<String, ThreadedLRUCacheStrategy.Node<String, String>> mockMap = mock(ConcurrentMap.class);
        when(mockMap.get(null)).thenThrow(new NullPointerException("null key not allowed"));
        // inject mock via reflection
        Field cacheField = ThreadedLRUCacheStrategy.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        cacheField.set(cache, mockMap);
        // act & assert
        Executable call = () -> cache.get(null);
        assertThrows(NullPointerException.class, call, "A NullPointerException should propagate when the underlying map rejects a null key");
    }

    @Test
    @DisplayName("get updates node timestamp when key is present")
    void get_UpdatesTimestamp_WhenKeyExists() throws Exception {
        // given
        String key = "timeKey";
        String value = "timeValue";
        cache.put(key, value);
        // obtain the internal Node via reflection
        Field cacheField = ThreadedLRUCacheStrategy.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentMap<String, ThreadedLRUCacheStrategy.Node<String, String>> internalMap = (ConcurrentMap<String, ThreadedLRUCacheStrategy.Node<String, String>>) cacheField.get(cache);
        ThreadedLRUCacheStrategy.Node<String, String> node = internalMap.get(key);
        assertNotNull(node, "Node must exist after put");
        // capture timestamp before get
        Field tsField = ThreadedLRUCacheStrategy.Node.class.getDeclaredField("timestamp");
        tsField.setAccessible(true);
        long before = (long) tsField.get(node);
        // when
        cache.get(key);
        // capture timestamp after get
        long after = (long) tsField.get(node);
        // then
        assertTrue(after > before, "Timestamp after get must be greater than timestamp before get, confirming updateTimestamp() was invoked");
    }

    @Test
    @DisplayName("get returns null for a key whose stored value is null, but still updates timestamp")
    void get_ReturnsNullAndUpdatesTimestamp_WhenStoredValueIsNull() throws Exception {
        // given
        String key = "nullValueKey";
        // store a null value
        cache.put(key, null);
        // obtain node and timestamp before get
        Field cacheField = ThreadedLRUCacheStrategy.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentMap<String, ThreadedLRUCacheStrategy.Node<String, String>> internalMap = (ConcurrentMap<String, ThreadedLRUCacheStrategy.Node<String, String>>) cacheField.get(cache);
        ThreadedLRUCacheStrategy.Node<String, String> node = internalMap.get(key);
        assertNotNull(node, "Node must exist even if value is null");
        Field tsField = ThreadedLRUCacheStrategy.Node.class.getDeclaredField("timestamp");
        tsField.setAccessible(true);
        long before = (long) tsField.get(node);
        // when
        String result = cache.get(key);
        // then
        assertNull(result, "get should return null when the stored value is null");
        long after = (long) tsField.get(node);
        assertTrue(after > before, "Timestamp must still be updated even when value is null");
    }

    @Test
    @DisplayName("get returns null when key type does not match cache generic type")
    void get_ReturnsNull_WhenKeyTypeMismatched() {
        // given: cache expects String keys, we supply an Integer
        Integer mismatchedKey = 12345;
        // when
        String result = cache.get(mismatchedKey);
        // then
        assertNull(result, "A mismatched key type should result in null (no entry found)");
    }

    @Test
    @DisplayName("node remains in cache after successful get (size unchanged)")
    void get_DoesNotRemoveNode_AndSizeUnchanged() {
        // given
        String key = "persistKey";
        String value = "persistValue";
        cache.put(key, value);
        int sizeBefore = cache.size();
        // when
        String returned = cache.get(key);
        int sizeAfter = cache.size();
        // then
        assertEquals(value, returned, "Returned value must match stored value");
        assertEquals(sizeBefore, sizeAfter, "Cache size must remain unchanged after get");
    }

    @Test
    @DisplayName("concurrent get calls on the same key all return the same value and update timestamps")
    void get_ConcurrentAccess_ReturnsConsistentValue() throws Exception {
        // given
        String key = "concurrentKey";
        String value = "concurrentValue";
        cache.put(key, value);
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<String>> futures = new ArrayList<>();
        // capture timestamp before any get
        Field cacheField = ThreadedLRUCacheStrategy.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentMap<String, ThreadedLRUCacheStrategy.Node<String, String>> internalMap = (ConcurrentMap<String, ThreadedLRUCacheStrategy.Node<String, String>>) cacheField.get(cache);
        ThreadedLRUCacheStrategy.Node<String, String> node = internalMap.get(key);
        Field tsField = ThreadedLRUCacheStrategy.Node.class.getDeclaredField("timestamp");
        tsField.setAccessible(true);
        long before = (long) tsField.get(node);
        // when: launch many threads that wait on the latch then call get()
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                // ensure simultaneous start
                startLatch.await();
                return cache.get(key);
            }));
        }
        // release all threads
        startLatch.countDown();
        // collect results
        for (Future<String> f : futures) {
            assertEquals(value, f.get(2, TimeUnit.SECONDS), "All concurrent get calls must return the stored value");
        }
        // ensure timestamp has been advanced at least once
        long after = (long) tsField.get(node);
        assertTrue(after > before, "Timestamp must be updated by concurrent get calls");
        executor.shutdownNow();
    }

    @Test
    @DisplayName("get throws NullPointerException when internal cache field is null")
    void get_ThrowsNPE_WhenInternalCacheIsNull() throws Exception {
        // set the internal cache field to null via reflection
        Field cacheField = ThreadedLRUCacheStrategy.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        cacheField.set(cache, null);
        // act & assert
        Executable call = () -> cache.get("anyKey");
        assertThrows(NullPointerException.class, call, "Invoking get when internal cache is null should throw NullPointerException");
    }

    @ParameterizedTest(name = "get returns null for absent key ''{0}''")
    @ValueSource(strings = { "absent1", "absent2", "absent3" })
    void get_ReturnsNull_ForVariousAbsentKeys(String missingKey) {
        assertNull(cache.get(missingKey), "get should return null for keys that have never been added");
    }

    @ParameterizedTest(name = "get returns stored value for present key ''{0}''")
    @ValueSource(strings = { "keyA", "keyB", "keyC" })
    void get_ReturnsValue_ForVariousPresentKeys(String key) {
        String value = "value_of_" + key;
        cache.put(key, value);
        assertEquals(value, cache.get(key), "get should return the correct value for an existing key");
    }
}
