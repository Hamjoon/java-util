package com.cedarsoftware.util;

import java.util.stream.Stream;
// import org.junit.Test;
// import static org.junit.Assert.*;

class StringUtilities_trimToEmpty_Test {

    /**
     * Strings that contain only whitespace characters.
     */
//     private static Stream<Arguments> whitespaceStrings() {
//         return Stream.of(" ", "\t", "\n", "\r\n", " \t \n  ");
//     }

    /**
     * Strings that contain non‑whitespace characters, possibly with surrounding whitespace.
     */
//     private static Stream<Arguments> nonWhitespaceStrings() {
//         return Stream.of(// already trimmed
//         new Object[] { "abc", "abc" }, // leading whitespace only
//         new Object[] { "  abc", "abc" }, // trailing whitespace only
//         new Object[] { "abc   ", "abc" }, // both sides
//         new Object[] { "  abc  ", "abc" }, // internal whitespace must be preserved
//         new Object[] { " a b c ", "a b c" }, // mixed whitespace characters
//         new Object[] { "\t\n abc \r\n", "abc" });
//     }

    /**
     * Provides test data that exercises Unicode characters which are
     * *not* treated as whitespace by {@link String#trim()}.
     * Each argument includes a description that will appear in the test
     * name for easier debugging.
     */
//     private static Stream<Arguments> additionalStrings() {
//         return Stream.of(// Non‑breaking space (U+00A0) is not trimmed
//         new Object[] { "\u00A0leading", "\u00A0leading", "Non‑breaking space (U+00A0) at start should remain unchanged" }, // Figure space (U+2007) inside string – normal spaces are trimmed
//         new Object[] { " \u2007inner\u2007 ", "\u2007inner\u2007", "Standard spaces trimmed, figure spaces (U+2007) preserved" }, // Em space (U+2003) inside – no surrounding whitespace
//         new Object[] { "a\u2003b", "a\u2003b", "Em space (U+2003) inside string should be preserved" }, // Em space with surrounding normal spaces
//         new Object[] { " \u2003a\u2003 ", "\u2003a\u2003", "Leading/trailing normal spaces trimmed, internal em spaces preserved" }, // String consisting solely of em spaces – not trimmed
//         new Object[] { "\u2003\u2003", "\u2003\u2003", "String of only em spaces (U+2003) should remain unchanged" }, // Non‑ASCII character (sharp s) – no trimming needed
//         new Object[] { "ß", "ß", "Non‑ASCII character without surrounding whitespace unchanged" }, // Byte Order Mark (U+FEFF) – not trimmed, but leading normal space removed
//         new Object[] { " \uFEFF", "\uFEFF", "Leading normal space removed, BOM (U+FEFF) remains" }, // Zero‑width space (U+200B) – not considered whitespace by trim()
//         new Object[] { " \u200B", "\u200B", "Leading normal space removed, zero‑width space (U+200B) remains" });
//     }

//     @Test
//     void trimToEmpty_whenNull_returnsEmptyConstant() {
//         String result = StringUtilities.trimToEmpty(null);
//         assertNotNull("Result should never be null", result);
//         assertEquals("Null input must return the EMPTY constant", StringUtilities.EMPTY, result);
//     }

//     @Test
//     void trimToEmpty_whenEmptyString_returnsEmptyString() {
//         String result = StringUtilities.trimToEmpty("");
//         assertEquals("Empty input must return an empty string", "", result);
//     }

//     @ParameterizedTest
//     @MethodSource("whitespaceStrings")
//     void trimToEmpty_whenOnlyWhitespace_returnsEmptyString(String ws) {
//         whitespaceStrings().forEach(ws -> {
//             String result = StringUtilities.trimToEmpty(ws);
//             assertEquals("Whitespace‑only string should be trimmed to empty", "", result);
//         });
//     }

//     @ParameterizedTest
//     @MethodSource("nonWhitespaceStrings")
//     void trimToEmpty_whenStringHasContent_trimsOnlyEdges(String input, String expected) {
//         nonWhitespaceStrings().forEach(pair -> {
//             String input = (String) pair[0];
//             String expected = (String) pair[1];
//             String result = StringUtilities.trimToEmpty(input);
//             assertEquals("Input [" + input + "] should be trimmed to [" + expected + "]", expected, result);
//         });
//     }

//     @ParameterizedTest(name = "{2}")
//     @MethodSource("additionalStrings")
//     void trimToEmpty_additionalCases(String input, String expected, String description) {
//         additionalStrings().forEach(triple -> {
//             String input = (String) triple[0];
//             String expected = (String) triple[1];
//             String description = (String) triple[2];
//             String result = StringUtilities.trimToEmpty(input);
//             assertEquals(description, expected, result);
//         });
//     }
}