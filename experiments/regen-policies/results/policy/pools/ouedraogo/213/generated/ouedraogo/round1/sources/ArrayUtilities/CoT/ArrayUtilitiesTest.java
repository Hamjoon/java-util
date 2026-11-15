package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * JUnit 4 test suite for {@link ArrayUtilities}.
 *
 * The test suite is generated in several steps:
 * 1. List of all public methods (including signatures).
 * 2. Basic functional tests for each method.
 * 3. Identification of edge‑case / exception scenarios.
 * 4. Specific tests that verify the edge‑case handling.
 *
 * All tests are self‑contained and use only the public API that
 * exists in {@link ArrayUtilities}.
 */
public class ArrayUtilitiesTest {

    /* -------------------------------------------------------------
     * 1. PUBLIC METHOD SIGNATURES (extracted from ArrayUtilities)
     * -------------------------------------------------------------
     *
     * public static boolean isEmpty(final Object array)
     * public static int size(final Object array)
     * public static <T> T[] shallowCopy(final T[] array)
     * public static <T> T[] createArray(T... elements)
     * public static <T> T[] addAll(final T[] array1, final T[] array2)
     * public static <T> T[] removeItem(T[] array, int pos)
     * public static <T> T[] getArraySubset(T[] array, int start, int end)
     * public static <T> T[] toArray(Class<T> classToCastTo, Collection<?> c)
     *
     */

    /* -------------------------------------------------------------
     * 2. BASIC FUNCTIONAL TESTS
     * ------------------------------------------------------------- */

    @Test
    public void testIsEmpty() {
        Assert.assertTrue(ArrayUtilities.isEmpty(null));
        Assert.assertTrue(ArrayUtilities.isEmpty(new Object[0]));
        Assert.assertFalse(ArrayUtilities.isEmpty(new String[]{"a"}));
    }

    @Test
    public void testSize() {
        Assert.assertEquals(0, ArrayUtilities.size(null));
        Assert.assertEquals(0, ArrayUtilities.size(new int[0]));
        Assert.assertEquals(3, ArrayUtilities.size(new Double[]{1.0, 2.0, 3.0}));
    }

    @Test
    public void testShallowCopy() {
        String[] original = {"x", "y", "z"};
        String[] copy = ArrayUtilities.shallowCopy(original);
        Assert.assertArrayEquals(original, copy);
        // ensure a new instance is created
        Assert.assertNotSame(original, copy);

        // null input should yield null
        Assert.assertNull(ArrayUtilities.shallowCopy(null));
    }

    @Test
    public void testCreateArrayBasic() {
        Integer[] ints = ArrayUtilities.createArray(1, 2, 3);
        Assert.assertArrayEquals(new Integer[]{1, 2, 3}, ints);

        String[] strings = ArrayUtilities.createArray("a", "b");
        Assert.assertArrayEquals(new String[]{"a", "b"}, strings);
    }

    @Test
    public void testAddAllBasic() {
        String[] a = {"a", "b"};
        String[] b = {"c", "d"};
        String[] result = ArrayUtilities.addAll(a, b);
        Assert.assertArrayEquals(new String[]{"a", "b", "c", "d"}, result);
    }

    @Test
    public void testRemoveItemBasic() {
        Integer[] source = {1, 2, 3, 4, 5};
        Integer[] result = ArrayUtilities.removeItem(source, 2); // remove element '3'
        Assert.assertArrayEquals(new Integer[]{1, 2, 4, 5}, result);
    }

    @Test
    public void testGetArraySubsetBasic() {
        String[] words = {"apple", "banana", "cherry", "date", "elderberry"};
        String[] subset = ArrayUtilities.getArraySubset(words, 1, 4);
        Assert.assertArrayEquals(new String[]{"banana", "cherry", "date"}, subset);
    }

    @Test
    public void testToArrayBasic() {
        List<String> list = List.of("x", "y", "z");
        String[] array = ArrayUtilities.toArray(String.class, list);
        Assert.assertArrayEquals(new String[]{"x", "y", "z"}, array);
    }

    /* -------------------------------------------------------------
     * 3. EDGE CASE / EXCEPTION IDENTIFICATION
     * -------------------------------------------------------------
     *
     * isEmpty: null, empty, non‑empty (already covered)
     *
     * size: null, empty, non‑empty (already covered)
     *
     * shallowCopy: null input (covered), empty array
     *
     * createArray:
     *   - zero arguments (empty array)
     *   - null elements inside the array
     *   - passing a null var‑args array → should throw NullPointerException
     *
     * addAll:
     *   - both arguments null → result null
     *   - first null, second non‑null → cloned copy of second
     *   - second null, first non‑null → cloned copy of first
     *   - both empty arrays → empty array
     *
     * removeItem:
     *   - pos == 0 (remove first)
     *   - pos == length‑1 (remove last)
     *   - negative pos → ArrayIndexOutOfBoundsException
     *   - pos >= length → ArrayIndexOutOfBoundsException
     *   - null array → NullPointerException
     *
     * getArraySubset:
     *   - start == 0, end == length (full copy)
     *   - start == end (empty array)
     *   - start < 0 → ArrayIndexOutOfBoundsException
     *   - end > length → ArrayIndexOutOfBoundsException
     *   - start > end → ArrayIndexOutOfBoundsException
     *   - null array → NullPointerException
     *
     * toArray:
     *   - empty collection → empty array
     *   - collection with a single null element
     *   - null classToCastTo → NullPointerException (from Array.newInstance)
     *   - null collection → NullPointerException
     *
     */

    /* -------------------------------------------------------------
     * 4. SPECIFIC EDGE‑CASE / EXCEPTION TESTS
     * ------------------------------------------------------------- */

    // ---------- shallowCopy edge cases ----------
    @Test
    public void testShallowCopyEmptyArray() {
        String[] empty = new String[0];
        String[] copy = ArrayUtilities.shallowCopy(empty);
        Assert.assertArrayEquals(empty, copy);
        Assert.assertNotSame(empty, copy);
    }

    // ---------- createArray edge cases ----------
    @Test
    public void testCreateArrayEmpty() {
        Object[] empty = ArrayUtilities.createArray();
        Assert.assertEquals(0, empty.length);
    }

    @Test
    public void testCreateArrayWithNullElements() {
        String[] arr = ArrayUtilities.createArray(null, "test", null);
        Assert.assertArrayEquals(new String[]{null, "test", null}, arr);
    }

    @Test(expected = NullPointerException.class)
    public void testCreateArrayNullVarArgsArray() {
        // Explicitly pass a null array reference as the var‑args argument.
        ArrayUtilities.createArray((String[]) null);
    }

    // ---------- addAll edge cases ----------
    @Test
    public void testAddAllBothNull() {
        Assert.assertNull(ArrayUtilities.addAll(null, null));
    }

    @Test
    public void testAddAllFirstNullSecondNonNull() {
        Integer[] second = {10, 20};
        Integer[] result = ArrayUtilities.addAll(null, second);
        // Should be a shallow copy of second
        Assert.assertArrayEquals(second, result);
        Assert.assertNotSame(second, result);
    }

    @Test
    public void testAddAllSecondNullFirstNonNull() {
        String[] first = {"alpha"};
        String[] result = ArrayUtilities.addAll(first, null);
        Assert.assertArrayEquals(first, result);
        Assert.assertNotSame(first, result);
    }

    @Test
    public void testAddAllBothEmpty() {
        Integer[] empty1 = new Integer[0];
        Integer[] empty2 = new Integer[0];
        Integer[] result = ArrayUtilities.addAll(empty1, empty2);
        Assert.assertArrayEquals(new Integer[0], result);
    }

    // ---------- removeItem edge cases ----------
    @Test
    public void testRemoveItemFirstElement() {
        String[] source = {"first", "second", "third"};
        String[] result = ArrayUtilities.removeItem(source, 0);
        Assert.assertArrayEquals(new String[]{"second", "third"}, result);
    }

    @Test
    public void testRemoveItemLastElement() {
        String[] source = {"first", "second", "third"};
        String[] result = ArrayUtilities.removeItem(source, 2);
        Assert.assertArrayEquals(new String[]{"first", "second"}, result);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testRemoveItemNegativeIndex() {
        Integer[] source = {1, 2, 3};
        ArrayUtilities.removeItem(source, -1);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testRemoveItemIndexTooLarge() {
        Integer[] source = {1, 2, 3};
        ArrayUtilities.removeItem(source, 3); // length is 3, valid indices are 0‑2
    }

    @Test(expected = NullPointerException.class)
    public void testRemoveItemNullArray() {
        ArrayUtilities.removeItem(null, 0);
    }

    // ---------- getArraySubset edge cases ----------
    @Test
    public void testGetArraySubsetFullCopy() {
        Double[] src = {0.1, 0.2, 0.3};
        Double[] copy = ArrayUtilities.getArraySubset(src, 0, src.length);
        Assert.assertArrayEquals(src, copy);
        Assert.assertNotSame(src, copy);
    }

    @Test
    public void testGetArraySubsetEmptyRange() {
        String[] src = {"a", "b", "c"};
        String[] empty = ArrayUtilities.getArraySubset(src, 1, 1);
        Assert.assertEquals(0, empty.length);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetArraySubsetStartNegative() {
        Integer[] src = {1, 2, 3};
        ArrayUtilities.getArraySubset(src, -1, 2);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetArraySubsetEndTooLarge() {
        Integer[] src = {1, 2, 3};
        ArrayUtilities.getArraySubset(src, 0, 4);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetArraySubsetStartGreaterThanEnd() {
        Integer[] src = {1, 2, 3};
        ArrayUtilities.getArraySubset(src, 2, 1);
    }

    @Test(expected = NullPointerException.class)
    public void testGetArraySubsetNullArray() {
        ArrayUtilities.getArraySubset(null, 0, 1);
    }

    // ---------- toArray edge cases ----------
    @Test
    public void testToArrayEmptyCollection() {
        List<String> empty = Collections.emptyList();
        String[] result = ArrayUtilities.toArray(String.class, empty);
        Assert.assertEquals(0, result.length);
    }

    @Test
    public void testToArrayWithNullElements() {
        List<Object> list = new ArrayList<>();
        list.add(null);
        list.add("value");
        Object[] result = ArrayUtilities.toArray(Object.class, list);
        Assert.assertArrayEquals(new Object[]{null, "value"}, result);
    }

    @Test(expected = NullPointerException.class)
    public void testToArrayNullClass() {
        List<Integer> list = List.of(1, 2);
        ArrayUtilities.toArray(null, list);
    }

    @Test(expected = NullPointerException.class)
    public void testToArrayNullCollection() {
        ArrayUtilities.toArray(String.class, null);
    }
}
