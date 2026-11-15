package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;

/**
 * Comprehensive JUnit 4 test suite for {@link ArrayUtilities}.
 */
public class ArrayUtilitiesTest {

    /* ----------------------------------------------------------------------
     * isEmpty()
     * ---------------------------------------------------------------------- */
    @Test
    public void testIsEmptyWithNull() {
        assertTrue(ArrayUtilities.isEmpty(null));
    }

    @Test
    public void testIsEmptyWithEmptyObjectArray() {
        Object[] empty = new Object[0];
        assertTrue(ArrayUtilities.isEmpty(empty));
    }

    @Test
    public void testIsEmptyWithEmptyPrimitiveArray() {
        int[] empty = new int[0];
        assertTrue(ArrayUtilities.isEmpty(empty));
    }

    @Test
    public void testIsEmptyWithNonEmptyArray() {
        String[] arr = {"a"};
        assertFalse(ArrayUtilities.isEmpty(arr));
    }

    /* ----------------------------------------------------------------------
     * size()
     * ---------------------------------------------------------------------- */
    @Test
    public void testSizeWithNull() {
        assertEquals(0, ArrayUtilities.size(null));
    }

    @Test
    public void testSizeWithEmptyArray() {
        double[] empty = new double[0];
        assertEquals(0, ArrayUtilities.size(empty));
    }

    @Test
    public void testSizeWithNonEmptyArray() {
        char[] chars = {'x', 'y', 'z'};
        assertEquals(3, ArrayUtilities.size(chars));
    }

    /* ----------------------------------------------------------------------
     * shallowCopy()
     * ---------------------------------------------------------------------- */
    @Test
    public void testShallowCopyWithNull() {
        assertNull(ArrayUtilities.shallowCopy(null));
    }

    @Test
    public void testShallowCopyCreatesNewArray() {
        Integer[] original = {1, 2, 3};
        Integer[] copy = ArrayUtilities.shallowCopy(original);
        assertArrayEquals(original, copy);
        assertNotSame("The returned array should be a new instance", original, copy);
    }

    @Test
    public void testShallowCopyIsShallow() {
        StringBuilder sb = new StringBuilder("test");
        StringBuilder[] original = {sb};
        StringBuilder[] copy = ArrayUtilities.shallowCopy(original);
        // modify the element through the copy and verify original sees the change
        copy[0].append("-changed");
        assertEquals("test-changed", original[0].toString());
    }

    /* ----------------------------------------------------------------------
     * createArray()
     * ---------------------------------------------------------------------- */
    @Test
    public void testCreateArrayWithElements() {
        String[] arr = ArrayUtilities.createArray("a", "b", "c");
        assertArrayEquals(new String[]{"a", "b", "c"}, arr);
    }

    @Test
    public void testCreateArrayWithNoElements() {
        Integer[] empty = ArrayUtilities.createArray();
        assertNotNull(empty);
        assertEquals(0, empty.length);
    }

    @Test
    public void testCreateArrayWithNullElements() {
        String[] arr = ArrayUtilities.createArray("x", null, "z");
        assertArrayEquals(new String[]{"x", null, "z"}, arr);
    }

    @Test(expected = NullPointerException.class)
    public void testCreateArrayWithNullArrayArgument() {
        // Explicitly pass a null array to the varargs parameter
        ArrayUtilities.<String>createArray((String[]) null);
    }

    /* ----------------------------------------------------------------------
     * addAll()
     * ---------------------------------------------------------------------- */
    @Test
    public void testAddAllBothNull() {
        assertNull(ArrayUtilities.addAll(null, null));
    }

    @Test
    public void testAddAllFirstNullSecondNonNull() {
        String[] second = {"b", "c"};
        String[] result = ArrayUtilities.addAll(null, second);
        assertArrayEquals(second, result);
        assertNotSame("Result should be a shallow copy, not the same reference", second, result);
    }

    @Test
    public void testAddAllFirstNonNullSecondNull() {
        Integer[] first = {1, 2};
        Integer[] result = ArrayUtilities.addAll(first, null);
        assertArrayEquals(first, result);
        assertNotSame(first, result);
    }

    @Test
    public void testAddAllBothEmpty() {
        String[] empty1 = new String[0];
        String[] empty2 = new String[0];
        String[] result = ArrayUtilities.addAll(empty1, empty2);
        assertArrayEquals(new String[0], result);
    }

    @Test
    public void testAddAllWithNullElements() {
        String[] a = {null};
        String[] b = {null, "x"};
        String[] result = ArrayUtilities.addAll(a, b);
        assertArrayEquals(new String[]{null, null, "x"}, result);
    }

    @Test
    public void testAddAllTypicalCase() {
        String[] a = {"a", "b", "c"};
        String[] b = {"1", "2", "3"};
        String[] result = ArrayUtilities.addAll(a, b);
        assertArrayEquals(new String[]{"a", "b", "c", "1", "2", "3"}, result);
    }

    /* ----------------------------------------------------------------------
     * removeItem()
     * ---------------------------------------------------------------------- */
    @Test
    public void testRemoveItemMiddle() {
        String[] src = {"a", "b", "c", "d"};
        String[] expected = {"a", "b", "d"};
        String[] result = ArrayUtilities.removeItem(src, 2);
        assertArrayEquals(expected, result);
    }

    @Test
    public void testRemoveItemFirst() {
        Integer[] src = {10, 20, 30};
        Integer[] expected = {20, 30};
        Integer[] result = ArrayUtilities.removeItem(src, 0);
        assertArrayEquals(expected, result);
    }

    @Test
    public void testRemoveItemLast() {
        Character[] src = {'x', 'y', 'z'};
        Character[] expected = {'x', 'y'};
        Character[] result = ArrayUtilities.removeItem(src, 2);
        assertArrayEquals(expected, result);
    }

    @Test
    public void testRemoveItemSingleElementArray() {
        Boolean[] src = {Boolean.TRUE};
        Boolean[] result = ArrayUtilities.removeItem(src, 0);
        assertEquals(0, result.length);
    }

    @Test(expected = NullPointerException.class)
    public void testRemoveItemNullArray() {
        ArrayUtilities.removeItem(null, 0);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testRemoveItemNegativeIndex() {
        String[] src = {"a", "b"};
        ArrayUtilities.removeItem(src, -1);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testRemoveItemIndexEqualToLength() {
        String[] src = {"a", "b"};
        ArrayUtilities.removeItem(src, 2);
    }

    /* ----------------------------------------------------------------------
     * getArraySubset()
     * ---------------------------------------------------------------------- */
    @Test
    public void testGetArraySubsetNormalRange() {
        String[] src = {"apple", "banana", "cherry", "date", "elderberry"};
        String[] expected = {"banana", "cherry", "date"};
        assertArrayEquals(expected, ArrayUtilities.getArraySubset(src, 1, 4));
    }

    @Test
    public void testGetArraySubsetFullRange() {
        Integer[] src = {1, 2, 3};
        Integer[] result = ArrayUtilities.getArraySubset(src, 0, src.length);
        assertArrayEquals(src, result);
    }

    @Test
    public void testGetArraySubsetEmptyResult() {
        Double[] src = {0.1, 0.2, 0.3};
        Double[] result = ArrayUtilities.getArraySubset(src, 1, 1);
        assertEquals(0, result.length);
    }

    @Test(expected = NullPointerException.class)
    public void testGetArraySubsetNullArray() {
        ArrayUtilities.getArraySubset(null, 0, 1);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetArraySubsetStartNegative() {
        String[] src = {"a", "b"};
        ArrayUtilities.getArraySubset(src, -1, 1);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetArraySubsetEndBeyondLength() {
        String[] src = {"a", "b"};
        ArrayUtilities.getArraySubset(src, 0, 3);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetArraySubsetStartGreaterThanEnd() {
        String[] src = {"a", "b", "c"};
        ArrayUtilities.getArraySubset(src, 2, 1);
    }

    /* ----------------------------------------------------------------------
     * toArray()
     * ---------------------------------------------------------------------- */
    @Test
    public void testToArrayWithStrings() {
        List<String> list = new ArrayList<>();
        list.add("x");
        list.add("y");
        list.add("z");

        String[] result = ArrayUtilities.toArray(String.class, list);
        assertArrayEquals(new String[]{"x", "y", "z"}, result);
        // Verify the returned array is of the exact requested component type
        assertEquals(String.class, result.getClass().getComponentType());
    }

    @Test
    public void testToArrayWithIntegersAndNulls() {
        List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(null);
        list.add(3);

        Integer[] result = ArrayUtilities.toArray(Integer.class, list);
        assertArrayEquals(new Integer[]{1, null, 3}, result);
    }

    @Test
    public void testToArrayEmptyCollection() {
        Collection<Object> empty = new ArrayList<>();
        Object[] result = ArrayUtilities.toArray(Object.class, empty);
        assertEquals(0, result.length);
    }

    @Test
    public void testToArrayEnsuresElementsAreSetViaReflection() {
        // The method uses Array.set internally; we verify that it correctly
        // copies each element even when the collection's iterator returns distinct objects.
        List<StringBuilder> list = new ArrayList<>();
        list.add(new StringBuilder("a"));
        list.add(new StringBuilder("b"));

        StringBuilder[] result = ArrayUtilities.toArray(StringBuilder.class, list);
        assertEquals(2, result.length);
        assertEquals("a", result[0].toString());
        assertEquals("b", result[1].toString());
    }

    /* ----------------------------------------------------------------------
     * Immutable EMPTY_* arrays (basic sanity)
     * ---------------------------------------------------------------------- */
    @Test
    public void testEmptyObjectArrayIsZeroLength() {
        assertEquals(0, ArrayUtilities.EMPTY_OBJECT_ARRAY.length);
    }

    @Test
    public void testEmptyByteArrayIsZeroLength() {
        assertEquals(0, ArrayUtilities.EMPTY_BYTE_ARRAY.length);
    }

    @Test
    public void testEmptyCharArrayIsZeroLength() {
        assertEquals(0, ArrayUtilities.EMPTY_CHAR_ARRAY.length);
    }

    @Test
    public void testEmptyCharacterArrayIsZeroLength() {
        assertEquals(0, ArrayUtilities.EMPTY_CHARACTER_ARRAY.length);
    }

    @Test
    public void testEmptyClassArrayIsZeroLength() {
        assertEquals(0, ArrayUtilities.EMPTY_CLASS_ARRAY.length);
    }
}
