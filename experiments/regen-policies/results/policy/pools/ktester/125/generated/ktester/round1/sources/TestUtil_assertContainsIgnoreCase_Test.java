package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TestUtil_assertContainsIgnoreCase_Test {

    static Stream<Arguments> successfulCases() {
        return Stream.of(// All substrings appear in order, case‑insensitive
        Arguments.of("The quick brown Fox jumps over the lazy dog", new String[] { "quick", "FOX", "lazy" }), // Back‑to‑back substrings after trimming lowerSource
        Arguments.of("CaseInsensitive", new String[] { "case", "INSENSITIVE" }), // Empty source and empty var‑args
        Arguments.of("", new String[] {}), // Empty source with a single empty string in contains
        Arguments.of("", new String[] { "" }), // Repeated substring – appears twice
        Arguments.of("abcabc", new String[] { "abc", "abc" }), // Overlapping substrings – lowerSource is not advanced past previous match
        Arguments.of("aaaa", new String[] { "aa", "aa" }), // Empty strings inside the contains array – should be ignored
        Arguments.of("test", new String[] { "", "e", "" }), // Whitespace handling
        Arguments.of("   leading spaces", new String[] { "   ", "leading", "SPACES" }));
    }

    static Stream<Arguments> failedAssertions() {
        return Stream.of(// First fragment found, second missing
        Arguments.of("Hello World", new String[] { "world", "planet" }, "'planet' not found in 'world'"), // Order‑sensitive failure – after first match source is trimmed
        Arguments.of("JavaTesting", new String[] { "testing", "java" }, "'java' not found in 'testing'"), // Empty source cannot contain non‑empty substring
        Arguments.of("", new String[] { "x" }, "'x' not found in ''"), // Empty string matches, then a non‑empty substring fails
        Arguments.of("", new String[] { "", "a" }, "'a' not found in ''"), // Source shorter than required substring
        Arguments.of("ab", new String[] { "abc" }, "'abc' not found in 'ab'"));
    }

    static Stream<Arguments> nullArgumentCases() {
        return Stream.of(// Null source with non‑null contains
        Arguments.of(null, new String[] { "any" }), // Non‑null source with a null element inside contains
        Arguments.of("NonNull", new String[] { null }), // Null var‑args array
        Arguments.of("NonNull", null), // Null source with empty contains – still NPE because source is null
        Arguments.of(null, new String[] {}), // Empty source with a null element in contains
        Arguments.of("", new String[] { null }));
    }

    @Test
    @DisplayName("assertContainsIgnoreCase succeeds when all fragments are present in order")
    void testAssertContainsIgnoreCaseSuccess() {
        // The method should complete without throwing an AssertionError.
        assertDoesNotThrow(() -> TestUtil.assertContainsIgnoreCase("This is the source string to test.", "Source", "string", "Test"));
    }

    @Test
    @DisplayName("assertContainsIgnoreCase throws AssertionError with proper message when a fragment is missing")
    void testAssertContainsIgnoreCaseMissingFragment() {
        AssertionError error = assertThrows(AssertionError.class, () -> TestUtil.assertContainsIgnoreCase("This is the source string to test.", "Source", "string", // "Text" is not present
        "Text"));
        // Verify that the generated message contains the expected fragments.
        // The message format is: '<missing>' not found in '<lower‑cased source>'
        TestUtil.assertContainsIgnoreCase(error.getMessage(), "not found", "text", "this is the source string to test.");
    }

    @Test
    @DisplayName("assertContainsIgnoreCase fails when fragments are present but out of order")
    void testAssertContainsIgnoreCaseOutOfOrder() {
        AssertionError error = assertThrows(AssertionError.class, () -> TestUtil.assertContainsIgnoreCase("This is the source string to test.", "Test", "Source", // order is wrong
        "string"));
        // The failure message should mention the first out‑of‑order fragment.
        TestUtil.assertContainsIgnoreCase(error.getMessage(), "source", "not found", "test");
    }

    @Test
    @DisplayName("assertContainsIgnoreCase works with single‑character fragments")
    void testAssertContainsIgnoreCaseSingleChars() {
        assertDoesNotThrow(() -> TestUtil.assertContainsIgnoreCase("abcde", "A", "c", "E"));
    }

    @Test
    @DisplayName("assertContainsIgnoreCase works with empty var‑args (no fragments to check)")
    void testAssertContainsIgnoreCaseNoFragments() {
        // No fragments means there is nothing to assert; should pass trivially.
        assertDoesNotThrow(() -> TestUtil.assertContainsIgnoreCase("any source string"));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] source=''{0}'' contains={1}")
    @MethodSource("successfulCases")
    @DisplayName("Successful assertions – method should not throw")
    void testSuccessfulAssertions(String source, String[] contains) {
        assertDoesNotThrow(() -> TestUtil.assertContainsIgnoreCase(source, contains));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] source=''{0}'' contains={1}")
    @MethodSource("failedAssertions")
    @DisplayName("Failed assertions – method should throw AssertionError with exact message")
    void testFailedAssertions(String source, String[] contains, String expectedMessage) {
        AssertionError error = assertThrows(AssertionError.class, () -> TestUtil.assertContainsIgnoreCase(source, contains));
        assertEquals(expectedMessage, error.getMessage(), () -> "Unexpected assertion message for source='" + source + "'");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] source={0} contains={1}")
    @MethodSource("nullArgumentCases")
    @DisplayName("Null argument handling – should throw NullPointerException")
    void testNullArgumentHandling(String source, String[] contains) {
        assertThrows(NullPointerException.class, () -> TestUtil.assertContainsIgnoreCase(source, contains));
    }
}
