package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CollectionUtilities_setOf_Test {

    // This uses the public API (calling setOf with no arguments) which returns the same instance.
    private static <T> Set<T> sharedEmptySet() {
        return CollectionUtilities.setOf();
    }

    private static Stream<Arguments> nullOrEmptyProvider() {
        return Stream.of(// null var‑args
        Arguments.of((Object) null), // empty array
        Arguments.of(new Object[0]));
    }

    private static Stream<Arguments> singleElementProvider() {
        return Stream.of(// non‑null element
        Arguments.of(new Object[] { 42 }), // null element
        Arguments.of(new Object[] { null }));
    }

    @Test
    @DisplayName("setOf returns the shared unmodifiable empty set when passed null")
    void setOf_NullArray_ReturnsSharedEmptySet() {
        // given
        String[] input = null;
        // when
        Set<String> result = CollectionUtilities.setOf(input);
        // then
        assertSame(CollectionUtilities.setOf(), result, "When null is supplied the method should return the same shared empty set instance");
        assertTrue(result.isEmpty(), "Returned set must be empty");
        assertThrows(UnsupportedOperationException.class, () -> result.add("x"), "Returned set must be unmodifiable");
    }

    @Test
    @DisplayName("setOf returns the shared unmodifiable empty set when passed an empty array")
    void setOf_EmptyArray_ReturnsSharedEmptySet() {
        // given
        Integer[] input = new Integer[0];
        // when
        Set<Integer> result = CollectionUtilities.setOf(input);
        // then
        assertSame(CollectionUtilities.setOf(), result, "When an empty array is supplied the method should return the same shared empty set instance");
        assertTrue(result.isEmpty(), "Returned set must be empty");
        assertThrows(UnsupportedOperationException.class, () -> result.add(1));
    }

    @Test
    @DisplayName("setOf creates an unmodifiable set containing the supplied elements")
    void setOf_NonEmptyArray_ReturnsUnmodifiableSetWithElements() {
        // given
        String a = "alpha";
        String b = "beta";
        String c = "gamma";
        // when
        Set<String> result = CollectionUtilities.setOf(a, b, c);
        // then
        assertEquals(3, result.size(), "Result set should contain exactly three elements");
        assertTrue(result.containsAll(Set.of(a, b, c)), "Result set should contain the supplied elements");
        assertThrows(UnsupportedOperationException.class, () -> result.add("delta"), "Result set must be unmodifiable");
    }

    @Test
    @DisplayName("setOf preserves insertion order using LinkedHashSet semantics")
    void setOf_PreservesInsertionOrder() {
        // given
        String first = "first";
        String second = "second";
        String third = "third";
        // when
        Set<String> result = CollectionUtilities.setOf(first, second, third);
        // then
        // LinkedHashSet preserves order, so we can compare to an expected LinkedHashSet
        Set<String> expected = new LinkedHashSet<>();
        expected.add(first);
        expected.add(second);
        expected.add(third);
        assertEquals(expected, result, "Result set should preserve the order of the supplied items");
    }

    @Test
    @DisplayName("setOf eliminates duplicate values")
    void setOf_DuplicatesAreRemoved() {
        // given
        String dup = "dup";
        // when
        Set<String> result = CollectionUtilities.setOf(dup, dup, dup);
        // then
        assertEquals(1, result.size(), "Duplicates should be collapsed to a single element");
        assertTrue(result.contains(dup));
    }

    @Test
    @DisplayName("setOf works with heterogeneous types via generic inference")
    void setOf_HeterogeneousTypes() {
        // given
        Object o1 = "string";
        Object o2 = 42;
        Object o3 = true;
        // when
        Set<Object> result = CollectionUtilities.setOf(o1, o2, o3);
        // then
        assertEquals(3, result.size());
        assertTrue(result.containsAll(Set.of(o1, o2, o3)));
    }

    @Test
    @DisplayName("setOf returns the same shared empty set instance for multiple empty calls")
    void setOf_MultipleEmptyCalls_ReturnSameInstance() {
        // when
        Set<String> first = CollectionUtilities.setOf();
        Set<String> second = CollectionUtilities.setOf(new String[0]);
        Set<String> third = CollectionUtilities.setOf((String[]) null);
        // then
        assertSame(first, second, "Empty calls should return the same shared instance");
        assertSame(first, third, "Empty calls should return the same shared instance");
    }

    @Test
    @DisplayName("setOf result cannot be cast to a mutable Set without throwing")
    void setOf_UnmodifiableBehaviour() {
        // given
        Set<String> result = CollectionUtilities.setOf("a", "b");
        // when / then
        assertThrows(UnsupportedOperationException.class, () -> ((Set<String>) result).clear(), "Attempting to modify the returned set must throw UnsupportedOperationException");
    }

    @Test
    @DisplayName("setOf works with null elements inside the array")
    void setOf_ArrayContainingNullElements() {
        // given
        String a = "a";
        String b = null;
        String c = "c";
        // when
        Set<String> result = CollectionUtilities.setOf(a, b, c);
        // then
        assertEquals(3, result.size(), "Null is a valid element and should be retained");
        assertTrue(result.contains(null), "Result set must contain the null element");
    }

    /* --------------------------------------------------------------------- *
     *  Group: Null or empty input                                            *
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => items={0}")
    @MethodSource("nullOrEmptyProvider")
    @DisplayName("setOf returns shared unmodifiable empty set for null or empty input")
    void setOf_NullOrEmpty_ReturnsSharedEmptySet(Object[] items) {
        // when
        @SuppressWarnings("unchecked")
        Set<Object> result = CollectionUtilities.setOf(items);
        // then
        // the canonical empty instance
        Set<Object> shared = CollectionUtilities.setOf();
        assertSame(shared, result, "Method must return the shared empty set instance");
        assertTrue(result.isEmpty(), "Returned set must be empty");
        assertThrows(UnsupportedOperationException.class, () -> result.add(new Object()), "Returned set must be unmodifiable");
    }

    /* --------------------------------------------------------------------- *
     *  Group: Single element                                                  *
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => items={0}")
    @MethodSource("singleElementProvider")
    @DisplayName("setOf creates an unmodifiable set containing a single element")
    void setOf_SingleElement_ReturnsUnmodifiableSet(Object[] items) {
        // when
        @SuppressWarnings("unchecked")
        Set<Object> result = CollectionUtilities.setOf(items);
        // then
        assertEquals(1, result.size(), "Set should contain exactly one element");
        assertTrue(result.contains(items[0]), "Set must contain the supplied element");
        assertThrows(UnsupportedOperationException.class, () -> result.add(new Object()), "Returned set must be unmodifiable");
    }

    /* --------------------------------------------------------------------- *
     *  Group: Multiple distinct elements                                      *
     * --------------------------------------------------------------------- */
    @Test
    @DisplayName("setOf preserves insertion order for distinct elements")
    void setOf_MultipleDistinctElements_PreservesOrder() {
        // given
        String[] items = { "apple", "banana", "cherry" };
        // when
        Set<String> result = CollectionUtilities.setOf(items);
        // then
        Set<String> expected = new LinkedHashSet<>(Arrays.asList(items));
        assertEquals(expected, result, "Result set should preserve insertion order");
        assertThrows(UnsupportedOperationException.class, () -> result.add("date"), "Returned set must be unmodifiable");
    }

    /* --------------------------------------------------------------------- *
     *  Group: Duplicate elements                                              *
     * --------------------------------------------------------------------- */
    @Test
    @DisplayName("setOf removes duplicates while preserving first‑occurrence order")
    void setOf_DuplicateElements_RemovedAndOrderPreserved() {
        // given
        String[] items = { "x", "y", "x", "z", "y" };
        // when
        Set<String> result = CollectionUtilities.setOf(items);
        // then
        Set<String> expected = new LinkedHashSet<>(Arrays.asList("x", "y", "z"));
        assertEquals(expected, result, "Duplicates must be collapsed, order of first appearance kept");
        assertThrows(UnsupportedOperationException.class, () -> result.add("new"), "Returned set must be unmodifiable");
    }

    /* --------------------------------------------------------------------- *
     *  Group: Array containing null among others                              *
     * --------------------------------------------------------------------- */
    @Test
    @DisplayName("setOf treats null as a regular element and keeps order")
    void setOf_ArrayWithNullElements_HandledCorrectly() {
        // given
        String[] items = { null, "a", null, "b" };
        // when
        Set<String> result = CollectionUtilities.setOf(items);
        // then
        Set<String> expected = new LinkedHashSet<>(Arrays.asList(null, "a", "b"));
        assertEquals(expected, result, "Null should be stored once and ordered correctly");
        assertThrows(UnsupportedOperationException.class, () -> result.remove(null), "Returned set must be unmodifiable");
    }

    /* --------------------------------------------------------------------- *
     *  Group: Immutability verification                                       *
     * --------------------------------------------------------------------- */
    @Test
    @DisplayName("setOf result is immutable – add, remove, clear all throw")
    void setOf_Immutability_AllMutatingOperationsFail() {
        // given
        Set<Integer> result = CollectionUtilities.setOf(1, 2);
        // then
        assertAll("mutating operations", () -> assertThrows(UnsupportedOperationException.class, () -> result.add(3), "add must throw"), () -> assertThrows(UnsupportedOperationException.class, () -> result.remove(1), "remove must throw"), () -> assertThrows(UnsupportedOperationException.class, () -> result.clear(), "clear must throw"));
    }

    /* --------------------------------------------------------------------- *
     *  Group: Instance uniqueness for non‑empty input                         *
     * --------------------------------------------------------------------- */
    @Test
    @DisplayName("setOf creates a new unmodifiable Set instance for each call")
    void setOf_NonEmptyCalls_CreateDistinctInstances() {
        // when
        Set<String> first = CollectionUtilities.setOf("same");
        Set<String> second = CollectionUtilities.setOf("same");
        // then
        assertNotSame(first, second, "Each invocation must produce a distinct Set object");
        assertEquals(first, second, "Content of the two sets must be equal");
    }

    /* --------------------------------------------------------------------- *
     *  Group: Original array mutation independence                             *
     * --------------------------------------------------------------------- */
    @Test
    @DisplayName("setOf does not reflect later modifications to the source array")
    void setOf_ArrayMutationDoesNotAffectReturnedSet() {
        // given
        String[] source = { "initial" };
        Set<String> result = CollectionUtilities.setOf(source);
        // mutate the original array after the call
        source[0] = "changed";
        // then
        assertEquals(1, result.size(), "Result set size must remain 1");
        assertTrue(result.contains("initial"), "Result set must still contain the original value");
        assertFalse(result.contains("changed"), "Result set must not reflect the mutated array");
    }
}
