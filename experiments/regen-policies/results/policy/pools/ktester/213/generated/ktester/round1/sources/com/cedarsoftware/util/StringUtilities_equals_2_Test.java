package com.cedarsoftware.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class StringUtilities_equals_2_Test {

    /**
     * Provides test data for {@link #testEquals_StringOverload(String, String, boolean)}.
     */
    private static Stream<Arguments> equalPairs() {
        return Stream.of(// Null handling (group: equals(String, String) - null handling)
        // both null → true
        Arguments.of(null, null, true), // first null, second non‑null → false
        Arguments.of(null, "any", false), // first non‑null, second null → false
        Arguments.of("any", null, false), // Empty / whitespace (group: equals(String, String) - normal and edge cases)
        // both empty → true
        Arguments.of("", "", true), // both whitespace only → true
        Arguments.of("   ", "   ", true), // empty vs single space → false
        Arguments.of("", " ", false), // leading whitespace → false
        Arguments.of("  hello", "hello", false), // Identical strings (case‑sensitive)
        // identical non‑empty strings
        Arguments.of("test", "test", true), // identical Unicode emoji (surrogate pair)
        Arguments.of("😀", "😀", true), // different objects, same content
        Arguments.of(new String("same"), new String("same"), true), // Case‑sensitivity
        // differs only by case
        Arguments.of("test", "Test", false), // different emoji
        Arguments.of("😀", "😃", false), // Trailing / internal whitespace differences
        // trailing space
        Arguments.of("test", "test ", false), // leading space (covered by earlier case)
        Arguments.of("test", " test", false), // Special characters
        // null character inside second string
        Arguments.of("test", "test\u0000", false), // zero‑width space in first string
        Arguments.of("abc\u200bdef", "abcdef", false), // Length differences
        // shorter vs longer
        Arguments.of("a", "ab", false), // prefix case
        Arguments.of("abc", "abcd", false));
    }

    /**
     * Returns a readable representation of a string, handling {@code null}.
     */
    private static String repr(String s) {
        return s == null ? "null" : "\"" + s + "\"";
    }

//     @ParameterizedTest
//     @MethodSource("equalPairs")
//     void testEquals_StringOverload(String s1, String s2, boolean expected) {
//         // WHEN
//         boolean actual = StringUtilities.equals(s1, s2);
//         // THEN
//         assertEquals(String.format("StringUtilities.equals(%s, %s) expected %s but was %s", repr(s1), repr(s2), expected, actual), expected, actual);
//     }
}