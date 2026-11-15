package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ClassConversions_toString_Test {

    // -------------------------------------------------------------------------
    static class DummyConverter implements Converter {
        // No behaviour required – the method under test never calls any Converter method
    }

    static Stream<Arguments> normalBehaviorProvider() {
        return Stream.of(Arguments.of("Standard class conversion – java.lang.String", java.lang.String.class, null, "java.lang.String"), Arguments.of("Array class conversion – int[]", int[].class, null, "[I"), Arguments.of("Inner class conversion – java.util.Map$Entry", java.util.Map.Entry.class, null, "java.util.Map$Entry"), Arguments.of("Primitive void type – Void.TYPE", java.lang.Void.TYPE, null, "void"), Arguments.of("Converter argument ignored – non‑null DummyConverter", java.lang.Integer.class, new DummyConverter(), "java.lang.Integer"));
    }

    static Stream<Arguments> errorHandlingProvider() {
        return Stream.of(Arguments.of("Null 'from' argument – should throw NullPointerException", null, null, NullPointerException.class), Arguments.of("Invalid type – passing a non‑Class object should throw ClassCastException", new Object(), null, ClassCastException.class));
    }

    static Stream<Arguments> primitiveTypesProvider() {
        return Stream.of(Arguments.of("Primitive int type", int.class, "int"), Arguments.of("Primitive boolean type", boolean.class, "boolean"), Arguments.of("Primitive double type", double.class, "double"), Arguments.of("Primitive char type", char.class, "char"));
    }

    static Stream<Arguments> multiDimArrayProvider() {
        return Stream.of(Arguments.of("Two‑dimensional int array", int[][].class, "[[I"), Arguments.of("Two‑dimensional double array", double[][].class, "[[D"), Arguments.of("Two‑dimensional String array", java.lang.String[][].class, "[[Ljava.lang.String;"));
    }

    static Stream<Arguments> interfaceEnumProvider() {
        return Stream.of(Arguments.of("Interface type – java.util.List", java.util.List.class, "java.util.List"), Arguments.of("Enum type – java.time.DayOfWeek", java.time.DayOfWeek.class, "java.time.DayOfWeek"));
    }

    static Stream<Arguments> converterIgnoredProvider() {
        return Stream.of(Arguments.of("Non‑null Converter supplied – int.class", int.class, new DummyConverter(), "int"), Arguments.of("Non‑null Converter with inner class – java.util.Map$Entry", java.util.Map.Entry.class, new DummyConverter(), "java.util.Map$Entry"));
    }

    @Test
    void testToStringReturnsClassName() {
        Class<?> clazz = java.util.ArrayList.class;
        // Converter is not used by the method
        Converter converter = mock(Converter.class);
        String result = ClassConversions.toString(clazz, converter);
        assertEquals("java.util.ArrayList", result);
    }

    @Test
    void testToStringWithPrimitiveClass() {
        Class<?> clazz = int.class;
        Converter converter = mock(Converter.class);
        String result = ClassConversions.toString(clazz, converter);
        assertEquals("int", result);
    }

    @Test
    void testToStringWithInvalidObjectThrowsClassCastException() {
        Object notAClass = "I am not a Class object";
        Converter converter = mock(Converter.class);
        assertThrows(ClassCastException.class, () -> ClassConversions.toString(notAClass, converter));
    }

    @Test
    void testToStringWithNullObjectThrowsNullPointerException() {
        Converter converter = mock(Converter.class);
        assertThrows(NullPointerException.class, () -> ClassConversions.toString(null, converter));
    }

    // -------------------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("normalBehaviorProvider")
    void testNormalBehavior(String description, Object from, Converter converter, String expected) {
        String result = ClassConversions.toString(from, converter);
        assertEquals(expected, result, description);
    }

    // -------------------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("errorHandlingProvider")
    void testErrorHandling(String description, Object from, Converter converter, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> ClassConversions.toString(from, converter), description);
    }

    // -------------------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("primitiveTypesProvider")
    void testPrimitiveTypes(String description, Class<?> primitiveClass, String expected) {
        String result = ClassConversions.toString(primitiveClass, null);
        assertEquals(expected, result, description);
    }

    // -------------------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("multiDimArrayProvider")
    void testMultiDimensionalArrays(String description, Class<?> arrayClass, String expected) {
        String result = ClassConversions.toString(arrayClass, null);
        assertEquals(expected, result, description);
    }

    // -------------------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("interfaceEnumProvider")
    void testInterfaceAndEnumTypes(String description, Class<?> clazz, String expected) {
        String result = ClassConversions.toString(clazz, null);
        assertEquals(expected, result, description);
    }

    // -------------------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("converterIgnoredProvider")
    void testConverterIgnored(String description, Class<?> clazz, Converter converter, String expected) {
        String result = ClassConversions.toString(clazz, converter);
        assertEquals(expected, result, description);
    }

    @Disabled("Local/anonymous class names depend on the package structure; not reproducible in this test module.")
    @Test
    void testLocalClassName() {
        // Intended test (illustrative only):
        // Class<?> local = com.example.TestClass$LocalClass.class;
        // assertEquals("com.example.TestClass$LocalClass", ClassConversions.toString(local, null));
    }

    @Disabled("Anonymous class names depend on the package structure; not reproducible in this test module.")
    @Test
    void testAnonymousClassName() {
        // Intended test (illustrative only):
        // Class<?> anon = com.example.TestClass$1.class;
        // assertEquals("com.example.TestClass$1", ClassConversions.toString(anon, null));
    }
}
