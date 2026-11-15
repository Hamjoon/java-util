package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class StringUtilities_levenshteinDistance_Test {

    /**
     * CharSequence whose {@code length()} always throws a {@link RuntimeException}.
     */
    private static final class BadLengthCharSequence implements CharSequence {

        private final String delegate;

        BadLengthCharSequence(String delegate) {
            this.delegate = delegate;
        }

        @Override
        public int length() {
            throw new RuntimeException("length() failure");
        }

        @Override
        public char charAt(int index) {
            // Not expected to be called because length() already fails,
            // but we provide a dummy implementation to satisfy the interface.
            return delegate.charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return delegate.subSequence(start, end);
        }

        @Override
        public String toString() {
            return delegate;
        }
    }

    /**
     * CharSequence whose {@code charAt(int)} always throws a {@link RuntimeException}.
     */
    private static final class BadCharAtCharSequence implements CharSequence {

        private final String delegate;

        BadCharAtCharSequence(String delegate) {
            this.delegate = delegate;
        }

        @Override
        public int length() {
            return delegate.length();
        }

        @Override
        public char charAt(int index) {
            throw new RuntimeException("charAt() failure");
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return delegate.subSequence(start, end);
        }

        @Override
        public String toString() {
            return delegate;
        }
    }

    /**
     * CharSequence that reports a negative length, triggering array allocation failure.
     */
    private static final class NegativeLengthCharSequence implements CharSequence {

        @Override
        public int length() {
            // illegal length
            return -1;
        }

        @Override
        public char charAt(int index) {
            throw new IndexOutOfBoundsException();
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            throw new IndexOutOfBoundsException();
        }

        @Override
        public String toString() {
            return "";
        }
    }

    private static Stream<Arguments> nullAndEmptyProvider() {
        return Stream.of(// both null → 0
        Arguments.of(null, null, 0), // one null, other empty → 0
        Arguments.of(null, "", 0), Arguments.of("", null, 0), // one null, other non‑empty → length of the non‑empty argument
        Arguments.of(null, "abc", 3), Arguments.of("abc", null, 3), // both empty → 0
        Arguments.of("", "", 0), // empty vs non‑empty
        Arguments.of("", "kitten", 6), Arguments.of("kitten", "", 6));
    }

    private static Stream<Arguments> classicExamplesProvider() {
        return Stream.of(Arguments.of("kitten", "sitting", 3), Arguments.of("flaw", "lawn", 2), Arguments.of("gumbo", "gambol", 2), Arguments.of("book", "back", 2), Arguments.of("Saturday", "Sunday", 3));
    }

    private static String randomString(java.util.Random random, int length) {
        StringBuilder sb = new StringBuilder(length);
        // printable ASCII range 32‑126
        for (int i = 0; i < length; i++) {
            sb.append((char) (32 + random.nextInt(95)));
        }
        return sb.toString();
    }

    private static Stream<Arguments> normalCasesProvider() {
        return Stream.of(// intention / execution – expected distance 5
        Arguments.of("intention", "execution", 5), // completely different same‑length strings
        Arguments.of("abcde", "fghij", 5), // insertion case
        Arguments.of("a", "ab", 1), // deletion case
        Arguments.of("ab", "a", 1), // known mixed edit distance
        Arguments.of("abcdef", "azced", 3), // Unicode surrogate pair test
        Arguments.of("😀🐱", "😀🐶", 1));
    }

    private static Stream<Arguments> edgeCasesProvider() {
        return Stream.of(Arguments.of("aaaa", "aaab", 1), Arguments.of("abcdef", "abcfde", 2), Arguments.of("abc", "xbc", 1), Arguments.of("abc", "abx", 1), Arguments.of("abc", "aXbYcZ", 3), // combining character vs pre‑composed character – two edits
        Arguments.of("é", "é", 2), // leading null character
        Arguments.of("\u0000abc", "abc", 1), // three spaces vs empty
        Arguments.of("   ", "", 3), // swapped whitespace characters
        Arguments.of("\t\n", "\n\t", 2));
    }

    private static Stream<Arguments> charSequenceImplProvider() {
        return Stream.of(Arguments.of(new StringBuilder("kitten"), "sitting", 3), Arguments.of(new StringBuffer("flaw"), "lawn", 2));
    }

    /* --------------------------------------------------------------------- *
     *  Null / empty handling                                                *
     * --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("nullAndEmptyProvider")
    void levenshteinDistance_nullOrEmpty(CharSequence s, CharSequence t, int expected) {
        assertEquals(expected, StringUtilities.levenshteinDistance(s, t));
    }

    /* --------------------------------------------------------------------- *
     *  Classic examples from the literature                                 *
     * --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("classicExamplesProvider")
    void levenshteinDistance_classicExamples(String s, String t, int expected) {
        assertEquals(expected, StringUtilities.levenshteinDistance(s, t));
        // Levenshtein distance is symmetric
        assertEquals(expected, StringUtilities.levenshteinDistance(t, s));
    }

    /* --------------------------------------------------------------------- *
     *  Identical strings – distance must be zero                           *
     * --------------------------------------------------------------------- */
    @ParameterizedTest
    @ValueSource(strings = { "", "a", "abc", "The quick brown fox", "😀🐶🐱" })
    void levenshteinDistance_identicalStrings(String s) {
        assertEquals(0, StringUtilities.levenshteinDistance(s, s));
    }

    /* --------------------------------------------------------------------- *
     *  Stress test with longer random strings (deterministic seed)         *
     * --------------------------------------------------------------------- */
    @Test
    void levenshteinDistance_longRandomStrings() {
        // deterministic random source for reproducibility
        java.util.Random random = new java.util.Random(12345L);
        String a = randomString(random, 200);
        String b = randomString(random, 200);
        // The distance must be between 0 and the length of the longer string
        int distance = StringUtilities.levenshteinDistance(a, b);
        assertTrue(distance >= 0 && distance <= Math.max(a.length(), b.length()));
    }

    /* --------------------------------------------------------------------- *
     *  Verify that the algorithm correctly handles case‑sensitivity       *
     * --------------------------------------------------------------------- */
    @Test
    void levenshteinDistance_caseSensitivity() {
        assertEquals(1, StringUtilities.levenshteinDistance("Apple", "apple"));
        assertEquals(0, StringUtilities.levenshteinDistance("Apple", "Apple"));
    }

    /* --------------------------------------------------------------------- *
     *  Additional normal cases from the JSON payload                         *
     * --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("normalCasesProvider")
    void levenshteinDistance_normalCases(String s, String t, int expected) {
        assertEquals(expected, StringUtilities.levenshteinDistance(s, t));
        // symmetry check
        assertEquals(expected, StringUtilities.levenshteinDistance(t, s));
    }

    /* --------------------------------------------------------------------- *
     *  Edge‑case character variations                                        *
     * --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("edgeCasesProvider")
    void levenshteinDistance_edgeCases(String s, String t, int expected) {
        assertEquals(expected, StringUtilities.levenshteinDistance(s, t));
    }

    /* --------------------------------------------------------------------- *
     *  CharSequence implementation handling (StringBuilder / StringBuffer) *
     * --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("charSequenceImplProvider")
    void levenshteinDistance_charSequenceImplementations(CharSequence s, CharSequence t, int expected) {
        assertEquals(expected, StringUtilities.levenshteinDistance(s, t));
    }

    /* --------------------------------------------------------------------- *
     *  Large input handling – long strings differing only at the end        *
     * --------------------------------------------------------------------- */
    @Test
    void levenshteinDistance_largeSimilarStrings() {
        String s = "aaaaaaaaaaaaaaaaaaaa";
        String t = "aaaaaaaaaaaaaaaaaaab";
        assertEquals(1, StringUtilities.levenshteinDistance(s, t));
    }

    /* --------------------------------------------------------------------- *
     *  Exception handling – length() throws RuntimeException               *
     * --------------------------------------------------------------------- */
    @Test
    void levenshteinDistance_lengthThrowsRuntimeException_firstArgument() {
        CharSequence bad = new BadLengthCharSequence("ignored");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> StringUtilities.levenshteinDistance(bad, "hello"));
        assertEquals("length() failure", ex.getMessage());
    }

    @Test
    void levenshteinDistance_lengthThrowsRuntimeException_secondArgument() {
        CharSequence bad = new BadLengthCharSequence("ignored");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> StringUtilities.levenshteinDistance("world", bad));
        assertEquals("length() failure", ex.getMessage());
    }

    /* --------------------------------------------------------------------- *
     *  Exception handling – charAt() throws RuntimeException                *
     * --------------------------------------------------------------------- */
    @Test
    void levenshteinDistance_charAtThrowsRuntimeException_firstArgument() {
        CharSequence bad = new BadCharAtCharSequence("sample");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> StringUtilities.levenshteinDistance(bad, "sample"));
        assertEquals("charAt() failure", ex.getMessage());
    }

    @Test
    void levenshteinDistance_charAtThrowsRuntimeException_secondArgument() {
        CharSequence bad = new BadCharAtCharSequence("sample");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> StringUtilities.levenshteinDistance("sample", bad));
        assertEquals("charAt() failure", ex.getMessage());
    }

    /* --------------------------------------------------------------------- *
     *  Exception handling – negative length() results in NegativeArraySizeException *
     * --------------------------------------------------------------------- */
    @Test
    void levenshteinDistance_negativeLengthThrowsArraySizeException() {
        CharSequence bad = new NegativeLengthCharSequence();
        assertThrows(NegativeArraySizeException.class, () -> StringUtilities.levenshteinDistance("any", bad));
    }
}
