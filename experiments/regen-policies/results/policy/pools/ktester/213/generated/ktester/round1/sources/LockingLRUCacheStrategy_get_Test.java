package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.concurrent.locks.Lock;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LockingLRUCacheStrategy_get_Test {

    private LockingLRUCacheStrategy<Integer, String> cache;

    @BeforeEach
    void setupBeforeEach() {
        // capacity = 2 is enough to test LRU eviction behaviour
        cache = new LockingLRUCacheStrategy<>(2);
        // Populate the cache with two entries
        cache.put(1, "one");
        cache.put(2, "two");
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the cache after each test to avoid cross‑test contamination
        cache.clear();
    }

    /**
     * Replaces a private field on the given target object.
     */
    private static void setPrivateField(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Stream<Arguments> missingKeyProvider() {
        return Stream.of(Arguments.of("missingKey"), // any object not present
        Arguments.of(12345));
    }

    @Test
    void get_ExistingKey_ReturnsValueAndPreservesEntryInCache() {
        // Access the first key – this should trigger moveToHead via tryLock()
        String value = cache.get(1);
        assertEquals("one", value, "Cache should return the correct value for an existing key");
        // Adding a new entry will cause eviction because capacity is 2.
        // Since key 1 was accessed recently, key 2 should be evicted.
        cache.put(3, "three");
        // Verify that key 1 is still present and key 2 has been evicted.
        assertEquals("one", cache.get(1), "Key 1 should still be present after eviction of the LRU entry");
        assertNull(cache.get(2), "Key 2 should have been evicted as the least recently used entry");
        assertEquals("three", cache.get(3), "Key 3 should be present after insertion");
    }

    @Test
    void get_NonExistingKey_ReturnsNull() {
        assertNull(cache.get(99), "Getting a non‑existing key should return null");
    }

    @Test
    void get_NullKey_ReturnsNullWhenNotPresent() {
        // The cache does not contain a null key yet
        assertNull(cache.get(null), "Getting a null key that is not present should return null");
        // Put a null key and verify retrieval
        cache.put(null, "nullValue");
        assertEquals("nullValue", cache.get(null), "Cache should return the value associated with a null key");
    }

    /**
     * Retrieves a private field value from the given target object.
     */
    @SuppressWarnings("unchecked")
    private static <T> T getPrivateField(Object target, String fieldName) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return (T) f.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void get_KeyNotPresent_ReturnsNullWithoutLocking() {
        // The cache does not contain the supplied key.
        assertNull(cache.get("missingKey"), "When the cache does not contain the supplied key, get() must return null");
    }

    @Test
    void get_KeyPresent_LockAcquired_MovesToHead() {
        // At this point the LRU order (most‑recent → least‑recent) is: 2, 1
        // Access key 1 – tryLock() will succeed with the real ReentrantLock,
        // causing moveToHead() to be invoked and making 1 the most‑recent entry.
        String value = cache.get(1);
        assertEquals("one", value, "Existing key should return its associated value");
        // Insert a third entry, which forces eviction because capacity = 2.
        // Since key 1 was moved to the head, key 2 should be evicted.
        cache.put(3, "three");
        assertEquals("one", cache.get(1), "Key 1 should still be present after eviction");
        assertNull(cache.get(2), "Key 2 should have been evicted as the LRU entry");
        assertEquals("three", cache.get(3), "Key 3 should be present after insertion");
    }

    @Test
    void get_KeyPresent_LockNotAcquired_DoesNotMoveToHead() {
        // Replace the internal lock with a mock that always fails tryLock().
        Lock mockLock = Mockito.mock(Lock.class);
        when(mockLock.tryLock()).thenReturn(false);
        // unlock() should be a no‑op for the mock.
        doNothing().when(mockLock).unlock();
        setPrivateField(cache, "lock", mockLock);
        // Before the get() call the LRU order is: 2 (most recent), 1 (least recent).
        // Because tryLock() fails, moveToHead() is NOT executed, so the order stays unchanged.
        String value = cache.get(1);
        assertEquals("one", value, "Existing key should still return its value even when lock is not acquired");
        // Adding a third entry forces eviction. The LRU entry should still be key 1.
        cache.put(3, "three");
        assertNull(cache.get(1), "Key 1 should have been evicted because it remained the LRU entry");
        assertEquals("two", cache.get(2), "Key 2 should still be present");
        assertEquals("three", cache.get(3), "Key 3 should be present after insertion");
    }

    @Test
    void get_NullKey_ReturnsNullWhenAbsentAndValueWhenPresent() {
        // Null key not present yet.
        assertNull(cache.get(null), "Getting a null key that is not present should return null");
        // Put a null key and verify retrieval.
        cache.put(null, "nullValue");
        assertEquals("nullValue", cache.get(null), "Cache should return the value associated with a null key");
    }

    @Test
    void get_KeyPresent_WithNullValue_ReturnsNull() {
        // Insert a key whose value is null.
        cache.put(99, null);
        // The node exists, tryLock() will succeed, moveToHead() runs, and the stored null value is returned.
        assertNull(cache.get(99), "When the stored value is null, get() must return null");
    }

    @ParameterizedTest
    @MethodSource("missingKeyProvider")
    void get_MissingKeyParameterized_ReturnsNull(Object key) {
        assertNull(cache.get(key), "Cache.get() must return null for a key that is not present: " + key);
    }
}
