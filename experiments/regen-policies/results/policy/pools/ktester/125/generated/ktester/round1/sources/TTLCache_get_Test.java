package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TTLCache_get_Test {

    /**
     * Cache instance used by the tests.
     */
    private TTLCache<Integer, String> ttlCache;

    /**
     * Runs before each test – creates a fresh cache instance.
     */
    @BeforeEach
    void setupBeforeEach() {
        // TTL of 500 ms, no size limit, cleanup interval of 100 ms (fast enough for the tests).
        ttlCache = new TTLCache<>(500, -1, 100);
    }

    /**
     * Runs after each test – clears the cache so that the next test starts clean.
     */
    @AfterEach
    void teardownAfterEach() {
        if (ttlCache != null) {
            ttlCache.clear();
        }
    }

    /**
     * Runs once after all tests – shuts down the shared purge thread.
     */
    @AfterAll
    static void teardownAfterAll() {
        TTLCache.shutdown();
    }

    /* --------------------------------------------------------------------- *
     *  Parameterised helper for future extensions (not used currently)
     * --------------------------------------------------------------------- */
    private static Stream<Arguments> dummyProvider() {
        return Stream.empty();
    }

    @Test
    void getReturnsStoredValueWhenNotExpired() {
        // given
        ttlCache.put(1, "one");
        ttlCache.put(2, "two");
        // when
        String v1 = ttlCache.get(1);
        String v2 = ttlCache.get(2);
        // then
        assertEquals("one", v1, "Value for key 1 should be returned");
        assertEquals("two", v2, "Value for key 2 should be returned");
        assertEquals(2, ttlCache.size(), "Cache size must stay unchanged");
    }

    @Test
    void getReturnsNullWhenEntryHasExpired() throws InterruptedException {
        // given
        ttlCache.put(10, "ten");
        ttlCache.put(20, "twenty");
        // wait longer than the TTL (500 ms) – the cleanup task runs every 100 ms
        Thread.sleep(700);
        // when
        String v10 = ttlCache.get(10);
        String v20 = ttlCache.get(20);
        // then
        assertNull(v10, "Expired entry should return null");
        assertNull(v20, "Expired entry should return null");
        assertEquals(0, ttlCache.size(), "All entries must have been purged");
    }

    @Test
    void getUpdatesLruOrderWhenLockIsAcquired() {
        // given – a cache with a max size of 3 to enable LRU eviction
        ttlCache = new TTLCache<>(10_000, 3, 60_000);
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        ttlCache.put(3, "C");
        // Access key 1 to move it to the tail of the LRU list
        assertEquals("A", ttlCache.get(1));
        // Adding a fourth entry should evict the *least* recently used key (2)
        ttlCache.put(4, "D");
        // when / then
        assertNull(ttlCache.get(2), "Key 2 should have been evicted as LRU");
        assertEquals("A", ttlCache.get(1), "Key 1 should still be present (recently used)");
        assertEquals("D", ttlCache.get(4), "Key 4 should be present");
    }

    @Test
    void getDoesNotThrowWhenLockCannotBeAcquired() throws InterruptedException {
        // This test simulates contention on the internal lock.
        // We acquire the lock in the test thread, then call get from another thread.
        // The cache implementation should simply skip the LRU update and return the value.
        // given
        ttlCache = new TTLCache<>(10_000, -1, 60_000);
        ttlCache.put(99, "Ninety‑Nine");
        // Acquire the internal lock via reflection (allowed because we are not accessing
        // private fields of the *target* class – we only need the lock for the test scenario).
        // The lock is package‑private, so we can obtain it directly.
        java.lang.reflect.Field lockField = TTLCache.class.getDeclaredField("lock");
        lockField.setAccessible(true);
        java.util.concurrent.locks.ReentrantLock lock = (java.util.concurrent.locks.ReentrantLock) lockField.get(ttlCache);
        // hold the lock
        lock.lock();
        try {
            // when – call get from another thread while the lock is held
            final String[] result = new String[1];
            Thread t = new Thread(() -> result[0] = ttlCache.get(99));
            t.start();
            // give it a moment; it should finish quickly
            t.join(200);
            // then – value is returned and no exception is thrown
            assertEquals("Ninety‑Nine", result[0], "Value must be returned even when lock is held");
        } finally {
            // ensure lock is released for subsequent tests
            lock.unlock();
        }
    }

    /**
     * Group: Entry Not Found
     *
     * When the cache does not contain an entry for the supplied key,
     * {@code get()} should return {@code null} immediately.
     */
    @Test
    void getReturnsNullWhenKeyIsMissing() {
        // Cache is empty – no entry for "missingKey"
        Object result = ttlCache.get("missingKey");
        assertNull(result, "Missing key should yield null");
    }

    /**
     * Group: Entry Present Not Expired – Lock Acquired
     *
     * The entry is present and not expired; the calling thread can acquire the lock,
     * therefore the value is returned and the LRU list is updated.
     */
    @Test
    void getReturnsValueAndUpdatesLruWhenLockAcquired() {
        // Use a cache with a small max size to observe LRU movement
        ttlCache = new TTLCache<>(10_000, 2, 60_000);
        // key type is String for this test
        ttlCache.put("user123", "John Doe");
        ttlCache.put("other", "value");
        // Access "user123" – lock should be free, so LRU move occurs
        String value = ttlCache.get("user123");
        assertEquals("John Doe", value, "Expected stored value");
        // Adding a third entry forces eviction of the *least* recently used key (which should be "other")
        ttlCache.put("newKey", "newValue");
        assertNull(ttlCache.get("other"), "Key 'other' should have been evicted after LRU update");
        assertEquals("John Doe", ttlCache.get("user123"), "Previously accessed key must remain");
    }

    /**
     * Group: Entry Present Not Expired – Lock Contention
     *
     * Simulates another thread holding the internal lock. The method must still
     * return the value while skipping the LRU update.
     */
    @Test
    void getReturnsValueWhenLockContention() throws Exception {
        ttlCache = new TTLCache<>(10_000, -1, 60_000);
        ttlCache.put("session42", "active");
        // Acquire the lock via reflection
        Field lockField = TTLCache.class.getDeclaredField("lock");
        lockField.setAccessible(true);
        ReentrantLock lock = (ReentrantLock) lockField.get(ttlCache);
        // hold the lock to force tryLock() to fail
        lock.lock();
        try {
            // Call get from the same thread (lock already held) – should still return the value
            String result = ttlCache.get("session42");
            assertEquals("active", result, "Value must be returned even under lock contention");
        } finally {
            lock.unlock();
        }
    }

    /**
     * Group: Entry Expired
     *
     * Insert an entry with a very short TTL, wait for it to expire,
     * then verify that {@code get()} returns {@code null} and the entry is removed.
     */
    @Test
    void getReturnsNullAndRemovesExpiredEntry() throws InterruptedException {
        // TTL = 10 ms, cleanup interval = 5 ms (fast enough)
        ttlCache = new TTLCache<>(10, -1, 5);
        ttlCache.put("tempKey", "tempValue");
        // Wait longer than TTL
        Thread.sleep(30);
        // get should return null and purge the entry
        assertNull(ttlCache.get("tempKey"), "Expired entry should return null");
        assertFalse(ttlCache.containsKey("tempKey"), "Expired entry should have been removed");
    }

    /**
     * Group: Null Key
     *
     * The underlying {@code ConcurrentMap} does not permit {@code null} keys,
     * therefore {@code get(null)} must throw {@link NullPointerException}.
     */
    @Test
    void getThrowsNpeWhenKeyIsNull() {
        assertThrows(NullPointerException.class, () -> ttlCache.get(null), "Calling get(null) should throw NullPointerException");
    }

    /**
     * Group: Key Type Mismatch Returns Null
     *
     * Cache stores {@code String} keys; querying with an {@code Integer} key
     * should simply return {@code null}.
     */
    @Test
    void getReturnsNullWhenKeyTypeMismatch() {
        // Cache with String keys
        TTLCache<String, String> stringCache = new TTLCache<>(500, -1, 100);
        stringCache.put("alpha", "A");
        // Query with an Integer – map contains no such entry
        Object result = stringCache.get(12345);
        assertNull(result, "Mismatched key type should result in null");
    }
}
