package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import java.util.function.Predicate;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getAllDeclaredFieldsMap_2_Test {

    /* -----------------------------------------------------------
       1.  Simple POJO – two instance fields.
       ----------------------------------------------------------- */
    static class SimplePojo {

        private int fieldA;

        protected String fieldB;
    }

    /* -----------------------------------------------------------
       2.  Static field must be filtered out by the default filter.
       ----------------------------------------------------------- */
    static class WithStatic {

        private int instanceField;

        public static final String STATIC_FIELD = "CONST";
    }

    /* -----------------------------------------------------------
       5.  Inheritance – fields from super‑classes are also present.
       ----------------------------------------------------------- */
    static class SuperClass {

        private long superField;
    }

    static class SubClass extends SuperClass {

        private double subField;
    }

    /* -----------------------------------------------------------
       1. Simple class – a single instance field.
       ----------------------------------------------------------- */
    static class SimpleClass {

        private int instanceField;
    }

    /* -----------------------------------------------------------
       2. Class that only declares a static field – should be filtered out.
       ----------------------------------------------------------- */
    static class ClassWithStaticField {

        public static final String STATIC_FIELD = "CONST";
    }

    /* -----------------------------------------------------------
       5. Groovy metaClass field – should be filtered out.
       ----------------------------------------------------------- */
    @Disabled("Groovy library not present on the classpath – kept for documentation purposes")
    static class GroovyMetaClassHolder {

        // The type name must be exactly "groovy.lang.MetaClass" for the filter to match.
        // In a real project the Groovy library would provide this class.
        // use Object to avoid compile‑time dependency on Groovy
        public Object metaClass;
    }

    /* -----------------------------------------------------------
       6. Enum that also declares a field named "hash" – should be kept because the
       declaring class is not java.lang.Enum.
       ----------------------------------------------------------- */
    static class ClassWithHashField {

        private int hash;
    }

    /* -----------------------------------------------------------
       8. Custom predicate – exclude a particular non‑static field.
       ----------------------------------------------------------- */
    static class MixedClass {

        private int instanceField;

        private static final String STATIC_FIELD = "CONST";

        // placeholder – will be filtered by default but not by custom predicate
        // use Object instead of Groovy's MetaClass
        public Object metaClass;

//         private EnumWithCustomField customEnumField;

        // not an enum hash, should stay
        private int hash;
    }

    /* -----------------------------------------------------------
       10. Field hiding – subclass redeclares a field with the same name.
       ----------------------------------------------------------- */
    static class HideSuper {

        private String name;
    }

    static class HideSub extends HideSuper {

        // hides the super field
        private String name;

        private int age;
    }

    static Stream<Class<?>> primitiveAndArrayProvider() {
        return Stream.of(int.class, double.class, boolean.class, String[].class, int[].class);
    }

    @Test
    void getAllDeclaredFieldsMap_returnsAllInstanceFields() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(SimplePojo.class);
        assertEquals(2, map.size(), "SimplePojo should expose exactly 2 fields");
        assertTrue(map.containsKey("fieldA"));
        assertTrue(map.containsKey("fieldB"));
        // Verify the fields belong to SimplePojo
        assertEquals(SimplePojo.class, map.get("fieldA").getDeclaringClass());
        assertEquals(SimplePojo.class, map.get("fieldB").getDeclaringClass());
    }

    @Test
    void defaultFilter_excludesStaticFields() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(WithStatic.class);
        assertEquals(1, map.size(), "Only the instance field should survive the default filter");
        assertTrue(map.containsKey("instanceField"));
        assertFalse(map.containsKey("STATIC_FIELD"));
    }

//     @Test
//     void defaultFilter_excludesEnumSyntheticFields() {
//         Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(SampleEnum.class);
//         // Enum declares: name, ordinal (from java.lang.Enum) + our 'value' + synthetic $VALUES
//         // The default filter removes static, internal, ENUM$VALUES, hash, ordinal etc.
//         assertEquals(1, map.size(), "Only the user‑declared instance field should be present");
//         assertTrue(map.containsKey("value"));
//     }

    /* -----------------------------------------------------------
       4.  Custom predicate – include static fields.
       ----------------------------------------------------------- */
    @Test
    void customPredicate_canIncludeStaticFields() {
        // accept everything
        Predicate<Field> includeAll = f -> true;
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(WithStatic.class, includeAll);
        assertEquals(2, map.size(), "Custom predicate should expose both instance and static fields");
        assertTrue(map.containsKey("instanceField"));
        assertTrue(map.containsKey("STATIC_FIELD"));
    }

    @Test
    void getAllDeclaredFieldsMap_traversesClassHierarchy() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(SubClass.class);
        assertEquals(2, map.size(), "Both super‑class and sub‑class fields should be returned");
        assertTrue(map.containsKey("superField"));
        assertTrue(map.containsKey("subField"));
    }

    @Test
    void defaultFilter_includesRegularInstanceField() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(SimpleClass.class);
        assertEquals(1, map.size(), "SimpleClass should expose exactly one instance field");
        assertTrue(map.containsKey("instanceField"));
        assertEquals(SimpleClass.class, map.get("instanceField").getDeclaringClass());
    }

    @Test
    void defaultFilter_excludesStaticOnlyClass() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(ClassWithStaticField.class);
        assertTrue(map.isEmpty(), "Static‑only fields must be filtered out, resulting in an empty map");
    }

//     @Test
//     void defaultFilter_excludesAllSyntheticEnumFields() {
//         Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(EmptyEnum.class);
//         assertTrue(map.isEmpty(), "Enum with no user fields should yield an empty map after filtering");
//     }

//     @Test
//     void defaultFilter_includesUserDeclaredEnumInstanceField() {
//         Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(EnumWithCustomField.class);
//         assertEquals(1, map.size(), "Only the user‑declared instance field should be present");
//         assertTrue(map.containsKey("customValue"));
//     }

    @Test
    @Disabled("Groovy library not present – test skipped")
    void defaultFilter_excludesGroovyMetaClassField() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(GroovyMetaClassHolder.class);
        assertTrue(map.isEmpty(), "Groovy metaClass field must be filtered out");
    }

    @Test
    void defaultFilter_allowsHashFieldWhenNotEnum() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(ClassWithHashField.class);
        assertEquals(1, map.size());
        assertTrue(map.containsKey("hash"));
    }

    /* -----------------------------------------------------------
       7. Custom predicate – include static fields.
       ----------------------------------------------------------- */
    @Test
    void customPredicate_includesStaticFields() {
        // accept everything
        Predicate<Field> includeAll = f -> true;
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(ClassWithStaticField.class, includeAll);
        assertEquals(1, map.size(), "Custom predicate should expose the static field");
        assertTrue(map.containsKey("STATIC_FIELD"));
    }

    @Test
    void customPredicate_excludesSpecificField() {
        Predicate<Field> withoutCustomEnum = f -> !f.getName().equals("customEnumField");
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(MixedClass.class, withoutCustomEnum);
        // Expected: instanceField, hash, STATIC_FIELD, metaClass (type unknown but still present)
        assertTrue(map.containsKey("instanceField"));
        assertTrue(map.containsKey("hash"));
        assertTrue(map.containsKey("STATIC_FIELD"));
        assertTrue(map.containsKey("metaClass"));
        assertFalse(map.containsKey("customEnumField"));
    }

    @Test
    void defaultFilter_traversesClassHierarchy() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(SubClass.class);
        assertEquals(2, map.size(), "Both super‑class and sub‑class fields should be returned");
        assertTrue(map.containsKey("superField"));
        assertTrue(map.containsKey("subField"));
    }

    @Test
    void inheritance_fieldHidingKeepsSubclassVersion() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(HideSub.class);
        assertEquals(2, map.size());
        Field nameField = map.get("name");
        assertNotNull(nameField);
        assertEquals(HideSub.class, nameField.getDeclaringClass(), "When a field is hidden, the map must contain the subclass declaration");
        assertTrue(map.containsKey("age"));
    }

    /* -----------------------------------------------------------
       11. Class with no declared fields (java.lang.Object).
       ----------------------------------------------------------- */
    @Test
    void defaultFilter_classWithNoFields_returnsEmptyMap() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(Object.class);
        assertTrue(map.isEmpty(), "Object has no declared fields – map must be empty");
    }

    /* -----------------------------------------------------------
       12. Primitive and array types – should return empty maps.
       ----------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("primitiveAndArrayProvider")
    void defaultFilter_primitiveOrArray_returnsEmptyMap(Class<?> clazz) {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(clazz);
        assertTrue(map.isEmpty(), clazz + " should yield an empty map");
    }

    /* -----------------------------------------------------------
       13. Null class argument – must throw IllegalArgumentException.
       ----------------------------------------------------------- */
    @Test
    void getAllDeclaredFieldsMap_nullClass_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getAllDeclaredFieldsMap(null), "Passing null should result in IllegalArgumentException");
    }

    /* -----------------------------------------------------------
       14. Null predicate argument – must throw IllegalArgumentException.
       ----------------------------------------------------------- */
    @Test
    void getAllDeclaredFieldsMap_nullPredicate_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getAllDeclaredFieldsMap(SimpleClass.class, null), "Passing null predicate should result in IllegalArgumentException");
    }

    /* -----------------------------------------------------------
       15. SecurityManager that denies reflective access.
       ----------------------------------------------------------- */
    @Test
    @Disabled("SecurityManager is deprecated and may be disabled by the JVM; kept for completeness")
    void securityManager_deniesReflection_propagatesException() {
        SecurityManager original = System.getSecurityManager();
        try {
            System.setSecurityManager(new SecurityManager() {

                public void checkPermission(java.security.Permission perm) {
                    // allow everything else
                }

                protected void checkMemberAccess(Class<?> clazz, int which) {
                    throw new SecurityException("Access denied");
                }
            });
            assertThrows(SecurityException.class, () -> ReflectionUtils.getAllDeclaredFieldsMap(SimpleClass.class));
        } finally {
            System.setSecurityManager(original);
        }
    }
}