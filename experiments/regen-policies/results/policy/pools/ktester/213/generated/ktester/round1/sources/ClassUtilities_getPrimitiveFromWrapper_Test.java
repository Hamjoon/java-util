package com.cedarsoftware.util;

import java.util.Date;
import java.util.stream.Stream;
// import org.junit.Test;
// import static org.junit.Assert.*;

// @ExtendWith(MockitoExtension.class)
class ClassUtilities_getPrimitiveFromWrapper_Test {

    /**
     * Provide the standard Java wrapper → primitive pairs.
     */
//     private static Stream<Arguments> wrapperToPrimitiveProvider() {
//         return Stream.of(Arguments.of(Boolean.class, boolean.class), Arguments.of(Byte.class, byte.class), Arguments.of(Character.class, char.class), Arguments.of(Short.class, short.class), Arguments.of(Integer.class, int.class), Arguments.of(Long.class, long.class), Arguments.of(Float.class, float.class), Arguments.of(Double.class, double.class), Arguments.of(Void.class, void.class));
//     }

//     private static Stream<Arguments> arrayTypeProvider() {
//         return Stream.of(Arguments.of(Boolean[].class), Arguments.of(Integer[][].class), Arguments.of(String[].class));
//     }

    /**
     * Returns {@code true} if the supplied method has a {@code void} return type.
     *
     * @param method the method to inspect; must not be {@code null}
     * @return {@code true} if the method returns {@code void}, otherwise {@code false}
     */
//     public static boolean returnsVoid(Method method) {
//         if (method == null) {
//             throw new NullPointerException("method must not be null");
//         }
//         return method.getReturnType() == void.class;
//     }

    /**
     * Verify that each known wrapper class maps to its corresponding primitive type.
     */
//     @ParameterizedTest(name = "{index}: wrapper {0} -> primitive {1}")
//     @MethodSource("wrapperToPrimitiveProvider")
//     void testGetPrimitiveFromWrapper_KnownWrappers(Class<?> wrapper, Class<?> expectedPrimitive) {
//         Object[][] data = { { Boolean.class, boolean.class }, { Byte.class, byte.class }, { Character.class, char.class }, { Short.class, short.class }, { Integer.class, int.class }, { Long.class, long.class }, { Float.class, float.class }, { Double.class, double.class }, { Void.class, void.class } };
//         for (Object[] pair : data) {
//             Class<?> wrapper = (Class<?>) pair[0];
//             Class<?> expected = (Class<?>) pair[1];
//             Class<?> actual = ClassUtilities.getPrimitiveFromWrapper(wrapper);
//             assertEquals("Wrapper " + wrapper.getName() + " should map to primitive " + expected.getName(), expected, actual);
//         }
//     }

    /**
     * Passing a primitive class (or any non‑wrapper) should return {@code null}
     * because the internal map only contains wrapper keys.
     */
//     @Test
//     void testGetPrimitiveFromWrapper_NonWrapperReturnsNull() {
//         assertNull(ClassUtilities.getPrimitiveFromWrapper(int.class));
//         assertNull(ClassUtilities.getPrimitiveFromWrapper(String.class));
//         assertNull(ClassUtilities.getPrimitiveFromWrapper(Date.class));
//         // Void.class is a wrapper for the void primitive; the method is specified to return null for non‑wrapper primitives
//         assertNull(ClassUtilities.getPrimitiveFromWrapper(Void.class));
//     }

    /**
     * The method validates its argument is not {@code null}.  Verify that a
     * {@code NullPointerException} (or the exception thrown by Convention.throwIfNull)
     * is raised with a non‑null message.
     */
//     @Test
//     void testGetPrimitiveFromWrapper_NullArgument() {
//         try {
//             ClassUtilities.getPrimitiveFromWrapper(null);
//             fail("Expected IllegalArgumentException for null argument");
//         } catch (IllegalArgumentException ex) {
//             assertNotNull("Exception message should not be null", ex.getMessage());
//         }
//     }

    /**
     * Array types are not considered wrappers; they should also return {@code null}.
     */
//     @ParameterizedTest(name = "{index}: array type {0} -> null")
//     @MethodSource("arrayTypeProvider")
//     void testGetPrimitiveFromWrapper_ArrayTypesReturnNull(Class<?> arrayType) {
//         Class<?>[] arrayTypes = { Boolean[].class, Integer[][].class, String[].class };
//         for (Class<?> arrayType : arrayTypes) {
//             assertNull("Array type " + arrayType.getName() + " should return null", ClassUtilities.getPrimitiveFromWrapper(arrayType));
//         }
//     }
}