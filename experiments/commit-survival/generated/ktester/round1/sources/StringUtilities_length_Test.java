package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

class StringUtilities_length_Test {

    /**
     * Simple holder for normal (non‑exception) test data.
     */
    private static final class NormalCase {

        final CharSequence input;

        final int expected;

        final String description;

        NormalCase(CharSequence input, int expected, String description) {
            this.input = input;
            this.expected = expected;
            this.description = description;
        }

        NormalCase(String literal, int expected, String description) {
            this.input = literal;
            this.expected = expected;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    /**
     * Simple holder for exception‑expectation test data.
     */
    private static final class ExceptionCase {

        final CharSequence input;

        final Class<? extends Throwable> expectedException;

        final String description;

        ExceptionCase(CharSequence input, Class<? extends Throwable> expectedException, String description) {
            this.input = input;
            this.expectedException = expectedException;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    /**
     * Minimal {@link CharSequence} implementation used for the first set of tests.
     */
    private static final class SimpleCharSequence implements CharSequence {

        private final String data;

        SimpleCharSequence(String data) {
            this.data = data;
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
            return data;
        }
    }

    /**
     * CharSequence backed by a primitive {@code char[]} array.
     */
    private static final class CharArraySequence implements CharSequence {

        private final char[] data;

        CharArraySequence(char[] data) {
            this.data = data.clone();
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
            int len = end - start;
            char[] sub = new char[len];
            System.arraycopy(data, start, sub, 0, len);
            return new CharArraySequence(sub);
        }

        @Override
        public String toString() {
            return new String(data);
        }
    }

    /**
     * Represents a sub‑sequence of a given {@link String}. Mirrors the behaviour of
     * {@code String.subSequence(int, int)} but is a distinct class for clarity in tests.
     */
    private static final class StringSubSequence implements CharSequence {

        private final String source;

        private final int start;

        // exclusive
        private final int end;

        StringSubSequence(String source, int start, int end) {
            if (start < 0 || end > source.length() || start > end) {
                throw new IllegalArgumentException("Invalid start/end for subsequence");
            }
            this.source = source;
            this.start = start;
            this.end = end;
        }

        @Override
        public int length() {
            return end - start;
        }

        @Override
        public char charAt(int index) {
            return source.charAt(start + index);
        }

        @Override
        public CharSequence subSequence(int subStart, int subEnd) {
            return new StringSubSequence(source, start + subStart, start + subEnd);
        }

        @Override
        public String toString() {
            return source.substring(start, end);
        }
    }

    /**
     * CharSequence whose {@code length()} method deliberately throws an exception.
     * The exception is supplied via a {@link Runnable} (or any {@link java.util.function.Supplier})
     * to allow different exception types per test case.
     */
    private static final class ThrowingCharSequence implements CharSequence {

        private final Runnable lengthAction;

        ThrowingCharSequence(Runnable lengthAction) {
            this.lengthAction = lengthAction;
        }

        @Override
        public int length() {
            // will throw
            lengthAction.run();
            // unreachable
            return 0;
        }

        @Override
        public char charAt(int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return "ThrowingCharSequence";
        }
    }

    /**
     * Provides a stream of normal (non‑exception) test data.
     */
    static Stream<NormalCase> normalCases() {
        return Stream.of(// whitespace‑only string
        new NormalCase("   ", 3, "String containing only whitespace should return its actual length"), // CharArraySequence (custom implementation)
        new NormalCase(new CharArraySequence(new char[] { 'a', 'b', 'c', 'd', 'e' }), 5, "CharArraySequence backed by a char array should report correct length"), // Empty StringBuilder
        new NormalCase(new StringBuilder(), 0, "Empty StringBuilder should return 0"), // StringBuffer
        new NormalCase(new StringBuffer("buffer"), 6, "StringBuffer implements CharSequence; length should reflect its content length"), // StringSubSequence (sub‑sequence of a larger string)
        new NormalCase(new StringSubSequence("substring", 2, 5), 3, "Subsequence of a String should return the length of the subsequence"));
    }

    /**
     * Provides a stream of cases where the CharSequence implementation throws.
     */
    static Stream<ExceptionCase> exceptionCases() {
        return Stream.of(new ExceptionCase(new ThrowingCharSequence(() -> {
            throw new NullPointerException("length NPE");
        }), NullPointerException.class, "CharSequence whose length() throws NullPointerException should propagate it"), new ExceptionCase(new ThrowingCharSequence(() -> {
            throw new IllegalArgumentException("invalid length");
        }), IllegalArgumentException.class, "CharSequence whose length() throws IllegalArgumentException should propagate it"), new ExceptionCase(new ThrowingCharSequence(() -> {
            throw new RuntimeException("unexpected error");
        }), RuntimeException.class, "CharSequence whose length() throws RuntimeException should propagate it"));
    }

    @Test
    @DisplayName("length(null) should return 0")
    void lengthReturnsZeroForNull() {
        // given
        CharSequence cs = null;
        // when
        int result = StringUtilities.length(cs);
        // then
        assertEquals(0, result, "Expected length to be 0 when CharSequence is null");
    }

    @Test
    @DisplayName("length(empty String) should return 0")
    void lengthReturnsZeroForEmptyString() {
        // given
        CharSequence cs = "";
        // when
        int result = StringUtilities.length(cs);
        // then
        assertEquals(0, result, "Expected length to be 0 for an empty String");
    }

    @Test
    @DisplayName("length(non‑empty String) should return its length")
    void lengthReturnsCorrectValueForString() {
        // given
        String cs = "OpenAI";
        // when
        int result = StringUtilities.length(cs);
        // then
        assertEquals(cs.length(), result, "Length should match String.length()");
    }

    @Test
    @DisplayName("length(StringBuilder) should return its length")
    void lengthWorksWithStringBuilder() {
        // given
        StringBuilder cs = new StringBuilder("builder");
        // when
        int result = StringUtilities.length(cs);
        // then
        assertEquals(cs.length(), result, "Length should match StringBuilder.length()");
    }

    @Test
    @DisplayName("length(custom CharSequence) should return its length")
    void lengthWorksWithCustomCharSequence() {
        // given
        CharSequence cs = new SimpleCharSequence("custom");
        // when
        int result = StringUtilities.length(cs);
        // then
        assertEquals(cs.length(), result, "Length should match custom CharSequence.length()");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("normalCases")
    @DisplayName("length(CharSequence) – additional normal cases")
    void lengthAdditionalNormalCases(NormalCase tc) {
        int result = StringUtilities.length(tc.input);
        assertEquals(tc.expected, result, tc.description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("exceptionCases")
    @DisplayName("length(CharSequence) – exception propagation")
    void lengthExceptionPropagation(ExceptionCase ec) {
        assertThrows(ec.expectedException, () -> StringUtilities.length(ec.input), ec.description);
    }
}
