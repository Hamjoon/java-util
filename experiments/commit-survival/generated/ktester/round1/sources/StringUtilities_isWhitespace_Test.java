package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Method;
import java.nio.CharBuffer;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class StringUtilities_isWhitespace_Test {

    /**
     * Provides a stream of arguments covering the majority of the JSON‑defined
     * test cases that expect a boolean result.
     *
     * Each argument consists of:
     *   1. The {@link CharSequence} to test,
     *   2. The expected boolean result,
     *   3. A human‑readable description (used as the assertion message).
     */
    private static Stream<Arguments> whitespaceProvider() {
        return Stream.of(// Group: isWhitespace tests
        arguments("", true, "Empty string should be considered whitespace (length == 0 triggers early true)."), arguments(null, true, "Null CharSequence is treated as length 0, therefore returns true."), arguments("   \t\n\r", true, "String containing only standard whitespace characters should return true."), arguments("\u2003\u2009", true, "Unicode whitespace characters (EM SPACE, THIN SPACE) should be recognised as whitespace."), arguments("abc", false, "String with only non‑whitespace characters should return false."), arguments("  a  ", false, "String with a non‑whitespace character in the middle should cause false."), arguments("a   ", false, "String with a non‑whitespace character at the start should return false immediately."), arguments("   a", false, "String with a non‑whitespace character at the end should return false after scanning preceding whitespace."), arguments("\u00A0", false, "Non‑breaking space (U+00A0) is not considered whitespace by Character.isWhitespace."), arguments(new StringBuilder(" \t \n"), true, "StringBuilder containing only whitespace should return true."), arguments(new StringBuilder("x \t"), false, "StringBuilder with a non‑whitespace character should cause false."), // Group: isWhitespace additional tests
        arguments(" ", true, "Single space character should be recognised as whitespace."), arguments("\t", true, "Single horizontal tab character is whitespace."), arguments("\n", true, "Single newline character is whitespace."), arguments("\f", true, "Form feed (\\f) is considered whitespace."), arguments("\u000B", true, "Vertical tab (U+000B) is recognised as whitespace."), arguments("\u2000", true, "EN QUAD (U+2000) is a Unicode whitespace character."), arguments("\u3000", true, "IDEOGRAPHIC SPACE (U+3000) is recognised as whitespace."), arguments("\u200B", false, "Zero‑width space (U+200B) is *not* considered whitespace."), arguments(new StringBuilder(), true, "Empty StringBuilder has length 0, so the method returns true."), arguments(CharBuffer.wrap(" \t\r\n"), true, "CharBuffer containing only whitespace characters should return true."), arguments(CharBuffer.wrap("x"), false, "CharBuffer with a single non‑whitespace character should return false."), arguments("  a  ".subSequence(0, 5), false, "A subsequence view that includes a non‑whitespace character must be evaluated correctly."), arguments(" ".repeat(1000), true, "A very long string consisting solely of spaces (1000 characters) should still be identified as whitespace."));
    }

    @Test
    @DisplayName("isWhitespace returns true for null CharSequence")
    void testIsWhitespace_Null() {
        assertTrue(StringUtilities.isWhitespace(null), "Null should be treated as empty and therefore whitespace");
    }

    @Test
    @DisplayName("isWhitespace returns true for empty CharSequence")
    void testIsWhitespace_Empty() {
        assertTrue(StringUtilities.isWhitespace(""), "Empty string should be considered whitespace");
    }

    @Test
    @DisplayName("isWhitespace returns true for only whitespace characters")
    void testIsWhitespace_OnlyWhitespace() {
        // space, tab, linefeed, carriage return, form feed, em space
        String spaces = " \t\n\r\f\u2003";
        assertTrue(StringUtilities.isWhitespace(spaces), "String consisting solely of whitespace characters should return true");
    }

    @Test
    @DisplayName("isWhitespace returns false for a non‑whitespace character")
    void testIsWhitespace_SingleNonWhitespace() {
        assertFalse(StringUtilities.isWhitespace("a"), "A single non‑whitespace character should return false");
    }

    @Test
    @DisplayName("isWhitespace returns false for mixed whitespace and non‑whitespace")
    void testIsWhitespace_Mixed() {
        assertFalse(StringUtilities.isWhitespace(" \tX\n"), "Mixed whitespace and non‑whitespace should return false");
    }

    @Test
    @DisplayName("isWhitespace works with CharSequence implementation other than String")
    void testIsWhitespace_CustomCharSequence() {
        CharSequence cs = new CharSequence() {

            private final char[] data = { ' ', '\t', '\n' };

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
                return new String(data, start, end - start);
            }

            @Override
            public String toString() {
                return new String(data);
            }
        };
        assertTrue(StringUtilities.isWhitespace(cs), "Custom CharSequence containing only whitespace should return true");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("whitespaceProvider")
    @DisplayName("Parameterized isWhitespace behaviour tests")
    void testIsWhitespace_Parameterized(CharSequence cs, boolean expected, String description) {
        assertEquals(expected, StringUtilities.isWhitespace(cs), description);
    }

    /**
     * Invokes {@code StringUtilities.isWhitespace} via reflection with an
     * argument that is not a {@link CharSequence}. The reflective call
     * should fail with {@link IllegalArgumentException} because the
     * parameter type does not match.
     */
    @Test
    @DisplayName("isWhitespace throws IllegalArgumentException when called via reflection with a non‑CharSequence")
    void testIsWhitespace_NonCharSequenceReflection() throws Exception {
        Method method = StringUtilities.class.getMethod("isWhitespace", CharSequence.class);
        // not a CharSequence
        Object nonCharSeq = new Object();
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> method.invoke(null, nonCharSeq), "Calling isWhitespace with a non‑CharSequence via reflection should throw IllegalArgumentException");
        // The cause of the reflection exception is an IllegalArgumentException
        assertTrue(thrown.getMessage().contains("argument type mismatch"));
    }

    /**
     * Supplies a {@link CharSequence} implementation whose {@code charAt}
     * method deliberately throws {@link IndexOutOfBoundsException}. The
     * exception must propagate out of {@code isWhitespace}.
     */
    @Test
    @DisplayName("isWhitespace propagates IndexOutOfBoundsException from a faulty CharSequence")
    void testIsWhitespace_FaultyCharSequence() {
        CharSequence faulty = new CharSequence() {

            @Override
            public int length() {
                return 5;
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
        };
        assertThrows(IndexOutOfBoundsException.class, () -> StringUtilities.isWhitespace(faulty), "The IndexOutOfBoundsException from charAt should be propagated");
    }

    /**
     * Creates a {@link CharBuffer} subsequence with an end index that exceeds
     * the underlying buffer length. The construction itself throws
     * {@link IndexOutOfBoundsException}, which we verify.
     */
    @Test
    @DisplayName("isWhitespace receives a CharBuffer subsequence with out‑of‑bounds indices")
    void testIsWhitespace_CharBufferSubsequenceOutOfBounds() {
        CharBuffer buffer = CharBuffer.wrap(new char[] { 'a', 'b', 'c' });
        // The subsequence call is expected to throw immediately
        assertThrows(IndexOutOfBoundsException.class, () -> buffer.subSequence(0, 5), "Creating a subsequence beyond the buffer length should throw IndexOutOfBoundsException");
    }
}
