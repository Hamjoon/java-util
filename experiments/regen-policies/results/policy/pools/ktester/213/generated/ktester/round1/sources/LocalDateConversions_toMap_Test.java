package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class LocalDateConversions_toMap_Test {

    private Converter converter;

    /**
     * A minimal dummy {@link Converter} implementation used in tests where a
     * non‑null Converter instance is required. The class does not add any
     * behaviour; it simply satisfies the method signature.
     */
    private static class DummyConverter extends Converter {

        DummyConverter() {
            super(new ConverterOptions());
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Default Converter instance – the method under test does not use it,
        // but we provide a non‑null instance for the tests that require it.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clean up instance references
        converter = null;
    }

    /**
     * Provides arguments for {@link #toMap_successfulConversion}.
     * Each argument consists of:
     *   - the LocalDate to convert,
     *   - a Converter instance (null or DummyConverter),
     *   - the expected ISO‑8601 string representation.
     */
    private static Stream<Arguments> successfulConversionProvider() {
        return Stream.of(// Regular date, null converter
        arguments(LocalDate.of(2023, 5, 17), null, "2023-05-17"), // Maximum representable LocalDate, dummy converter
        arguments(LocalDate.MAX, new DummyConverter(), "+999999999-12-31"), // Minimum representable LocalDate, null converter
        arguments(LocalDate.MIN, null, "-999999999-01-01"), // Leap day, dummy converter
        arguments(LocalDate.of(2020, 2, 29), new DummyConverter(), "2020-02-29"), // Additional check for map size and key/value (also covered by other tests)
        arguments(LocalDate.of(2000, 1, 1), null, "2000-01-01"));
    }

    @Test
    void toMap_returnsMapWithLocalDateString() {
        // GIVEN
        LocalDate date = LocalDate.of(2023, 1, 15);
        // WHEN
        Map<String, Object> result = LocalDateConversions.toMap(date, converter);
        // THEN
        assertNotNull(result, "Resulting map should not be null");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        assertTrue(result.containsKey(MapConversions.LOCAL_DATE), "Map should contain the key defined by MapConversions.LOCAL_DATE");
        assertEquals(date.toString(), result.get(MapConversions.LOCAL_DATE), "Map value should be the string representation of the LocalDate");
    }

    @Test
    void toMap_nullInput_throwsNullPointerException() {
        // GIVEN
        Object nullObj = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toMap(nullObj, converter), "Passing null as the source should result in NullPointerException");
    }

    /**
     * Parameterized test for successful conversions, covering normal, extreme,
     * and leap‑year dates. The supplied {@link Converter} instance is ignored by
     * the implementation, but we test both {@code null} and a dummy instance.
     */
    @ParameterizedTest(name = "[{index}] from={0}, converter={1}")
    @MethodSource("successfulConversionProvider")
    void toMap_successfulConversion(LocalDate from, Converter suppliedConverter, String expectedIsoString) {
        // WHEN
        Map<String, Object> result = LocalDateConversions.toMap(from, suppliedConverter);
        // THEN
        assertNotNull(result, "Result map must not be null");
        assertEquals(1, result.size(), "Result map must contain exactly one entry");
        assertEquals(expectedIsoString, result.get(MapConversions.LOCAL_DATE), "Map should contain the ISO‑8601 string representation of the LocalDate");
    }

    /**
     * Verifies that the map contains exactly one entry with the expected key
     * and value for a specific date (2000‑01‑01). This test mirrors the JSON
     * description "Map contains single entry".
     */
    @Test
    void toMap_mapContainsSingleEntry() {
        // GIVEN
        LocalDate date = LocalDate.of(2000, 1, 1);
        // WHEN
        Map<String, Object> result = LocalDateConversions.toMap(date, converter);
        // THEN
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        assertTrue(result.containsKey(MapConversions.LOCAL_DATE), "Map should contain the expected key");
        assertEquals("2000-01-01", result.get(MapConversions.LOCAL_DATE), "Map value should match the ISO‑8601 date string");
    }

    /**
     * Passing an object of an incompatible type (e.g., a String) must cause
     * a {@link ClassCastException} because the method casts the input to
     * {@link LocalDate}.
     */
    @Test
    void toMap_invalidSourceType_throwsClassCastException() {
        // GIVEN
        Object notALocalDate = "2023-05-17";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> LocalDateConversions.toMap(notALocalDate, converter), "Providing a non‑LocalDate source should throw ClassCastException");
    }
}
