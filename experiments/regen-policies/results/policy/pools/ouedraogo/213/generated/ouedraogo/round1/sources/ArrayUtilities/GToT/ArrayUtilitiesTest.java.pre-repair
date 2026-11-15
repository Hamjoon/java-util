package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class ArrayUtilitiesTest {

    /* --------------------------------------------------------------------- *
     *  isEmpty(Object)                                                       *
     * --------------------------------------------------------------------- */

    @Test
    public void testIsEmpty_NullArray() {
        assertTrue(ArrayUtilities.isEmpty(null));
    }

    @Test
    public void testIsEmpty_EmptyPrimitiveArray() {
        int[] empty = new int[0];
        assertTrue(ArrayUtilities.isEmpty(empty));
    }

    @Test
    public void testIsEmpty_NonEmptyObjectArray() {
        String[] data = {"a", "b"};
        assertFalse(ArrayUtilities.isEmpty(data));
    }

    @Test
    public void testIsEmpty_ArrayWithNullElements() {
        Object[] data = {null, null};
        assertFalse(ArrayUtilities.isEmpty(data));
    }

    /* --------------------------------------------------------------------- *
     *  size(Object)                                                          *
     * --------------------------------------------------------------------- */

    @Test
    public void testSize_NullArray() {
        assertEquals(0, ArrayUtilities.size(null));
    }

    @Test
    public void testSize_EmptyArray() {
        String[] empty = new String[0];
        assertEquals(0, ArrayUtilities.size(empty));
    }

    @Test
    public void testSize_NonEmptyArray() {
        double[] numbers = {1.0, 2.0, 3.0};
        assertEquals(3, ArrayUtilities.size(numbers));
    }

    /* --------------------------------------------------------------------- *
     *  shallowCopy(T[])                                                      *
     * --------------------------------------------------------------------- */

    @Test
    public void testShallowCopy_Null() {
        assertNull(ArrayUtilities.shallowCopy(null));
    }

    @Test
    public void testShallowCopy_EmptyArray() {
        Integer[] empty = new Integer[0];
        Integer[] copy = ArrayUtilities.shallowCopy(empty);
        assertNotSame(empty, copy);
        assertArrayEquals(empty, copy);
    }

    @Test
    public void testShallowCopy_ModifyOriginalDoesNotAffectCopy() {
        String[] original = {"x", "y"};
        String[] copy = ArrayUtilities.shallowCopy(original);
        original[0] = "changed";
        // shallow copy copies references, not deep clone, so element objects stay same,
        // but the array reference is distinct – the changed element is visible in copy.
        assertEquals("changed", copy[0]);
        // Ensure the array objects themselves are different instances
        assertNotSame(original, copy);
    }

    /* --------------------------------------------------------------------- *
     *  createArray(T...)                                                     *
     * --------------------------------------------------------------------- */

    @Test
    public void testCreateArray_VariousTypes() {
        String[] strings = ArrayUtilities.createArray("a", "b", "c");
        assertArrayEquals(new String[]{"a", "b", "c"}, strings);

        Integer[] ints = ArrayUtilities.createArray(1, 2, 3, 4);
        assertArrayEquals(new Integer[]{1, 2, 3, 4}, ints);
    }

    @Test
    public void testCreateArray_Empty() {
        Object[] empty = ArrayUtilities.createArray();
        assertEquals(0, empty.length);
    }

    @Test(expected = NullPointerException.class)
    public void testCreateArray_NullVarargsArray() {
        // Force a null varargs array by casting null to an Object[]
        ArrayUtilities.createArray((Object[]) null);
    }

    @Test
    public void testCreateArray_WithNullElements() {
        String[] withNull = ArrayUtilities.createArray("a", null, "c");
        assertArrayEquals(new String[]{"a", null, "c"}, withNull);
    }

    /* --------------------------------------------------------------------- *
     *  addAll(T[], T[])                                                      *
     * --------------------------------------------------------------------- */

    @Test
    public void testAddAll_BothNull() {
        assertNull(ArrayUtilities.addAll(null, null));
    }

    @Test
    public void testAddAll_FirstNull() {
        Integer[] second = {1, 2, 3};
        Integer[] result = ArrayUtilities.addAll(null, second);
        assertArrayEquals(second, result);
        assertNotSame(second, result); // must be a shallow copy
    }

    @Test
    public void testAddAll_SecondNull() {
        String[] first = {"a", "b"};
        String[] result = ArrayUtilities.addAll(first, null);
        assertArrayEquals(first, result);
        assertNotSame(first, result);
    }

    @Test
    public void testAddAll_BothEmpty() {
        Integer[] empty1 = new Integer[0];
        Integer[] empty2 = new Integer[0];
        Integer[] result = ArrayUtilities.addAll(empty1, empty2);
        assertArrayEquals(new Integer[0], result);
    }

    @Test
    public void testAddAll_WithNullElements() {
        String[] a = {null};
        String[] b = {null};
        String[] result = ArrayUtilities.addAll(a, b);
        assertArrayEquals(new String[]{null, null}, result);
    }

    @Test
    public void testAddAll_TypicalConcat() {
        String[] a = {"a", "b", "c"};
        String[] b = {"1", "2", "3"};
        String[] expected = {"a", "b", "c", "1", "2", "3"};
        assertArrayEquals(expected, ArrayUtilities.addAll(a, b));
    }

    /* --------------------------------------------------------------------- *
     *  removeItem(T[], int)                                                   *
     * --------------------------------------------------------------------- */

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testRemoveItem_MiddleElement() {
        Integer[] src = {1, 2, 3, 4, 5};
        Integer[] expected = {1, 2, 4, 5};
        assertArrayEquals(expected, ArrayUtilities.removeItem(src, 2));
    }

    @Test
    public void testRemoveItem_FirstElement() {
        String[] src = {"first", "second", "third"};
        String[] expected = {"second", "third"};
        assertArrayEquals(expected, ArrayUtilities.removeItem(src, 0));
    }

    @Test
    public void testRemoveItem_LastElement() {
        Character[] src = {'a', 'b', 'c'};
        Character[] expected = {'a', 'b'};
        assertArrayEquals(expected, ArrayUtilities.removeItem(src, 2));
    }

    @Test
    public void testRemoveItem_ArrayWithNulls() {
        Object[] src = {null, "x", null};
        Object[] expected = {"x", null};
        assertArrayEquals(expected, ArrayUtilities.removeItem(src, 0));
    }

    @Test
    public void testRemoveItem_NullArray() {
        thrown.expect(NullPointerException.class);
        ArrayUtilities.removeItem(null, 0);
    }

    @Test
    public void testRemoveItem_NegativeIndex() {
        thrown.expect(ArrayIndexOutOfBoundsException.class);
        ArrayUtilities.removeItem(new Integer[]{1, 2, 3}, -1);
    }

    @Test
    public void testRemoveItem_IndexEqualToLength() {
        thrown.expect(ArrayIndexOutOfBoundsException.class);
        ArrayUtilities.removeItem(new Integer[]{1, 2, 3}, 3);
    }

    /* --------------------------------------------------------------------- *
     *  getArraySubset(T[], int, int)                                          *
     * --------------------------------------------------------------------- */

    @Test
    public void testGetArraySubset_NormalRange() {
        String[] src = {"apple", "banana", "cherry", "date", "elderberry"};
        String[] expected = {"banana", "cherry", "date"};
        assertArrayEquals(expected, ArrayUtilities.getArraySubset(src, 1, 4));
    }

    @Test
    public void testGetArraySubset_EmptyResult() {
        Integer[] src = {1, 2, 3};
        Integer[] expected = new Integer[0];
        assertArrayEquals(expected, ArrayUtilities.getArraySubset(src, 2, 2));
    }

    @Test
    public void testGetArraySubset_StartZero() {
        int[] src = {10, 20, 30};
        int[] expected = {10, 20};
        assertArrayEquals(expected, ArrayUtilities.getArraySubset(src, 0, 2));
    }

    @Test
    public void testGetArraySubset_NullArray() {
        thrown.expect(NullPointerException.class);
        ArrayUtilities.getArraySubset(null, 0, 1);
    }

    @Test
    public void testGetArraySubset_NegativeStart() {
        thrown.expect(ArrayIndexOutOfBoundsException.class);
        ArrayUtilities.getArraySubset(new String[]{"a", "b"}, -1, 1);
    }

    @Test
    public void testGetArraySubset_EndBeyondLength() {
        thrown.expect(ArrayIndexOutOfBoundsException.class);
        ArrayUtilities.getArraySubset(new String[]{"a", "b"}, 0, 3);
    }

    @Test
    public void testGetArraySubset_StartGreaterThanEnd() {
        thrown.expect(ArrayIndexOutOfBoundsException.class);
        ArrayUtilities.getArraySubset(new String[]{"a", "b"}, 2, 1);
    }

    /* --------------------------------------------------------------------- *
     *  toArray(Class<T>, Collection<?>)                                       *
     * --------------------------------------------------------------------- */

    @Test
    public void testToArray_TypicalUsage() {
        List<String> list = List.of("x", "y", "z");
        String[] result = ArrayUtilities.toArray(String.class, list);
        assertArrayEquals(new String[]{"x", "y", "z"}, result);
    }

    @Test
    public void testToArray_EmptyCollection() {
        List<Integer> empty = Collections.emptyList();
        Integer[] result = ArrayUtilities.toArray(Integer.class, empty);
        assertEquals(0, result.length);
    }

    @Test
    public void testToArray_CollectionWithNullElements() {
        List<String> list = new ArrayList<>();
        list.add(null);
        list.add("a");
        String[] result = ArrayUtilities.toArray(String.class, list);
        assertArrayEquals(new String[]{null, "a"}, result);
    }

    @Test
    public void testToArray_IncompatibleElementType() {
        List<Object> list = new ArrayList<>();
        list.add(123); // Integer
        thrown.expect(IllegalArgumentException.class);
        // Attempt to cast to String[] – Array.set will reject the Integer value
        ArrayUtilities.toArray(String.class, list);
    }

    @Test
    public void testToArray_NullCollection() {
        thrown.expect(NullPointerException.class);
        ArrayUtilities.toArray(Object.class, null);
    }

    @Test
    public void testToArray_NullClassParameter() {
        List<String> list = List.of("a");
        thrown.expect(NullPointerException.class);
        // The method will attempt to call Array.newInstance with a null class -> NPE
        ArrayUtilities.toArray(null, list);
    }
}
