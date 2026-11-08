package com.cedarsoftware.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class ClassUtilities_getAllSupertypes_Test {

    /* ----------------------------------------------------------------------
     * Helper types used by the tests – placed inside the test class to avoid
     * any visibility issues with the production code.
     * ---------------------------------------------------------------------- */
    interface TestInterface {
    }

    interface SubInterface extends TestInterface {
    }

    static class TestClass {
    }

    static class SubClass extends TestClass implements TestInterface {
    }

    /**
     * Resolves a class name to a {@link Class} object.
     * <p>
     * Supports:
     * <ul>
     *   <li>Fully‑qualified class names (e.g. {@code java.util.List})</li>
     *   <li>Primitive type names (e.g. {@code int})</li>
     *   <li>Array types expressed with {@code []} (e.g. {@code java.lang.String[]})</li>
     * </ul>
     *
     * @param name the textual representation of the class
     * @return the corresponding {@link Class} object
     * @throws ClassNotFoundException if the class cannot be found
     */
    private Class<?> resolveClass(String name) throws ClassNotFoundException {
        // primitives
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
                // array syntax – translate "java.lang.String[]" to "[Ljava.lang.String;"
                if (name.endsWith("[]")) {
                    String component = name.substring(0, name.length() - 2);
                    switch(component) {
                        case "byte":
                            return Class.forName("[B");
                        case "short":
                            return Class.forName("[S");
                        case "int":
                            return Class.forName("[I");
                        case "long":
                            return Class.forName("[J");
                        case "float":
                            return Class.forName("[F");
                        case "double":
                            return Class.forName("[D");
                        case "char":
                            return Class.forName("[C");
                        case "boolean":
                            return Class.forName("[Z");
                        default:
                            return Class.forName("[L" + component + ";");
                    }
                }
                // regular reference type
                return Class.forName(name);
        }
    }

    /**
     * Returns the distance of {@code clazz} from {@code Object.class} in its
     * class hierarchy (not counting interfaces).  This method is used by other
     * utilities for sorting by depth.
     *
     * @param clazz the class whose depth is required
     * @return the number of steps from {@code clazz} to {@code Object.class};
     *         {@code 0} for {@code Object.class} itself; {@code -1} for
     *         {@code null}.
     */
    private static int getDepth(Class<?> clazz) {
        if (clazz == null) {
            return -1;
        }
        int depth = 0;
        Class<?> cur = clazz;
        while (cur != null && cur != Object.class) {
            cur = cur.getSuperclass();
            depth++;
        }
        return depth;
    }

    /**
     * Provides a stream of arguments for the parameterized test.
     * Each argument consists of:
     *   1. The class name to test.
     *   2. The list of expected super‑type names (including the class itself).
     *   3. A short description (used only for test naming).
     */
    private static Stream<Arguments> supertypeProvider() {
        return Stream.of(Arguments.of("java.util.ArrayList", List.of("java.util.ArrayList", "java.util.AbstractList", "java.util.AbstractCollection", "java.lang.Object", "java.util.List", "java.util.RandomAccess", "java.lang.Cloneable", "java.io.Serializable", "java.util.Collection", "java.lang.Iterable"), "Concrete class with superclass and multiple interfaces"), Arguments.of("java.lang.Object", List.of("java.lang.Object"), "Object has no superclass and no interfaces"), Arguments.of("java.lang.Runnable", List.of("java.lang.Runnable"), "Pure interface without super‑interfaces"), Arguments.of("java.util.List", List.of("java.util.List", "java.util.Collection", "java.lang.Iterable"), "Interface that extends other interfaces"), Arguments.of("java.util.AbstractList", List.of("java.util.AbstractList", "java.util.AbstractCollection", "java.lang.Object", "java.util.List", "java.util.Collection", "java.lang.Iterable", "java.util.RandomAccess"), "Abstract class with super‑class and interfaces"), Arguments.of("java.time.DayOfWeek", List.of("java.time.DayOfWeek", "java.lang.Enum", "java.lang.Object", "java.lang.Comparable", "java.io.Serializable"), "Enum type implements Comparable and Serializable"), Arguments.of("int", List.of("int"), "Primitive type has no super‑type"), Arguments.of("java.lang.Integer", List.of("java.lang.Integer", "java.lang.Number", "java.lang.Object", "java.lang.Comparable", "java.io.Serializable"), "Wrapper class extends Number and implements Comparable, Serializable"), Arguments.of("java.lang.String[]", List.of("java.lang.String[]", "java.lang.Object", "java.lang.Cloneable", "java.io.Serializable"), "Array class implements Cloneable and Serializable"), Arguments.of("java.util.HashMap", List.of("java.util.HashMap", "java.util.AbstractMap", "java.util.Map", "java.lang.Object", "java.lang.Cloneable", "java.io.Serializable"), "Concrete map implementation"));
    }

    @Test
    @DisplayName("null argument returns an empty set")
    void getAllSupertypes_NullArgument_ReturnsEmptySet() {
        Set<Class<?>> result = ClassUtilities.getAllSupertypes(null);
        assertNotNull(result, "Result should never be null");
        assertTrue(result.isEmpty(), "Result should be empty for null input");
    }

    @Test
    @DisplayName("Object class returns a set containing only Object")
    void getAllSupertypes_Object_ReturnsSelfOnly() {
        Set<Class<?>> expected = Set.of(Object.class);
        Set<Class<?>> actual = ClassUtilities.getAllSupertypes(Object.class);
        assertEquals(expected, actual, "Object should have no super‑type other than itself");
    }

    @Test
    @DisplayName("Simple subclass returns itself, its superclass and Object")
    void getAllSupertypes_SubClass_ReturnsHierarchy() {
        Set<Class<?>> expected = new HashSet<>(Arrays.asList(SubClass.class, TestClass.class, Object.class));
        Set<Class<?>> actual = ClassUtilities.getAllSupertypes(SubClass.class);
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Class implementing an interface includes the interface")
    void getAllSupertypes_ClassWithInterface_IncludesInterface() {
        Set<Class<?>> expected = new HashSet<>(Arrays.asList(SubClass.class, TestClass.class, TestInterface.class, Object.class));
        Set<Class<?>> actual = ClassUtilities.getAllSupertypes(SubClass.class);
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Interface extending another interface includes both")
    void getAllSupertypes_InterfaceExtendingAnother_IncludesBoth() {
        Set<Class<?>> expected = new HashSet<>(Arrays.asList(SubInterface.class, TestInterface.class));
        Set<Class<?>> actual = ClassUtilities.getAllSupertypes(SubInterface.class);
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Primitive type returns a set containing only the primitive class")
    void getAllSupertypes_Primitive_ReturnsSelfOnly() {
        Set<Class<?>> expected = Set.of(int.class);
        Set<Class<?>> actual = ClassUtilities.getAllSupertypes(int.class);
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Array type includes the array class and Object")
    void getAllSupertypes_Array_ReturnsArrayAndObject() {
        Set<Class<?>> expected = new HashSet<>(Arrays.asList(String[].class, Object.class));
        Set<Class<?>> actual = ClassUtilities.getAllSupertypes(String[].class);
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("supertypeProvider")
    @DisplayName("getAllSupertypes – various class categories")
    void getAllSupertypes_VariousInputs(String className, List<String> expectedNames, String description) throws ClassNotFoundException {
        // Resolve the class under test (handles primitives and arrays)
        Class<?> clazz = resolveClass(className);
        // Resolve expected class objects
        Set<Class<?>> expected = expectedNames.stream().map(name -> {
            try {
                return resolveClass(name);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toSet());
        // Invoke the method under test
        Set<Class<?>> actual = ClassUtilities.getAllSupertypes(clazz);
        // Verify that the returned set matches the expected set (order is irrelevant)
        assertEquals(expected, actual, () -> "Unexpected super‑type set for " + className + ". Expected: " + expected + " but got: " + actual);
    }
}
