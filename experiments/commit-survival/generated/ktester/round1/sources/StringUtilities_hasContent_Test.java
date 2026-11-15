package com.cedarsoftware.util;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class StringUtilities_hasContent_Test{

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialise for this utility class
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    /* --------------------------------------------------------------------- *
     *  hasContent(String) – behavioural tests
     * --------------------------------------------------------------------- */

    @Test
    @DisplayName("hasContent returns false for null")
    void hasContent_null() {
        assertFalse(StringUtilities.hasContent(null),
                "hasContent(null) should be false because a null reference has no content");
    }

    @Test
    @DisplayName("hasContent returns false for empty string")
    void hasContent_emptyString() {
        assertFalse(StringUtilities.hasContent(""),
                "hasContent(\"\") should be false because the string is empty");
    }

    @Test
    @DisplayName("hasContent returns false for whitespace‑only strings")
    void hasContent_whitespaceOnly() {
        assertAll(
                () -> assertFalse(StringUtilities.hasContent(" "),
                        "Single space should be considered whitespace only"),
                () -> assertFalse(StringUtilities.hasContent("\t"),
                        "Tab character should be considered whitespace only"),
                () -> assertFalse(StringUtilities.hasContent("\n\r  \t"),
                        "Mixed whitespace characters should be considered whitespace only")
        );
    }

    @Test
    @DisplayName("hasContent returns true for strings containing non‑whitespace characters")
    void hasContent_nonWhitespace() {
        assertAll(
                () -> assertTrue(StringUtilities.hasContent("a"),
                        "A single non‑whitespace character should be considered having content"),
                () -> assertTrue(StringUtilities.hasContent(" abc "),
                        "String with leading/trailing whitespace but containing a non‑whitespace character should have content"),
                () -> assertTrue(StringUtilities.hasContent("0"),
                        "Numeric character should be considered having content"),
                () -> assertTrue(StringUtilities.hasContent("!@#$"),
                        "Special characters should be considered having content")
        );
    }

    @Test
    @DisplayName("hasContent behaves correctly with Unicode whitespace")
    void hasContent_unicodeWhitespace() {
        // Unicode non‑breaking space (U+00A0) is considered whitespace by Character.isWhitespace
        String nonBreakingSpace = "\u00A0";
        assertFalse(StringUtilities.hasContent(nonBreakingSpace),
                "Unicode non‑breaking space should be treated as whitespace only");
    }
}