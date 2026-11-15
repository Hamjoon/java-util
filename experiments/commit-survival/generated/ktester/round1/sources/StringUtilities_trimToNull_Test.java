package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringUtilities_trimToNull_Test {

    /**
     * Supplies all test cases for {@link #trimToNull_cases(String, String, String)}.
     *
     * The data mirrors the JSON test cases provided in the task description.
     *
     * @return a stream of {@link Arguments}
     */
    static Stream<Arguments> trimToNullProvider() {
        return Stream.of(// null input
        Arguments.of(null, null, "When the input is null, trim() returns null and isEmpty(null) is true, so trimToNull should return null."), // empty string
        Arguments.of("", null, "Empty string is trimmed to empty; isEmpty(\"\") is true, therefore the method returns null."), // whitespace‑only (regular spaces)
        Arguments.of("   ", null, "String containing only whitespace is trimmed to empty; isEmpty after trim is true, so null is returned."), // leading and trailing regular spaces
        Arguments.of("  abc  ", "abc", "String with leading and trailing whitespace is trimmed to \"abc\"; isEmpty is false, so the trimmed value is returned."), // already trimmed, non‑empty
        Arguments.of("xyz", "xyz", "String without any surrounding whitespace remains unchanged; isEmpty is false, so the original string is returned."), // mixed whitespace characters (tab, newline, carriage return, form‑feed)
        Arguments.of("\t\n\r\f  mixed \t whitespace \n", "mixed \t whitespace", "String containing various whitespace characters is trimmed; internal whitespace is preserved, resulting in a non‑empty string that is returned."), // two non‑breaking spaces – not trimmed by String.trim()
        Arguments.of("\u00A0\u00A0", "\u00A0\u00A0", "String consisting solely of non‑breaking spaces (U+00A0) is not removed by String.trim, so isEmpty returns false and the original string is returned."), // non‑breaking spaces surrounding text – not trimmed
        Arguments.of("\u00A0abc\u00A0", "\u00A0abc\u00A0", "Non‑breaking spaces surrounding text are not trimmed; the method should return the unchanged string."), // regular spaces trimmed, non‑breaking spaces preserved
        Arguments.of(" \u00A0abc\u00A0 ", "\u00A0abc\u00A0", "Leading and trailing regular spaces are trimmed away, but inner non‑breaking spaces remain, resulting in \"\\u00A0abc\\u00A0\"."), // NUL characters (U+0000) are ≤ U+0020 and are stripped by trim()
        Arguments.of("\0\0abc\0\0", "abc", "Null characters (U+0000) are <= '\\u0020' and are stripped by trim; after trimming the string becomes \"abc\"."), // control characters U+0001 and U+0002 – trimmed away leaving empty string
        Arguments.of("\u0001\u0002", null, "Control characters U+0001 and U+0002 are trimmed away, leaving an empty string; isEmpty returns true, so null is returned."), // internal em‑space (U+2003) – not affected by trim()
        Arguments.of("abc\u2003def", "abc\u2003def", "The string contains an em‑space (U+2003) inside; trim does not affect internal characters, so the original string is returned."), // spaces trimmed, leaving a single non‑breaking space
        Arguments.of("   \u00A0   ", "\u00A0", "Spaces are trimmed, leaving a single non‑breaking space; since it is not empty, the method returns \"\\u00A0\"."));
    }

    @Test
    void trimToNull_givenNull_returnsNull() {
        assertNull(StringUtilities.trimToNull(null), "trimToNull(null) should return null");
    }

    @Test
    void trimToNull_givenEmptyString_returnsNull() {
        assertNull(StringUtilities.trimToNull(""), "trimToNull(\"\") should return null");
    }

    @Test
    void trimToNull_givenWhitespaceOnly_returnsNull() {
        assertNull(StringUtilities.trimToNull("   \t\n  "), "trimToNull(whitespace only) should return null");
    }

    @Test
    void trimToNull_givenStringWithLeadingAndTrailingSpaces_returnsTrimmedString() {
        String input = "   hello world   ";
        String expected = "hello world";
        assertEquals(expected, StringUtilities.trimToNull(input), "trimToNull should trim surrounding whitespace and return the trimmed value");
    }

    @Test
    void trimToNull_givenAlreadyTrimmedNonEmptyString_returnsSameString() {
        String input = "foobar";
        assertEquals(input, StringUtilities.trimToNull(input), "trimToNull should return the original string when it is already trimmed and non‑empty");
    }

    @Test
    void trimToNull_givenStringThatBecomesEmptyAfterTrim_returnsNull() {
        String input = "   \t  ";
        assertNull(StringUtilities.trimToNull(input), "trimToNull should return null when trimming results in an empty string");
    }

    /**
     * Parameterized test that executes every scenario from the JSON specification.
     *
     * @param input       the value passed to {@code trimToNull}
     * @param expected    the expected return value (null or trimmed string)
     * @param description human‑readable description of the scenario
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("trimToNullProvider")
    void trimToNull_cases(String input, String expected, String description) {
        assertEquals(expected, StringUtilities.trimToNull(input), description);
    }
}
