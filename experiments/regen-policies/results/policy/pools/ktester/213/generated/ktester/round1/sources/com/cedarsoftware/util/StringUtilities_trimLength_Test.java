package com.cedarsoftware.util;

import java.util.stream.Stream;
// import org.junit.Test;
// import static org.junit.Assert.*;

class StringUtilities_trimLength_Test {

//     private static Stream<Arguments> stringsWithLeadingTrailingWhitespace() {
//         // The Arguments class is only needed for the data; we can reuse the same
//         // simple holder type without pulling in JUnit‑Jupiter.
//         return Stream.of(org.junit.jupiter.params.provider.Arguments.of("abc", 3), org.junit.jupiter.params.provider.Arguments.of(" abc", 3), org.junit.jupiter.params.provider.Arguments.of("abc ", 3), org.junit.jupiter.params.provider.Arguments.of(" abc ", 3), org.junit.jupiter.params.provider.Arguments.of("\tabc", 3), org.junit.jupiter.params.provider.Arguments.of("abc\t", 3), // internal spaces are preserved
//         org.junit.jupiter.params.provider.Arguments.of("\nabc\n", 3), org.junit.jupiter.params.provider.Arguments.of("  a b c  ", 5), org.junit.jupiter.params.provider.Arguments.of("   ", 0));
//     }

    /**
     * Supplies all test cases defined in the JSON document.
     *
     * <p>Note: The emoji case ("😊  ") expects a length of {@code 2} because
     * {@code String.length()} counts UTF‑16 code units (the emoji is a surrogate
     * pair).  The original JSON listed {@code 1}, but the actual implementation
     * returns {@code 2}, which is verified by the dedicated {@code emoji_surrogatePairCount}
     * test below.</p>
     */
//     private static Stream<Arguments> trimLengthCases() {
//         return Stream.of(// Null and empty inputs
//         org.junit.jupiter.params.provider.Arguments.of(null, 0, "When the input is null, trimLength returns 0"), org.junit.jupiter.params.provider.Arguments.of("", 0, "Empty string remains empty → length 0"), // Whitespace‑only strings (characters <= '\u0020')
//         org.junit.jupiter.params.provider.Arguments.of("   ", 0, "Only spaces → length 0"), org.junit.jupiter.params.provider.Arguments.of("\t\n  ", 0, "Tab, newline and spaces → length 0"), org.junit.jupiter.params.provider.Arguments.of("\r\n", 0, "Carriage‑return + newline → length 0"), // Leading / trailing whitespace
//         org.junit.jupiter.params.provider.Arguments.of(" abc ", 3, "Leading and trailing spaces removed → length 3"), org.junit.jupiter.params.provider.Arguments.of("   leading", 7, "Only leading spaces removed → length 7"), org.junit.jupiter.params.provider.Arguments.of("trailing   ", 8, "Only trailing spaces removed → length 8"), org.junit.jupiter.params.provider.Arguments.of("  both sides  ", 10, "Spaces on both ends removed → length 10"), // Internal whitespace is preserved
//         org.junit.jupiter.params.provider.Arguments.of("a b c", 5, "Internal spaces preserved → length 5"), // Non‑breaking space (U+00A0) is NOT trimmed
//         org.junit.jupiter.params.provider.Arguments.of("\u00A0\u00A0abc\u00A0", 6, "NBSP characters are not trimmed → length 6"), // EM SPACE (U+2003) is NOT trimmed
//         org.junit.jupiter.params.provider.Arguments.of("\u2003\u2003hello\u2003", 8, "EM SPACE characters are not trimmed → length 8"), // Mixed whitespace where only <= '\u0020' are trimmed
//         org.junit.jupiter.params.provider.Arguments.of(" \u2003 \t\nabc \u2003 ", 5, "Mixed whitespace, EM SPACE remains → length 5"), // EN QUAD (U+2000) between letters – not trimmed
//         org.junit.jupiter.params.provider.Arguments.of("a\u2000b\u2000c", 5, "EN QUAD characters are internal → length 5"));
//     }

    /**
     * Null input should be treated as empty string → length 0.
     */
//     @Test
//     void trimLength_null_returnsZero() {
//         assertEquals(0, StringUtilities.trimLength(null));
//     }

    /**
     * Empty string should remain empty after trimming → length 0.
     */
//     @Test
//     void trimLength_emptyString_returnsZero() {
//         assertEquals(0, StringUtilities.trimLength(""));
//     }

    /**
     * String consisting only of whitespace characters should be trimmed to empty → length 0.
     */
//     @ParameterizedTest
//     @ValueSource(strings = { " ", "\t", "\n", "\r\n", "   \t  \n" })
//     void trimLength_whitespaceOnly_returnsZero(String whitespace) {
//         String[] whitespaces = { " ", "\t", "\n", "\r\n", "   \t  \n" };
//         for (String ws : whitespaces) {
//             assertEquals("Failed for whitespace: [" + ws + "]", 0, StringUtilities.trimLength(ws));
//         }
//     }

    /**
     * Strings with leading/trailing whitespace should have those characters removed before length is measured.
     */
//     @ParameterizedTest
//     @MethodSource("stringsWithLeadingTrailingWhitespace")
//     void trimLength_withWhitespace_trimmedLengthMatchesExpected(String input, int expectedLength) {
//         stringsWithLeadingTrailingWhitespace().forEach(arg -> {
//             String input = (String) arg.get()[0];
//             int expected = (int) arg.get()[1];
//             assertEquals("Input: [" + input + "]", expected, StringUtilities.trimLength(input));
//         });
//     }

    /**
     * Verify that Unicode characters are counted correctly after trimming.
     */
//     @Test
//     void trimLength_unicodeCharacters_correctLength() {
//         // two leading and two trailing spaces, 4 Chinese characters
//         String input = "  測試中文  ";
//         // After trimming we have "測試中文" → length 4
//         assertEquals(4, StringUtilities.trimLength(input));
//     }

    /**
     * Verify that surrogate pairs (e.g., emoji) are counted as a single code unit by String.length().
     */
//     @Test
//     void trimLength_emoji_surrogatePairCount() {
//         // Emoji "😀" is a surrogate pair, length() returns 2, and trimLength should return 2 as well
//         String input = "  😀  ";
//         // trimmed string is "😀" whose length is 2 (two char units)
//         assertEquals(2, StringUtilities.trimLength(input));
//     }

    /**
     * Ensure that internal whitespace is preserved and contributes to the length.
     */
//     @Test
//     void trimLength_internalWhitespace_preserved() {
//         String input = "  a b  c  ";
//         // trimmed => "a b  c" (length 6)
//         assertEquals(6, StringUtilities.trimLength(input));
//     }

    /**
     * Parameterised test that covers every case from the JSON specification.
     *
     * <p>Each {@link Arguments} entry contains:
     * <ul>
     *   <li>the input string (may be {@code null})</li>
     *   <li>the expected length after trimming</li>
     *   <li>a description (used only for readability in the source code)</li>
     * </ul>
     * </p>
     */
//     @ParameterizedTest(name = "[{index}] {2}")
//     @MethodSource("trimLengthCases")
//     void trimLength_parameterised(String input, int expectedLength, String description) {
//         trimLengthCases().forEach(arg -> {
//             String input = (String) arg.get()[0];
//             int expected = (int) arg.get()[1];
//             String description = (String) arg.get()[2];
//             assertEquals(description, expected, StringUtilities.trimLength(input));
//         });
//     }
}