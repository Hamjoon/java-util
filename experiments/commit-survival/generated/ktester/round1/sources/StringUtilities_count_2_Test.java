package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringUtilities_count_2_Test {

    /**
     * A {@link CharSequence} whose {@code toString()} method throws a {@link RuntimeException}.
     */
    private static final class FaultyToStringCharSequence implements CharSequence {

        private final String delegate;

        FaultyToStringCharSequence(String delegate) {
            this.delegate = delegate;
        }

        @Override
        public int length() {
            return delegate.length();
        }

        @Override
        public char charAt(int index) {
            return delegate.charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return delegate.subSequence(start, end);
        }

        @Override
        public String toString() {
            // Different messages allow us to know which argument caused the failure
            if (delegate.equals("any")) {
                throw new RuntimeException("Content toString failure");
            }
            throw new RuntimeException("Token toString failure");
        }
    }

    /**
     * A {@link CharSequence} whose {@code toString()} method returns {@code null}.
     */
    private static final class NullReturningCharSequence implements CharSequence {

        private final String delegate;

        NullReturningCharSequence(String delegate) {
            this.delegate = delegate;
        }

        @Override
        public int length() {
            return delegate.length();
        }

        @Override
        public char charAt(int index) {
            return delegate.charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return delegate.subSequence(start, end);
        }

        @Override
        public String toString() {
            // Intentional null to trigger NPE inside StringUtilities.count
            return null;
        }
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> nullAndEmptyProvider() {
        return Stream.of(// both null
        org.junit.jupiter.params.provider.Arguments.of(null, null, 0), // content null
        org.junit.jupiter.params.provider.Arguments.of(null, "any", 0), // token null
        org.junit.jupiter.params.provider.Arguments.of("content", null, 0), // content empty
        org.junit.jupiter.params.provider.Arguments.of("", "any", 0), // token empty
        org.junit.jupiter.params.provider.Arguments.of("some content", "", 0));
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> normalCountingProvider() {
        return Stream.of(// multiple occurrences (including overlapping)
        org.junit.jupiter.params.provider.Arguments.of("hello world", "l", 3), // token longer than content
        org.junit.jupiter.params.provider.Arguments.of("short", "much longer token", 0), // token equals content
        org.junit.jupiter.params.provider.Arguments.of("exactmatch", "exactmatch", 1), // overlapping token
        org.junit.jupiter.params.provider.Arguments.of("aaa", "aa", 2), // token not present
        org.junit.jupiter.params.provider.Arguments.of("abcdef", "z", 0), // token appears at start and end
        org.junit.jupiter.params.provider.Arguments.of("token middle token", "token", 2), // token appears multiple non‑overlapping times
        org.junit.jupiter.params.provider.Arguments.of("cat dog cat dog cat", "cat", 3), org.junit.jupiter.params.provider.Arguments.of("ababab", "ab", 3), // overlapping complex example
        org.junit.jupiter.params.provider.Arguments.of("abababa", "aba", 3), // mixed overlap "aaaa" with "aa"
        org.junit.jupiter.params.provider.Arguments.of("aaaa", "aa", 3), // whitespace token
        org.junit.jupiter.params.provider.Arguments.of("   ", " ", 3), // repeated non‑overlapping token
        org.junit.jupiter.params.provider.Arguments.of("ababababab", "ab", 5));
    }

    @Test
    void count_whenBothArgumentsAreNull_returnsZero() {
        assertEquals(0, StringUtilities.count(null, null));
    }

    @Test
    void count_whenContentIsNull_returnsZero() {
        assertEquals(0, StringUtilities.count(null, "token"));
    }

    @Test
    void count_whenTokenIsNull_returnsZero() {
        assertEquals(0, StringUtilities.count("content", null));
    }

    @Test
    void count_whenContentIsEmpty_returnsZero() {
        assertEquals(0, StringUtilities.count("", "any"));
    }

    @Test
    void count_whenTokenIsEmpty_returnsZero() {
        assertEquals(0, StringUtilities.count("some content", ""));
    }

    @Test
    void count_whenTokenAppearsMultipleTimes_countsCorrectly() {
        assertEquals(3, StringUtilities.count("hello world", "l"));
    }

    @Test
    void count_whenTokenLongerThanContent_returnsZero() {
        assertEquals(0, StringUtilities.count("short", "much longer token"));
    }

    @Test
    void count_whenTokenEqualsContent_returnsOne() {
        assertEquals(1, StringUtilities.count("exactmatch", "exactmatch"));
    }

    @Test
    void count_whenTokenAppearsOverlapping_countsEachOccurrence() {
        // "aaa" contains "aa" at positions 0 and 1 (overlap)
        assertEquals(2, StringUtilities.count("aaa", "aa"));
    }

    @Test
    void count_whenTokenIsSingleCharacterAndContentHasNoMatch_returnsZero() {
        assertEquals(0, StringUtilities.count("abcdef", "z"));
    }

    @Test
    void count_withCharSequenceImplementations_countsCorrectly() {
        CharSequence content = new StringBuilder("ababa");
        CharSequence token = new StringBuilder("aba");
        // "aba" appears twice in "ababa" (positions 0 and 2, overlapping)
        assertEquals(2, StringUtilities.count(content, token));
    }

    @ParameterizedTest(name = "{index}: content={0}, token={1} => expected={2}")
    @MethodSource("nullAndEmptyProvider")
    void count_nullAndEmptyInputs(Object contentObj, Object tokenObj, int expected) {
        CharSequence content = (CharSequence) contentObj;
        CharSequence token = (CharSequence) tokenObj;
        assertEquals(expected, StringUtilities.count(content, token));
    }

    @ParameterizedTest(name = "{index}: content=\"{0}\", token=\"{1}\" => expected={2}")
    @MethodSource("normalCountingProvider")
    void count_normalScenarios(String content, String token, int expected) {
        assertEquals(expected, StringUtilities.count(content, token));
    }

    @Test
    void count_caseSensitivity() {
        // Different case – should be case‑sensitive → 0 matches
        assertEquals(0, StringUtilities.count("CaseSensitive", "casesensitive"));
        // Exact case – 1 match
        assertEquals(1, StringUtilities.count("CaseSensitive", "CaseSensitive"));
    }

    @Test
    void count_tokenLongerThanContent() {
        assertEquals(0, StringUtilities.count("abc", "abcd"));
        assertEquals(0, StringUtilities.count("a", "abc"));
    }

    @Test
    void count_tokenEqualsContent() {
        assertEquals(1, StringUtilities.count("same", "same"));
    }

    @Test
    void count_differentCharSequenceTypes_builderContent() {
        CharSequence content = new StringBuilder("buildercontentbuilder");
        CharSequence token = new StringBuilder("builder");
        // Two non‑overlapping occurrences
        assertEquals(2, StringUtilities.count(content, token));
    }

    @Test
    void count_unicodeSurrogatePairs() {
        // Emoji (each emoji is a surrogate pair)
        assertEquals(2, StringUtilities.count("😀😃😀😃", "😀"));
        // Mathematical bold Unicode characters
        assertEquals(1, StringUtilities.count("𝔘𝔫𝔦𝔠𝔬𝔡𝔢", "𝔘"));
    }

    @Test
    void count_contentToStringThrowsRuntimeException() {
        CharSequence faulty = new FaultyToStringCharSequence("any");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> StringUtilities.count(faulty, "any"));
        assertEquals("Content toString failure", ex.getMessage());
    }

    @Test
    void count_tokenToStringThrowsRuntimeException() {
        CharSequence faulty = new FaultyToStringCharSequence("token");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> StringUtilities.count("any", faulty));
        assertEquals("Token toString failure", ex.getMessage());
    }

    @Test
    void count_contentToStringReturnsNull() {
        CharSequence nullReturning = new NullReturningCharSequence("any");
        // The implementation will return null, leading to a NullPointerException
        assertThrows(NullPointerException.class, () -> StringUtilities.count(nullReturning, "any"));
    }

    @Test
    void count_tokenToStringReturnsNull() {
        CharSequence nullReturning = new NullReturningCharSequence("token");
        assertThrows(NullPointerException.class, () -> StringUtilities.count("any", nullReturning));
    }
}
