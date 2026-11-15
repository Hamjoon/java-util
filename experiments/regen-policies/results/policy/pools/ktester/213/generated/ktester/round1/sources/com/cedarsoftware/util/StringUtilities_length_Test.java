package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.function.Executable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import java.nio.CharBuffer;

class StringUtilities_length_Test {

    /**
     * Simple custom {@link CharSequence} that reports a fixed length.
     */
    private static final class CustomCharSequence implements CharSequence {

        private final int length;

        CustomCharSequence(int length) {
            this.length = length;
        }

        @Override
        public int length() {
            return length;
        }

        @Override
        public char charAt(int index) {
            // Not needed for length tests; return a dummy value
            return 'x';
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            // Not needed for length tests
            return this;
        }

        @Override
        public String toString() {
            // Produce a string of the requested length for debugging
            return "x".repeat(Math.max(0, length));
        }
    }

    /**
     * {@link CharSequence} backed by a {@code char[]} array.
     */
    private static final class CharArraySequence implements CharSequence {

        private final char[] data;

        CharArraySequence(char[] data) {
            this.data = data != null ? data.clone() : new char[0];
        }

        @Override
        public int length() {
            return data.length;
        }

        @Override
        public char charAt(int index) {
            return data[index];
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            int newLen = end - start;
            char[] sub = new char[newLen];
            System.arraycopy(data, start, sub, 0, newLen);
            return new CharArraySequence(sub);
        }

        @Override
        public String toString() {
            return new String(data);
        }
    }

    /**
     * {@link CharSequence} whose {@code length()} deliberately throws {@link NullPointerException}.
     */
    private static final class NPEThrowingCharSequence implements CharSequence {

        @Override
        public int length() {
            throw new NullPointerException("Deliberate NPE from length()");
        }

        @Override
        public char charAt(int index) {
            return 'x';
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return this;
        }

        @Override
        public String toString() {
            return "";
        }
    }

    /**
     * Supplies a stream of {@link Arguments} containing the {@link CharSequence}
     * instances described in the test cases together with their expected lengths.
     */
    private static Stream<Arguments> provideCharSequences() {
        return Stream.of(Arguments.of("", 0), Arguments.of(new StringBuilder(""), 0), Arguments.of("abc", 3), Arguments.of(new StringBuilder("hello"), 5), Arguments.of(new CustomCharSequence(7), 7), Arguments.of(" ", 1), Arguments.of("\u0000", 1), // 😀 (surrogate pair)
        Arguments.of("\uD83D\uDE00", 2), Arguments.of(new CharArraySequence(new char[] { 'a', 'b', 'c', 'd', 'e' }), 5), Arguments.of(new CharArraySequence(new char[] {}), 0), Arguments.of(new StringBuilder("a\n\tb"), 4), Arguments.of(new AtomicReference<>("test").get(), 4), Arguments.of(new java.util.Formatter().format("%d", 12345).toString(), 5), Arguments.of(CharBuffer.wrap("buffer"), 6), Arguments.of(String.valueOf(1234567890L), 10));
    }

    /**
     * Returns the length of the supplied {@link CharSequence}, or {@code 0}
     * if the argument is {@code null}. This mirrors the behaviour described
     * in the API documentation for {@code StringUtilities.length(CharSequence)}.
     *
     * @param cs a {@link CharSequence} or {@code null}
     * @return the length of {@code cs}, or {@code 0} when {@code cs} is {@code null}
     */
    public static int length(CharSequence cs) {
        return cs == null ? 0 : cs.length();
    }

    @Test
    void length_null_returnsZero() {
        assertEquals(0, StringUtilities.length(null));
    }

    @ParameterizedTest
    @ValueSource(strings = { "" })
    void length_emptyString_returnsZero(String input) {
        assertEquals(0, StringUtilities.length(input));
    }

    @ParameterizedTest
    @ValueSource(strings = { "a", "abc", "Hello, World!", "   leading and trailing spaces   ", "\u2603\u2603\u2603" })
    void length_nonEmptyString_returnsActualLength(String input) {
        assertEquals(input.length(), StringUtilities.length(input));
    }

    @Test
    void length_stringBuilder_returnsCorrectLength() {
        StringBuilder sb = new StringBuilder("StringBuilder content");
        assertEquals(sb.length(), StringUtilities.length(sb));
    }

    @Test
    void length_surrogatePair_countsCodeUnits() {
        // surrogate pair, length == 2
        String emoji = "😀";
        assertEquals(2, emoji.length());
        assertEquals(2, StringUtilities.length(emoji));
    }

    @ParameterizedTest(name = "{index} => cs={0}, expected={1}")
    @MethodSource("provideCharSequences")
    void length_variousCharSequences(CharSequence cs, int expected) {
        assertEquals(expected, StringUtilities.length(cs));
    }

    @Test
    void length_NPEThrowingCharSequence_propagatesException() {
        CharSequence cs = new NPEThrowingCharSequence();
        Executable call = () -> StringUtilities.length(cs);
        assertThrows(NullPointerException.class, call);
    }
}
