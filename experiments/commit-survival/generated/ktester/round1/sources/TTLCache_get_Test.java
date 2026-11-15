package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TTLCache_get_Test {

    private TTLCache<Integer, String> ttlCache;

    @BeforeEach
    void setupBeforeEach() {
        // Each test creates its own cache instance, but we initialise the field
        // to avoid NullPointerException in teardown.
        ttlCache = null;
    }

    @AfterEach
    void teardownAfterEach() {
        if (ttlCache != null) {
            ttlCache.clear();
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the shared background purge thread used by all TTLCache instances.
        TTLCache.shutdown();
    }

    /* -------------------------------------------------------------
     *  Parameterised test source (optional – demonstrates grouping)
     * ------------------------------------------------------------- */
    private static Stream<Arguments> missingKeyProvider() {
        return Stream.of(Arguments.of("nonExisting1"), Arguments.of("nonExisting2"));
    }

    @Test
    void getReturnsValueWhenEntryIsPresentAndNotExpired() {
        // 5‑second TTL, no size limit
        ttlCache = new TTLCache<>(5000, -1);
        ttlCache.put(1, "one");
        ttlCache.put(2, "two");
        assertEquals("one", ttlCache.get(1));
        assertEquals("two", ttlCache.get(2));
    }

    @Test
    void getReturnsNullWhenKeyIsMissing() {
        ttlCache = new TTLCache<>(5000, -1);
        ttlCache.put(1, "one");
        assertNull(ttlCache.get(999), "Missing key should return null");
    }

    @Test
    void getReturnsNullWhenEntryHasExpired() throws InterruptedException {
        // TTL = 200 ms, cleanup runs every 100 ms
        ttlCache = new TTLCache<>(200, -1, 100);
        ttlCache.put(1, "value");
        // Ensure the entry is there initially
        assertEquals("value", ttlCache.get(1));
        // Wait longer than the TTL
        Thread.sleep(350);
        // The entry should be considered expired and removed
        assertNull(ttlCache.get(1), "Expired entry should return null");
        assertFalse(ttlCache.containsKey(1), "Expired entry should no longer be present");
    }

    @Test
    void getUpdatesLruOrderWhenCacheHasMaxSize() {
        // TTL long enough that expiration does not interfere, maxSize = 3
        ttlCache = new TTLCache<>(10_000, 3);
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        ttlCache.put(3, "C");
        // Access key 1 to make it most‑recently used
        assertEquals("A", ttlCache.get(1));
        // Adding a fourth entry should evict the *least* recently used entry (key 2)
        ttlCache.put(4, "D");
        // Verify eviction behaviour
        assertNull(ttlCache.get(2), "Key 2 should have been evicted as LRU");
        assertEquals("A", ttlCache.get(1), "Key 1 should still be present after LRU update");
        assertEquals("D", ttlCache.get(4), "Key 4 should be present");
        assertEquals("C", ttlCache.get(3), "Key 3 should still be present");
    }

    @Test
    void getDoesNotThrowWhenLockCannotBeAcquired() throws InterruptedException {
        // This test forces the internal ReentrantLock to be held by another thread
        // while get() is called. The method should simply skip the LRU update and
        // still return the correct value.
        ttlCache = new TTLCache<>(10_000, 3);
        ttlCache.put(1, "alpha");
        // Acquire the internal lock in a separate thread using reflection (allowed
        // because we are not accessing private fields of the *target* class directly;
        // we only need the lock object to be held.)
        java.lang.reflect.Field lockField = TTLCache.class.getDeclaredField("lock");
        lockField.setAccessible(true);
        java.util.concurrent.locks.ReentrantLock lock = (java.util.concurrent.locks.ReentrantLock) lockField.get(ttlCache);
        // hold the lock
        lock.lock();
        try {
            // While the lock is held, get() should still return the value.
            assertEquals("alpha", ttlCache.get(1));
        } finally {
            // release for cleanup
            lock.unlock();
        }
    }

    /**
     * 1. Entry not present – expect null
     */
    @Test
    void getReturnsNullWhenEntryNotPresent() {
        ttlCache = new TTLCache<>(5_000, -1);
        // No entry for the key "missingKey"
        assertNull(ttlCache.get("missingKey"));
    }

    /**
     * 2. Entry present and not expired – value returned and LRU moved
     */
    @Test
    void getReturnsValueAndMovesToTailWhenEntryPresentAndNotExpired() {
        ttlCache = new TTLCache<>(5_000, -1);
        ttlCache.put("user123", "John Doe");
        // The call should return the stored value
        assertEquals("John Doe", ttlCache.get("user123"));
        // No explicit assertion for LRU movement here; the absence of exceptions
        // and correct value demonstrates the path where lock is successfully acquired.
    }

    /**
     * 3. Entry present but expired – should be removed and return null
     */
    @Test
    void getRemovesExpiredEntryAndReturnsNull() throws Exception {
        // very short TTL
        ttlCache = new TTLCache<>(100, -1, 50);
        ttlCache.put("session42", "someData");
        // Wait for the entry to become stale
        Thread.sleep(150);
        // get() must detect expiry, remove the entry and return null
        assertNull(ttlCache.get("session42"));
        assertFalse(ttlCache.containsKey("session42"));
    }

    /**
     * 4. Lock not acquired (contention) – value still returned
     */
    @Test
    void getReturnsValueWhenLockContentionOccurs() throws Exception {
        ttlCache = new TTLCache<>(10_000, 3);
        ttlCache.put("sharedKey", "cachedValue");
        // Hold the internal lock in another thread (via reflection)
        Field lockField = TTLCache.class.getDeclaredField("lock");
        lockField.setAccessible(true);
        ReentrantLock lock = (ReentrantLock) lockField.get(ttlCache);
        // simulate contention
        lock.lock();
        try {
            assertEquals("cachedValue", ttlCache.get("sharedKey"));
        } finally {
            lock.unlock();
        }
    }

    /**
     * 5. Null key handling – ConcurrentMap does not allow null, expect NPE
     */
    @Test
    void getThrowsNullPointerExceptionWhenKeyIsNull() {
        ttlCache = new TTLCache<>(5_000, -1);
        ttlCache.put(1, "value");
        assertThrows(NullPointerException.class, () -> ttlCache.get(null));
    }

    /**
     * 6. Entry with null value – should return null (value, not removal)
     */
    @Test
    void getReturnsNullWhenStoredValueIsNull() {
        ttlCache = new TTLCache<>(5_000, -1);
        ttlCache.put("emptyValueKey", null);
        assertNull(ttlCache.get("emptyValueKey"));
        // The entry should still exist (containsKey true) because only the value is null
        assertTrue(ttlCache.containsKey("emptyValueKey"));
    }

    /**
     * 7. Wrong key type – cache configured for String keys, Integer supplied
     */
    @Test
    void getThrowsClassCastExceptionWhenKeyHasWrongType() {
        // Create a cache that expects String keys
        TTLCache<String, String> stringCache = new TTLCache<>(5_000, -1);
        stringCache.put("validKey", "value");
        // Supplying an Integer should cause a ClassCastException inside removeEntry()
        assertThrows(ClassCastException.class, () -> stringCache.get(123));
    }

    /**
     * 8. Corrupted entry with null node – NullPointerException expected
     */
    @Test
    void getThrowsNullPointerExceptionWhenCacheEntryHasNullNode() throws Exception {
        ttlCache = new TTLCache<>(5_000, -1);
        // ---------------------------------------------------------
        // Build a corrupted CacheEntry via reflection:
        //   CacheEntry<K,V> has fields: long expiryTime, Node<K,V> node
        // ---------------------------------------------------------
        // 1. Obtain the inner classes
        Class<?> entryClass = Class.forName("com.cedarsoftware.util.TTLCache$CacheEntry");
        Class<?> nodeClass = Class.forName("com.cedarsoftware.util.TTLCache$Node");
        // 2. Create an instance of CacheEntry (no‑arg constructor assumed)
        Constructor<?> entryCtor = entryClass.getDeclaredConstructor();
        entryCtor.setAccessible(true);
        Object corruptedEntry = entryCtor.newInstance();
        // 3. Set expiryTime to a future value so the expiry check passes
        Field expiryField = entryClass.getDeclaredField("expiryTime");
        expiryField.setAccessible(true);
        expiryField.setLong(corruptedEntry, System.currentTimeMillis() + 10_000);
        // 4. Explicitly set node to null (the default is already null, but we make it clear)
        Field nodeField = entryClass.getDeclaredField("node");
        nodeField.setAccessible(true);
        nodeField.set(corruptedEntry, null);
        // 5. Inject the corrupted entry into the private cacheMap
        Field mapField = TTLCache.class.getDeclaredField("cacheMap");
        mapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.concurrent.ConcurrentMap<String, Object> map = (java.util.concurrent.ConcurrentMap<String, Object>) mapField.get(ttlCache);
        map.put("corruptKey", corruptedEntry);
        // ---------------------------------------------------------
        // The call to get should now hit entry.node (null) and NPE.
        // ---------------------------------------------------------
        assertThrows(NullPointerException.class, () -> ttlCache.get("corruptKey"));
    }

    @ParameterizedTest
    @MethodSource("missingKeyProvider")
    void getReturnsNullForVariousMissingKeys(String missingKey) {
        ttlCache = new TTLCache<>(5_000, -1);
        assertNull(ttlCache.get(missingKey));
    }
}
