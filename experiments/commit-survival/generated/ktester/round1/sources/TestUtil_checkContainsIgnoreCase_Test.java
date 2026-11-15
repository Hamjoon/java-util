package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TestUtil_checkContainsIgnoreCase_Test {

    static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(Arguments.of("HelloWorld", new String[] { "hello" }, true, "Single substring present, case‑insensitive match should return true."), Arguments.of("HelloWorld", new String[] { "world" }, true, "Single substring present at the end, case‑insensitive match should return true."), Arguments.of("HelloWorld", new String[] { "test" }, false, "Substring not present, method should return false."));
    }

    static Stream<Arguments> emptyInputsProvider() {
        return Stream.of(Arguments.of("", new String[0], true, "Both source and contains are empty; with no substrings to verify the method should return true."), Arguments.of("", new String[] { "" }, true, "Empty source with a single empty substring; indexOf(\"\") returns 0, so the method returns true."), Arguments.of("", new String[] { "a" }, false, "Empty source with a non‑empty substring; indexOf returns -1, method should return false."), Arguments.of("NonEmpty", new String[0], true, "Non‑empty source with no substrings to check; the loop never executes and the method returns true."));
    }

    static Stream<Arguments> orderSensitivityProvider() {
        return Stream.of(Arguments.of("abcde", new String[] { "c", "a" }, false, "Substrings appear in source but out of the supplied order; after finding \"c\" the remaining source is \"cde\" which no longer contains \"a\"."), Arguments.of("abcde", new String[] { "a", "c" }, true, "Substrings appear in the correct order; each step reduces the source correctly and the method returns true."), Arguments.of("ababa", new String[] { "aba", "ba" }, true, "Overlapping substrings are allowed; after locating \"aba\" the remaining source is still \"ababa\" and \"ba\" is found."), Arguments.of("ababa", new String[] { "ba", "aba" }, false, "When order is reversed, after finding \"ba\" the remaining source is \"ba\" and \"aba\" cannot be found, resulting in false."));
    }

    static Stream<Arguments> localeHandlingProvider() {
        return Stream.of(Arguments.of("CaféMünchen", new String[] { "É", "ü" }, true, "Unicode characters with different case; toLowerCase handles them and the method should return true."), Arguments.of("Straße", new String[] { "SS" }, true, "German sharp‑s \"ß\" lower‑cased becomes \"ß\"; however \"SS\" lower‑cased is \"ss\" which is a substring of \"straße\" after conversion, so the method returns true."));
    }

    static Stream<Arguments> emptySubstringProvider() {
        return Stream.of(Arguments.of("anyvalue", new String[] { "" }, true, "An empty substring is always found at index 0; method should return true regardless of source content."), Arguments.of("anyvalue", new String[] { "", "value" }, true, "Multiple substrings where the first is empty (always true) and the second is present; overall result is true."));
    }

    static Stream<Arguments> nullHandlingProvider() {
        return Stream.of(Arguments.of(null, new String[] { "anything" }, "NullPointerException", "Source is null; invoking toLowerCase() throws NullPointerException."), Arguments.of("some text", new String[] { null }, "NullPointerException", "One of the substrings is null; calling toLowerCase() on it throws NullPointerException."), Arguments.of(null, null, "NullPointerException", "Both parameters are null; the method attempts to call toLowerCase() on source first and fails."));
    }

    static Stream<Arguments> longerSubstringProvider() {
        return Stream.of(Arguments.of("short", new String[] { "muchlonger" }, false, "Substring length exceeds source length; indexOf returns -1, method returns false."), Arguments.of("abc", new String[] { "abcd", "bc" }, false, "First substring is longer than source, causing immediate false before checking the second."));
    }

    static Stream<Arguments> nullVarargsProvider() {
        return Stream.of(Arguments.of("text", null, "NullPointerException", "Varargs array is null; iterating over it throws NullPointerException."));
    }

    static Stream<Arguments> nullSourceEmptyContainsProvider() {
        return Stream.of(Arguments.of(null, new String[0], "NullPointerException", "Source is null; calling toLowerCase() throws NullPointerException even though the contains array is empty."));
    }

    /* -------------------------------------------------------------
     *  Helper method that decides between a normal assertion and an
     *  exception expectation based on the supplied {@code expected}
     *  argument.
     * ------------------------------------------------------------- */
    private void executeTest(String source, String[] contains, Object expected) {
        if (expected instanceof Boolean) {
            boolean actual = TestUtil.checkContainsIgnoreCase(source, contains);
            assertEquals(expected, actual, () -> String.format("Source='%s', contains=%s", source, java.util.Arrays.toString(contains)));
        } else if ("NullPointerException".equals(expected)) {
            assertThrows(NullPointerException.class, () -> TestUtil.checkContainsIgnoreCase(source, contains), () -> String.format("Expected NullPointerException for source='%s', contains=%s", source, java.util.Arrays.toString(contains)));
        } else {
            fail("Unsupported expected value: " + expected);
        }
    }

    @Test
    void basicPositiveMatch() {
        assertTrue(TestUtil.checkContainsIgnoreCase("This is the source string to test.", "Source", "string", "Test"));
    }

    @Test
    void basicNegativeMatch_MissingLastToken() {
        assertFalse(TestUtil.checkContainsIgnoreCase("This is the source string to test.", "Source", "string", "Text"));
    }

    @Test
    void basicNegativeMatch_WrongOrder() {
        // The method requires the tokens to appear in the given order.
        assertFalse(TestUtil.checkContainsIgnoreCase("This is the source string to test.", "Test", "Source", "string"));
    }

    @Test
    void caseInsensitivity() {
        assertTrue(TestUtil.checkContainsIgnoreCase("MiXeD CaSe StRiNg", "mixed", "case", "string"));
    }

    @Test
    void emptyContainsArrayReturnsTrue() {
        // No substrings to look for – by definition the source contains "nothing".
        assertTrue(TestUtil.checkContainsIgnoreCase("any source"));
    }

    @Test
    void emptySourceWithEmptyContains() {
        assertTrue(TestUtil.checkContainsIgnoreCase("", new String[0]));
    }

    @Test
    void emptySourceWithNonEmptyContainsReturnsFalse() {
        assertFalse(TestUtil.checkContainsIgnoreCase("", "a"));
    }

    @Test
    void containsSubstringAtStartAndEnd() {
        assertTrue(TestUtil.checkContainsIgnoreCase("Start middle End", "Start", "End"));
    }

    @Test
    void overlappingSubstrings() {
        // "aaa" contains "aa" twice; the algorithm should still succeed.
        assertTrue(TestUtil.checkContainsIgnoreCase("aaa", "aa", "a"));
    }

    @Test
    void nullSourceThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> TestUtil.checkContainsIgnoreCase(null, "anything"));
    }

    @Test
    void nullElementInContainsArrayThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> TestUtil.checkContainsIgnoreCase("source", (String) null));
    }

    /* -------------------------------------------------------------
     *  1. Basic functionality
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("basicFunctionalityProvider")
    void basicFunctionality(String source, String[] contains, Object expected, String description) {
        executeTest(source, contains, expected);
    }

    /* -------------------------------------------------------------
     *  2. Empty inputs
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("emptyInputsProvider")
    void emptyInputs(String source, String[] contains, Object expected, String description) {
        executeTest(source, contains, expected);
    }

    /* -------------------------------------------------------------
     *  3. Order sensitivity & overlapping substrings
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("orderSensitivityProvider")
    void orderSensitivity(String source, String[] contains, Object expected, String description) {
        executeTest(source, contains, expected);
    }

    /* -------------------------------------------------------------
     *  4. Case and locale handling
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("localeHandlingProvider")
    void localeHandling(String source, String[] contains, Object expected, String description) {
        executeTest(source, contains, expected);
    }

    /* -------------------------------------------------------------
     *  5. Empty string as a substring
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("emptySubstringProvider")
    void emptySubstring(String source, String[] contains, Object expected, String description) {
        executeTest(source, contains, expected);
    }

    /* -------------------------------------------------------------
     *  6. Null handling (source or elements)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("nullHandlingProvider")
    void nullHandling(String source, String[] contains, Object expected, String description) {
        executeTest(source, contains, expected);
    }

    /* -------------------------------------------------------------
     *  7. Substrings longer than source
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("longerSubstringProvider")
    void longerSubstring(String source, String[] contains, Object expected, String description) {
        executeTest(source, contains, expected);
    }

    /* -------------------------------------------------------------
     *  8. Null var‑args array
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("nullVarargsProvider")
    void nullVarargs(String source, String[] contains, Object expected, String description) {
        executeTest(source, contains, expected);
    }

    /* -------------------------------------------------------------
     *  9. Null source with empty contains (explicit test)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("nullSourceEmptyContainsProvider")
    void nullSourceEmptyContains(String source, String[] contains, Object expected, String description) {
        executeTest(source, contains, expected);
    }
}
