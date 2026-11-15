package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TTLCache_equals_Test {

    private TTLCache<Integer, String> cacheUnderTest;

    private TTLCache<Integer, String> anotherCache;

    @BeforeEach
    void setupBeforeEach() {
        // Use a short TTL so the cache does not interfere with equality logic
        cacheUnderTest = new TTLCache<>(10_000, -1);
        anotherCache = new TTLCache<>(10_000, -1);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure each cache is cleared so subsequent tests start fresh.
        if (cacheUnderTest != null) {
            cacheUnderTest.clear();
        }
        if (anotherCache != null) {
            anotherCache.clear();
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the shared background purge thread used by TTLCache.
        TTLCache.shutdown();
    }

    /**
     * Supplies the test data for {@link #equalsParameterizedScenarios}.
     */
    private static Stream<Arguments> equalsScenarios() {
        // Helper to create a TTLCache with a short TTL and unlimited size.
        java.util.function.Supplier<TTLCache<Object, Object>> newCache = () -> new TTLCache<>(10_000, -1);
        // 1. Empty cache vs empty HashMap – should be true.
        TTLCache<Object, Object> emptyCache = newCache.get();
        Map<Object, Object> emptyMap = new HashMap<>();
        // 2. Identical entries (a=1, b=2) – true.
        TTLCache<Object, Object> cacheIdentical = newCache.get();
        cacheIdentical.put("a", 1);
        cacheIdentical.put("b", 2);
        Map<Object, Object> mapIdentical = new HashMap<>();
        mapIdentical.put("a", 1);
        mapIdentical.put("b", 2);
        // 3. Different value for key "b" – false.
        TTLCache<Object, Object> cacheDiffValue = newCache.get();
        cacheDiffValue.put("a", 1);
        cacheDiffValue.put("b", 2);
        Map<Object, Object> mapDiffValue = new HashMap<>();
        mapDiffValue.put("a", 1);
        mapDiffValue.put("b", 99);
        // 4. Extra entry in the other map – false.
        TTLCache<Object, Object> cacheMissingEntry = newCache.get();
        cacheMissingEntry.put("a", 1);
        cacheMissingEntry.put("b", 2);
        Map<Object, Object> mapExtraEntry = new HashMap<>();
        mapExtraEntry.put("a", 1);
        mapExtraEntry.put("b", 2);
        mapExtraEntry.put("c", 3);
        // 5. Both contain null key and null value – true.
        TTLCache<Object, Object> cacheNullEntries = newCache.get();
        cacheNullEntries.put(null, null);
        cacheNullEntries.put("x", null);
        Map<Object, Object> mapNullEntries = new HashMap<>();
        mapNullEntries.put(null, null);
        mapNullEntries.put("x", null);
        // 6. Cache has two null entries, other map only one – false.
        TTLCache<Object, Object> cacheMoreNulls = newCache.get();
        cacheMoreNulls.put(null, null);
        cacheMoreNulls.put("x", null);
        Map<Object, Object> mapFewerNulls = new HashMap<>();
        mapFewerNulls.put(null, null);
        return Stream.of(Arguments.of("Empty cache vs empty HashMap", emptyCache, emptyMap, true), Arguments.of("Cache vs identical HashMap (a=1, b=2)", cacheIdentical, mapIdentical, true), Arguments.of("Cache vs HashMap with different value for key \"b\"", cacheDiffValue, mapDiffValue, false), Arguments.of("Cache vs HashMap with an extra entry (c=3)", cacheMissingEntry, mapExtraEntry, false), Arguments.of("Cache with null key/value equals identical HashMap", cacheNullEntries, mapNullEntries, true), Arguments.of("Cache with null entries not equal to map missing one entry", cacheMoreNulls, mapFewerNulls, false));
    }

    @Test
    void equalsSameInstance() {
        assertTrue(cacheUnderTest.equals(cacheUnderTest), "A cache should be equal to itself");
    }

    @Test
    void equalsAnotherCacheWithSameEntries() {
        cacheUnderTest.put(1, "A");
        cacheUnderTest.put(2, "B");
        anotherCache.put(1, "A");
        anotherCache.put(2, "B");
        assertTrue(cacheUnderTest.equals(anotherCache), "Two TTLCache instances containing the same entries should be equal");
    }

    @Test
    void equalsMapWithSameEntries() {
        cacheUnderTest.put(10, "X");
        cacheUnderTest.put(20, "Y");
        Map<Integer, String> plainMap = new HashMap<>();
        plainMap.put(10, "X");
        plainMap.put(20, "Y");
        assertTrue(cacheUnderTest.equals(plainMap), "TTLCache should be equal to any Map with an identical entry set");
    }

    @Test
    void notEqualsWhenEntriesDiffer() {
        cacheUnderTest.put(1, "A");
        cacheUnderTest.put(2, "B");
        anotherCache.put(1, "A");
        // different value
        anotherCache.put(2, "C");
        assertFalse(cacheUnderTest.equals(anotherCache), "Caches with different entries must not be equal");
    }

    @Test
    void notEqualsWhenOtherIsNotMap() {
        Object notAMap = "I am just a String";
        assertFalse(cacheUnderTest.equals(notAMap), "TTLCache should not be equal to an object that does not implement Map");
    }

    @Test
    void equalsHandlesNull() {
        assertFalse(cacheUnderTest.equals(null), "TTLCache should not be equal to null");
    }

    @Test
    void equalsEmptyCaches() {
        // both caches are empty
        assertTrue(cacheUnderTest.equals(anotherCache), "Two empty TTLCache instances should be equal");
    }

    /**
     * Parameterised test that drives all remaining scenarios described in the
     * JSON test specification.  Each {@link Arguments} instance contains:
     *
     * <ul>
     *   <li>a human‑readable description (used as the test name)</li>
     *   <li>a {@link TTLCache} pre‑populated with the required entries</li>
     *   <li>a {@link Map} to compare against</li>
     *   <li>the expected boolean result of {@code cache.equals(map)}</li>
     * </ul>
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("equalsScenarios")
    void equalsParameterizedScenarios(String description, TTLCache<Object, Object> cache, Map<Object, Object> other, boolean expected) {
        assertEquals(expected, cache.equals(other), description);
    }
}
