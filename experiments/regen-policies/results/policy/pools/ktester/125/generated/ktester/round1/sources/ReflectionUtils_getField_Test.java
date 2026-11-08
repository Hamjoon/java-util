package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.Collection;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.security.Permission;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getField_Test {

    /**
     * Simple class with two fields
     */
    static class Simple {

        private int intField;

        public String strField;
    }

    /**
     * Subclass that inherits a field from its parent
     */
    static class Parent {

        protected long parentField;
    }

    static class Child extends Parent {

        private double childField;
    }

    /**
     * Simple class with a private and a public field
     */
    static class SimpleClass {

        private int value;

        public String name;
    }

    /**
     * Class that only contains a private field
     */
    static class PrivateClass {

        private String secret;
    }

    /**
     * Class that contains a static field
     */
    static class StaticClass {

        static final double PI = 3.1415;
    }

    /**
     * Super‑class used for inheritance tests
     */
    static class SuperClass {

        protected long id;
    }

    /**
     * Sub‑class that inherits {@code id} and adds its own field
     */
    static class SubClass extends SuperClass {

        private String extra;
    }

    /**
     * Super‑class that defines a field which is hidden in the subclass
     */
    static class HidingSuperClass {

        int count;
    }

    /**
     * Sub‑class that hides {@code count} from its super‑class
     */
    static class HidingSubClass extends HidingSuperClass {

        private String count;
    }

    /**
     * Class used to provoke a {@link SecurityException} from {@code getAllDeclaredFields}
     */
    static class SecureClass {

        public int anyField;
    }

    private static class NullCheckCase {

        final Class<?> clazz;

        final String fieldName;

        final String expectedMessage;

        NullCheckCase(Class<?> clazz, String fieldName, String expectedMessage) {
            this.clazz = clazz;
            this.fieldName = fieldName;
            this.expectedMessage = expectedMessage;
        }

        @Override
        public String toString() {
            return "clazz=" + (clazz == null ? "null" : clazz.getSimpleName()) + ", fieldName=" + fieldName;
        }
    }

    /**
     * A minimal {@link SecurityManager} that throws {@link SecurityException}
     * when {@code Class#getDeclaredFields()} is invoked.
     */
    static class TestSecurityManager extends SecurityManager {

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkMemberAccess(Class<?> clazz, int which) {
            // Disallow reflective access to declared fields
            if (which == java.lang.reflect.Member.DECLARED) {
                throw new SecurityException("Access to declared fields denied by test SecurityManager");
            }
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Ensure the cache does not retain stale data between tests.
        // The cache is package‑private; we can clear it via reflection.
        try {
            java.lang.reflect.Field cacheField = ReflectionUtils.class.getDeclaredField("FIELD_NAME_CACHE");
            cacheField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<?, ?> cache = (java.util.Map<?, ?>) cacheField.get(null);
            cache.clear();
        } catch (Exception e) {
            // If we cannot clear the cache, the tests will still be valid; just log.
            System.err.println("Unable to clear ReflectionUtils cache: " + e);
        }
    }

    @AfterEach
    void teardownAfterEach() {
        // Restore the default SecurityManager if a test replaced it.
        if (System.getSecurityManager() instanceof TestSecurityManager) {
            System.setSecurityManager(null);
        }
    }

    static Stream<NullCheckCase> nullCheckProvider() {
        return Stream.of(new NullCheckCase(null, "any", "class cannot be null"), new NullCheckCase(String.class, null, "fieldName cannot be null"));
    }

    @Test
    void getField_existingPrivateField() {
        Field f = ReflectionUtils.getField(Simple.class, "intField");
        assertNotNull(f, "Field should be found");
        assertEquals("intField", f.getName());
        assertEquals(int.class, f.getType());
    }

    @Test
    void getField_existingPublicField() {
        Field f = ReflectionUtils.getField(Simple.class, "strField");
        assertNotNull(f);
        assertEquals("strField", f.getName());
        assertEquals(String.class, f.getType());
    }

    @Test
    void getField_inheritedField() {
        Field f = ReflectionUtils.getField(Child.class, "parentField");
        assertNotNull(f);
        assertEquals("parentField", f.getName());
        assertEquals(long.class, f.getType());
    }

    @Test
    void getField_nonExistentFieldReturnsNull() {
        Field f = ReflectionUtils.getField(Simple.class, "doesNotExist");
        assertNull(f, "Non‑existent field should return null");
    }

    @Test
    void getField_nullClassThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getField(null, "anyName"));
    }

    @Test
    void getField_nullFieldNameThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getField(Simple.class, null));
    }

    @Test
    void getField_cachesResult() {
        // First lookup – cache miss
        Field first = ReflectionUtils.getField(SimpleClass.class, "name");
        assertNotNull(first, "First lookup must succeed");
        // Verify that the cache now contains at least one entry
        try {
            java.lang.reflect.Field cacheField = ReflectionUtils.class.getDeclaredField("FIELD_NAME_CACHE");
            cacheField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<?, ?> cache = (java.util.Map<?, ?>) cacheField.get(null);
            assertTrue(cache.size() >= 1, "Cache should contain an entry after first lookup");
        } catch (Exception e) {
            fail("Unable to inspect cache: " + e);
        }
        // Second lookup – should return the exact same Field instance
        Field second = ReflectionUtils.getField(SimpleClass.class, "name");
        assertSame(first, second, "Subsequent lookup must return the cached Field instance");
    }

    @Test
    void getAllDeclaredFields_containsReturnedField() {
        // Verify that the field returned by getField is also present in the collection
        Collection<Field> all = ReflectionUtils.getAllDeclaredFields(Simple.class);
        Field f = ReflectionUtils.getField(Simple.class, "intField");
        assertTrue(all.contains(f), "All declared fields collection should contain the field obtained via getField");
    }

    @ParameterizedTest(name = "[{index}] class={0}, fieldName={1}")
    @MethodSource("nullCheckProvider")
    void getField_nullArgumentsThrowIllegalArgumentException(NullCheckCase c) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getField(c.clazz, c.fieldName));
        assertTrue(ex.getMessage().contains(c.expectedMessage), () -> "Exception message should contain '" + c.expectedMessage + "'");
    }

    @Test
    void getField_publicField() {
        Field f = ReflectionUtils.getField(SimpleClass.class, "name");
        assertNotNull(f, "Public field should be found");
        assertEquals("name", f.getName());
        assertEquals(String.class, f.getType());
        assertTrue(java.lang.reflect.Modifier.isPublic(f.getModifiers()));
    }

    @Test
    void getField_privateField() {
        Field f = ReflectionUtils.getField(PrivateClass.class, "secret");
        assertNotNull(f, "Private field should be found");
        assertEquals("secret", f.getName());
        assertEquals(String.class, f.getType());
        assertTrue(java.lang.reflect.Modifier.isPrivate(f.getModifiers()));
    }

    @Test
    void getField_staticField() {
        Field f = ReflectionUtils.getField(StaticClass.class, "PI");
        assertNotNull(f, "Static field should be found");
        assertEquals("PI", f.getName());
        assertEquals(double.class, f.getType());
        assertTrue(java.lang.reflect.Modifier.isStatic(f.getModifiers()));
    }

    @Test
    void getField_inheritedFieldFromSuperclass() {
        Field f = ReflectionUtils.getField(SubClass.class, "id");
        assertNotNull(f, "Inherited field should be found");
        assertEquals("id", f.getName());
        assertEquals(long.class, f.getType());
        // The field is declared in SuperClass, not SubClass
        assertEquals(SuperClass.class, f.getDeclaringClass());
    }

    @Test
    void getField_fieldDeclaredInSubclass() {
        Field f = ReflectionUtils.getField(SubClass.class, "extra");
        assertNotNull(f, "Subclass‑declared field should be found");
        assertEquals("extra", f.getName());
        assertEquals(String.class, f.getType());
        assertEquals(SubClass.class, f.getDeclaringClass());
    }

    @Test
    void getField_fieldHidingPrefersSubclassDeclaration() {
        Field f = ReflectionUtils.getField(HidingSubClass.class, "count");
        assertNotNull(f, "Field should be found even when hidden");
        assertEquals("count", f.getName());
        // The returned field must be the one declared in HidingSubClass
        assertEquals(HidingSubClass.class, f.getDeclaringClass(), "Subclass's field should mask the superclass field");
    }

    @Test
    void getField_missingFieldReturnsNull() {
        Field f = ReflectionUtils.getField(SimpleClass.class, "nonExistent");
        assertNull(f, "Lookup of a non‑existent field must return null");
    }

    @Test
    void getField_primitiveClassReturnsNull() {
        Field f = ReflectionUtils.getField(int.class, "any");
        assertNull(f, "Primitive types have no fields; should return null");
    }

    @Test
    void getField_propagatesSecurityException() {
        // Install the test SecurityManager
        System.setSecurityManager(new TestSecurityManager());
        SecurityException se = assertThrows(SecurityException.class, () -> ReflectionUtils.getField(SecureClass.class, "anyField"), "A SecurityException should be propagated when access is denied");
        assertTrue(se.getMessage().contains("denied"), "Exception message should indicate denial");
    }
}
