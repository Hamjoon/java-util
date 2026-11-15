package com.cedarsoftware.util;

import java.lang.reflect.Field;
import java.util.Set;
// import org.junit.Test;
// import static org.junit.Assert.*;

// @ExtendWith(MockitoExtension.class)
class ClassUtilities_isPrimitive_Test {

    /**
     * Simple custom class used only for the dynamic prims set test.
     */
    private static class CustomClass {
        // no content needed
    }

    /**
     * Provides a mix of primitive, wrapper, and non‑primitive classes.
     */
//     private static Stream<Arguments> primitiveAndNonPrimitiveProvider() {
//         return Stream.of(Arguments.of(int.class, true), Arguments.of(boolean.class, true), Arguments.of(double.class, true), Arguments.of(void.class, true), Arguments.of(Integer.class, false), Arguments.of(Boolean.class, false), Arguments.of(Double.class, false), Arguments.of(String.class, false), Arguments.of(Object.class, false), Arguments.of(int[].class, false));
//     }

    /**
     * Provides classes that will be added to the internal {@code prims} set before invoking
     * {@link ClassUtilities#isPrimitive(Class)}. After the test, the class is removed to avoid
     * side‑effects on other tests.
     */
//     private static Stream<Arguments> dynamicPrimsProvider() {
//         return Stream.of(Arguments.of(CustomClass.class, true, "CustomClass added to prims set should be recognized as primitive"), Arguments.of(Integer.class, true, "Integer added to prims set should be recognized as primitive"));
//     }

    /**
     * Helper method to obtain the private static {@code prims} field from {@link ClassUtilities}.
     *
     * @return the mutable Set used internally by {@code ClassUtilities.isPrimitive}
     */
    @SuppressWarnings("unchecked")
    private static Set<Class<?>> getPrimsSet() throws ReflectiveOperationException {
        Field field = ClassUtilities.class.getDeclaredField("prims");
        field.setAccessible(true);
        return (Set<Class<?>>) field.get(null);
    }

//     @ParameterizedTest
//     @MethodSource("primitiveAndNonPrimitiveProvider")
//     @DisplayName("ClassUtilities.isPrimitive returns expected result for various classes")
//     void testIsPrimitive(Class<?> clazz, boolean expected) {
//         boolean actual = ClassUtilities.isPrimitive(clazz);
//         assertEquals(expected, actual, () -> "isPrimitive(" + clazz.getName() + ") expected " + expected + " but got " + actual);
//     }

//     @Test
//     @DisplayName("ClassUtilities.isPrimitive throws NPE when passed null")
//     void testIsPrimitive_NullArgument() {
//         ClassUtilities.isPrimitive(null);
//     }

//     @ParameterizedTest
//     @MethodSource("dynamicPrimsProvider")
//     @DisplayName("isPrimitive returns true for classes added to the private 'prims' set via reflection")
//     void testIsPrimitive_WithDynamicPrimsSet(Class<?> clazz, boolean expected, String description) throws Exception {
//         Set<Class<?>> prims = getPrimsSet();
//         // Ensure a clean state: remove the class if it already exists
//         prims.remove(clazz);
//         try {
//             // Add the class to the internal set
//             prims.add(clazz);
//             // Verify that isPrimitive now returns the expected value for the added class
//             boolean actual = ClassUtilities.isPrimitive(clazz);
//             assertEquals(expected, actual, () -> "After adding " + clazz.getName() + " to prims set, expected isPrimitive to be " + expected);
//         } finally {
//             // Clean up – remove the class so other tests are not affected
//             prims.remove(clazz);
//         }
//     }

//     @Test
//     public void testIsPrimitive_int() {
//         assertTrue("int.class should be primitive", ClassUtilities.isPrimitive(int.class));
//     }

//     @Test
//     public void testIsPrimitive_boolean() {
//         assertTrue("boolean.class should be primitive", ClassUtilities.isPrimitive(boolean.class));
//     }

//     @Test
//     public void testIsPrimitive_double() {
//         assertTrue("double.class should be primitive", ClassUtilities.isPrimitive(double.class));
//     }

//     @Test
//     public void testIsPrimitive_void() {
//         assertTrue("void.class should be primitive", ClassUtilities.isPrimitive(void.class));
//     }

//     @Test
//     public void testIsPrimitive_IntegerWrapper() {
//         assertFalse("Integer.class should NOT be primitive by default", ClassUtilities.isPrimitive(Integer.class));
//     }

//     @Test
//     public void testIsPrimitive_BooleanWrapper() {
//         assertFalse("Boolean.class should NOT be primitive by default", ClassUtilities.isPrimitive(Boolean.class));
//     }

//     @Test
//     public void testIsPrimitive_DoubleWrapper() {
//         assertFalse("Double.class should NOT be primitive by default", ClassUtilities.isPrimitive(Double.class));
//     }

//     @Test
//     public void testIsPrimitive_String() {
//         assertFalse("String.class should NOT be primitive", ClassUtilities.isPrimitive(String.class));
//     }

//     @Test
//     public void testIsPrimitive_Object() {
//         assertFalse("Object.class should NOT be primitive", ClassUtilities.isPrimitive(Object.class));
//     }

//     @Test
//     public void testIsPrimitive_intArray() {
//         assertFalse("int[].class should NOT be primitive", ClassUtilities.isPrimitive(int[].class));
//     }

//     @Test
//     public void testIsPrimitive_WithCustomClassAddedToPrims() throws Exception {
//         Set<Class<?>> prims = getPrimsSet();
//         // ensure a clean state
//         prims.remove(CustomClass.class);
//         try {
//             prims.add(CustomClass.class);
//             assertTrue("CustomClass added to prims set should be recognized as primitive", ClassUtilities.isPrimitive(CustomClass.class));
//         } finally {
//             prims.remove(CustomClass.class);
//         }
//     }

//     @Test
//     public void testIsPrimitive_WithIntegerClassAddedToPrims() throws Exception {
//         Set<Class<?>> prims = getPrimsSet();
//         // ensure a clean state
//         prims.remove(Integer.class);
//         try {
//             prims.add(Integer.class);
//             assertTrue("Integer added to prims set should be recognized as primitive", ClassUtilities.isPrimitive(Integer.class));
//         } finally {
//             prims.remove(Integer.class);
//         }
//     }
}