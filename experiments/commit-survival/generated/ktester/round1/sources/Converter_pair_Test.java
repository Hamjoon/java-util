package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.util.AbstractMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_pair_Test {

    /**
     * Provides a stream of arguments representing each test case defined in the
     * JSON specification. Each argument consists of:
     * <ul>
     *   <li>source class (may be {@code null})</li>
     *   <li>target class (may be {@code null})</li>
     *   <li>description – used as the display name for the parameterized test</li>
     * </ul>
     */
    private static Stream<Arguments> pairMethodCases() {
        return Stream.of(// group: pairMethodTests
        Arguments.of(String.class, Integer.class, "Basic case: both source and target are non‑null classes"), Arguments.of(null, Double.class, "Source is null, target is a valid class"), Arguments.of(Object.class, null, "Target is null, source is a valid class"), Arguments.of(null, null, "Both source and target are null"), Arguments.of(java.util.ArrayList.class, java.util.LinkedList.class, "Two different concrete collection classes"), // group: pairMethodAdditionalTests
        Arguments.of(Integer.class, Integer.class, "Source and target are the same class"), Arguments.of(int.class, Integer.class, "Source is primitive, target is its wrapper"), Arguments.of(int[].class, Object[].class, "Both source and target are array class objects"), Arguments.of(java.util.Map.Entry.class, java.util.AbstractMap.SimpleEntry.class, "Source is an interface, target is a concrete implementation"), Arguments.of(Void.class, void.class, "Edge case using Void wrapper and primitive void"), Arguments.of(String[].class, null, "Source is an array class while target is null"), Arguments.of(null, int.class, "Target is a primitive type class and source is null"));
    }

    @Test
    void pairReturnsEntryWithGivenClasses() {
        // GIVEN
        Class<?> source = String.class;
        Class<?> target = Integer.class;
        // WHEN
        Map.Entry<Class<?>, Class<?>> entry = Converter.pair(source, target);
        // THEN
        assertNotNull(entry, "pair should never return null");
        assertSame(source, entry.getKey(), "Key should be the exact source class supplied");
        assertSame(target, entry.getValue(), "Value should be the exact target class supplied");
        // Verify that the entry equals another entry with the same key/value
        Map.Entry<Class<?>, Class<?>> expected = new AbstractMap.SimpleImmutableEntry<>(source, target);
        assertEquals(expected, entry, "Entry should be equal to a SimpleImmutableEntry with same key/value");
    }

    @Test
    void pairCreatesImmutableEntry() {
        // GIVEN
        Class<?> source = Double.class;
        Class<?> target = Float.class;
        Map.Entry<Class<?>, Class<?>> entry = Converter.pair(source, target);
        // WHEN / THEN
        assertThrows(UnsupportedOperationException.class, () -> entry.setValue(String.class), "SimpleImmutableEntry#setValue should throw UnsupportedOperationException");
    }

    @Test
    void pairHandlesNullArguments() {
        // GIVEN
        Class<?> source = null;
        Class<?> target = null;
        // WHEN
        Map.Entry<Class<?>, Class<?>> entry = Converter.pair(source, target);
        // THEN
        assertNotNull(entry, "pair should return a non‑null entry even when both arguments are null");
        assertNull(entry.getKey(), "Key should be null when source argument is null");
        assertNull(entry.getValue(), "Value should be null when target argument is null");
    }

    @Test
    void pairWorksWithPrimitiveTypes() {
        // GIVEN
        // primitive type
        Class<?> source = int.class;
        // primitive type
        Class<?> target = boolean.class;
        // WHEN
        Map.Entry<Class<?>, Class<?>> entry = Converter.pair(source, target);
        // THEN
        assertSame(source, entry.getKey(), "Key should be the primitive class supplied");
        assertSame(target, entry.getValue(), "Value should be the primitive class supplied");
    }

    /**
     * Verifies that {@link Converter#pair(Class, Class)} returns an immutable
     * {@link java.util.Map.Entry} containing exactly the supplied key/value
     * class objects for each scenario described in {@link #pairMethodCases()}.
     *
     * @param source      the source {@link Class} (may be {@code null})
     * @param target      the target {@link Class} (may be {@code null})
     * @param description textual description of the test case (used for display)
     */
    @ParameterizedTest(name = "{2}")
    @MethodSource("pairMethodCases")
    void pairReturnsCorrectImmutableEntry(Class<?> source, Class<?> target, String description) {
        // WHEN
        Map.Entry<Class<?>, Class<?>> entry = Converter.pair(source, target);
        // THEN – basic contract
        assertNotNull(entry, "pair must never return null");
        assertSame(source, entry.getKey(), () -> "Key should be the exact source class supplied (case: " + description + ")");
        assertSame(target, entry.getValue(), () -> "Value should be the exact target class supplied (case: " + description + ")");
        // THEN – immutability
        assertThrows(UnsupportedOperationException.class, () -> entry.setValue(Object.class), () -> "SimpleImmutableEntry#setValue must throw UnsupportedOperationException (case: " + description + ")");
        // THEN – equality with a freshly created SimpleImmutableEntry
        Map.Entry<Class<?>, Class<?>> expected = new AbstractMap.SimpleImmutableEntry<>(source, target);
        assertEquals(expected, entry, () -> "Entry should be equal to a SimpleImmutableEntry with same key/value (case: " + description + ")");
    }
}
