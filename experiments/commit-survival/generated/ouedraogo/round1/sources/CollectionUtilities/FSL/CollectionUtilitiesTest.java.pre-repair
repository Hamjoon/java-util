package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.Test;

/**
 * Unit tests for {@link CollectionUtilities}.
 */
public class CollectionUtilitiesTest {

    /*-------------------------------------------------------
     *  isEmpty()
     *-------------------------------------------------------*/

    @Test
    public void testIsEmpty_Null() {
        assertTrue(CollectionUtilities.isEmpty(null));
    }

    @Test
    public void testIsEmpty_EmptyCollection() {
        assertTrue(CollectionUtilities.isEmpty(new ArrayList<>()));
        assertTrue(CollectionUtilities.isEmpty(new HashSet<>()));
    }

    @Test
    public void testIsEmpty_NonEmptyCollection() {
        List<String> list = new ArrayList<>();
        list.add("one");
        assertFalse(CollectionUtilities.isEmpty(list));

        Set<Integer> set = new HashSet<>();
        set.add(42);
        assertFalse(CollectionUtilities.isEmpty(set));
    }

    /*-------------------------------------------------------
     *  hasContent()
     *-------------------------------------------------------*/

    @Test
    public void testHasContent_Null() {
        assertFalse(CollectionUtilities.hasContent(null));
    }

    @Test
    public void testHasContent_EmptyCollection() {
        assertFalse(CollectionUtilities.hasContent(Collections.emptyList()));
        assertFalse(CollectionUtilities.hasContent(Collections.emptySet()));
    }

    @Test
    public void testHasContent_NonEmptyCollection() {
        List<String> list = Arrays.asList("a", "b");
        assertTrue(CollectionUtilities.hasContent(list));

        Set<String> set = new LinkedHashSet<>();
        set.add("x");
        assertTrue(CollectionUtilities.hasContent(set));
    }

    /*-------------------------------------------------------
     *  size()
     *-------------------------------------------------------*/

    @Test
    public void testSize_Null() {
        assertEquals(0, CollectionUtilities.size(null));
    }

    @Test
    public void testSize_Empty() {
        assertEquals(0, CollectionUtilities.size(new ArrayList<>()));
        assertEquals(0, CollectionUtilities.size(new HashSet<>()));
    }

    @Test
    public void testSize_NonEmpty() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4);
        assertEquals(4, CollectionUtilities.size(list));

        Set<String> set = new LinkedHashSet<>(Arrays.asList("a", "b"));
        assertEquals(2, CollectionUtilities.size(set));
    }

    /*-------------------------------------------------------
     *  listOf()
     *-------------------------------------------------------*/

    @Test
    public void testListOf_NoArguments_ReturnsSameEmptyInstance() {
        List<Object> empty1 = CollectionUtilities.listOf();
        List<Object> empty2 = CollectionUtilities.listOf((Object[]) null);
        assertSame(empty1, empty2);
        assertTrue(empty1.isEmpty());
    }

    @Test
    public void testListOf_NullArray_ReturnsEmptyUnmodifiable() {
        List<String> empty = CollectionUtilities.listOf((String[]) null);
        assertTrue(empty.isEmpty());
        try {
            empty.add("should fail");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void testListOf_SingleElement() {
        List<String> list = CollectionUtilities.listOf("alpha");
        assertEquals(1, list.size());
        assertEquals("alpha", list.get(0));
    }

    @Test
    public void testListOf_MultipleElements_PreservesOrder() {
        List<String> list = CollectionUtilities.listOf("first", "second", "third");
        assertEquals(Arrays.asList("first", "second", "third"), list);
    }

    @Test
    public void testListOf_ContainsNullElement() {
        List<String> list = CollectionUtilities.listOf("a", null, "b");
        assertEquals(3, list.size());
        assertNull(list.get(1));
    }

    @Test
    public void testListOf_IsUnmodifiable() {
        List<Integer> list = CollectionUtilities.listOf(1, 2, 3);
        try {
            list.add(4);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            list.remove(0);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    /*-------------------------------------------------------
     *  setOf()
     *-------------------------------------------------------*/

    @Test
    public void testSetOf_NoArguments_ReturnsSameEmptyInstance() {
        Set<Object> empty1 = CollectionUtilities.setOf();
        Set<Object> empty2 = CollectionUtilities.setOf((Object[]) null);
        assertSame(empty1, empty2);
        assertTrue(empty1.isEmpty());
    }

    @Test
    public void testSetOf_NullArray_ReturnsEmptyUnmodifiable() {
        Set<String> empty = CollectionUtilities.setOf((String[]) null);
        assertTrue(empty.isEmpty());
        try {
            empty.add("should fail");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void testSetOf_SingleElement() {
        Set<String> set = CollectionUtilities.setOf("only");
        assertEquals(1, set.size());
        assertTrue(set.contains("only"));
    }

    @Test
    public void testSetOf_MultipleElements_PreservesInsertionOrder() {
        Set<String> set = CollectionUtilities.setOf("first", "second", "third");
        Iterator<String> it = set.iterator();
        assertEquals("first", it.next());
        assertEquals("second", it.next());
        assertEquals("third", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void testSetOf_DuplicateElements_AreRemoved() {
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

    @Test
    public void testSetOf_IsUnmodifiable() {
        Set<String> set = CollectionUtilities.setOf("a", "b");
        try {
            set.add("c");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            set.remove("a");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }
}
