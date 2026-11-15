package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getDeepDeclaredFields_Test {

    /* --------------------------------------------------------------------- */
    static class SuperClass {

        private int superPrivate;

        protected String superProtected;
    }

    static class SubClass extends SuperClass {

        public double subPublic;

        // default (package‑private) visibility
        String subPackage;
    }

    static class EmptyClass {
        // deliberately empty
    }

    // ---------- Cache behavior ----------
    static class BaseClass {

        private int baseField;
    }

    // ---------- Inheritance handling ----------
    static class GrandBaseClass {

        protected long grandBaseField;
    }

    static class DerivedClass extends GrandBaseClass {

        public String derivedField;

        // hides BaseClass.baseField – still a distinct field
        private boolean baseField;
    }

    // ---------- Interface with fields ----------
    interface InterfaceWithFields {

        String CONST_ONE = "ONE";

        int CONST_TWO = 2;
    }

    // ---------- Modifier coverage ----------
    static class ModifierClass {

        public int publicField;

        protected String protectedField;

        private double privateField;

        static final long staticField = 42L;
    }

    /**
     * Checks whether the supplied collection contains a {@link Field}
     * whose {@code getName()} matches the expected name.
     */
    private static boolean containsFieldNamed(Collection<Field> fields, String expectedName) {
        return fields.stream().anyMatch(f -> f.getName().equals(expectedName));
    }

    /* --------------------------------------------------------------------- */
    static Stream<Arguments> cacheBehaviorProvider() {
        return Stream.of(Arguments.of(BaseClass.class, Set.of("baseField")));
    }

    static Stream<Arguments> inheritanceProvider() {
        return Stream.of(Arguments.of(DerivedClass.class, Set.of("derivedField", "grandBaseField", "baseField")));
    }

    static Stream<Arguments> modifierProvider() {
        return Stream.of(Arguments.of(ModifierClass.class, Set.of("publicField", "protectedField", "privateField", "staticField")));
    }

    /* --------------------------------------------------------------------- */
    private static boolean containsAllFieldNames(Collection<Field> fields, Set<String> expectedNames) {
        Set<String> actual = new HashSet<>();
        for (Field f : fields) {
            actual.add(f.getName());
        }
        return actual.equals(expectedNames);
    }

    @Test
    void shouldReturnAllDeclaredFieldsFromClassHierarchy() {
        // Act
        Collection<Field> fields = ReflectionUtils.getDeepDeclaredFields(SubClass.class);
        // Assert – we expect four fields: superPrivate, superProtected, subPublic, subPackage
        assertNotNull(fields, "Returned collection must not be null");
        assertEquals(4, fields.size(), "SubClass ReflectionUtils_getDeepDeclaredFields_Testshould expose exactly four fields");
        // Verify each expected field is present (by name)
        assertTrue(containsFieldNamed(fields, "superPrivate"));
        assertTrue(containsFieldNamed(fields, "superProtected"));
        assertTrue(containsFieldNamed(fields, "subPublic"));
        assertTrue(containsFieldNamed(fields, "subPackage"));
    }

    @Test
    void shouldReturnEmptyCollectionForClassWithoutDeclaredFields() {
        // Act
        Collection<Field> fields = ReflectionUtils.getDeepDeclaredFields(EmptyClass.class);
        // Assert
        assertNotNull(fields, "Returned collection must not be null");
        assertTrue(fields.isEmpty(), "EmptyClass should yield an empty field collection");
    }

    @Test
    void shouldCacheResultForSubsequentInvocations() {
        // First call – populates the cache
        Collection<Field> firstCall = ReflectionUtils.getDeepDeclaredFields(SubClass.class);
        // Second call – should retrieve the same instance from the cache
        Collection<Field> secondCall = ReflectionUtils.getDeepDeclaredFields(SubClass.class);
        assertSame(firstCall, secondCall, "Subsequent calls for the same class must return the cached collection instance");
    }

    @Test
    void shouldThrowNullPointerExceptionWhenClassIsNull() {
        // The method does not guard against a null argument; a NPE is expected.
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getDeepDeclaredFields(null), "Calling getDeepDeclaredFields with a null argument should throw NPE");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] Cache first call populates for {0}")
    @MethodSource("cacheBehaviorProvider")
    void shouldCacheResultForSubsequentInvocations_ClassCache(Class<?> clazz, Set<String> expectedFieldNames) {
        // first call – cache miss
        Collection<Field> first = ReflectionUtils.getDeepDeclaredFields(clazz);
        assertNotNull(first);
        assertEquals(expectedFieldNames.size(), first.size(), "First call should return exactly the expected number of fields");
        assertTrue(containsAllFieldNames(first, expectedFieldNames));
        // second call – cache hit, same instance
        Collection<Field> second = ReflectionUtils.getDeepDeclaredFields(clazz);
        assertSame(first, second, "Second call must return the same cached collection instance");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] Inheritance fields for {0}")
    @MethodSource("inheritanceProvider")
    void shouldCollectFieldsFromSuperClasses(Class<?> clazz, Set<String> expectedFieldNames) {
        Collection<Field> fields = ReflectionUtils.getDeepDeclaredFields(clazz);
        assertNotNull(fields);
        assertEquals(expectedFieldNames.size(), fields.size(), "Collected field count does not match expected for hierarchy");
        assertTrue(containsAllFieldNames(fields, expectedFieldNames));
    }

    @Test
    void subclassShouldNotShareCacheEntryWithSuperclass() {
        Collection<Field> baseFields = ReflectionUtils.getDeepDeclaredFields(BaseClass.class);
        Collection<Field> derivedFields = ReflectionUtils.getDeepDeclaredFields(DerivedClass.class);
        assertNotSame(baseFields, derivedFields, "Cache keys must include the full class name, so subclass gets its own entry");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void emptyClassReturnsEmptyCollection() {
        Collection<Field> fields = ReflectionUtils.getDeepDeclaredFields(EmptyClass.class);
        assertNotNull(fields);
        assertTrue(fields.isEmpty(), "EmptyClass should yield an empty collection");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void primitiveClassReturnsEmptyCollection() {
        Collection<Field> fields = ReflectionUtils.getDeepDeclaredFields(int.class);
        assertNotNull(fields);
        assertTrue(fields.isEmpty(), "Primitive types have no declared fields");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void interfaceFieldsAreReturned() {
        Collection<Field> fields = ReflectionUtils.getDeepDeclaredFields(InterfaceWithFields.class);
        assertNotNull(fields);
        Set<String> expected = Set.of("CONST_ONE", "CONST_TWO");
        assertEquals(expected.size(), fields.size(), "Interface should expose exactly its static final fields");
        assertTrue(containsAllFieldNames(fields, expected));
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] Modifier fields for {0}")
    @MethodSource("modifierProvider")
    void shouldReturnAllFieldsRegardlessOfModifier(Class<?> clazz, Set<String> expectedFieldNames) {
        Collection<Field> fields = ReflectionUtils.getDeepDeclaredFields(clazz);
        assertNotNull(fields);
        assertEquals(expectedFieldNames.size(), fields.size(), "All declared fields, irrespective of modifiers, must be present");
        assertTrue(containsAllFieldNames(fields, expectedFieldNames));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void nullClassArgumentThrowsNPE() {
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getDeepDeclaredFields(null), "Passing null should result in NullPointerException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void voidClassReturnsEmptyCollection() {
        Collection<Field> fields = ReflectionUtils.getDeepDeclaredFields(Void.class);
        assertNotNull(fields);
        assertTrue(fields.isEmpty(), "Void class has no declared fields");
    }
}
