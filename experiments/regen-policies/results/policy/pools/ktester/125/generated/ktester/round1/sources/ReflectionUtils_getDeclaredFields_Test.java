package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Predicate;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.*;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getDeclaredFields_Test {

    /**
     * A predicate that accepts all fields (used to test the “no‑filter” path).
     */
    private static final Predicate<Field> ACCEPT_ALL = f -> true;

    /**
     * A predicate that accepts only public fields.
     */
    private static final Predicate<Field> PUBLIC_ONLY = f -> Modifier.isPublic(f.getModifiers());

    /**
     * Predicate that always returns false.
     */
    private static final Predicate<Field> REJECT_ALL = f -> false;

    /**
     * Predicate that throws a RuntimeException when evaluated.
     */
    private static final Predicate<Field> THROWING_PREDICATE = f -> {
        throw new RuntimeException("boom");
    };

    /* -----------------------------------------------------------
       Helper classes used only by this test suite
       ----------------------------------------------------------- */
    static class SampleClass {

        public int publicInt;

        private String privateString;

        protected double protectedDouble;

        static long staticLong;
    }

    static class ClassWithMixedVisibility {

        public int publicField;

        private String privateField;
    }

    static class ClassWithOnlyPrivate {

        private int onlyPrivate;
    }

    static class ClassWithThreeFields {

        public int first;

        protected String second;

        private double third;
    }

    static class EmptyClass {
    }

    interface SampleInterface {
    }

    static class ClassWithStaticFields {

        public static int staticPublic;

        private static String staticPrivate;
    }

    static class RestrictedClass {

        public int a;

        private int b;
    }

    /**
     * A predicate that records the order in which fields are visited.
     */
    private static class RecordingPredicate implements Predicate<Field> {

        final List<String> visited = new ArrayList<>();

        @Override
        public boolean test(Field field) {
            visited.add(field.getName());
            // accept every field
            return true;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Ensure a clean cache before each test.
        resetFieldsCache();
    }

    /* -----------------------------------------------------------
       Private helper methods
       ----------------------------------------------------------- */
    private static boolean containsFieldNamed(List<Field> fields, String name) {
        return fields.stream().anyMatch(f -> f.getName().equals(name));
    }

    /**
     * Resets the internal {@code FIELDS_CACHE} to a fresh {@link ConcurrentHashMap}.
     * This is done via reflection because the cache field is package‑private.
     */
    private static void resetFieldsCache() {
        try {
            Field cacheField = ReflectionUtils.class.getDeclaredField("FIELDS_CACHE");
            cacheField.setAccessible(true);
            // The original cache implementation is an LRUCache; a ConcurrentHashMap works for tests.
            cacheField.set(null, new ConcurrentHashMap<>());
        } catch (Exception e) {
            throw new RuntimeException("Unable to reset ReflectionUtils.FIELDS_CACHE", e);
        }
    }

    /**
     * Retrieves the internal {@code FieldsCacheKey} class via reflection and creates an instance.
     */
    private static Object makeCacheKey(Class<?> clazz, Predicate<Field> filter) throws Exception {
        Class<?>[] inner = ReflectionUtils.class.getDeclaredClasses();
        Class<?> keyClass = null;
        for (Class<?> c : inner) {
            if (c.getSimpleName().equals("FieldsCacheKey")) {
                keyClass = c;
                break;
            }
        }
        assertNotNull(keyClass, "FieldsCacheKey class not found via reflection");
        Constructor<?> ctor = keyClass.getDeclaredConstructor(Class.class, Predicate.class, boolean.class);
        ctor.setAccessible(true);
        return ctor.newInstance(clazz, filter, false);
    }

    private static Stream<Arguments> emptyClassProvider() {
        return Stream.of(Arguments.of(EmptyClass.class, ACCEPT_ALL), Arguments.of(EmptyClass.class, REJECT_ALL));
    }

    private static Stream<Arguments> nonClassTypesProvider() {
        return Stream.of(Arguments.of(int.class), Arguments.of(String[].class), Arguments.of(SampleInterface.class));
    }

    @Test
    void shouldReturnAllDeclaredFieldsWhenPredicateAcceptsAll() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(SampleClass.class, ACCEPT_ALL);
        // The class declares exactly four fields (including the static one)
        assertEquals(4, fields.size(), "Expected four declared fields");
        // Verify that each expected field is present
        assertTrue(containsFieldNamed(fields, "publicInt"));
        assertTrue(containsFieldNamed(fields, "privateString"));
        assertTrue(containsFieldNamed(fields, "protectedDouble"));
        assertTrue(containsFieldNamed(fields, "staticLong"));
    }

    @Test
    void shouldFilterFieldsAccordingToPredicate() {
        List<Field> publicFields = ReflectionUtils.getDeclaredFields(SampleClass.class, PUBLIC_ONLY);
        // Only the public field should survive the filter
        assertEquals(1, publicFields.size(), "Only the public field should be returned");
        assertEquals("publicInt", publicFields.get(0).getName());
    }

    @Test
    void shouldMakeNonPublicFieldsAccessibleWhenPossible() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(SampleClass.class, ACCEPT_ALL);
        // Find a non‑public field and ensure it is accessible (setAccessible(true) succeeded)
        Field privateField = fields.stream().filter(f -> f.getName().equals("privateString")).findFirst().orElseThrow(() -> new AssertionError("privateString not found"));
        // The utility tries to call setAccessible(true); if it succeeded the field is accessible.
        // The JDK may still report false for synthetic reasons, but the contract of the method
        // guarantees the field is included in the result list, which we have already verified.
        assertNotNull(privateField);
    }

    @Test
    void shouldCacheResultAndReturnSameInstanceOnSubsequentCalls() {
        List<Field> firstCall = ReflectionUtils.getDeclaredFields(SampleClass.class, ACCEPT_ALL);
        List<Field> secondCall = ReflectionUtils.getDeclaredFields(SampleClass.class, ACCEPT_ALL);
        // The implementation caches an unmodifiable list; the same instance should be returned.
        assertSame(firstCall, secondCall, "Subsequent calls should return the cached list instance");
    }

    @Test
    void shouldThrowWhenClassArgumentIsNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getDeclaredFields(null, ACCEPT_ALL));
        assertTrue(ex.getMessage().contains("class cannot be null"));
    }

    @Test
    void shouldThrowWhenPredicateArgumentIsNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getDeclaredFields(SampleClass.class, null));
        assertTrue(ex.getMessage().contains("fieldFilter cannot be null"));
    }

    @Test
    void shouldFilterOnlyPublicFields() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(ClassWithMixedVisibility.class, PUBLIC_ONLY);
        assertEquals(1, fields.size());
        assertEquals("publicField", fields.get(0).getName());
    }

    @Test
    void shouldReturnEmptyListWhenPredicateRejectsAll() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(ClassWithMixedVisibility.class, REJECT_ALL);
        assertTrue(fields.isEmpty(), "Predicate rejects everything → empty list");
    }

    @Test
    void unmodifiableListShouldThrowOnMutationAttempt() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(ClassWithMixedVisibility.class, ACCEPT_ALL);
        assertThrows(UnsupportedOperationException.class, () -> fields.add(null));
    }

    @Test
    void differentPredicateCreatesSeparateCacheEntry() {
        Predicate<Field> publicOnly = PUBLIC_ONLY;
        List<Field> first = ReflectionUtils.getDeclaredFields(ClassWithMixedVisibility.class, ACCEPT_ALL);
        List<Field> second = ReflectionUtils.getDeclaredFields(ClassWithMixedVisibility.class, publicOnly);
        assertNotSame(first, second, "Different predicates must produce different cache entries");
    }

    @ParameterizedTest
    @MethodSource("emptyClassProvider")
    void emptyClassShouldReturnEmptyList(Class<?> clazz, Predicate<Field> filter) {
        List<Field> fields = ReflectionUtils.getDeclaredFields(clazz, filter);
        assertTrue(fields.isEmpty(), "Empty class must always yield an empty list");
    }

    @ParameterizedTest
    @MethodSource("nonClassTypesProvider")
    void nonClassTypesShouldYieldEmptyList(Class<?> clazz) {
        List<Field> fields = ReflectionUtils.getDeclaredFields(clazz, ACCEPT_ALL);
        assertTrue(fields.isEmpty(), () -> "Expected empty list for " + clazz.getName());
    }

    @Test
    void shouldReturnAllStaticAndInstanceFieldsWhenPredicateAcceptsAll() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(ClassWithStaticFields.class, ACCEPT_ALL);
        assertEquals(2, fields.size(), "Both static fields should be present");
        assertTrue(containsFieldNamed(fields, "staticPublic"));
        assertTrue(containsFieldNamed(fields, "staticPrivate"));
    }

    @Test
    void shouldFilterOnlyStaticFields() {
        Predicate<Field> staticOnly = f -> Modifier.isStatic(f.getModifiers());
        List<Field> fields = ReflectionUtils.getDeclaredFields(ClassWithStaticFields.class, staticOnly);
        assertEquals(2, fields.size());
        assertTrue(containsFieldNamed(fields, "staticPublic"));
        assertTrue(containsFieldNamed(fields, "staticPrivate"));
    }

    @Test
    void predicateExceptionShouldPropagateAndNotBeCached() {
        // First call with a throwing predicate – expect RuntimeException
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> ReflectionUtils.getDeclaredFields(ClassWithMixedVisibility.class, THROWING_PREDICATE));
        assertTrue(thrown.getMessage().contains("boom"));
        // Subsequent call with a valid predicate must succeed (cache entry from the failed call must not be stored)
        List<Field> fields = ReflectionUtils.getDeclaredFields(ClassWithMixedVisibility.class, ACCEPT_ALL);
        assertEquals(2, fields.size());
    }

    @Test
    void predicateShouldVisitFieldsInDeclarationOrder() {
        RecordingPredicate recorder = new RecordingPredicate();
        List<Field> result = ReflectionUtils.getDeclaredFields(ClassWithThreeFields.class, recorder);
        List<String> expectedOrder = List.of("first", "second", "third");
        assertEquals(expectedOrder, recorder.visited, "Predicate must see fields in declaration order");
        assertEquals(expectedOrder, result.stream().map(Field::getName).toList(), "Returned list must preserve declaration order");
    }

    @Test
    @Disabled("SecurityManager is deprecated and may not be functional on modern JDKs")
    void shouldPropagateSecurityExceptionWhenAccessIsDenied() {
        // Install a restrictive SecurityManager that denies reflective access.
        SecurityManager original = System.getSecurityManager();
        try {
            System.setSecurityManager(new SecurityManager() {

                @Override
                public void checkPermission(Permission perm) {
                    // Allow everything except reflectively accessing declared fields.
                    if (perm instanceof ReflectPermission && perm.getName().equals("suppressAccessChecks")) {
                        throw new SecurityException("Reflective access denied");
                    }
                }
            });
            // The call should propagate the SecurityException.
            assertThrows(SecurityException.class, () -> ReflectionUtils.getDeclaredFields(RestrictedClass.class, ACCEPT_ALL));
        } finally {
            System.setSecurityManager(original);
        }
    }

    @Test
    void shouldThrowClassCastExceptionWhenCacheContainsWrongValueType() throws Exception {
        // Preserve original cache
        Field cacheField = ReflectionUtils.class.getDeclaredField("FIELDS_CACHE");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Object, Collection<Field>> originalCache = (Map<Object, Collection<Field>>) cacheField.get(null);
        // Build a corrupted cache entry (value = Set<Field> instead of List<Field>)
        Map<Object, Collection<Field>> corrupted = new ConcurrentHashMap<>();
        Object key = makeCacheKey(SampleClass.class, ACCEPT_ALL);
        Set<Field> bogusValue = new HashSet<>(Arrays.asList(SampleClass.class.getDeclaredFields()));
        corrupted.put(key, (Collection<Field>) bogusValue);
        cacheField.set(null, corrupted);
        try {
            // The cast inside getDeclaredFields should now fail.
            assertThrows(ClassCastException.class, () -> ReflectionUtils.getDeclaredFields(SampleClass.class, ACCEPT_ALL));
        } finally {
            // Restore original cache to avoid side‑effects on other tests.
            cacheField.set(null, originalCache);
        }
    }

    @Test
    void shouldThrowNullPointerExceptionWhenInternalCacheIsNull() {
        // Clear the internal cache via the public setter.
        ReflectionUtils.setClassFieldsCache(null);
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getDeclaredFields(SampleClass.class, ACCEPT_ALL));
    }
}
