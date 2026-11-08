package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.annotation.RetentionPolicy;
import java.time.DayOfWeek;
import java.time.IsoEra;
import java.time.Month;
import java.time.chrono.IsoEra;
import java.util.EnumSet;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ClassUtilities_getClassIfEnum_Test {

    /**
     * Sample non‑enum class used for testing.
     */
    private static final class SampleClass {
    }

    private static Stream<Arguments> basicNullAndNonEnumProvider() {
        return Stream.of(// null input → null
        Arguments.of(null, null), // regular non‑enum class (String) → null
        Arguments.of(String.class, null), // another non‑enum class (our own SampleClass) → null
        Arguments.of(SampleClass.class, null));
    }

    private static Stream<Arguments> enumAndSubclassProvider() {
        // Synthetic subclass of an enum constant (EnumWithBody.SPECIAL)
        Class<?> syntheticSubclass = EnumWithBody.SPECIAL.getClass();
        return Stream.of(// Enum class – should return itself
        Arguments.of(DayOfWeek.class, DayOfWeek.class), // Another enum class – should return itself
        Arguments.of(Month.class, Month.class), // Synthetic subclass of an enum – should be treated as non‑enum → null
        Arguments.of(syntheticSubclass, null), // Primitive wrapper – not an enum → null
        Arguments.of(Integer.class, null), // Generic collection class related to enums – not an enum → null
        Arguments.of(EnumSet.class, null), // Enum defined in java.lang.annotation package – should return itself
        Arguments.of(RetentionPolicy.class, RetentionPolicy.class), // Nested enum inside another class – should return itself
        Arguments.of(IsoEra.class, IsoEra.class));
    }

    @Test
    @DisplayName("getClassIfEnum returns null when input ClassUtilities_getClassIfEnum_Testnull")
    void testNullInput() {
        assertNull(ClassUtilities.getClassIfEnum(null), "When the input class ClassUtilities_getClassIfEnum_Testnull the method should return null.");
    }

    @Test
    @DisplayName("getClassIfEnum returns the enum class for an enum type")
    void testEnumInput() {
        Class<?> result = ClassUtilities.getClassIfEnum(SampleEnum.class);
        assertNotNull(result, "Result should not be null for an enum class.");
        assertEquals(SampleEnum.class, result, "The returned class should be the same as the supplied enum class.");
    }

    @Test
    @DisplayName("getClassIfEnum returns null for a non‑enum class")
    void testNonEnumInput() {
        Class<?> result = ClassUtilities.getClassIfEnum(SampleClass.class);
        assertNull(result, "A non‑enum class should cause the method to return null.");
    }

    @Test
    @DisplayName("getClassIfEnum caches the result – repeated calls return the same reference")
    void testCacheBehaviour() {
        // First call – populates the cache
        Class<?> firstCall = ClassUtilities.getClassIfEnum(SampleEnum.class);
        // Second call – should retrieve the cached value
        Class<?> secondCall = ClassUtilities.getClassIfEnum(SampleEnum.class);
        assertSame(firstCall, secondCall, "Subsequent calls for the same enum should return the identical Class object (cached).");
        // Verify that different enum types are cached independently
        Class<?> dayOfWeekFirst = ClassUtilities.getClassIfEnum(DayOfWeek.class);
        Class<?> dayOfWeekSecond = ClassUtilities.getClassIfEnum(DayOfWeek.class);
        assertSame(dayOfWeekFirst, dayOfWeekSecond, "Cache must work for multiple distinct enum types as well.");
        assertNotSame(firstCall, dayOfWeekFirst, "Different enum classes must not share the same cached reference.");
    }

    /* --------------------------------------------------------------
     *  Group 1 – basic null and non‑enum handling
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] input={0} → expected={1}")
    @MethodSource("basicNullAndNonEnumProvider")
    @DisplayName("getClassIfEnum – basic null and non‑enum handling")
    void testBasicNullAndNonEnum(Class<?> input, Class<?> expected) {
        Class<?> result = ClassUtilities.getClassIfEnum(input);
        assertEquals(expected, result, () -> String.format("Input %s should resolve to %s", input, expected));
    }

    /* --------------------------------------------------------------
     *  Group 2 – enum and subclass handling
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] enumInput={0} → expected={1}")
    @MethodSource("enumAndSubclassProvider")
    @DisplayName("getClassIfEnum – enum and subclass handling")
    void testEnumAndSubclassHandling(Class<?> input, Class<?> expected) {
        Class<?> result = ClassUtilities.getClassIfEnum(input);
        assertEquals(expected, result, () -> String.format("Input %s should resolve to %s", input, expected));
    }
}
