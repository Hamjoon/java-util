package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.security.Permission;
import java.util.concurrent.ConcurrentHashMap;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getDeepDeclaredFields_Test {

    /**
     * Simple base class with two fields (private and protected).
     */
    static class Base {

        private int baseInt;

        protected String baseString;
    }

    /**
     * Derived class adds a public field.
     */
    static class Derived extends Base {

        public double derivedDouble;
    }

    /**
     * Simple class with a public and a private field.
     */
    static class SimpleClass {

        public int publicField;

        private String privateField;
    }

    /**
     * Base class for inheritance tests.
     */
    static class BaseClass {

        public int basePublic;

        protected String baseProtected;
    }

    /**
     * Subclass that adds its own fields.
     */
    static class SubClass extends BaseClass {

        public double subPublic;

        private long subPrivate;
    }

    /**
     * Class with no declared fields.
     */
    static class EmptyClass {
    }

    /**
     * Interface with constants (implicitly public static final).
     */
    interface SampleInterface {

        String CONST_A = "A";

        int CONST_B = 42;
    }

    /**
     * Class used to demonstrate that the cached collection is mutable.
     */
    static class MutableClass {

        int fieldA;
    }

    /**
     * Two distinct classes that share the same simple name but live in different
     *  “packages” (simulated by nesting inside different outer classes).
     */
    static class pkg1 {

        static class DuplicateName {

            int p1Field;
        }
    }

    static class DuplicateName {

        int p1Field;
    }

    static class DuplicateName {

        int p1Field;
    }

    static class pkg2 {

        static class DuplicateName {

            String p2Field;
        }
    }

    static class DuplicateName {

        String p2Field;
    }

    static class DuplicateName {

        String p2Field;
    }

    /**
     * Class for the SecurityException test – any class works, the SecurityManager
     *  will block reflective access.
     */
    static class RestrictedClass {

        int secret;
    }

    @AfterEach
    void clearCache() throws Exception {
        // Use reflection to replace the internal ConcurrentMap with a fresh one.
        var field = ReflectionUtils.class.getDeclaredField("FIELD_MAP");
        field.setAccessible(true);
        field.set(null, new ConcurrentHashMap<String, Collection<Field>>());
    }

    /**
     * Extracts the set of field names from a collection of {@link Field}s.
     */
    private static Set<String> fieldNames(Collection<Field> fields) {
        Set<String> names = new HashSet<>();
        for (Field f : fields) {
            names.add(f.getName());
        }
        return names;
    }

    @Test
    void getDeepDeclaredFields_IncludesSuperclassFields() {
        // Act
        Collection<Field> fields = ReflectionUtils.getDeepDeclaredFields(Derived.class);
        // Collect field names for easier assertions
        Set<String> fieldNames = new HashSet<>();
        for (Field f : fields) {
            fieldNames.add(f.getName());
        }
        // Assert that all three fields are present
        assertTrue(fieldNames.contains("baseInt"), "Should contain private field from Base");
        assertTrue(fieldNames.contains("baseString"), "Should contain protected field from Base");
        assertTrue(fieldNames.contains("derivedDouble"), "Should contain public field from Derived");
        // No extra fields (the collection may contain synthetic fields, but those are not declared)
        assertEquals(3, fieldNames.size(), "Exactly three declared fields expected");
    }

    @Test
    void getDeepDeclaredFields_ReturnsSameInstanceOnSubsequentCalls() {
        // First call – creates and caches the collection
        Collection<Field> firstCall = ReflectionUtils.getDeepDeclaredFields(Base.class);
        // Second call – should retrieve the cached collection
        Collection<Field> secondCall = ReflectionUtils.getDeepDeclaredFields(Base.class);
        // Verify that the same object reference is returned (caching works)
        assertSame(firstCall, secondCall, "Subsequent calls should return the cached collection instance");
    }

    @Test
    void getDeepDeclaredFields_ObjectClassReturnsEmptyCollection() {
        Collection<Field> fields = ReflectionUtils.getDeepDeclaredFields(Object.class);
        assertNotNull(fields, "Result should never be null");
        assertTrue(fields.isEmpty(), "Object.class has no declared fields, collection should be empty");
    }

    @Test
    void cacheMiss_ReturnsAllDeclaredFields() {
        // First call – cache miss
        Collection<Field> fields = ReflectionUtils.getDeepDeclaredFields(SimpleClass.class);
        assertNotNull(fields, "Result must not be null on cache miss");
        Set<String> names = fieldNames(fields);
        assertEquals(2, names.size(), "SimpleClass should have exactly two declared fields");
        assertTrue(names.contains("publicField"));
        assertTrue(names.contains("privateField"));
    }

    @Test
    void cacheHit_ReturnsSameInstance() {
        // Populate the cache
        Collection<Field> first = ReflectionUtils.getDeepDeclaredFields(SimpleClass.class);
        // Second call – should hit the cache
        Collection<Field> second = ReflectionUtils.getDeepDeclaredFields(SimpleClass.class);
        assertSame(first, second, "Subsequent calls must return the same cached collection instance");
    }

    @Test
    void inheritance_IncludesSuperclassFields() {
        Collection<Field> fields = ReflectionUtils.getDeepDeclaredFields(SubClass.class);
        Set<String> names = fieldNames(fields);
        // Expected fields from SubClass and BaseClass
        assertTrue(names.containsAll(Set.of("subPublic", "subPrivate", "basePublic", "baseProtected")), "All subclass and superclass fields must be present");
        assertEquals(4, names.size(), "Exactly four declared fields expected");
    }

    @Test
    void inheritance_SuperclassOnlyDoesNotIncludeSubclassFields() {
        Collection<Field> fields = ReflectionUtils.getDeepDeclaredFields(BaseClass.class);
        Set<String> names = fieldNames(fields);
        assertTrue(names.containsAll(Set.of("basePublic", "baseProtected")));
        assertEquals(2, names.size(), "BaseClass should contain only its own fields");
    }

    @Test
    void emptyClass_ReturnsEmptyCollection() {
        Collection<Field> fields = ReflectionUtils.getDeepDeclaredFields(EmptyClass.class);
        assertNotNull(fields);
        assertTrue(fields.isEmpty(), "EmptyClass has no declared fields");
    }

    @Test
    void interfaceFields_AreReturned() {
        Collection<Field> fields = ReflectionUtils.getDeepDeclaredFields(SampleInterface.class);
        Set<String> names = fieldNames(fields);
        assertTrue(names.containsAll(Set.of("CONST_A", "CONST_B")), "Interface constants must be returned");
        assertEquals(2, names.size(), "Exactly two constants expected");
    }

    @Test
    void primitiveClass_ReturnsEmptyCollection() {
        Collection<Field> fields = ReflectionUtils.getDeepDeclaredFields(int.class);
        assertNotNull(fields);
        assertTrue(fields.isEmpty(), "Primitive types have no declared fields");
    }

    @Test
    void nullInput_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getDeepDeclaredFields(null), "Passing null should result in NullPointerException");
    }

    @Test
    void cacheIsolation_DifferentFullyQualifiedNames() {
        // First class (pkg1.DuplicateName)
        Collection<Field> first = ReflectionUtils.getDeepDeclaredFields(pkg1.DuplicateName.class);
        Set<String> firstNames = fieldNames(first);
        assertTrue(firstNames.contains("p1Field"));
        assertEquals(1, firstNames.size());
        // Second class (pkg2.DuplicateName) – must not collide with the first entry
        Collection<Field> second = ReflectionUtils.getDeepDeclaredFields(pkg2.DuplicateName.class);
        Set<String> secondNames = fieldNames(second);
        assertTrue(secondNames.contains("p2Field"));
        assertEquals(1, secondNames.size());
        // Ensure distinct collection instances (different cache keys)
        assertNotSame(first, second, "Cache entries for different classes must be distinct");
    }

    @Test
    void collectionMutability_ImpactOnCache() {
        // First call – cache populated
        Collection<Field> original = ReflectionUtils.getDeepDeclaredFields(MutableClass.class);
        assertEquals(1, original.size(), "MutableClass initially has one declared field");
        // Mutate the cached collection
        original.clear();
        assertTrue(original.isEmpty(), "Collection should be empty after clear");
        // Second call – should return the same (now empty) collection
        Collection<Field> afterClear = ReflectionUtils.getDeepDeclaredFields(MutableClass.class);
        assertSame(original, afterClear, "Cache must return the same (mutated) collection");
        assertTrue(afterClear.isEmpty(), "Cached collection reflects the mutation");
    }

    @Test
    void securityException_OnRestrictedClass() {
        // Install a SecurityManager that denies reflective access to fields.
        SecurityManager original = System.getSecurityManager();
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything except ReflectPermission "suppressAccessChecks"
                if ("suppressAccessChecks".equals(perm.getName())) {
                    throw new SecurityException("Reflective access denied by test SecurityManager");
                }
            }
        });
        try {
            assertThrows(SecurityException.class, () -> ReflectionUtils.getDeepDeclaredFields(RestrictedClass.class), "Expected SecurityException when reflective access is denied");
        } finally {
            // Restore the original SecurityManager to avoid side‑effects on other tests.
            System.setSecurityManager(original);
        }
    }
}
