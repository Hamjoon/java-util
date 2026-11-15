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
class StringConversions_toBoolean_Test {

    /**
     * The Converter dependency required by {@code StringConversions.toBoolean}.
     */
    @Mock
    private Converter // No behavior needed for the current tests
    converter;

    private static Stream<Arguments> provideExactMatches() {
        return Stream.of(Arguments.of("true", Boolean.TRUE), Arguments.of("false", Boolean.FALSE));
    }

    private static Stream<Arguments> provideCaseInsensitiveTrue() {
        return Stream.of(Arguments.of("TRUE"), Arguments.of("TrUe"));
    }

    private static Stream<Arguments> provideSingleCharTrue() {
        return Stream.of(Arguments.of("t"), Arguments.of("T"), Arguments.of("y"), Arguments.of("Y"));
    }

    private static Stream<Arguments> provideNonTrueValues() {
        return Stream.of(Arguments.of("False"), Arguments.of("no"), Arguments.of("0"), Arguments.of(""), Arguments.of(" true "));
    }

    @Test
    void shouldReturnTrueForExactStringTrue() {
        Boolean result = StringConversions.toBoolean("true", converter);
        assertTrue(result);
    }

    @Test
    void shouldReturnTrueForCaseInsensitiveTrue() {
        Boolean result = StringConversions.toBoolean("TrUe", converter);
        assertTrue(result);
    }

    @Test
    void shouldReturnTrueForSingleLetterT() {
        Boolean result = StringConversions.toBoolean("t", converter);
        assertTrue(result);
    }

    @Test
    void shouldReturnTrueForNumericOne() {
        Boolean result = StringConversions.toBoolean("1", converter);
        assertTrue(result);
    }

    @Test
    void shouldReturnTrueForLetterY() {
        Boolean result = StringConversions.toBoolean("y", converter);
        assertTrue(result);
    }

    @Test
    void shouldReturnTrueForQuotedTrue() {
        Boolean result = StringConversions.toBoolean("\"true\"", converter);
        assertTrue(result);
    }

    @Test
    void shouldReturnFalseForExactStringFalse() {
        Boolean result = StringConversions.toBoolean("false", converter);
        assertFalse(result);
    }

    @Test
    void shouldReturnFalseForUnrecognisedString() {
        Boolean result = StringConversions.toBoolean("maybe", converter);
        assertFalse(result);
    }

    @Test
    void shouldReturnFalseForNullInput() {
        // The implementation casts null to String, resulting in a null reference.
        // All subsequent equals checks safely return false, so the method returns Boolean.FALSE.
        Boolean result = StringConversions.toBoolean(null, converter);
        assertFalse(result, "Null input should be interpreted as false");
    }

    @ParameterizedTest(name = "[{index}] from=\"{0}\" → {1}")
    @MethodSource("provideExactMatches")
    void shouldHandleExactMatches(Object from, Boolean expected) {
        Boolean result = StringConversions.toBoolean(from, converter);
        assertEquals(expected, result, () -> "Expected " + expected + " for input: " + from);
    }

    @ParameterizedTest(name = "[{index}] from=\"{0}\" → true")
    @MethodSource("provideCaseInsensitiveTrue")
    void shouldTreatCaseInsensitiveTrueAsTrue(String from) {
        Boolean result = StringConversions.toBoolean(from, converter);
        assertTrue(result, () -> "Expected true for case‑insensitive input: " + from);
    }

    @ParameterizedTest(name = "[{index}] from=\"{0}\" → true")
    @MethodSource("provideSingleCharTrue")
    void shouldTreatSingleCharTrueAsTrue(String from) {
        Boolean result = StringConversions.toBoolean(from, converter);
        assertTrue(result, () -> "Expected true for shortcut input: " + from);
    }

    @Test
    void shouldTreatNumericOneAsTrue() {
        Boolean result = StringConversions.toBoolean("1", converter);
        assertTrue(result, "String \"1\" should be interpreted as true");
    }

    @Test
    void shouldTreatQuotedTrueAsTrue() {
        Boolean result = StringConversions.toBoolean("\"true\"", converter);
        assertTrue(result, "The literal \"\\\"true\\\"\" should be interpreted as true");
    }

    @ParameterizedTest(name = "[{index}] from=\"{0}\" → false")
    @MethodSource("provideNonTrueValues")
    void shouldReturnFalseForNonTrueValues(String from) {
        Boolean result = StringConversions.toBoolean(from, converter);
        assertFalse(result, () -> "Expected false for input: " + from);
    }

    @Test
    void shouldThrowClassCastExceptionForNonStringInput() {
        // Passing an Integer triggers the cast to String and therefore a ClassCastException.
        assertThrows(ClassCastException.class, () -> StringConversions.toBoolean(123, converter), "Non‑String input should cause ClassCastException");
    }
}
