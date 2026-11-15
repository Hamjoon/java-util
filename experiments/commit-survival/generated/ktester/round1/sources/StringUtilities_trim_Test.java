package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringUtilities_trim_Test {

    private static Stream<Arguments> trimNullAndWhitespaceProvider() {
        return Stream.of(// {input, expected}
        Arguments.of(null, null), Arguments.of("   leading and trailing spaces   ", "leading and trailing spaces"), Arguments.of("\t\n  mixed whitespace \r\n", "mixed whitespace"), Arguments.of("noWhitespace", "noWhitespace"), Arguments.of("   ", ""));
    }

    private static Stream<Arguments> trimAdditionalEdgeCasesProvider() {
        return Stream.of(Arguments.of("", ""), // Non‑breaking space (U+00A0) is > 0x20, therefore not trimmed.
        Arguments.of("\u00A0\u00A0", "\u00A0\u00A0"), // Leading NBSP stays, trailing normal spaces are trimmed.
        Arguments.of("\u00A0  abc  ", "\u00A0  abc"), // Zero‑width space (U+200B) is not trimmed.
        Arguments.of("\u200Babc\u200B", "\u200Babc\u200B"), // Control character U+0001 is <= 0x20, trimmed from both ends.
        Arguments.of("\u0001abc\u0001", "abc"), // Mixed control characters (tab, vertical tab, unit separator) trimmed.
        Arguments.of("\t\u000B abc \u001F", "abc"), // Many spaces around a single character.
        Arguments.of("      a      ", "a"));
    }

    /**
     * Returns {@code true} if the supplied string contains any leading or trailing
     * characters that {@link String#trim()} would remove.  This is used to decide
     * when we can safely assert that the returned instance should be the same as
     * the input.
     */
    private static boolean needsTrimming(String s) {
        if (s == null)
            return false;
        int len = s.length();
        int start = 0;
        while (start < len && s.charAt(start) <= 0x20) start++;
        int end = len - 1;
        while (end >= start && s.charAt(end) <= 0x20) end--;
        return start != 0 || end != len - 1;
    }

    /**
     * Makes whitespace and non‑printable characters visible in assertion messages.
     */
    private static String escapeVisually(String s) {
        if (s == null)
            return "null";
        return s.replace("\t", "\\t").replace("\n", "\\n").replace("\r", "\\r").replace("\f", "\\f").replace("\b", "\\b").replace("\u000B", // vertical tab
        "\\v").replace("\u001F", // unit separator
        "\\u001F").replace("\u00A0", // non‑breaking space
        "\\u00A0").replace("\u200B", // zero‑width space
        "\\u200B");
    }

    @Test
    @DisplayName("trim(null) should return null")
    void trim_givenNull_returnsNull() {
        // given
        String input = null;
        // when
        String result = StringUtilities.trim(input);
        // then
        assertNull(result, "Trimming a null reference must return null");
    }

    @Test
    @DisplayName("trim(empty string) should return empty string")
    void trim_givenEmptyString_returnsEmptyString() {
        // given
        String input = "";
        // when
        String result = StringUtilities.trim(input);
        // then
        assertEquals("", result, "Trimming an empty string must return an empty string");
    }

    @Test
    @DisplayName("trim(string with only whitespace) should return empty string")
    void trim_givenOnlyWhitespace_returnsEmptyString() {
        // given
        String input = "   \t\n\r  ";
        // when
        String result = StringUtilities.trim(input);
        // then
        assertEquals("", result, "All‑whitespace string should be trimmed to an empty string");
    }

    @Test
    @DisplayName("trim(string with leading and trailing whitespace) should remove those whitespaces")
    void trim_givenLeadingAndTrailingWhitespace_removesWhitespace() {
        // given
        String input = "   hello world   ";
        // when
        String result = StringUtilities.trim(input);
        // then
        assertEquals("hello world", result, "Leading and trailing whitespace must be removed");
    }

    @Test
    @DisplayName("trim(string without surrounding whitespace) should return the same instance")
    void trim_givenNoSurroundingWhitespace_returnsSameString() {
        // given
        String input = "noSpaces";
        // when
        String result = StringUtilities.trim(input);
        // then
        assertSame(input, result, "If no trimming is required, the original string should be returned");
    }

    @ParameterizedTest(name = "[{index}] trim({0}) → {1}")
    @MethodSource("trimNullAndWhitespaceProvider")
    @DisplayName("trim(String) – null and whitespace handling")
    void trim_nullAndWhitespace_handling(String input, String expected) {
        String result = StringUtilities.trim(input);
        assertEquals(expected, result, () -> "trim(\"" + escapeVisually(input) + "\") should return \"" + escapeVisually(expected) + "\"");
        // When the method should not modify the string, verify that the same instance is returned.
        if (input != null && input.equals(expected) && !needsTrimming(input)) {
            assertSame(input, result, () -> "When no trimming is required, the original instance must be returned");
        }
    }

    @ParameterizedTest(name = "[{index}] trim({0}) → {1}")
    @MethodSource("trimAdditionalEdgeCasesProvider")
    @DisplayName("trim(String) – additional edge cases")
    void trim_additionalEdgeCases(String input, String expected) {
        String result = StringUtilities.trim(input);
        assertEquals(expected, result, () -> "trim(\"" + escapeVisually(input) + "\") should return \"" + escapeVisually(expected) + "\"");
        // For cases where trimming is a no‑op, ensure the same reference is returned.
        if (input != null && input.equals(expected) && !needsTrimming(input)) {
            assertSame(input, result, () -> "When no trimming occurs, the original instance must be returned");
        }
    }
}
