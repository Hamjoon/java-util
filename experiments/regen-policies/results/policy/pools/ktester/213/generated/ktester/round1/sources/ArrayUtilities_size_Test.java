package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

// Mockito not actually used, but kept from template
@ExtendWith(MockitoExtension.class)
class ArrayUtilities_size_Test {

    /**
     * Minimal user‑defined class used to verify that {@code size} works
     * with arrays of custom reference types.
     */
    private static class Foo {
        // No state needed – existence is enough for the test
    }

    static Stream<Arguments> successfulSizeProvider() {
        return Stream.of(// null handling
        Arguments.of(null, 0), // empty arrays
        Arguments.of(new Object[0], 0), Arguments.of(new int[0], 0), Arguments.of(new char[0], 0), Arguments.of(new double[0], 0), // non‑empty primitive arrays
        Arguments.of(new int[] { 1, 2, 3, 4 }, 4), Arguments.of(new byte[] { 10, 20 }, 2), Arguments.of(new long[] { 100L, 200L }, 2), Arguments.of(new boolean[] { true, false, true }, 3), // object arrays
        Arguments.of(new String[] { "a", "b", "c" }, 3), Arguments.of(new Integer[] { 1, 2, 3, 4 }, 4), // custom class array
        Arguments.of(new Foo[] { new Foo(), new Foo() }, 2), // array containing null elements
        Arguments.of(new String[] { null, null, null }, 3), // multi‑dimensional arrays (size of first dimension)
        Arguments.of(new Object[5][2], 5), Arguments.of(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } }, 2), Arguments.of(new String[][][] { {}, {}, {}, {}, {} }, 5));
    }

    static Stream<Arguments> exceptionSizeProvider() {
        return Stream.of(Arguments.of("not an array"), Arguments.of(Collections.emptyList()), Arguments.of(Integer.valueOf(42)), Arguments.of(new Object()), Arguments.of(new HashMap<>()), Arguments.of(new StringBuilder()));
    }

    /**
     * Simple description used in assertion messages – avoids printing
     * large arrays directly.
     */
    private static String describe(Object obj) {
        if (obj == null) {
            return "null";
        }
        if (obj.getClass().isArray()) {
            int length = Array.getLength(obj);
            return obj.getClass().getComponentType().getSimpleName() + "[" + length + "]";
        }
        return obj.toString();
    }

    @Test
    void sizeReturnsZeroWhenArrayIsNull() {
        assertEquals(0, ArrayUtilities.size(null), "size(null) should return 0");
    }

    @Test
    void sizeReturnsZeroForEmptyArrays() {
        assertEquals(0, ArrayUtilities.size(new byte[] {}), "empty byte[]");
        assertEquals(0, ArrayUtilities.size(new int[] {}), "empty int[]");
        assertEquals(0, ArrayUtilities.size(new Object[] {}), "empty Object[]");
    }

    @Test
    void sizeReturnsCorrectLengthForNonEmptyArrays() {
        assertEquals(1, ArrayUtilities.size(new byte[] { 5 }), "byte[] with one element");
        assertEquals(3, ArrayUtilities.size(new int[] { 1, 2, 3 }), "int[] with three elements");
        String[] strings = { "a", "b", "c", "d" };
        assertEquals(4, ArrayUtilities.size(strings), "String[] with four elements");
    }

    @Test
    void sizeWorksOnMultidimensionalArrays() {
        int[][] matrix = new int[2][3];
        // size returns the length of the first dimension only
        assertEquals(2, ArrayUtilities.size(matrix), "int[][] with 2 rows");
        String[][][] cube = new String[5][0][0];
        assertEquals(5, ArrayUtilities.size(cube), "String[][][] with 5 elements in first dimension");
    }

    @Test
    void sizeDoesNotModifyInputArray() {
        int[] original = { 10, 20, 30 };
        int lengthBefore = original.length;
        int lengthReturned = ArrayUtilities.size(original);
        assertEquals(3, lengthReturned);
        assertEquals(lengthBefore, original.length, "original array length should stay unchanged");
    }

    /**
     * Successful size calculations – includes null, primitive, wrapper,
     * reference and multi‑dimensional arrays.
     */
    @ParameterizedTest(name = "[{index}] size({0}) = {1}")
    @MethodSource("successfulSizeProvider")
    void size_calculatesCorrectLength(Object array, int expected) {
        assertEquals(expected, ArrayUtilities.size(array), () -> "Unexpected size for array: " + describe(array));
    }

    /**
     * Cases where the argument is not an array – {@code Array.getLength}
     * must throw {@link IllegalArgumentException}.
     */
    @ParameterizedTest(name = "[{index}] size({0}) throws {1}")
    @MethodSource("exceptionSizeProvider")
    void size_throwsIllegalArgumentException(Object nonArray) {
        assertThrows(IllegalArgumentException.class, () -> ArrayUtilities.size(nonArray), () -> "Expected IllegalArgumentException for argument: " + describe(nonArray));
    }
}
