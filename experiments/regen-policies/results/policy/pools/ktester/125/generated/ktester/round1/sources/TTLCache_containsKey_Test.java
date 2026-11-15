package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

class TTLCache_containsKey_Test {

    private TTLCache<Integer, String> ttlCache;

    /* --------------------------------------------------------------------- */
    private static class ArgumentsHolder {

        // raw JSON value (e.g., "\"missingKey\"" or "null")
        final String key;

        // expected boolean result (ignored for exception cases)
        final boolean expected;

        final String description;

        ArgumentsHolder(String key, boolean expected, String description) {
            this.key = key;
            this.expected = expected;
            this.description = description;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Ensure any previously created background threads are stopped before the suite runs.
        TTLCache.shutdown();
    }

    @BeforeEach
    void setupBeforeEach() {
        // TTL of 200 ms, no size limit, cleanup interval of 100 ms.
        ttlCache = new TTLCache<>(200L, -1, 100L);
    }

    @AfterEach
    void teardownAfterEach() {
        if (ttlCache != null) {
            ttlCache.clear();
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the shared purge‑task thread used by all TTLCache instances.
        TTLCache.shutdown();
    }

    /**
     * Sets the expiryTime of the CacheEntry associated with the supplied key.
     */
    private void setExpiry(String key, long expiryEpochMillis) throws Exception {
        ConcurrentMap<String, Object> map = getCacheMap();
        Object entry = map.get(key);
        assertNotNull(entry, "CacheEntry for key '" + key + "' must exist before adjusting expiry.");
        Field expiryField = entry.getClass().getDeclaredField("expiryTime");
        expiryField.setAccessible(true);
        expiryField.setLong(entry, expiryEpochMillis);
    }

    // ---------- group: containsKey - key not present ----------
    static Stream<ArgumentsHolder> notPresentProvider() {
        return Stream.of(new ArgumentsHolder("\"missingKey\"", false, "The cache does not contain the supplied key; cacheMap.get returns null, so containsKey should return false."), new ArgumentsHolder("null", false, "A null key is looked up; ConcurrentMap.get(null) returns null, therefore containsKey must return false."));
    }

    @Test
    void containsKey_returnsTrueWhenKeyExistsAndNotExpired() {
        ttlCache.put(1, "one");
        ttlCache.put(2, "two");
        assertTrue(ttlCache.containsKey(1), "Key 1 should be present immediately after insertion");
        assertTrue(ttlCache.containsKey(2), "Key 2 should be present immediately after insertion");
        assertEquals(2, ttlCache.size(), "Cache size should reflect the two inserted entries");
    }

    @Test
    void containsKey_returnsFalseWhenKeyHasExpired() throws InterruptedException {
        ttlCache.put(10, "ten");
        ttlCache.put(20, "twenty");
        // Verify they are present before the TTL elapses.
        assertTrue(ttlCache.containsKey(10));
        assertTrue(ttlCache.containsKey(20));
        // Wait longer than the TTL (200 ms) so the entries expire.
        Thread.sleep(350);
        // After expiration the entries should be removed and containsKey must return false.
        assertFalse(ttlCache.containsKey(10), "Key 10 should be reported as absent after expiration");
        assertFalse(ttlCache.containsKey(20), "Key 20 should be reported as absent after expiration");
        assertEquals(0, ttlCache.size(), "Cache should be empty after entries have expired");
    }

    @Test
    void containsKey_returnsFalseWhenKeyNeverAdded() {
        assertFalse(ttlCache.containsKey(999), "containsKey must return false for a key that was never inserted");
    }

    /**
     * Returns the internal ConcurrentMap used by the cache (via reflection).
     */
    @SuppressWarnings("unchecked")
    private ConcurrentMap<String, Object> getCacheMap() throws Exception {
        Field mapField = TTLCache.class.getDeclaredField("cacheMap");
        mapField.setAccessible(true);
        return (ConcurrentMap<String, Object>) mapField.get(ttlCache);
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("notPresentProvider")
    void containsKey_keyNotPresent(ArgumentsHolder arg) {
        Object key = arg.key.equals("null") ? null : arg.key.replace("\"", "");
        boolean result = ttlCache.containsKey(key);
        assertEquals(arg.expected, result);
    }

    // ---------- group: containsKey - key present and not expired ----------
    @Test
    @DisplayName("containsKey returns true when key exists and is not expired")
    void containsKey_keyPresentNotExpired() throws Exception {
        ttlCache.put("presentKey", "value");
        // ensure expiry is far in the future (now + 5 seconds)
        setExpiry("presentKey", System.currentTimeMillis() + 5_000L);
        assertTrue(ttlCache.containsKey("presentKey"), "The cache contains the supplied key and its entry is not expired.");
    }

    // ---------- group: containsKey - key present but expired ----------
    @Test
    @DisplayName("containsKey returns false and removes entry when key is expired")
    void containsKey_keyPresentExpired() throws Exception {
        ttlCache.put("expiredKey", "value");
        // set expiry to the past
        setExpiry("expiredKey", System.currentTimeMillis() - 10_000L);
        assertFalse(ttlCache.containsKey("expiredKey"), "Expired entry should be removed and containsKey must return false.");
        // verify removal
        assertEquals(0, ttlCache.size(), "Cache size should be zero after the expired entry is removed.");
    }

    // ---------- group: containsKey - key present with expiry exactly now ----------
    @Test
    @DisplayName("containsKey treats expiry == now as NOT expired (boundary condition)")
    void containsKey_boundaryExpiry() throws Exception {
        ttlCache.put("boundaryKey", "value");
        long now = System.currentTimeMillis();
        // exact boundary
        setExpiry("boundaryKey", now);
        // The method uses `< now`, so equality means not expired.
        assertTrue(ttlCache.containsKey("boundaryKey"), "Entry with expiry equal to current time should be considered valid.");
    }

    // ---------- group: containsKey - key present with past (negative) expiry ----------
    @Test
    @DisplayName("containsKey returns false for entry with past (negative) expiry")
    void containsKey_negativeExpiry() throws Exception {
        ttlCache.put("negativeExpiryKey", "value");
        setExpiry("negativeExpiryKey", System.currentTimeMillis() - 10_000L);
        assertFalse(ttlCache.containsKey("negativeExpiryKey"), "Entry with a past expiry must be treated as expired.");
    }

    // ---------- group: containsKey - key present with far‑future expiry ----------
    @Test
    @DisplayName("containsKey returns true for entry with far‑future expiry")
    void containsKey_farFutureExpiry() throws Exception {
        ttlCache.put("farFutureKey", "value");
        // far future: now + 1 hour
        setExpiry("farFutureKey", System.currentTimeMillis() + 3_600_000L);
        assertTrue(ttlCache.containsKey("farFutureKey"), "Entry with a far‑future expiry should be considered present.");
    }

    // ---------- group: containsKey - expired entry with mismatched key type throws ClassCastException ----------
    @Test
    @DisplayName("containsKey throws ClassCastException when key type mismatches during removal")
    void containsKey_mismatchedKeyTypeThrowsClassCastException() throws Exception {
        // Use a raw TTLCache to allow us to insert a wrong‑typed entry.
        TTLCache rawCache = new TTLCache<>(200L, -1, 100L);
        // Insert a normal entry so we have a CacheEntry instance to reuse.
        rawCache.put("dummy", "value");
        // Obtain the internal map and the CacheEntry object.
        Field mapField = TTLCache.class.getDeclaredField("cacheMap");
        mapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentMap rawMap = (ConcurrentMap) mapField.get(rawCache);
        Object entry = rawMap.values().iterator().next();
        // Set expiry to the past so the method will attempt removal.
        Field expiryField = entry.getClass().getDeclaredField("expiryTime");
        expiryField.setAccessible(true);
        expiryField.setLong(entry, System.currentTimeMillis() - 1_000L);
        // Insert the entry under an Integer key (mismatched type).
        rawMap.put(123, entry);
        // The cache is typed as <String,String> but we call containsKey with Integer.
        assertThrows(ClassCastException.class, () -> rawCache.containsKey(123), "Calling containsKey with a mismatched key type should cause ClassCastException during removeEntry.");
    }

    // ---------- group: containsKey - internal cacheMap is null throws NullPointerException ----------
    @Test
    @DisplayName("containsKey throws NullPointerException when internal cacheMap is null")
    void containsKey_nullCacheMapThrowsNPE() throws Exception {
        // Force the private cacheMap field to null via reflection.
        Field mapField = TTLCache.class.getDeclaredField("cacheMap");
        mapField.setAccessible(true);
        mapField.set(ttlCache, null);
        assertThrows(NullPointerException.class, () -> ttlCache.containsKey("anyKey"), "When cacheMap is null, containsKey must throw NullPointerException.");
    }
}
