package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import java.util.function.Predicate;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getAllDeclaredFieldsMap_2_Test {

    /* -------------------------------------------------
       Helper types used by the tests
       ------------------------------------------------- */
    static class SimpleClass {

        private int instanceField1;

        protected String instanceField2;

        public static final double STATIC_CONST = 3.14;

        static int staticField;
    }

    class SuperClass {

        private long superField;
    }

    class SubClass extends SuperClass {

        private int subField;
    }

    static class StaticFieldClass {

        private int instanceField;

        static int staticField;
    }

    // Groovy MetaClass test – disabled because Groovy is not on the classpath.
    @Disabled("Groovy library not present; test verifies intended behaviour when available.")
    static class GroovyMetaClassHolder {

        // The type name must be exactly "groovy.lang.MetaClass" for the filter to match.
        // A placeholder class is used when Groovy is available.
        // compile‑time requires Groovy
        private groovy.lang.MetaClass metaClass;

        private int regularField;
    }

    static class ParentClass {

        private long parentField;
    }

    static class ChildClass extends ParentClass {

        private int childField;
    }

    static class ShadowParent {

        private String shadow;
    }

    static class ShadowingChild extends ShadowParent {

        // redeclared – should shadow the parent field
        private String shadow;
    }

    static class EmptyClass {
        // No fields
    }

    static class SampleClass {

        private int foo;
    }

    /* -------------------------------------------------
       Tests for ReflectionUtils.getAllDeclaredFieldsMap(...)
       ------------------------------------------------- */
    @Test
    void testGetAllDeclaredFieldsMap_DefaultFilter_IncludesInstanceFieldsAndExcludesStatic() {
        // Act
        Map<String, Field> fieldMap = ReflectionUtils.getAllDeclaredFieldsMap(SimpleClass.class);
        // Assert – only the two instance fields should be present
        assertEquals(2, fieldMap.size(), "Expected exactly two instance fields");
        assertTrue(fieldMap.containsKey("instanceField1"));
        assertTrue(fieldMap.containsKey("instanceField2"));
        // Verify that static fields are not present
        assertFalse(fieldMap.containsKey("STATIC_CONST"));
        assertFalse(fieldMap.containsKey("staticField"));
    }

    @Test
    void testGetAllDeclaredFieldsMap_DefaultFilter_FiltersEnumSyntheticFields() {
        // Act
        Map<String, Field> enumFieldMap = ReflectionUtils.getAllDeclaredFieldsMap(SampleEnum.class);
        // The default filter should remove the synthetic "$VALUES" and "enumConstantDirectory"
        // as well as any static fields added by the compiler.
        // For this simple enum we expect the map to be empty because all declared fields are filtered.
        assertTrue(enumFieldMap.isEmpty(), "Enum synthetic/static fields should be filtered out, leaving an empty map");
    }

    @Test
    void testGetAllDeclaredFieldsMap_InheritedFieldsAreIncluded() {
        // Arrange – subclass inherits fields from its superclass
        class SuperClass {

            private long superField;
        }
        class SubClass extends SuperClass {

            private int subField;
        }
        // Act
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(SubClass.class);
        // Assert – both subclass and superclass instance fields should be present
        assertEquals(2, map.size(), "Both subclass and superclass instance fields should be returned");
        assertTrue(map.containsKey("subField"));
        assertTrue(map.containsKey("superField"));
    }

    @Test
    void testStaticFieldExclusion() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(StaticFieldClass.class);
        assertTrue(map.containsKey("instanceField"));
        assertFalse(map.containsKey("staticField"));
    }

    @Test
    void testEnumInternalFieldsExclusion() {
        Map<String, Field> enumMap = ReflectionUtils.getAllDeclaredFieldsMap(SampleEnum.class);
        // The default filter should keep the user‑defined field 'value' and drop synthetic ones.
        assertTrue(enumMap.containsKey("value"));
        assertFalse(enumMap.containsKey("internal"));
        assertFalse(enumMap.containsKey("ENUM$VALUES"));
    }

    @Test
    void testEnumHashAndOrdinalExclusion() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(OrdinalEnum.class);
        assertTrue(map.containsKey("custom"));
        assertFalse(map.containsKey("hash"));
        assertFalse(map.containsKey("ordinal"));
    }

    @Test
    @Disabled("Groovy library not present; test would fail to compile without groovy.lang.MetaClass.")
    void testGroovyMetaClassFieldExclusion() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(GroovyMetaClassHolder.class);
        assertTrue(map.containsKey("regularField"));
        assertFalse(map.containsKey("metaClass"));
    }

    @Test
    void testInheritedFieldsAreIncluded() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(ChildClass.class);
        assertEquals(2, map.size(), "Both subclass and superclass instance fields should be returned");
        assertTrue(map.containsKey("childField"));
        assertTrue(map.containsKey("parentField"));
    }

    @Test
    void testDuplicateFieldNameOverride() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(ShadowingChild.class);
        assertEquals(1, map.size());
        assertTrue(map.containsKey("shadow"));
        Field shadowField = map.get("shadow");
        // The declaring class should be the subclass, not the parent.
        assertEquals(ShadowingChild.class, shadowField.getDeclaringClass(), "Shadowing field should be the one declared in the subclass");
    }

    @Test
    void testEmptyClassReturnsEmptyMap() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(EmptyClass.class);
        assertTrue(map.isEmpty(), "Empty class should produce an empty map");
    }

    @Test
    void testNullClassThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getAllDeclaredFieldsMap(null), "Passing null should raise IllegalArgumentException");
    }

    @Test
    void testNullPredicateThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getAllDeclaredFieldsMap(SampleClass.class, null), "Null predicate should raise NullPointerException");
    }

    @Test
    void testPredicateRuntimeExceptionPropagation() {
        Predicate<Field> throwingPredicate = field -> {
            throw new IllegalStateException("Predicate failure");
        };
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> ReflectionUtils.getAllDeclaredFieldsMap(SampleClass.class, throwingPredicate), "Exception from predicate must propagate");
        assertEquals("Predicate failure", ex.getMessage());
    }

    @Test
    void testPrimitiveClassReturnsEmptyMap() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(int.class);
        assertTrue(map.isEmpty(), "Primitive types have no declared fields");
    }

    @Test
    void testArrayClassReturnsEmptyMap() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(String[].class);
        assertTrue(map.isEmpty(), "Array classes have no declared fields");
    }
}
