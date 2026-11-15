package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.EmptySource;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Convention_throwIfNullOrEmpty_Test {

    /**
     * Supplies the test data for {@link #throwIfNullOrEmpty_variousScenarios(String, String, boolean)}.
     *
     * @return a stream of arguments: value, message, expectException
     */
    private static Stream<Arguments> provideThrowIfNullOrEmptyScenarios() {
        return Stream.of(// Case: null value – should throw
        Arguments.of(null, "Value must not be null or empty", true), // Case: empty string – should throw
        Arguments.of("", "Empty string not allowed", true), // Case: whitespace only – should NOT throw (StringUtilities.isEmpty treats it as non‑empty)
        Arguments.of("   ", "Whitespace only should not trigger", false), // Case: regular non‑empty string – should NOT throw
        Arguments.of("valid", "Should not be thrown", false));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void throwIfNullOrEmpty_shouldThrowWhenValueIsNullOrEmpty(String value) {
        // given
        String message = "value must not be null or empty";
        // when / then
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfNullOrEmpty(value, message)).withMessageContaining(message);
    }

    @Test
    void throwIfNullOrEmpty_shouldNotThrowWhenValueIsNonEmpty() {
        // given
        String value = "non‑empty string";
        String message = "should never be used";
        // when / then
        assertThatNoException().isThrownBy(() -> Convention.throwIfNullOrEmpty(value, message));
    }

    /**
     * Parameterized test that drives all scenarios described in the JSON test
     * specification:
     * <ul>
     *   <li>null → exception</li>
     *   <li>empty string → exception</li>
     *   <li>whitespace‑only string → no exception</li>
     *   <li>valid non‑empty string → no exception</li>
     * </ul>
     *
     * @param value          the string to validate
     * @param message        the message that should appear in the exception (if any)
     * @param expectException true if an IllegalArgumentException is expected
     */
    @ParameterizedTest
    @MethodSource("provideThrowIfNullOrEmptyScenarios")
    void throwIfNullOrEmpty_variousScenarios(String value, String message, boolean expectException) {
        if (expectException) {
            // The method must throw IllegalArgumentException with the supplied message
            assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfNullOrEmpty(value, message)).withMessageContaining(message);
        } else {
            // No exception should be thrown for valid inputs
            assertThatNoException().isThrownBy(() -> Convention.throwIfNullOrEmpty(value, message));
        }
    }
}
