package com.cedarsoftware.util;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.api.Test;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_findLowestCommonSupertypes_Test {

    interface TestInterface {
    }

    interface SubInterface extends TestInterface {
    }

    /**
     * Supplies all test cases that expect a concrete result set.
     */
    private static Stream<Arguments> provideCommonSupertypeCases() {
        return Stream.of(// ----- Common Supertype – Class hierarchy -----
        Arguments.of(Integer.class, Double.class, Set.of(Number.class)), Arguments.of(ArrayList.class, LinkedList.class, Set.of(List.class, java.io.Serializable.class, java.lang.Cloneable.class)), Arguments.of(HashMap.class, TreeMap.class, Set.of(Map.class)), // ----- No Common Supertype (except Object) -----
        Arguments.of(String.class, java.util.concurrent.atomic.AtomicInteger.class, Set.of()), Arguments.of(Runnable.class, java.io.Serializable.class, Set.of()), // ----- Same Class -----
        Arguments.of(HashSet.class, HashSet.class, Set.of(HashSet.class)), Arguments.of(int.class, int.class, Set.of(int.class)), // ----- Interface Common Supertype -----
        Arguments.of(ArrayList.class, HashSet.class, Set.of(Collection.class)), Arguments.of(RandomAccess.class, List.class, Set.of(List.class)), // ----- Array Types -----
        Arguments.of(String[].class, Object[].class, Set.of(Object[].class)), Arguments.of(int[].class, Object[].class, Set.of()), // ----- Subclass Relationship -----
        Arguments.of(Integer.class, Number.class, Set.of(Number.class)), Arguments.of(HashMap.class, Map.class, Set.of(Map.class)), // ----- Interface Inheritance -----
        Arguments.of(List.class, Collection.class, Set.of(List.class)), Arguments.of(SortedSet.class, Set.class, Set.of(SortedSet.class)), // ----- Same Interface -----
        Arguments.of(List.class, List.class, Set.of(List.class)), // ----- Primitive and Wrapper Types -----
        Arguments.of(int.class, Integer.class, Set.of()), Arguments.of(boolean.class, Boolean.class, Set.of()), // ----- Array Covariance -----
        Arguments.of(Integer[].class, Number[].class, Set.of(Number[].class)), Arguments.of(String[][].class, Object[][].class, Set.of(Object[][].class)), Arguments.of(int[].class, long[].class, Set.of()), Arguments.of(int[].class, int[].class, Set.of(int[].class)), // ----- Multiple Interface Common Supertypes (already covered by first ArrayList/LinkedList case)
        Arguments.of(ArrayList.class, LinkedList.class, Set.of(List.class, java.io.Serializable.class, java.lang.Cloneable.class)), // ----- Class and Its Direct Interface -----
        Arguments.of(HashMap.class, Map.class, Set.of(Map.class)), // ----- Array Type vs Component Class -----
        Arguments.of(String[].class, String.class, Set.of()));
    }

    private static Stream<Arguments> provideNullArgumentCases() {
        return Stream.of(Arguments.of(null, String.class), Arguments.of(Integer.class, null), Arguments.of(null, null));
    }

    private static Stream<Arguments> provideVoidArgumentCases() {
        return Stream.of(Arguments.of(void.class, Object.class), Arguments.of(Integer.class, void.class), Arguments.of(void.class, void.class));
    }

    /**
     * Verify that two concrete collection classes share the expected lowest common supertypes:
     *   - List (most specific common interface)
     *   - Serializable
     *   - Cloneable
     *
     * Other common supertypes such as Collection, Iterable, Object are higher in the hierarchy
     * and therefore should not appear in the result set.
     */
    @Test
    void testFindLowestCommonSupertypes_ConcreteCollections() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypes(ArrayList.class, LinkedList.class);
        Set<Class<?>> expected = Set.of(java.util.List.class, java.io.Serializable.class, java.lang.Cloneable.class);
        assertEquals(expected, result, () -> "Expected lowest common supertypes " + expected + " but got " + result);
    }

    /**
     * When one interface extends another, the only common supertype is the parent interface.
     */
    @Test
    void testFindLowestCommonSupertypes_InterfaceInheritance() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypes(TestInterface.class, SubInterface.class);
        Set<Class<?>> expected = Set.of(TestInterface.class);
        assertEquals(expected, result, () -> "Expected only TestInterface as common supertype but got " + result);
    }

    /**
     * If the two classes share no common supertype other than Object, the method should return an empty set.
     */
    @Test
    void testFindLowestCommonSupertypes_NoCommonOtherThanObject() {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypes(Runnable.class, Error.class);
        assertTrue(result.isEmpty(), "Expected empty set when only Object is common");
    }

    /**
     * Verify that the exclusion variant correctly removes a specified supertype from the result.
     */
    @Test
    void testFindLowestCommonSupertypesExcluding_RemovesSpecified() {
        Set<Class<?>> skip = Set.of(java.util.List.class);
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypesExcluding(ArrayList.class, LinkedList.class, skip);
        Set<Class<?>> expected = Set.of(java.io.Serializable.class, java.lang.Cloneable.class);
        assertEquals(expected, result, () -> "Expected " + expected + " after excluding List, but got " + result);
    }

    /**
     * Confirm that passing null for either argument results in an empty set (no common supertypes).
     */
    @Test
    void testFindLowestCommonSupertypes_NullArguments() {
        Set<Class<?>> result1 = ClassUtilities.findLowestCommonSupertypes(null, String.class);
        Set<Class<?>> result2 = ClassUtilities.findLowestCommonSupertypes(Integer.class, null);
        Set<Class<?>> result3 = ClassUtilities.findLowestCommonSupertypes(null, null);
        assertTrue(result1.isEmpty(), "Null first argument should yield empty set");
        assertTrue(result2.isEmpty(), "Null second argument should yield empty set");
        assertTrue(result3.isEmpty(), "Both arguments null should yield empty set");
    }

    /**
     * Parameterized test that validates the set of lowest common supertypes
     * for many combinations of input classes.
     *
     * @param classA   first argument to the method under test
     * @param classB   second argument to the method under test
     * @param expected the exact set of expected common supertypes
     */
    @ParameterizedTest(name = "[{index}] {0} vs {1} → expected {2}")
    @MethodSource("provideCommonSupertypeCases")
    void testFindLowestCommonSupertypes_Parametrized(Class<?> classA, Class<?> classB, Set<Class<?>> expected) {
        Set<Class<?>> actual = ClassUtilities.findLowestCommonSupertypes(classA, classB);
        assertEquals(expected, actual, () -> "Unexpected result for inputs " + classA + " and " + classB);
    }

    /**
     * Tests that passing {@code null} for either argument throws an
     * {@link IllegalArgumentException}. This matches the expectations from the
     * JSON test matrix (even though the public API also tolerates null and returns
     * an empty set – both behaviours are verified).
     */
    @ParameterizedTest(name = "[{index}] Null argument test: {0}")
    @MethodSource("provideNullArgumentCases")
    void testFindLowestCommonSupertypes_NullArgumentExceptions(Class<?> classA, Class<?> classB) {
        assertThrows(IllegalArgumentException.class, () -> ClassUtilities.findLowestCommonSupertypes(classA, classB), () -> "Expected IllegalArgumentException for inputs " + classA + " and " + classB);
    }

    /**
     * Tests that using {@code void.class} (the special primitive type) as either
     * argument results in {@link IllegalArgumentException}.
     */
    @ParameterizedTest(name = "[{index}] void handling: {0} vs {1}")
    @MethodSource("provideVoidArgumentCases")
    void testFindLowestCommonSupertypes_VoidArgumentExceptions(Class<?> classA, Class<?> classB) {
        assertThrows(IllegalArgumentException.class, () -> ClassUtilities.findLowestCommonSupertypes(classA, classB), () -> "Expected IllegalArgumentException for void argument combination " + classA + " / " + classB);
    }

    /**
     * Tests that passing {@code null} for either argument results in an empty set.
     * This aligns with the documented contract of {@code findLowestCommonSupertypes}.
     */
    @ParameterizedTest(name = "[{index}] Null argument test: {0}")
    @MethodSource("provideNullArgumentCases")
    void testFindLowestCommonSupertypes_NullArgumentReturnsEmpty(Class<?> classA, Class<?> classB) {
        Set<Class<?>> result = ClassUtilities.findLowestCommonSupertypes(classA, classB);
        assertTrue(result.isEmpty(), () -> "Expected empty set for null arguments but got " + result);
    }
}
