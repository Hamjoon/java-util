package com.cedarsoftware.util;

import static org.junit.Assert.*;

import org.junit.Test;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Comprehensive JUnit 4 test suite for {@link ArrayUtilities}.
 */
public class ArrayUtilitiesTest {

    /* -------------------------------------------------------
     *  isEmpty() tests
     * ------------------------------------------------------- */
    @Test
    public void testIsEmpty_NullArray() {
        assertTrue(ArrayUtilities.isEmpty(null));
    }

    @Test
    public void testIsEmpty_EmptyObjectArray() {
        Object[] empty = new Object[0];
        assertTrue(ArrayUtilities.isEmpty(empty));
    }

    @Test
    public void testIsEmpty_NonEmptyObjectArray() {
        String[] arr = {"a", "b"};
        assertFalse(ArrayUtilities.isEmpty(arr));
    }

    @Test
    public void testIsEmpty_EmptyPrimitiveArray() {
        int[] empty = new int[0];
        assertTrue(ArrayUtilities.isEmpty(empty));
    }

    @Test
    public void testIsEmpty_NonEmptyPrimitiveArray() {
        double[] arr = {1.0, 2.0};
        assertFalse(ArrayUtilities.isEmpty(arr));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsEmpty_NonArrayObjectThrows() {
        ArrayUtilities.isEmpty("not an array");
    }

    /* -------------------------------------------------------
     *  size() tests
     * ------------------------------------------------------- */
    @Test
    public void testSize_NullArray() {
        assertEquals(0, ArrayUtilities.size(null));
    }

    @Test
    public void testSize_EmptyObjectArray() {
        assertEquals(0, ArrayUtilities.size(new Object[0]));
    }

    @Test
    public void testSize_NonEmptyObjectArray() {
        Integer[] ints = {1, 2, 3};
        assertEquals(3, ArrayUtilities.size(ints));
    }

    @Test
    public void testSize_EmptyPrimitiveArray() {
        char[] empty = new char[0];
        assertEquals(0, ArrayUtilities.size(empty));
    }

    @Test
    public void testSize_NonEmptyPrimitiveArray() {
        long[] longs = {10L, 20L, 30L, 40L};
        assertEquals(4, ArrayUtilities.size(longs));
    }

    /* -------------------------------------------------------
     *  shallowCopy() tests
     * ------------------------------------------------------- */
    @Test
    public void testShallowCopy_NullReturnsNull() {
        assertNull(ArrayUtilities.shallowCopy(null));
    }

    @Test
    public void testShallowCopy_ClonesArray() {
        String[] original = {"x", "y", "z"};
        String[] copy = ArrayUtilities.shallowCopy(original);
        assertNotSame("shallowCopy must return a new array instance", original, copy);
        assertArrayEquals("contents must be equal", original, copy);
    }

    @Test
    public void testShallowCopy_EmptyArray() {
        Integer[] empty = new Integer[0];
        Integer[] copy = ArrayUtilities.shallowCopy(empty);
        assertNotSame(empty, copy);
        assertEquals(0, copy.length);
    }

    /* -------------------------------------------------------
     *  createArray() tests
     * ------------------------------------------------------- */
    @Test
    public void testCreateArray_NoElements() {
        String[] arr = ArrayUtilities.createArray();
        assertNotNull(arr);
        assertEquals(0, arr.length);
    }

    @Test
    public void testCreateArray_WithElements() {
        String[] arr = ArrayUtilities.createArray("a", "b", "c");
        assertArrayEquals(new String[]{"a", "b", "c"}, arr);
    }

    @Test
    public void testCreateArray_WithNullElements() {
        Integer[] arr = ArrayUtilities.createArray(1, null, 3);
        assertEquals(3, arr.length);
        assertEquals(Integer.valueOf(1), arr[0]);
        assertNull(arr[1]);
        assertEquals(Integer.valueOf(3), arr[2]);
    }

    @Test
    public void testCreateArray_TypeInference() {
        // The compiler infers String[] here
        String[] arr = ArrayUtilities.createArray("one", "two");
        assertTrue(arr instanceof String[]);
    }

    /* -------------------------------------------------------
     *  addAll() tests
     * ------------------------------------------------------- */
    @Test
    public void testAddAll_BothNullReturnsNull() {
        assertNull(ArrayUtilities.addAll(null, null));
    }

    @Test
    public void testAddAll_FirstNullReturnsCopyOfSecond() {
        String[] second = {"s1", "s2"};
        String[] result = ArrayUtilities.addAll(null, second);
        assertNotSame(second, result);
        assertArrayEquals(second, result);
    }

    @Test
    public void testAddAll_SecondNullReturnsCopyOfFirst() {
        Integer[] first = {10, 20, 30};
        Integer[] result = ArrayUtilities.addAll(first, null);
        assertNotSame(first, result);
        assertArrayEquals(first, result);
    }

    @Test
    public void testAddAll_BothEmpty() {
        String[] a = new String[0];
        String[] b = new String[0];
        String[] result = ArrayUtilities.addAll(a, b);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void testAddAll_ConcatArrays() {
        String[] a = {"a", "b", "c"};
        String[] b = {"1", "2", "3"};
        String[] result = ArrayUtilities.addAll(a, b);
        assertArrayEquals(new String[]{"a", "b", "c", "1", "2", "3"}, result);
    }

    @Test
    public void testAddAll_WithNullElements() {
        String[] a = {null, "x"};
        String[] b = {"y", null};
        String[] result = ArrayUtilities.addAll(a, b);
        assertArrayEquals(new String[]{null, "x", "y", null}, result);
    }

    /* -------------------------------------------------------
     *  removeItem() tests
     * ------------------------------------------------------- */
    @Test
    public void testRemoveItem_RemoveMiddle() {
        String[] src = {"a", "b", "c", "d"};
        String[] result = ArrayUtilities.removeItem(src, 2); // remove "c"
        assertArrayEquals(new String[]{"a", "b", "d"}, result);
    }

    @Test
    public void testRemoveItem_RemoveFirst() {
        Integer[] src = {1, 2, 3};
        Integer[] result = ArrayUtilities.removeItem(src, 0);
        assertArrayEquals(new Integer[]{2, 3}, result);
    }

    @Test
    public void testRemoveItem_RemoveLast() {
        Character[] src = {'x', 'y', 'z'};
        Character[] result = ArrayUtilities.removeItem(src, 2);
        assertArrayEquals(new Character[]{'x', 'y'}, result);
    }

    @Test
    public void testRemoveItem_SingleElementArray() {
        Double[] src = {9.9};
        Double[] result = ArrayUtilities.removeItem(src, 0);
        assertEquals(0, result.length);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testRemoveItem_InvalidIndexThrows() {
        String[] src = {"only"};
        ArrayUtilities.removeItem(src, 5);
    }

    /* -------------------------------------------------------
     *  getArraySubset() tests
     * ------------------------------------------------------- */
    @Test
    public void testGetArraySubset_FullRange() {
        Integer[] src = {1, 2, 3, 4, 5};
        Integer[] sub = ArrayUtilities.getArraySubset(src, 0, src.length);
        assertArrayEquals(src, sub);
        assertNotSame(src, sub);
    }

    @Test
    public void testGetArraySubset_PartialRange() {
        String[] src = {"a", "b", "c", "d"};
        String[] sub = ArrayUtilities.getArraySubset(src, 1, 3); // "b","c"
        assertArrayEquals(new String[]{"b", "c"}, sub);
    }

    @Test
    public void testGetArraySubset_EmptyRange() {
        String[] src = {"x", "y"};
        String[] sub = ArrayUtilities.getArraySubset(src, 1, 1);
        assertEquals(0, sub.length);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetArraySubset_InvalidRangeThrows() {
        String[] src = {"a", "b"};
        ArrayUtilities.getArraySubset(src, -1, 3);
    }

    /* -------------------------------------------------------
     *  toArray() tests
     * ------------------------------------------------------- */
    @Test
    public void testToArray_StandardCollection() {
        List<String> list = new ArrayList<>();
        list.add("one");
        list.add("two");
        String[] arr = ArrayUtilities.toArray(String.class, list);
        assertArrayEquals(new String[]{"one", "two"}, arr);
        assertTrue(arr.getClass() == String[].class);
    }

    @Test
    public void testToArray_EmptyCollection() {
        Collection<Integer> empty = new ArrayList<>();
        Integer[] arr = ArrayUtilities.toArray(Integer.class, empty);
        assertNotNull(arr);
        assertEquals(0, arr.length);
        assertTrue(arr.getClass() == Integer[].class);
    }

    @Test
    public void testToArray_WithNullElementsInCollection() {
        List<String> list = new ArrayList<>();
        list.add(null);
        list.add("value");
        String[] arr = ArrayUtilities.toArray(String.class, list);
        assertArrayEquals(new String[]{null, "value"}, arr);
    }

    @Test(expected = NullPointerException.class)
    public void testToArray_NullCollectionThrows() {
        ArrayUtilities.toArray(Object.class, null);
    }

    @Test
    public void testToArray_TypeMismatchHandledByArraySet() {
        // The collection holds Numbers, we ask for Integer[]. The internal
        // Array.set will throw an ArrayStoreException if a non‑Integer is stored.
        Collection<Number> numbers = new ArrayList<>();
        numbers.add(1);          // Integer, OK
        numbers.add(2L);         // Long, should cause exception when set
        try {
            ArrayUtilities.toArray(Integer.class, numbers);
            fail("Expected ArrayStoreException");
        } catch (ArrayStoreException expected) {
            // expected path
        }
    }

    /* -------------------------------------------------------
     *  Constants sanity checks (optional)
     * ------------------------------------------------------- */
    @Test
    public void testEmptyArraysConstants() {
        assertEquals(0, ArrayUtilities.EMPTY_OBJECT_ARRAY.length);
        assertEquals(0, ArrayUtilities.EMPTY_BYTE_ARRAY.length);
        assertEquals(0, ArrayUtilities.EMPTY_CHAR_ARRAY.length);
        assertEquals(0, ArrayUtilities.EMPTY_CHARACTER_ARRAY.length);
        assertEquals(0, ArrayUtilities.EMPTY_CLASS_ARRAY.length);
    }
}
