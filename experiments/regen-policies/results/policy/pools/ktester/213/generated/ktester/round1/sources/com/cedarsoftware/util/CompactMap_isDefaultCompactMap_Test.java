package com.cedarsoftware.util;

import java.util.Map;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CompactMap_isDefaultCompactMap_Test {

    /* --------------------------------------------------------------------- */
    private static Stream<Arguments> defaultConfigProvider() {
        return Stream.of(Arguments.of(Collections.emptyMap(), true));
    }

    /* --------------------------------------------------------------------- */
    private static Stream<Arguments> compactSizeProvider() {
        return Stream.of(Arguments.of(mapOf(CompactMap.COMPACT_SIZE, 100), false));
    }

    /* --------------------------------------------------------------------- */
    private static Stream<Arguments> caseInsensitiveProvider() {
        return Stream.of(Arguments.of(mapOf(CompactMap.CASE_SENSITIVE, false), false));
    }

    /* --------------------------------------------------------------------- */
    private static Stream<Arguments> orderingProvider() {
        return Stream.of(Arguments.of(mapOf(CompactMap.ORDERING, "sorted"), false), Arguments.of(mapOf(CompactMap.ORDERING, "insertion"), false), Arguments.of(mapOf(CompactMap.ORDERING, "reverse"), false));
    }

    /* --------------------------------------------------------------------- */
    private static Stream<Arguments> singleKeyProvider() {
        return Stream.of(Arguments.of(mapOf(CompactMap.SINGLE_KEY, "uid"), false));
    }

    /* --------------------------------------------------------------------- */
    private static Stream<Arguments> backingMapProvider() {
        return Stream.of(Arguments.of(mapOf(CompactMap.MAP_TYPE, "java.util.LinkedHashMap"), false), Arguments.of(mapOf(CompactMap.MAP_TYPE, "java.util.TreeMap"), false));
    }

    /* --------------------------------------------------------------------- */
    private static Stream<Arguments> multipleOverridesProvider() {
        Map<String, Object> opts = new HashMap<>();
        opts.put(CompactMap.COMPACT_SIZE, 30);
        opts.put(CompactMap.ORDERING, "sorted");
        opts.put(CompactMap.CASE_SENSITIVE, false);
        opts.put(CompactMap.SINGLE_KEY, "uid");
        opts.put(CompactMap.MAP_TYPE, "java.util.LinkedHashMap");
        return Stream.of(Arguments.of(opts, false));
    }

    /* --------------------------------------------------------------------- */
    private static Map<String, Object> mapOf(Object... keyVals) {
        if (keyVals.length % 2 != 0) {
            throw new IllegalArgumentException("Even number of arguments required");
        }
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keyVals.length; i += 2) {
            map.put((String) keyVals[i], keyVals[i + 1]);
        }
        return map;
    }

    /**
     * Finds a public method with the given name and parameter types on the
     * supplied class. Returns {@code null} if no such method exists.
     */
    public static java.lang.reflect.Method findMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        try {
            return clazz.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * Returns {@code true} if a public method with the given signature is
     * present on the supplied class.
     */
    public static boolean isMethodPresent(Class<?> clazz, String name, Class<?>... parameterTypes) {
        return findMethod(clazz, name, parameterTypes) != null;
    }

    /**
     * Determines whether the supplied method returns {@code void}.
     */
    public static boolean returnsVoid(java.lang.reflect.Method method) {
        return method != null && void.class.equals(method.getReturnType());
    }

    /**
     * The default configuration of {@link CompactMap} should satisfy all
     * conditions checked by {@code isDefaultCompactMap()}.
     */
    @Test
    void testIsDefaultCompactMapReturnsTrueForDefaultConfig() {
        CompactMap<String, Object> map = new CompactMap<>();
        assertTrue(map.isDefaultCompactMap(), "Default CompactMap should be recognised as the default configuration");
    }

    /**
     * Changing the compact size should cause the method to return {@code false}.
     */
    @Test
    void testIsDefaultCompactMapReturnsFalseWhenCompactSizeChanged() {
        CompactMap<String, Object> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                // non‑default
                return 10;
            }
        };
        assertFalse(map.isDefaultCompactMap(), "CompactMap with a non‑default compactSize should not be recognised as default");
    }

    /**
     * Making the map case‑insensitive should cause the method to return {@code false}.
     */
    @Test
    void testIsDefaultCompactMapReturnsFalseWhenCaseInsensitive() {
        CompactMap<String, Object> map = new CompactMap<>() {

            @Override
            protected boolean isCaseInsensitive() {
                // non‑default (default is false)
                return true;
            }
        };
        assertFalse(map.isDefaultCompactMap(), "Case‑insensitive CompactMap should not be recognised as default");
    }

    /**
     * Changing the ordering from {@code "unordered"} to any other value should cause {@code false}.
     */
    @Test
    void testIsDefaultCompactMapReturnsFalseWhenOrderingChanged() {
        CompactMap<String, Object> map = new CompactMap<>() {

            @Override
            protected String getOrdering() {
                // non‑default ordering
                return "sorted";
            }
        };
        assertFalse(map.isDefaultCompactMap(), "CompactMap with a non‑default ordering should not be recognised as default");
    }

    /**
     * Changing the single‑value key from {@code "id"} to another key should cause {@code false}.
     */
    @Test
    void testIsDefaultCompactMapReturnsFalseWhenSingleKeyChanged() {
        CompactMap<String, Object> map = new CompactMap<>() {

            @Override
            protected String getSingleValueKey() {
                // non‑default key
                return "uid";
            }
        };
        assertFalse(map.isDefaultCompactMap(), "CompactMap with a non‑default singleValueKey should not be recognised as default");
    }

    /**
     * Using a backing map type other than {@link HashMap} should cause {@code false}.
     */
    @Test
    void testIsDefaultCompactMapReturnsFalseWhenBackingMapChanged() {
        CompactMap<String, Object> map = new CompactMap<>() {

            @Override
            protected Map<String, Object> getNewMap() {
                // non‑default backing map
                return new LinkedHashMap<>();
            }
        };
        assertFalse(map.isDefaultCompactMap(), "CompactMap with a non‑HashMap backing should not be recognised as default");
    }

    /* --------------------------------------------------------------------- */
    @SuppressWarnings("unchecked")
    private static CompactMap<?, ?> createMap(Map<String, Object> options) {
        // The library provides a static factory that builds the appropriate
        // specialized CompactMap subclass based on the supplied options.
        return CompactMap.newMap(options);
    }

    @ParameterizedTest(name = "Default config → expected={1}")
    @MethodSource("defaultConfigProvider")
    void testIsDefaultCompactMap_AllDefaults(Map<String, Object> options, boolean expected) {
        CompactMap<?, ?> map = createMap(options);
        assertEquals(expected, map.isDefaultCompactMap(), "A CompactMap created with no overrides must be recognised as the default configuration");
    }

    @ParameterizedTest(name = "compactSize={0} → expected={1}")
    @MethodSource("compactSizeProvider")
    void testIsDefaultCompactMap_NonDefaultCompactSize(Map<String, Object> options, boolean expected) {
        CompactMap<?, ?> map = createMap(options);
        assertEquals(expected, map.isDefaultCompactMap(), "Changing compactSize away from the default must cause isDefaultCompactMap() to return false");
    }

    @ParameterizedTest(name = "caseSensitive={0} → expected={1}")
    @MethodSource("caseInsensitiveProvider")
    void testIsDefaultCompactMap_CaseInsensitive(Map<String, Object> options, boolean expected) {
        CompactMap<?, ?> map = createMap(options);
        assertEquals(expected, map.isDefaultCompactMap(), "A case‑insensitive map must not be considered the default configuration");
    }

    @ParameterizedTest(name = "ordering={0} → expected={1}")
    @MethodSource("orderingProvider")
    void testIsDefaultCompactMap_NonDefaultOrdering(Map<String, Object> options, boolean expected) {
        CompactMap<?, ?> map = createMap(options);
        assertEquals(expected, map.isDefaultCompactMap(), "Any ordering other than \"unordered\" must cause isDefaultCompactMap() to return false");
    }

    @ParameterizedTest(name = "singleKey={0} → expected={1}")
    @MethodSource("singleKeyProvider")
    void testIsDefaultCompactMap_CustomSingleKey(Map<String, Object> options, boolean expected) {
        CompactMap<?, ?> map = createMap(options);
        assertEquals(expected, map.isDefaultCompactMap(), "Changing the single‑value key must cause isDefaultCompactMap() to return false");
    }

    @ParameterizedTest(name = "mapType={0} → expected={1}")
    @MethodSource("backingMapProvider")
    void testIsDefaultCompactMap_NonHashMapBacking(Map<String, Object> options, boolean expected) throws ClassNotFoundException {
        // Convert the string class name to an actual Class object as required by the API.
        if (options.containsKey(CompactMap.MAP_TYPE)) {
            String className = (String) options.get(CompactMap.MAP_TYPE);
            Class<?> mapClass = Class.forName(className);
            options.put(CompactMap.MAP_TYPE, mapClass);
        }
        CompactMap<?, ?> map = createMap(options);
        assertEquals(expected, map.isDefaultCompactMap(), "Using a backing map type other than HashMap must cause isDefaultCompactMap() to return false");
    }

    @ParameterizedTest(name = "multiple overrides → expected={1}")
    @MethodSource("multipleOverridesProvider")
    void testIsDefaultCompactMap_MultipleOverrides(Map<String, Object> options, boolean expected) throws ClassNotFoundException {
        // Resolve possible class name for MAP_TYPE as in the previous test.
        if (options.containsKey(CompactMap.MAP_TYPE) && options.get(CompactMap.MAP_TYPE) instanceof String) {
            String className = (String) options.get(CompactMap.MAP_TYPE);
            options.put(CompactMap.MAP_TYPE, Class.forName(className));
        }
        CompactMap<?, ?> map = createMap(options);
        assertEquals(expected, map.isDefaultCompactMap(), "When several configuration options are overridden the method must still return false");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testIsDefaultCompactMap_NegativeCompactSizeThrows() {
        Map<String, Object> options = mapOf(CompactMap.COMPACT_SIZE, -5);
        assertThrows(IllegalArgumentException.class, () -> createMap(options), "A negative compactSize must be rejected with IllegalArgumentException");
    }
}
