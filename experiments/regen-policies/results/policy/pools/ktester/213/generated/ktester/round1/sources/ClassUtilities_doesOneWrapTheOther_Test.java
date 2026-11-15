package com.cedarsoftware.util;

// import org.junit.Test;
// import org.junit.runner.RunWith;
// import org.junit.runners.JUnit4;

// @ExtendWith(MockitoExtension.class)
// @RunWith(JUnit4.class)
class ClassUtilities_doesOneWrapTheOther_Test {

    /**
     * Supplies a mix of primitive / wrapper pairs and unrelated classes.
     */
//     private static Stream<Arguments> provideWrapperPairs() {
//         return Stream.of(// Positive mappings – primitive → its wrapper (should be true)
//         Arguments.of(int.class, Integer.class, true), Arguments.of(boolean.class, Boolean.class, true), Arguments.of(char.class, Character.class, true), Arguments.of(byte.class, Byte.class, true), Arguments.of(short.class, Short.class, true), Arguments.of(long.class, Long.class, true), Arguments.of(float.class, Float.class, true), Arguments.of(double.class, Double.class, true), Arguments.of(void.class, Void.class, true), // Negative mappings – wrapper → primitive (no reverse entry)
//         Arguments.of(Integer.class, int.class, false), Arguments.of(Boolean.class, boolean.class, false), Arguments.of(Character.class, char.class, false), Arguments.of(Byte.class, byte.class, false), Arguments.of(Short.class, short.class, false), Arguments.of(Long.class, long.class, false), Arguments.of(Float.class, float.class, false), Arguments.of(Double.class, double.class, false), // Negative mappings – same type (primitive‑primitive or wrapper‑wrapper)
//         Arguments.of(int.class, int.class, false), Arguments.of(Integer.class, Integer.class, false), // Negative mappings – unrelated classes
//         Arguments.of(String.class, String.class, false), Arguments.of(String.class, Integer.class, false), Arguments.of(List.class, Map.class, false), // Negative mappings – primitive to unrelated wrapper
//         Arguments.of(int.class, Long.class, false), Arguments.of(int.class, String.class, false), // Negative mappings – different primitives / different wrappers
//         Arguments.of(int.class, long.class, false), Arguments.of(Integer.class, Long.class, false), // Negative mappings – unknown/custom class (no entry in wrapperMap)
//         Arguments.of(java.util.ArrayList.class, java.util.ArrayList.class, false), // Null handling – one argument null (should be false)
//         Arguments.of(null, Integer.class, false), // Null handling – both arguments null (should be true because
//         // wrapperMap.get(null) returns null and null == null)
//         Arguments.of(null, null, true));
//     }

    /* ----------------------------------------------------------------------
     *  Defensive tests – verify behaviour with null arguments.
     * ---------------------------------------------------------------------- */
//     @Test
//     void testDoesOneWrapTheOther_NullFirstArgument() {
//         // The method is documented to throw NullPointerException when either
//         // argument is null, but the original test only invoked it.  Keeping the
//         // same behaviour – the test will fail if an unexpected exception is
//         // thrown.
//         ClassUtilities.doesOneWrapTheOther(null, Integer.class);
//     }

//     @Test
//     void testDoesOneWrapTheOther_NullSecondArgument() {
//         ClassUtilities.doesOneWrapTheOther(int.class, null);
//     }

//     @Test
//     void testDoesOneWrapTheOther_BothNull() {
//         ClassUtilities.doesOneWrapTheOther(null, null);
//     }
}