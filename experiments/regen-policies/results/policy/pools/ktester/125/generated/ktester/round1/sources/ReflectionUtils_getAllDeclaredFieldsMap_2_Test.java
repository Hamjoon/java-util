package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getAllDeclaredFieldsMap_2_Test {

    static class SubClass extends SuperClass {

        private String subField;

        // should be filtered out
        public static long staticField;
    }

    class WithStaticSuper extends SuperClass {

        public static String staticSuperField;

        private double instanceInSub;
    }

    /* ---------------------------------------------------------------------- */
    static class SuperClass {

        private int superField;

        // should be filtered out
        public static int staticSuper;
    }

    /* ---------------------------------------------------------------------- */
    static class SimpleClass {

        // static – filtered
        public static int staticField;

        // instance – kept
        private String instanceField;
    }

    static class EmptyClass {
        // No fields at all
    }

    static class OnlyStaticFields {

        public static final String STATIC_ONE = "ONE";

        public static final String STATIC_TWO = "TWO";
    }

    static class NonEnumWithHashField {

        // not filtered because the class is not an enum
        private int hash;
    }

    // Super‑class / sub‑class pair used to verify field shadowing.
    static class SuperShadow {

        // will be shadowed
        private int dup;
    }

    static class SubShadow extends SuperShadow {

        // shadows the field from SuperShadow
        private int dup;
    }

    private static Stream<Arguments> standardCases() {
        return Stream.of(Arguments.of(SimpleClass.class, List.of("instanceField"), List.of("staticField")), Arguments.of(SubClass.class, List.of("subField", "superField"), List.of("staticSuper", "staticField")));
    }

    private static Stream<Arguments> edgeCases() {
        return Stream.of(Arguments.of(EmptyClass.class, Collections.emptyList(), Collections.emptyList()), Arguments.of(OnlyStaticFields.class, Collections.emptyList(), List.of("STATIC_ONE", "STATIC_TWO")), Arguments.of(NonEnumWithHashField.class, List.of("hash"), Collections.emptyList()));
    }

    @Test
    void shouldCollectAllInstanceFieldsFromClassHierarchy() {
        Map<String, Field> fieldMap = ReflectionUtils.getAllDeclaredFieldsMap(SubClass.class);
        // Expect exactly the two instance fields (superField and subField)
        assertEquals(2, fieldMap.size(), "Map should contain only the non‑static instance fields from the hierarchy");
        assertTrue(fieldMap.containsKey("superField"), "Superclass field must be present");
        assertTrue(fieldMap.containsKey("subField"), "Subclass field must be present");
        // Verify that the static field is not present
        assertFalse(fieldMap.containsKey("staticField"), "Static fields must be filtered out");
    }

    @Test
    void shouldFilterOutStaticFieldsEvenWhenDeclaredInSuperclass() {
        class WithStaticSuper extends SuperClass {

            public static String staticSuperField;

            private double instanceInSub;
        }
        Map<String, Field> fieldMap = ReflectionUtils.getAllDeclaredFieldsMap(WithStaticSuper.class);
        // superField (from SuperClass) + instanceInSub = 2 fields
        assertEquals(2, fieldMap.size(), "Only non‑static fields should be returned");
        assertTrue(fieldMap.containsKey("superField"));
        assertTrue(fieldMap.containsKey("instanceInSub"));
        assertFalse(fieldMap.containsKey("staticSuperField"));
    }

    @Test
    void enumShouldExcludeSyntheticEnumFieldsButIncludeUserFields() {
        Map<String, Field> fieldMap = ReflectionUtils.getAllDeclaredFieldsMap(SampleEnum.class);
        // enumField and customValue are the only user‑declared instance fields; synthetic fields are filtered
        assertTrue(fieldMap.containsKey("enumField"));
        assertTrue(fieldMap.containsKey("customValue"));
        // Ensure internal synthetic fields are not present
        assertFalse(fieldMap.containsKey("ordinal"));
        assertFalse(fieldMap.containsKey("hash"));
        assertFalse(fieldMap.containsKey("internal"));
        assertFalse(fieldMap.containsKey("ENUM$VALUES"));
    }

    /* ---------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("standardCases")
    void standardCases(Class<?> clazz, List<String> expectedKeys, List<String> unexpectedKeys) {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(clazz);
        expectedKeys.forEach(k -> assertTrue(map.containsKey(k), () -> "Expected key '" + k + "' to be present for class " + clazz.getSimpleName()));
        unexpectedKeys.forEach(k -> assertFalse(map.containsKey(k), () -> "Unexpected key '" + k + "' found for class " + clazz.getSimpleName()));
    }

    /* ---------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] Edge case: {0}")
    @MethodSource("edgeCases")
    void edgeCases(Class<?> clazz, List<String> expectedKeys, List<String> unexpectedKeys) {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(clazz);
        assertEquals(expectedKeys.size(), map.size(), "Map size should match expected number of kept fields");
        expectedKeys.forEach(k -> assertTrue(map.containsKey(k), () -> "Expected key '" + k + "' to be present"));
        unexpectedKeys.forEach(k -> assertFalse(map.containsKey(k), () -> "Unexpected key '" + k + "' found"));
    }

    /* ---------------------------------------------------------------------- */
    @Test
    void enumHandlingCustomFieldIsKeptAndSyntheticAreRemoved() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(SampleEnum.class);
        // Custom field must be present
        assertTrue(map.containsKey("customValue"), "User‑defined enum field 'customValue' should be present");
        // Synthetic/enum‑base fields must be absent
        assertFalse(map.containsKey("internal"));
        assertFalse(map.containsKey("ENUM$VALUES"));
        assertFalse(map.containsKey("hash"));
        assertFalse(map.containsKey("ordinal"));
    }

    /* ---------------------------------------------------------------------- */
    @Test
    void nullClassArgumentThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getAllDeclaredFieldsMap(null), "Passing a null Class should result in IllegalArgumentException");
    }

    /* ---------------------------------------------------------------------- */
    @Test
    void fieldShadowingKeepsSubclassDeclaration() {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(SubShadow.class);
        assertEquals(1, map.size(), "Only one entry for the shadowed field name should exist");
        assertTrue(map.containsKey("dup"), "Shadowed field name must be present");
        Field dupField = map.get("dup");
        assertEquals(SubShadow.class, dupField.getDeclaringClass(), "The Field object should be the one declared in SubShadow");
    }

    /* ---------------------------------------------------------------------- */
    @Test
    void nullPredicateArgumentThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getAllDeclaredFieldsMap(SimpleClass.class, null), "Providing a null Predicate should throw NullPointerException");
    }
}
