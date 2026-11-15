package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.HashMap;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimestampConversions_toMap_Test {

    // the Converter instance used in the tests
    private Converter converter;

    // a deterministic Timestamp for verification
    private Timestamp testTimestamp;

    @BeforeEach
    void setupBeforeEach() {
        // Create a deterministic timestamp: 2023‑08‑15T12:34:56.123456789Z
        Instant instant = Instant.parse("2023-08-15T12:34:56.123456789Z");
        testTimestamp = Timestamp.from(instant);
        // Initialise a Converter with a known zone (UTC) to avoid platform‑dependent results
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(ZoneId.of("UTC"));
        converter = new Converter(options);
    }

    /**
     * Supplies the data for {@link #toMap_normalConversions(Timestamp, ZoneId, Map)}.
     *
     * @return a stream of arguments: timestamp, zoneId, expected map
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> normalConversionProvider() {
        // case 1 – UTC, nanosecond precision
        Timestamp ts1 = Timestamp.valueOf("2023-03-15 10:20:30.123456789");
        Map<String, Object> exp1 = new HashMap<>();
        exp1.put(MapConversions.DATE, "2023-03-15");
        exp1.put(MapConversions.TIME, "10:20:30.123456789");
        exp1.put(MapConversions.ZONE, "UTC");
        exp1.put(MapConversions.EPOCH_MILLIS, 1678879230123L);
        exp1.put(MapConversions.NANOS, 123456789);
        // case 2 – America/New_York (UTC‑05:00), no nanoseconds
        // 2022‑12‑31 23:59:59 UTC
        Timestamp ts2 = new Timestamp(1672531199000L);
        Map<String, Object> exp2 = new HashMap<>();
        exp2.put(MapConversions.DATE, "2022-12-31");
        // shifted 5 hours earlier
        exp2.put(MapConversions.TIME, "18:59:59");
        exp2.put(MapConversions.ZONE, "America/New_York");
        exp2.put(MapConversions.EPOCH_MILLIS, 1672531199000L);
        exp2.put(MapConversions.NANOS, 0);
        return Stream.of(org.junit.jupiter.params.provider.Arguments.of(ts1, ZoneId.of("UTC"), exp1), org.junit.jupiter.params.provider.Arguments.of(ts2, ZoneId.of("America/New_York"), exp2));
    }

    /**
     * Helper that creates a Mockito mock of {@link Converter} whose {@code getOptions()}
     * returns a mock {@link ConverterOptions} yielding the supplied {@code zoneId}.
     */
    private static Converter mockConverterWithZone(ZoneId zoneId) {
        ConverterOptions mockOptions = mock(ConverterOptions.class);
        when(mockOptions.getZoneId()).thenReturn(zoneId);
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        return mockConverter;
    }

    @Test
    void toMap_returnsCorrectMapContents() {
        // GIVEN – the testTimestamp and a Converter configured with UTC (handled in @BeforeEach)
        // WHEN
        Map<String, Object> result = TimestampConversions.toMap(testTimestamp, converter);
        // THEN – basic sanity checks
        assertNotNull(result, "Resulting map must not be null");
        assertEquals(5, result.size(), "Map must contain exactly five entries");
        // Expected OffsetDateTime derived by the same internal method used by TimestampConversions
        OffsetDateTime expectedOdt = TimestampConversions.toOffsetDateTime(testTimestamp, converter);
        // DATE
        assertEquals(expectedOdt.toLocalDate().toString(), result.get(MapConversions.DATE), "DATE entry must match the local date part of the OffsetDateTime");
        // TIME
        assertEquals(expectedOdt.toLocalTime().toString(), result.get(MapConversions.TIME), "TIME entry must match the local time part of the OffsetDateTime");
        // ZONE
        assertEquals(converter.getOptions().getZoneId().toString(), result.get(MapConversions.ZONE), "ZONE entry must reflect the Converter's configured ZoneId");
        // EPOCH_MILLIS
        assertEquals(testTimestamp.getTime(), result.get(MapConversions.EPOCH_MILLIS), "EPOCH_MILLIS entry must be the raw millisecond value of the Timestamp");
        // NANOS
        assertEquals(expectedOdt.getNano(), result.get(MapConversions.NANOS), "NANOS entry must be the nanosecond component of the OffsetDateTime");
    }

    /**
     * Parameterised test that checks the map content for a variety of timestamps
     * and converter zones.
     *
     * @param timestamp the {@link Timestamp} supplied to {@code toMap}
     * @param zoneId    the {@link ZoneId} that the {@link Converter} is configured with
     * @param expected  a map containing the expected key/value pairs
     */
    @ParameterizedTest(name = "[{index}] zone={1}")
    @MethodSource("normalConversionProvider")
    void toMap_normalConversions(Timestamp timestamp, ZoneId zoneId, Map<String, Object> expected) {
        // GIVEN – a mock Converter that returns the supplied zoneId via its options
        Converter converter = mockConverterWithZone(zoneId);
        // WHEN
        Map<String, Object> result = TimestampConversions.toMap(timestamp, converter);
        // THEN – the map must contain exactly the expected entries
        assertNotNull(result, "Resulting map must not be null");
        assertEquals(expected.size(), result.size(), "Resulting map must contain exactly " + expected.size() + " entries");
        // verify each key/value pair
        expected.forEach((key, value) -> assertEquals(value, result.get(key), () -> "Map entry for key '" + key + "' does not match expected value"));
    }

    /**
     * Verifies that {@link TimestampConversions#toMap(Object, Converter)} throws the
     * appropriate unchecked exception for each illegal argument combination.
     */
    @Test
    void toMap_exceptionScenarios() {
        // a valid timestamp used for the cases that need a non‑null 'from'
        Timestamp validTimestamp = Timestamp.valueOf("2023-03-15 10:20:30.123456789");
        // -----------------------------------------------------------------
        // 1. Wrong type – should raise ClassCastException
        // -----------------------------------------------------------------
        Converter converterUtc = mockConverterWithZone(ZoneId.of("UTC"));
        assertThrows(ClassCastException.class, () -> TimestampConversions.toMap("not a timestamp", converterUtc), "Providing a non‑Timestamp should cause ClassCastException");
        // -----------------------------------------------------------------
        // 2. Null 'from' – should raise NullPointerException when the method
        //    attempts to dereference the casted Timestamp.
        // -----------------------------------------------------------------
        assertThrows(NullPointerException.class, () -> TimestampConversions.toMap(null, converterUtc), "Null 'from' argument must trigger NullPointerException");
        // -----------------------------------------------------------------
        // 3. Null converter – dereferencing converter.getOptions() must NPE.
        // -----------------------------------------------------------------
        assertThrows(NullPointerException.class, () -> TimestampConversions.toMap(validTimestamp, null), "Null converter must trigger NullPointerException");
        // -----------------------------------------------------------------
        // 4. Converter with null options – getOptions() returns null.
        // -----------------------------------------------------------------
        Converter converterNullOptions = mock(Converter.class);
        when(converterNullOptions.getOptions()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> TimestampConversions.toMap(validTimestamp, converterNullOptions), "Converter returning null options must trigger NullPointerException");
        // -----------------------------------------------------------------
        // 5. Converter whose options have a null ZoneId.
        // -----------------------------------------------------------------
        ConverterOptions optionsNullZone = mock(ConverterOptions.class);
        when(optionsNullZone.getZoneId()).thenReturn(null);
        Converter converterNullZone = mock(Converter.class);
        when(converterNullZone.getOptions()).thenReturn(optionsNullZone);
        assertThrows(NullPointerException.class, () -> TimestampConversions.toMap(validTimestamp, converterNullZone), "Converter with null ZoneId must trigger NullPointerException");
    }
}
