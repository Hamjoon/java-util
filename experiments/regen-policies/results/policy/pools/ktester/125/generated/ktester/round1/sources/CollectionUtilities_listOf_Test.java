package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

class CollectionUtilities_listOf_Test {

    /**
     * Supplies arguments for {@link #listOf_VariousInputs_ReturnsExpectedList(Object[], List, String)}.
     */
    static Stream<Arguments> provideVariedInputs() {
        return Stream.of(// single element
        Arguments.of(new Integer[] { 42 }, List.of(42), "single element test"), // multiple elements preserving order
        Arguments.of(new String[] { "first", "second", "third" }, List.of("first", "second", "third"), "multiple elements preserving order"), // contains null elements (nulls are allowed)
        Arguments.of(new Object[] { null, "a", null }, List.of(null, "a", null), "null elements inside list"), // duplicate elements
        Arguments.of(new Integer[] { 5, 5, 5 }, List.of(5, 5, 5), "duplicate elements test"));
    }

    @Test
    void listOf_WithNullArray_ReturnsEmptyUnmodifiableList() {
        // given
        String[] nullArray = null;
        // when
        List<String> result = CollectionUtilities.listOf(nullArray);
        // then
        assertNotNull(result, "Result should never be null");
        assertTrue(result.isEmpty(), "Result list must be empty");
        assertThrows(UnsupportedOperationException.class, () -> result.add("x"));
    }

    @Test
    void listOf_WithNoArguments_ReturnsEmptyUnmodifiableList() {
        // when
        List<String> result = CollectionUtilities.listOf();
        // then
        assertNotNull(result);
        assertTrue(result.isEmpty(), "Result list must be empty");
        assertThrows(UnsupportedOperationException.class, () -> result.add("x"));
    }

    @Test
    void listOf_WithMultipleElements_ReturnsCorrectUnmodifiableList() {
        // when
        List<String> result = CollectionUtilities.listOf("A", "B", "C");
        // then
        assertEquals(3, result.size());
        assertEquals("A", result.get(0));
        assertEquals("B", result.get(1));
        assertEquals("C", result.get(2));
        assertThrows(UnsupportedOperationException.class, () -> result.remove(0));
    }

    @Test
    void listOf_ReturnedListIsIndependentFromSourceArray() {
        // given
        String[] source = new String[] { "first", "second" };
        List<String> result = CollectionUtilities.listOf(source);
        // when
        source[0] = "changed";
        // then
        assertEquals("first", result.get(0), "List should contain the original value, not the mutated array element");
    }

    @Test
    void listOf_WithDifferentGenericType_ReturnsCorrectList() {
        // when
        List<Integer> result = CollectionUtilities.listOf(1, 2, 3, 4);
        // then
        assertEquals(4, result.size());
        assertIterableEquals(List.of(1, 2, 3, 4), result);
        assertThrows(UnsupportedOperationException.class, () -> result.add(5));
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("provideVariedInputs")
    void listOf_VariousInputs_ReturnsExpectedList(Object[] items, List<?> expected, String description) {
        // when
        @SuppressWarnings("unchecked")
        List<Object> result = CollectionUtilities.listOf(items);
        // then
        assertEquals(expected.size(), result.size(), "Size mismatch for " + description);
        assertIterableEquals(expected, result, "Content mismatch for " + description);
        assertThrows(UnsupportedOperationException.class, () -> result.add(new Object()), "List must be unmodifiable for " + description);
    }

    @Test
    void listOf_SharedEmptyListInstance_IsIdenticalForNullAndEmptyArray() {
        // null input
        List<?> fromNull = CollectionUtilities.listOf((Object[]) null);
        // empty var‑args array
        List<?> fromEmpty = CollectionUtilities.listOf();
        // both should refer to the same singleton instance
        assertSame(fromNull, fromEmpty, "Both calls must return the identical shared empty list instance");
    }

    @Test
    void listOf_IteratorRemove_ThrowsUnsupportedOperationException() {
        // given
        List<String> list = CollectionUtilities.listOf("x", "y");
        Iterator<String> iterator = list.iterator();
        // when & then
        assertTrue(iterator.hasNext(), "Iterator should have elements");
        // advance to first element
        iterator.next();
        assertThrows(UnsupportedOperationException.class, iterator::remove, "Iterator.remove() must throw UnsupportedOperationException");
    }

    @Test
    void listOf_ListSetOperation_ThrowsUnsupportedOperationException() {
        // given
        List<String> list = CollectionUtilities.listOf("a");
        // then
        assertThrows(UnsupportedOperationException.class, () -> list.set(0, "b"), "List.set(...) must throw UnsupportedOperationException");
    }

    @Test
    void listOf_GenericTypeSafety_WithMixedNumberSubtypes() {
        // when
        List<Number> result = CollectionUtilities.listOf(1, 2.5);
        // then
        assertEquals(2, result.size(), "Result list should contain two elements");
        assertEquals(Integer.valueOf(1), result.get(0), "First element should be Integer 1");
        assertEquals(Double.valueOf(2.5), result.get(1), "Second element should be Double 2.5");
        assertThrows(UnsupportedOperationException.class, () -> result.add(3L), "Result list must be immutable even for Number type");
    }

    @Test
    void listOf_SharedEmptyList_IsUnmodifiable() {
        List<?> empty = CollectionUtilities.listOf();
        assertTrue(empty.isEmpty(), "Shared empty list must be empty");
        assertThrows(UnsupportedOperationException.class, () -> empty.add("anything"));
        assertThrows(UnsupportedOperationException.class, () -> empty.remove("anything"));
    }
}
