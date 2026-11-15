package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.*;

@ExtendWith(MockitoExtension.class)
class ThreadedLRUCacheStrategy_values_Test {

    /**
     * Instance under test – created fresh for each test method.
     */
    private ThreadedLRUCacheStrategy<Integer, String> cache;

    /**
     * Initialise a new cache before each test.
     */
    @BeforeEach
    void setupBeforeEach() {
        // Capacity 100 and a short cleanup delay – values() does not depend on the cleanup task.
        cache = new ThreadedLRUCacheStrategy<>(100, 10);
    }

    /**
     * No per‑test teardown needed – a fresh cache is created for every test.
     */
    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the cache to aid GC and to avoid interference with the static scheduler.
        cache.clear();
    }

    /**
     * Shut down the shared scheduler after all tests have finished.
     */
    @AfterAll
    static void teardownAfterAll() {
        ThreadedLRUCacheStrategy.shutdown();
    }

    /**
     * Populate the cache with the supplied entries (key → value).
     */
    private void populateCache(List<Map.Entry<?, ?>> entries) {
        for (Map.Entry<?, ?> e : entries) {
            cache.put(e.getKey(), e.getValue());
        }
    }

    /**
     * Creates a simple immutable entry for test data.
     */
    private static Map.Entry<Object, Object> entry(Object k, Object v) {
        return new AbstractMap.SimpleImmutableEntry<>(k, v);
    }

    private static Stream<Arguments> unmodifiableOperationsProvider() {
        return Stream.of(Arguments.of("add", (Executable) () -> {
            Collection<Object> col = new ThreadedLRUCacheStrategyValuesTest().cache.values();
        }), // placeholder, will be replaced in test method
        // We'll construct the actions lazily inside the test method
        // Using lambda that captures the collection
        Arguments.of("add", (Executable) () -> {
            // no-op placeholder
        }));
    }

    /**
     * Verify that {@code values()} returns a collection containing all inserted values.
     */
    @Test
    void valuesReturnsAllCurrentValues() {
        // given
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
        // when
        Collection<String> values = cache.values();
        // then
        assertEquals(3, values.size(), "The collection should contain three elements.");
        // The order is not guaranteed – use a list for easier contains checks.
        List<String> expected = new ArrayList<>();
        Collections.addAll(expected, "one", "two", "three");
        assertTrue(values.containsAll(expected), "The collection should contain all inserted values.");
    }

    /**
     * Verify that the collection returned by {@code values()} is unmodifiable.
     */
    @Test
    void valuesCollectionIsUnmodifiable() {
        // given
        cache.put(1, "alpha");
        Collection<String> values = cache.values();
        // when / then
        assertThrows(UnsupportedOperationException.class, () -> values.add("should fail"), "The collection returned by values() must be unmodifiable.");
    }

    /**
     * Verify that the collection reflects modifications made to the cache after the call.
     */
    @Test
    void valuesReflectsCacheChanges() {
        // given
        cache.put(10, "ten");
        cache.put(20, "twenty");
        Collection<String> values = cache.values();
        // when
        // add a new entry
        cache.put(30, "thirty");
        // remove an existing entry
        cache.remove(10);
        // then
        // The collection is a snapshot of the cache at the time of the call,
        // so it should NOT contain the newly added value nor the removed one.
        assertTrue(values.contains("ten"));
        assertTrue(values.contains("twenty"));
        assertFalse(values.contains("thirty"));
        assertEquals(2, values.size(), "Snapshot should still have the original size.");
    }

    @Test
    void valuesReturnsEmptyUnmodifiableCollectionWhenCacheIsEmpty() {
        Collection<Object> values = cache.values();
        assertTrue(values.isEmpty(), "Expected an empty collection for an empty cache.");
        assertThrows(UnsupportedOperationException.class, () -> values.add("x"), "The collection must be unmodifiable.");
    }

    @Test
    void valuesContainsAllInsertedValues() {
        List<Map.Entry<Object, Object>> entries = List.of(entry("A", 1), entry("B", 2), entry("C", 3));
        populateCache(entries);
        Collection<Object> values = cache.values();
        assertEquals(3, values.size(), "Should contain three values.");
        assertTrue(values.containsAll(List.of(1, 2, 3)), "All inserted values must be present.");
    }

    @Test
    void valuesIncludesNullValue() {
        cache.put("X", null);
        Collection<Object> values = cache.values();
        assertEquals(1, values.size());
        assertTrue(values.contains(null), "The collection must contain the null value.");
    }

    @Test
    void valuesHandlesDuplicateValues() {
        cache.put("K1", "v1");
        // duplicate value, different key
        cache.put("K2", "v1");
        Collection<Object> values = cache.values();
        assertEquals(2, values.size(), "Both entries should be present even if values are equal.");
        long count = values.stream().filter(v -> Objects.equals(v, "v1")).count();
        assertEquals(2, count, "The value 'v1' must appear twice.");
    }

    @Test
    void valuesReflectsLatestValueWhenKeyIsOverwritten() {
        cache.put("temp", "value");
        // overwrite
        cache.put("temp", "newValue");
        Collection<Object> values = cache.values();
        assertEquals(1, values.size());
        assertTrue(values.contains("newValue"));
        assertFalse(values.contains("value"));
    }

    @Test
    void valuesHandlesNullKey() {
        cache.put(null, "nullKeyValue");
        cache.put("X", "xValue");
        Collection<Object> values = cache.values();
        assertEquals(2, values.size());
        assertTrue(values.containsAll(List.of("nullKeyValue", "xValue")));
    }

    @Test
    void valuesHandlesLargeNumberOfEntries() {
        // Insert 100 entries (0‑99)
        IntStream.range(0, 100).forEach(i -> cache.put(i, i));
        Collection<Object> values = cache.values();
        assertEquals(100, values.size(), "All 100 values must be present.");
        assertTrue(values.containsAll(IntStream.range(0, 100).boxed().collect(Collectors.toList())), "Every inserted value should be present.");
    }

    @Test
    void valuesIsSnapshotWhenLaterEntryIsAdded() {
        cache.put("A", 1);
        cache.put("B", 2);
        Collection<Object> snapshot = cache.values();
        // later addition
        cache.put("C", 3);
        assertEquals(2, snapshot.size());
        assertTrue(snapshot.containsAll(List.of(1, 2)));
        assertFalse(snapshot.contains(3), "Snapshot must not contain the later added value.");
    }

    @Test
    void valuesIsSnapshotWhenEntryIsRemoved() {
        cache.put("A", 1);
        cache.put("B", 2);
        cache.put("C", 3);
        Collection<Object> snapshot = cache.values();
        // removal after snapshot
        cache.remove("B");
        assertEquals(3, snapshot.size());
        assertTrue(snapshot.containsAll(List.of(1, 2, 3)));
        // The snapshot should still contain the removed value.
        assertTrue(snapshot.contains(2), "Snapshot must still contain the value that was removed from the cache.");
    }

    @Test
    void valuesIsSnapshotAcrossClearAndNewPut() {
        cache.put("A", 1);
        cache.put("B", 2);
        Collection<Object> snapshot = cache.values();
        cache.clear();
        cache.put("D", 4);
        assertEquals(2, snapshot.size());
        assertTrue(snapshot.containsAll(List.of(1, 2)));
        assertFalse(snapshot.contains(4), "Snapshot must not reflect new entries after clear.");
    }

    @Test
    void valuesReturnsJdkUnmodifiableCollectionImplementation() {
        Collection<Object> values = cache.values();
        // The concrete class name used by Collections.unmodifiableCollection()
        assertTrue(values.getClass().getName().contains("UnmodifiableCollection"), "Expected a JDK unmodifiable collection wrapper.");
    }

    @ParameterizedTest(name = "values collection should reject operation: {0}")
    @MethodSource("unmodifiableOperationsProvider")
    void valuesCollectionRejectsMutatingOperations(String operation, Executable action) {
        cache.put("A", 1);
        cache.put("B", 2);
        Collection<Object> values = cache.values();
        assertThrows(UnsupportedOperationException.class, action, "Operation '" + operation + "' must throw UnsupportedOperationException.");
    }

    // The above provider is only a formality; we will generate the real actions in the test body.
    @Test
    void valuesRejectsAllMutatingOperations() {
        cache.put("A", 1);
        cache.put("B", 2);
        Collection<Object> values = cache.values();
        List<Executable> mutatingOps = List.of(() -> values.add("x"), () -> values.remove("x"), () -> values.addAll(List.of("x", "y")), () -> values.removeAll(List.of("x")), () -> values.retainAll(List.of("x")), () -> values.clear(), () -> {
            Iterator<Object> it = values.iterator();
            if (it.hasNext()) {
                it.next();
                it.remove();
            }
        });
        for (Executable op : mutatingOps) {
            assertThrows(UnsupportedOperationException.class, op, "All mutating operations on the values collection must throw UnsupportedOperationException.");
        }
    }

    @Test
    void valuesThrowsNullPointerExceptionWhenInternalCacheIsNull() throws Exception {
        // Use reflection to set the private 'cache' field to null.
        Field cacheField = ThreadedLRUCacheStrategy.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        cacheField.set(cache, null);
        assertThrows(NullPointerException.class, () -> cache.values(), "Calling values() when internal cache is null should throw NullPointerException.");
    }
}
