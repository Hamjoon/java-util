package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ArrayUtilities_shallowCopy_Test {

    /**
     * 5. Simple mutable class used in one of the test cases **
     */
    private static class MyObj {

        private final String name;

        MyObj(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "MyObj{" + "name='" + name + '\'' + '}';
        }
    }

    /**
     * 4. Provider for the parameterized test **
     */
    static Stream<Arguments> arrayProvider() {
        return Stream.of(// Empty String array
        Arguments.of(new String[0]), // Non‑empty array of immutable Strings
        Arguments.of(new String[] { "a", "b", "c" }), // Array of boxed primitives (Integer)
        Arguments.of(new Integer[] { 1, 2, 3 }), // Array containing mutable custom objects and a null entry
        Arguments.of(new MyObj[] { new MyObj("x"), null, new MyObj("y") }));
    }

    @Test
    void shallowCopyShouldReturnNullWhenInputIsNull() {
        assertNull(ArrayUtilities.shallowCopy(null), "shallowCopy(null) must return null");
    }

    @Test
    void shallowCopyShouldReturnNewArrayWithSameElements() {
        // Arrange
        String[] original = new String[] { "alpha", "beta", "gamma" };
        // Act
        String[] copy = ArrayUtilities.shallowCopy(original);
        // Assert
        assertNotNull(copy, "Resulting copy must not be null");
        assertNotSame(original, copy, "The returned array must be a different instance");
        assertEquals(original.length, copy.length, "Lengths must be equal");
        // Elements must be the same object references (shallow copy)
        for (int i = 0; i < original.length; i++) {
            assertSame(original[i], copy[i], "Element at index " + i + " should be the same reference");
        }
    }

    @Test
    void shallowCopyShouldWorkWithEmptyArray() {
        Integer[] empty = new Integer[0];
        Integer[] copy = ArrayUtilities.shallowCopy(empty);
        assertNotSame(empty, copy, "Even an empty array should be cloned");
        assertEquals(0, copy.length, "Cloned empty array should have length 0");
    }

    @Test
    void shallowCopyShouldNotModifyOriginalArray() {
        // Arrange
        String[] original = new String[] { "one", "two", "three" };
        String[] copy = ArrayUtilities.shallowCopy(original);
        // Mutate the copy
        copy[0] = "changed";
        // Assert that original remains unchanged
        assertEquals("one", original[0], "Modifying the copy must not affect the original array");
    }
}
