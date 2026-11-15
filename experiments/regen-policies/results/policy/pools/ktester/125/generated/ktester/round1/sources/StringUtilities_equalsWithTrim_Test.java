package com.cedarsoftware.util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class StringUtilities_equalsWithTrim_Test {

    private static Stream<Arguments> nullAndBothNull() {
        return Stream.of(Arguments.of(null, null));
    }

    private static Stream<Arguments> oneNullOtherNonNull() {
        return Stream.of(Arguments.of(null, ""), Arguments.of("", null), Arguments.of(null, "   "), Arguments.of("text", null), // additional cases from JSON
        Arguments.of(null, "non‑null"), Arguments.of("non‑null", null));
    }

    private static Stream<Arguments> trimmedEqualPairs() {
        return Stream.of(Arguments.of(" abc ", "abc"), Arguments.of("\t\nspaced\t", "spaced"), Arguments.of("noSpace", "  noSpace  "), // additional cases from JSON
        Arguments.of("  trimmed  ", "trimmed"), Arguments.of("\t\nspaced\t", "spaced"), Arguments.of("  noSpace  ", "noSpace"));
    }

    private static Stream<Arguments> trimmedUnequalPairs() {
        return Stream.of(Arguments.of(" abc ", "ab c"), // this actually should be true – handled separately
        Arguments.of("test", "test "), Arguments.of("case", "Case"), // additional mismatches from JSON
        Arguments.of(" abc ", "ab c"), Arguments.of("case", "Case"));
    }

    private static Stream<Arguments> exactMatchPairs() {
        return Stream.of(Arguments.of("hello", "hello"), Arguments.of("JavaJUnit", "JavaJUnit"));
    }

    private static Stream<Arguments> unicodeWhitespacePairs() {
        return Stream.of(// EM SPACE (U+2003) is trimmed by String.trim()
        Arguments.of("\u2003\u2003unicode\u2003", "unicode", true), // NON‑BREAKING SPACE (U+00A0) is NOT trimmed
        Arguments.of("\u00A0nonBreakingSpace", "nonBreakingSpace", false));
    }

    private static Stream<Arguments> nullVsEmptyPairs() {
        return Stream.of(Arguments.of(null, ""), Arguments.of("", null));
    }

    /* -----------------------------------------------------------
       Parameterised tests for the equalsWithTrim method
       ----------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("nullAndBothNull")
    void equalsWithTrim_whenBothNull_returnsTrue(String s1, String s2) {
        assertTrue(StringUtilities.equalsWithTrim(s1, s2), () -> "Both arguments are null – expected true");
    }

    @ParameterizedTest
    @MethodSource("oneNullOtherNonNull")
    void equalsWithTrim_whenOneNull_returnsFalse(String s1, String s2) {
        assertFalse(StringUtilities.equalsWithTrim(s1, s2), () -> "One argument is null – expected false");
    }

    @ParameterizedTest
    @MethodSource("trimmedEqualPairs")
    void equalsWithTrim_whenTrimmedEqual_returnsTrue(String s1, String s2) {
        assertTrue(StringUtilities.equalsWithTrim(s1, s2), () -> String.format("Trimmed equality expected true for \"%s\" and \"%s\"", s1, s2));
    }

    @ParameterizedTest
    @MethodSource("trimmedUnequalPairs")
    void equalsWithTrim_whenTrimmedNotEqual_returnsFalse(String s1, String s2) {
        assertFalse(StringUtilities.equalsWithTrim(s1, s2), () -> String.format("Trimmed inequality expected false for \"%s\" and \"%s\"", s1, s2));
    }

    @Test
    void equalsWithTrim_whenBothEmptyOrWhitespace_returnsTrue() {
        assertTrue(StringUtilities.equalsWithTrim("", ""));
        assertTrue(StringUtilities.equalsWithTrim("   ", ""));
        assertTrue(StringUtilities.equalsWithTrim("\t\n\r", " "));
    }

    @Test
    void equalsWithTrim_whenBothNonNullButSameReference_returnsTrue() {
        String s = "  same  ";
        assertTrue(StringUtilities.equalsWithTrim(s, s));
    }

    /* -----------------------------------------------------------
       Exact match without whitespace
       ----------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("exactMatchPairs")
    void equalsWithTrim_whenExactMatch_returnsTrue(String s1, String s2) {
        assertTrue(StringUtilities.equalsWithTrim(s1, s2), () -> String.format("Exact match expected true for \"%s\" and \"%s\"", s1, s2));
    }

    @Test
    void equalsWithTrim_whenTrailingSpaceOnly_returnsTrue() {
        // Specific case where only trailing whitespace exists on the second argument
        assertTrue(StringUtilities.equalsWithTrim("test", "test "), "Trailing space should be ignored after trim");
    }

    /* -----------------------------------------------------------
       Unicode whitespace handling
       ----------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("unicodeWhitespacePairs")
    void equalsWithTrim_whenUnicodeWhitespaceHandledCorrectly(String s1, String s2, boolean expected) {
        if (expected) {
            assertTrue(StringUtilities.equalsWithTrim(s1, s2), () -> "Expected true for Unicode whitespace case");
        } else {
            assertFalse(StringUtilities.equalsWithTrim(s1, s2), () -> "Expected false for Unicode whitespace case");
        }
    }

    /* -----------------------------------------------------------
       Very long whitespace‑only strings
       ----------------------------------------------------------- */
    @Test
    void equalsWithTrim_whenVeryLongWhitespaceStrings_returnsTrue() {
        // Build a 10 000‑character string consisting of spaces with a leading/trailing space
        String longSpaces = " ".repeat(10_000);
        assertTrue(StringUtilities.equalsWithTrim(longSpaces, longSpaces), "Two identical long whitespace strings should be equal after trim");
        // Both become empty after trim
        assertTrue(StringUtilities.equalsWithTrim(longSpaces, "   "), "Long whitespace string should equal a short whitespace string after trim");
    }

    /* -----------------------------------------------------------
       Null vs empty string (should be false)
       ----------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("nullVsEmptyPairs")
    void equalsWithTrim_whenNullVsEmpty_returnsFalse(String s1, String s2) {
        assertFalse(StringUtilities.equalsWithTrim(s1, s2), () -> "Null vs empty should be false");
    }
}
