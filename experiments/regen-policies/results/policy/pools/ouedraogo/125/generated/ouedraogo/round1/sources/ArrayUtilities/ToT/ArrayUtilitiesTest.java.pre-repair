package com.cedarsoftware.util;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * JUnit 4 test suite for {@link ArrayUtilities}.
 *
 * <p>This test class exercises every public static method in {@code ArrayUtilities}
 * covering typical usage, boundary conditions and expected error scenarios.
 * The tests are deliberately written without any external mocking frameworks,
 * using only the API that is visible in the source file.</p>
 *
 * @author  ChatGPT
 */
public class ArrayUtilitiesTest {

    /* ---------------------------------------------------------------------- */
    /*  isEmpty()                                                             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIsEmpty_NullArray() {
        assertTrue("null should be considered empty", ArrayUtilities.isEmpty(null));
    }

    @Test
    public void testIsEmpty_EmptyObjectArray() {
        Object[] empty = new Object[0];
        assertTrue("empty Object[] should be empty", ArrayUtilities.isEmpty(empty));
    }

    @Test
    public void testIsEmpty_NonEmptyPrimitiveArray() {
        int[] ints = {1, 2, 3};
        assertFalse("non‑empty int[] should not be empty", ArrayUtilities.isEmpty(ints));
    }

    @Test
    public void testIsEmpty_EmptyPrimitiveArray() {
        byte[] empty = new byte[0];
        assertTrue("empty byte[] should be empty", ArrayUtilities.isEmpty(empty));
    }

    /* ---------------------------------------------------------------------- */
    /*  size()                                                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testSize_NullArray() {
        assertEquals("null array size should be 0", 0, ArrayUtilities.size(null));
    }

    @Test
    public void testSize_EmptyArray() {
        String[] empty = new String[0];
        assertEquals("empty array size should be 0", 0, ArrayUtilities.size(empty));
    }

    @Test
    public void testSize_NonEmptyArray() {
        Double[] values = {0.1, 0.2, 0.3, 0.4};
        assertEquals("size should match number of elements", 4, ArrayUtilities.size(values));
    }

    @Test
    public void testSize_PrimitiveArray() {
        char[] chars = {'a', 'b', 'c'};
        assertEquals("size of primitive array", 3, ArrayUtilities.size(chars));
    }

    /* ---------------------------------------------------------------------- */
    /*  shallowCopy()                                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testShallowCopy_NullInput() {
        assertNull("shallowCopy(null) must return null", ArrayUtilities.shallowCopy(null));
    }

    @Test
    public void testShallowCopy_EmptyArray() {
        Integer[] empty = new Integer[0];
        Integer[] copy = ArrayUtilities.shallowCopy(empty);
        assertNotSame("clone should be a new instance", empty, copy);
        assertArrayEquals("contents of cloned empty array", empty, copy);
    }

    @Test
    public void testShallowCopy_ModifyOriginalDoesNotAffectCopy() {
        String[] original = {"one", "two"};
        String[] copy = ArrayUtilities.shallowCopy(original);
        original[0] = "changed";
        // shallow copy means the array object is different, but elements are same references.
        assertEquals("changed", original[0]);
        assertEquals("one", copy[0]); // copy still holds original reference to "one"
    }

    /* ---------------------------------------------------------------------- */
    /*  createArray()                                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCreateArray_Varargs() {
        String[] result = ArrayUtilities.createArray("a", "b", "c");
        assertArrayEquals(new String[]{"a", "b", "c"}, result);
    }

    @Test
    public void testCreateArray_EmptyVarargs() {
        Integer[] result = ArrayUtilities.createArray();
        assertNotNull("Result must not be null", result);
        assertEquals("Length of empty varargs array", 0, result.length);
    }

    @Test
    public void testCreateArray_WithNullElements() {
        Object[] result = ArrayUtilities.createArray(null, "x", null);
        assertArrayEquals(new Object[]{null, "x", null}, result);
    }

    /* ---------------------------------------------------------------------- */
    /*  addAll()                                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAddAll_BothNull() {
        assertNull("addAll(null, null) should return null", ArrayUtilities.addAll(null, null));
    }

    @Test
    public void testAddAll_FirstNullSecondNonNull() {
        String[] second = {"z"};
        String[] result = ArrayUtilities.addAll(null, second);
        assertArrayEquals(second, result);
        assertNotSame("Result must be a clone, not the original reference", second, result);
    }

    @Test
    public void testAddAll_FirstNonNullSecondNull() {
        Integer[] first = {1, 2, 3};
        Integer[] result = ArrayUtilities.addAll(first, null);
        assertArrayEquals(first, result);
        assertNotSame("Result must be a clone, not the original reference", first, result);
    }

    @Test
    public void testAddAll_BothNonNull() {
        String[] a = {"a", "b"};
        String[] b = {"c", "d", "e"};
        String[] expected = {"a", "b", "c", "d", "e"};
        String[] result = ArrayUtilities.addAll(a, b);
        assertArrayEquals(expected, result);
        // Ensure new array is not the same instance as either source
        assertNotSame(a, result);
        assertNotSame(b, result);
    }

    @Test
    public void testAddAll_WithNullElementsInsideArrays() {
        String[] a = {null, "x"};
        String[] b = {"y", null};
        String[] expected = {null, "x", "y", null};
        String[] result = ArrayUtilities.addAll(a, b);
        assertArrayEquals(expected, result);
    }

    /* ---------------------------------------------------------------------- */
    /*  removeItem()                                                           */
    /* ---------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testRemoveItem_NullArray() {
        ArrayUtilities.removeItem(null, 0);
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

    @Test
    public void testRemoveItem_RemoveFirstElement() {
        String[] source = {"a", "b", "c"};
        String[] expected = {"b", "c"};
        assertArrayEquals(expected, ArrayUtilities.removeItem(source, 0));
    }

    @Test
    public void testRemoveItem_RemoveMiddleElement() {
        Integer[] source = {10, 20, 30, 40};
        Integer[] expected = {10, 20, 40};
        assertArrayEquals(expected, ArrayUtilities.removeItem(source, 2));
    }

    @Test
    public void testRemoveItem_RemoveLastElement() {
        Character[] source = {'x', 'y', 'z'};
        Character[] expected = {'x', 'y'};
        assertArrayEquals(expected, ArrayUtilities.removeItem(source, 2));
    }

    /* ---------------------------------------------------------------------- */
    /*  getArraySubset()                                                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetArraySubset_FullRange() {
        Long[] source = {1L, 2L, 3L, 4L};
        assertArrayEquals(source, ArrayUtilities.getArraySubset(source, 0, source.length));
    }

    @Test
    public void testGetArraySubset_PartialRange() {
        String[] source = {"zero", "one", "two", "three", "four"};
        String[] expected = {"one", "two", "three"};
        assertArrayEquals(expected, ArrayUtilities.getArraySubset(source, 1, 4));
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetArraySubset_StartNegative() {
        Integer[] source = {1, 2, 3};
        ArrayUtilities.getArraySubset(source, -1, 2);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetArraySubset_EndBeyondLength() {
        Integer[] source = {1, 2, 3};
        ArrayUtilities.getArraySubset(source, 0, 5);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetArraySubset_StartGreaterThanEnd() {
        Integer[] source = {1, 2, 3};
        ArrayUtilities.getArraySubset(source, 2, 1);
    }

    @Test(expected = NullPointerException.class)
    public void testGetArraySubset_NullArray() {
        ArrayUtilities.getArraySubset(null, 0, 1);
    }

    /* ---------------------------------------------------------------------- */
    /*  toArray()                                                              */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToArray_StringClass() {
        Collection<String> coll = Arrays.asList("alpha", "beta");
        String[] result = ArrayUtilities.toArray(String.class, coll);
        assertArrayEquals(new String[]{"alpha", "beta"}, result);
    }

    @Test
    public void testToArray_WithDifferentRuntimeTypes() {
        // Collection of objects that are subclasses of Number
        Collection<Number> coll = Arrays.asList(Integer.valueOf(5), Double.valueOf(2.5));
        Number[] result = ArrayUtilities.toArray(Number.class, coll);
        assertEquals(2, result.length);
        assertTrue(result[0] instanceof Integer);
        assertTrue(result[1] instanceof Double);
        assertArrayEquals(new Number[]{5, 2.5}, result);
    }

    @Test
    public void testToArray_EmptyCollection() {
        Collection<Object> empty = Collections.emptyList();
        Object[] result = ArrayUtilities.toArray(Object.class, empty);
        assertNotNull("Result array must not be null", result);
        assertEquals(0, result.length);
    }

    @Test
    public void testToArray_CollectionWithNullElements() {
        Collection<String> coll = Arrays.asList("first", null, "third");
        String[] result = ArrayUtilities.toArray(String.class, coll);
        assertArrayEquals(new String[]{"first", null, "third"}, result);
    }

    @Test(expected = NullPointerException.class)
    public void testToArray_NullCollection() {
        // According to JDK, Collection.toArray(null) throws NPE; we follow that contract.
        ArrayUtilities.toArray(Integer.class, null);
    }

    @Test(expected = NullPointerException.class)
    public void testToArray_NullClassArgument() {
        Collection<Integer> coll = Arrays.asList(1, 2, 3);
        // The method will attempt to call Array.newInstance with a null class, causing NPE.
        ArrayUtilities.toArray(null, coll);
    }

    /* ---------------------------------------------------------------------- */
    /*  Constants sanity checks (optional)                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testEmptyObjectArray_IsImmutable() {
        assertEquals(0, ArrayUtilities.EMPTY_OBJECT_ARRAY.length);
        // Attempt to modify should throw ArrayIndexOutOfBoundsException
        try {
            ArrayUtilities.EMPTY_OBJECT_ARRAY[0] = new Object();
            fail("Modifying EMPTY_OBJECT_ARRAY should throw exception");
        } catch (ArrayIndexOutOfBoundsException ignored) {
        }
    }

    @Test
    public void testEmptyByteArray_IsImmutable() {
        assertEquals(0, ArrayUtilities.EMPTY_BYTE_ARRAY.length);
        try {
            ArrayUtilities.EMPTY_BYTE_ARRAY[0] = (byte) 1;
            fail("Modifying EMPTY_BYTE_ARRAY should throw exception");
        } catch (ArrayIndexOutOfBoundsException ignored) {
        }
    }
}
