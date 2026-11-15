package com.cedarsoftware.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

class ClassUtilities_getClassLoader_2_Test {

    /**
     * Holds the original context class loader so we can restore it after each test.
     */
    private ClassLoader originalContextLoader;

    //     @BeforeEach
    void setupBeforeEach() {
        originalContextLoader = Thread.currentThread().getContextClassLoader();
    }

    //     @AfterEach
    void teardownAfterEach() {
        Thread.currentThread().setContextClassLoader(originalContextLoader);
    }

    /**
     * Returns {@code true} if the supplied method has a {@code void} return type.
     *
     * @param method the method to inspect
     * @return {@code true} if the method returns {@code void}
     */
    public static boolean returnsVoid(Method method) {
        return method.getReturnType() == void.class;
    }

    /**
     * Returns {@code true} if the supplied method is {@code public}.
     *
     * @param method the method to inspect
     * @return {@code true} if the method is public
     */
    public static boolean isPublic(Method method) {
        return Modifier.isPublic(method.getModifiers());
    }

    /**
     * Returns {@code true} if the supplied method is {@code static}.
     *
     * @param method the method to inspect
     * @return {@code true} if the method is static
     */
    public static boolean isStatic(Method method) {
        return Modifier.isStatic(method.getModifiers());
    }

    /**
     * Returns {@code true} if the supplied method is annotated with the given
     * annotation type.
     *
     * @param method     the method to inspect
     * @param annotation the annotation class to look for
     * @return {@code true} if the method is annotated with {@code annotation}
     */
    public static boolean isAnnotated(Method method, Class<? extends Annotation> annotation) {
        return method.isAnnotationPresent(annotation);
    }

    /**
     * Finds a declared method with the given name and parameter types in the
     * supplied class.  If no such method exists {@code null} is returned.
     *
     * @param clazz          the class to search
     * @param name           the method name
     * @param parameterTypes the parameter types
     * @return the {@link Method} or {@code null} if not found
     */
    public static Method findMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        try {
            return clazz.getDeclaredMethod(name, parameterTypes);
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    /**
     * Returns {@code true} if a method with the given name and parameter types
     * exists in the supplied class.
     *
     * @param clazz          the class to search
     * @param name           the method name
     * @param parameterTypes the parameter types
     * @return {@code true} if such a method is present
     */
    public static boolean isMethodPresent(Class<?> clazz, String name, Class<?>... parameterTypes) {
        return findMethod(clazz, name, parameterTypes) != null;
    }
}
