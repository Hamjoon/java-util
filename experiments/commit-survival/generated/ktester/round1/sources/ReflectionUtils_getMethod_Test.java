package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Method;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getMethod_Test {

    // -----------------------------------------------------------------------
    @SuppressWarnings("unused")
    private static class SampleClass {

        public void process(int count, String label) {
            // no‑op – method exists solely for reflection testing
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Ensure a clean cache for each test to avoid cross‑test interference.
        clearMethodCache();
    }

    /**
     * Helper that clears the private static METHOD_MAP cache via reflection.
     */
    private static void clearMethodCache() throws Exception {
        var field = ReflectionUtils.class.getDeclaredField("METHOD_MAP");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentMap<String, Method> map = (ConcurrentMap<String, Method>) field.get(null);
        map.clear();
    }

    /**
     * Utility to convert a textual representation of a class (as used in the JSON test data)
     * into a {@link Class} instance.
     */
    private static Class<?> classFromString(String name) throws ClassNotFoundException {
        if (name == null) {
            return null;
        }
        switch(name) {
            case "int":
                return int.class;
            case "long":
                return long.class;
            case "double":
                return double.class;
            case "float":
                return float.class;
            case "short":
                return short.class;
            case "byte":
                return byte.class;
            case "char":
                return char.class;
            case "boolean":
                return boolean.class;
            case "void":
                return void.class;
            default:
                // Handles array types like "int[]"
                if (name.endsWith("[]")) {
                    String element = name.substring(0, name.length() - 2);
                    return java.lang.reflect.Array.newInstance(classFromString(element), 0).getClass();
                }
                return Class.forName(name);
        }
    }

    static Stream<Arguments> nullClassArguments() {
        return Stream.of(arguments(null, "foo", new Class<?>[0]));
    }

    static Stream<Arguments> nullMethodNameArguments() throws Exception {
        return Stream.of(arguments(String.class, null, new Class<?>[0]));
    }

    static Stream<Arguments> nullTypesArrayArguments() {
        return Stream.of(arguments(String.class, "length", null));
    }

    static Stream<Arguments> nullElementInTypesArrayArguments() {
        return Stream.of(arguments(String.class, "charAt", new Class<?>[] { int.class, null }));
    }

    static Stream<Arguments> emptyMethodNameArguments() {
        return Stream.of(arguments(String.class, "", new Class<?>[0]));
    }

    static Stream<Arguments> primitiveClassArguments() {
        return Stream.of(arguments(int.class, "toString", new Class<?>[0]));
    }

    static Stream<Arguments> normalMethodRetrievalArguments() throws Exception {
        return Stream.of(arguments(String.class, "substring", new Class<?>[] { int.class, int.class }));
    }

    static Stream<Arguments> methodNotFoundArguments() {
        return Stream.of(arguments(Integer.class, "nonExistentMethod", new Class<?>[0]));
    }

    static Stream<Arguments> overloadedMethodSelectionArguments() throws Exception {
        return Stream.of(arguments(Math.class, "max", new Class<?>[] { int.class, int.class }), arguments(Math.class, "max", new Class<?>[] { double.class, double.class }));
    }

    static Stream<Arguments> primitiveParameterTypesArguments() throws Exception {
        // Using Collections.rotate(List, int) as a reliable method with a primitive int parameter.
        return Stream.of(arguments(Collections.class, "rotate", new Class<?>[] { List.class, int.class }));
    }

    static Stream<Arguments> arrayParameterTypesArguments() throws Exception {
        return Stream.of(arguments(Arrays.class, "sort", new Class<?>[] { int[].class }));
    }

    /**
     * Verify that a known public method can be retrieved.
     */
    @Test
    void getMethod_existingMethod_returnsMethod() {
        // given
        Class<?> targetClass = String.class;
        String methodName = "substring";
        Class<?>[] paramTypes = new Class<?>[] { int.class, int.class };
        // when
        Method method = ReflectionUtils.getMethod(targetClass, methodName, paramTypes);
        // then
        assertNotNull(method, "Method should be found");
        assertEquals(methodName, method.getName(), "Method name should match");
        assertArrayEquals(paramTypes, method.getParameterTypes(), "Parameter types should match");
        assertEquals(targetClass, method.getDeclaringClass(), "Declaring class should be String");
    }

    /**
     * Verify that the caching mechanism returns the same {@link Method} instance
     * for identical look‑ups.
     */
    @Test
    void getMethod_sameLookup_returnsCachedMethodInstance() {
        // given
        Class<?> targetClass = String.class;
        String methodName = "charAt";
        Class<?>[] paramTypes = new Class<?>[] { int.class };
        // when – first lookup
        Method first = ReflectionUtils.getMethod(targetClass, methodName, paramTypes);
        // when – second lookup (should hit the cache)
        Method second = ReflectionUtils.getMethod(targetClass, methodName, paramTypes);
        // then
        assertNotNull(first, "First lookup should succeed");
        assertNotNull(second, "Second lookup should succeed");
        assertSame(first, second, "Both lookups should return the same cached Method instance");
    }

    /**
     * Verify that a request for a non‑existent method returns {@code null}
     * instead of throwing an exception.
     */
    @Test
    void getMethod_nonExistingMethod_returnsNull() {
        // given
        Class<?> targetClass = String.class;
        String methodName = "nonExistingMethod";
        Class<?>[] paramTypes = new Class<?>[] {};
        // when
        Method result = ReflectionUtils.getMethod(targetClass, methodName, paramTypes);
        // then
        assertNull(result, "Lookup of a non‑existent method should return null");
    }

    /**
     * Verify that a method with primitive and reference parameter types is resolved correctly.
     */
    @Test
    void getMethod_mixedParameterTypes_returnsMethod() {
        // given
        Class<?> targetClass = SampleClass.class;
        String methodName = "process";
        Class<?>[] paramTypes = new Class<?>[] { int.class, String.class };
        // when
        Method method = ReflectionUtils.getMethod(targetClass, methodName, paramTypes);
        // then
        assertNotNull(method, "Method with mixed parameter types should be found");
        assertEquals(methodName, method.getName());
        assertArrayEquals(paramTypes, method.getParameterTypes());
    }

    @ParameterizedTest(name = "[{index}] null class argument")
    @MethodSource("nullClassArguments")
    void getMethod_nullClass_returnsNull(Class<?> c, String methodName, Class<?>[] types) {
        Method result = ReflectionUtils.getMethod(c, methodName, types);
        assertNull(result, "Passing a null Class should result in null");
    }

    @ParameterizedTest(name = "[{index}] null methodName")
    @MethodSource("nullMethodNameArguments")
    void getMethod_nullMethodName_returnsNull(Class<?> c, String methodName, Class<?>[] types) {
        Method result = ReflectionUtils.getMethod(c, methodName, types);
        assertNull(result, "Passing a null methodName should result in null");
    }

    @ParameterizedTest(name = "[{index}] null types array")
    @MethodSource("nullTypesArrayArguments")
    void getMethod_nullTypesArray_returnsNull(Class<?> c, String methodName, Class<?>[] types) {
        Method result = ReflectionUtils.getMethod(c, methodName, types);
        assertNull(result, "Passing a null types array should result in null");
    }

    @ParameterizedTest(name = "[{index}] null element in types array")
    @MethodSource("nullElementInTypesArrayArguments")
    void getMethod_nullElementInTypesArray_returnsNull(Class<?> c, String methodName, Class<?>[] types) {
        Method result = ReflectionUtils.getMethod(c, methodName, types);
        assertNull(result, "Null element inside types array should result in null");
    }

    @ParameterizedTest(name = "[{index}] empty method name")
    @MethodSource("emptyMethodNameArguments")
    void getMethod_emptyMethodName_returnsNull(Class<?> c, String methodName, Class<?>[] types) {
        Method result = ReflectionUtils.getMethod(c, methodName, types);
        assertNull(result, "Empty method name should result in null");
    }

    @ParameterizedTest(name = "[{index}] primitive class argument")
    @MethodSource("primitiveClassArguments")
    void getMethod_primitiveClassArgument_returnsNull(Class<?> c, String methodName, Class<?>[] types) {
        Method result = ReflectionUtils.getMethod(c, methodName, types);
        assertNull(result, "Using a primitive Class as target should result in null");
    }

    @ParameterizedTest(name = "[{index}] normal method retrieval")
    @MethodSource("normalMethodRetrievalArguments")
    void getMethod_normalMethodRetrieval_returnsMethod(Class<?> c, String methodName, Class<?>[] types) {
        Method method = ReflectionUtils.getMethod(c, methodName, types);
        assertNotNull(method, "Method should be found");
        assertEquals(methodName, method.getName());
        assertArrayEquals(types, method.getParameterTypes());
    }

    @ParameterizedTest(name = "[{index}] method not found")
    @MethodSource("methodNotFoundArguments")
    void getMethod_methodNotFound_returnsNull(Class<?> c, String methodName, Class<?>[] types) {
        Method method = ReflectionUtils.getMethod(c, methodName, types);
        assertNull(method, "Non‑existent method should return null");
    }

    @ParameterizedTest(name = "[{index}] overloaded method selection")
    @MethodSource("overloadedMethodSelectionArguments")
    void getMethod_overloadedSelection_returnsMethod(Class<?> c, String methodName, Class<?>[] types) {
        Method method = ReflectionUtils.getMethod(c, methodName, types);
        assertNotNull(method, "Overloaded method should be found");
        assertEquals(methodName, method.getName());
        assertArrayEquals(types, method.getParameterTypes());
    }

    @ParameterizedTest(name = "[{index}] primitive parameter types")
    @MethodSource("primitiveParameterTypesArguments")
    void getMethod_primitiveParameterTypes_returnsMethod(Class<?> c, String methodName, Class<?>[] types) {
        Method method = ReflectionUtils.getMethod(c, methodName, types);
        assertNotNull(method, "Method with primitive parameter types should be found");
        assertEquals(methodName, method.getName());
        assertArrayEquals(types, method.getParameterTypes());
    }

    @ParameterizedTest(name = "[{index}] array parameter types")
    @MethodSource("arrayParameterTypesArguments")
    void getMethod_arrayParameterTypes_returnsMethod(Class<?> c, String methodName, Class<?>[] types) {
        Method method = ReflectionUtils.getMethod(c, methodName, types);
        assertNotNull(method, "Method with array parameter type should be found");
        assertEquals(methodName, method.getName());
        assertArrayEquals(types, method.getParameterTypes());
    }
}
