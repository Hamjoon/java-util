package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getClassName_Test {

    class LocalTestClass {
    }

    /**
     * Simple user‑defined class for the “Custom Types” group.
     */
    static class SampleClass {
        // No behaviour required – only the class name is relevant.
    }

    static Stream<Arguments> nullInputProvider() {
        return Stream.of(arguments(null, "null", "When the argument is null, getClassName should return the literal string \"null\"."));
    }

    static Stream<Arguments> simpleTypesProvider() {
        return Stream.of(arguments(Integer.valueOf(123), Integer.class.getName(), "A boxed Integer should return its fully‑qualified class name."), arguments("sample string", String.class.getName(), "A String instance should return java.lang.String."), arguments(Boolean.TRUE, Boolean.class.getName(), "A Boolean constant should return java.lang.Boolean."));
    }

    static Stream<Arguments> arrayTypesProvider() {
        return Stream.of(arguments(new int[] { 1, 2, 3 }, "[I", "A primitive int array should return the JVM internal name for int[] (\"[I\")."), arguments(new String[] { "a", "b" }, String[].class.getName(), "A String array should return the JVM internal name for String[] (\"[Ljava.lang.String;\")."), arguments(new Object[] { new Object() }, Object[].class.getName(), "An Object array should return the JVM internal name for Object[] (\"[Ljava.lang.Object;\")."));
    }

    static Stream<Arguments> customTypesProvider() {
        return Stream.of(arguments(new SampleClass(), SampleClass.class.getName(), "An instance of a user‑defined inner class should return its fully‑qualified name."));
    }

    static Stream<Arguments> enumTypesProvider() {
        return Stream.of(arguments(SampleEnum.VALUE, SampleEnum.class.getName(), "An enum constant should return the name of the enum type, not the constant name."));
    }

    static Stream<Arguments> classObjectProvider() {
        return Stream.of(arguments(String.class, Class.class.getName(), "Passing a Class object should return java.lang.Class because the object's runtime type is Class."));
    }

    @Test
    @DisplayName("getClassName(null) returns the literal string \"null\"")
    void testGetClassNameWithNull() {
        // Given
        Object input = null;
        // When
        String result = ReflectionUtils.getClassName(input);
        // Then
        assertEquals("null", result, "When the input is null the method should return the literal string \"null\".");
    }

    @Test
    @DisplayName("getClassName on a standard object returns its fully‑qualified class name")
    void testGetClassNameWithStandardObject() {
        // Given
        String input = "sample string";
        // When
        String result = ReflectionUtils.getClassName(input);
        // Then
        assertEquals(String.class.getName(), result, "The class name of a String instance should be java.lang.String.");
    }

    @Test
    @DisplayName("getClassName on an array returns the JVM internal array class name")
    void testGetClassNameWithArray() {
        // Given
        String[] input = new String[] { "a", "b" };
        // When
        String result = ReflectionUtils.getClassName(input);
        // Then
        assertEquals(String[].class.getName(), result, "Array class names follow the JVM internal format (e.g. \"[Ljava.lang.String;\").");
    }

    @Test
    @DisplayName("getClassName on a primitive wrapper returns its class name")
    void testGetClassNameWithWrapper() {
        // Given
        Integer input = 42;
        // When
        String result = ReflectionUtils.getClassName(input);
        // Then
        assertEquals(Integer.class.getName(), result, "Wrapper class names should be reported correctly.");
    }

    @Test
    @DisplayName("getClassName on a user‑defined class returns its fully‑qualified name")
    void testGetClassNameWithUserClass() {
        // Given
        class LocalTestClass {
        }
        LocalTestClass input = new LocalTestClass();
        // When
        String result = ReflectionUtils.getClassName(input);
        // Then
        assertEquals(LocalTestClass.class.getName(), result, "The method should return the fully‑qualified name of a user‑defined class.");
    }

    /**
     * Null input – should return the literal string {@code "null"}
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("nullInputProvider")
    void testNullInput(Object input, String expected, String description) {
        assertEquals(expected, ReflectionUtils.getClassName(input), description);
    }

    /**
     * Simple (non‑array, non‑enum) types – wrappers, String, Boolean
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("simpleTypesProvider")
    void testSimpleTypes(Object input, String expected, String description) {
        assertEquals(expected, ReflectionUtils.getClassName(input), description);
    }

    /**
     * Array types – primitive, object and multi‑dimensional arrays
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("arrayTypesProvider")
    void testArrayTypes(Object input, String expected, String description) {
        assertEquals(expected, ReflectionUtils.getClassName(input), description);
    }

    /**
     * User‑defined (custom) classes – inner class example
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("customTypesProvider")
    void testCustomTypes(Object input, String expected, String description) {
        assertEquals(expected, ReflectionUtils.getClassName(input), description);
    }

    /**
     * Enum constants – should return the enum type name, not the constant name
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("enumTypesProvider")
    void testEnumTypes(Object input, String expected, String description) {
        assertEquals(expected, ReflectionUtils.getClassName(input), description);
    }

    /**
     * Passing a {@code Class} object – the runtime type is {@code java.lang.Class}
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("classObjectProvider")
    void testClassObject(Object input, String expected, String description) {
        assertEquals(expected, ReflectionUtils.getClassName(input), description);
    }
}
