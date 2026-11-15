package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.ArrayList;
import java.util.Date;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Convention_throwIfNull_Test {

    /**
     * Supplies arguments for {@link #throwIfNull_primaryGroup}.
     */
    private static Stream<Arguments> throwIfNullPrimaryCases() {
        return Stream.of(// non‑null String
        Arguments.of("test", "Value must not be null", false, null), // non‑null Integer
        Arguments.of(123, "Number cannot be null", false, null), // non‑null generic Object
        Arguments.of(new Object(), "Object must be present", false, null), // null value → exception with provided message
        Arguments.of(null, "Parameter cannot be null", true, "Parameter cannot be null"), // null value + empty message
        Arguments.of(null, "", true, ""), // non‑null empty String (should NOT throw)
        Arguments.of("", "Should not throw for empty string", false, null));
    }

    /**
     * Supplies arguments for {@link #throwIfNull_additionalGroup}.
     */
    private static Stream<Arguments> throwIfNullAdditionalCases() {
        return Stream.of(// null value, null message → exception with null message
        Arguments.of(null, null, true, null), // null value, whitespace/special chars message
        Arguments.of(null, "   !@#$%^&*()   ", true, "   !@#$%^&*()   "), // non‑null primitive int array
        Arguments.of(new int[] { 1, 2, 3 }, "Array must not be null", false, null), // non‑null empty int array
        Arguments.of(new int[0], "Empty array should not trigger", false, null), // non‑null ArrayList instance
        Arguments.of(new ArrayList<>(), "List cannot be null", false, null), // non‑null Boolean wrapper
        Arguments.of(Boolean.TRUE, "Boolean cannot be null", false, null), // non‑null lambda (Runnable)
        Arguments.of((Runnable) () -> {
        }, "Lambda must not be null", false, null), // non‑null java.util.Date instance
        Arguments.of(new Date(), "Date cannot be null", false, null));
    }

    @Test
    void throwIfNull_givenNullValue_throwsIllegalArgumentException() {
        // GIVEN
        Object value = null;
        String message = "value must not be null";
        // WHEN / THEN
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfNull(value, message)).withMessageContaining(message);
    }

    @Test
    void throwIfNull_givenNonNullValue_doesNotThrow() {
        // GIVEN
        Object value = new Object();
        String message = "should never be used";
        // WHEN / THEN
        assertThatNoException().isThrownBy(() -> Convention.throwIfNull(value, message));
    }

    @Test
    void resolveTypeUsingInstance_example_nullTarget_throws() {
        // This mirrors the example in the documentation:
        // Convention.throwIfNull(target, "target cannot be null");
        Object target = null;
        String msg = "target cannot be null";
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfNull(target, msg)).withMessageContaining(msg);
    }

    @Test
    void getPrimitiveFromWrapper_example_nullToType_throws() {
        // Mirrors: Convention.throwIfNull(toType, "toType cannot be null");
        Class<?> toType = null;
        String msg = "toType cannot be null";
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfNull(toType, msg)).withMessageContaining(msg);
    }

    @Test
    void newInstance_example_nullClass_throws() {
        // Mirrors: Convention.throwIfNull(c, "Class cannot be null");
        Class<?> clazz = null;
        String msg = "Class cannot be null";
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfNull(clazz, msg)).withMessageContaining(msg);
    }

    /**
     * Parameterized test for the first group: "Convention.throwIfNull tests".
     *
     * @param value          the object to pass to {@code throwIfNull}
     * @param message        the message to pass to {@code throwIfNull}
     * @param expectException true if an IllegalArgumentException is expected
     * @param expectedMessage the exact message expected (may be null)
     */
    @ParameterizedTest(name = "[{index}] value={0}, message={1}")
    @MethodSource("throwIfNullPrimaryCases")
    void throwIfNull_primaryGroup(Object value, String message, boolean expectException, String expectedMessage) {
        if (expectException) {
            // Expect an IllegalArgumentException with the exact message (including null)
            assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfNull(value, message)).satisfies(ex -> assertThat(ex.getMessage()).isEqualTo(expectedMessage));
        } else {
            // No exception should be thrown
            assertThatNoException().isThrownBy(() -> Convention.throwIfNull(value, message));
        }
    }

    /**
     * Parameterized test for the second group: "Convention.throwIfNull additional tests".
     *
     * @param value          the object to pass to {@code throwIfNull}
     * @param message        the message to pass to {@code throwIfNull}
     * @param expectException true if an IllegalArgumentException is expected
     * @param expectedMessage the exact message expected (may be null)
     */
    @ParameterizedTest(name = "[{index}] value={0}, message={1}")
    @MethodSource("throwIfNullAdditionalCases")
    void throwIfNull_additionalGroup(Object value, String message, boolean expectException, String expectedMessage) {
        if (expectException) {
            assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfNull(value, message)).satisfies(ex -> assertThat(ex.getMessage()).isEqualTo(expectedMessage));
        } else {
            assertThatNoException().isThrownBy(() -> Convention.throwIfNull(value, message));
        }
    }
}
