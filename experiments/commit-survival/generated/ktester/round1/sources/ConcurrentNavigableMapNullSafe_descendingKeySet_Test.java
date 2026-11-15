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
class ConcurrentNavigableMapNullSafe_descendingKeySet_Test {

    private static ConcurrentNavigableMapNullSafe<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh map for each test to avoid cross‑test interference
        map = new ConcurrentNavigableMapNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map explicitly (not strictly required because a new instance is created each time)
        map.clear();
    }

    private static Stream<Arguments> emptyMapProvider() {
        return Stream.of(Arguments.of(Collections.emptyList()));
    }

    private static Stream<Arguments> singleEntryProvider() {
        return Stream.of(Arguments.of("alpha"));
    }

    private static Stream<Arguments> multipleEntriesNaturalOrderProvider() {
        return Stream.of(Arguments.of(Arrays.asList("alpha", "beta", "delta", "epsilon", "zeta"), Arrays.asList("zeta", "epsilon", "delta", "beta", "alpha")));
    }

    private static Stream<Arguments> customComparatorReverseProvider() {
        return Stream.of(Arguments.of(Comparator.<String>reverseOrder(), Arrays.asList("alpha", "beta", "delta", "epsilon", "zeta"), Arrays.asList("alpha", "beta", "delta", "epsilon", "zeta")));
    }

    private static Stream<Arguments> nullKeyHandlingProvider() {
        return Stream.of(Arguments.of(Arrays.asList(null, "b", "c"), Arrays.asList(null, "c", "b")));
    }

    private static Stream<Arguments> iteratorOrderProvider() {
        return Stream.of(Arguments.of(Arrays.asList("w", "x", "y", "z"), Arrays.asList("z", "y", "x", "w")));
    }

    private static Stream<Arguments> firstAndLastProvider() {
        // keys from "a" to "z"
        List<String> keys = new ArrayList<>();
        for (char c = 'a'; c <= 'z'; c++) {
            keys.add(String.valueOf(c));
        }
        return Stream.of(Arguments.of(keys, "z", "a"));
    }

    private static Stream<Arguments> caseInsensitiveComparatorProvider() {
        return Stream.of(Arguments.of(String.CASE_INSENSITIVE_ORDER, Arrays.asList("alpha", "Mike", "Zulu"), Arrays.asList("Zulu", "Mike", "alpha")));
    }

    @Test
    void descendingKeySetOrdersKeysInReverseIncludingNullFirst() {
        // Given: a map with several keys, including a null key
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // null is considered last in ascending order, first in descending
        map.put(null, 99);
        // When: retrieving the descending key set
        NavigableSet<String> descendingSet = map.descendingKeySet();
        // Then: the set should be sorted in reverse natural order, with null first
        List<String> expectedOrder = new ArrayList<>();
        // null comes first in descending view
        expectedOrder.add(null);
        expectedOrder.add("c");
        expectedOrder.add("b");
        expectedOrder.add("a");
        assertEquals(expectedOrder, new ArrayList<>(descendingSet), "Descending key set should contain keys in reverse order with null first");
    }

    @Test
    void descendingKeySetReflectsSubsequentModifications() {
        // Given: an initial map
        map.put("x", 10);
        map.put("y", 20);
        map.put(null, 0);
        NavigableSet<String> descendingSet = map.descendingKeySet();
        // When: we modify the map after obtaining the view
        map.put("z", 30);
        map.remove("x");
        // Then: the descending set view must reflect those changes
        List<String> expected = Arrays.asList(null, "z", "y");
        assertEquals(expected, new ArrayList<>(descendingSet), "Descending key set view should stay in sync with the underlying map");
    }

    @Test
    void descendingKeySetIsUnmodifiableThroughSetMethods() {
        // Given: a map with some entries
        map.put("one", 1);
        map.put(null, 100);
        NavigableSet<String> descendingSet = map.descendingKeySet();
        // When/Then: attempts to modify the set directly should throw UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, () -> descendingSet.add("newKey"));
        assertThrows(UnsupportedOperationException.class, () -> descendingSet.remove(null));
        assertThrows(UnsupportedOperationException.class, () -> descendingSet.clear());
    }

    @Test
    void descendingKeySetHandlesEmptyMap() {
        // Given: an empty map
        // When: retrieving the descending key set
        NavigableSet<String> descendingSet = map.descendingKeySet();
        // Then: the set should be empty and not null
        assertNotNull(descendingSet);
        assertTrue(descendingSet.isEmpty());
    }

    @Test
    void descendingKeySetIteratesCorrectly() {
        // Given: a map with several keys
        map.put("1", 1);
        map.put("2", 2);
        map.put("3", 3);
        map.put(null, 0);
        NavigableSet<String> descendingSet = map.descendingKeySet();
        // When: iterating over the set
        Iterator<String> iterator = descendingSet.iterator();
        // Then: iterator should return keys in the expected order
        List<String> iterated = new ArrayList<>();
        while (iterator.hasNext()) {
            iterated.add(iterator.next());
        }
        List<String> expected = Arrays.asList(null, "3", "2", "1");
        assertEquals(expected, iterated);
    }

    @ParameterizedTest(name = "empty map → expected size {0}")
    @MethodSource("emptyMapProvider")
    void descendingKeySet_EmptyMap_ReturnsEmptySet(List<String> ignored) {
        // When
        NavigableSet<String> descendingSet = map.descendingKeySet();
        // Then
        assertNotNull(descendingSet, "The returned set must never be null");
        assertTrue(descendingSet.isEmpty(), "Descending set of an empty map should be empty");
        assertEquals(0, descendingSet.size());
    }

    @ParameterizedTest(name = "single entry ''{0}'' → set contains only that key")
    @MethodSource("singleEntryProvider")
    void descendingKeySet_SingleEntry(String key) {
        map.put(key, 1);
        NavigableSet<String> descendingSet = map.descendingKeySet();
        assertEquals(Collections.singletonList(key), new ArrayList<>(descendingSet));
    }

    @ParameterizedTest(name = "multiple entries natural order")
    @MethodSource("multipleEntriesNaturalOrderProvider")
    void descendingKeySet_MultipleEntriesNaturalOrder(List<String> keysToAdd, List<String> expectedOrder) {
        int i = 0;
        for (String k : keysToAdd) {
            map.put(k, ++i);
        }
        NavigableSet<String> descendingSet = map.descendingKeySet();
        assertEquals(expectedOrder, new ArrayList<>(descendingSet));
    }

    @ParameterizedTest(name = "custom comparator reverse order")
    @MethodSource("customComparatorReverseProvider")
    void descendingKeySet_CustomComparator_ReverseOrder(Comparator<String> comparator, List<String> keysToAdd, List<String> expectedOrder) {
        // The map constructor that accepts a comparator is assumed to exist.
        map = new ConcurrentNavigableMapNullSafe<>(comparator);
        int i = 0;
        for (String k : keysToAdd) {
            map.put(k, ++i);
        }
        NavigableSet<String> descendingSet = map.descendingKeySet();
        assertEquals(expectedOrder, new ArrayList<>(descendingSet));
    }

    @ParameterizedTest(name = "null key handling")
    @MethodSource("nullKeyHandlingProvider")
    void descendingKeySet_NullKeyHandling(List<String> keysToAdd, List<String> expectedOrder) {
        int i = 0;
        for (String k : keysToAdd) {
            map.put(k, ++i);
        }
        NavigableSet<String> descendingSet = map.descendingKeySet();
        assertEquals(expectedOrder, new ArrayList<>(descendingSet));
    }

    @ParameterizedTest(name = "iterator order consistency")
    @MethodSource("iteratorOrderProvider")
    void descendingKeySet_IteratorOrderConsistency(List<String> keysToAdd, List<String> expectedOrder) {
        int i = 0;
        for (String k : keysToAdd) {
            map.put(k, ++i);
        }
        NavigableSet<String> descendingSet = map.descendingKeySet();
        List<String> iterated = new ArrayList<>();
        for (String s : descendingSet) {
            iterated.add(s);
        }
        assertEquals(expectedOrder, iterated);
    }

    @ParameterizedTest(name = "first() and last() elements")
    @MethodSource("firstAndLastProvider")
    void descendingKeySet_FirstAndLastElements(List<String> keys, String expectedFirst, String expectedLast) {
        int i = 0;
        for (String k : keys) {
            map.put(k, ++i);
        }
        NavigableSet<String> descendingSet = map.descendingKeySet();
        assertEquals(expectedFirst, descendingSet.first());
        assertEquals(expectedLast, descendingSet.last());
    }

    @ParameterizedTest(name = "custom comparator case‑insensitive")
    @MethodSource("caseInsensitiveComparatorProvider")
    void descendingKeySet_CustomComparator_CaseInsensitive(Comparator<String> comparator, List<String> keysToAdd, List<String> expectedOrder) {
        map = new ConcurrentNavigableMapNullSafe<>(comparator);
        int i = 0;
        for (String k : keysToAdd) {
            map.put(k, ++i);
        }
        NavigableSet<String> descendingSet = map.descendingKeySet();
        assertEquals(expectedOrder, new ArrayList<>(descendingSet));
    }

    @Test
    void descendingKeySet_LiveViewReflectsRemoval() {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        NavigableSet<String> descendingSet = map.descendingKeySet();
        // Remove "a" after view creation
        map.remove("a");
        List<String> expected = Arrays.asList("c", "b");
        assertEquals(expected, new ArrayList<>(descendingSet));
    }

    @Test
    void descendingKeySet_LiveViewReflectsAddition() {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        NavigableSet<String> descendingSet = map.descendingKeySet();
        // Add "d" after view creation
        map.put("d", 4);
        List<String> expected = Arrays.asList("d", "c", "b", "a");
        assertEquals(expected, new ArrayList<>(descendingSet));
    }

    @Test
    void descendingKeySet_ConcurrencySafety_NoCME() throws Exception {
        // Populate map with initial data
        for (char c = 'a'; c <= 'e'; c++) {
            map.put(String.valueOf(c), (int) c);
        }
        NavigableSet<String> descendingSet = map.descendingKeySet();
        // Prepare a thread that mutates the map concurrently
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(1);
        Thread mutator = new Thread(() -> {
            try {
                // wait until main thread starts iterating
                startLatch.await();
                map.put("z", 122);
                map.remove("b");
                // null key is allowed
                map.put(null, 0);
            } catch (InterruptedException ignored) {
            } finally {
                doneLatch.countDown();
            }
        });
        mutator.start();
        // Start iteration while mutations happen
        List<String> observed = new ArrayList<>();
        // let mutator proceed
        startLatch.countDown();
        for (String key : descendingSet) {
            observed.add(key);
        }
        // ensure mutator finished
        doneLatch.await();
        // No ConcurrentModificationException should have been thrown.
        // The observed set must contain at least the original elements; additional
        // elements may appear because the view is live.
        assertTrue(observed.containsAll(Arrays.asList("e", "d", "c", "a")), "Observed elements should include the original keys (except the removed one)");
        // Verify that the map reflects the concurrent changes
        assertTrue(map.containsKey("z"));
        assertFalse(map.containsKey("b"));
        assertTrue(map.containsKey(null));
    }

    @Test
    void descendingKeySet_RemoveViaSetUpdatesMap() {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        NavigableSet<String> descendingSet = map.descendingKeySet();
        // Remove "b" via the set view
        boolean removed = descendingSet.remove("b");
        assertTrue(removed, "Set.remove should return true for existing key");
        assertFalse(map.containsKey("b"), "Underlying map must no longer contain the removed key");
        assertEquals(Arrays.asList("c", "a"), new ArrayList<>(descendingSet));
    }

    @Test
    void descendingKeySet_ClearViewClearsMap() {
        map.put("one", 1);
        map.put("two", 2);
        map.put(null, 0);
        NavigableSet<String> descendingSet = map.descendingKeySet();
        descendingSet.clear();
        assertTrue(map.isEmpty(), "Clearing the descending set view must empty the underlying map");
        assertEquals(0, descendingSet.size());
    }

    @Test
    void descendingKeySet_UnsupportedAddOperation() {
        map.put("existing", 1);
        NavigableSet<String> descendingSet = map.descendingKeySet();
        assertThrows(UnsupportedOperationException.class, () -> descendingSet.add("newKey"));
    }
}
