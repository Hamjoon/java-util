package com.cedarsoftware.util;

import com.cedarsoftware.util.convert.CollectionsWrappers;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class CollectionUtilitiesTest {

    /* ----------------------------------------------------------------------
     *  Public method signatures extracted from CollectionUtilities
     * ---------------------------------------------------------------------- */
    // public static boolean isEmpty(Collection<?> col)
    // public static boolean hasContent(Collection<?> col)
    // public static int size(Collection<?> col)
    // public static <T> List<T> listOf(T... items)
    // public static <T> Set<T> setOf(T... items)
    // public static boolean isUnmodifiable(Class<?> targetType)
    // public static boolean isSynchronized(Class<?> targetType)
    // public static <T> Collection<T> getUnmodifiableCollection(Collection<T> collection)
    // public static <T> Collection<T> getEmptyCollection(Collection<T> collection)
    // public static <T extends Collection<?>, E> Collection<E> getCheckedCollection(T collection, Class<E> type)
    // public static <T> Collection<T> getSynchronizedCollection(Collection<T> collection)

    /* ----------------------------------------------------------------------
     *  Basic functionality tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testIsEmpty_NullAndEmpty() {
        assertTrue(CollectionUtilities.isEmpty(null));
        assertTrue(CollectionUtilities.isEmpty(Collections.emptyList()));
        assertTrue(CollectionUtilities.isEmpty(new HashSet<>()));
    }

    @Test
    public void testIsEmpty_NonEmpty() {
        List<String> list = Arrays.asList("a");
        assertFalse(CollectionUtilities.isEmpty(list));
    }

    @Test
    public void testHasContent_NullAndEmpty() {
        assertFalse(CollectionUtilities.hasContent(null));
        assertFalse(CollectionUtilities.hasContent(Collections.emptySet()));
    }

    @Test
    public void testHasContent_NonEmpty() {
        Set<Integer> set = new HashSet<>(Arrays.asList(1, 2, 3));
        assertTrue(CollectionUtilities.hasContent(set));
    }

    @Test
    public void testSize_Null() {
        assertEquals(0, CollectionUtilities.size(null));
    }

    @Test
    public void testSize_NonNull() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4);
        assertEquals(4, CollectionUtilities.size(list));
    }

    @Test
    public void testListOf_NullArrayReturnsEmpty() {
        List<String> list = CollectionUtilities.listOf((String[]) null);
        assertNotNull(list);
        assertTrue(list.isEmpty());
        // returned list must be unmodifiable
        try {
            list.add("x");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    @Test
    public void testListOf_EmptyArrayReturnsEmpty() {
        List<Integer> list = CollectionUtilities.listOf();
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testListOf_ValidElements() {
        List<String> list = CollectionUtilities.listOf("A", "B", "C");
        assertEquals(Arrays.asList("A", "B", "C"), list);
        // verify unmodifiable
        try {
            list.add("D");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    @Test
    public void testListOf_NullElementAllowed() {
        // According to Javadoc it should NPE, but implementation permits null.
        List<String> list = CollectionUtilities.listOf((String) null);
        assertEquals(1, list.size());
        assertNull(list.get(0));
    }

    @Test
    public void testSetOf_NullArrayReturnsEmpty() {
        Set<String> set = CollectionUtilities.setOf((String[]) null);
        assertNotNull(set);
        assertTrue(set.isEmpty());
        try {
            set.add("x");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    @Test
    public void testSetOf_EmptyArrayReturnsEmpty() {
        Set<Integer> set = CollectionUtilities.setOf();
        assertNotNull(set);
        assertTrue(set.isEmpty());
    }

    @Test
    public void testSetOf_ValidElementsAndDuplicates() {
        Set<String> set = CollectionUtilities.setOf("A", "B", "A", "C");
        // LinkedHashSet preserves insertion order, duplicates removed
        assertEquals(new LinkedHashSet<>(Arrays.asList("A", "B", "C")), set);
        try {
            set.add("D");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    @Test
    public void testSetOf_NullElementAllowed() {
        // Implementation permits null
        Set<String> set = CollectionUtilities.setOf((String) null);
        assertEquals(1, set.size());
        assertTrue(set.contains(null));
    }

    @Test
    public void testIsUnmodifiable_PositiveAndNegative() {
        Class<?> unmodClass = CollectionsWrappers.getUnmodifiableCollectionClass();
        assertTrue(CollectionUtilities.isUnmodifiable(unmodClass));

        // A regular mutable collection class should return false
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

        assertFalse(CollectionUtilities.isSynchronized(LinkedList.class));
    }

    @Test(expected = NullPointerException.class)
    public void testIsSynchronized_NullArgument() {
        CollectionUtilities.isSynchronized(null);
    }

    @Test(expected = NullPointerException.class)
    public void testGetUnmodifiableCollection_NullArgument() {
        CollectionUtilities.getUnmodifiableCollection(null);
    }

    @Test
    public void testGetUnmodifiableCollection_List() {
        List<String> mutable = new ArrayList<>(Arrays.asList("x", "y"));
        Collection<String> result = CollectionUtilities.getUnmodifiableCollection(mutable);
        assertTrue(result instanceof List);
        assertEquals(mutable, result);
        try {
            ((List<String>) result).add("z");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    @Test
    public void testGetUnmodifiableCollection_Set() {
        Set<Integer> mutable = new HashSet<>(Arrays.asList(1, 2));
        Collection<Integer> result = CollectionUtilities.getUnmodifiableCollection(mutable);
        assertTrue(result instanceof Set);
        try {
            ((Set<Integer>) result).add(3);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    @Test
    public void testGetUnmodifiableCollection_NavigableSet() {
        NavigableSet<String> mutable = new TreeSet<>(Arrays.asList("a", "b"));
        Collection<String> result = CollectionUtilities.getUnmodifiableCollection(mutable);
        assertTrue(result instanceof NavigableSet);
        try {
            ((NavigableSet<String>) result).add("c");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    @Test
    public void testGetUnmodifiableCollection_OtherCollection() {
        Queue<String> mutable = new ArrayDeque<>(Arrays.asList("q"));
        Collection<String> result = CollectionUtilities.getUnmodifiableCollection(mutable);
        assertTrue(result instanceof Collection);
        try {
            result.add("new");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    @Test(expected = NullPointerException.class)
    public void testGetEmptyCollection_NullArgument() {
        CollectionUtilities.getEmptyCollection(null);
    }

    @Test
    public void testGetEmptyCollection_List() {
        List<String> list = new ArrayList<>();
        Collection<String> empty = CollectionUtilities.getEmptyCollection(list);
        assertTrue(empty instanceof List);
        assertTrue(empty.isEmpty());
        try {
            empty.add("a");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    @Test
    public void testGetEmptyCollection_Set() {
        Set<Integer> set = new HashSet<>();
        Collection<Integer> empty = CollectionUtilities.getEmptyCollection(set);
        assertTrue(empty instanceof Set);
        assertTrue(empty.isEmpty());
    }

    @Test
    public void testGetEmptyCollection_NavigableSet() {
        NavigableSet<String> ns = new TreeSet<>();
        Collection<String> empty = CollectionUtilities.getEmptyCollection(ns);
        assertTrue(empty instanceof NavigableSet);
        assertTrue(empty.isEmpty());
    }

    @Test
    public void testGetEmptyCollection_SortedSet() {
        SortedSet<Double> ss = new TreeSet<>();
        Collection<Double> empty = CollectionUtilities.getEmptyCollection(ss);
        assertTrue(empty instanceof SortedSet);
        assertTrue(empty.isEmpty());
    }

    @Test
    public void testGetCheckedCollection_NullCollection() {
        try {
            CollectionUtilities.getCheckedCollection(null, String.class);
            fail("Expected NullPointerException");
        } catch (NullPointerException ignored) {
        }
    }

    @Test
    public void testGetCheckedCollection_NullType() {
        List<String> list = new ArrayList<>();
        try {
            CollectionUtilities.getCheckedCollection(list, null);
            fail("Expected NullPointerException");
        } catch (NullPointerException ignored) {
        }
    }

    @Test
    public void testGetCheckedCollection_List_CorrectType() {
        List<Object> raw = new ArrayList<>();
        raw.add("one");
        raw.add("two");
        Collection<String> checked = CollectionUtilities.getCheckedCollection(raw, String.class);
        // Adding a correct type works
        checked.add("three");
        assertEquals(3, checked.size());
    }

    @Test
    public void testGetCheckedCollection_List_WrongTypeThrows() {
        List<Object> raw = new ArrayList<>();
        raw.add("one");
        Collection<String> checked = CollectionUtilities.getCheckedCollection(raw, String.class);
        try {
            // This should cause ClassCastException at runtime
            ((Collection) checked).add(42);
            fail("Expected ClassCastException");
        } catch (ClassCastException ignored) {
        }
    }

    @Test
    public void testGetCheckedCollection_Set_CorrectAndWrongTypes() {
        Set<Object> raw = new HashSet<>();
        raw.add("a");
        Collection<String> checked = CollectionUtilities.getCheckedCollection(raw, String.class);
        checked.add("b");
        assertTrue(checked.contains("b"));
        try {
            ((Collection) checked).add(100);
            fail("Expected ClassCastException");
        } catch (ClassCastException ignored) {
        }
    }

    @Test
    public void testGetCheckedCollection_NavigableSet() {
        NavigableSet<Object> raw = new TreeSet<>();
        raw.add("alpha");
        Collection<String> checked = CollectionUtilities.getCheckedCollection(raw, String.class);
        checked.add("beta");
        try {
            ((Collection) checked).add(1);
            fail("Expected ClassCastException");
        } catch (ClassCastException ignored) {
        }
    }

    @Test(expected = NullPointerException.class)
    public void testGetSynchronizedCollection_NullArgument() {
        CollectionUtilities.getSynchronizedCollection(null);
    }

    @Test
    public void testGetSynchronizedCollection_List() {
        List<String> mutable = new ArrayList<>(Arrays.asList("x"));
        Collection<String> sync = CollectionUtilities.getSynchronizedCollection(mutable);
        // The class name of the wrapper contains "Synchronized"
        assertTrue(sync.getClass().getName().contains("Synchronized"));
        // Modifications should work
        sync.add("y");
        assertEquals(2, sync.size());
    }

    @Test
    public void testGetSynchronizedCollection_Set() {
        Set<Integer> mutable = new HashSet<>(Arrays.asList(1, 2));
        Collection<Integer> sync = CollectionUtilities.getSynchronizedCollection(mutable);
        assertTrue(sync.getClass().getName().contains("Synchronized"));
        sync.add(3);
        assertTrue(sync.contains(3));
    }

    @Test
    public void testGetSynchronizedCollection_NavigableSet() {
        NavigableSet<String> mutable = new TreeSet<>(Arrays.asList("a"));
        Collection<String> sync = CollectionUtilities.getSynchronizedCollection(mutable);
        assertTrue(sync.getClass().getName().contains("Synchronized"));
        sync.add("b");
        assertTrue(sync.contains("b"));
    }

    @Test
    public void testGetSynchronizedCollection_OtherCollection() {
        Queue<String> mutable = new ArrayDeque<>(Arrays.asList("q"));
        Collection<String> sync = CollectionUtilities.getSynchronizedCollection(mutable);
        assertTrue(sync.getClass().getName().contains("Synchronized"));
        sync.add("new");
        assertTrue(sync.contains("new"));
    }
}
