package com.cedarsoftware.util;

import java.lang.reflect.Method;

// @ExtendWith(MockitoExtension.class)
class CompactMap_isDefaultCompactMap_Test {

    /**
     * Creates a {@link CompactMap} using the default builder and then applies the
     * supplied configuration via {@link CompactMap#withConfig(Map)}.
     *
     * @param config configuration map (may be empty or {@code null})
     * @return a CompactMap instance ready for testing
     */
//     private static CompactMap<String, Object> buildMapWithConfig(Map<String, Object> config) {
//         CompactMap<String, Object> map = CompactMap.<String, Object>builder().build();
//         if (config != null && !config.isEmpty()) {
//             map = map.withConfig(config);
//         }
//         return map;
//     }

//     private static Stream<Arguments> branchCoverageProvider() {
//         return // Default configuration – should be true
//         Stream.// Default configuration – should be true
//         of(// Non‑default compact size – false
//         Arguments.of("Default configuration (no options)", Collections.emptyMap(), true), // Case‑insensitive – false
//         Arguments.of("Custom compactSize (100)", Collections.singletonMap("compactSize", 100), false), // Sorted ordering – false
//         Arguments.of("Case‑insensitive map", Collections.singletonMap("caseSensitive", false), false), // Different single‑value key – false
//         Arguments.of("Sorted ordering", Collections.singletonMap("ordering", "sorted"), false), // Non‑HashMap backing – false
//         Arguments.of("Single key set to 'uuid'", Collections.singletonMap("singleKey", "uuid"), false), // Explicit null ordering – false
//         Arguments.of("Backing map type TreeMap", Collections.singletonMap("mapType", "java.util.TreeMap"), false), Arguments.of("Ordering explicitly set to null", Collections.singletonMap("ordering", null), false));
//     }

//     private static Stream<Arguments> additionalScenariosProvider() {
//         return // Explicit default compact size
//         Stream.// Explicit default compact size
//         of(// Explicit default ordering
//         Arguments.of("Explicit compactSize = 50 (default)", Collections.singletonMap("compactSize", 50), true), // Explicit default single key
//         Arguments.of("Explicit ordering = 'unordered'", Collections.singletonMap("ordering", "unordered"), true), // Explicit default map type (class name)
//         Arguments.of("Explicit singleKey = 'id'", Collections.singletonMap("singleKey", "id"), true), // Non‑default map type (LinkedHashMap)
//         Arguments.of("Explicit mapType = 'java.util.HashMap'", Collections.singletonMap("mapType", "java.util.HashMap"), true), // Ordering case mismatch
//         Arguments.of("mapType = 'java.util.LinkedHashMap'", Collections.singletonMap("mapType", "java.util.LinkedHashMap"), false), // Correct ordering but TreeMap backing
//         Arguments.of("ordering = 'UNORDERED' (wrong case)", Collections.singletonMap("ordering", "UNORDERED"), false), Arguments.of("ordering = 'unordered', mapType = TreeMap", new HashMap<String, Object>() {
// 
//             {
//                 put("ordering", "unordered");
//                 put("mapType", "java.util.TreeMap");
//             }
//         }, // Compact size zero (non‑default)
//         false), // All defaults supplied explicitly
//         Arguments.of("compactSize = 0", Collections.singletonMap("compactSize", 0), false), Arguments.of("All defaults explicitly supplied", new HashMap<String, Object>() {
// 
//             {
//                 put("caseSensitive", true);
//                 put("ordering", "unordered");
//                 put("compactSize", 50);
//                 put("singleKey", "id");
//                 put("mapType", "java.util.HashMap");
//             }
//         }, // All defaults except backing map (LinkedHashMap)
//         true), Arguments.of("All defaults except mapType = LinkedHashMap", new HashMap<String, Object>() {
// 
//             {
//                 put("caseSensitive", true);
//                 put("ordering", "unordered");
//                 put("compactSize", 50);
//                 put("singleKey", "id");
//                 put("mapType", "java.util.LinkedHashMap");
//             }
//         }, false));
//     }

//     private static Stream<Arguments> exceptionScenariosProvider() {
//         return // Non‑numeric compactSize
//         Stream.// Non‑numeric compactSize
//         of(// caseSensitive supplied as String
//         Arguments.of("compactSize = 'fifty' (String)", Collections.singletonMap("compactSize", "fifty"), NumberFormatException.class), // Non‑existent map type class
//         Arguments.of("caseSensitive = 'true' (String)", Collections.singletonMap("caseSensitive", "true"), ClassCastException.class), // mapType = java.lang.String (does not implement Map)
//         Arguments.of("mapType = 'java.util.NonExistentMap'", Collections.singletonMap("mapType", "java.util.NonExistentMap"), ClassNotFoundException.class), // mapType explicitly null
//         Arguments.of("mapType = java.lang.String", Collections.singletonMap("mapType", "java.lang.String"), ClassCastException.class), // ordering supplied as a Map object
//         Arguments.of("mapType = null", Collections.singletonMap("mapType", null), NullPointerException.class), // singleKey supplied as Integer
//         Arguments.of("ordering = {} (Map instead of String)", Collections.singletonMap("ordering", new HashMap<>()), ClassCastException.class), // Negative compactSize (validation error)
//         Arguments.of("singleKey = 12345 (Integer)", Collections.singletonMap("singleKey", 12345), ClassCastException.class), Arguments.of("compactSize = -1 (negative)", Collections.singletonMap("compactSize", -1), IllegalArgumentException.class));
//     }

    /**
     * Returns {@code true} if the supplied method has a {@code void} return type.
     *
     * @param method the method to inspect
     * @return {@code true} if the method returns {@code void}
     */
    public static boolean returnsVoid(Method method) {
        return method != null && void.class.equals(method.getReturnType());
    }

    /**
     * Finds a public method with the given name and parameter types on the supplied class.
     *
     * @param clazz          the class to search
     * @param name           the method name
     * @param parameterTypes the parameter types (may be empty)
     * @return an {@link Optional} containing the {@link Method} if found, otherwise empty
     */
//     public static Optional<Method> findMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
//         try {
//             return Optional.of(clazz.getMethod(name, parameterTypes));
//         } catch (NoSuchMethodException e) {
//             return Optional.empty();
//         }
//     }

    /**
     * Checks whether a public method with the given name and parameter types exists on the class.
     *
     * @param clazz          the class to inspect
     * @param name           the method name
     * @param parameterTypes the parameter types (may be empty)
     * @return {@code true} if such a method exists, {@code false} otherwise
     */
    public static boolean isMethodPresent(Class<?> clazz, String name, Class<?>... parameterTypes) {
        try {
            clazz.getMethod(name, parameterTypes);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /**
     * The default configuration (case‑sensitive, unordered, compact size 50,
     * single key "id", backing map HashMap) should be recognised as a
     * default CompactMap.
     */
//     @Test
//     void defaultConfiguration_isDefaultCompactMap() {
//         CompactMap<String, Object> map = CompactMap.<String, Object>builder().build();
//         assertTrue(map.isDefaultCompactMap(), "Default CompactMap should report true for isDefaultCompactMap()");
//     }

    /**
     * Changing the compact size should cause the method to return false.
     */
//     @Test
//     void nonDefaultCompactSize_isNotDefaultCompactMap() {
//         CompactMap<String, Object> map = CompactMap.<String, Object>builder().compactSize(100).build();
//         assertFalse(map.isDefaultCompactMap(), "Changing compactSize should make isDefaultCompactMap() return false");
//     }

    /**
     * Making the map case‑insensitive should cause the method to return false.
     */
//     @Test
//     void caseInsensitive_isNotDefaultCompactMap() {
//         CompactMap<String, Object> map = CompactMap.<String, Object>builder().caseSensitive(false).build();
//         assertFalse(map.isDefaultCompactMap(), "Case‑insensitive map should not be considered a default CompactMap");
//     }

    /**
     * Using a sorted ordering should cause the method to return false.
     */
//     @Test
//     void sortedOrdering_isNotDefaultCompactMap() {
//         CompactMap<String, Object> map = CompactMap.<String, Object>builder().sortedOrder().build();
//         assertFalse(map.isDefaultCompactMap(), "Sorted ordering should make isDefaultCompactMap() return false");
//     }

    /**
     * Changing the single‑value key from the default "id" should cause false.
     */
//     @Test
//     void differentSingleValueKey_isNotDefaultCompactMap() {
//         CompactMap<String, Object> map = CompactMap.<String, Object>builder().singleValueKey("uuid").build();
//         assertFalse(map.isDefaultCompactMap(), "Changing the single value key should make isDefaultCompactMap() return false");
//     }

    /**
     * Using a backing map type other than HashMap should cause false.
     */
//     @Test
//     void nonHashMapBacking_isNotDefaultCompactMap() {
//         CompactMap<String, Object> map = CompactMap.<String, Object>builder().mapType(LinkedHashMap.class).build();
//         assertFalse(map.isDefaultCompactMap(), "Backing map type other than HashMap should make isDefaultCompactMap() return false");
//     }

    /**
     * Verify that the backing map really is a HashMap for the default configuration.
     * This also indirectly confirms the last condition of isDefaultCompactMap().
     */
//     @Test
//     void defaultBackingMap_isHashMap() {
//         CompactMap<String, Object> map = CompactMap.<String, Object>builder().build();
//         assertTrue(map.isDefaultCompactMap());
//         // As an extra sanity check, ensure that a map built with explicit HashMap
//         // backing also reports true.
//         CompactMap<String, Object> explicit = CompactMap.<String, Object>builder().mapType(HashMap.class).build();
//         assertTrue(explicit.isDefaultCompactMap(), "Explicitly setting mapType to HashMap should still be a default CompactMap");
//     }

//     @ParameterizedTest(name = "[{index}] {0}")
//     @MethodSource("branchCoverageProvider")
//     void branchCoverage_isDefaultCompactMap(String description, Map<String, Object> config, boolean expected) {
//         CompactMap<String, Object> map = buildMapWithConfig(config);
//         assertEquals(expected, map.isDefaultCompactMap(), () -> "Configuration: " + config + " – expected: " + expected);
//     }

//     @ParameterizedTest(name = "[{index}] {0}")
//     @MethodSource("additionalScenariosProvider")
//     void additionalScenarios_isDefaultCompactMap(String description, Map<String, Object> config, boolean expected) {
//         CompactMap<String, Object> map = buildMapWithConfig(config);
//         assertEquals(expected, map.isDefaultCompactMap(), () -> "Configuration: " + config + " – expected: " + expected);
//     }

//     @ParameterizedTest(name = "[{index}] {0}")
//     @MethodSource("exceptionScenariosProvider")
//     void exceptionScenarios_isDefaultCompactMap(String description, Map<String, Object> config, Class<? extends Throwable> expectedException) {
//         // The exception can be thrown either when applying the config or when invoking
//         // isDefaultCompactMap(). We therefore wrap both calls in the same assertThrows.
//         assertThrows(expectedException, () -> {
//             CompactMap<String, Object> map = buildMapWithConfig(config);
//             map.isDefaultCompactMap();
//         }, () -> "Configuration: " + config + " – expected exception: " + expectedException.getSimpleName());
//     }
}