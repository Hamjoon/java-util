package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link ConcurrentSet}.
 *
 * The tests cover:
 * <ul>
 *   <li>All three constructors (empty, from Collection, from existing Set)</li>
 *   <li>Null handling (sentinel conversion)</li>
 *   <li>Standard {@link Set} contract methods: {@code add}, {@code remove},
 *       {@code contains}, {@code containsAll}, {@code addAll},
 *       {@code removeAll}, {@code retainAll}, {@code clear},
 *       {@code size}, {@code isEmpty}</li>
 *   <li>Immutable view methods: {@code equals}, {@code hashCode},
 *       {@code toString}, {@code iterator}</li>
 *   <li>Array conversion methods: {@code toArray()}, {@code toArray(T[])} with
 *       array sized smaller, equal and larger than the set.</li>
 *   <li>Iterator {@code remove()} semantics.</li>
 *   <li>Special {@code toString()} handling of the set referencing itself.</li>
 * </ul>
 *
 * No external mocking frameworks are used – all objects are created via the
 * public API of {@link ConcurrentSet} only.
 */
public class ConcurrentSetTest {

    /* -------------------------------------------------------------
     *  Constructor tests
     * ------------------------------------------------------------- */

    @Test
    public void testEmptyConstructor() {
        ConcurrentSet<String> cs = new ConcurrentSet<>();
        assertTrue(cs.isEmpty());
        assertEquals(0, cs.size());
    }

    @Test
    public void testCollectionConstructor() {
        Collection<String> src = Arrays.asList("a", "b", null, "a");
        ConcurrentSet<String> cs = new ConcurrentSet<>(src);
        // Duplicates are eliminated, null is kept once
        assertEquals(3, cs.size());
        assertTrue(cs.contains("a"));
        assertTrue(cs.contains("b"));
        assertTrue(cs.contains(null));
    }

    @Test
    public void testSetWrappingConstructor() {
        Set<String> src = new HashSet<>(Arrays.asList("x", null, "y"));
        ConcurrentSet<String> cs = new ConcurrentSet<>(src);
        assertEquals(3, cs.size());
        assertTrue(cs.containsAll(src));
    }

    /* -------------------------------------------------------------
     *  Basic add / contains / remove (including null handling)
     * ------------------------------------------------------------- */

    @Test
    public void testAddAndContains() {
        ConcurrentSet<Integer> cs = new ConcurrentSet<>();
        assertTrue(cs.add(1));
        assertTrue(cs.add(null));
        assertTrue(cs.contains(1));
        assertTrue(cs.contains(null));
        // Adding duplicate returns false
        assertFalse(cs.add(1));
        assertFalse(cs.add(null));
        assertEquals(2, cs.size());
    }

    @Test
    public void testRemove() {
        ConcurrentSet<String> cs = new ConcurrentSet<>();
        cs.add("alpha");
        cs.add(null);
        assertTrue(cs.remove("alpha"));
        assertFalse(cs.contains("alpha"));
        assertTrue(cs.remove(null));
        assertFalse(cs.contains(null));
        // Removing non‑existent element returns false
        assertFalse(cs.remove("beta"));
    }

    /* -------------------------------------------------------------
     *  Bulk operations
     * ------------------------------------------------------------- */

    @Test
    public void testAddAllAndContainsAll() {
        ConcurrentSet<String> cs = new ConcurrentSet<>();
        Collection<String> col = Arrays.asList("one", "two", null, "one");
        assertTrue(cs.addAll(col));
        // addAll should have added three distinct elements
        assertEquals(3, cs.size());
        assertTrue(cs.containsAll(Arrays.asList("one", "two", null)));
        // Adding the same collection again returns false (no modification)
        assertFalse(cs.addAll(col));
    }

    @Test
    public void testRemoveAll() {
        ConcurrentSet<String> cs = new ConcurrentSet<>(Arrays.asList("a", "b", null, "c"));
        Collection<String> toRemove = Arrays.asList("b", null, "x");
        assertTrue(cs.removeAll(toRemove));
        assertEquals(2, cs.size());
        assertFalse(cs.contains("b"));
        assertFalse(cs.contains(null));
        assertTrue(cs.contains("a"));
        assertTrue(cs.contains("c"));
        // Removing again should report no modification
        assertFalse(cs.removeAll(toRemove));
    }

    @Test
    public void testRetainAll() {
        ConcurrentSet<Integer> cs = new ConcurrentSet<>(Arrays.asList(1, 2, 3, null));
        Collection<Integer> retain = Arrays.asList(2, null, 5);
        assertTrue(cs.retainAll(retain));
        assertEquals(2, cs.size());
        assertTrue(cs.contains(2));
        assertTrue(cs.contains(null));
        assertFalse(cs.contains(1));
        assertFalse(cs.contains(3));
        // Retaining the same collection again yields no change
        assertFalse(cs.retainAll(retain));
    }

    @Test
    public void testClearAndIsEmpty() {
        ConcurrentSet<String> cs = new ConcurrentSet<>();
        cs.add("x");
        cs.add(null);
        cs.clear();
        assertTrue(cs.isEmpty());
        assertEquals(0, cs.size());
    }

    /* -------------------------------------------------------------
     *  Iterator behavior
     * ------------------------------------------------------------- */

    @Test
    public void testIteratorAndRemoveViaIterator() {
        ConcurrentSet<String> cs = new ConcurrentSet<>(Arrays.asList("a", "b", null));
        Iterator<String> it = cs.iterator();
        Set<String> seen = new HashSet<>();
        while (it.hasNext()) {
            String val = it.next();
            seen.add(val);
            if ("b".equals(val)) {
                it.remove(); // remove "b" via iterator
            }
        }
        assertEquals(3, seen.size());
        assertFalse(cs.contains("b"));
        assertTrue(cs.contains("a"));
        assertTrue(cs.contains(null));
    }

    /* -------------------------------------------------------------
     *  equals() and hashCode()
     * ------------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCodeWithOtherSetImplementations() {
        Set<String> reference = new HashSet<>(Arrays.asList("x", null, "y"));
        ConcurrentSet<String> cs = new ConcurrentSet<>(reference);
        assertEquals(reference, cs);
        assertEquals(cs, reference);
        // hashCode must be the sum of element hashCodes (null contributes 0)
        int expectedHash = 0;
        for (String s : reference) {
            expectedHash += (s == null ? 0 : s.hashCode());
        }
        assertEquals(expectedHash, cs.hashCode());
    }

    @Test
    public void testEqualsWithDifferentSizesOrTypes() {
        ConcurrentSet<Integer> cs = new ConcurrentSet<>(Arrays.asList(1, 2));
        Set<Integer> other = new HashSet<>(Arrays.asList(1, 2, 3));
        assertNotEquals(cs, other);
        assertNotEquals(cs, "not a set");
    }

    /* -------------------------------------------------------------
     *  toString()
     * ------------------------------------------------------------- */

    @Test
    public void testToStringEmpty() {
        ConcurrentSet<String> cs = new ConcurrentSet<>();
        assertEquals("{}", cs.toString());
    }

    @Test
    public void testToStringWithElementsIncludingNullAndSelfReference() {
        ConcurrentSet<Object> cs = new ConcurrentSet<>();
        cs.add("hello");
        cs.add(null);
        cs.add(cs); // add the set itself to test the "(this Set)" placeholder
        String s = cs.toString();

        // The string must start with '{' and end with '}'
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));

        // It must contain the literal "hello", the literal "null" (as the null element)
        // and the placeholder "(this Set)".
        assertTrue(s.contains("hello"));
        assertTrue(s.contains("null"));
        assertTrue(s.contains("(this Set)"));
    }

    /* -------------------------------------------------------------
     *  toArray() variants
     * ------------------------------------------------------------- */

    @Test
    public void testToArrayObject() {
        ConcurrentSet<String> cs = new ConcurrentSet<>(Arrays.asList("a", null, "b"));
        Object[] arr = cs.toArray();
        assertEquals(3, arr.length);
        List<Object> list = Arrays.asList(arr);
        assertTrue(list.contains("a"));
        assertTrue(list.contains("b"));
        assertTrue(list.contains(null));
    }

    @Test
    public void testToArrayGeneric_SmallerArray() {
        ConcurrentSet<Integer> cs = new ConcurrentSet<>(Arrays.asList(10, null, 20));
        Integer[] small = new Integer[1]; // smaller than set size
        Integer[] result = cs.toArray(small);
        // Should get a new array of exact size
        assertEquals(3, result.length);
        List<Integer> list = Arrays.asList(result);
        assertTrue(list.contains(10));
        assertTrue(list.contains(20));
        assertTrue(list.contains(null));
    }

    @Test
    public void testToArrayGeneric_ExactSizeArray() {
        ConcurrentSet<String> cs = new ConcurrentSet<>(Arrays.asList("x", null));
        String[] exact = new String[2];
        String[] result = cs.toArray(exact);
        // Should reuse the supplied array
        assertSame(exact, result);
        List<String> list = Arrays.asList(result);
        assertTrue(list.contains("x"));
        assertTrue(list.contains(null));
    }

    @Test
    public void testToArrayGeneric_LargerArray() {
        ConcurrentSet<String> cs = new ConcurrentSet<>(Arrays.asList("p"));
        String[] larger = new String[3];
        larger[1] = "will be overwritten";
        larger[2] = "will stay null after call";
        String[] result = cs.toArray(larger);
        // The first element(s) are filled, next element set to null per contract
        assertSame(larger, result);
        assertEquals("p", result[0]);
        assertNull(result[1]); // null sentinel from set becomes null
        assertNull(result[2]); // extra slot cleared
    }

    /* -------------------------------------------------------------
     *  retainAll edge cases (null collection, empty collection)
     * ------------------------------------------------------------- */

    @Test
    public void testRetainAllWithEmptyCollection() {
        ConcurrentSet<String> cs = new ConcurrentSet<>(Arrays.asList("a", "b", null));
        assertTrue(cs.retainAll(new ArrayList<>())); // all elements removed
        assertTrue(cs.isEmpty());
    }

    @Test
    public void testRetainAllWithNullElementsOnly() {
        ConcurrentSet<String> cs = new ConcurrentSet<>(Arrays.asList("a", null, "b"));
        assertTrue(cs.retainAll(Arrays.asList((String) null)));
        assertEquals(1, cs.size());
        assertTrue(cs.contains(null));
    }
}
