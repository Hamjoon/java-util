package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ArrayUtilities_toArray_Test {

    static Stream<Arguments> normalBehaviourProvider() {
        return Stream.of(// empty collection → empty array of the requested type
        Arguments.of("Empty collection should result in an empty array of the requested component type.", String.class, Collections.emptyList(), new String[0]), // normal integer collection
        Arguments.of("A collection of Integers is converted to an Integer[] preserving order.", Integer.class, List.of(1, 2, 3), new Integer[] { 1, 2, 3 }), // collection with null element
        Arguments.of("Collection containing null elements should produce an array with the same nulls in the correct positions.", String.class, Arrays.asList("a", null, "b"), new String[] { "a", null, "b" }));
    }

    static Stream<Arguments> nullHandlingProvider() {
        return Stream.of(// null collection
        Arguments.of("Passing a null collection should cause a NullPointerException before any array allocation.", String.class, null, NullPointerException.class), // null class reference – we need to cast to raw Class to compile
        Arguments.of("Passing a null class reference should cause a NullPointerException when Array.newInstance is invoked.", null, List.of("x", "y"), NullPointerException.class));
    }

    static Stream<Arguments> typeMismatchProvider() {
        return Stream.of(// collection of Integers, target String[]
        Arguments.of("Collection contains Integer elements while target component type is String; toArray should throw ArrayStoreException.", String.class, List.of(1, 2)), // collection of Strings, target Integer[]
        Arguments.of("Collection contains String elements while target component type is Integer; toArray should throw ArrayStoreException.", Integer.class, List.of("a", "b")));
    }

    static Stream<Arguments> primitiveComponentProvider() {
        return Stream.of(// int.class with Integer collection → ArrayStoreException (cannot store Integer into int[])
        Arguments.of("Target component type is primitive int but collection holds Integer objects; Array.set will attempt to store reference into primitive array and throw ArrayStoreException.", int.class, List.of(1, 2), ArrayStoreException.class), // int.class with null element → IllegalArgumentException (null cannot be stored in primitive array)
        Arguments.of("Target component type is primitive int and collection contains a null element; Array.set cannot assign null to a primitive array and throws IllegalArgumentException.", int.class, Collections.singletonList(null), IllegalArgumentException.class));
    }

    @Test
    void toArray_convertsStringCollection() {
        Collection<String> strings = new ArrayList<>();
        strings.add("foo");
        strings.add("bar");
        strings.add("baz");
        String[] result = ArrayUtilities.toArray(String.class, strings);
        assertNotNull(result, "Resulting array must not be null");
        assertEquals(3, result.length, "Array length should match collection size");
        assertArrayEquals(new String[] { "foo", "bar", "baz" }, result);
    }

    @Test
    void toArray_emptyCollectionReturnsEmptyArray() {
        // immutable empty list
        Collection<Integer> empty = List.of();
        Integer[] result = ArrayUtilities.toArray(Integer.class, empty);
        assertNotNull(result, "Resulting array must not be null even for empty collection");
        assertEquals(0, result.length, "Array length should be zero for empty collection");
    }

    @Test
    void toArray_nullCollectionThrowsNPE() {
        assertThrows(NullPointerException.class, () -> ArrayUtilities.toArray(Object.class, null), "Passing a null collection should throw NullPointerException");
    }

    @Test
    void toArray_collectionWithSubclassElements() {
        // Collection holds Integer objects, but we ask for Number[]. The conversion should succeed.
        Collection<Integer> ints = List.of(1, 2, 3);
        Number[] result = ArrayUtilities.toArray(Number.class, ints);
        assertNotNull(result);
        assertEquals(3, result.length);
        assertArrayEquals(new Number[] { 1, 2, 3 }, result);
    }

    @Test
    void toArray_incompatibleTargetTypeThrowsArrayStoreException() {
        // Collection holds Integer, but we request a String[] – the assignment will fail.
        Collection<Integer> ints = List.of(10, 20);
        assertThrows(ArrayStoreException.class, () -> ArrayUtilities.toArray(String.class, ints), "Incompatible target type should result in ArrayStoreException");
    }

    @Test
    void toArray_collectionContainingNullElements() {
        // The method must preserve null entries.
        Collection<String> withNulls = new ArrayList<>();
        withNulls.add("first");
        withNulls.add(null);
        withNulls.add("third");
        String[] result = ArrayUtilities.toArray(String.class, withNulls);
        assertEquals(3, result.length);
        assertEquals("first", result[0]);
        assertNull(result[1], "Null element should be preserved in the resulting array");
        assertEquals("third", result[2]);
    }

    /**
     * Normal behaviour – empty, normal, and null‑containing collections.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("normalBehaviourProvider")
    void toArray_normalBehaviour(String description, Class<?> targetClass, Collection<?> collection, Object[] expectedArray) {
        Object[] result = ArrayUtilities.toArray(targetClass, collection);
        assertNotNull(result, "Result must never be null");
        assertArrayEquals(expectedArray, result, description);
    }

    /**
     * Null handling – null collection or null class argument.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullHandlingProvider")
    void toArray_nullHandling(String description, Class<?> targetClass, Collection<?> collection, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> ArrayUtilities.toArray(targetClass, collection), description);
    }

    /**
     * Type mismatch handling – incompatible element types.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("typeMismatchProvider")
    void toArray_typeMismatch(String description, Class<?> targetClass, Collection<?> collection) {
        assertThrows(ArrayStoreException.class, () -> ArrayUtilities.toArray(targetClass, collection), description);
    }

    /**
     * Primitive component type handling – int.class with reference elements.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("primitiveComponentProvider")
    void toArray_primitiveComponent(String description, Class<?> primitiveClass, Collection<?> collection, Class<? extends Throwable> expectedException) {
        // The generic signature expects a reference type, so we have to use a raw call.
        assertThrows(expectedException, () -> {
            // Suppress unchecked warnings because we deliberately pass a primitive class.
            @SuppressWarnings("unchecked")
            Object[] result = ArrayUtilities.toArray((Class) primitiveClass, collection);
            // The call should have thrown; if it doesn't, we still verify the result is not used.
        }, description);
    }
}
