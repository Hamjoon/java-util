package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class StringUtilities_isWhitespace_Test {

    private static Stream<Arguments> stringsWithAllWhitespace() {
        return Stream.of(Arguments.of(""), Arguments.of(" "), Arguments.of("\t"), Arguments.of("\n"), Arguments.of("\r"), Arguments.of("\f"), Arguments.of(" \t\n\r\f "), // EN QUAD
        Arguments.of("\u2000"), // IDEOGRAPHIC SPACE
        Arguments.of("\u3000"), // EM SPACE + IDEOGRAPHIC SPACE
        Arguments.of("\u2003\u3000"), // FIGURE SPACE – treated as whitespace by Character.isWhitespace
        Arguments.of("\u2007"));
    }

    private static Stream<Arguments> stringsWithContentOtherThanWhitespace() {
        return Stream.of(Arguments.of("a"), Arguments.of(" a"), Arguments.of("a "), Arguments.of(" a "), Arguments.of("foo"), Arguments.of("\ta"), Arguments.of("a\t"), Arguments.of("a\nb"), // contains a non‑standard whitespace between letters
        Arguments.of("a\u2001b"));
    }

    private static Stream<Arguments> stringsWithNonWhitespace() {
        return Stream.of(Arguments.of("a"), Arguments.of(" a"), Arguments.of("a "), Arguments.of(" a "), Arguments.of("foo"), Arguments.of("\ta"), Arguments.of("a\t"), Arguments.of("a\nb"), // NON‑BREAKING SPACE – NOT whitespace per Character.isWhitespace
        Arguments.of("\u00A0"), // Figure space + non‑whitespace
        Arguments.of(" \u2007X"), // long series of regular spaces – still whitespace, but included for completeness
        Arguments.of("          "));
    }

    private static Stream<Arguments> customCharSequencesReturningBoolean() {
        // StringBuilder containing only whitespace
        CharSequence sbWhitespace = new StringBuilder("\t \n");
        // StringBuffer containing whitespace + non‑whitespace
        CharSequence sbufMixed = new StringBuffer(" \u2007X");
        // Anonymous CharSequence: two whitespace chars followed by a non‑whitespace 'a'
        CharSequence anonymous = new CharSequence() {

            @Override
            public int length() {
                return 3;
            }

            // EM SPACE then 'a'
            @Override
            public char charAt(int i) {
                return i == 2 ? 'a' : '\u2003';
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                return null;
            }

            @Override
            public String toString() {
                return "custom";
            }
        };
        return Stream.of(Arguments.of(sbWhitespace, true), Arguments.of(sbufMixed, false), Arguments.of(anonymous, false));
    }

    @ParameterizedTest
    @MethodSource("stringsWithAllWhitespace")
    void testIsWhitespace_whenStringHasOnlyWhitespace_returnsTrue(String s) {
        assertTrue(StringUtilities.isWhitespace(s), () -> "Expected true for whitespace‑only string: [" + s + "]");
    }

    @ParameterizedTest
    @MethodSource("stringsWithContentOtherThanWhitespace")
    void testIsWhitespace_whenStringHasNonWhitespace_returnsFalse(String s) {
        assertFalse(StringUtilities.isWhitespace(s), () -> "Expected false for string containing non‑whitespace: [" + s + "]");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testIsWhitespace_whenNullOrEmpty_returnsTrue(String s) {
        // According to the implementation, null or empty is considered whitespace.
        assertTrue(StringUtilities.isWhitespace(s), () -> "Expected true for null or empty input: " + s);
    }

    @ParameterizedTest
    @MethodSource("stringsWithNonWhitespace")
    void testIsWhitespace_whenStringContainsNonWhitespace_returnsFalse(String s) {
        assertFalse(StringUtilities.isWhitespace(s), () -> "Expected false for string containing non‑whitespace: [" + s + "]");
    }

    @ParameterizedTest
    @MethodSource("customCharSequencesReturningBoolean")
    void testIsWhitespace_withCustomCharSequence_returnsExpected(CharSequence cs, boolean expected) {
        assertEquals(expected, StringUtilities.isWhitespace(cs), () -> "Unexpected result for custom CharSequence: " + cs);
    }

    @Test
    void testIsWhitespace_whenLengthThrowsException_propagatesException() {
        CharSequence cs = new CharSequence() {

            @Override
            public int length() {
                throw new IllegalStateException("length failure");
            }

            @Override
            public char charAt(int i) {
                return ' ';
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                return null;
            }
        };
        assertThrows(IllegalStateException.class, () -> StringUtilities.isWhitespace(cs), "Expected IllegalStateException to be propagated from length()");
    }

    @Test
    void testIsWhitespace_whenLengthIsNegative_throwsStringIndexOutOfBoundsException() {
        CharSequence cs = new CharSequence() {

            @Override
            public int length() {
                return -1;
            }

            @Override
            public char charAt(int i) {
                return ' ';
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                return null;
            }
        };
        // The implementation will attempt to iterate from 0 to -1, which should result in
        // a StringIndexOutOfBoundsException when charAt is accessed (if the loop were to run).
        // We assert that such an exception is thrown.
        assertThrows(StringIndexOutOfBoundsException.class, () -> StringUtilities.isWhitespace(cs), "Expected StringIndexOutOfBoundsException due to negative length");
    }
}
