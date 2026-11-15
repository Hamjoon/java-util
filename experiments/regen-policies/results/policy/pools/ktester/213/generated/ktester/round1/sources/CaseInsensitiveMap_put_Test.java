package com.cedarsoftware.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

class CaseInsensitiveMap_put_Test {

    private CaseInsensitiveMap<String, String> stringMap;

    private CaseInsensitiveMap<Object, String> mixedMap;

    //     @BeforeEach
    void setupBeforeEach() {
        // Fresh map for each test – default backing map (LinkedHashMap)
        stringMap = new CaseInsensitiveMap<>();
        mixedMap = new CaseInsensitiveMap<>();
    }

    //     @AfterEach
    void teardownAfterEach() {
        // Reset static cache configuration to avoid side‑effects between tests
        CaseInsensitiveMap.setMaxCacheLengthString(100);
        CaseInsensitiveMap.replaceCache(new LRUCache<>(5000, LRUCache.StrategyType.THREADED));
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

    public static boolean isPublic(Method method) {
        return Modifier.isPublic(method.getModifiers());
    }

    public static boolean isStatic(Method method) {
        return Modifier.isStatic(method.getModifiers());
    }

    public static boolean isAnnotated(Method method, Class<? extends Annotation> annotation) {
        return method.isAnnotationPresent(annotation);
    }
}
