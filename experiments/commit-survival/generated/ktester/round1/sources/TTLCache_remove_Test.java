package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@ExtendWith(MockitoExtension.class)
class TTLCache_remove_Test {

    private TTLCache<Integer, String> ttlCache;

    /**
     * Create a fresh cache before each test. The TTL is set to a large value
     * (10 seconds) and LRU eviction is disabled (maxSize = -1) so that the
     * behaviour of {@code remove(Object)} can be observed without interference
     * from expiration or eviction logic.
     */
    @BeforeEach
    void setupBeforeEach() {
        ttlCache = new TTLCache<>(10_000L, -1);
    }

    /**
     * Clear the cache after each test to avoid cross‑test contamination.
     */
    @AfterEach
    void teardownAfterEach() {
        if (ttlCache != null) {
            ttlCache.clear();
        }
    }

    /**
     * Shut down the shared purge thread created by TTLCache instances.
     */
    @AfterAll
    static void teardownAfterAll() {
        TTLCache.shutdown();
    }

    @Test
    void removeExistingKey_returnsValueAndRemovesEntry() {
        // given
        ttlCache.put("alpha", "value1");
        ttlCache.put("beta", "value2");
        assertEquals(2, ttlCache.size());
        // when
        String removed = ttlCache.remove("alpha");
        // then
        assertEquals("value1", removed, "remove should return the associated value");
        assertNull(ttlCache.get("alpha"), "Removed key should no longer be present");
        assertFalse(ttlCache.containsKey("alpha"), "Cache should not contain the removed key");
        assertEquals(1, ttlCache.size(), "Cache size should decrease by one");
        // other entry must stay untouched
        assertEquals("value2", ttlCache.get("beta"));
    }

    @Test
    void removeNonExistingKey_returnsNullAndLeavesCacheUntouched() {
        // given
        ttlCache.put(10, "ten");
        ttlCache.put(20, "twenty");
        assertEquals(2, ttlCache.size());
        // when
        // key 99 was never added
        String removed = ttlCache.remove(99);
        // then
        assertNull(removed, "remove should return null for a missing key");
        assertEquals(2, ttlCache.size(), "Cache size must stay unchanged");
        assertTrue(ttlCache.containsKey(10));
        assertTrue(ttlCache.containsKey(20));
    }

    @Test
    void removeMultipleKeys_sequentialRemovalsWorkCorrectly() {
        // given
        ttlCache.put("A", "a");
        ttlCache.put("B", "b");
        ttlCache.put("C", "c");
        assertEquals(3, ttlCache.size());
        // when & then – first removal
        assertEquals("b", ttlCache.remove("B"));
        assertNull(ttlCache.get("B"));
        assertEquals(2, ttlCache.size());
        // when & then – second removal
        assertEquals("a", ttlCache.remove("A"));
        assertNull(ttlCache.get("A"));
        assertEquals(1, ttlCache.size());
        // remaining entry should still be accessible
        assertEquals("c", ttlCache.get("C"));
    }

    @Test
    void removeNullKey_behavesAccordingToMapContract() {
        // given
        ttlCache.put(null, "nullValue");
        ttlCache.put("five", "5");
        assertEquals(2, ttlCache.size());
        // when
        String removed = ttlCache.remove(null);
        // then
        assertEquals("nullValue", removed);
        assertNull(ttlCache.get(null));
        assertEquals(1, ttlCache.size());
        assertTrue(ttlCache.containsKey("five"));
    }

    @Test
    void removeNullKey_throwsNullPointerException_whenUnderlyingMapDisallowsNulls() throws Exception {
        // TTLCache internally uses a ConcurrentMap that does not permit null keys.
        // By replacing the internal map with a plain ConcurrentHashMap (which throws NPE on null key),
        // we can verify that remove(null) propagates the exception as per the contract.
        Field mapField = TTLCache.class.getDeclaredField("cacheMap");
        mapField.setAccessible(true);
        ConcurrentMap<String, TTLCache.CacheEntry<String, String>> rawMap = new ConcurrentHashMap<>();
        mapField.set(ttlCache, rawMap);
        // now the cache behaves like a normal ConcurrentHashMap regarding null keys
        assertThrows(NullPointerException.class, () -> ttlCache.remove(null), "remove(null) should throw NullPointerException when underlying map forbids null keys");
    }

    @Test
    void removeKeyOfDifferentType_returnsNullWithoutAffectingCache() {
        // given a cache that stores String keys
        ttlCache.put("key1", "value1");
        ttlCache.put("key2", "value2");
        assertEquals(2, ttlCache.size());
        // when removing an Integer key (different type)
        String removed = ttlCache.remove(123);
        // then
        assertNull(removed, "Removing a key of a different type should return null");
        assertEquals(2, ttlCache.size(), "Cache size must remain unchanged");
        assertTrue(ttlCache.containsKey("key1"));
        assertTrue(ttlCache.containsKey("key2"));
    }

    @Test
    void removeAfterClear_returnsNull() {
        ttlCache.put("delta", "D");
        ttlCache.put("epsilon", "E");
        assertEquals(2, ttlCache.size());
        ttlCache.clear();
        assertEquals(0, ttlCache.size());
        String removed = ttlCache.remove("delta");
        assertNull(removed, "After clear, remove should return null for any key");
    }

    @Test
    void removeKeyWithNullValue_returnsNullAndRemovesEntry() {
        ttlCache.put("gamma", null);
        ttlCache.put("zeta", "Z");
        assertEquals(2, ttlCache.size());
        String removed = ttlCache.remove("gamma");
        assertNull(removed, "Stored null value should be returned as null");
        assertNull(ttlCache.get("gamma"));
        assertEquals(1, ttlCache.size());
        assertTrue(ttlCache.containsKey("zeta"));
    }

    @Test
    void removeWithUnmodifiableUnderlyingMap_propagatesUnsupportedOperationException() throws Exception {
        // Build an unmodifiable map and inject it via reflection.
        ConcurrentMap<String, TTLCache.CacheEntry<String, String>> modifiable = new ConcurrentHashMap<>();
        // Populate with a dummy entry so the map is not empty (not required but realistic)
        modifiable.put("anyKey", null);
        ConcurrentMap<String, TTLCache.CacheEntry<String, String>> unmodifiable = Collections.unmodifiableMap(modifiable);
        Field mapField = TTLCache.class.getDeclaredField("cacheMap");
        mapField.setAccessible(true);
        mapField.set(ttlCache, unmodifiable);
        // The cache now delegates to an unmodifiable map; remove should throw.
        assertThrows(UnsupportedOperationException.class, () -> ttlCache.remove("anyKey"), "remove should propagate UnsupportedOperationException from the underlying map");
    }
}
