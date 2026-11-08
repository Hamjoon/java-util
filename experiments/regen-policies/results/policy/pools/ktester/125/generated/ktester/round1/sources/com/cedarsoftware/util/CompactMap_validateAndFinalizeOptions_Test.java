package com.cedarsoftware.util;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.params.provider.Arguments;
import static com.cedarsoftware.util.CompactMap.COMPACT_SIZE;
import java.util.HashMap;
import org.junit.jupiter.params.provider.MethodSource;
import static com.cedarsoftware.util.CompactMap.CASE_SENSITIVE;
import static com.cedarsoftware.util.CompactMap.SORTED;
import static com.cedarsoftware.util.CompactMap.MAP_TYPE;
import java.lang.reflect.Field;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.*;
import java.util.stream.Stream;
import java.util.Map;
import java.util.Arrays;
import static com.cedarsoftware.util.CompactMap.ORDERING;
import java.util.LinkedHashMap;
import org.junit.jupiter.params.ParameterizedTest;
import static com.cedarsoftware.util.CompactMap.REVERSE;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;

/**
 * Unit tests for {@link CompactMap#validateAndFinalizeOptions(java.util.Map)}.
 */
@ExtendWith(MockitoExtension.class)
class CompactMap_validateAndFinalizeOptions_Test {

    private static final int DEFAULT_COMPACT_SIZE = (int) getStaticField(CompactMap.class, "DEFAULT_COMPACT_SIZE");

    private static final boolean DEFAULT_CASE_SENSITIVE = (boolean) getStaticField(CompactMap.class, "DEFAULT_CASE_SENSITIVE");

    private static final String INNER_MAP_TYPE_KEY = (String) getStaticField(CompactMap.class, "INNER_MAP_TYPE");

    private static Stream<Arguments> compactSizeValidationProvider() {
        return Stream.of(
                Arguments.of(1, IllegalArgumentException.class, "compactSize must be >= 2"),
                Arguments.of(2, null, null));
    }

    private static Stream<Arguments> orderingMapTypeProvider() {
        return Stream.of(
                Arguments.of(null, HashMap.class),               // No ordering supplied → UNORDERED → default map (HashMap)
                Arguments.of(SORTED, TreeMap.class),            // Sorted ordering → TreeMap (or subclass)
                Arguments.of(REVERSE, TreeMap.class)           // Reverse ordering → also a TreeMap (reverse comparator)
        );
    }

    private static Stream<Arguments> caseSensitivityOrderingProvider() {
        return Stream.of(
                // insertion + caseSensitive false → wrapper expected
                Arguments.of("insertion", false, LinkedHashMap.class, true),
                // sorted + caseSensitive false → no wrapper
                Arguments.of(SORTED, false, TreeMap.class, false),
                // reverse + caseSensitive false → no wrapper
                Arguments.of(REVERSE, false, TreeMap.class, false)
        );
    }

    private static Stream<Arguments> sourceMapCompatibilityProvider() {
        // Empty unordered map (HashMap) – compatible with any ordering
        Map<String, Object> emptyHash = new HashMap<>();
        // Sorted source map (TreeMap)
        Map<String, Object> sortedTree = new TreeMap<>();
        sortedTree.put("a", 1);
        sortedTree.put("b", 2);
        // Insertion‑ordered source map (LinkedHashMap)
        Map<String, Object> insertionLinked = new LinkedHashMap<>();
        insertionLinked.put("a", 1);
        insertionLinked.put("b", 2);
        return Stream.of(
                // source unordered, ordering insertion → compatible
                Arguments.of(emptyHash, "insertion", false),
                // source unordered, ordering sorted → compatible (no ordering constraints)
                Arguments.of(emptyHash, SORTED, false),
                // source sorted, ordering insertion → conflict
                Arguments.of(sortedTree, "insertion", true),
                // source insertion‑ordered, ordering insertion → compatible
                Arguments.of(insertionLinked, "insertion", false)
        );
    }

    /* Helper methods to read private static fields of CompactMap via reflection */
    private static Object getStaticField(Class<?> clazz, String fieldName) {
        try {
            Field f = clazz.getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.get(null);
        } catch (Exception e) {
            throw new RuntimeException("Unable to access field " + fieldName + " in " + clazz, e);
        }
    }

    public static boolean returnsVoid(Method method) {
        return method.getReturnType() == void.class;
    }

    public static Method findMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        try {
            return clazz.getDeclaredMethod(name, paramTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    public static boolean isMethodPresent(Class<?> clazz, String name, Class<?>... paramTypes) {
        return findMethod(clazz, name, paramTypes) != null;
    }

    public static boolean isPublic(Method method) {
        return Modifier.isPublic(method.getModifiers());
    }

    public static boolean isStatic(Method method) {
        return Modifier.isStatic(method.getModifiers());
    }

    public static boolean isAbstract(Method method) {
        return Modifier.isAbstract(method.getModifiers());
    }

    public static boolean isPublicOrProtected(Method method) {
        int m = method.getModifiers();
        return Modifier.isPublic(m) || Modifier.isProtected(m);
    }

    public static boolean isPublicOrProtectedOrPackage(Method method) {
        int m = method.getModifiers();
        return Modifier.isPublic(m) || Modifier.isProtected(m) || !Modifier.isPrivate(m);
    }

    public static boolean isNonStatic(Method method) {
        return !isStatic(method);
    }

    public static boolean isNonAbstract(Method method) {
        return !isAbstract(method);
    }

    public static boolean isNonPrivate(Method method) {
        return !Modifier.isPrivate(method.getModifiers());
    }

    public static boolean isNonPublic(Method method) {
        return !Modifier.isPublic(method.getModifiers());
    }

    public static boolean isNonPublicOrProtected(Method method) {
        int m = method.getModifiers();
        return !(Modifier.isPublic(m) || Modifier.isProtected(m));
    }

    public static boolean isNonPublicOrProtectedOrPackage(Method method) {
        int m = method.getModifiers();
        return !(Modifier.isPublic(m) || Modifier.isProtected(m) || !Modifier.isPrivate(m));
    }

    public static Method[] getAllDeclaredMethods(Class<?> clazz) {
        return clazz.getDeclaredMethods();
    }

    public static Method[] getDeclaredMethods(Class<?> clazz) {
        return clazz.getDeclaredMethods();
    }

    public static Method getDeclaredMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        try {
            return clazz.getDeclaredMethod(name, paramTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    public static boolean isPublic(Class<?> clazz) {
        return Modifier.isPublic(clazz.getModifiers());
    }

    public static boolean isProtected(Class<?> clazz) {
        return Modifier.isProtected(clazz.getModifiers());
    }

    public static boolean isPrivate(Class<?> clazz) {
        return Modifier.isPrivate(clazz.getModifiers());
    }

    public static boolean isAbstract(Class<?> clazz) {
        return Modifier.isAbstract(clazz.getModifiers());
    }

    public static boolean isPublicOrProtected(Class<?> clazz) {
        int m = clazz.getModifiers();
        return Modifier.isPublic(m) || Modifier.isProtected(m);
    }

    public static boolean isPublicOrProtectedOrPackage(Class<?> clazz) {
        int m = clazz.getModifiers();
        return Modifier.isPublic(m) || Modifier.isProtected(m) || !Modifier.isPrivate(m);
    }

    public static boolean isNonPublic(Class<?> clazz) {
        return !Modifier.isPublic(clazz.getModifiers());
    }

    public static boolean isNonPrivate(Class<?> clazz) {
        return !Modifier.isPrivate(clazz.getModifiers());
    }

    public static boolean isNonProtected(Class<?> clazz) {
        return !Modifier.isProtected(clazz.getModifiers());
    }

    public static boolean isAssignableFrom(Class<?> superClass, Class<?> subClass) {
        return superClass.isAssignableFrom(subClass);
    }

    public static boolean isInstance(Object obj, Class<?> clazz) {
        return clazz.isInstance(obj);
    }

    public static String[] getClassNames(Class<?>[] classes) {
        return Arrays.stream(classes).map(Class::getName).toArray(String[]::new);
    }

    /**
     * Verify that an empty options map is populated with the default values
     * and that a concrete map type is resolved.
     */
    @Test
    void testDefaultsAreApplied() {
        Map<String, Object> options = new HashMap<>();
        CompactMap.validateAndFinalizeOptions(options);
        // defaults must be present
        assertEquals(DEFAULT_COMPACT_SIZE, options.get(COMPACT_SIZE));
        assertEquals(DEFAULT_CASE_SENSITIVE, options.get(CASE_SENSITIVE));
        // a map type must be resolved and be a subclass of Map
        Object mapType = options.get(MAP_TYPE);
        assertNotNull(mapType);
        assertTrue(mapType instanceof Class);
        assertTrue(Map.class.isAssignableFrom((Class<?>) mapType));
    }

    /**
     * compactSize must be >= 2, otherwise an IllegalArgumentException is thrown.
     */
    @Test
    void testInvalidCompactSizeThrows() {
        Map<String, Object> options = new HashMap<>();
        // illegal value
        options.put(COMPACT_SIZE, 1);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> CompactMap.validateAndFinalizeOptions(options));
        assertTrue(ex.getMessage().contains("compactSize must be >= 2"));
    }

    /**
     * When a source map is supplied, its ordering must be compatible with the requested ordering.
     */
    @Test
    void testOrderingConflictWithSourceMapThrows() {
        // source map is a sorted TreeMap
        Map<String, Object> source = new TreeMap<>();
        source.put("a", 1);
        source.put("b", 2);
        Map<String, Object> options = new HashMap<>();
        // request insertion ordering
        options.put(ORDERING, "insertion");
        options.put(CompactMap.SOURCE_MAP, source);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> CompactMap.validateAndFinalizeOptions(options));
        assertTrue(ex.getMessage().contains("conflicts with source map's ordering"));
    }

    /**
     * When caseSensitive is false and ordering is unordered, the map type should be wrapped
     * with {@link CaseInsensitiveMap}.
     */
    @Test
    void testCaseInsensitiveWrappingWhenUnordered() {
        Map<String, Object> options = new HashMap<>();
        options.put(CASE_SENSITIVE, false);
        options.put(ORDERING, CompactMap.UNORDERED);
        CompactMap.validateAndFinalizeOptions(options);
        assertEquals(CaseInsensitiveMap.class, options.get(MAP_TYPE));
        // inner map type should also be recorded
        assertNotNull(options.get(INNER_MAP_TYPE_KEY));
    }

    /**
     * When caseSensitive is false but ordering is sorted, the map type must NOT be wrapped
     * with {@link CaseInsensitiveMap} because the sorted map already handles ordering.
     */
    @Test
    void testCaseInsensitiveNotWrappedWhenSorted() {
        Map<String, Object> options = new HashMap<>();
        options.put(CASE_SENSITIVE, false);
        // request sorted ordering
        options.put(ORDERING, SORTED);
        CompactMap.validateAndFinalizeOptions(options);
        // MAP_TYPE must be a concrete map class, but NOT CaseInsensitiveMap
        assertNotEquals(CaseInsensitiveMap.class, options.get(MAP_TYPE));
        // inner map type should be absent because no wrapping took place
        assertNull(options.get(INNER_MAP_TYPE_KEY));
    }

    /**
     * Verify that options supplied by the caller are preserved when valid.
     */
    @Test
    void testUserSuppliedValidOptionsArePreserved() {
        Map<String, Object> options = new HashMap<>();
        options.put(COMPACT_SIZE, 100);
        options.put(CASE_SENSITIVE, false);
        options.put(ORDERING, CompactMap.UNORDERED);
        options.put(CompactMap.SINGLE_KEY, "id");
        CompactMap.validateAndFinalizeOptions(options);
        assertEquals(100, options.get(COMPACT_SIZE));
        assertEquals(false, options.get(CASE_SENSITIVE));
        assertEquals("id", options.get(CompactMap.SINGLE_KEY));
        // MAP_TYPE must still be resolved
        assertNotNull(options.get(MAP_TYPE));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] compactSize={0}")
    @MethodSource("compactSizeValidationProvider")
    void testCompactSizeValidation(Object compactSizeValue, Class<? extends Throwable> expectedException, String expectedMessageFragment) {
        Map<String, Object> options = new HashMap<>();
        options.put(COMPACT_SIZE, compactSizeValue);
        if (expectedException != null) {
            Throwable ex = assertThrows(expectedException, () -> CompactMap.validateAndFinalizeOptions(options));
            if (expectedMessageFragment != null) {
                assertTrue(ex.getMessage().contains(expectedMessageFragment));
            }
        } else {
            // No exception expected – method should succeed and preserve the supplied size
            CompactMap.validateAndFinalizeOptions(options);
            assertEquals(compactSizeValue, options.get(COMPACT_SIZE));
            // defaults for other options must be present
            assertEquals(DEFAULT_CASE_SENSITIVE, options.get(CASE_SENSITIVE));
            assertNotNull(options.get(MAP_TYPE));
        }
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] ordering={0}")
    @MethodSource("orderingMapTypeProvider")
    void testOrderingAndMapTypeDetermination(String ordering, Class<? extends Map> expectedMapSupertype) {
        Map<String, Object> options = new HashMap<>();
        if (ordering != null) {
            options.put(ORDERING, ordering);
        }
        CompactMap.validateAndFinalizeOptions(options);
        Object mapTypeObj = options.get(MAP_TYPE);
        assertNotNull(mapTypeObj);
        assertTrue(mapTypeObj instanceof Class);
        Class<?> mapType = (Class<?>) mapTypeObj;
        assertTrue(expectedMapSupertype.isAssignableFrom(mapType), "Expected MAP_TYPE to be assignable to " + expectedMapSupertype.getSimpleName() + " but was " + mapType.getName());
        // defaults must be present
        assertEquals(DEFAULT_COMPACT_SIZE, options.get(COMPACT_SIZE));
        assertEquals(DEFAULT_CASE_SENSITIVE, options.get(CASE_SENSITIVE));
        // No inner map type for default ordering paths
        assertNull(options.get(INNER_MAP_TYPE_KEY));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] ordering={0}, caseSensitive={1}")
    @MethodSource("caseSensitivityOrderingProvider")
    void testCaseSensitivityInteraction(String ordering, boolean caseSensitive, Class<?> expectedMapType, boolean expectInnerMap) {
        Map<String, Object> options = new HashMap<>();
        options.put(ORDERING, ordering);
        options.put(CASE_SENSITIVE, caseSensitive);
        CompactMap.validateAndFinalizeOptions(options);
        assertEquals(caseSensitive, options.get(CASE_SENSITIVE));
        Object mapTypeObj = options.get(MAP_TYPE);
        assertNotNull(mapTypeObj);
        assertTrue(mapTypeObj instanceof Class);
        Class<?> mapType = (Class<?>) mapTypeObj;
        if (!caseSensitive && !SORTED.equals(ordering) && !REVERSE.equals(ordering)) {
            // Should be wrapped in CaseInsensitiveMap
            assertEquals(CaseInsensitiveMap.class, mapType);
            if (expectInnerMap) {
                assertNotNull(options.get(INNER_MAP_TYPE_KEY));
            } else {
                assertNull(options.get(INNER_MAP_TYPE_KEY));
            }
        } else {
            // No wrapping – map type must be a concrete implementation (not CaseInsensitiveMap)
            assertNotEquals(CaseInsensitiveMap.class, mapType);
            assertTrue(expectedMapType.isAssignableFrom(mapType));
            assertNull(options.get(INNER_MAP_TYPE_KEY));
        }
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] source={0}, ordering={1}")
    @MethodSource("sourceMapCompatibilityProvider")
    void testSourceMapOrderingCompatibility(Map<?, ?> sourceMap, String ordering, boolean expectException) {
        Map<String, Object> options = new HashMap<>();
        options.put(ORDERING, ordering);
        options.put(CompactMap.SOURCE_MAP, sourceMap);
        if (expectException) {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> CompactMap.validateAndFinalizeOptions(options));
            assertTrue(ex.getMessage().contains("conflicts with source map's ordering"));
        } else {
            // Should succeed without exception
            CompactMap.validateAndFinalizeOptions(options);
            assertNotNull(options.get(MAP_TYPE));
        }
    }

    // -----------------------------------------------------------------
    @Test
    void testOnlyCompactSizeSupplied() {
        Map<String, Object> options = new HashMap<>();
        options.put(COMPACT_SIZE, 10);
        CompactMap.validateAndFinalizeOptions(options);
        assertEquals(10, options.get(COMPACT_SIZE));
        assertEquals(DEFAULT_CASE_SENSITIVE, options.get(CASE_SENSITIVE));
        assertNotNull(options.get(MAP_TYPE));
        assertNull(options.get(INNER_MAP_TYPE_KEY));
    }

    @Test
    void testOnlyCaseSensitiveSupplied() {
        Map<String, Object> options = new HashMap<>();
        // default ordering UNORDERED
        options.put(CASE_SENSITIVE, false);
        CompactMap.validateAndFinalizeOptions(options);
        assertEquals(false, options.get(CASE_SENSITIVE));
        assertEquals(DEFAULT_COMPACT_SIZE, options.get(COMPACT_SIZE));
        // With UNORDERED and caseSensitive false we expect a wrapper
        assertEquals(CaseInsensitiveMap.class, options.get(MAP_TYPE));
        assertNotNull(options.get(INNER_MAP_TYPE_KEY));
    }

    // -----------------------------------------------------------------
    @Test
    void testMapTypeAlreadyCaseInsensitiveMapIsUnchanged() {
        Map<String, Object> options = new HashMap<>();
        options.put(ORDERING, "insertion");
        options.put(CASE_SENSITIVE, false);
        // pre‑set
        options.put(MAP_TYPE, CaseInsensitiveMap.class);
        CompactMap.validateAndFinalizeOptions(options);
        // MAP_TYPE must stay as CaseInsensitiveMap.class
        assertEquals(CaseInsensitiveMap.class, options.get(MAP_TYPE));
        // No inner map type should be added because wrapping is unnecessary
        assertNull(options.get(INNER_MAP_TYPE_KEY));
    }

    // -----------------------------------------------------------------
    @Test
    void testInvalidOrderingTypeThrowsClassCastException() {
        Map<String, Object> options = new HashMap<>();
        // non‑String
        options.put(ORDERING, 123);
        assertThrows(ClassCastException.class, () -> CompactMap.validateAndFinalizeOptions(options));
    }

    @Test
    void testInvalidCompactSizeTypeThrowsClassCastException() {
        Map<String, Object> options = new HashMap<>();
        // non‑int
        options.put(COMPACT_SIZE, "large");
        assertThrows(ClassCastException.class, () -> CompactMap.validateAndFinalizeOptions(options));
    }

    @Test
    void testNullCompactSizeValueThrowsNullPointerException() {
        Map<String, Object> options = new HashMap<>();
        // explicit null
        options.put(COMPACT_SIZE, null);
        assertThrows(NullPointerException.class, () -> CompactMap.validateAndFinalizeOptions(options));
    }

    @Test
    void testInvalidCaseSensitiveTypeThrowsClassCastException() {
        Map<String, Object> options = new HashMap<>();
        // non‑boolean
        options.put(CASE_SENSITIVE, "yes");
        assertThrows(ClassCastException.class, () -> CompactMap.validateAndFinalizeOptions(options));
    }

    @Test
    void testInvalidSourceMapTypeThrowsClassCastException() {
        Map<String, Object> options = new HashMap<>();
        // not a Map
        options.put(CompactMap.SOURCE_MAP, List.of(1, 2, 3));
        assertThrows(ClassCastException.class, () -> CompactMap.validateAndFinalizeOptions(options));
    }

    @Test
    void testUnknownOrderingValueThrowsIllegalArgumentException() {
        Map<String, Object> options = new HashMap<>();
        // not a recognised ordering
        options.put(ORDERING, "random");
        assertThrows(IllegalArgumentException.class, () -> CompactMap.validateAndFinalizeOptions(options));
    }
}