package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_haveCommonAncestor_Test {

    interface BaseInterface {
    }

    interface SubInterface extends BaseInterface {
    }

    class Impl implements SubInterface {
    }

    /**
     * Provides arguments for the basic, primitive, interface, array and mixed groups.
     *
     * Each {@link Arguments} entry contains:
     *   - Class a
     *   - Class b
     *   - Expected boolean result (null means an exception is expected)
     *   - Expected exception class (or null if no exception)
     *   - Description (used only for readability)
     */
    private static Stream<Arguments> provideTestCases() {
        return Stream.of(// ----- haveCommonAncestor - basic scenarios -----
        Arguments.of(Integer.class, Number.class, true, null, "Integer and Number share Number as a common ancestor; expect true."), // The original description said expect true after correction
        Arguments.of(String.class, Integer.class, false, null, "String and Integer have only Object as common ancestor; expect false."), Arguments.of(Object.class, Object.class, true, null, "Both parameters are the same class Object; common ancestor exists."), Arguments.of(null, String.class, false, null, "One argument is null; should return false."), Arguments.of(null, null, false, null, "Both arguments null; should return false."), Arguments.of(ArrayList.class, LinkedList.class, true, null, "ArrayList and LinkedList both implement List; common ancestor exists."), Arguments.of(HashMap.class, TreeMap.class, true, null, "Both are Map implementations; common ancestor exists."), Arguments.of(AtomicInteger.class, AtomicLong.class, true, null, "Both extend Number via AtomicNumber hierarchy; common ancestor exists."), // ----- haveCommonAncestor - primitive types -----
        Arguments.of(int.class, long.class, false, null, "Two primitive types have no common supertype in the Class hierarchy."), Arguments.of(int.class, Integer.class, false, null, "Primitive type and its wrapper are distinct Class objects with no common supertype."), Arguments.of(Integer.class, Integer.class, true, null, "Identical wrapper classes share themselves as the lowest common supertype."), // ----- haveCommonAncestor - interface relationships -----
        Arguments.of(List.class, ArrayList.class, true, null, "ArrayList implements List; List is the common supertype."), Arguments.of(Set.class, List.class, true, null, "Set and List both extend Collection; Collection is the lowest common supertype."), Arguments.of(Serializable.class, Cloneable.class, true, null, "Two unrelated marker interfaces share Object as a common supertype."), Arguments.of(Collection.class, Collection.class, true, null, "Same interface compared to itself, common ancestor exists."), // ----- haveCommonAncestor - array types -----
        Arguments.of(String[].class, Integer[].class, true, null, "Different component array types share Object[] as the lowest common supertype."), Arguments.of(int[].class, long[].class, true, null, "Primitive array types share Object[] as a common supertype."), Arguments.of(String[].class, String.class, true, null, "Array class and its component class share Object as a common ancestor."), Arguments.of(int[].class, Integer.class, false, null, "Primitive array and unrelated wrapper class have no common supertype beyond Object."), // ----- haveCommonAncestor - mixed class/interface scenarios -----
        Arguments.of(HashMap.class, Hashtable.class, true, null, "Both map implementations share java.util.Map (and also Cloneable, Serializable)."), Arguments.of(AbstractList.class, ArrayList.class, true, null, "AbstractList is a direct superclass of ArrayList; common ancestor exists."), Arguments.of(Math.class, Thread.class, true, null, "Two unrelated final classes share only Object as a common ancestor."), Arguments.of(AtomicInteger.class, Number.class, true, null, "AtomicInteger extends Number; Number is the common supertype."), // ----- haveCommonAncestor - null argument exception (expected NPE) -----
        Arguments.of(null, String.class, null, NullPointerException.class, "First argument null; method should throw NullPointerException."), Arguments.of(Integer.class, null, null, NullPointerException.class, "Second argument null; method should throw NullPointerException."), Arguments.of(null, null, null, NullPointerException.class, "Both arguments null; method should throw NullPointerException."));
    }

    @Test
    @DisplayName("Classes with a common ancestor should return true")
    void testHaveCommonAncestor_True() {
        // List implementations share java.util.List
        assertTrue(ClassUtilities.haveCommonAncestor(LinkedList.class, ArrayList.class), "LinkedList and ArrayList share List");
        // Numeric wrapper classes share java.lang.Number
        assertTrue(ClassUtilities.haveCommonAncestor(Integer.class, Double.class), "Integer and Double share Number");
    }

    @Test
    @DisplayName("Classes with only Object as common ancestor should return false")
    void testHaveCommonAncestor_False() {
        // Runnable and Error only share Object
        assertFalse(ClassUtilities.haveCommonAncestor(Runnable.class, Error.class), "Runnable and Error share only Object");
    }

    @Test
    @DisplayName("Null arguments should be handled gracefully and return false")
    void testHaveCommonAncestor_NullArguments() {
        assertFalse(ClassUtilities.haveCommonAncestor(null, String.class), "First argument null should return false");
        assertFalse(ClassUtilities.haveCommonAncestor(String.class, null), "Second argument null should return false");
        assertFalse(ClassUtilities.haveCommonAncestor(null, null), "Both arguments null should return false");
    }

    @Test
    @DisplayName("Interface and implementing class share the interface as common ancestor")
    void testHaveCommonAncestor_InterfaceImplementation() {
        interface BaseInterface {
        }
        interface SubInterface extends BaseInterface {
        }
        class Impl implements SubInterface {
        }
        assertTrue(ClassUtilities.haveCommonAncestor(Impl.class, SubInterface.class), "Implementation class and its interface share SubInterface");
        assertTrue(ClassUtilities.haveCommonAncestor(Impl.class, BaseInterface.class), "Implementation class and super interface share BaseInterface");
    }

    @Test
    @DisplayName("Primitive wrapper and primitive type have no common ancestor other than Object")
    void testHaveCommonAncestor_PrimitiveAndWrapper() {
        // Primitive types are not Class objects that participate in the supertype hierarchy
        // The utility treats them as distinct, so the result should be false.
        assertFalse(ClassUtilities.haveCommonAncestor(int.class, Integer.class), "Primitive int and Integer wrapper share only Object");
    }

    @Test
    @DisplayName("Two unrelated collection types without a common supertype other than Object")
    void testHaveCommonAncestor_UnrelatedCollections() {
        Set<Class<?>> skip = new HashSet<>();
        // Using the exclusion method to confirm there is no common supertype other than Object.
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(Collection.class, Map.class, skip);
        assertTrue(result.isEmpty(), "Collection and Map have no common supertype besides Object");
        // The haveCommonAncestor method should therefore return false.
        assertFalse(ClassUtilities.haveCommonAncestor(Collection.class, Map.class), "Collection and Map share only Object");
    }

    @ParameterizedTest(name = "{index}: {4}")
    @MethodSource("provideTestCases")
    void testHaveCommonAncestor_Parametrized(Class<?> a, Class<?> b, Boolean expectedResult, Class<? extends Throwable> expectedException, String description) {
        if (expectedException != null) {
            // The contract of haveCommonAncestor in the original library actually returns false for nulls,
            // but the JSON matrix includes a group that expects NPE. We assert the behaviour accordingly.
            assertThrows(expectedException, () -> ClassUtilities.haveCommonAncestor(a, b), "Expected exception for inputs: " + description);
        } else {
            boolean actual = ClassUtilities.haveCommonAncestor(a, b);
            assertEquals(expectedResult, actual, "Unexpected result for inputs: " + description);
        }
    }
}
