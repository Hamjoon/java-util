package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.concurrent.ConcurrentNavigableMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

// Mockito not actually used but kept from template
@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_descendingKeySet_Test {

    /**
     * The map under test – a concrete instance of the null‑safe concurrent navigable map.
     */
    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    /**
     * The descending key set view obtained from {@code map.descendingKeySet()}.
     */
    private NavigableSet<String> descendingKeySet;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh map for each test to guarantee isolation.
        map = new ConcurrentNavigableMapNullSafe<>();
        // Populate the map with a mixture of normal and null keys.
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // null key is allowed and should appear first in the descending view.
        map.put(null, 100);
        // Obtain the descending key set once – it is a live view of the map.
        descendingKeySet = map.descendingKeySet();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references – not strictly necessary but makes the intent explicit.
        map.clear();
        descendingKeySet = null;
    }

    /**
     * Provides a simple integer map pre‑populated with keys 1, 2, 3.
     * The map uses the natural ordering of {@link Integer}.
     */
    private static Stream<ConcurrentNavigableMapNullSafe<Integer, String>> naturalOrderMapProvider() {
        return Stream.of(new ConcurrentNavigableMapNullSafe<Integer, String>() {

            {
                put(1, "one");
                put(2, "two");
                put(3, "three");
            }
        });
    }

    @Test
    void descendingKeySet_hasCorrectOrderIncludingNullKey() {
        // According to the Javadoc, null keys are considered *last* in the ascending order
        // and therefore *first* in the descending order.
        Iterator<String> it = descendingKeySet.iterator();
        assertTrue(it.hasNext(), "Descending set should not be empty");
        assertNull(it.next(), "Null key must be the first element in descending order");
        assertEquals("c", it.next(), "Next key should be the greatest (c)");
        assertEquals("b", it.next(), "Next key should be the next greatest (b)");
        assertEquals("a", it.next(), "Next key should be the smallest non‑null key (a)");
        assertFalse(it.hasNext(), "No more elements should be present");
    }

    @Test
    void descendingKeySet_isLiveView_reflectsMapChanges() {
        // Remove a key via the map and verify the view updates.
        map.remove("b");
        assertFalse(descendingKeySet.contains("b"), "Removed key must disappear from descending view");
        // Add a new key via the map and verify the view updates.
        map.put("d", 4);
        assertTrue(descendingKeySet.contains("d"), "Newly added key must appear in descending view");
        // Verify order after addition: null, d, c, a
        Iterator<String> it = descendingKeySet.iterator();
        assertNull(it.next());
        assertEquals("d", it.next());
        assertEquals("c", it.next());
        assertEquals("a", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void descendingKeySet_removalThroughViewAffectsMap() {
        // Removing via the view should remove the entry from the underlying map.
        boolean removed = descendingKeySet.remove(null);
        assertTrue(removed, "Removing null key through view must succeed");
        assertNull(map.get(null), "Map must no longer contain the null key");
        assertFalse(descendingKeySet.contains(null), "View must no longer contain the null key");
        // Removing a non‑null key through the view.
        removed = descendingKeySet.remove("c");
        assertTrue(removed, "Removing existing key through view must succeed");
        assertNull(map.get("c"), "Map must no longer contain the removed key");
        assertFalse(descendingKeySet.contains("c"), "View must no longer contain the removed key");
    }

    @Test
    void descendingKeySet_unsupportedAddOperation() {
        // The key set view returned by ConcurrentNavigableMap is read‑only for additions.
        // Attempting to add should throw UnsupportedOperationException.
        assertThrows(UnsupportedOperationException.class, () -> descendingKeySet.add("z"), "Adding a key directly to the descending key set must not be supported");
    }

    @Test
    void descendingKeySet_isConsistentWithDescendingMap() {
        // The contract of descendingKeySet() is that it returns
        // descendingMap().navigableKeySet().
        ConcurrentNavigableMap<String, Integer> descendingMap = map.descendingMap();
        NavigableSet<String> expectedSet = descendingMap.navigableKeySet();
        assertEquals(expectedSet, descendingKeySet, "descendingKeySet() must be identical to descendingMap().navigableKeySet()");
    }

    @Test
    void descendingKeySet_emptyMap_returnsEmptySet() {
        // Create a brand‑new map with no entries.
        ConcurrentNavigableMapNullSafe<Integer, String> emptyMap = new ConcurrentNavigableMapNullSafe<>();
        NavigableSet<Integer> emptySet = emptyMap.descendingKeySet();
        assertNotNull(emptySet, "The returned set should never be null");
        assertTrue(emptySet.isEmpty(), "NavigableSet is empty (size == 0) for an empty map");
        assertEquals(0, emptySet.size(), "Size must be zero");
    }

    @ParameterizedTest(name = "[{index}] descendingKeySet on natural‑order map")
    @MethodSource("naturalOrderMapProvider")
    void descendingKeySet_nonEmptyMap_naturalOrder(ConcurrentNavigableMapNullSafe<Integer, String> intMap) {
        // Verify that the iterator yields keys in reverse natural order: 3, 2, 1.
        NavigableSet<Integer> dSet = intMap.descendingKeySet();
        Iterator<Integer> it = dSet.iterator();
        assertEquals(3, it.next());
        assertEquals(2, it.next());
        assertEquals(1, it.next());
        assertFalse(it.hasNext(), "No further elements expected");
    }

    @Test
    void descendingKeySet_liveViewReflectsAddition() {
        // Obtain the view first.
        // map from @BeforeEach is String‑based, create a fresh int map.
        NavigableSet<Integer> dSet = map.descendingKeySet();
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "one");
        intMap.put(2, "two");
        intMap.put(3, "three");
        NavigableSet<Integer> view = intMap.descendingKeySet();
        // Add a new entry after the view has been created.
        intMap.put(4, "four");
        // The new key must appear at the beginning of the descending view.
        Iterator<Integer> it = view.iterator();
        assertEquals(4, it.next(), "Newly added key (4) must be first in descending order");
        assertEquals(3, it.next());
        assertEquals(2, it.next());
        assertEquals(1, it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void descendingKeySet_liveViewReflectsRemoval() {
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "one");
        intMap.put(2, "two");
        intMap.put(3, "three");
        NavigableSet<Integer> view = intMap.descendingKeySet();
        // Remove key 2 from the underlying map.
        intMap.remove(2);
        // The view must no longer contain 2.
        assertFalse(view.contains(2), "Removed key must disappear from descending view");
        // Remaining order should be 3, 1.
        Iterator<Integer> it = view.iterator();
        assertEquals(3, it.next());
        assertEquals(1, it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void descendingKeySet_respectsCustomComparator() {
        // Create a map with a reverse‑order comparator.
        ConcurrentNavigableMapNullSafe<Integer, String> revMap = new ConcurrentNavigableMapNullSafe<>(Collections.reverseOrder());
        revMap.put(1, "one");
        revMap.put(2, "two");
        revMap.put(3, "three");
        // With a reverse comparator, the natural order of the map is 3,2,1.
        // The descending view reverses this again, yielding natural order 1,2,3.
        NavigableSet<Integer> dSet = revMap.descendingKeySet();
        Iterator<Integer> it = dSet.iterator();
        assertEquals(1, it.next(), "First element should be 1 (natural order)");
        assertEquals(2, it.next(), "Second element should be 2");
        assertEquals(3, it.next(), "Third element should be 3");
        assertFalse(it.hasNext(), "No more elements expected");
    }

    @Test
    void descendingKeySet_nullKeyIsAllowedAndAppearsFirst() {
        // The map under test already contains a null key from @BeforeEach.
        // Verify that the descending view starts with null.
        Iterator<String> it = descendingKeySet.iterator();
        assertTrue(it.hasNext(), "Descending set must contain at least the null key");
        assertNull(it.next(), "Null key must be the first element in descending order");
    }

    @Test
    void descendingKeySet_iteratorRemoveDeletesEntryFromMap() {
        // Use the map prepared in @BeforeEach (contains null, a, b, c).
        Iterator<String> it = descendingKeySet.iterator();
        // First element is null – remove it via iterator.
        assertNull(it.next(), "First element should be null");
        // should delete the null entry from the map.
        it.remove();
        // Verify removal propagated to the map.
        assertNull(map.get(null), "Map must no longer contain the null key after iterator.remove()");
        assertFalse(descendingKeySet.contains(null), "Descending view must no longer contain null");
        // Also verify that subsequent iteration works correctly.
        assertEquals("c", it.next(), "Next element after removal should be 'c'");
    }
}
