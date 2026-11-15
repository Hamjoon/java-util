package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

// kept from the template, though not used
@ExtendWith(MockitoExtension.class)
class StringUtilities_count_2_Test {

    /**
     * A {@link CharSequence} that throws a {@link RuntimeException}
     * (or a wrapped checked exception) when {@code toString()} is invoked.
     */
    private static final class FaultyCharSequence implements CharSequence {

        private final String data;

        private final boolean wrapChecked;

        /**
         * @param data          underlying string data (unused, but required by the interface)
         * @param wrapChecked   if true, a checked exception is wrapped in a RuntimeException;
         *                      if false, a plain RuntimeException is thrown.
         */
        FaultyCharSequence(String data, boolean wrapChecked) {
            this.data = data;
            this.wrapChecked = wrapChecked;
        }

        @Override
        public int length() {
            return data.length();
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
            if (wrapChecked) {
                try {
                    throw new java.io.IOException("checked exception");
                } catch (java.io.IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new RuntimeException("Faulty CharSequence");
            }
        }
    }

    private static Stream<Arguments> nullOrEmptyProvider() {
        return Stream.of(Arguments.of(null, null), Arguments.of(null, "abc"), Arguments.of("abc", null), Arguments.of("", "a"), Arguments.of("a", ""), Arguments.of("", ""), Arguments.of(null, ""), Arguments.of("", null));
    }

    private static Stream<Arguments> typicalScenariosProvider() {
        return Stream.of(Arguments.of("hello world", "o", 2), Arguments.of("ababab", "ab", 3), Arguments.of("abc", "abc", 1), Arguments.of("abc", "abcd", 0), Arguments.of("aaaaa", "aa", 4), Arguments.of("😀😁😀😁", "😀", 2), Arguments.of("CaseSensitive", "casesensitive", 0));
    }

    @ParameterizedTest
    @MethodSource("nullOrEmptyProvider")
    void testCount_nullOrEmptyInputs_returnsZero(CharSequence content, CharSequence token) {
        assertEquals(0, StringUtilities.count(content, token));
    }

    @Test
    void testCount_tokenLongerThanContent_returnsZero() {
        assertEquals(0, StringUtilities.count("hi", "hello"));
    }

    @Test
    void testCount_singleOccurrence() {
        assertEquals(1, StringUtilities.count("hello world", "world"));
        assertEquals(1, StringUtilities.count("abc", "a"));
    }

    @Test
    void testCount_multipleNonOverlappingOccurrences() {
        assertEquals(3, StringUtilities.count("one,two,three,two,one", "one"));
        assertEquals(2, StringUtilities.count("ababab", "ab"));
    }

    @Test
    void testCount_overlappingOccurrences() {
        // "aaa" contains "aa" twice (positions 0‑1 and 1‑2)
        assertEquals(2, StringUtilities.count("aaa", "aa"));
        // "aaaaa" contains "aa" four times overlapping
        assertEquals(4, StringUtilities.count("aaaaa", "aa"));
    }

    @Test
    void testCount_withStringBuilder() {
        CharSequence content = new StringBuilder("abcabcabc");
        CharSequence token = new StringBuilder("abc");
        assertEquals(3, StringUtilities.count(content, token));
    }

    @Test
    void testCount_withStringBuffer() {
        CharSequence content = new StringBuffer("xxxxxx");
        CharSequence token = new StringBuffer("xx");
        // Overlapping: positions 0‑1,1‑2,2‑3,3‑4,4‑5 => 5 matches
        assertEquals(5, StringUtilities.count(content, token));
    }

    @Test
    void testCount_contentWithWhitespace() {
        assertEquals(2, StringUtilities.count(" a  a ", "a"));
        assertEquals(1, StringUtilities.count("\t\nabc\t\n", "\t\n"));
    }

    @ParameterizedTest
    @MethodSource("typicalScenariosProvider")
    void testCount_typicalScenarios(CharSequence content, CharSequence token, int expected) {
        assertEquals(expected, StringUtilities.count(content, token));
    }

    @Test
    void testCount_contentThrowsRuntimeException_propagates() {
        CharSequence faultyContent = new FaultyCharSequence("any content", false);
        CharSequence token = "any";
        assertThrows(RuntimeException.class, () -> StringUtilities.count(faultyContent, token));
    }

    @Test
    void testCount_tokenThrowsRuntimeException_propagates() {
        CharSequence content = "any";
        CharSequence faultyToken = new FaultyCharSequence("any token", false);
        assertThrows(RuntimeException.class, () -> StringUtilities.count(content, faultyToken));
    }

    @Test
    void testCount_contentThrowsWrappedCheckedException_propagates() {
        CharSequence faultyContent = new FaultyCharSequence("any content", true);
        CharSequence token = "any";
        assertThrows(RuntimeException.class, () -> StringUtilities.count(faultyContent, token));
    }

    @Test
    void testCount_tokenThrowsWrappedCheckedException_propagates() {
        CharSequence content = "any";
        CharSequence faultyToken = new FaultyCharSequence("any token", true);
        assertThrows(RuntimeException.class, () -> StringUtilities.count(content, faultyToken));
    }
}
