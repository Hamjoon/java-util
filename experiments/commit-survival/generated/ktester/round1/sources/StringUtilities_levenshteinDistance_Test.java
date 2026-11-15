package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringUtilities_levenshteinDistance_Test {

    /**
     * A {@link CharSequence} whose {@code length()} returns a negative value.
     * Used to provoke {@link NegativeArraySizeException} when the algorithm
     * allocates the work vectors.
     */
    private static final class BadLengthCharSequence implements CharSequence {

        private final String data;

        BadLengthCharSequence(String data) {
            this.data = data;
        }

        @Override
        public int length() {
            // illegal length
            return -1;
        }

        @Override
        public char charAt(int index) {
            return data.charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return data.subSequence(start, end);
        }

        @Override
        public String toString() {
            return data;
        }
    }

    /**
     * A {@link CharSequence} that throws a {@link RuntimeException}
     * from {@code charAt(int)}. This checks that the method does not
     * swallow unexpected runtime exceptions.
     */
    private static final class BadCharAtException implements CharSequence {

        private final String data;

        BadCharAtException(String data) {
            this.data = data;
        }

        @Override
        public int length() {
            return data.length();
        }

        @Override
        public char charAt(int index) {
            throw new RuntimeException("Intentional failure in charAt");
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return data.subSequence(start, end);
        }

        @Override
        public String toString() {
            return data;
        }
    }

    private static Stream<Arguments> nullAndEmptyProvider() {
        return Stream.of(Arguments.of("Both inputs are null – distance 0", null, null, 0), Arguments.of("First null, second empty – distance 0", null, "", 0), Arguments.of("First null, second non‑empty – distance equals length of second", null, "abc", 3), Arguments.of("First empty, second null – distance 0", "", null, 0), Arguments.of("First empty, second non‑empty – distance equals length of second", "", "xyz", 3), Arguments.of("Second null, first non‑empty – distance equals length of first", "hello", null, 5), Arguments.of("Second empty, first non‑empty – distance equals length of first", "world", "", 5));
    }

    private static Stream<Arguments> standardCasesProvider() {
        return Stream.of(Arguments.of("kitten → sitting (distance 3)", "kitten", "sitting", 3), Arguments.of("flaw → lawn (distance 2)", "flaw", "lawn", 2), Arguments.of("intention → execution (distance 5)", "intention", "execution", 5), Arguments.of("Identical strings", "abc", "abc", 0), Arguments.of("Deletion of one character", "abc", "ab", 1), Arguments.of("Insertion of one character", "ab", "abc", 1), Arguments.of("Single‑character substitution", "a", "b", 1), Arguments.of("Empty first, non‑empty second", "", "nonempty", 8), Arguments.of("Non‑empty first, empty second", "nonempty", "", 8), Arguments.of("Two transpositions counted as two edits", "abcd", "abdc", 2), Arguments.of("Unicode emoji substitution", "😀🐶", "😀🐱", 1), // a + combining grave vs precomposed
        Arguments.of("Different Unicode normalization forms", "à", "à", 1));
    }

    @Test
    void bothNull_ReturnsZero() {
        assertEquals(0, StringUtilities.levenshteinDistance(null, null));
    }

    @Test
    void oneNullOtherEmpty_ReturnsZero() {
        assertEquals(0, StringUtilities.levenshteinDistance(null, ""));
        assertEquals(0, StringUtilities.levenshteinDistance("", null));
    }

    @Test
    void oneNullOtherNonEmpty_ReturnsLengthOfNonEmpty() {
        assertEquals(5, StringUtilities.levenshteinDistance(null, "hello"));
        assertEquals(3, StringUtilities.levenshteinDistance("cat", null));
    }

    @Test
    void bothEmpty_ReturnsZero() {
        assertEquals(0, StringUtilities.levenshteinDistance("", ""));
    }

    @Test
    void emptyAndNonEmpty_ReturnsLengthOfNonEmpty() {
        assertEquals(4, StringUtilities.levenshteinDistance("", "test"));
        assertEquals(2, StringUtilities.levenshteinDistance("ab", ""));
    }

    @Test
    void identicalStrings_ReturnsZero() {
        assertEquals(0, StringUtilities.levenshteinDistance("same", "same"));
        assertEquals(0, StringUtilities.levenshteinDistance("12345", "12345"));
    }

    @Test
    void singleEditInsertions() {
        assertEquals(1, StringUtilities.levenshteinDistance("cat", "cats"));
        assertEquals(1, StringUtilities.levenshteinDistance("cat", "acat"));
    }

    @Test
    void singleEditDeletions() {
        assertEquals(1, StringUtilities.levenshteinDistance("cats", "cat"));
        assertEquals(1, StringUtilities.levenshteinDistance("acat", "cat"));
    }

    @Test
    void singleEditSubstitutions() {
        assertEquals(1, StringUtilities.levenshteinDistance("cat", "cut"));
        assertEquals(1, StringUtilities.levenshteinDistance("kitten", "sitten"));
    }

    @Test
    void multipleEdits_MixedOperations() {
        // classic Levenshtein example: "kitten" → "sitting" (distance 3)
        assertEquals(3, StringUtilities.levenshteinDistance("kitten", "sitting"));
        // "flaw" → "lawn" (distance 2)
        assertEquals(2, StringUtilities.levenshteinDistance("flaw", "lawn"));
    }

    @Test
    void unicodeCharacters() {
        // Emoji and accented characters should be treated as single code units
        assertEquals(1, StringUtilities.levenshteinDistance("café", "cafe"));
        assertEquals(1, StringUtilities.levenshteinDistance("😀", "😁"));
        assertEquals(2, StringUtilities.levenshteinDistance("😀a", "😁b"));
    }

    @Test
    void longStringsPerformanceCheck() {
        // Build two long strings that differ by a known number of edits
        StringBuilder sb1 = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        int edits = 0;
        for (int i = 0; i < 1000; i++) {
            sb1.append('a');
            if (i % 100 == 0) {
                // substitution
                sb2.append('b');
                edits++;
            } else {
                sb2.append('a');
            }
        }
        assertEquals(edits, StringUtilities.levenshteinDistance(sb1, sb2));
    }

    // ----- Null and Empty Handling -------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullAndEmptyProvider")
    void nullAndEmptyHandling(String description, CharSequence s, CharSequence t, int expected) {
        assertEquals(expected, StringUtilities.levenshteinDistance(s, t), description);
    }

    // ----- Standard Cases -----------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("standardCasesProvider")
    void standardCases(String description, CharSequence s, CharSequence t, int expected) {
        assertEquals(expected, StringUtilities.levenshteinDistance(s, t), description);
    }

    @Test
    void negativeLengthFirstArgument_throwsNegativeArraySizeException() {
        CharSequence bad = new BadLengthCharSequence("BadLengthCharSequence");
        assertThrows(NegativeArraySizeException.class, () -> StringUtilities.levenshteinDistance(bad, "abc"));
    }

    @Test
    void negativeLengthSecondArgument_throwsNegativeArraySizeException() {
        CharSequence bad = new BadLengthCharSequence("BadLengthCharSequence");
        assertThrows(NegativeArraySizeException.class, () -> StringUtilities.levenshteinDistance("abc", bad));
    }

    @Test
    void charAtExceptionFirstArgument_propagatesRuntimeException() {
        CharSequence bad = new BadCharAtException("BadCharAtException");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> StringUtilities.levenshteinDistance(bad, "abc"));
        assertEquals("Intentional failure in charAt", ex.getMessage());
    }

    @Test
    void charAtExceptionSecondArgument_propagatesRuntimeException() {
        CharSequence bad = new BadCharAtException("BadCharAtException");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> StringUtilities.levenshteinDistance("abc", bad));
        assertEquals("Intentional failure in charAt", ex.getMessage());
    }
}
