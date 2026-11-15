package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.function.Predicate;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getAllDeclaredFieldsMap_Test {

    /**
     * Simple class with two public fields
     */
    static class SimpleClass {

        public int fieldA;

        public String fieldB;
    }

    /**
     * Parent class containing a private field named "dup"
     */
    static class ParentDup {

        // duplicate name
        private int dup;

        public long parentOnly;
    }

    /**
     * Child class that also declares a private field named "dup"
     */
    static class ChildDup extends ParentDup {

        // duplicate name, should be qualified
        private int dup;

        protected double childOnly;
    }

    class WithStatic {

        public int instanceField;

        public static int staticField;
    }

    /**
     * Parent/Child pair used for duplicate‑field tests (different naming)
     */
    static class ParentChild {

        // shadowed by child
        private int privateField;

        public int parentOnly;
    }

    static class ChildOnly extends ParentChild {

        // shadows parent’s field
        private int privateField;

        public int childOnly;
    }

    /**
     * Class with no inheritance – used to verify simple mapping
     */
    static class StandAlone {

        public int a;

        protected String b;

        private double c;
    }

    @Test
    void testNullClassThrowsException() {
        Predicate<Field> anyField = f -> true;
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getAllDeclaredFieldsMap(null, anyField));
        assertTrue(ex.getMessage().contains("class cannot be null"));
    }

    @Test
    void testNullPredicateThrowsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getAllDeclaredFieldsMap(SimpleClass.class, null));
        assertTrue(ex.getMessage().contains("fieldFilter cannot be null"));
    }

    @Test
    void testSimpleClassFields() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(SimpleClass.class, f -> true);
        assertEquals(2, map.size(), "SimpleClass should expose exactly 2 fields");
        assertTrue(map.containsKey("fieldA"));
        assertTrue(map.containsKey("fieldB"));
        assertEquals(int.class, map.get("fieldA").getType());
        assertEquals(String.class, map.get("fieldB").getType());
    }

    @Test
    void testDuplicateFieldNamesAreQualified() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(ChildDup.class, f -> true);
        /*
         Expected fields:
           - ParentDup.dup   -> qualified as "dup" (simple name belongs to parent because child shadows it)
           - ChildDup.dup    -> qualified as "ChildDup.dup"
           - parentOnly
           - childOnly
         */
        assertEquals(4, map.size(), "ChildDup should expose 4 distinct entries");
        // Simple key refers to the field from the parent class (the first occurrence)
        assertTrue(map.containsKey("dup"));
        Field parentDupField = map.get("dup");
        assertEquals(ParentDup.class, parentDupField.getDeclaringClass(), "Unqualified key should map to the field from the parent class");
        // Qualified key refers to the child’s duplicate field
        String qualifiedKey = ChildDup.class.getName() + ".dup";
        assertTrue(map.containsKey(qualifiedKey));
        Field childDupField = map.get(qualifiedKey);
        assertEquals(ChildDup.class, childDupField.getDeclaringClass(), "Qualified key should map to the field from the child class");
        // The other fields should be present with their simple names
        assertTrue(map.containsKey("parentOnly"));
        assertEquals(ParentDup.class, map.get("parentOnly").getDeclaringClass());
        assertTrue(map.containsKey("childOnly"));
        assertEquals(ChildDup.class, map.get("childOnly").getDeclaringClass());
    }

    @Test
    void testFieldFilterExcludesStaticFields() {
        // Create a class with a static field to verify the filter works
        class WithStatic {

            public int instanceField;

            public static int staticField;
        }
        // Predicate that excludes static fields
        Predicate<Field> nonStatic = f -> !Modifier.isStatic(f.getModifiers());
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(WithStatic.class, nonStatic);
        assertEquals(1, map.size(), "Only the non‑static field should be present");
        assertTrue(map.containsKey("instanceField"));
        assertFalse(map.containsKey("staticField"));
    }

    @Test
    void testPredicateThrowsIllegalArgumentException() {
        Predicate<Field> throwing = f -> {
            throw new IllegalArgumentException("predicate failure");
        };
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getAllDeclaredFieldsMap(SimpleClass.class, throwing));
        assertEquals("predicate failure", ex.getMessage());
    }

    @Test
    void testPredicateThrowsNullPointerException() {
        Predicate<Field> throwing = f -> {
            throw new NullPointerException("null in predicate");
        };
        NullPointerException ex = assertThrows(NullPointerException.class, () -> ReflectionUtils.getAllDeclaredFieldsMap(SimpleClass.class, throwing));
        assertEquals("null in predicate", ex.getMessage());
    }

    @Test
    void testPredicateReturnsNullCausesNPE() {
        // Returning null from a Predicate leads to NPE when the result is unboxed to boolean
        Predicate<Field> nullReturning = f -> null;
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getAllDeclaredFieldsMap(SimpleClass.class, nullReturning));
    }

    @Test
    void testParentChildDuplicateHandling() {
        /*
         * ParentChild defines a private field "privateField" that is also declared
         * in ChildOnly. The map should contain:
         *   - "privateField" -> ParentChild.privateField (simple name belongs to parent)
         *   - "ChildOnly.privateField" -> ChildOnly.privateField (qualified)
         *   - "parentOnly"
         *   - "childOnly"
         */
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(ChildOnly.class, f -> true);
        assertEquals(4, map.size());
        // Simple key maps to parent's private field
        assertTrue(map.containsKey("privateField"));
        assertEquals(ParentChild.class, map.get("privateField").getDeclaringClass());
        // Qualified key maps to child's private field
        String qualified = ChildOnly.class.getName() + ".privateField";
        assertTrue(map.containsKey(qualified));
        assertEquals(ChildOnly.class, map.get(qualified).getDeclaringClass());
        // Verify other fields
        assertTrue(map.containsKey("parentOnly"));
        assertEquals(ParentChild.class, map.get("parentOnly").getDeclaringClass());
        assertTrue(map.containsKey("childOnly"));
        assertEquals(ChildOnly.class, map.get("childOnly").getDeclaringClass());
    }

    @Test
    void testStandaloneClassAllFieldsPresent() {
        // No inheritance, no name collisions – all fields should appear with simple names
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(StandAlone.class, f -> true);
        assertEquals(3, map.size());
        assertTrue(map.containsKey("a"));
        assertTrue(map.containsKey("b"));
        assertTrue(map.containsKey("c"));
        assertEquals(int.class, map.get("a").getType());
        assertEquals(String.class, map.get("b").getType());
        assertEquals(double.class, map.get("c").getType());
    }
}
