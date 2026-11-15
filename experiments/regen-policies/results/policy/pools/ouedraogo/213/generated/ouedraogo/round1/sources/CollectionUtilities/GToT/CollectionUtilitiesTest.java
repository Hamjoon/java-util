package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link CollectionUtilities}.
 *
 * The tests are organised in the order the public methods appear in the source.
 * For each method we provide:
 *   • a basic “happy‑path” test,
 *   • tests for edge‑cases (null, empty, wrong type, etc.),
 *   • tests that verify the documented exceptions are thrown.
 *
 * No external libraries or mocking frameworks are used – only the
 * classes and factory methods that appear in the supplied source code.
 */
public class CollectionUtilitiesTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /* --------------------------------------------------------------------- */
    /* 1. isEmpty(Collection<?>)                                              */
    /* --------------------------------------------------------------------- */
    @Test
    public void testIsEmpty_NullCollection() {
        assertTrue(CollectionUtilities.isEmpty(null));
    }

    @Test
    public void testIsEmpty_EmptyCollection() {
        assertTrue(CollectionUtilities.isEmpty(new ArrayList<>()));
    }

    @Test
    public void testIsEmpty_NonEmptyCollection() {
        List<String> list = Arrays.asList("a");
        assertFalse(CollectionUtilities.isEmpty(list));
    }

    /* --------------------------------------------------------------------- */
    /* 2. hasContent(Collection<?>)                                           */
    /* --------------------------------------------------------------------- */
    @Test
    public void testHasContent_NullCollection() {
        assertFalse(CollectionUtilities.hasContent(null));
    }

    @Test
    public void testHasContent_EmptyCollection() {
        assertFalse(CollectionUtilities.hasContent(new HashSet<>()));
    }

    @Test
    public void testHasContent_NonEmptyCollection() {
        Set<Integer> set = new HashSet<>(Arrays.asList(1, 2));
        assertTrue(CollectionUtilities.hasContent(set));
    }

    /* --------------------------------------------------------------------- */
    /* 3. size(Collection<?>)                                                 */
    /* --------------------------------------------------------------------- */
    @Test
    public void testSize_NullCollection() {
        assertEquals(0, CollectionUtilities.size(null));
    }

    @Test
    public void testSize_EmptyCollection() {
        assertEquals(0, CollectionUtilities.size(Collections.emptyList()));
    }

    @Test
    public void testSize_NonEmptyCollection() {
        List<String> list = Arrays.asList("x", "y", "z");
        assertEquals(3, CollectionUtilities.size(list));
    }

    /* --------------------------------------------------------------------- */
    /* 4. listOf(T... items)                                                  */
    /* --------------------------------------------------------------------- */
    @Test
    public void testListOf_NullArray() {
        List<Object> result = CollectionUtilities.listOf((Object[]) null);
        assertTrue(result.isEmpty());
        // verify immutability
        thrown.expect(UnsupportedOperationException.class);
        result.add(new Object());
    }

    @Test
    public void testListOf_EmptyArray() {
        List<String> result = CollectionUtilities.listOf();
        assertTrue(result.isEmpty());
        thrown.expect(UnsupportedOperationException.class);
        result.add("any");
    }

    @Test
    public void testListOf_ValidElements() {
        List<String> result = CollectionUtilities.listOf("a", "b", "c");
        assertEquals(Arrays.asList("a", "b", "c"), result);
        // immutability check
        thrown.expect(UnsupportedOperationException.class);
        result.remove(0);
    }

    @Test
    public void testListOf_NullElementThrowsNPE() {
        thrown.expect(NullPointerException.class);
        CollectionUtilities.listOf("good", null, "bad");
    }

    /* --------------------------------------------------------------------- */
    /* 5. setOf(T... items)                                                   */
    /* --------------------------------------------------------------------- */
    @Test
    public void testSetOf_NullArray() {
        Set<Object> result = CollectionUtilities.setOf((Object[]) null);
        assertTrue(result.isEmpty());
        thrown.expect(UnsupportedOperationException.class);
        result.add(new Object());
    }

    @Test
    public void testSetOf_EmptyArray() {
        Set<String> result = CollectionUtilities.setOf();
        assertTrue(result.isEmpty());
        thrown.expect(UnsupportedOperationException.class);
        result.add("x");
    }

    @Test
    public void testSetOf_ValidElements() {
        Set<String> result = CollectionUtilities.setOf("x", "y", "z");
        // order is preserved because LinkedHashSet is used internally
        assertEquals(new LinkedHashSet<>(Arrays.asList("x", "y", "z")), result);
        thrown.expect(UnsupportedOperationException.class);
        result.clear();
    }

    @Test
    public void testSetOf_NullElementThrowsNPE() {
        thrown.expect(NullPointerException.class);
        CollectionUtilities.setOf("ok", null);
    }

    /* --------------------------------------------------------------------- */
    /* 6. isUnmodifiable(Class<?>)                                            */
    /* --------------------------------------------------------------------- */
    @Test
    public void testIsUnmodifiable_WithUnmodifiableClass() {
        Class<?> unmodClass = CollectionsWrappers.getUnmodifiableCollectionClass();
        assertTrue(CollectionUtilities.isUnmodifiable(unmodClass));
    }

    @Test
    public void testIsUnmodifiable_WithMutableClass() {
        assertFalse(CollectionUtilities.isUnmodifiable(ArrayList.class));
    }

    @Test(expected = NullPointerException.class)
    public void testIsUnmodifiable_NullArgument() {
        CollectionUtilities.isUnmodifiable(null);
    }

    /* --------------------------------------------------------------------- */
    /* 7. isSynchronized(Class<?>)                                            */
    /* --------------------------------------------------------------------- */
    @Test
    public void testIsSynchronized_WithSynchronizedClass() {
        Class<?> syncClass = CollectionsWrappers.getSynchronizedCollectionClass();
        assertTrue(CollectionUtilities.isSynchronized(syncClass));
    }

    @Test
    public void testIsSynchronized_WithNonSynchronizedClass() {
        assertFalse(CollectionUtilities.isSynchronized(HashSet.class));
    }

    @Test(expected = NullPointerException.class)
    public void testIsSynchronized_NullArgument() {
        CollectionUtilities.isSynchronized(null);
    }

    /* --------------------------------------------------------------------- */
    /* 8. getUnmodifiableCollection(Collection<T>)                            */
    /* --------------------------------------------------------------------- */
    @Test(expected = NullPointerException.class)
    public void testGetUnmodifiableCollection_Null() {
        CollectionUtilities.getUnmodifiableCollection(null);
    }

    @Test
    public void testGetUnmodifiableCollection_NavigableSet() {
        NavigableSet<String> nav = new TreeSet<>(Arrays.asList("a", "b"));
        Collection<String> unmod = CollectionUtilities.getUnmodifiableCollection(nav);
        assertTrue(unmod instanceof NavigableSet);
        thrown.expect(UnsupportedOperationException.class);
        ((NavigableSet<String>) unmod).add("c");
    }

    @Test
    public void testGetUnmodifiableCollection_SortedSet() {
        SortedSet<Integer> sorted = new TreeSet<>(Arrays.asList(1, 2, 3));
        Collection<Integer> unmod = CollectionUtilities.getUnmodifiableCollection(sorted);
        assertTrue(unmod instanceof SortedSet);
        thrown.expect(UnsupportedOperationException.class);
        ((SortedSet<Integer>) unmod).add(4);
    }

    @Test
    public void testGetUnmodifiableCollection_Set() {
        Set<String> set = new HashSet<>(Arrays.asList("x"));
        Collection<String> unmod = CollectionUtilities.getUnmodifiableCollection(set);
        assertTrue(unmod instanceof Set);
        thrown.expect(UnsupportedOperationException.class);
        ((Set<String>) unmod).add("y");
    }

    @Test
    public void testGetUnmodifiableCollection_List() {
        List<String> list = new ArrayList<>(Arrays.asList("one"));
        Collection<String> unmod = CollectionUtilities.getUnmodifiableCollection(list);
        assertTrue(unmod instanceof List);
        thrown.expect(UnsupportedOperationException.class);
        ((List<String>) unmod).add("two");
    }

    @Test
    public void testGetUnmodifiableCollection_OtherCollection() {
        // ArrayDeque implements Collection but not List/Set/NavigableSet/SortedSet
        Collection<Integer> deque = new ArrayDeque<>(Arrays.asList(5, 6));
        Collection<Integer> unmod = CollectionUtilities.getUnmodifiableCollection(deque);
        assertFalse(unmod instanceof List);
        assertFalse(unmod instanceof Set);
        thrown.expect(UnsupportedOperationException.class);
        unmod.add(7);
    }

    /* --------------------------------------------------------------------- */
    /* 9. getEmptyCollection(Collection<T>)                                   */
    /* --------------------------------------------------------------------- */
    @Test(expected = NullPointerException.class)
    public void testGetEmptyCollection_Null() {
        CollectionUtilities.getEmptyCollection(null);
    }

    @Test
    public void testGetEmptyCollection_NavigableSet() {
        NavigableSet<String> nav = new TreeSet<>(Arrays.asList("a"));
        Collection<String> empty = CollectionUtilities.getEmptyCollection(nav);
        assertTrue(empty instanceof NavigableSet);
        assertTrue(empty.isEmpty());
    }

    @Test
    public void testGetEmptyCollection_SortedSet() {
        SortedSet<Integer> sorted = new TreeSet<>(Arrays.asList(1));
        Collection<Integer> empty = CollectionUtilities.getEmptyCollection(sorted);
        assertTrue(empty instanceof SortedSet);
        assertTrue(empty.isEmpty());
    }

    @Test
    public void testGetEmptyCollection_Set() {
        Set<String> set = new HashSet<>(Arrays.asList("x"));
        Collection<String> empty = CollectionUtilities.getEmptyCollection(set);
        assertTrue(empty instanceof Set);
        assertTrue(empty.isEmpty());
    }

    @Test
    public void testGetEmptyCollection_List() {
        List<String> list = new ArrayList<>(Arrays.asList("y"));
        Collection<String> empty = CollectionUtilities.getEmptyCollection(list);
        assertTrue(empty instanceof List);
        assertTrue(empty.isEmpty());
    }

    @Test
    public void testGetEmptyCollection_OtherCollection() {
        Collection<Integer> deque = new ArrayDeque<>(Arrays.asList(1, 2));
        Collection<Integer> empty = CollectionUtilities.getEmptyCollection(deque);
        // Default is emptyList()
        assertTrue(empty instanceof List);
        assertTrue(empty.isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /* 10. getCheckedCollection(T collection, Class<E> type)                 */
    /* --------------------------------------------------------------------- */
    @Test(expected = NullPointerException.class)
    public void testGetCheckedCollection_NullCollection() {
        CollectionUtilities.getCheckedCollection(null, String.class);
    }

    @Test(expected = NullPointerException.class)
    public void testGetCheckedCollection_NullType() {
        CollectionUtilities.getCheckedCollection(new ArrayList<>(), null);
    }

    @Test
    public void testGetCheckedCollection_List() {
        List<Object> raw = new ArrayList<>(Arrays.asList("a", "b"));
        Collection<String> checked = CollectionUtilities.getCheckedCollection(raw, String.class);
        // Adding a correct type succeeds
        checked.add("c");
        assertEquals(3, checked.size());

        // Adding a wrong type throws ClassCastException
        try {
            checked.add((String) (Object) Integer.valueOf(1));
            fail("Expected ClassCastException");
        } catch (ClassCastException expected) {
            // expected
        }
    }

    @Test
    public void testGetCheckedCollection_Set() {
        Set<Number> raw = new HashSet<>(Arrays.asList(1, 2.5));
        Collection<Integer> checked = CollectionUtilities.getCheckedCollection(raw, Integer.class);
        // Adding Integer works
        checked.add(3);
        assertTrue(checked.contains(3));

        // Adding Double should fail
        try {
            checked.add((Integer) (Object) Double.valueOf(4.2));
            fail("Expected ClassCastException");
        } catch (ClassCastException expected) {
            // expected
        }
    }

    @Test
    public void testGetCheckedCollection_NavigableSet() {
        NavigableSet<String> nav = new TreeSet<>(Arrays.asList("a"));
        Collection<String> checked = CollectionUtilities.getCheckedCollection(nav, String.class);
        checked.add("b");
        assertTrue(checked.contains("b"));
    }

    @Test
    public void testGetCheckedCollection_OtherCollection() {
        Collection<Object> raw = new ArrayDeque<>(Arrays.asList("x"));
        Collection<String> checked = CollectionUtilities.getCheckedCollection(raw, String.class);
        checked.add("y");
        try {
            checked.add((String) (Object) Integer.valueOf(5));
            fail("Expected ClassCastException");
        } catch (ClassCastException expected) {
            // expected
        }
    }

    /* --------------------------------------------------------------------- */
    /* 11. getSynchronizedCollection(Collection<T>)                           */
    /* --------------------------------------------------------------------- */
    @Test(expected = NullPointerException.class)
    public void testGetSynchronizedCollection_Null() {
        CollectionUtilities.getSynchronizedCollection(null);
    }

    @Test
    public void testGetSynchronizedCollection_NavigableSet() {
        NavigableSet<String> nav = new TreeSet<>(Arrays.asList("a"));
        Collection<String> sync = CollectionUtilities.getSynchronizedCollection(nav);
        assertTrue(sync instanceof NavigableSet);
        // modifications are allowed (wrapper is synchronized)
        sync.add("b");
        assertTrue(sync.contains("b"));
    }

    @Test
    public void testGetSynchronizedCollection_SortedSet() {
        SortedSet<Integer> sorted = new TreeSet<>(Arrays.asList(1));
        Collection<Integer> sync = CollectionUtilities.getSynchronizedCollection(sorted);
        assertTrue(sync instanceof SortedSet);
        sync.add(2);
        assertTrue(sync.contains(2));
    }

    @Test
    public void testGetSynchronizedCollection_Set() {
        Set<String> set = new HashSet<>(Arrays.asList("x"));
        Collection<String> sync = CollectionUtilities.getSynchronizedCollection(set);
        assertTrue(sync instanceof Set);
        sync.add("y");
        assertTrue(sync.contains("y"));
    }

    @Test
    public void testGetSynchronizedCollection_List() {
        List<String> list = new ArrayList<>(Arrays.asList("first"));
        Collection<String> sync = CollectionUtilities.getSynchronizedCollection(list);
        assertTrue(sync instanceof List);
        sync.add("second");
        assertEquals(2, sync.size());
    }

    @Test
    public void testGetSynchronizedCollection_OtherCollection() {
        Collection<Integer> deque = new ArrayDeque<>(Arrays.asList(10));
        Collection<Integer> sync = CollectionUtilities.getSynchronizedCollection(deque);
        // Default wrapper is synchronizedCollection (still a Collection)
        sync.add(20);
        assertTrue(sync.contains(20));
    }
}
