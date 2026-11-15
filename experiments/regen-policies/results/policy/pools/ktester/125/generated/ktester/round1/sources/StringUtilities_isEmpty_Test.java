package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.CharBuffer;

@ExtendWith(MockitoExtension.class)
class StringUtilities_isEmpty_Test {

    /**
     * CharSequence with zero length.
     */
    private static final class EmptyCharSequence implements CharSequence {

        @Override
        public int length() {
            return 0;
        }

        @Override
        public char charAt(int index) {
            throw new IndexOutOfBoundsException();
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
     * CharSequence that delegates to an underlying String.
     */
    private static final class WhitespaceCharSequence implements CharSequence {

        private final String data;

        WhitespaceCharSequence(String data) {
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
     * CharSequence whose length() throws a RuntimeException.
     */
    private static final class FaultyCharSequenceLength implements CharSequence {

        @Override
        public int length() {
            throw new RuntimeException("length() failure");
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
            return "faulty";
        }
    }

    /**
     * CharSequence whose charAt(int) throws a RuntimeException.
     */
    private static final class FaultyCharSequenceCharAt implements CharSequence {

        @Override
        public int length() {
            return 5;
        }

        @Override
        public char charAt(int index) {
            throw new RuntimeException("charAt() failure");
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
     * CharSequence that throws UnsupportedOperationException for charAt.
     */
    private static final class UnsupportedOperationCharSequence implements CharSequence {

        @Override
        public int length() {
            return 3;
        }

        @Override
        public char charAt(int index) {
            throw new UnsupportedOperationException("charAt not supported");
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return "unsupported";
        }
    }

    /* --------------------------------------------------------------------- */
    private static Stream<String> stringsWithAllWhitespace() {
        return Stream.of(// single space
        " ", // tab
        "\t", // newline
        "\n", // Windows line break
        "\r\n", // mixed whitespace
        "   \t  \n  ", // em‑space (Unicode whitespace)
        "\u2003", // ideographic space
        "\u3000");
    }

    private static Stream<String> stringsWithContentOtherThanWhitespace() {
        return Stream.of(// single non‑whitespace character
        "a", // whitespace surrounding content
        "  a  ", // regular word
        "foo", // mixed whitespace with content
        " \tfoo\n", // numeric character
        "0", // literal text
        "null", // non‑breaking space + content
        "\u00A0a", // CharSequence implementation with content
        new StringBuilder("builder").toString());
    }

    private static Stream<Arguments> unicodeWhitespaceProvider() {
        return Stream.of(// NBSP – not recognized as whitespace by Character.isWhitespace
        Arguments.of("\u00A0", false), // Unicode space separators
        Arguments.of("\u2000\u2001\u2002", true), // line separator & paragraph separator
        Arguments.of("\u2028\u2029", true));
    }

    private static Stream<Arguments> zeroWidthProvider() {
        return Stream.of(// zero‑width space
        Arguments.of("\u200B", false), // zero‑width non‑joiner
        Arguments.of("\u200C", false));
    }

    private static Stream<Arguments> customCharSequenceProvider() {
        return Stream.of(Arguments.of(new EmptyCharSequence(), true), Arguments.of(new WhitespaceCharSequence("\t \n"), true), Arguments.of(new WhitespaceCharSequence("a\t"), false));
    }

    private static Stream<Arguments> charBufferProvider() {
        return Stream.of(Arguments.of(CharBuffer.wrap("   "), true), Arguments.of(CharBuffer.wrap("  x  "), false));
    }

    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(Arguments.of(new FaultyCharSequenceLength(), RuntimeException.class), Arguments.of(new FaultyCharSequenceCharAt(), RuntimeException.class), Arguments.of(new UnsupportedOperationCharSequence(), UnsupportedOperationException.class));
    }

    private static Stream<Arguments> longStringProvider() {
        String fiveThousandSpaces = " ".repeat(5000);
        String fiveThousandSpacesPlusX = fiveThousandSpaces + "x";
        return Stream.of(Arguments.of(fiveThousandSpaces, true), Arguments.of(fiveThousandSpacesPlusX, false));
    }

    @ParameterizedTest
    @MethodSource("stringsWithAllWhitespace")
    void testIsEmpty_whenStringHasOnlyWhitespace_returnsTrue(String s) {
        assertTrue(StringUtilities.isEmpty(s), () -> "Expected isEmpty to return true for whitespace-only input: [" + s + "]");
    }

    @ParameterizedTest
    @MethodSource("stringsWithContentOtherThanWhitespace")
    void testIsEmpty_whenStringHasContent_returnsFalse(String s) {
        assertFalse(StringUtilities.isEmpty(s), () -> "Expected isEmpty to return false for input containing non‑whitespace: [" + s + "]");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testIsEmpty_whenNullOrEmpty_returnsTrue(String s) {
        assertTrue(StringUtilities.isEmpty(s), () -> "Expected isEmpty to return true for null or empty input: [" + s + "]");
    }

    @Test
    void testIsEmpty_withStringBuilder() {
        // only whitespace
        CharSequence cs = new StringBuilder("   ");
        assertTrue(StringUtilities.isEmpty(cs));
        cs = new StringBuilder("abc");
        assertFalse(StringUtilities.isEmpty(cs));
    }

    @ParameterizedTest
    @MethodSource("unicodeWhitespaceProvider")
    void testIsEmpty_unicodeWhitespace(String input, boolean expected) {
        assertEquals(expected, StringUtilities.isEmpty(input), () -> "Unicode whitespace test failed for input: [" + input + "]");
    }

    @ParameterizedTest
    @MethodSource("zeroWidthProvider")
    void testIsEmpty_zeroWidthCharacters(String input, boolean expected) {
        assertEquals(expected, StringUtilities.isEmpty(input), () -> "Zero‑width character test failed for input: [" + input + "]");
    }

    @ParameterizedTest
    @MethodSource("customCharSequenceProvider")
    void testIsEmpty_customCharSequence(CharSequence cs, boolean expected) {
        assertEquals(expected, StringUtilities.isEmpty(cs), () -> "Custom CharSequence test failed for: " + cs);
    }

    @ParameterizedTest
    @MethodSource("charBufferProvider")
    void testIsEmpty_charBuffer(CharBuffer buffer, boolean expected) {
        assertEquals(expected, StringUtilities.isEmpty(buffer), () -> "CharBuffer test failed for: [" + buffer + "]");
    }

    @ParameterizedTest
    @MethodSource("exceptionProvider")
    void testIsEmpty_exceptionPropagation(CharSequence cs, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> StringUtilities.isEmpty(cs), () -> "Expected exception " + expectedException.getSimpleName() + " was not thrown");
    }

    @ParameterizedTest
    @MethodSource("longStringProvider")
    void testIsEmpty_longStrings(CharSequence cs, boolean expected) {
        assertEquals(expected, StringUtilities.isEmpty(cs), () -> "Long string test failed for length: " + cs.length());
    }
}
