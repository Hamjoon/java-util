package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

// Mockito not actually needed but kept from template
@ExtendWith(MockitoExtension.class)
class StringUtilities_equalsIgnoreCaseWithTrim_Test {

    private static Stream<Arguments> nullBothArguments() {
        return Stream.of(Arguments.of(null, null));
    }

    private static Stream<Arguments> nullOneArgument() {
        return Stream.of(Arguments.of(null, "abc"), Arguments.of("abc", null));
    }

    private static Stream<Arguments> equalAfterTrimAndIgnoreCase() {
        return Stream.of(// identical after trimming, same case
        Arguments.of("hello", "hello"), // different case, same after trimming
        Arguments.of("  HeLLo  ", "hello"), // both have surrounding whitespace
        Arguments.of("\tFooBar\n", "  foobar  "), // empty strings (trim does not change them)
        Arguments.of("", ""), // strings that become empty after trim
        Arguments.of("   ", ""));
    }

    private static Stream<Arguments> notEqualCases() {
        return Stream.of(// different after trimming
        Arguments.of("abc", "abd"), // same letters but different after trimming because of extra characters
        Arguments.of("  abc  ", "ab c"), // same letters but different case and no trimming needed – should be true, so we use a case that differs after ignoring case
        // trailing space makes it different after trim
        Arguments.of("abc", "abC "), // one empty, the other not empty after trim
        Arguments.of("", "   "), // both non‑null but one is whitespace only, the other has content
        Arguments.of("   ", "x"));
    }

    /**
     * Provides all test cases required by the JSON description.
     *
     * <p>Each {@code Arguments.of(...)} entry is accompanied by a comment that
     * mirrors the original description, making the intent of the test clear.
     */
    private static Stream<Arguments> equalsIgnoreCaseWithTrimCases() {
        return Stream.of(// Both arguments are null – should return true.
        Arguments.of(null, null, true), // One argument null – should return false.
        Arguments.of(null, "abc", false), Arguments.of("abc", null, false), // Same letters, different case – true.
        Arguments.of("Hello", "hello", true), // Leading/trailing whitespace on first argument – true.
        Arguments.of("  Hello  ", "hello", true), // Whitespace on second argument – true.
        Arguments.of("Hello", "  HeLLo  ", true), // Internal spaces unchanged, case differs – true.
        Arguments.of("Hello World", "hello world", true), // Extra internal whitespace – false.
        Arguments.of("Hello  World", "hello world", false), // Completely different strings – false.
        Arguments.of("Hello", "World", false), // Both become empty after trim – true.
        Arguments.of("   ", "", true), // Various whitespace characters trimmed to empty – true.
        Arguments.of(" \t\n", " ", true), // Both empty strings – true.
        Arguments.of("", "", true), // Unicode characters with different case – true.
        Arguments.of("ábc", "ÁBC", true), // Leading non‑breaking space (U+00A0) is NOT trimmed – false.
        Arguments.of("\u00A0Hello", "Hello", false), // Zero‑width space (U+200B) is NOT trimmed – false.
        Arguments.of("\u200BHello", "Hello", false), // Different kinds of whitespace (space, tab, newline) – true.
        Arguments.of("  abc  ", "\tABC\n", true), // Internal newline characters preserved – true.
        Arguments.of("Hello\nWorld", "hello\nworld", true), // Internal tab vs space – false.
        Arguments.of("Hello\tWorld", "hello world", false), // Both strings consist solely of a non‑breaking space – true.
        Arguments.of("\u00A0", "\u00A0", true), // One becomes empty after trim, the other is a non‑breaking space – false.
        Arguments.of("   ", "\u00A0", false), // Identical emoji strings – true.
        Arguments.of("😀", "😀", true));
    }

    /**
     * Helper to make {@code null} values appear as the string {@code "null"} in
     * assertion messages, avoiding a literal {@code "null"} string that could be
     * confused with an actual input value.
     */
    private static String repr(String s) {
        return s == null ? "null" : "\"" + s + "\"";
    }

    @ParameterizedTest
    @MethodSource("nullBothArguments")
    void equalsIgnoreCaseWithTrim_bothNull_returnsTrue(String s1, String s2) {
        assertTrue(StringUtilities.equalsIgnoreCaseWithTrim(s1, s2));
    }

    @ParameterizedTest
    @MethodSource("nullOneArgument")
    void equalsIgnoreCaseWithTrim_oneNull_returnsFalse(String s1, String s2) {
        assertFalse(StringUtilities.equalsIgnoreCaseWithTrim(s1, s2));
    }

    @ParameterizedTest
    @MethodSource("equalAfterTrimAndIgnoreCase")
    void equalsIgnoreCaseWithTrim_equalAfterTrimAndIgnoreCase_returnsTrue(String s1, String s2) {
        assertTrue(StringUtilities.equalsIgnoreCaseWithTrim(s1, s2));
    }

    @ParameterizedTest
    @MethodSource("notEqualCases")
    void equalsIgnoreCaseWithTrim_notEqual_returnsFalse(String s1, String s2) {
        assertFalse(StringUtilities.equalsIgnoreCaseWithTrim(s1, s2));
    }

    /**
     * Executes the method with a wide range of input combinations and checks
     * that the returned boolean matches the expected outcome.
     */
    @ParameterizedTest(name = "[{index}] s1=''{0}'' s2=''{1}'' → expected={2}")
    @MethodSource("equalsIgnoreCaseWithTrimCases")
    void equalsIgnoreCaseWithTrim_variousCases(String s1, String s2, boolean expected) {
        boolean actual = StringUtilities.equalsIgnoreCaseWithTrim(s1, s2);
        assertEquals(expected, actual, () -> String.format("equalsIgnoreCaseWithTrim(%s, %s) expected %s but got %s", repr(s1), repr(s2), expected, actual));
    }
}
