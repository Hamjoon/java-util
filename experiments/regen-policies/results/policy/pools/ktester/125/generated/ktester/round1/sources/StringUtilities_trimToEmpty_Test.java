package com.cedarsoftware.util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

class StringUtilities_trimToEmpty_Test {

    /**
     * Provides a stream of arguments for the additional edge‑case tests.
     * Each argument consists of:
     *   1. The input string.
     *   2. The expected result after {@code trimToEmpty}.
     */
    private static Stream<Arguments> provideAdditionalTrimCases() {
        return Stream.of(// Non‑breaking spaces (U+00A0) are not trimmed by String.trim()
        Arguments.of("\u00A0\u00A0test\u00A0", "\u00A0\u00A0test\u00A0"), // EM SPACE (U+2003) is not trimmed
        Arguments.of("\u2003\u2003hello\u2003", "\u2003\u2003hello\u2003"), // Leading spaces only
        Arguments.of("   leadingOnly", "leadingOnly"), // Trailing spaces only
        Arguments.of("trailingOnly   ", "trailingOnly"), // Mixed spaces, tab and newline – only outer whitespace removed
        Arguments.of("  mixed \t whitespace \n ", "mixed \t whitespace"), // Zero‑width space (U+200B) is not trimmed
        Arguments.of("\u200BzeroWidthSpace", "\u200BzeroWidthSpace"), // Standard whitespace characters only – result should be empty
        Arguments.of("\t\n\f\r ", ""), // Emoji with trailing spaces
        Arguments.of("😀   ", "😀"), // Emoji with leading spaces
        Arguments.of("   😀", "😀"), // Control character U+001F (unit separator) is trimmed away
        Arguments.of("\u001F", ""));
    }

    @Test
    void trimToEmpty_givenNull_returnsEmptyString() {
        String result = StringUtilities.trimToEmpty(null);
        assertNotNull(result, "Result should never be null");
        assertEquals(StringUtilities.EMPTY, result, "Null input must be converted to empty string");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void trimToEmpty_givenNullOrEmpty_returnsEmptyString(String input) {
        // The method is null‑safe, so both null and "" should yield EMPTY.
        String result = StringUtilities.trimToEmpty(input);
        assertEquals(StringUtilities.EMPTY, result);
    }

    @ParameterizedTest
    @ValueSource(strings = { // spaces only
    "   ", // other whitespace characters
    "\t\n\r", // mixed whitespace
    " \t \n  " })
    void trimToEmpty_givenOnlyWhitespace_returnsEmptyString(String whitespace) {
        String result = StringUtilities.trimToEmpty(whitespace);
        assertEquals(StringUtilities.EMPTY, result);
    }

    @ParameterizedTest
    @ValueSource(strings = { "  abc  ", "\tHello World\n", "\ntrim me\r", "  leading", "trailing  " })
    void trimToEmpty_givenLeadingOrTrailingWhitespace_trimsCorrectly(String input) {
        // the contract is exactly String.trim()
        String expected = input.trim();
        String actual = StringUtilities.trimToEmpty(input);
        assertEquals(expected, actual);
    }

    @ParameterizedTest
    @ValueSource(strings = { "noSpace", "12345", // internal whitespace should be preserved
    "a b c", // internal whitespace preserved after outer trim
    "   a   b   " })
    void trimToEmpty_givenStringWithContent_returnsTrimmedOrOriginal(String input) {
        String expected = input.trim();
        String actual = StringUtilities.trimToEmpty(input);
        assertEquals(expected, actual);
    }

    @Test
    void trimToEmpty_isStateless_andThreadSafe() throws InterruptedException {
        // Run the method concurrently to ensure no shared mutable state.
        final String original = "  concurrent  ";
        final String expected = original.trim();
        Runnable task = () -> {
            for (int i = 0; i < 10_000; i++) {
                assertEquals(expected, StringUtilities.trimToEmpty(original));
            }
        };
        Thread t1 = new Thread(task);
        Thread t2 = new Thread(task);
        Thread t3 = new Thread(task);
        t1.start();
        t2.start();
        t3.start();
        t1.join();
        t2.join();
        t3.join();
    }

    @ParameterizedTest(name = "[{index}] trimToEmpty(\"{0}\") => \"{1}\"")
    @MethodSource("provideAdditionalTrimCases")
    void trimToEmpty_additionalEdgeCases(String input, String expected) {
        String actual = StringUtilities.trimToEmpty(input);
        assertEquals(expected, actual, () -> String.format("trimToEmpty failed for input \"%s\"; expected \"%s\" but got \"%s\"", input, expected, actual));
    }
}
