package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Comprehensive JUnit‑4 test suite for {@link ArrayUtilities}.
 *
 * <p>This test class covers:
 * <ul>
 *   <li>Typical usage scenarios for each public method.</li>
 *   <li>Edge cases such as {@code null} inputs, empty arrays/collections and
 *       boundary indices.</li>
 *   <li>Error conditions that must result in the documented exceptions.</li>
 * </ul>
 * </p>
 *
 * <p>All tests use only the public API of {@code ArrayUtilities} and core JDK
 * classes; no mocking frameworks are employed.</p>
 */
public class ArrayUtilitiesTest {

    /*------------------------------------------------------------*/
    /*----------------------- isEmpty() --------------------------*/
    /*------------------------------------------------------------*/

    @Test
    public void testIsEmpty_NullArray() {
        Assert.assertTrue(ArrayUtilities.isEmpty(null));
    }

    @Test
    public void testIsEmpty_EmptyObjectArray() {
        Object[] empty = new Object[0];
        Assert.assertTrue(ArrayUtilities.isEmpty(empty));
    }

    @Test
    public void testIsEmpty_NonEmptyPrimitiveArray() {
        int[] numbers = {1, 2, 3};
        Assert.assertFalse(ArrayUtilities.isEmpty(numbers));
    }

    @Test
    public void testIsEmpty_EmptyPrimitiveArray() {
        byte[] empty = new byte[0];
        Assert.assertTrue(ArrayUtilities.isEmpty(empty));
    }

    /*------------------------------------------------------------*/
    /*-------------------------- size() --------------------------*/
    /*------------------------------------------------------------*/

    @Test
    public void testSize_NullArray() {
        Assert.assertEquals(0, ArrayUtilities.size(null));
    }

    @Test
    public void testSize_EmptyArray() {
        String[] empty = new String[0];
        Assert.assertEquals(0, ArrayUtilities.size(empty));
    }

    @Test
    public void testSize_NonEmptyArray() {
        char[] chars = {'a', 'b', 'c'};
        Assert.assertEquals(3, ArrayUtilities.size(chars));
    }

    /*------------------------------------------------------------*/
    /*---------------------- shallowCopy() ----------------------*/
    /*------------------------------------------------------------*/

    @Test
    public void testShallowCopy_Null() {
        Assert.assertNull(ArrayUtilities.shallowCopy(null));
    }

    @Test
    public void testShallowCopy_CloneIsNewInstance() {
        String[] original = {"x", "y", "z"};
        String[] copy = ArrayUtilities.shallowCopy(original);
        Assert.assertNotSame(original, copy);
        Assert.assertArrayEquals(original, copy);
    }

    @Test
    public void testShallowCopy_IsShallow_NotDeep() {
        Integer[] original = {new Integer(1), new Integer(2)};
        Integer[] copy = ArrayUtilities.shallowCopy(original);
        // modify an element via the copy and verify original sees the change (same reference)
        copy[0] = new Integer(99);
        Assert.assertEquals(Integer.valueOf(99), original[0]); // because we replaced reference, original unchanged
        // to really test shallow nature, keep same objects inside
        original = new Integer[]{new Integer(10), new Integer(20)};
        copy = ArrayUtilities.shallowCopy(original);
        copy[0] = original[0]; // both refer to same Integer object
        Assert.assertSame(original[0], copy[0]);
    }

    /*------------------------------------------------------------*/
    /*---------------------- createArray() -----------------------*/
    /*------------------------------------------------------------*/

    @Test
    public void testCreateArray_Varargs() {
        String[] arr = ArrayUtilities.createArray("a", "b", "c");
        Assert.assertArrayEquals(new String[]{"a", "b", "c"}, arr);
    }

    @Test
    public void testCreateArray_EmptyVarargs() {
        Integer[] empty = ArrayUtilities.createArray();
        Assert.assertEquals(0, empty.length);
    }

    @Test
    public void testCreateArray_WithNullElement() {
        String[] arr = ArrayUtilities.createArray("first", null, "third");
        Assert.assertArrayEquals(new String[]{"first", null, "third"}, arr);
    }

    /*------------------------------------------------------------*/
    /*-------------------------- addAll() ------------------------*/
    /*------------------------------------------------------------*/

    @Test
    public void testAddAll_BothNull() {
        Assert.assertNull(ArrayUtilities.addAll(null, null));
    }

    @Test
    public void testAddAll_FirstNull() {
        String[] second = {"b", "c"};
        String[] result = ArrayUtilities.addAll(null, second);
        Assert.assertArrayEquals(second, result);
        Assert.assertNotSame(second, result); // should be a shallow copy
    }

    @Test
    public void testAddAll_SecondNull() {
        Integer[] first = {1, 2, 3};
        Integer[] result = ArrayUtilities.addAll(first, null);
        Assert.assertArrayEquals(first, result);
        Assert.assertNotSame(first, result);
    }

    @Test
    public void testAddAll_TwoNonNullArrays() {
        String[] a1 = {"a", "b"};
        String[] a2 = {"c", "d", "e"};
        String[] combined = ArrayUtilities.addAll(a1, a2);
        Assert.assertArrayEquals(new String[]{"a", "b", "c", "d", "e"}, combined);
        // ensure a new array instance is returned
        Assert.assertNotSame(a1, combined);
        Assert.assertNotSame(a2, combined);
    }

    @Test
    public void testAddAll_EmptyArrays() {
        Object[] empty1 = new Object[0];
        Object[] empty2 = new Object[0];
        Object[] result = ArrayUtilities.addAll(empty1, empty2);
        Assert.assertArrayEquals(new Object[0], result);
    }

    /*------------------------------------------------------------*/
    /*----------------------- removeItem() ----------------------*/
    /*------------------------------------------------------------*/

    @Test
    public void testRemoveItem_MiddleElement() {
        Integer[] source = {1, 2, 3, 4, 5};
        Integer[] result = ArrayUtilities.removeItem(source, 2); // remove '3'
        Assert.assertArrayEquals(new Integer[]{1, 2, 4, 5}, result);
    }

    @Test
    public void testRemoveItem_FirstElement() {
        String[] source = {"a", "b", "c"};
        String[] result = ArrayUtilities.removeItem(source, 0);
        Assert.assertArrayEquals(new String[]{"b", "c"}, result);
    }

    @Test
    public void testRemoveItem_LastElement() {
        Character[] source = {'x', 'y', 'z'};
        Character[] result = ArrayUtilities.removeItem(source, 2);
        Assert.assertArrayEquals(new Character[]{'x', 'y'}, result);
    }

    @Test(expected = NullPointerException.class)
    public void testRemoveItem_NullArray() {
        ArrayUtilities.removeItem(null, 0);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testRemoveItem_NegativeIndex() {
        Integer[] src = {1, 2, 3};
        ArrayUtilities.removeItem(src, -1);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testRemoveItem_IndexEqualToLength() {
        Integer[] src = {1, 2, 3};
        ArrayUtilities.removeItem(src, 3);
    }

    /*------------------------------------------------------------*/
    /*--------------------- getArraySubset() ---------------------*/
    /*------------------------------------------------------------*/

    @Test
    public void testGetArraySubset_FullRange() {
        String[] src = {"zero", "one", "two", "three"};
        String[] sub = ArrayUtilities.getArraySubset(src, 0, src.length);
        Assert.assertArrayEquals(src, sub);
        Assert.assertNotSame(src, sub);
    }

    @Test
    public void testGetArraySubset_PartialRange() {
        Integer[] src = {10, 20, 30, 40, 50};
        Integer[] sub = ArrayUtilities.getArraySubset(src, 1, 4); // {20,30,40}
        Assert.assertArrayEquals(new Integer[]{20, 30, 40}, sub);
    }

    @Test
    public void testGetArraySubset_EmptyResult() {
        Double[] src = {1.1, 2.2, 3.3};
        Double[] sub = ArrayUtilities.getArraySubset(src, 2, 2);
        Assert.assertEquals(0, sub.length);
    }

    @Test(expected = NullPointerException.class)
    public void testGetArraySubset_NullArray() {
        ArrayUtilities.getArraySubset(null, 0, 1);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetArraySubset_StartNegative() {
        String[] src = {"a", "b"};
        ArrayUtilities.getArraySubset(src, -1, 1);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetArraySubset_EndBeyondLength() {
        String[] src = {"a", "b"};
        ArrayUtilities.getArraySubset(src, 0, 3);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetArraySubset_StartGreaterThanEnd() {
        String[] src = {"a", "b", "c"};
        ArrayUtilities.getArraySubset(src, 2, 1);
    }

    /*------------------------------------------------------------*/
    /*-------------------------- toArray() -----------------------*/
    /*------------------------------------------------------------*/

    @Test
    public void testToArray_TypicalCollection() {
        List<String> list = new ArrayList<>();
        Collections.addAll(list, "alpha", "beta", "gamma");
        String[] array = ArrayUtilities.toArray(String.class, list);
        Assert.assertArrayEquals(new String[]{"alpha", "beta", "gamma"}, array);
        // Verify the returned array is of the exact component type
        Assert.assertEquals(String.class, array.getClass().getComponentType());
    }

    @Test
    public void testToArray_EmptyCollection() {
        List<Integer> empty = Collections.emptyList();
        Integer[] array = ArrayUtilities.toArray(Integer.class, empty);
        Assert.assertEquals(0, array.length);
        Assert.assertEquals(Integer.class, array.getClass().getComponentType());
    }

    @Test
    public void testToArray_CollectionWithNulls() {
        List<Object> list = new ArrayList<>();
        list.add(null);
        list.add("nonNull");
        Object[] array = ArrayUtilities.toArray(Object.class, list);
        Assert.assertArrayEquals(new Object[]{null, "nonNull"}, array);
    }

    @Test(expected = NullPointerException.class)
    public void testToArray_NullCollection() {
        // The method itself does not guard against a null collection; it will throw NPE
        ArrayUtilities.toArray(String.class, null);
    }

    @Test(expected = NullPointerException.class)
    public void testToArray_NullClassArgument() {
        List<String> list = Collections.singletonList("test");
        // Passing a null class leads to a NPE when Array.newInstance is called
        ArrayUtilities.toArray(null, list);
    }

    /*------------------------------------------------------------*/
    /*------------------- Immutable Empty Arrays -----------------*/
    /*------------------------------------------------------------*/

    @Test
    public void testImmutableEmptyArrays_AreZeroLength() {
        Assert.assertEquals(0, ArrayUtilities.EMPTY_OBJECT_ARRAY.length);
        Assert.assertEquals(0, ArrayUtilities.EMPTY_BYTE_ARRAY.length);
        Assert.assertEquals(0, ArrayUtilities.EMPTY_CHAR_ARRAY.length);
        Assert.assertEquals(0, ArrayUtilities.EMPTY_CHARACTER_ARRAY.length);
        Assert.assertEquals(0, ArrayUtilities.EMPTY_CLASS_ARRAY.length);
    }

    @Test
    public void testImmutableEmptyArrays_CannotBeModified() {
        // Attempting to write into the empty arrays should throw ArrayIndexOutOfBoundsException
        try {
            ArrayUtilities.EMPTY_OBJECT_ARRAY[0] = new Object();
            Assert.fail("Expected ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException ignored) {
        }
        try {
            ArrayUtilities.EMPTY_BYTE_ARRAY[0] = (byte) 1;
            Assert.fail("Expected ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException ignored) {
        }
    }
}
