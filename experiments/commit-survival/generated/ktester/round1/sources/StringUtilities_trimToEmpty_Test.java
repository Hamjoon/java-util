package com.cedarsoftware.util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

// No mocks needed, but kept for consistency with template
@ExtendWith(MockitoExtension.class)
class StringUtilities_trimToEmpty_Test {

    private static Stream<org.junit.jupiter.params.provider.Arguments> basicTrimToEmptyProvider() {
        return Stream.of(arguments(null, StringUtilities.EMPTY, "When the input is null the method should return the constant EMPTY (empty string)."), arguments("", "", "An empty string has no whitespace to trim, so the result should be the same empty string."), arguments("   ", "", "A string consisting only of spaces should be trimmed to an empty string."), arguments("\t\n  abc  \r\n", "abc", "String with leading and trailing whitespace characters (tabs, newlines, spaces, carriage returns) should be trimmed to the core content."), arguments("abc", "abc", "A string without any surrounding whitespace should be returned unchanged."), arguments("  abc def  ", "abc def", "String with internal spaces should retain those spaces after trimming only the outer whitespace."));
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> additionalTrimToEmptyProvider() {
        return Stream.of(// Unicode whitespace (EN QUAD) – not trimmed
        arguments("\u2000\u2000Hello\u2000", "\u2000\u2000Hello\u2000", "Unicode whitespace characters (EN QUAD, U+2000) are not trimmed because String.trim() only removes characters <= U+0020. The original string should be returned unchanged."), // Non‑breaking spaces – not trimmed
        arguments("\u00A0\u00A0", "\u00A0\u00A0", "Non‑breaking spaces (U+00A0) are not considered whitespace by String.trim(). The method should return the original string unchanged."), // Mixed ASCII space and NBSP – only ASCII spaces trimmed
        arguments(" \u00A0 ", "\u00A0", "Leading and trailing ASCII spaces are trimmed, but the inner non‑breaking space remains, resulting in a string that contains only the NBSP character."), // Form‑feed (\\f) – trimmed
        arguments("\f\fHello\f", "Hello", "Form‑feed (\\f) is <= U+0020 and therefore trimmed from both ends."), // Vertical tab – trimmed
        arguments("\u000BHello\u000B", "Hello", "Vertical tab (U+000B) is trimmed because it falls within the range of characters removed by String.trim()."), // Emoji with trailing spaces – spaces trimmed, emoji preserved
        arguments("😀   ", "😀", "Trailing ASCII spaces are removed while the emoji (a surrogate pair) is preserved."), // Emoji with leading spaces – spaces trimmed, emoji preserved
        arguments("   😀", "😀", "Leading ASCII spaces are removed; the emoji remains unchanged."), // Emoji alone – no trimming, same reference expected
        arguments("😀", "😀", "String contains no leading or trailing whitespace; trimToEmpty should return the original string (same reference)."), // Long string with many spaces – trimmed to core content
        arguments("                    content                    ", "content", "A relatively long string (20 spaces on each side) is trimmed to its core content, testing performance on larger inputs."), // Various control whitespace characters – trimmed to empty
        arguments("\t\n\r\f\u000B", "", "String consisting solely of various ASCII control whitespace characters should be trimmed to an empty string."));
    }

    @Test
    void trimToEmpty_givenNull_returnsEmptyString() {
        // given
        String input = null;
        // when
        String result = StringUtilities.trimToEmpty(input);
        // then
        assertNotNull(result, "Result should never be null");
        assertEquals(StringUtilities.EMPTY, result, "Null input must be converted to EMPTY constant");
    }

    @Test
    void trimToEmpty_givenEmptyString_returnsEmptyString() {
        // given
        String input = "";
        // when
        String result = StringUtilities.trimToEmpty(input);
        // then
        assertEquals("", result, "Empty input should be returned unchanged");
    }

    @Test
    void trimToEmpty_givenStringWithOnlyWhitespace_returnsEmptyString() {
        // given
        String input = "   \t\n  ";
        // when
        String result = StringUtilities.trimToEmpty(input);
        // then
        assertEquals("", result, "String consisting solely of whitespace should be trimmed to empty");
    }

    @Test
    void trimToEmpty_givenStringWithLeadingAndTrailingWhitespace_trimsCorrectly() {
        // given
        String input = "  \tHello World  \n";
        // when
        String result = StringUtilities.trimToEmpty(input);
        // then
        assertEquals("Hello World", result, "Whitespace at both ends must be removed");
    }

    @Test
    void trimToEmpty_givenStringWithoutWhitespace_returnsSameInstance() {
        // given
        String input = "NoSpaces";
        // when
        String result = StringUtilities.trimToEmpty(input);
        // then
        // The implementation uses String.trim(), which returns the same instance if no trimming is needed.
        // This is an implementation detail, but we can assert equality of content.
        assertSame(input, result, "When no trimming is required, the original string should be returned");
    }

    @Test
    void trimToEmpty_isConsistentWithTrimLengthExample() {
        // given
        String input = "  abc  ";
        // when
        int lengthFromTrimToEmpty = StringUtilities.trimToEmpty(input).length();
        // uses trimToEmpty internally
        int lengthFromTrimLength = StringUtilities.trimLength(input);
        // then
        assertEquals(3, lengthFromTrimToEmpty, "trimToEmpty should produce a string of length 3");
        assertEquals(lengthFromTrimToEmpty, lengthFromTrimLength, "trimLength should delegate to trimToEmpty and yield the same length");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("basicTrimToEmptyProvider")
    @DisplayName("trimToEmpty – basic scenarios")
    void trimToEmpty_basicScenarios(String input, String expected, String description) {
        // when
        String result = StringUtilities.trimToEmpty(input);
        // then – content must match
        assertEquals(expected, result, description);
        // additionally verify reference equality when no trimming is required
        if (input != null && input.equals(expected) && input.equals(input.trim())) {
            assertSame(input, result, "When the input requires no trimming, the original reference should be returned");
        } else {
            // for null input we expect the constant EMPTY, which is a distinct object
            if (input == null) {
                assertSame(StringUtilities.EMPTY, result, "Null input must return the EMPTY constant instance");
            }
        }
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("additionalTrimToEmptyProvider")
    @DisplayName("trimToEmpty – additional edge‑case scenarios")
    void trimToEmpty_additionalScenarios(String input, String expected, String description) {
        // when
        String result = StringUtilities.trimToEmpty(input);
        // then – verify the trimmed content
        assertEquals(expected, result, description);
        // verify reference equality when no characters are trimmed
        if (input != null && input.equals(expected) && input.equals(input.trim())) {
            assertSame(input, result, "When no trimming occurs (including Unicode whitespace not recognised by String.trim()), the original reference should be returned");
        } else {
            // cases where trimming occurs must produce a new instance
            if (!input.equals(expected)) {
                assertNotSame(input, result, "When trimming occurs, a new String instance should be returned");
            }
        }
    }
}
