package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_computeInheritanceDistance_Test {

    interface SubInterface extends TestInterface {
    }

    static class TestClass {
    }

    static class SubClass extends TestClass implements TestInterface {
    }

    interface TestInterface {
    }

    private static Stream<Arguments> inheritanceDistanceProvider() {
        return Stream.of(// Class → direct superclass
        Arguments.of(java.util.ArrayList.class, java.util.List.class, 1), Arguments.of(java.util.LinkedList.class, java.util.List.class, 1), // Interface → super‑interface
        Arguments.of(TestInterface.class, TestInterface.class, 0), Arguments.of(SubInterface.class, TestInterface.class, 1), // Class implements interface
        Arguments.of(SubClass.class, TestInterface.class, 1), // Class → indirect superclass
        Arguments.of(java.util.ArrayList.class, java.util.Collection.class, 2), // Interface → indirect super‑interface
        Arguments.of(SubInterface.class, java.io.Serializable.class, 2), // No relationship
        Arguments.of(String.class, Integer.class, -1), // Primitive vs. non‑primitive (both non‑primitive case should be -1)
        Arguments.of(int.class, Integer.class, -1), // Wrapper → its primitive wrapper class (treated as unrelated)
        Arguments.of(Integer.class, int.class, -1));
    }

    /**
     * Returns the {@link Class} represented by the supplied name.
     *
     * <ul>
     *   <li>Primitive names (e.g. "int") are mapped to their primitive {@code Class}.</li>
     *   <li>All other names are resolved via {@link Class#forName(String)}.</li>
     * </ul>
     *
     * @param name class name as it appears in the JSON test data
     * @return the corresponding {@code Class} object
     */
    private static Class<?> getClassFromName(String name) {
        switch(name) {
            case "byte":
                return byte.class;
            case "short":
                return short.class;
            case "int":
                return int.class;
            case "long":
                return long.class;
            case "float":
                return float.class;
            case "double":
                return double.class;
            case "char":
                return char.class;
            case "boolean":
                return boolean.class;
            case "void":
                return void.class;
            default:
                try {
                    return Class.forName(name);
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException("Unable to resolve class name: " + name, e);
                }
        }
    }

    private static Stream<Arguments> nullParametersProvider() {
        return Stream.of(Arguments.of(null, "java.lang.String", -1), Arguments.of("java.lang.Integer", null, -1), Arguments.of(null, null, -1));
    }

    private static Stream<Arguments> sameClassProvider() {
        return Stream.of(Arguments.of("java.lang.String", "java.lang.String", 0), Arguments.of("int", "int", 0));
    }

    private static Stream<Arguments> primitiveTypesProvider() {
        return Stream.of(Arguments.of("int", "long", -1), Arguments.of("boolean", "boolean", 0));
    }

    private static Stream<Arguments> primitiveVsWrapperProvider() {
        return Stream.of(Arguments.of("int", "java.lang.Integer", -1), Arguments.of("java.lang.Double", "double", -1));
    }

    private static Stream<Arguments> superclassDistanceProvider() {
        return Stream.of(Arguments.of("java.lang.Integer", "java.lang.Number", 1), Arguments.of("java.lang.Integer", "java.lang.Object", 2), Arguments.of("java.util.HashMap", "java.util.Map", 1));
    }

    private static Stream<Arguments> interfaceImplementationProvider() {
        return Stream.of(Arguments.of("java.util.ArrayList", "java.util.List", 1), Arguments.of("java.util.ArrayList", "java.util.Collection", 2), Arguments.of("java.util.HashSet", "java.util.Collection", 1));
    }

    private static Stream<Arguments> unrelatedClassesProvider() {
        return Stream.of(Arguments.of("java.util.HashMap", "java.util.List", -1), Arguments.of("java.lang.Thread", "java.util.Collection", -1));
    }

    private static Stream<Arguments> interfaceSuperinterfaceProvider() {
        return Stream.of(Arguments.of("java.util.List", "java.util.Collection", 1), Arguments.of("java.util.SortedSet", "java.util.Set", 1));
    }

    private static Stream<Arguments> interfaceToClassProvider() {
        return Stream.of(Arguments.of("java.util.List", "java.lang.Object", -1), Arguments.of("java.util.Collection", "java.lang.Object", -1));
    }

    @Test
    @DisplayName("Both arguments null → -1")
    void bothNull() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, null));
    }

    @Test
    @DisplayName("Source null → -1")
    void sourceNull() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, String.class));
    }

    @Test
    @DisplayName("Destination null → -1")
    void destinationNull() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, null));
    }

    @Test
    @DisplayName("Primitive same type returns 0")
    void samePrimitive() {
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, int.class));
    }

    @Test
    @DisplayName("Different primitives return -1")
    void differentPrimitives() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, long.class));
    }

    @ParameterizedTest(name = "{index}: {0} → {1} distance = {2}")
    @MethodSource("inheritanceDistanceProvider")
    @DisplayName("Various inheritance distances")
    void testInheritanceDistances(Class<?> source, Class<?> destination, int expected) {
        assertEquals(expected, ClassUtilities.computeInheritanceDistance(source, destination), () -> String.format("Distance from %s to %s", source.getName(), destination.getName()));
    }

    @ParameterizedTest(name = "[{index}] source={0}, destination={1} → expected={2}")
    @MethodSource("nullParametersProvider")
    @DisplayName("Null Parameters → -1")
    void nullParameters(String sourceName, String destinationName, int expected) {
        Class<?> source = sourceName == null ? null : getClassFromName(sourceName);
        Class<?> destination = destinationName == null ? null : getClassFromName(destinationName);
        assertEquals(expected, ClassUtilities.computeInheritanceDistance(source, destination));
    }

    @ParameterizedTest(name = "[{index}] {0} → {1} = {2}")
    @MethodSource("primitiveTypesProvider")
    @DisplayName("Primitive Types")
    void primitiveTypes(String sourceName, String destinationName, int expected) {
        Class<?> source = getClassFromName(sourceName);
        Class<?> destination = getClassFromName(destinationName);
        assertEquals(expected, ClassUtilities.computeInheritanceDistance(source, destination));
    }

    @ParameterizedTest(name = "[{index}] {0} → {1} = {2}")
    @MethodSource("superclassDistanceProvider")
    @DisplayName("Superclass Distance")
    void superclassDistance(String sourceName, String destinationName, int expected) {
        Class<?> source = getClassFromName(sourceName);
        Class<?> destination = getClassFromName(destinationName);
        assertEquals(expected, ClassUtilities.computeInheritanceDistance(source, destination));
    }

    @ParameterizedTest(name = "[{index}] {0} → {1} = {2}")
    @MethodSource("interfaceImplementationProvider")
    @DisplayName("Interface Implementation Distance")
    void interfaceImplementationDistance(String sourceName, String destinationName, int expected) {
        Class<?> source = getClassFromName(sourceName);
        Class<?> destination = getClassFromName(destinationName);
        assertEquals(expected, ClassUtilities.computeInheritanceDistance(source, destination));
    }

    @ParameterizedTest(name = "[{index}] {0} ↔ {1} → {2}")
    @MethodSource("unrelatedClassesProvider")
    @DisplayName("Unrelated Classes")
    void unrelatedClasses(String sourceName, String destinationName, int expected) {
        Class<?> source = getClassFromName(sourceName);
        Class<?> destination = getClassFromName(destinationName);
        assertEquals(expected, ClassUtilities.computeInheritanceDistance(source, destination));
    }

    @ParameterizedTest(name = "[{index}] {0} → {1} = {2}")
    @MethodSource("interfaceSuperinterfaceProvider")
    @DisplayName("Interface Superinterface Distance")
    void interfaceSuperinterfaceDistance(String sourceName, String destinationName, int expected) {
        Class<?> source = getClassFromName(sourceName);
        Class<?> destination = getClassFromName(destinationName);
        assertEquals(expected, ClassUtilities.computeInheritanceDistance(source, destination));
    }

    @ParameterizedTest(name = "[{index}] {0} → {1} = {2}")
    @MethodSource("interfaceToClassProvider")
    @DisplayName("Interface to Class Relationship")
    void interfaceToClassRelationship(String sourceName, String destinationName, int expected) {
        Class<?> source = getClassFromName(sourceName);
        Class<?> destination = getClassFromName(destinationName);
        assertEquals(expected, ClassUtilities.computeInheritanceDistance(source, destination));
    }

//     @Test
//     @DisplayName("Repeated calls return consistent result (cache exercised)")
//     void repeatedCallsConsistency() {
//         int first = ClassUtilities.computeInheritanceDistance(ArrayList.class, List.class);
//         int second = ClassUtilities.computeInheritanceDistance(ArrayList.class, List.class);
//         assertEquals(1, first);
//         assertEquals(first, second, "Second invocation should return the same cached distance");
//     }
}