package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class StringUtilities_hasContent_Test {

    /* --------------------------------------------------------------------- *
     *  Helper to make invisible characters visible in assertion messages.
     * --------------------------------------------------------------------- */
    private static String printable(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t").replace("\u00A0", "\\u00A0").replace("\u2003", "\\u2003").replace("\u200B", "\\u200B").replace("\u2028", "\\u2028").replace("\u2029", "\\u2029").replace("\u3000", "\\u3000").replace("\u180E", "\\u180E").replace("\u000B", "\\u000B").replace("\u000C", "\\u000C").replace("\u0000", "\\u0000");
    }

    /**
     * Checks if a {@link CharSequence} is {@code null}, empty, or contains only
     * whitespace characters.
     *
     * @param cs the character sequence to test; may be {@code null}
     * @return {@code true} if the sequence is {@code null}, has length zero,
     *         or consists solely of whitespace; {@code false} otherwise.
     */
    public static boolean isEmpty(CharSequence cs) {
        if (cs == null) {
            return true;
        }
        int len = cs.length();
        if (len == 0) {
            return true;
        }
        for (int i = 0; i < len; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns {@code true} if the supplied {@link CharSequence} is not {@code null},
     * not empty and contains at least one non‑whitespace character.
     *
     * @param s the character sequence to check; may be {@code null}
     * @return {@code true} if the sequence has visible content; {@code false}
     *         otherwise.
     */
    public static boolean hasContent(CharSequence s) {
        // Delegating to the already‑tested {@code isEmpty} method keeps the logic
        // in a single place and guarantees consistent behaviour.
        return !isEmpty(s);
    }

    private static Stream<Arguments> stringsWithContent() {
        return Stream.of(Arguments.of("a"), Arguments.of(" abc "), Arguments.of("0"), Arguments.of("!@#$"), Arguments.of("\u00A0x"), Arguments.of("中文"), Arguments.of(" \t\nx\r"));
    }

    private static Stream<Arguments> stringsWithOnlyWhitespace() {
        return Stream.of(Arguments.of(" "), Arguments.of("\t"), Arguments.of("\n"), Arguments.of("\r\n"), Arguments.of(" \t \n "), Arguments.of("\u2003"));
    }

    private static Stream<Arguments> positiveCases() {
        return Stream.of(Arguments.of("a"), Arguments.of(" a "), Arguments.of("0"), Arguments.of("!@#$"), Arguments.of("中文"), Arguments.of(" \t\nx\r"), Arguments.of(" \u2003 a"), Arguments.of("\u200b"), Arguments.of("\u200b😀"), Arguments.of(" \u200b "), Arguments.of("\t\u200b\n"), Arguments.of("\u00A0x"), Arguments.of("\u200b\u200b"), Arguments.of("\u2003\u00A0\u2028a"), Arguments.of("\u0000"), Arguments.of("\u0000\u0001\u0002"), Arguments.of("\uD83D\uDE00"));
    }

    private static Stream<Arguments> negativeWhitespaceOnly() {
        return Stream.of(Arguments.of(" "), Arguments.of("\t"), Arguments.of("\n"), Arguments.of("\r\n"), Arguments.of(" \t \n "), Arguments.of("\u2003"), Arguments.of("\u00A0"), Arguments.of("\u2028"), Arguments.of("\u2029"), Arguments.of("\u3000"), Arguments.of("\u180E"), Arguments.of("\u000B"), Arguments.of("\u000C"));
    }

    /* --------------------------------------------------------------------- *
     *  Positive cases – strings that contain at least one non‑whitespace
     *  character should cause hasContent() to return true.
     * --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("stringsWithContent")
    void testHasContent_whenStringHasNonWhitespace_returnsTrue(String input) {
        assertTrue(StringUtilities.hasContent(input), () -> "Expected true for input: [" + printable(input) + "]");
    }

    /* --------------------------------------------------------------------- *
     *  Negative cases – null, empty or whitespace‑only strings should cause
     *  hasContent() to return false.
     * --------------------------------------------------------------------- */
    @ParameterizedTest
    @NullAndEmptySource
    void testHasContent_whenNullOrEmpty_returnsFalse(String input) {
        assertFalse(StringUtilities.hasContent(input), () -> "Expected false for null/empty input");
    }

    @ParameterizedTest
    @MethodSource("stringsWithOnlyWhitespace")
    void testHasContent_whenOnlyWhitespace_returnsFalse(String input) {
        assertFalse(StringUtilities.hasContent(input), () -> "Expected false for whitespace‑only input: [" + printable(input) + "]");
    }

    /* --------------------------------------------------------------------- *
     *  Defensive test – ensure the method does not throw for unexpected
     *  characters (e.g., control characters) and still follows the contract.
     * --------------------------------------------------------------------- */
    @Test
    void testHasContent_withControlCharacters() {
        String input = "\u0000\u0001\u0002";
        // Control characters are not considered whitespace by StringUtilities.isEmpty,
        // therefore hasContent should return true.
        assertTrue(StringUtilities.hasContent(input), "Control characters that are not whitespace should yield true");
    }

    /* --------------------------------------------------------------------- *
     *  Additional positive cases.
     * --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("positiveCases")
    void testHasContent_positive(String input) {
        assertTrue(StringUtilities.hasContent(input), () -> "Expected true for input: [" + printable(input) + "]");
    }

    /* --------------------------------------------------------------------- *
     *  Additional negative cases.
     * --------------------------------------------------------------------- */
    @ParameterizedTest
    @NullAndEmptySource
    void testHasContent_nullOrEmpty(String input) {
        assertFalse(StringUtilities.hasContent(input), "Expected false for null or empty input");
    }

    @ParameterizedTest
    @MethodSource("negativeWhitespaceOnly")
    void testHasContent_whitespaceOnly(String input) {
        assertFalse(StringUtilities.hasContent(input), () -> "Expected false for whitespace‑only input: [" + printable(input) + "]");
    }
}
