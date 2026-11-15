package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

public class CollectionUtilitiesTest {

    private Collection<String> nullCollection;
    private Collection<String> emptyCollection;
    private Collection<String> nonEmptyCollection;

    @Before
    public void setUp() {
        nullCollection = null;
        emptyCollection = new ArrayList<>();
        nonEmptyCollection = new ArrayList<>(Arrays.asList("a", "b", "c"));
    }

    /* -------------------------------------------------
     * Tests for isEmpty()
     * ------------------------------------------------- */
    @Test
    public void testIsEmpty_Null() {
        assertTrue("null collection should be considered empty", CollectionUtilities.isEmpty(nullCollection));
    }

    @Test
    public void testIsEmpty_Empty() {
        assertTrue("empty collection should be considered empty", CollectionUtilities.isEmpty(emptyCollection));
    }

    @Test
    public void testIsEmpty_NonEmpty() {
        assertFalse("non‑empty collection should not be considered empty", CollectionUtilities.isEmpty(nonEmptyCollection));
    }

    /* -------------------------------------------------
     * Tests for hasContent()
     * ------------------------------------------------- */
    @Test
    public void testHasContent_Null() {
        assertFalse("null collection should not have content", CollectionUtilities.hasContent(nullCollection));
    }

    @Test
    public void testHasContent_Empty() {
        assertFalse("empty collection should not have content", CollectionUtilities.hasContent(emptyCollection));
    }

    @Test
    public void testHasContent_NonEmpty() {
        assertTrue("non‑empty collection should have content", CollectionUtilities.hasContent(nonEmptyCollection));
    }

    /* -------------------------------------------------
     * Tests for size()
     * ------------------------------------------------- */
    @Test
    public void testSize_Null() {
        assertEquals("null collection size should be 0", 0, CollectionUtilities.size(nullCollection));
    }

    @Test
    public void testSize_Empty() {
        assertEquals("empty collection size should be 0", 0, CollectionUtilities.size(emptyCollection));
    }

    @Test
    public void testSize_NonEmpty() {
        assertEquals("non‑empty collection size should match actual size", 3, CollectionUtilities.size(nonEmptyCollection));
    }

    /* -------------------------------------------------
     * Tests for listOf()
     * ------------------------------------------------- */
    @Test
    public void testListOf_NullArgument() {
        List<Object> result = CollectionUtilities.listOf((Object[]) null);
        assertNotNull("Result should never be null", result);
        assertTrue("Result should be empty for null argument", result.isEmpty());
    }

    @Test
    public void testListOf_EmptyArray() {
        List<String> result = CollectionUtilities.listOf();
        assertNotNull("Result should never be null", result);
        assertTrue("Result should be empty for empty varargs", result.isEmpty());
    }

    @Test
    public void testListOf_SingleElement() {
        List<Integer> result = CollectionUtilities.listOf(42);
        assertEquals("List should contain exactly one element", 1, result.size());
        assertEquals("Element should be the one supplied", Integer.valueOf(42), result.get(0));
    }

    @Test
    public void testListOf_MultipleElements() {
        List<String> result = CollectionUtilities.listOf("alpha", "beta", "gamma");
        assertEquals("List should contain three elements", 3, result.size());
        assertEquals("First element should be 'alpha'", "alpha", result.get(0));
        assertEquals("Second element should be 'beta'", "beta", result.get(1));
        assertEquals("Third element should be 'gamma'", "gamma", result.get(2));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testListOf_Unmodifiable() {
        List<String> result = CollectionUtilities.listOf("x", "y");
        // Attempt to modify should throw UnsupportedOperationException
        result.add("z");
    }

    @Test
    public void testListOf_ContainsNullElement() {
        List<String> result = CollectionUtilities.listOf("first", null, "third");
        assertEquals("List size should reflect null element as a valid entry", 3, result.size());
        assertNull("Second element should be null", result.get(1));
    }

    /* -------------------------------------------------
     * Tests for setOf()
     * ------------------------------------------------- */
    @Test
    public void testSetOf_NullArgument() {
        Set<Object> result = CollectionUtilities.setOf((Object[]) null);
        assertNotNull("Result should never be null", result);
        assertTrue("Result should be empty for null argument", result.isEmpty());
    }

    @Test
    public void testSetOf_EmptyArray() {
        Set<String> result = CollectionUtilities.setOf();
        assertNotNull("Result should never be null", result);
        assertTrue("Result should be empty for empty varargs", result.isEmpty());
    }

    @Test
    public void testSetOf_SingleElement() {
        Set<Integer> result = CollectionUtilities.setOf(7);
        assertEquals("Set should contain exactly one element", 1, result.size());
        assertTrue("Set should contain the supplied element", result.contains(7));
    }

    @Test
    public void testSetOf_MultipleElements() {
        Set<String> result = CollectionUtilities.setOf("one", "two", "three");
        assertEquals("Set should contain three unique elements", 3, result.size());
        // Verify iteration order matches insertion order (LinkedHashSet behavior)
        List<String> ordered = new ArrayList<>(result);
        assertEquals("First element should be 'one'", "one", ordered.get(0));
        assertEquals("Second element should be 'two'", "two", ordered.get(1));
        assertEquals("Third element should be 'three'", "three", ordered.get(2));
    }

    @Test
    public void testSetOf_DuplicateElements() {
        Set<String> result = CollectionUtilities.setOf("dup", "dup", "unique");
        assertEquals("Duplicate values should be collapsed", 2, result.size());
        assertTrue("Set should contain 'dup'", result.contains("dup"));
        assertTrue("Set should contain 'unique'", result.contains("unique"));
        // Verify order: first occurrence of "dup" then "unique"
        List<String> ordered = new ArrayList<>(result);
        assertEquals("First element should be 'dup'", "dup", ordered.get(0));
        assertEquals("Second element should be 'unique'", "unique", ordered.get(1));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSetOf_Unmodifiable() {
        Set<String> result = CollectionUtilities.setOf("a", "b");
        // Attempt to modify should throw UnsupportedOperationException
        result.add("c");
    }

    @Test
    public void testSetOf_ContainsNullElement() {
        Set<String> result = CollectionUtilities.setOf("first", null, "third");
        assertEquals("Set size should reflect null as a distinct element", 3, result.size());
        assertTrue("Set should contain null", result.contains(null));
    }
}
