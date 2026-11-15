package com.cedarsoftware.util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.junit.jupiter.MockitoExtension.*;

/**
 * Tests for {@link StringUtilities#isEmpty(String)}.
 *
 * The method is a thin wrapper around {@link StringUtilities#isWhitespace(CharSequence)}.
 * The test suite therefore verifies the contract of {@code isEmpty} for:
 * <ul>
 *   <li>null values</li>
 *   <li>empty strings</li>
 *   <li>strings that consist only of whitespace characters</li>
 *   <li>strings that contain at least one non‑whitespace character</li>
 * </ul>
 *
 * No external dependencies are required; the class StringUtilities_isEmpty_2_Test*/
@ExtendWith(MockitoExtension.class)
class StringUtilities_isEmpty_2_Test {

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialise.
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /* --------------------------------------------------------------------- *
     *  Parameterised tests
     * --------------------------------------------------------------------- */

    @ParameterizedTest
    @MethodSource("stringsWithAllWhitespace")
    void testIsEmpty_whenStringHasOnlyWhitespace_returnsTrue(String s) {
        assertTrue(StringUtilities.isEmpty(s),
                () -> "Expected isEmpty to return true for whitespace-only string: [" + s + "]");
    }

    @ParameterizedTest
    @MethodSource("stringsWithContentOtherThanWhitespace")
    void testIsEmpty_whenStringHasContent_returnsFalse(String s) {
        assertFalse(StringUtilities.isEmpty(s),
                () -> "Expected isEmpty to return false for string with non‑whitespace content: [" + s + "]");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testIsEmpty_whenNullOrEmpty_returnsTrue(String s) {
        assertTrue(StringUtilities.isEmpty(s),
                () -> "Expected isEmpty to return true for null or empty string: [" + s + "]");
    }

    /* --------------------------------------------------------------------- *
     *  Argument providers
     * --------------------------------------------------------------------- */

    private static Stream<String> stringsWithAllWhitespace() {
        return Stream.of(
                " ",
                "\t",
                "\n",
                "\r",
                "\f",
                " \t\n\r\f",
                "\u2000", // EN QUAD – considered whitespace by Character.isWhitespace
                "\u3000"  // IDEOGRAPHIC SPACE – also whitespace
        );
    }

    private static Stream<String> stringsWithContentOtherThanWhitespace() {
        return Stream.of(
                "a",
                "0",
                "false",
                "null",
                "text",
                "  a  ",      // whitespace surrounding a non‑whitespace char
                "x\t",        // non‑whitespace followed by a tab
                "\nY",        // newline followed by a non‑whitespace char
                "🚀",         // Unicode emoji (non‑whitespace)
                "foo bar",    // internal space but also letters
                "   \u200B   " // zero‑width space is NOT considered whitespace by isWhitespace
        );
    }
}