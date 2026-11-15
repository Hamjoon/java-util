package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Array;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ArrayUtilities_removeItem_Test {

    static class Person {

        final String name;

        Person(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "Person{name='" + name + "'}";
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof Person) && ((Person) o).name.equals(this.name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }

    static Stream<Arguments> provideVariousPositions() {
        return Stream.of(// Remove first element (Integer[])
        Arguments.of(toArray(Integer.class, 1, 2, 3, 4, 5), 0, toArray(Integer.class, 2, 3, 4, 5)), // Remove middle element (String[])
        Arguments.of(toArray(String.class, "a", "b", "c", "d"), 2, toArray(String.class, "a", "b", "d")), // Remove last element (Boolean[])
        Arguments.of(toArray(Boolean.class, true, false, true), 2, toArray(Boolean.class, true, false)), // Remove sole element (String[])
        Arguments.of(toArray(String.class, "single"), 0, toArray(String.class)));
    }

    static Stream<Arguments> provideErrorCases() {
        return Stream.of(// null array -> NullPointerException
        Arguments.of(null, 0, NullPointerException.class), // negative position
        Arguments.of(toArray(Integer.class, 1, 2, 3), -1, ArrayIndexOutOfBoundsException.class), // position == length
        Arguments.of(toArray(Integer.class, 1, 2, 3), 3, ArrayIndexOutOfBoundsException.class), // empty array, pos 0 -> NegativeArraySizeException (array creation fails)
        Arguments.of(toArray(Integer.class), 0, NegativeArraySizeException.class));
    }

    static Stream<Arguments> provideNullElementCases() {
        return Stream.of(Arguments.of(toArray(String.class, null, "a", "b"), 0, toArray(String.class, "a", "b")), Arguments.of(toArray(String.class, "x", null, "y"), 1, toArray(String.class, "x", "y")));
    }

    @Test
    void removeFirstElement() {
        // given
        Integer[] original = { 1, 2, 3, 4 };
        // when
        Integer[] result = ArrayUtilities.removeItem(original, 0);
        // then
        assertArrayEquals(new Integer[] { 2, 3, 4 }, result, "The first element should be removed and the remaining order preserved.");
        assertEquals(4, original.length, "The original array must remain unchanged.");
    }

    @Test
    void removeMiddleElement() {
        // given
        String[] original = { "a", "b", "c", "d", "e" };
        // when
        // remove "c"
        String[] result = ArrayUtilities.removeItem(original, 2);
        // then
        assertArrayEquals(new String[] { "a", "b", "d", "e" }, result, "The element at the specified middle position should be removed.");
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" }, original, "The original array must remain unchanged.");
    }

    @Test
    void removeLastElement() {
        // given
        Double[] original = { 1.1, 2.2, 3.3 };
        // when
        Double[] result = ArrayUtilities.removeItem(original, original.length - 1);
        // then
        assertArrayEquals(new Double[] { 1.1, 2.2 }, result, "The last element should be removed.");
        assertArrayEquals(new Double[] { 1.1, 2.2, 3.3 }, original, "The original array must remain unchanged.");
    }

    @Test
    void removeItemWithInvalidPositionThrowsException() {
        // given
        Character[] original = { 'x', 'y', 'z' };
        // when / then
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> ArrayUtilities.removeItem(original, -1), "Negative index should cause ArrayIndexOutOfBoundsException.");
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> ArrayUtilities.removeItem(original, original.length), "Index equal to length should cause ArrayIndexOutOfBoundsException.");
    }

    @Test
    void removeItemFromSingleElementArrayReturnsEmptyArray() {
        // given
        Long[] original = { 42L };
        // when
        Long[] result = ArrayUtilities.removeItem(original, 0);
        // then
        assertEquals(0, result.length, "Removing the only element should produce an empty array.");
        assertArrayEquals(new Long[] { 42L }, original, "The original array must remain unchanged.");
    }

    /**
     * Helper to create a generic array from a var‑args list (used in sources).
     */
    @SuppressWarnings("unchecked")
    private static <T> T[] toArray(Class<T> component, T... elements) {
        T[] arr = (T[]) Array.newInstance(component, elements.length);
        System.arraycopy(elements, 0, arr, 0, elements.length);
        return arr;
    }

    @ParameterizedTest(name = "[{index}] removeItem from {0} at pos {1}")
    @MethodSource("provideVariousPositions")
    <T> void testRemoveVariousPositions(T[] original, int pos, T[] expected) {
        T[] result = ArrayUtilities.removeItem(original, pos);
        assertArrayEquals(expected, result, () -> "Result array does not match expected after removing position " + pos);
        // ensure original is unchanged
        assertEquals(original.length, result.length + 1, "Original length should be one greater than result length");
    }

    @ParameterizedTest(name = "[{index}] removeItem error case")
    @MethodSource("provideErrorCases")
    <T> void testErrorHandling(T[] array, int pos, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> ArrayUtilities.removeItem(array, pos));
    }

    @Test
    void removeItemFromCustomObjectArray() {
        Person p1 = new Person("Alice");
        Person p2 = new Person("Bob");
        Person p3 = new Person("Carol");
        Person[] original = { p1, p2, p3 };
        Person[] result = ArrayUtilities.removeItem(original, 1);
        assertArrayEquals(new Person[] { p1, p3 }, result, "Result should contain the same object instances except the removed one");
        // verify shallow copy (same instances)
        assertSame(p1, result[0]);
        assertSame(p3, result[1]);
    }

    @Test
    void removeItemCreatesNewArrayInstance() {
        Integer[] original = { 1, 2, 3 };
        Integer[] result = ArrayUtilities.removeItem(original, 1);
        assertArrayEquals(new Integer[] { 1, 3 }, result);
        assertNotSame(original, result, "removeItem must return a new array instance");
    }

    @ParameterizedTest
    @MethodSource("provideNullElementCases")
    <T> void testRemovalWithNullElements(T[] original, int pos, T[] expected) {
        T[] result = ArrayUtilities.removeItem(original, pos);
        assertArrayEquals(expected, result);
    }

    @Test
    void removeItemFromNumberArray() {
        Number[] original = { 1, 2.5, 3L };
        Number[] result = ArrayUtilities.removeItem(original, 1);
        assertArrayEquals(new Number[] { 1, 3L }, result);
        // ensure types are preserved
        assertTrue(result[0] instanceof Integer);
        assertTrue(result[1] instanceof Long);
    }

    @Test
    void removeItemFromMultidimensionalArray() {
        Integer[][] original = { { 1, 2 }, { 3, 4 }, { 5, 6 } };
        Integer[][] result = ArrayUtilities.removeItem(original, 2);
        assertArrayEquals(new Integer[][] { { 1, 2 }, { 3, 4 } }, result);
    }

    @Test
    void removeItemFromEnumArray() {
        Day[] original = { Day.MON, Day.TUE, Day.WED };
        Day[] result = ArrayUtilities.removeItem(original, 1);
        assertArrayEquals(new Day[] { Day.MON, Day.WED }, result);
    }

    @Test
    void removeItemWithDuplicates() {
        Integer[] original = { 1, 2, 2, 3 };
        // remove second '2'
        Integer[] result = ArrayUtilities.removeItem(original, 2);
        assertArrayEquals(new Integer[] { 1, 2, 3 }, result);
    }

    @Test
    void originalArrayRemainsUnchanged() {
        Integer[] original = { 1, 2, 3 };
        Integer[] copyBefore = original.clone();
        ArrayUtilities.removeItem(original, 1);
        assertArrayEquals(copyBefore, original, "The source array must stay unchanged after removeItem.");
    }

    @Test
    void removeItemPositionBeyondLengthThrows() {
        Integer[] original = { 1, 2, 3 };
        assertThrows(IndexOutOfBoundsException.class, () -> ArrayUtilities.removeItem(original, 4));
    }

    @Test
    void removeItemFromEmptyArrayThrowsNegativeArraySize() {
        Integer[] empty = new Integer[0];
        assertThrows(NegativeArraySizeException.class, () -> ArrayUtilities.removeItem(empty, 0));
    }
}
