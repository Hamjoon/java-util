package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ArrayUtilities_addAll_Test {

    /**
     * Simple POJO used for the custom‑object array test.
     * Equality is based on the {@code id} field to make {@link Assertions#assertArrayEquals}
     * work without needing a custom comparator.
     */
    static class MyObj {

        private final int id;

        MyObj(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof MyObj))
                return false;
            MyObj myObj = (MyObj) o;
            return id == myObj.id;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(id);
        }

        @Override
        public String toString() {
            return "MyObj{id=" + id + '}';
        }
    }

    static Stream<Arguments> nullFirstArrayProvider() {
        return Stream.of(Arguments.of("First array null, second non‑null", null, new String[] { "alpha", "beta" }, new String[] { "alpha", "beta" }), Arguments.of("Both arrays null", null, null, null));
    }

    static Stream<Arguments> nullSecondArrayProvider() {
        return Stream.of(Arguments.of("Second array null, first non‑null", new Integer[] { 1, 2, 3 }, null, new Integer[] { 1, 2, 3 }));
    }

    static Stream<Arguments> bothNonNullProvider() {
        return Stream.of(Arguments.of("Both arrays non‑null with ints (using Integer)", new Integer[] { 4, 5 }, new Integer[] { 6, 7, 8 }, new Integer[] { 4, 5, 6, 7, 8 }), Arguments.of("First empty, second one element", new String[] {}, new String[] { "x" }, new String[] { "x" }), Arguments.of("Second empty, first one element", new String[] { "y" }, new String[] {}, new String[] { "y" }));
    }

    @Test
    void addAll_TwoNonNullArrays_ConcatenatesInOrder() {
        String[] first = { "a", "b" };
        String[] second = { "c", "d", "e" };
        String[] result = ArrayUtilities.addAll(first, second);
        assertNotNull(result, "Result should not be null");
        assertEquals(first.length + second.length, result.length, "Result length should be the sum of both input lengths");
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" }, result, "Elements should appear in the same order as the inputs");
        // Ensure a new array instance is created
        assertNotSame(first, result);
        assertNotSame(second, result);
    }

    @Test
    void addAll_FirstArrayNull_ReturnsShallowCopyOfSecond() {
        Integer[] second = { 1, 2, 3 };
        Integer[] result = ArrayUtilities.addAll(null, second);
        assertNotNull(result, "Result should not be null when second array is non‑null");
        assertArrayEquals(second, result, "Result should equal the second array");
        // Must be a different instance (shallow copy)
        assertNotSame(second, result);
    }

    @Test
    void addAll_SecondArrayNull_ReturnsShallowCopyOfFirst() {
        Double[] first = { 1.1, 2.2 };
        Double[] result = ArrayUtilities.addAll(first, null);
        assertNotNull(result, "Result should not be null when first array is non‑null");
        assertArrayEquals(first, result, "Result should equal the first array");
        // Must be a different instance (shallow copy)
        assertNotSame(first, result);
    }

    @Test
    void addAll_BothArraysNull_ReturnsNull() {
        // According to the implementation, shallowCopy(null) will return null.
        // The contract is therefore that a null input yields null output.
        assertNull(ArrayUtilities.addAll(null, null), "When both arguments are null the method should return null");
    }

    @Test
    void addAll_FirstArrayEmpty_SecondNonEmpty_ReturnsSecondArrayCopy() {
        String[] first = new String[0];
        String[] second = { "only" };
        String[] result = ArrayUtilities.addAll(first, second);
        assertNotNull(result);
        assertArrayEquals(new String[] { "only" }, result);
        // The returned array must be a new instance, not the original second array
        assertNotSame(second, result);
    }

    @Test
    void addAll_BothArraysEmpty_ReturnsEmptyArray() {
        Integer[] first = new Integer[0];
        Integer[] second = new Integer[0];
        Integer[] result = ArrayUtilities.addAll(first, second);
        assertNotNull(result);
        assertEquals(0, result.length);
        // The returned array should be a distinct empty array
        assertNotSame(first, result);
        assertNotSame(second, result);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("nullFirstArrayProvider")
    void addAll_nullFirstArray(String description, Object[] array1, Object[] array2, Object[] expected) {
        Object[] result = ArrayUtilities.addAll(array1, array2);
        if (expected == null) {
            assertNull(result, description);
        } else {
            assertNotNull(result, description);
            assertArrayEquals(expected, result, description);
            // verify shallow copy (different instance)
            assertNotSame(array2, result, description);
        }
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("nullSecondArrayProvider")
    void addAll_nullSecondArray(String description, Object[] array1, Object[] array2, Object[] expected) {
        Object[] result = ArrayUtilities.addAll(array1, array2);
        assertNotNull(result, description);
        assertArrayEquals(expected, result, description);
        assertNotSame(array1, result, description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("bothNonNullProvider")
    void addAll_bothNonNull(String description, Object[] array1, Object[] array2, Object[] expected) {
        Object[] result = ArrayUtilities.addAll(array1, array2);
        assertNotNull(result, description);
        assertArrayEquals(expected, result, description);
        assertNotSame(array1, result, description);
        assertNotSame(array2, result, description);
    }

    // -----------------------------------------------------------------
    @Test
    void addAll_bothEmpty() {
        Double[] first = new Double[] {};
        Double[] second = new Double[] {};
        Double[] result = ArrayUtilities.addAll(first, second);
        assertNotNull(result);
        assertEquals(0, result.length);
        assertNotSame(first, result);
        assertNotSame(second, result);
    }

    // -----------------------------------------------------------------
    @Test
    void addAll_genericTypePreservation() {
        Character[] first = { 'a', 'b' };
        Character[] second = { 'c' };
        Character[] result = ArrayUtilities.addAll(first, second);
        assertArrayEquals(new Character[] { 'a', 'b', 'c' }, result);
        // Verify component type is Character (same as first array)
        assertEquals(Character.class, result.getClass().getComponentType(), "Component type should be Character");
    }

    // -----------------------------------------------------------------
    @Test
    void addAll_nullFirstArrayEmptySecond() {
        String[] result = ArrayUtilities.addAll(null, new String[] {});
        assertNotNull(result);
        assertEquals(0, result.length);
        // Must be a new empty array, not the same reference as the (empty) second array
        assertNotSame(new String[] {}, result);
    }

    // -----------------------------------------------------------------
    @Test
    void addAll_nullSecondArrayEmptyFirst() {
        String[] result = ArrayUtilities.addAll(new String[] {}, null);
        assertNotNull(result);
        assertEquals(0, result.length);
        // Must be a new empty array, not the same reference as the (empty) first array
        assertNotSame(new String[] {}, result);
    }

    // -----------------------------------------------------------------
    @Test
    void addAll_containsNullElements() {
        String[] first = { "a", null };
        String[] second = { null, "b" };
        String[] result = ArrayUtilities.addAll(first, second);
        assertArrayEquals(new String[] { "a", null, null, "b" }, result);
    }

    // -----------------------------------------------------------------
    @Test
    void addAll_mutationIndependence() {
        Integer[] first = { 1, 2 };
        Integer[] second = { 3, 4 };
        Integer[] result = ArrayUtilities.addAll(first, second);
        // Mutate the result
        result[0] = 99;
        // Original arrays must stay unchanged
        assertArrayEquals(new Integer[] { 1, 2 }, first, "First array unchanged");
        assertArrayEquals(new Integer[] { 3, 4 }, second, "Second array unchanged");
    }

    // -----------------------------------------------------------------
    @Test
    void addAll_referenceIdentity() {
        Long[] first = { 10L, 20L };
        Long[] second = { 30L };
        Long[] result = ArrayUtilities.addAll(first, second);
        assertNotSame(first, result);
        assertNotSame(second, result);
    }

    // -----------------------------------------------------------------
    @Test
    void addAll_customObjectArray() {
        MyObj[] first = { new MyObj(1), new MyObj(2) };
        MyObj[] second = { new MyObj(3) };
        MyObj[] result = ArrayUtilities.addAll(first, second);
        assertArrayEquals(new MyObj[] { new MyObj(1), new MyObj(2), new MyObj(3) }, result);
        // Verify component type preservation
        assertEquals(MyObj.class, result.getClass().getComponentType());
    }

    // -----------------------------------------------------------------
    @Test
    void addAll_largeArrays() {
        Integer[] first = IntStream.range(0, 1000).boxed().toArray(Integer[]::new);
        Integer[] second = IntStream.range(1000, 2000).boxed().toArray(Integer[]::new);
        Integer[] result = ArrayUtilities.addAll(first, second);
        assertEquals(2000, result.length);
        for (int i = 0; i < 2000; i++) {
            assertEquals(i, result[i], "Element at index " + i);
        }
    }

    // -----------------------------------------------------------------
    @Test
    void addAll_incompatibleComponentTypes() {
        String[] first = { "foo", "bar" };
        Integer[] second = { 1, 2 };
        assertThrows(ArrayStoreException.class, () -> ArrayUtilities.addAll(first, second), "Mismatched component types should cause ArrayStoreException");
    }

    @Test
    void addAll_incompatibleComponentTypes_EmptyFirst() {
        String[] first = {};
        Integer[] second = { 42 };
        assertThrows(ArrayStoreException.class, () -> ArrayUtilities.addAll(first, second), "Even when first array is empty, component type mismatch must cause ArrayStoreException");
    }
}
