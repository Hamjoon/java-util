package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import java.time.DayOfWeek;
import java.time.Month;

// @ExtendWith(MockitoExtension.class)
class ClassUtilities_findLowestCommonSupertype_Test {

    interface TestInterface {
    }

    interface SubInterface extends TestInterface {
    }

    static class TestClass {
    }

    static class TestClassImplementingInterface implements TestInterface {
    }

    static class TestClassImplementingSubInterface implements SubInterface {
    }

    static class SubClass extends TestClass implements TestInterface {
    }

    /* --------------------------------------------------------------------- *
     *  Parameterised test data for findLowestCommonSupertype                 *
     * --------------------------------------------------------------------- */
    private static Stream<Arguments> commonSupertypeProvider() {
        return Stream.of(// Simple class‑class relationship – lowest common is the superclass
        Arguments.of(ArrayList.class, LinkedList.class, List.class), // Interface‑class relationship – interface is the lowest common supertype
        Arguments.of(TestClassImplementingSubInterface.class, TestClassImplementingInterface.class, TestInterface.class), // Two wrappers of the same primitive – common supertype is Number
        Arguments.of(Integer.class, Double.class, Number.class), // Class and its direct subclass – result should be the subclass' superclass
        Arguments.of(SubClass.class, TestClass.class, TestClass.class), // Two unrelated types – only Object is common, method should return null
        Arguments.of(Runnable.class, Error.class, null));
    }

    /* --------------------------------------------------------------------- *
     *  Parameterised test data for findLowestCommonSupertype                 *
     * --------------------------------------------------------------------- */
    private static Stream<Arguments> lowestCommonSupertypeProvider() {
        return // Simple class‑class relationship – lowest common is a concrete superclass (AbstractList).
        Stream.// Simple class‑class relationship – lowest common is a concrete superclass (AbstractList).
        of(// Two unrelated concrete collections – only Object is common.
        Arguments.of(ArrayList.class, LinkedList.class, AbstractList.class, null), // Same class – the class itself is the lowest common supertype.
        Arguments.of(HashMap.class, TreeSet.class, Object.class, null), // Primitive vs. reference type – no common reference supertype.
        Arguments.of(ArrayList.class, ArrayList.class, ArrayList.class, null), // Null argument handling – should throw NPE.
        Arguments.of(int.class, String.class, null, null), // Interface‑class relationship – the interface is the lowest common.
        Arguments.of(null, String.class, null, NullPointerException.class), // Interface‑class relationship – the interface is the lowest common.
        Arguments.of(null, null, null, NullPointerException.class), // Class implements an interface – the interface is the most specific.
        Arguments.of(List.class, Set.class, Collection.class, null), // Subclass‑superclass relationship – the superclass is returned.
        Arguments.of(ArrayList.class, List.class, List.class, null), // Different numeric wrapper classes – common supertype is Number.
        Arguments.of(Integer.class, Number.class, Number.class, null), // Different enum types – common supertype is java.lang.Enum.
        Arguments.of(Double.class, Long.class, Number.class, null), // Object with an interface – only Object is common.
        Arguments.of(DayOfWeek.class, Month.class, Enum.class, null), // Same interface – the interface itself is returned.
        Arguments.of(Object.class, List.class, Object.class, null), // Primitive vs. its wrapper – no common reference supertype.
        Arguments.of(Runnable.class, Runnable.class, Runnable.class, null), // Two different primitives – no common reference supertype.
        Arguments.of(int.class, Integer.class, null, null), // Null as second argument – should throw NPE.
        Arguments.of(int.class, double.class, null, null), Arguments.of(String.class, null, null, NullPointerException.class));
    }

    @ParameterizedTest
    @MethodSource("commonSupertypeProvider")
    void testFindLowestCommonSupertype(Class<?> a, Class<?> b, Class<?> expected) {
        // Normal execution – compare the returned type (may be null).
        Class<?> result = ClassUtilities.findLowestCommonSupertype(a, b);
        assertEquals(expected, result, () -> String.format("Expected lowest common supertype of %s and %s to be %s but got %s", a == null ? "null" : a.getName(), b == null ? "null" : b.getName(), expected == null ? "null" : expected.getName(), result == null ? "null" : result.getName()));
    }

    @Test
    void testFindLowestCommonSupertype_WithNullArguments() {
        // Both null – defined behaviour is to return null
        assertNull(ClassUtilities.findLowestCommonSupertype(null, null));
        // One null – also returns null (no common supertype can be computed)
        assertNull(ClassUtilities.findLowestCommonSupertype(String.class, null));
        assertNull(ClassUtilities.findLowestCommonSupertype(null, Integer.class));
    }
}