package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link CollectionUtilities}.
 * The tests cover null‑safe helpers, immutable factories, type‑checking utilities,
 * and the various wrapper methods (unmodifiable, checked, synchronized, empty).
 *
 * The test class uses only the public API shown in the source and the
 * {@code CollectionsWrappers} helper class – no additional libraries or
 * mocking frameworks are required.
 */
public class CollectionUtilitiesTest {

    /* --------------------------------------------------------------------- */
    /*  Helper data used by many tests                                       */
    /* --------------------------------------------------------------------- */
    private List<String> mutableList;
    private Set<Integer> mutableSet;
    private NavigableSet<String> mutableNavigableSet;
    private SortedSet<Double> mutableSortedSet;
    private Collection<Object> mutableCollection;

    @Before
    public void setUp() {
        mutableList = new ArrayList<>(Arrays.asList("a", "b", "c"));
        mutableSet = new HashSet<>(Arrays.asList(1, 2, 3));
        mutableNavigableSet = new TreeSet<>(Arrays.asList("x", "y", "z"));
        mutableSortedSet = new TreeSet<>(Arrays.asList(1.1, 2.2, 3.3));
        mutableCollection = new ArrayList<>(Arrays.asList(new Object(), new Object()));
    }

    @After
    public void tearDown() {
        mutableList = null;
        mutableSet = null;
        mutableNavigableSet = null;
        mutableSortedSet = null;
        mutableCollection = null;
    }

    /* --------------------------------------------------------------------- */
    /*  Null‑safe checks                                                     */
    /* --------------------------------------------------------------------- */

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
        assertFalse(CollectionUtilities.hasContent(Collections.emptyList()));
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

    /* --------------------------------------------------------------------- */
    /*  Immutable factory methods                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testListOf_NullOrEmpty() {
        List<String> empty1 = CollectionUtilities.listOf((String[]) null);
        List<String> empty2 = CollectionUtilities.listOf();

        assertSame(empty1, empty2); // both should be the same immutable empty list instance
        assertTrue(empty1.isEmpty());

        // verify immutability
        try {
            empty1.add("x");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) { }
    }

    @Test
    public void testListOf_WithElements() {
        List<Integer> list = CollectionUtilities.listOf(1, 2, 3);
        assertEquals(Arrays.asList(1, 2, 3), list);
        assertTrue(list instanceof java.util.Collections.UnmodifiableRandomAccessList
                || list.getClass().getName().contains("Unmodifiable"));
        // immutability check
        try {
            list.remove(0);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) { }
    }

    @Test
    public void testSetOf_NullOrEmpty() {
        Set<String> empty1 = CollectionUtilities.setOf((String[]) null);
        Set<String> empty2 = CollectionUtilities.setOf();

        assertSame(empty1, empty2); // both should be the same immutable empty set instance
        assertTrue(empty1.isEmpty());

        try {
            empty1.add("x");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) { }
    }

    @Test
    public void testSetOf_WithElements() {
        Set<String> set = CollectionUtilities.setOf("a", "b", "c", "a"); // duplicate "a" should be ignored
        assertEquals(3, set.size());
        assertTrue(set.containsAll(Arrays.asList("a", "b", "c")));

        try {
            set.add("d");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) { }
    }

    /* --------------------------------------------------------------------- */
    /*  isUnmodifiable / isSynchronized checks                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testIsUnmodifiable_PositiveAndNegative() {
        // The class returned by CollectionsWrappers is the marker for unmodifiable collections.
        Class<?> unmodClass = CollectionsWrappers.getUnmodifiableCollectionClass();

        assertTrue(CollectionUtilities.isUnmodifiable(unmodClass));

        // A normal mutable list class should NOT be considered unmodifiable.
        assertFalse(CollectionUtilities.isUnmodifiable(ArrayList.class));
    }

    @Test(expected = NullPointerException.class)
    public void testIsUnmodifiable_NullArgument() {
        CollectionUtilities.isUnmodifiable(null);
    }

    @Test
    public void testIsSynchronized_PositiveAndNegative() {
        Class<?> syncClass = CollectionsWrappers.getSynchronizedCollectionClass();

        assertTrue(CollectionUtilities.isSynchronized(syncClass));
        assertFalse(CollectionUtilities.isSynchronized(LinkedHashSet.class));
    }

    @Test(expected = NullPointerException.class)
    public void testIsSynchronized_NullArgument() {
        CollectionUtilities.isSynchronized(null);
    }

    /* --------------------------------------------------------------------- */
    /*  getUnmodifiableCollection                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetUnmodifiableCollection_List() {
        Collection<String> result = CollectionUtilities.getUnmodifiableCollection(mutableList);
        assertTrue(result instanceof List);
        assertTrue(result instanceof java.util.Collections.UnmodifiableList
                || result.getClass().getName().contains("Unmodifiable"));
        assertEquals(mutableList, result);

        // modification must fail
        try {
            ((List<String>) result).add("new");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) { }
    }

    @Test
    public void testGetUnmodifiableCollection_NavigableSet() {
        Collection<String> result = CollectionUtilities.getUnmodifiableCollection(mutableNavigableSet);
        assertTrue(result instanceof NavigableSet);
        assertTrue(result.getClass().getName().contains("Unmodifiable"));
        assertEquals(mutableNavigableSet, result);

        try {
            ((NavigableSet<String>) result).add("new");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) { }
    }

    @Test
    public void testGetUnmodifiableCollection_OtherCollection() {
        Collection<Object> result = CollectionUtilities.getUnmodifiableCollection(mutableCollection);
        assertTrue(result.getClass().getName().contains("Unmodifiable"));
        assertEquals(mutableCollection, result);
    }

    @Test(expected = NullPointerException.class)
    public void testGetUnmodifiableCollection_Null() {
        CollectionUtilities.getUnmodifiableCollection(null);
    }

    /* --------------------------------------------------------------------- */
    /*  getEmptyCollection                                                   */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetEmptyCollection_List() {
        Collection<String> empty = CollectionUtilities.getEmptyCollection(mutableList);
        assertTrue(empty instanceof List);
        assertTrue(empty.isEmpty());

        // ensure immutability
        try {
            ((List<String>) empty).add("x");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) { }
    }

    @Test
    public void testGetEmptyCollection_NavigableSet() {
        Collection<String> empty = CollectionUtilities.getEmptyCollection(mutableNavigableSet);
        assertTrue(empty instanceof NavigableSet);
        assertTrue(empty.isEmpty());
    }

    @Test
    public void testGetEmptyCollection_SortedSet() {
        Collection<Double> empty = CollectionUtilities.getEmptyCollection(mutableSortedSet);
        assertTrue(empty instanceof SortedSet);
        assertTrue(empty.isEmpty());
    }

    @Test
    public void testGetEmptyCollection_Set() {
        Collection<Integer> empty = CollectionUtilities.getEmptyCollection(mutableSet);
        assertTrue(empty instanceof Set);
        assertTrue(empty.isEmpty());
    }

    @Test(expected = NullPointerException.class)
    public void testGetEmptyCollection_Null() {
        CollectionUtilities.getEmptyCollection(null);
    }

    /* --------------------------------------------------------------------- */
    /*  getCheckedCollection                                                  */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetCheckedCollection_List() {
        List<Object> source = new ArrayList<>(Arrays.asList("one", "two"));
        Collection<String> checked = CollectionUtilities.getCheckedCollection(source, String.class);
        assertTrue(checked instanceof List);
        checked.add("three"); // allowed

        try {
            // This cast compiles because the collection is raw, but runtime should fail
            ((Collection)checked).add(42);
            fail("Expected ClassCastException");
        } catch (ClassCastException ignored) { }
    }

    @Test
    public void testGetCheckedCollection_Set() {
        Set<Object> source = new HashSet<>(Arrays.asList(1, 2));
        Collection<Integer> checked = CollectionUtilities.getCheckedCollection(source, Integer.class);
        checked.add(3);
        try {
            ((Collection)checked).add("bad");
            fail("Expected ClassCastException");
        } catch (ClassCastException ignored) { }
    }

    @Test
    public void testGetCheckedCollection_NavigableSet() {
        NavigableSet<Object> source = new TreeSet<>(Arrays.asList("a", "b"));
        Collection<String> checked = CollectionUtilities.getCheckedCollection(source, String.class);
        checked.add("c");
        try {
            ((Collection)checked).add(10);
            fail("Expected ClassCastException");
        } catch (ClassCastException ignored) { }
    }

    @Test
    public void testGetCheckedCollection_SortedSet() {
        SortedSet<Object> source = new TreeSet<>(Arrays.asList(1.0, 2.0));
        Collection<Double> checked = CollectionUtilities.getCheckedCollection(source, Double.class);
        checked.add(3.0);
        try {
            ((Collection)checked).add("nope");
            fail("Expected ClassCastException");
        } catch (ClassCastException ignored) { }
    }

    @Test
    public void testGetCheckedCollection_GenericCollection() {
        Collection<Object> source = new ArrayList<>(Arrays.asList("x"));
        Collection<String> checked = CollectionUtilities.getCheckedCollection(source, String.class);
        checked.add("y");
        try {
            ((Collection)checked).add(99);
            fail("Expected ClassCastException");
        } catch (ClassCastException ignored) { }
    }

    @Test(expected = NullPointerException.class)
    public void testGetCheckedCollection_NullCollection() {
        CollectionUtilities.getCheckedCollection(null, String.class);
    }

    @Test(expected = NullPointerException.class)
    public void testGetCheckedCollection_NullClass() {
        CollectionUtilities.getCheckedCollection(mutableList, null);
    }

    /* --------------------------------------------------------------------- */
    /*  getSynchronizedCollection                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetSynchronizedCollection_List() {
        Collection<String> sync = CollectionUtilities.getSynchronizedCollection(mutableList);
        assertTrue(sync instanceof List);
        sync.add("new");
        assertTrue(mutableList.contains("new"));
    }

    @Test
    public void testGetSynchronizedCollection_NavigableSet() {
        Collection<String> sync = CollectionUtilities.getSynchronizedCollection(mutableNavigableSet);
        assertTrue(sync instanceof NavigableSet);
        sync.add("new");
        assertTrue(mutableNavigableSet.contains("new"));
    }

    @Test
    public void testGetSynchronizedCollection_Other() {
        Collection<Object> sync = CollectionUtilities.getSynchronizedCollection(mutableCollection);
        sync.add(new Object());
        assertEquals(mutableCollection.size() + 1, sync.size());
    }

    @Test(expected = NullPointerException.class)
    public void testGetSynchronizedCollection_Null() {
        CollectionUtilities.getSynchronizedCollection(null);
    }
}
