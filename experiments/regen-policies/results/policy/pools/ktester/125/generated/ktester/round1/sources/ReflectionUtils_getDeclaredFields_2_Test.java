package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.security.Permission;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getDeclaredFields_2_Test {

    /**
     * Simple POJO with both instance and static fields.
     */
    static class SimpleClass {

        private int instanceField;

        public static String staticField = "STATIC";
    }

    /**
     * Class that contains a field named "metaClass" but of a non‑Groovy type.
     *  The default filter only removes it when the type is {@code groovy.lang.MetaClass}.
     */
    static class GroovyMetaClassHolder {

        // should be kept
        private Object metaClass = new Object();

        private String otherField = "other";
    }

    /**
     * Empty class – no declared fields.
     */
    static class EmptyClass {
    }

    /**
     * Parent / child hierarchy to test that only declared fields of the child are returned.
     */
    static class ParentClass {

        private int parentField = 1;
    }

    static class ChildClass extends ParentClass {

        private int childField = 2;
    }

    /**
     * Regular class with a field named "internal". Should NOT be filtered.
     */
    static class ClassWithInternalField {

        private String internal = "keep";
    }

    /**
     * Regular class with a field named "metaClass" of type {@link Object}. Should be kept.
     */
    static class ClassWithMetaClassDifferentType {

        private Object metaClass = new Object();
    }

    /**
     * Regular class with fields named "hash" and "ordinal". Should be kept.
     */
    static class ClassWithHashOrdinalFields {

        private int hash = 7;

        private int ordinal = 8;

        private String regular = "ok";
    }

    /**
     * Class used for custom predicate tests – includes a static field.
     */
    static class ClassWithExcludeField {

        private String includeMe = "yes";

        private String excludeMe = "no";
    }

    /**
     * Class that will trigger a {@link SecurityException} when reflective access is attempted.
     */
    static class SecureClass {

        private int secret = 42;
    }

    class GroovyHolder {

        // name matches, type matches -> filtered
        private metaClassType metaClass;

        private String other = "ok";
    }

    class GroovyHolder {

        // name matches, type matches -> filtered
        private metaClassType metaClass;

        private String other = "ok";
    }

    private static Stream<Arguments> defaultFilterScenarios() {
        return Stream.of(Arguments.of(SimpleClass.class, List.of("instanceField")), Arguments.of(SampleEnum.class, List.of("label")), Arguments.of(HashOrdinalEnum.class, List.of()), Arguments.of(EmptyClass.class, List.of()));
    }

    @Test
    void getDeclaredFields_ignoresStaticFields() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(SimpleClass.class);
        List<String> names = fields.stream().map(Field::getName).collect(Collectors.toList());
        // Only the instance field should be present.
        assertEquals(1, fields.size(), "Static fields must be filtered out");
        assertTrue(names.contains("instanceField"));
        assertFalse(names.contains("staticField"));
    }

    @Test
    void getDeclaredFields_filtersSyntheticEnumFields() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(SampleEnum.class);
        List<String> names = fields.stream().map(Field::getName).collect(Collectors.toList());
        // The enum should expose only the user‑declared instance field "label".
        assertEquals(1, fields.size(), "Synthetic enum fields must be filtered out");
        assertTrue(names.contains("label"));
        assertFalse(names.contains("ENUM$VALUES"));
        assertFalse(names.contains("internal"));
    }

    @Test
    void getDeclaredFields_filtersHashAndOrdinalForEnumSubclass() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(HashOrdinalEnum.class);
        List<String> names = fields.stream().map(Field::getName).collect(Collectors.toList());
        // The default filter removes "hash" and "ordinal" fields.
        assertEquals(0, fields.size(), "Fields named hash and ordinal must be filtered out");
        assertFalse(names.contains("hash"));
        assertFalse(names.contains("ordinal"));
    }

    @Test
    void getDeclaredFields_nullClassThrowsNPE() {
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getDeclaredFields(null));
    }

    @Test
    void getDeclaredFields_enumWithInternalField_keepsNonStaticInternal() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(EnumWithInternal.class);
        List<String> names = fields.stream().map(Field::getName).collect(Collectors.toList());
        // Both fields are non‑static and should be present.
        assertEquals(2, fields.size());
        assertTrue(names.containsAll(Arrays.asList("internal", "normalField")));
    }

    @Test
    void getDeclaredFields_groovyMetaClassField_isFilteredWhenTypeMatches() throws Exception {
        // Simulate Groovy's MetaClass type via reflection (we don't have the class at compile time).
        Class<?> metaClassType = Class.forName("groovy.lang.MetaClass");
        // Create a holder class with a field of that exact type.
        class GroovyHolder {

            // name matches, type matches -> filtered
            private metaClassType metaClass;

            private String other = "ok";
        }
        List<Field> fields = ReflectionUtils.getDeclaredFields(GroovyHolder.class);
        List<String> names = fields.stream().map(Field::getName).collect(Collectors.toList());
        assertEquals(1, fields.size());
        assertTrue(names.contains("other"));
        assertFalse(names.contains("metaClass"));
    }

    @Test
    void getDeclaredFields_groovyMetaClassField_notFilteredWhenDifferentType() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(GroovyMetaClassHolder.class);
        List<String> names = fields.stream().map(Field::getName).collect(Collectors.toList());
        assertEquals(2, fields.size());
        assertTrue(names.containsAll(Arrays.asList("metaClass", "otherField")));
    }

    @Test
    void getDeclaredFields_enumWithHashOrdinal_customPredicateKeepsAll() {
        // Use the overload with a predicate that always returns true.
        Predicate<Field> all = f -> true;
        List<Field> fields = ReflectionUtils.getDeclaredFields(EnumWithHashOrdinal.class, all);
        List<String> names = fields.stream().map(Field::getName).collect(Collectors.toList());
        // All three fields should be present.
        assertEquals(3, fields.size());
        assertTrue(names.containsAll(Arrays.asList("hash", "ordinal", "regularField")));
    }

    @Test
    void getDeclaredFields_emptyClass_returnsEmptyList() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(EmptyClass.class);
        assertTrue(fields.isEmpty(), "Empty class should yield an empty list");
    }

    @Test
    void getDeclaredFields_inheritanceOnlyDeclaredFieldsReturned() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(ChildClass.class);
        List<String> names = fields.stream().map(Field::getName).collect(Collectors.toList());
        assertEquals(1, fields.size());
        assertTrue(names.contains("childField"));
        assertFalse(names.contains("parentField"));
    }

    @Test
    void getDeclaredFields_nonEnumClassWithInternalField_isNotFiltered() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(ClassWithInternalField.class);
        List<String> names = fields.stream().map(Field::getName).collect(Collectors.toList());
        assertEquals(1, fields.size());
        assertTrue(names.contains("internal"));
    }

    @Test
    void getDeclaredFields_nonEnumClassWithMetaClassDifferentType_isNotFiltered() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(ClassWithMetaClassDifferentType.class);
        List<String> names = fields.stream().map(Field::getName).collect(Collectors.toList());
        assertEquals(1, fields.size());
        assertTrue(names.contains("metaClass"));
    }

    @Test
    void getDeclaredFields_nonEnumClassWithHashOrdinalFields_areKept() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(ClassWithHashOrdinalFields.class);
        List<String> names = fields.stream().map(Field::getName).collect(Collectors.toList());
        assertEquals(3, fields.size());
        assertTrue(names.containsAll(Arrays.asList("hash", "ordinal", "regular")));
    }

    @Test
    void getDeclaredFields_withCustomPredicate_includesStaticFields() {
        // accept everything
        Predicate<Field> all = f -> true;
        List<Field> fields = ReflectionUtils.getDeclaredFields(SimpleClass.class, all);
        List<String> names = fields.stream().map(Field::getName).collect(Collectors.toList());
        assertEquals(2, fields.size());
        assertTrue(names.containsAll(Arrays.asList("instanceField", "staticField")));
    }

    @Test
    void getDeclaredFields_withCustomPredicate_excludesSpecificField() {
        Predicate<Field> exclude = f -> !"excludeMe".equals(f.getName());
        List<Field> fields = ReflectionUtils.getDeclaredFields(ClassWithExcludeField.class, exclude);
        List<String> names = fields.stream().map(Field::getName).collect(Collectors.toList());
        assertEquals(1, fields.size());
        assertTrue(names.contains("includeMe"));
        assertFalse(names.contains("excludeMe"));
    }

    @Test
    void getDeclaredFields_withNullPredicateThrowsNPE() {
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getDeclaredFields(SimpleClass.class, null));
    }

    @Test
    void getDeclaredFields_securityManagerDeniesAccess() {
        SecurityManager original = System.getSecurityManager();
        try {
            System.setSecurityManager(new SecurityManager() {

                @Override
                public void checkPermission(Permission perm) {
                    // Allow everything except reflective access.
                }

                @Override
                public void checkMemberAccess(Class<?> clazz, int which) {
                    throw new SecurityException("Reflective access denied");
                }
            });
            assertThrows(SecurityException.class, () -> ReflectionUtils.getDeclaredFields(SecureClass.class));
        } finally {
            // Restore original security manager to avoid side‑effects on other tests.
            System.setSecurityManager(original);
        }
    }

    @ParameterizedTest(name = "Default filter on {0} yields {1}")
    @MethodSource("defaultFilterScenarios")
    void parameterizedDefaultFilter(Class<?> target, List<String> expectedNames) {
        List<String> actual = ReflectionUtils.getDeclaredFields(target).stream().map(Field::getName).collect(Collectors.toList());
        assertEquals(expectedNames.size(), actual.size());
        assertTrue(actual.containsAll(expectedNames));
    }
}
