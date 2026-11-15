package com.cedarsoftware.util;

import java.util.Map;
import java.util.Collections;
import java.util.HashMap;

// @ExtendWith(MockitoExtension.class)
class ClassUtilities_toPrimitiveWrapperClass_Test {

    /**
     * Mapping from primitive types to their corresponding wrapper classes.
     */
    static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER;

    /* -------------------------------------------------------------
     *  Tests for primitive → wrapper conversion
     * ------------------------------------------------------------- */
//     private static Stream<Arguments> primitiveAndWrapperProvider() {
//         return Stream.of(Arguments.of(boolean.class, Boolean.class), Arguments.of(byte.class, Byte.class), Arguments.of(char.class, Character.class), Arguments.of(short.class, Short.class), Arguments.of(int.class, Integer.class), Arguments.of(long.class, Long.class), Arguments.of(float.class, Float.class), Arguments.of(double.class, Double.class), Arguments.of(void.class, Void.class));
//     }

    /**
     * Converts a primitive class to its corresponding wrapper class.
     *
     * @param primitiveClass the class to convert; must not be {@code null}
     * @return the wrapper class if {@code primitiveClass} is a primitive,
     *         otherwise the original class
     * @throws NullPointerException     if {@code primitiveClass} is {@code null}
     * @throws IllegalArgumentException if {@code primitiveClass} is a primitive
     *                                  type that is not present in the internal map
     */
    public static Class<?> toPrimitiveWrapperClass(Class<?> primitiveClass) {
        if (primitiveClass == null) {
            throw new NullPointerException("primitiveClass must not be null");
        }
        // Non‑primitive types are returned unchanged.
        if (!primitiveClass.isPrimitive()) {
            return primitiveClass;
        }
        // Primitive – look up the wrapper.
        Class<?> wrapper = PRIMITIVE_TO_WRAPPER.get(primitiveClass);
        if (wrapper == null) {
            // This should never happen for standard JDK primitives,
            // but we keep the check to satisfy the contract.
            throw new IllegalArgumentException("Unrecognized primitive type: " + primitiveClass.getName());
        }
        return wrapper;
    }

    /**
     * Determines whether one class is the wrapper type of the other.
     *
     * @param x first class to check; must not be {@code null}
     * @param y second class to check; must not be {@code null}
     * @return {@code true} if one class wraps the other, {@code false} otherwise
     * @throws NullPointerException if either argument is {@code null}
     */
    public static boolean doesOneWrapTheOther(Class<?> x, Class<?> y) {
        if (x == null || y == null) {
            throw new NullPointerException("Arguments must not be null");
        }
        if (x.isPrimitive()) {
            return toPrimitiveWrapperClass(x) == y;
        }
        if (y.isPrimitive()) {
            return toPrimitiveWrapperClass(y) == x;
        }
        return false;
    }

    /**
     * Helper used internally to determine whether two types represent the same
     * primitive type (either both primitives or one primitive and its wrapper).
     *
     * @param source      first type; must not be {@code null}
     * @param destination second type; must not be {@code null}
     * @return {@code true} if both represent the same primitive type
     */
    private static boolean areSamePrimitiveType(Class<?> source, Class<?> destination) {
        if (source == null || destination == null) {
            throw new NullPointerException("Arguments must not be null");
        }
        if (source.isPrimitive() && destination.isPrimitive()) {
            return source == destination;
        }
        if (source.isPrimitive()) {
            return toPrimitiveWrapperClass(source) == destination;
        }
        if (destination.isPrimitive()) {
            return toPrimitiveWrapperClass(destination) == source;
        }
        return false;
    }

//     @ParameterizedTest
//     @MethodSource("primitiveAndWrapperProvider")
//     void toPrimitiveWrapperClass_ConvertsPrimitiveToWrapper(Class<?> primitive, Class<?> expectedWrapper) {
//         Class<?> result = ClassUtilities.toPrimitiveWrapperClass(primitive);
//         Assertions.assertEquals(expectedWrapper, result);
//     }

    /* -------------------------------------------------------------
     *  Tests for non‑primitive input – should be returned unchanged
     * ------------------------------------------------------------- */
//     @Test
//     void toPrimitiveWrapperClass_NonPrimitive_ReturnsSameClass() {
//         Class<?>[] nonPrimitives = { String.class, Object.class, // wrapper itself
//         Integer.class, Void.class, ClassUtilities.class };
//         for (Class<?> clazz : nonPrimitives) {
//             Class<?> result = ClassUtilities.toPrimitiveWrapperClass(clazz);
//             Assertions.assertSame(clazz, result);
//         }
//     }

    /* -------------------------------------------------------------
     *  Null handling – the method does not guard against null,
     *  so a NullPointerException is expected.
     * ------------------------------------------------------------- */
//     @Test
//     void toPrimitiveWrapperClass_Null_ThrowsNPE() {
//         Assertions.assertThrows(NullPointerException.class, () -> ClassUtilities.toPrimitiveWrapperClass(null));
//     }

    /* -------------------------------------------------------------
     *  Defensive test – ensure an IllegalArgumentException is thrown
     *  for a primitive class that is not present in the internal map.
     *  This situation cannot normally happen with the JDK primitives,
     *  but we simulate it by using reflection to temporarily replace the
     *  internal map entry for a known primitive with null.
     * ------------------------------------------------------------- */
//     @Test
//     void toPrimitiveWrapperClass_PrimitiveMissingInMap_ThrowsIllegalArgumentException() throws Exception {
//         // Access the private static map via reflection
//         Field mapField = ClassUtilities.class.getDeclaredField("PRIMITIVE_TO_WRAPPER");
//         mapField.setAccessible(true);
//         @SuppressWarnings("unchecked")
//         Map<Class<?>, Class<?>> map = (Map<Class<?>, Class<?>>) mapField.get(null);
//         // Remove the entry for int.class temporarily
//         Class<?> removed = map.remove(int.class);
//         try {
//             Assertions.assertThrows(IllegalArgumentException.class, () -> ClassUtilities.toPrimitiveWrapperClass(int.class));
//         } finally {
//             // Restore the original mapping to avoid side‑effects for other tests
//             if (removed != null) {
//                 map.put(int.class, removed);
//             }
//         }
//     }
}