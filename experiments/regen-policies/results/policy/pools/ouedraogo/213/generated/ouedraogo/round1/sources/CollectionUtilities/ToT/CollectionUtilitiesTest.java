package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link CollectionUtilities}.
 *
 * <p>Each public method of {@code CollectionUtilities} is exercised with
 * typical use cases, edge cases and error scenarios.  The tests use only
 * the API that is visible in the source code and the {@code CollectionsWrappers}
 * helper class – no external mocking frameworks are employed.</p>
 */
public class CollectionUtilitiesTest {

    /** Re‑usable collections used across several tests. */
    private List<String> mutableList;
    private Set<Integer> mutableSet;
    private NavigableSet<String> mutableNavigableSet;
    private Collection<String> mutableCollection;
    private ArrayDeque<String> mutableDeque; // non‑List, non‑Set collection

    @Before
    public void setUp() {
        mutableList = new ArrayList<>(Arrays.asList("a", "b", "c"));
        mutableSet = new HashSet<>(Arrays.asList(1, 2, 3));
        mutableNavigableSet = new TreeSet<>(Arrays.asList("x", "y", "z"));
        mutableCollection = new HashSet<>(Arrays.asList("c1", "c2"));
        mutableDeque = new ArrayDeque<>(Arrays.asList("d1", "d2"));
    }

    /* ---------------------------------------------------------------------- */
    /*                     Null‑safe checks (isEmpty, hasContent, size)       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIsEmpty_NullAndEmpty() {
        assertTrue(CollectionUtilities.isEmpty(null));
        assertTrue(CollectionUtilities.isEmpty(Collections.emptyList()));
        assertTrue(CollectionUtilities.isEmpty(new HashSet<>()));
    }

    @Test
    public void testIsEmpty_NonEmpty() {
        assertFalse(CollectionUtilities.isEmpty(mutableList));
        assertFalse(CollectionUtilities.isEmpty(mutableSet));
    }

    @Test
    public void testHasContent_NullAndEmpty() {
        assertFalse(CollectionUtilities.hasContent(null));
        assertFalse(CollectionUtilities.hasContent(Collections.emptySet()));
    }

    @Test
    public void testHasContent_NonEmpty() {
        assertTrue(CollectionUtilities.hasContent(mutableList));
        assertTrue(CollectionUtilities.hasContent(mutableSet));
    }

    @Test
    public void testSize_NullAndVariousCollections() {
        assertEquals(0, CollectionUtilities.size(null));
        assertEquals(0, CollectionUtilities.size(Collections.emptyList()));
        assertEquals(mutableList.size(), CollectionUtilities.size(mutableList));
        assertEquals(mutableSet.size(), CollectionUtilities.size(mutableSet));
        assertEquals(mutableNavigableSet.size(), CollectionUtilities.size(mutableNavigableSet));
    }

    /* ---------------------------------------------------------------------- */
    /*                     Immutable collection factories (listOf, setOf)    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testListOf_NullOrEmptyReturnsSharedEmpty() {
        List<String> empty1 = CollectionUtilities.listOf();
        List<String> empty2 = CollectionUtilities.listOf((String[]) null);
        assertSame(empty1, empty2); // both reference the same constant
        assertTrue(empty1.isEmpty());
        // The list must be unmodifiable
        try {
            empty1.add("x");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {}
    }

    @Test
    public void testListOf_CreatesImmutableList() {
        List<String> list = CollectionUtilities.listOf("one", "two", "three");
        assertEquals(3, list.size());
        assertEquals(Arrays.asList("one", "two", "three"), list);
        // verify immutability
        try {
            list.remove(0);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {}
    }

    @Test(expected = NullPointerException.class)
    public void testListOf_NullElementThrowsNPE() {
        CollectionUtilities.listOf("good", null, "bad");
    }

    @Test
    public void testSetOf_NullOrEmptyReturnsSharedEmpty() {
        Set<Integer> empty1 = CollectionUtilities.setOf();
        Set<Integer> empty2 = CollectionUtilities.setOf((Integer[]) null);
        assertSame(empty1, empty2);
        assertTrue(empty1.isEmpty());
        try {
            empty1.add(1);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {}
    }

    @Test
    public void testSetOf_CreatesImmutableSetPreservingInsertionOrder() {
        Set<String> set = CollectionUtilities.setOf("first", "second", "third");
        assertEquals(3, set.size());
        // LinkedHashSet preserves order; verify via iterator
        Iterator<String> it = set.iterator();
        assertEquals("first", it.next());
        assertEquals("second", it.next());
        assertEquals("third", it.next());
        // verify immutability
        try {
            set.clear();
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {}
    }

    @Test(expected = NullPointerException.class)
    public void testSetOf_NullElementThrowsNPE() {
        CollectionUtilities.setOf("a", null);
    }

    /* ---------------------------------------------------------------------- */
    /*                     Type detection (isUnmodifiable, isSynchronized)  */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIsUnmodifiable_RecognisesKnownUnmodifiableTypes() {
        // Use the classes supplied by CollectionsWrappers
        Class<?> listClass = CollectionsWrappers.getUnmodifiableListClass();
        Class<?> setClass = CollectionsWrappers.getUnmodifiableSetClass();
        Class<?> sortedSetClass = CollectionsWrappers.getUnmodifiableSortedSetClass();
        Class<?> navigableSetClass = CollectionsWrappers.getUnmodifiableNavigableSetClass();

        assertTrue(CollectionUtilities.isUnmodifiable(listClass));
        assertTrue(CollectionUtilities.isUnmodifiable(setClass));
        assertTrue(CollectionUtilities.isUnmodifiable(sortedSetClass));
        assertTrue(CollectionUtilities.isUnmodifiable(navigableSetClass));

        // A mutable class must be reported as false
        assertFalse(CollectionUtilities.isUnmodifiable(ArrayList.class));
        assertFalse(CollectionUtilities.isUnmodifiable(HashSet.class));
    }

    @Test(expected = NullPointerException.class)
    public void testIsUnmodifiable_NullArgumentThrowsNPE() {
        CollectionUtilities.isUnmodifiable(null);
    }

    @Test
    public void testIsSynchronized_RecognisesKnownSynchronizedTypes() {
        Class<?> listClass = CollectionsWrappers.getSynchronizedListClass();
        Class<?> setClass = CollectionsWrappers.getSynchronizedSetClass();
        Class<?> sortedSetClass = CollectionsWrappers.getSynchronizedSortedSetClass();
        Class<?> navigableSetClass = CollectionsWrappers.getSynchronizedNavigableSetClass();

        assertTrue(CollectionUtilities.isSynchronized(listClass));
        assertTrue(CollectionUtilities.isSynchronized(setClass));
        assertTrue(CollectionUtilities.isSynchronized(sortedSetClass));
        assertTrue(CollectionUtilities.isSynchronized(navigableSetClass));

        // A non‑synchronized class must be reported as false
        assertFalse(CollectionUtilities.isSynchronized(LinkedList.class));
        assertFalse(CollectionUtilities.isSynchronized(TreeSet.class));
    }

    @Test(expected = NullPointerException.class)
    public void testIsSynchronized_NullArgumentThrowsNPE() {
        CollectionUtilities.isSynchronized(null);
    }

    /* ---------------------------------------------------------------------- */
    /*                     Unmodifiable wrapper (getUnmodifiableCollection) */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetUnmodifiableCollection_NavigableSet() {
        Collection<String> result = CollectionUtilities.getUnmodifiableCollection(mutableNavigableSet);
        assertTrue(result instanceof NavigableSet);
        assertTrue(result instanceof Set);
        // Ensure it is truly unmodifiable
        try {
            ((NavigableSet<String>) result).add("new");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {}
    }

    @Test
    public void testGetUnmodifiableCollection_SortedSet() {
        SortedSet<Integer> sorted = new TreeSet<>(Arrays.asList(5, 1, 3));
        Collection<Integer> result = CollectionUtilities.getUnmodifiableCollection(sorted);
        assertTrue(result instanceof SortedSet);
        try {
            ((SortedSet<Integer>) result).add(10);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {}
    }

    @Test
    public void testGetUnmodifiableCollection_Set() {
        Collection<Integer> result = CollectionUtilities.getUnmodifiableCollection(mutableSet);
        assertTrue(result instanceof Set);
        assertFalse(result instanceof List);
        try {
            ((Set<Integer>) result).remove(1);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {}
    }

    @Test
    public void testGetUnmodifiableCollection_List() {
        Collection<String> result = CollectionUtilities.getUnmodifiableCollection(mutableList);
        assertTrue(result instanceof List);
        try {
            ((List<String>) result).add("new");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {}
    }

    @Test
    public void testGetUnmodifiableCollection_OtherCollection() {
        Collection<String> result = CollectionUtilities.getUnmodifiableCollection(mutableDeque);
        assertTrue(result instanceof Collection);
        assertFalse(result instanceof List);
        assertFalse(result instanceof Set);
        try {
            result.add("new");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {}
    }

    @Test(expected = NullPointerException.class)
    public void testGetUnmodifiableCollection_NullArgumentThrowsNPE() {
        CollectionUtilities.getUnmodifiableCollection(null);
    }

    /* ---------------------------------------------------------------------- */
    /*                     Empty collection factory (getEmptyCollection)   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetEmptyCollection_NavigableSet() {
        Collection<String> empty = CollectionUtilities.getEmptyCollection(mutableNavigableSet);
        assertTrue(empty instanceof NavigableSet);
        assertTrue(empty.isEmpty());
        // verify immutability
        try {
            ((NavigableSet<String>) empty).add("x");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {}
    }

    @Test
    public void testGetEmptyCollection_SortedSet() {
        Collection<Integer> empty = CollectionUtilities.getEmptyCollection(new TreeSet<>());
        assertTrue(empty instanceof SortedSet);
        assertTrue(empty.isEmpty());
    }

    @Test
    public void testGetEmptyCollection_Set() {
        Collection<Integer> empty = CollectionUtilities.getEmptyCollection(new HashSet<>());
        assertTrue(empty instanceof Set);
        assertTrue(empty.isEmpty());
    }

    @Test
    public void testGetEmptyCollection_List() {
        Collection<String> empty = CollectionUtilities.getEmptyCollection(new ArrayList<>());
        assertTrue(empty instanceof List);
        assertTrue(empty.isEmpty());
    }

    @Test
    public void testGetEmptyCollection_OtherCollectionDefaultsToEmptyList() {
        Collection<String> empty = CollectionUtilities.getEmptyCollection(mutableDeque);
        assertTrue(empty instanceof List);
        assertTrue(empty.isEmpty());
    }

    @Test(expected = NullPointerException.class)
    public void testGetEmptyCollection_NullArgumentThrowsNPE() {
        CollectionUtilities.getEmptyCollection(null);
    }

    /* ---------------------------------------------------------------------- */
    /*                     Checked collection wrapper (getCheckedCollection) */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetCheckedCollection_ListAllowsCorrectType() {
        List<Object> original = new ArrayList<>(Arrays.asList("a", "b"));
        Collection<String> checked = CollectionUtilities.getCheckedCollection(original, String.class);
        // Adding correct type works
        checked.add("c");
        assertEquals(3, checked.size());
        // Original list reflects the addition
        assertEquals(Arrays.asList("a", "b", "c"), original);
    }

    @Test
    public void testGetCheckedCollection_ListRejectsWrongType() {
        List<Object> original = new ArrayList<>(Arrays.asList("x"));
        Collection<String> checked = CollectionUtilities.getCheckedCollection(original, String.class);
        try {
            // This should cause a ClassCastException at runtime
            ((Collection) checked).add(123);
            fail("Expected ClassCastException");
        } catch (ClassCastException ignored) {}
    }

    @Test(expected = NullPointerException.class)
    public void testGetCheckedCollection_NullCollectionThrowsNPE() {
        CollectionUtilities.getCheckedCollection(null, String.class);
    }

    @Test(expected = NullPointerException.class)
    public void testGetCheckedCollection_NullTypeThrowsNPE() {
        CollectionUtilities.getCheckedCollection(new ArrayList<>(), null);
    }

    @Test
    public void testGetCheckedCollection_NavigableSet() {
        NavigableSet<Integer> nav = new TreeSet<>(Arrays.asList(1, 2));
        Collection<Integer> checked = CollectionUtilities.getCheckedCollection(nav, Integer.class);
        checked.add(3);
        assertTrue(nav.contains(3));
        try {
            ((Collection) checked).add("string");
            fail("Expected ClassCastException");
        } catch (ClassCastException ignored) {}
    }

    /* ---------------------------------------------------------------------- */
    /*                     Synchronized collection wrapper (getSynchronizedCollection) */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetSynchronizedCollection_List() {
        List<String> original = new ArrayList<>(Arrays.asList("a"));
        Collection<String> sync = CollectionUtilities.getSynchronizedCollection(original);
        assertTrue(sync instanceof List);
        // The returned collection is a different wrapper instance
        assertNotSame(original, sync);
        // Modifications go through to the original collection
        sync.add("b");
        assertEquals(Arrays.asList("a", "b"), original);
    }

    @Test
    public void testGetSynchronizedCollection_Set() {
        Set<Integer> original = new HashSet<>(Arrays.asList(10));
        Collection<Integer> sync = CollectionUtilities.getSynchronizedCollection(original);
        assertTrue(sync instanceof Set);
        sync.add(20);
        assertTrue(original.contains(20));
    }

    @Test
    public void testGetSynchronizedCollection_NavigableSet() {
        NavigableSet<String> original = new TreeSet<>(Arrays.asList("one"));
        Collection<String> sync = CollectionUtilities.getSynchronizedCollection(original);
        assertTrue(sync instanceof NavigableSet);
        sync.add("two");
        assertTrue(original.contains("two"));
    }

    @Test
    public void testGetSynchronizedCollection_OtherCollection() {
        Collection<String> original = mutableDeque;
        Collection<String> sync = CollectionUtilities.getSynchronizedCollection(original);
        assertTrue(sync instanceof Collection);
        sync.add("new");
        assertTrue(original.contains("new"));
    }

    @Test(expected = NullPointerException.class)
    public void testGetSynchronizedCollection_NullArgumentThrowsNPE() {
        CollectionUtilities.getSynchronizedCollection(null);
    }
}
