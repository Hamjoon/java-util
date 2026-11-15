package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ArrayUtilities_isEmpty_Test {

    /**
     * Provides a stream of test arguments: each element is an array where
     *   args[0] = the object to test,
     *   args[1] = expected outcome (Boolean true/false or Class of exception).
     */
    private static Stream<Object[]> isEmptyTestProvider() {
        return Stream.of(// null
        new Object[] { null, true }, // empty object array
        new Object[] { new Object[0], true }, // empty primitive int array
        new Object[] { new int[0], true }, // constant EMPTY_BYTE_ARRAY
        new Object[] { ArrayUtilities.EMPTY_BYTE_ARRAY, true }, // non‑empty String array
        new Object[] { new String[] { "a", "b" }, false }, // non‑empty primitive int array
        new Object[] { new int[] { 1, 2, 3 }, false }, // non‑array String (should throw)
        new Object[] { "not an array", IllegalArgumentException.class }, // empty primitive char array
        new Object[] { new char[0], true }, // empty Character object array
        new Object[] { new Character[0], true }, // empty Class array
        new Object[] { new Class[0], true }, // empty primitive boolean array
        new Object[] { new boolean[0], true }, // empty primitive double array
        new Object[] { new double[0], true }, // empty primitive long array
        new Object[] { new long[0], true }, // empty primitive short array
        new Object[] { new short[0], true }, // empty primitive float array
        new Object[] { new float[0], true }, // non‑empty primitive char array
        new Object[] { new char[] { 'x' }, false }, // non‑empty Boolean object array
        new Object[] { new Boolean[] { Boolean.TRUE }, false }, // non‑empty primitive double array
        new Object[] { new double[] { 0.0, 1.1 }, false }, // non‑empty primitive long array
        new Object[] { new long[] { 42L }, false }, // Object array containing only nulls (length > 0)
        new Object[] { new String[] { null, null }, false }, // multi‑dimensional array with first dimension 0
        new Object[] { new int[0][5], true }, // multi‑dimensional array with first dimension >0
        new Object[] { new int[2][0], false }, // constant EMPTY_OBJECT_ARRAY
        new Object[] { ArrayUtilities.EMPTY_OBJECT_ARRAY, true }, // Collection (not an array) – should throw
        new Object[] { java.util.Collections.emptyList(), IllegalArgumentException.class }, // Map (not an array) – should throw
        new Object[] { java.util.Collections.emptyMap(), IllegalArgumentException.class }, // Integer object – should throw
        new Object[] { Integer.valueOf(5), IllegalArgumentException.class });
    }

    /**
     * Helper to produce a readable description of the test input.
     */
    private static String describe(Object obj) {
        if (obj == null) {
            return "null";
        }
        if (obj.getClass().isArray()) {
            int length = java.lang.reflect.Array.getLength(obj);
            return obj.getClass().getComponentType().getSimpleName() + "[" + length + "]";
        }
        return obj.toString() + " (" + obj.getClass().getSimpleName() + ")";
    }

    @Test
    void isEmpty_givenNull_returnsTrue() {
        // given
        Object array = null;
        // when
        boolean result = ArrayUtilities.isEmpty(array);
        // then
        assertTrue(result, "isEmpty should return true for null");
    }

    @Test
    void isEmpty_givenEmptyObjectArray_returnsTrue() {
        // given
        Object[] empty = new Object[0];
        // when
        boolean result = ArrayUtilities.isEmpty(empty);
        // then
        assertTrue(result, "isEmpty should return true for an empty array");
    }

    @Test
    void isEmpty_givenNonEmptyArray_returnsFalse() {
        // given
        int[] numbers = { 1, 2, 3 };
        // when
        boolean result = ArrayUtilities.isEmpty(numbers);
        // then
        assertFalse(result, "isEmpty should return false for a non‑empty array");
    }

    @Test
    void isEmpty_givenNonArrayObject_throwsIllegalArgumentException() {
        // given
        Object notAnArray = new Object();
        // when / then
        assertThrows(IllegalArgumentException.class, () -> ArrayUtilities.isEmpty(notAnArray), "isEmpty should throw IllegalArgumentException when the argument is not an array");
    }

    @ParameterizedTest(name = "[{index}] isEmpty({0}) => {1}")
    @MethodSource("isEmptyTestProvider")
    void isEmpty_variousInputs(Object array, Object expected) {
        if (expected instanceof Class && Throwable.class.isAssignableFrom((Class<?>) expected)) {
            // Expected an exception
            assertThrows((Class<? extends Throwable>) expected, () -> ArrayUtilities.isEmpty(array), () -> "Expected " + ((Class<?>) expected).getSimpleName() + " for input: " + describe(array));
        } else {
            // Expected a boolean result
            boolean result = ArrayUtilities.isEmpty(array);
            assertEquals(expected, result, () -> "isEmpty returned " + result + " for input: " + describe(array));
        }
    }
}
