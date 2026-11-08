package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_isPrimitive_Test {

    static Stream<Arguments> primitiveProvider() {
        return Stream.of(Arguments.of(int.class, true, "Primitive int"), Arguments.of(boolean.class, true, "Primitive boolean"), Arguments.of(void.class, true, "Primitive void"), Arguments.of(char.class, true, "Primitive char"), Arguments.of(byte.class, true, "Primitive byte"), Arguments.of(short.class, true, "Primitive short"), Arguments.of(long.class, true, "Primitive long"), Arguments.of(float.class, true, "Primitive float"), Arguments.of(double.class, true, "Primitive double"));
    }

    static Stream<Arguments> nonPrimitiveProvider() {
        ???;
    }

    @Test
    void primitiveTypesShouldBeRecognised() {
        assertTrue(ClassUtilities.isPrimitive(int.class), "int should be primitive");
        assertTrue(ClassUtilities.isPrimitive(boolean.class), "boolean should be primitive");
        assertTrue(ClassUtilities.isPrimitive(byte.class), "byte should be primitive");
        assertTrue(ClassUtilities.isPrimitive(char.class), "char should be primitive");
        assertTrue(ClassUtilities.isPrimitive(short.class), "short should be primitive");
        assertTrue(ClassUtilities.isPrimitive(long.class), "long should be primitive");
        assertTrue(ClassUtilities.isPrimitive(float.class), "float should be primitive");
        assertTrue(ClassUtilities.isPrimitive(double.class), "double should be primitive");
        assertTrue(ClassUtilities.isPrimitive(void.class), "void should be primitive");
    }

    @Test
    void wrapperTypesShouldBeRecognised() {
        assertTrue(ClassUtilities.isPrimitive(Integer.class), "Integer wrapper should be recognised");
        assertTrue(ClassUtilities.isPrimitive(Boolean.class), "Boolean wrapper should be recognised");
        assertTrue(ClassUtilities.isPrimitive(Byte.class), "Byte wrapper should be recognised");
        assertTrue(ClassUtilities.isPrimitive(Character.class), "Character wrapper should be recognised");
        assertTrue(ClassUtilities.isPrimitive(Short.class), "Short wrapper should be recognised");
        assertTrue(ClassUtilities.isPrimitive(Long.class), "Long wrapper should be recognised");
        assertTrue(ClassUtilities.isPrimitive(Float.class), "Float wrapper should be recognised");
        assertTrue(ClassUtilities.isPrimitive(Double.class), "Double wrapper should be recognised");
        assertTrue(ClassUtilities.isPrimitive(Void.class), "Void wrapper should be recognised");
    }

    @Test
    void nonPrimitiveTypesShouldReturnFalse() {
        assertFalse(ClassUtilities.isPrimitive(String.class), "String is not primitive");
        assertFalse(ClassUtilities.isPrimitive(Object.class), "Object is not primitive");
        assertFalse(ClassUtilities.isPrimitive(java.util.Date.class), "Date is not primitive");
        assertFalse(ClassUtilities.isPrimitive(int[].class), "Array of primitives is not primitive class");
        ???;
    }

    @Test
    void nullArgumentShouldThrowNpe() {
        assertThrows(NullPointerException.class, () -> ClassUtilities.isPrimitive(null), "Calling isPrimitive(null) should throw NullPointerException");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("primitiveProvider")
    void primitiveTypesParameterized(Class<?> clazz, boolean expected, String description) {
        assertEquals(expected, ClassUtilities.isPrimitive(clazz), () -> description + " should be reported as primitive");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("nonPrimitiveProvider")
    void nonPrimitiveTypesParameterized(Class<?> clazz, boolean expected, String description) {
        assertEquals(expected, ClassUtilities.isPrimitive(clazz), () -> description + " should be reported as non‑primitive");
    }

    @Test
    void addingClassToPrimsMakesItPrimitive() throws Exception {
        Class<?> target = java.math.BigDecimal.class;
        assertFalse(ClassUtilities.isPrimitive(target), "BigDecimal should initially be non‑primitive");
        Field primsField = ClassUtilities.class.getDeclaredField("prims");
        primsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<Class<?>> primsSet = (Set<Class<?>>) primsField.get(null);
        boolean added = primsSet.add(target);
        assertTrue(added, "Failed to add BigDecimal to the internal prims set");
        try {
            assertTrue(ClassUtilities.isPrimitive(target), "After insertion into prims, BigDecimal should be reported as primitive");
        } finally {
            primsSet.remove(target);
        }
    }

    @Test
    void primsSetInitiallyDoesNotContainWrapperClasses() throws Exception {
        Field primsField = ClassUtilities.class.getDeclaredField("prims");
        primsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<Class<?>> primsSet = (Set<Class<?>>) primsField.get(null);
        assertFalse(primsSet.contains(Integer.class), "Internal prims set should not contain Integer by default");
        assertFalse(primsSet.contains(Boolean.class), "Internal prims set should not contain Boolean by default");
    }
}
