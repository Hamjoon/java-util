package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;

public class ArrayUtilitiesTest {

    /* ---------------------------------------------------------------------- */
    /*  isEmpty()                                                             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIsEmpty_Null() {
        assertTrue("null should be considered empty", ArrayUtilities.isEmpty(null));
    }

    @Test
    public void testIsEmpty_EmptyObjectArray() {
        assertTrue("empty Object[] should be considered empty",
                ArrayUtilities.isEmpty(new Object[0]));
    }

    @Test
    public void testIsEmpty_EmptyPrimitiveArray() {
        assertTrue("empty int[] should be considered empty",
                ArrayUtilities.isEmpty(new int[0]));
    }

    @Test
    public void testIsEmpty_NonEmptyArray() {
        String[] data = {"a", "b"};
        assertFalse("non‑empty array should NOT be considered empty",
                ArrayUtilities.isEmpty(data));
    }

    /* ---------------------------------------------------------------------- */
    /*  size()                                                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testSize_Null() {
        assertEquals("null array size must be 0", 0, ArrayUtilities.size(null));
    }

    @Test
    public void testSize_EmptyArray() {
        assertEquals("empty array size must be 0", 0, ArrayUtilities.size(new byte[0]));
    }

    @Test
    public void testSize_NonEmptyArray() {
        Integer[] ints = {1, 2, 3, 4};
        assertEquals("size should match length of the array", 4, ArrayUtilities.size(ints));
    }

    /* ---------------------------------------------------------------------- */
    /*  shallowCopy()                                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testShallowCopy_Null() {
        assertNull("shallowCopy(null) must return null", ArrayUtilities.shallowCopy(null));
    }

    @Test
    public void testShallowCopy_ReturnsNewArray() {
        String[] original = {"x", "y", "z"};
        String[] copy = ArrayUtilities.shallowCopy(original);
        assertNotSame("Returned array must be a different instance", original, copy);
        assertArrayEquals("Contents must be identical", original, copy);
    }

    @Test
    public void testShallowCopy_ModifyingOriginalDoesNotAffectCopy() {
        String[] original = {"first", "second"};
        String[] copy = ArrayUtilities.shallowCopy(original);
        original[0] = "changed";
        assertEquals("Copy should retain original first element", "first", copy[0]);
    }

    /* ---------------------------------------------------------------------- */
    /*  createArray(...)                                                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCreateArray_Varargs() {
        Integer[] result = ArrayUtilities.createArray(10, 20, 30);
        assertArrayEquals(new Integer[]{10, 20, 30}, result);
    }

    @Test
    public void testCreateArray_Empty() {
        String[] result = ArrayUtilities.createArray();
        assertEquals("Empty varargs should produce zero‑length array", 0, result.length);
    }

    @Test
    public void testCreateArray_WithNullElement() {
        String[] result = ArrayUtilities.createArray((String) null, "hello");
        assertNull("First element must be null", result[0]);
        assertEquals("Second element must be 'hello'", "hello", result[1]);
    }

    @Test(expected = NullPointerException.class)
    public void testCreateArray_NullArrayThrows() {
        // Force a null varargs array by casting null to the appropriate type.
        ArrayUtilities.<String>createArray((String[]) null);
    }

    /* ---------------------------------------------------------------------- */
    /*  addAll(...)                                                             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAddAll_BothNull() {
        assertNull("Both arguments null -> result null", ArrayUtilities.addAll(null, null));
    }

    @Test
    public void testAddAll_FirstNull() {
        String[] second = {"b", "c"};
        String[] result = ArrayUtilities.addAll(null, second);
        assertNotSame("Result must be a new array", second, result);
        assertArrayEquals("Result must equal the second array", second, result);
    }

    @Test
    public void testAddAll_SecondNull() {
        Integer[] first = {1, 2};
        Integer[] result = ArrayUtilities.addAll(first, null);
        assertNotSame("Result must be a new array", first, result);
        assertArrayEquals("Result must equal the first array", first, result);
    }

    @Test
    public void testAddAll_BothEmpty() {
        String[] empty1 = new String[0];
        String[] empty2 = new String[0];
        String[] result = ArrayUtilities.addAll(empty1, empty2);
        assertEquals("Result of two empty arrays must be empty", 0, result.length);
    }

    @Test
    public void testAddAll_WithNullElements() {
        String[] a = {null, "a"};
        String[] b = {"b", null};
        String[] result = ArrayUtilities.addAll(a, b);
        assertArrayEquals(new String[]{null, "a", "b", null}, result);
    }

    @Test
    public void testAddAll_RegularConcatenation() {
        String[] first = {"a", "b", "c"};
        String[] second = {"1", "2", "3"};
        String[] result = ArrayUtilities.addAll(first, second);
        assertArrayEquals(new String[]{"a", "b", "c", "1", "2", "3"}, result);
    }

    /* ---------------------------------------------------------------------- */
    /*  removeItem(...)                                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testRemoveItem_Middle() {
        String[] src = {"zero", "one", "two", "three"};
        String[] dst = ArrayUtilities.removeItem(src, 2); // remove "two"
        assertArrayEquals(new String[]{"zero", "one", "three"}, dst);
    }

    @Test
    public void testRemoveItem_FirstElement() {
        Integer[] src = {10, 20, 30};
        Integer[] dst = ArrayUtilities.removeItem(src, 0);
        assertArrayEquals(new Integer[]{20, 30}, dst);
    }

    @Test
    public void testRemoveItem_LastElement() {
        Character[] src = {'a', 'b', 'c'};
        Character[] dst = ArrayUtilities.removeItem(src, 2);
        assertArrayEquals(new Character[]{'a', 'b'}, dst);
    }

    @Test
    public void testRemoveItem_SingleElementArray() {
        Double[] src = {3.14};
        Double[] dst = ArrayUtilities.removeItem(src, 0);
        assertEquals("Result must be zero‑length array", 0, dst.length);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testRemoveItem_IndexOutOfBounds() {
        String[] src = {"only"};
        // This should throw because index 5 is beyond the array length.
        ArrayUtilities.removeItem(src, 5);
    }

    /* ---------------------------------------------------------------------- */
    /*  getArraySubset(...)                                                     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetArraySubset_NormalRange() {
        Integer[] src = {0, 1, 2, 3, 4, 5};
        Integer[] sub = ArrayUtilities.getArraySubset(src, 2, 5); // 2,3,4
        assertArrayEquals(new Integer[]{2, 3, 4}, sub);
    }

    @Test
    public void testGetArraySubset_EmptyResult() {
        String[] src = {"a", "b", "c"};
        String[] sub = ArrayUtilities.getArraySubset(src, 1, 1);
        assertEquals("Empty range must produce zero‑length array", 0, sub.length);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetArraySubset_StartNegative() {
        Integer[] src = {1, 2, 3};
        ArrayUtilities.getArraySubset(src, -1, 2);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetArraySubset_EndBeyondLength() {
        Integer[] src = {1, 2, 3};
        ArrayUtilities.getArraySubset(src, 0, 5);
    }

    /* ---------------------------------------------------------------------- */
    /*  toArray(...)                                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToArray_Basic() {
        List<String> list = new ArrayList<>();
        list.add("alpha");
        list.add("beta");
        String[] result = ArrayUtilities.toArray(String.class, list);
        assertArrayEquals(new String[]{"alpha", "beta"}, result);
    }

    @Test
    public void testToArray_EmptyCollection() {
        Collection<Integer> empty = new ArrayList<>();
        Integer[] result = ArrayUtilities.toArray(Integer.class, empty);
        assertEquals("Result array must have length 0", 0, result.length);
    }

    @Test
    public void testToArray_WithSubclassElements() {
        // Collection of Integer, but we request Number[].  The method uses reflection
        // to allocate an array of the requested component type.
        List<Integer> numbers = new ArrayList<>();
        numbers.add(7);
        numbers.add(42);
        Number[] result = ArrayUtilities.toArray(Number.class, numbers);
        assertArrayEquals(new Number[]{7, 42}, result);
        // Verify runtime type of the array
        assertEquals("Component type must be Number", Number.class, result.getClass().getComponentType());
    }

    @Test(expected = NullPointerException.class)
    public void testToArray_NullCollectionThrows() {
        // Passing a null collection will cause a NPE inside the method.
        ArrayUtilities.toArray(Object.class, null);
    }
}
