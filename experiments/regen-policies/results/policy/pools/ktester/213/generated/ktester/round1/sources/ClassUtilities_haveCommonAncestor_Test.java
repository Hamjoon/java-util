package com.cedarsoftware.util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_haveCommonAncestor_Test {

    interface A {
    }

    interface B extends A {
    }

    /**
     * Supplies the test data extracted from the JSON specification.
     *
     * @return a stream of arguments: class‑name A, class‑name B, expected result, description
     */
    private static Stream<Arguments> provideHaveCommonAncestorCases() {
        return Stream.of(/* Common ancestor exists */
        args("java.lang.String", "java.lang.Integer", "true", "Two unrelated concrete classes both extend Object, so true."), args("java.util.ArrayList", "java.util.LinkedList", "true", "Both are List implementations; common ancestors include List and Object."), args("java.util.List", "java.util.ArrayList", "true", "Interface List and concrete class ArrayList – List is a common supertype."), args("java.lang.String", "java.lang.String", "true", "Same class – the class itself is the lowest common supertype."), /* No common ancestor (primitive vs reference) */
        args("int", "java.lang.String", "false", "Primitive int has no supertype relationship with a reference type."), args("int", "long", "false", "Two different primitive types share no common supertype in the utility logic."), args("void", "java.lang.Object", "false", "void is not a class and cannot have a common ancestor with Object."), /* Invalid inputs – null arguments */
        args("null", "java.lang.String", "NullPointerException", "First argument is null – should throw NPE."), args("java.lang.String", "null", "NullPointerException", "Second argument is null – should throw NPE."), args("null", "null", "NullPointerException", "Both arguments null – should throw NPE."), /* Superclass relationship */
        args("java.util.ArrayList", "java.util.AbstractList", "true", "ArrayList extends AbstractList – common ancestor exists."), args("java.util.AbstractList", "java.util.ArrayList", "true", "Reverse order – still true."), args("java.lang.Object", "java.lang.String", "true", "Object is the root of the hierarchy – any reference type shares it."), /* Interface relationships */
        args("java.util.Collection", "java.util.List", "true", "List extends Collection – common ancestor is Collection."), args("java.util.Map", "java.util.Collection", "true", "Unrelated interfaces still share Object as a common supertype."), args("java.util.RandomAccess", "java.util.HashMap", "true", "Both are reference types – Object is common ancestor."), args("java.util.RandomAccess", "java.util.ArrayList", "true", "ArrayList implements RandomAccess – interface is common ancestor."), args("java.util.List", "int", "false", "Primitive type cannot share a supertype with an interface."), /* Enum relationships */
        args("java.time.DayOfWeek", "java.time.Month", "true", "All enums extend java.lang.Enum – common ancestor exists."), args("java.time.DayOfWeek", "java.lang.String", "true", "Enum and regular class share Object as common ancestor."), /* Array relationships */
        args("java.lang.String[]", "java.lang.Integer[]", "true", "Both are array types – Object is the lowest common supertype."), args("int[]", "java.lang.String[]", "true", "Primitive and reference arrays are still objects – common ancestor Object."), args("java.lang.String[]", "java.lang.String", "true", "Array class and its component type share Object as common ancestor."), /* Wrapper and primitive relationships */
        args("java.lang.Integer", "int", "false", "Utility treats primitive types and wrappers as unrelated for common‑ancestor detection."), args("java.lang.Integer", "java.lang.Long", "true", "Both wrappers extend Number – common ancestor exists."), args("int", "int", "false", "Identical primitive types have no supertype in the utility logic."), /* Void edge cases */
        args("void", "int", "false", "void is not a class; pairing it with a primitive yields no common ancestor."));
    }

    /**
     * Helper to convert the textual representation used in the JSON test data
     * into a {@link Class} object. Handles primitives, {@code void}, arrays and the
     * literal {@code "null"} (which is represented by {@code null}).
     *
     * @param name the textual class name from the JSON data
     * @return the corresponding {@link Class} instance, or {@code null} if the name is "null"
     */
    private static Class<?> classFromString(String name) {
        if ("null".equals(name)) {
            return null;
        }
        switch(name) {
            case "int":
                return int.class;
            case "long":
                return long.class;
            case "void":
                return void.class;
            case "java.lang.String[]":
                return java.lang.String[].class;
            case "java.lang.Integer[]":
                return java.lang.Integer[].class;
            case "int[]":
                return int[].class;
            case "java.lang.String":
                return java.lang.String.class;
            case "java.lang.Integer":
                return java.lang.Integer.class;
            case "java.lang.Long":
                return java.lang.Long.class;
            case "java.util.ArrayList":
                return java.util.ArrayList.class;
            case "java.util.LinkedList":
                return java.util.LinkedList.class;
            case "java.util.List":
                return java.util.List.class;
            case "java.util.Collection":
                return java.util.Collection.class;
            case "java.util.AbstractList":
                return java.util.AbstractList.class;
            case "java.util.Map":
                return java.util.Map.class;
            case "java.util.RandomAccess":
                return java.util.RandomAccess.class;
            case "java.util.HashMap":
                return java.util.HashMap.class;
            case "java.time.DayOfWeek":
                return java.time.DayOfWeek.class;
            case "java.time.Month":
                return java.time.Month.class;
            case "java.lang.Object":
                return java.lang.Object.class;
            case "java.lang.Runnable":
                return java.lang.Runnable.class;
            case "java.lang.Error":
                return java.lang.Error.class;
            case "java.lang.Cloneable":
                return java.lang.Cloneable.class;
            default:
                // Fallback – try to load the class by name (covers any future additions)
                try {
                    return Class.forName(name);
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException("Unsupported class name in test data: " + name, e);
                }
        }
    }

    /**
     * Convenience method for creating {@link Arguments} without verbose imports.
     */
    private static Arguments args(String a, String b, String expected, String description) {
        return Arguments.of(a, b, expected, description);
    }

    private static Stream<Arguments> provideExclusionCases() {
        Set<Class<?>> exclude = new HashSet<>();
        // Collection and Map share only Object; we also exclude Object to force emptiness.
        exclude.add(java.util.Collection.class);
        exclude.add(java.util.Map.class);
        exclude.add(Object.class);
        return Stream.of(Arguments.of(java.util.Collection.class, java.util.Map.class, exclude));
    }

    @Test
    @DisplayName("haveCommonAncestor returns true for classes sharing a common interface")
    void testHaveCommonAncestor_True_Interface() {
        // LinkedList and ArrayList both implement java.util.List
        assertTrue(ClassUtilities.haveCommonAncestor(java.util.LinkedList.class, java.util.ArrayList.class), "LinkedList and ArrayList should have a common ancestor (List)");
    }

    @Test
    @DisplayName("haveCommonAncestor returns true for numeric wrapper classes")
    void testHaveCommonAncestor_True_Numeric() {
        // Integer and Double both extend Number
        assertTrue(ClassUtilities.haveCommonAncestor(Integer.class, Double.class), "Integer and Double should have a common ancestor (Number)");
    }

    @Test
    @DisplayName("haveCommonAncestor returns false when only Object is common")
    void testHaveCommonAncestor_False_NoCommonBeyondObject() {
        // Runnable and Error share only Object
        assertFalse(ClassUtilities.haveCommonAncestor(Runnable.class, Error.class), "Runnable and Error share only Object, therefore false");
    }

    @Test
    @DisplayName("haveCommonAncestor returns false when either argument is null")
    void testHaveCommonAncestor_NullArguments() {
        assertFalse(ClassUtilities.haveCommonAncestor(null, String.class), "Null first argument should yield false");
        assertFalse(ClassUtilities.haveCommonAncestor(String.class, null), "Null second argument should yield false");
        assertFalse(ClassUtilities.haveCommonAncestor(null, null), "Both arguments null should yield false");
    }

    @Test
    @DisplayName("haveCommonAncestor works with interfaces that extend each other")
    void testHaveCommonAncestor_InterfaceHierarchy() {
        interface A {
        }
        interface B extends A {
        }
        assertTrue(ClassUtilities.haveCommonAncestor(A.class, B.class), "B extends A, so they have a common ancestor");
    }

    @Test
    @DisplayName("haveCommonAncestor returns false for unrelated primitive wrapper vs. interface")
    void testHaveCommonAncestor_UnrelatedTypes() {
        // Primitive wrapper (Integer) vs. an unrelated interface (Cloneable)
        assertFalse(ClassUtilities.haveCommonAncestor(Integer.class, Cloneable.class), "Integer and Cloneable share only Object");
    }

    @Test
    @DisplayName("findLowestCommonSupertypesExcluding returns empty set when all supertypes are excluded")
    void testFindLowestCommonSupertypesExcluding_AllExcluded() {
        Set<Class<?>> exclude = new HashSet<>();
        // Exclude the only common supertype (Collection) between Collection and Map
        exclude.add(java.util.Collection.class);
        exclude.add(java.util.Map.class);
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(java.util.Collection.class, java.util.Map.class, exclude);
        assertTrue(result.isEmpty(), "When all common supertypes are excluded, the result should be empty");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("provideHaveCommonAncestorCases")
    @DisplayName("haveCommonAncestor – JSON driven scenarios")
    void testHaveCommonAncestor(String aName, String bName, String expected, String description) {
        Class<?> a = classFromString(aName);
        Class<?> b = classFromString(bName);
        if ("NullPointerException".equals(expected)) {
            // The contract (as per the JSON spec) is that a null argument should cause NPE.
            assertThrows(NullPointerException.class, () -> ClassUtilities.haveCommonAncestor(a, b), description);
        } else {
            boolean expectedBool = Boolean.parseBoolean(expected);
            boolean actual = ClassUtilities.haveCommonAncestor(a, b);
            assertEquals(expectedBool, actual, description);
        }
    }
}
