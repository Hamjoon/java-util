package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TTLCache_putAll_Test {

    private TTLCache<Integer, String> ttlCache;

    private Map<Integer, String> sourceMap;

    @BeforeEach
    void setupBeforeEach() {
        // TTL of 10 seconds, no size limit – sufficient for the putAll tests
        ttlCache = new TTLCache<>(10_000L, -1);
        sourceMap = new LinkedHashMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the cache is cleared between tests to avoid cross‑test contamination
        if (ttlCache != null) {
            ttlCache.clear();
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the shared background purge thread used by TTLCache
        TTLCache.shutdown();
    }

    /**
     * Provides normal‑operation test data for {@code putAll}.
     *
     * Each {@link Arguments} contains:
     * <ul>
     *   <li>a description (for readability in test reports)</li>
     *   <li>a source map to feed into {@code putAll}</li>
     *   <li>the expected size after the operation</li>
     *   <li>the expected content (key → value) in the cache</li>
     * </ul>
     */
    private static Stream<Arguments> normalOperationProvider() {
        return Stream.of(// Regular map with three distinct entries
        Arguments.of("Regular map with three entries", new HashMap<String, Integer>() {

            {
                put("a", 1);
                put("b", 2);
                put("c", 3);
            }
        }, 3, Map.of("a", 1, "b", 2, "c", 3)), // Duplicate key – last value wins
        Arguments.of("Map with duplicate key (last value should win)", new HashMap<String, Integer>() {

            {
                put("x", 10);
                // overwrites previous entry
                put("x", 20);
            }
        }, 1, Map.of("x", 20)), // Empty map – no‑op
        Arguments.of("Empty map should leave cache unchanged", new HashMap<String, Integer>(), 0, Collections.emptyMap()), // Null key and null value handling
        Arguments.of("Map containing null key and null value", new HashMap<String, Integer>() {

            {
                put(null, 777);
                put("key", null);
            }
        }, 2, new HashMap<String, Integer>() {

            {
                put(null, 777);
                put("key", null);
            }
        }), // Insertion order preservation (LinkedHashMap)
        Arguments.of("LinkedHashMap preserves insertion order (size check only)", new LinkedHashMap<String, Integer>() {

            {
                put("first", 1);
                put("second", 2);
                put("third", 3);
            }
        }, 3, Map.of("first", 1, "second", 2, "third", 3)), // Immutable source map
        Arguments.of("Unmodifiable map should be processed without exception", Collections.unmodifiableMap(new HashMap<String, Integer>() {

            {
                put("u", 100);
            }
        }), 1, Map.of("u", 100)));
    }

    @Test
    void putAllAddsAllEntriesFromMap() {
        // given
        sourceMap.put("1", 100);
        sourceMap.put("2", 200);
        sourceMap.put("3", 300);
        // when
        ttlCache.putAll(sourceMap);
        // then
        assertEquals(3, ttlCache.size(), "Cache should contain three entries after putAll");
        assertEquals(100, ttlCache.get("1"));
        assertEquals(200, ttlCache.get("2"));
        assertEquals(300, ttlCache.get("3"));
    }

    @Test
    void putAllWithEmptyMapLeavesCacheUnchanged() {
        // given
        // pre‑populate cache
        ttlCache.put("10", 999);
        assertEquals(1, ttlCache.size());
        // when
        // empty map
        ttlCache.putAll(Map.of());
        // then
        assertEquals(1, ttlCache.size(), "Cache size should remain unchanged after putting an empty map");
        assertEquals(999, ttlCache.get("10"));
    }

    @Test
    void putAllOverwritesExistingKeys() {
        // given
        ttlCache.put(1, "old");
        ttlCache.put(2, "old2");
        sourceMap.put(1, "newA");
        sourceMap.put(2, "newB");
        sourceMap.put(3, "newC");
        // when
        ttlCache.putAll(sourceMap);
        // then
        assertEquals(3, ttlCache.size());
        assertEquals("newA", ttlCache.get(1), "Existing key should be overwritten");
        assertEquals("newB", ttlCache.get(2), "Existing key should be overwritten");
        assertEquals("newC", ttlCache.get(3), "New key should be added");
    }

    @Test
    void putAllAcceptsNullKeyAndNullValue() {
        // given
        sourceMap.put(null, "nullKey");
        sourceMap.put(5, null);
        // when
        ttlCache.putAll(sourceMap);
        // then
        assertEquals(2, ttlCache.size());
        assertEquals("nullKey", ttlCache.get(null), "Cache should store entry with null key");
        assertNull(ttlCache.get(5), "Cache should store entry with null value");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("normalOperationProvider")
    void putAllNormalOperations(String description, Map<String, Integer> source, int expectedSize, Map<String, Integer> expectedContent) {
        // when
        ttlCache.putAll(source);
        // then
        assertEquals(expectedSize, ttlCache.size(), () -> "Cache size mismatch for case: " + description);
        expectedContent.forEach((k, v) -> assertEquals(v, ttlCache.get(k), () -> "Incorrect value for key '" + k + "' in case: " + description));
    }

    /**
     * Verifies that {@code putAll(null)} complies with the {@link Map}
     * contract by throwing a {@link NullPointerException}.
     */
    @Test
    void putAllNullMapThrowsNPE() {
        assertThrows(NullPointerException.class, () -> ttlCache.putAll(null), "putAll(null) should throw NullPointerException");
    }
}
