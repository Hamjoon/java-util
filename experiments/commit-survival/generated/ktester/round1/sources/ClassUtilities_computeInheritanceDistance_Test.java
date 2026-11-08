package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Collection;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_computeInheritanceDistance_Test {

    interface SubInterface extends TestInterface {
    }

    static class TestClass {
    }

    static class SubClass extends TestClass implements TestInterface {
    }

    static class AnotherClass {
    }

    /* -----------------------------------------------------------
     *  Helper types used by the tests (some already referenced above)
     * ----------------------------------------------------------- */
    interface TestInterface {
    }

    private static Stream<Arguments> nullChecksProvider() {
        return Stream.of(Arguments.of("When source is null, method should return -1 regardless of destination.", null, ClassUtilities.class, -1), Arguments.of("When destination is null, method should return -1 regardless of source.", String.class, null, -1));
    }

    private static Stream<Arguments> equalityProvider() {
        return Stream.of(Arguments.of("Identical source and destination classes should yield distance 0.", String.class, String.class, 0), Arguments.of("Identical primitive types should also yield distance 0.", int.class, int.class, 0), Arguments.of("Identical wrapper types should also yield distance 0.", Integer.class, Integer.class, 0));
    }

    private static Stream<Arguments> primitiveHandlingProvider() {
        return Stream.of(Arguments.of("Two different primitive types – source is primitive, destination is primitive – should return -1.", int.class, long.class, -1), Arguments.of("Primitive source with a non‑primitive, non‑wrapper destination should return -1.", int.class, String.class, -1), Arguments.of("Primitive int source with its wrapper Integer destination should be considered matching and return 0.", int.class, Integer.class, 0), Arguments.of("Wrapper Integer source with primitive int destination should also be considered matching and return 0.", Integer.class, int.class, 0));
    }

    private static Stream<Arguments> superclassDistanceProvider() {
        return Stream.of(Arguments.of("Integer extends Number directly – distance should be 1.", Integer.class, Number.class, 1), Arguments.of("ArrayDeque → AbstractCollection → Object gives a distance of 2.", java.util.ArrayDeque.class, Object.class, 2));
    }

    private static Stream<Arguments> interfaceDistanceProvider() {
        return Stream.of(Arguments.of("ArrayList implements List directly – distance should be 1.", java.util.ArrayList.class, java.util.List.class, 1), Arguments.of("HashMap inherits AbstractMap (which implements Map); the interface is found at distance 2.", java.util.HashMap.class, java.util.Map.class, 2));
    }

    private static Stream<Arguments> noRelationshipProvider() {
        return Stream.of(Arguments.of("Unrelated classes should return -1 indicating no inheritance path.", String.class, Integer.class, -1));
    }

    private static Stream<Arguments> primitiveWrapperEdgeCasesProvider() {
        return Stream.of(Arguments.of("Primitive byte matches its wrapper Byte – distance should be 0.", byte.class, Byte.class, 0), Arguments.of("Wrapper Short matches its primitive short – distance should be 0.", Short.class, short.class, 0), Arguments.of("Primitive char matches its wrapper Character – distance should be 0.", char.class, Character.class, 0), Arguments.of("Wrapper Boolean matches its primitive boolean – distance should be 0.", Boolean.class, boolean.class, 0), Arguments.of("Primitive float does not wrap to Double – should return -1.", float.class, Double.class, -1));
    }

    private static Stream<Arguments> interfaceMultipleInheritanceProvider() {
        return Stream.of(Arguments.of("HashSet → AbstractSet → Set (interface) → Collection; first interface match (Set) is at distance 2, but Collection reachable via Set’s super‑interface, still distance 2.", java.util.HashSet.class, java.util.Collection.class, 2), Arguments.of("ConcurrentHashMap implements Map directly – distance should be 1.", java.util.concurrent.ConcurrentHashMap.class, java.util.Map.class, 1));
    }

    private static Stream<Arguments> superclassChainDepthProvider() {
        return Stream.of(Arguments.of("LinkedList → AbstractSequentialList → AbstractList → Object gives distance 3.", java.util.LinkedList.class, Object.class, 3), Arguments.of("FileInputStream extends InputStream directly – distance 1.", java.io.FileInputStream.class, java.io.InputStream.class, 1));
    }

    private static Stream<Arguments> interfaceVsSuperclassPreferenceProvider() {
        return Stream.of(Arguments.of("ArrayList extends AbstractList (superclass) directly – distance 1, even though it also implements List.", java.util.ArrayList.class, java.util.AbstractList.class, 1), Arguments.of("ArrayList → AbstractList (1) → AbstractCollection (2) → Object; Collection interface is implemented by AbstractCollection, reachable at distance 2.", java.util.ArrayList.class, java.util.Collection.class, 2));
    }

    private static Stream<Arguments> circularInterfaceSafetyProvider() {
        return Stream.of(Arguments.of("Identical interface classes should return 0, confirming no infinite loop.", Runnable.class, Runnable.class, 0));
    }

    private static Stream<Arguments> nonAssignablePrimitiveWrapperProvider() {
        return Stream.of(Arguments.of("Primitive int does not wrap to Long – should return -1.", int.class, Long.class, -1), Arguments.of("Wrapper Float does not match primitive double – should return -1.", Float.class, double.class, -1));
    }

    private static Stream<Arguments> objectAsDestinationWithPrimitiveSourceProvider() {
        return Stream.of(Arguments.of("Primitive source cannot be assigned to Object – should return -1.", int.class, Object.class, -1));
    }

    // -----------------------------------------------------------------
    private static Class<?> classFrom(String descriptor) {
        // Primitive shortcuts
        switch(descriptor) {
            case "int":
                return int.class;
            case "long":
                return long.class;
            case "short":
                return short.class;
            case "byte":
                return byte.class;
            case "char":
                return char.class;
            case "float":
                return float.class;
            case "double":
                return double.class;
            case "boolean":
                return boolean.class;
            case "void":
                return void.class;
        }
        // Normal class name ending with ".class"
        if (descriptor.endsWith(".class")) {
            String className = descriptor.substring(0, descriptor.length() - 6);
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Unable to load class: " + className, e);
            }
        }
        throw new IllegalArgumentException("Unsupported class descriptor: " + descriptor);
    }

    @Test
    void shouldReturnMinusOneWhenEitherArgumentIsNull() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, null));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, null));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, Object.class));
    }

    @Test
    void shouldReturnZeroWhenSourceEqualsDestination() {
        assertEquals(0, ClassUtilities.computeInheritanceDistance(String.class, String.class));
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, int.class));
        assertEquals(0, ClassUtilities.computeInheritanceDistance(Integer.class, Integer.class));
    }

    @Test
    void shouldHandleSuperclassRelationships() {
        assertEquals(1, ClassUtilities.computeInheritanceDistance(String.class, Object.class));
        assertEquals(1, ClassUtilities.computeInheritanceDistance(Integer.class, Number.class));
        assertEquals(2, ClassUtilities.computeInheritanceDistance(Integer.class, Object.class));
    }

    @Test
    void shouldHandleInterfaceImplementation() {
        assertEquals(1, ClassUtilities.computeInheritanceDistance(ArrayList.class, List.class));
        assertEquals(2, ClassUtilities.computeInheritanceDistance(HashSet.class, Collection.class));
    }

    @Test
    void shouldReturnMinusOneWhenNoInheritancePathExists() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, List.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(HashMap.class, List.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(TestClass.class, AnotherClass.class));
    }

    @Test
    void shouldTreatPrimitiveAndWrapperAsSameType() {
        // int ↔ Integer
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, Integer.class));
        assertEquals(0, ClassUtilities.computeInheritanceDistance(Integer.class, int.class));
        // double ↔ Double
        assertEquals(0, ClassUtilities.computeInheritanceDistance(double.class, Double.class));
        assertEquals(0, ClassUtilities.computeInheritanceDistance(Double.class, double.class));
    }

    @Test
    void shouldReturnMinusOneWhenPrimitiveAndNonWrapperAreMismatched() {
        // primitive to unrelated reference type
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, String.class));
        // wrapper to unrelated primitive
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(Integer.class, long.class));
    }

    @Test
    void shouldHandleInterfaceToInterfaceInheritance() {
        // SubInterface extends TestInterface
        assertEquals(1, ClassUtilities.computeInheritanceDistance(SubInterface.class, TestInterface.class));
        // reverse direction – not allowed
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(TestInterface.class, SubInterface.class));
    }

    @Test
    void shouldHandleClassToInterfaceDirectImplementation() {
        assertEquals(1, ClassUtilities.computeInheritanceDistance(SubClass.class, TestInterface.class));
    }

    @Test
    void shouldReturnMinusOneWhenClassDoesNotImplementInterface() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(TestClass.class, TestInterface.class));
    }

    @Test
    void shouldReturnMinusOneWhenInterfaceIsSourceAndClassIsDestination() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(TestInterface.class, TestClass.class));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullChecksProvider")
    void nullChecks(String description, Class<?> source, Class<?> destination, int expected) {
        assertEquals(expected, ClassUtilities.computeInheritanceDistance(source, destination), description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("equalityProvider")
    void equality(String description, Class<?> source, Class<?> destination, int expected) {
        assertEquals(expected, ClassUtilities.computeInheritanceDistance(source, destination), description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("primitiveHandlingProvider")
    void primitiveHandling(String description, Class<?> source, Class<?> destination, int expected) {
        assertEquals(expected, ClassUtilities.computeInheritanceDistance(source, destination), description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("superclassDistanceProvider")
    void superclassDistance(String description, Class<?> source, Class<?> destination, int expected) {
        assertEquals(expected, ClassUtilities.computeInheritanceDistance(source, destination), description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("interfaceDistanceProvider")
    void interfaceDistance(String description, Class<?> source, Class<?> destination, int expected) {
        assertEquals(expected, ClassUtilities.computeInheritanceDistance(source, destination), description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("noRelationshipProvider")
    void noRelationship(String description, Class<?> source, Class<?> destination, int expected) {
        assertEquals(expected, ClassUtilities.computeInheritanceDistance(source, destination), description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("primitiveWrapperEdgeCasesProvider")
    void primitiveWrapperEdgeCases(String description, Class<?> source, Class<?> destination, int expected) {
        assertEquals(expected, ClassUtilities.computeInheritanceDistance(source, destination), description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("interfaceMultipleInheritanceProvider")
    void interfaceMultipleInheritance(String description, Class<?> source, Class<?> destination, int expected) {
        assertEquals(expected, ClassUtilities.computeInheritanceDistance(source, destination), description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("superclassChainDepthProvider")
    void superclassChainDepth(String description, Class<?> source, Class<?> destination, int expected) {
        assertEquals(expected, ClassUtilities.computeInheritanceDistance(source, destination), description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("interfaceVsSuperclassPreferenceProvider")
    void interfaceVsSuperclassPreference(String description, Class<?> source, Class<?> destination, int expected) {
        assertEquals(expected, ClassUtilities.computeInheritanceDistance(source, destination), description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("circularInterfaceSafetyProvider")
    void circularInterfaceSafety(String description, Class<?> source, Class<?> destination, int expected) {
        assertEquals(expected, ClassUtilities.computeInheritanceDistance(source, destination), description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nonAssignablePrimitiveWrapperProvider")
    void nonAssignablePrimitiveWrapper(String description, Class<?> source, Class<?> destination, int expected) {
        assertEquals(expected, ClassUtilities.computeInheritanceDistance(source, destination), description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("objectAsDestinationWithPrimitiveSourceProvider")
    void objectAsDestinationWithPrimitiveSource(String description, Class<?> source, Class<?> destination, int expected) {
        assertEquals(expected, ClassUtilities.computeInheritanceDistance(source, destination), description);
    }
}
