package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BigDecimalConversions_toBigInteger_Test {

    @Mock
    Converter // Converter is not used by the method but required for the signature
    mockConverter;

    /**
     * Minimal implementation of {@link Converter} used only to verify that a
     * non‑null converter argument does not affect the conversion outcome.
     */
    private static class MockConverter implements Converter {
        // No methods are needed for this test; the class exists solely as a distinct instance.
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Positive BigDecimal with fraction – truncate toward zero
        Arguments.of(new BigDecimal("12345.678"), null, "12345", "Convert a positive BigDecimal with fractional part; should truncate toward zero"), // Negative BigDecimal with fraction – truncate toward zero
        Arguments.of(new BigDecimal("-9876.543"), null, "-9876", "Convert a negative BigDecimal with fractional part; should truncate toward zero"), // Zero
        Arguments.of(BigDecimal.ZERO, null, "0", "Convert zero BigDecimal"), // Large integer without fraction
        Arguments.of(new BigDecimal("12345678901234567890"), null, "12345678901234567890", "Convert a large BigDecimal without fraction"), // Zero with fractional zeros
        Arguments.of(new BigDecimal("0.0000"), null, "0", "Zero with fractional zeros should convert to 0"), // Negative fraction less than 1 – truncates to 0
        Arguments.of(new BigDecimal("-0.999"), null, "0", "Negative fraction less than 1 should truncate toward zero yielding 0"), // Scientific notation positive exponent
        Arguments.of(new BigDecimal("1E+3"), null, "1000", "Scientific notation positive exponent should convert correctly"), // Scientific notation negative exponent
        Arguments.of(new BigDecimal("-1E+3"), null, "-1000", "Scientific notation negative exponent should convert correctly"), // Negative zero
        Arguments.of(new BigDecimal("-0.0"), null, "0", "Negative zero should convert to zero"), // Very large number with fraction
        Arguments.of(new BigDecimal("123456789012345678901234567890.123456789"), null, "123456789012345678901234567890", "Very large BigDecimal with fraction should truncate correctly"), // Constructed from double – potential rounding issues are ignored
        Arguments.of(new BigDecimal(1.999), null, "1", "BigDecimal constructed from double with rounding issues should truncate to integer part"), // Non‑null converter argument (should be ignored)
        Arguments.of(new BigDecimal("42"), new MockConverter(), "42", "Non‑null converter argument should be ignored and conversion succeed"));
    }

    private static Stream<Arguments> invalidConversionProvider() {
        return Stream.of(// Null 'from' argument
        Arguments.of(null, null, NullPointerException.class, "Passing null as 'from' should throw NullPointerException"), // Non‑BigDecimal string
        Arguments.of("not a BigDecimal", null, ClassCastException.class, "Passing a non‑BigDecimal object should throw ClassCastException"), // BigInteger instead of BigDecimal
        Arguments.of(new BigInteger("123"), null, ClassCastException.class, "Passing a BigInteger instead of BigDecimal should throw ClassCastException"), // Integer type
        Arguments.of(Integer.valueOf(123), null, ClassCastException.class, "Passing an Integer should trigger ClassCastException because the method casts to BigDecimal"), // Double type
        Arguments.of(Double.valueOf(45.6), null, ClassCastException.class, "Passing a Double should trigger ClassCastException"), // Generic Object
        Arguments.of(new Object(), null, ClassCastException.class, "Passing a generic Object that is not a BigDecimal should trigger ClassCastException"), // int array
        Arguments.of(new int[] { 1, 2, 3 }, null, ClassCastException.class, "Passing an int array should trigger ClassCastException"), // Boolean
        Arguments.of(Boolean.TRUE, null, ClassCastException.class, "Passing a Boolean should trigger ClassCastException"));
    }

    @Test
    void toBigInteger_returnsCorrectValue_forBigDecimalInput() {
        // given
        BigDecimal bd = new BigDecimal("12345678901234567890.999");
        // when
        BigInteger result = BigDecimalConversions.toBigInteger(bd, mockConverter);
        // then
        assertEquals(new BigInteger("12345678901234567890"), result, "toBigInteger should drop the fractional part and return the integer component");
    }

    @Test
    void toBigInteger_throwsNullPointerException_whenInputIsNull() {
        // given
        Object nullObj = null;
        // when / then
        assertThrows(NullPointerException.class, () -> BigDecimalConversions.toBigInteger(nullObj, mockConverter), "Calling toBigInteger with a null argument should throw NPE");
    }

    @Test
    void toBigInteger_throwsClassCastException_whenInputIsNotBigDecimal() {
        // given
        Object notABigDecimal = "not a bigdecimal";
        // when / then
        assertThrows(ClassCastException.class, () -> BigDecimalConversions.toBigInteger(notABigDecimal, mockConverter), "Calling toBigInteger with a non‑BigDecimal should throw ClassCastException");
    }

    /**
     * Valid conversion scenarios – the method should return a {@link BigInteger}
     * that matches the expected decimal string (fractional part truncated).
     */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("validConversionProvider")
    void toBigInteger_validConversions(Object from, Converter converter, String expectedDecimalString, String description) {
        // when
        BigInteger result = BigDecimalConversions.toBigInteger(from, converter);
        // then
        assertEquals(new BigInteger(expectedDecimalString), result, () -> "Failed case: " + description);
    }

    /**
     * Invalid input scenarios – the method should throw the expected exception type.
     */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("invalidConversionProvider")
    void toBigInteger_invalidInputs(Object from, Converter converter, Class<? extends Throwable> expectedException, String description) {
        // when / then
        assertThrows(expectedException, () -> BigDecimalConversions.toBigInteger(from, converter), () -> "Failed case: " + description);
    }
}
