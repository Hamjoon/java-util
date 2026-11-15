package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BigDecimalConversions_toBigInteger_Test {

    // We provide a mock to satisfy the parameter contract.
    @Mock
    private Converter mockConverter;

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Positive whole number
        Arguments.of("Converts a positive whole-number BigDecimal to the same value as a BigInteger.", new BigDecimal("12345"), null, "12345"), // Negative whole number
        Arguments.of("Converts a negative whole-number BigDecimal to the corresponding negative BigInteger.", new BigDecimal("-9876"), null, "-9876"), // Zero
        Arguments.of("Zero BigDecimal should produce a zero BigInteger.", BigDecimal.ZERO, null, "0"), // Positive with fractional part – truncates toward zero
        Arguments.of("Fractional part is discarded (truncation toward zero) when converting to BigInteger.", new BigDecimal("123.999"), null, "123"), // Negative with fractional part – truncates toward zero
        Arguments.of("Negative fractional BigDecimal truncates toward zero, yielding -123.", new BigDecimal("-123.456"), null, "-123"), // Very large number
        Arguments.of("Very large BigDecimal is correctly converted to a BigInteger of the same magnitude.", new BigDecimal("123456789012345678901234567890"), null, "123456789012345678901234567890"), // Non‑null converter should be ignored
        Arguments.of("Method ignores the Converter argument; conversion works with a non‑null converter.", new BigDecimal("10"), // The mock will be injected by JUnit at runtime; we use a placeholder here.
        // The actual mock instance is supplied by the test method parameter.
        // We pass null here and replace it later in the test method.
        null, "10"));
    }

    private static Stream<Arguments> nullInputProvider() {
        return Stream.of(Arguments.of("Passing a null `from` argument should cause a NullPointerException when the method attempts to invoke `toBigInteger()` on the casted value.", null, null), Arguments.of("Even when a non‑null Converter is supplied, a null `from` still results in a NullPointerException.", null, /* placeholder for mock – will be replaced in test */
        null));
    }

    private static Stream<Arguments> invalidTypeProvider() {
        return Stream.of(Arguments.of("Passing an Integer instead of a BigDecimal should cause a ClassCastException during the cast.", Integer.valueOf(42), null), Arguments.of("Passing a String representation of a number should still result in a ClassCastException because the method only accepts BigDecimal.", "12345", null), Arguments.of("Passing a generic Object that is not a BigDecimal triggers a ClassCastException.", new Object(), null));
    }

    private static Stream<Arguments> validConversionProviderWithMock() {
        return Stream.of(Arguments.of("Converts a positive whole-number BigDecimal to the same value as a BigInteger.", new BigDecimal("12345"), null, "12345"), Arguments.of("Converts a negative whole-number BigDecimal to the corresponding negative BigInteger.", new BigDecimal("-9876"), null, "-9876"), Arguments.of("Zero BigDecimal should produce a zero BigInteger.", BigDecimal.ZERO, null, "0"), Arguments.of("Fractional part is discarded (truncation toward zero) when converting to BigInteger.", new BigDecimal("123.999"), null, "123"), Arguments.of("Negative fractional BigDecimal truncates toward zero, yielding -123.", new BigDecimal("-123.456"), null, "-123"), Arguments.of("Very large BigDecimal is correctly converted to a BigInteger of the same magnitude.", new BigDecimal("123456789012345678901234567890"), null, "123456789012345678901234567890"), // The placeholder `null` will be swapped for `mockConverter` inside the test method.
        Arguments.of("Method ignores the Converter argument; conversion works with a non‑null converter.", new BigDecimal("10"), null, "10"));
    }

    @Test
    void toBigInteger_convertsPositiveDecimalTruncatingFraction() {
        BigDecimal input = new BigDecimal("12345.678");
        BigInteger expected = new BigInteger("12345");
        BigInteger actual = BigDecimalConversions.toBigInteger(input, mockConverter);
        assertEquals(expected, actual);
    }

    @Test
    void toBigInteger_convertsNegativeDecimalTruncatingFraction() {
        BigDecimal input = new BigDecimal("-987.654");
        BigInteger expected = new BigInteger("-987");
        BigInteger actual = BigDecimalConversions.toBigInteger(input, mockConverter);
        assertEquals(expected, actual);
    }

    @Test
    void toBigInteger_convertsWholeNumberWithoutFraction() {
        BigDecimal input = new BigDecimal("1000");
        BigInteger expected = new BigInteger("1000");
        BigInteger actual = BigDecimalConversions.toBigInteger(input, mockConverter);
        assertEquals(expected, actual);
    }

    @Test
    void toBigInteger_convertsVeryLargeNumber() {
        BigDecimal input = new BigDecimal("123456789012345678901234567890");
        BigInteger expected = new BigInteger("123456789012345678901234567890");
        BigInteger actual = BigDecimalConversions.toBigInteger(input, mockConverter);
        assertEquals(expected, actual);
    }

    @Test
    void toBigInteger_nullInput_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> BigDecimalConversions.toBigInteger(null, mockConverter));
    }

    @Test
    void toBigInteger_invalidType_throwsClassCastException() {
        String notABigDecimal = "not a BigDecimal";
        assertThrows(ClassCastException.class, () -> BigDecimalConversions.toBigInteger(notABigDecimal, mockConverter));
    }

    /* --------------------------------------------------------------
     *  Valid conversion tests – parameterized
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversionProvider")
    void toBigInteger_validConversions(String description, Object from, Converter converter, String expectedStr) {
        // Arrange
        BigInteger expected = new BigInteger(expectedStr);
        // Act
        BigInteger actual = BigDecimalConversions.toBigInteger(from, converter);
        // Assert
        assertEquals(expected, actual, "Conversion result does not match expected value.");
        // Verify that the converter (if supplied) is never used.
        if (converter != null) {
            verifyNoInteractions(converter);
        }
    }

    /* --------------------------------------------------------------
     *  Null‑input tests – parameterized
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullInputProvider")
    void toBigInteger_nullInput(String description, Object from, Converter converter) {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> BigDecimalConversions.toBigInteger(from, converter), "Expected NullPointerException for null 'from' argument.");
    }

    /* --------------------------------------------------------------
     *  Invalid‑type tests – parameterized
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidTypeProvider")
    void toBigInteger_invalidType(String description, Object from, Converter converter) {
        // Act & Assert
        assertThrows(ClassCastException.class, () -> BigDecimalConversions.toBigInteger(from, converter), "Expected ClassCastException for non‑BigDecimal input.");
    }

    // Adjusted test method to handle the placeholder logic for the mockConverter.
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversionProviderWithMock")
    void toBigInteger_validConversionsWithMock(String description, Object from, Converter converter, String expectedStr) {
        // If the description mentions a non‑null converter, replace the placeholder with the mock.
        if (description.contains("non‑null converter")) {
            converter = mockConverter;
        }
        // Arrange
        BigInteger expected = new BigInteger(expectedStr);
        // Act
        BigInteger actual = BigDecimalConversions.toBigInteger(from, converter);
        // Assert
        assertEquals(expected, actual, "Conversion result does not match expected value.");
        verifyNoInteractions(mockConverter);
    }
}
