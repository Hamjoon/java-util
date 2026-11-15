package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ArrayUtilities_removeItem_Test {

    private static Stream<Arguments> validRemovalProvider() {
        return Stream.of(// middle removal
        Arguments.of(new String[] { "apple", "banana", "cherry", "date" }, 2, new String[] { "apple", "banana", "date" }), // first element removal (Integer array)
        Arguments.of(new Integer[] { 1, 2, 3, 4, 5 }, 0, new Integer[] { 2, 3, 4, 5 }), // last element removal (String array)
        Arguments.of(new String[] { "x", "y", "z" }, 2, new String[] { "x", "y" }), // single‑element array becomes empty (Double)
        Arguments.of(new Double[] { 3.14 }, 0, new Double[] {}), // boundary: first element of two‑element array
        Arguments.of(new String[] { "first", "second" }, 0, new String[] { "second" }), // boundary: last element of two‑element array
        Arguments.of(new String[] { "first", "second" }, 1, new String[] { "first" }), // null elements – remove leading null
        Arguments.of(new Object[] { null, "a", null }, 0, new Object[] { "a", null }), // null elements – remove trailing null
        Arguments.of(new Object[] { null, "a", null }, 2, new Object[] { null, "a" }), // duplicate values – remove middle false
        Arguments.of(new Boolean[] { true, false, true }, 1, new Boolean[] { true, true }), // large array removal (int values boxed as Integer)
        Arguments.of(createIntegerRange(0, 99), 50, createIntegerRangeExcluding(0, 99, 50)));
    }

    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(// null array
        Arguments.of(null, 0, NullPointerException.class), // negative index
        Arguments.of(new Integer[] { 1, 2, 3 }, -1, ArrayIndexOutOfBoundsException.class), // index equal to length
        Arguments.of(new Integer[] { 1, 2, 3 }, 3, ArrayIndexOutOfBoundsException.class), // index greater than length
        Arguments.of(new Integer[] { 1, 2, 3 }, 5, ArrayIndexOutOfBoundsException.class), // empty array – any pos results in NegativeArraySizeException
        Arguments.of(new String[0], 0, NegativeArraySizeException.class), // empty array with negative pos (still NegativeArraySizeException)
        Arguments.of(new String[0], -1, NegativeArraySizeException.class), // empty array with positive out‑of‑range pos
        Arguments.of(new String[0], 1, NegativeArraySizeException.class));
    }

    /* -------------------------------------------------------------
     *  Helper methods to generate large test data
     * ------------------------------------------------------------- */
    private static Integer[] createIntegerRange(int startInclusive, int endInclusive) {
        int size = endInclusive - startInclusive + 1;
        Integer[] arr = new Integer[size];
        for (int i = 0; i < size; i++) {
            arr[i] = startInclusive + i;
        }
        return arr;
    }

    private static Integer[] createIntegerRangeExcluding(int startInclusive, int endInclusive, int exclude) {
        // one element removed
        int size = (endInclusive - startInclusive + 1) - 1;
        Integer[] arr = new Integer[size];
        int idx = 0;
        for (int i = startInclusive; i <= endInclusive; i++) {
            if (i == exclude)
                continue;
            arr[idx++] = i;
        }
        return arr;
    }

    @Test
    void removeItemFromMiddle() {
        String[] original = new String[] { "foo", "bar", "baz" };
        String[] result = ArrayUtilities.removeItem(original, 1);
        String[] expected = new String[] { "foo", "baz" };
        assertArrayEquals(expected, result);
        // original must stay unchanged
        assertEquals(3, original.length);
        assertArrayEquals(new String[] { "foo", "bar", "baz" }, original);
    }

    @Test
    void removeItemFromFirstAndLast() {
        Integer[] original = new Integer[] { 1, 2, 3, 4 };
        // remove first element (index 0)
        Integer[] afterFirst = ArrayUtilities.removeItem(original, 0);
        assertArrayEquals(new Integer[] { 2, 3, 4 }, afterFirst);
        // remove last element (index length-1)
        Integer[] afterLast = ArrayUtilities.removeItem(original, original.length - 1);
        assertArrayEquals(new Integer[] { 1, 2, 3 }, afterLast);
    }

    @Test
    void removeItemInvalidIndexThrows() {
        String[] original = new String[] { "a", "b", "c" };
        // negative index
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> ArrayUtilities.removeItem(original, -1));
        // index equal to length
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> ArrayUtilities.removeItem(original, original.length));
        // index greater than length
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> ArrayUtilities.removeItem(original, original.length + 5));
    }

    @Test
    void removeItemFromSingleElementArrayResultsInEmptyArray() {
        Double[] original = new Double[] { 3.14 };
        Double[] result = ArrayUtilities.removeItem(original, 0);
        assertEquals(0, result.length);
        // type is preserved
        assertSame(Double[].class, result.getClass());
    }

    @Test
    void removeItemFromEmptyArrayThrowsNegativeArraySizeException() {
        String[] empty = new String[0];
        // Removing any element from an empty array would try to create an array of size -1
        assertThrows(NegativeArraySizeException.class, () -> ArrayUtilities.removeItem(empty, 0));
    }

    /* -------------------------------------------------------------
     *  Valid removal scenarios – parameterised to cover many cases
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: removeItem({0}, {1}) => {2}")
    @MethodSource("validRemovalProvider")
    void removeItem_ValidRemovals(Object[] array, int pos, Object[] expected) {
        // The generic method returns an array of the same component type.
        // Cast is safe because the test data respects the component type.
        Object[] result = ArrayUtilities.removeItem(array, pos);
        assertArrayEquals(expected, result, "Result array does not match expected");
        // Ensure original array is unchanged
        assertEquals(array.length, array.length);
    }

    /* -------------------------------------------------------------
     *  Exception handling – covers null array, out‑of‑bounds, etc.
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: removeItem({0}, {1}) throws {2}")
    @MethodSource("exceptionProvider")
    void removeItem_ExceptionCases(Object[] array, int pos, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> ArrayUtilities.removeItem(array, pos));
    }

    /* -------------------------------------------------------------
     *  Additional explicit tests for type preservation and edge cases
     * ------------------------------------------------------------- */
    @Test
    void removeItem_SingleElementResultingArrayPreservesComponentType() {
        Double[] original = new Double[] { 3.14 };
        Double[] result = ArrayUtilities.removeItem(original, 0);
        assertEquals(0, result.length);
        // The returned array must be a Double[] (not Object[])
        assertSame(Double[].class, result.getClass(), "Component type of the returned array should be Double[]");
    }

    @Test
    void removeItem_EmptyArrayThrowsNegativeArraySizeException() {
        String[] empty = new String[0];
        assertThrows(NegativeArraySizeException.class, () -> ArrayUtilities.removeItem(empty, 0));
    }
}
