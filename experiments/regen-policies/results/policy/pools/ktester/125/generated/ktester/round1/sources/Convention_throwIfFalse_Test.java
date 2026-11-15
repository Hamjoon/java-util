package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Convention_throwIfFalse_Test {

    /**
     * Cases where {@code logic == true} – no exception expected.
     */
    private static Stream<Arguments> trueCases() {
        // >1000 characters
        String longMessage = "L".repeat(1500);
        return Stream.of(Arguments.of("any message"), Arguments.of(null), Arguments.of(""), Arguments.of("   "), Arguments.of(longMessage));
    }

    /**
     * Cases where {@code logic == false} – IllegalArgumentException expected.
     */
    private static Stream<Arguments> falseCases() {
        // >1000 characters
        String longMessage = "L".repeat(1500);
        String controlCharMessage = "Control\u0000Char";
        return Stream.of(// simple messages
        Arguments.of("validation failed"), Arguments.of(""), Arguments.of(null), Arguments.of("   "), // long message
        Arguments.of(longMessage), // special / Unicode characters
        Arguments.of("✓✔✕"), Arguments.of("😀🚀🌟"), Arguments.of("Line1\nLine2"), Arguments.of("\tTabbed message"), Arguments.of(controlCharMessage));
    }

    @Test
    void testThrowIfFalse_whenFalse() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfFalse(false, "condition failed")).withMessageContaining("condition failed");
    }

    @Test
    void testThrowIfFalse_whenTrue() {
        assertThatNoException().isThrownBy(() -> Convention.throwIfFalse(true, "should not be used"));
    }

    /**
     * Verify that no exception is thrown when the logic argument is {@code true},
     * regardless of the supplied message (including {@code null}, empty, whitespace,
     * or a very long string).
     */
    @ParameterizedTest(name = "[{index}] message=''{0}''")
    @MethodSource("trueCases")
    void testThrowIfFalse_NoExceptionWhenLogicTrue(String message) {
        assertThatNoException().isThrownBy(() -> Convention.throwIfFalse(true, message));
    }

    /**
     * Verify that an {@link IllegalArgumentException} is thrown when the logic
     * argument is {@code false} and that the exception message exactly matches the
     * supplied message (including {@code null}, empty, whitespace, long strings,
     * Unicode symbols, control characters, etc.).
     */
    @ParameterizedTest(name = "[{index}] message=''{0}''")
    @MethodSource("falseCases")
    void testThrowIfFalse_ExceptionWhenLogicFalse(String message) {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfFalse(false, message)).satisfies(e -> assertThat(e.getMessage()).isEqualTo(message));
    }
}
