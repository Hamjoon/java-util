package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TTLCache_values_Test {

    private TTLCache<Integer, String> ttlCache;

    @BeforeEach
    void setupBeforeEach() {
        // Use a long TTL so entries do not expire during most tests.
        ttlCache = new TTLCache<>(10_000L, -1);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the cache is cleared between tests to avoid cross‑test contamination.
        if (ttlCache != null) {
            ttlCache.clear();
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the shared background purge thread used by TTLCache.
        TTLCache.shutdown();
    }

    /* -------------------------------------------------------------
     * Basic functionality – parameterized
     * ------------------------------------------------------------- */
    private static List<Arguments> basicCases() {
        return List.of(// empty cache
        Arguments.of(Collections.emptyMap(), Collections.emptyList(), "When the cache is empty, values() should return an empty list."), // three distinct values
        Arguments.of(Map.of(1, "a", 2, "b", 3, "c"), List.of("a", "b", "c"), "When the cache contains three entries with values \"a\", \"b\", \"c\", values() should return a list containing those values in iteration order."), // null value together with a non‑null value
        Arguments.of(new LinkedHashMap<Integer, String>() {

            {
                put(1, null);
                put(2, "x");
            }
        }, List.of(null, "x"), "When the cache contains entries where one value is null and another is \"x\", values() should include the null element in the returned list."));
    }

    @Test
    void valuesShouldContainAllInsertedValues() {
        ttlCache.put(1, "Alpha");
        ttlCache.put(2, "Beta");
        ttlCache.put(3, "Gamma");
        Collection<String> values = ttlCache.values();
        assertEquals(3, values.size(), "values() should return a collection with three elements");
        Set<String> expected = new HashSet<>();
        expected.add("Alpha");
        expected.add("Beta");
        expected.add("Gamma");
        assertTrue(values.containsAll(expected), "values() should contain all inserted values");
    }

    @Test
    void valuesShouldBeEmptyAfterAllEntriesExpire() throws InterruptedException {
        // Short TTL and a short cleanup interval so expiration happens quickly.
        ttlCache = new TTLCache<>(200L, -1, 50L);
        ttlCache.put(10, "X");
        ttlCache.put(20, "Y");
        // Verify values are present before expiration.
        assertEquals(2, ttlCache.values().size());
        // Wait longer than the TTL.
        Thread.sleep(300L);
        // The background purge task should have removed the entries.
        Collection<String> valuesAfterExpiry = ttlCache.values();
        assertTrue(valuesAfterExpiry.isEmpty(), "After TTL expiry, values() should return an empty collection");
    }

    @Test
    void valuesCollectionIsASnapshotNotALiveView() {
        ttlCache.put(1, "One");
        ttlCache.put(2, "Two");
        Collection<String> snapshot = ttlCache.values();
        // Mutate the snapshot – this must NOT affect the underlying cache.
        assertTrue(snapshot.remove("One"), "Removal from the snapshot should succeed");
        assertEquals(1, snapshot.size(), "Snapshot should reflect the removal");
        // The cache itself must still contain both entries.
        assertEquals(2, ttlCache.size(), "Cache size must remain unchanged after mutating the snapshot");
        assertTrue(ttlCache.containsKey(1));
        assertTrue(ttlCache.containsKey(2));
        // Adding a new element to the snapshot must also be isolated.
        assertTrue(snapshot.add("Three"), "Adding to the snapshot should succeed");
        assertEquals(2, ttlCache.size(), "Cache size must still be unchanged after adding to snapshot");
        assertFalse(ttlCache.containsValue("Three"));
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("basicCases")
    void valuesBasicFunctionality(Map<Integer, String> entries, List<String> expected, String description) {
        // Populate the cache with the supplied entries
        entries.forEach(ttlCache::put);
        Collection<String> values = ttlCache.values();
        assertEquals(expected.size(), values.size(), "Collection size should match expected size");
        // Preserve order for the list‑based expectations
        if (expected instanceof List) {
            assertEquals(expected, new ArrayList<>(values), "Returned values should preserve insertion order");
        } else {
            assertTrue(values.containsAll(expected), "Returned collection must contain all expected elements");
        }
    }

    @Test
    void valuesShouldThrowWhenInternalMapIsNull() throws Exception {
        // Set the private field `cacheMap` to null via reflection.
        Field mapField = TTLCache.class.getDeclaredField("cacheMap");
        mapField.setAccessible(true);
        mapField.set(ttlCache, null);
        // Expect a NullPointerException when values() tries to dereference the map.
        assertThrows(NullPointerException.class, ttlCache::values, "Calling values() with a null internal map must throw NPE");
    }

    @Test
    void valuesShouldThrowWhenCacheMapContainsNullEntry() throws Exception {
        // Mock a ConcurrentMap that returns a collection containing a null entry.
        @SuppressWarnings("unchecked")
        ConcurrentMap<Integer, TTLCache.CacheEntry<Integer, String>> mockMap = mock(ConcurrentMap.class);
        Collection<TTLCache.CacheEntry<Integer, String>> valuesCollection = Arrays.asList(// the problematic null entry
        null, // a benign entry to keep the collection non‑empty
        mock(TTLCache.CacheEntry.class));
        when(mockMap.values()).thenReturn(valuesCollection);
        // Inject the mock map.
        Field mapField = TTLCache.class.getDeclaredField("cacheMap");
        mapField.setAccessible(true);
        mapField.set(ttlCache, mockMap);
        // The iteration will hit the null entry and cause an NPE.
        assertThrows(NullPointerException.class, ttlCache::values, "Iterating over a null CacheEntry should result in NPE");
    }

    @Test
    void valuesShouldThrowWhenCacheEntryHasNullNode() throws Exception {
        // Create a mock CacheEntry whose `node` field is null.
        @SuppressWarnings("unchecked")
        TTLCache.CacheEntry<Integer, String> entryWithNullNode = mock(TTLCache.CacheEntry.class);
        // Ensure the node field is null – we don't need to stub any method because the
        // values() implementation accesses `entry.node.value` directly via reflection.
        Field nodeField = entryWithNullNode.getClass().getDeclaredField("node");
        nodeField.setAccessible(true);
        nodeField.set(entryWithNullNode, null);
        // Mock the map to return a collection containing this malformed entry.
        @SuppressWarnings("unchecked")
        ConcurrentMap<Integer, TTLCache.CacheEntry<Integer, String>> mockMap = mock(ConcurrentMap.class);
        when(mockMap.values()).thenReturn(Collections.singleton(entryWithNullNode));
        // Inject the mock map.
        Field mapField = TTLCache.class.getDeclaredField("cacheMap");
        mapField.setAccessible(true);
        mapField.set(ttlCache, mockMap);
        // Expect NPE because `entry.node` is null.
        assertThrows(NullPointerException.class, ttlCache::values, "Accessing a CacheEntry with a null node should throw NPE");
    }
}
