package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StringUtilities_trimEmptyToDefault_Test {

    private static Stream<Arguments> emptyOrWhitespaceProvider() {
        return Stream.of(Arguments.of(""), Arguments.of("   "), Arguments.of("\t\n\r"), Arguments.of(" \t "));
    }

    private static Stream<Arguments> nonEmptyProvider() {
        return Stream.of(Arguments.of("abc", "abc"), Arguments.of("  abc", "abc"), Arguments.of("abc  ", "abc"), Arguments.of("  abc  ", "abc"), Arguments.of("\t\nabc\r", "abc"), // Additional cases from the JSON spec
        Arguments.of("  hello world  ", "hello world"), Arguments.of("noSpaces", "noSpaces"));
    }

    /**
     * Verify that a {@code null} input returns the supplied default value unchanged.
     */
    @Test
    void trimEmptyToDefault_nullInput_returnsDefault() {
        String defaultValue = "default";
        assertEquals(defaultValue, StringUtilities.trimEmptyToDefault(null, defaultValue));
    }

    /**
     * Verify that an empty string (or a string consisting only of whitespace)
     * is treated as “empty” and the default value is returned.
     */
    @ParameterizedTest
    @MethodSource("emptyOrWhitespaceProvider")
    void trimEmptyToDefault_emptyOrWhitespace_returnsDefault(String value) {
        String defaultValue = "fallback";
        assertEquals(defaultValue, StringUtilities.trimEmptyToDefault(value, defaultValue));
    }

    /**
     * When the input contains non‑whitespace characters, the method should return the
     * trimmed version of the input (the default value is ignored).
     */
    @ParameterizedTest
    @MethodSource("nonEmptyProvider")
    void trimEmptyToDefault_nonEmpty_returnsTrimmedValue(String value, String expected) {
        // The default value should not affect the result.
        String defaultValue = "ignored";
        assertEquals(expected, StringUtilities.trimEmptyToDefault(value, defaultValue));
    }

    /**
     * If the supplied default value is {@code null}, the method should still return
     * {@code null} for a {@code null} or empty input.
     */
    @Test
    void trimEmptyToDefault_nullDefaultAndNullInput_returnsNull() {
        assertNull(StringUtilities.trimEmptyToDefault(null, null));
    }

    @Test
    void trimEmptyToDefault_nullDefaultAndEmptyInput_returnsNull() {
        assertNull(StringUtilities.trimEmptyToDefault("   ", null));
    }

    /**
     * The default value is returned verbatim – it is **not** trimmed.
     */
    @Test
    void trimEmptyToDefault_defaultValueIsNotTrimmed() {
        String defaultValue = "   default   ";
        assertEquals(defaultValue, StringUtilities.trimEmptyToDefault(null, defaultValue));
    }

    /**
     * Whitespace‑only string with a {@code null} default should also yield {@code null}.
     */
    @Test
    void trimEmptyToDefault_whitespaceOnlyAndNullDefault_returnsNull() {
        assertNull(StringUtilities.trimEmptyToDefault("\t\n\r", null));
    }
}
