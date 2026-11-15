package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TestUtil_assertContainsIgnoreCase_Test {

    private static Stream<Arguments> validCasesProvider() {
        return Stream.of(Arguments.of("Single substring present, case‑insensitive", "Hello World", new String[] { "hello" }), Arguments.of("Two substrings present in the correct order", "Hello World", new String[] { "hello", "world" }), Arguments.of("All substrings present with mixed case; method should ignore case", "AbcDefGhi", new String[] { "ABC", "def", "GHI" }), Arguments.of("Repeated substring; after the first match the source is trimmed to the same start, allowing the second match", "abcabc", new String[] { "abc", "abc" }), // Empty substring is considered found at index 0 – should not throw
        Arguments.of("Empty substring is considered found at index 0", "NonEmpty", new String[] { "" }));
    }

    private static Stream<Arguments> invalidCasesProvider() {
        return Stream.of(Arguments.of("Substrings are present but in reverse order; should fail on second token", "Hello World", new String[] { "world", "hello" }, "'hello' not found"), Arguments.of("Required substring does not exist; should throw AssertionError", "Sample Text", new String[] { "missing" }, "'missing' not found"), Arguments.of("First substring found, second appears before trimmed position; should fail", "CaseSensitive", new String[] { "sensitive", "case" }, "'case' not found"), Arguments.of("Empty source string cannot contain any non‑empty substring; should trigger an assertion", "", new String[] { "any" }, "'any' not found"));
    }

    /**
     * Positive scenario – all expected substrings are present (ignoring case)
     * and appear in the order supplied.
     */
    @Test
    void shouldPassWhenAllSubstringsExistInOrder() {
        // given
        String source = "This is the source string to test.";
        // when / then
        assertDoesNotThrow(() -> TestUtil.assertContainsIgnoreCase(source, "Source", "string", "Test"));
    }

    /**
     * Negative scenario – a substring is missing; the method should throw
     * an AssertionError with a message that contains the missing token and
     * the phrase “not found”.
     */
    @Test
    void shouldFailWhenSubstringIsMissing() {
        // given
        String source = "This is the source string to test.";
        AssertionError error = assertThrows(AssertionError.class, () -> TestUtil.assertContainsIgnoreCase(source, "Source", "string", "Text"));
        // then – verify the error message mentions the missing token and the
        // standard “not found” wording (case‑insensitive check)
        TestUtil.assertContainsIgnoreCase(error.getMessage(), "not found", "string", "test");
    }

    /**
     * Negative scenario – substrings are present but out of the expected order.
     * The method should fail on the first out‑of‑order token.
     */
    @Test
    void shouldFailWhenSubstringsOutOfOrder() {
        // given
        String source = "This is the source string to test.";
        AssertionError error = assertThrows(AssertionError.class, () -> TestUtil.assertContainsIgnoreCase(source, "Test", "Source", "string"));
        // then – the message should contain the word “source” (the token that
        // caused the failure) and the standard “not found” wording.
        TestUtil.assertContainsIgnoreCase(error.getMessage(), "source", "not found", "test");
    }

    /**
     * Edge case – empty source string with no expected substrings should pass.
     */
    @Test
    void shouldPassWhenNoSubstringsProvided() {
        assertDoesNotThrow(() -> TestUtil.assertContainsIgnoreCase("any text"));
    }

    /**
     * Edge case – null source should cause a NullPointerException before the
     * utility logic runs (as the method calls toLowerCase on the argument).
     */
    @Test
    void shouldThrowNPEWhenSourceIsNull() {
        assertThrows(NullPointerException.class, () -> TestUtil.assertContainsIgnoreCase(null, "anything"));
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validCasesProvider")
    void validCasesShouldPass(String description, String source, String[] contains) {
        assertDoesNotThrow(() -> TestUtil.assertContainsIgnoreCase(source, contains));
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidCasesProvider")
    void invalidCasesShouldThrowAssertionError(String description, String source, String[] contains, String expectedMessageFragment) {
        AssertionError error = assertThrows(AssertionError.class, () -> TestUtil.assertContainsIgnoreCase(source, contains));
        // Verify that the error message contains the expected fragment(s)
        // (case‑insensitive check using the utility itself)
        TestUtil.assertContainsIgnoreCase(error.getMessage(), expectedMessageFragment.toLowerCase().split("\\s+"));
    }

    // --------------------------------------------------------------------- //
    @Test
    void shouldThrowNPEWhenContainsArrayIsNull() {
        assertThrows(NullPointerException.class, () -> TestUtil.assertContainsIgnoreCase("any string", (String[]) null));
    }

    @Test
    void shouldThrowNPEWhenContainsArrayHasNullElement() {
        assertThrows(NullPointerException.class, () -> TestUtil.assertContainsIgnoreCase("abcde", "a", null, "e"));
    }

    // --------------------------------------------------------------------- //
    @Test
    void shouldThrowStringIndexOutOfBoundsWhenAssertionsDisabled() {
        // This test relies on the JVM running with assertions *disabled*.
        // In most build environments (including Maven Surefire) assertions are off,
        // so the Java `assert` statement is ignored, `indexOf` returns -1,
        // and the subsequent `substring(-1)` throws StringIndexOutOfBoundsException.
        assertThrows(StringIndexOutOfBoundsException.class, () -> TestUtil.assertContainsIgnoreCase("Hello", "missing"));
    }
}
