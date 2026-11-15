package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TTLCache_keySet_Test {

    private TTLCache<Integer, String> ttlCache;

    @BeforeEach
    void setupBeforeEach() {
        // Use a long TTL so entries do not expire during most tests
        ttlCache = new TTLCache<>(10_000L, -1);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the cache to avoid interference between tests
        if (ttlCache != null) {
            ttlCache.clear();
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the shared background purge thread used by TTLCache
        TTLCache.shutdown();
    }

    /* -------------------------------------------------------------
     *  Parameterised test source (used for potential future extensions)
     * ------------------------------------------------------------- */
    private static Stream<Arguments> emptyCacheProvider() {
        return Stream.of(Arguments.of(new TTLCache<>(10_000L, -1)));
    }

    @Test
    void testKeySetContainsAllInsertedKeys() {
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        ttlCache.put(3, "C");
        Set<Integer> keys = ttlCache.keySet();
        assertEquals(3, keys.size(), "keySet should contain three keys");
        assertTrue(keys.contains(1));
        assertTrue(keys.contains(2));
        assertTrue(keys.contains(3));
    }

    @Test
    void testKeySetIsSnapshotAndDoesNotAffectCache() {
        ttlCache.put(10, "X");
        ttlCache.put(20, "Y");
        Set<Integer> keys = ttlCache.keySet();
        // Modify the returned set
        keys.remove(10);
        keys.add(99);
        // The cache must remain unchanged
        assertEquals(2, ttlCache.size(), "Cache size should stay unchanged after modifying keySet");
        assertTrue(ttlCache.containsKey(10));
        assertTrue(ttlCache.containsKey(20));
        assertFalse(ttlCache.containsKey(99));
    }

    @Test
    void testKeySetAfterEntriesExpire() throws InterruptedException {
        // Short TTL and short cleanup interval to force expiration quickly
        ttlCache = new TTLCache<>(200L, -1, 50L);
        ttlCache.put(5, "P");
        ttlCache.put(6, "Q");
        // Ensure entries are present initially
        assertEquals(2, ttlCache.keySet().size());
        // Wait longer than TTL so entries expire
        Thread.sleep(300);
        Set<Integer> keysAfterExpiry = ttlCache.keySet();
        assertTrue(keysAfterExpiry.isEmpty(), "keySet should be empty after entries have expired");
    }

    @Test
    void testKeySetOnEmptyCache() {
        Set<Integer> emptyKeys = ttlCache.keySet();
        assertNotNull(emptyKeys, "keySet should never be null");
        assertTrue(emptyKeys.isEmpty(), "keySet of an empty cache must be empty");
    }

    @Test
    void testKeySetSingleEntry() {
        ttlCache.put("alpha", 1);
        Set<Object> keys = ttlCache.keySet();
        assertEquals(Set.of("alpha"), keys);
    }

    @Test
    void testKeySetMultipleDistinctEntries() {
        ttlCache.put("one", 1);
        ttlCache.put("two", 2);
        ttlCache.put("three", 3);
        Set<Object> keys = ttlCache.keySet();
        assertEquals(Set.of("one", "two", "three"), keys);
    }

    @Test
    void testKeySetDuplicateKey() {
        ttlCache.put("dup", "first");
        // overwrite
        ttlCache.put("dup", "second");
        Set<Object> keys = ttlCache.keySet();
        assertEquals(Set.of("dup"), keys);
    }

    @Test
    void testKeySetAllowsNullKey() {
        ttlCache.put(null, "nullValue");
        Set<Object> keys = ttlCache.keySet();
        assertEquals(1, keys.size());
        assertTrue(keys.contains(null));
    }

    @Test
    void testKeySetAfterRemoval() {
        ttlCache.put("keep", "value1");
        ttlCache.put("remove", "value2");
        ttlCache.remove("remove");
        Set<Object> keys = ttlCache.keySet();
        assertEquals(Set.of("keep"), keys);
    }

    @Test
    void testKeySetSnapshotImmutabilityAfterExternalModification() {
        ttlCache.put("original", "v");
        Set<Object> snapshot = ttlCache.keySet();
        // modify the snapshot – this must not affect the cache
        snapshot.remove("original");
        snapshot.add("newInSnapshot");
        // cache should still contain the original key
        Set<Object> after = ttlCache.keySet();
        assertEquals(Set.of("original"), after);
    }

    @Test
    void testKeySetAfterClear() {
        ttlCache.put("a", 1);
        ttlCache.put("b", 2);
        ttlCache.clear();
        Set<Object> keys = ttlCache.keySet();
        assertTrue(keys.isEmpty(), "keySet should be empty after clear()");
    }

    @Test
    void testKeySetWithPutAll() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);
        source.put("c", 3);
        ttlCache.putAll(source);
        Set<Object> keys = ttlCache.keySet();
        assertEquals(Set.of("a", "b", "c"), keys);
    }

    @Test
    void testKeySetUpdateSameKeyDoesNotChangeSet() {
        ttlCache.put("update", "first");
        ttlCache.put("update", "second");
        Set<Object> keys = ttlCache.keySet();
        assertEquals(Set.of("update"), keys);
    }

    @Test
    void testKeySetRemoveNonExistentKeyHasNoEffect() {
        ttlCache.put("stay", "value");
        // non‑existent
        ttlCache.remove("ghost");
        Set<Object> keys = ttlCache.keySet();
        assertEquals(Set.of("stay"), keys);
    }

    @Test
    void testKeySetSnapshotImmutabilityWhenNewEntryAddedLater() {
        ttlCache.put("original", "v");
        // capture snapshot
        Set<Object> snapshot = ttlCache.keySet();
        // modify cache after snapshot
        ttlCache.put("newKey", "newValue");
        // snapshot must stay unchanged
        assertEquals(Set.of("original"), snapshot);
        // a fresh call reflects both keys
        Set<Object> fresh = ttlCache.keySet();
        assertEquals(Set.of("original", "newKey"), fresh);
    }

    @Test
    void testKeySetLargeNumberOfKeys() {
        for (int i = 0; i < 1000; i++) {
            ttlCache.put("k" + i, i);
        }
        Set<Object> keys = ttlCache.keySet();
        assertEquals(1000, keys.size());
        // Spot‑check a few keys
        assertTrue(keys.contains("k0"));
        assertTrue(keys.contains("k500"));
        assertTrue(keys.contains("k999"));
    }

    @Test
    void testKeySetSpecialAndUnicodeKeys() {
        ttlCache.put("sp ace", 1);
        ttlCache.put("sp!@#", 2);
        ttlCache.put("中文", 3);
        Set<Object> keys = ttlCache.keySet();
        assertEquals(Set.of("sp ace", "sp!@#", "中文"), keys);
    }

    @Test
    void testKeySetThrowsNPEWhenCacheMapIsNull() throws Exception {
        // Obtain the private field and set it to null
        Field mapField = TTLCache.class.getDeclaredField("cacheMap");
        mapField.setAccessible(true);
        mapField.set(ttlCache, null);
        assertThrows(NullPointerException.class, ttlCache::keySet, "keySet() should throw NPE when internal cacheMap is null");
    }

    @Test
    void testKeySetThrowsNPEWhenCacheEntryIsNull() throws Exception {
        // Insert a null CacheEntry directly into the internal map
        Field mapField = TTLCache.class.getDeclaredField("cacheMap");
        mapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentMap<Object, Object> map = (ConcurrentMap<Object, Object>) mapField.get(ttlCache);
        // null value
        map.put("nullEntryKey", null);
        assertThrows(NullPointerException.class, ttlCache::keySet, "Iterating over a null CacheEntry should cause NPE");
    }

    @Test
    void testKeySetThrowsNPEWhenCacheEntryNodeIsNull() throws Exception {
        // Create a mock CacheEntry whose node field is null
        Class<?> entryClass = Class.forName("com.cedarsoftware.util.TTLCache$CacheEntry");
        Object mockEntry = java.lang.reflect.Proxy.newProxyInstance(entryClass.getClassLoader(), new Class[] { entryClass }, (proxy, method, args) -> {
            if ("node".equals(method.getName())) {
                // node is null
                return null;
            }
            return null;
        });
        // Insert the malformed entry into the map
        Field mapField = TTLCache.class.getDeclaredField("cacheMap");
        mapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentMap<Object, Object> map = (ConcurrentMap<Object, Object>) mapField.get(ttlCache);
        map.put("badNodeKey", mockEntry);
        assertThrows(NullPointerException.class, ttlCache::keySet, "Accessing entry.node.key when node is null should throw NPE");
    }

    @ParameterizedTest
    @MethodSource("emptyCacheProvider")
    void parameterisedTestKeySetOnEmptyCache(TTLCache<Object, Object> cache) {
        Set<Object> keys = cache.keySet();
        assertNotNull(keys);
        assertTrue(keys.isEmpty());
    }
}
