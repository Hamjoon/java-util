package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;

@ExtendWith(MockitoExtension.class)
class ThreadedLRUCacheStrategy_entrySet_Test {

    /**
     * Instance under test – a fresh cache for each test method.
     */
    private ThreadedLRUCacheStrategy<String, Integer> cache;

    /**
     * Reasonable defaults for the constructor arguments.
     */
    private static final int CAPACITY = 10;

    private static final int CLEANUP_DELAY_MILLIS = 50;

    @BeforeEach
    void setupBeforeEach() {
        // Create a new cache before each test to guarantee isolation.
        cache = new ThreadedLRUCacheStrategy<>(CAPACITY, CLEANUP_DELAY_MILLIS);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the cache so that any background purge task does not retain state.
        cache.clear();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the shared scheduler used by all ThreadedLRUCacheStrategy instances.
        ThreadedLRUCacheStrategy.shutdown();
    }

    @Test
    void entrySet_isEmptyWhenCacheIsEmpty() {
        Set<Map.Entry<String, Integer>> entrySet = cache.entrySet();
        assertNotNull(entrySet, "entrySet should never be null");
        assertTrue(entrySet.isEmpty(), "entrySet should be empty for a newly created cache");
    }

    @Test
    void entrySet_containsAllPutEntries() {
        // Arrange – put a few distinct key/value pairs.
        cache.put("one", 1);
        cache.put("two", 2);
        cache.put("three", 3);
        // Act
        Set<Map.Entry<String, Integer>> entrySet = cache.entrySet();
        // Assert – size matches and each entry is present.
        assertEquals(3, entrySet.size(), "entrySet size should match number of inserted entries");
        assertTrue(entrySet.contains(new AbstractMap.SimpleEntry<>("one", 1)), "entrySet should contain entry (one,1)");
        assertTrue(entrySet.contains(new AbstractMap.SimpleEntry<>("two", 2)), "entrySet should contain entry (two,2)");
        assertTrue(entrySet.contains(new AbstractMap.SimpleEntry<>("three", 3)), "entrySet should contain entry (three,3)");
    }

    @Test
    void entrySet_reflectsRemovals() {
        // Arrange
        cache.put("alpha", 100);
        cache.put("beta", 200);
        cache.put("gamma", 300);
        // Verify initial state
        Set<Map.Entry<String, Integer>> initialSet = cache.entrySet();
        assertEquals(3, initialSet.size());
        // Act – remove one key
        cache.remove("beta");
        // Assert – the entrySet no longer contains the removed entry
        Set<Map.Entry<String, Integer>> afterRemovalSet = cache.entrySet();
        assertEquals(2, afterRemovalSet.size(), "entrySet size should decrease after removal");
        assertFalse(afterRemovalSet.contains(new AbstractMap.SimpleEntry<>("beta", 200)), "entrySet should not contain the removed entry (beta,200)");
    }

    @Test
    void entrySet_isUnmodifiable() {
        // Arrange
        cache.put("key", 42);
        Set<Map.Entry<String, Integer>> entrySet = cache.entrySet();
        // Act & Assert – attempts to modify the set must throw UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, () -> entrySet.add(new AbstractMap.SimpleEntry<>("newKey", 99)), "entrySet should be unmodifiable (add)");
        assertThrows(UnsupportedOperationException.class, () -> entrySet.remove(new AbstractMap.SimpleEntry<>("key", 42)), "entrySet should be unmodifiable (remove)");
        assertThrows(UnsupportedOperationException.class, () -> entrySet.clear(), "entrySet should be unmodifiable (clear)");
    }

    @Test
    void entrySet_singleEntry() {
        // Single entry scenario
        cache.put("key1", 111);
        Set<Map.Entry<String, Integer>> entrySet = cache.entrySet();
        assertEquals(1, entrySet.size(), "Set size should be 1 after inserting a single entry");
        assertTrue(entrySet.contains(new SimpleEntry<>("key1", 111)), "Set must contain the inserted entry (key1,111)");
    }

    @Test
    void entrySet_snapshotAfterPutDoesNotChange() {
        // Capture a snapshot, then mutate the cache.
        cache.put("a", 1);
        cache.put("b", 2);
        Set<Map.Entry<String, Integer>> snapshot = cache.entrySet();
        // Mutate underlying cache
        cache.put("c", 3);
        cache.remove("a");
        // Snapshot must remain unchanged
        assertEquals(2, snapshot.size(), "Snapshot size must stay the same after cache mutation");
        assertTrue(snapshot.contains(new SimpleEntry<>("a", 1)));
        assertTrue(snapshot.contains(new SimpleEntry<>("b", 2)));
        assertFalse(snapshot.contains(new SimpleEntry<>("c", 3)));
    }

    @Test
    void entrySet_entryMutationIndependence() {
        // Put an entry, obtain entrySet, mutate the entry's value.
        cache.put("k1", 10);
        Set<Map.Entry<String, Integer>> entrySet = cache.entrySet();
        // Find the entry for "k1"
        Map.Entry<String, Integer> entry = entrySet.stream().filter(e -> Objects.equals(e.getKey(), "k1")).findFirst().orElseThrow(() -> new AssertionError("Entry for k1 not found"));
        // Mutate the entry value
        entry.setValue(20);
        // Verify cache still holds original value
        assertEquals(10, cache.get("k1"), "Cache value must remain unchanged after entry mutation");
        // Verify the entry in the set reflects the new value
        assertEquals(20, entry.getValue(), "Entry value in the set should reflect the mutation");
    }

    @Test
    void entrySet_iteratorRemoveProhibited() {
        cache.put("x", 1);
        Set<Map.Entry<String, Integer>> entrySet = cache.entrySet();
        Iterator<Map.Entry<String, Integer>> iterator = entrySet.iterator();
        assertTrue(iterator.hasNext(), "Iterator should have at least one element");
        // advance to first element
        iterator.next();
        assertThrows(UnsupportedOperationException.class, iterator::remove, "Iterator.remove() must throw UnsupportedOperationException for unmodifiable set");
    }

    @Test
    void entrySet_containsAndContainsAllSemantics() {
        cache.put("a", 1);
        cache.put("b", 2);
        Set<Map.Entry<String, Integer>> entrySet = cache.entrySet();
        // contains checks
        assertTrue(entrySet.contains(new SimpleEntry<>("a", 1)), "contains should be true for existing entry");
        assertFalse(entrySet.contains(new SimpleEntry<>("c", 3)), "contains should be false for non‑existing entry");
        // containsAll true case
        Collection<Map.Entry<String, Integer>> allExisting = Arrays.asList(new SimpleEntry<>("a", 1), new SimpleEntry<>("b", 2));
        assertTrue(entrySet.containsAll(allExisting), "containsAll should be true when all entries exist");
        // containsAll false case (one missing)
        Collection<Map.Entry<String, Integer>> mixed = Arrays.asList(new SimpleEntry<>("a", 1), new SimpleEntry<>("c", 3));
        assertFalse(entrySet.containsAll(mixed), "containsAll should be false when at least one entry is missing");
    }

    @Test
    void entrySet_snapshotAfterClear() {
        cache.put("p", 100);
        cache.put("q", 200);
        cache.put("r", 300);
        Set<Map.Entry<String, Integer>> snapshot = cache.entrySet();
        // Clear underlying cache
        cache.clear();
        // Snapshot must stay intact
        assertEquals(3, snapshot.size(), "Snapshot size must remain 3 after cache.clear()");
        assertTrue(snapshot.contains(new SimpleEntry<>("p", 100)));
        assertTrue(snapshot.contains(new SimpleEntry<>("q", 200)));
        assertTrue(snapshot.contains(new SimpleEntry<>("r", 300)));
    }

    @Test
    void entrySet_sizeConsistencyAfterRemoval() {
        cache.put("x", 10);
        cache.put("y", 20);
        cache.put("z", 30);
        cache.remove("y");
        Set<Map.Entry<String, Integer>> entrySet = cache.entrySet();
        assertEquals(2, entrySet.size(), "Set size should match cache size after removal");
        assertEquals(2, cache.size(), "Cache size should be 2 after removal");
        assertTrue(entrySet.contains(new SimpleEntry<>("x", 10)));
        assertTrue(entrySet.contains(new SimpleEntry<>("z", 30)));
        assertFalse(entrySet.contains(new SimpleEntry<>("y", 20)));
    }

    @Test
    void entrySet_toArrayCorrectness() {
        cache.put("p", 1);
        cache.put("q", 2);
        Set<Map.Entry<String, Integer>> entrySet = cache.entrySet();
        Object[] array = entrySet.toArray();
        assertEquals(2, array.length, "toArray length must equal number of entries");
        // Verify each element is a Map.Entry and matches one of the inserted pairs
        Set<Map.Entry<String, Integer>> expected = new HashSet<>(Arrays.asList(new SimpleEntry<>("p", 1), new SimpleEntry<>("q", 2)));
        for (Object obj : array) {
            assertTrue(obj instanceof Map.Entry, "Array element must be a Map.Entry");
            @SuppressWarnings("unchecked")
            Map.Entry<String, Integer> e = (Map.Entry<String, Integer>) obj;
            assertTrue(expected.remove(e), "Array should contain entry " + e);
        }
        assertTrue(expected.isEmpty(), "All expected entries should have been found in the array");
    }
}
