package com.cedarsoftware.util;

import java.util.stream.Stream;
import java.nio.CharBuffer;
// import org.junit.Test;
// import static org.junit.Assert.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import static org.junit.jupiter.api.Assertions.*;

class StringUtilities_isEmpty_Test {

    /**
     * Helper holder for the additional‑whitespace test data.
     */
    private static final class AdditionalWhitespace {

        final CharSequence cs;

        final boolean expectedIsEmpty;

        AdditionalWhitespace(CharSequence cs, boolean expectedIsEmpty) {
            this.cs = cs;
            this.expectedIsEmpty = expectedIsEmpty;
        }
    }

    /**
     * Helper holder for the exception‑propagation test data.
     */
    private static final class ExceptionCase {

        final CharSequence cs;

        final Class<? extends Throwable> expectedException;

        ExceptionCase(CharSequence cs, Class<? extends Throwable> expectedException) {
            this.cs = cs;
            this.expectedException = expectedException;
        }
    }

    /**
     * Simple {@link CharSequence} that delegates to an underlying {@link String}.
     */
    private static final class CustomCharSequence implements CharSequence {

        private final String data;

        CustomCharSequence(String data) {
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
     * CharSequence whose {@code length()} method always throws a {@link RuntimeException}.
     */
    private static final class ExceptionThrowingLengthCharSequence implements CharSequence {

        @Override
        public int length() {
            throw new RuntimeException("length() failure");
        }

        // never reached
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
     * CharSequence whose {@code charAt(int)} method always throws a {@link RuntimeException}.
     */
    private static final class ExceptionThrowingCharAtCharSequence implements CharSequence {

        private final String backing;

        ExceptionThrowingCharAtCharSequence(String backing) {
            this.backing = backing;
        }

        @Override
        public int length() {
            return backing.length();
        }

        @Override
        public char charAt(int index) {
            throw new RuntimeException("charAt() failure at index " + index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return backing.subSequence(start, end);
        }

        @Override
        public String toString() {
            return backing;
        }
    }

    /**
     * CharSequence that reports a negative length. The {@code charAt(int)} implementation
     * will be called with a negative index, causing {@link IndexOutOfBoundsException}.
     */
    private static final class NegativeLengthCharSequence implements CharSequence {

        private final String data;

        NegativeLengthCharSequence(String data) {
            this.data = data;
        }

        // illegal length
        @Override
        public int length() {
            return -1;
        }

        // will throw IOBE
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

    private static Stream<Arguments> charSequencesWithOnlyWhitespace() {
        return Stream.of(Arguments.of("   "), Arguments.of("\t\n\r"), Arguments.of("\u2003\u3000"), Arguments.of(new StringBuilder("   ")), Arguments.of(new StringBuilder("\t\n\r")), Arguments.of(new java.lang.StringBuffer("   ")));
    }

    private static Stream<Arguments> charSequencesWithContent() {
        return Stream.of(Arguments.of("a"), Arguments.of(" a "), Arguments.of("text"), Arguments.of("  x  "), Arguments.of(new StringBuilder("a")), Arguments.of(new StringBuilder("  b  ")), Arguments.of(new java.lang.StringBuffer("c")));
    }

    private static Stream<Arguments> additionalWhitespaceSequences() {
        return Stream.of(// EN QUAD + EM QUAD – whitespace (both are whitespace)
        Arguments.of(new StringBuilder("\u2000\u2001"), true), // ZERO WIDTH SPACE – NOT whitespace
        Arguments.of(new StringBuilder("\u200B"), false), // space, tab, LF – all whitespace
        Arguments.of(CharBuffer.wrap(" \t\n".toCharArray()), true), // single non‑whitespace character
        Arguments.of(CharBuffer.wrap("a".toCharArray()), false), // three spaces
        Arguments.of(new CustomCharSequence("   "), true), // space, 'a', space
        Arguments.of(new CustomCharSequence(" a "), false), // LINE & PARAGRAPH SEPARATOR – whitespace
        Arguments.of(" \u2028\u2029 ", true), // contains a non‑whitespace char
        Arguments.of(" \u2028a", false), // ZERO WIDTH SPACE – not whitespace
        Arguments.of(" \u200B ", false), // FORM FEED – whitespace
        Arguments.of(" \f ", true), // VERTICAL TAB – whitespace
        Arguments.of(" \u000B ", true));
    }

    private static Stream<Arguments> exceptionThrowingSequences() {
        return Stream.of(Arguments.of(new ExceptionThrowingLengthCharSequence(), RuntimeException.class), Arguments.of(new ExceptionThrowingCharAtCharSequence("abc"), RuntimeException.class), Arguments.of(new NegativeLengthCharSequence("   "), IndexOutOfBoundsException.class));
    }

    @ParameterizedTest
    @MethodSource("charSequencesWithOnlyWhitespace")
    void testIsEmpty_whenOnlyWhitespace_returnsTrue(CharSequence cs) {
        assertTrue(StringUtilities.isEmpty(cs), "Expected true for whitespace‑only CharSequence: [" + cs + "]");
    }

    @ParameterizedTest
    @MethodSource("charSequencesWithContent")
    void testIsEmpty_whenContainsNonWhitespace_returnsFalse(CharSequence cs) {
        assertFalse(StringUtilities.isEmpty(cs), "Expected false for CharSequence with content: [" + cs + "]");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testIsEmpty_whenNullOrEmpty_returnsTrue(String s) {
        // null case and empty string case are both covered by the annotation
        assertTrue(StringUtilities.isEmpty(s), "Expected true for null or empty input");
    }

    @ParameterizedTest
    @MethodSource("additionalWhitespaceSequences")
    void testIsEmpty_additionalWhitespaceVariations(CharSequence cs, boolean expectedIsEmpty) {
        if (expectedIsEmpty) {
            assertTrue(StringUtilities.isEmpty(cs), "Expected true for whitespace‑only sequence: [" + cs + "]");
        } else {
            assertFalse(StringUtilities.isEmpty(cs), "Expected false for sequence containing non‑whitespace: [" + cs + "]");
        }
    }

    @ParameterizedTest
    @MethodSource("exceptionThrowingSequences")
    void testIsEmpty_propagatesExceptions(CharSequence cs, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> StringUtilities.isEmpty(cs));
    }
}
