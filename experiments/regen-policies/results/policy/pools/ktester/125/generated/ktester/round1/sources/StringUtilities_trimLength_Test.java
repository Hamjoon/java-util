package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class StringUtilities_trimLength_Test {

    private static Stream<Arguments> trimLength_withoutWhitespaceProvider() {
        return Stream.of(Arguments.of("abc"), Arguments.of("HelloWorld"), Arguments.of("1234567890"), Arguments.of("!@#$%^&*()"));
    }

    private static Stream<Arguments> trimLength_withWhitespaceProvider() {
        return Stream.of(// leading & trailing spaces
        Arguments.of("  abc  ", 3), // mix of tabs, newlines and spaces
        Arguments.of("\t\n  hello world  \r\n", 11), // spaces inside should be preserved
        Arguments.of("  a b c  ", 5), // only leading whitespace
        Arguments.of("   xyz", 3), // only trailing whitespace
        Arguments.of("xyz   ", 3));
    }

    private static Stream<Arguments> withoutWhitespaceProvider() {
        return Stream.of(Arguments.of("abc"), Arguments.of("HelloWorld"), Arguments.of("1234567890"), Arguments.of("!@#$%^&*()"));
    }

    private static Stream<Arguments> withWhitespaceProvider() {
        return Stream.of(// leading & trailing spaces
        Arguments.of("  abc  ", 3), // mix of tabs, newlines and spaces
        Arguments.of("\t\n  hello world  \r\n", 11), // internal spaces are preserved
        Arguments.of("  a b c  ", 5), // only leading whitespace
        Arguments.of("   xyz", 3), // only trailing whitespace
        Arguments.of("xyz   ", 3));
    }

    private static Stream<Arguments> jsonProvidedCases() {
        return Stream.of(// From "trimLength method tests"
        Arguments.of(null, 0), Arguments.of("   ", 0), Arguments.of("abc", 3), Arguments.of("  abc  ", 3), Arguments.of("\t\nabc\r", 3), Arguments.of("", 0), // From "trimLength additional tests"
        Arguments.of("a b c", 5), Arguments.of("\u00A0abc\u00A0", 5), Arguments.of("\u00A0\u2007", 2), Arguments.of("😀", 2), Arguments.of("  😀  ", 2));
    }

    @Test
    void trimLength_null_returnsZero() {
        assertEquals(0, StringUtilities.trimLength(null), "null input should be treated as empty and return length 0");
    }

    @Test
    void trimLength_emptyString_returnsZero() {
        assertEquals(0, StringUtilities.trimLength(""), "empty string should stay empty after trimming");
    }

    @Test
    void trimLength_whitespaceOnly_returnsZero() {
        assertEquals(0, StringUtilities.trimLength("   \t\n\r"), "string consisting only of whitespace should be trimmed to empty");
    }

    @ParameterizedTest
    @MethodSource("trimLength_withoutWhitespaceProvider")
    void trimLength_withoutWhitespace_returnsExactLength(String input) {
        // no characters are removed
        int expected = input.length();
        assertEquals(expected, StringUtilities.trimLength(input));
    }

    @ParameterizedTest
    @MethodSource("trimLength_withWhitespaceProvider")
    void trimLength_withWhitespace_returnsTrimmedLength(String input, int expectedLength) {
        assertEquals(expectedLength, StringUtilities.trimLength(input));
    }

    @Test
    void trimLength_unicodeString_trimmedCorrectly() {
        // The string contains leading/trailing zero‑width spaces (U+200B)
        String input = "\u200B\u200BUnicode\u200B";
        // trimToEmpty uses Character.isWhitespace which treats U+200B as whitespace
        // After trimming we expect "Unicode" (length 7)
        assertEquals(7, StringUtilities.trimLength(input));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void trimLength_nullOrEmpty_alwaysZero(String input) {
        assertEquals(0, StringUtilities.trimLength(input));
    }

    @Test
    void trimLength_unicodeZeroWidthSpace_trimmedCorrectly() {
        // U+200B (ZERO WIDTH SPACE) is considered whitespace by Character.isWhitespace
        String input = "\u200B\u200BUnicode\u200B";
        // After trimming we expect "Unicode" (length 7)
        assertEquals(7, StringUtilities.trimLength(input));
    }

    @Test
    void trimLength_nonBreakingSpace_notTrimmed() {
        // U+00A0 (NO‑BREAK SPACE) is NOT trimmed by String.trim()
        String input = "\u00A0abc\u00A0";
        // Length stays 5 (two NBSP + three letters)
        assertEquals(5, StringUtilities.trimLength(input));
    }

    @Test
    void trimLength_unicodeWhitespaceNotTrimmedByStringTrim() {
        // U+2007 (FIGURE SPACE) is also not trimmed by String.trim()
        String input = "\u00A0\u2007";
        // Both characters remain, length = 2
        assertEquals(2, StringUtilities.trimLength(input));
    }

    @Test
    void trimLength_emoji_surrogatePairLengthIsTwo() {
        // Emoji "😀" is represented by a surrogate pair (length 2)
        String input = "😀";
        assertEquals(2, StringUtilities.trimLength(input));
    }

    @Test
    void trimLength_emojiWithSurroundingSpaces_trimmedToSurrogatePair() {
        // Spaces are trimmed, emoji remains (length 2)
        String input = "  😀  ";
        assertEquals(2, StringUtilities.trimLength(input));
    }

    @ParameterizedTest(name = "JSON case: \"{0}\" → {1}")
    @MethodSource("jsonProvidedCases")
    void trimLength_jsonCases(String input, int expected) {
        assertEquals(expected, StringUtilities.trimLength(input));
    }
}
