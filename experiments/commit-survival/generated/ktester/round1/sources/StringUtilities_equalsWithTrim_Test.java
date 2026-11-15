package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringUtilities_equalsWithTrim_Test {

    /**
     * Helper to make null and whitespace visible in failure messages.
     */
    private static String repr(String s) {
        return s == null ? "null" : '"' + s.replace("\n", "\\n").replace("\t", "\\t").replace("\r", "\\r") + '"';
    }

    /**
     * Supplies a stream of arguments representing every test case from the JSON.
     */
    private static Stream<Arguments> provideEqualsWithTrimCases() {
        return Stream.of(// Null handling
        Arguments.of("Both arguments are null – the method should treat them as equal.", null, null, true), Arguments.of("First argument is null, second is non‑null – should return false.", null, "any", false), Arguments.of("First argument is non‑null, second is null – should return false.", "any", null, false), // Basic equality
        Arguments.of("Two identical non‑null strings without whitespace should be equal.", "hello", "hello", true), Arguments.of("Two different non‑null strings without whitespace should not be equal.", "hello", "world", false), // Whitespace handling
        Arguments.of("Leading and trailing spaces are trimmed; the core content matches.", "  hello  ", "hello", true), Arguments.of("Tabs, newlines and carriage returns are trimmed by String.trim().", "\t\nhello\r", "hello", true), Arguments.of("Both strings have extra spaces on opposite sides; after trimming they become identical.", "hello  ", "  hello", true), Arguments.of("Internal spaces are preserved; only surrounding whitespace is removed, resulting in equality.", "  hello world  ", "hello world", true), Arguments.of("After trimming, s1 still contains a double space between words, so strings differ.", "  hello  world  ", "hello world", false), // Empty and whitespace strings
        Arguments.of("Two empty strings are equal.", "", "", true), Arguments.of("A string consisting only of spaces trims to an empty string, matching the other empty string.", "   ", "", true), Arguments.of("Both strings contain only whitespace; after trim both become empty.", "   ", " ", true), Arguments.of("One side trims to empty, the other to a non‑empty character; not equal.", " ", "a", false), // Unicode whitespace
        Arguments.of("Non‑breaking space (U+00A0) is not removed by String.trim(), so strings differ.", "\u00A0hello\u00A0", "hello", false), Arguments.of("EM SPACE (U+2003) is not trimmed; the leading Unicode whitespace remains.", "\u2003hello", "hello", false), // Case sensitivity
        Arguments.of("equalsWithTrim performs a case‑sensitive comparison after trimming.", "Hello", "hello", false), // Null vs whitespace
        Arguments.of("Null compared to a whitespace‑only string should return false.", null, "   ", false), Arguments.of("Whitespace‑only string compared to null should return false.", "   ", null, false));
    }

    @Test
    @DisplayName("Both arguments null should be considered equal")
    void bothNullReturnsTrue() {
        assertTrue(StringUtilities.equalsWithTrim(null, null), "When both strings are null the method should return true");
    }

    @Test
    @DisplayName("One argument null should return false")
    void oneNullReturnsFalse() {
        assertFalse(StringUtilities.equalsWithTrim(null, "non-null"), "When only one argument is null the method should return false");
        assertFalse(StringUtilities.equalsWithTrim("non-null", null), "When only one argument is null the method should return false");
    }

    @Test
    @DisplayName("Strings that are equal after trimming should return true")
    void equalAfterTrimReturnsTrue() {
        assertTrue(StringUtilities.equalsWithTrim("  hello  ", "hello"), "Strings that differ only by surrounding whitespace should be equal");
        assertTrue(StringUtilities.equalsWithTrim("\tfoo\n", "\nfoo\t"), "Strings that differ only by whitespace characters should be equal");
    }

    @Test
    @DisplayName("Strings that differ after trimming should return false")
    void differentAfterTrimReturnsFalse() {
        assertFalse(StringUtilities.equalsWithTrim("  hello", "world  "), "Different trimmed contents should not be considered equal");
        assertFalse(StringUtilities.equalsWithTrim("test", "test1"), "Different strings should not be considered equal even after trim");
    }

    @Test
    @DisplayName("Strings consisting solely of whitespace become empty after trim and should be equal")
    void whitespaceOnlyStringsAreEqual() {
        assertTrue(StringUtilities.equalsWithTrim("   ", "\t\n"), "Both strings become empty after trim and therefore should be equal");
    }

    @Test
    @DisplayName("Identical non‑null strings without whitespace should return true")
    void identicalStringsReturnTrue() {
        assertTrue(StringUtilities.equalsWithTrim("same", "same"), "Identical strings should be considered equal");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideEqualsWithTrimCases")
    @DisplayName("Parameterized test for equalsWithTrim covering all JSON cases")
    void equalsWithTrimParameterized(String description, String s1, String s2, boolean expected) {
        boolean actual = StringUtilities.equalsWithTrim(s1, s2);
        assertEquals(expected, actual, () -> String.format("Case \"%s\" failed: equalsWithTrim(%s, %s) expected %s but got %s", description, repr(s1), repr(s2), expected, actual));
    }
}
