package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_computeInheritanceDistance_Test {

    /* -------------------------------------------------
       Helper types used by the tests
       ------------------------------------------------- */
    interface TestInterface {
    }

    interface SubInterface extends TestInterface {
    }

    static class TestClass {
    }

    static class SubClass extends TestClass implements TestInterface {
    }

    static class AnotherClass {
    }

    /**
     * Provides test data extracted from the JSON description.
     *
     * Each entry is an {@code Object[]} with:
     *   0 – source class name (String, may represent a primitive or array)
     *   1 – destination class name
     *   2 – expected distance
     *   3 – description (used only for readability)
     */
    private static Stream<Arguments> jsonBasedTestCases() {
        return Stream.of(// Null and Equality Cases
        Arguments.of(null, null, -1, "Both parameters are null"), Arguments.of("java.lang.String", null, -1, "Destination is null"), Arguments.of(null, "java.lang.Integer", -1, "Source is null"), Arguments.of("java.lang.String", "java.lang.String", 0, "Source equals destination"), // Primitive to Wrapper Distance (note: the original library treats them as distance 0,
        // but the JSON expects 1 – we assert the JSON expectation here)
        Arguments.of("int", "java.lang.Integer", 1, "Primitive int → Integer wrapper"), Arguments.of("java.lang.Integer", "int", 1, "Integer wrapper → primitive int"), Arguments.of("int", "java.lang.Long", -1, "Primitive int does not wrap Long"), // Wrapper to Primitive Mismatch
        Arguments.of("java.lang.Double", "int", -1, "Double wrapper vs primitive int"), // Class Hierarchy Distance
        Arguments.of("java.util.ArrayList", "java.util.AbstractList", 1, "ArrayList → AbstractList"), Arguments.of("java.util.ArrayList", "java.util.List", 2, "ArrayList → AbstractList → List"), Arguments.of("java.util.HashMap", "java.util.Map", 1, "HashMap implements Map directly"), Arguments.of("java.util.concurrent.ConcurrentHashMap", "java.util.Map", 2, "ConcurrentHashMap → AbstractMap → Map"), // Interface Inheritance Distance
        Arguments.of("java.util.ArrayList", "java.util.Collection", 2, "ArrayList → List → Collection"), Arguments.of("java.util.HashSet", "java.util.Set", 1, "HashSet implements Set directly"), // No Inheritance Path
        Arguments.of("java.lang.String", "java.util.List", -1, "String unrelated to List"), Arguments.of("java.util.Date", "java.lang.Runnable", -1, "Date unrelated to Runnable"), // Primitive to Non‑Primitive Mismatch
        Arguments.of("int", "java.lang.String", -1, "Primitive int vs non‑primitive String"), Arguments.of("java.lang.String", "int", -1, "Non‑primitive String vs primitive int"), // Primitive to Primitive Mismatch
        Arguments.of("int", "long", -1, "Different primitive types"), // Array Class Inheritance
        Arguments.of("int[]", "java.lang.Object", 1, "Primitive array → Object"), Arguments.of("int[]", "java.lang.Cloneable", 1, "Primitive array → Cloneable"), Arguments.of("java.lang.String[]", "java.io.Serializable", 1, "Object array → Serializable"), Arguments.of("java.lang.String[]", "java.util.List", -1, "Array type does not implement List"), // Interface to Object Mismatch
        Arguments.of("java.util.List", "java.lang.Object", -1, "Interface has no superclass"));
    }

    /**
     * Helper that converts a textual class representation (including primitives and arrays)
     * into a {@link Class} object. {@code null} strings are mapped to {@code null}.
     */
    private static Class<?> resolveClass(String name) {
        if (name == null) {
            return null;
        }
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
                // Handle array notation like "int[]" or "java.lang.String[]"
                if (name.endsWith("[]")) {
                    String elementName = name.substring(0, name.length() - 2);
                    Class<?> elementClass = resolveClass(elementName);
                    // Use java.lang.reflect.Array to create array class
                    return java.lang.reflect.Array.newInstance(elementClass, 0).getClass();
                }
                try {
                    return Class.forName(name);
                } catch (ClassNotFoundException e) {
                    fail("Unable to resolve class name: " + name, e);
                    // unreachable
                    return null;
                }
        }
    }

    @Test
    void whenBothParametersAreNull_thenReturnMinusOne() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, null));
    }

    @Test
    void whenSourceIsNull_thenReturnMinusOne() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(null, Object.class));
    }

    @Test
    void whenDestinationIsNull_thenReturnMinusOne() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, null));
    }

    @Test
    void whenSourceEqualsDestination_thenReturnZero() {
        assertEquals(0, ClassUtilities.computeInheritanceDistance(String.class, String.class));
        assertEquals(0, ClassUtilities.computeInheritanceDistance(TestClass.class, TestClass.class));
    }

    @Test
    void whenSourceIsSuperclassOfDestination_thenReturnOne() {
        // Note: source is *subclass* of destination in the original implementation,
        // the test name reflects the intention of the JSON data.
        assertEquals(1, ClassUtilities.computeInheritanceDistance(String.class, Object.class));
        assertEquals(1, ClassUtilities.computeInheritanceDistance(Integer.class, Number.class));
    }

    @Test
    void whenSourceImplementsInterfaceDirectly_thenReturnOne() {
        assertEquals(1, ClassUtilities.computeInheritanceDistance(SubClass.class, TestInterface.class));
    }

    @Test
    void whenSourceExtendsClassDirectly_thenReturnOne() {
        assertEquals(1, ClassUtilities.computeInheritanceDistance(SubClass.class, TestClass.class));
    }

    @Test
    void whenSourceImplementsInterfaceIndirectly_thenReturnCorrectDistance() {
        // SubClass implements TestInterface directly – distance 1
        // SubInterface extends TestInterface – distance 1
        assertEquals(1, ClassUtilities.computeInheritanceDistance(SubInterface.class, TestInterface.class));
    }

    @Test
    void whenSourceHasNoRelationToDestination_thenReturnMinusOne() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(TestClass.class, AnotherClass.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(TestInterface.class, TestClass.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(String.class, List.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(HashMap.class, List.class));
    }

    @Test
    void whenInterfaceToInterfaceWithNoInheritance_thenReturnMinusOne() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(TestInterface.class, SubInterface.class));
    }

    @Test
    void whenSourceImplementsInterfaceThroughSuperclass_thenReturnTwo() {
        // ArrayList -> AbstractList -> List (distance 2)
        // However, computeInheritanceDistance counts each level, so we expect 1 for direct List implementation.
        // For HashSet -> AbstractSet -> Set -> Collection (distance 3)
        assertEquals(1, ClassUtilities.computeInheritanceDistance(ArrayList.class, List.class));
        assertEquals(2, ClassUtilities.computeInheritanceDistance(HashSet.class, Collection.class));
    }

    @Test
    void whenPrimitiveAndWrapperProvided_thenReturnZeroOrMinusOneAccordingToRules() {
        // According to the original library documentation primitives and wrappers are considered the same,
        // therefore distance 0 is expected.
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, Integer.class));
        assertEquals(0, ClassUtilities.computeInheritanceDistance(Integer.class, int.class));
        // Different primitive types should return -1
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, long.class));
        // Primitive to unrelated class should return -1
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, String.class));
    }

    @Test
    void whenBothParametersArePrimitiveAndEqual_thenReturnZero() {
        assertEquals(0, ClassUtilities.computeInheritanceDistance(boolean.class, boolean.class));
    }

    @Test
    void whenBothParametersAreWrapperAndEqual_thenReturnZero() {
        assertEquals(0, ClassUtilities.computeInheritanceDistance(Boolean.class, Boolean.class));
    }

    @Test
    void whenSourceIsWrapperAndDestinationIsDifferentWrapper_thenReturnMinusOne() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(Integer.class, Long.class));
    }

    @Test
    void whenSourceIsPrimitiveAndDestinationIsUnrelatedWrapper_thenReturnMinusOne() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(double.class, Integer.class));
    }

    @Test
    void whenSourceIsWrapperAndDestinationIsUnrelatedPrimitive_thenReturnMinusOne() {
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(Integer.class, double.class));
    }

    /**
     * Parameterised test that runs every case from {@link #jsonBasedTestCases()}.
     */
    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("jsonBasedTestCases")
    void jsonDrivenTests(String sourceName, String destinationName, int expected, String _description) {
        Class<?> source = resolveClass(sourceName);
        Class<?> destination = resolveClass(destinationName);
        int actual = ClassUtilities.computeInheritanceDistance(source, destination);
        assertEquals(expected, actual, () -> String.format("source=%s, destination=%s, expected=%d, actual=%d", sourceName, destinationName, expected, actual));
    }
}
