package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.time.LocalTime;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class LocalTimeConversions_toMap_Test {

    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter is required by the method signature but not used internally.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the shared Converter reference.
        converter = null;
    }

    /* the literal "time" if the constant is not initialised).               */
    private static String timeKey() {
        return MapConversions.TIME != null ? MapConversions.TIME : "time";
    }

    /* Parameterized test data for valid LocalTime conversions                */
    private static Stream<Arguments> validLocalTimeProvider() {
        return Stream.of(// Full nanosecond precision
        arguments(LocalTime.of(13, 45, 30, 123_456_789), "13:45:30.123456789", "When a proper LocalTime instance with nanos is supplied, the map should contain the ISO‑8601 string."), // Zero‑nanosecond, hour/minute/second only
        arguments(LocalTime.of(12, 0, 0, 0), "12:00", "When nanoseconds are zero, LocalTime.toString() omits the fraction."), // Hour/minute only
        arguments(LocalTime.of(5, 30), "05:30", "LocalTime created with hour and minute only should render as \"05:30\"."), // Boundary MIN
        arguments(LocalTime.MIN, "00:00", "LocalTime.MIN represents midnight without nanos."), // Boundary MAX
        arguments(LocalTime.MAX, "23:59:59.999999999", "LocalTime.MAX is the greatest representable time with full nanosecond precision."), // Example with half‑second fraction
        arguments(LocalTime.of(9, 15, 45, 500_000_000), "09:15:45.500000000", "Fractional seconds are rendered with nine‑digit nanosecond field."));
    }

    @Test
    @DisplayName("toMap should return a map containing the TIME key with the LocalTime string")
    void toMapReturnsCorrectMap() {
        // GIVEN
        LocalTime localTime = LocalTime.of(13, 45, 30);
        // WHEN
        Map<String, Object> result = LocalTimeConversions.toMap(localTime, converter);
        // THEN
        assertNotNull(result, "Resulting map must not be null");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        // MapConversions.TIME is defined as the key used for time values; fallback to literal if unavailable.
        String timeKey = MapConversions.TIME != null ? MapConversions.TIME : "time";
        assertEquals(localTime.toString(), result.get(timeKey), "Map value must match LocalTime.toString()");
    }

    @Test
    @DisplayName("toMap should throw NullPointerException when source object is null")
    void toMapWithNullSourceThrowsException() {
        assertThrows(NullPointerException.class, () -> LocalTimeConversions.toMap(null, converter), "Calling toMap with a null source should throw NPE");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validLocalTimeProvider")
    @DisplayName("toMap returns correct map for various LocalTime values")
    void toMapValidLocalTime(LocalTime input, String expectedString, String _description) {
        // WHEN
        Map<String, Object> result = LocalTimeConversions.toMap(input, converter);
        // THEN
        assertNotNull(result, "Resulting map must not be null");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        assertEquals(expectedString, result.get(timeKey()), "Map value must match LocalTime.toString()");
    }

    @Test
    @DisplayName("toMap should throw ClassCastException when source is not a LocalTime")
    void toMapInvalidSourceTypeThrowsException() {
        String notATime = "13:45";
        assertThrows(ClassCastException.class, () -> LocalTimeConversions.toMap(notATime, converter), "Supplying a non‑LocalTime object must trigger a ClassCastException");
    }

    @Test
    @DisplayName("Non‑null converter instance is ignored by toMap")
    void toMapIgnoresProvidedConverter() {
        // GIVEN a distinct Converter instance (different from the shared one)
        Converter anotherConverter = new Converter(new ConverterOptions());
        LocalTime time = LocalTime.of(9, 15, 45, 500_000_000);
        // WHEN
        Map<String, Object> result = LocalTimeConversions.toMap(time, anotherConverter);
        // THEN
        assertEquals("09:15:45.500000000", result.get(timeKey()), "Result must be identical regardless of the Converter supplied");
    }

    @Test
    @DisplayName("Returned map is mutable and allows adding new entries")
    void toMapReturnedMapIsMutable() {
        LocalTime time = LocalTime.of(22, 0, 0);
        Map<String, Object> result = LocalTimeConversions.toMap(time, converter);
        // The map should be mutable – adding a new entry must not throw.
        assertDoesNotThrow(() -> result.put("extra", "value"), "Map returned by toMap should be mutable");
        assertEquals("value", result.get("extra"), "Newly added entry must be present in the map");
    }
}
