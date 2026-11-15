package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TTLCache_putAll_Test {

    private TTLCache<Integer, String> ttlCache;

    @BeforeEach
    void setupBeforeEach() {
        // Use a generous TTL and no size limit – the TTL does not matter for putAll.
        ttlCache = new TTLCache<>(10_000L, -1);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the cache is cleared between tests so that state does not leak.
        if (ttlCache != null) {
            ttlCache.clear();
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the shared background purge thread used by TTLCache.
        TTLCache.shutdown();
    }

    private static Stream<Arguments> provideNormalCases() {
        return Stream.of(Arguments.of("Standard case: two distinct entries", mapOf("alpha", 1, "beta", 2), 2, mapOf("alpha", 1, "beta", 2)), Arguments.of("Edge case: empty map – cache stays empty", Collections.emptyMap(), 0, Collections.emptyMap()));
    }

    private static Stream<Arguments> provideOverwriteCases() {
        return Stream.of(Arguments.of("Overwrite existing key and add new key", mapOf("alpha", 1), mapOf("alpha", 2, "beta", 3), 2, mapOf("alpha", 2, "beta", 3)), Arguments.of("Identical entries – no change", mapOf("alpha", 1, "beta", 2), mapOf("alpha", 1, "beta", 2), 2, mapOf("alpha", 1, "beta", 2)));
    }

    @Test
    void testPutAllCopiesAllEntries() {
        Map<Integer, String> source = new HashMap<>();
        source.put(1, "One");
        source.put(2, "Two");
        source.put(3, "Three");
        ttlCache.putAll(source);
        assertEquals(3, ttlCache.size(), "Cache should contain three entries");
        assertEquals("One", ttlCache.get(1));
        assertEquals("Two", ttlCache.get(2));
        assertEquals("Three", ttlCache.get(3));
    }

    @Test
    void testPutAllOverridesExistingKeys() {
        // Existing entries
        ttlCache.put(1, "OldOne");
        ttlCache.put(2, "OldTwo");
        // New map with overlapping key 2 and a new key 3
        Map<Integer, String> source = new HashMap<>();
        source.put(2, "NewTwo");
        source.put(3, "Three");
        ttlCache.putAll(source);
        assertEquals(3, ttlCache.size(), "Cache should now contain three distinct keys");
        assertEquals("OldOne", ttlCache.get(1), "Key 1 should remain unchanged");
        assertEquals("NewTwo", ttlCache.get(2), "Key 2 should be replaced by new value");
        assertEquals("Three", ttlCache.get(3), "Key 3 should be added");
    }

    @Test
    void testPutAllAcceptsNullKeyAndNullValue() {
        Map<Integer, String> source = new HashMap<>();
        source.put(null, "NullKey");
        source.put(4, null);
        ttlCache.putAll(source);
        assertEquals(2, ttlCache.size(), "Cache should contain two entries (null key and null value)");
        assertEquals("NullKey", ttlCache.get(null), "Null key should retrieve its value");
        assertNull(ttlCache.get(4), "Value for key 4 should be null");
    }

    @Test
    void testPutAllWithEmptyMapDoesNotChangeCache() {
        ttlCache.put(10, "Ten");
        ttlCache.put(20, "Twenty");
        Map<Integer, String> empty = new HashMap<>();
        ttlCache.putAll(empty);
        assertEquals(2, ttlCache.size(), "Size should stay unchanged after putting an empty map");
        assertEquals("Ten", ttlCache.get(10));
        assertEquals("Twenty", ttlCache.get(20));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideNormalCases")
    void testPutAll_NormalCases(String description, Map<String, Integer> entriesMap, int expectedSize, Map<String, Integer> expectedContent) {
        // Use a String‑Integer cache for these cases
        TTLCache<String, Integer> cache = new TTLCache<>(10_000L, -1);
        cache.putAll(entriesMap);
        assertEquals(expectedSize, cache.size(), "Size mismatch");
        expectedContent.forEach((k, v) -> assertEquals(v, cache.get(k), "Value mismatch for key: " + k));
    }

    // -----------------------------------------------------------------
    @Test
    void testPutAll_NullMapArgument() {
        assertThrows(NullPointerException.class, () -> ttlCache.putAll(null), "putAll(null) must throw NullPointerException as per Map contract");
    }

    @Test
    void testPutAll_NullValueForValidKey() {
        Map<String, Integer> source = new HashMap<>();
        // null value is allowed
        source.put("alpha", null);
        TTLCache<String, Integer> cache = new TTLCache<>(10_000L, -1);
        cache.putAll(source);
        assertEquals(1, cache.size());
        assertNull(cache.get("alpha"));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideOverwriteCases")
    void testPutAll_OverwriteBehavior(String description, Map<String, Integer> prepopulate, Map<String, Integer> toPut, int expectedSize, Map<String, Integer> expectedContent) {
        TTLCache<String, Integer> cache = new TTLCache<>(10_000L, -1);
        cache.putAll(prepopulate);
        cache.putAll(toPut);
        assertEquals(expectedSize, cache.size(), "Size after overwrite");
        expectedContent.forEach((k, v) -> assertEquals(v, cache.get(k), "Incorrect value for key " + k));
    }

    // -----------------------------------------------------------------
    @Test
    void testPutAll_NullEntryInEntrySet() {
        Map<Integer, String> brokenMap = new AbstractMap<Integer, String>() {

            @Override
            public Set<Entry<Integer, String>> entrySet() {
                // Create a set that contains a single null element
                return new HashSet<Entry<Integer, String>>() {

                    {
                        add(null);
                    }
                };
            }
        };
        assertThrows(NullPointerException.class, () -> ttlCache.putAll(brokenMap), "Iterating over a null entry should cause NullPointerException");
    }

    // -----------------------------------------------------------------
    @Test
    void testPutAll_AfterShutdown() {
        // globally shut down the background thread
        TTLCache.shutdown();
        Map<Integer, String> source = new HashMap<>();
        source.put(1, "one");
        assertThrows(IllegalStateException.class, () -> ttlCache.putAll(source), "putAll after shutdown must throw IllegalStateException");
        // Re‑initialise the global state for subsequent tests
        // ensure clean state (no‑op if already shut down)
        TTLCache.shutdown();
    }

    // -----------------------------------------------------------------
    @Test
    void testPutAll_TypeMismatch_Key() {
        TTLCache<Integer, String> cache = new TTLCache<>(10_000L, -1);
        Map<String, String> wrongMap = new HashMap<>();
        wrongMap.put("notAnInteger", "value");
        assertThrows(ClassCastException.class, () -> cache.putAll(wrongMap), "Putting a map with wrong key type should throw ClassCastException");
    }

    @Test
    void testPutAll_TypeMismatch_Value() {
        TTLCache<String, Integer> cache = new TTLCache<>(10_000L, -1);
        Map<String, String> wrongMap = new HashMap<>();
        wrongMap.put("alpha", "notAnInteger");
        assertThrows(ClassCastException.class, () -> cache.putAll(wrongMap), "Putting a map with wrong value type should throw ClassCastException");
    }

    /**
     * Convenience var‑args map builder for test readability.
     */
    @SafeVarargs
    private static <K, V> Map<K, V> mapOf(Object... entries) {
        if (entries.length % 2 != 0) {
            throw new IllegalArgumentException("Even number of arguments required");
        }
        Map<K, V> map = new HashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            @SuppressWarnings("unchecked")
            K key = (K) entries[i];
            @SuppressWarnings("unchecked")
            V value = (V) entries[i + 1];
            map.put(key, value);
        }
        return map;
    }
}
