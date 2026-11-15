package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

class ArrayUtilities_size_Test {

    // Sample arrays used in the tests
    private Object[] objectArray;

    private int[] intArray;

    private byte[] byteArray;

    private char[] charArray;

    private String[] stringArray;

    private double[] doubleArray;

    /**
     * Simple user‑defined class to test handling of custom object arrays.
     */
    private static class Foo {
        // No fields required; existence of the class is enough for the test.
    }

    @BeforeEach
    void setupBeforeEach() {
        objectArray = new Object[] { "a", 1, new Object() };
        intArray = new int[] { 1, 2, 3, 4 };
        byteArray = new byte[] { 0x1, 0x2 };
        charArray = new char[] { 'x', 'y', 'z' };
        stringArray = new String[] { "one", "two" };
        doubleArray = new double[] { 1.0, 2.5, 3.75 };
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset fields to help GC and avoid accidental reuse.
        objectArray = null;
        intArray = null;
        byteArray = null;
        charArray = null;
        stringArray = null;
        doubleArray = null;
    }

    /**
     * Provides a stream of arguments for normal (non‑exception) size checks.
     * Each argument consists of:
     * <ul>
     *   <li>the array instance to pass to {@code ArrayUtilities.size}</li>
     *   <li>the expected integer result</li>
     *   <li>a description used in the test name</li>
     * </ul>
     */
    private static Stream<Arguments> normalSizeProvider() {
        return Stream.of(// empty primitive arrays
        Arguments.of(new int[0], 0, "empty int[]"), Arguments.of(new byte[0], 0, "empty byte[]"), Arguments.of(new char[0], 0, "empty char[]"), Arguments.of(new Character[0], 0, "empty Character[]"), Arguments.of(new Class[0], 0, "empty Class[]"), // non‑empty primitive arrays
        Arguments.of(new int[] { 1, 2, 3 }, 3, "non‑empty int[]"), Arguments.of(new byte[] { 0x1, 0x2, 0x3, 0x4 }, 4, "non‑empty byte[]"), Arguments.of(new char[] { 'a', 'b', 'c' }, 3, "non‑empty char[]"), Arguments.of(new double[] { 1.1, 2.2, 3.3, 4.4 }, 4, "non‑empty double[]"), Arguments.of(new long[] { 10L, 20L, 30L }, 3, "non‑empty long[]"), Arguments.of(new float[] { 1.0f, 2.0f }, 2, "non‑empty float[]"), Arguments.of(new short[] { 5, 6, 7, 8, 9 }, 5, "non‑empty short[]"), Arguments.of(new boolean[] { true, false, true }, 3, "non‑empty boolean[]"), // object arrays
        Arguments.of(new String[] { "a", "b" }, 2, "non‑empty String[]"), Arguments.of(new Object[] { null }, 1, "single‑element Object[] with null"), Arguments.of(new Foo[2], 2, "Foo[] of length 2"), // multi‑dimensional arrays (only first dimension matters)
        Arguments.of(new int[2][3], 2, "int[2][3] first dimension 2"), Arguments.of(new int[0][5], 0, "int[0][5] first dimension 0"), Arguments.of(new int[3][0], 3, "int[3][0] first dimension 3"));
    }

    /**
     * Provides a stream of arguments that are *not* arrays and therefore should cause
     * {@link IllegalArgumentException} when passed to {@code ArrayUtilities.size}.
     */
    private static Stream<Arguments> nonArrayProvider() {
        return Stream.of(Arguments.of(new java.util.ArrayList<>(), "ArrayList instance"));
    }

    @Test
    @DisplayName("size(null) should return 0")
    void size_nullArray_returnsZero() {
        // given
        Object nullArray = null;
        // when
        int result = ArrayUtilities.size(nullArray);
        // then
        assertEquals(0, result, "size(null) must be 0");
    }

    @Test
    @DisplayName("size(Object[]) should return the correct length")
    void size_objectArray_returnsLength() {
        // given – objectArray initialized in @BeforeEach (length 3)
        // when
        int result = ArrayUtilities.size(objectArray);
        // then
        assertEquals(3, result);
    }

    @Test
    @DisplayName("size(empty Object[]) should return 0")
    void size_emptyObjectArray_returnsZero() {
        // given
        Object[] empty = new Object[0];
        // when
        int result = ArrayUtilities.size(empty);
        // then
        assertEquals(0, result);
    }

    @Test
    @DisplayName("size(int[]) should return the correct length")
    void size_intArray_returnsLength() {
        // given – intArray length is 4
        // when
        int result = ArrayUtilities.size(intArray);
        // then
        assertEquals(4, result);
    }

    @Test
    @DisplayName("size(byte[]) should return the correct length")
    void size_byteArray_returnsLength() {
        // given – byteArray length is 2
        // when
        int result = ArrayUtilities.size(byteArray);
        // then
        assertEquals(2, result);
    }

    @Test
    @DisplayName("size(char[]) should return the correct length")
    void size_charArray_returnsLength() {
        // given – charArray length is 3
        // when
        int result = ArrayUtilities.size(charArray);
        // then
        assertEquals(3, result);
    }

    @Test
    @DisplayName("size(String[]) should return the correct length")
    void size_stringArray_returnsLength() {
        // given – stringArray length is 2
        // when
        int result = ArrayUtilities.size(stringArray);
        // then
        assertEquals(2, result);
    }

    @Test
    @DisplayName("size(double[]) should return the correct length")
    void size_doubleArray_returnsLength() {
        // given – doubleArray length is 3
        // when
        int result = ArrayUtilities.size(doubleArray);
        // then
        assertEquals(3, result);
    }

    @ParameterizedTest(name = "size({2}) → {1}")
    @MethodSource("normalSizeProvider")
    void size_variousArrays_returnsExpectedLength(Object array, int expected, String description) {
        // when
        int result = ArrayUtilities.size(array);
        // then
        assertEquals(expected, result, () -> "Expected size for " + description + " to be " + expected + " but was " + result);
    }

    @ParameterizedTest(name = "size(non‑array {1}) throws IllegalArgumentException")
    @MethodSource("nonArrayProvider")
    void size_nonArray_throwsIllegalArgumentException(Object notArray, String description) {
        // when / then
        assertThrows(IllegalArgumentException.class, () -> ArrayUtilities.size(notArray), () -> "Calling size on a non‑array (" + description + ") should throw IllegalArgumentException");
    }
}
