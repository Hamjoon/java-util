package com.cedarsoftware.util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Convention_throwIfNull_Test {

    private static Stream<Arguments> nullValueProvider() {
        return Stream.of(// Standard null‑value case with a regular message.
        Arguments.of(null, "Value must not be null", "Value must not be null"), // Custom null message – the exception message should be null.
        Arguments.of(null, null, null), // Empty string as message – the exception should have an empty message.
        Arguments.of(null, "", ""), // Additional custom message from the JSON spec.
        Arguments.of(null, "Custom null message", "Custom null message"));
    }

    private static Stream<Arguments> nonNullValueProvider() {
        return Stream.of(// Object instance.
        Arguments.of(new Object(), "Any message"), // Empty map (non‑null) with empty message.
        Arguments.of(java.util.Collections.emptyMap(), ""), // Integer zero (wrapper) – non‑null.
        Arguments.of(Integer.valueOf(0), "Zero is not null"), // Boolean true – non‑null.
        Arguments.of(Boolean.TRUE, "Boolean true is not null"), // Regular non‑null string.
        Arguments.of("non‑null string", "String should not trigger exception"), // Non‑null value with null message – still should not throw.
        Arguments.of("someValue", null), // Non‑null value with empty message – still should not throw.
        Arguments.of(123, ""));
    }

    @Test
    void throwIfNull_givenNull_throwsIllegalArgumentException() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfNull(null, "value cannot be null")).withMessageContaining("value cannot be null");
    }

    @Test
    void throwIfNull_givenNonNull_doesNotThrow() {
        assertThatNoException().isThrownBy(() -> Convention.throwIfNull("someValue", "value cannot be null"));
    }

    @ParameterizedTest
    @NullSource
    void throwIfNull_parameterized_givenNull_throwsException(Object value) {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfNull(value, "parameter is required")).withMessageContaining("parameter is required");
    }

    /**
     * All cases where the {@code value} argument is {@code null} and an
     * {@link IllegalArgumentException} is expected.  The expected message may be
     * a regular string, {@code null}, or an empty string.
     */
    @ParameterizedTest(name = "[{index}] value=null, message=''{1}'' → expect exception with message=''{2}''")
    @MethodSource("nullValueProvider")
    void throwIfNull_givenNull_throwsExpectedException(Object value, String message, String expectedMessage) {
        // The method under test should always throw IllegalArgumentException for null value.
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfNull(value, message)).satisfies(ex -> {
            // Assert the message exactly matches the expectation (including null/empty).
            if (expectedMessage == null) {
                // When the supplied message is null, the exception message should also be null.
                assert ex.getMessage() == null;
            } else {
                assert ex.getMessage().equals(expectedMessage);
            }
        });
    }
}
