package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

import static org.junit.Assert.*;

/**
 * JUnit 4 test suite for {@link ConcurrentNavigableSetNullSafe}.
 *
 * The tests are divided into three logical sections:
 * 1. Extraction of public method signatures (documented in the comments).
 * 2. Basic functionality tests that verify normal operation.
 * 3. Edge‑case / exception tests that target special situations such as null handling,
 *    duplicate elements, illegal arguments and constructor validation.
 *
 * All tests use only the public API of {@code ConcurrentNavigableSetNullSafe} and the
 * standard Java collections that appear in the source code. No mocking or subclassing is used.
 */
public class ConcurrentNavigableSetNullSafeTest {

    /* -----------------------------------------------------------------------
     * 1.  PUBLIC METHOD SIGNATURES (extracted from the class)
     * -----------------------------------------------------------------------
     *
     * Constructors
     *   ConcurrentNavigableSetNullSafe()
     *   ConcurrentNavigableSetNullSafe(Comparator<? super E> comparator)
     *   ConcurrentNavigableSetNullSafe(Collection<? extends E> c)
     *   ConcurrentNavigableSetNullSafe(Collection<? extends E> c,
     *                                 Comparator<? super E> comparator)
     *
     * Set / SortedSet / NavigableSet methods
     *   Comparator<? super E> comparator()
     *   boolean add(E e)
     *   boolean remove(Object o)
     *   boolean contains(Object o)
     *   int size()
     *   boolean isEmpty()
     *   void clear()
     *   Iterator<E> iterator()
     *
     *   E first()
     *   E last()
     *   E lower(E e)
     *   E floor(E e)
     *   E ceiling(E e)
     *   E higher(E e)
     *   E pollFirst()
     *   E pollLast()
     *
     *   NavigableSet<E> descendingSet()
     *   Iterator<E> descendingIterator()
     *
     *   NavigableSet<E> subSet(E fromElement, boolean fromInclusive,
     *                          E toElement,   boolean toInclusive)
     *   NavigableSet<E> headSet(E toElement, boolean inclusive)
     *   NavigableSet<E> tailSet(E fromElement, boolean inclusive)
     *
     *   SortedSet<E> subSet(E fromElement, E toElement)
     *   SortedSet<E> headSet(E toElement)
     *   SortedSet<E> tailSet(E fromElement)
     */

    private ConcurrentNavigableSetNullSafe<String> set;

    @Before
    public void setUp() {
        set = new ConcurrentNavigableSetNullSafe<>();
    }

    /* -----------------------------------------------------------------------
     * 2. BASIC FUNCTIONALITY TESTS
     * ----------------------------------------------------------------------- */

    @Test
    public void testAddAndContains_NonNullElements() {
        assertTrue(set.add("apple"));
        assertTrue(set.contains("apple"));
        assertFalse(set.add("apple")); // duplicate, should return false
        assertEquals(1, set.size());
    }

    @Test
    public void testAddAndContains_NullElement() {
        assertTrue(set.add(null));
        assertTrue(set.contains(null));
        assertFalse(set.add(null)); // duplicate null
        assertEquals(1, set.size());
    }

    @Test
    public void testSizeAndIsEmpty() {
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());

        set.add("a");
        set.add(null);
        assertFalse(set.isEmpty());
        assertEquals(2, set.size());
    }

    @Test
    public void testRemove() {
        set.add("x");
        set.add(null);
        assertTrue(set.remove("x"));
        assertFalse(set.contains("x"));
        assertTrue(set.remove(null));
        assertFalse(set.contains(null));
        assertEquals(0, set.size());
    }

    @Test
    public void testIterator_OrderWithNull() {
        set.add("b");
        set.add(null);
        set.add("a");

        List<String> iterated = new ArrayList<>();
        for (String s : set) {
            iterated.add(s);
        }

        // Natural ordering: null is considered greater, so it appears last
        assertEquals(Arrays.asList("a", "b", null), iterated);
    }

    @Test
    public void testFirstAndLast_NaturalOrdering() {
        set.add("c");
        set.add("a");
        set.add(null);
        set.add("b");

        assertEquals("a", set.first()); // smallest non‑null
        assertEquals(null, set.last()); // null is considered greatest
    }

    @Test
    public void testPollFirstAndPollLast() {
        set.add("c");
        set.add("a");
        set.add(null);
        set.add("b");

        assertEquals("a", set.pollFirst());
        assertEquals(null, set.pollLast());
        assertEquals(2, set.size());
        assertFalse(set.contains("a"));
        assertFalse(set.contains(null));
    }

    @Test
    public void testNavigableMethods_LowerFloorCeilingHigher() {
        set.add("a");
        set.add("c");
        set.add(null);
        set.add("b");

        // lower / floor / ceiling / higher with non‑null key
        assertEquals("a", set.lower("b"));
        assertEquals("b", set.floor("b"));
        assertEquals("b", set.ceiling("b"));
        assertEquals("c", set.higher("b"));

        // using null as the argument (null is considered greatest)
        assertEquals("c", set.lower(null));
        assertEquals(null, set.floor(null));
        assertEquals(null, set.ceiling(null));
        assertNull(set.higher(null));
    }

    @Test
    public void testDescendingSet_OrderReversed() {
        set.add("a");
        set.add("b");
        set.add(null);
        NavigableSet<String> descending = set.descendingSet();

        Iterator<String> it = descending.iterator();
        assertEquals(null, it.next());
        assertEquals("b", it.next());
        assertEquals("a", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void testSubSet_InclusiveExclusive() {
        set.add("a");
        set.add("b");
        set.add("c");
        set.add(null);

        NavigableSet<String> sub = set.subSet("b", true, null, false);
        // sub should contain "b" and "c" (null excluded)
        assertTrue(sub.contains("b"));
        assertTrue(sub.contains("c"));
        assertFalse(sub.contains(null));
        assertEquals(2, sub.size());
    }

    @Test
    public void testHeadSetAndTailSet_WithNull() {
        set.add("a");
        set.add("b");
        set.add(null);

        SortedSet<String> head = set.headSet("b"); // exclusive by default
        assertTrue(head.contains("a"));
        assertFalse(head.contains("b"));
        assertFalse(head.contains(null));

        SortedSet<String> tail = set.tailSet("b"); // inclusive by default
        assertTrue(tail.contains("b"));
        assertTrue(tail.contains(null));
        assertFalse(tail.contains("a"));
    }

    @Test
    public void testComparator_ReturnsOriginalComparator() {
        Comparator<String> reverse = (s1, s2) -> {
            if (s1 == null) return (s2 == null) ? 0 : 1;
            if (s2 == null) return -1;
            return s2.compareTo(s1);
        };
        ConcurrentNavigableSetNullSafe<String> revSet = new ConcurrentNavigableSetNullSafe<>(reverse);
        assertSame(reverse, revSet.comparator());

        revSet.add("a");
        revSet.add("b");
        revSet.add(null);

        // With reverse ordering, "b" is first, null is last
        assertEquals("b", revSet.first());
        assertEquals(null, revSet.last());
    }

    @Test
    public void testClear() {
        set.add("x");
        set.add(null);
        set.clear();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    /* -----------------------------------------------------------------------
     * 3. EDGE CASE / EXCEPTION TESTS
     * ----------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testConstructorCollection_NullArgument() {
        new ConcurrentNavigableSetNullSafe<String>((Collection<String>) null);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorCollectionAndComparator_NullCollection() {
        new ConcurrentNavigableSetNullSafe<String>((Collection<String>) null, null);
    }

    @Test
    public void testAddDuplicateElementsIncludingNull() {
        assertTrue(set.add("dup"));
        assertFalse(set.add("dup")); // duplicate non‑null
        assertTrue(set.add(null));
        assertFalse(set.add(null)); // duplicate null
        assertEquals(2, set.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubSet_InvalidRange_ShouldThrow() {
        set.add("a");
        set.add("b");
        // fromElement > toElement -> IllegalArgumentException from ConcurrentSkipListSet
        set.subSet("b", true, "a", true);
    }

    @Test
    public void testLower_Floor_Ceiling_Higher_WithNullKeyWhenOnlyNullPresent() {
        set.add(null);
        assertNull(set.lower(null));
        assertNull(set.floor(null));
        assertNull(set.ceiling(null));
        assertNull(set.higher(null));
        assertEquals(null, set.first());
        assertEquals(null, set.last());
    }

    @Test
    public void testHeadSet_InclusiveFalse_WithNullElement() {
        set.add(null);
        set.add("a");
        // headSet up to null (exclusive) should be empty because null is greatest
        SortedSet<String> head = set.headSet(null, false);
        assertTrue(head.isEmpty());
    }

    @Test
    public void testTailSet_InclusiveTrue_WithNullElement() {
        set.add("a");
        set.add(null);
        // tailSet from null inclusive should contain only null
        SortedSet<String> tail = set.tailSet(null, true);
        assertEquals(1, tail.size());
        assertTrue(tail.contains(null));
    }

    @Test
    public void testDescendingIterator_RemovesElements() {
        set.add("x");
        set.add(null);
        Iterator<String> descIt = set.descendingIterator();

        // remove first element (null, because descending order)
        assertTrue(descIt.hasNext());
        assertNull(descIt.next());
        descIt.remove();

        assertFalse(set.contains(null));
        assertEquals(1, set.size());

        // remove remaining element
        assertTrue(descIt.hasNext());
        assertEquals("x", descIt.next());
        descIt.remove();

        assertTrue(set.isEmpty());
    }

    @Test
    public void testSubSet_ContainsSentinelConversion() {
        set.add(null);
        set.add("a");
        set.add("b");
        // Subset from null (inclusive) to "b" (exclusive)
        NavigableSet<String> sub = set.subSet(null, true, "b", false);
        // Should contain null and "a"
        assertTrue(sub.contains(null));
        assertTrue(sub.contains("a"));
        assertFalse(sub.contains("b"));
        assertEquals(2, sub.size());
    }
}
