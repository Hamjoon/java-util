package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import java.lang.reflect.Modifier;
import java.security.SecurityException;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getField_Test {

    /**
     * Simple class with a private field used for the basic positive test.
     */
    private static class Sample {

        private int value;
    }

    /**
     * Sub‑class of {@link Sample} – used to verify that inherited fields are found.
     */
    private static class SubSample extends Sample {
        // no additional fields
    }

    /**
     * Class without the requested field – used for the negative test.
     */
    private static class NoSuchField {

        private String other;
    }

    /**
     * Simple class containing three fields of different visibility/modifiers.
     */
    private static class SampleClass {

        public int publicField;

        private String privateField;

        static long staticField;
    }

    /**
     * Superclass with a field that will be shadowed by {@link SubClass}.
     */
    private static class SuperClass {

        // will be shadowed
        int value;

        // only exists in the superclass
        String superValue;
    }

    /**
     * Subclass that declares its own {@code value} field, hiding the one from {@link SuperClass}.
     */
    private static class SubClass extends SuperClass {

        // shadows SuperClass.value
        int value;
    }

    /**
     * Dummy class used to simulate a SecurityManager denial.
     */
    private static class RestrictedClass {

        int anyField;
    }

    @BeforeEach
    void setupBeforeEach() {
        // Reset the internal field cache so each test starts with a clean slate.
        ReflectionUtils.setFieldCache(new ConcurrentHashMap<>());
        // Ensure a clean cache for every test method.
        ReflectionUtils.setFieldCache(new ConcurrentHashMap<>());
    }

    private static Stream<Arguments> provideSuccessfulFieldRetrieval() {
        return Stream.of(Arguments.of("Public instance field", SampleClass.class, "publicField", false), Arguments.of("Private instance field", SampleClass.class, "privateField", false), Arguments.of("Static field", SampleClass.class, "staticField", true));
    }

    @Test
    void shouldReturnFieldWhenItExistsInClass() {
        Field f = ReflectionUtils.getField(Sample.class, "value");
        assertNotNull(f, "Field should be found");
        assertEquals("value", f.getName());
        assertEquals(Sample.class, f.getDeclaringClass());
    }

    @Test
    void shouldReturnInheritedFieldWhenDeclaredInSuperclass() {
        Field f = ReflectionUtils.getField(SubSample.class, "value");
        assertNotNull(f, "Inherited field should be found");
        assertEquals("value", f.getName());
        // The declaring class is the superclass where the field is actually declared.
        assertEquals(Sample.class, f.getDeclaringClass());
    }

    @Test
    void shouldReturnNullWhenFieldDoesNotExist() {
        Field f = ReflectionUtils.getField(NoSuchField.class, "nonExistent");
        assertNull(f, "Non‑existent field should result in null");
    }

    @Test
    void shouldThrowWhenClassArgumentIsNull() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getField(null, "any"), "Null class argument must cause an exception");
        assertTrue(thrown.getMessage().contains("class cannot be null"));
    }

    @Test
    void shouldThrowWhenFieldNameArgumentIsNull() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getField(Sample.class, null), "Null fieldName argument must cause an exception");
        assertTrue(thrown.getMessage().contains("fieldName cannot be null"));
    }

    @Test
    void shouldCacheFieldInstance() {
        // First call – populates the cache.
        Field first = ReflectionUtils.getField(Sample.class, "value");
        assertNotNull(first);
        // Second call – should retrieve the same instance from the cache.
        Field second = ReflectionUtils.getField(Sample.class, "value");
        assertSame(first, second, "Subsequent calls must return the cached Field instance");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideSuccessfulFieldRetrieval")
    void shouldReturnFieldForVariousVisibilities(String description, Class<?> targetClass, String fieldName, boolean expectStatic) {
        Field f = ReflectionUtils.getField(targetClass, fieldName);
        assertNotNull(f, "Field should be found");
        assertEquals(fieldName, f.getName());
        assertEquals(targetClass, f.getDeclaringClass());
        // Verify static‑ness matches expectation (the method does not filter static fields)
        assertEquals(expectStatic, Modifier.isStatic(f.getModifiers()), "Static modifier does not match expectation");
    }

    @Test
    void shouldReturnSubclassShadowingField() {
        // SubClass declares its own 'value' field; it should be returned, not the one from SuperClass.
        Field f = ReflectionUtils.getField(SubClass.class, "value");
        assertNotNull(f);
        assertEquals("value", f.getName());
        assertEquals(SubClass.class, f.getDeclaringClass(), "Shadowing field should be the one declared in SubClass");
    }

    @Test
    void shouldReturnSuperclassFieldWhenNotShadowed() {
        // 'superValue' exists only in SuperClass; getField must walk the hierarchy.
        Field f = ReflectionUtils.getField(SubClass.class, "superValue");
        assertNotNull(f);
        assertEquals("superValue", f.getName());
        assertEquals(SuperClass.class, f.getDeclaringClass(), "Field should be declared in SuperClass");
    }

    @Test
    void shouldFindEnumOrdinalField() {
        // Enum fields are synthetic but still part of the class hierarchy.
        Field f = ReflectionUtils.getField(SampleEnum.class, "ordinal");
        assertNotNull(f);
        assertEquals("ordinal", f.getName());
        // The declaring class for synthetic enum fields is java.lang.Enum
        assertEquals(Enum.class, f.getDeclaringClass(), "Enum internal field should be declared in java.lang.Enum");
    }

    @Test
    void shouldFindEnumNameField() {
        Field f = ReflectionUtils.getField(SampleEnum.class, "name");
        assertNotNull(f);
        assertEquals("name", f.getName());
        assertEquals(Enum.class, f.getDeclaringClass(), "Enum internal field should be declared in java.lang.Enum");
    }

    @Test
    void shouldReturnNullForPrimitiveClass() {
        // Primitive types have no declared fields.
        Field f = ReflectionUtils.getField(int.class, "TYPE");
        assertNull(f, "Primitive class should return null for any field name");
    }

    @Test
    void shouldPropagateSecurityExceptionFromHelper() {
        // Mock the static helper method getAllDeclaredFields to throw a SecurityException.
        try (MockedStatic<ReflectionUtils> mocked = mockStatic(ReflectionUtils.class)) {
            // The lambda below mimics the behaviour of the real method for the specific class.
            mocked.when(() -> ReflectionUtils.getAllDeclaredFields(RestrictedClass.class)).thenThrow(new SecurityException("access denied"));
            // The cache is still cleared, so the computeIfAbsent lambda will invoke the mocked method.
            SecurityException ex = assertThrows(SecurityException.class, () -> ReflectionUtils.getField(RestrictedClass.class, "anyField"));
            assertEquals("access denied", ex.getMessage());
        }
    }

    @Test
    void shouldThrowNPEWhenHelperReturnsNullCollection() {
        // Mock getAllDeclaredFields to return null, which will cause the for‑each loop to NPE.
        try (MockedStatic<ReflectionUtils> mocked = mockStatic(ReflectionUtils.class)) {
            mocked.when(() -> ReflectionUtils.getAllDeclaredFields(SampleClass.class)).thenReturn(null);
            NullPointerException npe = assertThrows(NullPointerException.class, () -> ReflectionUtils.getField(SampleClass.class, "publicField"));
            // The exact message is not defined; we only care that NPE is thrown.
            assertNotNull(npe);
        }
    }
}
