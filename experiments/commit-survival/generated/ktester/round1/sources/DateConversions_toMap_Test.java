package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DateConversions_toMap_Test {

    @Mock
    private Converter mockConverter;

    @Mock
    private ConverterOptions mockOptions;

    @BeforeEach
    void setupBeforeEach() {
        // Configure the mock hierarchy used by the tests
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        // Default zone – individual tests will override as needed
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks after each test to avoid cross‑test interference
        Mockito.reset(mockConverter, mockOptions);
    }

    /**
     * Supplies the data for {@link #testToMapVariousScenarios(Date, ZoneId, Map)}.
     */
    private static Stream<Arguments> provideToMapCases() {
        return Stream.of(// Standard conversion – UTC zone
        Arguments.of(new Date(1672531199000L), ZoneId.of("UTC"), Map.of(MapConversions.DATE, "2022-12-31", MapConversions.TIME, "23:59:59.0", MapConversions.ZONE, "UTC", MapConversions.EPOCH_MILLIS, 1672531199000L)), // Conversion with non‑UTC zone (America/New_York)
        Arguments.of(new Date(1672531199000L), ZoneId.of("America/New_York"), Map.of(MapConversions.DATE, "2022-12-31", MapConversions.TIME, "18:59:59.0", MapConversions.ZONE, "America/New_York", MapConversions.EPOCH_MILLIS, 1672531199000L)), // Epoch zero conversion – UTC
        Arguments.of(new Date(0L), ZoneId.of("UTC"), Map.of(MapConversions.DATE, "1970-01-01", MapConversions.TIME, "00:00", MapConversions.ZONE, "UTC", MapConversions.EPOCH_MILLIS, 0L)), // Milliseconds precision (1234 ms)
        Arguments.of(new Date(1234L), ZoneId.of("UTC"), Map.of(MapConversions.DATE, "1970-01-01", MapConversions.TIME, "00:00:01.234", MapConversions.ZONE, "UTC", MapConversions.EPOCH_MILLIS, 1234L)), // Negative epoch (pre‑1970)
        Arguments.of(new Date(-86400000L), ZoneId.of("UTC"), Map.of(MapConversions.DATE, "1969-12-31", MapConversions.TIME, "00:00", MapConversions.ZONE, "UTC", MapConversions.EPOCH_MILLIS, -86400000L)), // Non‑standard offset zone (+05:30)
        Arguments.of(new Date(0L), ZoneId.of("Asia/Kolkata"), Map.of(MapConversions.DATE, "1970-01-01", MapConversions.TIME, "05:30", MapConversions.ZONE, "Asia/Kolkata", MapConversions.EPOCH_MILLIS, 0L)));
    }

    @Test
    void testToMapProducesCorrectEntries() {
        // GIVEN
        // current instant
        Date now = new Date();
        ZoneId zone = ZoneId.of("UTC");
        when(mockOptions.getZoneId()).thenReturn(zone);
        // WHEN
        Map<String, Object> result = DateConversions.toMap(now, mockConverter);
        // THEN
        assertNotNull(result, "Resulting map must not be null");
        assertEquals(4, result.size(), "Map should contain exactly four entries");
        // DATE entry – ISO‑8601 local date string
        assertEquals(now.toInstant().atZone(zone).toLocalDate().toString(), result.get(MapConversions.DATE), "DATE entry should match the local date part of the ZonedDateTime");
        // TIME entry – ISO‑8601 local time string
        assertEquals(now.toInstant().atZone(zone).toLocalTime().toString(), result.get(MapConversions.TIME), "TIME entry should match the local time part of the ZonedDateTime");
        // ZONE entry – the zone ID string supplied by the ConverterOptions
        assertEquals("UTC", result.get(MapConversions.ZONE), "ZONE entry should reflect the zone ID from ConverterOptions");
        // EPOCH_MILLIS entry – the original Date epoch milliseconds
        assertEquals(now.getTime(), result.get(MapConversions.EPOCH_MILLIS), "EPOCH_MILLIS entry should be the original Date's epoch milliseconds");
    }

    /**
     * Parameterized test covering a variety of normal conversion scenarios:
     * <ul>
     *   <li>UTC and non‑UTC zones</li>
     *   <li>Epoch zero, sub‑second precision, negative epoch</li>
     *   <li>Zones with non‑hour offsets (e.g., Asia/Kolkata)</li>
     * </ul>
     */
    @ParameterizedTest(name = "{index}: zone={1}, date={0}")
    @MethodSource("provideToMapCases")
    void testToMapVariousScenarios(Date from, ZoneId zone, Map<String, Object> expected) {
        // Arrange – configure the mock to return the required zone
        when(mockOptions.getZoneId()).thenReturn(zone);
        // Act
        Map<String, Object> result = DateConversions.toMap(from, mockConverter);
        // Assert – each entry must match the expected value
        assertEquals(expected.get(MapConversions.DATE), result.get(MapConversions.DATE), "DATE entry mismatch");
        assertEquals(expected.get(MapConversions.TIME), result.get(MapConversions.TIME), "TIME entry mismatch");
        assertEquals(expected.get(MapConversions.ZONE), result.get(MapConversions.ZONE), "ZONE entry mismatch");
        assertEquals(expected.get(MapConversions.EPOCH_MILLIS), result.get(MapConversions.EPOCH_MILLIS), "EPOCH_MILLIS entry mismatch");
    }

    /**
     * Verifies that a {@code null} {@code Date} argument results in a
     * {@link NullPointerException}.
     */
    @Test
    void testToMapNullDateThrowsNPE() {
        // GIVEN
        Date nullDate = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> DateConversions.toMap(nullDate, mockConverter), "Passing a null Date should throw NullPointerException");
    }

    /**
     * Verifies that providing an object that is not a {@code java.util.Date}
     * triggers a {@link ClassCastException}.
     */
    @Test
    void testToMapInvalidTypeThrowsClassCastException() {
        // GIVEN
        Object notADate = "2022-01-01";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> DateConversions.toMap(notADate, mockConverter), "Supplying a non‑Date object should throw ClassCastException");
    }

    /**
     * Verifies that a {@code null} {@code Converter} argument results in a
     * {@link NullPointerException} when the method attempts to read options.
     */
    @Test
    void testToMapNullConverterThrowsNPE() {
        // GIVEN
        Date date = new Date(0L);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> DateConversions.toMap(date, null), "Passing a null Converter should throw NullPointerException");
    }
}
