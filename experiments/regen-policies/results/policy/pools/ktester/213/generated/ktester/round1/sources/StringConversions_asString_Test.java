package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_asString_Test {

    /**
     * Object whose {@code toString()} returns a fixed non‑empty string.
     */
    private static class CustomToStringObj {

        @Override
        public String toString() {
            return "custom";
        }
    }

    /**
     * Object whose {@code toString()} returns an empty string.
     */
    private static class EmptyToStringObj {

        @Override
        public String toString() {
            return "";
        }
    }

    /**
     * Object whose {@code toString()} returns {@code null}.
     */
    private static class NullToStringObj {

        @Override
        public String toString() {
            return null;
        }
    }

    /**
     * Object whose {@code toString()} throws {@link IllegalStateException}.
     */
    private static class IllegalStateExceptionToStringObj {

        @Override
        public String toString() {
            throw new IllegalStateException("illegal state");
        }
    }

    /**
     * Object whose {@code toString()} throws {@link NullPointerException}.
     */
    private static class NullPointerExceptionToStringObj {

        @Override
        public String toString() {
            throw new NullPointerException("null pointer");
        }
    }

    /**
     * Custom unchecked exception used for testing propagation.
     */
    private static class CustomRuntimeException extends RuntimeException {

        CustomRuntimeException(String message) {
            super(message);
        }
    }

    /**
     * Object whose {@code toString()} throws a custom {@link RuntimeException}.
     */
    private static class CustomRuntimeExceptionToStringObj {

        @Override
        public String toString() {
            throw new CustomRuntimeException("custom runtime");
        }
    }

    /**
     * Object whose {@code toString()} throws an {@link AssertionError}.
     */
    private static class AssertionErrorToStringObj {

        @Override
        public String toString() {
            throw new AssertionError("assertion failed");
        }
    }

    /**
     * Object returning a Unicode string from {@code toString()}.
     */
    private static class UnicodeObj {

        @Override
        public String toString() {
            // Japanese greeting
            return "こんにちは";
        }
    }

    /**
     * Object that returns a very large string. The length is supplied via the
     * constructor to keep the test flexible.
     */
    private static class LargeStringObj {

        private final int length;

        LargeStringObj(int length) {
            this.length = length;
        }

        @Override
        public String toString() {
            return generate(length);
        }

        /**
         * Helper to generate a deterministic string of the given length.
         */
        static String generate(int length) {
            char[] chars = new char[length];
            Arrays.fill(chars, 'x');
            return new String(chars);
        }
    }

    private static Stream<Arguments> asStringNormalProvider() {
        return Stream.of(// null already covered by a dedicated test – we still include it for completeness
        Arguments.of(null, null), // Plain String
        Arguments.of("hello", "hello"), // Number
        Arguments.of(123, "123"), // Boolean
        Arguments.of(Boolean.TRUE, "true"), // Custom object with overridden toString() returning "custom"
        Arguments.of(new CustomToStringObj(), "custom"), // Empty string
        Arguments.of("", ""), // Object whose toString() returns empty string
        Arguments.of(new EmptyToStringObj(), ""), // Object whose toString() returns null (should propagate null)
        Arguments.of(new NullToStringObj(), null), // Enum constant – default toString() returns the name
        Arguments.of(SampleEnum.ALPHA, "ALPHA"), // java.util.ArrayList representation
        Arguments.of(new ArrayList<>(Arrays.asList(1, 2, 3)), "[1, 2, 3]"), // StringBuilder
        Arguments.of(new StringBuilder("builder"), "builder"), // StringBuffer
        Arguments.of(new StringBuffer("buffer"), "buffer"), // Unicode string
        Arguments.of(new UnicodeObj(), "こんにちは"), // Very large string (10 000 characters)
        Arguments.of(new LargeStringObj(10_000), LargeStringObj.generate(10_000)));
    }

    private static Stream<Arguments> asStringExceptionProvider() {
        return Stream.of(// toString() throws IllegalStateException
        Arguments.of(new IllegalStateExceptionToStringObj(), IllegalStateException.class), // toString() throws NullPointerException
        Arguments.of(new NullPointerExceptionToStringObj(), NullPointerException.class), // toString() throws a custom RuntimeException
        Arguments.of(new CustomRuntimeExceptionToStringObj(), CustomRuntimeException.class), // toString() throws an Error (AssertionError)
        Arguments.of(new AssertionErrorToStringObj(), AssertionError.class));
    }

    /**
     * Verify that a {@code null} argument returns {@code null}.
     */
    @Test
    void asString_returnsNullWhenInputIsNull() {
        assertNull(StringConversions.asString(null), "asString(null) should return null");
    }

    /**
     * Verify that a non‑null argument is converted by delegating to {@code Object#toString()}.
     */
    @Test
    void asString_returnsToStringResultForNonNullObject() {
        // Simple built‑in type
        Integer number = 42;
        assertEquals("42", StringConversions.asString(number));
        // String instance (its toString returns itself)
        String text = "hello world";
        assertEquals("hello world", StringConversions.asString(text));
        // Custom object with overridden toString()
        Object custom = new Object() {

            @Override
            public String toString() {
                return "customString";
            }
        };
        assertEquals("customString", StringConversions.asString(custom));
    }

    /**
     * Demonstrates that {@code asString} is used internally by other conversion methods.
     * This is not a direct test of {@code asString} itself but shows the expected behaviour
     * when it is invoked from {@code toCharBuffer} and {@code toByteArray}.
     */
    @Test
    void asString_isUsedByOtherConversions() {
        // toCharBuffer wraps the result of asString in a CharBuffer
        CharBuffer buffer = StringConversions.toCharBuffer(123, null);
        assertEquals("123", buffer.toString());
        // toByteArray obtains the string representation first
        byte[] bytes = StringConversions.toByteArray(3.14, null);
        assertArrayEquals("3.14".getBytes(), bytes);
    }

    /**
     * Normal (non‑exceptional) conversion scenarios.
     */
    @ParameterizedTest(name = "{index}: asString({0}) → \"{1}\"")
    @MethodSource("asStringNormalProvider")
    void asString_normalScenarios(Object input, String expected) {
        assertEquals(expected, StringConversions.asString(input));
    }

    /**
     * Scenarios where {@code toString()} throws an exception – the same
     * exception must be propagated by {@code asString}.
     */
    @ParameterizedTest(name = "{index}: asString({0}) throws {1}")
    @MethodSource("asStringExceptionProvider")
    void asString_exceptionScenarios(Object input, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> StringConversions.asString(input));
    }
}
