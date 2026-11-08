package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_getPrimitiveFromWrapper_Test {

    /**
     * Provides the standard Java wrapper → primitive pairs.
     */
    private static Stream<Arguments> wrapperToPrimitiveProvider() {
        return Stream.of(Arguments.of(Boolean.class, boolean.class), Arguments.of(Byte.class, byte.class), Arguments.of(Character.class, char.class), Arguments.of(Short.class, short.class), Arguments.of(Integer.class, int.class), Arguments.of(Long.class, long.class), Arguments.of(Float.class, float.class), Arguments.of(Double.class, double.class), Arguments.of(Void.class, void.class));
    }

    private static Stream<Arguments> nonWrapperProvider() {
        return Stream.of(Arguments.of(String.class), Arguments.of(Object.class), Arguments.of(java.util.Date.class), Arguments.of(java.math.BigDecimal.class), Arguments.of(java.util.concurrent.ConcurrentHashMap.class), // primitive array
        Arguments.of(int[].class), // array of wrappers
        Arguments.of(Integer[].class), // array of non‑wrappers
        Arguments.of(String[].class), // primitive void (not a wrapper)
        Arguments.of(void.class));
    }

    private static Stream<Arguments> primitiveClassProvider() {
        return Stream.of(Arguments.of(int.class), Arguments.of(boolean.class), Arguments.of(char.class), Arguments.of(void.class), Arguments.of(double.class), Arguments.of(float.class), Arguments.of(long.class), Arguments.of(short.class), Arguments.of(byte.class));
    }

    /**
     * Verifies that each known wrapper class returns its primitive counterpart.
     */
    @ParameterizedTest(name = "{index}: {0} → {1}")
    @MethodSource("wrapperToPrimitiveProvider")
    void getPrimitiveFromWrapper_ReturnsPrimitive(Class<?> wrapper, Class<?> expectedPrimitive) {
        Class<?> actual = ClassUtilities.getPrimitiveFromWrapper(wrapper);
        assertEquals(expectedPrimitive, actual, () -> "Wrapper " + wrapper.getName() + " should map to primitive " + expectedPrimitive.getName());
    }

    @Test
    void getPrimitiveFromWrapper_NonWrapperReturnsNull() {
        assertNull(ClassUtilities.getPrimitiveFromWrapper(String.class), "Non‑wrapper class should return null");
        assertNull(ClassUtilities.getPrimitiveFromWrapper(Object.class), "Non‑wrapper class should return null");
        assertNull(ClassUtilities.getPrimitiveFromWrapper(int[].class), "Array class should return null");
    }

    @Test
    void getPrimitiveFromWrapper_NullArgumentThrows() {
        // Convention.throwIfNull throws IllegalArgumentException (or NullPointerException);
        // The exact type is not part of the public contract, so we accept either.
        assertThrows(RuntimeException.class, () -> ClassUtilities.getPrimitiveFromWrapper(null));
    }

    @ParameterizedTest(name = "{index}: primitive {0}")
    @MethodSource("primitiveClassProvider")
    void getPrimitiveFromWrapper_PrimitiveArgumentReturnsNull(Class<?> primitiveClass) {
        assertNull(ClassUtilities.getPrimitiveFromWrapper(primitiveClass), () -> "Primitive class " + primitiveClass.getName() + " should return null");
    }

    @Test
    void getPrimitiveFromWrapper_RepeatedCallsReturnSameResult() {
        Class<?> first = ClassUtilities.getPrimitiveFromWrapper(Long.class);
        Class<?> second = ClassUtilities.getPrimitiveFromWrapper(Long.class);
        Class<?> third = ClassUtilities.getPrimitiveFromWrapper(Long.class);
        assertAll(() -> assertEquals(long.class, first, "First call should return long.class"), () -> assertEquals(long.class, second, "Second call should return long.class"), () -> assertEquals(long.class, third, "Third call should return long.class"));
    }
}
