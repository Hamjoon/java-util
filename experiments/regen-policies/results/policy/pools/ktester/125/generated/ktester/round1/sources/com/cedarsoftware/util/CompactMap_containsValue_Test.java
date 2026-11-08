package com.cedarsoftware.util;

import java.lang.reflect.Method;

class CompactMap_containsValue_Test {

    /**
     * The map under test – created fresh for each test method.
     */
    private CompactMap<String, Integer> map;

    /**
     * Common configuration used for most tests (compactSize = 3).
     */
    private static final int DEFAULT_COMPACT_SIZE = 3;

    //     @BeforeEach
    void setupBeforeEach() {
        /*
         * Create an anonymous subclass that lets us control the internal thresholds.
         * The overridden methods are part of the protected API, not private members,
         * so they are safe to use in tests.
         */
        map = new CompactMap<>() {

            @Override
            protected String getSingleValueKey() {
                // The key used for the single‑value optimisation – any non‑null value works.
                return "value";
            }

            @Override
            protected int compactSize() {
                // Use a small compact size so we can easily trigger the different states.
                return DEFAULT_COMPACT_SIZE;
            }

            @Override
            protected java.util.Map<String, Integer> getNewMap() {
                // Backing map when we exceed the compact size.
                return new java.util.LinkedHashMap<>();
            }
        };
    }

    //     @AfterEach
    void teardownAfterEach() {
        // Ensure the map is cleared after each test to avoid cross‑test contamination.
        map.clear();
    }

    /**
     * Returns {@code true} if the supplied method has a {@code void} return type.
     *
     * @param method the method to inspect; may not be {@code null}
     * @return {@code true} if the method returns {@code void}
     */
    public static boolean returnsVoid(Method method) {
        return method != null && void.class.equals(method.getReturnType());
    }

    /**
     * Finds a declared method with the given name and parameter types on the
     * supplied class, returning {@code null} if no such method exists.
     *
     * @param clazz          the class to search; may not be {@code null}
     * @param name           the method name; may not be {@code null}
     * @param parameterTypes the parameter types; may be {@code null}
     * @return the {@link Method} if found, otherwise {@code null}
     */
    public static Method findMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        if (clazz == null || name == null) {
            return null;
        }
        try {
            return clazz.getDeclaredMethod(name, parameterTypes);
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    /**
     * Returns {@code true} if the class declares a method with the given
     * name and parameter types.
     *
     * @param clazz          the class to inspect; may not be {@code null}
     * @param name           the method name; may not be {@code null}
     * @param parameterTypes the parameter types; may be {@code null}
     * @return {@code true} if such a method exists
     */
    public static boolean isMethodPresent(Class<?> clazz, String name, Class<?>... parameterTypes) {
        return findMethod(clazz, name, parameterTypes) != null;
    }
}
