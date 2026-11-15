package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toBigDecimal_Test {

    @Mock
    private Converter // not used by the method but required by signature
    mockConverter;

    /**
     * Valid inputs that should be parsed into the exact {@link BigDecimal} value.
     */
    private static Stream<Arguments> validBigDecimalProvider() {
        return Stream.of(// empty string -> ZERO (handled separately)
        Arguments.of("", BigDecimal.ZERO, "empty string"), // whitespace‑only string -> ZERO (handled separately)
        Arguments.of("   ", BigDecimal.ZERO, "whitespace only"), // regular decimal
        Arguments.of("123.45", new BigDecimal("123.45"), "regular decimal"), // negative decimal
        Arguments.of("-0.001", new BigDecimal("-0.001"), "negative decimal"), // scientific notation (upper‑case)
        Arguments.of("1E+3", new BigDecimal("1E+3"), "scientific upper‑case"), // scientific notation (lower‑case)
        Arguments.of("1e-10", new BigDecimal("1e-10"), "scientific lower‑case"), // explicit plus sign
        Arguments.of("+123.45", new BigDecimal("+123.45"), "explicit plus sign"), // zero
        Arguments.of("0", new BigDecimal("0"), "plain zero"), // negative zero
        Arguments.of("-0", new BigDecimal("-0"), "negative zero"), // leading dot
        Arguments.of(".5", new BigDecimal(".5"), "leading dot"), // trailing dot
        Arguments.of("5.", new BigDecimal("5."), "trailing dot"), // leading zeros and trailing zeros (scale preserved)
        Arguments.of("000123.4500", new BigDecimal("000123.4500"), "leading & trailing zeros"));
    }

    /**
     * Inputs that should cause an {@link IllegalArgumentException} with a message containing the original value.
     */
    private static Stream<Arguments> illegalArgumentProvider() {
        return Stream.of(Arguments.of("NaN", "NaN"), Arguments.of("abc123", "abc123"), Arguments.of(" 123.45 ", " 123.45 "), Arguments.of("$123.45", "$123.45"), Arguments.of("1.2.3", "1.2.3"), Arguments.of("0xFF", "0xFF"), Arguments.of("1E", "1E"), Arguments.of("1E+", "1E+"), Arguments.of("+ 123", "+ 123"), Arguments.of("\n123", "\n123"), Arguments.of(".", "."), Arguments.of("+", "+"), Arguments.of("-", "-"));
    }

    /**
     * Inputs that are not {@link String} (or are {@code null}) and should raise {@link ClassCastException}.
     */
    private static Stream<Arguments> classCastProvider() {
        return Stream.of(Arguments.of(null, "null"), Arguments.of(123, "Integer"), Arguments.of(true, "Boolean"), Arguments.of(new StringBuilder("123"), "StringBuilder"));
    }

    @Test
    void toBigDecimal_EmptyString_ReturnsZero() {
        // GIVEN
        String empty = "";
        // WHEN
        BigDecimal result = StringConversions.toBigDecimal(empty, mockConverter);
        // THEN
        assertEquals(BigDecimal.ZERO, result, "Empty string should be converted to BigDecimal.ZERO");
    }

    @Test
    void toBigDecimal_ValidNumberString_ReturnsExpectedBigDecimal() {
        // GIVEN
        String number = "12345.6789";
        BigDecimal expected = new BigDecimal(number);
        // WHEN
        BigDecimal result = StringConversions.toBigDecimal(number, mockConverter);
        // THEN
        assertEquals(expected, result, "The string should be parsed into the correct BigDecimal value");
    }

    @Test
    void toBigDecimal_InvalidNumberString_ThrowsIllegalArgumentException() {
        // GIVEN
        String badNumber = "12.34.56";
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toBigDecimal(badNumber, mockConverter), "Non‑numeric strings must cause IllegalArgumentException");
        assertTrue(ex.getCause() instanceof NumberFormatException, "The cause of the exception should be a NumberFormatException");
        assertTrue(ex.getMessage().contains(badNumber), "Exception message should contain the original value");
    }

    @ParameterizedTest(name = "[{index}] valid: \"{2}\"")
    @MethodSource("validBigDecimalProvider")
    void toBigDecimal_ValidInputs_ReturnExpectedBigDecimal(String input, BigDecimal expected, String description) {
        // WHEN
        BigDecimal result = StringConversions.toBigDecimal(input, mockConverter);
        // THEN
        assertEquals(expected, result, () -> "Failed for case: " + description);
    }

    @ParameterizedTest(name = "[{index}] illegal: \"{0}\"")
    @MethodSource("illegalArgumentProvider")
    void toBigDecimal_InvalidInputs_ThrowsIllegalArgumentException(String input, String originalValue) {
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toBigDecimal(input, mockConverter), () -> "Expected IllegalArgumentException for input: " + originalValue);
        // The cause must be a NumberFormatException
        assertTrue(ex.getCause() instanceof NumberFormatException, "Cause should be NumberFormatException for input: " + originalValue);
        // Exception message must contain the original value
        assertTrue(ex.getMessage().contains(originalValue), "Exception message should contain the original value: " + originalValue);
    }

    @ParameterizedTest(name = "[{index}] class‑cast: {1}")
    @MethodSource("classCastProvider")
    void toBigDecimal_NonStringInput_ThrowsClassCastException(Object input, String typeDescription) {
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> StringConversions.toBigDecimal(input, mockConverter), () -> "Expected ClassCastException when passing a non‑String (" + typeDescription + ")");
    }

    @Test
    void toBigDecimal_WhitespaceOnlyString_ReturnsZero() {
        // GIVEN
        String whitespace = "   ";
        // WHEN
        BigDecimal result = StringConversions.toBigDecimal(whitespace, mockConverter);
        // THEN
        assertEquals(BigDecimal.ZERO, result, "Whitespace‑only string should be treated as empty and return ZERO");
    }
}
