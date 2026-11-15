package com.cedarsoftware.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

class ClassUtilities_isPrimitive_Test {

    /**
     * Provides a stream of arguments: {@code Class<?>} to test and the expected boolean result.
     * The conversion from the textual representation used in the JSON file to a real
     * {@code Class} object is performed by {@link #classFromString(String)}.
     */
//     private static Stream<Arguments> primitiveTestProvider() {
//         return // Primitive Types
//         Stream.// Primitive Types
//         of(// Wrapper Types (mapped to primitives)
//         Arguments.of(classFromString("int"), true), // Wrapper Types (mapped to primitives)
//         Arguments.of(classFromString("boolean"), true), // Wrapper Types (mapped to primitives)
//         Arguments.of(classFromString("double"), true), // Wrapper Types (mapped to primitives)
//         Arguments.of(classFromString("void"), true), // Wrapper Types (mapped to primitives)
//         Arguments.of(classFromString("byte"), true), // Wrapper Types (mapped to primitives)
//         Arguments.of(classFromString("short"), true), // Wrapper Types (mapped to primitives)
//         Arguments.of(classFromString("long"), true), // Wrapper Types (mapped to primitives)
//         Arguments.of(classFromString("char"), true), // Wrapper Types (mapped to primitives)
//         Arguments.of(classFromString("float"), true), // Non‑primitive, Non‑wrapper Types
//         Arguments.of(classFromString("java.lang.Integer"), true), // Non‑primitive, Non‑wrapper Types
//         Arguments.of(classFromString("java.lang.Boolean"), true), // Non‑primitive, Non‑wrapper Types
//         Arguments.of(classFromString("java.lang.Double"), true), // Non‑primitive, Non‑wrapper Types
//         Arguments.of(classFromString("java.lang.Void"), true), // Non‑primitive, Non‑wrapper Types
//         Arguments.of(classFromString("java.lang.Byte"), true), // Non‑primitive, Non‑wrapper Types
//         Arguments.of(classFromString("java.lang.Short"), true), // Non‑primitive, Non‑wrapper Types
//         Arguments.of(classFromString("java.lang.Long"), true), // Non‑primitive, Non‑wrapper Types
//         Arguments.of(classFromString("java.lang.Character"), true), // Non‑primitive, Non‑wrapper Types
//         Arguments.of(classFromString("java.lang.Float"), true), // Edge Cases
//         Arguments.of(classFromString("java.lang.String"), false), // Edge Cases
//         Arguments.of(classFromString("java.util.List"), false), // Edge Cases
//         Arguments.of(classFromString("java.lang.Object"), false), Arguments.of(classFromString("int[]"), false), Arguments.of(classFromString("java.time.DayOfWeek"), false), Arguments.of(classFromString("java.lang.Number"), false), Arguments.of(classFromString("java.util.Map"), false));
//     }

    /**
     * Translates a string representation (e.g., "int", "java.lang.String", "int[]")
     * into the corresponding {@link Class} object.
     *
     * @param name the textual representation
     * @return the {@code Class} object
     */
    private static Class<?> classFromString(String name) {
        switch(name) {
            // primitives
            case "boolean":
                return boolean.class;
            case "byte":
                return byte.class;
            case "short":
                return short.class;
            case "int":
                return int.class;
            case "long":
                return long.class;
            case "char":
                return char.class;
            case "float":
                return float.class;
            case "double":
                return double.class;
            case "void":
                return void.class;
            // array of primitives (edge case)
            case "int[]":
                return int[].class;
            // fully‑qualified class names
            default:
                try {
                    return Class.forName(name);
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException("Unable to resolve class name: " + name, e);
                }
        }
    }

    /**
     * Returns {@code true} if the supplied method has a {@code void} return type.
     */
    public static boolean returnsVoid(Method method) {
        return method.getReturnType() == void.class;
    }

    /**
     * Returns {@code true} if the supplied class contains at least one method annotated with the given annotation.
     */
    public static boolean isAnnotatedMethodPresent(Class<?> clazz, Class<? extends Annotation> annotation) {
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.isAnnotationPresent(annotation)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if the supplied class declares a method with the given name (any signature).
     */
    public static boolean isMethodPresent(Class<?> clazz, String name) {
        try {
            clazz.getDeclaredMethod(name);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /**
     * Finds a method with the given name and parameter types, or {@code null} if none exists.
     */
    public static Method findMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        try {
            return clazz.getDeclaredMethod(name, paramTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * Returns {@code true} if the supplied method is {@code static}.
     */
    public static boolean isStatic(Method method) {
        return Modifier.isStatic(method.getModifiers());
    }

    /**
     * Returns {@code true} if the supplied method is {@code public}.
     */
    public static boolean isPublic(Method method) {
        return Modifier.isPublic(method.getModifiers());
    }

    /**
     * Returns {@code true} if the supplied method is {@code private}.
     */
    public static boolean isPrivate(Method method) {
        return Modifier.isPrivate(method.getModifiers());
    }

    /**
     * Returns {@code true} if the supplied method is {@code abstract}.
     */
    public static boolean isAbstract(Method method) {
        return Modifier.isAbstract(method.getModifiers());
    }

    /**
     * Returns {@code true} if the supplied method is {@code final}.
     */
    public static boolean isFinal(Method method) {
        return Modifier.isFinal(method.getModifiers());
    }

    /**
     * Returns {@code true} if the supplied class is {@code public}.
     */
    public static boolean isPublic(Class<?> clazz) {
        return Modifier.isPublic(clazz.getModifiers());
    }

//     @Test
//     @DisplayName("isPrimitive returns true for all Java primitive types")
//     void testPrimitiveTypes() {
//         assertTrue(ClassUtilities.isPrimitive(boolean.class));
//         assertTrue(ClassUtilities.isPrimitive(byte.class));
//         assertTrue(ClassUtilities.isPrimitive(char.class));
//         assertTrue(ClassUtilities.isPrimitive(short.class));
//         assertTrue(ClassUtilities.isPrimitive(int.class));
//         assertTrue(ClassUtilities.isPrimitive(long.class));
//         assertTrue(ClassUtilities.isPrimitive(float.class));
//         assertTrue(ClassUtilities.isPrimitive(double.class));
//         assertTrue(ClassUtilities.isPrimitive(void.class));
//     }

//     @Test
//     @DisplayName("isPrimitive returns true for primitive wrapper classes")
//     void testWrapperTypes() {
//         assertTrue(ClassUtilities.isPrimitive(Boolean.class));
//         assertTrue(ClassUtilities.isPrimitive(Byte.class));
//         assertTrue(ClassUtilities.isPrimitive(Character.class));
//         assertTrue(ClassUtilities.isPrimitive(Short.class));
//         assertTrue(ClassUtilities.isPrimitive(Integer.class));
//         assertTrue(ClassUtilities.isPrimitive(Long.class));
//         assertTrue(ClassUtilities.isPrimitive(Float.class));
//         assertTrue(ClassUtilities.isPrimitive(Double.class));
//         assertTrue(ClassUtilities.isPrimitive(Void.class));
//     }

//     @Test
//     @DisplayName("isPrimitive returns false for regular classes")
//     void testRegularClasses() {
//         assertFalse(ClassUtilities.isPrimitive(String.class));
//         assertFalse(ClassUtilities.isPrimitive(Object.class));
//         assertFalse(ClassUtilities.isPrimitive(List.class));
//         assertFalse(ClassUtilities.isPrimitive(Map.Entry.class));
//     }

//     @Test
//     @DisplayName("isPrimitive returns false for array types")
//     void testArrayTypes() {
//         assertFalse(ClassUtilities.isPrimitive(int[].class));
//         assertFalse(ClassUtilities.isPrimitive(Integer[].class));
//         assertFalse(ClassUtilities.isPrimitive(Object[].class));
//     }

//     @Test
//     @DisplayName("isPrimitive returns false for primitive type literals wrapped in generics")
//     void testGenericTypeParameters() {
//         // Using reflection to obtain a generic type (e.g., List<Integer>)
//         // The raw type is List, which is not primitive.
//         assertFalse(ClassUtilities.isPrimitive(List.class));
//     }

//     @Test
//     @DisplayName("isPrimitive returns false when null is supplied")
//     void testNullArgument() {
//         assertThrows(NullPointerException.class, () -> ClassUtilities.isPrimitive(null), "Method should throw NPE for null argument");
//     }

//     @Test
//     @DisplayName("Wrapper classes are recognised via the internal map")
//     void testWrapperRecognitionViaCache() {
//         // First call – may trigger lazy population of the internal map
//         assertTrue(ClassUtilities.isPrimitive(Integer.class), "First call should recognise Integer as primitive");
//         // Second call – map must already contain the entry
//         assertTrue(ClassUtilities.isPrimitive(Integer.class), "Second call should still recognise Integer as primitive");
//     }

    /**
     * Parameterized test that iterates over the arguments supplied by {@link #primitiveTestProvider()}.
     */
//     @ParameterizedTest(name = "[{index}] isPrimitive({0}) → {1}")
//     @MethodSource("primitiveTestProvider")
//     @DisplayName("isPrimitive – parameterized verification of primitives, wrappers and other types")
//     void testIsPrimitiveParameterized(Class<?> clazz, boolean expected) {
//         assertEquals(expected, ClassUtilities.isPrimitive(clazz), () -> "Unexpected result for class: " + clazz);
//     }
}