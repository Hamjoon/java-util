package com.cedarsoftware.util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_isPrimitive_Test {

    /* ----------------------------------------------------------------------
     *  Parameter sources
     * ---------------------------------------------------------------------- */
    private static Stream<Arguments> primitiveClasses() {
        return Stream.of(Arguments.of(boolean.class), Arguments.of(byte.class), Arguments.of(char.class), Arguments.of(short.class), Arguments.of(int.class), Arguments.of(long.class), Arguments.of(float.class), Arguments.of(double.class), Arguments.of(void.class));
    }

    private static Stream<Arguments> wrapperClasses() {
        return Stream.of(Arguments.of(Boolean.class), Arguments.of(Byte.class), Arguments.of(Character.class), Arguments.of(Short.class), Arguments.of(Integer.class), Arguments.of(Long.class), Arguments.of(Float.class), Arguments.of(Double.class), Arguments.of(Void.class));
    }

    private static Stream<Arguments> nonPrimitiveClasses() {
        return Stream.of(Arguments.of(String.class), Arguments.of(Object.class), Arguments.of(Number.class), Arguments.of(java.util.List.class), Arguments.of(java.util.Map.class), // primitive array
        Arguments.of(int[].class), // reference array
        Arguments.of(String[].class), // base Enum class
        Arguments.of(Enum.class), // JDK date class
        Arguments.of(LocalDate.class));
    }

    /* ----------------------------------------------------------------------
     *  Tests
     * ---------------------------------------------------------------------- */
    @DisplayName("isPrimitive returns true for all Java primitive types")
    @ParameterizedTest(name = "{index} => primitive={0}")
    @MethodSource("primitiveClasses")
    void testIsPrimitive_WithPrimitiveTypes(Class<?> primitive) {
        assertTrue(ClassUtilities.isPrimitive(primitive), () -> "Expected primitive type " + primitive.getName() + " to be reported as primitive");
    }

    @DisplayName("isPrimitive returns true for all primitive wrapper types")
    @ParameterizedTest(name = "{index} => wrapper={0}")
    @MethodSource("wrapperClasses")
    void testIsPrimitive_WithWrapperTypes(Class<?> wrapper) {
        assertTrue(ClassUtilities.isPrimitive(wrapper), () -> "Expected wrapper type " + wrapper.getName() + " to be reported as primitive");
    }

    @DisplayName("isPrimitive returns false for non‑primitive, non‑wrapper types")
    @ParameterizedTest(name = "{index} => type={0}")
    @MethodSource("nonPrimitiveClasses")
    void testIsPrimitive_WithNonPrimitiveTypes(Class<?> type) {
        assertFalse(ClassUtilities.isPrimitive(type), () -> "Expected non‑primitive type " + type.getName() + " to be reported as not primitive");
    }

    @Test
    @DisplayName("isPrimitive throws NullPointerException when passed null")
    void testIsPrimitive_NullArgument() {
        assertThrows(NullPointerException.class, () -> ClassUtilities.isPrimitive(null), "Calling isPrimitive(null) should throw NullPointerException");
    }
}
