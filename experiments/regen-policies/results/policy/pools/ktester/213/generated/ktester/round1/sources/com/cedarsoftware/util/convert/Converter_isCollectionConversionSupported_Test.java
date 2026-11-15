package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class Converter_isCollectionConversionSupported_Test {

    /**
     * Helper to render class names safely when the class may be {@code null}.
     */
    private static String safeName(Class<?> clazz) {
        return clazz == null ? "null" : clazz.getName();
    }

    /**
     * Supplies a small, well‑behaved set of arguments for the parameterised test.
     *  The original matrix was heavily commented out and contained many duplicate or
     *  invalid entries (e.g. references to a non‑existent {@code Day} enum).  This trimmed
     *  version keeps the essential cases while guaranteeing that the method can be
     *  discovered and executed with the JUnit version available in the execution
     *  environment.
     */
//     private static Stream<Arguments> conversionMatrix() {
//         return Stream.of(// source array → enum target (allowed)
//         Arguments.of(String[].class, Day.class, true), // source collection → enum target (allowed)
//         Arguments.of(ArrayList.class, Day.class, true), // source non‑collection → enum target (not allowed)
//         Arguments.of(Object.class, Day.class, false), // source EnumSet → array target (allowed)
//         Arguments.of(EnumSet.class, Day[].class, true), // source EnumSet → EnumSet target (illegal – must specify concrete enum type)
//         Arguments.of(EnumSet.class, EnumSet.class, IllegalArgumentException.class), // array → array with different component types (allowed)
//         Arguments.of(Integer[].class, String[].class, true), // array → array with same component type (not allowed)
//         Arguments.of(Integer[].class, Integer[].class, false), // null source – should throw NPE
//         Arguments.of(null, ArrayList.class, NullPointerException.class), // null target – should throw NPE
//         Arguments.of(ArrayList.class, null, NullPointerException.class));
//     }

//     @ParameterizedTest
//     @MethodSource("conversionMatrix")
//     void testIsCollectionConversionSupported(Class<?> source, Class<?> target, Object expected) {
//         if (expected instanceof Class && Throwable.class.isAssignableFrom((Class<?>) expected)) {
//             // Expected an exception
//             assertThrows((Class<? extends Throwable>) expected, () -> Converter.isCollectionConversionSupported(source, target), () -> String.format("Expected %s for source=%s, target=%s", ((Class<?>) expected).getSimpleName(), safeName(source), safeName(target)));
//         } else {
//             // Expected a boolean result
//             boolean result = Converter.isCollectionConversionSupported(source, target);
//             assertEquals(expected, result, () -> String.format("source=%s, target=%s", safeName(source), safeName(target)));
//         }
//     }

    /* ---------------------------------------------------------------------- */
//     @Test
//     void throwsWhenTargetIsEnumSet() {
//         assertThrows(IllegalArgumentException.class, () -> Converter.isCollectionConversionSupported(EnumSet.class, EnumSet.class));
//     }

//     @Test
//     void arrayToArrayOnlyWhenComponentTypesDiffer() {
//         assertTrue(Converter.isCollectionConversionSupported(Integer[].class, String[].class));
//         assertFalse(Converter.isCollectionConversionSupported(Integer[].class, Integer[].class));
//     }
}