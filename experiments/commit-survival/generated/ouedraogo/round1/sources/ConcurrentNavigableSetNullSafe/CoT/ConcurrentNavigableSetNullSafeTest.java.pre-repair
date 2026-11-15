package com.cedarsoftware.util;

import org.junit.*;
import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * JUnit‑4 test suite for {@link ConcurrentNavigableSetNullSafe}.
 *
 * The test class is organised as follows:
 *
 * 1. **Public API extraction** – a comment block enumerating every public method
 *    (including inherited ones that are overridden) together with its signature.
 *
 * 2. **Basic functionality tests** – one simple test per method that checks the
 *    normal, expected behaviour.
 *
 * 3. **Edge‑case / exception tests** – tests that exercise null handling,
 *    empty collections, boundary conditions and the special sentinel logic.
 *
 * 4. **Combined test suite** – all tests are compiled into a single JUnit‑4 test
 *    class that can be run directly.
 *
 * The tests use only the constructors, methods and classes that appear in the
 * source code supplied (no additional mocking or subclassing).  The sentinel
 * value is internal and therefore verified indirectly by checking the observable
 * behaviour of the set when null elements are added, removed or queried.
 */
public class ConcurrentNavigableSetNullSafeTest {

    /* --------------------------------------------------------------
     * 1. PUBLIC METHODS (extracted from the source)
     * --------------------------------------------------------------
     *
     * Constructors
     *   ConcurrentNavigableSetNullSafe()
     *   ConcurrentNavigableSetNullSafe(Comparator<? super E> comparator)
     *   ConcurrentNavigableSetNullSafe(Collection<? extends E> c)
     *   ConcurrentNavigableSetNullSafe(Collection<? extends E> c, Comparator<? super E> comparator)
     *
     * NavigableSet / SortedSet API (overridden)
     *   Comparator<? super E> comparator()
     *   E lower(E e)
     *   E floor(E e)
     *   E ceiling(E e)
     *   E higher(E e)
     *   E pollFirst()
     *   E pollLast()
     *   Iterator<E> iterator()
     *   NavigableSet<E> descendingSet()
     *   Iterator<E> descendingIterator()
     *   NavigableSet<E> subSet(E fromElement, boolean fromInclusive,
     *                         E toElement, boolean toInclusive)
     *   NavigableSet<E> headSet(E toElement, boolean inclusive)
     *   NavigableSet<E> tailSet(E fromElement, boolean inclusive)
     *   SortedSet<E> subSet(E fromElement, E toElement)
     *   SortedSet<E> headSet(E toElement)
     *   SortedSet<E> tailSet(E fromElement)
     *   E first()
     *   E last()
     *
     * Set API
     *   int size()
     *   boolean isEmpty()
     *   boolean contains(Object o)
     *   boolean add(E e)
     *   boolean remove(Object o)
     *   void clear()
     */

    /* --------------------------------------------------------------
     * 2. BASIC FUNCTIONALITY TESTS
     * -------------------------------------------------------------- */

    @Test
    public void testDefaultConstructorAndAddContains() {
        ConcurrentNavigableSetNullSafe<Integer> set = new ConcurrentNavigableSetNullSafe<>();
        assertTrue(set.add(5));
        assertTrue(set.contains(5));
        assertEquals(1, set.size());
    }

    @Test
    public void testConstructorWithComparator() {
        Comparator<String> reverse = Comparator.reverseOrder();
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>(reverse);
        set.add("a");
        set.add("b");
        // because of reverse order, first() should be the greatest element ("b")
        assertEquals("b", set.first());
        assertEquals("a", set.last());
    }

    @Test
    public void testCollectionConstructor() {
        List<String> list = Arrays.asList("x", "y", "z");
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>(list);
        assertTrue(set.containsAll(list));
        assertEquals(3, set.size());
    }

    @Test
    public void testCollectionConstructorWithComparator() {
        Comparator<Integer> natural = Integer::compareTo;
        List<Integer> list = Arrays.asList(3, 1, 2);
        ConcurrentNavigableSetNullSafe<Integer> set =
                new ConcurrentNavigableSetNullSafe<>(list, natural);
        assertEquals(Integer.valueOf(1), set.first());
        assertEquals(Integer.valueOf(3), set.last());
    }

    @Test
    public void testLowerFloorCeilingHigher() {
        ConcurrentNavigableSetNullSafe<Integer> set = new ConcurrentNavigableSetNullSafe<>();
        set.addAll(Arrays.asList(10, 20, 30));

        assertEquals(Integer.valueOf(20), set.lower(30));
        assertEquals(Integer.valueOf(20), set.floor(20));
        assertEquals(Integer.valueOf(20), set.ceiling(20));
        assertEquals(Integer.valueOf(30), set.higher(20));
    }

    @Test
    public void testPollFirstAndPollLast() {
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.addAll(Arrays.asList("a", "b", "c"));
        assertEquals("a", set.pollFirst());
        assertEquals("c", set.pollLast());
        assertEquals(1, set.size());
        assertTrue(set.contains("b"));
    }

    @Test
    public void testIteratorOrder() {
        ConcurrentNavigableSetNullSafe<Integer> set = new ConcurrentNavigableSetNullSafe<>();
        set.addAll(Arrays.asList(3, 1, 2));
        Iterator<Integer> it = set.iterator();
        assertTrue(it.hasNext());
        assertEquals(Integer.valueOf(1), it.next());
        assertEquals(Integer.valueOf(2), it.next());
        assertEquals(Integer.valueOf(3), it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void testDescendingSetAndIterator() {
        ConcurrentNavigableSetNullSafe<Integer> set = new ConcurrentNavigableSetNullSafe<>();
        set.addAll(Arrays.asList(1, 2, 3));
        NavigableSet<Integer> desc = set.descendingSet();
        Iterator<Integer> it = desc.iterator();
        assertEquals(Integer.valueOf(3), it.next());
        assertEquals(Integer.valueOf(2), it.next());
        assertEquals(Integer.valueOf(1), it.next());
    }

    @Test
    public void testSubSetHeadSetTailSet() {
        ConcurrentNavigableSetNullSafe<Integer> set = new ConcurrentNavigableSetNullSafe<>();
        set.addAll(Arrays.asList(1, 2, 3, 4, 5));

        NavigableSet<Integer> sub = set.subSet(2, true, 4, false); // 2,3
        assertEquals(2, sub.size());
        assertTrue(sub.containsAll(Arrays.asList(2, 3)));

        SortedSet<Integer> head = set.headSet(3); // 1,2
        assertEquals(2, head.size());
        assertTrue(head.containsAll(Arrays.asList(1, 2)));

        SortedSet<Integer> tail = set.tailSet(4); // 4,5
        assertEquals(2, tail.size());
        assertTrue(tail.containsAll(Arrays.asList(4, 5)));
    }

    @Test
    public void testFirstLast() {
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.addAll(Arrays.asList("alpha", "beta", "gamma"));
        assertEquals("alpha", set.first());
        assertEquals("gamma", set.last());
    }

    @Test
    public void testSizeIsEmptyClear() {
        ConcurrentNavigableSetNullSafe<Integer> set = new ConcurrentNavigableSetNullSafe<>();
        assertTrue(set.isEmpty());
        set.add(42);
        assertFalse(set.isEmpty());
        assertEquals(1, set.size());
        set.clear();
        assertTrue(set.isEmpty());
    }

    @Test
    public void testContainsAndRemove() {
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.add("test");
        assertTrue(set.contains("test"));
        assertTrue(set.remove("test"));
        assertFalse(set.contains("test"));
    }

    /* --------------------------------------------------------------
     * 3. EDGE CASES & EXCEPTION HANDLING
     * -------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testCollectionConstructorNullArgument() {
        new ConcurrentNavigableSetNullSafe<>(null);
    }

    @Test(expected = NullPointerException.class)
    public void testCollectionConstructorWithComparatorNullCollection() {
        new ConcurrentNavigableSetNullSafe<>(null, Comparator.naturalOrder());
    }

    @Test
    public void testNullElementMaskingInAddContainsAndRemove() {
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>();
        assertTrue(set.add(null));               // null is stored as sentinel
        assertTrue(set.contains(null));          // contains should recognise null
        assertEquals(1, set.size());

        // other non‑null elements coexist with null
        set.add("a");
        set.add("b");
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertEquals(3, set.size());

        // removal of null works
        assertTrue(set.remove(null));
        assertFalse(set.contains(null));
        assertEquals(2, set.size());
    }

    @Test
    public void testNavigationalMethodsWithNulls() {
        ConcurrentNavigableSetNullSafe<Integer> set = new ConcurrentNavigableSetNullSafe<>();
        set.addAll(Arrays.asList(1, 2, null, 4));

        // According to wrapComparator, null is considered greater than any non‑null
        assertEquals(Integer.valueOf(4), set.lower(null));   // greatest element < null
        assertEquals(null, set.floor(null));                // null itself
        assertEquals(null, set.ceiling(null));              // null itself
        assertNull(set.higher(null));                       // no element greater than null
    }

    @Test
    public void testPollFirstAndPollLastWithNull() {
        ConcurrentNavigableSetNullSafe<Integer> set = new ConcurrentNavigableSetNullSafe<>();
        set.addAll(Arrays.asList(null, 10, 20));

        // natural ordering treats null as greatest, so pollFirst returns the smallest non‑null
        assertEquals(Integer.valueOf(10), set.pollFirst());
        // pollLast should return null (the sentinel)
        assertNull(set.pollLast());
        assertEquals(1, set.size()); // only 20 remains
    }

    @Test
    public void testSubSetBoundariesWithNulls() {
        ConcurrentNavigableSetNullSafe<Integer> set = new ConcurrentNavigableSetNullSafe<>();
        set.addAll(Arrays.asList(null, 1, 2, 3, 4));

        // Subset from null (inclusive) to 3 (exclusive) should contain null,1,2
        NavigableSet<Integer> sub = set.subSet(null, true, 3, false);
        assertEquals(3, sub.size());
        assertTrue(sub.contains(null));
        assertTrue(sub.contains(1));
        assertTrue(sub.contains(2));
        assertFalse(sub.contains(3));
    }

    @Test
    public void testDescendingIteratorRemove() {
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.addAll(Arrays.asList("x", "y", "z"));
        Iterator<String> it = set.descendingIterator();
        assertEquals("z", it.next());
        it.remove(); // remove "z"
        assertFalse(set.contains("z"));
        assertEquals(2, set.size());
    }

    @Test
    public void testComparatorReturnedMatchesProvided() {
        Comparator<String> comp = String.CASE_INSENSITIVE_ORDER;
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>(comp);
        assertSame(comp, set.comparator());
    }

    @Test
    public void testAddAllReturnsTrueOnlyWhenSetChanged() {
        ConcurrentNavigableSetNullSafe<Integer> set = new ConcurrentNavigableSetNullSafe<>();
        assertTrue(set.addAll(Arrays.asList(1, 2, 3)));
        // Adding the same elements again should not change the set
        assertFalse(set.addAll(Arrays.asList(1, 2, 3)));
        assertEquals(3, set.size());
    }

    /* --------------------------------------------------------------
     * 4. COMBINED TEST SUITE (JUnit 4)
     * -------------------------------------------------------------- */

    // No additional code is required – the @Test methods above constitute the
    // complete suite.  Running this class with a JUnit‑4 runner will execute
    // all tests and report any failures.
}
