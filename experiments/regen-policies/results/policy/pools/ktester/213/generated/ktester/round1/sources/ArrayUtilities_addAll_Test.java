package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

// kept to match the template, though not used
@ExtendWith(MockitoExtension.class)
class ArrayUtilities_addAll_Test {

    /**
     * Helper class used for the shallow‑copy verification test.
     */
    private static class Element {

        private String value;

        Element(String value) {
            this.value = value;
        }

        void setValue(String v) {
            this.value = v;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Element))
                return false;
            Element e = (Element) o;
            return value.equals(e.value);
        }

        @Override
        public String toString() {
            return "Element{" + value + '}';
        }
    }

    private static Stream<Arguments> emptyArrayScenarios() {
        return Stream.of(Arguments.of("First empty, second non‑empty → shallow copy of second", new String[0], new String[] { "x", "y" }, new String[] { "x", "y" }), Arguments.of("Second empty, first non‑empty → shallow copy of first", new String[] { "a" }, new String[0], new String[] { "a" }), Arguments.of("Both empty → empty array", new String[0], new String[0], new String[0]));
    }

    @Test
    void addAllBothNullReturnsNull() {
        assertNull(ArrayUtilities.addAll(null, null), "Both arrays null should return null");
    }

    @Test
    void addAllFirstNullReturnsCopyOfSecond() {
        String[] second = new String[] { "a", "b", "c" };
        String[] result = ArrayUtilities.addAll(null, second);
        assertNotSame(second, result, "Result should be a new array, not the original second array");
        assertArrayEquals(second, result, "Result should contain the same elements as the second array");
    }

    @Test
    void addAllSecondNullReturnsCopyOfFirst() {
        Integer[] first = new Integer[] { 1, 2, 3 };
        Integer[] result = ArrayUtilities.addAll(first, null);
        assertNotSame(first, result, "Result should be a new array, not the original first array");
        assertArrayEquals(first, result, "Result should contain the same elements as the first array");
    }

    @Test
    void addAllBothNonNullConcatenatesCorrectly() {
        Long[] a = new Long[] { 10L, 20L };
        Long[] b = new Long[] { 30L, 40L, 50L };
        Long[] result = ArrayUtilities.addAll(a, b);
        assertNotSame(a, result, "Result should be a new array, not the first array");
        assertNotSame(b, result, "Result should be a new array, not the second array");
        Long[] expected = new Long[] { 10L, 20L, 30L, 40L, 50L };
        assertArrayEquals(expected, result, "Arrays should be concatenated in order");
    }

    @Test
    void addAllDifferentComponentTypesThrowsArrayStoreException() {
        Long[] longs = new Long[] { 1L, 2L };
        String[] strings = new String[] { "foo", "bar" };
        assertThrows(ArrayStoreException.class, () -> ArrayUtilities.addAll(longs, strings), "Mismatched component types should cause ArrayStoreException");
    }

    @Test
    void addAllEmptyArraysReturnEmptyArrayOfCorrectComponentType() {
        String[] empty1 = new String[0];
        String[] empty2 = new String[0];
        String[] result = ArrayUtilities.addAll(empty1, empty2);
        assertNotSame(empty1, result, "Result should be a new array even when both inputs are empty");
        assertEquals(0, result.length, "Resulting array should be empty");
        assertEquals(String.class, result.getClass().getComponentType(), "Component type of the result should match the input component type");
    }

    /**
     * Parameterized test for the three empty‑array scenarios:
     *   1) first empty, second non‑empty
     *   2) first non‑empty, second empty
     *   3) both empty
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("emptyArrayScenarios")
    void addAllEmptyArrayScenarios(String description, String[] first, String[] second, String[] expected) {
        String[] result = ArrayUtilities.addAll(first, second);
        assertNotSame(first, result, "Result must be a new array (first input)");
        assertNotSame(second, result, "Result must be a new array (second input)");
        assertArrayEquals(expected, result, description);
    }

    /**
     * Verify that null elements are preserved in the resulting array.
     */
    @Test
    void addAllPreservesNullElements() {
        String[] a = new String[] { null, "b" };
        String[] b = new String[] { "c", null };
        String[] result = ArrayUtilities.addAll(a, b);
        assertArrayEquals(new String[] { null, "b", "c", null }, result, "Result must contain nulls in the same order as the inputs");
    }

    /**
     * Test concatenation of arrays whose component type is the common super‑type {@code Number}.
     * This demonstrates that the method works when both arguments share a compatible component type.
     */
    @Test
    void addAllSubtypeArraysNumberSupertype() {
        // Integer values
        Number[] first = new Number[] { 1, 2 };
        // Double values
        Number[] second = new Number[] { 3.5, 4.5 };
        Number[] result = ArrayUtilities.addAll(first, second);
        assertArrayEquals(new Number[] { 1, 2, 3.5, 4.5 }, result, "Number[] concatenation should preserve all numeric values");
        // Ensure a new array was created
        assertNotSame(first, result);
        assertNotSame(second, result);
    }

    /**
     * After {@code addAll} the returned array must be independent of the original arrays.
     * Changing the original arrays' contents must not affect the result (shallow copy of the
     * *structure*, not deep copy of the elements).
     */
    @Test
    void addAllShallowCopyVerification() {
        Element[] first = new Element[] { new Element("x") };
        Element[] second = new Element[] { new Element("y") };
        Element[] result = ArrayUtilities.addAll(first, second);
        // Mutate the original arrays
        first[0].setValue("changedX");
        second[0].setValue("changedY");
        // The result should still contain the original values ("x" and "y")
        assertArrayEquals(new Element[] { new Element("x"), new Element("y") }, result, "Result array must remain unchanged after original arrays are mutated");
    }

    /**
     * Additional explicit test for the case where both inputs are {@code null}.
     * The method should return {@code null} (the shallowCopy of {@code null} is {@code null}).
     */
    @Test
    void addAllBothNullInputs() {
        assertNull(ArrayUtilities.addAll(null, null), "Both parameters are null; the method should return null");
    }
}
