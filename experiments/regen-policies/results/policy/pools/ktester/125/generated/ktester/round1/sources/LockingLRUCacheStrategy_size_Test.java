package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

class LockingLRUCacheStrategy_size_Test {

    /**
     * The cache instance used in each test.
     */
    private LockingLRUCacheStrategy<String, Integer> cache;

    /**
     * Capacity used for the cache – any positive number works for size tests.
     */
    private static final int CAPACITY = 10;

    /**
     * Default capacity – sufficient for most size‑related tests.
     */
    private static final int DEFAULT_CAPACITY = 10;

    /**
     * Data holder for a single test case.
     */
    private static class SizeTestCase {

        final String description;

        // list of {"key":..., "value":...}
        final List<Map<String, Object>> putOps;

        final List<String> removeOps;

        // map of key->value
        final Map<String, Object> putAllOp;

        final boolean clearOp;

        // custom capacity, null = default
        final Integer maxSize;

        // key to remove via iterator
        final String iteratorRemoveKey;

        final Class<? extends Throwable> expectedException;

        final int expectedSize;

        SizeTestCase(String description, List<Map<String, Object>> putOps, List<String> removeOps, Map<String, Object> putAllOp, boolean clearOp, Integer maxSize, String iteratorRemoveKey, Class<? extends Throwable> expectedException, int expectedSize) {
            this.description = description;
            this.putOps = putOps;
            this.removeOps = removeOps;
            this.putAllOp = putAllOp;
            this.clearOp = clearOp;
            this.maxSize = maxSize;
            this.iteratorRemoveKey = iteratorRemoveKey;
            this.expectedException = expectedException;
            this.expectedSize = expectedSize;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh cache before each test to guarantee isolation.
        cache = new LockingLRUCacheStrategy<>(CAPACITY);
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the cache and drop the reference.
        if (cache != null) {
            cache.clear();
        }
        cache = null;
    }

    /**
     * Supplies the parameterised test data derived from the JSON specification.
     */
    private static Stream<SizeTestCase> sizeTestProvider() {
        return Stream.of(// 1. Newly created cache – already covered, but kept for completeness
        new SizeTestCase("Cache is newly created and empty; size() should return 0.", Collections.emptyList(), Collections.emptyList(), null, false, null, null, null, 0), // 2. Three distinct puts
        new SizeTestCase("After inserting three distinct entries, size() should return 3.", List.of(mapOf("key", "a", "value", 1), mapOf("key", "b", "value", 2), mapOf("key", "c", "value", 3)), Collections.emptyList(), null, false, null, null, null, 3), // 3. Duplicate key replacement
        new SizeTestCase("Putting a duplicate key replaces the existing entry; size() remains 1.", List.of(mapOf("key", "x", "value", "foo"), mapOf("key", "x", "value", "bar")), Collections.emptyList(), null, false, null, null, null, 1), // 4. Add two, remove one
        new SizeTestCase("After adding two entries and removing one, size() should reflect the remaining entry.", List.of(mapOf("key", "k1", "value", "v1"), mapOf("key", "k2", "value", "v2")), List.of("k1"), null, false, null, null, null, 1), // 5. Null value accepted
        new SizeTestCase("Cache should accept null values; size() counts the entry.", List.of(mapOf("key", "p", "value", null)), Collections.emptyList(), null, false, null, null, null, 1), // 6. clear() operation
        new SizeTestCase("Calling clear() empties the cache; size() returns 0.", Collections.emptyList(), Collections.emptyList(), null, true, null, null, null, 0), // 7. putAll with four distinct entries
        new SizeTestCase("putAll with four distinct entries; size() should equal the number of unique keys inserted.", Collections.emptyList(), Collections.emptyList(), mapOf("a", 1, "b", 2, "c", 3, "d", 4), false, null, null, null, 4), // 8. putAll with duplicate key
        new SizeTestCase("putAll contains a duplicate key ('x'); later value overrides earlier, size reflects three unique keys.", Collections.emptyList(), Collections.emptyList(), mapOf("x", "first", "y", "second", "x", "override", "z", "third"), false, null, null, null, 3), // 9. Remove non‑existent key
        new SizeTestCase("Attempt to remove a key that is not present; size() should remain unchanged.", List.of(mapOf("key", "k1", "value", "v1"), mapOf("key", "k2", "value", "v2")), List.of("nonexistentKey"), null, false, null, null, null, 2), // 10. Capacity eviction (maxSize = 2)
        new SizeTestCase("Cache is configured with a maximum size of 2; after inserting three distinct entries the LRU entry is evicted, so size() equals the capacity.", List.of(mapOf("key", "one", "value", 1), mapOf("key", "two", "value", 2), mapOf("key", "three", "value", 3)), Collections.emptyList(), null, false, 2, null, null, 2), // 11. clear then add two entries
        new SizeTestCase("Clear the cache first, then add two entries; size() should reflect the newly added entries.", List.of(mapOf("key", "alpha", "value", "A"), mapOf("key", "beta", "value", "B")), Collections.emptyList(), null, true, null, null, null, 2), // 12. iterator remove
        new SizeTestCase("After inserting three entries, remove one via the iterator returned by entrySet(); size() should decrement accordingly.", List.of(mapOf("key", "p1", "value", 10), mapOf("key", "p2", "value", 20), mapOf("key", "p3", "value", 30)), Collections.emptyList(), null, false, null, "p2", null, 2), // 13. Null key should throw NPE (as per test spec)
        new SizeTestCase("Putting a null key is not allowed; the operation should throw NullPointerException and size() remains unchanged (still 0).", List.of(mapOf("key", null, "value", "nullKeyValue")), Collections.emptyList(), null, false, null, null, NullPointerException.class, 0), // 14. Internal cache null via reflection – expect NPE on size()
        new SizeTestCase("Calling size() when the internal cache reference is null (simulated via reflection) should throw NullPointerException.", Collections.emptyList(), Collections.emptyList(), null, false, null, null, NullPointerException.class, // size is irrelevant because exception is expected
        0));
    }

    @Test
    @DisplayName("size() returns 0 for a newly created cache")
    void sizeIsZeroWhenEmpty() {
        assertEquals(0, cache.size(), "Newly created cache must be empty");
    }

    @Test
    @DisplayName("size() reflects the number of entries after puts")
    void sizeReflectsNumberOfPuts() {
        cache.put("one", 1);
        cache.put("two", 2);
        cache.put("three", 3);
        assertEquals(3, cache.size(), "Cache size should match number of inserted entries");
    }

    @Test
    @DisplayName("size() decreases after removing an entry")
    void sizeDecreasesAfterRemove() {
        cache.put("a", 10);
        cache.put("b", 20);
        assertEquals(2, cache.size(), "Pre‑condition: two entries added");
        cache.remove("a");
        assertEquals(1, cache.size(), "Size should decrement after removal");
    }

    @Test
    @DisplayName("size() becomes zero after clear()")
    void sizeBecomesZeroAfterClear() {
        cache.put("x", 100);
        cache.put("y", 200);
        assertEquals(2, cache.size(), "Pre‑condition: two entries added");
        cache.clear();
        assertEquals(0, cache.size(), "Cache should be empty after clear()");
    }

    @Test
    @DisplayName("size() counts null keys and values correctly")
    void sizeCountsNullKeyAndValue() {
        cache.put(null, null);
        cache.put("notNull", null);
        // overwrites the previous null‑key entry
        cache.put(null, 42);
        // After the overwrite we have two distinct keys: null and "notNull"
        assertEquals(2, cache.size(), "Cache must count distinct keys, even if values are null");
    }

    /**
     * Helper to build a {@link Map} literal in a concise way.
     */
    @SafeVarargs
    private static <K, V> Map<K, V> mapOf(Object... entries) {
        if (entries.length % 2 != 0) {
            throw new IllegalArgumentException("Even number of arguments required");
        }
        Map<K, V> map = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            @SuppressWarnings("unchecked")
            K key = (K) entries[i];
            @SuppressWarnings("unchecked")
            V value = (V) entries[i + 1];
            map.put(key, value);
        }
        return map;
    }

    /**
     * Executes the parameterised test cases.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("sizeTestProvider")
    void sizeMethodComprehensiveTests(SizeTestCase tc) throws Exception {
        // If a custom capacity is required, recreate the cache with that capacity.
        if (tc.maxSize != null) {
            cache = new LockingLRUCacheStrategy<>(tc.maxSize);
        }
        // ---------- putAll ----------
        if (tc.putAllOp != null) {
            // The generic type of the cache is <String,Integer>, but putAll may contain
            // values of other types (e.g., String). Cast safely for the test purpose.
            @SuppressWarnings("unchecked")
            Map<String, Integer> map = (Map<String, Integer>) tc.putAllOp;
            cache.putAll(map);
        }
        // ---------- put operations ----------
        for (Map<String, Object> op : tc.putOps) {
            @SuppressWarnings("unchecked")
            String key = (String) op.get("key");
            @SuppressWarnings("unchecked")
            Integer value = (Integer) op.get("value");
            if (tc.expectedException != null && tc.expectedException == NullPointerException.class && key == null) {
                // Expect NPE for null key case – assertThrows and exit early.
                assertThrows(NullPointerException.class, () -> cache.put(key, value), tc.description + " – expected NullPointerException on put(null, …)");
                // After exception the cache should still be empty.
                assertEquals(0, cache.size(), "Cache size must remain 0 after failed null‑key insertion");
                return;
            }
            cache.put(key, value);
        }
        // ---------- clear operation ----------
        if (tc.clearOp) {
            cache.clear();
        }
        // ---------- remove operations ----------
        for (String keyToRemove : tc.removeOps) {
            cache.remove(keyToRemove);
        }
        // ---------- iterator remove ----------
        if (tc.iteratorRemoveKey != null) {
            Iterator<Map.Entry<String, Integer>> it = cache.entrySet().iterator();
            boolean removed = false;
            while (it.hasNext()) {
                Map.Entry<String, Integer> entry = it.next();
                if (tc.iteratorRemoveKey.equals(entry.getKey())) {
                    // iterator‑based removal
                    it.remove();
                    removed = true;
                    break;
                }
            }
            assertTrue(removed, "Iterator should have found key '" + tc.iteratorRemoveKey + "' to remove");
        }
        // ---------- internal cache null simulation ----------
        if (tc.description.contains("internal cache reference is null")) {
            // Use reflection to set the private field 'cache' to null.
            Field cacheField = LockingLRUCacheStrategy.class.getDeclaredField("cache");
            cacheField.setAccessible(true);
            cacheField.set(cache, null);
        }
        // ---------- expectation handling ----------
        if (tc.expectedException != null) {
            // When an exception is expected we invoke size() inside assertThrows.
            assertThrows(tc.expectedException, () -> cache.size(), tc.description + " – expected exception on size()");
        } else {
            // Normal case – verify the size matches expectation.
            assertEquals(tc.expectedSize, cache.size(), tc.description + " – size() returned unexpected value");
        }
    }

    @Test
    @DisplayName("size() after iterator.remove()")
    void sizeAfterIteratorRemove() {
        cache.put("p1", 10);
        cache.put("p2", 20);
        cache.put("p3", 30);
        assertEquals(3, cache.size(), "Pre‑condition: three entries added");
        Iterator<Map.Entry<String, Integer>> it = cache.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Integer> e = it.next();
            if ("p2".equals(e.getKey())) {
                it.remove();
                break;
            }
        }
        assertEquals(2, cache.size(), "Size should decrement after iterator.remove()");
        assertFalse(cache.containsKey("p2"), "Removed key must no longer be present");
    }

    @Test
    @DisplayName("size() throws NullPointerException when internal cache is null")
    void sizeThrowsWhenInternalCacheNull() throws Exception {
        // Create a cache, then nullify its internal map using reflection.
        Field cacheField = LockingLRUCacheStrategy.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        cacheField.set(cache, null);
        assertThrows(NullPointerException.class, () -> cache.size(), "Calling size() with a null internal cache should throw NPE");
    }
}
