package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringUtilities_equalsIgnoreCaseWithTrim_Test {

    static Stream<Arguments> nullHandlingProvider() {
        return Stream.of(// both null
        Arguments.of(null, null, true), // first null
        Arguments.of(null, "nonNull", false), // second null
        Arguments.of("nonNull", null, false));
    }

    static Stream<Arguments> basicEqualityProvider() {
        return Stream.of(// identical
        Arguments.of("hello", "hello", true), // different
        Arguments.of("hello", "world", false));
    }

    static Stream<Arguments> caseInsensitivityProvider() {
        return Stream.of(Arguments.of("Java", "jAvA", true), Arguments.of("TeStInG", "testing", true), // same prefix, different length
        Arguments.of("Java", "Jav", false));
    }

    static Stream<Arguments> whitespaceHandlingProvider() {
        return Stream.of(// leading/trailing spaces
        Arguments.of("  spaced  ", "spaced", true), // various whitespace chars
        Arguments.of("\t\nabc\r\n", "ABC", true), // internal spaces preserved
        Arguments.of("  a b  ", "A B", true), // different internal spacing
        Arguments.of("a  b", "a b", false));
    }

    static Stream<Arguments> emptyAndWhitespaceProvider() {
        return Stream.of(Arguments.of("", "", true), Arguments.of("   ", "", true), Arguments.of("\t\n", " ", true), Arguments.of(" ", "a", false));
    }

    static Stream<Arguments> unicodeHandlingProvider() {
        return Stream.of(// Greek sigma case‑folding
        Arguments.of("Σ", "σ", true), // Turkish dotted I – not equal in default locale
        Arguments.of("İ", "i", false), // German sharp s – not equal under equalsIgnoreCase
        Arguments.of("ß", "SS", false));
    }

    /**
     * Helper to generate a repeated‑character string of given length.
     */
    private static String generateLongString(char ch, int length) {
        char[] arr = new char[length];
        java.util.Arrays.fill(arr, ch);
        return new String(arr);
    }

    @Test
    @DisplayName("Both arguments null → true")
    void bothNullReturnsTrue() {
        assertTrue(StringUtilities.equalsIgnoreCaseWithTrim(null, null), "When both strings are null the method should return true");
    }

    @Test
    @DisplayName("One argument null → false")
    void oneNullReturnsFalse() {
        assertFalse(StringUtilities.equalsIgnoreCaseWithTrim(null, "test"), "Null compared with a non‑null string should return false");
        assertFalse(StringUtilities.equalsIgnoreCaseWithTrim("test", null), "Non‑null compared with null should return false");
    }

    @Test
    @DisplayName("Equal strings ignoring case and surrounding whitespace")
    void equalIgnoringCaseAndTrim() {
        String s1 = "  HelloWorld  ";
        String s2 = "helloworld";
        assertTrue(StringUtilities.equalsIgnoreCaseWithTrim(s1, s2), "Strings that differ only by case and surrounding whitespace must be considered equal");
    }

    @Test
    @DisplayName("Unequal strings after trim")
    void notEqualAfterTrim() {
        String s1 = "  HelloWorld  ";
        // internal spaces differ
        String s2 = "Hello  World";
        assertFalse(StringUtilities.equalsIgnoreCaseWithTrim(s1, s2), "Strings that differ after trimming should not be considered equal");
    }

    @Test
    @DisplayName("Trim only one side")
    void trimOnlyOneSide() {
        String s1 = "  spaced  ";
        String s2 = "spaced";
        assertTrue(StringUtilities.equalsIgnoreCaseWithTrim(s1, s2), "Trailing/leading whitespace on one side must be ignored");
    }

    @Test
    @DisplayName("Empty strings and whitespace")
    void emptyAndWhitespace() {
        assertTrue(StringUtilities.equalsIgnoreCaseWithTrim("", ""), "Two empty strings are equal");
        assertTrue(StringUtilities.equalsIgnoreCaseWithTrim("   ", ""), "Whitespace‑only string should be equal to empty after trim");
        assertTrue(StringUtilities.equalsIgnoreCaseWithTrim("\t\n", "\r"), "Various whitespace characters should be trimmed to empty and considered equal");
    }

    /**
     * ---------- Null handling ----------
     */
    @ParameterizedTest(name = "[{index}] s1={0} s2={1} → {2}")
    @MethodSource("nullHandlingProvider")
    void nullHandling(String s1, String s2, boolean expected) {
        assertEquals(expected, StringUtilities.equalsIgnoreCaseWithTrim(s1, s2), "Null handling test failed");
    }

    /**
     * ---------- Basic equality ----------
     */
    @ParameterizedTest(name = "[{index}] \"{0}\" vs \"{1}\" → {2}")
    @MethodSource("basicEqualityProvider")
    void basicEquality(String s1, String s2, boolean expected) {
        assertEquals(expected, StringUtilities.equalsIgnoreCaseWithTrim(s1, s2), "Basic equality test failed");
    }

    /**
     * ---------- Case insensitivity ----------
     */
    @ParameterizedTest(name = "[{index}] \"{0}\" vs \"{1}\" → {2}")
    @MethodSource("caseInsensitivityProvider")
    void caseInsensitivity(String s1, String s2, boolean expected) {
        assertEquals(expected, StringUtilities.equalsIgnoreCaseWithTrim(s1, s2), "Case‑insensitivity test failed");
    }

    /**
     * ---------- Whitespace handling (trim) ----------
     */
    @ParameterizedTest(name = "[{index}] \"{0}\" vs \"{1}\" → {2}")
    @MethodSource("whitespaceHandlingProvider")
    void whitespaceHandling(String s1, String s2, boolean expected) {
        assertEquals(expected, StringUtilities.equalsIgnoreCaseWithTrim(s1, s2), "Whitespace handling (trim) test failed");
    }

    /**
     * ---------- Unicode handling ----------
     */
    @ParameterizedTest(name = "[{index}] \"{0}\" vs \"{1}\" → {2}")
    @MethodSource("unicodeHandlingProvider")
    void unicodeHandling(String s1, String s2, boolean expected) {
        assertEquals(expected, StringUtilities.equalsIgnoreCaseWithTrim(s1, s2), "Unicode handling test failed");
    }

    /**
     * ---------- Non‑standard whitespace (NBSP) ----------
     */
    @Test
    @DisplayName("Non‑breaking space is not trimmed")
    void nonBreakingSpaceNotTrimmed() {
        // NBSP before/after
        String s1 = "\u00A0abc\u00A0";
        String s2 = "abc";
        assertFalse(StringUtilities.equalsIgnoreCaseWithTrim(s1, s2), "NBSP is not removed by trim, so strings should be considered different");
    }

    /**
     * ---------- Long strings ----------
     */
    @Test
    @DisplayName("Very long identical strings should be equal")
    void longIdenticalStrings() {
        String longA = generateLongString('a', 10_000);
        String longB = generateLongString('a', 10_000);
        assertTrue(StringUtilities.equalsIgnoreCaseWithTrim(longA, longB), "Two identical 10,000‑character strings must be equal");
    }
}
