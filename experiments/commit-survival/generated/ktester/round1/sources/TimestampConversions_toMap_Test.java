package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TimestampConversions_toMap_Test {

    @Mock
    private Converter mockConverter;

    @Mock
    private ConverterOptions mockOptions;

    /**
     * Simple holder for a single test case.
     */
    private static final class TestCase {

        final Object from;

        // null means the converter itself is null or options null
        final ZoneId zone;

        // null when an exception is expected
        final Map<String, Object> expectedMap;

        // null when success expected
        final Class<? extends Throwable> expectedException;

        final String description;

        TestCase(Object from, ZoneId zone, Map<String, Object> expectedMap, Class<? extends Throwable> expectedException, String description) {
            this.from = from;
            this.zone = zone;
            this.expectedMap = expectedMap;
            this.expectedException = expectedException;
            this.description = description;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Configure the mock Converter to return a mock ConverterOptions that supplies a known ZoneId
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks after each test to avoid interaction leakage
        reset(mockConverter, mockOptions);
    }

    static Stream<Arguments> successfulConversionsProvider() {
        return Stream.of(// Group: Successful conversion with java.util.Date (UTC zone)
        Arguments.of(new TestCase(new Date(0L), ZoneId.of("UTC"), Map.of(MapConversions.DATE, "1970-01-01", MapConversions.TIME, "00:00", MapConversions.ZONE, "UTC", MapConversions.EPOCH_MILLIS, 0L, MapConversions.NANOS, 0), null, "Convert the epoch start (java.util.Date with time 0) to a map while the converter is configured for the UTC zone.")), // Group: Conversion with java.util.Date in America/New_York zone
        Arguments.of(new TestCase(new Date(0L), ZoneId.of("America/New_York"), Map.of(MapConversions.DATE, "1969-12-31", MapConversions.TIME, "19:00", MapConversions.ZONE, "America/New_York", MapConversions.EPOCH_MILLIS, 0L, MapConversions.NANOS, 0), null, "Convert the epoch instant using a converter configured for America/New_York (UTC‑5).")), // Group: Conversion with java.sql.Timestamp containing nanoseconds (UTC zone)
        Arguments.of(new TestCase(Timestamp.from(Instant.ofEpochSecond(0, 123_456_789)), ZoneId.of("UTC"), Map.of(MapConversions.DATE, "1970-01-01", MapConversions.TIME, "00:00:00.123456789", MapConversions.ZONE, "UTC", MapConversions.EPOCH_MILLIS, 0L, MapConversions.NANOS, 123_456_789), null, "Timestamp at epoch with nanoseconds; NANOS field must reflect the nanosecond part.")), // Group: Conversion with custom ZoneOffset (+05:30) and epoch start
        Arguments.of(new TestCase(new Date(0L), ZoneOffset.ofHoursMinutes(5, 30), Map.of(MapConversions.DATE, "1970-01-01", MapConversions.TIME, "05:30", MapConversions.ZONE, "+05:30", MapConversions.EPOCH_MILLIS, 0L, MapConversions.NANOS, 0), null, "Fixed offset +05:30 should represent the epoch instant as 05:30 on the same day.")), // Group: Conversion with Date before epoch (negative millis) in UTC
        Arguments.of(new TestCase(// one day before epoch
        new Date(-86_400_000L), ZoneId.of("UTC"), Map.of(MapConversions.DATE, "1969-12-31", MapConversions.TIME, "00:00", MapConversions.ZONE, "UTC", MapConversions.EPOCH_MILLIS, -86_400_000L, MapConversions.NANOS, 0), null, "Date exactly one day before epoch should yield previous calendar date.")));
    }

    static Stream<Arguments> exceptionCasesProvider() {
        return Stream.of(// Group: Conversion with null Date object
        Arguments.of(new TestCase(null, ZoneId.of("UTC"), null, NullPointerException.class, "Passing a null Date should result in a NullPointerException.")), // Group: Conversion with non‑Date input (String)
        Arguments.of(new TestCase("2021-01-01", ZoneId.of("UTC"), null, ClassCastException.class, "Providing a non‑Date object triggers a ClassCastException.")), // Group: Conversion with converter set to null
        Arguments.of(new TestCase(new Date(0L), // zone is irrelevant because converter itself is null
        null, null, NullPointerException.class, "If the Converter argument is null, a NullPointerException is expected.")), // Group: Conversion with converter having null options
        Arguments.of(new TestCase(new Date(0L), // we will make getOptions() return null
        ZoneId.of("UTC"), null, NullPointerException.class, "Converter returns null for getOptions(), leading to NPE.")));
    }

    @Test
    void testToMapProducesCorrectEntries() {
        // GIVEN: a deterministic Date instance
        // 2021-10-01T00:00:00Z
        long epochMillis = 1_632_072_800_000L;
        Date date = new Date(epochMillis);
        // WHEN: the focal method is invoked
        Map<String, Object> result = TimestampConversions.toMap(date, mockConverter);
        // THEN: verify the map contains the expected keys and values
        assertNotNull(result, "Resulting map should not be null");
        assertEquals(5, result.size(), "Map should contain exactly 5 entries");
        // Compute the expected OffsetDateTime using the same zone the mock provides
        OffsetDateTime expectedOdt = OffsetDateTime.ofInstant(date.toInstant(), ZoneId.of("UTC"));
        assertEquals(expectedOdt.toLocalDate().toString(), result.get(MapConversions.DATE), "DATE entry should match the local date string");
        assertEquals(expectedOdt.toLocalTime().toString(), result.get(MapConversions.TIME), "TIME entry should match the local time string");
        assertEquals("UTC", result.get(MapConversions.ZONE), "ZONE entry should reflect the mocked ZoneId");
        assertEquals(epochMillis, result.get(MapConversions.EPOCH_MILLIS), "EPOCH_MILLIS entry should match the original date's epoch milliseconds");
        assertEquals(expectedOdt.getNano(), result.get(MapConversions.NANOS), "NANOS entry should match the nanosecond part of the OffsetDateTime");
    }

    @ParameterizedTest(name = "{index}: {0.description}")
    @MethodSource("successfulConversionsProvider")
    void testToMapSuccessful(TestCase tc) {
        // Arrange -----------------------------------------------------------
        if (tc.zone != null) {
            when(mockConverter.getOptions()).thenReturn(mockOptions);
            when(mockOptions.getZoneId()).thenReturn(tc.zone);
        }
        // Act ---------------------------------------------------------------
        Map<String, Object> result = TimestampConversions.toMap(tc.from, mockConverter);
        // Assert -------------------------------------------------------------
        assertNotNull(result, "Result map must not be null");
        assertEquals(tc.expectedMap.size(), result.size(), "Result map should contain exactly " + tc.expectedMap.size() + " entries");
        // Verify each expected entry
        tc.expectedMap.forEach((key, expectedValue) -> {
            assertTrue(result.containsKey(key), "Result map should contain key: " + key);
            assertEquals(expectedValue, result.get(key), "Value for key '" + key + "' does not match expected.");
        });
    }

    @ParameterizedTest(name = "{index}: {0.description}")
    @MethodSource("exceptionCasesProvider")
    void testToMapExceptions(TestCase tc) {
        // Arrange -----------------------------------------------------------
        if (tc.zone != null) {
            when(mockConverter.getOptions()).thenReturn(mockOptions);
            when(mockOptions.getZoneId()).thenReturn(tc.zone);
        } else if (tc.expectedException == NullPointerException.class && tc.from != null) {
            // Specific case: converter is null
            // No stubbing for mockConverter – we will pass null directly
        }
        // Special handling for "converter having null options"
        if (tc.description.contains("null options")) {
            when(mockConverter.getOptions()).thenReturn(null);
        }
        // Act & Assert -------------------------------------------------------
        if (tc.expectedException != null) {
            assertThrows(tc.expectedException, () -> TimestampConversions.toMap(tc.from, tc.zone == null ? null : mockConverter), "Expected exception: " + tc.expectedException.getSimpleName());
        } else {
            fail("Test case configuration error – expectedException should not be null for this provider.");
        }
    }
}
