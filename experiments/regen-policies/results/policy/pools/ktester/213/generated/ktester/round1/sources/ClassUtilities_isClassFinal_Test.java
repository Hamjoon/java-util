package com.cedarsoftware.util;

import java.lang.reflect.Modifier;
import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ClassUtilities_isClassFinal_Test {

    /**
     * Determines if a class is declared as {@code final}.
     *
     * @param c the class to check; must not be {@code null}
     * @return {@code true} if the class is {@code final}, {@code false} otherwise
     * @throws NullPointerException if {@code c} is {@code null}
     */
    public static boolean isClassFinal(Class<?> c) {
        if (c == null) {
            throw new NullPointerException("Class argument must not be null");
        }
        return Modifier.isFinal(c.getModifiers());
    }

    private static Stream<Arguments> finalClassProvider() {
        // Standard library final classes
        return Stream.of(// java.lang.String is final
        Arguments.of(String.class), // java.lang.Integer is final
        Arguments.of(Integer.class), // java.math.BigDecimal is final
        Arguments.of(java.math.BigDecimal.class), // java.time.DayOfWeek is an enum, implicitly final
        Arguments.of(java.time.DayOfWeek.class));
    }

    private static Stream<Arguments> nonFinalClassProvider() {
        // Mutable / non‑final types
        return Stream.of(// concrete mutable class
        Arguments.of(java.util.ArrayList.class), // abstract class (cannot be final)
        Arguments.of(java.util.AbstractList.class), // interface
        Arguments.of(java.util.List.class), // primitive type (not final)
        Arguments.of(int.class), // void type (not final)
        Arguments.of(void.class));
    }

    @ParameterizedTest(name = "{index}: {0} is final")
    @MethodSource("finalClassProvider")
    @DisplayName("isClassFinal should return true for final classes")
    void testIsClassFinal_True(Class<?> clazz) {
        assertTrue(ClassUtilities.isClassFinal(clazz), () -> "Expected " + clazz.getName() + " to be reported as final");
    }

    @ParameterizedTest(name = "{index}: {0} is not final")
    @MethodSource("nonFinalClassProvider")
    @DisplayName("isClassFinal should return false for non‑final classes")
    void testIsClassFinal_False(Class<?> clazz) {
        assertFalse(ClassUtilities.isClassFinal(clazz), () -> "Expected " + clazz.getName() + " to be reported as non‑final");
    }

    @Test
    @DisplayName("isClassFinal should throw NullPointerException for null argument")
    void testIsClassFinal_NullArgument() {
        Executable call = () -> ClassUtilities.isClassFinal(null);
        assertThrows(NullPointerException.class, call, "Calling isClassFinal(null) should throw NPE");
    }
}
