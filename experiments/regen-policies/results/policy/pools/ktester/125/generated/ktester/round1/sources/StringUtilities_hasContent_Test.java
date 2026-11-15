package com.cedarsoftware.util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

// No mocks are required for this class, but the annotation is kept
@ExtendWith(MockitoExtension.class)
class StringUtilities_hasContent_Test {

    /* --------------------------------------------------------------------- *
     *  Method sources used by the parameterized tests above.                *
     * --------------------------------------------------------------------- */
    private static Stream<String> stringsWithAllWhitespace() {
        return Stream.of(// single regular space
        " ", // horizontal tab
        "\t", // line feed
        "\n", // carriage‑return + line feed
        "\r\n", // mixed whitespace
        " \t \n ", // EM SPACE (Unicode whitespace)
        "\u2003", // IDEOGRAPHIC SPACE (Unicode whitespace)
        "\u3000", // LINE SEPARATOR (Unicode whitespace)
        "\u2028", // PARAGRAPH SEPARATOR (Unicode whitespace)
        "\u2029", // FORM FEED (U+000C)
        "\f", // VERTICAL TAB (U+000B)
        "\u000B");
    }

    private static Stream<String> stringsWithContentOtherThanWhitespace() {
        return Stream.of(// single non‑whitespace character
        "a", // surrounded by whitespace
        "  a  ", // typical sentence
        "Hello, World!", // whitespace with a character at the end
        "\t\nx\r", // numeric character
        "0", // the word “null”
        "null", // non‑breaking space plus a character
        "   \u00A0a", // emoji
        "😀", // multiple lines with content
        "text\nwith\rnewlines", // leading whitespace only
        "   leading", // trailing whitespace only
        "trailing   ");
    }

    /**
     * Strings that contain at least one character that {@link Character#isWhitespace(int)}
     * does <em>not</em> recognise as whitespace.  These correspond to the “true”
     * expectations in the supplied JSON test cases.
     */
    private static Stream<String> stringsWithContent() {
        return Stream.of(// single non‑whitespace character
        "a", // surrounded by whitespace
        "  a  ", // typical sentence
        "Hello, World!", // whitespace with a character at the end
        "\t\nx\r", // numeric character
        "0", // the word “null”
        "null", // NON‑BREAKING SPACE (U+00A0) – not whitespace
        "\u00A0", // ZERO‑WIDTH SPACE (U+200B) – not whitespace
        "\u200b", // emoji
        "😀", // multiple lines with content
        "text\nwith\rnewlines", // leading whitespace only
        "   leading", // trailing whitespace only
        "trailing   ", // NBSP plus a character
        "   \u00A0a", // regular space, NBSP, and tab
        " \u00A0\t", // spaces and multiple NBSPs
        " \u00A0\u00A0 ", // punctuation
        ".", // two zero‑width spaces
        "\u200b\u200b", // mix of regular space, NBSP, zero‑width, tab
        " \u00A0\u200b\t", // non‑whitespace surrounded by EM SPACE (still content)
        "a\u2003b", // emoji followed by whitespace
        "😀\u2003", // space, zero‑width, space (zero‑width gives content)
        " \u200b ", // line separator followed by a character
        "\u2028a", // paragraph separator followed by a character
        "\u2029b");
    }

    /* --------------------------------------------------------------------- *
     *  hasContent should return false for null, empty, or whitespace‑only *
     *  strings.                                                             *
     * --------------------------------------------------------------------- */
    @ParameterizedTest
    @NullAndEmptySource
    void hasContent_nullOrEmpty_returnsFalse(String s) {
        assertFalse(StringUtilities.hasContent(s), () -> "hasContent should be false for null or empty string, but got true for: " + s);
    }

    @ParameterizedTest
    @MethodSource("stringsWithAllWhitespace")
    void hasContent_whitespaceOnly_returnsFalse(String s) {
        assertFalse(StringUtilities.hasContent(s), () -> "hasContent should be false for whitespace‑only string, but got true for: [" + s + "]");
    }

    /* --------------------------------------------------------------------- *
     *  hasContent should return true for any string that contains at least *
     *  one non‑whitespace character.                                        *
     * --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("stringsWithContentOtherThanWhitespace")
    void hasContent_withContent_returnsTrue(String s) {
        assertTrue(StringUtilities.hasContent(s), () -> "hasContent should be true for string with content, but got false for: [" + s + "]");
    }
}
