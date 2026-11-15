package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

class Converter_convert2AtomicBoolean_Test {

    private static Stream<Arguments> booleanInputProvider() {
        return Stream.of(Arguments.of(Boolean.TRUE, true), Arguments.of(Boolean.FALSE, false));
    }

    private static Stream<Arguments> stringInputProvider() {
        return Stream.of(// lower‑case
        Arguments.of("true", true), // upper‑case
        Arguments.of("TRUE", true), Arguments.of("false", false), // mixed case
        Arguments.of("FaLsE", false), // not recognised as true
        Arguments.of("yes", false));
    }

    private static Stream<Arguments> numericInputProvider() {
        return Stream.of(Arguments.of(1, true), Arguments.of(0, false), Arguments.of(-42, true), Arguments.of(0.0, false), Arguments.of(3.14, true), // very small non‑zero double
        Arguments.of(1e-07, true));
    }

    private static Stream<Arguments> atomicBooleanInputProvider() {
        return Stream.of(Arguments.of(new AtomicBoolean(true), true), Arguments.of(new AtomicBoolean(false), false));
    }

    private static Stream<Arguments> unsupportedInputProvider() {
        return Stream.of(// An empty map (plain Object) – not a supported source for boolean conversion
        Arguments.of(new java.util.HashMap<>()), // A list that cannot be sensibly interpreted as a boolean
        Arguments.of(java.util.List.of("unexpected", "list")));
    }

    @Test
    @DisplayName("convert2AtomicBoolean returns false AtomicBoolean for null input")
    void testConvertNullReturnsFalse() {
        // given
        Object input = null;
        // when
        AtomicBoolean result = Converter.convert2AtomicBoolean(input);
        // then
        assertNotNull(result, "Result should never be null");
        assertFalse(result.get(), "AtomicBoolean should be false when input is null");
    }

    @Test
    @DisplayName("convert2AtomicBoolean correctly converts various truthy/falsy values")
    void testConvertVariousValues() {
        // true‑like values
        Object[] truthy = { "true", true, 1, 1L, 1.0, "yes", "1" };
        for (Object val : truthy) {
            AtomicBoolean result = Converter.convert2AtomicBoolean(val);
            assertTrue(result.get(), "Expected true for input: " + val);
        }
        // false‑like values
        Object[] falsy = { "false", false, 0, 0L, 0.0, "no", "0", "" };
        for (Object val : falsy) {
            AtomicBoolean result = Converter.convert2AtomicBoolean(val);
            assertFalse(result.get(), "Expected false for input: " + val);
        }
    }

    // -------------------- Boolean Input --------------------
    @ParameterizedTest(name = "Boolean input {0} should become {1}")
    @MethodSource("booleanInputProvider")
    void testBooleanInput(Object input, boolean expected) {
        AtomicBoolean result = Converter.convert2AtomicBoolean(input);
        assertEquals(expected, result.get(), () -> "Boolean conversion failed for input: " + input);
    }

    // -------------------- String Input --------------------
    @ParameterizedTest(name = "String input \"{0}\" should become {1}")
    @MethodSource("stringInputProvider")
    void testStringInput(String input, boolean expected) {
        AtomicBoolean result = Converter.convert2AtomicBoolean(input);
        assertEquals(expected, result.get(), () -> "String conversion failed for input: \"" + input + "\"");
    }

    // -------------------- Numeric Input --------------------
    @ParameterizedTest(name = "Numeric input {0} should become {1}")
    @MethodSource("numericInputProvider")
    void testNumericInput(Number input, boolean expected) {
        AtomicBoolean result = Converter.convert2AtomicBoolean(input);
        assertEquals(expected, result.get(), () -> "Numeric conversion failed for input: " + input);
    }

    // -------------------- AtomicBoolean Input --------------------
    @ParameterizedTest(name = "AtomicBoolean input {0} should become {1}")
    @MethodSource("atomicBooleanInputProvider")
    void testAtomicBooleanInput(AtomicBoolean input, boolean expected) {
        AtomicBoolean result = Converter.convert2AtomicBoolean(input);
        assertEquals(expected, result.get(), () -> "AtomicBoolean conversion failed for input: " + input);
    }

    // -------------------- Unsupported Input --------------------
    @ParameterizedTest(name = "Unsupported input {0} should become false")
    @MethodSource("unsupportedInputProvider")
    void testUnsupportedInput(Object input) {
        AtomicBoolean result = Converter.convert2AtomicBoolean(input);
        assertFalse(result.get(), () -> "Unsupported type should convert to false, but got true for input: " + input);
    }
}
