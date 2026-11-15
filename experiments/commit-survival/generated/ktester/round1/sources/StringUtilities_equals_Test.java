package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class StringUtilities_equals_Test {

    /**
     * Simple wrapper around a {@link String} that implements {@link CharSequence}.
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
     * CharSequence whose {@code length()} method always throws {@link IllegalStateException}.
     */
    private static final class BadLengthCharSequence implements CharSequence {

        @Override
        public int length() {
            throw new IllegalStateException("Bad length");
        }

        @Override
        public char charAt(int index) {
            // Not expected to be called
            return 0;
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            // Not expected to be called
            return null;
        }
    }

    /**
     * CharSequence whose {@code charAt(int)} method always throws {@link IndexOutOfBoundsException}.
     */
    private static final class BadCharAtCharSequence implements CharSequence {

        @Override
        public int length() {
            // any positive length to pass the length check
            return 3;
        }

        @Override
        public char charAt(int index) {
            throw new IndexOutOfBoundsException("Bad charAt");
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            // Not expected to be called
            return null;
        }
    }

    /**
     * Mutable CharSequence that throws on most operations but is safe when the reference
     * equality shortcut is taken (i.e., {@code cs1 == cs2}).
     */
    private static final class SelfReturningCharSequence implements CharSequence {

        @Override
        public int length() {
            throw new IllegalStateException("Should not be called");
        }

        @Override
        public char charAt(int index) {
            throw new IllegalStateException("Should not be called");
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            throw new IllegalStateException("Should not be called");
        }

        @Override
        public String toString() {
            return "self";
        }
    }

    static Stream<Arguments> booleanEqualityProvider() {
        // Helper instances for cases that need the same reference
        CharSequence sameRef = new StringBuilder("sameRef");
        CharSequence emptyBuilder = new StringBuilder();
        CharSequence emptyBuffer = new java.lang.StringBuffer();
        // Simple custom CharSequence implementation
        CharSequence simple1 = new SimpleCharSequence("abc");
        CharSequence simple2 = new SimpleCharSequence("abc");
        CharSequence simpleWithString = new SimpleCharSequence("custom");
        return Stream.of(// reference equality (already covered, but kept for completeness)
        arguments(sameRef, sameRef, true, "Both parameters reference the exact same CharSequence instance; should return true via cs1 == cs2"), // null / non‑null
        arguments(null, "nonNull", false, "First argument null, second non‑null → false"), arguments(null, null, true, "Both arguments null → true"), arguments("abc", "abcd", false, "Different lengths → false"), arguments("hello", new String("hello"), true, "Two equal Strings (different objects) → true"), arguments("hello", "world", false, "Two different Strings → false"), arguments(new StringBuilder("abc"), new java.lang.StringBuffer("abc"), true, "StringBuilder vs StringBuffer with same content → true"), arguments(new StringBuilder("abc"), new java.lang.StringBuffer("abx"), false, "StringBuilder vs StringBuffer differing at last char → false"), arguments("test", new StringBuilder("test"), true, "String vs StringBuilder same content → true"), arguments("", "", true, "Two empty strings → true"), arguments(emptyBuilder, emptyBuffer, true, "Empty StringBuilder vs empty StringBuffer → true"), arguments("", new StringBuilder("a"), false, "Empty vs non‑empty → false"), arguments(null, "", false, "Null vs empty string → false"), arguments("same", new java.lang.StringBuffer("same"), true, "String vs StringBuffer same content → true"), arguments("same", new java.lang.StringBuffer("diff"), false, "String vs StringBuffer different content → false"), arguments("Hello", "hello", false, "Case‑sensitive mismatch → false"), arguments("😀", new StringBuilder("😀"), true, "Unicode emoji vs StringBuilder same content → true"), arguments("😀", "😃", false, "Different Unicode characters → false"), arguments(simple1, simple2, true, "Two custom SimpleCharSequence instances with identical content → true"), arguments("custom", simpleWithString, true, "String vs SimpleCharSequence with same content → true"), // Self‑returning reference equality – same object passed twice
        arguments(new SelfReturningCharSequence(), new SelfReturningCharSequence(), true, "Two references to the same SelfReturningCharSequence (reference equality) → true"));
    }

    static Stream<Arguments> exceptionEqualityProvider() {
        // Instances that throw on length()
        CharSequence badLength = new BadLengthCharSequence();
        // Instances that throw on charAt()
        CharSequence badCharAt = new BadCharAtCharSequence();
        return Stream.of(arguments(badLength, "normal", IllegalStateException.class, "BadLengthCharSequence.length() throws IllegalStateException → propagate"), arguments("normal", badLength, IllegalStateException.class, "BadLengthCharSequence.length() throws IllegalStateException on second argument → propagate"), arguments(badCharAt, "abc", IndexOutOfBoundsException.class, "BadCharAtCharSequence.charAt() throws IndexOutOfBoundsException → propagate"), arguments("abc", badCharAt, IndexOutOfBoundsException.class, "BadCharAtCharSequence.charAt() throws IndexOutOfBoundsException on second argument → propagate"));
    }

    @Test
    void givenBothReferencesAreSame_returnsTrue() {
        CharSequence cs = "same";
        assertTrue(StringUtilities.equals(cs, cs), "When both references point to the same object the method must return true");
    }

    @Test
    void givenBothNullReferences_returnsTrue() {
        CharSequence cs1 = null;
        CharSequence cs2 = null;
        assertTrue(StringUtilities.equals(cs1, cs2), "Two null references are considered equal");
    }

    @Test
    void givenOneNullAndOneNonNull_returnsFalse() {
        CharSequence cs1 = null;
        CharSequence cs2 = "not null";
        assertFalse(StringUtilities.equals(cs1, cs2), "Null compared with non‑null must be false");
        assertFalse(StringUtilities.equals(cs2, cs1), "Non‑null compared with null must be false");
    }

    @Test
    void givenDifferentLengths_returnsFalse() {
        CharSequence cs1 = "short";
        CharSequence cs2 = "much longer";
        assertFalse(StringUtilities.equals(cs1, cs2), "Different length CharSequences cannot be equal");
    }

    @Test
    void givenBothStrings_useStringEquals() {
        String s1 = "ExactMatch";
        // distinct object but same content
        String s2 = new String("ExactMatch");
        assertTrue(StringUtilities.equals(s1, s2), "Two equal Strings must be reported as equal");
        assertTrue(StringUtilities.equals(s2, s1), "Symmetry check for String arguments");
    }

    @Test
    void givenStringAndStringBuilderSameContent_returnsTrue() {
        CharSequence cs1 = "builder";
        CharSequence cs2 = new StringBuilder("builder");
        assertTrue(StringUtilities.equals(cs1, cs2), "String and StringBuilder with same content should be equal");
    }

    @Test
    void givenStringBuilderAndStringBuilderDifferentContent_returnsFalse() {
        CharSequence cs1 = new StringBuilder("first");
        CharSequence cs2 = new StringBuilder("second");
        assertFalse(StringUtilities.equals(cs1, cs2), "Different content in CharSequences must yield false");
    }

    @Test
    void givenCharMismatchAtMiddle_returnsFalse() {
        CharSequence cs1 = "abXde";
        CharSequence cs2 = "abYde";
        assertFalse(StringUtilities.equals(cs1, cs2), "A single character difference should cause inequality");
    }

    @Test
    void givenEmptySequences_returnsTrue() {
        CharSequence cs1 = "";
        CharSequence cs2 = new StringBuilder();
        assertTrue(StringUtilities.equals(cs1, cs2), "Two empty CharSequences must be equal");
    }

    /**
     * Boolean‑returning scenarios (true / false).
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("booleanEqualityProvider")
    void booleanEquality(CharSequence cs1, CharSequence cs2, boolean expected, String description) {
        assertEquals(expected, StringUtilities.equals(cs1, cs2), description);
    }

    /**
     * Exception‑propagation scenarios.
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("exceptionEqualityProvider")
    void exceptionEquality(CharSequence cs1, CharSequence cs2, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> StringUtilities.equals(cs1, cs2), description);
    }
}
