package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.function.Predicate;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getAllDeclaredFieldsMap_Test {

    static class SuperClass {

        private int dup;

        protected String superOnly;
    }

    /**
     * Used for basic (non‑inheritance) tests.
     */
    static class SimpleClass {

        public int a;

        private String b;
    }

    /**
     * Super‑class for duplicate‑field tests.
     */
    static class BaseClass {

        // hidden in subclass
        private int x;

        protected String baseOnly;
    }

    /**
     * Sub‑class that hides {@code BaseClass.x}.
     */
    static class SubClass extends BaseClass {

        // hides BaseClass.x
        private int x;

        public double z;
    }

    /**
     * Class with no declared fields.
     */
    static class EmptyClass {
        // intentionally empty
    }

    /**
     * Class containing both static and instance fields.
     */
    static class StaticFieldsClass {

        static int s;

        int i;
    }

    /**
     * Class used to verify iteration order of the returned LinkedHashMap.
     */
    static class OrderedClass {

        int first;

        int second;

        int third;
    }

    /**
     * Simple holder class for parameterized test data.
     */
    private static class ArgumentsHolder {

        final String name;

        final Predicate<Field> predicate;

        final List<String> expectedFieldNames;

        ArgumentsHolder(String name, Predicate<Field> predicate, List<String> expectedFieldNames) {
            this.name = name;
            this.predicate = predicate;
            this.expectedFieldNames = expectedFieldNames;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static Stream<ArgumentsHolder> predicateProvider() {
        return Stream.of(new ArgumentsHolder("publicOnly", (Predicate<Field>) f -> Modifier.isPublic(f.getModifiers()), // only 'a' is public in SimpleClass
        List.of("a")), new ArgumentsHolder("nonStatic", (Predicate<Field>) f -> !Modifier.isStatic(f.getModifiers()), // both fields in SimpleClass are non‑static
        List.of("a", "b")));
    }

    @Test
    void shouldThrowWhenClassIsNull() {
        Predicate<Field> any = f -> true;
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getAllDeclaredFieldsMap(null, any));
        assertTrue(ex.getMessage().contains("class cannot be null"));
    }

    @Test
    void shouldThrowWhenPredicateIsNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getAllDeclaredFieldsMap(SimpleClass.class, null));
        assertTrue(ex.getMessage().contains("fieldFilter cannot be null"));
    }

    @Test
    void shouldReturnAllFieldsWhenPredicateAlwaysTrue() {
        Predicate<Field> any = f -> true;
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(SimpleClass.class, any);
        assertEquals(2, map.size(), "SimpleClass has exactly two declared fields");
        assertTrue(map.containsKey("publicField"), "Map should contain the public field");
        assertTrue(map.containsKey("privateField"), "Map should contain the private field");
        // Verify the actual Field objects are the ones declared in SimpleClass
        assertEquals(SimpleClass.class, map.get("publicField").getDeclaringClass());
        assertEquals(SimpleClass.class, map.get("privateField").getDeclaringClass());
    }

    @Test
    void shouldFilterFieldsAccordingToPredicate() {
        // Keep only public fields
        Predicate<Field> publicOnly = f -> Modifier.isPublic(f.getModifiers());
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(SimpleClass.class, publicOnly);
        assertEquals(1, map.size(), "Only the public field should survive the filter");
        assertTrue(map.containsKey("publicField"));
        assertFalse(map.containsKey("privateField"));
    }

    @Test
    void shouldHandleDuplicateFieldNamesInHierarchy() {
        // Predicate that accepts everything
        Predicate<Field> any = f -> true;
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(SubClass.class, any);
        // Expected keys:
        // - "dup"               -> the field from SuperClass (first occurrence)
        // - "com.cedarsoftware.util.ReflectionUtils_getAllDeclaredFieldsMap_Test$SubClass.dup"
        //                        -> the field from SubClass (duplicate name)
        // - "superOnly"
        // - "subOnly"
        assertEquals(4, map.size(), "SubClass ReflectionUtils_getAllDeclaredFieldsMap_Test should expose four distinct entries");
        // The simple name refers to the first encountered field with that name (SuperClass)
        assertTrue(map.containsKey("dup"));
        // The qualified name must refer to the SubClass field
        String qualifiedDup = SubClass.class.getName() + ".dup";
        assertTrue(map.containsKey(qualifiedDup));
        // Verify that the Field objects belong to the right declaring class
        assertEquals(SuperClass.class, map.get("dup").getDeclaringClass(), "Simple key 'dup' should map to the field declared in SuperClass");
        assertEquals(SubClass.class, map.get(qualifiedDup).getDeclaringClass(), "Qualified key should map to the field declared in SubClass");
    }

    @Test
    void shouldReturnEmptyMapWhenPredicateFiltersAll() {
        Predicate<Field> none = f -> false;
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(SimpleClass.class, none);
        assertTrue(map.isEmpty(), "When predicate rejects all fields, the result map must be empty");
    }

    @Test
    void shouldFilterFieldsByType() {
        // Keep only int‑typed fields
        Predicate<Field> intOnly = f -> f.getType() == int.class;
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(SimpleClass.class, intOnly);
        assertEquals(1, map.size());
        assertTrue(map.containsKey("a"));
        assertFalse(map.containsKey("b"));
    }

    @Test
    void shouldFilterDuplicateFieldsByName() {
        // Accept only fields named "x"
        Predicate<Field> nameIsX = f -> "x".equals(f.getName());
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(SubClass.class, nameIsX);
        assertEquals(2, map.size());
        assertTrue(map.containsKey("x"));
        String qualifiedX = BaseClass.class.getName() + ".x";
        assertTrue(map.containsKey(qualifiedX));
    }

    @Test
    void shouldReturnEmptyMapForClassWithoutFields() {
        Predicate<Field> any = f -> true;
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(EmptyClass.class, any);
        assertTrue(map.isEmpty(), "EmptyClass declares no fields, so the result must be empty");
    }

    @Test
    void shouldIncludeStaticFieldsWhenPredicateAllowsAll() {
        Predicate<Field> any = f -> true;
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(StaticFieldsClass.class, any);
        assertEquals(2, map.size());
        assertTrue(map.containsKey("s"));
        assertTrue(map.containsKey("i"));
        assertTrue(Modifier.isStatic(map.get("s").getModifiers()));
        assertFalse(Modifier.isStatic(map.get("i").getModifiers()));
    }

    @Test
    void shouldExcludeStaticFieldsWhenPredicateFiltersThemOut() {
        Predicate<Field> nonStatic = f -> !Modifier.isStatic(f.getModifiers());
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(StaticFieldsClass.class, nonStatic);
        assertEquals(1, map.size());
        assertTrue(map.containsKey("i"));
        assertFalse(map.containsKey("s"));
    }

    @Test
    void shouldPreserveDeclarationOrder() {
        Predicate<Field> any = f -> true;
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(OrderedClass.class, any);
        List<String> expectedOrder = List.of("first", "second", "third");
        List<String> actualOrder = new ArrayList<>(map.keySet());
        assertEquals(expectedOrder, actualOrder, "LinkedHashMap must preserve the order fields are declared in the class");
    }

    @Test
    void shouldPropagateIllegalArgumentExceptionFromPredicate() {
        Predicate<Field> badPredicate = f -> {
            if ("a".equals(f.getName())) {
                throw new IllegalArgumentException("bad field");
            }
            return true;
        };
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getAllDeclaredFieldsMap(SimpleClass.class, badPredicate));
        assertEquals("bad field", ex.getMessage());
    }

    @Test
    void shouldPropagateRuntimeExceptionFromPredicate() {
        Predicate<Field> runtimePredicate = f -> {
            throw new RuntimeException("unexpected");
        };
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ReflectionUtils.getAllDeclaredFieldsMap(SimpleClass.class, runtimePredicate));
        assertEquals("unexpected", ex.getMessage());
    }

    @ParameterizedTest(name = "Predicate ''{0}'' should keep fields {2}")
    @MethodSource("predicateProvider")
    void shouldApplyVariousPredicatesCorrectly(ArgumentsHolder holder) {
        Map<String, Field> map = ReflectionUtils.getAllDeclaredFieldsMap(SimpleClass.class, holder.predicate);
        assertEquals(holder.expectedFieldNames.size(), map.size(), "Unexpected number of fields for predicate '" + holder.name + "'");
        holder.expectedFieldNames.forEach(name -> assertTrue(map.containsKey(name), "Expected field '" + name + "' to be present"));
    }
}
