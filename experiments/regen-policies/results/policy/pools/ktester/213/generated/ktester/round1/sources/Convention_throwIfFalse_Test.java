package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.assertj.core.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class Convention_throwIfFalse_Test {

    /**
     * Supplies all test cases for {@link #testThrowIfFalse(boolean, String, boolean, String)}.
     *
     * @return a stream of arguments representing each scenario.
     */
    static Stream<Arguments> provideThrowIfFalseCases() {
        return Stream.of(// true → no exception, message ignored
        arguments(true, "any message", false, null), // false → exception with provided message
        arguments(false, "Condition failed", true, "Condition failed"), // false with empty string message
        arguments(false, "", true, ""), // false with null message
        arguments(false, null, true, null), // true with null message (still no exception)
        arguments(true, null, false, null), // true with empty string message (still no exception)
        arguments(true, "", false, null), // true with special/unicode characters in the message (no exception)
        arguments(true, "特殊字符!@#\n\t", false, null), // false with a long message
        arguments(false, "A very long message ", true, "A very long message "), // false with Unicode emoji characters
        arguments(false, "Unicode 🚀🔥💧", true, "Unicode 🚀🔥💧"));
    }

    @Test
    void testThrowIfFalse_whenLogicIsFalse_throwsExceptionWithMessage() {
        // given
        boolean logic = false;
        String message = "validation failed";
        // then
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfFalse(logic, message)).withMessageContaining(message);
    }

    @Test
    void testThrowIfFalse_whenLogicIsTrue_doesNotThrow() {
        // given
        boolean logic = true;
        String message = "should never be used";
        // then
        assertThatNoException().isThrownBy(() -> Convention.throwIfFalse(logic, message));
    }

    @Test
    void testThrowIfFalse_whenLogicIsFalse_andMessageIsNull_throwsExceptionWithNullMessage() {
        // given
        boolean logic = false;
        String message = null;
        // then
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfFalse(logic, message)).hasMessage(null);
    }

    /**
     * Parameterized test that covers all scenarios described in the JSON test cases.
     *
     * @param logic        the boolean condition passed to {@code throwIfFalse}
     * @param message      the message to be used if an exception is thrown
     * @param shouldThrow  {@code true} if an {@link IllegalArgumentException} is expected
     * @param expectedMsg  the exact message expected on the exception (may be {@code null})
     */
    @ParameterizedTest(name = "[{index}] logic={0}, message={1}, shouldThrow={2}")
    @MethodSource("provideThrowIfFalseCases")
    void testThrowIfFalse(boolean logic, String message, boolean shouldThrow, String expectedMsg) {
        if (shouldThrow) {
            // Expect an IllegalArgumentException with the supplied (or null) message.
            assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfFalse(logic, message)).satisfies(e -> {
                if (expectedMsg == null) {
                    assertThat(e).hasMessage(null);
                } else {
                    assertThat(e).hasMessage(expectedMsg);
                }
            });
        } else {
            // No exception should be thrown when the logic is true, regardless of the message.
            assertThatNoException().isThrownBy(() -> Convention.throwIfFalse(logic, message));
        }
    }
}
