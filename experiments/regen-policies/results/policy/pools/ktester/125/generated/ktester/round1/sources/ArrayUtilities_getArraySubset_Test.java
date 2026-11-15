package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;
import java.util.Objects;

class ArrayUtilities_getArraySubset_Test {

    /* -------------------------------------------------------
     *  Helper class for the “Custom Object Subset” test group
     * ------------------------------------------------------- */
    private static final class TestObj {

        private final int id;

        private final String name;

        TestObj(int id, String name) {
            this.id = id;
            this.name = name;
        }

        // Getters are not strictly required for the test but make debugging easier
        int getId() {
            return id;
        }

        String getName() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof TestObj))
                return false;
            TestObj that = (TestObj) o;
            return id == that.id && Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name);
        }

        @Override
        public String toString() {
            return "TestObj{id=" + id + ", name='" + name + '\'' + '}';
        }
    }

    private static Stream<Arguments> validSubsetProvider() {
        return Stream.of(// Full copy of the source array – start at 0 and end at length.
        Arguments.of(new String[] { "a", "b", "c" }, 0, 3, new String[] { "a", "b", "c" }, "Full copy of the source array – start at 0 and end at length."), // Extract a middle subset (elements 1‑2).
        Arguments.of(new String[] { "a", "b", "c" }, 1, 3, new String[] { "b", "c" }, "Extract a middle subset (elements 1‑2)."), // Extract a single‑element subset (last element).
        Arguments.of(new String[] { "a", "b", "c" }, 2, 3, new String[] { "c" }, "Extract a single‑element subset (last element)."), // Empty subset when start equals end.
        Arguments.of(new String[] { "a", "b", "c" }, 2, 2, new String[] {}, "Empty subset when start equals end."), // Empty source array returns an empty subset.
        Arguments.of(new String[] {}, 0, 0, new String[] {}, "Empty source array returns an empty subset."), // Works with a non‑String generic type (Integer).
        Arguments.of(new Integer[] { 1, 2, 3, 4, 5 }, 1, 4, new Integer[] { 2, 3, 4 }, "Works with a non‑String generic type (Integer)."));
    }

    private static Stream<Arguments> boundarySubsetProvider() {
        return Stream.of(// Empty subset at the beginning of a non‑empty array (start == end == 0).
        Arguments.of(new String[] { "x", "y", "z" }, 0, 0, new String[] {}, "Empty subset at the beginning of a non‑empty array (start == end == 0)."), // Empty subset at the end of the array (start == end == length).
        Arguments.of(new String[] { "x", "y", "z" }, 3, 3, new String[] {}, "Empty subset at the end of the array (start == end == length)."), // Subset that excludes the last element (start = 0, end = length‑1).
        Arguments.of(new String[] { "x", "y", "z" }, 0, 2, new String[] { "x", "y" }, "Subset that excludes the last element (start = 0, end = length‑1)."), // Single‑element middle subset (start = 1, end = 2).
        Arguments.of(new String[] { "x", "y", "z" }, 1, 2, new String[] { "y" }, "Single‑element middle subset (start = 1, end = 2)."), // Empty subset where start and end are equal in the middle of the array.
        Arguments.of(new String[] { "x", "y", "z" }, 1, 1, new String[] {}, "Empty subset where start and end are equal in the middle of the array."));
    }

    private static Stream<Arguments> nullElementsProvider() {
        return Stream.of(// Array containing null elements should be copied unchanged.
        Arguments.of(new String[] { null, "a", null, "b" }, 0, 4, new String[] { null, "a", null, "b" }, "Array containing null elements should be copied unchanged."), // Subset that includes a null element in the middle of the result.
        Arguments.of(new String[] { null, "a", null, "b" }, 1, 3, new String[] { "a", null }, "Subset that includes a null element in the middle of the result."));
    }

    private static Stream<Arguments> customObjectProvider() {
        TestObj o1 = new TestObj(1, "obj1");
        TestObj o2 = new TestObj(2, "obj2");
        TestObj o3 = new TestObj(3, "obj3");
        return Stream.of(// Works with an array of user‑defined objects; extracts the first two objects.
        Arguments.of(new TestObj[] { o1, o2, o3 }, 0, 2, new TestObj[] { o1, o2 }, "Works with an array of user‑defined objects; extracts the first two objects."), // Extracts a single custom object (the last element).
        Arguments.of(new TestObj[] { o1, o2, o3 }, 2, 3, new TestObj[] { o3 }, "Extracts a single custom object (the last element)."));
    }

    private static Stream<Arguments> exceptionScenariosProvider() {
        return Stream.of(// Null source array should cause NullPointerException.
        Arguments.of(null, 0, 1, NullPointerException.class, "Null source array should cause NullPointerException."), // Negative start index triggers ArrayIndexOutOfBoundsException.
        Arguments.of(new String[] { "a", "b" }, -1, 2, ArrayIndexOutOfBoundsException.class, "Negative start index triggers ArrayIndexOutOfBoundsException."), // End index beyond array length triggers ArrayIndexOutOfBoundsException.
        Arguments.of(new String[] { "a", "b", "c" }, 0, 5, ArrayIndexOutOfBoundsException.class, "End index beyond array length triggers ArrayIndexOutOfBoundsException."), // Start greater than end triggers IllegalArgumentException.
        Arguments.of(new String[] { "a", "b", "c" }, 3, 2, IllegalArgumentException.class, "Start greater than end triggers IllegalArgumentException."), // Negative end index should trigger ArrayIndexOutOfBoundsException.
        Arguments.of(new String[] { "a", "b", "c" }, 0, -1, ArrayIndexOutOfBoundsException.class, "Negative end index should trigger ArrayIndexOutOfBoundsException."), // Start index greater than array length should cause ArrayIndexOutOfBoundsException.
        Arguments.of(new String[] { "a", "b", "c" }, 4, 4, ArrayIndexOutOfBoundsException.class, "Start index greater than array length should cause ArrayIndexOutOfBoundsException."), // Both start and end beyond array length should trigger ArrayIndexOutOfBoundsException.
        Arguments.of(new String[] { "a", "b", "c" }, 4, 5, ArrayIndexOutOfBoundsException.class, "Both start and end beyond array length should trigger ArrayIndexOutOfBoundsException."), // Non‑zero start/end on an empty source array should cause ArrayIndexOutOfBoundsException.
        Arguments.of(new String[] {}, 1, 1, ArrayIndexOutOfBoundsException.class, "Non‑zero start/end on an empty source array should cause ArrayIndexOutOfBoundsException."));
    }

    @Test
    void testGetArraySubsetTypical() {
        Integer[] source = { 1, 2, 3, 4, 5 };
        Integer[] expected = { 2, 3, 4 };
        Integer[] result = ArrayUtilities.getArraySubset(source, 1, 4);
        assertArrayEquals(expected, result, "Subset should contain elements from index 1 (inclusive) to 4 (exclusive)");
    }

    @Test
    void testGetArraySubsetWholeArray() {
        String[] source = { "a", "b", "c" };
        String[] result = ArrayUtilities.getArraySubset(source, 0, source.length);
        assertArrayEquals(source, result, "Subset covering the whole array should be equal to the original array");
        assertNotSame(source, result, "Result should be a new array, not the original reference");
    }

    @Test
    void testGetArraySubsetEmptyResult() {
        Double[] source = { 0.1, 0.2, 0.3 };
        Double[] result = ArrayUtilities.getArraySubset(source, 2, 2);
        assertEquals(0, result.length, "When start == end the result should be an empty array");
    }

    @Test
    void testGetArraySubsetStartGreaterThanEnd() {
        Character[] source = { 'x', 'y', 'z' };
        assertThrows(IllegalArgumentException.class, () -> ArrayUtilities.getArraySubset(source, 2, 1), "start > end should throw IllegalArgumentException");
    }

    @Test
    void testGetArraySubsetNegativeStart() {
        Long[] source = { 10L, 20L, 30L };
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> ArrayUtilities.getArraySubset(source, -1, 2), "Negative start index should throw ArrayIndexOutOfBoundsException");
    }

    @Test
    void testGetArraySubsetEndBeyondLength() {
        Byte[] source = { 1, 2, 3 };
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> ArrayUtilities.getArraySubset(source, 0, 5), "End index greater than array length should throw ArrayIndexOutOfBoundsException");
    }

    @Test
    void testGetArraySubsetNullArray() {
        assertThrows(NullPointerException.class, () -> ArrayUtilities.getArraySubset(null, 0, 1), "Passing a null array should throw NullPointerException");
    }

    /* -------------------------------------------------------
     *  VALID SUBSET EXTRACTION
     * ------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("validSubsetProvider")
    <T> void testValidSubsetExtraction(T[] array, int start, int end, T[] expected, String description) {
        T[] result = ArrayUtilities.getArraySubset(array, start, end);
        assertArrayEquals(expected, result, description);
        // Ensure a new array instance is returned (except when the result is the shared empty array)
        if (expected.length > 0) {
            assertNotSame(array, result, "Result should be a new array, not the original reference");
        }
    }

    /* -------------------------------------------------------
     *  BOUNDARY SUBSET EXTRACTION
     * ------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("boundarySubsetProvider")
    <T> void testBoundarySubsetExtraction(T[] array, int start, int end, T[] expected, String description) {
        T[] result = ArrayUtilities.getArraySubset(array, start, end);
        assertArrayEquals(expected, result, description);
    }

    /* -------------------------------------------------------
     *  NULL ELEMENTS HANDLING
     * ------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("nullElementsProvider")
    <T> void testNullElementsHandling(T[] array, int start, int end, T[] expected, String description) {
        T[] result = ArrayUtilities.getArraySubset(array, start, end);
        assertArrayEquals(expected, result, description);
    }

    /* -------------------------------------------------------
     *  CUSTOM OBJECT SUBSET
     * ------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("customObjectProvider")
    void testCustomObjectSubset(TestObj[] array, int start, int end, TestObj[] expected, String description) {
        TestObj[] result = ArrayUtilities.getArraySubset(array, start, end);
        assertArrayEquals(expected, result, description);
    }

    /* -------------------------------------------------------
     *  EXCEPTION SCENARIOS
     * ------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("exceptionScenariosProvider")
    void testExceptionScenarios(Object[] array, int start, int end, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> ArrayUtilities.getArraySubset(array, start, end), description);
    }
}
