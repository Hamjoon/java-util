package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class ArrayUtilities_shallowCopy_Test {

    /**
     * Simple mutable class used for shallow‑copy verification.
     */
    private static final class Point {

        final int x, y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    /**
     * Supplies a variety of arrays together with a description used for assertion messages.
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> shallowCopyProvider() {
        return Stream.of(// 1. EMPTY_OBJECT_ARRAY constant
        arguments(ArrayUtilities.EMPTY_OBJECT_ARRAY, "EMPTY_OBJECT_ARRAY should produce a new empty Object[] instance"), // 2. Array containing null elements
        arguments(new String[] { null, "test" }, "Array with a null element should preserve the null and other references"), // 3. Array of mutable Point objects
        arguments(new Point[] { new Point(1, 2), new Point(3, 4) }, "Array of Point objects should be shallow‑copied (same Point references)"), // 4. Single‑element Double array
        arguments(new Double[] { 3.14 }, "Single‑element Double array should be cloned with a different container"), // 5. Large Integer array (0‑999)
        arguments(IntStream.range(0, 1000).boxed().toArray(Integer[]::new), "Large Integer array (size 1000) should be cloned preserving all element references"), // 6. Heterogeneous Number array
        arguments(new Number[] { Integer.valueOf(1), Double.valueOf(2.0) }, "Number[] with heterogeneous elements should be shallow‑copied"));
    }

    @Test
    void testShallowCopy_withNonNullArray() {
        // Given
        String[] original = new String[] { "alpha", "beta", "gamma" };
        // When
        String[] copy = ArrayUtilities.shallowCopy(original);
        // Then
        assertNotNull(copy, "The returned copy should not be null");
        assertNotSame(original, copy, "The copy should be a different instance");
        assertArrayEquals(original, copy, "The contents of the copy should match the original");
        // Verify that the elements themselves are the same references (shallow copy)
        for (int i = 0; i < original.length; i++) {
            assertSame(original[i], copy[i], "Element at index " + i + " should be the same reference in a shallow copy");
        }
    }

    @Test
    void testShallowCopy_withEmptyArray() {
        // Given
        Integer[] empty = new Integer[0];
        // When
        Integer[] copy = ArrayUtilities.shallowCopy(empty);
        // Then
        assertNotNull(copy, "Copy of an empty array should not be null");
        assertNotSame(empty, copy, "Copy should be a different instance even if empty");
        assertEquals(0, copy.length, "Copy should also be empty");
    }

    @Test
    void testShallowCopy_withNullArray() {
        // When
        Object[] result = ArrayUtilities.shallowCopy(null);
        // Then
        assertNull(result, "shallowCopy should return null when the input array is null");
    }

    @ParameterizedTest(name = "{index}: {1}")
    @MethodSource("shallowCopyProvider")
    <T> void testShallowCopy_variousCases(T[] inputArray, String description) {
        // When
        T[] copy = ArrayUtilities.shallowCopy(inputArray);
        // Then
        if (inputArray == null) {
            assertNull(copy, "When input is null the result must be null");
            return;
        }
        assertNotNull(copy, "Result must not be null");
        assertNotSame(inputArray, copy, "The returned array must be a different instance (shallow copy) – " + description);
        assertEquals(inputArray.length, copy.length, "Length of the cloned array must match the original – " + description);
        // Content equality: for reference types we use assertSame for each element,
        // for primitive wrapper types assertEquals works as they are immutable.
        for (int i = 0; i < inputArray.length; i++) {
            T originalElem = inputArray[i];
            T copyElem = copy[i];
            if (originalElem == null) {
                assertNull(copyElem, "Null element at index " + i + " must be preserved – " + description);
            } else {
                // For objects we expect the same reference (shallow copy)
                assertSame(originalElem, copyElem, "Element at index " + i + " should be the same reference – " + description);
            }
        }
    }
}
