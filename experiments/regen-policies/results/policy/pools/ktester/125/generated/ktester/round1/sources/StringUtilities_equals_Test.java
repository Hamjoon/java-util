package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class StringUtilities_equals_Test {

    /**
     * CharSequence whose {@code length()} throws an {@link IllegalStateException}.
     */
    private static final class ThrowingLengthCharSequence implements CharSequence {

        @Override
        public int length() {
            throw new IllegalStateException("length() failure");
        }

        @Override
        public char charAt(int index) {
            return 0;
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return "";
        }

        @Override
        public String toString() {
            return "";
        }
    }

    /**
     * CharSequence whose {@code charAt(int)} throws an {@link IndexOutOfBoundsException}
     * even for valid indices.
     */
    private static final class ThrowingCharAtCharSequence implements CharSequence {

        private final String data;

        ThrowingCharAtCharSequence(String data) {
            this.data = data;
        }

        @Override
        public int length() {
            return data.length();
        }

        @Override
        public char charAt(int index) {
            throw new IndexOutOfBoundsException("charAt() failure at index " + index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return data.substring(start, end);
        }

        @Override
        public String toString() {
            return data;
        }
    }

    private static Stream<Arguments> equalStringPairs() {
        return Stream.of(Arguments.of("", ""), Arguments.of("a", "a"), Arguments.of("abc", "abc"), // whitespace is significant
        Arguments.of("   ", "   "), // Unicode characters
        Arguments.of("éclair", "éclair"));
    }

    private static Stream<Arguments> unequalStringPairs() {
        return Stream.of(Arguments.of("a", "b"), // different length
        Arguments.of("abc", "ab"), Arguments.of("abc", "abd"), // case‑sensitive
        Arguments.of("ABC", "abc"), // whitespace vs empty
        Arguments.of(" ", ""), // Unicode vs ASCII
        Arguments.of("éclair", "eclair"));
    }

    /* --------------------------------------------------------------------- *
     *  Equality when both CharSequence references are the same object
     * --------------------------------------------------------------------- */
    @Test
    void equals_sameReference_returnsTrue() {
        CharSequence cs = "identical";
        assertTrue(StringUtilities.equals(cs, cs), "equals should return true when both arguments reference the same object");
    }

    /* --------------------------------------------------------------------- *
     *  Equality when both arguments are null
     * --------------------------------------------------------------------- */
    @Test
    void equals_bothNull_returnsTrue() {
        assertTrue(StringUtilities.equals(null, null), "equals should treat two nulls as equal");
    }

    /* --------------------------------------------------------------------- *
     *  Equality when one argument is null and the other is not
     * --------------------------------------------------------------------- */
    @Test
    void equals_oneNull_returnsFalse() {
        assertFalse(StringUtilities.equals(null, "not null"));
        assertFalse(StringUtilities.equals("not null", null));
    }

    /* --------------------------------------------------------------------- *
     *  Equality for equal strings (String instances)
     * --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("equalStringPairs")
    void equals_equalStrings_returnsTrue(String s1, String s2) {
        assertTrue(StringUtilities.equals(s1, s2));
    }

    /* --------------------------------------------------------------------- *
     *  Equality for unequal strings (different content or length)
     * --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("unequalStringPairs")
    void equals_unequalStrings_returnsFalse(String s1, String s2) {
        assertFalse(StringUtilities.equals(s1, s2));
    }

    /* --------------------------------------------------------------------- *
     *  Equality when arguments are non‑String CharSequence implementations
     * --------------------------------------------------------------------- */
    @Test
    void equals_stringBuilderAndString_returnsTrueWhenContentMatches() {
        CharSequence cs1 = new StringBuilder("builder");
        CharSequence cs2 = "builder";
        assertTrue(StringUtilities.equals(cs1, cs2));
    }

    @Test
    void equals_stringBufferAndStringBuilder_returnsFalseWhenContentDiffers() {
        CharSequence cs1 = new StringBuilder("builder");
        CharSequence cs2 = new java.lang.StringBuffer("buffer");
        assertFalse(StringUtilities.equals(cs1, cs2));
    }

    /* --------------------------------------------------------------------- *
     *  Delegation test: StringUtilities.equals(String, String) should delegate
     *  to the CharSequence version.
     * --------------------------------------------------------------------- */
    @Test
    void equals_stringOverload_delegatesToCharSequenceVersion() {
        // Direct call to the overload
        assertTrue(StringUtilities.equals("delegate", "delegate"));
        // Different strings
        assertFalse(StringUtilities.equals("delegate", "different"));
    }

    /* --------------------------------------------------------------------- *
     *  Additional edge cases: large strings and characters outside BMP
     * --------------------------------------------------------------------- */
    @Test
    void equals_largeStrings_returnsTrueWhenIdentical() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10_000; i++) {
            sb.append("xyz");
        }
        CharSequence cs1 = sb.toString();
        // separate but identical content
        CharSequence cs2 = sb.toString();
        assertTrue(StringUtilities.equals(cs1, cs2));
    }

    @Test
    void equals_surrogatePairs_areComparedCorrectly() {
        // Unicode character U+1F600 (GRINNING FACE) represented by a surrogate pair
        String smiley = new String(Character.toChars(0x1F600));
        assertTrue(StringUtilities.equals(smiley, smiley));
        assertFalse(StringUtilities.equals(smiley, "😀a"));
    }

    @Test
    void equals_twoStringBuilders_sameContent_returnsTrue() {
        CharSequence cs1 = new StringBuilder("abc");
        CharSequence cs2 = new StringBuilder("abc");
        assertTrue(StringUtilities.equals(cs1, cs2), "Two distinct StringBuilder instances with identical content must be equal");
    }

    /* --------------------------------------------------------------------- *
     *  Exception propagation tests – custom CharSequence implementations
     * --------------------------------------------------------------------- */
    @Test
    void equals_throwingLengthCharSequence_propagatesException() {
        CharSequence throwing = new ThrowingLengthCharSequence();
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> StringUtilities.equals(throwing, "any"));
        assertEquals("length() failure", ex.getMessage());
    }

    @Test
    void equals_throwingCharAtCharSequence_propagatesException() {
        CharSequence throwing = new ThrowingCharAtCharSequence("abc");
        IndexOutOfBoundsException ex = assertThrows(IndexOutOfBoundsException.class, () -> StringUtilities.equals(throwing, "abc"));
        assertEquals("charAt() failure at index 0", ex.getMessage());
    }
}
