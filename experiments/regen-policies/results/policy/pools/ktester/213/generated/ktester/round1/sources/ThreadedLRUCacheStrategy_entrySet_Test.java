package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ThreadedLRUCacheStrategy_entrySet_Test {

    private ThreadedLRUCacheStrategy<String, Integer> cache;

    /**
     * Create a fresh cache instance before each test.
     * Capacity 5 and a short cleanup delay are sufficient for the unit‑test scenario.
     */
    @BeforeEach
    void setupBeforeEach() {
        cache = new ThreadedLRUCacheStrategy<>(5, 100);
    }

    /**
     * No per‑test teardown required beyond letting the cache be GC‑ed.
     */
    @AfterEach
    void teardownAfterEach() {
        // Clear the cache to avoid interference between tests (good practice).
        cache.clear();
    }

    private static Stream<String> unmodifiableOperationsProvider() {
        return Stream.of("add", "remove");
    }

    @Test
    void entrySetContainsAllCurrentEntries() {
        // given
        cache.put("one", 1);
        cache.put("two", 2);
        cache.put("three", 3);
        // when
        Set<Map.Entry<String, Integer>> entrySet = cache.entrySet();
        // then
        assertEquals(3, entrySet.size(), "entrySet size should match number of entries put into the cache");
        assertTrue(entrySet.contains(new java.util.AbstractMap.SimpleEntry<>("one", 1)));
        assertTrue(entrySet.contains(new java.util.AbstractMap.SimpleEntry<>("two", 2)));
        assertTrue(entrySet.contains(new java.util.AbstractMap.SimpleEntry<>("three", 3)));
    }

    @Test
    void entrySetIsUnmodifiable() {
        // given
        cache.put("alpha", 10);
        Set<Map.Entry<String, Integer>> entrySet = cache.entrySet();
        // when / then
        assertThrows(UnsupportedOperationException.class, () -> entrySet.add(new java.util.AbstractMap.SimpleEntry<>("beta", 20)), "entrySet should be unmodifiable – add operation must throw");
        assertThrows(UnsupportedOperationException.class, () -> entrySet.remove(entrySet.iterator().next()), "entrySet should be unmodifiable – remove operation must throw");
    }

    @Test
    void entrySetReflectsCacheChangesOnSubsequentCalls() {
        // given
        cache.put("A", 100);
        cache.put("B", 200);
        Set<Map.Entry<String, Integer>> firstSet = cache.entrySet();
        assertEquals(2, firstSet.size());
        // when: remove an entry from the cache
        cache.remove("A");
        // then: a new call to entrySet must reflect the removal
        Set<Map.Entry<String, Integer>> secondSet = cache.entrySet();
        assertEquals(1, secondSet.size());
        assertTrue(secondSet.contains(new java.util.AbstractMap.SimpleEntry<>("B", 200)));
        assertFalse(secondSet.stream().anyMatch(e -> e.getKey().equals("A")), "Removed key should no longer appear in the entry set");
    }

    @Test
    void emptyCacheReturnsEmptyUnmodifiableSet() {
        // cache is newly created and contains no entries
        Set<Map.Entry<String, Integer>> entrySet = cache.entrySet();
        assertTrue(entrySet.isEmpty(), "Entry set of an empty cache must be empty");
        assertThrows(UnsupportedOperationException.class, () -> entrySet.add(new AbstractMap.SimpleEntry<>("any", 1)), "Entry set must be unmodifiable even when empty");
    }

    @Test
    void singleEntryCacheReturnsSetWithThatEntry() {
        cache.put("key1", 42);
        Set<Map.Entry<String, Integer>> entrySet = cache.entrySet();
        assertEquals(1, entrySet.size(), "Entry set size should be 1");
        assertTrue(entrySet.contains(new AbstractMap.SimpleEntry<>("key1", 42)), "Entry set must contain the single inserted mapping");
        assertThrows(UnsupportedOperationException.class, () -> entrySet.remove(entrySet.iterator().next()), "Entry set must be unmodifiable");
    }

    @Test
    void multipleEntriesCacheReturnsAllEntriesIrrespectiveOfOrder() {
        cache.put("k1", 10);
        cache.put("k2", 20);
        cache.put("k3", 30);
        Set<Map.Entry<String, Integer>> entrySet = cache.entrySet();
        assertEquals(3, entrySet.size(), "Entry set size should match number of inserted entries");
        assertTrue(entrySet.contains(new AbstractMap.SimpleEntry<>("k1", 10)));
        assertTrue(entrySet.contains(new AbstractMap.SimpleEntry<>("k2", 20)));
        assertTrue(entrySet.contains(new AbstractMap.SimpleEntry<>("k3", 30)));
        // order is irrelevant – just verify presence
    }

    @ParameterizedTest(name = "Unmodifiable operation ''{0}'' throws UnsupportedOperationException")
    @MethodSource("unmodifiableOperationsProvider")
    void unmodifiableSetOperationsThrow(String operation) {
        cache.put("a", 1);
        cache.put("b", 2);
        Set<Map.Entry<String, Integer>> entrySet = cache.entrySet();
        if ("add".equals(operation)) {
            assertThrows(UnsupportedOperationException.class, () -> entrySet.add(new AbstractMap.SimpleEntry<>("c", 3)), "Add operation must be unsupported");
        } else if ("remove".equals(operation)) {
            assertThrows(UnsupportedOperationException.class, () -> entrySet.remove(entrySet.iterator().next()), "Remove operation must be unsupported");
        } else {
            fail("Unsupported test operation: " + operation);
        }
    }

    @Test
    void entrySetIsSnapshotAndNotAffectedByLaterModifications() {
        // initial population
        cache.put("x", 100);
        cache.put("y", 200);
        Set<Map.Entry<String, Integer>> snapshot = cache.entrySet();
        // modify cache after obtaining the snapshot
        cache.put("z", 300);
        cache.remove("x");
        // snapshot must stay unchanged
        assertEquals(2, snapshot.size(), "Snapshot size must remain as it was at creation time");
        assertTrue(snapshot.contains(new AbstractMap.SimpleEntry<>("x", 100)));
        assertTrue(snapshot.contains(new AbstractMap.SimpleEntry<>("y", 200)));
        assertFalse(snapshot.stream().anyMatch(e -> e.getKey().equals("z")), "Newly added entry must not appear in the previously obtained set");
    }

    @Test
    void nullValueEntryIsRepresentedInEntrySet() {
        cache.put("nullKey", null);
        Set<Map.Entry<String, Integer>> entrySet = cache.entrySet();
        assertEquals(1, entrySet.size(), "Entry set should contain exactly one mapping");
        Map.Entry<String, Integer> expected = new AbstractMap.SimpleEntry<>("nullKey", null);
        assertTrue(entrySet.contains(expected), "Entry set must contain the (nullKey, null) entry");
        assertThrows(UnsupportedOperationException.class, () -> entrySet.add(new AbstractMap.SimpleEntry<>("another", 1)), "Entry set must remain unmodifiable");
    }

    @Test
    void entrySetThrowsNullPointerExceptionWhenInternalCacheIsNull() throws Exception {
        // Use reflection to set the private final field 'cache' to null.
        Field cacheField = ThreadedLRUCacheStrategy.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        cacheField.set(cache, null);
        // The contract of entrySet() does not guard against a null internal map,
        // so a NullPointerException is expected when it tries to iterate over cache.values().
        assertThrows(NullPointerException.class, () -> cache.entrySet(), "Calling entrySet() when the internal cache map is null must throw NPE");
    }
}
