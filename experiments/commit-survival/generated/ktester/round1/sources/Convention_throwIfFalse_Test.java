package com.cedarsoftware.util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class Convention_throwIfFalse_Test {

    /**
     * Supplies the arguments for {@link #testThrowIfFalse_variousScenarios}.
     *
     * The data mirrors the JSON test cases:
     * <ul>
     *   <li>logic = true  → no exception (message ignored, even if null)</li>
     *   <li>logic = false → {@link IllegalArgumentException} with the supplied message
     *       (which may be {@code null})</li>
     * </ul>
     *
     * @return a stream of {@link Arguments}
     */
    private static Stream<Arguments> provideThrowIfFalseCases() {
        return Stream.of(// logic true, non‑null message → no exception
        Arguments.of(true, "any message", false, null), // logic false, non‑null message → exception with that message
        Arguments.of(false, "invalid argument", true, "invalid argument"), // logic false, null message → exception with null message
        Arguments.of(false, null, true, null), // logic true, null message → no exception
        Arguments.of(true, null, false, null));
    }

    @Test
    void testThrowIfFalse_whenFalse() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfFalse(false, "foo")).withMessageContaining("foo");
    }

    @Test
    void testThrowIfFalse_whenTrue() {
        assertThatNoException().isThrownBy(() -> Convention.throwIfFalse(true, "foo"));
    }

    /**
     * Validates that {@code Convention.throwIfFalse} behaves correctly for
     * every combination of {@code logic} and {@code message} described in the
     * JSON test cases.
     *
     * @param logic            the boolean condition supplied to the method
     * @param message          the message supplied to the method (may be {@code null})
     * @param expectException  {@code true} if an {@link IllegalArgumentException}
     *                         is expected, {@code false} otherwise
     * @param expectedMessage  the exact message the exception should carry;
     *                         may be {@code null}
     */
    @ParameterizedTest(name = "[{index}] logic={0}, message={1}, expectException={2}")
    @MethodSource("provideThrowIfFalseCases")
    void testThrowIfFalse_variousScenarios(boolean logic, String message, boolean expectException, String expectedMessage) {
        if (expectException) {
            assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfFalse(logic, message)).satisfies(ex -> {
                // When the expected message is null we assert that the
                // exception's message is also null; otherwise we check
                // containment (matches the original contract).
                if (expectedMessage == null) {
                    assertThat(ex).hasMessage(null);
                } else {
                    assertThat(ex).hasMessageContaining(expectedMessage);
                }
            });
        } else {
            assertThatNoException().isThrownBy(() -> Convention.throwIfFalse(logic, message));
        }
    }
}
