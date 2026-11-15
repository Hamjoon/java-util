package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ArrayUtilities_toArray_Test {

    private static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(// 1. three strings
        Arguments.of("Convert a collection of three strings to a String[]", String.class, Arrays.asList("a", "b", "c"), new String[] { "a", "b", "c" }), // 2. empty Integer collection
        Arguments.of("Empty collection should produce an empty Integer[]", Integer.class, Collections.emptyList(), new Integer[] {}), // 3. singleton Object collection
        Arguments.of("Single-element collection should produce an array of length 1", Object.class, Collections.singleton("single"), new Object[] { "single" }), // 4. mixed Number subclasses
        Arguments.of("Collection with mixed Number subclasses", Number.class, Arrays.asList(1, 2L, 3.0f), new Number[] { 1, 2L, 3.0f }), // 5. collection of int[] objects
        Arguments.of("Collection of int[] objects", int[].class, Arrays.asList(new int[] { 1 }, new int[] { 2, 3 }), new int[][] { new int[] { 1 }, new int[] { 2, 3 } }), // 6. collection containing nulls
        Arguments.of("Collection containing null elements", String.class, Arrays.asList(null, "test", null), new String[] { null, "test", null }), // 7. Void.class with empty collection (edge case)
        Arguments.of("Edge case: using Void.class with empty collection", Void.class, Collections.emptyList(), new Void[] {}), // 8. collection that was modified after creation
        Arguments.of("Collection that has been modified after creation", String.class, modifiedList(), new String[] { "y", "z" }));
    }

    /**
     * Helper to create the mutable list described in case 8
     */
    private static List<String> modifiedList() {
        List<String> list = new ArrayList<>();
        list.add("x");
        list.add("y");
        // remove "x"
        list.remove(0);
        list.add("z");
        return list;
    }

    private static Stream<Arguments> typeMismatchProvider() {
        return Stream.of(Arguments.of("Collection of Integers cannot be stored in String[]", String.class, Arrays.asList(1, 2, 3)), Arguments.of("Mixed element types where one element is not assignable to List", List.class, Arrays.asList(new ArrayList<>(), "not a list")));
    }

    private static Stream<Arguments> nullParameterProvider() {
        return Stream.of(Arguments.of("classToCastTo is null", null, Collections.emptyList()), Arguments.of("Collection argument is null", String.class, null));
    }

    @Test
    void testToArrayWithStrings() {
        // given
        Collection<String> strings = new ArrayList<>();
        strings.add("foo");
        strings.add("bar");
        strings.add("baz");
        // when
        String[] result = ArrayUtilities.toArray(String.class, strings);
        // then
        assertNotNull(result);
        assertEquals(3, result.length);
        assertArrayEquals(new String[] { "foo", "bar", "baz" }, result);
        // ensure a new array instance is returned (not the same reference as any internal array)
        assertNotSame(strings.toArray(new String[0]), result);
    }

    @Test
    void testToArrayEmptyCollection() {
        // given
        Collection<Integer> empty = new ArrayList<>();
        // when
        Integer[] result = ArrayUtilities.toArray(Integer.class, empty);
        // then
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    void testToArrayWithIntegers() {
        // given
        List<Integer> numbers = List.of(1, 2, 3, 4);
        // when
        Integer[] array = ArrayUtilities.toArray(Integer.class, numbers);
        // then
        assertArrayEquals(new Integer[] { 1, 2, 3, 4 }, array);
    }

    @Test
    void testToArrayNullCollectionThrowsNPE() {
        // given
        Collection<String> nullCollection = null;
        // when / then
        assertThrows(NullPointerException.class, () -> ArrayUtilities.toArray(String.class, nullCollection));
    }

    @Test
    void testToArrayClassMismatchThrowsArrayStoreException() {
        // given: collection holds Numbers, but we ask for Integer[] (Number is not necessarily Integer)
        Collection<Number> numbers = new ArrayList<>();
        // Integer
        numbers.add(1);
        // Double
        numbers.add(2.5);
        // when / then
        assertThrows(ArrayStoreException.class, () -> ArrayUtilities.toArray(Integer.class, numbers));
    }

    @Test
    void testArrayIsIndependentFromOriginalCollection() {
        // given
        List<String> original = new ArrayList<>();
        original.add("alpha");
        original.add("beta");
        // when
        String[] array = ArrayUtilities.toArray(String.class, original);
        // modify original collection after conversion
        original.add("gamma");
        // then
        assertEquals(2, array.length);
        assertArrayEquals(new String[] { "alpha", "beta" }, array);
    }

    /**
     * BASIC FUNCTIONALITY **
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("basicFunctionalityProvider")
    <T> void testToArrayBasicFunctionality(String description, Class<T> targetClass, Collection<?> collection, T[] expected) {
        T[] result = ArrayUtilities.toArray(targetClass, collection);
        assertNotNull(result, "Resulting array must not be null");
        assertArrayEquals(expected, result, "Array content mismatch for: " + description);
    }

    /**
     * TYPE MISMATCH EXCEPTIONS **
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("typeMismatchProvider")
    void testToArrayTypeMismatchException(String description, Class<?> targetClass, Collection<?> collection) {
        assertThrows(IllegalArgumentException.class, () -> ArrayUtilities.toArray(targetClass, collection), "Expected IllegalArgumentException for: " + description);
    }

    /**
     * NULL PARAMETERS **
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullParameterProvider")
    void testToArrayNullParameters(String description, Class<?> targetClass, Collection<?> collection) {
        assertThrows(NullPointerException.class, () -> ArrayUtilities.toArray(targetClass, collection), "Expected NullPointerException for: " + description);
    }

    /**
     * PRIMITIVE ARRAY HANDLING **
     */
    @Test
    void testToArrayPrimitiveIntArray() {
        List<Integer> numbers = Arrays.asList(5, 10, 15);
        int[] result = ArrayUtilities.toArray(int.class, numbers);
        assertArrayEquals(new int[] { 5, 10, 15 }, result);
    }

    @Test
    void testToArrayPrimitiveBooleanArray() {
        List<Boolean> bools = Arrays.asList(true, false, true);
        boolean[] result = ArrayUtilities.toArray(boolean.class, bools);
        assertArrayEquals(new boolean[] { true, false, true }, result);
    }

    /**
     * INTERFACE COMPONENT TYPE **
     */
    @Test
    void testToArrayInterfaceComponentType() {
        List<List<?>> source = Arrays.asList(new ArrayList<>(), new LinkedList<>());
        List<?>[] result = ArrayUtilities.toArray(List.class, source);
        assertEquals(2, result.length);
        assertTrue(result[0] instanceof ArrayList);
        assertTrue(result[1] instanceof LinkedList);
        // content equality (both empty)
        assertTrue(((Collection<?>) result[0]).isEmpty());
        assertTrue(((Collection<?>) result[1]).isEmpty());
    }

    /**
     * LARGE COLLECTION **
     */
    @Test
    void testToArrayLargeCollection() {
        List<Integer> large = IntStream.rangeClosed(1, 1000).boxed().collect(Collectors.toList());
        Integer[] result = ArrayUtilities.toArray(Integer.class, large);
        assertEquals(1000, result.length);
        assertEquals(1, result[0]);
        assertEquals(1000, result[999]);
    }

    /**
     * UNORDERED SET COLLECTION **
     */
    @Test
    void testToArrayUnorderedSet() {
        Set<String> set = new HashSet<>(Arrays.asList("a", "b", "c"));
        String[] result = ArrayUtilities.toArray(String.class, set);
        assertEquals(3, result.length);
        // order is not guaranteed – verify content only
        assertTrue(Arrays.asList(result).containsAll(Arrays.asList("a", "b", "c")));
    }

    /**
     * COLLECTION WITH DUPLICATE ELEMENTS **
     */
    @Test
    void testToArrayDuplicatesPreserved() {
        List<String> dupes = Arrays.asList("dup", "dup", "unique");
        String[] result = ArrayUtilities.toArray(String.class, dupes);
        assertArrayEquals(new String[] { "dup", "dup", "unique" }, result);
    }

    /**
     * SELF‑REFERENTIAL ELEMENT **
     */
    @Test
    void testToArraySelfReferentialElement() {
        List<Object> source = Collections.singletonList((Object) Collections.emptyList());
        Object[] result = ArrayUtilities.toArray(Object.class, source);
        assertEquals(1, result.length);
        assertTrue(result[0] instanceof List);
        assertTrue(((List<?>) result[0]).isEmpty());
    }
}
