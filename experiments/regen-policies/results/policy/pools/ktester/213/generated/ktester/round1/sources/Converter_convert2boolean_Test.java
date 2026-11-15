package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.nio.CharBuffer;
import java.time.DayOfWeek;

class Converter_convert2boolean_Test {

    private static Stream<Arguments> provideTruthyStrings() {
        return Stream.of(Arguments.of("true"), Arguments.of("TRUE"), Arguments.of("yes"), Arguments.of("1"), Arguments.of("on"));
    }

    private static Stream<Arguments> provideFalsyStrings() {
        return Stream.of(Arguments.of("false"), Arguments.of("FALSE"), Arguments.of("no"), Arguments.of("0"), Arguments.of("off"));
    }

    private static Stream<Arguments> provideNumericValues() {
        return Stream.of(Arguments.of(1, true), Arguments.of(-5, true), Arguments.of(0, false), Arguments.of(0.0, false), Arguments.of(0.0001, true), Arguments.of(123L, true), Arguments.of(0L, false));
    }

    private static Stream<Arguments> provideBigDecimalValues() {
        return Stream.of(Arguments.of(BigDecimal.ZERO, false), Arguments.of(BigDecimal.ONE, true));
    }

    private static Stream<Arguments> provideBigIntegerValues() {
        return Stream.of(Arguments.of(BigInteger.ZERO, false), Arguments.of(BigInteger.TEN, true));
    }

    private static Stream<Arguments> provideAtomicBooleanValues() {
        return Stream.of(Arguments.of(new AtomicBoolean(true), true), Arguments.of(new AtomicBoolean(false), false));
    }

    private static Stream<Arguments> provideCharBufferValues() {
        return Stream.of(Arguments.of(CharBuffer.wrap("true"), true), Arguments.of(CharBuffer.wrap("false"), false));
    }

    private static Stream<Arguments> provideUnsupportedTypes() {
        return Stream.of(Arguments.of(new Object()), // unsupported collection
        Arguments.of(List.of(1, 2, 3)), // unsupported map
        Arguments.of(Map.of("key", "value")), // enum
        Arguments.of(DayOfWeek.MONDAY));
    }

    private static Stream<Arguments> provideInvalidStrings() {
        return Stream.of(Arguments.of("maybe"), Arguments.of("2"), Arguments.of("truth"));
    }

    private static Stream<Arguments> provideInvalidNumbers() {
        return Stream.of(Arguments.of(Double.NaN), Arguments.of(Double.POSITIVE_INFINITY), Arguments.of(Double.NEGATIVE_INFINITY));
    }

    private static Stream<Arguments> provideInvalidCharBuffers() {
        return Stream.of(Arguments.of(CharBuffer.wrap("maybe")), Arguments.of(CharBuffer.wrap("2")));
    }

    /**
     * When the source object is {@code null} the method should return {@code false}
     * (as defined by the Converter's null‑handling contract for primitive booleans).
     */
    @Test
    void convert2boolean_nullInput_returnsFalse() {
        assertFalse(Converter.convert2boolean(null), "convert2boolean(null) should return false");
    }

    /**
     * Direct Boolean values must be returned unchanged.
     */
    @Test
    void convert2boolean_booleanInput_returnsSameValue() {
        assertTrue(Converter.convert2boolean(Boolean.TRUE), "convert2boolean(Boolean.TRUE) should return true");
        assertFalse(Converter.convert2boolean(Boolean.FALSE), "convert2boolean(Boolean.FALSE) should return false");
    }

    /**
     * Numeric values are converted according to the usual truthiness rules:
     * zero → false, any non‑zero → true.
     */
    @Test
    void convert2boolean_numericInput_respectsZeroAndNonZero() {
        assertFalse(Converter.convert2boolean(0), "convert2boolean(0) should return false");
        assertTrue(Converter.convert2boolean(1), "convert2boolean(1) should return true");
        assertTrue(Converter.convert2boolean(-42), "convert2boolean(-42) should return true");
        assertFalse(Converter.convert2boolean(0L), "convert2boolean(0L) should return false");
        assertTrue(Converter.convert2boolean(123L), "convert2boolean(123L) should return true");
    }

    /**
     * String representations of booleans are recognized.
     * The Converter treats the strings "true" (case‑insensitive) as {@code true},
     * and any other non‑null string as {@code false}.
     */
    @Test
    void convert2boolean_stringInput_recognizesTrueAndFalse() {
        assertTrue(Converter.convert2boolean("true"), "convert2boolean(\"true\") should return true");
        assertTrue(Converter.convert2boolean("TrUe"), "convert2boolean(\"TrUe\") should be case‑insensitive and return true");
        assertFalse(Converter.convert2boolean("false"), "convert2boolean(\"false\") should return false");
        assertFalse(Converter.convert2boolean("random"), "convert2boolean(\"random\") should return false");
    }

    /**
     * Other common types (e.g., {@link AtomicBoolean}) are also supported.
     */
    @Test
    void convert2boolean_otherSupportedTypes_behaveCorrectly() {
        assertTrue(Converter.convert2boolean(new java.util.concurrent.atomic.AtomicBoolean(true)), "AtomicBoolean(true) should convert to true");
        assertFalse(Converter.convert2boolean(new java.util.concurrent.atomic.AtomicBoolean(false)), "AtomicBoolean(false) should convert to false");
    }

    @ParameterizedTest(name = "truthy string \"{0}\" → true")
    @MethodSource("provideTruthyStrings")
    void convert2boolean_stringTruthy(Object input) {
        assertTrue(Converter.convert2boolean(input), () -> "convert2boolean(\"" + input + "\") should return true");
    }

    @ParameterizedTest(name = "falsy string \"{0}\" → false")
    @MethodSource("provideFalsyStrings")
    void convert2boolean_stringFalsy(Object input) {
        assertFalse(Converter.convert2boolean(input), () -> "convert2boolean(\"" + input + "\") should return false");
    }

    @ParameterizedTest(name = "numeric {0} → {1}")
    @MethodSource("provideNumericValues")
    void convert2boolean_numericInput(Number input, boolean expected) {
        assertEquals(expected, Converter.convert2boolean(input), () -> "convert2boolean(" + input + ") should return " + expected);
    }

    @ParameterizedTest(name = "BigDecimal {0} → {1}")
    @MethodSource("provideBigDecimalValues")
    void convert2boolean_bigDecimalInput(BigDecimal input, boolean expected) {
        assertEquals(expected, Converter.convert2boolean(input), () -> "convert2boolean(" + input + ") should return " + expected);
    }

    @ParameterizedTest(name = "BigInteger {0} → {1}")
    @MethodSource("provideBigIntegerValues")
    void convert2boolean_bigIntegerInput(BigInteger input, boolean expected) {
        assertEquals(expected, Converter.convert2boolean(input), () -> "convert2boolean(" + input + ") should return " + expected);
    }

    @ParameterizedTest(name = "AtomicBoolean {0} → {1}")
    @MethodSource("provideAtomicBooleanValues")
    void convert2boolean_atomicBooleanInput(AtomicBoolean input, boolean expected) {
        assertEquals(expected, Converter.convert2boolean(input), () -> "convert2boolean(" + input + ") should return " + expected);
    }

    @ParameterizedTest(name = "CharBuffer \"{0}\" → {1}")
    @MethodSource("provideCharBufferValues")
    void convert2boolean_charBufferInput(CharBuffer input, boolean expected) {
        assertEquals(expected, Converter.convert2boolean(input), () -> "convert2boolean(CharBuffer(\"" + input + "\")) should return " + expected);
    }

    @ParameterizedTest(name = "unsupported type {0} throws IAE")
    @MethodSource("provideUnsupportedTypes")
    void convert2boolean_unsupportedType_throwsException(Object input) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2boolean(input), () -> "convert2boolean(" + input.getClass().getSimpleName() + ") should throw IllegalArgumentException");
    }

    @ParameterizedTest(name = "invalid string \"{0}\" throws IAE")
    @MethodSource("provideInvalidStrings")
    void convert2boolean_invalidString_throwsException(String input) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2boolean(input), () -> "convert2boolean(\"" + input + "\") should throw IllegalArgumentException");
    }

    @ParameterizedTest(name = "invalid numeric {0} throws IAE")
    @MethodSource("provideInvalidNumbers")
    void convert2boolean_invalidNumeric_throwsException(Number input) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2boolean(input), () -> "convert2boolean(" + input + ") should throw IllegalArgumentException");
    }

    @ParameterizedTest(name = "invalid CharBuffer \"{0}\" throws IAE")
    @MethodSource("provideInvalidCharBuffers")
    void convert2boolean_invalidCharBuffer_throwsException(CharBuffer input) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2boolean(input), () -> "convert2boolean(CharBuffer(\"" + input + "\")) should throw IllegalArgumentException");
    }
}
