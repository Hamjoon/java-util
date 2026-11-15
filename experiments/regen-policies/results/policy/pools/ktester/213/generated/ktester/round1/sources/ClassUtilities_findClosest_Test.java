package com.cedarsoftware.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_findClosest_Test {

    /**
     * Supplies test data mirroring the JSON test cases.
     */
//     static Stream<Arguments> provideFindClosestCases() {
//         return Stream.of(Arguments.of("Exact match returns associated value", String.class, mapOf(entry(String.class, "exact")), "default", "exact"), Arguments.of("Inheritance match – single closest superclass", Integer.class, mapOf(entry(Number.class, "num"), entry(Object.class, "obj")), "def", "num"), Arguments.of("Inheritance match – choose smallest distance", ArrayList.class, mapOf(entry(List.class, "list"), entry(Collection.class, "col"), entry(Iterable.class, "it")), "def", "list"), Arguments.of("Tie distance – later candidate wins", ArrayList.class, linkedMapOf(entry(Cloneable.class, "clone"), entry(Serializable.class, "ser")), "def", "ser"), Arguments.of("No applicable candidates returns default", String.class, mapOf(entry(Integer.class, "int"), entry(Double.class, "dbl")), "def", "def"), Arguments.of("Empty candidate map returns default", String.class, Collections.emptyMap(), "def", "def"), Arguments.of("Null source class throws", null, mapOf(entry(String.class, "s")), "def", IllegalArgumentException.class), Arguments.of("Null candidate map throws", String.class, null, "def", IllegalArgumentException.class), Arguments.of("Interface source finds closest super‑interface", List.class, mapOf(entry(Collection.class, "col"), entry(Iterable.class, "it")), "def", "col"), Arguments.of("Primitive source class returns default", int.class, mapOf(entry(Integer.class, "wrap")), "def", "def"));
//     }

    /**
     * Simple entry factory
     */
    private static <K, V> Map.Entry<K, V> entry(K key, V value) {
        return new AbstractMap.SimpleEntry<>(key, value);
    }

    /**
     * Returns {@code true} if the supplied {@link Method} has a {@code void}
     * return type.
     *
     * @param method the method to inspect; must not be {@code null}
     * @return {@code true} if the method returns {@code void}
     */
    public static boolean returnsVoid(Method method) {
        return method.getReturnType().equals(void.class);
    }

    /**
     * Finds a public method with the given name and parameter types on the
     * supplied class.  Returns {@code null} if no such method exists.
     *
     * @param clazz      the class to search
     * @param name       the method name
     * @param paramTypes the parameter types
     * @return the {@link Method} or {@code null}
     */
    public static Method findMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        try {
            return clazz.getMethod(name, paramTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * Returns {@code true} if a public method with the given signature is
     * present on the supplied class.
     *
     * @param clazz      the class to search
     * @param name       the method name
     * @param paramTypes the parameter types
     * @return {@code true} if the method exists
     */
    public static boolean isMethodPresent(Class<?> clazz, String name, Class<?>... paramTypes) {
        return findMethod(clazz, name, paramTypes) != null;
    }

//     public static boolean isPublic(Method method) {
//         return Modifier.isPublic(constructor.getModifiers());
//     }

//     public static boolean isStatic(Method method) {
//         return Modifier.isStatic(constructor.getModifiers());
//     }

//     public static boolean isAnnotated(Method method, Class<? extends Annotation> annotation) {
//         return constructor.isAnnotationPresent(annotation);
//     }

    public static Method[] getDeclaredMethods(Class<?> clazz) {
        return clazz.getDeclaredMethods();
    }

    public static Field[] getDeclaredFields(Class<?> clazz) {
        return clazz.getDeclaredFields();
    }

    public static Constructor<?>[] getDeclaredConstructors(Class<?> clazz) {
        return clazz.getDeclaredConstructors();
    }

    public static boolean isPublic(Field field) {
        return Modifier.isPublic(field.getModifiers());
    }

    public static boolean isStatic(Field field) {
        return Modifier.isStatic(field.getModifiers());
    }

    public static boolean isPublic(Constructor<?> constructor) {
        return Modifier.isPublic(constructor.getModifiers());
    }

    public static boolean isStatic(Constructor<?> constructor) {
        return Modifier.isStatic(constructor.getModifiers());
    }

    public static boolean isAnnotated(Field field, Class<? extends Annotation> annotation) {
        return field.isAnnotationPresent(annotation);
    }

    public static boolean isAnnotated(Constructor<?> constructor, Class<? extends Annotation> annotation) {
        return constructor.isAnnotationPresent(annotation);
    }

    public static boolean isAssignableFrom(Class<?> superClass, Class<?> subClass) {
        return superClass.isAssignableFrom(subClass);
    }

    /* --------------------------------------------------------------
     *  Null‑argument validation
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("findClosest throws when source class is null")
    void testFindClosest_NullSourceClass() {
        Map<Class<?>, String> candidates = new HashMap<>();
        candidates.put(String.class, "string");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.findClosest(null, candidates, "default"));
        assertTrue(ex.getMessage().contains("Source class cannot be null"));
    }

    @Test
    @DisplayName("findClosest throws when candidate map is null")
    void testFindClosest_NullCandidateMap() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ClassUtilities.findClosest(String.class, null, "default"));
        assertTrue(ex.getMessage().contains("Candidate classes Map cannot be null"));
    }

    /* --------------------------------------------------------------
     *  Exact match takes precedence over inheritance search
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("Exact match is returned even when closer inheritance exists")
    void testFindClosest_ExactMatch() {
        Map<Class<?>, String> candidates = new HashMap<>();
        candidates.put(Number.class, "number");
        candidates.put(Integer.class, "exact");
        String result = ClassUtilities.findClosest(Integer.class, candidates, "default");
        assertEquals("exact", result, "Exact match should be returned before any inheritance logic");
    }

    /* --------------------------------------------------------------
     *  Closest inheritance distance is selected
     * -------------------------------------------------------------- */
//     @Test
//     @DisplayName("Closest inheritance distance is chosen")
//     void testFindClosest_ClosestInheritance() {
//         Map<Class<?>, String> candidates = new HashMap<>();
//         candidates.put(Object.class, "object");
//         candidates.put(Collection.class, "collection");
//         candidates.put(List.class, "list");
//         candidates.put(Iterable.class, "iterable");
//         // ArrayList extends List -> Collection -> Iterable -> Object
//         String result = ClassUtilities.findClosest(ArrayList.class, candidates, "default");
//         assertEquals("list", result, "List is the closest ancestor of ArrayList");
//     }

    /* --------------------------------------------------------------
     *  When no candidate is related, default is returned
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("Default value is returned when no candidate matches")
    void testFindClosest_NoMatchReturnsDefault() {
        Map<Class<?>, String> candidates = new HashMap<>();
        candidates.put(Runnable.class, "runnable");
        candidates.put(Cloneable.class, "cloneable");
        // String does not implement Runnable nor Cloneable
        String result = ClassUtilities.findClosest(String.class, candidates, "default");
        assertEquals("default", result, "Default should be returned when no candidate is related");
    }

    /* --------------------------------------------------------------
     *  Candidates with a distance of -1 are ignored
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("Candidates with no inheritance relationship are ignored")
    void testFindClosest_IgnoresUnrelatedCandidates() {
        Map<Class<?>, String> candidates = new HashMap<>();
        // unrelated to Integer
        candidates.put(Runnable.class, "runnable");
        // related (distance 1)
        candidates.put(Number.class, "number");
        String result = ClassUtilities.findClosest(Integer.class, candidates, "default");
        assertEquals("number", result, "Only related candidates should be considered");
    }

    /* --------------------------------------------------------------
     *  Tie‑breaking – when two candidates have the same distance,
     *  the method prefers the candidate that is a subclass of the other
     *  (or the newer candidate as defined by the private helper).
     *  This test verifies the current observable behaviour.
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("Tie‑breaking prefers the more specific candidate")
    void testFindClosest_TieBreaking() {
        Map<Class<?>, String> candidates = new HashMap<>();
        // Both Number and Comparable are one step away from Integer
        candidates.put(Number.class, "number");
        candidates.put(Comparable.class, "comparable");
        // According to the current implementation, Number (a class) is preferred over Comparable (an interface)
        String result = ClassUtilities.findClosest(Integer.class, candidates, "default");
        assertEquals("number", result, "When distances are equal, the more concrete class should be chosen");
    }

    /* --------------------------------------------------------------
     *  Tie‑breaking – later entry in a LinkedHashMap wins when distances equal
     * -------------------------------------------------------------- */
//     @Test
//     @DisplayName("Tie‑breaking prefers later candidate when distances are equal")
//     void testFindClosest_TieBreaking_LaterCandidateWins() {
//         // LinkedHashMap preserves insertion order, which the implementation uses to decide
//         // which candidate is “newer” when distances are identical.
//         Map<Class<?>, String> candidates = new LinkedHashMap<>();
//         candidates.put(Cloneable.class, "clone");
//         // later entry, same distance (1)
//         candidates.put(Serializable.class, "ser");
//         String result = ClassUtilities.findClosest(ArrayList.class, candidates, "def");
//         assertEquals("ser", result, "When distances are equal, the later candidate (newer entry) should be chosen");
//     }

    /* --------------------------------------------------------------
     *  Tie‑breaking – prefers more specific class over interface
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("Tie‑breaking prefers the more specific candidate (class over interface)")
    void testFindClosest_TieBreaking_MoreSpecificClass() {
        Map<Class<?>, String> candidates = new HashMap<>();
        // Both Number and Comparable are one step away from Integer
        candidates.put(Number.class, "number");
        candidates.put(Comparable.class, "comparable");
        // According to the current implementation, Number (a concrete class) is preferred over Comparable (an interface)
        String result = ClassUtilities.findClosest(Integer.class, candidates, "default");
        assertEquals("number", result, "When distances are equal, the more concrete class should be chosen");
    }

    /* --------------------------------------------------------------
     *  Empty candidate map returns default
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("Empty candidate map returns the default value")
    void testFindClosest_EmptyMapReturnsDefault() {
        Map<Class<?>, String> candidates = Collections.emptyMap();
        String result = ClassUtilities.findClosest(String.class, candidates, "def");
        assertEquals("def", result, "Default must be returned when the candidate map is empty");
    }

    /* --------------------------------------------------------------
     *  Source is an interface – find closest super‑interface
     * -------------------------------------------------------------- */
//     @Test
//     @DisplayName("Interface source finds closest super‑interface")
//     void testFindClosest_InterfaceSource() {
//         Map<Class<?>, String> candidates = new HashMap<>();
//         candidates.put(Collection.class, "col");
//         candidates.put(Iterable.class, "it");
//         // List extends Collection which extends Iterable
//         String result = ClassUtilities.findClosest(List.class, candidates, "def");
//         assertEquals("col", result, "Closest super‑interface (Collection) should be selected");
//     }

    /* --------------------------------------------------------------
     *  Primitive source class – no match, return default
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("Primitive source class returns default when no match")
    void testFindClosest_PrimitiveSourceReturnsDefault() {
        Map<Class<?>, String> candidates = new HashMap<>();
        candidates.put(Integer.class, "wrap");
        String result = ClassUtilities.findClosest(int.class, candidates, "def");
        assertEquals("def", result, "Primitive types are not assignable to wrapper classes, so default must be returned");
    }

    /* --------------------------------------------------------------
     *  Null key in candidate map – should raise an exception
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("Null key in candidate map causes exception")
    void testFindClosest_NullKeyInMapThrows() {
        Map<Class<?>, String> candidates = new HashMap<>();
        candidates.put(null, "nullKeyValue");
        candidates.put(Integer.class, "int");
        assertThrows(RuntimeException.class, () -> ClassUtilities.findClosest(String.class, candidates, "def"));
    }

    /* --------------------------------------------------------------
     *  Parameterized tests generated from the JSON description
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("provideFindClosestCases")
    @DisplayName("Parameterized findClosest scenarios from JSON")
    void testFindClosest_Parameterized(String description, Class<?> clazz, Map<Class<?>, String> candidateMap, String defaultValue, Object expected) {
        if (expected instanceof Class && Throwable.class.isAssignableFrom((Class<?>) expected)) {
            // Expected an exception
            assertThrows((Class<? extends Throwable>) expected, () -> ClassUtilities.findClosest(clazz, candidateMap, defaultValue));
        } else {
            String result = ClassUtilities.findClosest(clazz, candidateMap, defaultValue);
            assertEquals(expected, result, description);
        }
    }

    /**
     * Helper to create a mutable HashMap from entries
     */
    @SafeVarargs
    private static <K, V> Map<K, V> mapOf(Map.Entry<K, V>... entries) {
        Map<K, V> map = new HashMap<>();
        for (Map.Entry<K, V> e : entries) {
            map.put(e.getKey(), e.getValue());
        }
        return map;
    }

    /**
     * Helper to create a LinkedHashMap preserving insertion order
     */
    @SafeVarargs
    private static <K, V> Map<K, V> linkedMapOf(Map.Entry<K, V>... entries) {
        Map<K, V> map = new LinkedHashMap<>();
        for (Map.Entry<K, V> e : entries) {
            map.put(e.getKey(), e.getValue());
        }
        return map;
    }
}