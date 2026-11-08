package com.cedarsoftware.util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getClassName_Test {

    class Sample {
    }

    /**
     * Simple static inner class to verify handling of user‑defined types.
     */
    static class SampleClass {
    }

    /**
     * Provides a stream of arguments: the object to test and the expected class name.
     * The expected value is computed at runtime (except for the null case) to avoid
     * hard‑coding package‑specific names that can change if the test class is moved.
     */
    private static Stream<Arguments> classNameProvider() {
        // Primitive wrapper instances
        Integer integer = Integer.valueOf(42);
        Long aLong = Long.valueOf(123L);
        Double aDouble = Double.valueOf(3.14);
        Boolean aBoolean = Boolean.TRUE;
        // Array instances
        int[] intArray = { 1, 2, 3 };
        int[][] int2dArray = new int[2][3];
        String[] stringArray = { "a", "b" };
        // User‑defined types
        SampleClass sampleClassInstance = new SampleClass();
        MyEnum enumInstance = MyEnum.VALUE;
        // Class object (String.class)
        Class<?> classObject = String.class;
        // Standard collection
        ArrayList<String> arrayList = new ArrayList<>();
        // Dynamic proxy implementing Runnable
        InvocationHandler handler = (proxy, method, args) -> null;
        Runnable proxyInstance = (Runnable) Proxy.newProxyInstance(ReflectionUtils.class.getClassLoader(), new Class[] { Runnable.class }, handler);
        return Stream.of(// null
        Arguments.of(null, "null"), // plain Object
        Arguments.of(new Object(), Object.class.getName()), // primitive wrappers
        Arguments.of(integer, Integer.class.getName()), Arguments.of(aLong, Long.class.getName()), Arguments.of(aDouble, Double.class.getName()), Arguments.of(aBoolean, Boolean.class.getName()), // String literal
        Arguments.of("sample string", String.class.getName()), // arrays
        // "[I"
        Arguments.of(intArray, intArray.getClass().getName()), // "[[I"
        Arguments.of(int2dArray, int2dArray.getClass().getName()), // "[Ljava.lang.String;"
        Arguments.of(stringArray, stringArray.getClass().getName()), // user‑defined inner class
        Arguments.of(sampleClassInstance, sampleClassInstance.getClass().getName()), // enum constant
        Arguments.of(enumInstance, enumInstance.getClass().getName()), // Class object
        Arguments.of(classObject, Class.class.getName()), // collection
        Arguments.of(arrayList, ArrayList.class.getName()), // dynamic proxy
        Arguments.of(proxyInstance, proxyInstance.getClass().getName()));
    }

    @Test
    void getClassName_returnsNullStringWhenArgumentIsNull() {
        assertEquals("null", ReflectionUtils.getClassName(null), "When the argument is null the method should return the literal string \"null\"");
    }

    @Test
    void getClassName_returnsClassNameForPlainObject() {
        Object obj = new Object();
        String expected = obj.getClass().getName();
        assertEquals(expected, ReflectionUtils.getClassName(obj), "The returned class name should match Object's runtime class name");
    }

    @Test
    void getClassName_returnsClassNameForCustomClass() {
        class Sample {
        }
        Sample sample = new Sample();
        String expected = sample.getClass().getName();
        assertEquals(expected, ReflectionUtils.getClassName(sample), "The method should correctly handle user‑defined classes");
    }

    @Test
    void getClassName_returnsClassNameForWrapperTypes() {
        Integer intObj = Integer.valueOf(42);
        assertEquals(Integer.class.getName(), ReflectionUtils.getClassName(intObj));
        Boolean boolObj = Boolean.TRUE;
        assertEquals(Boolean.class.getName(), ReflectionUtils.getClassName(boolObj));
    }

    @Test
    void getClassName_returnsClassNameForArrayObjects() {
        int[] intArray = { 1, 2, 3 };
        // e.g., "[I"
        String expected = intArray.getClass().getName();
        assertEquals(expected, ReflectionUtils.getClassName(intArray));
        String[] strArray = { "a", "b" };
        assertEquals(strArray.getClass().getName(), ReflectionUtils.getClassName(strArray));
    }

    @ParameterizedTest(name = "{index}: getClassName({0}) → \"{1}\"")
    @MethodSource("classNameProvider")
    void getClassName_variousInputs(Object input, String expected) {
        assertEquals(expected, ReflectionUtils.getClassName(input), () -> "Unexpected class name for input: " + (input == null ? "null" : input.getClass()));
    }
}
