package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Comprehensive JUnit‑4 test suite for {@link ArrayUtilities}.
 *
 * <p>Each public static method of {@code ArrayUtilities} is exercised with:
 * <ul>
 *   <li>typical (happy‑path) usage,</li>
 *   <li>edge‑case scenarios (nulls, empty arrays, boundary indexes, etc.),</li>
 *   <li>exception‑throwing conditions.</li>
 * </ul>
 * The tests deliberately avoid any external libraries – only the classes
 * and methods present in the supplied source are used.
 */
public class ArrayUtilitiesTest {

    /* ---------------------------------------------------------------------- */
    /* isEmpty(Object)                                                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIsEmpty_WithNullArray_ShouldReturnTrue() {
        Assert.assertTrue(ArrayUtilities.isEmpty(null));
    }

    @Test
    public void testIsEmpty_WithEmptyObjectArray_ShouldReturnTrue() {
        Assert.assertTrue(ArrayUtilities.isEmpty(new Object[0]));
    }

    @Test
    public void testIsEmpty_WithNonEmptyObjectArray_ShouldReturnFalse() {
        Assert.assertFalse(ArrayUtilities.isEmpty(new String[]{"a"}));
    }

    @Test
    public void testIsEmpty_WithEmptyPrimitiveArray_ShouldReturnTrue() {
        Assert.assertTrue(ArrayUtilities.isEmpty(new int[0]));
        Assert.assertTrue(ArrayUtilities.isEmpty(new byte[0]));
        Assert.assertTrue(ArrayUtilities.isEmpty(new char[0]));
    }

    @Test
    public void testIsEmpty_WithNonEmptyPrimitiveArray_ShouldReturnFalse() {
        Assert.assertFalse(ArrayUtilities.isEmpty(new int[]{1, 2, 3}));
    }

    /* ---------------------------------------------------------------------- */
    /* size(Object)                                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testSize_WithNullArray_ShouldReturnZero() {
        Assert.assertEquals(0, ArrayUtilities.size(null));
    }

    @Test
    public void testSize_WithEmptyArray_ShouldReturnZero() {
        Assert.assertEquals(0, ArrayUtilities.size(new Object[0]));
        Assert.assertEquals(0, ArrayUtilities.size(new int[0]));
    }

    @Test
    public void testSize_WithNonEmptyArray_ShouldReturnLength() {
        String[] arr = {"x", "y", "z"};
        Assert.assertEquals(3, ArrayUtilities.size(arr));
        int[] prim = {1, 2, 3, 4};
        Assert.assertEquals(4, ArrayUtilities.size(prim));
    }

    /* ---------------------------------------------------------------------- */
    /* shallowCopy(T[])                                                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testShallowCopy_WithNull_ShouldReturnNull() {
        Assert.assertNull(ArrayUtilities.shallowCopy(null));
    }

    @Test
    public void testShallowCopy_WithNonNullArray_ShouldReturnClone() {
        Integer[] original = {1, 2, 3};
        Integer[] copy = ArrayUtilities.shallowCopy(original);

        // Different object reference
        Assert.assertNotSame(original, copy);
        // Same contents
        Assert.assertArrayEquals(original, copy);
        // Modifying copy does not affect original (shallow copy)
        copy[0] = 99;
        Assert.assertEquals(Integer.valueOf(1), original[0]);
    }

    /* ---------------------------------------------------------------------- */
    /* createArray(T...)                                                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCreateArray_WithElements_ShouldReturnArrayContainingThem() {
        String[] result = ArrayUtilities.createArray("a", "b", "c");
        Assert.assertArrayEquals(new String[]{"a", "b", "c"}, result);
    }

    @Test(expected = NullPointerException.class)
    public void testCreateArray_WithNullVarargs_ShouldThrowNPE() {
        // Explicit cast forces the varargs array itself to be null
        ArrayUtilities.<String>createArray((String[]) null);
    }

    /* ---------------------------------------------------------------------- */
    /* addAll(T[], T[])                                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAddAll_BothNull_ShouldReturnNull() {
        Assert.assertNull(ArrayUtilities.addAll(null, null));
    }

    @Test
    public void testAddAll_FirstNull_ShouldReturnCloneOfSecond() {
        Integer[] second = {5, 6};
        Integer[] result = ArrayUtilities.addAll(null, second);
        Assert.assertNotSame(second, result);
        Assert.assertArrayEquals(second, result);
    }

    @Test
    public void testAddAll_SecondNull_ShouldReturnCloneOfFirst() {
        String[] first = {"x", "y"};
        String[] result = ArrayUtilities.addAll(first, null);
        Assert.assertNotSame(first, result);
        Assert.assertArrayEquals(first, result);
    }

    @Test
    public void testAddAll_BothEmpty_ShouldReturnEmptyArray() {
        String[] result = ArrayUtilities.addAll(new String[0], new String[0]);
        Assert.assertEquals(0, result.length);
    }

    @Test
    public void testAddAll_WithNullElements_ShouldPreserveNulls() {
        String[] a = {null};
        String[] b = {null};
        String[] result = ArrayUtilities.addAll(a, b);
        Assert.assertArrayEquals(new String[]{null, null}, result);
    }

    @Test
    public void testAddAll_TypicalCase_ShouldConcatenateInOrder() {
        String[] a = {"a", "b"};
        String[] b = {"c", "d"};
        String[] result = ArrayUtilities.addAll(a, b);
        Assert.assertArrayEquals(new String[]{"a", "b", "c", "d"}, result);
    }

    /* ---------------------------------------------------------------------- */
    /* removeItem(T[], int)                                                     */
    /* ---------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testRemoveItem_NullArray_ShouldThrowNPE() {
        ArrayUtilities.removeItem(null, 0);
    }

    @Test
    public void testRemoveItem_RemoveMiddleElement() {
        Integer[] src = {1, 2, 3, 4, 5};
        Integer[] dest = ArrayUtilities.removeItem(src, 2); // remove element "3"
        Assert.assertArrayEquals(new Integer[]{1, 2, 4, 5}, dest);
        // original unchanged
        Assert.assertArrayEquals(new Integer[]{1, 2, 3, 4, 5}, src);
    }

    @Test
    public void testRemoveItem_RemoveFirstElement() {
        String[] src = {"first", "second", "third"};
        String[] dest = ArrayUtilities.removeItem(src, 0);
        Assert.assertArrayEquals(new String[]{"second", "third"}, dest);
    }

    @Test
    public void testRemoveItem_RemoveLastElement() {
        String[] src = {"a", "b", "c"};
        String[] dest = ArrayUtilities.removeItem(src, 2);
        Assert.assertArrayEquals(new String[]{"a", "b"}, dest);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testRemoveItem_NegativeIndex_ShouldThrowException() {
        Integer[] src = {1, 2, 3};
        ArrayUtilities.removeItem(src, -1);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testRemoveItem_IndexEqualToLength_ShouldThrowException() {
        Integer[] src = {1, 2, 3};
        ArrayUtilities.removeItem(src, 3);
    }

    /* ---------------------------------------------------------------------- */
    /* getArraySubset(T[], int, int)                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetArraySubset_TypicalRange() {
        String[] src = {"a", "b", "c", "d", "e"};
        String[] sub = ArrayUtilities.getArraySubset(src, 1, 4);
        Assert.assertArrayEquals(new String[]{"b", "c", "d"}, sub);
    }

    @Test
    public void testGetArraySubset_FullRange_ShouldReturnCopy() {
        Integer[] src = {10, 20, 30};
        Integer[] sub = ArrayUtilities.getArraySubset(src, 0, src.length);
        Assert.assertArrayEquals(src, sub);
        Assert.assertNotSame(src, sub); // ensure new array
    }

    @Test
    public void testGetArraySubset_EmptyRange_ShouldReturnEmptyArray() {
        String[] src = {"x", "y"};
        String[] sub = ArrayUtilities.getArraySubset(src, 1, 1);
        Assert.assertEquals(0, sub.length);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetArraySubset_StartNegative_ShouldThrowException() {
        String[] src = {"a", "b"};
        ArrayUtilities.getArraySubset(src, -1, 1);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetArraySubset_EndBeyondLength_ShouldThrowException() {
        String[] src = {"a", "b"};
        ArrayUtilities.getArraySubset(src, 0, 3);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetArraySubset_StartGreaterThanEnd_ShouldThrowException() {
        String[] src = {"a", "b", "c"};
        ArrayUtilities.getArraySubset(src, 2, 1);
    }

    @Test(expected = NullPointerException.class)
    public void testGetArraySubset_NullArray_ShouldThrowNPE() {
        ArrayUtilities.getArraySubset(null, 0, 1);
    }

    /* ---------------------------------------------------------------------- */
    /* toArray(Class<T>, Collection<?>)                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToArray_WithNonEmptyCollection_ShouldReturnTypedArray() {
        List<String> list = new ArrayList<>();
        Collections.addAll(list, "one", "two", "three");
        String[] arr = ArrayUtilities.toArray(String.class, list);
        Assert.assertArrayEquals(new String[]{"one", "two", "three"}, arr);
    }

    @Test
    public void testToArray_WithEmptyCollection_ShouldReturnEmptyArray() {
        Collection<Integer> empty = Collections.emptyList();
        Integer[] arr = ArrayUtilities.toArray(Integer.class, empty);
        Assert.assertEquals(0, arr.length);
    }

    @Test
    public void testToArray_WithNullElements_ShouldPreserveNulls() {
        List<String> list = new ArrayList<>();
        list.add(null);
        list.add("valid");
        String[] arr = ArrayUtilities.toArray(String.class, list);
        Assert.assertArrayEquals(new String[]{null, "valid"}, arr);
    }

    @Test
    public void testToArray_WithPrimitiveComponentClass_ShouldWork() {
        // Using Integer.class is the usual case; primitive class (int.class) also works
        List<Integer> list = List.of(1, 2, 3);
        int[] arr = ArrayUtilities.toArray(int.class, list);
        Assert.assertArrayEquals(new int[]{1, 2, 3}, arr);
    }

    @Test(expected = NullPointerException.class)
    public void testToArray_NullCollection_ShouldThrowNPE() {
        // The method itself will throw NPE when trying to call c.toArray(...)
        ArrayUtilities.toArray(String.class, null);
    }

    @Test(expected = NullPointerException.class)
    public void testToArray_NullClassParameter_ShouldThrowNPE() {
        List<String> list = List.of("a");
        // Passing null for classToCastTo results in NPE inside Array.newInstance()
        ArrayUtilities.toArray(null, list);
    }

    /* ---------------------------------------------------------------------- */
    /* Constants (sanity checks)                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testEmptyConstants_ShouldBeImmutableAndZeroLength() {
        Assert.assertEquals(0, ArrayUtilities.EMPTY_OBJECT_ARRAY.length);
        Assert.assertEquals(0, ArrayUtilities.EMPTY_BYTE_ARRAY.length);
        Assert.assertEquals(0, ArrayUtilities.EMPTY_CHAR_ARRAY.length);
        Assert.assertEquals(0, ArrayUtilities.EMPTY_CHARACTER_ARRAY.length);
        Assert.assertEquals(0, ArrayUtilities.EMPTY_CLASS_ARRAY.length);
    }
}
