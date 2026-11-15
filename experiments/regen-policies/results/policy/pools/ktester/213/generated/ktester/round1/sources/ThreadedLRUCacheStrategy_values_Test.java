package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ThreadedLRUCacheStrategy_values_Test {

    /**
     * Cache instance used in each test.
     */
    private ThreadedLRUCacheStrategy<String, Integer> cache;

    /**
     * Sample data used to populate the cache.
     */
    private static final List<String> KEYS = List.of("one", "two", "three");

    private static final List<Integer> VALUES = List.of(1, 2, 3);

    @BeforeEach
    void setupBeforeEach() {
        // capacity = 10, cleanup delay = 100 ms (arbitrary, safe values)
        cache = new ThreadedLRUCacheStrategy<>(10, 100);
        // Populate with deterministic data for the default fixture.
        for (int i = 0; i < KEYS.size(); i++) {
            cache.put(KEYS.get(i), VALUES.get(i));
        }
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the cache is cleared between tests to avoid cross‑test contamination.
        cache.clear();
    }

    private static Stream<Arguments> unsupportedCollectionOperations() {
        return Stream.of(Arguments.of("remove", (Runnable) () -> {
            Collection<Integer> c = new ThreadedLRUCacheStrategy<>(5, 50).values();
            c.remove(1);
        }), Arguments.of("clear", (Runnable) () -> {
            Collection<Integer> c = new ThreadedLRUCacheStrategy<>(5, 50).values();
            c.clear();
        }), Arguments.of("addAll", (Runnable) () -> {
            Collection<Integer> c = new ThreadedLRUCacheStrategy<>(5, 50).values();
            c.addAll(Arrays.asList(1, 2));
        }), Arguments.of("removeAll", (Runnable) () -> {
            Collection<Integer> c = new ThreadedLRUCacheStrategy<>(5, 50).values();
            c.removeAll(Collections.singleton(1));
        }), Arguments.of("retainAll", (Runnable) () -> {
            Collection<Integer> c = new ThreadedLRUCacheStrategy<>(5, 50).values();
            c.retainAll(Collections.emptySet());
        }));
    }

    @Test
    void values_returnsAllValuesInUnmodifiableCollection() {
        // When
        Collection<Integer> returned = cache.values();
        // Then
        assertNotNull(returned, "values() should never return null");
        // The collection must contain exactly the values we inserted.
        assertEquals(VALUES.size(), returned.size(), "Returned collection size should match number of inserted entries");
        // Order is not guaranteed because the underlying map is concurrent;
        // we therefore compare contents as a bag.
        List<Integer> expected = new ArrayList<>(VALUES);
        List<Integer> actual = new ArrayList<>(returned);
        Collections.sort(expected);
        Collections.sort(actual);
        assertEquals(expected, actual, "Returned collection should contain all inserted values");
        // Verify that the collection is unmodifiable.
        assertThrows(UnsupportedOperationException.class, () -> returned.add(999), "Returned collection must be unmodifiable");
    }

    @Test
    void values_reflectsUpdatesToCache() {
        // Given the cache already contains three entries (setupBeforeEach)
        // When we add a new entry and remove an existing one
        cache.put("four", 4);
        cache.remove("one");
        // Then the values collection should reflect the current state.
        Collection<Integer> values = cache.values();
        assertEquals(3, values.size(), "Cache should now contain three entries");
        // Build the expected set of values after the mutation.
        List<Integer> expected = List.of(2, 3, 4);
        List<Integer> actual = new ArrayList<>(values);
        Collections.sort(expected);
        Collections.sort(actual);
        assertEquals(expected, actual, "Values collection should reflect recent modifications");
    }

    @Test
    void values_onNewlyCreatedCache_returnsEmptyUnmodifiableCollection() {
        ThreadedLRUCacheStrategy<String, String> emptyCache = new ThreadedLRUCacheStrategy<>(5, 50);
        Collection<String> values = emptyCache.values();
        assertNotNull(values, "values() must never be null");
        assertTrue(values.isEmpty(), "Newly created cache should return an empty collection");
        assertThrows(UnsupportedOperationException.class, () -> values.add("x"), "Empty collection must also be unmodifiable");
    }

    @Test
    void values_afterSinglePut_containsExactlyThatValue() {
        ThreadedLRUCacheStrategy<Integer, String> singleCache = new ThreadedLRUCacheStrategy<>(5, 50);
        singleCache.put(1, "one");
        Collection<String> values = singleCache.values();
        assertEquals(1, values.size(), "Collection size should be 1 after a single put");
        assertTrue(values.contains("one"), "Collection must contain the inserted value");
        assertThrows(UnsupportedOperationException.class, () -> values.remove("one"), "Collection must be immutable");
    }

    @Test
    void values_withMultipleDistinctEntries_containsAllValues() {
        ThreadedLRUCacheStrategy<String, String> multiCache = new ThreadedLRUCacheStrategy<>(5, 50);
        multiCache.put("k1", "a");
        multiCache.put("k2", "b");
        multiCache.put("k3", "c");
        Collection<String> values = multiCache.values();
        assertEquals(3, values.size(), "Should contain three values");
        List<String> expected = List.of("a", "b", "c");
        List<String> actual = new ArrayList<>(values);
        Collections.sort(expected);
        Collections.sort(actual);
        assertEquals(expected, actual, "All inserted values must be present (order not significant)");
        assertThrows(UnsupportedOperationException.class, () -> values.add("x"), "Returned collection must be unmodifiable");
    }

    @Test
    void values_includesNullValueAlongsideNonNull() {
        ThreadedLRUCacheStrategy<String, String> nullCache = new ThreadedLRUCacheStrategy<>(5, 50);
        nullCache.put("nullKey", null);
        nullCache.put("presentKey", "present");
        Collection<String> values = nullCache.values();
        assertEquals(2, values.size(), "Collection size should be 2 (null + non‑null)");
        assertTrue(values.contains(null), "Collection must contain the null value");
        assertTrue(values.contains("present"), "Collection must contain the non‑null value");
        assertThrows(UnsupportedOperationException.class, () -> values.add("x"), "Returned collection must be immutable");
    }

    @Test
    void values_snapshotIsUnchangedAfterCacheMutation() {
        // Obtain a snapshot.
        Collection<Integer> snapshot = cache.values();
        // Mutate the underlying cache.
        cache.put("newKey", 99);
        cache.remove("one");
        // The previously obtained collection must stay the same.
        assertEquals(3, snapshot.size(), "Snapshot size must remain the original size");
        List<Integer> expected = List.of(1, 2, 3);
        List<Integer> actual = new ArrayList<>(snapshot);
        Collections.sort(expected);
        Collections.sort(actual);
        assertEquals(expected, actual, "Snapshot must not reflect later mutations");
    }

    @Test
    void iterator_remove_onValuesCollection_throwsUnsupportedOperationException() {
        Collection<Integer> values = cache.values();
        Iterator<Integer> it = values.iterator();
        assertTrue(it.hasNext(), "Iterator should have at least one element");
        // advance to first element
        it.next();
        assertThrows(UnsupportedOperationException.class, it::remove, "Iterator.remove() must throw UnsupportedOperationException");
    }

    @ParameterizedTest
    @MethodSource("unsupportedCollectionOperations")
    void collectionMutationOperations_throwUnsupportedOperationException(String operation, Runnable action) {
        Collection<Integer> values = cache.values();
        assertThrows(UnsupportedOperationException.class, action, "Operation '" + operation + "' must be unsupported");
    }
}
