package com.cedarsoftware.util;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Arrays;
import java.util.Collection;
// import org.junit.Assert;
// import org.junit.Test;
// import org.junit.runner.RunWith;
// import org.junit.runners.Parameterized;

// @ExtendWith(MockitoExtension.class)
// @RunWith(Parameterized.class)
class ClassUtilities_toPrimitiveWrapperClass_Test {

//     @Parameterized.Parameter(0)
    public Class<?> primitive;

//     @Parameterized.Parameter(1)
    public Class<?> expectedWrapper;

    /**
     * Provides a stream of primitive types together with their expected wrapper classes.
     */
//     private static Stream<Arguments> primitiveWrapperProvider() {
//         return Arrays.asList(new Object[][] { { boolean.class, Boolean.class }, { byte.class, Byte.class }, { char.class, Character.class }, { short.class, Short.class }, { int.class, Integer.class }, { long.class, Long.class }, { float.class, Float.class }, { double.class, Double.class }, { void.class, Void.class } });
//     }

//     @ParameterizedTest
//     @MethodSource("primitiveWrapperProvider")
//     void toPrimitiveWrapperClass_returnsWrapperForPrimitive(Class<?> primitive, Class<?> expectedWrapper) {
//         Class<?> actual = ClassUtilities.toPrimitiveWrapperClass(primitive);
//         Assert.assertEquals("Wrapper for primitive " + primitive.getName() + " should be " + expectedWrapper.getName(), expectedWrapper, actual);
//     }

//     @Test
//     void toPrimitiveWrapperClass_returnsSameWhenNotPrimitive() {
//         // Non‑primitive classes should be returned unchanged
//         Class<?>[] nonPrimitives = { String.class, Object.class, Integer.class, Void.class, ClassUtilities.class, int[].class };
//         for (Class<?> cls : nonPrimitives) {
//             Class<?> result = ClassUtilities.toPrimitiveWrapperClass(cls);
//             Assert.assertSame("Non‑primitive class " + cls.getName() + " should be returned unchanged", cls, result);
//         }
//     }

//     @Test
//     void toPrimitiveWrapperClass_nullArgumentThrowsNullPointerException() {
//         ClassUtilities.toPrimitiveWrapperClass(null);
//     }

//     @Test
//     void toPrimitiveWrapperClass_missingMappingThrowsIllegalArgumentException() throws Exception {
//         // Access the private static map
//         Field mapField = ClassUtilities.class.getDeclaredField("primitiveToWrapper");
//         mapField.setAccessible(true);
//         @SuppressWarnings("unchecked")
//         Map<Class<?>, Class<?>> map = (Map<Class<?>, Class<?>>) mapField.get(null);
//         // Remove a mapping
//         Class<?> removed = map.remove(int.class);
//         try {
//             ClassUtilities.toPrimitiveWrapperClass(int.class);
//             Assert.fail("Expected IllegalArgumentException because the mapping was removed");
//         } catch (IllegalArgumentException expected) {
//             // expected path
//         } finally {
//             // Restore the original mapping
//             if (removed != null) {
//                 map.put(int.class, removed);
//             }
//         }
//     }

//     @Test
//     void toPrimitiveWrapperClass_nullInternalMapThrowsNullPointerException() throws Exception {
//         // Access the private static final field
//         Field mapField = ClassUtilities.class.getDeclaredField("primitiveToWrapper");
//         mapField.setAccessible(true);
//         @SuppressWarnings("unchecked")
//         Map<Class<?>, Class<?>> originalMap = (Map<Class<?>, Class<?>>) mapField.get(null);
//         // Break the internal state
//         mapField.set(null, null);
//         try {
//             ClassUtilities.toPrimitiveWrapperClass(boolean.class);
//             Assert.fail("Expected NullPointerException because internal map is null");
//         } catch (NullPointerException expected) {
//             // expected path
//         } finally {
//             // Restore original map reference
//             mapField.set(null, originalMap);
//         }
//     }
}