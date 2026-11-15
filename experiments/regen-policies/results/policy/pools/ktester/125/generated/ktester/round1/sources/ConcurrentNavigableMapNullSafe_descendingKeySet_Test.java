package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.ConcurrentNavigableMap;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_descendingKeySet_Test {

    private static ConcurrentNavigableMapNullSafe<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh map for each test case – default natural ordering
        map = new ConcurrentNavigableMapNullSafe<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // null key is considered last in ascending order,
        map.put(null, 100);
        // thus first in descending order
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to avoid cross‑test contamination
        map.clear();
    }

    @Test
    void descendingKeySet_containsAllKeysInCorrectOrder() {
        NavigableSet<String> descendingSet = map.descendingKeySet();
        // The set must contain exactly the same keys as the map
        assertEquals(4, descendingSet.size());
        assertTrue(descendingSet.contains("a"));
        assertTrue(descendingSet.contains("b"));
        assertTrue(descendingSet.contains("c"));
        assertTrue(descendingSet.contains(null));
        // Verify ordering: null first, then "c", "b", "a"
        Iterator<String> iterator = descendingSet.iterator();
        assertNull(iterator.next(), "Null key should be first in descending order");
        assertEquals("c", iterator.next());
        assertEquals("b", iterator.next());
        assertEquals("a", iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    void descendingKeySet_reflectsChangesInUnderlyingMap() {
        NavigableSet<String> descendingSet = map.descendingKeySet();
        // Remove a key via the set view
        assertTrue(descendingSet.remove("b"));
        assertFalse(map.containsKey("b"));
        assertEquals(3, map.size());
        // Add a new key via the map and ensure the set sees it
        map.put("d", 4);
        assertTrue(descendingSet.contains("d"));
        // New ordering should be: null, d, c, a
        Iterator<String> it = descendingSet.iterator();
        assertNull(it.next());
        assertEquals("d", it.next());
        assertEquals("c", it.next());
        assertEquals("a", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void descendingKeySet_isUnmodifiableViaAdd() {
        NavigableSet<String> descendingSet = map.descendingKeySet();
        // The set view of keys should not support add operations
        assertThrows(UnsupportedOperationException.class, () -> descendingSet.add("z"));
    }

    @Test
    void descendingKeySet_handlesEmptyMap() {
        map.clear();
        NavigableSet<String> descendingSet = map.descendingKeySet();
        assertTrue(descendingSet.isEmpty(), "Descending key set of an empty map must be empty");
    }

    @Test
    void descendingKeySet_naturalOrderReversal_withoutNull() {
        // Build a map that does NOT contain a null key
        map.clear();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        NavigableSet<String> descendingSet = map.descendingKeySet();
        // Expected order: c, b, a
        List<String> expected = List.of("c", "b", "a");
        assertEquals(expected, new ArrayList<>(descendingSet), "Keys must appear in strict descending order");
    }

    @Test
    void descendingKeySet_liveViewRemovalViaIterator() {
        NavigableSet<String> descendingSet = map.descendingKeySet();
        Iterator<String> it = descendingSet.iterator();
        // iterator.next() -> null (first element)
        assertNull(it.next());
        // iterator.next() -> "c"
        assertEquals("c", it.next());
        // Remove "c" through iterator
        it.remove();
        // Map must no longer contain "c"
        assertFalse(map.containsKey("c"));
        // Set size must have decreased
        assertEquals(3, descendingSet.size());
        // Verify new ordering: null, b, a
        List<String> expected = List.of(null, "b", "a");
        assertEquals(expected, new ArrayList<>(descendingSet));
    }

    @Test
    void descendingKeySet_doubleDescendingRestoresAscendingOrder() {
        NavigableSet<String> descendingSet = map.descendingKeySet();
        // back to ascending view
        NavigableSet<String> doubleDesc = descendingSet.descendingSet();
        // Expected ascending order: a, b, c, null
        List<String> expected = List.of("a", "b", "c", null);
        assertEquals(expected, new ArrayList<>(doubleDesc));
    }

    @Test
    void descendingKeySet_customComparatorOrder() {
        // Comparator: shorter strings first; if equal length, natural order
        Comparator<String> lengthComparator = Comparator.comparingInt(String::length).thenComparing(Comparator.naturalOrder());
        // Create a new map with the custom comparator
        map = new ConcurrentNavigableMapNullSafe<>(lengthComparator);
        // length 1
        map.put("a", 1);
        // length 2
        map.put("bb", 2);
        // length 1
        map.put("c", 3);
        NavigableSet<String> descendingSet = map.descendingKeySet();
        // Expected descending order: longest to shortest -> "bb", "c", "a"
        List<String> expected = List.of("bb", "c", "a");
        assertEquals(expected, new ArrayList<>(descendingSet));
    }

    @Test
    void descendingKeySet_containsAndBoundaryMethods() {
        NavigableSet<String> descendingSet = map.descendingKeySet();
        assertTrue(descendingSet.contains("b"));
        assertFalse(descendingSet.contains("z"));
        // In descending view, first() should be the *largest* key (null is first)
        assertNull(descendingSet.first());
        // last() should be the *smallest* key (i.e., "a")
        assertEquals("a", descendingSet.last());
    }

    @Test
    void descendingKeySet_clearOperationUnsupported() {
        NavigableSet<String> descendingSet = map.descendingKeySet();
        assertThrows(UnsupportedOperationException.class, descendingSet::clear);
    }

    @Test
    void descendingKeySet_subSetViewConsistency() {
        // Subset from "c" (inclusive) to "a" (exclusive) in descending view
        NavigableSet<String> descendingSet = map.descendingKeySet();
        NavigableSet<String> sub = descendingSet.subSet("c", true, "a", false);
        // Expected content: only "b"
        List<String> expected = List.of("b");
        assertEquals(expected, new ArrayList<>(sub));
    }

    @Test
    void descendingKeySet_addAllUnsupported() {
        NavigableSet<String> descendingSet = map.descendingKeySet();
        Collection<String> toAdd = List.of("x", "y");
        assertThrows(UnsupportedOperationException.class, () -> descendingSet.addAll(toAdd));
    }

    @Test
    void descendingKeySet_removeAllUnsupported() {
        NavigableSet<String> descendingSet = map.descendingKeySet();
        Collection<String> toRemove = List.of("a", "b");
        assertThrows(UnsupportedOperationException.class, () -> descendingSet.removeAll(toRemove));
    }

    @Test
    void descendingKeySet_retainAllUnsupported() {
        NavigableSet<String> descendingSet = map.descendingKeySet();
        Collection<String> toRetain = List.of("a");
        assertThrows(UnsupportedOperationException.class, () -> descendingSet.retainAll(toRetain));
    }

    @Test
    void descendingKeySet_iteratorRemoveWithoutNextThrows() {
        NavigableSet<String> descendingSet = map.descendingKeySet();
        Iterator<String> it = descendingSet.iterator();
        assertThrows(IllegalStateException.class, it::remove);
    }

    @Test
    void descendingKeySet_firstOnEmptyMapThrows() {
        map.clear();
        NavigableSet<String> descendingSet = map.descendingKeySet();
        assertThrows(NoSuchElementException.class, descendingSet::first);
    }

    @Test
    void descendingKeySet_lastOnEmptyMapThrows() {
        map.clear();
        NavigableSet<String> descendingSet = map.descendingKeySet();
        assertThrows(NoSuchElementException.class, descendingSet::last);
    }

    @Test
    void descendingKeySet_pollFirstUnsupported() {
        NavigableSet<String> descendingSet = map.descendingKeySet();
        assertThrows(UnsupportedOperationException.class, descendingSet::pollFirst);
    }

    @Test
    void descendingKeySet_pollLastUnsupported() {
        NavigableSet<String> descendingSet = map.descendingKeySet();
        assertThrows(UnsupportedOperationException.class, descendingSet::pollLast);
    }
}
