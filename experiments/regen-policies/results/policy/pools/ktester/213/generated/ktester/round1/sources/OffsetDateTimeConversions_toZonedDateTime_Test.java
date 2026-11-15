package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class OffsetDateTimeConversions_toZonedDateTime_Test {

    // the Converter instance used in the tests
    private Converter converter;

    /**
     * Minimal stub for {@link Converter}.
     */
    private static class StubConverter extends Converter {

        private final ConverterOptions options;

        StubConverter(ZoneId zone) {
            super(new ConverterOptions(zone));
            this.options = super.getOptions();
        }

        StubConverter(ConverterOptions opts) {
            super(opts);
            this.options = opts;
        }

        @Override
        public ConverterOptions getOptions() {
            return options;
        }
    }

    /**
     * Minimal stub for {@link ConverterOptions}.
     */
    private static class StubOptions extends ConverterOptions {

        StubOptions(ZoneId zone) {
            super(zone);
        }

        @Override
        public ZoneId getZoneId() {
            return super.getZoneId();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a fresh Converter for each test.  The default
        // ConverterOptions will provide a deterministic ZoneId (system default).
        // If the library offers a way to set a specific ZoneId we could do it here,
        // but the test only needs to verify that the method respects whatever
        // zone the ConverterOptions supplies.
        converter = new Converter(new ConverterOptions());
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(Arguments.of("Converts a proper OffsetDateTime to a ZonedDateTime in the converter's zone (UTC).", OffsetDateTime.parse("2023-05-15T10:15:30+02:00"), ZoneId.of("UTC")), Arguments.of("Same zone conversion (UTC) – source already in UTC.", OffsetDateTime.parse("2023-01-01T12:00:00Z"), ZoneId.of("UTC")), Arguments.of("Different zone conversion causing date change (UTC).", OffsetDateTime.parse("2023-01-01T01:00:00+02:00"), ZoneId.of("UTC")), Arguments.of("Maximum OffsetDateTime value conversion (UTC).", OffsetDateTime.MAX, ZoneId.of("UTC")), Arguments.of("Minimum OffsetDateTime value conversion (UTC).", OffsetDateTime.MIN, ZoneId.of("UTC")), Arguments.of("Conversion to a custom ZoneOffset (-03:30).", OffsetDateTime.parse("2023-03-10T15:30:00+05:45"), ZoneOffset.ofHoursMinutes(-3, -30)), Arguments.of("Daylight‑Saving time zone conversion (America/New_York).", OffsetDateTime.parse("2023-07-01T12:00:00+02:00"), ZoneId.of("America/New_York")), Arguments.of("Immutability of source OffsetDateTime (Asia/Tokyo).", OffsetDateTime.parse("2023-05-15T10:15:30+02:00"), ZoneId.of("Asia/Tokyo")));
    }

    @Test
    void toZonedDateTime_convertsOffsetDateTimePreservingInstantAndApplyingConverterZone() {
        // GIVEN – an OffsetDateTime with a known instant and offset
        OffsetDateTime offsetDateTime = OffsetDateTime.of(// year, month, day
        2023, // year, month, day
        5, // year, month, day
        10, // hour, minute, second, nano
        12, // hour, minute, second, nano
        30, // hour, minute, second, nano
        45, // hour, minute, second, nano
        0, // offset -05:00
        ZoneOffset.ofHours(-5));
        // WHEN – the static conversion method is invoked
        ZonedDateTime result = OffsetDateTimeConversions.toZonedDateTime(offsetDateTime, converter);
        // THEN – the instant part must be identical
        assertEquals(offsetDateTime.toInstant(), result.toInstant(), "The instant (point on the timeline) must be preserved");
        // AND – the zone must be the one supplied by the Converter's options
        assertEquals(converter.getOptions().getZoneId(), result.getZone(), "The resulting ZonedDateTime must use the zone from ConverterOptions");
    }

    @Test
    void toZonedDateTime_withSystemDefaultZoneMatchesConverterOption() {
        // GIVEN – the system default zone (used by ConverterOptions when none is set)
        // and an OffsetDateTime that represents the same instant as now
        OffsetDateTime nowOffset = OffsetDateTime.now();
        // WHEN
        ZonedDateTime zoned = OffsetDateTimeConversions.toZonedDateTime(nowOffset, converter);
        // THEN – the zone of the result must equal the zone configured in the Converter
        assertEquals(converter.getOptions().getZoneId(), zoned.getZone(), "Resulting zone should match the Converter's zone configuration");
        // The instant should still be identical
        assertEquals(nowOffset.toInstant(), zoned.toInstant(), "Instant must remain unchanged after conversion");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validConversionProvider")
    void validConversion(String description, OffsetDateTime from, ZoneId targetZone) {
        // GIVEN
        converter = new StubConverter(targetZone);
        // WHEN
        ZonedDateTime result = OffsetDateTimeConversions.toZonedDateTime(from, converter);
        // THEN
        ZonedDateTime expected = ZonedDateTime.ofInstant(from.toInstant(), targetZone);
        assertEquals(expected, result, description);
    }

    // -----------------------------------------------------------------
    @Test
    void nullSourceObject_throwsNullPointerException() {
        // GIVEN
        converter = new StubConverter(ZoneId.of("America/New_York"));
        Object from = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toZonedDateTime(from, converter), "When the source object is null, a NullPointerException is expected.");
    }

    // -----------------------------------------------------------------
    @Test
    void invalidSourceType_throwsClassCastException() {
        // GIVEN
        converter = new StubConverter(ZoneId.of("Europe/Paris"));
        // a String, not an OffsetDateTime
        Object from = "2023-05-15T10:15:30+02:00";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> OffsetDateTimeConversions.toZonedDateTime(from, converter), "Passing a non‑OffsetDateTime should cause a ClassCastException.");
    }

    // -----------------------------------------------------------------
    @Test
    void nullConverter_throwsNullPointerException() {
        // GIVEN
        OffsetDateTime from = OffsetDateTime.now();
        Converter nullConverter = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toZonedDateTime(from, nullConverter), "A null Converter should result in NullPointerException when getOptions() is accessed.");
    }

    // -----------------------------------------------------------------
    @Test
    void converterWithNullOptions_throwsNullPointerException() {
        // GIVEN
        OffsetDateTime from = OffsetDateTime.now();
        Converter converterWithNullOptions = new StubConverter((ConverterOptions) null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toZonedDateTime(from, converterWithNullOptions), "If Converter#getOptions() returns null, a NullPointerException is expected.");
    }

    // -----------------------------------------------------------------
    @Test
    void converterOptionsWithNullZoneId_throwsNullPointerException() {
        // GIVEN
        OffsetDateTime from = OffsetDateTime.now();
        ConverterOptions optsWithNullZone = new StubOptions(null);
        Converter converter = new StubConverter(optsWithNullZone);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toZonedDateTime(from, converter), "When the Options object exists but its ZoneId is null, atZone(null) should throw NPE.");
    }
}
