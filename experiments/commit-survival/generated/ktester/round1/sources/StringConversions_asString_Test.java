package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.nio.CharBuffer;
import java.util.stream.Stream;

class StringConversions_asString_Test {

    /**
     * Class whose {@code toString()} returns a custom non‑null value.
     */
    private static final class CustomToString {

        @Override
        public String toString() {
            return "custom-value";
        }
    }

    /**
     * Class whose {@code toString()} deliberately returns {@code null}.
     */
    private static final class NullToString {

        @Override
        public String toString() {
            return null;
        }
    }

    /**
     * Class that throws a supplied {@link RuntimeException} (or subclass) from its {@code toString()}.
     * The exception instance is stored in a public field so the test can verify that the exact same
     * instance is propagated.
     */
    private static final class ThrowingToString {

        final RuntimeException exceptionToThrow;

        ThrowingToString(RuntimeException exceptionToThrow) {
            this.exceptionToThrow = exceptionToThrow;
        }

        @Override
        public String toString() {
            throw exceptionToThrow;
        }
    }

    private static Stream<Arguments> nonNullInputProvider() {
        return Stream.of(Arguments.of("Plain String should be returned unchanged", "hello world", "hello world"), Arguments.of("Integer object's toString() yields its decimal representation", 12345, "12345"), Arguments.of("Boolean true should be converted to the string \"true\"", true, "true"), Arguments.of("Custom class overriding toString() returns \"custom-value\"", new CustomToString(), "custom-value"), Arguments.of("Class whose toString() returns null propagates null", new NullToString(), null), Arguments.of("Empty String should be returned as empty", "", ""), Arguments.of("StringBuilder's toString() returns its current sequence", new StringBuilder("builder"), "builder"), Arguments.of("CharBuffer's toString() returns remaining characters", CharBuffer.wrap("charbuf"), "charbuf"), Arguments.of("Enum constant uses its name as toString() result", TestEnum.VALUE1, "VALUE1"));
    }

    private static Stream<Arguments> exceptionThrowingInputProvider() {
        return Stream.of(Arguments.of("toString() throws NullPointerException", new ThrowingToString(new NullPointerException("NPE in toString()")), NullPointerException.class), Arguments.of("toString() throws IllegalArgumentException", new ThrowingToString(new IllegalArgumentException("bad argument")), IllegalArgumentException.class), Arguments.of("toString() throws generic RuntimeException", new ThrowingToString(new RuntimeException("generic runtime")), RuntimeException.class));
    }

    @Test
    void asStringReturnsNullWhenInputIsNull() {
        // given
        Object input = null;
        // when
        String result = StringConversions.asString(input);
        // then
        assertNull(result, "asString should return null for a null argument");
    }

    @Test
    void asStringReturnsToStringResultForNonNullObject() {
        // given
        Object input = new StringBuilder("test123");
        // when
        String result = StringConversions.asString(input);
        // then
        assertEquals("test123", result, "asString should delegate to Object.toString()");
    }

    @Test
    void asStringWorksWithPrimitiveWrapper() {
        // given
        Integer input = 42;
        // when
        String result = StringConversions.asString(input);
        // then
        assertEquals("42", result, "asString should correctly convert Integer to its string representation");
    }

    @Test
    void asStringWorksWithCustomObject() {
        // given
        Object input = new Object() {

            @Override
            public String toString() {
                return "custom";
            }
        };
        // when
        String result = StringConversions.asString(input);
        // then
        assertEquals("custom", result, "asString should use the overridden toString() implementation");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nonNullInputProvider")
    void asStringNonNullHandling(String description, Object input, String expected) {
        // when
        String result = StringConversions.asString(input);
        // then
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("exceptionThrowingInputProvider")
    void asStringExceptionPropagation(String description, ThrowingToString input, Class<? extends Throwable> expectedException) {
        // when / then
        Throwable thrown = assertThrows(expectedException, () -> StringConversions.asString(input), description);
        // Verify that the thrown exception is the same instance that the ThrowingToString was configured with
        assertSame(input.exceptionToThrow, thrown, "asString should propagate the exact exception thrown by toString()");
    }
}
