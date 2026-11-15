package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.concurrent.locks.ReentrantLock;

@ExtendWith(MockitoExtension.class)
class TTLCache_get_Test {

    private TTLCache<Integer, String> cache;

    /**
     * Simple key class that pretends to be equal to a String with the same content.
     */
    static class BadKey {

        private final String value;

        BadKey(String value) {
            this.value = value;
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof String) {
                return value.equals(obj);
            }
            if (obj instanceof BadKey) {
                return value.equals(((BadKey) obj).value);
            }
            return false;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Ensure any previous background threads are stopped before the suite runs
        TTLCache.shutdown();
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use a TTL long enough to avoid accidental expiration in most tests
        cache = new TTLCache<>(10_000, -1);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the cache to avoid cross‑test contamination
        if (cache != null) {
            cache.clear();
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the shared purge executor used by TTLCache
        TTLCache.shutdown();
    }

    /**
     * Retrieves a private field value from an object.
     */
    private static Object getPrivateField(Object target, String fieldName) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.get(target);
    }

    /**
     * Sets a private field value on an object.
     */
    private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    /**
     * Retrieves the CacheEntry object stored for a given key.
     */
    private static Object getCacheEntry(TTLCache<?, ?> cache, Object key) throws Exception {
        // cacheMap is a ConcurrentMap<K, CacheEntry<K,V>>
        Object map = getPrivateField(cache, "cacheMap");
        @SuppressWarnings("unchecked")
        java.util.concurrent.ConcurrentMap<Object, Object> cm = (java.util.concurrent.ConcurrentMap<Object, Object>) map;
        return cm.get(key);
    }

    @Test
    void getReturnsValueWhenEntryExistsAndNotExpired() {
        cache.put(1, "one");
        cache.put(2, "two");
        assertEquals("one", cache.get(1), "Should return the stored value for key 1");
        assertEquals("two", cache.get(2), "Should return the stored value for key 2");
    }

    @Test
    void getReturnsNullWhenKeyIsMissing() {
        cache.put(1, "one");
        assertNull(cache.get(999), "Missing key should yield null");
    }

    @Test
    void getRemovesExpiredEntryAndReturnsNull() throws InterruptedException {
        // TTL of 200 ms, cleanup interval 50 ms
        TTLCache<Integer, String> shortTtlCache = new TTLCache<>(200, -1, 50);
        shortTtlCache.put(1, "temp");
        shortTtlCache.put(2, "data");
        // Verify they are present initially
        assertEquals("temp", shortTtlCache.get(1));
        assertEquals("data", shortTtlCache.get(2));
        assertEquals(2, shortTtlCache.size());
        // Wait longer than TTL so entries expire
        Thread.sleep(350);
        // get should return null and also remove the stale entry
        assertNull(shortTtlCache.get(1), "Expired entry should be removed and return null");
        assertNull(shortTtlCache.get(2), "Expired entry should be removed and return null");
        assertEquals(0, shortTtlCache.size(), "Cache should be empty after expiration");
    }

    @Test
    void getUpdatesLRUOrderWhenLockAcquired() {
        // TTL long enough, maxSize enables LRU tracking
        TTLCache<Integer, String> lruCache = new TTLCache<>(10_000, 3);
        lruCache.put(1, "A");
        lruCache.put(2, "B");
        lruCache.put(3, "C");
        // Access key 2 to make it most recently used
        assertEquals("B", lruCache.get(2));
        // Adding a new entry should evict the least recently used (key 1)
        lruCache.put(4, "D");
        assertNull(lruCache.get(1), "Key 1 should have been evicted as LRU");
        assertEquals("B", lruCache.get(2), "Key 2 should still be present");
        assertEquals("C", lruCache.get(3), "Key 3 should still be present");
        assertEquals("D", lruCache.get(4), "Key 4 should be present");
    }

    @Test
    void getKeyNotPresentReturnsNull() {
        // cache is empty, ask for a missing key (String type for demonstration)
        assertNull(cache.get("missingKey"), "When the cache does not contain the supplied key, get() should return null.");
    }

    @Test
    void getPresentNotExpiredLockAcquiredReturnsValue() {
        // Use a cache with a reasonable TTL and no size limit
        TTLCache<String, String> c = new TTLCache<>(10_000, -1);
        c.put("validKey", "validValue");
        // The lock is free, tryLock() will succeed → LRU tail update occurs
        assertEquals("validValue", c.get("validKey"), "The cache contains an entry for the supplied key whose expiryTime is far in the future. The internal lock can be obtained, so the entry is moved to the LRU tail and the stored value is returned.");
    }

    @Test
    void getPresentNotExpiredLockNotAcquiredReturnsValue() throws Exception {
        // Create a cache and pre‑populate it
        TTLCache<String, String> c = new TTLCache<>(10_000, -1);
        c.put("validKey", "validValue");
        // Acquire the internal lock in the current thread to force tryLock() to fail
        ReentrantLock lock = (ReentrantLock) getPrivateField(c, "lock");
        // now tryLock() inside get() will return false
        lock.lock();
        try {
            assertEquals("validValue", c.get("validKey"), "Another thread holds the internal ReentrantLock when get() is called, causing tryLock() to fail. The method should still return the correct value but skip the LRU‑tail update.");
        } finally {
            // clean up
            lock.unlock();
        }
    }

    @Test
    void getPresentButExpiredRemovesEntryAndReturnsNull() throws Exception {
        TTLCache<String, String> c = new TTLCache<>(10_000, -1);
        c.put("expiredKey", "oldValue");
        // Force the entry to be expired by setting its expiryTime in the past
        Object entry = getCacheEntry(c, "expiredKey");
        setPrivateField(entry, "expiryTime", System.currentTimeMillis() - 1_000L);
        assertNull(c.get("expiredKey"), "The cache contains an entry whose expiryTime is earlier than the current system time. get() must remove the entry from the cache and return null.");
        assertFalse(c.containsKey("expiredKey"), "Expired entry should have been removed from the cache.");
    }

    @Test
    void getBoundaryExpiryEqualsCurrentTimeReturnsValue() throws Exception {
        TTLCache<String, String> c = new TTLCache<>(10_000, -1);
        c.put("boundaryKey", "boundaryValue");
        // Set expiryTime to exactly now
        Object entry = getCacheEntry(c, "boundaryKey");
        long now = System.currentTimeMillis();
        setPrivateField(entry, "expiryTime", now);
        // Slight delay to ensure System.currentTimeMillis() inside get() is >= now
        Thread.sleep(1);
        assertEquals("boundaryValue", c.get("boundaryKey"), "The entry’s expiryTime is exactly equal to System.currentTimeMillis() at the moment of the call. Because the code checks `expiryTime < currentTime`, the entry is considered valid and its value is returned.");
    }

    @Test
    void getNullKeyPropagatesNullPointerException() {
        // ConcurrentMap does not permit null keys; TTLCache should let the NPE bubble up
        assertThrows(NullPointerException.class, () -> cache.get(null), "Calling get(null) should propagate a NullPointerException.");
    }

    @Test
    void getExpiredEntryWithIncompatibleKeyCausesClassCastException() throws Exception {
        // Cache holds a String key that will be expired
        TTLCache<String, String> c = new TTLCache<>(10_000, -1);
        c.put("expiredKey", "someValue");
        // Expire it
        Object entry = getCacheEntry(c, "expiredKey");
        setPrivateField(entry, "expiryTime", System.currentTimeMillis() - 1_000L);
        // BadKey equals the String "expiredKey" but is a different type
        BadKey badKey = new BadKey("expiredKey");
        // Expect ClassCastException when removeEntry attempts to cast BadKey to String
        assertThrows(ClassCastException.class, () -> c.get(badKey), "The cache holds an expired entry whose key is a String \"expiredKey\". A different object (BadKey) that is equal to that String is passed to get(). The map returns the entry, the expiry check triggers removeEntry((K)key) where K is String, causing a ClassCastException.");
    }
}
