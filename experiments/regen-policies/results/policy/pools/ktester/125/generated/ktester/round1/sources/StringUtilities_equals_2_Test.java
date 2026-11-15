package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class StringUtilities_equals_2_Test {

    private static Stream<Arguments> provideEqualityScenarios() {
        return Stream.of(// ----- branch‑coverage cases (from JSON) -----
        // both null – true
        Arguments.of(null, null, true), // one null – false
        Arguments.of(null, "abc", false), Arguments.of("xyz", null, false), // same reference – true (shortcut)
        Arguments.of("sameReference", "sameReference", true), // different objects, same content – true
        Arguments.of(new String("content"), new String("content"), true), // different literals, same length – false
        Arguments.of("hello", "world", false), // different lengths – false
        Arguments.of("short", "longer", false), // both empty – true
        Arguments.of("", "", true), // identical Unicode code point (é) – true
        Arguments.of("\u00E9", "\u00E9", true), // visually similar but different code points (é vs e + ́) – false
        Arguments.of("\u00E9", "e\u0301", false), // ----- additional cases (from JSON) -----
        // case‑sensitive mismatch – false
        Arguments.of("ABC", "abc", false), // empty vs non‑empty – false
        Arguments.of("", "nonempty", false), // trailing whitespace – false
        Arguments.of("same", "same ", false), // last character differs – false
        Arguments.of("abcz", "abcy", false), // identical emoji (surrogate pair) – true
        Arguments.of("\uD83D\uDE00", "\uD83D\uDE00", true), // different emoji – false
        Arguments.of("\uD83D\uDE00", "\uD83C\uDF0A", false), // embedded NUL character – true
        Arguments.of("a\0b", "a\0b", true), // embedded NUL vs missing NUL – false
        Arguments.of("a\0b", "ab", false), // strings consisting solely of spaces – true
        Arguments.of("   ", "   ", true), // strings with newline characters – true
        Arguments.of("line1\nline2", "line1\nline2", true), // ----- extra sanity checks not in JSON -----
        // whitespace matters (leading/trailing)
        Arguments.of("  spaced  ", "spaced", false), // same content, different objects (already covered but kept for clarity)
        Arguments.of(new String("same"), new String("same"), true), // case‑sensitivity check
        Arguments.of("Case", "case", false));
    }

    @ParameterizedTest(name = "[{index}] equals(\"{0}\", \"{1}\") => {2}")
    @MethodSource("provideEqualityScenarios")
    void testEquals_StringString(String s1, String s2, boolean expected) {
        boolean actual = StringUtilities.equals(s1, s2);
        assertEquals(expected, actual, () -> String.format("Expected equals(%s,%s) to be %s", s1, s2, expected));
    }

    @Test
    void testEquals_WithStringBuilderAndString() {
        // The overload we are testing accepts only String, but we can verify
        // that a StringBuilder converted to String behaves as expected.
        StringBuilder sb = new StringBuilder("builder");
        assertTrue(StringUtilities.equals(sb.toString(), "builder"), "StringBuilder.toString() should be equal to the same literal");
    }

    @Test
    void testEquals_SameReference() {
        // Reference equality shortcut should return true without further checks.
        String s = "reference";
        assertTrue(StringUtilities.equals(s, s), "Same reference should be equal");
    }
}
