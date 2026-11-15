package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.Date;
import java.util.Objects;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toInt_Test {

    /**
     * A mock {@link Converter}. The implementation of {@code toInt} does not use it, but the signature requires it.
     */
    @Mock
    private Converter mockConverter;

    private static Stream<Arguments> emptyAndDirectParsingProvider() {
        return Stream.of(// Empty string
        Arguments.of("Empty string should return 0", "", null, 0), // Direct positive integer
        Arguments.of("Valid integer string \"123\"", "123", null, 123), // Direct negative integer
        Arguments.of("Valid negative integer string \"-456\"", "-456", null, -456));
    }

    private static Stream<Arguments> nullAndWhitespaceProvider() {
        return Stream.of(// Null input
        Arguments.of("Null input should be treated as empty and return 0", null, null, 0), // Whitespace only – not empty per StringUtilities, leads to exception
        Arguments.of("Whitespace‑only string should throw IllegalArgumentException", "   ", null, IllegalArgumentException.class));
    }

    private static Stream<Arguments> plusAndLeadingZerosProvider() {
        return Stream.of(Arguments.of("Leading plus sign is accepted", "+42", null, 42), Arguments.of("Leading zeros are ignored", "00000123", null, 123));
    }

    private static Stream<Arguments> outOfRangeProvider() {
        return Stream.of(Arguments.of("Value just above Integer.MAX_VALUE", "2147483648", null, IllegalArgumentException.class), Arguments.of("Value just below Integer.MIN_VALUE", "-2147483649", null, IllegalArgumentException.class), Arguments.of("Huge long value exceeding int range", "9223372036854775807", null, IllegalArgumentException.class));
    }

    private static Stream<Arguments> nonNumericProvider() {
        return Stream.of(Arguments.of("Alphabetic characters mixed with digits", "abc123", null, IllegalArgumentException.class), Arguments.of("Decimal representation with fraction", "123.45", null, IllegalArgumentException.class), Arguments.of("Scientific notation not accepted", "1e3", null, IllegalArgumentException.class));
    }

    private static Stream<Arguments> nonStringInputProvider() {
        return Stream.of(Arguments.of("Integer object causes ClassCastException", 12345, null, ClassCastException.class), Arguments.of("Date object causes ClassCastException", new Date(), null, ClassCastException.class), Arguments.of("Boolean true causes ClassCastException", true, null, ClassCastException.class), Arguments.of("StringBuilder instance causes ClassCastException", new StringBuilder("123"), null, ClassCastException.class), Arguments.of("Generic Object causes ClassCastException", new Object(), null, ClassCastException.class));
    }

    private static Stream<Arguments> whitespaceNumericProvider() {
        return Stream.of(Arguments.of("Leading space before number", " 123", null, IllegalArgumentException.class), Arguments.of("Trailing space after number", "123 ", null, IllegalArgumentException.class), Arguments.of("Newline character before number", "\n456", null, IllegalArgumentException.class));
    }

    private static Stream<Arguments> plusSignOutOfRangeProvider() {
        return Stream.of(Arguments.of("Plus sign with value exceeding MAX_VALUE", "+2147483648", null, IllegalArgumentException.class), Arguments.of("Malformed plus‑sign with space", "+ -123", null, IllegalArgumentException.class));
    }

    @Test
    void emptyStringReturnsZero() {
        // given
        String input = "";
        // when
        Integer result = StringConversions.toInt(input, mockConverter);
        // then
        assertEquals(0, result);
    }

    @Test
    void nullStringReturnsZero() {
        // given
        String input = null;
        // when
        Integer result = StringConversions.toInt(input, mockConverter);
        // then
        assertEquals(0, result);
    }

    @Test
    void validIntegerStringIsParsed() {
        // given
        String input = "12345";
        // when
        Integer result = StringConversions.toInt(input, mockConverter);
        // then
        assertEquals(12345, result);
    }

    @Test
    void numericStringBeyondIntRangeThrowsException() {
        // given
        // "2147483648"
        String input = String.valueOf((long) Integer.MAX_VALUE + 1);
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toInt(input, mockConverter));
        assertTrue(ex.getMessage().contains("not parseable as an int value"));
    }

    @Test
    void nonNumericStringThrowsException() {
        // given
        String input = "notANumber";
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toInt(input, mockConverter));
        assertTrue(ex.getMessage().contains("not parseable as an int value"));
    }

    @Test
    void negativeNumberBeyondIntRangeThrowsException() {
        // given
        // "-2147483649"
        String input = String.valueOf((long) Integer.MIN_VALUE - 1);
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toInt(input, mockConverter));
        assertTrue(ex.getMessage().contains("not parseable as an int value"));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("emptyAndDirectParsingProvider")
    void toInt_emptyAndDirectParsing(String description, Object from, Object converter, Object expected) {
        if (expected instanceof Class && ((Class<?>) expected).equals(ClassCastException.class)) {
            assertThrows(ClassCastException.class, () -> StringConversions.toInt(from, (Converter) converter));
        } else {
            Integer result = StringConversions.toInt(from, (Converter) converter);
            assertEquals(expected, result, description);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullAndWhitespaceProvider")
    void toInt_nullAndWhitespace(String description, Object from, Object converter, Object expected) {
        if (expected instanceof Class && ((Class<?>) expected).equals(IllegalArgumentException.class)) {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toInt(from, (Converter) converter));
            assertTrue(ex.getMessage().contains("not parseable as an int value"));
        } else {
            Integer result = StringConversions.toInt(from, (Converter) converter);
            assertEquals(expected, result, description);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("plusAndLeadingZerosProvider")
    void toInt_plusAndLeadingZeros(String description, Object from, Object converter, Object expected) {
        Integer result = StringConversions.toInt(from, (Converter) converter);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("outOfRangeProvider")
    void toInt_outOfRange(String description, Object from, Object converter, Class<?> expectedException) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toInt(from, (Converter) converter));
        assertTrue(ex.getMessage().contains("not parseable as an int value"));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nonNumericProvider")
    void toInt_nonNumeric(String description, Object from, Object converter, Class<?> expectedException) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toInt(from, (Converter) converter));
        assertTrue(ex.getMessage().contains("not parseable as an int value"));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nonStringInputProvider")
    void toInt_nonStringInput(String description, Object from, Object converter, Class<?> expectedException) {
        assertThrows(ClassCastException.class, () -> StringConversions.toInt(from, (Converter) converter));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("whitespaceNumericProvider")
    void toInt_whitespaceNumeric(String description, Object from, Object converter, Class<?> expectedException) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toInt(from, (Converter) converter));
        assertTrue(ex.getMessage().contains("not parseable as an int value"));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("plusSignOutOfRangeProvider")
    void toInt_plusSignOutOfRange(String description, Object from, Object converter, Class<?> expectedException) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toInt(from, (Converter) converter));
        assertTrue(ex.getMessage().contains("not parseable as an int value"));
    }
}
