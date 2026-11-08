package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Predicate;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getDeclaredFields_Test {

    /**
     * Simple class with a mix of static and instance fields.
     */
    static class Sample {

        public int a = 1;

        private String b = "text";

        static long staticField = 42L;
    }

    @Test
    void shouldThrowWhenClassIsNull() {
        Predicate<Field> any = f -> true;
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getDeclaredFields(null, any));
        assertTrue(ex.getMessage().contains("class cannot be null"));
    }

    @Test
    void shouldThrowWhenPredicateIsNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getDeclaredFields(Sample.class, null));
        assertTrue(ex.getMessage().contains("fieldFilter cannot be null"));
    }

    @Test
    void shouldReturnAllFieldsWhenPredicateAcceptsAll() {
        Predicate<Field> acceptAll = f -> true;
        List<Field> fields = ReflectionUtils.getDeclaredFields(Sample.class, acceptAll);
        // Expect three fields: a, b and staticField
        assertEquals(3, fields.size());
        // Verify each expected field is present
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("a")));
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("b")));
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("staticField")));
        // Verify that the fields are accessible (setAccessible(true) was called)
        Field staticField = fields.stream().filter(f -> f.getName().equals("staticField")).findFirst().orElseThrow();
        assertTrue(staticField.canAccess(null) || staticField.isAccessible());
    }

    @Test
    void defaultFilterShouldExcludeStaticAndEnumSyntheticFields() {
        // Use the overloaded method that applies the DEFAULT_FIELD_FILTER internally
        List<Field> fields = ReflectionUtils.getDeclaredFields(MyEnum.class);
        // Enum constants are static, and the synthetic "$VALUES" field is also static,
        // therefore the default filter should return an empty list.
        assertTrue(fields.isEmpty(), "Default filter should exclude all static/enum synthetic fields");
    }

    @Test
    void shouldCacheResultAndReturnSameInstanceOnSubsequentCalls() {
        Predicate<Field> acceptAll = field -> true;
        // First call – populates the cache
        List<Field> firstCall = ReflectionUtils.getDeclaredFields(com.cedarsoftware.util.ReflectionUtilsTestClasses.SimpleClass.class, acceptAll);
        // Second call – should retrieve the same list instance from the cache
        List<Field> secondCall = ReflectionUtils.getDeclaredFields(com.cedarsoftware.util.ReflectionUtilsTestClasses.SimpleClass.class, acceptAll);
        assertSame(firstCall, secondCall, "Subsequent calls with identical arguments must return the same cached list");
        // Verify the list is unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> firstCall.add(null), "The cached list should be unmodifiable");
    }

    @Test
    @DisplayName("DEFAULT_FIELD_FILTER should keep only instance and enum fields in ComplexClass")
    void defaultFilterShouldKeepOnlyAllowedFieldsInComplexClass() {
        // ComplexClass is defined in the test sources (see ReflectionUtilsTestClasses)
        List<Field> fields = ReflectionUtils.getDeclaredFields(com.cedarsoftware.util.ReflectionUtilsTestClasses.ComplexClass.class, ReflectionUtils.DEFAULT_FIELD_FILTER);
        // Expected field names according to the JSON description
        assertEquals(2, fields.size(), "Exactly two fields should survive the default filter");
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("instanceField")), "instanceField must be present");
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("enumField")), "enumField must be present");
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("custom predicate that excludes field \"b\" should return only \"a\"")
    void customPredicateShouldFilterOutFieldB() {
        Predicate<Field> predicate = field -> !field.getName().equals("b");
        List<Field> fields = ReflectionUtils.getDeclaredFields(com.cedarsoftware.util.ReflectionUtilsTestClasses.TwoFields.class, predicate);
        assertEquals(1, fields.size());
        assertEquals("a", fields.get(0).getName());
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("predicate that always returns true should include static and instance fields")
    void alwaysTruePredicateShouldReturnAllDeclaredFields() {
        Predicate<Field> alwaysTrue = field -> true;
        List<Field> fields = ReflectionUtils.getDeclaredFields(com.cedarsoftware.util.ReflectionUtilsTestClasses.StaticAndInstance.class, alwaysTrue);
        // The class contains a static field named "STATIC_FIELD" and an instance field "instanceField"
        assertEquals(2, fields.size());
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("STATIC_FIELD")));
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("instanceField")));
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("predicate that always returns false should produce an empty list")
    void alwaysFalsePredicateShouldYieldEmptyList() {
        Predicate<Field> alwaysFalse = field -> false;
        List<Field> fields = ReflectionUtils.getDeclaredFields(com.cedarsoftware.util.ReflectionUtilsTestClasses.SimpleClass.class, alwaysFalse);
        assertTrue(fields.isEmpty(), "Result should be empty when predicate never matches");
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("exception thrown by predicate should propagate")
    void predicateExceptionShouldPropagate() {
        Predicate<Field> explodingPredicate = field -> {
            if (field.getName().equals("a")) {
                throw new IllegalStateException("predicate failure");
            }
            return true;
        };
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> ReflectionUtils.getDeclaredFields(com.cedarsoftware.util.ReflectionUtilsTestClasses.TwoFields.class, explodingPredicate));
        assertEquals("predicate failure", ex.getMessage());
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("fields returned by the method should be made accessible")
    void fieldsShouldBeMadeAccessible() {
        Predicate<Field> acceptAll = f -> true;
        List<Field> fields = ReflectionUtils.getDeclaredFields(Sample.class, acceptAll);
        // Verify that each field is accessible (either via canAccess or isAccessible)
        for (Field f : fields) {
            assertTrue(f.canAccess(null) || f.isAccessible(), "Field '" + f.getName() + "' should be accessible after retrieval");
        }
    }
}
