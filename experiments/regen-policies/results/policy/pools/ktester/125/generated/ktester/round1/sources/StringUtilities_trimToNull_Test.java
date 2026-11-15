package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;
import static com.cedarsoftware.util.StringUtilities.trimToNull;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class StringUtilities_trimToNull_Test {

    private static Stream<Arguments> whitespaceStrings() {
        return Stream.of(Arguments.of(" "), Arguments.of("\t"), Arguments.of("\n"), Arguments.of("\r\n"), Arguments.of(" \t \n "), // EM SPACE – not trimmed by String.trim()
        Arguments.of("\u2003"));
    }

    private static Stream<Arguments> nonWhitespaceStrings() {
        return Stream.of(Arguments.of("foo", "foo"), Arguments.of("  foo", "foo"), Arguments.of("foo  ", "foo"), Arguments.of("  foo  ", "foo"), Arguments.of("\tfoo\n", "foo"), // EM SPACE trimmed because it is not whitespace for String.trim()
        Arguments.of("\u2003bar\u2003", "bar"));
    }

    /**
     * All test cases extracted from the JSON description.
     * The expected value may be {@code null}.
     */
    private static Stream<Arguments> trimToNullCases() {
        return Stream.of(// null and empty are already covered, but we keep them for completeness
        Arguments.of(null, null), Arguments.of("", null), // Only whitespace (space, tab, newline, carriage‑return)
        Arguments.of("   ", null), Arguments.of("\t\n\r", null), // Normal trimming
        Arguments.of(" abc ", "abc"), Arguments.of("abc", "abc"), Arguments.of("   abc   def   ", "abc   def"), // Non‑breaking space (U+00A0) – not removed by String.trim()
        Arguments.of("\u00A0", "\u00A0"), // Null characters (U+0000) – trimmed because they are <= U+0020
        Arguments.of("\u0000\u0000", null), Arguments.of("\u0000abc\u0000", "abc"), // EM SPACE (U+2003) – not considered whitespace by String.trim()
        Arguments.of("\u2003", "\u2003"), // NBSP followed by regular space
        Arguments.of("\u00A0 ", "\u00A0"), Arguments.of(" \t\u2003abc \n", "\u2003abc"), Arguments.of("abc\u2003def", "abc\u2003def"), // Zero‑width space (U+200B) – not trimmed
        Arguments.of("\u200B", "\u200B"));
    }

    /**
     * Helper to make invisible characters visible in assertion messages.
     */
    private static String escapeForMessage(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t").replace("\u00A0", "\\u00A0").replace("\u2003", "\\u2003").replace("\u200B", "\\u200B").replace("\u0000", "\\u0000");
    }

    @Test
    void trimToNull_givenNull_returnsNull() {
        assertNull(trimToNull(null), "trimToNull(null) should return null");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void trimToNull_givenNullOrEmpty_returnsNull(String input) {
        assertNull(trimToNull(input), "trimToNull(\"\" or null) should return null");
    }

    @ParameterizedTest
    @MethodSource("whitespaceStrings")
    void trimToNull_givenOnlyWhitespace_returnsNull(String whitespace) {
        assertNull(trimToNull(whitespace), "trimToNull(whitespace-only) should return null");
    }

    @ParameterizedTest
    @MethodSource("nonWhitespaceStrings")
    void trimToNull_givenNonWhitespace_returnsTrimmedString(String original, String expected) {
        assertEquals(expected, trimToNull(original), "trimToNull should trim surrounding whitespace and return the result");
    }

    /**
     * Comprehensive parameterised test that exercises all cases from the provided JSON matrix,
     * including special Unicode characters and control characters.
     */
    @ParameterizedTest
    @MethodSource("trimToNullCases")
    void trimToNull_variousCases(String input, String expected) {
        assertEquals(expected, trimToNull(input), () -> "trimToNull(\"" + escapeForMessage(input) + "\") should return " + (expected == null ? "null" : "\"" + escapeForMessage(expected) + "\""));
    }
}
