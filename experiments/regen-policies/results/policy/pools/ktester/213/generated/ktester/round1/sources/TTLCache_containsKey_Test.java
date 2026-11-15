package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class TTLCache_containsKey_Test {

    /**
     * Cache instance used by the tests.
     */
    private TTLCache<Integer, String> ttlCache;

    /**
     * Create a fresh cache before each test.
     */
    @BeforeEach
    void setupBeforeEach() {
        // TTL = 200 ms, no size limit, cleanup interval = 50 ms (fast enough for the tests)
        ttlCache = new TTLCache<>(200L, -1, 50L);
    }

    /**
     * Clear the cache after each test to avoid cross‑test contamination.
     */
    @AfterEach
    void teardownAfterEach() {
        ttlCache.clear();
    }

    /**
     * Shut down the shared purge‑executor after all tests have finished.
     */
    @AfterAll
    static void teardownAfterAll() {
        TTLCache.shutdown();
    }

    /**
     * Sets the {@code expiryTime} of the entry associated with {@code key}.
     *
     * @param key        the cache key whose entry should be modified
     * @param expiryTime the new expiry timestamp (epoch ms)
     */
    private void setExpiry(String key, long expiryTime) {
        try {
            TTLCache.CacheEntry<String, String> entry = cacheMap().get(key);
            assertNotNull(entry, "Entry for key [" + key + "] must exist before changing expiry");
            Field f = entry.getClass().getDeclaredField("expiryTime");
            f.setAccessible(true);
            f.setLong(entry, expiryTime);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void containsKey_returnsTrueWhenKeyIsPresentAndNotExpired() {
        ttlCache.put(1, "one");
        ttlCache.put(2, "two");
        assertTrue(ttlCache.containsKey(1), "Key 1 should be reported as present");
        assertTrue(ttlCache.containsKey(2), "Key 2 should be reported as present");
        assertEquals(2, ttlCache.size(), "Cache size should remain unchanged");
    }

    @Test
    void containsKey_returnsFalseAndRemovesEntryWhenKeyIsExpired() throws InterruptedException {
        ttlCache.put(10, "ten");
        ttlCache.put(20, "twenty");
        // Ensure the entries are there initially
        assertTrue(ttlCache.containsKey(10));
        assertTrue(ttlCache.containsKey(20));
        // Wait longer than the TTL so the entries become stale
        // > 200 ms TTL
        Thread.sleep(300L);
        // The first call should detect expiration, remove the entry and return false
        assertFalse(ttlCache.containsKey(10), "Expired key 10 should return false");
        // Subsequent call must still return false because the entry was removed
        assertFalse(ttlCache.containsKey(10), "Repeated call for expired key 10 should still return false");
        // The same behaviour for the second key
        assertFalse(ttlCache.containsKey(20), "Expired key 20 should return false");
        // Cache should now be empty
        assertEquals(0, ttlCache.size(), "All expired entries should have been removed");
    }

    @Test
    void containsKey_returnsFalseWhenKeyIsAbsent() {
        ttlCache.put(5, "five");
        assertFalse(ttlCache.containsKey(99), "A key that was never added must return false");
        assertTrue(ttlCache.containsKey(5), "Existing key must still return true");
    }

    @Test
    void containsKey_handlesNullKey() {
        // The cache implementation supports null keys via a sentinel value.
        ttlCache.put(null, "nullValue");
        assertTrue(ttlCache.containsKey(null), "Null key should be reported as present");
        // After expiration the null key should disappear as well
        assertTrue(ttlCache.containsKey(null), "Null key still within TTL should be present");
        // Wait for expiration
        try {
            TimeUnit.MILLISECONDS.sleep(300L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        assertFalse(ttlCache.containsKey(null), "Expired null key must return false");
    }

    /**
     * Retrieves the internal {@code ConcurrentMap} that stores {@code CacheEntry} objects.
     */
    @SuppressWarnings("unchecked")
    private java.util.concurrent.ConcurrentMap<String, TTLCache.CacheEntry<String, String>> cacheMap() {
        try {
            Field f = TTLCache.class.getDeclaredField("cacheMap");
            f.setAccessible(true);
            return (java.util.concurrent.ConcurrentMap<String, TTLCache.CacheEntry<String, String>>) f.get(ttlCache);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("containsKey - key absent")
    void containsKey_keyAbsent() {
        // No entry for "nonExistingKey"
        assertFalse(ttlCache.containsKey("nonExistingKey"), "When the cacheMap does not contain the supplied key, containsKey should return false.");
    }

    @Test
    @DisplayName("containsKey - key present and not expired")
    void containsKey_keyPresentNotExpired() {
        ttlCache.put("validKey", "value");
        assertTrue(ttlCache.containsKey("validKey"), "When the cacheMap contains an entry whose expiryTime is in the future, containsKey should return true.");
    }

    @Test
    @DisplayName("containsKey - key present but expired")
    void containsKey_keyPresentExpired() throws InterruptedException {
        ttlCache.put("expiredKey", "value");
        // Wait longer than TTL (200 ms) so the entry becomes stale
        Thread.sleep(300L);
        // First call should detect expiration, remove the entry and return false
        assertFalse(ttlCache.containsKey("expiredKey"), "Expired entry must cause containsKey to return false and remove the entry.");
        // Subsequent call must still return false because the entry has been removed
        assertFalse(ttlCache.containsKey("expiredKey"), "Repeated call for the same expired key must continue to return false.");
    }

    @Test
    @DisplayName("containsKey - null key throws NPE")
    void containsKey_nullKeyThrowsNPE() {
        // According to the JSON spec the underlying ConcurrentMap does not accept null keys.
        assertThrows(NullPointerException.class, () -> ttlCache.containsKey(null), "Passing a null key to containsKey should propagate NullPointerException.");
    }

    @Test
    @DisplayName("containsKey - key of incompatible type returns false")
    void containsKey_incompatibleKeyType() {
        ttlCache.put("stringKey", "value");
        // The cache holds String keys; an Integer key should simply be absent.
        assertFalse(ttlCache.containsKey(123), "When the supplied key is of a different type, cacheMap.get returns null, so containsKey must return false.");
    }

    @Test
    @DisplayName("containsKey - key with expiryTime exactly now returns true")
    void containsKey_expiryExactlyNow() {
        ttlCache.put("edgeNowKey", "value");
        long now = System.currentTimeMillis();
        // expiry == now (not < now)
        setExpiry("edgeNowKey", now);
        assertTrue(ttlCache.containsKey("edgeNowKey"), "Entry whose expiryTime equals now must be considered NOT expired and return true.");
    }

    @Test
    @DisplayName("containsKey - key with expiryTime just before now returns false and removes entry")
    void containsKey_justExpired() {
        ttlCache.put("justExpiredKey", "value");
        long justBeforeNow = System.currentTimeMillis() - 1;
        // expiry < now
        setExpiry("justExpiredKey", justBeforeNow);
        assertFalse(ttlCache.containsKey("justExpiredKey"), "Entry whose expiryTime is just before now must be considered expired, removed, and return false.");
        // Verify removal
        assertFalse(ttlCache.containsKey("justExpiredKey"), "Subsequent call must still return false because the entry was removed.");
    }

    @Test
    @DisplayName("containsKey - does not affect other keys after expired removal")
    void containsKey_otherKeysUnaffectedAfterExpiredRemoval() {
        // Insert two keys: one will be forced to expire, the other stays fresh.
        ttlCache.put("validKey2", "good");
        ttlCache.put("expiredKeyForRemoval", "bad");
        // Force the second entry to be expired
        long past = System.currentTimeMillis() - 10_000L;
        setExpiry("expiredKeyForRemoval", past);
        // Trigger expiration handling
        assertFalse(ttlCache.containsKey("expiredKeyForRemoval"), "Expired key must be removed and return false.");
        // The other key must still be present
        assertTrue(ttlCache.containsKey("validKey2"), "Other non‑expired entries must remain unaffected.");
    }
}
