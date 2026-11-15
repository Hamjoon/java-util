package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

// @ExtendWith(MockitoExtension.class)
class StringUtilities_levenshteinDistance_Test {

    /**
     * Minimal {@link CharSequence} whose {@code length()} returns a value that
     * triggers integer overflow when {@code +1} is performed inside the
     * Levenshtein implementation.
     */
    private static final class OverflowCharSequence implements CharSequence {

        private final int length;

        OverflowCharSequence(int length) {
            this.length = length;
        }

        @Override
        public int length() {
            return length;
        }

        @Override
        public char charAt(int index) {
            // Not expected to be called because the overflow occurs before any indexing.
            throw new IndexOutOfBoundsException();
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return "[OverflowCharSequence length=" + length + "]";
        }
    }

    private static Stream<Arguments> degenerateCases() {
        return Stream.of(// both null → 0
        Arguments.of(null, null, 0), // first null, second empty → 0
        Arguments.of(null, "", 0), // first null, second non‑empty → length of second
        Arguments.of(null, "abc", 3), // first empty, second null → 0
        Arguments.of("", null, 0), // first empty, second empty → 0
        Arguments.of("", "", 0), // first empty, second non‑empty → length of second
        Arguments.of("", "xyz", 3), // first non‑empty, second null → length of first
        Arguments.of("hello", null, 5), // first non‑empty, second empty → length of first
        Arguments.of("world", "", 5));
    }

    private static Stream<Arguments> standardExamples() {
        return Stream.of(// identical strings → 0
        Arguments.of("same", "same", 0), // simple insert/delete
        Arguments.of("kitten", "sitting", 3), Arguments.of("flaw", "lawn", 2), // case‑sensitivity check
        Arguments.of("Case", "case", 1), // completely different strings of same length
        Arguments.of("abcd", "wxyz", 4), // one character difference
        Arguments.of("a", "b", 1), // empty vs non‑empty (already covered in degenerate, but kept for clarity)
        Arguments.of("", "nonempty", 8));
    }

    private static Stream<Arguments> basicScenarios() {
        return Stream.of(// identical
        Arguments.of("kitten", "kitten", 0), // one substitution
        Arguments.of("kitten", "sitten", 1), // one insertion at end
        Arguments.of("kitten", "kitteny", 1), // one deletion
        Arguments.of("kitten", "kiten", 1), // classic example
        Arguments.of("kitten", "sitting", 3));
    }

    private static Stream<Arguments> complexScenarios() {
        return Stream.of(// é vs e
        Arguments.of("café", "cafe", 1), // mixed edits
        Arguments.of("abcdef", "azced", 3), // all different
        Arguments.of("aaaaaaaaaa", "bbbbbbbbbb", 10), Arguments.of("The quick brown fox jumps over the lazy dog", "The quick brown fox jumps over the lazy dog.", 1), // large disparity
        Arguments.of("short", "a very long string that exceeds the short one", 41));
    }

    private static Stream<Arguments> singleCharScenarios() {
        return Stream.of(// substitution
        Arguments.of("a", "b", 1), // empty → one char
        Arguments.of("", "a", 1));
    }

    private static Stream<Arguments> prefixSuffixScenarios() {
        return Stream.of(// added suffix
        Arguments.of("prefix", "prefixsuffix", 6), // no common prefix
        Arguments.of("suffix", "pre", 7));
    }

    private static Stream<Arguments> unicodeSpecialScenarios() {
        return Stream.of(// each emoji is a surrogate pair → length 2, substitution of each pair = 2
        Arguments.of("😀", "😃", 2), // emoji (2 code units) vs ASCII (1 code unit) → one deletion + one substitution = 2
        Arguments.of("😀", "a", 2), Arguments.of("ab\u0000c", "abxc", 1));
    }

    /**
     * Verify that the method correctly handles the degenerate cases where one
     * or both arguments are {@code null} or empty.
     */
    @ParameterizedTest
    @MethodSource("degenerateCases")
    void testLevenshteinDistance_degenerateCases(CharSequence s, CharSequence t, int expected) {
        assertEquals(expected, StringUtilities.levenshteinDistance(s, t));
    }

    /**
     * Typical examples taken from the literature.
     */
    @ParameterizedTest
    @MethodSource("standardExamples")
    void testLevenshteinDistance_standardExamples(String s, String t, int expected) {
        assertEquals(expected, StringUtilities.levenshteinDistance(s, t));
    }

    /**
     * Randomised round‑trip sanity check: the distance between a string and
     * itself must always be zero, regardless of length or content.
     */
    @ParameterizedTest
    @ValueSource(strings = { "a", "abcde", "The quick brown fox jumps over the lazy dog", "1234567890!@#$%^&*()_+", "   leading and trailing spaces   " })
    void testLevenshteinDistance_selfIsZero(String s) {
        assertEquals(0, StringUtilities.levenshteinDistance(s, s));
    }

    /**
     * Verify that the algorithm works correctly when one string is a prefix
     * of the other.
     */
    @Test
    void testLevenshteinDistance_prefixScenario() {
        String prefix = "prefix";
        String longer = "prefixAndMore";
        int expected = longer.length() - prefix.length();
        assertEquals(expected, StringUtilities.levenshteinDistance(prefix, longer));
        assertEquals(expected, StringUtilities.levenshteinDistance(longer, prefix));
    }

    /**
     * Stress test with relatively long strings to ensure no overflow or
     * unexpected behaviour. The exact distance is pre‑computed.
     */
    @Test
    void testLevenshteinDistance_longStrings() {
        String s = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. ".repeat(5);
        String t = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. ".repeat(4) + "Extra tail";
        int expected = (s.length() - t.length()) + "Extra tail".length();
        assertEquals(expected, StringUtilities.levenshteinDistance(s, t));
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("basicScenarios")
    void testLevenshteinDistance_basicScenarios(String s, String t, int expected) {
        assertEquals(expected, StringUtilities.levenshteinDistance(s, t));
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("complexScenarios")
    void testLevenshteinDistance_complexScenarios(String s, String t, int expected) {
        assertEquals(expected, StringUtilities.levenshteinDistance(s, t));
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("singleCharScenarios")
    void testLevenshteinDistance_singleCharScenarios(String s, String t, int expected) {
        assertEquals(expected, StringUtilities.levenshteinDistance(s, t));
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("prefixSuffixScenarios")
    void testLevenshteinDistance_prefixSuffixScenarios(String s, String t, int expected) {
        assertEquals(expected, StringUtilities.levenshteinDistance(s, t));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testLevenshteinDistance_repeatingPattern() {
        assertEquals(2, StringUtilities.levenshteinDistance("ababab", "baba"));
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("unicodeSpecialScenarios")
    void testLevenshteinDistance_unicodeSpecialCases(String s, String t, int expected) {
        assertEquals(expected, StringUtilities.levenshteinDistance(s, t));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testLevenshteinDistance_charSequenceImplementation() {
        CharSequence s = new StringBuilder("kitten");
        CharSequence t = new StringBuilder("sitten");
        assertEquals(1, StringUtilities.levenshteinDistance(s, t));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testLevenshteinDistance_largeIdenticalStrings() {
        String s = "a".repeat(50);
        String t = "a".repeat(50);
        assertEquals(0, StringUtilities.levenshteinDistance(s, t));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testLevenshteinDistance_largeCompletelyDifferentStrings() {
        String s = "a".repeat(50);
        String t = "b".repeat(50);
        assertEquals(50, StringUtilities.levenshteinDistance(s, t));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testLevenshteinDistance_caseSensitivity() {
        assertEquals(1, StringUtilities.levenshteinDistance("Case", "case"));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testLevenshteinDistance_overflowException() {
        CharSequence overflow = new OverflowCharSequence(Integer.MAX_VALUE);
        // The method should attempt to allocate an array of size length+1 which overflows
        assertThrows(NegativeArraySizeException.class, () -> StringUtilities.levenshteinDistance("a", overflow));
    }
}