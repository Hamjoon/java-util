package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_navigableKeySet_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh map for each test
        map = new ConcurrentNavigableMapNullSafe<>();
        // Populate with a few entries, including a null key (null is treated as the greatest key)
        map.put("alpha", 1);
        map.put("beta", 2);
        map.put(null, 99);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to help GC and avoid cross‑test contamination
        map.clear();
    }

    private static Stream<Arguments> provideComparatorAndKeys() {
        Comparator<String> reverse = Comparator.reverseOrder();
        return Stream.of(// Natural order
        Arguments.of(Comparator.naturalOrder(), Arrays.asList("delta", "alpha", "gamma", "beta"), Arrays.asList("alpha", "beta", "delta", "gamma")), // Reverse order
        Arguments.of(reverse, Arrays.asList("delta", "alpha", "gamma", "beta"), Arrays.asList("gamma", "delta", "beta", "alpha")));
    }

    /**
     * Verify that {@link ConcurrentNavigableMapNullSafe#navigableKeySet()}
     * returns a view containing all keys, that the view is a {@link NavigableSet},
     * and that the ordering respects the null‑key rule (null is considered last
     * in the ascending view).
     */
    @Test
    void testNavigableKeySetContentsAndOrdering() {
        // Exercise the method under test
        NavigableSet<String> keySet = map.navigableKeySet();
        // The set must contain exactly the three keys we inserted
        assertEquals(3, keySet.size(), "Key set size should match map size");
        assertTrue(keySet.contains("alpha"));
        assertTrue(keySet.contains("beta"));
        assertTrue(keySet.contains(null));
        // Verify ordering – natural order for non‑null keys, null last
        Iterator<String> iterator = keySet.iterator();
        assertEquals("alpha", iterator.next(), "First key should be 'alpha'");
        assertEquals("beta", iterator.next(), "Second key should be 'beta'");
        assertNull(iterator.next(), "Third key should be null (null is last in ascending order)");
        // Additional NavigableSet behaviour checks
        assertEquals("alpha", keySet.first(), "first() should return the smallest non‑null key");
        assertNull(keySet.last(), "last() should return null because null is considered the greatest");
        // Removing a key via the set should remove it from the map as well
        assertTrue(keySet.remove("beta"), "remove() should succeed for existing key");
        assertFalse(map.containsKey("beta"));
        assertEquals(2, map.size());
        assertEquals(2, keySet.size());
        // Adding a new key through the set should affect the map
        assertTrue(keySet.add("gamma"));
        assertTrue(map.containsKey("gamma"));
        assertEquals(3, map.size());
        assertEquals(3, keySet.size());
        // The newly added key should appear in the correct order (after 'alpha' and before null)
        Iterator<String> itAfterAdd = keySet.iterator();
        assertEquals("alpha", itAfterAdd.next());
        assertEquals("gamma", itAfterAdd.next());
        assertNull(itAfterAdd.next());
    }

    /**
     * Ensure that the returned set is a live view: modifications to the map
     * are reflected in the set and vice‑versa.
     */
    @Test
    void testNavigableKeySetReflectsMapChanges() {
        NavigableSet<String> keySet = map.navigableKeySet();
        // Direct map modification
        map.put("delta", 4);
        assertTrue(keySet.contains("delta"));
        // one extra key beyond the initial three
        assertEquals(4, keySet.size() - 3);
        // Direct map removal of null key
        map.remove(null);
        assertFalse(keySet.contains(null));
        // now only three non‑null keys
        assertEquals(3, keySet.size());
    }

    @Test
    void testNavigableKeySetDelegatesToKeySet() {
        NavigableSet<String> navSet = map.navigableKeySet();
        NavigableSet<String> keySet = map.keySet();
        assertNotNull(navSet, "navigableKeySet() must not return null");
        assertSame(keySet, navSet, "navigableKeySet() should return the same instance as keySet()");
    }

    @Test
    void testNavigableKeySetReflectsAdditionsAfterObtainingView() {
        NavigableSet<String> navSet = map.navigableKeySet();
        // Add a new entry directly to the map
        map.put("delta", 4);
        assertTrue(navSet.contains("delta"), "The set view must reflect additions made to the map");
        assertEquals(4, navSet.size(), "Set size must grow after map addition");
    }

    @Test
    void testNavigableKeySetReflectsRemovalsAfterObtainingView() {
        NavigableSet<String> navSet = map.navigableKeySet();
        // Remove an existing key directly from the map
        map.remove("beta");
        assertFalse(navSet.contains("beta"), "The set view must reflect removals made to the map");
        assertEquals(2, navSet.size(), "Set size must shrink after map removal");
    }

    @Test
    void testNavigableKeySetOrderingNatural() {
        // Re‑populate the map in a random order (no null key for this test)
        map.clear();
        map.put("delta", 4);
        map.put("alpha", 1);
        map.put("gamma", 3);
        map.put("beta", 2);
        NavigableSet<String> navSet = map.navigableKeySet();
        // Expected natural ordering
        List<String> expected = Arrays.asList("alpha", "beta", "delta", "gamma");
        List<String> actual = new ArrayList<>(navSet);
        assertEquals(expected, actual, "Iterating the NavigableSet must follow natural ordering");
    }

    @Test
    void testNavigableKeySetThrowsNPEWhenUnderlyingMapIsNull() throws Exception {
        // Use reflection to force the internal delegate map to null.
        // The field name in AbstractConcurrentNullSafeMap is assumed to be "map".
        Field delegateField = AbstractConcurrentNullSafeMap.class.getDeclaredField("map");
        delegateField.setAccessible(true);
        delegateField.set(map, null);
        assertThrows(NullPointerException.class, () -> map.navigableKeySet(), "Calling navigableKeySet() when the internal map is null should throw NPE");
    }

    @Test
    void testNavigableKeySetIsMutableRemoveViaSet() {
        NavigableSet<String> navSet = map.navigableKeySet();
        assertTrue(navSet.remove("beta"), "remove(Object) must succeed for an existing key");
        assertFalse(map.containsKey("beta"), "Removal via the set must affect the underlying map");
        assertEquals(2, navSet.size(), "Set size must reflect the removal");
    }

    @Test
    void testNavigableKeySetReflectsMapClear() {
        NavigableSet<String> navSet = map.navigableKeySet();
        map.clear();
        assertTrue(navSet.isEmpty(), "The set must become empty after map.clear()");
        assertEquals(0, navSet.size(), "Set size must be zero after map.clear()");
    }

    @Test
    void testNavigableKeySetIteratorNoSuchElementException() {
        NavigableSet<String> navSet = map.navigableKeySet();
        Iterator<String> it = navSet.iterator();
        // Exhaust the iterator
        while (it.hasNext()) {
            it.next();
        }
        // Next call must throw NoSuchElementException
        assertThrows(NoSuchElementException.class, it::next, "Iterator must throw NoSuchElementException after all elements are traversed");
    }

    @Test
    void testNavigableSetHigherAndLower() {
        map.clear();
        map.put("1", 1);
        map.put("3", 3);
        map.put("5", 5);
        NavigableSet<String> navSet = map.navigableKeySet();
        assertEquals("5", navSet.higher("3"), "higher(3) should return 5");
        assertEquals("1", navSet.lower("3"), "lower(3) should return 1");
    }

    @Test
    void testNavigableSetSubSetReflectsMapChanges() {
        // Create a subset view that should contain keys between "beta" (inclusive) and "delta" (exclusive)
        NavigableSet<String> navSet = map.navigableKeySet();
        SortedSet<String> sub = navSet.subSet("beta", true, "delta", false);
        // Initially sub contains only "beta"
        assertTrue(sub.contains("beta"));
        assertEquals(1, sub.size());
        // Add a key that falls inside the sub‑range
        // "gamma" > "beta" and < "delta"? No, "gamma" > "delta". Use "c" instead.
        map.put("gamma", 7);
        // clean up
        map.remove("gamma");
        // "c" is between "beta" and "delta"
        map.put("c", 3);
        assertTrue(sub.contains("c"), "SubSet view must reflect newly added key that lies within its range");
        assertEquals(2, sub.size());
    }

    @Test
    void testNavigableSetComparatorMatchesMapComparator() throws Exception {
        // Build a map with a custom reverse‑order comparator via reflection
        Comparator<String> reverse = Comparator.reverseOrder();
        ConcurrentSkipListMap<String, Integer> customDelegate = new ConcurrentSkipListMap<>(reverse);
        // Populate the custom delegate
        customDelegate.put("a", 1);
        customDelegate.put("b", 2);
        // null handling is done by the wrapper, not the delegate
        customDelegate.put(null, 99);
        // Inject the custom delegate into the map instance
        Field delegateField = AbstractConcurrentNullSafeMap.class.getDeclaredField("map");
        delegateField.setAccessible(true);
        delegateField.set(map, customDelegate);
        NavigableSet<String> navSet = map.navigableKeySet();
        assertEquals(reverse, navSet.comparator(), "The NavigableSet's comparator must be the same as the map's comparator (reverse order)");
        // Verify ordering is indeed reverse
        Iterator<String> it = navSet.iterator();
        assertEquals("b", it.next());
        assertEquals("a", it.next());
        // null is considered greatest even in reverse order
        assertNull(it.next());
    }

    @Test
    void testNavigableSetFirstAndLastMatchMapFirstAndLastKey() {
        NavigableSet<String> navSet = map.navigableKeySet();
        assertEquals(map.firstKey(), navSet.first(), "first() must return the map's firstKey()");
        assertEquals(map.lastKey(), navSet.last(), "last() must return the map's lastKey()");
    }

    @Test
    void testNavigableSetConcurrentModificationsDoNotThrowCME() throws Exception {
        // Populate map with many entries
        for (int i = 0; i < 1000; i++) {
            map.put("k" + i, i);
        }
        NavigableSet<String> navSet = map.navigableKeySet();
        // Background thread that mutates the map while the main thread iterates
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(() -> {
            for (int i = 1000; i < 1100; i++) {
                map.put("k" + i, i);
                map.remove("k" + (i - 1000));
            }
        });
        // Iterate – we expect no ConcurrentModificationException because the underlying
        // implementation (ConcurrentSkipListMap) provides weakly consistent iterators.
        assertDoesNotThrow(() -> {
            int count = 0;
            for (String key : navSet) {
                // Just consume the iterator; the exact set of elements is not deterministic.
                count++;
                // safety break
                if (count > 2000)
                    break;
            }
        }, "Iterating over the NavigableSet while concurrent modifications occur should not throw CME");
        // ensure background thread completed
        future.get();
        executor.shutdownNow();
    }

    @ParameterizedTest
    @MethodSource("provideComparatorAndKeys")
    void testNavigableKeySetWithCustomComparator(Comparator<String> comparator, List<String> insertionOrder, List<String> expectedOrder) throws Exception {
        // Build a delegate map with the supplied comparator
        ConcurrentSkipListMap<String, Integer> delegate = new ConcurrentSkipListMap<>(comparator);
        Field delegateField = AbstractConcurrentNullSafeMap.class.getDeclaredField("map");
        delegateField.setAccessible(true);
        delegateField.set(map, delegate);
        // Insert keys in the given (random) order
        for (String k : insertionOrder) {
            map.put(k, 1);
        }
        NavigableSet<String> navSet = map.navigableKeySet();
        List<String> actual = new ArrayList<>(navSet);
        assertEquals(expectedOrder, actual, "NavigableSet must respect the provided comparator ordering");
    }
}
