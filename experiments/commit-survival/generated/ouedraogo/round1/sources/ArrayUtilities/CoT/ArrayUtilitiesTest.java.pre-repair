package com.cedarsoftware.util;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.*;

public class ArrayUtilitiesTest {

    /* ---------- isEmpty ---------- */
    @Test
    public void testIsEmpty_basic() {
        assertTrue(ArrayUtilities.isEmpty(null));
        assertTrue(ArrayUtilities.isEmpty(new int[0]));
        assertFalse(ArrayUtilities.isEmpty(new String[]{"a"}));
    }

    @Test
    public void testIsEmpty_null() {
        assertTrue(ArrayUtilities.isEmpty(null));
    }

    @Test
    public void testIsEmpty_emptyPrimitiveArray() {
        assertTrue(ArrayUtilities.isEmpty(new int[0]));
    }

    @Test
    public void testIsEmpty_nonEmptyPrimitiveArray() {
        assertFalse(ArrayUtilities.isEmpty(new double[]{1.0}));
    }

    /* ---------- size ---------- */
    @Test
    public void testSize_basic() {
        assertEquals(0, ArrayUtilities.size(null));
        assertEquals(0, ArrayUtilities.size(new double[0]));
        assertEquals(3, ArrayUtilities.size(new Integer[]{1,2,3}));
    }

    @Test
    public void testSize_null() {
        assertEquals(0, ArrayUtilities.size(null));
    }

    @Test
    public void testSize_emptyArray() {
        assertEquals(0, ArrayUtilities.size(new char[0]));
    }

    @Test
    public void testSize_nonEmptyArray() {
        assertEquals(2, ArrayUtilities.size(new Long[]{10L,20L}));
    }

    /* ---------- shallowCopy ---------- */
    @Test
    public void testShallowCopy_basic() {
        String[] src = {"x","y"};
        String[] copy = ArrayUtilities.shallowCopy(src);
        assertArrayEquals(src, copy);
        assertNotSame(src, copy);
    }

    @Test
    public void testShallowCopy_null() {
        assertNull(ArrayUtilities.shallowCopy(null));
    }

    @Test
    public void testShallowCopy_emptyArray() {
        Integer[] empty = new Integer[0];
        Integer[] copy = ArrayUtilities.shallowCopy(empty);
        assertNotSame(empty, copy);
        assertArrayEquals(empty, copy);
    }

    @Test
    public void testShallowCopy_mutableElements() {
        StringBuilder sb = new StringBuilder("a");
        StringBuilder[] src = {sb};
        StringBuilder[] copy = ArrayUtilities.shallowCopy(src);
        assertNotSame(src, copy);
        assertSame(src[0], copy[0]); // shallow copy – same element reference
        sb.append("b");
        assertEquals("ab", copy[0].toString());
    }

    /* ---------- createArray ---------- */
    @Test
    public void testCreateArray_basic() {
        Integer[] a = ArrayUtilities.createArray(1,2,3);
        assertArrayEquals(new Integer[]{1,2,3}, a);
    }

    @Test
    public void testCreateArray_noArguments() {
        String[] arr = ArrayUtilities.createArray();
        assertNotNull(arr);
        assertEquals(0, arr.length);
    }

    @Test(expected = NullPointerException.class)
    public void testCreateArray_nullVarargsArray() {
        ArrayUtilities.createArray((String[]) null);
    }

    /* ---------- addAll ---------- */
    @Test
    public void testAddAll_basic() {
        String[] a = {"a","b"};
        String[] b = {"c"};
        String[] res = ArrayUtilities.addAll(a,b);
        assertArrayEquals(new String[]{"a","b","c"}, res);
    }

    @Test
    public void testAddAll_bothNull() {
        assertNull(ArrayUtilities.addAll(null, null));
    }

    @Test
    public void testAddAll_firstNullSecondNonNull() {
        Integer[] second = {1,2};
        Integer[] result = ArrayUtilities.addAll(null, second);
        assertArrayEquals(second, result);
        assertNotSame(second, result);
    }

    @Test
    public void testAddAll_secondNullFirstNonNull() {
        String[] first = {"x"};
        String[] result = ArrayUtilities.addAll(first, null);
        assertArrayEquals(first, result);
        assertNotSame(first, result);
    }

    @Test
    public void testAddAll_oneEmpty_oneNonEmpty() {
        String[] empty = new String[0];
        String[] nonEmpty = {"a","b"};
        String[] result = ArrayUtilities.addAll(empty, nonEmpty);
        assertArrayEquals(nonEmpty, result);
        assertNotSame(nonEmpty, result);
    }

    /* ---------- removeItem ---------- */
    @Test
    public void testRemoveItem_basic() {
        Integer[] src = {10,20,30,40};
        Integer[] dst = ArrayUtilities.removeItem(src, 2); // remove 30
        assertArrayEquals(new Integer[]{10,20,40}, dst);
    }

    @Test
    public void testRemoveItem_removeFirst() {
        Integer[] src = {1,2,3};
        Integer[] dst = ArrayUtilities.removeItem(src, 0);
        assertArrayEquals(new Integer[]{2,3}, dst);
    }

    @Test
    public void testRemoveItem_removeLast() {
        Integer[] src = {1,2,3};
        Integer[] dst = ArrayUtilities.removeItem(src, 2);
        assertArrayEquals(new Integer[]{1,2}, dst);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testRemoveItem_invalidNegativePos() {
        Integer[] src = {1,2,3};
        ArrayUtilities.removeItem(src, -1);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testRemoveItem_invalidPosTooLarge() {
        Integer[] src = {1,2,3};
        ArrayUtilities.removeItem(src, 3);
    }

    /* ---------- getArraySubset ---------- */
    @Test
    public void testGetArraySubset_basic() {
        String[] src = {"a","b","c","d"};
        String[] sub = ArrayUtilities.getArraySubset(src, 1, 3);
        assertArrayEquals(new String[]{"b","c"}, sub);
    }

    @Test
    public void testGetArraySubset_startEqualsEnd() {
        String[] src = {"a","b"};
        String[] sub = ArrayUtilities.getArraySubset(src, 1, 1);
        assertEquals(0, sub.length);
    }

    @Test
    public void testGetArraySubset_fullRange() {
        Integer[] src = {5,6,7};
        Integer[] sub = ArrayUtilities.getArraySubset(src, 0, src.length);
        assertArrayEquals(src, sub);
        assertNotSame(src, sub);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetArraySubset_negativeStart() {
        Integer[] src = {1,2,3};
        ArrayUtilities.getArraySubset(src, -1, 2);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetArraySubset_endBeyondLength() {
        Integer[] src = {1,2,3};
        ArrayUtilities.getArraySubset(src, 1, 5);
    }

    /* ---------- toArray ---------- */
    @Test
    public void testToArray_basic() {
        List<String> list = Arrays.asList("x","y");
        String[] arr = ArrayUtilities.toArray(String.class, list);
        assertArrayEquals(new String[]{"x","y"}, arr);
    }

    @Test
    public void testToArray_emptyCollection() {
        Collection<String> empty = Collections.emptyList();
        String[] arr = ArrayUtilities.toArray(String.class, empty);
        assertNotNull(arr);
        assertEquals(0, arr.length);
    }

    @Test(expected = NullPointerException.class)
    public void testToArray_nullClass() {
        List<Integer> list = Arrays.asList(1,2);
        ArrayUtilities.toArray(null, list);
    }

    @Test(expected = NullPointerException.class)
    public void testToArray_nullCollection() {
        ArrayUtilities.toArray(Integer.class, null);
    }
}
