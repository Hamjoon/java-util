package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getDeepDeclaredFieldMap_Test {

    // Parent class with a private field named "parentPrivate"
    static class ParentWithDuplicateField {

        private int parentPrivate;
    }

    // Child class that also declares a private field with the same name
    static class ChildWithDuplicateField extends ParentWithDuplicateField {

        // duplicate name
        private int parentPrivate;

        protected double childOnly;
    }

    // Concrete subclass adding its own field
    static class ConcreteExtendingAbstract extends AbstractBase {

        public boolean concreteField;
    }

    // Simple class with distinct fields (used by the starter test)
    static class SimpleClass {

        public int publicInt;

        private String privateString;
    }

    // Empty class – no declared fields
    static class EmptyClass {
    }

    // Class with a single public field
    static class SinglePublicFieldClass {

        public int publicField;
    }

    // Parent with a private field named "value"
    static class ParentDuplicateValue {

        private int value;
    }

    // Child that also declares a private field named "value"
    static class ChildDuplicateValue extends ParentDuplicateValue {

        // duplicate name
        private int value;
    }

    // Visibility modifiers
    static class VisibilityModifiersClass {

        public int publicField;

        protected long protectedField;

        // default (package‑private) visibility
        int packagePrivateField;
    }

    // Static vs. instance fields
    static class StaticAndInstanceFieldsClass {

        static String staticField;

        double instanceField;
    }

    // Three‑level hierarchy without name collisions
    static class GrandParent {

        private int grandParentField;
    }

    static class Parent extends GrandParent {

        protected String parentField;
    }

    static class ChildNoCollision extends Parent {

        public boolean childField;
    }

    // Multi‑level duplicate field name hierarchy
    static class BaseDuplicate {

        private int value;
    }

    static class MidDuplicate extends BaseDuplicate {

        // duplicate
        private int value;
    }

    static class LeafWithTripleDuplicate extends MidDuplicate {

        // duplicate
        private int value;
    }

    // Superclass with a public field
    static class SuperClassWithPublicField {

        public int shared;
    }

    // Subclass that does NOT redeclare the field
    static class SubClassWithoutOverride extends SuperClassWithPublicField {
    }

    // Parent with a field, child declares none
    static class NonEmptyParentClass {

        private String parentField;
    }

    static class EmptyChildOfNonEmptyParent extends NonEmptyParentClass {
    }

    // Abstract hierarchy used in the starter test
    static abstract class AbstractBase {

        protected long abstractField;
    }

    /**
     * Simple class with two public fields. The map should contain both
     * fields keyed by their simple names.
     */
    @Test
    void shouldReturnAllDeclaredFieldsFromSingleClass() {
        // given
        Class<?> clazz = SimpleClass.class;
        // when
        Map<String, Field> result = ReflectionUtils.getDeepDeclaredFieldMap(clazz);
        // then
        assertEquals(2, result.size(), "Expected two fields");
        assertTrue(result.containsKey("publicInt"));
        assertTrue(result.containsKey("privateString"));
        Field publicInt = result.get("publicInt");
        assertEquals(int.class, publicInt.getType());
        Field privateString = result.get("privateString");
        assertEquals(String.class, privateString.getType());
    }

    /**
     * Subclass that inherits a private field with the same name as its
     * superclass. The map must disambiguate the duplicate name by using
     * the fully‑qualified key (declaringClassName.fieldName) for the second
     * occurrence.
     */
    @Test
    void shouldDisambiguateDuplicatePrivateFieldNamesInHierarchy() {
        // given
        Class<?> clazz = ChildWithDuplicateField.class;
        // when
        Map<String, Field> result = ReflectionUtils.getDeepDeclaredFieldMap(clazz);
        // then
        // three fields total: one from parent, one from child (same name), and one unique child field
        assertEquals(3, result.size());
        // parent private field appears with its simple name
        assertTrue(result.containsKey("parentPrivate"));
        Field parentField = result.get("parentPrivate");
        assertEquals(ParentWithDuplicateField.class, parentField.getDeclaringClass());
        // child duplicate field is stored with qualified key
        String qualifiedKey = ChildWithDuplicateField.class.getName() + ".parentPrivate";
        assertTrue(result.containsKey(qualifiedKey));
        Field childDuplicate = result.get(qualifiedKey);
        assertEquals(ChildWithDuplicateField.class, childDuplicate.getDeclaringClass());
        // unique child field
        assertTrue(result.containsKey("childOnly"));
        Field childOnly = result.get("childOnly");
        assertEquals(ChildWithDuplicateField.class, childOnly.getDeclaringClass());
    }

    /**
     * Interface hierarchy does not contribute fields, but abstract classes do.
     * Verify that only concrete class fields (including those from abstract
     * super‑classes) are present.
     */
    @Test
    void shouldIncludeFieldsFromAbstractSuperClass() {
        // given
        Class<?> clazz = ConcreteExtendingAbstract.class;
        // when
        Map<String, Field> result = ReflectionUtils.getDeepDeclaredFieldMap(clazz);
        // then
        assertEquals(2, result.size());
        assertTrue(result.containsKey("abstractField"));
        assertTrue(result.containsKey("concreteField"));
    }

    /**
     * Empty class – expect an empty map.
     */
    @Test
    void shouldReturnEmptyMapForClassWithNoDeclaredFields() {
        Map<String, Field> map = ReflectionUtils.getDeepDeclaredFieldMap(EmptyClass.class);
        assertTrue(map.isEmpty(), "Map should be empty for a class without fields");
    }

    /**
     * Single public field – map contains exactly one entry with the plain name.
     */
    @Test
    void shouldReturnMapWithSinglePublicField() {
        Map<String, Field> map = ReflectionUtils.getDeepDeclaredFieldMap(SinglePublicFieldClass.class);
        assertEquals(1, map.size(), "Expected exactly one field");
        assertTrue(map.containsKey("publicField"));
        Field f = map.get("publicField");
        assertEquals(SinglePublicFieldClass.class, f.getDeclaringClass());
        assertEquals(int.class, f.getType());
    }

    /**
     * Duplicate private field name between parent and child – qualified key for child.
     */
    @Test
    void shouldQualifyDuplicateFieldNamesBetweenParentAndChild() {
        Map<String, Field> map = ReflectionUtils.getDeepDeclaredFieldMap(ChildDuplicateValue.class);
        // two entries: child “value” (plain) and parent “value” (qualified)
        assertEquals(2, map.size());
        // child field
        assertTrue(map.containsKey("value"));
        Field childField = map.get("value");
        assertEquals(ChildDuplicateValue.class, childField.getDeclaringClass());
        // qualified parent field
        String qualifiedKey = ParentDuplicateValue.class.getName() + ".value";
        assertTrue(map.containsKey(qualifiedKey));
        Field parentField = map.get(qualifiedKey);
        assertEquals(ParentDuplicateValue.class, parentField.getDeclaringClass());
    }

    /**
     * Null argument must throw NullPointerException.
     */
    @Test
    void shouldThrowNPEWhenClassIsNull() {
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getDeepDeclaredFieldMap(null), "Passing null should result in NPE");
    }

    /**
     * Visibility modifiers – public, protected, package‑private.
     */
    @Test
    void shouldCollectAllVisibilityModifiers() {
        Map<String, Field> map = ReflectionUtils.getDeepDeclaredFieldMap(VisibilityModifiersClass.class);
        assertEquals(3, map.size());
        assertTrue(map.containsKey("publicField"));
        assertTrue(map.containsKey("protectedField"));
        assertTrue(map.containsKey("packagePrivateField"));
        assertEquals(VisibilityModifiersClass.class, map.get("publicField").getDeclaringClass());
        assertEquals(VisibilityModifiersClass.class, map.get("protectedField").getDeclaringClass());
        assertEquals(VisibilityModifiersClass.class, map.get("packagePrivateField").getDeclaringClass());
    }

    /**
     * Static and instance fields are both present.
     */
    @Test
    void shouldIncludeStaticAndInstanceFields() {
        Map<String, Field> map = ReflectionUtils.getDeepDeclaredFieldMap(StaticAndInstanceFieldsClass.class);
        assertEquals(2, map.size());
        assertTrue(map.containsKey("staticField"));
        assertTrue(map.containsKey("instanceField"));
        Field staticF = map.get("staticField");
        assertTrue(Modifier.isStatic(staticF.getModifiers()));
        Field instanceF = map.get("instanceField");
        assertFalse(Modifier.isStatic(instanceF.getModifiers()));
    }

    /**
     * Multi‑level inheritance without name collisions – all fields are present with simple keys.
     */
    @Test
    void shouldCollectFieldsFromThreeLevelHierarchyWithoutCollisions() {
        Map<String, Field> map = ReflectionUtils.getDeepDeclaredFieldMap(ChildNoCollision.class);
        assertEquals(3, map.size());
        assertTrue(map.containsKey("grandParentField"));
        assertTrue(map.containsKey("parentField"));
        assertTrue(map.containsKey("childField"));
        assertEquals(GrandParent.class, map.get("grandParentField").getDeclaringClass());
        assertEquals(Parent.class, map.get("parentField").getDeclaringClass());
        assertEquals(ChildNoCollision.class, map.get("childField").getDeclaringClass());
    }

    /**
     * Multi‑level inheritance with duplicate field name “value” at each level – qualified keys for super‑classes.
     */
    @Test
    void shouldQualifyDuplicateFieldNamesAcrossThreeLevels() {
        Map<String, Field> map = ReflectionUtils.getDeepDeclaredFieldMap(LeafWithTripleDuplicate.class);
        assertEquals(3, map.size());
        // leaf field (plain name)
        assertTrue(map.containsKey("value"));
        Field leaf = map.get("value");
        assertEquals(LeafWithTripleDuplicate.class, leaf.getDeclaringClass());
        // middle class field (qualified)
        String midKey = MidDuplicate.class.getName() + ".value";
        assertTrue(map.containsKey(midKey));
        Field mid = map.get(midKey);
        assertEquals(MidDuplicate.class, mid.getDeclaringClass());
        // base class field (qualified)
        String baseKey = BaseDuplicate.class.getName() + ".value";
        assertTrue(map.containsKey(baseKey));
        Field base = map.get(baseKey);
        assertEquals(BaseDuplicate.class, base.getDeclaringClass());
    }

    /**
     * Inherited public field not overridden – map contains the field from the super‑class.
     */
    @Test
    void shouldIncludeInheritedPublicFieldWhenNotOverridden() {
        Map<String, Field> map = ReflectionUtils.getDeepDeclaredFieldMap(SubClassWithoutOverride.class);
        assertEquals(1, map.size());
        assertTrue(map.containsKey("shared"));
        Field f = map.get("shared");
        assertEquals(SuperClassWithPublicField.class, f.getDeclaringClass());
    }

    /**
     * Child class declares no fields but its parent does – map contains only the parent field.
     */
    @Test
    void shouldReturnParentFieldWhenChildHasNoOwnFields() {
        Map<String, Field> map = ReflectionUtils.getDeepDeclaredFieldMap(EmptyChildOfNonEmptyParent.class);
        assertEquals(1, map.size());
        assertTrue(map.containsKey("parentField"));
        Field f = map.get("parentField");
        assertEquals(NonEmptyParentClass.class, f.getDeclaringClass());
    }
}
