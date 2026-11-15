package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.util.*;
import org.junit.Test;

/**
 * JUnit 4 tests for {@link CollectionUtilities}.
 */
public class CollectionUtilitiesTest {

    /* -------------------- Null‑safe checks -------------------- */

    @Test
    public void testIsEmpty() {
        assertTrue(CollectionUtilities.isEmpty(null));

        Collection<String> empty = new ArrayList<>();
        assertTrue(CollectionUtilities.isEmpty(empty));

        empty.add("x");
        assertFalse(CollectionUtilities.isEmpty(empty));
    }

    @Test
    public void testHasContent() {
        assertFalse(CollectionUtilities.hasContent(null));

        Collection<Integer> empty = new HashSet<>();
        assertFalse(CollectionUtilities.hasContent(empty));

        empty.add(1);
        assertTrue(CollectionUtilities.hasContent(empty));
    }

    @Test
    public void testSize() {
        assertEquals(0, CollectionUtilities.size(null));

        Collection<String> coll = new LinkedList<>();
        assertEquals(0, CollectionUtilities.size(coll));

        coll.add("a");
        coll.add("b");
        assertEquals(2, CollectionUtilities.size(coll));
    }

    /* -------------------- Immutable collection factories -------------------- */

    @Test
    public void testListOfImmutable() {
        List<String> list = CollectionUtilities.listOf("a", "b", "c");
        assertEquals(3, list.size());
        assertEquals("a", list.get(0));
        assertEquals("c", list.get(2));

        try {
            list.add("d");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
        }
    }

    @Test
    public void testListOfEmptyWhenNoElements() {
        List<String> list = CollectionUtilities.listOf();
        assertTrue(list.isEmpty());

        try {
            list.add("x");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
        }
    }

    @Test
    public void testListOfEmptyWhenNullArray() {
        List<String> list = CollectionUtilities.listOf((String[]) null);
        assertTrue(list.isEmpty());

        try {
            list.add("y");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
        }
    }

    @Test
    public void testSetOfImmutable() {
        Set<Integer> set = CollectionUtilities.setOf(1, 2, 3);
        assertEquals(3, set.size());
        assertTrue(set.containsAll(Arrays.asList(1, 2, 3)));

        try {
            set.add(4);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
        }
    }

    @Test
    public void testSetOfEmptyWhenNoElements() {
        Set<Integer> set = CollectionUtilities.setOf();
        assertTrue(set.isEmpty());

        try {
            set.add(1);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
        }
    }

    @Test
    public void testSetOfEmptyWhenNullArray() {
        Set<Integer> set = CollectionUtilities.setOf((Integer[]) null);
        assertTrue(set.isEmpty());

        try {
            set.add(2);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
        }
    }

    /* -------------------- isUnmodifiable / isSynchronized -------------------- */

    @Test
    public void testIsUnmodifiable() {
        Class<?> unmodList = CollectionsWrappers.getUnmodifiableListClass();
        assertTrue(CollectionUtilities.isUnmodifiable(unmodList));

        assertFalse(CollectionUtilities.isUnmodifiable(ArrayList.class));
    }

    @Test(expected = NullPointerException.class)
    public void testIsUnmodifiableNull() {
        CollectionUtilities.isUnmodifiable(null);
    }

    @Test
    public void testIsSynchronized() {
        Class<?> syncSet = CollectionsWrappers.getSynchronizedSetClass();
        assertTrue(CollectionUtilities.isSynchronized(syncSet));

        assertFalse(CollectionUtilities.isSynchronized(HashSet.class));
    }

    @Test(expected = NullPointerException.class)
    public void testIsSynchronizedNull() {
        CollectionUtilities.isSynchronized(null);
    }

    /* -------------------- getUnmodifiableCollection -------------------- */

    @Test
    public void testGetUnmodifiableCollection_List() {
        List<String> mutable = new ArrayList<>(Arrays.asList("a", "b"));
        Collection<String> unmod = CollectionUtilities.getUnmodifiableCollection(mutable);
        assertTrue(unmod instanceof List);
        assertEquals(2, unmod.size());

        try {
            unmod.add("c");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
        }
    }

    @Test
    public void testGetUnmodifiableCollection_Set() {
        Set<Integer> mutable = new HashSet<>(Arrays.asList(1, 2));
        Collection<Integer> unmod = CollectionUtilities.getUnmodifiableCollection(mutable);
        assertTrue(unmod instanceof Set);
        assertEquals(2, unmod.size());

        try {
            unmod.remove(1);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
        }
    }

    @Test
    public void testGetUnmodifiableCollection_NavigableSet() {
        NavigableSet<String> mutable = new TreeSet<>(Arrays.asList("x", "y"));
        Collection<String> unmod = CollectionUtilities.getUnmodifiableCollection(mutable);
        assertTrue(unmod instanceof NavigableSet);
        assertEquals(2, unmod.size());

        try {
            ((NavigableSet<String>) unmod).add("z");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
        }
    }

    @Test
    public void testGetUnmodifiableCollection_GenericCollection() {
        Collection<Double> mutable = new LinkedHashSet<>(Arrays.asList(1.1, 2.2));
        Collection<Double> unmod = CollectionUtilities.getUnmodifiableCollection(mutable);
        assertFalse(unmod instanceof List);
        assertFalse(unmod instanceof Set);
        assertEquals(2, unmod.size());

        try {
            unmod.clear();
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
        }
    }

    @Test(expected = NullPointerException.class)
    public void testGetUnmodifiableCollection_Null() {
        CollectionUtilities.getUnmodifiableCollection(null);
    }

    /* -------------------- getEmptyCollection -------------------- */

    @Test
    public void testGetEmptyCollection_List() {
        List<String> list = new ArrayList<>();
        Collection<String> empty = CollectionUtilities.getEmptyCollection(list);
        assertTrue(empty instanceof List);
        assertTrue(empty.isEmpty());

        try {
            empty.add("x");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
        }
    }

    @Test
    public void testGetEmptyCollection_Set() {
        Set<Integer> set = new HashSet<>();
        Collection<Integer> empty = CollectionUtilities.getEmptyCollection(set);
        assertTrue(empty instanceof Set);
        assertTrue(empty.isEmpty());

        try {
            empty.remove(0);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
        }
    }

    @Test
    public void testGetEmptyCollection_NavigableSet() {
        NavigableSet<String> nav = new TreeSet<>();
        Collection<String> empty = CollectionUtilities.getEmptyCollection(nav);
        assertTrue(empty instanceof NavigableSet);
        assertTrue(empty.isEmpty());

        try {
            ((NavigableSet<String>) empty).pollFirst();
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
        }
    }

    @Test
    public void testGetEmptyCollection_GenericCollection() {
        Collection<Long> coll = new LinkedList<>();
        Collection<Long> empty = CollectionUtilities.getEmptyCollection(coll);
        assertTrue(empty instanceof List); // fallback is emptyList()
        assertTrue(empty.isEmpty());

        try {
            empty.add(10L);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
        }
    }

    @Test(expected = NullPointerException.class)
    public void testGetEmptyCollection_Null() {
        CollectionUtilities.getEmptyCollection(null);
    }

    /* -------------------- getCheckedCollection -------------------- */

    @Test
    public void testGetCheckedCollection_List() {
        List<Object> mutable = new ArrayList<>(Arrays.asList("a", "b"));
        Collection<String> checked = CollectionUtilities.getCheckedCollection(mutable, String.class);
        assertTrue(checked.add("c"));
        assertEquals(3, checked.size());

        // bypass compile‑time generics to provoke ClassCastException
        try {
            ((Collection) checked).add(123);
            fail("Expected ClassCastException");
        } catch (ClassCastException expected) {
        }
    }

    @Test
    public void testGetCheckedCollection_Set() {
        Set<Object> mutable = new HashSet<>(Arrays.asList("x"));
        Collection<String> checked = CollectionUtilities.getCheckedCollection(mutable, String.class);
        assertTrue(checked.add("y"));
        assertEquals(2, checked.size());

        try {
            ((Collection) checked).add(Boolean.TRUE);
            fail("Expected ClassCastException");
        } catch (ClassCastException expected) {
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

    /* -------------------- getSynchronizedCollection -------------------- */

    @Test
    public void testGetSynchronizedCollection_List() {
        List<String> original = new ArrayList<>(Arrays.asList("one"));
        Collection<String> sync = CollectionUtilities.getSynchronizedCollection(original);
        assertTrue(sync instanceof List);
        assertNotSame(original, sync);

        sync.add("two");
        assertEquals(2, original.size());
        assertTrue(original.contains("two"));
    }

    @Test
    public void testGetSynchronizedCollection_Set() {
        Set<Integer> original = new HashSet<>(Arrays.asList(1));
        Collection<Integer> sync = CollectionUtilities.getSynchronizedCollection(original);
        assertTrue(sync instanceof Set);
        assertNotSame(original, sync);

        sync.remove(1);
        assertFalse(original.contains(1));
    }

    @Test
    public void testGetSynchronizedCollection_NavigableSet() {
        NavigableSet<String> original = new TreeSet<>(Arrays.asList("a"));
        Collection<String> sync = CollectionUtilities.getSynchronizedCollection(original);
        assertTrue(sync instanceof NavigableSet);
        assertNotSame(original, sync);

        sync.add("b");
        assertTrue(original.contains("b"));
    }

    @Test(expected = NullPointerException.class)
    public void testGetSynchronizedCollection_Null() {
        CollectionUtilities.getSynchronizedCollection(null);
    }
}
