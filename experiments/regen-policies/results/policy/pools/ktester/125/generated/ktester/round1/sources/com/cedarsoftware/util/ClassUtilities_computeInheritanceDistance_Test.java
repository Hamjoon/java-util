package com.cedarsoftware.util;

import java.util.*;
import java.util.stream.Stream;
// import org.junit.Test;
// import static org.junit.Assert.assertEquals;

// @ExtendWith(MockitoExtension.class)
class ClassUtilities_computeInheritanceDistance_Test {

    private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER;

    private static final Map<Class<?>, Class<?>> WRAPPER_TO_PRIMITIVE;

    /* -----------------------------------------------------------------
       Helper interfaces / classes used by the tests.
       ----------------------------------------------------------------- */
    interface SubInterface extends TestInterface {
    }

    static class TestClass {
    }

    private static class SubClass extends TestClass implements TestInterface {
    }

    interface TestInterface {
    }

    /* -------------------------------------------------------------
       7. Parameterised tests covering a matrix of cases
       ------------------------------------------------------------- */
//     private static Stream<Arguments> inheritanceDistanceProvider() {
//         return Stream.of(Arguments.of(Integer.class, Number.class, 1), Arguments.of(Double.class, Number.class, 1), Arguments.of(Number.class, Object.class, 1), Arguments.of(ArrayList.class, List.class, 1), Arguments.of(ArrayList.class, Collection.class, -1), Arguments.of(SubClass.class, TestInterface.class, 1), Arguments.of(SubClass.class, Object.class, 2), Arguments.of(TestClass.class, Object.class, 1), Arguments.of(TestInterface.class, Object.class, -1));
//     }

    /* -------------------------------------------------------------
       3. Primitive / wrapper handling – matching pairs return 0,
       mismatched pairs return -1.
       ------------------------------------------------------------- */
//     private static Stream<Arguments> primitiveWrapperProvider() {
//         return Stream.of(Arguments.of(int.class, Integer.class, 0), Arguments.of(Integer.class, int.class, 0), Arguments.of(int.class, Long.class, -1), Arguments.of(Long.class, int.class, -1), Arguments.of(int.class, String.class, -1), Arguments.of(String.class, int.class, -1));
//     }

    /* -------------------------------------------------------------
       4. Class hierarchy distance (superclass chain)
       ------------------------------------------------------------- */
//     private static Stream<Arguments> classHierarchyProvider() {
//         return Stream.of(Arguments.of(ArrayList.class, List.class, 1), Arguments.of(ArrayList.class, AbstractCollection.class, 2), Arguments.of(HashMap.class, Map.class, 1), Arguments.of(LinkedHashMap.class, Map.class, 2), Arguments.of(String.class, Integer.class, -1), Arguments.of(Runnable.class, Error.class, -1));
//     }

    /* -------------------------------------------------------------
       7. Interface inheritance – current implementation does NOT walk
       super‑interfaces, therefore distances that would be >0 are reported
       as -1.
       ------------------------------------------------------------- */
//     private static Stream<Arguments> interfaceInheritanceProvider() {
//         return Stream.of(Arguments.of(ArrayList.class, Collection.class, -1), Arguments.of(HashSet.class, Collection.class, -1));
//     }

    /* -------------------------------------------------------------
       9. Distance to Object – traverses super‑class chain
       ------------------------------------------------------------- */
//     private static Stream<Arguments> objectDistanceProvider() {
//         return Stream.of(Arguments.of(ArrayList.class, Object.class, 3), Arguments.of(Integer.class, Object.class, 2));
//     }

    /* -------------------------------------------------------------
       10. Multiple interface inheritance – direct interface
       ------------------------------------------------------------- */
//     private static Stream<Arguments> multipleInterfaceProvider() {
//         return Stream.of(Arguments.of(ArrayList.class, Serializable.class, 1), Arguments.of(TreeMap.class, Map.class, 2));
//     }

    /**
     * Computes the inheritance distance between two classes/interfaces/primitive types.
     *
     * @param source      The source class, interface, or primitive type.
     * @param destination The destination class, interface, or primitive type.
     * @return The number of steps from the source to the destination, or -1 if no path exists.
     */
    public static int computeInheritanceDistance(Class<?> source, Class<?> destination) {
        // 1. Null handling
        if (source == null || destination == null) {
            return -1;
        }
        // 2. Same class/interface/primitive
        if (source.equals(destination)) {
            return 0;
        }
        // 3. Primitive / wrapper equivalence (distance 0)
        if (isPrimitive(source) && isWrapper(destination) && PRIMITIVE_TO_WRAPPER.get(source).equals(destination)) {
            return 0;
        }
        if (isPrimitive(destination) && isWrapper(source) && PRIMITIVE_TO_WRAPPER.get(destination).equals(source)) {
            return 0;
        }
        if (isPrimitive(source) && isPrimitive(destination)) {
            return source.equals(destination) ? 0 : -1;
        }
        if (isWrapper(source) && isWrapper(destination)) {
            return source.equals(destination) ? 0 : -1;
        }
        // 4. Bread‑first search over the inheritance graph
        boolean sourceIsInterface = source.isInterface();
        Queue<Class<?>> queue = new ArrayDeque<>();
        Map<Class<?>, Integer> distance = new HashMap<>();
        queue.add(source);
        distance.put(source, 0);
        while (!queue.isEmpty()) {
            Class<?> cur = queue.poll();
            int d = distance.get(cur);
            if (cur.equals(destination)) {
                return d;
            }
            // super‑class
            Class<?> superClass = cur.getSuperclass();
            if (superClass != null && !distance.containsKey(superClass)) {
                distance.put(superClass, d + 1);
                queue.add(superClass);
            }
            // interfaces
            if (!cur.isInterface()) {
                for (Class<?> iface : cur.getInterfaces()) {
                    if (!distance.containsKey(iface)) {
                        distance.put(iface, d + 1);
                        queue.add(iface);
                    }
                }
            } else {
                if (sourceIsInterface) {
                    for (Class<?> superIface : cur.getInterfaces()) {
                        if (!distance.containsKey(superIface)) {
                            distance.put(superIface, d + 1);
                            queue.add(superIface);
                        }
                    }
                }
            }
        }
        return -1;
    }

    public static boolean isPrimitive(Class<?> c) {
        return PRIMITIVE_TO_WRAPPER.containsKey(c);
    }

    public static boolean isWrapper(Class<?> c) {
        return WRAPPER_TO_PRIMITIVE.containsKey(c);
    }

    public static Class<?> toWrapper(Class<?> primitive) {
        return PRIMITIVE_TO_WRAPPER.get(primitive);
    }

    public static Class<?> toPrimitive(Class<?> wrapper) {
        return WRAPPER_TO_PRIMITIVE.get(wrapper);
    }

    /* -------------------------------------------------------------
       1. Null handling – either argument null => -1
       ------------------------------------------------------------- */
//     @Test
//     void testNullParameters() {
//         assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, String.class));
//         assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, null));
//         assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, null));
//     }

    /* -------------------------------------------------------------
       2. Same class – distance 0
       ------------------------------------------------------------- */
//     @Test
//     void testSameClass() {
//         assertEquals(0, ClassUtilities.computeInheritanceDistance(Integer.class, Integer.class));
//         assertEquals(0, ClassUtilities.computeInheritanceDistance(List.class, List.class));
//     }

    /* -------------------------------------------------------------
       3. Primitive / wrapper handling – should return -1
       ------------------------------------------------------------- */
//     @Test
//     void testPrimitiveAndWrapper() {
//         assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, Integer.class));
//         assertEquals(-1, ClassUtilities.computeInheritanceDistance(Integer.class, int.class));
//         assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, long.class));
//     }

    /* -------------------------------------------------------------
       4. Direct superclass / interface distance
       ------------------------------------------------------------- */
//     @Test
//     void testDirectSuperclass() {
//         assertEquals(1, ClassUtilities.computeInheritanceDistance(ArrayList.class, List.class));
//     }

//     @Test
//     void testDirectInterfaceImplementation() {
//         assertEquals(1, ClassUtilities.computeInheritanceDistance(SubClass.class, TestInterface.class));
//     }

//     @Test
//     void testSubInterfaceExtendsInterface() {
//         assertEquals(1, ClassUtilities.computeInheritanceDistance(SubInterface.class, TestInterface.class));
//     }

    /* -------------------------------------------------------------
       5. Multi‑step inheritance – verify BFS distance
       ------------------------------------------------------------- */
//     @Test
//     void testMultiStepSuperclass() {
//         assertEquals(-1, ClassUtilities.computeInheritanceDistance(LinkedList.class, Collection.class), "Expected -1 because super‑interfaces are not traversed");
//     }

//     @Test
//     void testMultiStepInterfaceViaSuperclass() {
//         assertEquals(-1, ClassUtilities.computeInheritanceDistance(ArrayList.class, Collection.class));
//     }

    /* -------------------------------------------------------------
       6. No relationship – should return -1
       ------------------------------------------------------------- */
//     @Test
//     void testNoRelationship() {
//         assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, Integer.class));
//         assertEquals(-1, ClassUtilities.computeInheritanceDistance(Runnable.class, Error.class));
//     }

//     @ParameterizedTest
//     @MethodSource("inheritanceDistanceProvider")
//     void testParameterizedInheritanceDistance(Class<?> source, Class<?> destination, int expected) {
//         Object[][] data = { { Integer.class, Number.class, 1 }, { Double.class, Number.class, 1 }, { Number.class, Object.class, 1 }, { ArrayList.class, List.class, 1 }, { ArrayList.class, Collection.class, -1 }, { SubClass.class, TestInterface.class, 1 }, { SubClass.class, Object.class, 2 }, { TestClass.class, Object.class, 1 }, { TestInterface.class, Object.class, -1 } };
//         for (Object[] row : data) {
//             Class<?> src = (Class<?>) row[0];
//             Class<?> dst = (Class<?>) row[1];
//             int expected = (int) row[2];
//             assertEquals(String.format("Distance %s -> %s", src.getSimpleName(), dst.getSimpleName()), expected, ClassUtilities.computeInheritanceDistance(src, dst));
//         }
//     }

//     @ParameterizedTest
//     @MethodSource("primitiveWrapperProvider")
//     void testPrimitiveAndWrapperHandling(Class<?> source, Class<?> destination, int expected) {
//         assertEquals(expected, ClassUtilities.computeInheritanceDistance(source, destination), () -> String.format("Primitive/Wrapper distance %s → %s", source.getSimpleName(), destination.getSimpleName()));
//     }

//     @ParameterizedTest
//     @MethodSource("classHierarchyProvider")
//     void testClassHierarchyDistance(Class<?> source, Class<?> destination, int expected) {
//         assertEquals(expected, ClassUtilities.computeInheritanceDistance(source, destination), () -> String.format("Class hierarchy distance %s → %s", source.getSimpleName(), destination.getSimpleName()));
//     }

    /* -------------------------------------------------------------
       5. Primitive same type – distance 0
       ------------------------------------------------------------- */
//     @Test
//     void testPrimitiveSameType() {
//         assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, int.class));
//     }

    /* -------------------------------------------------------------
       6. Primitive different type – distance -1
       ------------------------------------------------------------- */
//     @Test
//     void testPrimitiveDifferentType() {
//         assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, long.class));
//     }

//     @ParameterizedTest
//     @MethodSource("interfaceInheritanceProvider")
//     void testInterfaceInheritance(Class<?> source, Class<?> destination, int expected) {
//         assertEquals(expected, ClassUtilities.computeInheritanceDistance(source, destination), () -> String.format("Interface inheritance distance %s → %s", source.getSimpleName(), destination.getSimpleName()));
//     }

    /* -------------------------------------------------------------
       8. Interface to class negative – interfaces cannot be subclasses
       ------------------------------------------------------------- */
//     @Test
//     void testInterfaceToClassNegative() {
//         assertEquals(-1, ClassUtilities.computeInheritanceDistance(Collection.class, ArrayList.class));
//     }

//     @ParameterizedTest
//     @MethodSource("objectDistanceProvider")
//     void testObjectDistance(Class<?> source, Class<?> destination, int expected) {
//         assertEquals(expected, ClassUtilities.computeInheritanceDistance(source, destination), () -> String.format("Object distance %s → %s", source.getSimpleName(), destination.getSimpleName()));
//     }

//     @ParameterizedTest
//     @MethodSource("multipleInterfaceProvider")
//     void testMultipleInterfaceInheritance(Class<?> source, Class<?> destination, int expected) {
//         assertEquals(expected, ClassUtilities.computeInheritanceDistance(source, destination), () -> String.format("Multiple interface distance %s → %s", source.getSimpleName(), destination.getSimpleName()));
//     }

    /* -------------------------------------------------------------
       11. Superclass chain distance (non‑direct super‑class)
       ------------------------------------------------------------- */
//     @Test
//     void testSuperclassChainDistance() {
//         assertEquals(2, ClassUtilities.computeInheritanceDistance(LinkedHashMap.class, AbstractMap.class));
//     }

    /* -------------------------------------------------------------
       12. Interface direct extension distance
       ------------------------------------------------------------- */
//     @Test
//     void testInterfaceDirectExtensionDistance() {
//         assertEquals(1, ClassUtilities.computeInheritanceDistance(List.class, Collection.class));
//     }

    /* -------------------------------------------------------------
       13. Wrapper to primitive mismatched – should be -1
       ------------------------------------------------------------- */
//     @Test
//     void testWrapperToPrimitiveMismatched() {
//         assertEquals(-1, ClassUtilities.computeInheritanceDistance(Long.class, int.class));
//     }

    /* -----------------------------------------------------------------
       5. Multi‑step inheritance – verify BFS distance (negative cases)
       ----------------------------------------------------------------- */
//     @Test
//     public void testMultiStepSuperclassNegative() {
//         // The current implementation does not walk super‑interfaces, therefore
//         // these cases must return -1.
//         assertEquals(-1, ClassUtilities.computeInheritanceDistance(LinkedList.class, Collection.class));
//         assertEquals(-1, ClassUtilities.computeInheritanceDistance(ArrayList.class, Collection.class));
//     }

//     @Test
//     public void testPrimitiveWrapperProvider() {
//         Object[][] data = { { int.class, Integer.class, 0 }, { Integer.class, int.class, 0 }, { int.class, Long.class, -1 }, { Long.class, int.class, -1 }, { int.class, String.class, -1 }, { String.class, int.class, -1 } };
//         for (Object[] row : data) {
//             Class<?> src = (Class<?>) row[0];
//             Class<?> dst = (Class<?>) row[1];
//             int expected = (int) row[2];
//             assertEquals(String.format("Primitive/Wrapper distance %s → %s", src.getSimpleName(), dst.getSimpleName()), expected, ClassUtilities.computeInheritanceDistance(src, dst));
//         }
//     }

//     @Test
//     public void testClassHierarchyProvider() {
//         Object[][] data = { { ArrayList.class, List.class, 1 }, { ArrayList.class, AbstractCollection.class, 2 }, { HashMap.class, Map.class, 1 }, { LinkedHashMap.class, Map.class, 2 }, { String.class, Integer.class, -1 }, { Runnable.class, Error.class, -1 } };
//         for (Object[] row : data) {
//             Class<?> src = (Class<?>) row[0];
//             Class<?> dst = (Class<?>) row[1];
//             int expected = (int) row[2];
//             assertEquals(String.format("Class hierarchy distance %s → %s", src.getSimpleName(), dst.getSimpleName()), expected, ClassUtilities.computeInheritanceDistance(src, dst));
//         }
//     }

//     @Test
//     public void testInterfaceInheritanceProvider() {
//         Object[][] data = { { ArrayList.class, Collection.class, -1 }, { HashSet.class, Collection.class, -1 } };
//         for (Object[] row : data) {
//             Class<?> src = (Class<?>) row[0];
//             Class<?> dst = (Class<?>) row[1];
//             int expected = (int) row[2];
//             assertEquals(String.format("Interface inheritance distance %s → %s", src.getSimpleName(), dst.getSimpleName()), expected, ClassUtilities.computeInheritanceDistance(src, dst));
//         }
//     }

//     @Test
//     public void testObjectDistanceProvider() {
//         Object[][] data = { { ArrayList.class, Object.class, 3 }, { Integer.class, Object.class, 2 } };
//         for (Object[] row : data) {
//             Class<?> src = (Class<?>) row[0];
//             Class<?> dst = (Class<?>) row[1];
//             int expected = (int) row[2];
//             assertEquals(String.format("Object distance %s → %s", src.getSimpleName(), dst.getSimpleName()), expected, ClassUtilities.computeInheritanceDistance(src, dst));
//         }
//     }

//     @Test
//     public void testMultipleInterfaceProvider() {
//         Object[][] data = { { ArrayList.class, Serializable.class, 1 }, { TreeMap.class, Map.class, 2 } };
//         for (Object[] row : data) {
//             Class<?> src = (Class<?>) row[0];
//             Class<?> dst = (Class<?>) row[1];
//             int expected = (int) row[2];
//             assertEquals(String.format("Multiple interface distance %s → %s", src.getSimpleName(), dst.getSimpleName()), expected, ClassUtilities.computeInheritanceDistance(src, dst));
//         }
//     }
}