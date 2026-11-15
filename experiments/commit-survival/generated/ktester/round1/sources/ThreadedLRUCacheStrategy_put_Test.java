package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ThreadedLRUCacheStrategy_put_Test {

    /**
     * Cache instance used in each test.
     */
    private ThreadedLRUCacheStrategy<Integer, String> cache;

    /**
     * Capacity used for the cache in the tests.
     */
    private static final int CAPACITY = 2;

    /**
     * Small delay to allow the asynchronous cleanup task to run.
     */
    private static final int CLEANUP_DELAY_MILLIS = 50;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh cache for every test to avoid cross‑test interference.
        cache = new ThreadedLRUCacheStrategy<>(CAPACITY, CLEANUP_DELAY_MILLIS);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the cache to release references; the internal scheduler is shared,
        // so we do not shut it down here.
        cache.clear();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the shared scheduler used by all ThreadedLRUCacheStrategy instances.
        ThreadedLRUCacheStrategy.shutdown();
    }

    static Stream<ExistingKeyReplacementData> existingKeyReplacementProvider() {
        return Stream.of(new ExistingKeyReplacementData(2, "A", "old", "new"));
    }

    static Stream<CapacityOverflowData> capacityOverflowProvider() {
        return Stream.of(new CapacityOverflowData(1, "X", "v1", "Y", "v2"));
    }

    static Stream<NullKeyData> nullKeyProvider() {
        return Stream.of(new NullKeyData(2, "someValue"));
    }

    /**
     * Retrieves the {@code timestamp} field of the internal {@code Node} associated with the given key.
     *
     * @param cacheInstance the cache under test
     * @param key           the key whose node timestamp is required
     * @return the timestamp (nanoseconds) stored in the node
     * @throws Exception if reflection fails
     */
    private static long readNodeTimestamp(ThreadedLRUCacheStrategy<?, ?> cacheInstance, Object key) throws Exception {
        // Access the private 'cache' field (ConcurrentMap<K, Node<K,V>>)
        Field cacheField = ThreadedLRUCacheStrategy.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentMap<Object, Object> internalMap = (ConcurrentMap<Object, Object>) cacheField.get(cacheInstance);
        Object node = internalMap.get(key);
        assertNotNull(node, "Node should exist for key: " + key);
        // The inner class is named ThreadedLRUCacheStrategy$Node
        Class<?> nodeClass = node.getClass();
        // Access the 'timestamp' field of the node.
        Field timestampField = nodeClass.getDeclaredField("timestamp");
        timestampField.setAccessible(true);
        return (long) timestampField.get(node);
    }

    @Test
    @DisplayName("put returns null for a new key and stores the entry")
    void testPutNewKeyReturnsNullAndStoresValue() {
        // given
        Integer key = 1;
        String value = "one";
        // when
        String previous = cache.put(key, value);
        // then
        assertNull(previous, "put should return null when the key is new");
        assertEquals(1, cache.size(), "Cache size should be 1 after inserting one entry");
        assertEquals(value, cache.get(key), "The value retrieved with get() must match the inserted value");
    }

    @Test
    @DisplayName("put returns the old value when the key already exists")
    void testPutExistingKeyReturnsOldValue() {
        // given
        Integer key = 2;
        String firstValue = "first";
        String secondValue = "second";
        // initial insertion
        cache.put(key, firstValue);
        // when
        String returned = cache.put(key, secondValue);
        // then
        assertEquals(firstValue, returned, "put should return the previous value for an existing key");
        assertEquals(1, cache.size(), "Cache size must stay unchanged when replacing an existing entry");
        assertEquals(secondValue, cache.get(key), "The new value must replace the old one");
    }

    @Test
    @DisplayName("put triggers cleanup when capacity is exceeded")
    void testPutExceedsCapacityTriggersCleanup() throws InterruptedException {
        // given
        // Insert entries up to capacity
        cache.put(1, "one");
        cache.put(2, "two");
        // when: add a third entry, which puts the cache over the limit
        cache.put(3, "three");
        // The cleanup is scheduled to run asynchronously. Wait a little longer than the
        // configured cleanup delay to give the task a chance to execute.
        TimeUnit.MILLISECONDS.sleep(CLEANUP_DELAY_MILLIS * 3);
        // then
        assertTrue(cache.size() <= CAPACITY, "After cleanup the cache size should not exceed the configured capacity");
        // Verify that the most recently used entry (key 3) is still present.
        assertEquals("three", cache.get(3), "The most recently inserted entry should survive the cleanup");
    }

    @ParameterizedTest(name = "[{index}] capacity={0}, key={1}")
    @MethodSource("existingKeyReplacementProvider")
    @DisplayName("ExistingKeyReplacement – put returns old value and updates timestamp")
    void testExistingKeyReplacement(ExistingKeyReplacementData data) throws Exception {
        // Create a cache with the requested capacity.
        cache = new ThreadedLRUCacheStrategy<>(data.capacity(), CLEANUP_DELAY_MILLIS);
        // Insert the first entry.
        cache.put(data.firstKey(), data.firstValue());
        // Capture the timestamp of the internal node after the first insertion.
        long timestampBefore = readNodeTimestamp(cache, data.firstKey());
        // Small pause to guarantee a measurable timestamp difference.
        TimeUnit.NANOSECONDS.sleep(1_000L);
        // Insert the same key with a new value.
        String returned = cache.put(data.firstKey(), data.secondValue());
        // Capture the timestamp after replacement.
        long timestampAfter = readNodeTimestamp(cache, data.firstKey());
        // Assertions
        assertEquals(data.firstValue(), returned, "put should return the previous value when the key already exists");
        assertTrue(timestampAfter > timestampBefore, "Node timestamp must be updated to a newer value after replacement");
        assertEquals(data.secondValue(), cache.get(data.firstKey()), "The value associated with the key must be the newly inserted one");
        assertEquals(1, cache.size(), "Cache size must remain 1 after replacing an existing entry");
    }

    @ParameterizedTest(name = "[{index}] capacity={0}, firstKey={1}, secondKey={3}")
    @MethodSource("capacityOverflowProvider")
    @DisplayName("CapacityOverflowTriggersCleanup – put returns null and schedules cleanup")
    void testCapacityOverflowTriggersCleanup(CapacityOverflowData data) throws Exception {
        // Cache with the given (small) capacity.
        cache = new ThreadedLRUCacheStrategy<>(data.capacity(), CLEANUP_DELAY_MILLIS);
        // Insert the first entry – fills the cache.
        cache.put(data.firstKey(), data.firstValue());
        // Insert a second, distinct key which exceeds capacity.
        String secondReturn = cache.put(data.secondKey(), data.secondValue());
        // According to the contract, a new key insertion returns null.
        assertNull(secondReturn, "put should return null when inserting a new key that causes overflow");
        // Wait for the cleanup task to execute.
        TimeUnit.MILLISECONDS.sleep(CLEANUP_DELAY_MILLIS * 3);
        // After cleanup the size must not exceed the configured capacity.
        assertTrue(cache.size() <= data.capacity(), "Cache size must not exceed the configured capacity after cleanup");
        // The most recently inserted entry should still be present.
        assertEquals(data.secondValue(), cache.get(data.secondKey()), "The most recently inserted entry must survive the cleanup");
    }

    @ParameterizedTest(name = "[{index}] capacity={0}")
    @MethodSource("nullKeyProvider")
    @DisplayName("NullKeyThrowsException – put(null, …) throws NullPointerException")
    void testNullKeyThrowsException(NullKeyData data) {
        cache = new ThreadedLRUCacheStrategy<>(data.capacity(), CLEANUP_DELAY_MILLIS);
        assertThrows(NullPointerException.class, () -> cache.put(null, data.value()), "Putting a null key should throw NullPointerException");
    }
}
