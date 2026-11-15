package com.cedarsoftware.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.cedarsoftware.util.CompactMap.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link CompactMap#getOrdering()}.
 */
// @ExtendWith(MockitoExtension.class)
class CompactMap_getOrdering_Test {

    /**
     * Supplies different builder configurations for the parameterized test above.
     */
    private static Stream<Arguments> builderOrderingProvider() {
        return Stream.of(
                Arguments.of("sortedOrder()", CompactMap.<String, Object>builder().sortedOrder().build()),
                Arguments.of("insertionOrder()", CompactMap.<String, Object>builder().insertionOrder().build()),
                Arguments.of("reverseOrder()", CompactMap.<String, Object>builder().reverseOrder().build()));
    }

    /**
     * Returns {@code true} if the supplied {@link Method} has a {@code void}
     * return type.
     *
     * @param method the method to inspect; never {@code null}
     * @return {@code true} if the method returns {@code void}
     */
    public static boolean returnsVoid(Method method) {
        return method.getReturnType() == void.class;
    }

    /**
     * Finds a declared method with the given name and parameter types on the
     * supplied class.  If no such method exists {@code null} is returned.
     *
     * @param clazz          the class to search; never {@code null}
     * @param methodName     the name of the method; never {@code null}
     * @param parameterTypes the parameter types; may be {@code null}
     * @return the {@link Method} if found, otherwise {@code null}
     */
    public static Method findMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        try {
            return clazz.getDeclaredMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    /**
     * Returns {@code true} if a method with the given signature is present on
     * the supplied class.
     *
     * @param clazz          the class to inspect; never {@code null}
     * @param methodName     the name of the method; never {@code null}
     * @param parameterTypes the parameter types; may be {@code null}
     * @return {@code true} if such a method exists
     */
    public static boolean isMethodPresent(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        return findMethod(clazz, methodName, parameterTypes) != null;
    }

    /**
     * Determines whether the supplied method is {@code public}.
     *
     * @param method the method to inspect; never {@code null}
     * @return {@code true} if the method is public
     */
    public static boolean isPublic(Method method) {
        return Modifier.isPublic(method.getModifiers());
    }

    /**
     * Determines whether the supplied method is {@code static}.
     *
     * @param method the method to inspect; never {@code null}
     * @return {@code true} if the method is static
     */
    public static boolean isStatic(Method method) {
        return Modifier.isStatic(method.getModifiers());
    }

    /**
     * Determines whether the supplied method is annotated with the given
     * annotation type.
     *
     * @param method            the method to inspect; never {@code null}
     * @param annotationType    the annotation type; never {@code null}
     * @return {@code true} if the annotation is present
     */
    public static boolean isAnnotated(Method method, Class<? extends Annotation> annotationType) {
        return method.isAnnotationPresent(annotationType);
    }

    /**
     * Determines whether the supplied class is {@code public}.
     *
     * @param clazz the class to inspect; never {@code null}
     * @return {@code true} if the class is public
     */
    public static boolean isPublic(Class<?> clazz) {
        return Modifier.isPublic(clazz.getModifiers());
    }

    /**
     * Determines whether the supplied class is {@code abstract}.
     *
     * @param clazz the class to inspect; never {@code null}
     * @return {@code true} if the class is abstract
     */
    public static boolean isAbstract(Class<?> clazz) {
        return Modifier.isAbstract(clazz.getModifiers());
    }

    /**
     * Determines whether the supplied class is {@code static}.  This is only
     * meaningful for nested classes.
     *
     * @param clazz the class to inspect; never {@code null}
     * @return {@code true} if the class is static
     */
    public static boolean isStatic(Class<?> clazz) {
        return Modifier.isStatic(clazz.getModifiers());
    }

    /**
     * Determines whether the supplied class is annotated with the given
     * annotation type.
     *
     * @param clazz            the class to inspect; never {@code null}
     * @param annotationType   the annotation type; never {@code null}
     * @return {@code true} if the annotation is present
     */
    public static boolean isAnnotated(Class<?> clazz, Class<? extends Annotation> annotationType) {
        return clazz.isAnnotationPresent(annotationType);
    }

    /**
     * Verify that a plain CompactMap created via the default constructor reports the
     * default ordering (UNORDERED).
     */
    @Test
    void defaultOrderingIsUnordered() {
        CompactMap<String, Object> map = new CompactMap<>();
        // getOrdering() is protected but accessible because this test class is in the same package
        String ordering = map.getOrdering();
        assertEquals(UNORDERED, ordering, "A CompactMap created with the default constructor should have ordering UNORDERED");
    }

    /**
     * Verify that a CompactMap built with {@code sortedOrder()} reports SORTED ordering.
     */
    @Test
    void builderSortedOrdering() {
        CompactMap<String, Object> map = CompactMap.<String, Object>builder().sortedOrder().build();
        String ordering = map.getOrdering();
        assertEquals(SORTED, ordering, "Builder with sortedOrder() should produce a map with ordering SORTED");
    }

    /**
     * Verify that a CompactMap built with {@code insertionOrder()} reports INSERTION ordering.
     */
    @Test
    void builderInsertionOrdering() {
        CompactMap<String, Object> map = CompactMap.<String, Object>builder().insertionOrder().build();
        String ordering = map.getOrdering();
        assertEquals(INSERTION, ordering, "Builder with insertionOrder() should produce a map with ordering INSERTION");
    }

    /**
     * Verify that a CompactMap built with {@code reverseOrder()} reports REVERSE ordering.
     */
    @Test
    void builderReverseOrdering() {
        CompactMap<String, Object> map = CompactMap.<String, Object>builder().reverseOrder().build();
        String ordering = map.getOrdering();
        assertEquals(REVERSE, ordering, "Builder with reverseOrder() should produce a map with ordering REVERSE");
    }

    /**
     * Verify that case‑sensitivity does not affect the reported ordering.
     */
    @Test
    void builderCaseInsensitiveSortedOrdering() {
        CompactMap<String, Object> map = CompactMap.<String, Object>builder()
                .caseSensitive(false)
                .sortedOrder()
                .build();
        String ordering = map.getOrdering();
        assertEquals(SORTED, ordering, "Case‑insensitive map with sortedOrder() should still report ordering SORTED");
    }

    /**
     * Verify that a map created via the copy‑constructor preserves the ordering of the source.
     * The source map is built with a specific ordering; the copy should report the same.
     */
    @Test
    void copyConstructorPreservesOrdering() {
        CompactMap<String, Object> source = CompactMap.<String, Object>builder()
                .insertionOrder()
                .build();
        source.put("a", 1);
        source.put("b", 2);
        CompactMap<String, Object> copy = new CompactMap<>(source);
        String ordering = copy.getOrdering();
        assertEquals(INSERTION, ordering, "Copy‑constructed CompactMap should retain the ordering of the source map");
    }

    /**
     * Verify that inserting entries and then removing them does not affect the ordering
     * reported by {@code getOrdering()}.
     */
    @Test
    void afterInsertAndRemoveOrderingIsUnordered() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("one", 1);
        map.put("two", 2);
        map.remove("one");
        map.remove("two");
        String ordering = map.getOrdering();
        assertEquals(UNORDERED, ordering, "After inserting and removing entries, ordering should still be UNORDERED");
    }

    /**
     * Parameterized test that checks various builder ordering options.
     * Regardless of the requested ordering, {@code getOrdering()} must still return UNORDERED.
     */
    @ParameterizedTest(name = "Builder ordering ''{0}'' should still report UNORDERED")
    @MethodSource("builderOrderingProvider")
    void builderOrderingAlwaysUnordered(String description, CompactMap<String, Object> map) {
        String ordering = map.getOrdering();
        assertEquals(UNORDERED, ordering, "Builder with " + description + " should produce a map with ordering UNORDERED");
    }

    /**
     * Verify that case‑sensitivity does not affect the ordering result.
     * Even when the map is built as case‑insensitive and sorted, the ordering remains UNORDERED.
     */
    @Test
    void caseInsensitiveBuilderOrderingIsUnordered() {
        CompactMap<String, Object> map = CompactMap.<String, Object>builder()
                .caseSensitive(false)
                .sortedOrder()
                .build();
        assertEquals(UNORDERED, map.getOrdering(), "Case‑insensitive map with sortedOrder() should still report ordering UNORDERED");
    }

    /**
     * Verify that a map created via the copy‑constructor preserves the ordering value
     * (which is always UNORDERED).
     */
    @Test
    void copyConstructorPreservesUnordered() {
        CompactMap<String, Object> source = CompactMap.<String, Object>builder()
                .insertionOrder()
                .build();
        source.put("a", 1);
        source.put("b", 2);
        CompactMap<String, Object> copy = new CompactMap<>(source);
        assertEquals(UNORDERED, copy.getOrdering(), "Copy‑constructed CompactMap should retain ordering UNORDERED");
    }
}