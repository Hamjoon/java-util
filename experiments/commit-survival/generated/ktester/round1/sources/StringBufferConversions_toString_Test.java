package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.List;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class StringBufferConversions_toString_Test {

    @Mock
    private Converter // not used by the method but required for signature
    mockConverter;

    class Dummy {

        @Override
        public String toString() {
            return "dummy-string";
        }
    }

    /**
     * Simple class that overrides {@code toString()} to return a custom value.
     */
    static class CustomToString {

        private final String value;

        CustomToString(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    /**
     * toString() that deliberately throws an {@link IllegalStateException}.
     */
    static class ThrowingToString {

        @Override
        public String toString() {
            throw new IllegalStateException("boom");
        }
    }

    /**
     * toString() that throws a generic {@link RuntimeException}.
     */
    static class ThrowingRuntimeExceptionToString {

        @Override
        public String toString() {
            throw new RuntimeException("runtime boom");
        }
    }

    private static Stream<Arguments> normalObjectsProvider() {
        return Stream.of(// plain String
        arguments("simple string", null, "simple string", "Standard case: a plain String object should return its own value."), // Integer (auto‑boxed)
        arguments(42, mock(Converter.class), "42", "Integer input should be converted via Integer.toString(). Converter is ignored."), // StringBuilder
        arguments(new StringBuilder("builder"), mock(Converter.class), "builder", "StringBuilder's toString() should be returned unchanged."), // List
        arguments(List.of("a", "b", "c"), null, "[a, b, c]", "A List's default toString representation should be returned."), // Custom object with overridden toString()
        arguments(new CustomToString("custom"), null, "custom", "Object with an overridden toString() should return the overridden value."));
    }

    private static Stream<Arguments> converterIgnoredProvider() {
        return Stream.of(arguments("converter ignored", mock(Converter.class), "converter ignored", "Even when a non‑null Converter is supplied, the method should ignore it and return from.toString()."));
    }

    /**
     * Utility method to obtain a Mockito mock of {@link Converter} without needing an
     * instance field (used in static method sources).
     */
    private static Converter mock(Class<Converter> cls) {
        return org.mockito.Mockito.mock(cls);
    }

    /**
     * Verify that the method returns the result of the object's {@code toString()} method.
     */
    @Test
    void toString_returnsObjectToString() {
        // GIVEN
        class Dummy {

            @Override
            public String toString() {
                return "dummy-string";
            }
        }
        Dummy dummy = new Dummy();
        // WHEN
        String result = StringBufferConversions.toString(dummy, mockConverter);
        // THEN
        assertEquals("dummy-string", result, "StringBufferConversions.toString should return the object's toString() value");
    }

    /**
     * Verify that a {@code NullPointerException} is thrown when the source object is {@code null}.
     */
    @Test
    void toString_nullObject_throwsNPE() {
        // GIVEN
        Object nullObject = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> StringBufferConversions.toString(nullObject, mockConverter), "Calling toString with a null source should throw NullPointerException");
    }

    /* -------------------------------------------------------------
     *  Group: toString_normalObjects
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("normalObjectsProvider")
    void toString_normalObjects(Object from, Converter converter, String expected, String description) {
        // WHEN
        String result = StringBufferConversions.toString(from, converter);
        // THEN
        assertEquals(expected, result, description);
    }

    /* -------------------------------------------------------------
     *  Group: toString_nullInput
     * ------------------------------------------------------------- */
    @Test
    void toString_nullInput_throwsNPE() {
        // GIVEN
        Object nullObject = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> StringBufferConversions.toString(nullObject, mockConverter), "Passing a null reference for 'from' must result in a NullPointerException.");
    }

    /* -------------------------------------------------------------
     *  Group: toString_converterIgnored
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("converterIgnoredProvider")
    void toString_converterIgnored(Object from, Converter converter, String expected, String description) {
        // WHEN
        String result = StringBufferConversions.toString(from, converter);
        // THEN
        assertEquals(expected, result, description);
    }

    /* -------------------------------------------------------------
     *  Group: toString_toStringThrows
     * ------------------------------------------------------------- */
    @Test
    void toString_toStringThrows_propagatesIllegalStateException() {
        // GIVEN
        Object throwing = new ThrowingToString();
        // WHEN / THEN
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> StringBufferConversions.toString(throwing, mockConverter), "Method should propagate IllegalStateException thrown by object's toString().");
        assertEquals("boom", ex.getMessage());
    }

    /* -------------------------------------------------------------
     *  Group: toString_runtimeException
     * ------------------------------------------------------------- */
    @Test
    void toString_runtimeException_propagatesRuntimeException() {
        // GIVEN
        Object throwing = new ThrowingRuntimeExceptionToString();
        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class, () -> StringBufferConversions.toString(throwing, mockConverter), "Method should propagate RuntimeException thrown by object's toString().");
        assertEquals("runtime boom", ex.getMessage());
    }
}
