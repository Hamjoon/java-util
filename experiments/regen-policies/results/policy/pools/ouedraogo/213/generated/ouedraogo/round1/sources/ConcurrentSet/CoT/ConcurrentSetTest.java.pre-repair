package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.util.*;

import static org.junit.Assert.*;

public class ConcurrentSetTest {

    // ---------- Basic functionality tests ----------
    @Test
    public void testDefaultConstructorIsEmpty() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    public void testConstructorFromCollection() {
        List<String> src = Arrays.asList("a", "b", null);
        ConcurrentSet<String> set = new ConcurrentSet<>(src);
        assertEquals(3, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertTrue(set.contains(null));
    }

    @Test
    public void testConstructorFromSet() {
        Set<Integer> src = new HashSet<>(Arrays.asList(1, 2, 3));
        ConcurrentSet<Integer> set = new ConcurrentSet<>(src);
        assertEquals(3, set.size());
        assertTrue(set.containsAll(src));
    }

    @Test
    public void testAddAndContains() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        assertTrue(set.add(10));
        assertTrue(set.contains(10));
        assertFalse(set.add(10)); // duplicate
        assertEquals(1, set.size());
    }

    @Test
    public void testAddNull() {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        assertTrue(set.add(null));
        assertTrue(set.contains(null));
        assertEquals(1, set.size());
    }

    @Test
    public void testRemove() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        set.add(5);
        assertTrue(set.remove(5));
        assertFalse(set.contains(5));
        assertTrue(set.isEmpty());
    }

    @Test
    public void testRemoveNull() {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.add(null);
        assertTrue(set.remove(null));
        assertFalse(set.contains(null));
        assertTrue(set.isEmpty());
    }

    @Test
    public void testAddAll() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        List<Integer> src = Arrays.asList(1, 2, 3);
        assertTrue(set.addAll(src));
        assertEquals(3, set.size());
        assertTrue(set.containsAll(src));
    }

    @Test
    public void testRemoveAll() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>(Arrays.asList(1, 2, 3, 4));
        List<Integer> toRemove = Arrays.asList(2, 4);
        assertTrue(set.removeAll(toRemove));
        assertEquals(2, set.size());
        assertFalse(set.contains(2));
        assertFalse(set.contains(4));
    }

    @Test
    public void testRetainAll() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>(Arrays.asList(1, 2, 3, 4));
        List<Integer> toKeep = Arrays.asList(2, 3);
        assertTrue(set.retainAll(toKeep));
        assertEquals(2, set.size());
        assertTrue(set.containsAll(toKeep));
        assertFalse(set.contains(1));
        assertFalse(set.contains(4));
    }

    @Test
    public void testClear() {
        ConcurrentSet<String> set = new ConcurrentSet<>(Arrays.asList("x", "y"));
        set.clear();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    public void testIteratorAndRemoveViaIterator() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>(Arrays.asList(1, 2, 3));
        Iterator<Integer> it = set.iterator();
        while (it.hasNext()) {
            Integer v = it.next();
            if (v == 2) {
                it.remove();
            }
        }
        assertFalse(set.contains(2));
        assertEquals(2, set.size());
    }

    @Test
    public void testToArrayObject() {
        ConcurrentSet<String> set = new ConcurrentSet<>(Arrays.asList("a", null, "c"));
        Object[] arr = set.toArray();
        List<Object> list = Arrays.asList(arr);
        assertTrue(list.contains("a"));
        assertTrue(list.contains("c"));
        assertTrue(list.contains(null));
        assertEquals(3, arr.length);
    }

    @Test
    public void testToArrayTypedSmallerArray() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>(Arrays.asList(1, 2, 3));
        Integer[] a = new Integer[2];
        Integer[] result = set.toArray(a);
        assertEquals(3, result.length);
        assertArrayEquals(new Integer[]{1, 2, 3}, result);
    }

    @Test
    public void testToArrayTypedLargerArray() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>(Arrays.asList(7, 8));
        Integer[] a = new Integer[5];
        Integer[] result = set.toArray(a);
        assertSame(a, result);
        assertEquals(Integer.valueOf(7), result[0]);
        assertEquals(Integer.valueOf(8), result[1]);
        assertNull(result[2]); // first extra slot must be null
    }

    // ---------- Edge‑case & exception tests ----------
    @Test(expected = ClassCastException.class)
    public void testContainsIncompatibleTypeThrows() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        set.add(1);
        set.contains("not-an-integer");
    }

    @Test(expected = ClassCastException.class)
    public void testRemoveIncompatibleTypeThrows() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        set.add(42);
        set.remove("string");
    }

    @Test
    public void testAddDuplicateReturnsFalse() {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        assertTrue(set.add("dup"));
        assertFalse(set.add("dup"));
        assertEquals(1, set.size());
    }

    @Test
    public void testAddMultipleNulls() {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        assertTrue(set.add(null));
        assertFalse(set.add(null));
        assertEquals(1, set.size());
        assertTrue(set.contains(null));
    }

    @Test
    public void testRetainAllWithNulls() {
        ConcurrentSet<String> set = new ConcurrentSet<>(Arrays.asList("a", null, "b"));
        List<String> keep = Arrays.asList(null, "b", "c");
        assertTrue(set.retainAll(keep));
        assertEquals(2, set.size());
        assertTrue(set.contains(null));
        assertTrue(set.contains("b"));
        assertFalse(set.contains("a"));
    }

    @Test(expected = NullPointerException.class)
    public void testToArrayTypedNullArrayThrows() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>(Arrays.asList(1, 2));
        set.toArray((Integer[]) null);
    }

    @Test(expected = ArrayStoreException.class)
    public void testToArrayTypedIncompatibleComponentTypeThrows() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>(Arrays.asList(1, 2));
        String[] a = new String[2];
        set.toArray(a);
    }

    @Test
    public void testEqualsWithNonSetReturnsFalse() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>(Arrays.asList(1, 2));
        assertFalse(set.equals("not a set"));
    }

    @Test
    public void testEqualsDifferentSizeReturnsFalse() {
        ConcurrentSet<Integer> set1 = new ConcurrentSet<>(Arrays.asList(1, 2, 3));
        ConcurrentSet<Integer> set2 = new ConcurrentSet<>(Arrays.asList(1, 2));
        assertFalse(set1.equals(set2));
    }

    @Test
    public void testEqualsWithIncompatibleElementTypesReturnsFalse() {
        ConcurrentSet<Integer> intSet = new ConcurrentSet<>(Arrays.asList(1, 2));
        Set<String> strSet = new HashSet<>(Arrays.asList("1", "2"));
        assertFalse(intSet.equals(strSet));
    }

    @Test
    public void testHashCodeWithNulls() {
        ConcurrentSet<String> set = new ConcurrentSet<>(Arrays.asList(null, "x"));
        int expected = 0 + "x".hashCode();
        assertEquals(expected, set.hashCode());
    }

    @Test(expected = IllegalStateException.class)
    public void testIteratorRemoveBeforeNextThrows() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>(Arrays.asList(1));
        Iterator<Integer> it = set.iterator();
        it.remove(); // illegal state
    }

    @Test
    public void testClearOnEmptySet() {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.clear(); // should not throw
        assertTrue(set.isEmpty());
    }
}
