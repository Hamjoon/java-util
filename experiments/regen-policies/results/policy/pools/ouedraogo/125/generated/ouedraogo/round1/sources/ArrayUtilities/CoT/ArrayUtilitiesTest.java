package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link ArrayUtilities}.
 *
 * The test file is generated in several steps:
 * 1. List of public static methods (signatures).
 * 2. Basic functional tests for each method.
 * 3. Identification of edge‑cases / exception scenarios.
 * 4. Specific tests that cover those edge‑cases.
 * 5. All tests merged into a single class.
 */
public class ArrayUtilitiesTest {

    /* ---------------------------------------------------------------------- */
    /* 1. PUBLIC METHOD SIGNATURES                                            */
    /* ---------------------------------------------------------------------- */
    // boolean isEmpty(Object array)
    // int size(Object array)
    // <T> T[] shallowCopy(T[] array)
    // <T> T[] createArray(T... elements)
    // <T> T[] addAll(T[] array1, T[] array2)
    // <T> T[] removeItem(T[] array, int pos)
    // <T> T[] getArraySubset(T[] array, int start, int end)
    // <T> T[] toArray(Class<T> classToCastTo, Collection<?> c)

    /* ---------------------------------------------------------------------- */
    /* 2. BASIC FUNCTIONAL TESTS                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIsEmpty() {
        assertTrue(ArrayUtilities.isEmpty(null));
        assertTrue(ArrayUtilities.isEmpty(new Object[0]));
        assertTrue(ArrayUtilities.isEmpty(new int[0]));
        assertFalse(ArrayUtilities.isEmpty(new String[]{"a"}));
        assertFalse(ArrayUtilities.isEmpty(new int[]{1, 2, 3}));
    }

    @Test
    public void testSize() {
        assertEquals(0, ArrayUtilities.size(null));
        assertEquals(0, ArrayUtilities.size(new Object[0]));
        assertEquals(3, ArrayUtilities.size(new Integer[]{1, 2, 3}));
        assertEquals(5, ArrayUtilities.size(new int[]{1, 2, 3, 4, 5}));
    }

    @Test
    public void testShallowCopy() {
        String[] original = {"one", "two"};
        String[] copy = ArrayUtilities.shallowCopy(original);
        assertNotSame(original, copy);
        assertArrayEquals(original, copy);

        // null input must return null
        assertNull(ArrayUtilities.shallowCopy(null));
    }

    @Test
    public void testCreateArray() {
        String[] arr = ArrayUtilities.createArray("a", "b", "c");
        assertArrayEquals(new String[]{"a", "b", "c"}, arr);

        Integer[] ints = ArrayUtilities.createArray(1, 2, 3, 4);
        assertArrayEquals(new Integer[]{1, 2, 3, 4}, ints);
    }

    @Test
    public void testAddAll() {
        // both null → null
        assertNull(ArrayUtilities.addAll(null, null));

        // one null → shallow copy of the non‑null array
        String[] a = {"a", "b"};
        String[] copyA = ArrayUtilities.addAll(a, null);
        assertNotSame(a, copyA);
        assertArrayEquals(a, copyA);

        String[] copyB = ArrayUtilities.addAll(null, a);
        assertNotSame(a, copyB);
        assertArrayEquals(a, copyB);

        // both empty
        String[] empty1 = new String[0];
        String[] empty2 = new String[0];
        String[] resultEmpty = ArrayUtilities.addAll(empty1, empty2);
        assertArrayEquals(new String[0], resultEmpty);

        // normal concatenation
        String[] first = {"x", "y"};
        String[] second = {"1", "2", "3"};
        String[] combined = ArrayUtilities.addAll(first, second);
        assertArrayEquals(new String[]{"x", "y", "1", "2", "3"}, combined);
    }

    @Test
    public void testRemoveItem() {
        Integer[] src = {10, 20, 30, 40, 50};

        // remove middle element (index 2)
        Integer[] removed = ArrayUtilities.removeItem(src, 2);
        assertArrayEquals(new Integer[]{10, 20, 40, 50}, removed);

        // remove first element
        Integer[] removedFirst = ArrayUtilities.removeItem(src, 0);
        assertArrayEquals(new Integer[]{20, 30, 40, 50}, removedFirst);

        // remove last element
        Integer[] removedLast = ArrayUtilities.removeItem(src, src.length - 1);
        assertArrayEquals(new Integer[]{10, 20, 30, 40}, removedLast);
    }

    @Test
    public void testGetArraySubset() {
        String[] src = {"a", "b", "c", "d", "e"};
        String[] sub = ArrayUtilities.getArraySubset(src, 1, 4);
        assertArrayEquals(new String[]{"b", "c", "d"}, sub);
    }

    @Test
    public void testToArray() {
        List<String> list = new ArrayList<>();
        Collections.addAll(list, "x", "y", "z");
        String[] arr = ArrayUtilities.toArray(String.class, list);
        assertArrayEquals(new String[]{"x", "y", "z"}, arr);
        assertEquals(String.class, arr.getClass().getComponentType());

        // empty collection
        List<Integer> empty = Collections.emptyList();
        Integer[] emptyArr = ArrayUtilities.toArray(Integer.class, empty);
        assertEquals(0, emptyArr.length);
    }

    /* ---------------------------------------------------------------------- */
    /* 3. EDGE‑CASE & EXCEPTION IDENTIFICATION                               */
    /* ---------------------------------------------------------------------- */
    // createArray(null) → NullPointerException
    // addAll with arrays containing null elements – ensure they are preserved
    // removeItem with invalid index → ArrayIndexOutOfBoundsException
    // removeItem with null array → NullPointerException
    // getArraySubset with start == end → empty array
    // getArraySubset with out‑of‑bounds indices → ArrayIndexOutOfBoundsException
    // getArraySubset with start > end → ArrayIndexOutOfBoundsException
    // toArray with collection containing null values – null should appear in result
    // toArray with null collection → NullPointerException (as per Collection contract)

    /* ---------------------------------------------------------------------- */
    /* 4. SPECIFIC EDGE‑CASE / EXCEPTION TESTS                               */
    /* ---------------------------------------------------------------------- */

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testCreateArray_NullArgumentThrowsNPE() {
        thrown.expect(NullPointerException.class);
        // Passing a single null reference to varargs makes the varargs array itself null
        ArrayUtilities.createArray((String) null);
    }

    @Test
    public void testAddAll_PreserveNullElements() {
        String[] a = {null, "a"};
        String[] b = {"b", null};
        String[] result = ArrayUtilities.addAll(a, b);
        assertArrayEquals(new String[]{null, "a", "b", null}, result);
    }

    @Test
    public void testRemoveItem_NullArrayThrowsNPE() {
        thrown.expect(NullPointerException.class);
        ArrayUtilities.removeItem(null, 0);
    }

    @Test
    public void testRemoveItem_NegativeIndexThrowsAIOOBE() {
        thrown.expect(ArrayIndexOutOfBoundsException.class);
        Integer[] src = {1, 2, 3};
        ArrayUtilities.removeItem(src, -1);
    }

    @Test
    public void testRemoveItem_IndexEqualLengthThrowsAIOOBE() {
        thrown.expect(ArrayIndexOutOfBoundsException.class);
        Integer[] src = {1, 2, 3};
        ArrayUtilities.removeItem(src, src.length);
    }

    @Test
    public void testGetArraySubset_StartEqualsEndReturnsEmpty() {
        String[] src = {"a", "b", "c"};
        String[] sub = ArrayUtilities.getArraySubset(src, 2, 2);
        assertEquals(0, sub.length);
    }

    @Test
    public void testGetArraySubset_StartNegativeThrowsAIOOBE() {
        thrown.expect(ArrayIndexOutOfBoundsException.class);
        String[] src = {"a", "b", "c"};
        ArrayUtilities.getArraySubset(src, -1, 2);
    }

    @Test
    public void testGetArraySubset_EndGreaterThanLengthThrowsAIOOBE() {
        thrown.expect(ArrayIndexOutOfBoundsException.class);
        String[] src = {"a", "b", "c"};
        ArrayUtilities.getArraySubset(src, 0, 5);
    }

    @Test
    public void testGetArraySubset_StartGreaterThanEndThrowsAIOOBE() {
        thrown.expect(ArrayIndexOutOfBoundsException.class);
        String[] src = {"a", "b", "c"};
        ArrayUtilities.getArraySubset(src, 3, 2);
    }

    @Test
    public void testToArray_CollectionWithNullElements() {
        List<String> list = new ArrayList<>();
        list.add("first");
        list.add(null);
        list.add("last");
        String[] arr = ArrayUtilities.toArray(String.class, list);
        assertArrayEquals(new String[]{"first", null, "last"}, arr);
    }

    @Test
    public void testToArray_NullCollectionThrowsNPE() {
        thrown.expect(NullPointerException.class);
        ArrayUtilities.toArray(Integer.class, null);
    }
}
