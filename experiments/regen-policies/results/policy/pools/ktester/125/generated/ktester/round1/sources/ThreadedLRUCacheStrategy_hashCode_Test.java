package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

class ThreadedLRUCacheStrategy_hashCode_Test {

    private ThreadedLRUCacheStrategy<String, String> cache;

    @BeforeEach
    void setupBeforeEach() {
        // capacity = 10, cleanup delay = 100 ms (arbitrary safe values)
        cache = new ThreadedLRUCacheStrategy<>(10, 100);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the cache is cleared so that no background purge task interferes with the next test
        if (cache != null) {
            cache.clear();
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the shared scheduler used by all ThreadedLRUCacheStrategy instances
        ThreadedLRUCacheStrategy.shutdown();
    }

    /**
     * Supplies a stream of test data for branch‑coverage scenarios.
     * Each element is an array: {description, Map of key→value, expected hash calculation lambda}
     */
    static Stream<Arguments> branchCoverageProvider() {
        return Stream.of(Arguments.of("null key, null value", Collections.singletonMap(null, null), (IntUnaryOperator) hash -> {
            int h = 1;
            h = 31 * h + 0;
            h = 31 * h + 0;
            return h;
        }), Arguments.of("non‑null key, null value", Collections.singletonMap("key1", null), (IntUnaryOperator) hash -> {
            int h = 1;
            h = 31 * h + "key1".hashCode();
            h = 31 * h + 0;
            return h;
        }), Arguments.of("null key, non‑null value", Collections.singletonMap(null, "123"), (IntUnaryOperator) hash -> {
            int h = 1;
            h = 31 * h + 0;
            h = 31 * h + "123".hashCode();
            return h;
        }), Arguments.of("both non‑null", Collections.singletonMap("k", "v"), (IntUnaryOperator) hash -> {
            int h = 1;
            h = 31 * h + "k".hashCode();
            h = 31 * h + "v".hashCode();
            return h;
        }), Arguments.of("two entries", new LinkedHashMap<String, String>() {

            {
                put("a", "b");
                put("c", "d");
            }
        }, (IntUnaryOperator) hash -> {
            int h = 1;
            h = 31 * h + "a".hashCode();
            h = 31 * h + "b".hashCode();
            h = 31 * h + "c".hashCode();
            h = 31 * h + "d".hashCode();
            return h;
        }), Arguments.of("duplicate entries", new LinkedHashMap<String, String>() {

            {
                put("same", "same");
                // second put replaces the first, but we want two nodes.
                put("same", "same");
            }
        }, // To force two nodes we will use reflection later; expected hash assumes two nodes.
        (IntUnaryOperator) hash -> {
            int h = 1;
            h = 31 * h + "same".hashCode();
            h = 31 * h + "same".hashCode();
            h = 31 * h + "same".hashCode();
            h = 31 * h + "same".hashCode();
            return h;
        }));
    }

    /**
     * Helper to set the private field {@code cache} on the cache instance.
     */
    private void setInternalCache(Object newCache) throws Exception {
        Field cacheField = ThreadedLRUCacheStrategy.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        cacheField.set(cache, newCache);
    }

    @Test
    void hashCode_emptyCache_returnsInitialValue() {
        // GIVEN an empty cache (no puts performed)
        // WHEN
        int actualHash = cache.hashCode();
        // THEN
        // According to the implementation, the initial hashCode value is 1
        assertEquals(1, actualHash, "Hash code of an empty cache should be 1");
    }

    @Test
    void hashCode_singleEntry_computesCorrectHash() {
        // GIVEN a cache with a single entry ("key1" -> "value1")
        String key = "key1";
        String value = "value1";
        cache.put(key, value);
        // WHEN
        int actualHash = cache.hashCode();
        // THEN
        // Expected hash calculation follows the same algorithm used in ThreadedLRUCacheStrategy
        int expected = 1;
        expected = 31 * expected + (key == null ? 0 : key.hashCode());
        expected = 31 * expected + (value == null ? 0 : value.hashCode());
        assertEquals(expected, actualHash, "Hash code does not match expected calculation for a single entry");
    }

    @Test
    void hashCode_nullKeyAndValue_handledGracefully() {
        // GIVEN a cache containing a null key and a null value
        cache.put(null, null);
        // WHEN
        int actualHash = cache.hashCode();
        // THEN
        // Both key and value are null, contributing 0 each to the hash
        int expected = 1;
        // null key
        expected = 31 * expected + 0;
        // null value
        expected = 31 * expected + 0;
        assertEquals(expected, actualHash, "Hash code should correctly handle null key and null value");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("branchCoverageProvider")
    void hashCode_branchCoverage(String description, Map<String, String> entries, IntUnaryOperator expectedCalculator) throws Exception {
        // Populate the cache using the public API where possible.
        // For the "duplicate entries" case we need to inject a second node manually.
        if (description.equals("duplicate entries")) {
            // First, put a single entry normally.
            cache.put("same", "same");
            // Now use reflection to access the private ConcurrentMap cache and insert a second Node with the same key/value.
            Field cacheField = ThreadedLRUCacheStrategy.class.getDeclaredField("cache");
            cacheField.setAccessible(true);
            @SuppressWarnings("unchecked")
            ConcurrentHashMap<String, Object> internalMap = (ConcurrentHashMap<String, Object>) cacheField.get(cache);
            // Obtain the inner Node class via reflection.
            Class<?> nodeClass = Class.forName("com.cedarsoftware.util.cache.ThreadedLRUCacheStrategy$Node");
            Constructor<?> nodeCtor = nodeClass.getDeclaredConstructor(Object.class, Object.class);
            nodeCtor.setAccessible(true);
            Object secondNode = nodeCtor.newInstance("same", "same");
            // Insert under a distinct synthetic key to keep both nodes present.
            internalMap.put("duplicateSyntheticKey", secondNode);
        } else {
            // Normal population via public put()
            entries.forEach(cache::put);
        }
        // WHEN
        int actual = cache.hashCode();
        // THEN
        int expected = expectedCalculator.applyAsInt(0);
        assertEquals(expected, actual, "Unexpected hash for case: " + description);
    }

    @Test
    void hashCode_consistency_multipleCalls() {
        // GIVEN a cache with two entries
        cache.put("x", "y");
        cache.put("z", null);
        // WHEN
        int first = cache.hashCode();
        int second = cache.hashCode();
        // THEN
        assertEquals(first, second, "hashCode should be deterministic across multiple invocations");
    }

    @Test
    void hashCode_internalCacheIsNull_throwsNPE() throws Exception {
        // GIVEN the internal cache field set to null
        setInternalCache(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, cache::hashCode, "Calling hashCode when internal cache is null should throw NPE");
    }

    @Test
    void hashCode_nullNodeInCache_throwsNPE() throws Exception {
        // GIVEN a map that contains a null Node value
        ConcurrentHashMap<String, Object> map = new ConcurrentHashMap<>();
        // null Node
        map.put("key", null);
        setInternalCache(map);
        // WHEN / THEN
        assertThrows(NullPointerException.class, cache::hashCode, "Iterating over a null node should cause NPE");
    }

    @Test
    void hashCode_keyHashCodeThrows_throwsIllegalStateException() throws Exception {
        // GIVEN a key whose hashCode() throws IllegalStateException
        Object badKey = new Object() {

            @Override
            public int hashCode() {
                throw new IllegalStateException("bad key");
            }
        };
        cache.put(badKey, "value");
        // WHEN / THEN
        IllegalStateException ex = assertThrows(IllegalStateException.class, cache::hashCode, "Exception from key.hashCode() should propagate");
        assertEquals("bad key", ex.getMessage());
    }

    @Test
    void hashCode_valueHashCodeThrows_throwsIllegalArgumentException() throws Exception {
        // GIVEN a value whose hashCode() throws IllegalArgumentException
        Object badValue = new Object() {

            @Override
            public int hashCode() {
                throw new IllegalArgumentException("bad value");
            }
        };
        cache.put("key", badValue);
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, cache::hashCode, "Exception from value.hashCode() should propagate");
        assertEquals("bad value", ex.getMessage());
    }
}
