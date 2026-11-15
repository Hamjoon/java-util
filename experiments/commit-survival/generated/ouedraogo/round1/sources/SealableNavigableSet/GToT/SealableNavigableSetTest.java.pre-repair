package com.cedarsoftware.util;

import org.junit.*;
import org.junit.rules.ExpectedException;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Comprehensive JUnit‑4 test suite for {@link SealableNavigableSet}.
 */
public class SealableNavigableSetTest {

    /** Helper that can be toggled to make the set sealed / unsealed */
    private static class SealableFlag {
        private final AtomicBoolean flag = new AtomicBoolean(false);
        Supplier<Boolean> supplier() { return flag::get; }
        void seal()   { flag.set(true); }
        void unseal() { flag.set(false); }
    }

    private SealableFlag sealable;
    private SealableNavigableSet<Integer> set;

    @Rule public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        sealable = new SealableFlag();
        set = new SealableNavigableSet<>(sealable.supplier());
    }

    /* --------------------------------------------------------------------- */
    /* --------------------------- Immutable API --------------------------- */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddAndSize() {
        Assert.assertTrue(set.add(1));
        Assert.assertEquals(1, set.size());
        Assert.assertTrue(set.contains(1));
    }

    @Test
    public void testEqualsAndHashCode() {
        set.add(10);
        set.add(20);
        SealableNavigableSet<Integer> other = new SealableNavigableSet<>(sealable.supplier());
        other.addAll(Arrays.asList(10, 20));
        Assert.assertEquals(set, other);
        Assert.assertEquals(set.hashCode(), other.hashCode());
        Assert.assertNotEquals(set, Collections.emptySet());
    }

    @Test
    public void testToString() {
        set.addAll(Arrays.asList(3, 1, 2));
        // Order is natural (sorted)
        Assert.assertEquals("[1, 2, 3]", set.toString());
    }

    @Test
    public void testIsEmptyAndClear() {
        Assert.assertTrue(set.isEmpty());
        set.add(5);
        Assert.assertFalse(set.isEmpty());
        set.clear();
        Assert.assertTrue(set.isEmpty());
    }

    @Test
    public void testContainsAll() {
        set.addAll(Arrays.asList(1, 2, 3));
        Assert.assertTrue(set.containsAll(Arrays.asList(1, 3)));
        Assert.assertFalse(set.containsAll(Arrays.asList(1, 4)));
    }

    @Test
    public void testComparator() {
        SealableNavigableSet<String> stringSet = new SealableNavigableSet<>(Comparator.reverseOrder(),
                sealable.supplier());
        stringSet.addAll(Arrays.asList("a", "b", "c"));
        Assert.assertEquals(Comparator.reverseOrder(), stringSet.comparator());
        // Elements are stored in reverse order
        Assert.assertEquals("[c, b, a]", stringSet.toString());
    }

    @Test
    public void testFirstAndLast() {
        set.addAll(Arrays.asList(5, 2, 9));
        Assert.assertEquals(Integer.valueOf(2), set.first());
        Assert.assertEquals(Integer.valueOf(9), set.last());
    }

    @Test(expected = NoSuchElementException.class)
    public void testFirstOnEmptyThrows() {
        new SealableNavigableSet<>(sealable.supplier()).first();
    }

    @Test(expected = NoSuchElementException.class)
    public void testLastOnEmptyThrows() {
        new SealableNavigableSet<>(sealable.supplier()).last();
    }

    @Test
    public void testToArray() {
        set.addAll(Arrays.asList(1, 2, 3));
        Object[] arr = set.toArray();
        Assert.assertArrayEquals(new Object[]{1, 2, 3}, arr);
    }

    @Test
    public void testToArrayTyped() {
        set.addAll(Arrays.asList(4, 5));
        Integer[] a = new Integer[5];
        Integer[] result = set.toArray(a);
        // result should be the same array instance when it is large enough
        Assert.assertSame(a, result);
        Assert.assertArrayEquals(new Integer[]{4, 5, null, null, null}, result);
    }

    @Test
    public void testNavigableQueries() {
        set.addAll(Arrays.asList(10, 20, 30));
        Assert.assertEquals(Integer.valueOf(20), set.lower(30));
        Assert.assertEquals(Integer.valueOf(20), set.floor(20));
        Assert.assertEquals(Integer.valueOf(20), set.ceiling(20));
        Assert.assertEquals(Integer.valueOf(30), set.higher(20));
        Assert.assertNull(set.lower(10));
        Assert.assertNull(set.higher(30));
    }

    @Test
    public void testIteratorAndRemoveWhenUnsealed() {
        set.addAll(Arrays.asList(1, 2, 3));
        Iterator<Integer> it = set.iterator();
        Assert.assertTrue(it.hasNext());
        Assert.assertEquals(Integer.valueOf(1), it.next());
        it.remove();               // should succeed because set is unsealed
        Assert.assertFalse(set.contains(1));
    }

    @Test
    public void testIteratorRemoveWhenSealed() {
        set.addAll(Arrays.asList(1, 2));
        sealable.seal();               // make the set immutable
        Iterator<Integer> it = set.iterator();
        it.next();                     // advance to first element
        thrown.expect(UnsupportedOperationException.class);
        it.remove();                   // must throw
    }

    @Test
    public void testDescendingIteratorOrder() {
        set.addAll(Arrays.asList(1, 2, 3));
        Iterator<Integer> it = set.descendingIterator();
        List<Integer> list = new ArrayList<>();
        while (it.hasNext()) {
            list.add(it.next());
        }
        Assert.assertEquals(Arrays.asList(3, 2, 1), list);
    }

    @Test
    public void testDescendingSetView() {
        set.addAll(Arrays.asList(1, 2, 3));
        NavigableSet<Integer> desc = set.descendingSet();
        Assert.assertEquals("[3, 2, 1]", desc.toString());

        // Mutating via view while unsealed works
        desc.add(0);
        Assert.assertTrue(set.contains(0));
        Assert.assertEquals("[0, 1, 2, 3]", set.toString());

        // Mutating via view while sealed must fail
        sealable.seal();
        thrown.expect(UnsupportedOperationException.class);
        desc.remove(2);
    }

    @Test
    public void testSubSetViewAndRangeValidation() {
        set.addAll(Arrays.asList(1, 2, 3, 4, 5));
        NavigableSet<Integer> sub = set.subSet(2, true, 4, false);
        Assert.assertEquals("[2, 3]", sub.toString());

        // Invalid range – should throw IllegalArgumentException
        thrown.expect(IllegalArgumentException.class);
        set.subSet(5, true, 2, true);
    }

    @Test
    public void testHeadSetAndTailSetViews() {
        set.addAll(Arrays.asList(10, 20, 30, 40));
        NavigableSet<Integer> head = set.headSet(30, false);
        Assert.assertEquals("[10, 20]", head.toString());

        NavigableSet<Integer> tail = set.tailSet(20, true);
        Assert.assertEquals("[20, 30, 40]", tail.toString());

        // Mutate through headSet while sealed → exception
        sealable.seal();
        thrown.expect(UnsupportedOperationException.class);
        head.add(15);
    }

    /* --------------------------------------------------------------------- */
    /* ---------------------------- Mutable API ---------------------------- */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddAllAndRemoveAll() {
        Assert.assertTrue(set.addAll(Arrays.asList(1, 2, 3)));
        Assert.assertTrue(set.removeAll(Arrays.asList(2, 3)));
        Assert.assertFalse(set.contains(2));
        Assert.assertFalse(set.contains(3));
        Assert.assertTrue(set.contains(1));
    }

    @Test
    public void testRetainAll() {
        set.addAll(Arrays.asList(1, 2, 3, 4));
        Assert.assertTrue(set.retainAll(Arrays.asList(2, 4, 6)));
        Assert.assertEquals("[2, 4]", set.toString());
    }

    @Test
    public void testPollFirstAndPollLast() {
        set.addAll(Arrays.asList(5, 10, 15));
        Assert.assertEquals(Integer.valueOf(5), set.pollFirst());
        Assert.assertEquals(Integer.valueOf(15), set.pollLast());
        Assert.assertEquals("[10]", set.toString());
    }

    @Test
    public void testPollOnEmptyReturnsNull() {
        Assert.assertNull(set.pollFirst());
        Assert.assertNull(set.pollLast());
    }

    @Test
    public void testMutableMethodsWhenSealedThrow() {
        set.add(1);
        sealable.seal();

        // each mutable operation must throw UnsupportedOperationException
        thrown.expect(UnsupportedOperationException.class);
        set.add(2);
    }

    @Test
    public void testRemoveNonExistentElementReturnsFalse() {
        set.add(100);
        Assert.assertFalse(set.remove(200));
        Assert.assertTrue(set.contains(100));
    }

    @Test(expected = NullPointerException.class)
    public void testAddNullThrows() {
        // ConcurrentSkipListSet does not allow null elements
        set.add(null);
    }

    /* --------------------------------------------------------------------- */
    /* -------------------------- Constructor Tests ----------------------- */
    /* --------------------------------------------------------------------- */

    @Test
    public void testConstructorWithComparator() {
        Comparator<String> rev = Comparator.reverseOrder();
        SealableNavigableSet<String> s = new SealableNavigableSet<>(rev, sealable.supplier());
        s.addAll(Arrays.asList("a", "b", "c"));
        Assert.assertEquals("[c, b, a]", s.toString());
        Assert.assertEquals(rev, s.comparator());
    }

    @Test
    public void testConstructorWithCollection() {
        List<Integer> source = Arrays.asList(3, 1, 2);
        SealableNavigableSet<Integer> s = new SealableNavigableSet<>(source, sealable.supplier());
        Assert.assertEquals("[1, 2, 3]", s.toString());
    }

    @Test
    public void testConstructorWithSortedSet() {
        SortedSet<Integer> src = new TreeSet<>(Arrays.asList(5, 4, 6));
        SealableNavigableSet<Integer> s = new SealableNavigableSet<>(src, sealable.supplier());
        Assert.assertEquals("[4, 5, 6]", s.toString());
    }

    @Test
    public void testConstructorWrappingExistingNavigableSet() {
        NavigableSet<Integer> backing = new TreeSet<>();
        backing.addAll(Arrays.asList(9, 8, 7));
        SealableNavigableSet<Integer> s = new SealableNavigableSet<>(backing, sealable.supplier());
        Assert.assertEquals("[7, 8, 9]", s.toString());

        // Mutate through wrapper and verify backing set changes
        s.add(10);
        Assert.assertTrue(backing.contains(10));
    }
}
