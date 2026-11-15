package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigInteger;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toBigInteger_Test {

    /**
     * Mocked Converter – the method does not use it, but it satisfies the signature.
     */
    @Mock
    private Converter converter;

    /**
     * Mocked Converter – the method does not use it, but it satisfies the signature.
     */
    @Mock
    private Converter mockConverter;

    static Stream<Arguments> validInputs() {
        return Stream.of(// Empty / whitespace strings – should be treated as zero.
        Arguments.of("", BigInteger.ZERO), Arguments.of("0", BigInteger.ZERO), Arguments.of("   ", BigInteger.ZERO), Arguments.of("\n\t", BigInteger.ZERO), // Simple integer strings.
        Arguments.of("12345678901234567890", new BigInteger("12345678901234567890")), Arguments.of("-987654321", new BigInteger("-987654321")), // Leading/trailing whitespace is ignored by BigDecimal.
        Arguments.of("   42   ", new BigInteger("42")), Arguments.of(" +42 ", new BigInteger("42")), // Explicit plus sign.
        Arguments.of("+123", new BigInteger("123")), // Leading zeros.
        Arguments.of("000123", new BigInteger("123")), Arguments.of("-000123", new BigInteger("-123")), // Decimal numbers – fraction is discarded (truncation toward zero).
        Arguments.of("12.34", new BigInteger("12")), Arguments.of("+12.9", new BigInteger("12")), Arguments.of("123.999", new BigInteger("123")), // Scientific notation.
        Arguments.of("1e3", new BigInteger("1000")), Arguments.of("1e-3", BigInteger.ZERO));
    }

    static Stream<Arguments> illegalInputs() {
        return Stream.of(Arguments.of("notANumber", "notANumber"), Arguments.of("+", "+"), Arguments.of("-", "-"), Arguments.of("++123", "++123"), Arguments.of("1 2", "1 2"), Arguments.of("0xFF", "0xFF"), Arguments.of("NaN", "NaN"), // non‑breaking space
        Arguments.of("\u00A0", "\\u00A0"));
    }

    static Stream<Arguments> nonStringInputs() {
        return Stream.of(// Integer
        Arguments.of(5), // StringBuilder
        Arguments.of(new StringBuilder("123")));
    }

    @Test
    void returnsZeroWhenInputIsNull() {
        assertEquals(BigInteger.ZERO, StringConversions.toBigInteger(null, converter), "Null input should be interpreted as an empty string and return BigInteger.ZERO");
    }

    @Test
    void returnsZeroWhenInputIsEmptyString() {
        assertEquals(BigInteger.ZERO, StringConversions.toBigInteger("", converter), "Empty string should return BigInteger.ZERO");
    }

    @Test
    void parsesSimpleIntegerString() {
        String input = "12345678901234567890";
        assertEquals(new BigInteger(input), StringConversions.toBigInteger(input, converter), "A plain integer string should be parsed correctly");
    }

    @Test
    void parsesDecimalStringTruncatingFraction() {
        // BigDecimal("123.999") -> toBigInteger() yields 123 (fraction is discarded)
        String input = "123.999";
        assertEquals(new BigInteger("123"), StringConversions.toBigInteger(input, converter), "Decimal strings should be parsed as BigDecimal and truncated to a BigInteger");
    }

    @Test
    void throwsIllegalArgumentExceptionForNonNumericString() {
        String input = "not-a-number";
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toBigInteger(input, converter));
        assertTrue(ex.getMessage().contains("not parseable as a BigInteger"), "Exception message should indicate the value is not parseable");
        assertNotNull(ex.getCause(), "The thrown IllegalArgumentException should wrap the original parsing exception");
    }

    @ParameterizedTest(name = "[{index}] ''{0}'' → {1}")
    @MethodSource("validInputs")
    void convertsValidStringToBigInteger(Object input, BigInteger expected) {
        // The mock converter is ignored by the implementation.
        assertEquals(expected, StringConversions.toBigInteger(input, mockConverter), () -> "Input '" + input + "' should be parsed to " + expected);
    }

    /**
     * Same successful conversion tests but with a {@code null} converter.
     * This verifies that the method truly ignores the converter argument.
     */
    @ParameterizedTest(name = "[nullConverter] ''{0}'' → {1}")
    @MethodSource("validInputs")
    void convertsValidStringToBigInteger_WithNullConverter(Object input, BigInteger expected) {
        assertEquals(expected, StringConversions.toBigInteger(input, null), () -> "Input '" + input + "' should be parsed to " + expected + " even when converter is null");
    }

    @ParameterizedTest(name = "[{index}] IllegalArgumentException for ''{0}''")
    @MethodSource("illegalInputs")
    void throwsIllegalArgumentExceptionForInvalidString(String input, String expectedFragment) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toBigInteger(input, mockConverter), () -> "Input '" + input + "' should cause IllegalArgumentException");
        // Verify that the message contains the original value and the expected fragment.
        assertTrue(ex.getMessage().contains("Value '" + input + "' not parseable as a BigInteger value."), "Exception message must contain the original value");
        assertNotNull(ex.getCause(), "The IllegalArgumentException should wrap the original parsing exception");
    }

    @ParameterizedTest(name = "[{index}] ClassCastException for non‑String input {0}")
    @MethodSource("nonStringInputs")
    void throwsClassCastExceptionWhenInputIsNotString(Object nonStringInput) {
        assertThrows(ClassCastException.class, () -> StringConversions.toBigInteger(nonStringInput, mockConverter), () -> "Non‑String input '" + nonStringInput + "' should cause ClassCastException");
    }
}
