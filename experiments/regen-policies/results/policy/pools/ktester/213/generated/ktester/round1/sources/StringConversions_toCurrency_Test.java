package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Currency;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class StringConversions_toCurrency_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // The Converter is required as a parameter but its behaviour is not used
        // by the method under test, so a default instance is sufficient.
        converter = new Converter(new ConverterOptions());
    }

    /**
     * Supplies the test data.  The data mirrors the JSON test cases provided
     * in the task description.
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> currencyTestCases() {
        return Stream.of(// Standard valid codes
        arguments("USD", "Standard valid code", "currency", "USD"), arguments("GBP", "Another valid code", "currency", "GBP"), // Whitespace handling (spaces, tabs, newlines)
        arguments("  EUR  ", "Whitespace trimmed (spaces)", "currency", "EUR"), arguments("\tJPY\t", "Whitespace trimmed (tabs)", "currency", "JPY"), arguments("\nCHF\n", "Whitespace trimmed (newlines)", "currency", "CHF"), // Non‑breaking space (U+00A0) – not trimmed, leads to IllegalArgumentException
        arguments("\u00A0CAD\u00A0", "Non‑breaking space not trimmed", "IllegalArgumentException", null), // EM SPACE (U+2003) – not trimmed
        arguments("\u2003USD\u2003", "EM SPACE not trimmed", "IllegalArgumentException", null), // Byte Order Mark (U+FEFF) – not trimmed
        arguments("\uFEFFUSD\uFEFF", "BOM not trimmed", "IllegalArgumentException", null), // Lower‑case code – Currency.getInstance is case‑sensitive, throws
        arguments("usd", "Lower‑case code", "IllegalArgumentException", null), // Valid code with extra character
        arguments("USD1", "Extra character after code", "IllegalArgumentException", null), // Internal whitespace
        arguments("U S D", "Internal spaces not removed", "IllegalArgumentException", null), // Empty string after trimming
        arguments("   ", "String becomes empty after trim", "IllegalArgumentException", null), // Empty string literal
        arguments("", "Empty string literal", "IllegalArgumentException", null), // Unknown three‑letter code
        arguments("AAA", "Unknown three‑letter code", "IllegalArgumentException", null), // Too short
        arguments("US", "Code length less than 3", "IllegalArgumentException", null), // Too long
        arguments("EURO", "Code length greater than 3", "IllegalArgumentException", null), // Non‑alphabetic character
        arguments("U$D", "Code contains non‑alphabetic character", "IllegalArgumentException", null), // Null input
        arguments(null, "Null input object", "NullPointerException", null), // Non‑String object (Integer)
        arguments(12345, "Non‑String numeric input", "ClassCastException", null), // Non‑String object (StringBuilder)
        arguments(new StringBuilder("USD"), "Non‑String StringBuilder input", "ClassCastException", null));
    }

    @Test
    void toCurrency_validCode_returnsCurrency() {
        // GIVEN
        String input = "USD";
        // WHEN
        Currency result = StringConversions.toCurrency(input, converter);
        // THEN
        assertNotNull(result);
        assertEquals("USD", result.getCurrencyCode());
    }

    @Test
    void toCurrency_codeWithWhitespace_isTrimmed() {
        // GIVEN
        // lower‑case with surrounding spaces
        String input = "  eur  ";
        // WHEN
        Currency result = StringConversions.toCurrency(input, converter);
        // THEN
        // Currency#getInstance is case‑insensitive for the code, but the
        // method under test trims whitespace, so we expect a valid Currency.
        assertNotNull(result);
        assertEquals("EUR", result.getCurrencyCode());
    }

    @Test
    void toCurrency_unknownCode_throwsIllegalArgumentException() {
        // GIVEN
        String input = "XYZ";
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toCurrency(input, converter));
    }

    @Test
    void toCurrency_nullInput_throwsNullPointerException() {
        // GIVEN
        Object input = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> StringConversions.toCurrency(input, converter));
    }

    /**
     * Parameterized test that covers all scenarios described in the JSON test
     * case list.  Each {@code Arguments} entry contains:
     *
     * <ul>
     *   <li>the input object for the {@code from} parameter,</li>
     *   <li>a description (used only for readability in the test report),</li>
     *   <li>the expected outcome type:
     *       <ul>
     *         <li>"currency" – the method should return a {@link Currency} whose code matches the expected value,</li>
     *         <li>"IllegalArgumentException", "NullPointerException", or "ClassCastException" – the method should throw the specified exception.</li>
     *       </ul>
     *   </li>
     *   <li>the expected currency code when the outcome is a Currency.</li>
     * </ul>
     */
    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("currencyTestCases")
    void toCurrency_variousInputs(Object input, String description, String expectedOutcome, String expectedCode) {
        switch(expectedOutcome) {
            case "currency":
                Currency result = StringConversions.toCurrency(input, converter);
                assertNotNull(result, "Resulting Currency should not be null");
                assertEquals(expectedCode, result.getCurrencyCode(), () -> "Expected currency code '" + expectedCode + "' but got '" + result.getCurrencyCode() + "'");
                break;
            case "IllegalArgumentException":
                assertThrows(IllegalArgumentException.class, () -> StringConversions.toCurrency(input, converter), () -> "Expected IllegalArgumentException for input: " + input);
                break;
            case "NullPointerException":
                assertThrows(NullPointerException.class, () -> StringConversions.toCurrency(input, converter), () -> "Expected NullPointerException for input: null");
                break;
            case "ClassCastException":
                assertThrows(ClassCastException.class, () -> StringConversions.toCurrency(input, converter), () -> "Expected ClassCastException for non‑String input: " + input);
                break;
            default:
                fail("Unrecognised expected outcome: " + expectedOutcome);
        }
    }
}
