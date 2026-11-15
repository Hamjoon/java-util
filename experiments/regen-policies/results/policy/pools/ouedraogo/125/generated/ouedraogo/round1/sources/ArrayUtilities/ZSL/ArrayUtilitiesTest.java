package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Comprehensive JUnit 4 test suite for {@link ArrayUtilities}.
 */
public class ArrayUtilitiesTest {

    /* ---------------------------------------------------------------------- */
    /*  isEmpty / size                                                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIsEmpty_Null() {
        Assert.assertTrue(ArrayUtilities.isEmpty(null));
    }

    @Test
    public void testIsEmpty_EmptyObjectArray() {
        Object[] empty = new Object[0];
        Assert.assertTrue(ArrayUtilities.isEmpty(empty));
    }

    @Test
    public void testIsEmpty_NonEmptyObjectArray() {
        String[] arr = {"a"};
        Assert.assertFalse(ArrayUtilities.isEmpty(arr));
    }

    @Test
    public void testIsEmpty_EmptyPrimitiveArray() {
        int[] empty = new int[0];
        Assert.assertTrue(ArrayUtilities.isEmpty(empty));
    }

    @Test
    public void testIsEmpty_NonEmptyPrimitiveArray() {
        char[] arr = {'x', 'y'};
        Assert.assertFalse(ArrayUtilities.isEmpty(arr));
    }

    @Test
    public void testSize_Null() {
        Assert.assertEquals(0, ArrayUtilities.size(null));
    }

    @Test
    public void testSize_ObjectArray() {
        String[] arr = {"a", "b", "c"};
        Assert.assertEquals(3, ArrayUtilities.size(arr));
    }

    @Test
    public void testSize_PrimitiveArray() {
        double[] arr = {1.0, 2.0};
        Assert.assertEquals(2, ArrayUtilities.size(arr));
    }

    /* ---------------------------------------------------------------------- */
    /*  shallowCopy                                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testShallowCopy_Null() {
        Assert.assertNull(ArrayUtilities.shallowCopy(null));
    }

    @Test
    public void testShallowCopy_CloneIsSeparateInstance() {
        Integer[] original = {1, 2, 3};
        Integer[] copy = ArrayUtilities.shallowCopy(original);
        Assert.assertNotSame(original, copy);
        Assert.assertArrayEquals(original, copy);

        // Mutate original reference array to ensure copy is independent
        original[0] = 99;
        Assert.assertNotEquals(original[0], copy[0]);
    }

    /* ---------------------------------------------------------------------- */
    /*  createArray                                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCreateArray_Varargs() {
        String[] result = ArrayUtilities.createArray("one", "two", "three");
        Assert.assertArrayEquals(new String[]{"one", "two", "three"}, result);
    }

    @Test
    public void testCreateArray_WithNullElements() {
        Integer[] result = ArrayUtilities.createArray(1, null, 3);
        Assert.assertEquals(3, result.length);
        Assert.assertEquals(Integer.valueOf(1), result[0]);
        Assert.assertNull(result[1]);
        Assert.assertEquals(Integer.valueOf(3), result[2]);
    }

    @Test(expected = NullPointerException.class)
    public void testCreateArray_NullArrayParameter() {
        // Explicitly pass a null array as the varargs container.
        // This triggers the NPE as documented.
        ArrayUtilities.createArray((String[]) null);
    }

    /* ---------------------------------------------------------------------- */
    /*  addAll                                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAddAll_BothNull() {
        Assert.assertNull(ArrayUtilities.addAll(null, null));
    }

    @Test
    public void testAddAll_FirstNull_SecondNonNull() {
        String[] second = {"x", "y"};
        String[] result = ArrayUtilities.addAll(null, second);
        Assert.assertNotSame(second, result); // shallow copy => new instance
        Assert.assertArrayEquals(second, result);
    }

    @Test
    public void testAddAll_SecondNull_FirstNonNull() {
        Integer[] first = {1, 2, 3};
        Integer[] result = ArrayUtilities.addAll(first, null);
        Assert.assertNotSame(first, result);
        Assert.assertArrayEquals(first, result);
    }

    @Test
    public void testAddAll_BothEmpty() {
        String[] a = new String[0];
        String[] b = new String[0];
        String[] result = ArrayUtilities.addAll(a, b);
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.length);
    }

    @Test
    public void testAddAll_WithNullElements() {
        String[] a = {null};
        String[] b = {null};
        String[] result = ArrayUtilities.addAll(a, b);
        Assert.assertArrayEquals(new String[]{null, null}, result);
    }

    @Test
    public void testAddAll_OrderPreserved() {
        String[] a = {"a", "b", "c"};
        String[] b = {"1", "2", "3"};
        String[] result = ArrayUtilities.addAll(a, b);
        Assert.assertArrayEquals(new String[]{"a", "b", "c", "1", "2", "3"}, result);
    }

    /* ---------------------------------------------------------------------- */
    /*  removeItem                                                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testRemoveItem_RemoveMiddle() {
        Integer[] source = {10, 20, 30, 40};
        Integer[] result = ArrayUtilities.removeItem(source, 2);
        Assert.assertArrayEquals(new Integer[]{10, 20, 40}, result);
        // Ensure original unchanged
        Assert.assertArrayEquals(new Integer[]{10, 20, 30, 40}, source);
    }

    @Test
    public void testRemoveItem_RemoveFirst() {
        String[] source = {"first", "second", "third"};
        String[] result = ArrayUtilities.removeItem(source, 0);
        Assert.assertArrayEquals(new String[]{"second", "third"}, result);
    }

    @Test
    public void testRemoveItem_RemoveLast() {
        String[] source = {"a", "b", "c"};
        String[] result = ArrayUtilities.removeItem(source, 2);
        Assert.assertArrayEquals(new String[]{"a", "b"}, result);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testRemoveItem_NegativeIndex() {
        Integer[] source = {1, 2, 3};
        ArrayUtilities.removeItem(source, -1);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testRemoveItem_IndexEqualLength() {
        Integer[] source = {1, 2, 3};
        ArrayUtilities.removeItem(source, 3);
    }

    @Test(expected = NullPointerException.class)
    public void testRemoveItem_NullArray() {
        ArrayUtilities.removeItem(null, 0);
    }

    /* ---------------------------------------------------------------------- */
    /*  getArraySubset                                                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetArraySubset_NormalRange() {
        String[] source = {"zero", "one", "two", "three", "four"};
        String[] subset = ArrayUtilities.getArraySubset(source, 1, 4);
        Assert.assertArrayEquals(new String[]{"one", "two", "three"}, subset);
    }

    @Test
    public void testGetArraySubset_FullRange() {
        Integer[] source = {5, 6, 7};
        Integer[] subset = ArrayUtilities.getArraySubset(source, 0, source.length);
        Assert.assertArrayEquals(source, subset);
        Assert.assertNotSame(source, subset); // must be a new array
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetArraySubset_StartNegative() {
        String[] source = {"a", "b"};
        ArrayUtilities.getArraySubset(source, -1, 1);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetArraySubset_EndTooLarge() {
        String[] source = {"a", "b"};
        ArrayUtilities.getArraySubset(source, 0, 3);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetArraySubset_StartGreaterThanEnd() {
        String[] source = {"a", "b", "c"};
        ArrayUtilities.getArraySubset(source, 2, 1);
    }

    @Test(expected = NullPointerException.class)
    public void testGetArraySubset_NullArray() {
        ArrayUtilities.getArraySubset(null, 0, 1);
    }

    /* ---------------------------------------------------------------------- */
    /*  toArray                                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToArray_StringCollection() {
        List<String> list = new ArrayList<>();
        Collections.addAll(list, "alpha", "beta", "gamma");
        String[] result = ArrayUtilities.toArray(String.class, list);
        Assert.assertArrayEquals(new String[]{"alpha", "beta", "gamma"}, result);
        // Ensure the returned array is of the exact component type
        Assert.assertEquals(String.class, result.getClass().getComponentType());
    }

    @Test
    public void testToArray_EmptyCollection() {
        List<Integer> empty = Collections.emptyList();
        Integer[] result = ArrayUtilities.toArray(Integer.class, empty);
        Assert.assertEquals(0, result.length);
        Assert.assertEquals(Integer.class, result.getClass().getComponentType());
    }

    @Test
    public void testToArray_WithNullElementsInCollection() {
        List<String> list = new ArrayList<>();
        list.add("first");
        list.add(null);
        list.add("third");
        String[] result = ArrayUtilities.toArray(String.class, list);
        Assert.assertArrayEquals(new String[]{"first", null, "third"}, result);
    }

    /* ---------------------------------------------------------------------- */
    /*  Constants                                                             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testConstants_AreEmpty() {
        Assert.assertEquals(0, ArrayUtilities.EMPTY_OBJECT_ARRAY.length);
        Assert.assertEquals(0, ArrayUtilities.EMPTY_BYTE_ARRAY.length);
        Assert.assertEquals(0, ArrayUtilities.EMPTY_CHAR_ARRAY.length);
        Assert.assertEquals(0, ArrayUtilities.EMPTY_CHARACTER_ARRAY.length);
        Assert.assertEquals(0, ArrayUtilities.EMPTY_CLASS_ARRAY.length);
    }
}
