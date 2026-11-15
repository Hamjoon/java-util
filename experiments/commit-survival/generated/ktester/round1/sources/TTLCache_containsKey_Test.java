package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TTLCache_containsKey_Test {

    private TTLCache<Integer, String> cache;

    private static class SimpleCase {

        final String key;

        final boolean expected;

        final String description;

        SimpleCase(String key, boolean expected, String description) {
            this.key = key;
            this.expected = expected;
            this.description = description;
        }
    }

    private static class FakeKey {

        private final String id;

        FakeKey(String id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof String) {
                return id.equals(o);
            }
            if (o instanceof FakeKey) {
                return id.equals(((FakeKey) o).id);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }
    }

    /**
     * Create a fresh cache before each test.  A short TTL (100 ms) together with a
     * short cleanup interval (50 ms) makes the expiration behaviour deterministic
     * without slowing the test suite.
     */
    @BeforeEach
    void setupBeforeEach() {
        // ttl = 100 ms, no LRU size limit, cleanup interval = 50 ms
        cache = new TTLCache<>(100L, -1, 50L);
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
     * The background purge thread used by TTLCache is shared across all instances.
     * Shut it down once after all tests have run.
     */
    @AfterAll
    static void teardownAfterAll() {
        TTLCache.shutdown();
    }

    private static Stream<SimpleCase> simpleCases() {
        return Stream.of(new SimpleCase("missingKey", false, "The cache does not contain the supplied key."), new SimpleCase("validKey", true, "Key present and not expired."), new SimpleCase("123", false, "Wrong key type (Integer) – map returns null."), new SimpleCase("boundaryKey", true, "Expiry exactly equals now – not considered expired."));
    }

    @Test
    void containsKey_returnsTrueWhenKeyIsPresentAndNotExpired() {
        cache.put(1, "one");
        cache.put(2, "two");
        assertTrue(cache.containsKey(1), "Key 1 should be present");
        assertTrue(cache.containsKey(2), "Key 2 should be present");
        // sanity check – size should be 2
        assertEquals(2, cache.size());
    }

    @Test
    void containsKey_returnsFalseWhenKeyIsAbsent() {
        cache.put(10, "ten");
        assertFalse(cache.containsKey(99), "Key 99 was never added");
        // existing key still works
        assertTrue(cache.containsKey(10));
    }

    @Test
    void containsKey_returnsFalseAndRemovesEntryWhenKeyHasExpired() throws InterruptedException {
        // Insert a key that will expire quickly
        cache.put(5, "five");
        // Verify it is present immediately
        assertTrue(cache.containsKey(5));
        assertEquals(1, cache.size());
        // Wait longer than the TTL (100 ms) to guarantee expiration
        Thread.sleep(250);
        // The call should now report false and also clean the entry out
        assertFalse(cache.containsKey(5), "Expired key should return false");
        assertEquals(0, cache.size(), "Expired entry should have been removed");
    }

    @Test
    void containsKey_multipleCallsAfterExpirationDoNotThrowAndRemainFalse() throws InterruptedException {
        cache.put(42, "answer");
        assertTrue(cache.containsKey(42));
        // Let the entry expire
        Thread.sleep(200);
        // First check after expiry
        assertFalse(cache.containsKey(42));
        // Subsequent checks should still be false and must not cause NPEs or other errors
        assertFalse(cache.containsKey(42));
        // a key that never existed
        assertFalse(cache.containsKey(999));
    }

    /**
     * Inserts a {@link TTLCache.CacheEntry} directly into the private
     * {@code cacheMap} field.  This allows us to set an arbitrary expiry time
     * without waiting for the TTL thread.
     *
     * @param key        the logical key (must match the generic type K)
     * @param value      the value to store
     * @param expiryTime absolute epoch millis when the entry should expire
     */
    @SuppressWarnings("unchecked")
    private void putEntryDirectly(String key, String value, long expiryTime) throws Exception {
        // Obtain the private CacheEntry class
        Class<?> entryClass = Class.forName("com.cedarsoftware.util.TTLCache$CacheEntry");
        // Find the (K key, V value, long expiryTime) constructor (the actual signature may differ;
        // we use reflection to set fields directly if constructor is not accessible)
        Object entry = entryClass.getDeclaredConstructor().newInstance();
        // Set fields via reflection
        Field keyField = entryClass.getDeclaredField("key");
        Field valueField = entryClass.getDeclaredField("value");
        Field expiryField = entryClass.getDeclaredField("expiryTime");
        keyField.setAccessible(true);
        valueField.setAccessible(true);
        expiryField.setAccessible(true);
        keyField.set(entry, key);
        valueField.set(entry, value);
        expiryField.setLong(entry, expiryTime);
        // Insert into the internal map
        Field mapField = TTLCache.class.getDeclaredField("cacheMap");
        mapField.setAccessible(true);
        ConcurrentMap<String, Object> map = (ConcurrentMap<String, Object>) mapField.get(cache);
        map.put(key, entry);
    }

    @ParameterizedTest(name = "{index}: containsKey({0}) → {2}")
    @MethodSource("simpleCases")
    void containsKey_variousScenarios(SimpleCase c) throws Exception {
        // Prepare the cache according to the case description
        switch(c.key) {
            case "validKey":
                cache.put("validKey", "value");
                break;
            case "boundaryKey":
                // expiryTime == now
                long now = System.currentTimeMillis();
                putEntryDirectly("boundaryKey", "value", now);
                break;
            case "123":
                // Insert a String key, then query with an Integer – map will return null
                cache.put("123", "value");
                break;
            default:
                // "missingKey" – nothing to insert
                break;
        }
        // Act & Assert
        assertEquals(c.expected, cache.containsKey(c.key), c.description);
    }

    @Test
    void containsKey_expiredEntryIsRemoved() throws Exception {
        // Insert an entry that is already expired (expiryTime in the past)
        long past = System.currentTimeMillis() - 1_000L;
        putEntryDirectly("expiredKey", "old", past);
        // First call should trigger removal and return false
        assertFalse(cache.containsKey("expiredKey"), "Expired entry must cause containsKey to return false");
        // Verify that the entry has been removed from the map
        assertEquals(0, cache.size(), "Expired entry should have been removed from the cache");
    }

    @Test
    void containsKey_nullKeyPropagatesNullPointerException() {
        assertThrows(NullPointerException.class, () -> cache.containsKey(null), "Passing null should propagate NullPointerException from the underlying map");
    }

    @Test
    void containsKey_neverExpiringEntry() throws Exception {
        putEntryDirectly("maxExpiryKey", "immortal", Long.MAX_VALUE);
        assertTrue(cache.containsKey("maxExpiryKey"), "Entry with expiryTime = Long.MAX_VALUE must be considered not expired");
    }

    @Test
    void containsKey_multipleCallsAfterExpirationStayFalse() throws Exception {
        long past = System.currentTimeMillis() - 10L;
        putEntryDirectly("removedAfterExpired", "gone", past);
        // First call removes the entry
        assertFalse(cache.containsKey("removedAfterExpired"));
        // Subsequent calls must still be false and must not throw
        assertFalse(cache.containsKey("removedAfterExpired"));
        assertFalse(cache.containsKey("removedAfterExpired"));
    }

    @Test
    void containsKey_ClassCastException_onExpiredKey() throws Exception {
        // Insert a proper String key that is already expired
        long past = System.currentTimeMillis() - 1_000L;
        putEntryDirectly("expiredKey", "value", past);
        // Use a different runtime type that is equal to the stored key
        FakeKey fake = new FakeKey("expiredKey");
        // The call should attempt to cast the FakeKey to String inside removeEntry()
        // and therefore throw ClassCastException
        assertThrows(ClassCastException.class, () -> cache.containsKey(fake), "Calling containsKey with a key of wrong runtime type on an expired entry should throw ClassCastException");
    }

    @Test
    void containsKey_internalMapIsNull_throwsNullPointerException() throws Exception {
        // Use reflection to set the private cacheMap field to null
        Field mapField = TTLCache.class.getDeclaredField("cacheMap");
        mapField.setAccessible(true);
        mapField.set(cache, null);
        // Any call to containsKey should now NPE
        assertThrows(NullPointerException.class, () -> cache.containsKey("anyKey"), "When internal cacheMap is null, containsKey must throw NullPointerException");
    }
}
