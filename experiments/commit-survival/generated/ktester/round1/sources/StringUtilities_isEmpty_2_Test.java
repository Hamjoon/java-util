package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringUtilities_isEmpty_2_Test {

    /**
     * Provides all test cases defined in the JSON specification.
     *
     * @return a stream of {@link Arguments} (input, expected, description)
     */
    private static Stream<Arguments> isEmptyTestCases() {
        return Stream.of(// -------- Branch‑coverage group ----------
        Arguments.of(null, true, "null input should be treated as empty"), Arguments.of("", true, "empty string should be treated as empty"), Arguments.of("   ", true, "string with only spaces is empty"), Arguments.of("\t\n\r", true, "string with only tab, newline and carriage‑return is empty"), Arguments.of("\u2003", true, "Unicode EM SPACE (U+2003) is whitespace"), Arguments.of("abc", false, "non‑whitespace characters make the string non‑empty"), Arguments.of(" abc ", false, "mixed whitespace and non‑whitespace is non‑empty"), // -------- Additional edge‑cases ----------
        Arguments.of("\u00A0", true, "Non‑breaking space (U+00A0) is whitespace"), Arguments.of("\u3000", true, "Ideographic space (U+3000) is whitespace"), Arguments.of("\u202F", true, "Thin space (U+202F) is whitespace"), Arguments.of("\u200B", false, "Zero‑width space (U+200B) is NOT whitespace"), Arguments.of("\uFEFF", false, "Byte Order Mark (U+FEFF) is NOT whitespace"), Arguments.of(" \u2003 \t", true, "combination of space, EM SPACE and tab is empty"), Arguments.of("a\u2003b", false, "non‑whitespace characters surrounding EM SPACE are non‑empty"), Arguments.of("\u2003a", false, "whitespace followed by non‑whitespace is non‑empty"), Arguments.of("   \u2003   ", true, "spaces and EM SPACE only are empty"), Arguments.of("\u2028\u2029", true, "line separator and paragraph separator are whitespace"));
    }

    @Test
    void isEmpty_returnsTrue_whenInputIsNull() {
        // given
        String input = null;
        // when
        boolean result = StringUtilities.isEmpty(input);
        // then
        assertTrue(result, "isEmpty should return true for null input");
    }

    @Test
    void isEmpty_returnsTrue_whenInputIsEmptyString() {
        // given
        String input = "";
        // when
        boolean result = StringUtilities.isEmpty(input);
        // then
        assertTrue(result, "isEmpty should return true for empty string");
    }

    @Test
    void isEmpty_returnsTrue_whenInputContainsOnlyWhitespace() {
        // given
        String input = " \t\n\r  ";
        // when
        boolean result = StringUtilities.isEmpty(input);
        // then
        assertTrue(result, "isEmpty should return true for a string that contains only whitespace characters");
    }

    @Test
    void isEmpty_returnsFalse_whenInputContainsNonWhitespaceCharacters() {
        // given
        String input = "abc";
        // when
        boolean result = StringUtilities.isEmpty(input);
        // then
        assertFalse(result, "isEmpty should return false for a non‑empty, non‑whitespace string");
    }

    @Test
    void isEmpty_returnsFalse_whenInputContainsMixedWhitespaceAndNonWhitespace() {
        // given
        String input = "  foo  ";
        // when
        boolean result = StringUtilities.isEmpty(input);
        // then
        assertFalse(result, "isEmpty should return false when the string contains any non‑whitespace characters");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("isEmptyTestCases")
    void isEmpty_parameterisedTests(String input, boolean expected, String description) {
        // when
        boolean result = StringUtilities.isEmpty(input);
        // then
        if (expected) {
            assertTrue(result, () -> "Expected true for case: " + description);
        } else {
            assertFalse(result, () -> "Expected false for case: " + description);
        }
    }
}
