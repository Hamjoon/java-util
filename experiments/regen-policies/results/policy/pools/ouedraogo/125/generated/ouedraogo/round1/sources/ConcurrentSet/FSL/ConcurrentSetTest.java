package com.cedarsoftware.util;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.*;

public class ConcurrentSetTest {

    // ----------------------------------------------------------------------
    // Constructor tests
    // ----------------------------------------------------------------------
    @Test
    public void testEmptyConstructor() {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    public void testCollectionConstructorWithNulls() {
        List<String> list = Arrays.asList("a", null, "b", "a");
        ConcurrentSet<String> set = new ConcurrentSet<>(list);
        assertEquals(3, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertTrue(set.contains(null));
    }

    @Test
    public void testSetConstructorWrapsNulls() {
        Set<String> source = new HashSet<>();
        source.add(null);
        source.add("x");
        ConcurrentSet<String> set = new ConcurrentSet<>(source);
        assertEquals(2, set.size());
        assertTrue(set.contains(null));
        assertTrue(set.contains("x"));
    }

    // ----------------------------------------------------------------------
    // Basic mutable operations
    // ----------------------------------------------------------------------
    @Test
    public void testAddAndContains() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        assertTrue(set.add(1));
        assertTrue(set.contains(1));
        assertFalse(set.add(1)); // duplicate
        assertTrue(set.add(null));
        assertTrue(set.contains(null));
    }

    @Test
    public void testRemove() {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.add("foo");
        set.add(null);
        assertTrue(set.remove("foo"));
        assertFalse(set.contains("foo"));
        assertTrue(set.remove(null));
        assertFalse(set.contains(null));
        // removing non‑existent element returns false
        assertFalse(set.remove("bar"));
    }

    @Test
    public void testAddAllAndContainsAll() {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        List<String> items = Arrays.asList("a", "b", null, "c");
        assertTrue(set.addAll(items));
        assertEquals(4, set.size());
        assertTrue(set.containsAll(Arrays.asList("a", null, "c")));
        assertFalse(set.containsAll(Arrays.asList("a", "d")));
    }

    @Test
    public void testRemoveAll() {
        ConcurrentSet<String> set = new ConcurrentSet<>(Arrays.asList("x", "y", null));
        assertTrue(set.removeAll(Arrays.asList("y", null)));
        assertEquals(1, set.size());
        assertTrue(set.contains("x"));
        assertFalse(set.removeAll(Collections.singleton("non‑existent")));
    }

    @Test
    public void testRetainAll() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>(Arrays.asList(1, 2, 3, null));
        assertTrue(set.retainAll(Arrays.asList(2, null, 4)));
        assertEquals(2, set.size());
        assertTrue(set.contains(2));
        assertTrue(set.contains(null));
        assertFalse(set.contains(1));
        assertFalse(set.contains(3));
    }

    @Test
    public void testClearAndIsEmpty() {
        ConcurrentSet<String> set = new ConcurrentSet<>(Arrays.asList("a", "b"));
        assertFalse(set.isEmpty());
        set.clear();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    // ----------------------------------------------------------------------
    // Iterator and array conversions
    // ----------------------------------------------------------------------
    @Test
    public void testIteratorIncludesNullAndAllElements() {
        ConcurrentSet<String> set = new ConcurrentSet<>(Arrays.asList("one", null, "two"));
        Set<String> seen = new HashSet<>();
        for (String s : set) {
            seen.add(s);
        }
        assertEquals(3, seen.size());
        assertTrue(seen.contains("one"));
        assertTrue(seen.contains("two"));
        assertTrue(seen.contains(null));
    }

    @Test
    public void testToArrayWithoutType() {
        ConcurrentSet<String> set = new ConcurrentSet<>(Arrays.asList("x", null, "y"));
        Object[] arr = set.toArray();
        assertEquals(3, arr.length);
        List<Object> list = Arrays.asList(arr);
        assertTrue(list.contains("x"));
        assertTrue(list.contains("y"));
        assertTrue(list.contains(null));
    }

    @Test
    public void testToArrayWithTypedArray() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>(Arrays.asList(10, null, 20));
        Integer[] supplied = new Integer[5]; // larger than needed
        Integer[] result = set.toArray(supplied);
        // result should be the same array instance because it was large enough
        assertSame(supplied, result);
        // first three positions hold the elements (order not guaranteed)
        List<Integer> elems = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            elems.add(result[i]);
        }
        assertTrue(elems.contains(10));
        assertTrue(elems.contains(20));
        assertTrue(elems.contains(null));
        // the element after the last should be null per contract
        assertNull(result[3]);
    }

    @Test
    public void testToArrayWithTooSmallArray() {
        ConcurrentSet<String> set = new ConcurrentSet<>(Arrays.asList("a", "b"));
        String[] small = new String[1];
        String[] result = set.toArray(small);
        // a new array must be created
        assertNotSame(small, result);
        assertEquals(2, result.length);
        List<String> elems = Arrays.asList(result);
        assertTrue(elems.contains("a"));
        assertTrue(elems.contains("b"));
    }

    // ----------------------------------------------------------------------
    // Equality, hashCode and toString
    // ----------------------------------------------------------------------
    @Test
    public void testEqualsWithHashSet() {
        Set<String> hashSet = new HashSet<>(Arrays.asList("p", null, "q"));
        ConcurrentSet<String> concSet = new ConcurrentSet<>(hashSet);
        assertTrue(concSet.equals(hashSet));
        assertTrue(hashSet.equals(concSet));
    }

    @Test
    public void testEqualsNegativeCases() {
        ConcurrentSet<String> set = new ConcurrentSet<>(Arrays.asList("a", "b"));
        assertFalse(set.equals(null));
        assertFalse(set.equals("not a set"));
        // different size
        assertFalse(set.equals(new HashSet<>(Collections.singleton("a"))));
        // same size but different element
        assertFalse(set.equals(new HashSet<>(Arrays.asList("a", "c"))));
    }

    @Test
    public void testHashCodeMatchesSpecification() {
        ConcurrentSet<String> set = new ConcurrentSet<>(Arrays.asList("alpha", null, "beta"));
        int expected = "alpha".hashCode() + "beta".hashCode(); // null contributes 0
        assertEquals(expected, set.hashCode());
    }

    @Test
    public void testToStringRepresentation() {
        ConcurrentSet<String> set = new ConcurrentSet<>(Arrays.asList("x", null, "y"));
        String s = set.toString();
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        // remove braces and split by comma
        String inner = s.substring(1, s.length() - 1).trim();
        Set<String> parts = new HashSet<>();
        if (!inner.isEmpty()) {
            for (String token : inner.split(",")) {
                parts.add(token.trim());
            }
        }
        // expected textual elements
        assertTrue(parts.contains("x"));
        assertTrue(parts.contains("y"));
        assertTrue(parts.contains("null"));
        // there should be exactly three elements
        assertEquals(3, parts.size());
    }

    // ----------------------------------------------------------------------
    // Miscellaneous edge cases
    // ----------------------------------------------------------------------
    @Test
    public void testWrapUnwrapDoesNotExposeSentinel() {
        // The sentinel should never be visible via public APIs
        ConcurrentSet<Object> set = new ConcurrentSet<>();
        set.add(null);
        for (Object o : set) {
            assertNull(o);
        }
        Object[] arr = set.toArray();
        assertNull(arr[0]);
    }

    @Test
    public void testSelfReferenceInToString() {
        ConcurrentSet<Object> set = new ConcurrentSet<>();
        set.add(set); // add the set itself as an element
        String s = set.toString();
        assertTrue(s.contains("(this Set)"));
    }
}
