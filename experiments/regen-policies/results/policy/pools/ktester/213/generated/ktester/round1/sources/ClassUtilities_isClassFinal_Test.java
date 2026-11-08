package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_isClassFinal_Test {

    /**
     * Provides a stream of arguments: the class to test, the expected boolean result,
     * and a short description useful for debugging failures.
     */
    private static Stream<Arguments> classFinalProvider() {
        return Stream.of(// ----- Final class detection -----
        Arguments.of(String.class, true, "java.lang.String is final"), Arguments.of(Integer.class, true, "java.lang.Integer is final"), Arguments.of(java.util.Collections.class, true, "java.util.Collections is a final utility class"), Arguments.of(java.time.DayOfWeek.class, true, "Enum types are implicitly final"), Arguments.of(int[].class, true, "Array classes are generated as final by the JVM"), Arguments.of(java.time.LocalDate.class, true, "LocalDate (record‑like) is final"), // ----- Non‑final class detection -----
        Arguments.of(java.util.ArrayList.class, false, "ArrayList is not final"), Arguments.of(java.util.List.class, false, "Interfaces are never final"), Arguments.of(java.util.AbstractList.class, false, "AbstractList is abstract, not final"), Arguments.of(java.lang.Number.class, false, "Number is abstract, not final"), Arguments.of(java.lang.Override.class, false, "Annotation types are interfaces, not final"), Arguments.of(ClassUtilities.ClassHierarchyInfo.class, false, "Nested static class ClassHierarchyInfo is not final"), Arguments.of(Object.class, false, "Object is not final"), // ----- Primitive type handling -----
        Arguments.of(int.class, false, "Primitive int is not a class with FINAL modifier"));
    }

    @Test
    @DisplayName("isClassFinal returns true for a final class")
    void testIsClassFinal_WithFinalClass() {
        // java.lang.String is declared as final
        assertTrue(ClassUtilities.isClassFinal(String.class), "String should be recognised as final");
    }

    @Test
    @DisplayName("isClassFinal returns false for a non‑final class")
    void testIsClassFinal_WithNonFinalClass() {
        // java.util.ArrayList is not final
        assertFalse(ClassUtilities.isClassFinal(ArrayList.class), "ArrayList should not be recognised as final");
    }

    @Test
    @DisplayName("isClassFinal returns false for an interface")
    void testIsClassFinal_WithInterface() {
        // java.util.List is an interface; interfaces are not final
        assertFalse(ClassUtilities.isClassFinal(java.util.List.class), "Interface should not be recognised as final");
    }

    @Test
    @DisplayName("isClassFinal returns false for a primitive type")
    void testIsClassFinal_WithPrimitive() {
        // Primitive types are not classes with the FINAL modifier
        assertFalse(ClassUtilities.isClassFinal(int.class), "Primitive type int should not be recognised as final");
    }

    @Test
    @DisplayName("isClassFinal returns false for an anonymous (non‑final) class")
    void testIsClassFinal_WithAnonymousClass() {
        Object anon = new Object() {
        };
        assertFalse(ClassUtilities.isClassFinal(anon.getClass()), "Anonymous class should not be recognised as final");
    }

    @Test
    @DisplayName("isClassFinal throws NullPointerException for null argument")
    void testIsClassFinal_NullArgument() {
        assertThrows(NullPointerException.class, () -> ClassUtilities.isClassFinal(null), "Calling isClassFinal with null should throw NullPointerException");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("classFinalProvider")
    @DisplayName("isClassFinal returns expected result for various class types")
    void testIsClassFinal_VariousClasses(Class<?> clazz, boolean expected, String description) {
        assertEquals(expected, ClassUtilities.isClassFinal(clazz), () -> "Unexpected result for class: " + clazz.getName() + " (" + description + ")");
    }
}
