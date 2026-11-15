package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Comprehensive JUnit 4 test suite for {@link ArrayUtilities}.
 */
public class ArrayUtilitiesTest {

    /* ---------------------------------------------------------------------- */
    /*  isEmpty()                                                             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIsEmptyWithNull() {
        Assert.assertTrue(ArrayUtilities.isEmpty(null));
    }

    @Test
    public void testIsEmptyWithEmptyObjectArray() {
        Object[] empty = new Object[0];
        Assert.assertTrue(ArrayUtilities.isEmpty(empty));
    }

    @Test
    public void testIsEmptyWithNonEmptyObjectArray() {
        String[] arr = {"a", "b"};
        Assert.assertFalse(ArrayUtilities.isEmpty(arr));
    }

    @Test
    public void testIsEmptyWithEmptyPrimitiveArray() {
        int[] empty = new int[0];
        Assert.assertTrue(ArrayUtilities.isEmpty(empty));
    }

    @Test
    public void testIsEmptyWithNonEmptyPrimitiveArray() {
        byte[] bytes = {1, 2, 3};
        Assert.assertFalse(ArrayUtilities.isEmpty(bytes));
    }

    /* ---------------------------------------------------------------------- */
    /*  size()                                                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testSizeWithNull() {
        Assert.assertEquals(0, ArrayUtilities.size(null));
    }

    @Test
    public void testSizeWithObjectArray() {
        Integer[] numbers = {1, 2, 3, 4};
        Assert.assertEquals(4, ArrayUtilities.size(numbers));
    }

    @Test
    public void testSizeWithPrimitiveArray() {
        char[] chars = {'a', 'b', 'c'};
        Assert.assertEquals(3, ArrayUtilities.size(chars));
    }

    /* ---------------------------------------------------------------------- */
    /*  shallowCopy()                                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testShallowCopyNullReturnsNull() {
        Assert.assertNull(ArrayUtilities.shallowCopy(null));
    }

    @Test
    public void testShallowCopyCreatesNewArray() {
        String[] original = {"x", "y", "z"};
        String[] copy = ArrayUtilities.shallowCopy(original);
        Assert.assertArrayEquals(original, copy);
        // Ensure a different instance
        Assert.assertNotSame(original, copy);
    }

    @Test
    public void testShallowCopyDoesNotCloneElements() {
        Object obj = new Object();
        Object[] original = {obj};
        Object[] copy = ArrayUtilities.shallowCopy(original);
        Assert.assertSame(obj, copy[0]);
    }

    /* ---------------------------------------------------------------------- */
    /*  createArray()                                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCreateArrayWithElements() {
        Integer[] arr = ArrayUtilities.createArray(1, 2, 3);
        Assert.assertArrayEquals(new Integer[]{1, 2, 3}, arr);
    }

    @Test
    public void testCreateArrayWithNullElements() {
        String[] arr = ArrayUtilities.createArray("a", null, "c");
        Assert.assertEquals(3, arr.length);
        Assert.assertNull(arr[1]);
    }

    @Test(expected = NullPointerException.class)
    public void testCreateArrayWithNullVarargsArray() {
        // This call is equivalent to passing a null array reference to the varargs.
        // The compiler allows it via a cast.
        ArrayUtilities.<String>createArray((String[]) null);
    }

    /* ---------------------------------------------------------------------- */
    /*  addAll()                                                                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAddAllBothNull() {
        Assert.assertNull(ArrayUtilities.addAll(null, null));
    }

    @Test
    public void testAddAllFirstNullSecondNonNull() {
        String[] second = {"second"};
        String[] result = ArrayUtilities.addAll(null, second);
        Assert.assertArrayEquals(second, result);
        Assert.assertNotSame(second, result); // shallowCopy creates a new array
    }

    @Test
    public void testAddAllFirstNonNullSecondNull() {
        Integer[] first = {1, 2};
        Integer[] result = ArrayUtilities.addAll(first, null);
        Assert.assertArrayEquals(first, result);
        Assert.assertNotSame(first, result);
    }

    @Test
    public void testAddAllBothEmpty() {
        String[] empty1 = new String[0];
        String[] empty2 = new String[0];
        String[] result = ArrayUtilities.addAll(empty1, empty2);
        Assert.assertArrayEquals(new String[0], result);
    }

    @Test
    public void testAddAllWithNullElements() {
        String[] a = {null};
        String[] b = {null};
        String[] result = ArrayUtilities.addAll(a, b);
        Assert.assertArrayEquals(new String[]{null, null}, result);
    }

    @Test
    public void testAddAllPreservesOrderAndType() {
        String[] a = {"a", "b"};
        String[] b = {"c", "d"};
        String[] result = ArrayUtilities.addAll(a, b);
        Assert.assertArrayEquals(new String[]{"a", "b", "c", "d"}, result);
        // Verify component type is the same as the first array's component type
        Assert.assertEquals(String.class, result.getClass().getComponentType());
    }

    /* ---------------------------------------------------------------------- */
    /*  removeItem()                                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testRemoveItemMiddle() {
        Integer[] source = {1, 2, 3, 4, 5};
        Integer[] expected = {1, 2, 4, 5};
        Integer[] result = ArrayUtilities.removeItem(source, 2);
        Assert.assertArrayEquals(expected, result);
    }

    @Test
    public void testRemoveItemFirst() {
        String[] source = {"first", "second"};
        String[] expected = {"second"};
        String[] result = ArrayUtilities.removeItem(source, 0);
        Assert.assertArrayEquals(expected, result);
    }

    @Test
    public void testRemoveItemLast() {
        Character[] source = {'a', 'b', 'c'};
        Character[] expected = {'a', 'b'};
        Character[] result = ArrayUtilities.removeItem(source, 2);
        Assert.assertArrayEquals(expected, result);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testRemoveItemNegativeIndex() {
        Integer[] source = {1, 2, 3};
        ArrayUtilities.removeItem(source, -1);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testRemoveItemIndexEqualsLength() {
        Integer[] source = {1, 2, 3};
        ArrayUtilities.removeItem(source, 3);
    }

    @Test(expected = NullPointerException.class)
    public void testRemoveItemNullArray() {
        ArrayUtilities.removeItem(null, 0);
    }

    /* ---------------------------------------------------------------------- */
    /*  getArraySubset()                                                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetArraySubsetNormalRange() {
        String[] source = {"apple", "banana", "cherry", "date", "elderberry"};
        String[] expected = {"banana", "cherry", "date"};
        String[] result = ArrayUtilities.getArraySubset(source, 1, 4);
        Assert.assertArrayEquals(expected, result);
    }

    @Test
    public void testGetArraySubsetFullRange() {
        Integer[] source = {1, 2, 3};
        Integer[] result = ArrayUtilities.getArraySubset(source, 0, source.length);
        Assert.assertArrayEquals(source, result);
        Assert.assertNotSame(source, result); // should be a new array
    }

    @Test
    public void testGetArraySubsetEmptyRange() {
        Double[] source = {1.1, 2.2, 3.3};
        Double[] result = ArrayUtilities.getArraySubset(source, 2, 2);
        Assert.assertEquals(0, result.length);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetArraySubsetStartNegative() {
        String[] source = {"a", "b"};
        ArrayUtilities.getArraySubset(source, -1, 1);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetArraySubsetEndTooLarge() {
        String[] source = {"a", "b"};
        ArrayUtilities.getArraySubset(source, 0, 3);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetArraySubsetStartGreaterThanEnd() {
        String[] source = {"a", "b", "c"};
        ArrayUtilities.getArraySubset(source, 2, 1);
    }

    @Test(expected = NullPointerException.class)
    public void testGetArraySubsetNullArray() {
        ArrayUtilities.getArraySubset(null, 0, 1);
    }

    /* ---------------------------------------------------------------------- */
    /*  toArray()                                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToArrayWithStringCollection() {
        List<String> list = new ArrayList<>();
        list.add("x");
        list.add("y");
        list.add("z");

        String[] result = ArrayUtilities.toArray(String.class, list);
        Assert.assertArrayEquals(new String[]{"x", "y", "z"}, result);
        // Verify the runtime component type
        Assert.assertEquals(String.class, result.getClass().getComponentType());
    }

    @Test
    public void testToArrayWithIntegerCollectionAndEmpty() {
        Collection<Integer> empty = new ArrayList<>();
        Integer[] result = ArrayUtilities.toArray(Integer.class, empty);
        Assert.assertEquals(0, result.length);
        Assert.assertEquals(Integer.class, result.getClass().getComponentType());
    }

    @Test
    public void testToArrayEnsuresElementsAreSetViaArraySet() {
        // Use a collection with mutable objects to ensure the loop inside toArray works.
        List<StringBuilder> list = new ArrayList<>();
        list.add(new StringBuilder("a"));
        list.add(new StringBuilder("b"));

        StringBuilder[] result = ArrayUtilities.toArray(StringBuilder.class, list);
        Assert.assertEquals(2, result.length);
        Assert.assertEquals("a", result[0].toString());
        Assert.assertEquals("b", result[1].toString());

        // Mutate one element and verify the array reflects the change (proving same reference)
        result[0].append("X");
        Assert.assertEquals("aX", list.get(0).toString());
    }

    /* ---------------------------------------------------------------------- */
    /*  Constants (basic sanity)                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testEmptyObjectArrayIsImmutable() {
        Assert.assertEquals(0, ArrayUtilities.EMPTY_OBJECT_ARRAY.length);
        // Attempting to modify should throw ArrayIndexOutOfBoundsException
        try {
            ArrayUtilities.EMPTY_OBJECT_ARRAY[0] = new Object();
            Assert.fail("Expected ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException ignored) {
        }
    }

    @Test
    public void testEmptyByteArrayLength() {
        Assert.assertEquals(0, ArrayUtilities.EMPTY_BYTE_ARRAY.length);
    }

    @Test
    public void testEmptyCharArrayLength() {
        Assert.assertEquals(0, ArrayUtilities.EMPTY_CHAR_ARRAY.length);
    }

    @Test
    public void testEmptyCharacterArrayLength() {
        Assert.assertEquals(0, ArrayUtilities.EMPTY_CHARACTER_ARRAY.length);
    }

    @Test
    public void testEmptyClassArrayLength() {
        Assert.assertEquals(0, ArrayUtilities.EMPTY_CLASS_ARRAY.length);
    }
}
