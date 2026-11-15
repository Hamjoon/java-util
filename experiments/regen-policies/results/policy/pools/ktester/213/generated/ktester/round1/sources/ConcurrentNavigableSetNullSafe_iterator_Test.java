package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_iterator_Test {

    /**
     * The set under test – created fresh before each test.
     */
    private NavigableSet<String> set;

    /**
     * Initialise a new {@link ConcurrentNavigableSetNullSafe} instance
     * and populate it with a deterministic collection of elements,
     * including a {@code null} element to verify null‑masking behaviour.
     */
    @BeforeEach
    void setupBeforeEach() {
        set = new ConcurrentNavigableSetNullSafe<>();
        // Elements are added in a non‑sorted order on purpose;
        // the iterator must return them in the natural (ascending) order
        // with the null element appearing last (as defined by the class contract).
        assertTrue(set.add("banana"));
        assertTrue(set.add("apple"));
        assertTrue(set.add(null));
        assertTrue(set.add("cherry"));
    }

    /**
     * Clean up after each test method.
     */
    @AfterEach
    void teardownAfterEach() {
        if (set != null) {
            set.clear();
        }
        set = null;
    }

    /**
     * Replaces the private {@code internalSet} field with a new
     * {@link ConcurrentSkipListSet} that uses the supplied comparator.
     * This is required because the public API of {@code ConcurrentNavigableSetNullSafe}
     * does not expose a constructor that accepts a comparator.
     */
    private void setComparator(Comparator<? super String> comparator) throws Exception {
        Field f = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        f.setAccessible(true);
        // The field is declared as final; we still can modify it via reflection for testing.
        ConcurrentSkipListSet<Object> newInternal = new ConcurrentSkipListSet<>(comparator);
        f.set(set, newInternal);
    }

    /**
     * Sets the private {@code internalSet} field to {@code null} to provoke
     * a {@link NullPointerException} when {@code iterator()} is invoked.
     */
    private void nullifyInternalSet() throws Exception {
        Field f = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        f.setAccessible(true);
        f.set(set, null);
    }

    /**
     * Verify that the iterator returns elements in the expected order:
     * natural ordering for non‑null values followed by the null sentinel.
     */
    @Test
    void testIteratorOrder() {
        Iterator<String> it = set.iterator();
        assertEquals("apple", it.next(), "First element should be the smallest string");
        assertEquals("banana", it.next(), "Second element should be the next smallest string");
        assertEquals("cherry", it.next(), "Third element should be the next smallest string");
        assertNull(it.next(), "Null element should be the last returned value");
        assertFalse(it.hasNext(), "Iterator should be exhausted");
    }

    /**
     * Ensure that {@code Iterator.remove()} correctly removes the last
     * element returned by {@code next()} and that the underlying set
     * reflects the change.
     */
    @Test
    void testIteratorRemove() {
        Iterator<String> it = set.iterator();
        // Advance to the element we intend to remove
        assertEquals("apple", it.next());
        // remove "apple"
        it.remove();
        // The set should now contain three elements, without "apple"
        assertEquals(3, set.size());
        assertFalse(set.contains("apple"));
        assertTrue(set.contains("banana"));
        assertTrue(set.contains("cherry"));
        assertTrue(set.contains(null));
        // Continue iteration to verify remaining order
        assertEquals("banana", it.next());
        assertEquals("cherry", it.next());
        assertNull(it.next());
        assertFalse(it.hasNext());
    }

    /**
     * Two independent iterators must operate on the same underlying set
     * without interfering with each other's traversal state.
     */
    @Test
    void testMultipleIteratorsIndependence() {
        Iterator<String> it1 = set.iterator();
        Iterator<String> it2 = set.iterator();
        // Advance first iterator once
        assertEquals("apple", it1.next());
        // Second iterator should still start at the beginning
        assertEquals("apple", it2.next());
        // Advance first iterator again and remove an element
        assertEquals("banana", it1.next());
        // remove "banana"
        it1.remove();
        // The set now no longer contains "banana"
        assertFalse(set.contains("banana"));
        assertEquals(3, set.size());
        // The second iterator should still see "banana" because it
        // captured the state before the removal (the underlying
        // ConcurrentSkipListSet is weakly consistent).
        assertEquals("banana", it2.next());
        // Continue both iterators to the end
        assertEquals("cherry", it1.next());
        assertEquals("cherry", it2.next());
        assertNull(it1.next());
        assertNull(it2.next());
        assertFalse(it1.hasNext());
        assertFalse(it2.hasNext());
    }

    /**
     * Populates {@code set} with the supplied elements. The {@code elements}
     * array may contain {@code null} values – the underlying class masks them
     * internally.
     */
    @SafeVarargs
    private final void addElements(Object... elements) {
        for (Object e : elements) {
            //noinspection unchecked
            set.add((String) e);
        }
    }

    /**
     * Iterator basic traversal – natural ordering.
     */
    @Test
    void iteratorBasicTraversal() {
        addElements("alpha", "bravo", "charlie");
        Iterator<String> it = set.iterator();
        assertTrue(it.hasNext());
        assertEquals("alpha", it.next());
        assertEquals("bravo", it.next());
        assertEquals("charlie", it.next());
        assertFalse(it.hasNext());
    }

    /**
     * Iterator with a null element – null should be returned last.
     */
    @Test
    void iteratorWithNullElement() {
        addElements(null, "alpha", "bravo");
        Iterator<String> it = set.iterator();
        // natural order: "alpha", "bravo", null (sentinel is ordered after all non‑null)
        assertEquals("alpha", it.next());
        assertEquals("bravo", it.next());
        assertNull(it.next());
        assertFalse(it.hasNext());
    }

    /**
     * Iterator on an empty set – hasNext() is false immediately.
     */
    @Test
    void iteratorOnEmptySet() {
        Iterator<String> it = set.iterator();
        assertFalse(it.hasNext());
    }

    /**
     * Iterator remove operation – element removed from underlying set.
     */
    @Test
    void iteratorRemoveOperation() {
        addElements("alpha", "bravo", "charlie");
        Iterator<String> it = set.iterator();
        assertEquals("alpha", it.next());
        // remove "alpha"
        it.remove();
        // underlying set should now contain only "bravo" and "charlie"
        assertEquals(2, set.size());
        assertFalse(set.contains("alpha"));
        assertTrue(set.contains("bravo"));
        assertTrue(set.contains("charlie"));
        // continue iteration
        assertEquals("bravo", it.next());
        assertEquals("charlie", it.next());
        assertFalse(it.hasNext());
    }

    /**
     * Iterator with a custom comparator (reverse order).
     */
    @Test
    void iteratorWithCustomComparatorReverseOrder() throws Exception {
        // inject a reverse‑order comparator into the internal set
        setComparator(Collections.reverseOrder());
        addElements("alpha", "bravo", "charlie");
        Iterator<String> it = set.iterator();
        // Expected order: descending because of reverse comparator
        assertEquals("charlie", it.next());
        assertEquals("bravo", it.next());
        assertEquals("alpha", it.next());
        assertFalse(it.hasNext());
    }

    /**
     * Iterator weakly consistent after a concurrent add.
     */
    @Test
    void iteratorWeaklyConsistentAfterConcurrentAdd() {
        addElements("alpha", "bravo");
        Iterator<String> it = set.iterator();
        // modify the set after iterator creation
        set.add("charlie");
        // The iterator must not see the newly added element
        List<String> observed = new ArrayList<>();
        while (it.hasNext()) {
            observed.add(it.next());
        }
        assertEquals(Arrays.asList("alpha", "bravo"), observed);
        // The set itself now contains the third element
        assertTrue(set.contains("charlie"));
    }

    /**
     * Iterator next() on empty set throws NoSuchElementException.
     */
    @Test
    void iteratorNextOnEmptySetThrows() {
        Iterator<String> it = set.iterator();
        assertThrows(NoSuchElementException.class, it::next);
    }

    /**
     * Iterator remove() before next() throws IllegalStateException.
     */
    @Test
    void iteratorRemoveBeforeNextThrows() {
        addElements("alpha", "bravo");
        Iterator<String> it = set.iterator();
        assertThrows(IllegalStateException.class, it::remove);
    }

    /**
     * Iterator remove() after already removed throws IllegalStateException.
     */
    @Test
    void iteratorRemoveAfterAlreadyRemovedThrows() {
        addElements("alpha", "bravo");
        Iterator<String> it = set.iterator();
        assertEquals("alpha", it.next());
        // first removal – OK
        it.remove();
        assertThrows(IllegalStateException.class, it::remove);
    }

    /**
     * Iterator with non‑comparable elements throws ClassCastException.
     */
    @Test
    void iteratorWithNonComparableElementsThrows() {
        // Directly add a non‑comparable mix; the exception is thrown on add,
        // but we verify that attempting to obtain an iterator also fails.
        assertThrows(ClassCastException.class, () -> {
            // The first add succeeds, the second triggers the exception
            // inside the underlying ConcurrentSkipListSet.
            // Suppress unchecked warnings because we deliberately add a mixed type.
            @SuppressWarnings("unchecked")
            NavigableSet rawSet = (NavigableSet) set;
            rawSet.add("alpha");
            // Integer is not comparable with String
            rawSet.add(123);
        });
    }

    /**
     * Iterator when internalSet is null throws NullPointerException.
     */
    @Test
    void iteratorWhenInternalSetIsNullThrows() throws Exception {
        nullifyInternalSet();
        assertThrows(NullPointerException.class, () -> set.iterator().hasNext());
    }
}
