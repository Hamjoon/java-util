package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getField_Test {

    /* --------------------------------------------------------------------- */
    static class Base {

        private int baseInt;

        protected String baseString;
    }

    static class Derived extends Base {

        private double derivedDouble;
    }

    /**
     * Superclass with a field that will be hidden by a subclass.
     */
    static class SuperClass {

        public int hidden = 1;
    }

    /**
     * Subclass that declares a field with the same name as its superclass.
     */
    static class SubClass extends SuperClass {

        public String hidden = "sub";
    }

    private static Stream<Arguments> nullCheckProvider() {
        return Stream.of(Arguments.of(null, "any", "class cannot be null"), Arguments.of(String.class, null, "fieldName cannot be null"));
    }

    private static Stream<Arguments> existingFieldProvider() {
        return Stream.of(// java.lang.String has a private char[] value field
        Arguments.of(String.class, "value", (java.util.function.Predicate<Field>) f -> "value".equals(f.getName()) && f.getDeclaringClass() == String.class), // java.util.Collections.EMPTY_LIST is public static final
        Arguments.of(Collections.class, "EMPTY_LIST", (java.util.function.Predicate<Field>) f -> "EMPTY_LIST".equals(f.getName()) && Modifier.isStatic(f.getModifiers())), // java.util.ArrayList has a private int size field
        Arguments.of(ArrayList.class, "size", (java.util.function.Predicate<Field>) f -> "size".equals(f.getName()) && Modifier.isPrivate(f.getModifiers())));
    }

    private static Stream<Arguments> inheritedFieldProvider() {
        return Stream.of(// AbstractList.modCount (protected) is inherited by ArrayList
        Arguments.of(ArrayList.class, "modCount", (java.util.function.Predicate<Field>) f -> "modCount".equals(f.getName()) && f.getDeclaringClass() == java.util.AbstractList.class), // HashMap.size is package‑private and declared directly in HashMap
        Arguments.of(HashMap.class, "size", (java.util.function.Predicate<Field>) f -> "size".equals(f.getName()) && f.getDeclaringClass() == HashMap.class));
    }

    private static Stream<Arguments> nonExistentFieldProvider() {
        return Stream.of(Arguments.of(Integer.class, "nonExisting"), Arguments.of(String.class, ""));
    }

    private static Stream<Arguments> edgeTypeProvider() {
        return Stream.of(// Primitive types have no fields
        Arguments.of(int.class, "ANY", null), // Array classes have no declared fields
        Arguments.of(String[].class, "length", null), // Interface constant folding – Runnable inherits MAX_PRIORITY from Thread (static final)
        Arguments.of(Runnable.class, "MAX_PRIORITY", (java.util.function.Predicate<Field>) f -> f.getName().equals("MAX_PRIORITY") && Modifier.isStatic(f.getModifiers()) && f.getDeclaringClass() == Thread.class));
    }

    @Test
    void shouldReturnDeclaredFieldFromExactClass() {
        Field f = ReflectionUtils.getField(Derived.class, "derivedDouble");
        assertNotNull(f, "Field should be found on the exact class");
        assertEquals("derivedDouble", f.getName());
        assertEquals(double.class, f.getType());
    }

    @Test
    void shouldReturnInheritedFieldFromSuperclass() {
        Field f = ReflectionUtils.getField(Derived.class, "baseInt");
        assertNotNull(f, "Inherited field should be found");
        assertEquals("baseInt", f.getName());
        assertEquals(int.class, f.getType());
    }

    @Test
    void shouldReturnNullWhenFieldDoesNotExist() {
        Field f = ReflectionUtils.getField(Derived.class, "nonExistent");
        assertNull(f, "Non‑existent field should result in null");
    }

    @Test
    void shouldThrowWhenClassIsNull() {
        assertThrows(RuntimeException.class, () -> ReflectionUtils.getField(null, "any"));
    }

    @Test
    void shouldThrowWhenFieldNameIsNull() {
        assertThrows(RuntimeException.class, () -> ReflectionUtils.getField(Derived.class, null));
    }

    @Test
    void subsequentCallsShouldReturnSameFieldInstance() {
        Field first = ReflectionUtils.getField(ArrayList.class, "size");
        Field second = ReflectionUtils.getField(ArrayList.class, "size");
        assertSame(first, second, "Two consecutive calls must return the identical cached Field instance");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: class={0}, fieldName={1}")
    @MethodSource("nullCheckProvider")
    void shouldThrowWhenArgumentsAreNull(Class<?> clazz, String fieldName, String expectedMessage) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getField(clazz, fieldName));
        assertEquals(expectedMessage, ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: class={0}, field={1}")
    @MethodSource("existingFieldProvider")
    void shouldReturnDeclaredField(Class<?> clazz, String fieldName, java.util.function.Predicate<Field> assertions) {
        Field f = ReflectionUtils.getField(clazz, fieldName);
        assertNotNull(f, "Field should be found");
        assertTrue(assertions.test(f), "Field does not satisfy the expected conditions");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: class={0}, field={1}")
    @MethodSource("inheritedFieldProvider")
    void shouldReturnInheritedField(Class<?> clazz, String fieldName, java.util.function.Predicate<Field> assertions) {
        Field f = ReflectionUtils.getField(clazz, fieldName);
        assertNotNull(f, "Inherited field should be found");
        assertTrue(assertions.test(f), "Field does not satisfy the expected conditions");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void shouldReturnSubclassFieldWhenNameIsHidden() {
        Field f = ReflectionUtils.getField(SubClass.class, "hidden");
        assertNotNull(f, "Field should be found");
        assertEquals(SubClass.class, f.getDeclaringClass(), "The field declared in the subclass must be returned when a name is hidden");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: class={0}, field={1}")
    @MethodSource("edgeTypeProvider")
    void shouldHandleEdgeTypes(Class<?> clazz, String fieldName, java.util.function.Predicate<Field> expectations) {
        Field f = ReflectionUtils.getField(clazz, fieldName);
        if (expectations == null) {
            assertNull(f, "Expected null for edge type " + clazz);
        } else {
            assertNotNull(f, "Expected a field for edge type " + clazz);
            assertTrue(expectations.test(f), "Field does not meet expected criteria");
        }
    }
}
