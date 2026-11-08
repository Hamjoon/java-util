package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Predicate;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getDeclaredFields_Test {

    /**
     * Simple class used for reflection tests.
     */
    static class Sample {

        public int publicInt;

        private String privateString;

        protected double protectedDouble;

        static long staticLong;

        final boolean finalBool = true;
    }

    /**
     * Simple class used for most reflection tests.
     */
    static class SampleClass {

        public int publicField;

        private String privateField;

        protected double protectedField;

        // default (package‑private) visibility
        long packageField;
    }

    /**
     * Class with fields declared in a specific order.
     */
    static class OrderedFieldsClass {

        int first;

        int second;

        int third;
    }

    /**
     * Super‑class with a field that should NOT be returned for a subclass query.
     */
    static class SuperClass {

        public int superPublic;
    }

    /**
     * Sub‑class that declares its own field.
     */
    static class SubClass extends SuperClass {

        public int subPublicField;
    }

    /**
     * Class without any declared fields.
     */
    static class EmptyClass {
    }

    /**
     * Class with a mix of annotated and non‑annotated fields.
     */
    static class AnnotatedFieldsClass {

        @SampleAnnotation
        private int annotatedField;

        private int nonAnnotatedField;
    }

    /**
     * Predicate that counts how many times {@code test} is invoked.
     */
    static class CountingPredicate implements Predicate<Field> {

        private int count = 0;

        @Override
        public boolean test(Field field) {
            count++;
            return true;
        }

        int getCount() {
            return count;
        }
    }

    /**
     * Predicate that always throws a RuntimeException.
     */
    static class ThrowingPredicate implements Predicate<Field> {

        @Override
        public boolean test(Field field) {
            throw new RuntimeException("predicate failure");
        }
    }

    /**
     * Simple predicate that always returns {@code true}.
     */
    static class AcceptAllPredicate implements Predicate<Field> {

        @Override
        public boolean test(Field field) {
            return true;
        }
    }

    @AfterEach
    void cleanUpCache() throws Exception {
        resetCache();
    }

    /**
     * Restores the internal cache to a fresh, empty map.
     */
    private static void resetCache() throws Exception {
        Field cacheField = ReflectionUtils.class.getDeclaredField("FIELDS_CACHE");
        cacheField.setAccessible(true);
        // Use a plain HashMap – the concrete type does not matter for the tests.
        cacheField.set(null, new HashMap<>());
    }

    /**
     * Provides parameters for the null‑check tests.
     */
    private static Stream<Arguments> nullCheckProvider() {
        return Stream.of(Arguments.of(null, (Predicate<Field>) f -> true, "class cannot be null"), Arguments.of(SampleClass.class, null, "fieldFilter cannot be null"));
    }

    @Test
    void getDeclaredFields_nullClass_throwsException() {
        Predicate<Field> any = f -> true;
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getDeclaredFields(null, any));
    }

    @Test
    void getDeclaredFields_nullPredicate_throwsException() {
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getDeclaredFields(SampleClass.class, null));
    }

    @Test
    void getDeclaredFields_allFieldsReturned_andPrivateMadeAccessible() throws Exception {
        // Predicate that accepts every field
        Predicate<Field> acceptAll = f -> true;
        List<Field> fields = ReflectionUtils.getDeclaredFields(Sample.class, acceptAll);
        // Verify that all declared fields are present
        assertEquals(5, fields.size(), "Sample declares 5 fields");
        // Ensure the private field is present and can be accessed
        Field privateField = fields.stream().filter(f -> f.getName().equals("privateString")).findFirst().orElseThrow(() -> new AssertionError("privateString field not found"));
        // The field should be accessible after the utility method runs
        assertTrue(privateField.canAccess(new Sample()), "privateString field should be accessible after getDeclaredFields");
        // Try to read the value to prove accessibility works
        Sample instance = new Sample();
        privateField.set(instance, "secret");
        assertEquals("secret", privateField.get(instance));
    }

    @Test
    void getDeclaredFields_filterWorks() {
        // Predicate that only accepts public fields
        Predicate<Field> publicOnly = f -> Modifier.isPublic(f.getModifiers());
        List<Field> fields = ReflectionUtils.getDeclaredFields(Sample.class, publicOnly);
        // Only the public field should be returned
        assertEquals(1, fields.size(), "Only one public field should be returned");
        assertEquals("publicInt", fields.get(0).getName());
    }

    @Test
    void getDeclaredFields_isCached_sameInstanceReturned() {
        Predicate<Field> acceptAll = f -> true;
        List<Field> firstCall = ReflectionUtils.getDeclaredFields(Sample.class, acceptAll);
        List<Field> secondCall = ReflectionUtils.getDeclaredFields(Sample.class, acceptAll);
        // The cache should return the exact same List instance for identical parameters
        assertSame(firstCall, secondCall, "Result should be cached and identical on subsequent calls");
    }

    /**
     * Null‑check tests expecting IllegalArgumentException with a specific message.
     */
    @ParameterizedTest
    @MethodSource("nullCheckProvider")
    void nullChecks_throwIllegalArgumentException(Class<?> clazz, Predicate<Field> predicate, String expectedMessage) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getDeclaredFields(clazz, predicate));
        assertEquals(expectedMessage, ex.getMessage());
    }

    /**
     * Predicate that rejects everything – the result must be an empty list.
     */
    @Test
    void filtering_behavior_rejectAll_returnsEmptyList() {
        Predicate<Field> rejectAll = f -> false;
        List<Field> fields = ReflectionUtils.getDeclaredFields(SampleClass.class, rejectAll);
        assertTrue(fields.isEmpty(), "All fields rejected – list should be empty");
    }

    /**
     * Predicate that keeps only public fields – verifies correct filtering.
     */
    @Test
    void filtering_behavior_publicOnly() {
        Predicate<Field> publicOnly = f -> Modifier.isPublic(f.getModifiers());
        List<Field> fields = ReflectionUtils.getDeclaredFields(SampleClass.class, publicOnly);
        assertEquals(1, fields.size());
        assertEquals("publicField", fields.get(0).getName());
    }

    /**
     * Predicate that accepts all fields – ensures every declared field is returned.
     */
    @Test
    void filtering_behavior_acceptAll() {
        Predicate<Field> acceptAll = f -> true;
        List<Field> fields = ReflectionUtils.getDeclaredFields(SampleClass.class, acceptAll);
        List<String> names = fields.stream().map(Field::getName).sorted().toList();
        assertEquals(List.of("privateField", "protectedField", "publicField", "packageField"), names);
    }

    /**
     * Accessibility handling – non‑public fields should be made accessible (or at least remain present).
     */
    @Test
    void accessibility_handling_nonPublicFieldsMadeAccessible() {
        Predicate<Field> acceptAll = f -> true;
        List<Field> fields = ReflectionUtils.getDeclaredFields(SampleClass.class, acceptAll);
        for (Field f : fields) {
            if (!Modifier.isPublic(f.getModifiers())) {
                // The field must be present; if setAccessible succeeded, canAccess should be true.
                assertTrue(f.canAccess(null) || f.canAccess(new SampleClass()), "Non‑public field '" + f.getName() + "' should be accessible or at least present");
            }
        }
    }

    /**
     * Caching behaviour – the predicate must not be invoked on the second call.
     */
    @Test
    void caching_behavior_predicateInvokedOnlyOnce() {
        CountingPredicate counting = new CountingPredicate();
        List<Field> first = ReflectionUtils.getDeclaredFields(SampleClass.class, counting);
        int afterFirst = counting.getCount();
        List<Field> second = ReflectionUtils.getDeclaredFields(SampleClass.class, counting);
        int afterSecond = counting.getCount();
        assertSame(first, second, "Cache should return the same list instance");
        assertEquals(afterFirst, afterSecond, "Predicate should not be invoked again when result is cached");
    }

    /**
     * The returned list must be unmodifiable.
     */
    @Test
    void unmodifiable_list_modificationThrows() {
        Predicate<Field> acceptAll = f -> true;
        List<Field> fields = ReflectionUtils.getDeclaredFields(SampleClass.class, acceptAll);
        assertThrows(UnsupportedOperationException.class, () -> fields.add(null));
        assertThrows(UnsupportedOperationException.class, () -> fields.remove(0));
    }

    /**
     * Exception propagation – a predicate that throws must let the exception bubble up.
     */
    @Test
    void exception_propagation_predicateThrows() {
        ThrowingPredicate throwing = new ThrowingPredicate();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ReflectionUtils.getDeclaredFields(SampleClass.class, throwing));
        assertEquals("predicate failure", ex.getMessage());
    }

    /**
     * Declaration order preservation – the list order must match the order returned by Class.getDeclaredFields().
     */
    @Test
    void declaration_order_preservation() {
        Predicate<Field> acceptAll = f -> true;
        List<Field> fields = ReflectionUtils.getDeclaredFields(OrderedFieldsClass.class, acceptAll);
        String[] expected = { "first", "second", "third" };
        assertEquals(expected.length, fields.size());
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], fields.get(i).getName(), "Field order should be preserved");
        }
    }

    /**
     * Cache key differentiates distinct predicate instances – different instances produce different cache entries.
     */
    @Test
    void cacheKey_differentPredicateInstancesProduceSeparateEntries() {
        Predicate<Field> firstPredicate = new AcceptAllPredicate();
        // distinct object
        Predicate<Field> secondPredicate = new AcceptAllPredicate();
        List<Field> firstResult = ReflectionUtils.getDeclaredFields(SampleClass.class, firstPredicate);
        List<Field> secondResult = ReflectionUtils.getDeclaredFields(SampleClass.class, secondPredicate);
        assertNotSame(firstResult, secondResult, "Different predicate instances must result in different cached lists");
    }

    /**
     * Subclass declared fields only – inherited fields must not appear.
     */
    @Test
    void subclass_declaredFieldsOnly() {
        Predicate<Field> acceptAll = f -> true;
        List<Field> fields = ReflectionUtils.getDeclaredFields(SubClass.class, acceptAll);
        assertEquals(1, fields.size());
        assertEquals("subPublicField", fields.get(0).getName());
    }

    /**
     * Class with no declared fields – should return an empty, unmodifiable list.
     */
    @Test
    void emptyClass_returnsEmptyList() {
        Predicate<Field> acceptAll = f -> true;
        List<Field> fields = ReflectionUtils.getDeclaredFields(EmptyClass.class, acceptAll);
        assertTrue(fields.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> fields.add(null));
    }

    /**
     * Annotation‑based filtering – only fields annotated with @SampleAnnotation are returned.
     */
    @Test
    void annotationBased_filtering() {
        Predicate<Field> annotatedOnly = f -> f.isAnnotationPresent(SampleAnnotation.class);
        List<Field> fields = ReflectionUtils.getDeclaredFields(AnnotatedFieldsClass.class, annotatedOnly);
        assertEquals(1, fields.size());
        assertEquals("annotatedField", fields.get(0).getName());
    }

    /**
     * Cache tampering – setting the internal cache to {@code null} must cause a NullPointerException.
     */
    @Test
    void cacheTampering_nullCache_causesNPE() throws Exception {
        // Save original cache and replace it with null
        Field cacheField = ReflectionUtils.class.getDeclaredField("FIELDS_CACHE");
        cacheField.setAccessible(true);
        Object originalCache = cacheField.get(null);
        cacheField.set(null, null);
        try {
            Predicate<Field> acceptAll = f -> true;
            assertThrows(NullPointerException.class, () -> ReflectionUtils.getDeclaredFields(SampleClass.class, acceptAll));
        } finally {
            // Restore the original cache to avoid affecting other tests
            cacheField.set(null, originalCache);
        }
    }

    /**
     * Cache tampering – a cache that returns a {@code Set} instead of a {@code List} triggers a ClassCastException.
     */
    @Test
    void cacheTampering_wrongCollectionType_causesClassCastException() throws Exception {
        // Create a map that always returns a Set from computeIfAbsent
        Map<Object, Collection<Field>> badCache = new HashMap<>() {

            @Override
            public Collection<Field> computeIfAbsent(Object key, java.util.function.Function<? super Object, ? extends Collection<Field>> mappingFunction) {
                // not a List
                return new HashSet<>();
            }
        };
        // Inject the bad cache
        Field cacheField = ReflectionUtils.class.getDeclaredField("FIELDS_CACHE");
        cacheField.setAccessible(true);
        Object originalCache = cacheField.get(null);
        cacheField.set(null, badCache);
        try {
            Predicate<Field> acceptAll = f -> true;
            assertThrows(ClassCastException.class, () -> ReflectionUtils.getDeclaredFields(SampleClass.class, acceptAll));
        } finally {
            // Restore original cache
            cacheField.set(null, originalCache);
        }
    }
}
