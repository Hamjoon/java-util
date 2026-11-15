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

    @Mock
    private Converter // not used by the method but required by signature
    mockConverter;

    private static Stream<Arguments> provideValidStringInputs() {
        return Stream.of(// Empty / null inputs – should return ZERO
        Arguments.of("Null input returns ZERO", null, "0"), Arguments.of("Empty string returns ZERO", "", "0"), // Simple integer strings
        Arguments.of("Positive integer", "123", "123"), Arguments.of("Negative integer", "-456", "-456"), // Large integer beyond Long.MAX_VALUE
        Arguments.of("Very large integer", "123456789012345678901234567890", "123456789012345678901234567890"), // Leading plus sign
        Arguments.of("Positive integer with plus sign", "+42", "42"), // Decimal truncation toward zero
        Arguments.of("Positive decimal truncates", "123.99", "123"), Arguments.of("Negative decimal truncates", "-123.99", "-123"), // Scientific notation (BigDecimal accepts it)
        Arguments.of("Positive scientific notation", "1e3", "1000"), Arguments.of("Negative scientific notation", "-2.5e2", "-250"));
    }

    private static Stream<Arguments> provideInvalidStringInputs() {
        return Stream.of(Arguments.of("Non‑numeric string", "abc"), Arguments.of("Hexadecimal notation not supported", "0xFF"), // Whitespace handling – BigDecimal constructor does not trim spaces
        Arguments.of("Leading whitespace", " 123"), Arguments.of("Trailing whitespace", "123 "));
    }

    private static Stream<Arguments> provideNonStringInputs() {
        return Stream.of(Arguments.of("Integer object", 123), Arguments.of("Generic Object instance", new Object()), Arguments.of("StringBuilder instance", new StringBuilder("123")));
    }

    @Test
    void toBigInteger_ValidDecimalString_ReturnsBigInteger() {
        // given
        String source = "12345678901234567890";
        // when
        BigInteger result = StringConversions.toBigInteger(source, mockConverter);
        // then
        assertEquals(new BigInteger(source), result);
    }

    @Test
    void toBigInteger_EmptyString_ReturnsZero() {
        // given
        String source = "";
        // when
        BigInteger result = StringConversions.toBigInteger(source, mockConverter);
        // then
        assertEquals(BigInteger.ZERO, result);
    }

    @Test
    void toBigInteger_WhitespaceString_ReturnsZero() {
        // given
        String source = "   ";
        // when
        BigInteger result = StringConversions.toBigInteger(source, mockConverter);
        // then
        // StringUtilities.isEmpty treats whitespace as empty (as in the original library),
        // so the method should return ZERO.
        assertEquals(BigInteger.ZERO, result);
    }

    @Test
    void toBigInteger_NonParseableString_ThrowsIllegalArgumentException() {
        // given
        String source = "not-a-number";
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toBigInteger(source, mockConverter));
        assertTrue(ex.getMessage().contains("Value '" + source + "' not parseable as a BigInteger value."));
        assertNotNull(ex.getCause());
        assertTrue(ex.getCause() instanceof NumberFormatException);
    }

    @Test
    void toBigInteger_NullString_ThrowsIllegalArgumentException() {
        // given
        String source = null;
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toBigInteger(source, mockConverter));
        // The message should still contain the literal "null"
        assertTrue(ex.getMessage().contains("Value 'null' not parseable as a BigInteger value."));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideValidStringInputs")
    void toBigInteger_ValidString_ReturnsExpected(String description, Object from, String expectedString) {
        // when
        BigInteger result = StringConversions.toBigInteger(from, mockConverter);
        // then
        assertEquals(new BigInteger(expectedString), result, () -> "Failed for input: " + from);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideInvalidStringInputs")
    void toBigInteger_InvalidString_ThrowsIllegalArgumentException(String description, Object from) {
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toBigInteger(from, mockConverter), () -> "Expected IllegalArgumentException for input: " + from);
        // sanity check that the cause is a NumberFormatException
        assertNotNull(ex.getCause(), "Cause should not be null");
        assertTrue(ex.getCause() instanceof NumberFormatException, "Cause should be NumberFormatException, but was: " + ex.getCause());
        // message must contain the original value representation
        assertTrue(ex.getMessage().contains("Value '" + from + "' not parseable as a BigInteger value."), "Exception message should contain the original value");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideNonStringInputs")
    void toBigInteger_NonStringInput_ThrowsClassCastException(String description, Object from) {
        // when / then
        assertThrows(ClassCastException.class, () -> StringConversions.toBigInteger(from, mockConverter), () -> "Expected ClassCastException for non‑String input: " + from);
    }

    @Test
    void toBigInteger_WhitespaceOnlyString_ReturnsZero() {
        // According to StringUtilities.isEmpty, a string that contains only whitespace is considered empty.
        String source = "   ";
        BigInteger result = StringConversions.toBigInteger(source, mockConverter);
        assertEquals(BigInteger.ZERO, result);
    }
}
