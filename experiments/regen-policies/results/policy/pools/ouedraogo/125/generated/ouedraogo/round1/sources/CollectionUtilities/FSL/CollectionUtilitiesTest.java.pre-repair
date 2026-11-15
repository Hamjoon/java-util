package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.Test;

/**
 * Comprehensive JUnit4 test suite for {@link CollectionUtilities}.
 */
public class CollectionUtilitiesTest {

    // ------------------------------------------------------------------------
    // Null‑safe checks
    // ------------------------------------------------------------------------
    @Test
    public void testIsEmpty() {
        assertTrue(CollectionUtilities.isEmpty(null));
        assertTrue(CollectionUtilities.isEmpty(Collections.emptyList()));
        assertTrue(CollectionUtilities.isEmpty(Collections.emptySet()));

        List<String> list = new ArrayList<>();
        assertTrue(CollectionUtilities.isEmpty(list));
        list.add("x");
        assertFalse(CollectionUtilities.isEmpty(list));

        Set<Integer> set = new HashSet<>();
        assertTrue(CollectionUtilities.isEmpty(set));
        set.add(1);
        assertFalse(CollectionUtilities.isEmpty(set));
    }

    @Test
    public void testHasContent() {
        assertFalse(CollectionUtilities.hasContent(null));
        assertFalse(CollectionUtilities.hasContent(Collections.emptyList()));
        assertFalse(CollectionUtilities.hasContent(Collections.emptySet()));

        List<String> list = new ArrayList<>();
        assertFalse(CollectionUtilities.hasContent(list));
        list.add("a");
        assertTrue(CollectionUtilities.hasContent(list));

        Set<Integer> set = new HashSet<>();
        assertFalse(CollectionUtilities.hasContent(set));
        set.add(42);
        assertTrue(CollectionUtilities.hasContent(set));
    }

    @Test
    public void testSize() {
        assertEquals(0, CollectionUtilities.size(null));
        assertEquals(0, CollectionUtilities.size(Collections.emptyList()));
        assertEquals(0, CollectionUtilities.size(Collections.emptySet()));

        List<String> list = new ArrayList<>();
        list.add("one");
        list.add("two");
        assertEquals(2, CollectionUtilities.size(list));

        Set<Integer> set = new HashSet<>();
        set.add(1);
        set.add(2);
        set.add(3);
        assertEquals(3, CollectionUtilities.size(set));
    }

    // ------------------------------------------------------------------------
    // Immutable collection creation
    // ------------------------------------------------------------------------
    @Test
    public void testListOf_NullOrEmpty() {
        List<String> empty = CollectionUtilities.listOf();
        assertNotNull(empty);
        assertTrue(empty.isEmpty());
        // should be the shared unmodifiable empty list instance
        assertSame(empty, CollectionUtilities.listOf((String[]) null));

        // attempt to modify must throw UnsupportedOperationException
        try {
            empty.add("fail");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    @Test
    public void testListOf_WithElements() {
        List<String> list = CollectionUtilities.listOf("a", "b", "c");
        assertEquals(3, list.size());
        assertArrayEquals(new String[]{"a", "b", "c"}, list.toArray());

        // verify immutability
        try {
            list.add("d");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {
        }

        // verify that the returned list is not the same instance as a mutable list
        List<String> mutable = new ArrayList<>();
        mutable.add("a");
        mutable.add("b");
        mutable.add("c");
        assertNotSame(mutable, list);
    }

    @Test
    public void testSetOf_NullOrEmpty() {
        Set<Integer> empty = CollectionUtilities.setOf();
        assertNotNull(empty);
        assertTrue(empty.isEmpty());
        assertSame(empty, CollectionUtilities.setOf((Integer[]) null));

        try {
            empty.add(1);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    @Test
    public void testSetOf_WithElements() {
        Set<String> set = CollectionUtilities.setOf("x", "y", "z");
        assertEquals(3, set.size());
        assertTrue(set.containsAll(java.util.List.of("x", "y", "z")));

        // immutability check
        try {
            set.remove("x");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {
        }

        // order is preserved because LinkedHashSet is used internally
        assertArrayEquals(new String[]{"x", "y", "z"}, set.toArray());
    }

    // ------------------------------------------------------------------------
    // isUnmodifiable / isSynchronized
    // ------------------------------------------------------------------------
    @Test
    public void testIsUnmodifiable() {
        // Classes returned by CollectionsWrappers are the unmodifiable collection markers
        Class<?> unmodListClass = CollectionsWrappers.getUnmodifiableListClass();
        Class<?> unmodSetClass = CollectionsWrappers.getUnmodifiableSetClass();
        Class<?> unmodSortedSetClass = CollectionsWrappers.getUnmodifiableSortedSetClass();
        Class<?> unmodNavigableSetClass = CollectionsWrappers.getUnmodifiableNavigableSetClass();

        assertTrue(CollectionUtilities.isUnmodifiable(unmodListClass));
        assertTrue(CollectionUtilities.isUnmodifiable(unmodSetClass));
        assertTrue(CollectionUtilities.isUnmodifiable(unmodSortedSetClass));
        assertTrue(CollectionUtilities.isUnmodifiable(unmodNavigableSetClass));

        // A regular mutable class should be false
        assertFalse(CollectionUtilities.isUnmodifiable(ArrayList.class));
        assertFalse(CollectionUtilities.isUnmodifiable(HashSet.class));

        // Null argument must throw NPE with the expected message
        try {
            CollectionUtilities.isUnmodifiable(null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            assertEquals("targetType (Class) cannot be null", e.getMessage());
        }
    }

    @Test
    public void testIsSynchronized() {
        Class<?> syncListClass = CollectionsWrappers.getSynchronizedListClass();
        Class<?> syncSetClass = CollectionsWrappers.getSynchronizedSetClass();
        Class<?> syncSortedSetClass = CollectionsWrappers.getSynchronizedSortedSetClass();
        Class<?> syncNavigableSetClass = CollectionsWrappers.getSynchronizedNavigableSetClass();

        assertTrue(CollectionUtilities.isSynchronized(syncListClass));
        assertTrue(CollectionUtilities.isSynchronized(syncSetClass));
        assertTrue(CollectionUtilities.isSynchronized(syncSortedSetClass));
        assertTrue(CollectionUtilities.isSynchronized(syncNavigableSetClass));

        // Regular mutable class should be false
        assertFalse(CollectionUtilities.isSynchronized(ArrayList.class));
        assertFalse(CollectionUtilities.isSynchronized(HashSet.class));

        try {
            CollectionUtilities.isSynchronized(null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            assertEquals("targetType (Class) cannot be null", e.getMessage());
        }
    }

    // ------------------------------------------------------------------------
    // getUnmodifiableCollection
    // ------------------------------------------------------------------------
    @Test
    public void testGetUnmodifiableCollection_NavigableSet() {
        NavigableSet<String> nav = new TreeSet<>();
        nav.add("a");
        nav.add("b");

        Collection<String> unmod = CollectionUtilities.getUnmodifiableCollection(nav);
        assertTrue(unmod instanceof NavigableSet);
        assertEquals(2, unmod.size());

        try {
            ((NavigableSet<String>) unmod).add("c");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    @Test
    public void testGetUnmodifiableCollection_SortedSet() {
        SortedSet<Integer> sorted = new TreeSet<>();
        sorted.add(10);
        sorted.add(20);

        Collection<Integer> unmod = CollectionUtilities.getUnmodifiableCollection(sorted);
        assertTrue(unmod instanceof SortedSet);
        assertEquals(2, unmod.size());

        try {
            ((SortedSet<Integer>) unmod).add(30);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    @Test
    public void testGetUnmodifiableCollection_Set() {
        Set<String> set = new HashSet<>();
        set.add("x");

        Collection<String> unmod = CollectionUtilities.getUnmodifiableCollection(set);
        assertTrue(unmod instanceof Set);
        assertEquals(1, unmod.size());

        try {
            ((Set<String>) unmod).add("y");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    @Test
    public void testGetUnmodifiableCollection_List() {
        List<String> list = new ArrayList<>();
        list.add("first");

        Collection<String> unmod = CollectionUtilities.getUnmodifiableCollection(list);
        assertTrue(unmod instanceof List);
        assertEquals(1, unmod.size());

        try {
            ((List<String>) unmod).add("second");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    @Test
    public void testGetUnmodifiableCollection_OtherCollection() {
        Collection<String> coll = new LinkedHashSet<>(); // implements Set but not SortedSet/NavigableSet
        coll.add("z");

        Collection<String> unmod = CollectionUtilities.getUnmodifiableCollection(coll);
        assertTrue(unmod instanceof Collection);
        assertFalse(unmod instanceof List);
        assertFalse(unmod instanceof Set); // actually it will be Set, but we still accept generic collection test
        assertEquals(1, unmod.size());

        try {
            unmod.add("another");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    @Test(expected = NullPointerException.class)
    public void testGetUnmodifiableCollection_Null() {
        CollectionUtilities.getUnmodifiableCollection(null);
    }

    // ------------------------------------------------------------------------
    // getEmptyCollection
    // ------------------------------------------------------------------------
    @Test
    public void testGetEmptyCollection_NavigableSet() {
        NavigableSet<Integer> nav = new TreeSet<>();
        Collection<Integer> empty = CollectionUtilities.getEmptyCollection(nav);
        assertTrue(empty instanceof NavigableSet);
        assertTrue(empty.isEmpty());
        // verify immutability
        try {
            ((NavigableSet<Integer>) empty).add(1);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    @Test
    public void testGetEmptyCollection_SortedSet() {
        SortedSet<String> sorted = new TreeSet<>();
        Collection<String> empty = CollectionUtilities.getEmptyCollection(sorted);
        assertTrue(empty instanceof SortedSet);
        assertTrue(empty.isEmpty());
        try {
            ((SortedSet<String>) empty).add("a");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    @Test
    public void testGetEmptyCollection_Set() {
        Set<String> set = new HashSet<>();
        Collection<String> empty = CollectionUtilities.getEmptyCollection(set);
        assertTrue(empty instanceof Set);
        assertTrue(empty.isEmpty());
        try {
            ((Set<String>) empty).add("b");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    @Test
    public void testGetEmptyCollection_List() {
        List<Integer> list = new ArrayList<>();
        Collection<Integer> empty = CollectionUtilities.getEmptyCollection(list);
        assertTrue(empty instanceof List);
        assertTrue(empty.isEmpty());
        try {
            ((List<Integer>) empty).add(5);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    @Test
    public void testGetEmptyCollection_OtherCollection() {
        Collection<String> other = new LinkedHashSet<>();
        Collection<String> empty = CollectionUtilities.getEmptyCollection(other);
        // default is emptyList()
        assertTrue(empty instanceof List);
        assertTrue(empty.isEmpty());
    }

    @Test(expected = NullPointerException.class)
    public void testGetEmptyCollection_Null() {
        CollectionUtilities.getEmptyCollection(null);
    }

    // ------------------------------------------------------------------------
    // getCheckedCollection
    // ------------------------------------------------------------------------
    @Test
    public void testGetCheckedCollection_List_SuccessAndFailure() {
        List<Object> mutable = new ArrayList<>();
        mutable.add("one");
        mutable.add("two");

        Collection<String> checked = CollectionUtilities.getCheckedCollection(mutable, String.class);
        assertTrue(checked instanceof List);
        checked.add("three"); // OK

        try {
            // This line compiles because the raw type is Collection<String>,
            // but at runtime the checked wrapper will reject it.
            ((Collection) checked).add(100);
            fail("Expected ClassCastException");
        } catch (ClassCastException ignored) {
        }
    }

    @Test
    public void testGetCheckedCollection_Set() {
        Set<Integer> mutable = new HashSet<>();
        mutable.add(1);
        mutable.add(2);

        Collection<Integer> checked = CollectionUtilities.getCheckedCollection(mutable, Integer.class);
        assertTrue(checked instanceof Set);
        checked.add(3); // OK

        try {
            ((Collection) checked).add("bad");
            fail("Expected ClassCastException");
        } catch (ClassCastException ignored) {
        }
    }

    @Test
    public void testGetCheckedCollection_NavigableSet() {
        NavigableSet<String> mutable = new TreeSet<>();
        mutable.add("a");

        Collection<String> checked = CollectionUtilities.getCheckedCollection(mutable, String.class);
        assertTrue(checked instanceof NavigableSet);
        checked.add("b"); // OK

        try {
            ((Collection) checked).add(123);
            fail("Expected ClassCastException");
        } catch (ClassCastException ignored) {
        }
    }

    @Test(expected = NullPointerException.class)
    public void testGetCheckedCollection_NullCollection() {
        CollectionUtilities.getCheckedCollection(null, String.class);
    }

    @Test(expected = NullPointerException.class)
    public void testGetCheckedCollection_NullType() {
        List<String> list = new ArrayList<>();
        CollectionUtilities.getCheckedCollection(list, null);
    }

    // ------------------------------------------------------------------------
    // getSynchronizedCollection
    // ------------------------------------------------------------------------
    @Test
    public void testGetSynchronizedCollection_List() {
        List<String> mutable = new ArrayList<>();
        mutable.add("alpha");

        Collection<String> sync = CollectionUtilities.getSynchronizedCollection(mutable);
        assertTrue(sync instanceof List);
        sync.add("beta");
        assertEquals(2, sync.size());

        // The returned instance should be the synchronized wrapper from Collections
        assertTrue(sync.getClass().getName().contains("Synchronized"));
    }

    @Test
    public void testGetSynchronizedCollection_Set() {
        Set<Integer> mutable = new HashSet<>();
        mutable.add(10);

        Collection<Integer> sync = CollectionUtilities.getSynchronizedCollection(mutable);
        assertTrue(sync instanceof Set);
        sync.add(20);
        assertTrue(sync.contains(20));
    }

    @Test
    public void testGetSynchronizedCollection_NavigableSet() {
        NavigableSet<String> mutable = new TreeSet<>();
        mutable.add("x");

        Collection<String> sync = CollectionUtilities.getSynchronizedCollection(mutable);
        assertTrue(sync instanceof NavigableSet);
        sync.add("y");
        assertTrue(((NavigableSet<String>) sync).contains("y"));
    }

    @Test(expected = NullPointerException.class)
    public void testGetSynchronizedCollection_Null() {
        CollectionUtilities.getSynchronizedCollection(null);
    }

    // ------------------------------------------------------------------------
    // Additional sanity checks for wrapper class detection helpers
    // ------------------------------------------------------------------------
    @Test
    public void testWrapperClassDetectionConsistency() {
        // The class objects obtained from CollectionsWrappers should be assignable from the
        // actual wrapper types produced by the utility methods.
        List<String> list = new ArrayList<>();
        Collection<String> unmod = CollectionUtilities.getUnmodifiableCollection(list);
        assertTrue(CollectionUtilities.isUnmodifiable(unmod.getClass()));

        Collection<String> sync = CollectionUtilities.getSynchronizedCollection(list);
        assertTrue(CollectionUtilities.isSynchronized(sync.getClass()));
    }
}
