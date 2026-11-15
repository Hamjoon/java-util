package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class StringConversions_toDouble_Test {

    /**
     * A mock {@link Converter} – the method under test does not call any of its methods.
     */
    @Mock
    private Converter mockConverter;

    static Stream<Object> emptyAndNullProvider() {
        return Stream.of(// null is treated as empty
        null, // empty string
        "");
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> validNumericProvider() {
        return Stream.of(org.junit.jupiter.params.provider.Arguments.of("123.45", 123.45), org.junit.jupiter.params.provider.Arguments.of("-987.65", -987.65), org.junit.jupiter.params.provider.Arguments.of("1e6", 1_000_000.0), org.junit.jupiter.params.provider.Arguments.of("1.7976931348623157E308", // Double.MAX_VALUE literal
        Double.MAX_VALUE));
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> specialValuesProvider() {
        return Stream.of(org.junit.jupiter.params.provider.Arguments.of("NaN", "NaN"), org.junit.jupiter.params.provider.Arguments.of("Infinity", "Infinity"), org.junit.jupiter.params.provider.Arguments.of("-Infinity", "-Infinity"), // Overflow – Double.valueOf parses to +Infinity
        org.junit.jupiter.params.provider.Arguments.of("1e309", "Infinity"));
    }

    static Stream<String> invalidNumericProvider() {
        return Stream.of(// non‑numeric characters
        "abc123", // malformed number
        "12.3.4", // leading/trailing whitespace (Double.valueOf does not trim)
        "  42.0  ", // whitespace only – not considered empty by StringUtilities.isEmpty
        "   ");
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> nonStringProvider() {
        return Stream.of(org.junit.jupiter.params.provider.Arguments.of(12345, Integer.class), org.junit.jupiter.params.provider.Arguments.of(Boolean.TRUE, Boolean.class));
    }

    @Test
    void toDouble_validNumberString_returnsParsedDouble() {
        // GIVEN
        String source = "1234.56";
        // WHEN
        Double result = StringConversions.toDouble(source, mockConverter);
        // THEN
        assertNotNull(result, "Result should not be null for a valid numeric string");
        assertEquals(1234.56, result, 0.000001, "Parsed double value is incorrect");
    }

    @Test
    void toDouble_emptyString_returnsZero() {
        // GIVEN
        String source = "";
        // WHEN
        Double result = StringConversions.toDouble(source, mockConverter);
        // THEN
        assertEquals(0.0, result, "Empty string should be interpreted as 0.0");
    }

    @Test
    void toDouble_nullString_returnsZero() {
        // GIVEN
        // casting null to String is allowed; StringUtilities.isEmpty treats it as empty.
        String source = null;
        // WHEN
        Double result = StringConversions.toDouble(source, mockConverter);
        // THEN
        assertEquals(0.0, result, "Null source should be interpreted as 0.0");
    }

    @Test
    void toDouble_invalidNumberString_throwsIllegalArgumentException() {
        // GIVEN
        String source = "not-a-number";
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toDouble(source, mockConverter), "Invalid numeric string should cause IllegalArgumentException");
        // Verify that the exception message contains the original value for easier debugging.
        assertTrue(ex.getMessage().contains(source), "Exception message should contain the offending value");
    }

    @Test
    void toDouble_doesNotInteractWithConverter() {
        // GIVEN
        String source = "42.0";
        // WHEN
        Double result = StringConversions.toDouble(source, mockConverter);
        // THEN
        assertEquals(42.0, result, 0.000001);
        // Verify that no method on the mock converter was called.
        // This ensures the implementation truly ignores the Converter argument.
        verifyNoInteractions(mockConverter);
    }

    /* -------------------------------------------------------------
     *  Group 1 – Empty and null handling
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("emptyAndNullProvider")
    void toDouble_emptyOrNull_returnsZero(Object from) {
        // WHEN
        Double result = StringConversions.toDouble(from, mockConverter);
        // THEN
        assertEquals(0.0, result, "Empty or null input should be interpreted as 0.0");
        verifyNoInteractions(mockConverter);
    }

    /* -------------------------------------------------------------
     *  Group 2 – Valid numeric strings
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from=\"{0}\" → expected={1}")
    @MethodSource("validNumericProvider")
    void toDouble_validNumericStrings(String from, Double expected) {
        // WHEN
        Double result = StringConversions.toDouble(from, mockConverter);
        // THEN
        assertNotNull(result, "Result must not be null for a valid numeric string");
        assertEquals(expected, result, 0.0000001, () -> "Parsed value for \"" + from + "\" is incorrect");
        verifyNoInteractions(mockConverter);
    }

    /* -------------------------------------------------------------
     *  Group 3 – Special double values (NaN, infinities, overflow)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from=\"{0}\" → special expected={1}")
    @MethodSource("specialValuesProvider")
    void toDouble_specialDoubleValues(String from, String expectedKeyword) {
        // WHEN
        Double result = StringConversions.toDouble(from, mockConverter);
        // THEN – evaluate based on the expected keyword
        switch(expectedKeyword) {
            case "NaN":
                assertTrue(Double.isNaN(result), "Expected NaN for input \"" + from + "\"");
                break;
            case "Infinity":
                assertEquals(Double.POSITIVE_INFINITY, result, "Expected positive infinity for input \"" + from + "\"");
                break;
            case "-Infinity":
                assertEquals(Double.NEGATIVE_INFINITY, result, "Expected negative infinity for input \"" + from + "\"");
                break;
            default:
                fail("Unsupported expected keyword: " + expectedKeyword);
        }
        verifyNoInteractions(mockConverter);
    }

    /* -------------------------------------------------------------
     *  Group 4 – Invalid numeric strings (should throw IllegalArgumentException)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from=\"{0}\" → throws IllegalArgumentException")
    @MethodSource("invalidNumericProvider")
    void toDouble_invalidNumericStrings(String from) {
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toDouble(from, mockConverter), "Invalid numeric string should cause IllegalArgumentException");
        assertTrue(ex.getMessage().contains(from), "Exception message must contain the offending value");
        verifyNoInteractions(mockConverter);
    }

    /* -------------------------------------------------------------
     *  Group 5 – Non‑String inputs (should throw ClassCastException)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0} (type={1}) → throws ClassCastException")
    @MethodSource("nonStringProvider")
    void toDouble_nonStringInputs(Object from, Class<?> type) {
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> StringConversions.toDouble(from, mockConverter), "Passing a non‑String (" + type.getSimpleName() + ") must throw ClassCastException");
        verifyNoInteractions(mockConverter);
    }
}
