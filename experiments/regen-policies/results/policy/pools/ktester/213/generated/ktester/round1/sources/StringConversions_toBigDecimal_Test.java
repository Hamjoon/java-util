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
    converter;

    /**
     * Supplies the test data extracted from the JSON specification.
     */
    private static Stream<Arguments> provideToBigDecimalCases() {
        return Stream.of(// Empty string → zero
        Arguments.of("", BigDecimal.ZERO, null, "Empty string should be treated as zero and return BigDecimal.ZERO."), // Simple integer string
        Arguments.of("123", new BigDecimal("123"), null, "A simple integer string is parsed correctly into a BigDecimal with the same value."), // Signed decimal
        Arguments.of("-45.67", new BigDecimal("-45.67"), null, "A signed decimal string is parsed correctly, preserving sign and scale."), // Scientific notation (positive exponent)
        Arguments.of("1.23e4", new BigDecimal("12300"), null, "Scientific notation is accepted and converted to the equivalent plain BigDecimal value."), // Invalid numeric string
        Arguments.of("abc", null, IllegalArgumentException.class, "A non‑numeric string causes NumberFormatException which is wrapped in IllegalArgumentException."), // Non‑String object → ClassCastException
        Arguments.of(5, null, ClassCastException.class, "Passing a non‑String object results in a ClassCastException during the cast to String."), // Null input → zero (StringUtilities.isEmpty treats null as empty)
        Arguments.of(null, BigDecimal.ZERO, null, "Null input is treated as empty by StringUtilities.isEmpty and returns BigDecimal.ZERO."), // Whitespace‑only string → IllegalArgumentException
        Arguments.of("   ", null, IllegalArgumentException.class, "A string containing only whitespace is not parseable by BigDecimal and results in an IllegalArgumentException."), // Leading plus sign
        Arguments.of("+123.45", new BigDecimal("123.45"), null, "A leading plus sign is accepted and the value is parsed correctly."), // Leading zeros with trailing zeros (scale preserved)
        Arguments.of("0000123.4500", new BigDecimal("123.4500"), null, "Leading zeros are ignored for the numeric value but the scale (trailing zeros) is preserved."), // Scientific notation with negative exponent
        Arguments.of("1.23e-2", new BigDecimal("0.0123"), null, "Scientific notation with a negative exponent is correctly converted to a plain decimal representation."), // Scientific notation with explicit plus exponent
        Arguments.of("1.23e+2", new BigDecimal("123"), null, "Scientific notation with an explicit plus exponent is parsed to the equivalent plain value."), // Very large number beyond primitive ranges
        Arguments.of("123456789012345678901234567890.1234567890", new BigDecimal("123456789012345678901234567890.1234567890"), null, "A very large number beyond primitive ranges is still parsed correctly by BigDecimal."), // Leading/trailing spaces cause NumberFormatException → IllegalArgumentException
        Arguments.of(" 123 ", null, IllegalArgumentException.class, "Leading/trailing spaces cause BigDecimal constructor to throw NumberFormatException, which is wrapped."), // Control characters (tab, newline) → IllegalArgumentException
        Arguments.of("\t\n", null, IllegalArgumentException.class, "Control characters (tab, newline) are not valid numeric representations and result in an IllegalArgumentException."), // The literal "NaN" is not a valid BigDecimal literal
        Arguments.of("NaN", null, IllegalArgumentException.class, "The string \"NaN\" is not a valid BigDecimal literal and triggers an IllegalArgumentException."));
    }

    @Test
    void toBigDecimal_validNumericString_returnsCorrectBigDecimal() {
        Object input = "12345.6789";
        BigDecimal expected = new BigDecimal("12345.6789");
        BigDecimal result = StringConversions.toBigDecimal(input, converter);
        assertEquals(expected, result);
    }

    @Test
    void toBigDecimal_emptyString_returnsZero() {
        Object input = "";
        BigDecimal result = StringConversions.toBigDecimal(input, converter);
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void toBigDecimal_nullInput_returnsZero() {
        Object input = null;
        BigDecimal result = StringConversions.toBigDecimal(input, converter);
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void toBigDecimal_invalidString_throwsIllegalArgumentException() {
        Object input = "not-a-number";
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toBigDecimal(input, converter));
        assertTrue(ex.getMessage().contains("not parseable"));
        assertTrue(ex.getCause() instanceof NumberFormatException);
    }

    /**
     * Parameterised test that drives all scenarios described in the JSON test matrix.
     *
     * @param from            the input value passed to {@code toBigDecimal}
     * @param expectedValue   the expected {@link BigDecimal} result (null when an exception is expected)
     * @param expectedException the expected exception type (null when a normal result is expected)
     * @param description     a short description of the test case (used as the display name)
     */
    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("provideToBigDecimalCases")
    void toBigDecimal_parameterised(Object from, BigDecimal expectedValue, Class<? extends Throwable> expectedException, String description) {
        if (expectedException != null) {
            Throwable thrown = assertThrows(expectedException, () -> StringConversions.toBigDecimal(from, converter), description);
            // For IllegalArgumentException we also verify that the cause is a NumberFormatException
            if (expectedException == IllegalArgumentException.class) {
                assertTrue(thrown.getCause() instanceof NumberFormatException, "Cause should be NumberFormatException");
            }
        } else {
            BigDecimal result = StringConversions.toBigDecimal(from, converter);
            assertEquals(expectedValue, result, description);
        }
    }
}
