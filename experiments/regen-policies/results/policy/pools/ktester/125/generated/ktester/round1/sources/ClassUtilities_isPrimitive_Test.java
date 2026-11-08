package com.cedarsoftware.util;

import java.lang.reflect.Field;
// import org.junit.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

class ClassUtilities_isPrimitive_Test {

    //     @BeforeAll
//     static void setupBeforeAll() throws Exception {
//         // Access the private static field "prims" inside ClassUtilities
//         Field primsField = ClassUtilities.class.getDeclaredField("prims");
//         primsField.setAccessible(true);
//         @SuppressWarnings("unchecked")
//         Set<Class<?>> primsSet = (Set<Class<?>>) primsField.get(null);
//         primsSet.addAll(Arrays.asList(Boolean.class, Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class, Character.class, Void.class));
//     }

    private static Stream<Arguments> primitiveClasses() {
        return Stream.of(Arguments.of(boolean.class), Arguments.of(byte.class), Arguments.of(short.class), Arguments.of(int.class), Arguments.of(long.class), Arguments.of(float.class), Arguments.of(double.class), Arguments.of(char.class), Arguments.of(void.class));
    }

    private static Stream<Arguments> nonPrimitiveClasses() {
        return Stream.of(Arguments.of(String.class), Arguments.of(Object.class), Arguments.of(java.util.List.class), Arguments.of(java.util.Map.class), Arguments.of(int[].class), Arguments.of(java.util.Date.class));
    }

    private static Stream<Arguments> wrapperClasses() {
        return Stream.of(Arguments.of(Boolean.class), Arguments.of(Byte.class), Arguments.of(Short.class), Arguments.of(Integer.class), Arguments.of(Long.class), Arguments.of(Float.class), Arguments.of(Double.class), Arguments.of(Character.class), Arguments.of(Void.class));
    }

    @ParameterizedTest
    @MethodSource("primitiveClasses")
    void testIsPrimitive_PrimitiveClasses(Class<?> clazz) {
        Assertions.assertTrue(ClassUtilities.isPrimitive(clazz), "Expected primitive class " + clazz.getName() + " to be recognised as primitive");
    }

    @ParameterizedTest
    @MethodSource("nonPrimitiveClasses")
    void testIsPrimitive_NonPrimitiveClasses(Class<?> clazz) {
        Assertions.assertFalse(ClassUtilities.isPrimitive(clazz), "Expected non‑primitive class " + clazz.getName() + " to be reported as non‑primitive");
    }

    @Test
    void testIsPrimitive_NullArgument() {
        Assertions.assertThrows(NullPointerException.class, () -> ClassUtilities.isPrimitive(null), "Calling isPrimitive(null) should throw NullPointerException");
    }

    @ParameterizedTest
    @MethodSource("wrapperClasses")
    void testIsPrimitive_WrapperClasses(Class<?> clazz) {
        Assertions.assertTrue(ClassUtilities.isPrimitive(clazz), "Expected wrapper class " + clazz.getName() + " to be recognised as primitive");
    }
}