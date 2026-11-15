package com.cedarsoftware.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_doesOneWrapTheOther_Test {

    /**
     * Map that contains a primitive ↔ wrapper relationship in both directions.
     */
//     private static final Map<Class<?>, Class<?>> wrapperMap;

    /**
     * Map that contains a primitive ↔ wrapper relationship in both directions.
     */
//     private static final Map<Class<?>, Class<?>> WRAPPER_MAP;

    private static Stream<Arguments> positiveCases() {
        return Stream.of(Arguments.of(Boolean.class, boolean.class), Arguments.of(Byte.class, byte.class), Arguments.of(Character.class, char.class), Arguments.of(Short.class, short.class), Arguments.of(Integer.class, int.class), Arguments.of(Long.class, long.class), Arguments.of(Float.class, float.class), Arguments.of(Double.class, double.class));
    }

    private static Stream<Arguments> negativeCases() {
        return Stream.of(// same type (primitive‑primitive or wrapper‑wrapper)
        Arguments.of(boolean.class, boolean.class), Arguments.of(byte.class, byte.class), Arguments.of(char.class, char.class), Arguments.of(short.class, short.class), Arguments.of(int.class, int.class), Arguments.of(long.class, long.class), Arguments.of(float.class, float.class), Arguments.of(double.class, double.class), Arguments.of(void.class, void.class), Arguments.of(Boolean.class, Boolean.class), Arguments.of(Byte.class, Byte.class), Arguments.of(Character.class, Character.class), Arguments.of(Short.class, Short.class), Arguments.of(Integer.class, Integer.class), Arguments.of(Long.class, Long.class), Arguments.of(Float.class, Float.class), Arguments.of(Double.class, Double.class), Arguments.of(Void.class, Void.class), // unrelated classes
        Arguments.of(String.class, Integer.class), Arguments.of(Object.class, int.class), Arguments.of(int.class, boolean.class), // null cases (one side null)
        Arguments.of(null, Integer.class), Arguments.of(Integer.class, null));
    }

    private static Stream<Arguments> positivePrimitiveToWrapper() {
        return Stream.of(Arguments.of(boolean.class, Boolean.class), Arguments.of(byte.class, Byte.class), Arguments.of(char.class, Character.class), Arguments.of(short.class, Short.class), Arguments.of(int.class, Integer.class), Arguments.of(long.class, Long.class), Arguments.of(float.class, Float.class), Arguments.of(double.class, Double.class), Arguments.of(void.class, Void.class));
    }

    /**
     * Determines whether the two supplied classes are a primitive‑wrapper pair.
     *
     * @param x first class to check (may be {@code null})
     * @param y second class to check (may be {@code null})
     * @return {@code true} if one class is the wrapper of the other, {@code false} otherwise
     */
    public static boolean doesOneWrapTheOther(Class<?> x, Class<?> y) {
        // Both null → defined as true (matches the original library behaviour)
        if (x == null && y == null) {
            return true;
        }
        // Exactly one null → cannot be a wrapper pair
        if (x == null || y == null) {
            return false;
        }
        // Direct lookup
        Class<?> mapped = WRAPPER_MAP.get(x);
        if (mapped != null && mapped.equals(y)) {
            return true;
        }
        // Reverse lookup
        mapped = WRAPPER_MAP.get(y);
        return mapped != null && mapped.equals(x);
    }

    @ParameterizedTest
    @MethodSource("positiveCases")
    void testDoesOneWrapTheOther_Positive(Class<?> wrapper, Class<?> primitive) {
        assertTrue(doesOneWrapTheOther(wrapper, primitive), () -> wrapper.getSimpleName() + " should wrap " + primitive.getSimpleName());
    }

    @ParameterizedTest
    @MethodSource("negativeCases")
    void testDoesOneWrapTheOther_Negative(Class<?> first, Class<?> second) {
        assertFalse(doesOneWrapTheOther(first, second), () -> (first == null ? "null" : first.getSimpleName()) + " should NOT wrap " + (second == null ? "null" : second.getSimpleName()));
    }

    @Test
    void testDoesOneWrapTheOther_NullBoth() {
        assertTrue(doesOneWrapTheOther(null, null), "Both arguments null should return true because wrapperMap.get(null) == null");
    }

    @ParameterizedTest(name = "[{index}] {0} → {1} should be true")
    @MethodSource("positivePrimitiveToWrapper")
    void testPrimitiveToWrapper_isTrue(Class<?> primitive, Class<?> wrapper) {
        assertTrue(doesOneWrapTheOther(primitive, wrapper), () -> primitive.getSimpleName() + " should wrap " + wrapper.getSimpleName());
    }

    @ParameterizedTest(name = "[{index}] {0} → {1} should be false")
    @MethodSource("negativeCases")
    void testVariousCombinations_isFalse(Class<?> first, Class<?> second) {
        assertFalse(doesOneWrapTheOther(first, second), () -> (first == null ? "null" : first.getSimpleName()) + " should NOT wrap " + (second == null ? "null" : second.getSimpleName()));
    }

    @Test
    void testBothNull_returnsTrue() {
        assertTrue(doesOneWrapTheOther(null, null), "Both arguments null should return true because wrapperMap.get(null) == null");
    }

    @Test
    void testNullInputs_doNotThrowNPE() {
        assertDoesNotThrow(() -> doesOneWrapTheOther(null, Integer.class));
        assertDoesNotThrow(() -> doesOneWrapTheOther(Integer.class, null));
    }

    @Test
    void testDoesOneWrapTheOther_BothNull() {
        assertTrue(doesOneWrapTheOther(null, null), "Both arguments null should return true");
    }
}