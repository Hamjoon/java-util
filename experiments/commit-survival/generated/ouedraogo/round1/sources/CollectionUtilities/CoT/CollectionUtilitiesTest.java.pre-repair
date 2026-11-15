package com.cedarsoftware.util;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.*;

public class CollectionUtilitiesTest {

    /* ----------------------------------------------------
     *  Public method signatures extracted from CollectionUtilities
     * ----------------------------------------------------
     * public static boolean isEmpty(Collection col)
     * public static boolean hasContent(Collection col)
     * public static int size(Collection col)
     * public static <T> List<T> listOf(T... items)
     * public static <T> Set<T> setOf(T... items)
     * ---------------------------------------------------- */

    /* ==================== isEmpty ==================== */

    @Test
    public void testIsEmpty_NullCollection() {
        assertTrue(CollectionUtilities.isEmpty(null));
    }

    @Test
    public void testIsEmpty_EmptyCollection() {
        Collection<String> empty = new ArrayList<>();
        assertTrue(CollectionUtilities.isEmpty(empty));
    }

    @Test
    public void testIsEmpty_NonEmptyCollection() {
        Collection<Integer> list = Arrays.asList(1, 2, 3);
        assertFalse(CollectionUtilities.isEmpty(list));
    }

    /* ==================== hasContent ==================== */

    @Test
    public void testHasContent_NullCollection() {
        assertFalse(CollectionUtilities.hasContent(null));
    }

    @Test
    public void testHasContent_EmptyCollection() {
        Collection<String> empty = new LinkedHashSet<>();
        assertFalse(CollectionUtilities.hasContent(empty));
    }

    @Test
    public void testHasContent_NonEmptyCollection() {
        Collection<String> set = new HashSet<>(Arrays.asList("a", "b"));
        assertTrue(CollectionUtilities.hasContent(set));
    }

    /* ==================== size ==================== */

    @Test
    public void testSize_NullCollection() {
        assertEquals(0, CollectionUtilities.size(null));
    }

    @Test
    public void testSize_EmptyCollection() {
        List<Integer> empty = new ArrayList<>();
        assertEquals(0, CollectionUtilities.size(empty));
    }

    @Test
    public void testSize_NonEmptyCollection() {
        Set<String> set = new HashSet<>(Arrays.asList("x", "y", "z"));
        assertEquals(3, CollectionUtilities.size(set));
    }

    /* ==================== listOf ==================== */

    @Test
    public void testListOf_NullArray() {
        List<Object> result = CollectionUtilities.listOf((Object[]) null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
        // verify unmodifiable
        try {
            result.add(new Object());
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void testListOf_EmptyArray() {
        List<String> result = CollectionUtilities.listOf();
        assertNotNull(result);
        assertTrue(result.isEmpty());
        try {
            result.add("test");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void testListOf_WithElements() {
        List<Integer> result = CollectionUtilities.listOf(1, 2, 3);
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testListOf_WithNullElement() {
        List<String> result = CollectionUtilities.listOf("a", null, "c");
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertNull(result.get(1));
        assertEquals("c", result.get(2));
    }

    @Test
    public void testListOf_Unmodifiable() {
        List<String> result = CollectionUtilities.listOf("x", "y");
        try {
            result.set(0, "z");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            result.add("new");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    /* ==================== setOf ==================== */

    @Test
    public void testSetOf_NullArray() {
        Set<Object> result = CollectionUtilities.setOf((Object[]) null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
        try {
            result.add(new Object());
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void testSetOf_EmptyArray() {
        Set<Integer> result = CollectionUtilities.setOf();
        assertNotNull(result);
        assertTrue(result.isEmpty());
        try {
            result.add(1);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void testSetOf_WithElements() {
        Set<String> result = CollectionUtilities.setOf("a", "b", "c");
        Set<String> expected = new LinkedHashSet<>(Arrays.asList("a", "b", "c"));
        assertEquals(expected, result);
        // order must be preserved because LinkedHashSet is used before unmodifiable wrapper
        Iterator<String> it = result.iterator();
        assertEquals("a", it.next());
        assertEquals("b", it.next());
        assertEquals("c", it.next());
    }

    @Test
    public void testSetOf_WithDuplicates() {
        Set<Integer> result = CollectionUtilities.setOf(1, 2, 2, 3, 1);
        Set<Integer> expected = new LinkedHashSet<>(Arrays.asList(1, 2, 3));
        assertEquals(expected, result);
        assertEquals(3, result.size());
    }

    @Test
    public void testSetOf_WithNullElement() {
        Set<String> result = CollectionUtilities.setOf("x", null, "y");
        assertTrue(result.contains(null));
        assertTrue(result.contains("x"));
        assertTrue(result.contains("y"));
        assertEquals(3, result.size());
    }

    @Test
    public void testSetOf_Unmodifiable() {
        Set<String> result = CollectionUtilities.setOf("alpha", "beta");
        try {
            result.add("gamma");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            result.remove("alpha");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }
}
