package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TestUtil_assertContainsIgnoreCase_Test {

    // ---------- Successful assertions ----------
    private static Stream<Arguments> successfulAssertionsProvider() {
        return Stream.of(Arguments.of("The quick brown fox jumps over the lazy dog", new String[] { "quick", "FOX", "LAZY" }), Arguments.of("ABCabcABC", new String[] { "abc", "ABC" }), Arguments.of("HelloWorld", // empty var‑args
        new String[] {}), Arguments.of("   leading and trailing   ", new String[] { "LEADING", "TRAILING" }), // overlapping substrings case
        Arguments.of("ababa", new String[] { "aba", "ba" }));
    }

    // ---------- Assertion failures ----------
    private static Stream<Arguments> assertionFailureProvider() {
        return Stream.of(// missing substring
        Arguments.of("abc", new String[] { "d" }), // wrong order: 'c' found, then 'a' appears before it in original source
        Arguments.of("abc", new String[] { "c", "a" }));
    }

    // ---------- Null and empty handling ----------
    private static Stream<Arguments> nullHandlingProvider() {
        return Stream.of(// source is null
        Arguments.of(null, new String[] { "a" }, NullPointerException.class), // var‑args array itself is null
        Arguments.of("abc", null, NullPointerException.class), // element inside var‑args is null
        Arguments.of("abc", new String[] { "a", null }, NullPointerException.class));
    }

    // ---------- Empty source / empty substrings ----------
    private static Stream<Arguments> emptySourceProvider() {
        return Stream.of(// both empty, no substrings
        Arguments.of("", new String[] {}, false), // empty source, empty substring (always found)
        Arguments.of("", new String[] { "" }, false), // empty source, non‑empty substring -> AssertionError
        Arguments.of("", new String[] { "a" }, true), // non‑empty source, empty substring -> succeeds
        Arguments.of("abc", new String[] { "" }, false));
    }

    @Test
    void shouldPassWhenAllSubstringsAreFoundInOrder() {
        // given
        String source = "This is the source string to test.";
        // when / then – no exception expected
        assertDoesNotThrow(() -> TestUtil.assertContainsIgnoreCase(source, "Source", "string", "Test"));
    }

    @Test
    void shouldFailWhenARequiredSubstringIsMissing() {
        // given
        String source = "This is the source string to test.";
        // when
        AssertionError error = assertThrows(AssertionError.class, () -> TestUtil.assertContainsIgnoreCase(source, "Source", "string", // "Text" does not exist
        "Text"));
        // then – verify the error message contains the expected fragments (case‑insensitive)
        TestUtil.assertContainsIgnoreCase(error.getMessage(), "not found", "string", "test");
    }

    @Test
    void shouldFailWhenSubstringsAreOutOfOrder() {
        // given
        String source = "This is the source string to test.";
        // when
        AssertionError error = assertThrows(AssertionError.class, () -> TestUtil.assertContainsIgnoreCase(source, "Test", "Source", // order is wrong
        "string"));
        // then – the message should mention the first missing piece ("source")
        TestUtil.assertContainsIgnoreCase(error.getMessage(), "source", "not found", "test");
    }

    @Test
    void shouldPassWhenContainsArrayIsEmpty() {
        // given
        String source = "Anything at all";
        // when / then – an empty var‑args list means nothing to check, so it must succeed
        assertDoesNotThrow(() -> TestUtil.assertContainsIgnoreCase(source));
    }

    @Test
    void shouldHandleSingleCharacterSubstrings() {
        // given
        String source = "ABCdefGHI";
        // when / then – each character appears in order, ignoring case
        assertDoesNotThrow(() -> TestUtil.assertContainsIgnoreCase(source, "a", "d", "g"));
    }

    @ParameterizedTest(name = "Successful case #{index}: source=''{0}'', contains={1}")
    @MethodSource("successfulAssertionsProvider")
    void shouldPassForSuccessfulAssertions(String source, String[] contains) {
        assertDoesNotThrow(() -> TestUtil.assertContainsIgnoreCase(source, contains));
    }

    @ParameterizedTest(name = "Failure case #{index}: source=''{0}'', contains={1}")
    @MethodSource("assertionFailureProvider")
    void shouldThrowAssertionErrorWhenExpectationNotMet(String source, String[] contains) {
        assertThrows(AssertionError.class, () -> TestUtil.assertContainsIgnoreCase(source, contains));
    }

    @ParameterizedTest(name = "Null handling case #{index}: source=''{0}'', contains={1}")
    @MethodSource("nullHandlingProvider")
    void shouldThrowNullPointerExceptionForNullInputs(String source, String[] contains, Class<? extends Throwable> expected) {
        assertThrows(expected, () -> TestUtil.assertContainsIgnoreCase(source, contains));
    }

    @ParameterizedTest(name = "Empty source case #{index}: source=''{0}'', contains={1}")
    @MethodSource("emptySourceProvider")
    void shouldHandleEmptySourceAndSubstrings(String source, String[] contains, boolean expectFailure) {
        if (expectFailure) {
            assertThrows(AssertionError.class, () -> TestUtil.assertContainsIgnoreCase(source, contains));
        } else {
            assertDoesNotThrow(() -> TestUtil.assertContainsIgnoreCase(source, contains));
        }
    }

    /**
     * This test demonstrates the behaviour when Java assertions are disabled.
     * With assertions disabled the {@code assert} statement is ignored, the
     * method proceeds to {@code lowerSource.substring(idx)} where {@code idx}
     * is {@code -1}, causing a {@link StringIndexOutOfBoundsException}.
     *
     * The test is marked {@code @Disabled} by default because the default
     * JVM used by most build tools runs with assertions enabled.  It can be
     * enabled manually (e.g. by launching the JVM with {@code -da}) to
     * observe the expected exception.
     */
    @Test
    @Disabled("Run with assertions disabled (-da) to verify this behaviour")
    void shouldThrowStringIndexOutOfBoundsWhenAssertionsDisabled() {
        String source = "abc";
        // The contains array has a missing token, which would normally trigger
        // an AssertionError when assertions are enabled.
        assertThrows(StringIndexOutOfBoundsException.class, () -> TestUtil.assertContainsIgnoreCase(source, new String[] { "d" }));
    }
}
