package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TTLCache_equals_Test {

    private TTLCache<Integer, String> cacheUnderTest;

    // a long TTL to avoid expiration during tests
    private static final long TTL = 10_000L;

    @BeforeAll
    static void setupBeforeAll() {
        // Ensure any background threads from previous tests are stopped
        TTLCache.shutdown();
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh cache for each test case
        cacheUnderTest = new TTLCache<>(TTL, -1);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the cache to avoid cross‑test contamination
        if (cacheUnderTest != null) {
            cacheUnderTest.clear();
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the shared purge‑task thread pool once all tests have run
        TTLCache.shutdown();
    }

    private static Stream<Object> nonMapArguments() {
        return Stream.of("just a string", new Object());
    }

    private static Stream<Arguments> standardMapScenarios() {
        Map<String, Integer> same = new HashMap<>();
        same.put("a", 1);
        same.put("b", 2);
        Map<String, Integer> diffValue = new HashMap<>();
        diffValue.put("a", 1);
        diffValue.put("b", 3);
        Map<String, Integer> missingEntry = new HashMap<>();
        missingEntry.put("a", 1);
        return Stream.of(Arguments.of(same, true), Arguments.of(diffValue, false), Arguments.of(missingEntry, false));
    }

    private static Stream<Arguments> ttlCacheScenarios() {
        return Stream.of(// same entries
        Arguments.of(new TTLCache<String, Integer>(TTL, -1), true), // different entries
        Arguments.of(new TTLCache<String, Integer>(TTL, -1), false));
    }

    @Test
    void equalsSameInstanceReturnsTrue() {
        // The argument refers to the same TTLCache instance (this == o).
        assertTrue(cacheUnderTest.equals(cacheUnderTest), "A cache should be equal to itself");
    }

    @Test
    void equalsNullReturnsFalse() {
        // The argument is null – the instanceof check fails.
        assertFalse(cacheUnderTest.equals(null), "A cache should not be equal to null");
    }

    @Test
    void equalsDifferentTypeReturnsFalse() {
        Map<Integer, String> other = new HashMap<>();
        other.put(1, "A");
        assertFalse(cacheUnderTest.equals(other), "A cache should not be equal to an object that is not a Map");
    }

    @Test
    void equalsEmptyCachesAreEqual() {
        TTLCache<Integer, String> another = new TTLCache<>(TTL, -1);
        assertTrue(cacheUnderTest.equals(another), "Two empty caches should be equal");
        // clean up its background thread
        another.shutdown();
    }

    @Test
    void equalsCachesWithSameEntriesAreEqual() {
        TTLCache<Integer, String> another = new TTLCache<>(TTL, -1);
        cacheUnderTest.put(1, "A");
        cacheUnderTest.put(2, "B");
        another.put(1, "A");
        another.put(2, "B");
        assertTrue(cacheUnderTest.equals(another), "Caches containing identical entries should be equal");
        another.shutdown();
    }

    @Test
    void equalsCachesWithDifferentEntriesAreNotEqual() {
        TTLCache<Integer, String> another = new TTLCache<>(TTL, -1);
        cacheUnderTest.put(1, "A");
        cacheUnderTest.put(2, "B");
        another.put(1, "A");
        // different value for key 2
        another.put(2, "Different");
        assertFalse(cacheUnderTest.equals(another), "Caches with differing entry values should not be equal");
        another.shutdown();
    }

    @Test
    void equalsAgainstStandardMapWithSameEntries() {
        Map<Integer, String> standardMap = new HashMap<>();
        standardMap.put(1, "A");
        standardMap.put(2, "B");
        cacheUnderTest.put(1, "A");
        cacheUnderTest.put(2, "B");
        assertTrue(cacheUnderTest.equals(standardMap), "TTLCache should be equal to any Map with the same entry set");
    }

    @Test
    void equalsIsSymmetric() {
        TTLCache<Integer, String> another = new TTLCache<>(TTL, -1);
        cacheUnderTest.put(1, "A");
        another.put(1, "A");
        assertTrue(cacheUnderTest.equals(another) && another.equals(cacheUnderTest), "Equality should be symmetric");
        another.shutdown();
    }

    @Test
    void equalsIsConsistentAfterModification() {
        TTLCache<String, Integer> another = new TTLCache<>(TTL, -1);
        cacheUnderTest.put("k", 1);
        another.put("k", 1);
        assertTrue(cacheUnderTest.equals(another), "Caches should be equal initially");
        // Modify one cache – equality must reflect the change.
        cacheUnderTest.put("new", 2);
        assertFalse(cacheUnderTest.equals(another), "After modification, caches should no longer be equal");
        another.shutdown();
    }

    @ParameterizedTest(name = "[{index}] non‑Map argument: {0}")
    @MethodSource("nonMapArguments")
    void equalsNonMapReturnsFalse(Object nonMap) {
        // Objects that do not implement Map must cause equals to return false.
        assertFalse(cacheUnderTest.equals(nonMap), "equals should return false for non‑Map argument: " + nonMap);
    }

    @ParameterizedTest(name = "[{index}] compare with Map: {1}")
    @MethodSource("standardMapScenarios")
    void equalsStandardMapComparison(Map<String, Integer> otherMap, boolean expected) {
        // Populate the cache with a fixed set of entries.
        cacheUnderTest.put("a", 1);
        cacheUnderTest.put("b", 2);
        assertEquals(expected, cacheUnderTest.equals(otherMap), "Comparison with standard Map yielded unexpected result");
    }

    @ParameterizedTest(name = "[{index}] compare with other TTLCache: {1}")
    @MethodSource("ttlCacheScenarios")
    void equalsOtherTTLCacheComparison(TTLCache<String, Integer> otherCache, boolean expected) {
        // Populate the primary cache.
        cacheUnderTest.put("x", 100);
        cacheUnderTest.put("y", 200);
        // Populate the other cache according to the scenario.
        if (expected) {
            // Same entries
            otherCache.put("x", 100);
            otherCache.put("y", 200);
        } else {
            // Different entries
            otherCache.put("x", 100);
            otherCache.put("z", 300);
        }
        assertEquals(expected, cacheUnderTest.equals(otherCache), "TTLCache vs TTLCache equality mismatch");
        // Clean up the auxiliary cache's background thread.
        otherCache.shutdown();
    }

    @Test
    void equalsEmptyStandardMapReturnsTrue() {
        // Both cache and map are empty.
        Map<String, Integer> emptyMap = new HashMap<>();
        assertTrue(cacheUnderTest.equals(emptyMap), "Empty TTLCache should be equal to an empty standard Map");
    }

    @Test
    void equalsEmptyTTLCacheReturnsTrue() {
        // Compare two empty TTLCache instances.
        TTLCache<String, Integer> anotherEmpty = new TTLCache<>(TTL, -1);
        assertTrue(cacheUnderTest.equals(anotherEmpty), "Two empty TTLCache instances should be equal");
        anotherEmpty.shutdown();
    }
}
