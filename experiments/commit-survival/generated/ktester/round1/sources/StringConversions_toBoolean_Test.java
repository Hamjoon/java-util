package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toBoolean_Test {

    /**
     * Mocked Converter – not used by the method under test but required by the signature.
     */
    @Mock
    private Converter converter;

    /**
     * Supplies all inputs that should return a Boolean value (true/false).
     * The data mirrors the groups defined in the supplied JSON.
     */
    private static Stream<Arguments> provideBooleanReturnValues() {
        return Stream.of(// Exact match true/false literals
        Arguments.of("true", true), Arguments.of("false", false), // Case‑insensitive true equivalents
        Arguments.of("TRUE", true), Arguments.of("t", true), Arguments.of("T", true), Arguments.of("1", true), Arguments.of("y", true), Arguments.of("Y", true), Arguments.of("\"true\"", true), Arguments.of("\"TrUe\"", true), // Negative / non‑true values
        Arguments.of("no", false), Arguments.of("", false), Arguments.of(null, false), // Whitespace handling (method does not trim)
        Arguments.of(" true ", false), Arguments.of("\ttrue\n", false), // Mixed case variations
        Arguments.of("TrUe", true), Arguments.of("FaLsE", false), // Numeric edge cases
        Arguments.of("0", false), Arguments.of("-1", false), // Quoted true variations
        // single‑quoted, should be false
        Arguments.of("'true'", false), // case‑insensitive quoted true
        Arguments.of("\"True\"", true));
    }

    /**
     * Supplies inputs that are not {@code String} instances and therefore
     * trigger a {@link ClassCastException}.
     */
    private static Stream<Arguments> provideExceptionValues() {
        return Stream.of(Arguments.of(new StringBuilder("true")), Arguments.of(12345));
    }

    @Test
    void shouldReturnTrueWhenExactTrueString() {
        assertTrue(StringConversions.toBoolean("true", converter));
    }

    @Test
    void shouldReturnTrueWhenExactFalseString() {
        assertFalse(StringConversions.toBoolean("false", converter));
    }

    @Test
    void shouldReturnTrueWhenCaseInsensitiveTrue() {
        assertTrue(StringConversions.toBoolean("TrUe", converter));
    }

    @Test
    void shouldReturnTrueWhenSingleLetterT() {
        assertTrue(StringConversions.toBoolean("t", converter));
    }

    @Test
    void shouldReturnTrueWhenSingleLetterTUpperCase() {
        assertTrue(StringConversions.toBoolean("T", converter));
    }

    @Test
    void shouldReturnTrueWhenNumericOne() {
        assertTrue(StringConversions.toBoolean("1", converter));
    }

    @Test
    void shouldReturnTrueWhenLetterY() {
        assertTrue(StringConversions.toBoolean("y", converter));
    }

    @Test
    void shouldReturnTrueWhenLetterYUpperCase() {
        assertTrue(StringConversions.toBoolean("Y", converter));
    }

    @Test
    void shouldReturnTrueWhenQuotedTrue() {
        assertTrue(StringConversions.toBoolean("\"true\"", converter));
    }

    @Test
    void shouldReturnTrueWhenQuotedTrueMixedCase() {
        assertTrue(StringConversions.toBoolean("\"TrUe\"", converter));
    }

    @Test
    void shouldReturnFalseForUnrecognisedString() {
        assertFalse(StringConversions.toBoolean("maybe", converter));
    }

    @Test
    void shouldReturnFalseForNumericZero() {
        assertFalse(StringConversions.toBoolean("0", converter));
    }

    @Test
    void shouldReturnFalseForEmptyString() {
        assertFalse(StringConversions.toBoolean("", converter));
    }

    @Test
    void shouldReturnFalseForWhitespaceString() {
        assertFalse(StringConversions.toBoolean("   ", converter));
    }

    @Test
    void shouldReturnFalseForRandomQuoteString() {
        assertFalse(StringConversions.toBoolean("\"false\"", converter));
    }

    @Test
    void shouldReturnFalseWhenExactFalseString() {
        assertFalse(StringConversions.toBoolean("false", converter));
    }

    /**
     * Tests that the method returns the expected {@link Boolean} value for a wide
     * range of string inputs (including {@code null}).
     */
    @ParameterizedTest(name = "[{index}] toBoolean({0}) ⇒ {1}")
    @MethodSource("provideBooleanReturnValues")
    void testToBooleanReturnsExpected(Object from, Boolean expected) {
        Boolean actual = StringConversions.toBoolean(from, converter);
        assertEquals(expected, actual, () -> "Expected toBoolean(" + String.valueOf(from) + ") to be " + expected);
    }

    /**
     * Tests that non‑String inputs cause a {@link ClassCastException} because the
     * implementation casts the argument directly to {@code String}.
     */
    @ParameterizedTest(name = "[{index}] toBoolean({0}) throws ClassCastException")
    @MethodSource("provideExceptionValues")
    void testToBooleanThrowsClassCastException(Object from) {
        assertThrows(ClassCastException.class, () -> StringConversions.toBoolean(from, converter), () -> "Expected ClassCastException for input: " + from);
    }
}
