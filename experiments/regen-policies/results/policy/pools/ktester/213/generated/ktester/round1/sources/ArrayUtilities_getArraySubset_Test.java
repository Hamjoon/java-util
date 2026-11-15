package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ArrayUtilities_getArraySubset_Test {

    /* -------------------------------------------------------------
     *  Special Types – custom objects
     * ------------------------------------------------------------- */
    private static class Person {

        private final String name;

        Person(String name) {
            this.name = name;
        }

        // Equality based on name for test assertions
        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Person))
                return false;
            Person person = (Person) o;
            return name.equals(person.name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public String toString() {
            return "Person{name='" + name + "'}";
        }
    }

    /* -------------------------------------------------------------
     *  Normal Cases – parameterized
     * ------------------------------------------------------------- */
    private static Stream<Arguments> normalCasesProvider() {
        return Stream.of(// Typical subset extraction from the middle of an Integer array.
        Arguments.of(new Integer[] { 1, 2, 3, 4 }, 1, 3, new Integer[] { 2, 3 }), // Full‑range subset of a String array.
        Arguments.of(new String[] { "a", "b", "c" }, 0, 3, new String[] { "a", "b", "c" }), // Empty array with zero‑length subset.
        Arguments.of(new Double[] {}, 0, 0, new Double[] {}), // Array containing null elements; subset should preserve nulls.
        Arguments.of(new String[] { null, null, null }, 0, 2, new String[] { null, null }));
    }

    /* -------------------------------------------------------------
     *  Boundary Conditions – parameterized
     * ------------------------------------------------------------- */
    private static Stream<Arguments> boundaryCasesProvider() {
        return Stream.of(// Zero‑length subset at the beginning of a non‑empty array.
        Arguments.of(new Integer[] { 10, 20, 30 }, 0, 0, new Integer[] {}), // Zero‑length subset at the end of the array (start == length).
        Arguments.of(new String[] { "x", "y", "z" }, 3, 3, new String[] {}), // Subset that extracts the last element.
        Arguments.of(new Integer[] { 5, 6, 7, 8 }, 3, 4, new Integer[] { 8 }), // First two elements of a String array.
        Arguments.of(new String[] { "a", "b", "c" }, 0, 2, new String[] { "a", "b" }), // End index exceeds array length – copyOfRange pads with nulls.
        Arguments.of(new Integer[] { 1, 2, 3 }, 1, 5, new Integer[] { 2, 3, null, null }));
    }

    /* -------------------------------------------------------------
     *  Exception Cases – parameterized
     * ------------------------------------------------------------- */
    private static Stream<Arguments> exceptionCasesProvider() {
        return Stream.of(// Negative start index.
        Arguments.of(new Integer[] { 9, 8, 7 }, -1, 2, ArrayIndexOutOfBoundsException.class), // Start greater than end.
        Arguments.of(new String[] { "p", "q" }, 2, 1, IllegalArgumentException.class), // Null array reference.
        Arguments.of(null, 0, 0, NullPointerException.class), // Start index beyond length.
        Arguments.of(new Integer[] { 1, 2, 3 }, 4, 5, ArrayIndexOutOfBoundsException.class));
    }

    @Test
    void testGetArraySubsetTypical() {
        String[] source = { "alpha", "beta", "gamma", "delta", "epsilon" };
        String[] expected = { "beta", "gamma", "delta" };
        String[] actual = ArrayUtilities.getArraySubset(source, 1, 4);
        assertArrayEquals(expected, actual, "Subset should contain elements 1 (inclusive) to 4 (exclusive)");
    }

    @Test
    void testGetArraySubsetFullRange() {
        Integer[] source = { 1, 2, 3, 4 };
        Integer[] actual = ArrayUtilities.getArraySubset(source, 0, source.length);
        // The returned array must be a new instance, not the original.
        assertNotSame(source, actual, "Returned array should be a copy, not the original reference");
        assertArrayEquals(source, actual, "Full‑range subset should be identical to the source");
    }

    @Test
    void testGetArraySubsetEmptyArray() {
        Double[] empty = new Double[0];
        Double[] result = ArrayUtilities.getArraySubset(empty, 0, 0);
        assertNotSame(empty, result, "Even for empty arrays a new instance should be returned");
        assertArrayEquals(new Double[0], result, "Result of empty subset should be an empty array");
    }

    @Test
    void testGetArraySubsetZeroLength() {
        Character[] source = { 'a', 'b', 'c' };
        Character[] result = ArrayUtilities.getArraySubset(source, 2, 2);
        assertArrayEquals(new Character[0], result, "When start == end the result should be an empty array");
    }

    @Test
    void testGetArraySubsetNegativeStart() {
        String[] source = { "x", "y", "z" };
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> ArrayUtilities.getArraySubset(source, -1, 2), "Negative start index should trigger ArrayIndexOutOfBoundsException");
    }

    @Test
    void testGetArraySubsetEndBeyondLength() {
        String[] source = { "x", "y", "z" };
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> ArrayUtilities.getArraySubset(source, 0, 5), "End index greater than array length should trigger ArrayIndexOutOfBoundsException");
    }

    @Test
    void testGetArraySubsetStartGreaterThanEnd() {
        String[] source = { "x", "y", "z" };
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> ArrayUtilities.getArraySubset(source, 3, 2), "Start index greater than end index should trigger ArrayIndexOutOfBoundsException");
    }

    @Test
    void testGetArraySubsetNullArray() {
        assertThrows(NullPointerException.class, () -> ArrayUtilities.getArraySubset(null, 0, 1), "Passing a null array should result in NullPointerException");
    }

    @ParameterizedTest(name = "[Normal] {index}: array={0}, start={1}, end={2}")
    @MethodSource("normalCasesProvider")
    <T> void testGetArraySubset_NormalCases(T[] array, int start, int end, T[] expected) {
        T[] actual = ArrayUtilities.getArraySubset(array, start, end);
        assertArrayEquals(expected, actual, () -> String.format("Expected subset %s but got %s", java.util.Arrays.toString(expected), java.util.Arrays.toString(actual)));
    }

    @ParameterizedTest(name = "[Boundary] {index}: array={0}, start={1}, end={2}")
    @MethodSource("boundaryCasesProvider")
    <T> void testGetArraySubset_BoundaryCases(T[] array, int start, int end, T[] expected) {
        T[] actual = ArrayUtilities.getArraySubset(array, start, end);
        assertArrayEquals(expected, actual, () -> String.format("Expected padded subset %s but got %s", java.util.Arrays.toString(expected), java.util.Arrays.toString(actual)));
    }

    @ParameterizedTest(name = "[Exception] {index}: array={0}, start={1}, end={2}")
    @MethodSource("exceptionCasesProvider")
    <T> void testGetArraySubset_ExceptionCases(T[] array, int start, int end, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> ArrayUtilities.getArraySubset(array, start, end), () -> String.format("Expected %s for array=%s, start=%d, end=%d", expectedException.getSimpleName(), java.util.Arrays.toString(array), start, end));
    }

    @Test
    void testGetArraySubset_CharacterArray() {
        Character[] source = { 'a', 'b', 'c', 'd' };
        Character[] expected = { 'a', 'b' };
        Character[] actual = ArrayUtilities.getArraySubset(source, 0, 2);
        assertArrayEquals(expected, actual, "Character array subset should match expected values");
    }

    @Test
    void testGetArraySubset_CustomObjectArray() {
        Person[] source = { new Person("John"), new Person("Jane") };
        Person[] expected = { new Person("Jane") };
        Person[] actual = ArrayUtilities.getArraySubset(source, 1, 2);
        assertArrayEquals(expected, actual, "Custom object array subset should preserve object identity via equals");
    }
}
