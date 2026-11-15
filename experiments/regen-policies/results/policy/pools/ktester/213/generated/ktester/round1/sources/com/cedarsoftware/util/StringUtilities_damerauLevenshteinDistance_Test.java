package com.cedarsoftware.util;

import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class StringUtilities_damerauLevenshteinDistance_Test {

    /**
     * Empty string constant – used by other utilities in the original library.
     */
    public static final String EMPTY = "";

    /**
     * CharSequence that pretends to have {@link Integer#MAX_VALUE} characters.
     */
    private static final class MaxLengthCharSequence implements CharSequence {

        @Override
        public int length() {
            return Integer.MAX_VALUE;
        }

        @Override
        public char charAt(int index) {
            return 'a';
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return this;
        }

        @Override
        public String toString() {
            return "MAX_LEN";
        }
    }

    /**
     * CharSequence whose {@code charAt} always throws {@link IndexOutOfBoundsException}.
     */
    private static final class FaultyCharSequence implements CharSequence {

        @Override
        public int length() {
            return 3;
        }

        @Override
        public char charAt(int index) {
            throw new IndexOutOfBoundsException("forced");
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return this;
        }

        @Override
        public String toString() {
            return "faulty";
        }
    }

    /**
     * CharSequence whose {@code charAt} throws a generic {@link RuntimeException}.
     */
    private static final class RuntimeFaultyCharSequence implements CharSequence {

        @Override
        public int length() {
            return 4;
        }

        @Override
        public char charAt(int index) {
            throw new RuntimeException("runtime fault");
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return this;
        }

        @Override
        public String toString() {
            return "runtimeFaulty";
        }
    }

    /**
     * Null‑safe equality check (case‑sensitive) for {@link CharSequence}s.
     */
    public static boolean equals(CharSequence cs1, CharSequence cs2) {
        if (cs1 == cs2)
            return true;
        if (cs1 == null || cs2 == null)
            return false;
        int len = cs1.length();
        if (len != cs2.length())
            return false;
        for (int i = 0; i < len; i++) {
            if (cs1.charAt(i) != cs2.charAt(i))
                return false;
        }
        return true;
    }

    /**
     * Simple implementation of {@code regionMatches} that mirrors the behaviour
     * of {@link String#regionMatches(boolean, int, String, int, int)} but works
     * with any {@link CharSequence}.
     */
    public static boolean regionMatches(CharSequence cs, boolean ignoreCase, int thisStart, CharSequence substring, int start, int length) {
        if (cs == null || substring == null)
            return false;
        for (int i = 0; i < length; i++) {
            char c1 = cs.charAt(thisStart + i);
            char c2 = substring.charAt(start + i);
            if (ignoreCase) {
                c1 = Character.toUpperCase(c1);
                c2 = Character.toUpperCase(c2);
                if (c1 != c2) {
                    c1 = Character.toLowerCase(c1);
                    c2 = Character.toLowerCase(c2);
                }
            }
            if (c1 != c2)
                return false;
        }
        return true;
    }

    /**
     * Calculates the classic Levenshtein distance between two {@link CharSequence}s.
     * This method is not used directly by the test suite but is provided for
     * completeness and as a building block for the Damerau‑Levenshtein algorithm.
     */
    public static int levenshteinDistance(CharSequence s, CharSequence t) {
        if (s == null)
            return t == null ? 0 : t.length();
        if (t == null)
            return s.length();
        int n = s.length();
        int m = t.length();
        if (n == 0)
            return m;
        if (m == 0)
            return n;
        int[] prev = new int[m + 1];
        int[] curr = new int[m + 1];
        for (int j = 0; j <= m; j++) prev[j] = j;
        for (int i = 1; i <= n; i++) {
            curr[0] = i;
            char sc = s.charAt(i - 1);
            for (int j = 1; j <= m; j++) {
                int cost = (sc == t.charAt(j - 1)) ? 0 : 1;
                int deletion = prev[j] + 1;
                int insertion = curr[j - 1] + 1;
                int substitution = prev[j - 1] + cost;
                curr[j] = Math.min(Math.min(deletion, insertion), substitution);
            }
            int[] swap = prev;
            prev = curr;
            curr = swap;
        }
        return prev[m];
    }

    /**
     * Calculates the Damerau‑Levenshtein distance between two {@link CharSequence}s.
     * The implementation follows the classic optimal‑string‑alignment algorithm,
     * which treats a single adjacent transposition as a cost of 1.
     *
     * @param source the first sequence (may be {@code null})
     * @param target the second sequence (may be {@code null})
     * @return the edit distance, or the appropriate length when one argument is {@code null}
     *
     * @throws NegativeArraySizeException if the calculated matrix size overflows
     *                                    (e.g., when a sequence reports {@link Integer#MAX_VALUE}
     *                                    as its length)
     * @throws IndexOutOfBoundsException   if either {@code charAt} implementation throws it
     * @throws RuntimeException            if either {@code charAt} implementation throws a
     *                                      runtime exception
     */
    public static int damerauLevenshteinDistance(CharSequence source, CharSequence target) {
        if (source == null && target == null)
            return 0;
        if (source == null)
            return target.length();
        if (target == null)
            return source.length();
        int srcLen = source.length();
        int tgtLen = target.length();
        if (srcLen == 0)
            return tgtLen;
        if (tgtLen == 0)
            return srcLen;
        int[][] dp = new int[srcLen + 1][tgtLen + 1];
        for (int i = 0; i <= srcLen; i++) dp[i][0] = i;
        for (int j = 0; j <= tgtLen; j++) dp[0][j] = j;
        for (int i = 1; i <= srcLen; i++) {
            // may throw
            char sc = source.charAt(i - 1);
            for (int j = 1; j <= tgtLen; j++) {
                // may throw
                char tc = target.charAt(j - 1);
                int cost = (sc == tc) ? 0 : 1;
                int deletion = dp[i - 1][j] + 1;
                int insertion = dp[i][j - 1] + 1;
                int substitution = dp[i - 1][j - 1] + cost;
                int best = Math.min(Math.min(deletion, insertion), substitution);
                if (i > 1 && j > 1 && sc == target.charAt(j - 2) && source.charAt(i - 2) == tc) {
                    best = Math.min(best, dp[i - 2][j - 2] + 1);
                }
                dp[i][j] = best;
            }
        }
        return dp[srcLen][tgtLen];
    }

    private static Stream<Arguments> distanceProvider() {
        return Stream.of(Arguments.of("hello", "hello", 0), Arguments.of("test", "tests", 1), Arguments.of("tests", "test", 1), Arguments.of("flaw", "lawn", 2), Arguments.of("converse", "convers", 1), Arguments.of("converse", "convsere", 2), Arguments.of("book", "back", 2), Arguments.of("Saturday", "Sunday", 3));
    }

    private static Stream<Arguments> jsonBasedProvider() {
        return Stream.of(// Null / Empty handling
        Arguments.of(null, null, 0), Arguments.of(null, "", 0), Arguments.of(null, "abc", 3), Arguments.of("", null, 0), Arguments.of("", "", 0), Arguments.of("", "xyz", 3), Arguments.of("hello", null, 5), Arguments.of("world", "", 5), // Basic without transposition
        Arguments.of("kitten", "sitting", 3), Arguments.of("abc", "abc", 0), Arguments.of("a", "b", 1), Arguments.of("a", "ab", 1), Arguments.of("ab", "a", 1), // Mixed insert/delete/substitution
        Arguments.of("ca", "ac", 1), Arguments.of("ab", "ba", 1), Arguments.of("abcd", "abdc", 1), Arguments.of("example", "exapmle", 1), Arguments.of("abcdef", "abcfde", 2), // Multiple operations with transposition
        Arguments.of("abc", "acb", 1), Arguments.of("abc", "zabc", 1), Arguments.of("abcdef", "abcf", 2), Arguments.of("abcde", "abXde", 1), // Unicode / non‑ASCII
        Arguments.of("aabb", "abab", 1), Arguments.of("acbd", "abcd", 1), // Case sensitivity
        Arguments.of("abc", "bca", 2), Arguments.of("😀", "😀", 0), Arguments.of("😀", "😁", 1), Arguments.of("café", "cafe", 1), Arguments.of("ABC", "abc", 3), // Longer strings / edge lengths
        Arguments.of("HelloWorld", "helloworld", 10), Arguments.of("a b", "ab", 1), Arguments.of("a!b", "ab", 1), Arguments.of("\t\n\r", "", 3), // All‑different characters
        Arguments.of("", "aaaaaaaaaa", 10), Arguments.of("aaaaaaaaaa", "", 10), // Length overflow – expect exception class name
        Arguments.of("abcdefghij", "abcfghij", 2), // Max length sequences – expect NegativeArraySizeException
        Arguments.of(new MaxLengthCharSequence(), "", "java.lang.NegativeArraySizeException"), Arguments.of("", new MaxLengthCharSequence(), "java.lang.NegativeArraySizeException"));
    }

    @Test
    void bothNull_returnsZero() {
        assertEquals(0, damerauLevenshteinDistance(null, null));
    }

    @Test
    void sourceNull_targetNonNull_returnsTargetLength() {
        assertEquals(5, damerauLevenshteinDistance(null, "hello"));
    }

    @Test
    void targetNull_sourceNonNull_returnsSourceLength() {
        assertEquals(4, damerauLevenshteinDistance("test", null));
    }

    @Test
    void bothEmpty_returnsZero() {
        assertEquals(0, damerauLevenshteinDistance("", ""));
    }

    @Test
    void sourceEmpty_targetNonEmpty_returnsTargetLength() {
        assertEquals(3, damerauLevenshteinDistance("", "abc"));
    }

    @Test
    void targetEmpty_sourceNonEmpty_returnsSourceLength() {
        assertEquals(2, damerauLevenshteinDistance("xy", ""));
    }

    @Test
    void identicalStrings_distanceZero() {
        assertEquals(0, damerauLevenshteinDistance("same", "same"));
    }

    @Test
    void simpleSubstitution_distanceOne() {
        assertEquals(1, damerauLevenshteinDistance("cat", "bat"));
    }

    @Test
    void insertionDeletion_distanceTwo() {
        assertEquals(2, damerauLevenshteinDistance("ab", "aXbY"));
    }

    @Test
    void levenshteinExample_distanceThree() {
        assertEquals(3, damerauLevenshteinDistance("kitten", "sitting"));
    }

    @Test
    void transpositionSingleSwap_distanceOne() {
        assertEquals(1, damerauLevenshteinDistance("ca", "ac"));
    }

    @Test
    void transpositionWithinLongerString_distanceOne() {
        assertEquals(1, damerauLevenshteinDistance("abcd", "acbd"));
    }

    @Test
    void transpositionAndSubstitution_distanceTwo() {
        assertEquals(2, damerauLevenshteinDistance("book", "back"));
    }

    @ParameterizedTest
    @MethodSource("distanceProvider")
    void distanceVariousInputs(String source, String target, int expected) {
        assertEquals(expected, damerauLevenshteinDistance(source, target), () -> String.format("Distance between \"%s\" and \"%s\" should be %d", source, target, expected));
    }

    @Test
    void sourceLengthOverflow_throwsNegativeArraySizeException() {
        CharSequence source = new MaxLengthCharSequence();
        CharSequence target = "";
        assertThrows(NegativeArraySizeException.class, () -> damerauLevenshteinDistance(source, target));
    }

    @Test
    void targetLengthOverflow_throwsNegativeArraySizeException() {
        CharSequence source = "";
        CharSequence target = new MaxLengthCharSequence();
        assertThrows(NegativeArraySizeException.class, () -> damerauLevenshteinDistance(source, target));
    }

    @Disabled("Running this test may exhaust the JVM heap; enable only when needed.")
    @Test
    void hugeLengths_mayThrowOutOfMemoryError() {
        final int huge = 1_000_000;
        CharSequence source = new CharSequence() {

            @Override
            public int length() {
                return huge;
            }

            @Override
            public char charAt(int index) {
                return 'a';
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                return this;
            }
        };
        CharSequence target = source;
        assertThrows(OutOfMemoryError.class, () -> damerauLevenshteinDistance(source, target));
    }

    @Test
    void sourceCharAtThrowsIndexOutOfBoundsException_propagates() {
        CharSequence source = new FaultyCharSequence();
        CharSequence target = "abc";
        assertThrows(IndexOutOfBoundsException.class, () -> damerauLevenshteinDistance(source, target));
    }

    @Test
    void targetCharAtThrowsIndexOutOfBoundsException_propagates() {
        CharSequence source = "abc";
        CharSequence target = new FaultyCharSequence();
        assertThrows(IndexOutOfBoundsException.class, () -> damerauLevenshteinDistance(source, target));
    }

    @Test
    void sourceCharAtThrowsRuntimeException_propagates() {
        CharSequence source = new RuntimeFaultyCharSequence();
        CharSequence target = "test";
        assertThrows(RuntimeException.class, () -> damerauLevenshteinDistance(source, target));
    }

    @Test
    void targetCharAtThrowsRuntimeException_propagates() {
        CharSequence source = "test";
        CharSequence target = new RuntimeFaultyCharSequence();
        assertThrows(RuntimeException.class, () -> damerauLevenshteinDistance(source, target));
    }

    @ParameterizedTest(name = "[{index}] {0} → {1} = {2}")
    @MethodSource("jsonBasedProvider")
    void jsonBasedCases(Object source, Object target, Object expected) {
        if (expected instanceof Integer) {
            int exp = (Integer) expected;
            assertEquals(exp, damerauLevenshteinDistance((CharSequence) source, (CharSequence) target));
        } else if (expected instanceof String) {
            // expected is a fully‑qualified exception class name
            Class<? extends Throwable> exClass;
            try {
                exClass = (Class<? extends Throwable>) Class.forName((String) expected);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("Unknown exception class: " + expected, e);
            }
            assertThrows(exClass, () -> damerauLevenshteinDistance((CharSequence) source, (CharSequence) target));
        } else {
            fail("Unexpected expected value: " + expected);
        }
    }
}
