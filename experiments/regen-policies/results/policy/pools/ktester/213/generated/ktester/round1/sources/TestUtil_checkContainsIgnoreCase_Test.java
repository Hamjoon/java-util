package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TestUtil_checkContainsIgnoreCase_Test {

    static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(Arguments.of("HelloWorld", new String[] { "world" }, true, "Single substring present in source, different case – should return true."), Arguments.of("HelloWorld", new String[] { "test" }, false, "Single substring not present – method should return false at first check."), Arguments.of("HelloWorld", new String[] {}, true, "Empty var‑args array – loop is skipped and method returns true."), Arguments.of("abcde", new String[] { "cd", "de" }, true, "Two substrings appear sequentially; after first match the search continues from that index, both are found."), Arguments.of("abcde", new String[] { "de", "cd" }, false, "Second substring appears before the first one in the original string; after matching \"de\" the remaining source does not contain \"cd\"."), Arguments.of("HelloWorld", new String[] { "hello", "WORLD" }, true, "Multiple substrings with mixed case – all are found case‑insensitively."));
    }

    static Stream<Arguments> emptyStringsProvider() {
        return Stream.of(Arguments.of("", new String[] { "" }, true, "Both source and substring are empty; indexOf(\"\") returns 0, so method returns true."), Arguments.of("", new String[] { "a" }, false, "Empty source with a non‑empty substring – indexOf returns -1, method returns false."));
    }

    static Stream<Arguments> errorHandlingProvider() {
        return Stream.of(Arguments.of(null, new String[] { "a" }, NullPointerException.class, "Source is null; calling toLowerCase() throws NPE."), Arguments.of("test", new String[] { null }, NullPointerException.class, "One element of the var‑args array is null; contain.toLowerCase() throws NPE."), Arguments.of("anything", null, NullPointerException.class, "The var‑args parameter itself is null; the enhanced for‑loop attempts to iterate over a null array and throws NPE."));
    }

    static Stream<Arguments> emptySubstringProvider() {
        return Stream.of(Arguments.of("abc", new String[] { "", "b" }, true, "First element is an empty string (indexOf(\"\") returns 0) – search continues unchanged and the second element \"b\" is found; method should return true."), Arguments.of("abc", new String[] { "", "d" }, false, "Empty string matches at position 0, but the subsequent non‑empty substring \"d\" is not present; method should return false."), Arguments.of("nonempty", new String[] { "", "" }, true, "Array consists only of empty strings; each indexOf(\"\") returns 0, loop never fails, so the method returns true."));
    }

    static Stream<Arguments> repeatedSubstringsProvider() {
        return Stream.of(Arguments.of("aaa", new String[] { "a", "a", "a" }, true, "The same substring \"a\" is searched three times; after each match the remaining source still starts with \"a\", so all three succeed."));
    }

    static Stream<Arguments> overlappingSubstringsProvider() {
        return Stream.of(Arguments.of("ababa", new String[] { "aba", "ba" }, true, "First substring \"aba\" matches at index 0; the remaining source becomes \"ababa\" (same string). The second substring \"ba\" is found starting at index 1, demonstrating correct handling of overlapping matches."));
    }

    static Stream<Arguments> whitespaceHandlingProvider() {
        return Stream.of(Arguments.of("  HelloWorld", new String[] { "hello" }, true, "Source contains leading spaces; after lower‑casing, \"hello\" is found at index 2, so the method returns true."), Arguments.of("Line1\nLine2", new String[] { "\nli" }, true, "Substring includes a newline character; indexOf correctly locates it, confirming the method works with control characters."), Arguments.of("tab\tseparated", new String[] { "\tsep" }, true, "Substring contains a tab character; method should still locate it case‑insensitively."));
    }

    static Stream<Arguments> emptySourceMixedProvider() {
        return Stream.of(Arguments.of("", new String[] { "", "a" }, false, "First empty string matches at index 0, but the subsequent non‑empty substring \"a\" cannot be found in an empty source; method returns false."));
    }

    @Test
    @DisplayName("All substrings present in the correct order → true")
    void testAllSubstringsPresentInOrder() {
        boolean result = TestUtil.checkContainsIgnoreCase("This is the source string to test.", "Source", "string", "Test");
        assertTrue(result, "Expected all substrings to be found in order");
    }

    @Test
    @DisplayName("Missing substring → false")
    void testMissingSubstring() {
        boolean result = TestUtil.checkContainsIgnoreCase("This is the source string to test.", "Source", "string", "Text");
        assertFalse(result, "Expected false because \"Text\" is not present");
    }

    @Test
    @DisplayName("Substrings present but wrong order → false")
    void testSubstringsWrongOrder() {
        boolean result = TestUtil.checkContainsIgnoreCase("This is the source string to test.", "Test", "Source", "string");
        assertFalse(result, "Expected false because substrings are not in the required order");
    }

    @Test
    @DisplayName("Case‑insensitivity works")
    void testCaseInsensitivity() {
        assertTrue(TestUtil.checkContainsIgnoreCase("AbC DeF gHi", "aBc", "DEF", "ghi"));
    }

    @Test
    @DisplayName("Empty var‑args → true (nothing to search for)")
    void testEmptyVarArgs() {
        assertTrue(TestUtil.checkContainsIgnoreCase("any source string"));
    }

    @Test
    @DisplayName("Empty source with empty search string → true")
    void testEmptySourceAndEmptySearch() {
        assertTrue(TestUtil.checkContainsIgnoreCase("", ""));
    }

    @Test
    @DisplayName("Empty source with non‑empty search → false")
    void testEmptySourceNonEmptySearch() {
        assertFalse(TestUtil.checkContainsIgnoreCase("", "nonempty"));
    }

    @Test
    @DisplayName("Search string longer than source → false")
    void testSearchLongerThanSource() {
        assertFalse(TestUtil.checkContainsIgnoreCase("short", "muchlonger"));
    }

    @Test
    @DisplayName("Repeated substrings are handled correctly")
    void testRepeatedSubstrings() {
        // The method will look for the first occurrence of each subsequent substring
        // starting from the previous match position.
        assertTrue(TestUtil.checkContainsIgnoreCase("abc abc abc", "abc", "abc", "abc"));
    }

    @Test
    @DisplayName("Null source throws NullPointerException")
    void testNullSource() {
        assertThrows(NullPointerException.class, () -> TestUtil.checkContainsIgnoreCase(null, "anything"));
    }

    @Test
    @DisplayName("Null element in var‑args throws NullPointerException")
    void testNullElementInVarArgs() {
        assertThrows(NullPointerException.class, () -> TestUtil.checkContainsIgnoreCase("some source", (String) null));
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("basicFunctionalityProvider")
    void basicFunctionality(String source, String[] contains, boolean expected, String description) {
        boolean result = TestUtil.checkContainsIgnoreCase(source, contains);
        assertEquals(expected, result, description);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("emptyStringsProvider")
    void emptyStrings(String source, String[] contains, boolean expected, String description) {
        boolean result = TestUtil.checkContainsIgnoreCase(source, contains);
        assertEquals(expected, result, description);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("errorHandlingProvider")
    void errorHandling(String source, String[] contains, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> TestUtil.checkContainsIgnoreCase(source, contains), description);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("emptySubstringProvider")
    void emptySubstringHandling(String source, String[] contains, boolean expected, String description) {
        boolean result = TestUtil.checkContainsIgnoreCase(source, contains);
        assertEquals(expected, result, description);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("repeatedSubstringsProvider")
    void repeatedSubstrings(String source, String[] contains, boolean expected, String description) {
        boolean result = TestUtil.checkContainsIgnoreCase(source, contains);
        assertEquals(expected, result, description);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("overlappingSubstringsProvider")
    void overlappingSubstrings(String source, String[] contains, boolean expected, String description) {
        boolean result = TestUtil.checkContainsIgnoreCase(source, contains);
        assertEquals(expected, result, description);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("whitespaceHandlingProvider")
    void whitespaceHandling(String source, String[] contains, boolean expected, String description) {
        boolean result = TestUtil.checkContainsIgnoreCase(source, contains);
        assertEquals(expected, result, description);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("emptySourceMixedProvider")
    void emptySourceMixed(String source, String[] contains, boolean expected, String description) {
        boolean result = TestUtil.checkContainsIgnoreCase(source, contains);
        assertEquals(expected, result, description);
    }
}
