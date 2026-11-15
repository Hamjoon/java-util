package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class OffsetTimeConversions_toMap_Test {

    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Create a real Converter instance with default options.
        // The Converter does not affect the toMap method, but we provide a valid instance.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release static resources if any (none needed here).
        converter = null;
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Standard case – typical offset time
        Arguments.of("Standard case – a valid OffsetTime is supplied", OffsetTime.of(12, 30, 45, 123_000_000, ZoneOffset.ofHoursMinutes(2, 0)), null, MapConversions.OFFSET_TIME, "12:30:45.123+02:00"), // Edge case – midnight UTC
        Arguments.of("Edge case – midnight UTC", OffsetTime.of(0, 0, 0, 0, ZoneOffset.UTC), null, MapConversions.OFFSET_TIME, "00:00Z"), // Negative offset
        Arguments.of("Negative offset – -05:30", OffsetTime.of(7, 15, 30, 0, ZoneOffset.ofHoursMinutes(-5, -30)), null, MapConversions.OFFSET_TIME, "07:15:30-05:30"), // Maximum time with maximum positive offset (+18:00)
        Arguments.of("Maximum time with positive offset +18:00", OffsetTime.of(23, 59, 59, 999_999_999, ZoneOffset.ofHours(18)), null, MapConversions.OFFSET_TIME, "23:59:59.999999999+18:00"), // Zero nanoseconds with offset minutes (+00:45)
        Arguments.of("Zero nanoseconds with offset minutes", OffsetTime.of(12, 0, 0, 0, ZoneOffset.ofHoursMinutes(0, 45)), null, MapConversions.OFFSET_TIME, "12:00+00:45"), // Non‑null converter ignored – method should behave identically
        Arguments.of("Non‑null converter ignored", OffsetTime.of(5, 45, 12, 250_000_000, ZoneOffset.UTC), // dummy non‑null converter
        new Converter(new ConverterOptions()), MapConversions.OFFSET_TIME, "05:45:12.250Z"));
    }

    @Test
    void toMap_returnsMapWithStringRepresentation() {
        // GIVEN
        OffsetTime offsetTime = OffsetTime.of(13, 45, 30, 123_456_789, ZoneOffset.ofHoursMinutes(2, 30));
        // WHEN
        Map<String, Object> result = OffsetTimeConversions.toMap(offsetTime, converter);
        // THEN
        assertNotNull(result, "Resulting map should not be null");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        assertTrue(result.containsKey(MapConversions.OFFSET_TIME), "Map should contain the key defined in MapConversions.OFFSET_TIME");
        assertEquals(offsetTime.toString(), result.get(MapConversions.OFFSET_TIME), "Value should be the string representation of the OffsetTime");
    }

    @Test
    void toMap_withNonOffsetTime_throwsClassCastException() {
        // GIVEN
        Object notAnOffsetTime = "I am not an OffsetTime";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> OffsetTimeConversions.toMap(notAnOffsetTime, converter), "Passing a non‑OffsetTime object should result in ClassCastException");
    }

    /* --------------------------------------------------------------
     *  Parameterized tests for the various valid conversion scenarios
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversionProvider")
    void toMap_validConversions(String description, OffsetTime input, Converter suppliedConverter, String expectedKey, String expectedValue) {
        // WHEN
        Map<String, Object> result = OffsetTimeConversions.toMap(input, suppliedConverter);
        // THEN
        assertNotNull(result, "Resulting map should never be null");
        assertEquals(1, result.size(), "Map must contain exactly one entry");
        assertTrue(result.containsKey(expectedKey), "Map should contain the expected key");
        assertEquals(expectedValue, result.get(expectedKey), "Map value must match the ISO‑8601 string representation of the OffsetTime");
    }

    @Test
    void toMap_withNullSource_throwsNullPointerException() {
        // GIVEN a null 'from' argument
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> OffsetTimeConversions.toMap(null, converter), "Passing null as the source should result in NullPointerException");
    }

    @Test
    void toMap_withInvalidSourceType_throwsClassCastException() {
        // GIVEN an object that is not an OffsetTime
        Object notAnOffsetTime = LocalDate.of(2023, 1, 1);
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> OffsetTimeConversions.toMap(notAnOffsetTime, converter), "Passing a non‑OffsetTime object should cause a ClassCastException");
    }
}
