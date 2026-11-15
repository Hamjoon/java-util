package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentSkipListMap;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveSet_iterator_Test {

    /**
     * Sample data used by several tests
     */
    private List<String> sampleStrings;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a deterministic list of strings – the original case is important
        sampleStrings = Arrays.asList("Apple", "banana", "Cherry", // duplicate (different case) – should not create a new entry
        "apple", // duplicate (different case)
        "BANANA", "Durian");
    }

    /**
     * Verifies that {@link CaseInsensitiveSet#iterator()} traverses all distinct elements
     * and that the iterator returns the elements in the original case as they were added.
     */
    @Test
    void testIteratorTraversesAllElementsPreservingCase() {
        // GIVEN a set populated with a mixture of case‑variant strings
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        // duplicates are ignored by case‑insensitive logic
        set.addAll(sampleStrings);
        // WHEN we obtain an iterator and walk through the set
        Iterator<String> it = set.iterator();
        List<String> iterated = new ArrayList<>();
        while (it.hasNext()) {
            iterated.add(it.next());
        }
        // THEN the size reported by the set equals the number of distinct case‑insensitive values
        assertEquals(4, set.size(), "Set should contain 4 distinct values");
        // AND the iterator has visited exactly those distinct values
        assertTrue(iterated.containsAll(Arrays.asList("Apple", "banana", "Cherry", "Durian")), "Iterator must contain the original‑case elements");
        // AND no duplicate (case‑variant) entries are present
        assertEquals(4, iterated.size(), "Iterator should yield exactly 4 elements");
    }

    /**
     * Verifies that the iteration order depends on the backing map implementation.
     * Using a {@link java.util.LinkedHashMap} preserves insertion order,
     * while the default {@link CaseInsensitiveMap} (backed by a plain HashMap) does not guarantee order.
     */
    @Test
    void testIteratorOrderDiffersWithDifferentBackingMaps() {
        // GIVEN two sets with identical contents but different backing maps
        Set<String> unordered = new CaseInsensitiveSet<>(Collections.emptySet(), // unordered backing map
        new CaseInsensitiveMap<>(Collections.emptyMap(), new HashMap<>()));
        Set<String> ordered = new CaseInsensitiveSet<>(Collections.emptySet(), // ordered backing map
        new CaseInsensitiveMap<>(Collections.emptyMap(), new LinkedHashMap<>()));
        unordered.addAll(sampleStrings);
        ordered.addAll(sampleStrings);
        // Sanity check – both sets are equal as collections
        assertEquals(unordered, ordered, "Both sets should be equal as collections");
        // WHEN we iterate over both sets
        Iterator<String> itUnordered = unordered.iterator();
        Iterator<String> itOrdered = ordered.iterator();
        // THEN we expect at least one position where the order differs
        boolean orderDiffers = false;
        while (itUnordered.hasNext() && itOrdered.hasNext()) {
            String u = itUnordered.next();
            String o = itOrdered.next();
            if (!Objects.equals(u, o)) {
                orderDiffers = true;
                break;
            }
        }
        // If one iterator finishes earlier, the sizes differ – also considered a difference
        if (itUnordered.hasNext() != itOrdered.hasNext()) {
            orderDiffers = true;
        }
        assertTrue(orderDiffers, "Iteration order should differ between unordered and ordered backing maps");
    }

    /**
     * iterator() – basic functionality: empty set.
     * The iterator should have no elements and {@code hasNext()} must return {@code false}.
     */
    @Test
    void testIteratorEmptySet() {
        CaseInsensitiveSet<String> empty = new CaseInsensitiveSet<>();
        Iterator<String> it = empty.iterator();
        assertFalse(it.hasNext(), "Iterator over an empty set must report hasNext() == false");
        assertThrows(NoSuchElementException.class, it::next, "Calling next() on empty iterator should throw");
    }

    /**
     * iterator() – basic functionality with elements.
     * Adding mixed‑case strings should result in distinct case‑insensitive keys.
     * The iterator must return each distinct key exactly once.
     */
    @Test
    void testIteratorWithMixedCaseElements() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        List<String> input = Arrays.asList("Apple", "Banana", "apple");
        set.addAll(input);
        // Expected distinct values: "Apple" (covers "apple") and "Banana"
        assertEquals(2, set.size(), "Set should contain 2 distinct values after case‑insensitive addition");
        List<String> iterated = new ArrayList<>();
        for (Iterator<String> it = set.iterator(); it.hasNext(); ) {
            iterated.add(it.next());
        }
        assertTrue(iterated.containsAll(Arrays.asList("Apple", "Banana")), "Iterator must contain the original‑case elements for each distinct key");
        assertEquals(2, iterated.size(), "Iterator should yield exactly 2 elements");
    }

    /**
     * iterator() – remove operation.
     * The iterator returned by {@code iterator()} must support {@code remove()}
     * and correctly update the underlying set.
     */
    @Test
    void testIteratorRemoveUpdatesSet() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        set.addAll(Arrays.asList("One", "Two", "Three"));
        Iterator<String> it = set.iterator();
        assertTrue(it.hasNext(), "Iterator should have at least one element");
        // capture element to be removed
        String first = it.next();
        // remove the element just returned
        it.remove();
        // After removal the set size must be reduced and the removed element absent
        assertEquals(2, set.size(), "Set size should be 2 after removing one element via iterator");
        assertFalse(set.contains(first), "Set must no longer contain the removed element");
        // Remaining elements should be the other two (order not important)
        assertTrue(set.containsAll(Arrays.asList("Two", "Three")), "Remaining elements must be present");
    }

    /**
     * iterator() – null element handling.
     * When the underlying map permits {@code null} keys, the iterator must return the {@code null}
     * entry without throwing.
     */
    @Test
    void testIteratorHandlesNullElement() {
        // Use a backing map that allows null keys (HashMap does)
        CaseInsensitiveSet<Object> set = new CaseInsensitiveSet<>(Collections.emptySet(), new CaseInsensitiveMap<>(Collections.emptyMap(), new HashMap<>()));
        set.add(null);
        set.add("Alpha");
        List<Object> iterated = new ArrayList<>();
        for (Iterator<Object> it = set.iterator(); it.hasNext(); ) {
            iterated.add(it.next());
        }
        assertEquals(2, iterated.size(), "Iterator should return two elements (null and \"Alpha\")");
        assertTrue(iterated.contains(null), "Iterator must contain the null element");
        assertTrue(iterated.contains("Alpha"), "Iterator must contain the non‑null element");
    }

    /**
     * iterator() – case‑insensitive alphabetical ordering.
     * When a {@link TreeMap} with {@link String#CASE_INSENSITIVE_ORDER} is used as the backing map,
     * the iterator must follow that sorted order.
     */
    @Test
    void testIteratorCaseInsensitiveAlphabeticalOrder() {
        // TreeMap with case‑insensitive comparator provides sorted order
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(Collections.emptySet(), new CaseInsensitiveMap<>(Collections.emptyMap(), new TreeMap<>(String.CASE_INSENSITIVE_ORDER)));
        set.addAll(Arrays.asList("banana", "Apple", "cherry"));
        List<String> iterated = new ArrayList<>();
        for (Iterator<String> it = set.iterator(); it.hasNext(); ) {
            iterated.add(it.next());
        }
        // Expected order: Apple, banana, cherry (alphabetical ignoring case)
        List<String> expected = Arrays.asList("Apple", "banana", "cherry");
        assertEquals(expected, iterated, "Iterator must follow case‑insensitive alphabetical order");
    }

    /**
     * iterator() – weakly consistent behavior on concurrent modification.
     * Using a {@link ConcurrentSkipListMap} as backing map yields a weakly consistent iterator
     * that tolerates modifications after its creation.
     */
    @Test
    void testIteratorWeaklyConsistentDuringConcurrentModification() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(Collections.emptySet(), new CaseInsensitiveMap<>(Collections.emptyMap(), new ConcurrentSkipListMap<>()));
        set.addAll(Arrays.asList("first", "second"));
        // capture iterator before modification
        Iterator<String> it = set.iterator();
        // concurrent modification
        set.add("third");
        List<String> iterated = new ArrayList<>();
        while (it.hasNext()) {
            iterated.add(it.next());
        }
        // The iterator should not throw and must contain at least the original two elements.
        assertTrue(iterated.containsAll(Arrays.asList("first", "second")), "Iterator must contain the elements that existed when it was created");
        // The newly added element may or may not appear; we only assert no exception occurred.
        assertEquals(2, iterated.size(), "Weakly‑consistent iterator should yield exactly the elements present at creation time");
    }

    /**
     * iterator() – independent iterators.
     * Two iterators obtained sequentially must operate independently; advancing one must not affect the other.
     */
    @Test
    void testIteratorsAreIndependent() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        set.addAll(Arrays.asList("A", "B", "C"));
        Iterator<String> it1 = set.iterator();
        Iterator<String> it2 = set.iterator();
        // Advance first iterator by one element
        assertTrue(it1.hasNext());
        String firstFromIt1 = it1.next();
        // Second iterator should still start at the beginning
        assertTrue(it2.hasNext());
        String firstFromIt2 = it2.next();
        assertEquals(firstFromIt1, firstFromIt2, "Both iterators start at the same first element");
        // Continue iterating both and ensure they each see all elements
        List<String> remaining1 = new ArrayList<>();
        while (it1.hasNext()) {
            remaining1.add(it1.next());
        }
        List<String> remaining2 = new ArrayList<>();
        while (it2.hasNext()) {
            remaining2.add(it2.next());
        }
        // Both should have seen the same remaining elements
        assertEquals(remaining1, remaining2, "Both iterators must see identical remaining elements");
    }

    /**
     * iterator() – null internal map handling.
     * By forcing the private {@code map} field to {@code null} via reflection,
     * invoking {@code iterator()} must throw a {@link NullPointerException}.
     */
    @Test
    void testIteratorThrowsWhenInternalMapIsNull() throws Exception {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        // Use reflection to set the private 'map' field to null
        Field mapField = CaseInsensitiveSet.class.getDeclaredField("map");
        mapField.setAccessible(true);
        mapField.set(set, null);
        // Expect NPE when iterator() tries to dereference the null map
        assertThrows(NullPointerException.class, set::iterator, "Calling iterator() with a null internal map should throw NullPointerException");
    }
}
