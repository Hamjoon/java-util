package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ArrayUtilities_createArray_Test {

    class Dummy {

        final String name;

        Dummy(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof Dummy) && ((Dummy) o).name.equals(this.name);
        }
    }

    class CustomObject {

        final int id;

        CustomObject(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof CustomObject && ((CustomObject) o).id == this.id;
        }

        @Override
        public String toString() {
            return "CustomObject{id=" + id + '}';
        }
    }

    class CustomObject {

        final int id;

        CustomObject(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof CustomObject && ((CustomObject) o).id == this.id;
        }

        @Override
        public String toString() {
            return "CustomObject{id=" + id + '}';
        }
    }

    private static Stream<Arguments> normalBehaviorProvider() {
        // Helper objects used in several test cases
        class CustomObject {

            final int id;

            CustomObject(int id) {
                this.id = id;
            }

            @Override
            public boolean equals(Object o) {
                return o instanceof CustomObject && ((CustomObject) o).id == this.id;
            }

            @Override
            public String toString() {
                return "CustomObject{id=" + id + '}';
            }
        }
        return Stream.of(// empty var‑args → empty Object[]
        Arguments.of("Empty var‑args should return zero‑length array", new Object[] {}, new Object[] {}), // single String element
        Arguments.of("Single String element", new Object[] { "single" }, new Object[] { "single" }), // multiple String elements
        Arguments.of("Multiple String elements preserve order", new Object[] { "a", "b", "c" }, new Object[] { "a", "b", "c" }), // Integer elements (autoboxed)
        Arguments.of("Integer elements", new Object[] { 1, 2, 3 }, new Object[] { 1, 2, 3 }), // single null element
        Arguments.of("Single null element", new Object[] { null }, new Object[] { null }), // mixed null and non‑null
        Arguments.of("Mixed null and non‑null values", new Object[] { null, "x", null }, new Object[] { null, "x", null }), // custom objects
        Arguments.of("User‑defined type", new Object[] { new CustomObject(1), new CustomObject(2) }, new Object[] { new CustomObject(1), new CustomObject(2) }));
    }

    @Test
    void testCreateArrayWithElements_returnsSameReference() {
        String[] source = new String[] { "alpha", "beta", "gamma" };
        // Passing an existing array as the var‑args argument should return the same array instance
        String[] result = ArrayUtilities.createArray(source);
        assertSame(source, result, "The returned array should be the same instance that was passed in");
        assertArrayEquals(source, result, "The contents must be identical");
    }

    @Test
    void testCreateArrayWithElements_createsArrayWhenUsingLiterals() {
        // Using literal arguments forces the compiler to create a new array for the var‑args
        String[] result = ArrayUtilities.createArray("one", "two", "three");
        assertNotNull(result);
        assertEquals(3, result.length);
        assertArrayEquals(new String[] { "one", "two", "three" }, result);
    }

    @Test
    void testCreateArrayEmptyVarargs_returnsZeroLengthArray() {
        // No arguments – the compiler creates an empty Object[] (type inferred as Object)
        Object[] result = ArrayUtilities.createArray();
        assertNotNull(result, "Result should never be null for zero arguments");
        assertEquals(0, result.length, "Array length should be zero");
    }

    @Test
    void testCreateArrayNullArgument_returnsNull() {
        // Explicitly passing a null array reference should be returned unchanged
        String[] result = ArrayUtilities.createArray((String[]) null);
        assertNull(result, "Passing a null var‑args array should result in null");
    }

    @Test
    void testCreateArrayWithDifferentGenericTypes() {
        Integer[] intResult = ArrayUtilities.createArray(1, 2, 3);
        assertArrayEquals(new Integer[] { 1, 2, 3 }, intResult);
        Double[] doubleResult = ArrayUtilities.createArray(1.1, 2.2);
        assertArrayEquals(new Double[] { 1.1, 2.2 }, doubleResult);
    }

    @Test
    void testCreateArrayWithCustomObject() {
        class Dummy {

            final String name;

            Dummy(String name) {
                this.name = name;
            }

            @Override
            public boolean equals(Object o) {
                return (o instanceof Dummy) && ((Dummy) o).name.equals(this.name);
            }
        }
        Dummy d1 = new Dummy("A");
        Dummy d2 = new Dummy("B");
        Dummy[] result = ArrayUtilities.createArray(d1, d2);
        assertArrayEquals(new Dummy[] { d1, d2 }, result);
    }

    /* --------------------------------------------------------------
     *  Normal behavior – typical usage (parameterized)
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("normalBehaviorProvider")
    void testCreateArrayNormalBehavior(String description, Object[] input, Object[] expected) {
        // The var‑args call is performed via reflection to keep the test generic.
        // This also allows us to pass a null var‑args array when needed.
        Object result;
        if (input == null) {
            // explicit null var‑args array
            result = ArrayUtilities.createArray((Object[]) null);
        } else {
            // normal var‑args invocation
            result = ArrayUtilities.createArray(input);
        }
        if (expected == null) {
            assertNull(result, description);
        } else {
            assertNotNull(result, description);
            assertArrayEquals(expected, (Object[]) result, description);
        }
    }

    /* --------------------------------------------------------------
     *  Edge case – explicit null varargs array
     * -------------------------------------------------------------- */
    @Test
    void testCreateArrayExplicitNullVarargs() {
        // Explicitly passing a null array reference should return null
        String[] result = ArrayUtilities.createArray((String[]) null);
        assertNull(result, "Explicit null var‑args should result in null");
    }

    /* --------------------------------------------------------------
     *  Edge case – passing an existing array as a single element
     * -------------------------------------------------------------- */
    @Test
    void testCreateArrayExistingArrayAsSingleElement() {
        // When a pre‑created array is supplied as the sole vararg element,
        // the generic type becomes String[] and the result is a String[][].
        String[] inner = new String[] { "x", "y" };
        String[][] result = ArrayUtilities.createArray(inner);
        assertNotNull(result);
        assertEquals(1, result.length, "Result should contain exactly one element");
        assertArrayEquals(inner, result[0], "The inner array must be the same instance");
    }

    /* --------------------------------------------------------------
     *  Generic inference – mixed numeric types
     * -------------------------------------------------------------- */
    @Test
    void testCreateArrayMixedNumericTypes_NumberSupertype() {
        Number[] result = ArrayUtilities.createArray(1, 2.5);
        assertArrayEquals(new Number[] { 1, 2.5 }, result);
    }

    @Test
    void testCreateArrayMixedNumericTypes_BigDecimalAndLong() {
        Number[] result = ArrayUtilities.createArray(new BigDecimal("10"), 5L);
        assertArrayEquals(new Number[] { new BigDecimal("10"), 5L }, result);
    }

    @Test
    void testCreateArrayEnumConstants() {
        Color[] result = ArrayUtilities.createArray(Color.RED, Color.BLUE);
        assertArrayEquals(new Color[] { Color.RED, Color.BLUE }, result);
    }

    /* --------------------------------------------------------------
     *  Interface type handling (functional interface)
     * -------------------------------------------------------------- */
    @Test
    void testCreateArrayLambdasImplementingRunnable() {
        Runnable r1 = () -> System.out.println("run1");
        Runnable r2 = () -> System.out.println("run2");
        Runnable[] result = ArrayUtilities.createArray(r1, r2);
        assertEquals(2, result.length);
        assertSame(r1, result[0]);
        assertSame(r2, result[1]);
    }

    /* --------------------------------------------------------------
     *  Primitive wrapper arrays
     * -------------------------------------------------------------- */
    @Test
    void testCreateArrayByteWrappers() {
        Byte[] result = ArrayUtilities.createArray((byte) 1, (byte) 2, (byte) 3);
        assertArrayEquals(new Byte[] { 1, 2, 3 }, result);
    }

    @Test
    void testCreateArrayCharacterWrappers() {
        Character[] result = ArrayUtilities.createArray('a', 'b', 'c');
        assertArrayEquals(new Character[] { 'a', 'b', 'c' }, result);
    }

    /* --------------------------------------------------------------
     *  Large input size (100 integers)
     * -------------------------------------------------------------- */
    @Test
    void testCreateArrayLargeInputSize() {
        // Build 0..99 as Integer objects
        Integer[] expected = new Integer[100];
        for (int i = 0; i < 100; i++) {
            expected[i] = i;
        }
        // Use var‑args; the compiler will autobox each literal
        Integer[] result = ArrayUtilities.createArray(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99);
        assertArrayEquals(expected, result, "Array should contain integers 0 through 99 in order");
        assertEquals(100, result.length, "Array length must be 100");
    }
}
