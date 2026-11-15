package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Proxy;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getClassName_Test {

    class Sample {
    }

    class MyClass {
    }

    class MyClass {
    }

    /**
     * Provides a wide range of objects together with the expected class name
     * returned by {@link ReflectionUtils#getClassName(Object)}.
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> provideObjectsForClassName() {
        // Custom class used for the “Custom Class” group.
        class MyClass {
        }
        // Proxy instance for the “Proxy Instance” group.
        Object proxyInstance = Proxy.newProxyInstance(ReflectionUtils.class.getClassLoader(), new Class[] { Runnable.class }, (proxy, method, args) -> null);
        // Expected proxy class name (e.g., $Proxy0). The exact name depends on the JVM,
        // but it always starts with "$Proxy".
        String proxyClassName = proxyInstance.getClass().getName();
        return Stream.of(// Null Input
        org.junit.jupiter.params.provider.Arguments.of(null, "null"), // Primitive Wrapper (single case – already covered above but kept for completeness)
        org.junit.jupiter.params.provider.Arguments.of(Integer.valueOf(42), Integer.class.getName()), // String Input
        org.junit.jupiter.params.provider.Arguments.of("test", String.class.getName()), // Array Input – one‑dimensional primitive array
        org.junit.jupiter.params.provider.Arguments.of(new int[] { 1, 2, 3 }, "[I"), // Custom Class – using the local MyClass defined above
        org.junit.jupiter.params.provider.Arguments.of(new MyClass(), MyClass.class.getName()), // Primitive Wrapper Variants
        org.junit.jupiter.params.provider.Arguments.of(Boolean.TRUE, Boolean.class.getName()), org.junit.jupiter.params.provider.Arguments.of(Character.valueOf('c'), Character.class.getName()), org.junit.jupiter.params.provider.Arguments.of(Double.valueOf(3.14), Double.class.getName()), // Multi‑Dimensional Array – two‑dimensional String array
        org.junit.jupiter.params.provider.Arguments.of(new String[][] { { "a" } }, "[[Ljava.lang.String;"), // Multi‑Dimensional Array – two‑dimensional int array
        org.junit.jupiter.params.provider.Arguments.of(new int[2][3], "[[I"), // Enum Constant
        org.junit.jupiter.params.provider.Arguments.of(DayOfWeek.MONDAY, DayOfWeek.class.getName()), // Class Object
        org.junit.jupiter.params.provider.Arguments.of(String.class, Class.class.getName()), // Proxy Instance
        org.junit.jupiter.params.provider.Arguments.of(proxyInstance, proxyClassName), // Collection Instance
        org.junit.jupiter.params.provider.Arguments.of(new ArrayList<>(), ArrayList.class.getName()), // Plain Object Instance
        org.junit.jupiter.params.provider.Arguments.of(new Object(), Object.class.getName()));
    }

    @Test
    @DisplayName("getClassName returns \"null\" when argument is null")
    void getClassName_returnsNullStringWhenArgumentIsNull() {
        // Given
        Object input = null;
        // When
        String result = ReflectionUtils.getClassName(input);
        // Then
        assertEquals("null", result, "Expected the string \"null\" for a null argument");
    }

    @Test
    @DisplayName("getClassName returns fully‑qualified class name for a regular object")
    void getClassName_returnsClassNameForNonNullObject() {
        // Given
        String input = "test string";
        // When
        String result = ReflectionUtils.getClassName(input);
        // Then
        assertEquals(String.class.getName(), result, "Expected the fully‑qualified class name of java.lang.String");
    }

    @Test
    @DisplayName("getClassName works with custom class instances")
    void getClassName_returnsClassNameForCustomObject() {
        // Given
        class Sample {
        }
        Sample input = new Sample();
        // When
        String result = ReflectionUtils.getClassName(input);
        // Then
        assertEquals(Sample.class.getName(), result, "Expected the fully‑qualified name of the custom Sample class");
    }

    @Test
    @DisplayName("getClassName works with boxed primitive types")
    void getClassName_returnsClassNameForBoxedPrimitive() {
        // Given
        Integer input = 42;
        // When
        String result = ReflectionUtils.getClassName(input);
        // Then
        assertEquals(Integer.class.getName(), result, "Expected the fully‑qualified class name of java.lang.Integer");
    }

    @ParameterizedTest(name = "{index} => object={0}, expectedClassName={1}")
    @MethodSource("provideObjectsForClassName")
    @DisplayName("getClassName returns correct class name for a variety of inputs")
    void getClassName_returnsExpectedClassName(Object input, String expectedClassName) {
        // When
        String result = ReflectionUtils.getClassName(input);
        // Then
        assertEquals(expectedClassName, result, () -> "Expected class name [" + expectedClassName + "] but got [" + result + "] for input: " + input);
    }
}
