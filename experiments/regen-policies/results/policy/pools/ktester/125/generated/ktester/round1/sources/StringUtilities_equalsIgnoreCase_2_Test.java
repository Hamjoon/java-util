package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.*;
import java.util.stream.Stream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class StringUtilities_equalsIgnoreCase_2_Test {

    private static Stream<Arguments> equalPairs() {
        return Stream.of(// identical strings, same case
        Arguments.of("abc", "abc"), // same letters, different case
        Arguments.of("abc", "ABC"), Arguments.of("ABC", "abc"), // mixed case
        Arguments.of("AbC", "aBc"), // empty strings
        Arguments.of("", ""), // null handling – both null considered equal
        Arguments.of(null, null), // one null and one empty – defined as false, but we also test true for both null only
        // (the true case for null/empty is not expected, kept for completeness)
        // strings containing Unicode characters
        Arguments.of("café", "CAFÉ"), // German ß vs SS – case‑insensitive should match per Unicode rules
        Arguments.of("Straße", "STRASSE"));
    }

    private static Stream<Arguments> nonEqualPairs() {
        return Stream.of(Arguments.of("abc", "ab"), Arguments.of("abc", "abcd"), // trailing space
        Arguments.of("abc", "abC "), // leading space
        Arguments.of(" abc", "abc"), // one null, one non‑null
        Arguments.of("abc", null), Arguments.of(null, "abc"), // empty vs null
        Arguments.of("", null), // whitespace vs empty
        Arguments.of(" ", ""), // accent difference
        Arguments.of("café", "cafe"), // extra character
        Arguments.of("Straße", "STRASSEX"));
    }

    private static Stream<Arguments> trueCases() {
        return Stream.of(// null handling – both null are considered equal
        Arguments.of(null, null), // basic case‑insensitivity
        Arguments.of("Hello", "hello"), Arguments.of("WORLD", "world"), // empty strings
        Arguments.of("", ""), // whitespace strings (identical sequences)
        Arguments.of("   ", "   "), Arguments.of("\t\n\r", "\t\n\r"), // Unicode with accents
        Arguments.of("café", "CAFÉ"), // Turkish dotted I – Character.toLowerCase handles it correctly
        // i + combining dot above
        Arguments.of("İstanbul", "i\u0307stanbul"), // short alternating case pattern
        Arguments.of("aA", "Aa"), // single character case difference
        Arguments.of("a", "A"), // very long strings, same letter different case
        Arguments.of("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
    }

    private static Stream<Arguments> falseCases() {
        return Stream.of(// different lengths
        Arguments.of("World", "worlds"), // one null, one non‑null
        Arguments.of(null, "test"), Arguments.of("test", null), // empty vs non‑empty
        Arguments.of("", "nonempty"), Arguments.of("", " "), // whitespace vs empty
        Arguments.of(" ", ""), // trailing / leading spaces
        Arguments.of("abc", "abC "), Arguments.of(" abc", "abc"), // accent difference
        Arguments.of("café", "cafe"), // German ß does NOT map to SS in a simple case‑insensitive comparison
        Arguments.of("straße", "STRASSE"), // extra character
        Arguments.of("Straße", "STRASSEX"));
    }

    @ParameterizedTest
    @MethodSource("equalPairs")
    void equalsIgnoreCase_returnsTrue(String s1, String s2) {
        assertTrue(StringUtilities.equalsIgnoreCase(s1, s2), () -> "Expected true for [" + s1 + "] and [" + s2 + "]");
    }

    @ParameterizedTest
    @MethodSource("nonEqualPairs")
    void equalsIgnoreCase_returnsFalse(String s1, String s2) {
        assertFalse(StringUtilities.equalsIgnoreCase(s1, s2), () -> "Expected false for [" + s1 + "] and [" + s2 + "]");
    }

    @Test
    void equalsIgnoreCase_nullAndEmpty() {
        // Both null → true (delegates to CharSequence overload which treats null as null)
        assertTrue(StringUtilities.equalsIgnoreCase(null, null));
        // One null, one empty → false
        assertFalse(StringUtilities.equalsIgnoreCase(null, ""));
        assertFalse(StringUtilities.equalsIgnoreCase("", null));
        // Both empty → true
        assertTrue(StringUtilities.equalsIgnoreCase("", ""));
    }

    @Test
    void equalsIgnoreCase_doesNotTrimAutomatically() {
        // The method does not trim; spaces make strings unequal
        assertFalse(StringUtilities.equalsIgnoreCase("  foo", "foo"));
        assertFalse(StringUtilities.equalsIgnoreCase("foo  ", "foo"));
        assertFalse(StringUtilities.equalsIgnoreCase("  foo  ", "foo"));
    }

    @Test
    void equalsIgnoreCase_callsCharSequenceVersion() {
        CharSequence cs1 = new StringBuilder("TeSt");
        CharSequence cs2 = new StringBuffer("tEsT");
        // Directly invoke the CharSequence overload via reflection to avoid ambiguity
        boolean result = StringUtilities.equalsIgnoreCase(cs1, cs2);
        assertTrue(result, "CharSequence overload should treat different case as equal");
    }

    @Test
    void equalsIgnoreCase_nonStringArgument_throwsIllegalArgumentException() throws NoSuchMethodException {
        Method method = StringUtilities.class.getMethod("equalsIgnoreCase", String.class, String.class);
        // First argument is a Map (not a String)
        Object[] args1 = { Map.of("key", "value"), "test" };
        InvocationTargetException ite1 = assertThrows(InvocationTargetException.class, () -> method.invoke(null, args1), "Invoking with a non‑String first argument should wrap an exception");
        assertTrue(ite1.getCause() instanceof IllegalArgumentException, "Cause should be IllegalArgumentException for incompatible argument type");
        // Second argument is a List (not a String)
        Object[] args2 = { "test", java.util.List.of(1, 2, 3) };
        InvocationTargetException ite2 = assertThrows(InvocationTargetException.class, () -> method.invoke(null, args2), "Invoking with a non‑String second argument should wrap an exception");
        assertTrue(ite2.getCause() instanceof IllegalArgumentException, "Cause should be IllegalArgumentException for incompatible argument type");
    }
}
