package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;
import static com.cedarsoftware.util.convert.MapConversions.DATE;
import static com.cedarsoftware.util.convert.MapConversions.TIME;
import static com.cedarsoftware.util.convert.MapConversions.ZONE;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.time.zone.ZoneRules;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class ZonedDateTimeConversions_toMap_Test {

    @Mock
    private Converter // not used by the method but required by signature
    mockConverter;

    /**
     * Minimal {@link ZoneId} implementation that throws a RuntimeException
     * when {@link #getId()} is called. Used to verify exception propagation.
     */
    private static final class ThrowingZoneId extends ZoneId {

        @Override
        public String getId() {
            throw new RuntimeException("ZoneId failure");
        }

        @Override
        public ZoneRules getRules() {
            // Return a dummy rules object – it will never be used in this test.
            return ZoneRules.of(0, 0);
        }
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> validZonedDateTimeProvider() {
        return Stream.of(// Valid ZonedDateTime conversion – typical case
        arguments(ZonedDateTime.of(2023, 5, 10, 14, 30, 0, 0, ZoneId.of("America/New_York")), "2023-05-10", "14:30", "America/New_York", "Typical ZonedDateTime conversion"), // UTC zone (ZoneOffset.UTC) – zone string is "Z"
        arguments(ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), "2000-01-01", "00:00", "Z", "UTC zone handling"), // Positive offset with nanosecond precision
        arguments(ZonedDateTime.of(2022, 12, 31, 23, 59, 59, 999_999_999, ZoneId.of("Asia/Tokyo")), "2022-12-31", "23:59:59.999999999", "Asia/Tokyo", "Nanosecond precision in positive offset zone"), // Negative offset zone (America/Los_Angeles)
        arguments(ZonedDateTime.of(2023, 12, 25, 6, 15, 30, 123_000_000, ZoneId.of("America/Los_Angeles")), "2023-12-25", "06:15:30.123", "America/Los_Angeles", "Negative offset zone handling"), // Fixed offset ZoneId (‑05:30)
        arguments(ZonedDateTime.of(2024, 2, 29, 23, 59, 59, 0, ZoneOffset.ofHoursMinutes(-5, -30)), "2024-02-29", "23:59:59", "-05:30", "Fixed offset ZoneId"), // Zero nanosecond precision – fractional part omitted
        arguments(ZonedDateTime.of(2021, 7, 1, 12, 0, 0, 0, ZoneId.of("UTC")), "2021-07-01", "12:00", "Z", "Zero nanosecond precision"), // Extreme future year
        arguments(ZonedDateTime.of(9999, 12, 31, 23, 59, 59, 999_999_999, ZoneId.of("UTC")), "9999-12-31", "23:59:59.999999999", "Z", "Extreme future year"), // Conversion from Instant‑based ZonedDateTime (epoch)
        arguments(ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.of("UTC")), "1970-01-01", "00:00", "Z", "Instant‑based ZonedDateTime (epoch)"));
    }

    private static Stream<Object> invalidFromProvider() {
        return Stream.of(// String
        "2023-05-10T14:30:00Z", // java.util.Date
        Date.from(Instant.now()));
    }

    @Test
    void toMap_ShouldReturnMapWithDateTimeAndZoneStrings() {
        // GIVEN
        ZonedDateTime input = ZonedDateTime.of(// year, month, day
        2023, // year, month, day
        5, // year, month, day
        17, // hour, minute, second, nanosecond
        14, // hour, minute, second, nanosecond
        30, // hour, minute, second, nanosecond
        45, // hour, minute, second, nanosecond
        123_000_000, ZoneId.of("America/New_York"));
        // WHEN
        Map<String, Object> result = ZonedDateTimeConversions.toMap(input, mockConverter);
        // THEN
        assertNotNull(result, "Resulting map must not be null");
        assertEquals(3, result.size(), "Map should contain exactly three entries");
        // Verify each expected key/value
        assertEquals("2023-05-17", result.get(DATE), "DATE key should contain the ISO local date");
        assertEquals("14:30:45.123", result.get(TIME), "TIME key should contain the ISO local time with nanoseconds trimmed");
        assertEquals("America/New_York", result.get(ZONE), "ZONE key should contain the zone id");
    }

    /* --------------------------------------------------------------
     *  Parameterized tests for all “valid conversion” scenarios.
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("validZonedDateTimeProvider")
    void toMap_ValidConversions_ReturnsExpectedMap(ZonedDateTime input, String expectedDate, String expectedTime, String expectedZone, String description) {
        // WHEN
        Map<String, Object> result = ZonedDateTimeConversions.toMap(input, mockConverter);
        // THEN
        assertNotNull(result);
        assertEquals(3, result.size(), "Map should contain exactly three entries");
        assertEquals(expectedDate, result.get(DATE), "DATE value mismatch");
        assertEquals(expectedTime, result.get(TIME), "TIME value mismatch");
        assertEquals(expectedZone, result.get(ZONE), "ZONE value mismatch");
    }

    /* --------------------------------------------------------------
     *  Null `from` argument – should throw NullPointerException.
     * -------------------------------------------------------------- */
    @Test
    void toMap_NullFrom_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toMap(null, mockConverter), "Passing null for `from` must raise NPE");
    }

    /* --------------------------------------------------------------
     *  Invalid `from` type – should throw ClassCastException.
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] Invalid type: {0}")
    @MethodSource("invalidFromProvider")
    void toMap_InvalidFromType_ThrowsClassCastException(Object invalidFrom) {
        assertThrows(ClassCastException.class, () -> ZonedDateTimeConversions.toMap(invalidFrom, mockConverter), "Supplying a non‑ZonedDateTime should raise ClassCastException");
    }

    /* --------------------------------------------------------------
     *  Verify that the returned map is a CompactLinkedMap instance.
     * -------------------------------------------------------------- */
    @Test
    void toMap_ReturnedMap_IsCompactLinkedMap() {
        ZonedDateTime input = ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        Map<String, Object> result = ZonedDateTimeConversions.toMap(input, mockConverter);
        assertTrue(result instanceof CompactLinkedMap, "Implementation must return a CompactLinkedMap");
    }

    /* --------------------------------------------------------------
     *  Converter argument is ignored – ensure no interaction.
     * -------------------------------------------------------------- */
    @Test
    void toMap_ConverterIgnored_DoesNotInvokeConverter() {
        // A Converter that throws on any call – if the method ever uses it the test will fail.
        Converter explodingConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> dest) {
                throw new AssertionError("Converter should not be used");
            }
        };
        ZonedDateTime input = ZonedDateTime.of(2022, 3, 15, 9, 45, 0, 0, ZoneId.of("Europe/Paris"));
        Map<String, Object> result = ZonedDateTimeConversions.toMap(input, explodingConverter);
        assertEquals("2022-03-15", result.get(DATE));
        assertEquals("09:45", result.get(TIME));
        assertEquals("Europe/Paris", result.get(ZONE));
    }

    /* --------------------------------------------------------------
     *  Custom ZoneId whose toString/getId throws – exception should propagate.
     * -------------------------------------------------------------- */
    @Test
    void toMap_CustomZoneIdThrows_RuntimeExceptionPropagated() {
        ZoneId throwingZoneId = new ThrowingZoneId();
        ZonedDateTime input = ZonedDateTime.of(2023, 1, 1, 0, 0, 0, 0, throwingZoneId);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ZonedDateTimeConversions.toMap(input, mockConverter), "Exception from custom ZoneId should propagate");
        assertEquals("ZoneId failure", ex.getMessage());
    }
}
