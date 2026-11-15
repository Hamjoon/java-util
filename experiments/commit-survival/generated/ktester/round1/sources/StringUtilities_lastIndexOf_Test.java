package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

class StringUtilities_lastIndexOf_Test {

    private static Stream<Arguments> nullAndNormalCases() {
        return Stream.of(// Null input
        Arguments.of(null, 'a', -1, "When the input string is null the method should return -1 without throwing an exception."), // Empty string
        Arguments.of("", 'a', -1, "Empty string does not contain any character, so lastIndexOf should return -1."), // Character at the start
        Arguments.of("abcde", 'a', 0, "Character 'a' appears at the beginning; last index is 0."), // Character appears twice – last occurrence
        Arguments.of("abcda", 'a', 4, "Character 'a' appears twice, last occurrence at index 4."), // Character not present
        Arguments.of("abcde", 'z', -1, "Character not present in the string should result in -1."), // Case‑sensitive search
        Arguments.of("aAaAaA", 'A', 5, "Case-sensitive search; last uppercase 'A' is at index 5."), // Unicode surrogate handling – searching for the high surrogate of an emoji
        Arguments.of("😀😃😀", '\uD83D', 4, "Unicode surrogate characters are handled; last index of the high surrogate is 4."));
    }

    private static Stream<Arguments> additionalEdgeCases() {
        return Stream.of(// Single‑character match
        Arguments.of("x", 'x', 0, "Single‑character string where the searched character matches; should return index 0."), // Single‑character no match
        Arguments.of("x", 'y', -1, "Single‑character string where the searched character does not exist; should return -1."), // Character at the very end
        Arguments.of("hello", 'o', 4, "Character appears at the very end of the string; last index should be the final position."), // Multiple non‑consecutive occurrences
        Arguments.of("abacada", 'a', 6, "Multiple non‑consecutive occurrences of the character; the method must return the highest index."), // All characters identical
        Arguments.of("bbbb", 'b', 3, "All characters are the same; last occurrence is at the last position."), // Embedded null characters (U+0000)
        Arguments.of("ab\u0000cd\u0000", '\u0000', 5, "String contains embedded null characters; the method should locate the last null character."), // Whitespace occurring multiple times
        Arguments.of(" a b c ", ' ', 6, "Whitespace character occurs multiple times, including at the end; last index should be 6."), // Newline character
        Arguments.of("line1\nline2", '\n', 5, "Newline character is present; method must return its index (5) correctly."), // Searching for a high surrogate code unit of an emoji
        Arguments.of("😀😃😀", '\uD83D', 4, "Search for a high surrogate code unit of an emoji; the last high surrogate appears at index 4."), // Empty string with any character (including null)
        Arguments.of("", '\u0000', -1, "Empty string with any character (including null character) should return -1."));
    }

    @Test
    void lastIndexOf_NullPath_ReturnsMinusOne() {
        // given
        String path = null;
        char ch = '/';
        // when
        int result = StringUtilities.lastIndexOf(path, ch);
        // then
        assertEquals(-1, result, "lastIndexOf should return -1 when the input string is null");
    }

    @Test
    void lastIndexOf_EmptyString_ReturnsMinusOne() {
        // given
        String path = "";
        char ch = 'a';
        // when
        int result = StringUtilities.lastIndexOf(path, ch);
        // then
        assertEquals(-1, result, "lastIndexOf should return -1 for an empty string when the character is not present");
    }

    @Test
    void lastIndexOf_CharNotPresent_ReturnsMinusOne() {
        // given
        String path = "abcdef";
        char ch = 'z';
        // when
        int result = StringUtilities.lastIndexOf(path, ch);
        // then
        assertEquals(-1, result, "lastIndexOf should return -1 when the character is absent");
    }

    @Test
    void lastIndexOf_CharPresentOnce_ReturnsCorrectIndex() {
        // given
        String path = "hello/world";
        char ch = '/';
        // when
        int result = StringUtilities.lastIndexOf(path, ch);
        // then
        assertEquals(5, result, "lastIndexOf should return the index of the sole occurrence of the character");
    }

    @Test
    void lastIndexOf_MultipleOccurrences_ReturnsLastIndex() {
        // given
        String path = "a/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p/q/r/s/t/u/v/w/x/y/z/";
        char ch = '/';
        // when
        int result = StringUtilities.lastIndexOf(path, ch);
        // then
        assertEquals(path.length() - 1, result, "lastIndexOf should return the index of the last occurrence of the character");
    }

    @Test
    void lastIndexOf_UnicodeCharacter_ReturnsCorrectIndex() {
        // given
        String path = "café☕️";
        char ch = '☕';
        // when
        int result = StringUtilities.lastIndexOf(path, ch);
        // then
        assertEquals(4, result, "lastIndexOf should correctly handle Unicode characters");
    }

    /* -------------------------------------------------------------
     *  Parameterised tests – Null and Normal Cases
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("nullAndNormalCases")
    void lastIndexOf_NullAndNormalCases(String path, char ch, int expected, String description) {
        // when
        int result = StringUtilities.lastIndexOf(path, ch);
        // then
        assertEquals(expected, result, description);
    }

    /* -------------------------------------------------------------
     *  Parameterised tests – Additional Edge Cases
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("additionalEdgeCases")
    void lastIndexOf_AdditionalEdgeCases(String path, char ch, int expected, String description) {
        // when
        int result = StringUtilities.lastIndexOf(path, ch);
        // then
        assertEquals(expected, result, description);
    }
}
