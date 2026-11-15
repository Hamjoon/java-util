package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.ZoneId;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DateConversions_toMap_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter converter;

    /**
     * Minimal interface representing the options object used by {@link Converter}.
     * The real library defines a concrete class with a {@code getZoneId()} method.
     * Declaring it here allows the test to compile without pulling in the full library.
     */
    interface Options {

        ZoneId getZoneId();
    }

    /**
     * Set up a real {@link Converter} with default options once for the whole test class.
     */
    @BeforeAll
    static void setupBeforeAll() {
        // Use the default options – they contain a ZoneId (usually the system default).
        // If you need a deterministic zone you could configure ConverterOptions accordingly.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the shared converter reference.
        converter = null;
    }

    /**
     * Helper to create a mock {@link Converter} that returns the supplied {@link ZoneId}.
     */
    private static Converter mockConverterWithZone(ZoneId zone) {
        // Mock the Options object (the real type is {@code ConverterOptions} or {@code Options}).
        // We only need the {@code getZoneId()} method.
        Object optionsMock = mock(Object.class, withSettings().extraInterfaces(Options.class));
        Options opts = (Options) optionsMock;
        when(opts.getZoneId()).thenReturn(zone);
        // Mock the Converter to return the mocked Options.
        Converter converter = mock(Converter.class);
        when(converter.getOptions()).thenReturn(opts);
        return converter;
    }

    /**
     * Provider for the valid conversion cases.
     */
    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// 1. Epoch instant with UTC zone
        Arguments.of("Epoch Date (0 ms) with UTC zone", new Date(0L), ZoneId.of("UTC"), Map.of()), // 2. Specific Timestamp with America/New_York zone
        Arguments.of("Timestamp 2023‑03‑15T12:34:56.789Z with America/New_York", new java.sql.Timestamp(java.time.Instant.parse("2023-03-15T12:34:56.789Z").toEpochMilli()), ZoneId.of("America/New_York"), Map.of()), // 3. Last second of 2022 with Asia/Tokyo zone
        Arguments.of("Date 2022‑12‑31T23:59:59Z (epoch 1672531199000) with Asia/Tokyo", new Date(1672531199000L), ZoneId.of("Asia/Tokyo"), Map.of()), // 4. Negative epoch (one day before Unix epoch) with UTC
        Arguments.of("Date -86400000 ms (1969‑12‑31) with UTC", new Date(-86400000L), ZoneId.of("UTC"), Map.of()), // 5. Fractional seconds (12.345 s after epoch) with UTC
        Arguments.of("Date 12345 ms (12.345 s) with UTC", new Date(12345L), ZoneId.of("UTC"), Map.of()), // 6. Epoch with Asia/Kolkata (+05:30) zone
        Arguments.of("Epoch Date with Asia/Kolkata (+05:30)", new Date(0L), ZoneId.of("Asia/Kolkata"), Map.of()), // 7. Epoch with fixed offset UTC‑02:00
        Arguments.of("Epoch Date with UTC‑02:00", new Date(0L), ZoneId.of("UTC-02:00"), Map.of()));
    }

    /**
     * Provider for exception‑related cases.
     */
    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(// Null 'from' argument
        Arguments.of("Null `from` argument", null, mockConverterWithZone(ZoneId.of("UTC")), NullPointerException.class), // Non‑Date object as 'from'
        Arguments.of("`from` is a String (not a Date)", "not a date", mockConverterWithZone(ZoneId.of("UTC")), ClassCastException.class), // Null converter
        Arguments.of("Null `converter` argument", new Date(0L), null, NullPointerException.class), // Converter whose getOptions() returns null
        Arguments.of("Converter with null options", new Date(0L), mockConverterWithNullOptions(), NullPointerException.class), // Converter whose options contain a null ZoneId
        Arguments.of("Converter with options having null ZoneId", new Date(0L), mockConverterWithNullZoneId(), NullPointerException.class));
    }

    /**
     * Helper that returns a mock Converter whose {@code getOptions()} returns {@code null}.
     */
    private static Converter mockConverterWithNullOptions() {
        Converter converter = mock(Converter.class);
        when(converter.getOptions()).thenReturn(null);
        return converter;
    }

    /**
     * Helper that returns a mock Converter whose options return a null {@code ZoneId}.
     */
    private static Converter mockConverterWithNullZoneId() {
        // Mock Options to return null ZoneId
        Object optionsMock = mock(Object.class, withSettings().extraInterfaces(Options.class));
        Options opts = (Options) optionsMock;
        when(opts.getZoneId()).thenReturn(null);
        Converter converter = mock(Converter.class);
        when(converter.getOptions()).thenReturn(opts);
        return converter;
    }

    /**
     * Verifies that {@code DateConversions.toMap} returns a map with
     * the correct date, time, zone and epoch‑millis entries.
     */
    @Test
    void testToMapProducesExpectedEntries() {
        // GIVEN – a known instant (epoch) and a fresh Converter
        // 1970‑01‑01T00:00:00Z
        Date date = new Date(0L);
        ZonedDateTime expectedZdt = DateConversions.toZonedDateTime(date, converter);
        // WHEN – the focal method is invoked
        Map<String, Object> result = DateConversions.toMap(date, converter);
        // THEN – the map contains exactly the four expected entries
        assertNotNull(result, "Resulting map must not be null");
        assertEquals(4, result.size(), "Map should contain exactly four entries");
        // DATE – local date part of the ZonedDateTime
        assertEquals(expectedZdt.toLocalDate().toString(), result.get(MapConversions.DATE), "DATE entry should match the local date string");
        // TIME – local time part of the ZonedDateTime
        assertEquals(expectedZdt.toLocalTime().toString(), result.get(MapConversions.TIME), "TIME entry should match the local time string");
        // ZONE – the zone ID configured in the Converter's options
        assertEquals(converter.getOptions().getZoneId().toString(), result.get(MapConversions.ZONE), "ZONE entry should match the Converter's zone ID");
        // EPOCH_MILLIS – the original epoch milliseconds of the Date
        assertEquals(date.getTime(), result.get(MapConversions.EPOCH_MILLIS), "EPOCH_MILLIS entry should match the original Date time value");
    }

    /**
     * --------------------------------------------------------------------- *
     *  Valid conversion test cases (parameterized).                         *
     *  ---------------------------------------------------------------------
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validConversionProvider")
    void testToMapValidConversions(String description, Date date, ZoneId zone, Map<String, Object> expected) {
        // GIVEN
        Converter converter = mockConverterWithZone(zone);
        ZonedDateTime expectedZdt = DateConversions.toZonedDateTime(date, converter);
        // WHEN
        Map<String, Object> result = DateConversions.toMap(date, converter);
        // THEN
        assertNotNull(result, "Resulting map must not be null");
        assertEquals(4, result.size(), "Map must contain exactly four entries");
        // DATE – local date part
        assertEquals(expectedZdt.toLocalDate().toString(), result.get(MapConversions.DATE), "DATE entry should match the local date string");
        // TIME – local time part (no offset, as per implementation)
        assertEquals(expectedZdt.toLocalTime().toString(), result.get(MapConversions.TIME), "TIME entry should match the local time string");
        // ZONE – zone id string from the converter
        assertEquals(zone.toString(), result.get(MapConversions.ZONE), "ZONE entry should match the converter's zone id");
        // EPOCH_MILLIS – original epoch milliseconds
        assertEquals(date.getTime(), result.get(MapConversions.EPOCH_MILLIS), "EPOCH_MILLIS entry should match the original Date time value");
    }

    /**
     * --------------------------------------------------------------------- *
     *  Null and type‑safety test cases (parameterized).                     *
     *  ---------------------------------------------------------------------
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("exceptionProvider")
    void testToMapExceptions(String description, Object from, Converter converter, Class<? extends Throwable> expectedException) {
        // WHEN / THEN
        assertThrows(expectedException, () -> DateConversions.toMap(from, converter), "Expected " + expectedException.getSimpleName() + " for case: " + description);
    }
}
