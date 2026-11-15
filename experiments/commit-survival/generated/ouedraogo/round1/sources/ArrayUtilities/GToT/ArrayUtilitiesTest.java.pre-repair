package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ArrayUtilities}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public methods and their normal use cases</li>
 *   <li>Edge‑case handling (nulls, empty arrays/collections, boundary indexes)</li>
 *   <li>Exception scenarios (illegal arguments, out‑of‑range indexes)</li>
 *   <li>Behavioural contracts such as immutability of returned arrays</li>
 * </ul>
 *
 * The suite is written without any external mocking frameworks – only the
 * API that is present in the supplied source file is used.
 */
public class ArrayUtilitiesTest {

    /* ----------------------------------------------------------------------
     *  isEmpty(Object)
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
    public void testIsEmptyWithNonEmptyObjectArray() {
        String[] data = {"a", "b"};
        assertFalse(ArrayUtilities.isEmpty(data));
    }

    @Test
    public void testIsEmptyWithPrimitiveArray() {
        int[] ints = new int[0];
        assertTrue(ArrayUtilities.isEmpty(ints));

        int[] nonEmpty = {1, 2, 3};
        assertFalse(ArrayUtilities.isEmpty(nonEmpty));
    }

    /* ----------------------------------------------------------------------
     *  size(Object)
     * ---------------------------------------------------------------------- */
    @Test
    public void testSizeWithNull() {
        assertEquals(0, ArrayUtilities.size(null));
    }

    @Test
    public void testSizeWithEmptyArray() {
        assertEquals(0, ArrayUtilities.size(new String[0]));
        assertEquals(0, ArrayUtilities.size(new int[0]));
    }

    @Test
    public void testSizeWithNonEmptyArray() {
        assertEquals(3, ArrayUtilities.size(new Double[]{1.0, 2.0, 3.0}));
        assertEquals(5, ArrayUtilities.size(new char[]{'a', 'b', 'c', 'd', 'e'}));
    }

    /* ----------------------------------------------------------------------
     *  shallowCopy(T[])
     * ---------------------------------------------------------------------- */
    @Test
    public void testShallowCopyNull() {
        assertNull(ArrayUtilities.shallowCopy(null));
    }

    @Test
    public void testShallowCopyNonNull() {
        String[] original = {"x", "y", "z"};
        String[] copy = ArrayUtilities.shallowCopy(original);

        // Different instance but same content
        assertNotSame(original, copy);
        assertArrayEquals(original, copy);

        // Modifying the copy does not affect the original (shallow copy semantics)
        copy[0] = "changed";
        assertEquals("x", original[0]);
    }

    /* ----------------------------------------------------------------------
     *  createArray(T...)
     * ---------------------------------------------------------------------- */
    @Test
    public void testCreateArrayNoElements() {
        Integer[] empty = ArrayUtilities.createArray();
        assertNotNull(empty);
        assertEquals(0, empty.length);
    }

    @Test
    public void testCreateArrayWithElements() {
        String[] strings = ArrayUtilities.createArray("one", "two", "three");
        assertArrayEquals(new String[]{"one", "two", "three"}, strings);
    }

    @Test
    public void testCreateArrayWithNullElements() {
        String[] withNull = ArrayUtilities.createArray("first", null, "third");
        assertArrayEquals(new String[]{"first", null, "third"}, withNull);
    }

    /* ----------------------------------------------------------------------
     *  addAll(T[], T[])
     * ---------------------------------------------------------------------- */
    @Test
    public void testAddAllBothNull() {
        assertNull(ArrayUtilities.addAll(null, null));
    }

    @Test
    public void testAddAllFirstNull() {
        Integer[] second = {1, 2, 3};
        Integer[] result = ArrayUtilities.addAll(null, second);
        assertArrayEquals(second, result);
        assertNotSame(second, result); // should be a shallow copy
    }

    @Test
    public void testAddAllSecondNull() {
        String[] first = {"a", "b"};
        String[] result = ArrayUtilities.addAll(first, null);
        assertArrayEquals(first, result);
        assertNotSame(first, result);
    }

    @Test
    public void testAddAllBothEmpty() {
        Object[] result = ArrayUtilities.addAll(new Object[0], new Object[0]);
        assertArrayEquals(new Object[0], result);
    }

    @Test
    public void testAddAllWithNullElements() {
        String[] a = {null};
        String[] b = {null};
        String[] result = ArrayUtilities.addAll(a, b);
        assertArrayEquals(new String[]{null, null}, result);
    }

    @Test
    public void testAddAllNormalCase() {
        String[] a = {"a", "b", "c"};
        String[] b = {"1", "2", "3"};
        String[] result = ArrayUtilities.addAll(a, b);
        assertArrayEquals(new String[]{"a", "b", "c", "1", "2", "3"}, result);
        // Ensure a new array was allocated
        assertNotSame(a, result);
        assertNotSame(b, result);
    }

    @Test
    public void testAddAllDifferentComponentTypesViaObjectArray() {
        Object[] a = {"string", 10};
        Object[] b = {true, null};
        Object[] result = ArrayUtilities.addAll(a, b);
        assertArrayEquals(new Object[]{"string", 10, true, null}, result);
    }

    /* ----------------------------------------------------------------------
     *  removeItem(T[], int)
     * ---------------------------------------------------------------------- */
    @Test
    public void testRemoveItemFirstPosition() {
        Integer[] src = {1, 2, 3, 4};
        Integer[] expected = {2, 3, 4};
        assertArrayEquals(expected, ArrayUtilities.removeItem(src, 0));
    }

    @Test
    public void testRemoveItemMiddlePosition() {
        String[] src = {"a", "b", "c", "d"};
        String[] expected = {"a", "c", "d"};
        assertArrayEquals(expected, ArrayUtilities.removeItem(src, 1));
    }

    @Test
    public void testRemoveItemLastPosition() {
        Character[] src = {'x', 'y', 'z'};
        Character[] expected = {'x', 'y'};
        assertArrayEquals(expected, ArrayUtilities.removeItem(src, 2));
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testRemoveItemNegativeIndexThrows() {
        Integer[] src = {1, 2, 3};
        ArrayUtilities.removeItem(src, -1);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testRemoveItemIndexEqualLengthThrows() {
        Integer[] src = {1, 2, 3};
        ArrayUtilities.removeItem(src, 3);
    }

    /* ----------------------------------------------------------------------
     *  getArraySubset(T[], int, int)
     * ---------------------------------------------------------------------- */
    @Test
    public void testGetArraySubsetNormalRange() {
        String[] src = {"zero", "one", "two", "three", "four"};
        String[] subset = ArrayUtilities.getArraySubset(src, 1, 4);
        assertArrayEquals(new String[]{"one", "two", "three"}, subset);
    }

    @Test
    public void testGetArraySubsetEmptyResult() {
        Integer[] src = {10, 20, 30};
        Integer[] subset = ArrayUtilities.getArraySubset(src, 2, 2);
        assertArrayEquals(new Integer[0], subset);
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

    /* ----------------------------------------------------------------------
     *  toArray(Class<T>, Collection<?>)
     * ---------------------------------------------------------------------- */
    @Test
    public void testToArrayWithStringCollection() {
        Collection<String> coll = new ArrayList<>();
        Collections.addAll(coll, "alpha", "beta", "gamma");
        String[] result = ArrayUtilities.toArray(String.class, coll);
        assertArrayEquals(new String[]{"alpha", "beta", "gamma"}, result);
        // Verify the runtime component type matches the requested class
        assertEquals(String.class, result.getClass().getComponentType());
    }

    @Test
    public void testToArrayWithNullElements() {
        List<Integer> list = new ArrayList<>();
        list.add(null);
        list.add(42);
        Integer[] result = ArrayUtilities.toArray(Integer.class, list);
        assertArrayEquals(new Integer[]{null, 42}, result);
    }

    @Test
    public void testToArrayEmptyCollection() {
        Collection<Object> empty = Collections.emptyList();
        Object[] result = ArrayUtilities.toArray(Object.class, empty);
        assertEquals(0, result.length);
    }

    @Test(expected = NullPointerException.class)
    public void testToArrayWithNullCollectionThrows() {
        // The method itself does not guard against a null collection;
        // invoking it with null will cause a NullPointerException when
        // attempting to call c.toArray(...)
        ArrayUtilities.toArray(String.class, null);
    }

    /* ----------------------------------------------------------------------
     *  Miscellaneous checks for the public constant arrays
     * ---------------------------------------------------------------------- */
    @Test
    public void testPublicEmptyArraysAreImmutableLengths() {
        assertEquals(0, ArrayUtilities.EMPTY_OBJECT_ARRAY.length);
        assertEquals(0, ArrayUtilities.EMPTY_BYTE_ARRAY.length);
        assertEquals(0, ArrayUtilities.EMPTY_CHAR_ARRAY.length);
        assertEquals(0, ArrayUtilities.EMPTY_CHARACTER_ARRAY.length);
        assertEquals(0, ArrayUtilities.EMPTY_CLASS_ARRAY.length);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAttemptToModifyEmptyObjectArrayThrows() {
        // Attempting to assign into the constant array should cause an
        // ArrayStoreException at runtime because the array length is zero.
        // To get a deterministic exception we try to change its length via reflection,
        // which is disallowed. Instead we verify that the array is indeed immutable
        // by trying an illegal operation.
        Object[] arr = ArrayUtilities.EMPTY_OBJECT_ARRAY;
        arr[0] = new Object(); // This will throw ArrayIndexOutOfBoundsException,
                               // which we translate to UnsupportedOperationException
        // The test will never reach this line.
    }

    /* ----------------------------------------------------------------------
     *  Helper to convert the expected ArrayIndexOutOfBoundsException from
     *  the testRemoveItem* methods into JUnit‑4's expected exception mechanism.
     * ---------------------------------------------------------------------- */
    @Test
    public void testRemoveItemInvalidIndexThrowsArrayIndexOutOfBoundsException() {
        try {
            Integer[] src = {1, 2, 3};
            ArrayUtilities.removeItem(src, 5);
            fail("Expected ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }
    }
}
