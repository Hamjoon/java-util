package com.cedarsoftware.util;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Comprehensive JUnit4 test suite for {@link ArrayUtilities}.
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
    public void testIsEmptyWithNonEmptyObjectArray() {
        String[] arr = {"a", "b"};
        assertFalse(ArrayUtilities.isEmpty(arr));
    }

    @Test
    public void testIsEmptyWithEmptyPrimitiveArray() {
        int[] emptyInts = new int[0];
        assertTrue(ArrayUtilities.isEmpty(emptyInts));

        byte[] emptyBytes = new byte[0];
        assertTrue(ArrayUtilities.isEmpty(emptyBytes));
    }

    @Test
    public void testIsEmptyWithNonEmptyPrimitiveArray() {
        int[] ints = {1, 2, 3};
        assertFalse(ArrayUtilities.isEmpty(ints));

        char[] chars = {'x'};
        assertFalse(ArrayUtilities.isEmpty(chars));
    }

    /* ----------------------------------------------------------------------
     * size()
     * ---------------------------------------------------------------------- */

    @Test
    public void testSizeWithNull() {
        assertEquals(0, ArrayUtilities.size(null));
    }

    @Test
    public void testSizeWithObjectArray() {
        String[] arr = {"one", "two", "three"};
        assertEquals(3, ArrayUtilities.size(arr));
    }

    @Test
    public void testSizeWithPrimitiveArray() {
        double[] doubles = {0.1, 0.2};
        assertEquals(2, ArrayUtilities.size(doubles));
    }

    /* ----------------------------------------------------------------------
     * shallowCopy()
     * ---------------------------------------------------------------------- */

    @Test
    public void testShallowCopyNull() {
        assertNull(ArrayUtilities.shallowCopy(null));
    }

    @Test
    public void testShallowCopyCreatesNewArray() {
        Integer[] original = {1, 2, 3};
        Integer[] copy = ArrayUtilities.shallowCopy(original);
        assertNotSame(original, copy);
        assertArrayEquals(original, copy);
    }

    @Test
    public void testShallowCopyIsShallow() {
        StringBuilder sb = new StringBuilder("x");
        StringBuilder[] original = {sb};
        StringBuilder[] copy = ArrayUtilities.shallowCopy(original);
        // modify the inner object
        sb.append("y");
        assertEquals("xy", copy[0].toString());
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
    public void testCreateArrayWithNullElements() {
        Integer[] arr = ArrayUtilities.createArray(1, null, 3);
        assertEquals(3, arr.length);
        assertEquals(Integer.valueOf(1), arr[0]);
        assertNull(arr[1]);
        assertEquals(Integer.valueOf(3), arr[2]);
    }

    @Test
    public void testCreateArrayWithNoElementsReturnsEmptyArray() {
        Object[] empty = ArrayUtilities.createArray();
        assertNotNull(empty);
        assertEquals(0, empty.length);
    }

    @Test
    public void testCreateArrayNullVarargsThrowsNPE() {
        try {
            ArrayUtilities.createArray((String[]) null);
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
            // expected
        }
    }

    /* ----------------------------------------------------------------------
     * addAll()
     * ---------------------------------------------------------------------- */

    @Test
    public void testAddAllBothNullReturnsNull() {
        assertNull(ArrayUtilities.addAll(null, null));
    }

    @Test
    public void testAddAllFirstNullReturnsCopyOfSecond() {
        String[] second = {"x", "y"};
        String[] result = ArrayUtilities.addAll(null, second);
        assertNotSame(second, result);
        assertArrayEquals(second, result);
    }

    @Test
    public void testAddAllSecondNullReturnsCopyOfFirst() {
        Integer[] first = {1, 2, 3};
        Integer[] result = ArrayUtilities.addAll(first, null);
        assertNotSame(first, result);
        assertArrayEquals(first, result);
    }

    @Test
    public void testAddAllBothNonNullCombined() {
        String[] a = {"a", "b"};
        String[] b = {"c", "d"};
        String[] combined = ArrayUtilities.addAll(a, b);
        assertArrayEquals(new String[]{"a", "b", "c", "d"}, combined);
        // ensure new array is not same reference as inputs
        assertNotSame(a, combined);
        assertNotSame(b, combined);
    }

    @Test
    public void testAddAllWithEmptyArrays() {
        String[] empty1 = new String[0];
        String[] empty2 = new String[0];
        String[] result = ArrayUtilities.addAll(empty1, empty2);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void testAddAllWithNullElementsInsideArrays() {
        String[] a = {null, "a"};
        String[] b = {"b", null};
        String[] result = ArrayUtilities.addAll(a, b);
        assertArrayEquals(new String[]{null, "a", "b", null}, result);
    }

    /* ----------------------------------------------------------------------
     * removeItem()
     * ---------------------------------------------------------------------- */

    @Test
    public void testRemoveItemMiddleElement() {
        String[] source = {"first", "second", "third", "fourth"};
        String[] result = ArrayUtilities.removeItem(source, 2); // remove "third"
        assertArrayEquals(new String[]{"first", "second", "fourth"}, result);
        // original unchanged
        assertArrayEquals(new String[]{"first", "second", "third", "fourth"}, source);
    }

    @Test
    public void testRemoveItemFirstElement() {
        Integer[] source = {10, 20, 30};
        Integer[] result = ArrayUtilities.removeItem(source, 0);
        assertArrayEquals(new Integer[]{20, 30}, result);
    }

    @Test
    public void testRemoveItemLastElement() {
        Character[] source = {'a', 'b', 'c'};
        Character[] result = ArrayUtilities.removeItem(source, 2);
        assertArrayEquals(new Character[]{'a', 'b'}, result);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testRemoveItemNegativeIndexThrows() {
        String[] source = {"x", "y"};
        ArrayUtilities.removeItem(source, -1);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testRemoveItemIndexEqualsLengthThrows() {
        String[] source = {"x", "y"};
        ArrayUtilities.removeItem(source, 2);
    }

    @Test(expected = NullPointerException.class)
    public void testRemoveItemNullArrayThrows() {
        ArrayUtilities.removeItem(null, 0);
    }

    /* ----------------------------------------------------------------------
     * getArraySubset()
     * ---------------------------------------------------------------------- */

    @Test
    public void testGetArraySubsetNormalRange() {
        String[] source = {"a", "b", "c", "d", "e"};
        String[] subset = ArrayUtilities.getArraySubset(source, 1, 4);
        assertArrayEquals(new String[]{"b", "c", "d"}, subset);
    }

    @Test
    public void testGetArraySubsetFullRangeReturnsCopy() {
        Integer[] source = {1, 2, 3};
        Integer[] subset = ArrayUtilities.getArraySubset(source, 0, source.length);
        assertArrayEquals(source, subset);
        assertNotSame(source, subset);
    }

    @Test
    public void testGetArraySubsetEmptyRange() {
        String[] source = {"only"};
        String[] subset = ArrayUtilities.getArraySubset(source, 0, 0);
        assertEquals(0, subset.length);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetArraySubsetStartNegativeThrows() {
        String[] source = {"a", "b"};
        ArrayUtilities.getArraySubset(source, -1, 1);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetArraySubsetEndTooLargeThrows() {
        String[] source = {"a", "b"};
        ArrayUtilities.getArraySubset(source, 0, 3);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetArraySubsetStartGreaterThanEndThrows() {
        String[] source = {"a", "b", "c"};
        ArrayUtilities.getArraySubset(source, 2, 1);
    }

    @Test(expected = NullPointerException.class)
    public void testGetArraySubsetNullArrayThrows() {
        ArrayUtilities.getArraySubset(null, 0, 1);
    }

    /* ----------------------------------------------------------------------
     * toArray()
     * ---------------------------------------------------------------------- */

    @Test
    public void testToArrayWithStringCollection() {
        Collection<String> coll = new ArrayList<>();
        coll.add("alpha");
        coll.add("beta");
        String[] result = ArrayUtilities.toArray(String.class, coll);
        assertArrayEquals(new String[]{"alpha", "beta"}, result);
    }

    @Test
    public void testToArrayWithIntegerCollection() {
        Collection<Integer> coll = Collections.singletonList(42);
        Integer[] result = ArrayUtilities.toArray(Integer.class, coll);
        assertArrayEquals(new Integer[]{42}, result);
    }

    @Test
    public void testToArrayEmptyCollection() {
        Collection<Double> empty = Collections.emptyList();
        Double[] result = ArrayUtilities.toArray(Double.class, empty);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test(expected = NullPointerException.class)
    public void testToArrayNullCollectionThrows() {
        ArrayUtilities.toArray(Object.class, null);
    }

    @Test(expected = NullPointerException.class)
    public void testToArrayNullClassThrows() {
        Collection<String> coll = Collections.singletonList("test");
        ArrayUtilities.toArray(null, coll);
    }

    /* ----------------------------------------------------------------------
     * Immutable EMPTY_* arrays
     * ---------------------------------------------------------------------- */

    @Test
    public void testEmptyObjectArrayIsImmutableAndEmpty() {
        assertNotNull(ArrayUtilities.EMPTY_OBJECT_ARRAY);
        assertEquals(0, ArrayUtilities.EMPTY_OBJECT_ARRAY.length);
    }

    @Test
    public void testEmptyByteArrayLengthZero() {
        assertEquals(0, ArrayUtilities.EMPTY_BYTE_ARRAY.length);
    }

    @Test
    public void testEmptyCharArrayLengthZero() {
        assertEquals(0, ArrayUtilities.EMPTY_CHAR_ARRAY.length);
    }

    @Test
    public void testEmptyCharacterArrayLengthZero() {
        assertEquals(0, ArrayUtilities.EMPTY_CHARACTER_ARRAY.length);
    }

    @Test
    public void testEmptyClassArrayLengthZero() {
        assertEquals(0, ArrayUtilities.EMPTY_CLASS_ARRAY.length);
    }
}
