package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_findLowestCommonSupertype_Test {

    interface Parent {
    }

//     interface Parent {
    }

    interface ChildA extends Parent {
    }

    interface ChildB extends Parent {
    }

    // Simple hierarchy used in several tests
//     interface Parent {
    }

//     interface ChildA extends Parent {
    }

//     interface ChildB extends Parent {
    }

    /* -----------------------------------------------------------
     *  Helper types used in the parameterised tests (inner interfaces / classes)
     * ----------------------------------------------------------- */
    interface TestInterface {
    }

    interface SubInterface extends TestInterface {
    }

    static class TestClass {
    }

    static class SubClass extends TestClass implements TestInterface {
    }

    // Simple hierarchy used in several tests
//     interface Parent {
    }

//     interface ChildA extends Parent {
    }

//     interface ChildB extends Parent {
    }

    private static Stream<Arguments> provideCommonSupertypeScenarios() {
        return Stream.of(// two concrete collection classes – lowest common supertype is List
        Arguments.of("List is common for ArrayList & LinkedList", ArrayList.class, LinkedList.class, List.class), // interface inheritance – TestInterface is the lowest common supertype
        Arguments.of("SubInterface & TestInterface share TestInterface", SubInterface.class, TestInterface.class, TestInterface.class), // class implements an interface – the interface is the common supertype
        Arguments.of("SubClass implements TestInterface", SubClass.class, TestInterface.class, TestInterface.class), // primitive vs wrapper – wrapper is considered the common supertype
        Arguments.of("int primitive and Integer wrapper", int.class, Integer.class, Integer.class), // two unrelated types – only Object in common → null per contract
        Arguments.of("Runnable & Error have no common supertype other than Object", Runnable.class, Error.class, null), // null arguments – should return null
        Arguments.of("null & String.class", null, String.class, null), Arguments.of("String.class & null", String.class, null, null), Arguments.of("null & null", null, null, null));
    }

    /**
     * Builds a {@link Stream} of {@link Arguments} covering every case described
     * in the JSON test matrix as well as a few additional sanity checks.
     */
    private static Stream<Arguments> provideAllScenarios() {
        return Stream.of(// Same class
        Arguments.of("Both parameters refer to the same class (String)", resolveClass("java.lang.String"), resolveClass("java.lang.String"), resolveClass("java.lang.String")), // Subclass / Superclass (ArrayList implements List)
        Arguments.of("ArrayList implements List – List is the lowest common supertype", resolveClass("java.util.ArrayList"), resolveClass("java.util.List"), resolveClass("java.util.List")), // Unrelated concrete classes (String & Integer) – only Object is common
        Arguments.of("String & Integer share only Object as a common ancestor", resolveClass("java.lang.String"), resolveClass("java.lang.Integer"), resolveClass("java.lang.Object")), // Class implements Interface (HashMap & Map)
        Arguments.of("HashMap implements Map – Map is the lowest common supertype", resolveClass("java.util.HashMap"), resolveClass("java.util.Map"), resolveClass("java.util.Map")), // Two interfaces with a common super‑interface (List & Set → Collection)
        Arguments.of("List & Set both extend Collection – Collection is the lowest common supertype", resolveClass("java.util.List"), resolveClass("java.util.Set"), resolveClass("java.util.Collection")), // Primitive types – no common reference type
        Arguments.of("int & double – primitives have no common supertype", resolveClass("int"), resolveClass("double"), null), // Primitive and its wrapper – not related in the class hierarchy
        Arguments.of("int & Integer – primitive and wrapper are unrelated", resolveClass("int"), resolveClass("java.lang.Integer"), null), // Null first parameter
        Arguments.of("null & String – cannot determine common supertype", null, resolveClass("java.lang.String"), null), // Both parameters null
        Arguments.of("null & null – both arguments are null", null, null, null), // Array types – String[] extends Object[]
        Arguments.of("String[] & Object[] – Object[] is the lowest common supertype", resolveClass("java.lang.String[]"), resolveClass("java.lang.Object[]"), resolveClass("java.lang.Object[]")), // Array and plain Object – all arrays are subclasses of Object
        Arguments.of("String[] & Object – Object is the lowest common supertype", resolveClass("java.lang.String[]"), resolveClass("java.lang.Object"), resolveClass("java.lang.Object")), // Enum types – both extend java.lang.Enum
        Arguments.of("DayOfWeek & Month – both extend Enum", resolveClass("java.time.DayOfWeek"), resolveClass("java.time.Month"), resolveClass("java.lang.Enum")), // Wrapper and its superclass (Integer & Number)
        Arguments.of("Integer & Number – Number is the lowest common supertype", resolveClass("java.lang.Integer"), resolveClass("java.lang.Number"), resolveClass("java.lang.Number")), // Abstract class and concrete subclass (AbstractList & ArrayList)
        Arguments.of("AbstractList & ArrayList – AbstractList is the lowest common supertype", resolveClass("java.util.AbstractList"), resolveClass("java.util.ArrayList"), resolveClass("java.util.AbstractList")), // Interface extends another interface (Collection & List)
        Arguments.of("Collection & List – Collection is the lowest common supertype", resolveClass("java.util.Collection"), resolveClass("java.util.List"), resolveClass("java.util.Collection")), // Additional sanity checks (not in original JSON)
        Arguments.of("Same class (Date) – returns the class itself", resolveClass("java.util.Date"), resolveClass("java.util.Date"), resolveClass("java.util.Date")), Arguments.of("Subclass and its superclass (SubClass & TestClass)", SubClass.class, TestClass.class, TestClass.class), Arguments.of("Two interfaces with a common parent (ChildA & ChildB)", ChildA.class, ChildB.class, Parent.class));
    }

    /**
     * Resolves a class name to a {@link Class} instance.
     *
     * <p>
     * Supports:
     * <ul>
     *   <li>Fully‑qualified class names (e.g. {@code java.util.List})</li>
     *   <li>Primitive type names (e.g. {@code int})</li>
     *   <li>Array types expressed with {@code []} (e.g. {@code java.lang.String[]})</li>
     * </ul>
     * Returns {@code null} for a {@code null} input.
     * </p>
     *
     * @param name the class name or {@code null}
     * @return the corresponding {@link Class} object or {@code null}
     */
    private static Class<?> resolveClass(String name) {
        if (name == null) {
            return null;
        }
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
        }
        // Array types – simple handling for one‑dimensional arrays
        if (name.endsWith("[]")) {
            String componentName = name.substring(0, name.length() - 2);
            Class<?> component = resolveClass(componentName);
            return java.lang.reflect.Array.newInstance(component, 0).getClass();
        }
        // Regular reference types
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Unable to resolve class name: " + name, e);
        }
    }

    /* -----------------------------------------------------------
     *  Parameterised tests for findLowestCommonSupertype()
     * ----------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideCommonSupertypeScenarios")
    void testFindLowestCommonSupertype(String description, Class<?> a, Class<?> b, Class<?> expected) {
        Class<?> result = ClassUtilities.findLowestCommonSupertype(a, b);
        assertEquals(expected, result, () -> "Unexpected lowest common supertype for " + (a == null ? "null" : a.getName()) + " & " + (b == null ? "null" : b.getName()));
    }

    /* -----------------------------------------------------------
     *  Additional explicit tests for edge‑cases
     * ----------------------------------------------------------- */
    @Test
    @DisplayName("When both arguments are the same class, that class is returned")
    void testSameClassReturnsItself() {
        Class<?> result = ClassUtilities.findLowestCommonSupertype(Date.class, Date.class);
        assertEquals(Date.class, result);
    }

    @Test
    @DisplayName("When one argument is a subclass of the other, the superclass is returned")
    void testSubclassAndSuperclass() {
        Class<?> result = ClassUtilities.findLowestCommonSupertype(SubClass.class, TestClass.class);
        assertEquals(TestClass.class, result);
    }

    @Test
    @DisplayName("When both arguments are interfaces with a common parent interface")
    void testInterfaceWithCommonParent() {
        Class<?> result = ClassUtilities.findLowestCommonSupertype(ChildA.class, ChildB.class);
        assertEquals(Parent.class, result);
    }
}