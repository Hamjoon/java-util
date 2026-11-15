package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ArrayUtilities_shallowCopy_Test {

    /**
     * Simple POJO used for the shallow‑copy test
     */
    private static class Dummy {

        private int value;

        Dummy(int value) {
            this.value = value;
        }

        int getValue() {
            return value;
        }

        void setValue(int value) {
            this.value = value;
        }
    }

    static Stream<Arguments> nullArrayProvider() {
        // One dummy argument is enough to trigger the parameterized test.
        return Stream.of(Arguments.of((Object) null));
    }

    static Stream<Arguments> emptyArrayProvider() {
        return Stream.of(Arguments.of((Object) new Object[0]), Arguments.of((Object) new String[0]), Arguments.of((Object) new Integer[0]));
    }

    static Stream<Arguments> singleElementProvider() {
        return Stream.of(Arguments.of(new String[] { "test" }), Arguments.of(new String[] { "" }), Arguments.of(new String[] { " " }));
    }

    static Stream<Arguments> multipleElementsProvider() {
        return Stream.of(Arguments.of(new Integer[] { 1, 2, 3, 4, 5 }), Arguments.of(new Integer[] { -1, 0, 1 }), Arguments.of(new Integer[] { Integer.MAX_VALUE, Integer.MIN_VALUE }));
    }

    static Stream<Arguments> arrayWithNullsProvider() {
        return Stream.of(Arguments.of(new String[] { null, "a", null }), Arguments.of(new String[] { "first", null, "last" }), Arguments.of(new String[] { null, null, null }));
    }

    static Stream<Arguments> referenceInequalityProvider() {
        return Stream.of(Arguments.of(new Double[] { 1.1, 2.2 }), Arguments.of(new Double[] { 0.0 }), Arguments.of(new Double[] { Double.NaN, Double.POSITIVE_INFINITY }));
    }

    static Stream<Arguments> modificationIndependenceProvider() {
        return Stream.of(Arguments.of(new String[] { "x", "y" }));
    }

    @Test
    void shallowCopy_nullArray_returnsNull() {
        String[] result = ArrayUtilities.shallowCopy(null);
        assertNull(result, "shallowCopy(null) should return null");
    }

    @Test
    void shallowCopy_nonNullArray_returnsClone() {
        Integer[] original = { 1, 2, 3 };
        Integer[] copy = ArrayUtilities.shallowCopy(original);
        // The returned array must be a different instance
        assertNotSame(original, copy, "The cloned array should be a different object");
        // But it must contain the same elements in the same order
        assertArrayEquals(original, copy, "The cloned array should contain the same elements");
    }

    @Test
    void shallowCopy_emptyArray_returnsEmptyClone() {
        String[] original = new String[0];
        String[] copy = ArrayUtilities.shallowCopy(original);
        assertNotSame(original, copy, "Even an empty array should be cloned to a new instance");
        assertEquals(0, copy.length, "Cloned empty array should have length 0");
    }

    @Test
    void shallowCopy_isShallow_notDeep() {
        Dummy[] original = { new Dummy(1), new Dummy(2) };
        Dummy[] copy = ArrayUtilities.shallowCopy(original);
        // Verify different array instances
        assertNotSame(original, copy, "Array instance must be different");
        // Verify that the elements inside are the *same* objects (shallow copy)
        assertSame(original[0], copy[0], "Elements should be the same object (shallow copy)");
        assertSame(original[1], copy[1], "Elements should be the same object (shallow copy)");
        // Mutate an element and ensure the change is visible through both arrays
        original[0].setValue(99);
        assertEquals(99, copy[0].getValue(), "Modification of an element should be reflected in the cloned array");
    }

    /**
     * 1. Null array – should return null
     */
    @ParameterizedTest(name = "[{index}] shallowCopy(null) returns null")
    @MethodSource("nullArrayProvider")
    void shallowCopy_nullArray(Object[] ignored) {
        // The provider supplies a single dummy argument; the method under test receives null.
        String[] result = ArrayUtilities.shallowCopy(null);
        assertNull(result, "shallowCopy(null) must return null");
    }

    /**
     * 2. Empty array – clone must be a new empty instance
     */
    @ParameterizedTest(name = "[{index}] shallowCopy(empty array) returns new empty array")
    @MethodSource("emptyArrayProvider")
    void shallowCopy_emptyArray(Object[] original) {
        Object[] copy = ArrayUtilities.shallowCopy(original);
        assertNotSame(original, copy, "Empty array clone should be a different instance");
        assertEquals(0, copy.length, "Cloned empty array must have length 0");
    }

    /**
     * 3. Single‑element array – clone must contain the same element
     */
    @ParameterizedTest(name = "[{index}] shallowCopy(single element) preserves element")
    @MethodSource("singleElementProvider")
    void shallowCopy_singleElement(String[] original) {
        String[] copy = ArrayUtilities.shallowCopy(original);
        assertNotSame(original, copy, "Clone must be a different instance");
        assertArrayEquals(original, copy, "Clone must contain the same single element");
    }

    /**
     * 4. Multiple‑element array – order and values must be preserved
     */
    @ParameterizedTest(name = "[{index}] shallowCopy(multi‑element) preserves order and values")
    @MethodSource("multipleElementsProvider")
    void shallowCopy_multipleElements(Integer[] original) {
        Integer[] copy = ArrayUtilities.shallowCopy(original);
        assertNotSame(original, copy, "Clone must be a different instance");
        assertArrayEquals(original, copy, "Clone must preserve order and values");
    }

    /**
     * 5. Arrays containing null entries – nulls must be preserved at the same indices
     */
    @ParameterizedTest(name = "[{index}] shallowCopy(array with nulls) preserves null positions")
    @MethodSource("arrayWithNullsProvider")
    void shallowCopy_arrayWithNulls(String[] original) {
        String[] copy = ArrayUtilities.shallowCopy(original);
        assertNotSame(original, copy, "Clone must be a different instance");
        assertArrayEquals(original, copy, "Clone must preserve null entries at the same positions");
    }

    /**
     * 6. Reference inequality – the returned array must not be the same object as the input
     */
    @ParameterizedTest(name = "[{index}] shallowCopy(reference inequality) returns different instance")
    @MethodSource("referenceInequalityProvider")
    void shallowCopy_referenceInequality(Double[] original) {
        Double[] copy = ArrayUtilities.shallowCopy(original);
        assertNotSame(original, copy, "The returned array must be a different instance (clone)");
        assertArrayEquals(original, copy, "Contents must be identical");
    }

    /**
     * 7. Modification independence – changes to the original after cloning must not affect the clone
     */
    @ParameterizedTest(name = "[{index}] shallowCopy(modification independence) original changes do not affect clone")
    @MethodSource("modificationIndependenceProvider")
    void shallowCopy_modificationIndependence(String[] original) {
        String[] copy = ArrayUtilities.shallowCopy(original);
        // Mutate the original array after cloning
        original[0] = "modified";
        // The copy must retain the original value
        assertEquals("x", copy[0], "Clone should retain original value after source modification");
        // Ensure the rest of the elements are unchanged as well
        assertArrayEquals(new String[] { "x", "y" }, copy, "Clone should remain unchanged");
    }
}
