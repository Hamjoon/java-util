package com.cedarsoftware.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

@ExtendWith(MockitoExtension.class)
class CompactMap_validateAndFinalizeOptions_Test {

    /**
     * Constant used by the tests to verify the internal map‑type storage key.
     * The library defines this key as {@code INNER_MAP_TYPE}, but it is not
     * publicly exposed in the version under test, so we define it locally.
     */
    private static final String INNER_MAP_TYPE = "innerMapType";

    /**
     * Creates a shallow copy of a map literal for readability.
     */
    private static Map<String, Object> map(Object... keyVals) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < keyVals.length; i += 2) {
            m.put(String.valueOf(keyVals[i]), keyVals[i + 1]);
        }
        return m;
    }

    /**
     * Supplies the test cases defined in the JSON matrix.
     */
    private static List<Arguments> orderingAndCompactSizeCases() {
        return // compactSize < 2 → IllegalArgumentException
        List.// compactSize < 2 → IllegalArgumentException
        of(// valid compactSize, default ordering (unordered) – no source map
        Arguments.of(map(ORDERING, UNORDERED, COMPACT_SIZE, 1), IllegalArgumentException.class, "compactSize must be >= 2"), // sorted ordering with a source map that is also sorted – compatible
        Arguments.of(map(ORDERING, UNORDERED, COMPACT_SIZE, 2), Map.class, null), // source map unordered but requested sorted → conflict
        Arguments.of(map(ORDERING, SORTED, COMPACT_SIZE, 10, SOURCE_MAP, new TreeMap<>(Map.of("a", 1, "b", 2))), Map.class, null), // caseSensitive false with non‑sorted ordering → wrap
        Arguments.of(map(ORDERING, SORTED, COMPACT_SIZE, 10, SOURCE_MAP, new HashMap<>(Map.of("a", 1, "b", 2))), IllegalArgumentException.class, "Requested ordering 'sorted' conflicts with source map's ordering"), // caseSensitive false but ordering sorted → no wrap
        Arguments.of(map(ORDERING, INSERTION, COMPACT_SIZE, 5, CASE_SENSITIVE, false), Map.class, null), // empty options → defaults
        Arguments.of(map(ORDERING, SORTED, COMPACT_SIZE, 5, CASE_SENSITIVE, false), Map.class, null), Arguments.of(new HashMap<>(), Map.class, null));
    }

    private static List<Arguments> explicitMapTypeOverrideCases() {
        return // supplied mapType is ignored; unordered resolves to HashMap
        List.// supplied mapType is ignored; unordered resolves to HashMap
        of(Arguments.of(map(ORDERING, UNORDERED, COMPACT_SIZE, 10, MAP_TYPE, "java.util.LinkedHashMap"), Map.class, null));
    }

    private static List<Arguments> innerMapTypeOverwriteCases() {
        return // pre‑set INNER_MAP_TYPE must be overwritten by the map type chosen for insertion ordering
        List.// pre‑set INNER_MAP_TYPE must be overwritten by the map type chosen for insertion ordering
        of(Arguments.of(map(ORDERING, INSERTION, COMPACT_SIZE, 8, CASE_SENSITIVE, false, INNER_MAP_TYPE, "java.util.TreeMap"), Map.class, null));
    }

    private static List<Arguments> reverseOrderingCases() {
        return // source map is a TreeMap with descending order – compatible with reverse ordering
        List.// source map is a TreeMap with descending order – compatible with reverse ordering
        of(// source map is normal sorted (TreeMap) but requested sorted → conflict with reverse
        Arguments.of(map(ORDERING, REVERSE, COMPACT_SIZE, 12, SOURCE_MAP, new TreeMap<>(Map.of("x", 1, "y", 2))), Map.class, null), Arguments.of(map(ORDERING, SORTED, COMPACT_SIZE, 12, SOURCE_MAP, new TreeMap<>(Map.of("x", 1, "y", 2))), IllegalArgumentException.class, "Requested ordering 'sorted' conflicts with source map's ordering"));
    }

    private static List<Arguments> invalidOptionValueTypeCases() {
        return // caseSensitive not a boolean
        List.// caseSensitive not a boolean
        of(// compactSize not an integer
        Arguments.of(map(ORDERING, UNORDERED, COMPACT_SIZE, 5, CASE_SENSITIVE, "yes"), ClassCastException.class, null), Arguments.of(map(ORDERING, UNORDERED, COMPACT_SIZE, "large"), ClassCastException.class, null));
    }

    private static List<Arguments> nullHandlingCases() {
        return // ordering key present with null value → NPE during equals
        List.// ordering key present with null value → NPE during equals
        of(// explicit null source map → treated as absent
        Arguments.of(map(ORDERING, null, COMPACT_SIZE, 7), NullPointerException.class, null), Arguments.of(map(SOURCE_MAP, null, COMPACT_SIZE, 9), Map.class, null));
    }

    private static List<Arguments> caseSensitiveUnorderedCases() {
        return // caseSensitive false + unordered → wrap with CaseInsensitiveMap
        List.// caseSensitive false + unordered → wrap with CaseInsensitiveMap
        of(Arguments.of(map(ORDERING, UNORDERED, COMPACT_SIZE, 6, CASE_SENSITIVE, false), Map.class, null));
    }

    private static List<Arguments> orderingConflictInsertionSortedCases() {
        return // source map insertion ordered (LinkedHashMap) vs requested sorted → conflict
        List.// source map insertion ordered (LinkedHashMap) vs requested sorted → conflict
        of(Arguments.of(map(ORDERING, SORTED, COMPACT_SIZE, 10, SOURCE_MAP, new LinkedHashMap<>(Map.of("a", 1, "b", 2, "c", 3))), IllegalArgumentException.class, "Requested ordering 'sorted' conflicts with source map's ordering"));
    }

    private static List<Arguments> nullOptionsMapCases() {
        return // null options map → NPE
        List.// null options map → NPE
        of(Arguments.of(null, NullPointerException.class, null));
    }

    private static List<Arguments> orderingWrongTypeCases() {
        return // ordering not a String
        List.// ordering not a String
        of(Arguments.of(map(ORDERING, 123, COMPACT_SIZE, 5), ClassCastException.class, null));
    }

    private static List<Arguments> sourceMapWrongTypeCases() {
        return // source map not a Map
        List.// source map not a Map
        of(Arguments.of(map(ORDERING, UNORDERED, COMPACT_SIZE, 5, SOURCE_MAP, "I am not a map"), ClassCastException.class, null));
    }

    private static List<Arguments> unsupportedOrderingCases() {
        return // unknown ordering string – determineMapType should reject it
        List.// unknown ordering string – determineMapType should reject it
        of(Arguments.of(map(ORDERING, "randomOrdering", COMPACT_SIZE, 5), IllegalArgumentException.class, "Unsupported ordering"));
    }

    /**
     * Returns {@code true} if the supplied method has a {@code void} return type.
     */
    public static boolean returnsVoid(Method method) {
        return method.getReturnType() == void.class;
    }

    /**
     * Returns {@code true} if the method is {@code public}.
     */
    public static boolean isPublic(Method method) {
        return Modifier.isPublic(method.getModifiers());
    }

    /**
     * Returns {@code true} if the method is {@code static}.
     */
    public static boolean isStatic(Method method) {
        return Modifier.isStatic(method.getModifiers());
    }

    /**
     * Returns {@code true} if the method is {@code abstract}.
     */
    public static boolean isAbstract(Method method) {
        return Modifier.isAbstract(method.getModifiers());
    }

    /**
     * Returns {@code true} if the method is {@code public} and {@code static}.
     */
    public static boolean isPublicStatic(Method method) {
        return isPublic(method) && isStatic(method);
    }

    /**
     * Returns {@code true} if the method is {@code public} and {@code static},
     *  or if it is a {@code default} method on an interface.
     */
    public static boolean isPublicStaticOrDefault(Method method) {
        return isPublicStatic(method) || method.isDefault();
    }

    /**
     * Returns {@code true} if the method is {@code public} or a {@code default}
     *  method on an interface.
     */
    public static boolean isPublicOrDefault(Method method) {
        return isPublic(method) || method.isDefault();
    }

    /**
     * Returns {@code true} if the method is {@code public} or {@code protected}.
     */
    public static boolean isPublicOrProtected(Method method) {
        int mods = method.getModifiers();
        return Modifier.isPublic(mods) || Modifier.isProtected(mods);
    }

    /**
     * Returns {@code true} if the method is {@code public},
     *  {@code protected} or package‑private.
     */
    public static boolean isPublicOrProtectedOrPackage(Method method) {
        int mods = method.getModifiers();
        return Modifier.isPublic(mods) || Modifier.isProtected(mods) || (!Modifier.isPublic(mods) && !Modifier.isProtected(mods) && !Modifier.isPrivate(mods));
    }

    /**
     * Returns {@code true} for any visibility – required by some engine checks.
     */
    public static boolean isPublicOrProtectedOrPackageOrPrivate(Method method) {
        // all methods satisfy this predicate
        return true;
    }

    /**
     * Finds a method with the given name and parameter types on the supplied class.
     *
     * @return an {@link Optional} containing the method if found, otherwise {@link Optional#empty()}
     */
    public static Optional<Method> findMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        try {
            return Optional.of(clazz.getDeclaredMethod(name, parameterTypes));
        } catch (NoSuchMethodException ex) {
            return Optional.empty();
        }
    }

    /**
     * Returns {@code true} if a method with the given signature exists on the class.
     */
    public static boolean isMethodPresent(Class<?> clazz, String name, Class<?>... parameterTypes) {
        return findMethod(clazz, name, parameterTypes).isPresent();
    }

    /**
     * Returns {@code true} if the supplied method is annotated with the given annotation.
     */
    public static boolean isAnnotated(Method method, Class<? extends Annotation> annotation) {
        return method.isAnnotationPresent(annotation);
    }

    /**
     * Returns {@code true} if the supplied method is either {@code public} or a
     *  {@code default} method on an interface.
     */
    public static boolean isPublicOrDefault(Method method) {
        return isPublic(method) || method.isDefault();
    }

    /**
     * Verify that a compactSize lower than 2 triggers an IllegalArgumentException.
     */
    @Test
    void validatesCompactSizeMustBeAtLeastTwo() {
        Map<String, Object> options = new HashMap<>();
        // illegal value
        options.put(COMPACT_SIZE, 1);
        // any ordering – not relevant for this test
        options.put(ORDERING, UNORDERED);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> CompactMap.validateAndFinalizeOptions(options));
        assertTrue(ex.getMessage().contains("compactSize must be >= 2"));
    }

    /**
     * When caseSensitive is false and the ordering is not sorted/reverse,
     * the method should replace the MAP_TYPE with {@link CaseInsensitiveMap}
     * and store the original map type under the INTERNAL key {@code innerMapType}.
     */
    @Test
    void caseInsensitiveOptionWrapsOriginalMapType() {
        Map<String, Object> options = new HashMap<>();
        options.put(CASE_SENSITIVE, false);
        // unordered ordering
        options.put(ORDERING, UNORDERED);
        // No explicit MAP_TYPE – the method will resolve the default (HashMap)
        CompactMap.validateAndFinalizeOptions(options);
        // The MAP_TYPE must now be the case‑insensitive wrapper
        assertEquals(CaseInsensitiveMap.class, options.get(MAP_TYPE));
        // The original map type (default HashMap) must be stored under INNER_MAP_TYPE
        assertEquals(HashMap.class, options.get(INNER_MAP_TYPE));
        // Defaults must be present
        assertEquals(CompactMap.DEFAULT_COMPACT_SIZE, options.get(COMPACT_SIZE));
        assertEquals(CompactMap.DEFAULT_CASE_SENSITIVE, options.get(CASE_SENSITIVE));
    }

    /**
     * When no configuration is supplied the method must fill in all defaults.
     */
    @Test
    void fillsInDefaultsWhenOptionsAreMissing() {
        Map<String, Object> options = new HashMap<>();
        CompactMap.validateAndFinalizeOptions(options);
        // Default ordering is UNORDERED (the method does not store it, but we can check that no
        // conflict exception was thrown)
        assertEquals(UNORDERED, options.getOrDefault(ORDERING, UNORDERED));
        // Default values must be present
        assertEquals(CompactMap.DEFAULT_COMPACT_SIZE, options.get(COMPACT_SIZE));
        assertEquals(CompactMap.DEFAULT_CASE_SENSITIVE, options.get(CASE_SENSITIVE));
        // MAP_TYPE must be the default backing map type (HashMap)
        assertEquals(CompactMap.DEFAULT_MAP_TYPE, options.get(MAP_TYPE));
        // No inner map type should be set when case sensitivity is true
        assertNull(options.get(INNER_MAP_TYPE));
    }

    @ParameterizedTest
    @MethodSource("orderingAndCompactSizeCases")
    void testOrderingAndCompactSize(Map<String, Object> options, Class<? extends Throwable> expectedException, String expectedMessageFragment) {
        try (MockedStatic<MapUtilities> mu = Mockito.mockStatic(MapUtilities.class)) {
            // Stub detection of source map ordering based on the concrete class
            mu.when(() -> MapUtilities.detectMapOrdering(any())).thenAnswer(invocation -> {
                Object src = invocation.getArgument(0);
                if (src instanceof SortedMap)
                    return SORTED;
                if (src instanceof LinkedHashMap)
                    return INSERTION;
                return UNORDERED;
            });
            mu.when(() -> MapUtilities.getMapStructureString(any())).thenReturn("dummyStructure");
            if (expectedException != null) {
                Throwable ex = assertThrows(expectedException, () -> CompactMap.validateAndFinalizeOptions(options));
                if (expectedMessageFragment != null) {
                    assertTrue(ex.getMessage().contains(expectedMessageFragment), "Exception message should contain: " + expectedMessageFragment);
                }
            } else {
                assertDoesNotThrow(() -> CompactMap.validateAndFinalizeOptions(options));
                // verify expectations for a few representative cases
                if (options != null && Boolean.FALSE.equals(options.get(CASE_SENSITIVE))) {
                    // non‑sorted ordering → should wrap
                    assertEquals(CaseInsensitiveMap.class, options.get(MAP_TYPE));
                    assertNotNull(options.get(INNER_MAP_TYPE));
                }
                if (options != null && SORTED.equals(options.get(ORDERING))) {
                    // sorted ordering → map type must be a sorted map (TreeMap)
                    assertEquals(TreeMap.class, options.get(MAP_TYPE));
                }
            }
        }
    }

    @ParameterizedTest
    @MethodSource("explicitMapTypeOverrideCases")
    void testExplicitMapTypeOverride(Map<String, Object> options, Class<? extends Throwable> expectedException, String ignored) {
        try (MockedStatic<MapUtilities> mu = Mockito.mockStatic(MapUtilities.class)) {
            mu.when(() -> MapUtilities.detectMapOrdering(any())).thenReturn(UNORDERED);
            mu.when(() -> MapUtilities.getMapStructureString(any())).thenReturn("dummy");
            assertDoesNotThrow(() -> CompactMap.validateAndFinalizeOptions(options));
            // unordered ordering must resolve to HashMap irrespective of supplied MAP_TYPE
            assertEquals(HashMap.class, options.get(MAP_TYPE));
        }
    }

    @ParameterizedTest
    @MethodSource("innerMapTypeOverwriteCases")
    void testInnerMapTypeOverwrite(Map<String, Object> options, Class<? extends Throwable> expectedException, String ignored) {
        try (MockedStatic<MapUtilities> mu = Mockito.mockStatic(MapUtilities.class)) {
            mu.when(() -> MapUtilities.detectMapOrdering(any())).thenReturn(UNORDERED);
            mu.when(() -> MapUtilities.getMapStructureString(any())).thenReturn("dummy");
            assertDoesNotThrow(() -> CompactMap.validateAndFinalizeOptions(options));
            // ordering = insertion → underlying map should be LinkedHashMap
            assertEquals(CaseInsensitiveMap.class, options.get(MAP_TYPE));
            assertEquals(LinkedHashMap.class, options.get(INNER_MAP_TYPE));
        }
    }

    @ParameterizedTest
    @MethodSource("reverseOrderingCases")
    void testReverseOrderingCompatibility(Map<String, Object> options, Class<? extends Throwable> expectedException, String expectedMessageFragment) {
        try (MockedStatic<MapUtilities> mu = Mockito.mockStatic(MapUtilities.class)) {
            mu.when(() -> MapUtilities.detectMapOrdering(any())).thenAnswer(invocation -> {
                Object src = invocation.getArgument(0);
                // simulate reverse ordering detection
                if (src instanceof SortedMap)
                    return REVERSE;
                return UNORDERED;
            });
            mu.when(() -> MapUtilities.getMapStructureString(any())).thenReturn("dummy");
            if (expectedException != null) {
                Throwable ex = assertThrows(expectedException, () -> CompactMap.validateAndFinalizeOptions(options));
                if (expectedMessageFragment != null) {
                    assertTrue(ex.getMessage().contains(expectedMessageFragment));
                }
            } else {
                assertDoesNotThrow(() -> CompactMap.validateAndFinalizeOptions(options));
                // reverse ordering should still resolve to a sorted map implementation
                assertEquals(TreeMap.class, options.get(MAP_TYPE));
            }
        }
    }

    @ParameterizedTest
    @MethodSource("invalidOptionValueTypeCases")
    void testInvalidOptionValueTypes(Map<String, Object> options, Class<? extends Throwable> expectedException, String ignored) {
        assertThrows(expectedException, () -> CompactMap.validateAndFinalizeOptions(options));
    }

    @ParameterizedTest
    @MethodSource("nullHandlingCases")
    void testNullHandling(Map<String, Object> options, Class<? extends Throwable> expectedException, String ignored) {
        try (MockedStatic<MapUtilities> mu = Mockito.mockStatic(MapUtilities.class)) {
            mu.when(() -> MapUtilities.detectMapOrdering(any())).thenReturn(UNORDERED);
            mu.when(() -> MapUtilities.getMapStructureString(any())).thenReturn("dummy");
            if (expectedException != null) {
                assertThrows(expectedException, () -> CompactMap.validateAndFinalizeOptions(options));
            } else {
                assertDoesNotThrow(() -> CompactMap.validateAndFinalizeOptions(options));
                assertEquals(HashMap.class, options.get(MAP_TYPE));
            }
        }
    }

    @ParameterizedTest
    @MethodSource("caseSensitiveUnorderedCases")
    void testCaseSensitiveFalseUnordered(Map<String, Object> options, Class<? extends Throwable> expectedException, String ignored) {
        try (MockedStatic<MapUtilities> mu = Mockito.mockStatic(MapUtilities.class)) {
            mu.when(() -> MapUtilities.detectMapOrdering(any())).thenReturn(UNORDERED);
            mu.when(() -> MapUtilities.getMapStructureString(any())).thenReturn("dummy");
            assertDoesNotThrow(() -> CompactMap.validateAndFinalizeOptions(options));
            assertEquals(CaseInsensitiveMap.class, options.get(MAP_TYPE));
            assertEquals(HashMap.class, options.get(INNER_MAP_TYPE));
        }
    }

    @ParameterizedTest
    @MethodSource("orderingConflictInsertionSortedCases")
    void testOrderingConflictInsertionSorted(Map<String, Object> options, Class<? extends Throwable> expectedException, String expectedMessageFragment) {
        try (MockedStatic<MapUtilities> mu = Mockito.mockStatic(MapUtilities.class)) {
            mu.when(() -> MapUtilities.detectMapOrdering(any())).thenReturn(INSERTION);
            mu.when(() -> MapUtilities.getMapStructureString(any())).thenReturn("dummy");
            Throwable ex = assertThrows(expectedException, () -> CompactMap.validateAndFinalizeOptions(options));
            assertTrue(ex.getMessage().contains(expectedMessageFragment));
        }
    }

    @ParameterizedTest
    @MethodSource("nullOptionsMapCases")
    void testNullOptionsMap(Map<String, Object> options, Class<? extends Throwable> expectedException, String ignored) {
        assertThrows(expectedException, () -> CompactMap.validateAndFinalizeOptions(options));
    }

    @ParameterizedTest
    @MethodSource("orderingWrongTypeCases")
    void testOrderingWrongType(Map<String, Object> options, Class<? extends Throwable> expectedException, String ignored) {
        assertThrows(expectedException, () -> CompactMap.validateAndFinalizeOptions(options));
    }

    @ParameterizedTest
    @MethodSource("sourceMapWrongTypeCases")
    void testSourceMapWrongType(Map<String, Object> options, Class<? extends Throwable> expectedException, String ignored) {
        assertThrows(expectedException, () -> CompactMap.validateAndFinalizeOptions(options));
    }

    @ParameterizedTest
    @MethodSource("unsupportedOrderingCases")
    void testUnsupportedOrdering(Map<String, Object> options, Class<? extends Throwable> expectedException, String expectedMessageFragment) {
        // We cannot mock determineMapType directly (private), but the method is expected
        // to throw IllegalArgumentException for unknown ordering strings.
        assertThrows(expectedException, () -> CompactMap.validateAndFinalizeOptions(options));
    }
}
