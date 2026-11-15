package com.cedarsoftware.util;

import org.junit.jupiter.params.ParameterizedTest;
import java.util.stream.Stream;
import static com.cedarsoftware.util.StringUtilities.regionMatches;
import static org.assertj.core.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class StringUtilities_regionMatches_Test {

    private static Stream<Arguments> regionMatches_trueCases() {
        return Stream.of(// Simple exact match, case‑sensitive
        Arguments.of("HelloWorld", false, 0, "HelloWorld", 0, 10), // Case‑insensitive match
        Arguments.of("HelloWorld", true, 0, "helloworld", 0, 10), // Sub‑region match
        Arguments.of("abcdef", false, 2, "cde", 0, 3), // Case‑insensitive sub‑region match
        Arguments.of("aBcDeF", true, 1, "bcd", 0, 3), // Using a non‑String CharSequence (StringBuilder)
        Arguments.of(new StringBuilder("StringBuilderTest"), false, 0, new StringBuilder("StringBuilderTest"), 0, 19), // Mixed String and StringBuilder, case‑insensitive
        Arguments.of(new StringBuilder("MixedCase"), false, 0, "mixedcase", 0, 9), // Sub‑sequence from a StringBuilder compared with a StringBuilder
        Arguments.of(new StringBuilder("StringBuilder:HelloWorld").subSequence(13, 18), false, 0, new StringBuilder("World"), 0, 5), // Sub‑sequence from a StringBuffer compared with a String (ignore case)
        Arguments.of(new StringBuffer("StringBuffer:HelloWorld").subSequence(13, 18), true, 0, "world", 0, 5));
    }

    private static Stream<Arguments> regionMatches_falseCases() {
        return Stream.of(// Different characters, case‑sensitive
        Arguments.of("HelloWorld", false, 0, "helloworld", 0, 10), // Length too short in source
        Arguments.of("abc", false, 0, "abcd", 0, 3), // Length too short in substring
        Arguments.of("abcd", false, 0, "abc", 0, 4), // Start index out of bounds (negative)
        Arguments.of("abc", false, -1, "abc", 0, 3), // Length negative
        Arguments.of("abc", false, 0, "abc", 0, -1), // Mismatch inside region
        Arguments.of("abcdef", false, 1, "bXf", 0, 3), // Zero‑length region but start beyond source length – should be false
        Arguments.of(new StringBuilder("StringBuilder:HelloWorld"), false, 13, new StringBuilder("world"), 13, 0), // Zero‑length region, start beyond source – false
        Arguments.of(new StringBuilder("StringBuilder:HelloWorld"), true, 13, new StringBuilder("worXe"), 13, 0), // Zero‑length region start beyond source
        Arguments.of("abc", false, 5, "xyz", 2, 0));
    }

    private static Stream<Arguments> regionMatches_throwsIllegalArgumentException() {
        return Stream.of(Arguments.of("a", true, 0, null, 0, 0, "substring cannot be null"), Arguments.of(null, true, 0, null, 0, 0, "cs to be processed cannot be null"), Arguments.of(null, true, 0, "", 0, 0, "cs to be processed cannot be null"));
    }

    private static Stream<Arguments> unicodeCaseFoldingCases() {
        return Stream.of(// Greek final sigma (U+03C2) vs regular sigma (U+03C3); ignoreCase true → true
        Arguments.of("ς", true, 0, "σ", 0, 1, true), // Uppercase sigma (U+03A3) vs final sigma (U+03C2); ignoreCase true → true
        Arguments.of("Σ", true, 0, "ς", 0, 1, true));
    }

    private static Stream<Arguments> boundaryLengthExceedCases() {
        return Stream.of(// Length exceeds remaining chars of cs
        Arguments.of("abc", false, 1, "abc", 0, 3, false), // Length exceeds remaining chars of substring
        Arguments.of("abcde", false, 0, "ab", 0, 3, false), // Zero‑length region but start beyond cs length – false
        Arguments.of("abc", false, 5, "xyz", 2, 0, false));
    }

    @ParameterizedTest
    @MethodSource("regionMatches_trueCases")
    void testRegionMatches_returnsTrue(CharSequence cs, boolean ignoreCase, int thisStart, CharSequence substring, int subStart, int length) {
        boolean result = regionMatches(cs, ignoreCase, thisStart, substring, subStart, length);
        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @MethodSource("regionMatches_falseCases")
    void testRegionMatches_returnsFalse(CharSequence cs, boolean ignoreCase, int thisStart, CharSequence substring, int subStart, int length) {
        boolean result = regionMatches(cs, ignoreCase, thisStart, substring, subStart, length);
        assertThat(result).isFalse();
    }

    @ParameterizedTest
    @MethodSource("regionMatches_throwsIllegalArgumentException")
    void testRegionMatches_nullArguments_throwsIllegalArgumentException(CharSequence cs, boolean ignoreCase, int thisStart, CharSequence substring, int subStart, int length, String expectedMessage) {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> regionMatches(cs, ignoreCase, thisStart, substring, subStart, length)).withMessageContaining(expectedMessage);
    }

    @Test
    void testRegionMatches_behavesLikeStringImplementation() {
        String source = "OpenAI ChatGPT";
        String target = "openai chatgpt";
        // Case‑sensitive – should be false
        assertThat(regionMatches(source, false, 0, target, 0, source.length())).isFalse();
        // Case‑insensitive – should be true
        assertThat(regionMatches(source, true, 0, target, 0, source.length())).isTrue();
        // Verify a middle region ("AI C" vs "ai c")
        assertThat(regionMatches(source, true, 5, target, 5, 4)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("unicodeCaseFoldingCases")
    void testRegionMatches_unicodeCaseFolding(CharSequence cs, boolean ignoreCase, int thisStart, CharSequence substring, int subStart, int length, boolean expected) {
        boolean result = regionMatches(cs, ignoreCase, thisStart, substring, subStart, length);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void testRegionMatches_exactBoundaryMatching() {
        CharSequence cs = "12345";
        CharSequence sub = "xyzab345";
        // cs[2..4] = "345", sub[3..5] = "345"
        assertThat(regionMatches(cs, false, 2, sub, 3, 3)).isTrue();
    }

    @Test
    void testRegionMatches_zeroLengthRegionStartBeyondSource() {
        // length = 0 but thisStart is out of bounds – should return false
        assertThat(regionMatches("abc", false, 5, "xyz", 2, 0)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("boundaryLengthExceedCases")
    void testRegionMatches_boundaryLengthExceed(CharSequence cs, boolean ignoreCase, int thisStart, CharSequence substring, int subStart, int length, boolean expected) {
        boolean result = regionMatches(cs, ignoreCase, thisStart, substring, subStart, length);
        assertThat(result).isEqualTo(expected);
    }
}
