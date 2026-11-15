package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toAtomicBoolean_Test {

    /**
     * Shared {@link Converter} used for all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance – the default constructor is sufficient for the tests.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No external resources to clean up, but the reference is cleared for completeness.
        converter = null;
    }

    private static Stream<Arguments> standardConversionsProvider() {
        return Stream.of(// case‑insensitive true
        Arguments.of("true", true), // case‑insensitive false
        Arguments.of("FALSE", false), // Boolean object true
        Arguments.of(Boolean.TRUE, true), // Boolean object false
        Arguments.of(Boolean.FALSE, false), // Integer 1 → true
        Arguments.of(1, true), // Integer 0 → false
        Arguments.of(0, false));
    }

    private static Stream<Arguments> nullAndEmptyProvider() {
        return Stream.of(// null source
        Arguments.of(null), // empty string
        Arguments.of(""), // whitespace only
        Arguments.of("   "));
    }

    private static Stream<Arguments> numericTypesProvider() {
        return Stream.of(Arguments.of(Long.valueOf(1L), "Long 1L", true), Arguments.of(Long.valueOf(0L), "Long 0L", false), Arguments.of(Double.valueOf(0.5), "Double 0.5", true), Arguments.of(Double.valueOf(0.0), "Double 0.0", false), Arguments.of(new BigDecimal("123"), "BigDecimal 123", true), Arguments.of(new BigDecimal("0"), "BigDecimal 0", false), Arguments.of(Byte.valueOf((byte) 2), "Byte 2", true), Arguments.of(Byte.valueOf((byte) 0), "Byte 0", false), Arguments.of(Short.valueOf((short) -3), "Short -3", true));
    }

    private static Stream<Arguments> charSequenceAndBooleanObjectsProvider() {
        return Stream.of(Arguments.of(new AtomicBoolean(true), true), Arguments.of(new AtomicBoolean(false), false), Arguments.of(new StringBuilder("TrUe"), true), Arguments.of(new StringBuilder("false"), false), // string with surrounding whitespace
        Arguments.of("  true  ", true));
    }

    private static Stream<Arguments> unsupportedTypesProvider() {
        return Stream.of(Arguments.of(new Object()), Arguments.of(new int[] { 1, 2 }), Arguments.of(Collections.singletonList("true")));
    }

    private static Stream<Arguments> invalidStringProvider() {
        return Stream.of(Arguments.of("notabool"), Arguments.of("123abc"), // CharSequence that resolves to a String
        Arguments.of(new StringBuilder("maybe").toString()));
    }

    private static Stream<Arguments> nanInfinityProvider() {
        return Stream.of(Arguments.of(Double.NaN), Arguments.of(Float.POSITIVE_INFINITY), Arguments.of(Float.NEGATIVE_INFINITY));
    }

    @Test
    void shouldConvertBooleanTrueToAtomicBoolean() {
        AtomicBoolean result = StringConversions.toAtomicBoolean(Boolean.TRUE, converter);
        assertNotNull(result);
        assertTrue(result.get(), "AtomicBoolean should be true when source is Boolean.TRUE");
    }

    @Test
    void shouldConvertBooleanFalseToAtomicBoolean() {
        AtomicBoolean result = StringConversions.toAtomicBoolean(Boolean.FALSE, converter);
        assertNotNull(result);
        assertFalse(result.get(), "AtomicBoolean should be false when source is Boolean.FALSE");
    }

    @Test
    void shouldConvertStringTrueToAtomicBoolean() {
        AtomicBoolean result = StringConversions.toAtomicBoolean("true", converter);
        assertNotNull(result);
        assertTrue(result.get(), "AtomicBoolean should be true when source string is \"true\"");
    }

    @Test
    void shouldConvertStringFalseToAtomicBoolean() {
        AtomicBoolean result = StringConversions.toAtomicBoolean("false", converter);
        assertNotNull(result);
        assertFalse(result.get(), "AtomicBoolean should be false when source string is \"false\"");
    }

    @Test
    void shouldTreatNullAsFalse() {
        AtomicBoolean result = StringConversions.toAtomicBoolean(null, converter);
        assertNotNull(result);
        assertFalse(result.get(), "AtomicBoolean should be false when source is null (per Converter null‑handling)");
    }

    @Test
    void shouldConvertNumericOneToAtomicBooleanTrue() {
        // The Converter treats non‑zero numbers as true when converting to Boolean.
        AtomicBoolean result = StringConversions.toAtomicBoolean(1, converter);
        assertNotNull(result);
        assertTrue(result.get(), "AtomicBoolean should be true when source numeric value is 1");
    }

    @Test
    void shouldConvertNumericZeroToAtomicBooleanFalse() {
        AtomicBoolean result = StringConversions.toAtomicBoolean(0, converter);
        assertNotNull(result);
        assertFalse(result.get(), "AtomicBoolean should be false when source numeric value is 0");
    }

    @ParameterizedTest(name = "[{index}] from={0} → expected={1}")
    @MethodSource("standardConversionsProvider")
    void standardConversions(Object from, boolean expected) {
        AtomicBoolean result = StringConversions.toAtomicBoolean(from, converter);
        assertNotNull(result);
        assertEquals(expected, result.get(), () -> "AtomicBoolean should be " + expected + " for source: " + from);
    }

    @ParameterizedTest(name = "[{index}] from={0} → false")
    @MethodSource("nullAndEmptyProvider")
    void nullAndEmptyInputs(Object from) {
        AtomicBoolean result = StringConversions.toAtomicBoolean(from, converter);
        assertNotNull(result);
        assertFalse(result.get(), () -> "AtomicBoolean should be false for source: " + (from == null ? "null" : "'" + from + "'"));
    }

    @ParameterizedTest(name = "[{index}] from={0} ({1}) → expected={2}")
    @MethodSource("numericTypesProvider")
    void numericTypes(Object from, String description, boolean expected) {
        AtomicBoolean result = StringConversions.toAtomicBoolean(from, converter);
        assertNotNull(result);
        assertEquals(expected, result.get(), () -> "AtomicBoolean should be " + expected + " for " + description);
    }

    @ParameterizedTest(name = "[{index}] from={0} → expected={1}")
    @MethodSource("charSequenceAndBooleanObjectsProvider")
    void charSequenceAndBooleanObjects(Object from, boolean expected) {
        AtomicBoolean result = StringConversions.toAtomicBoolean(from, converter);
        assertNotNull(result);
        assertEquals(expected, result.get(), () -> "AtomicBoolean should be " + expected + " for source: " + from);
    }

    @ParameterizedTest(name = "[{index}] from={0} (unsupported) → IllegalArgumentException")
    @MethodSource("unsupportedTypesProvider")
    void unsupportedTypesShouldThrow(Object from) {
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toAtomicBoolean(from, converter), () -> "Expected IllegalArgumentException for unsupported source: " + from);
    }

    @ParameterizedTest(name = "[{index}] from=\"{0}\" → IllegalArgumentException")
    @MethodSource("invalidStringProvider")
    void invalidStringFormatsShouldThrow(String from) {
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toAtomicBoolean(from, converter), () -> "Expected IllegalArgumentException for invalid string: \"" + from + "\"");
    }

    @ParameterizedTest(name = "[{index}] from={0} → IllegalArgumentException")
    @MethodSource("nanInfinityProvider")
    void nanAndInfinityShouldThrow(Object from) {
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toAtomicBoolean(from, converter), () -> "Expected IllegalArgumentException for special numeric value: " + from);
    }
}
