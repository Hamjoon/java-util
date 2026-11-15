package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.Collections;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Convention_throwIfNullOrEmpty_Test {

    private static Stream<Arguments> validAndInvalidInputs() {
        return Stream.of(// non‑empty string → no exception
        Arguments.of("nonEmptyString", "should not throw", false, null), // empty string → exception with supplied message
        Arguments.of("", "empty value", true, "empty value"), // null value → exception with supplied message
        Arguments.of(null, "null value", true, "null value"), // whitespace‑only string is NOT considered empty by StringUtilities.isEmpty
        Arguments.of("   ", "whitespace only", false, null));
    }

    private static Stream<Arguments> messageEdgeCases() {
        return Stream.of(// null value & null message → exception with null message
        Arguments.of(null, null, true, null), // empty value & empty message → exception with empty message
        Arguments.of("", "", true, ""), // empty value & whitespace‑only message → exception preserving whitespace
        Arguments.of("", "   ", true, "   "));
    }

    private static Stream<Arguments> valueEdgeCases() {
        return Stream.of(// single character
        Arguments.of("a", "single char", false), // newline character (length 1)
        Arguments.of("\n", "newline only", false), // zero‑width space (still a character)
        Arguments.of("\u200B", "zero‑width space", false), // surrogate pair representing an emoji (length 2)
        Arguments.of("\uD83D\uDE00", "emoji", false));
    }

    private static Stream<Arguments> largeInputCases() {
        // 10 000 “x” characters – non‑empty
        String tenThousandX = String.join("", Collections.nCopies(10_000, "x"));
        // Empty string generated via nCopies(0,…)
        String emptyGenerated = String.join("", Collections.nCopies(0, "x"));
        return Stream.of(Arguments.of(tenThousandX, "large string", false, null), Arguments.of(emptyGenerated, "empty after generation", true, "empty after generation"));
    }

    /**
     * Verifies that an {@link IllegalArgumentException} is thrown when the
     * supplied value is {@code null} or an empty string. The exception message
     * must contain the supplied message argument.
     */
    @ParameterizedTest
    @NullAndEmptySource
    void throwsWhenValueIsNullOrEmpty(String value) {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfNullOrEmpty(value, "must not be null/empty")).withMessageContaining("must not be null/empty");
    }

    /**
     * Verifies that no exception is thrown when the supplied value is a
     * non‑null, non‑empty string.
     */
    @Test
    void doesNotThrowWhenValueIsNonEmpty() {
        assertThatNoException().isThrownBy(() -> Convention.throwIfNullOrEmpty("valid", "should not matter"));
    }

    /**
     * Ensures that the method forwards the exact message argument to the
     * thrown exception when the value is invalid.
     */
    @Test
    void exceptionMessageIsExactlyTheProvidedOne() {
        String customMessage = "custom error message";
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfNullOrEmpty("", customMessage)).hasMessage(customMessage);
    }

    /**
     * Covers the “throwIfNullOrEmpty - valid and invalid inputs” group.
     */
    @ParameterizedTest(name = "[{index}] value={0}, message={1}, expectsException={2}")
    @MethodSource("validAndInvalidInputs")
    void testValidAndInvalidInputs(String value, String message, boolean expectsException, String expectedMessage) {
        if (expectsException) {
            assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfNullOrEmpty(value, message)).hasMessage(expectedMessage);
        } else {
            assertThatNoException().isThrownBy(() -> Convention.throwIfNullOrEmpty(value, message));
        }
    }

    /**
     * Covers the “throwIfNullOrEmpty - message edge cases” group.
     */
    @ParameterizedTest(name = "[{index}] value={0}, message={1}")
    @MethodSource("messageEdgeCases")
    void testMessageEdgeCases(String value, String message, boolean expectsException, String expectedMessage) {
        if (expectsException) {
            assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfNullOrEmpty(value, message)).hasMessage(expectedMessage);
        } else {
            assertThatNoException().isThrownBy(() -> Convention.throwIfNullOrEmpty(value, message));
        }
    }

    /**
     * Covers the “throwIfNullOrEmpty - value edge cases” group.
     */
    @ParameterizedTest(name = "[{index}] value={0} (len={1}), message={2}")
    @MethodSource("valueEdgeCases")
    void testValueEdgeCases(String value, String message, boolean expectsException) {
        if (expectsException) {
            assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfNullOrEmpty(value, message));
        } else {
            assertThatNoException().isThrownBy(() -> Convention.throwIfNullOrEmpty(value, message));
        }
    }

    /**
     * Covers the “throwIfNullOrEmpty - large input” group.
     */
    @ParameterizedTest(name = "[{index}] generatedLength={0}, expectsException={1}")
    @MethodSource("largeInputCases")
    void testLargeInputCases(String value, String message, boolean expectsException, String expectedMessage) {
        if (expectsException) {
            assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfNullOrEmpty(value, message)).hasMessage(expectedMessage);
        } else {
            assertThatNoException().isThrownBy(() -> Convention.throwIfNullOrEmpty(value, message));
        }
    }
}
