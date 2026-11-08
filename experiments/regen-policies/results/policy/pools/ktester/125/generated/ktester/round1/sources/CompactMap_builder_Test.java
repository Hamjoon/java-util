package com.cedarsoftware.util;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.annotation.Annotation;
import java.util.concurrent.Callable;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import java.util.List;
// Static import of JUnit assertion methods
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CompactMap_builder_Test {

    /**
     * Returns {@code true} if the method’s return type is {@code void}.
     */
    public static boolean returnsVoid(Method method) {
        return method.getReturnType().equals(void.class);
    }

    /**
     * Returns {@code true} if the method is {@code public}.
     */
    public static boolean isPublic(Method method) {
        Method m = findMethod(clazz, name, parameterTypes);
        return m != null && isPublic(m);
    }

    /**
     * Returns {@code true} if the method is {@code static}.
     */
    public static boolean isStatic(Method method) {
        Method m = findMethod(clazz, name, parameterTypes);
        return m != null && isStatic(m);
    }

    /**
     * Returns {@code true} if the method is both {@code public} and {@code static}.
     */
    public static boolean isPublicStatic(Method method) {
        Method m = findMethod(clazz, name, parameterTypes);
        return m != null && isPublicStatic(m);
    }

    /**
     * Returns {@code true} if the method is {@code public static void}.
     */
    public static boolean isPublicStaticVoid(Method method) {
        Method m = findMethod(clazz, name, parameterTypes);
        return m != null && isPublicStaticVoid(m);
    }

    /**
     * Returns {@code true} if the method is annotated with the given annotation.
     */
    public static boolean isAnnotated(Method method, Class<? extends Annotation> annotation) {
        Method m = findMethod(clazz, name, parameterTypes);
        return m != null && isAnnotated(m, annotation);
    }

    /**
     * Finds a method with the given name and parameter types on the supplied class.
     * Returns {@code null} if no such method exists.
     */
    public static Method findMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        try {
            return clazz.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * Returns {@code true} if a method with the given signature exists on the class.
     */
    public static boolean isMethodPresent(Class<?> clazz, String name, Class<?>... parameterTypes) {
        return findMethod(clazz, name, parameterTypes) != null;
    }

    public static boolean isPublicStaticVoid(Class<?> clazz, String name, Class<?>... parameterTypes) {
        Method m = findMethod(clazz, name, parameterTypes);
        return m != null && isPublicStaticVoid(m);
    }

    public static boolean isPublicStatic(Class<?> clazz, String name, Class<?>... parameterTypes) {
        Method m = findMethod(clazz, name, parameterTypes);
        return m != null && isPublicStatic(m);
    }

    public static boolean isPublic(Class<?> clazz, String name, Class<?>... parameterTypes) {
        Method m = findMethod(clazz, name, parameterTypes);
        return m != null && isPublic(m);
    }

    public static boolean isStatic(Class<?> clazz, String name, Class<?>... parameterTypes) {
        Method m = findMethod(clazz, name, parameterTypes);
        return m != null && isStatic(m);
    }

    public static boolean isAnnotated(Class<?> clazz, String name, Class<? extends Annotation> annotation, Class<?>... parameterTypes) {
        Method m = findMethod(clazz, name, parameterTypes);
        return m != null && isAnnotated(m, annotation);
    }

    /**
     * Verify that the default builder creates an empty, mutable CompactMap.
     */
    @Test
    void testBuilderCreatesEmptyMap() {
        CompactMap<String, Object> map = CompactMap.<String, Object>builder().build();
        assertNotNull(map, "Builder should return a non‑null CompactMap");
        assertTrue(map.isEmpty(), "Newly built map must be empty");
        assertEquals(0, map.size(), "Size of a new map must be zero");
        // Mutability check – put a value and verify size changes.
        map.put("key", "value");
        assertEquals(1, map.size(), "Map size should reflect added entry");
        assertFalse(map.isEmpty(), "Map should no longer be empty after a put");
    }

    /**
     * The insertionOrder option must preserve the order in which entries are added.
     */
    @Test
    void testInsertionOrderPreserved() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().insertionOrder().build();
        // Insert entries in a known order.
        map.put("first", 1);
        map.put("second", 2);
        map.put("third", 3);
        // Verify iteration order of the key set matches insertion order.
        List<String> keys = new ArrayList<>(map.keySet());
        assertEquals(Arrays.asList("first", "second", "third"), keys, "Insertion order should be preserved by the map");
    }

    /**
     * The caseSensitive(false) together with sortedOrder() must treat keys case‑insensitively.
     */
    @Test
    void testCaseInsensitiveSortedOrder() {
        CompactMap<String, String> map = CompactMap.<String, String>builder().caseSensitive(false).sortedOrder().build();
        map.put("Apple", "red");
        // Adding a key that differs only by case should replace the previous entry.
        map.put("apple", "green");
        assertEquals(1, map.size(), "Case‑insensitive map should contain only one entry");
        assertEquals("green", map.get("APPLE"), "Retrieval must be case‑insensitive");
    }

    /**
     * Verify that a custom compactSize influences behaviour when the map grows beyond the threshold.
     */
    @Test
    void testCustomCompactSizeTransition() {
        // Use a very small compact size to force a transition to the backing map quickly.
        // after 2 entries we expect a backing map to be used
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().compactSize(2).build();
        // Fill the map beyond the compact size.
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);
        // The map should still behave like a normal Map.
        assertEquals(4, map.size(), "Map size must reflect all inserted entries");
        assertTrue(map.containsKey("c"));
        assertEquals(4, map.get("d"));
    }

    /**
     * Ensure that the builder can be used without a type witness by separating creation from configuration.
     */
    @Test
    void testBuilderWithoutTypeWitness() {
        // Separate the builder creation from the fluent configuration.
        CompactMap.Builder<String, Object> builder = CompactMap.builder();
        CompactMap<String, Object> map = builder.caseSensitive(false).sortedOrder().compactSize(5).build();
        assertNotNull(map);
        assertTrue(map.isEmpty());
        // Simple functional check – add and retrieve a value.
        map.put("Key", "value");
        // case‑insensitive due to configuration
        assertEquals("value", map.get("key"));
    }

    /**
     * Verify that {@code CompactMap.builder()} never returns {@code null}.
     */
    @Test
    void builderReturnsNonNullInstance() {
        CompactMap.Builder<String, Integer> builder = CompactMap.builder();
        assertNotNull(builder, "CompactMap.builder() must return a non‑null Builder instance");
    }

    /**
     * Two successive calls must produce distinct Builder objects.
     */
    @Test
    void successiveCallsYieldDistinctBuilders() {
        CompactMap.Builder<String, Integer> b1 = CompactMap.builder();
        CompactMap.Builder<String, Integer> b2 = CompactMap.builder();
        assertNotSame(b1, b2, "Each call to CompactMap.builder() should create a new Builder instance");
    }

    /**
     * The returned object must be an instance of the nested static class {@code CompactMap$Builder}.
     */
    @Test
    void builderClassNameAndEnclosingClassVerification() {
        Object builder = CompactMap.builder();
        // Class name check
        assertEquals("CompactMap$Builder", builder.getClass().getSimpleName(), "Builder class name should be \"CompactMap$Builder\"");
        // Enclosing class check via reflection
        Class<?> enclosing = builder.getClass().getEnclosingClass();
        assertNotNull(enclosing, "Builder should have an enclosing class");
        assertEquals(CompactMap.class, enclosing, "Builder's enclosing class must be CompactMap");
    }

    /**
     * The builder instance must be assignable to the generic type {@code CompactMap.Builder<?,?>}.
     */
    @Test
    void builderCastabilityCheck() {
        Object builder = CompactMap.builder();
        // The cast itself will throw ClassCastException if it fails.
        assertDoesNotThrow(() -> {
            @SuppressWarnings("unchecked")
            CompactMap.Builder<?, ?> casted = (CompactMap.Builder<?, ?>) builder;
        }, "Builder should be castable to CompactMap.Builder<?,?>");
    }

    /**
     * Verify that the Builder exposes a public no‑arg {@code build()} method returning a CompactMap.
     */
    @Test
    void builderHasPublicBuildMethod() throws Exception {
        CompactMap.Builder<String, String> builder = CompactMap.builder();
        // Locate the method via reflection
        Method buildMethod = builder.getClass().getMethod("build");
        assertNotNull(buildMethod, "Builder must have a public method named build()");
        assertTrue(Modifier.isPublic(buildMethod.getModifiers()), "build() method should be public");
        // Invoke and verify the result
        Object mapObj = buildMethod.invoke(builder);
        assertNotNull(mapObj, "build() must return a non‑null object");
        assertTrue(mapObj instanceof CompactMap, "build() should return an instance of CompactMap");
        @SuppressWarnings("unchecked")
        CompactMap<String, String> map = (CompactMap<String, String>) mapObj;
        assertTrue(map.isEmpty(), "Newly built CompactMap must be empty");
        assertEquals(0, map.size(), "Size of a new CompactMap must be zero");
    }

    /**
     * Concurrently request builders from multiple threads and ensure each is a distinct instance.
     */
    @Test
    void builderIsThreadSafeAndCreatesDistinctInstances() throws Exception {
        final int THREAD_COUNT = 10;
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        try {
            List<Callable<CompactMap.Builder<?, ?>>> tasks = new ArrayList<>();
            for (int i = 0; i < THREAD_COUNT; i++) {
                tasks.add(() -> CompactMap.builder());
            }
            List<Future<CompactMap.Builder<?, ?>>> futures = executor.invokeAll(tasks);
            List<CompactMap.Builder<?, ?>> builders = new ArrayList<>();
            for (Future<CompactMap.Builder<?, ?>> f : futures) {
                // propagate any exception
                builders.add(f.get());
            }
            // Ensure we collected the expected number of builders
            assertEquals(THREAD_COUNT, builders.size(), "Should have collected a builder from each thread");
            // Verify all references are unique
            Set<CompactMap.Builder<?, ?>> distinct = new HashSet<>(builders);
            assertEquals(THREAD_COUNT, distinct.size(), "All Builder instances obtained concurrently must be distinct");
        } finally {
            executor.shutdownNow();
        }
    }
}
