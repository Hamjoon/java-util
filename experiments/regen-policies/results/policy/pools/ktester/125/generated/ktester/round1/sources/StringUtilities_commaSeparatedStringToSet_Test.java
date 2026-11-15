package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class StringUtilities_commaSeparatedStringToSet_Test {

    private static Stream<Arguments> commaSeparatedStringToSetProvider() {
        return Stream.of(// Null input → empty set
        Arguments.of(null, Collections.emptySet()), // Empty string → empty set
        Arguments.of("", Collections.emptySet()), // Whitespace only → empty set
        Arguments.of("   ", Collections.emptySet()), // Single token without commas
        Arguments.of("apple", Set.of("apple")), // Multiple tokens with surrounding spaces
        Arguments.of(" apple , banana , cherry ", Set.of("apple", "banana", "cherry")), // Empty entries (consecutive commas or commas with only spaces) → filtered out
        Arguments.of("apple,, ,banana,,,", Set.of("apple", "banana")), // Duplicate exact strings are collapsed; case‑sensitivity preserved
        Arguments.of("apple,banana,apple,APPLE ", Set.of("apple", "banana", "APPLE")), // Tokens may contain internal spaces; only leading/trailing whitespace removed
        Arguments.of("  a b  ,  c d  ", Set.of("a b", "c d")), // Long list of ten tokens
        Arguments.of("one,two,three,four,five,six,seven,eight,nine,ten", Set.of("one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten")), // Leading and trailing commas are ignored
        Arguments.of(",apple,banana,", Set.of("apple", "banana")), // Only commas → empty set
        Arguments.of(",,,", Collections.emptySet()), // Duplicate after trimming, case‑sensitivity preserved
        Arguments.of("apple, apple ,APPLE", Set.of("apple", "APPLE")), // Mixed whitespace characters (tab, newline, carriage‑return)
        Arguments.of("\tapple,\nbanana , \r\n , cherry ", Set.of("apple", "banana", "cherry")), // Unicode emojis – duplicates removed
        Arguments.of("😀,😊,😀", Set.of("😀", "😊")), // Alphabet from a‑z – verifies handling of many elements
        Arguments.of("a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u,v,w,x,y,z", Set.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z")), // Mixed empty tokens and real values
        Arguments.of("apple,,banana,, , ,cherry", Set.of("apple", "banana", "cherry")), // Case‑sensitive distinct tokens
        Arguments.of("Apple,apple", Set.of("Apple", "apple")));
    }

    /* --------------------------------------------------------------------- *
     *  Null, empty and whitespace only inputs should return an empty set   *
     * --------------------------------------------------------------------- */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   ", "\t\n", " , , " })
    void testCommaSeparatedStringToSet_whenNullOrBlankOrOnlySeparators_returnsEmptySet(String input) {
        Set<String> result = StringUtilities.commaSeparatedStringToSet(input);
        assertTrue(result.isEmpty(), "Result should be empty for input: " + input);
    }

    /* --------------------------------------------------------------------- *
     *  Simple single‑value input                                            *
     * --------------------------------------------------------------------- */
    @Test
    void testCommaSeparatedStringToSet_singleValue() {
        Set<String> result = StringUtilities.commaSeparatedStringToSet("foo");
        assertEquals(Set.of("foo"), result);
    }

    /* --------------------------------------------------------------------- *
     *  Multiple values separated by commas                                 *
     * --------------------------------------------------------------------- */
    @Test
    void testCommaSeparatedStringToSet_multipleValues() {
        Set<String> result = StringUtilities.commaSeparatedStringToSet("a,b,c");
        assertEquals(Set.of("a", "b", "c"), result);
    }

    /* --------------------------------------------------------------------- *
     *  Values with surrounding whitespace and empty tokens                  *
     * --------------------------------------------------------------------- */
    @Test
    void testCommaSeparatedStringToSet_valuesWithWhitespaceAndEmptyTokens() {
        String input = "  a  , ,  b ,   ,c  ";
        Set<String> result = StringUtilities.commaSeparatedStringToSet(input);
        assertEquals(Set.of("a", "b", "c"), result);
    }

    /* --------------------------------------------------------------------- *
     *  Duplicate values should be collapsed into a set                      *
     * --------------------------------------------------------------------- */
    @Test
    void testCommaSeparatedStringToSet_duplicateValuesAreRemoved() {
        Set<String> result = StringUtilities.commaSeparatedStringToSet("x,x,y");
        assertEquals(Set.of("x", "y"), result);
    }

    /* --------------------------------------------------------------------- *
     *  Only commas and whitespace – should yield an empty set              *
     * --------------------------------------------------------------------- */
    @Test
    void testCommaSeparatedStringToSet_onlySeparatorsAndWhitespace() {
        Set<String> result = StringUtilities.commaSeparatedStringToSet(", , ,   ,");
        assertTrue(result.isEmpty());
    }

    /* --------------------------------------------------------------------- *
     *  Parameterized source covering a variety of edge cases                *
     * --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("commaSeparatedStringToSetProvider")
    void testCommaSeparatedStringToSet_variousScenarios(String input, Set<String> expected) {
        Set<String> actual = StringUtilities.commaSeparatedStringToSet(input);
        assertEquals(expected, actual, () -> "Failed for input: [" + Objects.toString(input) + "]");
    }
}
