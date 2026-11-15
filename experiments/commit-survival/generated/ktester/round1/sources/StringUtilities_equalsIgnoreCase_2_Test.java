package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class StringUtilities_equalsIgnoreCase_2_Test {

    static Stream<Arguments> branchCoverageCases() {
        return Stream.of(// Both null
        arguments(null, null, true, "Both arguments are null – should be equal."), // Null / non‑null
        arguments(null, "abc", false, "First argument null, second non‑null – should be false."), arguments("abc", null, false, "Second argument null, first non‑null – should be false."), // Empty strings
        arguments("", "", true, "Both strings empty – equal regardless of case."), // Identical strings, same case
        arguments("HelloWorld", "HelloWorld", true, "Identical strings with same case – fast‑path true."), // Identical strings, different case
        arguments("HelloWorld", "helloworld", true, "Same letters, different case – case‑insensitive comparison succeeds."), arguments("HelloWorld", "HELLOWORLD", true, "All‑uppercase version – still equal ignoring case."), // Different lengths
        arguments("Hello", "HelloWorld", false, "Different lengths – should return false without character loop."), // Same length, mismatching last character
        arguments("HelloWorld", "HelloWorle", false, "Same length but a mismatching character at the end – should return false during iteration."), // German sharp‑s vs SS
        arguments("Straße", "STRASSE", true, "German sharp‑s (ß) vs. \"SS\" – Java's toLowerCase treats them as equal case‑insensitively."), // Turkish dotted I vs i + dot above
        arguments("İstanbul", "i\u0307stanbul", true, "Turkish dotted capital I vs. lowercase i with dot – verifies Unicode case handling."), // Whitespace preserved
        arguments("  spaced  ", "  SPACED  ", true, "Strings contain leading/trailing spaces – equalsIgnoreCase does not trim, spaces must also match."), arguments("  spaced  ", "spaced", false, "Same letters but different whitespace – should be considered different."));
    }

    static Stream<Arguments> additionalEdgeCases() {
        // Helper string for the "same object" case – the same reference is passed twice.
        String sameObject = "SameObject";
        return Stream.of(// Supplementary characters (surrogate pairs) – case mismatch not handled
        arguments("𝐀", "𝐚", false, "MATHEMATICAL BOLD CAPITAL A vs. its lowercase – equalsIgnoreCase works on char values, returns false."), // Greek sigma vs final sigma
        arguments("Σ", "ς", false, "Greek capital sigma vs. final sigma – toLowerCase maps Σ → σ, not to ς, so comparison fails."), // Composed vs decomposed characters
        arguments("é", "e\u0301", false, "Composed 'é' vs. decomposed 'e' + combining acute – no Unicode normalization, result false."), // Embedded null character
        arguments("abc\u0000def", "ABC\u0000DEF", true, "Strings contain an embedded null character; letters compare case‑insensitively, null stays unchanged."), // Control characters only
        arguments("\n\r\t", "\n\r\t", true, "Both strings consist solely of control characters – identical, so true."), // Same object reference (fast‑path)
        arguments(sameObject, sameObject, true, "Both parameters reference the same String instance – fast‑path equality should trigger."), // Non‑breaking space vs regular space
        arguments("a\u00A0b", "a b", false, "Non‑breaking space (U+00A0) vs regular space (U+0020) – not treated as equivalent."));
    }

    @Test
    @DisplayName("Both arguments null → true")
    void bothNullShouldBeEqual() {
        // given
        String s1 = null;
        String s2 = null;
        // when
        boolean result = StringUtilities.equalsIgnoreCase(s1, s2);
        // then
        assertTrue(result, "Two null strings should be considered equal");
    }

    @Test
    @DisplayName("One argument null → false")
    void oneNullShouldNotBeEqual() {
        // given
        String s1 = null;
        String s2 = "nonNull";
        // when
        boolean result1 = StringUtilities.equalsIgnoreCase(s1, s2);
        boolean result2 = StringUtilities.equalsIgnoreCase(s2, s1);
        // then
        assertFalse(result1, "null vs non‑null should be false");
        assertFalse(result2, "non‑null vs null should be false");
    }

    @Test
    @DisplayName("Identical strings, same case → true")
    void identicalSameCase() {
        // given
        String s1 = "OpenAI";
        String s2 = "OpenAI";
        // when
        boolean result = StringUtilities.equalsIgnoreCase(s1, s2);
        // then
        assertTrue(result);
    }

    @Test
    @DisplayName("Identical strings, different case → true")
    void identicalDifferentCase() {
        // given
        String s1 = "OpenAI";
        String s2 = "openai";
        // when
        boolean result = StringUtilities.equalsIgnoreCase(s1, s2);
        // then
        assertTrue(result);
    }

    @Test
    @DisplayName("Different strings → false")
    void differentStrings() {
        // given
        String s1 = "Hello";
        String s2 = "World";
        // when
        boolean result = StringUtilities.equalsIgnoreCase(s1, s2);
        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("Empty strings → true")
    void emptyStrings() {
        // given
        String s1 = "";
        String s2 = "";
        // when
        boolean result = StringUtilities.equalsIgnoreCase(s1, s2);
        // then
        assertTrue(result);
    }

    @Test
    @DisplayName("Empty vs non‑empty → false")
    void emptyVsNonEmpty() {
        // given
        String s1 = "";
        String s2 = "a";
        // when
        boolean result = StringUtilities.equalsIgnoreCase(s1, s2);
        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("Strings with mixed Unicode case → true")
    void unicodeCaseInsensitive() {
        // given
        // German sharp s
        String s1 = "Straße";
        // Upper‑case representation
        String s2 = "STRASSE";
        // when
        boolean result = StringUtilities.equalsIgnoreCase(s1, s2);
        // then
        // The utility uses Character.toLowerCase internally, which maps ß to ß,
        // so this test documents the expected behaviour of the library.
        // If the implementation differs, the assertion will highlight it.
        assertTrue(result);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("branchCoverageCases")
    @DisplayName("Branch‑coverage cases for equalsIgnoreCase(String, String)")
    void branchCoverage(String s1, String s2, boolean expected, String description) {
        boolean result = StringUtilities.equalsIgnoreCase(s1, s2);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("additionalEdgeCases")
    @DisplayName("Additional edge‑case tests for equalsIgnoreCase(String, String)")
    void additionalEdgeCases(String s1, String s2, boolean expected, String description) {
        boolean result = StringUtilities.equalsIgnoreCase(s1, s2);
        assertEquals(expected, result, description);
    }
}
