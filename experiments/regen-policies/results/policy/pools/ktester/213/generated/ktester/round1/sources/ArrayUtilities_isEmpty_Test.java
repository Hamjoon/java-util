package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Array;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ArrayUtilities_isEmpty_Test {

    class Foo {
    }

    class Foo {
    }

    private static Stream<Arguments> provideIsEmptyBooleanCases() {
        // Helper class for a user‑defined type
        class Foo {
        }
        return Stream.of(// basic functionality
        Arguments.of("null reference is considered empty", null, true), Arguments.of("empty primitive int array", new int[0], true), Arguments.of("empty Integer object array", new Integer[0], true), Arguments.of("non‑empty primitive int array", new int[] { 1, 2, 3 }, false), Arguments.of("non‑empty String object array", new String[] { "a", "b" }, false), Arguments.of("multi‑dimensional array with first dimension 0", new int[0][5], true), // additional types and dimensions
        Arguments.of("EMPTY_BYTE_ARRAY constant", ArrayUtilities.EMPTY_BYTE_ARRAY, true), Arguments.of("empty primitive char array", new char[0], true), Arguments.of("empty primitive double array", new double[0], true), Arguments.of("empty primitive boolean array", new boolean[0], true), Arguments.of("empty Foo object array", new Foo[0], true), Arguments.of("reflection‑created empty String array", Array.newInstance(String.class, 0), true), Arguments.of("multi‑dimensional array first dimension >0, inner length 0", new int[2][0], false), Arguments.of("object multi‑dimensional array with null sub‑arrays", new String[3][], false));
    }

    private static Stream<Arguments> provideIsEmptyExceptionCases() {
        return Stream.of(Arguments.of("non‑array object (String) should cause IllegalArgumentException", "not an array"));
    }

    @Test
    @DisplayName("isEmpty returns true for a null reference")
    void givenNullArray_whenIsEmpty_thenTrueReturned() {
        // given
        Object array = null;
        // when
        boolean result = ArrayUtilities.isEmpty(array);
        // then
        assertTrue(result, "null should be considered empty");
    }

    @Test
    @DisplayName("isEmpty returns true for an empty primitive array")
    void givenEmptyPrimitiveArray_whenIsEmpty_thenTrueReturned() {
        // given
        byte[] empty = new byte[0];
        // when
        boolean result = ArrayUtilities.isEmpty(empty);
        // then
        assertTrue(result, "empty primitive array should be considered empty");
    }

    @Test
    @DisplayName("isEmpty returns false for a non‑empty primitive array")
    void givenNonEmptyPrimitiveArray_whenIsEmpty_thenFalseReturned() {
        // given
        int[] numbers = new int[] { 1, 2, 3 };
        // when
        boolean result = ArrayUtilities.isEmpty(numbers);
        // then
        assertFalse(result, "non‑empty primitive array should not be considered empty");
    }

    @Test
    @DisplayName("isEmpty works with empty object array")
    void givenEmptyObjectArray_whenIsEmpty_thenTrueReturned() {
        // given
        String[] empty = new String[0];
        // when
        boolean result = ArrayUtilities.isEmpty(empty);
        // then
        assertTrue(result, "empty object array should be considered empty");
    }

    @Test
    @DisplayName("isEmpty works with non‑empty object array")
    void givenNonEmptyObjectArray_whenIsEmpty_thenFalseReturned() {
        // given
        Object[] objects = new Object[] { new Object() };
        // when
        boolean result = ArrayUtilities.isEmpty(objects);
        // then
        assertFalse(result, "non‑empty object array should not be considered empty");
    }

    @Test
    @DisplayName("isEmpty correctly handles multi‑dimensional arrays")
    void givenMultiDimensionalArray_whenIsEmpty_thenCorrectResult() {
        // given
        int[][] empty2D = new int[0][];
        int[][] nonEmpty2D = new int[1][];
        nonEmpty2D[0] = new int[] { 42 };
        // when / then
        assertTrue(ArrayUtilities.isEmpty(empty2D), "empty 2‑D array should be empty");
        assertFalse(ArrayUtilities.isEmpty(nonEmpty2D), "non‑empty 2‑D array should not be empty");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideIsEmptyBooleanCases")
    @DisplayName("isEmpty returns expected boolean for various array inputs")
    void givenArray_whenIsEmpty_thenReturnsExpected(String description, Object array, boolean expected) {
        // when
        boolean result = ArrayUtilities.isEmpty(array);
        // then
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideIsEmptyExceptionCases")
    @DisplayName("isEmpty throws IllegalArgumentException for non‑array arguments")
    void givenNonArray_whenIsEmpty_thenThrowsIllegalArgumentException(String description, Object notArray) {
        assertThrows(IllegalArgumentException.class, () -> ArrayUtilities.isEmpty(notArray), description);
    }

    @Test
    @DisplayName("isEmpty works with empty object array (String[])")
    void givenEmptyStringArray_whenIsEmpty_thenTrueReturned() {
        String[] empty = new String[0];
        assertTrue(ArrayUtilities.isEmpty(empty), "empty String[] should be considered empty");
    }

    @Test
    @DisplayName("isEmpty correctly handles a 2‑D array where first dimension is empty")
    void givenEmpty2DArray_whenIsEmpty_thenTrueReturned() {
        int[][] empty2D = new int[0][];
        assertTrue(ArrayUtilities.isEmpty(empty2D), "empty 2‑D array should be empty");
    }

    @Test
    @DisplayName("isEmpty correctly handles a 2‑D array where first dimension is non‑empty")
    void givenNonEmpty2DArray_whenIsEmpty_thenFalseReturned() {
        int[][] nonEmpty2D = new int[1][];
        nonEmpty2D[0] = new int[] { 42 };
        assertFalse(ArrayUtilities.isEmpty(nonEmpty2D), "non‑empty 2‑D array should not be empty");
    }
}
