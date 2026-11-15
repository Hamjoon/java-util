package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.function.Executable;
import java.lang.reflect.Field;
import java.util.*;

class ConcurrentNavigableSetNullSafe_descendingIterator_Test {

    private ConcurrentNavigableSetNullSafe<String> set;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default (natural ordering) constructor for most tests
        set = new ConcurrentNavigableSetNullSafe<>();
        set.add("apple");
        set.add("banana");
        set.add("cherry");
        set.add(null);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the set is cleared between tests
        set.clear();
    }

    @Test
    void testDescendingIteratorOrder_NaturalOrdering() {
        Iterator<String> it = set.descendingIterator();
        // Expected descending order: null (sentinel), cherry, banana, apple
        assertEquals(null, it.next(), "First element should be the masked null");
        assertEquals("cherry", it.next(), "Second element should be 'cherry'");
        assertEquals("banana", it.next(), "Third element should be 'banana'");
        assertEquals("apple", it.next(), "Fourth element should be 'apple'");
        assertFalse(it.hasNext(), "Iterator should be exhausted");
    }

    @Test
    void testDescendingIteratorRemove() {
        Iterator<String> it = set.descendingIterator();
        // Remove the first element (null) via the descending iterator
        assertEquals(null, it.next(), "First element should be the masked null");
        it.remove();
        // Verify that null is no longer present in the set
        assertFalse(set.contains(null), "Set should no longer contain null after removal");
        assertEquals(3, set.size(), "Set size should be reduced after removal");
        // Continue iteration to ensure the iterator remains functional
        assertEquals("cherry", it.next(), "Next element should be 'cherry'");
        assertEquals("banana", it.next(), "Next element should be 'banana'");
        assertEquals("apple", it.next(), "Next element should be 'apple'");
        assertFalse(it.hasNext(), "Iterator should be exhausted after removing all elements");
    }

    @Test
    void testDescendingIteratorOrder_WithCustomComparator() {
        // Comparator that orders strings by length (null is treated as length 0)
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        // Use the comparator‑based constructor
        NavigableSet<String> customSet = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        // length 4
        customSet.add("kiwi");
        // length 5
        customSet.add("apple");
        // length 6
        customSet.add("banana");
        // length 0
        customSet.add(null);
        Iterator<String> it = customSet.descendingIterator();
        // Ascending order would be: null, kiwi, apple, banana
        // Descending order should therefore be: banana, apple, kiwi, null
        assertEquals("banana", it.next(), "First element should be 'banana'");
        assertEquals("apple", it.next(), "Second element should be 'apple'");
        assertEquals("kiwi", it.next(), "Third element should be 'kiwi'");
        assertEquals(null, it.next(), "Fourth element should be the masked null");
        assertFalse(it.hasNext(), "Iterator should be exhausted");
    }

    /**
     * EmptySet **
     */
    @Test
    @DisplayName("Empty set: iterator hasNext false and next throws NoSuchElementException")
    void testDescendingIterator_EmptySet() {
        ConcurrentNavigableSetNullSafe<Integer> empty = new ConcurrentNavigableSetNullSafe<>();
        Iterator<Integer> it = empty.descendingIterator();
        assertFalse(it.hasNext(), "hasNext() should be false for an empty set");
        assertThrows(NoSuchElementException.class, it::next, "next() must throw NoSuchElementException when no elements are present");
    }

    /**
     * NonEmptySet – ordering **
     */
    @Test
    @DisplayName("Non‑empty set: descending order is reverse of natural order")
    void testDescendingIterator_NonEmptySet_Order() {
        ConcurrentNavigableSetNullSafe<String> ns = new ConcurrentNavigableSetNullSafe<>();
        ns.add("a");
        ns.add("b");
        ns.add("c");
        Iterator<String> it = ns.descendingIterator();
        assertTrue(it.hasNext());
        assertEquals("c", it.next());
        assertEquals("b", it.next());
        assertEquals("a", it.next());
        assertFalse(it.hasNext());
    }

    /**
     * NonEmptySet – removal during iteration **
     */
    @Test
    @DisplayName("Non‑empty set: remove first element via iterator")
    void testDescendingIterator_NonEmptySet_Remove() {
        ConcurrentNavigableSetNullSafe<String> ns = new ConcurrentNavigableSetNullSafe<>();
        ns.add("a");
        ns.add("b");
        ns.add("c");
        Iterator<String> it = ns.descendingIterator();
        assertEquals("c", it.next());
        // remove 'c'
        it.remove();
        assertFalse(ns.contains("c"));
        assertEquals(2, ns.size());
        // Remaining elements should be b then a
        assertEquals("b", it.next());
        assertEquals("a", it.next());
        assertFalse(it.hasNext());
    }

    /**
     * NullElement – iterator returns null **
     */
    @Test
    @DisplayName("Set containing only null returns null from iterator")
    void testDescendingIterator_OnlyNullElement() {
        ConcurrentNavigableSetNullSafe<String> onlyNull = new ConcurrentNavigableSetNullSafe<>();
        onlyNull.add(null);
        Iterator<String> it = onlyNull.descendingIterator();
        assertTrue(it.hasNext(), "hasNext() should be true when only null is present");
        assertNull(it.next(), "next() should return the genuine null");
        assertFalse(it.hasNext(), "hasNext() should be false after consuming the only element");
    }

    /**
     * NullElement – iterator hasNext before and after null **
     */
    @Test
    @DisplayName("Set with null and other elements: iterator handles null correctly")
    void testDescendingIterator_NullElement_Mixed() {
        ConcurrentNavigableSetNullSafe<String> mixed = new ConcurrentNavigableSetNullSafe<>();
        mixed.add("x");
        mixed.add(null);
        mixed.add("y");
        Iterator<String> it = mixed.descendingIterator();
        // Expected descending order: y, null, x
        assertEquals("y", it.next());
        assertTrue(it.hasNext(), "hasNext() should be true before null");
        assertNull(it.next(), "next() should return null for the masked element");
        assertTrue(it.hasNext(), "hasNext() should be true after null");
        assertEquals("x", it.next());
        assertFalse(it.hasNext());
    }

    /**
     * NullAndOtherElements – remove null **
     */
    @Test
    @DisplayName("Removing the null element via descending iterator")
    void testDescendingIterator_RemoveNullElement() {
        ConcurrentNavigableSetNullSafe<String> mixed = new ConcurrentNavigableSetNullSafe<>();
        mixed.add("x");
        mixed.add(null);
        mixed.add("y");
        Iterator<String> it = mixed.descendingIterator();
        // y
        assertEquals("y", it.next());
        // null
        assertEquals(null, it.next());
        // remove null
        it.remove();
        assertFalse(mixed.contains(null), "Set should no longer contain null after removal");
        assertEquals(2, mixed.size());
        // Continue iteration – should now yield only "x"
        assertEquals("x", it.next());
        assertFalse(it.hasNext());
    }

    /**
     * SingleElement **
     */
    @Test
    @DisplayName("Single element set yields exactly one element")
    void testDescendingIterator_SingleElement() {
        ConcurrentNavigableSetNullSafe<String> single = new ConcurrentNavigableSetNullSafe<>();
        single.add("z");
        Iterator<String> it = single.descendingIterator();
        assertTrue(it.hasNext());
        assertEquals("z", it.next());
        assertFalse(it.hasNext());
    }

    /**
     * RemoveOperation – normal removal **
     */
    @Test
    @DisplayName("Remove after next works and reflects in underlying set")
    void testDescendingIterator_RemoveOperation_Normal() {
        ConcurrentNavigableSetNullSafe<Integer> numbers = new ConcurrentNavigableSetNullSafe<>();
        numbers.add(1);
        numbers.add(2);
        numbers.add(3);
        // order: 3,2,1
        Iterator<Integer> it = numbers.descendingIterator();
        assertEquals(3, it.next());
        // remove 3
        it.remove();
        assertFalse(numbers.contains(3));
        assertEquals(2, numbers.size());
        // Remaining iteration should give 2 then 1
        assertEquals(2, it.next());
        assertEquals(1, it.next());
        assertFalse(it.hasNext());
    }

    @Test
    @DisplayName("Calling remove() before next() throws IllegalStateException")
    void testDescendingIterator_RemoveOperation_IllegalStateBeforeNext() {
        ConcurrentNavigableSetNullSafe<Integer> numbers = new ConcurrentNavigableSetNullSafe<>();
        numbers.add(1);
        numbers.add(2);
        numbers.add(3);
        Iterator<Integer> it = numbers.descendingIterator();
        assertThrows(IllegalStateException.class, it::remove, "remove() without a preceding next() must throw IllegalStateException");
    }

    /**
     * RemoveEdgeCases – double remove **
     */
    @Test
    @DisplayName("Calling remove() twice consecutively throws IllegalStateException")
    void testDescendingIterator_RemoveEdgeCases_DoubleRemove() {
        ConcurrentNavigableSetNullSafe<String> ns = new ConcurrentNavigableSetNullSafe<>();
        ns.add("a");
        ns.add("b");
        ns.add("c");
        Iterator<String> it = ns.descendingIterator();
        assertEquals("c", it.next());
        // first remove – ok
        it.remove();
        // Second remove without another next()
        assertThrows(IllegalStateException.class, it::remove, "Second consecutive remove() must throw IllegalStateException");
    }

    @Test
    @DisplayName("remove() after clearing the set throws IllegalStateException")
    void testDescendingIterator_RemoveEdgeCases_AfterClear() {
        ConcurrentNavigableSetNullSafe<String> ns = new ConcurrentNavigableSetNullSafe<>();
        ns.add("a");
        ns.add("b");
        Iterator<String> it = ns.descendingIterator();
        // clear while iterator exists
        ns.clear();
        // According to the underlying iterator contract, remove() now illegal
        assertThrows(IllegalStateException.class, it::remove, "remove() after the set has been cleared should throw IllegalStateException");
    }

    /**
     * CustomComparator – reverse natural order **
     */
    @Test
    @DisplayName("Comparator that reverses natural order: descending iterator yields natural order")
    void testDescendingIterator_CustomComparator_ReverseNatural() {
        // descending natural order
        Comparator<Integer> reverse = Comparator.reverseOrder();
        NavigableSet<Integer> revSet = new ConcurrentNavigableSetNullSafe<>(reverse);
        revSet.add(1);
        revSet.add(2);
        // internal order: 3,2,1
        revSet.add(3);
        // should give 1,2,3 (natural order)
        Iterator<Integer> it = revSet.descendingIterator();
        assertEquals(1, it.next());
        assertEquals(2, it.next());
        assertEquals(3, it.next());
        assertFalse(it.hasNext());
    }

    /**
     * ConcurrentModification – weak consistency **
     */
    @Test
    @DisplayName("Iterator snapshot is not affected by later additions")
    void testDescendingIterator_ConcurrentModification_Addition() {
        ConcurrentNavigableSetNullSafe<Integer> numbers = new ConcurrentNavigableSetNullSafe<>();
        numbers.add(10);
        numbers.add(20);
        // order: 20,10
        Iterator<Integer> it = numbers.descendingIterator();
        assertEquals(20, it.next());
        // Add a new element after iterator creation
        numbers.add(30);
        // Iterator must continue with snapshot (only 10 remains)
        assertEquals(10, it.next());
        assertFalse(it.hasNext(), "Iterator should not see the element added after its creation");
    }

    @Test
    @DisplayName("Iterator reflects removal performed directly on the set")
    void testDescendingIterator_ConcurrentModification_Removal() {
        ConcurrentNavigableSetNullSafe<Integer> numbers = new ConcurrentNavigableSetNullSafe<>();
        numbers.add(5);
        numbers.add(15);
        numbers.add(25);
        // order: 25,15,5
        Iterator<Integer> it = numbers.descendingIterator();
        assertEquals(25, it.next());
        // Remove 15 directly from the set
        numbers.remove(15);
        // The iterator should skip the removed element and return 5 next
        assertEquals(5, it.next());
        assertFalse(it.hasNext());
    }

    /**
     * NullInternalSet – constructor with null internalSet **
     */
    @Test
    @DisplayName("Constructing with a null internalSet leads to NullPointerException on descendingIterator")
    void testDescendingIterator_NullInternalSet() throws Exception {
        // Create a normal instance then force its internalSet field to null via reflection
        ConcurrentNavigableSetNullSafe<String> broken = new ConcurrentNavigableSetNullSafe<>();
        Field internalSetField = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        internalSetField.setAccessible(true);
        internalSetField.set(broken, null);
        // Expect NPE when descendingIterator() tries to delegate to internalSet
        assertThrows(NullPointerException.class, broken::descendingIterator, "descendingIterator() should propagate NullPointerException when internalSet is null");
    }

    /**
     * Additional sanity test – iterator remove after next on null element **
     */
    @Test
    @DisplayName("Removing the null element via iterator.remove() works correctly")
    void testDescendingIterator_RemoveNullViaIterator() {
        ConcurrentNavigableSetNullSafe<String> mixed = new ConcurrentNavigableSetNullSafe<>();
        mixed.add("alpha");
        mixed.add(null);
        mixed.add("omega");
        Iterator<String> it = mixed.descendingIterator();
        // omega
        assertEquals("omega", it.next());
        // null
        assertEquals(null, it.next());
        // remove null
        it.remove();
        assertFalse(mixed.contains(null));
        assertEquals(2, mixed.size());
        // Remaining iteration should give "alpha"
        assertEquals("alpha", it.next());
        assertFalse(it.hasNext());
    }
}
