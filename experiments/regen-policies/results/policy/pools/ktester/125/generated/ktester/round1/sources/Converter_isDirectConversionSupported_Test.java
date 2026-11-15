package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.ZoneId;
import java.util.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_isDirectConversionSupported_Test {

    /**
     * The Converter instance used for all tests.
     */
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Each test gets a fresh Converter to avoid cross‑test contamination.
        converter = new Converter(defaultOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Help the GC – not strictly required but keeps the pattern consistent.
        converter = null;
    }

    /**
     * Simple {@link ConverterOptions} implementation that uses the defaults.
     */
    private static ConverterOptions defaultOptions() {
        return new ConverterOptions() {

            @Override
            public <T> T getCustomOption(String name) {
                return null;
            }
        };
    }

    /**
     * Provides test data where the expected outcome is a boolean.
     */
    private static Stream<Arguments> booleanCases() {
        return Stream.of(// Direct conversion defined in map
        Arguments.of(String.class, Integer.class, true, "String → Integer is present in the internal conversion map"), // Array‑to‑array conversion where component conversion is supported
        Arguments.of(String[].class, Integer[].class, true, "String[] → Integer[] – component conversion exists"), // Array‑to‑array conversion where component conversion is NOT supported
        Arguments.of(String[].class, Date[].class, false, "String[] → Date[] – component conversion missing"), // Collection conversion (List → Set) supported
        Arguments.of(List.class, Set.class, true, "List → Set – collection conversion should be true"), // Array to Collection conversion supported
        Arguments.of(String[].class, List.class, true, "String[] → List – array‑to‑collection conversion"), // Collection to Array conversion supported
        Arguments.of(List.class, Integer[].class, true, "List → Integer[] – collection‑to‑array conversion"), // Neither map nor collection conversion supported
        Arguments.of(String.class, Date.class, false, "String → Date – no conversion defined"), // Same source and target type without explicit map entry
        Arguments.of(UUID.class, UUID.class, false, "UUID → UUID – no explicit entry, should be false"), // Primitive array to wrapper array conversion not supported
        Arguments.of(int[].class, Integer[].class, false, "int[] → Integer[] – primitive‑to‑wrapper array not supported"));
    }

    /**
     * Provides test data where a {@link NullPointerException} is expected.
     */
    private static Stream<Arguments> nullCases() {
        return Stream.of(Arguments.of(null, String.class, "null source"), Arguments.of(String.class, null, "null target"));
    }

    @Test
    void directConversion_StringToInteger_isSupported() {
        assertTrue(converter.isDirectConversionSupported(String.class, Integer.class), "String → Integer should be a direct conversion");
    }

    @Test
    void directConversion_BigDecimalToDouble_isSupported() {
        assertTrue(converter.isDirectConversionSupported(BigDecimal.class, Double.class), "BigDecimal → Double should be a direct conversion");
    }

    @Test
    void collectionConversion_ListToSet_isSupported() {
        assertTrue(converter.isDirectConversionSupported(List.class, Set.class), "List → Set conversion should be supported via collection handling");
    }

    @Test
    void collectionConversion_MapToProperties_isSupported() {
        assertTrue(converter.isDirectConversionSupported(Map.class, Properties.class), "Map → Properties conversion should be supported via collection handling");
    }

    @Test
    void arrayToArray_ComponentsConvertible_isSupported() {
        Class<?> source = Integer[].class;
        // Integer → Long conversion exists in the built‑in map
        Class<?> target = Long[].class;
        assertTrue(converter.isDirectConversionSupported(source, target), "Integer[] → Long[] should be supported because Integer → Long is direct");
    }

    @Test
    void arrayToArray_ComponentsNotConvertible_isNotSupported() {
        Class<?> source = String[].class;
        // No direct String → Date conversion in the map
        Class<?> target = Date[].class;
        assertFalse(converter.isDirectConversionSupported(source, target), "String[] → Date[] should NOT be supported because component conversion is missing");
    }

    @Test
    void unrelatedTypes_isNotSupported() {
        assertFalse(converter.isDirectConversionSupported(Object.class, UUID.class), "Object → UUID should not be a supported conversion");
    }

    @Test
    void enumSetConversion_ArrayToEnumSet_isSupported() {
        // source is an array of enums (or enum names) – collection conversion should kick in.
        assertTrue(converter.isDirectConversionSupported(DayOfWeek[].class, EnumSet.class), "Array → EnumSet conversion should be reported as supported");
    }

    @Test
    void primitiveWrapperToPrimitive_isSupported() {
        assertTrue(converter.isDirectConversionSupported(Integer.class, int.class), "Wrapper → primitive conversion should be considered direct");
    }

    @Test
    void primitiveToWrapper_isSupported() {
        assertTrue(converter.isDirectConversionSupported(int.class, Integer.class), "Primitive → wrapper conversion should be considered direct");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("booleanCases")
    void booleanScenarios(Class<?> source, Class<?> target, boolean expected, String description) {
        if (expected) {
            assertTrue(converter.isDirectConversionSupported(source, target), description);
        } else {
            assertFalse(converter.isDirectConversionSupported(source, target), description);
        }
    }

    @ParameterizedTest(name = "[{index}] Null handling – {2}")
    @MethodSource("nullCases")
    void nullParameterHandling(Class<?> source, Class<?> target, String description) {
        assertThrows(NullPointerException.class, () -> converter.isDirectConversionSupported(source, target), "Expected NullPointerException when " + description);
    }
}
