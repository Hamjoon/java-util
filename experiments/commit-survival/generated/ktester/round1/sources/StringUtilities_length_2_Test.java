package com.cedarsoftware.util;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class StringUtilities_length_2_Test{

    /* ----------------------------------------------------------------------
     *  Lifecycle methods – the class under test has no mutable state nor
     *  external resources, so the bodies are intentionally empty.  They are
     *  kept for symmetry with the provided template and to illustrate where
     *  future setup/teardown logic would belong.
     * ---------------------------------------------------------------------- */
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

    /* ----------------------------------------------------------------------
     *  Tests for StringUtilities.length(String)
     *
     *  The method contract is:
     *      - If the argument is null → return 0
     *      - Otherwise → return s.length()
     *
     *  The following test cases cover the boundary conditions and typical
     *  usage scenarios.
     * ---------------------------------------------------------------------- */

    @Test
    @DisplayName("length(null) should return 0")
    void length_nullInput_returnsZero() {
        // GIVEN
        String input = null;

        // WHEN
        int result = StringUtilities.length(input);

        // THEN
        assertEquals(0, result, "Expected length of null to be 0");
    }

    @Test
    @DisplayName("length(empty string) should return 0")
    void length_emptyString_returnsZero() {
        // GIVEN
        String input = "";

        // WHEN
        int result = StringUtilities.length(input);

        // THEN
        assertEquals(0, result, "Expected length of empty string to be 0");
    }

    @Test
    @DisplayName("length(single‑character string) should return 1")
    void length_singleCharacter_returnsOne() {
        // GIVEN
        String input = "a";

        // WHEN
        int result = StringUtilities.length(input);

        // THEN
        assertEquals(1, result, "Expected length of a single‑character string to be 1");
    }

    @Test
    @DisplayName("length(multi‑character string) should return correct size")
    void length_multiCharacterString_returnsCorrectLength() {
        // GIVEN
        String input = "OpenAI ChatGPT";

        // WHEN
        int result = StringUtilities.length(input);

        // THEN
        assertEquals(input.length(), result,
                "Expected length to match String.length() for a regular string");
    }

    @Test
    @DisplayName("length(string with Unicode surrogate pairs) counts code units")
    void length_unicodeString_countsCodeUnits() {
        // GIVEN
        // The string contains a musical G clef (U+1D11E) which is represented by a surrogate pair.
        String input = "𝄞music"; // length should be 6 code units (2 for the glyph + 4 ASCII)

        // WHEN
        int result = StringUtilities.length(input);

        // THEN
        assertEquals(input.length(), result,
                "StringUtilities.length should count Java char units, not Unicode code points");
    }

    @Test
    @DisplayName("length(very long string) should handle large sizes")
    void length_veryLongString_returnsCorrectLength() {
        // GIVEN
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10_000; i++) {
            sb.append('x');
        }
        String input = sb.toString();

        // WHEN
        int result = StringUtilities.length(input);

        // THEN
        assertEquals(10_000, result, "Expected length of the generated long string to be 10,000");
    }
}