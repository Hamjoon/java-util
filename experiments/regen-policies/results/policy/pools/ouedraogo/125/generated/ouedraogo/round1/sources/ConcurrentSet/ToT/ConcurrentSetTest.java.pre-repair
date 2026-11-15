package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.*;

public class ConcurrentSetTest {

    private ConcurrentSet<String> emptySet;
    private ConcurrentSet<Integer> intSet;
    private ConcurrentSet<String> setWithNull;

    @Before
    public void setUp() {
        emptySet = new ConcurrentSet<>();
        intSet = new ConcurrentSet<>(Arrays.asList(1, 2, 3));
        setWithNull = new ConcurrentSet<>(Arrays.asList("a", null, "b"));
    }

    // -----------------------------------------------------------------
    //  Constructor tests
    // -----------------------------------------------------------------
    @Test
    public void testDefaultConstructorCreatesEmptySet() {
        assertTrue(emptySet.isEmpty());
        assertEquals(0, emptySet.size());
    }

    @Test
    public void testCollectionConstructorCopiesElements() {
        ConcurrentSet<String> copy = new ConcurrentSet<>(Arrays.asList("x", "y", null));
        assertEquals(3, copy.size());
        assertTrue(copy.contains("x"));
        assertTrue(copy.contains("y"));
        assertTrue(copy.contains(null));
    }

    @Test
    public void testSetConstructorCopiesElements() {
        Set<String> source = new HashSet<>(Arrays.asList("p", null, "q"));
        ConcurrentSet<String> copy = new ConcurrentSet<>(source);
        assertEquals(3, copy.size());
        assertTrue(copy.containsAll(source));
    }

    // -----------------------------------------------------------------
    //  Basic immutable API tests
    // -----------------------------------------------------------------
    @Test
    public void testEqualsAndHashCode() {
        ConcurrentSet<Integer> a = new ConcurrentSet<>(Arrays.asList(1, 2, 3));
        ConcurrentSet<Integer> b = new ConcurrentSet<>(Arrays.asList(3, 2, 1));
        ConcurrentSet<Integer> c = new ConcurrentSet<>(Arrays.asList(1, 2));

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

        assertNotEquals(a, c);
        assertNotEquals(a, null);
        assertNotEquals(a, "not a set");
    }

    @Test
    public void testToStringEmptyAndNonEmpty() {
        assertEquals("{}", emptySet.toString());

        ConcurrentSet<String> s = new ConcurrentSet<>(Arrays.asList("one", "two"));
        String result = s.toString();
        // Order is not guaranteed, so check both possible orders
        assertTrue(result.equals("{one, two}") || result.equals("{two, one}"));
    }

    @Test
    public void testContainsHandlesNullSentinel() {
        assertTrue(setWithNull.contains(null));
        assertTrue(setWithNull.contains("a"));
        assertFalse(setWithNull.contains("c"));
    }

    @Test
    public void testContainsAll() {
        List<String> sub = Arrays.asList("a", null);
        assertTrue(setWithNull.containsAll(sub));

        List<String> notSub = Arrays.asList("a", "c");
        assertFalse(setWithNull.containsAll(notSub));
    }

    // -----------------------------------------------------------------
    //  Basic mutable API tests
    // -----------------------------------------------------------------
    @Test
    public void testAddAndRemove() {
        assertTrue(emptySet.add("hello"));
        assertTrue(emptySet.contains("hello"));
        assertEquals(1, emptySet.size());

        // Adding duplicate returns false
        assertFalse(emptySet.add("hello"));
        assertEquals(1, emptySet.size());

        // Remove existing element
        assertTrue(emptySet.remove("hello"));
        assertFalse(emptySet.contains("hello"));
        assertTrue(emptySet.isEmpty());

        // Remove non‑existing element
        assertFalse(emptySet.remove("world"));
    }

    @Test
    public void testAddNullAndRemoveNull() {
        assertTrue(emptySet.add(null));
        assertTrue(emptySet.contains(null));
        assertEquals(1, emptySet.size());

        // Duplicate null add should be false
        assertFalse(emptySet.add(null));

        assertTrue(emptySet.remove(null));
        assertFalse(emptySet.contains(null));
        assertTrue(emptySet.isEmpty());
    }

    @Test
    public void testAddAllAndRemoveAll() {
        List<Integer> toAdd = Arrays.asList(4, 5, null);
        // intSet does not contain null yet
        assertTrue(intSet.addAll(toAdd));
        assertTrue(intSet.containsAll(toAdd));
        assertEquals(6, intSet.size());

        // Adding same collection again returns false (no modification)
        assertFalse(intSet.addAll(toAdd));

        // RemoveAll
        List<Integer> toRemove = Arrays.asList(2, null);
        assertTrue(intSet.removeAll(toRemove));
        assertFalse(intSet.contains(2));
        assertFalse(intSet.contains(null));
        assertEquals(4, intSet.size());

        // Removing a collection that is not present returns false
        assertFalse(intSet.removeAll(Arrays.asList(99)));
    }

    @Test
    public void testRetainAll() {
        // intSet currently has 1,2,3
        List<Integer> retain = Arrays.asList(2, 3, 99);
        assertTrue(intSet.retainAll(retain));
        assertEquals(2, intSet.size());
        assertTrue(intSet.contains(2));
        assertTrue(intSet.contains(3));
        assertFalse(intSet.contains(1));

        // Retaining with a superset should not modify
        assertFalse(intSet.retainAll(Arrays.asList(2, 3, 4)));
    }

    @Test
    public void testClear() {
        assertFalse(emptySet.isEmpty());
        emptySet.add("x");
        emptySet.add(null);
        assertFalse(emptySet.isEmpty());
        emptySet.clear();
        assertTrue(emptySet.isEmpty());
        assertEquals(0, emptySet.size());
    }

    // -----------------------------------------------------------------
    //  Iterator and array conversion tests
    // -----------------------------------------------------------------
    @Test
    public void testIteratorTraversalAndRemove() {
        Set<String> expected = new HashSet<>(Arrays.asList("a", null, "b"));
        Iterator<String> it = setWithNull.iterator();
        while (it.hasNext()) {
            String val = it.next();
            assertTrue(expected.remove(val));
        }
        assertTrue(expected.isEmpty());

        // Re‑populate and test iterator.remove()
        setWithNull.add("c");
        Iterator<String> it2 = setWithNull.iterator();
        while (it2.hasNext()) {
            String val = it2.next();
            if ("a".equals(val) || val == null) {
                it2.remove();
            }
        }
        assertFalse(setWithNull.contains("a"));
        assertFalse(setWithNull.contains(null));
        assertTrue(setWithNull.contains("b"));
        assertTrue(setWithNull.contains("c"));
    }

    @Test
    public void testToArrayObject() {
        Object[] arr = setWithNull.toArray();
        // Convert to a Set for order‑independent verification
        Set<Object> asSet = new HashSet<>(Arrays.asList(arr));
        assertEquals(3, asSet.size());
        assertTrue(asSet.contains("a"));
        assertTrue(asSet.contains("b"));
        assertTrue(asSet.contains(null));
    }

    @Test
    public void testToArrayTypedExactSize() {
        String[] dest = new String[3];
        String[] result = setWithNull.toArray(dest);
        assertSame(dest, result);
        List<String> list = Arrays.asList(result);
        assertTrue(list.contains("a"));
        assertTrue(list.contains("b"));
        assertTrue(list.contains(null));
    }

    @Test
    public void testToArrayTypedLargerDestination() {
        String[] dest = new String[5];
        String[] result = setWithNull.toArray(dest);
        // Should be the same array instance
        assertSame(dest, result);
        // First three entries are the set elements (order not guaranteed)
        Set<String> firstThree = new HashSet<>();
        for (int i = 0; i < 3; i++) {
            firstThree.add(result[i]);
        }
        assertTrue(firstThree.contains("a"));
        assertTrue(firstThree.contains("b"));
        assertTrue(firstThree.contains(null));

        // The element after the last should be null per spec
        assertNull(result[3]);
        // The final slot remains untouched (could be null)
    }

    @Test
    public void testToArrayTypedSmallerDestination() {
        String[] dest = new String[1];
        String[] result = setWithNull.toArray(dest);
        // Should be a new array because destination was too small
        assertNotSame(dest, result);
        assertEquals(3, result.length);
        Set<String> asSet = new HashSet<>(Arrays.asList(result));
        assertTrue(asSet.contains("a"));
        assertTrue(asSet.contains("b"));
        assertTrue(asSet.contains(null));
    }

    // -----------------------------------------------------------------
    //  Edge cases & error handling
    // -----------------------------------------------------------------
    @Test(expected = ClassCastException.class)
    public void testContainsWithIncompatibleTypeThrows() {
        // The internal wrap casts the argument to T; passing an incompatible type
        // should result in a ClassCastException.
        emptySet.contains(new Object());
    }

    @Test
    public void testEqualsWithDifferentImplementationSet() {
        Set<String> hashSet = new HashSet<>(Arrays.asList("a", null, "b"));
        assertTrue(setWithNull.equals(hashSet));
        assertTrue(hashSet.equals(setWithNull)); // HashSet's equals uses containsAll, which works
    }

    @Test
    public void testHashCodeConsistencyAfterModifications() {
        int initialHash = setWithNull.hashCode();
        setWithNull.add("c");
        int afterAddHash = setWithNull.hashCode();
        assertNotEquals(initialHash, afterAddHash);

        setWithNull.remove("a");
        int afterRemoveHash = setWithNull.hashCode();
        assertNotEquals(afterAddHash, afterRemoveHash);
    }

    @Test
    public void testIteratorRemoveOnEmptySetThrows() {
        Iterator<String> it = emptySet.iterator();
        try {
            it.remove();
            fail("Expected IllegalStateException when calling remove() before next()");
        } catch (IllegalStateException expected) {
            // expected
        }
    }
}
