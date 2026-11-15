package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.lang.reflect.Array;
import java.util.stream.Stream;

// kept to match the supplied template; not used here
@ExtendWith(MockitoExtension.class)
class ArrayUtilities_size_Test {

    private static Stream<Arguments> validArrayProvider() {
        return Stream.of(// Null input – handled separately below for clarity
        // Empty arrays
        Arguments.of(new int[0], 0), Arguments.of(new byte[0], 0), Arguments.of(new char[0], 0), Arguments.of(new double[0], 0), Arguments.of(new boolean[0], 0), Arguments.of(new short[0], 0), Arguments.of(new long[0], 0), Arguments.of(new float[0], 0), Arguments.of(new String[0], 0), Arguments.of(new Object[0], 0), // Constant empty arrays from the class
        Arguments.of(ArrayUtilities.EMPTY_OBJECT_ARRAY, 0), Arguments.of(ArrayUtilities.EMPTY_BYTE_ARRAY, 0), Arguments.of(ArrayUtilities.EMPTY_CHAR_ARRAY, 0), Arguments.of(ArrayUtilities.EMPTY_CHARACTER_ARRAY, 0), Arguments.of(ArrayUtilities.EMPTY_CLASS_ARRAY, 0), // Non‑empty primitive arrays
        Arguments.of(new int[] { 1, 2, 3 }, 3), Arguments.of(new byte[] { 1, 2, 3 }, 3), Arguments.of(new char[] { 'a', 'b' }, 2), Arguments.of(new double[] { 0.1, 0.2, 0.3, 0.4 }, 4), Arguments.of(new boolean[] { true, false, true }, 3), Arguments.of(new short[] { 1, 2, 3, 4, 5 }, 5), Arguments.of(new int[] { 10, 20, 30, 40 }, 4), Arguments.of(new long[] { 100L, 200L, 300L }, 3), Arguments.of(new float[] { 1.5f, 2.5f }, 2), // Single‑element primitive arrays
        Arguments.of(new int[] { 42 }, 1), Arguments.of(new boolean[] { true }, 1), // Wrapper type arrays
        Arguments.of(new Long[] { 5L, 10L, 15L }, 3), Arguments.of(new Short[] { 1, 2 }, 2), Arguments.of(new Boolean[] { Boolean.TRUE, Boolean.FALSE, Boolean.TRUE }, 3), // Object arrays
        Arguments.of(new String[] { "a", "b", "c" }, 3), Arguments.of(new Integer[] { 1, 2 }, 2), Arguments.of(new Object[] { new Object(), new Object(), new Object(), new Object() }, 4), // Enum array
        Arguments.of(new TestEnum[] { TestEnum.A, TestEnum.B, TestEnum.A }, 3), // Interface‑typed array (Runnable)
        Arguments.of(new Runnable[] { () -> {
        }, () -> System.out.println("run") }, 2), // Multi‑dimensional arrays (size reports first dimension)
        Arguments.of(new int[3][4], 3), Arguments.of(new String[0][5], 0), // Large array (length 1000)
        Arguments.of(new Object[1000], 1000));
    }

    private static Stream<Arguments> invalidArrayProvider() {
        return Stream.of(Arguments.of("not an array"), Arguments.of(new java.util.ArrayList<>()), Arguments.of(Integer.valueOf(5)), Arguments.of(new java.util.HashMap<>()), Arguments.of(TestEnum.A), Arguments.of(String.class), Arguments.of(new Thread()));
    }

    /* -------------------------------------------------------
       Utility method to produce a readable description for debugging
       ------------------------------------------------------- */
    private static String describe(Object obj) {
        if (obj == null) {
            return "null";
        }
        Class<?> clazz = obj.getClass();
        if (clazz.isArray()) {
            int length = Array.getLength(obj);
            return clazz.getComponentType().getSimpleName() + "[" + length + "] (first dimension)";
        }
        return obj.toString() + " (type: " + clazz.getSimpleName() + ")";
    }

    @Test
    void sizeReturnsZeroForNull() {
        assertEquals(0, ArrayUtilities.size(null), "size(null) should return 0");
    }

    @Test
    void sizeReturnsZeroForEmptyPrimitiveArrays() {
        assertEquals(0, ArrayUtilities.size(new int[] {}));
        assertEquals(0, ArrayUtilities.size(new byte[] {}));
        assertEquals(0, ArrayUtilities.size(new char[] {}));
        assertEquals(0, ArrayUtilities.size(new double[] {}));
        assertEquals(0, ArrayUtilities.size(new boolean[] {}));
    }

    @Test
    void sizeReturnsZeroForEmptyObjectArray() {
        assertEquals(0, ArrayUtilities.size(new String[] {}));
        assertEquals(0, ArrayUtilities.size(new Object[] {}));
    }

    @Test
    void sizeReturnsCorrectLengthForNonEmptyArrays() {
        assertEquals(3, ArrayUtilities.size(new int[] { 1, 2, 3 }));
        assertEquals(2, ArrayUtilities.size(new byte[] { 5, 6 }));
        assertEquals(4, ArrayUtilities.size(new String[] { "a", "b", "c", "d" }));
        assertEquals(1, ArrayUtilities.size(new Object[] { new Object() }));
    }

    @Test
    void sizeHandlesMultiDimensionalArrays() {
        int[][] twoDim = new int[5][10];
        // size should report the length of the first dimension (5)
        assertEquals(5, ArrayUtilities.size(twoDim));
        String[][][] threeDim = new String[2][3][4];
        // size should report the length of the outermost dimension (2)
        assertEquals(2, ArrayUtilities.size(threeDim));
    }

    @Test
    void sizeWorksWithArrayUtilitiesEmptyConstants() {
        // The constants are defined as empty arrays; size should be 0.
        assertEquals(0, ArrayUtilities.size(ArrayUtilities.EMPTY_OBJECT_ARRAY));
        assertEquals(0, ArrayUtilities.size(ArrayUtilities.EMPTY_BYTE_ARRAY));
        assertEquals(0, ArrayUtilities.size(ArrayUtilities.EMPTY_CHAR_ARRAY));
        assertEquals(0, ArrayUtilities.size(ArrayUtilities.EMPTY_CHARACTER_ARRAY));
        assertEquals(0, ArrayUtilities.size(ArrayUtilities.EMPTY_CLASS_ARRAY));
    }

    /* -------------------------------------------------------
       Parameterized test for all inputs that should return a size
       ------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] size({0}) = {1}")
    @MethodSource("validArrayProvider")
    void sizeReturnsExpectedLength(Object array, int expected) {
        assertEquals(expected, ArrayUtilities.size(array), () -> "Expected size " + expected + " for array: " + describe(array));
    }

    /* -------------------------------------------------------
       Parameterized test for inputs that must throw IllegalArgumentException
       ------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] size({0}) throws IllegalArgumentException")
    @MethodSource("invalidArrayProvider")
    void sizeThrowsIllegalArgumentException(Object notAnArray) {
        assertThrows(IllegalArgumentException.class, () -> ArrayUtilities.size(notAnArray), () -> "Expected IllegalArgumentException for input: " + describe(notAnArray));
    }
}
