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
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getDeclaredFields_Test {

    static class SuperClass {

        protected int superField;
    }

    /**
     * Simple class with two fields – used for caching tests.
     */
    static class SimpleClass {

        int a;

        String b;
    }

    /**
     * Base class for inheritance test.
     */
    static class BaseClass {

        protected long baseField;
    }

    /**
     * Subclass that extends {@link BaseClass}.
     */
    static class SubClass extends BaseClass {

        private double subField;
    }

    /**
     * Class that declares fields with all Java visibility modifiers.
     */
    static class ModifierClass {

        public int publicField;

        protected int protectedField;

        // package‑private
        int packageField;

        private int privateField;
    }

    /**
     * Interface with static final constants – they are implicitly public static final.
     */
    interface SampleInterface {

        String CONST_ONE = "ONE";

        int CONST_TWO = 2;
    }

    /**
     * Two unrelated classes for cache‑isolation test.
     */
    static class FirstClass {

        int first;
    }

    static class SecondClass {

        int second;
    }

    /**
     * Empty class used for the SecurityManager test.
     */
    static class SecureClass {
        // no fields
    }

    /* -----------------------------------------------------------
     *  Additional simple helper class used by earlier tests
     * ----------------------------------------------------------- */
    static class NoFieldClass {
        // intentionally empty
    }

    static Stream<Arguments> noDeclaredFieldsProvider() {
        return Stream.of(Arguments.of(Object.class, "java.lang.Object"), Arguments.of(int.class, "primitive int"), Arguments.of(String[].class, "String array"));
    }

    @Test
    void getDeclaredFields_returnsOnlyFieldsDeclaredInClass() {
        // GIVEN
        Class<?> target = SubClass.class;
        // WHEN
        List<Field> fields = ReflectionUtils.getDeclaredFields(target);
        // THEN
        Set<String> fieldNames = fields.stream().map(Field::getName).collect(Collectors.toSet());
        // SubClass declares three fields (a, b, C).  The inherited
        // field 'superField' must NOT be present.
        assertEquals(3, fields.size(), "Expected exactly the fields declared in SubClass");
        assertTrue(fieldNames.containsAll(Set.of("a", "b", "C")), "Missing expected declared fields");
        assertFalse(fieldNames.contains("superField"), "Inherited fields must not be returned");
    }

    @Test
    void getDeclaredFields_returnsEmptyListForClassWithoutFields() {
        // GIVEN
        Class<?> target = NoFieldClass.class;
        // WHEN
        List<Field> fields = ReflectionUtils.getDeclaredFields(target);
        // THEN
        assertNotNull(fields, "Result should never be null");
        assertTrue(fields.isEmpty(), "Class with no declared fields should return an empty list");
    }

    @Test
    void getDeclaredFields_cachesResultForSubsequentCalls() {
        // GIVEN
        Class<?> target = SimpleClass.class;
        // WHEN – first call populates the cache
        List<Field> firstCall = ReflectionUtils.getDeclaredFields(target);
        // WHEN – second call should retrieve the cached list
        List<Field> secondCall = ReflectionUtils.getDeclaredFields(target);
        // THEN
        assertSame(firstCall, secondCall, "Subsequent calls for the same class must return the cached list instance");
    }

    @Test
    void getDeclaredFields_nullClass_throwsNullPointerException() {
        // GIVEN
        Class<?> target = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getDeclaredFields(target), "Passing a null class should result in NPE");
    }

    @ParameterizedTest(name = "[{index}] No declared fields for {1}")
    @MethodSource("noDeclaredFieldsProvider")
    void getDeclaredFields_noDeclaredFields_returnsEmptyList(Class<?> clazz, String description) {
        // WHEN
        List<Field> fields = ReflectionUtils.getDeclaredFields(clazz);
        // THEN
        assertNotNull(fields, "Result must never be null");
        assertTrue(fields.isEmpty(), "Expected empty list for " + description);
    }

    @Test
    void getDeclaredFields_modifierVariations_allIncluded() {
        // GIVEN
        Class<?> target = ModifierClass.class;
        // WHEN
        List<Field> fields = ReflectionUtils.getDeclaredFields(target);
        Set<String> names = fields.stream().map(Field::getName).collect(Collectors.toSet());
        // THEN
        assertEquals(4, fields.size(), "All four modifiers should be present");
        assertTrue(names.containsAll(Set.of("publicField", "protectedField", "packageField", "privateField")), "Missing one or more fields with different modifiers");
    }

    @Test
    void getDeclaredFields_interfaceStaticFinalFields_areReturned() {
        // GIVEN
        Class<?> target = SampleInterface.class;
        // WHEN
        List<Field> fields = ReflectionUtils.getDeclaredFields(target);
        Set<String> names = fields.stream().map(Field::getName).collect(Collectors.toSet());
        // THEN
        assertEquals(2, fields.size(), "Interface should expose its two constant fields");
        assertTrue(names.containsAll(Set.of("CONST_ONE", "CONST_TWO")), "Interface constants not found");
    }

    @Test
    void getDeclaredFields_cacheIsolationBetweenDifferentClasses() {
        // First call for FirstClass – populates cache with L1
        List<Field> l1 = ReflectionUtils.getDeclaredFields(FirstClass.class);
        assertNotNull(l1, "FirstClass list must not be null");
        // Call for SecondClass – should produce a different list L2
        List<Field> l2 = ReflectionUtils.getDeclaredFields(SecondClass.class);
        assertNotNull(l2, "SecondClass list must not be null");
        assertNotSame(l1, l2, "Different classes must have distinct cached lists");
        // Second call for FirstClass – must return the original L1 instance
        List<Field> l1Again = ReflectionUtils.getDeclaredFields(FirstClass.class);
        assertSame(l1, l1Again, "Cache must return the original list for FirstClass");
    }

    @Test
    void getDeclaredFields_securityManagerDeniesAccess_throwsSecurityException() {
        // Preserve the original security manager so we can restore it afterwards.
        SecurityManager original = System.getSecurityManager();
        // Install a restrictive security manager that denies reflective access.
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything else.
            }

            @Override
            public void checkMemberAccess(Class<?> clazz, int which) {
                // Deny any reflective member access.
                throw new SecurityException("Reflective access denied by test SecurityManager");
            }
        });
        try {
            // The call should propagate the SecurityException thrown by the manager.
            assertThrows(SecurityException.class, () -> ReflectionUtils.getDeclaredFields(SecureClass.class), "Expected SecurityException when SecurityManager blocks reflective access");
        } finally {
            // Restore the original security manager to avoid side effects on other tests.
            System.setSecurityManager(original);
        }
    }
}
