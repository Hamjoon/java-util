package com.cedarsoftware.util;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.*;

public class CollectionUtilitiesTest {

    /* ----------------------------------------------------------------------
     * isEmpty()
     * ---------------------------------------------------------------------- */
    @Test
    public void testIsEmpty_Null() {
        assertTrue(CollectionUtilities.isEmpty(null));
    }

    @Test
    public void testIsEmpty_EmptyCollection() {
        Collection<?> empty = new ArrayList<>();
        assertTrue(CollectionUtilities.isEmpty(empty));
    }

    @Test
    public void testIsEmpty_NonEmptyCollection() {
        Collection<Integer> col = new ArrayList<>(Arrays.asList(1, 2, 3));
        assertFalse(CollectionUtilities.isEmpty(col));
    }

    /* ----------------------------------------------------------------------
     * hasContent()
     * ---------------------------------------------------------------------- */
    @Test
    public void testHasContent_Null() {
        assertFalse(CollectionUtilities.hasContent(null));
    }

    @Test
    public void testHasContent_EmptyCollection() {
        Collection<String> empty = new LinkedHashSet<>();
        assertFalse(CollectionUtilities.hasContent(empty));
    }

    @Test
    public void testHasContent_NonEmptyCollection() {
        Collection<String> col = new LinkedHashSet<>(Arrays.asList("a"));
        assertTrue(CollectionUtilities.hasContent(col));
    }

    /* ----------------------------------------------------------------------
     * size()
     * ---------------------------------------------------------------------- */
    @Test
    public void testSize_Null() {
        assertEquals(0, CollectionUtilities.size(null));
    }

    @Test
    public void testSize_EmptyCollection() {
        assertEquals(0, CollectionUtilities.size(Collections.emptyList()));
    }

    @Test
    public void testSize_NonEmptyCollection() {
        List<Integer> list = Arrays.asList(5, 6, 7, 8);
        assertEquals(4, CollectionUtilities.size(list));
    }

    /* ----------------------------------------------------------------------
     * listOf()
     * ---------------------------------------------------------------------- */
    @Test
    public void testListOf_NullArray() {
        List<?> list = CollectionUtilities.listOf((Object[]) null);
        assertTrue(list.isEmpty());
        // should be the same immutable empty instance
        assertSame(list, CollectionUtilities.listOf());
    }

    @Test
    public void testListOf_NoArgs() {
        List<?> list = CollectionUtilities.listOf();
        assertTrue(list.isEmpty());
        assertSame(list, CollectionUtilities.listOf((Object[]) null));
    }

    @Test
    public void testListOf_SingleElement() {
        List<String> list = CollectionUtilities.listOf("single");
        assertEquals(1, list.size());
        assertEquals("single", list.get(0));
    }

    @Test
    public void testListOf_MultipleElements() {
        List<Integer> list = CollectionUtilities.listOf(1, 2, 3);
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void testListOf_ContainsNullElement() {
        List<String> list = CollectionUtilities.listOf("first", null, "last");
        assertEquals(3, list.size());
        assertNull(list.get(1));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testListOf_Unmodifiable() {
        List<Integer> list = CollectionUtilities.listOf(10, 20);
        list.add(30); // should throw
    }

    /* ----------------------------------------------------------------------
     * setOf()
     * ---------------------------------------------------------------------- */
    @Test
    public void testSetOf_NullArray() {
        Set<?> set = CollectionUtilities.setOf((Object[]) null);
        assertTrue(set.isEmpty());
        // same immutable empty instance as a no‑arg call
        assertSame(set, CollectionUtilities.setOf());
    }

    @Test
    public void testSetOf_NoArgs() {
        Set<?> set = CollectionUtilities.setOf();
        assertTrue(set.isEmpty());
        assertSame(set, CollectionUtilities.setOf((Object[]) null));
    }

    @Test
    public void testSetOf_SingleElement() {
        Set<String> set = CollectionUtilities.setOf("only");
        assertEquals(1, set.size());
        assertTrue(set.contains("only"));
    }

    @Test
    public void testSetOf_MultipleElements() {
        Set<String> set = CollectionUtilities.setOf("a", "b", "c");
        assertEquals(new LinkedHashSet<>(Arrays.asList("a", "b", "c")), set);
    }

    @Test
    public void testSetOf_DuplicatesRemoved() {
        Set<Integer> set = CollectionUtilities.setOf(1, 2, 2, 3, 1);
        assertEquals(3, set.size());
        assertTrue(set.containsAll(Arrays.asList(1, 2, 3)));
    }

    @Test
    public void testSetOf_ContainsNullElement() {
        Set<String> set = CollectionUtilities.setOf("x", null, "y");
        assertEquals(3, set.size());
        assertTrue(set.contains(null));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSetOf_Unmodifiable() {
        Set<String> set = CollectionUtilities.setOf("one", "two");
        set.add("three"); // should throw
    }
}
