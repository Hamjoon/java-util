package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.lang.reflect.Array;
import java.util.*;

import org.junit.Test;

/**
 * Test suite for {@link ConcurrentSet}.
 *
 * The tests cover:
 * <ul>
 *   <li>All constructors (empty, from Collection, from Set)</li>
 *   <li>Basic Set contract methods (size, isEmpty, clear, contains, containsAll)</li>
 *   <li>Mutable operations (add, remove, addAll, removeAll, retainAll)</li>
 *   <li>Iteration and iterator.remove()</li>
 *   <li>Array conversion (toArray() and toArray(T[])) including handling of null elements</li>
 *   <li>Equals, hashCode and toString implementations, including special handling of {@code null}
 *       and self‑reference in {@code toString()}</li>
 * </ul>
 *
 * No external mocking frameworks are used; all objects are created via the public API.
 */
public class ConcurrentSetTest {

    /* -------------------------------------------------------
     *  Helper methods
     * -----------------------------------------------------*/

    /** Creates a {@link ConcurrentSet} populated with the given elements (including nulls). */
    private ConcurrentSet<String> setOf(String... elements) {
        List<String> list = new ArrayList<>(Arrays.asList(elements));
        return new ConcurrentSet<>(list);
    }

    /** Returns a regular {@link HashSet} with the same (unwrapped) elements as the given ConcurrentSet. */
    private Set<String> asHashSet(ConcurrentSet<String> cs) {
        Set<String> s = new HashSet<>();
        for (String e : cs) {
            s.add(e);
        }
        return s;
    }

    /* -------------------------------------------------------
     *  Constructor tests
     * -----------------------------------------------------*/

    @Test
    public void testEmptyConstructor() {
        ConcurrentSet<Integer> cs = new ConcurrentSet<>();
        assertTrue(cs.isEmpty());
        assertEquals(0, cs.size());
    }

    @Test
    public void testCollectionConstructorWithNulls() {
        List<String> src = Arrays.asList("a", null, "b", null);
        ConcurrentSet<String> cs = new ConcurrentSet<>(src);
        assertEquals(3, cs.size()); // null is stored only once
        assertTrue(cs.contains("a"));
        assertTrue(cs.contains("b"));
        assertTrue(cs.contains(null));
        // verify iterator yields null exactly once
        int nullCount = 0;
        for (String s : cs) {
            if (s == null) nullCount++;
        }
        assertEquals(1, nullCount);
    }

    @Test
    public void testSetConstructorCopiesElements() {
        Set<String> src = new HashSet<>(Arrays.asList("x", "y", null));
        ConcurrentSet<String> cs = new ConcurrentSet<>(src);
        assertEquals(3, cs.size());
        assertTrue(cs.containsAll(src));
        // modify original set and ensure cs is independent
        src.remove("x");
        assertTrue(cs.contains("x"));
    }

    /* -------------------------------------------------------
     *  Basic immutable API tests
     * -----------------------------------------------------*/

    @Test
    public void testContainsAndIsEmpty() {
        ConcurrentSet<String> cs = setOf("one", "two");
        assertTrue(cs.contains("one"));
        assertFalse(cs.contains("three"));
        assertTrue(cs.contains(null) == false);
        cs.add(null);
        assertTrue(cs.contains(null));
        assertFalse(cs.isEmpty());
    }

    @Test
    public void testContainsAll() {
        ConcurrentSet<String> cs = setOf("a", "b", null);
        List<String> superset = Arrays.asList("a", "b", null, "c");
        List<String> subset = Arrays.asList("a", null);
        List<String> notSubset = Arrays.asList("a", "z");
        assertTrue(cs.containsAll(superset) == false);
        assertTrue(cs.containsAll(subset));
        assertFalse(cs.containsAll(notSubset));
    }

    @Test
    public void testEqualsAndHashCode() {
        ConcurrentSet<String> cs1 = setOf("x", null, "y");
        ConcurrentSet<String> cs2 = setOf("y", "x", null);
        HashSet<String> hs = new HashSet<>(Arrays.asList("x", "y", null));

        // equality with another ConcurrentSet
        assertEquals(cs1, cs2);
        assertEquals(cs1.hashCode(), cs2.hashCode());

        // equality with a plain Set (HashSet)
        assertEquals(cs1, hs);
        assertEquals(hs, cs1); // HashSet's equals will call cs1.contains for each element

        // inequality with different content
        cs2.remove(null);
        assertNotEquals(cs1, cs2);
        assertNotEquals(cs1, Collections.singleton("x"));
    }

    @Test
    public void testEqualsHandlesClassCastAndNullPointerExceptionGracefully() {
        // Create a set that contains an element of a different type
        ConcurrentSet raw = new ConcurrentSet();
        raw.add("string");
        raw.add(123); // raw add bypasses generics, but still uses wrap()
        // Compare with a Set<String>
        Set<String> stringSet = new HashSet<>(Collections.singletonList("string"));
        // The equals implementation should catch ClassCastException and return false
        assertFalse(raw.equals(stringSet));
    }

    @Test
    public void testToStringFormatting() {
        ConcurrentSet<String> empty = new ConcurrentSet<>();
        assertEquals("{}", empty.toString());

        ConcurrentSet<String> cs = setOf("a", null);
        String s = cs.toString();
        // Order is unspecified; verify content via Set comparison
        Set<String> expected = new HashSet<>(Arrays.asList("a", null));
        // Strip surrounding braces and split
        String inner = s.substring(1, s.length() - 1).trim();
        Set<String> actual = new HashSet<>();
        if (!inner.isEmpty()) {
            for (String part : inner.split(",\\s*")) {
                if (part.equals("null")) {
                    actual.add(null);
                } else {
                    actual.add(part);
                }
            }
        }
        assertEquals(expected, actual);
    }

    @Test
    public void testToStringSelfReference() {
        ConcurrentSet<Object> cs = new ConcurrentSet<>();
        cs.add(cs); // add the set itself
        String s = cs.toString();
        // Expected: {(this Set)}
        assertEquals("{(this Set)}", s);
    }

    /* -------------------------------------------------------
     *  Mutable API tests
     * -----------------------------------------------------*/

    @Test
    public void testAddAndRemove() {
        ConcurrentSet<Integer> cs = new ConcurrentSet<>();
        assertTrue(cs.add(10));
        assertTrue(cs.contains(10));
        assertFalse(cs.add(10)); // duplicate, should not modify
        assertTrue(cs.remove(10));
        assertFalse(cs.contains(10));
        assertFalse(cs.remove(10)); // already removed
    }

    @Test
    public void testAddNullAndRemoveNull() {
        ConcurrentSet<String> cs = new ConcurrentSet<>();
        assertTrue(cs.add(null));
        assertTrue(cs.contains(null));
        assertFalse(cs.add(null)); // duplicate null
        assertTrue(cs.remove(null));
        assertFalse(cs.contains(null));
    }

    @Test
    public void testAddAll() {
        ConcurrentSet<String> cs = new ConcurrentSet<>();
        List<String> src = Arrays.asList("a", "b", null, "a");
        assertTrue(cs.addAll(src));
        assertEquals(3, cs.size()); // null only once, "a" duplicate ignored
        assertTrue(cs.containsAll(Arrays.asList("a", "b", null)));
    }

    @Test
    public void testRemoveAll() {
        ConcurrentSet<String> cs = setOf("a", "b", null);
        List<String> toRemove = Arrays.asList("b", null);
        assertTrue(cs.removeAll(toRemove));
        assertEquals(1, cs.size());
        assertTrue(cs.contains("a"));
        assertFalse(cs.contains("b"));
        assertFalse(cs.contains(null));

        // removing non‑present elements should return false
        assertFalse(cs.removeAll(Collections.singleton("z")));
    }

    @Test
    public void testRetainAll() {
        ConcurrentSet<String> cs = setOf("a", "b", null, "c");
        List<String> retain = Arrays.asList("b", null, "x");
        assertTrue(cs.retainAll(retain));
        assertEquals(2, cs.size());
        assertTrue(cs.contains("b"));
        assertTrue(cs.contains(null));
        assertFalse(cs.contains("a"));
        assertFalse(cs.contains("c"));
    }

    @Test
    public void testClearAndIsEmpty() {
        ConcurrentSet<Integer> cs = setOf(1, 2, 3);
        assertFalse(cs.isEmpty());
        cs.clear();
        assertTrue(cs.isEmpty());
        assertEquals(0, cs.size());
    }

    /* -------------------------------------------------------
     *  Iterator tests
     * -----------------------------------------------------*/

    @Test
    public void testIteratorTraversalAndRemove() {
        ConcurrentSet<String> cs = setOf("x", "y", null);
        Iterator<String> it = cs.iterator();
        Set<String> seen = new HashSet<>();
        while (it.hasNext()) {
            String val = it.next();
            assertTrue(seen.add(val)); // ensure each element appears once
            if ("y".equals(val)) {
                it.remove(); // remove via iterator
            }
        }
        assertEquals(2, cs.size());
        assertFalse(cs.contains("y"));
        assertTrue(cs.contains("x"));
        assertTrue(cs.contains(null));
    }

    /* -------------------------------------------------------
     *  Array conversion tests
     * -----------------------------------------------------*/

    @Test
    public void testToArrayWithoutNulls() {
        ConcurrentSet<Integer> cs = setOf(1, 2, 3);
        Object[] arr = cs.toArray();
        assertEquals(3, arr.length);
        Set<Integer> expected = new HashSet<>(Arrays.asList(1, 2, 3));
        Set<Integer> actual = new HashSet<>();
        for (Object o : arr) {
            actual.add((Integer) o);
        }
        assertEquals(expected, actual);
    }

    @Test
    public void testToArrayWithNulls() {
        ConcurrentSet<String> cs = setOf("a", null, "b");
        Object[] arr = cs.toArray();
        assertEquals(3, arr.length);
        List<Object> list = Arrays.asList(arr);
        assertTrue(list.contains("a"));
        assertTrue(list.contains("b"));
        assertTrue(list.contains(null));
    }

    @Test
    public void testToArrayGenericExactSize() {
        ConcurrentSet<String> cs = setOf("p", "q");
        String[] dest = new String[2];
        String[] result = cs.toArray(dest);
        assertSame(dest, result);
        Set<String> expected = new HashSet<>(Arrays.asList("p", "q"));
        Set<String> actual = new HashSet<>(Arrays.asList(result));
        assertEquals(expected, actual);
    }

    @Test
    public void testToArrayGenericSmallerArrayCreatesNewOne() {
        ConcurrentSet<String> cs = setOf("alpha", "beta", "gamma");
        String[] small = new String[2];
        String[] result = cs.toArray(small);
        assertNotSame(small, result);
        assertEquals(3, result.length);
        Set<String> expected = new HashSet<>(Arrays.asList("alpha", "beta", "gamma"));
        assertEquals(expected, new HashSet<>(Arrays.asList(result)));
    }

    @Test
    public void testToArrayGenericLargerArrayHasTrailingNull() {
        ConcurrentSet<Integer> cs = setOf(5, 6);
        Integer[] larger = new Integer[5];
        Integer[] result = cs.toArray(larger);
        assertSame(larger, result);
        // first two positions contain the elements (order unspecified)
        Set<Integer> found = new HashSet<>();
        for (int i = 0; i < cs.size(); i++) {
            found.add(result[i]);
        }
        assertEquals(new HashSet<>(Arrays.asList(5, 6)), found);
        // the element after the last should be null
        assertNull(result[2]);
        // remaining positions should stay null (as per spec)
        assertNull(result[3]);
        assertNull(result[4]);
    }

    @Test
    public void testToArrayGenericHandlesNullElements() {
        ConcurrentSet<String> cs = setOf(null, "z");
        String[] arr = cs.toArray(new String[0]);
        assertEquals(2, arr.length);
        List<String> list = Arrays.asList(arr);
        assertTrue(list.contains(null));
        assertTrue(list.contains("z"));
    }

    /* -------------------------------------------------------
     *  Additional edge‑case tests
     * -----------------------------------------------------*/

    @Test
    public void testAddAllWithEmptyCollectionDoesNotModify() {
        ConcurrentSet<String> cs = setOf("a");
        assertFalse(cs.addAll(Collections.emptyList()));
        assertEquals(1, cs.size());
    }

    @Test
    public void testRemoveAllWithEmptyCollectionDoesNotModify() {
        ConcurrentSet<String> cs = setOf("a");
        assertFalse(cs.removeAll(Collections.emptyList()));
        assertEquals(1, cs.size());
    }

    @Test
    public void testRetainAllWithEmptyCollectionEmptiesSet() {
        ConcurrentSet<String> cs = setOf("a", "b");
        assertTrue(cs.retainAll(Collections.emptyList()));
        assertTrue(cs.isEmpty());
    }

    @Test
    public void testHashCodeMatchesSumOfElementHashCodes() {
        ConcurrentSet<String> cs = setOf("one", null, "two");
        int expected = "one".hashCode() + "two".hashCode(); // null contributes 0
        assertEquals(expected, cs.hashCode());
    }

    @Test
    public void testEqualsWithDifferentImplementationButSameElements() {
        ConcurrentSet<String> cs = setOf("x", "y");
        Set<String> linked = new LinkedHashSet<>(Arrays.asList("y", "x"));
        assertEquals(cs, linked);
        assertEquals(linked, cs);
    }
}
