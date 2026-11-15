package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_isConversionSupportedFor_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Use a minimal ConverterOptions implementation – only the required methods
        ConverterOptions options = new ConverterOptions() {

            @Override
            public <T> T getCustomOption(String name) {
                return null;
            }
        };
        converter = new Converter(options);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup needed
        converter = null;
    }

    private static Stream<Arguments> conversionInMapProvider() {
        return Stream.of(// Direct conversion is present in the built‑in map (String → Integer)
        Arguments.of(String.class, Integer.class, true));
    }

    private static Stream<Arguments> collectionConversionProvider() {
        return Stream.of(// Array → Array where target component type is Object (always true)
        Arguments.of(String[].class, Object[].class, true), // Array → Array where component conversion exists (Integer → Number)
        Arguments.of(Integer[].class, Number[].class, true), // Array → Array where component conversion does NOT exist (String → Integer)
        Arguments.of(String[].class, Integer[].class, false), // Collection → Collection (List → Set)
        Arguments.of(ArrayList.class, HashSet.class, true), // Collection → Array (HashSet → String[])
        Arguments.of(HashSet.class, String[].class, true));
    }

    private static Stream<Arguments> nullArgumentProvider() {
        return Stream.of(// source null, target non‑null
        Arguments.of(null, String.class), // source non‑null, target null
        Arguments.of(Integer.class, null), // both null
        Arguments.of(null, null));
    }

    /* -----------------------------------------------------------
     *  Tests for simple (non‑collection) conversions
     * ----------------------------------------------------------- */
    @Test
    void simpleConversionSupported() {
        assertTrue(converter.isConversionSupportedFor(String.class, Integer.class), "String → Integer should be supported");
    }

    @Test
    void simpleConversionNotSupported() {
        assertFalse(converter.isConversionSupportedFor(String.class, Thread.class), "String → Thread should NOT be supported");
    }

    /* -----------------------------------------------------------
     *  Tests for collection conversions
     * ----------------------------------------------------------- */
    @Test
    void collectionConversionSupported() {
        assertTrue(converter.isConversionSupportedFor(List.class, Set.class), "List → Set should be supported");
    }

    @Test
    void collectionConversionSupportedForArrayToArray_ObjectComponent() {
        assertTrue(converter.isConversionSupportedFor(Integer[].class, Object[].class), "Integer[] → Object[] should be supported because target component is Object");
    }

    @Test
    void arrayToArrayComponentConversionSupported() {
        // Integer → Long conversion exists, so Integer[] → Long[] should be supported
        assertTrue(converter.isConversionSupportedFor(Integer[].class, Long[].class), "Integer[] → Long[] should be supported via component conversion");
    }

    @Test
    void arrayToArrayComponentConversionNotSupported() {
        // No conversion from String to Thread, so String[] → Thread[] should be false
        assertFalse(converter.isConversionSupportedFor(String[].class, Thread[].class), "String[] → Thread[] should NOT be supported");
    }

    /* -----------------------------------------------------------
     *  Tests for inheritance‑based conversions (fallback path)
     * ----------------------------------------------------------- */
    @Test
    void inheritanceConversionSupported() {
        // ArrayList implements List – conversion should be found via inheritance handling
        assertTrue(converter.isConversionSupportedFor(ArrayList.class, List.class), "ArrayList → List should be supported via inheritance");
    }

    @Test
    void inheritanceConversionNotSupported() {
        // No conversion defined from Thread to Runnable (conversion map does not contain it)
        assertFalse(converter.isConversionSupportedFor(Thread.class, Runnable.class), "Thread → Runnable should NOT be supported");
    }

    /* -----------------------------------------------------------
     *  Parameterized tests for direct conversion map look‑ups
     * ----------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] source={0}, target={1} → expected={2}")
    @MethodSource("conversionInMapProvider")
    void conversionInMap_ShouldReturnExpectedResult(Class<?> source, Class<?> target, boolean expected) {
        // The method under test should return true when a direct conversion exists in the map.
        assertEquals(expected, converter.isConversionSupportedFor(source, target), () -> String.format("Expected %s for %s → %s", expected, source.getName(), target.getName()));
    }

    /* -----------------------------------------------------------
     *  Parameterized tests for collection / array conversion logic
     * ----------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] source={0}, target={1} → expected={2}")
    @MethodSource("collectionConversionProvider")
    void collectionConversionSupported_ShouldReturnExpectedResult(Class<?> source, Class<?> target, boolean expected) {
        assertEquals(expected, converter.isConversionSupportedFor(source, target), () -> String.format("Expected %s for %s → %s", expected, source.getName(), target.getName()));
    }

    /* -----------------------------------------------------------
     *  Null‑argument handling – expect NullPointerException
     * ----------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] source={0}, target={1} → expects NPE")
    @MethodSource("nullArgumentProvider")
    void nullArguments_ShouldThrowNullPointerException(Class<?> source, Class<?> target) {
        assertThrows(NullPointerException.class, () -> converter.isConversionSupportedFor(source, target), () -> String.format("Calling with source=%s, target=%s should throw NPE", source == null ? "null" : source.getName(), target == null ? "null" : target.getName()));
    }
}
