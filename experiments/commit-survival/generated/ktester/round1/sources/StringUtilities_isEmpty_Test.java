package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringUtilities_isEmpty_Test {

    private static Stream<Arguments> provideCharSequenceAndExpectedResult() {
        return Stream.of(// Null input
        Arguments.of("Null Input", null, true), // Empty string
        Arguments.of("Empty String", "", true), // Single space
        Arguments.of("Single Space", " ", true), // Multiple whitespace characters (space, tab, newline, carriage‑return, form‑feed)
        Arguments.of("Multiple Whitespace Characters", " \t\n\r\f", true), // Unicode EM SPACE (U+2003) – recognised as whitespace by Character.isWhitespace
        Arguments.of("Unicode Whitespace (EM SPACE)", "\u2003", true), // Non‑whitespace single character
        Arguments.of("Non‑Whitespace Single Character", "a", false), // Mixed whitespace and visible content
        Arguments.of("Mixed Whitespace and Content", " a ", false), // StringBuilder containing only whitespace (including a tab)
        Arguments.of("StringBuilder Whitespace Only", new StringBuilder("   \t"), true), // StringBuilder with a non‑whitespace character followed by whitespace
        Arguments.of("StringBuilder With Content", new StringBuilder("x\n"), false), // Custom CharSequence with length 0
        Arguments.of("Custom Empty CharSequence", new CharSequence() {

            @Override
            public int length() {
                return 0;
            }

            @Override
            public char charAt(int index) {
                throw new IndexOutOfBoundsException();
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                return this;
            }

            @Override
            public String toString() {
                return "";
            }
        }, true), // Custom CharSequence that returns only whitespace characters
        Arguments.of("Custom Whitespace CharSequence", new CharSequence() {

            private final char[] data = { ' ', '\t' };

            @Override
            public int length() {
                return data.length;
            }

            @Override
            public char charAt(int index) {
                return data[index];
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                return new String(data, start, end - start);
            }

            @Override
            public String toString() {
                return new String(data);
            }
        }, true), // Custom CharSequence with a non‑whitespace character
        Arguments.of("Custom Non‑Whitespace CharSequence", new CharSequence() {

            private final String data = "z";

            @Override
            public int length() {
                return data.length();
            }

            @Override
            public char charAt(int index) {
                return data.charAt(index);
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                return data.subSequence(start, end);
            }

            @Override
            public String toString() {
                return data;
            }
        }, false));
    }

    private static Stream<Arguments> provideCharSequenceAndExpectedException() {
        return Stream.of(// length() throws IllegalStateException
        Arguments.of("CharSequence length() throws RuntimeException", new CharSequence() {

            @Override
            public int length() {
                throw new IllegalStateException("length failure");
            }

            @Override
            public char charAt(int index) {
                return ' ';
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                return this;
            }

            @Override
            public String toString() {
                return "";
            }
        }, IllegalStateException.class), // charAt throws IndexOutOfBoundsException
        Arguments.of("CharSequence charAt throws IndexOutOfBoundsException", new CharSequence() {

            @Override
            public int length() {
                return 1;
            }

            @Override
            public char charAt(int index) {
                throw new IndexOutOfBoundsException("charAt failure");
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                return this;
            }

            @Override
            public String toString() {
                return "";
            }
        }, IndexOutOfBoundsException.class), // charAt throws UnsupportedOperationException
        Arguments.of("CharSequence charAt throws UnsupportedOperationException", new CharSequence() {

            @Override
            public int length() {
                return 2;
            }

            @Override
            public char charAt(int index) {
                throw new UnsupportedOperationException("charAt unsupported");
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                return this;
            }

            @Override
            public String toString() {
                return "  ";
            }
        }, UnsupportedOperationException.class));
    }

    /**
     * The {@code isEmpty(CharSequence)} method delegates to {@code isWhitespace(CharSequence)}.
     * According to the library contract, a CharSequence is considered empty when it is
     * {@code null}, has length zero, or consists solely of whitespace characters.
     */
    @Test
    void givenNullCharSequence_thenIsEmptyReturnsTrue() {
        // given
        CharSequence cs = null;
        // when
        boolean result = StringUtilities.isEmpty(cs);
        // then
        assertTrue(result, "null CharSequence should be considered empty");
    }

    @Test
    void givenEmptyString_thenIsEmptyReturnsTrue() {
        // given
        CharSequence cs = "";
        // when
        boolean result = StringUtilities.isEmpty(cs);
        // then
        assertTrue(result, "empty string should be considered empty");
    }

    @Test
    void givenWhitespaceOnlyString_thenIsEmptyReturnsTrue() {
        // given
        CharSequence cs = " \t\n\r";
        // when
        boolean result = StringUtilities.isEmpty(cs);
        // then
        assertTrue(result, "string containing only whitespace should be considered empty");
    }

    @Test
    void givenNonWhitespaceString_thenIsEmptyReturnsFalse() {
        // given
        CharSequence cs = "Hello";
        // when
        boolean result = StringUtilities.isEmpty(cs);
        // then
        assertFalse(result, "non‑empty, non‑whitespace string should not be considered empty");
    }

    @Test
    void givenStringWithLeadingAndTrailingWhitespace_thenIsEmptyReturnsFalse() {
        // given
        CharSequence cs = "  abc  ";
        // when
        boolean result = StringUtilities.isEmpty(cs);
        // then
        assertFalse(result, "string with visible characters surrounded by whitespace should not be empty");
    }

    @Test
    void givenStringBuilderInstance_thenIsEmptyBehavesCorrectly() {
        // given
        CharSequence cs = new StringBuilder("   ");
        // when
        boolean result = StringUtilities.isEmpty(cs);
        // then
        assertTrue(result, "StringBuilder containing only whitespace should be considered empty");
    }

    @Test
    void givenStringBufferInstanceWithContent_thenIsEmptyBehavesCorrectly() {
        // given
        CharSequence cs = new StringBuffer("content");
        // when
        boolean result = StringUtilities.isEmpty(cs);
        // then
        assertFalse(result, "StringBuffer with non‑whitespace content should not be considered empty");
    }

    /**
     * Parameterised test for all inputs that are expected to return a boolean
     * result (true = empty/whitespace, false = non‑empty).
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideCharSequenceAndExpectedResult")
    void testIsEmptyBooleanScenarios(String description, CharSequence cs, boolean expected) {
        assertEquals(expected, StringUtilities.isEmpty(cs), description);
    }

    /**
     * Parameterised test for inputs that are expected to throw an exception.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideCharSequenceAndExpectedException")
    void testIsEmptyExceptionScenarios(String description, CharSequence cs, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> StringUtilities.isEmpty(cs), description);
    }
}
