package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringUtilities_trimEmptyToDefault_Test {

    /**
     * Supplies all test cases for {@link #trimEmptyToDefault_cases}.
     *
     * @return a stream of arguments: value, defaultValue, expected, description
     */
    private static Stream<Arguments> trimEmptyToDefaultProvider() {
        return Stream.of(// 1. value is null, default is non‑null
        Arguments.of(null, "default", "default", "When the first argument is null, the method should bypass trimToNull and directly return the supplied default value."), // 2. value is whitespace only, default non‑null
        Arguments.of("   ", "fallback", "fallback", "A non‑null string that becomes empty after trimming (only whitespace) causes trimToNull to return null, so the method should fall back to the default value."), // 3. value is empty string, default non‑null
        Arguments.of("", "fallback", "fallback", "An empty string is treated as empty by trimToNull, resulting in the default value being returned."), // 4. value with surrounding whitespace, default non‑null
        Arguments.of("  abc  ", "fallback", "abc", "A string with surrounding whitespace is trimmed to a non‑null value; the method should return the trimmed string, ignoring the default."), // 5. value non‑null, default null
        Arguments.of("xyz", null, "xyz", "When the default value is null but the input string is non‑null and non‑empty after trimming, the method returns the trimmed input."), // 6. both parameters null
        Arguments.of(null, null, null, "Both parameters are null; the method should return null because there is no value to fall back on."), // 7. value contains various whitespace characters, default non‑null
        Arguments.of("\t\n  \r", "default", "default", "String consisting only of various whitespace characters becomes null after trimToNull, so the default is returned."), // 8. value with leading whitespace only, default non‑null
        Arguments.of("  leading", "default", "leading", "String with leading whitespace only; after trimToNull it yields a non‑null trimmed string which should be returned."), // 9. value null, default empty string
        Arguments.of(null, "", "", "Value is null and default is an empty string; the method should return the empty default unchanged."), //10. value whitespace only, default empty string
        Arguments.of("   ", "", "", "Value consists only of whitespace, which becomes null after trimToNull; the empty default should be returned."), //11. value with surrounding whitespace, default empty string
        Arguments.of("  abc  ", "", "abc", "Value has surrounding whitespace and a non‑empty core; after trimming the core is returned, ignoring the empty default."), //12. value null, default whitespace only
        Arguments.of(null, "   ", "   ", "Value is null but default contains only whitespace; the method returns the default as‑is (no trimming of the default)."), //13. both value and default whitespace only
        Arguments.of("   ", "   ", "   ", "Both value and default are whitespace‑only strings; trimToNull turns the value into null, so the whitespace default is returned unchanged."), //14. value with internal whitespace, default non‑null
        Arguments.of("a b", "fallback", "a b", "Value contains internal whitespace only; trimToNull does not modify internal spaces, so the original string is returned."), //15. value is EM SPACE (Unicode whitespace not trimmed by String.trim)
        Arguments.of("\u2003", "def", "\u2003", "Value is an EM SPACE (Unicode whitespace not trimmed by String.trim); trimToNull returns the original string, which is then returned."), //16. value contains non‑breaking spaces (U+00A0)
        Arguments.of("\u00A0test\u00A0", "def", "\u00A0test\u00A0", "Value contains non‑breaking spaces (U+00A0) which are not removed by trim; the method returns the original string unchanged."), //17. value whitespace only, default null
        Arguments.of("\t\n", null, null, "Value is whitespace only, becoming null after trimToNull, and default is null; the method should return null."), //18. value whitespace only, default null (duplicate scenario with different whitespace)
        Arguments.of("   ", null, null, "Whitespace‑only value yields null after trimToNull and default is null, so the result is null."), //19. non‑empty value, default empty string
        Arguments.of("nonempty", "", "nonempty", "Non‑empty value is returned after trimming (no change), regardless of the empty default."), //20. both value and default empty strings
        Arguments.of("", "", "", "Both value and default are empty strings; trimToNull converts the value to null, so the empty default is returned."), //21. empty value, default null
        Arguments.of("", null, null, "Empty value becomes null after trimToNull and default is null; the method should return null."));
    }

    @Test
    void whenValueIsNull_returnsDefaultValue() {
        String defaultValue = "default";
        String result = StringUtilities.trimEmptyToDefault(null, defaultValue);
        assertEquals(defaultValue, result, "Expected default value when input is null");
    }

    @Test
    void whenValueIsBlank_returnsDefaultValue() {
        String defaultValue = "fallback";
        String result = StringUtilities.trimEmptyToDefault("   ", defaultValue);
        assertEquals(defaultValue, result, "Expected default value when input is blank after trim");
    }

    @Test
    void whenValueIsNonBlank_returnsTrimmedValue() {
        String defaultValue = "default";
        String result = StringUtilities.trimEmptyToDefault("  abc  ", defaultValue);
        assertEquals("abc", result, "Expected trimmed non‑blank value to be returned");
    }

    @Test
    void whenValueIsAlreadyTrimmed_returnsSameValue() {
        String defaultValue = "default";
        String result = StringUtilities.trimEmptyToDefault("xyz", defaultValue);
        assertEquals("xyz", result, "Expected original value to be returned unchanged");
    }

    @Test
    void whenDefaultIsNull_andValueIsNull_returnsNull() {
        String result = StringUtilities.trimEmptyToDefault(null, null);
        assertNull(result, "Expected null when both value and default are null");
    }

    @Test
    void whenDefaultIsNull_andValueIsBlank_returnsNull() {
        String result = StringUtilities.trimEmptyToDefault("   ", null);
        assertNull(result, "Expected null when value becomes null after trim and default is null");
    }

    @Test
    void whenDefaultIsNull_andValueIsNonBlank_returnsTrimmedValue() {
        String result = StringUtilities.trimEmptyToDefault("  hello ", null);
        assertEquals("hello", result, "Expected trimmed value even when default is null");
    }

    /**
     * Parameterized test that executes every scenario described in the
     * supplied JSON document.  The {@code description} argument is used as the
     * display name for each invocation, making test reports easy to read.
     *
     * @param value         the first argument to {@code trimEmptyToDefault}
     * @param defaultValue  the second argument to {@code trimEmptyToDefault}
     * @param expected      the expected return value (may be {@code null})
     * @param description   a human‑readable description of the scenario
     */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("trimEmptyToDefaultProvider")
    void trimEmptyToDefault_cases(String value, String defaultValue, String expected, String description) {
        // Act
        String result = StringUtilities.trimEmptyToDefault(value, defaultValue);
        // Assert
        if (expected == null) {
            assertNull(result, description);
        } else {
            assertEquals(expected, result, description);
        }
    }
}
