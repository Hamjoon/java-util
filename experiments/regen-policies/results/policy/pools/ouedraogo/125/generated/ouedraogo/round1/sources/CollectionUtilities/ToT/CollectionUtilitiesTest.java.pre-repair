package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class CollectionUtilitiesTest {

    /* --------------------------------------------------------------------- */
    /*  isEmpty, hasContent, size                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testIsEmpty() {
        assertTrue(CollectionUtilities.isEmpty(null));
        assertTrue(CollectionUtilities.isEmpty(Collections.emptyList()));
        assertFalse(CollectionUtilities.isEmpty(Arrays.asList(1, 2, 3)));
    }

    @Test
    public void testHasContent() {
        assertFalse(CollectionUtilities.hasContent(null));
        assertFalse(CollectionUtilities.hasContent(Collections.emptySet()));
        assertTrue(CollectionUtilities.hasContent(Collections.singleton("x")));
    }

    @Test
    public void testSize() {
        assertEquals(0, CollectionUtilities.size(null));
        assertEquals(0, CollectionUtilities.size(Collections.emptyList()));
        assertEquals(3, CollectionUtilities.size(Arrays.asList("a", "b", "c")));
    }

    /* --------------------------------------------------------------------- */
    /*  listOf                                                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testListOfNullOrEmpty() {
        List<String> empty = CollectionUtilities.listOf();
        assertSame(empty, CollectionUtilities.listOf((String[]) null));
        assertTrue(empty.isEmpty());
        // must be the same immutable empty instance used by the class
        assertSame(empty, (List<String>) CollectionUtilities.listOf());
    }

    @Test
    public void testListOfTypical() {
        List<Integer> list = CollectionUtilities.listOf(1, 2, 3);
        assertEquals(Arrays.asList(1, 2, 3), list);
        // verify immutability
        try {
            list.add(4);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) { }
    }

    @Test
    public void testListOfWithNullElement() {
        List<String> list = CollectionUtilities.listOf("a", null, "b");
        assertEquals(3, list.size());
        assertNull(list.get(1));
        // still immutable
        try {
            list.set(0, "z");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) { }
    }

    /* --------------------------------------------------------------------- */
    /*  setOf                                                                */
    /* --------------------------------------------------------------------- */

    @Test
    public void testSetOfNullOrEmpty() {
        Set<String> empty = CollectionUtilities.setOf();
        assertSame(empty, CollectionUtilities.setOf((String[]) null));
        assertTrue(empty.isEmpty());
    }

    @Test
    public void testSetOfTypical() {
        Set<String> set = CollectionUtilities.setOf("a", "b", "c");
        assertEquals(new LinkedHashSet<>(Arrays.asList("a", "b", "c")), set);
        // immutability
        try {
            set.add("d");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) { }
    }

    @Test
    public void testSetOfDuplicatesAndNulls() {
        Set<String> set = CollectionUtilities.setOf("x", "y", "x", null);
        // LinkedHashSet preserves insertion order and removes duplicates
        List<String> expectedOrder = Arrays.asList("x", "y", null);
        assertEquals(expectedOrder, new ArrayList<>(set));
        // immutability
        try {
            set.remove(null);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) { }
    }

    /* --------------------------------------------------------------------- */
    /*  isUnmodifiable / isSynchronized                                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void testIsUnmodifiableTrueAndFalse() {
        Class<?> unmodClass = CollectionsWrappers.getUnmodifiableCollectionClass();
        assertTrue(CollectionUtilities.isUnmodifiable(unmodClass));

        // a regular mutable collection class should be false
        assertFalse(CollectionUtilities.isUnmodifiable(ArrayList.class));
    }

    @Test(expected = NullPointerException.class)
    public void testIsUnmodifiableNull() {
        CollectionUtilities.isUnmodifiable(null);
    }

    @Test
    public void testIsSynchronizedTrueAndFalse() {
        Class<?> syncClass = CollectionsWrappers.getSynchronizedCollectionClass();
        assertTrue(CollectionUtilities.isSynchronized(syncClass));
        assertFalse(CollectionUtilities.isSynchronized(HashSet.class));
    }

    @Test(expected = NullPointerException.class)
    public void testIsSynchronizedNull() {
        CollectionUtilities.isSynchronized(null);
    }

    /* --------------------------------------------------------------------- */
    /*  getUnmodifiableCollection                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetUnmodifiableCollectionByType() {
        // NavigableSet
        NavigableSet<Integer> nav = new TreeSet<>(Arrays.asList(3, 1, 2));
        Collection<Integer> unmodNav = CollectionUtilities.getUnmodifiableCollection(nav);
        assertTrue(unmodNav instanceof NavigableSet);
        try {
            ((NavigableSet<Integer>) unmodNav).add(4);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) { }

        // SortedSet (but not NavigableSet)
        SortedSet<String> sorted = new TreeSet<>(Arrays.asList("b", "a"));
        Collection<String> unmodSorted = CollectionUtilities.getUnmodifiableCollection(sorted);
        assertTrue(unmodSorted instanceof SortedSet);
        try {
            ((SortedSet<String>) unmodSorted).add("c");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) { }

        // Set
        Set<Double> set = new HashSet<>(Arrays.asList(1.1, 2.2));
        Collection<Double> unmodSet = CollectionUtilities.getUnmodifiableCollection(set);
        assertTrue(unmodSet instanceof Set);
        try {
            ((Set<Double>) unmodSet).add(3.3);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) { }

        // List
        List<String> list = new ArrayList<>(Arrays.asList("x", "y"));
        Collection<String> unmodList = CollectionUtilities.getUnmodifiableCollection(list);
        assertTrue(unmodList instanceof List);
        try {
            ((List<String>) unmodList).add("z");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) { }

        // Generic Collection (e.g., custom collection)
        Collection<Integer> generic = new ArrayDeque<>(Arrays.asList(5, 6));
        Collection<Integer> unmodGeneric = CollectionUtilities.getUnmodifiableCollection(generic);
        assertFalse(unmodGeneric instanceof List);
        assertFalse(unmodGeneric instanceof Set);
        try {
            unmodGeneric.add(7);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) { }
    }

    @Test(expected = NullPointerException.class)
    public void testGetUnmodifiableCollectionNull() {
        CollectionUtilities.getUnmodifiableCollection(null);
    }

    /* --------------------------------------------------------------------- */
    /*  getEmptyCollection                                                   */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetEmptyCollectionByType() {
        // NavigableSet
        NavigableSet<String> nav = new TreeSet<>(Arrays.asList("a"));
        Collection<String> emptyNav = CollectionUtilities.getEmptyCollection(nav);
        assertTrue(emptyNav instanceof NavigableSet);
        assertTrue(emptyNav.isEmpty());

        // SortedSet
        SortedSet<Integer> sorted = new TreeSet<>(Arrays.asList(1, 2));
        Collection<Integer> emptySorted = CollectionUtilities.getEmptyCollection(sorted);
        assertTrue(emptySorted instanceof SortedSet);
        assertTrue(emptySorted.isEmpty());

        // Set
        Set<String> set = new HashSet<>(Arrays.asList("x"));
        Collection<String> emptySet = CollectionUtilities.getEmptyCollection(set);
        assertTrue(emptySet instanceof Set);
        assertTrue(emptySet.isEmpty());

        // List
        List<Integer> list = new ArrayList<>(Arrays.asList(10));
        Collection<Integer> emptyList = CollectionUtilities.getEmptyCollection(list);
        assertTrue(emptyList instanceof List);
        assertTrue(emptyList.isEmpty());

        // Other collection (e.g., Queue)
        Queue<String> queue = new ArrayDeque<>();
        Collection<String> emptyOther = CollectionUtilities.getEmptyCollection(queue);
        assertTrue(emptyOther instanceof List); // default to emptyList as per implementation
        assertTrue(emptyOther.isEmpty());

        // verify immutability
        try {
            emptyList.add(99);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) { }
    }

    @Test(expected = NullPointerException.class)
    public void testGetEmptyCollectionNull() {
        CollectionUtilities.getEmptyCollection(null);
    }

    /* --------------------------------------------------------------------- */
    /*  getCheckedCollection                                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetCheckedCollectionAllowsCorrectType() {
        List<Object> rawList = new ArrayList<>(Arrays.asList("one", "two"));
        Collection<String> checked = CollectionUtilities.getCheckedCollection(rawList, String.class);
        assertTrue(checked instanceof List);
        checked.add("three"); // should succeed
        assertEquals(3, checked.size());
    }

    @Test
    public void testGetCheckedCollectionRejectsWrongType() {
        List<Object> rawList = new ArrayList<>(Arrays.asList("a"));
        Collection<String> checked = CollectionUtilities.getCheckedCollection(rawList, String.class);
        try {
            // This cast is safe at compile time because getCheckedCollection returns Collection<E>
            ((Collection) checked).add(123); // raw add to trigger ClassCastException
            fail("Expected ClassCastException");
        } catch (ClassCastException ignored) { }
    }

    @Test
    public void testGetCheckedCollectionForVariousTypes() {
        // NavigableSet
        NavigableSet<Integer> nav = new TreeSet<>(Arrays.asList(1, 2));
        Collection<Integer> checkedNav = CollectionUtilities.getCheckedCollection(nav, Integer.class);
        assertTrue(checkedNav instanceof NavigableSet);
        checkedNav.add(3);
        try {
            ((Collection) checkedNav).add("bad");
            fail("Expected ClassCastException");
        } catch (ClassCastException ignored) { }

        // SortedSet
        SortedSet<String> sorted = new TreeSet<>(Arrays.asList("a"));
        Collection<String> checkedSorted = CollectionUtilities.getCheckedCollection(sorted, String.class);
        assertTrue(checkedSorted instanceof SortedSet);
        checkedSorted.add("b");

        // Set
        Set<Double> set = new HashSet<>(Arrays.asList(1.0));
        Collection<Double> checkedSet = CollectionUtilities.getCheckedCollection(set, Double.class);
        assertTrue(checkedSet instanceof Set);
        checkedSet.add(2.0);

        // List
        List<Long> list = new ArrayList<>(Arrays.asList(10L));
        Collection<Long> checkedList = CollectionUtilities.getCheckedCollection(list, Long.class);
        assertTrue(checkedList instanceof List);
        checkedList.add(20L);
    }

    @Test(expected = NullPointerException.class)
    public void testGetCheckedCollectionNullCollection() {
        CollectionUtilities.getCheckedCollection(null, String.class);
    }

    @Test(expected = NullPointerException.class)
    public void testGetCheckedCollectionNullType() {
        CollectionUtilities.getCheckedCollection(new ArrayList<>(), null);
    }

    /* --------------------------------------------------------------------- */
    /*  getSynchronizedCollection                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetSynchronizedCollectionByType() {
        // NavigableSet
        NavigableSet<Integer> nav = new TreeSet<>(Arrays.asList(5, 6));
        Collection<Integer> syncNav = CollectionUtilities.getSynchronizedCollection(nav);
        assertTrue(syncNav instanceof NavigableSet);
        syncNav.add(7);
        assertTrue(syncNav.contains(7));

        // SortedSet
        SortedSet<String> sorted = new TreeSet<>(Arrays.asList("x"));
        Collection<String> syncSorted = CollectionUtilities.getSynchronizedCollection(sorted);
        assertTrue(syncSorted instanceof SortedSet);
        syncSorted.add("y");

        // Set
        Set<Double> set = new HashSet<>(Arrays.asList(3.14));
        Collection<Double> syncSet = CollectionUtilities.getSynchronizedCollection(set);
        assertTrue(syncSet instanceof Set);
        syncSet.remove(3.14);

        // List
        List<String> list = new ArrayList<>(Arrays.asList("first"));
        Collection<String> syncList = CollectionUtilities.getSynchronizedCollection(list);
        assertTrue(syncList instanceof List);
        syncList.add("second");

        // Generic collection (e.g., Deque)
        Deque<Integer> deque = new ArrayDeque<>(Arrays.asList(1, 2));
        Collection<Integer> syncDeque = CollectionUtilities.getSynchronizedCollection(deque);
        // default wrapper is a synchronizedCollection, which is not a List/Set/NavigableSet/SortedSet
        assertFalse(syncDeque instanceof List);
        assertFalse(syncDeque instanceof Set);
        syncDeque.add(3);
    }

    @Test(expected = NullPointerException.class)
    public void testGetSynchronizedCollectionNull() {
        CollectionUtilities.getSynchronizedCollection(null);
    }

    /* --------------------------------------------------------------------- */
    /*  Defensive test for internal constants (optional)                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void testInternalEmptyConstantsAreUnmodifiable() {
        // unmodifiableEmptyList
        List<?> emptyList = (List<?>) CollectionUtilities.listOf();
        try {
            emptyList.add("x");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) { }

        // unmodifiableEmptySet
        Set<?> emptySet = (Set<?>) CollectionUtilities.setOf();
        try {
            emptySet.add("x");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) { }
    }
}
