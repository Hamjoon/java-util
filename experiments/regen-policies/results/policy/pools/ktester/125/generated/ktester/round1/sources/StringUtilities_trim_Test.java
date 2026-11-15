package com.cedarsoftware.util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

// retained for consistency with the template; not used here
@ExtendWith(MockitoExtension.class)
class StringUtilities_trim_Test {

    /**
     * Whitespace‑only strings (ASCII whitespace characters).
     */
    private static Stream<String> whitespaceOnlyProvider() {
        return Stream.of(" ", "\t", "\n", "\r", "   \t\n\r  ");
    }

    /**
     * Normal strings that may have leading/trailing whitespace but are otherwise simple.
     */
    private static Stream<String> normalStringProvider() {
        return Stream.of(// no whitespace
        "abc", // leading space
        " abc", // trailing space
        "abc ", // both sides
        " abc ", // mixed whitespace characters
        "\t\nabc\r\n", // interior spaces must stay
        "  a b c  ");
    }

    /**
     * Provider for null and empty values used by the combined test.
     */
    private static Stream<String> nullOrEmptyProvider() {
        return Stream.of(null, "");
    }

    /**
     * Provides a set of inputs together with the exact expected trimmed result.
     * The cases correspond to the JSON matrix under the groups
     * “StringUtilities.trim(String) - branch coverage” and
     * “StringUtilities.trim(String) - additional cases”.
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> trimValueProvider() {
        return Stream.of(// null input
        org.junit.jupiter.params.provider.Arguments.of(null, null), // empty string
        org.junit.jupiter.params.provider.Arguments.of("", ""), // only ASCII whitespace (already covered by whitespaceOnlyProvider but kept for completeness)
        org.junit.jupiter.params.provider.Arguments.of("   ", ""), // leading/trailing spaces
        org.junit.jupiter.params.provider.Arguments.of("  hello world  ", "hello world"), // newline & tab at both ends
        org.junit.jupiter.params.provider.Arguments.of("\n\tfoo\t\n", "foo"), // internal spaces only – result should be identical
        org.junit.jupiter.params.provider.Arguments.of("a b c", "a b c"), // non‑breaking space (U+00A0) is NOT trimmed
        org.junit.jupiter.params.provider.Arguments.of("\u00A0test\u00A0", "\u00A0test\u00A0"), // leading/trailing null character (U+0000) – should be trimmed
        org.junit.jupiter.params.provider.Arguments.of("\u0000abc\u0000", "abc"), // mixed ASCII space and NBSP – ASCII spaces trimmed, NBSP retained
        org.junit.jupiter.params.provider.Arguments.of(" \u00A0foo\u00A0 ", "\u00A0foo\u00A0"), // zero‑width space (U+200B) – not considered whitespace by String.trim()
        org.junit.jupiter.params.provider.Arguments.of("\u200bhello\u200b", "\u200bhello\u200b"), // control characters U+0009 – U+001F (all <= 32) – should all be trimmed
        org.junit.jupiter.params.provider.Arguments.of("\t\n\u000B\f\r\u001C\u001D\u001E\u001F", ""), // surrogate‑pair characters with internal spaces – no trimming needed
        org.junit.jupiter.params.provider.Arguments.of("𠜎 test 𠜎", "𠜎 test 𠜎"));
    }

    /**
     * Strings that have no leading or trailing characters <= 32, therefore
     * {@link String#trim()} returns the original reference.  These are the cases
     * where we assert {@code assertSame}.
     */
    private static Stream<String> sameInstanceProvider() {
        return Stream.of(// simple ASCII without surrounding whitespace
        "abc", // contains surrogate pair but no trim‑eligible edges
        "𠜎 test 𠜎");
    }

    /**
     * Makes invisible characters printable for assertion messages.
     */
    private static String escape(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\t", "\\t").replace("\n", "\\n").replace("\r", "\\r").replace("\f", "\\f").replace("\b", "\\b").replace("\u0000", "\\u0000").replace("\u00A0", "\\u00A0").replace("\u200B", "\\u200B");
    }

    /**
     * {@code trim(null)} must return {@code null}.
     */
    @Test
    void trim_nullInput_returnsNull() {
        assertNull(StringUtilities.trim(null), "trim(null) should return null");
    }

    /**
     * {@code trim("")} must return an empty string.
     */
    @Test
    void trim_emptyString_returnsEmptyString() {
        assertEquals("", StringUtilities.trim(""), "trim(\"\") should return an empty string");
    }

    /**
     * Strings that contain only whitespace characters are trimmed to an empty string.
     */
    @ParameterizedTest
    @ValueSource(strings = { " ", "\t", "\n", "\r", "   \t\n\r  " })
    void trim_onlyWhitespace_returnsEmptyString(String whitespace) {
        assertEquals("", StringUtilities.trim(whitespace), () -> "trim(\"" + whitespace + "\") should return an empty string");
    }

    /**
     * Strings with leading and/or trailing whitespace should have that whitespace removed,
     *  while preserving interior whitespace.
     */
    @ParameterizedTest
    @ValueSource(strings = { // no whitespace
    "abc", // leading space
    " abc", // trailing space
    "abc ", // both sides
    " abc ", // mixed whitespace characters
    "\t\nabc\r\n", // interior spaces must stay
    "  a b c  " })
    void trim_normalStrings_trimsOnlyEdges(String input) {
        // delegate to JDK for expected result
        String expected = input.trim();
        assertEquals(expected, StringUtilities.trim(input), () -> "trim(\"" + input + "\") should behave like String.trim()");
    }

    /**
     * Null and empty values together (provided by JUnit) should both result in the same
     *  behaviour as the dedicated tests above.
     */
    @ParameterizedTest
    @NullAndEmptySource
    void trim_nullOrEmpty_returnsSameValue(String value) {
        // For null we expect null; for empty we expect empty.
        String expected = (value == null) ? null : "";
        assertEquals(expected, StringUtilities.trim(value), () -> "trim on " + (value == null ? "null" : "\"\"") + " should return " + expected);
    }

    /**
     * Verify that {@code trim} returns the expected string value for a variety of
     * inputs, including control characters, non‑breaking spaces, zero‑width spaces,
     * and surrogate‑pair characters.
     */
    @ParameterizedTest
    @MethodSource("trimValueProvider")
    void trim_returnsExpectedValue(String input, String expected) {
        assertEquals(expected, StringUtilities.trim(input), () -> "trim(\"" + escape(input) + "\") expected \"" + escape(expected) + "\"");
    }

    /**
     * When the input string does not have any leading or trailing characters
     * that {@link String#trim()} would remove, the method must return the *same*
     * instance (i.e., {@code ==}) as the argument.
     */
    @ParameterizedTest
    @MethodSource("sameInstanceProvider")
    void trim_returnsSameInstanceWhenNoTrimNeeded(String input) {
        String result = StringUtilities.trim(input);
        assertSame(input, result, () -> "trim should return the same instance for input \"" + escape(input) + "\"");
    }
}
