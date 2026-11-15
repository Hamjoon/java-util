package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.Timestamp;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TimestampConversions_toOffsetDateTime_Test {

    private Converter converter;

    private Timestamp timestamp;

    /**
     * Minimal stub for {@code ConverterOptions}.
     */
    static final class ConverterOptions {

        private final ZoneId zoneId;

        ConverterOptions(ZoneId zoneId) {
            this.zoneId = zoneId;
        }

        ZoneId getZoneId() {
            return zoneId;
        }
    }

    /**
     * Minimal stub for {@code Converter}.
     */
    static final class ConverterStub {

        private final ConverterOptions options;

        private ConverterStub(ConverterOptions options) {
            this.options = options;
        }

        ConverterOptions getOptions() {
            return options;
        }

        /**
         * Creates a stub whose {@code getOptions().getZoneId()} returns the supplied zone.
         */
        static ConverterStub withZoneId(ZoneId zoneId) {
            return new ConverterStub(new ConverterOptions(zoneId));
        }

        /**
         * Creates a stub whose {@code getOptions()} returns {@code null}.
         */
        static ConverterStub withNullOptions() {
            return new ConverterStub(null);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use default options – they provide a ZoneId (system default if none is set)
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
        // Create a deterministic Timestamp (e.g., 2023‑03‑15T12:34:56.789Z)
        // corresponds to 2023-03-15T12:34:56.789Z
        long epochMilli = 1678886096789L;
        timestamp = new Timestamp(epochMilli);
    }

    /**
     * Provides all successful conversion scenarios.
     */
    private static Stream<Arguments> validConversionsProvider() {
        return Stream.of(// Valid Conversions - UTC Zone
        Arguments.of("UTC zone – preserve nanoseconds", Timestamp.valueOf("2023-03-25 12:34:56.123456789"), ConverterStub.withZoneId(ZoneId.of("UTC")), OffsetDateTime.parse("2023-03-25T12:34:56.123456789Z")), // Valid Conversions - Non‑UTC Zone
        Arguments.of("America/New_York (EDT) – offset -04:00", Timestamp.valueOf("2023-07-01 15:00:00.000000000"), ConverterStub.withZoneId(ZoneId.of("America/New_York")), OffsetDateTime.parse("2023-07-01T11:00:00-04:00")), // Different Offsets (Asia/Tokyo)
        Arguments.of("Asia/Tokyo (+09:00) with date roll‑over", Timestamp.valueOf("2022-12-31 23:00:00.500000000"), ConverterStub.withZoneId(ZoneId.of("Asia/Tokyo")), OffsetDateTime.parse("2023-01-01T08:00:00.500+09:00")), // Edge Cases - Epoch Start UTC
        Arguments.of("Epoch start UTC", Timestamp.valueOf("1970-01-01 00:00:00.0"), ConverterStub.withZoneId(ZoneId.of("UTC")), OffsetDateTime.parse("1970-01-01T00:00:00Z")), // Edge Cases - Epoch Start Europe/Paris
        Arguments.of("Epoch start Europe/Paris (+01:00)", Timestamp.valueOf("1970-01-01 00:00:00.0"), ConverterStub.withZoneId(ZoneId.of("Europe/Paris")), OffsetDateTime.parse("1970-01-01T01:00:00+01:00")), // Historical Date Before 1970 – UTC
        Arguments.of("Historical UTC – 1965 with nanos", Timestamp.valueOf("1965-06-15 08:30:00.123456789"), ConverterStub.withZoneId(ZoneId.of("UTC")), OffsetDateTime.parse("1965-06-15T08:30:00.123456789Z")), // Historical Date Before 1970 – America/New_York
        Arguments.of("Historical America/New_York – 1965 offset -04:00", Timestamp.valueOf("1965-06-15 08:30:00.123456789"), ConverterStub.withZoneId(ZoneId.of("America/New_York")), OffsetDateTime.parse("1965-06-15T04:30:00.123456789-04:00")), // Far Future Date with Extreme Positive Offset
        Arguments.of("Future date – Pacific/Kiritimati (+14:00)", Timestamp.valueOf("2100-01-01 12:00:00.0"), ConverterStub.withZoneId(ZoneId.of("Pacific/Kiritimati")), OffsetDateTime.parse("2100-01-02T02:00:00+14:00")), // Fixed Offset Zone (UTC+05:30)
        Arguments.of("Fixed offset UTC+05:30", Timestamp.valueOf("2023-01-01 00:00:00.0"), ConverterStub.withZoneId(ZoneOffset.ofHoursMinutes(5, 30)), OffsetDateTime.parse("2023-01-01T05:30:00+05:30")), // DST Transition - Spring Forward (America/New_York)
        Arguments.of("DST spring forward – New York", Timestamp.valueOf("2021-03-14 07:00:00.0"), ConverterStub.withZoneId(ZoneId.of("America/New_York")), OffsetDateTime.parse("2021-03-14T03:00:00-04:00")), // DST Transition - Fall Back (America/New_York)
        Arguments.of("DST fall back – New York", Timestamp.valueOf("2021-11-07 07:30:00.0"), ConverterStub.withZoneId(ZoneId.of("America/New_York")), OffsetDateTime.parse("2021-11-07T02:30:00-05:00")), // Maximum Nanosecond Fraction
        Arguments.of("Maximum nanosecond fraction – UTC", Timestamp.valueOf("2023-05-10 23:59:59.999999999"), ConverterStub.withZoneId(ZoneId.of("UTC")), OffsetDateTime.parse("2023-05-10T23:59:59.999999999Z")), // Zero Nanosecond Fraction with DST Zone (Europe/London)
        Arguments.of("Zero nanos – London summer time (+01:00)", Timestamp.valueOf("2023-05-10 12:00:00.0"), ConverterStub.withZoneId(ZoneId.of("Europe/London")), OffsetDateTime.parse("2023-05-10T13:00:00+01:00")));
    }

    /**
     * Provides all scenarios that must throw an exception.
     */
    private static Stream<Arguments> invalidConversionsProvider() {
        return Stream.of(// Invalid Input - Null Timestamp
        Arguments.of("Null Timestamp argument", null, ConverterStub.withZoneId(ZoneId.of("UTC")), NullPointerException.class), // Invalid Input - Wrong Type
        Arguments.of("Wrong source type (java.util.Date)", new java.util.Date(), ConverterStub.withZoneId(ZoneId.of("UTC")), ClassCastException.class), // Invalid Input - Converter with Null ZoneId
        Arguments.of("Converter with null ZoneId", Timestamp.valueOf("2023-01-01 00:00:00.0"), ConverterStub.withZoneId(null), NullPointerException.class), // Invalid Input - Converter is Null
        Arguments.of("Null Converter argument", Timestamp.valueOf("2023-01-01 00:00:00.0"), null, NullPointerException.class), // Invalid Input - Converter Options Null
        Arguments.of("Converter with null options", Timestamp.valueOf("2023-01-01 00:00:00.0"), ConverterStub.withNullOptions(), NullPointerException.class));
    }

    @Test
    void toOffsetDateTime_convertsTimestampUsingConverterZone() {
        // GIVEN – a Timestamp and a Converter with a known ZoneId
        // usually system default
        ZoneId expectedZone = converter.getOptions().getZoneId();
        // WHEN
        OffsetDateTime result = TimestampConversions.toOffsetDateTime(timestamp, converter);
        // THEN
        assertNotNull(result, "Resulting OffsetDateTime should not be null");
        // The instant part must match the original Timestamp instant
        assertEquals(timestamp.toInstant(), result.toInstant(), "OffsetDateTime instant should equal the Timestamp instant");
        // The offset must correspond to the Converter's ZoneId
        assertEquals(expectedZone.getRules().getOffset(timestamp.toInstant()), result.getOffset(), "Offset should match the Converter's ZoneId offset");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversionsProvider")
    void toOffsetDateTime_validConversions(String description, Timestamp from, ConverterStub converter, OffsetDateTime expected) {
        // WHEN
        OffsetDateTime result = TimestampConversions.toOffsetDateTime(from, converter);
        // THEN
        assertNotNull(result, "Result must not be null");
        assertEquals(expected.toInstant(), result.toInstant(), "Instant (epoch second + nanos) must match expected");
        assertEquals(expected.getOffset(), result.getOffset(), "Offset must match the expected zone offset");
        assertEquals(expected, result, "Full OffsetDateTime (date, time, nanos, offset) must be equal");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidConversionsProvider")
    void toOffsetDateTime_invalidInputs(String description, Object from, ConverterStub converter, Class<? extends Throwable> expectedException) {
        // WHEN / THEN
        assertThrows(expectedException, () -> TimestampConversions.toOffsetDateTime(from, converter), "Expected " + expectedException.getSimpleName() + " for: " + description);
    }
}
