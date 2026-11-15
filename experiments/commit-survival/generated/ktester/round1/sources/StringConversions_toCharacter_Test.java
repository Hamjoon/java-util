package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class StringConversions_toCharacter_Test {

    @Mock
    private Converter // injected by Mockito
    mockConverter;

    private static Stream<Arguments> numericEdgeCaseProvider() {
        return Stream.of(// "0" → null character
        arguments("0", (char) 0), // leading zeros are ignored
        arguments("065", 'A'), // plus sign not matched by allDigits
        arguments("+65", IllegalArgumentException.class), // negative number not matched
        arguments("-1", IllegalArgumentException.class));
    }

    private static Stream<Arguments> unicodeEscapeEdgeCaseProvider() {
        return Stream.of(// lower‑case hex digits
        arguments("\\u00a9", '©'), // extra characters after escape
        arguments("\\u0041extra", IllegalArgumentException.class), // leading whitespace
        arguments(" \\u0041", IllegalArgumentException.class), // fewer than four hex digits
        arguments("\\u123", IllegalArgumentException.class), // non‑hex character inside escape
        arguments("\\u00G1", IllegalArgumentException.class));
    }

    @Test
    void emptyStringReturnsZeroChar() {
        // given
        String input = "";
        // when
        char result = StringConversions.toCharacter(input, mockConverter);
        // then
        assertEquals((char) 0, result, "Empty string should convert to the null character (value 0).");
    }

    @Test
    void singleCharacterStringReturnsThatChar() {
        // given
        String input = "Z";
        // when
        char result = StringConversions.toCharacter(input, mockConverter);
        // then
        assertEquals('Z', result);
    }

    @Test
    void numericStringIsParsedAsUnicodeCodePoint() {
        // given
        // 65 == 'A'
        String input = "65";
        // when
        char result = StringConversions.toCharacter(input, mockConverter);
        // then
        assertEquals('A', result);
    }

    @Test
    void numericStringWithWhitespaceIsTrimmedBeforeParsing() {
        // given
        // 66 == 'B'
        String input = "  66  ";
        // when
        char result = StringConversions.toCharacter(input, mockConverter);
        // then
        assertEquals('B', result);
    }

    @Test
    void nonParsableNumericStringThrowsIllegalArgumentException() {
        // given
        // exceeds int range
        String input = "9999999999";
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toCharacter(input, mockConverter));
        assertTrue(ex.getMessage().contains("Unable to parse"), "Exception message should indicate parsing failure.");
    }

    @Test
    void unicodeEscapeSequenceIsConvertedToCorrespondingChar() {
        // given
        // The method expects a literal backslash followed by 'u' and four hex digits.
        // Unicode for 'A'
        String input = "\\u0041";
        // when
        char result = StringConversions.toCharacter(input, mockConverter);
        // then
        assertEquals('A', result);
    }

    @Test
    void malformedUnicodeEscapeThrowsIllegalArgumentException() {
        // given
        // not a valid 4‑digit hex
        String input = "\\uXYZ";
        // when / then
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toCharacter(input, mockConverter));
    }

    @Test
    void nullInputThrowsNullPointerException() {
        // given
        Object input = null;
        // when / then
        assertThrows(NullPointerException.class, () -> StringConversions.toCharacter(input, mockConverter));
    }

    @Test
    void nonStringInputThrowsClassCastException() {
        // given
        // Integer, not a String
        Object input = 123;
        // when / then
        assertThrows(ClassCastException.class, () -> StringConversions.toCharacter(input, mockConverter));
    }

    @ParameterizedTest(name = "[{index}] from=\"{0}\" → expected={1}")
    @MethodSource("numericEdgeCaseProvider")
    void numericConversionEdgeCases(Object from, Object expected) {
        if (expected instanceof Character) {
            char result = StringConversions.toCharacter(from, mockConverter);
            assertEquals(expected, result);
        } else if (expected instanceof Class) {
            assertThrows((Class<? extends Throwable>) expected, () -> StringConversions.toCharacter(from, mockConverter));
        } else {
            fail("Invalid test data");
        }
    }

    @ParameterizedTest(name = "[{index}] from=\"{0}\" → expected={1}")
    @MethodSource("unicodeEscapeEdgeCaseProvider")
    void unicodeEscapeEdgeCases(String from, Object expected) {
        if (expected instanceof Character) {
            char result = StringConversions.toCharacter(from, mockConverter);
            assertEquals(expected, result);
        } else if (expected instanceof Class) {
            assertThrows((Class<? extends Throwable>) expected, () -> StringConversions.toCharacter(from, mockConverter));
        } else {
            fail("Invalid test data");
        }
    }

    @Test
    void converterParameterIsIgnoredEvenWhenNonNull() {
        // given
        // should become 'B'
        String input = "66";
        Converter anotherMock = org.mockito.Mockito.mock(Converter.class);
        // when
        char result = StringConversions.toCharacter(input, anotherMock);
        // then
        assertEquals('B', result, "Method must ignore the Converter instance and parse the string correctly.");
    }

    @Test
    void whitespaceOnlyStringThrowsIllegalArgumentException() {
        // given
        String input = "   ";
        // when / then
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toCharacter(input, mockConverter));
    }
}
