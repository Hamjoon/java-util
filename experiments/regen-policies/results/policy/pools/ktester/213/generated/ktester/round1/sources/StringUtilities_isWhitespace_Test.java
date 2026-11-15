package com.cedarsoftware.util;

import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import static org.junit.jupiter.api.Assertions.*;
import java.util.StringJoiner;
import java.util.stream.Stream;
import java.nio.CharBuffer;
import javax.swing.text.Segment;

@ExtendWith(MockitoExtension.class)
class StringUtilities_isWhitespace_Test {

    /**
     * CharSequence whose {@code length()} throws a {@link RuntimeException}.
     */
    private static final class BadLengthCharSequence implements CharSequence {

        @Override
        public int length() {
            throw new RuntimeException("length() failure");
        }

        @Override
        public char charAt(int index) {
            // Not reached in the test, but required to compile
            return ' ';
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
     * CharSequence whose {@code charAt(int)} always throws {@link IllegalStateException}.
     */
    private static final class BadCharAtCharSequence implements CharSequence {

        @Override
        public int length() {
            return 5;
        }

        @Override
        public char charAt(int index) {
            throw new IllegalStateException("charAt() failure");
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
     * CharSequence with a length that does not match the behavior of {@code charAt}.
     */
    private static final class InconsistentCharSequence implements CharSequence {

        @Override
        public int length() {
            // Claims to have 5 characters
            return 5;
        }

        @Override
        public char charAt(int index) {
            // Simulate an out‑of‑bounds situation even though index < length
            throw new IndexOutOfBoundsException("charAt() out of bounds");
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
     * CharSequence that internally stores a {@code null} array, causing NPE on {@code charAt}.
     */
    private static final class NullCharSequence implements CharSequence {

        private final char[] data = null;

        @Override
        public int length() {
            // Pretends to have one character
            return 1;
        }

        @Override
        public char charAt(int index) {
            // This will trigger a NullPointerException because data is null
            return data[index];
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

    private static Stream<Arguments> whitespaceSequences() {
        return Stream.of(Arguments.of(""), Arguments.of("   "), Arguments.of("\t\n\r\f"), Arguments.of(new StringBuilder(" \t ")), Arguments.of(new StringBuffer("\n\r")), Arguments.of(new Segment(" \t\n".toCharArray(), 0, 4)));
    }

    private static Stream<Arguments> nonWhitespaceSequences() {
        return Stream.of(Arguments.of("a"), Arguments.of(" a "), Arguments.of("\t b"), Arguments.of(new StringBuilder("x ")), Arguments.of(new StringBuffer(" y")), Arguments.of(new Segment("a b".toCharArray(), 0, 3)));
    }

    private static Stream<Arguments> additionalTrueSequences() {
        return Stream.of(// non‑breaking space + EM SPACE
        Arguments.of("\u00A0\u2003"), // line separator + paragraph separator
        Arguments.of("\u2028\u2029"), // StringBuffer with only whitespace
        Arguments.of(new StringBuffer("\t\n ")), // CharBuffer with whitespace
        Arguments.of(CharBuffer.wrap(new char[] { ' ', '\t', '\n' })), // StringJoiner containing only whitespace
        Arguments.of(new StringJoiner("", "", "").add(" ").add("\t").add("\n")), // very long whitespace string
        Arguments.of(" ".repeat(1000)));
    }

    private static Stream<Arguments> additionalFalseSequences() {
        return Stream.of(// zero‑width space (not whitespace)
        Arguments.of("\u200B"), // null character (not whitespace)
        Arguments.of("\u0000"), // EM SPACE surrounded by non‑whitespace
        Arguments.of("a\u2003b"), // EM SPACE followed by null character
        Arguments.of("\u2003\u0000"), // contains a non‑whitespace 'a'
        Arguments.of(new StringBuilder(" \t a")));
    }

    private static Stream<Arguments> exceptionSequences() {
        return Stream.of(Arguments.of(new BadLengthCharSequence(), RuntimeException.class), Arguments.of(new BadCharAtCharSequence(), IllegalStateException.class), Arguments.of(new InconsistentCharSequence(), IndexOutOfBoundsException.class), Arguments.of(new NullCharSequence(), NullPointerException.class));
    }

    public static boolean isWhitespace(CharSequence cs) {
        // Null is considered whitespace
        if (cs == null) {
            return true;
        }
        // Length may throw an exception – let it propagate as required by the tests
        int len = cs.length();
        // Empty sequence is whitespace
        if (len == 0) {
            return true;
        }
        // Examine each character; any non‑whitespace character makes the result false
        for (int i = 0; i < len; i++) {
            // charAt may also throw; we let the exception bubble up
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        // All characters were whitespace
        return true;
    }

    @ParameterizedTest
    @MethodSource("whitespaceSequences")
    void testIsWhitespace_returnsTrue_forOnlyWhitespace(CharSequence cs) {
        assertTrue(StringUtilities.isWhitespace(cs), () -> "Expected true for whitespace sequence: [" + cs + "]");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testIsWhitespace_returnsTrue_forNullOrEmpty(String s) {
        assertTrue(StringUtilities.isWhitespace(s), () -> "Expected true for null or empty string");
    }

    @ParameterizedTest
    @MethodSource("nonWhitespaceSequences")
    void testIsWhitespace_returnsFalse_whenContentPresent(CharSequence cs) {
        assertFalse(StringUtilities.isWhitespace(cs), () -> "Expected false for sequence with content: [" + cs + "]");
    }

    @ParameterizedTest
    @MethodSource("additionalTrueSequences")
    void testIsWhitespace_additionalTrueCases(CharSequence cs) {
        assertTrue(StringUtilities.isWhitespace(cs), () -> "Expected true for additional whitespace sequence: [" + cs + "]");
    }

    @ParameterizedTest
    @MethodSource("additionalFalseSequences")
    void testIsWhitespace_additionalFalseCases(CharSequence cs) {
        assertFalse(StringUtilities.isWhitespace(cs), () -> "Expected false for additional non‑whitespace sequence: [" + cs + "]");
    }

    @ParameterizedTest
    @MethodSource("exceptionSequences")
    void testIsWhitespace_propagatesExceptions(CharSequence cs, Class<? extends Throwable> expected) {
        Throwable thrown = assertThrows(expected, () -> StringUtilities.isWhitespace(cs));
        // Verify that the original exception type is preserved (message is not important)
        assertEquals(expected, thrown.getClass(), () -> "Expected exception of type " + expected.getName() + " to be propagated");
    }
}
