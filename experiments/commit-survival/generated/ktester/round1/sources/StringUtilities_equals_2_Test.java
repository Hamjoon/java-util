package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringUtilities_equals_2_Test {

    /**
     * Provides the branch‑coverage test cases defined in the JSON input.
     */
    static Stream<Arguments> branchCoverageProvider() {
        return Stream.of(// Both arguments are null – the method should treat two null references as equal.
        Arguments.of(null, null, true, "Both arguments are null – should be true"), // First argument null, second non‑null.
        Arguments.of(null, "abc", false, "First argument null, second non‑null – should be false"), // Second argument null, first non‑null.
        Arguments.of("abc", null, false, "Second argument null, first non‑null – should be false"), // Same instance reference.
        Arguments.of("sameInstance", "sameInstance", true, "Same String instance – fast‑path reference equality should be true"), // Different objects, same content.
        Arguments.of("hello", "hello", true, "Different objects, identical content – should be true"), // Same length, different characters.
        Arguments.of("hello", "world", false, "Same length strings with differing characters – should be false"), // Different lengths.
        Arguments.of("short", "longer", false, "Strings of different lengths – should be false"), // Both empty.
        Arguments.of("", "", true, "Both strings empty – should be true"), // Unicode character é (U+00E9) – same code point.
        Arguments.of("\u00E9", "\u00E9", true, "Unicode character é – should be true"), // Decomposed e + combining acute (U+0301) vs precomposed é – not equal.
        Arguments.of("\u00E9", "e\u0301", false, "Precomposed é vs decomposed e+´ – should be false"));
    }

    /**
     * Provides the additional coverage test cases defined in the JSON input.
     */
    static Stream<Arguments> additionalCoverageProvider() {
        return Stream.of(// Empty string vs null.
        Arguments.of("", null, false, "Empty string vs null – should be false"), // Null vs empty string.
        Arguments.of(null, "", false, "Null vs empty string – should be false"), // Single‑character equal.
        Arguments.of("a", "a", true, "Single‑character equal strings – should be true"), // Single‑character different.
        Arguments.of("a", "b", false, "Single‑character different strings – should be false"), // Strings containing an embedded null character (U+0000).
        Arguments.of("abc\u0000def", "abc\u0000def", true, "Embedded null character – should be true"), // Embedded null, later characters differ.
        Arguments.of("abc\u0000def", "abc\u0000xyz", false, "Embedded null with later mismatch – should be false"), // Surrogate pair (emoji 😀) equal.
        Arguments.of("\uD83D\uDE00", "\uD83D\uDE00", true, "Emoji 😀 (surrogate pair) – should be true"), // Different surrogate pair (emoji 😁) – not equal.
        Arguments.of("\uD83D\uDE00", "\uD83D\uDE01", false, "Different emojis 😀 vs 😁 – should be false"), // Two distinct empty String instances created via concatenation.
        Arguments.of("" + "", "", true, "Distinct empty String instances – should be true"), // Strings consisting solely of spaces.
        Arguments.of("   ", "   ", true, "Strings of only spaces – should be true"), // Control whitespace characters (tab, newline, carriage‑return).
        Arguments.of("\t\n\r", "\t\n\r", true, "Control whitespace characters – should be true"), // Zero‑width space appended.
        Arguments.of("abc", "abc\u200B", false, "Zero‑width space appended – should be false"), // Zero‑width space only in first argument.
        Arguments.of("abc\u200B", "abc", false, "Zero‑width space only in first argument – should be false"), // Different Unicode representations of Å (U+00C5 vs U+212B) – not equal.
        Arguments.of("\u00C5", "\u212B", false, "Different code points for visual same character Å – should be false"), // Identical Unicode character Å.
        Arguments.of("\u00C5", "\u00C5", true, "Identical Unicode character Å – should be true"));
    }

    @Test
    @DisplayName("Both strings are null → should be true")
    void bothNull() {
        assertTrue(StringUtilities.equals((String) null, (String) null), "equals(null, null) must return true");
    }

    @Test
    @DisplayName("First string null, second non‑null → should be false")
    void firstNullSecondNonNull() {
        assertFalse(StringUtilities.equals(null, "test"), "equals(null, \"test\") must return false");
    }

    @Test
    @DisplayName("First string non‑null, second null → should be false")
    void firstNonNullSecondNull() {
        assertFalse(StringUtilities.equals("test", null), "equals(\"test\", null) must return false");
    }

    @Test
    @DisplayName("Identical non‑null strings → should be true")
    void identicalStrings() {
        assertTrue(StringUtilities.equals("hello", "hello"), "equals(\"hello\", \"hello\") must return true");
    }

    @Test
    @DisplayName("Different strings of same length → should be false")
    void differentStringsSameLength() {
        assertFalse(StringUtilities.equals("hello", "hELLo"), "equals is case‑sensitive; different case should return false");
    }

    @Test
    @DisplayName("Different strings of different length → should be false")
    void differentStringsDifferentLength() {
        assertFalse(StringUtilities.equals("hello", "helloo"), "Strings of different length cannot be equal");
    }

    @Test
    @DisplayName("Empty strings → should be true")
    void emptyStrings() {
        assertTrue(StringUtilities.equals("", ""), "equals(\"\", \"\") must return true");
    }

    @Test
    @DisplayName("Empty string vs non‑empty string → should be false")
    void emptyVsNonEmpty() {
        assertFalse(StringUtilities.equals("", "nonempty"), "equals(\"\", \"nonempty\") must return false");
    }

    @Test
    @DisplayName("Strings containing whitespace are compared exactly")
    void whitespaceSensitive() {
        assertFalse(StringUtilities.equals("a b", "ab"), "Whitespace is significant; strings should not be equal");
        assertTrue(StringUtilities.equals(" a b ", " a b "), "Identical strings with whitespace should be equal");
    }

    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("branchCoverageProvider")
    void testEqualsBranchCoverage(String s1, String s2, boolean expected, String description) {
        assertEquals(expected, StringUtilities.equals(s1, s2), description);
    }

    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("additionalCoverageProvider")
    void testEqualsAdditionalCoverage(String s1, String s2, boolean expected, String description) {
        assertEquals(expected, StringUtilities.equals(s1, s2), description);
    }
}
