package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TTLCache_keySet_Test {

    private TTLCache<Integer, String> ttlCache;

    /**
     * Create a fresh cache instance before each test. The default TTL of 10 000 ms
     * is long enough to avoid accidental expiration unless a test explicitly
     * wants to verify that behaviour.
     */
    @BeforeEach
    void setupBeforeEach() {
        // TTL 10 s, no size limit
        ttlCache = new TTLCache<>(10_000, -1);
    }

    /**
     * Clear the cache after each test to avoid cross‑test contamination.
     */
    @AfterEach
    void teardownAfterEach() {
        if (ttlCache != null) {
            ttlCache.clear();
        }
        ttlCache = null;
    }

    /**
     * Shut down the shared purge thread after all tests have run.
     */
    @AfterAll
    static void teardownAfterAll() {
        TTLCache.shutdown();
    }

    private static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(// empty cache
        Arguments.of("Empty cache returns empty Set", Collections.emptyList(), Collections.emptySet()), // two distinct entries
        Arguments.of("Two distinct entries – both keys present", Arrays.asList(new AbstractMap.SimpleEntry<>(1, "A"), new AbstractMap.SimpleEntry<>(2, "B")), new HashSet<>(Arrays.asList(1, 2))), // duplicate key – second overwrites first
        Arguments.of("Duplicate keys – only one unique key present", Arrays.asList(new AbstractMap.SimpleEntry<>(5, "first"), new AbstractMap.SimpleEntry<>(5, "second")), new HashSet<>(Collections.singletonList(5))), // null key
        Arguments.of("Cache contains a null key", Collections.singletonList(new AbstractMap.SimpleEntry<>(null, "value")), new HashSet<>(Collections.singletonList(null))), // null value (key still present)
        Arguments.of("Cache contains a null value – key still appears", Collections.singletonList(new AbstractMap.SimpleEntry<>(10, null)), new HashSet<>(Collections.singletonList(10))));
    }

    private static Stream<Arguments> additionalScenariosProvider() {
        return Stream.of(// multiple distinct keys
        Arguments.of("Multiple distinct keys – all should be present", Arrays.asList(new AbstractMap.SimpleEntry<>(1, "A"), new AbstractMap.SimpleEntry<>(2, "B"), new AbstractMap.SimpleEntry<>(3, "C")), null, new HashSet<>(Arrays.asList(1, 2, 3))), // overwrite with null value
        Arguments.of("Overwrite existing key with null value – key still present", Arrays.asList(new AbstractMap.SimpleEntry<>(7, "val"), new AbstractMap.SimpleEntry<>(7, null)), null, new HashSet<>(Collections.singletonList(7))), // null key with null value
        Arguments.of("Null key with null value – null key appears", Collections.singletonList(new AbstractMap.SimpleEntry<>(null, null)), null, new HashSet<>(Collections.singletonList(null))), // explicit empty cache (same as first test but kept for completeness)
        Arguments.of("Explicit empty cache – returns empty Set", Collections.emptyList(), null, Collections.emptySet()), // remove after insertion
        Arguments.of("Insert two keys then remove one – only remaining key appears", Arrays.asList(new AbstractMap.SimpleEntry<>(100, "X"), new AbstractMap.SimpleEntry<>(200, "Y")), 100, new HashSet<>(Collections.singletonList(200))));
    }

    @Test
    void testKeySetBasic() {
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        ttlCache.put(3, "C");
        Set<Integer> expected = new HashSet<>(Arrays.asList(1, 2, 3));
        Set<Integer> actual = ttlCache.keySet();
        assertEquals(expected, actual, "keySet should contain exactly the inserted keys");
    }

    @Test
    void testKeySetAfterRemove() {
        ttlCache.put(10, "X");
        ttlCache.put(20, "Y");
        ttlCache.put(30, "Z");
        ttlCache.remove(20);
        Set<Integer> expected = new HashSet<>(Arrays.asList(10, 30));
        Set<Integer> actual = ttlCache.keySet();
        assertEquals(expected, actual, "keySet should reflect removal of a key");
        assertFalse(actual.contains(20), "Removed key must not be present");
    }

    @Test
    void testKeySetEmptyCache() {
        Set<Integer> keys = ttlCache.keySet();
        assertTrue(keys.isEmpty(), "keySet of a newly created cache should be empty");
    }

    @Test
    void testKeySetAfterExpiration() throws InterruptedException {
        // Short TTL and a short cleanup interval to force expiration quickly
        ttlCache = new TTLCache<>(200, -1, 50);
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        // Verify keys are present before expiration
        Set<Integer> before = ttlCache.keySet();
        assertEquals(new HashSet<>(Arrays.asList(1, 2)), before, "Keys should be present before TTL expires");
        // Wait longer than TTL so the background task can purge the entries
        Thread.sleep(300);
        Set<Integer> after = ttlCache.keySet();
        assertTrue(after.isEmpty(), "keySet should be empty after entries have expired");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("basicFunctionalityProvider")
    void testKeySetParameterized(String description, List<Entry<Integer, String>> entries, Set<Integer> expected) {
        // Populate the cache using the public API (which respects null handling)
        for (Entry<Integer, String> e : entries) {
            ttlCache.put(e.getKey(), e.getValue());
        }
        Set<Integer> actual = ttlCache.keySet();
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("additionalScenariosProvider")
    void testKeySetAdditionalScenarios(String description, List<Entry<Integer, String>> entries, Integer removeKey, Set<Integer> expected) {
        for (Entry<Integer, String> e : entries) {
            ttlCache.put(e.getKey(), e.getValue());
        }
        if (removeKey != null) {
            ttlCache.remove(removeKey);
        }
        Set<Integer> actual = ttlCache.keySet();
        assertEquals(expected, actual, description);
    }

    @Test
    void testKeySetWhenCacheMapIsNull() throws Exception {
        // Force the private field cacheMap to null via reflection
        Field cacheMapField = TTLCache.class.getDeclaredField("cacheMap");
        cacheMapField.setAccessible(true);
        cacheMapField.set(ttlCache, null);
        // Expect a NullPointerException when keySet() tries to call cacheMap.values()
        assertThrows(NullPointerException.class, ttlCache::keySet, "Calling keySet() with a null internal cacheMap should throw NPE");
    }

    @Test
    void testKeySetWhenEntryIsNull() throws Exception {
        // Insert a null CacheEntry directly into the underlying map
        Field cacheMapField = TTLCache.class.getDeclaredField("cacheMap");
        cacheMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentMap<Integer, TTLCache.CacheEntry<Integer, String>> map = (ConcurrentMap<Integer, TTLCache.CacheEntry<Integer, String>>) cacheMapField.get(ttlCache);
        // Put a legitimate entry first so the map is not empty
        ttlCache.put(1, "A");
        // Now replace the entry with null using the map directly
        map.put(1, null);
        // keySet() iterates over values and will encounter a null entry,
        // leading to a NullPointerException when accessing entry.node
        assertThrows(NullPointerException.class, ttlCache::keySet, "A null CacheEntry in the map should cause NPE in keySet()");
    }

    @Test
    void testKeySetWhenNodeIsNull() throws Exception {
        // Create a mock CacheEntry whose node field is null
        @SuppressWarnings("unchecked")
        TTLCache.CacheEntry<Integer, String> mockEntry = mock(TTLCache.CacheEntry.class);
        // Ensure the mock returns null for the node field via reflection
        Field nodeField = TTLCache.CacheEntry.class.getDeclaredField("node");
        nodeField.setAccessible(true);
        nodeField.set(mockEntry, null);
        // Insert the mocked entry into the internal map
        Field cacheMapField = TTLCache.class.getDeclaredField("cacheMap");
        cacheMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentMap<Integer, TTLCache.CacheEntry<Integer, String>> map = (ConcurrentMap<Integer, TTLCache.CacheEntry<Integer, String>>) cacheMapField.get(ttlCache);
        map.put(42, mockEntry);
        // keySet() will try to read entry.node.key and hit NPE
        assertThrows(NullPointerException.class, ttlCache::keySet, "A CacheEntry with a null node should cause NPE in keySet()");
    }
}
