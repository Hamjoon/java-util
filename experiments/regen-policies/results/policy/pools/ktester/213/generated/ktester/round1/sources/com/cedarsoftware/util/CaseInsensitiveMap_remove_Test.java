package com.cedarsoftware.util;

class CaseInsensitiveMap_remove_Test {

    /**
     * Map used in the majority of tests – a simple {@code String → String} map.
     */
    private CaseInsensitiveMap<String, String> stringMap;

    /**
     * Map used for non‑String key tests.
     */
    private CaseInsensitiveMap<Object, String> mixedMap;

    /**
     * Simple container for the arguments of a parameterised test case.
     */
    private static class ArgumentsHolder {

        // may be null – test creates its own map
        final CaseInsensitiveMap<String, String> map;

        final Object key;

        // null or a concrete value
        final Object expectedValue;

        // null or a Class<? extends Throwable>
        final Object expectedException;

        final String description;

        ArgumentsHolder(CaseInsensitiveMap<String, String> map, Object key, Object expected, String description) {
            this.map = map;
            this.key = key;
            if (expected instanceof Class) {
                this.expectedException = expected;
                this.expectedValue = null;
            } else {
                this.expectedException = null;
                this.expectedValue = expected;
            }
            this.description = description;
        }
    }

    //     @BeforeAll
    static void setupBeforeAll() {
        // Ensure the cache is in a known state for all tests.
        CaseInsensitiveMap.setMaxCacheLengthString(100);
        CaseInsensitiveMap.replaceCache(new LRUCache<>(5000, LRUCache.StrategyType.THREADED));
    }

    //     @BeforeEach
    void setupBeforeEach() {
        stringMap = new CaseInsensitiveMap<>();
        stringMap.put("One", "First");
        stringMap.put("Two", "Second");
        stringMap.put("Three", "Third");
        mixedMap = new CaseInsensitiveMap<>();
        mixedMap.put(42, "Answer");
        mixedMap.put(null, "NullValue");
    }

    //     @AfterEach
    void teardownAfterEach() {
        // Clear the maps so that a failed test does not affect the next one.
        if (stringMap != null) {
            stringMap.clear();
        }
        if (mixedMap != null) {
            mixedMap.clear();
        }
    }

    //     @AfterAll
    static void teardownAfterAll() {
        // Restore defaults – this mirrors the cleanup performed in the original test suite.
        CaseInsensitiveMap.setMaxCacheLengthString(100);
        CaseInsensitiveMap.replaceCache(new LRUCache<>(5000, LRUCache.StrategyType.THREADED));
    }

    /**
     * Creates a {@link CaseInsensitiveMap} pre‑populated with the supplied entries.
     */
    private static CaseInsensitiveMap<String, String> createMap(Object[][] entries) {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        for (Object[] e : entries) {
            @SuppressWarnings("unchecked")
            String k = (String) e[0];
            @SuppressWarnings("unchecked")
            String v = (String) e[1];
            map.put(k, v);
        }
        return map;
    }

    /* --------------------------------------------------------------------- */
//     public static boolean isPublic(Method method) {
//         return Modifier.isPublic(method.getModifiers());
//     }

//     public static boolean isStatic(Method method) {
//         return Modifier.isStatic(method.getModifiers());
//     }

//     public static boolean isAbstract(Method method) {
//         return Modifier.isAbstract(method.getModifiers());
//     }

//     public static boolean isPublic(Class<?> clazz) {
//         return Modifier.isPublic(clazz.getModifiers());
//     }

    /* --------------------------------------------------------------------- */
//     public static boolean isAnnotated(Method method, Class<? extends Annotation> annotation) {
//         return method.isAnnotationPresent(annotation);
//     }

//     public static boolean isAnnotated(Class<?> clazz, Class<? extends Annotation> annotation) {
//         return clazz.isAnnotationPresent(annotation);
//     }

    /* --------------------------------------------------------------------- */
//     public static boolean returnsVoid(Method method) {
//         return method != null && void.class.equals(method.getReturnType());
//     }

    public static boolean isConstructorPresent(Class<?> clazz, Class<?>... parameterTypes) {
        try {
            clazz.getDeclaredConstructor(parameterTypes);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /* --------------------------------------------------------------------- */
//     public static Method findMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
//         try {
//             return clazz.getMethod(name, parameterTypes);
//         } catch (NoSuchMethodException e) {
//             return null;
//         }
//     }

    /* --------------------------------------------------------------------- */
//     public static void makeAccessible(AccessibleObject ao) {
//         if (!ao.canAccess(null)) {
//             ao.setAccessible(true);
//         }
//     }

    /**
     * Checks whether a public method with the given name is present on the
     * supplied class.
     */
//     public static boolean isMethodPresent(Class<?> clazz, String name) {
//         for (Method m : clazz.getMethods()) {
//             if (m.getName().equals(name)) {
//                 return true;
//             }
//         }
//         return false;
//     }
}