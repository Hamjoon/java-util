package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Predicate;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getDeclaredFields_2_Test {

    /**
     * Simple class with a mixture of static and instance fields.
     */
    static class Sample {

        public int publicField;

        private String privateField;

        // should be filtered out
        static int staticField;
    }

    /**
     * Sub‑class that declares its own field – inherited fields must not appear.
     */
    static class SubSample extends Sample {

        int subField;
    }

    /**
     * Class with one static and one instance field.
     */
    static class WithStatic {

        // should be filtered out
        static int count;

        // should be returned
        String value;
    }

    /**
     * Empty class – declares no fields at all.
     */
    static class EmptyClass {
    }

    /**
     * Class that only declares static fields.
     */
    static class OnlyStaticFields {

        static int a;

        static String b;
    }

    /**
     * Class with fields of every visibility level.
     */
    static class MixedVisibility {

        public int publicField;

        protected int protectedField;

        // package‑private
        int packageField;

        private int privateField;
    }

    // Example of how one could drive the above tests with a MethodSource if desired.
    static Stream<Arguments> provideClassesForDefaultFilter() {
        return Stream.of(Arguments.of(WithStatic.class, List.of("value")), Arguments.of(EmptyClass.class, List.of()), Arguments.of(OnlyStaticFields.class, List.of()), Arguments.of(MixedVisibility.class, List.of("publicField", "protectedField", "packageField", "privateField")), Arguments.of(SampleEnum.class, List.of("custom")), Arguments.of(EnumWithInternal.class, List.of("data")));
    }

    @Test
    void testGetDeclaredFieldsFiltersStaticFields() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(Sample.class);
        // The static field must be filtered out by the default predicate
        assertEquals(2, fields.size(), "Only the two instance fields should be returned");
        // Verify that the returned fields are the expected ones
        boolean hasPublic = fields.stream().anyMatch(f -> f.getName().equals("publicField"));
        boolean hasPrivate = fields.stream().anyMatch(f -> f.getName().equals("privateField"));
        assertTrue(hasPublic, "publicField should be present");
        assertTrue(hasPrivate, "privateField should be present");
    }

    @Test
    void testGetDeclaredFieldsDoesNotIncludeInheritedFields() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(SubSample.class);
        // Only the field declared directly in SubSample should be present
        assertEquals(1, fields.size(), "Only the field declared in SubSample should be returned");
        assertEquals("subField", fields.get(0).getName(), "Returned field should be subField");
    }

    @Test
    void testGetDeclaredFieldsOnEnumFiltersSyntheticFields() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(MyEnum.class);
        // Enum does not declare any user fields; synthetic fields are filtered out
        assertTrue(fields.isEmpty(), "Enum should have no fields after default filtering");
    }

    @Test
    void testGetDeclaredFieldsHandlesNullClass() {
        // Defensive programming: passing null should result in a NullPointerException
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getDeclaredFields(null));
    }

    @Test
    void testStaticFieldExclusion() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(WithStatic.class);
        assertEquals(1, fields.size(), "Only the instance field should be returned");
        assertEquals("value", fields.get(0).getName(), "Returned field should be 'value'");
    }

    @Test
    void testClassWithNoDeclaredFields() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(EmptyClass.class);
        assertTrue(fields.isEmpty(), "Empty class should yield an empty field list");
    }

    @Test
    void testClassWithOnlyStaticFields() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(OnlyStaticFields.class);
        assertTrue(fields.isEmpty(), "All‑static class should yield an empty field list");
    }

    @Test
    void testMixedVisibilityFields() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(MixedVisibility.class);
        assertEquals(4, fields.size(), "All four instance fields should be returned");
        // Verify each expected field name is present (order is not guaranteed by the JVM)
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("publicField")));
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("protectedField")));
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("packageField")));
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("privateField")));
    }

    @Test
    void testEnumWithCustomInstanceField() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(SampleEnum.class);
        assertEquals(1, fields.size(), "Enum with a user field should return only that field");
        assertEquals("custom", fields.get(0).getName(), "Returned field should be 'custom'");
    }

    @Test
    void testEnumInternalFieldExclusion() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(EnumWithInternal.class);
        // The default filter removes the synthetic "internal" field, leaving only "data"
        assertEquals(1, fields.size(), "Only the user‑defined 'data' field should survive filtering");
        assertEquals("data", fields.get(0).getName(), "Returned field should be 'data'");
    }

    @Test
    void testNullFieldFilterArgument() {
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getDeclaredFields(WithStatic.class, null), "A null Predicate should cause NullPointerException");
    }

    @Test
    void testFieldFilterThrowsException() {
        // Predicate that deliberately throws an exception for any field
        Predicate<Field> throwingPredicate = f -> {
            throw new IllegalStateException("boom");
        };
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> ReflectionUtils.getDeclaredFields(WithStatic.class, throwingPredicate), "Exception from predicate should propagate");
        assertEquals("boom", ex.getMessage());
    }

    /**
     * Demonstrates the behaviour when a {@link SecurityManager} blocks reflective access.
     * Modern JVMs may not enforce a SecurityManager, therefore the test is disabled
     * by default. It can be enabled on an older JDK or when a custom SecurityManager
     * is installed that throws {@link SecurityException} on {@code getDeclaredFields()}.
     */
    @Disabled("SecurityManager is deprecated; enable only on platforms that support it")
    @Test
    void testSecurityManagerRestriction() throws Exception {
        // Install a SecurityManager that denies reflective access to declared fields
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkPermission(java.security.Permission perm) {
                // Allow everything else
            }

            @Override
            public void checkMemberAccess(Class<?> clazz, int which) {
                throw new SecurityException("Access denied");
            }
        });
        try {
            // Use reflection to obtain the private DEFAULT_FIELD_FILTER instance
            java.lang.reflect.Field filterField = ReflectionUtils.class.getDeclaredField("DEFAULT_FIELD_FILTER");
            filterField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Predicate<Field> defaultFilter = (Predicate<Field>) filterField.get(null);
            // The call should propagate the SecurityException thrown by the SecurityManager
            assertThrows(SecurityException.class, () -> ReflectionUtils.getDeclaredFields(WithStatic.class, defaultFilter));
        } finally {
            // Reset the security manager to avoid side effects on other tests
            System.setSecurityManager(null);
        }
    }

    @ParameterizedTest
    @MethodSource("provideClassesForDefaultFilter")
    void parameterisedDefaultFilterTest(Class<?> clazz, List<String> expectedFieldNames) {
        List<Field> fields = ReflectionUtils.getDeclaredFields(clazz);
        assertEquals(expectedFieldNames.size(), fields.size(), "Unexpected number of fields for class " + clazz.getSimpleName());
        expectedFieldNames.forEach(name -> assertTrue(fields.stream().anyMatch(f -> f.getName().equals(name)), "Expected field '" + name + "' not found in " + clazz.getSimpleName()));
    }
}
