package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toDouble_Test {

    @Mock
    private Converter // Converter is not used by the method, but required for the signature
    mockConverter;

    // ---------- valid conversions ----------
    private static Stream<Arguments> validDoubleProvider() {
        return Stream.of(Arguments.of("123.45", 123.45), Arguments.of("-0.001", -0.001), Arguments.of("0", 0.0), Arguments.of("1e3", 1000.0), Arguments.of("NaN", Double.NaN), Arguments.of("Infinity", Double.POSITIVE_INFINITY), Arguments.of("-Infinity", Double.NEGATIVE_INFINITY), // overflow
        Arguments.of("1e309", Double.POSITIVE_INFINITY), // overflow negative
        Arguments.of("-1e309", Double.NEGATIVE_INFINITY), // smallest normal double
        Arguments.of("2.2250738585072014e-308", 2.2250738585072014E-308));
    }

    // ---------- inputs that should result in IllegalArgumentException ----------
    private static Stream<Arguments> illegalArgumentProvider() {
        return Stream.of(Arguments.of("abc"), Arguments.of("1.2.3"), // leading whitespace
        Arguments.of(" 1.0"), // trailing whitespace
        Arguments.of("1.0 "), // literal "null"
        Arguments.of("null"), // underscore not allowed
        Arguments.of("1_000"), // null character
        Arguments.of("\u0000"));
    }

    // ---------- inputs that should result in ClassCastException ----------
    private static Stream<Arguments> classCastProvider() {
        return Stream.of(// Integer, not a String
        Arguments.of(123), // generic Object
        Arguments.of(new Object()));
    }

    // ---------- empty / null handling ----------
    private static Stream<Arguments> emptyOrNullProvider() {
        return Stream.of(// empty string
        Arguments.of("", 0.0), // null reference – cast yields null, StringUtilities treats as empty
        Arguments.of(null, 0.0));
    }

    @Test
    void toDouble_EmptyString_ReturnsZero() {
        // given
        String empty = "";
        // when
        Double result = StringConversions.toDouble(empty, mockConverter);
        // then
        assertEquals(0.0, result);
    }

    @Test
    void toDouble_Null_ReturnsZero() {
        // given
        Object nullObj = null;
        // when
        Double result = StringConversions.toDouble(nullObj, mockConverter);
        // then
        assertEquals(0.0, result);
    }

    @Test
    void toDouble_ValidNumberString_ReturnsParsedValue() {
        // given
        String number = "1234.56";
        // when
        Double result = StringConversions.toDouble(number, mockConverter);
        // then
        assertEquals(1234.56, result);
    }

    @Test
    void toDouble_InvalidNumberString_ThrowsIllegalArgumentException() {
        // given
        String bad = "not-a-number";
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toDouble(bad, mockConverter));
        assertTrue(ex.getMessage().contains("Value '" + bad + "' not parseable as a double value"));
        assertNotNull(ex.getCause());
        assertTrue(ex.getCause() instanceof NumberFormatException);
    }

    @ParameterizedTest(name = "[{index}] toDouble(''{0}'') => {1}")
    @MethodSource("validDoubleProvider")
    void toDouble_ValidValues_ReturnsExpected(String input, Double expected) {
        // when
        Double result = StringConversions.toDouble(input, mockConverter);
        // then
        if (expected.isNaN()) {
            assertTrue(result.isNaN(), "Result should be NaN");
        } else if (expected.isInfinite()) {
            assertEquals(expected, result, "Result should be infinite with correct sign");
        } else {
            assertEquals(expected, result, 1e-12, "Parsed double does not match expected value");
        }
    }

    @ParameterizedTest(name = "[{index}] toDouble(''{0}'') => IllegalArgumentException")
    @MethodSource("illegalArgumentProvider")
    void toDouble_InvalidFormats_ThrowsIllegalArgumentException(String input) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toDouble(input, mockConverter));
        // verify that the cause is a NumberFormatException (as per implementation)
        assertNotNull(ex.getCause());
        assertTrue(ex.getCause() instanceof NumberFormatException, "Cause should be NumberFormatException");
        assertTrue(ex.getMessage().contains("Value '" + input + "' not parseable as a double value"));
    }

    @ParameterizedTest(name = "[{index}] toDouble(non‑String {0}) => ClassCastException")
    @MethodSource("classCastProvider")
    void toDouble_NonStringInput_ThrowsClassCastException(Object input) {
        assertThrows(ClassCastException.class, () -> StringConversions.toDouble(input, mockConverter));
    }

    @ParameterizedTest(name = "[{index}] toDouble(''{0}'') => {1}")
    @MethodSource("emptyOrNullProvider")
    void toDouble_EmptyOrNull_ReturnsZero(String input, Double expected) {
        Double result = StringConversions.toDouble(input, mockConverter);
        assertEquals(expected, result, "Empty or null input should yield 0.0");
    }

    @Test
    void toDouble_WhitespaceOnlyString_ThrowsIllegalArgumentException() {
        String ws = "   ";
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toDouble(ws, mockConverter));
        assertNotNull(ex.getCause());
        assertTrue(ex.getCause() instanceof NumberFormatException);
    }
}
