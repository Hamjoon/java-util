package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringUtilities_wildcardToRegexString_Test {

    /**
     * Provides all normal (non‑null) test cases from the supplied JSON.
     * Each argument consists of:
     *   - a description (used only for readability in the source)
     *   - the wildcard input string
     *   - the expected regex result
     */
    private static Stream<Arguments> wildcardToRegexProvider() {
        return Stream.of(// Empty input
        Arguments.of("Empty input should produce a regex that matches only the empty string.", "", "^$"), // Simple literal
        Arguments.of("String with only ordinary characters passes through unchanged.", "abc", "^abc$"), // Single asterisk
        Arguments.of("A single asterisk is translated to the regex '.*' (any sequence of characters).", "*", "^.*$"), // Single question mark
        Arguments.of("A single question mark is translated to the regex '.' (any single character).", "?", "^.$"), // Escaped special characters
        Arguments.of("Opening parenthesis is a special regex character and must be escaped.", "(", "^\\($"), Arguments.of("Closing parenthesis is escaped.", ")", "^\\)$"), Arguments.of("Opening square bracket is escaped.", "[", "^\\[$"), Arguments.of("Closing square bracket is escaped.", "]", "^\\]$"), Arguments.of("Dollar sign is escaped because it has special meaning in regex.", "$", "^\\$$"), Arguments.of("Caret is escaped.", "^", "^\\^$"), Arguments.of("Dot is escaped.", ".", "^\\.$"), Arguments.of("Opening curly brace is escaped.", "{", "^\\{$"), Arguments.of("Closing curly brace is escaped.", "}", "^\\}$"), Arguments.of("Vertical bar is escaped.", "|", "^\\|$"), Arguments.of("Backslash itself must be escaped, resulting in two backslashes in the regex.", "\\", "^\\\\$"), // Mixed ordinary and wildcards
        Arguments.of("Combination of ordinary characters, '*', and '?' to test mixed handling.", "a*b?c", "^a.*b.c$"), // Full escape logic with all special chars plus wildcards
        Arguments.of("String containing every special character that requires escaping plus '*' and '?' to verify full escape logic.", ".*?^$", "^\\..*\\.\\^\\$$"), // Real‑world filename pattern with brackets and dot
        Arguments.of("Real‑world filename pattern with brackets and a dot; brackets and dot must be escaped.", "file[0].txt", "^file\\[0\\]\\.txt$"), // Numeric character
        Arguments.of("Numeric character falls into the default branch (no escaping).", "5", "^5$"), // Backslash followed by asterisk
        Arguments.of("Backslash followed by asterisk – the backslash must be escaped and the asterisk turned into \"*\".", "\\*", "^\\\\\\\\.*$"), // Consecutive wildcards
        Arguments.of("Consecutive wildcards: two '*', two '?', then two '*'. Verifies that the method does not collapse repeated wildcards.", "**??**", "^.*.*..*.*$"), // Characters not special to the method
        Arguments.of("String containing characters that are not special to the method (+, -). They should pass through unchanged.", "a+b-c", "^a+b-c$"), // Unicode character
        Arguments.of("Unicode characters (é) are treated as ordinary characters.", "café", "^café$"), // Bracket with asterisk inside
        Arguments.of("Bracket characters are escaped while the asterisk inside is converted to \"*\".", "[*]", "^\\\\\\[.*\\\\\\]$"), // Dollar sign followed by digit
        Arguments.of("Dollar sign must be escaped; following digits are ordinary.", "$1", "^\\\\\\$1$"), // Caret at the beginning
        Arguments.of("Caret at the beginning is escaped.", "^abc", "^\\\\\\^abc$"), // Dot inside a string
        Arguments.of("Dot inside a string is escaped.", "a.b", "^a\\\\.b$"), // Curly braces and dot
        Arguments.of("Curly braces and a dot are escaped while other characters stay unchanged.", "file{1}.txt", "^file\\\\{1\\\\}\\\\.txt$"), // Vertical bar
        Arguments.of("Vertical bar is escaped.", "a|b", "^a\\\\|b$"), // Trailing backslash
        Arguments.of("Trailing backslash is escaped (produces two backslashes in the regex).", "path\\", "^path\\\\\\\\$"), // Two consecutive backslashes
        Arguments.of("Two consecutive backslashes are each escaped, resulting in four backslashes in the final regex.", "C:\\\\temp", "^C:\\\\\\\\\\\\temp$"));
    }

    @Test
    void shouldReturnExactRegexForLiteralString() {
        String input = "HelloWorld";
        String expected = "^HelloWorld$";
        assertEquals(expected, StringUtilities.wildcardToRegexString(input));
    }

    @Test
    void shouldConvertAsteriskToDotStar() {
        String input = "file*.txt";
        String expected = "^file.*\\.txt$";
        assertEquals(expected, StringUtilities.wildcardToRegexString(input));
    }

    @Test
    void shouldConvertQuestionMarkToDot() {
        String input = "data?.csv";
        String expected = "^data.\\.csv$";
        assertEquals(expected, StringUtilities.wildcardToRegexString(input));
    }

    @Test
    void shouldEscapeAllRegexSpecialCharacters() {
        String input = "a(b)c[d]e$f^g.h{i}j|k\\l";
        // Each special character is prefixed with a backslash.
        String expected = "^a\\(b\\)c\\[d\\]e\\$f\\^g\\.h\\{i\\}j\\|k\\\\l$";
        assertEquals(expected, StringUtilities.wildcardToRegexString(input));
    }

    @Test
    void shouldHandleEmptyString() {
        String input = "";
        String expected = "^$";
        assertEquals(expected, StringUtilities.wildcardToRegexString(input));
    }

    @Test
    void shouldHandleStringContainingOnlyWildcards() {
        String input = "*?*";
        String expected = "^.*..*$";
        assertEquals(expected, StringUtilities.wildcardToRegexString(input));
    }

    @Test
    void shouldConvertComplexMixedPattern() {
        String input = "log_??-*.{log|txt}";
        // Breakdown:
        //   log_          -> literal
        //   ??            -> two '.' characters
        //   -             -> literal '-'
        //   *             -> ".*"
        //   .             -> escaped "\."
        //   {log|txt}     -> each special char escaped, '|' escaped as "\|"
        String expected = "^log_..-.*\\.\\{log\\|txt\\}$";
        assertEquals(expected, StringUtilities.wildcardToRegexString(input));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("wildcardToRegexProvider")
    void shouldConvertWildcardToRegex(String description, String wildcard, String expectedRegex) {
        // Act
        String actual = StringUtilities.wildcardToRegexString(wildcard);
        // Assert
        assertEquals(expectedRegex, actual, description);
    }

    /**
     * Null input test – the implementation calls {@code wildcard.length()},
     * which throws a {@link NullPointerException}. This test verifies that behaviour.
     */
    @Test
    void shouldThrowNullPointerExceptionWhenWildcardIsNull() {
        assertThrows(NullPointerException.class, () -> StringUtilities.wildcardToRegexString(null), "Passing null should result in a NullPointerException because the method calls wildcard.length().");
    }
}
