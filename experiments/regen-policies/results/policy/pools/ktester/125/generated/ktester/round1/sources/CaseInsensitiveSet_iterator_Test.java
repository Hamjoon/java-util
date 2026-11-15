package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveSet_iterator_Test {

    private CaseInsensitiveSet<Object> mixedSet;

    private Set<String> unorderedSet;

    private Set<String> orderedSet;

    @BeforeEach
    void setupBeforeEach() {
        // A set containing both String and non‑String elements.
        mixedSet = new CaseInsensitiveSet<>();
        // Two sets that share the same elements but use different backing maps.
        unorderedSet = new CaseInsensitiveSet<>(Collections.emptySet(), new CaseInsensitiveMap<>(Collections.emptyMap(), new HashMap<>()));
        orderedSet = new CaseInsensitiveSet<>(Collections.emptySet(), new CaseInsensitiveMap<>(Collections.emptyMap(), new LinkedHashMap<>()));
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear mutable state to avoid cross‑test interference.
        mixedSet.clear();
        unorderedSet.clear();
        orderedSet.clear();
    }

    /**
     * --------------------------------------------------------------------
     *  Test that the iterator traverses all elements and respects the
     *  original case of String entries.
     *  -------------------------------------------------------------------
     */
    @Test
    void iteratorTraversesAllElementsPreservingCase() {
        // Populate the set with mixed‑case strings and a non‑String element.
        mixedSet.add("One");
        mixedSet.add("two");
        mixedSet.add("THREE");
        mixedSet.add(42);
        // Expected elements in insertion order (the default backing map preserves order).
        List<Object> expected = Arrays.asList("One", "two", "THREE", 42);
        Iterator<Object> it = mixedSet.iterator();
        List<Object> actual = new ArrayList<>();
        while (it.hasNext()) {
            actual.add(it.next());
        }
        assertEquals(expected.size(), actual.size(), "Iterator should return the same number of elements as the set size");
        assertEquals(expected, actual, "Iterator must preserve the original case and insertion order");
    }

    /**
     * --------------------------------------------------------------------
     *  Test iterator on an empty set – hasNext should be false immediately.
     *  -------------------------------------------------------------------
     */
    @Test
    void iteratorOnEmptySet() {
        CaseInsensitiveSet<String> empty = new CaseInsensitiveSet<>();
        Iterator<String> it = empty.iterator();
        assertFalse(it.hasNext(), "Iterator of an empty set must report no next element");
    }

    /**
     * --------------------------------------------------------------------
     *  Verify that different backing maps can lead to different iteration
     *  orderings.  The unordered set uses a plain HashMap (no ordering guarantee)
     *  while the ordered set uses a LinkedHashMap (insertion order).
     *  -------------------------------------------------------------------
     */
    @Test
    void iteratorOrderDiffersWithDifferentBackingMaps() {
        String[] strings = { "alpha", "beta", "gamma", "delta", "epsilon" };
        // Populate both sets with the same collection.
        unorderedSet.addAll(Arrays.asList(strings));
        orderedSet.addAll(Arrays.asList(strings));
        // The logical equality (ignoring order) must hold.
        assertTrue(orderedSet.equals(unorderedSet), "Both sets contain the same elements, so equals() should be true");
        // Compare iteration order.
        Iterator<String> unorderedIt = unorderedSet.iterator();
        Iterator<String> orderedIt = orderedSet.iterator();
        boolean orderDiffers = false;
        while (unorderedIt.hasNext() && orderedIt.hasNext()) {
            String fromUnordered = unorderedIt.next();
            String fromOrdered = orderedIt.next();
            if (!Objects.equals(fromUnordered, fromOrdered)) {
                orderDiffers = true;
                break;
            }
        }
        // If the unordered set truly has no ordering guarantee, at least one run
        // should produce a different order.  The test is written defensively:
        // we assert that a difference *was observed*.
        assertTrue(orderDiffers, "Iteration order should differ between a HashMap‑backed set and a LinkedHashMap‑backed set");
    }

    /* --------------------------------------------------------------------
     *  2. Non‑empty Set Iteration (case‑insensitive distinct elements)
     * -------------------------------------------------------------------- */
    @Test
    void iteratorReturnsDistinctCaseInsensitiveElements() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        // "Apple" and "apple" are considered the same element.
        set.add("Apple");
        set.add("apple");
        set.add("Banana");
        // The set should contain exactly two distinct values.
        assertEquals(2, set.size(), "Set size must reflect case‑insensitive uniqueness");
        Iterator<String> it = set.iterator();
        List<String> iterated = new ArrayList<>();
        while (it.hasNext()) {
            iterated.add(it.next());
        }
        // Both distinct values must appear, order is implementation‑defined.
        assertTrue(iterated.containsAll(Arrays.asList("Apple", "Banana")), "Iterator must return both distinct elements");
        assertEquals(2, iterated.size(), "Iterator must yield each element exactly once");
    }

    /* --------------------------------------------------------------------
     *  3. Iterator Remove Operation
     * -------------------------------------------------------------------- */
    @Test
    void iteratorRemoveDeletesElementFromSet() {
        CaseInsensitiveSet<Integer> set = new CaseInsensitiveSet<>();
        set.addAll(Arrays.asList(1, 2, 3));
        Iterator<Integer> it = set.iterator();
        assertTrue(it.hasNext(), "Iterator should have a first element");
        Integer first = it.next();
        assertEquals(1, first, "First element (in insertion order) should be 1");
        // remove the element we just visited
        it.remove();
        assertEquals(2, set.size(), "Size must decrease after removal");
        assertFalse(set.contains(1), "Removed element must no longer be present");
        // a fresh iterator must see only the remaining elements
        List<Integer> remaining = new ArrayList<>();
        for (Integer i : set) {
            remaining.add(i);
        }
        assertEquals(Arrays.asList(2, 3), remaining, "Remaining elements after removal should be 2 and 3");
    }

    /* --------------------------------------------------------------------
     *  4. Iterator Reflects Modifications (Weak Consistency)
     * -------------------------------------------------------------------- */
    @Test
    void iteratorIsWeaklyConsistentDuringConcurrentModification() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        set.addAll(Arrays.asList("A", "B"));
        Iterator<String> it = set.iterator();
        // modify the set after the iterator has been created
        set.add("C");
        // The iterator must not throw CME and must return one of the original elements.
        assertTrue(it.hasNext(), "Iterator should still have elements");
        String next = it.next();
        assertTrue(Arrays.asList("A", "B").contains(next), "Iterator must return an element that existed at creation time");
        // Exhaust the iterator – it may or may not see "C".
        while (it.hasNext()) {
            it.next();
        }
        // A new iterator must see the newly added element.
        assertTrue(set.iterator().hasNext(), "A fresh iterator must see the element added after the first iterator was created");
        assertTrue(set.contains("C"), "Set must contain the newly added element");
    }

    /* --------------------------------------------------------------------
     *  5. Null Element Iteration
     * -------------------------------------------------------------------- */
    @Test
    void iteratorHandlesNullElement() {
        CaseInsensitiveSet<Object> set = new CaseInsensitiveSet<>();
        // The implementation permits null (the underlying map is a regular HashMap).
        set.add(null);
        Iterator<Object> it = set.iterator();
        assertTrue(it.hasNext(), "Iterator must report a next element when null is present");
        Object element = it.next();
        assertNull(element, "The element returned by next() should be null");
        assertFalse(it.hasNext(), "Iterator must be exhausted after the sole null element");
    }

    /* --------------------------------------------------------------------
     *  6. Iterator Order Consistency (Case‑Insensitive Sorted Order)
     * -------------------------------------------------------------------- */
    @Test
    void iteratorRespectsCaseInsensitiveSortedBackingMap() throws Exception {
        // Create a set backed by a TreeMap that uses CASE_INSENSITIVE_ORDER.
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        replaceBackingMapWithTreeMap(set, String.CASE_INSENSITIVE_ORDER);
        set.addAll(Arrays.asList("banana", "Apple", "cherry"));
        // Expected order: Apple, banana, cherry (case‑insensitive natural order)
        List<String> expected = Arrays.asList("Apple", "banana", "cherry");
        List<String> actual = new ArrayList<>();
        for (String s : set) {
            actual.add(s);
        }
        assertEquals(expected, actual, "Iterator must follow the ordering of the underlying case‑insensitive TreeMap");
    }

    /* --------------------------------------------------------------------
     *  Helper: replace the private 'map' field with a TreeMap using the given comparator.
     * -------------------------------------------------------------------- */
    @SuppressWarnings("unchecked")
    private void replaceBackingMapWithTreeMap(CaseInsensitiveSet<String> set, Comparator<String> comparator) throws Exception {
        // The backing map type expected by CaseInsensitiveSet is a Map<E, Object>.
        // We create a TreeMap with the supplied comparator and wrap it with a
        // CaseInsensitiveMap (the public constructor mirrors the one used in the
        // framework's examples).  If the library provides a direct constructor that
        // accepts a map, it could be used instead; reflection keeps the test
        // independent of such overloads.
        TreeMap<String, Object> treeMap = new TreeMap<>(comparator);
        CaseInsensitiveMap<String, Object> ciMap = new CaseInsensitiveMap<>(treeMap, treeMap);
        // Use reflection to set the private field.
        Field mapField = CaseInsensitiveSet.class.getDeclaredField("map");
        mapField.setAccessible(true);
        mapField.set(set, ciMap);
    }
}
