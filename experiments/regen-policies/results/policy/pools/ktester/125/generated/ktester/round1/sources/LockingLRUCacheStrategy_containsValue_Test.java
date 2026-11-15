package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LockingLRUCacheStrategy_containsValue_Test {

    private static final int CAPACITY = 5;

    private LockingLRUCacheStrategy<Integer, String> cache;

    /**
     * Simple POJO used for the EqualityCheckCache test.
     * Equality is based solely on the {@code id} field.
     */
    private static final class CustomObject {

        private final int id;

        CustomObject(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof CustomObject))
                return false;
            CustomObject that = (CustomObject) o;
            return this.id == that.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return "CustomObject{id=" + id + '}';
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh cache for each test to guarantee isolation.
        // The default population matches the original test suite.
        cache = new LockingLRUCacheStrategy<>(CAPACITY);
        cache.put(1, "one");
        cache.put(2, "two");
        // test handling of null values
        cache.put(3, null);
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the cache – helps the GC and makes the intent clear
        if (cache != null) {
            cache.clear();
            cache = null;
        }
    }

    static Stream<Arguments> emptyCacheProvider() {
        return Stream.of(Arguments.of(new Object(), false, "When the cache is empty, containsValue should return false for any searched value."));
    }

    static Stream<Arguments> singleElementCacheProvider() {
        return Stream.of(Arguments.of("value1", true, "Cache contains a single entry (key1->\"value1\"). containsValue should return true when searching for the exact stored value."), Arguments.of("different", false, "Cache contains a single entry (key1->\"value1\"). containsValue should return false for a value that is not present."));
    }

    static Stream<Arguments> multipleElementsCacheProvider() {
        return Stream.of(Arguments.of("headValue", true, "Cache has three entries (k1->\"headValue\", k2->\"midValue\", k3->\"tailValue\"). Searching for the value at the head of the list should return true."), Arguments.of("midValue", true, "Cache has three entries (k1->\"headValue\", k2->\"midValue\", k3->\"tailValue\"). Searching for the middle value should return true."), Arguments.of("tailValue", true, "Cache has three entries (k1->\"headValue\", k2->\"midValue\", k3->\"tailValue\"). Searching for the value stored in the last node before the tail sentinel should return true."), Arguments.of("absent", false, "Cache has three entries, none of which match \"absent\". containsValue should return false."));
    }

    static Stream<Arguments> nullValueCacheProvider() {
        return Stream.of(Arguments.of(null, true, "Cache contains an entry with a null value (keyNull->null). Searching for null should return true."), Arguments.of("nonNull", false, "Cache contains an entry with a null value. Searching for a non‑null value should return false."));
    }

    static Stream<Arguments> duplicateValuesCacheProvider() {
        return Stream.of(Arguments.of("dup", true, "Two different keys map to the same value \"dup\". containsValue should return true as soon as it finds the first match."));
    }

    static Stream<Arguments> equalityCheckCacheProvider() {
        return Stream.of(Arguments.of(new CustomObject(42), true, "Cache stores a CustomObject instance with id=42. The searched value is a different CustomObject instance with the same id. Because CustomObject overrides equals, containsValue should return true."));
    }

    static Stream<Arguments> afterRemovalCacheProvider() {
        return Stream.of(Arguments.of("toBeRemoved", false, "An entry (keyRem->\"toBeRemoved\") is added then removed. After removal, containsValue should no longer find the value and return false."));
    }

    static Stream<Arguments> largeCachePerformanceProvider() {
        return Stream.of(Arguments.of("value500", true, "Cache is filled with 1000 distinct entries (value0 … value999). The searched value is located near the middle (value500). containsValue should correctly iterate through many nodes and return true."));
    }

    @Test
    void containsValue_returnsTrueWhenValueExists() {
        // given – cache already contains "one" and "two"
        // when
        boolean resultOne = cache.containsValue("one");
        boolean resultTwo = cache.containsValue("two");
        // then
        assertTrue(resultOne, "Cache should report that it contains value \"one\"");
        assertTrue(resultTwo, "Cache should report that it contains value \"two\"");
    }

    @Test
    void containsValue_returnsFalseWhenValueDoesNotExist() {
        // given – cache does not contain "three"
        // when
        boolean result = cache.containsValue("three");
        // then
        assertFalse(result, "Cache should report that it does not contain value \"three\"");
    }

    @Test
    void containsValue_correctlyHandlesNullValue() {
        // given – a null value was inserted for key 3
        // when
        boolean result = cache.containsValue(null);
        // then
        assertTrue(result, "Cache should report that it contains a null value");
    }

    @Test
    void containsValue_returnsFalseAfterValueRemoved() {
        // given – remove the entry with value "two"
        cache.remove(2);
        // when
        boolean result = cache.containsValue("two");
        // then
        assertFalse(result, "Cache should not contain value \"two\" after it was removed");
    }

    @Test
    void containsValue_isThreadSafeUnderConcurrentAccess() throws InterruptedException {
        // This test exercises the lock used inside containsValue by calling it from
        // multiple threads simultaneously. The exact ordering is nondeterministic,
        // but we assert that the method never throws and always returns a consistent
        // result for the same snapshot of the cache.
        final int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        boolean[] results = new boolean[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            threads[i] = new Thread(() -> {
                // each thread queries for a value that is present
                results[idx] = cache.containsValue("one");
            });
        }
        // start all threads
        for (Thread t : threads) {
            t.start();
        }
        // wait for completion
        for (Thread t : threads) {
            t.join();
        }
        // every thread should have observed the value
        for (int i = 0; i < threadCount; i++) {
            assertTrue(results[i], "All concurrent calls to containsValue should return true for existing value");
        }
    }

    // ----------------------------------------------------------------------
    @ParameterizedTest(name = "[EmptyCache] {2}")
    @MethodSource("emptyCacheProvider")
    void containsValue_emptyCache(Object value, boolean expected, String description) {
        // Re‑initialise an empty cache (capacity does not matter here)
        // ensure empty
        cache.clear();
        assertEquals(expected, cache.containsValue(value), description);
    }

    // ----------------------------------------------------------------------
    @ParameterizedTest(name = "[SingleElementCache] {2}")
    @MethodSource("singleElementCacheProvider")
    void containsValue_singleElementCache(Object value, boolean expected, String description) {
        // Build a cache that contains exactly one mapping: 10 -> "value1"
        cache.clear();
        cache.put(10, "value1");
        assertEquals(expected, cache.containsValue(value), description);
    }

    // ----------------------------------------------------------------------
    @ParameterizedTest(name = "[MultipleElementsCache] {2}")
    @MethodSource("multipleElementsCacheProvider")
    void containsValue_multipleElementsCache(Object value, boolean expected, String description) {
        // Populate three distinct entries
        cache.clear();
        cache.put(1, "headValue");
        cache.put(2, "midValue");
        cache.put(3, "tailValue");
        assertEquals(expected, cache.containsValue(value), description);
    }

    // ----------------------------------------------------------------------
    @ParameterizedTest(name = "[NullValueCache] {2}")
    @MethodSource("nullValueCacheProvider")
    void containsValue_nullValueCache(Object value, boolean expected, String description) {
        cache.clear();
        // only a null value
        cache.put(99, null);
        assertEquals(expected, cache.containsValue(value), description);
    }

    // ----------------------------------------------------------------------
    @ParameterizedTest(name = "[DuplicateValuesCache] {2}")
    @MethodSource("duplicateValuesCacheProvider")
    void containsValue_duplicateValuesCache(Object value, boolean expected, String description) {
        cache.clear();
        cache.put(1, "dup");
        // two different keys, same value
        cache.put(2, "dup");
        assertEquals(expected, cache.containsValue(value), description);
    }

    // ----------------------------------------------------------------------
    @ParameterizedTest(name = "[EqualityCheckCache] {2}")
    @MethodSource("equalityCheckCacheProvider")
    void containsValue_equalityCheckCache(Object value, boolean expected, String description) {
        cache.clear();
        cache.put(1, new CustomObject(42));
        // 'value' is a different instance with the same id
        assertEquals(expected, cache.containsValue(value), description);
    }

    // ----------------------------------------------------------------------
    @ParameterizedTest(name = "[AfterRemovalCache] {2}")
    @MethodSource("afterRemovalCacheProvider")
    void containsValue_afterRemovalCache(Object value, boolean expected, String description) {
        cache.clear();
        cache.put(1, "toBeRemoved");
        // remove before checking
        cache.remove(1);
        assertEquals(expected, cache.containsValue(value), description);
    }

    // ----------------------------------------------------------------------
    @ParameterizedTest(name = "[LargeCachePerformance] {2}")
    @MethodSource("largeCachePerformanceProvider")
    void containsValue_largeCachePerformance(Object value, boolean expected, String description) {
        // Use a larger capacity to avoid premature eviction
        cache = new LockingLRUCacheStrategy<>(2000);
        for (int i = 0; i < 1000; i++) {
            cache.put(i, "value" + i);
        }
        assertEquals(expected, cache.containsValue(value), description);
    }
}
