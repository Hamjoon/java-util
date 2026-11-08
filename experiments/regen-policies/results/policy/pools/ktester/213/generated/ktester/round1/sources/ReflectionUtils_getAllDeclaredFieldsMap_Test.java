package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import java.util.function.Predicate;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Stream;

// Mockito not used but kept to match template
@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getAllDeclaredFieldsMap_Test {

    // Simple class with a normal field and a static field
    static class SimpleClass {

        public int instanceField = 1;

        private static int staticField = 2;
    }

    // Parent/Child hierarchy with duplicate field names
    static class Parent {

        private int dup = 10;
    }

    static class Child extends Parent {

        private int dup = 20;
    }

    /**
     * Simple class with two instance fields – used for the “Simple Class without Duplicates” case.
     */
    static class SimpleTwoFieldClass {

        public int id = 1;

        public String name = "test";
    }

    /**
     * Base class that declares a private field named {@code value}.
     */
    static class BaseWithDuplicateField {

        private int value = 10;
    }

    /**
     * Child that also declares a private field named {@code value}.
     */
    static class ChildWithDuplicateField extends BaseWithDuplicateField {

        private int value = 20;
    }

    /**
     * Class containing one static and one instance field – used for custom static‑filter test.
     */
    static class ClassWithStaticField {

        public int instanceField = 5;

        public static final String STATIC_FIELD = "CONST";
    }

    /**
     * Class with three fields; the predicate will exclude {@code ignoreMe}.
     */
    static class ClassWithMultipleFields {

        public String keepMe = "a";

        public String another = "b";

        public String ignoreMe = "c";
    }

    /**
     * Class that declares no fields at all.
     */
    static class EmptyClass {
        // intentionally empty
    }

    /**
     * Class with a field named {@code metaClass} of type {@code groovy.lang.MetaClass}.
     */
    static class GroovyLikeClass {

        // The type name must be exactly "groovy.lang.MetaClass" for the default filter to drop it.
        // We create a dummy class with that fully‑qualified name below.
        public groovy.lang.MetaClass metaClass;

        public int id = 1;

        public String name = "groovy";
    }

    // The package declaration is simulated by a static inner class with the same binary name.
    public static final class groovy {

        public static final class lang {

            public static class MetaClass {
                // empty placeholder
            }
        }
    }

    public static final class lang {

        public static class MetaClass {
            // empty placeholder
        }
    }

    public static class MetaClass {
        // empty placeholder
    }

    public static final class lang {

        public static class MetaClass {
            // empty placeholder
        }
    }

    public static class MetaClass {
        // empty placeholder
    }

    public static class MetaClass {
        // empty placeholder
    }

    /**
     * Class with fields of different types – used for the “String‑only” predicate test.
     */
    static class MixedTypeClass {

        public String title = "t";

        public String description = "d";

        public int count = 3;

        public boolean flag = true;
    }

    /**
     * Parent class for order‑preservation test.
     */
    static class ParentClass {

        public String parentField = "p";
    }

    /**
     * Child class that adds its own field – order should be child then parent.
     */
    static class ChildClass extends ParentClass {

        public String childField = "c";
    }

    /**
     * Class that will be used to provoke a SecurityException when reflective access is attempted.
     */
    static class SecureClass {

        public int secret = 123;
    }

    /**
     * Supplies a stream of arguments for the custom predicate tests that exclude a specific field name.
     */
    static Stream<Arguments> excludeFieldNameProvider() {
        return Stream.of(Arguments.of(ClassWithMultipleFields.class, "ignoreMe"), Arguments.of(SimpleClass.class, "instanceField"));
    }

    @Test
    void throwsWhenClassIsNull() {
        Predicate<Field> any = f -> true;
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getAllDeclaredFieldsMap(null, any));
        assertTrue(ex.getMessage().contains("class cannot be null"));
    }

    @Test
    void throwsWhenPredicateIsNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getAllDeclaredFieldsMap(SimpleClass.class, null));
        assertTrue(ex.getMessage().contains("fieldFilter cannot be null"));
    }

    @Test
    void filtersStaticFieldsUsingDefaultFilter() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(SimpleClass.class, ReflectionUtils.DEFAULT_FIELD_FILTER);
        assertTrue(map.containsKey("instanceField"), "instance field should be present");
        assertFalse(map.containsKey("staticField"), "static field must be filtered out");
        assertEquals(1, map.size(), "Only the non‑static field should be returned");
    }

    @Test
    void handlesDuplicateFieldNamesInInheritance() {
        // Use a predicate that accepts every non‑static field
        Predicate<Field> acceptAll = f -> true;
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(ChildWithDuplicateField.class, acceptAll);
        // Expect two entries: one for the child’s “value” and one qualified with the parent’s class name
        assertEquals(2, map.size(), "Two entries expected for duplicate field names");
        assertTrue(map.containsKey("value"), "Child’s field should be stored under its simple name");
        String qualifiedParentKey = BaseWithDuplicateField.class.getName() + ".value";
        assertTrue(map.containsKey(qualifiedParentKey), "Parent’s field should be stored under a qualified name");
    }

    @Test
    void defaultFilterExcludesEnumSyntheticFields() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(SampleEnum.class, ReflectionUtils.DEFAULT_FIELD_FILTER);
        // customValue should be present, internal and ENUM$VALUES (synthetic) must be filtered out
        assertTrue(map.containsKey("customValue"), "User‑defined enum field must be present");
        assertFalse(map.containsKey("internal"), "Static internal field must be filtered");
        assertFalse(map.keySet().stream().anyMatch(k -> k.contains("ENUM$VALUES")), "Synthetic ENUM$VALUES field must not be present");
        // ordinal and hash are also filtered by the default predicate
        assertFalse(map.keySet().stream().anyMatch(k -> k.equals("ordinal") || k.equals("hash")), "Enum synthetic fields ordinal/hash must be filtered");
    }

    @Test
    void customPredicateCanExcludeSpecificFields() {
        // Predicate that excludes any field named "instanceField"
        Predicate<Field> excludeInstance = f -> !"instanceField".equals(f.getName());
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(SimpleClass.class, excludeInstance);
        assertFalse(map.containsKey("instanceField"), "Custom predicate should have excluded the field");
        assertEquals(0, map.size(), "No fields should remain after exclusion");
    }

    @Test
    void simpleClassWithoutDuplicates() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(SimpleTwoFieldClass.class, ReflectionUtils.DEFAULT_FIELD_FILTER);
        assertEquals(2, map.size(), "Exactly two fields expected");
        assertTrue(map.containsKey("id"));
        assertTrue(map.containsKey("name"));
        assertEquals(int.class, map.get("id").getType());
        assertEquals(String.class, map.get("name").getType());
    }

    @Test
    void customPredicateExcludesStaticFields() {
        Predicate<Field> nonStatic = f -> !Modifier.isStatic(f.getModifiers());
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(ClassWithStaticField.class, nonStatic);
        assertTrue(map.containsKey("instanceField"));
        assertFalse(map.containsKey("STATIC_FIELD"));
        assertEquals(1, map.size());
    }

    @Test
    void customPredicateExcludesSpecificFieldName() {
        Predicate<Field> excludeIgnoreMe = f -> !"ignoreMe".equals(f.getName());
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(ClassWithMultipleFields.class, excludeIgnoreMe);
        assertTrue(map.containsKey("keepMe"));
        assertTrue(map.containsKey("another"));
        assertFalse(map.containsKey("ignoreMe"));
        assertEquals(2, map.size());
    }

    @Test
    void emptyClassReturnsEmptyMap() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(EmptyClass.class, ReflectionUtils.DEFAULT_FIELD_FILTER);
        assertTrue(map.isEmpty(), "Empty class should yield an empty map");
    }

    @Test
    void customPredicateIncludesAllEnumFields() {
        Predicate<Field> includeAll = f -> true;
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(SampleEnum.class, includeAll);
        // All declared fields (including static internal) should appear
        assertTrue(map.containsKey("customValue"));
        assertTrue(map.containsKey("internal"));
        // The synthetic field ENUM$VALUES is not a declared field, but if it were, the predicate would let it through.
        // Verify that no unexpected synthetic field sneaks in.
        assertFalse(map.keySet().stream().anyMatch(k -> k.contains("ENUM$VALUES")), "Synthetic ENUM$VALUES should not be present as it is not a declared field");
    }

    @Test
    void groovyMetaClassFieldIsExcludedByDefaultFilter() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(GroovyLikeClass.class, ReflectionUtils.DEFAULT_FIELD_FILTER);
        assertFalse(map.containsKey("metaClass"), "metaClass field must be excluded by default filter");
        assertTrue(map.containsKey("id"));
        assertTrue(map.containsKey("name"));
        assertEquals(2, map.size());
    }

    @Test
    void predicateExceptionIsPropagated() {
        Predicate<Field> exploding = f -> {
            throw new RuntimeException("boom");
        };
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ReflectionUtils.getAllDeclaredFieldsMap(SimpleClass.class, exploding));
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void fieldFilterByStringTypeOnly() {
        Predicate<Field> stringsOnly = f -> f.getType() == String.class;
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(MixedTypeClass.class, stringsOnly);
        assertEquals(2, map.size());
        assertTrue(map.containsKey("title"));
        assertTrue(map.containsKey("description"));
        assertFalse(map.containsKey("count"));
        assertFalse(map.containsKey("flag"));
    }

    @Test
    void fieldOrderPreservationChildThenParent() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(ChildClass.class, ReflectionUtils.DEFAULT_FIELD_FILTER);
        // The map should be a LinkedHashMap preserving insertion order:
        // child fields first, then parent fields.
        List<String> expectedOrder = List.of("childField", "parentField");
        List<String> actualOrder = new ArrayList<>(map.keySet());
        assertEquals(expectedOrder, actualOrder, "Iteration order must reflect child‑first then parent‑field order");
    }

    @Disabled("SecurityManager behavior is platform‑dependent and may be disabled in modern JVMs.")
    @Test
    void securityExceptionWhenAccessingDeclaredFields() {
        // Install a custom SecurityManager that throws SecurityException for any reflective
        // access to SecureClass. This is only illustrative – many test environments disable
        // the SecurityManager, so the test is disabled by default.
        SecurityManager original = System.getSecurityManager();
        try {
            System.setSecurityManager(new SecurityManager() {

                @Override
                public void checkPermission(java.security.Permission perm) {
                    // allow everything else
                }

                @Override
                public void checkMemberAccess(Class<?> clazz, int which) {
                    if (clazz == SecureClass.class) {
                        throw new SecurityException("Access denied to SecureClass");
                    }
                }
            });
            assertThrows(SecurityException.class, () -> ReflectionUtils.getAllDeclaredFieldsMap(SecureClass.class, ReflectionUtils.DEFAULT_FIELD_FILTER));
        } finally {
            System.setSecurityManager(original);
        }
    }

    @ParameterizedTest(name = "exclude field \"{1}\" from {0}")
    @MethodSource("excludeFieldNameProvider")
    void customPredicateExcludesGivenFieldName(Class<?> clazz, String fieldToExclude) {
        Predicate<Field> predicate = f -> !fieldToExclude.equals(f.getName());
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(clazz, predicate);
        assertFalse(map.containsKey(fieldToExclude), "Field \"" + fieldToExclude + "\" should have been excluded by the predicate");
    }
}
