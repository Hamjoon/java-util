package com.cedarsoftware.util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests for {@link Convention#throwIfNullOrEmpty(String, String)}.
 *
 * The class Convention_throwIfNullOrEmpty_Test{@code ConventionTest}
 * but isolates the tests for the focal method. No external resources or
 * collaborators are required, therefore the setup/teardown methods are
 * intentionally left empty.
 */
@ExtendWith(MockitoExtension.class)
class Convention_throwIfNullOrEmpty_Test {

    @BeforeAll
    static void setupBeforeAll() {
        // No global fixtures required for this static utility method.
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test fixtures required.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /**
     * Verifies that an {@link IllegalArgumentException} is thrown when the
     * supplied value is {@code null}.
     */
    @Test
    void throwIfNullOrEmpty_whenValueIsNull_throwsException() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Convention.throwIfNullOrEmpty(null, "value must be supplied"))
                .withMessageContaining("value must be supplied");
    }

    /**
     * Verifies that an {@link IllegalArgumentException} is thrown when the
     * supplied value is an empty {@code String}.
     */
    @ParameterizedTest
    @NullAndEmptySource
    void throwIfNullOrEmpty_whenValueIsNullOrEmpty_throwsException(String value) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Convention.throwIfNullOrEmpty(value, "value cannot be empty"))
                .withMessageContaining("value cannot be empty");
    }

    /**
     * Confirms that no exception is thrown when a non‑null, non‑empty string is
     * provided.
     */
    @Test
    void throwIfNullOrEmpty_whenValueIsValid_doesNotThrow() {
        assertThatNoException()
                .isThrownBy(() -> Convention.throwIfNullOrEmpty("valid", "should not matter"));
    }

    /**
     * Additional sanity check: the method should also reject strings that contain
     * only whitespace if {@link StringUtilities#isEmpty} treats them as empty.
     * (This depends on the implementation of {@code StringUtilities.isEmpty} – if
     * it does not treat whitespace as empty, the test will simply pass without
     * throwing.)
     */
    @Test
    void throwIfNullOrEmpty_whenValueIsWhitespace_throwsExceptionIfConsideredEmpty() {
        // The expected behaviour is defined by StringUtilities.isEmpty.
        // We assert the same contract: an exception should be raised if the
        // utility deems the string empty.
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Convention.throwIfNullOrEmpty("   ", "whitespace not allowed"));
    }
}