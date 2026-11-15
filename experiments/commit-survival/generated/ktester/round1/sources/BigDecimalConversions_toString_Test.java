package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigInteger;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class BigDecimalConversions_toString_Test {

    /**
     * The Converter is not used by {@code BigDecimalConversions.toString}, but the method signature requires it.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Provides the successful test cases extracted from the JSON document.
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> successCasesProvider() {
        // Re‑use the mock Converter for the majority of cases (null is also acceptable).
        // explicit null where required
        Converter dummyConverter = null;
        // A Converter that would throw if ever used – demonstrates that the method truly ignores it.
        Converter throwingConverter = new Converter() {

            @Override
            public String toString() {
                throw new RuntimeException("Should not be called");
            }
        };
        return Stream.of(// Basic functionality
        arguments(new BigDecimal("12345"), dummyConverter, "12345", "Simple integer BigDecimal – no trailing zeros, should return the exact plain string."), arguments(new BigDecimal("123.45000"), dummyConverter, "123.45", "BigDecimal with trailing zeros – stripTrailingZeros() removes them, plain string should not contain the zeros."), arguments(new BigDecimal("0.0000"), dummyConverter, "0", "Zero value with scale – after stripping trailing zeros the result is plain \"0\"."), arguments(new BigDecimal("-9876.54321000"), dummyConverter, "-9876.54321", "Negative number with trailing zeros – ensure sign is preserved and zeros are stripped."), arguments(new BigDecimal("1E+9"), dummyConverter, "1000000000", "Scientific notation input – toPlainString() should produce the full decimal representation without exponent."), arguments(new BigDecimal("0.000001"), dummyConverter, "0.000001", "Small fractional value – no trailing zeros to strip, plain string should keep leading zeros after decimal point."), arguments(new BigDecimal("12345678901234567890.12345678901234567890"), dummyConverter, "12345678901234567890.12345678901234567890", "Very large precision BigDecimal – verify that the method handles arbitrary precision without loss."), // Non‑null Converter (method ignores it)
        arguments(new BigDecimal("10.00"), new Converter() {
        }, "10", "Pass a non‑null Converter instance (even though the method ignores it) to confirm it does not affect the result."), // Additional edge cases
        arguments(new BigDecimal("1.2300E+3"), dummyConverter, "1230", "Positive exponent with trailing zeros after decimal – stripTrailingZeros() removes the fractional zeros and toPlainString() expands the exponent."), arguments(new BigDecimal("1.23E-3"), dummyConverter, "0.00123", "Negative exponent – verifies that the method correctly converts scientific notation with a negative exponent to a plain decimal string."), arguments(BigDecimal.valueOf(0L), dummyConverter, "0", "Zero created via valueOf(long) – ensures that different factory methods producing zero behave identically."), arguments(BigDecimal.valueOf(10.0), dummyConverter, "10", "BigDecimal from double with trailing .0 – confirms that the trailing zero is stripped and the plain string is an integer."), arguments(new BigDecimal(BigInteger.valueOf(12345), -2), dummyConverter, "1234500", "BigDecimal with negative scale – tests handling of numbers where the scale is negative, resulting in additional trailing zeros."), arguments(new BigDecimal("+123.45"), dummyConverter, "123.45", "BigDecimal with an explicit plus sign – verifies that the sign is normalized and does not affect the output."), arguments(new BigDecimal("10.00"), throwingConverter, "10", "Non‑null Converter that would throw if used – demonstrates that the method truly ignores the Converter argument."));
    }

    /**
     * Provides the exceptional test cases extracted from the JSON document.
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> exceptionCasesProvider() {
        // Null converter is acceptable; the exception originates from the 'from' argument.
        return Stream.of(arguments(null, null, NullPointerException.class, "Null input for 'from' – casting to BigDecimal should throw NullPointerException."), arguments("not a bigdecimal", null, ClassCastException.class, "Invalid type for 'from' – casting to BigDecimal should throw ClassCastException."));
    }

    @Test
    void toString_stripsTrailingZeros() {
        BigDecimal input = new BigDecimal("123.4500");
        String result = BigDecimalConversions.toString(input, mockConverter);
        assertEquals("123.45", result, "Trailing zeros should be removed and plain string returned");
    }

    @Test
    void toString_handlesLargeScaleWithoutScientificNotation() {
        // 1E+3 is a BigDecimal with scale -3; stripTrailingZeros().toPlainString() yields "1000"
        BigDecimal input = new BigDecimal("1E+3");
        String result = BigDecimalConversions.toString(input, mockConverter);
        assertEquals("1000", result, "Scientific notation should be converted to plain string");
    }

    @Test
    void toString_zeroValue() {
        BigDecimal input = new BigDecimal("0.0000");
        String result = BigDecimalConversions.toString(input, mockConverter);
        assertEquals("0", result, "Zero should be represented as \"0\"");
    }

    @Test
    void toString_nullInputThrowsNPE() {
        assertThrows(NullPointerException.class, () -> BigDecimalConversions.toString(null, mockConverter), "Passing null should result in NullPointerException");
    }

    /**
     * Covers all cases where a {@link BigDecimal} (or a value that can be
     * constructed as a {@link BigDecimal}) is supplied and a plain string
     * representation is expected.
     */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("successCasesProvider")
    void toString_successful(Object from, Converter converter, String expected, String description) {
        // When converter is null we simply pass null; otherwise we use the supplied instance.
        String result = BigDecimalConversions.toString(from, converter);
        assertEquals(expected, result, description);
    }

    /**
     * Verifies that the method throws the expected exception for invalid inputs.
     */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("exceptionCasesProvider")
    void toString_exceptions(Object from, Converter converter, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> BigDecimalConversions.toString(from, converter), description);
    }
}
