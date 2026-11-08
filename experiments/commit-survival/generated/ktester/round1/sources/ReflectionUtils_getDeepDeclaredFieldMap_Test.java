package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getDeepDeclaredFieldMap_Test {

    static class SimpleClass {

        public int publicInt;

        private String privateString;

        protected double protectedDouble;
    }

    static class SuperClass {

        private int duplicate;

        public long superLong;
    }

    static class SubClass extends SuperClass {

        // same name as in SuperClass, different type
        private String duplicate;

        public boolean subBool;
    }

    // Simple class with no fields
    static class EmptyClass {
    }

    // Simple POJO used for the “basic scenario” with two fields
    static class SimpleClass2 {

        private int id;

        public String name;
    }

    // Parent / child hierarchy used to test duplicate private field names
    static class ParentClass {

        // parent field (private)
        private String value;
    }

    static class ChildClass extends ParentClass {

        // child field (same name, different type)
        private int value;

        public String extra;
    }

    // Interface with a constant field
    interface SampleInterface {

        String CONST = "CONST";
    }

    // Class containing both static and instance fields
    static class StaticFieldClass {

        static int staticVal;

        int instanceVal;
    }

    // Three‑level inheritance with the same field name in each class
    static class GrandParentClass {

        private String value;
    }

    static class ParentClass2 extends GrandParentClass {

        private String value;
    }

    static class GrandChild extends ParentClass2 {

        private String value;
    }

    // Base class with protected field, subclass with private field of same name
    static class BaseClass {

        protected String shared;
    }

    static class MixedVisibilityClass extends BaseClass {

        private String shared;
    }

    // Class used to simulate a SecurityException from getDeepDeclaredFields
    static class RestrictedClass {
    }

    // Example of a parameter source – not used currently but kept for extensibility
    static Stream<Class<?>> emptyClassProvider() {
        return Stream.of(EmptyClass.class);
    }

    @Test
    void getDeepDeclaredFieldMap_returnsAllFieldsWithSimpleNames() {
        // GIVEN
        Class<?> clazz = SimpleClass.class;
        // WHEN
        Map<String, Field> fieldMap = ReflectionUtils.getDeepDeclaredFieldMap(clazz);
        // THEN
        assertNotNull(fieldMap, "Resulting map must not be null");
        // SimpleClass declares three fields
        assertEquals(3, fieldMap.size(), "Map should contain exactly three entries");
        // Verify each expected key is present and maps to the correct Field instance
        Field publicInt = fieldMap.get("publicInt");
        assertNotNull(publicInt, "publicInt field should be present");
        assertEquals(int.class, publicInt.getType());
        Field privateString = fieldMap.get("privateString");
        assertNotNull(privateString, "privateString field should be present");
        assertEquals(String.class, privateString.getType());
        Field protectedDouble = fieldMap.get("protectedDouble");
        assertNotNull(protectedDouble, "protectedDouble field should be present");
        assertEquals(double.class, protectedDouble.getType());
    }

    @Test
    void getDeepDeclaredFieldMap_handlesDuplicatePrivateFieldNames() {
        // GIVEN
        Class<?> clazz = SubClass.class;
        // WHEN
        Map<String, Field> fieldMap = ReflectionUtils.getDeepDeclaredFieldMap(clazz);
        // THEN
        assertNotNull(fieldMap, "Resulting map must not be null");
        // SubClass declares two fields (duplicate, subBool) and inherits one (superLong)
        // SuperClass also declares a private field named duplicate.
        // Total distinct fields = 4 (duplicate appears twice, one entry will be qualified)
        assertEquals(4, fieldMap.size(), "Map should contain four entries (one qualified duplicate key)");
        // The simple name "duplicate" will correspond to the field discovered first
        // (the order of getDeepDeclaredFields is subclass‑first, then super‑class).
        Field duplicateSimple = fieldMap.get("duplicate");
        assertNotNull(duplicateSimple, "Simple duplicate key must exist");
        assertEquals(String.class, duplicateSimple.getType(), "Simple duplicate should refer to SubClass's field");
        // The qualified key should reference the SuperClass's duplicate field
        String qualifiedKey = SuperClass.class.getName() + ".duplicate";
        Field duplicateQualified = fieldMap.get(qualifiedKey);
        assertNotNull(duplicateQualified, "Qualified duplicate key must exist");
        assertEquals(int.class, duplicateQualified.getType(), "Qualified duplicate should refer to SuperClass's field");
        // Verify other fields are present with their simple names
        Field subBool = fieldMap.get("subBool");
        assertNotNull(subBool, "subBool field should be present");
        assertEquals(boolean.class, subBool.getType());
        Field superLong = fieldMap.get("superLong");
        assertNotNull(superLong, "superLong field should be present");
        assertEquals(long.class, superLong.getType());
    }

    /**
     * Basic scenario – class with no declared fields.
     */
    @Test
    void getDeepDeclaredFieldMap_EmptyClass_returnsEmptyMap() {
        Map<String, Field> map = ReflectionUtils.getDeepDeclaredFieldMap(EmptyClass.class);
        assertNotNull(map);
        assertTrue(map.isEmpty(), "EmptyClass should yield an empty map");
    }

    /**
     * Basic scenario – simple POJO with two fields, no name clash.
     */
    @Test
    void getDeepDeclaredFieldMap_SimpleClass2_containsIdAndName() {
        Map<String, Field> map = ReflectionUtils.getDeepDeclaredFieldMap(SimpleClass2.class);
        assertNotNull(map);
        assertEquals(2, map.size(), "SimpleClass2 should have exactly two entries");
        Field id = map.get("id");
        assertNotNull(id);
        assertEquals(int.class, id.getType());
        Field name = map.get("name");
        assertNotNull(name);
        assertEquals(String.class, name.getType());
    }

    /**
     * Basic scenario – child class hides a private field from its parent.
     */
    @Test
    void getDeepDeclaredFieldMap_ChildClass_duplicateHandling() {
        Map<String, Field> map = ReflectionUtils.getDeepDeclaredFieldMap(ChildClass.class);
        assertNotNull(map);
        assertEquals(3, map.size(), "ChildClass should produce three map entries");
        // Child's private field should win the simple name
        Field childValue = map.get("value");
        assertNotNull(childValue);
        assertEquals(int.class, childValue.getType());
        // Parent's field must be stored under a qualified key
        String qualifiedKey = ParentClass.class.getName() + ".value";
        Field parentValue = map.get(qualifiedKey);
        assertNotNull(parentValue);
        assertEquals(String.class, parentValue.getType());
        // The extra field from the child stays simple
        Field extra = map.get("extra");
        assertNotNull(extra);
        assertEquals(String.class, extra.getType());
    }

    /**
     * Edge case – null argument should result in NPE.
     */
    @Test
    void getDeepDeclaredFieldMap_nullArgument_throwsNPE() {
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getDeepDeclaredFieldMap(null), "Passing null should throw NullPointerException");
    }

    /**
     * Edge case – primitive type has no declared fields.
     */
    @Test
    void getDeepDeclaredFieldMap_primitiveClass_returnsEmptyMap() {
        Map<String, Field> map = ReflectionUtils.getDeepDeclaredFieldMap(int.class);
        assertNotNull(map);
        assertTrue(map.isEmpty(), "Primitive class should yield an empty map");
    }

    /**
     * Edge case – array class has no declared fields.
     */
    @Test
    void getDeepDeclaredFieldMap_arrayClass_returnsEmptyMap() {
        Map<String, Field> map = ReflectionUtils.getDeepDeclaredFieldMap(String[].class);
        assertNotNull(map);
        assertTrue(map.isEmpty(), "Array class should yield an empty map");
    }

    /**
     * Edge case – interfaces can declare (public static final) fields.
     */
    @Test
    void getDeepDeclaredFieldMap_interfaceIncludesConstants() {
        Map<String, Field> map = ReflectionUtils.getDeepDeclaredFieldMap(SampleInterface.class);
        assertNotNull(map);
        assertEquals(1, map.size(), "Interface should expose its constant field");
        Field constField = map.get("CONST");
        assertNotNull(constField);
        assertEquals(String.class, constField.getType());
        assertTrue(Modifier.isStatic(constField.getModifiers()));
        assertTrue(Modifier.isPublic(constField.getModifiers()));
    }

    /**
     * Edge case – static fields are treated like any other declared field.
     */
    @Test
    void getDeepDeclaredFieldMap_staticAndInstanceFields() {
        Map<String, Field> map = ReflectionUtils.getDeepDeclaredFieldMap(StaticFieldClass.class);
        assertNotNull(map);
        assertEquals(2, map.size(), "Both static and instance fields should be present");
        Field staticField = map.get("staticVal");
        assertNotNull(staticField);
        assertTrue(Modifier.isStatic(staticField.getModifiers()));
        Field instanceField = map.get("instanceVal");
        assertNotNull(instanceField);
        assertFalse(Modifier.isStatic(instanceField.getModifiers()));
    }

    /**
     * Edge case – three‑level inheritance with repeated field name.
     */
    @Test
    void getDeepDeclaredFieldMap_GrandChild_multipleDuplicates() {
        Map<String, Field> map = ReflectionUtils.getDeepDeclaredFieldMap(GrandChild.class);
        assertNotNull(map);
        assertEquals(3, map.size(), "GrandChild hierarchy should yield three entries");
        // Simple name refers to the most‑derived field
        Field childValue = map.get("value");
        assertNotNull(childValue);
        assertEquals(String.class, childValue.getType());
        assertEquals(GrandChild.class, childValue.getDeclaringClass());
        // Ancestors are stored with qualified keys
        String parentKey = ParentClass2.class.getName() + ".value";
        Field parentValue = map.get(parentKey);
        assertNotNull(parentValue);
        assertEquals(String.class, parentValue.getType());
        assertEquals(ParentClass2.class, parentValue.getDeclaringClass());
        String grandParentKey = GrandParentClass.class.getName() + ".value";
        Field grandParentValue = map.get(grandParentKey);
        assertNotNull(grandParentValue);
        assertEquals(String.class, grandParentValue.getType());
        assertEquals(GrandParentClass.class, grandParentValue.getDeclaringClass());
    }

    /**
     * Edge case – same field name with different visibility modifiers.
     */
    @Test
    void getDeepDeclaredFieldMap_MixedVisibilityClass_visibilityHandling() {
        Map<String, Field> map = ReflectionUtils.getDeepDeclaredFieldMap(MixedVisibilityClass.class);
        assertNotNull(map);
        assertEquals(2, map.size(), "MixedVisibilityClass should have two entries");
        // Child's private field wins the simple name
        Field childShared = map.get("shared");
        assertNotNull(childShared);
        assertEquals(MixedVisibilityClass.class, childShared.getDeclaringClass());
        // Parent's protected field is stored with qualified key
        String qualifiedKey = BaseClass.class.getName() + ".shared";
        Field parentShared = map.get(qualifiedKey);
        assertNotNull(parentShared);
        assertEquals(BaseClass.class, parentShared.getDeclaringClass());
    }

    /**
     * Exception scenario – underlying reflective call throws SecurityException.
     */
    @Test
    void getDeepDeclaredFieldMap_RestrictedClass_propagatesSecurityException() {
        try (MockedStatic<ReflectionUtils> mocked = mockStatic(ReflectionUtils.class, CALLS_REAL_METHODS)) {
            // When getDeepDeclaredFields is called for RestrictedClass, throw SecurityException
            mocked.when(() -> ReflectionUtils.getDeepDeclaredFields(RestrictedClass.class)).thenThrow(new SecurityException("Access denied"));
            // The wrapper method should let the exception bubble up
            assertThrows(SecurityException.class, () -> ReflectionUtils.getDeepDeclaredFieldMap(RestrictedClass.class), "SecurityException should propagate from the underlying call");
        }
    }
}
