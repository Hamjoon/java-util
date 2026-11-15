package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static com.cedarsoftware.util.StringUtilities.lastIndexOf;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringUtilities_lastIndexOf_Test {

    static Stream<Arguments> provideVariousScenarios() {
        return Stream.of(Arguments.of("abcabc", 'a', 3), Arguments.of("abcabc", 'b', 4), Arguments.of("abcabc", 'c', 5), Arguments.of("aaaaa", 'a', 4), Arguments.of("xyz", 'x', 0), Arguments.of("xyz", 'y', 1), Arguments.of("xyz", 'z', 2));
    }

    static Stream<Arguments> provideNullAndNormalCases() {
        // Note: the emoji case uses the high surrogate character as explained above.
        char emojiHighSurrogate = "\uD83D".charAt(0);
        return Stream.of(Arguments.of("null path", null, 'a', -1), Arguments.of("empty string", "", 'a', -1), Arguments.of("single occurrence at start", "abc", 'a', 0), Arguments.of("multiple occurrences, last at 3", "abca", 'a', 3), Arguments.of("character not present", "abca", 'z', -1), Arguments.of("single‑character match", "a", 'a', 0), Arguments.of("single‑character no match", "a", 'b', -1), Arguments.of("unicode surrogate pair (emoji) last index", "😀a😀b😀", emojiHighSurrogate, 6), Arguments.of("newline character", "Hello\nWorld", '\n', 5));
    }

    static Stream<Arguments> provideSpecialCharacterCases() {
        return Stream.of(Arguments.of("null character (U+0000)", "ab\u0000cd", '\u0000', 2), Arguments.of("all spaces", "   ", ' ', 2), Arguments.of("two tabs", "\t\t", '\t', 1), Arguments.of("carriage‑return before final newline", "line1\r\nline2\r\n", '\r', 12), Arguments.of("case‑sensitive mismatch", "abc", 'A', -1), Arguments.of("non‑existent high Unicode (U+FFFF)", "abc", '\uffff', -1));
    }

    @Test
    void testLastIndexOf_whenPathIsNull_returnsMinusOne() {
        assertEquals(-1, lastIndexOf(null, 'a'), "null path should return -1");
    }

    @Test
    void testLastIndexOf_whenPathIsEmpty_returnsMinusOne() {
        assertEquals(-1, lastIndexOf("", 'x'), "empty path should return -1");
    }

    @Test
    void testLastIndexOf_whenCharNotPresent_returnsMinusOne() {
        assertEquals(-1, lastIndexOf("hello world", 'z'), "character not present should return -1");
    }

    @Test
    void testLastIndexOf_whenCharPresentOnce_returnsCorrectIndex() {
        // "openai"
        // indexes: 0=o,1=p,2=e,3=n,4=a,5=i
        assertEquals(3, lastIndexOf("openai", 'n'), "should return index of the sole occurrence");
    }

    @Test
    void testLastIndexOf_whenCharPresentMultipleTimes_returnsLastIndex() {
        // "banana"
        // indexes of 'a': 1,3,5
        assertEquals(5, lastIndexOf("banana", 'a'), "should return the last occurrence index");
    }

    @ParameterizedTest
    @CsvSource({ "'abcabc', a, 3", "'abcabc', b, 4", "'abcabc', c, 5", "'aaaaa', a, 4", "'xyz', x, 0", "'xyz', y, 1", "'xyz', z, 2" })
    void testLastIndexOf_variousScenarios(String path, char ch, int expected) {
        assertEquals(expected, lastIndexOf(path, ch), () -> String.format("lastIndexOf('%s', '%c') should be %d", path, ch, expected));
    }

    @Test
    void testLastIndexOf_withUnicodeCharacters() {
        // Using a surrogate‑pair emoji (U+1F600). The high surrogate is '\uD83D'.
        String path = "😀a😀b😀";
        // first char of the emoji
        char highSurrogate = "\uD83D".charAt(0);
        assertEquals(6, lastIndexOf(path, highSurrogate), "should locate the high surrogate of the last emoji at index 6");
        // 'δ' appears at indexes 3 and 7, last should be 7
        assertEquals(7, lastIndexOf("αβγδαβγδ", 'δ'));
    }

    @Test
    void testLastIndexOf_withSpecialCharacters() {
        String path = "a!b@c#d$e%f^g&h*i(j)k";
        assertEquals(12, lastIndexOf(path, '^'), "should locate special character '^'");
        assertEquals(18, lastIndexOf(path, ')'), "should locate ')' correctly");
    }

    /**
     * Group: "lastIndexOf(String, char) - null and normal cases"
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideNullAndNormalCases")
    void testLastIndexOf_nullAndNormalCases(String description, String path, char ch, int expected) {
        assertEquals(expected, lastIndexOf(path, ch), () -> String.format("%s => expected %d", description, expected));
    }

    /**
     * Group: "lastIndexOf - special characters"
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideSpecialCharacterCases")
    void testLastIndexOf_specialCharacters(String description, String path, char ch, int expected) {
        assertEquals(expected, lastIndexOf(path, ch), () -> String.format("%s => expected %d", description, expected));
    }
}
