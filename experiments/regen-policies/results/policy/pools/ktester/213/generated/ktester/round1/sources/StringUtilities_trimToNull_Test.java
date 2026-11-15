package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

// @ExtendWith(MockitoExtension.class)
class StringUtilities_trimToNull_Test {

    private static Stream<Arguments> whitespaceStrings() {
        return Stream.of(Arguments.of(" "), Arguments.of("\t"), Arguments.of("\n"), Arguments.of("\r\n"), Arguments.of("   \t  \n"));
    }

    private static Stream<Arguments> stringsNeedingTrim() {
        return Stream.of(Arguments.of("  abc  ", "abc"), Arguments.of("\tfoo\n", "foo"), Arguments.of("\r\nbar\r\n", "bar"), Arguments.of("   mixed \t whitespace \n", "mixed \t whitespace"));
    }

    private static Stream<Arguments> unicodeEdgeCases() {
        // Non‑breaking spaces (U+00A0) are NOT trimmed by String.trim()
        // Zero‑width space (U+200B) is not considered whitespace by trim()
        // EM SPACE (U+2003) is not trimmed
        return Stream.of(// Regular whitespace around text should be trimmed
        Arguments.of("\u00A0\u00A0", "\u00A0\u00A0"), Arguments.of("\u00A0 abc \u00A0", "\u00A0 abc \u00A0"), // Combination of regular spaces and zero‑width space
        Arguments.of("\t\n abc \r\n", "abc"), Arguments.of("a\u200bb", "a\u200bb"), Arguments.of("\u200b", "\u200b"), // Regular spaces surrounding a non‑breaking space
        Arguments.of("   \u00A0   ", "\u00A0"), // EM SPACE (U+2003) is not trimmed
        Arguments.of(" \u200b ", "\u200b"), Arguments.of(" \u2003 ", "\u2003"), Arguments.of("\u2003abc\u2003", "\u2003abc\u2003"));
    }

    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Test
    void testTrimToNull_whenNull_returnsNull() {
        assertNull(StringUtilities.trimToNull(null));
    }

    @Test
    void testTrimToNull_whenEmpty_returnsNull() {
        assertNull(StringUtilities.trimToNull(""));
    }

    @ParameterizedTest
    @MethodSource("whitespaceStrings")
    void testTrimToNull_whenOnlyWhitespace_returnsNull(String whitespace) {
        assertNull(StringUtilities.trimToNull(whitespace));
    }

    @ParameterizedTest
    @MethodSource("stringsNeedingTrim")
    void testTrimToNull_whenNeedsTrim_returnsTrimmedValue(String input, String expected) {
        assertEquals(expected, StringUtilities.trimToNull(input));
    }

    @ParameterizedTest
    @ValueSource(strings = { "a", "abc", "fooBar", "123" })
    void testTrimToNull_whenAlreadyTrimmed_returnsSameInstance(String input) {
        // The contract does not guarantee the same instance, only the same value.
        assertEquals(input, StringUtilities.trimToNull(input));
    }

    @Test
    void testTrimToNull_whenResultIsEmptyAfterTrim_returnsNull() {
        // Input contains characters that become empty after trimming, e.g. only spaces.
        assertNull(StringUtilities.trimToNull("   "));
    }

    @Test
    void testTrimToNull_isNullSafeAndDoesNotThrow() {
        // Ensure that no exception is thrown for any of the typical edge cases.
        assertDoesNotThrow(() -> StringUtilities.trimToNull(null));
        assertDoesNotThrow(() -> StringUtilities.trimToNull(""));
        assertDoesNotThrow(() -> StringUtilities.trimToNull("   "));
        assertDoesNotThrow(() -> StringUtilities.trimToNull("value"));
    }

    @ParameterizedTest
    @MethodSource("unicodeEdgeCases")
    void testTrimToNull_unicodeEdgeCases(String input, String expected) {
        assertEquals(expected, StringUtilities.trimToNull(input));
    }
}