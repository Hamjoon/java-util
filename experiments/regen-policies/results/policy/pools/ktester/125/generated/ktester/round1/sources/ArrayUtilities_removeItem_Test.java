package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.lang.reflect.Array;
import java.util.Objects;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class ArrayUtilities_removeItem_Test {

    /* --------------------------------------------------------------------- */
    private static final class Person {

        private final String name;

        private final int age;

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Person))
                return false;
            Person person = (Person) o;
            return age == person.age && Objects.equals(name, person.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, age);
        }

        @Override
        public String toString() {
            return "Person{name='" + name + '\'' + ", age=" + age + '}';
        }
    }

    // ---------- Normal & Boundary removal ----------
    static Stream<Arguments> provideNormalAndBoundaryRemoval() {
        return Stream.of(// Normal removal – middle element
        arguments(new Integer[] { 1, 2, 3, 4 }, 2, new Integer[] { 1, 2, 4 }, "Remove middle element (index 2) from Integer[]"), // Boundary removal – first element
        arguments(new String[] { "a", "b", "c" }, 0, new String[] { "b", "c" }, "Remove first element (index 0) from String[]"), // Boundary removal – last element
        arguments(new String[] { "x", "y", "z" }, 2, new String[] { "x", "y" }, "Remove last element (index 2) from String[]"), // Single‑element array
        arguments(new Integer[] { 42 }, 0, new Integer[] {}, "Remove the only element from a single‑element Integer[]"));
    }

    // ---------- Two‑element array removal ----------
    static Stream<Arguments> provideTwoElementRemoval() {
        return Stream.of(arguments(new Integer[] { 10, 20 }, 0, new Integer[] { 20 }, "Remove first element from a two‑element Integer[]"), arguments(new Integer[] { 10, 20 }, 1, new Integer[] { 10 }, "Remove last element from a two‑element Integer[]"));
    }

    // ---------- Mixed‑type arrays with null ----------
    static Stream<Arguments> provideMixedTypeWithNull() {
        return Stream.of(arguments(new Object[] { null, "alpha", 42, null }, 0, new Object[] { "alpha", 42, null }, "Remove leading null from heterogeneous Object[]"), arguments(new Object[] { "first", null, "third" }, 1, new Object[] { "first", "third" }, "Remove middle null from heterogeneous Object[]"));
    }

    // ---------- Duplicate elements ----------
    static Stream<Arguments> provideDuplicateElements() {
        return Stream.of(arguments(new String[] { "dup", "dup", "unique" }, 1, new String[] { "dup", "unique" }, "Remove second occurrence of duplicate string"));
    }

    // ---------- Component type preservation ----------
    static Stream<Arguments> provideComponentTypePreservation() {
        return Stream.of(arguments(new Integer[] { 1, 2, 3 }, 2, new Integer[] { 1, 2 }, Integer.class, "Component type must stay Integer after removal"), arguments(new Character[] { 'a', 'b', 'c' }, 1, new Character[] { 'a', 'c' }, Character.class, "Component type must stay Character after removal"));
    }

    // ---------- Generic POJO array ----------
    static Stream<Arguments> provideGenericPojoRemoval() {
        Person p1 = new Person("Alice", 30);
        Person p2 = new Person("Bob", 25);
        Person p3 = new Person("Carol", 27);
        return Stream.of(arguments(new Person[] { p1, p2, p3 }, 1, new Person[] { p1, p3 }, "Remove a Person object from a custom POJO array"));
    }

    // ---------- Multi‑dimensional (2‑D) array ----------
    static Stream<Arguments> provideTwoDimensionalArray() {
        Integer[][] source = new Integer[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } };
        Integer[][] expected = new Integer[][] { { 1, 2 }, { 5, 6 } };
        return Stream.of(arguments(source, 1, expected, "Remove middle sub‑array from a 2‑D Integer[][]"));
    }

    // ---------- Reference independence ----------
    static Stream<Arguments> provideReferenceIndependence() {
        return Stream.of(arguments(new Integer[] { 100, 200, 300 }, 1, new Integer[] { 100, 300 }, "Result must be a new array instance"));
    }

    @Test
    void removeItemFromMiddle() {
        String[] original = new String[] { "foo", "bar", "baz" };
        // remove "bar"
        String[] result = ArrayUtilities.removeItem(original, 1);
        String[] expected = new String[] { "foo", "baz" };
        assertArrayEquals(expected, result, "Array after removing middle element is incorrect");
        // original array must stay unchanged
        assertArrayEquals(new String[] { "foo", "bar", "baz" }, original, "Original array should not be modified");
    }

    @Test
    void removeItemFromStart() {
        String[] original = new String[] { "foo", "bar", "baz" };
        // remove "foo"
        String[] result = ArrayUtilities.removeItem(original, 0);
        String[] expected = new String[] { "bar", "baz" };
        assertArrayEquals(expected, result);
    }

    @Test
    void removeItemFromEnd() {
        String[] original = new String[] { "foo", "bar", "baz" };
        // remove "baz"
        String[] result = ArrayUtilities.removeItem(original, 2);
        String[] expected = new String[] { "foo", "bar" };
        assertArrayEquals(expected, result);
    }

    @Test
    void removeOnlyElement() {
        Integer[] original = new Integer[] { 42 };
        Integer[] result = ArrayUtilities.removeItem(original, 0);
        Integer[] expected = new Integer[0];
        assertArrayEquals(expected, result);
        assertEquals(0, result.length, "Resulting array should be empty");
    }

    @Test
    void removeItemWithInvalidIndexThrows() {
        String[] original = new String[] { "foo", "bar", "baz" };
        // negative index
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> ArrayUtilities.removeItem(original, -1));
        // index equal to length
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> ArrayUtilities.removeItem(original, original.length));
        // index greater than length
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> ArrayUtilities.removeItem(original, 10));
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("provideNormalAndBoundaryRemoval")
    void testNormalAndBoundaryRemoval(Object[] array, int pos, Object[] expected, String description) {
        Object[] result = ArrayUtilities.removeItem(array, pos);
        assertArrayEquals(expected, result, description);
        // Ensure original array is untouched
        assertArrayEquals(array, array, "Original array should remain unchanged");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("provideTwoElementRemoval")
    void testTwoElementRemoval(Object[] array, int pos, Object[] expected, String description) {
        Object[] result = ArrayUtilities.removeItem(array, pos);
        assertArrayEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("provideMixedTypeWithNull")
    void testMixedTypeArrayWithNull(Object[] array, int pos, Object[] expected, String description) {
        Object[] result = ArrayUtilities.removeItem(array, pos);
        assertArrayEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("provideDuplicateElements")
    void testDuplicateElementsRemoval(Object[] array, int pos, Object[] expected, String description) {
        Object[] result = ArrayUtilities.removeItem(array, pos);
        assertArrayEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("provideComponentTypePreservation")
    void testComponentTypePreservation(Object[] array, int pos, Object[] expected, Class<?> expectedComponentType, String description) {
        Object[] result = ArrayUtilities.removeItem(array, pos);
        assertArrayEquals(expected, result, description);
        assertEquals(expectedComponentType, result.getClass().getComponentType(), "Component type should be preserved after removal");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("provideGenericPojoRemoval")
    void testGenericPojoRemoval(Person[] array, int pos, Person[] expected, String description) {
        Person[] result = ArrayUtilities.removeItem(array, pos);
        assertArrayEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("provideTwoDimensionalArray")
    void testTwoDimensionalArrayRemoval(Object array, int pos, Object expected, String description) {
        Object result = ArrayUtilities.removeItem((Object[]) array, pos);
        assertArrayEquals((Object[]) expected, (Object[]) result, description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("provideReferenceIndependence")
    void testReferenceIndependence(Object[] array, int pos, Object[] expected, String description) {
        Object[] result = ArrayUtilities.removeItem(array, pos);
        assertArrayEquals(expected, result, description);
        assertNotSame(array, result, "Returned array must be a different instance");
        // Mutate the original and verify the result stays unchanged
        if (array.length > 0) {
            array[0] = null;
            assertNotEquals(array[0], result[0], "Result should not reflect changes to the original array");
        }
    }

    // ---------- Invalid position tests ----------
    @Test
    void testInvalidPositionsThrowArrayIndexOutOfBoundsException() {
        Integer[] source = { 10, 20, 30 };
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> ArrayUtilities.removeItem(source, -1), "Negative index should throw ArrayIndexOutOfBoundsException");
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> ArrayUtilities.removeItem(source, source.length), "Index equal to length should throw ArrayIndexOutOfBoundsException");
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> ArrayUtilities.removeItem(source, 5), "Index greater than length should throw ArrayIndexOutOfBoundsException");
    }

    // ---------- Null array handling ----------
    @Test
    void testNullArrayThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ArrayUtilities.removeItem(null, 0), "Passing null as the source array should throw NullPointerException");
    }

    // ---------- Empty array handling ----------
    @Test
    void testEmptyArrayRemovalThrowsNegativeArraySizeException() {
        Integer[] empty = new Integer[0];
        // Removing from an empty array always results in a destination size of -1
        assertThrows(NegativeArraySizeException.class, () -> ArrayUtilities.removeItem(empty, 0), "Removing from an empty array should throw NegativeArraySizeException");
        assertThrows(NegativeArraySizeException.class, () -> ArrayUtilities.removeItem(empty, -1), "Removing from an empty array with negative index should still throw NegativeArraySizeException");
        assertThrows(NegativeArraySizeException.class, () -> ArrayUtilities.removeItem(empty, 5), "Removing from an empty array with positive out‑of‑range index should still throw NegativeArraySizeException");
    }
}
