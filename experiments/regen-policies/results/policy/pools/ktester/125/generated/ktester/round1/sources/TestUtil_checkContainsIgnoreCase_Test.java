package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

// No mocks are required for this class, but the annotation is kept from the template
@ExtendWith(MockitoExtension.class)
class TestUtil_checkContainsIgnoreCase_Test {

    private static Stream<Arguments> positiveCasesProvider() {
        return Stream.of(Arguments.of("Single token present in source, case‑insensitive match should return true.", "Hello World", new String[] { "hello" }, true), Arguments.of("Single token present at the end of the source, case‑insensitive match should return true.", "Hello World", new String[] { "world" }, true), Arguments.of("Multiple tokens appear sequentially; each is found after the previous one, so the method returns true.", "Hello World", new String[] { "hello", "world" }, true), Arguments.of("Overlapping substrings with different cases are found in order; method should return true.", "abcXYZabc", new String[] { "XYZ", "abc" }, true), Arguments.of("Mixed‑case token matches source regardless of case.", "AbCdeF", new String[] { "aBc" }, true), Arguments.of("Upper‑case token matches lower‑case part of source.", "AbCdeF", new String[] { "DEF" }, true));
    }

    private static Stream<Arguments> edgeCasesProvider() {
        return Stream.of(Arguments.of("Both source and contains are empty; with no tokens to check the method should return true.", "", new String[] {}, true), Arguments.of("Empty source cannot contain any non‑empty token; method should return false.", "", new String[] { "a" }, false), Arguments.of("An empty string token is always found at index 0; method should ignore it and return true.", "nonempty", new String[] { "" }, true), Arguments.of("Mixed empty and non‑empty tokens; empty token does not affect the search, remaining token is found.", "nonempty", new String[] { "", "non" }, true));
    }

    private static Stream<Arguments> exceptionCasesProvider() {
        return Stream.of(Arguments.of("source is null; method should throw NullPointerException when calling toLowerCase().", null, new String[] {}, NullPointerException.class));
    }

    @Test
    @DisplayName("All substrings present in order, different cases")
    void testAllSubstringsPresent() {
        boolean result = TestUtil.checkContainsIgnoreCase("This is the source string to test.", "Source", "string", "Test");
        assertTrue(result, "All substrings should be found in order, ignoring case");
    }

    @Test
    @DisplayName("Empty var‑args should return true")
    void testEmptyContainsArray() {
        boolean result = TestUtil.checkContainsIgnoreCase(// source can be any non‑null string
        "Anything at all", // no substrings to look for
        new String[0]);
        assertTrue(result, "When no substrings are supplied the method should return true");
    }

    @Test
    @DisplayName("Source and substrings are identical (case‑insensitive)")
    void testExactMatchDifferentCase() {
        assertTrue(TestUtil.checkContainsIgnoreCase("ABCdef", "abcDEF"));
    }

    @Test
    @DisplayName("Source contains repeated substring")
    void testRepeatedSubstring() {
        assertTrue(TestUtil.checkContainsIgnoreCase("ababab", "ab", "ab", "ab"));
    }

    @Test
    @DisplayName("Substring not present")
    void testMissingSubstring() {
        boolean result = TestUtil.checkContainsIgnoreCase("This is the source string to test.", "Source", "string", // "Text" does not occur
        "Text");
        assertFalse(result, "Method should return false when a substring is missing");
    }

    @Test
    @DisplayName("Substrings present but out of order")
    void testOutOfOrderSubstrings() {
        boolean result = TestUtil.checkContainsIgnoreCase("This is the source string to test.", "Test", "Source", // "Test" appears after the others
        "string");
        assertFalse(result, "Method should return false when substrings are not in the required order");
    }

    @Test
    @DisplayName("Null source should throw NullPointerException")
    void testNullSource() {
        assertThrows(NullPointerException.class, () -> TestUtil.checkContainsIgnoreCase(null, "anything"));
    }

    @Test
    @DisplayName("Null element inside var‑args should throw NullPointerException")
    void testNullElementInContainsArray() {
        assertThrows(NullPointerException.class, () -> TestUtil.checkContainsIgnoreCase("some source", "some", null));
    }

    @Test
    @DisplayName("Empty source string with empty substring")
    void testEmptySourceAndEmptySubstring() {
        assertTrue(TestUtil.checkContainsIgnoreCase("", ""));
    }

    @Test
    @DisplayName("Empty source string with non‑empty substring")
    void testEmptySourceNonEmptySubstring() {
        assertFalse(TestUtil.checkContainsIgnoreCase("", "a"));
    }

    @Test
    @DisplayName("Substring longer than source")
    void testSubstringLongerThanSource() {
        assertFalse(TestUtil.checkContainsIgnoreCase("short", "muchLongerSubstring"));
    }

    /**
     * Parameterised positive cases taken from the supplied JSON.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("positiveCasesProvider")
    void testPositiveCases(String description, String source, String[] contains, boolean expected) {
        boolean actual = TestUtil.checkContainsIgnoreCase(source, contains);
        assertEquals(expected, actual, description);
    }

    /**
     * Parameterised edge‑case tests (empty inputs) from the supplied JSON.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("edgeCasesProvider")
    void testEdgeCases(String description, String source, String[] contains, boolean expected) {
        boolean actual = TestUtil.checkContainsIgnoreCase(source, contains);
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("exceptionCasesProvider")
    void testExceptionCases(String description, String source, String[] contains, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> TestUtil.checkContainsIgnoreCase(source, contains), description);
    }
}
