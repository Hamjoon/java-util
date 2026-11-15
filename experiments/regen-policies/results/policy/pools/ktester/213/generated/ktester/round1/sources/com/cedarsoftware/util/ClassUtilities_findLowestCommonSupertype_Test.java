package com.cedarsoftware.util;

import java.lang.reflect.Method;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractCollection;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
// import java.util.ConcurrentHashMap;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
// <-- uncommented
import java.util.concurrent.ConcurrentHashMap;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_findLowestCommonSupertype_Test {

    /* -----------------------------------------------------------
     *  Helper types used by the tests
     * ----------------------------------------------------------- */
    interface TestInterface {
    }

    interface AnotherInterface {
    }

    static class BaseClass {
    }

    static class SubClass extends BaseClass implements TestInterface {
    }

    static class MultiInterfaceClass implements TestInterface, AnotherInterface {
    }

    /**
     * Centralised assertion handling – treats {@code Object.class} as a null expectation.
     */
    private void assertResultMatchesExpectation(Class<?> actual, Class<?> expected, String description) {
        // The utility excludes Object from the result set; when the only common ancestor is Object it returns null.
        if (expected == Object.class) {
            assertNull(actual, description + " – expected null because only Object is common");
        } else if (expected == null) {
            assertNull(actual, description);
        } else {
            assertEquals(expected, actual, description);
        }
    }

    /**
     * Provider for the “basic functionality” group.
     */
    static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(Arguments.of(Integer.class, Double.class, Number.class, "Both classes are wrappers of primitive numeric types; their lowest common supertype should be java.lang.Number."), Arguments.of(ArrayList.class, LinkedList.class, AbstractList.class, "Two List implementations share AbstractList as the lowest common concrete supertype."), Arguments.of(HashMap.class, TreeMap.class, AbstractMap.class, "Two Map implementations share AbstractMap as the lowest common supertype."), Arguments.of(String.class, StringBuilder.class, Object.class, "String and StringBuilder have only Object as common supertype."), Arguments.of(Integer.class, Integer.class, Integer.class, "Identical classes should return the class itself."));
    }

    /**
     * Provider for primitive / wrapper interaction cases.
     */
    static Stream<Arguments> primitiveWrapperProvider() {
        return Stream.of(Arguments.of(int.class, Integer.class, Integer.class, "Primitive int and its wrapper Integer share the wrapper type as the lowest common supertype."), Arguments.of(double.class, Float.class, Number.class, "Different primitive/wrapper numeric types should resolve to Number, the common superclass of all numeric wrappers."));
    }

    /**
     * Provider for interface‑based commonality cases.
     */
    static Stream<Arguments> interfaceBasedProvider() {
        return Stream.of(Arguments.of(ArrayList.class, HashSet.class, Collection.class, "ArrayList and HashSet implement Collection; the lowest common supertype is the Collection interface."), Arguments.of(BlockingQueue.class, BlockingDeque.class, BlockingQueue.class, "BlockingDeque extends BlockingQueue, so the common supertype is BlockingQueue."));
    }

    /**
     * Provider for unrelated class pairs.
     */
    static Stream<Arguments> unrelatedClassesProvider() {
        return Stream.of(Arguments.of(Thread.class, LocalDate.class, Object.class, "Two completely unrelated classes share only Object as a common ancestor."));
    }

    /**
     * Provider for null‑argument handling.
     */
    static Stream<Arguments> nullHandlingProvider() {
        return Stream.of(Arguments.of(null, String.class, null, "If one argument is null, the method should return null (no common supertype)."), Arguments.of(null, null, null, "Both arguments null should also result in null."));
    }

    /**
     * Provider for array‑type interactions.
     */
    static Stream<Arguments> arrayTypesProvider() {
        return Stream.of(Arguments.of(String[].class, Object[].class, Object[].class, "Arrays of reference types share a common supertype of the array of the nearest common component type."), Arguments.of(int[].class, Object.class, Object.class, "Primitive array and non‑array class have only Object as common supertype."));
    }

    /**
     * Returns {@code true} if the supplied {@link Method} has a return
     * type of {@code void}.
     *
     * @param method the method to inspect; never {@code null}
     * @return {@code true} if the method returns {@code void}
     */
    public static boolean returnsVoid(Method method) {
        return method.getReturnType() == void.class;
    }

    /**
     * Finds a method with the given name and parameter types on the
     * supplied class (including its super‑classes and interfaces).
     *
     * @param clazz          the class to search; never {@code null}
     * @param name           the method name; never {@code null}
     * @param parameterTypes the parameter types; may be empty but never {@code null}
     * @return an {@link Optional} containing the matching {@link Method},
     *         or an empty {@link Optional} if no such method exists
     */
    public static Optional<Method> findMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        // Search declared methods of the class hierarchy
        for (Class<?> current = clazz; current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(name) && java.util.Arrays.equals(method.getParameterTypes(), parameterTypes)) {
                    return Optional.of(method);
                }
            }
        }
        // Search interfaces (including super‑interfaces)
        for (Class<?> iface : clazz.getInterfaces()) {
            Optional<Method> result = findMethod(iface, name, parameterTypes);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }

    /**
     * Determines whether a method with the specified signature is present
     * on the given class.
     *
     * @param clazz          the class to inspect; never {@code null}
     * @param name           the method name; never {@code null}
     * @param parameterTypes the parameter types; may be empty but never {@code null}
     * @return {@code true} if such a method exists
     */
    public static boolean isMethodPresent(Class<?> clazz, String name, Class<?>... parameterTypes) {
        return findMethod(clazz, name, parameterTypes).isPresent();
    }

    /**
     * Provider for generic collection implementation cases.
     */
    static Stream<Arguments> genericCollectionProvider() {
        // <-- uncommented
        return Stream.of(Arguments.of(ConcurrentHashMap.class, Hashtable.class, Dictionary.class, "Both classes extend Dictionary (via abstract superclasses), making it the lowest common supertype."), Arguments.of(PriorityQueue.class, ArrayDeque.class, AbstractCollection.class, "Both are concrete collections that share AbstractCollection as the lowest common concrete supertype."));
    }

    @Test
    @DisplayName("Same class returns itself")
    void testSameClass() {
        Class<?> result = ClassUtilities.findLowestCommonSupertype(String.class, String.class);
        assertEquals(String.class, result, "When both arguments are the same class the result should be that class");
    }

    @Test
    @DisplayName("Superclass is returned for subclass / superclass pair")
    void testSuperclassRelationship() {
        Class<?> result = ClassUtilities.findLowestCommonSupertype(SubClass.class, BaseClass.class);
        assertEquals(BaseClass.class, result, "The common supertype of a subclass and its direct superclass must be the superclass");
    }

    @Test
    @DisplayName("Common interface is returned for two unrelated concrete classes")
    void testCommonInterface() {
        Class<?> result = ClassUtilities.findLowestCommonSupertype(ArrayList.class, LinkedList.class);
        // Both implement java.util.List – that is the lowest common supertype (excluding Object)
        assertEquals(List.class, result, "ArrayList and LinkedList share List as the lowest common supertype");
    }

    @Test
    @DisplayName("No common supertype other than Object yields null")
    void testNoCommonSupertype() {
        Class<?> result = ClassUtilities.findLowestCommonSupertype(Runnable.class, Error.class);
        assertNull(result, "When the only common supertype is Object the method must return null");
    }

    @Test
    @DisplayName("Null arguments produce null result")
    void testNullArguments() {
        assertNull(ClassUtilities.findLowestCommonSupertype(null, String.class), "First argument null should result in null");
        assertNull(ClassUtilities.findLowestCommonSupertype(Integer.class, null), "Second argument null should result in null");
        assertNull(ClassUtilities.findLowestCommonSupertype(null, null), "Both arguments null should also result in null");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("basicFunctionalityProvider")
    void testBasicFunctionality(Class<?> a, Class<?> b, Class<?> expected, String description) {
        Class<?> result = ClassUtilities.findLowestCommonSupertype(a, b);
        assertResultMatchesExpectation(result, expected, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("primitiveWrapperProvider")
    void testPrimitiveWrapperInteractions(Class<?> a, Class<?> b, Class<?> expected, String description) {
        Class<?> result = ClassUtilities.findLowestCommonSupertype(a, b);
        assertResultMatchesExpectation(result, expected, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("interfaceBasedProvider")
    void testInterfaceBasedCommonality(Class<?> a, Class<?> b, Class<?> expected, String description) {
        Class<?> result = ClassUtilities.findLowestCommonSupertype(a, b);
        assertResultMatchesExpectation(result, expected, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("unrelatedClassesProvider")
    void testUnrelatedClasses(Class<?> a, Class<?> b, Class<?> expected, String description) {
        Class<?> result = ClassUtilities.findLowestCommonSupertype(a, b);
        assertResultMatchesExpectation(result, expected, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("nullHandlingProvider")
    void testNullHandling(Class<?> a, Class<?> b, Class<?> expected, String description) {
        Class<?> result = ClassUtilities.findLowestCommonSupertype(a, b);
        assertResultMatchesExpectation(result, expected, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("arrayTypesProvider")
    void testArrayTypes(Class<?> a, Class<?> b, Class<?> expected, String description) {
        Class<?> result = ClassUtilities.findLowestCommonSupertype(a, b);
        assertResultMatchesExpectation(result, expected, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("genericCollectionProvider")
    void testGenericCollectionImplementations(Class<?> a, Class<?> b, Class<?> expected, String description) {
        Class<?> result = ClassUtilities.findLowestCommonSupertype(a, b);
        assertResultMatchesExpectation(result, expected, description);
    }
}
