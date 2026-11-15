package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import static com.cedarsoftware.util.StringUtilities.equalsIgnoreCase;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.CharBuffer;

@ExtendWith(MockitoExtension.class)
class StringUtilities_equalsIgnoreCase_Test {

    private static Stream<Arguments> trueCases() {
        return Stream.of(// same reference (including null)
        Arguments.of(null, null), Arguments.of("same", "same"), // empty strings
        Arguments.of("", ""), // case‑insensitive matches
        Arguments.of("Hello", "hello"), Arguments.of("HeLLo WoRLd", "hEllO wOrLD"), // mixed CharSequence implementations
        Arguments.of(new StringBuilder("AbC"), "aBc"));
    }

    private static Stream<Arguments> falseCases() {
        return Stream.of(// one side null, the other not
        Arguments.of(null, "notnull"), Arguments.of("notnull", null), // different lengths
        Arguments.of("short", "longer"), // same length but different characters (case‑sensitive mismatch)
        Arguments.of("abc", "abD"), // whitespace differences
        Arguments.of("test ", "test"), // different CharSequence types with mismatch
        Arguments.of(new StringBuilder("abc"), "abC"));
    }

    private static Stream<Arguments> allCases() {
        return Stream.of(// -----------------------------------------------------------------
        // Reference Equality
        // -----------------------------------------------------------------
        // same reference (interned)
        Arguments.of("sameString", "sameString", true), // both null
        Arguments.of(null, null, true), Arguments.of(new StringBuilder("abc"), new StringBuilder("abc"), false), // -----------------------------------------------------------------
        // Null Handling
        // -----------------------------------------------------------------
        Arguments.of(null, "nonNull", false), Arguments.of("nonNull", null, false), // -----------------------------------------------------------------
        // Length Mismatch
        // -----------------------------------------------------------------
        Arguments.of("abc", "abcd", false), // -----------------------------------------------------------------
        // Case Insensitivity
        // -----------------------------------------------------------------
        Arguments.of("AbC", "aBc", true), Arguments.of("Java", "JaVa", true), // -----------------------------------------------------------------
        // Empty Strings
        // -----------------------------------------------------------------
        Arguments.of("", "", true), Arguments.of("", " ", false), // -----------------------------------------------------------------
        // Unicode Case Mapping
        // -----------------------------------------------------------------
        // Greek capital sigma vs small sigma
        Arguments.of("\u03A3", "\u03C3", true), // Greek capital sigma vs final sigma
        Arguments.of("\u03A3", "\u03C2", false), // -----------------------------------------------------------------
        // Different CharSequence Implementations
        // -----------------------------------------------------------------
        Arguments.of(new StringBuilder("Test"), new StringBuffer("test"), true), Arguments.of(CharBuffer.wrap("Case"), "case", true), // -----------------------------------------------------------------
        // Surrogate Pair Handling (emoji)
        // -----------------------------------------------------------------
        // 😀 vs 😀
        Arguments.of("\uD83D\uDE00", "\uD83D\uDE00", true), // 😀 vs 😁
        Arguments.of("\uD83D\uDE00", "\uD83D\uDE01", false), // -----------------------------------------------------------------
        // Whitespace Sensitivity
        // -----------------------------------------------------------------
        Arguments.of("a b", "A B", true), Arguments.of("a\tb", "A B", false), // -----------------------------------------------------------------
        // RegionMatches Edge Cases
        // -----------------------------------------------------------------
        // single char
        Arguments.of("a", "A", true), // Turkish capital I with dot vs i
        Arguments.of("\u0130", "i", false), // Kelvin sign vs k
        Arguments.of("\u212A", "k", true), // German sharp s vs ss
        Arguments.of("\u00DF", "ss", false), // Musical G clef
        Arguments.of("\uD834\uDD1E", "\uD834\uDD1E", true), // Different musical symbols
        Arguments.of("\uD834\uDD1E", "\uD834\uDD1F", false), // a + combining acute
        Arguments.of("a\u0301", "A\u0301", true), // different combining marks
        Arguments.of("a\u0301", "a\u0300", false), // En quad space
        Arguments.of("\u2000", "\u2000", true), // En quad vs Em quad
        Arguments.of("\u2000", "\u2001", false));
    }

    @ParameterizedTest
    @MethodSource("trueCases")
    void testEqualsIgnoreCase_returnsTrue(CharSequence cs1, CharSequence cs2) {
        assertTrue(equalsIgnoreCase(cs1, cs2), () -> "Expected true for cs1=[" + cs1 + "], cs2=[" + cs2 + "]");
    }

    @ParameterizedTest
    @MethodSource("falseCases")
    void testEqualsIgnoreCase_returnsFalse(CharSequence cs1, CharSequence cs2) {
        assertFalse(equalsIgnoreCase(cs1, cs2), () -> "Expected false for cs1=[" + cs1 + "], cs2=[" + cs2 + "]");
    }

    @Test
    void testEqualsIgnoreCase_sameObjectReference_returnsTrue() {
        CharSequence seq = "reference";
        assertTrue(equalsIgnoreCase(seq, seq));
    }

    @Test
    void testEqualsIgnoreCase_stringOverload_delegatesToCharSequenceVersion() {
        // The overload simply casts to CharSequence, so behaviour must be identical
        assertTrue(StringUtilities.equalsIgnoreCase("MiXeD", "mixed"));
        assertFalse(StringUtilities.equalsIgnoreCase("MiXeD", "mi xed"));
    }

    @Test
    void testEqualsIgnoreCase_bothEmptyStrings() {
        assertTrue(equalsIgnoreCase("", ""));
    }

    @Test
    void testEqualsIgnoreCase_bothNull() {
        assertTrue(equalsIgnoreCase(null, null));
    }

    @Test
    void testEqualsIgnoreCase_oneNullOneEmpty() {
        assertFalse(equalsIgnoreCase(null, ""));
        assertFalse(equalsIgnoreCase("", null));
    }

    /**
     * All cases from the supplied JSON document.  Each {@link Arguments}
     * consists of {@code cs1}, {@code cs2} and the expected boolean result.
     */
    @ParameterizedTest(name = "[{index}] {2}: equalsIgnoreCase({0}, {1})")
    @MethodSource("allCases")
    void testEqualsIgnoreCase_allCases(CharSequence cs1, CharSequence cs2, boolean expected) {
        assertEquals(expected, equalsIgnoreCase(cs1, cs2), () -> "Unexpected result for cs1=[" + cs1 + "], cs2=[" + cs2 + "]");
    }
}
