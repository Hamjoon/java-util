package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.*;

public class CollectionUtilitiesTest {

    // -------------------------------------------------------------------------
    // 1. isEmpty
    // -------------------------------------------------------------------------
    @Test
    public void testIsEmpty_Null() {
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

    // -------------------------------------------------------------------------
    // 2. hasContent
    // -------------------------------------------------------------------------
    @Test
    public void testHasContent_Null() {
        assertFalse(CollectionUtilities.hasContent(null));
    }

    @Test
    public void testHasContent_Empty() {
        assertFalse(CollectionUtilities.hasContent(new HashSet<>()));
    }

    @Test
    public void testHasContent_NonEmpty() {
        Set<Integer> set = new HashSet<>(Arrays.asList(1, 2));
        assertTrue(CollectionUtilities.hasContent(set));
    }

    // -------------------------------------------------------------------------
    // 3. size
    // -------------------------------------------------------------------------
    @Test
    public void testSize_Null() {
        assertEquals(0, CollectionUtilities.size(null));
    }

    @Test
    public void testSize_Empty() {
        assertEquals(0, CollectionUtilities.size(Collections.emptyList()));
    }

    @Test
    public void testSize_NonEmpty() {
        List<Integer> list = Arrays.asList(1, 2, 3);
        assertEquals(3, CollectionUtilities.size(list));
    }

    // -------------------------------------------------------------------------
    // 4. listOf
    // -------------------------------------------------------------------------
    @Test
    public void testListOf_NullArray() {
        List<String> result = CollectionUtilities.listOf((String[]) null);
        assertSame(CollectionUtilities.listOf(), result); // both empty unmodifiable lists
        assertTrue(result.isEmpty());
        assertTrue(isUnmodifiableList(result));
    }

    @Test
    public void testListOf_EmptyArray() {
        List<String> result = CollectionUtilities.listOf();
        assertTrue(result.isEmpty());
        assertTrue(isUnmodifiableList(result));
    }

    @Test
    public void testListOf_NormalElements() {
        List<String> result = CollectionUtilities.listOf("A", "B", "C");
        assertEquals(Arrays.asList("A", "B", "C"), result);
        assertTrue(isUnmodifiableList(result));
    }

    @Test(expected = NullPointerException.class)
    public void testListOf_NullElement() {
        CollectionUtilities.listOf("good", null, "bad");
    }

    // -------------------------------------------------------------------------
    // 5. setOf
    // -------------------------------------------------------------------------
    @Test
    public void testSetOf_NullArray() {
        Set<String> result = CollectionUtilities.setOf((String[]) null);
        assertTrue(result.isEmpty());
        assertTrue(isUnmodifiableSet(result));
    }

    @Test
    public void testSetOf_EmptyArray() {
        Set<String> result = CollectionUtilities.setOf();
        assertTrue(result.isEmpty());
        assertTrue(isUnmodifiableSet(result));
    }

    @Test
    public void testSetOf_NormalElements() {
        Set<String> result = CollectionUtilities.setOf("X", "Y", "Z");
        // LinkedHashSet preserves insertion order
        assertEquals(new LinkedHashSet<>(Arrays.asList("X", "Y", "Z")), result);
        assertTrue(isUnmodifiableSet(result));
    }

    @Test
    public void testSetOf_DuplicateElements() {
        Set<String> result = CollectionUtilities.setOf("dup", "dup", "unique");
        assertEquals(2, result.size());
        assertTrue(result.containsAll(Arrays.asList("dup", "unique")));
    }

    @Test(expected = NullPointerException.class)
    public void testSetOf_NullElement() {
        CollectionUtilities.setOf("ok", null);
    }

    // -------------------------------------------------------------------------
    // 6. isUnmodifiable
    // -------------------------------------------------------------------------
    @Test(expected = NullPointerException.class)
    public void testIsUnmodifiable_NullArgument() {
        CollectionUtilities.isUnmodifiable(null);
    }

    @Test
    public void testIsUnmodifiable_TrueForInternalClass() {
        Class<?> unmodClass = CollectionsWrappers.getUnmodifiableCollectionClass();
        assertTrue(CollectionUtilities.isUnmodifiable(unmodClass));
    }

    @Test
    public void testIsUnmodifiable_FalseForRegularClass() {
        assertFalse(CollectionUtilities.isUnmodifiable(ArrayList.class));
    }

    // -------------------------------------------------------------------------
    // 7. isSynchronized
    // -------------------------------------------------------------------------
    @Test(expected = NullPointerException.class)
    public void testIsSynchronized_NullArgument() {
        CollectionUtilities.isSynchronized(null);
    }

    @Test
    public void testIsSynchronized_TrueForInternalClass() {
        Class<?> syncClass = CollectionsWrappers.getSynchronizedCollectionClass();
        assertTrue(CollectionUtilities.isSynchronized(syncClass));
    }

    @Test
    public void testIsSynchronized_FalseForRegularClass() {
        assertFalse(CollectionUtilities.isSynchronized(HashSet.class));
    }

    // -------------------------------------------------------------------------
    // 8. getUnmodifiableCollection
    // -------------------------------------------------------------------------
    @Test(expected = NullPointerException.class)
    public void testGetUnmodifiableCollection_Null() {
        CollectionUtilities.getUnmodifiableCollection(null);
    }

    @Test
    public void testGetUnmodifiableCollection_List() {
        List<Integer> src = new ArrayList<>(Arrays.asList(1, 2));
        Collection<Integer> unmod = CollectionUtilities.getUnmodifiableCollection(src);
        assertTrue(unmod instanceof List);
        assertTrue(isUnmodifiableList(unmod));
        // Mutating must throw
        try {
            ((List<Integer>) unmod).add(3);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) { }
    }

    @Test
    public void testGetUnmodifiableCollection_Set() {
        Set<String> src = new HashSet<>(Arrays.asList("a"));
        Collection<String> unmod = CollectionUtilities.getUnmodifiableCollection(src);
        assertTrue(unmod instanceof Set);
        assertTrue(isUnmodifiableSet(unmod));
        try {
            ((Set<String>) unmod).add("b");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) { }
    }

    @Test
    public void testGetUnmodifiableCollection_NavigableSet() {
        NavigableSet<Integer> src = new java.util.TreeSet<>(Arrays.asList(5, 1, 3));
        Collection<Integer> unmod = CollectionUtilities.getUnmodifiableCollection(src);
        assertTrue(unmod instanceof NavigableSet);
        assertTrue(isUnmodifiableNavigableSet(unmod));
        try {
            ((NavigableSet<Integer>) unmod).add(7);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) { }
    }

    // -------------------------------------------------------------------------
    // 9. getEmptyCollection
    // -------------------------------------------------------------------------
    @Test(expected = NullPointerException.class)
    public void testGetEmptyCollection_Null() {
        CollectionUtilities.getEmptyCollection(null);
    }

    @Test
    public void testGetEmptyCollection_List() {
        List<String> src = new ArrayList<>();
        Collection<String> empty = CollectionUtilities.getEmptyCollection(src);
        assertTrue(empty instanceof List);
        assertTrue(empty.isEmpty());
        assertTrue(isUnmodifiableList(empty));
    }

    @Test
    public void testGetEmptyCollection_Set() {
        Set<Integer> src = new HashSet<>();
        Collection<Integer> empty = CollectionUtilities.getEmptyCollection(src);
        assertTrue(empty instanceof Set);
        assertTrue(empty.isEmpty());
        assertTrue(isUnmodifiableSet(empty));
    }

    @Test
    public void testGetEmptyCollection_NavigableSet() {
        NavigableSet<String> src = new java.util.TreeSet<>();
        Collection<String> empty = CollectionUtilities.getEmptyCollection(src);
        assertTrue(empty instanceof NavigableSet);
        assertTrue(empty.isEmpty());
        // The specific empty type is the singleton provided by Collections
        assertSame(Collections.emptyNavigableSet(), empty);
    }

    // -------------------------------------------------------------------------
    // 10. getCheckedCollection
    // -------------------------------------------------------------------------
    @Test(expected = NullPointerException.class)
    public void testGetCheckedCollection_NullCollection() {
        CollectionUtilities.getCheckedCollection(null, String.class);
    }

    @Test(expected = NullPointerException.class)
    public void testGetCheckedCollection_NullType() {
        List<String> src = new ArrayList<>();
        CollectionUtilities.getCheckedCollection(src, null);
    }

    @Test
    public void testGetCheckedCollection_List_CorrectType() {
        List<Object> src = new ArrayList<>();
        Collection<String> checked = CollectionUtilities.getCheckedCollection(src, String.class);
        checked.add("hello");
        assertEquals(1, checked.size());
        assertTrue(checked.contains("hello"));
    }

    @Test(expected = ClassCastException.class)
    public void testGetCheckedCollection_List_WrongType() {
        List<Object> src = new ArrayList<>();
        Collection<String> checked = CollectionUtilities.getCheckedCollection(src, String.class);
        // This should throw
        ((Collection) checked).add(42);
    }

    @Test
    public void testGetCheckedCollection_Set_CorrectType() {
        Set<Object> src = new HashSet<>();
        Collection<Integer> checked = CollectionUtilities.getCheckedCollection(src, Integer.class);
        checked.add(10);
        assertTrue(checked.contains(10));
    }

    @Test(expected = ClassCastException.class)
    public void testGetCheckedCollection_Set_WrongType() {
        Set<Object> src = new HashSet<>();
        Collection<Integer> checked = CollectionUtilities.getCheckedCollection(src, Integer.class);
        ((Collection) checked).add("string");
    }

    // -------------------------------------------------------------------------
    // 11. getSynchronizedCollection
    // -------------------------------------------------------------------------
    @Test(expected = NullPointerException.class)
    public void testGetSynchronizedCollection_Null() {
        CollectionUtilities.getSynchronizedCollection(null);
    }

    @Test
    public void testGetSynchronizedCollection_List() {
        List<String> src = new ArrayList<>(Arrays.asList("a", "b"));
        Collection<String> sync = CollectionUtilities.getSynchronizedCollection(src);
        assertTrue(sync instanceof List);
        // The returned class name contains "Synchronized"
        assertTrue(sync.getClass().getName().contains("Synchronized"));
        // Modifications are allowed when performed inside a synchronized block
        synchronized (sync) {
            ((List<String>) sync).add("c");
        }
        assertEquals(3, sync.size());
    }

    @Test
    public void testGetSynchronizedCollection_Set() {
        Set<Integer> src = new HashSet<>(Arrays.asList(1, 2));
        Collection<Integer> sync = CollectionUtilities.getSynchronizedCollection(src);
        assertTrue(sync instanceof Set);
        assertTrue(sync.getClass().getName().contains("Synchronized"));
        synchronized (sync) {
            ((Set<Integer>) sync).add(3);
        }
        assertTrue(sync.contains(3));
    }

    // -------------------------------------------------------------------------
    // Helper methods to detect specific wrapper types without relying on internal
    // implementation details of java.util.Collections (the class names are stable
    // across JDKs).
    // -------------------------------------------------------------------------
    private static boolean isUnmodifiableList(Collection<?> c) {
        return c.getClass().getName().contains("Unmodifiable") && c instanceof List;
    }

    private static boolean isUnmodifiableSet(Collection<?> c) {
        return c.getClass().getName().contains("Unmodifiable") && c instanceof Set;
    }

    private static boolean isUnmodifiableNavigableSet(Collection<?> c) {
        return c.getClass().getName().contains("Unmodifiable") && c instanceof NavigableSet;
    }
}
