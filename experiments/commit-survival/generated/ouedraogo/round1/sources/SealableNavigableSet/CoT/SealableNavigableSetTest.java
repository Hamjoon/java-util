package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.junit.Assert.*;

public class SealableNavigableSetTest {

    private Supplier<Boolean> sealedSupplier;
    private AtomicBoolean sealedFlag;
    private SealableNavigableSet<Integer> set;

    @Before
    public void setUp() {
        sealedFlag = new AtomicBoolean(false);
        sealedSupplier = sealedFlag::get;
        set = new SealableNavigableSet<>(sealedSupplier);
    }

    /* -----------------------------------------------------------------
     *  List of public methods (for reference)
     * -----------------------------------------------------------------
     *  public boolean equals(Object o)
     *  public int hashCode()
     *  public String toString()
     *  public int size()
     *  public boolean isEmpty()
     *  public boolean contains(Object o)
     *  public boolean containsAll(Collection<?> col)
     *  public Comparator<? super E> comparator()
     *  public E first()
     *  public E last()
     *  public Object[] toArray()
     *  public <T1> T1[] toArray(T1[] a)
     *  public E lower(E e)
     *  public E floor(E e)
     *  public E ceiling(E e)
     *  public E higher(E e)
     *  public Iterator<E> iterator()
     *  public Iterator<E> descendingIterator()
     *  public NavigableSet<E> descendingSet()
     *  public SortedSet<E> subSet(E fromElement, E toElement)
     *  public NavigableSet<E> subSet(E fromElement, boolean fromInclusive,
     *                                 E toElement, boolean toInclusive)
     *  public SortedSet<E> headSet(E toElement)
     *  public NavigableSet<E> headSet(E toElement, boolean inclusive)
     *  public SortedSet<E> tailSet(E fromElement)
     *  public NavigableSet<E> tailSet(E fromElement, boolean inclusive)
     *  public boolean add(E e)
     *  public boolean addAll(Collection<? extends E> col)
     *  public void clear()
     *  public boolean remove(Object o)
     *  public boolean removeAll(Collection<?> col)
     *  public boolean retainAll(Collection<?> col)
     *  public E pollFirst()
     *  public E pollLast()
     * ----------------------------------------------------------------- */

    /* --------------------------- Basic Functionality --------------------------- */

    @Test
    public void testAddAndContainsWhenUnsealed() {
        assertTrue(set.add(10));
        assertTrue(set.contains(10));
        assertEquals(1, set.size());
    }

    @Test
    public void testAddAllWhenUnsealed() {
        List<Integer> list = Arrays.asList(1, 2, 3);
        assertTrue(set.addAll(list));
        assertTrue(set.containsAll(list));
        assertEquals(3, set.size());
    }

    @Test
    public void testRemoveWhenUnsealed() {
        set.add(5);
        assertTrue(set.remove(Integer.valueOf(5)));
        assertFalse(set.contains(5));
        assertTrue(set.isEmpty());
    }

    @Test
    public void testClearWhenUnsealed() {
        set.add(1);
        set.add(2);
        set.clear();
        assertTrue(set.isEmpty());
    }

    @Test
    public void testPollFirstAndLastWhenUnsealed() {
        set.add(20);
        set.add(10);
        set.add(30);
        assertEquals(Integer.valueOf(10), set.pollFirst());
        assertEquals(Integer.valueOf(30), set.pollLast());
        assertEquals(1, set.size());
        assertEquals(Integer.valueOf(20), set.first());
    }

    @Test
    public void testIteratorAndDescendingIteratorOrder() {
        set.addAll(Arrays.asList(1, 2, 3, 4));
        Iterator<Integer> it = set.iterator();
        List<Integer> forward = new ArrayList<>();
        while (it.hasNext()) forward.add(it.next());
        assertEquals(Arrays.asList(1, 2, 3, 4), forward);

        Iterator<Integer> dit = set.descendingIterator();
        List<Integer> backward = new ArrayList<>();
        while (dit.hasNext()) backward.add(dit.next());
        assertEquals(Arrays.asList(4, 3, 2, 1), backward);
    }

    @Test
    public void testFirstLastOnEmptyThrowsException() {
        try {
            set.first();
            fail("Expected NoSuchElementException for first() on empty set");
        } catch (NoSuchElementException ignored) { }

        try {
            set.last();
            fail("Expected NoSuchElementException for last() on empty set");
        } catch (NoSuchElementException ignored) { }
    }

    @Test
    public void testLowerFloorCeilingHigher() {
        set.addAll(Arrays.asList(10, 20, 30));
        assertEquals(Integer.valueOf(10), set.lower(20));
        assertEquals(Integer.valueOf(10), set.floor(10));
        assertEquals(Integer.valueOf(20), set.ceiling(20));
        assertEquals(Integer.valueOf(30), set.higher(20));
    }

    @Test
    public void testSubSetInclusiveExclusive() {
        set.addAll(Arrays.asList(1, 2, 3, 4, 5));
        NavigableSet<Integer> sub = set.subSet(2, true, 5, false); // {2,3,4}
        assertEquals(new HashSet<>(Arrays.asList(2,3,4)), new HashSet<>(sub));
        assertFalse(sub.contains(5));
        assertTrue(sub.contains(2));
    }

    @Test
    public void testHeadSetInclusive() {
        set.addAll(Arrays.asList(5, 10, 15, 20));
        NavigableSet<Integer> head = set.headSet(15, true); // {5,10,15}
        assertEquals(new HashSet<>(Arrays.asList(5,10,15)), new HashSet<>(head));
    }

    @Test
    public void testTailSetExclusive() {
        set.addAll(Arrays.asList(5, 10, 15, 20));
        NavigableSet<Integer> tail = set.tailSet(10, false); // {15,20}
        assertEquals(new HashSet<>(Arrays.asList(15,20)), new HashSet<>(tail));
    }

    @Test
    public void testDescendingSetView() {
        set.addAll(Arrays.asList(1, 2, 3));
        NavigableSet<Integer> desc = set.descendingSet();
        List<Integer> list = new ArrayList<>(desc);
        assertEquals(Arrays.asList(3, 2, 1), list);
    }

    @Test
    public void testEqualsAndHashCodeDelegate() {
        SealableNavigableSet<Integer> other = new SealableNavigableSet<>(sealedSupplier);
        set.addAll(Arrays.asList(1, 2));
        other.addAll(Arrays.asList(1, 2));
        assertEquals(set, other);
        assertEquals(set.hashCode(), other.hashCode());
        assertEquals(set.toString(), other.toString());
    }

    /* --------------------------- Seal Behaviour --------------------------- */

    @Test(expected = UnsupportedOperationException.class)
    public void testAddWhenSealedThrows() {
        sealedFlag.set(true);
        set.add(99);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddAllWhenSealedThrows() {
        sealedFlag.set(true);
        set.addAll(Arrays.asList(1, 2));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemoveWhenSealedThrows() {
        set.add(42);
        sealedFlag.set(true);
        set.remove(42);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testClearWhenSealedThrows() {
        set.add(1);
        sealedFlag.set(true);
        set.clear();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemoveAllWhenSealedThrows() {
        set.addAll(Arrays.asList(1,2,3));
        sealedFlag.set(true);
        set.removeAll(Arrays.asList(1,2));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRetainAllWhenSealedThrows() {
        set.addAll(Arrays.asList(1,2,3));
        sealedFlag.set(true);
        set.retainAll(Arrays.asList(1));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPollFirstWhenSealedThrows() {
        set.add(10);
        sealedFlag.set(true);
        set.pollFirst();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPollLastWhenSealedThrows() {
        set.add(10);
        sealedFlag.set(true);
        set.pollLast();
    }

    @Test
    public void testIteratorRemoveHonorsSeal() {
        set.addAll(Arrays.asList(1, 2, 3));
        Iterator<Integer> it = set.iterator();
        it.next(); // advance to first element
        sealedFlag.set(true);
        try {
            it.remove();
            fail("Expected UnsupportedOperationException from iterator.remove() when sealed");
        } catch (UnsupportedOperationException ignored) { }
    }

    @Test
    public void testViewMethodsRespectSealAfterToggle() {
        // Populate set
        set.addAll(Arrays.asList(1, 2, 3, 4, 5));

        // Get a view before sealing
        NavigableSet<Integer> head = set.headSet(4, false); // {1,2,3}
        sealedFlag.set(true); // seal now

        // Mutating the view should fail
        try {
            head.add(99);
            fail("Expected UnsupportedOperationException when adding to sealed view");
        } catch (UnsupportedOperationException ignored) { }

        // Mutating original set via unsealed view should also fail
        try {
            set.add(6);
            fail("Expected UnsupportedOperationException when adding to sealed original set");
        } catch (UnsupportedOperationException ignored) { }
    }
}
