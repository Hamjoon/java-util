package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.concurrent.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_navigableKeySet_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh map for each test – natural ordering is fine.
        map = new ConcurrentNavigableMapNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to aid GC; not strictly required.
        map.clear();
    }

    static Stream<List<String>> sortedKeyProvider() {
        return Stream.of(List.of("a", "b", "c"), List.of("apple", "banana", "cherry", "date"), List.of("z", "y", "x", "w"));
    }

    @Test
    void navigableKeySetReflectsAllKeysIncludingNull() {
        // Arrange
        map.put("apple", 1);
        map.put("banana", 2);
        map.put(null, 99);
        // Act
        NavigableSet<String> keySet = map.navigableKeySet();
        // Assert – size and content
        assertEquals(3, keySet.size(), "Key set size must match map size");
        assertTrue(keySet.containsAll(Arrays.asList("apple", "banana", null)), "Key set must contain all inserted keys");
        // Assert – ordering (null should be last in ascending view)
        Iterator<String> iterator = keySet.iterator();
        List<String> ordered = new ArrayList<>();
        iterator.forEachRemaining(ordered::add);
        assertEquals("apple", ordered.get(0));
        assertEquals("banana", ordered.get(1));
        assertNull(ordered.get(2), "Null key must be the last element in the ascending view");
    }

    @Test
    void navigableKeySetIsLiveWhenMapIsModifiedAfterRetrieval() {
        // Arrange
        map.put("first", 10);
        NavigableSet<String> keySet = map.navigableKeySet();
        // Act – add a new entry directly to the map
        map.put("second", 20);
        // Assert – the previously obtained set sees the new key
        assertTrue(keySet.contains("second"), "Key set must reflect entries added to the map after the set was obtained");
        assertEquals(2, keySet.size());
    }

    @Test
    void removalThroughNavigableKeySetUpdatesTheMap() {
        // Arrange
        map.put("x", 1);
        map.put("y", 2);
        map.put(null, 3);
        NavigableSet<String> keySet = map.navigableKeySet();
        // Act – remove a non‑null key via the set view
        boolean removed = keySet.remove("y");
        // Assert
        assertTrue(removed, "Removal via keySet should return true for existing key");
        assertFalse(map.containsKey("y"), "Map must no longer contain the removed key");
        assertEquals(2, map.size(), "Map size must shrink after removal through the set");
        assertFalse(keySet.contains("y"), "Key set must no longer contain the removed key");
    }

    @Test
    void navigableKeySetIsEmptyWhenMapIsEmpty() {
        // No entries have been added.
        NavigableSet<String> keySet = map.navigableKeySet();
        assertTrue(keySet.isEmpty(), "Key set should be empty when the map is empty");
        assertEquals(0, keySet.size());
    }

    @Test
    void keySetDoesNotSupportAddOperation() {
        // Arrange
        map.put("a", 1);
        NavigableSet<String> keySet = map.navigableKeySet();
        // Act & Assert – adding a key directly to the set view is unsupported
        assertThrows(UnsupportedOperationException.class, () -> keySet.add("newKey"), "Adding via the key set view should throw UnsupportedOperationException");
    }

    @Test
    void navigableKeySetBasicEmptyMap() {
        // When the map is freshly created it must return an empty NavigableSet.
        NavigableSet<String> set = map.navigableKeySet();
        assertTrue(set.isEmpty(), "The set should be empty for a newly created map");
        assertEquals(0, set.size());
        assertFalse(set.iterator().hasNext(), "Iterator should have no elements");
    }

    @Test
    void navigableKeySetSingleEntry() {
        map.put("apple", 1);
        NavigableSet<String> set = map.navigableKeySet();
        assertEquals(1, set.size());
        assertTrue(set.contains("apple"));
        assertEquals("apple", set.first(), "Single element should be both first and last");
        assertEquals("apple", set.last());
    }

    @Test
    void navigableKeySetMultipleEntriesSortedOrder() {
        // Insert out of order and verify natural ordering in the view.
        map.put("banana", 2);
        map.put("apple", 1);
        map.put("cherry", 3);
        NavigableSet<String> set = map.navigableKeySet();
        List<String> expected = List.of("apple", "banana", "cherry");
        assertEquals(expected, new ArrayList<>(set), "Keys must be sorted in natural order");
    }

    @Test
    void navigableKeySetLiveViewAfterPut() {
        map.put("first", 10);
        NavigableSet<String> set = map.navigableKeySet();
        // modify the map after obtaining the view
        map.put("second", 20);
        assertTrue(set.contains("second"), "The set view must reflect later puts");
        assertEquals(2, set.size());
    }

    @Test
    void navigableKeySetLiveViewAfterRemoveViaSet() {
        map.putAll(Map.of("alpha", 1, "beta", 2, "gamma", 3));
        NavigableSet<String> set = map.navigableKeySet();
        assertTrue(set.remove("beta"), "remove() should return true for existing key");
        assertFalse(map.containsKey("beta"), "Map must no longer contain the removed key");
        assertFalse(set.contains("beta"));
        assertEquals(2, set.size());
    }

    @Test
    void navigableKeySetNavigableOperations() {
        map.putAll(Map.of("alpha", 1, "beta", 2, "gamma", 3));
        NavigableSet<String> set = map.navigableKeySet();
        assertEquals("alpha", set.lower("beta"));
        assertEquals("gamma", set.higher("beta"));
        assertEquals("alpha", set.first());
        assertEquals("gamma", set.last());
    }

    @Test
    void navigableKeySetNullKeyHandling() {
        // Null keys are allowed and should appear as null in the view.
        map.put(null, 42);
        map.put("x", 1);
        NavigableSet<String> set = map.navigableKeySet();
        assertTrue(set.contains(null), "Set must report containing null");
        assertTrue(set.remove(null), "Removing null should succeed");
        assertFalse(set.contains(null));
        assertFalse(map.containsKey(null));
    }

    @Test
    void navigableKeySetConsistencyWithKeySet() {
        NavigableSet<String> ks1 = map.keySet();
        NavigableSet<String> ks2 = map.navigableKeySet();
        assertSame(ks1, ks2, "navigableKeySet() must return the exact same instance as keySet()");
    }

    @Test
    void navigableKeySetEmptyMapAfterRemoval() {
        map.put("x", 1);
        NavigableSet<String> set = map.navigableKeySet();
        assertTrue(set.remove("x"));
        assertTrue(set.isEmpty());
        assertTrue(map.isEmpty());
    }

    @Test
    void navigableKeySetSubMapViewIsolation() {
        map.put("a", 1);
        map.put("c", 3);
        // Obtain a subMap that includes both "a" and "c"
        ConcurrentNavigableMap<String, Integer> sub = map.subMap("a", true, "c", true);
        // modify via subMap
        sub.put("b", 2);
        NavigableSet<String> set = map.navigableKeySet();
        List<String> expected = List.of("a", "b", "c");
        assertEquals(expected, new ArrayList<>(set), "Original set must reflect changes made through subMap");
    }

    @Test
    void navigableKeySetComparatorCustom() {
        // Create a map with a descending comparator for Integer keys.
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>(Comparator.reverseOrder());
        intMap.put(1, "one");
        intMap.put(3, "three");
        intMap.put(2, "two");
        NavigableSet<Integer> set = intMap.navigableKeySet();
        List<Integer> expected = List.of(3, 2, 1);
        assertEquals(expected, new ArrayList<>(set), "Set must respect the custom comparator (descending)");
    }

    @Test
    void navigableKeySetConcurrentModification() throws Exception {
        // Obtain iterator, then add an element from another thread.
        map.put("a", 1);
        map.put("b", 2);
        Iterator<String> it = map.navigableKeySet().iterator();
        // Ensure the iterator has started (hasNext true) before we add.
        assertTrue(it.hasNext());
        // consume first element
        String first = it.next();
        // Add a new entry concurrently.
        ExecutorService exec = Executors.newSingleThreadExecutor();
        CountDownLatch latch = new CountDownLatch(1);
        exec.submit(() -> {
            try {
                // wait until main thread is ready
                latch.await();
                map.put("c", 3);
            } catch (InterruptedException ignored) {
            }
        });
        // release the worker
        latch.countDown();
        exec.shutdown();
        exec.awaitTermination(1, TimeUnit.SECONDS);
        // Continue iteration – the iterator of ConcurrentSkipListMap is weakly consistent,
        // it should see the newly added element eventually.
        List<String> iterated = new ArrayList<>();
        iterated.add(first);
        while (it.hasNext()) {
            iterated.add(it.next());
        }
        // The set must contain "c" somewhere in the iteration order.
        assertTrue(iterated.contains("c"), "Iterator should eventually see the concurrently added key");
    }

    @Test
    void navigableKeySetPollFirstEntryImpact() {
        map.putAll(Map.of("a", 1, "b", 2, "c", 3));
        // removes "a"
        Map.Entry<String, Integer> removed = map.pollFirstEntry();
        assertEquals("a", removed.getKey());
        NavigableSet<String> set = map.navigableKeySet();
        assertFalse(set.contains("a"));
        assertEquals(2, set.size());
    }

    @Test
    void navigableKeySetPollLastEntryImpact() {
        map.putAll(Map.of("a", 1, "b", 2, "c", 3));
        // removes "c"
        Map.Entry<String, Integer> removed = map.pollLastEntry();
        assertEquals("c", removed.getKey());
        NavigableSet<String> set = map.navigableKeySet();
        assertFalse(set.contains("c"));
        assertEquals(2, set.size());
    }

    @Test
    void navigableKeySetDescendingKeySetConsistency() {
        map.putAll(Map.of("1", 1, "2", 2, "3", 3));
        NavigableSet<String> asc = map.navigableKeySet();
        NavigableSet<String> desc = map.descendingKeySet();
        List<String> ascList = new ArrayList<>(asc);
        List<String> descList = new ArrayList<>(desc);
        List<String> expectedAsc = List.of("1", "2", "3");
        List<String> expectedDesc = List.of("3", "2", "1");
        assertEquals(expectedAsc, ascList, "Ascending view order");
        assertEquals(expectedDesc, descList, "Descending view order must be reverse of ascending");
    }

    @Test
    void navigableKeySetNullKeyRemoval() {
        map.put(null, 99);
        map.put("x", 1);
        NavigableSet<String> set = map.navigableKeySet();
        assertTrue(set.remove(null), "Removing null key via set must succeed");
        assertFalse(map.containsKey(null));
        assertFalse(set.contains(null));
    }

    @Test
    void navigableKeySetNullKeyHandlingExceptionScenarios() {
        // Insertion of null key must not throw.
        assertDoesNotThrow(() -> map.put(null, 42), "Putting a null key must not throw");
        NavigableSet<String> set = map.navigableKeySet();
        assertTrue(set.contains(null), "Set must report containing null after insertion");
        // Removal of null must also be safe.
        assertDoesNotThrow(() -> set.remove(null), "Removing null from set must not throw");
        assertFalse(map.containsKey(null), "Map must no longer contain null after removal");
    }

    @ParameterizedTest(name = "{index}: keys={0}")
    @MethodSource("sortedKeyProvider")
    void navigableKeySetSortedOrderParameterized(List<String> keys) {
        // Insert keys in random order.
        List<String> shuffled = new ArrayList<>(keys);
        Collections.shuffle(shuffled);
        shuffled.forEach(k -> map.put(k, k.length()));
        NavigableSet<String> set = map.navigableKeySet();
        assertEquals(keys, new ArrayList<>(set), "NavigableSet must present keys in natural sorted order");
    }
}
