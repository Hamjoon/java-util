package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;

// kept from the template; no mocks are required for this class
@ExtendWith(MockitoExtension.class)
class StringUtilities_length_2_Test {

    private static Stream<Arguments> stringLengthProvider() {
        return Stream.of(Arguments.of("a", 1), Arguments.of("ab", 2), Arguments.of("abc", 3), Arguments.of(" ", 1), Arguments.of("  leading and trailing  ", 24), // Unicode characters – each counts as a single char in Java strings
        // ©
        Arguments.of("\u00A9", 1), // 😀 (surrogate pair, length 2)
        Arguments.of("\uD83D\uDE00", 2), // Longer text
        Arguments.of("The quick brown fox jumps over the lazy dog", 43));
    }

    /**
     * Provider that supplies the test data.
     */
    private static Stream<Arguments> lengthProvider() {
        return Stream.of(// JSON‑derived cases
        // null → 0
        Arguments.of(null, 0), // empty → 0
        Arguments.of("", 0), // simple ASCII
        Arguments.of("hello", 5), // emoji (surrogate pair)
        Arguments.of("😀", 2), // a + combining acute accent + b
        Arguments.of("a\u0301b", 3), // Additional edge‑cases
        // single space
        Arguments.of(" ", 1), // spaces at both ends
        Arguments.of("  leading and trailing  ", 24), // © (single code point)
        Arguments.of("\u00A9", 1), // 😀 expressed as two chars
        Arguments.of("\uD83D\uDE00", 2), Arguments.of("The quick brown fox jumps over the lazy dog", 43), // newlines & tabs
        Arguments.of("line1\nline2\r\n\tend", 15), // embedded null char
        Arguments.of("abc\u0000def", 7));
    }

    @Test
    void length_nullInput_returnsZero() {
        assertEquals(0, StringUtilities.length(null), "length(null) should return 0");
    }

    @Test
    void length_emptyString_returnsZero() {
        assertEquals(0, StringUtilities.length(""), "length(\"\") should return 0");
    }

    @ParameterizedTest(name = "[{index}] length(\"{0}\") = {1}")
    @MethodSource("stringLengthProvider")
    void length_variousInputs_returnsExpected(String input, int expected) {
        assertEquals(expected, StringUtilities.length(input), () -> "Unexpected length for input: \"" + input + "\"");
    }

    @Test
    void length_stringWithNewLinesAndTabs_returnsCorrectCount() {
        String s = "line1\nline2\r\n\tend";
        // characters: l i n e 1 \n l i n e 2 \r \n \t e n d  => 15
        assertEquals(15, StringUtilities.length(s));
    }

    @Test
    void length_stringWithEmbeddedNullChar_returnsCorrectCount() {
        String s = "abc\u0000def";
        // The embedded null character is a regular char in Java strings
        assertEquals(7, StringUtilities.length(s));
    }
}
