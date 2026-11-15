package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Assert;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * JUnit 4 test cases for {@link CollectionUtilities}.
 */
public class CollectionUtilitiesTest {

    /* ----------------------------------------------------------------------
     *  Public method signatures extracted from CollectionUtilities
     * ---------------------------------------------------------------------- */
    // boolean isEmpty(Collection<?> col)
    // boolean hasContent(Collection<?> col)
    // int size(Collection<?> col)
    // <T> List<T> listOf(T... items)
    // <T> Set<T> setOf(T... items)
    // boolean isUnmodifiable(Class<?> targetType)
    // boolean isSynchronized(Class<?> targetType)
    // <T> Collection<T> getUnmodifiableCollection(Collection<T> collection)
    // <T> Collection<T> getEmptyCollection(Collection<T> collection)
    // <T extends Collection<?>, E> Collection<E> getCheckedCollection(T collection, Class<E> type)
    // <T> Collection<T> getSynchronizedCollection(Collection<T> collection)

    /* ----------------------------------------------------------------------
     *  Basic functionality tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testIsEmpty_NullAndEmpty() {
        Assert.assertTrue(CollectionUtilities.isEmpty(null));
        Assert.assertTrue(CollectionUtilities.isEmpty(new ArrayList<>()));
        List<Integer> list = Arrays.asList(1, 2, 3);
        Assert.assertFalse(CollectionUtilities.isEmpty(list));
    }

    @Test
    public void testHasContent_NullAndEmpty() {
        Assert.assertFalse(CollectionUtilities.hasContent(null));
        Assert.assertFalse(CollectionUtilities.hasContent(new HashSet<>()));
        Set<String> set = new HashSet<>(Arrays.asList("a"));
        Assert.assertTrue(CollectionUtilities.hasContent(set));
    }

    @Test
    public void testSize_NullAndNonNull() {
        Assert.assertEquals(0, CollectionUtilities.size(null));
        List<String> list = Arrays.asList("x", "y");
        Assert.assertEquals(2, CollectionUtilities.size(list));
    }

    @Test
    public void testListOf_BasicAndEmpty() {
        List<String> list = CollectionUtilities.listOf("A", "B", "C");
        Assert.assertEquals(3, list.size());
        Assert.assertEquals(Arrays.asList("A", "B", "C"), list);
        // Verify immutability
        try {
            list.add("D");
            Assert.fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) { }

        // Empty list case (null array)
        List<Object> empty = CollectionUtilities.listOf();
        Assert.assertTrue(empty.isEmpty());
        try {
            empty.add(new Object());
            Assert.fail("Expected UnsupportedOperationException on empty list");
        } catch (UnsupportedOperationException ignored) { }
    }

    @Test(expected = NullPointerException.class)
    public void testListOf_NullElementThrows() {
        CollectionUtilities.listOf("A", null, "B");
    }

    @Test
    public void testSetOf_BasicAndEmpty() {
        Set<Integer> set = CollectionUtilities.setOf(1, 2, 3);
        Assert.assertEquals(3, set.size());
        Assert.assertTrue(set.containsAll(Arrays.asList(1, 2, 3)));

        // Verify immutability
        try {
            set.add(4);
            Assert.fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) { }

        // Empty set case (null array)
        Set<Object> empty = CollectionUtilities.setOf();
        Assert.assertTrue(empty.isEmpty());
        try {
            empty.add(new Object());
            Assert.fail("Expected UnsupportedOperationException on empty set");
        } catch (UnsupportedOperationException ignored) { }
    }

    @Test(expected = NullPointerException.class)
    public void testSetOf_NullElementThrows() {
        CollectionUtilities.setOf("X", null);
    }

    @Test
    public void testIsUnmodifiable_PositiveAndNegative() {
        // Positive – a known unmodifiable collection class
        List<Integer> unmod = Collections.unmodifiableList(new ArrayList<>());
        Assert.assertTrue(CollectionUtilities.isUnmodifiable(unmod.getClass()));

        // Negative – a regular mutable collection class
        Assert.assertFalse(CollectionUtilities.isUnmodifiable(ArrayList.class));
    }

    @Test(expected = NullPointerException.class)
    public void testIsUnmodifiable_NullClassThrows() {
        CollectionUtilities.isUnmodifiable(null);
    }

    @Test
    public void testIsSynchronized_PositiveAndNegative() {
        // Positive – a known synchronized collection class
        List<Integer> sync = Collections.synchronizedList(new ArrayList<>());
        Assert.assertTrue(CollectionUtilities.isSynchronized(sync.getClass()));

        // Negative – a regular mutable collection class
        Assert.assertFalse(CollectionUtilities.isSynchronized(LinkedHashSet.class));
    }

    @Test(expected = NullPointerException.class)
    public void testIsSynchronized_NullClassThrows() {
        CollectionUtilities.isSynchronized(null);
    }

    /* ----------------------------------------------------------------------
     *  Edge case / exception handling tests
     * ---------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testGetUnmodifiableCollection_NullInput() {
        CollectionUtilities.getUnmodifiableCollection(null);
    }

    @Test
    public void testGetUnmodifiableCollection_VariousTypes() {
        // NavigableSet (TreeSet implements NavigableSet)
        NavigableSet<String> navSet = new TreeSet<>(Arrays.asList("a", "b"));
        Collection<String> unmodNav = CollectionUtilities.getUnmodifiableCollection(navSet);
        Assert.assertTrue(unmodNav instanceof NavigableSet);
        try {
            ((NavigableSet<String>) unmodNav).add("c");
            Assert.fail("Expected UnsupportedOperationException for NavigableSet");
        } catch (UnsupportedOperationException ignored) { }

        // SortedSet (use a TreeSet but treat as SortedSet reference)
        SortedSet<Integer> sortedSet = new TreeSet<>(Arrays.asList(1, 2));
        Collection<Integer> unmodSorted = CollectionUtilities.getUnmodifiableCollection(sortedSet);
        Assert.assertTrue(unmodSorted instanceof SortedSet);
        try {
            ((SortedSet<Integer>) unmodSorted).add(3);
            Assert.fail("Expected UnsupportedOperationException for SortedSet");
        } catch (UnsupportedOperationException ignored) { }

        // Plain Set
        Set<Double> set = new HashSet<>(Arrays.asList(1.1, 2.2));
        Collection<Double> unmodSet = CollectionUtilities.getUnmodifiableCollection(set);
        Assert.assertTrue(unmodSet instanceof Set);
        try {
            ((Set<Double>) unmodSet).add(3.3);
            Assert.fail("Expected UnsupportedOperationException for Set");
        } catch (UnsupportedOperationException ignored) { }

        // List
        List<String> list = new ArrayList<>(Arrays.asList("x", "y"));
        Collection<String> unmodList = CollectionUtilities.getUnmodifiableCollection(list);
        Assert.assertTrue(unmodList instanceof List);
        try {
            ((List<String>) unmodList).add("z");
            Assert.fail("Expected UnsupportedOperationException for List");
        } catch (UnsupportedOperationException ignored) { }

        // Generic Collection (e.g., a custom collection – use ArrayDeque)
        Collection<Character> coll = new ArrayDeque<>(Arrays.asList('a', 'b'));
        Collection<Character> unmodColl = CollectionUtilities.getUnmodifiableCollection(coll);
        Assert.assertTrue(unmodColl instanceof Collection);
        try {
            unmodColl.add('c');
            Assert.fail("Expected UnsupportedOperationException for generic Collection");
        } catch (UnsupportedOperationException ignored) { }
    }

    @Test(expected = NullPointerException.class)
    public void testGetEmptyCollection_NullInput() {
        CollectionUtilities.getEmptyCollection(null);
    }

    @Test
    public void testGetEmptyCollection_ReturnsCorrectEmptyType() {
        // NavigableSet
        NavigableSet<String> nav = new TreeSet<>();
        Collection<String> emptyNav = CollectionUtilities.getEmptyCollection(nav);
        Assert.assertTrue(emptyNav instanceof NavigableSet);
        Assert.assertTrue(emptyNav.isEmpty());

        // SortedSet (use TreeSet but reference as SortedSet)
        SortedSet<Integer> sorted = new TreeSet<>();
        Collection<Integer> emptySorted = CollectionUtilities.getEmptyCollection(sorted);
        Assert.assertTrue(emptySorted instanceof SortedSet);
        Assert.assertTrue(emptySorted.isEmpty());

        // Set
        Set<Long> set = new HashSet<>();
        Collection<Long> emptySet = CollectionUtilities.getEmptyCollection(set);
        Assert.assertTrue(emptySet instanceof Set);
        Assert.assertTrue(emptySet.isEmpty());

        // List
        List<String> list = new ArrayList<>();
        Collection<String> emptyList = CollectionUtilities.getEmptyCollection(list);
        Assert.assertTrue(emptyList instanceof List);
        Assert.assertTrue(emptyList.isEmpty());

        // Other collection type (ArrayDeque)
        Collection<Double> deque = new ArrayDeque<>();
        Collection<Double> emptyDeque = CollectionUtilities.getEmptyCollection(deque);
        // Default is emptyList, which implements List, not Deque
        Assert.assertTrue(emptyDeque instanceof List);
        Assert.assertTrue(emptyDeque.isEmpty());

        // Verify immutability of the returned collections
        try {
            emptyList.add("test");
            Assert.fail("Expected UnsupportedOperationException on empty List");
        } catch (UnsupportedOperationException ignored) { }
    }

    @Test(expected = NullPointerException.class)
    public void testGetCheckedCollection_NullCollection() {
        CollectionUtilities.getCheckedCollection(null, String.class);
    }

    @Test(expected = NullPointerException.class)
    public void testGetCheckedCollection_NullClass() {
        List<String> list = new ArrayList<>();
        CollectionUtilities.getCheckedCollection(list, null);
    }

    @Test
    public void testGetCheckedCollection_TypeSafety() {
        // List example
        List<Object> rawList = new ArrayList<>(Arrays.asList("one", "two"));
        Collection<String> checked = CollectionUtilities.getCheckedCollection(rawList, String.class);
        // Adding a correct type succeeds
        checked.add("three");
        Assert.assertEquals(3, checked.size());

        // Adding an incorrect type throws ClassCastException
        try {
            // The cast to raw is necessary to compile the illegal add
            ((Collection) checked).add(42);
            Assert.fail("Expected ClassCastException for wrong element type");
        } catch (ClassCastException ignored) { }

        // Set example
        Set<Object> rawSet = new HashSet<>(Arrays.asList("a", "b"));
        Collection<Integer> checkedSet = CollectionUtilities.getCheckedCollection(rawSet, Integer.class);
        try {
            checkedSet.add(1);
            Assert.fail("Expected ClassCastException because underlying set contains Strings");
        } catch (ClassCastException ignored) { }
    }

    @Test(expected = NullPointerException.class)
    public void testGetSynchronizedCollection_NullInput() {
        CollectionUtilities.getSynchronizedCollection(null);
    }

    @Test
    public void testGetSynchronizedCollection_VariousTypes() {
        // List
        List<String> list = new ArrayList<>(Arrays.asList("a", "b"));
        Collection<String> syncList = CollectionUtilities.getSynchronizedCollection(list);
        Assert.assertNotSame(list, syncList);
        Assert.assertTrue(syncList instanceof List);
        syncList.add("c");
        Assert.assertEquals(3, syncList.size());

        // Set
        Set<Integer> set = new HashSet<>(Arrays.asList(1, 2));
        Collection<Integer> syncSet = CollectionUtilities.getSynchronizedCollection(set);
        Assert.assertNotSame(set, syncSet);
        Assert.assertTrue(syncSet instanceof Set);
        syncSet.add(3);
        Assert.assertEquals(3, syncSet.size());

        // NavigableSet
        NavigableSet<String> nav = new TreeSet<>(Arrays.asList("x"));
        Collection<String> syncNav = CollectionUtilities.getSynchronizedCollection(nav);
        Assert.assertNotSame(nav, syncNav);
        Assert.assertTrue(syncNav instanceof NavigableSet);
        syncNav.add("y");
        Assert.assertTrue(syncNav.containsAll(Arrays.asList("x", "y")));

        // Generic Collection (ArrayDeque)
        Collection<Double> deque = new ArrayDeque<>(Arrays.asList(1.0, 2.0));
        Collection<Double> syncDeque = CollectionUtilities.getSynchronizedCollection(deque);
        Assert.assertNotSame(deque, syncDeque);
        Assert.assertTrue(syncDeque instanceof Collection);
        syncDeque.add(3.0);
        Assert.assertEquals(3, syncDeque.size());
    }

    /* ----------------------------------------------------------------------
     *  Additional regression checks for duplicate handling and immutability
     * ---------------------------------------------------------------------- */

    @Test
    public void testSetOf_DuplicatesAreRemoved() {
        Set<String> set = CollectionUtilities.setOf("dup", "dup", "unique");
        Assert.assertEquals(2, set.size());
        Assert.assertTrue(set.containsAll(Arrays.asList("dup", "unique")));
    }

    @Test
    public void testListOf_ModifyOriginalDoesNotAffectReturned() {
        List<String> mutable = new ArrayList<>(Arrays.asList("a", "b"));
        List<String> unmod = CollectionUtilities.listOf(mutable.toArray(new String[0]));
        mutable.add("c");
        // The unmodifiable list should still have size 2
        Assert.assertEquals(2, unmod.size());
    }
}
