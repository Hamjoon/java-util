package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.OffsetTime;
import java.util.Map;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.ZoneOffset;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OffsetTimeConversions_toMap_Test {

    /**
     * Converter instance used for the tests – the method under test does not actually use it.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter requires a ConverterOptions instance; use the default options.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        converter = null;
    }

    private static Stream<Arguments> validOffsetTimeProvider() {
        return Stream.of(// Case 1 – null converter (the method ignores the converter)
        Arguments.of("Proper OffsetTime with null converter", OffsetTime.of(10, 15, 30, 0, ZoneOffset.ofHours(1)), null, "10:15:30+01:00"), // Case 2 – non‑null (mock) converter
        Arguments.of("Proper OffsetTime with non‑null converter", OffsetTime.of(23, 59, 59, 999_999_999, ZoneOffset.ofHoursMinutes(-5, -30)), mock(Converter.class), "23:59:59.999999999-05:30"));
    }

    @Test
    void toMap_returnsMapWithTimeKeyAndStringValue() {
        // GIVEN
        OffsetTime offsetTime = OffsetTime.parse("12:34:56+02:00");
        // WHEN
        Map<String, Object> result = OffsetTimeConversions.toMap(offsetTime, converter);
        // THEN
        assertNotNull(result, "Resulting map should not be null");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        assertEquals(offsetTime.toString(), result.get(MapConversions.TIME), "Map value for the TIME key should be the OffsetTime's string representation");
    }

    @Test
    void toMap_ignoresNullConverter() {
        // GIVEN
        OffsetTime offsetTime = OffsetTime.parse("23:45:01-05:00");
        // WHEN
        Map<String, Object> result = OffsetTimeConversions.toMap(offsetTime, null);
        // THEN
        assertNotNull(result);
        assertEquals(offsetTime.toString(), result.get(MapConversions.TIME));
    }

    /**
     * Parameterized test covering valid {@link OffsetTime} inputs.
     *
     * The test verifies that the resulting map contains exactly one entry
     * with key {@link MapConversions#TIME} and that the value matches the
     * {@code OffsetTime}'s {@code toString()} representation.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validOffsetTimeProvider")
    void toMap_validOffsetTime(String description, OffsetTime input, Converter suppliedConverter, String expectedString) {
        // WHEN
        Map<String, Object> result = OffsetTimeConversions.toMap(input, suppliedConverter);
        // THEN
        assertNotNull(result, "Resulting map should not be null");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        assertEquals(expectedString, result.get(MapConversions.TIME), "Map value for the TIME key should be the OffsetTime's string representation");
    }

    @Test
    void toMap_nullFrom_throwsNullPointerException() {
        // GIVEN
        Object from = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> OffsetTimeConversions.toMap(from, null), "Casting a null reference to OffsetTime should throw NullPointerException");
    }

    @Test
    void toMap_stringFrom_throwsClassCastException() {
        // GIVEN
        Object from = "2023-01-01T12:00:00Z";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> OffsetTimeConversions.toMap(from, null), "Providing a String instead of OffsetTime should throw ClassCastException");
    }

    @Test
    void toMap_objectFrom_throwsClassCastException() {
        // GIVEN
        Object from = new Object();
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> OffsetTimeConversions.toMap(from, null), "Providing a non‑OffsetTime object should throw ClassCastException");
    }
}
