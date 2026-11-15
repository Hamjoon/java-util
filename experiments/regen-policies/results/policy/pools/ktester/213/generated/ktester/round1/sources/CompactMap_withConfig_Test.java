package com.cedarsoftware.util;

import java.util.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;

class CompactMap_withConfig_Test {

    /**
     * The map that will be used as the source for {@code withConfig}.
     */
    private CompactMap<String, String> sourceMap;

    //     @BeforeEach
    void setupBeforeEach() {
        /*
         * Create a simple CompactMap instance that we can safely mutate.
         * We override {@code compactSize()} to a small value so that the
         * internal representation can change during the test without
         * needing a huge number of entries.
         */
        sourceMap = new CompactMap<>() {

            @Override
            protected int compactSize() {
                // small threshold for easier testing
                return 2;
            }

            @Override
            protected Map<String, String> getNewMap() {
                // Use a LinkedHashMap so insertion order is deterministic.
                return new java.util.LinkedHashMap<>();
            }
        };
        // Populate the source map with a few entries.
        sourceMap.put("alpha", "A");
        sourceMap.put("beta", "B");
        sourceMap.put("gamma", "C");
    }

    //     @AfterEach
    void teardownAfterEach() {
        // Clear reference to help GC; not strictly required.
        sourceMap = null;
    }

    /* --------------------------------------------------------------------- */
    private static Object invokeProtected(Object target, String methodName, Object... args) {
        try {
            Class<?>[] argTypes = Arrays.stream(args).map(Object::getClass).toArray(Class[]::new);
            Method m = target.getClass().getDeclaredMethod(methodName, argTypes);
            m.setAccessible(true);
            return m.invoke(target, args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns {@code true} if the method’s return type is {@code void}.
     */
    public static boolean returnsVoid(Method method) {
        return method.getReturnType() == void.class;
    }

    /**
     * Returns {@code true} if the method is {@code static}.
     */
//     public static boolean isStatic(Method method) {
//         return Modifier.isStatic(field.getModifiers());
//     }

    /**
     * Returns {@code true} if the method is {@code public}.
     */
//     public static boolean isPublic(Method method) {
//         return Modifier.isPublic(field.getModifiers());
//     }

    /**
     * Returns {@code true} if the method is {@code abstract}.
     */
    public static boolean isAbstract(Method method) {
        return Modifier.isAbstract(method.getModifiers());
    }

    /**
     * Returns {@code true} if the supplied annotation is present on the method.
     */
    public static boolean isAnnotationPresent(Method method, Class<? extends Annotation> annotationClass) {
        return method.isAnnotationPresent(annotationClass);
    }

    /**
     * Returns the number of parameters declared by the method.
     */
    public static int getParameterCount(Method method) {
        return method.getParameterCount();
    }

    /**
     * Returns {@code true} if the class is {@code public}.
     */
    public static boolean isPublic(Class<?> clazz) {
        return Modifier.isPublic(clazz.getModifiers());
    }

    /**
     * Returns {@code true} if the field is {@code static}.
     */
    public static boolean isStatic(Field field) {
        return Modifier.isStatic(field.getModifiers());
    }

    /**
     * Returns {@code true} if the field is {@code public}.
     */
    public static boolean isPublic(Field field) {
        return Modifier.isPublic(field.getModifiers());
    }

    /**
     * Returns {@code true} if the class is an annotation type.
     */
    public static boolean isAnnotation(Class<?> clazz) {
        return clazz.isAnnotation();
    }

    /**
     * Returns {@code true} if the class is an enum type.
     */
    public static boolean isEnum(Class<?> clazz) {
        return clazz.isEnum();
    }

    /**
     * Returns {@code true} if the class is an interface.
     */
    public static boolean isInterface(Class<?> clazz) {
        return clazz.isInterface();
    }

    /**
     * Returns {@code true} if {@code target} is assignable from {@code source}.
     */
    public static boolean isAssignableFrom(Class<?> target, Class<?> source) {
        return target.isAssignableFrom(source);
    }

    /**
     * Returns {@code true} if {@code source} is assignable to {@code target}.
     */
    public static boolean isAssignableTo(Class<?> source, Class<?> target) {
        return target.isAssignableFrom(source);
    }

    /**
     * Finds a declared method with the given name and parameter types, or {@code null} if not found.
     */
    public static Method findMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        try {
            return clazz.getDeclaredMethod(name, paramTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * Finds a declared field with the given name, or {@code null} if not found.
     */
    public static Field findField(Class<?> clazz, String name) {
        try {
            return clazz.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    /**
     * Returns a list of all declared methods in the class hierarchy.
     */
    public static List<Method> getAllDeclaredMethods(Class<?> clazz) {
        List<Method> methods = new ArrayList<>();
        while (clazz != null) {
            Collections.addAll(methods, clazz.getDeclaredMethods());
            clazz = clazz.getSuperclass();
        }
        return methods;
    }

    /**
     * Returns a list of all declared fields in the class hierarchy.
     */
    public static List<Field> getAllDeclaredFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null) {
            Collections.addAll(fields, clazz.getDeclaredFields());
            clazz = clazz.getSuperclass();
        }
        return fields;
    }
}