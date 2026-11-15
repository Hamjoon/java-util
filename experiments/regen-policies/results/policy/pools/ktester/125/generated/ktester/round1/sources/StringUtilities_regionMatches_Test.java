package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.cedarsoftware.util.StringUtilities.regionMatches;

@ExtendWith(MockitoExtension.class)
class StringUtilities_regionMatches_Test {

    private static Stream<Arguments> regionMatchesPositiveCases() {
        // Simple exact match (String vs String)
        Arguments a1 = Arguments.of(false, 0, 0, 5, "Hello", "Hello");
        // Case‑insensitive match
        Arguments a2 = Arguments.of(true, 0, 0, 5, "HeLLo", "hello");
        // Sub‑region match inside a longer string
        Arguments a3 = Arguments.of(false, 2, 0, 3, "abCDEf", "CDE");
        // Using a non‑String CharSequence (StringBuilder)
        Arguments a4 = Arguments.of(true, 1, 0, 4, // cs
        new StringBuilder("xAbcDef"), // sub
        new StringBuilder("ABCD"));
        // Zero‑length region – always true when indices are valid
        Arguments a5 = Arguments.of(false, 3, 2, 0, "any", "thing");
        return Stream.of(a1, a2, a3, a4, a5);
    }

    private static Stream<Arguments> regionMatchesNegativeCases() {
        // Different content, case‑sensitive
        Arguments b1 = Arguments.of(false, 0, 0, 5, "Hello", "hELLo");
        // Different content, case‑insensitive but length mismatch
        Arguments b2 = Arguments.of(true, 0, 0, 4, "Test", "Tesx");
        // Region exceeds source length
        Arguments b3 = Arguments.of(false, 4, 0, 3, "abcd", "abc");
        // Region exceeds substring length
        Arguments b4 = Arguments.of(false, 0, 2, 4, "abcd", "ab");
        // Valid indices but characters differ
        Arguments b5 = Arguments.of(true, 1, 0, 2, "abC", "AB");
        return Stream.of(b1, b2, b3, b4, b5);
    }

    private static Stream<Arguments> regionMatchesNullCases() {
        // cs is null
        Arguments n1 = Arguments.of(null, true, 0, "abc", 0, 0, "cs to be processed cannot be null");
        // substring is null
        Arguments n2 = Arguments.of("abc", true, 0, null, 0, 0, "substring cannot be null");
        // both null – cs check wins first
        Arguments n3 = Arguments.of(null, true, 0, null, 0, 0, "cs to be processed cannot be null");
        return Stream.of(n1, n2, n3);
    }

    private static Stream<Arguments> nullArgumentCases() {
        return Stream.of(Arguments.of(null, "abc", "cs to be processed cannot be null"), Arguments.of("abc", null, "substring cannot be null"));
    }

    private static Stream<Arguments> stringDelegationCases() {
        return Stream.of(// exact match, case‑sensitive
        Arguments.of(false, 0, 0, 10, "HelloWorld", "HelloWorld", true), // mismatch, case‑sensitive
        Arguments.of(false, 0, 0, 10, "HelloWorld", "HellaWorld", false), // case‑insensitive match
        Arguments.of(true, 0, 0, 10, "HelloWorld", "helloworld", true));
    }

    private static Stream<Arguments> negativeParameterCases() {
        return Stream.of(Arguments.of(-1, 0, 3), Arguments.of(0, -2, 3), Arguments.of(0, 0, -5));
    }

    private static Stream<Arguments> insufficientRegionCases() {
        return Stream.of(// source shorter than requested length
        Arguments.of("abc", "abcdef", 0, 0, 4, false), // substring shorter than requested length
        Arguments.of("abcdef", "abc", 0, 0, 4, false));
    }

    private static Stream<Arguments> nonStringCaseSensitivityCases() {
        return Stream.of(// exact match, ignoreCase false
        Arguments.of(false, "AbCdEf", "AbCdEf", 6, true), // same letters different case, ignoreCase false
        Arguments.of(false, "AbCdEf", "aBcDeF", 6, false), // same letters different case, ignoreCase true
        Arguments.of(true, "AbCdEf", "aBcDeF", 6, true), // last character differs even with ignoreCase true
        Arguments.of(true, "AbCdEf", "aBcDeG", 6, false));
    }

    private static Stream<Arguments> mixedCharSequenceCases() {
        return Stream.of(// both StringBuilder
        Arguments.of(new StringBuilder("HelloWorld"), new StringBuilder("HelloWorld"), false, 10, true), // cs String, sub StringBuilder
        Arguments.of("HelloWorld", new StringBuilder("HelloWorld"), false, 10, true), // cs StringBuilder, sub String, ignoreCase true
        Arguments.of(new StringBuilder("HelloWorld"), "helloworld", true, 10, true));
    }

    private static Stream<Arguments> boundaryIndexCases() {
        return Stream.of(// zero‑length region at end of cs
        Arguments.of("HelloWorld", "HelloWorld", 10, 0, 0, true), // length > 0 but start at end → false
        Arguments.of("HelloWorld", "HelloWorld", 10, 0, 1, false), // zero‑length region at end of sub
        Arguments.of("HelloWorld", "HelloWorld", 0, 10, 0, true), // length > 0 but start at end of sub → false
        Arguments.of("HelloWorld", "HelloWorld", 0, 10, 1, false));
    }

    private static Stream<Arguments> emptyStringCases() {
        return Stream.of(// both empty, length zero
        Arguments.of("", "", 0, 0, 0, true), // both empty, length > 0
        Arguments.of("", "", 0, 0, 3, false), // empty cs, non‑empty sub, zero length
        Arguments.of("", "abc", 0, 0, 0, true), // non‑empty cs, empty sub, zero length
        Arguments.of("abc", "", 0, 0, 0, true), // non‑empty cs, empty sub, length > 0
        Arguments.of("abc", "", 0, 0, 1, false));
    }

    private static Stream<Arguments> unicodeCaseCases() {
        return Stream.of(// é vs É, ignoreCase true → true
        Arguments.of(true, "café", "CAFÉ", 4, true), // same but ignoreCase false → false
        Arguments.of(false, "café", "CAFÉ", 4, false));
    }

    private static Stream<Arguments> zeroLengthOutOfBoundsCases() {
        return Stream.of(// thisStart beyond cs length, length zero → false (srcLen negative)
        Arguments.of("abc", "xyz", 5, 0, 0, false), // start beyond sub length, length zero → false (otherLen negative)
        Arguments.of("abc", "xyz", 0, 5, 0, false));
    }

    private static Stream<Arguments> surrogatePairCases() {
        // 😀 = \uD83D\uDE00
        // surrogate pair + 'A'
        String smile = "\uD83D\uDE00A";
        // surrogate pair + 'B'
        String smileDiff = "\uD83D\uDE00B";
        // high surrogate alone
        String highSurrogateOnly = "\uD83D";
        // low surrogate alone
        String lowSurrogateOnly = "\uDE00";
        return Stream.of(// exact match of surrogate pair + 'A', length 2 (pair counts as two chars)
        Arguments.of(smile, smile, 0, 0, 2, true), // same pair but second char differs
        Arguments.of(smile, smileDiff, 0, 0, 2, false), // compare only the first char of the pair (high surrogate)
        Arguments.of("\uD83D\uDE00", "\uD83D\uDE00", 0, 0, 1, true), // lone low surrogate – direct char comparison
        Arguments.of(lowSurrogateOnly, lowSurrogateOnly, 0, 0, 1, true));
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] ignoreCase={0}, start={1}, subStart={2}, length={3}")
    @MethodSource("regionMatchesPositiveCases")
    void testRegionMatches_returnsTrue(boolean ignoreCase, int start, int subStart, int length, CharSequence cs, CharSequence sub) {
        assertThat(regionMatches(cs, ignoreCase, start, sub, subStart, length)).isTrue();
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] ignoreCase={0}, start={1}, subStart={2}, length={3}")
    @MethodSource("regionMatchesNegativeCases")
    void testRegionMatches_returnsFalse(boolean ignoreCase, int start, int subStart, int length, CharSequence cs, CharSequence sub) {
        assertThat(regionMatches(cs, ignoreCase, start, sub, subStart, length)).isFalse();
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("regionMatchesNullCases")
    void testRegionMatches_throwsIllegalArgumentException(CharSequence cs, boolean ignoreCase, int start, CharSequence sub, int subStart, int length, String expectedMessage) {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> regionMatches(cs, ignoreCase, start, sub, subStart, length)).withMessageContaining(expectedMessage);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testRegionMatches_negativeParameters_returnFalse() {
        // negative start index
        assertFalse(regionMatches("abc", false, -1, "abc", 0, 3));
        // negative subStart index
        assertFalse(regionMatches("abc", false, 0, "abc", -1, 3));
        // negative length
        assertFalse(regionMatches("abc", false, 0, "abc", 0, -1));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testRegionMatches_zeroLengthRegion() {
        assertTrue(regionMatches("any", false, 2, "thing", 4, 0));
        // also works when both CharSequences are empty
        assertTrue(regionMatches("", false, 0, "", 0, 0));
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] cs={0}, sub={1}")
    @MethodSource("nullArgumentCases")
    void testRegionMatches_nullArguments_throwsNPE(CharSequence cs, CharSequence sub, String expectedMessage) {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> regionMatches(cs, false, 0, sub, 0, 0)).withMessageContaining(expectedMessage);
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] ignoreCase={0}, length={5}")
    @MethodSource("stringDelegationCases")
    void testRegionMatches_stringDelegation(boolean ignoreCase, int thisStart, int start, int length, String cs, String sub, boolean expected) {
        assertThat(regionMatches(cs, ignoreCase, thisStart, sub, start, length)).isEqualTo(expected);
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] srcLen={0}, subLen={1}, length={2}")
    @MethodSource("insufficientRegionCases")
    void testRegionMatches_insufficientRegion(String cs, String sub, int thisStart, int start, int length, boolean expected) {
        assertThat(regionMatches(cs, false, thisStart, sub, start, length)).isEqualTo(expected);
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] ignoreCase={0}")
    @MethodSource("nonStringCaseSensitivityCases")
    void testRegionMatches_nonStringCaseSensitivity(boolean ignoreCase, CharSequence cs, CharSequence sub, int length, boolean expected) {
        assertThat(regionMatches(cs, ignoreCase, 0, sub, 0, length)).isEqualTo(expected);
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] csType={0}, subType={1}")
    @MethodSource("mixedCharSequenceCases")
    void testRegionMatches_mixedCharSequences(CharSequence cs, CharSequence sub, boolean ignoreCase, int length, boolean expected) {
        assertThat(regionMatches(cs, ignoreCase, 0, sub, 0, length)).isEqualTo(expected);
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] thisStart={0}, length={1}")
    @MethodSource("boundaryIndexCases")
    void testRegionMatches_boundaryIndices(String cs, String sub, int thisStart, int start, int length, boolean expected) {
        assertThat(regionMatches(cs, false, thisStart, sub, start, length)).isEqualTo(expected);
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] cs=\"{0}\", sub=\"{1}\", length={2}")
    @MethodSource("emptyStringCases")
    void testRegionMatches_emptyStrings(String cs, String sub, int thisStart, int start, int length, boolean expected) {
        assertThat(regionMatches(cs, false, thisStart, sub, start, length)).isEqualTo(expected);
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] ignoreCase={0}")
    @MethodSource("unicodeCaseCases")
    void testRegionMatches_unicodeCase(boolean ignoreCase, String cs, String sub, int length, boolean expected) {
        assertThat(regionMatches(cs, ignoreCase, 0, sub, 0, length)).isEqualTo(expected);
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] thisStart={0}, start={1}")
    @MethodSource("zeroLengthOutOfBoundsCases")
    void testRegionMatches_zeroLengthOutOfBounds(String cs, String sub, int thisStart, int start, int length, boolean expected) {
        assertThat(regionMatches(cs, false, thisStart, sub, start, length)).isEqualTo(expected);
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] length={4}")
    @MethodSource("surrogatePairCases")
    void testRegionMatches_surrogatePairs(String cs, String sub, int thisStart, int start, int length, boolean expected) {
        assertThat(regionMatches(cs, false, thisStart, sub, start, length)).isEqualTo(expected);
    }
}
