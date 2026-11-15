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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class Convention_throwIfNull_Test {

    /**
     * Supplies all test cases described in the JSON input.
     *
     * @return a stream of arguments for {@link #testThrowIfNull(Object, String, boolean, String)}
     */
    private static Stream<Arguments> throwIfNullCases() {
        // Helper objects used in several cases
        Object newObject = new Object();
        int[] intArray = new int[] { 1, 2, 3 };
        List<String> stringList = Arrays.asList("a", "b");
        return Stream.of(// 1. value = null, message = "Value cannot be null"
        Arguments.of(null, "Value cannot be null", true, "Value cannot be null"), // 2. non‑null String, message ignored
        Arguments.of("some non‑null string", "This message should be ignored", false, null), // 3. non‑null Integer, empty message
        Arguments.of(12345, "", false, null), // 4. null value, empty message
        Arguments.of(null, "", true, ""), // 5. null value, null message
        Arguments.of(null, null, true, null), // 6. empty String (non‑null), any message
        Arguments.of("", "any message", false, null), // 7. Boolean false (autoboxed), null message
        Arguments.of(Boolean.FALSE, null, false, null), // 8. freshly created Object instance, irrelevant message
        Arguments.of(newObject, "irrelevant", false, null), // 9. array reference (non‑null), message provided
        Arguments.of(intArray, "array test", false, null), // 10. List instance, message provided
        Arguments.of(stringList, "list test", false, null));
    }

    private static Stream<Arguments> nullValueCases() {
        return Stream.of(Arguments.of(null, "value must not be null"));
    }

    @Test
    void testThrowIfNull_whenValueIsNull_throwsException() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfNull(null, "value must not be null")).withMessageContaining("value must not be null");
    }

    @Test
    void testThrowIfNull_whenValueIsNotNull_doesNotThrow() {
        assertThatNoException().isThrownBy(() -> Convention.throwIfNull("some value", "value must not be null"));
    }

    /**
     * Parameterised test that validates both the exception‑throwing and the
     * non‑exceptional behaviour of {@code Convention.throwIfNull}.
     *
     * @param value            the object to be checked for {@code null}
     * @param message          the message supplied to the method (may be {@code null})
     * @param expectException  {@code true} if an {@link IllegalArgumentException}
     *                         is expected, {@code false} otherwise
     * @param expectedMessage  the exact message expected on the exception
     *                         (may be {@code null}); ignored when {@code expectException}
     *                         is {@code false}
     */
    @ParameterizedTest(name = "[{index}] value={0}, message={1}, expectException={2}")
    @MethodSource("throwIfNullCases")
    void testThrowIfNull(Object value, String message, boolean expectException, String expectedMessage) {
        if (expectException) {
            // When a null value is supplied the method must throw IllegalArgumentException
            // with the exact message (which can be null or empty).
            assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfNull(value, message)).satisfies(ex -> assertThat(ex.getMessage()).isEqualTo(expectedMessage));
        } else {
            // For any non‑null value the method must complete silently,
            // regardless of the message (including null or empty messages).
            assertThatNoException().isThrownBy(() -> Convention.throwIfNull(value, message));
        }
    }
}
