package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

// No mocks are required but keep the annotation as in the template
@ExtendWith(MockitoExtension.class)
class StringUtilities_hashCodeIgnoreCase_Test {

    private static Stream<Arguments> stringsWithMixedCase() {
        return Stream.of(Arguments.of("HelloWorld"), Arguments.of("java"), Arguments.of("JUnit5"), Arguments.of("StringUtilities"), Arguments.of("TeStInG123"), // characters with locale‑independent case mapping
        Arguments.of("ÄÖÜäöüß"), Arguments.of("MiXeD CaSe   With   Spaces"));
    }

    private static Stream<Arguments> differentCasePairs() {
        return Stream.of(Arguments.of("abc", "ABC"), Arguments.of("Java", "jAvA"), Arguments.of("TestCase", "tEStcASE"), // note: toLowerCase('ß') == 'ß', but hashCodeIgnoreCase uses toLowerCase, so both map to same lower case
        Arguments.of("ß", "SS"), // Turkish dotted I – toLowerCase works with Unicode, not locale‑specific
        Arguments.of("İ", "i"));
    }

    private static Stream<Arguments> basicAsciiCases() {
        return Stream.of(// lower‑case only
        Arguments.of("abc", 96354), // mixed case – should produce the same hash as the lower‑case version
        Arguments.of("AbC", 96354));
    }

    private static Stream<Arguments> mixedCaseLongerStrings() {
        return Stream.of(Arguments.of("aBcD", 2_987_074));
    }

    private static Stream<Arguments> unicodeCaseMapping() {
        return Stream.of(// Capital A with grave accent → lower case à
        Arguments.of("ÀB", 7_042), // Café – contains é
        Arguments.of("Café", 3_045_921), // Greek capital sigma → lower case sigma (code point 963)
        Arguments.of("Σ", 963));
    }

    private static Stream<Arguments> surrogatePairCases() {
        return Stream.of(// Single emoji (😀) is a surrogate pair
        Arguments.of("😀", 1_772_899), // Combination of normal chars and an emoji
        Arguments.of("a😀b", 57_849_694));
    }

    private static Stream<Arguments> repeatedCharOverflowCases() {
        return Stream.of(Arguments.of("aaaaa", 92_567_585), // Ten 'a's – overflow expected
        Arguments.of("aaaaaaaaaa", -799_347_552));
    }

    private static Stream<Arguments> whitespaceCases() {
        return Stream.of(Arguments.of(" a ", 33_791));
    }

    @Test
    void hashCodeIgnoreCase_nullInput_returnsZero() {
        assertEquals(0, StringUtilities.hashCodeIgnoreCase(null), "hashCodeIgnoreCase should return 0 for a null argument");
    }

    @Test
    void hashCodeIgnoreCase_emptyString_returnsZero() {
        assertEquals(0, StringUtilities.hashCodeIgnoreCase(""), "hashCodeIgnoreCase should return 0 for an empty string (same as String.hashCode)");
    }

    @ParameterizedTest
    @MethodSource("stringsWithMixedCase")
    void hashCodeIgnoreCase_caseInsensitiveEquality(String original) {
        // Expected hash is the hash of the lower‑cased version using the JDK implementation
        int expected = original.toLowerCase().hashCode();
        int actual = StringUtilities.hashCodeIgnoreCase(original);
        assertEquals(expected, actual, () -> "hashCodeIgnoreCase should treat '" + original + "' case‑insensitively");
    }

    @ParameterizedTest
    @MethodSource("differentCasePairs")
    void hashCodeIgnoreCase_sameHashForDifferentCases(String s1, String s2) {
        int hash1 = StringUtilities.hashCodeIgnoreCase(s1);
        int hash2 = StringUtilities.hashCodeIgnoreCase(s2);
        assertEquals(hash1, hash2, () -> "Different case variants '" + s1 + "' and '" + s2 + "' must produce the same hash");
    }

    @ParameterizedTest(name = "[{index}] ''{0}'' → {1}")
    @MethodSource("basicAsciiCases")
    void hashCodeIgnoreCase_basicAscii(String input, int expected) {
        int actual = StringUtilities.hashCodeIgnoreCase(input);
        assertEquals(expected, actual, () -> "ASCII case‑insensitive hash mismatch for '" + input + "'");
    }

    @ParameterizedTest(name = "[{index}] ''{0}'' → {1}")
    @MethodSource("mixedCaseLongerStrings")
    void hashCodeIgnoreCase_mixedCaseLonger(String input, int expected) {
        int actual = StringUtilities.hashCodeIgnoreCase(input);
        assertEquals(expected, actual, () -> "Mixed‑case hash mismatch for '" + input + "'");
    }

    @ParameterizedTest(name = "[{index}] ''{0}'' → {1}")
    @MethodSource("unicodeCaseMapping")
    void hashCodeIgnoreCase_unicodeMapping(String input, int expected) {
        int actual = StringUtilities.hashCodeIgnoreCase(input);
        assertEquals(expected, actual, () -> "Unicode case‑mapping hash mismatch for '" + input + "'");
    }

    @ParameterizedTest(name = "[{index}] ''{0}'' → {1}")
    @MethodSource("surrogatePairCases")
    void hashCodeIgnoreCase_surrogatePairs(String input, int expected) {
        int actual = StringUtilities.hashCodeIgnoreCase(input);
        assertEquals(expected, actual, () -> "Surrogate‑pair hash mismatch for '" + input + "'");
    }

    @ParameterizedTest(name = "[{index}] ''{0}'' → {1}")
    @MethodSource("repeatedCharOverflowCases")
    void hashCodeIgnoreCase_repeatedChars(String input, int expected) {
        int actual = StringUtilities.hashCodeIgnoreCase(input);
        assertEquals(expected, actual, () -> "Repeated‑character hash mismatch for '" + input + "'");
    }

    @ParameterizedTest(name = "[{index}] ''{0}'' → {1}")
    @MethodSource("whitespaceCases")
    void hashCodeIgnoreCase_whitespace(String input, int expected) {
        int actual = StringUtilities.hashCodeIgnoreCase(input);
        assertEquals(expected, actual, () -> "Whitespace handling hash mismatch for '" + input + "'");
    }
}
