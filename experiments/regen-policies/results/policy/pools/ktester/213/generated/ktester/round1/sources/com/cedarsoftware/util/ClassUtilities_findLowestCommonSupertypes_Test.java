package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Stream;
import java.io.Serializable;
import org.junit.jupiter.params.provider.Arguments;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_findLowestCommonSupertypes_Test {

    /**
     * Simple holder for method‑source arguments to keep the code readable.
     */
    private static class ArgumentsHolder {

        final String classA;

        final String classB;

        // null for exception cases
        final List<String> expected;

        final String description;

        ArgumentsHolder(String classA, String classB, List<String> expected, String description) {
            this.classA = classA;
            this.classB = classB;
            this.expected = expected;
            this.description = description;
        }
    }

    /**
     * Resolves a class name used in the JSON test data to a {@link Class} instance.
     *
     * @param name fully‑qualified class name, primitive name (e.g. "int") or array notation (e.g. "java.lang.String[]")
     * @return the corresponding {@link Class} object, or {@code null} if the input name is {@code null}
     */
    private static Class<?> resolveClass(String name) {
        if (name == null) {
            return null;
        }
        try {
            // Primitive types
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
                    // Array types – recognise the trailing "[]"
                    if (name.endsWith("[]")) {
                        String componentName = name.substring(0, name.length() - 2);
                        Class<?> component = resolveClass(componentName);
                        // Create a zero‑length array to obtain the array class object
                        return Array.newInstance(component, 0).getClass();
                    }
                    // Normal reference type
                    return Class.forName(name);
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Unable to resolve class name: " + name, e);
        }
    }

    /**
     * Provides test data for successful (non‑exception) scenarios.
     * Each entry contains:
     *   - classA name
     *   - classB name
     *   - expected result expressed as a list of fully‑qualified class names (may be empty)
     *   - a short description (used only for readability in the test name)
     */
    private static Stream<ArgumentsHolder> successDataProvider() {
        return // Same class
        Stream.// Same class
        of(// Unrelated classes (only Object common)
        new ArgumentsHolder("java.lang.String", "java.lang.String", List.of("java.lang.String"), "Both arguments are the same concrete class."), // Subclass relationship
        new ArgumentsHolder("java.lang.String", "java.util.Date", Collections.emptyList(), "String and Date share only Object as a common ancestor."), // Interface hierarchy (List & Set)
        new ArgumentsHolder("java.lang.Number", "java.lang.Integer", List.of("java.lang.Number"), "Integer extends Number."), // Multiple implementations (ArrayList & LinkedList)
        new ArgumentsHolder("java.util.List", "java.util.Set", List.of("java.util.Collection"), "List and Set both extend Collection."), // Primitive vs Wrapper (int & Integer) – no common supertype beyond Object
        new ArgumentsHolder("java.util.ArrayList", "java.util.LinkedList", List.of("java.util.List"), "Both implement List."), // Enum types (Month & Thread.State)
        new ArgumentsHolder("int", "java.lang.Integer", Collections.emptyList(), "Primitive int and its wrapper Integer share only Object."), // Array types (String[] & Integer[])
        new ArgumentsHolder("java.time.Month", "java.lang.Thread$State", List.of("java.lang.Enum"), "Both are enums; common supertype is Enum."), // Subinterface relationship (NavigableSet & SortedSet)
        new ArgumentsHolder("java.lang.String[]", "java.lang.Integer[]", List.of("java.lang.Object[]"), "String[] and Integer[] share Object[] as supertype."), new ArgumentsHolder("java.util.NavigableSet", "java.util.SortedSet", List.of("java.util.SortedSet"), "NavigableSet extends SortedSet."));
    }

    /**
     * Provides test data for cases that must throw {@link NullPointerException}.
     */
    private static Stream<ArgumentsHolder> nullDataProvider() {
        return Stream.of(new ArgumentsHolder(null, "java.lang.String", null, "classA is null – should throw NPE."), new ArgumentsHolder("java.lang.Integer", null, null, "classB is null – should throw NPE."), new ArgumentsHolder(null, null, null, "Both arguments null – should throw NPE."));
    }

    /**
     * Provides test data for successful (non‑exception) scenarios.
     */
    private static Stream<Arguments> successProvider() {
        return Stream.of(Arguments.of("java.lang.String", "java.lang.String", List.of("java.lang.String"), "Both arguments are the same concrete class."), Arguments.of("java.lang.String", "java.util.Date", Collections.emptyList(), "String and Date share only Object as a common ancestor."), Arguments.of("java.lang.Number", "java.lang.Integer", List.of("java.lang.Number"), "Integer extends Number."), Arguments.of("java.util.List", "java.util.Set", List.of("java.util.Collection"), "List and Set both extend Collection."), Arguments.of("java.util.ArrayList", "java.util.LinkedList", List.of("java.util.List"), "Both implement List."), Arguments.of("int", "java.lang.Integer", Collections.emptyList(), "Primitive int and its wrapper Integer share only Object."), Arguments.of("java.time.Month", "java.lang.Thread$State", List.of("java.lang.Enum"), "Both are enums; common supertype is Enum."), Arguments.of("java.lang.String[]", "java.lang.Integer[]", List.of("java.lang.Object[]"), "String[] and Integer[] share Object[] as supertype."), Arguments.of("java.util.NavigableSet", "java.util.SortedSet", List.of("java.util.SortedSet"), "NavigableSet extends SortedSet."));
    }

    /**
     * Provides test data for cases that must throw {@link NullPointerException}.
     */
    private static Stream<Arguments> nullProvider() {
        return Stream.of(Arguments.of(null, "java.lang.String", null, "classA is null – should throw NPE."), Arguments.of("java.lang.Integer", null, null, "classB is null – should throw NPE."), Arguments.of(null, null, null, "Both arguments null – should throw NPE."));
    }

    @Test
    @DisplayName("Common interface between two List implementations")
    void testCommonInterfaceBetweenLists() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypes(ArrayList.class, LinkedList.class);
        assertNotNull(result);
        // Both share List and Collection (Object is excluded by the implementation)
        assertTrue(result.contains(List.class) || result.contains(Collection.class), "Result should contain List or Collection");
    }

    @Test
    @DisplayName("No common supertype other than Object yields an empty set")
    void testNoCommonSupertype() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypes(Runnable.class, Error.class);
        assertTrue(result.isEmpty(), "Only Object is common; the default exclusion set removes it, so the result must be empty");
    }

    @Test
    @DisplayName("Excluding default super types (Object, Serializable, Externalizable, Cloneable)")
    void testExcludingDefaults() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypes(String.class, StringBuilder.class);
        // Object, Serializable, Cloneable, Externalizable are excluded by default
        assertFalse(result.contains(Object.class), "Object should be excluded");
        assertFalse(result.contains(Serializable.class), "Serializable should be excluded");
        assertFalse(result.contains(Cloneable.class), "Cloneable should be excluded");
        // CharSequence is a legitimate common supertype
        assertTrue(result.contains(CharSequence.class), "CharSequence must be present");
    }

    @Test
    @DisplayName("Identical classes return a set containing only that class")
    void testIdenticalClasses() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypes(Integer.class, Integer.class);
        assertEquals(1, result.size(), "Only the identical class should be returned");
        assertTrue(result.contains(Integer.class), "Result must contain Integer.class");
    }

    @Test
    @DisplayName("Interface and its implementation")
    void testInterfaceAndImplementation() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypes(List.class, ArrayList.class);
        assertTrue(result.contains(List.class), "Result must contain the interface List");
    }

    @Test
    @DisplayName("Superclass and subclass")
    void testSuperclassAndSubclass() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypes(Number.class, Integer.class);
        // Number is a direct superclass of Integer, and it is not excluded
        assertTrue(result.contains(Number.class), "Result must contain Number");
    }

    @Test
    @DisplayName("Two unrelated classes with only Object in common")
    void testUnrelatedClasses() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypes(Thread.class, java.sql.Timestamp.class);
        assertTrue(result.isEmpty(), "Only Object is common, which is excluded, so the set should be empty");
    }

    /**
     * Parameterised test that verifies the returned set matches the expected class list.
     */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("successProvider")
    void testFindLowestCommonSupertypes_success(String classAName, String classBName, List<String> expectedClassNames, String description) {
        Class<?> classA = resolveClass(classAName);
        Class<?> classB = resolveClass(classBName);
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypes(classA, classB);
        assertNotNull(result, "Result set must never be null");
        // Convert expected class names to Class objects for comparison
        Set<Class<?>> expected = new HashSet<>();
        for (String name : expectedClassNames) {
            expected.add(resolveClass(name));
        }
        assertEquals(expected, result, () -> "Unexpected result for inputs [" + classAName + ", " + classBName + "]");
    }

    /**
     * Ensures that a {@code NullPointerException} is thrown when either argument is {@code null}.
     */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("nullProvider")
    void testFindLowestCommonSupertypes_nullArguments(String classAName, String classBName, List<String> ignored, String description) {
        Class<?> classA = resolveClass(classAName);
        Class<?> classB = resolveClass(classBName);
        assertThrows(NullPointerException.class, () -> ClassUtilities.findLowestCommonSupertypes(classA, classB), description);
    }
}
