package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import java.security.Permission;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getDeclaredFields_2_Test {

    /**
     * Simple POJO with both static and instance fields.
     */
    static class SampleClass {

        // should be filtered out
        static int staticField = 10;

        // should be returned
        int instanceField = 20;

        // should be returned as well
        private String privateField = "x";
    }

    class OnlyStatic {

        static final int A = 1;

        static final String B = "b";
    }

    /**
     * Class that only defines a static field and a single public instance field.
     *  Used for the JSON‑driven “Static vs Instance fields” test where the expected
     *  result contains only the instance field name.
     */
    static class StaticInstanceClass {

        static int staticField = 1;

        int instanceField = 2;
    }

    /**
     * Base class with a field that should NOT appear in the subclass result.
     */
    static class BaseClass {

        int baseField = 100;
    }

    /**
     * Subclass that declares its own field.
     */
    static class SubClass extends BaseClass {

        int subField = 200;
    }

    /**
     * Class used to trigger a SecurityException via a custom SecurityManager.
     */
    static class RestrictedClass {

        int allowedField = 42;
    }

    /**
     * A minimal SecurityManager that throws {@link SecurityException}
     * when {@code checkMemberAccess} is invoked for a specific target class.
     */
    private static class DenyReflectionSecurityManager extends SecurityManager {

        private final Class<?> deniedClass;

        DenyReflectionSecurityManager(Class<?> deniedClass) {
            this.deniedClass = deniedClass;
        }

        @Override
        public void checkMemberAccess(Class<?> clazz, int which) {
            if (clazz == deniedClass) {
                throw new SecurityException("Access to fields of " + clazz.getName() + " is denied");
            }
        }

        // Permit everything else – required to avoid interfering with the test harness.
        @Override
        public void checkPermission(Permission perm) {
            // No‑op
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // No‑op
        }
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure any custom SecurityManager is cleared after each test
        if (System.getSecurityManager() instanceof DenyReflectionSecurityManager) {
            System.setSecurityManager(null);
        }
    }

    @Test
    void testStaticFieldsAreFilteredOut() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(SampleClass.class);
        // Only the two non‑static fields should be present
        assertEquals(2, fields.size(), "Expected only instance fields to be returned");
        boolean hasInstanceField = fields.stream().anyMatch(f -> f.getName().equals("instanceField"));
        boolean hasPrivateField = fields.stream().anyMatch(f -> f.getName().equals("privateField"));
        assertTrue(hasInstanceField, "instanceField should be present");
        assertTrue(hasPrivateField, "privateField should be present");
        // staticField must not be present
        assertFalse(fields.stream().anyMatch(f -> f.getName().equals("staticField")), "staticField must be filtered out");
    }

    @Test
    void testEnumInstanceFieldIsReturned() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(SampleEnum.class);
        // The enum has one explicit instance field (enumValue). The synthetic
        // fields generated for the constants are static and therefore filtered.
        assertEquals(1, fields.size(), "Only the explicit instance field should be returned");
        Field enumValueField = fields.get(0);
        assertEquals("enumValue", enumValueField.getName(), "The returned field should be the explicit enum instance field");
    }

    @Test
    void testEmptyResultWhenOnlyStaticFieldsExist() {
        // An inner class that only defines static fields
        class OnlyStatic {

            static final int A = 1;

            static final String B = "b";
        }
        List<Field> fields = ReflectionUtils.getDeclaredFields(OnlyStatic.class);
        assertTrue(fields.isEmpty(), "All static fields should be filtered out, resulting in an empty list");
    }

    /**
     * JSON‑driven test: “Static vs Instance fields”.
     * The class defines one static and one instance field; the static one must be filtered out.
     */
    @Test
    void testStaticVsInstanceFields_JSONScenario() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(StaticInstanceClass.class);
        // Expect exactly one field – the instance field.
        assertEquals(1, fields.size(), "Only the instance field should survive the default filter");
        assertEquals("instanceField", fields.get(0).getName(), "The remaining field must be the declared instance field");
    }

    /**
     * JSON‑driven test: “Superclass fields excluded”.
     * Only fields declared directly in the supplied class should be returned.
     */
    @Test
    void testSuperclassFieldsExcluded_JSONScenario() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(SubClass.class);
        // Only the field declared in SubClass should appear.
        assertEquals(1, fields.size(), "Declared fields from super‑classes must be excluded");
        assertEquals("subField", fields.get(0).getName(), "The returned field must be the one declared in SubClass");
    }

    /**
     * JSON‑driven test: “Null class argument”.
     * The contract states an {@link IllegalArgumentException} is thrown for a null argument.
     */
    @Test
    void testNullClassArgument_JSONScenario() {
        assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getDeclaredFields(null), "Passing null should result in IllegalArgumentException");
    }

    /**
     * JSON‑driven test: “Security manager restriction”.
     * A custom {@link SecurityManager} denies reflective access to {@link RestrictedClass},
     * causing {@link SecurityException} to be propagated.
     */
    @Test
    void testSecurityManagerRestriction_JSONScenario() {
        // Install a SecurityManager that throws SecurityException for RestrictedClass
        System.setSecurityManager(new DenyReflectionSecurityManager(RestrictedClass.class));
        // The call must propagate the SecurityException thrown by the SecurityManager
        assertThrows(SecurityException.class, () -> ReflectionUtils.getDeclaredFields(RestrictedClass.class), "A SecurityException should be thrown when reflective access is denied");
    }
}
