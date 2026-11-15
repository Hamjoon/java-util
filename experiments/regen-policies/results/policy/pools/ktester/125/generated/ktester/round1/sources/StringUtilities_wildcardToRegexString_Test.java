package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class StringUtilities_wildcardToRegexString_Test {

    private static Stream<Arguments> wildcardAndExpectedProvider() {
        return Stream.of(Arguments.of("*", "^.*$"), Arguments.of("file?.txt", "^file.\\.txt$"), Arguments.of("data*202?.csv", "^data.*202.\\.csv$"), // spaces are just for readability; actual string is "^..*$"
        Arguments.of("?*?", "^. .*.$"));
    }

    private static Stream<Arguments> emptyInputProvider() {
        return Stream.of(Arguments.of("", "^$"));
    }

    private static Stream<Arguments> asteriskAndQuestionMarkProvider() {
        return Stream.of(Arguments.of("*", "^.*$"), Arguments.of("?", "^.$"), Arguments.of("*?*", "^.*..*$"));
    }

    private static Stream<Arguments> escapingSpecialCharsProvider() {
        return Stream.of(// parentheses
        Arguments.of("(test)", "^\\(test\\)$"), // square brackets
        Arguments.of("[a]", "^\\[a\\]$"), // dollar, caret and dot
        Arguments.of("$^.", "^\\$\\^\\.$"), // braces, pipe and backslash
        Arguments.of("{a}|b\\c", "^\\{a\\}\\|b\\\\c$"));
    }

    private static Stream<Arguments> mixedCharactersProvider() {
        return Stream.of(// literal dash, ?, escaped dot and *
        Arguments.of("file-?.*", "^file-\\.\\..*$"));
    }

    private static Stream<Arguments> literalOnlyProvider() {
        return Stream.of(Arguments.of("abcXYZ123", "^abcXYZ123$"));
    }

    private static Stream<Arguments> backslashEscapingProvider() {
        return Stream.of(// a single backslash must become "\\" in the regex (escaped backslash)
        Arguments.of("\\", "^\\\\\\\\$"));
    }

    private static Stream<Arguments> multipleConsecutiveWildcardsProvider() {
        return Stream.of(Arguments.of("*?*?*", "^.*..*..*$"));
    }

    private static Stream<Arguments> leadingTrailingWildcardsProvider() {
        return Stream.of(Arguments.of("*test*", "^.*test.*$"), Arguments.of("?test?", "^.test.$"));
    }

    private static Stream<Arguments> unicodeWhitespaceProvider() {
        return Stream.of(// Unicode character and spaces are treated as literals
        Arguments.of("é * ?", "^é .* .$"));
    }

    private static Stream<Arguments> nonEscapedMetaCharsProvider() {
        return Stream.of(// '+' is not escaped by the method, '|' is escaped
        Arguments.of("a+b|c", "^a+b\\|c$"));
    }

    private static Stream<Arguments> complexMixedPatternProvider() {
        return Stream.of(// combination of escaped parentheses, escaped pipe, asterisk, escaped dot
        Arguments.of("(a|b)*.txt", "^\\(a\\|b\\).*\\.txt$"));
    }

    /**
     * Verify that ordinary characters are copied unchanged and that the start (^) and end ($)
     * anchors are added.
     */
    @Test
    void wildcardToRegexString_simpleString() {
        String input = "abc123";
        String expected = "^abc123$";
        assertEquals(expected, StringUtilities.wildcardToRegexString(input));
    }

    /**
     * Verify handling of the wildcard characters '*' (zero or more) and '?' (single).
     */
    @ParameterizedTest
    @MethodSource("wildcardAndExpectedProvider")
    void wildcardToRegexString_wildcards(String wildcard, String expectedRegex) {
        assertEquals(expectedRegex, StringUtilities.wildcardToRegexString(wildcard));
    }

    /**
     * Verify that all regex‑special characters are escaped correctly.
     */
    @Test
    void wildcardToRegexString_escapesSpecialRegexChars() {
        String wildcard = "a(b)c[d]e$f^g.h{i}j|k\\l";
        // Expected: each special char preceded by a backslash, plus start/end anchors
        String expected = "^a\\(b\\)c\\[d\\]e\\$f\\^g\\.h\\{i\\}j\\|k\\\\l$";
        assertEquals(expected, StringUtilities.wildcardToRegexString(wildcard));
    }

    /**
     * Verify that an empty string yields a regex that matches only the empty string.
     */
    @Test
    void wildcardToRegexString_emptyString() {
        assertEquals("^$", StringUtilities.wildcardToRegexString(""));
    }

    /**
     * The implementation does not guard against {@code null}. It should throw a
     * {@link NullPointerException} when a null argument is supplied.
     */
    @Test
    void wildcardToRegexString_nullArgument_throwsNPE() {
        assertThrows(NullPointerException.class, () -> StringUtilities.wildcardToRegexString(null));
    }

    /* -------------------------------------------------------------
     *  Group: Empty input
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] wildcard=''{0}'' → regex=''{1}''")
    @MethodSource("emptyInputProvider")
    void wildcardToRegexString_emptyInput(String wildcard, String expected) {
        assertEquals(expected, StringUtilities.wildcardToRegexString(wildcard));
    }

    /* -------------------------------------------------------------
     *  Group: Asterisk and Question Mark
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] wildcard=''{0}'' → regex=''{1}''")
    @MethodSource("asteriskAndQuestionMarkProvider")
    void wildcardToRegexString_asteriskAndQuestionMark(String wildcard, String expected) {
        assertEquals(expected, StringUtilities.wildcardToRegexString(wildcard));
    }

    /* -------------------------------------------------------------
     *  Group: Mixed characters
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] wildcard=''{0}'' → regex=''{1}''")
    @MethodSource("mixedCharactersProvider")
    void wildcardToRegexString_mixedCharacters(String wildcard, String expected) {
        assertEquals(expected, StringUtilities.wildcardToRegexString(wildcard));
    }

    /* -------------------------------------------------------------
     *  Group: Literal characters only
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] wildcard=''{0}'' → regex=''{1}''")
    @MethodSource("literalOnlyProvider")
    void wildcardToRegexString_literalOnly(String wildcard, String expected) {
        assertEquals(expected, StringUtilities.wildcardToRegexString(wildcard));
    }

    /* -------------------------------------------------------------
     *  Group: Backslash escaping
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] wildcard=''{0}'' → regex=''{1}''")
    @MethodSource("backslashEscapingProvider")
    void wildcardToRegexString_backslashEscaping(String wildcard, String expected) {
        assertEquals(expected, StringUtilities.wildcardToRegexString(wildcard));
    }

    /* -------------------------------------------------------------
     *  Group: Multiple consecutive wildcards
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] wildcard=''{0}'' → regex=''{1}''")
    @MethodSource("multipleConsecutiveWildcardsProvider")
    void wildcardToRegexString_multipleConsecutiveWildcards(String wildcard, String expected) {
        assertEquals(expected, StringUtilities.wildcardToRegexString(wildcard));
    }

    /* -------------------------------------------------------------
     *  Group: Leading and trailing wildcards
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] wildcard=''{0}'' → regex=''{1}''")
    @MethodSource("leadingTrailingWildcardsProvider")
    void wildcardToRegexString_leadingTrailingWildcards(String wildcard, String expected) {
        assertEquals(expected, StringUtilities.wildcardToRegexString(wildcard));
    }

    /* -------------------------------------------------------------
     *  Group: Unicode and whitespace handling
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] wildcard=''{0}'' → regex=''{1}''")
    @MethodSource("unicodeWhitespaceProvider")
    void wildcardToRegexString_unicodeAndWhitespace(String wildcard, String expected) {
        assertEquals(expected, StringUtilities.wildcardToRegexString(wildcard));
    }

    /* -------------------------------------------------------------
     *  Group: Non‑escaped regex meta characters
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] wildcard=''{0}'' → regex=''{1}''")
    @MethodSource("nonEscapedMetaCharsProvider")
    void wildcardToRegexString_nonEscapedMetaChars(String wildcard, String expected) {
        assertEquals(expected, StringUtilities.wildcardToRegexString(wildcard));
    }

    /* -------------------------------------------------------------
     *  Group: Complex mixed pattern
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] wildcard=''{0}'' → regex=''{1}''")
    @MethodSource("complexMixedPatternProvider")
    void wildcardToRegexString_complexMixedPattern(String wildcard, String expected) {
        assertEquals(expected, StringUtilities.wildcardToRegexString(wildcard));
    }
}
