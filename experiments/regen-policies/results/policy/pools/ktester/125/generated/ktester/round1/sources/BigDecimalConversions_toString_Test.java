package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BigDecimalConversions_toString_Test {

    @Mock
    private Converter // Converter is not used by the method, a mock is sufficient
    mockConverter;

    @AfterEach
    void teardownAfterEach() {
        // Reset mock interactions to keep each test isolated.
        clearInvocations(mockConverter);
    }

    private static Stream<Arguments> provideSuccessfulCases() {
        return Stream.of(// Simple integer
        Arguments.of(new BigDecimal("123"), null, "123"), // Trailing zeros are stripped
        Arguments.of(new BigDecimal("123.4500"), null, "123.45"), // Scientific notation (positive exponent)
        Arguments.of(new BigDecimal("1E+10"), null, "10000000000"), // Zero (plain)
        Arguments.of(BigDecimal.ZERO, null, "0"), // Negative number with trailing zeros
        Arguments.of(new BigDecimal("-0.0012300"), null, "-0.00123"), // Scientific notation (negative exponent)
        Arguments.of(new BigDecimal("1E-10"), null, "0.0000000001"), // Zero with non‑zero scale
        Arguments.of(new BigDecimal("0.0000"), null, "0"), // Scientific notation with trailing zeros (positive exponent)
        Arguments.of(new BigDecimal("1.2300E+3"), null, "1230"), // Scientific notation with trailing zeros (negative exponent)
        Arguments.of(new BigDecimal("1.2300E-3"), null, "0.00123"), // BigDecimal created from a double – exact binary‑decimal representation
        Arguments.of(new BigDecimal(0.1), null, "0.1000000000000000055511151231257827021181583404541015625"), // Explicit non‑null Converter instance (method ignores it)
        Arguments.of(BigDecimal.valueOf(0L), new Converter() {
        }, "0"), // Negative zero becomes plain zero
        Arguments.of(new BigDecimal("-0"), null, "0"), // Very large integer beyond long range
        Arguments.of(new BigDecimal("123456789012345678901234567890"), null, "123456789012345678901234567890"));
    }

    private static Stream<Arguments> provideExceptionCases() {
        return Stream.of(// Null source object – NPE from stripTrailingZeros()
        Arguments.of(null, null, NullPointerException.class), // Wrong type – ClassCastException from explicit cast
        Arguments.of("not a bigdecimal", null, ClassCastException.class));
    }

    @Test
    void toStringStripsTrailingZeros() {
        BigDecimal input = new BigDecimal("123.4500");
        String result = BigDecimalConversions.toString(input, mockConverter);
        assertEquals("123.45", result, "Trailing zeros should be removed");
    }

    @Test
    void toStringHandlesZero() {
        BigDecimal input = new BigDecimal("0.0000");
        String result = BigDecimalConversions.toString(input, mockConverter);
        assertEquals("0", result, "Zero should be represented as \"0\"");
    }

    @Test
    void toStringHandlesNegativeNumber() {
        BigDecimal input = new BigDecimal("-987.6500");
        String result = BigDecimalConversions.toString(input, mockConverter);
        assertEquals("-987.65", result, "Negative numbers should keep the sign and strip trailing zeros");
    }

    @Test
    void toStringProducesPlainStringNotScientificNotation() {
        // BigDecimal created from scientific notation; toPlainString should give a plain decimal representation
        // equals 1000000
        BigDecimal input = new BigDecimal("1E+6");
        String result = BigDecimalConversions.toString(input, mockConverter);
        assertEquals("1000000", result, "Result should be a plain string without scientific notation");
    }

    @Test
    void toStringLargeScaleNumber() {
        BigDecimal input = new BigDecimal("0.0000012300");
        String result = BigDecimalConversions.toString(input, mockConverter);
        assertEquals("0.00000123", result, "Large scale numbers should retain necessary leading zeros and strip trailing zeros");
    }

    @Test
    void toStringThrowsClassCastExceptionWhenInputIsNotBigDecimal() {
        Object notABigDecimal = "123.45";
        assertThrows(ClassCastException.class, () -> BigDecimalConversions.toString(notABigDecimal, mockConverter), "Method should throw ClassCastException when the source object is not a BigDecimal");
    }

    @ParameterizedTest(name = "{index}: toString({0}) => \"{2}\"")
    @MethodSource("provideSuccessfulCases")
    void toStringReturnsExpectedString(Object from, Converter converter, String expected) {
        // Act
        String result = BigDecimalConversions.toString(from, converter);
        // Assert
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "{index}: toString({0}) throws {2}")
    @MethodSource("provideExceptionCases")
    void toStringThrowsExpectedException(Object from, Converter converter, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> BigDecimalConversions.toString(from, converter));
    }
}
