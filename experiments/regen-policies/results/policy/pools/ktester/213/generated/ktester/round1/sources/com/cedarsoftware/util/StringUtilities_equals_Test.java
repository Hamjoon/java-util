package com.cedarsoftware.util;

import java.lang.reflect.Method;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StringUtilities_equals_Test {

    /**
     * A {@link CharSequence} that throws {@link IllegalStateException}
     * when {@link #length()} is called.
     */
    private static final class LengthExceptionCharSequence implements CharSequence {

        private final String data;

        LengthExceptionCharSequence(String data) {
            this.data = data;
        }

        @Override
        public int length() {
            throw new IllegalStateException("length() exception");
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
     * A {@link CharSequence} that throws {@link IndexOutOfBoundsException}
     * when {@link #charAt(int)} is called.
     */
    private static final class CharAtExceptionCharSequence implements CharSequence {

        private final String data;

        CharAtExceptionCharSequence(String data) {
            this.data = data;
        }

        @Override
        public int length() {
            return data.length();
        }

        @Override
        public char charAt(int index) {
            throw new IndexOutOfBoundsException("charAt() exception");
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
     * Returns {@code true} if the supplied {@link Method} has a {@code void}
     * return type.
     *
     * @param method the method to inspect; may be {@code null}
     * @return {@code true} if the method returns {@code void}, otherwise {@code false}
     */
    public static boolean returnsVoid(Method method) {
        return method != null && void.class.equals(method.getReturnType());
    }

    /**
     * Finds a public method with the given name and parameter types on the
     * supplied class.  If no such method exists {@code null} is returned.
     *
     * @param clazz      the class to search
     * @param name       the method name
     * @param paramTypes the parameter types (may be empty)
     * @return the {@link Method} if found, otherwise {@code null}
     */
    public static Method findMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        if (clazz == null || name == null) {
            return null;
        }
        try {
            return clazz.getMethod(name, paramTypes);
        } catch (NoSuchMethodException ex) {
            // No public method with the given signature.
            return null;
        }
    }

    /**
     * Convenience overload used by JUnit Jupiter: determines whether a
     * method with the given name and parameter types is present on the
     * supplied class.
     *
     * @param clazz      the class to search
     * @param name       the method name
     * @param paramTypes the parameter types (may be empty)
     * @return {@code true} if such a method exists, otherwise {@code false}
     */
//     public static boolean isMethodPresent(Class<?> clazz, String name, Class<?>... paramTypes) {
//         return method != null;
//     }

    /**
     * Determines whether the supplied {@link Method} object is non‑null.
     *
     * @param method the method to test
     * @return {@code true} if the method is not {@code null}
     */
    public static boolean isMethodPresent(Method method) {
        return method != null;
    }

    private static Stream<Arguments> provideEqualsScenarios() {
        return Stream.of(// both null
        Arguments.of(null, null, true), // one null
        Arguments.of(null, "x", false), Arguments.of("x", null, false), // same reference (String literals)
        Arguments.of("same", "same", true), // equal strings
        Arguments.of("text", "text", true), // different case
        Arguments.of("text", "tExt", false), // empty strings
        Arguments.of("", "", true), // CharSequence implementations
        Arguments.of(new StringBuilder("builder"), new StringBuffer("builder"), true), Arguments.of(new StringBuilder("builder"), new StringBuffer("Builder"), false), // different lengths
        Arguments.of("short", "longer", false));
    }

    @Test
    void testEquals_sameReference_returnsTrue() {
        CharSequence cs = "identical";
        Assertions.assertTrue(StringUtilities.equals(cs, cs));
    }

    @Test
    void testEquals_bothNull_returnsTrue() {
        Assertions.assertTrue(StringUtilities.equals(null, null));
    }

    @Test
    void testEquals_bothEmptyStrings_returnsTrue() {
        Assertions.assertTrue(StringUtilities.equals("", ""));
    }

    @Test
    void testEquals_bothStringInstancesEqual_returnsTrue() {
        Assertions.assertTrue(StringUtilities.equals("hello", "hello"));
    }

    @Test
    void testEquals_nonStringCharSequencesEqual_returnsTrue() {
        CharSequence cs1 = new StringBuilder("builder");
        CharSequence cs2 = new StringBuffer("builder");
        Assertions.assertTrue(StringUtilities.equals(cs1, cs2));
    }

    @Test
    void testEquals_oneNull_returnsFalse() {
        Assertions.assertFalse(StringUtilities.equals(null, "notNull"));
        Assertions.assertFalse(StringUtilities.equals("notNull", null));
    }

    @Test
    void testEquals_differentLengths_returnsFalse() {
        Assertions.assertFalse(StringUtilities.equals("short", "longer"));
    }

    @Test
    void testEquals_stringsNotEqual_returnsFalse() {
        Assertions.assertFalse(StringUtilities.equals("test", "diff"));
    }

    @Test
    void testEquals_nonStringCharSequencesNotEqual_returnsFalse() {
        CharSequence cs1 = new StringBuilder("abc");
        // different case
        CharSequence cs2 = new StringBuffer("abC");
        Assertions.assertFalse(StringUtilities.equals(cs1, cs2));
    }

    @Test
    void testEquals_sameLengthDifferentChars_returnsFalse() {
        Assertions.assertFalse(StringUtilities.equals("abcd", "abcf"));
    }

    @ParameterizedTest
    @MethodSource("provideEqualsScenarios")
    void testEquals_parameterized(CharSequence cs1, CharSequence cs2, boolean expected) {
        Assertions.assertEquals(expected, StringUtilities.equals(cs1, cs2));
    }

    @Test
    void testEqualsStringOverload_delegatesCorrectly() {
        String s1 = "delegate";
        String s2 = "delegate";
        // Expected result via the CharSequence overload
        boolean expected = StringUtilities.equals((CharSequence) s1, (CharSequence) s2);
        // Actual result via the String overload
        boolean actual = StringUtilities.equals(s1, s2);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void testEquals_stringVsNonStringCharSequenceEqual_returnsTrue() {
        Assertions.assertTrue(StringUtilities.equals("hello", new StringBuilder("hello")));
    }

    @Test
    void testEquals_stringVsNonStringCharSequenceNotEqual_returnsFalse() {
        Assertions.assertFalse(StringUtilities.equals("hello", new StringBuilder("hellx")));
    }

    @Test
    void testEquals_unicodeSupplementaryCharacters_true() {
        // Emoji 😀 is a surrogate pair; comparison is done at the char level.
        Assertions.assertTrue(StringUtilities.equals("😀", new StringBuilder("😀")));
    }

    @Test
    void testEquals_unicodeSupplementaryCharacters_false() {
        Assertions.assertFalse(StringUtilities.equals("😀", new StringBuilder("😁")));
    }

    @Test
    void testEquals_mismatchCharacterEarly_returnsFalse() {
        Assertions.assertFalse(StringUtilities.equals("abcd", "abXd"));
    }

    @Test
    void testEquals_exceptionFromCs1Length_propagates() {
        CharSequence cs1 = new LengthExceptionCharSequence("test");
        CharSequence cs2 = "test";
        Assertions.assertThrows(IllegalStateException.class, () -> StringUtilities.equals(cs1, cs2));
    }

    @Test
    void testEquals_exceptionFromCs2Length_propagates() {
        CharSequence cs1 = "test";
        CharSequence cs2 = new LengthExceptionCharSequence("test");
        Assertions.assertThrows(IllegalStateException.class, () -> StringUtilities.equals(cs1, cs2));
    }

    @Test
    void testEquals_exceptionFromCs1CharAt_propagates() {
        CharSequence cs1 = new CharAtExceptionCharSequence("abc");
        CharSequence cs2 = "abc";
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> StringUtilities.equals(cs1, cs2));
    }

    @Test
    void testEquals_exceptionFromCs2CharAt_propagates() {
        CharSequence cs1 = "abc";
        CharSequence cs2 = new CharAtExceptionCharSequence("abc");
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> StringUtilities.equals(cs1, cs2));
    }
}