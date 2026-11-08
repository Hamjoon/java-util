package com.cedarsoftware.util;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

class ClassUtilities_forName_Test {

    /**
     * Store the default class loader so we can reuse it in the tests.
     */
    private static ClassLoader defaultClassLoader;

    // -----------------------------------------------------------------------
    private static final Map<Class<?>, Class<?>> primitiveWrapperMap = new HashMap<>();

    private static final Map<Class<?>, Class<?>> primitiveToWrapperMap = new HashMap<>();

    private static final Map<String, Class<?>> classAliasMap = new ConcurrentHashMap<>();

    /**
     * A ClassLoader that throws a SecurityException for any attempt to load a class.
     */
    private static class SecurityExceptionThrowingClassLoader extends ClassLoader {

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            throw new SecurityException("Simulated security failure for testing");
        }
    }

    /**
     * Simple delegating class loader that forwards to a parent.
     */
    private static class DelegatingClassLoader extends ClassLoader {

        DelegatingClassLoader(ClassLoader parent) {
            super(parent);
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            // Delegate to the parent (default) loader – normal resolution path
            return super.loadClass(name);
        }
    }

    private static class RuntimeExceptionThrowingClassLoader extends ClassLoader {

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            throw new RuntimeException("Simulated runtime exception");
        }
    }

    private static class NoClassDefFoundErrorThrowingClassLoader extends ClassLoader {

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            throw new NoClassDefFoundError("Simulated NoClassDefFoundError");
        }
    }

    private static class LinkageErrorThrowingClassLoader extends ClassLoader {

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            throw new LinkageError("Simulated LinkageError");
        }
    }

    //     @BeforeAll
    static void setupBeforeAll() {
        // safe way to obtain a non‑null loader
        defaultClassLoader = ClassUtilities.getClassLoader();
    }

    /**
     * Returns a {@link ClassLoader} that can be used for general class‑loading
     * operations.  The method first tries the thread context class loader and,
     * if that is {@code null}, falls back to the class loader that loaded this
     * utility class.
     *
     * @return a non‑null {@link ClassLoader}
     */
    public static ClassLoader getClassLoader() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return (cl != null) ? cl : ClassUtilities.class.getClassLoader();
    }

    /**
     * Given the passed in String class name, return the named JVM class.
     *
     * @param name        String name of a JVM class.
     * @param classLoader ClassLoader to use when searching for JVM classes.
     * @return Class instance of the named JVM class or {@code null} if not found.
     *
     * @throws IllegalArgumentException if a {@link SecurityException} occurs while
     *                                  loading the class.  The original exception
     *                                  is attached as the cause.
     */
    public static Class<?> forName(String name, ClassLoader classLoader) {
        // 1. Guard against null / empty name – contract says return null.
        if (name == null || name.isEmpty()) {
            return null;
        }
        // 2. The tests explicitly require a null loader to result in null,
        //    not a NullPointerException.
        if (classLoader == null) {
            return null;
        }
        try {
            // The second argument (initialize) is false – we do not want static
            // initializers to run as part of the lookup.
            return Class.forName(name, false, classLoader);
        } catch (SecurityException se) {
            // Wrap a security problem in an IllegalArgumentException while preserving the cause.
            throw new IllegalArgumentException("Security exception while loading class " + name, se);
        } catch (ClassNotFoundException cnfe) {
            // Class not found – the utility returns null rather than propagating.
            return null;
        } catch (Exception e) {
            // Any other exception (including RuntimeException from a custom loader) results in null.
            return null;
        }
        // Errors (NoClassDefFoundError, LinkageError, etc.) are not caught and therefore propagate.
    }

    /**
     * Computes the inheritance distance between two classes or interfaces.
     * Returns {@code -1} if there is no relationship.
     */
    public static int computeInheritanceDistance(Class<?> child, Class<?> parent) {
        if (child == null || parent == null) {
            return -1;
        }
        if (parent.isAssignableFrom(child)) {
            int distance = 0;
            Class<?> current = child;
            while (current != null && !parent.equals(current)) {
                distance++;
                current = current.getSuperclass();
            }
            return distance;
        }
        return -1;
    }

    /**
     * Returns {@code true} if the supplied class represents a primitive type or its wrapper.
     */
    public static boolean isPrimitive(Class<?> cls) {
        return cls != null && (cls.isPrimitive() || primitiveWrapperMap.containsKey(cls));
    }

    /**
     * Returns the wrapper class for a primitive type, or the primitive type for a wrapper.
     */
    public static Class<?> toPrimitiveWrapperClass(Class<?> cls) {
        if (cls == null) {
            return null;
        }
        if (cls.isPrimitive()) {
            return primitiveWrapperMap.get(cls);
        }
        return primitiveToWrapperMap.get(cls);
    }

    /**
     * Loads a resource from the classpath as a UTF‑8 string.
     */
    public static String loadResourceAsString(String resourcePath) {
        byte[] bytes = loadResourceAsBytes(resourcePath);
        return (bytes == null) ? null : new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Loads a resource from the classpath as a byte array.
     */
    public static byte[] loadResourceAsBytes(String resourcePath) {
        if (resourcePath == null) {
            return null;
        }
        InputStream is = getClassLoader().getResourceAsStream(resourcePath);
        if (is == null) {
            return null;
        }
        try (is) {
            return is.readAllBytes();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Adds a permanent alias for a class name.
     */
    public static void addPermanentClassAlias(String alias, Class<?> clazz) {
        if (alias != null && clazz != null) {
            classAliasMap.put(alias, clazz);
        }
    }

    /**
     * Removes a previously added permanent alias.
     */
    public static void removePermanentClassAlias(String alias) {
        if (alias != null) {
            classAliasMap.remove(alias);
        }
    }

    /**
     * Returns {@code true} if every constructor of the supplied class is private.
     */
    public static boolean areAllConstructorsPrivate(Class<?> cls) {
        if (cls == null) {
            return false;
        }
        Constructor<?>[] ctors = cls.getDeclaredConstructors();
        for (Constructor<?> c : ctors) {
            if (!Modifier.isPrivate(c.getModifiers())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Finds the most specific class in the supplied collection that is assignable from {@code target}.
     */
    public static Class<?> findClosest(Class<?> target, Collection<Class<?>> candidates) {
        if (target == null || candidates == null) {
            return null;
        }
        Class<?> best = null;
        int bestDist = Integer.MAX_VALUE;
        for (Class<?> cand : candidates) {
            int dist = computeInheritanceDistance(target, cand);
            if (dist >= 0 && dist < bestDist) {
                bestDist = dist;
                best = cand;
            }
        }
        return best;
    }

    // -----------------------------------------------------------------------
    private static Stream<Arguments> provideNullOrEmptyNames() {
        // Both cases use a null class loader as per the JSON definition
        return Stream.of(Arguments.of(null, null), Arguments.of("", null));
    }

    private static Stream<Arguments> provideValidNamesWithDefaultLoader() {
        return Stream.of(Arguments.of("java.lang.String", java.lang.String.class), Arguments.of("java.util.ArrayList", java.util.ArrayList.class));
    }

    // -----------------------------------------------------------------------
    @Test
    @DisplayName("forName returns null when name is null")
    void testForName_NullName_ReturnsNull() {
        Assertions.assertNull(ClassUtilities.forName(null, defaultClassLoader), "Expected null when class name is null");
    }

    @Test
    @DisplayName("forName returns null when name is empty")
    void testForName_EmptyName_ReturnsNull() {
        Assertions.assertNull(ClassUtilities.forName("", defaultClassLoader), "Expected null when class name is empty");
    }

    @Test
    @DisplayName("forName returns the correct Class for a known type")
    void testForName_ValidClass_ReturnsClass() {
        Class<?> result = ClassUtilities.forName("java.util.ArrayList", defaultClassLoader);
        Assertions.assertNotNull(result, "Result should not be null for a valid class name");
        Assertions.assertEquals(java.util.ArrayList.class, result, "Result should be java.util.ArrayList.class");
    }

    @Test
    @DisplayName("forName returns null when the class cannot be found")
    void testForName_ClassNotFound_ReturnsNull() {
        Class<?> result = ClassUtilities.forName("com.cedarsoftware.util.NonExistentClass", defaultClassLoader);
        Assertions.assertNull(result, "Result should be null when the class cannot be located");
    }

    @Test
    @DisplayName("forName returns null when a null ClassLoader is supplied")
    void testForName_NullClassLoader_ReturnsNull() {
        // The implementation catches generic Exception and returns null,
        // so a null loader should lead to a null result rather than a NPE.
        Class<?> result = ClassUtilities.forName("java.lang.String", null);
        Assertions.assertNull(result, "Result should be null when ClassLoader argument is null");
    }

    @Test
    @DisplayName("forName re‑throws SecurityException as IllegalArgumentException")
    void testForName_SecurityException_ThrowsIllegalArgumentException() {
        ClassLoader hostileLoader = new SecurityExceptionThrowingClassLoader();
        IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () -> ClassUtilities.forName("java.lang.String", hostileLoader), "Expected IllegalArgumentException when a SecurityException is raised");
        Assertions.assertTrue(thrown.getMessage().contains("Security exception"), "Exception message should indicate a security problem");
        Assertions.assertTrue(thrown.getCause() instanceof SecurityException, "Cause of the IllegalArgumentException should be the original SecurityException");
    }

    @ParameterizedTest(name = "[{index}] name=''{0}'' → expected null")
    @MethodSource("provideNullOrEmptyNames")
    void testForName_NullOrEmptyName_ReturnsNull(String name, ClassLoader loader) {
        Assertions.assertNull(ClassUtilities.forName(name, loader), "Expected null for name='" + name + "'");
    }

    @ParameterizedTest(name = "[{index}] name=''{0}'' → expected {1}")
    @MethodSource("provideValidNamesWithDefaultLoader")
    void testForName_ValidName_WithDefaultClassLoader(String name, Class<?> expected) {
        Class<?> result = ClassUtilities.forName(name, null);
        Assertions.assertNotNull(result, "Result should not be null for name='" + name + "'");
        Assertions.assertEquals(expected, result, "Unexpected class returned for name='" + name + "'");
    }

    @Test
    @DisplayName("forName resolves class using a custom class loader")
    void testForName_ValidName_WithCustomClassLoader() {
        ClassLoader customLoader = new DelegatingClassLoader(defaultClassLoader);
        Class<?> result = ClassUtilities.forName("java.lang.Integer", customLoader);
        Assertions.assertNotNull(result, "Result should not be null when using a custom class loader");
        Assertions.assertEquals(Integer.class, result, "Expected java.lang.Integer.class");
    }

    @Test
    @DisplayName("forName returns null when internalClassForName throws a RuntimeException")
    void testForName_RuntimeException_ReturnsNull() {
        ClassLoader runtimeExceptionLoader = new RuntimeExceptionThrowingClassLoader();
        Class<?> result = ClassUtilities.forName("java.lang.Thread", runtimeExceptionLoader);
        Assertions.assertNull(result, "Expected null when a RuntimeException is thrown inside internalClassForName");
    }

    @Test
    @DisplayName("forName propagates NoClassDefFoundError")
    void testForName_NoClassDefFoundError_Propagates() {
        ClassLoader errorLoader = new NoClassDefFoundErrorThrowingClassLoader();
        Assertions.assertThrows(NoClassDefFoundError.class, () -> ClassUtilities.forName("com.example.FaultyClass", errorLoader), "Expected NoClassDefFoundError to propagate");
    }

    @Test
    @DisplayName("forName propagates LinkageError")
    void testForName_LinkageError_Propagates() {
        ClassLoader linkageErrorLoader = new LinkageErrorThrowingClassLoader();
        Assertions.assertThrows(LinkageError.class, () -> ClassUtilities.forName("com.example.LinkageProblem", linkageErrorLoader), "Expected LinkageError to propagate");
    }
}
