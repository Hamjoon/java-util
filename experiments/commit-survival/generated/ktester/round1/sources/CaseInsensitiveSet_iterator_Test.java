package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.concurrent.ConcurrentModificationException;
import java.util.stream.Stream;

class CaseInsensitiveSet_iterator_Test {

    private CaseInsensitiveSet<String> stringSet;

    private CaseInsensitiveSet<Integer> intSet;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh sets for each test to avoid cross‑test contamination.
        stringSet = new CaseInsensitiveSet<>();
        intSet = new CaseInsensitiveSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicit clear is not required but demonstrates good hygiene.
        if (stringSet != null) {
            stringSet.clear();
        }
        if (intSet != null) {
            intSet.clear();
        }
    }

    private static Stream<Arguments> emptySetProvider() {
        return Stream.of(Arguments.of(new CaseInsensitiveSet<String>()), Arguments.of(new CaseInsensitiveSet<Integer>()));
    }

    @Test
    void iteratorReturnsOriginalCaseStrings() {
        // GIVEN
        stringSet.add("Apple");
        stringSet.add("banana");
        stringSet.add("Cherry");
        // WHEN
        List<String> iterated = new ArrayList<>();
        for (Iterator<String> it = stringSet.iterator(); it.hasNext(); ) {
            iterated.add(it.next());
        }
        // THEN
        // The iterator must contain exactly the three elements added,
        // preserving the original case. Order is not guaranteed because the
        // default backing map is a hash‑based map.
        assertEquals(3, iterated.size(), "Iterator should return three elements");
        assertTrue(iterated.containsAll(Arrays.asList("Apple", "banana", "Cherry")), "Iterator must contain the original strings with original case");
    }

    @Test
    void iteratorWorksForNonStringElements() {
        // GIVEN
        intSet.add(10);
        intSet.add(20);
        intSet.add(30);
        // WHEN
        Set<Integer> collected = new HashSet<>();
        for (Iterator<Integer> it = intSet.iterator(); it.hasNext(); ) {
            collected.add(it.next());
        }
        // THEN
        assertEquals(Set.of(10, 20, 30), collected, "Iterator should return all integer elements added to the set");
    }

    @Test
    void iteratorReflectsCaseInsensitiveBehaviour() {
        // GIVEN
        stringSet.add("Test");
        // Adding a different‑cased version should not create a new entry.
        stringSet.add("test");
        // WHEN
        List<String> iterated = new ArrayList<>();
        for (Iterator<String> it = stringSet.iterator(); it.hasNext(); ) {
            iterated.add(it.next());
        }
        // THEN
        // Only one element should be present, and it must be the first one added
        // ("Test") preserving its original case.
        assertEquals(1, iterated.size(), "Case‑insensitive set should contain a single element after adding \"Test\" and \"test\"");
        assertEquals("Test", iterated.get(0), "The iterator must return the original element that was first added");
    }

    /**
     * Group: iterator basic functionality – empty set.
     */
    @Test
    void iteratorOnEmptySetIsNonNullAndHasNoElements() {
        // GIVEN – a freshly created set (already done in @BeforeEach)
        // WHEN
        Iterator<String> it = stringSet.iterator();
        // THEN
        assertNotNull(it, "Iterator should never be null");
        assertFalse(it.hasNext(), "Empty set iterator must report no next element");
        assertThrows(NoSuchElementException.class, it::next, "Calling next() on an empty iterator must throw NoSuchElementException");
    }

    /**
     * Group: iterator basic functionality – sorted order.
     * The underlying implementation uses a case‑insensitive {@link ConcurrentSkipListMap},
     * therefore iteration order should be alphabetical ignoring case.
     */
    @Test
    void iteratorYieldsElementsInCaseInsensitiveSortedOrder() {
        // GIVEN
        stringSet.add("banana");
        stringSet.add("Apple");
        stringSet.add("Cherry");
        // WHEN
        List<String> iterated = new ArrayList<>();
        for (Iterator<String> it = stringSet.iterator(); it.hasNext(); ) {
            iterated.add(it.next());
        }
        // THEN
        List<String> expected = Arrays.asList("Apple", "banana", "Cherry");
        assertEquals(expected, iterated, "Iterator should return elements sorted case‑insensitively");
    }

    /**
     * Group: iterator basic functionality – remove via iterator.
     */
    @Test
    void iteratorRemoveDeletesLastReturnedElement() {
        // GIVEN
        stringSet.add("One");
        stringSet.add("Two");
        stringSet.add("Three");
        // WHEN
        Iterator<String> it = stringSet.iterator();
        while (it.hasNext()) {
            String val = it.next();
            if ("Two".equals(val)) {
                // remove the element we just visited
                it.remove();
                break;
            }
        }
        // THEN
        assertFalse(stringSet.contains("Two"), "Element removed via iterator must no longer be present");
        assertEquals(2, stringSet.size(), "Size must decrease after iterator.remove()");
    }

    /**
     * Group: iterator basic functionality – illegal remove before next().
     */
    @Test
    void iteratorRemoveBeforeNextThrowsIllegalStateException() {
        // GIVEN
        stringSet.add("Alpha");
        // WHEN / THEN
        Iterator<String> it = stringSet.iterator();
        assertThrows(IllegalStateException.class, it::remove, "Calling remove() before next() must throw IllegalStateException");
    }

    /**
     * Group: iterator basic functionality – concurrent modification after iterator creation.
     */
    @Test
    void iteratorDetectsConcurrentModificationOnNext() {
        // GIVEN
        stringSet.add("A");
        stringSet.add("B");
        Iterator<String> it = stringSet.iterator();
        // Modify the set after iterator creation
        stringSet.add("C");
        // WHEN / THEN
        assertThrows(ConcurrentModificationException.class, it::next, "Iterator must be fail‑fast and throw CME on next() after external modification");
    }

    /**
     * Group: iterator edge cases – single element set.
     */
    @Test
    void iteratorSingleElementBehaviour() {
        // GIVEN
        stringSet.add("Solo");
        // WHEN
        Iterator<String> it = stringSet.iterator();
        // THEN
        assertTrue(it.hasNext(), "hasNext() should be true for a set with one element");
        assertEquals("Solo", it.next(), "next() must return the sole element");
        assertFalse(it.hasNext(), "After consuming the only element, hasNext() must be false");
    }

    /**
     * Group: iterator edge cases – NoSuchElementException after exhaustion.
     */
    @Test
    void iteratorNextAfterExhaustionThrowsNoSuchElementException() {
        // GIVEN
        stringSet.add("X");
        Iterator<String> it = stringSet.iterator();
        // consume the only element
        it.next();
        // WHEN / THEN
        assertThrows(NoSuchElementException.class, it::next, "Calling next() after all elements are consumed must throw NoSuchElementException");
    }

    /**
     * Group: iterator edge cases – duplicate case‑insensitive values.
     */
    @Test
    void iteratorShowsOnlyOneEntryForCaseInsensitiveDuplicates() {
        // GIVEN
        stringSet.add("test");
        // duplicate in different case
        stringSet.add("TEST");
        // WHEN
        List<String> iterated = new ArrayList<>();
        for (Iterator<String> it = stringSet.iterator(); it.hasNext(); ) {
            iterated.add(it.next());
        }
        // THEN
        assertEquals(1, iterated.size(), "Only one element should be present when case‑insensitive duplicates are added");
        assertEquals("test", iterated.get(0), "The iterator must return the first inserted value preserving its original case");
    }

    /**
     * Group: iterator edge cases – double remove on same iterator element.
     */
    @Test
    void iteratorRemoveTwiceThrowsIllegalStateException() {
        // GIVEN
        stringSet.add("First");
        stringSet.add("Second");
        Iterator<String> it = stringSet.iterator();
        // Consume first element and remove it
        it.next();
        it.remove();
        // Second remove without another next()
        assertThrows(IllegalStateException.class, it::remove, "Calling remove() twice consecutively must throw IllegalStateException");
    }

    /**
     * Group: iterator edge cases – iterator after clear().
     */
    @Test
    void iteratorAfterClearReportsEmpty() {
        // GIVEN
        stringSet.add("A");
        stringSet.add("B");
        stringSet.clear();
        // WHEN
        Iterator<String> it = stringSet.iterator();
        // THEN
        assertFalse(it.hasNext(), "Iterator created after clear() must report no elements");
        assertThrows(NoSuchElementException.class, it::next, "next() on an iterator of a cleared set must throw NoSuchElementException");
    }

    /**
     * Group: iterator fail‑fast on external removal.
     */
    @Test
    void iteratorDetectsConcurrentModificationOnRemoval() {
        // GIVEN
        stringSet.add("Alpha");
        stringSet.add("Beta");
        Iterator<String> it = stringSet.iterator();
        // Remove an element directly from the set
        stringSet.remove("Beta");
        // WHEN / THEN
        assertThrows(ConcurrentModificationException.class, it::next, "Iterator must detect external removal and throw CME on next()");
    }

    /**
     * Group: iterator remove after external modification.
     */
    @Test
    void iteratorRemoveAfterExternalDeletionThrowsIllegalStateException() {
        // GIVEN
        stringSet.add("One");
        stringSet.add("Two");
        Iterator<String> it = stringSet.iterator();
        // Advance to "Two"
        while (it.hasNext()) {
            String val = it.next();
            if ("Two".equals(val)) {
                break;
            }
        }
        // Remove "Two" directly from the set (outside iterator)
        stringSet.remove("Two");
        // WHEN / THEN
        assertThrows(IllegalStateException.class, it::remove, "Calling iterator.remove() after the element has been removed externally must throw IllegalStateException");
    }

    /**
     * Group: iterator concurrent modification detection on hasNext().
     */
    @Test
    void iteratorHasNextDetectsConcurrentModificationAfterAdd() {
        // GIVEN
        stringSet.add("X");
        Iterator<String> it = stringSet.iterator();
        // External addition
        stringSet.add("Y");
        // WHEN / THEN
        assertThrows(ConcurrentModificationException.class, it::hasNext, "hasNext() must be fail‑fast after external addition");
    }

    @Test
    void iteratorHasNextDetectsConcurrentModificationAfterRemove() {
        // GIVEN
        stringSet.add("X");
        stringSet.add("Y");
        Iterator<String> it = stringSet.iterator();
        // External removal
        stringSet.remove("Y");
        // WHEN / THEN
        assertThrows(ConcurrentModificationException.class, it::hasNext, "hasNext() must be fail‑fast after external removal");
    }

    @ParameterizedTest
    @MethodSource("emptySetProvider")
    void iteratorOnVariousEmptySetsIsEmpty(CaseInsensitiveSet<?> set) {
        Iterator<?> it = set.iterator();
        assertNotNull(it);
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
    }
}
