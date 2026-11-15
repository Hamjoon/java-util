package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.*;

public class ConcurrentSetTest {

    private ConcurrentSet<String> emptySet;
    private ConcurrentSet<String> setWithNull;
    private ConcurrentSet<Integer> intSet;

    @Before
    public void setUp() {
        emptySet = new ConcurrentSet<>();
        setWithNull = new ConcurrentSet<>(Arrays.asList("a", null, "b"));
        intSet = new ConcurrentSet<>(Arrays.asList(1, 2, 3));
    }

    /* ==================== Constructor Tests ==================== */

    @Test
    public void testEmptyConstructor() {
        assertTrue(emptySet.isEmpty());
        assertEquals(0, emptySet.size());
    }

    @Test
    public void testCollectionConstructorCopiesElementsIncludingNull() {
        assertEquals(3, setWithNull.size());
        assertTrue(setWithNull.contains("a"));
        assertTrue(setWithNull.contains("b"));
        assertTrue(setWithNull.contains(null));
    }

    @Test
    public void testSetConstructorCopiesElementsIncludingNull() {
        Set<String> source = new HashSet<>(Arrays.asList("x", null, "y"));
        ConcurrentSet<String> cs = new ConcurrentSet<>(source);
        assertEquals(3, cs.size());
        assertTrue(cs.contains("x"));
        assertTrue(cs.contains("y"));
        assertTrue(cs.contains(null));
    }

    /* ==================== equals() Tests ==================== */

    @Test
    public void testEqualsReflexive() {
        assertEquals(setWithNull, setWithNull);
    }

    @Test
    public void testEqualsSymmetricAndTransitive() {
        Set<String> other = new HashSet<>(Arrays.asList("a", "b", null));
        assertEquals(setWithNull, other);
        assertEquals(other, setWithNull);
        // Transitive via a third set
        ConcurrentSet<String> third = new ConcurrentSet<>(other);
        assertEquals(setWithNull, third);
        assertEquals(other, third);
    }

    @Test
    public void testEqualsDifferentSizes() {
        Set<String> smaller = new HashSet<>(Arrays.asList("a", null));
        assertNotEquals(setWithNull, smaller);
    }

    @Test
    public void testEqualsWithNonSetObject() {
        assertNotEquals(setWithNull, "not a set");
    }

    @Test
    public void testEqualsHandlesClassCastExceptionGracefully() {
        // Create a set that contains an element whose equals throws ClassCastException when compared to a String
        Set<Object> weirdSet = new HashSet<>();
        weirdSet.add(new Object() {
            @Override
            public boolean equals(Object o) {
                throw new ClassCastException("cannot compare");
            }
        });
        // Our set contains a normal string; equals should catch and return false
        ConcurrentSet<String> cs = new ConcurrentSet<>(Collections.singleton("test"));
        assertFalse(cs.equals(weirdSet));
    }

    /* ==================== hashCode() Tests ==================== */

    @Test
    public void testHashCodeConsistentWithEquals() {
        Set<String> other = new HashSet<>(Arrays.asList("a", "b", null));
        assertEquals(setWithNull, other);
        assertEquals(setWithNull.hashCode(), other.hashCode());
    }

    @Test
    public void testHashCodeIgnoresNullContribution() {
        ConcurrentSet<String> cs = new ConcurrentSet<>(Arrays.asList(null));
        // hashCode should be 0 because null contributes 0
        assertEquals(0, cs.hashCode());
    }

    /* ==================== toString() Tests ==================== */

    @Test
    public void testToStringEmpty() {
        assertEquals("{}", emptySet.toString());
    }

    @Test
    public void testToStringNormalElements() {
        String s = setWithNull.toString();
        // Order is not guaranteed; verify that all expected tokens appear
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        assertTrue(s.contains("a"));
        assertTrue(s.contains("b"));
        assertTrue(s.contains("null"));
    }

    @Test
    public void testToStringSelfReference() {
        ConcurrentSet<Object> cs = new ConcurrentSet<>();
        cs.add(cs); // add the set itself
        assertEquals("{(this Set)}", cs.toString());
    }

    /* ==================== size() & isEmpty() Tests ==================== */

    @Test
    public void testSizeAndIsEmpty() {
        assertTrue(emptySet.isEmpty());
        assertEquals(0, emptySet.size());

        assertFalse(setWithNull.isEmpty());
        assertEquals(3, setWithNull.size());
    }

    /* ==================== contains() Tests ==================== */

    @Test
    public void testContainsExistingAndNull() {
        assertTrue(setWithNull.contains("a"));
        assertTrue(setWithNull.contains(null));
        assertFalse(setWithNull.contains("c"));
    }

    /* ==================== iterator() Tests ==================== */

    @Test
    public void testIteratorTraversesAllElementsIncludingNull() {
        Set<String> collected = new HashSet<>();
        for (String s : setWithNull) {
            collected.add(s);
        }
        assertEquals(3, collected.size());
        assertTrue(collected.contains("a"));
        assertTrue(collected.contains("b"));
        assertTrue(collected.contains(null));
    }

    @Test
    public void testIteratorRemoveWorks() {
        Iterator<String> it = setWithNull.iterator();
        while (it.hasNext()) {
            String val = it.next();
            if ("a".equals(val)) {
                it.remove();
            }
        }
        assertFalse(setWithNull.contains("a"));
        assertEquals(2, setWithNull.size());
    }

    /* ==================== toArray() Tests ==================== */

    @Test
    public void testToArrayReturnsObjectArrayWithNulls() {
        Object[] arr = setWithNull.toArray();
        List<Object> list = Arrays.asList(arr);
        assertEquals(3, arr.length);
        assertTrue(list.contains("a"));
        assertTrue(list.contains("b"));
        assertTrue(list.contains(null));
    }

    @Test
    public void testToArrayTypedArraySmallerThanSet() {
        String[] small = new String[1];
        String[] result = setWithNull.toArray(small);
        // Should be a new array of size 3
        assertEquals(3, result.length);
        List<String> list = Arrays.asList(result);
        assertTrue(list.contains("a"));
        assertTrue(list.contains("b"));
        assertTrue(list.contains(null));
    }

    @Test
    public void testToArrayTypedArrayLargerThanSet() {
        String[] big = new String[5];
        big[3] = "preserve";
        big[4] = "extra";
        String[] result = setWithNull.toArray(big);
        // The returned array should be the same instance (big)
        assertSame(big, result);
        // First three entries should be the set elements (order unspecified)
        List<String> firstThree = Arrays.asList(result[0], result[1], result[2]);
        assertTrue(firstThree.contains("a"));
        assertTrue(firstThree.contains("b"));
        assertTrue(firstThree.contains(null));
        // The element after the last should be null per spec
        assertNull(result[3]);
        // The remaining element should stay unchanged
        assertEquals("extra", result[4]);
    }

    /* ==================== containsAll() Tests ==================== */

    @Test
    public void testContainsAllTrue() {
        assertTrue(setWithNull.containsAll(Arrays.asList("a", null)));
    }

    @Test
    public void testContainsAllFalse() {
        assertFalse(setWithNull.containsAll(Arrays.asList("a", "z")));
    }

    /* ==================== add() Tests ==================== */

    @Test
    public void testAddNewElementReturnsTrue() {
        assertTrue(emptySet.add("x"));
        assertTrue(emptySet.contains("x"));
    }

    @Test
    public void testAddDuplicateReturnsFalse() {
        assertTrue(intSet.add(4));
        assertFalse(intSet.add(4)); // duplicate
    }

    @Test
    public void testAddNullElement() {
        assertTrue(emptySet.add(null));
        assertTrue(emptySet.contains(null));
        // Adding null again should return false
        assertFalse(emptySet.add(null));
    }

    /* ==================== remove() Tests ==================== */

    @Test
    public void testRemoveExistingElement() {
        assertTrue(setWithNull.remove("a"));
        assertFalse(setWithNull.contains("a"));
    }

    @Test
    public void testRemoveNonExistingElement() {
        assertFalse(setWithNull.remove("z"));
    }

    @Test
    public void testRemoveNullElement() {
        assertTrue(setWithNull.remove(null));
        assertFalse(setWithNull.contains(null));
    }

    /* ==================== addAll() Tests ==================== */

    @Test
    public void testAddAllAddsNewElementsOnly() {
        List<String> toAdd = Arrays.asList("b", "c", null);
        assertTrue(setWithNull.addAll(toAdd)); // "c" is new, null already present
        assertEquals(4, setWithNull.size());
        assertTrue(setWithNull.contains("c"));
    }

    @Test
    public void testAddAllNoChangeReturnsFalse() {
        List<String> same = Arrays.asList("a", "b", null);
        assertFalse(setWithNull.addAll(same));
    }

    /* ==================== removeAll() Tests ==================== */

    @Test
    public void testRemoveAllRemovesExistingElements() {
        List<String> toRemove = Arrays.asList("a", "nonexistent", null);
        assertTrue(setWithNull.removeAll(toRemove));
        assertFalse(setWithNull.contains("a"));
        assertFalse(setWithNull.contains(null));
        assertEquals(1, setWithNull.size()); // only "b" remains
    }

    @Test
    public void testRemoveAllNoChangeReturnsFalse() {
        List<String> none = Arrays.asList("x", "y");
        assertFalse(setWithNull.removeAll(none));
    }

    /* ==================== retainAll() Tests ==================== */

    @Test
    public void testRetainAllKeepsIntersection() {
        List<String> keep = Arrays.asList("a", null, "z");
        assertTrue(setWithNull.retainAll(keep));
        assertEquals(2, setWithNull.size());
        assertTrue(setWithNull.contains("a"));
        assertTrue(setWithNull.contains(null));
        assertFalse(setWithNull.contains("b"));
    }

    @Test
    public void testRetainAllNoChangeReturnsFalse() {
        List<String> all = Arrays.asList("a", "b", null);
        assertFalse(setWithNull.retainAll(all));
    }

    /* ==================== clear() Tests ==================== */

    @Test
    public void testClearEmptiesSet() {
        setWithNull.clear();
        assertTrue(setWithNull.isEmpty());
        assertEquals(0, setWithNull.size());
    }

    /* ==================== Additional Edge Cases ==================== */

    @Test
    public void testEqualityWithDifferentImplementationSetContainingSelfReference() {
        // Create a normal HashSet that contains a reference to the ConcurrentSet instance
        ConcurrentSet<Object> cs = new ConcurrentSet<>();
        Set<Object> other = new HashSet<>();
        cs.add("x");
        other.add(cs); // add the ConcurrentSet as an element
        other.add("x");
        // cs does not contain itself, so equals should be false
        assertFalse(cs.equals(other));
    }

    @Test
    public void testConcurrentModificationDuringIteration() throws InterruptedException {
        // Simple concurrency test: modify the set from another thread while iterating
        ConcurrentSet<Integer> cs = new ConcurrentSet<>(Arrays.asList(1, 2, 3));
        Thread t = new Thread(() -> cs.add(4));
        t.start();
        t.join(); // ensure addition has occurred

        // Now iterate; should see the new element without ConcurrentModificationException
        Set<Integer> collected = new HashSet<>();
        for (Integer i : cs) {
            collected.add(i);
        }
        assertTrue(collected.containsAll(Arrays.asList(1,2,3,4)));
    }
}
