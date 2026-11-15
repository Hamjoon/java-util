package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.ParameterizedTest;

// @ExtendWith(MockitoExtension.class)
class StringUtilities_equalsIgnoreCase_2_Test {

    private static Stream<Arguments> positiveCases() {
        return Stream.of(Arguments.of("abc", "abc"), Arguments.of("ABC", "abc"), Arguments.of("AbC", "aBc"), Arguments.of("", ""), Arguments.of(" ", " "), Arguments.of("MixedCASEString", "mixedcasestring"), Arguments.of("12345", "12345"), Arguments.of("Ünicode", "ünicode"));
    }

    private static Stream<Arguments> negativeCases() {
        return Stream.of(Arguments.of("abc", "abd"), Arguments.of("abc", "ab"), Arguments.of("abc", "abcd"), Arguments.of("abc", "abCdef"), Arguments.of("Hello", "World"), Arguments.of(" ", ""), Arguments.of("abc ", "abc"), Arguments.of("abc", " abc"));
    }

    private static Stream<Arguments> nullAndReferenceCases() {
        return Stream.of(Arguments.of(null, null, true), Arguments.of(null, "any", false), Arguments.of("any", null, false), Arguments.of("sample", "sample", true));
    }

    private static Stream<Arguments> caseVariationCases() {
        return Stream.of(Arguments.of("HelloWorld", "helloworld", true), Arguments.of("Java", "JaVa", true), Arguments.of("Test", "TesTt", false));
    }

    private static Stream<Arguments> unicodeCases() {
        return Stream.of(Arguments.of("café", "CAFÉ", true), Arguments.of("Straße", "STRASSE", false), Arguments.of("Σίσυφος", "σίσυφος", true));
    }

    private static Stream<Arguments> emptyWhitespaceCases() {
        return Stream.of(Arguments.of("", "", true), Arguments.of("", "nonempty", false), Arguments.of(" ", " ", true), Arguments.of(" a ", "A", false));
    }

    private static Stream<Arguments> emojiCases() {
        return Stream.of(Arguments.of("😀", "😀", true), Arguments.of("😀", "😁", false));
    }

    private static Stream<Arguments> longStringCases() {
        String base = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        return Stream.of(Arguments.of(base + "Z", base.toUpperCase() + "z", true), Arguments.of(base + "Z", base.toUpperCase() + "b", false));
    }

    /**
     * Basic positive cases – strings that differ only by case should be considered equal.
     */
    @ParameterizedTest
    @MethodSource("positiveCases")
    void testEqualsIgnoreCase_Positive(String s1, String s2) {
        assertTrue(StringUtilities.equalsIgnoreCase(s1, s2), "Expected \"" + s1 + "\" and \"" + s2 + "\" to be equal ignoring case");
    }

    /**
     * Negative cases – strings that are different (including length differences) must return false.
     */
    @ParameterizedTest
    @MethodSource("negativeCases")
    void testEqualsIgnoreCase_Negative(String s1, String s2) {
        assertFalse(StringUtilities.equalsIgnoreCase(s1, s2), "Expected \"" + s1 + "\" and \"" + s2 + "\" to NOT be equal ignoring case");
    }

    /**
     * Null handling – the method is null‑safe: two nulls are equal, a null with a non‑null is not.
     */
    @Test
    void testEqualsIgnoreCase_NullHandling() {
        assertTrue(StringUtilities.equalsIgnoreCase(null, null));
        assertFalse(StringUtilities.equalsIgnoreCase(null, "abc"));
        assertFalse(StringUtilities.equalsIgnoreCase("abc", null));
    }

    /**
     * Null‑safe behavior – two nulls are considered equal,
     * a null with a non‑null is not.
     */
    @ParameterizedTest(name = "[{index}] s1={0}, s2={1} → {2}")
    @MethodSource("nullAndReferenceCases")
    void testEqualsIgnoreCase_NullAndReferenceHandling(String s1, String s2, boolean expected) {
        assertEquals(expected, StringUtilities.equalsIgnoreCase(s1, s2), "Unexpected result for s1=" + s1 + ", s2=" + s2);
    }

    /**
     * Explicit test for reference equality (same object instance).
     */
    @Test
    void testEqualsIgnoreCase_ReferenceEquality() {
        String sample = "sampleString";
        assertTrue(StringUtilities.equalsIgnoreCase(sample, sample), "Method should return true when both arguments reference the same object");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" vs \"{1}\" → {2}")
    @MethodSource("caseVariationCases")
    void testEqualsIgnoreCase_CaseVariations(String s1, String s2, boolean expected) {
        assertEquals(expected, StringUtilities.equalsIgnoreCase(s1, s2), "Case variation test failed for s1=" + s1 + ", s2=" + s2);
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" vs \"{1}\" → {2}")
    @MethodSource("unicodeCases")
    void testEqualsIgnoreCase_Unicode(String s1, String s2, boolean expected) {
        assertEquals(expected, StringUtilities.equalsIgnoreCase(s1, s2), "Unicode case handling failed for s1=" + s1 + ", s2=" + s2);
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" vs \"{1}\" → {2}")
    @MethodSource("emptyWhitespaceCases")
    void testEqualsIgnoreCase_EmptyAndWhitespace(String s1, String s2, boolean expected) {
        assertEquals(expected, StringUtilities.equalsIgnoreCase(s1, s2), "Empty/whitespace test failed for s1=" + s1 + ", s2=" + s2);
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" vs \"{1}\" → {2}")
    @MethodSource("emojiCases")
    void testEqualsIgnoreCase_Emoji(String s1, String s2, boolean expected) {
        assertEquals(expected, StringUtilities.equalsIgnoreCase(s1, s2), "Emoji comparison failed for s1=" + s1 + ", s2=" + s2);
    }

    @ParameterizedTest(name = "[{index}] Long strings equality → {2}")
    @MethodSource("longStringCases")
    void testEqualsIgnoreCase_LongStrings(String s1, String s2, boolean expected) {
        assertEquals(expected, StringUtilities.equalsIgnoreCase(s1, s2), "Long string test failed");
    }
}