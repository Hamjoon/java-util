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
import java.math.BigInteger;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BigDecimalConversions_toString_Test {

    /**
     * The Converter is not used by {@code BigDecimalConversions.toString}, but the method signature requires it.
     *  A mock is sufficient and keeps the test lightweight.
     */
    @Mock
    private Converter mockConverter;

    private static Stream<Arguments> normalConversionProvider() {
        return Stream.of(// Basic trailing‑zero removal
        Arguments.of("BigDecimal with trailing zeros after decimal point should have those zeros stripped.", new BigDecimal("123.4500"), "123.45", true), // Leading zeros are kept
        Arguments.of("Leading zeros are kept, trailing zeros after the last non‑zero digit are removed.", new BigDecimal("0.0012300"), "0.00123", true), // Negative number with trailing zeros
        Arguments.of("Negative number with trailing zeros should lose the decimal part after stripping.", new BigDecimal("-9876.000"), "-9876", true), // Whole number unchanged
        Arguments.of("Whole number without a fractional part remains unchanged.", new BigDecimal("1000"), "1000", true), // Scientific notation converted to plain string
        Arguments.of("Scientific notation is converted to plain string after stripping trailing zeros.", new BigDecimal("1E+3"), "1000", true), // Zero handling
        Arguments.of("Zero should be represented as \"0\".", BigDecimal.ZERO, "0", true), // Very large integer with trailing zeros
        Arguments.of("Very large BigDecimal with trailing zeros should be handled correctly.", new BigDecimal("12345678901234567890.0000"), "12345678901234567890", true), // Scientific notation with negative exponent
        Arguments.of("Scientific notation with a negative exponent should be converted to a plain decimal string after stripping trailing zeros.", new BigDecimal("1E-3"), "0.001", true), // Scientific notation with positive exponent
        Arguments.of("Scientific notation with a positive exponent should be rendered as a plain integer string after stripping trailing zeros.", new BigDecimal("1E+6"), "1000000", true), // Negative zero variants
        Arguments.of("Negative zero (\"-0.0\") after stripping trailing zeros becomes plain zero.", new BigDecimal("-0.0"), "0", true), Arguments.of("Negative zero with multiple fractional zeros should also collapse to \"0\".", new BigDecimal("-0.0000"), "0", true), // Very small number with many leading zeros
        Arguments.of("A tiny number with many leading zeros and trailing zeros; after stripping, only the significant digits remain.", new BigDecimal("0.0000000000000000000001234000"), "0.0000000000000000000001234", true), // Fractional part ending with zeros
        Arguments.of("Fractional part ending with zeros should be trimmed, leaving the minimal representation.", new BigDecimal("0.1000"), "0.1", true), // Huge integer without fractional part
        Arguments.of("A huge integer without any fractional part should be returned unchanged.", new BigDecimal("123456789012345678901234567890"), "123456789012345678901234567890", true), // Large integer expressed with many trailing fractional zeros
        Arguments.of("Large integer value expressed with many trailing fractional zeros should collapse to a plain integer string.", new BigDecimal("1000.0000000000"), "1000", true), // Converter parameter influence – non‑null converter is ignored
        Arguments.of("A non‑null Converter instance is ignored by the method; the result should be identical to the null‑converter case.", new BigDecimal("123.4500"), "123.45", true), Arguments.of("Even when a Converter is supplied, zero conversion remains unchanged.", BigDecimal.ZERO, "0", true));
    }

    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(// Null input – cast triggers NullPointerException
        Arguments.of("Passing a null `from` argument should result in a NullPointerException when the cast is attempted.", null, NullPointerException.class, true), // Non‑BigDecimal inputs – ClassCastException expected
        Arguments.of("If `from` is not a BigDecimal (e.g., an Integer), a ClassCastException is expected.", Integer.valueOf(42), ClassCastException.class, true), Arguments.of("A String cannot be cast to BigDecimal, causing a ClassCastException.", "not a number", ClassCastException.class, true), // Additional ClassCastException scenarios
        Arguments.of("Passing a Double should cause a ClassCastException because the method expects a BigDecimal.", Double.valueOf(1.23), ClassCastException.class, true), Arguments.of("Passing a Long (primitive boxed) should trigger a ClassCastException.", Long.valueOf(1234567890123L), ClassCastException.class, true), Arguments.of("A BigInteger instance cannot be cast to BigDecimal, resulting in a ClassCastException.", new BigInteger("12345678901234567890"), ClassCastException.class, true), Arguments.of("Passing a Boolean value should cause a ClassCastException.", Boolean.TRUE, ClassCastException.class, true), Arguments.of("Passing a generic Object that is not a BigDecimal should result in a ClassCastException.", new Object(), ClassCastException.class, true));
    }

    @Test
    void toString_stripsTrailingZeros() {
        BigDecimal input = new BigDecimal("123.4500");
        String result = BigDecimalConversions.toString(input, mockConverter);
        assertEquals("123.45", result, "Trailing zeros should be removed");
    }

    @Test
    void toString_preservesWholeNumber() {
        BigDecimal input = new BigDecimal("1000");
        String result = BigDecimalConversions.toString(input, mockConverter);
        assertEquals("1000", result, "Whole numbers should be unchanged");
    }

    @Test
    void toString_zeroValue() {
        BigDecimal input = BigDecimal.ZERO;
        String result = BigDecimalConversions.toString(input, mockConverter);
        assertEquals("0", result, "Zero should be represented as \"0\"");
    }

    @Test
    void toString_negativeNumber() {
        BigDecimal input = new BigDecimal("-45.6000");
        String result = BigDecimalConversions.toString(input, mockConverter);
        assertEquals("-45.6", result, "Negative numbers should retain sign and strip zeros");
    }

    @Test
    void toString_nonBigDecimal_throwsClassCastException() {
        Object notABigDecimal = "123.45";
        assertThrows(ClassCastException.class, () -> BigDecimalConversions.toString(notABigDecimal, mockConverter), "Passing a non‑BigDecimal should throw ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("normalConversionProvider")
    void toString_normalConversions(String description, Object from, String expected, boolean useMock) {
        Converter converter = useMock ? mockConverter : null;
        String result = BigDecimalConversions.toString(from, converter);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("exceptionProvider")
    void toString_exceptionScenarios(String description, Object from, Class<? extends Throwable> expectedException, boolean useMock) {
        Converter converter = useMock ? mockConverter : null;
        assertThrows(expectedException, () -> BigDecimalConversions.toString(from, converter), description);
    }
}
