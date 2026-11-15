package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.*;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class CollectionUtilitiesTest {

    /* ----------------------------------------------------------------------
     * Tests for isEmpty(Collection)
     * ---------------------------------------------------------------------- */

    @Test
    public void testIsEmpty_NullCollection() {
        assertTrue("null collection should be considered empty", CollectionUtilities.isEmpty(null));
    }

    @Test
    public void testIsEmpty_EmptyCollection() {
        Collection<String> empty = new ArrayList<>();
        assertTrue("empty collection should be considered empty", CollectionUtilities.isEmpty(empty));
    }

    @Test
    public void testIsEmpty_NonEmptyCollection() {
        Collection<Integer> list = Arrays.asList(1, 2, 3);
        assertFalse("non‑empty collection should not be considered empty", CollectionUtilities.isEmpty(list));
    }

    /* ----------------------------------------------------------------------
     * Tests for hasContent(Collection)
     * ---------------------------------------------------------------------- */

    @Test
    public void testHasContent_NullCollection() {
        assertFalse("null collection has no content", CollectionUtilities.hasContent(null));
    }

    @Test
    public void testHasContent_EmptyCollection() {
        Collection<Object> empty = new LinkedHashSet<>();
        assertFalse("empty collection has no content", CollectionUtilities.hasContent(empty));
    }

    @Test
    public void testHasContent_NonEmptyCollection() {
        Collection<String> set = new HashSet<>(Arrays.asList("a"));
        assertTrue("collection with an element has content", CollectionUtilities.hasContent(set));
    }

    /* ----------------------------------------------------------------------
     * Tests for size(Collection)
     * ---------------------------------------------------------------------- */

    @Test
    public void testSize_NullCollection() {
        assertEquals("null collection size should be 0", 0, CollectionUtilities.size(null));
    }

    @Test
    public void testSize_EmptyCollection() {
        Collection<Double> empty = Collections.emptyList();
        assertEquals("empty collection size should be 0", 0, CollectionUtilities.size(empty));
    }

    @Test
    public void testSize_NonEmptyCollection() {
        Collection<Character> coll = new ArrayList<>(Arrays.asList('x', 'y'));
        assertEquals("size should match number of elements", 2, CollectionUtilities.size(coll));
    }

    /* ----------------------------------------------------------------------
     * Tests for listOf(T...)
     * ---------------------------------------------------------------------- */

    @Test
    public void testListOf_NullArray() {
        List<Object> result = CollectionUtilities.listOf((Object[]) null);
        assertNotNull("result must never be null", result);
        assertTrue("null varargs should produce an empty list", result.isEmpty());
        // Verify that the returned list is the same unmodifiable instance used internally
        List<?> another = CollectionUtilities.listOf();
        assertSame("empty list instances should be identical", another, result);
    }

    @Test
    public void testListOf_EmptyArray() {
        List<String> result = CollectionUtilities.listOf();
        assertTrue("empty varargs should produce an empty list", result.isEmpty());
    }

    @Test
    public void testListOf_SingleElement() {
        List<String> result = CollectionUtilities.listOf("one");
        assertEquals("list should contain exactly one element", 1, result.size());
        assertEquals("element should be the one supplied", "one", result.get(0));
    }

    @Test
    public void testListOf_MultipleElements() {
        List<Integer> result = CollectionUtilities.listOf(1, 2, 3);
        assertEquals("list should contain three elements", 3, result.size());
        assertEquals(Integer.valueOf(1), result.get(0));
        assertEquals(Integer.valueOf(2), result.get(1));
        assertEquals(Integer.valueOf(3), result.get(2));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testListOf_Unmodifiable() {
        List<String> result = CollectionUtilities.listOf("a", "b");
        // Attempt to modify should throw UnsupportedOperationException
        result.add("c");
    }

    /* ----------------------------------------------------------------------
     * Tests for setOf(T...)
     * ---------------------------------------------------------------------- */

    @Test
    public void testSetOf_NullArray() {
        Set<Object> result = CollectionUtilities.setOf((Object[]) null);
        assertNotNull("result must never be null", result);
        assertTrue("null varargs should produce an empty set", result.isEmpty());
        // Verify that the returned set is the same unmodifiable instance used internally
        Set<?> another = CollectionUtilities.setOf();
        assertSame("empty set instances should be identical", another, result);
    }

    @Test
    public void testSetOf_EmptyArray() {
        Set<String> result = CollectionUtilities.setOf();
        assertTrue("empty varargs should produce an empty set", result.isEmpty());
    }

    @Test
    public void testSetOf_SingleElement() {
        Set<String> result = CollectionUtilities.setOf("single");
        assertEquals("set should contain exactly one element", 1, result.size());
        assertTrue("set must contain the supplied element", result.contains("single"));
    }

    @Test
    public void testSetOf_MultipleElementsWithDuplicates() {
        Set<Integer> result = CollectionUtilities.setOf(1, 2, 2, 3, 1);
        // Duplicates must be eliminated, order preserved by LinkedHashSet
        List<Integer> expectedOrder = Arrays.asList(1, 2, 3);
        assertEquals("set should contain three distinct elements", 3, result.size());
        assertEquals("iteration order must follow first occurrence", expectedOrder, new ArrayList<>(result));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSetOf_Unmodifiable() {
        Set<String> result = CollectionUtilities.setOf("x", "y");
        // Attempt to modify should throw UnsupportedOperationException
        result.add("z");
    }
}
