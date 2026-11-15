package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class ArrayUtilities_isEmpty_Test {

    private static Stream<Arguments> trueProvider() {
        return Stream.of(// Null
        arguments((Object) null), // Primitive empty arrays
        arguments(new int[0]), arguments(new byte[0]), arguments(new char[0]), arguments(new double[0]), arguments(new long[0]), arguments(new boolean[0]), arguments(new short[0]), arguments(new float[0]), // Object empty arrays
        arguments(new String[0]), arguments(new Object[0]), arguments(ArrayUtilities.EMPTY_OBJECT_ARRAY), arguments(ArrayUtilities.EMPTY_BYTE_ARRAY), // Multi‑dimensional empty first dimension
        arguments(new int[0][5]), arguments(new String[0][0]));
    }

    private static Stream<Arguments> falseProvider() {
        return Stream.of(// Primitive non‑empty arrays
        arguments(new int[] { 1, 2, 3 }), arguments(new byte[] { 0 }), arguments(new char[] { 'a' }), arguments(new double[] { 0.0 }), arguments(new long[] { 1L }), arguments(new boolean[] { true }), arguments(new short[] { 5 }), arguments(new float[] { 1.0f }), // Object non‑empty arrays
        arguments(new String[] { "a", "b" }), arguments(new Object[] { "a" }), arguments(new Object[] { new Object() }), // Arrays that contain only nulls but have length > 0
        // three null elements
        arguments(new String[3]), arguments(new Object[5]), // wrapper array, all null
        arguments(new Integer[2]), // Multi‑dimensional non‑empty first dimension
        arguments(new int[2][0]), arguments(new String[1][2]));
    }

    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(// Simple non‑array objects
        arguments("not an array"), arguments(new java.util.ArrayList<>()), // Integer (autoboxed)
        arguments(5), arguments(new java.util.HashMap<>()), arguments(new Object()), arguments(String.class), arguments(java.time.DayOfWeek.MONDAY), arguments(java.util.Optional.empty()));
    }

    /* --------------------------------------------------------------------- */
    private static String describe(Object o) {
        if (o == null) {
            return "null";
        }
        Class<?> c = o.getClass();
        if (c.isArray()) {
            int length = java.lang.reflect.Array.getLength(o);
            return String.format("%s (length=%d)", c.getComponentType().getTypeName(), length);
        }
        return o.toString();
    }

    @Test
    void testIsEmptyWithNull() {
        assertTrue(ArrayUtilities.isEmpty(null), "null should be considered empty");
    }

    @Test
    void testIsEmptyWithZeroLengthArrays() {
        assertTrue(ArrayUtilities.isEmpty(new byte[] {}), "empty byte array should be empty");
        assertTrue(ArrayUtilities.isEmpty(new int[] {}), "empty int array should be empty");
        assertTrue(ArrayUtilities.isEmpty(new Object[] {}), "empty Object array should be empty");
        assertTrue(ArrayUtilities.isEmpty(new String[] {}), "empty String array should be empty");
    }

    @Test
    void testIsEmptyWithNonEmptyArrays() {
        assertFalse(ArrayUtilities.isEmpty(new byte[] { 0 }), "byte array with one element should not be empty");
        assertFalse(ArrayUtilities.isEmpty(new int[] { 1, 2, 3 }), "int array with elements should not be empty");
        assertFalse(ArrayUtilities.isEmpty(new Object[] { "a" }), "Object array with an element should not be empty");
        assertFalse(ArrayUtilities.isEmpty(new String[] { "foo", "bar" }), "String array with elements should not be empty");
    }

    @Test
    void testIsEmptyWithMultiDimensionalArray() {
        // A multi‑dimensional array with zero length in the first dimension is still empty
        assertTrue(ArrayUtilities.isEmpty(new int[0][5]), "zero‑length first dimension should be empty");
        // A multi‑dimensional array with a non‑zero first dimension is not empty,
        // even if inner arrays are empty
        assertFalse(ArrayUtilities.isEmpty(new int[1][0]), "non‑zero first dimension should not be empty");
    }

    @Test
    void testIsEmptyWithNonArrayObject() {
        // According to java.lang.reflect.Array#getLength, passing a non‑array throws
        // IllegalArgumentException. The utility method does not catch this, so the
        // exception propagates.
        Object notAnArray = new Object();
        assertThrows(IllegalArgumentException.class, () -> ArrayUtilities.isEmpty(notAnArray), "Calling isEmpty with a non‑array should throw IllegalArgumentException");
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] isEmpty should return true for {0}")
    @MethodSource("trueProvider")
    void shouldReturnTrueForEmptyOrNull(Object array) {
        assertTrue(ArrayUtilities.isEmpty(array), () -> "Expected true for array: " + describe(array));
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] isEmpty should return false for {0}")
    @MethodSource("falseProvider")
    void shouldReturnFalseForNonEmpty(Object array) {
        assertFalse(ArrayUtilities.isEmpty(array), () -> "Expected false for array: " + describe(array));
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] isEmpty should throw IllegalArgumentException for {0}")
    @MethodSource("exceptionProvider")
    void shouldThrowForNonArray(Object notArray) {
        assertThrows(IllegalArgumentException.class, () -> ArrayUtilities.isEmpty(notArray), () -> "Expected IllegalArgumentException for argument: " + describe(notArray));
    }
}
