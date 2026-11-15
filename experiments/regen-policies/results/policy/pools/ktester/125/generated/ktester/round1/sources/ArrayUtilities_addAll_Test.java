package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Array;
import java.util.Objects;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class ArrayUtilities_addAll_Test {

    private static Long[] longArray1;

    private static Long[] longArray2;

    private static String[] stringArray1;

    private static String[] stringArray2;

    private static Long[] emptyLongArray;

    private static String[] emptyStringArray;

    // -----------------------------------------------------------------
    private static final class IdHolder {

        final int id;

        IdHolder(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof IdHolder) && ((IdHolder) o).id == this.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return "IdHolder{id=" + id + '}';
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // initialise immutable test data that will be reused by all tests
        longArray1 = new Long[] { 1L, 2L };
        longArray2 = new Long[] { 3L, 4L };
        stringArray1 = new String[] { "foo", "bar" };
        stringArray2 = new String[] { "baz", "qux" };
        emptyLongArray = new Long[0];
        emptyStringArray = new String[0];
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nullify static references to help GC (optional)
        longArray1 = null;
        longArray2 = null;
        stringArray1 = null;
        stringArray2 = null;
        emptyLongArray = null;
        emptyStringArray = null;
    }

    // -----------------------------------------------------------------
    static Stream<org.junit.jupiter.params.provider.Arguments> nullHandlingProvider() {
        return Stream.of(// first array null, second non‑null
        arguments(null, new String[] { "a", "b", "c" }, new String[] { "a", "b", "c" }, "shallow copy of second array"), // second array null, first non‑null
        arguments(new Integer[] { 10, 20, 30 }, null, new Integer[] { 10, 20, 30 }, "shallow copy of first array"));
    }

    // -----------------------------------------------------------------
    static Stream<org.junit.jupiter.params.provider.Arguments> normalConcatProvider() {
        return Stream.of(arguments(new Integer[] { 1, 2 }, new Integer[] { 3, 4 }, new Integer[] { 1, 2, 3, 4 }, "both non‑empty"), arguments(new String[] {}, new String[] { "x", "y" }, new String[] { "x", "y" }, "first empty, second non‑empty"), arguments(new String[] { "alpha" }, new String[] {}, new String[] { "alpha" }, "second empty, first non‑empty"), arguments(new String[] {}, new String[] {}, new String[] {}, "both empty"));
    }

    // -----------------------------------------------------------------
    static Stream<org.junit.jupiter.params.provider.Arguments> compatibleTypesProvider() {
        return Stream.of(arguments(new Boolean[] { true, false }, new Boolean[] { true }, new Boolean[] { true, false, true }, "Boolean wrapper array"), arguments(new String[] { "first", "second" }, new String[] { "third" }, new String[] { "first", "second", "third" }, "String array"));
    }

    // -----------------------------------------------------------------
    static Stream<org.junit.jupiter.params.provider.Arguments> nullWithEmptyProvider() {
        return Stream.of(arguments(null, new String[] {}, new String[] {}, "first null, second empty"), arguments(new String[] {}, null, new String[] {}, "first empty, second null"));
    }

    @Test
    void addAllBothNonNullSameType() {
        Long[] result = ArrayUtilities.addAll(longArray1, longArray2);
        assertNotSame(longArray1, result, "Result should be a new array, not the first argument");
        assertNotSame(longArray2, result, "Result should be a new array, not the second argument");
        assertArrayEquals(new Long[] { 1L, 2L, 3L, 4L }, result);
    }

    @Test
    void addAllFirstArrayNull() {
        Long[] result = ArrayUtilities.addAll(null, longArray1);
        assertNotSame(longArray1, result, "Result should be a shallow copy, not the original array");
        assertArrayEquals(longArray1, result);
    }

    @Test
    void addAllSecondArrayNull() {
        Long[] result = ArrayUtilities.addAll(longArray2, null);
        assertNotSame(longArray2, result, "Result should be a shallow copy, not the original array");
        assertArrayEquals(longArray2, result);
    }

    @Test
    void addAllBothNullReturnsNull() {
        Long[] result = ArrayUtilities.addAll(null, null);
        assertNull(result, "When both arguments are null the method should return null");
    }

    @Test
    void addAllWithEmptyArrays() {
        // empty + non‑empty
        Long[] result1 = ArrayUtilities.addAll(emptyLongArray, longArray1);
        assertNotSame(longArray1, result1);
        assertArrayEquals(longArray1, result1);
        // non‑empty + empty
        Long[] result2 = ArrayUtilities.addAll(longArray2, emptyLongArray);
        assertNotSame(longArray2, result2);
        assertArrayEquals(longArray2, result2);
        // empty + empty
        Long[] result3 = ArrayUtilities.addAll(emptyLongArray, emptyLongArray);
        assertNotSame(emptyLongArray, result3);
        assertEquals(0, result3.length);
    }

    @Test
    void addAllDifferentComponentTypesThrowsArrayStoreException() {
        // longArray1 has component type Long, stringArray1 has component type String.
        // The runtime type of the resulting array is Long[], so attempting to store a String[] should fail.
        assertThrows(ArrayStoreException.class, () -> ArrayUtilities.addAll(longArray1, stringArray1));
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("nullHandlingProvider")
    <T> void addAll_NullHandling(T[] array1, T[] array2, T[] expected, String description) {
        T[] result = ArrayUtilities.addAll(array1, array2);
        assertNotSame(array1 != null ? array1 : array2, result, "Result must be a new array (shallow copy)");
        assertArrayEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("normalConcatProvider")
    <T> void addAll_NormalConcatenation(T[] array1, T[] array2, T[] expected, String description) {
        T[] result = ArrayUtilities.addAll(array1, array2);
        // Ensure a new instance is returned
        if (array1.length > 0)
            assertNotSame(array1, result);
        if (array2.length > 0)
            assertNotSame(array2, result);
        assertArrayEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("compatibleTypesProvider")
    <T> void addAll_CompatibleComponentTypes(T[] array1, T[] array2, T[] expected, String description) {
        T[] result = ArrayUtilities.addAll(array1, array2);
        assertArrayEquals(expected, result);
    }

    // -----------------------------------------------------------------
    @Test
    void addAll_ReferenceIntegrity() {
        IdHolder h1 = new IdHolder(1);
        IdHolder h2 = new IdHolder(2);
        IdHolder h3 = new IdHolder(3);
        IdHolder[] a1 = new IdHolder[] { h1, h2 };
        IdHolder[] a2 = new IdHolder[] { h3 };
        IdHolder[] result = ArrayUtilities.addAll(a1, a2);
        assertArrayEquals(new IdHolder[] { h1, h2, h3 }, result);
        // Verify that the references are the same (shallow copy)
        assertSame(h1, result[0]);
        assertSame(h2, result[1]);
        assertSame(h3, result[2]);
    }

    // -----------------------------------------------------------------
    @Test
    void addAll_BothNullReturnsNull() {
        Object[] result = ArrayUtilities.addAll(null, null);
        assertNull(result, "Both arguments null should yield null");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("nullWithEmptyProvider")
    <T> void addAll_NullWithEmptyArray(T[] array1, T[] array2, T[] expected, String description) {
        T[] result = ArrayUtilities.addAll(array1, array2);
        assertNotSame(array1 != null ? array1 : array2, result);
        assertArrayEquals(expected, result);
    }

    // -----------------------------------------------------------------
    @Test
    void addAll_NullElementsPreserved() {
        String[] a1 = new String[] { null, "a" };
        String[] a2 = new String[] { "b", null };
        String[] result = ArrayUtilities.addAll(a1, a2);
        assertArrayEquals(new String[] { null, "a", "b", null }, result);
    }

    // -----------------------------------------------------------------
    @Test
    void addAll_TypeMismatchThrowsArrayStoreException() {
        Integer[] ints = new Integer[] { 1, 2 };
        String[] strs = new String[] { "x" };
        assertThrows(ArrayStoreException.class, () -> ArrayUtilities.addAll(ints, strs), "Mismatched component types must cause ArrayStoreException");
    }

    // -----------------------------------------------------------------
    @Test
    void addAll_SupertypeComponentHandling() {
        Object[] a1 = new Object[] { "obj1" };
        String[] a2 = new String[] { "str" };
        Object[] result = ArrayUtilities.addAll(a1, a2);
        assertArrayEquals(new Object[] { "obj1", "str" }, result);
        assertTrue(result.getClass() == Object[].class, "Resulting array type should be Object[] because the first array's component type is Object");
    }

    // -----------------------------------------------------------------
    @Test
    void addAll_StructuralIndependence() {
        Integer[] a1 = new Integer[] { 1, 2 };
        Integer[] a2 = new Integer[] { 3 };
        Integer[] result = ArrayUtilities.addAll(a1, a2);
        // mutate result
        result[0] = 99;
        // original arrays must stay unchanged
        assertArrayEquals(new Integer[] { 1, 2 }, a1);
        assertArrayEquals(new Integer[] { 3 }, a2);
    }

    // -----------------------------------------------------------------
    @Test
    void addAll_LargeConcatenation() {
        int size = 100;
        Integer[] a1 = new Integer[size];
        Integer[] a2 = new Integer[size];
        for (int i = 0; i < size; i++) {
            a1[i] = i;
            a2[i] = i + size;
        }
        Integer[] result = ArrayUtilities.addAll(a1, a2);
        assertEquals(200, result.length);
        for (int i = 0; i < 200; i++) {
            assertEquals(i, result[i]);
        }
    }

    // -----------------------------------------------------------------
    @Disabled("Creating arrays whose combined length exceeds Integer.MAX_VALUE would OOM. " + "The behaviour is verified by the JDK's Array.newInstance which throws NegativeArraySizeException.")
    @Test
    void addAll_LengthOverflowThrowsNegativeArraySizeException() {
        // The test is intentionally disabled; see the comment above.
        // If a custom classloader or a byte‑code manipulation library were allowed,
        // we could mock Array.newInstance, but that is out of scope for plain JUnit/Mockito.
    }
}
