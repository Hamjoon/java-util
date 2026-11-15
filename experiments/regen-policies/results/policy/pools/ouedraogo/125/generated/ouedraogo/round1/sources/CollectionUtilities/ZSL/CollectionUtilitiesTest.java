package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.*;
import static org.junit.Assert.*;

public class CollectionUtilitiesTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private List<String> mutableList;
    private Set<String> mutableSet;
    private NavigableSet<String> mutableNavigableSet;
    private SortedSet<String> mutableSortedSet;
    private Collection<String> mutableCollection;

    @Before
    public void setUp() {
        mutableList = new ArrayList<>(Arrays.asList("a", "b"));
        mutableSet = new HashSet<>(Arrays.asList("c", "d"));
        mutableNavigableSet = new TreeSet<>(Arrays.asList("e", "f"));
        mutableSortedSet = new TreeSet<>(Arrays.asList("g", "h"));
        mutableCollection = new ArrayList<>(Arrays.asList("i", "j"));
    }

    /* -------------------------------------------------
     *  Null‑safe helpers
     * ------------------------------------------------- */

    @Test
    public void testIsEmpty_NullAndEmpty() {
        assertTrue(CollectionUtilities.isEmpty(null));
        assertTrue(CollectionUtilities.isEmpty(Collections.emptyList()));
        assertTrue(CollectionUtilities.isEmpty(new HashSet<>()));
    }

    @Test
    public void testIsEmpty_NonEmpty() {
        assertFalse(CollectionUtilities.isEmpty(Arrays.asList(1, 2, 3)));
        assertFalse(CollectionUtilities.isEmpty(new HashSet<>(Arrays.asList("x"))));
    }

    @Test
    public void testHasContent_NullAndEmpty() {
        assertFalse(CollectionUtilities.hasContent(null));
        assertFalse(CollectionUtilities.hasContent(Collections.emptyList()));
    }

    @Test
    public void testHasContent_NonEmpty() {
        assertTrue(CollectionUtilities.hasContent(Arrays.asList("a")));
        assertTrue(CollectionUtilities.hasContent(new HashSet<>(Arrays.asList(1, 2))));
    }

    @Test
    public void testSize_NullAndEmpty() {
        assertEquals(0, CollectionUtilities.size(null));
        assertEquals(0, CollectionUtilities.size(Collections.emptySet()));
    }

    @Test
    public void testSize_NonEmpty() {
        assertEquals(3, CollectionUtilities.size(Arrays.asList(1, 2, 3)));
        assertEquals(2, CollectionUtilities.size(new HashSet<>(Arrays.asList("x", "y"))));
    }

    /* -------------------------------------------------
     *  Immutable factories
     * ------------------------------------------------- */

    @Test
    public void testListOf_NullOrEmptyReturnsSharedEmpty() {
        List<String> empty1 = CollectionUtilities.listOf();
        List<String> empty2 = CollectionUtilities.listOf((String[]) null);
        assertSame(empty1, empty2);
        assertTrue(empty1.isEmpty());

        // verify immutability
        thrown.expect(UnsupportedOperationException.class);
        empty1.add("should fail");
    }

    @Test
    public void testListOf_CreatesUnmodifiableList() {
        List<Integer> list = CollectionUtilities.listOf(1, 2, 3);
        assertEquals(Arrays.asList(1, 2, 3), list);
        assertTrue(list instanceof List);
        assertTrue(list.getClass().getName().contains("Unmodifiable"));

        thrown.expect(UnsupportedOperationException.class);
        list.add(4);
    }

    @Test
    public void testSetOf_NullOrEmptyReturnsSharedEmpty() {
        Set<String> empty1 = CollectionUtilities.setOf();
        Set<String> empty2 = CollectionUtilities.setOf((String[]) null);
        assertSame(empty1, empty2);
        assertTrue(empty1.isEmpty());

        thrown.expect(UnsupportedOperationException.class);
        empty1.add("fail");
    }

    @Test
    public void testSetOf_CreatesUnmodifiableSet() {
        Set<String> set = CollectionUtilities.setOf("x", "y", "z");
        assertEquals(new HashSet<>(Arrays.asList("x", "y", "z")), set);
        assertTrue(set instanceof Set);
        assertTrue(set.getClass().getName().contains("Unmodifiable"));

        thrown.expect(UnsupportedOperationException.class);
        set.add("new");
    }

    /* -------------------------------------------------
     *  isUnmodifiable / isSynchronized
     * ------------------------------------------------- */

    @Test
    public void testIsUnmodifiable_PositiveAndNegative() {
        List<String> unmod = Collections.unmodifiableList(new ArrayList<>());
        Set<String> unmodSet = Collections.unmodifiableSet(new HashSet<>());
        assertTrue(CollectionUtilities.isUnmodifiable(unmod.getClass()));
        assertTrue(CollectionUtilities.isUnmodifiable(unmodSet.getClass()));
        assertFalse(CollectionUtilities.isUnmodifiable(ArrayList.class));

        thrown.expect(NullPointerException.class);
        CollectionUtilities.isUnmodifiable(null);
    }

    @Test
    public void testIsSynchronized_PositiveAndNegative() {
        List<String> sync = Collections.synchronizedList(new ArrayList<>());
        Set<String> syncSet = Collections.synchronizedSet(new HashSet<>());
        assertTrue(CollectionUtilities.isSynchronized(sync.getClass()));
        assertTrue(CollectionUtilities.isSynchronized(syncSet.getClass()));
        assertFalse(CollectionUtilities.isSynchronized(ArrayList.class));

        thrown.expect(NullPointerException.class);
        CollectionUtilities.isSynchronized(null);
    }

    /* -------------------------------------------------
     *  getUnmodifiableCollection
     * ------------------------------------------------- */

    @Test
    public void testGetUnmodifiableCollection_NullThrows() {
        thrown.expect(NullPointerException.class);
        CollectionUtilities.getUnmodifiableCollection(null);
    }

    @Test
    public void testGetUnmodifiableCollection_List() {
        Collection<String> result = CollectionUtilities.getUnmodifiableCollection(mutableList);
        assertTrue(result instanceof List);
        assertEquals(mutableList, result);
        thrown.expect(UnsupportedOperationException.class);
        result.add("new");
    }

    @Test
    public void testGetUnmodifiableCollection_Set() {
        Collection<String> result = CollectionUtilities.getUnmodifiableCollection(mutableSet);
        assertTrue(result instanceof Set);
        assertEquals(mutableSet, result);
        thrown.expect(UnsupportedOperationException.class);
        result.add("new");
    }

    @Test
    public void testGetUnmodifiableCollection_NavigableSet() {
        Collection<String> result = CollectionUtilities.getUnmodifiableCollection(mutableNavigableSet);
        assertTrue(result instanceof NavigableSet);
        assertEquals(mutableNavigableSet, result);
        thrown.expect(UnsupportedOperationException.class);
        ((NavigableSet<String>) result).add("new");
    }

    @Test
    public void testGetUnmodifiableCollection_SortedSet() {
        Collection<String> result = CollectionUtilities.getUnmodifiableCollection(mutableSortedSet);
        assertTrue(result instanceof SortedSet);
        assertEquals(mutableSortedSet, result);
        thrown.expect(UnsupportedOperationException.class);
        ((SortedSet<String>) result).add("new");
    }

    @Test
    public void testGetUnmodifiableCollection_OtherCollection() {
        Collection<String> result = CollectionUtilities.getUnmodifiableCollection(mutableCollection);
        assertFalse(result instanceof List);
        assertFalse(result instanceof Set);
        assertFalse(result instanceof SortedSet);
        assertFalse(result instanceof NavigableSet);
        thrown.expect(UnsupportedOperationException.class);
        result.add("new");
    }

    /* -------------------------------------------------
     *  getEmptyCollection
     * ------------------------------------------------- */

    @Test
    public void testGetEmptyCollection_NullThrows() {
        thrown.expect(NullPointerException.class);
        CollectionUtilities.getEmptyCollection(null);
    }

    @Test
    public void testGetEmptyCollection_NavigableSet() {
        Collection<String> empty = CollectionUtilities.getEmptyCollection(mutableNavigableSet);
        assertTrue(empty instanceof NavigableSet);
        assertTrue(empty.isEmpty());

        thrown.expect(UnsupportedOperationException.class);
        ((NavigableSet<String>) empty).add("x");
    }

    @Test
    public void testGetEmptyCollection_SortedSet() {
        Collection<String> empty = CollectionUtilities.getEmptyCollection(mutableSortedSet);
        assertTrue(empty instanceof SortedSet);
        assertTrue(empty.isEmpty());

        thrown.expect(UnsupportedOperationException.class);
        ((SortedSet<String>) empty).add("x");
    }

    @Test
    public void testGetEmptyCollection_Set() {
        Collection<String> empty = CollectionUtilities.getEmptyCollection(mutableSet);
        assertTrue(empty instanceof Set);
        assertTrue(empty.isEmpty());

        thrown.expect(UnsupportedOperationException.class);
        empty.add("x");
    }

    @Test
    public void testGetEmptyCollection_List() {
        Collection<String> empty = CollectionUtilities.getEmptyCollection(mutableList);
        assertTrue(empty instanceof List);
        assertTrue(empty.isEmpty());

        thrown.expect(UnsupportedOperationException.class);
        ((List<String>) empty).add("x");
    }

    @Test
    public void testGetEmptyCollection_OtherCollection() {
        Collection<String> empty = CollectionUtilities.getEmptyCollection(mutableCollection);
        assertTrue(empty instanceof List); // default is emptyList()
        assertTrue(empty.isEmpty());

        thrown.expect(UnsupportedOperationException.class);
        empty.add("x");
    }

    /* -------------------------------------------------
     *  getCheckedCollection
     * ------------------------------------------------- */

    @Test
    public void testGetCheckedCollection_NullCollectionThrows() {
        thrown.expect(NullPointerException.class);
        CollectionUtilities.getCheckedCollection(null, String.class);
    }

    @Test
    public void testGetCheckedCollection_NullClassThrows() {
        thrown.expect(NullPointerException.class);
        CollectionUtilities.getCheckedCollection(mutableList, null);
    }

    @Test
    public void testGetCheckedCollection_ListAcceptsCorrectTypeAndRejectsWrong() {
        Collection<String> checked = CollectionUtilities.getCheckedCollection(mutableList, String.class);
        assertTrue(checked instanceof List);
        checked.add("new"); // ok

        thrown.expect(ClassCastException.class);
        // unchecked cast needed for compilation
        ((Collection) checked).add(42);
    }

    @Test
    public void testGetCheckedCollection_SetAcceptsCorrectTypeAndRejectsWrong() {
        Collection<Integer> checked = CollectionUtilities.getCheckedCollection(mutableSet, Integer.class);
        assertTrue(checked instanceof Set);
        checked.add(99); // ok

        thrown.expect(ClassCastException.class);
        ((Collection) checked).add("bad");
    }

    @Test
    public void testGetCheckedCollection_NavigableSet() {
        Collection<String> checked = CollectionUtilities.getCheckedCollection(mutableNavigableSet, String.class);
        assertTrue(checked instanceof NavigableSet);
        checked.add("new");

        thrown.expect(ClassCastException.class);
        ((Collection) checked).add(1);
    }

    @Test
    public void testGetCheckedCollection_SortedSet() {
        Collection<Double> checked = CollectionUtilities.getCheckedCollection(mutableSortedSet, Double.class);
        assertTrue(checked instanceof SortedSet);
        checked.add(3.14);

        thrown.expect(ClassCastException.class);
        ((Collection) checked).add("bad");
    }

    @Test
    public void testGetCheckedCollection_OtherCollection() {
        Collection<String> checked = CollectionUtilities.getCheckedCollection(mutableCollection, String.class);
        assertTrue(checked instanceof Collection);
        checked.add("new");

        thrown.expect(ClassCastException.class);
        ((Collection) checked).add(123);
    }

    /* -------------------------------------------------
     *  getSynchronizedCollection
     * ------------------------------------------------- */

    @Test
    public void testGetSynchronizedCollection_NullThrows() {
        thrown.expect(NullPointerException.class);
        CollectionUtilities.getSynchronizedCollection(null);
    }

    @Test
    public void testGetSynchronizedCollection_List() {
        Collection<String> sync = CollectionUtilities.getSynchronizedCollection(mutableList);
        assertTrue(sync instanceof List);
        assertNotSame(mutableList, sync);
        sync.add("added");
        assertTrue(mutableList.contains("added"));
    }

    @Test
    public void testGetSynchronizedCollection_Set() {
        Collection<String> sync = CollectionUtilities.getSynchronizedCollection(mutableSet);
        assertTrue(sync instanceof Set);
        sync.add("new");
        assertTrue(mutableSet.contains("new"));
    }

    @Test
    public void testGetSynchronizedCollection_NavigableSet() {
        Collection<String> sync = CollectionUtilities.getSynchronizedCollection(mutableNavigableSet);
        assertTrue(sync instanceof NavigableSet);
        sync.add("z");
        assertTrue(mutableNavigableSet.contains("z"));
    }

    @Test
    public void testGetSynchronizedCollection_SortedSet() {
        Collection<String> sync = CollectionUtilities.getSynchronizedCollection(mutableSortedSet);
        assertTrue(sync instanceof SortedSet);
        sync.add("a");
        assertTrue(mutableSortedSet.contains("a"));
    }

    @Test
    public void testGetSynchronizedCollection_OtherCollection() {
        Collection<String> sync = CollectionUtilities.getSynchronizedCollection(mutableCollection);
        assertFalse(sync instanceof List);
        assertFalse(sync instanceof Set);
        sync.add("x");
        assertTrue(mutableCollection.contains("x"));
    }
}
