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

@ExtendWith(MockitoExtension.class)
class ClassConversions_toString_Test {

    /**
     * Mocked Converter – not used by the method under test but required by the signature.
     */
    @Mock
    private Converter converter;

    /**
     * Provides all valid {@code from} arguments together with an indication
     * whether the {@link Converter} should be a mock (true) or {@code null}
     * (false).  The expected string is the value returned by {@code Class#getName()}.
     */
    private static Stream<Arguments> validInputsProvider() {
        return Stream.of(// Valid class inputs – converter = null
        Arguments.of(String.class, false, "java.lang.String", "When a valid Class object (String.class) is supplied, the method should return its fully‑qualified name."), Arguments.of(Integer.class, false, "java.lang.Integer", "When a different valid Class object (Integer.class) is supplied, the method returns the correct name."), Arguments.of(int.class, false, "int", "Primitive class literals (e.g., int.class) are also Class objects; the method should return the primitive name."), Arguments.of(Double.class, false, "java.lang.Double", "A different reference type (Double.class) should return its fully‑qualified name."), Arguments.of(boolean.class, false, "boolean", "Another primitive class literal (boolean.class) should return the primitive name."), Arguments.of(List.class, false, "java.util.List", "An interface class (java.util.List) should be handled like any other Class object."), Arguments.of(java.time.DayOfWeek.class, false, "java.time.DayOfWeek", "An enum type should return its fully‑qualified name."), Arguments.of(Override.class, false, "java.lang.Override", "An annotation type should return its fully‑qualified name."), Arguments.of(String[].class, false, "[Ljava.lang.String;", "A single‑dimensional object array class returns the JVM internal name."), Arguments.of(int[][].class, false, "[[I", "A multi‑dimensional primitive array class returns the correct internal name."), // Valid class inputs – converter = non‑null mock
        Arguments.of(String.class, true, "java.lang.String", "Even when a Converter instance is supplied, the method ignores it and returns the class name."), Arguments.of(int.class, true, "int", "Non‑null converter does not affect primitive class handling."));
    }

    /**
     * Provides inputs that are expected to cause exceptions.
     * The {@code useMockConverter} flag determines whether the {@link Converter}
     * argument is {@code null} or a mock instance.
     */
    private static Stream<Arguments> invalidInputsProvider() {
        return Stream.of(// Null 'from' argument – both with null and non‑null converter
        Arguments.of(null, false, NullPointerException.class, "If the 'from' argument is null, the cast succeeds (null) but calling getName() on a null reference throws NullPointerException."), Arguments.of(null, true, NullPointerException.class, "Null 'from' still triggers NPE regardless of the converter being non‑null."), // Non‑Class object – both with null and non‑null converter
        Arguments.of(new Object(), false, ClassCastException.class, "If 'from' is not a Class instance, the explicit cast to Class<?> fails, resulting in a ClassCastException."), Arguments.of(new Object(), true, ClassCastException.class, "A non‑Class object causes a ClassCastException even when a converter is provided."));
    }

    @Test
    void toString_returnsFullyQualifiedClassName() {
        // GIVEN
        // java.lang.String
        Class<?> inputClass = String.class;
        // WHEN
        String result = ClassConversions.toString(inputClass, converter);
        // THEN
        assertEquals("java.lang.String", result, "The toString method should return the fully‑qualified name of the class.");
    }

    @Test
    void toString_returnsPrimitiveClassName() {
        // GIVEN
        // primitive int
        Class<?> inputClass = int.class;
        // WHEN
        String result = ClassConversions.toString(inputClass, converter);
        // THEN
        assertEquals("int", result, "The toString method should correctly handle primitive class objects.");
    }

    @Test
    void toString_nullInput_throwsNullPointerException() {
        // GIVEN
        Object nullObject = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ClassConversions.toString(nullObject, converter), "Passing null as the 'from' argument should result in a NullPointerException.");
    }

    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("validInputsProvider")
    void toString_returnsExpected(Object from, boolean useMockConverter, String expected, String description) {
        // GIVEN
        Converter conv = useMockConverter ? converter : null;
        // WHEN
        String result = ClassConversions.toString(from, conv);
        // THEN
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("invalidInputsProvider")
    void toString_throwsExpected(Object from, boolean useMockConverter, Class<? extends Throwable> expectedException, String description) {
        // GIVEN
        Converter conv = useMockConverter ? converter : null;
        // WHEN / THEN
        assertThrows(expectedException, () -> ClassConversions.toString(from, conv), description);
    }
}
